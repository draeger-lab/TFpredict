/*
 * $Id: WekaLauncher.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/liblinear/WekaLauncher.java $
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 * 
 * Copyright (C) 2010-2014 Center for Bioinformatics Tuebingen (ZBIT),
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
package liblinear;

import io.BasicTools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import liblinear.WekaClassifier.ClassificationMethod;

/**
 * 
 * @author Florian Topf
 * @version $Rev: 99 $
 * @since 1.0
 */
public class WekaLauncher {

	/**
	 * 
	 * @param featureFile
	 * @param resultsDir
	 * @param modelFileDir
	 * @param computeClassProbabilities if {@code true} the {@link #resultsDir} is also used as {@link #classProbFileDir}
	 */
	public WekaLauncher(String featureFile, String resultsDir, String modelFileDir, boolean computeClassProbabilities) {
		this(featureFile, resultsDir, modelFileDir);
		if (computeClassProbabilities) {
			classProbFileDir = resultsDir;
		}
	}

	/**
	 * 
	 * @param featureFile
	 * @param resultsDir
	 * @param modelFileDir
	 */
	public WekaLauncher(String featureFile, String resultsDir, String modelFileDir) {
		this(featureFile, resultsDir);
		this.modelFileDir = modelFileDir;
	}

	/**
	 * 
	 * @param featureFile
	 * @param resultsDir
	 */
	public WekaLauncher(String featureFile, String resultsDir) {
		this(featureFile);
		this.resultsDir = resultsDir;
	}

	/**
	 * 
	 * @param featureFile
	 */
	public WekaLauncher(String featureFile) {
		libsvmFeatureFile = featureFile;
	}

	private static final String mergedResultsFileName = "evaluationResults.txt";

	static boolean silent = true;
	static PrintStream defaultOutstream = System.out;

	private String libsvmFeatureFile;
	private String resultsDir = null;
	private String modelFileDir = null;
	private String classProbFileDir = null;
	private String cvSplitFile = null;

	private boolean nestedCV = true;
	private int multiruns = 1;
	private int folds = 4;


	public void setNestedCV(boolean performNestedCV) {
		nestedCV = performNestedCV;
	}

	public void setMultiruns(int multiruns) {
		this.multiruns = multiruns;
	}

	public void setFolds(int folds) {
		this.folds = folds;
	}


	/**
	 * 
	 * @param classifierName
	 * @param multithreading
	 * @return
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

	/**
	 * 
	 * @return
	 */
	private int[][][] getCVSplit() {
		return getCVSplit(libsvmFeatureFile, folds, multiruns);
	}

	/**
	 * 
	 * @param libsvmFeatureFile
	 * @param folds
	 * @param multiruns
	 * @return
	 */
	public static int[][][] getCVSplit(String libsvmFeatureFile, int folds, int multiruns) {

		// get unscaled libsvm feature vectors for each run and split
		String[][][] splittedDatasets = new String[multiruns][][];
		for (int run = 0; run < multiruns; run++) {
			splittedDatasets[run] = WekaClassifier.getSplittedDataset(libsvmFeatureFile, folds, run);
		}

		// read original feature file
		List<String> featureVectors = BasicTools.readFile2List(libsvmFeatureFile, false);
		for (int i = 0; i < featureVectors.size(); i++) {
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
		return(cvSplit);
	}

	private void saveWekaCVsplit() {

		int[][][] cvSplit = getCVSplit();

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
		BasicTools.writeList2File(cvSplitList, cvSplitFile);
	}

	public double[][] runWekaClassifier(boolean multithreading) {

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
			while (!exec.isTerminated()) {
				;
			}


			// run classifiers in single thread
		} else {
			// TODO: For test exchange next line and for loop
			//ClassificationMethod classMethod = ClassificationMethod.SVM_ecoc;
			for (ClassificationMethod classMethod : ClassificationMethod.values()) {
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
		// TODO: For test exchange next line and for loop
		// ClassificationMethod classMethod = ClassificationMethod.SVM_ecoc;
		for (ClassificationMethod classMethod: ClassificationMethod.values()) {
			String classResultFile = resultsDir + classMethod.modelFileName.replace(".model", ".eval");
			mergedClassResults = mergedClassResults.concat(BasicTools.readFile2String(classResultFile));
		}
		BasicTools.writeString2File(mergedClassResults, resultsDir + mergedResultsFileName);
	}

	private double[][] parseResultsFile() {

		int numScores = folds * multiruns;
		// TODO: For test exchange next line and for loop
		// int numMethods = 1;
		int numMethods = ClassificationMethod.values().length;
		double[][] classResults = new double[numScores][numMethods];
		List<String[]> resultsTable = BasicTools.readFile2ListSplitLines(resultsDir + mergedResultsFileName);
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
			classifierType = argsClassifier;

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

		String feature_file = "test/libsvm_featurefile_small.txt";
		String results_dir = "test/class_dir/";
		String model_dir = "test/model_dir/";

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


