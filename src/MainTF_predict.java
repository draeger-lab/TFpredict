/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

import modes.GalaxyPredict;
import modes.GalaxyPredictBatch;
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

		// galaxy options
		options.addOption("galaxy", false, "predict the given sequence");
		options.addOption("galaxybatch", true, "predict the given sequences");
		options.addOption("basedir", true, "directory for temporary files");
		options.addOption("html_outfile", true, "HTML report");
		options.addOption("sabine_outfile", true, "output file in SABINE format");
		options.addOption("sequence", true, "output file in SABINE format");
		options.addOption("uniprot_id", true, "UniProt Accession Number or Entry Name");
		options.addOption("species", true, "organism (e.g., Homo sapiens)");
		
		// add options
		options.addOption("train", false, "train classifier");
		options.addOption("super", false, "train super classifier");
		options.addOption("predict", true, "predict this sequence file");
		options.addOption("i", true, "model input");
		options.addOption("s", true, "super model input");
		options.addOption("f", true, "number of folds");
		options.addOption("p", true, "Path to IPRscan");
		options.addOption("r", true, "iprs object");
		options.addOption("sr", true, "super iprs object");
		options.addOption("g", true, "go-terms file");
		options.addOption("t", true, "transfac map");

		
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
		else if (cmd.hasOption("galaxybatch")) {
			try {
				GalaxyPredictBatch.main(cmd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			lvFormater.printHelp("Available options: ", options);
		}
	}
}
