package staticManagers;

import agent.AgentType;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberMatrixTableView.TableViewObject;
import environment.Environment;
import environment.ValueDistributionType;

/**
 * A class that contains a shadow of the .out file. Specifically, a shadow contains only information on the
 * .out files:
 * - Absolute filename (i.e., including the path to the filename)
 * - File name
 * - Agent type
 * - Mean resource quality
 * - SD resource quality
 * - Mean extrinsicEvent
 * - SD extrinsicEvent
 * - Interruption rate.
 *
 */
public class OutputShadow implements TableViewObject{

	public final String 			filename;
	public final AgentType 			agentType;
	public final DecimalNumber 		resourceValueMean, 
									resourceValueSD, 
									extrinsicEventMean, 
									extrinsicEventSD, 
									interruptionRate;
	public final ValueDistributionType resourceValueDistributionType,
									  extrinsicEventDistributionType;

	public OutputShadow(String absoluteFilename, AgentType type, Environment e)
	{
		this.filename= absoluteFilename;
		this.agentType = type;
		this.resourceValueMean = e.resourceValueMean;
		this.resourceValueSD = e.resourceValueSD;
		this.extrinsicEventMean = e.extrinsicEventMean;
		this.extrinsicEventSD = e.extrinsicEventSD;
		this.interruptionRate = e.interruptionRate;
		this.resourceValueDistributionType = e.resourceValueDistributionType;
		this.extrinsicEventDistributionType = e.extrinsicEventDistributionType;
	}
	
	/** Constructor that creates a shadow from another shadow. Used when
	 * some other class wants to extend the OutputShadow class to include
	 * for instance an is-selected flag.
	 * @param original
	 */
	public OutputShadow(OutputShadow original) {
		this.filename= original.filename;
		this.agentType = original.agentType;
		this.resourceValueMean = original.resourceValueMean;
		this.resourceValueSD = original.resourceValueSD;
		this.extrinsicEventMean = original.extrinsicEventMean;
		this.extrinsicEventSD = original.extrinsicEventSD;
		this.interruptionRate = original.interruptionRate;
		this.resourceValueDistributionType = original.resourceValueDistributionType;
		this.extrinsicEventDistributionType = original.extrinsicEventDistributionType;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}

	/**
	 * @return the agentType
	 */
	public AgentType getAgentType() {
		return agentType;
	}

	/**
	 * @return the resourceValueMean
	 */
	public DecimalNumber getResourceValueMean() {
		return resourceValueMean;
	}

	/**
	 * @return the resourceValueSD
	 */
	public DecimalNumber getResourceValueSD() {
		return resourceValueSD;
	}

	/**
	 * @return the extrinsicEventMean
	 */
	public DecimalNumber getExtrinsicEventMean() {
		return extrinsicEventMean;
	}

	/**
	 * @return the extrinsicEventSD
	 */
	public DecimalNumber getExtrinsicEventSD() {
		return extrinsicEventSD;
	}

	/**
	 * @return the interruptionRate
	 */
	public DecimalNumber getInterruptionRate() {
		return interruptionRate;
	}

	/**
	 * @return the resourceValueDistributionType
	 */
	public ValueDistributionType getResourceValueDistributionType() {
		return resourceValueDistributionType;
	}

	/**
	 * @return the extrinsicEventDistributionType
	 */
	public ValueDistributionType getExtrinsicEventDistributionType() {
		return extrinsicEventDistributionType;
	}

	@Override
	public void setFieldValue(String fieldName, Object value) {
		// Since the output shadows are immutable, this function need not be implemented
		
	}

	@Override
	public Object getFieldValue(String fieldName) {
		// Since the output shadows are immutable, this function need not be implemented
		return null;
	}


}
