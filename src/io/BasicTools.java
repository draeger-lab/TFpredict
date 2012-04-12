package io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
}
