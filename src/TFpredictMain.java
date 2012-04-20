/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

import java.io.File;
import java.io.IOException;

import modes.Predict;
import modes.Train;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


public class TFpredictMain {

	private static boolean galaxyMode = false;
	private static boolean standAloneMode = false;
	private static boolean trainMode = false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if (args.length == 0) {
			printCopyright();
		}
		
		// switch between modes based on first argument
		getMode(args[0]);
		
		if (standAloneMode) {
			args = prepareStandAloneMode(args);
		}
		
		// create Options object
		Options options = createOptions(args);

		// parse arguments	
		CommandLine cmd = null;
		CommandLineParser cmdparser = new PosixParser();
		try {
			cmd = cmdparser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		if (galaxyMode) {
			try {
				Predict.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		} else if (trainMode) {
			try {
				Train.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		} else if (standAloneMode) {
			printCopyright();
			checkArguments(cmd);
			try {
				Predict.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	// returns the correct mode based on the string passed as first argument
	private static void getMode(String firstArg) {
		if (firstArg.equals("-galaxy")) {
			galaxyMode = true;
			
		} else if (firstArg.equals("-train")) {
			trainMode = true;
			
		} else {
			standAloneMode = true;
		}
	}
	
	private static String[] prepareStandAloneMode(String[] args) {

		// generate base directory to save temporary files
		File tempDir = null;
		try {
			tempDir = File.createTempFile("TFpredict_", "_basedir");
			tempDir.delete();
			if (!tempDir.mkdir()) {
				System.out.println("Error. Directory to save temporary files could not be created.");
				System.exit(0);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
        
		// append argument name "-fasta" and base directory to complete command line input
		String[] originalArgs = args;
		args = new String[originalArgs.length+4];
		args[0] = "-fasta";
		for (int i=0; i<originalArgs.length; i++) {
			args[i+1] = originalArgs[i];
		}
		args[args.length-3] = "-standAloneMode";
		args[args.length-2] = "-basedir";
		args[args.length-1] = tempDir.getAbsolutePath() + File.separator;
		
		return(args);
	}
	
	private static Options createOptions(String[] args) {
		
		Options options = new Options();
		
		// GALAXY MODE
		if (galaxyMode) {
			
			// mandatory arguments passed from Galaxy
			options.addOption("galaxy", false, "use InterProScan webservice");
			options.addOption("sequence", true, "input protein sequence");
			options.addOption("uniprotID", true, "input UniProt Accession Number or Entry Name");
			options.addOption("fasta", true, "input FASTA file for batch mode");
			options.addOption("htmlOutfile", true, "output HTML report");
			options.addOption("sabineOutfile", true, "output file in SABINE format");
			options.addOption("basedir", true, "directory for temporary files");
			options.addOption("species", true, "organism (e.g. Homo sapiens)");
			options.addOption("useWeb", false, "use InterProScan webservice");
			
			// optional arguments
			options.addOption("iprscanPath", true, "path to InterProScan");
			options.addOption("tfClassifierFile", true, "file containing TF/Non-TF classifier");
			options.addOption("superClassifierFile", true, "file containing Superclass classifier");
			options.addOption("tfClassFeatureFile", true, "file containing features used by TF/Non-TF classifier");
			options.addOption("superClassFeatureFile", true, "file containing features used by Superclass classifier");
			options.addOption("relGOtermsFile", true, "file containing GO terms relevant for DNA-binding domain prediction");
			options.addOption("tfName2ClassFile", true, "file containing mapping from TF names to TransFac classes");
		
		// TRAINING MODE
		//TODO: @Florian: Trainingsmode überarbeiten
		} else if (trainMode) {
				
			// options not needed for web-tool version of TFpredict
			options.addOption("train", false, "train classifier");
			options.addOption("super", false, "train super classifier");
			options.addOption("predict", true, "predict this sequence file");
			options.addOption("f", true, "number of folds");	
			
	    // STAND-ALONE-MODE
		} else if (standAloneMode) {
			
			options.addOption("fasta", true, "input FASTA file for batch mode");
			options.addOption("sabineOutfile", true, "output file in SABINE format");
			options.addOption("species", true, "organism (e.g. Homo sapiens)");
			options.addOption("iprscanPath", true, "path to InterProScan");
			options.addOption("basedir", true, "directory for temporary files");
			options.addOption("standAloneMode", false, "directory for temporary files");
		}
		return options;
	}
	
	private static void checkArguments(CommandLine cmd) {
		
		// check if input FASTA file exists
		if (!new File(cmd.getOptionValue("fasta")).exists()) {
			System.out.println("Error. Input FASTA file not found.");
			usage();
		}
		
		//  if SABINE output file shall be generated, check if species is also provided
		if (cmd.hasOption("sabineOutfile") && !cmd.hasOption("species")) {
			System.out.println("Error. Species has to be provided as argument if output file for SABINE shall be created.");
			usage();
		
		} else {
			System.out.println("  Input FASTA file:       " + cmd.getOptionValue("fasta"));
			if (cmd.hasOption("sabineOutfile")) {
				System.out.println("  SABINE output file:     " + cmd.getOptionValue("sabineOutfile"));
				System.out.println("  Organism:               " + cmd.getOptionValue("species"));
			} else {
				System.out.println("  SABINE output file:     not generated." + cmd.getOptionValue("sabineOutfile"));
			} 
		}
	}
	
	/*
	 *  print copyright message
	 */
	
	public static void printCopyright() {

		System.out.println("\n-----------------------------------------------------------------------------------");
		System.out.println("TFpredict - Identification and structural characterization of transcription factors");
		System.out.println("-----------------------------------------------------------------------------------");
		System.out.println("(version 1.0)\n");
		System.out.println("Copyright (C) 2012 Center for Bioinformatics T\u00fcbingen (ZBIT),");
        System.out.println("University of T\u00fcbingen, Florian Topf und Johannes Eichner.\n");
        System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
        System.out.println("This is free software, and you are welcome to redistribute it under certain conditions.");
        System.out.print("For details see: ");
        System.out.println("http://www.gnu.org/licenses/gpl-3.0.html\n");
        System.out.println("Third-party software used by this program:");
        System.out.println("  WEKA. Copyright (C) 1998, 1999 Eibe Frank, Leonard Trigg, Mark Hall. All rights reserved.");
        System.out.println("  InterProScan. Copyright (C) 2011 Sarah Hunter, EMBL-EBI. All rights reserved.");
        System.out.println();  
	}
	
	private static void usage() {
		
		System.out.println("  Usage   : java -jar TFpredict.jar <fasta_file> [OPTIONS]\n");
		System.out.println("  OPTIONS : -sabineOutput <output_file_name>");
		System.out.println("            -species <organism_name>          (e.g., \"Homo sapiens\")");		
		System.out.println("            -iprscanPath <path_to_iprscan>    (e.g., \"/opt/iprscan/bin/iprscan\")\n");
		System.exit(0);
		
	}
}
