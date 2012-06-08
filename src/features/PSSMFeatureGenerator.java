package features;

import io.BasicTools;

public class PSSMFeatureGenerator extends BLASTfeatureGenerator {
	
	public PSSMFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
		this.pssmFeat = true;
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
	
	
	public static void main(String[] args) {
		
		/*
		 *  Generation of feature files
		 */
		
		String dataDir = "/rahome/eichner/projects/tfpredict/data/";
		
		// generate feature file for TF prediction
		String tfFastaFile = dataDir + "tf_pred/fasta_files/TFandNonTF.fasta"; 
		String tfFeatureFile = dataDir + "tf_pred/feature_files/latest/pssm_featurefile.txt";

		PSSMFeatureGenerator tfFeatureGenerator = new PSSMFeatureGenerator(tfFastaFile, tfFeatureFile, false);
		tfFeatureGenerator.generateFeatures();
		
		
		// generate feature file for superclass prediction
		String superFastaFile = dataDir + "super_pred/fasta_files/superclassTF.fasta"; 
		String superFeatureFile = dataDir + "super_pred/feature_files/latest/pssm_featurefile.txt";

		PSSMFeatureGenerator superFeatureGenerator = new PSSMFeatureGenerator(superFastaFile, superFeatureFile, false);
		superFeatureGenerator.generateFeatures();
	}
}
