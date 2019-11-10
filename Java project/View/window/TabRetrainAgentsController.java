package window;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import com.sun.javafx.charts.Legend;
import com.sun.javafx.charts.Legend.LegendItem;

import agent.AgentType;
import agent.Output;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberMatrixTableView;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import defaultAndHelper.ColorPalette;
import environment.ValueDistributionType;
import estimationParameters.Optimizer;
import externalPrograms.RserveManager;
import helper.Helper;
import helper.Helper.MisspecifiedException;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.util.Callback;
import start.Model.RetrainingSpecifications;
import staticManagers.ObserverManager;
import staticManagers.OutputShadow;
import window.interfacesAndAbstractions.AbstractTab;
import window.interfacesAndAbstractions.LayoutManager;
import window.interfacesAndAbstractions.LayoutManager.TextFieldValidInputCriterium;

public class TabRetrainAgentsController extends AbstractTab{

	/** A class that extends the OutputShadow class by adding a 
	 * boolean variable to indicate whether it is selected. This way,
	 * the table can show a checkbox per row. 
	 */
	public class OutputShadowSelected extends OutputShadow{
		public final BooleanProperty selectedProperty;
		
		public OutputShadowSelected(OutputShadow shadow) {
			super(shadow);
			selectedProperty = new SimpleBooleanProperty(false);
		}
		
		public boolean getSelected() { return selectedProperty.get();}
		public void setSelected(boolean newValue ) { this.selectedProperty.set(newValue); }
		
	}

	private Output selectedOutput;
	
	private final int							significantDigits = 5;
	private 		File						outputFolder;
	
	@FXML public	TextField					textfieldFolderInput;
	@FXML public	Button						buttonBrowseFolderInput;
	@FXML public 	Button						buttonRefreshFolderInput;
	@FXML public    CheckBox					checkBoxSubDirectories;
	@FXML public	TableView<OutputShadowSelected> 	tableviewOutputShadows;
	private final 	ObservableList<OutputShadowSelected> 	shadows;
	@FXML public 	Button						buttonSelectAll;
	@FXML public	Button						buttonUnselectAll;
	@FXML public 	Button						buttonResetChanges;
	
	// Change markov decision process tab
	@FXML public 	TextField					textFieldFitnessFunction;
	@FXML public 	LineChart<Number, Number> 	fitnessPlot;
	@FXML public 	NumberAxis 					fitnessPlotXAxis;
	@FXML public 	NumberAxis 					fitnessPlotYAxis;

	// Change environment tab (not implemented)
	
	// Change estimation procedure tab
	@FXML public	ComboBox<AgentType>			comboBoxAgentSelection;

	@FXML public 	CheckBox					checkBoxStoppingCriteriaTime;
	@FXML public 	TextField					textFieldStoppingCriteriaTime;
	@FXML public 	ComboBox<TimeUnit>			comboBoxStoppingCriteriaTimeUnits;
	
	@FXML public 	CheckBox					checkBoxStoppingCriteriaConvergence;
	@FXML public	TextField					textFieldStoppingCriteriaConvergence;
	
	@FXML public 	CheckBox					checkBoxAdditionalIterations;
	@FXML public 	Spinner<Integer>			spinnerIterations;
	
	@FXML public 	TextField					textFieldIndifferencePoint;
	@FXML public 	ComboBox<Optimizer>			comboBoxOptimizer;
	@FXML public 	CheckBox					checkBoxBatchUpdating;
	
	// Retrain agents tab
	@FXML public  	TextField					textfieldFolderOutput;
	@FXML public 	Button						buttonBrowseOutput;
	@FXML public 	Spinner<Integer>			spinnerThreads;
	
	@FXML public 	Button						buttonRetrainAgents;
	
	public TabRetrainAgentsController(FrameController fc) {
		super(fc, "paneRetrainAgents.fxml"); 
		this.shadows = FXCollections.observableArrayList();
		update();
	}

	/** In this tab the update() function is called when a new output shadow is selected within the same tab. 
	 * The update has to set all Nodes to the selected agent's values (i.e., the values that the agent used when
	 * training). If an outputShadow is selected in the table, enable all functionality in this tab
	 * 
	 * If update() is called without a selected outputShadow, disable some of the functionality of this tab
	 */
	@Override
	public void update() {
		if (tableviewOutputShadows.getSelectionModel().getSelectedItem() != null) {
			// Enable all functionality
			textFieldFitnessFunction.setDisable(false);
			fitnessPlot.setVisible(true);
			comboBoxAgentSelection.setDisable(false);
			checkBoxStoppingCriteriaTime.setDisable(false);
			checkBoxStoppingCriteriaConvergence.setDisable(false);
			checkBoxAdditionalIterations.setDisable(false);
			comboBoxOptimizer.setDisable(false);
			textFieldIndifferencePoint.setDisable(false);
			checkBoxBatchUpdating.setDisable(false);
			
			// Read in the shadow - i.e., get the Output object from storage
			selectedOutput = frame.model.OutputShadowToOutput(tableviewOutputShadows.getSelectionModel().getSelectedItem());
			
			// Set all the fields/plots in the change markov decision process tab
			textFieldFitnessFunction.setText(selectedOutput.mdp.FITNESS_FUNCTION);
			try {
				setFitnessFunction(selectedOutput.mdp.FITNESS_FUNCTION, textFieldFitnessFunction.getText());
			} catch (REngineException | REXPMismatchException e) {
				ObserverManager.notifyObserversOfError(e);
				e.printStackTrace();
			} catch (MisspecifiedException e) {
				ObserverManager.notifyObserversOfError(e);
				textFieldFitnessFunction.setText(selectedOutput.mdp.FITNESS_FUNCTION);
			}
			
			// Set the fields in the change environent tab (not implemented yet)
			
			// Set the fields in the change estimation procedure tab
			comboBoxAgentSelection.getSelectionModel().select(selectedOutput.agentType);
			comboBoxOptimizer.getSelectionModel().select(selectedOutput.estimationParameters.optimizer);
			textFieldIndifferencePoint.setText(selectedOutput.estimationParameters.indifferencePoint.toPlainString());
			checkBoxBatchUpdating.setSelected(selectedOutput.estimationParameters.batchUpdating);
		
		} else {
			// Disable some functionality
			textFieldFitnessFunction.setDisable(true);
			fitnessPlot.setVisible(false);
			comboBoxAgentSelection.setDisable(true);
			checkBoxStoppingCriteriaTime.setDisable(true);
			checkBoxStoppingCriteriaConvergence.setDisable(true);
			checkBoxAdditionalIterations.setDisable(true);
			comboBoxOptimizer.setDisable(true);
			textFieldIndifferencePoint.setDisable(true);
			checkBoxBatchUpdating.setDisable(true);
		}

	}

	/** Get R to parse the new fitness function and plot the values on the fitnessPlot. Requires that an Output is selected and loaded 
	 * @throws REXPMismatchException 
	 * @throws REngineException 
	 * @throws MisspecifiedException */
	private void setFitnessFunction(String oldFunction, String newFunction) throws REngineException, REXPMismatchException, MisspecifiedException {
		if (selectedOutput == null) {
			ObserverManager.makeWarningToast("Please select");
		}

		// Get R to parse the function, and give the budgets as input
		System.out.println(Helper.timestamp() + " Setting new fitness function...");

		// Get some values to plug into the function for demonstration purposes.
		// If this fails, tell the user why it fails and reset the fitness function
		double[] values = Helper.sequence(0, selectedOutput.mdp.BUDGET_MAXIMUM.doubleValue(), selectedOutput.mdp.BUDGET_STEP.doubleValue());
		double[] oldFitnesses = RserveManager.evaluateFunction(oldFunction, values);
		double[] newFitnesses = RserveManager.evaluateFunction(newFunction, values);

		if (oldFitnesses.length != values.length || newFitnesses.length != values.length)
			throw new Helper.MisspecifiedException("The result from R is an array of different size than input from Java. The fitness function has been misspecified. Did you include an x in the function?");
		for (double f: newFitnesses)
			if (Double.isNaN(f))
				throw new MisspecifiedException("The new fitness function produced NaN's.");

		//draw the plot
		System.out.println(Helper.timestamp() + "\t Drawing budget-to-fitness plot (Retrain tab)");

		// Set the axes
		fitnessPlotXAxis.setLowerBound(0);
		fitnessPlotXAxis.setUpperBound(selectedOutput.mdp.BUDGET_MAXIMUM.doubleValue());
		fitnessPlotXAxis.setTickUnit(selectedOutput.mdp.BUDGET_MAXIMUM.doubleValue()/4);
		fitnessPlotXAxis.setLabel("Budget at final time step");

		fitnessPlotYAxis.setAutoRanging(true);
		fitnessPlotYAxis.setLabel("Fitness (budget)");
		fitnessPlotYAxis.lookup(".axis-label")
		.setStyle("-fx-label-padding: -50 0 0 0;");

		// Erase the previous data points
		fitnessPlot.getData().removeAll(fitnessPlot.getData());

		// Set the data
		Series<Number, Number> oldSeries = new Series<>();
		for (int i =0; i< values.length; i ++)
			oldSeries.getData().add(new Data<>(values[i], oldFitnesses[i]));
		oldSeries.setName("Previous fitness function");
		
		Series<Number, Number> newSeries = new Series<>();
		for (int i =0; i< values.length; i ++)
			newSeries.getData().add(new Data<>(values[i], newFitnesses[i]));
		newSeries.setName("New fitness function");
		
		// Add the data
		fitnessPlot.getData().add(oldSeries);
		if (oldFunction.compareTo(newFunction) != 0)
			fitnessPlot.getData().add(newSeries);
		
		// Draw the lines, skipping the new function line if it is the same as the old function line
		//Do not plot the symbols (otherwise they will overlap)
		for (Data<Number, Number> dataPoint: oldSeries.getData())
			dataPoint.getNode().lookup(".chart-line-symbol").setStyle("-fx-background-color: rgba(0,0,0,0), rgba(0,0,0,0)"	 );
		oldSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: #99cc66; -fx-stroke-width: 3px;");
		
		if (oldFunction.compareTo(newFunction) != 0) {
			for (Data<Number, Number> dataPoint: newSeries.getData())
				dataPoint.getNode().lookup(".chart-line-symbol").setStyle("-fx-background-color: rgba(0,0,0,0), rgba(0,0,0,0)"	 );
			newSeries.getNode().lookup(".chart-series-line").setStyle("-fx-stroke: rgb(66, 105, 200); -fx-stroke-width: 3px;");
		}

		// Finally, add the legend (if there are less than 8 cue labels)
		// The legend does not immediately take over these colors specified above - with the code above
		// 		all the lines in the plot are on a red-blue gradient, but the legend colors do not match.
		// Interestingly enough, the legend is only added to the plot after the full plot has been drawn. This
		// 		means we cannot immediately change the colors in the legend. A somewhat cheesy workaround
		//		is to change them a little later - using Platform's runLater.
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					Object[] nodes = fitnessPlot.lookupAll(".chart-legend-item-symbol").toArray();
					Legend legend = (Legend)fitnessPlot.lookup(".chart-legend");
					ObservableList<LegendItem> existingItems = legend.getItems();
					ObservableList<LegendItem> legendItems = FXCollections.observableArrayList();

					// This piece of code is executed in parallel to the main thread. As a result, it might
					// 		occur that at runtime this code is already out of date - causing ArrayIndexOutOfBoundsExceptions
					// 		when the number of lines in the main thread is unequal to the number of lines of this mini-thread.
					// 	If this is the case, we can just ignore it - not having a legend is not that big of a deal.
					try {
						// Set the legend for the old function line
						((Node) nodes[0]).setStyle("-fx-background-color: #99cc66;");
						legendItems.add(existingItems.get(0));
						
						// Set the legend for the old function line
						((Node) nodes[1]).setStyle("-fx-background-color: rgb(66, 105, 200);");
						legendItems.add(existingItems.get(1));
						
						
					} catch (IndexOutOfBoundsException e) {System.out.println("\t- Information of parallel thread out of date. Skipped the legend drawing on the cue emissions plot.");}

					existingItems.removeAll(existingItems);
					existingItems.addAll(legendItems);
					fitnessPlot.setLegendVisible(true);
				}});


		//fitnessPlot.setLegendVisible(false);

	}
	
	@Override
	public void setNodes() {
	//// Select agent tab
		// If the browse button is pressed: prompt the user for a new directory to search for .out files
		this.buttonBrowseFolderInput.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				DirectoryChooser dc = new DirectoryChooser();
				File directory = dc.showDialog(frame.stage);
				if (directory != null) {
					textfieldFolderInput.setText(directory.getAbsolutePath());
					inputFolderChanged(directory);
				}
			}
		});

		// If the refresh button is pressed: reread the specified input folder
		this.buttonRefreshFolderInput.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				inputFolderChanged(new File(textfieldFolderInput.getText()));
			}
		});

		// Set all table view stuff
		this.setTableView();

		// Set the unselect/select buttons
		buttonSelectAll.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				for (OutputShadowSelected s:shadows) 
					s.selectedProperty.set(true);		
			}});
		buttonUnselectAll.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				for (OutputShadowSelected s:shadows) 
					s.selectedProperty.set(false);		
			}});
		buttonResetChanges.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				update();
			}
		});

	//// Change markov decision process tab
		// Set the fitness function textfield
		textFieldFitnessFunction.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) { 
				try {
					setFitnessFunction(selectedOutput.mdp.FITNESS_FUNCTION, textFieldFitnessFunction.getText());
				} catch (REngineException | REXPMismatchException e) {
					ObserverManager.notifyObserversOfError(e);
					e.printStackTrace();
				} catch (MisspecifiedException e) {
					ObserverManager.notifyObserversOfError(e);
					textFieldFitnessFunction.setText(selectedOutput.mdp.FITNESS_FUNCTION);
				}
			}
		});

	//// Change environment tab (Not implemented)
		
	//// Change estimation procedure tab
		// Add agents to the select agent type combo box
		comboBoxAgentSelection.getItems().addAll(AgentType.values());

		// Stopping criteria: MAXIMUM TIME. Set the defaults and the change listeners of (a) the checkBoxes, and
		// 	(b) the textField, and (c) the comboBox for time units.
		LayoutManager.setLayoutHandler(textFieldStoppingCriteriaTime, TextFieldValidInputCriterium.POSITIVE_DOUBLE); 
		checkBoxStoppingCriteriaTime.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				textFieldStoppingCriteriaTime.setDisable(!newValue);
				comboBoxStoppingCriteriaTimeUnits.setDisable(!newValue);
				// After re-abling the TextField: place the focus on that field
				if (!textFieldStoppingCriteriaTime.isDisable()) {textFieldStoppingCriteriaTime.requestFocus();}
			}});
		textFieldStoppingCriteriaTime.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldStoppingCriteriaTime)) {
					LayoutManager.setProcessed(textFieldStoppingCriteriaTime);
				}}});
		comboBoxStoppingCriteriaTimeUnits.getItems().addAll(new TimeUnit[]{TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS});
		
		// Stopping criteria: CONVERGENCE. Set the defaults and the change listeners of (a) the checkBoxes, and
		// 	(b) the textField. 
		LayoutManager.setLayoutHandler(textFieldStoppingCriteriaConvergence, TextFieldValidInputCriterium.POSITIVE_DOUBLE); 
		checkBoxStoppingCriteriaConvergence.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				textFieldStoppingCriteriaConvergence.setDisable(!newValue);
				// After re-abling the TextField: place the focus on that field
				if (!textFieldStoppingCriteriaConvergence.isDisable()) {textFieldStoppingCriteriaConvergence.requestFocus();}
		}});
		textFieldStoppingCriteriaConvergence.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldStoppingCriteriaConvergence)) {
					LayoutManager.setProcessed(textFieldStoppingCriteriaConvergence);
				}}});
		
		// Stopping criteria: ADDITIONAL ITERATIONS
		// Set the spinner for additional iterations
		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100000, 0);
		spinnerIterations.setValueFactory(valueFactory);
		spinnerIterations.getEditor().textProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Helper.isPositiveInteger(newValue)) 
					spinnerIterations.getValueFactory().setValue(Integer.parseInt(newValue));
				else if (newValue.length()>0) 
					spinnerIterations.getEditor().setText(oldValue);
			}});
		checkBoxAdditionalIterations.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				spinnerIterations.setDisable(!newValue);
				// After re-abling the TextField: place the focus on that field
				if (!spinnerIterations.isDisable()) {spinnerIterations.requestFocus();}
		}});
		
		// Set the optimizer combobox
		comboBoxOptimizer.getItems().addAll(Optimizer.values());
		
		// Set the indifference point node
		LayoutManager.setLayoutHandler(textFieldIndifferencePoint, TextFieldValidInputCriterium.DOUBLE); 
		textFieldIndifferencePoint.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldIndifferencePoint)) {
					LayoutManager.setProcessed(textFieldIndifferencePoint);
				}}});
		
	//// Retrain agent tab
		// Set folder/browse for the resulting retrained agents
		this.buttonBrowseOutput.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				DirectoryChooser dc = new DirectoryChooser();
				File directory = dc.showDialog(frame.stage);
				if (directory != null) {
					textfieldFolderOutput.setText(directory.getAbsolutePath());
					outputFolder = directory;
				}
			}
		});
		
		// Set the spinner for the number of thread to use
		valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 256, 1);
		spinnerThreads.setValueFactory(valueFactory);
		spinnerThreads.getEditor().textProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Helper.isPositiveInteger(newValue)) 
					spinnerThreads.getValueFactory().setValue(Integer.parseInt(newValue));
				else if (newValue.length()>0) 
					spinnerThreads.getEditor().setText(oldValue);
			}});
		
		// Set the button to start the retraining process
		buttonRetrainAgents.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent arg0) {
				retrainAgents();
			}
		});
		
	
	}
	
	/** Initializes the table view that shows the loaded agents (in shadow form) */
	private void setTableView() {
		DoubleBinding columnWidth= tableviewOutputShadows.widthProperty().subtract(52).divide(7);
		// Create the isSelected column
		TableColumn<OutputShadowSelected, Boolean> selectedCol = new TableColumn<>("S");
		selectedCol.setCellFactory(
			     new Callback<TableColumn<OutputShadowSelected,Boolean>,TableCell<OutputShadowSelected,Boolean>>(){
			         @Override public
			         TableCell<OutputShadowSelected,Boolean> call( TableColumn<OutputShadowSelected,Boolean> p ){
			        	 CheckBoxTableCell<OutputShadowSelected, Boolean> cell = new CheckBoxTableCell<>();
			        	 return cell;
			        	 }});
		selectedCol.setCellValueFactory(
			    new Callback<CellDataFeatures<OutputShadowSelected,Boolean>,ObservableValue<Boolean>>(){
			        @Override public
			        ObservableValue<Boolean> call( CellDataFeatures<OutputShadowSelected,Boolean> p ){
			           return p.getValue().selectedProperty; }});

		selectedCol.setEditable(true);
		selectedCol.prefWidthProperty().bind(new SimpleDoubleProperty(45));
		tableviewOutputShadows.getColumns().add(selectedCol);
		
		
		// Create and add the Resource quality super-column to the table view.
		TableColumn<OutputShadowSelected, TableColumn<OutputShadow, Object>> resourceValueColumn = new TableColumn<OutputShadowSelected, TableColumn<OutputShadow, Object>>("Resource quality");

		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "resourceValueMean", columnWidth, significantDigits, frame, tableviewOutputShadows));
		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "resourceValueSD", columnWidth,significantDigits, frame, tableviewOutputShadows ));
		resourceValueColumn.getColumns().add(createValueDistributionTypeColumn("Distribution Type", "resourceValueDistributionType", columnWidth));
		tableviewOutputShadows.getColumns().add(resourceValueColumn);

		// Create and add the extrinsic event super-column to the table view.
		TableColumn<OutputShadowSelected, TableColumn<OutputShadow, Object>> extrinsicEventColumn = new TableColumn<OutputShadowSelected, TableColumn<OutputShadow, Object>>("Extrinsic events");
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "extrinsicEventMean", columnWidth, significantDigits, frame, tableviewOutputShadows));
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "extrinsicEventSD",columnWidth,significantDigits, frame, tableviewOutputShadows));
		extrinsicEventColumn.getColumns().add(createValueDistributionTypeColumn("Distribution Type", "extrinsicEventDistributionType",columnWidth));
		tableviewOutputShadows.getColumns().add(extrinsicEventColumn);

		
		// Add the interruption rate column to the table view
		TableColumn<OutputShadowSelected, DecimalNumber>interruptionCol = DecimalNumberMatrixTableView.createDecimalNumberColumn("Interruption rate", "interruptionRate", columnWidth,significantDigits, frame, tableviewOutputShadows);
		tableviewOutputShadows.getColumns().add(interruptionCol);
		

		// Add the items
		tableviewOutputShadows.setItems(shadows);

		// Make only selectedCol editable
		tableviewOutputShadows.setEditable(true);
		selectedCol.setEditable(true);
		for (TableColumn<OutputShadowSelected, ?> col:resourceValueColumn.getColumns())
				col.setEditable(false);
		for (TableColumn<OutputShadowSelected, ?> col:extrinsicEventColumn.getColumns())
			col.setEditable(false);
		interruptionCol.setEditable(false);
		
		// When an agent is selected in the table (really selected, not just a textbox selection): set all the fields (change MDP, change environment,
		// and change estimation procedure) in this tab to the agent's current value (i.e., the values the agent used when training)
		tableviewOutputShadows.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<OutputShadowSelected>() {
			public void changed(ObservableValue<? extends OutputShadowSelected> arg0, OutputShadowSelected arg1,
					OutputShadowSelected arg2) {
				update();
			}
		});
		
	}

	/**
	 * Create and return a new TableColumn that houses ValueDistributionType 
	 * @param <T>
	 * @param header
	 * @param variableName
	 * @param width
	 * @return
	 */
	private static TableColumn<OutputShadowSelected, ValueDistributionType> createValueDistributionTypeColumn (String header, String variableName, DoubleBinding width)
	{
		TableColumn<OutputShadowSelected, ValueDistributionType> newCol = new TableColumn<>(header);
		newCol.setCellValueFactory(new PropertyValueFactory<OutputShadowSelected, ValueDistributionType>(variableName));
		newCol.setCellFactory(ComboBoxTableCell.<OutputShadowSelected, ValueDistributionType>forTableColumn((ValueDistributionType.values())));
		newCol.prefWidthProperty().bind(width);
		return newCol;
	}

	/** Call this after the directory for the input folder has changed: it makes the model read in all the results in the folder.
	 * Since this IO can be rather slow, it is executed on a different thread.
	 */
	private void inputFolderChanged(File newDirectory)
	{
		// Check if the directory is valid
		if (!newDirectory.exists()) {
			ObserverManager.makeWarningToast("Please select a folder by pressing the 'browse' button.");
			return;
		}
		
		// Let the model know that it can stop searching for .out files (if applicable)
		frame.model.stopCreatingShadows();

		//Set the placeholder for the table to indicate that the programme is loading in the files.
		ProgressIndicator pi = new ProgressIndicator();
		pi.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
		tableviewOutputShadows.setPlaceholder(pi);

		// Inform the user that input files are being read
		ObserverManager.makeToast("Reading input files");

		// Remove the previous found shadows
		shadows.removeAll(shadows);
		tableviewOutputShadows.refresh();

		// Ask the model to read in all the .out files and get their shadows.
		//		However, this task will take quite a while, and hence should be run on a different thread than the JavaFX threat.
		Task<Void> task = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				ArrayList<OutputShadow> originalShadows = frame.model.createOutputShadows(newDirectory, checkBoxSubDirectories.isSelected());
				for (OutputShadow s: originalShadows)
					shadows.add(new OutputShadowSelected(s));

				Platform.runLater(new Runnable(){
					public void run() {
						//Set the placeholder for the table to indicate that there are no SavedResults (this is only shown if there are, in fact, no results to be shown).
						tableviewOutputShadows.setPlaceholder(new Label("No agents are stored in the folder"));
						
						// Select, if possible, the first item
						if (shadows.size() > 0)
							tableviewOutputShadows.getSelectionModel().select(shadows.get(0));
					}
				});

				// Update the view
				tableviewOutputShadows.refresh();
				
				return null;
			}

		};
		new Thread(task).start();
		
		// Set the output folder to be the same as the input folder
		outputFolder = newDirectory;
		this.textfieldFolderOutput.setText(outputFolder.getAbsolutePath());
	}

	/** Called when the retrain agent button is pressed. Also handles input validation
	 * 
	 */
	private void retrainAgents() {
		// Get a list of all selected output shadows
		ArrayList<OutputShadow> selectedShadows = new ArrayList<>();
		for (OutputShadowSelected oss: shadows)
			if (oss.selectedProperty.get())
				selectedShadows.add(oss);
		
		// Give a warning if no shadows are selected
		if (selectedShadows.size() == 0) {
			ObserverManager.makeWarningToast("Select at least one agent for retraining.");
			return;
		}
		
		// Make sure the output folder is valid
		if (outputFolder == null) {
			ObserverManager.makeWarningToast("Please select an output folder to save the retrained agents to.");
			return;
		}
		if (!outputFolder.exists()) {
			ObserverManager.makeWarningToast("Please select an output folder to save the retrained agents to.");
			return;
		}
			
		// Make sure that all input is valid, that is, the user should have selected...
		
		// ... A fitness function
		if (textFieldFitnessFunction.getText() == null) {
			ObserverManager.makeWarningToast("Please input a valid fitness function");
			return;
		}
		
		// ... An agent type
		if(comboBoxAgentSelection.getSelectionModel().getSelectedItem() == null) {
			ObserverManager.makeWarningToast("Please select a new agent type.");
			return;
		}
		
		// ... At least one new stopping criterium
		if (!this.checkBoxStoppingCriteriaTime.isSelected() && !this.checkBoxStoppingCriteriaConvergence.isSelected() && !this.checkBoxAdditionalIterations.isSelected()) {
			ObserverManager.makeWarningToast("Please select at least one new stopping criterium");
			return;
		}
		
		// ... no invalid values for the stopping criteria
		if(this.checkBoxStoppingCriteriaTime.isSelected() && (!LayoutManager.isNormal(this.textFieldStoppingCriteriaTime) || this.textFieldStoppingCriteriaTime.getText().length()==0) ){
			ObserverManager.makeWarningToast("You have selected a maximum time stopping criteria. However, the requested maximum time is not a valid number. Please input a valid number, or refrain from using a maximum time by unselecting the maximum time criterium checkbox");
			return;
		}
		if (this.checkBoxStoppingCriteriaTime.isSelected() && this.comboBoxStoppingCriteriaTimeUnits.getSelectionModel().getSelectedItem()==null) {
			ObserverManager.makeWarningToast("You have selected a maximum time stopping criteria. Please select a valid time unit or refrain from using a maximum time by unselecting the maximum time criterium checkbox");
			return;
		}
		if(this.checkBoxStoppingCriteriaConvergence.isSelected() && (!LayoutManager.isNormal(this.textFieldStoppingCriteriaConvergence) || this.textFieldStoppingCriteriaConvergence.getText().length()==0) ){
			ObserverManager.makeWarningToast("You have selected an epsilon-delta convergence stopping criteria. However, the requested convergence level is not a valid number. Please input a valid number, or refrain from using a maximum time by unselecting the maximum time criterium checkbox");
			return;
		}
		
		// ... a valid indifference point
		if(!LayoutManager.isNormal(this.textFieldIndifferencePoint)|| this.textFieldIndifferencePoint.getText().length()==0) {
			ObserverManager.makeWarningToast("Please select a new agent type.");
			return;
		}
		
		// ... some threads to run on
		if(this.spinnerThreads.getValue() == null) {
			ObserverManager.makeWarningToast("Please select the number simultanious threads to use.");
			return;
		}
		
		// Ask user for confirmation - note, running this will cancel all other simulations!
		if (!ObserverManager.showConfirmationMessage("Confirmation required", 
				"This will start the retraining of "  + selectedShadows.size()+ " agents. Do you want to continue?"+
						"\n\nIMPORTANT: These changes will apply to ALL agents that are selected in the table (left-most column)"+
						"\n\nIMPORTANT: starting retraining will cancel all active simulation. If there is an active simulation, progress might be lost! ",
						"Yes",
						"No",
						null)) {
			ObserverManager.makeToast("Simulation aborted.");
		}
		
		ObserverManager.makeToast("Preparing retraining...");
		
		// Determine if the model is running another simulation and cancel it if it is
		frame.model.cancelSimulation();
		
		// Compute the parameters required
		AgentType newAgentType = comboBoxAgentSelection.getValue();
		
		boolean useStoppingCriteriaTime = this.checkBoxStoppingCriteriaTime.isSelected();
		double maximumTime;
		if (useStoppingCriteriaTime) maximumTime = Double.parseDouble(this.textFieldStoppingCriteriaTime.getText());
		else {maximumTime = 0;}
		TimeUnit maximumTimeUnit = this.comboBoxStoppingCriteriaTimeUnits.getValue();
		
		boolean useStoppingCriteriaConvergence = this.checkBoxStoppingCriteriaConvergence.isSelected();
		DecimalNumber epsilon;
		if (useStoppingCriteriaConvergence) epsilon = new DecimalNumber(this.textFieldStoppingCriteriaConvergence.getText());
		else epsilon = new DecimalNumber(-1);
		
		boolean useStoppingCriteriaIterations = this.checkBoxAdditionalIterations.isSelected();
		int additionalIterations = this.spinnerIterations.getValue();
		
		Optimizer newOptimizer = this.comboBoxOptimizer.getValue();
		DecimalNumber indifferencePoint = new DecimalNumber(textFieldIndifferencePoint.getText());
		boolean useBatchUpdating = this.checkBoxBatchUpdating.isSelected();
		
		// Ask to the model to retrain - this will take some time, so do it on a new thread
		RetrainingSpecifications specs = new RetrainingSpecifications(	
				textFieldFitnessFunction.getText(),
				
				newAgentType,
				useStoppingCriteriaTime,
				maximumTime,
				maximumTimeUnit,
				useStoppingCriteriaConvergence,
				epsilon,
				useStoppingCriteriaIterations,
				additionalIterations,
				
				newOptimizer,
				indifferencePoint,
				useBatchUpdating);
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				frame.model.retrainAgents(
						specs,
						
						outputFolder, 
						
						spinnerThreads.getValue(),
						selectedShadows 
					
						);
				
			}
			
		});
		t.start();
		
	}
}
