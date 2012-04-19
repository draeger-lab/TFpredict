package ipr;

import java.util.ArrayList;

public class IprProcessed {
	
	String sequence_id;
	public ArrayList<String> binding_domains;
	public String anno_transfac_class;
	
	
	public IprProcessed() {
	}
	
	public IprProcessed(String id, ArrayList<String> bindings, String transfac) {
		this.sequence_id = id;
		this.binding_domains = bindings;
		this.anno_transfac_class = transfac;
	}

}
