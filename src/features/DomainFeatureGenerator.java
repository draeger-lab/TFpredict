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
import io.FTPsupport;
import ipr.IprEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import modes.Predict;

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
	protected List<String> relevantDomainIDs = new ArrayList<String>();
	protected Map<String, IprEntry> seq2domain = new HashMap<String, IprEntry>();
	
	protected static List<String> filterCurrentDomainsInSet(Set<String> domainSet, List<String> currentDomains) {
		List<String> domainsList = new ArrayList<String>();
		domainsList.addAll(domainSet);
		domainsList = BasicTools.intersect(domainsList, currentDomains);
		
		return(domainsList);
	}
	
	protected static Map<String, List<String>> filterCurrentDomainsInDom2SeqMap(Map<String, List<String>> domain2seq, List<String> currDomains) {
	    
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
	
	protected static List<String> downloadAllDomainIDs() {
		
		// retrieve file with InterPro domain IDs and descriptions via FTP site
		FTPsupport ftp = new FTPsupport();
		String domainDescFile = ftp.download(domainDescriptionURL);
		
		// parse all known InterPro domain IDs
		Map<String, String> domain2desc = BasicTools.readFile2Map(domainDescFile);
		List<String> allDomainIDs = new ArrayList<String>();
		allDomainIDs.addAll(domain2desc.keySet());
		Collections.sort(allDomainIDs);
		
		return(allDomainIDs);
	}
	
	/**
	 * 
	 * @param fastaFileHeaders
	 * @param superPred
	 * @param useUniprotIDasKey
	 * @return
	 */
	public static Map<String, Integer> getLabelsFromFastaHeaders(Set<String> fastaFileHeaders, boolean superPred, boolean useUniprotIDasKey) {
		
		Map<String, Integer> tf2superclass = new HashMap<String, Integer>();
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
					classLabel = Predict.TF;
				} else if (labelField.equals("NonTF")) {
					classLabel = Predict.Non_TF;
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
