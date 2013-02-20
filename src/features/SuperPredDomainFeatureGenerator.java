package features;

import io.BasicTools;
import ipr.IPRextract;
import ipr.IPRrun;
import ipr.IprEntry;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class SuperPredDomainFeatureGenerator extends DomainFeatureGenerator {

	private String fastaFileTF = "";

	public SuperPredDomainFeatureGenerator(String fastaFileTF, String iprscanResultFileTF, String libsvmOutfile) {
		this.fastaFileTF = fastaFileTF;
		this.iprscanResultFileTF = iprscanResultFileTF;
		this.libsvmOutfile = libsvmOutfile;
		this.basedir = new File(libsvmOutfile).getParent() + "/";
	}
	
	public SuperPredDomainFeatureGenerator() {
	}
	
	private void parseArguments(String[] args) {

		// create Options object
		Options options = new Options();
		options.addOption("fastaFileTF", true, "FASTA file for TFs with annotated superclass");
		options.addOption("iprscanResultFileTF", true, "InterProScan result file for TFs with annotated superclass");
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

		if(cmd.hasOption("fastaFileTF")) {
			fastaFileTF = cmd.getOptionValue("fastaFileTF");
		}
		else {
			System.out.println("Error. Argument \"-fastaFileTF <fasta_file_for_TFs>\" missing.");
			System.exit(1);
		}
		
		if(cmd.hasOption("iprscanResultFileTF")) {
			iprscanResultFileTF = cmd.getOptionValue("iprscanResultFileTF");
		}
		else {
			System.out.println("Error. Argument \"-iprscanResultFileTF <iprscan_result_file_for_TFs>\" missing.");
			System.exit(1);
		}
		
		if(cmd.hasOption("libsvmOutfile")) {
			libsvmOutfile = cmd.getOptionValue("libsvmOutfile");
			basedir = new File(libsvmOutfile).getParent() + "/";
		}
		else {
			System.out.println("Error. Argument \"-libsvmOutfile <libsvm_output_file>\" missing.");
			System.exit(1);
		}
		
		if (cmd.hasOption("verbose")) {
			silent = false;
		}
	}
	
	public void writeFeatureFile() {
		 
		// parse output files from InterProScan
		ArrayList<String[]> iprOutputTF = IPRrun.readIPRoutput(iprscanResultFileTF);
		
		// compute mapping from domain IDs to TF and Non-TF IDs, respectively
		HashMap<String,ArrayList<String>> domain2seqTF = IPRextract.getDomain2SeqMap(iprOutputTF);
		
		// filter domains which 1) exist in current InterPro release and 2) were detected in sequences from training set 
		ArrayList<String> currentDomains = downloadAllDomainIDs(); 
		relevantDomainIDs = filterCurrentDomainsInSet(domain2seqTF.keySet(), currentDomains);
		domain2seqTF = filterCurrentDomainsInDom2SeqMap(domain2seqTF, relevantDomainIDs);
		
		seq2domain = new HashMap<String, IprEntry>();
		seq2domain.putAll(IPRextract.getSeq2DomainMap(iprOutputTF, true));
		int numTrainSeq = seq2domain.size();
		filterCurrentDomainsInSeq2DomMap();
		
		// read superclass for each TF sequence
		HashMap<String, String> fastaFiles = BasicTools.readFASTA(fastaFileTF, true);
		HashMap<String, Integer> tf2superclass = getLabelsFromFastaHeaders(fastaFiles.keySet(), true, true);
		
		// add superclass to IprEntry objects
		int[] tfsPerClassCounter = new int[] {0,0,0,0,0};
		for (String seqID: seq2domain.keySet()) {
			String uniprotID = seqID;
			if (uniprotID.matches(".*_.*_.*")) {
				uniprotID = uniprotID.split("_")[1];
			}
			IprEntry currEntry = seq2domain.get(seqID);
			Integer currSuperclass = tf2superclass.get(uniprotID);
			if (currSuperclass == null) {
				System.out.println("Error. No superclass found for sequence: " + seqID);
				System.exit(0);
			}
			currEntry.superclass = currSuperclass;
			tfsPerClassCounter[currSuperclass]++; 
			seq2domain.put(seqID, currEntry);
		}
		
		BasicTools.writeArrayList2File(relevantDomainIDs, basedir + relevantDomainsFile);
		LibSVMOutfileWriter libsvmwriter = new LibSVMOutfileWriter();
		int[] numFeatVecRelevant = libsvmwriter.write(relevantDomainIDs, seq2domain, libsvmOutfile);
		int numFeatVec = numFeatVecRelevant[0] + numFeatVecRelevant[1] + numFeatVecRelevant[2] + numFeatVecRelevant[3] + numFeatVecRelevant[4];
		
		 if (!silent) {
			 System.out.println("\nNumber of domains in current InterPro release:     " + currentDomains.size());
			 System.out.println("Number of current domains detected in sequences:   " + relevantDomainIDs.size() + " / " + currentDomains.size() + "\n");
			 System.out.println("Number of sequences with current domains:          " + seq2domain.size() + " / " + numTrainSeq);
			 System.out.println("Number of basic domain TFs:                        " + tfsPerClassCounter[1] + " / " + seq2domain.size());
			 System.out.println("Number of zinc finger TFs:                         " + tfsPerClassCounter[2] + " / " + seq2domain.size());
			 System.out.println("Number of helix-turn-helix TFs                     " + tfsPerClassCounter[3] + " / " + seq2domain.size());
			 System.out.println("Number of beta scaffold TFs:                       " + tfsPerClassCounter[4] + " / " + seq2domain.size());
			 System.out.println("Number of other TFs:                               " + tfsPerClassCounter[0] + " / " + seq2domain.size() + "\n");
			 System.out.println("Number of unique feature vectors:                  " + numFeatVec + " / " + seq2domain.size());
			 System.out.println("Number of basic domain feature vectors:            " + numFeatVecRelevant[1] + " / " + numFeatVec);
			 System.out.println("Number of zinc finger feature vectors:             " + numFeatVecRelevant[2] + " / " + numFeatVec);
			 System.out.println("Number of helix-turn-helix feature vectors:        " + numFeatVecRelevant[3] + " / " + numFeatVec);
			 System.out.println("Number of beta scaffold feature vectors:           " + numFeatVecRelevant[4] + " / " + numFeatVec);
			 System.out.println("Number of other feature vectors:                   " + numFeatVecRelevant[0] + " / " + numFeatVec + "\n");
		 }
	}
			
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		SuperPredDomainFeatureGenerator featureFileGenerator = new SuperPredDomainFeatureGenerator();
		
		featureFileGenerator.parseArguments(args);
	}

}
