package estimationParameters;

import java.io.File;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import agent.AgentType;
import decimalNumber.DecimalNumber;
import defaults.Defaults;
import helper.Helper;

public class EstimationParameters implements Serializable {
	private static final long serialVersionUID = Helper.programmeVersion;

	
	public final AgentType agentType;
	public final  boolean 	useStoppingCriteriaTime,
						useStoppingCriteriaConvergence,
						useStoppingCriteriaIterations;
	
	public final  TimeUnit	stoppingCriteriaTimeUnit;
	public final  DecimalNumber 	stoppingCriteriaTimeMaximum,
						stoppingCriteriaConvergenceEpsilon;
	public final  int			stoppingCriteriaIterationsMaximum;
	
	public final  Optimizer optimizer;
	public final  DecimalNumber indifferencePoint;
	public final  DecimalNumber 	startingEstimates;
	public final  boolean		batchUpdating;
	public final  File		outputFolder;
	public final  int			numberOfSimultaniousThreads;
	
	public EstimationParameters(EstimationBuilder eb, File outputFolder) {
		this.agentType = eb.getAgentType();
		this.useStoppingCriteriaTime = eb.useStoppingCriteriaTime();
		this.useStoppingCriteriaConvergence = eb.useStoppingCriteriaConvergence();
		this.useStoppingCriteriaIterations = eb.useStoppingCriteriaIterations();
		
		this.stoppingCriteriaTimeUnit = eb.getStoppingCriteriaTimeUnit();
		this.stoppingCriteriaTimeMaximum = eb.getStoppingCriteriaTimeMaximum().clone().setImmutable(true);
		this.stoppingCriteriaConvergenceEpsilon = eb.getStoppingCriteriaConvergenceEpsilon().clone().setImmutable(true);
		this.stoppingCriteriaIterationsMaximum = eb.getStoppingCriteriaIterationsMaximum();
		
		this.optimizer=eb.getOptimizer();
		this.indifferencePoint = eb.getIndifferencePoint().clone().setImmutable(true);
		this.startingEstimates = eb.getStartingEstimates().clone().setImmutable(true);
		
		this.batchUpdating = eb.getBatchUpdating();
		this.outputFolder = outputFolder;
		this.numberOfSimultaniousThreads = eb.getNumberOfSimultaniousThreads();
		
		
	}
	
	/**
	 * Return an EstimationBuilder that has the same values as this immutable EstimationParameters.
	 * This builder can be changed and used to build a new (immutable) EstimationParameters - useful
	 * for when small things have to change (e.g., during retraining)
	 * @return
	 */
	public EstimationBuilder toBuilder() {
		EstimationBuilder builder = new EstimationBuilder();
		builder.setAgentType(this.agentType);
		builder.setUseStoppingCriteriaTime(this.useStoppingCriteriaTime);
		builder.setUseStoppingCriteriaConvergence(this.useStoppingCriteriaConvergence);
		builder.setUseStoppingCriteriaIterations(this.useStoppingCriteriaIterations);
		
		builder.setStoppingCriteriaTimeMaximum(this.stoppingCriteriaTimeMaximum.doubleValue());
		builder.setStoppingCriteriaTimeUnit(this.stoppingCriteriaTimeUnit);
		
		builder.setStoppingCriteriaConvergenceEpsilon(this.stoppingCriteriaConvergenceEpsilon.clone().setImmutable(false));
		
		builder.setStoppingCriteriaIterationsMaximum(this.stoppingCriteriaIterationsMaximum);
		
		builder.setOptimizer(this.optimizer);
		builder.setIndifferencePoint(this.indifferencePoint.clone().setImmutable(false));
		builder.setStartingEstimates(this.startingEstimates.clone().setImmutable(false));
		builder.setBatchUpdating(this.batchUpdating);
		builder.setOutputFolder(this.outputFolder);
		builder.setNumberOfSimultaniousThreads(this.numberOfSimultaniousThreads);
		
		return builder;
	}
	
	@Override
	public String toString() {

		StringBuilder sb = new StringBuilder();
		sb.append("Estimation parameters");
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
