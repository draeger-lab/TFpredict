package io;
/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import weka.core.*;
import weka.classifiers.*;
import weka.classifiers.evaluation.*;
import weka.gui.visualize.*;

/**
  * Generates and displays a ROC curve from a dataset.
  */
public class GenerateROC {
  
  public static void plot(Evaluation eval, String outfile) {

    // generate curve
    ThresholdCurve tc = new ThresholdCurve();
    int classIndex = 0;
    Instances result = tc.getCurve(eval.predictions(), classIndex);
    
    String rocr = "";
    
    for (Instance curr : result) {
    	rocr = rocr.concat(curr.value(4)+"\t"+curr.value(5)+"\n");
    	//System.out.println(curr.value(4)+"\t"+curr.value(5));
    }

	BufferedWriter bw;
	try {
		bw = new BufferedWriter(new FileWriter(new File(outfile)));
		bw.write(rocr);
		bw.flush();
		bw.close();
	} catch (IOException e) {
		e.printStackTrace();
	}

    /*
    // plot curve
    ThresholdVisualizePanel vmc = new ThresholdVisualizePanel();
    vmc.setROCString("(Area under ROC = " + Utils.doubleToString(ThresholdCurve.getROCArea(result), 4) + ")");
    vmc.setName(result.relationName());
    PlotData2D tempd = new PlotData2D(result);
    tempd.setPlotName(result.relationName());
    tempd.addInstanceNumberAttribute();
    // specify which points are connected
    boolean[] cp = new boolean[result.numInstances()];
    for (int n = 1; n < cp.length; n++)
      cp[n] = true;
    tempd.setConnectPoints(cp);
    // add plot
    vmc.addPlot(tempd);

    // display curve
    String plotName = vmc.getName(); 
    final javax.swing.JFrame jf = 
      new javax.swing.JFrame(plotName);
    jf.setSize(1000,800);
    jf.getContentPane().setLayout(new BorderLayout());
    jf.getContentPane().add(vmc, BorderLayout.CENTER);
    jf.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
      jf.dispose();
      }
    });
    jf.setVisible(true);
  */
  }

  
}

