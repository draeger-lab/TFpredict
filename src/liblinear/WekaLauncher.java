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
	private static final String singleResultsFileSuffix = "_results.txt"; 
	
	static boolean silent = false;
	static PrintStream defaultOutstream = System.out;
	
	private String libsvmFeatureFile;
    private String resultsDir = null;
    private String modelFileDir = null;
    private String classProbFileDir = null;

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
			argsClassifier.add(resultsDir + classifierName + singleResultsFileSuffix);
		}
		if (multithreading) {
			argsClassifier.add("-t");
			argsClassifier.add("true");
		}
		
		return argsClassifier.toArray(new String[]{});
	}
	
	
	public double[][] runWekaClassifier(Boolean multithreading) {
		
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
			String classResultFile = resultsDir + classMethod.name() + singleResultsFileSuffix;
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
			String summaryFile = resultsDir + classifierType + singleResultsFileSuffix;
			String classProbFile = classProbFileDir + ClassificationMethod.valueOf(classifierType).modelFileName.replace(".model", ".prob");

			WekaClassifier classifier = new WekaClassifier(classifierType, libsvmFeatureFile, multiruns, folds, nestedCV, modelFile, summaryFile, classProbFile);
			classifier.setMultithreading(true);
			classifier.run();

			return "Done.";
		}
	}
	
	/*
	public static void main(String[] args) {
		
		WekaLauncher launcher;
		if (args.length == 1) {
			launcher = new WekaLauncher(args[0]);
		} else {
			launcher = new WekaLauncher(args[0], args[1]);
		}
		
		launcher.printConfiguration();
		launcher.runWekaClassifierSingle();

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
		System.out.println("Multiruns:                " + multiruns);
		System.out.println("Folds:            	      " + folds);
		System.out.println(nestedCV ? "Nested CV:             yes" : "Nested CV:             no");
	}
	*/
}


