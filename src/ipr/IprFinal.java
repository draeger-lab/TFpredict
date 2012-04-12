package ipr;

import java.util.ArrayList;

public class IprFinal {
	
	String id;
	public ArrayList<String> bindings;
	public String transfac;
	
	
	public IprFinal() {
	}
	
	public IprFinal(String id, ArrayList<String> bindings, String transfac) {
		this.id = id;
		this.bindings = bindings;
		this.transfac = transfac;
	}

}
