/*  
 * $Id$
 * $URL$
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2014 Center for Bioinformatics Tuebingen (ZBIT),
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class TransFacParser extends TFdataParser {

	private boolean silent = false;
	
	/*
	 *  parses TFs from flatfile "factor.dat"
	 *  required attributes: accession, species, and sequence  
	 *  optional attributes: UniProt ID, TransFac class, PFMs
	 *  
	 */
	
	public void parseFactors(String infile) {
		parseFactors(infile, null);
	}
	
	public void parseFactors(String infile, String species_name) {
		
		String line = "";
		
		String curr_acc, curr_spec, curr_class, curr_seq, curr_ref;
		curr_acc = curr_spec = curr_seq = null;
		curr_ref = curr_class = "NA";
		ArrayList<String> curr_pfm_IDs = new ArrayList<String>();
		
		String[] split;
		StringTokenizer strtok;
		
		int entry_counter, line_counter;
		entry_counter = line_counter = 0;

		try {
			
			/*
			 *  count factors
			 */
			
			BufferedReader br = new BufferedReader(new FileReader(new File(infile)));
			
			while ((line=br.readLine()) != null) {
				if (line.startsWith("AC  ")) entry_counter++;
			}
			
				
			boolean[] acc_parsed = 		new boolean[entry_counter];
			boolean[] species_parsed = 	new boolean[entry_counter];
			boolean[] class_parsed = 	new boolean[entry_counter];
			boolean[] seq_parsed = 		new boolean[entry_counter];
			boolean[] ref_parsed = 		new boolean[entry_counter];
			boolean[] pfm_parsed = 		new boolean[entry_counter];
			
			
			/*
			 *  parse factors
			 */
			
			entry_counter = 0;

			br = new BufferedReader(new FileReader(new File(infile)));
			
			br.readLine();  // skip first three lines
			br.readLine();
			br.readLine();
			line_counter += 3;
			
			while ((line=br.readLine()) != null) {
				line_counter++;
				

				// parse accession number
				if (line.startsWith("AC")) {
					strtok = new StringTokenizer(line.substring(4));
					curr_acc = strtok.nextToken().trim();
					acc_parsed[entry_counter] = true;
				}
				
				// parse species
				if (line.startsWith("OS")) {
					curr_spec = line.substring(4).trim();
					if ((split = curr_spec.split(",")).length == 2) 
						curr_spec = split[1].trim();
					if (!line.contains("/")) species_parsed[entry_counter] = true;
					
				}
				
				// parse TRANSFAC class
				if (line.startsWith("CL")) {
					curr_class = line.substring(4).trim();
					class_parsed[entry_counter] = true;
				}
				
				// parse sequence
				if (line.startsWith("SQ")) {
					
					curr_seq = line.substring(4).trim();
				
					while ((line = br.readLine()).startsWith("SQ")) {
						curr_seq += line.substring(4).trim();
						line_counter++;
					}
					line_counter++;
					seq_parsed[entry_counter] = true;
				}
				
				// parse reference to UniProt
				if (line.startsWith("SC")) {
					strtok = new StringTokenizer(line.substring(4).replace("#", " "));
					while (strtok.hasMoreTokens()) {
						if (strtok.nextToken().trim().equals("Swiss-Prot")) {
							if (strtok.hasMoreTokens()) {
								curr_ref = strtok.nextToken().trim();
								ref_parsed[entry_counter] = true;
							}
							break;
						}
					}
				}
				
				if (!ref_parsed[entry_counter] && line.startsWith("DR  SWISSPROT")) {
					
					curr_ref = line.replace("DR  SWISSPROT: ", "").substring(0,6);
					ref_parsed[entry_counter] = true;
				}
					
				if (line.startsWith("MX")) {
					pfm_parsed[entry_counter] = true;
					curr_pfm_IDs = new ArrayList<String>();
					while (line.startsWith("MX")) {
						curr_pfm_IDs.add(line.substring(4, 10));
						line = br.readLine();
					}
				}
				
				// check parsed information
				if (line.startsWith("//")) {
					
					if (species_name != null && !curr_spec.equals(species_name)) {
						continue;
					}
					
					if (acc_parsed[entry_counter] 
					    && species_parsed[entry_counter]
					    && seq_parsed[entry_counter]) {
						
						tf_names.add(curr_acc);
						species.add(curr_spec);
						crossrefs.add(curr_ref);
						classes.add(curr_class);
						sequences1.add(curr_seq);
						sequences2.add(null);

						// add PFM (if available)
						if (pfm_parsed[entry_counter]) {
							pfm_names.add(curr_pfm_IDs);
						} else {
							pfm_names.add(null);
						}
					}
					entry_counter++;
					curr_ref = "NA";
					curr_class = "NA";
				}
			}
			br.close();
			
			/*
			 *  print statistics
			 */
			
			if (!silent) {
			
				int acc_counter, spec_counter, class_counter, seq_counter, ref_counter, pfm_counter;
				acc_counter = spec_counter = class_counter = seq_counter = ref_counter = pfm_counter = 0;
			
				for(int i=0; i<entry_counter; i++) if (acc_parsed[i]) acc_counter++;
				for(int i=0; i<entry_counter; i++) if (species_parsed[i]) spec_counter++;
				for(int i=0; i<entry_counter; i++) if (class_parsed[i]) class_counter++;
				for(int i=0; i<entry_counter; i++) if (seq_parsed[i]) seq_counter++;
				for(int i=0; i<entry_counter; i++) if (ref_parsed[i]) ref_counter++;
				for(int i=0; i<entry_counter; i++) if (pfm_parsed[i]) pfm_counter++;

				logger.info("Number of transcription factors: " + entry_counter);
				logger.info("Number of parsed factors:        " + tf_names.size() + "\n");

				logger.info("Accession numbers:  " + acc_counter);
				logger.info("Species:            " + spec_counter);
				logger.info("TRANSFAC class:     " + class_counter);
				logger.info("Sequences:          " + seq_counter);
				logger.info("UniProt References: " + ref_counter);
				logger.info("Matrices:           " + pfm_counter);
			}
		}
		catch(IOException ioe) {
			logger.warning(ioe.getMessage());
			logger.warning("IOException occurred while parsing transcription factors from FACTOR table.");
			logger.warning("Line " + line_counter + ": " + line);
		}
	}
	
	
	/*
	 *  parse matrices from flatfile "matrix.dat"
	 */
	
	public void parseMatrices(String matrixFile) {
		
		String line;
		String curr_acc = null;
		ArrayList<String> curr_matrix = new ArrayList<String>();
		
		boolean acc_parsed, matrix_parsed;
		acc_parsed = matrix_parsed = false;
		StringTokenizer strtok;
		int entry_counter = 0;
		
		HashMap<String, String[]> matrices = new HashMap<String, String[]>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(matrixFile)));
			
			// skip header
			br.readLine();
			br.readLine();
			br.readLine();
			
			while((line = br.readLine()) != null) {
				
				// read accession number
				if (line.startsWith("AC")) {
					strtok = new StringTokenizer(line.substring(4));
					curr_acc = strtok.nextToken().trim();
					acc_parsed = true;
				}
				
				// read matrix
				if (line.startsWith("P0")) {
					curr_matrix = new ArrayList<String>();
					while (!(line = br.readLine()).startsWith("XX")) {
						curr_matrix.add(line);
					}
					matrix_parsed = true;
				}
				
				// save matrix entry
				if (line.startsWith("//")) {
					if (acc_parsed && matrix_parsed) {
						
						String[] curr_pfm = TFdataTools.convertPFMformat(curr_matrix);

						matrices.put(curr_acc, curr_pfm);
					
						entry_counter++;
						acc_parsed = false;
						matrix_parsed = false;
					
					} else {
						logger.info("Parse Error. Matrix entry " + entry_counter + " could not be parsed.");
						System.exit(0);
					}
				}
			}
			br.close();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		/* 
		 *  map matrices from "matrix.dat" to TFs from "factor.dat"
		 */
		
		for (int i=0; i<tf_names.size(); i++) {
			
			if (pfm_names.get(i) != null) {
				ArrayList<String[]> curr_pfms = new ArrayList<String[]>();
				for (String pfm_name: pfm_names.get(i)) {
					curr_pfms.add(matrices.get(pfm_name));
				}
				pfms.add(curr_pfms);
				
			} else {
				pfms.add(null);
			}
		}
	}
}
