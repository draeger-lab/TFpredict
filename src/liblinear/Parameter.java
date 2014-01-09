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

import static liblinear.Linear.copyOf;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public final class Parameter {

    double     C;

    /** stopping criteria */
    double     eps;

    SolverType solverType;

    double[]   weight      = null;

    int[]      weightLabel = null;

    public Parameter( SolverType solverType, double C, double eps ) {
        setSolverType(solverType);
        setC(C);
        setEps(eps);
    }

    /**
     * <p>nr_weight, weight_label, and weight are used to change the penalty
     * for some classes (If the weight for a class is not changed, it is
     * set to 1). This is useful for training classifier using unbalanced
     * input data or with asymmetric misclassification cost.</p>
     *
     * <p>Each weight[i] corresponds to weight_label[i], meaning that
     * the penalty of class weight_label[i] is scaled by a factor of weight[i].</p>
     *
     * <p>If you do not want to change penalty for any of the classes,
     * just set nr_weight to 0.</p>
     */
    public void setWeights(double[] weights, int[] weightLabels) {
        if (weights == null) throw new IllegalArgumentException("'weight' must not be null");
        if (weightLabels == null || weightLabels.length != weights.length)
            throw new IllegalArgumentException("'weightLabels' must have same length as 'weight'");
        this.weightLabel = copyOf(weightLabels, weightLabels.length);
        this.weight = copyOf(weights, weights.length);
    }

    /**
     * @see #setWeights(double[], int[])
     */
    public double[] getWeights() {
        return copyOf(weight, weight.length);
    }

    /**
     * @see #setWeights(double[], int[])
     */
    public int[] getWeightLabels() {
        return copyOf(weightLabel, weightLabel.length);
    }

    /**
     * the number of weights
     * @see #setWeights(double[], int[])
     */
    public int getNumWeights() {
        if (weight == null) return 0;
        return weight.length;
    }

    /**
     * C is the cost of constraints violation. (we usually use 1 to 1000)
     */
    public void setC(double C) {
        if (C <= 0) throw new IllegalArgumentException("C must not be <= 0");
        this.C = C;
    }

    public double getC() {
        return C;
    }

    /**
     * eps is the stopping criterion. (we usually use 0.01).
     */
    public void setEps(double eps) {
        if (eps <= 0) throw new IllegalArgumentException("eps must not be <= 0");
        this.eps = eps;
    }

    public double getEps() {
        return eps;
    }

    public void setSolverType(SolverType solverType) {
        if (solverType == null) throw new IllegalArgumentException("solver type must not be null");
        this.solverType = solverType;
    }

    public SolverType getSolverType() {
        return solverType;
    }
}
