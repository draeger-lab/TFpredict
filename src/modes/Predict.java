/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2012 ZBIT, University of Tübingen, Florian Topf and Johannes Eichner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package modes;

import io.AnimatedChar;
import io.BasicTools;
import io.ObjectRW;
import io.UniProtClient;
import ipr.IPRextract;
import ipr.IPRprocess;
import ipr.IPRrun;
import ipr.IprEntry;
import ipr.IprProcessed;
import ipr.IprRaw;

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
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;

import resources.Resource;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.LibSVMLoader;

public class Predict {
	
	private static final String interproPrefix = "http://www.ebi.ac.uk/interpro/ISearch?query=";
	private static final String transfacURL = "http://www.gene-regulation.com/pub/databases/transfac/clSM.html";
	private static final int maxNumSequencesBatchMode = 10;
	
	private static final byte DuplicatedHeaderError = 1;
	private static final byte InvalidUniProtError = 2;
	private static final byte TooManySequencesError = 3;
	
	static Logger logger = Logger.getLogger(Predict.class.getName());
	static {
		logger.setLevel(Level.SEVERE);
	}

	
	// use webservice version by default (local version is used if argument "iprscanPath" is provided)
	static boolean useWeb = true;
	static boolean standAloneMode = false;
	static boolean batchMode = false;
	static boolean silent = false;

	// static arguments required by TFpredict
	static String iprpath = "/opt/iprscan/bin/iprscan";
	static String tfClassifier_file = "ipr.model";
	static String superClassifier_file = "super.model";
	static String relDomainsTF_file = "ipr.iprs";
	static String relDomainsSuper_file = "super.iprs";
	static String relGOterms_file = "DNA.go";
	static String tfName2class_file = "transHMan";
	
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

	private HashMap<String, IprEntry> seq2domain;
	private HashMap<String, IprRaw> IPRdomains;
	private HashMap<String, IprProcessed> seq2bindingDomain;
	
	// gfx related mapping of seqid to jobid
	private HashMap<String, String> seq2job;
	
	private String[] sequence_ids;
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
		
		Predict TFpredictor = new Predict();
		
		TFpredictor.parseArguments(cmd);
		TFpredictor.prepareInput();
		TFpredictor.prepareClassifiers();
		TFpredictor.runInterproScan();
	    TFpredictor.performClassification();

	    if (standAloneMode) {
	    	TFpredictor.writeConsoleOutput();
	    } else {

	    	TFpredictor.writeHTMLoutput();
	    }
	    if (sabine_outfile != null) {
	    	TFpredictor.writeSABINEoutput();
	    }
	}
	
	private void parseArguments(CommandLine cmd) {
		
		if(cmd.hasOption("sequence")) {
			sequence = cmd.getOptionValue("sequence").replaceAll("\\s", "");
		}
		
		if(cmd.hasOption("species")) {
			species = cmd.getOptionValue("species");
		}
		
		if(cmd.hasOption("uniprotID")) {
			uniprot_id = cmd.getOptionValue("uniprotID");
		}
		
		if(cmd.hasOption("fasta")) {
			fasta_file = cmd.getOptionValue("fasta");
			batchMode = true;
		}
		
		if(cmd.hasOption("htmlOutfile")) {
			html_outfile = cmd.getOptionValue("htmlOutfile");
		}
		
		if(cmd.hasOption("sabineOutfile")) {
			sabine_outfile = cmd.getOptionValue("sabineOutfile");
		}
		
        if(cmd.hasOption("basedir")) {
            basedir = cmd.getOptionValue("basedir");
            if (!basedir.endsWith("/")) basedir += "/" ;
            input_file = basedir + "query.fasta";
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
			useWeb = false;
		}
		
		if(cmd.hasOption("standAloneMode")) {
			standAloneMode = true;
			silent = true;
		} else {
	    	FileHandler logFileHandler;
			try {
				logFileHandler = new FileHandler(sabine_outfile);
				logFileHandler.setFormatter(new Formatter() {
					@Override
					public String format(LogRecord record) {
						return record.getMessage();
					}
				});
				logger.addHandler(logFileHandler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void prepareInput() {
		
		relDomains_TFclass = (ArrayList<String>) ObjectRW.readFromResource(relDomainsTF_file);
		relDomains_Superclass = (ArrayList<String>) ObjectRW.readFromResource(relDomainsSuper_file);
		tfName2class = (HashMap<String, String>) ObjectRW.readFromResource(tfName2class_file);
		
		relGOterms = BasicTools.readResource2List(relGOterms_file);
		
		// if UniProt ID was given --> retrieve sequence and species from UniProt
		if (uniprot_id != null) {
			UniProtClient uniprot_client = new UniProtClient();
			String fasta_seq = uniprot_client.getUniProtSequence(uniprot_id.toUpperCase(), true);
			
			// Stop, if given UniProt ID is invalid
			if (fasta_seq == null) {
				logger.log(Level.SEVERE, "Error. Invalid UniProt ID or Entry name: " + uniprot_id + ".");
				writeHTMLerrorOutput(InvalidUniProtError);
				System.exit(0);
			}
			
			String[] splitted_header = fasta_seq.substring(0, fasta_seq.indexOf(" ")).trim().split("\\|");
			
			tfName = splitted_header[2];
			uniprot_id = splitted_header[1];
			species = fasta_seq.substring(fasta_seq.indexOf("OS=")+3, fasta_seq.indexOf("GN=")-1);
			sequence = fasta_seq.replaceFirst(">.*\\n", "").replaceAll("\\n", "");
		} 
		
		if (batchMode) {
			// BatchMode --> parse sequences from given FASTA file (and shorten long headers)
			sequences = BasicTools.readFASTA(fasta_file);
			
			// Stop, if FASTA file contains duplicated headers
			if (sequences.containsKey(BasicTools.duplicatedHeaderKey)) {
				logger.log(Level.SEVERE, "Error. FASTA file contains duplicated headers.");
				writeHTMLerrorOutput(DuplicatedHeaderError);
				System.exit(0);
			}
			// Stop, if maximum number of sequences allowed for Batch mode was exceeded
			if (sequences.size() > maxNumSequencesBatchMode) {
				logger.log(Level.SEVERE, "Error. Maximum number of sequences allowed in Batch Mode: " + maxNumSequencesBatchMode + 
						   		   ". FASTA file contains " + sequences.size() + " sequences.");
				writeHTMLerrorOutput(TooManySequencesError);
				System.exit(0);
			}
			
			sequence_ids = sequences.keySet().toArray(new String[] {});
			BasicTools.writeFASTA(sequences, input_file);
			
		} else {
			// SingleQueryMode --> add default header and write protein sequence to file
			String[] inputSeq = BasicTools.wrapString(sequence);
			String[] fastaSeq = new String[inputSeq.length+1];
			fastaSeq[0] = ">" + tfName;
			sequence_ids = new String[] {tfName};
			for (int i=0; i<inputSeq.length; i++) {
				fastaSeq[i+1] = inputSeq[i];
			}
			BasicTools.writeArrayToFile(fastaSeq, input_file);
		}
	}
	
	private void prepareClassifiers() {

		// load TF/Non-TF and superclass classifier
		try {
			tfClassifier = (Classifier) weka.core.SerializationHelper.read(Resource.class.getResourceAsStream(tfClassifier_file));
			superClassifier = (Classifier) weka.core.SerializationHelper.read(Resource.class.getResourceAsStream(superClassifier_file));
		
		} catch (Exception e) {
			e.printStackTrace();
		}
		
    	if (!silent) {
    		//System.out.println("  " + relDomains_TFclass.size() + " domains used for TF/Non-TF classification.");
    		//System.out.println("  " + relDomains_Superclass.size() + " domains used for Superclass classification.\n");
    	}
	}
    
	 // execute iprscan and get results
	private void runInterproScan() {

		// HACK: line can be excluded for testing purposes
		IPRrun InterProScanRunner = new IPRrun(silent);
		AnimatedChar an = null;
		if (standAloneMode) {
			System.out.print("\n  Fetching domains from InterProScan. This may take several minutes... ");
			
			an = new AnimatedChar();
			an.setOutputStream(System.out);
			an.showAnimatedChar();
		}
		ArrayList<String[]> IPRoutput = InterProScanRunner.run(input_file, iprpath, basedir, useWeb, standAloneMode);
		seq2job = InterProScanRunner.getSeq2job();
		if (standAloneMode) {
			an.hideAnimatedChar();
			System.out.println();
		}
		
		// HACK: line can be included for testing purposes
		//basedir = "/tmp/TFpredict_4215575705942655684_basedir";
		//ArrayList<String[]> IPRoutput = BasicTools.readFile2ListSplitLines(basedir + "/iprscan-S20120425-145757-0037-1931185-pg.out.txt");
		
		// generates mapping from sequence IDs to InterPro domain IDs
		seq2domain = IPRextract.getSeq2DomainMap(IPRoutput);
		
		// generates map of from domain ID to object containing the InterPro ID, description, position, and GO classes
		IPRdomains = IPRextract.parseIPRoutput(IPRoutput);
	
		// process result
		seq2bindingDomain = IPRprocess.filterIPRdomains(seq2domain, IPRdomains, relGOterms, tfName2class);
		if (standAloneMode || !silent) {
			for	(String seq: sequence_ids) {
				System.out.println("\nProcessed " + seq + ":");
				int numDomains = 0;
				if (seq2domain.get(seq) != null) {
					numDomains = seq2domain.get(seq).domain_ids.size();
				}
				int numDomainsTFclass = 0;
				if (seq2domain.get(seq) != null) {
					numDomainsTFclass = BasicTools.intersect(seq2domain.get(seq).domain_ids, relDomains_TFclass).size();
				}
				int numDomainsSuperclass = 0;
				if (seq2domain.get(seq) != null) {
					numDomainsSuperclass = BasicTools.intersect(seq2domain.get(seq).domain_ids, relDomains_Superclass).size();
				}
				int numBindingDomains = 0;
				if (seq2bindingDomain.get(seq) != null) {
					numBindingDomains = seq2bindingDomain.get(seq).binding_domains.size();
				}
				System.out.println("  " + numDomains + " InterPro domain(s) found.");
				System.out.println("  " + numDomainsTFclass + " / " + numDomains + " InterPro domain(s) are relevant for TF/Non-TF classification.");
				System.out.println("  " + numDomainsSuperclass + " / " + numDomains + " InterPro domain(s) are relevant for Superclass prediction.");
				System.out.println("  " + numBindingDomains + " / " + numDomains + " InterPro domain(s) were identified as DNA-binding domain(s).");
			}
		}
	}
	
	private void performClassification() {
    
		// create feature vectors
		HashMap<String, Instance> seq2feat_TFclass = createFeatureVectors(seq2domain, relDomains_TFclass);
		HashMap<String, Instance> seq2feat_Superclass = createFeatureVectors(seq2domain, relDomains_Superclass);
		
		// flag all sequences for which no prediction is possible
		// (i.e., none of the IPRdomains which are relevant for TF/Non-TF classification was found)
		for (String seq: sequence_ids) {
			predictionPossible.put(seq, false);
		}
		for (String seq: seq2domain.keySet()) {
			if (seq2feat_TFclass.get(seq) != null) {
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
	
	// writes HTML header
	private static void writeHTMLheader(BufferedWriter bw) {
		
		try {
			bw.write("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n");
			bw.write("     \"http://www.w3.org/TR/html4/loose.dtd\">\n");
			bw.write("<html>\n");
			bw.write("<head>\n");
			bw.write("<title>TF_predict Result</title>\n");
			bw.write("<style type=\"text/css\">\n");
			bw.write("  h1 { font-size: 150%;color: #002780; }\n");
			bw.write("  h2 { font-size: 135%;color: #002780; }\n");
			bw.write("  h4 { font-size: 135%;color: #990000; }\n");
			bw.write("  table { width: 300px; background-color: #E6E8FA; border: 1px solid black; padding: 3px; vertical-align: middle;}\n");
			bw.write("  th { font-weight: bold; padding-bottom: 4px; padding-top: 4px; text-align: center;}\n");
			bw.write("  td { padding-bottom: 4px; padding-top: 4px; text-align: center; background-color:#F8F8FF;}\n");
			bw.write("  td.win { padding-bottom: 4px; padding-top: 4px; text-align: center; background-color:#98FB98;}\n");
			bw.write("  td.draw { padding-bottom: 4px; padding-top: 4px; text-align: center; background-color:#F0E68C}\n");
			bw.write("  td.lose { padding-bottom: 4px; padding-top: 4px; text-align: center; background-color:#F8F8FF;}\n");
			bw.write("</style>\n");
			bw.write("</head>\n");
			bw.write("<body style=\"padding-left: 30px\">\n");
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	private void writeHTMLerrorOutput(byte errorType) {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(html_outfile)));
			
			writeHTMLheader(bw);
			bw.write("<h4> Error </h4>\n");
			
			if (errorType == InvalidUniProtError) {
				bw.write("<h3> Invalid UniProt ID or Entry name: " + uniprot_id + ".</h3>\n");
						
			} else if (errorType == TooManySequencesError) {
				bw.write("<h3> Maximum number of sequences allowed in Batch Mode: " + maxNumSequencesBatchMode + "<br>\n" +
			   		   "Number of sequences in given FASTA file: " + sequences.size() + "</h3>\n");
				
			} else if (errorType == DuplicatedHeaderError) {
				bw.write("<h3>FASTA file contains duplicated headers.</h3>\n");
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
	
	private void writeHTMLoutput() {
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(html_outfile)));
			
			writeHTMLheader(bw);
		
			for (int i=0; i<sequence_ids.length; i++) {
				String seq = sequence_ids[i];
				
				if (i > 0) {
					bw.write("<br><hr>\n\n");
				}
				
				if (batchMode) {
					bw.write("<h1><span style=\"color:#000000\">Results report: </span>" + seq + "</h1>\n");
				}
				bw.write("<h2>TF/Non-TF prediction:</h2>\n");
				if (predictionPossible.get(seq)) {
					
					String[] outcomesTF = getClassificationOutcomes(BasicTools.Double2double(probDist_TFclass.get(seq)));
					bw.write("<table>\n");
					bw.write("  <tr><th></th><th>Probability<th></tr>\n");
					bw.write("  <tr><th> TF </th><td class=\"" + outcomesTF[TF] + "\"> " + df.format(probDist_TFclass.get(seq)[TF]) + " </td></tr>\n");
					bw.write("  <tr><th> Non-TF </th><td class=\"" + outcomesTF[Non_TF] + "\"> " + df.format(probDist_TFclass.get(seq)[Non_TF]) + " </td></tr>\n");
					bw.write("</table>\n\n");
					bw.write("<br>\n\n");
					    
					bw.write("<h2>Superclass prediction:</h2>\n");
					if (seqIsTF.get(seq)) {
						String[] outcomesSuper = getClassificationOutcomes(BasicTools.Double2double(probDist_Superclass.get(seq)));
						bw.write("<table>\n");
						bw.write("  <tr><th></th><th> Probability </th></tr>\n");
						bw.write("  <tr><th> Basic domain </th><td class=\"" + outcomesSuper[Basic_domain] + "\"> " + df.format(probDist_Superclass.get(seq)[Basic_domain]) + " </td></tr>\n");
						bw.write("  <tr><th> Zinc finger </th><td class=\"" + outcomesSuper[Zinc_finger] + "\"> " + df.format(probDist_Superclass.get(seq)[Zinc_finger]) + " </td></tr>\n");
						bw.write("  <tr><th> Helix-turn-helix </th><td class=\"" + outcomesSuper[Helix_turn_helix] + "\"> " + df.format(probDist_Superclass.get(seq)[Helix_turn_helix]) + " </td></tr>\n");
						bw.write("  <tr><th> Beta scaffold </th><td class=\"" + outcomesSuper[Beta_scaffold] + "\"> " + df.format(probDist_Superclass.get(seq)[Beta_scaffold]) + " </td></tr>\n");
						bw.write("  <tr><th> Other </th><td class=\"" + outcomesSuper[Other] + "\"> " + df.format(probDist_Superclass.get(seq)[Other]) + " </td></tr>\n");
						bw.write("</table>\n\n");
						bw.write("<br>\n\n");    	
						
						bw.write("<h2>Annotated structural class:</h2>\n");
			    		if (annotatedClassAvailable.get(seq)) {	
							bw.write("<h3>" + getAnnotatedSuperclass(annotatedClass.get(seq)) + " (<a href=\"" + transfacURL + "\" target=\"_blank\">" + annotatedClass.get(seq) + "</a>) </h3>\n");
							bw.write("<br>\n\n");
						} else {
				    		bw.write("<h3>Not available.</h3>\n");
				    		bw.write("<br>\n\n");
				    	}
					    
			    		// include result image from InterProScan into HTML report
					    if (!seq2job.isEmpty() && seq2job.containsKey(seq)) {
					    	String job = seq2job.get(seq);
							bw.write("<table>\n");
							bw.write("  <tr><th> <img src=\"" + job + ".visual-png.png\"/>" + "</th></tr>\n");	
							bw.write("  <tr><th> Illustration generated by <a href=https://www.ebi.ac.uk/Tools/services/web/toolresult.ebi?jobId="+job+"&tool=iprscan&analysis=visual target=\"_blank\"> InterProScan </a> </th></tr>\n");
							bw.write("</table>\n\n");
							bw.write("<br>\n\n");
					    }
			    		bw.write("<h2>DNA-binding domain(s):</h2>\n");
					    if (domainsPredicted.get(seq)) {
							bw.write("<table>\n");
							bw.write("  <tr><th> Domain ID </th><th> Start </th><th> End </th></tr>\n");	
					    	
							for (String domain : bindingDomains.get(seq)) {
								String[] splitted_domain = domain.replace("    ", "\t").split("\t");
								String currLink =  "<a href=\"" + interproPrefix + splitted_domain[0] + "\" target=\"_blank\"> " + splitted_domain[0] + " </a>";
								bw.write("  <tr><td> "+ currLink + " </td><td> "+ splitted_domain[1] +" </td><td> " + splitted_domain[2] +" </td></tr>\n"); 
							}
							bw.write("</table>\n\n");
							bw.write("<br>\n\n");
							
					    } else {
				    		bw.write("<h2>No DNA-binding domain found.</h2>\n");
				    	}
					    
				    // if sequence was classified as Non-TF --> display message 
					} else {
						bw.write("<h3>No prediction possible.</h3>");
				    	bw.write("The given sequence was classified as a Non-TF. As all further classification steps (e.g., superclass and DNA-binding domain prediction) require a TF sequence, these steps were not performed.");
					}
			    } else {
			    	bw.write("<h3>No prediction possible.</h3>");
			    	bw.write("InterProScan did not detect any of the domains relevant for TF-/Non-TF classification in the given sequence. Consequently, TFpredict could not perform the prediction task.");
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
	
	private void writeConsoleOutput() {
		
		String hline = "  -----------------------";
		
		for (int i=0; i<sequence_ids.length; i++) {
			String seq = sequence_ids[i];
			
			if (i > 0) {
				System.out.println("__________________________________________");
			}
			
			System.out.println("\n==========================================");
			System.out.println("Results report for sequence: " + seq);
			System.out.println("==========================================\n");
			
			if (predictionPossible.get(seq)) {
				System.out.println("  TF/Non-TF prediction:");
				System.out.println(hline);
				System.out.println("                Probability");
				System.out.println("  TF            " + df.format(probDist_TFclass.get(seq)[TF]));
				System.out.println("  Non-TF        " + df.format(probDist_TFclass.get(seq)[Non_TF]) + "\n");

				if (seqIsTF.get(seq)) {
					System.out.println("  Superclass prediction:");
					System.out.println(hline);
					System.out.println("                      Probability");
					System.out.println("  Basic domain        " + df.format(probDist_Superclass.get(seq)[Basic_domain]));
					System.out.println("  Zinc finger         " + df.format(probDist_Superclass.get(seq)[Zinc_finger]));
					System.out.println("  Helix-turn-helix    " + df.format(probDist_Superclass.get(seq)[Helix_turn_helix]));
					System.out.println("  Beta scaffold       " + df.format(probDist_Superclass.get(seq)[Beta_scaffold]));
					System.out.println("  Other               " + df.format(probDist_Superclass.get(seq)[Other]) + "\n");
				
					if (annotatedClassAvailable.get(seq)) {	
						System.out.println("  Annotated structural class:");
						System.out.println(hline);
						System.out.println("  " + getAnnotatedSuperclass(annotatedClass.get(seq)) + " (" + annotatedClass.get(seq) + ") \n");
					}
					
					if (domainsPredicted.get(seq)) {
						System.out.println("  DNA-binding domain(s):");
						System.out.println(hline);
						System.out.println("  Domain ID \t Start \t End");
						for (String domain : bindingDomains.get(seq)) {
							String[] splitted_domain = domain.replace("    ", "\t").split("\t");
							System.out.println("  " + splitted_domain[0] + " \t " + splitted_domain[1] + " \t " + splitted_domain[2]); 
						}
					
					} else {
						System.out.println("  DNA-binding domain could be predicted.\n");
					}
				}
				
			} else {
				System.out.println("  No prediction possible.\n");
			}
		}
	}
	
	private void writeSABINEoutput() {
			
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(sabine_outfile)));
			
			for (int i=0; i<sequence_ids.length; i++) {
				String seq = sequence_ids[i];
				
				if (i > 0) {
					bw.write("//\nXX\n");
				}
				
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
				
				if (predictionPossible.get(seq) && seqIsTF.get(seq)) {
					
					if (annotatedClassAvailable.get(seq)) {
						bw.write("CL  " + expandTransfacClass(annotatedClass.get(seq)) + "\n");
					} else {
						bw.write("CL  " + predictedSuperclass.get(seq) + ".0.0.0.0" + "\n");
					}
					bw.write("XX  \n");
		
					// write sequence
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
					
				// Protein was either not classified (no IPR domains found) or classified as Non-TF
				} else {
					if (predictionPossible.get(seq)) {
						bw.write("CL  Non-TF\nXX\n");
					} else {
						bw.write("CL  Unknown\nXX\n");
					}
				}
			}
			bw.flush();
			bw.close();
			
		} catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("IOException occurred while writing input file for SABINE.");
		}
	}
	
	// returns "win", "lose", or "draw" for each class depending on the given probabilities
	private static String[] getClassificationOutcomes(double[] probDist) {
		
		String[] classOutcome = new String[probDist.length];
		double maxProb = BasicTools.getMax(probDist);
		ArrayList<Integer> winIdx = new ArrayList<Integer>();
		int numWinners = 0;
		
		for (int i=0; i<probDist.length; i++) {
			if (probDist[i] == maxProb) {
				classOutcome[i] = "win";
				winIdx.add(i);
				numWinners++;
			} else {
				classOutcome[i] = "lose";
			}
		}
		
		// multiple winning classes -> set outcome for winning classes to "draw" 
		if (numWinners > 1) {
			for (int i=0; i<winIdx.size(); i++) {
				classOutcome[winIdx.get(i)] = "draw";
			}
		}
		return classOutcome;
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
	    	
	    	String fvector = createIPRvector(entry.domain_ids, relIPRdomains, 10);
	    	
	    	if (!fvector.isEmpty()) {
	    		seq2fvector.put(entry.sequence_id, getInst("0 " + fvector));
	    	}
	    }
		return seq2fvector;
	}

	
	public static String createIPRvector(ArrayList<String> predIPRdomains, ArrayList<String> relIPRdomains, int start) {
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
