/**
 * 
 */
package io;

import java.util.Map;

/**
 * @author draeger
 *
 */
public class FASTAReaderTest {

	/**
	 * 
	 * @param args path to a FASTA file.
	 */
	public static void main(String args[]) {
		Map<String, String> sequences = BasicTools.readFASTA(args[0]); // /rahome/eichner/web_home/test_seq.fasta

		for (String header: sequences.keySet()) {
			System.out.println("> " + header);
			System.out.println(sequences.get(header));
		}
	}

}
