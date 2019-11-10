package decimalNumber;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Iterator;

import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import helper.Helper;

/**
 * A wrapper that holds an array of DecimalNumbers, and defines several
 * often-used computations for such an array.
 */
public class DecimalNumberArray implements Serializable, Cloneable, Iterable<DecimalNumber>{

	private static final long serialVersionUID = Helper.programmeVersion;
	public DecimalNumber[] array;
	private int length;

	private boolean immutable = false;
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	CONSTRUCTORS 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	public DecimalNumberArray(DecimalNumber... numbers) { this.array = numbers; this.length = array.length; }
	
	/** Creates a DecimalNumberArray, consisting of mutable DecimalNumbers with the values specified and without a range limitation
	 * 
	 * @param numbers The double values to be converted to mutable DecimalNumbers
	 */
	public DecimalNumberArray(double... numbers) { 
		this.length = numbers.length;
		this.array = new DecimalNumber[length];
		for (int i = 0; i < length; i ++)
			array[i] = new DecimalNumber(numbers[i],false); }
	
	/** Creates a DecimalNumberArray, consisting of mutable DecimalNumbers with the values, range and immutability specified
	 * 
	 * @param numbers The double values to be converted to mutable DecimalNumbers
	 */
	public DecimalNumberArray(double minimum, double maximum, boolean immutable, double... numbers) { 
		this.length = numbers.length;
		this.array = new DecimalNumber[length];
		for (int i = 0; i < length; i ++)
			array[i] = new DecimalNumber(numbers[i], minimum, maximum, false); }

	/** Creates a DecimalNumberArray, consisting of mutable DecimalNumbers with the values specified and without a range limitation
	 * 
	 * @param numbers The BigDecimal values to be converted to mutable DecimalNumbers
	 */
	public DecimalNumberArray(BigDecimal... numbers) { 
		this.length = numbers.length;
		this.array = new DecimalNumber[length];
		for (int i = 0; i < length; i ++)
			array[i] = new DecimalNumber(numbers[i],false); }

	/** Creates a DecimalNumberArray of size n. The DecimalNumbers in this array are all non-initialized (i.e., null)
	 * 
	 * @param n
	 */
	public DecimalNumberArray(int n) {
		this.array = new DecimalNumber[n];
		this.length=n;
	}

	/** Creates an array of length length with values between min and max.*/
	public static DecimalNumberArray randomArray(int length, double min, double max, boolean integerOnly ) {
		double[] values = new double[length];
		for (int i = 0; i < length; i++) {
			values[i] = Math.random()*(max-min) + min;
			if (integerOnly) values[i] = Math.round(values[i]);
		}
		return new DecimalNumberArray(values);
	}
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////// 	Elementary operations 	/////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	public double[] toDoubleArray() {
		double[] doubleArray = new double[length];
		for (int i = 0; i < length; i ++)
			doubleArray[i] = array[i].doubleValue();
		return doubleArray;
	}
	
	public int[] toIntegerArray() {
		int[] doubleArray = new int[length];
		for (int i = 0; i < length; i ++)
			doubleArray[i] = array[i].intValue();
		return doubleArray;
	}
	
	public boolean isImmutable() { return this.immutable; }
	
	public int size() {
		return array.length;
	}

	public int length() {
		return this.length; 
	}
	
	public DecimalNumber[] array() { return this.array; }
	
	public DecimalNumber get(int index) { return array[index]; }
	
	/** Set the element at the index position of the array. If the index is larger than the length of the array, a false is returned. Otherwise a true is returned. If the array is declared immutable (default is false), this function returns false*/
	public boolean set(int index, DecimalNumber element) 	{ 
		if (immutable) return false;
		if (index> length ) 
			return false; array[index] = element; 
		return true;
	}

	/** Set the element at the index position of the array. If the index is larger than the length of the array, a false is returned. Otherwise a true is returned. If the array is declared immutable (default is false), this function returns false*/
	public boolean set (int index, double element) 	{ 
		if (immutable) return false;
		return this.set(index, new DecimalNumber(element));
	}
	
	/** Set all DecimalNumbers in the array to the specified value. If the DecimalNumbers in the array are immutable, these entries are not changed and a false is returned.
	 * Note: even in this case, ALL mutable values will be changed. If the DecimalNumber is null, a new DecimalNumber is created.
	 * 
	 * If the array is declared immutable (default is false), this function returns false
	  */
	public boolean setAll(double newValue){
		if (immutable) return false;
		boolean noImmutable = true;
		for (int i = 0; i <array.length; i++) {
			if (array[i] == null)
				array[i] = new DecimalNumber(newValue);
			else if (array[i].isImmutable())
				noImmutable = false;
			else
				array[i].set(newValue);
		}
		
		return noImmutable;
	}
	
	/** Replaces the values of the DecimalNumbers contained in this array, without
	 * changing the references. Returns false if the length of the newValues does
	 * not match the length of the array.
	 * @param newValues
	 * @return
	 */
	public boolean setAll(DecimalNumberArray newValues)   {
		if (length != newValues.length)
			return false;
		
		for (int i = 0; i < length; i ++)
			array[i].set(newValues.get(i));
		
		return true;
	}

	public boolean containsNull() {
		for (DecimalNumber dn: array)
			if (dn == null)
				return true;
		return false;
	}

	public boolean contains(DecimalNumber element, boolean approximately) {
		for (DecimalNumber dn: array)
			if (dn.equals(element, approximately))
				return true;
		return false;
	}

	public boolean contains(double element, boolean approximately) {
		for (DecimalNumber dn: array)
			if (dn.equals(element, approximately)) 
				return true;
		return false;
	}

	/**
	 * Returns the index of the element in the array. If the element is not in the array, a -1 is returned.
	 * If the element is at multiple positions, the first position is returned.
	 * @param element
	 * @return
	 */
	public int indexOf(DecimalNumber element){
		for (int i = 0; i < length; i++)
			if (array[i].equals(element))
				return i;
		return -1;

	}

	/**
	 * Returns the index of the element in the array with a value equal to the element specified. If the element is not in the array, a -1 is returned.
	 * If the element is at multiple positions, the first position is returned.
	 * @param element
	 * @return
	 */
	public int indexOf(BigDecimal element){
		for (int i = 0; i < length; i++)
			if (array[i].equals(element))
				return i;
		return -1;
	}
	
	/** Returns the index of the element in the array with a value equal to the element specified. If the element is not in the array, a -1 is returned.
	 * If the element is at multiple positions, the first position is returned.
	 * @param element
	 * @return
	 */
	public int indexOf(double element){
		for (int i = 0; i < length; i++)
			if (array[i].equals(element))
				return i;
		return -1;
	}

	/** Two arrays are equivalent if and only if each index position houses DecimalNumbers with the same vale.
	 * 
	 * @param other
	 * @return
	 */
	public boolean equals(DecimalNumberArray other) {
		if (length != other.length)
			return false;
		
		for (int i = 0; i < length; i++)
			if (!array[i].equals(other.get(i)))
				return false;
		return true;
	}
	
	/** Returns a string of this array. This string starts with [, and ends with ]. Values are delimited with a tab.
	 * Note that DecimalNumbers that are immutable have a "*" placed at the end (e.g., an immutable 3 is "3.0000*").
	 * DecimalNumbers with a specified range are affixed with a "'". To prevent these affixes, use toPlainString().
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#." + Helper.repString("#", DecimalNumber.scale));
		sb.append("[");
		
		for (int i = 0; i < array.length; i ++) {
			if (array[i]==null)
				sb.append("NULL");
			else {
				if (array[i] == DecimalNumber.NaN) sb.append("NaN");
				else if (array[i] == DecimalNumber.NULL) sb.append("Null");
				else if (array[i] == DecimalNumber.NEGATIVE_INFINITY) sb.append("-Inf");
				else if (array[i] == DecimalNumber.POSITIVE_INFINITY) sb.append("Inf");
				else
					sb.append(df.format(array[i].doubleValue()));
				
				if (array[i].isImmutable()) 
					sb.append("*");
			}
			if (i != array.length-1)
				sb.append("   ");
			
		}
		sb.append("]");
		if (immutable) sb.append("*");
		return sb.toString();
				
	}
	
	/** Returns a string of this array. This string starts with [, and ends with ]. Values are delimited with a tab.
	 * Does not show "*" for immutable DecimalNumbers, nor does it show "'" for DecimalNumbers with a specified range.
	 */
	public String toPlainString()
	{
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("#." + Helper.repString("#", DecimalNumber.scale));
		sb.append("[");
		for (int i = 0; i < array.length; i ++) {
			if (array[i]==null)
				sb.append("NULL");
			else
				sb.append(array[i].toPlainString());
		}
		sb.append("]");
		if (immutable) sb.append("*");
		return sb.toString();
				
	}
	
	/** Displays the array without any rounding. */
	public String toExactString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < array.length; i ++) {
			if (array[i]==null)
				sb.append("NULL");
			else
				sb.append(array[i].doubleValue());
			if (array[i].isImmutable()) 
				sb.append("*");
			if (i != array.length-1)
				sb.append("\t");
		}
		sb.append("]");
		if (immutable) sb.append("*");
		return sb.toString();
				
	}


	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////// 	Boolean properties of array ////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/** Returns true if, and only if, all values in the array are positive and sum to 1 (or sum to 1 approximately). */
	public boolean isProbability() {
		if (this.min().compareTo(0)==-1)
			return false;
		if (!this.sum().equals(1, true))
			return false;
		return true;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////// 	Operations on this array 	////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/** Transform the array such that it sums to one, while retaining the relative proportions of each element to each other element.
	 * Specifically, this function divides each element by the sum of all elements. If the sum is zero, a uniform distribution is set.
	 * 
	 * If the array contains negative values, an UnsupportedOperation exception is thrown.
	 * 
	 * If the underlying decimal number array is contains DecimalNumbers that are immutable, these DecimalNumbers are replaced. 
	 * If the new values of the DecimalNumbers are not in the range specified
	 * for those DecimalNumbers, an IllegalRangeException is thrown. If the values are of an incorrect scale, an IllegalScaleException is returned.
	 * 
	 * If the total sum is zero, a uniform distribution with values of 1/n is set, where n is the number of DecimalNumbers in the array.
	 * 
	 * This function does not do anything is the array is immutable (default is that it is not, but immutability can be set by setImmutable()).
	 */
	public void toProbability()   {
		if (immutable) return ;
		if (min().compareTo(0)==-1)
			throw new UnsupportedOperationException("Error at DecimalNumberArray.toProbability(): trying to turn an array containing negative values into a probability distribution");
		DecimalNumber sum = this.sum();
		
		// if sum ~ 0: create uniform distribution
		if (sum.equals(0, true)) {
			double uniform = 1.0/length;
			for (int i = 0 ; i < array.length; i ++)
				array[i].set(uniform);
		}
		
		else
			for (int i = 0 ; i < array.length; i ++)
				array[i] = array[i].divide(sum);
	}
	
	/** multiply all values in this array with the scalar. This function does not do anything if the array is immutable (default is that it is not, but immutability can be set by setImmutable()). Returns this*/
	public DecimalNumberArray scale(DecimalNumber scalar)   {
		if (immutable) return this;
		for (int i = 0; i < length; i ++)
			array[i] = array[i].multiply(scalar);
		return this;
	}
	
	/** multiply all values in this array with the scalar. This function does not do anything if the array is immutable (default is that it is not, but immutability can be set by setImmutable()). Returns this.*/
	public DecimalNumberArray scale(double scalar)   {
		if (immutable) return this;
		for (int i = 0; i < length; i ++)
			array[i] = array[i].multiply(scalar);
		return this;
	}
	
	/** Transforms the array sum that all values sum to one, while maintaining the proportions. This function does nothing is the array is set to immutable. If the array sums to zero, a uniform distribution is set. 
	 * returns this.
	 */
	public DecimalNumberArray scaleToSumToOne()   {
		if (immutable) return this;
		DecimalNumber sum = this.sum();
		if (sum.equals(0, false)) {
			DecimalNumber uniform = DecimalNumber.ONE.divide(length);
			for (DecimalNumber dn: array)
				dn.set(uniform);
			return this;
		}
		
		for (DecimalNumber dn: array)
			dn.set(dn.divide(sum));
		return this;
	}
	
	public DecimalNumber max()
	{
		DecimalNumber max = array[0];
		for (DecimalNumber n: array)
			if (n.compareTo(max)==1)
				max = n;
		return max;
	}

	public DecimalNumber min()
	{
		DecimalNumber min = array[0];
		for (DecimalNumber n: array)
			if (n.compareTo(min)==-1)
				min = n;
		return min;
	}

	/**
	 * Returns a concatenation of all BigDecimals using R's notation (i.e., c(BigDecimal1, BigDecimal2..., etc)
	 * @param array
	 * @return
	 */
	public String concatenateRStyle ()
	{
		DecimalFormat df = new DecimalFormat("0.0000000000");
		StringBuilder sb = new StringBuilder();
		sb.append("c(");
		for (int n = 0; n<array.length; n++)
		{
			String number = df.format(array[n].doubleValue());
			number = number.replace(",", ".");
			sb.append(number);
			if (n != array.length-1)
				sb.append(",");
		}
		sb.append(")");
		return sb.toString();
	}

	/** Determine sum of array. Infinite, NaN and Null values are ignored */
	public DecimalNumber sum()
	{
		DecimalNumber sum = new DecimalNumber(0);
		for (DecimalNumber dn:array)
			if (!dn.equals(DecimalNumber.NEGATIVE_INFINITY) && !dn.equals(DecimalNumber.POSITIVE_INFINITY) && !dn.equals(DecimalNumber.NULL) && !dn.equals(DecimalNumber.NaN))
				sum.add(dn);
		return sum;
	}
	
	/** Determine number of valid numbers in array. Infinite, NaN and Null values are ignored */
	public int validN() {
		int i = 0;
		for (DecimalNumber dn: array){
			if (dn != DecimalNumber.NEGATIVE_INFINITY && dn != DecimalNumber.POSITIVE_INFINITY && dn != DecimalNumber.NULL && dn != DecimalNumber.NaN && dn != null)
				i++;
		}
		return i;
	}

	/** Determine mean of array. Infinite, NaN and Null values are ignored */
	public DecimalNumber mean()   {
		return sum().divide(validN());
	}
	
	/** Determine variance of array. Infinite, NaN and Null values are ignored */
	public DecimalNumber variance()   {
		int validN = validN();
		if (validN <2)
			throw new IllegalStateException("Exception in DecimalNumberArray.variance(): trying to compute variance with less than 2 valid values (Nvalid = " + validN +". Array: " + this);
		
		DecimalNumber variance = new DecimalNumber(0, DecimalNumber.ZERO, DecimalNumber.POSITIVE_INFINITY, false);
		DecimalNumber mean = mean().setImmutable(true);

		for (DecimalNumber dn: array) 
			if (!dn.equals(DecimalNumber.NEGATIVE_INFINITY) && !dn.equals(DecimalNumber.POSITIVE_INFINITY) && !dn.equals(DecimalNumber.NULL) && !dn.equals(DecimalNumber.NaN))
				variance.add(dn.subtract(mean, false).abs().pow(2, true));
		return variance.divide(validN-1);
	}
	
	/** Determine standard deviation of array. Infinite, NaN and Null values are ignored */
	public DecimalNumber standardDeviation()   {
		return variance().pow(0.5, true);
	}
	
	/** Inserts a decimalNumber into the array at the specified position. Returns false if the index is not in the array. This function returns false if the array is immutable (default is that it is not, but immutability can be set by setImmutable()).*/
	public boolean insert(int index, DecimalNumber number) {
		if (immutable) return false;
		if (index < 0 || index > length)
			return false;
		
		DecimalNumber[] newArray = new DecimalNumber[length+1];
		System.arraycopy(array, 0, newArray, 0, index);
		System.arraycopy(array, index, newArray, index + 1, array.length - index );
		newArray[index] = number;
		array = newArray;
		length = array.length;
		return true;
	}
	
	/** Removes the DecimalNumber at the index position. Since this operation involves copying an array of DecimalNumberArrays, it might be an inefficient operation. Returns false if and only if the index does not exist. This function returns false if the array is immutable (default is that it is not, but immutability can be set by setImmutable()*/
	public boolean remove(int index) {
		if (immutable)
			return false;
		
		if (index < 0 || index > length-1)
			return false;

		DecimalNumber[] newArray = new DecimalNumber[array.length-1];		
		System.arraycopy(array,0,newArray,0,index);
		if (index != array.length)
			System.arraycopy(array, index+1, newArray, index, array.length-index-1);
		this.array = newArray;
		
		length = array.length;
		return true;
	}
	
	/** Sets the immutability of the array and all decimal numbers in this array. Returns this
	 * @return */
	public DecimalNumberArray setImmutable(boolean immutability) {
		this.immutable = immutability;
		for(DecimalNumber dn: array)
			dn.setImmutable(immutability);
		return this;
	}
	
	/** Sets the immutability of this array only. The DecimalNumber's contained within are not changed. */
	public void setImmutableArrayOnly(boolean immutability) {
		this.immutable = immutability;
	}
	
	/** Set this array to the array specified. Returns false is this array is immutable. */
	public boolean replaceAll(DecimalNumberArray newArray) {
		if (immutable)
			return false;
		
		this.array = newArray.array;
		this.length = newArray.length;
		return true;
	}
	
	/** Set the range of all mutable DecimalNumbers in this array to the specified range.
	 * Returns true if at least one immutable DecimalNumber is encountered (note, execution
	 * does not stop after hitting upon an immutable DecimalNumber - all mutable DecimalNumbers
	 * in this array are changed).
	 * @param minium
	 * @param maximum
	 * @return
	 */
	public boolean setAllRanges(double minimum, double maximum) {
		boolean encounteredImmutable = false;
		for (DecimalNumber dn: array) {
			if (dn.setRange(minimum, maximum) == null) 
				encounteredImmutable = true;
		}
		
		return encounteredImmutable;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	///////////////////////		Vector multiplication	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	/** Returns the dot product of both arrays. 
	 * Specifically, returns sum( this[i] * otherArray[i] ) for all i in [0, this.length]. 
	 * Returns UnsupportedOperationException if arrays are of unequal length 
	 */ 
	public DecimalNumber dotProduct(DecimalNumberArray otherArray)   {
		if (this.length != otherArray.length)
			throw new IllegalArgumentException("Dotproduct: trying to get dot product of arrays of unequal size.");
		
		DecimalNumber sum = new DecimalNumber(0);
		for (int i = 0; i < length; i ++)
			sum.add(array[i].multiply(otherArray.get(i), false));
		
		return sum;
	}
	
	/**
	 * Returns the dot product (AKA inner product) of both arrays. Formally, returns:
	 * sum( this[i] * otherArray[i] ) for all i in [0, this.length]. 
	 * 
	 * Throws a UnsupportedOperationException if the arrays are unequal in length.
	 * @param array1
	 * @param array2
	 * @return
	 */
	public static DecimalNumber dotProduct(DecimalNumberArray array1, DecimalNumberArray array2)   {
		if (array1.length != array2.length)
			throw new IllegalArgumentException("Dotproduct: trying to get dot product of arrays of unequal size.");
		
		DecimalNumber sum = new DecimalNumber(0);
		for (int i = 0; i < array1.length(); i ++)
			sum.add(array1.get(i).multiply(array2.get(i), false));
		
		return sum;
	}
	
	
	
	
	/////////////////////////////////////////////////////////////////////////////////////
	////////////// 	Other operations resulting in a new array 	/////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/** Returns a unity vector (a row vector containing only 1's) of the specified length */
	public static DecimalNumberArray unityVector(int length) {
		return DecimalNumberArray.rep(1, length);
	}
	
	/** Create a DEEP clone of the DecimalNumberArray. Note that the resulting array,
	 * and all clones within this array are SET TO MUTABLE, regardless of the mutability or
	 * immutability of the original.
	 * 
	 */
	public DecimalNumberArray clone() {
		DecimalNumber[] newArray = new DecimalNumber[length];
		for (int i = 0; i<length; i++) newArray[i] = new DecimalNumber(array[i]);
		
		DecimalNumberArray clone = new DecimalNumberArray(newArray);
		clone.setImmutable(false);
		return clone;
	}
	
	/** Returns a shallow clone of the original. That is, all
	 * objects in the clone reference the same objects in the 
	 * original - changes in one lead to changes in the other.
	 * 
	 * Note that the new array is mutable, even if the original is
	 * not. This does not hold for the DecimalNumbers contained within:
	 * because the copy references the same objects, they might still
	 * be immutable.
	 * @return
	 */
	public DecimalNumberArray shallowClone() {
		DecimalNumberArray clone = new DecimalNumberArray(array);
		clone.setImmutableArrayOnly(false);
		return clone;
	}

	/**
	 * Results in an array containing the sequence from <from> (inclusive) to <to> (exclusive),
	 *  increasing with step size <step>.
	 * @param from
	 * @param to
	 * @param step
	 * @return
	 */
	public static DecimalNumberArray sequence(DecimalNumber from, DecimalNumber to, DecimalNumber step)  
	{		
		int indices = to.subtract(from, false).divide(step, false).intValue()+1;

		DecimalNumber[] sequence = new DecimalNumber[indices];
		for (int i = 0; i < indices; i++) {
			sequence[i] = from.add(step.multiply(i, false), false).setImmutable(false);
		}

		return new DecimalNumberArray(sequence);
	}

	 /** Results in an array containing the sequence from <from> (inclusive) to <to> (exclusive),
	 *  increasing with step size <step>.
	 * @param from
	 * @param to
	 * @param step
	 * @return 
	 */
	public static DecimalNumberArray sequence(double from, double to, double step)  
	{
		int indices = (int) ( (to-from)/(step)) + 1;

		DecimalNumber[] sequence = new DecimalNumber[indices];
		for (int i = 0; i < indices; i++)
			sequence[i] = new DecimalNumber(from + (step*i));

		return new DecimalNumberArray(sequence);
	}

	/**
	 * Results in an array containing n clones of the value.
	 * @param from
	 * @param to
	 * @param step
	 * @return
	 */
	public static DecimalNumberArray rep(DecimalNumber value, int n)
	{
		DecimalNumber[] sequence = new DecimalNumber[n];
		for (int i = 0; i < n; i++)
			sequence[i] = value.clone();

		return new DecimalNumberArray(sequence);
	}

	/**
	 * Results in an array containing n clones of the value.
	 * @param from
	 * @param to
	 * @param step
	 * @return
	 */
	public static DecimalNumberArray rep(double value, int n)
	{
		DecimalNumber[] sequence = new DecimalNumber[n];
		for (int i = 0; i < n; i++)
			sequence[i] = new DecimalNumber(value);

		return new DecimalNumberArray(sequence);
	}

	/**
	 * Given an array, returns a subset of that array starting from start (inclusive) to stop (exclusive).
	 * Note that this is just a cheap wrapper function of Arrays.copyOfRange, created to increase code
	 * readability
	 * @return
	 */
	public DecimalNumberArray subset(int from, int to)
	{
		return new DecimalNumberArray(Arrays.copyOfRange(array, from, to));
	}

	/** Returns a new DecimalNumberArray that is the product of this array and the other array. That is, returns
	 * a new array with values result[i] = this[i] * other[i], for all i.  If the arrays
	 * are of unequal length, a null is returned.
	 * @param other
	 * @return
	 */
	public DecimalNumberArray multiply(DecimalNumberArray other)   {
		if (length != other.length) return null;
		
		DecimalNumberArray product = DecimalNumberArray.rep(0, length);
		for (int i = 0; i < product.length; i++)
			product.set(i, array[i].multiply(other.array[i], false));
		
		return product;
	}
	
	/**
	 * Returns a new DecimalNumberArray with the concatenated elements of both arrays.
	 * @param array1
	 * @param array2
	 * @return
	 */
	public DecimalNumberArray concatenate(DecimalNumberArray otherArray)
	{
		int newLength = array.length + otherArray.length;
		DecimalNumber[] newArray = Arrays.copyOfRange(array, 0, newLength);
	
		for (int i=0; i < otherArray.length; i++) 
			newArray[this.length+i] = otherArray.get(i);
		return new DecimalNumberArray(newArray);
	}
	
	/** Returns a copy of the BigDecimal numbers in this DecimalNumberArray. Changes in the resulting array do not result in changes in the DecimalNumberArray.
	 * 
	 * @return
	 */
	public BigDecimal[] toBigDecimalArray() {
		BigDecimal[] bdArray = new BigDecimal[length];
		for (int i = 0; i < length; i ++)
			bdArray[i] = array[i].getValue();
		return bdArray;
	}

	/** Returns a copy of the BigDecimal numbers in this DecimalNumberArray. Changes in the resulting array do not result in changes in the DecimalNumberArray.
	 * 
	 * @param scale
	 * @return
	 */
	public BigDecimal[] toBigDecimalArray(int scale) {
		BigDecimal[] bdArray = new BigDecimal[length];
		for ( int i = 0; i < length; i++)
			bdArray[i] = array[i].getValue().setScale(scale, DecimalNumber.roundingMode);
		return bdArray;
	}

	@Override
	public Iterator<DecimalNumber> iterator() {
		return new Iterator<DecimalNumber>() {
			int currentIndex = 0;
			
			@Override
			public boolean hasNext() {
				return currentIndex < length && array[currentIndex] != null;
			}

			@Override
			public DecimalNumber next() {
				return array[currentIndex++];
			}
			
		};
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
		result = prime * result + (immutable ? 1231 : 1237);
		result = prime * result + length;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */

	

}
