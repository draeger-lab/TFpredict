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
