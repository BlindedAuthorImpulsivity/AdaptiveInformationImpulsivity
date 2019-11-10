package environment;

import java.text.DecimalFormat;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.ComputationException;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import decimalNumber.DecimalNumberMatrixTableView.TableViewObject;
import helper.Helper.MisspecifiedException;
import markovDecisionProcess.MarkovDecisionProcessBuilder;

public class EnvironmentBuilderFull  extends AbstractEnvironmentBuilder implements TableViewObject
{
	private MarkovDecisionProcessBuilder mdpBuilder;
	private final DecimalNumberMatrix matrix;

	/** Constructs the EnvironmentBuilderFull. Requires an MDPbuilder, 
	 * The initialMatrix is optional: if specified, the environment is constructed with that matrix in mind (used when defining defaults) and the distribution types are set to manual.
	 * @param resourceValueMean
	 * @param resourceValueSD
	 * @param resourceValueDistributionType
	 * @param extrinsicEventMean
	 * @param extrinsicEventSD
	 * @param extrinsicEventDistributionType
	 * @param interruptionRate
	 * @param initialMatrix
	 * @param mdpBuilder
	 * @throws IllegalArgumentException
	 * @throws IllegalRangeException
	 * @throws IllegalScaleException
	 */
	public EnvironmentBuilderFull(
			double resourceValueMean,  double resourceValueSD,  ValueDistributionType resourceValueDistributionType, 
			double extrinsicEventMean, double extrinsicEventSD,	ValueDistributionType extrinsicEventDistributionType, 
			double interruptionRate,
			DecimalNumberMatrix initialMatrix) 	throws IllegalArgumentException, IllegalRangeException, IllegalScaleException
	{
		super(resourceValueMean, resourceValueSD, resourceValueDistributionType, extrinsicEventMean, extrinsicEventSD, extrinsicEventDistributionType, interruptionRate);

		if (initialMatrix != null) {
			this.matrix = initialMatrix;
			this.resourceValueDistributionType = ValueDistributionType.Manual;
			this.extrinsicEventDistributionType = ValueDistributionType.Manual;
			this.recalculateMeanAndSD();

		} else {
			this.matrix = new DecimalNumberMatrix(0,3);
		}

		matrix.setColumnNames("Value","p(Resource value = value)","p(Extrinsic event value = value)" );
	
	}
	
	
	public void setMDPBuilder(MarkovDecisionProcessBuilder mdpBuilder) {
		this.mdpBuilder = mdpBuilder;
	}
	
	/**
	 * Set the resource and extrinsic event distributions to match the type specified and the mdpBuilder constraints.
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	public void update() 	{
		if (this.mdpBuilder == null)
			throw new IllegalStateException("Exception in EnvironmentBuilderFull.update(): this function is called before a MDPBuilder is set. Please use setMDPBuilder() on this object before updating. ");
		// Create a new matrix to hold the new values and probability distributions
		DecimalNumberMatrix newMatrix = new DecimalNumberMatrix(mdpBuilder.getRangeOfValues().length(), 3);
		newMatrix.setColumnNames(matrix.getColumnNames());
		
		// Set the first column: the values
		newMatrix.setColumn("Value", mdpBuilder.getRangeOfValues());
		
		// Set the second column: the resource value probability distribution
		if (this.resourceValueDistributionType == ValueDistributionType.Manual)
			newMatrix.setColumn("p(Resource value = value)", updateManualDistribution(matrix.getColumn(0), matrix.getColumn(1), mdpBuilder));
		else
			newMatrix.setColumn("p(Resource value = value)", ValueDistributionType.getPredefinedDistribution(newMatrix.getColumn("Value"), this.resourceValueDistributionType, this.resourceValueMean, this.resourceValueSD));
	
		// Set the third column: the extrinsic event value probability distribution
		if (this.extrinsicEventDistributionType == ValueDistributionType.Manual)
			newMatrix.setColumn("p(Extrinsic event value = value)", updateManualDistribution(matrix.getColumn(0), matrix.getColumn(2), mdpBuilder));
		else
			newMatrix.setColumn("p(Extrinsic event value = value)", ValueDistributionType.getPredefinedDistribution(newMatrix.getColumn("Value"), this.extrinsicEventDistributionType, this.extrinsicEventMean, this.extrinsicEventSD));

		// Replace the old matrix with the new one
		this.matrix.replaceMatrixWith(newMatrix);
		
		// Recalculate means and std.devs for manually specified distributions
		this.recalculateMeanAndSD();

	}

	/** TODO: comments. oldValues is the previous array of possible values (i.e., from -VALUE_MAXIMUM to VALUE_MAXIMUM). Old probabilities is the previous distribution. 
	 * Assumes oldValues are sorted 
	 * Assumes mdpBuilder set*/
	private static DecimalNumberArray updateManualDistribution(DecimalNumberArray oldValues, DecimalNumberArray oldProbabilities, MarkovDecisionProcessBuilder mdpBuilder) {
		if (mdpBuilder == null)
			throw new IllegalStateException("Exception in EnvironmentBuilderFull.updateManualDistribution(): this function is called before a MDPBuilder is set. Please use setMDPBuilder() on this object before updating. ");
		
		if (oldValues.length() != oldProbabilities.length())
			throw new IllegalArgumentException("Exception in EnvironmentBuilderFull.updateManualDistribution(): the number of old values and old probabilities do not match.");
		
		// Request the new array of all possible values from the MDP
		DecimalNumberArray newValues = mdpBuilder.getRangeOfValues();
		
		// Create an array to hold the newProbabilities. This array starts with only 0s
		DecimalNumberArray newProbabilities = DecimalNumberArray.rep(0, newValues.length());
		
		// For each possible DecimalNumber in oldValues: check if the old value is in the new Value and if the old probability of that value was not 0.
		// 	If both conditions hold: set the old probability of that value in newProbabilities.
		// TODO: in its current state, this requires a possible of n places to search in the old values (the getIndexOfRowWhereColumnIs does not take into
		// 		account that the newValues are sorted), hence the Big-Oh of this function in n2. It can be made faster, although I wonder whether
		//		this is worth the effort. Should do some profiling/sampling during runtime to see if is matters much. Very low priority.
		for (int i = 0; i< oldValues.length(); i++) {
			int indexOldValueInNewValues = newValues.indexOf(oldValues.get(i));
			if (indexOldValueInNewValues != -1)
				newProbabilities.set(indexOldValueInNewValues, oldProbabilities.get(i));
		}
		
		// Normalize the newProbabilities to sum to 1
		newProbabilities.toProbability();
			
		return newProbabilities;
		
	}

	/** Calculates the mean and standard deviation of the manually specified distributions (if any) */
	private void recalculateMeanAndSD() throws IllegalRangeException, IllegalScaleException {
		if (resourceValueDistributionType == ValueDistributionType.Manual) {
			DecimalNumberArray values = matrix.getColumn(0);
			DecimalNumberArray probabilities = matrix.getColumn(1);
			this.resourceValueMean	.set(computeMean(values, probabilities));
			this.resourceValueSD	.set(computeSD(values, probabilities, resourceValueMean)); 
		}
		
		if (extrinsicEventDistributionType == ValueDistributionType.Manual) {
			DecimalNumberArray values = matrix.getColumn(0);
			DecimalNumberArray probabilities = matrix.getColumn(2);
			this.extrinsicEventMean	.set(computeMean(values, probabilities));
			this.extrinsicEventSD	.set(computeSD(values, probabilities, extrinsicEventMean)); 
		}
	}
	
	private DecimalNumber computeMean(DecimalNumberArray values, DecimalNumberArray probabilities) {
		return values.dotProduct(probabilities);
	}

	private DecimalNumber computeSD(DecimalNumberArray values, DecimalNumberArray probabilities, DecimalNumber mean) {
		DecimalNumber variance = new DecimalNumber(0);
		for (int i = 0;i < values.length(); i++)
			variance.add(
					mean.subtract(values.get(i),false).pow(2).multiply(probabilities.get(i))
					);
		return variance.pow(0.5);
	}
	
	/**Returns a matrix of the environment. This matrix consists of 3 column vectors, with the names: 
	 *  Value", "p(Resource value = value)", and "p(Extrinsic event value = value)".
	 *  
	 * This matrix always has [number of values] rows.
	 * 
	 *  Note that changes in this matrix DO INFLUENCE the environment (i.e., it is passed per pointer).
	 * @throws ComputationException 
	 */
	public DecimalNumberMatrix getMatrix() {
		return matrix;
	}

	@Override
 	public String toString()
	{
		DecimalFormat df = new DecimalFormat("#.####");
		StringBuilder sb = new StringBuilder();
		sb.append("\nMean resource value:                 " + df.format(this.resourceValueMean));
		sb.append("\nStandard deviation resource value:   " + df.format(this.resourceValueMean));
		sb.append("\nResource value distribution type:    " + this.resourceValueDistributionType);
		sb.append("\nMean extrinsic event value:          " + df.format(this.extrinsicEventMean));
		sb.append("\nStandard deviation extrinsic event:  " + df.format(this.extrinsicEventSD));
		sb.append("\nExtrinsic event distribution type:   " + this.extrinsicEventDistributionType);
		sb.append("\nInterruption rate:                   " + df.format(this.interruptionRate));
		sb.append("\nEnvironment matrix:" + this.matrix);
		return sb.toString();
	}


	@Override
	public Environment toEnvironment() throws MisspecifiedException {
		return new Environment(
				resourceValueMean, 
				resourceValueSD, 
				resourceValueDistributionType, 
				extrinsicEventMean, 
				extrinsicEventSD, 
				extrinsicEventDistributionType, 
				interruptionRate, 
				matrix);
	}

	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////// 	DEPRECATED (SCHEDULED FOR REMOVAL) 	/////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	
	/**@Deprecated Scheduled for removal. 
	 * 
	 * Update the matrix: computes the values for resource encounters and extrinsic events based
	 * on their value distribution (e.g., normal or uniform). Does nothing if the distribution is
	 * set to manual. Computes the corresponding probabilities for each value in the value column. 
	 * Hence,  the "Value" column should already contain all possible values prior to invoking this argument! */
	private  void updateMatrixWithPredefinedValueDistributions() {
		/*if (resourceValueDistributionType == ValueDistributionType.Normal) 
			matrix.setColumn(1, getNormalDistribution(resourceValueMean, resourceValueSD, matrix));
		if (resourceValueDistributionType == ValueDistributionType.Uniform)
			matrix.setColumn(1, getUniformDistribution(matrix));

		if (extrinsicEventDistributionType == ValueDistributionType.Normal) 
			matrix.setColumn(2, getNormalDistribution(extrinsicEventMean, extrinsicEventSD, matrix));
		if (extrinsicEventDistributionType == ValueDistributionType.Uniform)
			matrix.setColumn(2, getUniformDistribution(matrix));*/
	}

	/** @Deprecated Scheduled for removal.
	 * 
	 * Updates the matrix such that:
	 * 		- the value column ranges from  [-VALUE_MAXIMUM] to [VALUE_MAXIMUM] with step size [VALUE_STEP]
	 * 		- each manually specified value is maintained if it falls in the range (and discarded if it does not)
	 * 		- all other values are set to 0.
	 * 
	 * Hence, if either the resource value distribution type or the extrinsic event distribution type is manual, this function
	 * should be called BEFORE ANY OTHER UPDATE IS DONE.
	 * 
	 * Note that the all values in the values column have a range specified between [-VALUE_MAXIMUM] and [VALUE_MAXIMUM], whereas
	 * all other values have a range specified between 0 and 1.
	 * @throws IllegalRangeException
	 * @throws IllegalScaleException
	 * @throws ComputationException 
	 */
	private void updateMatrixWithManualValueDistributions(){
		// sort the current matrix based on the values, in ascending order
		matrix.sort(0, true);

		// Create a new matrix with values from [-VALUE_MAXIMUM] to [VALUE_MAXIMUM] with step size [VALUE_STEP] on the first column ("value"),
		// and zero's in the other two columns ("p(Resource value = value)" and "p(Extrinsic event value = value)").
		// Note that all values in the latter two columns have a specified range between 0 and 1.
		DecimalNumberArray newValues = mdpBuilder.getRangeOfValues();

		DecimalNumberArray newResourceValues = DecimalNumberArray.rep(new DecimalNumber(0, 0, 1, false), newValues.length());
		DecimalNumberArray newExtrinsicValues = DecimalNumberArray.rep(new DecimalNumber(0, 0, 1, false), newValues.length());
		DecimalNumberMatrix newMatrix = DecimalNumberMatrix.columnBind(newValues, newResourceValues, newExtrinsicValues);
		newMatrix.setColumnNames("Value", "p(Resource value = value)","p(Extrinsic event value = value)");

		// maintain the manually specified values (if applicable)
		// For each row in the original matrix:
		//  if the value in that row is in the new matrix:
		// 		if the resource value distribution type is manual: set the resource value probability for that value to the manually specified value
		// 		if the extrinsic value value distribution type is manual: set the extrinsic event value probability for that value to the manually specified value
		if (resourceValueDistributionType == ValueDistributionType.Manual|| extrinsicEventDistributionType == ValueDistributionType.Manual) 
		{
			for (DecimalNumberArray row : matrix) {
				int indexInNewMatrix = newMatrix.getIndexOfRowWhereColumnIs("Value", row.get(0));
				if (indexInNewMatrix != -1) {
					if (resourceValueDistributionType == ValueDistributionType.Manual) newMatrix.setValueAt(indexInNewMatrix, 1, row.get(1));
					if (extrinsicEventDistributionType == ValueDistributionType.Manual) newMatrix.setValueAt(indexInNewMatrix, 2, row.get(2));
				}
			}

			// Make sure the last two columns sum to one
			if (resourceValueDistributionType == ValueDistributionType.Manual) 
				newMatrix.getColumn(1).toProbability();
			if (extrinsicEventDistributionType == ValueDistributionType.Manual) 
				newMatrix.getColumn(2).toProbability();
		}
		matrix.replaceMatrixWith(newMatrix);
	}



}
