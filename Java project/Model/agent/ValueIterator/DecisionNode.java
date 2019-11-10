package agent.ValueIterator;

import java.text.DecimalFormat;
import java.util.ArrayList;

import agent.Action;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.TransformationFunction.TransformationFunctionDecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import environment.Environment;
import estimationParameters.EstimationParameters;
import helper.Helper;
import helper.Helper.ImpossibleStateException;
import helper.Helper.InvalidProportionException;
import markovDecisionProcess.MarkovDecisionProcess;

// All DecimalNumber immutable - use setter to change (which makes it temporary mutable)
public class DecisionNode implements Comparable<DecisionNode>
{
	public transient final MarkovDecisionProcess			mdp;
	public transient final Environment 						environment;
	public transient final EstimationParameters				estimationParameters;
	
	public final Integer[]									cueSet;
	public final int										cuesSampled;
	public transient final PosteriorProbabilityTable  		posteriorProbabilityTable;
	public final PosteriorProbability 						probabilityDistribution;
	
	public ArrayList<DecisionEdge> 							childEdges;
	public ArrayList<DecisionEdge> parentEdges;
	
	public		 DecimalNumber 								budget,
															proportion;
	
	// An array of size 3 containing DecimalNumbers for the expected outcomes of 1) sampling, 2) eating, 3) discarding.
	// Made private so that the user has to call expectedOutcomes(), which provides a useful javadoc
	private DecimalNumberArray								expectedOutcomes;
	
															
	public ArrayList<Action>								bestAction;
	public boolean											isLeafNode;
	
	// Fields that contain the benefits and costs for accepting and discarding. These fields are only changed after
	// the forward pruning pass. These fields should never, ever, ever be used for further computation during runtime.
	public  DecimalNumber	acceptingExpectedImmediateBenefits,
					acceptingExpectedFutureBenefits,
					acceptingProbabilityBenefits,
					acceptingExpectedImmediateCosts,
					acceptingExpectedFutureCosts,
					acceptingProbabilityCosts,
					acceptingExpectedImmediateNeutral,
					acceptingExpectedFutureNeutral,
					acceptingProbabilityNeutral,
	
					discardingExpectedImmediateBenefits,
					discardingExpectedFutureBenefits,
					discardingProbabilityBenefits,
					discardingExpectedImmediateCosts,
					discardingExpectedFutureCosts,
					discardingProbabilityCosts,
					discardingExpectedImmediateNeutral,
					discardingExpectedFutureNeutral,
					discardingProbabilityNeutral;
	
	// Some stuff that is used for printing a tree out to the screen in text form - useful when debugging, but rather obsolete now that 
	// the program is completed and has a visual interface
	public transient static final DecimalFormat df = new DecimalFormat("0.00000");
	public transient static final int colLength = 25;
		
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Constructor		/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	
	/** Creates a DecisionNode with the specified parameters. The budget and proportion are 
	 * NOT cloned in the constructor - hence, they should be cloned prior to instantiation. 
	 * 
	 * @param mdp
	 * @param posteriorProbabilityTable
	 * @param environment
	 * @param cueSet
	 * @param budget
	 * @param proportion
	 */
	public DecisionNode (	MarkovDecisionProcess mdp,
							PosteriorProbabilityTable posteriorProbabilityTable, 
							Environment environment,
							EstimationParameters estimationParameters,
							Integer[] cueSet, 
							DecimalNumber budget, 
							DecimalNumber proportion) 
	{
		this.mdp = mdp;
		this.posteriorProbabilityTable = posteriorProbabilityTable;
		this.environment = environment;
		this.estimationParameters = estimationParameters;
		this.cueSet = cueSet;
		this.cuesSampled = (int) Helper.sum(cueSet);
		this.probabilityDistribution = posteriorProbabilityTable.getPosterior(cueSet);
		
		this.proportion = 	proportion.setImmutable(true);
		this.budget = 		budget.setImmutable(true);
		this.bestAction = 	new ArrayList<>();
		this.childEdges = 	new ArrayList<>();			// List of edges connecting the children of this node to this node (this node is parent)
		this.parentEdges = 	new ArrayList<>();			// List of edges connecting the parents of this node to this node (this node is child)
		this.expectedOutcomes = new DecimalNumberArray(Action.values().length-1);
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Setters		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	/** Set the budget of this node. Normally this field is immutable. However,
	 * this function temporarily makes it mutable. It is highly recommended
	 * to not change this mutability anywhere else - allowing changes only in this 
	 * way ensure that no incidental changes occur.
	 */
	public void setBudget(DecimalNumber newBudget) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		this.budget.setImmutable(false);
		this.budget.set(newBudget);
		this.budget.setImmutable(true);
	}
	
	/** Set the proportion of this node. Normally this field is immutable. However,
	 * this function temporarily makes it mutable. It is highly recommended
	 * to not change this mutability anywhere else - allowing changes only in this 
	 * way ensure that no incidental changes occur.
	 */
	public void setProportion(DecimalNumber newBudget) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		this.proportion.setImmutable(false);
		this.proportion.set(newBudget);
		this.proportion.setImmutable(true);
	}
	
	/** Set the budget of this node. Normally this field is immutable. However,
	 * this function temporarily makes it mutable. It is highly recommended
	 * to not change this mutability anywhere else - allowing changes only in this 
	 * way ensure that no incidental changes occur.
	 */
	public void setBudget(double newBudget) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		this.budget.setImmutable(false);
		this.budget.set(newBudget);
		this.budget.setImmutable(true);
	}
	
	/** Set the proportion of this node. Normally this field is immutable. However,
	 * this function temporarily makes it mutable. It is highly recommended
	 * to not change this mutability anywhere else - allowing changes only in this 
	 * way ensure that no incidental changes occur.
	 */
	public void setProportion(double newBudget) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		this.proportion.setImmutable(false);
		this.proportion.set(newBudget);
		this.proportion.setImmutable(true);
	}
	
	/**
	 * Sets the expectedOutcome array and (calculated from that) the bestAction array. 
	 * The expectedOutcome array should contain three values, representing the expected
	 * outcome for (pos 0) sampling, (pos 1) eating, and (pos 2) discarding. 
	 * 
	 * If any of the three expectedOutcomes are null, no bestActions are computed.
	 * 
	 */
	public void setExpectedOutcomes(DecimalNumberArray expectedOutcomes)
	{
		this.expectedOutcomes = expectedOutcomes;
		DecimalNumber maximum = expectedOutcomes.max();
	
		if (expectedOutcomes.containsNull())
			throw new IllegalStateException("Exception in DecisionNodes.setExpectedOutcomes(): the array of expected outcomes contains a null");
		
		// If there is no indifference point specified (i.e., an indifference point lower than 0):
		// 		Take the action with the maximum expected outcome to be the best action
		if (estimationParameters.indifferencePoint.compareTo(0) <= 0) {
			if (expectedOutcomes().get(0).equals(maximum, true)) 	 	 	bestAction.add(Action.SAMPLE);
			if (expectedOutcomes().get(1).equals(maximum, true))     		bestAction.add(Action.EAT);
			if (expectedOutcomes().get(2).equals(maximum, true)) 			bestAction.add(Action.DISCARD);
		} 
		
		// Else, if the indifference point is specified:
		else {
		// If the difference between the expected outcome of sampling and the maximum outcome is below the threshold specified in the 
		// estimationParameters, add the sampling action to the list of best actions
		if (DecimalNumber.subtract(expectedOutcomes.get(0), maximum).abs().compareTo(estimationParameters.indifferencePoint)==-1)
			bestAction.add(Action.SAMPLE);
		
		// If the difference between the expected outcome of eating and the maximum outcome is below the threshold specified in the 
		// estimationParameters, add the eating action to the list of best actions
		if (DecimalNumber.subtract(expectedOutcomes.get(1), maximum).abs().compareTo(estimationParameters.indifferencePoint)==-1)
			bestAction.add(Action.EAT);
				
		// If the difference between the expected outcome of discarding and the maximum outcome is below the threshold specified in the 
		// estimationParameters, add the discarding action to the list of best actions
		if (DecimalNumber.subtract(expectedOutcomes.get(2), maximum).abs().compareTo(estimationParameters.indifferencePoint)==-1)
			bestAction.add(Action.DISCARD);
		}
				
	}
	
		
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Getters		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	/** Returns a DecimalNumberArray that contains 3 outcomes, representing the expected  outcomes for 0) sampling, 1) eating, 2) discarding, respectively. */
	public DecimalNumberArray expectedOutcomes() {
		return this.expectedOutcomes;
	}

	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////// 	Family ties		/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////	
	/**
	 * Find all DecisionNodes that result from adding one additional cue to the cueSet (i.e., find the children of this node).
	 * 
	 * Child nodes are not created if
	 * - the maximum number of cues that can be sampled is already reached, or
	 * - the child's budget after sampling is non-positive
	 * @return
	 * @throws InvalidProportionException 
	 * @throws ImpossibleStateException 
	 * @throws IllegalRangeException 
	 * @throws IllegalScaleException 
	 */
	public ArrayList<DecisionNode> createChildNodes () throws IllegalRangeException, IllegalScaleException
	{
		ArrayList<DecisionNode> children = new ArrayList<>();
	
		// Do not create child nodes if the maximum number of cues has already been reached
		if (this.cuesSampled >= mdp.MAXIMUM_CUES)
			return children;
		
		// Calculate what the budget after sampling would be
		DecimalNumber budgetAfterSampling = budget.subtract(mdp.COST_OF_SAMPLING);
		
		// Do not create children if the budget after sampling is lower than or equal to 0
		if (budgetAfterSampling.compareTo(DecimalNumber.ZERO)!=1 || budgetAfterSampling.equals(DecimalNumber.ZERO, true))
			return children;

		// Create a new child for each possible subsequent cue
		for(int i=0;i<cueSet.length;i++)
		{
			// Add a new cue to the cue set for Cue label i
			Integer[] childCueSet = cueSet.clone();
			childCueSet[i]++;
			
			// Retrieve the probability of observing this extra cue, conditioned on the parent's resource value probability distribution
			DecimalNumber probabilityOfChild = probabilityDistribution.posteriorProbabilityOfFutureCues().get(i).clone();
			
			// make a clone of the budgetAfterSampling to use as the child's budget.
			DecimalNumber childBudget = budgetAfterSampling.clone();
			
			// The proportion of the total population that ends up in the child node is the proportion of the parent multiplied by the probability of going from the parent to the child
			DecimalNumber childProportion = proportion.multiply(probabilityOfChild); 

			// The budget of the child is the budget of the parent minus the cost of sampling one additional cue
			DecisionNode child = new DecisionNode(mdp, posteriorProbabilityTable, this.environment, this.estimationParameters, childCueSet, childBudget, childProportion);
			DecisionEdge edge = new DecisionEdge(this, child, probabilityOfChild);
			
			this.childEdges.add(edge);
			child.parentEdges.add(edge);
			
			children.add(child);
		}
		
		return children;
	}


	/**
	 * Creates a DEEP clone of this node. Note that the clone's parent and child edges
	 * have a reference to the clone, not to the original. That is, edges in the childEdges
	 * have the clone as the parent, and edges in the parentEdges have the clone as the child.
	 * @throws IllegalRangeException 
	 */
	public DecisionNode clone (DecisionNode original) throws IllegalRangeException
	{
		DecisionNode clone = new DecisionNode(mdp, posteriorProbabilityTable, environment, this.estimationParameters, cueSet.clone(), budget.clone(), proportion.clone());
		clone.expectedOutcomes = expectedOutcomes.clone();

		clone.childEdges = new ArrayList<>();
		for (DecisionEdge de: childEdges) 
			clone.childEdges.add(new DecisionEdge(clone, de.child, de.proportion.clone()));

		clone.parentEdges = new ArrayList<>();
		for (DecisionEdge de: parentEdges) 
			clone.parentEdges.add(new DecisionEdge(de.parent, clone, de.proportion.clone()));

		clone.bestAction = new ArrayList<>();
		for (Action a: bestAction)
			clone.bestAction.add(a);
		return clone;
	}
		
	

	/**
	 * Compare two DecisionNodes. Decision nodes are sorted on the basis of 
	 * 	- number of cues sampled
	 *  - cueSets
	 *  - proportion
	 *  - budget
	 *  
	 *  @return -1, 0, or 1 as this DecisionNode is less than, equal to, or greater than the other DecisionNode
	 */
	@Override
	public int compareTo(DecisionNode other) {
		if (this.cuesSampled > other.cuesSampled)
			return -1;
		if (this.cuesSampled < other.cuesSampled)
			return 1;
		for (int i=0;i<cueSet.length;i++)
			if (cueSet[i] > other.cueSet[i])
				return -1;
			else if (cueSet[i] < other.cueSet[i])
				return 1;
		if (this.proportion.compareTo(other.proportion) == -1) return -1;
		if (this.proportion.compareTo(other.proportion) == 1) return 1;
		
		return (this.budget.compareTo(other.budget));
	}
	
	/**
	 * Determines whether two DecisionNodes are equal. Two nodes are equal if and only if
	 * they have the same probability distribution (i.e., pointing to the same posteriorProbability
	 * object) AND have the same number of cues sampled.
	 * @param other
	 * @return
	 */
	public boolean equals(DecisionNode other)
	{
		if (this.cuesSampled!= other.cuesSampled)
			return false;
		for (int i = 0;i < cueSet.length; i++)
			if (this.cueSet[i] != other.cueSet[i])
				return false;
		return true;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		String cuesSampledString = ""+this.cuesSampled;
		String cuesSampledStringSpaced = cuesSampledString + Helper.repString(" ", colLength-cuesSampledString.length());
		
		String budgetString = df.format(this.budget);
		String budgetStringSpaced = budgetString + Helper.repString(" ", colLength-budgetString.length());
		
		String proportionString = df.format(this.proportion);
		String proportionStringSpaced = proportionString + Helper.repString(" ", colLength-proportionString.length());
		
		String divider = "||" + Helper.repString(" ", colLength-2);
		
		StringBuilder cssb = new StringBuilder();
		for (Integer i: cueSet)
		{
			String cueI = ""+i;
			String cueISpaced = cueI + Helper.repString(" ", colLength - cueI.length());
			cssb.append(cueISpaced);
		}
		


		sb.append(cuesSampledStringSpaced+budgetStringSpaced+proportionStringSpaced+divider + cssb.toString() + divider);

		String bestActionString;

		StringBuilder bestActionStringBuilder = new StringBuilder();
		bestActionStringBuilder.append("{");
		if (bestAction.contains(Action.DEAD_ON_START)) bestActionStringBuilder.append(" X ");
		if (bestAction.contains(Action.SAMPLE)) bestActionStringBuilder.append(" SAM ");
		if (bestAction.contains(Action.EAT)) bestActionStringBuilder.append(" EAT ");
		if (bestAction.contains(Action.DISCARD)) bestActionStringBuilder.append(" DIS ");
		bestActionStringBuilder.append("}");
		bestActionString = bestActionStringBuilder.toString();


		String bestActionStringSpaced =  bestActionString + Helper.repString(" ", colLength - bestActionString.length());

		// Printing expected outcome
		String expectedOutcomeString;
		if (expectedOutcomes.containsNull()) 
			expectedOutcomeString = "N/A";
		else
			expectedOutcomeString = df.format(this.expectedOutcomes.max());
		String expectedOutcomeStringSpaced = expectedOutcomeString + Helper.repString(" ", colLength-expectedOutcomeString.length());

		// Printing expected outcome of sampling
		String expectedOutcomeSamplingString;
		if (expectedOutcomes.containsNull()) 
			expectedOutcomeSamplingString = "N/A";
		else 
			expectedOutcomeSamplingString = expectedOutcomes.get(0).toString(4);
		String expectedOutcomeSamplingStringSpaced = expectedOutcomeSamplingString + Helper.repString(" ", colLength-expectedOutcomeSamplingString.length());

		// Printing expected outcome of eating
		String expectedOutcomeEatingString;
		if (expectedOutcomes.containsNull()) 
			expectedOutcomeEatingString = "N/A";
		else 
			expectedOutcomeEatingString = expectedOutcomes.get(1).toString(4);
		String expectedOutcomeEatingStringSpaced = expectedOutcomeEatingString + Helper.repString(" ", colLength-expectedOutcomeEatingString.length());

		// Printing expected outcome of discarding
		String expectedOutcomeDiscardingString;
		if (expectedOutcomes.containsNull()) 
			expectedOutcomeDiscardingString = "N/A";
		else 
			expectedOutcomeDiscardingString = expectedOutcomes.get(2).toString(4);
		String expectedOutcomeDiscardingStringSpaced = expectedOutcomeDiscardingString + Helper.repString(" ", colLength-expectedOutcomeDiscardingString.length());
		
		sb.append(bestActionStringSpaced+expectedOutcomeStringSpaced+expectedOutcomeSamplingStringSpaced+expectedOutcomeEatingStringSpaced+expectedOutcomeDiscardingStringSpaced);
		
		
		sb.append("    "+"||"+"    ");
		DecimalNumberArray probabilities = this.probabilityDistribution.posteriorProbabilityOfResourceValues();
		for (int i = 0; i<probabilities.length();i++ )
		{
			String probString = df.format(probabilities.get(i));
			String probStringSpaced = probString + Helper.repString(" ", colLength-probString.length());
			sb.append(probStringSpaced);
		}
		
		sb.append("    "+"||"+"    ");
		for (String l: mdp.CUE_LABELS)
		{
			String probString = ""+this.getDominance(l);
			String probStringSpaced = probString + Helper.repString(" ", colLength-probString.length());
			sb.append(probStringSpaced);
		}
		return sb.toString();
		
	}
	
	/** Returns the dominance of cues with label cueLabel in the set of all observed cues in this node. 
	 * The dominance of a cue in is defined as the (number of cues with cueLabel) - (the number of all other cues). */
	public int getDominance(String cueLabel) {
		int cuesOfLabelType = this.cueSet[this.mdp.CUE_LABELS.indexOf(cueLabel)];
		return cuesOfLabelType - (this.cuesSampled-cuesOfLabelType);
	}
	
	/**
	 * Write this DecisionNode in a single line that can be read in in R. 
	 * @return
	 */
	public String toDataFrameRow()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append(cuesSampled+",");
		sb.append(budget + ",");
		sb.append(proportion + ",");
		for (int i = 0; i < cueSet.length; i ++) sb.append(cueSet[i]+",");
		
		if   (bestAction.size()==1) sb.append("\"" + bestAction.get(0)+"\",");
		else {
			sb.append("\"");
			for (int i = 0; i < bestAction.size(); i ++) {
				sb.append(bestAction.get(i).toString().charAt(0));
				sb.append(bestAction.get(i).toString().charAt(1));
			}
			sb.append("\",");
		}
		
		if (expectedOutcomes.max() == null) 	sb.append("NA,");
		else 							sb.append(expectedOutcomes.max()+",");
		
		if (expectedOutcomes.get(0) == null) 	sb.append("NA,");
		else 							sb.append(expectedOutcomes.get(0)+",");
		
		if (expectedOutcomes.get(1) == null) 	sb.append("NA,");
		else 							sb.append(expectedOutcomes.get(1)+",");
		
		if (expectedOutcomes.get(2) == null) 	sb.append("NA,");
		else 							sb.append(expectedOutcomes.get(2)+",");
		
		DecimalNumberArray probabilities = this.probabilityDistribution.posteriorProbabilityOfResourceValues();
		for (int i = 0; i < probabilities.length(); i ++)
		{
			if (i != probabilities.length()-1)
				sb.append(probabilities.get(i)+",");
			else
				sb.append(probabilities.get(i));
		}
		
		return "c(" + sb.toString() + ")";
	}

	
	
	
}