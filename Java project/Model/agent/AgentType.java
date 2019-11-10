package agent;

public enum AgentType {
	
	VALUE_ITERATOR("Value iteration (full information of MDP)");
	
	private final String name;
	
	private AgentType(String s) {this.name = s; }
	
	public String toString(){return this.name;}

}
