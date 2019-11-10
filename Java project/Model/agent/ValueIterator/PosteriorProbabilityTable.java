package agent.ValueIterator;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import environment.Environment;
import helper.Helper;
import helper.Helper.InvalidProbabilityException;
import markovDecisionProcess.MarkovDecisionProcess;

/**
 * PosteriorProbabilityTable is a table that contains the posterior probability for all combination of
 * cues possible. This table has to be calculated only once by each agent, which severely speeds up the learning 
 * procedure.
 * 
 * Individual records (i.e., of class PosteriorProbability) can be accessed
 * by getPosteriorProbability (Integer[] cueSet).
 * 
 * Each posterior probability entry also contains a table listing the probability distribution of the
 * next cue, given the posterior probability over resource values. 
 *
 */
public class PosteriorProbabilityTable implements Serializable
{
	private static final long serialVersionUID = Helper.programmeVersion;
	private Map<Integer[], PosteriorProbability> posteriorProbabilityMap;
	private Integer[][] keys;
	
	// Some fields that we have to save after construction for printing purposes
	private final int NUMBER_OF_CUE_LABELS;
	private final DecimalNumberArray resourceValues;
	
	public PosteriorProbabilityTable (MarkovDecisionProcess mdp, Environment environment) throws InvalidProbabilityException 
	{
		this.posteriorProbabilityMap = new ConcurrentHashMap<>();
		NUMBER_OF_CUE_LABELS = mdp.NUMBER_OF_CUE_LABELS;
		resourceValues = mdp.CUE_EMISSION_MATRIX(true).getColumn("Resource value");
		
		// Generate all possible keys (a key is a combination of cues. Hence, create all possible combinations of cues that can occur)
		Integer[] 	array = Helper.sequence(0, mdp.MAXIMUM_CUES, 1);
		keys  = Helper.gridExpand(Integer.class, array, array, mdp.MAXIMUM_CUES);
	
		for (int c=3;c<=NUMBER_OF_CUE_LABELS;c++)
				keys = Helper.gridExpand(Integer.class, keys, array,mdp.MAXIMUM_CUES);
		
		// For each key, create the corresponding posterior distribution 
		//  and add both the key and the resulting distribution to the map
		System.out.println(Helper.timestamp() + " CALCULATING\t\t "+ keys.length+ " posterior distributions...");
		ExecutorService es = Executors.newFixedThreadPool(1);
		for (Integer[] key: keys)
		{
			PosteriorProbability pp=new PosteriorProbability(mdp, environment, key);
			this.posteriorProbabilityMap.put(key, pp);
			es.submit(pp);
		}
		es.shutdown();
		try {	es.awaitTermination(5, TimeUnit.MINUTES);} catch (InterruptedException e) {			}
		System.out.println(Helper.timestamp() + " DONE CALCULATING\t posterior probalities.");
	
	}
	
	public PosteriorProbability[] getAllPosteriorProbabilities()
	{
		PosteriorProbability[] array = new PosteriorProbability[posteriorProbabilityMap.size()];
		for (int i = 0; i < array.length; i++)
			array[i] = posteriorProbabilityMap.get(keys[i]);
		return array;
			
	}
	

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String spacer = "    ";
		DecimalFormat df = new DecimalFormat("0.0000000000000");
		
		// Draw the 'cues received' part of the header
		StringBuilder subHeaderCues = new StringBuilder();
		StringBuilder subHeaderLinesCues = new StringBuilder();

		for (int i = 0; i < NUMBER_OF_CUE_LABELS; i++)
		{
			String cueType = "cue" + i;
			subHeaderCues.append( cueType + spacer);
			subHeaderLinesCues.append(Helper.repString("-", cueType.length()+spacer.length() ));
		}
		
		StringBuilder mainHeaderCues = new StringBuilder();
		String cuesReceived = "Cues received";
		mainHeaderCues.append("\n"+Helper.repString(" ", subHeaderCues.length()/2-cuesReceived.length()/2));
		mainHeaderCues.append(cuesReceived);
		mainHeaderCues.append(Helper.repString(" ", subHeaderCues.length() - mainHeaderCues.length())+" ||");
		subHeaderCues.append("||  ");
		subHeaderLinesCues.append("--");
		
		// Draw the 'p(Value = value)' part of the header
		StringBuilder subHeaderProb = new StringBuilder();
		StringBuilder subHeaderLinesProb = new StringBuilder();
		ArrayList<Integer> columnWidth = new ArrayList<>();
		for (DecimalNumber v:resourceValues)
		{
			String value = df.format(v);
			columnWidth.add(value.length());
			subHeaderProb.append( value + spacer);
			subHeaderLinesProb.append(Helper.repString("-", value.length()+spacer.length() ));
		}
		StringBuilder mainHeaderProb = new StringBuilder();
		String probabilities = "p(Value = value|cues received)";
		mainHeaderProb.append(Helper.repString(" ", subHeaderProb.length()/2-probabilities.length()/2));
		mainHeaderProb.append(probabilities);
		mainHeaderProb.append(Helper.repString(" ", subHeaderProb.length() - mainHeaderProb.length()));
		
		sb.append(mainHeaderCues.toString() + mainHeaderProb.toString());
		sb.append("\n"+subHeaderCues.toString() + subHeaderProb.toString());
		sb.append("\n" + subHeaderLinesCues.toString() + subHeaderLinesProb.toString());
		
		//Adding posterior probabilities
		for (Integer[] set:this.keys)
		{
			sb.append("\n");
			for (int i: set)
			{
				String entry = i + spacer;
				sb.append(entry + Helper.repString(" ", spacer.length()+4-entry.length()));
			}
			sb.append("||  ");
			PosteriorProbability pp = getPosterior(set);
			DecimalNumberArray entries = pp.posteriorProbabilityOfResourceValues();
			for (int i =0;i<entries.length();i++)
			{
				String column = df.format(entries.get(i).doubleValue())+spacer;
				sb.append(column + Helper.repString(" ", columnWidth.get(i)+spacer.length()-column.length()));
			}
			
		}
		
		return sb.toString();
	}
	
	/**
	 * Link an input Integer array to an Integer array used in the posteriorProbabilityMap
	 * @param proposedSet
	 */
	private Integer[] findCueSet (Integer[] proposedSet)
	{
		for (Integer[] set:keys)
		{
			boolean matching = true;
			for (int i = 0; i < set.length && matching;i++)
				if (!set[i].equals( proposedSet[i]))
					matching = false;
			if (matching) return set;
		}
		return null;
	}
	
	public PosteriorProbability getPosterior ( Integer[] cueSet )
	{
		return this.posteriorProbabilityMap.get(findCueSet(cueSet));
	}
	
	

}
