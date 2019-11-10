package tests;

import static org.junit.jupiter.api.Assertions.*;

import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import defaultAndHelper.JavaFXHelper;
import helper.Helper;

class benchMarkDecimalNumberMatrix {

	void testMultiplicationHeapStart(int matrixPairs, int n) {
		try {
			long startTime, estimatedTime;
			//Test set
			DecimalNumberMatrix[] matricesA = new DecimalNumberMatrix[matrixPairs];
			for (int i = 0; i < matrixPairs; i++)
				matricesA[i] = DecimalNumberMatrix.randomMatrix(n, n, 0, 100, false);

			DecimalNumberMatrix[] matricesB = new DecimalNumberMatrix[matrixPairs];
			for (int i = 0; i < matrixPairs; i++)
				matricesB[i] = DecimalNumberMatrix.randomMatrix(n, n, 0, 100, false);
			System.out.println("(finished setup of test bed)");
			
			// IJK DecimalNumber method (current)
			startTime = System.nanoTime();    
			//for (int i = 0; i < matrixPairs; i++)
			//	DecimalNumberMatrix.matrixMultiplication(matricesA[i],matricesB[i]);
			
			estimatedTime = System.nanoTime() - startTime;
			System.out.println("Time elapsed for DecimalNumber IJK method (current):      " + JavaFXHelper.formatNanoSeconds(estimatedTime, true));
			
			// IKJ DecimalNumber method
			startTime = System.nanoTime();    
			for (int i = 0; i < matrixPairs; i++)
				DecimalNumberMatrix.matrixMultiplicationDecimalNumberIKJ(matricesA[i],matricesB[i]);
			
			estimatedTime = System.nanoTime() - startTime;
			System.out.println("Time elapsed for DecimalNumber IKJ method:                " + JavaFXHelper.formatNanoSeconds(estimatedTime, true));
		

			// Double IKJ
			startTime = System.nanoTime();  
			for (int i = 0; i < matrixPairs; i++) 
				DecimalNumberMatrix.matrixMultiplicationDoubleIKJ(matricesA[i], matricesB[i]);
			
			
			estimatedTime = System.nanoTime() - startTime;
			System.out.println("Time elapsed for Double IKJ method:                        " + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

		} catch (Exception e) { e.printStackTrace(); }
	}
	

	void testMultiplicationUnsharedMatrices(int matrixPairs, int n) {

		long startTime, estimatedTime;

		// IJK DecimalNumber method (current)
		startTime = System.nanoTime();    
		for (int i = 0; i < matrixPairs; i++)
			DecimalNumberMatrix.matrixMultiplication(DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false),DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false));

		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for DecimalNumber IJK method (current):             \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

		// IKJ DecimalNumber method
		startTime = System.nanoTime();    
		for (int i = 0; i < matrixPairs; i++)
			DecimalNumberMatrix.matrixMultiplicationDecimalNumberIKJ(DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false),DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false));

		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for DecimalNumber IKJ method:                      \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));


		// Double IKJ
		startTime = System.nanoTime();  
		for (int i = 0; i < matrixPairs; i++) 
			DecimalNumberMatrix.matrixMultiplicationDoubleIKJ(DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false), DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false));


		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for Double IKJ method:                             \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

	
	}
	
	
	void testMultiplicationUnsharedVectorMatrices(int matrixPairs, int n) {

		long startTime, estimatedTime;

		// IJK DecimalNumber method (current)
		startTime = System.nanoTime();    
		for (int i = 0; i < matrixPairs; i++)
			DecimalNumberMatrix.matrixMultiplication(DecimalNumberMatrix.randomMatrix(n, 1, -100, 100, false),DecimalNumberMatrix.randomMatrix(1, n, -100, 100, false));

		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for DecimalNumber IJK method (current):             \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

		// IKJ DecimalNumber method
		startTime = System.nanoTime();    
		for (int i = 0; i < matrixPairs; i++)
			DecimalNumberMatrix.matrixMultiplicationDecimalNumberIKJ(DecimalNumberMatrix.randomMatrix(n, 1, -100, 100, false),DecimalNumberMatrix.randomMatrix(1, n, -100, 100, false));

		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for DecimalNumber IKJ method:                      \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));


		
		// Double IJK
		startTime = System.nanoTime();  
		for (int i = 0; i < matrixPairs; i++) 
			DecimalNumberMatrix.matrixMultiplicationDoubleIJK(DecimalNumberMatrix.randomMatrix(n, 1, -100, 100, false), DecimalNumberMatrix.randomMatrix(1, n, -100, 100, false));

		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for Double IJK method:                             \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

		
		// Double IKJ
		startTime = System.nanoTime();  
		for (int i = 0; i < matrixPairs; i++) 
			DecimalNumberMatrix.matrixMultiplicationDoubleIKJ(DecimalNumberMatrix.randomMatrix(n, 1, -100, 100, false), DecimalNumberMatrix.randomMatrix(1, n, -100, 100, false));

		estimatedTime = System.nanoTime() - startTime;
		System.out.println("Time elapsed for Double IKJ method:                             \t" + JavaFXHelper.formatNanoSeconds(estimatedTime, true));

	}
	

	Number[] discrepancyTest(DecimalNumberMatrix A, DecimalNumberMatrix B) {
		int discrepanciesExact = 0;
		int discrepanciesApprox = 0;
		int total = A.nrow() * A.ncol();
		DecimalNumber sum = new DecimalNumber(0);
		
		for (int r = 0; r < A.nrow(); r++)
			for (int c = 0; c < A.nrow(); c++) {
				sum.add(A.getRow(r).get(c).subtract(B.getRow(r).get(c), false).abs());
				
				if (!A.getRow(r).get(c).equals(B.getRow(r).get(c))) {
					discrepanciesExact++;
					//System.err.println(A.getRow(r).get(c).subtract(B.getRow(r).get(c), false).abs());
				}
				
				if (!A.getRow(r).get(c).equals(B.getRow(r).get(c), true))
					discrepanciesApprox++; 

			}
		
		return new Number[] {discrepanciesExact, discrepanciesApprox, total, sum};
		
	}
	
	void reliabilityTestIJK_IKJ(int matrixPairs, int n) {
		long discrepanciesExact = 0;
		long discrepanciesApprox = 0;
		long total = 0;
		DecimalNumber sum = new DecimalNumber(0);
		
		for (int i = 0; i < matrixPairs; i++) {
			DecimalNumberMatrix A = DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false);
			DecimalNumberMatrix B = DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false);

			DecimalNumberMatrix IJK = DecimalNumberMatrix.matrixMultiplication(A, B);
			DecimalNumberMatrix IKJ = DecimalNumberMatrix.matrixMultiplicationDecimalNumberIKJ(A, B);

			Number[] results = discrepancyTest(IJK, IKJ);
			discrepanciesExact += (int) results[0];
			discrepanciesApprox += (int) results[1];
			total += (int) results[2];
			sum.add((DecimalNumber)results[3]) ;
		}
		
		System.out.println("\nTesting reliability IJK vs. IKJ:");
		System.out.println("Number of discrepancies:                                           \t" + discrepanciesExact + " (" + ((double)discrepanciesExact/(double)total)*100 + "%) ");
		System.out.println("Sum of discrepancies:                                              \t" + sum);
		
	}
	

	
	
	void reliabilityTestIKJ_DoubleUnrounded(int matrixPairs, int n) {
		long discrepanciesExact = 0;
		long discrepanciesApprox = 0;
		long total = 0;
		DecimalNumber sum = new DecimalNumber(0);
		
		for (int i = 0; i < matrixPairs; i++) {
			DecimalNumberMatrix A = DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false);
			DecimalNumberMatrix B = DecimalNumberMatrix.randomMatrix(n, n, -100, 100, false);

			DecimalNumberMatrix IKJ = DecimalNumberMatrix.matrixMultiplicationDecimalNumberIKJ(A, B);
			DecimalNumberMatrix DU = DecimalNumberMatrix.matrixMultiplicationDoubleIKJ(A, B);

			Number[] results = discrepancyTest(IKJ, DU);
			discrepanciesExact += (int) results[0];
			discrepanciesApprox += (int) results[1];
			total += (int) results[2];
			sum.add((DecimalNumber)results[3]) ;
		}
		
		System.out.println("\nTesting reliability IKJ DecimalNumber vs. IKJ double (both unrounded)");
		System.out.println("Number of discrepancies:                                           \t" + discrepanciesExact + " (" + ((double)discrepanciesExact/(double)total)*100 + "%) ");
		System.out.println("Sum of discrepancies:                                              \t" + sum);
		
	}
	

//	@Test
//	void testMultiplicationMethods() {
//		try {
//			int[] numberOfMatrices, matrixSize;
//
//			System.out.println(Helper.repString("=", 200));
//			System.out.println(Helper.repString("=", 200));
//			System.out.println("\t\t\t\t\t\t\tTESTING VECTOR ([n*1] * [1*n]) MATRICES");
//			System.out.println(Helper.repString("=", 200));
//			System.out.println(Helper.repString("=", 200));
//
//			numberOfMatrices = new int[] {10};
//			matrixSize = new int[] { 200 };
//			for (int matrixPairs: numberOfMatrices)
//				for (int n: matrixSize) 
//				{
//					System.out.println("\n\n" + Helper.repString("-", 200) +"\n\n\nStarting run with [" + matrixPairs + "] matrix pairs, each of size [" + n + "]:\n");
//					testMultiplicationUnsharedVectorMatrices(matrixPairs,n );
//				}
//
//			System.out.println(Helper.repString("=", 200));
//			System.out.println(Helper.repString("=", 200));
//			System.out.println("\t\t\t\t\t\t\tTESTING SQUARE ([n*n] * [n*n]) MATRICES");
//			System.out.println(Helper.repString("=", 200));
//			System.out.println(Helper.repString("=", 200));
//
//			numberOfMatrices = new int[] {10};
//			matrixSize = new int[] { 10};
//			for (int matrixPairs: numberOfMatrices)
//				for (int n: matrixSize) 
//				{
//					System.out.println("\n\n" + Helper.repString("-", 200) +"\n\n\nStarting run with [" + matrixPairs + "] matrix pairs, each of size [" + n + "]:\n");
//					testMultiplicationUnsharedMatrices(matrixPairs, n);
//
//					//				System.out.println("\n\nTesting validity: ");
//					//				reliabilityTestIJK_IKJ(matrixPairs, n);
//					//				reliabilityTestIKJ_DoubleUnrounded(matrixPairs, n);
//				}
//
//
//			System.out.println(Helper.repString("=", 200));
//			System.out.println(Helper.repString("=", 200));
//			System.out.println("\t\t\t\t\t\t\tComparing both");
//			System.out.println(Helper.repString("=", 200));
//			System.out.println(Helper.repString("=", 200));
//
//			numberOfMatrices = new int[] {10, 100, 250};
//			matrixSize = new int[] { 10, 20, 50, 100, 200 };
//			for (int matrixPairs: numberOfMatrices)
//				for (int n: matrixSize) 
//				{
//					System.out.println("\n\n" + Helper.repString("-", 200) +"\n\n\nStarting run with [" + matrixPairs + "] matrix pairs, each of size [" + n + "]:\n");
//					testMultiplicationUnsharedMatrices(matrixPairs, n);
//					System.out.println(Helper.repString("-", 200));
//					testMultiplicationUnsharedVectorMatrices(matrixPairs,n );
//
//				}
//
//
//
//		} catch (Exception e) { e.printStackTrace();}
//	}
	
	@Test
	void doublePrecision() {
		DecimalNumberMatrix DNM = DecimalNumberMatrix.randomMatrix(4, 4, 0, 1, false);
		double[][] dm1 = DNM.toDoubleMatrix();
		double[][] dm2 = DecimalNumberMatrix.DOUBLE_clone(dm1);
		System.out.println(DNM);
		System.out.println(Helper.arrayToString(dm2));
	}
	
	
	
	

//	
//	DecimalNumberMatrix m = DecimalNumberMatrix.randomMatrix(3, 3, 0, 10, true);
//	m.setTwoDArray();
//	System.err.println(m);
//	for (int i = 0; i < m.nrow(); i++) {
//		for (int j = 0; j < m.ncol(); j++)
//			System.err.print(m.twoDArray[i][j] + "\t");
//		System.err.print("\n");
//	}
}
