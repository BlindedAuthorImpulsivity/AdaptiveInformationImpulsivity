package staticManagers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import helper.Helper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.stage.Stage;

/** Because I hate the fact that Observable has to be extended, not implemented:
 * this is a class that sends messages to the (JavaFX) observers. Hence, it is the control point
 * to all the views. 
 * 
 * Everything in this class should be static - since there can only one ObserverManager, 
 * we need not bother with instantiations.
 * 
 * Messages (but not toast) will be send to the stage that has most recently received focus.
 */
public class ObserverManager {
	private static final 	ArrayList<ObserverStage> observers = new ArrayList<>();
	private static 			ObserverStage focus;

	/**
	 * Registers the ObserverStage as a potential observer. Only observer can receive and display
	 * error/warning/confirmation/toast messages. Hence, all large stages should register themselves
	 * @param observer
	 */
	public static void registerObserver(ObserverStage observer) {
		observers.add(observer);
		focus = observer;
		// If the stage is gained focus, set it as the focussedWindow
		observer.getStage().focusedProperty().addListener(new ChangeListener<Boolean> () { 
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (newValue) 
					focus = observer;
			}

		});
	}

	public static void removeObserver(ObserverStage observer) {observers.remove(observer);}

	/**
	 * Returns the stage that has the input 
	 * @return
	 */
	private static ObserverStage getFocussedStage() {
		for (ObserverStage observer : observers)
			if (observer.getStage().isFocused())
				return observer;
		return observers.get(observers.size()-1);
	}

	public static void notifyObserversOfError(String title, String message) {
		System.err.println("- ERROR: ObserverManager was notified of an error with title \"" + title + "\" and message \"" + message + "\".");
		if (focus != null)
			Platform.runLater(new Runnable() {
				public void run() {
					focus.showErrorMessage(title, message, null);
				}
			});
	}


	public static void notifyObserversOfError(String title, String message, Exception e) {
		System.err.println("- ERROR: ObserverManager was notified of the following exception: \n ");
		e.printStackTrace();
		if (focus != null)
			Platform.runLater(new Runnable() {
				public void run() {
					focus.showErrorMessage(title, message + ".\n\nSee details for the exception stack trace.",  e.getMessage() + "\n\n"+Helper.repString("-", 50)+"\nStack trace: \n"+Helper.repString("-", 50)+"\n" +Arrays.toString(e.getStackTrace()).replaceAll(", ", "\n"));
				}
			});
	}

	public static void notifyObserversOfError(Exception e) {
		notifyObserversOfError("Exception encountered", "Java has encountered the following exception:\n\n" + e.getClass().getSimpleName() + ": " + e.getLocalizedMessage(), e);

	}


	public static void warnObservers(String title, String message)
	{
		System.err.println("- WARNING: ObserverManager was notified of a warning with title \"" + title + "\" and message \"" + message + "\".");
		if (focus != null)
			Platform.runLater(new Runnable() {
				public void run() {
					focus.showWarningMessage(title, message, null);
				}
			});

	}

	public static void warnObservers(String title, String message, String details)
	{
		System.err.println("- WARNING: ObserverManager was notified of a warning with title \"" + title + "\" and message \"" + message + "\" and the following details: \n ");
		System.err.println(details);
		if (focus != null)
			Platform.runLater(new Runnable() {
				public void run() {
					focus.showWarningMessage(title, message, details);
				}
			});
	}

	public static void makeToast(String message)
	{
		System.out.println(Helper.timestamp() + " TOASTING: \"" + message + "\"");
		if (focus != null)
			Platform.runLater(new Runnable() {
				public void run() {
					focus.showToast(message);
				}
			});
	}

	public static void makeWarningToast(String message)
	{
		System.out.println(Helper.timestamp() + " WARNING: TOASTING (warning): \"" + message + "\"");
		if (focus != null)
			Platform.runLater(new Runnable() {
				public void run() {
					focus.showWarningToast(message);
				}
			});
	}

	/**
	 * Prompts the user with a question, and two answer options: a yes button and a no button. 
	 * In case of an exception a false is returned and an error message is shown.
	 * @param title
	 * @param message
	 * @param textForYesButton
	 * @param textForNoButton
	 * @return
	 */
	public static boolean showConfirmationMessage(String title, String message, String textForYesButton, String textForNoButton, String details) {
		System.out.println(Helper.timestamp() + " QUERY: ObserverManager was notified of the following query: HEADER: " + title + ".\tMESSAGE: " + message + ". (Y=" + textForYesButton + ",N=" + textForNoButton + "). \t DETAILS: " + details);
		if (focus != null) {
			
			// If we are on the FX application thread we SHOULD NOT USE RUNLATER - this will result in a deadlock.
			if (Platform.isFxApplicationThread())
				return focus.confirmationMessage(title, message, textForYesButton, textForNoButton,details);
			else {

			// If we are not on the FX thread, we have to use runlater.
			FutureTask<Boolean> ft =  new FutureTask<Boolean>(new Callable<Boolean>() {

				@Override
				public Boolean call() throws Exception {
					return focus.confirmationMessage(title, message, textForYesButton, textForNoButton, details);
				}});
			Platform.runLater(ft);
			try {
				return ft.get();
			} catch (InterruptedException | ExecutionException e) {
				notifyObserversOfError(e);
			}
		}
		}
		return false;
	}

	public interface ObserverStage {
		public Stage getStage();
		public void showErrorMessage(String title, String message, String details);
		public void showWarningMessage(String title, String message, String details);
		public boolean confirmationMessage(String title, String message, String textForYesButton, String textForNoButton, String details);
		public void showToast(String message);
		public void showWarningToast(String message);
	}


}
