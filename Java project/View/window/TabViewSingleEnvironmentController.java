package window;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import ValueIteratorPolicyPlot.PolicyPlot_ValueIterator;
import ValueIteratorPolicyPlot.PolicyPlot_ValueIterator.ConstructionPhase;
import ValueIteratorPolicyPlot.PolicyPlot_ValueIterator.ShowingCycle;
import agent.AgentType;
import agent.Output;
import agent.ValueIterator.ValueIteratorOutput;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import decimalNumber.DecimalNumberMatrixTableView;
import defaultAndHelper.ColorPalette;
import defaults.Defaults;
import environment.Environment;
import environment.ValueDistributionType;
import helper.Helper;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import markovDecisionProcess.MarkovDecisionProcess;
import staticManagers.ObserverManager;
import staticManagers.OutputShadow;
import window.interfacesAndAbstractions.AbstractPolicyPlot;
import window.interfacesAndAbstractions.AbstractTab;
import window.interfacesAndAbstractions.LayoutManager;
import window.interfacesAndAbstractions.LayoutManager.TextFieldValidInputCriterium;

public class TabViewSingleEnvironmentController extends AbstractTab{

	///////////////////////////////////////////////////////////////////////////////////
	//////////////////////////	View single environment tab	///////////////////////////
	///////////////////////////////////////////////////////////////////////////////////
	@FXML public	TextField					textfieldFolderModelOutput;
	@FXML public 	TextField					textfieldCSVFilename;
	@FXML public	Button						buttonBrowseFolderModelOutput;
	@FXML public 	Button						buttonRefreshFolderModelOutput;
	@FXML public    CheckBox					checkBoxSubDirectories;
	
	@FXML public 	TextField					textFieldFilterResourceMean;
	@FXML public 	TextField					textFieldFilterExtrinsicMean;
	@FXML public 	TextField					textFieldFilterResourceSD;
	@FXML public 	TextField					textFieldFilterExtrinsicSD;
	@FXML public 	TextField					textFieldFilterInterruptionRate;

	@FXML public	TableView<OutputShadow> 	tableviewOutputShadows;
	@FXML public	TextArea					textareaMDP;

	@FXML public 	AnchorPane					anchorPaneTitledPaneOptimalPolicy;
	@FXML public 	Spinner<Double>				spinnerBudget;
	@FXML public  	AnchorPane					anchorPanePolicyPlot;
	@FXML public 	ToggleButton				toggleButtonFitness;
	@FXML public 	ToggleButton				toggleButtonValue;
	@FXML public 	ToggleButton				toggleButtonForward;
	@FXML public 	ToggleButton				toggleButtonBackward;
	@FXML public 	ToggleButton				toggleButtonForwardPruning;
	@FXML public	CheckBox					checkBoxScaleEdges;
	@FXML public 	CheckBox					checkBoxScaleNodes;
	@FXML public 	CheckBox					checkBoxShowUnusedEdges;
	@FXML public 	CheckBox					checkBoxCenterLine;
	@FXML public 	CheckBox					checkBoxDescriptionText;
	@FXML public 	CheckBox					checkBoxPrintTerminalOutcomes;
	@FXML public 	ToggleButton				toggleButtonShowImmediateOutcomes;
	@FXML public 	ToggleButton				toggleButtonShowFutureOutcomes;
	public	AbstractPolicyPlot					policyPlot;

	@FXML public 	Button						buttonShowEnvironment;
	@FXML public 	Button						buttonShowCuesSampled;
	@FXML public	Button						buttonShowValueFunction;
	@FXML public 	Button						buttonShowLegend;
	@FXML public	Button 						buttonExportResults;
	@FXML public 	Button						buttonOpenNewWindow;
	@FXML public 	Button						buttonSavePlot;
	
	private final int								significantDigits = 5;
	private final 	ObservableList<OutputShadow> 	shadowsDisplayed;
	private final   ArrayList<OutputShadow> 		shadows;
	private OutputShadow 							selectedOutputShadow;
	private Output									selectedOutput;

	public TabViewSingleEnvironmentController(FrameController fc) {
		super(fc, "paneViewSingleEnvironment.fxml");
		this.shadowsDisplayed = FXCollections.observableArrayList();
		this.shadows = new ArrayList<>();
	}

	@Override
	public void setNodes() {
		// If the browse button is pressed: prompt the user for a new directory to save the .csv in
		this.buttonBrowseFolderModelOutput.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				DirectoryChooser dc = new DirectoryChooser();
				File directory = dc.showDialog(frame.stage);
				if (directory != null) {
					textfieldFolderModelOutput.setText(directory.getAbsolutePath());
					inputFolderChanged(directory);
				}
			}
		});

		// If the refresh button is pressed: reread the specified input folder
		this.buttonRefreshFolderModelOutput.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				inputFolderChanged(new File(textfieldFolderModelOutput.getText()));
			}
		});

		// Set the allowable values for the filter text fields
		LayoutManager.setLayoutHandler(textFieldFilterResourceMean, TextFieldValidInputCriterium.DOUBLE_ALLOWING_EMPTY); 
		LayoutManager.setLayoutHandler(textFieldFilterExtrinsicMean, TextFieldValidInputCriterium.DOUBLE_ALLOWING_EMPTY); 
		LayoutManager.setLayoutHandler(textFieldFilterResourceSD, TextFieldValidInputCriterium.NON_NEGATIVE_DOUBLE_ALLOWING_EMPTY); 
		LayoutManager.setLayoutHandler(textFieldFilterExtrinsicSD, TextFieldValidInputCriterium.NON_NEGATIVE_DOUBLE_ALLOWING_EMPTY); 
		LayoutManager.setLayoutHandler(textFieldFilterInterruptionRate, TextFieldValidInputCriterium.PROBABILITY_ALLOWING_EMPTY); 

		// Set the behaviour of the filter text fields: when a valid change in registered, call applyFilter()
		textFieldFilterResourceMean.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldFilterResourceMean)) {
					LayoutManager.setProcessed(textFieldFilterResourceMean);
					applyFilter();
				}}});
		textFieldFilterExtrinsicMean.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldFilterExtrinsicMean)) {
					LayoutManager.setProcessed(textFieldFilterExtrinsicMean);
					applyFilter();
				}}});
		textFieldFilterResourceSD.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldFilterResourceSD)) {
					LayoutManager.setProcessed(textFieldFilterResourceSD);
					applyFilter();
				}}});
		textFieldFilterExtrinsicSD.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldFilterExtrinsicSD)) {
					LayoutManager.setProcessed(textFieldFilterExtrinsicSD);
					applyFilter();
				}}});
		textFieldFilterInterruptionRate.focusedProperty().addListener(new ChangeListener<Boolean>() {
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldFilterInterruptionRate)) {
					LayoutManager.setProcessed(textFieldFilterInterruptionRate);
					applyFilter();
				}}});
		
		// Set all table view stuff
		this.setTableView();

		// Set the spinner
		LayoutManager.setLayoutHandler(spinnerBudget.getEditor(), LayoutManager.TextFieldValidInputCriterium.NON_NEGATIVE_DOUBLE);
		spinnerBudget.valueProperty().addListener(new ChangeListener<Double>(){
			@Override
			public void changed(ObservableValue<? extends Double> observable, Double oldValue, Double newValue) {
				DecimalNumber valueOnSpinner = new DecimalNumber(newValue);
				
				if (valueOnSpinner.isDivisibleBy(selectedOutput.mdp.BUDGET_STEP)) {
					LayoutManager.setProcessed(spinnerBudget.getEditor());
					setPolicyPlot();
				} else
					System.err.println("Not equal - rounding issue");//spinnerBudget.getValueFactory().setValue(oldValue);
			}});
		// Commit when the user pressed enter
		spinnerBudget.getEditor().setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				spinnerBudget.getParent().requestFocus();
			}
		});


		// Set the action handler for all togglebuttons: redraw when state changes to true
		toggleButtonFitness.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			if (newValue)
				setPolicyPlot();
		}});
		toggleButtonValue.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			if (newValue)
				setPolicyPlot();
		}});
		toggleButtonForward.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			if (newValue)
				setPolicyPlot();
		}});
		toggleButtonBackward.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			if (newValue)
				setPolicyPlot();
		}});
		toggleButtonForwardPruning.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			if (newValue)
				setPolicyPlot();
		}});
		
		// Make sure that always one of the terminal fitness button or the expected value button is pressed
		// In addition, when the mode changes, reset the policy plot
		toggleButtonFitness.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			toggleButtonValue.setSelected(oldValue);
			setPolicyPlot();
		}});
		toggleButtonValue.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			toggleButtonFitness.setSelected(oldValue);
			setPolicyPlot();
		}});
		
		// If the checkBoxScaleNodes, checkBoxScaleEdges or the showUnusedEdge checkboxes are changed, redraw the plot
		checkBoxScaleNodes.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				setPolicyPlot();
			}
		});
		checkBoxScaleEdges.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				setPolicyPlot();
			}
		});
		checkBoxShowUnusedEdges.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				setPolicyPlot();
			}
		});
		checkBoxCenterLine.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				setPolicyPlot();
			}
		});
		checkBoxDescriptionText.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				setPolicyPlot();
			}
		});
		checkBoxPrintTerminalOutcomes.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				if (arg2) {
					toggleButtonShowFutureOutcomes.setDisable(false);
					toggleButtonShowImmediateOutcomes.setDisable(false);
				} else {
					toggleButtonShowFutureOutcomes.setDisable(true);
					toggleButtonShowImmediateOutcomes.setDisable(true);
				}
				setPolicyPlot();
			}
		});
		toggleButtonShowImmediateOutcomes.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			setPolicyPlot();
		}});
		toggleButtonShowFutureOutcomes.selectedProperty().addListener(new ChangeListener<Boolean>() { @Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			setPolicyPlot();
		}});
		
		
		// Set the action handler for the buttonExportResults
		this.buttonExportResults.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				ObserverManager.makeToast("Trying to save file to " + textfieldCSVFilename.getText());

				Runnable r = new Runnable(){public void run() {
					frame.model.writeCSV(textfieldFolderModelOutput.getText(), textfieldCSVFilename.getText(), frame.model.OutputShadowToOutput(tableviewOutputShadows.getItems().get(0)).mdp.CUE_LABELS);
				}
				};
				new Thread(r).start();
			}
		});

		// Set the buttonShowEnvironment event listener
		buttonShowEnvironment.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if (selectedOutput != null)
					new EnvironmentPopup(frame, selectedOutput.mdp, selectedOutput.environment, 400.0, 500.0, significantDigits);

			}
		});		


		// Set the buttonShowCueSampled event listener
		buttonShowCuesSampled.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if (selectedOutput != null) {
					DecimalNumberArray budgets = selectedOutput.finalValueFunction.getBudgets();
					DecimalNumberArray cuesSampled = selectedOutput.expectedCuesSampled;
					DecimalNumberMatrix m = new DecimalNumberMatrix(budgets.length(), 2);
					m.setColumn(0, budgets);
					m.setColumn(1, cuesSampled);
					m.setColumnNames("Budget", "Cues sampled");
					
					new DecimalNumberMatrixTableView.DecimalNumberMatrixPopup(frame, m, 500.0, 500.0, significantDigits, true);
				}
			}
		});
		// Set the buttonShowValueFunction event listener
		buttonShowValueFunction.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if (selectedOutput != null)
					new DecimalNumberMatrixTableView.DecimalNumberMatrixPopup(frame, selectedOutput.finalValueFunction.toDecimalNumberMatrix(), 500.0, 500.0, significantDigits, true);

			}
		});		
	
		// Set the Open in new Window button
		buttonOpenNewWindow.setVisible(false);
		buttonOpenNewWindow.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				DecimalNumber startingBudget = new DecimalNumber(spinnerBudget.getEditor().getText());
				SecondaryStageController_PolicyPlotPopup newWindow = new SecondaryStageController_PolicyPlotPopup("Decision tree with starting budget of " + startingBudget.toString(3));
				if (selectedOutput.agentType == AgentType.VALUE_ITERATOR) {
					// First, determine the cycle to show
					ShowingCycle sc;
					if (toggleButtonFitness.isSelected())
						 sc = ShowingCycle.FITNESS;
					else if (toggleButtonValue.isSelected())
						sc = ShowingCycle.VALUE;
					else
						throw new IllegalStateException("Error in setPolicyPlot(): unknown cycle selected");		
					// Next, determine the construction phase of the tree
					ConstructionPhase cp;
					if (toggleButtonForward.isSelected())
						 cp = ConstructionPhase.FORWARD_PASS;
					else if (toggleButtonBackward.isSelected())
						cp = ConstructionPhase.BACKWARD_PASS;
					else if (toggleButtonForwardPruning.isSelected())
						cp = ConstructionPhase.FORWARD_PRUNING_PASS;
					else
						throw new IllegalStateException("Error in setPolicyPlot(): unknown construction phase selected");
					
					try {		
						PolicyPlot_ValueIterator newPolicyPlot = new PolicyPlot_ValueIterator(
							frame,
							newWindow.anchorScrollPane, 
							newWindow.anchorScrollPane,
							newWindow.anchorScrollPane, 
							(ValueIteratorOutput) selectedOutput,
							startingBudget,
							cp, 
							sc,
							checkBoxScaleNodes.isSelected(),
							checkBoxScaleEdges.isSelected(),
							checkBoxShowUnusedEdges.isSelected(),
							checkBoxCenterLine.isSelected(),
							checkBoxDescriptionText.isSelected(),
							checkBoxPrintTerminalOutcomes.isSelected(),
							toggleButtonShowImmediateOutcomes.isSelected(),
							toggleButtonShowFutureOutcomes.isSelected());
						newWindow.setPolicyPlot(newPolicyPlot);
						} catch (Exception e) {	ObserverManager.notifyObserversOfError(e);			}

				}
			}
			
		});
		
		// Set the save to file button
		buttonSavePlot.setVisible(false);
		buttonSavePlot.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				if (selectedOutput == null) {
					ObserverManager.makeWarningToast("Please select an output file first.");
					return;
				}
				
				if (policyPlot == null) {
					ObserverManager.makeWarningToast("Please plot a tree first.");
					return;
				}
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save plot image as...");
				ExtensionFilter filter	    = new FileChooser.ExtensionFilter("Image Files", "*.png");
				fileChooser.getExtensionFilters().add(filter);
				
				File file = fileChooser.showSaveDialog(frame.stage);
				
				if (file == null)
					return;
				
				// Get a WritableImage that we can save
				policyPlot.writeToFile(file);
			}
			
		});
	}


	private void setTableView() {
		DoubleBinding columnWidthFullySpecified = tableviewOutputShadows.widthProperty().subtract(2).divide(7);
		// Create and add the Resource quality super-column to the table view.
		TableColumn<OutputShadow, TableColumn<OutputShadow, Object>> resourceValueColumn = new TableColumn<OutputShadow, TableColumn<OutputShadow, Object>>("Resource quality");

		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "resourceValueMean", columnWidthFullySpecified, significantDigits, frame, tableviewOutputShadows));
		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "resourceValueSD", columnWidthFullySpecified,significantDigits, frame, tableviewOutputShadows ));
		resourceValueColumn.getColumns().add(createValueDistributionTypeColumn("Distribution Type", "resourceValueDistributionType", columnWidthFullySpecified));
		tableviewOutputShadows.getColumns().add(resourceValueColumn);

		// Create and add the extrinsic event super-column to the table view.
		TableColumn<OutputShadow, TableColumn<OutputShadow, Object>> extrinsicEventColumn = new TableColumn<OutputShadow, TableColumn<OutputShadow, Object>>("Extrinsic events");
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "extrinsicEventMean", columnWidthFullySpecified, significantDigits, frame, tableviewOutputShadows));
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "extrinsicEventSD",columnWidthFullySpecified,significantDigits, frame, tableviewOutputShadows));
		extrinsicEventColumn.getColumns().add(createValueDistributionTypeColumn("Distribution Type", "extrinsicEventDistributionType",columnWidthFullySpecified));
		tableviewOutputShadows.getColumns().add(extrinsicEventColumn);

		// Add the interruption rate column to the table view
		tableviewOutputShadows.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Interruption rate", "interruptionRate", columnWidthFullySpecified,significantDigits, frame, tableviewOutputShadows));

		// Add the items
		tableviewOutputShadows.setItems(shadowsDisplayed);

		// Set the selection model listener
		tableviewOutputShadows.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<OutputShadow>(){

			@Override
			public void changed(ObservableValue<? extends OutputShadow> observable, OutputShadow oldValue,
					OutputShadow newValue) {
				if (newValue != null)
				{
					selectedOutputShadow = newValue;
					selectedOutput = frame.model.OutputShadowToOutput(selectedOutputShadow);

					textareaMDP.setText(
							"Conditions of termination\n" + Helper.repString("-", 50)+
							"\nFinal delta:                                    " + selectedOutput.finalDelta + 
							"\nIterations completed:                   " + selectedOutput.finalIteration +
							"\n\n\n" +Helper.repString("-", 50)+ 
							"\n Markov Decision Process: " + selectedOutput.mdp.toString()+
							"\n\n\n" +Helper.repString("-", 50)+ 
							"\n Estimation parameters " + selectedOutput.estimationParameters);
					spinnerBudget.setValueFactory( new SpinnerValueFactory.DoubleSpinnerValueFactory(0, selectedOutput.mdp.BUDGET_MAXIMUM.doubleValue(), 0, selectedOutput.mdp.BUDGET_STEP.doubleValue()));
					anchorPanePolicyPlot.getChildren().removeAll(anchorPanePolicyPlot.getChildren());
				}

			}
		});

	}

	/** Draw the policy plot for the agent that starting with startingBudget. 
	 * (this function is parameterized because, when moving the slider, the slider
	 * can have values between tick marks.)
	 * @param startingBudget
	 */
	private void setPolicyPlot() {
		if (selectedOutput == null) {
			buttonOpenNewWindow.setVisible(false);
			buttonSavePlot.setVisible(false);
			return;
		}
		DecimalNumber startingBudget = new DecimalNumber(spinnerBudget.getValue());
		
		buttonOpenNewWindow.setVisible(true);
		buttonSavePlot.setVisible(true);
		// If required: destroy the old policy plot
		if (policyPlot != null) {
			policyPlot.destroy();
			System.gc();}
		anchorPanePolicyPlot.getChildren().removeAll(anchorPanePolicyPlot.getChildren());

		if (selectedOutput.agentType == AgentType.VALUE_ITERATOR) {
			// First, determine the cycle to show
			ShowingCycle sc;
			if (toggleButtonFitness.isSelected())
				 sc = ShowingCycle.FITNESS;
			else if (toggleButtonValue.isSelected())
				sc = ShowingCycle.VALUE;
			else
				throw new IllegalStateException("Error in setPolicyPlot(): unknown cycle selected");
			
			// Next, determine the construction phase of the tree
			ConstructionPhase cp;
			if (toggleButtonForward.isSelected())
				 cp = ConstructionPhase.FORWARD_PASS;
			else if (toggleButtonBackward.isSelected())
				cp = ConstructionPhase.BACKWARD_PASS;
			else if (toggleButtonForwardPruning.isSelected())
				cp = ConstructionPhase.FORWARD_PRUNING_PASS;
			else
				throw new IllegalStateException("Error in setPolicyPlot(): unknown construction phase selected");
			
			try {	
				policyPlot = new PolicyPlot_ValueIterator(
						frame,
						frame.anchorPaneScrollPane, 
						anchorPaneTitledPaneOptimalPolicy,
						anchorPanePolicyPlot, 
						(ValueIteratorOutput) selectedOutput,
						startingBudget,
						cp, 
						sc,
						checkBoxScaleNodes.isSelected(),
						checkBoxScaleEdges.isSelected(),
						checkBoxShowUnusedEdges.isSelected(),
						checkBoxCenterLine.isSelected(),
						checkBoxDescriptionText.isSelected(),
						checkBoxPrintTerminalOutcomes.isSelected(),
						toggleButtonShowImmediateOutcomes.isSelected(),
						toggleButtonShowFutureOutcomes.isSelected());
			} catch (Exception e) {	ObserverManager.notifyObserversOfError(e);			}
		}

	}

	/**
	 * Create and return a new TableColumn that houses ValueDistributionType 
	 * @param <T>
	 * @param header
	 * @param variableName
	 * @param width
	 * @return
	 */
	public static TableColumn<OutputShadow, ValueDistributionType> createValueDistributionTypeColumn (String header, String variableName, DoubleBinding width)
	{
		TableColumn<OutputShadow, ValueDistributionType> newCol = new TableColumn<>(header);
		newCol.setCellValueFactory(new PropertyValueFactory<OutputShadow, ValueDistributionType>(variableName));
		newCol.setCellFactory(ComboBoxTableCell.<OutputShadow, ValueDistributionType>forTableColumn((ValueDistributionType.values())));
		newCol.prefWidthProperty().bind(width);
		return newCol;
	}

	@Override
	public void update() {
		this.textfieldFolderModelOutput.setText(Defaults.defaultInputFolderSingleEnvironment);
		this.inputFolderChanged(new File(Defaults.defaultInputFolderSingleEnvironment));

	}

	/**
	 * Call this after the directory for the input folder has changed: it makes the model read in all the results in the folder.
	 * Since this IO can be rather slow, it is executed on a different thread.
	 */
	private void inputFolderChanged(File newDirectory)
	{
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

		// Ask the model to read in all the .out files and get their shadows.
		//		However, this task will take quite a while, and hence should be run on a different thread than the JavaFX threat.
		Task<Void> task = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				shadows.addAll(frame.model.createOutputShadows(newDirectory, checkBoxSubDirectories.isSelected()));

				Platform.runLater(new Runnable(){
					public void run() {
						//Set the placeholder for the table to indicate that there are no SavedResults (this is only shown if there are, in fact, no results to be shown).
						tableviewOutputShadows.setPlaceholder(new Label("No agents are stored in the folder or none that match the filter"));
						
						// Update the view
						applyFilter();
						
						// Automatically select the first
						if (shadows.size()>0)
							tableviewOutputShadows.getSelectionModel().selectFirst();
					}
				});

				return null;
			}

		};
		new Thread(task).start();
	}

	/** Places all OutputShadows contained in shadows that match the filters (if any) into the shadowsDisplays, which are shown in the TableView */
	private void applyFilter() {
		// Remove all old elements shown in the TableView
		shadowsDisplayed.removeAll(shadowsDisplayed);
		
		// Add all OutputShadows to shadowsDisplayed
		shadowsDisplayed.addAll(shadows);
		// Apply filters: remove entries that do not agree with the specified filter(s)
		// Filter 1: resource mean
		if (LayoutManager.isNormal(textFieldFilterResourceMean) && textFieldFilterResourceMean.getText().length()>0) {
			double value = Double.parseDouble(textFieldFilterResourceMean.getText());
			for (Iterator<OutputShadow> it = shadowsDisplayed.iterator(); it.hasNext();){
				OutputShadow s = it.next();
				if (!s.resourceValueMean.equals(value, true))		it.remove();
			}
		}
		
		// Filter 2: resource SD
		if (LayoutManager.isNormal(textFieldFilterResourceSD) && textFieldFilterResourceSD.getText().length()>0) {
			double value = Double.parseDouble(textFieldFilterResourceSD.getText());
			for (Iterator<OutputShadow> it = shadowsDisplayed.iterator(); it.hasNext();){
				OutputShadow s = it.next();
				if (!s.resourceValueSD.equals(value, true))		it.remove();
			}
		}
		
		// Filter 3: extrinsic mean
		if (LayoutManager.isNormal(textFieldFilterExtrinsicMean) && textFieldFilterExtrinsicMean.getText().length()>0) {
			double value = Double.parseDouble(textFieldFilterExtrinsicMean.getText());
			for (Iterator<OutputShadow> it = shadowsDisplayed.iterator(); it.hasNext();){
				OutputShadow s = it.next();
				if (!s.extrinsicEventMean.equals(value, true))		it.remove();
			}
		}
		
		// Filter 4: extrinsic SD
		if (LayoutManager.isNormal(textFieldFilterExtrinsicSD) && textFieldFilterExtrinsicSD.getText().length()>0) {
			double value = Double.parseDouble(textFieldFilterExtrinsicSD.getText());
			for (Iterator<OutputShadow> it = shadowsDisplayed.iterator(); it.hasNext();){
				OutputShadow s = it.next();
				if (!s.extrinsicEventSD.equals(value, true))		it.remove();
			}
		}
		
		// Filter 5: interruption rate
		if (LayoutManager.isNormal(textFieldFilterInterruptionRate) && textFieldFilterInterruptionRate.getText().length()>0) {
			double value = Double.parseDouble(textFieldFilterInterruptionRate.getText());
			for (Iterator<OutputShadow> it = shadowsDisplayed.iterator(); it.hasNext();){
				OutputShadow s = it.next();
				if (!s.interruptionRate.equals(value, true))		it.remove();
			}
		}
				
		// Automatically select the first
		if (shadows.size()>0)
			tableviewOutputShadows.getSelectionModel().selectFirst();
		
		// Show the user the result
		tableviewOutputShadows.refresh();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// 	Popup window for environments 	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static class EnvironmentPopup implements EventHandler<MouseEvent>
	{
		private final MarkovDecisionProcess					mdp;
		private final Environment 							environment;

		@FXML public AnchorPane 							anchorPaneMainPane;
		@FXML public BorderPane								buttonPane;
		@FXML public Button									buttonClose;
		@FXML public Accordion						    	accordion;
		@FXML public TitledPane								titledPaneData;
		@FXML public AnchorPane								anchorPaneData;
		@FXML public TitledPane								titledPanePlot;
		@FXML public AnchorPane								anchorPanePlot;
		@FXML public DecimalNumberMatrixTableView			tableView;
		@FXML public AreaChart<DecimalNumber, DecimalNumber>areaChart;
		@FXML public NumberAxis								areaChartX;
		@FXML public NumberAxis								areaChartY;
		public Stage stage;
		public Scene scene;
		/**
		 * @param frame			The main window
		 * @param matrix		The matrix to show
		 * @param height		The height of the 
		 * @param width
		 * @param significantDigits
		 * @param readOnly
		 */
		public EnvironmentPopup(FrameController frame, MarkovDecisionProcess mdp, Environment environment, double width, double height, int significantDigits)
		{
			this.mdp=mdp;
			this.environment = environment;

			// Retrieve the anchorPane from the FXML file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("popupEnvironment.fxml"));
			loader.setController(this);
			Parent root = null;
			try {root = loader.load();} catch (IOException e) {ObserverManager.notifyObserversOfError(e);}


			scene = new Scene(root, width, height);
			stage = new Stage();
			stage.setTitle("");
			stage.initStyle(StageStyle.TRANSPARENT);
			scene.setFill(Color.TRANSPARENT);
			stage.setScene(scene);
			stage.show();

			// Make the scene movable
			AnchorPane[] anchorPanesRequiringMouseListener = new AnchorPane[] {anchorPanePlot, anchorPaneData, anchorPaneMainPane};
			for (AnchorPane a: anchorPanesRequiringMouseListener) {
				a.setOnMousePressed(this);
				a.setOnMouseDragged(this);
			}
			accordion.setOnMousePressed(this);
			accordion.setOnMouseDragged(this);

			// Initialize the table view
			DecimalNumberMatrix clone = environment.getMatrix().clone();
			this.tableView.initializeTableView(clone, 4, true, 100, null, null, null);
			tableView.setColumnName(1, "p(Resource)");
			tableView.setColumnName(2, "p(Extrinsic)");
			// Set the button
			buttonClose.setOnAction(new EventHandler<ActionEvent>(){@Override public void handle(ActionEvent event) {	stage.close();}});

			this.titledPanePlot.setAnimated(false);
			accordion.setExpandedPane(titledPanePlot);
			drawPlot();


		}


		private void drawPlot()
		{
			// Set all the axes
			areaChartX.setAutoRanging(false);
			areaChartX.setLowerBound(-1*mdp.VALUE_MAXIMUM.doubleValue());
			areaChartX.setUpperBound(mdp.VALUE_MAXIMUM.doubleValue());
			areaChartX.setTickUnit(mdp.VALUE_MAXIMUM.doubleValue()/4);
			areaChartX.setLabel("Value");

			areaChartY.setAutoRanging(true);
			areaChartY.setLabel(" ");
			
			areaChart.setCreateSymbols(false);
			areaChart.setAnimated(false);
			
			DecimalNumberArray values = environment.getMatrix().getColumn(0);
			DecimalNumberArray resourceProbabilities =   environment.getMatrix().getColumn(1);
			DecimalNumberArray extrinsicEventProbabilities =  environment.getMatrix().getColumn(2);

			// Add the new data point to the plot
			//		Resource values
			Series<DecimalNumber, DecimalNumber> resourceValueSeries = new Series<>();
			resourceValueSeries.setName("Resource value");
			for (int i = 0; i < values.length(); i++)
				resourceValueSeries.getData().add(new Data<>(values.get(i), resourceProbabilities.get(i)));

			//	extrinsic events
			Series<DecimalNumber, DecimalNumber> extrinsicEventSeries = new Series<>();
			extrinsicEventSeries.setName("Extrinsic event");
			for (int i = 0; i < values.length(); i++)
				extrinsicEventSeries.getData().add(new Data<>(values.get(i), extrinsicEventProbabilities.get(i)));

			// Add both lines to the plot
			areaChart.getData().addAll(resourceValueSeries, extrinsicEventSeries);

			// Restyle the Resource quality line. Note that we do not change the legend color here, that is done in the .css file!
			Color fillColor = ColorPalette.addOpacity(ColorPalette.resourceValuesColorFill, ColorPalette.resourceValueColorMaximumOpacity * ((1-environment.interruptionRate.doubleValue())));
			Color lineColor = ColorPalette.addOpacity(ColorPalette.resourceValuesColorLine, ColorPalette.resourceValueColorMaximumOpacity * ((1-environment.interruptionRate.doubleValue())));
			resourceValueSeries.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill: " + ColorPalette.toFXMLString(fillColor) + ";");
			resourceValueSeries.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke: "+   ColorPalette.toFXMLString(lineColor) + ";");

			fillColor = ColorPalette.extrinsicEventValueColorFill;
			lineColor = ColorPalette.extrinsicEventValueColorLine;
			extrinsicEventSeries.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill: " + ColorPalette.toFXMLString(fillColor) + ";");
			extrinsicEventSeries.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke: "+   ColorPalette.toFXMLString(lineColor) + ";");

			// Finally, add the legend 
			// The legend does not immediately take over these colors specified above - with the code above
			// 		all the lines in the plot are on a red-blue gradient, but the legend colors do not match.
			// Interestingly enough, the legend is only added to the plot after the full plot has been drawn. This
			// 		means we cannot immediately change the colors in the legend. A somewhat cheesy workaround
			//		is to change them a little later - using Platform's runLater.
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {					redraw();
					} catch (Exception e) {
						new Runnable() { public void run() { try {
							Thread.sleep(250);
							Platform.runLater( new Runnable() { public void run (){try {redraw();} catch(Exception e) {}}});
						}catch(Exception e) {}}}.run();

					}
				}
				private void redraw() throws Exception  {
					try {
						Object[] nodes = areaChart.lookupAll(".chart-legend-item-symbol").toArray();
						((Node) nodes[0]).setStyle("-fx-background-color: " +ColorPalette.toFXMLString(ColorPalette.resourceValuesColorLine) + ";");
						((Node) nodes[1]).setStyle("-fx-background-color: " +ColorPalette.toFXMLString(ColorPalette.extrinsicEventValueColorLine) + ";");
					}
					catch (Exception e) {  throw e; }
				}
			});


			// Make sure the plot is visible
			areaChart.setVisible(true);

		}


		double offsetX;
		double offsetY;

		@Override
		public void handle(MouseEvent event) {
			// If the mouse is pressed, record where the mouse is
			if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
				offsetX = scene.getWindow().getX() - event.getScreenX();
				offsetY = scene.getWindow().getY() - event.getScreenY();
			}

			if (event.getEventType() == MouseEvent.MOUSE_DRAGGED){
				stage.setX(event.getScreenX() + offsetX);
				stage.setY(event.getScreenY() + offsetY);
			}
		}



	}


}
