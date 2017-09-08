package usc.sql.instrument;

public class InstrumentMain {
	public static void main(String[] args) {

		
		//GlobalCallGraph gfg  = new GlobalCallGraph(args[0],args[1],args[2],args[3],args[4]);
		//new  GlobalCallGraph("/home/yingjun/Documents/eclipse/workspace/SQLiteDetection/bin/");
		
		String[] argsIns = new String [3];
		argsIns[0] = "-w";
		argsIns[1] = "-process-dir";
		argsIns[2] = args[1]+args[3]; //apk
		AutoInstrument ais = new AutoInstrument(argsIns);
		
			
	}
}
