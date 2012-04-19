package ipr;
/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */
import java.io.Serializable;
import java.util.ArrayList;


public class IprEntry implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public String sequence_id;
	public boolean label;
	public ArrayList<String> domain_ids = new ArrayList<String>();
	
	public boolean isLabel() {
		return label;
	}
	
	public IprEntry() {
	}
	
	public IprEntry(String id) {
		this.sequence_id = id;
	}
	
	public IprEntry(String id, boolean label) {
		this.sequence_id = id;
		this.label = label;
	}
	
	public IprEntry(String id, boolean label, ArrayList<String> iprs) {
		this.sequence_id = id;
		this.label = label;
		this.domain_ids = iprs;
	}
	
	public IprEntry(String id, boolean label, String ipr) {
		this.sequence_id = id;
		this.label = label;
		domain_ids.add(ipr);
	}
	
	public IprEntry(String id, String ipr) {
		this.sequence_id = id;
		domain_ids.add(ipr);
	}

}
