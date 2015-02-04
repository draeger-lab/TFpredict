/*
 * $Id: NaiveFeatureGenerator.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/features/NaiveFeatureGenerator.java $
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

import java.util.Map;
import java.util.logging.Logger;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev: 99 $
 * @since 1.0
 */
public class NaiveFeatureGenerator extends BLASTfeatureGenerator {

	/**
	 * A {@link Logger} for this class.
	 */
	private static final transient Logger logger = Logger.getLogger(NaiveFeatureGenerator.class.getName());

	/**
	 * 
	 * @param fastaFile
	 * @param featureFile
	 * @param superPred
	 */
	public NaiveFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		super(fastaFile, featureFile, superPred);
		pssmFeat = false;
		naiveFeat = true;
	}

	/* (non-Javadoc)
	 * @see features.BLASTfeatureGenerator#computeFeaturesFromBlastResult()
	 */
	@Override
	protected void computeFeaturesFromBlastResult(Map<String, Map<String, Double>> hits) {

		int numErrors = 0;
		int numWarnings = 0;

		for (String seqID: hits.keySet()) {

			BlastResultFeature feature = computeFeaturesFromBlastResult(seqID, hits.get(seqID));
			if (feature.isSetFeatures()) {
				features.put(seqID, feature.getFeatures());
			}
			numErrors += feature.getErrorCount();
			numWarnings += feature.getWarningCount();
		}

		if (numErrors > 0) {
			logger.info("Number of errors: " + numErrors);
		}
		if (numWarnings > 0) {
			logger.info("Number of warnings: " + numWarnings);
		}
	}

	/* (non-Javadoc)
	 * @see features.BLASTfeatureGenerator#computeFeaturesFromBlastResult(java.lang.String, java.util.Map)
	 */
	@Override
	protected <T extends Number> BlastResultFeature computeFeaturesFromBlastResult(String seqID,
			Map<String, T> currHits) {
		// obtain class of best hit in sequence database

		// TODO: This will never find anything because the following map will always be empty!
		Map<String, Integer> shortSeqID2label = getSeq2LabelMapWithShortenedIDs();
		double[] feature = null;

		int numErrors = 0;
		int numWarnings = 0;

		double bestScore = 0;
		String bestHit = "";
		for (String hit: currHits.keySet()) {
			if (hit.equals(seqID)) {
				continue;
			}
			if (currHits.get(hit).doubleValue() > bestScore) {
				bestHit = hit;
			}
		}
		if (shortSeqID2label.containsKey(bestHit)) {
			int predClass = shortSeqID2label.get(bestHit);
			feature = new double[] {predClass};

		} else if (bestHit.isEmpty()) {
			logger.warning("No BLAST hits found for sequence: " + seqID);
			numWarnings++;

		} else {
			logger.severe("No label found for sequence: " + bestHit);
			numErrors++;
		}

		return new BlastResultFeature(seqID, feature, numErrors, numWarnings);
	}

}
