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

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests if the Hamming distance function can be correctly computed.
 * 
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 */
public class HammingTest {

	private Hamming h;
	
	/**
	 * Initialize distance function.
	 * 
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {
		h = new Hamming();
	}

	/**
	 * Perform some tests.
	 */
	@Test
	public void test() {
		String x = "00110";
		String y = "00101";
		String z = "01110";
		
		assertEquals(1, h.distance(Integer.parseInt(x, 2), Integer.parseInt(z, 2)));
		assertEquals(2, h.distance(Integer.parseInt(x, 2), Integer.parseInt(y, 2)));
		assertEquals(3, h.distance(Integer.parseInt(y, 2), Integer.parseInt(z, 2)));
		
		assertEquals(1, h.distance(Integer.parseInt("00110", 2), Integer.parseInt("00100", 2)));
	}

}
