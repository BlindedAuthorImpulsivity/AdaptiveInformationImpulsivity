package agent.ValueIterator;

import java.util.HashMap;

import agent.ValueFunction;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import environment.Environment;
import estimationParameters.Optimizer;
import helper.Helper;
import markovDecisionProcess.MarkovDecisionProcess;

/** The ValueIteratorValueFunction stores all <budget, expected future value outcome> and 
 * <budget, terminal fitness> mappings in a HashMap. These values can be set/get by invoking the 
 * function from the ValueFunction superclass. 
 *
 */
public class ValueIteratorValueFunction extends ValueFunction
{
	private static final long serialVersionUID = Helper.programmeVersion;
	
	//The key list
	public final DecimalNumberArray budgets;

	// The HashMaps that map the budget (DecimalNumber) to the expected outcomes (DecimalNumber)
	private final HashMap<DecimalNumber, DecimalNumber> budgetToExpectedFutureValueOutcomeMap;
	private final HashMap<DecimalNumber, DecimalNumber> budgetToTerminalFitessMap;
	
	private final Optimizer optimizer;
	
	// WHEN USING THE LOSSY OPTIMIZER:
	// In the lossy optimization the values are all scaled with factor 1/(value step + budget step) to make all values integer (still stored in doubles, for now). 
	// In its naive (non-optimized) form we need to first scale the expected budget to the correct metric, change that double
	// value to a BigDecimal budget value, and find the corresponding expected future value or expected future fitness outcome in the corresponding
	// HashMap. Finally, because the optimizer uses double values rather than DecimalNumber values, we need to call the DecimalNumber.doubleValue() function.
	// These conversions are rather slow, but need to be done very often (even in the test runs this function was called billions to trillions of
	// times). 
	
	// Therefore this optimization: rather than doing all these conversions every time, we can map the scaled expected budget (double) values 
	// to the budgets (DecimalNumber) using a HashMap. Note that in different parts of the algorithm we still need to use the budget (DecimalNumber)
	// to expected outcome (DecimalNumber) mapping, hence we cannot get rid of the DecimalNumber format all together. Next, we can use an additional
	// budget (DecimalNumber) to expected outcome (double) HashMap mapping so that we do not have to repeatedly call the DecimalNumber.doubleValue()
	// function (which uses string parsing, and is quite slow). However, this required that whenever we update the <Budget (DecimalNumber), expected outcome (DecimalNumber)>
	// HashMaps (plural, for fitness and for value), we also need to update this <Budget (DecimalNUmber, expected outcome (Double)> HashMap.
	// we also have to update the 
	//Then, we can use the DecimalNumber form of the budget to 
	private final HashMap<Double, DecimalNumber> budgetScaledToBudgetMap;							
	private final HashMap<DecimalNumber, Double> DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap; 
	private final HashMap<DecimalNumber, Double> DOUBLE_MAP_budgetToTerminalFitnessMap; 
	
	
	public ValueIteratorValueFunction(MarkovDecisionProcess mdp, Environment environment, DecimalNumber startingValue, Optimizer optimizer) throws IllegalRangeException, IllegalScaleException 
	{
		super(mdp, environment);
		this.optimizer = optimizer;
		
		// Set all the main (i.e., non optimizer dependent) HashMaps
		budgets = DecimalNumberArray.sequence(DecimalNumber.ZERO, mdp.BUDGET_MAXIMUM, mdp.BUDGET_STEP).setImmutable(true); 	
		
		budgetToTerminalFitessMap = new HashMap<>();
		budgetToExpectedFutureValueOutcomeMap = new HashMap<>();
		for (int b =0; b < budgets.length(); b++) {
			budgetToExpectedFutureValueOutcomeMap.put(budgets.get(b), startingValue.clone());	
			budgetToTerminalFitessMap.put(budgets.get(b), startingValue.clone());
		}
		
		// Set all the optimizer dependent HashMaps - leaving them null if they will not be used
		if (optimizer == Optimizer.Lossy) {
			budgetScaledToBudgetMap = new HashMap<>();
			DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap = new HashMap<>();
			DOUBLE_MAP_budgetToTerminalFitnessMap = new HashMap<>();
			
			DecimalNumber scalingConstant = DecimalNumber.ONE.divide(mdp.VALUE_STEP).multiply(DecimalNumber.ONE.divide(mdp.BUDGET_STEP));
			double startingValueDouble = startingValue.doubleValue();
			
			for (int b=0; b< budgets.length(); b++ ) {
				budgetScaledToBudgetMap.put(budgets.get(b).multiply(scalingConstant).doubleValue(), budgets.get(b));
				DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap.put(budgets.get(b),startingValueDouble);
				DOUBLE_MAP_budgetToTerminalFitnessMap.put(budgets.get(b),startingValueDouble);
			}
		} else {
			budgetScaledToBudgetMap = null;
			DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap = null;
			DOUBLE_MAP_budgetToTerminalFitnessMap = null;
		}
	}
	
	
	/**
	 * Copy constructor
	 * @param mdp
	 * @param environment
	 * @param map
	 */
	private ValueIteratorValueFunction(ValueIteratorValueFunction oldValueFunction) throws IllegalRangeException, IllegalScaleException 
	{
		super(oldValueFunction.mdp, oldValueFunction.environment);
		this.optimizer = oldValueFunction.optimizer;
		
		budgets = oldValueFunction.budgets.clone().setImmutable(true); 
		
		budgetToExpectedFutureValueOutcomeMap = new HashMap<>();
		budgetToTerminalFitessMap = new HashMap<>();
		for (int b =0; b < budgets.length(); b++) {
			budgetToExpectedFutureValueOutcomeMap.put(budgets.get(b), oldValueFunction.getExpectedFutureValueOutcome(budgets.get(b)));
			budgetToTerminalFitessMap.put(budgets.get(b), oldValueFunction.getTerminalFitnessOutcome(budgets.get(b)));
		}
		
		// Set all the optimizer dependent HashMaps - leaving them null if they will not be used
		if (optimizer == Optimizer.Lossy) {
			budgetScaledToBudgetMap = oldValueFunction.budgetScaledToBudgetMap;
			DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap = new HashMap<>();
			DOUBLE_MAP_budgetToTerminalFitnessMap = new HashMap<>();

			for (int b =0; b < budgets.length(); b++) { 
				DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap.put(budgets.get(b), oldValueFunction.getExpectedFutureValueOutcome(budgets.get(b)).doubleValue());
				DOUBLE_MAP_budgetToTerminalFitnessMap.put(budgets.get(b), oldValueFunction.getTerminalFitnessOutcome(budgets.get(b)).doubleValue());
			}

		}
		else {
			budgetScaledToBudgetMap = null;
			DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap = null;
			DOUBLE_MAP_budgetToTerminalFitnessMap = null;
		}

	}
	
	/** Returns a DEEP clone. */
	public ValueIteratorValueFunction clone(){
		return new ValueIteratorValueFunction(this);
	}

	// Getters
	/**
	 * Given a budget after a resource encounter and extrinsic event, return 
	 * a mutable copy of the expected future VALUE stored in the value function.
	 * 
	 * Returns null if the budget is not in the value function.
	 * @param expectedBudget
	 * @return
	 */
	@Override
	public DecimalNumber getExpectedFutureValueOutcome(DecimalNumber budget)
	{
		DecimalNumber result = this.budgetToExpectedFutureValueOutcomeMap.get(budget);
		if (result != null)
			return result.clone();
		throw new NullPointerException("Exception in ValueIteratorValueFunction.getExpectedFutureValueOutcome(): argument budget not in budget list. Probably the budget is an invalid one.");

	}
	
	/**
	 * For a budget after a resource encounter and extrinsic event, 
	 * return a mutable copy of the expected terminal FITNESS stored in the value function.
	 * 
	 * Returns null if the budget is not in the value function.
	 * @param expectedBudget
	 * @return
	 */
	@Override
	public DecimalNumber getTerminalFitnessOutcome(DecimalNumber budget)
	{
		DecimalNumber result = this.budgetToTerminalFitessMap.get(budget);
		if (result != null)
			return result.clone();
		throw new NullPointerException("Exception in ValueIteratorValueFunction.getTerminalFitnessOutcomeDouble(): argument budget "+budget+" not in budget list. Probably the budget is an invalid one.");
	}
	
	
	// Getters: Optimization implementations
	/**
	 * @deprecated
	 * For a budget after a resource encounter and extrinsic event, return 
	 * a mutable copy of the expected future VALUE stored in the value function.
	 * 
	 * Returns null if the budget is not in the value function.
	 * @param expectedBudget
	 * @return
	 */
	@Override
	public double getExpectedFutureValueOutcomeDouble(DecimalNumber budget)
	{
		try { return DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap.get(budget); }
		
		// With values very close to 0 the probability of a floating point issue is increased. 
		// Hence this manual check
		catch (NullPointerException e) {
			if (budget.compareTo(mdp.BUDGET_STEP)==-1)
				return DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap.get(mdp.BUDGET_STEP); 
			else {
				System.err.println(budget); throw e;
			}
		}
		
	}
	
	/**
	 * For a SCALED budget after a resource encounter and extrinsic event, return 
	 * a mutable copy of the expected future VALUE stored in the value function.
	 * 
	 * Returns null if the budget is not in the value function.
	 * @param expectedBudget
	 * @return
	 */
	public double getExpectedFutureValueOutcome(double scaledBudget)
	{
		if (optimizer != null)
			if (optimizer != Optimizer.Lossy)
				throw new IllegalStateException("Exception in ValueIteratorValueFunction.getExpectedFutureValueOutcome(double scaledBudget): requesting the expected future value outcome associated with a scaled budget, but this function is not supported by chosen optimizer (optimizer = " + optimizer.name() + ")");
		DecimalNumber correspondingDecimalNumber = this.budgetScaledToBudgetMap.get(scaledBudget);
		return DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap.get(correspondingDecimalNumber); 
	}
	
	/**
	 * @deprecated
	 * Given an budget after a resource encounter and extrinsic event, 
	 * return a mutable copy of the expected terminal FITNESS stored in the value function.
	 * 
	 * Returns null if the budget is not in the value function.
	 * @param expectedBudget
	 * @return
	 */
	@Override
	public double getTerminalFitnessOutcomeDouble(DecimalNumber budget)
	{
		DecimalNumber result = this.budgetToTerminalFitessMap.get(budget);
		if (result != null)
			return result.doubleValue();
		throw new NullPointerException("Exception in ValueIteratorValueFunction.getTerminalFitnessOutcome(): argument budget not in budget list. Probably the budget is an invalid one.");
	}
	
	/**
	 * For a SCALED budget after a resource encounter and extrinsic event, return 
	 * a mutable copy of the TERMINAL FITNESS stored in the value function.
	 * 
	 * Returns null if the budget is not in the value function.
	 * @param expectedBudget
	 * @return
	 */
	public double getTerminalFitnessOutcome(double scaledBudget)
	{
		if (optimizer != null)
			if (optimizer != Optimizer.Lossy)
				throw new IllegalStateException("Exception in ValueIteratorValueFunction.getTerminalFitnessOutcome(double scaledBudget): requesting the terminal fitnees outcome associated with a scaled budget, but this function is not supported by chosen optimizer (optimizer = " + optimizer.name() + ")");
		
		DecimalNumber correspondingDecimalNumber = this.budgetScaledToBudgetMap.get(scaledBudget);
		return DOUBLE_MAP_budgetToTerminalFitnessMap.get(correspondingDecimalNumber); 
	}
	
	
	// Setters
	/**
	 * Sets (overrides) the value of the expected future VALUE outcome for the budget. 
	 * 
	 * Throws an IllegalArgumentException if the budget is not valid (i.e., has no
	 * entry in the value function).
	 * @param budget
	 * @param newValue
	 * @return
	 */
	@Override
	public void setExpectedFutureValueOutcome(DecimalNumber budget, DecimalNumber newValue) {
		if (!this.budgets.contains(budget, true))
			throw new IllegalArgumentException("Exception in ValueIteratorValueFunction.setExpectedFutureValueOutcome(): trying to set a new value for a budget that is not in the value function.");
		this.budgetToExpectedFutureValueOutcomeMap.put(budget, newValue);
		
		if (optimizer == Optimizer.Lossy)
			this.DOUBLE_MAP_budgetToExpectedFutureValueOutcomeMap.put(budget, newValue.doubleValue());
		
	}
	
	/**
	 * Sets (overrides) the value of the terminal fitness outcome for the budget. 
	 * 
	 * Throws an IllegalArgumentException if the budget is not valid (i.e., has no
	 * entry in the value function).
	 * @param budget
	 * @param newValue
	 * @return
	 */
	@Override
	public void setTerminalFitnessOutcome(DecimalNumber budget, DecimalNumber newValue) {
		if (!this.budgets.contains(budget, true))
			throw new IllegalArgumentException("Exception in ValueIteratorValueFunction.setTerminalFitnessOutcome(): trying to set a new value for a budget that is not in the value function.");
		this.budgetToTerminalFitessMap.put(budget, newValue);
		
		if (optimizer == Optimizer.Lossy)
			this.DOUBLE_MAP_budgetToTerminalFitnessMap.put(budget, newValue.doubleValue());
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\n" + Helper.repString("=", 100) +  "\n");
		sb.append("\n\nValue function:");
		sb.append(this.toDecimalNumberMatrix());
		return sb.toString();
	}



	/** <pre>
	 * Returns the DecimalNumberMatrix associated with the value function. This table has the following form:
	 * 
	 * =================================================================
	 * Budget                Expected value          Expected fitness         (ColumnNames)
	 * -----------------------------------------------------------------
	 * 0                     x.xxx                   x.xxx                    
	 * s                     x.xxx                   x.xxx
	 * ...                   ...                     x.xxx
	 * m-s                   x.xxx                   x.xxx
	 * m                     x.xxx                   x.xxx
	 * ==================================================================
	 * 
	 * Where m is the mdp.BUDGET_MAXIMUM, and s is the mdp.BUDGET_STEP. 
	 *
	 *
	 */
	@Override
	public DecimalNumberMatrix toDecimalNumberMatrix() {
		DecimalNumberArray expectedValue = new DecimalNumberArray(budgets.length());
		DecimalNumberArray expectedFitness = new DecimalNumberArray(budgets.length());
		for (int i = 0; i < budgets.length(); i++) {
			expectedValue.set(i, this.budgetToExpectedFutureValueOutcomeMap.get(budgets.get(i)));
			expectedFitness.set(i, this.budgetToTerminalFitessMap.get(budgets.get(i)));
		}
		
		DecimalNumberMatrix dnm = new DecimalNumberMatrix(budgets.length(), 3);
		dnm.setColumn(0, budgets);
		dnm.setColumn(1, expectedValue);
		dnm.setColumn(2, expectedFitness);
		dnm.setColumnNames("Budget", "Expected value", "Expected fitness");
		
		return dnm;
	}

	@Override
	public DecimalNumberArray getBudgets() {
		return budgets;
	}



}
