package features;

import io.BasicTools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class SequenceFeatureGenerator {
	
	private static boolean silent = false;
	private static ArrayList<String> allKmers = new ArrayList<String>();
	
	private static HashMap<String, HashMap<String, Integer>> getSpectrumFingerprints(HashMap<String, String> sequences, int kMin, int kMax) {
		
		HashMap<String, HashMap<String, Integer>> seq2fingerprint = new HashMap<String, HashMap<String, Integer>>();
		for (String header: sequences.keySet()) {
			
			int startIdx = header.indexOf("|")+1;
			String uniprotID = header.substring(startIdx, header.indexOf("|", startIdx));
			HashMap<String, Integer> fingerprint = getSpectrumFingerprint(sequences.get(header), kMin, kMax);
			
			seq2fingerprint.put(uniprotID, fingerprint);
		}
		return seq2fingerprint;
	}
	
	private static HashMap<String, Integer> getSpectrumFingerprint(String sequence, int kMin, int kMax) {
		
		HashMap<String, Integer> kmer2count = new HashMap<String, Integer>();
		
		for (int k=kMin; k<=kMax; k++) {
			for (int startIdx=0; startIdx <= (sequence.length() - k); startIdx++) {;	
				String currKmer = sequence.substring(startIdx, startIdx + k);
	
				if (kmer2count.containsKey(currKmer)) {
					kmer2count.put(currKmer, kmer2count.get(currKmer)+1);
				} else {
					kmer2count.put(currKmer, 1);
				}
			}
		}
		return kmer2count;
	}
	
	private static void printFingerprint(HashMap<String, Integer> map) {
		for (String key: map.keySet()) {
			System.out.println("  " + key + ": " + map.get(key));
		}
	}
	
	private static void getAllKmers(HashMap<String, HashMap<String, Integer>> seq2kmer) {
		
		HashSet<String> kmerSet = new HashSet<String>();
		ArrayList<String> kmerList = new ArrayList<String>();
		
		for (String seqID: seq2kmer.keySet()) {
			kmerSet.addAll(seq2kmer.get(seqID).keySet());
			kmerList.addAll(seq2kmer.get(seqID).keySet());
		}
		allKmers.addAll(kmerSet);
		Collections.sort(allKmers);
		
		HashMap<String, Integer> seqsPerKmer = new HashMap<String, Integer>();
		for (String kmer: allKmers) {
			
			int numOcc = Collections.frequency(kmerList, kmer);
			//System.out.println(kmer + ": " + numOcc);
			seqsPerKmer.put(kmer, numOcc);
		}
		
		if (!silent) System.out.println("Number of unique k-mers in all sequences: " + allKmers.size());
	}
	
	
	
	private static String getFeatVec(HashMap<String, Integer> kmer2count) {
		
		StringBuffer featVec = new StringBuffer();
		for (String kmer: kmer2count.keySet()) {
			int pos = allKmers.indexOf(kmer);
			int count = kmer2count.get(kmer);
			
			featVec.append(pos + ":" + count + " ");
		}
		return featVec.toString().trim();
	}
	
	private static HashMap<String, String> getAllFeatVec(HashMap<String, HashMap<String, Integer>> seq2kmer) {
		
		HashMap<String, String> seq2feat = new HashMap<String, String>();
		for (String seqID: seq2kmer.keySet()) {

			String featVector = getFeatVec(seq2kmer.get(seqID));
			//System.out.println(seqID + ": " + featVector);
			seq2feat.put(seqID, featVector);
			
		}
		
		return(seq2feat);
	}
	
	public static void main(String[] args) {
		
		String inputFile = "/rahome/eichner/projects/tfpredict/data/super_pred/fasta_files/superclassTF.fasta";
		HashMap<String, String> uniprot2seq = BasicTools.readFASTA(inputFile, true);
		HashMap<String, HashMap<String, Integer>> seq2kmer = getSpectrumFingerprints(uniprot2seq, 2, 2);
		getAllKmers(seq2kmer);
		
		HashMap<String, String> seq2feat = getAllFeatVec(seq2kmer);
	}
}
