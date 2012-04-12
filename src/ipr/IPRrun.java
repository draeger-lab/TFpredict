package ipr;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class IPRrun {

	public static ArrayList<String[]> run(String seqfile, String iprpath) {
		return run(seqfile, iprpath, false);
	}
	
	public static ArrayList<String[]> run(String seqfile, String iprpath, boolean use_web) {
		
		Runtime rt = Runtime.getRuntime();

	    Process proc = null;
		try {
			proc = rt.exec(iprpath +" -cli -i " + seqfile + " -format raw -goterms -iprlookup -altjobs");
			proc.waitFor();
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// get the output stream
		ArrayList<String[]> IPRoutput = readIPRoutput(proc.getInputStream());
		
		return IPRoutput;
	}
	
	
	// reads the standard output from InterProScan
	private static ArrayList<String[]> readIPRoutput(InputStream IPRoutputStream) {
		
		ArrayList<String[]> IPRoutput = new ArrayList<String[]>();
		
		String line = null;
		try {
			 BufferedReader br = new BufferedReader(new InputStreamReader(IPRoutputStream, "UTF-8")); 
			 while ((line = br.readLine()) != null) {
				 IPRoutput.add(line.split("\t"));
			 }			 
			 br.close();
		}
		
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("Parse Error. The error occurred while parsing the output of InterProScan.");
			System.exit(1);
		}
		
		return IPRoutput;
	}
}
