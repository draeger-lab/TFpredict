package features;

import io.BasicTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class PSSMFeatureGenerator {
	
	private static final String[] aminoAcids = new String[]{"A", "R", "N", "D", "C", "Q", "E", "G", "H", "I", "L", "K", "M", "F", "P", "S", "T", "W", "Y", "V"};
	private static final String path2BLAST = "/opt/blast/latest/";
	
	private static boolean silent = false;
	
	private boolean superPred;
	private String fastaFile;
	private String featureFile;
	private String database;
	
	HashMap<String, Integer> seq2label = new HashMap<String, Integer>();
	HashMap<String, String> sequences = new HashMap<String, String>();
	private HashMap<String, int[][]> pssms = new HashMap<String, int[][]>();
	private HashMap<String, double[]> pssmFeatures = new HashMap<String, double[]>();
	
	public PSSMFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
	}
	
	public void generatePSSMfeatures() {
		
		preparePsiBlast();
		runPsiBlast();
		computeFeaturesFromPSSMs();
		writeFeatureFile();
	}
	
	// reads sequences, labels, and creates database for PsiBlast
	private void preparePsiBlast() {

		// read sequences and labels
		sequences = BasicTools.readFASTA(fastaFile, true);
		seq2label = DomainFeatureGenerator.getLabelsFromFastaHeaders(sequences.keySet(), superPred, false);
		
		// create database
		String dbName = new File(fastaFile).getName().replace(".fasta", ".db");
		String cmd = path2BLAST + "bin/makeblastdb -in " + fastaFile  + " -out " + path2BLAST + "db/" + dbName + " -dbtype prot";
		BasicTools.runCommand(cmd, false);
		
		database = path2BLAST + "db/" + dbName;
	}
	
	
	private void runPsiBlast() {
		
		int numIter = 2;
		int seqCnt = 1;
		
		for (String seqID: sequences.keySet()) {
		
			// prepare temporary files for PSI-BLAST output
			String tempFilePrefix = "";
			try {
				tempFilePrefix = File.createTempFile("psiblast_", "").getAbsolutePath();
			
			} catch (IOException e) {
				e.printStackTrace();
			}
			String infileFasta = tempFilePrefix + "_fasta.txt";
			String outfileHits = tempFilePrefix + "_hits.txt";
			String outfilePSSM = tempFilePrefix + "_pssm.txt";
			
			BasicTools.writeFASTA(seqID, sequences.get(seqID), infileFasta);
			
			// run PSI-BLAST current sequence
			if (!silent) System.out.println("Processing sequence: " + seqID + "\t(" + seqCnt++ + "/" + sequences.size() + ")");
			pssms.put(seqID, getPsiBlastPSSM(infileFasta, database, outfileHits, outfilePSSM, numIter).toArray(new int[][]{}));
 		}
	}
	
	private void computeFeaturesFromPSSMs() {

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
			pssmFeatures.put(seqID, pssmFeatVec);
		}
	}
	
	private void writeFeatureFile() {
		
		ArrayList<String> libSVMfeatures = new ArrayList<String>();
		
		for (String seqID: pssmFeatures.keySet()) {
			
			double[] featureVector = pssmFeatures.get(seqID);
			int label = seq2label.get(seqID);
			StringBuffer featureString = new StringBuffer("" + label);
			for (int i=0; i<featureVector.length; i++) {
				featureString.append( " " + (i+1) + ":" + featureVector[i]);
			}
			libSVMfeatures.add(featureString.toString());
		}
		BasicTools.writeArrayList2File(libSVMfeatures, featureFile);
	}
	
	private static double scalePSSMscore(int pssmScore) {
		
		double scaledScore = 1 / (1 + Math.exp(-pssmScore));
		
		return scaledScore;
	}
		
	private ArrayList<int[]> getPsiBlastPSSM(String fastaFile, String database, String hitsOutfile, String pssmOutfile, int numIter) {	
		
		String cmd = path2BLAST + "bin/psiblast -query " + fastaFile + " -num_iterations " + numIter + " -db " + database + " -out " + hitsOutfile + " -out_ascii_pssm " + pssmOutfile;
		BasicTools.runCommand(cmd, false);

		// read PSI-BLAST output from temporary files
		ArrayList<String[]> pssmTable = BasicTools.readFile2ListSplitLines(pssmOutfile, true);
		ArrayList<int[]> pssm = new ArrayList<int[]>();

		for (int i=2; i<pssmTable.size(); i++) {

			// skip short lines after PSSM 
			if (pssmTable.get(i).length < 20) {
				continue;
			}

			String[] line = pssmTable.get(i);
			int[] PSSMrow = new int[20];
			for (int j=2; j<22; j++) {
				
				// line with missing blanks ?
				if (line.length < 44) {
					line = BasicTools.collapseStringArray(line, " ").replace("-", " -").replaceAll("\\s+", " ").split(" ");
				}
				// check if line was fixed
				if (line.length != 44) {
					System.out.println("Error. PSSM line could not be parsed.\nLine: " + BasicTools.collapseStringArray(line, " "));
					System.exit(0);
				}
				PSSMrow[j-2] = Integer.parseInt(line[j]);
			}
			pssm.add(PSSMrow);
		}
		return pssm;
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
		tfFeatureGenerator.generatePSSMfeatures();
	}
	
}
