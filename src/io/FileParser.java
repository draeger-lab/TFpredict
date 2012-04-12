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
			 line = br.readLine();
			 
			 while (line != null) {
				 
				 String[] entry = line.split("\t");
				 
				 IPRentries.add(entry);
				 
				 line = br.readLine();
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
