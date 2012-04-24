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
