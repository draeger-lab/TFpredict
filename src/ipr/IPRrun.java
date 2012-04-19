package ipr;

import io.NoExitSecurityManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import javax.xml.rpc.ServiceException;
import modes.Predict;
import uk.ac.ebi.webservices.jaxws.IPRScanClient;


public class IPRrun {
	
	// default: use local installation of InterProScan and do not write output of tool to file
	public static ArrayList<String[]> run(String seqfile, String iprpath) {
		return(run(seqfile, iprpath, null, false));
	}
	
	public static ArrayList<String[]> run(String seqfile, String iprpath, boolean useWeb) {
		return(run(seqfile, iprpath, null, useWeb));
	}
	
	public static ArrayList<String[]> run(String seqfile, String iprpath, String outputFile) {
		return(run(seqfile, iprpath, outputFile, false));
	}
	
	public static ArrayList<String[]> run(String seqfile, String iprpath, String outputFile, boolean useWeb) {
		
		ArrayList<String[]> IPRoutput = null;
		
		if (useWeb) { // SOAP
			
			// set parameter
			String[] param = new String[6];
			
			param[0] = "--async";
			param[1] = "--email";
			param[2] = Predict.email;
			param[3] = "--goterms";
			param[4] = "--multifasta";
			param[5] = seqfile;
			
			// redirect System.out
			PrintStream orig_stdout = System.out;
			ByteArrayOutputStream stdout = new ByteArrayOutputStream();
			System.setOut(new PrintStream(stdout));
			
			// prevent System.exit
			SecurityManager SecMan = System.getSecurityManager();
			System.setSecurityManager(new NoExitSecurityManager());
						
			// submit job
			try {
				IPRScanClient.main(param);
			} catch (Exception e) {	}
			
			// restore System.exit
			System.setSecurityManager(SecMan);
			
			// grab jobids
			ArrayList<String> joblist = grabJobIDs(new ByteArrayInputStream(stdout.toByteArray()));
			
			// get results only
			stdout.reset();
			IPRScanClient webIPR = new IPRScanClient();
			orig_stdout.println("Waiting for "+joblist.size()+" job(s) to finish ...");
			for (String jobid : joblist) {
				orig_stdout.println("Polling job \""+jobid+"\" ...");
				try {
					webIPR.getResults(jobid, "-", "out");
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ServiceException e) {
					e.printStackTrace();
				}
				orig_stdout.println("Job \""+jobid+"\" finished.");
			}
						
			// restore System.out
			System.setOut(orig_stdout);
			
			IPRoutput = readIPRoutput(new ByteArrayInputStream(stdout.toByteArray()));
			
		} else { // local
			
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
			IPRoutput = readIPRoutput(proc.getInputStream(), outputFile);
		}
		return IPRoutput;
	}
	
	private static ArrayList<String[]> readIPRoutput(InputStream IPRoutputStream) {
		return(readIPRoutput(IPRoutputStream, null));
	}
	
	// reads the standard output from InterProScan
	private static ArrayList<String[]> readIPRoutput(InputStream IPRoutputStream, String outputFile) {
		
		ArrayList<String[]> IPRoutput = new ArrayList<String[]>();
		
		// switch to save standard output of InterProScan to file (primarily used for testing and debugging)
		boolean saveIPRoutput2file = false;
		if (outputFile != null) {
			saveIPRoutput2file = true;
		}
		
		String line = null;
		try {
			 BufferedReader br = new BufferedReader(new InputStreamReader(IPRoutputStream, "UTF-8")); 
			 BufferedWriter bw = null;
			 if (saveIPRoutput2file) {
				 bw = new BufferedWriter(new FileWriter(new File(outputFile)));
			 }
			 
			 while ((line = br.readLine()) != null) {
				 IPRoutput.add(line.split("\t"));
				 if (saveIPRoutput2file) {
					 bw.write(line + "\n");
				 }
				 System.out.println(line);
			 }			 
			 br.close();
			 if (saveIPRoutput2file) {
				 bw.flush();
				 bw.close();
			 }
		}
		
		catch(IOException ioe) {
			System.out.println(ioe.getMessage());
			System.out.println("Parse Error. The error occurred while parsing the output of InterProScan.");
			System.exit(1);
		}
		
		return IPRoutput;
	}
	
	
	private static ArrayList<String> grabJobIDs(ByteArrayInputStream byteArrayInputStream) {
		
		ArrayList<String> jobids = new ArrayList<String>();
		
		String line = null;
		try {
			 BufferedReader br = new BufferedReader(new InputStreamReader(byteArrayInputStream, "UTF-8")); 
			 while ((line = br.readLine()) != null) {
				 jobids.add(line.trim());
			 }			 
			 br.close();
		} catch(IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		
		if (jobids.isEmpty()) System.err.println("Empty Joblist!");
		
		return jobids;
	}
}
