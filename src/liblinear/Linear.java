/*  
 * $Id$
 * $URL$
 * This file is part of the program TFpredict. TFpredict performs the
 * identification and structural characterization of transcription factors.
 *  
 * Copyright (C) 2010-2013 Center for Bioinformatics Tuebingen (ZBIT),
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;


/**
 * <h2>Java port of <a href="http://www.csie.ntu.edu.tw/~cjlin/liblinear/">liblinear</a> 1.51</h2>
 *
 * <p>The usage should be pretty similar to the C version of <tt>liblinear</tt>.</p>
 * <p>Please consider reading the <tt>README</tt> file of <tt>liblinear</tt>.</p>
 *
 * @author Benedikt Waldvogel
 * @version $Rev$
 * @since 1.0
 */
public class Linear {

    static final Charset       FILE_CHARSET        = Charset.forName("ISO-8859-1");

    static final Locale        DEFAULT_LOCALE      = Locale.ENGLISH;

    private static Object      OUTPUT_MUTEX        = new Object();
    private static PrintStream DEBUG_OUTPUT        = System.out;

    /** platform-independent new-line string */
    final static String        NL                  = System.getProperty("line.separator");

    private static final long  DEFAULT_RANDOM_SEED = 0L;
    static Random              random              = new Random(DEFAULT_RANDOM_SEED);

    /**
     * @param target predicted classes
     */
    public static void crossValidation(Problem prob, Parameter param, int nr_fold, int[] target) {
        int i;
        int[] fold_start = new int[nr_fold + 1];
        int l = prob.l;
        int[] perm = new int[l];

        for (i = 0; i < l; i++)
            perm[i] = i;
        for (i = 0; i < l; i++) {
            int j = i + random.nextInt(l - i);
            swap(perm, i, j);
        }
        for (i = 0; i <= nr_fold; i++)
            fold_start[i] = i * l / nr_fold;

        for (i = 0; i < nr_fold; i++) {
            int begin = fold_start[i];
            int end = fold_start[i + 1];
            int j, k;
            Problem subprob = new Problem();

            subprob.bias = prob.bias;
            subprob.n = prob.n;
            subprob.l = l - (end - begin);
            subprob.x = new FeatureNode[subprob.l][];
            subprob.y = new int[subprob.l];

            k = 0;
            for (j = 0; j < begin; j++) {
                subprob.x[k] = prob.x[perm[j]];
                subprob.y[k] = prob.y[perm[j]];
                ++k;
            }
            for (j = end; j < l; j++) {
                subprob.x[k] = prob.x[perm[j]];
                subprob.y[k] = prob.y[perm[j]];
                ++k;
            }
            Model submodel = train(subprob, param);
            for (j = begin; j < end; j++)
                target[perm[j]] = predict(submodel, prob.x[perm[j]]);
        }
    }

    /** used as complex return type */
    private static class GroupClassesReturn {

        final int[] count;
        final int[] label;
        final int   nr_class;
        final int[] start;

        GroupClassesReturn( int nr_class, int[] label, int[] start, int[] count ) {
            this.nr_class = nr_class;
            this.label = label;
            this.start = start;
            this.count = count;
        }
    }

    private static GroupClassesReturn groupClasses(Problem prob, int[] perm) {
        int l = prob.l;
        int max_nr_class = 16;
        int nr_class = 0;

        int[] label = new int[max_nr_class];
        int[] count = new int[max_nr_class];
        int[] data_label = new int[l];
        int i;

        for (i = 0; i < l; i++) {
            int this_label = prob.y[i];
            int j;
            for (j = 0; j < nr_class; j++) {
                if (this_label == label[j]) {
                    ++count[j];
                    break;
                }
            }
            data_label[i] = j;
            if (j == nr_class) {
                if (nr_class == max_nr_class) {
                    max_nr_class *= 2;
                    label = copyOf(label, max_nr_class);
                    count = copyOf(count, max_nr_class);
                }
                label[nr_class] = this_label;
                count[nr_class] = 1;
                ++nr_class;
            }
        }

        int[] start = new int[nr_class];
        start[0] = 0;
        for (i = 1; i < nr_class; i++)
            start[i] = start[i - 1] + count[i - 1];
        for (i = 0; i < l; i++) {
            perm[start[data_label[i]]] = i;
            ++start[data_label[i]];
        }
        start[0] = 0;
        for (i = 1; i < nr_class; i++)
            start[i] = start[i - 1] + count[i - 1];

        return new GroupClassesReturn(nr_class, label, start, count);
    }

    static void info(String message) {
        synchronized (OUTPUT_MUTEX) {
            if (DEBUG_OUTPUT == null) return;
            DEBUG_OUTPUT.print(message);
            DEBUG_OUTPUT.flush();
        }
    }

    static void info(String format, Object... args) {
        synchronized (OUTPUT_MUTEX) {
            if (DEBUG_OUTPUT == null) return;
            DEBUG_OUTPUT.printf(format, args);
            DEBUG_OUTPUT.flush();
        }
    }

    /**
     * @param s the string to parse for the double value
     * @throws IllegalArgumentException if s is empty or represents NaN or Infinity
     * @throws NumberFormatException see {@link Double#parseDouble(String)}
     */
    static double atof(String s) {
        if (s == null || s.length() < 1) throw new IllegalArgumentException("Can't convert empty string to integer");
        double d = Double.parseDouble(s);
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            throw new IllegalArgumentException("NaN or Infinity in input: " + s);
        }
        return (d);
    }

    /**
     * @param s the string to parse for the integer value
     * @throws IllegalArgumentException if s is empty
     * @throws NumberFormatException see {@link Integer#parseInt(String)}
     */
    static int atoi(String s) throws NumberFormatException {
        if (s == null || s.length() < 1) throw new IllegalArgumentException("Can't convert empty string to integer");
        // Integer.parseInt doesn't accept '+' prefixed strings
        if (s.charAt(0) == '+') s = s.substring(1);
        return Integer.parseInt(s);
    }

    /**
     * Java5 'backport' of Arrays.copyOf
     */
    public static double[] copyOf(double[] original, int newLength) {
        double[] copy = new double[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Java5 'backport' of Arrays.copyOf
     */
    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }

    /**
     * Loads the model from inputReader.
     * It uses {@link Locale.ENGLISH} for number formatting.
     *
     * <p><b>Note: The inputReader is closed after reading or in case of an exception.</b></p>
     */
    public static Model loadModel(Reader inputReader) throws IOException {
        Model model = new Model();

        model.label = null;

        Pattern whitespace = Pattern.compile("\\s+");

        BufferedReader reader = null;
        if (inputReader instanceof BufferedReader) {
            reader = (BufferedReader)inputReader;
        } else {
            reader = new BufferedReader(inputReader);
        }

        try {
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] split = whitespace.split(line);
                if (split[0].equals("solver_type")) {
                    SolverType solver = SolverType.valueOf(split[1]);
                    if (solver == null) {
                        throw new RuntimeException("unknown solver type");
                    }
                    model.solverType = solver;
                } else if (split[0].equals("nr_class")) {
                    model.nr_class = atoi(split[1]);
                    Integer.parseInt(split[1]);
                } else if (split[0].equals("nr_feature")) {
                    model.nr_feature = atoi(split[1]);
                } else if (split[0].equals("bias")) {
                    model.bias = atof(split[1]);
                } else if (split[0].equals("w")) {
                    break;
                } else if (split[0].equals("label")) {
                    model.label = new int[model.nr_class];
                    for (int i = 0; i < model.nr_class; i++) {
                        model.label[i] = atoi(split[i + 1]);
                    }
                } else {
                    throw new RuntimeException("unknown text in model file: [" + line + "]");
                }
            }

            int w_size = model.nr_feature;
            if (model.bias >= 0) w_size++;

            int nr_w = model.nr_class;
            if (model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS) nr_w = 1;

            model.w = new double[w_size * nr_w];
            int[] buffer = new int[128];

            for (int i = 0; i < w_size; i++) {
                for (int j = 0; j < nr_w; j++) {
                    int b = 0;
                    while (true) {
                        int ch = reader.read();
                        if (ch == -1) {
                            throw new EOFException("unexpected EOF");
                        }
                        if (ch == ' ') {
                            model.w[i * nr_w + j] = atof(new String(buffer, 0, b));
                            break;
                        } else {
                            buffer[b++] = ch;
                        }
                    }
                }
            }
        }
        finally {
            closeQuietly(reader);
        }

        return model;
    }

    /**
     * Loads the model from the file with ISO-8859-1 charset.
     * It uses {@link Locale.ENGLISH} for number formatting.
     */
    public static Model loadModel(File modelFile) throws IOException {
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(new FileInputStream(modelFile), FILE_CHARSET));
        return loadModel(inputReader);
    }

    static void closeQuietly(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {}
    }

    public static int predict(Model model, FeatureNode[] x) {
        double[] dec_values = new double[model.nr_class];
        return predictValues(model, x, dec_values);
    }
    
    public static double[] predictDV(Model model, FeatureNode[] x) {
        double[] dec_values = new double[model.nr_class];
        return predictValuesDV(model, x, dec_values);
    }

    public static int predictProbability(Model model, FeatureNode[] x, double[] prob_estimates) {
        if (model.solverType == SolverType.L2R_LR) {
            int nr_class = model.nr_class;
            int nr_w;
            if (nr_class == 2)
                nr_w = 1;
            else
                nr_w = nr_class;

            int label = predictValues(model, x, prob_estimates);
            for (int i = 0; i < nr_w; i++)
                prob_estimates[i] = 1 / (1 + Math.exp(-prob_estimates[i]));

            if (nr_class == 2) // for binary classification
                prob_estimates[1] = 1. - prob_estimates[0];
            else {
                double sum = 0;
                for (int i = 0; i < nr_class; i++)
                    sum += prob_estimates[i];

                for (int i = 0; i < nr_class; i++)
                    prob_estimates[i] = prob_estimates[i] / sum;
            }

            return label;
        } else
            return 0;
    }

    public static int predictValues(Model model, FeatureNode[] x, double[] dec_values) {
        int n;
        if (model.bias >= 0)
            n = model.nr_feature + 1;
        else
            n = model.nr_feature;

        double[] w = model.w;

        int nr_w;
        if (model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS)
            nr_w = 1;
        else
            nr_w = model.nr_class;

        for (int i = 0; i < nr_w; i++)
            dec_values[i] = 0;

        for (FeatureNode lx : x) {
            int idx = lx.index;
            // the dimension of testing data may exceed that of training
            if (idx <= n) {
                for (int i = 0; i < nr_w; i++) {
                    dec_values[i] += w[(idx - 1) * nr_w + i] * lx.value;
                }
            }
        }

        if (model.nr_class == 2)
            return (dec_values[0] > 0) ? model.label[0] : model.label[1];
        else {
            int dec_max_idx = 0;
            for (int i = 1; i < model.nr_class; i++) {
                if (dec_values[i] > dec_values[dec_max_idx]) dec_max_idx = i;
            }
            return model.label[dec_max_idx];
        }
    }

    public static double[] predictValuesDV(Model model, FeatureNode[] x, double[] dec_values) {
        int n;
        if (model.bias >= 0)
            n = model.nr_feature + 1;
        else
            n = model.nr_feature;

        double[] w = model.w;

        int nr_w;
        if (model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS)
            nr_w = 1;
        else
            nr_w = model.nr_class;

        for (int i = 0; i < nr_w; i++)
            dec_values[i] = 0;

        for (FeatureNode lx : x) {
            int idx = lx.index;
            // the dimension of testing data may exceed that of training
            if (idx <= n) {
                for (int i = 0; i < nr_w; i++) {
                    dec_values[i] += w[(idx - 1) * nr_w + i] * lx.value;
                }
            }
        }

        if (model.nr_class == 2)
            return dec_values;
        else {
            int dec_max_idx = 0;
            for (int i = 1; i < model.nr_class; i++) {
                if (dec_values[i] > dec_values[dec_max_idx]) dec_max_idx = i;
            }
            return dec_values;
        }
    }


    static void printf(Formatter formatter, String format, Object... args) throws IOException {
        formatter.format(format, args);
        IOException ioException = formatter.ioException();
        if (ioException != null) throw ioException;
    }

    /**
     * Writes the model to the modelOutput.
     * It uses {@link Locale.ENGLISH} for number formatting.
     *
     * <p><b>Note: The modelOutput is closed after reading or in case of an exception.</b></p>
     */
    public static void saveModel(Writer modelOutput, Model model) throws IOException {
        int nr_feature = model.nr_feature;
        int w_size = nr_feature;
        if (model.bias >= 0) w_size++;

        int nr_w = model.nr_class;
        if (model.nr_class == 2 && model.solverType != SolverType.MCSVM_CS) nr_w = 1;

        Formatter formatter = new Formatter(modelOutput, DEFAULT_LOCALE);
        try {
            printf(formatter, "solver_type %s\n", model.solverType.name());
            printf(formatter, "nr_class %d\n", model.nr_class);

            printf(formatter, "label");
            for (int i = 0; i < model.nr_class; i++) {
                printf(formatter, " %d", model.label[i]);
            }
            printf(formatter, "\n");

            printf(formatter, "nr_feature %d\n", nr_feature);
            printf(formatter, "bias %.16g\n", model.bias);

            printf(formatter, "w\n");
            for (int i = 0; i < w_size; i++) {
                for (int j = 0; j < nr_w; j++) {
                    double value = model.w[i * nr_w + j];

                    /** this optimization is the reason for {@link Model#equals(double[], double[])} */
                    if (value == 0.0) {
                        printf(formatter, "%d ", 0);
                    } else {
                        printf(formatter, "%.16g ", value);
                    }
                }
                printf(formatter, "\n");
            }

            formatter.flush();
            IOException ioException = formatter.ioException();
            if (ioException != null) throw ioException;
        }
        finally {
            formatter.close();
        }
    }

    /**
     * Writes the model to the file with ISO-8859-1 charset.
     * It uses {@link Locale.ENGLISH} for number formatting.
     */
    public static void saveModel(File modelFile, Model model) throws IOException {
        BufferedWriter modelOutput = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(modelFile), FILE_CHARSET));
        saveModel(modelOutput, model);
    }

    /*
     * this method corresponds to the following define in the C version:
     * #define GETI(i) (y[i]+1)
     */
    private static int GETI(byte[] y, int i) {
        return y[i] + 1;
    }

    /**
     * A coordinate descent algorithm for
     * L1-loss and L2-loss SVM dual problems
     *<pre>
     *  min_\alpha  0.5(\alpha^T (Q + D)\alpha) - e^T \alpha,
     *    s.t.      0 <= alpha_i <= upper_bound_i,
     *
     *  where Qij = yi yj xi^T xj and
     *  D is a diagonal matrix
     *
     * In L1-SVM case:
     *     upper_bound_i = Cp if y_i = 1
     *      upper_bound_i = Cn if y_i = -1
     *      D_ii = 0
     * In L2-SVM case:
     *      upper_bound_i = INF
     *      D_ii = 1/(2*Cp) if y_i = 1
     *      D_ii = 1/(2*Cn) if y_i = -1
     *
     * Given:
     * x, y, Cp, Cn
     * eps is the stopping tolerance
     *
     * solution will be put in w
     *</pre>
     */
    private static void solve_l2r_l1l2_svc(Problem prob, double[] w, double eps, double Cp, double Cn, SolverType solver_type) {
        int l = prob.l;
        int w_size = prob.n;
        int i, s, iter = 0;
        double C, d, G;
        double[] QD = new double[l];
        int max_iter = 1000;
        int[] index = new int[l];
        double[] alpha = new double[l];
        byte[] y = new byte[l];
        int active_size = l;

        // PG: projected gradient, for shrinking and stopping
        double PG;
        double PGmax_old = Double.POSITIVE_INFINITY;
        double PGmin_old = Double.NEGATIVE_INFINITY;
        double PGmax_new, PGmin_new;

        // default solver_type: L2R_L2LOSS_SVC_DUAL
        double diag[] = new double[] {0.5 / Cn, 0, 0.5 / Cp};
        double upper_bound[] = new double[] {Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY};
        if (solver_type == SolverType.L2R_L1LOSS_SVC_DUAL) {
            diag[0] = 0;
            diag[2] = 0;
            upper_bound[0] = Cn;
            upper_bound[2] = Cp;
        }

        for (i = 0; i < w_size; i++)
            w[i] = 0;
        for (i = 0; i < l; i++) {
            alpha[i] = 0;
            if (prob.y[i] > 0) {
                y[i] = +1;
            } else {
                y[i] = -1;
            }
            QD[i] = diag[GETI(y, i)];

            for (FeatureNode xi : prob.x[i]) {
                QD[i] += xi.value * xi.value;
            }
            index[i] = i;
        }

        while (iter < max_iter) {
            PGmax_new = Double.NEGATIVE_INFINITY;
            PGmin_new = Double.POSITIVE_INFINITY;

            for (i = 0; i < active_size; i++) {
                int j = i + random.nextInt(active_size - i);
                swap(index, i, j);
            }

            for (s = 0; s < active_size; s++) {
                i = index[s];
                G = 0;
                byte yi = y[i];

                for (FeatureNode xi : prob.x[i]) {
                    G += w[xi.index - 1] * xi.value;
                }
                G = G * yi - 1;

                C = upper_bound[GETI(y, i)];
                G += alpha[i] * diag[GETI(y, i)];

                PG = 0;
                if (alpha[i] == 0) {
                    if (G > PGmax_old) {
                        active_size--;
                        swap(index, s, active_size);
                        s--;
                        continue;
                    } else if (G < 0) {
                        PG = G;
                    }
                } else if (alpha[i] == C) {
                    if (G < PGmin_old) {
                        active_size--;
                        swap(index, s, active_size);
                        s--;
                        continue;
                    } else if (G > 0) {
                        PG = G;
                    }
                } else {
                    PG = G;
                }

                PGmax_new = Math.max(PGmax_new, PG);
                PGmin_new = Math.min(PGmin_new, PG);

                if (Math.abs(PG) > 1.0e-12) {
                    double alpha_old = alpha[i];
                    alpha[i] = Math.min(Math.max(alpha[i] - G / QD[i], 0.0), C);
                    d = (alpha[i] - alpha_old) * yi;

                    for (FeatureNode xi : prob.x[i]) {
                        w[xi.index - 1] += d * xi.value;
                    }
                }
            }

            iter++;
            if (iter % 10 == 0) info(".");

            if (PGmax_new - PGmin_new <= eps) {
                if (active_size == l)
                    break;
                else {
                    active_size = l;
                    info("*");
                    PGmax_old = Double.POSITIVE_INFINITY;
                    PGmin_old = Double.NEGATIVE_INFINITY;
                    continue;
                }
            }
            PGmax_old = PGmax_new;
            PGmin_old = PGmin_new;
            if (PGmax_old <= 0) PGmax_old = Double.POSITIVE_INFINITY;
            if (PGmin_old >= 0) PGmin_old = Double.NEGATIVE_INFINITY;
        }

        info(NL + "optimization finished, #iter = %d" + NL, iter);
        if (iter >= max_iter) info("\nWARNING: reaching max number of iterations\nUsing -s 2 may be faster (also see FAQ)\n\n");

        // calculate objective value

        double v = 0;
        int nSV = 0;
        for (i = 0; i < w_size; i++)
            v += w[i] * w[i];
        for (i = 0; i < l; i++) {
            v += alpha[i] * (alpha[i] * diag[GETI(y, i)] - 2);
            if (alpha[i] > 0) ++nSV;
        }
        info("Objective value = %f" + NL, v / 2);
        info("nSV = %d" + NL, nSV);
    }

    /**
     * A coordinate descent algorithm for
     * L1-regularized L2-loss support vector classification
     *
     *<pre>
     *  min_w \sum |wj| + C \sum max(0, 1-yi w^T xi)^2,
     *
     * Given:
     * x, y, Cp, Cn
     * eps is the stopping tolerance
     *
     * solution will be put in w
     *</pre>
     */
    private static void solve_l1r_l2_svc(Problem prob_col, double[] w, double eps, double Cp, double Cn) {
        int l = prob_col.l;
        int w_size = prob_col.n;
        int j, s, iter = 0;
        int max_iter = 1000;
        int active_size = w_size;
        int max_num_linesearch = 20;

        double sigma = 0.01;
        double d, G_loss, G, H;
        double Gmax_old = Double.POSITIVE_INFINITY;
        double Gmax_new;
        double Gmax_init = 0; // eclipse moans this variable might not be initialized
        double d_old, d_diff;
        double loss_old = 0; // eclipse moans this variable might not be initialized
        double loss_new;
        double appxcond, cond;

        int[] index = new int[w_size];
        byte[] y = new byte[l];
        double[] b = new double[l]; // b = 1-ywTx
        double[] xj_sq = new double[w_size];

        double[] C = new double[] {Cn, 0, Cp};

        for (j = 0; j < l; j++) {
            b[j] = 1;
            if (prob_col.y[j] > 0)
                y[j] = 1;
            else
                y[j] = -1;
        }
        for (j = 0; j < w_size; j++) {
            w[j] = 0;
            index[j] = j;
            xj_sq[j] = 0;
            for (FeatureNode xi : prob_col.x[j]) {
                int ind = xi.index - 1;
                double val = xi.value;
                xi.value *= y[ind]; // x->value stores yi*xij
                xj_sq[j] += C[GETI(y, ind)] * val * val;
            }
        }

        while (iter < max_iter) {
            Gmax_new = 0;

            for (j = 0; j < active_size; j++) {
                int i = j + random.nextInt(active_size - j);
                swap(index, i, j);
            }

            for (s = 0; s < active_size; s++) {
                j = index[s];
                G_loss = 0;
                H = 0;

                for (FeatureNode xi : prob_col.x[j]) {
                    int ind = xi.index - 1;
                    if (b[ind] > 0) {
                        double val = xi.value;
                        double tmp = C[GETI(y, ind)] * val;
                        G_loss -= tmp * b[ind];
                        H += tmp * val;
                    }
                }
                G_loss *= 2;

                G = G_loss;
                H *= 2;
                H = Math.max(H, 1e-12);

                double Gp = G + 1;
                double Gn = G - 1;
                double violation = 0;
                if (w[j] == 0) {
                    if (Gp < 0)
                        violation = -Gp;
                    else if (Gn > 0)
                        violation = Gn;
                    else if (Gp > Gmax_old / l && Gn < -Gmax_old / l) {
                        active_size--;
                        swap(index, s, active_size);
                        s--;
                        continue;
                    }
                } else if (w[j] > 0)
                    violation = Math.abs(Gp);
                else
                    violation = Math.abs(Gn);

                Gmax_new = Math.max(Gmax_new, violation);

                // obtain Newton direction d
                if (Gp <= H * w[j])
                    d = -Gp / H;
                else if (Gn >= H * w[j])
                    d = -Gn / H;
                else
                    d = -w[j];

                if (Math.abs(d) < 1.0e-12) continue;

                double delta = Math.abs(w[j] + d) - Math.abs(w[j]) + G * d;
                d_old = 0;
                int num_linesearch;
                for (num_linesearch = 0; num_linesearch < max_num_linesearch; num_linesearch++) {
                    d_diff = d_old - d;
                    cond = Math.abs(w[j] + d) - Math.abs(w[j]) - sigma * delta;

                    appxcond = xj_sq[j] * d * d + G_loss * d + cond;
                    if (appxcond <= 0) {
                        for (FeatureNode x : prob_col.x[j]) {
                            b[x.index - 1] += d_diff * x.value;
                        }
                        break;
                    }

                    if (num_linesearch == 0) {
                        loss_old = 0;
                        loss_new = 0;
                        for (FeatureNode x : prob_col.x[j]) {
                            int ind = x.index - 1;
                            if (b[ind] > 0) {
                                loss_old += C[GETI(y, ind)] * b[ind] * b[ind];
                            }
                            double b_new = b[ind] + d_diff * x.value;
                            b[ind] = b_new;
                            if (b_new > 0) {
                                loss_new += C[GETI(y, ind)] * b_new * b_new;
                            }
                        }
                    } else {
                        loss_new = 0;
                        for (FeatureNode x : prob_col.x[j]) {
                            int ind = x.index - 1;
                            double b_new = b[ind] + d_diff * x.value;
                            b[ind] = b_new;
                            if (b_new > 0) {
                                loss_new += C[GETI(y, ind)] * b_new * b_new;
                            }
                        }
                    }

                    cond = cond + loss_new - loss_old;
                    if (cond <= 0)
                        break;
                    else {
                        d_old = d;
                        d *= 0.5;
                        delta *= 0.5;
                    }
                }

                w[j] += d;

                // recompute b[] if line search takes too many steps
                if (num_linesearch >= max_num_linesearch) {
                    info("#");
                    for (int i = 0; i < l; i++)
                        b[i] = 1;

                    for (int i = 0; i < w_size; i++) {
                        if (w[i] == 0) continue;
                        for (FeatureNode x : prob_col.x[i]) {
                            b[x.index - 1] -= w[i] * x.value;
                        }
                    }
                }
            }

            if (iter == 0) Gmax_init = Gmax_new;
            iter++;
            if (iter % 10 == 0) info(".");

            if (Gmax_new <= eps * Gmax_init) {
                if (active_size == w_size)
                    break;
                else {
                    active_size = w_size;
                    info("*");
                    Gmax_old = Double.POSITIVE_INFINITY;
                    continue;
                }
            }

            Gmax_old = Gmax_new;
        }

        info("\noptimization finished, #iter = %d\n", iter);
        if (iter >= max_iter) info("\nWARNING: reaching max number of iterations\n");

        // calculate objective value

        double v = 0;
        int nnz = 0;
        for (j = 0; j < w_size; j++) {
            for (FeatureNode x : prob_col.x[j]) {
                x.value *= prob_col.y[x.index - 1]; // restore x->value
            }
            if (w[j] != 0) {
                v += Math.abs(w[j]);
                nnz++;
            }
        }
        for (j = 0; j < l; j++)
            if (b[j] > 0) v += C[GETI(y, j)] * b[j] * b[j];

        info("Objective value = %f\n", v);
        info("#nonzeros/#features = %d/%d\n", nnz, w_size);
    }

    /**
     * A coordinate descent algorithm for
     * L1-regularized logistic regression problems
     *
     *<pre>
     *  min_w \sum |wj| + C \sum log(1+exp(-yi w^T xi)),
     *
     * Given:
     * x, y, Cp, Cn
     * eps is the stopping tolerance
     *
     * solution will be put in w
     *</pre>
     */
    private static void solve_l1r_lr(Problem prob_col, double[] w, double eps, double Cp, double Cn) {
        int l = prob_col.l;
        int w_size = prob_col.n;
        int j, s, iter = 0;
        int max_iter = 1000;
        int active_size = w_size;
        int max_num_linesearch = 20;

        double x_min = 0;
        double sigma = 0.01;
        double d, G, H;
        double Gmax_old = Double.POSITIVE_INFINITY;
        double Gmax_new;
        double Gmax_init = 0; // eclipse moans this variable might not be initialized
        double sum1, appxcond1;
        double sum2, appxcond2;
        double cond;

        int[] index = new int[w_size];
        byte[] y = new byte[l];
        double[] exp_wTx = new double[l];
        double[] exp_wTx_new = new double[l];
        double[] xj_max = new double[w_size];
        double[] C_sum = new double[w_size];
        double[] xjneg_sum = new double[w_size];
        double[] xjpos_sum = new double[w_size];

        double[] C = new double[] {Cn, 0, Cp};

        for (j = 0; j < l; j++) {
            exp_wTx[j] = 1;
            if (prob_col.y[j] > 0)
                y[j] = 1;
            else
                y[j] = -1;
        }
        for (j = 0; j < w_size; j++) {
            w[j] = 0;
            index[j] = j;
            xj_max[j] = 0;
            C_sum[j] = 0;
            xjneg_sum[j] = 0;
            xjpos_sum[j] = 0;
            for (FeatureNode x : prob_col.x[j]) {
                int ind = x.index - 1;
                double val = x.value;
                x_min = Math.min(x_min, val);
                xj_max[j] = Math.max(xj_max[j], val);
                C_sum[j] += C[GETI(y, ind)];
                if (y[ind] == -1)
                    xjneg_sum[j] += C[GETI(y, ind)] * val;
                else
                    xjpos_sum[j] += C[GETI(y, ind)] * val;
            }
        }

        while (iter < max_iter) {
            Gmax_new = 0;

            for (j = 0; j < active_size; j++) {
                int i = j + random.nextInt(active_size) - j;
                swap(index, i, j);
            }

            for (s = 0; s < active_size; s++) {
                j = index[s];
                sum1 = 0;
                sum2 = 0;
                H = 0;

                for (FeatureNode x : prob_col.x[j]) {
                    int ind = x.index - 1;
                    double exp_wTxind = exp_wTx[ind];
                    double tmp1 = x.value / (1 + exp_wTxind);
                    double tmp2 = C[GETI(y, ind)] * tmp1;
                    double tmp3 = tmp2 * exp_wTxind;
                    sum2 += tmp2;
                    sum1 += tmp3;
                    H += tmp1 * tmp3;
                }

                G = -sum2 + xjneg_sum[j];

                double Gp = G + 1;
                double Gn = G - 1;
                double violation = 0;
                if (w[j] == 0) {
                    if (Gp < 0)
                        violation = -Gp;
                    else if (Gn > 0)
                        violation = Gn;
                    else if (Gp > Gmax_old / l && Gn < -Gmax_old / l) {
                        active_size--;
                        swap(index, s, active_size);
                        s--;
                        continue;
                    }
                } else if (w[j] > 0)
                    violation = Math.abs(Gp);
                else
                    violation = Math.abs(Gn);

                Gmax_new = Math.max(Gmax_new, violation);

                // obtain Newton direction d
                if (Gp <= H * w[j])
                    d = -Gp / H;
                else if (Gn >= H * w[j])
                    d = -Gn / H;
                else
                    d = -w[j];

                if (Math.abs(d) < 1.0e-12) continue;

                d = Math.min(Math.max(d, -10.0), 10.0);

                double delta = Math.abs(w[j] + d) - Math.abs(w[j]) + G * d;
                int num_linesearch;
                for (num_linesearch = 0; num_linesearch < max_num_linesearch; num_linesearch++) {
                    cond = Math.abs(w[j] + d) - Math.abs(w[j]) - sigma * delta;

                    if (x_min >= 0) {
                        double tmp = Math.exp(d * xj_max[j]);
                        appxcond1 = Math.log(1 + sum1 * (tmp - 1) / xj_max[j] / C_sum[j]) * C_sum[j] + cond - d * xjpos_sum[j];
                        appxcond2 = Math.log(1 + sum2 * (1 / tmp - 1) / xj_max[j] / C_sum[j]) * C_sum[j] + cond + d * xjneg_sum[j];
                        if (Math.min(appxcond1, appxcond2) <= 0) {
                            for (FeatureNode x : prob_col.x[j]) {
                                exp_wTx[x.index - 1] *= Math.exp(d * x.value);
                            }
                            break;
                        }
                    }

                    cond += d * xjneg_sum[j];

                    int i = 0;
                    for (FeatureNode x : prob_col.x[j]) {
                        int ind = x.index - 1;
                        double exp_dx = Math.exp(d * x.value);
                        exp_wTx_new[i] = exp_wTx[ind] * exp_dx;
                        cond += C[GETI(y, ind)] * Math.log((1 + exp_wTx_new[i]) / (exp_dx + exp_wTx_new[i]));
                        i++;
                    }

                    if (cond <= 0) {
                        i = 0;
                        for (FeatureNode x : prob_col.x[j]) {
                            int ind = x.index - 1;
                            exp_wTx[ind] = exp_wTx_new[i];
                            i++;
                        }
                        break;
                    } else {
                        d *= 0.5;
                        delta *= 0.5;
                    }
                }

                w[j] += d;

                // recompute exp_wTx[] if line search takes too many steps
                if (num_linesearch >= max_num_linesearch) {
                    info("#");
                    for (int i = 0; i < l; i++)
                        exp_wTx[i] = 0;

                    for (int i = 0; i < w_size; i++) {
                        if (w[i] == 0) continue;
                        for (FeatureNode x : prob_col.x[i]) {
                            exp_wTx[x.index - 1] += w[i] * x.value;
                        }
                    }

                    for (int i = 0; i < l; i++)
                        exp_wTx[i] = Math.exp(exp_wTx[i]);
                }
            }

            if (iter == 0) Gmax_init = Gmax_new;
            iter++;
            if (iter % 10 == 0) info(".");

            if (Gmax_new <= eps * Gmax_init) {
                if (active_size == w_size)
                    break;
                else {
                    active_size = w_size;
                    info("*");
                    Gmax_old = Double.POSITIVE_INFINITY;
                    continue;
                }
            }

            Gmax_old = Gmax_new;
        }

        info("\noptimization finished, #iter = %d\n", iter);
        if (iter >= max_iter) info("\nWARNING: reaching max number of iterations\n");

        // calculate objective value

        double v = 0;
        int nnz = 0;
        for (j = 0; j < w_size; j++)
            if (w[j] != 0) {
                v += Math.abs(w[j]);
                nnz++;
            }
        for (j = 0; j < l; j++)
            if (y[j] == 1)
                v += C[GETI(y, j)] * Math.log(1 + 1 / exp_wTx[j]);
            else
                v += C[GETI(y, j)] * Math.log(1 + exp_wTx[j]);

        info("Objective value = %f\n", v);
        info("#nonzeros/#features = %d/%d\n", nnz, w_size);
    }

    // transpose matrix X from row format to column format
    static Problem transpose(Problem prob) {
        int l = prob.l;
        int n = prob.n;
        int[] col_ptr = new int[n + 1];
        Problem prob_col = new Problem();
        prob_col.l = l;
        prob_col.n = n;
        prob_col.y = new int[l];
        prob_col.x = new FeatureNode[n][];

        for (int i = 0; i < l; i++)
            prob_col.y[i] = prob.y[i];

        for (int i = 0; i < l; i++) {
            for (FeatureNode x : prob.x[i]) {
                col_ptr[x.index]++;
            }
        }

        for (int i = 0; i < n; i++) {
            prob_col.x[i] = new FeatureNode[col_ptr[i + 1]];
            col_ptr[i] = 0; // reuse the array to count the nr of elements
        }

        for (int i = 0; i < l; i++) {
            for (int j = 0; j < prob.x[i].length; j++) {
                FeatureNode x = prob.x[i][j];
                int index = x.index - 1;
                prob_col.x[index][col_ptr[index]] = new FeatureNode(i + 1, x.value);
                col_ptr[index]++;
            }
        }

        return prob_col;
    }

    static void swap(double[] array, int idxA, int idxB) {
        double temp = array[idxA];
        array[idxA] = array[idxB];
        array[idxB] = temp;
    }

    static void swap(int[] array, int idxA, int idxB) {
        int temp = array[idxA];
        array[idxA] = array[idxB];
        array[idxB] = temp;
    }

    static void swap(IntArrayPointer array, int idxA, int idxB) {
        int temp = array.get(idxA);
        array.set(idxA, array.get(idxB));
        array.set(idxB, temp);
    }

    /**
     * @throws IllegalArgumentException if the feature nodes of prob are not sorted in ascending order
     */
    public static Model train(Problem prob, Parameter param) {

        if (prob == null) throw new IllegalArgumentException("problem must not be null");
        if (param == null) throw new IllegalArgumentException("parameter must not be null");

        for (FeatureNode[] nodes : prob.x) {
            int indexBefore = 0;
            for (FeatureNode n : nodes) {
                if (n.index <= indexBefore) {
                    throw new IllegalArgumentException("feature nodes must be sorted by index in ascending order");
                }
                indexBefore = n.index;
            }
        }

        int i, j;
        int l = prob.l;
        int n = prob.n;
        int w_size = prob.n;
        Model model = new Model();

        if (prob.bias >= 0)
            model.nr_feature = n - 1;
        else
            model.nr_feature = n;
        model.solverType = param.solverType;
        model.bias = prob.bias;

        int[] perm = new int[l];
        // group training data of the same class
        GroupClassesReturn rv = groupClasses(prob, perm);
        int nr_class = rv.nr_class;
        int[] label = rv.label;
        int[] start = rv.start;
        int[] count = rv.count;

        model.nr_class = nr_class;
        model.label = new int[nr_class];
        for (i = 0; i < nr_class; i++)
            model.label[i] = label[i];

        // calculate weighted C
        double[] weighted_C = new double[nr_class];
        for (i = 0; i < nr_class; i++) {
            weighted_C[i] = param.C;
        }

        for (i = 0; i < param.getNumWeights(); i++) {
            for (j = 0; j < nr_class; j++)
                if (param.weightLabel[i] == label[j]) break;
            if (j == nr_class) throw new IllegalArgumentException("class label " + param.weightLabel[i] + " specified in weight is not found");

            weighted_C[j] *= param.weight[i];
        }

        // constructing the subproblem
        FeatureNode[][] x = new FeatureNode[l][];
        for (i = 0; i < l; i++)
            x[i] = prob.x[perm[i]];

        int k;
        Problem sub_prob = new Problem();
        sub_prob.l = l;
        sub_prob.n = n;
        sub_prob.x = new FeatureNode[sub_prob.l][];
        sub_prob.y = new int[sub_prob.l];

        for (k = 0; k < sub_prob.l; k++)
            sub_prob.x[k] = x[k];

        // multi-class svm by Crammer and Singer
        if (param.solverType == SolverType.MCSVM_CS) {
            model.w = new double[n * nr_class];
            for (i = 0; i < nr_class; i++) {
                for (j = start[i]; j < start[i] + count[i]; j++) {
                    sub_prob.y[j] = i;
                }
            }

            SolverMCSVM_CS solver = new SolverMCSVM_CS(sub_prob, nr_class, weighted_C, param.eps);
            solver.solve(model.w);
        } else {
            if (nr_class == 2) {
                model.w = new double[w_size];

                int e0 = start[0] + count[0];
                k = 0;
                for (; k < e0; k++)
                    sub_prob.y[k] = +1;
                for (; k < sub_prob.l; k++)
                    sub_prob.y[k] = -1;

                train_one(sub_prob, param, model.w, weighted_C[0], weighted_C[1]);
            } else {
                model.w = new double[w_size * nr_class];
                double[] w = new double[w_size];
                for (i = 0; i < nr_class; i++) {
                    int si = start[i];
                    int ei = si + count[i];

                    k = 0;
                    for (; k < si; k++)
                        sub_prob.y[k] = -1;
                    for (; k < ei; k++)
                        sub_prob.y[k] = +1;
                    for (; k < sub_prob.l; k++)
                        sub_prob.y[k] = -1;

                    train_one(sub_prob, param, w, weighted_C[i], param.C);

                    for (j = 0; j < n; j++)
                        model.w[j * nr_class + i] = w[j];
                }
            }

        }
        return model;
    }

    private static void train_one(Problem prob, Parameter param, double[] w, double Cp, double Cn) {
        double eps = param.eps;
        int pos = 0;
        for (int i = 0; i < prob.l; i++)
            if (prob.y[i] == +1) pos++;
        int neg = prob.l - pos;

        Function fun_obj = null;
        switch (param.solverType) {
            case L2R_LR: {
                fun_obj = new L2R_LrFunction(prob, Cp, Cn);
                Tron tron_obj = new Tron(fun_obj, eps * Math.min(pos, neg) / prob.l);
                tron_obj.tron(w);
                break;
            }
            case L2R_L2LOSS_SVC: {
                fun_obj = new L2R_L2_SvcFunction(prob, Cp, Cn);
                Tron tron_obj = new Tron(fun_obj, eps * Math.min(pos, neg) / prob.l);
                tron_obj.tron(w);
                break;
            }
            case L2R_L2LOSS_SVC_DUAL:
                solve_l2r_l1l2_svc(prob, w, eps, Cp, Cn, SolverType.L2R_L2LOSS_SVC_DUAL);
                break;
            case L2R_L1LOSS_SVC_DUAL:
                solve_l2r_l1l2_svc(prob, w, eps, Cp, Cn, SolverType.L2R_L1LOSS_SVC_DUAL);
                break;
            case L1R_L2LOSS_SVC: {
                Problem prob_col = transpose(prob);
                solve_l1r_l2_svc(prob_col, w, eps * Math.min(pos, neg) / prob.l, Cp, Cn);
                break;
            }
            case L1R_LR: {
                Problem prob_col = transpose(prob);
                solve_l1r_lr(prob_col, w, eps * Math.min(pos, neg) / prob.l, Cp, Cn);
                break;
            }
            default:
                throw new IllegalStateException("unknown solver type: " + param.solverType);
        }
    }

    public static void disableDebugOutput() {
        setDebugOutput(null);
    }

    public static void enableDebugOutput() {
        setDebugOutput(System.out);
    }

    public static void setDebugOutput(PrintStream debugOutput) {
        synchronized (OUTPUT_MUTEX) {
            DEBUG_OUTPUT = debugOutput;
        }
    }

    /**
     * resets the PRNG
     *
     * this is i.a. needed for regression testing (eg. the Weka wrapper)
     */
    public static void resetRandom() {
        random = new Random(DEFAULT_RANDOM_SEED);
    }
}
