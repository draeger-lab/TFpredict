package io;
/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/*
 * Provides read/write support for serializable objects
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
	
	////
	// object reader
	public static Object read(String name, boolean silent) {
		
		if (! silent) System.out.println("  Reading object: " + name);
		
		Object tmp = new Object(); 
		
		// read object from file
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(name);
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
