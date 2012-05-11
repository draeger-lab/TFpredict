/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

package train;


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
		
		int numFeatureVectors = 0;
		int numTFfeatureVectors = 0;
		int numNonTFfeatureVectors = 0;
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			
			for (String currSeq : seq2domain.keySet()) {
				
				String label = "-1";
				IprEntry curr_entry = seq2domain.get(currSeq);
				if (curr_entry.label) {
					label = "+1";
				}
				
				String fvector = Predict.createIPRvector(curr_entry.domain_ids, domainIDs, 10);
				String line = label + " " + fvector + "\n";
				if (!fvector.isEmpty() && !libsvmFeatureTable.contains(line)) {
					libsvmFeatureTable.add(line);
					bw.write(line);
					
					numFeatureVectors++;
					if (curr_entry.label) {
						numTFfeatureVectors++;
					} else {
						numNonTFfeatureVectors++;
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
		return new int[] {numFeatureVectors, numTFfeatureVectors, numNonTFfeatureVectors};
	}
}
