/*  
 * $Id$
 * $URL$
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2014 Center for Bioinformatics Tuebingen (ZBIT),
 * University of Tuebingen by Johannes Eichner, Florian Topf, Andreas Draeger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package ipr;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
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
