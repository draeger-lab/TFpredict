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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class KmerFeatureGenerator {
	
	private static boolean silent = false;
	
	private boolean superPred;
	private String fastaFile;
	private String featureFile;
	private int kMin = 2;
	private int kMax = 2;
	private int minNumOcc = 1;
	
	private Map<String, Integer> seq2label = new HashMap<String, Integer>();
	private Map<String, String> sequences = new HashMap<String, String>();
	Map<String, Map<String, Integer>> seq2kmerCounts = new HashMap<String, Map<String, Integer>>();
	
	private List<String> allKmers = new ArrayList<String>();
	
	public KmerFeatureGenerator(String fastaFile, String featureFile, boolean superPred, int kMin, int kMax) {
		
		this(fastaFile, featureFile, superPred);
		this.kMin = kMin;
		this.kMax = kMax;
	}
	
	public KmerFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
	}

	
	public void generateKmerFeatures() {
		
		prepareKmerFeatureGenerator();
		getKmerCounts();
		getAllKmers();
		writeFeatureFile();
	}
	
	
	private void prepareKmerFeatureGenerator() {

		// read sequences and labels
		sequences = BasicTools.readFASTA(fastaFile, true);
		seq2label = DomainFeatureGenerator.getLabelsFromFastaHeaders(sequences.keySet(), superPred, false);
	}
	
	
	private void getKmerCounts() {	
		
		// compute k-mer features for all sequences
		for (String header: sequences.keySet()) {
			
			Map<String, Integer> fingerprint = getKmerCount(sequences.get(header), kMin, kMax);
			seq2kmerCounts.put(header, fingerprint);
		}
	}
	
	
	private Map<String, Integer> getKmerCount(String sequence, int kMin, int kMax) {
		
		Map<String, Integer> kmer2count = new HashMap<String, Integer>();
		
		for (int k=kMin; k<=kMax; k++) {
			for (int startIdx=0; startIdx <= (sequence.length() - k); startIdx++) {;	
				String currKmer = sequence.substring(startIdx, startIdx + k);
	
				if (kmer2count.containsKey(currKmer)) {
					kmer2count.put(currKmer, kmer2count.get(currKmer)+1);
				} else {
					kmer2count.put(currKmer, 1);
				}
			}
		}
		return kmer2count;
	}
	
	
	private void getAllKmers() {
		
		HashSet<String> kmerSet = new HashSet<String>();
		List<String> kmerList = new ArrayList<String>();
		
		for (String seqID: seq2kmerCounts.keySet()) {
			kmerSet.addAll(seq2kmerCounts.get(seqID).keySet());
			kmerList.addAll(seq2kmerCounts.get(seqID).keySet());
		}
		allKmers.addAll(kmerSet);
		Collections.sort(allKmers);
		
		// remove rare k-mers (if desired)
		if (minNumOcc > 1) {
			for (int i=(allKmers.size()-1); i>=0; i--) {
			
				int numOcc = Collections.frequency(kmerList, allKmers.get(i));
				if (numOcc < minNumOcc) {
					allKmers.remove(i);
				}
			}
		}
		
		if (!silent) System.out.println("Number of unique k-mers in all sequences: " + allKmers.size());
	}
	
	
	private void writeFeatureFile() {
		
		List<String> libSVMfeatures = new ArrayList<String>();
		List<String> proteinNames = new ArrayList<String>();
		
		int seqCnt= 0;
		for (String seqID: seq2kmerCounts.keySet()) {
			proteinNames.add(seqID);
			seqCnt++;
			if (seqCnt % 1000 == 0) {
				System.out.println("Processed " + seqCnt + " / " + seq2kmerCounts.size() + " sequences.");
			}

			int label = seq2label.get(seqID);
			StringBuffer featureString = new StringBuffer("" + label);
			Map<String, Integer> kmer2count = seq2kmerCounts.get(seqID);
			
			// sort feature indexes of contained k-mers
			int[] indices = new int[kmer2count.size()];
			int idxCnt = 0;
			for (String kmer: kmer2count.keySet()) {
				indices[idxCnt++] = allKmers.indexOf(kmer);
			}
			Arrays.sort(indices);
			
			// write k-mers as feature vector in libsvm format
			for (int idx: indices) {
			
				int count = kmer2count.get(allKmers.get(idx));
				
				featureString.append(" " + (idx+1) + ":" + count);
			}
			libSVMfeatures.add(featureString.toString());
		}

		BasicTools.writeList2File(libSVMfeatures, featureFile);
		BasicTools.writeList2File(proteinNames, featureFile.replace(".txt", "_names.txt"));
	}
}
