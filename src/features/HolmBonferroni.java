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
package features;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
public class HolmBonferroni {

	public List<Double> correct(List<String> iprs, List<Double> pvalues) {
		
		// sort minimum to maximum on basis of pvalue
		List<IprPsort> iprPsort = integrate(iprs, pvalues);

		int size = iprPsort.size();
		
		for (int i = 0; i < iprPsort.size(); i++) {
			
			String curr_ipr = iprPsort.get(i).Ipr;
			double curr_pvalue = iprPsort.get(i).pVal;
			
			////
			curr_pvalue *= size - i; 

			if (curr_pvalue > 1.0) curr_pvalue = 1.0;
			////
			
			IprPsort iprp = new IprPsort();
			iprp.Ipr = curr_ipr;
			iprp.pVal = curr_pvalue;
			
			iprPsort.set(i, iprp);
			
		}
		
		// restore sorting on basis of IPRXXXXXX
		pvalues = deintegrate(iprPsort);

		return pvalues;
	}

	
	// sorts iprs and pvalues on the basis of pvalue
	private List<IprPsort> integrate(List<String> iprs,
			List<Double> pvalues) {

		List<IprPsort> iprps = new ArrayList<IprPsort>();
		
		// create sortable object
		for (int i = 0; i < iprs.size(); i++) {
			
			String curr_ipr = iprs.get(i);
			double curr_pvalue = pvalues.get(i);
			
			IprPsort iprp = new IprPsort();
			iprp.Ipr = curr_ipr;
			iprp.pVal = curr_pvalue;
			
			iprps.add(iprp);
		}
		
		Collections.sort(iprps);

		return iprps;
	}
	
	
	// unsort ipr and values, recreate pvalues-arraylist
	private List<Double> deintegrate(List<IprPsort> iprps) {
		
		List<IprPunsort> iprPunsort = new ArrayList<IprPunsort>();
		
		for (int i = 0; i < iprps.size(); i++) {
			
			String curr_ipr = iprps.get(i).Ipr;
			double curr_pvalue = iprps.get(i).pVal;
			
			IprPunsort iprp = new IprPunsort();
			iprp.Ipr = curr_ipr;
			iprp.pVal = curr_pvalue;
			
			iprPunsort.add(iprp);
			
		}
		
		Collections.sort(iprPunsort);
		
		List<Double> pvalues = new ArrayList<Double>();
		// recreate pvalues-arraylist
		for (int i = 0; i < iprPunsort.size(); i++) {
			
			double curr_pvalue = iprPunsort.get(i).pVal;
			
			pvalues.add(curr_pvalue);
		}
		return pvalues;
	}
}

class IprPsort implements Comparable<Object>{

	double pVal;
	String Ipr;
	
	@Override
	public int compareTo(Object arg0) {
		
		return new Double(pVal).compareTo(((IprPsort)arg0).pVal); 
	}
}

class IprPunsort implements Comparable<Object>{

	double pVal;
	String Ipr;
	
	@Override
	public int compareTo(Object arg0) {
		
		return new String(Ipr).compareTo(((IprPunsort)arg0).Ipr); 
	}
}
