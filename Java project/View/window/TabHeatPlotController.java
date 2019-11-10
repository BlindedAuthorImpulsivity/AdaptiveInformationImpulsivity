package window;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;

import defaults.Defaults;
import externalPrograms.RserveManager;
import helper.Helper;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import staticManagers.ObserverManager;
import window.interfacesAndAbstractions.AbstractTab;
import window.interfacesAndAbstractions.LayoutManager;
import window.interfacesAndAbstractions.LayoutManager.State;
import window.interfacesAndAbstractions.LayoutManager.TextFieldValidInputCriterium;

public class TabHeatPlotController extends AbstractTab{
	
	/** Variables are the dimensions that can be plotted - consisting of input variables (that which the user 'puts' into the model)
	* and output variables (that what the user 'gets out' of the model, e.g., the optimal number cues to sample) */
	public enum Variable {

		resourceValueMean	("Mean resource quality", 					"resourceValueMean",			"Mean resource values", 					"Mean"), 
		resourceValueSD		("Std.Dev. resource quality", 				"resourceValueSD",				"Standard deviation of resource values", 		"SD"), 
		extrinsicEventMean	("Mean extrinsic event", 					"extrinsicEventMean",			"Mean extrinsic event values",			"Mean"),
		extrinsicEvent		("Std.Dev. extrinsic event", 				"extrinsicEventSD",				"Standard deviation of extrinsic event value", "SD"),
		interruptionRate	("Interruption rate", 						"interruptionRate",				"Interruption rate", 						"Rate"),
		budgetStart			("Starting somatic state" , 				"budget", 						"Budget when starting encounter",			"Budget"),
		
		
		cuesSampled			("Optimal number of cues to sample",			"expectedCuesSampled", 	"Optimal number of cues to sample",			"Cues sampled"),
		proportionEating	("Proportion population accepting",				"totalProportionAccepting",  			"Proportion of population accepting",			"Proportion"),
		proportionDiscarding("Proportion population rejecing",		    	"totalProportionDiscarding",			"Proportion of population rejecting",		"Proportion"),
		negativeCueSurplusRejecting("Negative cue surplus when rejecting",		    	"cueDominanceDiscarding_Negative_cue",			"Negative cue surplus rejecting",		"Surplus"),
		negativeCueSurplusAccepting("Negative cue surplus when accepting",		    	"cueDominanceEating_Negative_cue",				"Negative cue surplus accepting",		"Surplus"),
		positiveCueSurplusRejecting("Positive cue surplus when rejecting",		    	"cueDominanceDiscarding_Positive_cue",			"Positive cue surplus rejecting",		"Surplus"),
		positiveCueSurplusAccepting("Positive cue surplus when accepting",		    	"cueDominanceEating_Positive_cue",				"Positive cue surplus accepting",		"Surplus");
		
		// MARKER: if you want to add new variables to plot, make sure they are added below as well as above
		public static final Variable[] inputVariables  = new Variable[] {resourceValueMean, resourceValueSD, extrinsicEventMean, extrinsicEvent, interruptionRate, budgetStart};
		public static final Variable[] outputVariables = new Variable[] {cuesSampled, proportionEating, proportionDiscarding, negativeCueSurplusRejecting, negativeCueSurplusAccepting, positiveCueSurplusRejecting, positiveCueSurplusAccepting};
		
		private final String nameToDisplay, variableNameToUseInR, longNameInPlot, shortNameInPlot;
		private Variable(String nameToDisplay, String variableNameToUseInR, String longNameInPlot, String shortNameInPlot) {
			this.nameToDisplay = nameToDisplay;
			this.variableNameToUseInR = variableNameToUseInR;
			this.longNameInPlot = longNameInPlot;
			this.shortNameInPlot = shortNameInPlot; 
		}
		
		@Override
		public String toString(){return this.nameToDisplay;}
		public String toRString() {return "list(varName = \"" +  variableNameToUseInR + "\", longName = \"" + longNameInPlot + "\", shortName = \"" + shortNameInPlot +"\")";}

	}
	
	/** A role is a function in the plot that a variable has. For instance, the variable "cues sampled" can be used as the y-axis, as the color or used to make contour lines */
	public enum Role {
		color				("Color (dependent variable in individual plot)","color",				"color",				false), 
		xAxis				("X axis (individual plot)",					"X axis",				"xAxis",				false), 
		yAxis				("Y axis (individual plot)", 					"Y axis",				"yAxis",				false), 
		row					("Row (multiple plots, requires levels)", 		"row",					"row",					true),
		column				("Column (multiple plots, requires levels)", 	"column",				"column",				true),
		lowPass				("Low pass (exclude higher values than)", 		"low pass",				"lowPass",				true),
		highPass			("High pass(exclude lower values than)",	 	"high pass",			"highPass",				true),
		constant			("Constant (requires single level)", 			"constant",				"constant",				true);
		
		public final String fullName, shortName, nameToUseInR;
		public final boolean requiresLevels;
		private Role(String fullName, String shortName, String nameToUseInR, boolean requiresLevels) {
			this.fullName = fullName;
			this.shortName = shortName;
			this.nameToUseInR = nameToUseInR;
			this.requiresLevels = requiresLevels;
		}

		@Override
		public String toString(){return this.fullName;}


	}
	
	public class Included {
		private final Variable var; 
		private final Role role;
		private final ArrayList<Double> levels;
		public Included(Variable var, Role role, ArrayList<Double> levels) {
			this.var = var;
			this.role = role;
			this.levels = levels;
		}
		public String getVariable() { return var.toString(); }
		public String getRole() { return role.shortName; }
		public String getLevels() { if (levels.size()> 0) return Helper.arrayToString(levels); else return "None";}
		public String toString() 		{ 
			if (levels.size() == 0) 		return var + " (" + role.shortName + ")";		
			else 							return var + " (" + role.shortName + ", with levels " + Helper.arrayToString(levels) + ")";
		}
		public String toRString() {			return "list(var=" +var.toRString() + ", role = \"" + role.nameToUseInR + "\",levels =" + Helper.concatenateRStyle(levels) + ")";}
	}

	
		
	@FXML public	TextField				textfieldInputCSV;
	@FXML public	TextField				textfieldSavePlotTo;
	@FXML public	Button 					buttonBrowseCSV;
	@FXML public	Button					buttonBrowseSavePlotTo;

	@FXML public 	ListView<Variable> 		listViewVariableInput;
	@FXML public 	ListView<Variable> 		listViewVariableOutput;
	@FXML public 	ListView<Role> 			listViewRole;
	@FXML public	TableView<Included> 	tableViewIncluded;
	@FXML public	TextField				textFieldLevels;
	@FXML public	Button					buttonAdd;
	@FXML public 	Button					buttonRemove;

	@FXML public	TextField				textfieldWinsorizeCues;
	@FXML public	TextField				textfieldPlotName;
	@FXML public 	CheckBox				checkBoxUseBlackAndWhite;
	@FXML public	TextField				textFieldHeight;
	@FXML public	TextField				textFieldWidth;
	@FXML public 	TextField				textFieldDPI;
	
	@FXML public	ProgressIndicator 		progressIndicatorPlotting;
	@FXML public 	AnchorPane 				anchorPanePlotField;
	@FXML public	ImageView				imageHeatPlot;
	@FXML public	Button					buttonCreatePlot;

	private File csvFile;
	private File savePlotToFolder;

	public TabHeatPlotController(FrameController fc) {
		super(fc, "paneHeatPlot.fxml");
	}

	@Override
	public void setNodes() {
		// Set all the nodes for the select CSV input folder.
		// When the "Browse" button next to the .CSV file is pressed: make the user select a .csv file
		this.buttonBrowseCSV.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				FileChooser fileChooser = new FileChooser();

				// Set extension filter
				FileChooser.ExtensionFilter extFilter = 
						new FileChooser.ExtensionFilter(".CSV files (*.csv)", "*.csv");
				fileChooser.getExtensionFilters().add(extFilter);

				File file = fileChooser.showOpenDialog(frame.stage);
				if (file != null) {
					csvFile = file;
					textfieldInputCSV.setText(file.getPath());
				}
			}
		});


		// Set all the nodes for the save plot to output folder.
		this.buttonBrowseSavePlotTo.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				DirectoryChooser dc = new DirectoryChooser();
				File directory = dc.showDialog(frame.stage);
				if (directory != null) {
					savePlotToFolder = directory;
					textfieldSavePlotTo.setText(directory.getAbsolutePath());
				}
			}
		});
		
		// Initialize all the nodes under "Select plot settings"
		listViewVariableInput.getItems().addAll(Variable.inputVariables);
		listViewVariableOutput.getItems().addAll(Variable.outputVariables);
		listViewVariableInput.setOnMouseClicked(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
				Platform.runLater(() -> listViewVariableOutput.getSelectionModel().select(null));
			}
		});
		listViewVariableOutput.setOnMouseClicked(new EventHandler<MouseEvent>() {
			public void handle(MouseEvent event) {
				Platform.runLater(() -> listViewVariableInput.getSelectionModel().select(null));
			}
		});
		listViewRole.getItems().addAll(Role.values());
		
		buttonAdd.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				addButtonPressed();
			}
		});

		buttonRemove.setOnAction(new EventHandler<ActionEvent>() {
			public void handle(ActionEvent event) {
				Included i = tableViewIncluded.getSelectionModel().getSelectedItem();
				tableViewIncluded.getSelectionModel().select(null);
				tableViewIncluded.getItems().remove(i);
				
				if (listViewVariableInput.getItems().contains(i.var))
					listViewVariableInput.getSelectionModel().select(i.var);
				else
					listViewVariableOutput.getSelectionModel().select(i.var);

				listViewRole.getSelectionModel().select(i.role);
				StringBuilder levelsBuilder = new StringBuilder();
				for (double d: i.levels) levelsBuilder.append(d+ " ");
				textFieldLevels.setText(levelsBuilder.toString());
			}
		});
		textFieldLevels.setOnAction(new EventHandler<ActionEvent>() { public void handle(ActionEvent event) {addButtonPressed();}});
		textFieldLevels.textProperty().addListener(new ChangeListener<String>() {
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if( listViewRole.getSelectionModel().getSelectedItem().requiresLevels)
					if (validLevelsInput(newValue))
						LayoutManager.setState(textFieldLevels, State.Normal);
					else
						LayoutManager.setState(textFieldLevels, State.Invalid);
			}
		});
		listViewRole.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Role>() {
			public void changed(ObservableValue<? extends Role> observable, Role oldValue, Role newValue) {
				if (newValue.requiresLevels) {
					textFieldLevels.setDisable(false);
					if (validLevelsInput(textFieldLevels.getText()))
						LayoutManager.setState(textFieldLevels, State.Normal);
					else
						LayoutManager.setState(textFieldLevels, State.Invalid);
				} else {
					textFieldLevels.setDisable(true);
				}
					
			}});
		
		// Set the tableViewIncluded
		setTableViewIncluded();
		
		// Set the text field for winsorizing cues
		LayoutManager.setLayoutHandlerAllowingEmpty(textfieldWinsorizeCues, TextFieldValidInputCriterium.POSITIVE_DOUBLE);
		LayoutManager.setLayoutHandler(textFieldHeight, TextFieldValidInputCriterium.POSITIVE_DOUBLE);	
		LayoutManager.setLayoutHandler(textFieldWidth, TextFieldValidInputCriterium.POSITIVE_DOUBLE);	
		LayoutManager.setLayoutHandler(textFieldDPI, TextFieldValidInputCriterium.POSITIVE_INTEGER);
		
		// Set the Create plot button
		buttonCreatePlot.setOnAction(new EventHandler<ActionEvent> () {
			public void handle(ActionEvent event) {
				createHeatPlot();
			}});
		
		// Hide the progressIndicatorPlotting
		progressIndicatorPlotting.setVisible(false);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	/** Initializes the tableViewIncluded */
	public void setTableViewIncluded() {
		DoubleBinding width = tableViewIncluded.widthProperty().subtract(2).divide(6);
		
		TableColumn<Included, String> varColumn = new TableColumn<>("Variable");
		varColumn.setCellValueFactory(new PropertyValueFactory<Included, String>("variable"));
		varColumn.prefWidthProperty().bind(width.multiply(3));
		tableViewIncluded.getColumns().add(varColumn);
		
		TableColumn<Included, String> roleColumn = new TableColumn<>("Role");
		roleColumn.setCellValueFactory(new PropertyValueFactory<Included, String>("role"));
		roleColumn.prefWidthProperty().bind(width.multiply(2));
		tableViewIncluded.getColumns().add(roleColumn);
		
		TableColumn<Included, String> levelsColumn = new TableColumn<>("Levels");
		levelsColumn.setCellValueFactory(new PropertyValueFactory<Included, String>("levels"));
		levelsColumn.prefWidthProperty().bind(width);
		tableViewIncluded.getColumns().add(levelsColumn);
		
	}
	
	/** Tests whether a string is a valid input for the levels text field. An input is valid if, and only if,
	 * it follows the following structure:
	 * [an optional separator] [double] (+ any number of: ([seperator] + [double]))
	 * @param s
	 * @return
	 */
	public static boolean validLevelsInput(String s)
	{
		if (s.length()==0)
			return false;

		String doubleRegex = "((-?\\d*)|(-?\\d*\\.\\d+))";
		String seperatorRegex = "((\\s)*([\\s:;,_|\\-&])(\\s)*)";
		String plotRoleArgumentRegex = doubleRegex + "(" + seperatorRegex + doubleRegex+")*";

		return s.matches(plotRoleArgumentRegex);
	}

	/** Parse all levels in the textFieldLevels to a double arraylist. This list has size 0 if no (or no valid) levels are specified */
	private ArrayList<Double> getLevels(){
		ArrayList<Double> levels = new ArrayList<>();
		String s = textFieldLevels.getText();
		if (s.length() == 0) return levels;

		// Replace all white spaces with a ','
		s=s.replaceAll("\\s+",",");

		// replace all other separator signs with a comma:
		s=s.replace(':', ',');
		s=s.replace(";", ",");
		s=s.replace("_", ",");
		s=s.replace("|", ",");
		s=s.replace("-", ",");
		s=s.replace("&", ",");

		for (String sub: s.split(",")) {
			if (Helper.isDouble(sub))
				levels.add(Double.parseDouble(sub));
		}
		
		return levels;
	}

	/** Called when the add button is pressed, or enter is pressed on textFieldLevels */
	private void addButtonPressed() {
		Variable var = listViewVariableInput.getSelectionModel().getSelectedItem();
		if (var == null) var = listViewVariableOutput.getSelectionModel().getSelectedItem();
		
		Role role = listViewRole.getSelectionModel().getSelectedItem();
		ArrayList<Double> levels = new ArrayList<>();
		
		// First, make sure that both a variable and a plot is selected
		if (var == null || role ==null) {
			ObserverManager.makeWarningToast("Please select a variable and plot role");
			return;
		}
		
		// If the selected plot role requires levels: make sure that these levels are specified
		if (role.requiresLevels)	{
			levels  = getLevels();
			if (levels.size()==0) {
				ObserverManager.makeWarningToast("Please specify the levels for the " + role);
				return;
			}
		}
		
		// Make sure that the role is not already in the included list if the role is x axis/y axis/row/column
		if (role == Role.xAxis || role == Role.yAxis || role == Role.row || role == Role.column)
			for (Included i: tableViewIncluded.getItems())
				if (i.role == role) {
					ObserverManager.makeWarningToast("The list of included dimensions already has a " + role.shortName + ". Please select another plot role, or remove the plot role from the included section");
					return;
				}
		
		// If you've made it to this point, the included can be added
		tableViewIncluded.getItems().add(new Included(var, role, levels));
		
	}
	
	/**
	 * This function is called by the event handler of the create plot button. First, this function checks if all input is valid.
	 * If so, it then this function sends a string containing the R command to the model. The model will then instruct R to plot it,
	 * wait until the plotting is complete, and call setNewPlot() to set the new plot.
	 */
	private void createHeatPlot()
	{
		// First, check if a .csv file is selected
		if (csvFile == null) {
			ObserverManager.makeWarningToast("Please select a .csv file to plot");
			return;
		}
		
		// Check if the user selected a directory to store the plot in
		if (this.savePlotToFolder == null) {
			ObserverManager.makeWarningToast("Please select a folder to save the plot to");
			return;
		}
			
		// Next, check if all input is valid: there should at least be 2 effects, containing at least a value for color
		if (tableViewIncluded.getItems().size() < 2) {
			ObserverManager.makeWarningToast("Please specify at least two different dimensions for the plot");
			return;
		}
		
		boolean includedColor = false;
		for (Included i: tableViewIncluded.getItems())
			if (i.role==Role.color)
				includedColor = true;
		if (!includedColor) {
			ObserverManager.makeWarningToast("Please specify what variable should be used for the colors");
			return;
		}
		
		// Make sure the textfieldWinsorizeCues is valid - that is, allow for empty values
		double maximumCues=-1;
		if (textfieldWinsorizeCues.getText().length()==0)
			maximumCues = -1;
		else if (!LayoutManager.isInvalid(textfieldWinsorizeCues) ) {
			maximumCues = Double.parseDouble(textfieldWinsorizeCues.getText());
			LayoutManager.setProcessed(textfieldWinsorizeCues);
		}
		
		// Make sure the width, height and DPI are specified
		if (textFieldWidth.getText().length()==0 || LayoutManager.isInvalid(textFieldWidth)) {
			ObserverManager.makeWarningToast("The width of the plot is misspecified. Please enter a valid (positive) number");
			return;
		}
		if (textFieldHeight.getText().length()==0 || LayoutManager.isInvalid(textFieldHeight)) {
			ObserverManager.makeWarningToast("The height of the plot is misspecified. Please enter a valid (positive) number");
			return;
		}
		if (textFieldDPI.getText().length()==0 || LayoutManager.isInvalid(textFieldDPI)) {
			ObserverManager.makeWarningToast("The Dots Per Inch (DPI) of the plot is misspecified. Please enter a valid (positive) whole number");
			return;
		}
			
		double width =  Double.parseDouble(textFieldWidth.getText());
		double height = Double.parseDouble(textFieldHeight.getText());
		int DPI = Integer.parseInt(textFieldDPI.getText());
		String useColor = "TRUE";
		if (checkBoxUseBlackAndWhite.isSelected())
			useColor = "FALSE";
		

		/* Next, tell R to plot. The R function responsible for plotting is called "createHeatPlot" .
		 * The file containing this function is sourced when starting the RServe object. It takes as input (in this order):
		 *
		 * inputFile:			an absolute pathway to the .csv file used as the data set. Note that R uses "/"'s, while Java uses "\"s
		 * outputFile:			an absolute pathway to the to-be-created image. Note that R uses "/"'s, while Java uses "\"s
		 * dimensions:			a list containing the Included information. For example:
		 * 						dimensions = list(
  		 * 							list(var=list(varName = "resourceValueMean", longName = "Mean resource values", shortName = "Mean"), role = "xAxis",levels =c()), 
  		 *							list(var=list(varName = "extrinsicEventMean", longName = "Mean extrinsic event values", shortName = "Mean"), role = "yAxis",levels =c()), 
  		 *							list(var=list(varName = "resourceValueSD", longName = "Standard deviation of resource values", shortName = "SD"), role = "row",levels =c(2.0000000000,6.0000000000,10.0000000000)), 
  		 *							list(var=list(varName = "extrinsicEventSD", longName = "Standard deviation of extrinsic event value", shortName = "SD"), role = "column",levels =c(2.0000000000,6.0000000000,10.0000000000)), 
  		 *							list(var=list(varName = "optimalNumberOfCuesToSample", longName = "Optimal number of cues to sample", shortName = "Cues sampled"), role = "color",levels =c()),
		 *  						
		 * maxColor:			a numeric value indicating the maximum cues that will be displayed when plotting. All values higher than this value are winsorized. Use negative numbers to indicate no maximum.
		 * width				a double that determines the width of the total plot (in cm)
		 * height				a double that determines the height of the total plot (in cm)
		 * DPI					a double that determines the Dots Per Inch 
		 */
		// Create the inputFile string
		String inputFile = "\""+ csvFile.getAbsolutePath().replace("\\", "/") + "\"";

		// Create the outputFile string. Make sure the file name is unique, and ends with .png
		String outputFilename = textfieldPlotName.getText();
		String cleanFilename = "nameless";
		if (outputFilename.length()>0)
			cleanFilename = outputFilename;
		if (cleanFilename.contains("."))
			cleanFilename = cleanFilename.split("\\.")[0];
		File outputFile = new File(this.savePlotToFolder.getAbsolutePath() + "\\" + cleanFilename + ".png");

		// Ensure that the filename is unique. If it is not, change the name by appending (x) at the end
		int counter = 1;
		while (outputFile.exists())
		{
			String alternateFilename = savePlotToFolder.getAbsolutePath() + "\\"+ cleanFilename+ "(" +counter++ + ")" + ".png";
			outputFile = new File(alternateFilename);
		}
		String outputFileString = "\"" + outputFile.getAbsolutePath().replace("\\", "/") + "\"";

		// Place all the Included in a list that can be read by R
		StringBuilder includedSB = new StringBuilder("list(");
		for (int i = 0; i < tableViewIncluded.getItems().size();i++) {
			Included inc = tableViewIncluded.getItems().get(i);
			includedSB.append(inc.toRString());
			if (i <  tableViewIncluded.getItems().size() -1)
				includedSB.append(", ");
		}
		includedSB.append(")");
		
		// Create the total R command:
		String RCommand = "createHeatPlotFromFile("+
				"inputFile = " + inputFile + ", "+
				"outputFile = " + outputFileString+ ", "+
				"dimensions = " + includedSB + ", " +
				"maxColor = " + maximumCues + ", " + 
				"width = " + width + ", "+
				"height = " + height + ", " + 
				"DPI = " + DPI +
				")";

		// Inform the user and call the model
		ObserverManager.makeToast("Creating plot " + outputFileString + ". R might take a while, please hold...");
		final File finalOutputFile = outputFile;
		Task<Void> task = new Task<Void>(){
			@Override
			protected Void call() throws Exception {
				progressIndicatorPlotting.setVisible(true);
				createHeatPlot(RCommand, finalOutputFile);
				progressIndicatorPlotting.setVisible(false);

				return null;
			}

		};
		new Thread(task).start();

	}
	
	
	public void createHeatPlot(String rCommand, File outputFile) 
	{
		try {
		System.out.println(Helper.timestamp() + " SENDING command: " + rCommand);
		System.out.println(Helper.timestamp() + " PLOTTING image " + outputFile);
		RserveManager.evaluate(rCommand);

		while (!outputFile.exists()) {
			System.err.println("FILE " + outputFile.getAbsolutePath() + " NOT FOUND YET. SLEEPING....");
			Thread.sleep(100);
		}
		System.out.println(Helper.timestamp() + " FOUND plotting file: " + outputFile.getAbsolutePath());
	
		FileInputStream inputImage = new FileInputStream(outputFile);
		Image image = new Image(inputImage);
		imageHeatPlot.setImage(image);
		imageHeatPlot.fitWidthProperty().bind(anchorPanePlotField.widthProperty().subtract(20));
		
		System.out.println(Helper.timestamp() + " FINISHED plotting complete.");
		
		} catch (REngineException  e) {
			ObserverManager.notifyObserversOfError("Exception encountered", "The Rserve server reported an error. See details for more information", e);
		
		} catch (REXPMismatchException e) {
			ObserverManager.notifyObserversOfError("Exception encountered",  "The Rserve server presented an object of an unexpected class.", e );
		
		} catch (InterruptedException e) {
			ObserverManager.notifyObserversOfError(e);
			
		} catch (FileNotFoundException e) {
			ObserverManager.notifyObserversOfError(e);
		}
	
	}

	
	
}
