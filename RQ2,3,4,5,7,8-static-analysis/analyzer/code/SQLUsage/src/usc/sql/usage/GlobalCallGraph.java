package usc.sql.usage;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

import edu.usc.sql.graphs.cdg.Dominator;
import edu.usc.sql.graphs.cfg.CFGInterface;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import soot.ResolutionFailedException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import usc.sql.ir.Variable;
import usc.sql.region.LayerRegion;
import usc.sql.string.JavaAndroid;
import usc.sql.string.ReachingDefinition;
import CallGraph.EntryPoint;
import CallGraph.NewNode;
import SootEvironment.AndroidApp;

public class GlobalCallGraph {
	
	private Set<String> dbNames = new HashSet<>();
	private boolean containDB = false;
	private Map<String,Integer> sqliteAPIFreq = new HashMap<>();
	private Set<String> sqliteClass = new HashSet<>();
	private Set<String> sqliteMethodEntry = new HashSet<>();
	private Map<String,Integer> jdbcAPIFreq = new HashMap<>();
	private Map<String,Integer> dbFreq = new HashMap<>();
	private List<String> otherAPI = new ArrayList<>();
	private EntryPoint entryPoint = new EntryPoint();
	private String [] keywords = {"SET","DROP","CREATE","WITH","DELETE","SELECT","INSERT","UPDATE",
    		"REPLACE","TRUNCATE","ALTER","EXEC","MERGE","ALTER"};
	private String outputPath;
	private String appfolder;
	private JavaAndroid ja;
	private AndroidApp App;
	public GlobalCallGraph(String rtjar,String appfolder,String classlist,String apk, String output)
	{
		outputPath = output;
		this.appfolder = appfolder;
		//Berkeley DB
		dbNames.add("java.sql");
		//CouchBase
		dbNames.add("com.couchbase");
		//LevelDB
		dbNames.add("leveldb");
		//SQLite
		dbNames.add("android.database.sqlite");
		//Interbase
		dbNames.add("interbase");
		//realm
		dbNames.add("io.realm");
		//SnappyDB
		dbNames.add("com.snappydb");
		//Sparksee Mobile
		dbNames.add("com.ianywhere.ultralite");
		//UnQLite
		dbNames.add("unqlite");		
		//MongoDB
		dbNames.add("com.mongodb");		
		//WazeDB
		dbNames.add("com.waze.db");
		
		
		for(String db: dbNames)
			dbFreq.put(db, 0);

		
		//parseSQL("insert into Students('StudentName','StudentID') values (?,?)");
		startAndroid(rtjar,appfolder+apk,appfolder+classlist);
	}

	String appName;
	private void startAndroid(String arg0,String arg1,String arg2)
	{
		//"/home/yingjun/Documents/StringAnalysis/MethodSummary/"
		//"Usage: rt.jar app_folder classlist.txt"
		App=new AndroidApp(arg0,arg1,arg2);

		//appName = arg1;
		appName = arg1.substring(arg1.indexOf("/App"));
		/*
		for(CFGInterface bg : App.getCallgraph().getRTOInterfaceBlockGraph())
		{
			
			Dominator d = new Dominator(bg.getAllNodes(), bg.getAllEdges(), bg.getEntryNode());
		}
		*/
		
		analyzeQuery();

		//analyzeAppDB(App,arg1);
		
		//getDataSource();
		//analyzeInstructionSize(App,arg1);
		//analyzeTransaction();
		/*
		System.out.println(arg1);
		System.out.println("DB Freqency");
		for(String db: dbFreq.keySet())
		{
			if(dbFreq.get(db)!=0)
				System.out.println(db + ":" + dbFreq.get(db));
		}
		System.out.println("SQLite API Frequency");
		for(String api: sqliteAPIFreq.keySet())
		{
			System.out.println(api+":"+sqliteAPIFreq.get(api));
		}
		*/
	}
	public void analyzeQuery()
	{
		List<Integer> paraSet = new ArrayList<>();
		paraSet.add(1);
		Map<String,List<Integer>> target = new HashMap<>();
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteStatement compileStatement(java.lang.String)>",paraSet);
		target.put("<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String)>",paraSet);
		target.put("<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String,java.lang.Object[])>",paraSet);
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQuery(java.lang.String,java.lang.String[])>",paraSet);
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQuery(java.lang.String,java.lang.String[],android.os.CancellationSignal)>",paraSet);
		
		List<Integer> paraSet2 = new ArrayList<>();
		paraSet2.add(2);
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,java.lang.String,java.lang.String[],java.lang.String)>", paraSet2);
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,java.lang.String,java.lang.String[],java.lang.String,android.os.CancellationSignal)>", paraSet2);

		//query method
		
		
		App.getCallgraph().setPotentialAPI(identifyRelevant(target.keySet()));
		
		StringBuilder output = new StringBuilder();
		output.append(appName+"\n");
		ja = new JavaAndroid(App,appfolder,target,1);
		Map<String, Set<Variable>> stringResult = ja.getResult();
		QueryParser.parseStringResult(ja,stringResult, output);
	    try {
				PrintWriter pw = new PrintWriter(new FileWriter(outputPath, true));
				pw.println(output.toString());
				pw.println();
				pw.close();
	    }
	    catch(IOException e)
	    {
	    	
	    }
		
	}
	public Set<String> identifyRelevant(Set<String> targetScanList)
	{
		Set<String> targetMethod = new HashSet<>();
		List<NewNode> rto = App.getCallgraph().getRTOdering();
		Map<String,NewNode> rtoMap = App.getCallgraph().getRTOMap();
		Set<String> rtoSig = new HashSet<>();
		for(NewNode n: rto)
		{

			if(n.getMethod().isConcrete()&&!n.getMethod().getDeclaringClass().isAbstract()&&!containLib(n.getMethod().getSignature()))
			{
				rtoSig.add(n.getMethod().getSignature());
				boolean isRelevant = false;
				for(Unit actualNode:n.getMethod().retrieveActiveBody().getUnits())
				{
					
					if(((Stmt)actualNode).containsInvokeExpr())
					{
						SootMethod sm = null;
						try{
							sm =((Stmt)actualNode).getInvokeExpr().getMethod();
						}
						catch(ResolutionFailedException ex)
						{
							System.out.println("Soot fails to get the method:"+((Stmt)actualNode).getInvokeExpr());
						}	
						if(sm==null)
							continue;
						
						String sig= sm.getSignature();
						
						if(targetScanList.contains(sig)||targetMethod.contains(sig))
						{
							isRelevant = true;
						}
						
					}
				}
				
				if(isRelevant)
				{
					targetMethod.add(n.getMethod().getSignature());
					
				}


			}
			
		}
		System.out.println("Methods contain target APIs: "+ targetMethod.size());
		Stack<String> processMethod = new Stack<>();
		processMethod.addAll(targetMethod);
		//topo
		while(!processMethod.isEmpty())
		{
			String currentMethod = processMethod.pop();
			NewNode n = rtoMap.get(currentMethod);
			SootMethod sm = n.getMethod();
			if(sm.isConcrete()&&!sm.getDeclaringClass().isAbstract()&&!containLib(sm.getSignature()))
			{
				
				for(Unit actualNode:n.getMethod().retrieveActiveBody().getUnits())
				{
					
					if(((Stmt)actualNode).containsInvokeExpr())
					{
						
						SootMethod method = ((Stmt)actualNode).getInvokeExpr().getMethod();
						String type = method.getReturnType().toString();
						
						if(type.equals("java.lang.String")||type.equals("java.lang.StringBuilder")
								||type.equals("java.lang.StringBuffer"))
						{
							if(rtoSig.contains(method.getSignature()))
							{
								if(!targetMethod.contains(method.getSignature()))
								{
									processMethod.push(method.getSignature());
									targetMethod.add(method.getSignature());
								}
							}
						}
	
						
					}
				}
			}

		}
		System.out.println("Target methods and callees: "+ targetMethod.size());
		
		for(NewNode n: rto)
		{

			if(n.getMethod().isConcrete()&&!n.getMethod().getDeclaringClass().isAbstract()&&!containLib(n.getMethod().getSignature()))
			{

				for(Unit actualNode:n.getMethod().retrieveActiveBody().getUnits())
				{
					
					if(((Stmt)actualNode).containsInvokeExpr())
					{
						SootMethod sm = null;
						try{
							sm =((Stmt)actualNode).getInvokeExpr().getMethod();
						}
						catch(ResolutionFailedException ex)
						{
							System.out.println("Soot fails to get the method:"+((Stmt)actualNode).getInvokeExpr());
						}	
						if(sm==null)
							continue;
						
						String sig= sm.getSignature();
						
						if(targetMethod.contains(sig))
						{
							targetMethod.add(n.getMethod().getSignature());
						}
						
					}
				}
				

			}
			
		}
		System.out.println("Target methods and callers and callees: "+ targetMethod.size());
		Set<String> targetClass = new HashSet<>();
		for(String sig: targetMethod)
		{
			targetClass.add(sig.substring(0,sig.indexOf(":")+1));
		}
		for(NewNode n: rto)
		{
			String sig = n.getMethod().getSignature();
			String className = sig.substring(0,sig.indexOf(":")+1);
			if(sig.contains("<init>")||sig.contains("<clinit>"))
			{
				if(targetClass.contains(className))
				{
					targetMethod.add(sig);
				}
				
			}
				
		}
		System.out.println("Total Target Method including constructor: "+ targetMethod.size());
		return targetMethod;
	}
	public void getDataSource()
	{
		Set<String> targetAPI = new HashSet<>();
		targetAPI.add("<android.database.sqlite.SQLiteDatabase: long insert(java.lang.String,java.lang.String,android.content.ContentValues)>");
		targetAPI.add("<android.database.sqlite.SQLiteDatabase: long insertOrThrow(java.lang.String,java.lang.String,android.content.ContentValues)>");
		targetAPI.add("<android.database.sqlite.SQLiteDatabase: long insertWithOnConflict(java.lang.String,java.lang.String,android.content.ContentValues,int)>");
		targetAPI.add("<android.database.sqlite.SQLiteDatabase: int update(java.lang.String,android.content.ContentValues,java.lang.String,java.lang.String[])>");
		targetAPI.add("<android.database.sqlite.SQLiteDatabase: int updateWithOnConflict(java.lang.String,android.content.ContentValues,java.lang.String,java.lang.String[],int)>");
		targetAPI.add("<android.database.sqlite.SQLiteDatabase: long replace(java.lang.String,java.lang.String,android.content.ContentValues)>");
		targetAPI.add("<android.database.sqlite.SQLiteDatabase: long replaceOrThrow(java.lang.String,java.lang.String,android.content.ContentValues)>");
		
    	for(CFGInterface cfg:App.getCallgraph().getRTOInterface())
    	{

    		DefAnalysis da = new DefAnalysis(cfg, ja.getReachingDefinition().get(cfg.getSignature()), targetAPI);
    		da.findTargetVariable("android.content.ContentValues");
    		
    	}
	}
	public void analyzeInstructionSize(AndroidApp App, String appPath)
	{
		int count = 0;
		for(NewNode n: App.getCallgraph().getRTOdering())
		{
			if(n.getMethod().isConcrete())
			{
				count += n.getMethod().retrieveActiveBody().getUnits().size();
			}
		}
	    try {
				PrintWriter pw = new PrintWriter(new FileWriter(outputPath, true));
				pw.println(appPath);
				
				pw.println("Number of instructions:"+count);
				pw.println();
				pw.close();
	    }
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
	}
	public void analyzeAppDB(AndroidApp App,String appPath)
	{
		boolean inUserClass = false;
		boolean inUserEvent = false;
		for(NewNode n: App.getCallgraph().getRTOdering())
		{

			if(n.getMethod().isConcrete())
			{
				if(containLib(n.getMethod().getSignature()))
					continue;
				for(Unit actualNode:n.getMethod().retrieveActiveBody().getUnits())
				{					
					Stmt stmt = (Stmt) actualNode;
					if(stmt.containsInvokeExpr())
					{
						SootMethod sm = null;
						try{
							sm = stmt.getInvokeExpr().getMethod();
						}
						catch(ResolutionFailedException ex)
						{
							System.out.println("Soot fails to get the method:"+stmt.getInvokeExpr());
						}
						if(sm == null)
							continue;
						String className = stmt.getInvokeExpr().getMethod().getDeclaringClass().toString();
						String sig = stmt.getInvokeExpr().getMethod().getSignature();
						//DB usage frequency
						if(className.equals("java.sql.Timestamp")||
								className.equals("java.sql.SQLException")||
								className.equals("java.sql.Date")||
								className.equals("java.sql.Time"))
							continue;
						for(String db: dbNames)
							if(className.contains(db))
							{
								containDB = true;
								dbFreq.put(db, dbFreq.get(db)+1);
								if(!className.contains("android.database.sqlite")&&!className.contains("java.sql."))
									otherAPI.add(sig);
							}
						//SQLite API usage frequency
						if(className.contains("android.database.sqlite"))
						{
							if(sqliteAPIFreq.containsKey(sig))
								sqliteAPIFreq.put(sig, sqliteAPIFreq.get(sig)+1);
							else
								sqliteAPIFreq.put(sig,1);
							sqliteClass.add(n.getMethod().getDeclaringClass().toString());
							sqliteMethodEntry.addAll(App.getCallgraph().getEntryMethod(n.getMethod().getSignature()));							
						}
						else if(className.contains("java.sql"))
						{
							if(jdbcAPIFreq.containsKey(sig))
								jdbcAPIFreq.put(sig, jdbcAPIFreq.get(sig)+1);
							else
								jdbcAPIFreq.put(sig,1);
						}
						
					}
				}
			}
		}
	    try {
				PrintWriter pw = new PrintWriter(new FileWriter(outputPath, true));
				pw.println(appPath);
				
				for(String db: dbFreq.keySet())
				{
					if(dbFreq.get(db)!=0)
						pw.println(db + ":" + dbFreq.get(db));
				}
				pw.println("SQLite API Frequency");
				pw.println();
				pw.close();
	    }
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		/*
		StringBuilder result = new StringBuilder();
	    try {
				PrintWriter pw = new PrintWriter(new FileWriter(outputPath, true));
				pw.println(appPath.substring(appPath.indexOf("/App")+1));
				
				pw.println("DB Frequncy");
				for(String db: dbFreq.keySet())
				{
					if(dbFreq.get(db)!=0)
						pw.println(db + ":" + dbFreq.get(db));
				}
				pw.println("SQLite API Frequency");
				for(String api: sqliteAPIFreq.keySet())
				{
					pw.println(api+":"+sqliteAPIFreq.get(api));
				}
				pw.println("JDBC API Frequency");
				for(String api: jdbcAPIFreq.keySet())
				{
					pw.println(api+":"+jdbcAPIFreq.get(api));
				}
				String appName = appPath.substring(appPath.lastIndexOf("/")+1);

				Set<String> entries= new HashSet<>();
				if(appName.replaceAll(".apk", "").contains("."))
				{	
					String[] packageName = appName.split("\\.");
					String match = null;
					if(packageName.length == 2)
						match = packageName[0];
					else if(packageName.length > 2)
						match = packageName[0]+packageName[1];
					
					for(String sqliteC : sqliteClass)
					{
						if(sqliteC.replaceAll("\\.", "").contains(match))
						{
							inUserClass = true;
							break;
						}
					}
					for(String sqliteM : sqliteMethodEntry)
					{
						if(isEntry(sqliteM)&&!containLib(sqliteM))
						{
							
							inUserEvent = true;
							entries.add(sqliteM);
							//break;
						}
					}
				}
				
				pw.println("Invoke SQLite in user code very likely:"+inUserClass);
				pw.println("Invoke SQLite in user event:"+inUserEvent);
				pw.println("Classes invoke SQLite");				
				pw.println(sqliteClass);
				pw.println("Method entries invoke SQLite");
				pw.println(entries);
				pw.println();
				pw.close();
				
				
				if(!otherAPI.isEmpty())
				{
					PrintWriter pwother = new PrintWriter(new FileWriter("/home/yingjun/Documents/appset/other.txt", true));
					pwother.println(appPath.substring(appPath.indexOf("/App")+1));
					for(String other: otherAPI)
					{
						pwother.println(other);
					}
					pwother.close();
					
				}
				
				
	    }
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		*/
	}
	public void analyzeTransaction()
	{
		TransactionAnalysis ta = new TransactionAnalysis(App);
		ta.printResult(appName, outputPath);
		
	}
	public boolean isEntry(String methodSig)
	{
		String className = methodSig.split(":")[0].replace("<", "");
		SootClass sc = Scene.v().getSootClass(className);
		String secondHalf = methodSig.split(":")[1];
		String methodName = secondHalf.substring(1,secondHalf.length()-1);
		
		return entryPoint.isEntry(sc, methodName);
	}
	public static boolean containLib(String s)
	{

		if(s.startsWith("<com.google")||
				s.startsWith("<com.facebook")||
				s.startsWith("<com.urbanairship")||
				s.startsWith("<net.robotmedia")||
				s.startsWith("<com.localytics.android")||
				s.startsWith("<com.millennialmedia.android"))
			return true;
		else
			return false;
	}
}
