package window;

import java.util.Arrays;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrixTableView;
import helper.Helper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import markovDecisionProcess.CueEmissionMatrixBuilder;
import markovDecisionProcess.CueEmissionType;
import staticManagers.ObserverManager;
import window.interfacesAndAbstractions.AbstractSecondaryStage;
import window.interfacesAndAbstractions.LayoutManager;
import window.interfacesAndAbstractions.LayoutManager.TextFieldValidInputCriterium;

public class SecondaryStageController_CueEmissionMatrix extends AbstractSecondaryStage implements EventHandler<ActionEvent>
{
	@FXML public DecimalNumberMatrixTableView 	tableViewMatrix ;
	@FXML public ComboBox<CueEmissionType> 		comboBoxCueEmissionType;
	@FXML public Label							labelCueEmissionArgument;
	@FXML public TextField 						textFieldCueEmissionArgument;
	@FXML public Spinner<Integer> 				spinnerCueLabels;
	@FXML public Button 						buttonChangeCueLabels;
	@FXML public Button 						buttonDeleteValue;
	@FXML public Button 						buttonAddValue;
	@FXML public Button 						buttonDone;

	public SecondaryStageController_CueEmissionMatrix(FrameController fc)
	{
		super(fc, "secondaryStage_CueEmissions.fxml", "Define p(cue | resource quality) ...", 500,500, true);
		setNodes();
		update();
	}
	

	@Override
	public void setNodes ()
	{
		// Set the combobox containing the cue emission types
		for (CueEmissionType cet: CueEmissionType.values())
			comboBoxCueEmissionType.getItems().add(cet);

		// Set the spinner for the number of cue values
		SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 100, frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1);
		this.spinnerCueLabels.setValueFactory(valueFactory);
		this.spinnerCueLabels.getEditor().textProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (Helper.isPositiveInteger(newValue)) 
					spinnerCueLabels.getValueFactory().setValue(Integer.parseInt(newValue));
				else if (newValue.length()>0) 
					spinnerCueLabels.getEditor().setText(oldValue);
			}});
		
		this.spinnerCueLabels.valueProperty().addListener(new ChangeListener<Integer>() { 
			public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
			try {
				changeNumberOfColumns(spinnerCueLabels.getValue());
				cueEmissionChanged();
			} catch (Exception e) {
				ObserverManager.notifyObserversOfError(e);

			}

		}});
		
		// Set all action events/change listeners
		this.comboBoxCueEmissionType.valueProperty().addListener(new ChangeListener<CueEmissionType>() {public void changed(ObservableValue<? extends CueEmissionType> observable, CueEmissionType oldValue,
					CueEmissionType newValue) {try {
						cueEmissionChanged();
					} catch (Exception e) {
						ObserverManager.notifyObserversOfError("Error when setting cue emission distribution", "An exception was encountered when trying to change the cue emission distribution. See details", e);} 
			}});
		this.textFieldCueEmissionArgument.focusedProperty().addListener(new ChangeListener<Boolean> () {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue)
					try {
						cueEmissionChanged();
					} catch (Exception e) { ObserverManager.notifyObserversOfError(e);		}
			}});
		
		this.buttonAddValue.setOnAction(this);
		this.buttonChangeCueLabels.setOnAction(this);
		this.buttonDeleteValue.setOnAction(this);
		this.buttonDone.setOnAction(this);

		// Set the text in the tableViewTable that is displayed when no data is present
		tableViewMatrix.setPlaceholder(new Label("The matrix is empty. This can be caused either by the manual removal of all rows, or because an error occured."));
		
		// Initialize the table view
		this.tableViewMatrix.initializeTableView(frame.mdpBuilder.CUE_EMISSION_MATRIX, significantDigits, false, 100, frame, frame.mdpBuilder, this);
		tableViewMatrix.setEditable(0, false); // cannot change resource values!
		
	}

	@Override
	public void update() {
		comboBoxCueEmissionType.getSelectionModel().select(frame.mdpBuilder.CUE_EMISSION_TYPE);
		comboBoxCueEmissionType.setValue(frame.mdpBuilder.CUE_EMISSION_TYPE);
		this.textFieldCueEmissionArgument.setText(""+frame.mdpBuilder.CUE_EMISSION_ARGUMENT);
	}

	
	
	/////////////////////////////////////////////////////////////////////////////////////
	////////////////////////// 	Cue emission stage 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	
	
	@Override
	public void handle(ActionEvent event) {
		// If the delete value button is pressed: remove the selected row(s) from the table and set the emission type to manual
		if (event.getSource() == this.buttonDeleteValue) 	{ 
			tableViewMatrix.removeSelectedRows();
			frame.mdpBuilder.CUE_EMISSION_TYPE = CueEmissionType.Manual;
			this.update();
		}

		// If the add button is pressed: insert a new row at the selected index (if there is an selected row) or at the top (if there is no selected row) and set
		// the cue emission type to manual. The new row has a uniform distribution over cues.
		else if (event.getSource() == this.buttonAddValue) 		{
			try {
				// Create an uniform array of [1 resource value] + [n uniform probabilities)
				DecimalNumberArray uniformArray =  DecimalNumberArray.rep(DecimalNumber.ONE.divide(frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1), frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol());
				uniformArray.set(0,DecimalNumber.ZERO);
				tableViewMatrix.insertRowAtSelectedRow(uniformArray);
				frame.mdpBuilder.CUE_EMISSION_TYPE = CueEmissionType.Manual;
				this.update();
			} catch (IllegalRangeException | IllegalScaleException e) {
				ObserverManager.notifyObserversOfError(e);
			}
		}

		// If the done button is pressed: do some input validation and either warn the user or close the stage. These computations are long, and hence I have moved them to their own function
		else if (event.getSource() == this.buttonDone) 
			this.buttonDonePressed(); 

		// If the change cue labels button is pressed: prompt the user with the popup to change the names.
		else if (event.getSource() == this.buttonChangeCueLabels)  
			this.createPopupForCueLables();

		// Regardless of what button has been pressed: after handling the event, table view should be updated and the mainWindowStage should be notified that something has changed.
		tableViewMatrix.refresh();
		frame.notifyAll(frame.mdpBuilder, this);
	}

	/**
	 * Prompt the user for a change in cue names
	 */
	private void createPopupForCueLables()
	{
		//  Create the popup screen
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setWidth(300);
		alert.setHeight(500);
		alert.setTitle("Change cue labels");
		alert.setHeaderText("Please provide the labels for all possible values a cue might take");
		alert.initOwner(stage);

		// Create the listView that is the center of this popup screen.
		ObservableList<String> cueLabelsToShow = FXCollections.observableArrayList();
		for (int i = 1; i < this.tableViewMatrix.getColumns().size(); i++)
			cueLabelsToShow.add(tableViewMatrix.getColumns().get(i).getText());
		ListView<String> listViewCueLabels = new ListView<>();
		listViewCueLabels.setItems(cueLabelsToShow);
		listViewCueLabels.setEditable(true);
		listViewCueLabels.setCellFactory(TextFieldListCell.forListView());
		listViewCueLabels.getStylesheets().add(this.getClass().getResource("../CSSLayout/normalListView.css").toExternalForm());

		// Event handler: note that the ObservableArray listView is filled with String values, but changes in the
		// ObservableArray do not translate to the underlying array. 
		// Hence, if we want changes to be permanent, we have to make manual changes in the underlying frame.mdpBuilder.
		// Since columns can be shifted back and forth, we need to find what position each column has, and changes the names in the original columns
		listViewCueLabels.setOnEditCommit(new EventHandler<ListView.EditEvent<String>>() {
			@Override
			public void handle(ListView.EditEvent<String> t) {
				tableViewMatrix.setColumnName(cueLabelsToShow.get(t.getIndex()), t.getNewValue());
				cueLabelsToShow.set(t.getIndex(), t.getNewValue());
			}
		});

		// Some other view nodes that we need
		GridPane.setVgrow(listViewCueLabels, Priority.ALWAYS);
		GridPane.setHgrow(listViewCueLabels, Priority.ALWAYS);

		GridPane gp = new GridPane();
		gp.setMaxWidth(Double.MAX_VALUE);
		gp.add(listViewCueLabels, 0, 0);

		// Create a button for automatically changing all names to the negative--- to positive+++ format.
		Button autoName = new Button("Set to [Negative - Positive] range");
		autoName.getStylesheets().add(this.getClass().getResource("../CSSLayout/normalButton.css").toExternalForm());
		autoName.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				int totalCues = cueLabelsToShow.size();
				int neutralCue = (totalCues % 2);
				int negativeCues = (totalCues-neutralCue)/2;
				int positiveCues = negativeCues;
		
				int pos = 1;
				for (int n=0; n <negativeCues; n++) {
					tableViewMatrix.setColumnName(pos, Helper.repString("-", negativeCues-n));
					cueLabelsToShow.set(pos-1, tableViewMatrix.getColumns().get(pos).getText());
					pos++;
				}

				if (neutralCue == 1) {
					tableViewMatrix.setColumnName(pos, "+/-");
					cueLabelsToShow.set(pos-1, tableViewMatrix.getColumns().get(pos).getText());
					pos++;
				}

				for (int p=0; p <positiveCues; p++) {
					tableViewMatrix.setColumnName(pos, Helper.repString("+", p+1));
					cueLabelsToShow.set(pos-1, tableViewMatrix.getColumns().get(pos).getText());
					pos++;
				}
			}});
		gp.add(autoName, 0, 1);

		alert.getDialogPane().setContent(gp);
		alert.showAndWait();

	}


	/** Determines the behaviour of the stage when the combobox for the cue emission types is changed. Specifically, it tells the matrix to change, and 
	 * makes the argument box visible (and changes the label).
	 * 
	 * ~~ TAG: "adding cue emission type": should the user defined emission type require an argument, here is the place to set the text
	 * 
	 */
	private void cueEmissionChanged() throws NumberFormatException{
		frame.mdpBuilder.CUE_EMISSION_TYPE = comboBoxCueEmissionType.getValue();
		
		// ~~ TAG: "adding cue emission type"
		// If the TextField for the argument is the cause: inform the cue emission matrix that it needs to change. 
		// Note that this argument might have some restrictions (e.g., a standard deviation or variance must be >0) 
		// which might vary for different cue emission types. 
		//These restrictions are implemented here. If they are not met, change the layout of the TextField.
		if (frame.mdpBuilder.CUE_EMISSION_TYPE == CueEmissionType.Normal)
			LayoutManager.setLayoutHandler(textFieldCueEmissionArgument, TextFieldValidInputCriterium.POSITIVE_DOUBLE);
		
		// Reset the textFieldCueEmissionArgument's text to trigger the layout event
		textFieldCueEmissionArgument.setText(textFieldCueEmissionArgument.getText());
		
		// From the layout of the cue emission argument TextField, determine if we should update
		// the MDP builder cue emission argument (and do so if we have to)
		if (LayoutManager.isChanged(textFieldCueEmissionArgument)) {
			frame.mdpBuilder.CUE_EMISSION_ARGUMENT = Double.parseDouble(textFieldCueEmissionArgument.getText());
			LayoutManager.setProcessed(textFieldCueEmissionArgument);
		}
			
		// ~~ TAG: "adding cue emission type"
		// Please describe below what should happen should a Cue Emission Type be selected
		if (frame.mdpBuilder.CUE_EMISSION_TYPE == CueEmissionType.Manual) {
			toaster.makeToast("Setting cue emission probability to a manual distribution...");
			textFieldCueEmissionArgument.setVisible(false);
			labelCueEmissionArgument.setVisible(false);
		}

		else if (frame.mdpBuilder.CUE_EMISSION_TYPE == CueEmissionType.Linear) {
			toaster.makeToast("Setting cue emission probability to a linear distribution...");
			textFieldCueEmissionArgument.setVisible(false);
			labelCueEmissionArgument.setVisible(false);
			CueEmissionMatrixBuilder.setCueEmissionMatrix(comboBoxCueEmissionType.getValue(), 0, frame.mdpBuilder);
		}

		else if (frame.mdpBuilder.CUE_EMISSION_TYPE == CueEmissionType.Normal) {
			toaster.makeToast("Setting cue emission probability to a normal distribution...");
			textFieldCueEmissionArgument.setVisible(true);
			labelCueEmissionArgument.setVisible(true);
			labelCueEmissionArgument.setText("Standard deviation:");
			CueEmissionMatrixBuilder.setCueEmissionMatrix(comboBoxCueEmissionType.getValue(), frame.mdpBuilder.CUE_EMISSION_ARGUMENT, frame.mdpBuilder);
		}
		
		else {
			ObserverManager.notifyObserversOfError("Invalid cue emission type", "Attempting to set the cue emission type to a non-implemented function.");
		}
		
		//Update the changes
		tableViewMatrix.refresh();
		frame.notifyAll(frame.mdpBuilder, this);
	}

	/**
	 * Changes the number of cue labels (columns) in the cue emission matrix. If the number of cue labels is reduced, the last columns of the matrix are removed.
	 * If the number of cue labels is increased, new columns are appended at the end of the matrix. The new columns all have values of 0. 
	 * 
	 * For all intents and purposes, the numberOfCueLabels should always be higher than or equal to 2.
	 * @param newValue
	 */
	public void changeNumberOfColumns(int numberOfCueLabels) throws UnsupportedOperationException, IllegalRangeException, IllegalScaleException
	{
		try {
			// If the numberOfCueLabels is LOWER than the number of cue labels in the matrix: starting at the last column: remove columns until the (number of columns -1) matches numberOfCueLabels 
			// (-1 because the resource values are also in the matrix)
			while (numberOfCueLabels < frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1)
				this.tableViewMatrix.removeColumn(frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1);

			// If the numberOfCueLabels is HIGHER than the number of cue labels in the matrix: starting at the last column: insert new columns until the (number of columns -1) matches numberOfCueLabels 
			// (-1 because the resource values are also in the matrix)
			while (numberOfCueLabels > frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1) {
				DecimalNumberArray newColumn = new DecimalNumberArray(frame.mdpBuilder.CUE_EMISSION_MATRIX.nrow());
				newColumn.setAll(0);
				this.tableViewMatrix.insertColumn(frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol(), newColumn, "Cue " + (frame.mdpBuilder.CUE_EMISSION_MATRIX.ncol()-1));
			}
		} catch (Exception e) { ObserverManager.notifyObserversOfError(e);}
	}

	private void buttonDonePressed()
	{
		// Check if the matrix has a column named "Resource value"
		if (frame.mdpBuilder.CUE_EMISSION_MATRIX.getColumn("Resource value") == null) {
			ObserverManager.notifyObserversOfError("No resource value column specified", "No resource value column was found in the matrix. This should not have occurred, and indicates that something went wrong.");
			return;
		}

		// Check if all resource values are within the range [-frame.mdpBuilder.VALUE_MAXIMUM; frame.mdpBuilder.VALUE_MAXIMUM]
		boolean removedInvalidCueEmissions = removeInvalidCueEmissions();
		if (removedInvalidCueEmissions ) 
		{
			ObserverManager.notifyObserversOfError("Removed invalid cue emissions", "At least one cue emission had a specified resource value outside of the range allowed by the MDP process parameters (maximum Resource values). These cue emissions have been removed.)");
			return;
		}

		// Do a normalization check
		boolean valuesNormalized = false;
		try {
			for (int r = 0; r < frame.mdpBuilder.CUE_EMISSION_MATRIX.nrow(); r++) {
				DecimalNumberArray row = frame.mdpBuilder.CUE_EMISSION_MATRIX.getRow(r);
				if(!row.sum().subtract(row.get(0)).equals(1)) // the sum of the row minus the resource value
				{
					DecimalNumber resourceValue = row.get(0);
					row.remove(0);
					row.toProbability();
					row.insert(0, resourceValue);
					valuesNormalized = true;
				}
			} 
		} catch (Exception e) {ObserverManager.notifyObserversOfError(e);}

		if (valuesNormalized) 
			ObserverManager.warnObservers("Row has been changed to sum to 1", "At least one row did not sum to 1, and hence, was not a probability distribution. This row (or rows) has been scaled to be a probability distribution."); 


		// If there are duplicate values, inform the user and do not close the stage.
		DecimalNumberArray copyOfResourceValues = frame.mdpBuilder.CUE_EMISSION_MATRIX.getColumn("Resource value").clone();
		Arrays.sort(copyOfResourceValues.array);
		if (copyOfResourceValues.length() > 0)
			for (int i = 1; i < copyOfResourceValues.length(); i++)
				if (copyOfResourceValues.get(i).equals(copyOfResourceValues.get(i-1)))		{
					ObserverManager.notifyObserversOfError("Duplicate resource values", "At least one resource value has multiple entries. Please ensure that the resource values are all unique.");
					return;
				}

		//close the stage and inform the FrameController that the MDP has changed
		stage.close();
		frame.notifyAll(frame.mdpBuilder, this);
	}


	/**
	 * Remove all cue emissions in the MDP builder's cue emission matrix that have a resource value outside the range of [-VALUE_MAXIMUM, VALUE_MAXIMUM] 
	 * (the values of this range are stored in the frame.mdpBuilder). If values are removed, this function returns a true. If no values
	 * are removed, a false is returned. 
	 */
	private boolean removeInvalidCueEmissions()
	{
		boolean removedCueEmissions = false;
		for (int r = 0; r < frame.mdpBuilder.CUE_EMISSION_MATRIX.nrow(); r++)
			if (!frame.mdpBuilder.CUE_EMISSION_MATRIX.getColumn("Resource value").get(r).inRange(-frame.mdpBuilder.BUDGET_MAXIMUM, frame.mdpBuilder.BUDGET_MAXIMUM)) {
				tableViewMatrix.removeRow(r);
				removedCueEmissions = true;
			}

		return removedCueEmissions;
	}




}
