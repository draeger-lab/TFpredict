package ipr;
/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

import java.util.ArrayList;

public class IprRaw {
	
	String domain_id;
	// domains positions are represented in the form "DomainID  start_pos  end_pos"
	String domain_pos;
	// domains names are obtained from "description" fields returned by InterProScan
	String[] domain_names;
	ArrayList<String> go_terms;
	
	
	public IprRaw() {
	}
	
	public IprRaw(String id, String bind, String[] doms, ArrayList<String> gos) {
		this.domain_id = id;
		this.domain_pos = bind;
		this.domain_names = doms;
		this.go_terms = gos;
		
	}

}
