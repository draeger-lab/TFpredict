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

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public enum SolverType {

    /**
     * L2-regularized logistic regression
     *
     * (fka L2_LR)
     */
    L2R_LR,

    /**
     * L2-regularized L2-loss support vector classification (dual)
     *
     * (fka L2LOSS_SVM_DUAL)
     */
    L2R_L2LOSS_SVC_DUAL,

    /**
     * L2-regularized L2-loss support vector classification (primal)
     *
     * (fka L2LOSS_SVM)
     */
    L2R_L2LOSS_SVC,

    /**
     * L2-regularized L1-loss support vector classification (dual)
     *
     * (fka L1LOSS_SVM_DUAL)
     */
    L2R_L1LOSS_SVC_DUAL,

    /**
     * multi-class support vector classification by Crammer and Singer
     */
    MCSVM_CS,

    /**
     * L1-regularized L2-loss support vector classification
     *
     * @since 1.5
     */
    L1R_L2LOSS_SVC,

    /**
     * L1-regularized logistic regression
     *
     * @since 1.5
     */
    L1R_LR;
}
