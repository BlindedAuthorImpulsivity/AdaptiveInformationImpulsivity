package agent;

import java.io.Serializable;

public enum Action implements Serializable
{

	EAT(),
	DISCARD(),
	SAMPLE(),
	DEAD_ON_START();

	

}
