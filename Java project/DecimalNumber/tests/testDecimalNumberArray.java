package tests;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;

class testDecimalNumberArray {

	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////					      ///////////////////////////////
	///////////////////////////// 	DecimalNumberArray 	 ////////////////////////////////
	////////////////////////////						/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	
	@Test
	void arrayConstructors() {
		DecimalNumberArray dnad = new DecimalNumberArray(1,2,3,4);
		DecimalNumberArray dnabd = new DecimalNumberArray(new BigDecimal(1), new BigDecimal(2), new BigDecimal(3), new BigDecimal(4));
		DecimalNumberArray dnadn = new DecimalNumberArray(new DecimalNumber(1), new DecimalNumber(2), new DecimalNumber(3), new DecimalNumber(4));
		DecimalNumberArray dnai = new DecimalNumberArray(4);
		////System.out.println("DNA double constructor: " + dnad);
		////System.out.println("DNA bigDec constructor: " + dnabd);
		////System.out.println("DNA decNum constructor: " + dnadn);
		////System.out.println("DNA nullIn constructor: " + dnai);
		assert(dnad.equals(dnabd));
		assert(dnad.equals(dnadn));
		assert(!dnad.equals(dnai));
	}
	
	@Test
	void arrayElementaryOperations()
	{
		DecimalNumberArray dna = new DecimalNumberArray(-2,-1,-0,1,2);
		assert(dna.get(0).equals(-2));
		assert(dna.get(dna.length()-1).equals(2));
		
		dna.set(2, 10);
		assert(dna.get(2).equals(10));
		assert(dna.max().equals(10));
		assert(dna.min().equals(-2));
		assert(!dna.sum().equals(0));
		
		dna.set(2, 0);
		assert(!dna.contains(10, false));
		assert(!dna.contains(10, true));
		assert(!dna.contains(2.0000001, false));
		assert(!dna.contains(2.0000001, true));
		assert(dna.max().equals(2));
		assert(dna.sum().equals(0));
	}
	
	@Test
	void arrayClone() {
		DecimalNumberArray original = new DecimalNumberArray(-2,-1,-0,1,2);
		DecimalNumberArray clone    = original.clone();
		for (int i = 0; i < clone.length(); i ++)
			assert(clone.get(i).equals(original.get(i)) && clone.get(i)!= original.get(i));
	}
	
	@Test
	void arraySeq() throws IllegalRangeException, IllegalScaleException {
		DecimalNumberArray dna = new DecimalNumberArray(-2,-1,-0,1,2);
		DecimalNumberArray dnad = DecimalNumberArray.sequence(-2, 2, 1 );
		DecimalNumberArray dnadn = DecimalNumberArray.sequence(new DecimalNumber(-2), new DecimalNumber(2), new DecimalNumber(1));
		assert(dna.equals(dnad));
		assert(dna.equals(dnadn));
	}
	
	@Test
	void arrayRep() {
		DecimalNumberArray dna = new DecimalNumberArray(3,3,3,3,3,3);
		DecimalNumberArray dnad = DecimalNumberArray.rep(3, 6);
		DecimalNumberArray dnadn = DecimalNumberArray.rep(new DecimalNumber(3), 6);
		for (int i = 1; i < dnadn.array.length; i ++) 
			assert(dnadn.get(i-1) != dnadn.get(i));
		assert(dna.equals(dnad));
		assert(dna.equals(dnadn));
	}
	
	@Test
	void arraySubset() {
		DecimalNumberArray dna = new DecimalNumberArray(-2,-1,-0,1,2);
		assert(dna.subset(0, 3).equals(new DecimalNumberArray(-2,-1,0)));
		assert(dna.subset(2, dna.length()).equals(new DecimalNumberArray(0,1,2)));
	}
	
	@Test
	void arrayConcatenate() {
		DecimalNumberArray dna1 = new DecimalNumberArray(-2,-1,-0,1,2);
		DecimalNumberArray dna2 = new DecimalNumberArray(3,4,5,6,7);
		DecimalNumberArray dna3 = dna1.concatenate(dna2);
		
		assert(dna3.length() == dna1.length() + dna2.length());
		assert(dna3.subset(0, dna1.length()).equals(dna1));
		assert(dna3.subset(dna1.length(), dna3.length()).equals(dna2));
	}
	
	@Test
	void arrayNormalize() throws IllegalRangeException, IllegalScaleException {
		DecimalNumberArray dna1 = new DecimalNumberArray(2,1,0,1,2);
		dna1.toProbability();
		assert(dna1.sum().equals(1));
		for (DecimalNumber dn: dna1.array)
			assert(dn.compareTo(0)!= -1 && dn.compareTo(1)!=1);
	}
	
	@Test
	void arrayScale() throws IllegalRangeException, IllegalScaleException {
		DecimalNumberArray dna1 = new DecimalNumberArray(-2,-1,0,1,2);
		dna1.scale(10);
		assert(dna1.sum().equals(0));
		assert(dna1.get(0).equals(-20));
		
		dna1 = new DecimalNumberArray(-2,-1,0,1,2);
		dna1.scale(new DecimalNumber(10));
		assert(dna1.sum().equals(0));
		assert(dna1.get(0).equals(-20));
		
		dna1 = new DecimalNumberArray(2,1,0,1,2);
		dna1.scale(10);
		assert(dna1.sum().equals(60));
		assert(dna1.get(0).equals(20));
	}
	
	@Test
	void arrayConcatenateRStyle() {
		DecimalNumberArray dna1 = new DecimalNumberArray(-2,-1,0,1,2);
		////System.out.println("CONCATENATE R: \n" + dna1 + "\n TO R: \n" + dna1.concatenateRStyle());
	}
	
	@Test
	void arrayContains() {
		DecimalNumberArray dna1 = new DecimalNumberArray(-2,-1,0,1,2);
		assert (dna1.contains(-2.0000000001, true));
		assert (!dna1.contains(-2.000000001, false));
		System.out.println("\n-++---");
		assert (!dna1.contains(10, true));
		System.out.println("\n-++---");
		assert (!dna1.contains(10, false));
	}
	
	@Test
	void arrayIndexOf() {
		DecimalNumberArray dna1 = new DecimalNumberArray(-2,-1,0,1,2);
		assert (dna1.indexOf(-2) == 0);
		assert (dna1.indexOf(new BigDecimal(-2)) == 0);
		assert (dna1.indexOf(new DecimalNumber(-2)) == 0);
		
		assert (dna1.indexOf(2) == 4);
		assert (dna1.indexOf(new BigDecimal(2)) == 4);
		assert (dna1.indexOf(new DecimalNumber(2)) == 4);
		
		assert (dna1.indexOf(12) == -1);
		assert (dna1.indexOf(new BigDecimal(12)) == -1);
		assert (dna1.indexOf(new DecimalNumber(12)) == -1);
		
	}

	@Test
	void arrayInsertAndRemove() {
		DecimalNumberArray a = new DecimalNumberArray(1,2,3,4,5);
		a.insert(2, new DecimalNumber(12));
		assert(a.length()==6);
		//System.err.println(a);
		
		try {a.remove(2); } catch (Exception e) { e.printStackTrace();}
		//assert(a.length()==5);
		//System.err.println(a);
	}
	
	@Test
	void arrayMetrics() throws IllegalRangeException, IllegalScaleException {
		DecimalNumberArray dna = new DecimalNumberArray(2,3,4,5,6,7,8);
		assert (dna.mean().equals(5));
		try {
	
		}catch (Exception e) { e.printStackTrace();}
	}
	
	@Test
	void arrayDotProduct() throws IllegalRangeException, IllegalScaleException {
		DecimalNumberArray a1 = new DecimalNumberArray(1,2,3);
		DecimalNumberArray a2 = new DecimalNumberArray(0.5, 1, 2);
		DecimalNumber dotProduct = a1.dotProduct(a2);
		assert (dotProduct.equals(0.5+2+6));
	}
	
	@Test 
	void arrayStandardDeviation() {
		DecimalNumberArray a = new DecimalNumberArray(1,2,3,4,5,6,7,8,9,-100);
		System.err.println("M = " + a.mean() );
		System.err.println("Var = " + a.variance());
		System.err.println("SD = " + a.standardDeviation());
		
	}
}
