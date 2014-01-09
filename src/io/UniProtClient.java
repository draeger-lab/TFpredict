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
package io;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class UniProtClient {

	private boolean silent = true;
	
	public void setVerbose(boolean verbose) {
		silent = !verbose;
	}
	
	public String[] getUniProtSequences(String[] uniprot_ids) {
		return(getUniProtSequences(uniprot_ids, true));
	}
	
	public String[] getUniProtSequences(String[] uniprot_ids, boolean include_header) {
		
		String[] uniprot_seqs = new String[uniprot_ids.length];
		for (int i=0; i<uniprot_ids.length; i++) {
			uniprot_seqs[i] = getUniProtSequence(uniprot_ids[i], include_header);
		}
		return(uniprot_seqs);
	}
	
	// retrieves protein sequence for given UniProt ID or UniProt entry name
	public String getUniProtSequence(String uniprot_id) {
		return(getUniProtSequence(uniprot_id, true));
	}
	
	public String getUniProtSequence(String uniprot_id, boolean include_header) {
		
		Wget wget = new Wget();
		String url = "http://www.uniprot.org/uniprot/" + uniprot_id + ".fasta";
		if (!silent) {
			System.out.println("Fetching Data for: " + uniprot_id);
		}
	    String uniprot_seq = wget.fetchbuffered(url);
	    
	    // remove header of FASTA file (if desired)
	    if (uniprot_seq != null && !include_header) {
	    	uniprot_seq = uniprot_seq.replaceFirst(">.*\\n", "");
	    }
	    	
	    return(uniprot_seq);
	}
	
	public static void main(String[] args) {
		UniProtClient uniprot_client = new UniProtClient();
		String[] uniprotIDs = new String[] {"P00750", "A4_HUMAN", "P53_HUMAN", "CRAP"};
		String[] seqs = uniprot_client.getUniProtSequences(uniprotIDs, true);
		for (String seq: seqs) {
			System.out.println(seq);
		}
	}

}

