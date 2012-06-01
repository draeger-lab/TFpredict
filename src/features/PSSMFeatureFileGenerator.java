package features;

import io.BasicTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PSSMFeatureFileGenerator {

	private static String path2BLAST = "/opt/blast/latest/";
	
	// creates database and returns path to created database
	private String createDatabase4BLAST(String fastaFile) {

		String dbName = new File(fastaFile).getName().replace(".fasta", ".db");
	
		String cmd = path2BLAST + "bin/makeblastdb -in " + fastaFile  + " -out " + path2BLAST + "db/" + dbName + " -dbtype prot";
		BasicTools.runCommand(cmd, false);
		
		return (path2BLAST + "db/" + dbName);
	}
	
	
	private ArrayList<String[]> runPSIBLAST(String fastaFile, String database) {
		
		int numIter = 2;
		
		// prepare temporary files for PSI-BLAST output
		String outfileHits = null;
		String outfilePSSM = null;
		try {
			outfileHits = File.createTempFile("psiblast_", "_hits.txt").getAbsolutePath();
			outfilePSSM = File.createTempFile("psiblast_", "_pssm.txt").getAbsolutePath();
		
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// run PSI-BLAST
		String cmd = path2BLAST + "bin/psiblast -query " + fastaFile + " -num_iterations " + numIter + " -db " + database + " -out " + outfileHits + " -out_ascii_pssm " + outfilePSSM;
		System.out.println(cmd);
		BasicTools.runCommand(cmd, false);
		
		// read PSI-BLAST output from temporary files
		ArrayList<String[]> pssmTable = BasicTools.readFile2ListSplitLines(outfilePSSM, true);
		ArrayList<int[]> pssm = new ArrayList<int[]>();
		StringBuffer seq = new StringBuffer();
		
		for (int i=2; i<pssmTable.size(); i++) {
			seq.append(pssmTable.get(i)[2]);
			int[] PSSMline = new int[20];
			for (int j=3; j<23; j++) {
				PSSMline[j-3] = Integer.parseInt(pssmTable.get(i)[j]); 
			}
			pssm.add(PSSMline);
		}
		
		return pssmTable;
	}
	
	public static void main(String[] args) {
		
		/*
		 *  Generation of feature files
		 */
		
		String dataDir = "/rahome/eichner/projects/tfpredict/data/";
		
		// generate feature file for TF prediction
		String fastaFileTF =  dataDir + "tf_pred/fasta_files/TF.fasta"; 
		String fastaFileNonTF = dataDir + "tf_pred/fasta_files/NonTF.fasta";
		// String tfFeatureFile = dataDir + "tf_pred/feature_files/latest/pssm_featurefile.txt";
		String testInputFile = "/rahome/eichner/projects/tfpredict/versions/TFpredict_v1.1/test_seq.fasta";
		
		PSSMFeatureFileGenerator tfFeatFileGenerator = new PSSMFeatureFileGenerator();
		String tfDatabase = tfFeatFileGenerator.createDatabase4BLAST(fastaFileTF);
		String superDatabase = tfFeatFileGenerator.createDatabase4BLAST(fastaFileNonTF);
		
		tfFeatFileGenerator.runPSIBLAST(testInputFile, tfDatabase);
	}
	
}
