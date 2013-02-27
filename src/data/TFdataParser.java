/*  
 * $Id$
 * $URL$
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2013 Center for Bioinformatics Tuebingen (ZBIT),
 * University of Tuebingen by Johannes Eichner, Florian Topf, Andreas Draeger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package data;

import io.BasicTools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public abstract class TFdataParser {
	
	protected List<String> tf_names = new ArrayList<String>();
	protected List<String> species = new ArrayList<String>();
	protected List<String> crossrefs = new ArrayList<String>();
	protected List<String> classes = new ArrayList<String>();
	protected List<String> sequences1 = new ArrayList<String>();
	protected List<String> sequences2 = new ArrayList<String>();
	protected List<List<String>> domains = new ArrayList<List<String>>();
	protected List<List<String>> pfm_names = new ArrayList<List<String>>();
	protected List<List<String[]>> pfms = new ArrayList<List<String[]>>();
	
	/**
	 * A {@link Logger} for this class.
	 */
	public static Logger logger = Logger.getLogger(TFdataParser.class.getName());

	protected void setDataSource(String dataSource) {
		
		for (int i=0; i<tf_names.size(); i++) {
			tf_names.set(i, tf_names.get(i) + "|" + crossrefs.get(i) + "|" + dataSource);
		}
	}
	
	protected void filterTFsWithPFM() {
		
		for (int i=(pfms.size()-1); i>=0 ;i--) {
			if (pfms.get(i) == null) {
				removeTF(i);
			}
		}
	}
	 
	protected void filterTFsWithDBD() {
		
		for (int i=(domains.size()-1); i>=0 ;i--) {
			if (domains.get(i) == null) {
				removeTF(i);
			}
		}
	}
	
	protected void sanitizeTrainingset() {
		
		// eliminate duplicated UniProt IDs
		for (int i=(crossrefs.size()-1); i>=0 ;i--) {
			int numOcc = Collections.frequency(crossrefs, crossrefs.get(i));
			if (!crossrefs.get(i).equals("NA") && numOcc > 1) {
				removeTF(i);
			}
		}
		
		// eliminate duplicated sequences
		for (int i=(sequences1.size()-1); i>=0 ;i--) {
			int numOcc = Collections.frequency(sequences1, sequences1.get(i));
			if (numOcc > 1) {
				removeTF(i);
			}
		}
	}
	
	public void removeTF(int idx) {
		tf_names.remove(idx);
		species.remove(idx);
		crossrefs.remove(idx);
		classes.remove(idx);
		sequences1.remove(idx);
		sequences2.remove(idx);
		pfm_names.remove(idx);
		pfms.remove(idx);
		if (domains.size() > 0) {
			domains.remove(idx);
		}
	}
	
	
	protected void add(SabineTrainingSetParser parser) {
		
		for (int i=0; i<parser.tf_names.size(); i++) {
			
			boolean seqExists = sequences1.contains(parser.sequences1.get(i));
			boolean idExists = !parser.crossrefs.get(i).equals("NA") && crossrefs.contains(parser.crossrefs.get(i));
			
			// merge entries with identical sequences or UniProt IDs
			if (seqExists || idExists) {
				
				// get index of matching TF entry
				int idx = 0;
				if (seqExists) {
					idx = sequences1.indexOf(parser.sequences1.get(i));
					logger.log(Level.INFO, "Merging entry \"" + tf_names.get(idx) + "\" with added entry \"" + parser.tf_names.get(i) + "\" due to equal sequences.");
					
				} else {
					idx = crossrefs.indexOf(parser.crossrefs.get(i));
					logger.log(Level.INFO, "Merging entry \"" + tf_names.get(idx) + "\" with added entry \"" + parser.tf_names.get(i) + "\" due to equal UniProt IDs.");
				}
				
				if (crossrefs.get(idx).equals("NA") && !parser.crossrefs.get(i).equals("NA")) {
					crossrefs.set(idx, parser.crossrefs.get(i));
				}
				
				if (classes.get(idx).equals("NA") && !parser.classes.get(i).equals("NA")) {
					classes.set(idx, parser.classes.get(i));
				}
				
				List<String> curr_pfm_names = pfm_names.get(idx);
				curr_pfm_names.addAll(parser.pfm_names.get(i));
				pfm_names.set(idx, curr_pfm_names);
			
				List<String[]> curr_pfms = pfms.get(idx);
				curr_pfms.addAll(parser.pfms.get(i));
				pfms.set(idx, curr_pfms);
				
			// add new entry
			} else {
				logger.log(Level.INFO, "Adding new entry \"" + parser.tf_names.get(i) + "\"");
				
				tf_names.add(parser.tf_names.get(i));
				species.add(parser.species.get(i));
				crossrefs.add(parser.crossrefs.get(i));
				classes.add(parser.classes.get(i));
				sequences1.add(parser.sequences1.get(i));
				sequences2.add(parser.sequences2.get(i));
				pfm_names.add(parser.pfm_names.get(i));
				pfms.add(parser.pfms.get(i));
				if (domains.size() > 0) {
					domains.add(parser.domains.get(i));
				}
			}
		}
	}
	
	protected void writeTrainingset(String outfile) {
		
		int SEQLINELENGTH = 60;
		String curr_seq;
	
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
	
			for (int i=0; i<tf_names.size(); i++) {
	
				bw.write("NA  " + tf_names.get(i) + "\n" +
						"XX\n" + 
						"SP  " + species.get(i) + "\n" + 
						"XX\n" + 
						"RF  " + crossrefs.get(i) + "\n" + 
						"XX\n" + 
						"CL  " + classes.get(i) + "\n" + 
						"XX\n");
	
				curr_seq = sequences1.get(i);
	
				if (curr_seq != null) { 		
					for(int j=0; j<(curr_seq.length()/SEQLINELENGTH); j++) {
	
						bw.write("S1  "); 
						bw.write(curr_seq.toUpperCase(), j*SEQLINELENGTH, SEQLINELENGTH);
						bw.write("\n");	
					}
	
					if(curr_seq.length()-(curr_seq.length()/SEQLINELENGTH)*SEQLINELENGTH > 0) {						
	
						bw.write("S1  "); 
						bw.write(curr_seq.toUpperCase(), (curr_seq.length()/SEQLINELENGTH)*SEQLINELENGTH, curr_seq.length()-(curr_seq.length()/SEQLINELENGTH)*SEQLINELENGTH);
						bw.write("\n");	
					}
					bw.write("XX\n");
				}
	
				curr_seq = sequences2.get(i);
	
				if (curr_seq != null) { 
					for(int j=0; j<(curr_seq.length()/SEQLINELENGTH); j++) {
	
						bw.write("S2  "); 
						bw.write(curr_seq.toUpperCase(), j*SEQLINELENGTH, SEQLINELENGTH);
						bw.write("\n");	
					}
	
					if(curr_seq.length()-(curr_seq.length()/SEQLINELENGTH)*SEQLINELENGTH > 0) {
	
						bw.write("S2  "); 
						bw.write(curr_seq.toUpperCase(), (curr_seq.length()/SEQLINELENGTH)*SEQLINELENGTH, curr_seq.length()-(curr_seq.length()/SEQLINELENGTH)*SEQLINELENGTH);
						bw.write("\n");	
					}
					bw.write("XX\n");
				}
	
				// write domains 
				if (domains.size() > 0) {
					
					if (domains.get(i) != null) {
						for (int j=0; j<domains.get(i).size(); j++) {
							bw.write("FT  " + domains.get(i).get(j) + "\n");
						}
						if (domains.get(i).size() > 0) {
							bw.write("XX\n");
						}
					}
				}
	
	
				// write PFMs
				List<String> curr_pfm_names = pfm_names.get(i);
				if (curr_pfm_names != null) {
					for (int j=0; j<curr_pfm_names.size(); j++) {
						bw.write("MN  " + pfm_names.get(i).get(j) + "\n");
						bw.write("XX\n");
	
						bw.write("MA  " + pfms.get(i).get(j)[0] + "\n");
						bw.write("MA  " + pfms.get(i).get(j)[1] + "\n");
						bw.write("MA  " + pfms.get(i).get(j)[2] + "\n");
						bw.write("MA  " + pfms.get(i).get(j)[3] + "\n");
						bw.write("XX\n");
					}
				}
				bw.write("//\n" +
						"XX\n");
			}
	
			bw.flush();
			bw.close();
		}
	
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while parsing transcription factors .");
		}
	}
	
	protected void writeFastafile(String outfile) {
		writeFastafile(outfile, null);
	}
	
	protected void writeFastafile(String outfile, String[] unknownClassList) {
		
		List<String> unknownClasses = new ArrayList<String>();
		unknownClasses.add("NA");
		if (unknownClassList != null) {
			for (String tfClass: unknownClassList) {
				unknownClasses.add(tfClass);
			}
		}
	
		HashMap<String,String> fastaContent = new HashMap<String,String>();
		
		for (int i=0; i<tf_names.size(); i++) {
			
			// get TransFac class (if available)
			String currClass = classes.get(i);
			String transfacClass = "NA";
			if (!unknownClasses.contains(currClass)) {
				transfacClass = TFdataTools.getTransfacClass(currClass); 
			}
			
			// add class to FASTA header
			String header = tf_names.get(i) + "|TF|" + transfacClass;
			fastaContent.put(header, sequences1.get(i));
		}
		
		// write sequences to FASTA file
		BasicTools.writeFASTA(fastaContent, outfile);
	}
}
