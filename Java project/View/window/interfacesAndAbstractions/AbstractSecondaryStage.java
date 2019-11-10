package window.interfacesAndAbstractions;

import java.io.IOException;

import defaultAndHelper.JavaFXHelper;
import defaultAndHelper.JavaFXHelper.Toaster;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import staticManagers.ObserverManager;
import staticManagers.ObserverManager.ObserverStage;
import window.FrameController;

/** A secondary stage is a small stage that provides some functionality. This stage has a build-in toaster. Make sure that the
 * fxml file has a root anchor pane with the name "rootAnchor".
 *
 */
public abstract class AbstractSecondaryStage  implements ObserverStage {
	
	public final FrameController frame;
	public final Stage stage;
	public final Toaster toaster;
	@FXML public AnchorPane	rootAnchor;
	public final int significantDigits = 4;
	public final AbstractSecondaryStage ownReference = this;
	
	/**
	 * The super constructor that must be called. Note that this constructor does NOT call setNodes()!
	 * @param fc
	 * @param fxmlName
	 * @param title
	 * @param width
	 * @param height
	 * @param requiresValidation
	 */
	public AbstractSecondaryStage(FrameController fc, String fxmlName, String title, double width, double height, boolean requiresValidation) {
		this.frame = fc;
		
		// Start the stage
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource(fxmlName));
		loader.setController(this);
		Parent root = null;
		try { 			root = (Parent) loader.load();	} catch (IOException e) {	ObserverManager.notifyObserversOfError("Error starting secondary stage", "Unable to start secondary stage. See details.", e);		}
		
		Scene scene = new Scene(root, width,height);
		stage = new Stage();
		stage.initStyle(StageStyle.DECORATED);
		stage.setTitle(title);
		stage.setScene(scene);
		stage.show();

		// If the stage is requires validation:
		// override the functionality of the close button (a user should not be able to close the screen before changed have been validated)
		if (requiresValidation)
			stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
				@Override
				public void handle(WindowEvent event) {
					event.consume();
					toaster.makeToast("Cannot close this screen this way.");
				}
			});

		toaster = new Toaster(rootAnchor);

		
	}
	
	

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
	public void showWarningToast(String message) {
		toaster.makeWarningToast(message);
	}
	@Override
	public boolean confirmationMessage(String title, String message, String textForYesButton, String textForNoButton, String details) {
		return JavaFXHelper.showConfirmationYesNoAlert(title, message, this.stage, textForYesButton, textForNoButton, details);
	}

	@Override
	public Stage getStage() {
		return this.stage;
	}

	
	/** Initialize the nodes (handlers and such). Should not set values of nodes (this is done with update) */
	public abstract void setNodes();
	
	/** Update the nodes on the stage. Nodes should be initialized beforehand */
	public abstract void update();
	
	

}
