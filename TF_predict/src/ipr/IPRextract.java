package ipr;

import java.util.ArrayList;

public class IPRextract {
	

	public ArrayList<IprObject> extract(ArrayList<String[]> input) {
		
		int count = 0;
		
		ArrayList<IprObject> entries = new ArrayList<IprObject>();
		
		for (int i = 0; i < input.size(); i++) {
			
			String[] entry = input.get(i);
			String ipr_go = "NULL";
			
			if (entry.length == 14) {
				ipr_go = entry[13].trim();
			}
			
			if (!ipr_go.contains("NULL")) {
				
				count++;
				
				// id
				String id = entry[0].trim();
				// binding side
				String bind = id+"    "+ entry[6].trim()+"\t"+entry[7].trim();
				
				// domains
				String[] doms = new String[2];
				String[] dom_arr5 = entry[5].trim().split(",");
				doms[0] = dom_arr5[0].trim();
				String[] dom_arr12 = entry[12].trim().split(",");
				doms[1] = dom_arr12[0].trim();
				

				// go terms
				ArrayList<String> gos = new ArrayList<String>(); 
				String[] go_tmp = ipr_go.split(",");
				for (String curr_go : go_tmp) {

					if (curr_go.contains("GO:")) {
						int start_pos = curr_go.indexOf("(GO:")+1;
						int end_pos = curr_go.indexOf(")", start_pos);
						
						curr_go = curr_go.substring(start_pos, end_pos);
						gos.add(curr_go);
					}
					
				}
				
				IprObject IprO = new IprObject(id, bind, doms, gos);
				entries.add(IprO);
				
			}
			
		}
		System.out.println(count+" entrie(s) extracted.");
		return entries;
	}

}
