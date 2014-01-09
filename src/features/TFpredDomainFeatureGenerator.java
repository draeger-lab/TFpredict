/*  
 * $Id$
 * $URL$
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
import ipr.IPRextract;
import ipr.IPRrun;
import ipr.IprEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


/**
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
public class TFpredDomainFeatureGenerator extends DomainFeatureGenerator{

	/**
	 * @param args
	 * @throws InterruptedException 
	 */
	
	private static final String specificDomainsTFfile = "domains_TFonly.txt";
	private static final String specificDomainsNonTFfile = "domains_NonTFonly.txt";
	private static final String domain2pvalueFile = "domains2pvalues.txt";
	private static final String domain2pvalueCorrectedFile = "domains2correctedPvalues.txt"; 
	private static final String filteredDomainsFile = "filtered_domains.txt";

	private String iprscanResultFileNonTF = ""; 
	private boolean useBonferroniHolm = false;
	private double pValueCutoff = 0.05;
	
	private List<String> filteredDomainIDs = new ArrayList<String>();

	public TFpredDomainFeatureGenerator(String iprscanResultFileTF, String iprscanResultFileNonTF, String libsvmOutfile) {
		this.iprscanResultFileTF = iprscanResultFileTF;
		this.iprscanResultFileNonTF = iprscanResultFileNonTF;
		this.libsvmOutfile = libsvmOutfile;
		this.basedir = new File(libsvmOutfile).getParent() + "/";
	}
	
	public TFpredDomainFeatureGenerator() {
	}

	private void parseArguments(String[] args) {

		// create Options object
		Options options = new Options();	
		options.addOption("useBonferroniHolm", false, "use Bonferroni-Holm instead of Benjamini-Hochberg correction");
		options.addOption("pValueCutoff", true, "set p-value cutoff");
		options.addOption("iprscanResultFileTF", true, "InterProScan result file for TFs");
		options.addOption("iprscanResultFileNonTF", true, "InterProScan result file for Non-TFs");
		options.addOption("verbose", false, "print additional stats");
		options.addOption("libsvmOutfile", true, "libsvm output file");
		
		// parse arguments
		CommandLine cmd = null;
		CommandLineParser cmdparser = new PosixParser();
		try {
			cmd = cmdparser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if(cmd.hasOption("libsvmOutfile")) {
			libsvmOutfile = cmd.getOptionValue("libsvmOutfile");
			basedir = new File(libsvmOutfile).getParent() + "/";
		}
		else {
			System.out.println("Error. Argument \"-libsvmOutfile <libsvm_output_file>\" missing.");
			System.exit(1);
		}
		
		if(cmd.hasOption("iprscanResultFileTF")) {
			iprscanResultFileTF = cmd.getOptionValue("iprscanResultFileTF");
		}
		else {
			System.out.println("Error. Argument \"-iprscanResultFileTF <iprscan_result_file_for_TFs>\" missing.");
			System.exit(1);
		}
		
		if(cmd.hasOption("iprscanResultFileNonTF")) {
			iprscanResultFileNonTF = cmd.getOptionValue("iprscanResultFileNonTF");
		}
		else {
			System.out.println("Error. Argument \"-iprscanResultFileNonTF <iprscan_result_file_for_NonTFs>\" missing.");
			System.exit(1);
		}
		
		if(cmd.hasOption("useBonferroniHolm")) {
			useBonferroniHolm = true;
		}
		
		if(cmd.hasOption("pValueCutoff")) {
			pValueCutoff = Double.parseDouble(cmd.getOptionValue("pValueCutoff"));
		}
		
		if (cmd.hasOption("verbose")) {
			silent = false;
		}
	}	
	
	// use four-field test to filter significant domains
	private void filterDomainIDs(Map<String, List<String>> domain2seqTF, Map<String, List<String>> domain2seqNonTF) {
		
		filteredDomainIDs.addAll(relevantDomainIDs);
		FourFieldTest fourFieldTest = new FourFieldTest();
		fourFieldTest.run(filteredDomainIDs, domain2seqTF, domain2seqNonTF);

		filteredDomainIDs = fourFieldTest.getDomainIDs();		
		List<Double> pvalues = fourFieldTest.getPvalues();

		// write InterPro Domain IDs and corresponding p-values to file 
		BasicTools.writeSplittedList2File(BasicTools.combineLists(filteredDomainIDs, pvalues), basedir + domain2pvalueFile);

		// correct pvalues using Holm-Bonferroni method
		if(useBonferroniHolm) {
			HolmBonferroni BonHolm = new HolmBonferroni();
			pvalues = BonHolm.correct(filteredDomainIDs, pvalues);

			// correct pvalues using Benjamini-Hochberg (FDR) method
		} else {
			BenjaminiHochberg BenHoch = new BenjaminiHochberg();
			pvalues = BenHoch.correct(pvalues);
		}
		// filter significant domains
		for (int i=filteredDomainIDs.size()-1; i>=0; i--) {
			if (pvalues.get(i) >= pValueCutoff) {
				filteredDomainIDs.remove(i);
				pvalues.remove(i);
			}
		}
		
		// write InterPro Domain IDs and corresponding p-values to file 
		BasicTools.writeSplittedList2File(BasicTools.combineLists(filteredDomainIDs, pvalues), basedir + domain2pvalueCorrectedFile);
	}

	public void writeFeatureFile() {
 
		// parse output files from InterProScan
		List<String[]> iprOutputTF = IPRrun.readIPRoutput(iprscanResultFileTF);
		List<String[]> iprOutputNonTF = IPRrun.readIPRoutput(iprscanResultFileNonTF);
		
		// compute mapping from domain IDs to TF and Non-TF IDs, respectively
		Map<String, List<String>> domain2seqTF = IPRextract.getDomain2SeqMap(iprOutputTF);
		Map<String, List<String>> domain2seqNonTF = IPRextract.getDomain2SeqMap(iprOutputNonTF);
		int numTFwithDomain = domain2seqTF.keySet().size();
		int numNonTFwithDomain = domain2seqNonTF.keySet().size();
		
		// filter domains which 1) exist in current InterPro release and 2) were detected in sequences from training set 
		List<String> currentDomains = downloadAllDomainIDs(); 
		
		List<String> domainsTF = filterCurrentDomainsInSet(domain2seqTF.keySet(), currentDomains);
		domain2seqTF = filterCurrentDomainsInDom2SeqMap(domain2seqTF, domainsTF);
	    
	    List<String> domainsNonTF = filterCurrentDomainsInSet(domain2seqNonTF.keySet(), currentDomains);
	    domain2seqNonTF = filterCurrentDomainsInDom2SeqMap(domain2seqNonTF, domainsNonTF);
	    
	    relevantDomainIDs = BasicTools.union(domainsTF, domainsNonTF);  
	 
	    // get domains which are unique for TFs or Non-TFs
	    List<String> specificDomainsTF = BasicTools.setDiff(domainsTF, domainsNonTF);
	    List<String> specificDomainsNonTF = BasicTools.setDiff(domainsNonTF, domainsTF);
	    
		BasicTools.writeList2File(specificDomainsTF, basedir + specificDomainsTFfile);
		BasicTools.writeList2File(specificDomainsNonTF, basedir + specificDomainsNonTFfile);

		// use four-field test to filter domains significantly correlated with class labels
		filterDomainIDs(domain2seqTF, domain2seqNonTF);

		// extract mapping from sequence IDs  -->  domain IDs 
		seq2domain = new HashMap<String, IprEntry>();
		seq2domain.putAll(IPRextract.getSeq2DomainMap(iprOutputTF, true));
		seq2domain.putAll(IPRextract.getSeq2DomainMap(iprOutputNonTF, false));
		int numTrainSeq = seq2domain.size(); 
		
		// remove domains which are not contained in current version of InterPro
		filterCurrentDomainsInSeq2DomMap();
		int[] numSeqPerClass = getNumSeqPerClass(true);
		
		BasicTools.writeList2File(relevantDomainIDs, basedir + relevantDomainsFile);
		BasicTools.writeList2File(filteredDomainIDs, basedir + filteredDomainsFile);
		
		LibSVMOutfileWriter libsvmwriter = new LibSVMOutfileWriter();
		int[] numFeatVecRelevant = libsvmwriter.write(relevantDomainIDs, seq2domain, libsvmOutfile);
		int[] numFeatVecFiltered = libsvmwriter.write(filteredDomainIDs, seq2domain, libsvmOutfile.replace(".txt", "_filtered.txt"));	
		
		 if (!silent) {
			 System.out.println("\nNumber of domains in current InterPro release:     " + currentDomains.size());
			 System.out.println("Number of current domains detected in sequences:   " + relevantDomainIDs.size() + " / " + currentDomains.size());
			 System.out.println("Number of significant domains (p < " + pValueCutoff + "):          " + filteredDomainIDs.size() + " / " + relevantDomainIDs.size() + "\n");
			 
			 System.out.println("Number of current domains detected in TFs:         " + domainsTF.size() + " / " + numTFwithDomain);
			 System.out.println("Number of domains detected specificly in TFs:      " + specificDomainsTF.size() + " / " + domain2seqTF.size()  + "\n");
			 System.out.println("Number of current domains detected in Non-TFs:     " + domainsNonTF.size() + " / " + numNonTFwithDomain);
			 System.out.println("Number of domains detected specificly in Non-TFs:  " + specificDomainsNonTF.size() + " / " + domain2seqNonTF.size() + "\n");
			 
			 System.out.println("Number of sequences with current domains:          " + seq2domain.size() + " / " + numTrainSeq);
			 System.out.println("Number of unique feature vectors:                  " + (numFeatVecRelevant[0] + numFeatVecRelevant[1]) + " / " + seq2domain.size());
			 System.out.println("Number of feature vectors after significance test: " + (numFeatVecFiltered[0] + numFeatVecFiltered[1]) + " / " + (numFeatVecRelevant[0] + numFeatVecRelevant[1])  + "\n");
			 
			 System.out.println("Number of TF feature vectors after test:           " + numFeatVecFiltered[0] + " / " + numFeatVecRelevant[0] + " / " + numSeqPerClass[0]);
			 System.out.println("Number of Non-TF feature vectors after test:       " + numFeatVecFiltered[1] + " / " + numFeatVecRelevant[1] + " / " + numSeqPerClass[1]);
		 }
	}
	
	
	public static void main(String[] args) {
		
		TFpredDomainFeatureGenerator featureFileGenerator = new TFpredDomainFeatureGenerator();
		featureFileGenerator.parseArguments(args);
		featureFileGenerator.writeFeatureFile();
	}
}


