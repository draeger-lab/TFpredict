package modes;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;

import io.FileParser;
import io.ObjectRW;
import io.BasicTools;
import io.UniProtClient;

import ipr.IPRrun;
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

public class GalaxyPredict {
	
	static boolean silent = false;
	
	
	static String iprpath = "";
	static String basedir = "";
	static String html_outfile;
	static String sabine_outfile;
	static String sequence;
	static String species;
	static String uniprot_id;
	static String tfName = "InputTF";
	static String trainfile = "";
	static String superfile = "";
	static String tfName2classFile = "";
	
	private ArrayList<String> relDomains_TFclass;
	private ArrayList<String> relDomains_Superclass;
	private ArrayList<String> relGOterms;
	private FileParser fp = new FileParser(true);
	private HashMap<String,String> tfName2class;
	private Classifier cls;
	private Classifier supercls;
	
	private HashMap<String, IprEntry> seq2domain;
	private HashMap<String, IprRaw> IPRdomains;
	private HashMap<String,IprProcessed> seq2bindingDomain;
	private double[] fDis;
	private double[] sDis;
	private String annotatedClass;
	private ArrayList<String> bindingDomains;
	
	private boolean predictionPossible = false;
	private boolean seqIsTF = false;
	private boolean annotatedClassAvailable = false;
	private boolean domainsPredicted = false;
	
	static DecimalFormat df = new DecimalFormat("0.00");
	static {
		DecimalFormatSymbols symb = new DecimalFormatSymbols();
		symb.setDecimalSeparator('.');
		df.setDecimalFormatSymbols(symb);
	}


	public static void main(CommandLine cmd) throws Exception {
		
		GalaxyPredict TFpredictor = new GalaxyPredict();
		
		TFpredictor.parseArguments(cmd);
		TFpredictor.prepareInput();
		TFpredictor.prepareClassifiers();
		TFpredictor.runInterproScan();
	    TFpredictor.performClassification();
	    TFpredictor.writeHTMLoutput();
		TFpredictor.writeSABINEoutput();
	}
	
	@SuppressWarnings("unchecked")
	private void parseArguments(CommandLine cmd) {
		
		if(cmd.hasOption("i")) {
			trainfile = cmd.getOptionValue("i");
		}

		if(cmd.hasOption("s")) {
			superfile = cmd.getOptionValue("s");
		}
		
		if(cmd.hasOption("g")) {
			relGOterms = fp.parseGO(cmd.getOptionValue("g"));
		}
		
		if(cmd.hasOption("r")) {
			relDomains_TFclass = (ArrayList<String>) ObjectRW.read(cmd.getOptionValue("r"), true);
		}
	
		if(cmd.hasOption("sr")) {
		   	relDomains_Superclass = (ArrayList<String>) ObjectRW.read(cmd.getOptionValue("sr"), true);
		}
		
		// load static map (Factor name --> TransFac class) for TF class annotation
		if(cmd.hasOption("t")) {
			tfName2class = (HashMap<String, String>) ObjectRW.read(cmd.getOptionValue("t"), true);
		}
		
		if(cmd.hasOption("p")) {
			iprpath = cmd.getOptionValue("p");
		}
		
		// input protein can be given either as protein sequence or Uniprot Accession / Entry Name 
		if(cmd.hasOption("sequence")) {
			sequence = cmd.getOptionValue("sequence");
		}
		
		if(cmd.hasOption("species")) {
			species = cmd.getOptionValue("species");
		}
		
		if(cmd.hasOption("uniprot_id")) {
			uniprot_id = cmd.getOptionValue("uniprot_id");
		}
		
		if(cmd.hasOption("html_outfile")) {
			html_outfile = cmd.getOptionValue("html_outfile");
		}
		
		if(cmd.hasOption("sabine_outfile")) {
			sabine_outfile = cmd.getOptionValue("sabine_outfile");
		}
		
        if(cmd.hasOption("basedir")) {
            basedir = cmd.getOptionValue("basedir");
        }
	}
	
	private void prepareInput() {
		
		// if UniProt ID was given --> retrieve sequence and species from UniProt
		if (uniprot_id != null) {
			UniProtClient uniprot_client = new UniProtClient();
			String fasta_seq = uniprot_client.getUniProtSequence(uniprot_id.toUpperCase(), true);
			String[] splitted_header = fasta_seq.substring(0, fasta_seq.indexOf(" ")).trim().split("\\|");
			
			tfName = splitted_header[2];
			uniprot_id = splitted_header[1];
			species = fasta_seq.substring(fasta_seq.indexOf("OS=")+3, fasta_seq.indexOf("GN=")-1);
			sequence = fasta_seq.replaceFirst(">.*\\n", "").replaceAll("\\n", "");
			
			if (sequence == null) {
				System.out.println("Error. Invalid UniProt ID or Entry name given.");
				System.exit(0);
			}
		} 
		
		// write protein sequence to file
		String input_file = basedir + "/query.fasta";
        BasicTools.writeArrayToFile(BasicTools.wrapString(sequence), input_file);
	}
	
	private void prepareClassifiers() {

		// load TF/Non-TF and superclass classifier
		try {
			cls = (Classifier) weka.core.SerializationHelper.read(trainfile);
			supercls = (Classifier) weka.core.SerializationHelper.read(superfile);
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
    	if (!silent) {
    		System.out.println(relDomains_TFclass.size() + " domains used for TF/Non-TF classification.");
    		System.out.println(relDomains_Superclass.size() + " domains used for Superclass classification.\n");
    	}
	}
    
	 // execute iprscan and get results
	private void runInterproScan() {

		//TODO: fix query
		// HACK: lines were excluded for testing purposes
		// ArrayList<String[]> IPRentries = IPRrun.run(input_file, iprpath);
		
		// HACK: line was included for testing purposes
		ArrayList<String[]> IPRoutput = fp.parseIPRout("result");
	
		// generates mapping from sequence IDs to InterPro domain IDs
		seq2domain = IPRextract.getSeq2DomainMap(IPRoutput);
		
		String id = seq2domain.keySet().toArray(new String[]{})[0];
		if (!silent) System.out.println(seq2domain.get(id).domain_ids.size() + " InterPro domain(s) found in given protein sequence.");
    
		// generates map of from domain ID to object containing the InterPro ID, description, position, and GO classes
		IPRdomains = IPRextract.parseIPRoutput(IPRoutput);
	
		// process result
		seq2bindingDomain = IPRprocess.filterIPRdomains(seq2domain, IPRdomains, relGOterms, tfName2class);
		if (!silent) System.out.println(seq2bindingDomain.get(id).binding_domains.size() + " DNA-binding domain(s) found in given protein sequence.");
	}
	
	private void performClassification() {
    
		// create feature vectors
		HashMap<String, Instance> id2fvector = createFeatureVectors(seq2domain, relDomains_TFclass);
		HashMap<String, Instance> id2svector = createFeatureVectors(seq2domain, relDomains_Superclass);
		
		if (!id2fvector.keySet().isEmpty()) {
			predictionPossible = true;
		}
		
		// perform all classification steps if feature vector could be created
		try {
			if (predictionPossible) {
				String id = id2fvector.keySet().toArray(new String[]{})[0];
				
				// perform TF/Non-TF classification
				Instance fvector = id2fvector.get(id);
				fDis = cls.distributionForInstance(fvector);
				
				if (fDis[1]>fDis[0] && id2svector.containsKey(id)) {
					seqIsTF = true;
				}
	    		
				// if sequence was classified as TF --> predict superclass
				if (seqIsTF) {
					Instance svector = id2svector.get(id);
					sDis = supercls.distributionForInstance(svector);
					
					// predict DNA-binding domain
			    	IprProcessed ipr_res = seq2bindingDomain.get(id);
			    	
			    	if (ipr_res != null) {
			    		if (!ipr_res.anno_transfac_class.isEmpty()) {
			    			annotatedClassAvailable = true;
			    			annotatedClass = ipr_res.anno_transfac_class;
			    		}
			    		if (!ipr_res.binding_domains.isEmpty()) {
			    			domainsPredicted = true;
			    			bindingDomains = ipr_res.binding_domains;
			    		}
			    	}
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private void writeHTMLoutput() {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(html_outfile)));
			
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
			bw.write("  th { font-weight: bold; padding-bottom: 8px; padding-top: 8px; text-align: center}\n");
			bw.write("  td { padding-bottom: 8px; padding-top: 8px; text-align: center}\n");
			bw.write("</style>\n");
			bw.write("</head>\n");
			bw.write("<body style=\"padding-left: 30px\">\n");
			
			if (predictionPossible) {
				bw.write("<h1>TF/Non-TF prediction:</h1>\n");
				bw.write("<table>\n");
				bw.write("  <tr><th></th><th> TF </th><th> Non-TF </th></tr>\n");
				bw.write("  <tr><th> Probability </th><td> "+ df.format(fDis[1]) +" </td><td> " + df.format(fDis[0])+" </td></tr>\n");
				bw.write("</table>\n\n");
				bw.write("<br><br>\n\n");
				    
				if (seqIsTF) {
					bw.write("<h1>Superclass prediction:</h1>\n");
					bw.write("<table>\n");
					bw.write("  <tr><th> Superclass </th><td> 4 </td><td> 3 </td><td> 2 </td><td> 1 </td><td> 0 </td></tr>\n");
					bw.write("  <tr><th> Probability </th><td> "+df.format(sDis[4])+" </td><td> "+df.format(sDis[3])+" </td><td> "+df.format(sDis[2])+" </td><td> "+df.format(sDis[1])+" </td><td> "+df.format(sDis[0])+" </td></tr>\n");
					bw.write("</table>\n\n");
					bw.write("<br><br>\n\n");    	
			    }
					
	    		if (annotatedClassAvailable) {	
					bw.write("<h1>Annotated structural class:</h1>\n");
					bw.write("<h3>" + annotatedClass + "</h3>\n");
					bw.write("<br>\n\n");
				} else {
		    		bw.write("<h1>No structural class annotation available from TransFac.</h1>\n");
		    	}
			    	
			    if (domainsPredicted) {
					bw.write("<h1>DNA-binding domain(s):</h1>\n");
					bw.write("<table>\n");
					bw.write("  <tr><th> Start </th><th> End </th></tr>\n");	
			    	
					for (String domain : bindingDomains) {
						String[] splitted_domain = domain.replace("    ", "\t").split("\t");
						bw.write("  <tr><td> "+ splitted_domain[1] +" </td><td> " + splitted_domain[2] +" </td></tr>\n"); 
					}
					bw.write("</table>\n\n");
					bw.write("<br>\n\n");
					
			    } else {
		    		bw.write("<h1>No DNA-binding domain could be predicted.</h1>\n");
		    	}
		    }
		    else {
		    	bw.write("<h1>No prediction possible.</h1>");
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
	
	private void writeSABINEoutput() {
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(sabine_outfile)));
			
			bw.write("NA  " + tfName + "\n");
			bw.write("XX  \n");
			bw.write("SP  " + species + "\n");
			bw.write("XX  \n");
			if (uniprot_id != null) {
				bw.write("RF  " + uniprot_id + "\n");
				bw.write("XX  \n");
			}
			if (annotatedClassAvailable) {
				bw.write("CL  " + expandTransfacClass(annotatedClass) + "\n");
			} else {
				// TODO: determine superclass with highest probability
				bw.write("CL  " + sDis + ".0.0.0.0");
			}
			bw.write("XX  \n");

			// write sequence
			String[] wrapped_seq = BasicTools.wrapString(sequence);
			for (String line: wrapped_seq) {
				bw.write("S1  " + line + "\n"); 
			}
			bw.write("XX  \n");
					
			// write domains
			if (domainsPredicted) {
				for (String domain : bindingDomains) {
					bw.write("FT  " + domain + "\n");
				}
				bw.write("XX\n");
			}
			bw.flush();
			bw.close();
		}
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while writing input file for SABINE.");
		}
	}

   // convert transfac-classification to SABINE-InputFileFormat                                                                                                                                 
    private static String expandTransfacClass(String transfac_class) {
            String expanded_class = transfac_class + ".";

            while (expanded_class.length() <= 9) {
            	expanded_class = expanded_class.concat("0.");
            }

            return expanded_class;
    }

	
	
	/*
	 * functions used to create the feature vectors for TF/Non-TF classification and superclass prediction
	 */
	private static HashMap<String, Instance> createFeatureVectors(HashMap<String, IprEntry> seq2domain, ArrayList<String> relIPRdomains) {
		
		HashMap<String, Instance> seq2fvector = new HashMap<String, Instance>();
		
		for (String seq: seq2domain.keySet()) {
	    	
	    	IprEntry entry = seq2domain.get(seq);
	    	
	    	//TODO: Why is start set to 10 ???
	    	String fvector = createIPRvector(entry.domain_ids, relIPRdomains, 10);
	    	
	    	if (!fvector.isEmpty()) {
	    		seq2fvector.put(entry.sequence_id, getInst("0 " + fvector));
	    	}
	    }
		return seq2fvector;
	}

	
	private static String createIPRvector(ArrayList<String> predIPRdomains, ArrayList<String> relIPRdomains, int start) {
		String fvector = "";
		
		for (int i = 0; i < relIPRdomains.size(); i++) {
			String curr_ipr = relIPRdomains.get(i);
			if (predIPRdomains.contains(curr_ipr)) {
				
				int column = i+1+start;
				fvector = fvector.concat(column + ":" + "1 ");
			}
		}
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
}
