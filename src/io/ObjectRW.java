/*
    TFpredict performs the identification and structural characterization
    of transcription factors.
    Copyright (C) 2012 ZBIT, University of Tübingen, Florian Topf and Johannes Eichner

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import resources.Resource;

/**
 * Provides read/write support for serializable objects
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
public class ObjectRW {

	////
	// object writer
	public static void write(Object O, String name) {
		write(O, name, false);
	}
	
	
	public static void write(Object O, String name, boolean silent) {
		
		if (! silent) System.out.println(" Writing object: " + name);
		
		// write object to file
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			fos = new FileOutputStream(name);
			out = new ObjectOutputStream(fos);
			out.writeObject(O);
			out.close();
			}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		
	}
	
	
	public static Object read(String name) {
		return(read(name, false));
	}
	
	
	public static Object read(String name, boolean silent) {
		
		if (! silent) System.out.println("  Reading object: " + name);
		
		// read object from file
		Object tmp = null;
		
		try {
			FileInputStream fis = new FileInputStream(name);
			tmp = read(fis, silent);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return tmp;
	}
	
	public static Object readFromResource(String name) {
		return(read(Resource.class.getResourceAsStream(name), true));
	}
	
	public static Object read(InputStream fis, boolean silent) {
				
		Object tmp = new Object(); 
		
		// read object from file
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(fis);
			tmp = in.readObject();
			in.close();
			}
		catch(IOException ex) {
			ex.printStackTrace();
			}
		catch(ClassNotFoundException ex) {
			ex.printStackTrace();
			}
		
		return tmp;
	}
	

}
