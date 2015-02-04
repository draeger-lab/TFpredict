/**
 * 
 */
package io;

import java.io.File;
import java.io.IOException;

/**
 * @author draeger
 *
 */
public class BLASTparserTest {

	/**
	 * 
	 * @param args
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static void main(String args[]) throws NumberFormatException, IOException {
		BasicTools.parseBLASTHits(new File(BLASTparserTest.class.getResource(args[0]).getFile()));
	}

}
