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
final class IntArrayPointer {

    private final int[] _array;
    private int         _offset;


    public void setOffset(int offset) {
        if (offset < 0 || offset >= _array.length) throw new IllegalArgumentException("offset must be between 0 and the length of the array");
        _offset = offset;
    }

    public IntArrayPointer( final int[] array, final int offset ) {
        _array = array;
        setOffset(offset);
    }

    public int get(final int index) {
        return _array[_offset + index];
    }

    public void set(final int index, final int value) {
        _array[_offset + index] = value;
    }
}
