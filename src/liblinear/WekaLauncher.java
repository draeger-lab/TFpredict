/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

package liblinear;

import io.BasicTools;
import io.FileParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;

import liblinear.WekaClassifier.ClassificationMethod;


public class WekaLauncher {
	
	public WekaLauncher(String featureFile, String resultsDir, String modelFileDir, String classProbFileDir) {
		this(featureFile, resultsDir, modelFileDir);
		this.classProbFileDir = classProbFileDir;
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
	
	private String[] getClassifierArguments(String classifierName) {
		
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
			argsClassifier.add(resultsDir+classifierName);
		}
		
		return argsClassifier.toArray(new String[]{});
	}
	
	public double[][] runWekaClassifier(Boolean multi) {
		
		resultsDir += "/evaluationResults";
		// redirect standard output of classifier to file (if desired)
		if (resultsDir != null) {
			File classResultsFile = new File(resultsDir); 
			if (classResultsFile.exists()) {
				classResultsFile.delete();
			}
			for (ClassificationMethod classMethod: ClassificationMethod.values()) {
				classResultsFile = new File(resultsDir+classMethod.name()); 
				if (new File(resultsDir+classMethod.name()).exists()) {
					classResultsFile.delete();
				}	
			}
		}		
		
		if (multi) {
			Collection<Job> queue = new ArrayList<Job>();
			
			for (ClassificationMethod classMethod: ClassificationMethod.values()) {
				String[] argsClassifier = getClassifierArguments(classMethod.name());
				queue.add(new Job(argsClassifier));
			}
			
			ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
			try {
				exec.invokeAll(queue);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			exec.shutdown();
			while (!exec.isTerminated());
		}
		else {
			// run all Weka classifiers on given feature file
			for (ClassificationMethod classMethod: ClassificationMethod.values()) {
				String[] argsClassifier = getClassifierArguments(classMethod.name());
				try {
					WekaClassifier.main(argsClassifier);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		mergeResultsFiles();

		// reset output stream
		double[][] classResults = null;
		if (resultsDir != null) {
			classResults = parseResultsFile();
		} 
		
		return(classResults);
	}
	
	
	private void mergeResultsFiles() {
		String merged = "";
		for (ClassificationMethod classMethod: ClassificationMethod.values()) {
			merged = merged.concat(FileParser.read2String(resultsDir+classMethod.name()));
			}
		try {
			FileUtils.writeStringToFile(new File(resultsDir), merged);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	
	class Job implements Callable<String>{
		
		String[] argsClassifier;
		
		public Job(String[] argsClassifier) {
			this.argsClassifier = argsClassifier;

		}

		@Override
		public String call() throws Exception {
			try {
				WekaClassifier.main(argsClassifier);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "Done.";
		}
	}

	
	private double[][] parseResultsFile() {
		
		int numScores = folds * multiruns;
		int numMethods = ClassificationMethod.values().length;
		double[][] classResults = new double[numScores][numMethods];
		ArrayList<String[]> resultsTable = BasicTools.readFile2ListSplitLines(resultsDir);
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
