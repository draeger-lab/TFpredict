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
package ipr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This class contains functions for the post-processing of the results obtained from InterProScan
 * - filters InterPro domains based on child GO terms of "DNA-binding" to identify DNA-binding domains
 * - uses static mapping from TF names to TransFac classes for structural class annotation
 *   (TF names are parsed from descriptions of predicted InterPro domains)
 *
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class IPRprocess {

	public static Map<String, IprProcessed> filterIPRdomains(Map<String,IprEntry> seq2domain, Map<String,IprRaw> IPRdomains, List<String> relGOterms, Map<String, String> tfName2class) {
		
		Map<String, IprProcessed> seq2bindingDomain = new HashMap<String, IprProcessed>();
		
		for (String sequence_id: seq2domain.keySet()) {
			List<String> curr_domains = seq2domain.get(sequence_id).domain_ids;
			List<String> curr_domain_pos = seq2domain.get(sequence_id).domain_pos;
			
			for (int i=0; i<curr_domains.size(); i++) {
				String domain_id = curr_domains.get(i);
				String domain_pos = curr_domain_pos.get(i);
				IprRaw iprDomain = IPRdomains.get(domain_id);
				
				// current domain is a DNA-binding domain?
				boolean has_relevant_go = false;
				for (String go : relGOterms) {
					if (iprDomain.go_terms != null && iprDomain.go_terms.contains(go)) {
						has_relevant_go = true;
					}
				}
				
				if (has_relevant_go) {
					
					// try to obtain class annotation from TransFac 
					String transfacClass = getTransfacClassAnno(iprDomain.domain_names, tfName2class);
	
					if (!seq2bindingDomain.containsKey(sequence_id)) {
						List<String> bindingDomains = new ArrayList<String>();
						bindingDomains.add(domain_pos);
						IprProcessed currDBD = new IprProcessed(sequence_id, bindingDomains, transfacClass);
						seq2bindingDomain.put(sequence_id, currDBD);
					}
					else {
						IprProcessed currDBD = seq2bindingDomain.get(sequence_id);
						currDBD.anno_transfac_class = mergeTransfacClassAnnos(currDBD.anno_transfac_class, transfacClass);
						if (!currDBD.binding_domains.contains(domain_pos)) {
							currDBD.binding_domains.add(domain_pos);
						}
						seq2bindingDomain.put(sequence_id, currDBD);
					}
				}
			}
		}
		return seq2bindingDomain;
	}


	/**
	 * @param transfacClass1
	 * @param transfacClass2
	 * @return the more specific class given two Transfac classes (i.e., x.x.x.x.x).
	 */
	private static String mergeTransfacClassAnnos(String transfacClass1, String transfacClass2) {
		
		String max = "";
		if (transfacClass1.length() > transfacClass2.length()) {
			max = transfacClass1;
		}
		else {
			max = transfacClass2;
		}
		return max;
	}
		
	/**
	 * 
	 * @param domain_names
	 * @param tfName2class
	 * @return
	 */
	private static String getTransfacClassAnno(String[] domain_names, Map<String, String> tfName2class) {
		
		String max = "";
		String hit = "";
		for (int i = 0; i < domain_names.length; i++) {
			
			String entry = domain_names[i].replace(" domain", " ").replace("-domain", " ").replaceAll("[()]", "").toUpperCase().replace("WINGED HELIX", "").replaceAll("-", " ").replaceAll("_[1-3]", "").replaceAll("RELATED", "").trim();
			
			if (tfName2class.containsKey(entry)) {
				hit = tfName2class.get(entry);
				
				// use the most specific TransFac class annotation (i.e., x.x.x.x.x)
				if (hit.length() > max.length()) {
					max = hit;
				}
			}
		}
		return max;
	}
}
