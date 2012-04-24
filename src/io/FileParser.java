/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2012 ZBIT, University of Tübingen, Florian Topf and Johannes Eichner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io;
/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/*
 * File Parser
 */


public class FileParser {

	boolean silent = false;
	
	public FileParser(boolean silent) {
		this.silent = silent;
	}
	public FileParser() {}
	
	
	public String parseSeq(String infile) {
		
		if (! silent) System.out.println("Parsing: " +infile);
		
		
		String seq = "";
			
			String line = null;
		
			try {
				 
				 BufferedReader br = new BufferedReader(new FileReader(infile));
				 line = br.readLine();
				 
				 while (line != null) {
					 

					 seq = seq.concat(line);
					 
					 line = br.readLine();
					 
				 }
				 
				 br.close();
				 
			} 						
		
			catch(IOException ioe) {
				System.out.println(ioe.getMessage());
				System.out.println("IOException occurred while parsing...");
			}
			
	return seq;
			
	}
	
	
	public ArrayList<String[]> parseIPRout(String infile) {
		
		if (! silent) System.out.println("Parsing: "+infile);
		ArrayList<String[]> IPRentries = new ArrayList<String[]>();
		String line = null;
		
		try {
			 
			 BufferedReader br = new BufferedReader(new FileReader(infile));
			 
			 while ((line = br.readLine()) != null) {
				 
				 if (line.trim().equals("")) continue;
				 IPRentries.add(line.split("\t"));
			}		 
			 br.close();
		}
		
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while parsing...");
			System.exit(1);
		}
		
		return IPRentries;
	}
	
	
	// parse goterms
	public ArrayList<String> parseGO(String infile) {
		
		if (! silent) System.out.println("Parsing: "+infile);
		
		ArrayList<String> goterms = new ArrayList<String>();
		
		int count = 0;
		
		String line = null;
		
		try {
			 
			 BufferedReader br = new BufferedReader(new FileReader(infile));
			 line = br.readLine();
			 
			 while (line != null) {
				 
				 goterms.add(line.trim());
				 count++;
				 
				 line = br.readLine();
			}
			 			 
			 br.close();
		
		}
		
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while parsing...");
		}
		
		if (! silent) System.out.println(count+" goterms parsed.");
		
		return goterms;
	}
	
	
	
}
