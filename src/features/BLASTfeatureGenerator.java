/*  
 * $Id: BLASTfeatureGenerator.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/features/BLASTfeatureGenerator.java $
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2014 Center for Bioinformatics Tuebingen (ZBIT),
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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import data.TrainingDataGenerator;

/**
 * 
 * @author Johannes Eichner
 * @author Andreas Dr&auml;ger
 * @version $Rev: 99 $
 * @since 1.0
 */

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
	
	private static final Logger logger = Logger.getLogger(BLASTfeatureGenerator.class.getName());

	private String pathForTmpDir;
	
	/**
	 * 
	 * @param fastaFile
	 * @param featureFile
	 * @param superPred
	 */
	
	public Map<String, double[]> getFeatures() {
		return features;
	}
	
	public BLASTfeatureGenerator() {}
	
	public BLASTfeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		path2BLAST = System.getenv("BLAST_DIR");
		if ((path2BLAST == null) || (path2BLAST.length() == 0)) {
			throw new RuntimeException("Cannot execute the BLAST tool, because no path to its local installation has been defined. Please define the environment variable BLAST_DIR to point to the BLAST directory on your OS and run this program again.");
		}

		this.fastaFile = fastaFile;
		this.featureFile = featureFile;
		this.superPred = superPred;

		// TODO
		this.pathForTmpDir = System.getProperty("user.dir") + "/resources/tmp/";
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
	
	protected HashMap<String, Integer> getSeq2LabelMapWithShortenedIDs() {
		
		HashMap<String, Integer> shortSeq2Label = new HashMap<String, Integer>();
		for (String seqID: seq2label.keySet()) {
			shortSeq2Label.put(seqID.split(" ")[0], seq2label.get(seqID));
		}
		return(shortSeq2Label);
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
			File localTempDir = new File(pathForTmpDir);

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
			logger.info("Processing sequence: " + seqID + "\t(" + seqCnt++ + "/" + sequences.size() + ")");
			
			String uniprotID = seqID.split("\\|")[TrainingDataGenerator.UniProtIDField];
			if (pssmFeat) {
				String pssmFile = localTempDir + "/psiblast_" + uniprotID + "_pssm.txt";
				boolean pssmFileExists = false;
				if (new File(pssmFile).exists()) {
					outfilePSSM = pssmFile;
					pssmFileExists = true;
				} 
				pssms.put(seqID, getPsiBlastPSSM(infileFasta, database, outfileHits, outfilePSSM, numIter, pssmFileExists).toArray(new int[][]{}));
			
			} else {
				String hitsFile = localTempDir + "/psiblast_" + uniprotID + "_hits.txt";
				boolean hitsFileExists = false;
				if (new File(hitsFile).exists()) {
					outfileHits = hitsFile;
					hitsFileExists = true;
				} 
				hits.put(seqID, getPsiBlastHits(infileFasta, database, outfileHits, numIter, hitsFileExists));
			}
 		}
	}
	
	
	private Map<String, Double> getPsiBlastHits(String fastaFile, String database, String hitsOutfile, int numIter, boolean useExistingHitsFile) {	
		
		if (! useExistingHitsFile) {
			String cmd = path2BLAST + "bin/psiblast -query " + fastaFile + " -num_iterations " + numIter + " -db " + database + " -out " + hitsOutfile;
			BasicTools.runCommand(cmd, false);
		}
		
		// read PSI-BLAST output from temporary files
		List<String> hitsTable = BasicTools.readFile2List(hitsOutfile, false);
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
			// correct wrong UniProt ID for T03281 in factor.dat
			if (hitID.contains("|41817|TF|")) {
				hitID = hitID.replace("|41817|TF|", "|P41817|TF|");
			}
			String nextToken = null;
			while (strtok.hasMoreTokens() && (nextToken = strtok.nextToken()).startsWith("GO:"));  // skip GO terms in non-TF headers
			double hitScore = Double.parseDouble(nextToken); 
			currHits.put(hitID, hitScore);
			lineIdx++;
		}
		return currHits;
	}
	
	

	private List<int[]> getPsiBlastPSSM(String fastaFile, String database, String hitsOutfile, String pssmOutfile, int numIter, boolean useExistingPssmFile) {	
		
		if (! useExistingPssmFile) {
			String cmd = path2BLAST + "bin/psiblast -query " + fastaFile + " -num_iterations " + numIter + " -db " + database + " -out " + hitsOutfile + " -out_ascii_pssm " + pssmOutfile;
			BasicTools.runCommand(cmd, false);
		}

		// read PSI-BLAST output from temporary files
		List<String[]> pssmTable = BasicTools.readFile2ListSplitLines(pssmOutfile, true);
		List<int[]> pssm = new ArrayList<int[]>();

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
	 */
	protected void writeFeatureFile() {
		List<String> libSVMfeatures = new ArrayList<String>();
		List<String> sequenceNames = new ArrayList<String>();
		
		for (String seqID: features.keySet()) {

			double[] featureVector = features.get(seqID);
			int label = seq2label.get(seqID);
			StringBuffer featureString = new StringBuffer("" + label);
			for (int i=0; i<featureVector.length; i++) {
				if (!naiveFeat && featureVector[i] == 0) continue;     // skip features with value zero
				featureString.append( " " + (i+1) + ":" + (featureVector[i] + "").replaceFirst("\\.0$", ""));
			}
			
			int featVecIdx = libSVMfeatures.indexOf(featureString.toString());
			if (naiveFeat || featVecIdx == -1) {
				libSVMfeatures.add(featureString.toString());
				sequenceNames.add(seqID);
			} else {
				sequenceNames.set(featVecIdx, sequenceNames.get(featVecIdx) + "\t" + seqID);
			}
		}
		BasicTools.writeList2File(libSVMfeatures, featureFile);
		BasicTools.writeList2File(sequenceNames, featureFile.replace(".txt", "_names.txt"));
	}
}
