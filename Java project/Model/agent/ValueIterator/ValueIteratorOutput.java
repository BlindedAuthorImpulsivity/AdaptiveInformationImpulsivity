package agent.ValueIterator;

import agent.AgentType;
import agent.Output;
import agent.ValueFunction;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import environment.Environment;
import estimationParameters.EstimationParameters;
import helper.Helper;
import markovDecisionProcess.MarkovDecisionProcess;

public class ValueIteratorOutput extends Output
{
	private static final long serialVersionUID = Helper.programmeVersion;
	public final	PosteriorProbabilityTable	posteriorProbabilityTable;
	
	/**
	 * An object that stores all the fields that need to be saved
	 * @param filename						The name of the output (.out) file
	 * @param mdp							The Markov Decision Process that the agent used
	 * @param environment					The Environment the agent lived in
	 * @param finalValueFunction			The most recent (and therefore best estimate of) the value function. The value function is a [n, 3] DecimalNumberMatrix,
	 * 										where n is the number of possible budget levels, and the columns are "budget", "expected value outcome", and "expected fitness outcome"
	 * @param ppt							The Posterior probability table the agent used when contructing the trees. This table is saved so that it can be used later on,
	 * 										for instance when drawing a policy tree.
	 * @param cuesSampled					Denotes, for each possible budget, what proportion of agents samples x amount of cues. This is a DecimalNumberMatrix object
	 * 										of size [n, m], where n is the number of possible budget states and m is the maximum number of cues that can be sampled +1
	 * @param proportionAccepting 			Denotes, for each possible budget, what proportion of agent decides to accept the resources after sampling exactly x cues. 
	 * 										This is a DecimalNumberMatrix object of size [n, m], where n is the number of possible budget states and m is the 
	 * 										maximum number of cues that can be sampled +1.
	 * @param proportionDiscarding			Denotes, for each possible budget, what proportion of agent decides to discard the resources after sampling exactly x cues. 
	 * 										This is a DecimalNumberMatrix object of size [n, m], where n is the number of possible budget states and m is the 
	 * 										maximum number of cues that can be sampled +1
	 * @param expectedFitnessOutcomesRoot	A DecimalNumberMatrix of size [n,m] where n is the number of possible budget states, and m is 4. The columns denote
	 * 										"budget", "expected fitness outcome when sampling of root node", "expected fitness outcome when accepting of root node", and
	 * 										"expected fitness outcome when discarding of root node", respectively. 
	 * @param expectedImmediateOutcomesRoot  A DecimalNumberMatrix of size [n,m] where n is the number of possible budget states, and m is 4. The columns denote
	 * 										"budget", "expected immediate outcome when sampling of root node", "expected immediate outcome when accepting of root node", and
	 * 										"expected immediate outcome when discarding of root node", respectively.
	 * @param expectedCuesSampled			The expected number of cues sampled, per budget. Hence, a DecimalNumberMatrix of size [n,2] where n is the total number
	 * 										of possible budget states, and columns denote "budget" and "expectedCuesSampled", respectively.
	 * @param cuesSampledConditionalOnAccepting The number of cues the agent has sampled, given that it will accept the resource. That is, the sum of the proportion 
	 * 										in each accepting leaf node of the tree.
	 * @param cuesSampledConditionalOnDiscarding The number of cues the agent has sampled, given that it will discard the resource. That is, the sum of the proportion 
	 * 										in each accepting leaf node of the tree.
	 * @param totalProportionAccepting		The proportion of agents that will eat the resource; a DecimalNumberMatrix of size [n,2] where n is the total number
	 * 										of possible budget states, and columns denote "budget" and "proportionAccepting", respectively.
	 * @param totalProportionDiscarding		The proportion of agents that will discard the resource; a DecimalNumberMatrix of size [n,2] where n is the total number
	 * 										of possible budget states, and columns denote "budget" and "proportionAccepting", respectively.
	 * @param finalDelta					The final delta (change in estimates between iterations) that the agent observed
	 * @param finalIteration				The number of iterations the agent went through before stopping.
	 */
	public ValueIteratorOutput(
			String filename, 
			MarkovDecisionProcess mdp, 
			Environment environment, 
			EstimationParameters estimationParameters,
			ValueIteratorValueFunction finalValueFunction, 
			PosteriorProbabilityTable ppt, 
			
			DecimalNumberMatrix 	cuesSampled, 
			DecimalNumberMatrix		proportionAccepting,
			DecimalNumberMatrix		proportionDiscarding,
			DecimalNumberMatrix		expectedFitnessOutcomesRoot,
			DecimalNumberMatrix		expectedImmediateOutcomesRoot,
			DecimalNumberArray		expectedCuesSampled,
			DecimalNumberArray		cuesSampledConditionalOnAccepting,
			DecimalNumberArray		cuesSampledConditionalOnDiscarding,
			DecimalNumberMatrix 	cueDominanceEating,
			DecimalNumberMatrix		cueDominanceDiscarding,
			DecimalNumberArray 		totalProportionAccepting,
			DecimalNumberArray		totalProportionDiscarding,
			
			
			DecimalNumber finalDelta, 
			int finalIteration )
	{
		
		
		super(AgentType.VALUE_ITERATOR, 
			filename, 
			mdp, 
			environment, 
			estimationParameters,
			finalValueFunction, 
			cuesSampled, 
			proportionAccepting,
			proportionDiscarding,
			expectedFitnessOutcomesRoot,
			expectedImmediateOutcomesRoot,
			expectedCuesSampled,
			cuesSampledConditionalOnAccepting,
			cuesSampledConditionalOnDiscarding,
			totalProportionAccepting,
			totalProportionDiscarding,
			cueDominanceEating,
			cueDominanceDiscarding,
			finalDelta, 
			finalIteration);
		this.posteriorProbabilityTable = ppt;
		
	}

}
