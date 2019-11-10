package ValueIteratorPolicyPlot;

import java.io.IOException;

import ValueIteratorPolicyPlot.PolicyPlot_ValueIterator.ConstructionPhase;
import ValueIteratorPolicyPlot.PolicyPlot_ValueIterator.ShowingCycle;
import agent.Action;
import agent.ValueIterator.DecisionNode;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import defaultAndHelper.ColorPalette;
import environment.Environment;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import markovDecisionProcess.MarkovDecisionProcess;

/**
 * A DecisionNodeInformationPane (read: a pane that shows information of a decision node) is 
 * a popup that displays information relevant to the node. The user should be able to drag
 * this popup window across the right-hand side of the frame (i.e, everywhere except for
 * the buttons on the left hand side). In addition, if the user scrolls down, this popup
 * should likewise scroll (i.e., it should remain on the same position of the page, not 
 * of the screen). 
 * @author jesse
 *
 */
public class DecisionNodeInformationPane  implements EventHandler<MouseEvent>
{
	public final DecisionNodeCanvas nodeCanvas;
	public final DecisionNode dn;
	public final MarkovDecisionProcess mdp;
	public final Environment environment;
	
	public final AnchorPane scrollPane;
	public double x;
	public double y;
	private boolean visible;
	
	private final ConstructionPhase cp;
	
	@FXML public AnchorPane 	anchorPaneMainPane;
	@FXML public AnchorPane		anchorPaneNodeSummary;
	@FXML public AnchorPane		anchorPaneObservedCues;
	@FXML public AnchorPane		anchorPanePosterior;
	@FXML public AnchorPane		anchorPaneConditionalCue;
	

	// Labels under node summary
	@FXML public Label 			labelBudget;
	@FXML public Label 			labelProportion;
	
	@FXML public Label 			labelBestAction;
	@FXML public Label 			labelOutcome;
	
	@FXML public Label 			labelSamplingTotal;
	
	@FXML public Label 			labelAcceptingBenefitProbability;
	@FXML public Label 			labelAcceptingBenefitImmediate;
	@FXML public Label 			labelAcceptingBenefitFuture;
	
	@FXML public Label 			labelAcceptingCostProbability;
	@FXML public Label 			labelAcceptingCostImmediate;
	@FXML public Label 			labelAcceptingCostFuture;
	
	@FXML public Label 			labelAcceptingTotal;
	
	@FXML public Label 			labelDiscardingBenefitProbability;
	@FXML public Label 			labelDiscardingBenefitImmediate;
	@FXML public Label 			labelDiscardingBenefitFuture;
	@FXML public Label 			labelDiscardingCostProbability;
	@FXML public Label 			labelDiscardingCostImmediate;
	@FXML public Label 			labelDiscardingCostFuture;
	@FXML public Label 			labelDiscardingTotal;
	
		
	@FXML public Button 		buttonCloseButton;
	@FXML public Accordion		accordionPane;
	@FXML public TitledPane		titledPaneSummary;
	@FXML public TitledPane		titledPaneObservedCues;
	@FXML public TitledPane		titledPanePosterior;
	@FXML public TitledPane		titledPaneConditionalCue;

	
	@FXML public BarChart<String, Integer> 		barChartObservedCues;
	@FXML public CategoryAxis 	barChartObservedCuesXAxis;
	@FXML public NumberAxis 	barChartObservedCuesYAxis;
	
	@FXML public AreaChart<DecimalNumber, DecimalNumber> 		areaChartPosteriorProbabilityDistribution;
	@FXML public NumberAxis 	areaChartPosteriorProbabilityDistributionXAxis;
	@FXML public NumberAxis 	areaChartPosteriorProbabilityDistributionYAxis;
	
	@FXML public BarChart<String, DecimalNumber>		bartChartCueEmission;
	@FXML public CategoryAxis 	barChartCueEmissionXAxis;
	@FXML public NumberAxis 	barChartCueEmissionYAxis;
	
	/**
	 * 
	 * @param scrollPane			The anchor pane in which the whole tab is visible.
	 * @param dn					The decision node information to show
	 * @param nodeCanvas			The drawing of the node on the plot. In case the user closes this screen, this drawing should become unselected
	 * @param mdp					The MarkovDecisionProcess of the agent, found in the Output
	 * @param environment			The Environment of the agent, found in the Output.
	 * @param x						x position of the DecisionNodeCanvas that spawns this informationPane
	 * @param y						y position of the information pane on the scrollPane
	 * @throws IOException
	 */
	public DecisionNodeInformationPane(AnchorPane scrollPane, DecisionNode dn, DecisionNodeCanvas nodeCanvas,MarkovDecisionProcess mdp, Environment environment, double x, double y, ConstructionPhase cp) throws IOException
	{
		this.scrollPane = scrollPane;
		this.nodeCanvas = nodeCanvas;
		this.mdp=mdp;
		this.environment=environment;
		this.dn = dn;
		
		this.x=x;
		this.y=y;
		
		this.cp=cp;
		
		// Load the FXML
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("nodeInformation.fxml"));
		loader.setController(this);
		loader.load();
		anchorPaneMainPane.toFront();
		
		// Set the padding and size of the information pane
		anchorPaneMainPane.setPadding(new Insets(PlottingParameters.informationPanePadding,PlottingParameters.informationPanePadding,PlottingParameters.informationPanePadding,PlottingParameters.informationPanePadding ));
		anchorPaneMainPane.setPrefSize(PlottingParameters.informationPaneWidth, PlottingParameters.informationPaneHeight);
		
		setLabelValues();
		
		//Set the observed cues plot
		barChartObservedCuesXAxis.setVisible(false);
		barChartObservedCues.setLegendVisible(false);
		barChartObservedCuesYAxis.setAutoRanging(false);
		barChartObservedCuesYAxis.setUpperBound(dn.cuesSampled);
		barChartObservedCuesYAxis.setLowerBound(0);
		barChartObservedCuesYAxis.setTickUnit(1);
		
		Series<String, Integer> observedCueSeries = new Series<>();
		for (int i = 0; i < dn.cueSet.length; i ++)
			observedCueSeries.getData().add(new Data<String, Integer> (mdp.CUE_LABELS.get(i), dn.cueSet[i]));
		barChartObservedCues.getData().add(observedCueSeries);
		ColorPalette.setCueColors(mdp.CUE_LABELS.size());
		for (int cueLabel = 0; cueLabel < mdp.CUE_LABELS.size(); cueLabel++)
		{
			String nodeName = ".data" + cueLabel +".chart-bar";
			barChartObservedCues.lookup(nodeName).setStyle("-fx-bar-fill: " + ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.cueColors.get(cueLabel), 1)));
		}
		
		//Set the posterior probability distribution plot
		areaChartPosteriorProbabilityDistributionYAxis.setAutoRanging(true);
		areaChartPosteriorProbabilityDistributionYAxis.setUpperBound(1);
		areaChartPosteriorProbabilityDistributionYAxis.setLowerBound(0);
		areaChartPosteriorProbabilityDistributionYAxis.setTickUnit(0.1);
		
		areaChartPosteriorProbabilityDistributionXAxis.setAutoRanging(false);
		areaChartPosteriorProbabilityDistributionXAxis.setUpperBound(mdp.VALUE_MAXIMUM.doubleValue());
		areaChartPosteriorProbabilityDistributionXAxis.setLowerBound(mdp.VALUE_MAXIMUM.negate().doubleValue());
		areaChartPosteriorProbabilityDistributionXAxis.setTickUnit(mdp.VALUE_MAXIMUM.doubleValue()/2);
		
		areaChartPosteriorProbabilityDistribution.setLegendVisible(false);
		Series<DecimalNumber, DecimalNumber> postProbSeries = new Series<>();
		DecimalNumberArray resourceValues = environment.getMatrix().getColumn("Value");
		DecimalNumberArray posteriors = dn.probabilityDistribution.posteriorProbabilityOfResourceValues();
		for (int i = 0; i < resourceValues.size(); i ++)
			postProbSeries.getData().add(new Data<DecimalNumber, DecimalNumber>(resourceValues.get(i), posteriors.get(i)));
		areaChartPosteriorProbabilityDistribution.getData().add(postProbSeries);
		postProbSeries.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill:" + ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.resourceValuesColorFill, ColorPalette.resourceValueColorMaximumOpacity*(1-environment.interruptionRate.doubleValue())))+ ";");
		postProbSeries.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke:" + ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.resourceValuesColorLine, ColorPalette.resourceValueColorMaximumOpacity*(1-environment.interruptionRate.doubleValue())))+ ";"
						+"-fx-stroke-width: 3px;");
		for (Data<DecimalNumber, DecimalNumber> dataPoint :postProbSeries.getData()) {
			// Set the style for each node 
			Node node = 	dataPoint.getNode().lookup(".chart-area-symbol");
			node.setStyle(
					"-fx-background-color: #ffffff, " +   ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.resourceValuesColorLine, ColorPalette.resourceValueColorMaximumOpacity*(1-environment.interruptionRate.doubleValue())))+ ";\n"
					+"-fx-background-insets: 0, 2;\n"
					+"-fx-background-radius: 10;\n"
					+"-fx-padding: 5px;");
			
			// Create a tooltip hover if the mouse hovers over a node - without any delay
			Tooltip t = new Tooltip("p(" + PlottingParameters.format(dataPoint.getXValue().doubleValue())+ "|data) = " + PlottingParameters.format(dataPoint.getYValue().doubleValue()));
			t.getStyleClass().add("resource_value");

			// Make the tooltip appear when the mouse hovers.
			node.setOnMouseEntered(new EventHandler<MouseEvent>() {
			 
			    @Override
			    public void handle(MouseEvent event) {
			        Point2D p = node.localToScreen(node.getLayoutBounds().getMaxX(), node.getLayoutBounds().getMaxY()); //I position the tooltip at bottom right of the node (see below for explanation)
			        t.show(node, p.getX(), p.getY());
			    }
			});
			node.setOnMouseExited(new EventHandler<MouseEvent>() {
			 
			    @Override
			    public void handle(MouseEvent event) {
			        t.hide();
			    }
			});
			
			}
		
		
		
		//Set the conditional cue emissions plot
		barChartCueEmissionXAxis.setVisible(false);
		barChartCueEmissionYAxis.setAutoRanging(false);
		barChartCueEmissionYAxis.setUpperBound(1);
		barChartCueEmissionYAxis.setLowerBound(0);
		barChartCueEmissionYAxis.setTickUnit(0.5);
		bartChartCueEmission.legendVisibleProperty().set(false);
		Series<String, DecimalNumber> conditionalCueEmissionSeries = new Series<>();
		for (int i = 0; i < mdp.CUE_LABELS.size(); i ++)
			conditionalCueEmissionSeries.getData().add(new Data<String, DecimalNumber> (mdp.CUE_LABELS.get(i), dn.probabilityDistribution.posteriorProbabilityOfFutureCues().get(i)));
		bartChartCueEmission.getData().add(conditionalCueEmissionSeries);
		ColorPalette.setCueColors(mdp.CUE_LABELS.size());
		for (int cueLabel = 0; cueLabel < mdp.CUE_LABELS.size(); cueLabel++)
		{
			String nodeName = ".data" + cueLabel +".chart-bar";
			bartChartCueEmission.lookup(nodeName).setStyle("-fx-bar-fill: " + ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.cueColors.get(cueLabel), 1)));
		}
		
		// Set the close button and the mouse action listener: if this pane is closed, the selected node should become unselected
		buttonCloseButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				nodeCanvas.draw(false);
				hide();
		}});
		
		AnchorPane[] anchorPanesRequiringMouseListener = new AnchorPane[] {anchorPaneMainPane, anchorPaneNodeSummary,anchorPaneObservedCues, anchorPanePosterior, anchorPaneConditionalCue};
		for (AnchorPane a: anchorPanesRequiringMouseListener) {
			a.setOnMouseClicked(this);
			a.setOnMousePressed(this);
			a.setOnMouseDragged(this);
		}
		
		

		this.visible = false;
		
	}
	
	public void destroy() {
		AnchorPane[] anchorPanesRequiringMouseListener = new AnchorPane[] {anchorPaneMainPane, anchorPaneNodeSummary,anchorPaneObservedCues, anchorPanePosterior, anchorPaneConditionalCue};
		for (AnchorPane a: anchorPanesRequiringMouseListener) {
			a.setOnMouseClicked(null);
			a.setOnMousePressed(null);
			a.setOnMouseDragged(null);
		}
		for (Series<DecimalNumber, DecimalNumber> series: areaChartPosteriorProbabilityDistribution.getData())
			for (Data<DecimalNumber, DecimalNumber> dataPoint: series.getData()) {
				Node node = 	dataPoint.getNode().lookup(".chart-area-symbol");
				node.setOnMouseEntered(null);
				node.setOnMouseExited(null);
			}
		
		scrollPane.getChildren().remove(anchorPaneMainPane);
	}
	
	public void draw()
	{
		// Add the anchor pane to the parent
		scrollPane.getChildren().add(anchorPaneMainPane);
		AnchorPane.setLeftAnchor(anchorPaneMainPane,x);
		AnchorPane.setTopAnchor(anchorPaneMainPane, y);
		this.visible = true;
		this.titledPaneSummary.setExpanded(true);
	}
	
	/** Set all the label values under Node summary */
	private void setLabelValues() {
		this.labelBudget.setText(PlottingParameters.format(dn.budget.doubleValue()));
		this.labelProportion.setText(PlottingParameters.format(dn.proportion.doubleValue()*100)+"%");
		StringBuilder sb = new StringBuilder();
		for (Action a: dn.bestAction)
			sb.append(" " +a);
		this.labelBestAction.setText(" "+sb.toString());
		if (cp == ConstructionPhase.FORWARD_PASS) {
			labelOutcome.setText("N/A");
			labelAcceptingTotal.setText("N/A");
			labelDiscardingTotal.setText("N/A");
			
		} else {
			labelOutcome.setText(PlottingParameters.format(dn.expectedOutcomes().max()));
			labelSamplingTotal.setText(PlottingParameters.format(dn.expectedOutcomes().get(0)));
			labelAcceptingTotal.setText(PlottingParameters.format(				dn.expectedOutcomes().get(1).doubleValue()));
			labelDiscardingTotal.setText(PlottingParameters.format(				dn.expectedOutcomes().get(2).doubleValue()));
		}
	
		
		
		labelAcceptingBenefitProbability.setText(PlottingParameters.format(	dn.acceptingProbabilityBenefits.doubleValue()));
		labelAcceptingBenefitImmediate.setText(PlottingParameters.format(	dn.acceptingExpectedImmediateBenefits.doubleValue()));
		labelAcceptingBenefitFuture.setText(PlottingParameters.format(		dn.acceptingExpectedFutureBenefits.doubleValue()));
		
		labelAcceptingCostProbability.setText(PlottingParameters.format(	dn.acceptingProbabilityCosts.doubleValue()));
		labelAcceptingCostImmediate.setText(PlottingParameters.format(		dn.acceptingExpectedImmediateCosts.doubleValue()));
		labelAcceptingCostFuture.setText(PlottingParameters.format(			dn.acceptingExpectedFutureCosts.doubleValue()));
		
		
		labelDiscardingBenefitProbability.setText(PlottingParameters.format(dn.discardingProbabilityBenefits.doubleValue()));
		labelDiscardingBenefitImmediate.setText(PlottingParameters.format(	dn.discardingExpectedImmediateBenefits.doubleValue()));
		labelDiscardingBenefitFuture.setText(PlottingParameters.format(		dn.discardingExpectedFutureBenefits.doubleValue()));
		
		labelDiscardingCostProbability.setText(PlottingParameters.format(	dn.discardingProbabilityCosts.doubleValue()));
		labelDiscardingCostImmediate.setText(PlottingParameters.format(		dn.discardingExpectedImmediateCosts.doubleValue()));
		labelDiscardingCostFuture.setText(PlottingParameters.format(		dn.discardingExpectedFutureCosts.doubleValue()));
		
	}
	
	public void hide()
	{
		scrollPane.getChildren().remove(anchorPaneMainPane);
		visible = false;
	}
	
	public boolean isVisible() { return visible;}
	
	double startX;
	double xPressedOnScene;
	double startY;
	double yPressedOnScene;
	
	@Override
	public void handle(MouseEvent event) {
		//If the canvas is clicked with the right mouse button, close the informationPane
		if (event.getEventType() == MouseEvent.MOUSE_CLICKED &&  event.getButton() == MouseButton.SECONDARY) {
			nodeCanvas.draw(false);
			hide();
		}
		
		// If the mouse is pressed, record where the mouse is
		if (event.getEventType() == MouseEvent.MOUSE_PRESSED &&  event.getButton() == MouseButton.PRIMARY) {
			startX			    = x;
			xPressedOnScene 	= event.getSceneX();
			startY			    = y;
			yPressedOnScene 	= event.getSceneY();
		}
		
		if (event.getEventType() == MouseEvent.MOUSE_DRAGGED){
			x = startX + (event.getSceneX()-xPressedOnScene);
			y = startY + (event.getSceneY()-yPressedOnScene);
			// Make sure that the pane never is outside of the scrollPane
			if (x > scrollPane.getWidth() - PlottingParameters.informationPaneWidth)
				x = scrollPane.getWidth() - PlottingParameters.informationPaneWidth;
			
			AnchorPane.setLeftAnchor(anchorPaneMainPane, x);
			AnchorPane.setTopAnchor(anchorPaneMainPane, y);
		}
	}

}
