package markovDecisionProcess;

import java.math.BigDecimal;
import java.text.DecimalFormat;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.ComputationException;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import defaults.Defaults;
import helper.Helper;

public class MarkovDecisionProcessBuilder {
	
	/////////////		STATE SPACE PARAMETERS
	public double					BUDGET_MAXIMUM,
									BUDGET_STEP,
									VALUE_MAXIMUM,
									VALUE_STEP;
	public void updateBUDGET_STEP () {
		BUDGET_STEP= Helper.gcd(new BigDecimal(""+VALUE_STEP, Helper.mc), new BigDecimal("" +COST_OF_SAMPLING, Helper.mc),false,0,10000).doubleValue();										
		if (BUDGET_STEP < 0.001) BUDGET_STEP = 0.001;
	}


	/////////////		   	TASK PARAMETERS
	public double      	   			COST_OF_SAMPLING;
	public int         	   			MAXIMUM_CUES;
	public boolean					COMPOUND_INTERRUPTION_RATE;
	public DecimalNumberMatrix 		CUE_EMISSION_MATRIX;
	public double					CUE_EMISSION_ARGUMENT;
	public CueEmissionType			CUE_EMISSION_TYPE;
	
	/////////////			FITNESS PARAMETERS
	public String 					FITNESS_FUNCTION;

	// Parameters required for the RL algorithms
	public double 					DISCOUNT_RATE;
	
	
	public void setDefaults(Defaults defaults) throws ComputationException, IllegalRangeException, IllegalScaleException
	{
		this.BUDGET_MAXIMUM = defaults.maximumBudget;
		this.VALUE_MAXIMUM = defaults.valueMaximum;
		this.VALUE_STEP = defaults.valueStep;
		this.COST_OF_SAMPLING= defaults.costOfSampling;
		this.MAXIMUM_CUES = defaults.maximumCues;

		this.FITNESS_FUNCTION = defaults.fitnessFunction;
		this.DISCOUNT_RATE = defaults.discountRate;
		this.updateBUDGET_STEP();
		
		this.CUE_EMISSION_TYPE = defaults.cueEmissionType;
		this.CUE_EMISSION_ARGUMENT = defaults.cueEmissionArgument;
		
		if (defaults.cueEmissionMatrix  == null)
			CUE_EMISSION_MATRIX  = new DecimalNumberMatrix(0, defaults.cueLabels.length+1);
		else
			CUE_EMISSION_MATRIX = defaults.cueEmissionMatrix.clone();
		
		CUE_EMISSION_MATRIX .setColumnName(0,"Resource value");
		for (int s = 0; s < defaults.cueLabels.length; s++)
			CUE_EMISSION_MATRIX .setColumnName(s+1, defaults.cueLabels[s]);
		
		if (CUE_EMISSION_MATRIX .nrow()==0)
			CueEmissionMatrixBuilder.setCueEmissionMatrix(defaults.cueEmissionType, defaults.cueEmissionArgument, this);
	}
	
	//TODO: comment
	public DecimalNumberArray getRangeOfValues() throws IllegalRangeException, IllegalScaleException {
		DecimalNumber valueMaximum = new DecimalNumber(VALUE_MAXIMUM, true);
		DecimalNumber valueMinimum = new DecimalNumber(-VALUE_MAXIMUM, true);
		DecimalNumber valueStep    = new DecimalNumber(VALUE_STEP, true);
		DecimalNumberArray array = DecimalNumberArray.sequence( valueMinimum, valueMaximum, valueStep);
		for (DecimalNumber dn: array)
			dn.setRange(-VALUE_MAXIMUM, VALUE_MAXIMUM);
		return array;
	}
	
	public String toString() {
		DecimalFormat df = new DecimalFormat("#.####");
		StringBuilder sb = new StringBuilder();
		sb.append("State space parameters");
		sb.append("\nMaximum resource/event value:        " + df.format(this.VALUE_MAXIMUM));
		sb.append("\nStep size resource/event value:      " + df.format(this.VALUE_STEP));
		sb.append("\nMaximum budget:                      " + df.format(this.BUDGET_MAXIMUM));
		sb.append("\nStep size budget:                    " + df.format(this.BUDGET_STEP));
		
		sb.append("\n\nTask parameters");
		sb.append("\nMaximum number of cues:              " + this.MAXIMUM_CUES);
		sb.append("\nCost of sampling one cue:            " + this.COST_OF_SAMPLING);
		sb.append("\nUsing compound interruption rate:    " + this.COMPOUND_INTERRUPTION_RATE);
		sb.append("\n\nCue emission matrix distribution:    " + this.CUE_EMISSION_TYPE);
		sb.append("\nCue emission matrix argument:        " + this.CUE_EMISSION_ARGUMENT);
		sb.append("\nCue probability matrix:              " + this.CUE_EMISSION_MATRIX);
		
		sb.append("\n\nTask parameters");
		sb.append("\nTerminal budget to fitness function: " + this.FITNESS_FUNCTION);
		sb.append("\nDiscount rate (lambda):              " + df.format(this.DISCOUNT_RATE));
		return sb.toString();
	}


}
