/*  
 * $Id: IprRaw.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/ipr/IprRaw.java $
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

import java.util.List;

/**
 * 
 * @author Florian Topf
 * @version $Rev: 99 $
 * @since 1.0
 */
public class IprRaw {
	
	public String domain_id;
	public String[] domain_names;
	public List<String> go_terms;
	
	/**
	 * 
	 */
	public IprRaw() {
		super();
	}
	
	/**
	 * 
	 * @param id
	 * @param doms
	 * @param gos
	 */
	public IprRaw(String id, String[] doms, List<String> gos) {
		this.domain_id = id;
		this.domain_names = doms;
		this.go_terms = gos;
		
	}
}
