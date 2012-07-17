package features;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import features.PseAACGenerator.start;

import io.BasicTools;
import resources.Resource;

public class PseAACGenerator {
	
	public double weight = 0.05;
    public int lambda = 10;
  
	private static String aa_attr = "aa_attr.txt";
	
	private start start;
	public enum start {
	    Amino, Hpho, Hphi, Mass 
	}
	public void EnumSet(start Start) {
        this.start = Start;
    }
	
	private static HashMap<String, AAentry> readAttributes() {
		
		HashMap<String, AAentry> AAmap = new HashMap<String, AAentry>();
		
		ArrayList<String> lines = BasicTools.readResource2List(aa_attr);
				
		ArrayList<String[]> splittedLines = new ArrayList<String[]>();
		
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			if (!line.isEmpty()) splittedLines.add(line.split("\t"));
		}
		
		for (int i = 1; i <=20; i++) {
			
			String Amino	=	(splittedLines.get(0)[i]);
			double Hpho	=	Double.valueOf(splittedLines.get(1)[i]);
			double Hphi	=	Double.valueOf(splittedLines.get(2)[i]);
			int Mass 	=	Integer.valueOf(splittedLines.get(3)[i]);
			
			AAmap.put(Amino, new AAentry(Amino, Hpho, Hphi, Mass));
		}
		
		return AAmap;
	}
	

	//TODO http://sourceforge.net/projects/pseb/
	private String buildPseAAC(String sequence) {
		
		String pseVector = null;
		
        int length = sequence.length();
        
        /*
        int cntAttr = this.attrMasks.Length;

        String[] attrNames = this.queryAttrNames();
        double[] tmpResult = new double[lambda];

        double effAttr = (double)this.attrMasks.Sum();
        unsafe
        {
            for (int i = 0; i < lambda; i++)
            {
                int delay = i + 1;
                int skipped = 0;
                for (int j = 0; j < length - delay; j++)
                {
                    char sym1 = src[j];
                    char sym2 = src[j + delay];
                    if (!syms.Contains(sym1) || !syms.Contains(sym2))
                    {
                        skipped++;
                        continue;
                    }
                    double t_sum = 0;

                    foreach (String curID in effAttrList.Keys)
                    {
                        double delta = effAttrList[curID][sym1] - effAttrList[curID][sym2];
                        t_sum += delta * delta / effAttr;
                    }


                    tmpResult[i] += t_sum;
                }
                
                if (length-delay-skipped != 0)
                    tmpResult[i] /= (length - delay - skipped);
            }
        }
        */

        return pseVector;
    }
   
	
		
	public static void main(String[] args) {
		
		String sequence = "MRSSAKQEELVKAFKALLKEEKFSSQGEIVAALQEQGFDNINQSKVSRMLTKFGAVRTRNAKMEMVYCLPAELGVPTTSSPLKNLVLDIDYNDAVVVIHTSPGAAQLIARLLDSLGKAEGILGTIAGDDTIFTTPANGFTVKDLYEAILELFDQEL";
		
		PseAACGenerator PseAAC = new PseAACGenerator();
		
		HashMap<String, AAentry> AAmap = readAttributes();
		
		String pseVector = PseAAC.buildPseAAC(sequence);
		
		//System.out.println("Input:\t"+ sequence + "\n" + "PseAAC:\t" + pseVector + "\n" + "Contr:\t" + "5.390	0.337	3.032	4.042	2.358	3.369	0.337	3.032	3.705	6.400	1.347	2.021	1.684	2.358	1.684	3.369	3.369	3.705	0.000	1.011	4.998	5.065	4.598	4.587	4.812	5.047	4.231	4.970	4.472	4.670");
		
	}
	
}

