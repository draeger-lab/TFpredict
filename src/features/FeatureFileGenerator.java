package features;

import io.BasicTools;
import io.FTPsupport;
import ipr.IprEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public abstract class FeatureFileGenerator {
	
	protected static final String domainDescriptionURL = "ftp://ftp.ebi.ac.uk/pub/databases/interpro/names.dat";

	protected String iprscanResultFileTF = "";
	protected String libsvmOutfile = ""; 
	protected boolean silent = false;
	protected String basedir = "";
	protected ArrayList<String> relevantDomainIDs = new ArrayList<String>();
	protected HashMap<String, IprEntry> seq2domain = new HashMap<String, IprEntry>();
	
	
	protected static ArrayList<String> filterCurrentDomainsInSet(Set<String> domainSet, ArrayList<String> currentDomains) {
		ArrayList<String> domainsList = new ArrayList<String>();
		domainsList.addAll(domainSet);
		domainsList = BasicTools.intersect(domainsList, currentDomains);
		
		return(domainsList);
	}
	
	protected static HashMap<String, ArrayList<String>> filterCurrentDomainsInDom2SeqMap(HashMap<String, ArrayList<String>> domain2seq, ArrayList<String> currDomains) {
	    
		for (String domain: domain2seq.keySet().toArray(new String[]{})) {
	    	if (!currDomains.contains(domain)) {
	    		domain2seq.remove(domain);
	    	}
	    }
		return(domain2seq);
	}
	
	protected void filterCurrentDomainsInSeq2DomMap() {
		
		for (String seq: seq2domain.keySet().toArray(new String[]{})) {

			IprEntry currEntry = seq2domain.get(seq);
			for (int r=currEntry.domain_ids.size()-1; r>=0; r--) {
				if (!relevantDomainIDs.contains(currEntry.domain_ids.get(r))) {
					currEntry.domain_ids.remove(r);
				} 
			}

			if (currEntry.domain_ids.isEmpty()) {
				seq2domain.remove(seq);
			} else {
				seq2domain.put(seq, currEntry);
			}
		}
	}
	
	
	protected static ArrayList<String> downloadAllDomainIDs() {
		
		// retrieve file with InterPro domain IDs and descriptions via FTP site
		FTPsupport ftp = new FTPsupport();
		String domainDescFile = ftp.download(domainDescriptionURL);
		
		// parse all known InterPro domain IDs
		HashMap<String, String> domain2desc = BasicTools.readFile2Map(domainDescFile);
		ArrayList<String> allDomainIDs = new ArrayList<String>();
		allDomainIDs.addAll(domain2desc.keySet());
		Collections.sort(allDomainIDs);
		
		return(allDomainIDs);
	}
	
	
	public static void main(String[] args) {
		
		/*
		 *  Generation of feature files
		 */
		
		String dataDir = "/rahome/eichner/projects/tfpredict/data/";
		
		// generate feature file for TF prediction
		String iprscanResultFileTF =  dataDir + "tf_pred/interpro_files/TF.fasta.out"; 
		String iprscanResultsFileNonTF = dataDir + "tf_pred/interpro_files/NonTF.fasta.out";
		String tfFeatureFile = dataDir + "tf_pred/libsvm_files/libsvm_featurefile.txt";
		TFpredFeatureFileGenerator tfFeatFileGenerator = new TFpredFeatureFileGenerator(iprscanResultFileTF, iprscanResultsFileNonTF, tfFeatureFile);
		tfFeatFileGenerator.writeFeatureFile();
		
	    // generate feature file for superclass prediction
		String fastaFileSuper =  dataDir + "super_pred/fasta_files/superclassTF.fasta"; 
		String iprscanResultFileSuper =  dataDir + "super_pred/interpro_files/superclassTF.fasta.out"; 
		String superFeatureFile = dataDir + "super_pred/libsvm_files/libsvm_featurefile.txt";
		SuperPredFeatureFileGenerator superFeatFileGenerator = new SuperPredFeatureFileGenerator(fastaFileSuper, iprscanResultFileSuper, superFeatureFile);
		superFeatFileGenerator.writeFeatureFile();	
	}
}
