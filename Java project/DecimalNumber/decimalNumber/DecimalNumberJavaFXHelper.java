package decimalNumber;

import java.io.IOException;

import defaultAndHelper.ColorPalette;
import javafx.beans.binding.DoubleBinding;
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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
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
import staticManagers.ObserverManager;

/**
 * @deprecated
 * @author jesse
 *
 */
public class DecimalNumberJavaFXHelper {
	public static Color 	AREA_FILL = Color.rgb(176,196,222, 0.25);
	public static Color		AREA_LINE = Color.rgb(70,130,180, 0.9);
	
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
	
	public static abstract class DecimalNumberMatrixPopup implements EventHandler<MouseEvent>
	{
	//	private final DecimalNumberMatrix 					matrix;

		@FXML public Label									labelTitle;
		@FXML public AnchorPane 							anchorPaneMainPane;
		@FXML public BorderPane								buttonPane;
		@FXML public Button									buttonClose;
		@FXML public Accordion						    	accordion;
		@FXML public TitledPane								titledPaneData;
		@FXML public TitledPane								titledPanePlot;
		@FXML public TableView<DecimalNumberArray>			tableView;
		@FXML public AreaChart<DecimalNumber, DecimalNumber>areaChart;
		@FXML public NumberAxis								areaChartX;
		@FXML public NumberAxis								areaChartY;
		@FXML public ComboBox<String>						comboBoxX;
		@FXML public ComboBox<String> 						comboBoxY;

		public double x;
		public double y;
		double startX;
		double xPressedOnScene;
		double startY;
		double yPressedOnScene;

//		public DecimalNumberMatrixPopup(DecimalNumberMatrix matrix, String title, double height, double width, int significantDigits, boolean readOnly)
//		{
//			this.matrix = matrix;
//
//		
//			// Retrieve the anchorPane from the FXML file and start the stage
//			FXMLLoader loader = new FXMLLoader();
//			loader.setLocation(getClass().getResource("DecimalNumberMatrixPopup.fxml"));
//			loader.setController(this);
//			Parent root;
//			try {root = loader.load();} catch (IOException e) {ObserverManager.notifyObserversOfError(e);}
//
//			Scene scene = new Scene(root);
//			Stage stage = new Stage();
//			stage.setScene(scene);
//
//
//			// Initialize all nodes in the scene
//			startNodes();
//			
//			// If the main window stage closes, all opened substages should likewise close
//			stage.setOnCloseRequest(e -> System.exit(0));
//			stage.show();
//			
//			
//			// Set the preferred size
//			anchorPaneMainPane.setPrefSize(width, height);
//
//			// Set the header title
//			this.labelTitle.setText(title);
//
//			// Set the button
//			buttonClose.setOnAction(new EventHandler<ActionEvent>(){@Override public void handle(ActionEvent event) {	close();}});
//
//			// Create the table to show the results in
//			createDecimalNumberMatrixTableView ( tableView, matrix, significantDigits, readOnly);
//
//			// Set the items in the comboBoxes
//			ObservableList<String> columnNames = FXCollections.observableArrayList();
//			for (TableColumn<DecimalNumberArray, ?> columnInTable: tableView.getColumns())
//				columnNames.add(columnInTable.getText());
//			comboBoxX.setItems(columnNames);
//			comboBoxY.setItems(columnNames);
//
//			// Set the handler and default selections on the combo boxes
//			comboBoxX.setItems(columnNames);
//			comboBoxX.getSelectionModel().select(0);
//			comboBoxX.valueProperty().addListener(new ChangeListener<String>(){ public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {drawPlot();}});
//
//			comboBoxY.setItems(columnNames);
//			if (columnNames.size() >= 2) 	comboBoxY.getSelectionModel().select(1);
//			else						 	comboBoxY.getSelectionModel().select(0);
//			comboBoxY.valueProperty().addListener(new ChangeListener<String>(){ public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {drawPlot();}});
//			drawPlot();
//
//			// Set the mouse listeners for movement and closing
//			buttonPane.setOnMouseClicked(this);
//			buttonPane.setOnMousePressed(this);
//			buttonPane.setOnMouseDragged(this);
//			accordion.setOnMouseClicked(this);
//			accordion.setOnMousePressed(this);
//			accordion.setOnMouseDragged(this);
//			labelTitle.setOnMouseClicked(this);
//			labelTitle.setOnMousePressed(this);
//			labelTitle.setOnMouseDragged(this);
//
//			// draw the whole thing
//			AnchorPane.setLeftAnchor(anchorPaneMainPane, x);
//			AnchorPane.setTopAnchor(anchorPaneMainPane, y);
//			draw();
//		}

//		@Override
//		public void handle(MouseEvent event) {
//			// If the right mouse is clicked, close the screen
//			if (event.getButton() == MouseButton.SECONDARY)
//				this.close();
//
//			// If the left mouse button is pressed, record where the mouse is
//			if (event.getEventType() == MouseEvent.MOUSE_PRESSED &&  event.getButton() == MouseButton.PRIMARY) {
//				startX			    = x;
//				xPressedOnScene 	= event.getSceneX();
//				startY			    = y;
//				yPressedOnScene 	= event.getSceneY();
//			}
//
//			// If the mouse is dragged (left button): move the anchorPaneMainPane
//			if (event.getEventType() == MouseEvent.MOUSE_DRAGGED){
//				x = startX + (event.getSceneX()-xPressedOnScene);
//				y = startY + (event.getSceneY()-yPressedOnScene);
//				AnchorPane.setLeftAnchor(anchorPaneMainPane, x);
//				AnchorPane.setTopAnchor(anchorPaneMainPane, y);
//			}
//
//		}
//
//		/**
//		 * Draw the plot, used when the comboboxes change.
//		 * @param x
//		 * @param y
//		 */
//		private void drawPlot()
//		{
//			areaChartX.setLabel(comboBoxX.getValue());
//			areaChartY.setLabel(comboBoxY.getValue());
//
//			areaChart.getData().removeAll(areaChart.getData());
//			// If the column names are specified in the matrix, we canget the columns that corresponds to the names selected by the comboboxed
//			// If the column names are not specified, we are using generic value. In this case, we can get the columns by the index of the item selected in the combobox.
//			// Since the latter approach works for the first case as well, we can just get columns by index.
//			DecimalNumberArray dataX = matrix.getColumn(comboBoxX.getSelectionModel().selectedIndexProperty().get());
//			DecimalNumberArray dataY = matrix.getColumn(comboBoxY.getSelectionModel().selectedIndexProperty().get());
//
//			Series<DecimalNumber, DecimalNumber> series = new Series<>();
//			for (int i = 0; i < matrix.nrow(); i ++)
//				series.getData().add(new Data<DecimalNumber, DecimalNumber>(dataX.get(i), dataY.get(i)));
//			areaChart.getData().add(series);
//
//			series.getNode().lookup(".chart-series-area-fill").setStyle("-fx-fill:" + ColorPalette.toFXMLString(AREA_FILL)+ ";");
//			series.getNode().lookup(".chart-series-area-line").setStyle("-fx-stroke:" + ColorPalette.toFXMLString(AREA_LINE)+ ";"
//					+"-fx-stroke-width: 3px;");
//			for (Data<DecimalNumber, DecimalNumber> dataPoint : series.getData())
//				dataPoint.getNode().lookup(".chart-area-symbol").setStyle("-fx-background-color: rgba(0,0,0,0), rgba(0,0,0,0)"	 );
//
//			areaChart.setLegendVisible(false);
//
//		}

//		public void draw(){
//			accordion.setExpandedPane(titledPaneData);
//			// Add the anchor pane to the parent
//			root.getChildren().add(anchorPaneMainPane);
//			AnchorPane.setLeftAnchor(anchorPaneMainPane, x);
//			AnchorPane.setTopAnchor(anchorPaneMainPane, y);
//		}

//		public void close(){
//			root.getChildren().remove(anchorPaneMainPane);
//		}
	}

	
}
