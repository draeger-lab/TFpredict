package io;

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

