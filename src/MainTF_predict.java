/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

import modes.GalaxyPredict;
import modes.Predict;
import modes.SuperTrain;
import modes.Train;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


public class MainTF_predict {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// create Options object
		Options options = new Options();

		// mandatory arguments passed from Galaxy
		options.addOption("galaxy", false, "switch to run web-tool version of the tool");
		options.addOption("sequence", true, "input protein sequence");
		options.addOption("uniprot_id", true, "input UniProt Accession Number or Entry Name");
		options.addOption("fasta", true, "input FASTA file for batch mode");
		options.addOption("html_outfile", true, "output HTML report");
		options.addOption("sabine_outfile", true, "output file in SABINE format");
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
		
		// options not needed for web-tool version of TFpredict
		options.addOption("train", false, "train classifier");
		options.addOption("super", false, "train super classifier");
		options.addOption("predict", true, "predict this sequence file");
		options.addOption("f", true, "number of folds");		
		
		CommandLine cmd = null;
		CommandLineParser cmdparser = new PosixParser();
		try {
			cmd = cmdparser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		HelpFormatter lvFormater = new HelpFormatter();
		
		if (cmd.hasOption("train")) {
			System.out.println("Training mode.");
			try {
				Train.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (cmd.hasOption("super")) {
			System.out.println("Super-Training mode.");
			try {
				SuperTrain.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (cmd.hasOption("predict")) {
			System.out.println("Prediction mode.");
			try {
				Predict.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (cmd.hasOption("galaxy")) {
			try {
				GalaxyPredict.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			lvFormater.printHelp("Available options: ", options);
		}
	}
}
