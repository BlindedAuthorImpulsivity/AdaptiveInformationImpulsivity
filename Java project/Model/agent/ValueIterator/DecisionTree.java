package agent.ValueIterator;

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import agent.Action;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import decimalNumber.TransformationFunction.TransformationFunctionDecimalNumber;
import environment.Environment;
import estimationParameters.EstimationParameters;
import estimationParameters.Optimizer;
import helper.Helper;
import helper.Helper.ImpossibleStateException;
import helper.Helper.InvalidProbabilityException;
import helper.Helper.InvalidProportionException;
import helper.ImmutableArray;
import markovDecisionProcess.MarkovDecisionProcess;
import start.Model.InvalidFitnessInputException;

public class DecisionTree  
{
	// Estimating the backwards pass of the decision tree is an expensive operation - it can go up to 95%+
	// of the runtime of the ValueIterator. Almost all of this runtime is used to determine the expected
	// outcome of eating the resource. Hence, I created two different optimizations for this function:
	// a lossless optimization and a lossy optimization. 
	//
	// The lossless optimization uses double[][]'s  instead of DecimalNumberMatrices while computing 
	// the immediate outcome, and uses DecimalNumberMatrices when computing the probability of each outcome.
	// With the use of some scaling, this function will return the exact value of the non-optimized 
	// function, but is roughly 1.5 times faster (i.e., a run of the lossless optimization will take only
	// 50% of the non-optimized method). 
	//
	// The lossy optimization uses double[][]'s for both the computation of the immediate outcomes, as well
	// as for the probability of each outcomes. Because it uses double's instead of DecimalNumber's, it is
	// subject to floating point errors - especially when the tree becomes large and proportions become small.
	// A consequence of this is that the results cannot be trusted after the 9th (or so) decimal number. However,
	// since these floating point errors are undirected (i.e., equally likely to be > 0 as < 0), the floating
	// point impressions is relatively small. The lossy optimization is fast - about 9 times faster (i.e.,
	// a run of the lossy optimized function will take about 11% of the runtime of the non-optimized function).
	// 
	// The optimizer used can be set through changing the optimizer of this class. Due to its high speed and 
	// low (but present) error rate, I recommend the lossy optimizer.proportionAccepting
	public final 	MarkovDecisionProcess 		mdp;
	public final 	Environment 				environment;
	public final 	EstimationParameters		estimationParameters;
	
	public final 	DecisionNode 				root;
	public 			ArrayList<DecisionNode> 	nodes;
	public final 	ArrayList<DecisionEdge> 	edges;
	public final 	PosteriorProbabilityTable 	posteriorProbabilityTable;

	public final 	DecimalNumber 				startingBudget;
	
	// Variables that we will store after the forward pruning pass has been completed
	// They are listed as private so that the user has to request them via a get...() function,
	// which checks if the tree has already gone through its forward pruning pass.
	// For each possible tree we save: 
	// 1) an (optimal) distribution of cues sampled (i.e., {p(sampled 0 cues), p(sampled 1 cue), ..., p(sampled n cues)}, 
	// 2) a distribution of the proportion of agents eating, for each possible number of cues sampled:
	//		{p(eating|0 cues), p(eating|1 cue), p(eating|2 cues), ... }
	// 3) a distribution of the proportion of agents discarding, for each possible number of cues sampled:
	//		{p(discarding|0 cues), p(discarding|1 cue), p(discarding|2 cues), ... }
	// 4) the expected number of cues sampled
	// 5) the expected proportion of eating (i.e., the mean of 2)
	// 6) the expected proportion of discarding (i.e., the mean of 3)
	// 7) an expected fitness outcome for all possible actions (except dead-on-start) at the root node,
	// 8) The dominance of each cue type when eating. The dominance of a cue type in a node is defined as
	//     (number of cues of type label) - (total number of cues sampled). The dominance of a type when eating is
	//		for each eating leaf node: proportion(node)*dominance(cueLabel)
	// 9) The dominance of each cue type when Discarding.
	private			DecimalNumberArray			cuesSampled;
	private			DecimalNumberArray			proportionEating; 
	private			DecimalNumberArray			proportionDiscarding;	
	private 		DecimalNumber				expectedCuesSampled;
	private			DecimalNumber				totalProportionEating; 
	private			DecimalNumber				totalProportionDiscarding;
	private			DecimalNumberArray			expectedOutcomes;
	private			DecimalNumberArray			dominanceEating; 
	private			DecimalNumberArray			dominanceDiscarding;	

	public 			boolean 					printEdges 			= false; // if true prints the edges, if false ignores the edges when printing
	public 			boolean 					ranForward 			= false; // Did the tree do a forward pass?
	public			boolean 					ranBackward 		= false; // Did the tree do a backward pass?
	public 			boolean 					ranForwardPruning   = false; // Did the tree do a forward pruning pass?

	public DecisionTree (MarkovDecisionProcess mdp, Environment environment, EstimationParameters ep, PosteriorProbabilityTable posteriorProbabilityTable, DecimalNumber startingBudget) throws ImpossibleStateException, InvalidProportionException, IllegalRangeException
	{
		this.mdp = mdp;
		this.environment = environment;
		this.estimationParameters = ep;
		
		this.posteriorProbabilityTable = posteriorProbabilityTable;
		nodes = new ArrayList<>();
		edges = new ArrayList<>();
		
		this.startingBudget = startingBudget.clone().setImmutable(true);

		Integer[] initialCueSet = new Integer[mdp.NUMBER_OF_CUE_LABELS];
		for (int i = 0; i < initialCueSet.length; i++) initialCueSet[i] = 0;
		root  = new DecisionNode(mdp, posteriorProbabilityTable, environment, ep, initialCueSet, this.startingBudget, new DecimalNumber(1, 0, 1, true));

	}

	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Forward pass	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	/** <pre>
	 * Generate the decision tree. Starting from the root node, a node is expanded if the number of cues sampled does
	 * not exceeds the maximum available number of cues and its budget after sampling one cue is positive.
	 * The child nodes of a node are all nodes that result from sampling one additional cue. If a node is expanded,
	 * its child nodes are placed on a frontier. This child nodes are subsequently expanded again, until the full tree is grown.
	 * @throws InvalidProportionException
	 * @throws ImpossibleStateException
	 * @throws REngineException
	 * @throws REXPMismatchException
	 * @throws InvalidFitnessInputException
	 * @throws NumberFormatException
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	public void forwardPass() throws InvalidProportionException, ImpossibleStateException, NumberFormatException, InvalidFitnessInputException, REXPMismatchException, REngineException, IllegalRangeException, IllegalScaleException
	{
		// The forward pass can only start with a live node.
		if (root.budget.compareTo(DecimalNumber.ZERO)==0)
		{
			// If the root is dead on the start, set the outcome for each action to the outcome associated with a zero budget
			// This sad node is then the complete tree.
			//TODO: TURNED OFF DUE TO OVERFLOW ERROR
			root.setExpectedOutcomes(DecimalNumberArray.rep(DecimalNumber.ZERO, 3));
			root.bestAction.add(Action.DEAD_ON_START);

			nodes.add(root);
			this.ranForward=true;
			return;
		}

		// If, however, the node is not dead on start: build a tree
		ArrayList<DecisionNode> frontier = new ArrayList<>();
		frontier.add(root);

		while (frontier.size()>0)
		{
			//Expand all nodes: find children
			ArrayList<DecisionNode> children = new ArrayList<>();
			for (DecisionNode parent:frontier)
				children.addAll(parent.createChildNodes());

			// Add the nodes in the frontier to the nodes list, and all edges from that node to the edges list
			nodes.addAll(frontier);
			for (DecisionNode dn: frontier)
				edges.addAll(dn.childEdges);

			// Empty frontier
			frontier = new ArrayList<>();

			// Prune the children: sum together the proportions of the nodes that are the same in terms of cue set, budget, and proportion.
			for (DecisionNode potentialNode:children)
			{
				boolean unique=true;
				for (DecisionNode existingNode: frontier)
					if (potentialNode.equals(existingNode))
					{
						unique=false;
						existingNode.setProportion(existingNode.proportion.add(potentialNode.proportion));
						for (DecisionEdge de:potentialNode.parentEdges) 
							de.child = existingNode;
						existingNode.parentEdges.addAll(potentialNode.parentEdges);
					}
				if (unique) frontier.add(potentialNode);
			}
			// Assert that the total proportion of the frontier sum up to one (if there are live nodes) or sum up to zero (i.e., when there are no nodes)
			DecimalNumber sumOfProportions = new DecimalNumber(0, 0, 1, false); 
			for (DecisionNode newNode:frontier)
				sumOfProportions.add(newNode.proportion);

			if (!(sumOfProportions.equals(DecimalNumber.ZERO, true) || sumOfProportions.equals(DecimalNumber.ONE, true)) )
				throw new InvalidProportionException("Created a new generation of nodes (forward pass in a decision tree), but the total sum of the proportions does not equal zero or one. Total sum is " + sumOfProportions);

		}
		this.ranForward = true;
	}


	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Backward pass	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	/**
	 * Given a decision tree (which is grown via DecisionTree's forwardPass()), use backwards induction to construct
	 * a decision tree that maximizes the expected outcome.
	 *
	 * Note that there are two kinds of backwards passes. The first is the regular pass that is used by the VALUE ITERATION 
	 * algorithm (section 2 in the description of the ValueIterator class) to compute the expected VALUE outcomes
	 * from the second cycle to the final cycle (where a cycle is a combination of a resource encounter and an 
	 * extrinsic event). In the first pass, which is used during step 7 of the value iterator (see comments above the ValueIterator class), 
	 * the discarding and eating actions' expected
	 * outcome is:
	 * 	
	 * 		expected outcome = 	[immediate outcome of resource encounter + outcome of extrinsic event after encounter ] +
	 * 						 	[discounted future outcomes, given that the starting budget of the next encounter = budget+immediate outcome].
	 * The first pass uses the ValueFunction's expectedValueOutcome column.
	 * 
	 * In the second kind, used during step 10 of the ValueIterator, the ValueFunction's expected fitness outcomes are used is in terms of fitness, 
	 * not outcomes. These expected fitness of an eating or discarding action is the fitness value of starting the second cycle with the 
	 * budget that results from [starting budget + outcome of first cycle]:
	 * 		expected fitness = ValueFunctionFitness(budget + immediate outcome of cycle).
	 *
	 * The boolean argument firstCycle determines which of the two kinds is used (if true, use the latter).
	 * 
	 * UPDATE: now also sets the costs/benefits for each decision node if saveCostsBenefits is true
	 */
	public void backwardPass(boolean firstCycle, ValueIteratorValueFunction valueFunction, boolean saveCostsBenefits) throws InvalidProportionException, ImpossibleStateException, NumberFormatException, InvalidFitnessInputException, REXPMismatchException, REngineException, InvalidProbabilityException, IllegalRangeException, IllegalScaleException
	{
		// Make sure the tree already did a forward pass. 
		if (!this.ranForward)
			throw new IllegalStateException("Exception in DecisionTree.backwardPass(): trying to do a backward pass on a decision tree that has not yet ran a forwards pass.");
	
		// Make sure that the tree did not already do a backward pass (although doing this pass twice does not change the results, it's just inefficient)
		if (this.ranBackward)
			return;

		/* Working backwards in 'time', starting at the end of the network, set the best action and expected outcome
		 * for each node at that time.
		 * Note that the node list is sorted on time. This means that we can traverse the list from back to front,
		 * and never find ourselves in a situations where we did not yet compute all children of a given node.
		 *
		 * Note that if the root done is DEAD_ON_START, we do not need backwards pass at all.
		 */

		if (!(root.bestAction.contains(Action.DEAD_ON_START))) {
			for (int n=nodes.size()-1; n>=0; n--) {
				DecimalNumber sampling = calculateExpectedOutcomeSampling(nodes.get(n), firstCycle);
				
				// EATING
				DecimalNumber eating;
				if (estimationParameters.optimizer == Optimizer.Lossless)
					eating = OPTIMIZED_LOSSLESS_calculateExpectedOutcomeEating(nodes.get(n), firstCycle, valueFunction);
				
				else if (estimationParameters.optimizer == Optimizer.Lossy) {
					eating = OPTIMIZED_LOSSY_calculateExpectedOutcomeEating(nodes.get(n), firstCycle, valueFunction, saveCostsBenefits);
//					DecimalNumber eatingNew = OPTIMIZED_LOSSY_calculateExpectedOutcomeEatingNEW(nodes.get(n), firstCycle, valueFunction);
//					if (!eating.equals(eatingNew, false))
//						System.err.println("Previous: " + eating.toString(6) + ".\t\tNew: " + eatingNew.toString(6) + "\t\tEqual aprox:" + eating.equals(eatingNew, true) + "\tEquals abs: " + eating.equals(eatingNew, false));
//						
				}
				else if (estimationParameters.optimizer == Optimizer.None)
					eating = calculateExpectedOutcomeEating(nodes.get(n), firstCycle, valueFunction);
				else
					throw new IllegalArgumentException("Exception in DecisionTree.backwardPass(): unknown optimizer specified: " + estimationParameters.optimizer + " is not implemented for the backwards pass");
				
				// DISCARDING
				DecimalNumber discarding;
				if (estimationParameters.optimizer == Optimizer.Lossy) {

					discarding = OPTIMIZED_LOSSY_calculateExpectedOutcomeDiscarding(nodes.get(n), firstCycle, valueFunction, saveCostsBenefits);
//					DecimalNumber discardingOLD = calculateExpectedOutcomeDiscarding(nodes.get(n), firstCycle, valueFunction);
//					if (!discarding.equals(discardingOLD, false))
//						System.err.println("Previous: " + discardingOLD.toString(6) + ".\t\tNew: " + discarding.toString(6) + "\t\tEqual aprox:" + discarding.equals(discardingOLD, true) + "\tEquals abs: " + discarding.equals(discardingOLD, false));
////					
					}
				else if (estimationParameters.optimizer == Optimizer.None || estimationParameters.optimizer == Optimizer.Lossless)
					discarding = calculateExpectedOutcomeDiscarding(nodes.get(n), firstCycle, valueFunction);
				else
					throw new IllegalArgumentException("Exception in DecisionTree.backwardPass(): unknown optimizer specified: " + estimationParameters.optimizer + " is not implemented for the backwards pass");
				
				nodes.get(n).setExpectedOutcomes( new DecimalNumberArray(sampling, eating, discarding));
				//nodes.get(n).setCostsAndBenefitsAccepting(firstCycle, valueFunction);
				//nodes.get(n).setCostsAndBenefitsDiscarding(firstCycle, valueFunction);
				}
		}

		this.ranBackward = true;
	}

	//TODO: javadoc. Sampling is costly, and hence the expected outcome is the expected outcome of the children minus cost.
	/** <pre>
	 * Set the expected outcome of sampling for a specified DecisionNode. If the DecisionNode does not have any children
	 * (i.e., no sampling is possible), or the decisionNodes children are all dead,  a value of NEGATIVE_INFINITY is assigned
	 * @param dn
	 * @return
	 * @throws InvalidProportionException 
	 * @throws IllegalRangeException 
	 * @throws IllegalScaleException 
	 */
	private DecimalNumber calculateExpectedOutcomeSampling(DecisionNode dn, boolean firstCycle) throws InvalidProportionException, IllegalRangeException, IllegalScaleException
	{
		//System.err.println("Expected outcome of sampling");
		// Check if the DecisionNode has alive children. If not, set the expected outcome to -Infinity
		if (dn.childEdges.size()==0)  
			return DecimalNumber.NEGATIVE_INFINITY;

		// Otherwise, find the expected outcome of sampling one more cue. This expected outcome is the sum of a child's expected outcome,
		// discounted by the probability of receiving a cue that results in that child - for all children.
		DecimalNumber totalProportion = new DecimalNumber(0,false);
		DecimalNumber expectedOutcome = new DecimalNumber(0);

		for (DecisionEdge de: dn.childEdges)
		{
			DecimalNumber expectedOutcomeChild 	= de.child.expectedOutcomes().max();
			DecimalNumber probabilityOfChild   	= de.proportion;
			totalProportion.add(probabilityOfChild);
			expectedOutcome.add(expectedOutcomeChild.multiply(probabilityOfChild));
		}

		// Make sure the results are valid (allowing for a small margin of error - there might be a rounding issue between R and JAVA
		if (!totalProportion.equals(DecimalNumber.ONE, true))
			throw new Helper.InvalidProportionException("During the backwardPass(): during computation of the expected outcome of sampling: sum of children did not equal 1. Instead, the proportion summed to " + totalProportion);	

		//During estimation of expected VALUE outcome: sampling is costly: hence we have to subtract the cost of sampling from the expected outcome of the children
		if (firstCycle)
			return expectedOutcome.setImmutable(true);
		else
			return expectedOutcome.subtract(mdp.COST_OF_SAMPLING).setImmutable(true);
	}

	/** <pre>
	 * There are two ways to calculate the expected outcome of eating, depending on whether we are computing
	 * expected values (i.e., the "Expected value" column of the value function, which houses the direct outcomes 
	 * from resource encounters and extrinsic events), or whether we are computing expected terminal fitness (and the 
	 * input is a the "Expected fitness" column in the value function - but only after it has been computed!). Note
	 * that it is called terminal, since it represents the fitness associated with the budget at the terminal time step
	 * (see the description above ValueIterator class for more information). The ValueIterator agent computes
	 * expected VALUES in the 7th step (to calculate the expected future value outcomes from cycle 2 to the final cycle), 
	 * and computes expected FITNESS values in the 10th step (to calculate fitness outcomes in the first cycle - see
	 * comments about the ValueIterator class).
	 * 
	 * A brief summary of the value iterator procedure. Remember that i'll refer to the outcome of a resource encounter 
	 * and the following extrinsic event as a cycle. If the agents has a maximum of n cycles to go through, it first calculates
	 * the expected value obtained in all cycles following the first cycle (i.e., cycle 2 to cycle n), and calculates 
	 * the expected fitness value in the 1st cycle. That is, an agent first computes the expected
	 * future value outcome of all FUTURE cycles, and then uses these estimates to compute the expected fitness outcome
	 * of the first cycle.
	 *
	 *	Computing the expected VALUE outcome of eating
	 *	******************************************************************
	 * Based on the Bellman equation, we can write the expected value of eating as a sum of two parts:
	 *
	 *  1. the immediate (value or fitness) outcome  of eating (If the resource remains available) and the value
	 *  	of the extrinsic event that follows immediately afterwards
	 *  2. the (discounted) expected (value or fitness) of all future encounters, given the resulting budget
	 *  	from the immediate outcome
	 *
	 *  What happens when eating? With a probability of (interruption rate) the resource disappears, resulting
	 *  in a resource value of 0. With a probability of (1-interruption rate) the resource does not disappear, and
	 *  is consumed. In this case the agent receives the resource value. The node's posterior probability distribution
	 *  over all possible resource values determines how likely each value is.
	 *
	 *	If the agent's budget after the resource encounter is lower than or equal to 0, it dies and will not receive
	 *	an extrinsic event. If the agent survives the resource encounter (i.e., it's budget is higher than zero), 
	 *	it goes through an extrinsic event, regardless of whether the resource disappeared. The probability distribution of
	 *  extrinsic events is given by the environment.
	 *  
	 *  If the agent survives the extrinsic encounter as well, it will go to the next <resource encounter - extrinsic event>
	 *  pair. What we want to know is, for each possible resource value and extrinsic event value, what the expected
	 *  value outcome is, and take the weighted sum of all these expected value outcomes (weighted by the probability that
	 *  the resource value and extrinsic event have those values). The expected value outcome encompasses both the
	 *  immediate value outcome (the change in budget), and all (discounted) expected future outcomes, given that the
	 *  agent starts the next encounter with the budget (current budget + change in budget).
	 *  
	 *  Let's formalize these ideas. First, let EFVO(x) be the sum of all future value outcomes given that the agent starts the
	 *  next encounter with budget x. These values are stored in the value function (how they are computed is explained
	 *  in the comments in the ValueIterator class). Hence:
	 *  
	 *  EFVO(x)			:=			VFv(x), where VFv is the "value" column of the value function (as opposed to VFf, which is the 'fitness' column)
	 *  		
	 *  Next, let the function EV(rv, ee) be:
	 *  
	 *  EV(rv, ee) 	:=		the expected value outcome of eating, given that the resource value has value rv and the extrinsic event value has value ee
	 *  			 = 		[immediate outcome] + [discount rate]*[expected future outcome].
	 *  			 = 		[rv+ee] + [discount rate]*EFVO(current budget + rv + ee)
	 *  
	 *  Next, let EV be a [n by n] matrix that contains all possible resource values on the rows and all possible extrinsic events on the columns, 
	 *  and whose entries represent the expected value outcome associated with that resource value and extrinsic event:
	 *  													___													  ___
	 *  EV			 = 										| EV(rv = -n, ee = -n)		...		EV(rv = -n, ee = n) |
	 *  													| ...						...		...					|
	 *  													|_EV(rv = n, ee = -n)		...		EV(rv = n, ee = n)__|
	 *  
	 *  where n is the n'th entry in the list of all possible (resource value and extrinsic event) values. Similarly, let 
	 *  PEV be the matrix that contains the probability of each entry in EV, such that:
	 *   													___													         ___
	 *  PEV			 = 										| p(EV(rv = -n, ee = -n))		...		p(EV(rv = -n, ee = n)) |
	 *  													| ...							...		...					   |
	 *  													|_p(EV(rv = n, ee = -n))		...		p(EV(rv = n, ee = n))__|
	 *  
	 *  If we compute a new matrix that is the element-wise multiplication of EV and PEV and summing up all entries we get the the total 
	 *  expected value of eating for the this decision node. Hence, we have to define how to compute EV.
	 *  
	 *  First, lets compute IM, the matrix containing all the immediate rewards for each possible resource value and extrinsic event value.
	 *  Let RV be the column vector that specifies all possible resource values, and EE be the column vector that specifies all possible
	 *  extrinsic event values:
	 *  													__   __
	 *  													| -n  |
	 *  													| ... |
	 *  RV 			=			EE			=				|  0  |  
	 *    													| ... |
	 *     													|_ n _|
	 *     
	 *  A first idea might be that IM can be computed as RV*U(T) + U*EE(T), where U is a unity vector of length n (i.e., only contains 1's), 
	 *  and (T) means the transposed. This operation gives:
	 *  													 __								         __
	 *														| RV[0]+EE[0]		...		RV[0]+EE[j]   |
	 *   IMincorrect		 = 		RV*U(T) + U*EE(T)		=		| ...				...		...			  |
	 *  													|_RV[i]+EE[0]		...		RV[i]+EE[j]  _|
	 *  where i is the length of RV, and j is the length of EE.
	 *  
	 *  However, this is incorrect for two reasons. 
	 *  
	 *  The first reason is that this approach does not take into account that the agent might die if the 
	 *  resource value decreases its budget below 0. If the agent dies before the extrinsic event, it does not go through that extrnisic event. 
	 *  What we want is to remove all "+EE[x]" parts in the matrix where budget + RV[n] <= 0.
	 *  Note that, since the resource values are constant in the same row, this means we have to change IM such that:
	 *   													 __										__
	 *   													| RV[0]				...		RV[0]  		  |
	 *   													| ...				...		...			  |
	 *  IMcorrect	 = 		???						=		| RV[x]+EE[0]		...		RV[x]+EE[0]	  |
	 *   													| ...				...		...			  |
	 *  													|_RV[i]+EE[0]		...		RV[i]+EE[j]  _|
	 *  
	 *  Where x is the first position in RV such that budget+RV > 0. 
	 *  
	 *  The way to compute IM is use a [n by n] matrix F (short for filter), which has 0's on all rows r where budget + RV[r] <0 and contains 1's otherwise.
	 *  This filter can be computed with  function( (RV+budget) * U(T) ), where 
	 *  
	 *  function(x)	= 										1		if x > 0
	 *  													0		otherwise 
	 *  
	 *  Hence, F * U(T) gives us a matrix of 0's and 1's.
	 *  By applying this filter to U*EE(T), we are left with a matrix that has 0's on all positions where RV+budget<0, and contains EE[c] on each column c. 
	 *  Hence, the total immediate reward can be computed as:
	 *  
	 *     													 __										__
	 *   													| RV[0]				...		RV[0]  		  |
	 *   													| ...				...		...			  |
	 *  IMalmostcorrect =  RV*U(T) + U*EE(T)  .* F	=		| RV[x]+EE[0]		...		RV[x]+EE[0]	  |
	 *   													| ...				...		...			  |
	 *  													|_RV[i]+EE[0]		...		RV[i]+EE[j]  _|
	 *  Where * denotes matrix multiplication and .* denotes elementwise multiplication.
	 *  
	 *  The second reason there is a minimum and a maximum budget - the agent should never have budgets below 0 or above the maximum. 
	 *  Because the agents budget cannot fall below 0 or above the maximum, we have to winsorize the immediate outcomes. For instance,
	 *  if the maximum budget is 5 and the agent has a budget of 3 and it receives an immediate outcome of -4. In this case it can only 
	 *  occur a damage of -3 (brining its budget to 0), and hence the immediate outcome is -3. Similarly, if it were to receive an immediate
	 *  outcome of +3, it will only receive +2 instead. Note that this applies to the resource encounter outcome first, and to the 
	 *  extrinsic event outcome second.
	 *  
	 *  For the resource encounter this is rather straightforward to solve: rather than using RV (the array of all resource values), we use
	 *  RVcorrected, which we get by changing RV such that its entries never push the agent's budget below 0 or above the maximum:
	 *  
	 *  RVcorrected[i]	=	RV[i]				   	if budget + RV[i] > 0 and budget + RV[i] < BUDGET_MAXIMUM
	 *  					-Budget					if budget + RV[i] < 0
	 *  					BUDGET_MAXIMUM-budget	if budget + RV[i] > BUDGET_MAXIMUM  
	 *  
	 *  We can use RVcorrected in the computation of IMalmostCorrected:
	 *  
	 *  IMalmostcorrected = RVcorrected*U(T) + U*EE(T)  .* F
	 *  
	 *  After using RVcorrected in the computation of IMalmostcorrect we can winsorize the immediate outcomes of the extrinsic event outcomes
	 *  by winsorizing all values in IMalmostcorrect:
	 *  
	 *  IM[i,j]			= 	IMalmostcorrect[i,j]		if 0 < budget + IMalmostcorrect[i,j] <  BUDGET_MAXIMUM
	 *  					BUDGET_MAXIMUM - budget		if budget + IMalmostcorrect[i,j] > BUDGET_MAXIMUM
	 *  					-budget						if budget + IMalmostcorrect[i,j] < 0 
	 *  			
	 *  
	 *  
	 *  Next, we have to figure out how to compute the future expected value outcomes. Using the same function on IM, we get IMpositive:
	 *  
	 *     													 __								__
	 *   													| 0				...		0  		  |
	 *   													| ...			...		...		  |
	 *  IMpositive =		function(IM)			=		| IM(x,0)		...		IM(x,j)	  |
	 *   													| ...			...		...		  |
	 *  													|_IM(i,0)		...		IM(i,j)  _|
	 *  We can then find the discounted future value by:
	 *  
	 *  discounted future value outcome (DFVO)		=		[discount rate] * VFv(budget + IM) 
	 *  	   												 __																		  __
	 *   													| lambda*VFv(budget + IM[0,0])		...		lambda*VFv(budget + IM[0,j]) 	|
	 *  											=		| ...								...		...	 							|
	 *  													|_lambda*VFv(budget + IM[i,0])		...		lambda*VFv(budget + IM[i,j])   _|
	 *  Where lambda is the discount rate.
	 *  (Note that, computationally speaking, we go from IM to the discounted future value outcome in one go).
	 *  
	 *  EV is then computed as IM + discounted future value outcome, or:
	 *  
	 *  EV	=	IM	+ lambda * VFv ( IM + budget )
	 *  
	 *  
	 *  And to reiterate: the total expected value of eating is the sum of the matrix that results from an elementwise
	 *  multiplication of EV and PEV - the latter containing the probability of each expected value occurring. However, 
	 *  we have to take into account the interruption rate delta. There is a straight forward way to take the interruption 
	 *  rate into account. The insight here is that when a resource disappears, the result is the same as when the resource 
	 *  has a value of 0. Hence, we can 'add' on the interruption rate to the probability of a zero valued resource. 
	 *  Rather than using the resource value probabilities in PEV, we can use PRVi (obviously a short-hand
	 *  notation for "Probability distribution Resource Values with Interruption rate taken into account), which is a column vector. 
	 *  If PRV is the column vector containing the probability distribution of resource values, PRVi is defined as :
	 *
	 *  	PRVi[v]  = PRV[i] * (1-interruption rate)						forall v in RV, if RV[v] =/= 0
	 *  	PRVi[v]  = PRV[i] * (1-interruption rate) + interruption rate	if v in RVs and v == 0
	 *  
	 *	We can calculate PEV as the matrix multiplication of PRVi and the transpose of PEE, the column vector containing all
	 *	extrinsic event probabilities. 
	 *  
	 *  													___													         ___
	 *  PEV			 = 	PRVi * t(PEE) =						| p(EV(rv = -n, ee = -n))		...		p(EV(rv = -n, ee = n)) |
	 *  													| ...							...		...					   |
	 *  													|_p(EV(rv = n, ee = -n))		...		p(EV(rv = n, ee = n))__|
	 *  
	 *	Using PEV and EV we can now compute the expected VALUE outcome of eating as:
	 *
	 *	Expected value outcome of eating 	:=  [immediate outcome] + [discount rate]*[expected future value outcome].
	 *										 =  sum p(RV = rv) * p(EE = ee) * ( [immediate outcome|rv, ee] + [discount rate]*[expected future value outcome|rv,ee] )) for all resource values rv and all extrinsic event values ee
	 *										 =  sum ( PEV .* ( IM + DFVO) )
	 *
	 *
	 *
	 *
	 * 	Computing the expected FITNESS outcome of eating
	 *	******************************************************************
	 *	Let VFf be the "Expected fitness" column in the value function (see the ValueIterator class on details how this column is computed).
	 *  The expected fitness outcome os eating is then:
	 *
	 *	expected fitness outcome := sum VFf(starting budget cycle 2) for all possible starting budgets
	 *							  = sum VFf(b + immediate outcome) for all possible starting budgets
	 *	Which is the same as
	 *
	 *	expected fitness outcome = sum [ 	p(RV=rv) * p(EE=ee) * VFf( budget + rv + ee) ]
	 *
	 *	This feels (and is) very similar to the code described above. Remember that above we calculated
	 * 	the expected value outcome of eating as [immediate outcome] + [discount rate]*[expected future value outcome].
	 * 
	 *  Specifically, If we  (a) leave out the immediate outcome part, (b) do not use a discount rate, and (c) change value to fitness (i.e., change VFv to VFf), 
	 *  we get the expected fitness outcome!
	 *  
	 */
	private DecimalNumber calculateExpectedOutcomeEating(DecisionNode dn, boolean firstCycle, ValueIteratorValueFunction valueFunction) 
	{
		// First, set up some of the (unity) vectors that we will be using
		DecimalNumberMatrix RV						= DecimalNumberMatrix.toColumnVector(mdp.POSSIBLE_VALUES.clone());
		
		// Create RVcorrected (we'll store it in RV again).
		RV.apply(new TransformationFunctionDecimalNumber() {
			@Override
			public DecimalNumber function(DecimalNumber argument) {
				if (argument.add(dn.budget, false).compareTo(0)==-1)
					return dn.budget.negate(false);
				else if (argument.add(dn.budget, false).compareTo(mdp.BUDGET_MAXIMUM)==1)
					return mdp.BUDGET_MAXIMUM.subtract(dn.budget);
				return argument;
			}
		});
		

		DecimalNumberMatrix	EE						= DecimalNumberMatrix.toColumnVector(mdp.POSSIBLE_VALUES.clone());
		DecimalNumberMatrix U 						= DecimalNumberMatrix.toColumnVector(DecimalNumberArray.unityVector(RV.nrow()));
		DecimalNumberMatrix	UT 						= U.transpose();

		// Next, compute filter F, which is a column vector of length n (n being the number of possible resource values), 
		// and denotes which of these resource values will, after consumption, leave the agent alive.
		DecimalNumberMatrix F						= RV.clone();
		F.scalarAddition(dn.budget);
		F.apply(new TransformationFunctionDecimalNumber() {
			@Override
			public DecimalNumber function(DecimalNumber argument) {	
				if (argument.compareTo(0)<= 0)	
					return new DecimalNumber(0);	
				return new DecimalNumber(1);
			}});
		F = F.matrixMultiplication(UT);
	
		// Next, compute IM
		DecimalNumberMatrix IM						= RV.matrixMultiplication(UT);
		DecimalNumberMatrix UEET 					= U.matrixMultiplication(EE.transpose());
		UEET.entrywiseMultiplication(F);			// apply filter
		IM.matrixAddition(UEET);


		// Winsorize all IM values that result in a budget lower than 0 or higher than BUDGET_MAXIMUM
		IM.apply(new TransformationFunctionDecimalNumber() {
			@Override
			public DecimalNumber function(DecimalNumber argument) {
				if (dn.budget.add(argument, false).compareTo(0)==-1)
					return dn.budget.negate(false);
				if (dn.budget.add(argument, false).compareTo(mdp.BUDGET_MAXIMUM)==1)
					return mdp.BUDGET_MAXIMUM.subtract(dn.budget);
				return argument;
			}
		});
		

		// Compute the expected future outcomes
		DecimalNumberMatrix EFO;
		// If we are calculating VALUE outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the discounted expected future value outcomes (DEFVO)
		if (!firstCycle) {
			DecimalNumberMatrix DEFVO				= IM.clone();
			DEFVO.scalarAddition(dn.budget);
		
			//Next, we do a couple of steps at the same time. We go over DFVO and set the value to 
			// - 0						if the value at that entry is <= 0
			// lambda*VFv(value)		otherwise
			DEFVO.apply(new TransformationFunctionDecimalNumber() {
				@Override
				public DecimalNumber function(DecimalNumber argument) {	
					if (argument.compareTo(0)<= 0)	
						return new DecimalNumber(0);	
					else		
						return valueFunction.getExpectedFutureValueOutcome(argument).multiply(mdp.DISCOUNT_RATE);
				}});
			EFO = DEFVO;
		}

		// If we are calculating FITNESS outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the expected future fitness outcomes (EFFO)
		else { // i.e., if (firstCycle) 
			DecimalNumberMatrix EFFO				= IM.clone();
			EFFO.scalarAddition(dn.budget);
			EFFO.apply(new TransformationFunctionDecimalNumber() {
				@Override
				public DecimalNumber function(DecimalNumber argument) {	
					if (argument.compareTo(0)<= 0)	
						return new DecimalNumber(0);	
					else		
						return valueFunction.getTerminalFitnessOutcome(argument);
				}});
			EFO = EFFO;
		}

		// Now that we have the immediate rewards IM, and the future outcomes EFO, we need to compute PEV, the probability distributions over resource values and extrinsic event values
		// First, determine the complement of the interruption rate (i.e., 1- interruptionRate).
		// Note that complementOfInterruptionRate (and all derivatives thereof) have the same range limitation as interruptionRate
		DecimalNumber complementOfInterruptionRate = environment.interruptionRate.complementOfOne().setImmutable(true);
	
		// Next, If the MDP specifies a compound interruption rate, the probability that a resource has not while n cues have been sampled is
		// 		probabilityResourceAvailable = (1 - interruption rate) ^ n, or complementOfInterruptionRate^n. 
		// If the MDP does not specify a compound interruption rate, the probability that a resource is available is
		// 		probabilityResourceAvailable = (1 - interruption rate), or complementOfInterruptionRate. 
		// The probability that a resource has disappeared during sampling is (1-probabilityResourceDisappeared).
		DecimalNumber probabilityResourceAvailable;
		if (mdp.COMPOUND_INTERRUPTION_RATE) 
			probabilityResourceAvailable = complementOfInterruptionRate.pow(dn.cuesSampled, false);
		else
			probabilityResourceAvailable = complementOfInterruptionRate;
		DecimalNumber probabilityResourceDisappeared = probabilityResourceAvailable.complementOfOne();

		// Calculate PRVi, a column vector containing the probabilities (adjusted for the interruption rate) of the resource values
		DecimalNumberMatrix PRVi = DecimalNumberMatrix.toColumnVector(	dn.probabilityDistribution.posteriorProbabilityOfResourceValues() );
		PRVi.scalarMultiplication(probabilityResourceAvailable);
		// add the probabilityResourceDisappeared to the probability that the resource value has a value of 0
		int indexPositionOfZero = RV.getIndexOfRowWhereColumnIs(0,0);
		PRVi.getValueAt(indexPositionOfZero, 0).add(probabilityResourceDisappeared);

		// Get PEE, the column vector containing the probabilities of each extrinsic event
		DecimalNumberMatrix PEE = DecimalNumberMatrix.toColumnVector(	environment.getMatrix().getColumn("p(Extrinsic event value = value)"));

		// Almost there: compute PEV:
		DecimalNumberMatrix PEV = PRVi.matrixMultiplication(PEE.transpose());

		// Finally, compute the expected VALUE outcome of eating, defined as 			sum ( PEV .* ( IM + EFO) )
		DecimalNumber expectedOutcome;
		if (! firstCycle)
			expectedOutcome = DecimalNumberMatrix.entrywiseMultiplication(PEV, DecimalNumberMatrix.matrixAddition(IM, EFO)).sum();

		// Or, alternatively, if we are interested in terminal FITNESS outcomes:		sum ( PEV .* EFO )
		else
			expectedOutcome = PEV.entrywiseMultiplication(EFO).sum();

//		System.err.println("\n\n\nExcepted outcome of eating. Budget: " + dn.budget + ". Cues sampled: " + Helper.arrayToString(dn.cueSet));
//		String[] colNames = new String[mdp.POSSIBLE_VALUES.length()];
//		String[] rowNames = new String[mdp.POSSIBLE_VALUES.length()];
//		for (int i = 0; i < colNames.length; i ++) {
//			colNames[i] = mdp.POSSIBLE_VALUES.get(i).toString(1);
//			rowNames[i] = mdp.POSSIBLE_VALUES.get(i).toString(1);
//		}
//		
//		IM.setRowNames(rowNames);
//		IM.setColumnNames(colNames);
//		System.err.println("Immediate outcome: " + IM.toString(2, 7));
//		
//		EFO.setRowNames(rowNames);
//		EFO.setColumnNames(colNames);
//		System.err.println("EFO: " + EFO.toString(2,7));
//		
//		PEV.setRowNames(rowNames);
//		PEV.setColumnNames(colNames);
//		System.err.println("PEV: " + PEV.toString(2,7));
//		
//		System.err.println("OUT " + expectedOutcome.toString(2));

		return expectedOutcome.setImmutable(true);


	}

	/**
	 * An optimized version of calculateExpectedOutcomeEating that increases runtime speed (about 40-50% percent faster than 
	 * calculateExpectedOutcomeEating), while maintaining the decimal accuracy provided by DecimalNumber.
	 * 
	 * For details on what the algorithm is doing, see the javadoc associated with calculateExpectedOutcomeEating().
	 * @param dn
	 * @param firstCycle
	 * @param valueFunction
	 * @return
	 */
	private DecimalNumber OPTIMIZED_LOSSLESS_calculateExpectedOutcomeEating(DecisionNode dn, boolean firstCycle, ValueIteratorValueFunction valueFunction) 
	{
		// Rather than using RVcorrect (the matrix containing the usable resource values (i.e., >= -budget, <= maximum-budget),
		// we can multiply all values with 1/(mdp.VALUE_STEP * mdp.BUDGET_STEP), which ensures that, until we retrieve values
		// from the ValueFunction, all values are integers. After we have retrieved values from the value function, we still
		// have to use DecimalNumber if we want to avoid floating point issues. See OPTIMIZED_LOSSY for an implementation 
		// that does not switch back to DecimalNumbers (which has superior speed but is not precise when dealing with small values).
		
		// Initialize some constants, vectors and matrices
		DecimalNumberMatrix RV						= DecimalNumberMatrix.toColumnVector(mdp.POSSIBLE_VALUES.clone());
		DecimalNumber scalingFactor					= DecimalNumber.ONE.divide(mdp.VALUE_STEP).multiply(DecimalNumber.ONE.divide(mdp.BUDGET_STEP));
		DecimalNumber inverseScalingFactor			= DecimalNumber.ONE.divide(scalingFactor);
		double budgetScaled = dn.budget.multiply(scalingFactor).doubleValue();
		double maximumBudgetScaled = mdp.BUDGET_MAXIMUM.multiply(scalingFactor).doubleValue();
		RV.scalarMultiplication(scalingFactor);
		double[][] RVscaled 							= RV.toDoubleMatrix();
		
		for (int r = 0; r < RVscaled.length; r++)
			if ( RVscaled[r][0] + budgetScaled < 0 )
				RVscaled[r][0] = -budgetScaled;
			else if (RVscaled[r][0] + budgetScaled > maximumBudgetScaled)
				RVscaled[r][0] = maximumBudgetScaled - budgetScaled;		
		double[][]	EE						= RV.toDoubleMatrix();
		double[][]  U						= DecimalNumberMatrix.DOUBLE_unityMatrix(RVscaled.length, 1);
		double[][]  UT						= DecimalNumberMatrix.DOUBLE_unityMatrix(1, RVscaled.length);
		
		// Compute filter F
		double[][] F = new double[RVscaled.length][1];
		for (int r = 0; r < RVscaled.length; r++)
			if (RVscaled[r][0] + budgetScaled > 0)
				F[r][0]=1;
		F = DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(F, UT);

		// Compute IM (IMmediate outcome) and UEET ( (Unity vector) * transposed(Extrinsic Event vector)
		double[][]  IM						= DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(RVscaled, UT);
		double[][]  UEET					= DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(U, DecimalNumberMatrix.DOUBLE_transpose(EE));
		UEET = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(UEET, F);
		IM									= DecimalNumberMatrix.DOUBLE_matrixAddition(IM, UEET);
		
		// Winsorize all IM values that result in a scaled budget lower than 0 or higher than maximumBudgetScaled
		for (int r = 0; r < IM.length; r++)
			for (int c= 0; c < IM.length; c++) 
				if (IM[r][c]+budgetScaled < 0)
					IM[r][c] = -budgetScaled;
				else if (IM[r][c] + budgetScaled > maximumBudgetScaled)
					IM[r][c] = maximumBudgetScaled - budgetScaled;

		// Compute the expected future outcomes
		DecimalNumberMatrix EFO = null;
	
		// If we are calculating VALUE outcomes: compute the EXPECTED FUTURE VALUE OUTCOMES (EFVO) as the discounted expected future value outcomes (DEFVO)
		if (!firstCycle) {
			DecimalNumberMatrix DEFVO = new DecimalNumberMatrix(IM.length, IM.length);
			for (int r = 0; r < IM.length; r++)
				for (int c= 0; c < IM.length; c++) 
					if (IM[r][c] + budgetScaled <= 0 )
						DEFVO.getRow(r).set(c, new DecimalNumber(0)); 
					else {
						DecimalNumber budget = new DecimalNumber(IM[r][c] + budgetScaled).multiply(inverseScalingFactor);
						DEFVO.getRow(r).set(c, valueFunction.getExpectedFutureValueOutcome(budget).multiply(mdp.DISCOUNT_RATE));

					}
			EFO=DEFVO;		
		}


		// If we are calculating FITNESS outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the expected future fitness outcomes (EFFO)
		else if (firstCycle) { 
			DecimalNumberMatrix EFFO = new DecimalNumberMatrix(IM.length, IM.length);
			for (int r = 0; r < IM.length; r++)
				for (int c= 0; c < IM.length; c++) 
					if (IM[r][c] + budgetScaled <= 0 )
						EFFO.getRow(r).set(c, new DecimalNumber(0)); 
					else {
						DecimalNumber budget = new DecimalNumber(IM[r][c] + budgetScaled).multiply(inverseScalingFactor);
						EFFO.getRow(r).set(c, valueFunction.getTerminalFitnessOutcome(budget));

					}
			EFO=EFFO;
		}

		// Switch back to DecimalNumber
		DecimalNumberMatrix IM2 = new DecimalNumberMatrix(IM);
		IM2.scalarMultiplication(inverseScalingFactor);
		
		// Compute PEV, the probability distributions over resource values and extrinsic event values
		// Determine the complement of the interruption rate (i.e., 1- interruptionRate).
		DecimalNumber complementOfInterruptionRate = environment.interruptionRate.complementOfOne().setImmutable(true);
	
		// If the MDP specifies a compound interruption rate, the probability that a resource has not while n cues have been sampled is
		// 		probabilityResourceAvailable = (1 - interruption rate) ^ n, or complementOfInterruptionRate^n. 
		// If the MDP does not specify a compound interruption rate, the probability that a resource is available is
		// 		probabilityResourceAvailable = (1 - interruption rate), or complementOfInterruptionRate. 
		// The probability that a resource has disappeared during sampling is (1-probabilityResourceDisappeared).
		DecimalNumber probabilityResourceAvailable;
		if (mdp.COMPOUND_INTERRUPTION_RATE) 
			probabilityResourceAvailable = complementOfInterruptionRate.pow(dn.cuesSampled, false);
		else
			probabilityResourceAvailable = complementOfInterruptionRate;
		DecimalNumber probabilityResourceDisappeared = probabilityResourceAvailable.complementOfOne();

		// Calculate PRVi, a column vector containing the probabilities (adjusted for the interruption rate) of the resource values
		DecimalNumberMatrix PRVi = DecimalNumberMatrix.toColumnVector(	dn.probabilityDistribution.posteriorProbabilityOfResourceValues() );
		PRVi.scalarMultiplication(probabilityResourceAvailable);
		// add the probabilityResourceDisappeared to the probability that the resource value has a value of 0

		int indexPositionOfZero = RV.getIndexOfRowWhereColumnIs(0,0);
		PRVi.getValueAt(indexPositionOfZero, 0).add(probabilityResourceDisappeared);

		// Get PEE, the column vector containing the probabilities of each extrinsic event
		DecimalNumberMatrix PEE = DecimalNumberMatrix.toColumnVector(	environment.getMatrix().getColumn("p(Extrinsic event value = value)"));

		// Almost there: compute PEV:
		DecimalNumberMatrix PEV = PRVi.matrixMultiplication(PEE.transpose());

		// Finally, compute the expected VALUE outcome of eating, defined as 			sum ( PEV .* ( IM + EFO) )
		DecimalNumber expectedOutcome;
		if (! firstCycle)
			expectedOutcome = DecimalNumberMatrix.entrywiseMultiplication(PEV, DecimalNumberMatrix.matrixAddition(IM2, EFO)).sum();

		// Or, alternatively, if we are interested in terminal FITNESS outcomes:		sum ( PEV .* EFO )
		else
			expectedOutcome = PEV.entrywiseMultiplication(EFO).sum();

		return expectedOutcome.setImmutable(true);

	}

	/**
	 * @deprecated
	 * An optimized version of calculateExpectedOutcomeEating that increases runtime speed (about 80-85% percent faster than 
	 * calculateExpectedOutcomeEating), at the cost of accuracy. Specifically, this implementation CANNOT GARANTEE THE ABSENCE
	 * OF FLOATING POINT ERRORS. Hence, results cannot be trusted after the 9th (or so) decimal point. Note that this
	 * is a margin of error in which the DecimalNumber.equals() function will return true if approximation is used.
	 * 
	 * For details on what the algorithm is doing, see the javadoc associated with calculateExpectedOutcomeEating().
	 * @param dn
	 * @param firstCycle
	 * @param valueFunction
	 * @return
	 */
	private DecimalNumber OPTIMIZED_LOSSY_calculateExpectedOutcomeEating_NO_SCALING(DecisionNode dn, boolean firstCycle, ValueIteratorValueFunction valueFunction) 
	{
		// Rather than using RVcorrect (the matrix containing the usable resource values (i.e., >= -budget, <= maximum-budget),
		// we can multiply all values with 1/(mdp.VALUE_STEP * mdp.BUDGET_STEP), which ensures that, until we retrieve values
		// from the ValueFunction, all values are integers. After we have retrieved values from the value function, we still
		// have to use DecimalNumber if we want to avoid floating point issues. See OPTIMIZED_LOSSY for an implementation 
		// that does not switch back to DecimalNumbers (which has superior speed but is not precise when dealing with small values).

		// Initialize some constants, vectors and matrices
		DecimalNumberMatrix RV						= DecimalNumberMatrix.toColumnVector(mdp.POSSIBLE_VALUES.clone());
		DecimalNumber scalingFactor					= DecimalNumber.ONE.divide(mdp.VALUE_STEP).multiply(DecimalNumber.ONE.divide(mdp.BUDGET_STEP));
		double inverseScalingFactor					= DecimalNumber.ONE.divide(scalingFactor).doubleValue();
		DecimalNumber inverseScalingFactorDN		= new DecimalNumber(inverseScalingFactor);
		double budgetScaled 						= dn.budget.multiply(scalingFactor).doubleValue();//dn.budget.doubleValue() * scalingFactor.doubleValue();
		double maximumBudgetScaled 					= mdp.BUDGET_MAXIMUM.multiply(scalingFactor).doubleValue();
		double discountRate							= mdp.DISCOUNT_RATE.doubleValue();
		RV.scalarMultiplication(scalingFactor);
		double[][] RVscaled 						= RV.toDoubleMatrix();

		for (int r = 0; r < RVscaled.length; r++)
			if ( RVscaled[r][0] + budgetScaled < 0 )
				RVscaled[r][0] = -budgetScaled;
			else if (RVscaled[r][0] + budgetScaled > maximumBudgetScaled)
				RVscaled[r][0] = maximumBudgetScaled - budgetScaled;		
		double[][]	EE								= RV.toDoubleMatrix();
		double[][]  U								= DecimalNumberMatrix.DOUBLE_unityMatrix(RVscaled.length, 1);
		double[][]  UT								= DecimalNumberMatrix.DOUBLE_unityMatrix(1, RVscaled.length);

		// Compute filter F
		double[][] F = new double[RVscaled.length][1];
		for (int r = 0; r < RVscaled.length; r++)
			if (RVscaled[r][0] + budgetScaled > 0)
				F[r][0]=1;
		F = DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(F, UT);

		// Compute IM (IMmediate outcome) and UEET ( (Unity vector) * transposed(Extrinsic Event vector)
		double[][]  IM								= DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(RVscaled, UT);
		double[][]  UEET							= DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(U, DecimalNumberMatrix.DOUBLE_transpose(EE));
		UEET 										= DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(UEET, F);
		IM											= DecimalNumberMatrix.DOUBLE_matrixAddition(IM, UEET);

		// Winsorize all IM values that result in a scaled budget lower than 0 or higher than maximumBudgetScaled
		for (int r = 0; r < IM.length; r++)
			for (int c= 0; c < IM.length; c++) 
				if (IM[r][c]+budgetScaled < 0)
					IM[r][c] = -budgetScaled;
				else if (IM[r][c] + budgetScaled > maximumBudgetScaled) 
					IM[r][c] = maximumBudgetScaled - budgetScaled;
				
		// AFTER THIS POINT THE CODE WILL BE LOSSY
		// If this is the first cycle, we will first compute EFO (that is, EFVO), and then later on (after we have 
		// computed PEV, the Probability of Expected Values) add EFO to IM (which needs to be rescaled). Rather
		// than doing this in two loops, we can do this immediately in the first loop where we calculate 
		// Since we are going to iterate over all values in IM anyway, we might as well store the sum of
		// the scaled IM and EFVO when we compute EFVO, which saves us an additional loop. Hence, here, VALUE_OUTCOMES)
		// (for the first cycle) refers to EFVO+IM.
		double[][] VALUE_OUTCOMES = null;
		
		// Create a reference to EFFO
		double[][] EFFO = null;
		
		if (!firstCycle) {
			VALUE_OUTCOMES = new double[IM.length][IM.length];
			for (int r = 0; r < IM.length; r++)
				for (int c= 0; c < IM.length; c++) 
					if (IM[r][c] + budgetScaled <= 0 )
						VALUE_OUTCOMES[r][c]=IM[r][c] * inverseScalingFactor;
					else {
						DecimalNumber budget = new DecimalNumber((IM[r][c] + budgetScaled)).multiply(inverseScalingFactorDN);
						double discountedEFVO = valueFunction.getExpectedFutureValueOutcomeDouble(budget) * discountRate;
						VALUE_OUTCOMES[r][c] = IM[r][c] * inverseScalingFactor + discountedEFVO;
					}
		}

		// If we are calculating FITNESS outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the expected future fitness outcomes (EFFO)
		else if (firstCycle) {
			EFFO = new double[IM.length][IM.length];
			for (int r = 0; r < IM.length; r++)
				for (int c= 0; c < IM.length; c++) 
					if (IM[r][c] + budgetScaled <= 0 )
						EFFO[r][c]=0;
					else {
						DecimalNumber budget = new DecimalNumber(IM[r][c] + budgetScaled).multiply(inverseScalingFactor);
						EFFO[r][c] = valueFunction.getTerminalFitnessOutcome(budget).doubleValue();
					}
		}

		// Now that we have the immediate rewards IM, and the future outcomes EFO, we need to compute PEV, the probability distributions over resource values and extrinsic event values
		// First, determine the complement of the interruption rate (i.e., 1- interruptionRate).
		// Note that complementOfInterruptionRate (and all derivatives thereof) have the same range limitation as interruptionRate
		DecimalNumber complementOfInterruptionRate = environment.interruptionRate.complementOfOne().setImmutable(true);
	
		// Next, If the MDP specifies a compound interruption rate, the probability that a resource has not while n cues have been sampled is
		// 		probabilityResourceAvailable = (1 - interruption rate) ^ n, or complementOfInterruptionRate^n. 
		// If the MDP does not specify a compound interruption rate, the probability that a resource is available is
		// 		probabilityResourceAvailable = (1 - interruption rate), or complementOfInterruptionRate. 
		// The probability that a resource has disappeared during sampling is (1-probabilityResourceDisappeared).
		DecimalNumber probabilityResourceAvailable;
		if (mdp.COMPOUND_INTERRUPTION_RATE) 
			probabilityResourceAvailable = complementOfInterruptionRate.pow(dn.cuesSampled, false);
		else
			probabilityResourceAvailable = complementOfInterruptionRate;
		DecimalNumber probabilityResourceDisappeared = probabilityResourceAvailable.complementOfOne();

		// Calculate PRVi, a column vector containing the probabilities (adjusted for the interruption rate) of the resource values
		double probabilityResourceAvailableDouble = probabilityResourceAvailable.doubleValue();
		double[][] PRVi = DecimalNumberMatrix.toColumnVector(	dn.probabilityDistribution.posteriorProbabilityOfResourceValues() ).toDoubleMatrix();
		for (int r = 0; r < PRVi.length; r++)
			PRVi[r][0] *= probabilityResourceAvailableDouble;
		
		// add the probabilityResourceDisappeared to the probability that the resource value has a value of 0
		int indexPositionOfZero = 0;
		for (int r = 0; r < RVscaled.length; r++)
			if (RVscaled[r][0] == 0) {
				indexPositionOfZero = r;
				break;
			}
		PRVi[indexPositionOfZero][0] += probabilityResourceDisappeared.doubleValue();
		
		// Get PEE, the column vector containing the probabilities of each extrinsic event
		double[][] PEE = DecimalNumberMatrix.toColumnVector(	environment.getMatrix().getColumn("p(Extrinsic event value = value)")).toDoubleMatrix();

		// Almost there: compute PEV:
		double[][] PEV = DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(PRVi, DecimalNumberMatrix.DOUBLE_transpose(PEE));

		// Finally, compute the expected VALUE outcome of eating, defined as 			sum ( PEV .* ( IM + EFO) )
		if (! firstCycle) {
			double[][] expectedOutcome 				= DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEV, VALUE_OUTCOMES);
			double sum = 0;
			for (int r = 0; r < expectedOutcome.length; r++)
				for (int c = 0; c < expectedOutcome.length; c++ )
					sum += expectedOutcome[r][c];
			return new DecimalNumber(sum).setImmutable(true);
		}
			

		// Or, alternatively, if we are interested in terminal FITNESS outcomes:		sum ( PEV .* EFO )
		else {
			double[][] expectedOutcome 				= DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEV, EFFO);
			double sum = 0;
			for (int r = 0; r < expectedOutcome.length; r++)
				for (int c = 0; c < expectedOutcome.length; c++ )
					sum += expectedOutcome[r][c];
			return new DecimalNumber(sum).setImmutable(true);
		}
			

	}
	
	/**
	 * An optimized version of calculateExpectedOutcomeEating that increases runtime speed (about 80-85% percent faster than 
	 * calculateExpectedOutcomeEating), at the cost of accuracy. Specifically, this implementation CANNOT GARANTEE THE ABSENCE
	 * OF FLOATING POINT ERRORS. Hence, results cannot be trusted after the 9th (or so) decimal point. Note that this
	 * is a margin of error in which the DecimalNumber.equals() function will return true if approximation is used.
	 * 
	 * For details on what the algorithm is doing, see the javadoc associated with calculateExpectedOutcomeEating().
	 * 
	 * Also note that in contrast to the other (non-lossy and slower) implementations, this implementation computes the cost and benefits of accepting and
	 * discarding for each decision node (if saveCostBenefits is true)
	 * @param dn
	 * @param firstCycle
	 * @param valueFunction
	 * @return
	 */
	private DecimalNumber OPTIMIZED_LOSSY_calculateExpectedOutcomeEating(DecisionNode dn, boolean firstCycle, ValueIteratorValueFunction valueFunction, boolean saveCostBenefits ) 
	{
		// Rather than using RVcorrect (the matrix containing the usable resource values (i.e., >= -budget, <= maximum-budget),
		// we can multiply all values with 1/(mdp.VALUE_STEP * mdp.BUDGET_STEP), which ensures that, until we retrieve values
		// from the ValueFunction, all values are integers. After we have retrieved values from the value function, we still
		// have to use DecimalNumber if we want to avoid floating point issues. See OPTIMIZED_LOSSY for an implementation 
		// that does not switch back to DecimalNumbers (which has superior speed but is not precise when dealing with small values).

		// Initialize some constants, vectors and matrices
		DecimalNumberMatrix RV						= DecimalNumberMatrix.toColumnVector(mdp.POSSIBLE_VALUES.clone());
		DecimalNumber scalingFactor					= DecimalNumber.ONE.divide(mdp.VALUE_STEP).multiply(DecimalNumber.ONE.divide(mdp.BUDGET_STEP));
		double inverseScalingFactor					= DecimalNumber.ONE.divide(scalingFactor).doubleValue();

		double budgetScaled 						= dn.budget.multiply(scalingFactor).doubleValue();//dn.budget.doubleValue() * scalingFactor.doubleValue();
		double maximumBudgetScaled 					= mdp.BUDGET_MAXIMUM.multiply(scalingFactor).doubleValue();
		double discountRate							= mdp.DISCOUNT_RATE.doubleValue();
		RV.scalarMultiplication(scalingFactor);
		double[][] RVscaled 						= RV.toDoubleMatrix();


		for (int r = 0; r < RVscaled.length; r++)
			if ( RVscaled[r][0] + budgetScaled < 0 )
				RVscaled[r][0] = -budgetScaled;
			else if (RVscaled[r][0] + budgetScaled > maximumBudgetScaled)
				RVscaled[r][0] = maximumBudgetScaled - budgetScaled;		
		double[][]	EE								= RV.toDoubleMatrix();
		double[][]  U								= DecimalNumberMatrix.DOUBLE_unityMatrix(RVscaled.length, 1);
		double[][]  UT								= DecimalNumberMatrix.DOUBLE_unityMatrix(1, RVscaled.length);

		// Compute filter F
		double[][] F = new double[RVscaled.length][1];
		for (int r = 0; r < RVscaled.length; r++)
			if (RVscaled[r][0] + budgetScaled > 0)
				F[r][0]=1;
		F = DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(F, UT);

		// Compute IM (IMmediate outcome) and UEET ( (Unity vector) * transposed(Extrinsic Event vector)
		double[][]  IM								= DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(RVscaled, UT);
		double[][]  UEET							= DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(U, DecimalNumberMatrix.DOUBLE_transpose(EE));
		UEET 										= DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(UEET, F);
		IM											= DecimalNumberMatrix.DOUBLE_matrixAddition(IM, UEET);

		// Winsorize all IM values that result in a scaled budget lower than 0 or higher than maximumBudgetScaled
		for (int r = 0; r < IM.length; r++)
			for (int c= 0; c < IM.length; c++) 
				if (IM[r][c]+budgetScaled < 0)
					IM[r][c] = -budgetScaled;
				else if (IM[r][c] + budgetScaled > maximumBudgetScaled) 
					IM[r][c] = maximumBudgetScaled - budgetScaled;
				
		// AFTER THIS POINT THE CODE WILL BE LOSSY
		// If this is the first cycle, we will first compute EFO (that is, EFVO), and then later on (after we have 
		// computed PEV, the Probability of Expected Values) add EFO to IM (which needs to be rescaled). Rather
		// than doing this in two loops, we can do this immediately in the first loop where we calculate 
		// Since we are going to iterate over all values in IM anyway, we might as well store the sum of
		// the scaled IM and EFVO when we compute EFVO, which saves us an additional loop. Hence, here, VALUE_OUTCOMES)
		// (for the first cycle) refers to EFVO+IM.
		double[][] VALUE_OUTCOMES = null;
		
		// Create a reference to EFFO and EFVO
		double[][] EFFO = null;
	
		// Create a reference to FUTURE_OUTCOMES, which stores all the future (fitness or value) outcomes (only used when saveCostBenefits)
		double[][] FUTURE_OUTCOMES = null;
		
		if (!firstCycle) {
			if (saveCostBenefits) FUTURE_OUTCOMES = new double[IM.length][IM.length]; 
			VALUE_OUTCOMES = new double[IM.length][IM.length];
			for (int r = 0; r < IM.length; r++)
				for (int c= 0; c < IM.length; c++) 
					if (IM[r][c] + budgetScaled <= 0 ) {
						VALUE_OUTCOMES[r][c]=IM[r][c] * inverseScalingFactor;
						if (saveCostBenefits) FUTURE_OUTCOMES[r][c] = 0;
					}
					else {
						double EFVO = valueFunction.getExpectedFutureValueOutcome((IM[r][c] + budgetScaled));
						double discountedEFVO = EFVO * discountRate;
						VALUE_OUTCOMES[r][c] = IM[r][c] * inverseScalingFactor + discountedEFVO;
						if (saveCostBenefits) FUTURE_OUTCOMES[r][c] = discountedEFVO;
					}
		}

		// If we are calculating FITNESS outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the expected future fitness outcomes (EFFO)
		else if (firstCycle) {
			EFFO = new double[IM.length][IM.length];
			for (int r = 0; r < IM.length; r++)
				for (int c= 0; c < IM.length; c++) 
					if (IM[r][c] + budgetScaled <= 0 )
						EFFO[r][c]=0;
					else 
						EFFO[r][c] = valueFunction.getTerminalFitnessOutcome(IM[r][c] + budgetScaled);
			if (saveCostBenefits) FUTURE_OUTCOMES = EFFO; 
		}

		// Now that we have the immediate rewards IM, and the future outcomes EFO, we need to compute PEV, the probability distributions over resource values and extrinsic event values
		// First, determine the complement of the interruption rate (i.e., 1- interruptionRate).
		// Note that complementOfInterruptionRate (and all derivatives thereof) have the same range limitation as interruptionRate
		DecimalNumber complementOfInterruptionRate = environment.interruptionRate.complementOfOne().setImmutable(true);
	
		// Next, If the MDP specifies a compound interruption rate, the probability that a resource has not while n cues have been sampled is
		// 		probabilityResourceAvailable = (1 - interruption rate) ^ n, or complementOfInterruptionRate^n. 
		// If the MDP does not specify a compound interruption rate, the probability that a resource is available is
		// 		probabilityResourceAvailable = (1 - interruption rate), or complementOfInterruptionRate. 
		// The probability that a resource has disappeared during sampling is (1-probabilityResourceDisappeared).
		DecimalNumber probabilityResourceAvailable;
		if (mdp.COMPOUND_INTERRUPTION_RATE) 
			probabilityResourceAvailable = complementOfInterruptionRate.pow(dn.cuesSampled, false);
		else
			probabilityResourceAvailable = complementOfInterruptionRate;
		DecimalNumber probabilityResourceDisappeared = probabilityResourceAvailable.complementOfOne();

		// Calculate PRVi, a column vector containing the probabilities (adjusted for the interruption rate) of the resource values
		double probabilityResourceAvailableDouble = probabilityResourceAvailable.doubleValue();
		double[][] PRVi = DecimalNumberMatrix.toColumnVector(	dn.probabilityDistribution.posteriorProbabilityOfResourceValues() ).toDoubleMatrix();
		for (int r = 0; r < PRVi.length; r++)
			PRVi[r][0] *= probabilityResourceAvailableDouble;
		
		// add the probabilityResourceDisappeared to the probability that the resource value has a value of 0
		int indexPositionOfZero = 0;
		for (int r = 0; r < RVscaled.length; r++)
			if (RVscaled[r][0] == 0) {
				indexPositionOfZero = r;
				break;
			}
		PRVi[indexPositionOfZero][0] += probabilityResourceDisappeared.doubleValue();
		
		// Get PEE, the column vector containing the probabilities of each extrinsic event
		double[][] PEE = DecimalNumberMatrix.toColumnVector(	environment.getMatrix().getColumn("p(Extrinsic event value = value)")).toDoubleMatrix();

		// Almost there: compute PEV:
		double[][] PEV = DecimalNumberMatrix.DOUBLE_matrixMultiplicationIKJ(PRVi, DecimalNumberMatrix.DOUBLE_transpose(PEE));

		// Now that we have the IM and PEV matrices, we can compute the probability and outcomes of costs and benefits, if required
		if (saveCostBenefits) {
			// First, initialize the fields in the decision node 
			//(since this part of the script is not often used, but present in every decision node, I didn't want JAVA to have to store
			// initialized fields in all nodes every time)
			dn.acceptingExpectedImmediateBenefits 	= new DecimalNumber(DecimalNumber.NULL);
			dn.acceptingExpectedFutureBenefits 		= new DecimalNumber(DecimalNumber.NULL);
			dn.acceptingProbabilityBenefits 		= new DecimalNumber(0, 0, 1, false);
			dn.acceptingExpectedImmediateCosts		= new DecimalNumber(DecimalNumber.NULL);
			dn.acceptingExpectedFutureCosts			= new DecimalNumber(DecimalNumber.NULL);
			dn.acceptingProbabilityCosts			= new DecimalNumber(0, 0, 1, false);
			dn.acceptingExpectedImmediateNeutral	= new DecimalNumber(DecimalNumber.NULL);
			dn.acceptingExpectedFutureNeutral		= new DecimalNumber(DecimalNumber.NULL);
			dn.acceptingProbabilityNeutral			= new DecimalNumber(0, 0, 1, false);
			
			
			// Next, Create three filter:
			// filterPositive:		a matrix of the same dimensions as probabilityOutcomes that has a 1 if the immediateOutcome is larger than 0
			//																			    and	has a 0 otherwise
			// filterNegative:		a matrix of the same dimensions as probabilityOutcomes that has a 1 if the immediateOutcome is smaller than 0
			//																			    and	has a 0 otherwise
			// filterNeutral		a matrix of the same dimensions as probabilityOutcomes that has a 1 if the immediateOutcome is equal to 0
			//																			    and	has a 0 otherwise
			double[][] filterPositive	=  new double[IM.length][IM.length];
			double[][] filterNegative 	=  new double[IM.length][IM.length];
			double[][] filterNeutral 	=  new double[IM.length][IM.length];
			for (int r = 0; r < filterPositive.length; r++)
				for (int c= 0; c < filterPositive.length; c++) 
					if (IM[r][c] > 0 ) {
						filterPositive[r][c]= 1;
						filterNegative[r][c]= 0;
						filterNeutral[r][c] = 0;} 
					else if  (IM[r][c] < 0) {
						filterPositive[r][c]= 0;
						filterNegative[r][c]= 1;
						filterNeutral[r][c] = 0;} 
					else {
						filterPositive[r][c]= 0;
						filterNegative[r][c]= 0;
						filterNeutral[r][c] = 1;
					}
			
			// Now that we have the two filter, we can compute probabilityPositive, probabilityNeutral and probabilityNegative, all of which 
			// are copies of PEV with the filter applied (e.g., probabilityPositive removes all probabilities of negative outcomes)
			double[][] probabilityPositive = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEV, filterPositive);
			double[][] probabilityNegative = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEV, filterNegative);
			double[][] probabilityNeutral = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEV, filterNeutral);
			
			// The probability of a cost is now the sum over all probabilityNegative, and the probability of a benefit is the sum of all probabilityPositive
			double sumPositiveProbability = 0;
			double sumNegativeProbability = 0;
			double sumNeutralProbability = 0;
			for (int r = 0; r < probabilityPositive.length; r++)
				for (int c= 0; c < probabilityPositive.length; c++) {
					sumPositiveProbability += probabilityPositive[r][c];
					sumNegativeProbability += probabilityNegative[r][c];
					sumNeutralProbability += probabilityNeutral[r][c];
				}
			dn.acceptingProbabilityBenefits.set(sumPositiveProbability);
			dn.acceptingProbabilityCosts.set(sumNegativeProbability);
			dn.acceptingProbabilityNeutral.set(sumNeutralProbability);
			
			// For the next act we compute the values of the costs and benefits, conditional on the agent receiving a cost of benefit 
			// (e.g., the expected cost given that there is a cost). Hence, we need to normalize the two probability distributions to sum to 1
			// While we are doing that, we can use the loop to already keep track of the sum of positive/negative immediate/future outcomes by weighting
			// the [r][c] entry at IM and FUTURE_OUTCOMES with the normalized probability, and add these weighted values to a running total
			double IMpositive = 0;
			double IMnegative = 0;
			double IMneutral  = 0;
			double FUpositive = 0;
			double FUnegative = 0;
			double FUneutral  = 0;
			
			if (sumPositiveProbability>0) {
			for (int r = 0; r < probabilityPositive.length; r++)
				for (int c= 0; c < probabilityPositive.length; c++) {
					// normalize probabilities
					probabilityPositive[r][c] = probabilityPositive[r][c]/sumPositiveProbability;
					
					// weight and add immediate and future costs/benefits
					IMpositive += IM[r][c]*probabilityPositive[r][c];
					FUpositive += FUTURE_OUTCOMES[r][c]*probabilityPositive[r][c];
				}
			} else {
				IMpositive = 0;
				FUpositive = 0;
			}
			
			if (sumNegativeProbability>0) {
			for (int r = 0; r < probabilityNegative.length; r++)
				for (int c= 0; c < probabilityNegative.length; c++) {
					// normalize probabilities
					probabilityNegative[r][c] = probabilityNegative[r][c]/sumNegativeProbability;
					
					// weight and add immediate and future costs/benefits
					IMnegative += IM[r][c]*probabilityNegative[r][c];
					FUnegative += FUTURE_OUTCOMES[r][c]*probabilityNegative[r][c];
				}
			} else {
				IMnegative=0;
				FUnegative=0;
			}
			
			if (sumNeutralProbability>0) {
				for (int r = 0; r < probabilityNeutral.length; r++)
					for (int c= 0; c < probabilityNeutral.length; c++) {
						// normalize probabilities
						probabilityNeutral[r][c] = probabilityNeutral[r][c]/sumNeutralProbability;
						
						// weight and add immediate and future costs/benefits
						IMneutral+= IM[r][c]*probabilityNeutral[r][c];
						FUneutral+= FUTURE_OUTCOMES[r][c]*probabilityNeutral[r][c];
					}
				} else {
					IMnegative=0;
					FUnegative=0;
				}
			
			// Undo the scaling and set the corresponding fields in the DecisionNode
			dn.acceptingExpectedImmediateBenefits.set(IMpositive * inverseScalingFactor);
			dn.acceptingExpectedImmediateCosts.set(IMnegative * inverseScalingFactor);
			dn.acceptingExpectedImmediateNeutral.set(IMneutral* inverseScalingFactor);
			dn.acceptingExpectedFutureBenefits.set(FUpositive);
			dn.acceptingExpectedFutureCosts.set(FUnegative);
			dn.acceptingExpectedFutureNeutral.set(FUneutral);
			
			// Make the fields immutable
			dn.acceptingExpectedImmediateBenefits.setImmutable(true);
			dn.acceptingExpectedFutureBenefits.setImmutable(true);
			dn.acceptingProbabilityBenefits.setImmutable(true);
			dn.acceptingExpectedImmediateCosts.setImmutable(true);
			dn.acceptingExpectedFutureCosts.setImmutable(true);
			dn.acceptingProbabilityCosts.setImmutable(true);
			dn.acceptingExpectedImmediateNeutral.setImmutable(true);
			dn.acceptingExpectedFutureNeutral.setImmutable(true);
			dn.acceptingProbabilityNeutral.setImmutable(true);
		}
		
		// Finally, compute the expected VALUE outcome of eating, defined as 			sum ( PEV .* ( IM + EFO) )
		if (! firstCycle) {
			double[][] expectedOutcome 				= DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEV, VALUE_OUTCOMES);
			double sum = 0;
			for (int r = 0; r < expectedOutcome.length; r++)
				for (int c = 0; c < expectedOutcome.length; c++ )
					sum += expectedOutcome[r][c];
			return new DecimalNumber(sum).setImmutable(true);
		}
			

		// Or, alternatively, if we are interested in terminal FITNESS outcomes:		sum ( PEV .* EFO )
		else {
			double[][] expectedOutcome 				= DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEV, EFFO);
			double sum = 0;
			for (int r = 0; r < expectedOutcome.length; r++)
				for (int c = 0; c < expectedOutcome.length; c++ )
					sum += expectedOutcome[r][c];
			return new DecimalNumber(sum).setImmutable(true);
		}
			

	}


	/** <pre>
	 * Determine the expected outcome of discarding for a specified DecisionNode. 
	 * Discarding is very similar to eating, with the exception that we do not have to think about
	 * the resource as having a value. This function is hence a simplified version of the 
	 * expectedOutcomeEating() function. Rather than describing everything twice, I will 
	 * point to the javadoc above expectedOutcomeEating() as an explanation, and present
	 * below only the modified version of the equations. 
	 * 
	 * =========================================== VALUE OUTCOME
	 * If we are computing expected VALUES
	 * the expected outcome of discarding is equal to
	 * 
	 *  1. the immediate value outcome of the extrinsic event, plus
	 *  2. the ( discounted) expected value outcome of all future encounters, given the resulting budget from the immediate value outcome
	 *
	 * More formally:
	 * expected value outcome of discarding := 
	 * 		sum ee:Extrinsic event values { p(ee) * ( ee + discount rate * ValueFunction(current budget + ee) ) }
	 * 
	 * OR
	 * 
	 * expected value outcome = sum ( [ EE + (discount rate * VFv(budget + EE) ) ] .* PEE )
	 * 						  = sum ( PEE .* [EE + EFO(EE+b)]), 	where EFO = (discount rate * VFv(budget + EE)
	 *   
	 * ========================================== TERMINAL FITNESS OUTCOME
	 * If we are interested in calculating fitnesses, the expected fitness outcome is:
	 * 
	 * expected fitness outcome = sum ( PEE .* EFO(EE+b) ), 		where EFO = VFf(budget + EE)
	 * 
	 * 
	 * 
	 *
	 * @param dn
	 * @return
	 * @throws InvalidProportionException 
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	private DecimalNumber calculateExpectedOutcomeDiscarding(DecisionNode dn, boolean firstCycle, ValueIteratorValueFunction valueFunction) throws InvalidProportionException, IllegalRangeException, IllegalScaleException
	{
		// First, set up some of the (unity) vectors that we will be using
		DecimalNumberMatrix EE						= DecimalNumberMatrix.toColumnVector(mdp.POSSIBLE_VALUES.clone());
		DecimalNumberMatrix	PEE						= DecimalNumberMatrix.toColumnVector(environment.getMatrix().getColumn("p(Extrinsic event value = value)"));
	
		// Since at no time the budget of the agent can be higher than the maximum budget or lower than 0, no immediate rewards can push the agent lower than those values
		EE.apply(new TransformationFunctionDecimalNumber() {
			@Override
			public DecimalNumber function(DecimalNumber argument) {
				if (argument.add(dn.budget, false).compareTo(0)==-1)
					return dn.budget.negate(false);
				else if (argument.add(dn.budget, false).compareTo(mdp.BUDGET_MAXIMUM)==1)
					return mdp.BUDGET_MAXIMUM.subtract(dn.budget);
				return argument;
			}
		});
		
		// Compute the EFO
		DecimalNumberMatrix EFO;
		// If we are calculating VALUE outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the discounted expected future value outcomes (DEFVO)
		if (!firstCycle) {
			DecimalNumberMatrix DEFVO				= EE.clone();
			DEFVO.scalarAddition(dn.budget);
			//Next, we do a couple of steps at the same time. We go over DEFVO and set the value to 
			// - 0						if the value at that entry is <= 0
			// lambda*VFv(value)		otherwise
			DEFVO.apply(new TransformationFunctionDecimalNumber() {
				@Override
				public DecimalNumber function(DecimalNumber argument) {	
					if (argument.compareTo(0)<= 0)	
						return new DecimalNumber(0);	
					else		
						return valueFunction.getExpectedFutureValueOutcome(argument).multiply(mdp.DISCOUNT_RATE);
				}});
			EFO = DEFVO;
		}

		// If we are calculating FITNESS outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the expected future fitness outcomes (EFFO)
		else { // i.e., if (firstCycle) 
			DecimalNumberMatrix EFFO				= EE.clone();
			EFFO.scalarAddition(dn.budget);
			//Next, we do a couple of steps at the same time. We go over DFVO and set the value to 
			// - 0						if the value at that entry is <= 0
			// VFf(value)		otherwise
			EFFO.apply(new TransformationFunctionDecimalNumber() {
				@Override
				public DecimalNumber function(DecimalNumber argument) {	
					if (argument.compareTo(0)<= 0)	
						return new DecimalNumber(0);	
					else		
						return valueFunction.getTerminalFitnessOutcome(argument);
				}});
			EFO = EFFO;
		}
		
		DecimalNumber expectedOutcome;
		// If we are calculating expected VALUE outcomes:
		// expected outcome = sum ( PEE .* [EE + EFO(EE+b)]), 	where EFO = (discount rate * VFv(budget + EE)
		if (! firstCycle) 
			expectedOutcome = DecimalNumberMatrix.entrywiseMultiplication(PEE, DecimalNumberMatrix.matrixAddition(EE, EFO)).sum();
		
		// If we are calculating expected FITNESS outcomes:
		// expected outcome = sum ( PEE .* EFO(EE+b) ), 		where EFO = VFf(budget + EE)
		else 
			expectedOutcome = DecimalNumberMatrix.entrywiseMultiplication(PEE, EFO).sum();

		return expectedOutcome.setImmutable(true);

	}


	/** <pre>
	 * Determine the expected outcome of discarding for a specified DecisionNode. 
	 * Discarding is very similar to eating, with the exception that we do not have to think about
	 * the resource as having a value. This function is hence a simplified version of the 
	 * expectedOutcomeEating() function. Rather than describing everything twice, I will 
	 * point to the javadoc above expectedOutcomeEating() as an explanation, and present
	 * below only the modified version of the equations. 
	 * 
	 * =========================================== VALUE OUTCOME
	 * If we are computing expected VALUES
	 * the expected outcome of discarding is equal to
	 * 
	 *  1. the immediate value outcome of the extrinsic event, plus
	 *  2. the ( discounted) expected value outcome of all future encounters, given the resulting budget from the immediate value outcome
	 *
	 * More formally:
	 * expected value outcome of discarding := 
	 * 		sum ee:Extrinsic event values { p(ee) * ( ee + discount rate * ValueFunction(current budget + ee) ) }
	 * 
	 * OR
	 * 
	 * expected value outcome = sum ( [ EE + (discount rate * VFv(budget + EE) ) ] .* PEE )
	 * 						  = sum ( PEE .* [EE + EFO(EE+b)]), 	where EFO = (discount rate * VFv(budget + EE)
	 *   
	 * ========================================== TERMINAL FITNESS OUTCOME
	 * If we are interested in calculating fitnesses, the expected fitness outcome is:
	 * 
	 * expected fitness outcome = sum ( PEE .* EFO(EE+b) ), 		where EFO = VFf(budget + EE)
	 * 
	 * 
	 *  If  saveCostBenefits is true, saved the immediate/future benefits/costs for all decision nodes.
	 * 
	 *
	 * @param dn
	 * @return
	 * @throws InvalidProportionException 
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	private DecimalNumber OPTIMIZED_LOSSY_calculateExpectedOutcomeDiscarding(DecisionNode dn, boolean firstCycle, ValueIteratorValueFunction valueFunction, boolean saveCostBenefits) throws InvalidProportionException, IllegalRangeException, IllegalScaleException
	{
		// Initialize some constants, vectors and matrices
		DecimalNumber scalingFactor					= DecimalNumber.ONE.divide(mdp.VALUE_STEP).multiply(DecimalNumber.ONE.divide(mdp.BUDGET_STEP));
		double inverseScalingFactor					= DecimalNumber.ONE.divide(scalingFactor).doubleValue();

		double budgetScaled 						= dn.budget.multiply(scalingFactor).doubleValue();
		double maximumBudgetScaled 					= mdp.BUDGET_MAXIMUM.multiply(scalingFactor).doubleValue();
		double discountRate							= mdp.DISCOUNT_RATE.doubleValue();
		
		DecimalNumberMatrix EE_DMMATRIX				= DecimalNumberMatrix.toColumnVector(mdp.POSSIBLE_VALUES.clone());
		EE_DMMATRIX.scalarMultiplication(scalingFactor);
		double[][] EEscaled 						= EE_DMMATRIX.toDoubleMatrix();
		
		DecimalNumberMatrix	PEE						= DecimalNumberMatrix.toColumnVector(environment.getMatrix().getColumn("p(Extrinsic event value = value)"));
		double[][] PEE_DOUBLE						= PEE.toDoubleMatrix();
		
		// Since at no time the budget of the agent can be higher than the maximum budget or lower than 0, no immediate rewards can push the agent lower than those values
		// Note: EE is a [n]*[1] matrix of budgets
		for (int r = 0; r < EEscaled.length; r++)
			if ( EEscaled[r][0] + budgetScaled < 0 )
				EEscaled[r][0] = -budgetScaled;
			else if (EEscaled[r][0] + budgetScaled > maximumBudgetScaled)
				EEscaled[r][0] = maximumBudgetScaled - budgetScaled;	
		
		// Compute the EFO
		double[][] EFO;
		// If we are calculating VALUE outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the discounted expected future value outcomes (DEFVO)
		if (!firstCycle) {
			double[][] DEFVO = new double[EEscaled.length][1];
			
			//Next, we do a couple of steps at the same time. We go over DEFVO and set the value to 
			// - 0						if the value at that entry is <= 0
			// lambda*VFv(value)		otherwise
			for (int r = 0; r< DEFVO.length; r++) {
				double valueOutcome = (EEscaled[r][0] + budgetScaled);
				if (valueOutcome <= 0)
					DEFVO[r][0] = 0;
				else
					DEFVO[r][0]  = valueFunction.getExpectedFutureValueOutcome(valueOutcome) * discountRate;
			}
			EFO = DEFVO;
		}
		
	
		// If we are calculating FITNESS outcomes: compute the EXPECTED FUTURE OUTCOMES (EFO) as the expected future fitness outcomes (EFFO)
		//Next, we do a couple of steps at the same time. We go over DFVO and set the value to 
		// - 0						if the value at that entry is <= 0
		// VFf(value)		otherwise
		else {//if (firstCycle) {
			double[][] EFFO = new double[EEscaled.length][1];
			for (int r = 0; r< EFFO.length; r++) {
				double fitnessOutcome = EEscaled[r][0] + budgetScaled;
				if (fitnessOutcome <= 0)
					EFFO[r][0] = 0;
				else
					EFFO[r][0] = valueFunction.getTerminalFitnessOutcome(fitnessOutcome);
			}
			EFO = EFFO;
		}
		
		// Now that we have the EEscaled and PEE_DOUBLE matrices, we can compute the probability and outcomes of costs and benefits, if required
		if (saveCostBenefits) {
			// First, initialize the fields in the decision node 
			//(since this part of the script is not often used, but present in every decision node, I didn't want JAVA to have to store
			// initialized fields in all nodes every time)
			dn.discardingExpectedImmediateBenefits 	= new DecimalNumber(DecimalNumber.NULL);
			dn.discardingExpectedFutureBenefits		= new DecimalNumber(DecimalNumber.NULL);
			dn.discardingProbabilityBenefits 		= new DecimalNumber(0, 0, 1, false);
			dn.discardingExpectedImmediateCosts		= new DecimalNumber(DecimalNumber.NULL);
			dn.discardingExpectedFutureCosts		= new DecimalNumber(DecimalNumber.NULL);
			dn.discardingProbabilityCosts			= new DecimalNumber(0, 0, 1, false);
			dn.discardingExpectedImmediateNeutral	= new DecimalNumber(DecimalNumber.NULL);
			dn.discardingExpectedFutureNeutral		= new DecimalNumber(DecimalNumber.NULL);
			dn.discardingProbabilityNeutral			= new DecimalNumber(0, 0, 1, false);
			
			// Next, Create two filter:
			// filterPositive:		a matrix of the same dimensions as probabilityOutcomes that has a 1 if the EE is larger than 0
			//																			    and	has a 0 otherwise
			// filterNegative:		a matrix of the same dimensions as probabilityOutcomes that has a 1 if the EE is smaller than 0
			//																			    and	has a 0 otherwise
			// filterNeutral:		a matrix of the same dimensions as probabilityOutcomes that has a 1 if the EE is equal to 0
			//																			    and	has a 0 otherwise
			double[][] filterPositive	=  new double[EEscaled.length][1];
			double[][] filterNegative 	=  new double[EEscaled.length][1];
			double[][] filterNeutral 	=  new double[EEscaled.length][1];
			
			for (int r = 0; r < filterPositive.length; r++)
				if (EEscaled[r][0] > 0 ) {
					filterPositive[r][0]= 1;
					filterNegative[r][0]= 0;
					filterNeutral[r][0]= 0;} 
				else if  (EEscaled[r][0] < 0) {
					filterPositive[r][0]= 0;
					filterNegative[r][0]= 1;
					filterNeutral[r][0]= 0;} 
				else {
					filterPositive[r][0]= 0;
					filterNegative[r][0]= 0;
					filterNeutral[r][0]= 1;
				}
			
			// Now that we have the two filter, we can compute probabilityPositive, probabilityNeutral and probabilityNegative, all of which 
			// are copies of PEE_DOUBLE with the filter applied (i.e., probabilityPositive removes all probabilities of negative outcomes and vice versa)
			double[][] probabilityPositive = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEE_DOUBLE, filterPositive);
			double[][] probabilityNegative = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEE_DOUBLE, filterNegative);
			double[][] probabilityNeutral = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEE_DOUBLE, filterNeutral);
			
			// The probability of a cost is now the sum over all probabilityNegative, and the probability of a benefit is the sum of all probabilityPositive
			double sumPositiveProbability = 0;
			double sumNegativeProbability = 0;
			double sumNeutralProbability = 0;
			
			for (int r = 0; r < probabilityPositive.length; r++) {
				sumPositiveProbability += probabilityPositive[r][0];
				sumNegativeProbability += probabilityNegative[r][0];
				sumNeutralProbability += probabilityNeutral[r][0];
			}
			dn.discardingProbabilityBenefits.set(sumPositiveProbability);
			dn.discardingProbabilityCosts.set(sumNegativeProbability);
			dn.discardingProbabilityNeutral.set(sumNeutralProbability);
			
			// For the next act we compute the values of the costs and benefits, conditional on the agent receiving a cost of benefit 
			// (e.g., the expected cost given that there is a cost). Hence, we need to normalize the two probability distributions to sum to 1
			// While we are doing that, we can use the loop to already keep track of the sum of positive/negative immediate/future outcomes by weighting
			// the [r][0] entry at EEscaled and EFO with the normalized probability, and add these weighted values to a running total
			double IMpositive = 0;
			double IMnegative = 0;
			double IMneutral  = 0;
			double FUpositive = 0;
			double FUnegative = 0;
			double FUneutral  = 0;
			
			if (sumPositiveProbability>0)
				for (int r = 0; r < probabilityPositive.length; r++) {
					// normalize probabilities
					probabilityPositive[r][0] = probabilityPositive[r][0]/sumPositiveProbability;

					// weight and add immediate and future costs/benefits
					IMpositive += EEscaled[r][0]*probabilityPositive[r][0];
					FUpositive += EFO[r][0]*probabilityPositive[r][0];
				} else {
					IMpositive = 0;
					FUpositive = 0;
				}

			if (sumNegativeProbability>0) {
				for (int r = 0; r < probabilityNegative.length; r++) {
					// normalize probabilities
					probabilityNegative[r][0] = probabilityNegative[r][0]/sumNegativeProbability;

					// weight and add immediate and future costs/benefits
					IMnegative += EEscaled[r][0]*probabilityNegative[r][0];
					FUnegative += EFO[r][0]*probabilityNegative[r][0];
				} 
			} else {
				IMnegative = 0;
				FUnegative = 0;
			}

			if (sumNeutralProbability>0) {
				for (int r = 0; r < probabilityNeutral.length; r++) {
					// normalize probabilities
					probabilityNeutral[r][0] = probabilityNeutral[r][0]/sumNeutralProbability;

					// weight and add immediate and future costs/benefits
					IMneutral += EEscaled[r][0]*probabilityNeutral[r][0];
					FUneutral += EFO[r][0]*probabilityNeutral[r][0];
				} 
			} else {
				IMneutral= 0;
				FUneutral= 0;
			}

			
			// Undo the scaling and set the corresponding fields in the DecisionNode
			dn.discardingExpectedImmediateBenefits.set(IMpositive * inverseScalingFactor);
			dn.discardingExpectedImmediateCosts.set(IMnegative * inverseScalingFactor);
			dn.discardingExpectedImmediateNeutral.set(IMneutral * inverseScalingFactor);
			dn.discardingExpectedFutureBenefits.set(FUpositive);
			dn.discardingExpectedFutureCosts.set(FUnegative);
			dn.discardingExpectedFutureNeutral.set(FUneutral);

			// Make the fields immutable
			dn.discardingExpectedImmediateBenefits.setImmutable(true);
			dn.discardingExpectedFutureBenefits.setImmutable(true);
			dn.discardingProbabilityBenefits.setImmutable(true);
			dn.discardingExpectedImmediateCosts.setImmutable(true);
			dn.discardingExpectedFutureCosts.setImmutable(true);
			dn.discardingProbabilityCosts.setImmutable(true);
			dn.discardingExpectedImmediateNeutral.setImmutable(true);
			dn.discardingExpectedFutureNeutral.setImmutable(true);
			dn.discardingProbabilityNeutral.setImmutable(true);

		}
		
		// If we are calculating expected VALUE outcomes:
		// expected outcome = sum ( PEE .* [EE + EFO(EE+b)]), 	where EFO = (discount rate * VFv(budget + EE)
		if (! firstCycle) {
			// Rescale EE
			double[][] EE = new double[EEscaled.length][1];
			for (int r =0; r< EEscaled.length; r++)
				EE[r][0] = EEscaled[r][0]*inverseScalingFactor;
			
			double[][] expectedOutcome = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEE_DOUBLE, DecimalNumberMatrix.DOUBLE_matrixAddition(EE, EFO));

			double sum = 0;
			for (int r = 0; r < expectedOutcome.length; r++)
					sum += expectedOutcome[r][0];
			return new DecimalNumber(sum).setImmutable(true);
		}
		
		// If we are calculating expected FITNESS outcomes:
		// expected outcome = sum ( PEE .* EFO(EE+b) ), 		where EFO = VFf(budget + EE)
		else {
			double [][] expectedOutcome = DecimalNumberMatrix.DOUBLE_entrywiseMultiplication(PEE_DOUBLE, EFO);
			double sum = 0;
			for (int r = 0; r < expectedOutcome.length; r++)
					sum += expectedOutcome[r][0];
			return new DecimalNumber(sum).setImmutable(true);
		}
	
		

	}


	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////// 	Forward pruning pass	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	/**  <pre>
	 * Prune the decision tree to remove all nodes that will never be accessed. The resulting tree starts at the root node and only expands
	 * non-leaf nodes (i.e., only nodes which best action is to sample are expanded). While expanding, the proportion
	 * of each node is updated - the proportion originally created in the decision tree's forward pass are no longer correct:
	 * if a node has at least one parent that samples and at least one parent that does not, the node's original
	 * proportion has to be changed to make sure it no longer 'receives' the proportion from the non-sampling parent(s).
	 *
	 * <p> The forward pruning pass is done  is created with the following procedure:
	 * 0. Initialize all the fields that we want to save (see declaration in DecisionTree class)
	 * 0. Make sure that the tree already went through a forward and backward pass.
	 * 0. If the root node is dead on start, we can stop immediately - no pruning required
	 * 0. Empty the edge list of the tree
	 * 0. proportionFinished <- 0 (used for checking if everything went correct)
	 * 
	 * 1. Set the proportion of all agents that visit the root node to 1; set the same proportion in all other nodes to 0.
	 * 2. Initialize newNodes, an empty arraylist of decision nodes that represents the final pruned tree
	 * 3. Initialize the frontier, an empty arraylist of decision nodes
	 * 4. Add the root of the decision tree to the frontier
	 * 
	 * (Side note: we will iterate over frontiers. Each frontier contains nodes with the same amount of cues sampled - 
	 *  i.e., the first frontier contains all nodes with 0 cues sampled (only the root); the second frontier contains
	 *  all children of the nodes in the previous frontier, and hence contains of the nodes with 1 cue sampled; etc. 
	 *  As such, the frontier number is conceptually equal to the (number of cues sampled - 1) of the nodes in that frontier.)
	 *  
	 * 4b. frontiersExplored <- 0
	 * 5. While the frontier contains elements:
	 * 		a. initialize newFrontier, an empty array list
	 * 		b. for each node in frontier:
	 * 			i.   - add the node to newNodes
	 * 			ii.  - determine how much of the nodes proportion is passed on to future generations (propFuture).
	 * 					If the node's list of best actions does not contain sampling, this is 0.
	 * 					If the node's list of best actions contains sampling, this is 1/[number of actions in the best action list]
	 * 			iii. - A node is a leaf node if (propFuture == 0)
	 * 			iv.  - proportionFinished <- proportionFinished + (1-propFuture) * node proportion
	 * 			v.   - if the node is not a leaf node:
	 * 				   for each child of the node:  
	 * 					- add [the nodes proportion * the weight on the edge between the parent and the current node * propFuture] to the child's proportion
	 * 					- add the child to newFrontier (if its not already in there)
	 * 					- add the connection from the node to the child to the edgeList
	 *          vi. - Save all proportions that we want to save: for each node in the frontier:
	 *		            - cuesSampled[frontiersExplored]          <- cuesSampled[frontiersExplored] + [number of non-sampling actions]/[number of best actions] * proportion			
	 *				  	- proportionEating[frontiersExplored]     <- proportionEating[frontiersExplored] + 1/[number of best actions] * proportion		if best action include eat
	 *				  	- proportionDiscarding[frontiersExplored] <- proportionDiscarding[frontiersExplored] + 1/[number of best actions] * proportion	if best actions include discard
	 * 		c. frontier <-  newFrontier
	 * 		d. frontiersExplored++
	 * 6. nodes <- newNodes
	 * 
	 * 7. Hereafter, we have a fully specified decision tree (i.e., a map to show which actions to take
	 * 		for each situation). However, it is nice to be sure that everything went OK. So, in this step there are some validity checks (see code). 
	 * 
	 * 8. Save all the things that we want to save
	 *    - expectedCuesSampled  		<- dotproduct([0, 1, 2, ..., maximum number of cues], cuesSampled)
	 *	  - totalProportionEating   	<- proportionEating.sum()
	 *	  - totalProportionDiscarding	<- proportionDiscarding.sum()
	 *    - expectedOutcomes 			<- root.expectedOutcomes() 
	 *    
	 * @return
	 * @throws REngineException 
	 * @throws REXPMismatchException 
	 * @throws InvalidFitnessInputException 
	 * @throws NumberFormatException 
	 * @throws InvalidProbabilityException 
	 */
	public void forwardPruningPass () throws InvalidProportionException, ImpossibleStateException, NumberFormatException, InvalidFitnessInputException, REXPMismatchException, REngineException, InvalidProbabilityException
	{
		// 0. Make sure the tree already did a forward and backward pass, and has not already done a policy pass
		if (!this.ranForward)
			throw new IllegalStateException("Exception in DecisionTree.forwardPruningPass(): trying to do a forward pruning pass on a decision tree that has not yet ran a forwards pass.");
		
		if (!this.ranBackward)
			throw new IllegalStateException("Exception in DecisionTree.forwardPruningPass(): trying to do a forward pruning pass on a decision tree that has not yet ran a backwards pass.");

		if (this.ranForwardPruning)
			throw new IllegalStateException("Exception in DecisionTree.forwardPruningPass(): trying to do a forward pruning pass on a decision tree that already has gone through a forward pruning pass.");
		
		// Initialize the arrays that we want to save (see javadoc of this function or see above at the class declaration)
		cuesSampled 				= DecimalNumberArray.rep(new DecimalNumber(0,0,1,false), mdp.MAXIMUM_CUES+1);
		proportionEating 			= DecimalNumberArray.rep(new DecimalNumber(0,0,1,false), mdp.MAXIMUM_CUES+1);
		proportionDiscarding		= DecimalNumberArray.rep(new DecimalNumber(0,0,1,false), mdp.MAXIMUM_CUES+1);
		dominanceEating             = DecimalNumberArray.rep(new DecimalNumber(0), mdp.NUMBER_OF_CUE_LABELS);
		dominanceDiscarding         = DecimalNumberArray.rep(new DecimalNumber(0), mdp.NUMBER_OF_CUE_LABELS);
		
		// 0.  A tree with a DEAD_ON_START root can be ignored - it is already pruned. We just have to set the expectedNumberCuesSampled to 0
		if (root.bestAction.contains(Action.DEAD_ON_START)) {
			expectedOutcomes 			= root.expectedOutcomes().clone().setImmutable(true);
			expectedCuesSampled			= new DecimalNumber(0).setImmutable(true);
			totalProportionEating 		= new DecimalNumber(0).setImmutable(true);
			totalProportionDiscarding   = new DecimalNumber(0).setImmutable(true);
			ranForwardPruning=true;
			return;
		}
		

		// 0. Empty the edge list
		this.edges.removeAll(this.edges);

		// 7. Initializing proportionFinished
		DecimalNumber proportionFinished = new DecimalNumber(0,0,1,false);
		
		// 1. Set the proportion of the root node to 1, and the proportion of all other nodes to 0.
		for (DecisionNode dn: nodes)
			if (dn == root)
				dn.setProportion(1);
			else
				dn.setProportion(0);
		
		// 2. Initialize newNodes, an empty arraylist of decision nodes that represents the final policy-tree
		ArrayList<DecisionNode> newNodes = new ArrayList<>();

		// 3. Initialize the frontier, an empty arraylist of decision nodes
		ArrayList<DecisionNode> frontier = new ArrayList<>();

		// 4. Add the root of the decision tree to the frontier
		frontier.add(root);

		// 5. While the frontier contains elements:
		int frontiersExplored = 0; // (we do not strictly need this, but it makes the code easier: it is to keep track where in the 3 distributions we have to put new values (see above)
		while (frontier.size()!=0) {
			
			//a. initialize newFrontier, an empty array list
			ArrayList<DecisionNode> newFrontier = new ArrayList<>();

			//b. for each node in the frontier:
			for (DecisionNode dn: frontier){
				//i.   -add the node to the arraylist 'newNodes'
				newNodes.add(dn);

				//ii.  - determine how much of the nodes proportion is passed on to future generations (propFuture).
				//		If the node is a leaf node (i.e., its list of best actions does not contain sampling), this is 0.
				//		If the node is not a leaf node (i.e., its list of best actions contains sampling), this is 1/[number of actions in the best action list]
				//iii. - A node is a leaf node if propFuture == 0
				DecimalNumber propFuture = new DecimalNumber(0);
				if (dn.bestAction.contains(Action.SAMPLE)) {
					propFuture = new DecimalNumber(1).divide(new DecimalNumber (dn.bestAction.size()));
					dn.isLeafNode = false;
				} else
					dn.isLeafNode = true;
				
				// iv.  proportionFinished <- proportionFinished   += (1-propFuture) * node proportion
				proportionFinished.add (propFuture.complementOfOne(false).multiply(dn.proportion));
				
				// v.  - if the node is not a leaf node:
				//		   for each child of the node:  
				//			- add [the nodes proportion * the weight on the edge between the parent and the current node * propFuture] to the child's proportion
				//			- add the child to newFrontier (if its not already in there)
				//			- add the connection from the node to the child to the edgeList
				if (!dn.isLeafNode) 
					for (DecisionEdge de: dn.childEdges) {
						DecisionNode child = de.child;
						child.setProportion(child.proportion.add(dn.proportion.multiply(de.proportion).multiply(propFuture)));

						if (!newFrontier.contains(child)) newFrontier.add(child);
						edges.add(de);
					}
						
				//  vi. - Save all proportions that we want to save:
				//  		cuesSampled[frontiersExplored]          <- cuesSampled[frontiersExplored] + 1/[number of best actions] * proportion
				//	  		proportionEating[frontiersExplored]     <- proportionEating[frontiersExplored] + 1/[number of best actions] * proportion
				//	  		proportionDiscarding[frontiersExplored] <- proportionDiscarding[frontiersExplored] + 1/[number of best actions] * proportion
				//          dominanceEating[ct]                     <- dominanceEating[ct] + dominance(ct)* 1/[number of best actions]*proportion [for all ct:CueLabels]
				//	        dominanceDiscarding[ct]                 <- dominanceDiscarding[ct] + dominance(ct)*1/[number of best actions] *proportion [for all ct:CueLabels]
				int numberOfNonSamplingActions = dn.bestAction.size();
				if (dn.bestAction.contains(Action.SAMPLE)) numberOfNonSamplingActions--;
				cuesSampled.get(frontiersExplored).add(
						new DecimalNumber(numberOfNonSamplingActions).divide(new DecimalNumber (dn.bestAction.size())).multiply(dn.proportion)
						);
				if (dn.bestAction.contains(Action.EAT)) {
					proportionEating.get(frontiersExplored).add(
							new DecimalNumber(1).divide(new DecimalNumber (dn.bestAction.size())).multiply(dn.proportion)

							);
					for (int i = 0; i < mdp.NUMBER_OF_CUE_LABELS; i++)
						dominanceEating.get(i).add(
								new DecimalNumber(1).divide(new DecimalNumber (dn.bestAction.size())).multiply(dn.proportion).multiply(dn.getDominance(mdp.CUE_LABELS.get(i)))
								);
				}

				if (dn.bestAction.contains(Action.DISCARD)) {
					proportionDiscarding.get(frontiersExplored).add(
							new DecimalNumber(1).divide(new DecimalNumber (dn.bestAction.size())).multiply(dn.proportion)
							);
					
					for (int i = 0; i < mdp.NUMBER_OF_CUE_LABELS; i++)
						dominanceDiscarding.get(i).add(
								new DecimalNumber(1).divide(new DecimalNumber (dn.bestAction.size())).multiply(dn.proportion).multiply(dn.getDominance(mdp.CUE_LABELS.get(i)))
								);
				}

				
			}
			//c. replace frontier with newFrontier
			frontier = newFrontier;
			
			
			//d. increment frontiersExplored
			frontiersExplored++;
		}
		
		// 6. Update nodes = newNodes
		this.nodes = newNodes;
		
		// 7. Check whether the results are valid. To be honest, this code could be removed to increase the speed of the algorithm, 
		//	  but it does not matter much in terms of time and I really like to be sure that nothing is going wrong.
		
		// The outgoing edges of non-leaf nodes (i.e., nodes that ONLY sample) should sum to 1
		for (DecisionNode dn: nodes) {
			if (dn.bestAction.contains(Action.SAMPLE)) { 
				DecimalNumber sumOfEdgeProportions = new DecimalNumber(0);
				for (DecisionEdge de: dn.childEdges) sumOfEdgeProportions.add(de.proportion);
				
				if (!sumOfEdgeProportions.equals(1, true))
					throw new Helper.InvalidProportionException("Check failed in DecisionTree.forwardPruningPass: the outgoing edges of a non-leaf node did not sum to 1. The sum was " + sumOfEdgeProportions);
			}
		}
		// Proportion of leaf nodes should sum to 1
		if (!proportionFinished.equals(1, true)) 
			throw new Helper.InvalidProportionException("Check failed in DecisionTree.forwardPruningPass: sum of leaf nodes does not equal 1. The sum was " + proportionFinished);
		
		// The sum of (sum of proportion eating) and the (sum of proportion discarding) should be 1. (edge case: this does not hold if the node is dead-on-start)
		if (root.bestAction.contains(Action.DEAD_ON_START))
			if (!proportionEating.sum().add(proportionDiscarding.sum()).equals(1, true))
				throw new Helper.InvalidProportionException("Check failed in DecisionTree.forwardPruningPass: sum of (sum of proportion eating) and (sum of proportion discarding) does not equal 1. The sum was " +proportionEating.sum().add(proportionDiscarding.sum()));
		
		 /* 8. Save all the things that we want to save
		 *    - expectedCuesSampled  		<- dotproduct([0, 1, 2, ..., maximum number of cues], cuesSampled)
		 *	  - totalProportionEating   	<- proportionEating.sum()
		 *	  - totalProportionDiscarding	<- proportionDiscarding.sum()
		 *    - expectedOutcomes 			<- root.expectedOutcomes() 
		 */
		expectedCuesSampled			= DecimalNumberArray.dotProduct(
										DecimalNumberArray.sequence(0, mdp.MAXIMUM_CUES, 1),
										cuesSampled
									  ).setImmutable(true);
		totalProportionEating 		= proportionEating.sum().setImmutable(true);
		totalProportionDiscarding 	= proportionDiscarding.sum().setImmutable(true);	
		expectedOutcomes 			= root.expectedOutcomes().clone().setImmutable(true);
		
		if (!this.proportionEating.sum().equals(0))
			for (DecimalNumber dom:dominanceEating)
				dom.divide(this.proportionEating.sum()).setImmutable(true);
	
		if (!this.proportionDiscarding.sum().equals(0))
			for (DecimalNumber dom:dominanceDiscarding)
				dom.divide(this.proportionDiscarding.sum()).setImmutable(true);
	
		
		this.ranForwardPruning = true;
	}

	/** <pre>
	 * Returns the decision node with the same cueSet (where cueSet is a list of Integers). If there is no
	 * matching DecisionNode, returns null;
	 * @param cueSet
	 * @return
	 */
	public DecisionNode getNode(Integer[] cueSet)
	{
		for (DecisionNode dn: nodes)
		{
			boolean matching = true;
			for (int i = 0; i < cueSet.length && matching; i++)
				if (dn.cueSet[i] != cueSet[i] )
					matching = false;
			if (matching) return dn;
		}
		return null;
	}

	
	/**
	 * toString() override. Note that dead node's are not printed.
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("0.##############");

		// Create header
		StringBuilder h = new StringBuilder();
		String cuesSampled = "Cues Sampled";
		String cuesSampledSpaced = cuesSampled + Helper.repString(" ", DecisionNode.colLength-cuesSampled.length());
		h.append(cuesSampledSpaced);

		String budget = "Budget";
		String budgetSpaced = budget + Helper.repString(" ", DecisionNode.colLength-budget.length());
		h.append(budgetSpaced);

		String proportion = "Proportion";
		String proportionSpaced = proportion + Helper.repString(" ", DecisionNode.colLength-proportion.length());
		h.append(proportionSpaced);

		String divider = "||" + Helper.repString(" ", DecisionNode.colLength-2);
		h.append(divider);

		for (int s = 0; s < mdp.CUE_LABELS.length(); s++)
		{
			String cueName = mdp.CUE_LABELS.get(s);
			String cueNameSpaced = cueName + Helper.repString(" ", DecisionNode.colLength-cueName.length());
			h.append(cueNameSpaced);
		}

		h.append(divider);

		String bestAction = "Best action";
		String bestActionSpaced = bestAction + Helper.repString(" ", DecisionNode.colLength-bestAction.length());
		h.append(bestActionSpaced);

		String expectedOutcome = "Exp Outcome";
		String expectedOutcomeSpaced = expectedOutcome + Helper.repString(" ", DecisionNode.colLength-expectedOutcome.length());
		h.append(expectedOutcomeSpaced);

		String expectedOutcomeSampling = "EFit Sampling";
		String expectedOutcomeSamplingSpaced = expectedOutcomeSampling + Helper.repString(" ", DecisionNode.colLength-expectedOutcomeSampling.length());
		h.append(expectedOutcomeSamplingSpaced);

		String expectedOutcomeEating = "EFit Eating";
		String expectedOutcomeEatingSpaced = expectedOutcomeEating + Helper.repString(" ", DecisionNode.colLength-expectedOutcomeEating.length());
		h.append(expectedOutcomeEatingSpaced);

		String expectedOutcomeDicarding = "EFit Discarding";
		String expectedOutcomeDicardingSpaced = expectedOutcomeDicarding + Helper.repString(" ", DecisionNode.colLength-expectedOutcomeDicarding.length());
		h.append(expectedOutcomeDicardingSpaced);

		h.append("   "+"||"+"    ");

		for (DecimalNumber v:mdp.CUE_EMISSION_MATRIX(true).getColumn(0))
		{
			String value = "p(V=" +  df.format(v.doubleValue()) + "|cues)";
			String valueSpaced = value + Helper.repString(" ", DecisionNode.colLength-value.length());
			h.append(valueSpaced);
		}

		h.append("   "+"||"+"    ");
		for (String label: mdp.CUE_LABELS)
		{
			String valueSpaced = label + Helper.repString(" ", DecisionNode.colLength-label.length());
			h.append(valueSpaced);
		}

		// Add it all to a single stringBuilder
		// Add title
		sb.append("Decision tree starting with a budget of " + df.format(startingBudget.doubleValue()));
		sb.append("\n" + Helper.repString("=", h.length()));
		sb.append("\n" + "Nodes:");
		sb.append("\n" + h.toString()+"\n");
		sb.append(Helper.repString("-", h.length()));
		for (DecisionNode dn: nodes)
			sb.append("\n"+dn);

		if (printEdges)
		{
			sb.append("\n\nEdges:");
			for (DecisionEdge de: edges)
				sb.append("\n" + de);
		}

		// Add the cue dominances
		sb.append("\nCue dominance when discarding: " );
		for (int i = 0; i < mdp.CUE_LABELS.length(); i++)
			sb.append("\n\t" + mdp.CUE_LABELS.get(i) + ": " +this.dominanceDiscarding.get(i) );
		
		sb.append("\nCue dominance when eating: " );
		for (int i = 0; i < mdp.CUE_LABELS.length(); i++)
			sb.append("\n\t" + mdp.CUE_LABELS.get(i) + ": " +this.dominanceEating.get(i) );
		
		return sb.toString();
	}

	/**
	 * transform the nodes to a data frame compatible with R
	 * @return
	 */
	public String nodeDataFrame(String nodeFrameName)
	{
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("0.#####");
		int columns = 0;
		// Create column names
		StringBuilder cNames = new StringBuilder();
		cNames.append("colnames("+ nodeFrameName +") <- c(");

		cNames.append( "\"Cues_sampled\",");
		columns++;

		cNames.append( "\"Budget\",");
		columns++;

		cNames.append( "\"Proportion\",");
		columns++;

		for (int i = 1; i < mdp.CUE_LABELS.length(); i ++)
		{
			cNames.append( "\"NumberOfCues_" + mdp.CUE_LABELS.get(i) + "\",");
			columns++;
		}

		cNames.append( "\"Best_action\",");
		columns++;

		cNames.append( "\"Expected_outcome\",");
		columns++;

		cNames.append( "\"Expected_outcome_sampling\",");
		columns++;

		cNames.append( "\"Expected_outcome_eating\",");
		columns++;

		cNames.append( "\"Expected_outcome_discarding\",");
		columns++;

		DecimalNumberArray possibleResourceValues = mdp.CUE_EMISSION_MATRIX(true).getColumn("Resource value");
		for (int i = 0; i < possibleResourceValues.length(); i ++)
		{
			cNames.append("\"PrVequals" +  df.format(possibleResourceValues.get(i)) + "GivenCue\"");
			columns++;
			if (i != possibleResourceValues.length()-1) cNames.append(",");
		}

		cNames.append(")");

		// Create data frame
		sb.append(nodeFrameName + "<-as.data.frame(matrix(0,ncol=" + columns + ",nrow=0));");
		sb.append("\n"+cNames.toString() + ";");

		// add nodes
		for (int i = 0; i < nodes.size(); i ++)
			sb.append("\n" + nodeFrameName+"[" + (i+1) + ",]<-" + nodes.get(i).toDataFrameRow() + ";");

		// Some control things
		sb.append("\n" + nodeFrameName + "$Best_action <- as.factor(" + nodeFrameName + "$Best_action)");
		return sb.toString();

	}

	/**
	 * transform the edges to a data frame compatible with R
	 * @return
	 */
	public String edgeDataFrame(String edgeFrameName)
	{
		StringBuilder sb = new StringBuilder();
		int columns = 0;

		// Create column names
		StringBuilder cNames = new StringBuilder();
		cNames.append("colnames("+ edgeFrameName +") <- c(");

		cNames.append( "\"Parent_cues_sampled\",");
		columns++;

		ImmutableArray<String> cueNames = mdp.CUE_LABELS;
		for (int i = 0; i < cueNames.length(); i ++)
		{
			cNames.append( "\"Parent_NumberOfCues_" + cueNames.get(i) + "\",");
			columns++;
		}

		cNames.append( "\"Child_cues_sampled\",");
		columns++;

		for (int i = 1; i < cueNames.length(); i ++)
		{
			cNames.append( "\"Child_NumberOfCues_" + cueNames.get(i) + "\",");
			columns++;
		}

		cNames.append( "\"Proportion\"");
		columns++;

		cNames.append(")");

		// Create data frame
		sb.append(edgeFrameName + "<-as.data.frame(matrix(0,ncol=" + columns + ",nrow=0));");
		sb.append("\n"+cNames.toString() + ";");

		// add edges
		for (int i = 0; i < edges.size(); i ++)
			sb.append("\n" + edgeFrameName+"[" + (i+1) + ",]<-" + edges.get(i).toDataFrameRow() + ";");

		return sb.toString();

	}

	public boolean ranForwardPruning() {
		return ranForwardPruning;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Getters for fields to save	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * @return cuesSampled: a DecimalNumberArray of size m, where entries cuesSampled[i] stores the proportion of all agents 
	 * that have sampled exactly i cues (m is the maximum number of cues that can be sampled). 
	 * Throws an IllegalStateException if the tree has not gone through a forward pruning pass.
	 */
	public DecimalNumberArray getCuesSampled() {
		if (!ranForwardPruning)
			throw new IllegalStateException("Exception in DecisionTree.getCuesSampled(): requesting the cuesSampled (DecimalNumberArray) of a tree that has NOT gone through a forward pruning pass.");
		return cuesSampled;
	}
	
	/**
	 * @return proportionEating: a DecimalNumberArray of size m, where entries proportionEating[i] stores the proportion of all agents 
	 * that decide to eat the resource after having sampled exactly i cues (m is the maximum number of cues that can be sampled). 
	 * Throws an IllegalStateException if the tree has not gone through a forward pruning pass.
	 */
	public DecimalNumberArray getProportionEating() {
		if (!ranForwardPruning)
			throw new IllegalStateException("Exception in DecisionTree.getPoportionEating(): requesting the proportionEating (DecimalNumberArray) of a tree that has NOT gone through a forward pruning pass.");
		return proportionEating;
	}
	
	/**
	 * @return proportionDiscarding: a DecimalNumberArray of size m, where entries proportionDiscarding[i] stores the proportion of all agents 
	 * that decide to discard the resource after having sampled exactly i cues (m is the maximum number of cues that can be sampled). 
	 * Throws an IllegalStateException if the tree has not gone through a forward pruning pass.
	 */
	public DecimalNumberArray getProportionDiscarding() {
		if (!ranForwardPruning)
			throw new IllegalStateException("Exception in DecisionTree.getProportionDiscarding(): requesting the proportionDiscarding (DecimalNumberArray) of a tree that has NOT gone through a forward pruning pass.");
		return proportionDiscarding;
	}
	
	/**
	 * @return expectedOutcomes: a DecimalNumberArray of size 3, where entries indicate the expected outcome of [0] sampling, [1] eating,
	 * and [2] discarding of the ROOT node of the tree. 
	 * Throws an IllegalStateException if the tree has not gone through a backward pass.
	 */
	public DecimalNumberArray getExpectedOutcomes() {
		if (!ranBackward)
			throw new IllegalStateException("Exception in DecisionTree.getExpectedOutcomes(): requesting the expected outcomes of the root node (DecimalNumberArray) of a tree that has NOT gone through a backward pass.");
		
		return expectedOutcomes;
	}
	
	
	/**
	 * @return expectedCuesSampled: the expected number of cues that an agent will sample when following this policy. This
	 * expectation is computed as the dot product between the number of cues sampled and the probability of the agent sampling that
	 * exact amount of cues. 
	 * Throws an IllegalStateException if the tree has not gone through a forward pruning pass.
	 */
	public DecimalNumber getExpectedCuesSampled() {
		if (!ranForwardPruning)
			throw new IllegalStateException("Exception in DecisionTree.getExpectedCuesSampled(): requesting the expected number of cues sampled of a tree that has NOT gone through a forward pruning pass.");
		return expectedCuesSampled;
	}
	
	/** Returns the dominance of each cue type when discarding. The dominance of a cue type in a node is defined as
	//     (number of cues of type label) - (total number of cues sampled). The dominance of a type when eating is
	//		for each eating leaf node: proportion(node)*dominance(cueLabel)*/	
	public DecimalNumberArray getDominancesWhenDiscarding() {
		return this.dominanceDiscarding;
	}
	
	/** Returns the dominance of each cue type when eating. The dominance of a cue type in a node is defined as
	//     (number of cues of type label) - (total number of cues sampled). The dominance of a cue type when eating is
	//		sum ( for each eating leaf node: proportion(node)*dominance(cueLabel) ) / sum( for each eating leaf node: proportion(node))
	 * */	
	public DecimalNumberArray getDominancesWhenEating() {
		return this.dominanceEating;
	}
	
	
	/**
	 * @return totalProportionEating: the proportion of agents that will (eventually) eat the resource.
	 * Throws an IllegalStateException if the tree has not gone through a forward pruning pass.
	 */
	public DecimalNumber getTotalProportionEating() {
		if (!ranForwardPruning)
			throw new IllegalStateException("Exception in DecisionTree.getTotalProportionEating(): requesting the total proportion of agents that have eaten the resource of a tree that has NOT gone through a forward pruning pass.");
		return totalProportionEating;
	}
	
	
	/**
	 * @return totalProportionDiscarding: the proportion of agents that will (eventually) discard the resource.
	 * Throws an IllegalStateException if the tree has not gone through a forward pruning pass.
	 */
	public DecimalNumber getTotalProportionDiscarding() {
		if (!ranForwardPruning)
			throw new IllegalStateException("Exception in DecisionTree.getTotalProportionDiscarding(): requesting the total proportion of agents that have discarded the resource of a tree that has NOT gone through a forward pruning pass.");
		return totalProportionDiscarding;
	}
	
	
	
	
}
