/*  
 * $Id$
 * $URL$
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2013 Center for Bioinformatics Tuebingen (ZBIT),
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

import java.util.List;

/**
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
public class IprProcessed {
	
	public String sequence_id;
	public List<String> binding_domains;
	public String anno_transfac_class;
	
	/**
	 * 
	 * @param id
	 * @param bindings
	 * @param transfac
	 */
	public IprProcessed(String id, List<String> bindings, String transfac) {
		this.sequence_id = id;
		this.binding_domains = bindings;
		this.anno_transfac_class = transfac;
	}

}
