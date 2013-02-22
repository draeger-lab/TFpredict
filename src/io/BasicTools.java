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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Min;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

import resources.Resource;

public class BasicTools {
	
	public static final String duplicatedHeaderKey = "duplicated";
	
	public static boolean isWindows() {
		return(System.getProperty("os.name").startsWith("Windows"));
	}
	
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
	
	public static void writeSplittedArrayList2File(ArrayList<String[]> arraylist, String outfile) {
		
		ArrayList<String> collapsedList = new ArrayList<String>();
		for (int i=0; i<arraylist.size(); i++) {
			StringBuffer currLine = new StringBuffer();
			for (String token: arraylist.get(i)) {
				currLine.append(token + "\t");
			}
			collapsedList.add(currLine.toString().trim());
		}
		writeArrayList2File(collapsedList, outfile);
	}
	
	public static void writeArrayList2File(ArrayList<String> arraylist, String outfile) {
		
		String[] array = arraylist.toArray(new String[] {});
		writeArray2File(array, outfile);
	}
	
	public static void writeString2File(String lines, String outfile) {
		
		writeArray2File(lines.split("\\n"), outfile);
	}
	
	public static void writeArray2File(String[] array, String outfile) {
		
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
	
	public static int[] Integer2int(Integer[] intArray) {
		
		int[] res = new int[intArray.length];
		for (int i=0; i<res.length; i++) {
			res[i] = intArray[i].intValue();
		}
		return(res);
	}
	
	public static double[] Double2double(Double[] doubleArray) {
		
		double[] res = new double[doubleArray.length];
		for (int i=0; i<res.length; i++) {
			res[i] = doubleArray[i].doubleValue();
		}
		return(res);
	}
	
	public static double getMax(double[] doubleArray) {
		return getMax(doubleArray, false);
	}
	
	public static int getMaxIndex(double[] doubleArray) {
		return (int) getMax(doubleArray, true);
	}
	
	public static double getMax(double[] doubleArray, boolean returnIndex) {
		
		double max = doubleArray[0];
	    int maxIndex = 0;
	    for (int i=1; i<doubleArray.length; i++) {
	        if (doubleArray[i] > max) {
	            max = doubleArray[i];
	            maxIndex = i;
	        }
	    }
	    if (returnIndex) {
	    	return maxIndex;
	    } else {
	    	return max;
	    }
	}

	
	public static HashMap<String, String> readFASTA(String fasta_file) {
		return readFASTA(fasta_file, false);
	}
	
	public static HashMap<String, String> readFASTA(String fasta_file, boolean readFullHeader) {
		
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
					if (readFullHeader) {
						header = line.replaceFirst(">", "").trim();
					
					// generate headers as done by InterProScan 
					// ">sp|P04637|P53_HUMAN Cellular tumor..." --> "P53_HUMAN" 
					} else {
						header = new StringTokenizer(line.replaceFirst(">\\s*", "")).nextToken();
						if (header.contains("|")) {
							String[] splitted_header = header.split("\\|");
							header = splitted_header[splitted_header.length-1].trim();
						}
					}
					curr_seq = new StringBuffer();
					first = false;
				} else {
					curr_seq.append(line);
				}
			}
			
			// If FASTA file contains duplicated headers --> mark HashTable
			if (sequences.containsKey(header)) {
				sequences.put(duplicatedHeaderKey, "");
			}
			String seq = curr_seq.toString();
			if (!seq.matches("^[A-IK-NP-Za-ik-np-z\\s]*$")) {
				System.out.println("\nWarning. Given protein sequence \"" + header + "\" contains invalid symbols.");
			}
			
			sequences.put(header, seq);
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		return(sequences);
	}
	
	
	public static void writeFASTA(String header, String sequence, String output_file) {
		
		HashMap<String, String> sequenceMap = new HashMap<String, String>();
		sequenceMap.put(header, sequence);
		writeFASTA(sequenceMap, output_file);
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
		
	public static ArrayList<String> readResource2List(String resourceName) {
		return(readResource2List(resourceName, false));
	}
	
	public static ArrayList<String> readResource2List(String resourceName, boolean upperCase) {
		return(readStream2List(Resource.class.getResourceAsStream(resourceName), upperCase));
	}
		
	public static ArrayList<String> readFile2List(String fileName, boolean upperCase) {
		
		ArrayList<String> fileContent = null;
		
		try {
			fileContent = readStream2List(new FileInputStream(new File(fileName)), upperCase);
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return fileContent;
	}
	
	public static ArrayList<String> readStream2List(InputStream stream, boolean upperCase) {
		
		ArrayList<String> fileContent = new ArrayList<String>();
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(stream));
			
			String line;
			while ((line = br.readLine()) != null) {
				if (upperCase) {
					fileContent.add(line.trim().toUpperCase());
				} else {
					fileContent.add(line.trim());
				}
			}
			br.close();
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return(fileContent);
	}
	
	
	public static HashMap<String, String> readFile2Map(String infile) {
		
		ArrayList<String[]> keyValueList = readFile2ListSplitLines(infile);
		HashMap<String, String> keyValueMap= new HashMap<String,String>();
		
		for (String[] pair: keyValueList) {
			
			if (pair.length >= 2) {
				keyValueMap.put(pair[0], pair[1]);
			
			} else {
				System.out.println("Error. File does not contain tab-separated key value pairs.");
				System.exit(1);
			}
		}
		return (keyValueMap);
	}
	
	
	public static String readFile2String(String infile) {
		
		String lines = "";
		String line;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(infile));
			
			while ((line = br.readLine()) != null) {
				 lines += (line + "\n");
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		}	
		return lines;
	}
	
	public static ArrayList<String[]> readFile2ListSplitLines(String infile) {
		return readFile2ListSplitLines(infile, false);
	}
	
	public static ArrayList<String[]> readFile2ListSplitLines(String infile, boolean useTokenizer) {
		
		ArrayList<String[]> splittedLines = new ArrayList<String[]>();
		String line = null;
		
		try {
			 BufferedReader br = new BufferedReader(new FileReader(infile));
			 
			 while ((line = br.readLine()) != null) {
				 
				 // skip blank lines
				 if (line.trim().equals("")) continue;
				 
				 // add splitted line
				 if (useTokenizer) {
					 StringTokenizer strtok = new StringTokenizer(line);
					 int numValues = strtok.countTokens();
					 String[] currEntries = new String[numValues];
					 for (int t=0; t < numValues; t++) {
						 currEntries[t] = strtok.nextToken();
					 }
					 splittedLines.add(currEntries);
					 
				 } else {
					 splittedLines.add(line.split("\t"));
				 }
			}		 
			 br.close();
		}
		
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		
		return splittedLines;
	}	
	
	public static ArrayList<String> union(ArrayList<String> list1, ArrayList<String> list2) {
		
		HashSet<String> unionSet = new HashSet<String>();
		for (String element: list1) {
			unionSet.add(element);
		}
		for (String element: list2) {
			unionSet.add(element);
		}
		ArrayList<String> union = new ArrayList<String>();
		union.addAll(unionSet);
		
		return(union);
	}
	
	public static ArrayList<String> union(String[] list1, String[] list2) {
		return(union(new ArrayList<String>(Arrays.asList(list1)), new ArrayList<String>(Arrays.asList(list2))));
	}
	
	public static ArrayList<String> intersect(ArrayList<String> list1, ArrayList<String> list2) {
		
		HashSet<String> intersectionSet = new HashSet<String>();
		for (String element: list1) {
			if (list2.contains(element)) {
				intersectionSet.add(element);
			}
		}
		ArrayList<String> intersection = new ArrayList<String>();
		intersection.addAll(intersectionSet);
		
		return(intersection);
	}
	
	public static ArrayList<String> setDiff(String[] list1, String[] list2) {
		return(setDiff(new ArrayList<String>(Arrays.asList(list1)), new ArrayList<String>(Arrays.asList(list2))));
	}
	
	public static ArrayList<String> setDiff(ArrayList<String> list1, ArrayList<String> list2) {
		HashSet<String> diffSet = new HashSet<String>();
		for (String element: list1) {
			if (!list2.contains(element)) {
				diffSet.add(element);
			}
		}
		ArrayList<String> diff = new ArrayList<String>();
		diff.addAll(diffSet);
		
		return(diff);
	}
	
	public static ArrayList<String[]> combineLists(ArrayList<String> ids, ArrayList<Double> values) {
		
		if (ids.size() != values.size()) {
			System.out.println("Error. Lists have unequal sizes.");
			System.out.println(1);
		}
		
		ArrayList<String[]> mergedList = new ArrayList<String[]>();
		for (int i=0; i<ids.size(); i++) {
			mergedList.add(new String[] {ids.get(i), values.get(i).toString()});
		}
		return(mergedList);
	}
	
	public static double[] getColMeans(double[][] matrix) {
		
		double[] colMeans = new double[matrix[0].length];
		for (int i=0; i<matrix[0].length; i++) {
			double colSum = 0;
			for (int j=0; j<matrix.length; j++) {
				colSum += matrix[j][i];
			}
			colMeans[i] = colSum / matrix.length;
		}
		return(colMeans);
	}
	
	public static String padRight(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}

	public static String padLeft(String s, int n) {
	    return String.format("%1$#" + n + "s", s);  
	}
	
	public static void main(String args[]) {
		
		String fasta_file = "/rahome/eichner/web_home/test_seq.fasta";
		HashMap<String, String> sequences = readFASTA(fasta_file);
		
		for (String header: sequences.keySet()) {
			System.out.println("> " + header);
			System.out.println(sequences.get(header));
		}
	}
	
	
	public static String[] runCommand(String cmd) {
		return runCommand(cmd, true);
	}
	
	public static String[] runCommand(String cmd, boolean parseOutput) {
		
		String[] consoleOutput = null;
		
		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
			
			if (parseOutput) {
				ArrayList<String> stdout = new ArrayList<String>();
				String line;
				BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			
				while ((line = br.readLine()) != null) {
					stdout.add(line.trim());
				}
				consoleOutput = stdout.toArray(new String[]{});
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return consoleOutput;
	}
	
	public static String collapseStringArray(String[] stringArray) {
		
		return collapseStringArray(stringArray, "\t");
	}
	
	public static String collapseStringArray(String[] stringArray, String separator) {
		
		StringBuffer stringBuffer = new StringBuffer(stringArray[0]);
		for (int i=1; i<stringArray.length; i++) {
			stringBuffer.append(separator + stringArray[i]);
		}
		return stringBuffer.toString();
	}

	public static int[] getAllIndicesOf(String string, String substring) {
		
		ArrayList<Integer> indices = new ArrayList<Integer>();
		int index = string.indexOf(substring);
		while(index >= 0) {
			indices.add(index);
			index = string.indexOf(substring, index+1);
		}
		return Integer2int(indices.toArray(new Integer[]{}));
	}
	
	public static void createDir4File(String file) {
		
		String directory = new File(file).getParent();
		if (!new File(directory).exists() && !new File(directory).mkdirs()) {
			System.out.println("Error. Directory for file \"" + file + "\" could not be created.");
			System.exit(0);
		}
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] concatenateArrays(T[] array1, T[] array2) {
		
		int arraySize = array1.length + array2.length;
		T[] resArray = (T[]) new Object[arraySize];
		for (int i=0; i<array1.length; i++) {
			resArray[i] = array1[i];
		}
		for (int i=0; i<array2.length; i++) {
			resArray[array1.length+i] = array2[i];
		}
		return resArray;
	}
	
	public static double[] transform2zScores(double[] array) {
		
		Mean mean = new Mean();
		StandardDeviation sd = new StandardDeviation(false);
		
		double mu = mean.evaluate(array);
		double sigma = sd.evaluate(array);
		
		double[] zScores = new double[array.length];
		for (int i=0; i<array.length; i++) {
			zScores[i] = (array[i] - mu) / sigma;
		}
		return(zScores);
	}
	
	
	public static double computePercentile(double[] array, double perc) {
		
		double res = 0;
		if (perc == 0) {
			Min minCalculator = new Min();
			res = minCalculator.evaluate(array);
			
		} else if (perc <= 100) {
			Percentile percCalculator = new Percentile();
			res = percCalculator.evaluate(array, perc);
			
		} else {
			System.out.println("Error. Percentile has to be between 0 and 100.");
			System.exit(0);
		}
		return(res);
	}

} 


