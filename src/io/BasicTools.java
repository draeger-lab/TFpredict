/*
 * $Id: BasicTools.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/io/BasicTools.java $
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.apache.commons.math.stat.descriptive.moment.Mean;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Min;
import org.apache.commons.math.stat.descriptive.rank.Percentile;

import resources.Resource;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev: 99 $
 * @since 1.0
 */
public class BasicTools {

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(BasicTools.class.getName());

	/**
	 * 
	 */
	public static final String duplicatedHeaderKey = "duplicated";

	/**
	 * 
	 * @return
	 */
	public static boolean isWindows() {
		return(System.getProperty("os.name").contains("Windows"));
	}

	/**
	 * 
	 * @param string
	 * @return
	 */
	public static String[] wrapString(String string) {
		return(wrapString(string, 60));
	}

	/**
	 * 
	 * @param string
	 * @param max_line_length
	 * @return
	 */
	public static String[] wrapString(String string, int max_line_length) {

		List<String> lines = new ArrayList<String>();

		int numFullLines = string.length() / max_line_length;
		for (int i = 0; i < numFullLines; i++) {
			lines.add(string.toUpperCase().substring(i * max_line_length, (i + 1) * max_line_length));
		}

		int writtenStringLength = (string.length() / max_line_length) * max_line_length;
		if ((string.length() - writtenStringLength) > 0) {
			lines.add(string.toUpperCase().substring(writtenStringLength, string.length()));
		}
		return lines.toArray(new String[] {});
	}

	/**
	 * 
	 * @param string
	 * @param start
	 * @param end
	 * @return
	 */
	public static String[] getSubarray(String[] string, int start, int end) {

		int resLength = end-start+1;
		String[] res = new String[resLength];
		int idx = 0;
		for (int i=start; i<=end; i++) {
			res[idx++] = string[i];
		}
		return(res);
	}

	/**
	 * 
	 * @param list
	 * @param outfile
	 */
	public static void writeSplittedList2File(List<String[]> list, String outfile) {

		List<String> collapsedList = new ArrayList<String>();
		for (int i=0; i<list.size(); i++) {
			StringBuffer currLine = new StringBuffer();
			for (String token: list.get(i)) {
				currLine.append(token + "\t");
			}
			collapsedList.add(currLine.toString().trim());
		}
		writeList2File(collapsedList, outfile);
	}

	/**
	 * 
	 * @param list
	 * @param outfile
	 */
	public static void writeList2File(List<String> list, String outfile) {
		//String[] array = list.toArray(new String[] {});
		//writeArray2File(array, outfile);
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			for (String line : list) {
				bw.append(line);
				bw.newLine();
			}
			bw.close();
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * 
	 * @param lines
	 * @param outfile
	 */
	public static void writeString2File(String lines, String outfile) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			bw.write(lines);
			bw.close();
			//writeArray2File(lines.split("\\n"), outfile);
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * 
	 * @param array
	 * @param outfile
	 */
	public static void writeArray2File(String[] array, String outfile) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			for (String line : array) {
				bw.append(line);
				bw.newLine();
			}
			bw.close();
		} catch (IOException exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * 
	 * @param doubleArray
	 * @return
	 */
	public static Double[] double2Double(double[] doubleArray) {

		Double[] res = new Double[doubleArray.length];
		for (int i=0; i<res.length; i++) {
			res[i] = new Double(doubleArray[i]);
		}
		return(res);
	}

	/**
	 * 
	 * @param intArray
	 * @return
	 */
	public static int[] Integer2int(Integer[] intArray) {

		int[] res = new int[intArray.length];
		for (int i=0; i<res.length; i++) {
			res[i] = intArray[i].intValue();
		}
		return(res);
	}

	/**
	 * 
	 * @param doubleArray
	 * @return
	 */
	public static double[] Double2double(Double[] doubleArray) {

		double[] res = new double[doubleArray.length];
		for (int i=0; i<res.length; i++) {
			res[i] = doubleArray[i].doubleValue();
		}
		return(res);
	}

	/**
	 * 
	 * @param doubleArray
	 * @return
	 */
	public static double getMax(double[] doubleArray) {
		return getMax(doubleArray, false);
	}

	/**
	 * 
	 * @param doubleArray
	 * @return
	 */
	public static int getMaxIndex(double[] doubleArray) {
		return (int) getMax(doubleArray, true);
	}

	/**
	 * 
	 * @param doubleArray
	 * @param returnIndex
	 * @return
	 */
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

	/**
	 * 
	 * @param fasta_file
	 * @return
	 */
	public static Map<String, String> readFASTA(String fasta_file) {
		return readFASTA(fasta_file, false);
	}

	/**
	 * 
	 * @param fasta_file
	 * @param readFullHeader
	 * @return
	 */
	public static Map<String, String> readFASTA(String fasta_file, boolean readFullHeader) {

		Map<String, String> sequences = new HashMap<String, String>();

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(fasta_file)));

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
				System.out.println(header);
				sequences.put(duplicatedHeaderKey, "");
			}
			String seq = curr_seq.toString();
			if (!seq.matches("^[A-IK-NP-Za-ik-np-z\\s]*$")) {
				logger.warning("Warning. Given protein sequence \"" + header + "\" contains invalid symbols.");
			}

			sequences.put(header, seq);

		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sequences;
	}

	/**
	 * 
	 * @param header
	 * @param sequence
	 * @param output_file
	 */
	public static void writeFASTA(String header, String sequence, String output_file) {
		Map<String, String> sequenceMap = new HashMap<String, String>();
		sequenceMap.put(header, sequence);
		writeFASTA(sequenceMap, output_file);
	}

	/**
	 * 
	 * @param sequences
	 * @param output_file
	 */
	public static void writeFASTA(Map<String, String> sequences, String output_file) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output_file)));
			for (String header: sequences.keySet()) {
				bw.append('>');
				bw.append(header);
				bw.newLine();
				String[] curr_seq = BasicTools.wrapString(sequences.get(header));
				for (String line : curr_seq) {
					bw.append(line);
					bw.newLine();
				}
				bw.newLine();
			}
			bw.close();

		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * 
	 * @param resourceName
	 * @return
	 */
	public static List<String> readResource2List(String resourceName) {
		return(readResource2List(resourceName, false));
	}

	/**
	 * 
	 * @param resourceName
	 * @param upperCase
	 * @return
	 */
	public static List<String> readResource2List(String resourceName, boolean upperCase) {
		return(readStream2List(Resource.class.getResourceAsStream(resourceName), upperCase));
	}

	/**
	 * 
	 * @param fileName
	 * @param upperCase
	 * @return
	 */
	public static List<String> readFile2List(String fileName, boolean upperCase) {

		List<String> fileContent = null;

		try {
			fileContent = readStream2List(new FileInputStream(new File(fileName)), upperCase);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return fileContent;
	}

	/**
	 * 
	 * @param stream
	 * @param upperCase
	 * @return
	 */
	public static List<String> readStream2List(InputStream stream, boolean upperCase) {

		List<String> fileContent = new ArrayList<String>();

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

		return fileContent;
	}

	/**
	 * 
	 * @param infile
	 * @return
	 */
	public static Map<String, String> readFile2Map(String infile) {

		List<String[]> keyValueList = readFile2ListSplitLines(infile);
		Map<String, String> keyValueMap= new HashMap<String,String>();

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

	/**
	 * 
	 * @param infile
	 * @return
	 */
	public static String readFile2String(String infile) {

		String lines = "";
		String line;

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(infile));

			while ((line = br.readLine()) != null) {
				lines += (line + "\n");
			}
		}
		catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return lines;
	}

	/**
	 * 
	 * @param infile
	 * @return
	 */
	public static List<String[]> readFile2ListSplitLines(String infile) {
		return readFile2ListSplitLines(infile, false);
	}

	/**
	 * 
	 * @param infile
	 * @param useTokenizer
	 * @return
	 */
	public static List<String[]> readFile2ListSplitLines(String infile, boolean useTokenizer) {

		List<String[]> splittedLines = new ArrayList<String[]>();
		String line = null;

		try {
			BufferedReader br = new BufferedReader(new FileReader(infile));

			while ((line = br.readLine()) != null) {

				// skip blank lines
				if (line.trim().equals("")) {
					continue;
				}

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

	/**
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static List<String> union(List<String> list1, List<String> list2) {

		HashSet<String> unionSet = new HashSet<String>();
		for (String element: list1) {
			unionSet.add(element);
		}
		for (String element: list2) {
			unionSet.add(element);
		}
		List<String> union = new ArrayList<String>();
		union.addAll(unionSet);

		return(union);
	}

	/**
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static List<String> union(String[] list1, String[] list2) {
		return(union(new ArrayList<String>(Arrays.asList(list1)), new ArrayList<String>(Arrays.asList(list2))));
	}

	/**
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static List<String> intersect(List<String> list1, List<String> list2) {

		HashSet<String> intersectionSet = new HashSet<String>();
		for (String element: list1) {
			if (list2.contains(element)) {
				intersectionSet.add(element);
			}
		}
		List<String> intersection = new ArrayList<String>();
		intersection.addAll(intersectionSet);

		return(intersection);
	}

	/**
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static List<String> setDiff(String[] list1, String[] list2) {
		return(setDiff(new ArrayList<String>(Arrays.asList(list1)), new ArrayList<String>(Arrays.asList(list2))));
	}

	/**
	 * 
	 * @param list1
	 * @param list2
	 * @return
	 */
	public static List<String> setDiff(List<String> list1, List<String> list2) {
		HashSet<String> diffSet = new HashSet<String>();
		for (String element: list1) {
			if (!list2.contains(element)) {
				diffSet.add(element);
			}
		}
		List<String> diff = new ArrayList<String>();
		diff.addAll(diffSet);

		return(diff);
	}

	/**
	 * 
	 * @param ids
	 * @param values
	 * @return
	 */
	public static List<String[]> combineLists(List<String> ids, List<Double> values) {

		if (ids.size() != values.size()) {
			System.out.println("Error. Lists have unequal sizes.");
			System.out.println(1);
		}

		List<String[]> mergedList = new ArrayList<String[]>();
		for (int i=0; i<ids.size(); i++) {
			mergedList.add(new String[] {ids.get(i), values.get(i).toString()});
		}
		return(mergedList);
	}

	/**
	 * 
	 * @param matrix
	 * @return
	 */
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

	/**
	 * 
	 * @param s
	 * @param n
	 * @return
	 */
	public static String padRight(String s, int n) {
		return String.format("%1$-" + n + "s", s);
	}

	/**
	 * 
	 * @param s
	 * @param n
	 * @return
	 */
	public static String padLeft(String s, int n) {
		return String.format("%1$#" + n + "s", s);
	}

	/**
	 * 
	 * @param cmd
	 * @return
	 */
	public static String[] runCommand(String cmd) {
		return runCommand(cmd, true);
	}

	/**
	 * 
	 * @param cmd
	 * @param parseOutput
	 * @return
	 */
	public static String[] runCommand(String cmd, boolean parseOutput) {

		String[] consoleOutput = null;

		try {
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();

			if (parseOutput) {
				List<String> stdout = new ArrayList<String>();
				String line;
				BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));

				while ((line = br.readLine()) != null) {
					stdout.add(line.trim());
				}
				consoleOutput = stdout.toArray(new String[]{});

				br.close();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return consoleOutput;
	}

	/**
	 * 
	 * @param stringArray
	 * @return
	 */
	public static String collapseStringArray(String[] stringArray) {

		return collapseStringArray(stringArray, "\t");
	}

	/**
	 * 
	 * @param stringArray
	 * @param separator
	 * @return
	 */
	public static String collapseStringArray(String[] stringArray, String separator) {

		StringBuffer stringBuffer = new StringBuffer(stringArray[0]);
		for (int i=1; i<stringArray.length; i++) {
			stringBuffer.append(separator + stringArray[i]);
		}
		return stringBuffer.toString();
	}

	/**
	 * 
	 * @param string
	 * @param substring
	 * @return
	 */
	public static int[] getAllIndicesOf(String string, String substring) {

		List<Integer> indices = new ArrayList<Integer>();
		int index = string.indexOf(substring);
		while(index >= 0) {
			indices.add(index);
			index = string.indexOf(substring, index+1);
		}
		return Integer2int(indices.toArray(new Integer[]{}));
	}

	/**
	 * 
	 * @param file
	 */
	public static void createDir4File(String file) {

		String directory = new File(file).getParent();
		if (!new File(directory).exists() && !new File(directory).mkdirs()) {
			System.out.println("Error. Directory for file \"" + file + "\" could not be created.");
			System.exit(0);
		}
	}

	/**
	 * 
	 * @param array1
	 * @param array2
	 * @return
	 */
	public static double[] concatenateArrays(double[] array1, double[] array2) {

		int arraySize = array1.length + array2.length;
		double[] resArray = new double[arraySize];
		for (int i=0; i<array1.length; i++) {
			resArray[i] = array1[i];
		}
		for (int i=0; i<array2.length; i++) {
			resArray[array1.length+i] = array2[i];
		}
		return resArray;
	}

	/**
	 * 
	 * @param array1
	 * @param array2
	 * @return
	 */
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

	/**
	 * 
	 * @param array
	 * @return
	 */
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

	/**
	 * 
	 * @param array
	 * @param perc
	 * @return
	 */
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

	public static void copy(String infile, String outfile) {
		copy(infile, outfile, false);
	}

	/**
	 * 
	 * @param infile
	 * @param outfile
	 * @param isResource
	 */
	public static void copy(String infile, String outfile, boolean isResource) {

		try {
			BufferedReader br = null;
			if (isResource) {
				br = new BufferedReader(new InputStreamReader(Resource.class.getResourceAsStream(infile)));
			} else {
				br = new BufferedReader(new FileReader(new File(infile)));
			}
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));


			String line;
			while ((line = br.readLine()) != null) {
				bw.append(line);
				bw.newLine();
			}
			br.close();
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String doubleArrayToLibSVM(double[] doubleArray) {

		StringBuffer doubleString = new StringBuffer();
		for (int i=0; i<doubleArray.length; i++) {
			doubleString.append((i+1) + ":" + doubleArray[i] + " ");
		}
		return doubleString.toString().trim();
	}

	public static int getHammingDistance(String string1, String string2) {

		int hammingDist = 0;
		for (int i=0; i<string1.length(); i++) {
			if (string1.charAt(i) != string2.charAt(i)) {
				hammingDist += 1;
			}
		}
		return hammingDist;
	}

	/**
	 * 
	 * @param array
	 * @return
	 */
	public static int[] getMinPositions(int[] array) {

		int minDist = Integer.MAX_VALUE;
		ArrayList<Integer> minPos = null;

		for (int i=0; i<array.length; i++) {

			if (array[i] < minDist) {
				minDist = array[i];
				minPos = new ArrayList<Integer>();
				minPos.add(i);

			} else if (array[i] == minDist) {
				minPos.add(i);
			}
		}
		return Integer2int(minPos.toArray(new Integer[]{}));
	}

	/**
	 * 
	 * @param hitsOutfile
	 * @return
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static Map<String, Double> parseBLASTHits(File hitsOutfile) throws NumberFormatException, IOException {
		// read PSI-BLAST output from temporary files

		Map<String, Double> blastHits = new HashMap<String, Double>();

		int lineIdx = 1;
		String line;

		//List<String> hitsTable = BasicTools.readFile2List(hitsOutfile, false);
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(hitsOutfile)));
		for (boolean startReading = false; ((line = br.readLine()) != null) && !(startReading && line.startsWith(">")); lineIdx++) {
			line = line.trim();

			// skip header and empty or invalid lines
			if (line.startsWith("Sequences producing significant alignments")) {
				startReading = true;
				continue;
			}
			if (startReading && !line.isEmpty() && !line.startsWith(">")) {

				StringTokenizer strtok = new StringTokenizer(line);
				String hitID = strtok.nextToken();
				// correct wrong UniProt ID for T03281 in factor.dat
				if (hitID.contains("|41817|TF|")) {
					hitID = hitID.replace("|41817|TF|", "|P41817|TF|");
				}
				String nextToken = null;
				while (strtok.hasMoreTokens() && (nextToken = strtok.nextToken()).startsWith("GO:")) {
					// skip GO terms in non-TF headers
				}
				blastHits.put(hitID, Double.parseDouble(nextToken)); // hit score

			}
		}
		br.close();
		logger.fine(MessageFormat.format("Successfully read {0,number,integer} lines from hits file {1}.", lineIdx, hitsOutfile));

		return blastHits;
	}
}


