/*  
 * $Id: DoubleArrayPointer.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/liblinear/DoubleArrayPointer.java $
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
 * @version $Rev: 99 $
 * @since 1.0
 */
final class DoubleArrayPointer {

    private final double[] _array;
    private int            _offset;


    public void setOffset(int offset) {
        if (offset < 0 || offset >= _array.length) throw new IllegalArgumentException("offset must be between 0 and the length of the array");
        _offset = offset;
    }

    public DoubleArrayPointer( final double[] array, final int offset ) {
        _array = array;
        setOffset(offset);
    }

    public double get(final int index) {
        return _array[_offset + index];
    }

    public void set(final int index, final double value) {
        _array[_offset + index] = value;
    }
}
