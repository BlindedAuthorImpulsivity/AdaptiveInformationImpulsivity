package window.interfacesAndAbstractions;

/**
 * Every change that is made somewhere in the View has to be cascaded throughout the rest of the view. To make this 
 * 'wiring' of information more clear, all tabs and stages in the View should tell the Notification manager what
 * has changed (the objectChanged object), and what caused the change (the sourceOfChange Object). 
 * 
 * To retain some oversight on the flow of information, the following rules should be applied:
 * - After an object is changed, the change to the object itself is handled locally 
 * 		(e.g., a change in the MDP Builder by the user results in a direct change in the MDP Builder)
 * - After his direct, local change, the notificationManager is called via notifyAll(). This function
 * 		has as an argument the object that has changed. Based on which object is changed, the notification
 * 		manager then informs other objects of this change 
 * 		(e.g., it calls the update() function of all EnvironmentBuilders).
 * - These objects then deal with the updates themselves
 * 		(e.g., the EnvironmentBuilderFull recalculates the probability distribution over resource values and extrinsic events)
 * 
 * At times, the source of change might want to specify additional parameters (for instance, if a TableView object
 * changes something, it might want to send additional information about what exactly has been changed in the column).
 * For these cases, an array of Objects can be send as an additional parameter.
 * 
 * A NotificationManager is the object that handles all updates throughout the view. This is usually the FrameController. */
public interface NotificationManager {
	
	/** This function is called by all objects in the View (sourceOfChange) when they make a change
	 * to an object (objectChanged). Upon being called by notifyAll, the NoficationManager should
	 * prompt all other objects that need updating to update themselves. 
	 */
	public void notifyAll(Object objectChanged, Object sourceOfChange, Object... args);
	
	/** This function is called by all objects in the View (sourceOfChange) when they make a change
	 * to an object (objectChanged). Upon being called by notifyAll, the NoficationManager should
	 * prompt all other objects that need updating to update themselves. 
	 */
	public void notifyAll(Object objectChanged, Object sourceOfChange) ;

}
