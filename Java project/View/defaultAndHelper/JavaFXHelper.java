package defaultAndHelper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import decimalNumber.DecimalNumberMatrix;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class JavaFXHelper {


	public static final String defaultTextFieldStyle = "";
	public static final String invalidTextFieldValueStyle = "-fx-text-box-border: rgb(255,0,0)";

	public static void showErrorAlert(String title, String message, Stage owner)
	{
		showErrorAlert(title, message, null, owner);
	}

	public static void showErrorAlert(String title, String message, String details, Stage owner)
	{
		Alert alert = new Alert(AlertType.ERROR);
		alert.setTitle("");
		alert.setHeaderText(title);

		// Add the message: making sure that the user can select the text 
		TextArea messageArea = new TextArea(message);
		messageArea.setEditable(false);
		messageArea.setWrapText(true);
		
		messageArea.getStylesheets().add(JavaFXHelper.class.getResource("../CSSLayout/normalTextArea.css").toExternalForm());
		messageArea.setStyle("-fx-focus-color: transparent; -fx-text-box-border: transparent; -fx-border-color: transparent;");
		
		GridPane gridPane = new GridPane();
		gridPane.setMaxWidth(Double.MAX_VALUE);
		gridPane.setMaxHeight(Double.MAX_VALUE);
		gridPane.add(messageArea, 0, 0);
		alert.getDialogPane().setContent(gridPane);
		
		// Set expandable context  (if applicable)
		if (details != null) {
			TextArea textArea = new TextArea(details);
			textArea.setEditable(false);
			textArea.setWrapText(false);
			textArea.setFont(Font.font("Consolas", 12));

			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);

			GridPane detailPane = new GridPane();
			detailPane.setMaxWidth(Double.MAX_VALUE);
			detailPane.add(textArea, 0, 1);
			alert.getDialogPane().setExpandableContent(detailPane);

			alert.getDialogPane().setMinWidth(500);
		}

		// Set the style
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets().add(JavaFXHelper.class.getResource("../CSSLayout/normalDialog.css").toExternalForm());
		dialogPane.lookup(".dialog-pane:header *.header-panel *.label").setStyle(
				"-fx-text-fill: rgba(190,49,26,1);"
				);
		alert.initStyle(StageStyle.UTILITY);
		alert.setGraphic(null);

		alert.initOwner(owner);
		alert.showAndWait();
	}

	public static void showWarningAlert(String title, String message, Stage owner)
	{
		showWarningAlert(title, message, null, owner);
	}

	public static void showWarningAlert(String title, String message, String details, Stage owner)
	{
		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("");
		alert.setHeaderText("Warning: "+ title);
		alert.setContentText(message);
		
		// Set expandable context  (if applicable)
		if (details != null) {
			TextArea textArea = new TextArea(details);
			textArea.setEditable(false);
			textArea.setWrapText(false);
			textArea.setFont(Font.font("Consolas", 12));

			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);

			GridPane detailPane = new GridPane();
			detailPane.setMaxWidth(Double.MAX_VALUE);
			detailPane.add(textArea, 0, 1);
			alert.getDialogPane().setExpandableContent(detailPane);

			alert.getDialogPane().setMinWidth(500);
		}

		// Set the style
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets().add(JavaFXHelper.class.getResource("../CSSLayout/normalDialog.css").toExternalForm());
		dialogPane.lookup(".dialog-pane:header *.header-panel *.label").setStyle(
				"-fx-text-fill: rgba(190,49,26,1);"
				);
		alert.initStyle(StageStyle.UTILITY);
		alert.setGraphic(null);

		alert.initOwner(owner);
		alert.showAndWait();
	}

	public static boolean showConfirmationYesNoAlert(String title, String message, Stage owner,String textForYesButton, String textForNoButton, String details)
	{
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("");
		alert.setHeaderText(title);
		alert.setContentText(message);
		alert.initOwner(owner);
		
		// Set the buttons
		ButtonType yesButton = new ButtonType(textForYesButton, ButtonData.OK_DONE);
		ButtonType noButton = new ButtonType(textForNoButton, ButtonData.CANCEL_CLOSE);
		alert.getButtonTypes().removeAll(alert.getButtonTypes());
		alert.getButtonTypes().addAll(	yesButton, noButton);

		// Set expandable context  (if applicable)
		if (details != null) {
			TextArea textArea = new TextArea(details);
			textArea.setEditable(false);
			textArea.setWrapText(false);
			textArea.setFont(Font.font("Consolas", 12));
			
			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			GridPane.setVgrow(textArea, Priority.ALWAYS);
			GridPane.setHgrow(textArea, Priority.ALWAYS);

			GridPane detailPane = new GridPane();
			detailPane.setMaxWidth(Double.MAX_VALUE);
			detailPane.add(textArea, 0, 1);
			alert.getDialogPane().setExpandableContent(detailPane);
			
			alert.getDialogPane().setMinWidth(500);
		}

		// Set the style
		DialogPane dialogPane = alert.getDialogPane();
		dialogPane.getStylesheets().add(JavaFXHelper.class.getResource("../CSSLayout/normalDialog.css").toExternalForm());
		alert.initStyle(StageStyle.UTILITY);
		alert.setGraphic(null);
		
		Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == yesButton) return true;
		return false;
	}




	/**
	 * Format nanoseconds to [d]d [h]h [m]m [s]s, where [] denotes variables. For instance: 5d 10h 20m 30s
	 * @param nanoSeconds
	 * @return
	 */
	public static String formatNanoSeconds(long nanoSeconds, boolean includeMilis)
	{
		
		long seconds = TimeUnit.NANOSECONDS.toSeconds(nanoSeconds);
		long s = seconds % 60;
		long m = (seconds /60) % 60;
		long h = ((seconds/60)/60) %24;
		long d = (((seconds/60)/60)/24);

		if (! includeMilis)
			return d + "d " + h+ "h " + m +  "m " + s + "s";
		
		long ms = TimeUnit.NANOSECONDS.toMillis(nanoSeconds) % 1000;
		return d + "d " + h+ "h " + m +  "m " + s + "s " + ms +"ms";
	}


	/**
	 * A class that creates an Android-style toast. That is,
	 * is shows a small bar at the bottom of the screen with a
	 * simple text message.
	 */
	public static class Toaster {
		private final double fadeInTime = 1000;
		private final double visibleTime = 2000;
		private final double fadeOutTime = 2000;

		private final AnchorPane root;
		private Toast toast;
		public Toaster (AnchorPane root){
			this.root = root;
			this.toast = new Toast(root);
		}

		public void makeToast(String message)
		{
			toast.setStyle(Style.normal);
			toast.showText(message);
		}
		
		public void makeWarningToast(String message) {
			toast.setStyle(Style.warning);
			toast.showText(message);
		}
		
		public enum Style {normal, warning}
		
		private class Toast implements EventHandler<ActionEvent>
		{
			private final	AnchorPane 	root;
			private final	Label 		lbl;
			private final	Timeline 	animationTimeLine ;
			private 		boolean 	isVisible;

			public Toast(AnchorPane root)
			{
				this.root = root;
				lbl = new Label();
				lbl.setFont(Font.font("Cambria", FontPosture.REGULAR, 12));
				lbl.setTextFill(Color.WHITE);
				lbl.setWrapText(true);
				
				lbl.setOpacity(0);
				// Make sure that the message does not disappear if the mouse hovers over the label
				lbl.setOnMouseMoved(new EventHandler<MouseEvent>() {

					@Override
					public void handle(MouseEvent event) {
						if (isVisible && lbl.hoverProperty().get())
							animationTimeLine.jumpTo(Duration.millis(fadeInTime));
					}
					
				});
				animationTimeLine = new Timeline();
				KeyValue visible = new KeyValue(lbl.opacityProperty(), 1);
				KeyValue invisible = new KeyValue(lbl.opacityProperty(), 0);

				KeyFrame fadingIn = new KeyFrame(Duration.millis(fadeInTime), visible);
				KeyFrame remainingVisible = new KeyFrame(Duration.millis(fadeInTime + visibleTime), "remainingVisible", visible);
				KeyFrame fadingOut = new KeyFrame(Duration.millis(fadeInTime + visibleTime + fadeOutTime), this, invisible);

				animationTimeLine.getKeyFrames().addAll(fadingIn, remainingVisible, fadingOut);
				isVisible = false;
			}
			
			public void setStyle(Style style) {
				if (style == Style.normal)
					lbl.setStyle("-fx-background-radius: 20; "
						+ "-fx-background-color: rgba(0, 0, 65, 0.7); "
						+ "-fx-padding: 10px;"
						+ "-fx-text-wrap: true;"
						+ "-fx-alignment: center;"
						+ "-fx-border-color:white;"
						+ "-fx-border-radius: 20;"
						+ "-fx-border-width: 3");
				else if (style == Style.warning) {
					lbl.setStyle("-fx-background-radius: 20; "
							+ "-fx-background-color: rgba(150, 0, 0, 0.9); "
							+ "-fx-padding: 10px;"
							+ "-fx-text-wrap: true;"
							+ "-fx-alignment: center;"
							+ "-fx-border-color:white;"
							+ "-fx-border-radius: 20;"
							+ "-fx-border-width: 3");
				}
			}

			public void showText(String newText)
			{
				if (!isVisible)	{
					lbl.setText(newText);

					root.getChildren().add(lbl);
					AnchorPane.setLeftAnchor(lbl,root.getWidth()*0.05);
					AnchorPane.setRightAnchor(lbl, root.getWidth()*0.05);
					AnchorPane.setBottomAnchor(lbl, root.getHeight()*0.075);
					animationTimeLine.play();
					isVisible = true;
				} else	{
					lbl.setText(newText);
					animationTimeLine.jumpTo(Duration.millis(fadeInTime));
				}
			}

			@Override
			public void handle(ActionEvent event) {
				root.getChildren().remove(lbl);
				isVisible = false;
			}
		}

	}

}
