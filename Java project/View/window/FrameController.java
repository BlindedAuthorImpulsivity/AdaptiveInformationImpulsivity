package window;

import java.io.IOException;
import java.util.ArrayList;

import decimalNumber.DecimalNumber.ComputationException;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import defaultAndHelper.JavaFXHelper;
import defaultAndHelper.JavaFXHelper.Toaster;
import defaults.Defaults;
import environment.EnvironmentBuilderFull;
import environment.EnvironmentBuilderLazy;
import environment.ValueDistributionType;
import estimationParameters.EstimationBuilder;
import helper.Helper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import markovDecisionProcess.CueEmissionMatrixBuilder;
import markovDecisionProcess.MarkovDecisionProcessBuilder;
import start.Model;
import staticManagers.ObserverManager;
import staticManagers.ObserverManager.ObserverStage;
import window.interfacesAndAbstractions.AbstractTab;
import window.interfacesAndAbstractions.NotificationManager;


/**
 * The FrameController controls all separate tabs, all communication between tabs, and all communication between tabs and the Model.
 * The frame is the left-hand side menu, the selection menu at the top, and the content pane in the center (where all the buttons,
 * text, and other good stuff is). The content pane differs for different tabs. The control over these tabs is relegated to
 * AbstractTab objects. 
 * @author jesse
 *
 */
public class FrameController implements ObserverStage, EventHandler<ActionEvent>, NotificationManager
{
			public final Model							model;
			
			public final MarkovDecisionProcessBuilder 	mdpBuilder;
			public final ObservableList<EnvironmentBuilderFull> environmentPoolFullySpecified;
			public final ObservableList<EnvironmentBuilderLazy> environmentPoolLazySpecified;
			public final EstimationBuilder				estimationBuilder;
			public	Stage								stage;
			public Scene								scene;
			private final Toaster 						toaster;
			private ArrayList<ToggleButton>				toggleButtons;

	@FXML	private MenuItem							menuItemRunRunSimulation;
	@FXML 	private MenuItem							menuItemRunRetrain;
			
	@FXML	public AnchorPane							contentPaneContainer; // the anchor pane containing all right-hand side nodes in the screen (i.e., no tabs)
	@FXML 	public AnchorPane							anchorPaneScrollPane; // The anchorPane of the scrollPane
	@FXML 	public AnchorPane							contentPane;		  // The anchor pane contained in anchorPaneScrollPane - its is the area where all the content is. 
	@FXML	public ScrollPane 							scrollPane;
	@FXML	private	ToggleButton						toggleButtonIntroduction;
	@FXML	private	ToggleButton						toggleButtonMDP;
	@FXML	private	ToggleButton						toggleButtonEnvironment;
	@FXML	private	ToggleButton						toggleButtonEstimation;
	@FXML	private ToggleButton						toggleButtonRetrainAgents;
	@FXML	private	ToggleButton						toggleButtonViewSingleEnvironment;
	@FXML	private	ToggleButton						toggleButtonHeatPlot;
	
			public ArrayList<AbstractTab>				tabs;
			public AbstractTab							tabIntroduction;
			public AbstractTab							tabMDP;
			public AbstractTab							tabEnvironment;
			public AbstractTab							tabEstimation;
			public AbstractTab							tabRetrainAgents;
			public AbstractTab							tabViewSingleEnvironment;
			public AbstractTab							tabHeatPlot;
	
			
	
	/////////////////////////////////////////////////////////////////////////////////////
	/////////////////////// 	Constructor and initialization 	/////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	public FrameController (Model model)
	{
		// Set the main objects
		this.model = model;
		this.mdpBuilder = new MarkovDecisionProcessBuilder();
		environmentPoolFullySpecified = FXCollections.observableArrayList();
		environmentPoolLazySpecified = FXCollections.observableArrayList();
		this.estimationBuilder = new EstimationBuilder();
		
		// Parse the FXML and start all the nodes
		try {startStage();} catch (Exception e) {System.out.println("Could not start the frame stage.");e.printStackTrace();}

		//Register the view at the ObserverManager
		this.toaster = new Toaster(contentPaneContainer);
		ObserverManager.registerObserver(this);
		
		// Load the defaults
		try {
			loadDefaults(Defaults.mainDefaults); 
		}catch (Exception e) { };
	}
	
	/**
	 * Start the Window.
	 * @throws IOException
	 */
	private void startStage () throws IOException
	{		
		// Start the stage
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource("Frame.fxml"));
		loader.setController(this);
		Parent root = (Parent) loader.load();

		scene = new Scene(root);
		stage = new Stage();
		stage.setTitle("Adaptive information impulsivity version 1.0");
		stage.setScene(scene);


		// Initialize all nodes in the scene
		startNodes();
		
		// If the main window stage closes, all opened substages should likewise close
		stage.setOnCloseRequest(e -> System.exit(0));
		stage.show();
		
		// Load all the style classes that we might need to change the layout (e.g., for changing the layout of invalid text fields, tooltip layout etc)
		scene.getStylesheets().add(this.getClass().getResource("../CSSLayout/normalTextField.css").toExternalForm());
		scene.getStylesheets().add(this.getClass().getResource("../CSSLayout/normalSpinner.css").toExternalForm());
		scene.getStylesheets().add(this.getClass().getResource("../CSSLayout/normalTooltip.css").toExternalForm());
		
		// For all tabs: initialize the nodes by calling setNodes()
		for (AbstractTab tab: tabs)
			tab.setNodes();
		
		// Start with the introduction tab selected
		hideAllContent();
		setSelected(toggleButtonIntroduction);
	}
	 
	/** Set all settings in the frame to match the defaults */
	private void loadDefaults(Defaults defaults) {
		// Set the MDP builder to the defaults
		try {	mdpBuilder.setDefaults(defaults);	} catch (Exception e) {e.printStackTrace();}
		
		// Set the environments specified in the defaults
		for (EnvironmentBuilderFull ebf: defaults.startingEnvironmentPopulationFull)	 { 	
			ebf.setMDPBuilder(mdpBuilder);
			ebf.update();
			environmentPoolFullySpecified.add(ebf);	
		} 
		
		for (EnvironmentBuilderLazy ebl: defaults.startingEnvironmentPopulationLazy)	 { 	
			environmentPoolLazySpecified.add(ebl);
		} 
		
		// Set the estimation builder
		estimationBuilder.loadDefaults(defaults);
		
		// Update all tabs
		notifyAll(mdpBuilder, defaults);
		notifyAll(environmentPoolFullySpecified, defaults);
		notifyAll(estimationBuilder, defaults);
		
		// Update the ViewSingleEnvironmentTab separately - updating this tab will cause the system to read in all files.
		// This is only required once - at startup
		this.tabViewSingleEnvironment.update();
	}
	
	private void startNodes() {
		//Put all the toggleButtons in the arraylist
		this.toggleButtons = new ArrayList<>();
		toggleButtons.add(toggleButtonIntroduction);
		toggleButtons.add(toggleButtonMDP);
		toggleButtons.add(toggleButtonEnvironment);
		toggleButtons.add(toggleButtonEstimation);
		toggleButtons.add(toggleButtonRetrainAgents);
		toggleButtons.add(toggleButtonViewSingleEnvironment);
		toggleButtons.add(toggleButtonHeatPlot);
		
		for (ToggleButton tb: toggleButtons)
			tb.setOnAction(this);
		
		/// Create the tabs and place them in a collection
		this.tabs = new ArrayList<>();
		
		tabIntroduction = new TabIntroductionController(this);
		addTab(tabIntroduction);
		
		tabMDP = new TabMDP(this);
		addTab(tabMDP);
		
		tabEnvironment = new TabEnvironmentController(this);
		addTab(tabEnvironment);
		
		tabEstimation = new TabEstimationController(this);
		addTab(tabEstimation);
		
		tabRetrainAgents = new TabRetrainAgentsController(this);
		addTab(tabRetrainAgents);
		
		tabViewSingleEnvironment = new TabViewSingleEnvironmentController(this);
		addTab(tabViewSingleEnvironment);
		
		tabHeatPlot = new TabHeatPlotController(this);
		addTab(tabHeatPlot);
		
		//Menu items
		menuItemRunRunSimulation.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				StringBuilder detailBuilder = new StringBuilder();
				detailBuilder.append("Markov decision process parameters\n" + Helper.repString("=", 50)+"\n"+mdpBuilder);
				
				detailBuilder.append("\n\n\nFully specified environments\n" + Helper.repString("=", 50)+"\n");
				if (environmentPoolFullySpecified.size() ==0)
					detailBuilder.append("<None specified>");
				for (EnvironmentBuilderFull eb: environmentPoolFullySpecified) detailBuilder.append("\nNext fully specified environment\n"+Helper.repString("-", 25)+eb);
				
				detailBuilder.append("\n\n\nLazy generated environments\n" + Helper.repString("=", 50)+"\n");
				if (environmentPoolLazySpecified.size() ==0)
					detailBuilder.append("<None specified>");
				for (EnvironmentBuilderLazy ebf: environmentPoolLazySpecified) detailBuilder.append("\n"+ebf);

				detailBuilder.append("\n\n\nEstimation parameters\n" + Helper.repString("=", 50)+"\n"+estimationBuilder);
			
				if (ObserverManager.showConfirmationMessage("Confirmation required", 
						"This will start the simulation. Before starting, please consult the \"See details\" to make sure all parameters have been properly specified. \n\nDo you want to continue and run the simulation?",
						"Yes",
						"No",
						detailBuilder.toString())) {
					ObserverManager.makeToast("Starting simulation...");
					model.runSimulation(mdpBuilder, environmentPoolFullySpecified, environmentPoolLazySpecified, estimationBuilder);
				}
				else
					ObserverManager.makeToast("Simulation aborted");
				
			}});
	}

	/////////////////////////////////////////////////////////////////////////////////////  
	///////////////////////// 	Running simulation 	///////////////////////////////////// 
	/////////////////////////////////////////////////////////////////////////////////////
	SecondaryStageController_SimulationProgress simulationProgressBarStage;
	public void showSimulation(long startTime, int totalNumberOfAgents) {
		this.scrollPane.setOpacity(0.25);
		simulationProgressBarStage = new SecondaryStageController_SimulationProgress(this, startTime, totalNumberOfAgents);
	}
	
	public void stopShowingSimulation() {
		if (simulationProgressBarStage != null)
			simulationProgressBarStage.getStage().close();
		
		this.scrollPane.setOpacity(1);
	}
	
	public void agentFinished(int agentsCompleted) {
		simulationProgressBarStage.refresh(agentsCompleted);
	}
	
	public void requestSimulationCancellation() {
		model.cancelSimulation();
	}
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Mechanics 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////
	public interface TabPane {
		public AnchorPane getRootPane();
	}
	@Override
	public void handle(ActionEvent event) {
		if (event.getSource() instanceof ToggleButton) {
			hideAllContent();
			setSelected((ToggleButton) event.getSource());
		}
	}

	/** Sets all toggle buttons to their off mode, and hides all tabs in the contentPane */
	private void hideAllContent () {
		for (ToggleButton tb: toggleButtons) 
			tb.setSelected(false);
			
		for (AbstractTab tab: tabs)
			tab.getRootPane().setVisible(false);
	}

	private void setSelected(ToggleButton tb) {
		scrollPane.setVvalue(0);
		tb.setSelected(true);
		
		if (tb == toggleButtonIntroduction)				{tabIntroduction.getRootPane().setVisible(true);			anchorPaneScrollPane.setPrefHeight(tabIntroduction.getHeight());	}
		if (tb == toggleButtonMDP)						{tabMDP.getRootPane().setVisible(true);						anchorPaneScrollPane.setPrefHeight(tabMDP.getHeight());	}
		if (tb == toggleButtonEnvironment)				{tabEnvironment.getRootPane().setVisible(true);				anchorPaneScrollPane.setPrefHeight(tabEnvironment.getHeight());	}
		if (tb == toggleButtonEstimation)				{tabEstimation.getRootPane().setVisible(true);				anchorPaneScrollPane.setPrefHeight(tabEstimation.getHeight());	}
		if (tb == toggleButtonRetrainAgents)			{tabRetrainAgents.getRootPane().setVisible(true); 			anchorPaneScrollPane.setPrefHeight(tabRetrainAgents.getHeight());	}
		if (tb == toggleButtonViewSingleEnvironment)	{tabViewSingleEnvironment.getRootPane().setVisible(true);	anchorPaneScrollPane.setPrefHeight(tabViewSingleEnvironment.getHeight());	}
		if (tb == toggleButtonHeatPlot)					{tabHeatPlot.getRootPane().setVisible(true);				anchorPaneScrollPane.setPrefHeight(tabHeatPlot.getHeight());	}

	}
	
	/** Add an AbstractTab to the frame */
	private void addTab(AbstractTab tab) {
		this.tabs.add(tab);
		AnchorPane tabPane = tab.getRootPane();
		contentPane.getChildren().add(tabPane);
		AnchorPane.setTopAnchor(tabPane, 0.0);
		AnchorPane.setBottomAnchor(tabPane, 0.0);
		AnchorPane.setLeftAnchor(tabPane, 0.0);
		AnchorPane.setRightAnchor(tabPane, 0.0);
		
	}
	
	/** Notifies all stages that the objectChanged (e.g., the MDP builder) has changed, 
	 * and that the stage needs to update its nodes to reflect this change. Note that this
	 * updating might require more than cosmetic (e.g., updating text field) changes:
	 * for instance, the MDPBuilder stage needs to recreate a new cue emissions matrix if
	 * the MDP builder has changed. 
	 * 
	 * The source of the change is given via sourceOfChange.
	 * 
	 * In addition to specifying what object has changed and which object did the changing,
	 * any number of objects can be passed as arguments (Object... args). How these arguments 
	 * are used is unique per objectChanged.
	 * @param source
	 */
	@Override
	public void notifyAll(Object objectChanged, Object sourceOfChange, Object... args) {
		System.out.println(Helper.timestamp() + " FRAME: notifying all that a " + objectChanged.getClass().getSimpleName() + " was changed by a " + sourceOfChange.getClass().getSimpleName());
		
		// If the mdpBuilder has changed (NOT the cue emission matrix specifically)...
		if (objectChanged == mdpBuilder) { 
			try {
				updateMDPBuilder();	
			} catch (Exception e) {
			ObserverManager.notifyObserversOfError("Error when updating the Markov Decision Process", "An exception was encountered when updating the Markov Decision Problem. See details for more information. ", e);
		}
		}
		
		// If the cue emission matrix changed, and the source of this change was a DecimalNumberMatrixTableView,
		// set the cue emission type to manual
		if (objectChanged == mdpBuilder.CUE_EMISSION_MATRIX) {
			try {
				updateMDPBuilder();
				tabMDP.update();
				tabEnvironment.update();

			} catch (Exception e) {
				ObserverManager.notifyObserversOfError("Error when updating the Markov Decision Process", "An exception was encountered when updating the Markov Decision Problem. See details for more information. ", e);
			}
		}
				
		// If the environmentPool has changed, inform all tabs that use the EnvironmentPool that this is the case.
		if (objectChanged == environmentPoolFullySpecified) {
			tabEnvironment.update();
		}
			
		// If the source is an environmentBuilder, that means that the probability distribution of the
		// environmentBuilder has been adjusted by the user (most likely via a change in the "Change environment"
		// DecimalNumberMatrixTableView). 
		if (objectChanged instanceof EnvironmentBuilderFull) {
			EnvironmentBuilderFull builder = (EnvironmentBuilderFull) objectChanged;
			
			// if the source of the change is indeed a DecimalNumberMatrixTableView, then two
			// additional parameters are passed via args: the row index and the column index, respectively.
			// Also note that if a changed happened, that means that that environment Builder's value
			// distribution type for that column should be changed to manual
			if (sourceOfChange instanceof TabEnvironmentController) {
				System.out.println("\t- The change was made at row: " + args[0] + " and column: " + args[1]);
				builder.update();
			}
			
			if (sourceOfChange instanceof SecondaryStageController_ChangeEnvironment) {
				
				// If args is not empty, that means that the change was made by a change in the DecimalNumberTableView
				// Since this means that the updating is not completed yet (the user might change some more), the
				// builder's update() should NOT be called yet
				if (args != null)
				{
					System.out.println("\t- The change was made at row: " + args[0] + " and column: " + args[1]);
					
					// The 0th column corresponds to the value column - and cannot be changed
					// The 1st column corresponds to the Resource value column
					if ((int)args[1]==1) builder.setResourceValueDistributionType(ValueDistributionType.Manual);
					
					// The 2nd column corresponds to the extrinsic event value column
					if ((int) args[1]==2) builder.setExtrinsicEventDistributionType(ValueDistributionType.Manual);
				}
				
			}
				
			tabEnvironment.update();
			
		}
		
		
		if (objectChanged == estimationBuilder) {
			tabEstimation.update();
		}
	}
	
	/** Notifies all stages that the objectChanged (e.g., the MDP builder) has changed, 
	 * and that the stage needs to update its nodes to reflect this change. Note that this
	 * updating might require more than cosmetic (e.g., updating text field) changes:
	 * for instance, the MDPBuilder stage needs to recreate a new cue emissions matrix if
	 * the MDP builder has changed. 
	 * 
	 * The source of the change is given via sourceOfChange.
	 */
	@Override
	public void notifyAll(Object objectChanged, Object sourceOfChange) {
		notifyAll(objectChanged, sourceOfChange, (Object[]) null);
	}
	
	/** Update all the nodes in all tabs. */
	public void updateAll() {
		for (AbstractTab tab: tabs)
			tab.update();
	}
	
	/** Updates the MDPbuilder. This function does NOT 
	 * @throws IllegalScaleException 
	 * @throws IllegalRangeException 
	 * @throws ComputationException */
	public void updateMDPBuilder() {
		// Update all the fields
		mdpBuilder.updateBUDGET_STEP();

		CueEmissionMatrixBuilder.setCueEmissionMatrix(mdpBuilder.CUE_EMISSION_TYPE, mdpBuilder.CUE_EMISSION_ARGUMENT, mdpBuilder);
		for (EnvironmentBuilderFull eb: environmentPoolFullySpecified)
			eb.update();
		tabMDP.update();
		tabEnvironment.update();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	///////////////////////// 	Messaging system 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////


	@Override
	public void showErrorMessage(String title, String message, String details) {
		if (details == null)
			JavaFXHelper.showErrorAlert(title, message, this.stage);
		else
			JavaFXHelper.showErrorAlert(title, message, details, this.stage);
	}

	@Override
	public void showWarningMessage(String title, String message, String details) {
		if (details == null)
			JavaFXHelper.showWarningAlert(title, message, this.stage);
		else
			JavaFXHelper.showWarningAlert(title, message, details, this.stage);

	}

	@Override
	public void showToast(String message) {
		toaster.makeToast(message);
	}

	@Override
	public boolean confirmationMessage(String title, String message, String textForYesButton, String textForNoButton, String details) {
		return JavaFXHelper.showConfirmationYesNoAlert(title, message, this.stage, textForYesButton, textForNoButton, details);
	}

	@Override
	public Stage getStage() {
		return this.stage;
	}

	@Override
	public void showWarningToast(String message) {
		toaster.makeWarningToast(message);
		
	}



	
	
}
