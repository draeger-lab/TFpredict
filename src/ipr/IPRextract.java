/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2009 ZBIT, University of Tübingen, Johannes Eichner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ipr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.sound.sampled.Line;

/*
 * 
 * This class contains functions for parsing the standard output of InterProScan
 * - parses InterPro IDs                                                (needed for generation of feature vectors)
 * - parses start and end positions of each predicted InterPro domain   (needed for post-processing with SABINE)
 * - parses GO terms associated with each InterPro domain               (needed to filter DNA-binding domains)
 * - parses InterPro domain descriptions                                (needed for TF class annotation via TransFac)
 */

public class IPRextract {
	
	public static HashMap<String, IprEntry> getSeq2DomainMap(ArrayList<String[]> IPRoutput) {

		HashMap<String, IprEntry> seq2domain = new HashMap<String, IprEntry>();
		
		for (int i=0; i < IPRoutput.size(); i++) {

			String[] domain_entry = IPRoutput.get(i);
			String sequence_id = domain_entry[0].trim();
			System.out.println(sequence_id);
			String domain_id = domain_entry[11].trim();
			
			// skip domains for which no InterPro-ID is given
			if (domain_id.equals("NULL")) {
				continue;
			}
			
			IprEntry curr_entry;
			if (seq2domain.containsKey(sequence_id)) {
				curr_entry = seq2domain.get(sequence_id);
				
				if (!curr_entry.domain_ids.contains(domain_id)) {
					curr_entry.domain_ids.add(domain_id);
				}
				seq2domain.put(sequence_id, curr_entry);
			}
			else {
				curr_entry = new IprEntry(sequence_id, domain_id);
				seq2domain.put(sequence_id, curr_entry);
			}
		}
		return seq2domain;
	}
	
	public static HashMap<String, IprRaw> parseIPRoutput(ArrayList<String[]> IPRoutput) {
		
		HashMap<String, IprRaw> ipr_domains = new HashMap<String, IprRaw>();
		
		for (int i = 0; i < IPRoutput.size(); i++) {
			
			String[] domain_entry = IPRoutput.get(i);
					
			String domain_id = domain_entry[11].trim();
			String domain_name1 = domain_entry[5].trim().split(",")[0].trim();
			String domain_name2 = domain_entry[12].trim().split(",")[0].trim();
			String domain_interval = domain_id + "    " + domain_entry[6].trim() + "\t" + domain_entry[7].trim();
			
			// parse domain names from description fields (used for TF class annotation via TransFac)
			HashSet<String> domain_names_set = new HashSet<String>();
			domain_names_set.add(domain_name1);
			domain_names_set.add(domain_name2);
			domain_names_set.remove("no description");
			String[] domain_names = domain_names_set.toArray(new String[]{});
			
			// check if GO terms are available for current domain
			ArrayList<String> domain_GOterms = null;
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
			IprRaw ipr_domain = new IprRaw(domain_id, domain_interval, domain_names, domain_GOterms);
			ipr_domains.put(domain_id, ipr_domain);
		}
		return ipr_domains;
	}
}
