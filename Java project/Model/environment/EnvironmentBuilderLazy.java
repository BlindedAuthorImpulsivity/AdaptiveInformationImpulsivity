package environment;

import java.text.DecimalFormat;

import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import helper.Helper.MisspecifiedException;
import markovDecisionProcess.MarkovDecisionProcess;

/** An EnvironmentBuilderLazy is an EnvironmentBuilder that does not have a fully specified matrix. That is,
 * the exact resource and extrinsic event probabilities have not yet been specified - and hence do not have to
 * be placed in the working memory. 
 * 
 * The idea is that an agent can take a lazy specified and create an Environment at the runtime of that
 * agent. As such, the (possibly quite large) matrix containing the probabilities for each resource and
 * extrinsic events are only created when they are actually used.
 * 
 * 
 * 
 */
public class EnvironmentBuilderLazy extends AbstractEnvironmentBuilder {

	private MarkovDecisionProcess mdp;
	
	public EnvironmentBuilderLazy(double resourceValueMean, double resourceValueSD,
			ValueDistributionType resourceValueDistributionType, double extrinsicEventMean, double extrinsicEventSD,
			ValueDistributionType extrinsicEventDistributionType, double interruptionRate)  {
		super(resourceValueMean, resourceValueSD, resourceValueDistributionType, extrinsicEventMean, extrinsicEventSD, extrinsicEventDistributionType, interruptionRate);
		
	}
	
	public void setMDP(MarkovDecisionProcess mdp) {
		this.mdp=mdp;
	}

	public String toString()
	{
		DecimalFormat df = new DecimalFormat("#.####");
		StringBuilder sb = new StringBuilder();
		sb.append("[M resource = " + df.format(this.resourceValueMean));
		sb.append(", SD resource = " + df.format(this.resourceValueSD));
		sb.append(", Resource distribution = " + this.resourceValueDistributionType);
		sb.append(", M extrinsic = " + df.format(this.extrinsicEventMean));
		sb.append(", SD extrinsic = " + df.format(this.extrinsicEventSD));
		sb.append(", Extrinsic distribution = " + this.extrinsicEventDistributionType);
		sb.append(", Interruption = " + df.format(this.interruptionRate));
		sb.append("]");
		return sb.toString();
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Environment toEnvironment() throws MisspecifiedException {
		if (mdp == null) 
			throw new IllegalStateException("Exception in EnvironmentBuilderLazy.toEnvironment(): trying to create an environment, but the MDP has not been specified yet. Please make sure that the toEnvironment() function is called after a MDP has been set with setMDP().");
		
		// First, create the matrix containing all probabilities, using the MDP
		DecimalNumberArray values = mdp.POSSIBLE_VALUES;
		DecimalNumberArray resourceValues = ValueDistributionType.getPredefinedDistribution(values, resourceValueDistributionType, resourceValueMean, resourceValueSD);
		DecimalNumberArray extrinsicValues = ValueDistributionType.getPredefinedDistribution(values, extrinsicEventDistributionType, extrinsicEventMean, extrinsicEventSD);
		DecimalNumberMatrix matrix = new DecimalNumberMatrix(values.length(), 3);
		matrix.setColumn(0, values);
		matrix.setColumn(1, resourceValues);
		matrix.setColumn(2, extrinsicValues);
		matrix.setColumnNames("Value", "p(Resource value = value)", "p(Extrinsic event value = value)");
		
		return new Environment(resourceValueMean, resourceValueSD, resourceValueDistributionType, extrinsicEventMean, extrinsicEventSD, extrinsicEventDistributionType, interruptionRate, matrix);
	}


	

}
