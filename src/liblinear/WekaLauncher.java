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
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import liblinear.WekaClassifier.ClassificationMethod;


public class WekaLauncher {
	
	public WekaLauncher(String featureFile, String resultsDir, String modelFileDir, boolean computeClassProbabilities) {
		this(featureFile, resultsDir, modelFileDir);
		if (computeClassProbabilities) {
			classProbFileDir = resultsDir;
		}
	}
	
	public WekaLauncher(String featureFile, String resultsDir, String modelFileDir) {
		this(featureFile, resultsDir);
		this.modelFileDir = modelFileDir;
	}
	
	public WekaLauncher(String featureFile, String resultsDir) {
		this(featureFile);
		this.resultsDir = resultsDir;
	}
	
	public WekaLauncher(String featureFile) {
		libsvmFeatureFile = featureFile;
	}

	private static final String mergedResultsFileName = "evaluationResults.txt";
	
	static boolean silent = false;
	static PrintStream defaultOutstream = System.out;
	
	private String libsvmFeatureFile;
    private String resultsDir = null;
    private String modelFileDir = null;
    private String classProbFileDir = null;
    private String cvSplitFile = null;

    private boolean nestedCV = false;
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

	
	private String[] getClassifierArguments(String classifierName, boolean multithreading) {
		
		ArrayList<String> argsClassifier = new ArrayList<String>();
		
		argsClassifier.add("-c");
		argsClassifier.add(classifierName);
		argsClassifier.add("-f");
		argsClassifier.add(libsvmFeatureFile);
		argsClassifier.add("-r");
		argsClassifier.add(Integer.toString(multiruns));
		argsClassifier.add("-v");
		argsClassifier.add(Integer.toString(folds));
		if (classProbFileDir != null) {
			argsClassifier.add("-p");
			argsClassifier.add(classProbFileDir + ClassificationMethod.valueOf(classifierName).modelFileName.replace(".model", ".prob"));
		}
		if (modelFileDir != null) {
			argsClassifier.add("-m");
			argsClassifier.add(modelFileDir + ClassificationMethod.valueOf(classifierName).modelFileName);
		}
		if (nestedCV) {
			argsClassifier.add("-n");
			argsClassifier.add("true");
		} else {
			argsClassifier.add("-n");
			argsClassifier.add("false");
		}
		if (resultsDir != null) {
			argsClassifier.add("-s");
			argsClassifier.add(resultsDir + ClassificationMethod.valueOf(classifierName).modelFileName.replace(".model", ".eval"));
		}
		if (multithreading) {
			argsClassifier.add("-t");
			argsClassifier.add("true");
		}
		
		return argsClassifier.toArray(new String[]{});
	}
	
	private void saveWekaCVsplit() {
		
		// get unscaled libsvm feature vectors for each run and split
		String[][][] splittedDatasets = new String[multiruns][][];
		for (int run = 0; run < multiruns; run++) {
			splittedDatasets[run] = WekaClassifier.getSplittedDataset(libsvmFeatureFile, folds, run);
		}
		
		// read original feature file
		ArrayList<String> featureVectors = BasicTools.readFile2List(libsvmFeatureFile, false);
		for (int i=0; i<featureVectors.size(); i++) {
			String currFeatVec = featureVectors.get(i);
			if (currFeatVec.startsWith("-1")) {
				featureVectors.set(i, currFeatVec.replaceFirst("-1", "0"));
			} else if (currFeatVec.startsWith("+1")) {
				featureVectors.set(i, currFeatVec.replaceFirst("\\+1", "1"));
			}
		}
		
		// reconstruct random splits and permutations performed by Weka during cross-validation
		int [][][] cvSplit = new int[multiruns][folds][];
		for (int run = 0; run < multiruns; run++) {
			for (int fold = 0; fold < folds; fold++) {
				int foldSize = splittedDatasets[run][fold].length;
				int[] foldPerm = new int[foldSize];
				for (int instIdx = 0; instIdx < foldSize; instIdx++) {
					 String featVec = splittedDatasets[run][fold][instIdx].replaceAll("([0-9])\\.0\\s", "$1 ").replaceFirst("\\.0$", "");
					 int idx = featureVectors.indexOf(featVec);
					 if (idx != -1) {
						 foldPerm[instIdx] = idx;
					 
					 // instance was not found in CV-split --> Error.
					 } else {
						 System.out.println("Error. Feature vector was not found.");
						 System.out.println(featVec);
						 System.exit(0);
					 }
				}
				cvSplit[run][fold] = foldPerm;
			}
		}
		
		// write cross-validation split to file
		ArrayList<String> cvSplitList = new ArrayList<String>();
		for (int run = 0; run < multiruns; run++) {
			for (int fold = 0; fold < folds; fold++) {
				StringBuffer cvSplitLine = new StringBuffer("" + cvSplit[run][fold][0]);
				for (int instIdx = 1; instIdx < cvSplit[run][fold].length; instIdx++) {
					cvSplitLine.append(" " + cvSplit[run][fold][instIdx]);
				}
				cvSplitList.add(cvSplitLine.toString());
			}
		}
		BasicTools.writeArrayList2File(cvSplitList, cvSplitFile);
	}
	
	public double[][] runWekaClassifier(Boolean multithreading) {
		
		//save cross-validation split used by Weka
		if (resultsDir != null) {
			cvSplitFile = resultsDir + "cvSplit.txt";
			saveWekaCVsplit();
		}
		
		// run classifiers in separate threads
		if (multithreading) {
			Collection<Job> queue = new ArrayList<Job>();
			
			for (ClassificationMethod classMethod: ClassificationMethod.values()) {
				queue.add(new Job(classMethod.name()));
			}
			
			ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			try {
				exec.invokeAll(queue);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			exec.shutdown();
			while (!exec.isTerminated());
			
		
		// run classifiers in single thread
		} else {
			for (ClassificationMethod classMethod: ClassificationMethod.values()) {
				String[] argsClassifier = getClassifierArguments(classMethod.name(), multithreading);
				try {
					WekaClassifier.main(argsClassifier);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// return results from evaluation of classifiers
		double[][] classResults = null;
		if (resultsDir != null) {
			mergeResultsFiles();
			classResults = parseResultsFile();
		} 
		
		return(classResults);
	}
	
	
	private void mergeResultsFiles() {
		String mergedClassResults = "";
		for (ClassificationMethod classMethod: ClassificationMethod.values()) {
			String classResultFile = resultsDir + classMethod.modelFileName.replace(".model", ".eval");
			mergedClassResults = mergedClassResults.concat(BasicTools.readFile2String(classResultFile));
		}
		BasicTools.writeString2File(mergedClassResults, resultsDir + mergedResultsFileName);
	}
	
	private double[][] parseResultsFile() {
		
		int numScores = folds * multiruns;
		int numMethods = ClassificationMethod.values().length;
		double[][] classResults = new double[numScores][numMethods];
		ArrayList<String[]> resultsTable = BasicTools.readFile2ListSplitLines(resultsDir + mergedResultsFileName);
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
	
	class Job implements Callable<String>{
		
		String classifierType;
		
		public Job(String argsClassifier) {
			this.classifierType = argsClassifier;

		}

		@Override
		public String call() throws Exception {

			String modelFile = modelFileDir + ClassificationMethod.valueOf(classifierType).modelFileName;
			String summaryFile = resultsDir + ClassificationMethod.valueOf(classifierType).modelFileName.replace(".model", ".eval");
			String classProbFile = classProbFileDir + ClassificationMethod.valueOf(classifierType).modelFileName.replace(".model", ".prob");

			WekaClassifier classifier = new WekaClassifier(classifierType, libsvmFeatureFile, multiruns, folds, nestedCV, modelFile, summaryFile, classProbFile);
			classifier.setMultithreading(true);
			classifier.run();

			return "Done.";
		}
	}
	
	
	public static void main(String[] args) {
		
		String feature_file = "/rahome/eichner/projects/tfpredict/data/super_pred/feature_files/latest/libsvm_featurefile_small.txt";
		String results_dir = "/rahome/eichner/Desktop/tmp/class_dir/";
		String model_dir = "/rahome/eichner/Desktop/tmp/model_dir/";
		
		WekaLauncher launcher = new WekaLauncher(feature_file, results_dir, model_dir, true);
		launcher.printConfiguration();
		launcher.runWekaClassifier(false);
	}
	
	private void printConfiguration() {
		System.out.println("Input File:                " + libsvmFeatureFile);
		if (resultsDir != null) {
			System.out.println("Output File:               " + resultsDir);
		}
		if (modelFileDir != null) {
			System.out.println("Model File(s):             " + modelFileDir);
		}
		if (classProbFileDir != null) {
			System.out.println("Class Probability File(s): " + classProbFileDir);
		}
		System.out.println("Multiruns:                 " + multiruns);
		System.out.println("Folds:            	   " + folds);
		System.out.println(nestedCV ? "Nested CV:                 yes" : "Nested CV:                 no");
	}
	
}


