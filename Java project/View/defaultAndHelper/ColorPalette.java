package defaultAndHelper;

import java.util.ArrayList;

import agent.Action;
import agent.ValueIterator.DecisionEdge;
import javafx.scene.paint.Color;

public class ColorPalette {
	
	public static ArrayList<Color> cueColors = new ArrayList<>();
	
	public final static Color		policyPlotActionColorSample = Color.rgb(5,132,211); //5,110,176
	public final static Color		policyPlotActionColorEating = Color.rgb(65,171,93);
	public final static Color 		policyPlotActionColorDiscarding = Color.rgb(217,95,2);
	public final static Color		policyPlotActionColorDead = Color.WHITE;
	
	public final static Color 		policyPlotCenterLineColor = Color.rgb(50, 50, 50, 0.7);
	public final static Color		policyPlotDescriptionTextColor = Color.BLACK;
	public final static Color 		policyPlotProportionFontColor = Color.BLACK;
	
	public final static Color		policyPlotTerminalState = Color.WHITE;
	public final static Color		policyPlotColorInactive = Color.rgb(200,200,200);
	
	public final static Color		policyPlotSelectedNodeCorona = Color.rgb(190,49,26,1);
	public final static Color		policyPlotBackgroundColor = Color.WHITE;
	
	public final static Color		policyPlotStateNodeToActionNodeEdgeColor = Color.BLACK;
	
	public final static Color 		resourceValuesColorFill = Color.rgb(173,221,142);
	public final static Color		resourceValuesColorLine = Color.rgb(65,171,93);
	public final static double		resourceValueColorMaximumOpacity = 0.9;
	
	public final static Color 		extrinsicEventValueColorFill = Color.rgb(237,79,42, 0.25);
	public final static Color		extrinsicEventValueColorLine	= Color.rgb(217,56,36, 0.9);
	
	public final static Color		policyPlotDecisionEdgeColor = Color.BURLYWOOD;
	
	public final static Color		getColor (Action a) {
		if (a == Action.SAMPLE) 	return policyPlotActionColorSample;
		if (a == Action.EAT)		return policyPlotActionColorEating;
		if (a == Action.DISCARD)	return policyPlotActionColorDiscarding;
		return policyPlotActionColorDead;
	}
	
	
	public static double	hueChangeWeight = 1.5;
	


	/**
	 * Returns the RGBA for c. RGB values are in range 0-255, A (opacity) is in range 0-1. 
	 * @param c
	 * @return
	 */
	public static double[] colorToRGBA(Color c)
	{
		return new double[]{(int)c.getRed()*255, (int) c.getGreen()*255, (int)c.getBlue()*255, c.getOpacity()};
	}

	/**
	 * Returns the RGB for c. RGB values are in range 0-255, A (opacity) is implicitly 1.
	 * @param c
	 * @return
	 */
	public static int[] colorToRGB(Color c)
	{
		return new int[]{(int)(c.getRed()*255), (int) (c.getGreen()*255), (int) (c.getBlue()*255)};
	}
	
	public static Color addOpacity(Color c, double opacity)
	{
		int[] rgb = colorToRGB(c);
		return Color.rgb(rgb[0], rgb[1], rgb[2], opacity);
	}
	
	/**
	 * Returns a string of form "rgba(x,x,x,x)"
	 * @param c
	 * @return
	 */
	public static String toFXMLString(Color c)
	{
		int[] rgb = colorToRGB(c);
		return "rgba(" + rgb[0] + "," + rgb[1] + "," + rgb[2] + "," + c.getOpacity() + ")"; 
	}

	/**
	 * Set the colors used to denote the cue labels. Note that these color have an opacity attached that is not 0 (rather, is depends on the extremeness of the 
	 * cue label). 
	 * @param numberOfLabels
	 */
	public static void setCueColors(int numberOfLabels)
	{
		ColorPalette.cueColors.removeAll(ColorPalette.cueColors);
		for (int cue = 0; cue < numberOfLabels; cue++)
		{
			int red = 255 - (255 * cue/(numberOfLabels-1));
			int green = 0;//((cue%2)==0 && cue>5)? 125:0;
			int blue = 255 - red;
			double opacity = (0.15+ Math.abs(cue-(numberOfLabels-1)/2.0)/((numberOfLabels-1)*2));
			ColorPalette.cueColors.add(Color.rgb(red, green, blue, opacity));
		}
	}
}
