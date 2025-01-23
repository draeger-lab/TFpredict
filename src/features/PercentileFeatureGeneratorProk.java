/*
 * $Id: PercentileFeatureGeneratorProk.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/features/PercentileFeatureGeneratorProk.java $
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
package features;

import io.BasicTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zbit.util.logging.LogUtil;
import modes.Predict;


/**
 *
 * @author Johannes Eichner
 * @version $Rev: 99 $
 * @since 1.0
 */
public class PercentileFeatureGeneratorProk extends BLASTfeatureGeneratorProk {

	/**
	 * 
	 */
	private static final int[] percentiles = new int[] {0,25,50,75,100};

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(PercentileFeatureGeneratorProk.class.getName());

	/**
	 * 
	 * @param fastaFile
	 * @param featureFile
	 * @param superPred
	 */
	public PercentileFeatureGeneratorProk(String fastaFile, String featureFile, boolean superPred) {
		super(fastaFile, featureFile, superPred);
		pssmFeat = false;
		naiveFeat = false;
	}

	/**
	 * 
	 * @param seq2label
	 * @param superPred
	 */
	public PercentileFeatureGeneratorProk(Map<String, Integer> seq2label, boolean superPred) {
		super();
		this.seq2label = seq2label;
		this.superPred = superPred;
	}

	/* (non-Javadoc)
	 * @see features.BLASTfeatureGeneratorProk#computeFeaturesFromBlastResult()
	 */
	@Override
	public void computeFeaturesFromBlastResult(Map<String, Map<String, Double>> hits) {

		int numWarnings = 0;

		for (final String seqID: hits.keySet()) {
			BlastResultFeature feature = computeFeaturesFromBlastResult(seqID, hits.get(seqID));
			numWarnings += feature.getWarningCount();
			features.put(seqID, feature.getFeatures());
		}

		if (numWarnings > 0) {
			logger.fine("Number of warnings: " + numWarnings);
		}
	}

	/**
	 * 
	 * @param seqID
	 * @param currHits
	 * @return
	 */
	@Override
	protected <T extends Number> BlastResultFeature computeFeaturesFromBlastResult(String seqID, Map<String, T> currHits) {
		int numWarnings = 0;
		double[] percFeatVec = null;

		if (superPred) {
			List<Double> scoresBasicDomain = new ArrayList<Double>();
			List<Double> scoresZincFinger = new ArrayList<Double>();
			List<Double> scoresHelixTurnHelix = new ArrayList<Double>();
			List<Double> scoresBetaScaffold = new ArrayList<Double>();
			List<Double> scoresOther = new ArrayList<Double>();
			for (String hit: currHits.keySet()) {
				if (hit.equals(seqID)) {
					continue;
				}
				if (seq2label.get(hit) == Predict.Basic_domain) {
					scoresBasicDomain.add(currHits.get(hit).doubleValue());

				} else if (seq2label.get(hit) == Predict.Zinc_finger) {
					scoresZincFinger.add(currHits.get(hit).doubleValue());

				} else if (seq2label.get(hit) == Predict.Helix_turn_helix) {
					scoresHelixTurnHelix.add(currHits.get(hit).doubleValue());

				} else if (seq2label.get(hit) == Predict.Beta_scaffold) {
					scoresBetaScaffold.add(currHits.get(hit).doubleValue());

				} else if (seq2label.get(hit) == Predict.Other) {
					scoresOther.add(currHits.get(hit).doubleValue());

				} else {
					logger.severe("Error. Invalid label associated with BLAST hit \"" + hit + "\": " + seq2label.get(hit));
					System.exit(0);
				}
			}


			double[] bitScoresBasicDomain = BasicTools.Double2double(scoresBasicDomain.toArray(new Double[]{}));
			double[] bitScoresZincFinger = BasicTools.Double2double(scoresZincFinger.toArray(new Double[]{}));
			double[] bitScoresHelixTurnHelix = BasicTools.Double2double(scoresHelixTurnHelix.toArray(new Double[]{}));
			double[] bitScoresBetaScaffold = BasicTools.Double2double(scoresBetaScaffold.toArray(new Double[]{}));
			double[] bitScoresOther = BasicTools.Double2double(scoresOther.toArray(new Double[]{}));
			if (bitScoresBasicDomain.length == 0) {
				bitScoresBasicDomain = new double[] {0};
			}
			if (bitScoresZincFinger.length == 0) {
				bitScoresZincFinger = new double[] {0};
			}
			if (bitScoresHelixTurnHelix.length == 0) {
				bitScoresHelixTurnHelix = new double[] {0};
			}
			if (bitScoresBetaScaffold.length == 0) {
				bitScoresBetaScaffold = new double[] {0};
			}
			if (bitScoresOther.length == 0) {
				bitScoresOther = new double[] {0};
			}

			Double[] percVector = new Double[percentiles.length*5];

			for (int p=0; p<percentiles.length; p++) {
				percVector[p] = BasicTools.computePercentile(bitScoresBasicDomain, percentiles[p]);
				percVector[p+5] = BasicTools.computePercentile(bitScoresZincFinger, percentiles[p]);
				percVector[p+10] = BasicTools.computePercentile(bitScoresHelixTurnHelix, percentiles[p]);
				percVector[p+15] = BasicTools.computePercentile(bitScoresBetaScaffold, percentiles[p]);
				percVector[p+20] = BasicTools.computePercentile(bitScoresOther, percentiles[p]);
			}
			percFeatVec = BasicTools.Double2double(percVector);

		} else {

			List<Double> scoresTF = new ArrayList<Double>();
			List<Double> scoresNonTF = new ArrayList<Double>();
			for (String hit: currHits.keySet()) {
				if (hit.equals(seqID)) {
					continue;
				}
				Integer result = seq2label.get(hit);
				if (result != null) {
					if (result == Predict.TF) {
						scoresTF.add(currHits.get(hit).doubleValue());
					} else if (seq2label.get(hit) == Predict.Non_TF) {
						scoresNonTF.add(currHits.get(hit).doubleValue());
					} else {
						logger.severe("Error. Invalid label associated with BLAST hit \""
								+ hit + "\": " + seq2label.get(hit));
						System.exit(0);
					}
				}
			}

			if (scoresTF.isEmpty() && scoresNonTF.isEmpty()) {
				logger.fine("Warning. No BLAST hits found for sequence: " + seqID);
				numWarnings++;
				return new BlastResultFeature(seqID, percFeatVec, 0, numWarnings);
			}

			double[] bitScoresTF = BasicTools.Double2double(scoresTF.toArray(new Double[]{}));
			double[] bitScoresNonTF = BasicTools.Double2double(scoresNonTF.toArray(new Double[]{}));
			if (bitScoresTF.length == 0) {
				bitScoresTF = new double[] {0};
			}
			if (bitScoresNonTF.length == 0) {
				bitScoresNonTF = new double[] {0};
			}


			double[] percentilesTF = new double[percentiles.length];
			double[] percentilesNonTF = new double[percentiles.length];
			for (int p = 0; p < percentiles.length; p++) {
				percentilesTF[p] = BasicTools.computePercentile(bitScoresTF, percentiles[p]);
				percentilesNonTF[p] = BasicTools.computePercentile(bitScoresNonTF, percentiles[p]);
			}
			percFeatVec = BasicTools.concatenateArrays(percentilesTF, percentilesNonTF);
		}

		return new BlastResultFeature(seqID, percFeatVec, 0, numWarnings);
	}

	/**
	 * <p>
	 * Generation of feature files.
	 * <p>
	 * Please note that in order to use this class, the environment variable
	 * {@code BLAST_DIR} must be defined and point to your (local) installation
	 * folder of the BLAST program.
	 *
	 * @param args
	 *            {@code tfFastaFile} (FASTA format) and {@code tfFeatureFile}
	 *            (TXT format)
	 */
	public static void main(String[] args) {
		LogUtil.initializeLogging(Level.INFO, "data", "de", "edu", "features", "io", "ipr", "liblinear", "main", "modes", "resources");

		// generate feature file for TF prediction
		String tfFastaFile = args[0]; //"/rahome/eichner/projects/tfpredict/data/tf_pred/fasta_files/latest/TFandNonTF.fasta";
		String tfFeatureFile = args[1]; //"/rahome/eichner/projects/tfpredict/data/tf_pred/feature_files/latest/percentile_featurefile.txt";

		long time = System.currentTimeMillis();

		PercentileFeatureGeneratorProk tfFeatureGenerator = new PercentileFeatureGeneratorProk(tfFastaFile, tfFeatureFile, false);
		tfFeatureGenerator.generateFeatures();

		logger.fine("Time elapsed: " + ((System.currentTimeMillis() - time)/3600) + " minutes");
	}
}
