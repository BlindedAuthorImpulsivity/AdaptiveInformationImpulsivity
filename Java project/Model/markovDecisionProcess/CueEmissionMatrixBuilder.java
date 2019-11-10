package markovDecisionProcess;

import java.math.RoundingMode;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.ComputationException;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import decimalNumber.DecimalNumberMatrixTableView;
import externalPrograms.RserveManager;

//TODO: update comments to reflect static class
/**
 * The CueEmissionMatrixBuilder houses the matrix that specifies, for each resource value (rows), what the probability of each cue (column) is.
 * This builder is constructed by specifying the MarkovDecisionProcessBuilder - which houses all the temporary parameters for the model - and
 * an initial DecimalNumberMatrix. The constructor is typically called from the MDP builder. If the initial matrix has a non-zero amount of 
 * rows, the initial CueEmissionType is set to manual. If not, the default cue emission type and argument are used to construct a new 
 * matrix. 
 * 
 * In either cases, the initial cue emission matrix should have specified column names, of the form [Resource value, cue label 1, cue label ... , cue label n].
 * NOTE THAT THIS MATRIX IS USED BY THE MODEL DURING COMPUPATION, AND HENCE USES DECIMALNUMBER REPRESENTATION RATHER THAN THE VIEW'S DOUBLE REPRESENTATION.
 * AS SUCH, ALL ENTRIES SHOULD BE DECIMALNUMBERS THAT HAVE A RANGE SPECIFIED BETWEEN 0 AND 1
 */
public abstract class CueEmissionMatrixBuilder {

	
	/**<pre> Set a new CueEmissionMatrix. If the new CueEmissionType is normal, set all cue probabilities
	 * to a normal distribution. If the new CueEmissionType is linear, set all cue probabilities to a linear
	 * distribution. In both cases all existing values are overwritten, and new resource values (rows) are placed
	 * if necessary. If the new CueEmissionType is manual, nothing is changed and/or overwritten.
	 *
	 *  
	 *  ~~ TAG: "adding cue emission type"
	 *  If you want to add a new emission type, this is where the computational part comes in. 
	 *  Specifically, you should create a new function that specifies how the matrix should look if the user
	 *  selects the new emission type, and add that function to the switch below. The double argument
	 *  can be used if you so desire.
	 *  
	 *  -----------------------------------------------------------------------------------------------------------------------------------------------------------
	 * NOTE THAT THIS MATRIX IS USED BY THE MODEL DURING COMPUPATION, AND HENCE USES DECIMALNUMBER REPRESENTATION RATHER THAN THE VIEW'S DOUBLE REPRESENTATION.
	 * AS SUCH, ALL ENTRIES SHOULD BE DECIMALNUMBERS THAT HAVE A RANGE SPECIFIED BETWEEN 0 AND 1
	 * -----------------------------------------------------------------------------------------------------------------------------------------------------------
	 *  
	 * @param type
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws ComputationException 
	 */
	public static void setCueEmissionMatrix(CueEmissionType type, double argument, MarkovDecisionProcessBuilder mdpBuilder) throws ComputationException, IllegalRangeException, IllegalScaleException
	{
		if (type == CueEmissionType.Linear)   setLinearCueEmissions(mdpBuilder);
		if (type == CueEmissionType.Normal)   setNormalCueEmissions(argument,mdpBuilder);
		if (type == CueEmissionType.Manual)	  updateManualCueEmissions(mdpBuilder);
	}

	/**
	 * Create m means that span the range of true values, are equidistant, and cover the extremes.
	 * m is given by the number of different values a cue can take, specified in the mdpBuilder object.
	 * Each value is rounded, as specified by the roundingContext
	 * @return
	 */
	private static DecimalNumberArray getEquidistantMeans(DecimalNumber valueMinimum, DecimalNumber valueMaximum, MarkovDecisionProcessBuilder mdpBuilder)
	{
		try {
			// First, determine the range of the spectrum
			DecimalNumber range = valueMaximum.multiply(2.0);

			// Using the range and the number of different values that a cue might have, we can determine the step size
			DecimalNumber stepSize = range.divide(mdpBuilder.CUE_EMISSION_MATRIX.ncol()-2);
			DecimalNumberArray sequence = DecimalNumberArray.sequence(valueMinimum, valueMaximum, stepSize);

			sequence.setImmutable(true);
			return sequence;
		} catch (IllegalRangeException | IllegalScaleException e) {
			// Illegal range and scale exception cannot occur in this function, and hence, we do not have to deal with them.
			e.printStackTrace(); return null;
		}
	}

	/** <pre> -----------------------------------------------------------------------------------------------------------------------------------------------------------
	 * NOTE THAT THIS MATRIX IS USED BY THE MODEL DURING COMPUPATION, AND HENCE USES DECIMALNUMBER REPRESENTATION RATHER THAN THE VIEW'S DOUBLE REPRESENTATION.
	 * AS SUCH, ALL ENTRIES SHOULD BE DECIMALNUMBERS THAT HAVE A RANGE SPECIFIED BETWEEN 0 AND 1
	 * -----------------------------------------------------------------------------------------------------------------------------------------------------------
	 * Set all cue probabilities to a normal distribution.
	 *
	 *  This procedure uses three steps. In the first step, n means are selected such that
	 * these means are all in the range of [-Parameters.VALUE_MAX, Parameters.VALUE_MAX] and all means are equidistant from each other. n here
	 * refers to the number of possible values a cue can have.
	 *
	 * For example, if the intrinsic values have a range of [-10, 10] and there are two possible cue values (e.g., corresponding to 'positive'
	 * and 'negative'), the means are at {-5, 5}.
	 *
	 * In the second step, for each mean we create a normal distribution that is centered on this mean, and has a specified standard deviation. 
	 * Hence, we have n normal distributions, the probability mass function (mass, since values are discrete) of which
	 * gives the probability of a cue given a true resource value.
	 *
	 * However, for each true value the summed probability of all possible cues does not add up to one. Hence the third step, where we
	 * normalize the probabilities of observing that particular cue, for each true value.
	 *
	 * After the third step we have a table containing p(Cue=cue|Value = value) for each combination and cues, in the following format:
	 *
	 * value		p(cue1|value)		p(cue2|value)		...		p(cue-n|value)
	 * -10			0.001				0.002				...		0.001
	 * -9			0.005				0.010				...		0.0001
	 * ...			...					...					...		...
	 *
	 *-----------------------------------------------------------------------------------------------------------------------------------------------------------
	 *NOTE THAT THIS MATRIX IS USED BY THE MODEL DURING COMPUPATION, AND HENCE USES DECIMALNUMBER REPRESENTATION RATHER THAN THE VIEW'S DOUBLE REPRESENTATION.
	 *	AS SUCH, ALL ENTRIES SHOULD BE DECIMALNUMBERS THAT HAVE A RANGE SPECIFIED BETWEEN 0 AND 1
	 *-----------------------------------------------------------------------------------------------------------------------------------------------------------
	 * @throws ComputationException 
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	private static void setNormalCueEmissions(double standardDeviation, MarkovDecisionProcessBuilder mdpBuilder) throws ComputationException, IllegalRangeException, IllegalScaleException
	{
		// Since the variance (and standard deviation) of a normal distribution cannot be zero, we have to make sure that the standard deviation here is also not zero.
		if (standardDeviation == 0) standardDeviation = 0.00001;

		// Create an decimalNumber for the SD 
		DecimalNumber SD = new DecimalNumber(standardDeviation, true);

		//Next, specify some variables
		DecimalNumber valueMaximum = new DecimalNumber(mdpBuilder.VALUE_MAXIMUM, true);
		DecimalNumber valueMinimum = valueMaximum.negate();
		DecimalNumber valueStep    = new DecimalNumber(mdpBuilder.VALUE_STEP, true);
		DecimalNumberArray resourceValues = DecimalNumberArray.sequence( valueMinimum, valueMaximum, valueStep);

		// Step 1: create m means that span the range of true values, are equidistant, and cover the extremes.
		DecimalNumberArray means = getEquidistantMeans(valueMinimum, valueMaximum, mdpBuilder);

		// Step 2: Create normal distributions and extract the conditional cue probabilities p(cue|value), for each possible value.
		// Specifically, there is one mean for each cue label. This mean represents the resource value that has the highest probability of emitting that cue.
		// Using the mean for each cue label, and using the standard deviation defined, we can compute the probability of a resource value emitting the cue
		// with that mean and standard deviation. The resulting matrix is of the shape:
		//
		//  (cue)			value n-1			value n 			value n+1
		//	cue1
		//  cue2
		//
		// Note that in the actual matrix the resourceValue column is ommitted!
		// Note that these probabilities do not sum to 1 per row!
		DecimalNumberMatrix matrix = new DecimalNumberMatrix(means.length(), resourceValues.length());

		for (int m=0;m<means.length();m++)
		{
			// Make R calculate the cue emission probabilities for each mean (i.e, cue)
			REXP rResult = null;
			try {
				rResult = RserveManager.evaluate("getNormalDistributionProbabilities(" +resourceValues.concatenateRStyle()  + ","
						+ means.get(m).doubleValue()+","
						+ SD.doubleValue()+ ");");
			} catch (REngineException | REXPMismatchException e1) {
				e1.printStackTrace();
			}
			double[] probabilities = null;

			try {probabilities = rResult.asDoubles();} catch (REXPMismatchException e) {e.printStackTrace();	}

			// Convert the resulting array of doubles to a DecimalNumberArray array, with decimalnumbers that are bounded between 0 and 1
			DecimalNumberArray cueEmissionProbabilities = new DecimalNumberArray(probabilities.length);
			for (int p=0; p< probabilities.length; p++)
				cueEmissionProbabilities.set(p, new DecimalNumber(probabilities[p], 0, 1, false));

			// store in the matrix
			matrix.setRow(m, cueEmissionProbabilities);
		}

		// Step 3:
		// matrix now has the following form:
		//  (cue)			value -n			value ... 			value n
		//	cue1			p(cue1|value n-1)   ... 				...
		//  ...				...					...					...
		//  cuen			...					...					p(cuen|value
		//
		// Where n is the highest possible resource value, and -n the lowest possible resource value. 
		// The column vectors in this matrix represents the cue emission probability per resource value.
		// As such these columns should sum to 1 (if we sample a cue, the probabilities of all cues should sum to 1).
		// This is not the case yet, so we do the following:
		// - transpose the matrix
		// - divide each value in the matrix by the total sum of the row vector that value is in
		//
		// After his, we add a new column vector at position 0 that contains the resource values, and then we have the cue emission matrix
		matrix = matrix.transpose();
		for (DecimalNumberArray rowVector: matrix) 
			rowVector.scaleToSumToOne();

		matrix.insertColumn(0, resourceValues);
		matrix.setColumnNames(mdpBuilder.CUE_EMISSION_MATRIX.getColumnNames());
		mdpBuilder.CUE_EMISSION_MATRIX.replaceMatrixWith(matrix);
	}

	/**
	 * <pre>-----------------------------------------------------------------------------------------------------------------------------------------------------------
	 * NOTE THAT THIS MATRIX IS USED BY THE MODEL DURING COMPUPATION, AND HENCE USES DECIMALNUMBER REPRESENTATION RATHER THAN THE VIEW'S DOUBLE REPRESENTATION.
	 *	AS SUCH, ALL ENTRIES SHOULD BE DECIMALNUMBERS THAT HAVE A RANGE SPECIFIED BETWEEN 0 AND 1
	 *-----------------------------------------------------------------------------------------------------------------------------------------------------------
	 * Set all cue probabilities to a linear distribution.
	 *
	 *  This implementation uses three steps. In the first step, n means are selected such that
	 * these means are all in the range of [-Parameters.VALUE_MAX, Parameters.VALUE_MAX], all means are equidistant from each other AND there
	 * are means at the two extremes of the spectrum. n here refers to the number of possible values a cue can have.</p>
	 *
	 * <p>For example, if the intrinsic values have a range of [-10, 10] and there are two possible cue values (e.g., corresponding to 'positive'
	 * and 'negative'), the means are at {-10, 10}.
	 *
	 * <p>In the second step, for each mean we create a function is maximum at the mean and linearly moves to zero as values become more extreme.
	 * Hence, for three values we get two lines starting at one at one extremes and sloping downwards to reach zero at the other extreme. For the
	 * third line we have a line that is one at {value=0} and moves to zero to both extremes of the spectrum. Note that the CUE_RELIABLILITY
	 * in the Parameters does not affect these cue emissions.
	 *
	 * <p>However, for each true value the summed probability of all possible cues does not add up to one. Hence the third step, where we
	 * normalize the probabilities of observing that particular cue, for each true value.
	 *
	 * <p>After the third step we have a table containing p(Cue=cue|Value = value) for each combination and cues, in the following format:
	 *
	 *<p>value		p(cue1|value)		p(cue2|value)		...		p(cue-n|value)
	 *<p> -10			0.001				0.002				...		0.001
	 *<p> -9			0.005				0.010				...		0.0001
	 *<p> ...			...					...					...		...
	 * @throws ComputationException 
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * -----------------------------------------------------------------------------------------------------------------------------------------------------------
	 * NOTE THAT THIS MATRIX IS USED BY THE MODEL DURING COMPUPATION, AND HENCE USES DECIMALNUMBER REPRESENTATION RATHER THAN THE VIEW'S DOUBLE REPRESENTATION.
	 *	AS SUCH, ALL ENTRIES SHOULD BE DECIMALNUMBERS THAT HAVE A RANGE SPECIFIED BETWEEN 0 AND 1
	 *-----------------------------------------------------------------------------------------------------------------------------------------------------------
	 */
	private static void setLinearCueEmissions(MarkovDecisionProcessBuilder mdpBuilder) throws ComputationException, IllegalRangeException, IllegalScaleException
	{
		//First, specify some variables
		DecimalNumber valueMaximum = new DecimalNumber(mdpBuilder.VALUE_MAXIMUM, true);
		DecimalNumber valueMinimum = valueMaximum.negate();
		DecimalNumber valueStep    = new DecimalNumber(mdpBuilder.VALUE_STEP, true);
		DecimalNumberArray resourceValues = DecimalNumberArray.sequence( valueMinimum, valueMaximum, valueStep);

		// Step 1: create m means that span the range of true values, are equidistant, and cover the extremes.
		DecimalNumberArray means = getEquidistantMeans(valueMinimum, valueMaximum, mdpBuilder);

		// Step 2: Create linear distributions and extract the conditional cue probabilities p(cue|value), for each possible value.
		// Specifically, there is one mean for each cue label. This mean represents the resource value that has the highest probability of emitting that cue.
		// Using the mean for each cue label, and using the standard deviation defined, we can compute the probability of a resource value emitting the cue
		// with that mean and standard deviation. The resulting matrix is of the shape:
		//
		//  (cue)			value n-1			value n 			value n+1
		//	cue1
		//  cue2
		//
		// Note that in the actual matrix the resourceValue column is ommitted!
		// Note that these probabilities do not sum to 1 per row!
		DecimalNumberMatrix matrix = new DecimalNumberMatrix(means.length(), resourceValues.length());
		for (int m=0;m<means.length();m++)
		{
			DecimalNumberArray cueEmissionProbabilities = new DecimalNumberArray(resourceValues.length());
			// 1.Determine cue probabilities for the interval [-maximum, mean), where m is the mean of the cue
			// 1a. Determine how many discrete values fall in this interval. This is:
			// [number of steps] = ([mean m]-[-maximum value]) / value_step = ([mean m]+[maximum value])  / value_step , where the left hand side is rounded down
			DecimalNumber intervalSize = means.get(m).add(valueMaximum).divide(valueStep);
			intervalSize = intervalSize.round(0, RoundingMode.HALF_DOWN);

			// 1b. Determine the slope of that interval
			DecimalNumber slope = new DecimalNumber(1);
			if (intervalSize.compareTo(0)!=0)
				slope = DecimalNumber.ONE.divide(intervalSize);

			// 1c. add the probabilities to the cueEmissionProbabilities array
			for (int i = 0; i < resourceValues.length(); i++)
				if (resourceValues.get(i).compareTo(means.get(m))==-1)
					cueEmissionProbabilities.set(i, new DecimalNumber(slope.multiply(i), DecimalNumber.ZERO , DecimalNumber.ONE, false));
				else
					cueEmissionProbabilities.set(i, -1); 

			// 2. add the probability=1 for value=mean to the array
			for (int i = 0; i < resourceValues.length();i++)
				if (resourceValues.get(i).compareTo(means.get(m))==0)
					cueEmissionProbabilities.set(i, new DecimalNumber(1,0,1,false)); 

			// 3. repeat step 1, but then for the interval (mean, maximum]
			intervalSize = valueMaximum.subtract(means.get(m)).divide(valueStep);
			intervalSize = intervalSize.round(0, RoundingMode.HALF_UP);
			slope = DecimalNumber.ONE;
			if (intervalSize.compareTo(0)!=0)
				slope = DecimalNumber.ONE.divide(intervalSize);

			for(int i = resourceValues.length()-1; i >= 0; i--)
				if (resourceValues.get(i).compareTo(means.get(m))==1)
					cueEmissionProbabilities.set(i, new DecimalNumber(slope.multiply(resourceValues.length()-i-1), DecimalNumber.ZERO, DecimalNumber.ONE, false));

			// 4. add to the matrix
			matrix.setRow(m, cueEmissionProbabilities);
		}

		// Step 3:
		// matrix now has the following form:
		//  (cue)			value -n			value ... 			value n
		//	cue1			p(cue1|value n-1)   ... 				...
		//  ...				...					...					...
		//  cuen			...					...					p(cuen|value
		//
		// Where n is the highest possible resource value, and -n the lowest possible resource value. 
		// The column vectors in this matrix represents the cue emission probability per resource value.
		// As such these columns should sum to 1 (if we sample a cue, the probabilities of all cues should sum to 1).
		// This is not the case yet, so we do the following:
		// - transpose the matrix
		// - divide each value in the matrix by the total sum of the row vector that value is in
		//
		// After his, we add a new column vector at position 0 that contains the resource values, and then we have the cue emission matrix
		matrix = matrix.transpose();

		for (DecimalNumberArray rowVector: matrix) 
			rowVector.scaleToSumToOne();

		matrix.insertColumn(0, resourceValues);
		matrix.setColumnNames(mdpBuilder.CUE_EMISSION_MATRIX.getColumnNames());
		mdpBuilder.CUE_EMISSION_MATRIX.replaceMatrixWith(matrix);
	}

	/** Update the matrix such that all new resource values have a row in the matrix. 
	 * If there are new rows (resource values) to be added, the cue emission for 
	 * those resource values have an uniform distribution over all possible cue labels 
	 * (i.e., there is no information to be gained for that resource value, no matter the
	 * cues observed).
	 * 
	 * If there are resource values that have to be removed (e.g., when the range of resource
	 * values has decreased), these rows are removed.
	 * @param mdpBuilder
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	private static void updateManualCueEmissions(MarkovDecisionProcessBuilder mdpBuilder) throws IllegalRangeException, IllegalScaleException {
		// Sort the original matrix
		mdpBuilder.CUE_EMISSION_MATRIX.sort(0, true);
		
		// Create a new matrix with the new resource values on the first column and all other values set to a uniform distribution
		DecimalNumberArray newResourceValues = mdpBuilder.getRangeOfValues();
		DecimalNumberArray oldResourceValues = mdpBuilder.CUE_EMISSION_MATRIX.getColumn(0);
		DecimalNumberMatrix newMatrix = new DecimalNumberMatrix(newResourceValues.length(), mdpBuilder.CUE_EMISSION_MATRIX.ncol());
		DecimalNumber uniformProbability = new DecimalNumber(1.0/(mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1), 0, 1, false);

		for (int r = 0; r < newMatrix.nrow(); r++){
			// If there is an entry in the existing matrix with the same resource value: use that array in the new matrix
			if (oldResourceValues.contains(newResourceValues.get(r), true))
				newMatrix.setRow(r, mdpBuilder.CUE_EMISSION_MATRIX.getRowWhereColumnIs(0, newResourceValues.get(r)));
				
			// else: create a new row with uniform cue emission probabilities
			else {
				DecimalNumberArray newRow = DecimalNumberArray.rep(uniformProbability, mdpBuilder.CUE_EMISSION_MATRIX.ncol());
				newRow.set(0, newResourceValues.get(r));
				newMatrix.setRow(r, newRow);
			}
		}
		
		newMatrix.setColumnNames(mdpBuilder.CUE_EMISSION_MATRIX.getColumnNames());
		mdpBuilder.CUE_EMISSION_MATRIX.replaceMatrixWith(newMatrix);
	}
	
	
}
