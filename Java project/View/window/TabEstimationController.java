package window;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

import agent.AgentType;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import defaults.Defaults;
import estimationParameters.Optimizer;
import helper.Helper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import staticManagers.ObserverManager;
import window.interfacesAndAbstractions.AbstractTab;
import window.interfacesAndAbstractions.LayoutManager;
import window.interfacesAndAbstractions.LayoutManager.TextFieldValidInputCriterium;

public class TabEstimationController extends AbstractTab{

	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////////// 	ESTIMATION TAB		////????/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	//////////Estimation - FXML Nodes
	@FXML public ComboBox<AgentType>	comboBoxAgentSelection;
	@FXML public CheckBox 				checkBoxStoppingCriteriaTime;
	@FXML public CheckBox				checkBoxStoppingCriteriaConvergence;
	@FXML public CheckBox				checkBoxStoppingCriteriaIterations;
	@FXML public ComboBox<TimeUnit>		comboBoxStoppingCriteriaTimeUnits;
	@FXML public TextField				textFieldStoppingCriteriaTime;
	@FXML public TextField				textFieldStoppingCriteriaConvergence;
	@FXML public TextField				textFieldStoppingCriteriaIterations;
	@FXML public TextField				textFieldStartingEstimates;

	@FXML public ComboBox<Optimizer>	comboBoxOptimizer;
	@FXML public TextField				textFieldIndifferencePoint;
	@FXML public CheckBox				checkBoxBatchUpdating;
	@FXML public TextField				textFieldOutputFolder;
	@FXML public Button 				buttonBrowseOutputFolder;
	@FXML public Spinner<Integer> 		spinnerSimultaniousThreads;



	DecimalFormat df = new DecimalFormat("#.###############################################################");
	
	
	public TabEstimationController(FrameController fc) {
		super(fc, "paneEstimation.fxml");
	}

	@Override
	public void update() {
		comboBoxAgentSelection.setValue(frame.estimationBuilder.getAgentType());
		
		checkBoxStoppingCriteriaTime.setSelected(frame.estimationBuilder.useStoppingCriteriaTime());
		checkBoxStoppingCriteriaConvergence.setSelected(frame.estimationBuilder.useStoppingCriteriaConvergence());
		checkBoxStoppingCriteriaIterations.setSelected(frame.estimationBuilder.useStoppingCriteriaIterations());
		comboBoxStoppingCriteriaTimeUnits.setValue(frame.estimationBuilder.getStoppingCriteriaTimeUnit());
		
		if (frame.estimationBuilder.getStoppingCriteriaTimeMaximum() != null)
			textFieldStoppingCriteriaTime.setText(df.format(frame.estimationBuilder.getStoppingCriteriaTimeMaximum()));
		else
			textFieldStoppingCriteriaTime.setText("");
		
		if (frame.estimationBuilder.getStoppingCriteriaConvergenceEpsilon() != null)
			textFieldStoppingCriteriaConvergence.setText(df.format(frame.estimationBuilder.getStoppingCriteriaConvergenceEpsilon()));
		else
			textFieldStoppingCriteriaConvergence.setText("");
		
		textFieldStoppingCriteriaIterations.setText(df.format(frame.estimationBuilder.getStoppingCriteriaIterationsMaximum()));

		comboBoxOptimizer.setValue(frame.estimationBuilder.getOptimizer());
		textFieldIndifferencePoint.setText(df.format(frame.estimationBuilder.getIndifferencePoint()));
		textFieldStartingEstimates.setText(df.format(frame.estimationBuilder.getStartingEstimates()));
		checkBoxBatchUpdating.setSelected(frame.estimationBuilder.getBatchUpdating());
		textFieldOutputFolder.setText(frame.estimationBuilder.getOutputFolder().getAbsolutePath());

		spinnerSimultaniousThreads.getEditor().setText(df.format(frame.estimationBuilder.getNumberOfSimultaniousThreads()));
		
	}

	@Override
	public void setNodes() {
		// Set the comboBox for selecting the agent type
		comboBoxAgentSelection.getItems().addAll(AgentType.values());
		comboBoxAgentSelection.valueProperty().addListener(new ChangeListener<AgentType>(){
			@Override
			public void changed(ObservableValue<? extends AgentType> observable, AgentType oldValue,
					AgentType newValue) {
				frame.estimationBuilder.setAgentType(newValue);
			}});
		
		// Stopping criteria: MAXIMUM TIME. Set the defaults and the change listeners of (a) the checkBoxes, and
		// 	(b) the textField, and (c) the comboBox for time units.
		LayoutManager.setLayoutHandler(textFieldStoppingCriteriaTime, TextFieldValidInputCriterium.POSITIVE_DOUBLE); 
		checkBoxStoppingCriteriaTime.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				frame.estimationBuilder.setUseStoppingCriteriaTime(newValue);
				textFieldStoppingCriteriaTime.setDisable(!newValue);
				comboBoxStoppingCriteriaTimeUnits.setDisable(!newValue);
				// After re-abling the TextField: place the focus on that field
				if (!textFieldStoppingCriteriaTime.isDisable()) {textFieldStoppingCriteriaTime.requestFocus();}
		}});
		textFieldStoppingCriteriaTime.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldStoppingCriteriaTime)) {
					try {
						frame.estimationBuilder.setStoppingCriteriaTimeMaximum(Double.parseDouble(textFieldStoppingCriteriaTime.getText()));
					} catch (NumberFormatException | UnsupportedOperationException | IllegalRangeException| IllegalScaleException e) {
						ObserverManager.notifyObserversOfError(e);		}
					
					LayoutManager.setProcessed(textFieldStoppingCriteriaTime);
				}}});
		comboBoxStoppingCriteriaTimeUnits.getItems().addAll(new TimeUnit[]{TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS});
		comboBoxStoppingCriteriaTimeUnits.valueProperty().addListener(new ChangeListener<TimeUnit>(){
			@Override
			public void changed(ObservableValue<? extends TimeUnit> observable, TimeUnit oldValue, TimeUnit newValue) {
				frame.estimationBuilder.setStoppingCriteriaTimeUnit(newValue);
			}
		});
		
		
		// Stopping criteria: CONVERGENCE. Set the defaults and the change listeners of (a) the checkBoxes, and
		// 	(b) the textField. 
		LayoutManager.setLayoutHandler(textFieldStoppingCriteriaConvergence, TextFieldValidInputCriterium.POSITIVE_DOUBLE); 
		checkBoxStoppingCriteriaConvergence.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				frame.estimationBuilder.setUseStoppingCriteriaConvergence(newValue);
				textFieldStoppingCriteriaConvergence.setDisable(!newValue);
				// After re-abling the TextField: place the focus on that field
				if (!textFieldStoppingCriteriaConvergence.isDisable()) {textFieldStoppingCriteriaConvergence.requestFocus();}
		}});
		textFieldStoppingCriteriaConvergence.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldStoppingCriteriaConvergence)) {
					try {	frame.estimationBuilder.setStoppingCriteriaConvergenceEpsilon(new DecimalNumber(textFieldStoppingCriteriaConvergence.getText()));
					} catch (NumberFormatException | UnsupportedOperationException | IllegalRangeException| IllegalScaleException e) {
						ObserverManager.notifyObserversOfError(e);		}

					LayoutManager.setProcessed(textFieldStoppingCriteriaConvergence);
				}}});
		
		
		// Stopping criteria: MAXIMUM ITERATIONS. Set the defaults and the change listeners of (a) the checkBoxes, and
		// 	(b) the textField.
		LayoutManager.setLayoutHandler(textFieldStoppingCriteriaIterations, TextFieldValidInputCriterium.POSITIVE_INTEGER); 
		checkBoxStoppingCriteriaIterations.selectedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				frame.estimationBuilder.setUseStoppingCriteriaIterations(newValue);
				textFieldStoppingCriteriaIterations.setDisable(!newValue);
				// After re-abling the TextField: place the focus on that field
				if (!textFieldStoppingCriteriaIterations.isDisable()) {textFieldStoppingCriteriaIterations.requestFocus();}
		}});
		textFieldStoppingCriteriaIterations.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldStoppingCriteriaIterations)) {
					try {	frame.estimationBuilder.setStoppingCriteriaIterationsMaximum(Integer.parseInt(textFieldStoppingCriteriaIterations.getText()));
					} catch (NumberFormatException | UnsupportedOperationException | IllegalRangeException	| IllegalScaleException e) {
						ObserverManager.notifyObserversOfError(e);		}
					LayoutManager.setProcessed(textFieldStoppingCriteriaIterations);
				}}});
		
		// set the optimizer combo box
		comboBoxOptimizer.getItems().addAll(Optimizer.values());
		comboBoxOptimizer.valueProperty().addListener(new ChangeListener<Optimizer>(){
			@Override
			public void changed(ObservableValue<? extends Optimizer> observable, Optimizer oldValue, Optimizer newValue) {
				frame.estimationBuilder.setOptimizer(newValue);
			}
		});
		
		// Set the indifference point node
		LayoutManager.setLayoutHandler(textFieldIndifferencePoint, TextFieldValidInputCriterium.POSITIVE_DOUBLE); 
		textFieldIndifferencePoint.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldIndifferencePoint)) {
					try {	frame.estimationBuilder.setIndifferencePoint(new DecimalNumber(textFieldIndifferencePoint.getText()));
					} catch (NumberFormatException | UnsupportedOperationException | IllegalRangeException| IllegalScaleException e) {
						ObserverManager.notifyObserversOfError(e);		}
					LayoutManager.setProcessed(textFieldIndifferencePoint);
				}}});
		
		// Set the starting parameters node
		LayoutManager.setLayoutHandler(textFieldStartingEstimates, TextFieldValidInputCriterium.DOUBLE); 
		textFieldStartingEstimates.focusedProperty().addListener(new ChangeListener<Boolean>(){
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && LayoutManager.isChanged(textFieldStartingEstimates)) {
					try {	frame.estimationBuilder.setStartingEstimates(Double.parseDouble(textFieldStartingEstimates.getText()));
					} catch (NumberFormatException | UnsupportedOperationException | IllegalRangeException| IllegalScaleException e) {
						ObserverManager.notifyObserversOfError(e);		}
					LayoutManager.setProcessed(textFieldStartingEstimates);
				}}});
		
		//Set the update frequency node
		LayoutManager.setLayoutHandler(textFieldStartingEstimates, TextFieldValidInputCriterium.INTEGER); 
		checkBoxBatchUpdating.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				frame.estimationBuilder.setBatchUpdating(newValue);			}});
		
		// Set the spinner for the number of threads to use
		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 256, 1);
		spinnerSimultaniousThreads.setValueFactory(valueFactory);
		spinnerSimultaniousThreads.getEditor().textProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Helper.isPositiveInteger(newValue)) 
					spinnerSimultaniousThreads.getValueFactory().setValue(Integer.parseInt(newValue));
				else if (newValue.length()>0) 
					spinnerSimultaniousThreads.getEditor().setText(oldValue);
			}});
		spinnerSimultaniousThreads.valueProperty().addListener(new ChangeListener<Integer>(){
			@Override
			public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
				frame.estimationBuilder.setNumberOfSimultaniousThreads(newValue);
		}});

		// Set the Browse button action handler
		buttonBrowseOutputFolder.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				DirectoryChooser dc = new DirectoryChooser();
				dc.setInitialDirectory(new File(Defaults.defaultInputFolderResults));
				File directory = dc.showDialog(frame.stage);
				if (directory != null) {
					textFieldOutputFolder.setText(directory.getAbsolutePath());
					frame.estimationBuilder.setOutputFolder(directory);
				}
		}});

	}
	

	
	
	
	
	
}
