package window;

import java.io.File;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import window.interfacesAndAbstractions.AbstractPolicyPlot;
import window.interfacesAndAbstractions.AbstractSecondaryStage;

/** This class does not do much - it only opens a new window and loads the fxml file. The function
 * creating this object can call anchorPaneScrollPane to add a canvas to it.
 * @author jesse
 *
 */
public class SecondaryStageController_PolicyPlotPopup extends AbstractSecondaryStage {

	
	@FXML public AnchorPane	anchorScrollPane;
	@FXML public Button		buttonSaveToFile;
	
	public SecondaryStageController_PolicyPlotPopup(String title)
	{
		super(null, "secondaryStage_PolicyPlotPopup.fxml", title, 500,500, false);
		setNodes();
		update();
	}

	@Override
	public void setNodes() {
		
		
	}
	
	/** Links the policy plot to the button in this window. Note that the policy plot deals with drawing itself - the secondary stage here only saves a references, and performs no computation */
	public void setPolicyPlot(AbstractPolicyPlot plot) {
		buttonSaveToFile.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent arg0) {
				
				FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save plot image as...");
				ExtensionFilter filter	    = new FileChooser.ExtensionFilter("Image Files", "*.png");
				fileChooser.getExtensionFilters().add(filter);
				
				File file = fileChooser.showSaveDialog(stage);
				
				if (file == null)
					return;
				
				// Get a WritableImage that we can save
				plot.writeToFile(file);
			}
			
		});
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}
	
}
