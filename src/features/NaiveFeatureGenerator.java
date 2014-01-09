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
package features;

import java.util.Map;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class NaiveFeatureGenerator extends BLASTfeatureGenerator {

	/**
	 * 
	 * @param fastaFile
	 * @param featureFile
	 * @param superPred
	 */
	public NaiveFeatureGenerator(String fastaFile, String featureFile, boolean superPred) {
		super(fastaFile, featureFile, superPred);
		this.pssmFeat = false;
		this.naiveFeat = true;
	}
	
	/* (non-Javadoc)
	 * @see features.BLASTfeatureGenerator#computeFeaturesFromBlastResult()
	 */
	protected void computeFeaturesFromBlastResult() {
		
		Map<String, Integer> shortSeqID2label = getSeq2LabelMapWithShortenedIDs();
		
		for (String seqID: hits.keySet()) {
			
			// obtain class of best hit in sequence database
			Map<String, Double> currHits = hits.get(seqID);
			double bestScore = 0;
			String bestHit = "";
			for (String hit: currHits.keySet()) {
				if (hit.equals(seqID)) {
					continue;
				} 
				if (currHits.get(hit) > bestScore) {
					bestHit = hit;
				}
			}
			if (shortSeqID2label.containsKey(bestHit)) {
				int predClass = shortSeqID2label.get(bestHit);
				features.put(seqID, new double[] {predClass});
				
			} else if (bestHit.isEmpty()) {
				System.out.println("Warning. No BLAST hits found for sequence: " + seqID);
				
			} else {
				System.out.println("Error. No label found for sequence: " + bestHit);
			}
		}
	}
}
