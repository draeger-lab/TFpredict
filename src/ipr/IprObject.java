package ipr;

import java.util.ArrayList;

public class IprObject {
	
	String id;
	String bind;
	String[] doms;
	ArrayList<String> gos;
	
	
	public IprObject() {
	}
	
	public IprObject(String id, String bind, String[] doms, ArrayList<String> gos) {
		this.id = id;
		this.bind = bind;
		this.doms = doms;
		this.gos = gos;
		
	}

}
