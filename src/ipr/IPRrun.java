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
package ipr;

import io.BasicTools;
import io.NoExitSecurityManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import uk.ac.ebi.webservices.jaxws.IPRScanClient;

/**
 * 
 * @author Florian Topf
 * @version $Rev$
 * @since 1.0
 */
public class IPRrun {
	
	public static boolean addSpacerLine = true;
 	
	// fixes bug in current version of InterProScan which removes first line of sequence if header is given
	private static void addSpacerLine(String seqfile) {
		
		String SPACER_LINE = "SPACERLINESPACERLINESPACERLINESPACERLINESPACERLINESPACERLINE";
		
		Map<String,String> fastaSeqs = BasicTools.readFASTA(seqfile, true);
		for (String header: fastaSeqs.keySet()) {
			String currSeq = fastaSeqs.get(header);
			fastaSeqs.put(header, SPACER_LINE + currSeq);
		}
		BasicTools.writeFASTA(fastaSeqs, seqfile);
	}
	
	public IPRrun(boolean silent) {
		this.silent = silent;
	}
	
	public IPRrun() {}
	
	private boolean silent = false;

	// gfx related map
	private static Map<String, String> seq2job = new HashMap<String,String>();
	
	// default: use local installation of InterProScan and do not write output of tool to file
	public ArrayList<String[]> run(String seqfile, String iprpath) {
		return(run(seqfile, iprpath, null, false, false));
	}
	
	public ArrayList<String[]> run(String seqfile, String iprpath, boolean useWeb) {
		return(run(seqfile, iprpath, null, useWeb, false));
	}
	
	public ArrayList<String[]> run(String seqfile, String iprpath, boolean useWeb, boolean standAloneMode) {
		return(run(seqfile, iprpath, null, useWeb, standAloneMode));
	}
	
	public ArrayList<String[]> run(String seqfile, String iprpath, String basedir) {
		return(run(seqfile, iprpath, basedir, false, false));
	}
	
	public ArrayList<String[]> run(String seqfile, String iprpath, String basedir, boolean useWeb, boolean standAloneMode) {
		
		ArrayList<String[]> IPRoutput = null;
		InputStream iprScan_StdOut;
		
		// HACK: add dummy line to fasta file to fix bug in InterProScan
		if (addSpacerLine) {
			addSpacerLine(seqfile);
		}
		
		if (useWeb) { // SOAP

			// set parameter
			String[] param = new String[6];
			
			param[0] = "--async";
			param[1] = "--email";
			param[2] = "tfpredict@hushmail.com";
			param[3] = "--goterms";
			param[4] = "--multifasta";
			param[5] = seqfile;
			
			// redirect System.out and System.err
			PrintStream orig_stdout = System.out;
			ByteArrayOutputStream stdout = new ByteArrayOutputStream();
			System.setOut(new PrintStream(stdout));
			PrintStream orig_stderr = System.err;
			if (silent) {
				System.setErr(new PrintStream(new ByteArrayOutputStream()));
			}
			
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
			ArrayList<String> jobs = grabJobIDs(new ByteArrayInputStream(stdout.toByteArray()));
			
			// restore System.out
			System.setOut(orig_stdout);
			
			IPRScanClient webIPR = new IPRScanClient();

			if (!silent) System.out.println("Waiting for " + jobs.size() + " job(s) to finish ...");
			for (String jobid : jobs) {
				if (!silent) System.out.println("Polling job \"" + jobid + "\" ...");
					try {
						webIPR.getResults(jobid, basedir + jobid, "out");
						webIPR.getResults(jobid, basedir + jobid, "visual-png");
					} catch (IOException e) {
						e.printStackTrace();
					} catch (ServiceException e) {
						e.printStackTrace();
					}
				if (!silent) System.out.println("Job \"" + jobid + "\" finished.");
			}
			
			// restore System.err
			if (silent) {
				System.setErr(orig_stderr);
			}
			IPRoutput = readIPROutput(basedir, jobs);

		} else { // local
			
			Runtime rt = Runtime.getRuntime();

		    Process proc = null;
			try {
				proc = rt.exec(iprpath +" -cli -i " + seqfile + " -format raw -goterms -iprlookup -altjobs");
				proc.waitFor();
				
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			iprScan_StdOut = proc.getInputStream();
			String iprScan_resultFile = basedir + "/InterproScanOutput.txt";
			IPRoutput = readIPRoutput(iprScan_StdOut, iprScan_resultFile);
		}
		return IPRoutput;
	}	
	
	
	private static ArrayList<String[]> readIPROutput(String basedir, ArrayList<String> jobs) {
		
		ArrayList<String[]> IPRoutput = new ArrayList<String[]>();
		
		String line = null;
		
		for (String job : jobs) {
			try {
				 BufferedReader br = new BufferedReader(new FileReader(basedir+job+".out.txt"));
				 while ((line = br.readLine()) != null) {
					 String[] tabpos = line.split("\t");
					 String seqID = tabpos[0].trim();
					 if (!seq2job.containsKey(seqID) && !seq2job.containsValue(job)) seq2job.put(seqID, job);
					 if (!line.isEmpty()) {
						 IPRoutput.add(tabpos);
					 }
				 }			 
				 br.close();
			
			} catch(IOException ioe) {
				System.out.println(ioe.getMessage());
				System.exit(1);
			}
		}
		return IPRoutput;
	}

	// used by FeatureFileGenerator
	public static ArrayList<String[]> readIPRoutput(String outputFile) {
		
		InputStream IPRoutputStream = null;
		try {
			IPRoutputStream = new FileInputStream(new File(outputFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

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
				 // skip empty lines
				 if (line.trim().equals("")) continue;
				 
				 IPRoutput.add(line.split("\t"));
				 if (saveIPRoutput2file) {
					 bw.write(line + "\n");
				 }
				 //System.out.println(line);
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

	public Map<String, String> getSeq2job() {
		return seq2job;
	}

}
