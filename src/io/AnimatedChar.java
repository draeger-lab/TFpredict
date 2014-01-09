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
package io;

import java.io.PrintStream;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 
 * @author Johannes Eichner
 * @version $Rev$
 * @since 1.0
 */
public class AnimatedChar {
    private final static String anim= "|/-\\";
    
    int callNumber = 0;
    Timer t = null;
    
    PrintStream out = System.out;
    
    public void setOutputStream(PrintStream out) {
    	this.out = out;
    }
    
    public void showAnimatedChar() {
    	
    	t = new Timer();
    	t.schedule(new TimerTask() {
			@Override
			public void run() {
		    	callNumber = (callNumber+1)% anim.length();
		    	char c = anim.charAt(callNumber);
		    	out.print('\r');
		    	out.print(c);
			}
		}, 200, 200);
    }
    
    public void hideAnimatedChar() {
    	if (t!=null) {
    		t.cancel();
    		out.print('\r');
    	}
    }
    
    
    
    
    
}
