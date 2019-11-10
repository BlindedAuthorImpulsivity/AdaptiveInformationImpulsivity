package markovDecisionProcess;

/** An enumeration listing all possible cue emission types.
 * ~~ TAG: "adding cue emission type"
 * If you want to implement a new cue emission type, list the name and the text here.
 * @author jesse
 *
 */
public enum CueEmissionType { 
	Linear("Linear cue emissions"), 
	Normal("Normal cue emissions"), 
	Manual("Manual cue emissions");
	
	private final String name;
	
	private CueEmissionType(String s) {this.name = s; }
	
	public String toString(){return this.name;}
	}
