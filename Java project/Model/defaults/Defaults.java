package defaults;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;

import agent.AgentType;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import environment.EnvironmentBuilderFull;
import environment.EnvironmentBuilderLazy;
import environment.ValueDistributionType;
import estimationParameters.Optimizer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import markovDecisionProcess.CueEmissionType;
import staticManagers.ObserverManager;

//TODO: comment. Include that either (argument and type) have to specified, or (matrix). 
public class Defaults {		
	// MARKOV DECISION PROCESS PARAMETER DEFAULTS
	public final double 								maximumBudget;
	public final double 								valueMaximum ;
	public final double 								valueStep;
	public final double 								costOfSampling;
	public final boolean 								compoundInterruption;
	public final int 									maximumCues;
	public final String 								fitnessFunction;
	public final double 								discountRate;

	public final DecimalNumberMatrix					cueEmissionMatrix;
	public final CueEmissionType						cueEmissionType;
	public final double									cueEmissionArgument;
	public final String[]								cueLabels;

	// ENVIRONMENT DEFAULTS
	public ObservableList<EnvironmentBuilderLazy>		startingEnvironmentPopulationLazy;
	public ObservableList<EnvironmentBuilderFull>		startingEnvironmentPopulationFull;

	// ESTIMATION DEFAULTS
	public final AgentType 								initialAgent;

	// Which stopping criteria to use? Run a maximum number of time, run until the biggest difference between 
	//two iterations falls below epsilon, and/or run a maximum number of iterations? If so, what is the time, 
	// epsilon and/or maximum iteration?
	public final boolean 								useStoppingCriteriaMaximumTime;
	public final boolean 								useStoppingCriteriaConvergence;
	public final boolean 								useStoppingCriteriaIterations;
	public final double									stoppingCriteriaMaximumTime; //-1 represents "do not print"
	public final TimeUnit 								stoppingCriteriaMaximumTimeUnit;
	public final DecimalNumber							stoppingCriteriaConvergenceEpsilon;
	public final int 									stoppingCriteriaMaximumIterations; 

	// The smallest difference point between the expected outcomes of actions that the agent can still detect. That is,
	// differences in expected outcomes have to be larger than this point - otherwise, actions are considered equal.
	public final DecimalNumber							indifferencePoint; 
	
	// What, if any, optimzer should be used?
	public final Optimizer								optimizer;
	public final DecimalNumber							startingEstimates;
	public final boolean								batchUpdating; 

	// Static variables
	public static final int 							numberOfSimultaniousThreads = 3;
	public static final String							defaultOutputFolderEnvironment = System.getProperty("user.dir")+"\\Output\\";

	// Defaults for the output viewer - "View single environment" tab
	public static final String 	defaultInputFolderSingleEnvironment = System.getProperty("user.dir")+"\\Output\\";

	// Defaults for the output viewer - "Plot results" tab
	public static final String defaultInputFolderResults = System.getProperty("user.dir")+"\\Output\\";
	public static final String defaultSavePlotToFolder = System.getProperty("user.dir")+"\\Output\\";

	/** A very long winded constructor. It is set to private so that the factory has to be used. */
	public Defaults (DefaultFactory factory) {
		this.maximumBudget = 						factory.maximumBudget;
		this.valueMaximum = 						factory.valueMaximum;
		this.valueStep = 							factory.valueStep;
		this.costOfSampling =						factory.costOfSampling;
		this.compoundInterruption = 				factory.compoundInterruption;
		this.maximumCues = 							factory.maximumCues;
		this.fitnessFunction = 						factory.fitnessFunction;
		this.discountRate = 						factory.discountRate;

		this.cueLabels =							factory.cueLabels;
		this.cueEmissionMatrix = 					factory.cueEmissionMatrix;
		if (cueEmissionMatrix != null) {
			if (cueLabels.length+1 != cueEmissionMatrix.ncol()) 
				throw new IllegalArgumentException("Exception when reading in defaults: the number of cue labels does not match the number of columns specified.");
			this.cueEmissionType =					CueEmissionType.Manual;
		}
		else
			this.cueEmissionType = 					factory.cueEmissionType;

		this.cueEmissionArgument=					factory.cueEmissionArgument;

		this.startingEnvironmentPopulationLazy =	factory.startingEnvironmentPopulationLazy;
		this.startingEnvironmentPopulationFull =	factory.startingEnvironmentPopulationFull;
		this.initialAgent = 						factory.initialAgent;
		this.useStoppingCriteriaMaximumTime=		factory.useStoppingCriteriaMaximumTime;
		this.useStoppingCriteriaConvergence=		factory.useStoppingCriteriaConvergence;
		this.useStoppingCriteriaIterations= 		factory.useStoppingCriteriaIterations;
		this.stoppingCriteriaMaximumTime=			factory.stoppingCriteriaMaximumTime;
		this.stoppingCriteriaMaximumTimeUnit=		factory.stoppingCriteriaMaximumTimeUnit;
		this.stoppingCriteriaConvergenceEpsilon=	factory.stoppingCriteriaConvergenceEpsilon;
		this.stoppingCriteriaMaximumIterations=		factory.stoppingCriteriaMaximumIterations;
		this.optimizer=								factory.optimizer;
		this.indifferencePoint=						factory.indifferencePoint;
		this.startingEstimates=						factory.startingEstimates;
		this.batchUpdating=							factory.batchUpdating;

		// In most cases the field is not allowed to be null.
		// However, there are some exception: the cue emission matrix
		// can, for instance, be left null. However, at the very least
		// all primitive fields have to be non-null.
		try {
			for (Field f: Defaults.class.getDeclaredFields()) 
				if (f.get(this) == null && f.getType().isPrimitive() ) 
					throw new IllegalArgumentException("Expection in Defaults.constructor(): the factory provided had at least one null field (name: " + f.getName() + ")");

		} catch (IllegalArgumentException | IllegalAccessException e) {
			ObserverManager.notifyObserversOfError(e);
		}

	}

	public static class DefaultFactory{
		// MARKOV DECISION PROCESS PARAMETER DEFAULTS
		public double 						maximumBudget;
		public double 						valueMaximum ;
		public double 						valueStep;
		public double 						costOfSampling;
		public boolean 						compoundInterruption;
		public int 							maximumCues;
		public String 						fitnessFunction;
		public double 						discountRate;
		public DecimalNumberMatrix			cueEmissionMatrix;
		public CueEmissionType				cueEmissionType;
		public double						cueEmissionArgument;
		public String[]						cueLabels;

		// ENVIRONMENT DEFAULTS
		public ObservableList<EnvironmentBuilderLazy> 	startingEnvironmentPopulationLazy;
		public ObservableList<EnvironmentBuilderFull> 	startingEnvironmentPopulationFull;

		// ESTIMATION DEFAULTS
		public AgentType 					initialAgent;
		public boolean 						useStoppingCriteriaMaximumTime;
		public boolean 						useStoppingCriteriaConvergence;
		public boolean 						useStoppingCriteriaIterations;
		public double						stoppingCriteriaMaximumTime; 
		public TimeUnit 					stoppingCriteriaMaximumTimeUnit;
		public DecimalNumber				stoppingCriteriaConvergenceEpsilon;
		public int 							stoppingCriteriaMaximumIterations;
		public Optimizer					optimizer;
		public DecimalNumber				indifferencePoint;
		public DecimalNumber				startingEstimates;
		public boolean						batchUpdating;

		public DefaultFactory setMaximumBudget(double maximumBudget) {
			this.maximumBudget = maximumBudget;			return this;
		}
		public DefaultFactory setValueMaximum(double valueMaximum) {
			this.valueMaximum = valueMaximum;			return this;
		}
		public DefaultFactory setValueStep(double valueStep) {
			this.valueStep = valueStep;			return this;
		}
		public DefaultFactory setCostOfSampling(double costOfSampling) {
			this.costOfSampling = costOfSampling;			return this;
		}
		public DefaultFactory setCompoundInterruption(boolean compoundInterruption) {
			this.compoundInterruption = compoundInterruption;			return this;
		}
		public DefaultFactory setMaximumCues(int maximumCues) {
			this.maximumCues = maximumCues;			return this;
		}
		public DefaultFactory setFitnessFunction(String fitnessFunction) {
			this.fitnessFunction = fitnessFunction;			return this;
		}
		public DefaultFactory setDiscountRate(double discountRate) {
			this.discountRate = discountRate;			return this;
		}
		public DefaultFactory setCueEmissionMatrix(DecimalNumberMatrix cueEmissionMatrix) {
			this.cueEmissionMatrix = cueEmissionMatrix;			return this;
		}
		public DefaultFactory setCueEmissionType(CueEmissionType cueEmissionType) {
			this.cueEmissionType = cueEmissionType;			return this;
		}
		public DefaultFactory setCueEmissionArgument(double cueEmissionArgument) {
			this.cueEmissionArgument = cueEmissionArgument;			return this;
		}
		public DefaultFactory setCueLabels(String... cueLabels) {
			this.cueLabels = cueLabels; return this;
		}
		public DefaultFactory setStartingEnvironmentPopulationFull(ObservableList<EnvironmentBuilderFull> startingEnvironmentPopulation) {
			this.startingEnvironmentPopulationFull = startingEnvironmentPopulation;			return this;
		}
		public DefaultFactory setStartingEnvironmentPopulationLazy(ObservableList<EnvironmentBuilderLazy> startingEnvironmentPopulation) {
			this.startingEnvironmentPopulationLazy = startingEnvironmentPopulation;			return this;
		}
		public DefaultFactory setInitialAgent(AgentType initialAgent) {
			this.initialAgent = initialAgent;			return this;
		}
		public DefaultFactory setUseStoppingCriteriaMaximumTime(boolean useStoppingCriteriaMaximumTime) {
			this.useStoppingCriteriaMaximumTime = useStoppingCriteriaMaximumTime;			return this;
		}
		public DefaultFactory setUseStoppingCriteriaConvergence(boolean useStoppingCriteriaConvergence) {
			this.useStoppingCriteriaConvergence = useStoppingCriteriaConvergence;			return this;
		}
		public DefaultFactory setUseStoppingCriteriaIterations(boolean useStoppingCriteriaIterations) {
			this.useStoppingCriteriaIterations = useStoppingCriteriaIterations;			return this;
		}
		public DefaultFactory setStoppingCriteriaMaximumTime(double stoppingCriteriaMaximumTime) {
			this.stoppingCriteriaMaximumTime = stoppingCriteriaMaximumTime;			return this;
		}
		public DefaultFactory setStoppingCriteriaMaximumTimeUnit(TimeUnit stoppingCriteriaMaximumTimeUnit) {
			this.stoppingCriteriaMaximumTimeUnit = stoppingCriteriaMaximumTimeUnit;			return this;
		}
		public DefaultFactory setStoppingCriteriaConvergenceEpsilon(DecimalNumber stoppingCriteriaConvergenceEpsilon) {
			this.stoppingCriteriaConvergenceEpsilon = stoppingCriteriaConvergenceEpsilon;			return this;
		}
		public DefaultFactory setStoppingCriteriaMaximumIterations(int stoppingCriteriaMaximumIterations) {
			this.stoppingCriteriaMaximumIterations = stoppingCriteriaMaximumIterations;			return this;
		}
		public DefaultFactory setOptimizer(Optimizer optimizer) {
			this.optimizer=optimizer; return this;
		}
		public DefaultFactory setIndifferencePoint (DecimalNumber indifferencePoint) {
			this.indifferencePoint = indifferencePoint; return this;
		}
		public DefaultFactory setStartingEstimates(DecimalNumber startingEstimates) {
			this.startingEstimates = startingEstimates;			return this;
		}
		public DefaultFactory setBatchUpdating(boolean batchUpdating) {
			this.batchUpdating = batchUpdating;			return this;
		}
		public Defaults build() {
			return new Defaults(this);
		}
	}

	public static final Defaults mainDefaults = mainDefaults();
	private static Defaults mainDefaults() { 
		try {
			DefaultFactory df = new DefaultFactory()
					.setMaximumBudget(100)
					.setValueMaximum(20)
					.setValueStep(0.2)
					.setCostOfSampling(0.2)
					.setCompoundInterruption(false)
					.setMaximumCues(10)
					.setFitnessFunction("x")
					.setDiscountRate(0.95)
					.setCueEmissionMatrix(null)
					/*		new DecimalNumberMatrix( 
						//						Resource value		p(Cue 1| value) 		p(Cue 2| value)
						new DecimalNumberArray(	-10,				0.99,					0.01),
						new DecimalNumberArray(	-9,					0.95,					0.05),
						new DecimalNumberArray(	-8,					0.90,					0.10),
						new DecimalNumberArray(	-7,					0.85,					0.15),
						new DecimalNumberArray(	-6,					0.80,					0.20),
						new DecimalNumberArray(	-5,					0.75,					0.25),
						new DecimalNumberArray(	-4,					0.70,					0.30),
						new DecimalNumberArray(	-3,					0.65,					0.35),
						new DecimalNumberArray(	-2,					0.60,					0.40),
						new DecimalNumberArray(	-1,					0.55,					0.45),
						new DecimalNumberArray(	 0,					0.50,					0.50),
						new DecimalNumberArray(	 1, 				0.45,					0.55),
						new DecimalNumberArray(	 2, 				0.40,					0.60),
						new DecimalNumberArray(	 3, 				0.35,					0.65),
						new DecimalNumberArray(	 4, 				0.30,					0.70),
						new DecimalNumberArray(	 5, 				0.25,					0.75),
						new DecimalNumberArray(	 6, 				0.20,					0.80),
						new DecimalNumberArray(	 7, 				0.15,					0.85),
						new DecimalNumberArray(	 8, 				0.10,					0.90),
						new DecimalNumberArray(	 9, 				0.05,					0.95),
						new DecimalNumberArray(	 10, 				0.01,					0.99)
							))*/

					.setCueEmissionType(CueEmissionType.Normal)
					.setCueEmissionArgument(15)
					.setCueLabels("Negative cue", "Positive cue")
					// NOTE: this creates an EnvironmentBuilderFull that does not have initialized distributions. During runtime
					// you first need to call setMDPBuilder()!
					/*.setStartingEnvironmentPopulationFull(FXCollections.observableArrayList(
							// NOTE: this creates an EnvironmentBuilderFull that does not have initialized distributions. During runtime
							// you first need to call setMDPBuilder()!
							new EnvironmentBuilderFull(	 5,	5, 	ValueDistributionType.Uniform,
									5,	5,	ValueDistributionType.Normal,
									0, new DecimalNumberMatrix(9,3,true, 
										// value				p(Resource value = value) 				p(Extrinsic event = value)
										-2.0,					0.5,									0.5,
										-1.5,					0,										0,
										-1.0,					0,										0,
										-0.5,					0,										0,
										0.0,					0,										0,
										0.5,					0,	  									0,
										1.0,					0,										0,
										1.5,					0,										0,
										2.0,					0.5,									0.5

						)) ))*/
					.setStartingEnvironmentPopulationFull(FXCollections.observableArrayList(
							// NOTE: this creates an EnvironmentBuilderFull that does not have initialized distributions. During runtime
							// you first need to call setMDPBuilder()!
							new EnvironmentBuilderFull(	 0,	5, 	ValueDistributionType.Normal, 0,	5,	ValueDistributionType.Normal,	0, null )
							))
					.setStartingEnvironmentPopulationLazy(FXCollections.observableArrayList())
					.setInitialAgent(AgentType.VALUE_ITERATOR)
					.setUseStoppingCriteriaMaximumTime(false)
					.setUseStoppingCriteriaConvergence(true)
					.setUseStoppingCriteriaIterations(true)
					.setStoppingCriteriaMaximumTime(-1)
					.setStoppingCriteriaMaximumTimeUnit(TimeUnit.HOURS)
					.setStoppingCriteriaConvergenceEpsilon(new DecimalNumber(0.001))
					.setStoppingCriteriaMaximumIterations(100)
					.setOptimizer(Optimizer.Lossy)
					.setIndifferencePoint(new DecimalNumber(0.0000000001))
					.setStartingEstimates(new DecimalNumber(0))
					.setBatchUpdating(false);

			return df.build();
		}
		catch (Exception e) { e.printStackTrace(); return null;}
	}

}

