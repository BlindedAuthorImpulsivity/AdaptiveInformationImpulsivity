package ValueIteratorPolicyPlot;

import java.text.DecimalFormat;

import agent.ValueIterator.DecisionEdge;
import decimalNumber.DecimalNumber;
import defaultAndHelper.ColorPalette;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class PlottingParameters {
	
	public final static double minimumWidthPerColumn = 100;
	
	// Each row in the plot consists of 3 parts: first the state segment (where the state nodes are plotted), then the action segment (where the actions nodes are plotted) and then a spacer
	public final static double nodeRowHeightState = 40;
	public final static double nodeRowHeightAction = 40;
	public final static double nodeRowHeightSpacer = 10;
	public final static double nodeActionDotSize = 10;
	public final static double nodeTerminalSize = 12;
	public final static double nodeTerminalBorderWidth = 1.5;
	
	public final static double centerLineWidth = 1;
	public final static double descriptionTextSize = 12;
	public final static Font nodeStateProportionFont = Font.font("Courier New", 12);
	public final static Font nodeActionOutcomeFont = Font.font("Courier New", 10);
	public final static int nodeActionOutcomeFontSignificantDigits = 1;
	
	public final static double descriptionTextX = 20.5;
	public final static double descriptionTextY	= 20.5;
	public final static int descriptionTextSignificantDigits = 2;
	
	public final static double edgeStateToActionSize = 3;
	public final static double edgeActionToStateSizeMaximum = 6;
	public final static double edgeActionToStateSizeMinimum = 1;
	public final static double edgeActionToStateSizeUnscaled = 4;
	
	public final static double innerRadiusFractionActionNode = 0.8;
	public final static double stateNodeRadiusMinimum = 1;
	public final static double stateNodeRadiusUnscaled = 20;
	public final static double innerRadiusFractionStateNode = 0.85; // proportion of the node radius that is NOT dedicated to the padding
	
	public final static double informationPaneWidth = 350;
	public final static double informationPaneHeight = 350;
	public final static double informationPanePadding = 10; // absolute number of pixels used for the padding
	
	// Constants for the decision edges
	public final static double	hueOffset = 0.25;
	public final static double	hueChangeWeight = 1.5;
	
	public final static DecimalFormat df = new DecimalFormat("0.####");
	
	/** A formatter that adds a prefix " " to positive values */
	public static String format(double value)
	{
		String s = df.format(value);
		if (value >= 0)
			return " "+s;
		return s;
	}
	
	/** A formatter that adds a prefix " " to positive values */
	public static String format(DecimalNumber value)
	{
		if (value == null)
			return "N/A";
		return value.toString(4);
	}

	/** The proportion of a node determines its area (not radius). That is, if node A has a proportion of
	 * .50 and node B has a proportion of 0.25, then the area of node A is twice that of node B (the radius, however,
	 * is not twice as large). 
	 *  
	 * @param proportion
	 * @return
	 */
	public static double proportionToRadius(double proportion, boolean scaleNodes) {
		// If scaleNodes is false, return stateNodeRadiusUnscaled 
		if (!scaleNodes)
			return stateNodeRadiusUnscaled;
		
		// First, calculate the area of the largest possible node, i.e., the starting node with a proportion of 1:
		double maxRadius = (nodeRowHeightState/2);
		double maxArea = (Math.PI*Math.pow(maxRadius, 2));

		// Next, multiple the maximum area with the proportion:
		double scaledArea = maxArea*proportion;
		
		// And convert back again to the radius
		double scaledRadius = Math.sqrt(scaledArea/Math.PI);
	
		// Finally, make sure the scaledRadius is not smaller than the minimum radius
		return Math.max(stateNodeRadiusMinimum, scaledRadius);
		}
	
	
	/**
	 * Returns the color of the edges. This color is the edgeColor made darker for 
	 * high decision edge proportions, and lighter for low decision edge proportions;
	 * @param de
	 * @return
	 */
	public static Color 	getColor (DecisionEdge de){
		double red = ColorPalette.policyPlotDecisionEdgeColor.getRed();
		double green = ColorPalette.policyPlotDecisionEdgeColor.getGreen();
		double blue = ColorPalette.policyPlotDecisionEdgeColor.getBlue();
		
		double redProp = red / (red+green+blue);
		double greenProp = green / (red+green+blue);
		double blueProp = blue / (red + green + blue);

		double maxVal = redProp;
		if (Math.max(greenProp, blueProp) > maxVal) maxVal = Math.max(greenProp, blueProp);
		
		double maxFactor = 1.0/maxVal;

		double newRed = 	hueOffset + (maxFactor * redProp   * (1-de.proportion.doubleValue()) * hueChangeWeight) * (1-hueOffset);
		double newGreen = 	hueOffset + (maxFactor * greenProp * (1-de.proportion.doubleValue()) * hueChangeWeight) * (1-hueOffset);
		double newBlue = 	hueOffset + (maxFactor * blueProp  * (1-de.proportion.doubleValue()) * hueChangeWeight) * (1-hueOffset);
		
		int r = Math.min(255, (int) (newRed*255));
		int g = Math.min(255, (int) (newGreen*255));
		int b = Math.min(255, (int) (newBlue*255));
		
		return Color.rgb(r,g,b);
	}

}
