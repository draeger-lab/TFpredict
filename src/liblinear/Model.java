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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;


/**
 * <p>Model stores the model obtained from the training procedure</p>
 *
 * <p>use {@link Linear#loadModel(File)} and {@link Linear#saveModel(File, Model)} to load/save it</p>
 *
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public final class Model implements Serializable {

    private static final long serialVersionUID = -6456047576741854834L;

    double                    bias;

    /** label of each class */
    int[]                     label;

    int                       nr_class;

    int                       nr_feature;

    SolverType                solverType;

    /** feature weight array */
    double[]                  w;

    /**
     * @return number of classes
     */
    public int getNrClass() {
        return nr_class;
    }

    /**
     * @return number of features
     */
    public int getNrFeature() {
        return nr_feature;
    }

    public int[] getLabels() {
        return copyOf(label, nr_class);
    }

    /**
     * The nr_feature*nr_class array w gives feature weights. We use one
     * against the rest for multi-class classification, so each feature
     * index corresponds to nr_class weight values. Weights are
     * organized in the following way
     *
     * <pre>
     * +------------------+------------------+------------+
     * | nr_class weights | nr_class weights |  ...
     * | for 1st feature  | for 2nd feature  |
     * +------------------+------------------+------------+
     * </pre>
     *
     * If bias &gt;= 0, x becomes [x; bias]. The number of features is
     * increased by one, so w is a (nr_feature+1)*nr_class array. The
     * value of bias is stored in the variable bias.
     * @see #getBias()
     * @return a <b>copy of</b> the feature weight array as described
     */
    public double[] getFeatureWeights() {
        return Linear.copyOf(w, w.length);
    }

    /**
     * @see #getFeatureWeights()
     */
    public double getBias() {
        return bias;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Model");
        sb.append(" bias=").append(bias);
        sb.append(" nr_class=").append(nr_class);
        sb.append(" nr_feature=").append(nr_feature);
        sb.append(" solverType=").append(solverType);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(bias);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        result = prime * result + Arrays.hashCode(label);
        result = prime * result + nr_class;
        result = prime * result + nr_feature;
        result = prime * result + ((solverType == null) ? 0 : solverType.hashCode());
        result = prime * result + Arrays.hashCode(w);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Model other = (Model)obj;
        if (Double.doubleToLongBits(bias) != Double.doubleToLongBits(other.bias)) return false;
        if (!Arrays.equals(label, other.label)) return false;
        if (nr_class != other.nr_class) return false;
        if (nr_feature != other.nr_feature) return false;
        if (solverType == null) {
            if (other.solverType != null) return false;
        } else if (!solverType.equals(other.solverType)) return false;
        if (!equals(w, other.w)) return false;
        return true;
    }

    /**
     * don't use {@link Arrays#equals(double[], double[])} here, cause 0.0 and -0.0 should be handled the same
     *
     * @see Linear#saveModel(java.io.Writer, Model)
     */
    protected static boolean equals(double[] a, double[] a2) {
        if (a == a2) return true;
        if (a == null || a2 == null) return false;

        int length = a.length;
        if (a2.length != length) return false;

        for (int i = 0; i < length; i++)
            if (a[i] != a2[i]) return false;

        return true;
    }

    /**
     * see {@link Linear#saveModel(java.io.File, Model)}
     */
    public void save(File file) throws IOException {
        Linear.saveModel(file, this);
    }

    /**
     * see {@link Linear#saveModel(Writer, Model)}
     */
    public void save(Writer writer) throws IOException {
        Linear.saveModel(writer, this);
    }

    /**
     * see {@link Linear#loadModel(File)}
     */
    public static Model load(File file) throws IOException {
        return Linear.loadModel(file);
    }

    /**
     * see {@link Linear#loadModel(Reader)}
     */
    public static Model load(Reader inputReader) throws IOException {
        return Linear.loadModel(inputReader);
    }
}
