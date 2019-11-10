package environment;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import externalPrograms.RserveManager;
import helper.Helper.MisspecifiedException;
import staticManagers.ObserverManager;

public abstract class AbstractEnvironmentBuilder {

	protected DecimalNumber 				resourceValueMean;
	protected DecimalNumber 				resourceValueSD;
	protected ValueDistributionType 		resourceValueDistributionType;

	protected DecimalNumber 				extrinsicEventMean;
	protected DecimalNumber 				extrinsicEventSD;
	protected ValueDistributionType 		extrinsicEventDistributionType;

	protected DecimalNumber 				interruptionRate;

	public AbstractEnvironmentBuilder( 	DecimalNumber resourceValueMean, 
			DecimalNumber resourceValueSD,
			ValueDistributionType resourceValueDistributionType,
			DecimalNumber extrinsicEventMean,
			DecimalNumber extrinsicEventSD,
			ValueDistributionType extrinsicEventDistributionType,
			DecimalNumber interruptionRate) {
		this.resourceValueMean 				= resourceValueMean;
		this.resourceValueSD   				= resourceValueSD;
		this.resourceValueDistributionType 	= resourceValueDistributionType;

		this.extrinsicEventMean 			= extrinsicEventMean;
		this.extrinsicEventSD 				= extrinsicEventSD;
		this.extrinsicEventDistributionType = extrinsicEventDistributionType;

		this.interruptionRate 				= interruptionRate;
	}

	public AbstractEnvironmentBuilder( 	double resourceValueMean, 
			double resourceValueSD,
			ValueDistributionType resourceValueDistributionType,
			double extrinsicEventMean,
			double extrinsicEventSD,
			ValueDistributionType extrinsicEventDistributionType,
			double interruptionRate) {
		try {
			if (resourceValueSD < 0 || extrinsicEventSD < 0 || interruptionRate < 0 || interruptionRate > 1)
				throw new IllegalArgumentException("Created an environment with impossible variables. Standard deviations cannot be 0 (here: resourceValueSD = "+ resourceValueSD + ", extrinsicEventSD = " + extrinsicEventSD + "), and the interruption rate should be in the range [0,1] (here: " + interruptionRate + ").");

			this.resourceValueMean 				= new DecimalNumber(resourceValueMean);
			this.resourceValueSD 				= new DecimalNumber(resourceValueSD, DecimalNumber.ZERO, DecimalNumber.POSITIVE_INFINITY, false);
			this.resourceValueDistributionType 	= resourceValueDistributionType;
			this.extrinsicEventMean 			= new DecimalNumber(extrinsicEventMean);
			this.extrinsicEventSD 				= new DecimalNumber(extrinsicEventSD,DecimalNumber.ZERO, DecimalNumber.POSITIVE_INFINITY,false);
			this.extrinsicEventDistributionType = extrinsicEventDistributionType;
			this.interruptionRate 				= new DecimalNumber(interruptionRate,DecimalNumber.ZERO, DecimalNumber.ONE,false);
		} catch (Exception e) { ObserverManager.notifyObserversOfError(e);}
	}

	public abstract void update();
	
	public abstract Environment toEnvironment() throws MisspecifiedException ;

	
	/////////////////////////////////////////////////////////////////////////////////////
	////////////////////////// 	GETTERS AND SETTERS 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	public DecimalNumber getResourceValueMean() {
		return resourceValueMean;
	}

	public DecimalNumber getResourceValueSD() {
		return resourceValueSD;
	}

	public ValueDistributionType getResourceValueDistributionType() {
		return resourceValueDistributionType;
	}

	public DecimalNumber getExtrinsicEventMean() {
		return extrinsicEventMean;
	}

	public DecimalNumber getExtrinsicEventSD() {
		return extrinsicEventSD;
	}

	public ValueDistributionType getExtrinsicEventDistributionType() {
		return extrinsicEventDistributionType;
	}

	public DecimalNumber getInterruptionRate() {
		return interruptionRate;
	}

	public void setResourceValueMean(DecimalNumber resourceValueMean) {
		this.resourceValueMean = resourceValueMean;
	}

	public void setResourceValueSD(DecimalNumber resourceValueSD) {
		this.resourceValueSD = resourceValueSD;
	}

	public void setResourceValueDistributionType(ValueDistributionType resourceValueDistributionType) {
		this.resourceValueDistributionType = resourceValueDistributionType;
	}

	public void setExtrinsicEventMean(DecimalNumber extrinsicEventMean) {
		this.extrinsicEventMean = extrinsicEventMean;
	}

	public void setExtrinsicEventSD(DecimalNumber extrinsicEventSD) {
		this.extrinsicEventSD = extrinsicEventSD;
	}

	public void setExtrinsicEventDistributionType(ValueDistributionType extrinsicEventDistributionType) {
		this.extrinsicEventDistributionType = extrinsicEventDistributionType;
	}

	public void setInterruptionRate(DecimalNumber interruptionRate) {
		this.interruptionRate = interruptionRate;
	}



	// Implementations of the JavaFXHelper.TableViewObject interface
	/** 
	 * I am not a big fan of Java's Reflection. Hence, this workaround: get the value of the specified field.
	 * @param fieldName
	 * @return
	 */
	public Object getFieldValue(String fieldName)
	{
		switch(fieldName)
		{
		case "resourceValueMean": 					return this.resourceValueMean; 
		case "resourceValueSD": 					return this.resourceValueSD;
		case "resourceValueDistributionType": 		return this.resourceValueDistributionType;

		case "extrinsicEventMean": 					return this.extrinsicEventMean;
		case "extrinsicEventSD": 					return this.extrinsicEventSD;
		case "extrinsicEventDistributionType":		return this.extrinsicEventDistributionType;

		case "interruptionRate": 					return this.interruptionRate;
		}
		ObserverManager.notifyObserversOfError("Error during modification of environment builder", "An exception occurred while trying set a field in an environment builder");
		return null;
	}
	
	/** 
	 * I am not a big fan of Java's Reflection. Hence, this workaround: get the value of the specified field.
	 * @param fieldName
	 * @return
	 */
	public void setFieldValue(String fieldName, Object value)
	{
		try {
			switch(fieldName)
			{
			case "resourceValueMean": 					this.setResourceValueMean( ((DecimalNumber) value).doubleValue()); 						break;
			case "resourceValueSD": 					this.setResourceValueSD(((DecimalNumber) value).doubleValue());							break;
			case "resourceValueDistributionType": 		this.setResourceValueDistributionType((ValueDistributionType) value);	break;

			case "extrinsicEventMean": 					this.setExtrinsicEventMean(((DecimalNumber) value).doubleValue());							break;
			case "extrinsicEventSD": 					this.setExtrinsicEventSD(((DecimalNumber) value).doubleValue());							break;
			case "extrinsicEventDistributionType": 		this.setExtrinsicEventDistributionType((ValueDistributionType) value);	break;

			case "interruptionRate": 					this.setInterruptionRate(((DecimalNumber) value).doubleValue());							break;
			}
			
			update();
		} catch (Exception e) {ObserverManager.notifyObserversOfError("Error during modification of environment builder", "An exception occurred while trying set a field in an environment builder. See details.", e);}

	}


	/**
	 * Sets the mean. Does nothing if the distribution type is manual.
	 * Throws an exception if the new value is not within the range specified by the 	
	 * MDP builder.
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */
	public void setResourceValueMean(double newValue) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		if(resourceValueDistributionType != ValueDistributionType.Manual) {
			this.resourceValueMean.set(newValue);
			update();
		}
	}

	/**
	 * Sets the standard deviation. Does nothing if the distribution type is manual.
	 * Throws an exception if the new value is not within [0,infinity]
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */
	public void setResourceValueSD(double newValue) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		if(resourceValueDistributionType != ValueDistributionType.Manual) {
			this.resourceValueSD.set(newValue);
			update();
		}
	}



	/**
	 * Sets the mean. Does nothing if the distribution type is manual.
	 * Throws an exception if the new value is not within the range specified by the 
	 * MDP builder.
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */
	public void setExtrinsicEventMean(double newValue) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		if(extrinsicEventDistributionType != ValueDistributionType.Manual) {
			this.extrinsicEventMean.set(newValue);
		}
	}

	/**
		/**
	 * Sets the standard deviation. Does nothing if the distribution type is manual.
	 * Throws an exception if the new value is not within [0,infinity]
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */
	public void setExtrinsicEventSD(double newValue) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		if(resourceValueDistributionType != ValueDistributionType.Manual) {
			this.resourceValueSD.set(newValue);
			update();
		}
	}


	/**
	 * Sets the standard deviation. 
	 * Throws an exception if the new value is not within [0,infinity]
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws UnsupportedOperationException 
	 */

	public void setInterruptionRate(double newValue) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		this.interruptionRate.set(newValue);
		update();

	}


}
