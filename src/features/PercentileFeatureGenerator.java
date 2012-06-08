package features;

import io.BasicTools;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.math.stat.descriptive.rank.Percentile;


public class PercentileFeatureGenerator extends BLASTfeatureGenerator {
	
	private static final int[] percentiles = new int[] {0,25,50,75,100};
	
	public PercentileFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
		this.pssmFeat = false;
	}
	
	protected void computeFeaturesFromBlastResult() {
		
		for (String seqID: hits.keySet()) {
			
			HashMap<String, Double> currHits = hits.get(seqID); 
			double[] percFeatVec = null;
			
			if (superPred) {
				// TODO
			} else {
				
				ArrayList<Double> scoresTF = new ArrayList<Double>();
				ArrayList<Double> scoresNonTF = new ArrayList<Double>();
				for (String hit: currHits.keySet()) {
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
		String tfFastaFile = dataDir + "tf_pred/fasta_files/TFandNonTF.fasta"; 
		String tfFeatureFile = dataDir + "tf_pred/feature_files/latest/percentile_featurefile.txt";

		PercentileFeatureGenerator tfFeatureGenerator = new PercentileFeatureGenerator(tfFastaFile, tfFeatureFile, false);
		tfFeatureGenerator.generateFeatures();
	}
}
