package usc.sql.log;

public class Instance {

	String api;
	boolean inUI,inBegin,mayAlias,notInBegin,defineInLoop,initInLoop;
	String identifier;
	int lengthModToBegin;
	int lengthModToLoop;
	int lengthModToEntry;
	public Instance(String api,boolean inUI)
	{
		this.api = api;
		this.inUI = inUI;
	}
}
