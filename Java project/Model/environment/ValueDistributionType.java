package environment;

import java.util.ArrayList;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import externalPrograms.RserveManager;

public enum ValueDistributionType {
	Normal("Normal distribution"), 
	Uniform("Uniform distribution"),
	Manual("Manually specified distribution");
	
	private final String name;
	
	private ValueDistributionType(String s) {this.name = s; }
	
	public String toString(){return this.name;}
	
	public static ValueDistributionType[] getValuesExcludingManual ()
	{
		ArrayList<ValueDistributionType> arrayList = new ArrayList<>();
		for (ValueDistributionType vdt: ValueDistributionType.values())
			if (vdt != ValueDistributionType.Manual)
				arrayList.add(vdt);
		
		ValueDistributionType[] array = new ValueDistributionType[arrayList.size()];
		array = arrayList.toArray(array);
		
		return array;
	}
	
	
	public static DecimalNumberArray getPredefinedDistribution(DecimalNumberArray values, ValueDistributionType type, DecimalNumber mean, DecimalNumber standardDeviation) {
		if (type == ValueDistributionType.Normal)
			return getNormalDistribution(mean, standardDeviation, values);
			
		if (type == ValueDistributionType.Uniform)
			return getUniformDistribution(values);
		
		throw new IllegalArgumentException("Exception in ValueDistributionType.getPredefinedDistribution(): the ValueDistributionType argument is not a predefined distribution type (e.g., it might be manually specified)");
	}
	
	/** Returns a new ObservableList with the specified mean and standard deviation. 
	 * 
	 * IMPORANT: the matrix should contain the values (first column) already!
	 * @param mean
	 * @param std
	 * @return
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	public static DecimalNumberArray getNormalDistribution(DecimalNumber mean, DecimalNumber standardDeviation, DecimalNumberArray values) 
	{
		// Since the variance (and standard deviation) of a normal distribution cannot be zero, we have to make sure that the standard deviation here is also not zero.
		if (standardDeviation.compareTo(0)==-1)
			throw new IllegalArgumentException("Error when creating an environment's normal distribution: the standard deviation requested was " + standardDeviation + ". This value is not allowed.");

		// Nicely ask R to calculate the probabilities
		REXP rResult = null;
		try {
			rResult = RserveManager.evaluate("getNormalDistributionProbabilities(" + values.concatenateRStyle() + ","
					+ mean.doubleValue()+","
					+ standardDeviation.doubleValue() + ");");
		} catch (REngineException | REXPMismatchException e1) {
			e1.printStackTrace();
		}
		double[] probabilitiesDouble = null;

		try {probabilitiesDouble = rResult.asDoubles();} catch (REXPMismatchException e) {e.printStackTrace();	}

		// Convert the resulting array of doubles to a DecimalNumberArray (make sure that each decimal number in this array has to fall between 0 and 1, and is mutable)
		return new DecimalNumberArray(0,1, false, probabilitiesDouble);
	}
 
	/**
	 * Return a new DecimalNumberArray, containing an uniform distribution over all possible values stored in the matrix.
	 * 
	 * IMPORANT: the matrix should contain the values (first column) already!
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	public static DecimalNumberArray getUniformDistribution(DecimalNumberArray values) throws IllegalRangeException, IllegalScaleException
	{
		// All these values can only be between 0 and 1.
		return DecimalNumberArray.rep(DecimalNumber.ONE.divide(values.length()).setRange(0, 1)  , values.length());
	}


	}
