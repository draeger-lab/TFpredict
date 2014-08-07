/*  
 * $Id: FourFieldTest.java 99 2014-01-09 21:57:51Z draeger $
 * $URL: https://rarepos.cs.uni-tuebingen.de/svn-path/tfpredict/src/features/FourFieldTest.java $
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
package features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.TestUtils;

/**
 * 
 * @author Florian Topf
 * @version $Rev: 99 $
 * @since 1.0
 */
public class FourFieldTest {
	
	List<String> iprs_fft = new ArrayList<String>();
	List<Double> pvalues = new ArrayList<Double>();
	
	public List<String> getDomainIDs() {
		return iprs_fft;
	}

	public List<Double> getPvalues() {
		return pvalues;
	}

	
	public void run(List<String> iprs, Map<String, List<String>> iprTF2ids, Map<String, List<String>> iprNONTF2ids) {
		
		System.out.println("Collecting data ...");
		
		long start = System.currentTimeMillis();

		// simple threading ...
		Summon tfsummon = new Summon(iprs, iprTF2ids);
		Thread tfsummonthread = new Thread(tfsummon);
		
		Summon nontfsummon = new Summon(iprs, iprNONTF2ids);
		Thread nontfsummonthread = new Thread(nontfsummon);
		
		tfsummonthread.start();
		nontfsummonthread.start();
			
		try {
			tfsummonthread.join();
			nontfsummonthread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		// get results
		int sum_tf = tfsummon.res;
		int sum_nontf = nontfsummon.res;
		
		long end = System.currentTimeMillis();
		System.out.println("Collecting time was "+(end-start)+" ms.");
		
		System.out.println("Four-Fields-Test initiated.");
		start = System.currentTimeMillis();
		// calculate final result
		computeP(iprs, iprTF2ids, sum_tf, iprNONTF2ids, sum_nontf);
		end = System.currentTimeMillis();
		System.out.println("FFT time was "+(end-start)+" ms.");
	}


	class Summon implements Runnable {
		
		List<String> iprs;
		Map<String, List<String>> ipr2ids;

		// result
		int res;
		
		public Summon(List<String> iprs, Map<String, List<String>> ipr2ids) {
			this.iprs = iprs;
			this.ipr2ids = ipr2ids;
		}

		@Override
		public void run() {
			res = summon(iprs, ipr2ids);
			
		}
		
		// calculate number of unique TFs per ipr
		private int summon(List<String> iprs, Map<String, List<String>> ipr2ids) {
			
			List<String> ids = new ArrayList<String>();
			
			for (int i = 0; i < iprs.size(); i++) {
				
				String curr_ipr = iprs.get(i);
				
				if (ipr2ids.containsKey(curr_ipr)) {
					
					List<String> curr_ids = ipr2ids.get(curr_ipr);
					
					for (int j = 0; j < curr_ids.size(); j++) {
						
						String tmp_id = curr_ids.get(j);
						
						if (!ids.contains(tmp_id)) {
							ids.add(tmp_id);
						}
					}
				}
			}
			return ids.size();
		}
	}

	
	private Double computeM(int num_tfs_ipr, int sum_tf, int num_nontfs_ipr, int sum_nontf) {
		
		// a (tf, ipr)
		int a = num_tfs_ipr;
		// b (nontf, ipr)
		int b = num_nontfs_ipr;
		// c (tf, nonipr)
		int c = sum_tf - num_tfs_ipr;
		// d (nontf, nonipr)
		int d = sum_nontf - num_nontfs_ipr;
		
		if (d < 0) {
			System.out.println(d);
		}
		
		// four field test matrix
		double[][] matrix = new double[2][2];
		matrix[0][0] = a;
		matrix[0][1] = b;
		matrix[1][0] = c;
		matrix[1][1] = d;
		
		double pvalue = 1.0;
		
		boolean useChi = checkmatrix(matrix);
		
		if (!useChi) {
			FisherExact fisherExact = new FisherExact(a+b+c+d+10);
			pvalue = fisherExact.getTwoTailedP(a, b, c, d);
		}
		else {
			try {
				
				pvalue = TestUtils.chiSquareTest(conv2long(matrix));
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (MathException e) {
				e.printStackTrace();
			}
		}
		return pvalue;
	}
	
	
	// converts double[][] to long[][]
	private long[][] conv2long(double[][] matrix) {
		int rowSize = matrix.length;
	    int columnSize = matrix[0].length;
		long[][] mn = new long[rowSize][columnSize];
		for (int i = 0; i < rowSize; i++) {
			for (int j = 0; j < columnSize; j++) {
				mn[i][j] = (long) matrix[i][j];
			}
		}
		return mn;
	}


	// function to test if exact fisher test should be uses or not
	// fisher exact should be used with evalue (testA-D) below 5
	private boolean checkmatrix(double[][] matrix) {
		
		boolean useChi = false;
		
		double a = matrix[0][0];
		double b = matrix[0][1];
		double c = matrix[1][0];
		double d = matrix[1][1];

		double testA = ((a+b)*(a+c))/(a+b+c+d);
		double testB = ((a+b)*(b+d))/(a+b+c+d);
		double testC = ((c+d)*(a+c))/(a+b+c+d);
		double testD = ((c+d)*(b+d))/(a+b+c+d);
		
		if (testA >= 5 && testB >= 5 && testC >= 5 && testD >= 5) {
			useChi = true;
		}
		
		return useChi;
	}
	
	
	private void computeP(List<String> iprs,
			Map<String, List<String>> iprTF2ids, int sum_tf,
			Map<String, List<String>> iprNONTF2ids, int sum_nontf) {
		
		Collection<Job> queue = new ArrayList<Job>();
		
		System.out.println("Building queue ...");
		
		// build queue
		for (int i = 0; i < iprs.size(); i++) {
			
			String curr_ipr = iprs.get(i);
			int num_tfs_ipr = 0; 
			int num_nontfs_ipr = 0;
			
			if (iprTF2ids.containsKey(curr_ipr)) {
				num_tfs_ipr = iprTF2ids.get(curr_ipr).size();
			}
			if (iprNONTF2ids.containsKey(curr_ipr)) {
				num_nontfs_ipr = iprNONTF2ids.get(curr_ipr).size();
			}
			
			if (num_tfs_ipr > 0 || num_nontfs_ipr > 0) {
				
				// add job to queue
				queue.add(new Job(curr_ipr, num_tfs_ipr, sum_tf, num_nontfs_ipr, sum_nontf));
				
			}
		}
		
		System.out.println("Processing queue ...");
		
		// process queue
		//ExecutorService exec = Executors.newCachedThreadPool();
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		List<Future<Result>> results = null;
		try {
			results = exec.invokeAll(queue);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		exec.shutdown();
		
		// retrieve results
		for (Future<Result> res : results) {
			Result current = null;
			try {
				current = res.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
			iprs_fft.add(current.ipr);
			pvalues.add(current.pvalue);
		}
		
	}
	
	//job object: does the FFT
	class Job implements Callable<Result>{
		
		String ipr;
		int num_tfs_ipr;
		int sum_tf;
		int num_nontfs_ipr;
		int sum_nontf;
		
		public Job(String ipr, int num_tfs_ipr, int sum_tf, int num_nontfs_ipr, int sum_nontf) {
			this.ipr = ipr;
			this.num_tfs_ipr = num_tfs_ipr;
			this.sum_tf = sum_tf;
			this.num_nontfs_ipr = num_nontfs_ipr;
			this.sum_nontf = sum_nontf;
		}

		@Override
		public Result call() throws Exception {

			double pvalue = computeM(num_tfs_ipr, sum_tf, num_nontfs_ipr, sum_nontf);	
			return new Result(ipr, pvalue);
		}
		
	}

	//result object: holds ipr and its pvalue
	class Result {
		
		String ipr;
		double pvalue;
		
		public Result(String ipr, double pvalue) {
			this.ipr = ipr;
			this.pvalue = pvalue;
		}
		
	}


}

