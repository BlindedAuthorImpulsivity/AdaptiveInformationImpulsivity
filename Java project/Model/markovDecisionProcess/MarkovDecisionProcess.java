package markovDecisionProcess;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import helper.Helper;
import helper.Helper.ImpossibleStateException;
import helper.Helper.MisspecifiedException;
import helper.ImmutableArray;

/** The MarkovDecisionProcess (or MDP for short) specifies all the parameters
 * that are necessary for the agent to run the task. All objects contained
 * in the MDP, and all objects contained by objects in the MDP, should be
 * final and immutable. 
 *
 *The MDP has to be constructed from a MarkovDecisionProcessBuilder, which in 
 *turn is constructed by the View.
 *
 * Note that the MDP's CUE_EMISSION_MATRIX should be a fully specified matrix:
 * each possible resource value should have an entry (row) - even if
 * the probability of that resource value is 0. 
 *
 */
public class MarkovDecisionProcess implements Serializable
{
	private static final long serialVersionUID = Helper.programmeVersion;
	
	/////////////		STATE SPACE PARAMETERS
	public final DecimalNumber 						VALUE_MAXIMUM,
    												VALUE_STEP,
    												BUDGET_MAXIMUM,
    												BUDGET_STEP;
	public final DecimalNumberArray					POSSIBLE_VALUES; 

	/////////////		TASK PARAMETERS
	public final DecimalNumber  					COST_OF_SAMPLING;
	public final int	    						MAXIMUM_CUES,
													NUMBER_OF_CUE_LABELS;
	public final boolean							COMPOUND_INTERRUPTION_RATE;
    private final DecimalNumberMatrix 				CUE_EMISSION_MATRIX; // private so that users have to use CUE_EMISSION_MATRIX, which provides a nice javadoc.
    private final DecimalNumberMatrix				CUE_EMISSION_MATRIX_WITHOUT_RESOURCE_VALUES; // a shallow clone of CUE_EMISSION_MATRIX, without the first "Resource value" column.
    
    public final ImmutableArray<String>				CUE_LABELS;

	/////////////		FITNESS PARAMETERS
	public final String 							FITNESS_FUNCTION;

	// Parameters required for the RL algorithms
	public final DecimalNumber DISCOUNT_RATE;

	public MarkovDecisionProcess(MarkovDecisionProcessBuilder mdpBuilder) throws MisspecifiedException, ImpossibleStateException
	{
		// Perform some checks to ensure that the MDP is well specified
		if (mdpBuilder.BUDGET_MAXIMUM<=0)
			throw new MisspecifiedException("Misspecification during construction of Markov Decision Process: The maximum budget is a non-positive value.");
		
		if (mdpBuilder.BUDGET_STEP <= 0)
			throw new MisspecifiedException("Misspecification during construction of Markov Decision Process: The step size of the budgets is a non-positive value.");
		
		if (mdpBuilder.VALUE_MAXIMUM <= 0 )
			throw new MisspecifiedException("Misspecification during construction of Markov Decision Process: The maximum resource value/extrinsic event value is a non-positive value");
		
		if (mdpBuilder.MAXIMUM_CUES < 0)
			throw new MisspecifiedException("Misspecification during construction of Markov Decision Process: The maximum number of cues to sample is not a positive integer");
		
		if (mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1 <= 0)
			throw new MisspecifiedException("The number of different cue labels is not a positive integer");
		
		mdpBuilder.CUE_EMISSION_MATRIX.sort(0, true);
		DecimalNumberArray resourceValues = mdpBuilder.getRangeOfValues();
		for (int rv = 0; rv < resourceValues.length(); rv++)
			if (!mdpBuilder.CUE_EMISSION_MATRIX.getRow(rv).get(0).equals(resourceValues.get(rv)))
				throw new MisspecifiedException("Misspecification during construction of Markov Decision Process: The CUE_EMISSION_MATRIX is not fully specified, or contains resource values that are not possible.");
		
		/////////////		STATE SPACE PARAMETERS
		this.BUDGET_MAXIMUM 								= new DecimalNumber(mdpBuilder.BUDGET_MAXIMUM, true);
		this.BUDGET_STEP 									= new DecimalNumber(mdpBuilder.BUDGET_STEP,true);
		this.VALUE_MAXIMUM 									= new DecimalNumber(mdpBuilder.VALUE_MAXIMUM, true);
		this.VALUE_STEP 									= new DecimalNumber(mdpBuilder.VALUE_STEP, true);
		this.POSSIBLE_VALUES								= mdpBuilder.getRangeOfValues();

		/////////////		TASK PARAMETERS
		this.COST_OF_SAMPLING 								= new DecimalNumber(mdpBuilder.COST_OF_SAMPLING, true);
		this.MAXIMUM_CUES 									= mdpBuilder.MAXIMUM_CUES;
		this.COMPOUND_INTERRUPTION_RATE						= mdpBuilder.COMPOUND_INTERRUPTION_RATE;
		
		this.CUE_EMISSION_MATRIX 							= mdpBuilder.CUE_EMISSION_MATRIX;
		this.CUE_EMISSION_MATRIX.setImmutable(true);

		this.CUE_EMISSION_MATRIX_WITHOUT_RESOURCE_VALUES	= mdpBuilder.CUE_EMISSION_MATRIX.shallowClone();
		this.CUE_EMISSION_MATRIX_WITHOUT_RESOURCE_VALUES.removeColumn(0);
		this.CUE_EMISSION_MATRIX_WITHOUT_RESOURCE_VALUES.setImmutable(true);
		
		this.NUMBER_OF_CUE_LABELS 							= CUE_EMISSION_MATRIX.ncol()-1;
		String[] cueLabels 									= new String[NUMBER_OF_CUE_LABELS];
		for (int i = 0; i < cueLabels.length; i++) cueLabels[i] = CUE_EMISSION_MATRIX.getColumnNames()[i+1]; // the +1 is because the matrix's first column is the "Resource value" column.
		this.CUE_LABELS 									= new ImmutableArray<String>(cueLabels);

		/////////////		FITNESS PARAMETERS
		this.FITNESS_FUNCTION 								= mdpBuilder.FITNESS_FUNCTION;
		this.DISCOUNT_RATE 									= new DecimalNumber(mdpBuilder.DISCOUNT_RATE, true);
	}

	/**
	 * Returns a MarkovDecisionProcessBuilder that has the same values as this immutable MarkovDecisionProcess.
	 * This builder can be changed and used to build a new (immutable) MarkovDecisionProcess- useful
	 * for when small things have to change (e.g., during retraining)
	 * @return
	 */
	public MarkovDecisionProcessBuilder toBuilder() {
		MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
		mdpBuilder.BUDGET_MAXIMUM = this.BUDGET_MAXIMUM.doubleValue();
		mdpBuilder.VALUE_MAXIMUM = this.VALUE_MAXIMUM.doubleValue();
		mdpBuilder.VALUE_STEP = this.VALUE_STEP.doubleValue();
		mdpBuilder.COST_OF_SAMPLING = this.COST_OF_SAMPLING.doubleValue();
		mdpBuilder.MAXIMUM_CUES = this.MAXIMUM_CUES;
		mdpBuilder.updateBUDGET_STEP();
		
		mdpBuilder.FITNESS_FUNCTION = this.FITNESS_FUNCTION;
		mdpBuilder.DISCOUNT_RATE = this.DISCOUNT_RATE.doubleValue();
		
		mdpBuilder.CUE_EMISSION_MATRIX = this.CUE_EMISSION_MATRIX.clone();
		
		return mdpBuilder;
	}
	
	/**
	 * <pre>
	 * Returns the cue emission matrix. This matrix (table) has the following shape:
	 * 
	 * ======================================================================================
	 * Resource value       [Cue label 1]        [Cue label 2]      [...]      [Cue label n]     
	 * --------------------------------------------------------------------------------------
	 * -n                    0.xxx               0.xxx               ...       0.xxx              
	 * -n+1                  0.xxx               0.xxx               ...       0.xxx               
	 * ...                   ...                 ...                 ...       ...
	 * n-1                   0.xxx               0.xxx               ...       0.xxx			
	 * n                     0.xxx               0.xxx               ...       0.xxx				
	 * ======================================================================================
	 * 
	 * if includeResourceValues is set the false, the first column ("Resource values") is not included, 
	 * and the result is:
	 * 
	 * =================================================================
	 * [Cue label 1]        [Cue label 2]      [...]      [Cue label n]      
	 * -----------------------------------------------------------------
	 * 0.xxx               0.xxx               ...       0.xxx              
	 * 0.xxx               0.xxx               ...       0.xxx               
	 * ...                 ...                 ...       ...
	 * 0.xxx               0.xxx               ...       0.xxx			
	 * 0.xxx               0.xxx               ...       0.xxx				
	 * =================================================================
	 * 
	 * Where n is the resource value (range = [mdp.VALUES_MININMUM, mdp.VALUES_MAXIMUM]) and the column
	 * names are defined by the cue labels (String[] CUE_LABELS in mdp).
	 *
	 * @return
	 */
	public DecimalNumberMatrix CUE_EMISSION_MATRIX (boolean includeResourceValues)
	{
		if (includeResourceValues)
			return this.CUE_EMISSION_MATRIX;
		return this.CUE_EMISSION_MATRIX_WITHOUT_RESOURCE_VALUES;
	}

	@ Override
	public String toString ()
	{
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("0.######");
		sb.append("---------------Markov Decision Process Parameters---------------\n");

		sb.append("\n##### State space parameters");
		sb.append("\n  Maximum resource value:\t" + df.format(VALUE_MAXIMUM.doubleValue()));
		sb.append("\n  Step size resource value:\t" + df.format(VALUE_STEP.doubleValue()));
		sb.append("\n  Maximum budget:\t\t" + df.format(BUDGET_MAXIMUM.doubleValue()));
		sb.append("\n  Step size budget:\t\t" + df.format(BUDGET_STEP.doubleValue()));

		sb.append("\n\n##### Task parameters");
		sb.append("\n  Maximum number of cues:\t" + MAXIMUM_CUES);
		sb.append("\n  Cost of sampling:\t" + df.format(COST_OF_SAMPLING));
		sb.append("\n  Use compound interruption rate:\t" + COMPOUND_INTERRUPTION_RATE);
		sb.append("\n  Cue emission matrix: \n" + CUE_EMISSION_MATRIX);

		sb.append("\n\n\n##### Fitness parameters");
		sb.append("\n  Budget to fitness function:\t" + "Fitness(x=budget) := " + FITNESS_FUNCTION);
		sb.append("\n  Discount rate:\t\t" + df.format(DISCOUNT_RATE.doubleValue()));


		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((BUDGET_MAXIMUM == null) ? 0 : BUDGET_MAXIMUM.hashCode());
		result = prime * result + ((BUDGET_STEP == null) ? 0 : BUDGET_STEP.hashCode());
		result = prime * result + ((COST_OF_SAMPLING == null) ? 0 : COST_OF_SAMPLING.hashCode());
		result = prime * result + ((CUE_EMISSION_MATRIX == null) ? 0 : CUE_EMISSION_MATRIX.hashCode());
		result = prime * result + ((DISCOUNT_RATE == null) ? 0 : DISCOUNT_RATE.hashCode());
		result = prime * result + MAXIMUM_CUES;
		result = prime * result + ((VALUE_MAXIMUM == null) ? 0 : VALUE_MAXIMUM.hashCode());
		result = prime * result + ((VALUE_STEP == null) ? 0 : VALUE_STEP.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MarkovDecisionProcess other = (MarkovDecisionProcess) obj;
		if (BUDGET_MAXIMUM == null) {
			if (other.BUDGET_MAXIMUM != null)
				return false;
		} else if (!BUDGET_MAXIMUM.equals(other.BUDGET_MAXIMUM))
			return false;
		if (BUDGET_STEP == null) {
			if (other.BUDGET_STEP != null)
				return false;
		} else if (!BUDGET_STEP.equals(other.BUDGET_STEP))
			return false;
		if (COST_OF_SAMPLING == null) {
			if (other.COST_OF_SAMPLING != null)
				return false;
		} else if (!COST_OF_SAMPLING.equals(other.COST_OF_SAMPLING))
			return false;
		if (CUE_EMISSION_MATRIX == null) {
			if (other.CUE_EMISSION_MATRIX != null)
				return false;
		} else if (!CUE_EMISSION_MATRIX.equals(other.CUE_EMISSION_MATRIX))
			return false;
		if (DISCOUNT_RATE == null) {
			if (other.DISCOUNT_RATE != null)
				return false;
		} else if (!DISCOUNT_RATE.equals(other.DISCOUNT_RATE))
			return false;
		if (MAXIMUM_CUES != other.MAXIMUM_CUES)
			return false;
		if (VALUE_MAXIMUM == null) {
			if (other.VALUE_MAXIMUM != null)
				return false;
		} else if (!VALUE_MAXIMUM.equals(other.VALUE_MAXIMUM))
			return false;
		if (VALUE_STEP == null) {
			if (other.VALUE_STEP != null)
				return false;
		} else if (!VALUE_STEP.equals(other.VALUE_STEP))
			return false;
		return true;
	}


	public static ArrayList<String> getAllVariableNames() {
		Field[] fields = MarkovDecisionProcess.class.getDeclaredFields();
		ArrayList<String> names = new ArrayList<>();
		
		// we only want non-static fields
		for (Field f : fields) {
			if (!Modifier.isStatic(f.getModifiers()))
				names.add(f.getName());
		}
		return names;
	}
	
	

}
