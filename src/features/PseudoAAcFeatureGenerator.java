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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.stat.descriptive.summary.Sum;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class PseudoAAcFeatureGenerator {
	
	public double weight = 0.05;
    public int lambdaMax = 10;
    
    private static final String[] AAcs = {"A", "C", "D", "E", "F", "G", "H", "I", "K", "L", "M", "N", "P", "Q", "R", "S", "T", "V", "W", "Y"};
	private static final String aa_attr = "aa_attr.txt";
	private static Map<String, Double> aac2hpho = new HashMap<String, Double>();
	private static Map<String, Double> aac2hphi = new HashMap<String, Double>();
	private static Map<String, Double> aac2mass = new HashMap<String, Double>();
	
	protected String fastaFile;
	protected String featureFile;
	protected boolean superPred;
	
	protected Map<String, Integer> seq2label = new HashMap<String, Integer>();
	protected Map<String, String> sequences = new HashMap<String, String>();
	protected Map<String, double[]> features = new HashMap<String, double[]>();
	
	public PseudoAAcFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
	}
	
	public PseudoAAcFeatureGenerator(String fastaFile, String featureFile, boolean superPred, double weight, int lambdaMax) {
		
		this(fastaFile, featureFile, superPred);
		this.weight = weight;
		this.lambdaMax = lambdaMax;
	}
	
	public void generatePseudoAAcFeatures() {
		
		preparePseudoAAcCalculator();
		calcPseudoAAcFeatures();
		writeFeatureFile();
	}
	
	// reads sequences, labels, and physicochemical properties of amino acids
	private void preparePseudoAAcCalculator() {

		// read sequences and labels
		sequences = BasicTools.readFASTA(fastaFile, true);
		seq2label = DomainFeatureGenerator.getLabelsFromFastaHeaders(sequences.keySet(), superPred, false);
		
		// remove invalid amino acid symbols contained in sequences
		for (String seqID: sequences.keySet()) {
			sequences.put(seqID, sequences.get(seqID).replaceAll("[BJOUXZ]", ""));
		}
		
		// read amino acid attributes from annotation file
		readAAcAttributes();
	}	
	
	private static void readAAcAttributes() {
		
		List<String> lines = BasicTools.readResource2List(aa_attr);
		List<String[]> splittedLines = new ArrayList<String[]>();
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (!line.isEmpty()) splittedLines.add(line.split("\t"));
		}
		
		String[] aminoAcids = new String[20];
		double[] hphoValues = new double[20];
		double[] hphiValues = new double[20];
		double[] massValues = new double[20];
		
		for (int i=0; i<20; i++) {
			aminoAcids[i] = splittedLines.get(0)[i+1];
			hphoValues[i] = Double.parseDouble(splittedLines.get(1)[i+1]); 
			hphiValues[i] = Double.parseDouble(splittedLines.get(2)[i+1]); 
			massValues[i] = Double.parseDouble(splittedLines.get(3)[i+1]); 
		}
		
		// convert amino acid attributes to z-scores
		hphoValues = BasicTools.transform2zScores(hphoValues);
		hphiValues = BasicTools.transform2zScores(hphiValues);
		massValues = BasicTools.transform2zScores(massValues);
		
		// save amino acid properties in global Maps
		for (int i=0; i<20; i++) {
			aac2hpho.put(aminoAcids[i], hphoValues[i]);
			aac2hphi.put(aminoAcids[i], hphiValues[i]);
			aac2mass.put(aminoAcids[i], massValues[i]);
		}
	}
	
	private void calcPseudoAAcFeatures() {
		
		for (String seqID: sequences.keySet()) {
			features.put(seqID, calcPseudoAAcFeatureVector(sequences.get(seqID)));
		}
	}
	
	private double[] calcPseudoAAcFeatureVector(String seq) {
		
		double[] normalFeatures = calcAAcFrequencies(seq);
		double[] pseudoFeatures = calcPseudoAAcScores(seq, lambdaMax);
		
		Sum sum = new Sum();
		double denom = sum.evaluate(normalFeatures) + weight * sum.evaluate(pseudoFeatures);
		
		double[] featureVector = new double[normalFeatures.length + pseudoFeatures.length];
		for (int i=0; i<normalFeatures.length; i++) {
			featureVector[i] = normalFeatures[i] / denom;
		}
		for (int i=0; i<pseudoFeatures.length; i++) {
			featureVector[i+normalFeatures.length] = pseudoFeatures[i] / denom;
		}
        return featureVector;
    }
	
	private static double[] calcAAcFrequencies(String seq) {
		
		double[] aacFreq = new double[AAcs.length]; 
		for (int i=0; i<AAcs.length; i++) {
			aacFreq[i] = ((double) seq.length() - seq.replace(AAcs[i], "").length()) / seq.length();
		}
		return(aacFreq);
	}
   
	private static double calcPseudoAAcScore(String seq, int lambda) {
		
		double score = 0;
		for (int i=0; i<seq.length()-lambda; i++) {
			score += calcPairwisePseudoAAcScore(seq.substring(i, i+1), seq.substring(i+lambda,i+lambda+1));
		}
		return(score/(seq.length()-lambda));
	}

	private static double[] calcPseudoAAcScores(String seq, int lambdaMax) {
		
		double[] scores = new double[lambdaMax];
		for (int lambda=1; lambda<=lambdaMax; lambda++) {
			scores[lambda-1] = calcPseudoAAcScore(seq, lambda);
		}		 
		return(scores);
	}
	
	private static double calcPairwisePseudoAAcScore(String aac1, String aac2) {
		
		double hphoTerm = Math.pow(aac2hpho.get(aac1) - aac2hpho.get(aac2), 2);
		double hphiTerm = Math.pow(aac2hphi.get(aac1) - aac2hphi.get(aac2), 2);
		double massTerm = Math.pow(aac2mass.get(aac1) - aac2mass.get(aac2), 2);
	
		return(1.0/3 * (hphoTerm + hphiTerm + massTerm));
	}
	
	private void writeFeatureFile() {
		
		List<String> libSVMfeatures = new ArrayList<String>();
		List<String> sequenceNames = new ArrayList<String>();
		
		for (String seqID: features.keySet()) {
			
			sequenceNames.add(seqID);
			double[] featureVector = features.get(seqID);
			int label = seq2label.get(seqID);

			// write pseudo amino acids feature vector in libsvm format
			StringBuffer featureString = new StringBuffer("" + label);
			for (int idx=1; idx<=featureVector.length; idx++) {
				if (featureVector[idx-1] != 0) {
					featureString.append(" " + idx + (":" + featureVector[idx-1]).replaceFirst("\\.0$", ""));
				}
			}
			libSVMfeatures.add(featureString.toString());
		}
		BasicTools.writeList2File(libSVMfeatures, featureFile);
		BasicTools.writeList2File(sequenceNames, featureFile.replace(".txt", "_names.txt"));
	}
}

