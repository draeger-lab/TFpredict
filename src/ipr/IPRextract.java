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
package ipr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This class contains functions for parsing the standard output of InterProScan
 * - parses InterPro IDs                                                (needed for generation of feature vectors)
 * - parses start and end positions of each predicted InterPro domain   (needed for post-processing with SABINE)
 * - parses GO terms associated with each InterPro domain               (needed to filter DNA-binding domains)
 * - parses InterPro domain descriptions                                (needed for TF class annotation via TransFac)
 *
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class IPRextract {
	
	public static Map<String, IprEntry> getSeq2DomainMap(List<String[]> IPRoutput) {
		return(getSeq2DomainMap(IPRoutput, null));
	}
	
	public static Map<String, IprEntry> getSeq2DomainMap(List<String[]> IPRoutput, Boolean label) {

		Map<String, IprEntry> seq2domain = new HashMap<String, IprEntry>();
		
		for (int i=0; i < IPRoutput.size(); i++) {
			String[] domain_entry = IPRoutput.get(i);
			if (domain_entry.length < 12) continue;
			String sequence_id = domain_entry[0].trim();
			String domain_id = domain_entry[11].trim();
			int domain_start = Integer.parseInt(domain_entry[6]);
			int domain_end = Integer.parseInt(domain_entry[7]);
			int curr_domain_length = domain_end - domain_start + 1;
			String domain_interval = domain_id + "    " + domain_start + "\t" + domain_end;
			
			// skip domains for which no InterPro-ID is given
			if (domain_id.equals("NULL")) {
				System.out.println("WARNING: Skipped domain " + domain_id + " with missing InterPro-ID");
			  continue;
			}
			
			IprEntry curr_entry;
			if (seq2domain.containsKey(sequence_id)) {
				curr_entry = seq2domain.get(sequence_id);
				
				if (!curr_entry.domain_ids.contains(domain_id)) {
					curr_entry.domain_ids.add(domain_id);
					curr_entry.domain_pos.add(domain_interval);
				
				// use longest instance of domain if multiple intervals were found for the same domain ID
				} else {
					int idx = curr_entry.domain_ids.indexOf(domain_id);
					int old_domain_length = getDomainLength(curr_entry.domain_pos.get(idx));
					if (curr_domain_length > old_domain_length) {
						curr_entry.domain_pos.set(idx, domain_interval);
					}
				}
				seq2domain.put(sequence_id, curr_entry);
			
			} else {
				if (label == null) {
					curr_entry = new IprEntry(sequence_id, domain_id, domain_interval);
				} else {
					curr_entry = new IprEntry(sequence_id, label, domain_id, domain_interval);
				}
				seq2domain.put(sequence_id, curr_entry);
			}
		}
		return seq2domain;
	}
	
	private static int getDomainLength(String domain_pos) {
		StringTokenizer strtok = new StringTokenizer(domain_pos);
		strtok.nextToken();   // skip domain ID (e.g., IPR9381)
		int domainStart = Integer.parseInt(strtok.nextToken());
		int domainEnd = Integer.parseInt(strtok.nextToken());
		int domainLength = domainEnd - domainStart + 1; 
		return domainLength;
	}
	
	public static Map<String, List<String>> getDomain2SeqMap(List<String[]> IPRoutput) {
		
		Map<String, List<String>> domain2seq = new HashMap<String, List<String>>();
		
		for (int i = 0; i < IPRoutput.size(); i++) {
			
			String[] domain_entry = IPRoutput.get(i);
			String sequence_id = domain_entry[0].trim();
			String domain_id = domain_entry[11].trim();
			
			// skip domains for which no InterPro-ID is given
			if (domain_id.equals("NULL")) {
				continue;
			}

			List<String> currSeqIDs;
			if (domain2seq.containsKey(domain_id)) {
				currSeqIDs = domain2seq.get(domain_id);
				if (!currSeqIDs.contains(sequence_id)) {
					currSeqIDs.add(sequence_id);
					domain2seq.put(domain_id, currSeqIDs);
				}
			}
			else {
				currSeqIDs = new ArrayList<String>();
				currSeqIDs.add(sequence_id);
				domain2seq.put(domain_id, currSeqIDs);
			}
		}
		return domain2seq;
	}
	
	
	public static Map<String, IprRaw> parseIPRoutput(List<String[]> IPRoutput) {
		
		Map<String, IprRaw> ipr_domains = new HashMap<String, IprRaw>();
		
		for (int i = 0; i < IPRoutput.size(); i++) {
			
			String[] domain_entry = IPRoutput.get(i);
					
			if (domain_entry.length < 13) {
			  continue;
			}
			String domain_id = domain_entry[11].trim();
			// skip domains for which no InterPro-ID is given
			// skip domain IDs which were already added
			if (ipr_domains.containsKey(domain_id)) {
				continue;
			}
			
			String domain_name1 = domain_entry[5].trim().split(",")[0].trim();
			String domain_name2 = domain_entry[12].trim().split(",")[0].trim();
			
			
			// parse domain names from description fields (used for TF class annotation via TransFac)
			HashSet<String> domain_names_set = new HashSet<String>();
			domain_names_set.add(domain_name1);
			domain_names_set.add(domain_name2);
			domain_names_set.remove("no description");
			String[] domain_names = domain_names_set.toArray(new String[]{});
			
			// check if GO terms are available for current domain
			List<String> domain_GOterms = null;
			if (domain_entry.length == 14) {
				String domain_go = domain_entry[13].trim();
				
				// parse GO terms for current InterPro domain entry			
				domain_GOterms = new ArrayList<String>(); 
				for (String go_entry : domain_go.split("\\|")) {
				  domain_GOterms.add(go_entry);
				}
			}
			IprRaw ipr_domain = new IprRaw(domain_id, domain_names, domain_GOterms);
			ipr_domains.put(domain_id, ipr_domain);
		}
		return ipr_domains;
	}

	public static Map<String, IprRaw> parseIPRoutputProk(List<String[]> IPRoutput) {

		Map<String, IprRaw> ipr_domains = new HashMap<String, IprRaw>();

		for (int i = 0; i < IPRoutput.size(); i++) {

			String[] domain_entry = IPRoutput.get(i);

			String domain_id = domain_entry[11].trim();
			// skip domains for which no InterPro-ID is given
			if (domain_id.equals("NULL")) {
				continue;
			}
			// skip domain IDs which were already added
			if (ipr_domains.containsKey(domain_id)) {
				continue;
			}

			String domain_name1 = domain_entry[5].trim().split(",")[0].trim();
			String domain_name2 = domain_entry[12].trim().split(",")[0].trim();


			// parse domain names from description fields (used for TF class annotation via TransFac)
			HashSet<String> domain_names_set = new HashSet<String>();
			domain_names_set.add(domain_name1);
			domain_names_set.add(domain_name2);
			domain_names_set.remove("no description");
			String[] domain_names = domain_names_set.toArray(new String[]{});

			// check if GO terms are available for current domain
			List<String> domain_GOterms = null;
			if (domain_entry.length == 14) {
				String domain_go = domain_entry[13].trim();

				// parse GO terms for current InterPro domain entry
				domain_GOterms = new ArrayList<String>();
				for (String go_entry : domain_go.split(",")) {

					if (go_entry.contains("GO:")) {
						int start_pos = go_entry.indexOf("(GO:")+1;
						int end_pos = go_entry.indexOf(")", start_pos);

						go_entry = go_entry.substring(start_pos, end_pos);
						domain_GOterms.add(go_entry);
					}
				}
			}
			IprRaw ipr_domain = new IprRaw(domain_id, domain_names, domain_GOterms);
			ipr_domains.put(domain_id, ipr_domain);
		}
		return ipr_domains;
	}
	

}
