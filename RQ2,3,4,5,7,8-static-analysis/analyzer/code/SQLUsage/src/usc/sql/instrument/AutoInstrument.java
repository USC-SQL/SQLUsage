package usc.sql.instrument;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.sql.graphs.Node;
import edu.usc.sql.graphs.NodeInterface;
import edu.usc.sql.graphs.cfg.CFGInterface;
import soot.Body;
import soot.BodyTransformer;
import soot.BooleanType;
import soot.ByteType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.ResolutionFailedException;
import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootFieldRef;
import soot.SootMethod;
import soot.Transform;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.dexpler.TrapMinimizer;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.NullConstant;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.ThrowStmt;
import soot.jimple.internal.JTrap;
import soot.options.Options;
import soot.toDex.TrapSplitter;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import usc.sql.region.RegionNode;
import usc.sql.usage.GlobalCallGraph;

public class AutoInstrument {

	private String [] args;
	Map<String,Integer> target = new HashMap<>();
	public AutoInstrument(String [] args)
	{
		this.args = args;
		

		target.put("<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteStatement compileStatement(java.lang.String)>",0);
		target.put("<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String)>",0);
		target.put("<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String,java.lang.Object[])>",0);
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQuery(java.lang.String,java.lang.String[])>",0);
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQuery(java.lang.String,java.lang.String[],android.os.CancellationSignal)>",0);

		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,java.lang.String,java.lang.String[],java.lang.String)>", 1);
		target.put("<android.database.sqlite.SQLiteDatabase: android.database.Cursor rawQueryWithFactory(android.database.sqlite.SQLiteDatabase$CursorFactory,java.lang.String,java.lang.String[],java.lang.String,android.os.CancellationSignal)>", 1);

		
		
		performInstrument();
	}
	
	private void performInstrument()
	{
		
		Options.v().set_output_format(Options.output_format_dex);
		Options.v().force_overwrite();
		Options.v().set_output_dir(args[2].substring(0, args[2].lastIndexOf("/"))+"/ins");
		
		PackManager.v().getPack("jtp").add(new Transform("jtp.yourInstrumenter", new BodyTransformer() {


			@Override
			protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) 
			{

				final PatchingChain<Unit> units = b.getUnits();
				for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
					final Unit u = iter.next();
					Stmt stmt = (Stmt) u;
					
					if(stmt.containsInvokeExpr())
					{
						SootMethod sm = null;
						try{
							sm =stmt.getInvokeExpr().getMethod();
						}
						catch(ResolutionFailedException ex)
						{
							System.out.println("Soot fails to get the method:"+stmt.getInvokeExpr());
						}	
						if(sm==null)
							continue;
						
						String sig= sm.getSignature();
						
						String className = sm.getDeclaringClass().toString();
						if(className.contains("android.database.sqlite"))
						{
							System.out.println(sig);
							SootMethod startTime = Scene.v().getSootClass("usc.sql.log.InstrumentHelper").getMethod("void printFinerTimeBefore(java.lang.String,java.lang.String)");
					    	List<Value> paras = new ArrayList<>();
					    	String signature = sig.substring(1,sig.length()-1);
					    	paras.add(StringConstant.v(signature));
					    	
							if(target.containsKey(sig))
							{						    	
						    	//target parameter, currently we only support printing one target parameter						 
						    	int targetParaOffset = target.get(sig);
						    	Value targetPara = stmt.getInvokeExpr().getArg(targetParaOffset);
						    	paras.add(targetPara);		
							}
							else
							{
								//pass a null to the print method
								paras.add(NullConstant.v());
							}    	
					    	Stmt startInvoke = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(startTime.makeRef(),paras));
					    	units.insertBefore(startInvoke, u);
					    	
					    	SootMethod endTime = Scene.v().getSootClass("usc.sql.log.InstrumentHelper").getMethod("void printFinerTimeEnd(java.lang.String)");
					    	List<Value> parasEnd = new ArrayList<>();
					    	parasEnd.add(StringConstant.v(signature));
					    	Stmt endInvoke = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(endTime.makeRef(),parasEnd));
					    	units.insertAfter(endInvoke, u);
							b.validate();
						}
						
						
					}
					
				}
				
			}
		}
		));
		

		soot.Main.main(args);
/*	    try {
				PrintWriter pwTime = new PrintWriter(new FileWriter("/home/yingjun/Documents/sqlite/time.txt", true));
				pwTime.println("InsTotal:"+insTime);
				pwTime.close();
	    }
		catch(IOException ex)
		{
		
		}*/

/*		try {
			PrintWriter pwEva = new PrintWriter(new FileWriter(outputFile, true));

			pwEva.println("Repair Run Time:" + insTime*1.0/1000+"\n");
			pwEva.flush();
			pwEva.close();
		}
		catch(IOException ex)
		{
		
		}*/

	}


	private void addItrAfter(Body body,PatchingChain<Unit> units,Unit start,String regionNum)
	{
    	List<Unit> list = new ArrayList<>();
	 	
		// insert "$r4 = java.lang.System.out;"   $r4 -> tmp4
        Local $r4 = Jimple.v().newLocal("itr4", RefType.v("java.io.PrintStream"));
        body.getLocals().add($r4);

        list.add(Jimple.v().newAssignStmt($r4, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));
       
        // insert "$r3 = new java.lang.StringBuilder" $r3 -> tmp3
        Local $r3 = Jimple.v().newLocal("itr3", RefType.v("java.lang.StringBuilder"));
        body.getLocals().add($r3);

	        
        list.add(Jimple.v().newAssignStmt($r3, Jimple.v().newNewExpr(RefType.v("java.lang.StringBuilder"))));
        
       // insert " specialinvoke $r3.<java.lang.StringBuilder: void <init>(java.lang.String)>("Start") "
        
        SootMethod init = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("void <init>(java.lang.String)");
       
        list.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr($r3, init.makeRef(), StringConstant.v("Itr@"+regionNum))));
        
       //insert "$l1 = staticinvoke <java.lang.System: long currentTimeMillis()>()"   $l1 -> tmp1
        Local $l1 = Jimple.v().newLocal("itr1", LongType.v());
        body.getLocals().add($l1);
        
        SootMethod time = Scene.v().getSootClass("java.lang.System").getMethod("long currentTimeMillis()");

        
        list.add(Jimple.v().newAssignStmt($l1,Jimple.v().newStaticInvokeExpr(time.makeRef())));
        

        //insert "$r5 = virtualinvoke $r3.<java.lang.StringBuilder: java.lang.StringBuilder append(long)>($l1)"  $r5 -> tmp5
        Local $r5 = Jimple.v().newLocal("itr5", RefType.v("java.lang.StringBuilder"));
        body.getLocals().add($r5);
        SootMethod append = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.StringBuilder append(long)");

        
        list.add(Jimple.v().newAssignStmt($r5,Jimple.v().newVirtualInvokeExpr($r3, append.makeRef(), $l1)));
        
       //insert $r6 = virtualinvoke $r5.<java.lang.StringBuilder: java.lang.String toString()>()   $r6 -> tmp6
        Local $r6 = Jimple.v().newLocal("itr6",RefType.v("java.lang.String"));
        body.getLocals().add($r6);
        SootMethod toString = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.String toString()");
        
        list.add(Jimple.v().newAssignStmt($r6, Jimple.v().newVirtualInvokeExpr($r5, toString.makeRef())));
        
        //insert virtualinvoke $r4.<java.io.PrintStream: void println(java.lang.String)>($r6)
        SootMethod println = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
        
        list.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr($r4, println.makeRef(), $r6)));
        
        
        
        units.insertAfter(list, start);
	}
	
    private void addTimeStampBefore(Body body,PatchingChain<Unit> units,Unit target,String act)
    {
    	
    	List<Unit> list = new ArrayList<>();
	 	
		// insert "$r4 = java.lang.System.out;"   $r4 -> tmp4
        Local $r4 = Jimple.v().newLocal("start4", RefType.v("java.io.PrintStream"));
        body.getLocals().add($r4);

        list.add(Jimple.v().newAssignStmt($r4, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));
       
        // insert "$r3 = new java.lang.StringBuilder" $r3 -> tmp3
        Local $r3 = Jimple.v().newLocal("start3", RefType.v("java.lang.StringBuilder"));
        body.getLocals().add($r3);

	        
        list.add(Jimple.v().newAssignStmt($r3, Jimple.v().newNewExpr(RefType.v("java.lang.StringBuilder"))));
        
       // insert " specialinvoke $r3.<java.lang.StringBuilder: void <init>(java.lang.String)>("Start") "
        
        SootMethod init = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("void <init>(java.lang.String)");
       
        list.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr($r3, init.makeRef(), StringConstant.v("Start"+act+":"))));
        
       //insert "$l1 = staticinvoke <java.lang.System: long currentTimeMillis()>()"   $l1 -> tmp1
        Local $l1 = Jimple.v().newLocal("start1", LongType.v());
        body.getLocals().add($l1);
        
        SootMethod time = Scene.v().getSootClass("java.lang.System").getMethod("long currentTimeMillis()");

        
        list.add(Jimple.v().newAssignStmt($l1,Jimple.v().newStaticInvokeExpr(time.makeRef())));
        

        //insert "$r5 = virtualinvoke $r3.<java.lang.StringBuilder: java.lang.StringBuilder append(long)>($l1)"  $r5 -> tmp5
        Local $r5 = Jimple.v().newLocal("start5", RefType.v("java.lang.StringBuilder"));
        body.getLocals().add($r5);
        SootMethod append = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.StringBuilder append(long)");

        
        list.add(Jimple.v().newAssignStmt($r5,Jimple.v().newVirtualInvokeExpr($r3, append.makeRef(), $l1)));
        
       //insert $r6 = virtualinvoke $r5.<java.lang.StringBuilder: java.lang.String toString()>()   $r6 -> tmp6
        Local $r6 = Jimple.v().newLocal("start6",RefType.v("java.lang.String"));
        body.getLocals().add($r6);
        SootMethod toString = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.String toString()");
        
        list.add(Jimple.v().newAssignStmt($r6, Jimple.v().newVirtualInvokeExpr($r5, toString.makeRef())));
        
        //insert virtualinvoke $r4.<java.io.PrintStream: void println(java.lang.String)>($r6)
        SootMethod println = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
        
        list.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr($r4, println.makeRef(), $r6)));
        
        
        
        units.insertBefore(list, target);
    }
    private void addTimeStampAfter(Body body,PatchingChain<Unit> units,Unit target,String act)
    {
    	
    	List<Unit> list = new ArrayList<>();
    	 	
		// insert "$r4 = java.lang.System.out;"   $r4 -> tmp4
        Local $r4 = Jimple.v().newLocal("end4", RefType.v("java.io.PrintStream"));
        body.getLocals().add($r4);

        list.add(Jimple.v().newAssignStmt($r4, Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef())));
       
        // insert "$r3 = new java.lang.StringBuilder" $r3 -> tmp3
        Local $r3 = Jimple.v().newLocal("end3", RefType.v("java.lang.StringBuilder"));
        body.getLocals().add($r3);

	        
        list.add(Jimple.v().newAssignStmt($r3, Jimple.v().newNewExpr(RefType.v("java.lang.StringBuilder"))));
        
       // insert " specialinvoke $r3.<java.lang.StringBuilder: void <init>(java.lang.String)>("Start") "
        
        SootMethod init = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("void <init>(java.lang.String)");
       
        list.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr($r3, init.makeRef(), StringConstant.v("End"+act+":"))));
        
       //insert "$l1 = staticinvoke <java.lang.System: long currentTimeMillis()>()"   $l1 -> tmp1
        Local $l1 = Jimple.v().newLocal("end1", LongType.v());
        body.getLocals().add($l1);
        
        SootMethod time = Scene.v().getSootClass("java.lang.System").getMethod("long currentTimeMillis()");

        
        list.add(Jimple.v().newAssignStmt($l1,Jimple.v().newStaticInvokeExpr(time.makeRef())));
        

        //insert "$r5 = virtualinvoke $r3.<java.lang.StringBuilder: java.lang.StringBuilder append(long)>($l1)"  $r5 -> tmp5
        Local $r5 = Jimple.v().newLocal("end5", RefType.v("java.lang.StringBuilder"));
        body.getLocals().add($r5);
        SootMethod append = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.StringBuilder append(long)");

        
        list.add(Jimple.v().newAssignStmt($r5,Jimple.v().newVirtualInvokeExpr($r3, append.makeRef(), $l1)));
        
       //insert $r6 = virtualinvoke $r5.<java.lang.StringBuilder: java.lang.String toString()>()   $r6 -> tmp6
        Local $r6 = Jimple.v().newLocal("end6",RefType.v("java.lang.String"));
        body.getLocals().add($r6);
        SootMethod toString = Scene.v().getSootClass("java.lang.StringBuilder").getMethod("java.lang.String toString()");
        
        list.add(Jimple.v().newAssignStmt($r6, Jimple.v().newVirtualInvokeExpr($r5, toString.makeRef())));
        
        //insert virtualinvoke $r4.<java.io.PrintStream: void println(java.lang.String)>($r6)
        SootMethod println = Scene.v().getSootClass("java.io.PrintStream").getMethod("void println(java.lang.String)");
        
        list.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr($r4, println.makeRef(), $r6)));
        
        
        
        units.insertAfter(list, target);
        
    }
    private static void addFieldToClass(String className,String fieldName)
    {
    	SootClass sc=Scene.v().loadClassAndSupport(className);
    	
    	//add a field
    	SootField id = new SootField(fieldName,RefType.v("android.database.sqlite.SQLiteDatabase"),Modifier.PRIVATE);
    	sc.addField(id);
    
    	//add initialization to the constructor
    
    }

}

