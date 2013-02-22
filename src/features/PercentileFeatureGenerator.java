package features;

import io.BasicTools;

import java.util.ArrayList;
import java.util.HashMap;

import modes.Predict;

import org.apache.commons.math.stat.descriptive.rank.Percentile;


public class PercentileFeatureGenerator extends BLASTfeatureGenerator {
	
	private static final int[] percentiles = new int[] {0,25,50,75,100};
	
	public PercentileFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
		this.pssmFeat = false;
		this.naiveFeat = false;
	}
	
	protected void computeFeaturesFromBlastResult() {
		
		for (String seqID: hits.keySet()) {
			
			HashMap<String, Double> currHits = hits.get(seqID); 
			double[] percFeatVec = null;
			
			if (superPred) {
				ArrayList<Double> scoresBasicDomain = new ArrayList<Double>();
				ArrayList<Double> scoresZincFinger = new ArrayList<Double>();
				ArrayList<Double> scoresHelixTurnHelix = new ArrayList<Double>();
				ArrayList<Double> scoresBetaScaffold = new ArrayList<Double>();
				ArrayList<Double> scoresOther = new ArrayList<Double>();
				for (String hit: currHits.keySet()) {
					if (hit.equals(seqID)) {
						continue;
					}
					if (seq2label.get(hit) == Predict.Basic_domain) {
						scoresBasicDomain.add(currHits.get(hit));
						
					} else if (seq2label.get(hit) == Predict.Zinc_finger) {
						scoresZincFinger.add(currHits.get(hit));
						
					} else if (seq2label.get(hit) == Predict.Helix_turn_helix) {
						scoresHelixTurnHelix.add(currHits.get(hit));
						
					} else if (seq2label.get(hit) == Predict.Beta_scaffold) {
						scoresBetaScaffold.add(currHits.get(hit));
						
					} else if (seq2label.get(hit) == Predict.Other) {
						scoresOther.add(currHits.get(hit));
						
					} else {
						System.out.println("Error. Invalid label associated with BLAST hit \"" + hit + "\": " + seq2label.get(hit));
						System.exit(0);
					}
				}
				
				
				double[] bitScoresBasicDomain = BasicTools.Double2double(scoresBasicDomain.toArray(new Double[]{}));
				double[] bitScoresZincFinger = BasicTools.Double2double(scoresZincFinger.toArray(new Double[]{}));
				double[] bitScoresHelixTurnHelix = BasicTools.Double2double(scoresHelixTurnHelix.toArray(new Double[]{}));
				double[] bitScoresBetaScaffold = BasicTools.Double2double(scoresBetaScaffold.toArray(new Double[]{}));
				double[] bitScoresOther = BasicTools.Double2double(scoresOther.toArray(new Double[]{}));
				if (bitScoresBasicDomain.length == 0) bitScoresBasicDomain = new double[] {0};
				if (bitScoresZincFinger.length == 0) bitScoresZincFinger = new double[] {0};
				if (bitScoresHelixTurnHelix.length == 0) bitScoresHelixTurnHelix = new double[] {0};
				if (bitScoresBetaScaffold.length == 0) bitScoresBetaScaffold = new double[] {0};
				if (bitScoresOther.length == 0) bitScoresOther = new double[] {0};
				
				Double[] percVector = new Double[percentiles.length*5];
			
				for (int p=0; p<percentiles.length; p++) {
					percVector[p] = BasicTools.computePercentile(bitScoresBasicDomain, percentiles[p]);
					percVector[p+5] = BasicTools.computePercentile(bitScoresZincFinger, percentiles[p]);
					percVector[p+10] = BasicTools.computePercentile(bitScoresHelixTurnHelix, percentiles[p]);
					percVector[p+15] = BasicTools.computePercentile(bitScoresBetaScaffold, percentiles[p]);
					percVector[p+20] = BasicTools.computePercentile(bitScoresOther, percentiles[p]);
				}
				percFeatVec = BasicTools.Double2double(percVector);
				
			} else {
				
				ArrayList<Double> scoresTF = new ArrayList<Double>();
				ArrayList<Double> scoresNonTF = new ArrayList<Double>();
				for (String hit: currHits.keySet()) {
					if (hit.equals(seqID)) {
						continue;
					}
					if (seq2label.get(hit) == 1) {
						scoresTF.add(currHits.get(hit));
						
					} else if (seq2label.get(hit) == -1) {
						scoresNonTF.add(currHits.get(hit));
						
					} else {
						System.out.println("Error. Invalid label associated with BLAST hit \"" + hit + "\": " + seq2label.get(hit));
						System.exit(0);
					}
				}
				
				Percentile percObj = new Percentile();
				double[] bitScoresTF = BasicTools.Double2double(scoresTF.toArray(new Double[]{}));
				double[] bitScoresNonTF = BasicTools.Double2double(scoresNonTF.toArray(new Double[]{}));
				
				Double[] percentilesTF = new Double[percentiles.length];
				Double[] percentilesNonTF = new Double[percentiles.length];
				for (int p=0; p<percentiles.length; p++) {
					percentilesTF[p] = percObj.evaluate(bitScoresTF, percentiles[p]);
					percentilesNonTF[p] = percObj.evaluate(bitScoresNonTF, percentiles[p]);
				}
				percFeatVec = BasicTools.Double2double(BasicTools.concatenateArrays(percentilesTF, percentilesNonTF));
			}
			features.put(seqID, percFeatVec);
		}
	}

	public static void main(String[] args) {
		
		/*
		 *  Generation of feature files
		 */
		
		String dataDir = "/rahome/eichner/projects/tfpredict/data/";
		
		// generate feature file for TF prediction
		String tfFastaFile = dataDir + "tf_pred/fasta_files/latest/TFandNonTF.fasta"; 
		String tfFeatureFile = dataDir + "tf_pred/feature_files/latest/percentile_featurefile.txt";

		PercentileFeatureGenerator tfFeatureGenerator = new PercentileFeatureGenerator(tfFastaFile, tfFeatureFile, false);
		tfFeatureGenerator.generateFeatures();
	}
}
