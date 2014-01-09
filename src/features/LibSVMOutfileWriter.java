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
package features;


import io.BasicTools;
import ipr.IprEntry;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import modes.Predict;


/**
 * writes data in libsvm-file-format
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
public class LibSVMOutfileWriter {

	// clean version
	public int[] write(List<String> domainIDs, Map<String, IprEntry> seq2domain, String outfile) {
		
		List<String> libsvmFeatureTable = new ArrayList<String>();
		List<String> excludedSequences = new ArrayList<String>();
		Map<Integer, List<String>> feat2seq = new HashMap<Integer, List<String>>();

		// check if feature file for superclass prediction shall be generated
		boolean superclassFeatures = false;
		String firstKey = seq2domain.keySet().iterator().next();
		if (seq2domain.get(firstKey).superclass != null) {
			superclassFeatures = true;
		}
		
		int[] numFeatureVectors = null;
		if (superclassFeatures) {
			numFeatureVectors = new int[] {0,0,0,0,0};
		} else {
			numFeatureVectors = new int[] {0,0};
		}
		
		String label;
		
		try {
			BufferedWriter bw_libsvmfile = new BufferedWriter(new FileWriter(new File(outfile)));

			for (String currSeq : seq2domain.keySet()) {
				
				IprEntry curr_entry = seq2domain.get(currSeq);
				if (curr_entry.isTF) {
					
					// Superclass prediction
					if (superclassFeatures) {
						label = Integer.toString(curr_entry.superclass);

					// TF prediction (TF)
					} else {
						label = "+1";
					}
					
				// TF prediction (non-TF)
				} else {
					label = "-1";
				}
				
				String fvector = Predict.createIPRvector(curr_entry.domain_ids, domainIDs, Predict.featureOffset);
				String line = label + " " + fvector + "\n";
				
				// save names of sequences for which no feature vector could be generated as no InterPro domains were found
				if (fvector.isEmpty()) {
					excludedSequences.add(currSeq);
					continue;
				}
				
				if (currSeq.equals("GL2_P46607_MatBase")) {
					System.out.println("Superclass: " + curr_entry.superclass);
				}
				
				// save mapping from feature vectors to (multiple) sequence IDs
				int featVecIdx = libsvmFeatureTable.indexOf(line);
				if (featVecIdx != -1) {
					List<String> currSeqIDs = feat2seq.get(featVecIdx);
					currSeqIDs.add(currSeq);
					feat2seq.put(featVecIdx, currSeqIDs);
					continue;
				
				} else {
					libsvmFeatureTable.add(line);
					bw_libsvmfile.write(line);
					
					List<String> currSeqIDs = new ArrayList<String>();
					currSeqIDs.add(currSeq);
					feat2seq.put(feat2seq.size(), currSeqIDs);
				}

				if (curr_entry.isTF) {
					if (superclassFeatures) {
						numFeatureVectors[curr_entry.superclass]++;
					} else {
						numFeatureVectors[0]++;
					}
				} else {
					numFeatureVectors[1]++;
				}
			}
			bw_libsvmfile.flush();
			bw_libsvmfile.close();
		}
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while writing output file");
		}
		
		// write Sequence IDs for each feature vector to file
		String namesFile = outfile.replace(".txt", "_names.txt");
		List<String[]> namesList = new ArrayList<String[]>();
		for (int i=0; i<feat2seq.size(); i++) {
			namesList.add(feat2seq.get(i).toArray(new String[]{}));
		}
		BasicTools.writeSplittedList2File(namesList, namesFile);
		
		// write names of proteins for which no feature vectors were generated to file
		String excludedNamesFile = outfile.replace(".txt", "_excluded.txt");
		if (!excludedSequences.isEmpty()) {
			System.out.println("Excluded " + excludedSequences.size() + " protein sequences with empty feature vectors.");
			BasicTools.writeList2File(excludedSequences, excludedNamesFile);
		}
		return numFeatureVectors;
	}
}
