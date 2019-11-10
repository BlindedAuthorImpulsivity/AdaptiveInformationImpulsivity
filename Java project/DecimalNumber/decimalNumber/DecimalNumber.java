package decimalNumber;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import helper.Helper;

//TODO: check @param
/**
 * DecimalNumber is a class that wraps a single BigDecimal object, and contains some constants.
 * The DecimalNumbers can have a maximum and minimum value attached to them. If a range is specified,
 * the static boolean 'checkValidity' is set to true, and the value of the DecimalNumber 
 * is not between the minimum and maximum, an 'IllegalRangeException is thrown'.
 *  
 * DecimalNumbers are used as a wrapper to combat floating point issues, that kept persisting even after I used
 * BigDecimal objects for computation. Using this wrapper ensures that all BigDecimal values
 * used in the model have the same (limited) scale and rounding mode. In addition, the use
 * of the BigDecimal double constructor (new BigDecimal(double) ) and the BigDecimal double
 * factory (BigDecimal.valueOf(double)) are NO LONGER ALLOWED.
 * 
 * In addition, this class defines the precision (the number of digits in scientific
 * notation) and the scale (the number of decimal values) separately. 
 * 
 * By default, DecimalNumbers are mutable - it is allowed to change values. If, however,
 * the constructor is called with immutable = false or setImmutable(true) is called, 
 * the BigDecimal value contained within the DecimalNumber is not allowed to change - attempting
 * to force a change using set() will result in an "OperationNotSupportedException". 
 * 
 * This class specifies the most common operations (addition, division, multiplication, subtraction, 
 * exponentiation and negation). Each of these operations returns a DecimalNumber with the 
 * new value, making it possible to chain operations. Each operation can be called with
 * operation(Number argument) or with operation(Number argument, boolean changeOriginal). A call
 * to the first function, operation(Number argument), results in a call to the second, with 
 * changeOriginal set to true. That is, the function operation(Number argument) does nothing
 * else then call operation(Number argument, true).
 * 
 * What exact object is returned after each operation depends on whether the DecimalNumber
 * upon which an operation is called is mutable or immutable, and whether changeOrginal is
 * true or false. Three options are possible.
 * 
 *  1) DecimalNumber is mutable, changeOrginal is true.
 *  	In this case an operation changes the value of the DecimalNumber upon which the operation
 *  	is called, and the object returned is the same DecimalNumber.
 *  2) DecimalNumber is mutable, changeOrignal is false.
 *  	In this case the operation results in a new DecimalNumber, which is a copy (in terms of
 *  	range restrictions and mutability) of DecimalNumber upon which the operation is called, 
 *  	of course with a different value. 
 *  3) DecimalNumber is immutable (regardless of changeOriginal).
 *  	In this case the operation will NOT change the DecimalNumber itself. Instead,
 *  	it returns a new MUTABLE DecimalNumber with the same range restrictions as the original.
 *  
 *  As an example, here are the three different cases for addition:
 *  
 *  Case 1: mutable and changeOrginal is true:
 *  a = new DecimalNumber(3);
 * 	b = a.multiply(3); 					// implicitly calls a.multiply(3, true)
 * 	System.out.println(a); 				// Will result in "9" 
 *  System.out.println(b); 				// Will result in "9" 
 *  System.out.println(a == b); 		// Will result in "true" 
 *  
 *  Case 2: mutable and changeOrginal is false:
 *  a = new DecimalNumber(3);
 * 	b = a.multiply(3, false); 				
 * 	System.out.println(a); 				// Will result in "3" 
 *  System.out.println(b); 				// Will result in "9" 
 *  System.out.println(a == b); 		// Will result in "false" 
 *  
 *  Case 3: immutable
 *  a = new DecimalNumber(3, true);
 * 	b = a.multiply(3); 					// implicitly calls a.multiply(3, true)			
 * 	System.out.println(a); 				// Will result in "3" 
 *  System.out.println(b); 				// Will result in "9" 
 *  System.out.println(a == b); 		// Will result in "false" 
 *  System.out.println(b.isImmutable())	// Will result in "false"
 * 
 * Some notes on implementation:
 * 1) DecimalNumbers should be considered as OBJECTS containing a value, not as values per se.
 * Hence, typically the constructor of an object using DecimalNumbers should clone parameters,
 * or calls to the constructor of an object should only use cloned instances (or 
 * risk having a field that is shared with other objects).
 * 
 * 2) To safeguard that values are not changed inadvertently by other objects, a DecimalNumber, a DecimalNumberArray
 * or a DecimalNumberMatrix can be declared immutable by the owning object. The same object can
 * be given a setDecimalNumber() function, which changes the mutability of the DecimalNumber to
 * mutable, changes the value, and resets the mutability to immutable. Although this does not
 * provide complete protection (other objects might change the mutability as well), it at least
 * ensures that changes do not happen by accident.
 *
 */
public class DecimalNumber extends Number implements Comparable<DecimalNumber>, Serializable
{
	private static final long serialVersionUID = Helper.programmeVersion;

	public static final RoundingMode 	roundingMode 	= RoundingMode.HALF_EVEN;
	public static final int				precision 		= 64;		// The total number of digits (using scientific notation) of the DecimalNumber
	public static final int				scale 			= 32;		// The total number of digits after the decimal point. 
	public static final MathContext mc = new MathContext(precision, roundingMode);

	public static final boolean 		checkValidity 		= true;
	public static final boolean			useApproximateValuesWhenCheckingValidity = true;
	public static final DecimalNumber 	boundsOfApproximation = new DecimalNumber("0." + Helper.repString("0", 8) + "1");
	
	@SuppressWarnings("serial")
	public static class IllegalRangeException extends RuntimeException { public IllegalRangeException(String message) {     super(message);  }};
	@SuppressWarnings("serial")
	public static class IllegalScaleException extends RuntimeException { public IllegalScaleException(String message) { 	 super(message);  }};
	/** An exception to throw if a column or row is requested by name, but no column or row with that name exists. */
	@SuppressWarnings("serial")
	public static class NoSuchNameException extends RuntimeException { public NoSuchNameException(String message) { 	 super(message);  }};
	/**
	 * An exception to throw if a computation is impossible (even though the arguments are valid).
	 *
	 */
	@SuppressWarnings("serial")
	public static class ComputationException extends RuntimeException { public ComputationException(String message) {     super(message);  }}
	
	
	
	public static boolean isDouble (String s)
	{
		if (s.length() == 0) return false;
		String doubleRegex = "(-?\\d*)|(-?\\d*\\.\\d+)";
		return (s.matches(doubleRegex));
	}

	public static BigDecimal parseStringToBigDecimal(String doubleString){
		if (!isDouble(doubleString))
			throw new IllegalArgumentException("Trying to parse the string \"" + doubleString + "\" to a double.");
		return new BigDecimal(doubleString, mc).setScale(scale, roundingMode);
	}

	public static BigDecimal parseDoubleToBigDecimal(double value){
		return new BigDecimal("" + value, mc).setScale(scale, roundingMode);
	}



	private 		BigDecimal 		number;
	public  		BigDecimal		minimum;
	public 		 	BigDecimal		maximum;
	private  		boolean			immutable;
	private  		boolean			rangeSpecified;

	public DecimalNumber(BigDecimal value, BigDecimal minimum, BigDecimal maximum, boolean rangeSpecified, boolean immutable) {
		this.number= value;
		this.minimum = minimum;
		this.maximum = maximum;
		this.rangeSpecified = rangeSpecified;
		this.immutable = immutable;

		if (rangeSpecified && checkValidity)
			if (number.compareTo(minimum)==-1 || number.compareTo(maximum)==1)
				throw new IllegalRangeException("Creating a DecimalNumber with a value of " + number.toPlainString() + ", although the minimum and maximum are " + minimum.toPlainString() + " and " + maximum.toPlainString() + ", respectively.");

	}

	/** Returns a mutable copy with the same range limitation (if applicable)*/
	@Override
	public DecimalNumber clone() {
		try {return new DecimalNumber(number, minimum, maximum, rangeSpecified, false);		} catch (IllegalRangeException e) {	e.printStackTrace();	}
		return null;
	}
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	CONSTRUCTORS 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/** Creates a new mutable DecimalNumber without a specified range
	 * 
	 * @param value
	 * @return
	 */
	public DecimalNumber (double value)  {
		this.number = parseDoubleToBigDecimal(value);
		minimum = BigDecimal.ZERO;
		maximum = BigDecimal.ZERO;
		immutable = false;
		rangeSpecified = false;
	}

	/** Creates a new mutable DecimalNumber without a specified range
	 * 
	 * @param value
	 * @return
	 */
	public DecimalNumber(String value)  {
		this.number = parseStringToBigDecimal(value);
		minimum = BigDecimal.ZERO;
		maximum = BigDecimal.ZERO;
		immutable = false;
		rangeSpecified = false;
	}

	/** Creates a new mutable DecimalNumber without a specified range
	 * 
	 * @param value
	 * @return
	 */
	public  DecimalNumber (BigDecimal value)  {
		this.number = value.setScale(scale, roundingMode);
		minimum = BigDecimal.ZERO;
		maximum = BigDecimal.ZERO;
		immutable = false;
		rangeSpecified = false;
	}

	/** Creates a new mutable DecimalNumber without a specified range, with
	 * a value equal to the DecimalNumber argument.
	 * 
	 * @param value
	 * @return
	 */
	public DecimalNumber (DecimalNumber value)  {
		number  =value.getValue();
		minimum = value.minimum;
		maximum = value.maximum;
		immutable = false;
		rangeSpecified = false;
	}


	/** Creates a new DecimalNumber without a specified range and with a specified mutability
	 * 
	 * @param value
	 * @return
	 */
	public DecimalNumber (double value, boolean immutable)  {
		this.number = parseDoubleToBigDecimal(value);
		this.minimum = BigDecimal.ZERO;
		this.maximum = BigDecimal.ZERO;
		this.immutable = immutable;
		this.rangeSpecified = false;
	}

	/** Creates a new DecimalNumber without a specified range and with a specified mutability
	 * 
	 * @param value
	 * @return
	 */
	public  DecimalNumber (String value, boolean immutable)  {
		this.number = parseStringToBigDecimal(value);
		this.minimum = BigDecimal.ZERO;
		this.maximum = BigDecimal.ZERO;
		this.immutable = immutable;
		this.rangeSpecified = false;
	}

	/** Creates a new DecimalNumber without a specified range and with a specified mutability
	 * 
	 * @param value
	 * @return
	 */
	public DecimalNumber (BigDecimal value, boolean immutable)  {
		this.number = value;
		this.minimum = BigDecimal.ZERO;
		this.maximum = BigDecimal.ZERO;
		this.immutable = immutable;
		this.rangeSpecified = false;
	}

	/** Creates a new DecimalNumber without a specified range and with a specified mutability
	 * 
	 * @param value
	 * @return
	 */
	public DecimalNumber (DecimalNumber value, boolean immutable)  {
		this.number = value.getValue();
		this.minimum = BigDecimal.ZERO;
		this.maximum = BigDecimal.ZERO;
		this.immutable = immutable;
		this.rangeSpecified = false;
	}


	/** Creates a DecimalNumber with a specified range and mutability
	 * 
	 * @param value
	 * @param minimum
	 * @param maximum
	 * @return
	 */
	public DecimalNumber(double value, double minimum, double maximum, boolean immutable) {
		this.number = parseDoubleToBigDecimal(value);
		this.minimum = parseDoubleToBigDecimal(minimum);
		this.maximum = parseDoubleToBigDecimal(maximum);
		this.immutable = immutable;
		this.rangeSpecified = true;
	}
	
	/** Creates a DecimalNumber with a specified range and mutability
	 * 
	 * @param value
	 * @param minimum
	 * @param maximum
	 * @return
	 */
	public DecimalNumber(double value, DecimalNumber minimum, DecimalNumber maximum, boolean immutable) {
		this.number = parseDoubleToBigDecimal(value);
		this.minimum = minimum.number;
		this.maximum = maximum.number;
		this.immutable = immutable;
		this.rangeSpecified = true;
	}

	/** Creates a DecimalNumber with a specified range and mutability
	 * 
	 * @param value
	 * @param minimum
	 * @param maximum
	 * @return
	 */
	public DecimalNumber (String value, String minimum, String maximum, boolean immutable) {
		this.number = parseStringToBigDecimal(value);
		this.minimum = parseStringToBigDecimal(minimum);
		this.maximum = parseStringToBigDecimal(maximum);
		this.immutable = immutable;
		this.rangeSpecified = true;
	}

	/** Creates a DecimalNumber with a specified range and mutability
	 * 
	 * @param value
	 * @param minimum
	 * @param maximum
	 * @return
	 */
	public DecimalNumber (BigDecimal value, BigDecimal minimum, BigDecimal maximum, boolean immutable) {
		this.number = value.setScale(scale, roundingMode);
		this.minimum = minimum.setScale(scale, roundingMode);
		this.maximum = maximum.setScale(scale, roundingMode);
		this.immutable = immutable;
		this.rangeSpecified = true;
	}

	/** Creates a DecimalNumber with a specified range and mutability
	 * 
	 * @param value
	 * @param minimum
	 * @param maximum
	 * @return
	 */
	public DecimalNumber (DecimalNumber value, DecimalNumber minimum, DecimalNumber maximum, boolean immutable) {
		this.number = value.getValue();
		this.minimum = minimum.number;
		this.maximum = maximum.number;
		this.immutable = immutable;
		this.rangeSpecified = true;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Constants 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an immutable DecimalNumber with value 0 and without a specified range.
	 * @return
	 */
	private static DecimalNumber ZERO() {
		try {
			return new DecimalNumber(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, true);
		} catch (IllegalRangeException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static final DecimalNumber ZERO = ZERO();

	/**
	 * Creates a immutable DecimalNumber with value 1 and without a specified range.
	 * @return
	 */
	private static DecimalNumber ONE() {
		try {
			return new DecimalNumber(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, false, true);
		} catch (IllegalRangeException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static final DecimalNumber ONE = ONE();

	/**
	 * Creates a immutable DecimalNumber with value -1 and without a specified range.
	 * @return
	 */
	private static DecimalNumber NEGATIVE_ONE() {
		try {
			return new DecimalNumber(new BigDecimal("-1"), BigDecimal.ONE, BigDecimal.ONE, false, true);
		} catch (IllegalRangeException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static final DecimalNumber NEGATIVE_ONE = NEGATIVE_ONE();

	/**
	 * Creates a immutable DecimalNumber with very negative value and without a specified range.
	 * @return
	 */
	private static DecimalNumber NEGATIVE_INFINITY() {
		try {
			return new DecimalNumber(new BigDecimal("-999999999999999"), BigDecimal.ONE, BigDecimal.ONE, false, true);
		} catch (IllegalRangeException e) {
			e.printStackTrace(); 
			return null;
		}
	}
	public static final DecimalNumber NEGATIVE_INFINITY = NEGATIVE_INFINITY();

	/**
	 * Creates a immutable DecimalNumber with very high, positive value and without a specified range.
	 * @return
	 */
	private static DecimalNumber POSITIVE_INFINITY() {
		try {
			return new DecimalNumber(new BigDecimal("999999999999999"), BigDecimal.ONE, BigDecimal.ONE, false, true);
		} catch (IllegalRangeException e) {
			e.printStackTrace(); 
			return null;
		}
	}
	public static final DecimalNumber POSITIVE_INFINITY = POSITIVE_INFINITY();

	public static final DecimalNumber NaN = new DecimalNumber(0, true);
	public static final DecimalNumber NULL = new DecimalNumber(0, true);
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	SETTERS 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Sets the BigDecimal value contained in this DecimalNumber to the argument. If this DecimalNumber
	 * is immutable, a UnsupportedOperationException is thrown. If the new value is not in the range of
	 * values permissible, a IllegalRangeException is thrown. If the new BigDecimal has a scale that is
	 * not allowed, an IllegalScaleException is thrown.
	 * @param argument
	 * @return
	 */

	public DecimalNumber set(double argument) {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumber.set(): trying to set an immutable DecimalNumber.");
		number  = parseDoubleToBigDecimal(argument);
		checkValidity();
		return this;

	}

	/**
	 * Sets the BigDecimal value contained in this DecimalNumber to the argument. If this DecimalNumber
	 * is immutable, a UnsupportedOperationException is thrown. If the new value is not in the range of
	 * values permissible, a IllegalRangeException is thrown. If the new BigDecimal has a scale that is
	 * not allowed, an IllegalScaleException is thrown.
	 * @param argument
	 * @return 
	 */
	public DecimalNumber set(String argument) {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumber.set(): trying to set an immutable DecimalNumber.");
		number  = parseStringToBigDecimal(argument);
		checkValidity();
		return this;
	}

	/**
	 * Sets the BigDecimal value contained in this DecimalNumber to the argument. If this DecimalNumber
	 * is immutable, a UnsupportedOperationException is thrown. If the new value is not in the range of
	 * values permissible, a IllegalRangeException is thrown. If the new BigDecimal has a scale that is
	 * not allowed, an IllegalScaleException is thrown.
	 * @param argument
	 * @return
	 */
	public DecimalNumber set(BigDecimal argument)  {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumber.set(): trying to set an immutable DecimalNumber.");
		number  = argument.setScale(scale, roundingMode);
		checkValidity();
		return this;

	}

	/**
	 * Sets the BigDecimal value contained in this DecimalNumber to the argument. If this DecimalNumber
	 * is immutable, a UnsupportedOperationException is thrown. If the new value is not in the range of
	 * values permissible, a IllegalRangeException is thrown. If the new BigDecimal has a scale that is
	 * not allowed, an IllegalScaleException is thrown.
	 * @param argument
	 * @return
	 */
	public DecimalNumber set(DecimalNumber argument)  {
		if (immutable)
			throw new UnsupportedOperationException("Exception in DecimalNumber.set(): trying to set an immutable DecimalNumber.");
		number  =argument.getValue();
		checkValidity();
		return this;

	}
	/**
	 * Called after every operation. 
	 * Tests whether the BigDecimal number is within the specified range (if specified), 
	 * whether the scale of the number does not exceed the scale specified by DecimalNumber.scale,
	 * 
	 * If DecimalNumber.checkRange is set to false, no test is executed.
	 * 
	 */
	private void checkValidity() {
		if (!checkValidity ) return;
		if (number.scale() > scale)
			throw new IllegalScaleException("Check failed: the scale of a Decimal exceeded the scale specified. The scale was " + number.scale() + ", while the maximum was " + scale);

		if (rangeSpecified && (number.compareTo(minimum)==-1 || number.compareTo(maximum)==1)) {
			if (!useApproximateValuesWhenCheckingValidity)
				throw new IllegalRangeException("Check failed: ended up with a DecimalNumber with a value of " + number.toPlainString() + ", although the minimum and maximum are " + minimum.toPlainString() + " and " + maximum.toPlainString() + ", respectively.");
			else if (!this.equals(minimum, true) && !this.equals(maximum, true) )
				throw new IllegalRangeException("Check failed: ended up with a DecimalNumber with a value of " + number.toPlainString() + ", although the minimum and maximum are " + minimum.toPlainString() + " and " + maximum.toPlainString() + ", respectively.");
		}
	}

	public BigDecimal getValue() {
		return number;
	}
	/** Sets the mutability. Returns this */
	public DecimalNumber setImmutable(boolean immutability) {
		this.immutable = immutability;
		return this;
	}
	/** Sets the range of this DecimalNumber and returns this. If the range is violated at any moment, an IllegalRangeException will be thrown.	Returns null if the original DecimalNumber is set to immutable */
	public DecimalNumber setRange(double minimum, double maximum) {
		if (immutable)
			return null;
		
		rangeSpecified = true;
		setRangeMinimum(minimum);
		setRangeMaximum(maximum);
		return this;
	}
	
	/** Sets the range of this DecimalNumber and returns this. If the range is violated at any moment, an IllegalRangeException will be thrown.	Returns null if the original DecimalNumber is set to immutable */
	public DecimalNumber setRange(DecimalNumber minimum, DecimalNumber maximum) {
		if (immutable)
			return null;
		
		rangeSpecified = true;
		this.minimum = minimum.getValue(); 
		this.maximum = maximum.getValue();
		return this;
	}
	
	/** Sets the minimum value of the range. Does not set whether a range has been specified. Returns null if the original DecimalNumber is set to immutable. Otherwise, returns this */
	public DecimalNumber setRangeMinimum	(double minimum){
		if (immutable) return null;
		this.minimum = parseDoubleToBigDecimal(minimum);
		return this;
	}
	/** Sets the minimum value of the range. Does not set whether a range has been specified. Returns null if the original DecimalNumber is set to immutable. Otherwise, returns this */
	public DecimalNumber setRangeMaximum (double maximum){
		if (immutable) return null;
		this.maximum = parseDoubleToBigDecimal(maximum);
		return this;
	}
	/** Should the range be specified for this decimalNumber? Returns this */
	public DecimalNumber setHasRangeSpecified(boolean hasRangeSpecified) {
		this.rangeSpecified = hasRangeSpecified;
		return this;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	GETTERS 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	public boolean isImmutable() { return this.immutable; }
	public boolean hasSpecifiedRange() { return this.rangeSpecified; }
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Comparisons 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	

	@Override
	public int compareTo(DecimalNumber o) {
		if (this == DecimalNumber.NEGATIVE_INFINITY || o == DecimalNumber.POSITIVE_INFINITY)
			return -1;
		if (this == DecimalNumber.POSITIVE_INFINITY || o == DecimalNumber.NEGATIVE_INFINITY)
			return 1;
		
		return number.compareTo(o.getValue());
	}

	public int compareTo(double d) {
		if (number.doubleValue()>d) return 1;
		if (number.doubleValue()<d) return -1;
		return 0;
	}
	/**
	 * Compares the values of this and o, independent of scale.
	 * 
	 * If approximately is true, values close to (within a margin of 0.00000001) are
	 * considered equal. This function is useful with values might have rounding issues
	 * (e.g., when the values are derived from R), or when there are (possible) floating
	 * point issues.
	 * @param o
	 * @return
	 */
	public boolean equals(DecimalNumber o, boolean approximately) {
		if (!approximately) 
			return number.compareTo(o.getValue())==0;
		
		DecimalNumber referencePoint = o.clone();
		referencePoint.setHasRangeSpecified(false);
		if (inRange(referencePoint.subtract(boundsOfApproximation, false), referencePoint.add(boundsOfApproximation, false)))	
			return true;
		return false;
		
	}
	
	/**
	 * Compares the values of this and o, independent of scale.
	 * 
	 * If approximately is true, values close to (within a margin of 0.00000001) are
	 * considered equal. This function is useful with values might have rounding issues
	 * (e.g., when the values are derived from R), or when there are (possible) floating
	 * point issues.
	 * @param o
	 * @return
	 */
	public boolean equals(double o, boolean approximately) {
		return equals(new DecimalNumber(o), approximately);
	}

	/**
	 * Compares the values of this and o, independent of scale.
	 * 
	 * If approximately is true, values close to (within a margin of 0.00000001) are
	 * considered equal. This function is useful with values might have rounding issues
	 * (e.g., when the values are derived from R), or when there are (possible) floating
	 * point issues.
	 * @param o
	 * @return
	 */
	public boolean equals(BigDecimal o, boolean approximately) {
		return equals(new DecimalNumber(o), approximately);
	}

	public boolean equals(double o) {
		return equals(o, false);
	}
	
	public boolean equals(String s) {
		return this.equals(new DecimalNumber(s), false);
	}
	
	public boolean equals(BigDecimal bd) {
		return this.equals(bd, false);
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof DecimalNumber)
			return this.equals((DecimalNumber) o, false);
		return false;

	}

	/** Returns true if the DecimalNumber's value is in the range [lowerBound, upperBound] */
	public boolean inRange(double lowerBound, double upperBound) {
		return (number.doubleValue()>=lowerBound && number.doubleValue() <= upperBound);
	}
	

	/** Returns true if the DecimalNumber's value is in the range [lowerBound, upperBound] */
	public boolean inRange(DecimalNumber lowerBound, DecimalNumber upperBound) {
		return (number.doubleValue()>=lowerBound.doubleValue() && number.doubleValue() <= upperBound.doubleValue());
	}
	
	/** Returns true if the DecimalNumber's value is in the range [lowerBound, upperBound] */
	public boolean inRange(BigDecimal lowerBound, BigDecimal upperBound) {
		return (number.compareTo(lowerBound)>=0 && number.compareTo(upperBound)<=0);
	}
	
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	ADDITION 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function adds the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber + argument). In this case the number contained within this DecimalNumber is not changed. 
	 * In this case, the new DecimalNumber is mutable.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber add(BigDecimal argument, boolean changeOriginal) {
		BigDecimal newNumber =  number.add(argument, mc).setScale(scale, roundingMode);

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/**
	 * If the DecimalNumber is mutable, this function adds the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber + argument). In this case, the new DecimalNumber is mutable.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber add(BigDecimal argument)  {
		return add(argument, true); 
	}

	/**
	 * If the DecimalNumber is mutable, this function adds the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber + argument). In this case, the new DecimalNumber is mutable.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return 
	 */
	public DecimalNumber add(DecimalNumber argument) {
		return add(argument.getValue(), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function adds the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber + argument). In this case the number contained within this DecimalNumber is not changed.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber add(DecimalNumber argument, boolean changeOriginal) {
		return add(argument.getValue(), changeOriginal);
	}
	/**
	 * If the DecimalNumber is mutable, this function adds the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber + argument). In this case, the new DecimalNumber is mutable.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber add(double argument) {
		return add(parseDoubleToBigDecimal(argument), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function adds the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber + argument). In this case the number contained within this DecimalNumber is not changed.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber add(double argument, boolean changeOriginal)  {
		return add(parseDoubleToBigDecimal(argument), changeOriginal);
	}


	/**
	 * Adds the value of the two DecimalNumbers and returns a new mutable DecimalNumber. The range of
	 * the resulting DecimalNumber is unspecified.
	 * @param dn1
	 * @param dn2
	 * @return
	 */
	public static DecimalNumber add(DecimalNumber dn1, DecimalNumber dn2){
		return new DecimalNumber(dn1.add(dn2, false));
	}

	/**
	 * Adds the two double values and returns a DecimalNumber with that value. The DecimalNumber
	 * has a fixed scale and precision.
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static DecimalNumber add(double d1, double d2)  {
		DecimalNumber dn1 = new DecimalNumber(d1);
		DecimalNumber dn2 = new DecimalNumber(d2);
		return dn1.add(dn2);
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	SUBTRACTION 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function subtracts the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber - argument). In this case the number contained within this DecimalNumber is not changed. 
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber subtract(BigDecimal argument, boolean changeOriginal)  {
		BigDecimal newNumber =  number.subtract(argument, mc).setScale(scale, roundingMode);

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/**
	 * If the DecimalNumber is mutable, this function subtracts the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber - argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber subtract(BigDecimal argument)  {
		return subtract(argument, true); 
	}

	/**
	 * If the DecimalNumber is mutable, this function subtracts the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber - argument). 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber subtract(DecimalNumber argument)  {
		return subtract(argument.getValue(), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function subtracts the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber - argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber subtract(DecimalNumber argument, boolean changeOriginal)  {
		return subtract(argument.getValue(), changeOriginal);
	}

	/**
	 * If the DecimalNumber is mutable, this function subtracts the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber - argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber subtract(double argument)  {
		return subtract(parseDoubleToBigDecimal(argument), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function subtracts the argument to the BigDecimal
	 * contained within the DecimalNumber, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber - argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber subtract(double argument, boolean changeOriginal)  {
		return subtract(parseDoubleToBigDecimal(argument), changeOriginal);
	}

	/**
	 * Subtracts the value of the two DecimalNumbers and returns a new mutable DecimalNumber. The range of
	 * the resulting DecimalNumber is unspecified.
	 * @param dn1
	 * @param dn2
	 * @return
	 */
	public static DecimalNumber subtract(DecimalNumber dn1, DecimalNumber dn2) {
		return dn1.subtract(dn2, false);
	}

	/**
	 * Subtracts the two double values and returns a DecimalNumber with that value. The DecimalNumber
	 * has a fixed scale and precision.
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static DecimalNumber subtract(double d1, double d2)  {
		DecimalNumber dn1 = new DecimalNumber(d1);
		DecimalNumber dn2 = new DecimalNumber(d2);
		return dn1.subtract(dn2);
	}


	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	MULTIPLICATION 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function multiplies the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber * argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber multiply(BigDecimal argument, boolean changeOriginal) {
		BigDecimal newNumber =  number.multiply(argument, mc).setScale(scale, roundingMode);

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/**
	 * If the DecimalNumber is mutable, this function multiplies the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber * argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber multiply(BigDecimal argument)  {
		return multiply(argument, true); 
	}

	/**
	 * If the DecimalNumber is mutable, this function multiplies the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber * argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber multiply(DecimalNumber argument) {
		return multiply(argument.getValue(), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function multiplies the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber * argument). In this case the number contained within this DecimalNumber is not changed.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber multiply(DecimalNumber argument, boolean changeOriginal) {
		return multiply(argument.getValue(), changeOriginal);
	}

	/**
	 * If the DecimalNumber is mutable, this function multiplies the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber * argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return 
	 */
	public DecimalNumber multiply(double argument) {
		return multiply(parseDoubleToBigDecimal(argument), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function multiplies the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber * argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber multiply(double argument, boolean changeOriginal) {
		return multiply(parseDoubleToBigDecimal(argument), changeOriginal);
	}

	/**
	 * Multiplies the value of the two DecimalNumbers and returns a new mutable DecimalNumber. The range of
	 * the resulting DecimalNumber is unspecified.
	 * @param dn1
	 * @param dn2
	 * @return
	 */
	public static DecimalNumber multiply(DecimalNumber dn1, DecimalNumber dn2)  {
		return dn1.multiply(dn2, false);
	}

	/**
	 * Multiplies the two double values and returns a DecimalNumber with that value. The DecimalNumber
	 * has a fixed scale and precision.
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static DecimalNumber multiply(double d1, double d2)  {
		DecimalNumber dn1 = new DecimalNumber(d1);
		DecimalNumber dn2 = new DecimalNumber(d2);
		return dn1.multiply(dn2);
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	DIVISION 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function divides the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber / argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 */
	public DecimalNumber divide(BigDecimal argument, boolean changeOriginal)  {
		BigDecimal newNumber =  number.divide(argument, mc).setScale(scale, roundingMode);

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/**
	 * If the DecimalNumber is mutable, this function divides the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber / argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber divide(BigDecimal argument)  {
		return divide(argument, true); 
	}

	/**
	 * If the DecimalNumber is mutable, this function divides the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber / argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber divide(DecimalNumber argument) {
		return divide(argument.getValue(), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function divides the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber / argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber divide(DecimalNumber argument, boolean changeOriginal)   {
		return divide(argument.getValue(), changeOriginal);
	}

	/**
	 * If the DecimalNumber is mutable, this function divides the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber / argument). Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber divide(double argument)   {
		return divide(parseDoubleToBigDecimal(argument), true); 
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function divides the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber / argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber divide(double argument, boolean changeOriginal)   {
		return divide(parseDoubleToBigDecimal(argument), changeOriginal);
	}

	/**
	 * Divides the value of the two DecimalNumbers and returns a new mutable DecimalNumber. The range of
	 * the resulting DecimalNumber is unspecified.
	 * @param dn1
	 * @param dn2
	 * @return
	 */
	public static DecimalNumber divide(DecimalNumber dn1, DecimalNumber dn2)  {
		return dn1.divide(dn2, false);
	}

	/**
	 * Divides the two double values and returns a DecimalNumber with that value. The DecimalNumber
	 * has a fixed scale and precision.
	 * @param d1
	 * @param d2
	 * @return
	 */
	public static DecimalNumber divide(double d1, double d2)  {
		DecimalNumber dn1 = new DecimalNumber(d1);
		DecimalNumber dn2 = new DecimalNumber(d2);
		return dn1.divide(dn2);
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	POWER 	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function exponentiates the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber ^ argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber pow(int n, boolean changeOriginal)   {
		BigDecimal newNumber = number.pow(n, mc).setScale(scale, roundingMode);

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}
	
	 /** If the DecimalNumber is mutable, this function exponentiates the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable , a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber ^ argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber pow(int n)   {
		return pow(n, true);
	}


	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function exponentiates the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber ^ argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber pow(double exponent, boolean changeOriginal)   {
		double newValue = Math.pow(number.doubleValue(), exponent);
		BigDecimal newNumber = new BigDecimal(newValue, mc).setScale(scale, roundingMode);

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}


	/**
	 * If the DecimalNumber is mutable, this function exponentiates the BigDecimal
	 * contained within the DecimalNumber with the argument, and returns this DecimalNumber.
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (old DecimalNumber ^ argument). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * 
	 * This function is equivalent to calling pow(double exponent, true);
	 * @param argument
	 * @return
	 */
	public DecimalNumber pow(double exponent)   {
		return pow(exponent, true);
	}



	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Other 	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function negates the BigDecimal
	 * contained within the DecimalNumber (i.e., -1*value), and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (-1*value). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber negate(boolean changeOriginal)   {
		BigDecimal newNumber = number.negate(mc).setScale(scale, roundingMode);		

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}
	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function negates the BigDecimal
	 * contained within the DecimalNumber (i.e., -1*value), and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (-1*value). In this case the number contained within this DecimalNumber is not changed.
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber negate()   {
		return this.negate(true);
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function sets the value of this DecimalNumber to the complement of 1 of
	 *  the BigDecimal contained within the DecimalNumber (i.e., 1-value), and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (1-value). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber complementOfOne(boolean changeOriginal)   {
		BigDecimal newNumber = BigDecimal.ONE.subtract(number, mc).setScale(scale, roundingMode);		

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function return the complement of 1 of
	 *  the BigDecimal contained within the DecimalNumber (i.e., 1-value), and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (1-value). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber complementOfOne()   {
		return this.complementOfOne(true);
	}

	/** If the DecimalNumber is mutable and changeOrginal is true, this function sets the absolute value of
	 *  the BigDecimal contained within the DecimalNumber (i.e., 1-value), and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * (1-value). In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber abs(boolean changeOriginal)   {
		BigDecimal newNumber = number.abs();		

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/** Returns a new DecimalNumber with value (this.number.remainder(other.number)) -- that is, returns a sort-of-modulo that can be negative */
	public DecimalNumber remainder(DecimalNumber other) {
		return new DecimalNumber(this.number.remainder(other.number));
	}
	
	public boolean isDivisibleBy(DecimalNumber other) {
		DecimalNumber remainder = this.remainder(other);
		if (remainder.compareTo(0)==0)
			return true;
		return false;
	}
	
	/** If the DecimalNumber is mutable this function sets the DecimalNumber's value to the absolute value (i.e., 
	 * it removes a "-" if it contains a minus), and returns the DecimalNumber. 
	 * 
	 * If the DecimalNumber is immutable, a new DecimalNumber is created. This new DecimalNumber
	 * has the same range (if specified) as the original, and a value equal to 
	 * the absoltue value. In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber abs()   {
		return this.abs(true);
	}



	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function winsorizes the
	 *  BigDecimal contained within the DecimalNumber , and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same value range (if specified) as the original. In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber winsorize(DecimalNumber lowerBound, DecimalNumber upperBound, boolean changeOriginal)  
	{
		BigDecimal newNumber;
		if 			(number.compareTo(lowerBound.getValue())==-1) 	newNumber = lowerBound.getValue();
		else if 	(number.compareTo(upperBound.getValue())==1)  	newNumber = upperBound.getValue();
		else 														newNumber = number;

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function winsorizes the
	 *  BigDecimal contained within the DecimalNumber , and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same value range (if specified) as the original. In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber winsorize(DecimalNumber lowerBound, DecimalNumber upperBound)  
	{
		return this.winsorize(lowerBound, upperBound, true);
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function rounds the 
	 *  the BigDecimal contained within the DecimalNumber, and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same rounded value and same range (if specified) as the original. 
	 * In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber round(int digits, RoundingMode roundingMode, boolean changeOriginal)  
	{
		BigDecimal newNumber = number.setScale(digits, roundingMode);

		// If the DecimalNumber is mutable, and we want to change the DecimalNumber itself:
		if (changeOriginal && !immutable) {
			number = newNumber;
			checkValidity();
			return this;
		}

		// If the DecimalNumber is immutable or if we do not want to change the DecimalNumber itself:
		return new DecimalNumber(newNumber, minimum, maximum, rangeSpecified, false);
	}

	/**
	 * If the DecimalNumber is mutable and changeOrginal is true, this function rounds the 
	 *  the BigDecimal contained within the DecimalNumber, and returns this DecimalNumber 
	 * 
	 * If the DecimalNumber is immutable or changeOriginal is false, a new DecimalNumber is created. This new DecimalNumber
	 * has the same rounded value and same range (if specified) as the original. 
	 * In this case the number contained within this DecimalNumber is not changed.
	 * Note that the new DecimalNumber is mutable. 
	 * 
	 * In both cases, if the result is not within the range (if specified) a
	 * IllegalRangeException is thrown.
	 * 
	 * If the scale of the result exceeds the scale specified by DecimalNumber, an
	 * IllegalScaleException is thrown.
	 * 
	 * The resulting value has a precision AND scale as defined in the DecimalNumber class.
	 * @param argument
	 * @return
	 */
	public DecimalNumber round(int digits, RoundingMode roundingMode)  
	{
		return this.round(digits, roundingMode, true);
	}

	public static DecimalNumber max(DecimalNumber... decimalNumbers ) {
		DecimalNumber max = decimalNumbers[0];
		for (DecimalNumber dn: decimalNumbers)
			if (dn.compareTo(max) ==1)
				max = dn;
		return max;
			
	}
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	toString 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	public String toString() {
		if (this == DecimalNumber.NaN)
			return "NaN";
		if (this == DecimalNumber.NULL)
			return "NULL";
		if (this == DecimalNumber.NEGATIVE_INFINITY)
			return "-Infinity";
		if (this == DecimalNumber.POSITIVE_INFINITY)
			return "Infinity";
		
		String s = number.toPlainString();
		if (rangeSpecified) s = s + "'";
		if (immutable) s = s + "*";
		return s;
	}
	
	/** Rounds to significant digits and returns corresponding string. Does not influence value of DecimalNumber object. Does not show affixes. */
	public String toString(int significantDigits) {
		if (this == DecimalNumber.NaN)
			return "NaN";
		if (this == DecimalNumber.NULL)
			return "NULL";
		if (this == DecimalNumber.NEGATIVE_INFINITY)
			return "-Infinity";
		if (this == DecimalNumber.POSITIVE_INFINITY)
			return "Infinity";
		
		DecimalFormat df = new DecimalFormat("0." + Helper.repString("0", significantDigits));
		String s = df.format(number);
		return s;
	}
	
	/**Rounds to significant digits and returns corresponding string that reserves a white space for positive values. Does not influence value of DecimalNumber object. Does not show affixes.  */
	public String toSignSpacedString(int significantDigits)
	{
		String s = this.toString(significantDigits);
		if (this.compareTo(0) != -1) s = " " + s;
		return s;
	}
	
	/** Returns a string with the value of this decimal number. Does not display '*' for immutable values or "'" for values with a specified range */
	public String toPlainString() {
		if (this == DecimalNumber.NaN)
			return "NaN";
		if (this == DecimalNumber.NULL)
			return "NULL";
		if (this == DecimalNumber.NEGATIVE_INFINITY)
			return "-Infinity";
		if (this == DecimalNumber.POSITIVE_INFINITY)
			return "Infinity";
		
		return number.toPlainString();
	}

	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Interface Number 	//////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	@Override
	public int intValue() {
		return number.intValue();
	}

	@Override
	public long longValue() {
		return number.longValue();
	}

	@Override
	public float floatValue() {
		return number.floatValue();
	}

	@Override
	public double doubleValue() {
		return number.doubleValue();
	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Hash 	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		//result = prime * result + (immutable ? 1231 : 1237);
		//result = prime * result + ((maximum == null) ? 0 : maximum.hashCode());
		//result = prime * result + ((minimum == null) ? 0 : minimum.hashCode());
		result = prime * result + ((number == null) ? 0 : number.hashCode());
		//result = prime * result + (rangeSpecified ? 1231 : 1237);
		return result;
	}

}
