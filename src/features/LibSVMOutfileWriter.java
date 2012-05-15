/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

package features;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import modes.Predict;
import ipr.IprEntry;


/*
 * writes data in libsvm-file-format
 */
public class LibSVMOutfileWriter {

	// clean version
	public int[] write(ArrayList<String> domainIDs, HashMap<String, IprEntry> seq2domain, String outfile) {
		
		ArrayList<String> libsvmFeatureTable = new ArrayList<String>();

		// check if feature file for superclass prediction shall be generated
		boolean superclassFeatures = false;
		String firstKey = seq2domain.keySet().iterator().next();
		if (seq2domain.get(firstKey).superclass != null) {
			superclassFeatures = true;
		}
		
		int[] numFeatureVectors = null;
		if (superclassFeatures) {
			numFeatureVectors = new int[] {0,0,0,0,0};
		} else {
			numFeatureVectors = new int[] {0,0};
		}
		
		String label;
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			
			for (String currSeq : seq2domain.keySet()) {
				
				IprEntry curr_entry = seq2domain.get(currSeq);
				if (curr_entry.isTF) {
					
					// Superclass prediction
					if (superclassFeatures) {
						label = Integer.toString(curr_entry.superclass);

					// TF prediction (TF)
					} else {
						label = "+1";
					}
					
				// TF prediction (non-TF)
				} else {
					label = "-1";
				}
				
				String fvector = Predict.createIPRvector(curr_entry.domain_ids, domainIDs, 10);
				String line = label + " " + fvector + "\n";
				if (!fvector.isEmpty() && !libsvmFeatureTable.contains(line)) {
					libsvmFeatureTable.add(line);
					bw.write(line);
					
					if (curr_entry.isTF) {
						if (superclassFeatures) {
							numFeatureVectors[curr_entry.superclass]++;
						} else {
							numFeatureVectors[0]++;
						}
					} else {
						numFeatureVectors[1]++;
					}
				}
			}
			bw.flush();
			bw.close();
		}
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while writing output file");
		}
		return numFeatureVectors;
	}
}
