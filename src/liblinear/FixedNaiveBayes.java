/*  
 * $Id$
 * $URL$
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

import java.util.Enumeration;

import weka.classifiers.bayes.NaiveBayes;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Utils;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
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
