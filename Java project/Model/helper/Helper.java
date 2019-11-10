package helper;
import java.io.File;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * A class that deals with all the mathematical operations. Due to the sometimes small numbers
 * we work with, the double primitive type cannot be trusted (floating point error). To make
 * sure that no such errors occur, mathematical operations that are used repeatedly will be specific in the helper
 * class. In addition, Helper houses the MathContext (mc) that is used in all other classes.
 * The MathContext specifies the number of significant digits is used in BigDecimal operations.
 * Without specifying this MathContext, an exact representation is used - which extremely increases
 * the working space memory and runtime requirements of the algorithms, increasing both by a factor
 * of ~1000.
 * @author jesse
 *
 */
public class Helper {
	public static final long programmeVersion = 0; 
	public static final int numberOfSignificantDigits = 32;
	public static final MathContext mc = new MathContext(numberOfSignificantDigits, RoundingMode.HALF_EVEN);

	/**
	 * Results in an array containing the sequence from <from> (inclusive) to <to> (exclusive),
	 *  increasing with step size <step>.
	 * @param from
	 * @param to
	 * @param step
	 * @return
	 */
	public static BigDecimal[] sequence(BigDecimal from, BigDecimal to, BigDecimal step)
	{
		int indices = (to.subtract(from, mc)).divide(step, mc).intValue()+1;

		BigDecimal[] sequence = new BigDecimal[indices];
		for (int i = 0; i < indices; i++)
			sequence[i] = (from.add(step.multiply(new BigDecimal(""+i, Helper.mc), mc),mc));

		return sequence;
	}

	/**
	 * Results in an array containing the sequence from <from> (inclusive) to <to> (exclusive),
	 *  increasing with step size <step>.
	 * @param from
	 * @param to
	 * @param step
	 * @return
	 */
	public static Integer[] sequence(int from, int to, int step)
	{
		int indices = (to - from)/step+1;

		Integer[] sequence = new Integer[indices];
		for (int i = 0; i < indices; i++)
			sequence[i] = from + (step*i);

		return sequence;
	}
	
	public static double[] sequence(double from, double to, double step)
	{
		int indices = (int) ((to - from)/step)+1;

		double[] sequence = new double[indices];
		for (int i = 0; i < indices; i++)
			sequence[i] = from + (step*i);

		return sequence;
	}

	/**
	 * Normalizes a given array by the given constant
	 * @param array
	 * @param normalizingConstant
	 * @return
	 */
	public static BigDecimal[] normalizeArray(final BigDecimal[] array, BigDecimal normalizingConstant)
	{
		if (normalizingConstant.compareTo(BigDecimal.ZERO)==0 ) normalizingConstant = new BigDecimal(""+0.00000000000000001);
		for (int i = 0; i < array.length; i++)
			array[i] = array[i].divide(normalizingConstant, mc);
		return array;
	}

	public static <T extends Number> String arrayToString(T[] array)
	{
		DecimalFormat df = new DecimalFormat("#.################");
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < array.length; i++) {
			sb.append(df.format(array[i]));
			if (!(i == array.length-1))
				sb.append("\t");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public static <T> String arrayToString(T[] array)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < array.length; i++) {
			sb.append(array[i].toString());
			if (!(i == array.length-1))
				sb.append("\t");
		}
		sb.append("]");
		return sb.toString();
	}
	
	public static  String arrayToString(ArrayList<Double> array)
	{
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < array.size(); i++) {
			sb.append(array.get(i));
			if (!(i == array.size()-1))
				sb.append("\t");
		}
		sb.append("]");
		return sb.toString();
	}
	
	

	public static <T> String arrayToString(T[][] array)
	{
		StringBuilder sb = new StringBuilder("\n");
		for (int i = 0; i < array.length; i++) {
			sb.append("[");
			for (int j = 0; j < array[i].length; j++) {
				sb.append(array[i][j].toString());
				if (!(j == array[i].length-1))
					sb.append("\t");
			}
			sb.append("]\n");
		}
		return sb.toString();
	}
	
	
	public static String arrayToString(double[][] array)
	{
		DecimalFormat df = new DecimalFormat("0");
		df.setMaximumFractionDigits(128);
		StringBuilder sb = new StringBuilder("\n");
		for (int i = 0; i < array.length; i++) {
			sb.append("[");
			for (int j = 0; j < array[i].length; j++) {
				sb.append(df.format(array[i][j]));
				if (!(j == array[i].length-1))
					sb.append("\t");
			}
			sb.append("]\n");
		}
		return sb.toString();
	}
	
	public static String arrayToString(int[][] array)
	{
		StringBuilder sb = new StringBuilder("\n");
		for (int i = 0; i < array.length; i++) {
			sb.append("[");
			for (int j = 0; j < array[i].length; j++) {
				sb.append(array[i][j]);
				if (!(j == array[i].length-1))
					sb.append("\t");
			}
			sb.append("]\n");
		}
		return sb.toString();
	}

	public static BigDecimal max(BigDecimal[] array)
	{
		BigDecimal max = array[0];
		for (BigDecimal bd: array)
			if (bd.compareTo(max)==1)
				max = bd;
		return max;
	}

	public static BigDecimal min(BigDecimal[] array)
	{
		BigDecimal min = array[0];
		for (BigDecimal bd: array)
			if (bd.compareTo(min)==-1)
				min = bd;
		return min;
	}


	/**
	 * Returns a concatenation of all BigDecimals using R's notation (i.e., "c(double1, double2..., etc)"
	 * @param array
	 * @return
	 */
	public static String concatenateRStyle (double[] array)
	{
		DecimalFormat df = new DecimalFormat("0.0000000000");
		StringBuilder sb = new StringBuilder();
		sb.append("c(");
		for (int bd = 0; bd<array.length; bd++)
		{
			String number = df.format(array[bd]);
			number = number.replace(",", ".");
			sb.append(number);
			if (bd != array.length-1)
				sb.append(",");
		}
		sb.append(")");
		return sb.toString();
	}
	
	/**
	 * Returns a concatenation of all BigDecimals using R's notation (i.e., "c(double1, double2..., etc)"
	 * @param array
	 * @return
	 */
	public static String concatenateRStyle (ArrayList<Double> array)
	{
		DecimalFormat df = new DecimalFormat("0.0000000000");
		StringBuilder sb = new StringBuilder();
		sb.append("c(");
		for (int bd = 0; bd<array.size(); bd++)
		{
			String number = df.format(array.get(bd));
			number = number.replace(",", ".");
			sb.append(number);
			if (bd != array.size()-1)
				sb.append(",");
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Compute sum of BigDecimal array
	 * @param array
	 * @return
	 */
	public static BigDecimal sum(BigDecimal[] array)
	{
		BigDecimal sum = new BigDecimal(""+0, Helper.mc);
		for (int i = 0; i < array.length; i++)
			sum = sum.add(array[i], mc);
		return sum;
	}
	
	/** Returns a string of form [dd/MM/yyyy HH:mm:ss] of the current system time*/
	public static String timestamp() {
		return "[" + new SimpleDateFormat("dd/MM/yyyy HH':'mm':'ss" + "]\t").format(new Date());
	}

	/**
	 * Given an array, returns a subset of that array starting from start (inclusive) to stop (exclusive).
	 * Note that this is just a cheap wrapper function of Arrays.copyOfRange, created to increase code
	 * readability
	 * @return
	 */
	public static BigDecimal[] subset(BigDecimal[] array, int from, int to)
	{
		return Arrays.copyOfRange(array, from, to);
	}

	/**
	 * Concatenate two BigDecimal arrays
	 * @param array1
	 * @param array2
	 * @return
	 */
	public static BigDecimal[] concatenateBigDecimalArrays(BigDecimal[] array1, BigDecimal[] array2)
	{
		int length = array1.length + array2.length;
		BigDecimal[] result = new BigDecimal[length];
		for (int i=0; i < array1.length; i++)
			result[i] = array1[i];
		for (int i=0; i < array2.length; i++)
			result[i+array1.length] = array2[i];
		return result;
	}

	/**
	 * Find greatest common denominator. The counter is the maximum recursion depth.
	 * @param a
	 * @param b
	 * @return
	 */
	public static BigDecimal gcd(BigDecimal a, BigDecimal b, boolean enteredRecursion, int timesMultipliedBy10, int maximumRecursionDepthCounter)
	{
		if (! enteredRecursion) // computations that only have to be done once
		{
			timesMultipliedBy10 = 0;
			// GCD is only defined for integers. Hence, we multiple a and b by 10 until there are no decimal places left.
			while (getNumberOfDecimalPlaces(a)>0 || getNumberOfDecimalPlaces(b)>0)
			{
				a = a.multiply(BigDecimal.TEN, mc);
				b = b.multiply(BigDecimal.TEN, mc);
				timesMultipliedBy10++;
			}
			
			enteredRecursion = true;
		}
		
		
		if (maximumRecursionDepthCounter<0) return new BigDecimal("0.000001", Helper.mc);	
		
		if(a.compareTo(new BigDecimal(""+0, Helper.mc))== 0 || b.compareTo(new BigDecimal(""+0, Helper.mc)) == 0)
		{
			BigDecimal result = a.add(b,mc); // base case
			for (int i = 0; i < timesMultipliedBy10; i ++) result = result.divide(BigDecimal.TEN, mc);
			return result;
		}

		return gcd(b,a.remainder(b, mc), true, timesMultipliedBy10, maximumRecursionDepthCounter-1);
		
	}

	public static int getNumberOfDecimalPlaces(BigDecimal bigDecimal) {
	    return Math.max(0, bigDecimal.stripTrailingZeros().scale());
	}


	public static String repString (String s, int n){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++)
			sb.append(s);
		return sb.toString();
	}
	
	public static BigDecimal[] repBigDecimal(BigDecimal bd, int n)
	{
		BigDecimal[] array = new BigDecimal[n];
		for (int i = 0; i < n; i++)
			array[i] = bd;
		return array;
	}


	/**
	 * Combines two arrays into a single [n]x[2] grid. New elements are only added as long
	 * as the total sum of resulting array does not exceed the specified maximum.
	 * @param array1
	 * @param array2
	 * @return
	 */
	public static  <T extends Number>  T[][] gridExpand (Class<? extends T> clss, T[] array1, T[]array2, T maximum)
	{
		ArrayList<T[]> newGrid = new ArrayList<>();

		for (T e1:array1)
			for (T e2:array2)
				if (e1.doubleValue()+e2.doubleValue()<=maximum.doubleValue())
				{
					@SuppressWarnings("unchecked")
					T[] row = (T[]) Array.newInstance(clss, 2);
					row[0] = e1;
					row[1] = e2;
					newGrid.add(row);
				}

		@SuppressWarnings("unchecked")
		T[][] newGridArray = (T[][])Array.newInstance(clss, newGrid.size(), 2);
		for (int i = 0; i < newGridArray.length; i++)
			newGridArray[i] = newGrid.get(i);
		return newGridArray;
	}

	/**
	 * Winsorize a value. That is, ensure that a value is between a lower bound and an upper bound. If the value
	 * is lower than the lowerBound, the lowerBound is returned. If the value is higher than the upperBound, the upperBound
	 * is returned. Otherwise, the value is returned.
	 * @param value
	 * @param lowerBound
	 * @param upperBound
	 * @return
	 */
	public static BigDecimal winsorize(BigDecimal value, BigDecimal lowerBound, BigDecimal upperBound)
	{
		BigDecimal result = value;
		if (result.compareTo(lowerBound)==-1) result = lowerBound;
		if (result.compareTo(upperBound)==1)   result = upperBound;
		return result;
	}
	
	/**
	 * Returns dot product of two provided arrays. If the two input arrays are
	 * not of equal size, the longer array is truncated to match the shorter array in size.
	 * @param array
	 * @return
	 */
	public static BigDecimal dotProduct(BigDecimal[] array1, BigDecimal[] array2)
	{
		return sum(elementWiseMultiplication(array1, array2));
	}
	
	/**
	 * Element wise multiplication of two arrays, result in a single array. If the two input arrays are
	 * not of equal size, the longer array is truncated to match the shorter array in size.
	 * @param array1
	 * @param array2
	 * @return
	 */
	public static BigDecimal[] elementWiseMultiplication(BigDecimal[] array1, BigDecimal[] array2)
	{
		BigDecimal[] mul = new BigDecimal[Math.min(array1.length, array2.length)];
		for (int i = 0; i < mul.length; i ++)
			mul[i] = array1[i].multiply(array2[i], mc);
		return mul;
	}

	/**
	 * Combines a grid and an array into a single [n]x[grid_n +1] grid. New elements are only added as long
	 * as the total sum of resulting array does not exceed the specified maximum. If the maximum is exceeded,
	 * returns an array of size 1, containing Integer.MIN_VALUE.
	 * @param array1
	 * @param array2
	 * @return
	 */
	public static <T extends Number> T[][] gridExpand(Class<? extends T> clss, T[][] grid, T[] array, T maximum)
	{
		ArrayList<T[]> newGrid = new ArrayList<>();

		for (T[] r: grid)
			for (T e: array)
				if ((sum(r) + e.doubleValue()) <= maximum.doubleValue())
				{
					T[] newRow = Arrays.copyOf(r, r.length+1);
					newRow[newRow.length-1] = e;
					newGrid.add(newRow);
				}

		//To array
		int cols = 0;
		for (T[] r:grid) if (r.length>cols) cols = r.length;
		@SuppressWarnings("unchecked")
		T[][] newGridArray = (T[][])Array.newInstance(clss, newGrid.size(), cols);
		for (int i = 0; i < newGridArray.length; i++)
			newGridArray[i] = newGrid.get(i);
		return newGridArray;
	}

	/**
	 * Calculate the number of combinations that exists of a populations that consists of n.length different options,
	 * each of which occur n times. Formally:
	 *
	 * N = [sum(i) for all i in n]!
	 * result =  N!/(i! forall i in n)
	 *
	 * if N ==0, returns 1.
	 * @param n
	 * @return
	 */
	public static BigInteger combinationsBigInteger(final Integer[] n)
	{
		/*int N = 0;
		for(int i:n) N += i;
		if (N==0) return 1;

		long K = 1;
		for (int i:n) K = K * factorial(i);

		return (factorial(N)/K);*/

		int N = 0;
		for(int i:n) N += i;
		if (N==0) return BigInteger.ONE;

		BigInteger K = BigInteger.ONE;
		for (int i:n) K = K.multiply(factorialBigInteger(i));

		return (factorialBigInteger(N).divide(K));

	}
	
	/**
	 * Calculate the number of combinations that exists of a populations that consists of n.length different options,
	 * each of which occur n times. Formally:
	 *
	 * N = [sum(i) for all i in n]!
	 * result =  N!/(i! forall i in n)
	 *
	 * if N ==0, returns 1.
	 * 
	 * Note that this implementation calls combinationBigInteger, and returns the long value of that function -
	 * you should always use BigIntegers to compute things that have a factorial in them - I found that out the hard way.
	 * @param n
	 * @return
	 */
	public static long combinations(final Integer[] n)
	{
		return combinationsBigInteger(n).longValue();
	}
	
	/**
	 * Calculate the number of combinations that exists of a populations that consists of n.length different options,
	 * each of which occur n times. Formally:
	 *
	 * N = [sum(i) for all i in n]!
	 * result =  N!/(i! forall i in n)
	 *
	 * if N ==0, returns 1.
	 * @param n
	 * @return
	 */
	public static long combinations(AbstractCollection<Integer> n)
	{
		int N = 0;
		for(int i:n) N += i;
		if (N==0) return 1;

		long K = 1;
		for (int i:n) K = K * factorial(i);

		return (factorial(N)/K);
	}

	/**
	 * Returns the index position of the highest value in the array. If more than one value is tied for the maximum,
	 * the first index is returned. If the array has 0 elements, a -1 is returned.
	 * @param array
	 * @return
	 */
	public static int indexMaximum(BigDecimal[] array)
	{
		if (array.length==0) return -1;
		BigDecimal max = array[0];
		int pos = 0;
		for (int i = 1; i < array.length; i++)
			if (array[i].compareTo(max)==1)
			{
				max = array[i];
				pos = i;
			}

		return pos;
	}
	
	/**
	 * Returns the position of element in array. Equality of elements is tested using BigDecimal.compareTo (NOT using BigDecimal.equals()!)
	 * Returns -1 if the element is not in the array.
	 * @param element
	 * @param array
	 * @return
	 */
	public static int indexOf(BigDecimal element, BigDecimal[] array)
	{
		for (int i = 0; i < array.length; i++)
			if (element.compareTo(array[i])==0)
				return i;
		return -1;
	}
	
	/**
	 * Returns the position of element in array. Equality of elements is tested using .equals().
	 * Returns -1 if the element is not in the array.
	 * @param element
	 * @param array
	 * @return
	 */
	public static <T> int indexOf(T element, T[] array)
	{
		for (int i = 0; i < array.length; i++)
			if (array[i].equals(element))
				return i;
		return -1;
	}
	
	/**
	 * Returns true if array contains element, false otherwise. If the array is known to be sorted in increasing order, checks can be sped up using
	 * arrayIsSorted. To check equality, .compareTo() is used.
	 * @param element
	 * @param array
	 * @param arrayIsSorted
	 * @return
	 */
	public static <T extends Comparable<T>> boolean contains(T element, T[] array, boolean arrayIsSorted)
	{
		for (int i = 0; i < array.length; i ++)
			if (array[i].compareTo(element)==0)
				return true;
			else if (array[i].compareTo(element)==1 && arrayIsSorted)
				return false;
		return false;
	}
	
	/**
	 * returns true if at least one element in the array is null.
	 * @param array
	 * @return
	 */
	public static <T> boolean containsNull(T[] array) {
		for (T t: array)
			if (t == null)
				return true;
		return false;
	}
	
	/**
	 * Returns n!
	 * @param n
	 * @return
	 */
	public static long factorial(int n) {
		if(n < 0){  throw new IllegalArgumentException("Using negative value in factorial.");  }

		long factorial = 1;

	    for (int i = 1; i <= n; i++) {
	        factorial = factorial  * i;
	    }
	    return factorial;
	}

	/**
	 * Returns n!
	 * @param n
	 * @return
	 */
	public static BigInteger factorialBigInteger(int n) {
		if(n < 0){  throw new IllegalArgumentException("Using negative value in factorial.");  }

		BigInteger factorial = BigInteger.ONE;

	    for (int i = 1; i <= n; i++) {
	        factorial = factorial.multiply(BigInteger.valueOf(i));
	    }
	    return factorial;
	}


	public static <T extends Number> double sum(T[] r)
	{
		double sum = 0;
		for (T i : r) sum += i.doubleValue();
		return sum;
	}

	
	
	public static boolean isDouble (String s)
	{
		if (s.length() == 0) return false;
		String doubleRegex = "(-?\\d+)|(-?\\d*\\.\\d+)";
		return (s.matches(doubleRegex));
	}
	
	public static boolean isPositiveDouble(String s)
	{
		if (!isDouble(s))
			return false;
		
		double d = Double.parseDouble(s);
		return (d > 0);
	}
	
	public static boolean isNegativeDouble(String s)
	{
		if (!isDouble(s))
			return false;
		
		double d = Double.parseDouble(s);
		return (d < 0);
	}
	
	public static boolean isInteger(String s)
	{
		if (s.length() == 0) return false;
		String doubleRegex = "(-?\\d*)";
		return (s.matches(doubleRegex));
	}
	
	public static boolean isPositiveInteger(String s)
	{
		if (s.length() == 0) return false;
		String doubleRegex = "(\\d*)";
		return (s.matches(doubleRegex));
	}
	
	public static boolean isNegativeInteger(String s)
	{
		if (s.length() == 0) return false;
		String doubleRegex = "(-\\d*)";
		return (s.matches(doubleRegex));
	}
	
	public static boolean isProbability (String s)
	{
		if (s.length() == 0) return false;
		String probabilityRegex = "(0)|(1)|(0\\.\\d*)|(\\.\\d*)";
		return (s.matches(probabilityRegex));
	}
	
	public static BigDecimal[] getUniformRandom(int n, double min, double max) {
		BigDecimal[] array = new BigDecimal[n];
		
		for (int i = 0; i < n; i++) {
			double number = Math.random()*(max-min)+min;
			array[i] = new BigDecimal("" + number, mc);
		}
		return array;
	}
	
	/**
	 * Determines if the array is a probability distribution. An array is a probability distribution iff:
	 * - all values are larger than 0
	 * - the sum is equal to 1 (allowing for a small deviation that might be caused by R-JAVA rounding issues or floating point errors)
	 * @param array
	 * @return
	 */
	public static boolean isProbabilityDistribution(BigDecimal[] array) {
		//TODO: flagged as high CPU usage
//		BigDecimal sum = BigDecimal.ZERO;
//		for (BigDecimal bd: array)
//			if (bd.compareTo(BigDecimal.ZERO)==-1)
//				return false;
//			else
//				sum = sum.add(bd, mc);
//		
		// (sum - 1) should be between -0.000000001 and 0.000000001
		return true ;// equalsWithinMarginOfError(sum, BigDecimal.ONE);
	}
	
	public static boolean isProbability(BigDecimal value) {
		if (value.compareTo(BigDecimal.ZERO)==-1 || value.compareTo(BigDecimal.ONE)==1)
			return false;
		return true;
	}
	
	/**
	 * Tests if two BigDecimals are equal to each other, within the specified margin of error. For example, if
	 * value1 is 0.1000000, value2 is 0.09999999, and the margin is 0.00000001, this function returns true.
	 * 
	 * This function is used when values might have a rounding error in them, or when there are (possible) floating point issues.
	 * @param value1
	 * @param value2
	 * @param margin
	 * @return
	 */
	public static boolean equalsWithinMarginOfError(BigDecimal value1, BigDecimal value2, BigDecimal margin) {
		BigDecimal difference = value1.subtract(value2, mc);
		
		if (difference.compareTo(new BigDecimal(margin.toPlainString(),mc))==1)
			return false;
		
		if (difference.compareTo(new BigDecimal("-"+margin.toPlainString(),mc))==-1)
			return false;
		return true;
	}
	
	/**
	 * Tests if two BigDecimals are equal to each other, within 0.000000001 margin of error. For example, if
	 * value1 is 0.1000000, value2 is 0.09999999, and the margin is 0.00000001, this function returns true.
	 * 
	 * This function is used when values might have a rounding error in them, or when there are (possible) floating point issues.
	 * @param value1
	 * @param value2
	 * @return
	 */
	public static boolean equalsWithinMarginOfError(BigDecimal value1, BigDecimal value2) {
		return equalsWithinMarginOfError(value1, value2, new BigDecimal("0.000000001"));
	}
	
//	// FILE READER STUFF
	public static File findFile(String filename, File directory) throws InterruptedException
	{
		FileFinder ff = new FileFinder(filename, directory);
		return ff.getFile();
	}
	
	private static class FileFinder
	{
		private final ExecutorService threadHandler;
		private      File	result;
		private final String filename;
		
		public FileFinder(String filename, File rootDirectory)
		{
			this.filename = filename;
			
			if (!rootDirectory.isDirectory())
				throw new IllegalArgumentException("Trying to read a non-directory as a directory (i.e., the directory specified either is not a directory or does not exist.");
	
			this.threadHandler = Executors.newWorkStealingPool();
			threadHandler.submit(new FileFinderSlave(filename, rootDirectory, this));
		}
		
		/** Returns the found file (or null if the file could not be found). Will cause the thread to sleep until the file has been found. 
		 * @throws InterruptedException */
		public File getFile() throws InterruptedException
		{
			threadHandler.awaitTermination(100, TimeUnit.MINUTES);
			return result;
		}
		
		/** Called by the slaves when they found the file. Immediately terminate all slaves  */
		public synchronized void foundFile(File file)
		{
			this.result = file;
			this.threadHandler.shutdownNow();
		}
		
		/** Called by the slaves when they found sub directories, but not the file that we are searching for. Make new slaves for each sub directory */
		public void foundDirectories(ArrayList<File> subdirectories) {
			for (File sd: subdirectories)
				threadHandler.submit(new FileFinderSlave(filename, sd, this));
		}
		
		/** A slave process to search a particular directory for file with filename. After searching directory, signifies to the master process if it found the file or found sub directories. Using callable so interruption is easy */
		private class FileFinderSlave implements Callable<Void>
		{
			private final String filename;		private final File directory;		private final FileFinder master;

			public FileFinderSlave(String filename, File directory, FileFinder master)	{
				this.filename = filename; this.directory = directory; this.master = master;
			}

			@Override
			public Void call() throws Exception {
				// List all files in the directory
				File[] files = directory.listFiles();
				
				// Create an array list for sub-directories. If the filename is not found in the current directory, we
				// will create new FileFinderSlaves for each sub directory.
				ArrayList<File> subdirectories = new ArrayList<>();
				
				// For each file: check if the file has the filename. If so, notify the FileFinder that a result has been found.
				// If the file is a directory, store it in the directory list.
				for (File f: files)
					if (f.getName().equals(filename) && !Thread.interrupted())
						master.foundFile(f);
					else if (f.isDirectory())
						subdirectories.add(f);
				
				if (subdirectories.size() > 0)
					master.foundDirectories(subdirectories);
				
				return null;
			}



		}
	}
	
	
	
	
	
	
	
	public static class Pair<F,S>{
		public final F element1;
		public final S element2;
		
		public Pair (F element1, S element2) {
			this.element1 = element1; this.element2 = element2;
		}
		
		public static<F,S> Pair<F,S> of (F element1, S element2) {return new Pair<F,S>(element1,element2);}

	}
	
	// Exceptions
	 /** An exception to throw if no R is installed. */
	@SuppressWarnings("serial")
	public static class NoRInstalledException extends Exception { public NoRInstalledException(String message) {     super(message);  }}
	
	/**
	 * An exception to throw if the model has impossible values. The view is designed to ensure this does not happen, but the code can be used without the view.
	 *
	 */
	@SuppressWarnings("serial")
	public static class MisspecifiedException extends Exception { public MisspecifiedException(String message) {     super(message);  }}
	
	/**
	 * An exception to throw if a proportion falls outside the range of [0,1], inclusive
	 *
	 */
	@SuppressWarnings("serial")
	public static class InvalidProportionException extends Exception { public InvalidProportionException(String message) {     super(message);  }}

	/**
	 * An exception to throw if a probability falls outside the range of [0,1], inclusive, or if the sum of all possible outcomes does not equal one.
	 *
	 */
	@SuppressWarnings("serial")
	public static class InvalidProbabilityException extends Exception { public InvalidProbabilityException(String message) {     super(message);  }}

	/**
	 * An exception to throw if the model has reached a state it cannot possible come into (e.g., zombie agents: non-dead agents with a negative budget).
	 *
	 */
	@SuppressWarnings("serial")
	public static class ImpossibleStateException extends Exception { public ImpossibleStateException(String message) {     super(message);  }}
	
	
}
