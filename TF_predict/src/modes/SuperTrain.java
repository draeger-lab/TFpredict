package modes;

import io.GenerateROC;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import org.apache.commons.cli.CommandLine;

import weka.classifiers.Evaluation;
import weka.classifiers.lazy.KStar;
import weka.core.Instances;
import weka.core.converters.LibSVMLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.instance.NonSparseToSparse;

public class SuperTrain {

	static int folds = 5;
	
	public static void main(CommandLine cmd) throws Exception {
		
		String trainfile = "";
		if(cmd.hasOption("i")) {
			trainfile = cmd.getOptionValue("i");
		}
		
		if(cmd.hasOption("f")) {
			folds = Integer.parseInt(cmd.getOptionValue("f"));
		}
		
		// load data
	    Instances data = readData(trainfile);
	    
	    System.out.println("Number of folds: "+folds);
	    
	    
	    //// classifier specific
	    // initialize classifier
	    final KStar pls = new KStar();
	    
	    // train classifier
		pls.buildClassifier(data);
		Evaluation eval = new Evaluation(data);
	    eval.crossValidateModel(pls, data, folds, new Random(1));
	    
	    GenerateROC.plot(eval, trainfile+".roc");

	    System.out.println(eval.toClassDetailsString());
	    ////
	    
	    // serialize model
	    System.out.println("Writing model: "+trainfile+".model");
	    weka.core.SerializationHelper.write(trainfile+".model", pls);
	}
	
	
	private static Instances readData(String strFile) {
		
		System.out.println("Reading: "+strFile);
		
		if (!strFile.contains("libsvm")) {
			return readDataFromARFF(strFile);
		}
		else {
			return readDataFromLibsvm(strFile);
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
		trainInsts.randomize(new Random(1));
		trainInsts.stratify(folds);
		return trainInsts;
	}
	
	private static Instances readDataFromLibsvm(String strFile) {
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
		//Insts_filtered.stratify(folds);
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
