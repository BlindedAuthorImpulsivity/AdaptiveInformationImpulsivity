package window;

import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrixTableView;
import environment.EnvironmentBuilderFull;
import environment.ValueDistributionType;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import staticManagers.ObserverManager;
import window.interfacesAndAbstractions.AbstractSecondaryStage;

public class SecondaryStageController_ChangeEnvironment extends AbstractSecondaryStage
{
	
	@FXML public DecimalNumberMatrixTableView tableView;
	@FXML public Button buttonDone;
	@FXML public Button buttonResourceValue;
	@FXML public Button buttonExtrinsicEvent;

	private final EnvironmentBuilderFull selectedBuilder;
	
	public SecondaryStageController_ChangeEnvironment(FrameController fc, EnvironmentBuilderFull selectedBuilder)
	{
		super(fc, "secondaryStage_ChangeEnvironmentStage.fxml", "Manually set environment ...", 350	,400, true);
		this.selectedBuilder = selectedBuilder;
		setNodes();
	}
	
	
	public void setNodes()
	{
		this.tableView.initializeTableView(selectedBuilder.getMatrix(), significantDigits, false, 100, frame, selectedBuilder, this);
		tableView.setEditable(0, false);

		// Set the button behaviour
		buttonResourceValue.setOnAction(new EventHandler<ActionEvent>() {public void handle(ActionEvent event) {
			selectedBuilder.setResourceValueDistributionType(ValueDistributionType.Manual);
			for (DecimalNumber dn:  tableView.getMatrix().getColumn(1))
				dn.set(0);
			tableView.refresh();
		}});

		buttonExtrinsicEvent.setOnAction(new EventHandler<ActionEvent>() {public void handle(ActionEvent event) {
			selectedBuilder.setExtrinsicEventDistributionType(ValueDistributionType.Manual);
			DecimalNumberArray extrinsicEventProbabilities = tableView.getMatrix().getColumn(2);
			for (DecimalNumber dn: extrinsicEventProbabilities)
				dn.set(0);
			tableView.refresh();
		}});

		buttonDone.setOnAction(new EventHandler<ActionEvent>() {public void handle(ActionEvent event) {
			try {
				validateInput();
				// Only at the end do we have to actually update the builder
				selectedBuilder.update();
				frame.notifyAll(selectedBuilder, ownReference);
				stage.close();
			} catch (IllegalRangeException | IllegalScaleException e) {
				ObserverManager.notifyObserversOfError(e);
			}
		}});
	}
	
	public void update() {
		tableView.refresh();
	}

	
	/**
	 * Check if all input is valid, and if not, provides an Alert to show what is wrong.
	 * @return
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 */
	private boolean validateInput() throws IllegalRangeException, IllegalScaleException
	{
		
		// check if the resource value probability distribution is a probability distribution (i.e., sum to 1):
		DecimalNumberArray resourceValueProbabilities = tableView.getMatrix().getColumn(1);
		if (!resourceValueProbabilities.sum().equals(1, true)) {
			resourceValueProbabilities.toProbability();
			tableView.refresh();
			ObserverManager.warnObservers("Invalid resource value probability distribution", "The resource value probability distribution did not sum to 1. The column was scaled to sum to 1.");
			return false;
		}
		
		// check if the extrinsic event value probability distribution is a probability distribution (i.e., sum to 1):
		DecimalNumberArray extrinsicEventProbabilities = tableView.getMatrix().getColumn(2);
		if (!extrinsicEventProbabilities.sum().equals(1, true)) {
			extrinsicEventProbabilities.toProbability();
			tableView.refresh();
			ObserverManager.warnObservers("Invalid extrinsic event value probability distribution", "The extrinsic event value probability distribution did not sum to 1. The column was scaled to sum to 1.");
			return false;
		}
			
		return true;
	}
	

}
