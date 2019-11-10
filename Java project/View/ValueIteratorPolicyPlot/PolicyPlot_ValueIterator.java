package ValueIteratorPolicyPlot;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import agent.Action;
import agent.ValueIterator.DecisionEdge;
import agent.ValueIterator.DecisionNode;
import agent.ValueIterator.DecisionTree;
import agent.ValueIterator.ValueIteratorOutput;
import agent.ValueIterator.ValueIteratorValueFunction;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import defaultAndHelper.ColorPalette;
import environment.Environment;
import estimationParameters.EstimationParameters;
import helper.Helper;
import helper.Helper.ImpossibleStateException;
import helper.Helper.InvalidProbabilityException;
import helper.Helper.InvalidProportionException;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;
import markovDecisionProcess.MarkovDecisionProcess;
import start.Model.InvalidFitnessInputException;
import staticManagers.ObserverManager;
import window.FrameController;
import window.interfacesAndAbstractions.AbstractPolicyPlot;

/** The anchor pane that is the ground layer of the AbstractPolicyPlot. This plot consists of four
 * steps (or layers):
 * 
 * Step 0: 					Create an anchor pane (this) that contains the whole plot, and create the tree to plot	
 * 								(Also, determine all <x,y> coordinates of the nodes/edges)
 * Step	1 (layer 1):		Create a Canvas object that spans the whole anchor pane (this) and draw the background on that anchor plot
 * Step 2 (layer 2):		Draw all the edges on the canvas created in step 1
 * Step	3 (layer 3):		Create a new Canvas for all nodes in the tree, and place these canvasses on the anchor of step 0
 * 
 * When redrawing, all Canvasses are removed and steps 1-3 followed.

 *
 */
public class PolicyPlot_ValueIterator extends AbstractPolicyPlot implements EventHandler<MouseEvent>{
	
	/** 
	 * An Enum that determines how far the tree should be constructed.
	 */
	public enum ConstructionPhase {	FORWARD_PASS, BACKWARD_PASS, FORWARD_PRUNING_PASS; }
	public final ConstructionPhase constructionPhase;
	
	/** 
	 * An Enum that determines whether the plot show the first cycle (i.e., optimizes fitness) or any other cycle (i.e., optimizes expected value).
	 */
	public enum ShowingCycle {	FITNESS, VALUE; }
	public final ShowingCycle showingCycle;
	
	
	// Fields read out or constructed from the ValueIteratorOutput
	private DecisionTree tree;
		
	private final MarkovDecisionProcess mdp;
	private final Environment environment;
	private final EstimationParameters estimationParameters;
	
	private final ValueIteratorValueFunction valueFunction;
	
	private final DecimalNumber startingBudget;
	// In layer 1 the background is drawn here, in layer 2 the edges are drawn here
	private			Canvas 	canvas; 
	
	private int		maxDepth;
	private 		ArrayList<ArrayList<DecisionNode>> nodesPerDepth;
	private 	  	ArrayList<DecisionNodeCanvas> nodesToDraw;
	private double[] horizontalUnits;  // Stores the horizontalUnit of plotting: the units in which the x-axis is measured (the ticks, or places of the node on the x-axis). It determines by how many nodes there are per row, for each row

	//private		  ArrayList<DecisionEdgeDrawer> edgesToDraw;
	
	private final boolean scaleEdges;
	private final boolean scaleNodes;
	private final boolean showUnusedEdges;
	private final boolean showCenterLine;
	private final boolean showDescriptionText;
	private final boolean printTerminalOutcomes;
	private final boolean showImmediateOutcomes;
	private final boolean showFutureOutcomes;
	
	private enum EdgeStyle { Normal, Inactive, NotDrawn};
	
	 /** @param plottingPane
	 * @param vioutput
	 * @param startingBudget
	 * @param pruneNodes
	 * @param maximizeFitness			What should the policy plot maximize: the expected value outcome of one more cycle, or the expected terminal fitness?
	 */
	public PolicyPlot_ValueIterator(
			FrameController frame,
			AnchorPane anchorPaneScrollPane,
			AnchorPane anchorPaneTitledPane,
			AnchorPane anchorPanePlotting,
			ValueIteratorOutput vioutput,
			
			DecimalNumber startingBudget, 
			ConstructionPhase constructionPhase, 
			ShowingCycle showingCycle,
			boolean scaleNodes,
			boolean scaleEdges,
			boolean showUnusedEdges,
			boolean showCenterLine,
			boolean showDescriptionText,
			boolean printTerminalOutcomes,
			boolean showImmediateOutcomes,
			boolean showFutureOutcomes) throws ImpossibleStateException, InvalidProportionException, NumberFormatException, InvalidFitnessInputException, REXPMismatchException, REngineException, InvalidProbabilityException
	{
		super(frame, anchorPaneScrollPane, anchorPaneTitledPane, anchorPanePlotting, vioutput);
		
		// Read out or construct these fields
		this.mdp= vioutput.mdp;
		this.environment =vioutput.environment;
		this.estimationParameters = vioutput.estimationParameters;
		this.valueFunction = (ValueIteratorValueFunction)vioutput.finalValueFunction;
		this.constructionPhase = constructionPhase;
		this.showingCycle = showingCycle;
		this.startingBudget = startingBudget;
		this.scaleEdges = scaleEdges;
		this.scaleNodes = scaleNodes;
		this.showUnusedEdges = showUnusedEdges;
		this.showCenterLine = showCenterLine;
		this.showDescriptionText = showDescriptionText;
		this.printTerminalOutcomes = printTerminalOutcomes;
		this.showImmediateOutcomes=showImmediateOutcomes;
		this.showFutureOutcomes=showFutureOutcomes;
		// Inform the user
		String cps = constructionPhase.name().replace("_", " ").toLowerCase();
		String scs =  showingCycle.name().toLowerCase();
		ObserverManager.makeToast("Drawing a " + cps + " policy plot for a Value Iterator agent that maximizes " + scs + " with an initial endowment of " + startingBudget.toString(2) );
		
		// Set the mouse listener. If the user presses the secondary button, close all open information panes
		this.setOnMouseClicked(this);
		
		// Set all other variables
		try {			initialize();} catch (Exception e) { ObserverManager.notifyObserversOfError(e); 	}
		draw();
	}

	/** For the valueIterator plot this function call will construct the decision tree. Feedback to the user is provided. 
	 * @throws InvalidProportionException 
	 * @throws ImpossibleStateException 
	 * @throws IllegalRangeException */
	@Override
	public void initialize() throws Exception {
		
		// Next, construct the tree
		tree = new DecisionTree(mdp, environment, estimationParameters, ((ValueIteratorOutput)output).posteriorProbabilityTable, startingBudget);
		
		// The forward pass is always done
		tree.forwardPass();

		// The backwards pass is done only if the construction phase is set to backward pass or forward pruning
		if (this.constructionPhase== ConstructionPhase.BACKWARD_PASS || this.constructionPhase == ConstructionPhase.FORWARD_PRUNING_PASS)
			tree.backwardPass(this.showingCycle==ShowingCycle.FITNESS, valueFunction, true );

		// Determine the depth of the tree
		maxDepth = tree.nodes.get(tree.nodes.size()-1).cuesSampled;
		
		// From the backward pass, separate all the nodes according to their depth (i.e., the number of cues sampled)
		nodesPerDepth = new ArrayList<>();
		for (int depth = 0; depth <= maxDepth ; depth ++)
			nodesPerDepth.add(new ArrayList<DecisionNode>());

		for (DecisionNode dn: tree.nodes)
			nodesPerDepth.get(dn.cuesSampled).add(dn);

		// The forward pruning pass is done only if the construction phase is set to forward pruning
		if (constructionPhase == ConstructionPhase.FORWARD_PRUNING_PASS)
			tree.forwardPruningPass();

		// If the plot is placed in a titled pane, we need to update the height of this pane
		if (anchorPaneTitledPane != null) {
			
			// Set the height of the plot: the height of the plot is (PlottingParameters.plotHeightPerRow * maximumCuesSampled).
			// To change the plot height, we have to change the height of the anchorPaneTitledPane. The height of this pane
			// should be the plot height plus all the other rows in this titled pane.
			double heightOfOtherRows = anchorPaneTitledPane.getHeight() - anchorPanePlotting.getHeight();
			double newHeightOfPlot = PlottingParameters.nodeRowHeightState+(PlottingParameters.nodeRowHeightAction+PlottingParameters.nodeRowHeightState + PlottingParameters.nodeRowHeightSpacer) * (maxDepth+1);
			anchorPaneTitledPane.setPrefHeight(newHeightOfPlot + heightOfOtherRows + 50);
			anchorPanePlotting.setPrefHeight(newHeightOfPlot);
			this.setHeight(newHeightOfPlot);

			// Set the width
			double minimumWidth = nodesPerDepth.get(nodesPerDepth.size()-1).size() * PlottingParameters.minimumWidthPerColumn ;
			anchorPaneTitledPane.widthProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					anchorPanePlotting.setMinWidth(newValue.doubleValue()-20);
					setWidth(newValue.doubleValue());
				}

			});
		}
		
		// Attach this anchorPane to the anchorPanePlotting, and make this pane the same dimensions as anchorPanePlotting
		anchorPanePlotting.getChildren().add(this);
		AnchorPane.setTopAnchor(this, 0.0);
		AnchorPane.setLeftAnchor(this, 0.0);
		
		
		draw();
	}
	
	
	@Override
	public void draw() {
		this.setWidth(anchorPanePlotting.getWidth());

		// Layer 1: create the background canvas (which fills this anchor pane) (Also, determine all <x,y> coordinates of the nodes/edges)
		canvas = new Canvas(this.getWidth(), this.getHeight());
		this.getChildren().add(canvas);
		AnchorPane.setTopAnchor(canvas, 0.0);
		AnchorPane.setLeftAnchor(canvas, 0.0);
		
		// Set the background of the canvas - draw a rectangle that fills the whole space
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.setFill(ColorPalette.policyPlotBackgroundColor);
		gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    
		// Determine the verticalUnit of plotting: how big should the rows be?
		double verticalUnit = PlottingParameters.nodeRowHeightState+PlottingParameters.nodeRowHeightAction + PlottingParameters.nodeRowHeightSpacer;

		// Determine the horizontalUnit of plotting: how many columns does each row have?
		// these are the units in which the x-axis is measured (the ticks, or places of the node on the x-axis).
		horizontalUnits = new double[nodesPerDepth.size()];
		for (int d = 0; d < horizontalUnits.length; d++)
			horizontalUnits[d] =  this.getWidth()/(nodesPerDepth.get(d).size());

		// Layer 1C: background line positive/negative cues
		if (showCenterLine)
		{
			gc.setStroke(ColorPalette.policyPlotCenterLineColor);
			gc.setLineWidth(PlottingParameters.centerLineWidth);
			gc.setLineDashes(8,8);
			gc.strokeLine(this.getWidth()/2, 0, this.getWidth()/2, this.getHeight());
		}
		
		// Layer 1D: description text on top left corner
		if (showDescriptionText) {
			gc.setFill(ColorPalette.policyPlotDescriptionTextColor);
			gc.setFont(Font.font("Courier New", PlottingParameters.descriptionTextSize));
			StringBuilder sb = new StringBuilder();

			sb.append("Initial endowment:    " + tree.root.budget.toSignSpacedString(PlottingParameters.descriptionTextSignificantDigits) + "    ");
			sb.append("Interruption rate:    " + environment.interruptionRate.toSignSpacedString(PlottingParameters.descriptionTextSignificantDigits) + "\n");
			
			sb.append("Mean resource value:  " + environment.resourceValueMean.toSignSpacedString(PlottingParameters.descriptionTextSignificantDigits) + "    ");			
			sb.append("SD resource value:    " + environment.resourceValueSD.toSignSpacedString(PlottingParameters.descriptionTextSignificantDigits) + "\n");
			
			sb.append("Mean extrinsic value: " + environment.extrinsicEventMean.toSignSpacedString(PlottingParameters.descriptionTextSignificantDigits) + "    ");
			sb.append("SD extrinsic value:   " + environment.extrinsicEventSD.toSignSpacedString(PlottingParameters.descriptionTextSignificantDigits) );
			
			gc.fillText(sb.toString(), PlottingParameters.descriptionTextX, PlottingParameters.descriptionTextY);
		}
		
		// Test layer 1-d: show grid lines of state/action rows
/*		for (int depth = 0; depth<maxDepth+1; depth++) {
			// draw all large lines demarking rows (note that there is a PlottingParameters.nodeRowHeightState sized spacer at the top of the plot)
			double startOfRow = PlottingParameters.nodeRowHeightState + depth*verticalUnit;
			gc.setStroke(Color.RED);
			gc.setLineWidth(2);
			
			// Borders top and bottom
			gc.strokeLine(0, startOfRow, this.getWidth(), startOfRow);
			gc.strokeLine(0, startOfRow+verticalUnit, this.getWidth(),startOfRow+verticalUnit);
			
			// Separation State and Action
			gc.setLineWidth(1);
			gc.strokeLine(0,startOfRow+PlottingParameters.nodeRowHeightState ,this.getWidth(),startOfRow+PlottingParameters.nodeRowHeightState );
			
			// Separation Action and Spacer
			gc.setLineWidth(1);
			gc.setStroke(Color.BLUE);
			gc.strokeLine(0, startOfRow+PlottingParameters.nodeRowHeightState+PlottingParameters.nodeRowHeightAction,this.getWidth(),startOfRow+PlottingParameters.nodeRowHeightState+PlottingParameters.nodeRowHeightAction);
			
			// Draw the vertical lines marking the columns
			gc.setStroke(Color.GREEN);
			gc.setLineDashes(2,2);
			
			for (int c = 0; c<nodesPerDepth.get(depth).size(); c++) {
				gc.strokeLine(horizontalUnits[depth]*c, startOfRow, horizontalUnits[depth]*c, startOfRow+verticalUnit);
				gc.strokeLine(horizontalUnits[depth]*(c+1), startOfRow, horizontalUnits[depth]*(c+1), startOfRow+verticalUnit);
			}
			gc.setLineDashes(null);
		}
		*/
		// Determine in advance where all the state nodes are placed
		nodesToDraw = new ArrayList<>();
		for (DecisionNode dn: tree.nodes)
		{
			// First, find the depth of the node
			int depth = dn.cuesSampled;

			// Find the x axis coordinate: the place of the decision node in the ArrayList<DecisionNode> stored in nodesPerDepth,
			// multiplied by the horizontal unit, plus halve of a horizontal unit.
			int indexInArray = nodesPerDepth.get(depth).indexOf(dn);
			double x = indexInArray*horizontalUnits[depth] + horizontalUnits[depth]/2;
			
			// The y position of the state node is [all previous rows] + [halve of the state row]
			double y = PlottingParameters.nodeRowHeightState + verticalUnit * depth + PlottingParameters.nodeRowHeightState/2; // The +1 is to make sure there is a strip of whitespace at the top of the plot
			DecisionNodeCanvas dnc = new DecisionNodeCanvas(
					anchorPaneScrollPane, 
					this, 
					dn, 
					mdp, 
					environment, 
					x, 
					y, 
					showingCycle, 
					constructionPhase,
					scaleNodes,
					gc);
			nodesToDraw.add(dnc);
		}
		
		
		
		// Layer 2: draw the decision edges
		this.drawActionEdgesSampling(gc);
		this.drawActionEdgeAcceptingDiscarding(gc);

		// Layer 3: draw the node canvasses
		for (DecisionNodeCanvas dnc: nodesToDraw)
			dnc.draw(false); 
	}
	
	/** Draws the state-node-to-action-node edge, the action node, and all action-node-to-state-node lines for the sampling action */
	private void drawActionEdgesSampling(GraphicsContext gc) {
		// Draw the edges from the action nodes to the next state node
		for (DecisionEdge de: tree.edges)
		{		
			// Find the DecisionNodeCanvas belonging to the parent
			DecisionNodeCanvas parentCanvas = null;
			for (DecisionNodeCanvas dnc:nodesToDraw)
				if (dnc.dn == de.parent) {
					parentCanvas = dnc;
					break;
				}
			
			// Find the DecisionNodeCanvas belonging to the child
			DecisionNodeCanvas childCanvas = null;
			for (DecisionNodeCanvas dnc:nodesToDraw)
				if (dnc.dn == de.child) {
					childCanvas = dnc;
					break;
				}
			
			// Determine whether the action lines and action node should be drawn
			EdgeStyle esSampling = EdgeStyle.NotDrawn;
			if (constructionPhase==ConstructionPhase.FORWARD_PASS )
				esSampling = EdgeStyle.Normal;
			
			if (constructionPhase==ConstructionPhase.BACKWARD_PASS && parentCanvas.dn.bestAction.contains(Action.SAMPLE))
				esSampling = EdgeStyle.Normal;
				
			if (constructionPhase==ConstructionPhase.BACKWARD_PASS && !parentCanvas.dn.bestAction.contains(Action.SAMPLE) && showUnusedEdges)
				esSampling = EdgeStyle.Inactive;

			if (constructionPhase==ConstructionPhase.FORWARD_PRUNING_PASS && parentCanvas.dn.bestAction.contains(Action.SAMPLE))
				esSampling = EdgeStyle.Normal;
			
	
			if (esSampling != EdgeStyle.NotDrawn) {
				
				// Draw the edge connecting the parent state node to the action node
				// For drawing the edge connecting the parent state node to the action node: determine the width, color, fill etc for the edge based on the edgeStyle
				if (esSampling == EdgeStyle.Normal) {
					gc.setStroke(ColorPalette.policyPlotStateNodeToActionNodeEdgeColor );
					gc.setLineWidth(PlottingParameters.edgeStateToActionSize);
					gc.setLineDashes(null);
				} 
				if (esSampling == EdgeStyle.Inactive) {
					gc.setStroke(ColorPalette.policyPlotColorInactive );
					gc.setLineWidth(PlottingParameters.edgeStateToActionSize);
					gc.setLineDashes(5,7);
				}
				gc.strokeLine( parentCanvas.x, parentCanvas.y, parentCanvas.x, parentCanvas.y+PlottingParameters.nodeRowHeightState/2+ PlottingParameters.nodeRowHeightAction/2);
				
				// For drawing the edge connecting the action node to the child nodes: determine the width, color, fill etc for the state node based on the edgeStyle
				if (esSampling == EdgeStyle.Normal) {
					gc.setLineDashes(null);
					gc.setStroke(PlottingParameters.getColor(de));
					gc.setFill(PlottingParameters.getColor(de));
				} 
				if (esSampling == EdgeStyle.Inactive) {
					gc.setLineDashes(7,12);
					gc.setStroke(ColorPalette.policyPlotColorInactive);
					gc.setFill(ColorPalette.policyPlotColorInactive);
					gc.setLineCap(StrokeLineCap.ROUND);
				}
				
				// Find the x,y coordinates of the start (i.e., the action node after the parental state node)
				double startX = parentCanvas.x;
				double startY = parentCanvas.y+PlottingParameters.nodeRowHeightState/2 + PlottingParameters.nodeRowHeightAction/2;
				
				// Find the x,y coordinate of the state node of the child
				double childX = childCanvas.x;
				double childY = childCanvas.y;
				
				// Figure out the length and slope of the line connecting the action and state nodes
				double dx = childX-startX;
				double dy = childY-startY;
				
				// Determine the end points of the line
				double endX = startX+0.75*dx;
				double endY = startY+0.75*dy;
				
				if (Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) )> 150) {
					endX = startX+0.85*dx;
					endY = startY+0.85*dy;
				}
				if (Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) )< 100) {
					endX = startX+0.65*dx;
					endY = startY+0.65*dy;
				}
				if (Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) )< 60) {
					endX = startX+0.5*dx;
					endY = startY+0.5*dy;
				}
				
				// Determine the width of the line (depends on scaleEdges)
				double lineWidth;
				if (scaleEdges)
					lineWidth = PlottingParameters.edgeActionToStateSizeMinimum+ (PlottingParameters.edgeActionToStateSizeMaximum-PlottingParameters.edgeActionToStateSizeMinimum)*de.proportion.doubleValue() ;
				else
					lineWidth = PlottingParameters.edgeActionToStateSizeUnscaled;
				
				// Draw an arrow from the action node, stopping at the next state node
				gc.setLineWidth(lineWidth);
				
				drawArrow(gc, startX, startY, endX, endY,2*lineWidth );
				gc.setLineDashes(null);
				
				
				// Draw the action node (i.e., the dot)
				double dotX = parentCanvas.x;
				double dotY = parentCanvas.y+PlottingParameters.nodeRowHeightState/2+ PlottingParameters.nodeRowHeightAction/2;
				gc.setFill(Color.WHITE);
				gc.setStroke(Color.WHITE);
				gc.fillOval(
						dotX - PlottingParameters.nodeActionDotSize/2, 		// X start (left)
						dotY - PlottingParameters.nodeActionDotSize/2, 		// Y start (top)
						PlottingParameters.nodeActionDotSize, 			// width
						PlottingParameters.nodeActionDotSize);			// height
				gc.strokeOval(
						dotX - PlottingParameters.nodeActionDotSize/2, 		// X start (left)
						dotY - PlottingParameters.nodeActionDotSize/2, 		// Y start (top)
						PlottingParameters.nodeActionDotSize, 			// width
						PlottingParameters.nodeActionDotSize);			// height
				
				// draw the action node itself (i.e., the dot) with a slightly smaller radius
				double dotSize = PlottingParameters.nodeActionDotSize * PlottingParameters.innerRadiusFractionActionNode;
				gc.setFill(Color.BLACK);
				gc.fillOval(
						dotX - dotSize/2, 		// X start (left)
						dotY - dotSize/2, 		// Y start (top)
						dotSize, 			// width
						dotSize);			// height
			}
			
			
		
		}
		
	}
	
	/** Does the actual drawing for the accepting and discarding nodes. Should be called after coordinates have been calculated. End coordinates should reflect the location of the state node */
	private void drawNodesAndEdgesAcceptingDiscarding(GraphicsContext gc, double startX, double startY, double endX, double endY, EdgeStyle es, Color fillIfActive, String costsAndBenefits) {
		// Figure out the length and slope of the line connecting the action and state nodes
		double dx = endX-startX;
		double dy = endY-startY;

		// Determine the end points of the line
		double shortStopX = startX+0.75*dx;
		double shortStopY = startY+0.75*dy;
		
		if (Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2) )> 150) {
			shortStopX = startX+0.9*dx;
			shortStopY = startY+0.9*dy;
		}
		
		// For drawing the state node: determine the width, color, fill etc for the state node based on the edgeStyle
		if (es == EdgeStyle.Normal) {
			gc.setLineWidth(PlottingParameters.nodeTerminalBorderWidth);
			gc.setFill(fillIfActive);
			gc.setStroke(ColorPalette.policyPlotStateNodeToActionNodeEdgeColor );
			gc.setLineDashes(null);
			gc.setLineCap(StrokeLineCap.ROUND);
		} 
		if (es == EdgeStyle.Inactive) {
			gc.setLineWidth(PlottingParameters.nodeTerminalBorderWidth);
			gc.setFill(ColorPalette.policyPlotBackgroundColor);
			gc.setStroke(ColorPalette.policyPlotColorInactive );
			gc.setLineDashes(null);
			gc.setLineCap(StrokeLineCap.ROUND);
		}
		
		// Draw the state node
		gc.fillRect(
				endX - PlottingParameters.nodeTerminalSize/2, 		// X start (left)
				endY - PlottingParameters.nodeTerminalSize/2, 		// Y start (top)
				PlottingParameters.nodeTerminalSize, 			// width
				PlottingParameters.nodeTerminalSize);			// height
		gc.strokeRect(
				endX - PlottingParameters.nodeTerminalSize/2, 		// X start (left)
				endY - PlottingParameters.nodeTerminalSize/2, 		// Y start (top)
				PlottingParameters.nodeTerminalSize, 			// width
				PlottingParameters.nodeTerminalSize);			// height
		
		// if printTerminalOutcomes is true: print the expected costs/benefits of ending in that terminal (action) state:
		if (printTerminalOutcomes && this.constructionPhase != ConstructionPhase.FORWARD_PASS) {
			gc.setFont(PlottingParameters.nodeActionOutcomeFont);
			gc.setTextAlign(TextAlignment.LEFT);
			gc.setTextBaseline(VPos.CENTER);
			gc.setFill(ColorPalette.policyPlotDescriptionTextColor);
			gc.fillText(costsAndBenefits, 
		            endX-45,
		            endY+(PlottingParameters.nodeRowHeightAction/2)+3);	
		}
		
		// Set the width, color fill etc for the state-action lines (in this case the state-action-state line)
		if (es == EdgeStyle.Normal) {
			gc.setFill(ColorPalette.policyPlotStateNodeToActionNodeEdgeColor);
			gc.setStroke(ColorPalette.policyPlotStateNodeToActionNodeEdgeColor );
			gc.setLineDashes(null);
			gc.setLineWidth(PlottingParameters.edgeStateToActionSize);
			gc.setLineCap(StrokeLineCap.ROUND);
		} 
		if (es == EdgeStyle.Inactive) {
			gc.setFill(ColorPalette.policyPlotColorInactive);
			gc.setStroke(ColorPalette.policyPlotColorInactive);
			gc.setLineDashes(5,7);
			gc.setLineWidth(PlottingParameters.edgeStateToActionSize);
			gc.setLineCap(StrokeLineCap.ROUND);
		}
		
		drawArrow(gc, startX, startY, shortStopX, shortStopY,2*PlottingParameters.edgeStateToActionSize);
	}
	
	/** Determines if and where the accepting and discarding nodes should be drawn, and draws both action edges and action states. */
	private void drawActionEdgeAcceptingDiscarding(GraphicsContext gc) {

		for (DecisionNodeCanvas dnc: nodesToDraw ) {
			// Determine the edge style of the accepting line
			EdgeStyle esAccepting = EdgeStyle.NotDrawn;
			if (constructionPhase==ConstructionPhase.FORWARD_PASS )
				esAccepting = EdgeStyle.Normal;

			if (constructionPhase==ConstructionPhase.BACKWARD_PASS && dnc.dn.bestAction.contains(Action.EAT))
				esAccepting = EdgeStyle.Normal;

			if (constructionPhase==ConstructionPhase.BACKWARD_PASS && !dnc.dn.bestAction.contains(Action.EAT) && showUnusedEdges)
				esAccepting = EdgeStyle.Inactive;

			if (constructionPhase==ConstructionPhase.FORWARD_PRUNING_PASS && showUnusedEdges && !dnc.dn.bestAction.contains(Action.SAMPLE) )
				esAccepting = EdgeStyle.Inactive;
			
			if (constructionPhase==ConstructionPhase.FORWARD_PRUNING_PASS && dnc.dn.bestAction.contains(Action.EAT))
				esAccepting = EdgeStyle.Normal;

			if (esAccepting != EdgeStyle.Normal && drawAcceptingEdgeAsInactiveAnyway(dnc.dn))
				esAccepting = EdgeStyle.Inactive;
			
			// If we need to draw the accepting action:
			if (esAccepting != EdgeStyle.NotDrawn) {

				// Determine coordinates for the accepting
				double columnWidth = horizontalUnits[dnc.dn.cuesSampled];
				double acceptingX = snap(dnc.x+columnWidth/4);
				double acceptingY = snap(dnc.y+PlottingParameters.nodeRowHeightState/2+ PlottingParameters.nodeRowHeightAction/3);

				// Determine the costs and benefits text
				String costsAndBenefitsString;
				if (showImmediateOutcomes && !showFutureOutcomes)
					costsAndBenefitsString = format(dnc.dn.acceptingProbabilityCosts, true)   + ":" + format(dnc.dn.acceptingExpectedImmediateCosts, false) + 
									  "\n" + format(dnc.dn.acceptingProbabilityNeutral, true) + ":" + format(dnc.dn.acceptingExpectedImmediateNeutral, false) +
									  "\n" + format(dnc.dn.acceptingProbabilityBenefits, true)+ ":" + format(dnc.dn.acceptingExpectedImmediateBenefits, false)  	;
				else if (! showImmediateOutcomes && showFutureOutcomes)
					costsAndBenefitsString = format(dnc.dn.acceptingProbabilityCosts, true)   + ":" + format(dnc.dn.acceptingExpectedFutureCosts, false) + 
					  				  "\n" + format(dnc.dn.acceptingProbabilityNeutral, true) + ":" + format(dnc.dn.acceptingExpectedFutureNeutral, false) +
					  				  "\n" + format(dnc.dn.acceptingProbabilityBenefits, true)+ ":" + format(dnc.dn.acceptingExpectedFutureBenefits, false)  	;
				else if (showImmediateOutcomes && showFutureOutcomes) {
					costsAndBenefitsString = format(dnc.dn.acceptingProbabilityCosts, true)   + ":" + format(dnc.dn.acceptingExpectedImmediateCosts, false) + "/" +format(dnc.dn.acceptingExpectedFutureCosts, false) + 
									  "\n" + format(dnc.dn.acceptingProbabilityNeutral, true) + ":" + format(dnc.dn.acceptingExpectedImmediateNeutral, false) + "/" +format(dnc.dn.acceptingExpectedFutureNeutral, false) +
									  "\n" + format(dnc.dn.acceptingProbabilityBenefits, true)+ ":" + format(dnc.dn.acceptingExpectedImmediateBenefits, false) + "/" +format(dnc.dn.acceptingExpectedFutureBenefits, false)  	;
				} else costsAndBenefitsString = "";
					
					
				// Draw
				drawNodesAndEdgesAcceptingDiscarding(gc, dnc.x, dnc.y, acceptingX, acceptingY, esAccepting, ColorPalette.policyPlotActionColorEating,costsAndBenefitsString);
			}

			// Determine the edge style of the discarding line
			EdgeStyle esDiscarding = EdgeStyle.NotDrawn;
			if (constructionPhase==ConstructionPhase.FORWARD_PASS )
				esDiscarding = EdgeStyle.Normal;

			if (constructionPhase==ConstructionPhase.BACKWARD_PASS && dnc.dn.bestAction.contains(Action.DISCARD))
				esDiscarding = EdgeStyle.Normal;

			if (constructionPhase==ConstructionPhase.BACKWARD_PASS && !dnc.dn.bestAction.contains(Action.DISCARD) && showUnusedEdges)
				esDiscarding = EdgeStyle.Inactive;

			if (constructionPhase==ConstructionPhase.FORWARD_PRUNING_PASS && showUnusedEdges && !dnc.dn.bestAction.contains(Action.SAMPLE) )
				esDiscarding = EdgeStyle.Inactive;
			
			if (constructionPhase==ConstructionPhase.FORWARD_PRUNING_PASS && dnc.dn.bestAction.contains(Action.DISCARD))
				esDiscarding = EdgeStyle.Normal;

			if (esDiscarding != EdgeStyle.Normal && drawDiscardingEdgeAsInactiveAnyway(dnc.dn))
				esDiscarding = EdgeStyle.Inactive;
			
			// If we need to draw the discarding action:
			if (esDiscarding != EdgeStyle.NotDrawn) {

				// Determine coordinates for the discarding action nodes
				double columnWidth = horizontalUnits[dnc.dn.cuesSampled];
				double discardingX = dnc.x-columnWidth/4;
				double discardingY = dnc.y+PlottingParameters.nodeRowHeightState/2+ PlottingParameters.nodeRowHeightAction/3;

				// Determine the costs and benefits text
				String costsAndBenefitsString;
				if (showImmediateOutcomes && !showFutureOutcomes)
					costsAndBenefitsString = format(dnc.dn.discardingProbabilityCosts, true)   + ":" + format(dnc.dn.discardingExpectedImmediateCosts, false) + 
									  "\n" + format(dnc.dn.discardingProbabilityNeutral, true) + ":" + format(dnc.dn.discardingExpectedImmediateNeutral, false) +
									  "\n" + format(dnc.dn.discardingProbabilityBenefits, true)+ ":" + format(dnc.dn.discardingExpectedImmediateBenefits, false)  	;
				else if (! showImmediateOutcomes && showFutureOutcomes)
					costsAndBenefitsString = format(dnc.dn.discardingProbabilityCosts, true)   + ":" + format(dnc.dn.discardingExpectedFutureCosts, false) + 
					  				  "\n" + format(dnc.dn.discardingProbabilityNeutral, true) + ":" + format(dnc.dn.discardingExpectedFutureNeutral, false) +
					  				  "\n" + format(dnc.dn.discardingProbabilityBenefits, true)+ ":" + format(dnc.dn.discardingExpectedFutureBenefits, false)  	;
				else if (showImmediateOutcomes && showFutureOutcomes) {
					costsAndBenefitsString = format(dnc.dn.discardingProbabilityCosts, true)   + ":" + format(dnc.dn.discardingExpectedImmediateCosts, false) + "/" +format(dnc.dn.discardingExpectedFutureCosts, false) + 
									  "\n" + format(dnc.dn.discardingProbabilityNeutral, true) + ":" + format(dnc.dn.discardingExpectedImmediateNeutral, false) + "/" +format(dnc.dn.discardingExpectedFutureNeutral, false) +
									  "\n" + format(dnc.dn.discardingProbabilityBenefits, true)+ ":" + format(dnc.dn.discardingExpectedImmediateBenefits, false) + "/" +format(dnc.dn.discardingExpectedFutureBenefits, false)  	;
				} else costsAndBenefitsString = "";
					
					
				// Draw
				drawNodesAndEdgesAcceptingDiscarding(gc, dnc.x, dnc.y, discardingX, discardingY, esDiscarding, ColorPalette.policyPlotActionColorDiscarding, costsAndBenefitsString);
			}


		}
	}
	
	/** Draws an arrow with a point on the end. */
	public static void drawArrow(GraphicsContext gc, double x1, double y1, double x2, double y2, double arrowSize) {
	    Affine prev= gc.getTransform();
	    double dx = x2 - x1, dy = y2 - y1;
	    double angle = Math.atan2(dy, dx);
	    int len = (int) Math.sqrt(dx * dx + dy * dy);

	    Transform transform = Transform.translate(x1, y1);
	    transform = transform.createConcatenation(Transform.rotate(Math.toDegrees(angle), 0, 0));
	    gc.setTransform(new Affine(transform));

	    double[] xcoord = new double[]{len+arrowSize, len - arrowSize,  len - arrowSize/2,    len - arrowSize, len+arrowSize};
	    double[] ycoord = new double[]{0,   -arrowSize,        0          ,     arrowSize,      0};

	    gc.strokeLine(0, 0, len-arrowSize, 0);
	    gc.fillPolygon(xcoord, ycoord, 5);
	    gc.setTransform(prev);
	}
	
	 /** Formats a decimal number to display in the plot */
	private String format(DecimalNumber dn, boolean isProbability) {
		if (dn == null)
			return "";
		
		DecimalFormat df;
		if (isProbability)
			df = new DecimalFormat("#.##");
		else
			df = new DecimalFormat("#." + Helper.repString("#", PlottingParameters.nodeActionOutcomeFontSignificantDigits));
		
		if (dn.compareTo(0) < 0)
			return df.format(dn);
		return " " + df.format(dn);
	}
	
	@Override
	public void redraw() {
		this.getChildren().removeAll(this.getChildren());
		draw();
	}
	
	/** The accepting and discarding decision edges are drawn only when the agent takes that
	 * action at a node. However, in some cases we want the plot to show these actions anyway - 
	 * for instance, when the node is the first or second node visited.
	 */
	private boolean drawDiscardingEdgeAsInactiveAnyway(DecisionNode dn) {
		if (dn.cuesSampled <= 4 && dn.cueSet[0] > dn.cueSet[1] && printTerminalOutcomes && constructionPhase==ConstructionPhase.FORWARD_PRUNING_PASS )
			return true;
		return false;
	}
	
	/** The accepting and discarding decision edges are drawn only when the agent takes that
	 * action at a node. However, in some cases we want the plot to show these actions anyway - 
	 * for instance, when the node is the first or second node visited.
	 */
	private boolean drawAcceptingEdgeAsInactiveAnyway(DecisionNode dn) {
		if (dn.cuesSampled <= 4 && dn.cueSet[0] < dn.cueSet[1] && printTerminalOutcomes && constructionPhase==ConstructionPhase.FORWARD_PRUNING_PASS )
			return true;
		return false;
	}
	
	
	@Override
	public void destroy() {
	/*	this.anchorPaneScrollPane.widthProperty().removeListener(widthPropertyListener);
		this.anchorPanePlotting.getChildren().remove(this);
		for (DecisionNodeCanvas dnc: nodesToDraw)
		{
			if (dnc.informationPane != null)
			{
				dnc.informationPane.hide();
				dnc.draw(false);
			}
		}
		
		for (DecisionEdge de: tree.edges) {
			de.child = null;
			de.parent = null;
		}
		
		for (DecisionNode dn: tree.nodes) {
			dn.childEdges.removeAll(dn.childEdges);
			dn.parentEdges.removeAll(dn.parentEdges);
		}
		
		tree.nodes.removeAll(tree.nodes);
		tree = null;*/
	}
	
	@Override
	public void writeToFile(File file) {
		double scale = AbstractPolicyPlot.SCALE;
		WritableImage wi = new WritableImage((int)Math.rint(scale*canvas.getWidth()), (int)Math.rint(scale*canvas.getHeight()));
	    SnapshotParameters spa = new SnapshotParameters();
	    spa.setTransform(Transform.scale(scale, scale));
	    spa.setFill(Color.TRANSPARENT);
	    
	    try {
			ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(spa, wi), null), "png", file);
		} catch (IOException e) {
			ObserverManager.notifyObserversOfError(e);
		}
 
	}
	
	
	/**
	 * The mouse listener. If the user presses the secondary button, close all open information panes
	 */
	@Override
	public void handle(MouseEvent event) {
		//If the canvas is clicked with the right mouse button, close all informationPanes
		if (event.getEventType() == MouseEvent.MOUSE_CLICKED &&  event.getButton() == MouseButton.SECONDARY)
			for (DecisionNodeCanvas dnc: nodesToDraw)
			{
				if (dnc.informationPane != null)
				{
					dnc.informationPane.hide();
					dnc.draw(false);
				}
			}
	}

	

}

