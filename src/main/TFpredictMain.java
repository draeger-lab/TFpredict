/*  
 * $Id$
 * $URL$
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2013 Center for Bioinformatics Tuebingen (ZBIT),
 * University of Tuebingen by Johannes Eichner, Florian Topf, Andreas Draeger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package main;


import io.BasicTools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import liblinear.WekaClassifier.ClassificationMethod;
import modes.Predict;
import modes.Train;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class TFpredictMain {

	private static boolean galaxyMode = false;
	private static boolean standAloneMode = false;
	private static boolean trainMode = false;
	
	private static final String[] validClassifiers = new String[] {"SVM_linear", "KNN", "NaiveBayes"}; 
	private static final String version = "1.1";
	
	public static final String sabineSpeciesList = "organism_list.txt";
	public static final String classMappingFile = "class_mapping.txt"; 
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		java.util.Locale.setDefault(java.util.Locale.ENGLISH);
		
		if (args.length == 0) {
			printCopyright();
			usage();
		}
		
		// switch between modes based on first argument
		getMode(args[0]);
		
		if (standAloneMode) {
			args = prepareStandAloneMode(args);
		
		} else if (galaxyMode) {
			args = prepareOnlineMode(args);
		}
		
		// create Options object
		Options options = createOptions(args);

		// parse arguments	
		CommandLine cmd = null;
		CommandLineParser cmdparser = new PosixParser();
		try {
			cmd = cmdparser.parse(options, args);
			
		} catch (ParseException e) {
			printCopyright();
			System.out.println("Error. Invalid argument.\n");
			usage();
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
	
	private static String[] prepareOnlineMode(String[] args) {
		
		// concatenate species name to a single argument
		boolean containsSpecies = false;
		int speciesPos = 0;
		for (String arg: args) {
			if (arg.equals("-species")) {
				containsSpecies = true;
				break;
			}
			speciesPos++;
		}
		if (containsSpecies) {
			String[] originalArgs = args;
			args = new String[originalArgs.length-1];
			for (int i=0; i<=speciesPos; i++) {
				args[i] = originalArgs[i];
			}
			args[speciesPos+1] = originalArgs[speciesPos+1] + " " +originalArgs[speciesPos+2];
			for (int i=speciesPos+3; i<originalArgs.length; i++) {
				args[i-1] = originalArgs[i];
			}
		}		
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
			options.addOption("blastPath", true, "path to BLAST");
			
			// optional arguments
			options.addOption("tfClassifier", true, "file containing TF/Non-TF classifier");
			options.addOption("superClassifier", true, "file containing Superclass classifier");
		
		// TRAINING MODE
		} else if (trainMode) {
				
			// options not needed for web-tool version of TFpredict
			options.addOption("train", false, "switch for training mode");
			options.addOption("featureFile", true, "feature file in libsvm format");	
			options.addOption("resultsFileDir", true, "directory containing class probabilities and evaluation results");	
			options.addOption("modelFileDir", true, "directory where model files shall be saved");	
			options.addOption("multiruns", true, "number of repetitions of cross-validation");	
			options.addOption("folds", true, "number of folds");	
			options.addOption("nestedCV", false, "switch to run nested cross-validation with model selection");	
			options.addOption("multiThreading", false, "switch to enables multi-threading");	
			
	    // STAND-ALONE-MODE
		} else if (standAloneMode) {
			
			options.addOption("fasta", true, "input FASTA file for batch mode");
			options.addOption("sabineOutfile", true, "output file in SABINE format");
			options.addOption("species", true, "organism (e.g. Homo sapiens)");
			options.addOption("tfClassifier", true, "file containing TF/Non-TF classifier");
			options.addOption("superClassifier", true, "file containing Superclass classifier");
			options.addOption("iprscanPath", true, "path to InterProScan");
			options.addOption("blastPath", true, "path to BLAST");
			options.addOption("basedir", true, "directory for temporary files");
			options.addOption("standAloneMode", false, "directory for temporary files");
		}
		return options;
	}
	
	private static void checkArguments(CommandLine cmd) {

		// check if input FASTA file exists
		if (!new File(cmd.getOptionValue("fasta")).exists()) {
			System.out.println("  Error. Input FASTA file not found.");
			usage();
		}
		
		//  if SABINE output file shall be generated, check if species is also provided
		if (cmd.hasOption("sabineOutfile") && !cmd.hasOption("species")) {
			System.out.println("  Error. Species has to be provided as argument if output file for SABINE shall be created.");
			usage();
		}
		
		// check species for compatibility with SABINE
		if (cmd.hasOption("species")) { 
			List<String> speciesList = BasicTools.readResource2List(sabineSpeciesList, true);
			String species = cmd.getOptionValue("species");
			if (!speciesList.contains(species.toUpperCase())) {
				System.out.println("  Error. Unknown species: \"" + species + "\". A list of accepted values for the argument \"-species\" can be found here:");
				System.out.println("  http://www.cogsys.cs.uni-tuebingen.de/software/SABINE/doc/organism_list.txt");
				System.out.println("  Please make sure that species names are surrounded by quotes (e.g., -species \"Homo sapiens\").\n"); 
				usage();
			}
		}
		
		// check classifiers
		if (cmd.hasOption("tfClassifier")) {
			checkClassifier("tfClassifer", cmd.getOptionValue("tfClassifier"));
		}
		if (cmd.hasOption("superClassifier")) {
			checkClassifier("superClassifier", cmd.getOptionValue("superClassifier"));
		}
			
		// print values of provided arguments 
		System.out.println("  Input FASTA file:       " + cmd.getOptionValue("fasta"));
		if (cmd.hasOption("sabineOutfile")) {
			System.out.println("  SABINE output file:     " + cmd.getOptionValue("sabineOutfile"));
			System.out.println("  Organism:               " + cmd.getOptionValue("species"));
		} else {
			System.out.println("  SABINE output file:     not generated");
		} 
		String tfClassifier = ClassificationMethod.valueOf("SVM_linear").printName; 
		if (cmd.hasOption("tfClassifier")) {
			tfClassifier = ClassificationMethod.valueOf(cmd.getOptionValue("tfClassifier")).printName; 
		}
		String superClassifier = ClassificationMethod.valueOf("SVM_linear").printName; 
		if (cmd.hasOption("superClassifier")) {
			superClassifier = ClassificationMethod.valueOf(cmd.getOptionValue("superClassifier")).printName; 
		}
		System.out.println("  TF/non-TF classifier:   " + tfClassifier);
		System.out.println("  Superclass classifier:  " + superClassifier);
	}
	
 	public static void checkClassifier(String classifierType, String givenClassifier) {

 		ArrayList<String> classificationMethods = new ArrayList<String>();
 		for (String classMethod: validClassifiers) {
 			classificationMethods.add(classMethod);
 		}

 		if (!classificationMethods.contains(givenClassifier)) {
 			System.out.println("  Error. Unknown classifier: \"" + givenClassifier + "\". The following values are possible for the argument \"" + classifierType + "\":");
 			System.out.print("  " + classificationMethods.get(0));
 			for (int i=1; i<classificationMethods.size(); i++) {
 				System.out.print(", " + classificationMethods.get(i));
 			}
 			System.out.println("\n"); 
 			usage();
 		}
 	}
	
	/*
	 *  print copyright message
	 */
	
	public static void printCopyright() {

		System.out.println("\n-----------------------------------------------------------------------");
		System.out.println("TFpredict");
		System.out.println("Identification and structural characterization of transcription factors");
		System.out.println("-----------------------------------------------------------------------");
		System.out.println("(version " + version + ")\n");
		System.out.println("Copyright (C) 2012 Center for Bioinformatics Tuebingen (ZBIT),");
        System.out.println("University of Tuebingen, Florian Topf and Johannes Eichner.\n");
        System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
        System.out.println("This is free software, and you are welcome to redistribute it under certain");
        System.out.println("conditions.");
        System.out.print("For details see: ");
        System.out.println("http://www.gnu.org/licenses/gpl-3.0.html\n");
        System.out.println("Third-party software used by this program:");
        System.out.println("  WEKA. Copyright (C) 1998, 1999 Eibe Frank, Leonard Trigg, Mark Hall.");
        System.out.println("  All rights reserved.");
        System.out.println("  InterProScan. Copyright (C) 2011 Sarah Hunter, EMBL-EBI. All rights reserved.");
        System.out.println();  
	}
	
	private static void usage() {
		
		/*
		 * Alternative zu fester Usage:
		 * HelpFormatter lvFormater = new HelpFormatter();
		 * lvFormater.printHelp("Available options: ", options);
		 */
		
		System.out.println("  Usage   : java -jar TFpredict.jar <fasta_file> [OPTIONS]\n");
		System.out.println("  OPTIONS : -sabineOutfile <output_file_name>");
		System.out.println("            -species <organism_name>            (e.g., \"Homo sapiens\")");
		System.out.println("            -tfClassifier <classifier_name>     (possible values: SVM_linear, NaiveBayes, KNN)");
		System.out.println("            -superClassifier <classifier_name>  (possible values: SVM_linear, NaiveBayes, KNN)");	
		System.out.println("            -iprscanPath <path_to_iprscan>      (e.g., \"/opt/iprscan/bin/iprscan\")\n");
		System.out.println("            -blastPath <path_to_blast>          (e.g., \"/opt/blast/latest\")\n");
		System.exit(0);
		
	}
}
