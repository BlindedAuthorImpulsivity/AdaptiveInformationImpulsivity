package staticManagers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import agent.Output;
import agent.ValueIterator.ValueIteratorOutput;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import environment.Environment;
import helper.Helper;
import helper.ImmutableArray;
import markovDecisionProcess.MarkovDecisionProcess;

/**
 * Here is a problem:
 * 
 * If we load all the .out files into the working memory simultaneously, we run into some serious working memory
 * space issues (each files is at least 500 KB, a good run of the model might produce as many as a million 
 * .out files - these numbers cannot be stored on a 8gb - or 80 gb - RAM system.
 * 
 * Hence, we cannot load in all the .out files simultaneously. However, we do need to present the results to the user,
 * who at the very least needs to be able to get a .csv file containing information from all the agents in a run. In
 * addition, we have to be able to load an input file completely, so that we can create and plot the optimal policy
 * for that agent.
 * 
 * We thus have to both be able to read in all the files at some time, but cannot read in all the files simultaneously.
 * 
 * The solution for this problem is as follows. Rather than reading in all .out files once, we can read them all in first,
 * and save only a 'shadow' of each .out file. This shadow only the barest
 * of information about the agent: its file name, the agent type, the environmental harshness and the environmental unpredictability.
 * This information is shown in a table, allowing the user to select an agent. Note that we have to be able to 
 * find the file again, so this shadow should also contain a absolute path referencing the .out file.
 * 
 * Next, we show all shadows in a table. When the user selects a shadow, we read in the .out file again
 * and use the full information from the .out file to construct the optimal policy and show the MDP.
 * 
 * When the user requests an .csv file to be made, we read in all .out files again, extract their value functions
 * and optimal cues sampled tables, and combine the result into a single DecimalNumberMatrix. We can do this reading
 * in (producing) and aggregating (consuming) using multiple producer and consumer threads.
 * 
 * Since these are not all straight-forward operations, I have created a seperate class dealing with the implementation of
 * these processes.
 * 
 * Some other notes: the OutputFileManager should throw all exceptions to the Model that calls it (only the Model should be able to
 * call the OutputFileManager). It does, however, need access to the Model's ObserverManager, so that it can provide feedback
 * to the user to what extend the computations are done.
 *
 */
public class OutputFileManager {

	public final static boolean includeMDPInOutput = false;
	public OutputFileManager()	{
		shadows = new ArrayList<>();
		OutputFileManager.setFieldsToSave(includeMDPInOutput);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	INITIALIZING THE MANAGER 	//////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	private static final ArrayList<Field> MDPFieldsToSave = new ArrayList<>();
	private static final ArrayList<Field> environmentFieldsToSave = new ArrayList<>();
	/**
	 * Finds and adds all public, final, non-transient and non-static fields in the MDP and in the environment classes to the fieldsToSave arrayList.
	 * Should be called when the manager is constructed.
	 * @return
	 */
	private static void setFieldsToSave(boolean includeMDP){
		if (includeMDP)
			for (Field f: MarkovDecisionProcess.class.getDeclaredFields())
				if (Modifier.isPublic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers()) )
					MDPFieldsToSave.add(f);
		for (Field f: Environment.class.getDeclaredFields())
			if (Modifier.isPublic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && !Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers()) )
				environmentFieldsToSave.add(f);
	}
	
	//////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	MANAGING SHADOWS 	//////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	private ExecutorService 				shadowES;
	private ArrayList<OutputShadow> 		shadows;
	public  ArrayList<OutputShadow>			getShadows(){return shadows;}
	private int 							shadowsToReadIn;
	private int 							shadowsReadIn;
	

	
	/**
	 * Read all the .out files in the specified folder and subfolders (if includeSubDirectories is true)
	 * Sub folders are searches in a depth-first manner. Since this process might be slow, 
	 * it is heavily advised to not run this function on the JavaFX thread, but to use a worker thread.
	 * @param folder
	 * @return
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private ArrayList<File> listOutFiles(File directory, boolean includeSubDirectories) 
	{
		if (!directory.isDirectory())
			throw new IllegalArgumentException("Trying to read in files from a file that is not a directory.");

		ArrayList<File> outFiles = new ArrayList<>();
		for (File f: directory.listFiles())
			if (f.getAbsolutePath().endsWith(".out"))
				outFiles.add(f);
			else if (f.isDirectory() && includeSubDirectories)
				outFiles.addAll(listOutFiles(f, includeSubDirectories)); 

		return outFiles;

	}

	/** Loads all .out files in the directory (and sub-directories, depth-first), and extracts a 
	 * shadow from each output file.
	 * 
	 * A call to this function will block until all shadows are found. Strongly recommended to run
	 * a call in a separate thread.
	 * 
	 * Passes a process update to the Stage, indicating how many shadow have been read in thus far.
	 * @param directory
	 * @throws IOException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	public ArrayList<OutputShadow> createShadows(File directory, boolean includeSubDirectories) throws IOException, InterruptedException, ExecutionException
	{

		// Empty the shadows list
		shadows.removeAll(shadows);
		shadowsReadIn = 0;

		// First, create an overview of which .out files are listed in the directory and its sub directories
		ArrayList<File> outFiles = listOutFiles(directory, includeSubDirectories);
		shadowsToReadIn = outFiles.size();

		// Next, create the shadows. To speed up this process, we can use the private shadowReader class.
		shadowES = Executors.newWorkStealingPool();
		ArrayList<Future<OutputShadow>> futures = new ArrayList<>();

		for (File f: outFiles)
			futures.add(shadowES.submit(new ShadowReader(f, this)));
		shadowES.shutdown();

		// Extract the shadows from the futures. 
		// This will block when the future has not been read in yet, and thus may take some time.
		for (Future<OutputShadow> future: futures)
			shadows.add(future.get());

		// return the shadows arraylist
		return shadows; 


	}

	/**
	 * Stops the manager from reading in any more shadows.
	 * @throws InterruptedException
	 */
	public void stopCreatingShadows() throws InterruptedException
	{

		if (shadowES != null){
			this.shadowES.shutdownNow(); 
			shadowES.awaitTermination(5, TimeUnit.MINUTES);
		}
	}
	
	/** 
	 * A function called by the ShadowReaders to notify that they are done (which in turns notifies the us
	 */
	public void notifyShadowReaderDone(){
		int percentageDone = (int) ((double)++shadowsReadIn / (double) shadowsToReadIn * 100);
		ObserverManager.makeToast("Read in file " + shadowsReadIn + " of " + shadowsToReadIn + " (" + percentageDone + "%)");
	}

	/**
	 * Takes in an output shadow, reads the corresponding .out file, and returns the Output object stored
	 * in that .out file.
	 * @param shadow
	 * @return
	 * @throws FileNotFoundException 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Output OutputShadowToOutput(OutputShadow shadow) throws FileNotFoundException, IOException, ClassNotFoundException{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(shadow.filename)));
		Object readObject = ois.readObject();
		ois.close();
		Output output= (Output) readObject;
		return output;
	}

	/**
	 * A private class that reads a .out file and creates a shadow of that file.
	 * I use a private class so that the reading can be done with multi-threading.
	 */
	private class ShadowReader implements Callable<OutputShadow>
	{
		public final File file;
		public final OutputFileManager supervisor;
		public ShadowReader(File file, OutputFileManager supervisor) throws IOException
		{
			this.file = file;
			this.supervisor = supervisor;
			// Make sure the file ends with .tab
			if (!file.getAbsolutePath().endsWith(".out"))
				throw new IOException("File did not end in .out");
		}
		
		@Override
		public OutputShadow call() throws FileNotFoundException, IOException, ClassNotFoundException  {
			// Read the file
			System.out.println(Helper.timestamp() + "\t- READING file: " + file.getAbsolutePath());
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
			Object readObject = ois.readObject();
			ois.close();

			// Transform to a Shadow object
			Output output = (Output) readObject;
			OutputShadow shadow = new OutputShadow(file.getCanonicalPath(), output.agentType, output.environment);

			// Inform the supervisor that this reader is done
			if (!Thread.interrupted())
				supervisor.notifyShadowReaderDone();
			return shadow;
		}
	}



	//////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	MANAGING METATABLE 	//////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////////////////
	/* This part of the code deals with turning the already loaded OutputShadows into a .csv file
	 * that contains the value function values and the optimal cues to sample for each agent into a
	 * single meta table. The shadows are loaded above, using the function createShadows().
	 * 
	 * The resulting .csv file has the following 'long' structure:
	 * 
	 *  This table has a 'long' data structure:
	 * =====================================================================================================================================================
	 * resourceQualityMean     resourceQualitySD     extrinsicEventMean extrinsicEventSD interruptionRate     Budget     Fitness     optimalNumberOfCuesSamples 
	 * -----------------------------------------------------------------------------------------------------------------------------------------------------
	 * r1                      r1                     r1               r1               r1                 r1b1       r1           r1                                  (body)
	 * r1                      r1                     r1               r1               r1                 ...        r1           r1                          
	 * r1                      r1                     r1               r1               r1                 r1bn       r1           r1                          
	 * ..                      ..                     ..               ..               ..                 ....       ..           ..                         
	 * rn                      rn                     rn               rn               rn                 rnbm       rn           rn                                    
	 * =====================================================================================================================================================
	 * Note that a single entry corresponds to the optimal strategy for the an environment-budget pair. Here rn refers to the n'th environment,
	 * and rnbm refers to the m'th budget in the n'th environment.
	 * 
	 * To make this table, p producers read in (in parallel) an output shadow to an output object, and place that output object in an outputQueue. 
	 * Simultaneously, c consumers take the Output objects from this queue, store them internally until a certain number of objects are collected,
	 * and then write all these values to the .csv file. 
	 * 
	 * At the start, a value x is initialized as the number of producers.
	 * When there are no more shadows for a producer to read, the producer places a FLAG (i.e, a null) object into the queue and then stops.
	 * When a consumer reads in a FLAG, it decrements the number x. If x reaches 0 (which can only happen when the queue is empty), all consumers
	 * stop.
	 * 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private static int outputQueueCapacity = 10000;
	private static int writeToDiskAfterNShadowsRead = 100;
	public static String delimiter = ";";

	private int numberOfConsumers, numberOfProducers;
	private AtomicInteger flagsRemaining;
	
	public ArrayBlockingQueue<Output> outputQueue;
	@SuppressWarnings("serial")
	public final static Output FLAG_TO_STOP = new Output(null, null, null, null, null,null,null,null, null,null,null,null, null, null, null, null, null, null, null, -1){};

	private int shadowsSentToProducer;
	public boolean stopProducing;

	public BufferedWriter csvFileWriter;

	/** Returns the next shadow to be produced. Might return null if no more shadows need to be produced.
	 */
	public synchronized OutputShadow getNextOutputShadow() { 
		if (stopProducing) return null;
		
		OutputShadow outputShadow=  shadows.get(shadowsSentToProducer++);
		if (shadowsSentToProducer == shadows.size()) stopProducing = true;
		
		int percentageDone = (int) ((double)shadowsSentToProducer / (double) shadows.size() * 100);
		ObserverManager.makeToast("Processing to .CSV: processing file " + shadowsSentToProducer+ " of " + shadows.size() + " (" + percentageDone + "%)");
		
		return outputShadow;}

	/**
	 * Reads in a .out file to an Output objects, and adds that object to the outputQueue.
	 * After all .out files have been read it, the producer add n FLAG_TO_STOP objects
	 * to the outputQueue, where n is the number of consumers. When a consumer encounters
	 * a FLAG_TO_STOP, it knows to stop consuming.
	 */
	private class OutputProducer implements Runnable{
		private final OutputFileManager supervisor;

		/**Constructor for the producer */
		public OutputProducer(OutputFileManager supervisor) {	this.supervisor = supervisor;}

		@Override
		public void run() 
		{
			while (!supervisor.stopProducing){
				OutputShadow s = supervisor.getNextOutputShadow();
				if (s != null)
					try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(s.filename))))
					{
						Output output = (Output) ois.readObject();
						outputQueue.add(output);
					} catch (Exception e) {ObserverManager.notifyObserversOfError(e);}
			}

			for (int i = 0; i < supervisor.numberOfConsumers; i ++ ) outputQueue.add(FLAG_TO_STOP);
		}

	}

	/**
	 * Takes an Output object from the queue and adds it the the .CSV file
	 */
	private class OutputConsumer implements Runnable{
		private final OutputFileManager supervisor;

		/**Constructor for the consumer */
		public OutputConsumer(OutputFileManager supervisor) {this.supervisor=supervisor; }

		@Override
		public void run() {
			try {
			StringBuilder localBuffer = new StringBuilder();
			int objectsInBuffer = 0;
		
			while (true)
			{
				
				// Get the next output, or wait if there currently is nothing in the queue
				Output nextOutput = null;
				try {
					nextOutput = supervisor.outputQueue.take();
				} catch (InterruptedException e) { ObserverManager.notifyObserversOfError(e);}
				
				// If the object is a FLAG_TO_STOP: decrement the flags that remain
				// If, after decreasing, there are no flags left:
				//		add a FLAG to the queue to make sure every other consumer also stops
				//		stop consuming
				if (nextOutput == FLAG_TO_STOP) {
					if (flagsRemaining.decrementAndGet() <= 0) {
						supervisor.outputQueue.add(FLAG_TO_STOP);
						break;
					}
				}
				
				if (nextOutput != FLAG_TO_STOP) {

					// If the next output is actually an output: write it to the buffer
					try {
						localBuffer.append(toCSVEntries(nextOutput));
					} catch (IllegalArgumentException | IllegalAccessException e) {ObserverManager.notifyObserversOfError(e);}
					objectsInBuffer++;

					// After reading in a batch, open the .csv file and store the result to the .csv file and clear the buffer
					if (objectsInBuffer >= writeToDiskAfterNShadowsRead)
						synchronized(supervisor.csvFileWriter){
							try {
								supervisor.csvFileWriter.append(localBuffer.toString());
								localBuffer = new StringBuilder();
								objectsInBuffer = 0;
							} catch (IOException e) {ObserverManager.notifyObserversOfError(e);
							}
						}
				}
			}

			// After the consumer concludes that there are no more Outputs to be read: flush the buffer for good measure
			if (objectsInBuffer >0)
				synchronized(supervisor.csvFileWriter){
					try {
						supervisor.csvFileWriter.append(localBuffer.toString());
						localBuffer = new StringBuilder();
						objectsInBuffer = 0;
						} catch (IOException e) {ObserverManager.notifyObserversOfError(e);
					}
				}
				

			} catch (Exception e) {ObserverManager.notifyObserversOfError("Exception when creating CSV","Unable to write an agent to the CSV file. See details. ",e);}
		}

	}

	private String createCSVHeader(ImmutableArray<String> cueLabels) {
		StringBuilder header = new StringBuilder();
		for (int i = 0; i < MDPFieldsToSave.size(); i++)
			header.append(MDPFieldsToSave.get(i).getName()+delimiter);
		for (int i = 0; i < environmentFieldsToSave.size(); i++)
			header.append(environmentFieldsToSave.get(i).getName()+delimiter);
		
		//TAG: adding new variables: adding the header in the csv file
		header.append(
				"budget" + delimiter +  
				"pDistCuesSampled"+delimiter+
				"proportionEating"+delimiter+
				"proportionDiscarding"+delimiter+
				
				"expectedFitnessOutcomesRootSampling" +delimiter+
				"expectedFitnessOutcomesRootEating" +delimiter+
				"expectedFitnessOutcomesRootDiscarding" +delimiter+
				"expectedFitnessOutcomesRoot" +delimiter+
				
				"expectedImmediateOutcomesRootSampling" +delimiter+
				"expectedImmediateOutcomesRootEating" +delimiter+
				"expectedImmediateOutcomesRootDiscarding" +delimiter+
				"expectedImmediateOutcomesRoot" +delimiter+
				
				"expectedCuesSampled" + delimiter+
				"cuesSampledConditionedOnAccepting" + delimiter+
				"cuesSampledConditionedOnDiscarding" + delimiter+
				
				"totalProportionAccepting" + delimiter+
				"totalProportionDiscarding" +  delimiter + 
				"FinalDelta" + delimiter + 
				"NumberIterations");
		
		// Add the header for each action and each cue dominance
		for (String label: cueLabels)
			header.append(delimiter+"cueDominanceEating_" + label.replaceAll(" ", "_"));
		for (String label: cueLabels)
			header.append(delimiter+"cueDominanceDiscarding_" + label.replaceAll(" ", "_"));
		return header.toString();
	}
	
	/**
	 * Returns the correct CSV entries for the specified Output in a single string.
	 * See comments under METATABLE for more info.
	 * An entry ends with a new line
	 * @param mdp
	 * @param e
	 * @return
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static String toCSVEntries(Output o) throws IllegalArgumentException, IllegalAccessException {
		StringBuilder constants = new StringBuilder();
		constants.append("\n");
		
		// Add the MDP fields
		for (Field f : MDPFieldsToSave) {
			if (f.get(o.mdp) instanceof DecimalNumber)
				constants.append(((DecimalNumber)f.get(o.mdp)).toPlainString() + delimiter);
			else if (f.get(o.mdp) instanceof String) {
				String s = ((String) f.get(o.mdp)).replace(";", " ");
				constants.append(s + delimiter);
			}
			else
				constants.append(f.get(o.mdp) + delimiter);
		}
			
		// Add the environment fields
		for (Field f : environmentFieldsToSave) {
			if (f.get(o.environment) instanceof DecimalNumber)
				constants.append(((DecimalNumber)f.get(o.environment)).toPlainString() + delimiter);
			else
				constants.append(f.get(o.environment) + delimiter);
		}

		// Add all budgets, expected cues and expected outcomes
		StringBuilder entries = new StringBuilder();

		// TAG: adding new agent type: make sure the new agent's output file is recognized and correctly saved to .csv
		// TAG: adding new variables: adding the values in the csv rows
		if (o instanceof ValueIteratorOutput) {
			o = (ValueIteratorOutput) o;
			DecimalNumberArray budgets = o.finalValueFunction.toDecimalNumberMatrix().getColumn("Budget");
			
			for (int b = 0; b < budgets.length(); b ++) {
				entries.append(constants.toString() + 
						budgets.get(b).toPlainString() + delimiter + 
						o.cuesSampled.getRow(b).concatenateRStyle() + delimiter + 
						o.proportionAccepting.getRow(b).concatenateRStyle() + delimiter + 
						o.proportionDiscarding.getRow(b).concatenateRStyle() + delimiter + 

						o.expectedFitnessOutcomesRoot.getRow(b).get(0).toPlainString() + delimiter + 
						o.expectedFitnessOutcomesRoot.getRow(b).get(1).toPlainString() + delimiter + 
						o.expectedFitnessOutcomesRoot.getRow(b).get(2).toPlainString() + delimiter + 
						o.expectedFitnessOutcomesRoot.getRow(b).max().toPlainString() + delimiter + 

						o.expectedImmediateOutcomesRoot.getRow(b).get(0).toPlainString() + delimiter + 
						o.expectedImmediateOutcomesRoot.getRow(b).get(1).toPlainString() + delimiter + 
						o.expectedImmediateOutcomesRoot.getRow(b).get(2).toPlainString() + delimiter + 
						o.expectedImmediateOutcomesRoot.getRow(b).max().toPlainString() + delimiter + 

						o.expectedCuesSampled.get(b).toPlainString() + delimiter + 
						o.cuesSampledConditionalOnAccepting.get(b).toPlainString() + delimiter + 
						o.cuesSampledConditionalOnDiscarding.get(b).toPlainString() + delimiter + 

						o.totalProportionAccepting.get(b).toPlainString() + delimiter + 
						o.totalProportionDiscarding.get(b).toPlainString() + delimiter + 
						o.finalDelta.toPlainString() + delimiter +
						o.finalIteration);

				// Add the header for each action and each cue dominance
				for (int i = 0; i < o.mdp.NUMBER_OF_CUE_LABELS; i++)
					entries.append(delimiter + o.cueDominanceEating.getRow(b).get(i));
				for  (int i = 0; i < o.mdp.NUMBER_OF_CUE_LABELS; i++)
					entries.append(delimiter + o.cueDominanceDiscarding.getRow(b).get(i));
			}
		}
		return entries.toString();
	}
	
	/**
	 * Write all loaded OutputShadows to a newly created .csv file. The filename will be appended with (x) if
	 * the filename already exists.
	 * 
	 * This function will block until all .out files are writen to the .csv file.
	 * @param pathToDirectory
	 * @param filename
	 * @param delimiter
	 * @param consumers
	 * @param producers
	 * @return
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public String writeCSV(String pathToDirectory, String filename,  int consumers, int producers, ImmutableArray<String> cueLabels) throws IOException, InterruptedException
	{
		System.out.println(Helper.timestamp() + " Starting to write with " + consumers + " consumers, fed by " + producers + " producers.");
		
		try{
			// Count how many flags there will be (i.e., the number of producers)
			flagsRemaining = new AtomicInteger(producers);
			
			// Create the output queue
			outputQueue = new ArrayBlockingQueue<Output>(outputQueueCapacity, false);

			// Set the filename (adding (x) if the file name is not unique)
			String cleanFilename = "nameless";
			if (filename.length()>0)
				cleanFilename = filename;
			if (cleanFilename.contains("."))
				cleanFilename = cleanFilename.split("\\.")[0];

			File file = new File(pathToDirectory + "\\" + cleanFilename + ".csv");

			// Ensure that the filename is unique. If it is not, change the name by appending (x) at the end
			int counter = 1;
			while (file.exists())
			{
				String alternateFilename = cleanFilename+ "(" +counter++ + ")";
				file = new File(pathToDirectory + "\\"+alternateFilename + ".csv");
			}

			// Create the newly minted file
			file.createNewFile();

			// Create the csv writer
			csvFileWriter = new BufferedWriter(new FileWriter(file));

			// Write the header to the file
			String header = createCSVHeader(cueLabels);
			csvFileWriter.write(header + "\n");

			// Create the producers and consumers
			shadowsSentToProducer = 0;
			this.stopProducing = false;
			this.numberOfProducers = producers;
			this.numberOfConsumers = consumers;

			ExecutorService csvES = Executors.newFixedThreadPool(numberOfConsumers + numberOfProducers); 
			for (int p = 0; p < producers; p ++)
				csvES.submit(new OutputProducer(this));
			for (int c = 0; c < consumers; c++)
				csvES.submit(new OutputConsumer(this));

			csvES.shutdown();
			csvES.awaitTermination(120, TimeUnit.MINUTES);

			System.out.println(Helper.timestamp() + " FINISHED writing all .out files to " + file.getCanonicalPath());
			return file.getName();
		}
		catch (IOException | InterruptedException e) {
			throw e;
		} finally {
			if (csvFileWriter != null)
				try {
					csvFileWriter.close();
				} catch (IOException e) {	e.printStackTrace();		}
		}
	}



}
