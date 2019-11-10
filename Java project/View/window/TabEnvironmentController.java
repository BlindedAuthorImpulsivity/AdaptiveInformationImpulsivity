package window;

import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrixTableView;
import decimalNumber.DecimalNumberMatrixTableView.TableViewObject;
import defaultAndHelper.ColorPalette;
import environment.EnvironmentBuilderFull;
import environment.EnvironmentBuilderLazy;
import environment.ValueDistributionType;
import helper.Helper;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import staticManagers.ObserverManager;
import window.SecondaryStageController_EnvironmentRange.SpecificationType;
import window.interfacesAndAbstractions.AbstractTab;

public class TabEnvironmentController extends AbstractTab {
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////// 		ENVIRONMENTS TAB		/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////
	private final int significantDigits = 4;

	////////// Environments - FXML Nodes
	@FXML public TableView<EnvironmentBuilderFull> tableViewFullySpecified;
	@FXML public Button buttonAddNew;
	@FXML public Button buttonChangeSelected;
	@FXML public Button buttonCopySelected;
	@FXML public Button buttonRemoveSelected;
	@FXML public Button buttonAddRangeFullySpecified;
	@FXML public Button buttonRemoveAllFullySpecified;

	@FXML public TableView<EnvironmentBuilderLazy> tableViewLazySpecified;
	@FXML public Button buttonAddRangeLazySpecified;
	@FXML public Button buttonRemoveAllLazySpecified;

	@FXML public AreaChart<Number, Number> plotShowEnvironment;
	@FXML public NumberAxis plotShowEnvironmentXAxis;
	@FXML public NumberAxis plotShowEnvironmentYAxis;
	
	public TabEnvironmentController(FrameController fc) {
		super(fc, "paneEnvironment.fxml");
	}

	@Override
	public void update() {
		// This function can (and will) be called from non-JAVAFX threads (e.g., handlers). Hence, it has to be placed
		// 		on the JAVAFX application thread by invoking Platform.runLater()
		Platform.runLater(new Runnable() {public void run() {
			// Redraw the plot to represent the selected (if any) environment 
			try {
				drawPlotShowEnvironment(tableViewFullySpecified.getSelectionModel().getSelectedItem());
			} catch (Exception e) {
				ObserverManager.notifyObserversOfError("Error when drawing the selected environment.", "Java has encountered an error when redrawing the selected environment. See details.", e);
			}

			tableViewFullySpecified.refresh();
			tableViewLazySpecified.refresh();
		}});
	}





	////////// Environments - Initialization function: call this when the program starts
	@Override
	public void setNodes()
	{
		setNodesFullySpecified();
		setNodesLazySpecified();
	}

	private void setNodesFullySpecified() {
		/////////////////////////////////////////////////////////////
		// Set the nodes on the fully specified environments pane ///
		/////////////////////////////////////////////////////////////
		DoubleBinding columnWidthFullySpecified = tableViewFullySpecified.widthProperty().subtract(2).divide(7);
		// Create and add the Resource quality super-column to the table view.
		TableColumn<EnvironmentBuilderFull, TableColumn<EnvironmentBuilderFull, Object>> resourceValueColumn = new TableColumn<EnvironmentBuilderFull, TableColumn<EnvironmentBuilderFull, Object>>("Resource quality");

		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "resourceValueMean", columnWidthFullySpecified, significantDigits, frame, frame.tabEnvironment));
		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "resourceValueSD", columnWidthFullySpecified,significantDigits, frame, frame.tabEnvironment ));
		resourceValueColumn.getColumns().add(createValueDistributionTypeColumnFull("Distribution Type", "resourceValueDistributionType", columnWidthFullySpecified));
		tableViewFullySpecified.getColumns().add(resourceValueColumn);

		// Create and add the extrinsic event super-column to the table view.
		TableColumn<EnvironmentBuilderFull, TableColumn<EnvironmentBuilderFull, Object>> extrinsicEventColumn = new TableColumn<EnvironmentBuilderFull, TableColumn<EnvironmentBuilderFull, Object>>("Extrinsic events");
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "extrinsicEventMean", columnWidthFullySpecified, significantDigits, frame, frame.tabEnvironment));
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "extrinsicEventSD",columnWidthFullySpecified,significantDigits, frame, frame.tabEnvironment));
		extrinsicEventColumn.getColumns().add(createValueDistributionTypeColumnFull("Distribution Type", "extrinsicEventDistributionType",columnWidthFullySpecified));
		tableViewFullySpecified.getColumns().add(extrinsicEventColumn);

		// Add the interruption rate column to the table view
		tableViewFullySpecified.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Interruption rate", "interruptionRate", columnWidthFullySpecified,significantDigits, frame, tableViewFullySpecified));

		// Add the items and make the table editable
		tableViewFullySpecified.setItems(frame.environmentPoolFullySpecified);
		tableViewFullySpecified.setEditable(true);

		// Set a change listener (this) to listen for changed in which row has been selected,
		// This change listener sounds out a notification that the selected row has changed.
		tableViewFullySpecified.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<EnvironmentBuilderFull>() {public void changed(ObservableValue<? extends EnvironmentBuilderFull> observable, EnvironmentBuilderFull oldValue,EnvironmentBuilderFull newValue) {
			if (newValue != null)
				update();	
		} });
		plotShowEnvironment.setVisible(false);

		// Set the double click on the table to open the change environment window
		tableViewFullySpecified.setOnMousePressed(new EventHandler<MouseEvent>() {
		    @Override 
		    public void handle(MouseEvent event) {
		        if (event.isPrimaryButtonDown() && event.getClickCount() == 2) 
		        	if (tableViewFullySpecified.getSelectionModel().getSelectedItem() != null)
						new SecondaryStageController_ChangeEnvironment(frame, tableViewFullySpecified.getSelectionModel().getSelectedItem());      
		        if (event.isSecondaryButtonDown())
		        	tableViewFullySpecified.getSelectionModel().select(null);
		    }
		});
		
		// Set the action listeners on the buttons
		this.buttonAddNew.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				ObserverManager.makeToast("- Adding a new environment."); 
				try {
					EnvironmentBuilderFull ebf = new EnvironmentBuilderFull(0, 1,ValueDistributionType.Normal, 0,1,ValueDistributionType.Normal, 0, null);
					ebf.setMDPBuilder(frame.mdpBuilder);
					ebf.update();
					frame.environmentPoolFullySpecified.add(ebf);
					frame.notifyAll(frame.environmentPoolFullySpecified, frame.tabEnvironment);
				} catch (IllegalArgumentException | IllegalRangeException | IllegalScaleException e) {
					ObserverManager.notifyObserversOfError(e);
				}
			}});
		this.buttonChangeSelected.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				EnvironmentBuilderFull selectedBuilder = tableViewFullySpecified.getSelectionModel().getSelectedItem();
				if (selectedBuilder != null)
					new SecondaryStageController_ChangeEnvironment(frame, selectedBuilder);
				else
					ObserverManager.makeWarningToast("Please select an environment before pressing this button.");
			}});   
		this.buttonCopySelected.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				ObserverManager.makeWarningToast("NOT IMPLEMENTED YET"); //TODO
			} });
		this.buttonRemoveSelected.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) { 
				ObserverManager.makeToast("Removing selected environment."); 
				frame.environmentPoolFullySpecified.remove(tableViewFullySpecified.getSelectionModel().getSelectedItem()); 
				frame.notifyAll(frame.environmentPoolFullySpecified, frame.tabEnvironment);
			}});
		this.buttonRemoveAllFullySpecified.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				ObserverManager.makeToast("Removing all environments."); 
				frame.environmentPoolFullySpecified.removeAll(frame.environmentPoolFullySpecified); 
				frame.notifyAll(frame.environmentPoolFullySpecified, frame.tabEnvironment);
			}});
		this.buttonAddRangeFullySpecified.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) { new SecondaryStageController_EnvironmentRange(frame, SpecificationType.Full);}
		});    


	}

	private void setNodesLazySpecified() {
		////////////////////////////////////////////////////////////
		// Set the nodes on the lazy specified environments pane ///
		////////////////////////////////////////////////////////////
		DoubleBinding columnWidthLazySpecified = tableViewLazySpecified.widthProperty().subtract(2).divide(7);
		// Create and add the Resource quality super-column to the table view.
		TableColumn<EnvironmentBuilderLazy, TableColumn<EnvironmentBuilderLazy, Object>> resourceValueColumn = new TableColumn<EnvironmentBuilderLazy, TableColumn<EnvironmentBuilderLazy, Object>>("Resource quality");

		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "resourceValueMean", columnWidthLazySpecified, significantDigits));
		resourceValueColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "resourceValueSD", columnWidthLazySpecified,significantDigits));
		resourceValueColumn.getColumns().add(createValueDistributionTypeColumnLazy("Distribution Type", "resourceValueDistributionType", columnWidthLazySpecified));
		tableViewLazySpecified.getColumns().add(resourceValueColumn);

		// Create and add the extrinsic event super-column to the table view.
		TableColumn<EnvironmentBuilderLazy, TableColumn<EnvironmentBuilderLazy, Object>> extrinsicEventColumn = new TableColumn<EnvironmentBuilderLazy, TableColumn<EnvironmentBuilderLazy, Object>>("Extrinsic events");
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Mean", "extrinsicEventMean", columnWidthLazySpecified, significantDigits));
		extrinsicEventColumn.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("SD", "extrinsicEventSD",columnWidthLazySpecified,significantDigits));
		extrinsicEventColumn.getColumns().add(createValueDistributionTypeColumnLazy("Distribution Type", "extrinsicEventDistributionType",columnWidthLazySpecified));
		tableViewLazySpecified.getColumns().add(extrinsicEventColumn);

		// Add the interruption rate column to the table view
		tableViewLazySpecified.getColumns().add(DecimalNumberMatrixTableView.createDecimalNumberColumn("Interruption rate", "interruptionRate", columnWidthLazySpecified,significantDigits));

		// Add the items and make the table editable
		tableViewLazySpecified.setItems(frame.environmentPoolLazySpecified);
		tableViewLazySpecified.setEditable(false);

		// Set the action listeners on the buttons
		this.buttonRemoveAllLazySpecified.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				ObserverManager.makeToast("Removing all lazy specified environments."); 
				frame.environmentPoolLazySpecified.removeAll(frame.environmentPoolLazySpecified); 
				frame.notifyAll(frame.environmentPoolLazySpecified, frame.tabEnvironment);
			}});
		this.buttonAddRangeLazySpecified.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) { new SecondaryStageController_EnvironmentRange(frame, SpecificationType.Lazy);}
		});    
	}

	////////// Environments - Computational functions
	/**
	 * Create and return a new TableColumn that houses ValueDistributionType values in a ComboBox.
	 * The object from which the ValueDistributionTypesare extracted (e.g., an EnvironmentBuilderFull)
	 * should implement the TableViewMatrix interface.
	 *
	 * Note that ValueDistributionType.FullySpecified is not an option in the combobox - the user can only specify
	 * a manual distribution by actually specifying that distribution after pushing the 'Set manual distribution'
	 * button.
	 * @param <T>
	 * @param header
	 * @param variableName
	 * @param width
	 * @return
	 */
	private TableColumn<EnvironmentBuilderFull, ValueDistributionType> createValueDistributionTypeColumnFull (String header, String variableName, DoubleBinding width)
	{
		TableColumn<EnvironmentBuilderFull, ValueDistributionType> newCol = new TableColumn<>(header);
		newCol.setCellValueFactory(new PropertyValueFactory<EnvironmentBuilderFull, ValueDistributionType>(variableName));
		newCol.setCellFactory(ComboBoxTableCell.<EnvironmentBuilderFull, ValueDistributionType>forTableColumn((ValueDistributionType.getValuesExcludingManual())));

		newCol.setOnEditCommit(
				new EventHandler<CellEditEvent<EnvironmentBuilderFull, ValueDistributionType>>() {
					@Override
					public void handle(CellEditEvent<EnvironmentBuilderFull, ValueDistributionType> t) {
						TableViewObject origin = t.getTableView().getItems().get(t.getTablePosition().getRow());
						origin.setFieldValue(variableName, t.getNewValue());
						frame.notifyAll(frame.environmentPoolFullySpecified, frame.tabEnvironment);
					}
				}
				);

		newCol.prefWidthProperty().bind(width);
		return newCol;
	}
	
	/**
	 * Create and return a new TableColumn that houses ValueDistributionType 
	 * values in a ComboBox.
	 * This function is designed for the lazy factories. As such, it cannot
	 * be edited.
	 * @param <T>
	 * @param header
	 * @param variableName
	 * @param width
	 * @return
	 */
	private TableColumn<EnvironmentBuilderLazy, ValueDistributionType> createValueDistributionTypeColumnLazy (String header, String variableName, DoubleBinding width)
	{
		TableColumn<EnvironmentBuilderLazy, ValueDistributionType> newCol = new TableColumn<>(header);
		newCol.setCellValueFactory(new PropertyValueFactory<EnvironmentBuilderLazy, ValueDistributionType>(variableName));
		newCol.setCellFactory(ComboBoxTableCell.<EnvironmentBuilderLazy, ValueDistributionType>forTableColumn((ValueDistributionType.getValuesExcludingManual())));
		newCol.prefWidthProperty().bind(width);
		return newCol;
	}


	//////////Environments - Drawing functions
	/**
	 * Draws the Show Environment plot on the Environment tab
	 * @param selectedEnvironmentBuilder
	 * @throws ComputationException 
	 */
	@SuppressWarnings("unchecked")
	private void drawPlotShowEnvironment(EnvironmentBuilderFull selectedEnvironmentBuilder) 
	{
		System.out.println(Helper.timestamp() + "\t Drawing the show environment plot.");

		if (selectedEnvironmentBuilder == null) {
			plotShowEnvironment.setVisible(false);
			return;
		}

		// Set all the axes
		plotShowEnvironmentXAxis.setAutoRanging(false);
		plotShowEnvironmentXAxis.setLowerBound(-frame.mdpBuilder.VALUE_MAXIMUM);
		plotShowEnvironmentXAxis.setUpperBound(frame.mdpBuilder.VALUE_MAXIMUM);
		plotShowEnvironmentXAxis.setTickUnit(frame.mdpBuilder.VALUE_MAXIMUM/4);
		plotShowEnvironmentXAxis.setLabel("Value");

		plotShowEnvironmentYAxis.setAutoRanging(true);
		plotShowEnvironmentYAxis.setLabel(" ");
		plotShowEnvironmentYAxis.lookup(".axis-label")
		.setStyle("-fx-label-padding: -50 0 0 0;");

		plotShowEnvironment.setCreateSymbols(false);

		// Erase the previous data points
		plotShowEnvironment.getData().removeAll(plotShowEnvironment.getData());

		DecimalNumberArray values = selectedEnvironmentBuilder.getMatrix().getColumn(0);
		DecimalNumberArray resourceProbabilities =   selectedEnvironmentBuilder.getMatrix().getColumn(1);
		DecimalNumberArray extrinsicEventProbabilities =  selectedEnvironmentBuilder.getMatrix().getColumn(2);

		// Add the new data point to the plot
		//		Resource values
		Series<Number, Number> resourceValueSeries = new Series<>();
		resourceValueSeries.setName("Resource value");
		for (int i = 0; i < values.length(); i++)
			resourceValueSeries.getData().add(new Data<>(values.get(i), resourceProbabilities.get(i)));

		//	extrinsic events
		Series<Number, Number> extrinsicEventSeries = new Series<>();
		extrinsicEventSeries.setName("Extrinsic event");
		for (int i = 0; i < values.length(); i++)
			extrinsicEventSeries.getData().add(new Data<>(values.get(i), extrinsicEventProbabilities.get(i)));

		// Add both lines to the plot
		plotShowEnvironment.getData().addAll(resourceValueSeries, extrinsicEventSeries);

		// Restyle the Resource quality line. Note that we do not change the legend color here, that is done in the .css file!
		Color fillColor = ColorPalette.addOpacity(ColorPalette.resourceValuesColorFill, ColorPalette.resourceValueColorMaximumOpacity * ((1-selectedEnvironmentBuilder.getInterruptionRate().doubleValue())));
		Color lineColor = ColorPalette.addOpacity(ColorPalette.resourceValuesColorLine, ColorPalette.resourceValueColorMaximumOpacity * ((1-selectedEnvironmentBuilder.getInterruptionRate().doubleValue())));
		resourceValueSeries.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill: " + ColorPalette.toFXMLString(fillColor) + ";");
		resourceValueSeries.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke: "+   ColorPalette.toFXMLString(lineColor) + ";");

		fillColor = ColorPalette.extrinsicEventValueColorFill;
		lineColor = ColorPalette.extrinsicEventValueColorLine;
		extrinsicEventSeries.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill: " + ColorPalette.toFXMLString(fillColor) + ";");
		extrinsicEventSeries.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke: "+   ColorPalette.toFXMLString(lineColor) + ";");

		// Finally, add the legend 
		// The legend does not immediately take over these colors specified above - with the code above
		// 		all the lines in the plot are on a red-blue gradient, but the legend colors do not match.
		// Interestingly enough, the legend is only added to the plot after the full plot has been drawn. This
		// 		means we cannot immediately change the colors in the legend. A somewhat cheesy workaround
		//		is to change them a little later - using Platform's runLater.
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {					redraw();
				} catch (Exception e) {
					new Runnable() { public void run() { try {
						Thread.sleep(250);
						Platform.runLater( new Runnable() { public void run (){try {redraw();} catch(Exception e) {}}});
					}catch(Exception e) {}}}.run();

				}
			}
			private void redraw() throws Exception  {
				try {
					Object[] nodes = plotShowEnvironment.lookupAll(".chart-legend-item-symbol").toArray();
					((Node) nodes[0]).setStyle("-fx-background-color: " +ColorPalette.toFXMLString(ColorPalette.resourceValuesColorLine) + ";");
					((Node) nodes[1]).setStyle("-fx-background-color: " +ColorPalette.toFXMLString(ColorPalette.extrinsicEventValueColorLine) + ";");
				}
				catch (Exception e) {  throw e; }
			}
		});


		// Make sure the plot is visible
		plotShowEnvironment.setVisible(true);

	}



}
