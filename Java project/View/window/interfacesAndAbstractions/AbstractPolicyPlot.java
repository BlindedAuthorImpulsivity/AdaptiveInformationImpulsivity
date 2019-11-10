package window.interfacesAndAbstractions;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import agent.Output;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.AnchorPane;
import staticManagers.ObserverManager;
import window.FrameController;

public abstract class AbstractPolicyPlot extends AnchorPane{
	protected final FrameController frame;
	
	//Anchor panes that are given upon construction
	protected final AnchorPane anchorPaneScrollPane;		// The anchorPane of the frame that comes directly after the scroll pane - it is the content pane plus the strips on the side
														// 		The scroll pane is where all other popups are drawn on.
	protected final AnchorPane anchorPanePlotting;		// The anchor pane in the ViewSingleEnvironmentTab that holds the actual plot
	protected final AnchorPane anchorPaneTitledPane;		// The anchor pane in the ViewSingleEnvironmentTab that holds the items in the "Optimal policy" tab 
														// 		- we have to scale the titledPane to fit the plot. An addition, the width of this pane determines
														//       the width of the plot.
	protected final Output output;						// The output object to plot
	
	/**
	 * The constructor of all PolicyPlots	
	 * @param anchorPaneScrollPane: The anchorPane of the frame that comes directly after the scroll pane - it is the content pane plus the strips on the side. The scroll pane is where all other popups are drawn on.
	 * @param anchorPaneTitledPane: The anchor pane in the ViewSingleEnvironmentTab that holds the actual plot
	 * @param anchorPanePlotting: The anchor pane in the ViewSingleEnvironmentTab that holds the items in the "Optimal policy" tab. We have to scale the titledPane to fit the plot. An addition, the width of this pane determines the width of the plot.
	 * @param output
	 */
	public AbstractPolicyPlot(
			FrameController frame,
			AnchorPane anchorPaneScrollPane,
			AnchorPane anchorPaneTitledPane,
			AnchorPane anchorPanePlotting,
			Output output) {
		this.frame = frame;
		this.anchorPaneScrollPane=anchorPaneScrollPane;
		this.anchorPanePlotting = anchorPanePlotting;
		this.anchorPaneTitledPane = anchorPaneTitledPane;
		this.output = output;
		
		// Set a listener on the width of the plot: if we change the width (i.e., when the stage dimensions are changed), we
		// should redraw
		this.anchorPaneScrollPane.widthProperty().addListener(widthPropertyListener);

		
	}
	
	/**
	 * Here all computations are placed that are required before the drawing can start (e.g., computing decision trees). 
	 * This function should ALWAYS run on a separate thread. 
	 * @throws Exception 
	 */
	public abstract void initialize() throws Exception; 
	
	/** Draw the plot. */
	public abstract void draw();

	/** First remove the existing plot, and all components, and then recreate that plot. This is used when
	 * for instance when the stage dimensions are changed. */
	public abstract void redraw();
	
	/** Remove or destroy all Objects in the PolicyPlot that are not used after this plot is removed. 
	 * For instance, remove mouseListeners on objects other than the PolicyPlot. In addition, remove
	 * this plot from its parent. 
	 */
	public abstract void destroy();

	/** Save the canvas to the file as is - i.e., with the dimensions as plotted on screen.
	 * Should include a scale to determine the DPI. This scale is defined in AbstractPolicyPlot */
	public final static double SCALE = 1;
	public abstract void writeToFile(File file );
	
	public static double snap(double loc) {
		return ((int) loc) + .5;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////
	//////////////////////////////// 	Width updating 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////

	// Drawing the plot takes some time, which means that resizing a window (and redrawing the plot during resizing)
	// can become quite stuttery (at least at some computers). Hence, instead of redrawing the plot every time the size of the
	// window changes, I use a thread that updates it only once every 100 milliseconds.
	// Note that the change of width is determined by placing a listener on the anchorPaneScrollPane's width property.
	// This listener has to be removed upon destruction of the PolicyPlot

	// The listener to detect changes in width
	protected final	ChangeListener<Number> widthPropertyListener = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			if (updateThreadRunning)
				return;
			updateThreadRunning = true;
			executorService.submit(updateTask);
		}
	};

	//The thread infrastructure to ensure a update is processes once every 100 milisec
	private 		boolean 			updateThreadRunning = false;
	private final	ExecutorService 	executorService = Executors.newFixedThreadPool(1);
	private 		Runnable 			updateTask = new Runnable() {
		@Override
		public void run()  {
			try {Thread.sleep(100);} catch (InterruptedException e) {ObserverManager.notifyObserversOfError(e);}
			Platform.runLater(new Runnable() { public void run() {redraw();}});
			updateThreadRunning = false;
		}
	};

}
