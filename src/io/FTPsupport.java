/*
 * ===============================================
 * (C) Florian Topf, University of Tuebingen, 2010
 * ===============================================
 */

package io;
import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;

// This the FTP client library from www.sauronsoftware.it/projects/ftp4j/

public class FTPsupport {

	private String domain;
	private String user = "anonymous";
	private String pass = "password";
	private String dir = "/";
	private String filename;
	
	// parse url
	public String download(String url) {
		
		String[] input = url.split("/");
		domain = input[2];
		for (int i = 3; i < input.length-1; i++) {
			dir = dir.concat(input[i]+"/");
		}
		filename = input[input.length-1];
		
		ftpget();
		
		return filename;
	}
	
	
	private void ftpget() {
		FTPClient client = new FTPClient();

		try {
	    	
	    	System.out.println("Connecting to FTP ...");
	    	client.connect(domain);

	    	client.login(user, pass);
	    	
	    	boolean get = true;
	    	
	    	client.changeDirectory(dir);
	    	
			File f = new File(filename);
			long new_last = client.modifiedDate(filename).getTime();

			if (f.exists()) {
				long curr_last = f.lastModified();
				// check date
				if ( new_last == curr_last) {
					get = false;
					System.out.println("No newer file found.");
				}
			}
			if (get) {
				
				System.out.println("Downloading "+ filename +" ...");
				client.download(filename, new File(filename));
				System.out.println("Download successful.");
				f.setLastModified(new_last);
			}
			client.disconnect(true);
		    System.out.println("FTP Connection closed.");
		
	    } catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (FTPIllegalReplyException e) {
			e.printStackTrace();
		} catch (FTPException e) {
			e.printStackTrace();
		} catch (FTPDataTransferException e) {
			e.printStackTrace();
		} catch (FTPAbortedException e) {
			e.printStackTrace();
		}
	  }
}
