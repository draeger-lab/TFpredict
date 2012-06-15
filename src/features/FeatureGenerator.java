package features;

public class FeatureGenerator {

	private static int kMin = 3;
	private static int kMax = 3;
	
	private enum FeatureType {
		
		domain("domain_features.txt"),
		kmer("kmer_features.txt"),
		percentile("percentile_features.txt"),
		pssm("pssm_features.txt"),
		pseudo("pseudo_features.txt");
		
		public String featureFileName;
		
		private FeatureType(String featFileName) {
			this.featureFileName = featFileName;
		}
	}
	
	/*
	 *  Domain-based features
	 */

	private static void generateDomainFeaturesTFpred(String interproResultFileTF, String interproResultFileNonTF, String featureDir) {
		
		String featureFile = featureDir + FeatureType.domain.featureFileName;
		TFpredDomainFeatureGenerator featureGenerator = new TFpredDomainFeatureGenerator(interproResultFileTF, interproResultFileNonTF, featureFile);
		featureGenerator.writeFeatureFile();
	}
	
	private static void generateDomainFeaturesSuperPred(String fastaFile, String interproResultFile, String featureDir) {
		
		String featureFile = featureDir + FeatureType.domain.featureFileName;
		SuperPredDomainFeatureGenerator featureGenerator = new SuperPredDomainFeatureGenerator(fastaFile, interproResultFile, featureFile);
		featureGenerator.writeFeatureFile();	
	}
	
	/*
	 *  k-mer-based features
	 */
	
	private static void generateKmerFeaturesTFpred(String fastaFile, String featureDir) {
		generateKmerFeatures(fastaFile, featureDir, false);
	}
	
	private static void generateKmerFeaturesSuperPred(String fastaFile, String featureDir) {
		generateKmerFeatures(fastaFile, featureDir, true);
	}
	
	private static void generateKmerFeatures(String fastaFile, String featureDir, boolean superPred) {
		String featureFile = featureDir + FeatureType.kmer.featureFileName;
		KmerFeatureGenerator featureGenerator = new KmerFeatureGenerator(fastaFile, featureFile, superPred, kMin, kMax);
		featureGenerator.generateKmerFeatures();
	}
	
	/*
	 *  Pseudo amino acid composition features
	 */
	
	private static void generatePseudoFeaturesTFpred(String fastaFile, String featureDir) {
		//TODO
	}
	
	private static void generatePseudoFeaturesSuperPred(String fastaFile, String featureDir) {
		//TODO
	}
	
	/*
	 *  PSSM-based features
	 */
	
	private static void generatePssmFeaturesTFpred(String fastaFile, String featureDir) {
		generatePssmFeatures(fastaFile, featureDir, false);
	}
	
	private static void generatePssmFeaturesSuperPred(String fastaFile, String featureDir) {
		generatePssmFeatures(fastaFile, featureDir, true);
	}
	
	private static void generatePssmFeatures(String fastaFile, String featureDir, boolean superPred) {
		String featureFile = featureDir + FeatureType.pssm.featureFileName;
		PSSMFeatureGenerator featureGenerator = new PSSMFeatureGenerator(fastaFile, featureFile, superPred);
		featureGenerator.generateFeatures();
	}
	
	/*
	 *  Percentile features
	 */
	
	private static void generatePercentileFeaturesTFpred(String fastaFile, String featureFile) {
		//TODO
	}
	
	private static void generatePercentileFeaturesSuperPred(String fastaFile, String featureFile) {
		//TODO
	}

	
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {

		// set paths to input files
		String dataDir = "/rahome/eichner/projects/tfpredict/data/";
		
		String fastaFileTFnonTF =  dataDir + "tf_pred/fasta_files/TFnonTF.fasta"; 
		String fastaFileSuper =  dataDir + "super_pred/fasta_files/superclassTF.fasta"; 

		String interproResultFileTF =  dataDir + "tf_pred/interpro_files/TF.fasta.out"; 
		String interproResultFileNonTF = dataDir + "tf_pred/interpro_files/NonTF.fasta.out";
		String interproResultFileSuper =  dataDir + "super_pred/interpro_files/superclassTF.fasta.out"; 
		
		String tfFeatureDir = dataDir + "tf_pred/feature_files/latest/";
		String superFeatureDir = dataDir + "super_pred/feature_files/latest/";
		
	    // generate domain-based features
		generateDomainFeaturesTFpred(interproResultFileTF, interproResultFileNonTF, tfFeatureDir);
		generateDomainFeaturesSuperPred(fastaFileSuper, interproResultFileSuper, superFeatureDir);
		
		// generate k-mer-based features
		generateKmerFeaturesTFpred(fastaFileTFnonTF, tfFeatureDir);
		generateKmerFeaturesSuperPred(fastaFileSuper, superFeatureDir);
		
		// generate PSSM-based features
		generatePssmFeaturesTFpred(fastaFileTFnonTF, tfFeatureDir);
		generatePssmFeaturesSuperPred(fastaFileSuper, superFeatureDir);

		// generate percentile features
		generatePercentileFeaturesTFpred(fastaFileTFnonTF, tfFeatureDir);
		generatePercentileFeaturesSuperPred(fastaFileSuper, superFeatureDir);
		
	}
}
