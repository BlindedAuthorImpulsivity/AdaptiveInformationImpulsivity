package ValueIteratorPolicyPlot;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import ValueIteratorPolicyPlot.PolicyPlot_ValueIterator.ConstructionPhase;
import ValueIteratorPolicyPlot.PolicyPlot_ValueIterator.ShowingCycle;
import agent.ValueIterator.DecisionNode;
import defaultAndHelper.ColorPalette;
import environment.Environment;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.TextAlignment;
import markovDecisionProcess.MarkovDecisionProcess;

/**
 * A private class that handles all the drawing of the decision nodes themselves.
 * Because we want to be able to track mouse click events on this level (i.e., show
 * a DecisionNodeInformationPane, and because we want to draw the node, this class extends Canvas.
 *
 */
class DecisionNodeCanvas extends Canvas implements EventHandler<MouseEvent>
{
	private final AnchorPane scrollPane;
	public final AnchorPane plottingPane;
	public final DecisionNode dn;
	public final MarkovDecisionProcess mdp;
	public final Environment environment;
	public final ShowingCycle showingCycle;
	public final ConstructionPhase cp;
	private final GraphicsContext gcToDrawWith;
	
	// The radius is the TOTAL radius (inner node + shadow gradient that becomes visible when the node is selected)
	// The inner radius is the radius of the actual coloring (the 'best action' area)
	// The innerRadiusOffset is the difference between the two.
	public final double x,y, radius, innerRadius, innerRadiusOffset;
	public final boolean scaleNodes;

	public DecisionNodeInformationPane informationPane;
	
	/**
	 * Create a canvas on which the node is drawn
	 * @param scrollPane			The anchor pane on which the node's information screen can be drawn - the right hand side of the frame
	 * @param plottingPane			The anchor pane on which the node itself will be drawn
	 * @param dn					The node to plot
	 * @param mdp
	 * @param environment
	 * @param x
	 * @param y
	 * @param outcomeMode
	 */
	public DecisionNodeCanvas(
			AnchorPane scrollPane, 
			AnchorPane plottingPane, 
			DecisionNode dn, 
			MarkovDecisionProcess mdp,
			Environment environment, 
			double x, 
			double y, 
			ShowingCycle sc,
			ConstructionPhase cp,
			boolean scaleNodes,
			GraphicsContext gcToDrawWith)
	{
		this.scrollPane = scrollPane;
		this.plottingPane = plottingPane;
		this.environment = environment;
		this.dn = dn;
		this.mdp = mdp;
		this.x = x;
		this.y= y;
		this.showingCycle = sc;
		this.cp = cp;
		this.scaleNodes = scaleNodes;
		this.gcToDrawWith = gcToDrawWith;
		
		radius = PlottingParameters.proportionToRadius(dn.proportion.doubleValue(), scaleNodes);
		innerRadius = PlottingParameters.innerRadiusFractionStateNode * radius;
		innerRadiusOffset = radius - innerRadius;
		this.setWidth(radius*2);
		this.setHeight(radius*2);	

		// Place the node on the anchor pane. Note that the anchor pane coordinates denote the position of the upper left corner. 
		// Since we want the node to be centered on its center, we have to deduct the radius from the x and y coordinates
		plottingPane.getChildren().add(this);
		AnchorPane.setTopAnchor(this, y-radius);
		AnchorPane.setLeftAnchor(this, x-radius);
		
		// Set the mouse event handler
		this.setOnMouseClicked(this);

	}
	
	/**
	 * Draw the decision node.
	 */
	public void draw(boolean selected)
	{
		gcToDrawWith.translate(x-getWidth()/2, y-getHeight()/2);
		gcToDrawWith.setLineDashes(null);
		
		//Draw the background oval
		gcToDrawWith.setFill(ColorPalette.policyPlotBackgroundColor);
		gcToDrawWith.fillOval(
				0, 
				0, 
				this.getWidth(), 
				this.getHeight());


		// Set the corona around the node (if the node is selected)
		if (selected) {
			
			// Set the radiant for the corona
			ArrayList<Stop >stops = new ArrayList<>();
			stops.add(new Stop(0.0, ColorPalette.policyPlotBackgroundColor));
			stops.add(new Stop(PlottingParameters.innerRadiusFractionStateNode-0.01, ColorPalette.policyPlotBackgroundColor));
			stops.add(new Stop(PlottingParameters.innerRadiusFractionStateNode, ColorPalette.policyPlotSelectedNodeCorona));
			stops.add(new Stop(1, Color.rgb(0, 0, 0, 0)));
			
			gcToDrawWith.setFill( new RadialGradient(
					0, 		//Focus angle
					0, 		//Focus distance
					0.5, 	//centerX
					0.5, 	//CenterY
					0.5, 		//Radius
					true, 	//Proportional
					CycleMethod.NO_CYCLE, 
					stops));
			
			gcToDrawWith.fillRect(0, 0, this.getWidth(), this.getHeight());
		}
		
		// Draw the black outer line
		gcToDrawWith.setLineWidth(radius/10);
		gcToDrawWith.setStroke(Color.BLACK);
		gcToDrawWith.strokeOval(
				innerRadiusOffset, 		// X start (left)
				innerRadiusOffset, 		// Y start (top)
				innerRadius*2, 			// width
				innerRadius*2);			// height
		
		// Set the color of the oval in the center that reflects the best action of the node
		// If the decision node has only one best action: fill the node with the best action
		if (dn.bestAction.size()==1) 
			gcToDrawWith.setFill(ColorPalette.getColor(dn.bestAction.get(0)));
		
		
		// If the decision node has multiple best actions: fill the node with a linear gradient showing all best-action colors
		if (dn.bestAction.size()>1) {
			double stepSize = 1.0/dn.bestAction.size();
			ArrayList<Stop> stops = new ArrayList<>();
			
			for (int i = 0; i < dn.bestAction.size(); i ++)
			{
				Color c = ColorPalette.getColor(dn.bestAction.get(i));
				stops.add(new Stop(i*stepSize, c));
				stops.add(new Stop(i*stepSize + stepSize-0.01,c));
			}
			
			// Note that if the sampling action is in the bestAction list, it is always the first index.
			// For esthetic reasons, I like the sampling action color to be at the bottom (since the outgoing
			// edges start there).
			gcToDrawWith.setFill(new LinearGradient(0,1,0,0.5,true, CycleMethod.REPEAT,stops));
		}
		
		// Draw the colored oval in the center
		gcToDrawWith.fillOval(
				innerRadiusOffset, 		// X start (left)
				innerRadiusOffset, 		// Y start (top)
				innerRadius*2, 			// width
				innerRadius*2);			// height
		
		// Draw the black outer line
		gcToDrawWith.setLineWidth(radius/20);
		gcToDrawWith.setStroke(Color.BLACK);
		gcToDrawWith.strokeOval(
				innerRadiusOffset, 		// X start (left)
				innerRadiusOffset, 		// Y start (top)
				innerRadius*2, 			// width
				innerRadius*2);			// height
		
		// Draw the proportion text in the center of the node if the node is NOT scaled according to proportion
		if (!scaleNodes) {
			gcToDrawWith.setFont(PlottingParameters.nodeStateProportionFont);
			gcToDrawWith.setTextAlign(TextAlignment.CENTER);
			gcToDrawWith.setTextBaseline(VPos.CENTER);
			gcToDrawWith.setFill(ColorPalette.policyPlotProportionFontColor);
			gcToDrawWith.fillText(formatPercentage(), 
		            Math.round(getWidth()  / 2), 
		            Math.round(getHeight() / 2));
		}
	
		gcToDrawWith.translate(-(x-getWidth()/2), -(y-getHeight()/2));
	}
	
	public void destroy() {
		this.informationPane.destroy();
		this.informationPane= null;
	}
	
	/** Returns the text to print in the center of the node if the node is NOT scaled according to proportion.
	 * This values has the following format:
	 * 
	 * "x%" if x> 10%
	 * "x.x%" if x > 1% and x < 10%
	 * ".xx%" if x > 0.01% and x < 1%
	 * ">.01%" if x <= .01%
	 * 
	 * Returns an empty string if the constuctionPhase is the forward pass (since there is no notion of proportion here yet)
	 * @return
	 */
	private String formatPercentage() {
		if (cp == ConstructionPhase.FORWARD_PASS)
			return "";
		
		double percentage = this.dn.proportion.doubleValue() * 100;
		
		if (percentage < 0.01)
			return "<.01%";
		
		if (percentage < 1) {
			DecimalFormat df = new DecimalFormat(".00");
			return df.format(percentage) + "%";
		}
		
		if (percentage < 10) {
			DecimalFormat df = new DecimalFormat("0.0");
			return df.format(percentage) + "%";
		}
		
		return Math.round(percentage) + "%";
		
	}
	
	@Override
	public void handle(MouseEvent event) {
		if (event.getButton() == MouseButton.PRIMARY)
		{
			// Check if an informationPane already exists. If not, construct one
			if (informationPane == null)
			{
				// The DecisionNodeCanvas x and y coordinates refer to the coordinates inside the
				// anchorPanePlotting. The x and y coordinates of the InformationPane refer to the 
				// x and y coordinates in the scrollPane. Since the scrollPane is a parent of the
				// anchor pane plotting, it is always larger. In addition, the plottingPane is always
				// centered in the scrollPane. Thus, to find the correct x coordinates for the
				// informationPane, we need to add halve the difference in size of the scrollPane and 
				// the plottingPane to the x coordinate of the DecisionNodeCanvas
				// of the DecisionNodeCanvas 
				double differenceX = scrollPane.getWidth()-plottingPane.getWidth();
				double informationPaneX = this.x + differenceX/2;
				
				// There is another caveat: if the informationPane will cross the width of the scrollPane,
				// the scrollPane will adjust its width to incorporate the informationPane. This is 
				// a behaviour that I want. To prevent it, the x position should not exceed 
				/// (width scrollPane - width informationPane)
				if (informationPaneX > scrollPane.getWidth() - PlottingParameters.informationPaneWidth)
					informationPaneX = scrollPane.getWidth() - PlottingParameters.informationPaneWidth;
			
				// The y coordinate of the informationPane on the scrollPane is difficult to estimate: 
				// this would require finding out what the y position of the DecisionNodeCanvas on the
				// plottingPane is, and add to that the position of the plotting pane on the scrollPane.
				// The latter one is difficult to estimate, since it requires taking into account the
				// height of all other objects in the scrollPane. Rather than doing this, I just
				// determine the y position of the scrollPane on the screen, the y position of the
				// mouse click on the screen, and figure out what the y position of the mouse is on the
				// scrollPane.
				Bounds bounds = scrollPane.getBoundsInLocal();
		        Bounds screenBounds = scrollPane.localToScreen(bounds);
		        double scrollY =  screenBounds.getMinY();
		  		double mouseY = event.getScreenY();
		  		double informationPaneY = mouseY-scrollY;
		  		
				try {informationPane = new DecisionNodeInformationPane(
						scrollPane, 
						dn, 
						this, 
						mdp, 
						environment,
						informationPaneX, 
						informationPaneY, 
						cp);} catch (IOException e) {e.printStackTrace();}

			}

			if (informationPane.isVisible())
			{
				informationPane.hide();
				this.draw(false);
			}
			else
			{
				informationPane.draw();
				this.draw(true);
			}
		}
	}
	

}
