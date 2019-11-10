package decimalNumber;

import java.io.IOException;

import ValueIteratorPolicyPlot.PlottingParameters;
import decimalNumber.DecimalNumber.IllegalRangeException;
import decimalNumber.DecimalNumber.IllegalScaleException;
import defaultAndHelper.ColorPalette;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import staticManagers.ObserverManager;
import window.FrameController;
import window.interfacesAndAbstractions.NotificationManager;

/** An extension of TableView that shows a DecimalNumberMatrix. */
public  class DecimalNumberMatrixTableView extends TableView<DecimalNumberArray>
{
	private DecimalNumberMatrixTableView ownReference = this;
	private  DecimalNumberMatrix matrix;
	private int significantDigits;
	private boolean readOnly;
	private double minimumWidth;
	
	private NotificationManager notificationManager;
	private Object objectDisplayed;	 //After a change, the NotificationManager's notifyAll() will be called to signal this object has changed.
	private Object sourceOfChange; 
	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/////////////////// 	Static functions that can be used outside of a MatrixTableView 	////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * If a class implements this interface it can be used in a TableView View in the main class. The only thing this interface
	 * requires it that the implementing class has a setFieldValue(String fieldName, Object value) and a getFieldValue(String fieldName)
	 * method, which changes and retrieves, respectively, the value of the field with name fieldName.
	 *
	 * In addition there is an optional third method that can be implemented: update().
	 * This method is called any time an update is made to the fields of that object. Sometimes, however, the object needs
	 * to use these fields in other computations (for instance, if a normal distribution's mean changes, the whole
	 * distribution needs to be updated). In this case, the update() function should specify how to do the update to the
	 * normal distribution.
	 *
	 * (This class is a workaround for not having to use Java's Reflection - which I think makes code harder to understand).
	 *
	 */
	public interface TableViewObject {
		public Object getFieldValue(String fieldName);
		public void setFieldValue(String fieldName, Object newValue);
	}
	
	/**
	 * Create a TableColumn design specifically for the use in a DecimalNumberTableView.
	 * if readOnly is true the column can be edited. After a change the 
	 * (should the column be editable? If so, make sure to specify the notificationmanager, objectDisplayed and sourceOfChange as well).
	 * @param matrix
	 * @param columnNumber
	 * @param significantDigits
	 * @param readOnly				
	 * @param notificationManager
	 * @param objectDisplayed
	 * @param sourceOfChange
	 * @return
	 */
	public static TableColumn<DecimalNumberArray, DecimalNumber> createDecimalNumberArrayColumn (
			DecimalNumberMatrix matrix, 
			int columnNumber, 
			int significantDigits, 
			boolean readOnly, 
			NotificationManager notificationManager,
			Object objectDisplayed,
			Object sourceOfChange)
	{
		// Figure out if there is a column name in the matrix. If not, make a generic name
		String name = "Column " + columnNumber;
		if (matrix.getColumnNames() != null)
			name = matrix.getColumnNames()[columnNumber];
		
		// Create the new column
		TableColumn<DecimalNumberArray, DecimalNumber> col = new TableColumn<>(name);

		// check if a custom source of change has been specified
		if (sourceOfChange == null)
			sourceOfChange = col;
		final Object finalSourceOfChange = sourceOfChange;
		
		// Set the CellValueFactory: this factory determines what should be shown.
		// Normally, this is an easy thing to do. However, the DecimalNumber that have to be shown are 
		// stored in a DecimalNumberArray of variable size (the rows).
		// Hence, we have to override the standard CellValueFactory, and tell it to get the n'th item in the array.
		col.setCellValueFactory(
				new Callback<CellDataFeatures<DecimalNumberArray, DecimalNumber>, ObservableValue<DecimalNumber>>(){
					@Override
					public ObservableValue<DecimalNumber> call(CellDataFeatures<DecimalNumberArray, DecimalNumber> t) {
						return new SimpleObjectProperty<>(	t.getValue().get(columnNumber));
					}
				});

		// Tell Java how to go from a DecimalNumber to a string.
		col.setCellFactory(TextFieldTableCell.forTableColumn(new SafeDecimalNumberToStringConverter().setSignificantDigits(significantDigits)));

		// If the table is not read-only, set event handler for changes. This tells Java how to deal with changes made by the user in the 
		// table. For this we use a 
		if (!readOnly){
			col.setOnEditCommit(
					new EventHandler<TableColumn.CellEditEvent<DecimalNumberArray, DecimalNumber>>() {
						@Override
						public void handle(CellEditEvent<DecimalNumberArray, DecimalNumber> t) {
							// Find the row and column that is the origin of t
							int row = t.getTablePosition().getRow();
							int col = t.getTablePosition().getColumn();
							
							// Create the notification parameters so that the Frame can reconstruct which row and column have changed
							Object[] notificationParameters = new Object[2];
							notificationParameters[0] = row;
							notificationParameters[1] = col;
							
							
							// Get the decimal number that is changed
							DecimalNumber dn = matrix.getValueAt(row, col);
							
							// Set the value of the DecimalNumber in the matrix if:
							//		- the decimal number is not isImmutable()
							//		- the new value is within the allowable range of the decimal number (if a range is specified)
							// In all other cases: do nothing
							try {
								if (!dn.isImmutable()) {
									if (dn.hasSpecifiedRange()) {
										if (t.getNewValue().inRange(dn.minimum, dn.maximum)) {
											dn.set(t.getNewValue().getValue());
											notificationManager.notifyAll(objectDisplayed, finalSourceOfChange, notificationParameters);
										}
									} else {
										dn.set(t.getNewValue().getValue());
										notificationManager.notifyAll(objectDisplayed, finalSourceOfChange, notificationParameters);
									}
								}
							} catch (Exception e) {e.printStackTrace();}
							t.getTableView().refresh();
						}});
		}
		return col;
	}
	
	// TODO: comment sourceOfChange is optional
	public static <T extends TableViewObject> TableColumn<T, DecimalNumber> createDecimalNumberColumn (String header,
			String variableName,
			DoubleBinding columnWidth,
			int significantDigits,
			NotificationManager notificationManager,
			Object sourceOfChange)
	{
		TableColumn<T, DecimalNumber> newCol = new TableColumn<>(header);
		// The setCellValueFactory determines what values need to be printed out
		newCol.setCellValueFactory(new PropertyValueFactory<T, DecimalNumber>(variableName));
		newCol.prefWidthProperty().bind(columnWidth);

		// Make sure that there is always a sourceOfChange
		if (sourceOfChange == null)
			sourceOfChange = newCol;
		final Object finalSourceOfChange = sourceOfChange;
		
		// The setCellFactory determines how to render the cell from the cell item.
		newCol.setCellFactory(TextFieldTableCell.forTableColumn(new SafeDecimalNumberToStringConverter().setSignificantDigits(significantDigits)));

		// Add an action listener to make sure that changes in the table respond to changes in the TableViewMatrix.
		// Note that the t.getNewValue() might not be the value that is stored in the TableViewMatrix:
		// the setter of this object might change the input value (e.g., winsorizing to enforce a range of possible values).
		// After an update is processed, the Notifiable will be informed.
		newCol.setOnEditCommit(
				new EventHandler<CellEditEvent<T, DecimalNumber>>() {
					@Override
					public void handle(CellEditEvent<T, DecimalNumber> t) {
						try {
							DecimalNumber cellChanged = (DecimalNumber) t.getRowValue().getFieldValue(variableName);
							if (!cellChanged.isImmutable()) {
								// Find the row and column that is the origin of t
								int row = t.getTablePosition().getRow();
								int col = t.getTablePosition().getColumn();

								// Create the notification parameters so that the Frame can reconstruct which row and column have changed
								Object[] notificationParameters = new Object[] {row, col};

								// If the DecimalNumber has either no specified range, or does have a specified range and the new value is within this range: change the value of the DecimalNumber and inform the NotificationManager
								if (!cellChanged.hasSpecifiedRange() || (cellChanged.hasSpecifiedRange() && t.getNewValue().inRange(cellChanged.minimum, cellChanged.maximum))) {
									cellChanged.set(t.getNewValue().getValue());						
									notificationManager.notifyAll(t.getRowValue(), finalSourceOfChange, notificationParameters);
								}
							}
						} catch (ClassCastException e) {
							ObserverManager.notifyObserversOfError("Error: non-DecimalNumber input in DecimalNumber column", "Trying to modify a non-DecimalNumber object via a change in a DecimalNumberColumn", e);

						} catch (UnsupportedOperationException | IllegalRangeException | IllegalScaleException e) {
							ObserverManager.notifyObserversOfError("Error", "Encountered an exception when updating a DecimalNumberColumn (DecimalNumberJavaFXHelper). See details", e);

						}
						t.getTableColumn().getTableView().refresh();
					}
				}
				);

		return newCol;

	}
	
	/**
	 * Create a read-only table column displaying DecimalNumbers with 
	 * the specified header name, showing the specified variable name 
	 * (remember: javafx wants you to have getVarName() methods for 
	 * each field with name varName).
	 * @param header
	 * @param variableName
	 * @param columnWidth
	 * @param significantDigits
	 * @return
	 */
	public static <T> TableColumn<T, DecimalNumber> createDecimalNumberColumn (String header,
			String variableName,
			DoubleBinding columnWidth,
			int significantDigits)
	{
		TableColumn<T, DecimalNumber> newCol = new TableColumn<>(header);
		// The setCellValueFactory determines what values need to be printed out
		newCol.setCellValueFactory(new PropertyValueFactory<T, DecimalNumber>(variableName));
		newCol.prefWidthProperty().bind(columnWidth);

		// The setCellFactory determines how to render the cell from the cell item.
		newCol.setCellFactory(TextFieldTableCell.forTableColumn(new SafeDecimalNumberToStringConverter().setSignificantDigits(significantDigits)));
		return newCol;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////// 	Construction  & initialization	/////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public DecimalNumberMatrixTableView() {
		super();
		this.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
	}
	/**
	 * Initialize the table view to show the specified matrix. 
	 * 
	 * After each update the specified frame's notifiyAll() with the notificationObject as the Object
	 * parameter and the row name (or index number if no row names are specified) and the column name (or
	 * index if no column names are specified) as the args parameters (in that order). 
	 * @param matrix:					matrix to show
	 * @param significantDigits			number of decimal number to show
	 * @param readOnly					can the user make changes to the matrix?
	 * @param minimumWidth				minimum width of the columns
	 * @param objectDisplayed	After a change, the NotificationManager's notifyAll() will be called to signal this object has changed.
	 * @param sourceOfChange	After a change, the NotificationManager's notifyAll() will be called to signal this object has made a change to the objectDisplayed.
	 */
	public void initializeTableView (DecimalNumberMatrix matrix, int significantDigits, boolean readOnly,double minimumWidth, NotificationManager notificationManager, Object objectDisplayed, Object sourceOfChange) {
		this.matrix = matrix;
		this.significantDigits = significantDigits;
		this.readOnly = readOnly;
		this.minimumWidth = minimumWidth;
		this.notificationManager = notificationManager;
		this.objectDisplayed = objectDisplayed;
		this.sourceOfChange = sourceOfChange;
		
		this.getColumns().removeAll(this.getColumns());

		// Set all the columns in the matrix
		this.createAllColumns();

		// Add the matrix to the tableView
		this.getItems().addAll(matrix.rowMatrix());

		// Set the readability
		this.setEditable(!readOnly);

		//Update the view
		refresh();
	}

	private void createAllColumns() {
		// if row names are specified, create a column for row names:
		if (getMatrix().getRowNames() != null) 
			this.getColumns().add(createDecimalNumberArrayRowNameColumn());

		// create a TableColumn for each column in the matrix
		for (int c = 0; c< getMatrix().ncol(); c++)
			this.getColumns().add(createDecimalNumberArrayColumn(matrix, 
					c, 
					significantDigits, 
					readOnly, 
					notificationManager,
					objectDisplayed,
					sourceOfChange
					));
		
		this.resizeColumns();
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////// 	Public functions to manipulation the MatrixTableView 	/////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
 	@Override public void refresh() {
		this.getItems().removeAll(this.getItems());
		this.getItems().addAll(getMatrix().rowMatrix());
		super.refresh();
	}

	public void setEditable(int columnNumber, boolean value) {
		getColumns().get(columnNumber).setEditable(value);
	}
	
	/** Creates a TableColumn for the row names of the matrix. It is assumed that rownames are specified. */
	private TableColumn<DecimalNumberArray, String> createDecimalNumberArrayRowNameColumn ()
	{
		// Create the new column
		TableColumn<DecimalNumberArray, String> col = new TableColumn<>();

		// Set the CellValueFactory: this factory determines what should be shown.
		// In this case: find the row name of the array in the matrix
		col.setCellValueFactory(
				new Callback<CellDataFeatures<DecimalNumberArray, String>, ObservableValue<String>>(){
					@Override
					public ObservableValue<String> call(CellDataFeatures<DecimalNumberArray, String> t) {
						int index = getMatrix().getIndexOfDecimalNumberArrayRow(t.getValue());
						if (index != -1)
							return new SimpleStringProperty(getMatrix().getRowNames()[index]);
						else
							return new SimpleStringProperty("NO ROW NAME FOUND");
					}
				});

		// Tell Java how to go from a DecimalNumber to a string.
		col.setCellFactory(TextFieldTableCell.forTableColumn());

		// If the table is not read-only, set event handler for changes. This tells Java how to deal with changes made by the user in the 
		// table. For this we use a 
		if (!readOnly){
			col.setOnEditCommit(
					new EventHandler<TableColumn.CellEditEvent<DecimalNumberArray, String>>() {
						@Override
						public void handle(CellEditEvent<DecimalNumberArray, String> t) {
							// Find the row that is the origin of t
							int row = t.getTablePosition().getRow();
							if (row >-1)
								getMatrix().setRowName(row, t.getNewValue());
						}
					});
		}
		return col;
	}

	/** Removes the row at index position and updates the tableview. Returns false is the index is an invalid number*/
	public boolean removeRow(int index) {
		if (index < 0 || index > getMatrix().nrow()-1)
			return false;
		this.getMatrix().removeRow(index);
		refresh();
		return true;
	}

	/** Removes the selected rows (if any). */
	public void removeSelectedRows() { 
		for (int index: this.getSelectionModel().getSelectedIndices())
			this.removeRow(index) ;
		}
	
	/** Inserts the specified row at index position.  If the index position is not in the range [0,nrow()], the new row is inserted at position 0.	 */
	public void insertRow(int index, DecimalNumberArray array) {
		if (index == -1)
			this.getMatrix().insertRow(0, array);
		else
			this.getMatrix().insertRow(index, array);
		this.refresh();
		this.getSelectionModel().select(index);
	}
	
	/** Inserts the row after the selected row. If no row is selected, the row is inserted at index position 0 */
	public void insertRowAtSelectedRow(DecimalNumberArray array) {
		insertRow(this.getSelectionModel().getSelectedIndex(), array);
	}
	
	/** Removes a column from the table view and underlying matrix. Returns false if the index is not a valid position */
	public boolean removeColumn(int index) {
		if (index < 0 || index>= getMatrix().ncol())
			return false;
		this.getMatrix().removeColumn(index);
		this.getColumns().remove(index);
		this.refresh();
		this.resizeColumns();
		return true;
	}
	
	/** Adds the column from the table view and underlying matrix. Returns false if the index is not a valid position, or the column has a number of entries unequal to the number of rows in the matrix */
	public boolean insertColumn(int index, DecimalNumberArray columnVector, String newColumnName) {
		getMatrix().insertColumn(index, columnVector, newColumnName);
		this.getColumns().removeAll(this.getColumns());
		this.createAllColumns();
		this.refresh();
		return true;
	}
	
	/** Set a column to the specified column. Returns false if the index is invalid, the column vector length is not equal to the number of rows in the matrix, or if the matrix is immutable */
	public boolean setColumn(int index, DecimalNumberArray columnVector) {
		getMatrix().setColumn(index, columnVector);
		refresh();
		return true;
	}

	/** Set the name of the specified column by index.  If the column does not exist or does not match the number of columns, a false is returned. */
	public boolean setColumnName(int index, String newName) {
		this.getMatrix().setColumnName(index, newName) ;
		this.getColumns().get(index).setText(newName);
		return true;

	}

	/** Replaces an old column name with the new name. Returns false if the old name does not exist prior to the changing.*/
	public boolean setColumnName(String oldName, String newName){
		int index = getMatrix().getIndexOfColumn(oldName);
		if (index < 0) return false;

		return this.setColumnName(index, newName);
	}
	public DecimalNumberMatrix getMatrix() {
		return matrix;
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////// 	Private functions only used by the MatrixTableView 	/////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private void resizeColumns() {
		// First, figure out what the width of all columns would be if we space the columns equally across the table
		DoubleBinding widthBinding = this.widthProperty().subtract(1).divide((double) this.getColumns().size());
		
		// if this width is higher than or equal to the minimumWidth: space the columns. Else, use the minimumWidth
		if (widthBinding.doubleValue() >= minimumWidth)
			for (TableColumn<DecimalNumberArray, ?> tc: this.getColumns())
				tc.prefWidthProperty().bind(widthBinding);
		else
			for (TableColumn<DecimalNumberArray, ?> tc: this.getColumns())
				tc.setMinWidth(minimumWidth);
		

	}
	

	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////// 	Popup window 	/////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	public static class DecimalNumberMatrixPopup implements EventHandler<MouseEvent>
	{
		/*private static Color AREA_FILL = Color.WHITE;
		private static Color AREA_LINE = Color.WHITE;*/
		public static Color 	AREA_FILL = Color.rgb(0,0,0, 0.2);
		public static Color		AREA_LINE = Color.rgb(0,0,0, 1);
		private final DecimalNumberMatrix 					matrix;

		@FXML public AnchorPane 							anchorPaneMainPane;
		@FXML public BorderPane								buttonPane;
		@FXML public Button									buttonClose;
		@FXML public Accordion						    	accordion;
		@FXML public TitledPane								titledPaneData;
		@FXML public AnchorPane								anchorPaneData;
		@FXML public TitledPane								titledPanePlot;
		@FXML public AnchorPane								anchorPanePlot;
		@FXML public DecimalNumberMatrixTableView			tableView;
		@FXML public AreaChart<DecimalNumber, DecimalNumber>areaChart;
		@FXML public NumberAxis								areaChartX;
		@FXML public NumberAxis								areaChartY;
		@FXML public ComboBox<String>						comboBoxX;
		@FXML public ComboBox<String> 						comboBoxY;
		public Stage stage;
		public Scene scene;
		/**
		 * @param frame			The main window
		 * @param matrix		The matrix to show
		 * @param height		The height of the 
		 * @param width
		 * @param significantDigits
		 * @param readOnly
		 */
		public DecimalNumberMatrixPopup(FrameController frame, DecimalNumberMatrix matrix, double width, double height, int significantDigits, boolean readOnly)
		{
			this.matrix = matrix;

			// Retrieve the anchorPane from the FXML file
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(getClass().getResource("DecimalNumberMatrixPopup.fxml"));
			loader.setController(this);
			Parent root = null;
			try {root = loader.load();} catch (IOException e) {e.printStackTrace();}
			
			
			scene = new Scene(root, width, height);
			stage = new Stage();
			stage.setTitle("");
			stage.initStyle(StageStyle.TRANSPARENT);
			scene.setFill(Color.TRANSPARENT);
			stage.setScene(scene);
			stage.show();
			
			// Make the scene movable
			AnchorPane[] anchorPanesRequiringMouseListener = new AnchorPane[] {anchorPanePlot, anchorPaneData, anchorPaneMainPane};
			for (AnchorPane a: anchorPanesRequiringMouseListener) {
				a.setOnMousePressed(this);
				a.setOnMouseDragged(this);
			}
			accordion.setOnMousePressed(this);
			accordion.setOnMouseDragged(this);
			
			// Initialize the table view
			tableView.initializeTableView(matrix, significantDigits, readOnly, 100, frame, matrix, this);
		
			// Set the button
			buttonClose.setOnAction(new EventHandler<ActionEvent>(){@Override public void handle(ActionEvent event) {	stage.close();}});

			// Set the items in the comboBoxes
			ObservableList<String> columnNames = FXCollections.observableArrayList();
			for (TableColumn<DecimalNumberArray, ?> columnInTable: tableView.getColumns())
				columnNames.add(columnInTable.getText());
			comboBoxX.setItems(columnNames);
			comboBoxY.setItems(columnNames);

			// Set the handler and default selections on the combo boxes
			comboBoxX.setItems(columnNames);
			comboBoxX.getSelectionModel().select(0);
			comboBoxX.valueProperty().addListener(new ChangeListener<String>(){ public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {drawPlot();}});

			comboBoxY.setItems(columnNames);
			if (columnNames.size() >= 2) 	comboBoxY.getSelectionModel().select(1);
			else						 	comboBoxY.getSelectionModel().select(0);
			comboBoxY.valueProperty().addListener(new ChangeListener<String>(){ public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {drawPlot();}});
			
			
			drawPlot();
			accordion.setExpandedPane(titledPaneData);

		}

		

		/**
		 * Draw the plot, used when the comboboxes change.
		 * @param x
		 * @param y
		 */
		private void drawPlot()
		{
			areaChartX.setLabel(comboBoxX.getValue());
			areaChartY.setLabel(comboBoxY.getValue());

			areaChart.getData().removeAll(areaChart.getData());
			// If the column names are specified in the matrix, we can get the columns that corresponds to the names selected by the comboboxed
			// If the column names are not specified, we are using generic value. In this case, we can get the columns by the index of the item selected in the combobox.
			// Since the latter approach works for the first case as well, we can just get columns by index.
			DecimalNumberArray dataX = matrix.getColumn(comboBoxX.getSelectionModel().selectedIndexProperty().get());
			DecimalNumberArray dataY = matrix.getColumn(comboBoxY.getSelectionModel().selectedIndexProperty().get());

			Series<DecimalNumber, DecimalNumber> series = new Series<>();
			for (int i = 0; i < matrix.nrow(); i ++)
				series.getData().add(new Data<DecimalNumber, DecimalNumber>(dataX.get(i), dataY.get(i)));
			areaChart.getData().add(series);

			series.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill:" + ColorPalette.toFXMLString(AREA_FILL)+ ";");
			series.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke:" + ColorPalette.toFXMLString(AREA_LINE)+ ";"
					+"-fx-stroke-width: 3px;");
			for (Data<DecimalNumber, DecimalNumber> dataPoint : series.getData())
				dataPoint.getNode().lookup(".chart-area-symbol").setStyle("-fx-background-color: rgba(0,0,0,0), rgba(0,0,0,0)"	 );

			areaChart.setLegendVisible(false);

		}
		
		
		double offsetX;
		double offsetY;

		@Override
		public void handle(MouseEvent event) {
			// If the mouse is pressed, record where the mouse is
			if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
				offsetX = scene.getWindow().getX() - event.getScreenX();
				offsetY = scene.getWindow().getY() - event.getScreenY();
			}
			
			if (event.getEventType() == MouseEvent.MOUSE_DRAGGED){
				stage.setX(event.getScreenX() + offsetX);
				stage.setY(event.getScreenY() + offsetY);
			}
		}
		


	}
}