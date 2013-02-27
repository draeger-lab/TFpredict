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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class SabineTrainingSetParser extends TFdataParser {
	
	public void parseTrainingset(String infile) {
		
		String line, curr_name, curr_species, curr_ref, curr_class, seq1, seq2;
		StringTokenizer strtok;
		ArrayList<String> curr_domains, curr_pfm_names;
		ArrayList<String[]> curr_pfms;
		String[] splitted_species, curr_pfm;
		
		try {
			 
			 BufferedReader br = new BufferedReader(new FileReader(new File(infile)));
			 
			 while((line = br.readLine()) != null && line.length() > 0) {
				
				if (! line.startsWith("NA")) {
					System.out.println("Parse Error. \"NA\" expected at the beginning of the line.\nLine: " + line);
					System.exit(0);
				}
				 
				strtok = new StringTokenizer(line.substring(2));
				
				curr_name = strtok.nextToken().trim();                       // parse TRANSFAC name
				
				br.readLine();                                               // XX
				
				curr_species = (line = br.readLine()).substring(4).trim();   // parse species
				
				splitted_species = curr_species.split(",");
				
				if(splitted_species.length == 1) {
					curr_species = splitted_species[0];
				}
				else if(splitted_species.length == 2) {
					curr_species = splitted_species[1].trim();
				}
						
				br.readLine();											     // XX
				
				// parse UniProt ID
				if ((line = br.readLine()).startsWith("RF")) {
					curr_ref = line.substring(4).trim();   	 
				
					br.readLine();											     // XX
					line = br.readLine();										 // CL, S1 or S2
					
					if (! line.startsWith("CL") && ! line.startsWith("S")) {
						System.out.println("Parse Error. CL, S1 or S2 expected.");
						System.exit(0);
					}
				}
				else {
					curr_ref = "NA";
				}
				
				// parse superclass
				if (line.startsWith("CL")) {
					curr_class = line.substring(4).trim(); 
					
					br.readLine();												 // XX
					line = br.readLine();									     // S1 or S2
				}
				else {
					curr_class = "NA";
				}
				                                               
				
				// parse sequence(s)
				
				seq1 = null;
				seq2 = null;
				
				if (line.startsWith("S1  ")) {								 // parse first sequence
					seq1 = line.substring(4);
					while ( (line = br.readLine()).startsWith("S1  ") ) {
						seq1 += line.substring(4);
					}
					line = br.readLine();                                    // XX
				}
				
				if (line.startsWith("S2  ")) {								 // parse second sequence
					seq2 = line.substring(4);
					while ( (line = br.readLine()).startsWith("S2  ") ) {
						seq2 += line.substring(4);		    
					}
					line = br.readLine();								     // XX
				}
				
				// parse domains			
				curr_domains = new ArrayList<String>();
				
				while (line.startsWith("FT")) {
					curr_domains.add(line.substring(4).trim());
					line = br.readLine();
				}
				if (curr_domains.size() > 0) {
					line = br.readLine();
				}
				
				// parse PFMs
				curr_pfm_names = new ArrayList<String>();
				curr_pfms = new ArrayList<String[]>();
				
				while(line.startsWith("MN")) {
					
					strtok = new StringTokenizer(line.substring(4));	   // parse matrix name
					curr_pfm_names.add(strtok.nextToken().trim());
					
					line = br.readLine();										   // XX
					
					curr_pfm = new String[4];							   // parse matrix
					curr_pfm[0] = br.readLine().substring(4).trim();
					curr_pfm[1] = br.readLine().substring(4).trim();
					curr_pfm[2] = br.readLine().substring(4).trim();
					curr_pfm[3] = br.readLine().substring(4).trim();
					curr_pfms.add(curr_pfm);
					
					line = br.readLine();                                         // XX
					line = br.readLine();                                         // MN
				}
				
				if (line.startsWith("//")) {
					
					tf_names.add(curr_name);
					species.add(curr_species);
					crossrefs.add(curr_ref);
					classes.add(curr_class);
					sequences1.add(seq1);
					sequences2.add(seq2);
					domains.add(curr_domains);
					pfm_names.add(curr_pfm_names);
					pfms.add(curr_pfms);
					br.readLine();									  	  // XX
				}	
			 }
		}
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while parsing transcription factors .");
		}
	}

}
