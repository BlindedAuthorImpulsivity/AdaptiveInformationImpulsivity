package window.interfacesAndAbstractions;

import java.util.Collections;

import helper.Helper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TextField;
/** <pre>
 * A node can have three types of layouts:
 * 
 * - Normal: 		when the node displays a value that is valid and has already been processed
 * - Changed:		when the node displays a value that is valid, but this change has not yet been processed
 * - Invalid:		when the node displays an invalid value. By definition, this value has not been processed, and will not be processed.
 *
 */
public abstract class LayoutManager {

	public enum State{
		Normal, Changed, Invalid;
	}
	
	public enum TextFieldValidInputCriterium {
		DOUBLE, POSITIVE_DOUBLE, NEGATIVE_DOUBLE, NON_NEGATIVE_DOUBLE, NON_POSITIVE_DOUBLE,
		INTEGER, POSITIVE_INTEGER, NEGATIVE_INTEGER, NON_NEGATIVE_INTEGER, NON_POSITIVE_INTEGER,
		PROBABILITY,
		
		DOUBLE_ALLOWING_EMPTY, POSITIVE_DOUBLE_ALLOWING_EMPTY, NEGATIVE_DOUBLE_ALLOWING_EMPTY, NON_NEGATIVE_DOUBLE_ALLOWING_EMPTY, NON_POSITIVE_DOUBLE_ALLOWING_EMPTY,
		PROBABILITY_ALLOWING_EMPTY ;
	}

	/** 
	 * Set the layout of the TextField such that:
	 * 
	 * - it will have a normal layout when the TextField contains a valid value, and this
	 * 		value has already been processed (i.e., does not update);
	 * - it will have a 'changed' layout when the TextField contains a valid value, but 
	 * 		this value has not been passed on to other objects yet;
	 * - it will have a 'invalid' layout when the TextField does not contain a valid value.
	 * 
	 * What a valid value is, is determined by the TextFieldValidInput type. Note that layout
	 * changes only trigger when the text is changed AND the textfield is in focus.
	 */
	public static void setLayoutHandler(TextField tf, final TextFieldValidInputCriterium criterium) {
		// Set the listeners that govern the validity (and layout)
		tf.textProperty().addListener(new ChangeListener<String>(){	
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!tf.isFocused()) 
					return;
				
				if (isValid(newValue, criterium))
					setChangedLayout(tf);
				else
					setInvalidLayout(tf);
			}
		});	

		// make the focus listener fire after enter has been pressed
		tf.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				tf.getParent().requestFocus();
			}
		});
		
		// Update whenever the focus is lost: make the layout invalid if the value is invalid
		tf.focusedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				if (!newValue && !isValid(tf.getText(), criterium)) 
					setInvalidLayout(tf);
				
			}
			
		});
	}

	/** Set the layout of the TextField such that:
	 * 
	 * - it will have a normal layout when the TextField contains a valid value, and this
	 * 		value has already been processed (i.e., does not update);
	 * - it will have a normal layout when the TextField does not contain any text
	 * - it will have a 'changed' layout when the TextField contains a valid value, but 
	 * 		this value has not been passed on to other objects yet;
	 * - it will have a 'invalid' layout when the TextField does not contain a valid value.
	 * 
	 * What a valid value is, is determined by the TextFieldValidInput type. Note that layout
	 * changes only trigger when the text is changed AND the textfield is in focus.
	 */
		public static void setLayoutHandlerAllowingEmpty(TextField tf, final TextFieldValidInputCriterium criterium) {
			// Set the listeners that govern the validity (and layout)
			tf.textProperty().addListener(new ChangeListener<String>(){	
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (!tf.isFocused()) 
						return;
					
					if (isValid(newValue, criterium) || tf.getText().length() == 0)
						setChangedLayout(tf);
					else
						setInvalidLayout(tf);
				}
			});	

			// make the focus listener fire after enter has been pressed
			tf.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					tf.getParent().requestFocus();
				}
			});
			
			// Update whenever the focus is lost: make the layout invalid if the value is invalid
			tf.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (!newValue && !isValid(tf.getText(), criterium) && tf.getText().length() > 0) 
						setInvalidLayout(tf);
					
				}
				
			});
		}

	/** Returns true iff the string matches the specified criterium (e.g., if the string represents a valid double) */	 
	private static boolean isValid(String s, TextFieldValidInputCriterium criterium) {
		switch(criterium) {
		case DOUBLE: 			return Helper.isDouble(s);
		case POSITIVE_DOUBLE: 	return Helper.isPositiveDouble(s);
		case NEGATIVE_DOUBLE:	return Helper.isNegativeDouble(s);
		case NON_NEGATIVE_DOUBLE: return Helper.isDouble(s) && !Helper.isNegativeDouble(s);
		case NON_POSITIVE_DOUBLE: return Helper.isDouble(s) && !Helper.isPositiveDouble(s);
		
		case INTEGER: 			return Helper.isInteger(s);
		case POSITIVE_INTEGER: 	return Helper.isPositiveInteger(s);
		case NEGATIVE_INTEGER:	return Helper.isNegativeInteger(s);
		case NON_NEGATIVE_INTEGER: return Helper.isInteger(s) && !Helper.isNegativeInteger(s);
		case NON_POSITIVE_INTEGER: return Helper.isInteger(s) && !Helper.isPositiveInteger(s);
		
		case DOUBLE_ALLOWING_EMPTY: 			 return s.length()==0 || Helper.isDouble(s);
		case POSITIVE_DOUBLE_ALLOWING_EMPTY: 	 return s.length()==0 || Helper.isPositiveDouble(s);
		case NEGATIVE_DOUBLE_ALLOWING_EMPTY:	 return s.length()==0 || Helper.isNegativeDouble(s);
		case NON_NEGATIVE_DOUBLE_ALLOWING_EMPTY: return s.length()==0 || Helper.isDouble(s) && !Helper.isNegativeDouble(s);
		case NON_POSITIVE_DOUBLE_ALLOWING_EMPTY: return s.length()==0 || Helper.isDouble(s) && !Helper.isPositiveDouble(s);
		
		case PROBABILITY:						return Helper.isProbability(s);
		case PROBABILITY_ALLOWING_EMPTY:		return s.length()==0 || Helper.isProbability(s);
		
		default:				return false;
		}
	}
	

	/** Set the layout of the TextField such that:
	 * 
	 * - it will have a normal layout when the TextField contains an IN RANGE value, and this
	 * 		value has already been processed (i.e., does not update);
	 * - it will have a 'changed' layout when the TextField contains an IN RANGE value, but 
	 * 		this value has not been passed on to other objects yet;
	 * - it will have a 'invalid' layout when the TextField does not contain an IN RANGE value.
	 * 
	 * (A number is IN RANGE if and only if it has a value between [minimum,maximum])
	 */
	public static void setLayoutHandlerIntegerInRange(TextField tf, int minimum, int maximum) {
		// Set the listeners that govern the validity (and layout)
		tf.textProperty().addListener(new ChangeListener<String>(){	
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!tf.isFocused())
					return;
				if (Helper.isInteger(newValue))
					if (Integer.parseInt(newValue) >= minimum && Integer.parseInt(newValue) <= maximum )
						setChangedLayout(tf);
					else
						setInvalidLayout(tf);
			}
		});	

		// make the focus listener fire after enter has been pressed
		tf.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				tf.getParent().requestFocus();
			}
		});
	}



	/** Changes the layout of the node to normal.
	 * @param tf
	 * @param invalid
	 */
	private static void setNormalLayout(Node n) {
		n.getStyleClass().removeAll(Collections.singleton("invalid"));
		n.getStyleClass().removeAll(Collections.singleton("changed"));
	}

	/** 
	 * Set the layout of the node to invalid
	 * 
	 * @param n
	 * @param validInput
	 */
	private static void setInvalidLayout(Node n) {
		setNormalLayout(n);				// make sure no other states are present
		n.getStyleClass().add("invalid");
	}

	/** 
	 * Set the layout of the node to changed.
	 * 
	 * @param n
	 * @param validInput
	 */
	private static void setChangedLayout(Node n) {
		setNormalLayout(n);				// make sure no other states are present
		n.getStyleClass().add("changed");
	}

	/**
	 * After an event has been processed, the layout
	 * of the node should be set to normal. That is
	 * what this function does. Returns true if
	 * there was indeed an unprocessed change. Returns
	 * false if this function was called on a node
	 * that did not have an unprocessed value.
	 * @param n
	 * @return
	 */
	public static boolean setProcessed(Node n) {
		if (isChanged(n)) {
			setNormalLayout(n);
			return true;
		}
		return false;
	}

	/** Returns true if the Node has a 'invalid' layout 
	 * (i.e., the node contains an invalid value)
	 * @param tf
	 * @return
	 */
	public static boolean isInvalid (Node n) {
		return n.getStyleClass().contains("invalid");
	}

	/** Returns true if the Node has a 'changed' layout 
	 * (i.e., the node contains a value that is valid, but
	 * not yet processed)
	 * @param tf
	 * @return
	 */
	public static boolean isChanged (Node n) {
		return n.getStyleClass().contains("changed");
	}

	/** Returns true if the Node has a 'normal' layout 
	 * (i.e., the node contains a value that is valid and
	 * this valid has not been changed)
	 * @param tf
	 * @return
	 */
	public static boolean isNormal (Node n) {
		if (n.getStyleClass().contains("changed"))
			return false;
		if (n.getStyleClass().contains("invalid"))
			return false;
		return true;
	}

	/** 
	 * Returns one of three states:
	 * - Normal: 		when the node displays a value that is valid and has already been processed
	 * - Changed:		when the node displays a value that is valid, but this change has not yet been processed
	 * - Invalid:		when the node displays an invalid value. By definition, this value has not been processed, and will not be processed.
	 * @return
	 */
	public static State getState(Node n) {
		if (isInvalid(n))
			return State.Invalid;
		if (isChanged(n))
			return State.Changed;
		return State.Normal;
	}

	/** 
	 * Set the layout of the node to one of three states.
	 * - Normal: 		when the node displays a value that is valid and has already been processed
	 * - Changed:		when the node displays a value that is valid, but this change has not yet been processed
	 * - Invalid:		when the node displays an invalid value. By definition, this value has not been processed, and will not be processed.
	 * @param n
	 * @param s
	 */
	public static void setState(Node n, State s) {
		if (s == State.Normal)
			setNormalLayout(n);
		else if (s== State.Changed)
			setChangedLayout(n);
		else
			setInvalidLayout(n);
	}
}
