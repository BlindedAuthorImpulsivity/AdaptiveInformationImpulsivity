package agent.ValueIterator;

import java.text.DecimalFormat;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import helper.Helper;

/**
 * A class to store edges of a decision tree in. Note that the proportion here is a relative proportion: what is the
 * proportion of all children ending up from the parent node to the child node.
 * 
 * The proportion is semi-mutable - it has the 'immutable' flag, but can be changed by invoking
 * changeProportion();
 */
public class DecisionEdge    {

	public DecisionNode 	parent;
	public       DecisionNode 	child;
	public       DecimalNumber  proportion;
	

	public DecisionEdge(DecisionNode parent, DecisionNode child, DecimalNumber proportion) throws IllegalRangeException
	{
		this.parent = parent;
		this.child = child;
		this.proportion = proportion;
		
	}
	
	/** Set the proportion of this edge. Normally this field is immutable. However,
	 * this function temporarily makes it mutable. It is highly recommended
	 * to not change this mutability anywhere else - allowing changes only in this 
	 * way ensure that no incidental changes occur.
	 */
	public void setProportion(DecimalNumber newProportion) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException {
		this.proportion.setImmutable(false);
		this.proportion.set(newProportion);
		this.proportion.setImmutable(true);
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("<" + Helper.arrayToString(parent.cueSet) + "> connects to <" + Helper.arrayToString(child.cueSet) + "> with a relative proportion of " + proportion);
		return sb.toString();
		
	}
	
	public String toDataFrameRow()
	{
		StringBuilder sb = new StringBuilder();
		DecimalFormat df = new DecimalFormat("0.##########");
		sb.append("c(");
		sb.append(parent.cuesSampled+ ",");
		for (int i = 0; i < parent.cueSet.length; i++) { sb.append(parent.cueSet[i]+","); }

		sb.append(child.cuesSampled+ ",");
		for (int i = 0; i < child.cueSet.length; i++) { sb.append(child.cueSet[i]+","); }
		
		sb.append(df.format(proportion) + ")");
		
		return sb.toString();
	}
	

}
