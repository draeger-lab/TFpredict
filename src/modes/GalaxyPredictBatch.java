package modes;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintStream;
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
import ipr.IprProcessed;
import ipr.IprRaw;

import org.apache.commons.cli.CommandLine;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.LibSVMLoader;

public class GalaxyPredictBatch {
	
	static String iprpath;
	
	static DecimalFormat df = new DecimalFormat(".00");
	
	static ArrayList<String> iprs;
	static ArrayList<String> superiprs;
	static ArrayList<String> goterms;
	static FileParser fp = new FileParser();
	
	static String basedir;
	static String outfile;
	static String sequence;
	

	@SuppressWarnings("unchecked")
	public static void main(CommandLine cmd) throws Exception {
		
		// load data
		String trainfile = "";
		if(cmd.hasOption("i")) {
			trainfile = cmd.getOptionValue("i");
		}
		
		String superfile = "";
		if(cmd.hasOption("s")) {
			superfile = cmd.getOptionValue("s");
		}
		
		if(cmd.hasOption("g")) {
			goterms = fp.parseGO(cmd.getOptionValue("g"));
		}
		
		if(cmd.hasOption("r")) {
			iprs = (ArrayList<String>) ObjectRW.read(cmd.getOptionValue("r"));
		}

		if(cmd.hasOption("sr")) {
		   	superiprs = (ArrayList<String>) ObjectRW.read(cmd.getOptionValue("sr"));
		}
				
		String trans = "";
		if(cmd.hasOption("t")) {
			trans = cmd.getOptionValue("t");
		}
		
		iprpath = "";
		if(cmd.hasOption("p")) {
			iprpath = cmd.getOptionValue("p");
		}
		
		if(cmd.hasOption("galaxybatch")) {
			sequence = cmd.getOptionValue("galaxybatch");
		}
		
		if(cmd.hasOption("b")) {
			basedir = cmd.getOptionValue("b");
		}
		
		String query = writeInputFile(basedir+"query");
		
		if(cmd.hasOption("o")) {
			outfile = cmd.getOptionValue("o");
		}

		
		// load static map
		HashMap<String,String> transHM = (HashMap<String, String>) ObjectRW.read(trans);
		
		// deserialize model
	    Classifier cls = (Classifier) weka.core.SerializationHelper.read(trainfile);
	    System.out.print("Classifier-Info:\n"+cls.toString());
	    
	    // superclass classifier
	    Classifier supercls = (Classifier) weka.core.SerializationHelper.read(superfile);
	    System.out.print("Classifier-Info:\n"+supercls.toString());
	    
	    System.out.println("Number of significant IPRs: "+iprs.size());
	    
	    // execute iprscan and get results
	    ArrayList<String[]> IPRentries = runIPR(query);

	    // parse iprresults
	    //ArrayList<String[]> IPRentries = fp.parseIPRout("multiresult");
		
	    // extract iprresults
	    HashMap<String, IprEntry> id2entry = extract(IPRentries);
	    
	    
	    //// iprprocess subroutines (rip-off)
	    // process iprresults
	    IPRextract iprextract = new IPRextract();
		//ArrayList<IprRaw> entries = iprextract.parseIPRoutput(IPRentries);
		
		// process result
		IPRprocess iprprocess = new IPRprocess();
		HashMap<String,IprProcessed> id2ext = null;
		//HashMap<String,IprProcessed> id2ext = iprprocess.filterIPRdomains(entries, goterms, transHM);
	    ////
		
		
	    // create fvectors
	    HashMap<String, Instance> id2fvector = createFvectors(id2entry, iprs);
	    
	    HashMap<String, Instance> id2svector = createFvectors(id2entry, superiprs);
	    
	    ArrayList<String> ids = new ArrayList<String>();
	    ids.addAll(id2fvector.keySet());
	    for (String id : id2fvector.keySet()) {
	    	if (!ids.contains(id)) ids.add(id);
	    }
	    
	    //redirectSystemOut(outfile);
	    
	    try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outfile)));
			// write HTML header
			bw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
			bw.write("     \"http://www.w3.org/TR/html4/loose.dtd\">\n");
			bw.write("<html>\n");
			bw.write("<head>\n");
			bw.write("<title>TF_predict Result</title>\n");
			bw.write("<style type=\"text/css\">\n");
			bw.write("  h1 { font-size: 150%;color: #002780; }\n");
			bw.write("  table { width: 500px; background-color: #E6E8FA; border: 1px solid black; padding: 3px; vertical-align: middle;}\n");
			bw.write("  tr.secRow { background-color:#FFFFFF; margin-bottom: 50px; vertical-align: middle;}\n");
			bw.write("  th { padding-bottom: 8px; padding-top: 8px; text-align: center}\n");
			bw.write("  td { padding-bottom: 8px; padding-top: 8px; text-align: center}\n");
			bw.write("</style>\n");
			bw.write("</head>\n");
			bw.write("<body style=\"padding-left: 30px\">\n");
			
			if (!ids.isEmpty()) {
		    	for (String id : ids) {

		    		System.out.println("\n###############################");
		    		System.out.println("\nPrediction Results:\nID\tTF\tNon-TF");
			    	Instance fvector = id2fvector.get(id);
			    	
			    	bw.write("<h1>Results for: "+id+"</h1>\n");
			    	
			    	// write TF/Non-TF result
					bw.write("<h2>TF/Non-TF prediction results:</h2>\n");
					bw.write("<table>\n");
					bw.write("  <tr><th> Class </th><th> TF </th><th> Non-TF </th></tr>\n");
			    	
			    	// classify instance
				    final double[] fDis = cls.distributionForInstance(fvector);
				    System.out.println(id+"\t"+df.format(fDis[1])+"\t"+df.format(fDis[0]));
				    bw.write("  <tr><th> Probability </th><th> "+ df.format(fDis[1]) +" </th><th> " + df.format(fDis[0])+" </th></tr>\n");
				   
					bw.write("</table>\n\n");
					bw.write("<br>\n\n");
				    
				    // binding and domain
			    	IprProcessed curr = id2ext.get(id);
			    	
			    	if (curr != null) {
			    		ArrayList<String> binds = curr.binding_domains;
			    		
			    		if (!binds.isEmpty()) {
							System.out.println("\nBinding side(s):\nID\tstart\tend");
							
							bw.write("<h2>Binding side(s):</h2>\n");
							bw.write("<table>\n");
							bw.write("  <tr><th> Start </th><th> End </th></tr>\n");
							
							for (String bind : binds) {
								System.out.println(bind);
								
								String[] bind_tmp = bind.replace("    ", "\t").split("\t");
								bw.write("  <tr><th> "+ bind_tmp[1] +" </th><th> " + bind_tmp[2] +" </th></tr>\n");
								   
							}
							bw.write("</table>\n\n");
							bw.write("<br>\n\n");
			    		}
				    	
						if (!curr.anno_transfac_class.isEmpty()) {
							System.out.println("\nID\tTransfac\n"+id+"\t"+convert2full(curr.anno_transfac_class));
							
							bw.write("<h2>Static transfac-mapping: "+convert2full(curr.anno_transfac_class)+ "</h2>\n");
							//bw.write("<h3>"+convert2full(curr.transfac)+"</h3>\n");
							//bw.write("<br>\n\n");
						}
			    	}
			    	else {
			    		System.out.println("No binding information and transfac class found.");
			    		bw.write("<h1>No binding information and transfac class found.</h1>\n");
			    		
			    	}
			    	
			    	if (fDis[1]>fDis[0]) {
			    		// superclass stuff
				    	if (id2svector.containsKey(id)) {
				    		
				    		Instance svector = id2svector.get(id);
					    	
					    	// classify instance
					    	final double[] sDis = supercls.distributionForInstance(svector);
					    	System.out.println("\nSuperClass Prediction:\nID\t4\t3\t2\t1\t0");
					    	System.out.println(id+"\t"+df.format(sDis[4])+"\t"+df.format(sDis[3])+"\t"+df.format(sDis[2])+"\t"+df.format(sDis[1])+"\t"+df.format(sDis[0]));
					    	
					    	bw.write("<h2>Superclass prediction:</h2>\n");
							bw.write("<table>\n");
							bw.write("  <tr><th> Superclass </th><th> 4 </th><th> 3 </th><th> 2 </th><th> 1 </th><th> 0 </th></tr>\n");
							bw.write("  <tr><th> Probability </th><th> "+df.format(sDis[4])+" </th><th> "+df.format(sDis[3])+" </th><th> "+df.format(sDis[2])+" </th><th> "+df.format(sDis[1])+" </th><th> "+df.format(sDis[0])+" </th></tr>\n");
							bw.write("</table>\n\n");
							bw.write("<br>\n\n");
					    	
				    	}
			    	}
			    	bw.write("<br><hr>\n\n");
			    }
		    
		    }
		    else {
		    	System.out.println("No prediction possible.");
		    	bw.write("No prediction possible.");
		    }
			
			// close HTML file
			bw.write("</body>\n");
			bw.write("</html>\n");
			
			bw.flush();
			bw.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	    		
	}

	
	private static HashMap<String, Instance> createFvectors(HashMap<String, IprEntry> id2entry, ArrayList<String> iprs) {
		
		HashMap<String, Instance> id2fvector = new HashMap<String, Instance>();
		
		Set<Entry<String, IprEntry>> entries = id2entry.entrySet();
		
	    for (Entry<String, IprEntry> current : entries) {
	    	
	    	IprEntry entry = current.getValue();
	    	
	    	String fvector = createIPRvector(entry.domain_ids, iprs, 10);
	    	
	    	//System.out.println(fvector);
	    	
	    	if (!fvector.isEmpty()) {
	    		id2fvector.put(entry.sequence_id, getInst("0 "+fvector));
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
				ArrayList<String> curr_iprs = curr_entry.domain_ids;
				if (!curr_iprs.contains(ipr)) curr_iprs.add(ipr);
				curr_entry.domain_ids = curr_iprs;
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
	
	
	public static void redirectSystemOut(String outfile) {
        try {
            System.setOut(new PrintStream(new FileOutputStream(outfile)));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return;
        }
    }
	
	////
	// galaxy stuff (adapted from SABINE)
	
	public static String writeInputFile(String infile) {

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(new File(infile)));

			// write sequence
			int SEQLINELENGTH = 60;

			for (int i = 0; i < (sequence.length() / SEQLINELENGTH); i++) {
				bw.write(sequence.toUpperCase(), i * SEQLINELENGTH, SEQLINELENGTH);
				bw.write("\n");
			}

			if (sequence.length() - (sequence.length() / SEQLINELENGTH) * SEQLINELENGTH > 0) {
				bw.write(sequence.toUpperCase(), (sequence.length() / SEQLINELENGTH)
						* SEQLINELENGTH, sequence.length()
						- (sequence.length() / SEQLINELENGTH) * SEQLINELENGTH);
				bw.write("\n");
			}

			bw.flush();
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return infile;
	}

}
