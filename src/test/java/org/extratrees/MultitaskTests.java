package org.extratrees;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class MultitaskTests {
	public static FactorExtraTrees getData1(int ndata, int ndim) {
		int[] output = new int[ndata];
		double[] v   = new double[ndata*ndim];
		for (int i=0; i<v.length; i++) {
			v[i] = Math.random();
		}
		for (int i=0; i<ndata; i++) {
			output[i] = (i%2==0 ?0 :1);
		}
		Matrix m = new Matrix(v, ndata, ndim);
		return new FactorExtraTrees(m, output);
	}

	@Test
	public void testIdsSplitByTask() {
		FactorExtraTrees et = getData1(10, 5);
		et.tasks = new int[] {0, 0, 0, 1, 1, 1, 2, 3, 6, 3};
		Set<Integer> leftTasks = new HashSet<Integer>();
		leftTasks.add(0);
		leftTasks.add(3);
		int[][] split = et.splitIdsByTask(new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9}, leftTasks);
		//System.out.println( Arrays.toString(split[0]) );
		//System.out.println( Arrays.toString(split[1]) );
		// left node:
		assertArrayEquals(new int[]{0, 1, 2, 7, 9}, split[0]);
		// right node:
		assertArrayEquals(new int[]{3, 4, 6, 8}, split[1]);
	}
	
	@Test
	public void testSumAlong() {
		int[][] factorTaskTable = new int[2][];
		int N = 10;
		factorTaskTable[0] = new int[N];
		factorTaskTable[1] = new int[N];
		for (int task=0; task<N; task++) {
			factorTaskTable[0][task] = task % 5;
			factorTaskTable[1][task] = task % 3;
		}
		double[] sum = FactorExtraTrees.sumAlong2nd(factorTaskTable);
		assertEquals(20, sum[0], 1e-7);
		assertEquals(9,  sum[1], 1e-7);
	}

	public static FactorExtraTrees getData2(int ndata, int ndim, int ntasks) {
		if (ndim<2) {
			ndim = 2;
		}
		
		int[] output = new int[ndata];
		int[] tasks  = new int[ndata];
		double[] v   = new double[ndata*ndim];
		for (int i=0; i<v.length; i++) {
			v[i] = Math.random();
		}
		Matrix m = new Matrix(v, ndata, ndim);
		for (int i=0; i<ndata; i++) {
			tasks[i]  = i%ntasks;
			if (m.get(i, 0) < 0.5) {
				if (tasks[i]%2==0) {
					// type 1 task:
					output[i] = Math.random()<0.05  ?1  :0;
				} else {
					// type 2 task:
					output[i] = Math.random()<0.95  ?1  :0;
				}
			} else {
				// independeng of task:
				output[i] = m.get(i,1)<0.5  ?1  :0;
				
			}
		}
		return new FactorExtraTrees(m, output, tasks);
	}
	
	private double[] testMT(int ndata) {
		int ntasks = 50;
		int ndim   = 10;
		FactorExtraTrees et  = getData2(ndata, ndim, ntasks);
		FactorExtraTrees et0 = new FactorExtraTrees(et.input, et.output);
		FactorExtraTrees testing = getData2(1000, ndim, ntasks);
		assertEquals(ntasks, et.nTasks);
		int nmin = 7;
		int K    = 5;
		int nTrees = 10;
		et.setProbOfTaskCuts(0.700);
		et.learnTrees(nmin, K, nTrees);
		et0.learnTrees(nmin, K, nTrees);
		
		int[] yhat  = et.getValuesMT(testing.input, testing.tasks);
		int[] yhat0 = et0.getValues(testing.input);
		
		double errors = 0;
		double errors0 = 0;
		for (int i=0; i<testing.output.length; i++) {
			if (testing.output[i]!=yhat[i]) {
				errors ++;
			}
			if (testing.output[i]!=yhat0[i]) {
				errors0++;
			}
		}
		errors /= yhat.length;
		errors0 /= yhat0.length;

		// testing allValuesMT
		Matrix all = et.getAllValuesMT(testing.input, testing.tasks);
		assertEquals( testing.input.nrows, all.nrows );
		assertEquals( et.getNumTrees(),    all.ncols );
		
		return new double[]{ errors, errors0 };
	}

	@Test
	public void testMT() {
		double[] e500 = new double[2];
		double[] e1k  = new double[2];
		double[] e2k  = new double[2];
		double[] temp;
		int rep = 10;
		for (int i=0; i<rep; i++) {
			temp = testMT(500);
			e500[0] += temp[0];
			e500[1] += temp[1];
			temp = testMT(1000);
			e1k[0] += temp[0];
			e1k[1] += temp[1];
			temp = testMT(2000);
			e2k[0] += temp[0];
			e2k[1] += temp[1];
		}
		for (int i=0; i<2; i++) {
			e500[i] /= rep;
			e1k[i]  /= rep;
			e2k[i]  /= rep;
		}
		
		System.out.println(String.format("Error rate (N= 500)\t[MT] %1.3f\t [ST] %1.3f", e500[0], e500[1]) );
		System.out.println(String.format("Error rate (N=1000)\t[MT] %1.3f\t [ST] %1.3f", e1k[0], e1k[1]) );
		System.out.println(String.format("Error rate (N=2000)\t[MT] %1.3f\t [ST] %1.3f", e2k[0], e2k[1]) );
		//System.out.println(String.format("Error rate (single-task): %1.3f", errors0) );
		
	}
}