package features;

import io.BasicTools;

public class PSSMFeatureGenerator extends BLASTfeatureGenerator {
	
	public PSSMFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
		this.pssmFeat = true;
		this.naiveFeat = false;
	}
	
	protected void computeFeaturesFromBlastResult() {

		for (String seqID: pssms.keySet()) {
			
			String seq = sequences.get(seqID);
			int[][] currPSSM = pssms.get(seqID);
			double[] pssmFeatVec = new double[400];
			
			for (int i=0; i<aminoAcids.length; i++) {
				int[] indices = BasicTools.getAllIndicesOf(seq, aminoAcids[i]);
				for (int j=0; j<aminoAcids.length; j++) {
					int sum = 0;
					for (int k=0; k<indices.length; k++) {
						sum += currPSSM[k][j];
					}
					int featIdx = i * 20 + j;
					pssmFeatVec[featIdx] = scalePSSMscore(sum);
				}
			}
			features.put(seqID, pssmFeatVec);
		}
	}
	
	
	private static double scalePSSMscore(int pssmScore) {
		
		double scaledScore = 1 / (1 + Math.exp(-pssmScore));
		
		return scaledScore;
	}
}
