/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

package liblinear;

import io.BasicTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;


public class WekaLauncher {
	
	public WekaLauncher(String featureFile, String resultsFile, String modelFileDir) {
		this(featureFile, resultsFile);
		this.modelFileDir = modelFileDir;
	}
	
	public WekaLauncher(String featureFile, String resultsFile) {
		this(featureFile);
		this.resultsFile = resultsFile;
	}
	
	public WekaLauncher(String featureFile) {
		libsvmFeatureFile = featureFile;
	}
	
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
	static PrintStream defaultOutstream = System.out;
	
	private String libsvmFeatureFile;
    private String resultsFile = null;
    private String modelFileDir = null;
    
    private boolean nestedCV = true;
	private int multiruns = 1;
    private int folds = 4;
	

	public void setNestedCV(boolean performNestedCV) {
		this.nestedCV = performNestedCV;
	}

	public void setMultiruns(int multiruns) {
		this.multiruns = multiruns;
	}

	public void setFolds(int folds) {
		this.folds = folds;
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {

		WekaLauncher launcher = new WekaLauncher(args[0], args[1]);
		
		launcher.printConfiguration();
		launcher.runWekaClassifier();
	}
	
	private void printConfiguration() {
		System.out.println("Input File:    " + libsvmFeatureFile);
		if (resultsFile != null) {
			System.out.println("Output File:   " + resultsFile);
		}
		if (modelFileDir != null) {
			System.out.println("Model File(s): " + modelFileDir);
		}
		System.out.println("Multiruns:     " + multiruns);
		System.out.println("Folds:         " + folds);
		System.out.println(nestedCV ? "Nested CV:     yes" : "Nested CV:     no");
	}
	
	private String[] getClassifierArguments(int classifierID) {
		
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
		if (nestedCV) {
			argsClassifier.add("-n");
			argsClassifier.add("true");
		} else {
			argsClassifier.add("-n");
			argsClassifier.add("false");
		}
		
		return argsClassifier.toArray(new String[]{});
	}
	
	public double[][] runWekaClassifier() {
		
		// redirect standard output of classifier to file (if desired)
		if (resultsFile != null) {
			redirectSystemOut(resultsFile);
		}
		
		// run all Weka classifiers on given feature file
		double[][] classResults = null;
		for (int i=0; i<classificationMethods.length; i++) {
			String[] argsClassifier = getClassifierArguments(classificationMethods[i]);
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
			classResults = parseResultsFile();
		} 
		
		return(classResults);
	}
	
	private double[][] parseResultsFile() {
		
		int numScores = folds * multiruns;
		int numMethods = classificationMethods.length;
		double[][] classResults = new double[numScores][numMethods];
		ArrayList<String[]> resultsTable = BasicTools.readFile2ListSplitLines(resultsFile);
		int lineIdx = 0;
		for (int i=0; i<numMethods; i++) {
			lineIdx += 2; // skip classifier name and table header
			for (int j=0; j<numScores; j++) {
				classResults[j][i] = Double.parseDouble(resultsTable.get(lineIdx++)[0]);
			}
		}
		return(classResults);
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
