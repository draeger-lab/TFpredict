/*
 * $Id: PSSMFeatureGenerator.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/features/PSSMFeatureGenerator.java $
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

import io.BasicTools;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev: 99 $
 * @since 1.0
 */
public class PSSMFeatureGenerator extends BLASTfeatureGenerator {

	/**
	 * 
	 * @param fastaFile
	 * @param featureFile
	 * @param superPred
	 */
	public PSSMFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		super(fastaFile, featureFile, superPred);
		pssmFeat = true;
		naiveFeat = false;
	}

	/* (non-Javadoc)
	 * @see features.BLASTfeatureGenerator#computeFeaturesFromBlastResult(java.util.Map)
	 */
	@Override
	protected void computeFeaturesFromBlastResult(Map<String, Map<String, Double>> hits) {
		for (String seqID: pssms.keySet()) {
			BlastResultFeature feature = computeFeaturesFromBlastResult(seqID, null);
			if (feature.isSetFeatures()) {
				features.put(seqID, feature.getFeatures());
			}
		}
	}

	/**
	 * 
	 * @param pssmScore
	 * @return
	 */
	private static double scalePSSMscore(int pssmScore) {
		return 1d / (1d + Math.exp(-pssmScore));
	}

	/* (non-Javadoc)
	 * @see features.BLASTfeatureGenerator#computeFeaturesFromBlastResult(java.lang.String, java.util.Map)
	 */
	@Override
	protected <T extends Number> BlastResultFeature computeFeaturesFromBlastResult(String seqID,
			Map<String, T> currHits) {
		double[] pssmFeatVec = new double[400];

		String seq = sequences.get(seqID);
		int[][] currPSSM = pssms.get(seqID);

		for (int i = 0; i<aminoAcids.length; i++) {
			int[] indices = BasicTools.getAllIndicesOf(seq, aminoAcids[i]);
			for (int j = 0; j < aminoAcids.length; j++) {
				int sum = 0;
				for (int k=0; k<indices.length; k++) {
					sum += currPSSM[k][j];
				}
				int featIdx = i * 20 + j;
				pssmFeatVec[featIdx] = scalePSSMscore(sum);
			}
		}

		return new BlastResultFeature(seqID, pssmFeatVec);
	}
}
