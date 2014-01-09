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

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
class L2R_LrFunction implements Function {

    private final double[] C;
    private final double[] z;
    private final double[] D;
    private final Problem  prob;

    public L2R_LrFunction( Problem prob, double Cp, double Cn ) {
        int i;
        int l = prob.l;
        int[] y = prob.y;

        this.prob = prob;

        z = new double[l];
        D = new double[l];
        C = new double[l];

        for (i = 0; i < l; i++) {
            if (y[i] == 1)
                C[i] = Cp;
            else
                C[i] = Cn;
        }
    }


    private void Xv(double[] v, double[] Xv) {

        for (int i = 0; i < prob.l; i++) {
            Xv[i] = 0;
            for (FeatureNode s : prob.x[i]) {
                Xv[i] += v[s.index - 1] * s.value;
            }
        }
    }

    private void XTv(double[] v, double[] XTv) {
        int l = prob.l;
        int w_size = get_nr_variable();
        FeatureNode[][] x = prob.x;

        for (int i = 0; i < w_size; i++)
            XTv[i] = 0;

        for (int i = 0; i < l; i++) {
            for (FeatureNode s : x[i]) {
                XTv[s.index - 1] += v[i] * s.value;
            }
        }
    }


    public double fun(double[] w) {
        int i;
        double f = 0;
        int[] y = prob.y;
        int l = prob.l;
        int w_size = get_nr_variable();

        Xv(w, z);
        for (i = 0; i < l; i++) {
            double yz = y[i] * z[i];
            if (yz >= 0)
                f += C[i] * Math.log(1 + Math.exp(-yz));
            else
                f += C[i] * (-yz + Math.log(1 + Math.exp(yz)));
        }
        f = 2.0 * f;
        for (i = 0; i < w_size; i++)
            f += w[i] * w[i];
        f /= 2.0;

        return (f);
    }

    public void grad(double[] w, double[] g) {
        int i;
        int[] y = prob.y;
        int l = prob.l;
        int w_size = get_nr_variable();

        for (i = 0; i < l; i++) {
            z[i] = 1 / (1 + Math.exp(-y[i] * z[i]));
            D[i] = z[i] * (1 - z[i]);
            z[i] = C[i] * (z[i] - 1) * y[i];
        }
        XTv(z, g);

        for (i = 0; i < w_size; i++)
            g[i] = w[i] + g[i];
    }

    public void Hv(double[] s, double[] Hs) {
        int i;
        int l = prob.l;
        int w_size = get_nr_variable();
        double[] wa = new double[l];

        Xv(s, wa);
        for (i = 0; i < l; i++)
            wa[i] = C[i] * D[i] * wa[i];

        XTv(wa, Hs);
        for (i = 0; i < w_size; i++)
            Hs[i] = s[i] + Hs[i];
        // delete[] wa;
    }

    public int get_nr_variable() {
        return prob.n;
    }

}
