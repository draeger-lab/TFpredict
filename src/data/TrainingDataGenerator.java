/*
 * $Id: TrainingDataGenerator.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/data/TrainingDataGenerator.java $
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
package data;

import io.BasicTools;
import io.ObjectRW;
import ipr.IPRextract;
import ipr.IPRprocess;
import ipr.IprEntry;
import ipr.IprProcessed;
import ipr.IprRaw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import modes.Predict;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev: 99 $
 * @since 1.0
 */
public class TrainingDataGenerator {

	/**
	 * A {@link Logger} for this class.
	 */
	private static final Logger logger = Logger.getLogger(TrainingDataGenerator.class.getName());

	// General form of FASTA headers: Name|UniprotID|TF|TransfacClass|Database
	public static final int NameField = 0;
	public static final int UniProtIDField = 1;
	public static final int ProteinClassField = 2;
	public static final int TransfacClassField = 3;
	public static final int DatabaseField = 4;

	private static final String[] unmappedClasses = new String[]{"C0047; GRAS."};

	private static void generateTransFac2UniProtMapping(String factorFile, String outputFile) {

		TransFacParser parser = new TransFacParser();
		parser.parseFactors(factorFile);
		parser.writeTransFacID2UniProtMapping(outputFile);
	}

	private static void generateTransfacFlatfile(String factorFile, String matrixFile, String outputFile) {

		TransFacParser parser = new TransFacParser();
		parser.parseFactors(factorFile);
		parser.parseMatrices(matrixFile);
		parser.sanitizeTrainingset();
		parser.filterTFsWithPFM();
		parser.writeTrainingset(outputFile);
	}

	private static void generateTransfacFastafile(String factorFile, String matrixFile,  String outputFile) {

		TransFacParser parser = new TransFacParser();
		parser.parseFactors(factorFile);
		parser.parseMatrices(matrixFile);
		parser.sanitizeTrainingset();
		parser.writeFastafile(outputFile, unmappedClasses);
	}


	// 1) converts FASTA headers: "sp|TF|Q9Y2X0|MatBase" --> "Q9Y2X0|Q9Y2X0|TF|NA"
	// 2) adds TransFac classes (if available)
	private static void adjustHeadersInMatbaseFastafile(String oldFastaFile, String oldFastaFileSuper, String outFile) {

		Map<String, String> oldContent = BasicTools.readFASTA(oldFastaFile, true);
		Map<String, String> newContent = new HashMap<String, String>();

		// generate map from UniProt ID --> TransFac class based on FASTA headers
		// example header: >sp|P41738|P41738|TF|1.2.6.0.1.
		Map<String, String> superContent = BasicTools.readFASTA(oldFastaFileSuper, true);
		Map<String, String> uniprot2superclass = new HashMap<String, String>();
		for (String superHeader: superContent.keySet()) {
			String[] splittedHeader = superHeader.split("\\|");
			uniprot2superclass.put(splittedHeader[2], splittedHeader[4]);
		}

		for (String oldHeader: oldContent.keySet()) {

			String[] splittedHeader = oldHeader.split("\\|");
			String uniprotID = splittedHeader[2];
			String superclass = "NA";
			if (uniprot2superclass.containsKey(uniprotID)) {
				superclass = uniprot2superclass.get(uniprotID).replaceAll("(0\\.)+$", "");
			}
			String newHeader = uniprotID + "|" + uniprotID + "|TF|" + superclass;

			newContent.put(newHeader, oldContent.get(oldHeader));
		}
		BasicTools.writeFASTA(newContent, outFile);
	}

	// TF FASTA file: IRX3|A2VDX5|MatBase --> IRX3|A2VDX5|MatBase|TF|3.2.1.0.0
	private static void adjustHeadersInFastafile(String fastaFile, String flatFile, String outputFile) {

		// read FASTA file
		Map<String, String> sequences = BasicTools.readFASTA(fastaFile, true);

		// read SABINE flatfile
		SabineTrainingSetParser parser = new SabineTrainingSetParser();
		parser.parseTrainingset(flatFile);

		List<String> headers = new ArrayList<String>();
		headers.addAll(sequences.keySet());

		Map<String, String> headerMap = new HashMap<String, String>();
		for (String header: headers) {
			int idx = parser.tf_names.indexOf(header);
			if (idx == -1) {
				logger.severe("Error. No entry found for TF: " + header);
				System.exit(0);
			}

			// TODO: adjust superclass string (C0022; trp; 3.5.1.2.4. --> 3.5.1.2.4.)
			String newHeader = header + "|" + "TF" + "|" + parser.classes.get(idx);
			headerMap.put(header, newHeader);
		}

		// change headers of sequences and write FASTA file
		for (String header: sequences.keySet()) {
			sequences.put(headerMap.get(header), sequences.get(header));
			sequences.remove(header);
		}
		BasicTools.writeFASTA(sequences, outputFile);
	}

	private static void generateMergedFlatfile(String flatfile1, String flatfile2, String databaseName1, String databaseName2, String outputFile) {

		// parse first flatfile, generate unique headers, and remove duplicated sequences and IDs
		SabineTrainingSetParser parser1 = new SabineTrainingSetParser();
		parser1.parseTrainingset(flatfile1);
		parser1.setDataSource(databaseName1);
		parser1.sanitizeTrainingset();

		// parse second flatfile
		SabineTrainingSetParser parser2 = new SabineTrainingSetParser();
		parser2.parseTrainingset(flatfile2);
		parser2.setDataSource(databaseName2);
		parser2.sanitizeTrainingset();

		// write merged flatfile
		parser1.add(parser2);
		parser1.writeTrainingset(outputFile);
	}

	private static void generateMergedFastafile(String fastafile1, String fastafile2, String databaseName1, String databaseName2, String outputFile) {

		Map<String,String> mergedSeqs = new HashMap<String, String>();

		Map<String,String> fastaSeqs1 = BasicTools.readFASTA(fastafile1, true);
		Map<String,String> fastaSeqs2 = BasicTools.readFASTA(fastafile2, true);

		// add all sequences from first FASTA file to merged FASTA file
		for (String header: fastaSeqs1.keySet()) {
			String newHeader = header + "|" + databaseName1;
			mergedSeqs.put(newHeader, fastaSeqs1.get(header));
		}

		// generate set containing all UniProt IDs and sequences from first FASTA file
		HashSet<String> uniprot_ids = new HashSet<String>();
		HashSet<String> sequences = new HashSet<String>();
		for (String header: fastaSeqs1.keySet()) {
			uniprot_ids.add(header.split("\\|")[UniProtIDField].trim());
			sequences.add(fastaSeqs1.get(header));
		}
		uniprot_ids.remove("NA");

		// add sequences with new UniProt IDs from file
		for (String header: fastaSeqs2.keySet()) {
			String[] splittedHeader = header.split("\\|");
			String curr_id =  splittedHeader[UniProtIDField];
			String curr_seq = fastaSeqs2.get(header);
			if (!uniprot_ids.contains(curr_id) && !sequences.contains(curr_seq)) {
				String newHeader = header + "|" + databaseName2;
				mergedSeqs.put(newHeader, curr_seq);
			}
		}

		// write merged FASTA file
		BasicTools.writeFASTA(mergedSeqs, outputFile);
	}

	private static void generateFastafile4SuperPred(String fastaFile, String outputFile) {

		Map<String,String> sequences = BasicTools.readFASTA(fastaFile, true);
		Map<String,String> seqWithSuperclass = new HashMap<String, String>();

		// filter TF sequences with superclass annotation
		for (String header: sequences.keySet()) {

			String superclass = header.split("\\|")[TransfacClassField];
			if (!superclass.equals("NA")) {
				seqWithSuperclass.put(header, sequences.get(header));
			}
		}

		// write FASTA file for superclass prediction
		BasicTools.writeFASTA(seqWithSuperclass, outputFile);
	}

	private static void convertFlatfile2Fasta(String flatFile, String fastaFile) {

		// read flatfile in SABINE format
		SabineTrainingSetParser parser = new SabineTrainingSetParser();
		parser.parseTrainingset(flatFile);

		// extract IDs and sequences
		Map<String,String> sequences = new HashMap<String, String>();
		for (int i=0; i<parser.tf_names.size(); i++) {
			String currSeq = parser.sequences1.get(i);
			if (currSeq == null) {
				currSeq = parser.sequences2.get(i);
			}
			sequences.put(parser.tf_names.get(i), currSeq);
		}

		// write FASTA file
		BasicTools.writeFASTA(sequences, fastaFile);
	}


	// adds DNA-binding domains parsed from InterPro result to SABINE flatfile
	private static void addDBDs2Flatfile(String flatFile, String interproFile, String outputFile) {

		// parse SABINE training set
		SabineTrainingSetParser parser = new SabineTrainingSetParser();
		parser.parseTrainingset(flatFile);

		// read InterProScan output file and adjust TF names
		List<String> relGOterms = BasicTools.readResource2List(Predict.relGOterms_file);
		@SuppressWarnings("unchecked")
		Map<String, String> tfName2class = (Map<String, String>) ObjectRW.readFromResource(Predict.tfName2class_file);
		List<String[]> IPRoutput = BasicTools.readFile2ListSplitLines(interproFile);
		for (int i=0; i<IPRoutput.size(); i++) {
			String[] splittedLine = IPRoutput.get(i);
			splittedLine[0] = splittedLine[0].replace("_", "|");
			IPRoutput.set(i, splittedLine);
		}

		// parse domains from InterProScan result
		Map<String, IprEntry> seq2domain = IPRextract.getSeq2DomainMap(IPRoutput);
		Map<String, IprRaw>	IPRdomains = IPRextract.parseIPRoutput(IPRoutput);

		IprRaw domainsRaw = IPRdomains.get("T00650|NA|TransFac");

		Map<String, IprProcessed> seq2bindingDomain = IPRprocess.filterIPRdomains(seq2domain, IPRdomains, relGOterms, tfName2class);

		for (int i=0; i<parser.tf_names.size(); i++) {

			String currID = parser.tf_names.get(i);

			if (currID.equals("T00650|NA|TransFac")) {
				logger.info("Code reached.");
			}
			if (seq2bindingDomain.containsKey(currID)) {
				IprProcessed currDBDs = seq2bindingDomain.get(currID);
				parser.domains.set(i, currDBDs.binding_domains);

			} else {
				parser.domains.set(i, null);
			}
		}

		// remove TFs for which no DBDs are available
		parser.filterTFsWithDBD();

		// write SABINE training set with DBDs to file
		parser.writeTrainingset(outputFile);
	}


	private static void filterFlatfileUsingFasta(String flatFile, String fastaFile, String outputFile) {

		// parse SABINE training set
		SabineTrainingSetParser parser = new SabineTrainingSetParser();
		parser.parseTrainingset(flatFile);

		// read Fasta file
		Map<String, String> fastaSeqs = BasicTools.readFASTA(fastaFile, true);
		List<String> tfNames = new ArrayList<String>();
		tfNames.addAll(fastaSeqs.keySet());


		// adjust headers (IRX3|A2VDX5|TF|1.2.1.4.3.|MatBase --> IRX3|A2VDX5|MatBase) to enable comparison
		for (int i=0; i<tfNames.size(); i++) {
			String[] splittedHeader = tfNames.get(i).split("\\|");
			String newHeader = splittedHeader[NameField] + "|" + splittedHeader[UniProtIDField] + "|" + splittedHeader[DatabaseField];
			tfNames.set(i, newHeader);
		}


		for (int i=parser.tf_names.size()-1; i>=0; i--) {
			if (!tfNames.contains(parser.tf_names.get(i))) {
				parser.removeTF(i);
			}
		}

		// write SABINE training set with DBDs to file
		parser.writeTrainingset(outputFile);
	}

	// adds DNA-binding domains parsed from InterPro result to SABINE flatfile
	private static void filterIPRscanFileUsingFasta(String iprFile, String fastaFile, String outputFile) {

		// read InterProScan output file
		List<String[]> iprOutput = BasicTools.readFile2ListSplitLines(iprFile);
		logger.info("Number of parsed interpro domains: " + iprOutput.size());

		// read Fasta file
		Map<String, String> fastaSeqs = BasicTools.readFASTA(fastaFile, true);
		List<String> tfNames = new ArrayList<String>();
		tfNames.addAll(fastaSeqs.keySet());
		for (int i=0; i<tfNames.size(); i++) {
			tfNames.set(i, tfNames.get(i).split("\\|")[UniProtIDField]);
		}

		// generate filtered InterProScan output file
		for (int i=iprOutput.size()-1; i>=0; i--) {
			String currSeqID = iprOutput.get(i)[0];

			// is Sequence ID of form P10628 or Hoxd4_P10628_MatBase ?
			if (currSeqID.contains("_")) {
				currSeqID = currSeqID.split("_")[UniProtIDField];
			}

			if (!tfNames.contains(currSeqID)) {
				iprOutput.remove(i);
			}
		}
		logger.info("Number of parsed interpro domains after filtering: " + iprOutput.size());

		BasicTools.writeSplittedList2File(iprOutput, outputFile);
	}


	// add superclass annotation to fasta file headers (IRX3|A2VDX5|MatBase --> IRX3|A2VDX5|TF|1.2.1.4.3.|MatBase)
	private static void addSuperclass2Fasta(String sabineFlatfile, String inputFasta, String outputFasta) {

		// read FASTA file
		Map<String, String> fastaSeqs = BasicTools.readFASTA(inputFasta, true);

		// generate mapping from TF names to superclasses
		SabineTrainingSetParser parser = new SabineTrainingSetParser();
		parser.parseTrainingset(sabineFlatfile);
		Map<String, String> tfName2class = new HashMap<String, String>();
		for (int i=0; i<parser.tf_names.size(); i++) {
			tfName2class.put(parser.tf_names.get(i), parser.classes.get(i));
		}

		Map<String, String> fastaSeqsWithSuperclass = new HashMap<String, String>();
		for (String tfName: fastaSeqs.keySet()) {

			String currClass = tfName2class.get(tfName);
			if (currClass.equals("NA")) {
				continue;
			} else {
				currClass = TFdataTools.getTransfacClass(tfName2class.get(tfName));
			}

			String[] splittedHeader = tfName.split("\\|");
			String newHeader = splittedHeader[NameField] + "|" + splittedHeader[UniProtIDField] + "|TF|" + currClass + "|" + splittedHeader[DatabaseField-2];
			fastaSeqsWithSuperclass.put(newHeader, fastaSeqs.get(tfName));
		}

		// write FASTA file with modified headers
		BasicTools.writeFASTA(fastaSeqsWithSuperclass, outputFasta);
	}

	public static void printDoubletteUniprotID(String fastaFile) {

		Map<String, String> fastaSeqs = BasicTools.readFASTA(fastaFile, true);
		Map<String, String> uniprot2header = new HashMap<String, String>();
		for (String header: fastaSeqs.keySet()) {
			String uniprotID = header.split("\\|")[1];
			if (!uniprot2header.containsKey(uniprotID)) {
				uniprot2header.put(uniprotID, header);
			} else {
				logger.info("(1) " + uniprot2header.get(uniprotID) + "\t(2) " + header);
			}
		}
	}

	public static void adjustHeadersInIPRscanFile(String iprFile, String fastaFile, String outputFile) {

		// generate mapping from TF names to UniProt IDs
		Map<String, String> fastaSeqs = BasicTools.readFASTA(fastaFile, true);
		Map<String, String> tfName2uniprot = new HashMap<String, String>();
		for (String header: fastaSeqs.keySet()) {
			String tfName = header.split("\\|")[0];
			String uniprotID = header.split("\\|")[1];
			if (!tfName2uniprot.containsKey(tfName)) {
				tfName2uniprot.put(tfName, uniprotID);
			} else {
				logger.info("Name: " + tfName + "\tUniProt: " + uniprotID + " is already in map.");
			}
		}

		// read InterProScan output file
		List<String[]> iprOutput = BasicTools.readFile2ListSplitLines(iprFile);
		logger.info("Number of parsed interpro domains: " + iprOutput.size());

		boolean[] irrelLines = new boolean[iprOutput.size()];
		for (int i=0; i<iprOutput.size(); i++) {
			String[] currLine = iprOutput.get(i);
			String[] splittedID = currLine[0].split("_");
			if (splittedID[UniProtIDField].equals("NA")) {
				String currName = splittedID[NameField];
				if (tfName2uniprot.containsKey(currName)) {
					String currUniprot = tfName2uniprot.get(currName);
					currLine[0] = splittedID[NameField] + "_" + currUniprot + "_" + splittedID[DatabaseField-2];
					logger.fine(currLine[0]);
					iprOutput.set(i, currLine);

				} else {
					irrelLines[i] = true;
				}
			}
		}
		for (int i=iprOutput.size()-1; i>=0; i--) {
			if (irrelLines[i]) {
				iprOutput.remove(i);
			}
		}
		logger.info("Number of parsed interpro domains after filtering: " + iprOutput.size());

		BasicTools.writeSplittedList2File(iprOutput, outputFile);
	}

	private static void removeFalseNegativeNonTFs(String tfFastaFile, String nontfFastaFile, String outputFile) {

		Map<String, String> tfSeqs = BasicTools.readFASTA(tfFastaFile, true);
		Map<String, String> nontfSeqs = BasicTools.readFASTA(nontfFastaFile, true);

		ArrayList<String> tfNames = new ArrayList<String>(tfSeqs.keySet());
		for (int i=0; i<tfNames.size(); i++) {
			tfNames.set(i, tfNames.get(i).split("\\|")[1]);
		}

		// rewrite NonTF.fasta
		Map<String, String> correctedNontfSeqs = new HashMap<String, String>();
		for (String nontfID: nontfSeqs.keySet()) {
			if (!tfNames.contains(nontfID.split("\\|")[1])) {
				correctedNontfSeqs.put(nontfID, nontfSeqs.get(nontfID));
			}
		}
		BasicTools.writeFASTA(correctedNontfSeqs, outputFile);
	}

	private static void generateFastaSubsetFile(String fastaFile, String uniprotIDfile, String fastaSubsetFile) {

		List<String> uniprotIDs = BasicTools.readFile2List(uniprotIDfile, false);

		Map<String, String> seqs = BasicTools.readFASTA(fastaFile, true);
		Map<String, String> seqsSubset = new HashMap<String, String>();

		for (String seqID: seqs.keySet()) {
			if (uniprotIDs.contains(seqID.split("\\|")[1])) {
				seqsSubset.put(seqID, seqs.get(seqID));
			}
		}
		BasicTools.writeFASTA(seqsSubset, fastaSubsetFile);
	}

	private static void convertSabineFlatfile2Inputfile(String sabineFlatfile, String sabineInputfile) {

		// read SABINE flatfile
		SabineTrainingSetParser parser = new SabineTrainingSetParser();
		parser.parseTrainingset(sabineFlatfile);

		// remove PFMs
		for (int i=0; i<parser.pfm_names.size(); i++) {
			parser.pfm_names.set(i, null);
			parser.pfms.set(i, null);
		}
		parser.writeTrainingset(sabineInputfile);
	}


	public static void main(String args[]) {

		/*
		String fastaDir = "/rahome/eichner/projects/tfpredict/data/tf_pred/fasta_files/latest/";
		String transfacDir = "/rahome/eichner/data/biobase/transfac_2012.2/dat/";
		String flatfileDir = "/rahome/eichner/projects/tfpredict/data/tf_pred/sabine_files/latest/";

		String factorFile = transfacDir + "factor.dat";
		String matrixFile = transfacDir + "matrix.dat";

		String transfacFlatfile = flatfileDir + "transfac_2012.2_flatfile.txt";
		String matbaseFlatfile = flatfileDir + "matbase_8.2_flatfile.txt";
		String mergedFlatfile = flatfileDir + "transfac2012.2_matbase8.2_flatfile.txt";
		String mergedFlatfileDBDs = flatfileDir + "transfac2012.2_matbase8.2_flatfile_with_dbd.txt";
		String mergedFlatfileDBDsFBPs = flatfileDir + "transfac2012.2_matbase8.2_flatfile_with_dbd_and_mergedPFM.txt";

		String transfacFastafile = fastaDir + "transfac_2012.2.fasta";
		String matbaseFastafileOldHeaders = fastaDir + "matbase_8.2_oldHeaders.fasta";
		String matbaseFastafileOldHeadersSuper = fastaDir + "matbase_8.2_oldHeaders_with_superclass.fasta";
		String matbaseFastafile = fastaDir + "matbase_8.2.fasta";
		String mergedFastafile = fastaDir + "transfac2012.2_matbase8.2.fasta";
		String superclassFastafile = fastaDir + "transfac2012.2_matbase8.2_with_superclass.fasta";
		String sabineFastafile = fastaDir + "transfac2012.2_matbase8.2_with_superclass_and_pfm.fasta";

		generateTransfacFlatfile(factorFile, matrixFile, transfacFlatfile);
		generateTransfacFastafile(factorFile, matrixFile, transfacFastafile);

		adjustHeadersInMatbaseFastafile(matbaseFastafileOldHeaders, matbaseFastafileOldHeadersSuper, matbaseFastafile);

		generateMergedFlatfile(transfacFlatfile, matbaseFlatfile, "TransFac", "MatBase", mergedFlatfile);
		generateMergedFastafile(transfacFastafile, matbaseFastafile, "TransFac", "MatBase", mergedFastafile);

		generateFastafile4SuperPred(mergedFastafile, superclassFastafile);
		convertFlatfile2Fasta(mergedFlatfile, sabineFastafile);


		String interproFile = "/rahome/eichner/projects/tfpredict/data/tf_pred/interpro_files/latest/transfac2012.2_matbase8.2_with_superclass_and_pfm.fasta.out";
		addDBDs2Flatfile(mergedFlatfile, interproFile, mergedFlatfileDBDs);
		 */


		/*
		 * INFO: merging of PFMs was performed using class "TrainingDataGenerator" from SABINE project (as STAMP is required)
		 */

		/*
		 *  generate FASTA file for InterProScan based on old public and proprietary SABINE training set
		 */

		/*
		String sabineTrainingsetBiobase = "/rahome/eichner/workspace/SABINE/data/trainingsets_biobase/trainingset_full.txt";
		String sabineTrainingsetPublic = "/rahome/eichner/workspace/SABINE/data/trainingsets_public/trainingset_public.txt";
		String sabineTrainingsetInterpro = "/rahome/eichner/workspace/SABINE/data/trainingsets_interpro/trainingset_interpro.txt";
		String fastaTrainingsetBiobase = "/rahome/eichner/workspace/SABINE/data/trainingsets_biobase/trainingset_full.fasta";
		String fastaTrainingsetPublic = "/rahome/eichner/workspace/SABINE/data/trainingsets_public/trainingset_public.fasta";
		String fastaTrainingsetInterpro = "/rahome/eichner/workspace/SABINE/data/trainingsets_interpro/trainingset_interpro.fasta";

		convertFlatfile2Fasta(sabineTrainingsetBiobase, fastaTrainingsetBiobase);
		convertFlatfile2Fasta(sabineTrainingsetPublic, fastaTrainingsetPublic);
		convertFlatfile2Fasta(sabineTrainingsetInterpro, fastaTrainingsetInterpro);
		 */

		/*
		 *  generate homology-reduced training set for SABINE
		 */

		/*
		String sabineTrainingsetInterpro = "/rahome/eichner/workspace/SABINE/data/trainingsets_interpro/trainingset_interpro.txt";
		String fastaTrainingsetCdHit = "/rahome/eichner/workspace/SABINE/data/trainingsets_interpro/trainingset_interpro_cdhit70.fasta";
		String sabineTrainingsetCdHit = "/rahome/eichner/workspace/SABINE/data/trainingsets_interpro/trainingset_interpro_cdhit70.txt";
		filterFlatfileUsingFasta(sabineTrainingsetInterpro, fastaTrainingsetCdHit, sabineTrainingsetCdHit);
		 */

		/*
		 *  replace DBDs in old public/proprietary training set by InterPro domains
		 */

		/*
		String sabineTrainingsetBiobase = "/rahome/eichner/workspace/SABINE/data/trainingsets_biobase/trainingset_full.txt";
		String sabineTrainingsetBiobaseInterpro = "/rahome/eichner/workspace/SABINE/data/trainingsets_biobase_interpro/trainingset_biobase_interpro.txt";
		String sabineTrainingsetPublic = "/rahome/eichner/workspace/SABINE/data/trainingsets_public/trainingset_public.txt";
		String sabineTrainingsetPublicInterpro = "/rahome/eichner/workspace/SABINE/data/trainingsets_public_interpro/trainingset_public_interpro.txt";

		String domainsTrainingsetBiobase = "/home/eichner/projects/tfpredict/data/tf_pred/interpro_files/16.08.2012/trainingset_full.fasta.out";

		addDBDs2Flatfile(sabineTrainingsetBiobase, domainsTrainingsetBiobase, sabineTrainingsetBiobaseInterpro);
		addDBDs2Flatfile(sabineTrainingsetPublic, domainsTrainingsetBiobase, sabineTrainingsetPublicInterpro);
		 */

		/*
		 *  adjust InterPro files based on CD-HIT-filtered FASTA files
		 */

		String baseDir = "/rahome/eichner/projects/tfpredict/data/tf_pred/";
		/*
		String fastaDir = baseDir + "fasta_files/latest/";
		String interproDir = baseDir + "interpro_files/latest/";

		String trainTFfasta = fastaDir + "TF_cdhit80.fasta";
		String trainTFinterpro = interproDir + "TF.fasta.out";
		String trainTFinterproFiltered = interproDir + "TF_cdhit80.fasta.out";
		filterIPRscanFileUsingFasta(trainTFinterpro, trainTFfasta, trainTFinterproFiltered);

		String trainNonTFfasta = fastaDir + "NonTF_cdhit80.fasta";
		String trainNonTFinterpro = interproDir + "NonTF.fasta.out";
		String trainNonTFinterproFiltered = interproDir + "NonTF_cdhit80.fasta.out";
		filterIPRscanFileUsingFasta(trainNonTFinterpro, trainNonTFfasta, trainNonTFinterproFiltered);


		String nonTfTrainFasta = fastaDir + "latest/NonTF_cdhit80.fasta";			 // 24351 Non-TFs
		String nonTfTrainInterpro = interproDir + "NonTF_cdhit80.fasta.out";		 // 290056 Domains
		String tfTrainFasta = fastaDir + "TF_cdhit80.fasta";						 // 1648 TFs
		String tfTrainInterpro = interproDir + "TF_cdhit80.fasta.out";				 // 23155 Domains
		 */


		/*
		 *  Add superclass annotation to TF fastafile headers
		 */
		String sabineTrainFlatfile = baseDir + "sabine_files/29.11.2012/TF_cdhit80.txt";
		String sabineTrainFlatfileWithSuper = baseDir + "sabine_files/29.11.2012/TF_cdhit80_withSuper.txt";
		String tfTrainFastaWithSuper = baseDir + "fasta_files/29.11.2012/TF_cdhit80_withSuper.fasta";
		String tfTrainFastaWithSuperAndUniProt = baseDir + "fasta_files/12.02.2013/TF.fasta";
		String tfTrainInterpro = baseDir + "interpro_files/29.11.2012/TF_cdhit80.fasta.out";
		String nontfTrainInterpro = baseDir + "interpro_files/latest/NonTF.fasta.out";
		String nontfTrainInterproFiltered = baseDir + "interpro_files/latest/NonTF_filtered.fasta.out";
		String tfTrainInterproFiltered = baseDir + "interpro_files/29.11.2012/TF_cdhit80_filtered.fasta.out";
		String tfTrainInterproFilteredWithUniProt = baseDir + "interpro_files/29.11.2012/TF_cdhit80_filtered_uniprot.fasta.out";


		String tfTrainFasta = baseDir + "fasta_files/12.02.2013/TF.fasta";
		String nontfTrainFasta = baseDir + "fasta_files/12.02.2013/NonTF.fasta";
		String nontfTrainFastaCorrected = baseDir + "fasta_files/20.03.2013/NonTF.fasta";

		String trainFasta = baseDir + "fasta_files/20.03.2013/TFnonTF.fasta";
		String trainFastaSubset = baseDir + "fasta_files/20.03.2013/TFnonTFpercNA.fasta";
		String NAseqIDsPercFeatFile = "/rahome/eichner/Desktop/na_seqIDs_percFeat.txt";

		/*
		addSuperclass2Fasta(sabineTrainFlatfile, tfTrainFasta, tfTrainFastaWithSuper);
		filterFlatfileUsingFasta(sabineTrainFlatfile, tfTrainFastaWithSuper,  sabineTrainFlatfileWithSuper);
		 */
		//filterIPRscanFileUsingFasta(nontfTrainInterpro, nontfTrainFasta, nontfTrainInterproFiltered);

		//adjustHeadersInIPRscanFile(tfTrainInterproFiltered, tfTrainFastaWithSuperAndUniProt, tfTrainInterproFilteredWithUniProt);

		//removeFalseNegativeNonTFs(tfTrainFasta, nontfTrainFasta, nontfTrainFastaCorrected);

		//generateFastaSubsetFile(trainFasta, NAseqIDsPercFeatFile, trainFastaSubset);

		String sabineFlatfile = "/rahome/eichner/projects/sabine/data/trainingsets/latest/trainingset_public.txt";
		String sabineInputFile = "/rahome/eichner/projects/sabine/data/trainingsets/latest/inputset_public.txt";
		// convertSabineFlatfile2Inputfile(sabineFlatfile, sabineInputFile);

		String factorFile = "/rahome/eichner/data/biobase/transfac_2012.2/dat/factor.dat";
		String mappingFile = "/rahome/eichner/projects/tfpredict/failed_inputs/transfac2uniprot.txt";
		//generateTransFac2UniProtMapping(factorFile, mappingFile);

	}
}
