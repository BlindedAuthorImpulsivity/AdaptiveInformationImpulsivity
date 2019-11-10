package agent;

import java.io.Serializable;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import environment.Environment;
import estimationParameters.EstimationParameters;
import helper.Helper;
import markovDecisionProcess.MarkovDecisionProcess;

public abstract class Output implements Serializable
{
	private static final long serialVersionUID = Helper.programmeVersion;
	
	public final AgentType					agentType;
	public final String						filename;
	public final MarkovDecisionProcess		mdp;
	public final Environment				environment;
	public final EstimationParameters 		estimationParameters;
	public final ValueFunction				finalValueFunction;
	
	// Note that we save an environment in its entirety - including many different starting budgets. 
	// For each possible starting budget we build a different tree, and from this tree we save: 
	// 1) an (optimal) distribution of cues sampled (i.e., {p(sampled 1 cue), p(sampled 2 cues), ..., p(sampled all possible cues)}, 
	// 2) an expected fitness outcome for all possible actions (except dead-on-start) at the root node,
	// 3) for each non-sampling, non-dead action: the proportion of agents ending with this action
	
	
	public final DecimalNumberMatrix 		cuesSampled;
	public final DecimalNumberMatrix		proportionAccepting;
	public final DecimalNumberMatrix		proportionDiscarding;
	public final DecimalNumberMatrix		expectedFitnessOutcomesRoot;
	public final DecimalNumberMatrix		expectedImmediateOutcomesRoot;
	public final DecimalNumberArray			expectedCuesSampled;
	public final DecimalNumberArray			cuesSampledConditionalOnAccepting;
	public final DecimalNumberArray			cuesSampledConditionalOnDiscarding;
	public final DecimalNumberArray			totalProportionAccepting;
	public final DecimalNumberArray			totalProportionDiscarding;
	public final DecimalNumberMatrix	    cueDominanceEating;
	public final DecimalNumberMatrix        cueDominanceDiscarding;
	public final DecimalNumber				finalDelta;
	public final int 						finalIteration;

	public Output(
			AgentType agentType, 
			String filename, 
			MarkovDecisionProcess mdp, 
			Environment environment, 
			EstimationParameters estimationParameters,
			ValueFunction finalValueFunction, 
			DecimalNumberMatrix 	cuesSampled, 
			DecimalNumberMatrix		proportionAccepting,
			DecimalNumberMatrix		proportionDiscarding,
			DecimalNumberMatrix		expectedFitnessOutcomesRoot,
			DecimalNumberMatrix		expectedImmediateOutcomesRoot,
			DecimalNumberArray		expectedCuesSampled,
			DecimalNumberArray		cuesSampledConditionalOnAccepting,
			DecimalNumberArray		cuesSampledConditionalOnDiscarding,
			DecimalNumberArray		totalProportionAccepting,
			DecimalNumberArray		totalProportionDiscarding,
			DecimalNumberMatrix		cueDominanceEating,
			DecimalNumberMatrix 	cueDominanceDiscarding,
			DecimalNumber finalDelta, 
			int finalIteration)
	{
		this.agentType = agentType;
		this.filename = filename;
		this.mdp = mdp;
		this.environment = environment;
		this.estimationParameters = estimationParameters;
		this.finalValueFunction = finalValueFunction;
		
		this.cuesSampled= cuesSampled;
		this.proportionAccepting=proportionAccepting;
		this.proportionDiscarding=proportionDiscarding;
		this.expectedCuesSampled=expectedCuesSampled;
		this.cuesSampledConditionalOnAccepting=cuesSampledConditionalOnAccepting;
		this.cuesSampledConditionalOnDiscarding=cuesSampledConditionalOnDiscarding;
		this.totalProportionDiscarding= totalProportionDiscarding;
		this.totalProportionAccepting = totalProportionAccepting;
		this.expectedFitnessOutcomesRoot = expectedFitnessOutcomesRoot;
		this.expectedImmediateOutcomesRoot=expectedImmediateOutcomesRoot;
		
		this.cueDominanceEating=cueDominanceEating;
		this.cueDominanceDiscarding=cueDominanceDiscarding;
		
		this.finalDelta = finalDelta;
		this.finalIteration = finalIteration;

	}
	
	
}
