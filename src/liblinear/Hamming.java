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
package liblinear;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 */
public class Hamming {

	/**
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public int distance(int x, int y) {
		int weight = 0; // Anzahl 1-Bits, beginnt mit 0
		int word = x ^ y; // Ermittle alle unterschiedlichen Bits

		while (word > 0) { // Solange noch Bits vorhanden sind...
		      weight++;
		      word &= word - 1;
		}
		
		return weight; // Rückgabe der Anzahl der Bits
	}
	
}
