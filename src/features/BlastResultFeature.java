/*
 * $Id: BLASTfeatureGenerator.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/features/BLASTfeatureGenerator.java $
 * ----------------------------------------------------------------------------
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
 * ----------------------------------------------------------------------------
 */
package features;

/**
 * This class encapsulates the resulting features from one BLAST hit file.
 * 
 * @author Andreas Dr&auml;ger
 */
public class BlastResultFeature {

	private int errorCount;
	private double features[];
	private String sequenceId;
	private int warningCount;

	/**
	 * 
	 * @param seqId
	 * @param features
	 */
	public BlastResultFeature(String seqId, double features[]) {
		this(seqId, features, 0, 0);
	}

	/**
	 * 
	 * @param sequenceId
	 * @param features
	 * @param errorCount
	 * @param warningCount
	 */
	public BlastResultFeature(String sequenceId, double features[], int errorCount, int warningCount) {
		this.sequenceId = sequenceId;
		this.features = features;
		this.errorCount = errorCount;
		this.warningCount = warningCount;
	}

	/**
	 * @return the errorCount
	 */
	public int getErrorCount() {
		return errorCount;
	}

	/**
	 * @return the features
	 */
	public double[] getFeatures() {
		return features;
	}

	/**
	 * @return the sequenceId
	 */
	public String getSequenceId() {
		return sequenceId;
	}

	/**
	 * @return the warningCount
	 */
	public int getWarningCount() {
		return warningCount;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isSetFeatures() {
		return (features != null) && (features.length > 0);
	}

}
