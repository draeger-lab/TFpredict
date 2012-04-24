/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2009 ZBIT, University of Tübingen, Johannes Eichner

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
