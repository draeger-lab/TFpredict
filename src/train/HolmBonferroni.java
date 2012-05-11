/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

package train;
import java.util.ArrayList;
import java.util.Collections;

public class HolmBonferroni {

	public ArrayList<Double> correct(ArrayList<String> iprs, ArrayList<Double> pvalues) {
		
		// sort minimum to maximum on basis of pvalue
		ArrayList<IprPsort> iprPsort = integrate(iprs, pvalues);

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
	private ArrayList<IprPsort> integrate(ArrayList<String> iprs,
			ArrayList<Double> pvalues) {

		ArrayList<IprPsort> iprps = new ArrayList<IprPsort>();
		
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
	private ArrayList<Double> deintegrate(ArrayList<IprPsort> iprps) {
		
		ArrayList<IprPunsort> iprPunsort = new ArrayList<IprPunsort>();
		
		for (int i = 0; i < iprps.size(); i++) {
			
			String curr_ipr = iprps.get(i).Ipr;
			double curr_pvalue = iprps.get(i).pVal;
			
			IprPunsort iprp = new IprPunsort();
			iprp.Ipr = curr_ipr;
			iprp.pVal = curr_pvalue;
			
			iprPunsort.add(iprp);
			
		}
		
		Collections.sort(iprPunsort);
		
		ArrayList<Double> pvalues = new ArrayList<Double>();
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
