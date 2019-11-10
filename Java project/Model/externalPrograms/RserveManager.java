package externalPrograms;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import helper.Helper;
import helper.Helper.NoRInstalledException;
import start.Model.InvalidFitnessInputException;
import staticManagers.ObserverManager;

public final class RserveManager {
	
	private static String workingDirectoryPath;
	private static String sourceFolderDirectoryPath;
	private static String rScriptsDirectoryPath;
	@SuppressWarnings("unused")
	private static File   workingDirectory;
	@SuppressWarnings("unused")
	private static File   sourceFolderDirectory;
	private static File   rScriptsDirectory;
	private static RConnection connection;
	private static final boolean debug = false;
	
	private static ArrayList<File> dotRFiles;
	
	static {
		ObserverManager.makeToast("Attempting to start R via command line...");
		startRserve();
		ObserverManager.makeToast("Connection to RServe package in R established.");
	}

	private static void startRserve() 
	{
		// Get the required directories
		workingDirectoryPath 		= System.getProperty("user.dir");
		sourceFolderDirectoryPath 	= workingDirectoryPath + "\\sourceFolder\\";
		rScriptsDirectoryPath 		= sourceFolderDirectoryPath + "\\Rscripts\\";

		workingDirectory 			= new File(workingDirectoryPath);
		sourceFolderDirectory 		= new File(sourceFolderDirectoryPath);
		rScriptsDirectory 			= new File(rScriptsDirectoryPath);

		// Store all .R files in the sourceFolder\Rscripts directory in the ArrayList rFilesInSourceFolder
		dotRFiles = new ArrayList<>();
		for (File f: rScriptsDirectory.listFiles())
			if(f.toString().endsWith(".R"))
				dotRFiles.add(f);


		// Starting the connection to Rserve. 
		System.out.println(Helper.timestamp() + " Establishing connection to Rserve...");

		// First, find the location of R.exe
		String loc = null;
		try {
			loc = findRLocation();
		} catch (NoRInstalledException e) {
			ObserverManager.notifyObserversOfError("Error encountered","R.exe could not be found on this computer. Are you sure R was installed prior to launching this program?",e);
			e.printStackTrace();
		} catch (IOException e) {
			ObserverManager.notifyObserversOfError(e);
			e.printStackTrace();
		}
		System.out.println(Helper.timestamp() + " Found R location at: " + loc);

		// Next, tell R via the command line to load the Rserve library and start a server
		// There is a time delay between Java sending the command and R serve responding. Hence, we try to establish a connection 5 times before timing out
		String Rcmd = "library(Rserve); Rserve();";
		String cmd = "\"" + loc + "\" -e \"" + Rcmd + " \"";
		try {
			startRFromCommandLine(cmd, 1, 5);
			
		} catch (RserveException e) {
			ObserverManager.notifyObserversOfError("Error with Rserve", "An error was encountered when starting Rserve. See details for more information of the error." 
					+ "\n\nPlease make sure that R is properly installed on this machine (and the Rserve package is properly installed) and try to start the program again. "
					+ "\n\nIf this issue persists, please start Rserver() manually by running the following code in the R terminal directly:"
					+ "\n\nlibrary(Rserve); Rserve();\n\n"
					+ "to start Rserve manually (and restart the program afterwards)"
					,e);
			
			e.printStackTrace();
			
		} catch (IOException e) {
			ObserverManager.notifyObserversOfError(e);
			e.printStackTrace();
			
		} catch (REXPMismatchException e) {
			ObserverManager.notifyObserversOfError("Error encountered", "Received an unexpected object from the Rserve server.", e);
			e.printStackTrace();
		}
	}
	
	private static void startRFromCommandLine(String command, int attempt, int maximumAttempts) throws IOException, REXPMismatchException, RserveException {
		Process p = null;
		try {
			System.out.println(Helper.timestamp() + " Starting Rserve with command: " + command + ". Attempt " + attempt + " of " + maximumAttempts);
			p = Runtime.getRuntime().exec(command);
			
			System.out.println(Helper.timestamp() + "\t Initializing R...");
			initializeR();
			System.out.println(Helper.timestamp() + " \t Succesfully established connection to Rserve.");
		} catch (RserveException e) {
			p.destroy();
			if (attempt <= maximumAttempts)
			{
				attempt++;
				System.out.println(Helper.timestamp() + " \t FAILURE: Could not connect to Rserve. Trying again... | message: " + e.getLocalizedMessage());
				try {	Thread.sleep(10);	} catch (InterruptedException e1) { e1.printStackTrace();		}
				startRFromCommandLine(command, attempt, maximumAttempts);
			} else	{
				throw new RserveException(connection, Helper.timestamp() + " \tTime out while attempting to start Rserve. " + e.getMessage() );
			}
		}
		
	}
	
	/**
	 * Find and return the absolute path pointing to Rscript.exe. If multiple Rscript.exe's exist, the first one found is returned.
	 * 
	 * Due to multithreading, if more than one Rscript.exe exists on the computer, this function might return with different paths, 
	 * even if the underlying file system does not change. No guarantee can be made which Rscript.exe file is found first.
	 * @return
	 * @throws IOException
	 * @throws NoRInstalledException
	 */
	private static String findRLocation() throws NoRInstalledException, IOException 
	{

		// Check if there is a file called "RLocation.txt" in the source folder - if so, read the location from that file
		File RLocationtxt = new File(sourceFolderDirectoryPath + "\\RLocation.txt");
		if (RLocationtxt.exists())
		{
			// Read the file
			BufferedReader br = new BufferedReader(new FileReader(RLocationtxt));

			// Check if the text, which should be of the form /location/to/path/bin/Rscript.exe, actually exists
			String nextLine = br.readLine();
			while (nextLine != null) {
				System.out.println(Helper.timestamp() + " R was previously found at: " + nextLine );
				if (new File(nextLine).exists()) {
					System.out.println(Helper.timestamp() + " Found Rscript.exe at " + nextLine );
					br.close();
					return nextLine;
				}
				else {System.out.println(Helper.timestamp() + " Previously known location " + nextLine + " does not exist or is not accessible.");}
				nextLine = br.readLine();
			}
			br.close();
		}

		// If the file in RLocation.txt does not exist, or there is no RLocation file yet, find R.exe by searching through all
		// possible files and directories on the computer
		System.out.println(Helper.timestamp() + " No Rscript.exe location known yet. Searching all drives for R.exe");

		File rexe = null;
		for (File root: File.listRoots()){
			System.out.println(Helper.timestamp() + "\t Searching drive: " + root.getAbsolutePath()+ "...");
			try { rexe = Helper.findFile("Rscript.exe", root);} catch (InterruptedException e) {e.printStackTrace();	}
			if (rexe != null) break;
		}
		if (rexe == null)
			throw new NoRInstalledException("Could not find an instance of Rscript.exe on the computer. Are you sure that R is installed correctly?");

		// From this point onward we know that we either found R.exe (if it does not exist, this code is never reached).
		// Before returning the rexe path, store this path to a file that can be read in on the next occasion. 
		RLocationtxt.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(RLocationtxt));
		bw.append("\n" +rexe.getAbsolutePath());
		bw.close();

		// Finally, return the file path.
		return rexe.getAbsolutePath();
	}
	
	/**
	 * Load all files in the Rscripts folder into R.
	 * @throws RserveException
	 * @throws REXPMismatchException
	 */
	private static void initializeR() throws RserveException, REXPMismatchException
	{
		// setup the connection to RServe. Note, this requires R to run the Rserve() function from the Rserve library.
		connection = new RConnection();
		
		// set the working directory at the R server to the Rscripts path, using R's setwd()
		try {
			connection.eval("setwd(\"" + rScriptsDirectoryPath.replace("\\", "/") + "\")");
		} catch (RserveException e) {
			System.out.println(Helper.timestamp() + "\t Unable to set working directory");
			throw e;
		}
	
		// Source all .R files in dotRFiles. 
		System.out.println(Helper.timestamp() + "\t Searching for .R files in " + rScriptsDirectory.getAbsolutePath());
		for (File r: dotRFiles)
		{
			String filename = r.getAbsolutePath().replaceAll(".*\\\\", "") ;
			System.out.println(Helper.timestamp() + " \t Sourcing file \"" + filename + "\" to Rserve");
			try {
				connection.eval(("source(\"" + r + "\")").replace("\\", "/"));
			} catch (RserveException e) {
				System.out.println(Helper.timestamp() + "\t Unable to source file " + r.getAbsolutePath() + ": " + e.getLocalizedMessage());
				throw e;
			}
			System.out.println(Helper.timestamp() + " \t Succesfully sourced file \"" + filename + "\" to Rserve");
		}
	}


	
	
	
	/**
	 * Replaces the 'x''s in the specified function (String) with the value and evaluates the resulting expression.
	 * @param budget: change in budget
	 * @return the fitness increase associated with the budget.
	 * @throws REXPMismatchException
	 * @throws REngineException
	 * @throws Exception
	 */
	public static DecimalNumber evaluateFunction(String function, BigDecimal value) throws InvalidFitnessInputException, REXPMismatchException, NumberFormatException, REngineException
	{
		// Set the x variable in R
		evaluate("x<-" +value.toPlainString());
		
		// Tell R to evaluate the function
		double result = evaluate(function).asDouble();
		
		// parse the result to a BigDecimal and return
		return new DecimalNumber(result, true);
	}
	
	/**
	 * Replaces the 'x''s in the specified function (String) with the values and evaluates the resulting expression.
	 * return a BigDecimal for all input values
	 * @param budget: change in budget
	 * @return the fitness increase associated with the budget.
	 * @throws REXPMismatchException
	 * @throws REngineException
	 * @throws Exception
	 */
	public static DecimalNumberArray evaluateFunction(String function, DecimalNumberArray array) throws REngineException, REXPMismatchException{
		if (array.length()==0)
			return null;
		
		// create the string to tell R to set the x variable
		StringBuilder sb = new StringBuilder();
		sb.append("x<-c(");
		sb.append(array.get(0));
		for (int i = 1; i < array.length(); i ++)
			sb.append(","+array.get(i));
		sb.append(")");
		evaluate(sb.toString());
		
		// Tell R to evaluate the function
		double[] results = evaluate(function).asDoubles();
		
		// Parse to DecimalNumber array and return
		return new DecimalNumberArray(results);
	}
	
	/**
	 * Replaces the 'x''s in the specified function (String) with the values and evaluates the resulting expression.
	 * return a BigDecimal for all input values
	 * @param budget: change in budget
	 * @return the fitness increase associated with the budget.
	 * @throws REXPMismatchException
	 * @throws REngineException
	 * @throws Exception
	 */
	public static double[] evaluateFunction(String function, double[] array) throws REngineException, REXPMismatchException{
		if (array.length==0)
			return null;
		
		// create the string to tell R to set the x variable
		StringBuilder sb = new StringBuilder();
		sb.append("x<-c(");
		sb.append(array[0]);
		for (int i = 1; i < array.length; i ++)
			sb.append(","+array[i]);
		sb.append(")");
		evaluate(sb.toString());
		
		// Tell R to evaluate the function
		double[] results = evaluate(function).asDoubles();
		return results;
	}

	/**
	 * Evaluate a string in R, returns the resulting REXP object. This function prints the error messages generated in 
	 * R. Additionally:
	 * - multiple statements can be parsed at the same time - use ';' as a divider of statements. However, only the output of the
	 * 		last statement is returned.
	 * - This function uses R's try() functionality, which requires '<-' rather than '=' as the assignment operator.
	 *  
	 *  If replace equals is true all single occurrences of '=' in all command statements are replaced by '<-' (excluding '==' operators).
	 *  
	 *  
	 * @param command
	 * @return
	 * @throws REngineException 
	 * @throws REXPMismatchException 
	 */
	public static synchronized REXP evaluate(String command, boolean replaceEquals) throws REngineException, REXPMismatchException
	{
		// If there is a R error, we want to know this error. For this, we can use R's try(..., silent=TRUE).
		// This function returns a string value in case of an error, and a correct value otherwise
		// However, try cannot deal with ";" characters - we have to put each statement into its own try function first.
		String[] subCommands = command.split(";");
		
		for (int i = 0; i < subCommands.length; i ++)
		{
			subCommands[i] = subCommands[i].replaceAll("\t"," ");
			subCommands[i] = subCommands[i].replaceAll("\n"," ");
		}

		// Make sure that there are no empty commands
		for (int i = 0; i < subCommands.length; i ++)
		{
			int emptySpaces = 0;
			for(char c : subCommands[i].toCharArray()){
			   if(c == ' ' ){
				   emptySpaces++;
			   }}
			if (subCommands[i].length() == emptySpaces)
				subCommands[i] = "0";
		}
			
		
		if (replaceEquals)
		{
			// Likewise, R's try does not like '=' characters - replace them with '<-'
			for (int i = 0; i < subCommands.length; i++)
				if (subCommands[i].contains("="))
				{
					StringBuilder sb = new StringBuilder();
					for (int c = 0; c < subCommands[i].length(); c++)
						if (subCommands[i].charAt(c) == '=' && c != subCommands[i].length())
						{
							if (subCommands[i].charAt(c+1) == '=')
								{ sb.append("=="); c++; }		
							else 
								{ sb.append("<-"); }
						}
						else
							sb.append(subCommands[i].charAt(c));
					subCommands[i] = sb.toString();
				}
		}
		
		for (int i = 0; i < subCommands.length; i ++)
			subCommands[i] = "try(" + subCommands[i] + ", silent=TRUE)";
		
		REXP result=null;

		for (String s: subCommands)
		{
			try {
				if (debug) System.out.println(Helper.timestamp() + " Sending command to R: " + s);
				result = connection.parseAndEval(s);
				if (result.inherits("try-error")) ObserverManager.notifyObserversOfError("Error", result.asString());
			} catch (REngineException e) { 
				System.err.println("IS CONNECTED: " + connection.isConnected());
				throw new REngineException(connection, "Could not parse statement: " + s + "\nThis error most likely originated from JAVA sending invalid requests to R.\n Message: " + connection.getLastError());
			} catch (REXPMismatchException e) {
				throw new REXPMismatchException (result, "Mismatch between R's object and Java's interpretation of this object. Somewhere a cast has gone wrong.\n"); }
			
		}
	
		
		if (debug) System.out.println(Helper.timestamp() + " Succesfully received response from R: " + result.toDebugString());
		return result;
	}
	
	public static REXP evaluate(String command) throws REngineException, REXPMismatchException
	{
		return evaluate(command, false);
	}
	
	public void loadBigDecimalTable2D (DecimalNumberMatrix table, String dataFrameName)
	{
		try {
			evaluate(table.toDataFrame(dataFrameName));
		} catch (REngineException | REXPMismatchException e) {
			e.printStackTrace();
		}
	}

}
