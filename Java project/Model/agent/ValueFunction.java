package agent;

import java.io.Serializable;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import environment.Environment;
import helper.Helper;
import markovDecisionProcess.MarkovDecisionProcess;

/** <pre>
 * A value function should hold a DecimalNumberMatrix containing the expected 
 * future outcome values (i.e., the expected outcomes of resource encounters
 * and extrinsic events, from now until the end of the agent's lifetime) and
 * the terminal fitness function (i.e., the expected fitness AT the end of the 
 * lifetime), for all possible current budget. For instance, a ValueIterator's 
 * value function has the following form:
 * 
 * =================================================================
 * Budget                Expected value         Expected fitness         (column names)
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
 *
 */
public abstract class ValueFunction implements Serializable
{
	private static final long serialVersionUID = Helper.programmeVersion;

	protected final MarkovDecisionProcess mdp;
	protected final Environment environment;
	
	protected final DecimalNumber maximumBudget;

	public ValueFunction(MarkovDecisionProcess mdp, Environment e)
	{
		this.mdp = mdp;
		this.environment = e;
		this.maximumBudget = mdp.BUDGET_MAXIMUM.clone().setImmutable(true);
	}

	/** Should return a DecimalNumberMatrix with the following column names: 
	 * "Budget", "Expected outcome", "Expected fitness". The first column specifies 
	 * the agent's current budget, the second column specifies the expected budget at
	 * the end of the lifetime, given that the agent has the specified current budget
	 * (i.e, the future budget). The third and final column specifies the expected
	 * fitness associated with this future budget.
	 * @return
	 */
	public abstract DecimalNumberMatrix toDecimalNumberMatrix();

	/** Should return a copy of the expected value outcome associated with the current budget. 
	 * That is, return a copy of the sum of all future expected VALUE (the direct changes in budget)
	 * from now until the end of the agent's 'lifetime', given that the current budget is the
	 * specified currentBudget. 
	 * 
	 * Returns null if the currentBudget is not a valid budget.
	 * @param expectedBudget
	 * @return
	 */
	public abstract DecimalNumber getExpectedFutureValueOutcome(DecimalNumber currentBudget);
	
	/**  Should return a double version of the expected TERMINAL FITNESS outcome associated with the current budget. 
	 * That is, return the sum of all future expected FITNESS (i.e., how the direct changes in budget
	 * reflect on terminal fitness), at the end of the agent's 'lifetime', given that the current budget is the
	 * specified currentBudget.
	 * 
	 * Returns null if the currentBudget is not a valid budget.
	 * 
	 * @param expectedBudget
	 * @return
	 */
	public abstract double getTerminalFitnessOutcomeDouble(DecimalNumber currentBudget);
	
	/** Should return a copy of the expected value outcome associated with the current budget. 
	 * That is, return a copy of the sum of all future expected VALUE (the direct changes in budget)
	 * from now until the end of the agent's 'lifetime', given that the current budget is the
	 * specified currentBudget. 
	 * 
	 * Returns null if the currentBudget is not a valid budget.
	 * @param expectedBudget
	 * @return
	 */
	public abstract double getExpectedFutureValueOutcomeDouble(DecimalNumber currentBudget);
	
	/**  Should return a double version of the expected TERMINAL FITNESS outcome associated with the current budget. 
	 * That is, return the sum of all future expected FITNESS (i.e., how the direct changes in budget
	 * reflect on terminal fitness), at the end of the agent's 'lifetime', given that the current budget is the
	 * specified currentBudget.
	 * 
	 * Returns null if the currentBudget is not a valid budget.
	 * 
	 * @param expectedBudget
	 * @return
	 */
	public abstract DecimalNumber getTerminalFitnessOutcome(DecimalNumber currentBudget);
	
	
	public abstract DecimalNumberArray getBudgets();
	
	/** Should set the value of the VALUE outcome for the budget.
	 * @param budget
	 * @param newValue
	 * @return
	 */
	public abstract void setExpectedFutureValueOutcome(DecimalNumber budget, DecimalNumber newValue);

	/** Should set the value of the terminal FITNESS outcome for the budget.
	 * @param budget
	 * @param newValue
	 * @return
	 */
	public abstract void setTerminalFitnessOutcome(DecimalNumber budget, DecimalNumber newValue);


}
