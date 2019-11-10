package start;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import agent.Agent;
import agent.AgentType;
import agent.Output;
import agent.ValueFunction;
import agent.ValueIterator.ValueIterator;
import decimalNumber.DecimalNumber;
import environment.AbstractEnvironmentBuilder;
import environment.EnvironmentBuilderFull;
import environment.EnvironmentBuilderLazy;
import estimationParameters.EstimationBuilder;
import estimationParameters.EstimationParameters;
import estimationParameters.Optimizer;
import helper.Helper;
import helper.Helper.ImpossibleStateException;
import helper.Helper.MisspecifiedException;
import helper.ImmutableArray;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import markovDecisionProcess.MarkovDecisionProcess;
import markovDecisionProcess.MarkovDecisionProcessBuilder;
import staticManagers.ObserverManager;
import staticManagers.OutputFileManager;
import staticManagers.OutputShadow;
import window.FrameController;

/**
 * The Model of the program deals with all actual computations. It contains all the Builder objects that are used when
 * the user specifies it inputs, it deals with the creating/scheduling/computation of all agent threads, and it stores
 * all data. In a very meaningful way, the Model IS the program. The only things that the model does not do is communication
 * to the user. All communication (i.e., the graphical user interface) is handled by a FrameController object, which is 
 * created in the main() function.
 */
public class Model extends Application{
	
	@SuppressWarnings("serial")
	public static class InvalidFitnessInputException extends RuntimeException { public InvalidFitnessInputException(String message) {     super(message);  }};
	
	private FrameController fc;
	
	private final OutputFileManager	outputFileManager;
	
	private long startTime; //used to determine runtime and estimated time left
	private Date startDate; //used to make file names correspond to the date when a run started
	private int numberOfAgentsFinished;
	
	// When creating a .csv of all output files, the program has to first read in an Output file, then extract
	// all relevant information for that file, and finally write this information to a .csv file. This requires
	// a lot of IO, and might not be a fast process. To make use of potential multiple cores/threads, I have implemented
	// a producer (reads the .output file, extracts information) and consumer (takes information and writes to disk 
	// after many output files have been read in) pattern to speed things up.
	private static int numberOfConsumersDuringCSVWriting =  1;//1+ (Runtime.getRuntime().availableProcessors()-2)/4;
	private static int numberOfProducersDuringCSVWriting =  Runtime.getRuntime().availableProcessors()-1;//1+ (Runtime.getRuntime().availableProcessors()-2)/4*3;
	
	public static void main (String[] args)
	{
		// Set the number locale to US standards (decimal point, not a decimal comma)
		Locale.setDefault(new Locale("en", "US"));
		launch(); 
	
	}
	
	@Override
	public void start(Stage primaryStage)  {

	}
		
	public Model ()
	{
		// Create the output file manager
		this.outputFileManager = new OutputFileManager();
		
		// Call the constructor for the frame
		this.fc = new FrameController(this);	
	}
	

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////// 		Specification, construction and running of agents 			/////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	private ExecutorService simulationExecutorService;
	
	/** Creates a file containing all runtime parameters (MDP, environment list and estimationBuilder) in the output folder*/
	private void writeRuntimeParametersFile(MarkovDecisionProcessBuilder MDPBuilder, 
			ObservableList<EnvironmentBuilderFull> environmentPoolFullySpecified, 
			ObservableList<EnvironmentBuilderLazy> environmentPoolLazySpecified, 
			EstimationBuilder estimationBuilder,
			File outputDirectory) {
		
		StringBuilder runtimeParameterBuilder = new StringBuilder();
		runtimeParameterBuilder.append("Markov decision process parameters\n" + Helper.repString("=", 50)+"\n"+MDPBuilder);
		runtimeParameterBuilder.append("\n\n\nEstimation parameters\n" + Helper.repString("=", 50)+"\n"+estimationBuilder);
		runtimeParameterBuilder.append("\n\n\nFully specified environments\n" + Helper.repString("=", 50)+"\n");
		if (environmentPoolFullySpecified.size() ==0)
			runtimeParameterBuilder.append("<None specified>");
		for (EnvironmentBuilderFull eb: environmentPoolFullySpecified) runtimeParameterBuilder.append("\nNext fully specified environment\n"+Helper.repString("-", 25)+eb);
		
		runtimeParameterBuilder.append("\n\n\nLazy generated environments\n" + Helper.repString("=", 50)+"\n");
		if (environmentPoolLazySpecified.size() ==0)
			runtimeParameterBuilder.append("<None specified>");
		for (EnvironmentBuilderLazy ebf: environmentPoolLazySpecified) runtimeParameterBuilder.append("\n"+ebf);

		String runtimeParameters = runtimeParameterBuilder.toString().replaceAll("\n", System.lineSeparator());
				
		File fileToSaveTo = new File(outputDirectory.getAbsolutePath()+"\\" + "RUNTIME PARAMETERS.txt");
		try (FileOutputStream 	fop = new FileOutputStream(fileToSaveTo);
				ObjectOutputStream 	oos = new ObjectOutputStream(fop);){
			fileToSaveTo.createNewFile();
			oos.writeObject(runtimeParameters);
			oos.close();
			fop.close();
		} catch (IOException e) {
			ObserverManager.notifyObserversOfError(e);
		}
	}
	
	/**<pre>
	 * Create the specified agents, living in the specified environments, solving the task specified in the MDP. 
	 * 
	 * Prior to running an agent, the model creates three new directories in the specified folder. The first
	 * directory is the name of the run (with a name [day]-[month]-[year]_[hour]-[minutes] + an optional (x) if the name is not unique).
	 * In this directory the two others are created. The first is named "outputFolderTables", the second is called "outputFolderPolicies".
	 * 
	 * The agents all get a separate thread to run on, which are managed by the Model's executorService. When these
	 * agents have finished their run, they save their value function (a DecimalNumberMatrix) and their optimalNumberOfCuesSampled
	 * (likewise a DecimalNumberMatrix) to the newly created folder "outputFolderTables". These two objects are stored in a wrapper
	 * object called "OutputTables", which also contains a reference to the agent's MDP and Environment.  
	 * The saved output has the agent's name, and a suffix of .tab
	 * 
	 * Similarly, they write an arrayList consisting of DecisionTree's that have already gone through a policyPass() to "optimalPolicies" to
	 * the outputFolderPolicies directory. These decision tree's denote the optimal policy to follow, given the environment, MDP, and a starting budget. 
	 * The saved object has the agent's name, and a suffix of .pol. 
	 * 
	 * This separation between tables and policies is done because the policies tend to be big objects (roughly 1 MB in size). At a later
	 * stage the value functions and the optimal policy functions have to be combined into one big table, and hence, have to be read in again.
	 * However, reading in many, many optimal policies (which might not ever be used), is very slow, and places large strain on the working
	 * memory capacity of the machine. 
	 */
	public void runSimulation(MarkovDecisionProcessBuilder MDPBuilder, 
			ObservableList<EnvironmentBuilderFull> environmentPoolFullySpecified, 
			ObservableList<EnvironmentBuilderLazy> environmentPoolLazySpecified, 
			EstimationBuilder estimationBuilder) 
	{
		try 
		{
			// Provide some output
			System.out.println("\n\n" + Helper.repString("=", 100));
			System.out.println(Helper.timestamp() + " START: Model has been notified that a simulation can be started.");
			System.out.println("\n\n" + Helper.repString("-", 100));

			// Create the directories in which to save the output
			this.startTime = System.nanoTime();
			this.startDate = new Date();
			File outputFolder = createDirectories(estimationBuilder.getOutputFolder(), "");

			// Save a copy of all builders to a .txt file in the output folder
			writeRuntimeParametersFile(MDPBuilder, environmentPoolFullySpecified, environmentPoolLazySpecified, estimationBuilder,outputFolder);
			
			// Use the MarkovDecisionProcessBuilder to create a MarkovDecisionProcess
			MarkovDecisionProcess mdp = new MarkovDecisionProcess(MDPBuilder);
			System.out.println(mdp);

			// Make sure that there are environments specified (and count how many)
			int numberOfEnvironments = environmentPoolFullySpecified.size() + environmentPoolLazySpecified.size();
			if (numberOfEnvironments == 0)
				throw new IllegalStateException("Exception in Model.runSimulation(): no (lazy or fully) environments have been specified. Please specify at least one environment. Termination on simulation is halted.");
			
			// Create a single list in which both lazy and fully specified environmentBuilders are stored.
			ArrayList<AbstractEnvironmentBuilder> environmentBuilders = new ArrayList<>(numberOfEnvironments);
			environmentBuilders.addAll(environmentPoolFullySpecified);
			environmentBuilders.addAll(environmentPoolLazySpecified);
			System.out.println(Helper.timestamp() + "\t Listed a total of "+ numberOfEnvironments +" environments to create.");

			// Fixate the estimationParameters
			EstimationParameters estimationParameters = new EstimationParameters(estimationBuilder, outputFolder);
			
			// For each environment: create an agent 
			ArrayList<Agent> agents = new ArrayList<>();
			int idCounter = 1;
			System.out.println(Helper.timestamp() + " INITIALIZING " + environmentBuilders.size() + " agents of type " + estimationBuilder.getAgentType() + "...");
			for (AbstractEnvironmentBuilder eb: environmentBuilders)
			{
				Agent newAgent = null;
				if (estimationBuilder.getAgentType() == AgentType.VALUE_ITERATOR)
					newAgent = new ValueIterator(this, mdp, eb, estimationParameters, null,0);
				else
					throw new IllegalStateException("Exception in Model.runSimulation(): agent of type " + estimationBuilder.getAgentType() + " is not yet implemented");

				newAgent.setID(""+idCounter++);
				agents.add(newAgent);
			}
			int totalNumberOfAgents = agents.size();

			System.out.println(Helper.timestamp() + "\t DONE initializing agents.");
			System.out.println("\n\n\n" + Helper.repString("=", 100) +"\n-  RUN. \n" + Helper.repString("=", 100) + "\n");

			// Run the new agents
			//this.runningModel = true;
			numberOfAgentsFinished= 0;
			simulationExecutorService = Executors.newFixedThreadPool(estimationBuilder.getNumberOfSimultaniousThreads());

			for (Agent a: agents)
				simulationExecutorService.submit(a);
			simulationExecutorService.shutdown();

			// Remove the pointer to the agents
			agents = null;

			// Tell the main window stage to show the progress bar:
			this.fc.showSimulation(startTime, totalNumberOfAgents);
		} catch (IOException | MisspecifiedException | ImpossibleStateException e) {
			ObserverManager.notifyObserversOfError(e);
		} 
	}
	
	/**
	 * Cancel all ongoing agents.
	 */
	public void cancelSimulation()
	{
		if (simulationExecutorService != null)
			this.simulationExecutorService.shutdownNow();
		fc.stopShowingSimulation();
	}
	
	/**
	 * Called by an agent upon completion - this function updates the view.
	 * @param agent
	 */
	public void notifyAgentIsDone(Agent agent, boolean completedRun)
	{
		numberOfAgentsFinished++;
		if (completedRun)
			System.out.println(Helper.timestamp() + " SUCCES:  Agent " + agent.getID() +" has converged. In total " + numberOfAgentsFinished +" agents have finished.");
		else
			System.err.println(Helper.timestamp() + " FAILURE: Agent " + agent.getID() +" has failed to converged. In total " + numberOfAgentsFinished +" agents have finished.");
		
		fc.agentFinished(numberOfAgentsFinished);
	
	}
	
	/**
	 * Creates the  directory in which to save the outputs produced by the agents (the agents do the saving of the actual files themselves - this does not involve the model).
	 * The directory name is [prefix] [day]-[month]-[year]_[hour]-[minutes] + an optional (x) if the name is not unique).
	 * @param directory
	 * @return  the directory with the name [prefix] [day]-[month]-[year]_[hour]-[minutes] + an optional (x)
	 */
	private File createDirectories(File specifiedOutputDirectory, String prefix)
	{
		// Generate the filename
		String date = prefix + "Results model started at " + new SimpleDateFormat("dd-MM-yyyy_HH'h'-mm'm'").format(this.startDate);
		String outputDirectoyName = specifiedOutputDirectory.getAbsolutePath()+"\\"+date ;
		File outputDirectory = new File(outputDirectoyName);
		
		// Ensure that the filename is unique. If it is not, change the name by appending (x) at the end
		int counter = 1;
		while (outputDirectory.isDirectory())
		{
			String alternateFilename = outputDirectoyName+ "(" +counter++ + ")";
			outputDirectory = new File(alternateFilename);
		}
		
		// Create the newly minted directory
		outputDirectory.mkdir();
		
		return outputDirectory;
	}

	/////////////////////////////////////////////////////////////////////
	//////////////// 		Agent retraining 			/////////////////
	/////////////////////////////////////////////////////////////////////
	
	/** A class that wraps all possible specifications for retraining. 
	 * Note that this class has a lot of separate fields. Specifically, separate parameters for all MarkovDecisionProcessBuilder/environmentBuilder/estimationBuilder fields. 
	 * That is because the builder objects are created by the Model, NOT by the view. 
	 * 
	 * The reason that these parameters are not packaged into Builders is that these values will be different for each agent. 
	 * That is, each agent will have to build its own MarkovDecisionProcess, Environment and EstimationParameters based
	 * on the arguments passed to retrainAgents(). Because these computations are rather heavy and require multi-threading, 
	 * they are handled by the Model (specifically, by the same thread in which the agent will run) and not by the View directly (one of the 
	 * design principles of this program is that the Model does all the heavy lifting). 
	 * 
	 * To make it easy to transport the new specifications between model objects, this wrapper is created.
	 * */
	
	public static class RetrainingSpecifications{
		// Change MDP tab
		public final String newFitnessFunction;

		// Change environment parameters (not implemented yet)

		// Change estimation procedure parameters
		public final AgentType newType;
		public final boolean useStoppingCriteriaTime;
		public final double maximumTime;
		public final TimeUnit maximumTimeUnit;
		public final boolean useStoppingCriteriaConvergence;
		public final DecimalNumber epsilon;
		public final boolean useStoppingCriteriaIterations;
		public final int additionalIterations;

		public final Optimizer newOptimizer;
		public final DecimalNumber indifferencePoint;
		public final boolean batchUpdating;
					
		public RetrainingSpecifications(
				// Change MDP tab
				String newFitnessFunction,
				
				// Change environment parameters (not implemented yet)
				
				// Change estimation procedure parameters
				AgentType newType, 
				boolean useStoppingCriteriaTime,
				double maximumTime,
				TimeUnit maximumTimeUnit,
				boolean useStoppingCriteriaConvergence,
				DecimalNumber epsilon,
				boolean useStoppingCriteriaIterations,
				int additionalIterations, 
				
				Optimizer newOptimizer,
				DecimalNumber indifferencePoint,
				boolean batchUpdating) {
			this.newFitnessFunction = newFitnessFunction;
			
			this.newType = newType;
			this.useStoppingCriteriaTime = useStoppingCriteriaTime;
			this.maximumTime = maximumTime;
			this.maximumTimeUnit = maximumTimeUnit;
			this.useStoppingCriteriaConvergence = useStoppingCriteriaConvergence;
			this.epsilon = epsilon;
			this.useStoppingCriteriaIterations = useStoppingCriteriaIterations;
			this.additionalIterations = additionalIterations;
			
			this.newOptimizer = newOptimizer;
			this.indifferencePoint = indifferencePoint;
			this.batchUpdating = batchUpdating;
			
		}
	}
	
	private class RetrainingWorker implements Runnable {
		private Model model;
		private RetrainingSpecifications specs;
		private File outputFolder; 
		private ArrayBlockingQueue<OutputShadow> shadowsToTrain;;
		private AtomicInteger agentsRetrained;
		
		public RetrainingWorker(Model model, RetrainingSpecifications specs, File outputFolder, ArrayBlockingQueue<OutputShadow> shadowsToTrain, AtomicInteger agentsRetrained ) {
			this.model=model;
			this.specs = specs;
			this.outputFolder = outputFolder;
			this.shadowsToTrain = shadowsToTrain;
			this.agentsRetrained = agentsRetrained;
		}
		
		// During run: until the Queue is empty:
		// 		Take an outputShadow from the queue, 
		//		Read in the MDP/Environment/EstimationParameter
		//		Change these three objects to fit the new specifications
		//		Recreate a new agent
		//		Train that agent
		public void run() {
			while (!shadowsToTrain.isEmpty()) {
				
				while (true) {
					try {
						// Try to the get new shadow to inflate
						OutputShadow shadow = shadowsToTrain.poll();

						// Break to get out of the loop: the easiest (and computationally cheapest) way of ensuring thread safety. 
						if (shadow == null)
							break;

						// Increment the ID number
						int id = agentsRetrained.incrementAndGet();

						// Read in the shadow to a fully fledged Output object (i.e, inflate the shadow)
						Output output = OutputShadowToOutput(shadow);

						// Recreate the MDP
						MarkovDecisionProcessBuilder mdpBuilder = output.mdp.toBuilder();
						mdpBuilder.FITNESS_FUNCTION = specs.newFitnessFunction;
						MarkovDecisionProcess newMDP = new MarkovDecisionProcess(mdpBuilder);

						// Recreate the Environment
						AbstractEnvironmentBuilder newEnvironmentBuilder = output.environment.toBuilder();

						// Recreate the EstimationParameters
						EstimationBuilder estimationBuilder = output.estimationParameters.toBuilder();
						estimationBuilder.setOutputFolder(outputFolder);
						estimationBuilder.setAgentType(specs.newType);
						estimationBuilder.setUseStoppingCriteriaTime(specs.useStoppingCriteriaTime);
						estimationBuilder.setStoppingCriteriaTimeMaximum(specs.maximumTime);
						estimationBuilder.setStoppingCriteriaTimeUnit(specs.maximumTimeUnit);
						estimationBuilder.setUseStoppingCriteriaConvergence(specs.useStoppingCriteriaConvergence);
						estimationBuilder.setStoppingCriteriaConvergenceEpsilon(specs.epsilon);
						estimationBuilder.setUseStoppingCriteriaIterations(specs.useStoppingCriteriaIterations);
						estimationBuilder.setStoppingCriteriaIterationsMaximum(output.finalIteration+specs.additionalIterations);
						estimationBuilder.setOptimizer(specs.newOptimizer);
						estimationBuilder.setIndifferencePoint(specs.indifferencePoint);
						estimationBuilder.setBatchUpdating(specs.batchUpdating);
						EstimationParameters newEstimationParameters = new EstimationParameters(estimationBuilder, outputFolder);

						int finalIteration = output.finalIteration;
						ValueFunction finalValueFunction = output.finalValueFunction;

						// Set output to null - it takes up way to much space to maintain otherwise
						output = null;

						// Recreate the agent
						Agent a;
						if (specs.newType == AgentType.VALUE_ITERATOR)
							a = new ValueIterator(
									model, 
									newMDP, 
									newEnvironmentBuilder, 
									newEstimationParameters, 
									finalValueFunction,
									finalIteration); 
						else
							throw new IllegalStateException("Exception in Model.retrainAgents(): agent of type " + estimationBuilder.getAgentType() + " is not yet implemented");

						// Retrain the agent
						ObserverManager.makeToast("Recreated agent " + id + ". Retraining..." );
						a.setID(""+id);
						a.run();
						
						// After this agent is done: tell the System that it's a good time for a GC and take a break (1 sec) 
						System.gc();
						System.err.println("Thread " + this + " is taking a small break...");
						Thread.sleep(1000);

					}  catch (Exception e) { ObserverManager.notifyObserversOfError(e); break; }
				}
			}
		}
	}


	/** 
	 * Retrains (i.e,. use the value function of a previous agent) a set of agents (stored in the outputShadows) with the specified conditions.
	 * Assumes all arguments are correctly specified - i.e., no empty lists, non-existing folder etc.
	 * 
	 * 
	 */
	public void retrainAgents(
			RetrainingSpecifications specs, 
			File outputFolderDirectory, 
			int threadNumber,
			ArrayList<OutputShadow> shadowsToRetrain
			) {
		
		// Set up the fields used by the RetrainingWorkers and the View 
		this.startTime = System.nanoTime();
		this.startDate = new Date();
		File outputFolder = createDirectories(outputFolderDirectory, "RETRAINED ");
		ArrayBlockingQueue<OutputShadow> queue = new ArrayBlockingQueue<>(shadowsToRetrain.size()); 
		queue.addAll(shadowsToRetrain);
		AtomicInteger agentsRetrained = new AtomicInteger(0);
		
		// Tell the View to show the progress bar:
		numberOfAgentsFinished = 0;
		Platform.runLater(new Runnable() {public void run() {fc.showSimulation(startTime, shadowsToRetrain.size());}});
		
		// Create the RetrainingWorkers
		RetrainingWorker[] workers = new RetrainingWorker[threadNumber];
		for (int i = 0; i < threadNumber; i++)
			workers[i] = new RetrainingWorker(this, specs, outputFolder, queue, agentsRetrained);
	
		// Run the new agents
		simulationExecutorService = Executors.newFixedThreadPool(threadNumber);
		for (RetrainingWorker w: workers) simulationExecutorService.submit(w);
		simulationExecutorService.shutdown();

	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////// 		Post agent computation - writing to file, plotting etc 			/////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 *Loads all .out files in the directory (and sub-directories, depth-first), and extracts a shadow from each 
	 *output file. A call to this function will block until all shadows are found. Strongly recommended to run 
	 *a call in a separate thread. Passes a process update to the Stage, indicating how many shadow have been 
	 *read in thus far.
	 *
	 *Returns null if an exception is encountered.
	 * @param directory
	 * @param includeSubDirectories
	 * @return
	 */
	public ArrayList<OutputShadow> createOutputShadows(File directory, boolean includeSubDirectories) 
	{
		try {
			return this.outputFileManager.createShadows(directory, includeSubDirectories);
		} catch (IOException | InterruptedException | ExecutionException e) {
			ObserverManager.notifyObserversOfError("Exception encountered", "Java has encountered an exception when creating shadows of the output files. See details for the exception.", e);
			return null;
		}
	}
	
	/**
	 * Cancel the reading in of output shadows.
	 */
	public void stopCreatingShadows(){
		try {
			this.outputFileManager.stopCreatingShadows();
		} catch (InterruptedException e) {
			ObserverManager.notifyObserversOfError("Exception encountered", "Java has encountered an exception when trying to interrupt the reading of the shadows of output files. See details for more information.", e);
		}
	}
	
	/**
	 * Takes in an output shadow, reads the corresponding .out file, and returns the Output object stored
	 * in that .out file.
	 * 
	 * return null if an exception is encountered.
	 * @param shadow
	 * @return
	 */
	public Output OutputShadowToOutput(OutputShadow shadow) 
	{
		try {
			return this.outputFileManager.OutputShadowToOutput(shadow);
		} catch (ClassNotFoundException | IOException e) {
			ObserverManager.notifyObserversOfError("Exception encountered", "Java has encountered an exception when converting a shadow to an output. See details for more information.", e);
			return null;
		}
	}
	
	/**
	 * Write all loaded OutputShadows to a newly created .csv file. The filename will be appended with (x) if the filename already exists. This function will block until all .out files are writen to the .csv file.
	 * 
	 * This function uses a multi-threaded consumers/producers pattern. The call does, however, block until all
	 * producers and consumers have finished. Therefore it is strongly advised to run a call to this function in
	 * a separate thread. 
	 * 
	 * Another advice is to have a higher producer-to-consumer ratio, since producers will take longer than consumers.
	 * @param pathToDirectory
	 * @param filename
	 * @param consumers
	 * @param producers
	 * @return
	 */
	public void writeCSV(String pathToDirectory, String filename, ImmutableArray<String> cueLabels)
	{
		String finalFilename = null;
		try {
			finalFilename = this.outputFileManager.writeCSV(pathToDirectory, filename, numberOfConsumersDuringCSVWriting, numberOfProducersDuringCSVWriting, cueLabels);
		} catch (IOException | InterruptedException e) {
			ObserverManager.notifyObserversOfError("Exception encountered", "Java has encountered an exception when writing to the .CSV file. See details for more information", e);
		}
		ObserverManager.makeToast("Saved file to: " + finalFilename);
	}
	
	/**
	 * Returns all .csv files in the specified directory
	 * @param directory
	 * @return
	 */
	public ArrayList<File> getCSVFiles(File directory) 
	{
		try 
		{
			System.out.println(Helper.timestamp() + " Reading all .csv files in directory: " + directory.getAbsolutePath());
			if (!directory.isDirectory())
				throw new IOException("Error in reading in all .csv files in the specified folder: folder is not a directory.");

			ArrayList<File> files = new ArrayList<>();
			for (File f: directory.listFiles())
				if (f.getAbsolutePath().endsWith(".csv"))
					files.add(f);

			return files;
			
		} catch (IOException e)		{
			ObserverManager.notifyObserversOfError("Exception encountered", "Java has encountered an exception when reading in a .CSV file. See details for more information", e);
			return null;
		}
	}
	
	
	
}


