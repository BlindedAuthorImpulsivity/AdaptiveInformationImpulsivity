package window;

import java.text.DecimalFormat;

import defaultAndHelper.JavaFXHelper;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import staticManagers.ObserverManager;
import window.interfacesAndAbstractions.AbstractSecondaryStage;

public class SecondaryStageController_SimulationProgress extends AbstractSecondaryStage implements EventHandler<ActionEvent>{

	private final int			totalNumberOfAgents;
	private int 				agentsCompleted;

	private long				startTime;
	private Timeline			clock;
	
	private boolean 			finished;
	
	@FXML public Label 			labelProgressBar;
	@FXML public ProgressBar 	progressBar;
	@FXML public Label			labelElapsedTime;
	@FXML public Label			labelEstimatedTime;
	@FXML public Button			buttonCancelSimulation;
	
	public SecondaryStageController_SimulationProgress(FrameController fc, long startTime, int totalNumberOfAgents) {
		super(fc, "secondaryStage_SimulationProgress.fxml", "Running simulation....", 400, 150, true);
		this.startTime = startTime;
		this.agentsCompleted = 0;
		this.totalNumberOfAgents = totalNumberOfAgents;
		this.setNodes();
	}
	@Override
	public void setNodes() {
		clock = new Timeline(new KeyFrame(Duration.seconds(1), this));
		clock.setCycleCount(Timeline.INDEFINITE);
		clock.play();
		
		this.buttonCancelSimulation.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				if (finished) {
					frame.stopShowingSimulation();
					stage.close();
				}
				else if ( ObserverManager.showConfirmationMessage("Confirmation required", "This action will stop the simulation and will destroy all progress made thus far. This cannot be reversed. Are you sure you want to stop the simulation?", "Yes - Stop simulation", "No - continue simulation", null))
					frame.requestSimulationCancellation();
				
			}
		});

	}
	
	
	/**
	 * Called by the model (via the main window stage) to inform the progress bar that progress has been made, and hence that the bar should be updated
	 * @param agentsCompleted
	 */
	public void refresh(int agentsCompleted)
	{
		Platform.runLater(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				// Change the style of several nodes when the run is completed.
				if (agentsCompleted == totalNumberOfAgents)
				{
					clock.stop();
					finished = true;
					buttonCancelSimulation.setText("OK");
					labelEstimatedTime.setText("Simulation complete.");
					progressBar.setProgress(1);
					progressBar.getStyleClass().removeAll();
					progressBar.getStylesheets().add(getClass().getResource("ProgressBarComplete.css").toExternalForm());
					labelProgressBar.setText("Done");
					labelProgressBar.setTextFill(Color.WHITE);
					return;
				}
				

				int agentsNotCompleted = totalNumberOfAgents - agentsCompleted;
				double proportionDone = ((double) agentsCompleted)/((double) totalNumberOfAgents);
				DecimalFormat twoDigits = new DecimalFormat("0.##");
				
				// Set the text on the loading bar
				labelProgressBar.setText(agentsCompleted + " of " +totalNumberOfAgents + " completed [" + twoDigits.format(proportionDone*100) + "%]");
				
				// Calculate the elapsed time / number of completed agents
				double timePerAgent = ((double)(System.nanoTime() - startTime)) / ((double) agentsCompleted);
				
				// Calculate [agents still to go] * [time per agent]
				double estimatedTime = timePerAgent * agentsNotCompleted;
				
				// Format the estimated time remaining:
				labelEstimatedTime.setText(JavaFXHelper.formatNanoSeconds((long) estimatedTime, false));
				
				// set the progress bar
				progressBar.setProgress(proportionDone);
			}
			
		});
	}
		
		
	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}
	 /** The handle below fires every second - it is called by the clock. It's purpose is to update the 'estimated time'
	 */
	@Override
	public void handle(ActionEvent event) {
		// Set the elapsed time
		String elapsedTime = JavaFXHelper.formatNanoSeconds(System.nanoTime() - startTime, false);
		labelElapsedTime.setText(elapsedTime);
	}
	
	
	
	

}
