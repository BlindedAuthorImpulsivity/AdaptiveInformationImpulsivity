package window;

import java.text.DecimalFormat;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;

import decimalNumber.DecimalNumberArray;
import defaultAndHelper.ColorPalette;
import externalPrograms.RserveManager;
import helper.Helper;
import helper.Helper.MisspecifiedException;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import staticManagers.ObserverManager;
import window.interfacesAndAbstractions.AbstractTab;
import window.interfacesAndAbstractions.LayoutManager;
import window.interfacesAndAbstractions.LayoutManager.TextFieldValidInputCriterium;

public class TabMDP extends AbstractTab{

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////// 	MARKOV DECISION PROCESS TAB		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////// MDP Parameters - FXML Nodes
	// State space parameter nodes.
	@FXML public TextField		textFieldValueMax;
	@FXML public TextField		textFieldValueStep;
	@FXML public TextField		textFieldBudgetMax;
	@FXML public TextField		textFieldBudgetStep;

	// Task parameter nodes.
	@FXML public Button 		buttonCueEmissionOptions;
	@FXML public TextField		textFieldCueMax;
	@FXML public TextField		textFieldCueCost;
	@FXML public CheckBox		checkBoxCompoundInterruption;

	// Fitness parameter nodes
	@FXML public TextField		textFieldFitnessFunction;
	@FXML public TextField		textFieldDiscount;

	@FXML public AreaChart<Number, Number> cueEmissionsPlot;
	@FXML public NumberAxis cueEmissionsPlotXAxis;
	@FXML public NumberAxis cueEmissionsPlotYAxis;

	@FXML public LineChart<Number, Number> fitnessPlot;
	@FXML public NumberAxis fitnessPlotXAxis;
	@FXML public NumberAxis fitnessPlotYAxis;

	public TabMDP(FrameController fc) {
		super(fc, "paneMDP.fxml"); 
	}

	@Override
	public void update() {
		// Print the defaults on the screen
		DecimalFormat df = new DecimalFormat("#.####################");
		this.textFieldValueMax.setText(df.format(frame.mdpBuilder.VALUE_MAXIMUM));
		this.textFieldValueStep.setText(df.format(frame.mdpBuilder.VALUE_STEP));
		this.textFieldBudgetMax.setText(df.format(frame.mdpBuilder.BUDGET_MAXIMUM));
		this.textFieldBudgetStep.setText(df.format(frame.mdpBuilder.BUDGET_STEP));

		this.textFieldCueCost.setText(df.format(frame.mdpBuilder.COST_OF_SAMPLING));
		this.textFieldCueMax.setText(df.format(frame.mdpBuilder.MAXIMUM_CUES));
		this.checkBoxCompoundInterruption.setSelected(frame.mdpBuilder.COMPOUND_INTERRUPTION_RATE);

		this.textFieldFitnessFunction.setText(frame.mdpBuilder.FITNESS_FUNCTION);
		this.setFitnessFunction(textFieldFitnessFunction.getText());
		this.textFieldDiscount.setText(df.format(frame.mdpBuilder.DISCOUNT_RATE));

		this.drawCueEmissionsPlot();
	}

	/**
	 * Sets all the action listeners. Per text field there are two action listeners:
	 * 1. A change listener for the text property: this handler deals with the layout ONLY 
	 * (i.e., changes the layout to an error style when the input is not valid).
	 * 2. A change listener for the focused property: changes are only made final after
	 * the user unfocuses from the text field. This is where the actual updating takes
	 * place.
	 * 
	 * Note that some, but not all text fields, call frame.notifyAll. This function
	 * only has to be called when other values in the view (prior to running) depend
	 * on the value of this field (e.g., the maximum resource value changes things like
	 * the cue emission matrix, but the cost of sampling does not change anything else).
	 * 
	 */
	@Override
	public void setNodes()
	{
		// Set all input managers:
		LayoutManager.setLayoutHandler(	textFieldValueMax, 		TextFieldValidInputCriterium.POSITIVE_DOUBLE);
		LayoutManager.setLayoutHandler(	textFieldValueStep,		TextFieldValidInputCriterium.POSITIVE_DOUBLE);
		LayoutManager.setLayoutHandler(	textFieldBudgetMax, 	TextFieldValidInputCriterium.POSITIVE_DOUBLE);
		LayoutManager.setLayoutHandler(	textFieldCueMax, 		TextFieldValidInputCriterium.POSITIVE_INTEGER);
		LayoutManager.setLayoutHandler(	textFieldCueCost, 		TextFieldValidInputCriterium.DOUBLE);
		LayoutManager.setLayoutHandler(	textFieldDiscount, 		TextFieldValidInputCriterium.PROBABILITY);

		// Set the behaviour of all text fields: when to update
		this.textFieldValueMax.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldValueMax)) {
					frame.mdpBuilder.VALUE_MAXIMUM = Double.parseDouble(textFieldValueMax.getText());
					frame.notifyAll(frame.mdpBuilder, frame.tabMDP);
					LayoutManager.setProcessed(textFieldValueMax);
				}}});

		this.textFieldValueStep.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldValueStep)) {
					frame.mdpBuilder.VALUE_STEP = Double.parseDouble(textFieldValueStep.getText());
					frame.notifyAll(frame.mdpBuilder, frame.tabMDP);
					LayoutManager.setProcessed(textFieldValueStep);
				}}});

		this.textFieldBudgetMax.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldBudgetMax)) {
					frame.mdpBuilder.BUDGET_MAXIMUM = Double.parseDouble(textFieldBudgetMax.getText());
					frame.notifyAll(frame.mdpBuilder, frame.tabMDP);
					LayoutManager.setProcessed(textFieldBudgetMax);
				}}});

		this.textFieldCueMax.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldCueMax)) {
					frame.mdpBuilder.MAXIMUM_CUES =Integer.parseInt(textFieldCueMax.getText());
					LayoutManager.setProcessed(textFieldCueMax);
				}}});

		this.textFieldCueCost.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldCueCost)) {
					frame.mdpBuilder.COST_OF_SAMPLING = Double.parseDouble(textFieldCueCost.getText());
					frame.notifyAll(frame.mdpBuilder, frame.tabMDP);
					LayoutManager.setProcessed(textFieldCueCost);
				}}});

		this.textFieldDiscount.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldDiscount)) {
					frame.mdpBuilder.DISCOUNT_RATE = Double.parseDouble(textFieldDiscount.getText());
					LayoutManager.setProcessed(textFieldDiscount);
				}}});

		this.textFieldFitnessFunction.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) { 
				boolean validInput = textFieldFitnessFunction.getText().contains("x");
				if (!validInput) {
					LayoutManager.setState(textFieldFitnessFunction, LayoutManager.State.Invalid);
					ObserverManager.makeWarningToast("Cannot parse budget to fitness function: this function must contain at least one \"x\" (which represents the budget). To use constants, please use \"0*x+c\".");

				}else {
					LayoutManager.setState(textFieldFitnessFunction, LayoutManager.State.Normal);
					setFitnessFunction(textFieldFitnessFunction.getText()); 
				}
			}
		});

		// Button handler for the cue emission stage: when pressed, launch a new stage
		this.buttonCueEmissionOptions.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {	
				try { new SecondaryStageController_CueEmissionMatrix(frame);} catch (Exception e) {e.printStackTrace();}	
			}});

		// Check box handler for the compound interruption rate
		this.checkBoxCompoundInterruption.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				frame.mdpBuilder.COMPOUND_INTERRUPTION_RATE = newValue;
			}
		});

	}


	/**
	 * Set the fitness function. If the new fitness function is not valid, return to the old value and provide
	 * the user with a reason of failure. If the new fitness function is valid, redraw the fitnessPlot
	 * @param newFitnessFunction
	 */
	private void setFitnessFunction(String newFitnessFunction)
	{
		System.out.println(Helper.timestamp() + " Setting new fitness function...");
		// Save the previous fitness function in case we have to set it back
		String oldFitnessFunction = frame.mdpBuilder.FITNESS_FUNCTION;

		try {
			// Get some values to plug into the function for demonstration purposes.
			// If this fails, tell the user why it fails and reset the fitness function
			double[] values = Helper.sequence(0, frame.mdpBuilder.BUDGET_MAXIMUM, frame.mdpBuilder.BUDGET_STEP);
			double[] fitnesses = RserveManager.evaluateFunction(newFitnessFunction, values);

			if (fitnesses.length != values.length)
				throw new Helper.MisspecifiedException("Result from R is an array of different size than input from Java. The fitness function has been misspecified. Did you include an x in the function?");
			for (double f: fitnesses)
				if (Double.isNaN(f))
					throw new MisspecifiedException("The fitness function produced NaN's.");
			frame.mdpBuilder.FITNESS_FUNCTION = newFitnessFunction;
			drawFitnessPlot(values, fitnesses);

		} catch (REngineException e) {
			ObserverManager.notifyObserversOfError("R engine exception", "Rserve reports an error. Most likely the fitness function could not be parsed. If the fitness function is correct, please check R status. See details", e);
			textFieldFitnessFunction.setText(oldFitnessFunction);

		} catch (REXPMismatchException e) {
			ObserverManager.notifyObserversOfError("R mismatch exception", "Rserve sent an object of a class not anticipated by Java. See details.",e);
			textFieldFitnessFunction.setText(oldFitnessFunction);

		} catch (NumberFormatException e) {
			ObserverManager.notifyObserversOfError("Number format exception", "The fitness string provided results in invalid values. Did you divide by zero?", e);
			textFieldFitnessFunction.setText(oldFitnessFunction);

		} catch (MisspecifiedException e) {
			ObserverManager.notifyObserversOfError(e);
		}

	}

	////////// MDP parameters - Drawing functions
	/**
	 * Draws the cue emission matrix
	 */
	private void drawCueEmissionsPlot()
	{
		// Set all the axes
		cueEmissionsPlotXAxis.setAutoRanging(false);
		cueEmissionsPlotXAxis.setLowerBound(-frame.mdpBuilder.VALUE_MAXIMUM);
		cueEmissionsPlotXAxis.setUpperBound(frame.mdpBuilder.VALUE_MAXIMUM);
		cueEmissionsPlotXAxis.setTickUnit(frame.mdpBuilder.VALUE_MAXIMUM/4);
		cueEmissionsPlotXAxis.setLabel("Resource quality");

		cueEmissionsPlotYAxis.setAutoRanging(false);
		cueEmissionsPlotYAxis.setLowerBound(0);
		cueEmissionsPlotYAxis.setUpperBound(1);
		cueEmissionsPlotYAxis.setTickUnit(0.25);
		cueEmissionsPlotYAxis.setLabel("p( cue | resource quality)");
		cueEmissionsPlotYAxis.lookup(".axis-label")
		.setStyle("-fx-label-padding: -50 0 0 0;");

		cueEmissionsPlot.setLegendVisible(false);
		cueEmissionsPlot.setVisible(false);

		// Erase the previous data points
		cueEmissionsPlot.getData().removeAll(cueEmissionsPlot.getData());

		// compute the number of labels a cue might have
		int numberOfCueLabels = frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1;

		// Create the cueColors in class ColorPalette
		ColorPalette.setCueColors(numberOfCueLabels);

		// Draw the layers in the plot
		addLayerToCueEmissionPlot(1, numberOfCueLabels);
		addLayerToCueEmissionPlot(2, numberOfCueLabels);
		addLayerToCueEmissionPlot(3, numberOfCueLabels);

		// Finally, add the legend (if there are less than 8 cue labels)
		// The legend does not immediately take over these colors specified above - with the code above
		// 		all the lines in the plot are on a red-blue gradient, but the legend colors do not match.
		// Interestingly enough, the legend is only added to the plot after the full plot has been drawn. This
		// 		means we cannot immediately change the colors in the legend. A somewhat cheesy workaround
		//		is to change them a little later - using Platform's runLater.
		if (numberOfCueLabels < 8)
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					Object[] nodes = cueEmissionsPlot.lookupAll(".chart-legend-item-symbol").toArray();
					Legend legend = (Legend)cueEmissionsPlot.lookup(".chart-legend");
					ObservableList<LegendItem> existingItems = legend.getItems();
					ObservableList<LegendItem> legendItems = FXCollections.observableArrayList();

					// This piece of code is executed in parallel to the main thread. As a result, it might
					// 		occur that at runtime this code is already out of date - causing ArrayIndexOutOfBoundsExceptions
					// 		when the number of lines in the main thread is unequal to the number of lines of this mini-thread.
					// 	If this is the case, we can just ignore it - not having a legend is not that big of a deal.
					try {
						for (int cue = 0 ; cue < numberOfCueLabels; cue ++)
						{
							Node node = (Node) nodes[cue];
							node.setStyle("-fx-background-color: "+  ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.cueColors.get(cue),1)) + ";");
							legendItems.add(existingItems.get(cue));
						}
					} catch (IndexOutOfBoundsException e) {System.out.println("\t- Information of parallel thread out of date. Skipped the legend drawing on the cue emissions plot.");}

					existingItems.removeAll(existingItems);
					existingItems.addAll(legendItems);
					cueEmissionsPlot.setLegendVisible(true);
				}});

		cueEmissionsPlot.setVisible(true);
	}

	/** Adds a new series to the cue emission plot. If layer is:
	 * 1) 	add the shading
	 * 2)	add the lines
	 * @param layer
	 * @param numberOfCueLabels
	 */
	private void addLayerToCueEmissionPlot(int layer, int numberOfCueLabels){
		DecimalNumberArray resourceValues = frame.mdpBuilder.CUE_EMISSION_MATRIX.getColumn(0);
		for (int cue = 0; cue <numberOfCueLabels; cue++)
		{
			DecimalNumberArray cueEmissions = frame.mdpBuilder.CUE_EMISSION_MATRIX.getColumn(cue+1);

			Series<Number, Number> series = new Series<>();
			series.setName(frame.mdpBuilder.CUE_EMISSION_MATRIX.getColumnNames()[cue+1]);
			for (int rvIndex = 0; rvIndex < resourceValues.length(); rvIndex++)
				series.getData().add(new Data<>(resourceValues.get(rvIndex), cueEmissions.get(rvIndex)));
			cueEmissionsPlot.getData().add(series);

			// Set the .css components. 
			// If layer = 1: draw only the shaded areas
			if (layer == 1)
				series.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill: " + ColorPalette.toFXMLString(ColorPalette.cueColors.get(cue)) + ";");
			else
				series.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill: transparent;");

			// If layer = 2: draw only the lines 
			if (layer == 2)
				series.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke: " + ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.cueColors.get(cue), 0.9)) + "; -fx-stroke-width: 3px;");
			else
				series.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke:transparent;");

			// if layer = 3, and the number of resource values is lower than 21 (to prevent overcrowding): draw the points on the line per resource value
			if (layer ==3 && frame.mdpBuilder.CUE_EMISSION_MATRIX.nrow() < 21)
				for (Data<Number, Number> dataPoint :series.getData())
					dataPoint.getNode().lookup(".chart-area-symbol").setStyle(
							"-fx-background-color: #ffffff, " +  ColorPalette.toFXMLString(ColorPalette.addOpacity(ColorPalette.cueColors.get(cue),1))+ ";\n"
									+"-fx-background-insets: 0, 2;\n"
									+"-fx-background-radius: 10;\n"
									+"-fx-padding: 5px;"
							);
			else
				for (Data<Number, Number> dataPoint : series.getData())
					dataPoint.getNode().lookup(".chart-area-symbol").setStyle("-fx-background-color: transparent, transparent"	 );
		}
	}
	/**
	 * Draws the budget to fitness function
	 */
	private void drawFitnessPlot(double[] values, double[] fitnesses)
	{
		System.out.println(Helper.timestamp() + "\t Drawing budget-to-fitness plot (MDP tab)");

		// Set the axes
		fitnessPlotXAxis.setLowerBound(0);
		fitnessPlotXAxis.setUpperBound(frame.mdpBuilder.BUDGET_MAXIMUM);
		fitnessPlotXAxis.setTickUnit(frame.mdpBuilder.BUDGET_MAXIMUM/4);
		fitnessPlotXAxis.setLabel("Somatic state at final time step");


		fitnessPlotYAxis.setAutoRanging(true);
		fitnessPlotYAxis.setLabel("Fitness (budget)");
		fitnessPlotYAxis.lookup(".axis-label")
		.setStyle("-fx-label-padding: -50 0 0 0;");

		// Erase the previous data points
		fitnessPlot.getData().removeAll(fitnessPlot.getData());

		// Set the data
		Series<Number, Number> series = new Series<>();
		for (int i =0; i< values.length; i ++)
			series.getData().add(new Data<>(values[i], fitnesses[i]));

		// Add the data
		fitnessPlot.getData().add(series);

		//Do not plot the symbols (otherwise they will overlap)
		for (Data<Number, Number> dataPoint: series.getData())
			dataPoint.getNode().lookup(".chart-line-symbol").setStyle("-fx-background-color: rgba(0,0,0,0), rgba(0,0,0,0)"	 );

		fitnessPlot.setLegendVisible(false);
	}





}
