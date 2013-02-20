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
	
	private static void generatePseudoAAcFeaturesTFpred(String fastaFile, String featureDir) {
		generatePseudoAAcFeatures(fastaFile, featureDir, false);
	}
	
	private static void generatePseudoAAcFeaturesSuperPred(String fastaFile, String featureDir) {
		generatePseudoAAcFeatures(fastaFile, featureDir, true);
	}
	
	private static void generatePseudoAAcFeatures(String fastaFile, String featureDir, boolean superPred) {
		String featureFile = featureDir + FeatureType.pseudo.featureFileName;
		PseudoAAcFeatureGenerator featureGenerator = new PseudoAAcFeatureGenerator(fastaFile, featureFile, superPred);
		featureGenerator.generatePseudoAAcFeatures();
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
	
	private static void generatePercentileFeaturesTFpred(String fastaFile, String featureDir) {
		generatePercentileFeatures(fastaFile, featureDir, false);
	}
	
	private static void generatePercentileFeaturesSuperPred(String fastaFile, String featureDir) {
		generatePercentileFeatures(fastaFile, featureDir, true);
	}

	private static void generatePercentileFeatures(String fastaFile, String featureDir, boolean superPred) {
		String featureFile = featureDir + FeatureType.percentile.featureFileName;
		PercentileFeatureGenerator featureGenerator = new PercentileFeatureGenerator(fastaFile, featureFile, superPred);
		featureGenerator.generateFeatures();
	}
	
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {

		// set paths to input files
		String dataDir = "/rahome/eichner/projects/tfpredict/data/";
		
		String fastaFileTF =  dataDir + "tf_pred/fasta_files/latest/TF.fasta"; 
		String fastaFileNonTF =  dataDir + "tf_pred/fasta_files/latest/NonTF.fasta"; 
		String fastaFileTFnonTF =  dataDir + "tf_pred/fasta_files/latest/TFnonTF.fasta"; 

		String interproResultFileTF =  dataDir + "tf_pred/interpro_files/latest/TF.fasta.out"; 
		String interproResultFileNonTF = dataDir + "tf_pred/interpro_files/latest/NonTF.fasta.out";
		
		String tfFeatureDir = dataDir + "tf_pred/feature_files/latest/";
		String superFeatureDir = dataDir + "super_pred/feature_files/latest/";
		
	    // generate domain-based features
		//generateDomainFeaturesTFpred(interproResultFileTF, interproResultFileNonTF, tfFeatureDir);
		generateDomainFeaturesSuperPred(fastaFileTF, interproResultFileTF, superFeatureDir);
		
		// generate k-mer-based features
		//generateKmerFeaturesTFpred(fastaFileTFnonTF, tfFeatureDir);
		//generateKmerFeaturesSuperPred(fastaFileTF, superFeatureDir);
		
		// generate PSSM-based features
		//generatePssmFeaturesTFpred(fastaFileTFnonTF, tfFeatureDir);
		//generatePssmFeaturesSuperPred(fastaFileTF, superFeatureDir);

		// generate pseudo-amino-acid features
		//generatePseudoAAcFeaturesTFpred(fastaFileTFnonTF, tfFeatureDir);
		//generatePseudoAAcFeaturesSuperPred(fastaFileTF, superFeatureDir);
		
		// generate percentile features
		//generatePercentileFeaturesTFpred(fastaFileTFnonTF, tfFeatureDir);
		//generatePercentileFeaturesSuperPred(fastaFileTF, superFeatureDir);
	}
}
