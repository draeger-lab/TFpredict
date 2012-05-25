package liblinear;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.math.stat.descriptive.moment.Mean;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.GaussianProcesses;
import weka.classifiers.functions.LibLINEAR;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.converters.LibSVMLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.NonSparseToSparse;
import weka.filters.unsupervised.instance.Normalize;

public class WekaClassifier {

	public boolean silent = true;
	
	private static String summaryFile = null;
	private static String classProbabilityFile = null;
	private boolean hideLibsvmDebugOutput = true;
	private PrintStream stdOut = System.out;
	
	private boolean showProgress = true;
	private boolean showEstimatedDuration = true;
	private boolean performNestedCV = true;
	private String mainPerformanceMeasure = "ROC";
	private String modelFile = null;
	private boolean writeModelFile = false;
	
	private static int folds = 2;
	private static int repetitions = 1;
	private final static int innerRepetitions = 1;
	private static DecimalFormat df = new DecimalFormat();
	private String selectedClassifier;
	private static String strFile;

	
	public enum ClassificationMethod {
		RandomForest("Random Forest", "randomForest.model"),
		DecisionTree("Decision Tree", "decisionTree.model"),
		SVM_rbf("SVM rbf kernel", "svmRBF.model"),
		SVM_linear("SVM linear kernel", "svmLinear.model"),
		NaiveBayes("Naive Bayes", "naiveBayes.model"),
		Kstar("K*", "kStar.model"),
		KNN("kNN", "knn.model");
		
		public String printName;
		public String modelFileName;
		
		private ClassificationMethod(String printName, String modelFileName) {
			this.printName = printName;
			this.modelFileName = modelFileName;
		}
	}
	
	
	public enum RegressionMethod {
		GaussianProcesses("Gaussian Processes", "gaussianProcesses.model");
	
		public String printName;
		public String modelFileName;
		
		private RegressionMethod(String printName, String modelFileName) {
			this.printName = printName;
			this.modelFileName = modelFileName;
		}
	}
	
	
	/**
	 * @param args
	 * @throws IOException
	 * @throws Exception
	 */
	public static void main(String[] args) throws IOException {
		java.util.Locale.setDefault(java.util.Locale.ENGLISH);
		df = new DecimalFormat();
		final WekaClassifier classRunner = new WekaClassifier();
		Options options = classRunner.buildCommandLine();
		classRunner.parseCommandLine(args, options);
		if (!classRunner.silent) classRunner.printConfiguration();
		
		// to support libsvm-format
		Instances samples = classRunner.readData();
		classRunner.normalizeData(samples);

		// no model selection possible for Kstar and Naive Bayes
		if (ClassificationMethod.valueOf(classRunner.selectedClassifier).equals(ClassificationMethod.Kstar.name()) ||
			ClassificationMethod.valueOf(classRunner.selectedClassifier).equals(ClassificationMethod.NaiveBayes.name())) {
				classRunner.performNestedCV = false;
		}
		
		// show progress
		if (classRunner.showProgress) {
			System.out.println("\nClassifier: " + ClassificationMethod.valueOf(classRunner.selectedClassifier).printName);
			System.out.println("  Multiruns: " + repetitions + "  Folds: " + folds + "  Model selection: " + classRunner.performNestedCV + "\n");
			
		}
		
		// create a new classifier
		WekaClassifierResult[] classResult = null;
		if (classRunner.selectedClassifier == ClassificationMethod.RandomForest.name()) {
			classResult = classRunner.runNestedCVRandomForest(samples, repetitions);
			
		} else if (classRunner.selectedClassifier == ClassificationMethod.DecisionTree.name()) {
			classResult = classRunner.runNestedCVJ48(samples, repetitions);
		
		} else if (classRunner.selectedClassifier == ClassificationMethod.SVM_rbf.name()) {
			classResult = classRunner.runNestedCVLIBSVM(samples, repetitions);
		
		} else if (classRunner.selectedClassifier == ClassificationMethod.SVM_linear.name()) {
			classResult = classRunner.runNestedCVLIBLINEAR(samples, repetitions);
		
		} else if (classRunner.selectedClassifier == ClassificationMethod.NaiveBayes.name()) {
			classResult = classRunner.runNestedCVNaiveBayes(samples, repetitions);

		} else if (classRunner.selectedClassifier == ClassificationMethod.Kstar.name()) {
			classResult = classRunner.runNestedCVKStar(samples, repetitions);
		
		} else if (classRunner.selectedClassifier == ClassificationMethod.KNN.name()) {
			classResult = classRunner.runNestedCVkNN(samples, repetitions);
		
		} else if (classRunner.selectedClassifier == RegressionMethod.GaussianProcesses.name()) {
			classResult = classRunner.runNestedCVGaussianProcesses(samples, repetitions);
		
		} else {
			System.out.println("Please select a valid classifier.");
			System.exit(1);
		}
		String classifierName = ClassificationMethod.valueOf(classRunner.selectedClassifier).printName;
		if (summaryFile == null) {
			classRunner.printSummary2Console(classResult, classifierName, samples);
		} else {
			classRunner.printSummary2File(classResult, classifierName, samples, new File(summaryFile));
		}
		if (classProbabilityFile != null) {
			classRunner.writeClassProbabilityFile(classResult, classifierName, samples);
		}
	}

	private void printConfiguration() {
		System.out.println("repetitions: " + repetitions + ", inner repetitions: " + innerRepetitions + ", classifier: " + selectedClassifier + ", InFile: " + strFile + ", folds: " + folds);
	}

	/**
	 * read file
	 * @author Florian Topf
	 * @return
	 */
	private Instances readData() {
		
		if (!strFile.contains("libsvm")) {
			return readDataFromARFF();
		}
		else {
			return readDataFromLibsvm();
		}
	}
	
	/**
	 * read ARFF file
	 * 
	 * @return
	 */
	private Instances readDataFromARFF() {
		BufferedReader dataReader = null;
		try {
			dataReader = new BufferedReader(new FileReader(new File(strFile)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Instances trainInsts = null;
		try {
			trainInsts = new Instances(dataReader);
		} catch (IOException e) {
			e.printStackTrace();
		}
		trainInsts.setClassIndex(trainInsts.numAttributes() - 1);
		trainInsts = arffToSparse(trainInsts);
		trainInsts.randomize(new Random(1));
		trainInsts.stratify(folds);
		return trainInsts;
	}
	
	/**
	 * read LibSVM file
	 * @author Florian Topf
	 * 
	 * @return
	 */
	private Instances readDataFromLibsvm() {
		Instances trainInsts = null;
		try {
			LibSVMLoader lsl = new LibSVMLoader();
			lsl.setSource(new File(strFile));
			trainInsts = lsl.getDataSet();
		} catch (IOException e) {
			e.printStackTrace();
		}
		trainInsts.setClassIndex(trainInsts.numAttributes() - 1);
		
		// convert class label only
		NumericToNominal ntm = new NumericToNominal(); 
		Instances Insts_filtered = null;
		String[] options = new String[2];
		options[0] = "-R";
		options[1] = "last";
		try {
			ntm.setOptions(options);
			ntm.setInputFormat(trainInsts); 
			Insts_filtered = Filter.useFilter(trainInsts, ntm);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Insts_filtered.randomize(new Random(1));
		Insts_filtered.stratify(folds);
		return Insts_filtered;
	}

	/**
	 * normalizes the attribute value in 0,1
	 * 
	 * @param trainInsts
	 */
	private void normalizeData(Instances trainInsts) {
		// normalize input data
		if (!silent) System.out.println("Normalizing data ...");
		Normalize normalize = new Normalize();
		try {
			normalize.setInputFormat(trainInsts);
			for (int i = 0, n = trainInsts.numInstances(); i < n; i++) {
				normalize.input(trainInsts.get(i));
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	@SuppressWarnings("static-access")
	private Options buildCommandLine() {
		final Options options = new Options();
		final Option optSDF = (OptionBuilder.isRequired(true).withDescription("Classifier").hasArg(true).create("c"));
		final Option optFile = (OptionBuilder.isRequired(true).withDescription("InFile").hasArg(true).create("f"));
		final Option optRepetitions = (OptionBuilder.isRequired(false).withDescription("Repetitions").hasArg(true).create("r"));
		final Option optFolds = (OptionBuilder.isRequired(false).withDescription("Folds (default = 2)").hasArg(true).create("v"));
		final Option optModelFile = (OptionBuilder.isRequired(false).withDescription("Model file)").hasArg(true).create("m"));
		final Option optNestedCV = (OptionBuilder.isRequired(false).withDescription("Nested cross-validation (default = true)").hasArg(true).create("n"));
		final Option optSummaryFile = (OptionBuilder.isRequired(false).withDescription("Results file").hasArg(true).create("s"));
		final Option optClassProbFile = (OptionBuilder.isRequired(false).withDescription("Class probability file").hasArg(true).create("p"));
		options.addOption(optSDF);
		options.addOption(optFile);
		options.addOption(optFolds);
		options.addOption(optRepetitions);
		options.addOption(optModelFile);
		options.addOption(optNestedCV);
		options.addOption(optSummaryFile);
		options.addOption(optClassProbFile);
		return options;
	}

	private void parseCommandLine(String[] args, Options options) {
		CommandLine lvCmd = null;
		final HelpFormatter lvFormater = new HelpFormatter();
		final CommandLineParser lvParser = new BasicParser();
		/**
		 * parse command line
		 */
		try {
			lvCmd = lvParser.parse(options, args);
			if (lvCmd.hasOption('h')) {
				lvFormater.printHelp("java -jar jCMapper.jar", options);
				System.exit(1);
			}
		} catch (final ParseException pvException) {
			lvFormater.printHelp("jCMapper", options);
			System.out.println("Parse error: " + pvException.getMessage());
			System.exit(1);
		}

		try {
			if (lvCmd.hasOption("c")) {
				try {
					selectedClassifier = lvCmd.getOptionValue("c");
				} catch (Exception e) {
					System.exit(1);
				}
			}
			if (lvCmd.hasOption("f")) {
				try {
					strFile = new String(lvCmd.getOptionValue("f"));
				} catch (Exception e) {
					System.exit(1);
				}
			}
			if (lvCmd.hasOption("r")) {
				try {
					repetitions = new Integer(lvCmd.getOptionValue("r"));
				} catch (Exception e) {
					System.exit(1);
				}
			}
			if (lvCmd.hasOption("v")) {
				try {
					folds = new Integer(lvCmd.getOptionValue("v"));
				} catch (Exception e) {
					System.exit(1);
				}
			}
			if (lvCmd.hasOption("m")) {
				try {
					modelFile = new String(lvCmd.getOptionValue("m"));
					writeModelFile = true;
				} catch (Exception e) {
					System.exit(1);
				}
			}
			if (lvCmd.hasOption("n")) {
				try {
					performNestedCV = new Boolean(lvCmd.getOptionValue("n"));
				} catch (Exception e) {
					System.exit(1);
				}
			}
			if (lvCmd.hasOption("s")) {
				try {
					summaryFile = new String(lvCmd.getOptionValue("s"));
				} catch (Exception e) {
					System.exit(1);
				}
			}
			if (lvCmd.hasOption("p")) {
				try {
					classProbabilityFile = new String(lvCmd.getOptionValue("p"));
				} catch (Exception e) {
					System.exit(1);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.err.println("Please check your input.");
			System.exit(1);
		}
	}
	
	private static void write2FileOrConsole (String line, BufferedWriter bw) {
		if (bw == null) {
			System.out.println(line);
		} else {
			try {
				bw.write(line + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void printSummary2File(WekaClassifierResult[] classResult, String classifier, Instances data, File summaryFile) {
		
		try {
			BufferedWriter summaryWriter = new BufferedWriter(new FileWriter(summaryFile, true));
			printSummary(classResult, classifier, data, summaryWriter);
			summaryWriter.flush();
			summaryWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void printSummary2Console(WekaClassifierResult[] classResult, String classifier, Instances data) {
		printSummary(classResult, classifier, data, null);
	}

	private void printSummary(WekaClassifierResult[] classResult, String classifier, Instances data, BufferedWriter summaryWriter) {
		write2FileOrConsole("#" + classifier, summaryWriter);
		write2FileOrConsole("#AvgAreaUnderROC" + "\tAvgFMeasure" + "\tAvgMatthewsCorrelation" + "\tAvgAccuracy" + "\tAvgBalancedAccuracy", summaryWriter);

		for (int i=0; i<classResult.length; i++) {
			Evaluation eval = classResult[i].evaluation;
			write2FileOrConsole(df.format(getAvgROCAUC(eval, data)) + "\t" + df.format(getAvgFMeasure(eval, data)) + "\t" + df.format(getMatthewsCorrelation(eval, data)) + "\t" + df.format(getAvgAccuracy(eval, data)) + "\t" + df.format(getBalancedAccuracy(eval, data)), summaryWriter);
		}
		write2FileOrConsole("", summaryWriter);
	}
	
	private void writeClassProbabilityFile(WekaClassifierResult[] classResult, String classifier, Instances data) {
		
		// write only the class probabilities from first multirun
		int numClasses = classResult[0].classProbabilities[0].length;
		int numSamples = data.numInstances();
		int multirunCounter = 1;
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(classProbabilityFile)));
			bw.write("#" + classifier + "\n");
			
			for (int i=0; i<classResult.length; i++) {
				
				if (i % numSamples == 0) {
					bw.write("# Results for Multirun: " + multirunCounter++ + "\n");
					bw.write("# Class Probabilities\tLabels\n");
				}
				
				int trainSetSize = classResult[i].classLabels.length;
				for (int j=0; j<trainSetSize; j++) {
					bw.write("(" + df.format(classResult[i].classProbabilities[j][0]));
					for (int k=1; k<numClasses; k++) {
						bw.write(", " + df.format(classResult[i].classProbabilities[j][k]));
					}
					bw.write(")\t" + classResult[i].classLabels[j] + "\n");
				}
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * computes the mean balanced accuracy
	 * 
	 * @param eval
	 * @param data
	 * @return
	 */
	private double getBalancedAccuracy(Evaluation eval, Instances data) {
		int classes = data.numClasses();
		double bAUC = 0;
		for (int i = 0; i < classes; i++) {
			bAUC = bAUC + (eval.precision(i) + eval.precision(i)) / 2;
		}
		return bAUC / (double) classes;
	}

	/**
	 * computes the mean balanced accuracy
	 * 
	 * @param eval
	 * @param data
	 * @return
	 */
	private double getAvgROCAUC(Evaluation eval, Instances data) {
		int classes = data.numClasses();
		double auc = 0;
		for (int i = 0; i < classes; i++) {
			auc = auc + eval.areaUnderROC(i);
		}
		return auc / (double) classes;
	}

	/**
	 * computes the mean FMeasure accuracy
	 * 
	 * @param eval
	 * @param data
	 * @return
	 */
	private double getAvgFMeasure(Evaluation eval, Instances data) {
		int classes = data.numClasses();
		double fmeasure = 0;
		for (int i = 0; i < classes; i++) {
			fmeasure = fmeasure + eval.fMeasure(i);
		}
		return fmeasure / (double) classes;
	}

	/**
	 * computes the mean balanced accuracy
	 * 
	 * @param eval
	 * @param data
	 * @return
	 */
	private double getAvgAccuracy(Evaluation eval, Instances data) {
		int classes = data.numClasses();

		double acc = 0;
		for (int i = 0; i < classes; i++) {
			double tp = eval.numTruePositives(i);
			double tn = eval.numTrueNegatives(i);
			double fp = eval.numFalsePositives(i);
			double fn = eval.numFalseNegatives(i);
			double acc_i = (tp + tn) / (tp + tn + fp + fn);

			if (Double.isNaN(acc_i))
				acc_i = 0;

			acc = acc + acc_i;
		}
		return acc / (double) classes;
	}

	/**
	 * computes the mean balanced accuracy
	 * 
	 * @param eval
	 * @param data
	 * @return
	 */
	private double getMatthewsCorrelation(Evaluation eval, Instances data) {
		int classes = data.numClasses();

		double mcc = 0;
		for (int i = 0; i < classes; i++) {
			double tp = eval.numTruePositives(i);
			double tn = eval.numTrueNegatives(i);
			double fp = eval.numFalsePositives(i);
			double fn = eval.numFalseNegatives(i);
			double mcc_i = (tp * tn - fp * fn) / Math.sqrt((tp + fp) * (tp + fn) * (tn + fp) * (tn + fn));

			if (Double.isNaN(mcc_i))
				mcc_i = 0;

			mcc = mcc + mcc_i;
		}
		return mcc / (double) classes;
	}


	private void writeModelFile(Classifier model) {
		if (modelFile != null) {
			try {
			weka.core.SerializationHelper.write(modelFile, model);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * @param randomForest
	 * @param data
	 */
	private WekaClassifierResult[] runNestedCVRandomForest(Instances data, int repetitions) {
		final RandomForest randomForest = new RandomForest();
		final int maxFeatures = ((int) Math.sqrt(data.numAttributes()));
		randomForest.setNumTrees(10);
		randomForest.setNumFeatures(maxFeatures);
		randomForest.setSeed(1);

		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];
		int run = 0;

		if (! silent) System.out.println("Nested CV for Random forests ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}
				
				if (performNestedCV) {
					if (! silent) System.out.println("Model selection ...");
					//cross-validate on n-1 folds
					int bestN = 4;
					double bestAUC = 0.0;
					for (int numFeat = bestN; numFeat <= maxFeatures * 2; numFeat
							= numFeat * 2) {
						randomForest.setNumFeatures(numFeat);
						Evaluation[] evaluation =
								performCrossvalidation(randomForest, instancesTraining,
										folds, innerRepetitions);
						double auc = getMeanQuality(evaluation, instancesTraining);
						if (auc > bestAUC) {
							if (!silent) System.out.println("score=" + df.format(auc) + ", @numFeatures=" + numFeat);
							bestAUC = auc;
							bestN = numFeat;
						}
					}
					//predict external data
					randomForest.setNumFeatures(bestN);
				}
				
				buildClassifier(randomForest, instancesTraining);
				classResults[run] = predictAndEvaluate(randomForest, splits[splitIndex]);
				
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}
				
				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
			if (writeModelFile) {
				buildClassifier(randomForest, data);
				writeModelFile(randomForest);
			}
		}
		return classResults;
	}

	/**
	 * 
	 * @param randomForest
	 * @param data
	 */
	private WekaClassifierResult[] runNestedCVJ48(Instances data, int repetitions) {
		final J48 decisionTree = new J48();
		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];

		int run = 0;

		if (!silent) System.out.println("Nested CV for J48 tree ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}
				
				if (performNestedCV) {
					if (!silent) System.out.println("Model selection ...");
					// cross-validate on n-1 folds
					double bestPruningConfidence = 0.05;
					int bestNumFeatures = 2;
					double bestAUC = 0.0;
					for (double pruningC = 0.05; pruningC <= 0.5; pruningC = pruningC * 2) {
						for (int numFeat = 2; numFeat <= 10; numFeat = numFeat * 2) {
							decisionTree.setConfidenceFactor((float) pruningC);
							decisionTree.setMinNumObj(numFeat);
							Evaluation[] evaluation = performCrossvalidation(decisionTree, instancesTraining, folds, innerRepetitions);
							double auc = getMeanQuality(evaluation, instancesTraining);
							if (auc > bestAUC) {
								if (!silent) System.out.println("score=" + df.format(auc) + " @pruningC=" + pruningC + ", @numObj=" + numFeat);
								bestPruningConfidence = pruningC;
								bestAUC = auc;
								bestNumFeatures = numFeat;
							}
						}
					}
	
					// predict external data
					decisionTree.setConfidenceFactor((float) bestPruningConfidence);
					decisionTree.setMinNumObj(bestNumFeatures);
				}
				
				buildClassifier(decisionTree, instancesTraining);

				classResults[run] = predictAndEvaluate(decisionTree, splits[splitIndex]);
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				// System.out.println(externalEvals[run].toSummaryString());
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}

				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
		}
		if (writeModelFile) {
			buildClassifier(decisionTree, data);
			writeModelFile(decisionTree);
		}
		return classResults;
	}

	/**
	 * 
	 * @param randomForest
	 * @param data
	 */
	private WekaClassifierResult[] runNestedCVLIBSVM(Instances data, int repetitions) {
		final LibSVM libsvm = new LibSVM();
		
		// enable conversion of decision values to probability estimates
		libsvm.setProbabilityEstimates(true);
		
		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];

		int run = 0;

		if (!silent) System.out.println("Nested CV for LIBSVM ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}
				
				if (performNestedCV) {
					
					if (!silent) System.out.println("Model selection ...");
					// cross-validate on n-1 folds
					double bestGamma = 0.005;
					double bestC = 1;
					// double bestWeight = 1.0;
					double bestAUC = 0.0;
					for (double gamma = 0.005; gamma <= 0.1; gamma = gamma * 2) {
						for (double C = Math.pow(2, -5); C <= Math.pow(2, 4); C = C * 2) {
							// for (double weight = 1.0; weight <= 4.0; weight =
							// weight * 2) {
	
							libsvm.setGamma(gamma);
							libsvm.setCost(C);
							// libsvm.setWeights((1.0 / weight) + " " + 1.0);
							
							if (hideLibsvmDebugOutput) {
								String debugOutputFile = WekaLauncher.redirectSystemOut2TempFile();
								if (!silent) System.out.println("Redirecting LibSVM debug output to temporary file: " + debugOutputFile);
							}
							Evaluation[] evaluation = performCrossvalidation(libsvm, instancesTraining, folds, innerRepetitions);
							if (hideLibsvmDebugOutput) {
								System.setOut(stdOut);
							}
							double auc = getMeanQuality(evaluation, instancesTraining);
							if (auc > bestAUC) {
								if (!silent) System.out.println("score=" + df.format(auc) + " @C=" + C + ", @gamma=" + gamma);
								bestC = C;
								bestGamma = gamma;
								bestAUC = auc;
								// bestWeight = weight;
							}
							// }
						}
					}
	
					// predict external data
					libsvm.setGamma(bestGamma);
					libsvm.setCost(bestC);
					// libsvm.setWeights((1.0 / bestWeight) + " " + 1.0);
				}
				
				if (hideLibsvmDebugOutput) {
					String debugOutputFile = WekaLauncher.redirectSystemOut2TempFile();
					if (!silent) System.out.println("Redirecting LibSVM debug output to temporary file: " + debugOutputFile);
				}
				buildClassifier(libsvm, instancesTraining);
				if (hideLibsvmDebugOutput) {
					System.setOut(stdOut);
				}
				
				classResults[run] = predictAndEvaluate(libsvm, splits[splitIndex]);
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}
				
				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
		}
		if (writeModelFile) {
			if (hideLibsvmDebugOutput) {
				String debugOutputFile = WekaLauncher.redirectSystemOut2TempFile();
				if (!silent) System.out.println("Redirecting LibSVM debug output to temporary file: " + debugOutputFile);
			}
			buildClassifier(libsvm, data);
			if (hideLibsvmDebugOutput) {
				System.setOut(stdOut);
			}
			writeModelFile(libsvm);
		}
		return classResults;
	}

	/**
	 * 
	 * @param randomForest
	 * @param data
	 */
	private WekaClassifierResult[] runNestedCVLIBLINEAR(Instances data, int repetitions) {
		final LibLINEAR libsvm = new LibLINEAR();
		
		// enable conversion of decision values to probability estimates (requires L2-regularized logistic regression SVM)
		libsvm.setProbabilityEstimates(true);
		libsvm.setSVMType(new SelectedTag(LibLINEAR.SVMTYPE_L2_LR, LibLINEAR.TAGS_SVMTYPE));
		
		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];

		int run = 0;

		if (!silent) System.out.println("Nested CV for LIBLINEAR ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}
				
				if (performNestedCV) {
					if (!silent) System.out.println("Model selection ...");
					// cross-validate on n-1 folds
					double bestC = 1;
					double bestAUC = 0.0;
					double bestWeight = 1.0;
	
					for (double C = Math.pow(2, -5); C <= 2; C = C * 2) {
						for (double weight = 1.0; weight <= 8.0; weight = weight * 2) {
							libsvm.setCost(C);
							libsvm.setWeights((1.0 / weight) + " " + 1.0);
							
							Evaluation[] evaluation = performCrossvalidation(libsvm, instancesTraining, folds, innerRepetitions);
							
							double auc = getMeanQuality(evaluation, instancesTraining);
							if (auc > bestAUC) {
								if (!silent) System.out.println("score=" + df.format(auc) + " @C=" + C + " @weight=" + weight);
								bestAUC = auc;
								bestWeight = weight;
								bestC = C;
							}
						}
					}

					// predict external data
					libsvm.setCost(bestC);
					libsvm.setWeights((1.0 / bestWeight) + " " + 1.0);
				}
				
				instancesTraining.sort(instancesTraining.get(0).numAttributes() - 1);
				buildClassifier(libsvm, instancesTraining);

				classResults[run]  = predictAndEvaluate(libsvm, splits[splitIndex]);
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				// System.out.println(externalEvals[run].toSummaryString());
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}

				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
		}
		if (writeModelFile) {
			buildClassifier(libsvm, data);
			writeModelFile(libsvm);
		}
		return classResults;
	}

	/**
	 * tunes GaussianProcesses
	 * 
	 * @param data
	 * @param repetitions
	 * @return
	 */
	private WekaClassifierResult[] runNestedCVGaussianProcesses(Instances data, int repetitions) {
		final GaussianProcesses gaussianProcesses = new GaussianProcesses();
		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];

		int run = 0;

		if (!silent) System.out.println("Nested CV for Gaussian Processes ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}

				if (performNestedCV) {
					if (!silent) System.out.println("Model selection ...");
					// cross-validate on n-1 folds
					double bestNoise = -12;
					double bestMSE = 0.0;
	
					for (double C = -12; C <= -1; C++) {
						gaussianProcesses.setNoise(Math.pow(2, C));
						Evaluation[] evaluation = performCrossvalidation(gaussianProcesses, instancesTraining, folds, innerRepetitions);
						double mse = getMSEforRegression(evaluation, instancesTraining);
						if (mse > bestMSE) {
							if (!silent) System.out.println("score=" + df.format(mse) + " @noise=" + Math.pow(2, C));
							bestMSE = mse;
							bestNoise = C;
						}
					}
	
					// predict external data
					gaussianProcesses.setNoise(Math.pow(2, bestNoise));
				}
				instancesTraining.sort(instancesTraining.get(0).numAttributes() - 1);
				buildClassifier(gaussianProcesses, instancesTraining);

				classResults[run] = predictAndEvaluate(gaussianProcesses, splits[splitIndex]);
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				// System.out.println(externalEvals[run].toSummaryString());
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}

				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
		}
		if (writeModelFile) {
			buildClassifier(gaussianProcesses, data);
			writeModelFile(gaussianProcesses);
		}
		return classResults;
	}

	/**
	 * 
	 * @param randomForest
	 * @param data
	 */
	private WekaClassifierResult[] runNestedCVkNN(Instances data, int repetitions) {
		final IBk kNN = new IBk();
		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];

		int run = 0;

		if (!silent) System.out.println("Nested CV for kNN ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}
				
				if (performNestedCV) {
					if (!silent) System.out.println("Model selection ...");
					// cross-validate on n-1 folds
					int bestNeigbor = 1;
					double bestAUC = 0.0;
					for (int neigbor = 1; neigbor <= 16; neigbor = neigbor * 2) {
						kNN.setKNN(neigbor);
						Evaluation[] evaluation = performCrossvalidation(kNN, instancesTraining, folds, innerRepetitions);
						double auc = getMeanQuality(evaluation, instancesTraining);
						if (auc > bestAUC) {
							if (!silent) System.out.println("score=" + df.format(auc) + " @k=" + neigbor);
							bestAUC = auc;
							bestNeigbor = neigbor;
						}
					}
	
					// predict external data
					kNN.setKNN(bestNeigbor);
				}
				buildClassifier(kNN, instancesTraining);

				classResults[run] = predictAndEvaluate(kNN, splits[splitIndex]);
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				// System.out.println(externalEvals[run].toSummaryString());
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}
				
				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
		}
		if (writeModelFile) {
			buildClassifier(kNN, data);
			writeModelFile(kNN);
		}
		return classResults;
	}

	/**
	 * 
	 * @param randomForest
	 * @param data
	 */
	private WekaClassifierResult[] runNestedCVNaiveBayes(Instances data, int repetitions) {
		final NaiveBayes naiveBayes = new NaiveBayes();
		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];

		int run = 0;

		if (!silent) System.out.println("Nested CV for Naive Bayes ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}

				// predict external data
				buildClassifier(naiveBayes, instancesTraining);
				classResults[run] = predictAndEvaluate(naiveBayes, splits[splitIndex]);
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				// System.out.println(externalEvals[run].toSummaryString());
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}
				
				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
		}
		if (writeModelFile) {
			buildClassifier(naiveBayes, data);
			writeModelFile(naiveBayes);
		}
		return classResults;
	}

	/**
	 * 
	 * @param randomForest
	 * @param data
	 */
	private WekaClassifierResult[] runNestedCVKStar(Instances data, int repetitions) {
		final KStar pls = new KStar();
		final long systemMillisBegin = System.currentTimeMillis();
		WekaClassifierResult[] classResults = new WekaClassifierResult[folds * repetitions];

		int run = 0;

		if (!silent) System.out.println("Nested CV for K Star ...");

		for (int rep = 0; rep < repetitions; rep++) {

			// generate new splits for cross-validation
			Instances[] splits = getSplits(data, folds, rep);

			for (int splitIndex = 0; splitIndex < splits.length; splitIndex++) {
				Instances instancesTraining = new Instances(data, 1);
				for (int trainingSplitIndex = 0; trainingSplitIndex < splits.length; trainingSplitIndex++) {
					if (trainingSplitIndex != splitIndex) {
						instancesTraining = addInstances(instancesTraining, splits[trainingSplitIndex]);
					}
				}

				// predict external data
				buildClassifier(pls, instancesTraining);
				classResults[run] = predictAndEvaluate(pls, splits[splitIndex]);
				if (!silent) System.out.println("\nExternal prediction at fold = " + splitIndex);
				try {
					if (!silent) System.out.println(classResults[run].evaluation.toClassDetailsString());
				} catch (Exception e) {
					if (!silent) System.out.println("could not create summary");
				}
				
				if (showEstimatedDuration) {
					double percentDone = ((double) (run + 1)) / ((double) (repetitions * folds));
					double estimatedTime = getEstimatedTimeInseconds(systemMillisBegin, System.currentTimeMillis(), percentDone);
					System.out.println("  time remaining: " + df.format(estimatedTime) + "s");
				}
				run++;
			}
		}
		if (writeModelFile) {
			buildClassifier(pls, data);
			writeModelFile(pls);
		}
		return classResults;
	}

	/**
	 * returns the estimated time
	 * 
	 * @param systemMillisBegin
	 * @param systemMillisCurrent
	 * @param percentDone
	 *            in 0,1
	 * @return
	 */
	private double getEstimatedTimeInseconds(long systemMillisBegin, long systemMillisCurrent, double percentDone) {
		long diff = systemMillisCurrent - systemMillisBegin;
		double SecondsElapsed = (diff / 1000.0);
		double estimatedTime = (((1.0 / percentDone) * SecondsElapsed) - SecondsElapsed);
		return estimatedTime;
	}

	/**
	 * 
	 * @param eval
	 * @return
	 */
	private double getMeanQuality(Evaluation[] eval, Instances data) {
		Mean mean = new Mean();
		double[] aucs = new double[eval.length];
		for (int i = 0; i < eval.length; i++) {
			if (mainPerformanceMeasure.equals("ROC")) {
				aucs[i] = getAvgROCAUC(eval[i], data);
			} else if (mainPerformanceMeasure.equals("MCC")){
				aucs[i] = getMatthewsCorrelation(eval[i], data);
			} else {
				System.out.println("Error. Unknown performance measure. Only \"ROC\" or \"MCC\" can be used as main performance measure.");
				System.exit(1);
			}
		}
		return mean.evaluate(aucs);
	}

	/**
	 * 
	 * @param eval
	 * @return
	 */
	private double getMSEforRegression(Evaluation[] eval, Instances data) {
		Mean mean = new Mean();
		double[] aucs = new double[eval.length];
		for (int i = 0; i < eval.length; i++) {
			try {
				aucs[i] = eval[i].correlationCoefficient();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mean.evaluate(aucs);
	}

	/**
	 * trains a classifier on the given data
	 * 
	 * @param classifier
	 * @param trainingData
	 */
	private void buildClassifier(Classifier classifier, Instances trainingData) {
		try {
			if (classifier instanceof LibLINEARWekaAdapter)
				trainingData.sort(trainingData.numAttributes() - 1);

			classifier.buildClassifier(trainingData);
		} catch (final Exception e2) {
			e2.printStackTrace();
		}
	}

	/**
	 * read sparse
	 * 
	 * @param source
	 */
	private Instances arffToSparse(Instances source) {
		final NonSparseToSparse sp = new NonSparseToSparse();
		try {
			sp.setInputFormat(source);
			final Instances newData = Filter.useFilter(source, sp);
			return newData;
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return source;
	}

	/**
	 * returns the score for this fold
	 * 
	 * @param testSamples
	 * @return
	 */

	private WekaClassifierResult predictAndEvaluate(Classifier classifier, Instances testSamples) {
		
		double[][] classProb = getClassProbabilities(classifier, testSamples);
		int[] classLab = getClassLabels(testSamples);
		Evaluation eval = evaluate(classifier, testSamples);
		
		return new WekaClassifierResult(eval, classProb, classLab);
	}
	
	// evaluates the given model on test samples
	private Evaluation evaluate(Classifier classifier, Instances testSamples) {
		
		Evaluation eTest = null;	
		try {
			eTest = new Evaluation(testSamples);
			eTest.evaluateModel(classifier, testSamples);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return(eTest);
	}
	
	private double[][] getClassProbabilities(Classifier classifier, Instances testSamples) {
		
		double[][] classProbabilities = new double[testSamples.numInstances()][testSamples.numClasses()];
		
		// iterate over all instances in test set
		for (int i=0; i<testSamples.numInstances(); i++) {
			Instance instance = testSamples.instance(i);
			try {
				// get class distribution
				classProbabilities[i] = classifier.distributionForInstance(instance);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return classProbabilities;
	}
	
	private int[] getClassLabels(Instances testSamples) {
		
		int[] classLabels = new int[testSamples.numInstances()];
		
		// iterate over all instances in test set
		for (int i=0; i<testSamples.numInstances(); i++) {
			Instance instance = testSamples.instance(i);
			try {
				// get class label
				classLabels[i] = (int) instance.classValue();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return classLabels;
	}
	
	

	/**
	 * splits an array of instances into n folds of equal size
	 * 
	 * @param data
	 * @param folds
	 * @param seed
	 * @return
	 */
	private Instances[] getSplits(Instances data, int folds, int seed) {

		Instances[] splits = new Instances[folds];
		for (int i = 0; i < splits.length; i++) {
			// copy header information
			splits[i] = new Instances(data, 1);
		}

		// set seed according to the repetition run
		data.randomize(new Random(seed));
		data.stratify(folds);

		int foldIndex = 0;
		for (int i = 0, n = data.numInstances(); i < n; i++) {
			splits[foldIndex].add(data.instance(i));
			foldIndex++;
			if (foldIndex > folds - 1)
				foldIndex = 0;
		}
		return splits;
	}

	/**
	 * 
	 * @param classifier
	 * @param data
	 * @param folds
	 * @param repetitions
	 * @return
	 * @throws Exception
	 */
	private Evaluation[] performCrossvalidation(Classifier classifier, Instances data, int folds, int repetitions) {

		ArrayList<Evaluation> results = new ArrayList<Evaluation>();
		for (int rep = 0; rep < repetitions; rep++) {
			Instances[] splits = getSplits(data, folds, rep);

			for (int i = 0; i < folds; i++) {
				Instances trainingSetTemporary = new Instances(data, 1);
				for (int j = 0; j < folds; j++) {
					if (j != i)
						trainingSetTemporary = this.addInstances(trainingSetTemporary, splits[j]);
				}
				// train classifier
				this.buildClassifier(classifier, trainingSetTemporary);
				// predict the jth fold
				Evaluation screeningResult = this.evaluate(classifier, splits[i]);
				results.add(screeningResult);
			}
		}
		Evaluation[] resultArray = new Evaluation[results.size()];
		for (int i = 0; i < resultArray.length; i++) {
			resultArray[i] = results.get(i);
		}
		return resultArray;
	}

	/**
	 * returns join list of references of a and b
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	private Instances addInstances(Instances a, Instances b) {
		Instances instancesA = new Instances(a);
		for (int i = 0, n = b.numInstances(); i < n; i++) {
			instancesA.add(b.instance(i));
		}
		return instancesA;
	}
}

// object to save decision values, labels, and performance scores for Weka classifiers
class WekaClassifierResult {
	
	Evaluation evaluation;
	double[][] classProbabilities;
	int[] classLabels;
	
	WekaClassifierResult(Evaluation eval, double[][] classProb, int[] classLab) {
		evaluation = eval;
		classProbabilities = classProb;
		classLabels = classLab;
	}
}
