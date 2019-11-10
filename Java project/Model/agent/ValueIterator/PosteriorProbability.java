package agent.ValueIterator;

import java.io.Serializable;
import java.util.Arrays;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import environment.Environment;
import helper.Helper;
import markovDecisionProcess.MarkovDecisionProcess;
import staticManagers.ObserverManager;

// TODO: comment
public class PosteriorProbability implements Runnable, Serializable
{
	private static final long serialVersionUID = Helper.programmeVersion;
	
	private transient final MarkovDecisionProcess 	mdp;
	private transient final Environment 			environment;
	private final Integer[] 						cueSet;

	// Note that we only store the probability distribution over the resource values, and not
	// the corresponding resource values. That is, we store an array [-n, -n+1, ..., n-1, n], 
	// Representing the probabilities of the resource having value -n to n, but not the
	// actual values [-n ... n]. Instead, these values are stored in the environment, and are final and immutable.
	private  DecimalNumberArray					posteriorProbabilityOfResourceValues;
	
	// Likewise, we only store the actual probabilities for the future cues, and not the 
	// cue labels. Hence, the i'th index of posteriorProbabilityOfFutureCues corresponds to the
	// i'th cue label in the MDP.
	private  DecimalNumberArray					posteriorProbabilityOfFutureCues;

	public PosteriorProbability(MarkovDecisionProcess mdp, Environment environment, Integer[] cueSet)
	{
		this.mdp = mdp;
		this.environment = environment;
		this.cueSet = cueSet;
	}
	
	public  Integer[] 						getCueSet(){return this.cueSet;}
	
	public void run()
	{
		try {
			// Check some things to make sure values are valid. Otherwise throw an error.
			if (this.cueSet.length != mdp.NUMBER_OF_CUE_LABELS)
				throw new IllegalArgumentException("Exception when creating posterior probability: The number of different cue types in the specified cueSet does not match input parameters");
			for (Integer c:cueSet) if (c<0) throw new IllegalArgumentException("Received a negative number of cues.");

			// Now that we know for sure that we are dealing with valid input, calculate the posterior distribution of the resource quality given the cue set.
			boolean calculatedResourceValuePosterior = calculateBayesianPosteriors();
			boolean calculatedPosteriorCueDistribution = calculateConditionalCueEmissionMatrix();
			if (!calculatedResourceValuePosterior || !calculatedPosteriorCueDistribution) {
				ObserverManager.notifyObserversOfError("Error when computing Posterior Probability object", "Unable to calcuate posterior probability distribution for cue set " + Helper.arrayToString(cueSet) + ". Computation for this Posterior Probability stopped.");
				posteriorProbabilityOfResourceValues=null;
				posteriorProbabilityOfFutureCues=null;
				return;
			}
		} catch (Exception e) { ObserverManager.notifyObserversOfError(e);}
		
	}
	
	
	/**<pre>
	 * Update prior probability distribution of resource values (p(Resource value)) to posterior probability distribution ( p(Resource value|cueSet) )
	 *  as follows (Bayesian updating):
	 *  
	 * Step 0. Reminder: Bayes' theorem is p(Hypothesis|Data) = p(Data|Hypothesis)*p(Hypothesis) / p(Data)
	 * 			Where p(Data) = SUM ( p(Data|Hypothesis h)*p(Hypothesis h) ) over all possible Hypotheses h.
	 * 
	 * Step 1. determine for each resource value v the probability of observing the data (cueSet) given that the true value is v:
	 * p(cueSet|Resource value=v) =
	 *			The probability of observing a x_i numbers of a cue with label i is p(C= cue_i|Resource value=v)^[x]
	 *			The probability of observing all cues c present in the data is the product of observing x_i cues with i, 
	 *				for all cues.
	 *  		However, the ordering of the cues in the data does not matter (e.g., cue1-cue2-cue1 should give the 
	 *				belief as cue2-cue1-cue1). Hence we multiply the product above by the total number of combinations
	 *				of the cues in the data are possible. This gives us p(cueSet|V=v)
	 *
     * 			Formally: p(cueSet|Resource value=v) 
     * 			= combinations * product for all c in PossibleCues: p(C=c|Resource value=v) ^ [number of c in cueSet]
	 *			= combinations * product[ p(cue emission|V=v)^[#c in cueSet]   ]
	 *
	 * Step 2. For each possible true value v, multiply the probability of observing the data (p(cueSet|Resource value = value)) by
	 * the prior probability of the resource value (p(Resource value = value). These are the UNNORMALIZED posterior probabilities.
	 *
	 * Step 3. Normalize these posteriors by dividing each posterior probability of a cue by the sum of all posteriors. 
	 * 		In Bayes theorem this is the normalization constant p(Data).
	 *
	 *Note that:
	 *- cueSet is an array of integers, denoting how many cues of each type has been sampled. Hence, a cueSet of [10 0]
	 *means that there are 10 observed cues of the first label, and zero of the second label.
	 *
	 */ 
	private boolean calculateBayesianPosteriors()
	{
		try {
			// Step 0: If there are no cues, retain the prior distribution.
			int totalCues = 0;
			for (int c:cueSet) totalCues += c;
			if (totalCues == 0) {
				posteriorProbabilityOfResourceValues = environment.getMatrix().getColumn("p(Resource value = value)").clone();
				return true;
			}
					
			// Otherwise, calculate the posterior distribution.
			// Calculate the number of ways the cueSet can be ordered.
			long combinations = Helper.combinations(cueSet);

			// Step 1: calculate p(cueSet|Resource value = value), for all resource values.
			DecimalNumberArray values 				= this.environment.getMatrix().getColumn("Value");
			DecimalNumberArray priorProbabilities 	= this.environment.getMatrix().getColumn("p(Resource value = value)").clone();
			// Step 1b: Create an array that contains p(cueSet|Resource value = value), for all resource values.
			DecimalNumberArray probabilityOfDataGivenHypothesisArray = new DecimalNumberArray(values.length());
			for(int v=0; v<	values.length();v++)
			{
				//calculate p(cueSet|Resource value =v):= 
				//			product( [probability that cue n is observed]*[number of times that cue n is observed] ) for all cue labels n.
				// which is the same as: sum over all cue labels n: ( p(cueLabel|v) ^ [#cueLabel in cueSet] )
				DecimalNumber probabilityDataGivenHypothesis = new DecimalNumber(1); // the hypothesis is that ( Resource value = v ), the datum is a the set of cues observed
				for (int c=0; c< cueSet.length; c++){
					DecimalNumber probabilityOfSpecificCue= mdp.CUE_EMISSION_MATRIX(false).getRow(v).get(c)		.pow(cueSet[c], true);
					probabilityDataGivenHypothesis.multiply(probabilityOfSpecificCue);

				}

				// probabilityDataGivenHypothesis is now the probability of observing an ordered series of cues. For instance,
				// the probability of observing three cues: [positive] [positive] [negative] 
				// However, the ordering does not matter:   [positive] [negative] [positive] results in the same posterior distribution.
				// Hence, we multiple the probability of observing one particular order with the total combination of orderings we can make

				// Add the result to the array that contains p(cueSet|Resource value = v), for all v.
				probabilityOfDataGivenHypothesisArray.set(v,  probabilityDataGivenHypothesis.multiply(combinations));
			}

			// Step 2: calculate unnormalized posteriors: p(Resource value = value| cueSet) =  p(Resource value=value)*p(cueSet|Resource value=v)
			DecimalNumberArray unnormalizedPosteriors = new DecimalNumberArray(probabilityOfDataGivenHypothesisArray.length());
			for (int v=0;v<unnormalizedPosteriors.length();v++)
				unnormalizedPosteriors.set(v, priorProbabilities.get(v).multiply(probabilityOfDataGivenHypothesisArray.get(v)));

			// Step 3: normalize posterior probability: p(cueSet|V=v)*p(V=v)/p(cueSet)
			posteriorProbabilityOfResourceValues = unnormalizedPosteriors.scaleToSumToOne();
			posteriorProbabilityOfResourceValues.setImmutable(true);

			// Step 4: a check
			if (!posteriorProbabilityOfResourceValues.isProbability()) {
				ObserverManager.notifyObserversOfError("Error during computation of posterior proabability. ", 
						"When computing the posterior probabilities of the resource value  after observing a cue set of " + Helper.arrayToString(cueSet) +", the posterior was not a probability distribution: " + posteriorProbabilityOfResourceValues);
				return false;}
			return true;
		} catch (Exception e) {ObserverManager.notifyObserversOfError(e); return false;}
	}

	/** <pre>
	 * After calculating the posterior distribution of the resource values given the 
	 * observed cue set, we can calculate the posterior probability of the cues. That is,
	 * we can compute what the probability is that the next cue has a certain label,
	 * for each possible label. These probabilities are stored in the DecimalNumberArray
	 * posteriorProbabilityOfFutureCues. The cue labels are stored in the MDP's 
	 * CUE_LABELS (an ImmutableArray).
	 * 
	 * Formally, the posterior probability of a cue is: 
	 * p(Next cue = cue label | posterior distribution ), or
	 * 
	 * p(Next cue = cue label | Resource value = v) * p(Resource value = v), for all resource values v.
	 * 
	 * This probability is calculated 'simply' as the dot product of p(RV) and p(CE of c), where
	 * p(RV) 			= 		the vector of POSTERIOR resource value probabilities, and
	 * p(CE of c)		=		the vector of cue emission probabilities* for cue label c.
	 * 
	 * (* this vector describes the probability of observing a cue for each resource value v. 
	 * it is stored in the MDP's CUE_EMISSION_MATRIX at column [c], where c is the location of
	 * that cue in the MDP's CUE_LABELS).
	 * 
	 * 
	 * The posteriorProbabilityOfFutureCues is an array of the form:
	 * [ p(Next cue = cue label 1 | posterior distribution ), ..., p(Next cue = cue label n | posterior distribution ) ]
	 * 
	 * This function returns false if the resulting posterior probability distribution of future cues 
	 * is not a probability distribution (i.e., does not sum to 1 or has entries < 0). It returns true
	 * otherwise.
	 */
	private boolean calculateConditionalCueEmissionMatrix ()
	{
		try {
			posteriorProbabilityOfFutureCues = DecimalNumberArray.rep(new DecimalNumber(0), mdp.NUMBER_OF_CUE_LABELS);
			// For each cue labels c: calculate p(Next cue = cue label | posterior distribution)
			for (int c = 0; c < posteriorProbabilityOfFutureCues.length(); c++) {
				DecimalNumberArray probabilityOfCueGivenResource = mdp.CUE_EMISSION_MATRIX(false).getColumn(c);
				posteriorProbabilityOfFutureCues.set(c, probabilityOfCueGivenResource.dotProduct(posteriorProbabilityOfResourceValues) );
			}
			posteriorProbabilityOfFutureCues.setImmutable(true);
			if (!posteriorProbabilityOfFutureCues.isProbability()) {
				ObserverManager.notifyObserversOfError("Error when computing posterior probability of future cue", 
						"The calculated posterior probability of a future cue, conditional on receiving cue set " + Helper.arrayToString(cueSet) + " is not a probability"
								+ " distribution (i.e., has values lower than 1 or does not sum to 1). The conditional probability distribution of cues is: " + posteriorProbabilityOfFutureCues);
				return false;
			}
			return true; 
		} catch (Exception e) {ObserverManager.notifyObserversOfError(e); return false;}
	}
	
	/** <pre>
	 * Returns the posterior probability distribution of resource values after observing
	 * the PosteriotProbability's cueSet. This array has the following form:
	 * 
	 * [p(Resource value = v1|cueSet), p(Resource value = v2|cueSet), ..., p(Resource value = vn|cueSet)],
	 * 
	 * where vn is the n'th index position of the environment's resource value column.
	 * 
	 * This array, and all values in this array, are immutable.
	 * 
	 * @return
	 */
	public DecimalNumberArray posteriorProbabilityOfResourceValues ()
	{
		return this.posteriorProbabilityOfResourceValues;
	}
	
	/** <pre>
	 * Returns the posterior probability distribution of the next cue label after observing
	 * the PosteriotProbability's cueSet. This array has the following form:
	 * 
	 * [p(Next cue = cueLabel1 | cueSet), 	p(Next cue = cueLabel2 | cueSet), ..., p(Next cue = cueLabeln | cueSet)	]
	 * 
	 * where cueLabeln is the n'th cue label in the MDP's CUE_LABELS (corresponding to the (+1)th column in the
	 * MDP's CUE_EMISSION_MATRIX. 
	 * 
	 * This array, and all values in this array, are immutable.
	 * @return
	 */
	public DecimalNumberArray posteriorProbabilityOfFutureCues()
	{
		return this.posteriorProbabilityOfFutureCues;
	}
	

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();	
		sb.append("Posterior probability distribution of resource values:\n");
		sb.append(Helper.repString("=", 100) + "\n");
		
		sb.append("Cues received:\n");
		for (int c = 0; c<cueSet.length; c++) sb.append(mdp.CUE_LABELS.get(c) + "\t");
		sb.append("\n");
		for (int c= 0; c < cueSet.length; c++) sb.append(Helper.repString("-", mdp.CUE_LABELS.get(c).length())+"\t");
		sb.append("\n");
		for (int c = 0; c<cueSet.length; c++) {
			String numberOfCues = ""+cueSet[c];
			sb.append(numberOfCues + Helper.repString(" ", mdp.CUE_LABELS.get(c).length())+"\t");
		}
		
		sb.append("\n\nProbability distribution conditioned on these cues:");
		sb.append("\n" + Helper.repString( "-", 100) + "\n");
		sb.append("\nResource values: " + environment.getMatrix().getColumn("Value"));
		sb.append("\nDistribution:    " + posteriorProbabilityOfResourceValues);
		
		sb.append("\n\nCue emmissions based on this posterior probability distribution:");
		sb.append("\nCue label:       " + mdp.CUE_LABELS);
		sb.append("\nDistribution:    " + posteriorProbabilityOfFutureCues);
		
		sb.append("\n" + Helper.repString("=", 100));
		
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(cueSet);
		result = prime * result
				+ ((posteriorProbabilityOfFutureCues == null) ? 0 : posteriorProbabilityOfFutureCues.hashCode());
		result = prime * result + ((posteriorProbabilityOfResourceValues == null) ? 0
				: posteriorProbabilityOfResourceValues.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PosteriorProbability other = (PosteriorProbability) obj;
		if (!Arrays.equals(cueSet, other.cueSet))
			return false;
		if (posteriorProbabilityOfFutureCues == null) {
			if (other.posteriorProbabilityOfFutureCues != null)
				return false;
		} else if (!posteriorProbabilityOfFutureCues.equals(other.posteriorProbabilityOfFutureCues))
			return false;
		if (posteriorProbabilityOfResourceValues == null) {
			if (other.posteriorProbabilityOfResourceValues != null)
				return false;
		} else if (!posteriorProbabilityOfResourceValues.equals(other.posteriorProbabilityOfResourceValues))
			return false;
		return true;
	}

}
