package agent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberMatrix;
import environment.AbstractEnvironmentBuilder;
import environment.Environment;
import estimationParameters.EstimationParameters;
import helper.Helper;
import helper.Helper.MisspecifiedException;
import markovDecisionProcess.MarkovDecisionProcess;
import start.Model;

/**
 * The abstract class for all Agents. Note that this class requires the subclasses to implement both the Agent functions, as well
 * as the Runnable functions (i.e., the agent has to be able to deal with multi-threading).
 *
 */
public abstract class Agent implements Runnable, Serializable
{
	private static final long serialVersionUID = Helper.programmeVersion;
	
	public enum StoppingCriteria {
		CONVERGENCE,
		MAXIMUM_ITERATIONS,
		MAXIMUM_TIME;
	}

	public final 	Model							model;
	public final 	MarkovDecisionProcess 			mdp;
	public final 	AbstractEnvironmentBuilder		environmentBuilder;
	protected		Environment						environment;
	public final    EstimationParameters			estimationParameters;
	public final	ValueFunction					startingValueFunction;

	public	      	long							maximumTimeInSeconds; 
	public 			long							startTime;
	public 	final 	int						startingIteration;

	protected 		String 							ID;
	
	
	//TODO Update javadoc: this is the full constructor, used for e.g. retraining
	/**Constructor for an Agent. 	 
	 * 
	 * @param environmentBuilder.  An abstract environmentBuilder that contain the blueprints for the environment 
	 * in which the agent 'lives'. The agent only lives in a single environment, and tries to estimate the optimal 
	 * value function within this environment. During runtime the agent constructs the environment from the builder.
	 * 
	 * @param epsilon How close should the agent approach convergence? Converge here means that the largest update in the
	 * value function in the last batch is not larger than epsilon. More formally, see: https://en.wikipedia.org/wiki/(%CE%B5,_%CE%B4)-definition_of_limit
	 * 
	 * @param maximumIteration. Specifies how many iterations the algorithm can maximally run. One iteration is a single update of all
	 * budget estimates in the value function.
	 *  
	 * @param updateFrequency UpdateFrequency Specifies after how many budget evaluations the value function is updated. If this value is 1, updating
	 * occurs online (i.e., the value function is always the best estimate). If this value is -1, updating happens per batch (i.e.,
	 * when estimating a state, the agent uses the value function of the previous batch. The value function is updated after all
	 * states have been visited at least once). If this parameter is set to 1, the agent follows online updating.
	 * 
	 * @param stoppingCriteria. Specifies the stopping criteria used. Use on of the following enumerations.
	 * 
	 * @param outputFolder. The location where the results have to be stored. The output file should have the name "[agentID].out", and contain a reference to the 
	 * agents MPD, environment, final value function (in DecimalNumberMatrix format) and optimalCuesToSample (a DecimalNumberMatrix). In essence, the output
	 * file should include sufficient information for the model to reconstruct the optimal policies when it needs to.
	 * 
	 * 
	 * ""
	 * MAXIMUM_ITERATIONS: 					stop if the number of iterations exceeds the maximum number of iterations;
	 * CONVERGENCE: 						stop if agent convergences closer than epsilon to the true value function.
	 * CONVERGENCE_OR_MAXIMUM_ITERATIONS:	stop if either MAXIMUM_ITERATIONS or CONVERGENCE has been reached.
	 * 
	 * @return
	 * @throws IOException 
	 * @throws MisspecifiedException 
	 */
	public  Agent (Model model, MarkovDecisionProcess mdp, AbstractEnvironmentBuilder environmentBuilder, EstimationParameters estimationParameters, ValueFunction startingValueFunction, int startingIteration) throws IOException, MisspecifiedException {
		this.model = model;
		this.mdp = mdp;
		this.environmentBuilder = environmentBuilder;
		this.estimationParameters = estimationParameters;
		this.startingValueFunction = startingValueFunction;
		this.startingIteration = startingIteration;
		
		// The output folder should be valid
		if (estimationParameters.outputFolder != null)
			if (!estimationParameters.outputFolder.exists() || !estimationParameters.outputFolder.isDirectory())
				throw new IOException("Exception in Agent constructor: created an agent with an ouput folder that either does not exist, or is not a directory.");

		// Check if the stopping criteria are valid
		if (estimationParameters.useStoppingCriteriaConvergence && estimationParameters.stoppingCriteriaConvergenceEpsilon.compareTo(0)<=0)
			throw new MisspecifiedException("Exception in Agent.constructor()(abstract): epsilon convergence procedure requested with non-positive epsilon.");
		if (estimationParameters.useStoppingCriteriaIterations && estimationParameters.stoppingCriteriaIterationsMaximum<=0)
			throw new MisspecifiedException("Exception in Agent.constructor()(abstract): maximum iterations specified with a non-positive value");
		if (estimationParameters.useStoppingCriteriaTime) {
			if (estimationParameters.stoppingCriteriaTimeUnit == TimeUnit.DAYS) 
				this.maximumTimeInSeconds = estimationParameters.stoppingCriteriaTimeMaximum.longValue() * 86400; 
			else if (estimationParameters.stoppingCriteriaTimeUnit  == TimeUnit.HOURS)
				this.maximumTimeInSeconds = estimationParameters.stoppingCriteriaTimeMaximum.longValue() * 3600; 
			else if (estimationParameters.stoppingCriteriaTimeUnit  == TimeUnit.MINUTES)
				this.maximumTimeInSeconds = estimationParameters.stoppingCriteriaTimeMaximum.longValue() * 60; 
			else
				this.maximumTimeInSeconds = estimationParameters.stoppingCriteriaTimeMaximum.longValue();
			if (maximumTimeInSeconds <= 0)
				throw new MisspecifiedException("Exception in Agent.constructor()(abstract): maximum time specified with a non-positive value");
		}
		if (!(estimationParameters.useStoppingCriteriaConvergence || estimationParameters.useStoppingCriteriaIterations || estimationParameters.useStoppingCriteriaTime))
			throw new MisspecifiedException("Exception in Agent.constructor()(abstract): no stopping criteria specified.");
		
		this.startTime = System.nanoTime();

		
		int id = this.hashCode()/10000;
		if (id < 0) id=id*-1;
		this.ID = ""+ id;
	} 
	
	/** The constructor typically used when creating brand new agents */
	public  Agent (Model model, MarkovDecisionProcess mdp, AbstractEnvironmentBuilder environmentBuilder, EstimationParameters estimationParameters) throws IOException, MisspecifiedException {
		this(model, mdp, environmentBuilder, estimationParameters, null, 0);
	} 
	
	public void setID(String newID)
	{
		ID = newID;
	}
	
	public String getID()
	{
		return this.ID;
	}
	
	/**
	 * The location where the results have to be stored. The output file should have the name "[agentID].out", and contain a reference to the 
	 * agents MPD, environment, final value function (in DecimalNumberMatrix format) and optimalCuesToSample (a DecimalNumberMatrix). In essence, the output
	 * file should include sufficient information for the model to reconstruct the optimal policies when it needs to.
	 * @throws IOException 
	 */
	protected void writeOutput(Output output) throws IOException
	{
		// Write the tables
		if (!estimationParameters.outputFolder.isDirectory())
			throw new IOException("Directory outputFolderTables does not exist.");
		
		File outputTables = new File(estimationParameters.outputFolder.getAbsolutePath() +"\\" + output.filename + ".out");
		outputTables.createNewFile();
		
		FileOutputStream 	fop = new FileOutputStream(outputTables);
		ObjectOutputStream 	oos = new ObjectOutputStream(fop);
		oos.writeObject(output);
		oos.close();
		fop.close();
		System.out.println(Helper.timestamp() + " SAVED:\t\t\t Succesfully saved agent " + ID +"'s output to file: " + outputTables.getName());
		
	}
	
	protected boolean isDone(DecimalNumber delta, int iteration, long startTime) {
		
		if (estimationParameters.useStoppingCriteriaConvergence)
			if (delta.compareTo(estimationParameters.stoppingCriteriaConvergenceEpsilon)==-1) 	return true;
		
		if (estimationParameters.useStoppingCriteriaIterations)
			if (iteration >= estimationParameters.stoppingCriteriaIterationsMaximum-1)	return true;
		
		if (estimationParameters.useStoppingCriteriaTime)
			if (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - this.startTime) > this.maximumTimeInSeconds) 
				return true;
		return false;
	}
	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ID == null) ? 0 : ID.hashCode());
		result = prime * result + ((environment == null) ? 0 : environment.hashCode());
		result = prime * result + ((environmentBuilder == null) ? 0 : environmentBuilder.hashCode());
		result = prime * result + ((estimationParameters == null) ? 0 : estimationParameters.hashCode());
		result = prime * result + (int) (maximumTimeInSeconds ^ (maximumTimeInSeconds >>> 32));
		result = prime * result + ((mdp == null) ? 0 : mdp.hashCode());
		result = prime * result + ((model == null) ? 0 : model.hashCode());
		result = prime * result + (int) (startTime ^ (startTime >>> 32));
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
		Agent other = (Agent) obj;
		if (ID == null) {
			if (other.ID != null)
				return false;
		} else if (!ID.equals(other.ID))
			return false;
		if (environment == null) {
			if (other.environment != null)
				return false;
		} else if (!environment.equals(other.environment))
			return false;
		if (environmentBuilder == null) {
			if (other.environmentBuilder != null)
				return false;
		} else if (!environmentBuilder.equals(other.environmentBuilder))
			return false;
		if (estimationParameters == null) {
			if (other.estimationParameters != null)
				return false;
		} else if (!estimationParameters.equals(other.estimationParameters))
			return false;
		if (maximumTimeInSeconds != other.maximumTimeInSeconds)
			return false;
		if (mdp == null) {
			if (other.mdp != null)
				return false;
		} else if (!mdp.equals(other.mdp))
			return false;
		if (model == null) {
			if (other.model != null)
				return false;
		} else if (!model.equals(other.model))
			return false;
		if (startTime != other.startTime)
			return false;
		return true;
	}
	
	


		
	
}
