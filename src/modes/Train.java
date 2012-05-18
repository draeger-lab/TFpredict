package modes;

import features.SuperPredFeatureFileGenerator;
import features.TFpredFeatureFileGenerator;
import io.BasicTools;

import java.text.DecimalFormat;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

import org.apache.commons.cli.CommandLine;

import liblinear.WekaClassifier;
import liblinear.WekaLauncher;

public class Train {

	private static boolean silent = false;

	private static String featureFile;
	private static String classResultsFile;
	private static String modelFileDir;
	
	private static int multiruns = 1;
	private static int folds = 4;
	private static boolean nestedCV = false;

	
	private static void parseArguments(CommandLine cmd) {
		
		if(cmd.hasOption("featureFile")) {
			featureFile = new String(cmd.getOptionValue("featureFile"));
		}
		
		if(cmd.hasOption("resultsFile")) {
			classResultsFile = new String(cmd.getOptionValue("resultsFile"));
		}
		
		if(cmd.hasOption("modelFileDir")) {
			modelFileDir = new String(cmd.getOptionValue("modelFileDir"));;
		}
		
		if(cmd.hasOption("multiruns")) {
			multiruns = new Integer(cmd.getOptionValue("multiruns"));
		}
		
		if(cmd.hasOption("folds")) {
			folds = new Integer(cmd.getOptionValue("folds"));
		}
		
		if(cmd.hasOption("nestedCV")) {
			nestedCV = true;
		} else {
			nestedCV = false;
		}
	}
	
	
	private static void compareClassifiers(String featureFile, String resultsFile, String modelFileDir) {
		compareClassifiers(featureFile, resultsFile, modelFileDir, multiruns, folds, nestedCV);
	}
	
	private static void compareClassifiers(String featureFile, String resultsFile, String modelFileDir, int numMultiruns, int numFolds, boolean nestedCV) {	
		WekaLauncher launcher = new WekaLauncher(featureFile, resultsFile, modelFileDir);
		launcher.setFolds(numFolds);
		launcher.setMultiruns(numMultiruns);
		launcher.setNestedCV(nestedCV);
		
		double[][] classResults = launcher.runWekaClassifier();
		
		// determine best classifier
		double[] meanROC = BasicTools.getColMeans(classResults);
		int winIdx = BasicTools.getMaxIndex(meanROC);
		
		// write short report of classification
		if (!silent) {
			
			System.out.println("\n=======================");
			System.out.println("Classification results:");
			System.out.println("=======================");
			int paddingLength = 25;
			DecimalFormat df = new DecimalFormat();
			System.out.println(BasicTools.padRight("  Best classifier:", paddingLength) + WekaClassifier.ClassificationMethod.values()[winIdx].printName + "\n");
			System.out.println(BasicTools.padRight("  Classifier", paddingLength) + "ROC score");
			for (int i=0; i<meanROC.length; i++) {
				System.out.println("    " + BasicTools.padRight(WekaClassifier.ClassificationMethod.values()[i].printName + ":", paddingLength-2)  + df.format(meanROC[i]));
			}
		}
	}
	
	public static void main(CommandLine cmd) {
	
		parseArguments(cmd);
		compareClassifiers(featureFile, classResultsFile, modelFileDir, multiruns, folds, nestedCV);
		
		//String dataDir = "/rahome/eichner/projects/tfpredict/data/";
		
		/*
		 *  Generation of feature files
		 */
		
		/*
		// generate feature file for TF prediction
		String iprscanResultFileTF =  dataDir + "tf_pred/interpro_files/TF.fasta.out"; 
		String iprscanResultsFileNonTF = dataDir + "tf_pred/interpro_files/NonTF.fasta.out";
		String tfFeatureFile = dataDir + "tf_pred/libsvm_files/libsvm_featurefile.txt";
		TFpredFeatureFileGenerator tfFeatFileGenerator = new TFpredFeatureFileGenerator(iprscanResultFileTF, iprscanResultsFileNonTF, tfFeatureFile);
		tfFeatFileGenerator.writeFeatureFile();
		
	    // generate feature file for superclass prediction
		String fastaFileSuper =  dataDir + "super_pred/fasta_files/superclassTF.fasta"; 
		String iprscanResultFileSuper =  dataDir + "super_pred/interpro_files/superclassTF.fasta.out"; 
		String superFeatureFile = dataDir + "super_pred/libsvm_files/libsvm_featurefile.txt";
		SuperPredFeatureFileGenerator superFeatFileGenerator = new SuperPredFeatureFileGenerator(fastaFileSuper, iprscanResultFileSuper, superFeatureFile);
		superFeatFileGenerator.writeFeatureFile();
		*/
		
		/*
		 *  Training and comparison of classifiers
		 */
		
		/*
		// compare classifiers and generate model files for TF prediction
		String tfFeatureFile = dataDir + "tf_pred/libsvm_files/libsvm_featurefile.txt";
		String tfResultsFile = dataDir + "tf_pred/classifier_comparison.txt";
		String tfModelDir = dataDir + "tf_pred/model_files/";
		
		compareClassifiers(tfFeatureFile, tfResultsFile, tfModelDir);
		
		// compare classifiers and generate model files for superclass prediction
		String superFeatureFile = dataDir + "super_pred/libsvm_files/libsvm_featurefile.txt";
		String superResultsFile = dataDir + "super_pred/classifier_comparison.txt";
		String superModelDir = dataDir + "super_pred/model_files/";
		 
		compareClassifiers(superFeatureFile, superResultsFile, superModelDir);
		*/
	}
} 
