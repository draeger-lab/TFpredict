/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2012 ZBIT, University of Tübingen, Florian Topf and Johannes Eichner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	public boolean isTF;
	public Integer superclass = null;
	public ArrayList<String> domain_ids = new ArrayList<String>();
	public ArrayList<String> domain_pos = new ArrayList<String>();
	
	public IprEntry() {
	}
	
	
	public IprEntry(String id) {
		this.sequence_id = id;
	}
	
	public IprEntry(String id, boolean label) {
		this(id);
		this.isTF = label;
	}
	
	public IprEntry(String id, boolean label, ArrayList<String> iprs) {
		this(id, label);
		this.domain_ids = iprs;
	}
	
	public IprEntry(String id, boolean label, String ipr) {
		this(id, label);
		domain_ids.add(ipr);
	}
	
	public IprEntry(String id, boolean label, String ipr, String ipr_pos) {
		this(id, label, ipr);
		domain_pos.add(ipr_pos);
	}
	
	public IprEntry(String id, String ipr) {
		this(id);
		domain_ids.add(ipr);
	}
	
	public IprEntry(String id, String ipr, String ipr_pos) {
		this(id, ipr);
		domain_pos.add(ipr_pos);
	}
}
