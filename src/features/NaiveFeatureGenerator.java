package features;

import java.util.Map;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class NaiveFeatureGenerator extends BLASTfeatureGenerator {

	/**
	 * 
	 * @param fastaFile
	 * @param featureFile
	 * @param superPred
	 */
	public NaiveFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		super(fastaFile, featureFile, superPred);
		this.pssmFeat = false;
		this.naiveFeat = true;
	}
	
	/* (non-Javadoc)
	 * @see features.BLASTfeatureGenerator#computeFeaturesFromBlastResult()
	 */
	protected void computeFeaturesFromBlastResult() {
		
		for (String seqID: hits.keySet()) {
			
			// obtain class of best hit in sequence database
			Map<String, Double> currHits = hits.get(seqID);
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
