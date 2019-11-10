package window;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import environment.AbstractEnvironmentBuilder;
import environment.EnvironmentBuilderFull;
import environment.EnvironmentBuilderLazy;
import environment.ValueDistributionType;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import staticManagers.ObserverManager;
import window.interfacesAndAbstractions.AbstractSecondaryStage;
import window.interfacesAndAbstractions.LayoutManager;
import window.interfacesAndAbstractions.LayoutManager.State;
import window.interfacesAndAbstractions.LayoutManager.TextFieldValidInputCriterium;

public class SecondaryStageController_EnvironmentRange extends AbstractSecondaryStage 
{
	@FXML public GridPane  gridPane;
	@FXML public TextField textFieldResourceValueMeanFrom;
	@FXML public TextField textFieldResourceValueMeanTo;
	@FXML public TextField textFieldResourceValueMeanStep;

	@FXML public TextField textFieldResourceValueSDFrom;
	@FXML public TextField textFieldResourceValueSDTo;
	@FXML public TextField textFieldResourceValueSDStep;

	@FXML public TextField textFieldExtrinsicEventMeanFrom;
	@FXML public TextField textFieldExtrinsicEventMeanTo;
	@FXML public TextField textFieldExtrinsicEventMeanStep;

	@FXML public TextField textFieldExtrinsicEventSDFrom;
	@FXML public TextField textFieldExtrinsicEventSDTo;
	@FXML public TextField textFieldExtrinsicEventSDStep;

	@FXML public TextField textFieldInterruptionRateFrom;
	@FXML public TextField textFieldInterruptionRateTo;
	@FXML public TextField textFieldInterruptionRateStep;
	public ArrayList<TextField> textFields;

	@FXML public ComboBox<ValueDistributionType> comboBoxResourceValueDistributionType;
	@FXML public ComboBox<ValueDistributionType> comboBoxExtrinsicEventDistributionType;

	@FXML public Button buttonDone;

	// Is this stage opened because the user wants to add fully (i.e., eager) specified environments, 
	// or because the user wants to add lazy specified environments?
	public enum SpecificationType { Full, Lazy;}
	public final SpecificationType specificationType;


	public SecondaryStageController_EnvironmentRange(FrameController fc, SpecificationType specificationType)
	{
		super(fc, "secondaryStage_EnvironmentRange.fxml","Adding a range of " + specificationType.toString().toLowerCase() +" environments", 400,350, false);
		this.specificationType=specificationType;
		setNodes();
		update();
	}

	@Override
	public void setNodes()
	{
		// Set the button text
		this.buttonDone.setText("Add " + specificationType.toString().toLowerCase() + " environments");

		// Fill the comboboxes with non-manual types, and set Normal to be the default
		this.comboBoxResourceValueDistributionType.getItems().addAll(ValueDistributionType.getValuesExcludingManual());
		this.comboBoxResourceValueDistributionType.setValue(ValueDistributionType.Normal);
		this.comboBoxResourceValueDistributionType.setPrefWidth(200);
		this.comboBoxExtrinsicEventDistributionType.getItems().addAll(ValueDistributionType.getValuesExcludingManual());
		this.comboBoxExtrinsicEventDistributionType.setValue(ValueDistributionType.Normal);
		this.comboBoxExtrinsicEventDistributionType.setPrefWidth(200);

		// Set all TextField layout managers
		textFields = new ArrayList<>();
		for (Node n: this.gridPane.getChildren())
			if (n instanceof TextField)
			{
				TextField tf = (TextField)n;
				textFields.add(tf);
				setLayoutManager(tf);
			}

		// Add the action listener to the finalize button. Because this might take a while, it runs on its own thread.
		this.buttonDone.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if (allInputValid()) {

					// Step 1: create all the AbstractEnvironmentBuilders.
					// Because this is (or might be) a rather heavy task, its run on its own thread.
					Task<AbstractEnvironmentBuilder[]> factoryBuildingTask = new Task<AbstractEnvironmentBuilder[] >() { 
						@Override public AbstractEnvironmentBuilder[]  call() {
							try {
								return getRequestedRangeFactories() ;
							} catch (IllegalArgumentException | IllegalRangeException | IllegalScaleException e) {ObserverManager.notifyObserversOfError(e); return null;} 
						}
					};

					// After we have all the builder, two things can happen. Either we are building lazy builders, and we
					// can add those to the frame's lazy pool. Then again, we might be in the mood to build some fully
					// specified builder. If that feeling strikes, we still have to actually build them (i.e., give
					// them a sweet, sweet matrix) before we can add them to the frame's full pool.
					factoryBuildingTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
						@Override
						public void handle(WorkerStateEvent t) {
							if (specificationType == SpecificationType.Lazy) {
								frame.environmentPoolLazySpecified.addAll( Arrays.copyOf(factoryBuildingTask.getValue(), factoryBuildingTask.getValue().length, EnvironmentBuilderLazy[].class));
								frame.notifyAll(frame.environmentPoolLazySpecified, ownReference);
							}
							
							if (specificationType == SpecificationType.Full)
								startFactoryToBuilderTask(factoryBuildingTask.getValue());
							}
						});
					new Thread(factoryBuildingTask).start();
					}
				}});

	}

	@Override
	public void update() {

	}

	private void setLayoutManager(TextField tf)
	{
		// Set the listener for all Mean textFields
		if (tf.getId().contains("Mean"))
			LayoutManager.setLayoutHandler(tf, TextFieldValidInputCriterium.DOUBLE);

		// Set the listener for all SD textFields
		if (tf.getId().contains("SD"))
			LayoutManager.setLayoutHandler(tf, TextFieldValidInputCriterium.NON_NEGATIVE_DOUBLE);

		// Set the listener for all interruption rate textFields
		if (tf.getId().contains("InterruptionRate"))
			LayoutManager.setLayoutHandler(tf, TextFieldValidInputCriterium.PROBABILITY);

		// Set the listener for all step textFields
		if (tf.getId().contains("Step"))
			LayoutManager.setLayoutHandler(tf, TextFieldValidInputCriterium.POSITIVE_DOUBLE);

		// At the start, no TextField has an input. However, at least some input needs to 
		// be specified at every field. Hence, we make the layout invalid at the start:
		LayoutManager.setState(tf, State.Invalid);

	}



	/**
	 * Check whether the inputs provided are admissible. Since values are 
	 * OK if they have a normal or changed layout, this function checks
	 * if there is at least one TextField with an invalid layout. If so,
	 * it returns false. Else it returns true.
	 * 
	 * If the input is not valid a warning message is shown. 
	 * @return
	 */
	private boolean allInputValid ()
	{
		for (TextField tf: textFields) 
			if (LayoutManager.isInvalid(tf)) {
				toaster.makeWarningToast("At least one text field has an invalid value (red background). Please enter a valid number.");
				return false;
			}
		return true;
	}


	/**
	 * Creates an array of AbstractEnvironmentBuilders that matches the specified ranges. 
	 * This function assumes that all TextField values, and the combinations 
	 * of all values, are valid values.
	 * 
	 * Every 1000 
	 * @return
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws IllegalArgumentException 
	 */
	private AbstractEnvironmentBuilder[] getRequestedRangeFactories () throws IllegalArgumentException, IllegalRangeException, IllegalScaleException
	{
		// Set the starting values of each range to the FROM field
		double startResourceValueMean = 	Double.parseDouble(textFieldResourceValueMeanFrom.getText());
		double startResourceValueSD = 		Double.parseDouble(textFieldResourceValueSDFrom.getText());
		double startExtrinsicEventMean = 	Double.parseDouble(textFieldExtrinsicEventMeanFrom.getText());
		double startExtrinsicEventSD= 		Double.parseDouble(textFieldExtrinsicEventSDFrom.getText());
		double startInterruptionRate = 		Double.parseDouble(textFieldInterruptionRateFrom.getText());

		// Set the targets to end point of each range
		double targetResourceValueMean = 	Double.parseDouble(textFieldResourceValueMeanTo.getText());
		double targetResourceValueSD = 		Double.parseDouble(textFieldResourceValueSDTo.getText());
		double targetExtrinsicEventMean = 	Double.parseDouble(textFieldExtrinsicEventMeanTo.getText());
		double targetExtrinsicEventSD= 		Double.parseDouble(textFieldExtrinsicEventSDTo.getText());
		double targetInterruptionRate = 	Double.parseDouble(textFieldInterruptionRateTo.getText());

		// Set the increments to the step sizes
		double incrementResourceValueMean = 	Double.parseDouble(textFieldResourceValueMeanStep.getText());
		double incrementResourceValueSD = 		Double.parseDouble(textFieldResourceValueSDStep.getText());
		double incrementExtrinsicEventMean = 	Double.parseDouble(textFieldExtrinsicEventMeanStep.getText());
		double incrementExtrinsicEventSD= 		Double.parseDouble(textFieldExtrinsicEventSDStep.getText());
		double incrementInterruptionRate = 		Double.parseDouble(textFieldInterruptionRateStep.getText());

		// Get the ValueDistributionTypes
		ValueDistributionType rvVDT = this.comboBoxResourceValueDistributionType.getSelectionModel().getSelectedItem();
		ValueDistributionType mVDT = this.comboBoxExtrinsicEventDistributionType.getSelectionModel().getSelectedItem();

		// Figure out how large the array should be (we do not want to make an array list that 
		// might require resizing many times
		int numberOfFactories = 0;
		for (double rqM=startResourceValueMean; rqM <= targetResourceValueMean; rqM = rqM + incrementResourceValueMean)
			for (double rqSD=startResourceValueSD; rqSD <= targetResourceValueSD; rqSD = rqSD + incrementResourceValueSD)
				for (double mM=startExtrinsicEventMean; mM <= targetExtrinsicEventMean; mM = mM + incrementExtrinsicEventMean)
					for (double mSD=startExtrinsicEventSD; mSD <= targetExtrinsicEventSD; mSD = mSD+ incrementExtrinsicEventSD)
						for (double ii =startInterruptionRate; ii <= targetInterruptionRate; ii= ii + incrementInterruptionRate) 
							numberOfFactories++;
		
		// some output to let the user know that we starting making factories
		final DecimalFormat df = new DecimalFormat("#.##");
		if (specificationType == SpecificationType.Lazy)
			ObserverManager.makeToast("Creating " + numberOfFactories + " lazy environments: 0%.");
		else
			ObserverManager.makeToast("Step 1 of 2: preparing construction for " + numberOfFactories + " full environments: 0%.");
		
		// Create all the factories, keeping the user updated every 1 000 factories
		AbstractEnvironmentBuilder[] factoryArray = new AbstractEnvironmentBuilder[numberOfFactories];
		
		int pos = 0;
		int counter = 0;
		for (double rvM=startResourceValueMean; rvM <= targetResourceValueMean; rvM = rvM + incrementResourceValueMean)
			for (double rvSD=startResourceValueSD; rvSD <= targetResourceValueSD; rvSD = rvSD + incrementResourceValueSD)
				for (double mM=startExtrinsicEventMean; mM <= targetExtrinsicEventMean; mM = mM + incrementExtrinsicEventMean)
					for (double mSD=startExtrinsicEventSD; mSD <= targetExtrinsicEventSD; mSD = mSD+ incrementExtrinsicEventSD)
						for (double ii =startInterruptionRate; ii <= targetInterruptionRate; ii= ii + incrementInterruptionRate) {
							// Add either full or lazy environment builders (full builders will be updated to construct their matrix later on)
							if (specificationType == SpecificationType.Lazy)
								factoryArray[pos++] = new EnvironmentBuilderLazy(rvM, rvSD, rvVDT, mM, mSD, mVDT, ii);
							
							// Add either full or lazy environment builders
							if (specificationType == SpecificationType.Full) 
								factoryArray[pos++] = new EnvironmentBuilderFull(rvM, rvSD, rvVDT, mM, mSD, mVDT, ii, null);
						
							// Inform the user of the state
							if (++counter > 1000) {
								counter = 0;
								if (specificationType == SpecificationType.Lazy)
									ObserverManager.makeToast("Creating " + numberOfFactories + " lazy environments: " + df.format((double)pos/(double)numberOfFactories*100) + "%.");
								else
									ObserverManager.makeToast("Step 1 of 2: preparing construction for " + numberOfFactories + " full environments: " + df.format((double)pos/(double)numberOfFactories*100) + " %.");
							}
							
						}

		// Tell the user that the process is complete
		if (specificationType == SpecificationType.Lazy)
			ObserverManager.makeToast("Finished creating " + numberOfFactories + " lazy environments.");
		else
			ObserverManager.makeToast("Finished step 1 of 2: preparing construction for " + numberOfFactories + " full environments.");
		
		return factoryArray;
	}

	/** Only for when the mode is to create fully specified EnvironmentBuilders:sStart a new task (not on the FX thread) that creates all the fully specified environments. At the end of this task the results are added to the frame's fully specified environment pool*/
 	private void startFactoryToBuilderTask(final AbstractEnvironmentBuilder[] factories) {
		// Step 1: create all the environmentBuilderFactories that are necessary
		// Because this is (or might be) a rather heavy task, computationally speaking,
		// it is run in a different thread.
		Task<EnvironmentBuilderFull[]> factoryToBuilderTask = new Task<EnvironmentBuilderFull[] >() { 
			@Override public EnvironmentBuilderFull[]  call() {
				try {
					return buildFactories(factories) ;
				} catch (IllegalArgumentException | IllegalRangeException | IllegalScaleException e) {ObserverManager.notifyObserversOfError(e); return null;} 
			}
		};

		// After completion: add the newly minted fully specified environments to the frame's fully specified ool
		factoryToBuilderTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				try {frame.environmentPoolFullySpecified.addAll(factoryToBuilderTask.get());} catch (InterruptedException | ExecutionException e) { ObserverManager.notifyObserversOfError(e);	}
				frame.notifyAll(frame.environmentPoolFullySpecified, ownReference);
			}});
		new Thread(factoryToBuilderTask).start();
		
	}
	
	/**Only for when the mode is to create full factories: after we have constructed a fully specified environment, we still need to attach the MDPBuilder to this builder and construct its matrix */
	private EnvironmentBuilderFull[] buildFactories(AbstractEnvironmentBuilder[] factories) throws IllegalArgumentException, IllegalRangeException, IllegalScaleException {
		try {
		if ( specificationType != SpecificationType.Full)
			throw new IllegalStateException("Exception in SecondaryStageController_EnvironmentRange.buildFactories(): trying to build the matrix of a lazy specified environment. This should not happen.");
		
		// Create all the factories, keeping the user updated every 10 factories
		final DecimalFormat df = new DecimalFormat("#.##");
		EnvironmentBuilderFull[] builderArray = Arrays.copyOf(factories,factories.length, EnvironmentBuilderFull[].class);

		ObserverManager.makeToast("Step 2 of 2: creating " + builderArray.length + " fully specified environments construction: 0%.");
		int counter = 0;
		for (int pos = 0; pos < builderArray.length; pos++) {
			EnvironmentBuilderFull ebf = (EnvironmentBuilderFull) builderArray[pos];
			ebf.setMDPBuilder(frame.mdpBuilder);
			ebf.update();
			// Update user
			if (++counter > 100) {
				counter = 0;
				ObserverManager.makeToast("Step 2 of 2: creating " + builderArray.length + " fully specified environments construction: " + df.format((double)pos/(double)builderArray.length*100) + " %.");
			}

		}

		// Tell the user that the process is complete
		ObserverManager.makeToast("Finished step 2 of 2: preparing construction for " + builderArray.length + " full environments.");
		return builderArray;
		} catch(Exception e) { ObserverManager.notifyObserversOfError(e);}
		return null;
	}


	/**
	 * Returns true iff at least one item in the array is equal to the element specified. Equality is determined using the
	 * .equals function.
	 * @param element
	 * @param array
	 * @return
	 */
	public static boolean containsAlias(EnvironmentBuilderFull element, ArrayList<EnvironmentBuilderFull> array)
	{
		for (EnvironmentBuilderFull item: array)
		{
			if (element.equals(item))
				return true;
		}
		return false;
	}



}
