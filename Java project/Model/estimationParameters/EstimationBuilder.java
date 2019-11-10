package estimationParameters;

import java.io.File;
import java.util.concurrent.TimeUnit;

import agent.AgentType;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import defaults.Defaults;

/**
 * A class to wrap the fields associated with the estimation-tab variables. Essentially, nothing more than a collection
 * of getters and setters.
 */

public class EstimationBuilder {
	private AgentType agentType;
	private boolean 	useStoppingCriteriaTime,
						useStoppingCriteriaConvergence,
						useStoppingCriteriaIterations;
	
	private TimeUnit	stoppingCriteriaTimeUnit;
	private DecimalNumber 	stoppingCriteriaTimeMaximum,
						stoppingCriteriaConvergenceEpsilon;
	private int			stoppingCriteriaIterationsMaximum;
	
	private Optimizer		optimizer;
	private DecimalNumber indifferencePoint;
	private DecimalNumber 	startingEstimates;
	private boolean		batchUpdating;
	private File		outputFolder;
	private int			numberOfSimultaniousThreads;
	
	public EstimationBuilder() {
		
	}
	
	public void loadDefaults(Defaults defaults) {
		this.agentType = defaults.initialAgent;
		this.useStoppingCriteriaTime = defaults.useStoppingCriteriaMaximumTime;
		this.useStoppingCriteriaConvergence = defaults.useStoppingCriteriaConvergence;
		this.useStoppingCriteriaIterations = defaults.useStoppingCriteriaIterations;
		this.stoppingCriteriaTimeUnit = defaults.stoppingCriteriaMaximumTimeUnit;
		this.stoppingCriteriaTimeMaximum = new DecimalNumber(defaults.stoppingCriteriaMaximumTime);
		this.stoppingCriteriaConvergenceEpsilon = defaults.stoppingCriteriaConvergenceEpsilon;
		this.stoppingCriteriaIterationsMaximum = defaults.stoppingCriteriaMaximumIterations;
		this.optimizer=defaults.optimizer;
		this.indifferencePoint= defaults.indifferencePoint;
		this.startingEstimates = defaults.startingEstimates;
		this.batchUpdating = defaults.batchUpdating;
		this.outputFolder = new File(Defaults.defaultOutputFolderEnvironment);
		this.numberOfSimultaniousThreads = Defaults.numberOfSimultaniousThreads;
	}
	
	
	/**
	 * @return the agentType
	 */
	public AgentType getAgentType() {
		return agentType;
	}
	/**
	 * @param agentType the agentType to set
	 */
	public void setAgentType(AgentType agentType) {
		this.agentType = agentType;
	}
	
	/**
	 * @return should the agent run for a maximum amount of time?
	 */
	public boolean useStoppingCriteriaTime() {
		return useStoppingCriteriaTime;
	}
	/**
	 * @param stoppingCriteriaTime Set the maximum time the agent should run for. (Note, whether this maximum is enforced depends on the value of useStoppingCriteriaTime)
	 */
	public void setUseStoppingCriteriaTime(boolean stoppingCriteriaTime) {
		this.useStoppingCriteriaTime = stoppingCriteriaTime;
	}
	/**
	 * @return Should the agent run until the largest change between iterations (delta) is lower than epsilon?
	 */
	public boolean useStoppingCriteriaConvergence() {
		return useStoppingCriteriaConvergence;
	}
	/**
	 * @param stoppingCriteriaConvergence Set the largest delta (change in estimates after iteration) at which the agent should stop (epsilon). Whether this maximum is enforced depends on the value of useStoppingCriteriaConvergence
	 */
	public void setUseStoppingCriteriaConvergence(boolean stoppingCriteriaConvergence) {
		this.useStoppingCriteriaConvergence = stoppingCriteriaConvergence;
	}
	/**
	 * @return should the agent run a maximum number of iterations?
	 */
	public boolean useStoppingCriteriaIterations() {
		return useStoppingCriteriaIterations;
	}
	/**
	 * @param stoppingCriteriaIterations Set the maximum number of iterations that the agent should run for. Whether this value is enforced depends on useStoppingCriteriaIterations.
	 */
	public void setUseStoppingCriteriaIterations(boolean stoppingCriteriaIterations) {
		this.useStoppingCriteriaIterations = stoppingCriteriaIterations;
	}
	
	
	/**
	 * @return the stoppingCriteriaTimeUnit
	 */
	public TimeUnit getStoppingCriteriaTimeUnit() {
		return stoppingCriteriaTimeUnit;
	}
	/**
	 * @param stoppingCriteriaTimeUnit the stoppingCriteriaTimeUnit to set
	 */
	public void setStoppingCriteriaTimeUnit(TimeUnit stoppingCriteriaTimeUnit) {
		this.stoppingCriteriaTimeUnit = stoppingCriteriaTimeUnit;
	}
	/**
	 * @return the stoppingCriteriaTimeMaximum
	 */
	public DecimalNumber getStoppingCriteriaTimeMaximum() {
		return stoppingCriteriaTimeMaximum;
	}
	/**
	 * @param stoppingCriteriaTimeMaximum the stoppingCriteriaTimeMaximum to set
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */
	public void setStoppingCriteriaTimeMaximum(double stoppingCriteriaTimeMaximum) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		if (this.stoppingCriteriaTimeMaximum  != null)
			this.stoppingCriteriaTimeMaximum.set(stoppingCriteriaTimeMaximum);
		else
			this.stoppingCriteriaTimeMaximum = new DecimalNumber(stoppingCriteriaTimeMaximum);
	}
	/**
	 * @return the stoppingCriteriaConvergenceEpsilon
	 */
	public DecimalNumber getStoppingCriteriaConvergenceEpsilon() {
		return stoppingCriteriaConvergenceEpsilon;
	}
	/**
	 * @param stoppingCriteriaConvergenceEpsilon the stoppingCriteriaConvergenceEpsilon to set
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */
	public void setStoppingCriteriaConvergenceEpsilon(DecimalNumber stoppingCriteriaConvergenceEpsilon) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		if  (this.stoppingCriteriaConvergenceEpsilon != null)
			this.stoppingCriteriaConvergenceEpsilon.set(stoppingCriteriaConvergenceEpsilon);
		else
			this.stoppingCriteriaConvergenceEpsilon = stoppingCriteriaConvergenceEpsilon;
	}
	/**
	 * @return the stoppingCriteriaIterationsMaximum
	 */
	public int getStoppingCriteriaIterationsMaximum() {
		return stoppingCriteriaIterationsMaximum;
	}
	/**
	 * @param stoppingCriteriaIterationsMaximum the stoppingCriteriaIterationsMaximum to set
	 */
	public void setStoppingCriteriaIterationsMaximum(int stoppingCriteriaIterationsMaximum) {
		this.stoppingCriteriaIterationsMaximum = stoppingCriteriaIterationsMaximum;
	}
	
	public void setOptimizer(Optimizer optimizer) {
		this.optimizer = optimizer;
	}
	public Optimizer getOptimizer() {
		return this.optimizer;
	}
	
	/**
	 * @return the startingEstimates
	 */
	public DecimalNumber getStartingEstimates() {
		return startingEstimates;
	}
	/**
	 * @return the smallest difference in outcomes between actions that the agent is still responsive to (if the difference smaller, the two values are treated as equal)
	 */
	public void setStartingEstimates(DecimalNumber estimates) {
		if (this.startingEstimates != null)
			this.startingEstimates.set(estimates);
		else
			this.startingEstimates = estimates;
	}
	public DecimalNumber getIndifferencePoint() {
		return this.indifferencePoint;
	}
	public void setIndifferencePoint(DecimalNumber indifferencePoint) {
		if (this.indifferencePoint != null)
			this.indifferencePoint.set(indifferencePoint);
		else
			this.indifferencePoint = indifferencePoint;
	}
	/**
	 * @param startingEstimates the startingEstimates to set
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */
	public void setStartingEstimates(double startingEstimates) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		if (this.startingEstimates != null)
			this.startingEstimates.set(startingEstimates);
		else
			this.startingEstimates = new DecimalNumber(startingEstimates);
	}
	/**
	 * @return the batchUpdating
	 */
	public boolean getBatchUpdating() {
		return batchUpdating;
	}
	/**
	 * @param batchUpdating the batchUpdating to set
	 */
	public void setBatchUpdating(boolean batchUpdating) {
		this.batchUpdating = batchUpdating;
	}
	/**
	 * @return the outputFolder
	 */
	public File getOutputFolder() {
		return outputFolder;
	}
	/**
	 * @param outputFolder the outputFolder to set
	 */
	public void setOutputFolder(File outputFolder) {
		this.outputFolder = outputFolder;
	}
	/**
	 * @return the numberOfSimultaniousThreads
	 */
	public int getNumberOfSimultaniousThreads() {
		return numberOfSimultaniousThreads;
	}
	/**
	 * @param numberOfSimultaniousThreads the numberOfSimultaniousThreads to set
	 */
	public void setNumberOfSimultaniousThreads(int numberOfSimultaniousThreads) {
		this.numberOfSimultaniousThreads = numberOfSimultaniousThreads;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("Estimation procedure");
		sb.append("\nAgent type selected:                 " + this.agentType);
		sb.append("\nUsing maximum time criteria:         " + this.useStoppingCriteriaTime);
		sb.append("\nMaximum time:                        " + this.stoppingCriteriaTimeMaximum.toPlainString() + " " + this.stoppingCriteriaTimeUnit);
		sb.append("\nUsing convergence criteria:          " + this.useStoppingCriteriaConvergence);
		sb.append("\nEpsilon (lowest delta):              " + this.stoppingCriteriaConvergenceEpsilon.toPlainString());
		sb.append("\nUsing maximum iterations criteria:   " + this.useStoppingCriteriaIterations);
		sb.append("\nMaximum iterations:                  " + this.stoppingCriteriaIterationsMaximum);

		sb.append("\n\nOptimizer:                           " + this.optimizer);
		sb.append("\nIndifference point:                  " + this.indifferencePoint.toPlainString());
		sb.append("\nStarting estimates:                  " + this.startingEstimates.toPlainString());
		sb.append("\nUsing batch updating:                " + this.batchUpdating);

		sb.append("\n\nRuntime parameters");
		sb.append("\nOutput folder:                       " + this.outputFolder.getAbsolutePath());
		sb.append("\nNumber of threads to use:            " + this.numberOfSimultaniousThreads);
		
		return sb.toString();
	}
	
	
	
}
