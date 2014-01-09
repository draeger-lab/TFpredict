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
package liblinear;

import java.io.File;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class InvalidInputDataException extends Exception {

    private static final long serialVersionUID = 2945131732407207308L;

    private final int         _line;

    private File              _file;

    public InvalidInputDataException( String message, File file, int line ) {
        super(message);
        _file = file;
        _line = line;
    }

    public InvalidInputDataException( String message, String filename, int line ) {
        this(message, new File(filename), line);
    }

    public InvalidInputDataException( String message, File file, int lineNr, NumberFormatException cause ) {
        super(message, cause);
        _file = file;
        _line = lineNr;
    }

    public InvalidInputDataException( String message, String filename, int lineNr, NumberFormatException cause ) {
        this(message, new File(filename), lineNr, cause);
    }

    public File getFile() {
        return _file;
    }

    /**
     * This methods returns the path of the file.
     * The method name might be misleading.
     *
     * @deprecated use {@link #getFile()} instead
     */
    public String getFilename() {
        return _file.getPath();
    }

    public int getLine() {
        return _line;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + _file + ":" + _line + ")";
    }

}
