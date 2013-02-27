package features;

import io.BasicTools;
import io.FTPsupport;
import ipr.IprEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import data.TrainingDataGenerator;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public abstract class DomainFeatureGenerator {
	
	protected static final String domainDescriptionURL = "ftp://ftp.ebi.ac.uk/pub/databases/interpro/names.dat";
	protected static final String relevantDomainsFile = "relevant_domains.txt";
	
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
	
	protected int[] getNumSeqPerClass(boolean isTFnonTF) {
		
		int[] seqCounter; 
		if (isTFnonTF) {
			seqCounter = new int[2];
		} else {
			seqCounter = new int[5];
		}
		
		for (String seq: seq2domain.keySet().toArray(new String[]{})) {
			if (isTFnonTF) {
				if (seq2domain.get(seq).isTF) {
					seqCounter[0]++;
				} else {
					seqCounter[1]++;
				}
				
			} else {
				seqCounter[seq2domain.get(seq).superclass]++;
			}
		}
		return seqCounter;
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
	
	public static HashMap<String, Integer> getLabelsFromFastaHeaders(Set<String> fastaFileHeaders, boolean superPred, boolean useUniprotIDasKey) {
		
		HashMap<String, Integer> tf2superclass = new HashMap<String, Integer>();
		for (String header: fastaFileHeaders) {
			
			// use either UniProt ID or full Header (with label) as sequence ID
			String[] splittedHeader = header.split("\\|");
			String seqID = header;
			if (useUniprotIDasKey) { 
				seqID = splittedHeader[TrainingDataGenerator.UniProtIDField];
			}
			
			String labelField;
			int classLabel = 0;
			if (superPred) {
				labelField = splittedHeader[TrainingDataGenerator.TransfacClassField];
				classLabel = Integer.parseInt(labelField.substring(0, 1));
			
			} else {
				labelField = splittedHeader[TrainingDataGenerator.ProteinClassField];
				if (labelField.equals("TF")) {
					classLabel = 1;
				} else if (labelField.equals("NonTF")) {
					classLabel = -1;
				} else {
					System.out.println("Error. Unknown label in FASTA header: " + labelField);
					System.exit(0);
				}
			}
			
			tf2superclass.put(seqID, classLabel);
		}
		return(tf2superclass);
	}
}
