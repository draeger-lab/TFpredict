package features;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.StringTokenizer;

import main.TFpredictMain;

import resources.Resource;

public class PseAACGenerator {
	
	public double weight = 0.05;
    public int lambda = 10;
  
	private static String aa_attr = "aa_attr.txt";
	
	//TODO
	private static HashMap<String, String[]> readAttributes(String aa_attr) {
		
		HashMap<String, String[]> AAmap = new HashMap<String, String[]>();
		
		String line = null;
		String res = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(Resource.class.getResourceAsStream(PseAACGenerator.aa_attr)));
						
			while((line = br.readLine()) != null) {	
				String[] tmp = line.split("\t");
								
			}

			br.close();
		
		} catch (IOException e) {		
			System.out.println("IOException caught while fetching class."); 
		}
		
				
		return AAmap;
	}
	

	//TODO
	private String buildPseAAC(String sequence) {
		String pseVector = null;
		
        int length = sequence.length();

        return pseVector;
    }
   
	
		
	public static void main(String[] args) {
		
		String sequence = "MRSSAKQEELVKAFKALLKEEKFSSQGEIVAALQEQGFDNINQSKVSRMLTKFGAVRTRNAKMEMVYCLPAELGVPTTSSPLKNLVLDIDYNDAVVVIHTSPGAAQLIARLLDSLGKAEGILGTIAGDDTIFTTPANGFTVKDLYEAILELFDQEL";
		
		PseAACGenerator PseAAC = new PseAACGenerator();
		
		HashMap<String, String[]> AAmap = readAttributes(aa_attr);
		
		String pseVector = PseAAC.buildPseAAC(sequence);
		
		//System.out.println("Input:\t"+ sequence + "\n" + "PseAAC:\t" + pseVector + "\n" + "Contr:\t" + "5.390	0.337	3.032	4.042	2.358	3.369	0.337	3.032	3.705	6.400	1.347	2.021	1.684	2.358	1.684	3.369	3.369	3.705	0.000	1.011	4.998	5.065	4.598	4.587	4.812	5.047	4.231	4.970	4.472	4.670");
		
	}
	
}
