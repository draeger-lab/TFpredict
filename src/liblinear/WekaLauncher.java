/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

package liblinear;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;


public class WekaLauncher {
	
	
	private static Integer[] classificationMethods = new Integer[] {
		WekaClassifier.RandomForest,
		WekaClassifier.DecisionTree,
		WekaClassifier.SVM_rbf,
		WekaClassifier.SVM_linear,
		WekaClassifier.NaiveBayes,
		WekaClassifier.Kstar,
		WekaClassifier.KNN,
	};
	
	/*
	private static Integer[] classificationMethods = new Integer[] {
		WekaClassifier.SVM_rbf
	};
	*/
	
	private static final String[] modelFileNames = new String[] {
		"randomForest.model", "decisionTree.model", "svmRBF.model",
		"svmLinear.model", "naiveBayes.model", "kStar.model", "knn.model"
	};
	
	static boolean silent = false;
	
	static String libsvmFeatureFile;
	static String wekaOutputFile;
    static String resultsFile = null;
    static String modelFileDir = null;
    static boolean performNestedCV = true;
	static int multiruns = 1;
	static int folds = 4;
	
	static PrintStream defaultOutstream = System.out;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		parseArguments(args);
		printConfiguration();
		runWekaClassifier();
	}
	
	
	private static void parseArguments(String[] args) {
		
		if (args.length == 0) {
			System.out.println("Error. Please specify libsvm feature file as first argument.");
			System.out.println("Optionally, an output file can be specified as second argument.");
			System.exit(0);
		}
		libsvmFeatureFile = args[0];
		if (args.length > 1) {
			resultsFile = args[1];
		}
		if (args.length > 2) {
			modelFileDir = args[2];
			if (!modelFileDir.endsWith(File.separator)) {
				modelFileDir = modelFileDir + File.separator;
			}
		}
	}
	
	private static void printConfiguration() {
		System.out.println("Input File:    " + libsvmFeatureFile);
		if (resultsFile != null) {
			System.out.println("Output File:   " + resultsFile);
		}
		if (modelFileDir != null) {
			System.out.println("Model File(s): " + modelFileDir);
		}
		System.out.println("Multiruns:     " + multiruns);
		System.out.println("Folds:         " + folds);
		System.out.println(performNestedCV ? "Nested CV:     yes" : "Nested CV:     no");
	}
	
	private static String[] getClassifierArguments(int classifierID) {
		
		ArrayList<String> argsClassifier = new ArrayList<String>();
		
		argsClassifier.add("-c");
		argsClassifier.add(Integer.toString(classifierID));
		argsClassifier.add("-f");
		argsClassifier.add(libsvmFeatureFile);
		argsClassifier.add("-r");
		argsClassifier.add(Integer.toString(multiruns));
		argsClassifier.add("-v");
		argsClassifier.add(Integer.toString(folds));
		if (modelFileDir != null) {
			argsClassifier.add("-m");
			argsClassifier.add(modelFileDir + modelFileNames[classifierID]);
		}
		if (performNestedCV) {
			argsClassifier.add("-n");
			argsClassifier.add("true");
		} else {
			argsClassifier.add("-n");
			argsClassifier.add("false");
		}
		
		return argsClassifier.toArray(new String[]{});
	}
	
	private static void runWekaClassifier() {
		
		// redirect standard output of classifier to file (if desired)
		//if (resultsFile != null) {
		//	redirectSystemOut(resultsFile);
		//}
		
		// run all Weka classifiers on given feature file
		for (Integer classifierID: classificationMethods) {
			String[] argsClassifier = getClassifierArguments(classifierID);
			try {
				System.out.println();
				WekaClassifier.main(argsClassifier);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// reset output stream
		if (resultsFile != null) {
			System.setOut(defaultOutstream);
		} 
	}
				

	public static String redirectSystemOut2TempFile() {
		String tempFile = "";
		try {
			tempFile = File.createTempFile("wekaTemp_", ".txt").getAbsolutePath();
			redirectSystemOut(tempFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tempFile;
	}
	
	public static void redirectSystemOut(String outfile) {
        try {
            System.setOut(new PrintStream(new FileOutputStream(outfile)));
            
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return;
        }
    }
}
