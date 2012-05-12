/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2012 ZBIT, University of Tuebingen, Florian Topf and Johannes Eichner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package modes;
/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */
import io.GenerateROC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.IBk;
import weka.classifiers.lazy.KStar;
import weka.core.Instances;
import weka.core.converters.LibSVMLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.instance.NonSparseToSparse;

public class Train {

	private static DecimalFormat df = new DecimalFormat();
	private static final long randomSeed = 1;	
	
	private static String classifierType = "KNN";
	private static String trainingDataFile;
	private static String modelFile;
	private static int folds = 5;
	
	private static final int[] knnGrid = new int[] {1,2,4,8,16};

	private final static Logger logger = Logger.getLogger(Train.class.getName());
	static {
		logger.setLevel(Level.INFO);
	}
	
	
	private static void parseOptions(CommandLine cmd) {
		
		if(cmd.hasOption("trainingData")) {
			trainingDataFile = cmd.getOptionValue("trainingData");
		}
		
		if(cmd.hasOption("folds")) {
			folds = Integer.parseInt(cmd.getOptionValue("folds"));
		}
		
	    String modelFile = trainingDataFile + "." + classifierType + ".model";
	    if(cmd.hasOption("modelFile")) {
			modelFile = cmd.getOptionValue("modelFile");
		}
		
	    logger.log(Level.INFO, "Training data:   " + trainingDataFile);
	    logger.log(Level.INFO, "Number of folds: " + folds);
	    logger.log(Level.INFO, "Model file:      " + modelFile);
	}
	
	
	public static void main(CommandLine cmd) throws Exception {
		
		parseOptions(cmd);
		
		// load data
	    Instances data = readTrainingData(trainingDataFile);
	    Evaluation eval = new Evaluation(data);
	    Classifier classifier = null;
	    
	    if (classifierType.equals("KNN")) {
		    classifier = new IBk();
		    
		    // determine optimal number of neighbors k
		    int optNumNeighbors = getOptNumNeighbors((IBk) classifier, data);
			((IBk) classifier).setKNN(optNumNeighbors);
	    
	    } else if (classifierType.equals("kStar")) {
	    	classifier = new KStar();
	    
	    } else if (classifierType.equals("SVM")) {
	    	
	    } else if (classifierType.equals("randomForest")) {
	    	
	    } else {
	    	logger.log(Level.SEVERE, "Error. Unknown classifier.");
	    	System.exit(0);
	    }
	    
	    // perform cross-validation
		classifier.buildClassifier(data);
	    eval.crossValidateModel(classifier, data, folds, new Random(randomSeed));
	    GenerateROC.plot(eval, trainingDataFile + ".roc");
	    logger.log(Level.INFO, eval.toClassDetailsString());

	    // serialize model
	    logger.log(Level.INFO, "Writing model: " + modelFile);
	    weka.core.SerializationHelper.write(trainingDataFile + ".model", classifier);
	}
	
	
	private static int getOptNumNeighbors(IBk kNN, Instances data) throws Exception {
		
		logger.log(Level.INFO, "Determining best neighbor.");
		int optNumNeighbors = 1;
		double bestAUC = 0.0;
		
		for (int numNeighbors: knnGrid) {
			
			kNN.setKNN(numNeighbors);
			Evaluation eval = new Evaluation(data);
			kNN.buildClassifier(data);
			eval.crossValidateModel(kNN, data, folds, new Random(randomSeed));
		
			double auc = eval.weightedAreaUnderROC();
			if (auc > bestAUC) {
				bestAUC = auc;
				optNumNeighbors = numNeighbors;
				
				logger.log(Level.INFO, "ROC score=" + df.format(auc) + " @k=" + numNeighbors);
			}
		}
		logger.log(Level.INFO, "Best Neighbor: " + optNumNeighbors);
		return optNumNeighbors;
	}
	
	
	private static Instances readTrainingData(String trainFile) {
		
		logger.log(Level.INFO, "Reading training data: " + trainFile);
		
		if (!trainFile.contains("libsvm")) {
			return readDataFromARFF(trainFile);
		
		} else {
			return readDataFromLibsvm(trainFile);
		}
	}
	
	private static Instances readDataFromARFF(String strFile) {
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
		trainInsts.randomize(new Random(randomSeed));
		trainInsts.stratify(folds);
		return trainInsts;
	}
	
	private static Instances readDataFromLibsvm(String trainFile) {
		Instances trainInsts = null;
		try {
			LibSVMLoader lsl = new LibSVMLoader();
			lsl.setSource(new File(trainFile));
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
	
	private static Instances arffToSparse(Instances source) {
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
}
