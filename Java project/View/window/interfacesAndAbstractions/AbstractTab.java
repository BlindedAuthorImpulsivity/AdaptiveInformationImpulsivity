package window.interfacesAndAbstractions;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import window.FrameController;
import window.FrameController.TabPane;

/**
 * An abstract class for all Tabs in the frame.
 * Each tab has at least the functions update (which is called by the FrameController in case
 * another tab has changed something that requires other tabs to be updated). Upon creation
 * the setNodes() should be called to set all action handlers. This function is called by 
 * the FrameController upon creation of a tab.
 *
 */
public abstract class AbstractTab implements FrameController.TabPane{

			protected final 		FrameController					frame;
	@FXML	protected 				AnchorPane						rootPane;


	/**
	 * The constructor for the AbstractTab. Note that the constructor
	 * does not call setNodes(). Rather, the FrameController calls
	 * this function upon creation.
	 * @param fc
	 * @param fxmlName
	 */
	public AbstractTab(FrameController fc, String fxmlName) {
		this.frame = fc;
		
		// Retrieve the anchorPane from the FXML file
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource(fxmlName));
		loader.setController(this);
		try {loader.load();} catch (IOException e) {e.printStackTrace();}

	}
	public AnchorPane getRootPane() {
		return rootPane;
	}
	
	/** Update all nodes on the tab */
	public abstract void update();

	/** Initializes all the event handlers etc for the nodes on the tab. */
	public abstract void setNodes();

	/** Returns the height of the root pane */
	public double getHeight() {
		return rootPane.getPrefHeight();
	}
}
