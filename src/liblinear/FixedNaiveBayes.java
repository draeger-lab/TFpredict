package liblinear;

import java.util.Enumeration;

import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Utils;

public class FixedNaiveBayes extends NaiveBayes {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
	if (m_UseDiscretization) {
	    m_Disc.input(instance);
	    instance = m_Disc.output();
	}
	double[] probs = new double[m_NumClasses];
	for (int j = 0; j < m_NumClasses; j++) {
	    probs[j] = m_ClassDistribution.getProbability(j);
	}
	Enumeration<Attribute> enumAtts = instance.enumerateAttributes();
	int attIndex = 0;

	while (enumAtts.hasMoreElements()) {
	    Attribute attribute = enumAtts.nextElement();

	    if (!instance.isMissing(attribute)) {
		double temp;
		for (int j = 0; j < m_NumClasses; j++) {
		    temp = Math.log10((m_Distributions[attIndex][j].getProbability(instance.value(attribute)) * m_Instances.attribute(attIndex).weight()));
		    probs[j] += temp;
		    if (Double.isNaN(probs[j])) {
			throw new Exception("NaN returned from estimator for attribute " + attribute.name() + ":\n" + m_Distributions[attIndex][j].toString());
		    }
		}
	    }
	    attIndex++;
	}

	// raise the log10 values to the power of 10
	for (int i = 0; i < probs.length; i++) {
	    probs[i] = Math.pow(10.0, probs[i]);
	    if ((probs[i] < 1e-75)) {
		probs[i] = 1e-75;
	    }
	}

	// Display probabilities
	Utils.normalize(probs);
	return probs;
    }

}
