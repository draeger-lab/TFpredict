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

import ipr.*;

import org.apache.commons.cli.CommandLine;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.LibSVMLoader;

public class GalaxyPredict {
	
	static boolean silent = false;
	static boolean batchMode = false;
	public static boolean useWeb = false;
	public static String email;


	// static arguments required by TFpredict
	static String iprpath = "/opt/iprscan/bin/iprscan";
	static String tfClassifier_file = "data/ipr.model";
	static String superClassifier_file = "data/super.model";
	static String relDomainsTF_file = "data/ipr.iprs";
	static String relDomainsSuper_file = "data/super.iprs";
	static String relGOterms_file = "data/DNA.go";
	static String tfName2class_file = "data/transHMan";
	
	// arguments passed from Galaxy to TFpredict
	static String basedir = "";
	static String input_file = "";
	static String html_outfile;
	static String sabine_outfile;
	static String species;
	static String sequence;
	static String tfName = "Sequence_1";
	static String uniprot_id;
	static String fasta_file;
	
	private Classifier tfClassifier;
	private Classifier superClassifier;
	private ArrayList<String> relDomains_TFclass;
	private ArrayList<String> relDomains_Superclass;
	private ArrayList<String> relGOterms;
	private HashMap<String,String> tfName2class;
	private FileParser fp = new FileParser(true);
	
	private HashMap<String, IprEntry> seq2domain;
	private HashMap<String, IprRaw> IPRdomains;
	private HashMap<String, IprProcessed> seq2bindingDomain;
	
	private HashMap<String, String> sequences = new HashMap<String, String>();
	private HashMap<String, Double[]> probDist_TFclass  = new HashMap<String, Double[]>();
	private HashMap<String, Double[]> probDist_Superclass = new HashMap<String, Double[]>();
	private HashMap<String, Integer> predictedSuperclass  = new HashMap<String, Integer>();
	private HashMap<String, String> annotatedClass  = new HashMap<String, String>();
	private HashMap<String, String[]> bindingDomains  = new HashMap<String, String[]>();
	
	private HashMap<String, Boolean> predictionPossible = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> seqIsTF = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> annotatedClassAvailable = new HashMap<String, Boolean>();
	private HashMap<String, Boolean> domainsPredicted = new HashMap<String, Boolean>();
	
	private static final int Non_TF = 0;
	private static final int TF = 1;
	private static final int Basic_domain = 1;
	private static final int Zinc_finger = 2;
	private static final int Helix_turn_helix = 3;
	private static final int Beta_scaffold = 4;
	private static final int Other = 0;
	private static final String[] superclassNames = new String[] {"Other", "Basic domain", "Zinc finger", "Helix-turn-helix", "Beta scaffold"};
	
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
	
	private void parseArguments(CommandLine cmd) {
		
		if(cmd.hasOption("sequence")) {
			sequence = cmd.getOptionValue("sequence");
		}
		
		if(cmd.hasOption("species")) {
			species = cmd.getOptionValue("species");
		}
		
		if(cmd.hasOption("uniprot_id")) {
			uniprot_id = cmd.getOptionValue("uniprot_id");
		}
		
		if(cmd.hasOption("fasta")) {
			fasta_file = cmd.getOptionValue("fasta");
			batchMode = true;
		}
		
		if(cmd.hasOption("html_outfile")) {
			html_outfile = cmd.getOptionValue("html_outfile");
		}
		
		if(cmd.hasOption("sabine_outfile")) {
			sabine_outfile = cmd.getOptionValue("sabine_outfile");
		}
		
        if(cmd.hasOption("basedir")) {
            basedir = cmd.getOptionValue("basedir");
            input_file = basedir + "/query.fasta";
        }
        
		if(cmd.hasOption("tfClassifierFile")) {
			tfClassifier_file = cmd.getOptionValue("tfClassifierFile");
		}

		if(cmd.hasOption("superClassifierFile")) {
			superClassifier_file = cmd.getOptionValue("superClassifierFile");
		}
		
		if(cmd.hasOption("tfClassFeatureFile")) {
			relDomainsTF_file = cmd.getOptionValue("tfClassFeatureFile");
		}
	
		if(cmd.hasOption("superClassFeatureFile")) {
			relDomainsSuper_file = cmd.getOptionValue("superClassFeatureFile");
		}
		
		if(cmd.hasOption("relGOtermsFile")) {
			relGOterms_file = cmd.getOptionValue("relGOtermsFile");
		}

		if(cmd.hasOption("tfName2ClassFile")) {
			tfName2class_file = cmd.getOptionValue("tfName2ClassFile");
		}
		
		if(cmd.hasOption("iprscanPath")) {
			iprpath = cmd.getOptionValue("iprscanPath");
		}
		if(cmd.hasOption("useWeb")) {
			useWeb = true;
		}
		if(cmd.hasOption("email")) {
			email = cmd.getOptionValue("email");
		}
	}
	
	@SuppressWarnings("unchecked")
	private void prepareInput() {
		
		relDomains_TFclass = (ArrayList<String>) ObjectRW.read(relDomainsTF_file, true);
		relDomains_Superclass = (ArrayList<String>) ObjectRW.read(relDomainsSuper_file, true);
		tfName2class = (HashMap<String, String>) ObjectRW.read(tfName2class_file, true);
		
		relGOterms = fp.parseGO(relGOterms_file);
		
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
		
		if (batchMode) {
			// BatchMode --> parse sequences from given FASTA file (and shorten long headers)
			sequences = BasicTools.readFASTA(fasta_file);
			BasicTools.writeFASTA(sequences, input_file);
			
		} else {
			// SingleQueryMode --> write protein sequence to file
			BasicTools.writeArrayToFile(BasicTools.wrapString(sequence), input_file);
		}
	}
	
	private void prepareClassifiers() {

		// load TF/Non-TF and superclass classifier
		try {
			tfClassifier = (Classifier) weka.core.SerializationHelper.read(tfClassifier_file);
			superClassifier = (Classifier) weka.core.SerializationHelper.read(superClassifier_file);
		
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

		// HACK: line can be excluded for testing purposes
		ArrayList<String[]> IPRoutput = IPRrun.run(input_file, iprpath);
		
		// HACK: line can be included for testing purposes
		//ArrayList<String[]> IPRoutput = fp.parseIPRout("result");
	
		// generates mapping from sequence IDs to InterPro domain IDs
		seq2domain = IPRextract.getSeq2DomainMap(IPRoutput);
		if (!silent) {
			for	(String seq: seq2domain.keySet()) {
				System.out.println(seq2domain.get(seq).domain_ids.size() + " InterPro domain(s) found in given protein sequence.");
			}
		}
    
		// generates map of from domain ID to object containing the InterPro ID, description, position, and GO classes
		IPRdomains = IPRextract.parseIPRoutput(IPRoutput);
	
		// process result
		seq2bindingDomain = IPRprocess.filterIPRdomains(seq2domain, IPRdomains, relGOterms, tfName2class);
		if (!silent) {
			for	(String seq: seq2bindingDomain.keySet()) {
				System.out.println(seq2bindingDomain.get(seq).binding_domains.size() + " DNA-binding domain(s) found in given protein sequence.");
			}
		}
	}
	
	private void performClassification() {
    
		// create feature vectors
		HashMap<String, Instance> seq2feat_TFclass = createFeatureVectors(seq2domain, relDomains_TFclass);
		HashMap<String, Instance> seq2feat_Superclass = createFeatureVectors(seq2domain, relDomains_Superclass);
		
		for (String seq: seq2feat_TFclass.keySet()) {
			if (seq2feat_TFclass.get(seq) == null) {
				predictionPossible.put(seq, false);
			} else {
				predictionPossible.put(seq, true);
			}
		}
		
		// perform all classification steps if feature vector could be created
		try {
			for (String seq: seq2feat_TFclass.keySet()) {
				if (predictionPossible.get(seq)) {
					
					seqIsTF.put(seq, false);
					annotatedClassAvailable.put(seq, false);
					domainsPredicted.put(seq, false);
					
					// perform TF/Non-TF classification
					Instance featVectorTF = seq2feat_TFclass.get(seq);
					double[] currProbDistTF = tfClassifier.distributionForInstance(featVectorTF);
					probDist_TFclass.put(seq, BasicTools.double2Double(currProbDistTF));
					
					if (currProbDistTF[TF] > currProbDistTF[Non_TF] && seq2feat_Superclass.containsKey(seq)) {
						seqIsTF.put(seq, true);
					}
		    		
					// if sequence was classified as TF --> predict superclass
					if (seqIsTF.get(seq)) {
						Instance featVectorSuper = seq2feat_Superclass.get(seq);
						double[] currProbDistSuper = superClassifier.distributionForInstance(featVectorSuper);
						probDist_Superclass.put(seq, BasicTools.double2Double(currProbDistSuper));
						
						int maxIndex = BasicTools.getMaxIndex(currProbDistSuper);
						predictedSuperclass.put(seq, maxIndex);
						
						// predict DNA-binding domain
				    	IprProcessed ipr_res = seq2bindingDomain.get(seq);
				    	
				    	if (ipr_res != null) {
				    		if (!ipr_res.anno_transfac_class.isEmpty()) {
				    			annotatedClassAvailable.put(seq, true);
				    			annotatedClass.put(seq, ipr_res.anno_transfac_class);
				    		} 
				    		if (!ipr_res.binding_domains.isEmpty()) {
				    			domainsPredicted.put(seq, true);
				    			bindingDomains.put(seq, ipr_res.binding_domains.toArray(new String[]{}));
				    		}
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
			bw.write("  table { width: 300px; background-color: #E6E8FA; border: 1px solid black; padding: 3px; vertical-align: middle;}\n");
			bw.write("  tr.secRow { background-color:#FFFFFF; margin-bottom: 50px; vertical-align: middle;}\n");
			bw.write("  th { font-weight: bold; padding-bottom: 4px; padding-top: 4px; text-align: center;}\n");
			bw.write("  td { padding-bottom: 4px; padding-top: 4px; text-align: center; background-color:#FFFFFF;}\n");
			bw.write("</style>\n");
			bw.write("</head>\n");
			bw.write("<body style=\"padding-left: 30px\">\n");
			
			String[] sequence_ids = seq2domain.keySet().toArray(new String[]{});
			for (int i=0; i<sequence_ids.length; i++) {
				String seq = sequence_ids[i];
				
				if (i > 0) {
					bw.write("<br><hr>\n\n");
				}
				
				// Null pointer exception ?
				if (predictionPossible.get(seq)) {
					bw.write("<h1>TF/Non-TF prediction:</h1>\n");
					bw.write("<table>\n");
					bw.write("  <tr><th></th><th>Probability<th></tr>\n");
					bw.write("  <tr><th> TF </th><td> " + df.format(probDist_TFclass.get(seq)[TF]) + " </td></tr>\n");
					bw.write("  <tr><th> Non-TF </th><td> " + df.format(probDist_TFclass.get(seq)[Non_TF]) + " </td></tr>\n");
					bw.write("</table>\n\n");
					bw.write("<br><br>\n\n");
					    
					if (seqIsTF.get(seq)) {
						bw.write("<h1>Superclass prediction:</h1>\n");
						bw.write("<table>\n");
						bw.write("  <tr><th></th><th> Probability </th></tr>\n");
						bw.write("  <tr><th> Basic domain </th><td> " + df.format(probDist_Superclass.get(seq)[Basic_domain]) + " </td></tr>\n");
						bw.write("  <tr><th> Zinc finger </th><td> " + df.format(probDist_Superclass.get(seq)[Zinc_finger]) + " </td></tr>\n");
						bw.write("  <tr><th> Helix-turn-helix </th><td> " + df.format(probDist_Superclass.get(seq)[Helix_turn_helix]) + " </td></tr>\n");
						bw.write("  <tr><th> Beta scaffold </th><td> " + df.format(probDist_Superclass.get(seq)[Beta_scaffold]) + " </td></tr>\n");
						bw.write("  <tr><th> Other </th><td> " + df.format(probDist_Superclass.get(seq)[Other]) + " </td></tr>\n");
						bw.write("</table>\n\n");
						bw.write("<br><br>\n\n");    	
				    }
						
		    		if (annotatedClassAvailable.get(seq)) {	
						bw.write("<h1>Annotated structural class:</h1>\n");
						bw.write("<h3>" + getAnnotatedSuperclass(annotatedClass.get(seq)) + " (" + annotatedClass.get(seq) + ") </h3>\n");
						bw.write("<br>\n\n");
					} else {
			    		bw.write("<h1>No structural class annotation available from TransFac.</h1>\n");
			    	}
				    	
				    if (domainsPredicted.get(seq)) {
						bw.write("<h1>DNA-binding domain(s):</h1>\n");
						bw.write("<table>\n");
						bw.write("  <tr><th> Start </th><th> End </th></tr>\n");	
				    	
						for (String domain : bindingDomains.get(seq)) {
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
			
			String[] sequence_ids = seq2domain.keySet().toArray(new String[]{});
			for (int i=0; i<sequence_ids.length; i++) {
				String seq = sequence_ids[i];
				
				if (i > 0) {
					bw.write("//\nXX\n");
				}
				
				// TODO: use first token from FASTA header as "tfName" if batch-mode was called
				if (batchMode) {
					bw.write("NA  " + seq + "\n");
				} else {
					bw.write("NA  " + tfName + "\n");
				}
				bw.write("XX  \n");
				bw.write("SP  " + species + "\n");
				bw.write("XX  \n");
				if (uniprot_id != null) {
					bw.write("RF  " + uniprot_id + "\n");
					bw.write("XX  \n");
				}
				if (annotatedClassAvailable.get(seq)) {
					bw.write("CL  " + expandTransfacClass(annotatedClass.get(seq)) + "\n");
				} else {
					bw.write("CL  " + predictedSuperclass.get(seq) + ".0.0.0.0" + "\n");
				}
				bw.write("XX  \n");
	
				// write sequence
				// TODO: Fix Bug: sequence is not initialized in Batch mode
				String[] wrapped_seq;
				if (batchMode) {
					wrapped_seq = BasicTools.wrapString(sequences.get(seq));
				} else {
					wrapped_seq = BasicTools.wrapString(sequence);
				}
				for (String line: wrapped_seq) {
					bw.write("S1  " + line + "\n"); 
				}
				bw.write("XX  \n");
						
				// write domains
				if (domainsPredicted.get(seq)) {
					for (String domain : bindingDomains.get(seq)) {
						bw.write("FT  " + domain + "\n");
					}
					bw.write("XX\n");
				}
			}
			bw.flush();
			bw.close();
			
		} catch(IOException ioe) {
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

    private static String getAnnotatedSuperclass(String transfac_class) {
    	return(superclassNames[Integer.parseInt(transfac_class.substring(0,1))]);
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
