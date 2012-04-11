package ipr;
import java.io.Serializable;
import java.util.ArrayList;


public class IprEntry implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public String id;
	public boolean label;
	public ArrayList<String> iprs = new ArrayList<String>();
	
	public boolean isLabel() {
		return label;
	}
	
	public IprEntry() {
	}
	
	public IprEntry(String id) {
		this.id = id;
	}
	
	public IprEntry(String id, boolean label) {
		this.id = id;
		this.label = label;
	}
	
	public IprEntry(String id, boolean label, ArrayList<String> iprs) {
		this.id = id;
		this.label = label;
		this.iprs = iprs;
	}
	
	public IprEntry(String id, boolean label, String ipr) {
		this.id = id;
		this.label = label;
		iprs.add(ipr);
	}
	
	public IprEntry(String id, String ipr) {
		this.id = id;
		iprs.add(ipr);
	}

}
