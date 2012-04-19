package ipr;

import java.util.ArrayList;
import java.util.HashMap;


/*
 * This class contains functions for the post-processing of the results obtained from InterProScan
 * - filters InterPro domains based on child GO terms of "DNA-binding" to identify DNA-binding domains
 * - uses static mapping from TF names to TransFac classes for structural class annotation
 *   (TF names are parsed from descriptions of predicted InterPro domains)
 */

public class IPRprocess {

	public static HashMap<String, IprProcessed> filterIPRdomains(HashMap<String,IprEntry> seq2domain, HashMap<String,IprRaw> IPRdomains, ArrayList<String> relGOterms, HashMap<String, String> tfName2class) {
		
		HashMap<String, IprProcessed> seq2bindingDomain = new HashMap<String, IprProcessed>();
	
		
		for (String sequence_id: seq2domain.keySet()) {
			ArrayList<String> curr_domains = seq2domain.get(sequence_id).domain_ids;
			
			for (String domain_id : curr_domains) {
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
						ArrayList<String> bindingDomains = new ArrayList<String>();
						bindingDomains.add(iprDomain.domain_pos);
						IprProcessed currDBD = new IprProcessed(sequence_id, bindingDomains, transfacClass);
						seq2bindingDomain.put(sequence_id, currDBD);
					}
					else {
						IprProcessed currDBD = seq2bindingDomain.get(sequence_id);
						currDBD.anno_transfac_class = mergeTransfacClassAnnos(currDBD.anno_transfac_class, transfacClass);
						if (!currDBD.binding_domains.contains(iprDomain.domain_pos)) {
							currDBD.binding_domains.add(iprDomain.domain_pos);
						}
						seq2bindingDomain.put(sequence_id, currDBD);
					}
				}
			}
		}
		return seq2bindingDomain;
	}


	// returns the more specific class given two Transfac classes (i.e., x.x.x.x.x)
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
		
	private static String getTransfacClassAnno(String[] domain_names, HashMap<String, String> tfName2class) {
		
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
