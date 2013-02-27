package liblinear;

/**
 * origin: tron.h
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
interface Function {

    double fun(double[] w);

    void grad(double[] w, double[] g);

    void Hv(double[] s, double[] Hs);

    int get_nr_variable();
}
