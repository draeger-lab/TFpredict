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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;

/**
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
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