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
package data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.StringTokenizer;

import resources.Resource;


import main.TFpredictMain;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class TFdataTools {
	
	
	public static String getTransfacClass(String class_id) {
		
		String CLASS_FORMAT =    "[0-4]\\p{Punct}[0-9][0-2]?\\p{Punct}";
		String CLASS_ID_FORMAT = "C00\\d\\d\\p{Punct}";
		
	    // Method 1: check whether last token is of the form X.Y.Z. ...
		StringTokenizer strtok_class = new StringTokenizer(class_id);
		String class_token = null;
		while (strtok_class.hasMoreTokens()) {
			class_token = strtok_class.nextToken();	
		}
		
		if((class_token.length() > 3 && class_token.substring(0,4).matches(CLASS_FORMAT))
		   || (class_token.length() > 4 && class_token.substring(0,5).matches(CLASS_FORMAT))) {
			
			return class_token;
		}
		
	    // Method 2: check whether class is obtainable by its class_id ...	
		if(class_id.length() >= 6 && class_id.substring(0,6).matches(CLASS_ID_FORMAT)) {
			class_id = class_id.substring(0,5);	
		}
		
		String line = null;
		String res = null;
		boolean found = false;
		
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(Resource.class.getResourceAsStream(TFpredictMain.classMappingFile)));
			
			while((line = br.readLine()) != null) {	
				StringTokenizer strtok = new StringTokenizer(line);
				res = strtok.nextToken();
				if(class_id.equals(strtok.nextToken())) {
					found = true;
					break;
				}	
			}
			br.close();
		
		} catch (IOException e) {		
			System.out.println("IOException caught while fetching class."); 
		}
		
		if(!found) {
			System.out.println("No classification found for \"" + class_id + "\". Aborting.");
			System.exit(0);
		}
		return res;
	}
	
	
	public static String[] convertPFMformat(ArrayList<String> stamp_pfm) {
		
		StringTokenizer strtok = new StringTokenizer(stamp_pfm.get(0));;

		DecimalFormat fmt = new DecimalFormat();
		DecimalFormatSymbols symbs = fmt.getDecimalFormatSymbols();
		symbs.setDecimalSeparator('.');
		fmt.setDecimalFormatSymbols(symbs);
		fmt.setMaximumFractionDigits(4);
		fmt.setMinimumFractionDigits(4);

		strtok.nextToken();
		float a = Float.parseFloat(strtok.nextToken().trim());
		float c = Float.parseFloat(strtok.nextToken().trim());
		float g	= Float.parseFloat(strtok.nextToken().trim());
		float t	= Float.parseFloat(strtok.nextToken().trim());
		float sum = a + c + g + t;

		String line1 = "" + fmt.format(a / sum);
		String line2 = "" + fmt.format(c / sum);
		String line3 = "" + fmt.format(g / sum);
		String line4 = "" + fmt.format(t / sum);

		for (int i=1; i<stamp_pfm.size(); i++) {
			strtok = new StringTokenizer(stamp_pfm.get(i));

			strtok.nextToken();                    			  // Index
			a = Float.parseFloat(strtok.nextToken().trim());  // A
			c = Float.parseFloat(strtok.nextToken().trim());  // C
			g = Float.parseFloat(strtok.nextToken().trim());  // G
			t = Float.parseFloat(strtok.nextToken().trim());  // T
			sum = a + c + g + t;

			line1 += "   " + fmt.format(a / sum);
			line2 += "   " + fmt.format(c / sum);
			line3 += "   " + fmt.format(g / sum);
			line4 += "   " + fmt.format(t / sum);

		}
		return new String[] {line1, line2, line3, line4};
	}
}
