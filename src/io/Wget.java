package io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

public class Wget {
	
	public String fetchbuffered(String url) {
		
		BufferedInputStream bis = null;
		
		try {
			bis = new BufferedInputStream(new URL(url).openStream());

		} catch (IOException e) {
			System.out.println("The given URL \"" + url + "\" was not found.");
			return(null);
		}
		return readStream(bis);
	}
	
	private String readStream(BufferedInputStream bis) {
		String output = "";
		
        byte[] buffer = new byte[1024];
        
        try {
            int bytesRead = 0;
            
            while ((bytesRead = bis.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, bytesRead);
                output = output.concat(chunk);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (bis != null)
                    bis.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
		return output;
	}
}