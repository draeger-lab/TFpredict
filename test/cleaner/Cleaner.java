/**
 * 
 */
package cleaner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.biojava3.core.sequence.ProteinSequence;
import org.biojava3.core.sequence.features.FeatureInterface;
import org.biojava3.core.sequence.io.FastaReaderHelper;


/**
 * @author draeger
 *
 */
public class Cleaner {


	private static final Logger logger = Logger.getLogger(Cleaner.class.getName());

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LinkedHashMap<String, ProteinSequence> proteinSequenceHashMap = FastaReaderHelper.readFastaProteinSequence(new File(args[0]));
		int found = 0, total = 0;
		Pattern pattern = Pattern.compile(".*[Tt]ranscription\\s*[Ff]actor.*");
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(args[1])));
		for (ProteinSequence protSequence : proteinSequenceHashMap.values()) {
			total++;
			if (protSequence.getAccession() != null) {
				try {
					if (protSequence.getOriginalHeader().contains("NonTF")) {
						ProteinSequence seq = getSequenceForId(protSequence.getAccession().toString());
						if (seq != null) {
							found++;
							if (pattern.matcher(seq.getOriginalHeader()).matches()) {
								System.out.println(seq.getOriginalHeader());
								bw.append(seq.getOriginalHeader());
								bw.newLine();
							}
						}
					}
				} catch (FileNotFoundException exc) {
					logger.log(Level.FINE, exc.getLocalizedMessage() != null ? exc.getLocalizedMessage() : exc.getMessage());
				}
			} else {
				System.out.println(protSequence.getOriginalHeader());
			}
		}
		bw.close();
		System.out.println("found:\t" + found);
		System.out.println("not found:\t" + (total - found));
	}

	private static ProteinSequence getSequenceForId(String uniProtId) throws Exception {
		URL uniprotFasta = new URL(String.format("http://www.uniprot.org/uniprot/%s.fasta", uniProtId));
		ProteinSequence seq = FastaReaderHelper.readFastaProteinSequence(uniprotFasta.openStream()).get(uniProtId);
		//System.out.printf("id : %s %s%n%s%n", uniProtId, seq, seq.getOriginalHeader());
		return seq;
	}

}
