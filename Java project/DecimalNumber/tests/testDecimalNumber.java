package tests;

import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.MathContext;

import org.junit.jupiter.api.Test;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.ComputationException;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException; 

class testDecimalNumber {
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////					  ///////////////////////////////
	//////////////////////////////// 	DecimalNumber 	 ////////////////////////////////
	///////////////////////////////						/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	@Test
	void numberDecimalNumberConstructors() throws IllegalRangeException {
		DecimalNumber stringTwelve = new DecimalNumber("12");
		DecimalNumber doubleTwelve = new DecimalNumber(12.0);
		DecimalNumber bigDecimalTwelveD = new DecimalNumber(new BigDecimal(12.0));
		DecimalNumber bigDecimalTwelveS = new DecimalNumber(new BigDecimal("12.0"));

		assert (stringTwelve.equals(doubleTwelve));
		assert( doubleTwelve.equals(bigDecimalTwelveD) );
		assert (bigDecimalTwelveD.equals(bigDecimalTwelveS));

		DecimalNumber d1 = new DecimalNumber(0.26594756);
		DecimalNumber d2 = new DecimalNumber(0.26594756, true);
		DecimalNumber d3 = new DecimalNumber(0.26594756, 0, 1, true);
		DecimalNumber d4 = new DecimalNumber(0.26594756, 0, 1, false);
		DecimalNumber d5 = new DecimalNumber(0.2659475600000, 0, 1, true);
		DecimalNumber d6 = new DecimalNumber(0.26594756000000000000000000000001, 0, 1, true);
		DecimalNumber d7 = new DecimalNumber(0.26594756000000000999999999999999, 0, 1, true);
		assert(d1.equals(d2));
		assert(d2.equals(d3));
		assert(d3.equals(d4));
		assert(d4.equals(d5));
		assert(d5.equals(d6));
		assert(d6.equals(d7));
	}

	@Test
	void numberConstants() {
		assert (DecimalNumber.ONE.equals(1));
		assert (DecimalNumber.ZERO.equals(0));
		assert (DecimalNumber.NEGATIVE_ONE.equals(-1));
		assert (DecimalNumber.NEGATIVE_INFINITY.compareTo(1000000)==-1);
		assert (DecimalNumber.NEGATIVE_INFINITY.compareTo(-100000)==-1);
		assert (DecimalNumber.POSITIVE_INFINITY.compareTo(1000000)==1);
		assert (DecimalNumber.POSITIVE_INFINITY.compareTo(-100000)==1);
	}

	@Test 
	void numberSetters() throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(210.547);
		dn1.set(51.25);
		assert (dn1.equals(51.25));
		dn1.set("0.25");
		assert (dn1.equals(0.25));
		dn1.set(BigDecimal.TEN);
		assert(dn1.equals(10));

		DecimalNumber dn2 = new DecimalNumber(10.2369, true);
		try { dn2.set(BigDecimal.ZERO); fail(); }
		catch (UnsupportedOperationException e) {
			//////System.out.println("Unsupported operation exception expected and found");
		}
		assert (dn2.equals(10.2369));
	}

	@Test 
	void numberComparisons() throws IllegalRangeException, IllegalScaleException{
		DecimalNumber high = new DecimalNumber(-10.8);
		DecimalNumber low = new DecimalNumber(-927.34);
		assert(high.compareTo(low) == 1);
		assert(low.compareTo(high) == -1);
		assert(high.compareTo(-10.8)==0);
		assert (high.abs().compareTo(10.8)==0);
		assert (!high.equals(low));
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Addition 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	@Test 
	void numberAdditionMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(5.6, false);
		DecimalNumber dn2 = new DecimalNumber(5.8, false);
		DecimalNumber dn3 = dn1.add(dn2);
		assert(dn2.equals(5.8));

		assert(dn1 == dn3);
		assert(dn1.doubleValue() == 11.4);
		assert(dn1.equals(11.4));
	}

	@Test 
	void numberAdditionImmutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(5.6, true);
		DecimalNumber dn2 = new DecimalNumber(5.8, true);
		DecimalNumber dn3 = dn1.add(dn2);
		assert(dn2.equals(5.8));
		assert(dn1.doubleValue() == 5.6);
		assert(dn3.doubleValue() == 11.4);

		DecimalNumber dn4 = new DecimalNumber(11.2).add(5).add(DecimalNumber.parseStringToBigDecimal("-5"));
		assert (dn4.equals(11.2));

		DecimalNumber dn5 = dn1.add(2, false);
		assert (dn5.equals(7.6));
		assert (dn1.equals(5.6));
	}

	@Test 
	void numberAdditionMixedMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(5.6, true);
		DecimalNumber dn2 = new DecimalNumber(5.8, false);
		DecimalNumber dn3 = dn1.add(dn2);
		DecimalNumber dn4 = dn1.add(dn1, true);
		assert(dn2.equals(5.8));
		assert(dn1.doubleValue() == 5.6);
		assert(dn3.doubleValue() == 11.4);
		assert(dn4.equals(5.6*2));
	}

	@Test
	void numberStaticAddition() throws IllegalRangeException{
		assert(DecimalNumber.add(5.8, 5.6).doubleValue() == 11.4);
		DecimalNumber dn1 = DecimalNumber.add(1, 3);
		DecimalNumber dn2 = DecimalNumber.add(-1, -3);
		assert (DecimalNumber.add(dn1, dn2).equals(0));
		assert( dn1.equals(4));
		assert (dn2.equals(-4));
	}

	@Test
	void numberRangeMutable() throws IllegalRangeException	{
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, false);
		try {
			probability.add(1);
			fail();
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
		} catch (IllegalScaleException e) {
			////System.out.println("WARNING: Illegal scale exception not expected but found.");
		}
	}

	@Test
	void numberRangeImmutable() throws IllegalRangeException {
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, true);
		try {
			probability.add(1);
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
			assert(probability.equals(0.5));
		} catch (IllegalScaleException e) {
			assert(probability.equals(0.5));
		}

	}


	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Subtraction 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	@Test 
	void numberSubtractionMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(5.6, false);
		DecimalNumber dn2 = new DecimalNumber(5.8, false);
		DecimalNumber dn3 = dn2.subtract(dn1);
		assert(dn1.equals(5.6));

		assert(dn2 == dn3);
		assert(dn2.doubleValue() == 0.2);
		assert(dn2.equals(0.2));
	}

	@Test 
	void numberSubtractionImmutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(5.6, true);
		DecimalNumber dn2 = new DecimalNumber(5.8, true);
		DecimalNumber dn3 = dn1.subtract(dn2);
		assert(dn2.equals(5.8));
		assert(dn1.doubleValue() == 5.6);
		assert(dn3.doubleValue() == -0.2);

		DecimalNumber dn4 = new DecimalNumber(11.2).subtract(5).subtract(DecimalNumber.parseStringToBigDecimal("-5"));
		assert (dn4.equals(11.2));

		DecimalNumber dn5 = dn1.subtract(2, false);
		assert (dn5.equals(3.6));
		assert (dn1.equals(5.6));
	}

	@Test 
	void numberSubtractionMixedMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(5.6, true);
		DecimalNumber dn2 = new DecimalNumber(5.8, false);
		DecimalNumber dn3 = dn1.subtract(dn2);
		DecimalNumber dn4 = dn1.subtract(dn1, true);
		assert(dn2.equals(5.8));
		assert(dn1.doubleValue() == 5.6);
		assert(dn3.doubleValue() == -0.2);
		assert(dn4.equals(0));
	}

	@Test
	void numberStaticSubtraction() throws IllegalRangeException{
		assert(DecimalNumber.subtract(5.8, 5.6).doubleValue() == 0.2);
		DecimalNumber dn1 = DecimalNumber.subtract(1, 3);
		DecimalNumber dn2 = DecimalNumber.subtract(-1, -3);
		assert (DecimalNumber.subtract(dn1, dn2).equals(-4));
		assert (DecimalNumber.subtract(dn2, dn1).equals(4));
		assert( dn1.equals(-2));
		assert (dn2.equals(2));
	}

	@Test
	void numberRangeMutableSubtraction() throws IllegalRangeException, IllegalScaleException	{
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, false);
		try {
			probability.subtract(1);
			fail();
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
		} catch (IllegalScaleException e) {
			////System.out.println("WARNING: Illegal scale exception not expected but found.");
		}
		probability.set(0.5);
		assert(probability.subtract(0.2).equals(0.3));
	}

	@Test
	void numberRangeImmutableSubtract() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, true);
		try {
			probability.subtract(1);
			fail();
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
			assert(probability.equals(0.5));
		} catch (IllegalScaleException e) {
			assert(probability.equals(0.5));
		}
		assert(probability.subtract(0.2).equals(0.3));
	}


	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Multiplication 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	@Test 
	void numberMultiplicationMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(12.5, false);
		DecimalNumber dn2 = new DecimalNumber(3.1, false);
		DecimalNumber dn3 = dn2.multiply(dn1);
		assert(dn1.equals(12.5));

		assert(dn2 == dn3);
		assert(dn2.equals(38.75));
	}

	@Test 
	void numberMultiplicationImmutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(23, true);
		DecimalNumber dn2 = new DecimalNumber(-2.4, true);
		DecimalNumber dn3 = dn1.multiply(dn2);
		assert(dn2.equals(-2.4));
		assert(dn1.equals(23));
		assert(dn3.doubleValue() == -55.2);

		DecimalNumber dn4 = new DecimalNumber(11.2).multiply(5).multiply(DecimalNumber.parseStringToBigDecimal("-5"));
		assert (dn4.equals(-280));

		DecimalNumber dn5 = dn1.multiply(2, false);
		assert (dn5.equals(46));
		assert (dn1.equals(23));
	}

	@Test 
	void numberMultiplicationMixedMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(12.4, true);
		DecimalNumber dn2 = new DecimalNumber(6, false);
		DecimalNumber dn3 = dn1.multiply(dn2);
		DecimalNumber dn4 = dn1.multiply(dn1, true);
		assert(dn2.equals(6));
		assert(dn1.doubleValue() == 12.4);
		assert(dn3.doubleValue() == 74.4);
		assert(dn4.equals(153.76));
	}

	@Test
	void numberStaticMultiplication() throws IllegalRangeException{
		assert(DecimalNumber.multiply(2, 3).doubleValue() == 6);
		DecimalNumber dn1 = DecimalNumber.multiply(2, 3);
		DecimalNumber dn2 = DecimalNumber.multiply(-2, 3);

		assert( dn1.equals(6));
		assert (dn2.equals(-6));

		assert (DecimalNumber.multiply(dn1, dn2).equals(-36));
		assert (DecimalNumber.multiply(dn2, dn1).equals(-36));
	}

	@Test
	void numberRangeMutableMultiplication() throws IllegalRangeException, IllegalScaleException	{
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, false);
		try {
			probability.multiply(3);
			fail();
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
		} catch (IllegalScaleException e) {
			////System.out.println("WARNING: Illegal scale exception not expected but found.");
		}
		probability.set(0.5);
		assert(probability.multiply(0.5).equals(0.25));
	}

	@Test
	void numberRangeImmutableMultiplication() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, true);
		try {
			probability.multiply(3);
			fail();
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
			assert(probability.equals(0.5));
		} catch (IllegalScaleException e) {
			assert(probability.equals(0.5));
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Division 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	@Test 
	void numberDivisionMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(8.5, false);
		DecimalNumber dn2 = new DecimalNumber(4.5, false);
		DecimalNumber dn3 = dn2.divide(dn1);
		assert(dn1.equals(8.5));

		assert(dn2 == dn3);
		assert(dn2.equals(0.5294117647, true));
	}

	@Test 
	void numberDivisionImmutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(6, true);
		DecimalNumber dn2 = new DecimalNumber(-1.5, true);
		DecimalNumber dn3 = dn1.divide(dn2);
		assert(dn2.equals(-1.5));
		assert(dn1.equals(6));
		assert(dn3.doubleValue() == -4);

		DecimalNumber dn4 = new DecimalNumber(11.2).divide(5).divide(DecimalNumber.parseStringToBigDecimal("-5"));
		assert (dn4.equals(-0.448));

		DecimalNumber dn5 = dn1.divide(2, false);
		assert (dn5.equals(3));
		assert (dn1.equals(6));
		
		assert (DecimalNumber.ONE.divide(4).equals(0.25));
	}

	@Test 
	void numberDivisionMixedMutable() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn1 = new DecimalNumber(12.4, true);
		DecimalNumber dn2 = new DecimalNumber(6.2, false);
		DecimalNumber dn3 = dn1.divide(dn2);
		DecimalNumber dn4 = dn1.divide(dn1, true);
		assert(dn2.equals(6.2));
		assert(dn1.doubleValue() == 12.4);
		assert(dn3.doubleValue() == 2);
		assert(dn4.equals(1));
	}

	@Test
	void numberStaticDivision() throws IllegalRangeException{
		assert(DecimalNumber.divide(2, 4).doubleValue() == 0.5);
		DecimalNumber dn1 = DecimalNumber.divide(2, 8);
		DecimalNumber dn2 = DecimalNumber.divide(2, -8);

		assert( dn1.equals(0.25));
		assert (dn2.equals(-0.25));

		assert (DecimalNumber.divide(dn1, dn2).equals(-1));
		assert (DecimalNumber.divide(dn2, dn1).equals(-1));
	}

	@Test
	void numberRangeMutableDivision() throws IllegalRangeException, IllegalScaleException	{
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, false);
		try {
			probability.divide(0.25);
			fail();
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
		} catch (IllegalScaleException e) {
			////System.out.println("WARNING: Illegal scale exception not expected but found.");
		}
		probability.set(0.5);
		assert(probability.divide(2).equals(0.25));
	}

	@Test
	void numberRangeImmutableDivision() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber probability = new DecimalNumber(0.5, 0, 1, true);
		try {
			probability.divide(0.25);
			fail();
		} catch (IllegalRangeException e) {
			////System.out.println("Illegal range exception expected and found.");
			assert(probability.equals(0.5));
		} catch (IllegalScaleException e) {
			assert(probability.equals(0.5));
		}
	}


	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Exponentiation 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	@Test 
	void numberPowerMutableInteger() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber base = new DecimalNumber(2);
		assert(base.pow(20, false).equals(1048576));
		assert(base.equals(2));
		
		assert(base.pow(20, true).equals(1048576));
		assert(base.equals(1048576));
	}
	
	@Test 
	void numberPowerImmutableInteger() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber base = new DecimalNumber(2, true);
		assert(base.pow(-5, true).equals(0.03125));
		assert(base.equals(2));
	}
	
	@Test 
	void numberPowerMutableDouble() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber base = new DecimalNumber(2);
		DecimalNumber exponent = new DecimalNumber(20);
		assert(base.pow(exponent.doubleValue(), false).equals(1048576));
		assert(base.equals(2));
		
		assert(base.pow(exponent.doubleValue(), true).equals(1048576));
		assert(base.equals(1048576));
		
		
		base = new DecimalNumber(2);
		exponent = new DecimalNumber(2.5);
		assert(base.pow(exponent.doubleValue(), false).equals(5.6568542495, true));
		assert(base.equals(2));
		
		assert(base.pow(exponent.doubleValue(), true).equals(5.6568542495, true));
		assert(base.equals(5.6568542495, true));
	}
	
	@Test 
	void numberPowerImmutableDouble() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber base = new DecimalNumber(2, true);
		DecimalNumber exponent = new DecimalNumber(20, true);
		assert(base.pow(exponent.doubleValue(), false).equals(1048576));
		assert(base.equals(2));
		
		assert(base.pow(exponent.doubleValue(), true).equals(1048576));
		assert(base.equals(2));
		assert(exponent.equals(20));
		
		base = new DecimalNumber(2);
		exponent = new DecimalNumber(2.5);
		
		assert(base.pow(exponent.doubleValue(), false).equals(5.65685424949238, true));
		assert(base.equals(2));
		assert(exponent.equals(2.5));

		assert(base.pow(exponent.doubleValue(), true).equals(5.65685424949238, true));
		assert(base.equals(5.6568542495, true));
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	  Other		/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	@Test
	void numberNegation() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn = new DecimalNumber(3.1418);
		assert(dn.negate(false).equals(-3.1418));
		assert(dn.negate(true).equals(-3.1418));
		assert(dn.equals(-3.1418));
		assert(dn.negate().equals(3.1418));
		assert(dn.equals(3.1418));
		
		DecimalNumber dn2 = new DecimalNumber(3.14158,true);
		assert(dn2.negate(true).equals(-3.14158));
		assert(dn2.equals(3.14158));
	}
	
	@Test
	void complementOfOne() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn = new DecimalNumber(0.8);
		assert(dn.complementOfOne().equals(0.2));
		assert(dn.equals(0.2));
		assert(dn.complementOfOne(false).equals(0.8));
		assert(dn.equals(0.2));
			
		DecimalNumber dn2 = new DecimalNumber(0.7, true);
		assert(dn2.complementOfOne(true).equals(0.3));
		assert(dn2.equals(0.7));
	}
	
	@Test 
	void numberAbs() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber dn = new DecimalNumber(-42);
		assert(dn.abs().equals(42));
		assert(dn.equals(42));
		assert(dn.abs().equals(42));
		
		DecimalNumber dn2 = new DecimalNumber(-21, true);
		assert (dn2.abs().equals(21));
		assert (dn2.equals(-21));
	}
	
	@Test
	void numberWinsorize() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber lower = new DecimalNumber(-5);
		DecimalNumber higher = new DecimalNumber(5);
		DecimalNumber dn = new DecimalNumber(10);
		assert(dn.winsorize(lower, higher).equals(5));
		assert(dn.equals(5));
		
		dn.set(-10);
		assert(dn.winsorize(lower, higher, false).equals(-5));
		assert (dn.equals(-10));
	}
	

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Speed tests 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	public static int iterations = 100;
	@Test 
	void AAAASpeedBigDecimal(){
		try {
		MathContext mc = DecimalNumber.mc;
		BigDecimal bd = new BigDecimal(2);
		BigDecimal argument = new BigDecimal(1023);
	
		for (int i = 0; i < iterations; i ++)
			bd.add(argument,mc);
		
		for (int i = 0; i < iterations; i ++)
			bd.subtract(argument,mc);
		
		for (int i = 0; i < iterations; i ++)
			bd.multiply(argument,mc);
		
		for (int i = 0; i < iterations; i ++)
			bd.divide(argument,mc);
		}catch(Exception e) { e.printStackTrace();}
	}

	@Test 
	void AAAASpeedDecimalNumber() throws IllegalRangeException, IllegalScaleException{
		try {
		DecimalNumber dn = new DecimalNumber(2);
		DecimalNumber argument = new DecimalNumber(1023);
		for (int i = 0; i < iterations; i ++)
			dn.add(argument, false);
		
		for (int i = 0; i < iterations; i ++)
			dn.subtract(argument, false);
		
		for (int i = 0; i < iterations; i ++)
			dn.multiply(argument, false);
		
		for (int i = 0; i < iterations; i ++)
			dn.divide(argument, false);
		}catch(Exception e) { e.printStackTrace();}
	}
	
	@Test 
	void AAAASpeedDouble() throws IllegalRangeException, IllegalScaleException{
		try {
		double d = 2;
		double argument = 1023;
		for (int i = 0; i < iterations; i ++) {
			double x = d+argument;
		}
		for (int i = 0; i < iterations; i ++) {
			double x = d-argument;
		}
		for (int i = 0; i < iterations; i ++) {
			double x = d*argument;
		}
		for (int i = 0; i < iterations; i ++) {
			double x = d/argument;
		}
		}catch(Exception e) { e.printStackTrace();}
	}

	
	
}
