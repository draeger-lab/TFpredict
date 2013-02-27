/*  
 * $Id$
 * $URL$
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2013 Center for Bioinformatics Tuebingen (ZBIT),
 * University of Tuebingen by Johannes Eichner, Florian Topf, Andreas Draeger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package features;

import io.BasicTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public abstract class BLASTfeatureGenerator {

	protected static final String[] aminoAcids = new String[]{"A", "R", "N", "D", "C", "Q", "E", "G", "H", "I", "L", "K", "M", "F", "P", "S", "T", "W", "Y", "V"};
	
	protected String path2BLAST;
	
	protected static boolean silent = false;
	
	protected boolean pssmFeat;
	protected boolean naiveFeat;
	protected boolean superPred;
	
	protected String fastaFile;
	protected String featureFile;
	protected String database;
	
	protected Map<String, Integer> seq2label = new HashMap<String, Integer>();
	protected Map<String, String> sequences = new HashMap<String, String>();
	
	protected Map<String, Map<String, Double>> hits = new HashMap<String, Map<String, Double>>();
	protected Map<String, int[][]> pssms = new HashMap<String, int[][]>();
	protected Map<String, double[]> features = new HashMap<String, double[]>();
	
	static {
		java.util.Locale.setDefault(java.util.Locale.ENGLISH);
	}
	
	public void generateFeatures() {
		
		preparePsiBlast();
		runPsiBlast();
		computeFeaturesFromBlastResult();
		writeFeatureFile();
	}
	
	// reads sequences, labels, and creates database for PsiBlast
	protected void preparePsiBlast() {

		// read sequences and labels
		sequences = BasicTools.readFASTA(fastaFile, true);
		seq2label = DomainFeatureGenerator.getLabelsFromFastaHeaders(sequences.keySet(), superPred, false);
		
		// create database
		String dbName = new File(fastaFile).getName().replace(".fasta", ".db");
		String cmd = path2BLAST + "bin/makeblastdb -in " + fastaFile  + " -out " + path2BLAST + "db/" + dbName + " -dbtype prot";
		BasicTools.runCommand(cmd, false);
		
		database = path2BLAST + "db/" + dbName;
	}
	
	
	protected void runPsiBlast() {
		
		int numIter = 1;
		if (pssmFeat) {
			numIter = 2;
		}
		
		int seqCnt = 1;
		for (String seqID: sequences.keySet()) {
		
			// prepare temporary files for PSI-BLAST output
			String tempFilePrefix = "";
			File localTempDir = new File("/rascratch/user/eichner/tmp/");

			try {
				if (localTempDir.exists()) {
					tempFilePrefix = File.createTempFile("psiblast_", "", localTempDir).getAbsolutePath();
				
				// use system default directory for temporary files
				} else {
					tempFilePrefix = File.createTempFile("psiblast_", "").getAbsolutePath();
				}
			
			} catch (IOException e) {
				e.printStackTrace();
			}
			String infileFasta = tempFilePrefix + "_fasta.txt";
			String outfileHits = tempFilePrefix + "_hits.txt";
			String outfilePSSM = tempFilePrefix + "_pssm.txt";
			
			BasicTools.writeFASTA(seqID, sequences.get(seqID), infileFasta);
			
			// run PSI-BLAST current sequence
			if (!silent) System.out.println("Processing sequence: " + seqID + "\t(" + seqCnt++ + "/" + sequences.size() + ")");
			
			if (pssmFeat) {

				pssms.put(seqID, getPsiBlastPSSM(infileFasta, database, outfileHits, outfilePSSM, numIter).toArray(new int[][]{}));
			
			} else {
				hits.put(seqID, getPsiBlastHits(infileFasta, database, outfileHits, numIter));
			}
 		}
	}
	
	
	private Map<String, Double> getPsiBlastHits(String fastaFile, String database, String hitsOutfile, int numIter) {	

		String cmd = path2BLAST + "bin/psiblast -query " + fastaFile + " -num_iterations " + numIter + " -db " + database + " -out " + hitsOutfile;
		BasicTools.runCommand(cmd, false);
		
		// read PSI-BLAST output from temporary files
		ArrayList<String> hitsTable = BasicTools.readFile2List(hitsOutfile, false);
		Map<String, Double> currHits = new HashMap<String, Double>();
		
		int lineIdx = 0;
		String line;
		
		// skip header
		while (lineIdx < hitsTable.size() && !(line = hitsTable.get(lineIdx)).startsWith("Sequences producing significant alignments")) {
			lineIdx++;
		}
		lineIdx = lineIdx + 2; 

		while (lineIdx < hitsTable.size() && !hitsTable.get(lineIdx).isEmpty() && !(line = hitsTable.get(lineIdx)).startsWith(">")) {
			
			StringTokenizer strtok = new StringTokenizer(line);
			String hitID = strtok.nextToken();
			String nextToken;
			while ((nextToken = strtok.nextToken()).startsWith("GO:"));   // skip GO terms in non-TF headers
			double hitScore = Double.parseDouble(nextToken); 

			currHits.put(hitID, hitScore);
			lineIdx++;
		}
		return currHits;
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
	
	protected abstract void computeFeaturesFromBlastResult();
	
	/**
	 * 
	 * @param fastaFile
	 * @param featureFile
	 * @param superPred
	 */
	public BLASTfeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		path2BLAST = System.getenv("BLAST_DIR");
		
		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;
	}
	
	protected void writeFeatureFile() {
		
		ArrayList<String> libSVMfeatures = new ArrayList<String>();
		ArrayList<String> sequenceNames = new ArrayList<String>();
		
		for (String seqID: features.keySet()) {
			
			sequenceNames.add(seqID);
			double[] featureVector = features.get(seqID);
			int label = seq2label.get(seqID);
			StringBuffer featureString = new StringBuffer("" + label);
			for (int i=0; i<featureVector.length; i++) {
				if (!naiveFeat && featureVector[i] == 0) continue;     // skip features with value zero
				featureString.append( " " + (i+1) + ":" + (featureVector[i] + "").replaceFirst("\\.0$", ""));
			}
			libSVMfeatures.add(featureString.toString());
		}
		BasicTools.writeArrayList2File(libSVMfeatures, featureFile);
		BasicTools.writeArrayList2File(sequenceNames, featureFile.replace(".txt", "_names.txt"));
	}
}
