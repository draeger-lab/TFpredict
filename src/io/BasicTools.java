package io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class BasicTools {
	
	public static String[] wrapString(String string) {
		return(wrapString(string, 60));
	}
	
	public static String[] wrapString(String string, int max_line_length) {

		ArrayList<String> lines = new ArrayList<String>();

		int numFullLines = string.length() / max_line_length;
		for (int i=0; i < numFullLines; i++) {
			lines.add(string.toUpperCase().substring(i * max_line_length, (i+1) * max_line_length));
		}
		
		int writtenStringLength = (string.length() / max_line_length) * max_line_length;
		if (string.length() - writtenStringLength > 0) {
			lines.add(string.toUpperCase().substring(writtenStringLength, string.length()));
		}
		return(lines.toArray(new String[]{}));
	}
	
	public static String[] getSubarray(String[] string, int start, int end) {
		
		int resLength = end-start+1;
		String[] res = new String[resLength];
		int idx = 0;
		for (int i=start; i<=end; i++) {
			res[idx++] = string[i];
		}
		return(res);
	}
	
	public static void writeArrayToFile(String[] array, String outfile) {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
		
			for (String line: array) {
				bw.write(line + "\n");
			}
			bw.flush();
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Double[] double2Double(double[] doubleArray) {
		
		Double[] res = new Double[doubleArray.length];
		for (int i=0; i<res.length; i++) {
			res[i] = new Double(doubleArray[i]);
		}
		return(res);
	}
	
	public static int getMaxIndex(double[] doubleArray) {
		
		double max = doubleArray[0];
	    int maxIndex = 0;
	    for (int i=1; i<doubleArray.length; i++) {
	        if (doubleArray[i] > max) {
	            max = doubleArray[i];
	            maxIndex = i;
	        }
	    }
	    return maxIndex;
	}

	public static HashMap<String, String> readFASTA(String fasta_file) {
		
		HashMap<String, String> sequences = new HashMap<String, String>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(fasta_file)));
		
			StringBuffer curr_seq = new StringBuffer();
			String line;
			String header = "";
			boolean first = true;
			
			while ((line = br.readLine()) != null) {
					
				// new header ?
				if (line.startsWith(">")) {
					// add last sequence
					if (!first) {
						sequences.put(header, curr_seq.toString());
					}
					// read new header
					header = new StringTokenizer(line).nextToken().replaceFirst(">", "");
					curr_seq = new StringBuffer();
					first = false;
				} else {
					curr_seq.append(line);
				}
			}
			sequences.put(header, curr_seq.toString());
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return(sequences);
	}
	
	public static void writeFASTA(HashMap<String, String> sequences, String output_file) {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output_file)));
			
			for (String header: sequences.keySet()) {
				bw.write(">" + header + "\n");
				String[] curr_seq = BasicTools.wrapString(sequences.get(header));
				for (String line: curr_seq) {
					bw.write(line + "\n");
				}
				bw.write("\n");
			}
			bw.flush();
			bw.close();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		
		String fasta_file = "/rahome/eichner/web_home/test_seq.fasta";
		HashMap<String, String> sequences = readFASTA(fasta_file);
		
		for (String header: sequences.keySet()) {
			System.out.println("> " + header);
			System.out.println(sequences.get(header));
		}
	}
}


