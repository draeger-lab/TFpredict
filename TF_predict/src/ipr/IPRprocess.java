package ipr;

import java.util.ArrayList;
import java.util.HashMap;


public class IPRprocess {
	
	ArrayList<String> unmapped = new ArrayList<String>();

	public HashMap<String, IprFinal> process(ArrayList<IprObject> entries, ArrayList<String> goterms, HashMap<String, String> transHM) {
		
		HashMap<String, IprFinal> id2entry = new HashMap<String,IprFinal>();
		
		int count = 0;
		
		for (IprObject ipro : entries) {
			boolean has_relevant_go = false;
			for (String go : goterms) {
				if (ipro.gos.contains(go)) {
					// relevant found
					count++;
					has_relevant_go = true;
					
				}
			}
			if (has_relevant_go) {
				
				String id = ipro.id;
				String transfac = mapTrans(ipro.doms, transHM);

				if (!id2entry.containsKey(id)) {
					ArrayList<String> bindings = new ArrayList<String>();
					bindings.add(ipro.bind);
					IprFinal iprf = new IprFinal(id, bindings, transfac);
					id2entry.put(id, iprf);
				}
				else {
					IprFinal iprf = id2entry.get(id);
					iprf.transfac = mergeTrans(iprf.transfac, transfac);
					if (!iprf.bindings.contains(ipro.bind)) iprf.bindings.add(ipro.bind);
					id2entry.put(id, iprf);
				}
			}
		}
		System.out.println(count+" relevant entrie(s) processed.");
		System.out.println(id2entry.size()+" unique entrie(s) found.");
		System.out.println("Missing annotations:");
		for (String missing : unmapped) System.out.println(missing);
		return id2entry;
	}


	////
	// really primitive functions for transfac
		private String mergeTrans(String transfac, String transfac2) {
		
		String max = "";

		if (transfac.length() > transfac2.length()) {
			max = transfac;
		}
		else {
			max = transfac2;
		}
		
		return max;
	}
		
	private String mapTrans(String[] doms, HashMap<String, String> transHM) {
		
		String max = "";
		String hit = "";
		for (int i = 0; i < doms.length; i++) {
			
			String entry = doms[i].replace(" domain", " ").replace("-domain", " ").replaceAll("[()]", "").toUpperCase().replace("WINGED HELIX", "").replaceAll("-", " ").replaceAll("_[1-3]", "").replaceAll("RELATED", "").trim();
			
			if (transHM.containsKey(entry)) {
				hit = transHM.get(entry);
				if (hit.length() > max.length()) {
					max = hit;
				}
			}
			else {
				if (!unmapped.contains(entry)) unmapped.add(entry);
			}
		}
		return max;
	}
	////

}
