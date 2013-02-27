package liblinear;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import weka.classifiers.Evaluation;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.NonSparseToSparse;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class NaiveBayesExternalPredictionTest {

	/**
	 * @param args
	 * @throws IOException
	 * @throws Exception
	 */
	public static void main(String[] args) throws IOException {

		final NaiveBayesExternalPredictionTest naivebayes = new NaiveBayesExternalPredictionTest();

		final BufferedReader trainReader = new BufferedReader(new FileReader("./resources/train.arff"));
		final BufferedReader classifyReader = new BufferedReader(new FileReader("./resources/test.arff"));

		// final BufferedReader trainReader = new BufferedReader(new
		// FileReader("C:\\temp\\soybean.arff"));
		// final BufferedReader classifyReader = new BufferedReader(new
		// FileReader("C:\\temp\\soybean.arff"));

		Instances trainInsts = new Instances(trainReader);
		trainInsts.setClassIndex(trainInsts.numAttributes() - 1);
		trainInsts = naivebayes.arffToSparse(trainInsts);

		// TODO SparseInstance
		Instances classifyInsts = new Instances(classifyReader);
		classifyInsts.setClassIndex(classifyInsts.numAttributes() - 1);
		classifyInsts = naivebayes.arffToSparse(classifyInsts);

		double[] prob = null;
		try {
			naivebayes.trainOnData(trainInsts);

			Evaluation eTest = null;
			try {
				eTest = new Evaluation(classifyInsts);
			} catch (final Exception e1) {
				e1.printStackTrace();
			}
			try {
				eTest.crossValidateModel(cModel, classifyInsts, 10, new Random(5));
				System.out.println(eTest.toMatrixString());
				System.out.println(eTest.toSummaryString(true));
			} catch (final Exception e) {
				e.printStackTrace();
			}

			 prob = naivebayes.predictData(classifyInsts);
		} catch (final Exception e) {
			e.printStackTrace();
		}
		naivebayes.printArray(prob);
	}

	private static RandomForest cModel = new RandomForest();

	private Instances trainInsts;

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
	 * this method shows how to define and add a chemical fingerprint for WEKA
	 */
	private void createFingerprint() {

		// Declare two numeric attributes
		final Attribute Attribute1 = new Attribute("FP-1");
		final Attribute Attribute2 = new Attribute("FP-2");
		final Attribute Attribute3 = new Attribute("FP-3");

		// Declare the class attribute along with its values
		final FastVector fvClassVal = new FastVector(2);
		fvClassVal.addElement("+1");
		fvClassVal.addElement("-1");
		final Attribute ClassAttribute = new Attribute("ACTIVITY", fvClassVal);

		// Declare the feature vector
		final FastVector fvWekaAttributes = new FastVector(4);
		fvWekaAttributes.addElement(Attribute1);
		fvWekaAttributes.addElement(Attribute2);
		fvWekaAttributes.addElement(Attribute3);
		fvWekaAttributes.addElement(ClassAttribute);

		// Create an empty training set
		final Instances isTrainingSet = new Instances("Assay", fvWekaAttributes, 10);
		// Set class index
		isTrainingSet.setClassIndex(3);

		// Add a fingerprint

		// Create the instance
		final SparseInstance iExample = new SparseInstance(4);
		iExample.setValue((Attribute) fvWekaAttributes.elementAt(0), 1.0);
		//iExample.setValue((Attribute) fvWekaAttributes.elementAt(1), 0.5);
		iExample.setValue((Attribute) fvWekaAttributes.elementAt(2), 2.3);
		iExample.setValue((Attribute) fvWekaAttributes.elementAt(3), "+1");

		// add the instance
		isTrainingSet.add(iExample);
	}

	public double[] predictData(Instances classifyInsts) {
		// Test the model
		Evaluation eTest = null;
		try {
			eTest = new Evaluation(classifyInsts);
		} catch (final Exception e1) {
			e1.printStackTrace();
		}
		try {
			eTest.evaluateModel(this.cModel, classifyInsts);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final String strSummary = eTest.toSummaryString();
		System.out.println(strSummary);

		try {
			System.out.println(eTest.toMatrixString());
		} catch (final Exception e1) {
			e1.printStackTrace();
		}

		System.out.println("wROC = " + eTest.weightedAreaUnderROC());

		final double[] prob = new double[classifyInsts.numInstances()];

		for (int i = 0; i < classifyInsts.numInstances(); i++) {

			final Instance iUse = classifyInsts.instance(i);
			iUse.setDataset(this.trainInsts);

			// Get the likelihood of each classes
			// fDistribution[0] is the probability of being “positive”
			// fDistribution[1] is the probability of being “negative”
			try {
				final double[] fDistribution = this.cModel.distributionForInstance(iUse);
				System.out.println("prob(-) = " + fDistribution[0] + " prob(+) = " + fDistribution[1]
						+ " (true class of instance i = " + i + " " + iUse.value(iUse.numAttributes() - 1) + ")");
				prob[i] = fDistribution[1];
				System.out.println("Classification = " + this.cModel.classifyInstance(iUse));
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}

		return prob;
	}

	private void printArray(double[] array) {
		for (int i = 0; i < array.length; i++) {
			System.out.println(array[i]);
		}
	}

	public void trainOnData(Instances trainInsts) {

		// Create a naïve bayes classifier
		this.cModel = new RandomForest();
		try {
			this.cModel.buildClassifier(trainInsts);
		} catch (final Exception e2) {
			e2.printStackTrace();
		}
		this.trainInsts = trainInsts;

	}
}
