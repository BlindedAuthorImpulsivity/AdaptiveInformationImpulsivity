package tests;

import org.junit.jupiter.api.Test;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.ComputationException;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import helper.Helper;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import decimalNumber.TransformationFunction.TransformationFunctionDecimalNumber;

class testDecimalNumberMatrix {


	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////					      ///////////////////////////////
	///////////////////////////// 	DecimalNumberMatrix	 ////////////////////////////////
	////////////////////////////						/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	@Test
	void matrixConstructors() throws ComputationException {
		// These are all visual tests
	/*	DecimalNumberArray a1 = new DecimalNumberArray(1, 2);
		DecimalNumberArray a2 = new DecimalNumberArray(3, 4);
		DecimalNumberMatrix m = new DecimalNumberMatrix(a1, a2);
		//System.out.println(m);
		
		m.setColumnNames(new String[] {"Column1", "Column2"});
		//System.out.println("\n"+m);
		
		m.setColumnNames(null);
		m.setRowNames("row1", "row2");
		//System.out.println("\n" + m);
		
		m.setColumnNames("Column1", "column2");
		//System.out.println("\n" + m);
		
		m = new DecimalNumberMatrix(2,2);
		m.setColumnNames("Column1", "c2");
		m.setRowNames("row1", "row2");
		//System.out.println("\n" + m);
		
		m = new DecimalNumberMatrix(2,2, true, new DecimalNumber(1), new DecimalNumber(2), new DecimalNumber(3), new DecimalNumber(4));
		m.setColumnNames("c1", "c2");
		m.setRowNames("row1", "row2");
		//System.out.println("\n" + m);
		
		m = new DecimalNumberMatrix(2,2, false, new DecimalNumber(1), new DecimalNumber(2), new DecimalNumber(3), new DecimalNumber(4));
		m.setColumnNames("c1", "c2");
		m.setRowNames("row1", "row2");
		//System.out.println("\nBYROW = FALSE\n" + m);
		

		m = new DecimalNumberMatrix(2,2, true, 1,2,3,4);
		m.setColumnNames("c1", "c2");
		m.setRowNames("row1", "row2");
		//System.out.println("\n" + m);
		
		m = new DecimalNumberMatrix(2,2, false, 1,2,3,4);
		m.setColumnNames("c1", "c2");
		m.setRowNames("row1", "row2");
		//System.out.println("\nBYROW = FALSE\n" + m);
		
		m = new DecimalNumberMatrix(3,3, true, 1,2,3,4,5,6,7,8,9);
		m.setColumnNames("c1", "c2", "c3");
		m.setRowNames("row1", "row2", "r3");
		//System.out.println("\n" + m);
		
		m = new DecimalNumberMatrix(3,3,false, 1,2,3,4,5,6,7,8,9);
		m.setColumnNames("c1", "c2", "c3");
		m.setRowNames("row1", "row2", "r3");
		//System.out.println("\nBYROW = FALSE\n" + m);*/
	}
	
	@Test
	void matrixColumnRepresentation() {
		System.out.println("\n\n"+Helper.repString("*+", 50));
		System.err.println("Testing: matrixColumnRepresentation():" + "\n");
		
		DecimalNumberMatrix m = new DecimalNumberMatrix (3,3, true, 1,2,3, 4,5,6, 7,8,9);
		System.out.println(m);
		System.out.println("\nPer column:");
		for (DecimalNumberArray c : m.columnMatrix())
			System.out.println(c);
		
		assert(m.getRow(1).get(1) == m.getColumn(1).get(1));
	
		m.setValueAt(1, 1, 50);

		assert(m.getRow(1).get(1) == m.getColumn(1).get(1));
		
		System.out.println("After changing [1,1] from value 5 to value 50");
		System.out.println(m);
		System.out.println("\nPer column:");
		for (DecimalNumberArray c : m.columnMatrix())
			System.out.println(c);
			
	}
	
	@Test 
	void matrixGettersAndSetters() throws ComputationException, IllegalRangeException, UnsupportedOperationException, IllegalScaleException {
		DecimalNumberMatrix m = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
		m.setColumnNames("c1", "c2", "c3");
		m.setRowNames("r1", "r2", "r3");
		//System.out.println("\n" + m);
		assert(m.getIndexOfColumn("c1")==0);
		//assert(m.getIndexOfColumn("notAcolumnNAME")==-1);
		
		assert(m.getIndexOfRow("r1")==0);
		//assert(m.getIndexOfRow("notArowNAME")==-1);
		
		m.setAllValues(new DecimalNumber(1.23456789,true));
		//System.out.println("\n" + m);
		
		m = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
		m.setColumnNames("c1", "c2", "c3");
		m.setRowNames("r1", "r2", "r3");
		assert(m.getColumn(1).get(1).equals(5));
		assert(m.getColumn("c2").get(1).equals(5));
		
		System.err.println("Column before " + m.getColumn(1));
		m.setColumn(1, new DecimalNumberArray(10,11,12));
		System.err.println("Column after  " + m.getColumn(1));
		
		System.err.println("\n"+m + "\n\nEQUALS: " + m.getColumn(1).get(1));
		assert(m.getColumn(1).get(1).equals(11));
		assert(m.getColumn("c2").get(0).equals(10));
		m.setColumn("c2", new DecimalNumberArray(2,5,8));
		assert(m.getColumn(1).get(1).equals(5));
		assert(m.getColumn("c2").get(1).equals(5));
		
		assert(m.getRow(1).get(2).equals(6));
		assert(m.getRow("r2").get(2).equals(6));
		m.setRow(0, new DecimalNumberArray(21,22,23));
		assert(m.getRow("r1").get(1).equals(22));
		m.setRow(0, new DecimalNumberArray(1,2,3));
		//System.out.println("\n" + m);
		assert(m.getIndexOfRowWhereColumnIs("c2", new DecimalNumber(2)) == 0);
		assert(m.getIndexOfRowWhereColumnIs("c2", 8) == 2);
		
		assert(m.getValueAt(1, 2).equals(6));
		assert(m.getValueAt(2, "c1").equals(7));
		assert(m.getValueAt("r1", "c3").equals(3));
		
		  
        DecimalNumberArray r1 = new DecimalNumberArray( 
        		new DecimalNumber(0.1, 0, 1, false),
        		new DecimalNumber(0.11, 0, 1, false),
        		new DecimalNumber(0.12, 0, 1, false),
        		new DecimalNumber(0.13, 0, 1, false));
        
        DecimalNumberArray r2 = new DecimalNumberArray( 
        		new DecimalNumber(0.2, 0, 1, false),
        		new DecimalNumber(0.21, 0, 1, false),
        		new DecimalNumber(0.22, 0, 1, false),
        		new DecimalNumber(0.23, 0, 1, false));
        
        DecimalNumberArray r3 = new DecimalNumberArray( 
        		new DecimalNumber(0.3, 0, 1, false),
        		new DecimalNumber(0.31, 0, 1, false),
        		new DecimalNumber(0.32, 0, 1, false),
        		new DecimalNumber(0.33, 0, 1, false));
        
        DecimalNumberMatrix m1 = new DecimalNumberMatrix(r1, r2, r3);
        m1.setColumnNames("Column1", "Column2", "Column3", "Column42");
        System.out.println(m1);
        
        m1.getValueAt(0, 0).set(0.5);
        System.out.println("\n\n"+m1);
		
	}
	@Test
	void matrixContains() throws ComputationException {
		DecimalNumberMatrix m = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
		assert(m.contains(2.00000001, true));
		assert(!m.contains(2.00000001, false));
		assert(m.contains(2, false));
		assert(!m.contains(10.00000001, true));
	}
	
	@Test
	void matrixClone() throws ComputationException {
		DecimalNumberMatrix m = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
		DecimalNumberMatrix m2 = m.clone();
		m.setAllValues(new DecimalNumber(10));
		assert(m2.contains(2, false));
		assert(!m2.contains(10, false));
		m2.setAllValues(new DecimalNumber(11));
		assert(m.contains(10, true));
		assert(!m.contains(11, true));
	}
	
	@Test
	void matrixReduce() throws ComputationException {
		DecimalNumberMatrix m = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
		m.setColumnNames("c1", "c2", "c3");
		m.setRowNames("r1", "r2", "r3");
		DecimalNumberMatrix reduced = m.reduce("c2", new DecimalNumberArray(2.0), true, false);
		//System.out.println("REDUCED:\n" + reduced);
		reduced = reduced.reduce("c1", new DecimalNumberArray(4.0), false, false);
		//System.out.println("REDUCED:\n" + reduced);
	}
	
	@Test
	void matrixrowBind() throws ComputationException {
		DecimalNumberMatrix m1 = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
		DecimalNumberMatrix m2 = new DecimalNumberMatrix(1,3,true,	10,11,12);
		DecimalNumberMatrix m3 = DecimalNumberMatrix.rowBind(m1, m2);
		//System.out.println(m3);
		
		assert(m3.nrow()==4);
		m1.setColumnNames("col0", "col1", "col2");
		m2.setColumnNames("col0", "col1", "col2");
		m3 = DecimalNumberMatrix.rowBind(m1, m2);
		//System.out.println("BOUND\n" + m3);
		
		m1.setRowNames("row0", "row1", "row2");
		m3 = DecimalNumberMatrix.rowBind(m1, m2);
		//System.out.println("BOUND\n" + m3);
		
	}
	
	@Test
	void matrixSubsetRows() throws ComputationException {
		DecimalNumberMatrix m1 = new DecimalNumberMatrix(5,3,false,	1,2,3, 	4,5,6,	7,8,9,	10,11,12,	13,14,15);
		DecimalNumberMatrix m2 = m1.subsetRangeOfRows(0, 3);
		//System.out.println("SUBSETTED\n" + m2);
		
		DecimalNumberMatrix m3 = m1.subsetRows(0, 3);
		//System.out.println("SUBSETTED\n" + m3);
	}
	
	@Test
	void matrixTranspose() throws ComputationException {
		DecimalNumberMatrix m1 = new DecimalNumberMatrix(5,3,false,	1,2,3, 	4,5,6,	7,8,9,	10,11,12,	13,14,15);
		m1.setColumnNames("c1", "c2", "c3");
		m1.setRowNames("r1", "r2", "r3", "r4", "r5");
		DecimalNumberMatrix m2 = m1.transpose();
		//System.out.println("PRETRANSPOSED\n" + m1);
		//System.out.println("TRANSPOSED\n" + m2);
	}
	
	@Test
	void matrixVectorize() throws ComputationException {
		DecimalNumberMatrix m1 = new DecimalNumberMatrix(5,3,true,	1,2,3, 	4,5,6,	7,8,9,	10,11,12,	13,14,15);
		try {
		//System.out.println("Pre vectorization:\n" + m1 );
		//System.out.println("Vectorized by row:\n" + m1.vectorize(true) );
		//System.out.println("Vectorized by col:\n" + m1.vectorize(false) );
		} catch (Exception e) { e.printStackTrace(); }
	}

	@Test
	void matrixAddAndRemoveRows() throws ComputationException {
		try {
		
			// Appending row
			DecimalNumberMatrix M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
			
			M.appendRow(new DecimalNumberArray(100, 101, 102));
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 4);
			assert (M.ncol() == 3);
			assert (M.getRow(3).get(0).equals(100));
			assert (M.getRow(3).get(1).equals(101));
			assert (M.getRow(3).get(2).equals(102));
			
			//Removing first row
			M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
			
			M.removeRow(0);
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 2);
			assert (M.ncol() == 3);
			assert (M.getRow(0).get(0).equals(4));
			assert (M.getRow(0).get(1).equals(5));
			assert (M.getRow(0).get(2).equals(6));
			
			//Removing last row
			M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);

			M.removeRow(2);
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 2);
			assert (M.ncol() == 3);
			assert (M.getRow(M.nrow()-1).get(0).equals(4));
			assert (M.getRow(M.nrow()-1).get(1).equals(5));
			assert (M.getRow(M.nrow()-1).get(2).equals(6));

			
			// inserting row at 0
			M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);

			M.insertRow(0, new DecimalNumberArray(100,101,102));
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 4);
			assert (M.ncol() == 3);
			assert (M.getRow(0).get(0).equals(100));
			assert (M.getRow(0).get(1).equals(101));
			assert (M.getRow(0).get(2).equals(102));
	
			// inserting row at end
			M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);

			M.insertRow(3, new DecimalNumberArray(100,101,102));
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 4);
			assert (M.ncol() == 3);
			assert (M.getRow(M.nrow()-1).get(0).equals(100));
			assert (M.getRow(M.nrow()-1).get(1).equals(101));
			assert (M.getRow(M.nrow()-1).get(2).equals(102));

		} catch (Exception e) { e.printStackTrace(); }
	}
	
	@Test
	void matrixAddAndRemoveColumns() throws ComputationException {
		try {
			
			//Removing first column
			DecimalNumberMatrix M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);
			
			M.removeColumn(0);
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 3);
			assert (M.ncol() == 2);
			assert (M.getColumn(0).get(0).equals(2));
			assert (M.getColumn(0).get(1).equals(5));
			assert (M.getColumn(0).get(2).equals(8));
			
			//Removing last Column 
			M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);

			M.removeColumn(2);
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 3);
			assert (M.ncol() == 2);
			assert (M.getColumn(M.ncol()-1).get(0).equals(2));
			assert (M.getColumn(M.ncol()-1).get(1).equals(5));
			assert (M.getColumn(M.ncol()-1).get(2).equals(8));

			
			// inserting Column at 0
			M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);

			M.insertColumn(0, new DecimalNumberArray(100,101,102));
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 3);
			assert (M.ncol() == 4);
			assert (M.getColumn(0).get(0).equals(100));
			assert (M.getColumn(0).get(1).equals(101));
			assert (M.getColumn(0).get(2).equals(102));
	
			// inserting Column at end
			M = new DecimalNumberMatrix(3,3,true,	1,2,3, 	4,5,6,	7,8,9);

			M.insertColumn(3, new DecimalNumberArray(100,101,102));
			for (int r = 0; r < M.nrow(); r++)
				for (int c= 0; c< M.ncol(); c++)
					assert (M.getRow(r).get(c) == M.getColumn(c).get(r));
			assert (M.columnMatrix().length == M.ncol());
			assert (M.nrow() == 3);
			assert (M.ncol() == 4);
			assert (M.getColumn(M.ncol()-1).get(0).equals(100));
			assert (M.getColumn(M.ncol()-1).get(1).equals(101));
			assert (M.getColumn(M.ncol()-1).get(2).equals(102));

			} catch (Exception e) { e.printStackTrace(); }
		
		
	}
	
	@Test
	void matrixSortAndToColumnVector() throws ComputationException, UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		DecimalNumberMatrix m1 = new DecimalNumberMatrix(5,3,true,	1,2,3, 	7,8,9,	4,5,6 , 13,14,15, 10,11,12);
		System.out.println("--------------------------------------------\nMatrix before sorting:\n\n" + m1);
		
		m1.sort(2, true);
		System.out.println("\nMatrix after sorting 2 column ascending: \n" + m1);
		
		m1.sort(0, false);
		System.out.println("\nMatrix after sorting column 0 descending: \n" + m1 + "\n");
		
		DecimalNumberArray[] cvectors = m1.toColumnVectors();
		for (int c = 0; c < cvectors.length; c++  )
			System.out.println("Column vector " + c+ ": " + cvectors[c]);
		
		cvectors[0].get(0).set(100);
		System.out.println("\nMatrix settting [0,0] to 100 by changing the column vector: \n" + m1 + "\n");
		
		DecimalNumberMatrix m2 = DecimalNumberMatrix.columnBind(cvectors);
		System.out.println("\n Matrix after recombining column vectors: \n" + m2);
	}
	
	@Test
	void matrixTestShallowAndDeepCopy() throws ComputationException {
		DecimalNumberMatrix original = new DecimalNumberMatrix(5,3,true,	1,2,3, 	7,8,9,	4,5,6 , 13,14,15, 10,11,12);
		DecimalNumberMatrix deepClone = original.clone();
		DecimalNumberMatrix shallowClone = original.shallowClone();
		
		original.setValueAt(0, 0, new DecimalNumber(9999));
		assert(deepClone.getValueAt(0, 0).equals(1));
		assert(shallowClone.getValueAt(0, 0).equals(9999));
		
		shallowClone.setValueAt(1, 1, new DecimalNumber(-10));
		assert(deepClone.getValueAt(1, 1).equals(8));
		assert(original.getValueAt(1, 1).equals(-10));
		
		deepClone.setValueAt(1, 1, new DecimalNumber(-25));
		assert(shallowClone.getValueAt(1, 1).equals(-10));
		assert(original.getValueAt(1, 1).equals(-10));
		
		shallowClone.removeColumn(0);
		original.setValueAt(0, 0, new DecimalNumber(65));

		
		assert(shallowClone.getValueAt(0, 0).equals(2));
		assert(original.getValueAt(0, 0).equals(65));
	}
	
	@Test
	void matrixTextScalarMultiplication() {
		try {
			DecimalNumberMatrix A = new DecimalNumberMatrix(3,3,true, 1,2,3,4,5,6,7,8,9);
			DecimalNumberMatrix B = DecimalNumberMatrix.scalarMultiplication(A, 2);
			
			assert(A.getRow(0).get(0).equals(1));		assert(A.getRow(0).get(1).equals(2));		assert(A.getRow(0).get(2).equals(3));
			assert(A.getRow(1).get(0).equals(4));		assert(A.getRow(1).get(1).equals(5));		assert(A.getRow(1).get(2).equals(6));
			assert(A.getRow(2).get(0).equals(7));		assert(A.getRow(2).get(1).equals(8));		assert(A.getRow(2).get(2).equals(9));
		
			assert(B.getRow(0).get(0).equals(2));		assert(B.getRow(0).get(1).equals(4));		assert(B.getRow(0).get(2).equals(6));
			assert(B.getRow(1).get(0).equals(8));		assert(B.getRow(1).get(1).equals(10));		assert(B.getRow(1).get(2).equals(12));
			assert(B.getRow(2).get(0).equals(14));		assert(B.getRow(2).get(1).equals(16));		assert(B.getRow(2).get(2).equals(18));
			
			A.scalarMultiplication(new DecimalNumber(2));
			for (int r = 0; r < A.nrow(); r++)
				for (int c = 0; c < A.ncol(); c++)
					A.getRow(r).get(c).equals(B.getRow(r).get(c));
		} catch (Exception e) { e.printStackTrace();}
	}
	
	@Test
	void matrixTestMatrixMultiplication() {
		DecimalNumberMatrix A = new DecimalNumberMatrix(3,3,true, 1,2,3,4,5,6,7,8,9);
		DecimalNumberMatrix B = new DecimalNumberMatrix(3,3,true, 10,11,12,13,14,15,16,17,18);
		
		DecimalNumberMatrix C = A.matrixMultiplication(B);
		assert(A.getRow(0).get(0).equals(1));		assert(A.getRow(0).get(1).equals(2));		assert(A.getRow(0).get(2).equals(3));
		assert(A.getRow(1).get(0).equals(4));		assert(A.getRow(1).get(1).equals(5));		assert(A.getRow(1).get(2).equals(6));
		assert(A.getRow(2).get(0).equals(7));		assert(A.getRow(2).get(1).equals(8));		assert(A.getRow(2).get(2).equals(9));
		
		assert(C.getRow(0).get(0).equals(84));		assert(C.getRow(0).get(1).equals(90));		assert(C.getRow(0).get(2).equals(96));
		assert(C.getRow(1).get(0).equals(201));		assert(C.getRow(1).get(1).equals(216));		assert(C.getRow(1).get(2).equals(231));
		assert(C.getRow(2).get(0).equals(318));		assert(C.getRow(2).get(1).equals(342));		assert(C.getRow(2).get(2).equals(366));
		
		B = B.matrixMultiplication(A);
		assert(A.getRow(0).get(0).equals(1));		assert(A.getRow(0).get(1).equals(2));		assert(A.getRow(0).get(2).equals(3));
		assert(A.getRow(1).get(0).equals(4));		assert(A.getRow(1).get(1).equals(5));		assert(A.getRow(1).get(2).equals(6));
		assert(A.getRow(2).get(0).equals(7));		assert(A.getRow(2).get(1).equals(8));		assert(A.getRow(2).get(2).equals(9));
		
		assert(B.getRow(0).get(0).equals(138));		assert(B.getRow(0).get(1).equals(171));		assert(B.getRow(0).get(2).equals(204));
		assert(B.getRow(1).get(0).equals(174));		assert(B.getRow(1).get(1).equals(216));		assert(B.getRow(1).get(2).equals(258));
		assert(B.getRow(2).get(0).equals(210));		assert(B.getRow(2).get(1).equals(261));		assert(B.getRow(2).get(2).equals(312));
	}
	
	@Test 
	void matrixTestMatrixElementwise() {
		DecimalNumberMatrix A = new DecimalNumberMatrix(3,3,true, 1,2,3,4,5,6,7,8,9);
		DecimalNumberMatrix B = new DecimalNumberMatrix(3,3,true, 10,11,12,13,14,15,16,17,18);
		DecimalNumberMatrix C = DecimalNumberMatrix.entrywiseMultiplication(A, B);
		
		assert(A.getRow(0).get(0).equals(1));		assert(A.getRow(0).get(1).equals(2));		assert(A.getRow(0).get(2).equals(3));
		assert(A.getRow(1).get(0).equals(4));		assert(A.getRow(1).get(1).equals(5));		assert(A.getRow(1).get(2).equals(6));
		assert(A.getRow(2).get(0).equals(7));		assert(A.getRow(2).get(1).equals(8));		assert(A.getRow(2).get(2).equals(9));
		
		assert(C.getRow(0).get(0).equals(10));		assert(C.getRow(0).get(1).equals(22));		assert(C.getRow(0).get(2).equals(36));
		assert(C.getRow(1).get(0).equals(52));		assert(C.getRow(1).get(1).equals(70));		assert(C.getRow(1).get(2).equals(90));
		assert(C.getRow(2).get(0).equals(112));		assert(C.getRow(2).get(1).equals(136));		assert(C.getRow(2).get(2).equals(162));
			
	}
	
	@Test 
	void matrixTestScalarAddition() {
		DecimalNumberMatrix A = new DecimalNumberMatrix(3,3,true, 1,2,3,4,5,6,7,8,9);
		DecimalNumberMatrix B = DecimalNumberMatrix.scalarAddition(A, 2);
		
		assert(A.getRow(0).get(0).equals(1));		assert(A.getRow(0).get(1).equals(2));		assert(A.getRow(0).get(2).equals(3));
		assert(A.getRow(1).get(0).equals(4));		assert(A.getRow(1).get(1).equals(5));		assert(A.getRow(1).get(2).equals(6));
		assert(A.getRow(2).get(0).equals(7));		assert(A.getRow(2).get(1).equals(8));		assert(A.getRow(2).get(2).equals(9));
		
		assert(B.getRow(0).get(0).equals(3));		assert(B.getRow(0).get(1).equals(4));		assert(B.getRow(0).get(2).equals(5));
		assert(B.getRow(1).get(0).equals(6));		assert(B.getRow(1).get(1).equals(7));		assert(B.getRow(1).get(2).equals(8));
		assert(B.getRow(2).get(0).equals(9));		assert(B.getRow(2).get(1).equals(10));		assert(B.getRow(2).get(2).equals(11));
		
		A.scalarAddition(new DecimalNumber(2));

		for (int r = 0; r < A.nrow(); r++)
			for (int c =0; c < A.ncol(); c++)
				assert(A.getValueAt(r, c).equals(B.getValueAt(r, c)));
			
	}
	
	@Test 
	void matrixTestMatrixAddition() {
		DecimalNumberMatrix A = new DecimalNumberMatrix(3,3,true, 1,2,3,4,5,6,7,8,9);
		DecimalNumberMatrix B = new DecimalNumberMatrix(3,3,true, 10,11,12,13,14,15,16,17,18);
		DecimalNumberMatrix C = DecimalNumberMatrix.matrixAddition(A, B);
		
		assert(A.getRow(0).get(0).equals(1));		assert(A.getRow(0).get(1).equals(2));		assert(A.getRow(0).get(2).equals(3));
		assert(A.getRow(1).get(0).equals(4));		assert(A.getRow(1).get(1).equals(5));		assert(A.getRow(1).get(2).equals(6));
		assert(A.getRow(2).get(0).equals(7));		assert(A.getRow(2).get(1).equals(8));		assert(A.getRow(2).get(2).equals(9));
		
		assert(C.getRow(0).get(0).equals(11));		assert(C.getRow(0).get(1).equals(13));		assert(C.getRow(0).get(2).equals(15));
		assert(C.getRow(1).get(0).equals(17));		assert(C.getRow(1).get(1).equals(19));		assert(C.getRow(1).get(2).equals(21));
		assert(C.getRow(2).get(0).equals(23));		assert(C.getRow(2).get(1).equals(25));		assert(C.getRow(2).get(2).equals(27));
			
	}
	
	
	@ Test
	void matrixTestFunction() {
		DecimalNumberMatrix A = new DecimalNumberMatrix(3,3,true, 1,2,3,4,5,6,7,8,9);
		
		// A > 5
		DecimalNumberMatrix B = DecimalNumberMatrix.apply(A, new TransformationFunctionDecimalNumber() {

			@Override
			public DecimalNumber function(DecimalNumber argument) {
				if (argument.compareTo(5) == 1)
					return new DecimalNumber(1);
				return new DecimalNumber(0);
			}});
		
		assert(A.getRow(0).get(0).equals(1));		assert(A.getRow(0).get(1).equals(2));		assert(A.getRow(0).get(2).equals(3));
		assert(A.getRow(1).get(0).equals(4));		assert(A.getRow(1).get(1).equals(5));		assert(A.getRow(1).get(2).equals(6));
		assert(A.getRow(2).get(0).equals(7));		assert(A.getRow(2).get(1).equals(8));		assert(A.getRow(2).get(2).equals(9));
		
		assert(B.getRow(0).get(0).equals(0));		assert(B.getRow(0).get(1).equals(0));		assert(B.getRow(0).get(2).equals(0));
		assert(B.getRow(1).get(0).equals(0));		assert(B.getRow(1).get(1).equals(0));		assert(B.getRow(1).get(2).equals(1));
		assert(B.getRow(2).get(0).equals(1));		assert(B.getRow(2).get(1).equals(1));		assert(B.getRow(2).get(2).equals(1));
		
		A.scalarAddition(-5);
		
		// A' > 0
		DecimalNumberMatrix C = DecimalNumberMatrix.apply(A, new TransformationFunctionDecimalNumber() {

			@Override
			public DecimalNumber function(DecimalNumber argument) {
				if (argument.compareTo(0) == 1)
					return new DecimalNumber(1);
				return new DecimalNumber(0);
			}});
		

		assert(C.getRow(0).get(0).equals(0));		assert(C.getRow(0).get(1).equals(0));		assert(C.getRow(0).get(2).equals(0));
		assert(C.getRow(1).get(0).equals(0));		assert(C.getRow(1).get(1).equals(0));		assert(C.getRow(1).get(2).equals(1));
		assert(C.getRow(2).get(0).equals(1));		assert(C.getRow(2).get(1).equals(1));		assert(C.getRow(2).get(2).equals(1));
		
	}
}
