package modes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import io.FileParser;
import io.ObjectRW;
import ipr.IPRextract;
import ipr.IPRprocess;
import ipr.IprEntry;
import ipr.IprFinal;
import ipr.IprObject;

import org.apache.commons.cli.CommandLine;


import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.LibSVMLoader;

public class Predict {
	
	static String iprpath;
	
	static DecimalFormat df = new DecimalFormat(".00");
	
	static ArrayList<String> iprs;
	static ArrayList<String> superiprs;
	
	static ArrayList<String> goterms;
	
	static FileParser fp = new FileParser();

	@SuppressWarnings("unchecked")
	public static void main(CommandLine cmd) throws Exception {
		
		String seqfile = "";
		if(cmd.hasOption("predict")) {
			seqfile = cmd.getOptionValue("predict");
		}
		
		String trainfile = "";
		if(cmd.hasOption("i")) {
			trainfile = cmd.getOptionValue("i");
		}
		
		String superfile = "";
		if(cmd.hasOption("s")) {
			superfile = cmd.getOptionValue("s");
		}
		
		iprpath = "";
		if(cmd.hasOption("p")) {
			iprpath = cmd.getOptionValue("p");
		}
		
		if(cmd.hasOption("g")) {
			goterms = fp.parseGO(cmd.getOptionValue("g"));
		}
		
		String trans = "";
		if(cmd.hasOption("t")) {
			trans = cmd.getOptionValue("t");
		}
		HashMap<String,String> transHM = (HashMap<String, String>) ObjectRW.read(trans);
		
		// deserialize model
	    Classifier cls = (Classifier) weka.core.SerializationHelper.read(trainfile);
		//Classifier cls = (Classifier) ObjectRW.read(trainfile);
	    System.out.print("Classifier-Info:\n"+cls.toString());
	    
	    // superclass classifier
	    Classifier supercls = (Classifier) weka.core.SerializationHelper.read(superfile);
	    System.out.print("Classifier-Info:\n"+supercls.toString());
	    
	    // deserialize iprs
	    if(cmd.hasOption("r")) {
	    	iprs = (ArrayList<String>) ObjectRW.read(cmd.getOptionValue("r"));
		}
	    else {
	    	System.out.println("-r ipr object argument needed.");
			System.exit(1);
	    }
	    
	    if(cmd.hasOption("sr")) {
	    	superiprs = (ArrayList<String>) ObjectRW.read(cmd.getOptionValue("sr"));
		}
	    else {
	    	System.out.println("-sr superipr object argument needed.");
			System.exit(1);
	    }
	    
	    System.out.println("Number of significant IPRs: "+iprs.size());
	    
	    // execute iprscan and get results
	    ArrayList<String[]> IPRentries = runIPR(seqfile);
	    
	    // parse iprresults
	    //ArrayList<String[]> IPRentries = fp.parseIPRout(seqfile);
		
	    // extract iprresults
	    HashMap<String, IprEntry> id2entry = extract(IPRentries);
	    
	    
	    //// iprprocess subroutines (rip-off)
	    // process iprresults
	    IPRextract iprextract = new IPRextract();
		ArrayList<IprObject> entries = iprextract.extract(IPRentries);
		
		// process result
		IPRprocess iprprocess = new IPRprocess();
		HashMap<String,IprFinal> id2ext = iprprocess.process(entries, goterms, transHM);
	    ////
		
		
	    // create fvectors
	    HashMap<String, Instance> id2fvector = createFvectors(id2entry, iprs);
	    
	    HashMap<String, Instance> id2svector = createFvectors(id2entry, superiprs);
	    
	    ArrayList<String> ids = new ArrayList<String>();
	    ids.addAll(id2fvector.keySet());
	    for (String id : id2fvector.keySet()) {
	    	if (!ids.contains(id)) ids.add(id);
	    }
	    
	    if (!ids.isEmpty()) {
	    	for (String id : ids) {

	    		System.out.println("\n###############################");
	    		System.out.println("\nPrediction Results:\nID\tTF\tNon-TF");
		    	Instance fvector = id2fvector.get(id);
		    	
		    	// classify instance
			    final double[] fDis = cls.distributionForInstance(fvector);
			    System.out.println(id+"\t"+df.format(fDis[1])+"\t"+df.format(fDis[0]));
			    
			    
			    // binding and domain
		    	IprFinal curr = id2ext.get(id);
		    	
		    	if (curr != null) {
		    		ArrayList<String> binds = curr.bindings;
		    		
		    		if (!binds.isEmpty()) {
						System.out.println("\nBinding side(s):\nID\tstart\tend");
						for (String bind : binds) System.out.println(bind);
		    		}
			    	
					if (!curr.transfac.isEmpty()) System.out.println("\nID\tTransfac\n"+id+"\t"+convert2full(curr.transfac));
		    	}
		    	else {
		    		System.out.println("No binding information and transfac class found.");
		    	}
		    	
		    	if (fDis[1]>fDis[0]) {
		    		// superclass stuff
			    	if (id2svector.containsKey(id)) {
			    		
			    		Instance svector = id2svector.get(id);
				    	
				    	// classify instance
				    	final double[] sDis = supercls.distributionForInstance(svector);
				    	System.out.println("\nSuperClass Prediction:\nID\t4\t3\t2\t1\t0");
				    	System.out.println(id+"\t"+df.format(sDis[4])+"\t"+df.format(sDis[3])+"\t"+df.format(sDis[2])+"\t"+df.format(sDis[1])+"\t"+df.format(sDis[0]));
			    	}
		    	}
		    	
		    }
	    	
	    }
	    else {
	    	System.out.println("No prediction possible.");
	    }
	    
	}

	
	private static HashMap<String, Instance> createFvectors(HashMap<String, IprEntry> id2entry, ArrayList<String> iprs) {
		
		HashMap<String, Instance> id2fvector = new HashMap<String, Instance>();
		
		Set<Entry<String, IprEntry>> entries = id2entry.entrySet();
		
	    for (Entry<String, IprEntry> current : entries) {
	    	
	    	IprEntry entry = current.getValue();
	    	
	    	String fvector = createIPRvector(entry.iprs, iprs, 10);
	    	
	    	//System.out.println(fvector);
	    	
	    	if (!fvector.isEmpty()) {
	    		id2fvector.put(entry.id, getInst("0 "+fvector));
	    	}
	    	
	    }
		return id2fvector;
	}

	
	private static String createIPRvector(ArrayList<String> entry, ArrayList<String> iprs, int start) {
		String fvector = "";
		
		for (int i = 0; i < iprs.size(); i++) {
			String curr_ipr = iprs.get(i);
			if (entry.contains(curr_ipr)) {
				
				int column = i+1+start;
				fvector = fvector.concat(column +":"+"1 ");
			}
		}
		//System.out.println(fvector);
		return fvector.trim();
	}
	
	
	private static Instance getInst(String fvector) {

		Instance inst = null;
		
		LibSVMLoader lsl = new LibSVMLoader();
		
		Instances tmp = null;
		InputStream is;
		try {
			is = new ByteArrayInputStream(fvector.getBytes("UTF-8"));
			lsl.setSource(is);
			tmp = lsl.getDataSet();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		inst = tmp.firstInstance();
		
		return inst;
	}

	
	public static HashMap<String, IprEntry> extract(ArrayList<String[]> input) {
		
		System.out.println("Processing data ...");
		
		HashMap<String, IprEntry> id2entry = new HashMap<String, IprEntry>();
		
		for (int i = 0; i < input.size(); i++) {
			
			String[] entry = input.get(i);
			
			String id = entry[0].trim();
			
			String ipr = entry[11].trim();
			
			IprEntry curr_entry;
			if (id2entry.containsKey(id)) {
				curr_entry = id2entry.get(id);
				ArrayList<String> curr_iprs = curr_entry.iprs;
				if (!curr_iprs.contains(ipr)) curr_iprs.add(ipr);
				curr_entry.iprs = curr_iprs;
				id2entry.put(id, curr_entry);
			}
			else {
				curr_entry = new IprEntry(id, ipr);
				id2entry.put(id, curr_entry);
			}
				
		}
		
		return id2entry;
	}

	
	private static ArrayList<String[]> runIPR(String seqfile) {
		
		System.out.println("Running IPRscan ...");
		
		System.out.println(iprpath+" -cli -i "+seqfile+" -format raw -goterms -iprlookup -altjobs");
		
		Runtime rt = Runtime.getRuntime();

	    Process proc = null;
		try {
			proc = rt.exec(iprpath+" -cli -i "+seqfile+" -format raw -goterms -iprlookup -altjobs");
			proc.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// get the output stream
		ArrayList<String[]> IPRentries = readIPRout(proc.getInputStream());
		
		// TODO: GOTerms+Bindingdomains
		
		return IPRentries;
	}
	
	
	// read and convert ipr results
	public static ArrayList<String[]> readIPRout(InputStream is) {
		
		ArrayList<String[]> IPRentries = new ArrayList<String[]>();
		
		String line = null;
		
		try {
			 
			 BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			 line = br.readLine();
			 
			 while (line != null) {
				 
				 String[] entry = line.split("\t");
				 
				 IPRentries.add(entry);
				 
				 line = br.readLine();
			}
			 			 
			 br.close();
		
		}
		
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while parsing...");
			System.exit(1);
		}
		
		return IPRentries;
	}
	
	
	// convert transfac-classification to SABINE-InputFileFormat
	private static String convert2full(String input) {
		String output = input+".";
		
		while (output.length() <= 9) {
			output = output.concat("0.");
		}
		
		return output;
	}
	

	
}
