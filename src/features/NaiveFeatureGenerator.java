package features;

import java.util.HashMap;

public class NaiveFeatureGenerator extends BLASTfeatureGenerator {

	public NaiveFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
		this.pssmFeat = false;
		this.naiveFeat = true;
	}
	
	protected void computeFeaturesFromBlastResult() {
		
		for (String seqID: hits.keySet()) {
			
			// obtain class of best hit in sequence database
			HashMap<String, Double> currHits = hits.get(seqID);
			double bestScore = 0;
			String bestHit = "";
			for (String hit: currHits.keySet()) {
				if (hit.equals(seqID)) {
					continue;
				} 
				if (currHits.get(hit) > bestScore) {
					bestHit = hit;
				}
			}
			// TODO: fix null pointer exception
			int predClass = seq2label.get(bestHit);
			features.put(seqID, new double[] {predClass});
		}
	}
}
