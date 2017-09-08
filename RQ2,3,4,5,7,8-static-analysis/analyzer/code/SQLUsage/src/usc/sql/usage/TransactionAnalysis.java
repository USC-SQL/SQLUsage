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

import soot.ResolutionFailedException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocalBox;
import soot.tagkit.BytecodeOffsetTag;
import soot.tagkit.Tag;
import usc.sql.global.GlobalFlowAnalysis;
import usc.sql.global.InterReachingDefinition;
import usc.sql.global.Pair;
import usc.sql.global.RegionAnalysis;
import usc.sql.region.RegionNode;
import CallGraph.EntryPoint;
import CallGraph.NewNode;
import SootEvironment.AndroidApp;
import edu.usc.sql.graphs.Node;
import edu.usc.sql.graphs.NodeInterface;
import edu.usc.sql.graphs.cfg.CFGInterface;
import edu.usc.sql.graphs.cfg.SootCFG;

public class TransactionAnalysis {
	
	public AndroidApp App;
	
	private Map<String,SootMethod> targetMethod = new HashMap<>();
	private Set<String> targetAPI = new HashSet<>();
	private Set<String> writeAPI = new HashSet<>();

	private Map<String,Integer> apiAllFreqInLoop = new HashMap<>();
	private Map<String,Integer> apiFreqInLoop = new HashMap<>();
	private Map<String,Integer> apiFreqInLoopInBegin = new HashMap<>();
	private Map<String,Integer> apiFreqInLoopNotInBegin = new HashMap<>();
	private Map<NodeInterface,String> modToRegion = new HashMap<>();
	private Map<NodeInterface,RegionNode> modToRegionNode = new HashMap<>();
	private Map<NodeInterface,String> modToSig = new HashMap<>();
	private Map<NodeInterface,Set<String>> modToDefine = new HashMap<>();
	private EntryPoint entryPoint = new EntryPoint();
	private int dbAllInLoop = 0;
	private int dbWriteInLoop = 0;
	private int dbWriteInLoopNotInBegin = 0;
	private int dbWriteInLoopInBegin = 0;
	private StringBuilder result= new StringBuilder();
	public TransactionAnalysis(AndroidApp App)
	{
		this.App = App;
		
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: long insert(java.lang.String,java.lang.String,android.content.ContentValues)>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: long insertOrThrow(java.lang.String,java.lang.String,android.content.ContentValues)>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: long insertWithOnConflict(java.lang.String,java.lang.String,android.content.ContentValues,int)>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: int update(java.lang.String,android.content.ContentValues,java.lang.String,java.lang.String[])>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: int updateWithOnConflict(java.lang.String,android.content.ContentValues,java.lang.String,java.lang.String[],int)>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String)>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: void execSQL(java.lang.String,java.lang.Object[])>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: int delete(java.lang.String,java.lang.String,java.lang.String[])>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: long replace(java.lang.String,java.lang.String,android.content.ContentValues)>");
		writeAPI.add("<android.database.sqlite.SQLiteDatabase: long replaceOrThrow(java.lang.String,java.lang.String,android.content.ContentValues)>");
		writeAPI.add("<android.database.sqlite.SQLiteStatement: void execute()>");
		writeAPI.add("<android.database.sqlite.SQLiteStatement: long executeInsert()>");
		writeAPI.add("<android.database.sqlite.SQLiteStatement: int executeUpdateDelete()>");
		
	
		analyze();
	}
	
	public void analyze()
	{
		Set<SootMethod> targetSootMethod = new HashSet<>();
		SootClass sc = Scene.v().getSootClass("android.database.sqlite.SQLiteDatabase");
		targetSootMethod.addAll(sc.getMethods());
		SootClass sc1 = Scene.v().getSootClass("android.database.sqlite.SQLiteOpenHelper");
		targetSootMethod.addAll(sc1.getMethods());
		SootClass sc2 = Scene.v().getSootClass("android.database.sqlite.SQLiteQueryBuilder");
		targetSootMethod.addAll(sc2.getMethods());
		SootClass sc3 = Scene.v().getSootClass("android.database.sqlite.SQLiteStatement");
		targetSootMethod.addAll(sc3.getMethods());
		
		for(SootMethod sm : targetSootMethod)
			targetAPI.add(sm.getSignature());
		for(NewNode n: App.getCallgraph().getRTOdering())
		{
			if(n.getMethod().isConcrete()&&!n.getMethod().getDeclaringClass().isAbstract())
			{
				boolean isRelevant = false;
				if(GlobalCallGraph.containLib(n.getMethod().getSignature()))
					continue;
				for(Unit actualNode:n.getMethod().retrieveActiveBody().getUnits())
				{
					Stmt stmt = (Stmt)actualNode;
					if(((Stmt)stmt).containsInvokeExpr())
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

						if(targetAPI.contains(sig)||targetMethod.containsKey(sig))
						{
							isRelevant = true;
							break;
						}
					}
				}
				if(isRelevant)
				{
					targetMethod.put(n.getMethod().getSignature(),n.getMethod());					
				}
			}
		}
		//CCG
		Map<NodeInterface,CFGInterface> preStoreCFG = new HashMap<>();
		//Nodes that invoke target APIs
		Set<NodeInterface> sqlNodes = new HashSet<>();

		
		List<CFGInterface> heads = new ArrayList<>();
		
		for(NewNode n: App.getCallgraph().getHeads())
		{
			if(!targetMethod.keySet().contains(n.getMethod().getSignature()))
				continue;
			CFGInterface cfg = new SootCFG(n.getMethod().getSignature(),n.getMethod());
			heads.add(cfg);
		}
	
		Stack<CFGInterface> processMethod = new Stack<>();
		processMethod.addAll(heads);
		while(!processMethod.isEmpty())
		{
			CFGInterface currentMethod = processMethod.pop();
			if(!targetMethod.keySet().contains(currentMethod.getSignature()))
				continue;

				for(NodeInterface cfgNode:currentMethod.getAllNodes())
				{
					Stmt actualNode = (Stmt)((Node)cfgNode).getActualNode();
					if(actualNode!=null&&actualNode.containsInvokeExpr())
					{
						
						SootMethod nodeMethod = null;
						try{
							nodeMethod =actualNode.getInvokeExpr().getMethod();
						}
						catch(ResolutionFailedException ex)
						{
							System.out.println("Soot fails to get the method:"+actualNode.getInvokeExpr());
						}	
						if(nodeMethod==null)
							continue;
	
						String sig= nodeMethod.getSignature();
						if(targetMethod.keySet().contains(sig))
						{
							
							if(!preStoreCFG.containsKey(cfgNode))
							{
								CFGInterface cfg = new SootCFG(sig,nodeMethod);
								preStoreCFG.put(cfgNode, cfg);
								processMethod.push(cfg);
							}
						}
						
						if(targetAPI.contains(sig))
						{
							if(!sig.contains("beginTransaction")&&!sig.contains("endTransaction")&&!sig.contains("setTransactionSuccessful"))
								sqlNodes.add(cfgNode);
						}
					}
				}
			

		}
		
		Map<NodeInterface, String> targetMapToBegin = new HashMap<>();
		
		Set<NodeInterface> targetInBegin = new HashSet<>();
		Set<NodeInterface> targetInBeginAlias = new HashSet<>();
		Set<NodeInterface> targetNotInBegin = new HashSet<>();
		for(CFGInterface head : heads)
		{	
			new RegionAnalysis(head,targetAPI,targetMethod.keySet(),null,null, modToRegion,modToRegionNode,targetMethod,preStoreCFG,modToSig);
			Set<NodeInterface> loopHeaders = new HashSet<>();
			for(NodeInterface target : modToRegion.keySet())
			{
				RegionNode rn = modToRegionNode.get(target);
				loopHeaders.add(rn.getBackEdge().getDestination());
			}
			GlobalFlowAnalysis gfa = new GlobalFlowAnalysis(head, sqlNodes, targetMethod.keySet(), preStoreCFG, null, modToSig,targetMapToBegin,modToDefine, modToRegionNode, loopHeaders );
			for(Pair<NodeInterface,Integer> p:gfa.getTargetNode())
			{
				if(p.getSecond()==0)
					targetNotInBegin.add(p.getFirst());
			}
		}

		List<CFGInterface> rtoCFG = new ArrayList<>();
		Map<String,CFGInterface> rtoCFGMap = new HashMap<>();
		InterReachingDefinition ird = null;
		
		for(NewNode n: App.getCallgraph().getRTOdering())
		{
			if(n.getMethod()!=null&&targetMethod.keySet().contains(n.getMethod().getSignature()))
			{
				CFGInterface cfg = new SootCFG(n.getMethod().getSignature(),n.getMethod());
				rtoCFG.add(cfg);
				rtoCFGMap.put(cfg.getSignature(), cfg);
	
			}
		}
		ird = new InterReachingDefinition(rtoCFG,rtoCFGMap);
		
		 
		Map<NodeInterface,Integer> targetInBeginToLength = new HashMap<>();
		for(NodeInterface target : targetMapToBegin.keySet())
		{
			boolean isInBegin = targetMapToBegin.get(target).contains("->");
			if(isInBegin)
			{
				String beginLocation = targetMapToBegin.get(target).split("->")[1];
				String beginSig = beginLocation.substring(beginLocation.indexOf("<"),beginLocation.lastIndexOf(">")+1);
				int offset = Integer.parseInt(beginLocation.split("@")[1]);
				
				boolean mayAlias = identifyAlias(ird, rtoCFGMap, modToSig, preStoreCFG, target, offset, beginSig);
				Stmt actualNode = (Stmt)((Node)target).getActualNode();
				String api = actualNode.getInvokeExpr().getMethod().getSignature();
				if(writeAPI.contains(api))
				{			
					targetInBegin.add(target);
					
					String[] paths = modToSig.get(target).split("@");
					int beginCallChain = 0;					
					for(int i = 1; i < paths.length; i++)
					{
						if(paths[i].contains(beginSig))
							break;
						beginCallChain++;
					}
					targetInBeginToLength.put(target, beginCallChain);
					if(mayAlias)
						targetInBeginAlias.add(target);
				}			
			}
						
		}

		
		//# of writes in loops, # writes in loops and in begin, and # writes in loops and not in begin 
		for(NodeInterface target : modToRegion.keySet())
		{
			Stmt actualNode = (Stmt)((Node)target).getActualNode();
			String api = actualNode.getInvokeExpr().getMethod().getSignature();
			
			dbAllInLoop++;
			int c = apiAllFreqInLoop.containsKey(api)? apiAllFreqInLoop.get(api) : 0;
			apiAllFreqInLoop.put(api, c+1);
			
			if(!writeAPI.contains(api))
				continue;
			

			
			//api@<MethodName>BytecodeOffset@LoopInUI:true/false
			//InBegin:true@LengthModToBegin@MayAlias:true/false  | InBegin:false
			//NotInBegin:true@LengthModToLoop@LengthModToEntry  | NotInBegin:false
			boolean canLoopInMainThread = mayInUIThread(modToRegionNode.get(target).getCFG().getSignature());
			result.append(api+"@LoopInUI:"+canLoopInMainThread+"\n");
			if(targetInBegin.contains(target))
			{
				int bytecodeOffset = -1;
				for (Tag t : actualNode.getTags()) {
					if (t instanceof BytecodeOffsetTag)
						bytecodeOffset = ((BytecodeOffsetTag) t).getBytecodeOffset();
				}
				String methodName = modToSig.get(target).substring(0,modToSig.get(target).indexOf(">")+1);
				
				//just in case modToSig starts with a constructor
				//e.g.,<com.andromo.dev21730.app21166.br: void <init>(android.content.Context)>-1,130
				if(methodName.contains("<init>"))
				{
					int first = modToSig.get(target).indexOf(">");
					methodName = modToSig.get(target).substring(0,modToSig.get(target).indexOf(">",first+1)+1);
				}
				int length = targetInBeginToLength.get(target);
				result.append("InBegin:true"+"@"+methodName+bytecodeOffset+"@LengthModToBegin:"+length+"@MayAlias:"+targetInBeginAlias.contains(target)+"\n");
			}
			else
				result.append("InBegin:false\n");
			if(targetNotInBegin.contains(target))
			{
				//alias analysis(not ready yet)
				//boolean isDefineInLoop = isDefine(ird, rtoCFGMap, modToSig, preStoreCFG, target);
				
				//flow analysis
				boolean mayDefineInLoop = false;
				
				boolean mayInitializeInLoop = false;
				//format: defineAPI@DefineInMethod@Line
				for(String format: modToDefine.get(target))
				{
					if(!format.equals("@ReachLoop"))
					{
						mayDefineInLoop = true;
						String[] items = format.split("@");
						if(items[0].contains("open"))
								mayInitializeInLoop = true;
							
						
					}
				}
				
				String loopSig = modToRegionNode.get(target).getCFG().getSignature();
				String[] paths = modToSig.get(target).split("@");
				int loopCallChain = 0;
				
				for(int i = 1; i < paths.length; i++)
				{
					if(paths[i].contains(loopSig))
						break;
					loopCallChain++;

				}
				int bytecodeOffset = -1;
				for (Tag t : actualNode.getTags()) {
					if (t instanceof BytecodeOffsetTag)
						bytecodeOffset = ((BytecodeOffsetTag) t).getBytecodeOffset();
				}
				String methodName = modToSig.get(target).substring(0,modToSig.get(target).indexOf(">")+1);
				
				//just in case modToSig starts with a constructor
				//e.g.,<com.andromo.dev21730.app21166.br: void <init>(android.content.Context)>-1,130
				if(methodName.contains("<init>"))
				{
					int first = modToSig.get(target).indexOf(">");
					methodName = modToSig.get(target).substring(0,modToSig.get(target).indexOf(">",first+1)+1);
				}
				result.append("NotInBegin:true"+"@"+methodName+bytecodeOffset+
						"@LengthModToLoop:"+loopCallChain+"@LengthModToEntry:"+(paths.length-1)+
						"@MayDefineInLoop:"+mayDefineInLoop+
						"@MayInitializeInLoop:"+mayInitializeInLoop+"\n");
	
			}
			else
				result.append("NotInBegin:false\n");
			
			int count = 0;
				
			dbWriteInLoop++;
			count = apiFreqInLoop.containsKey(api) ? apiFreqInLoop.get(api) : 0;
			apiFreqInLoop.put(api, count + 1);
			
			if(targetInBegin.contains(target))
			{
				count = apiFreqInLoopInBegin.containsKey(api) ? apiFreqInLoopInBegin.get(api) : 0;
				apiFreqInLoopInBegin.put(api, count + 1);				
				dbWriteInLoopInBegin++;
			}
			if(targetNotInBegin.contains(target))
			{
				count = apiFreqInLoopNotInBegin.containsKey(api) ? apiFreqInLoopNotInBegin.get(api) : 0;
				apiFreqInLoopNotInBegin.put(api, count + 1);
				dbWriteInLoopNotInBegin++;
			}				
					
			

		}
		
		/*
		//#database api that are in beginTransaction
		//#database api that are in loops
		for(NodeInterface target : targetInBegin.keySet())
		{
			dbAPIInBegin++;
			boolean isInBegin = targetInBegin.get(target).contains("->");
			if(isInBegin)
			{
				Stmt actualNode = (Stmt)((Node)target).getActualNode();
				String api = actualNode.getInvokeExpr().getMethod().getSignature();
				if(apiFreq.containsKey(api))
					apiFreq.put(api, apiFreq.get(api)+1);
				else
					apiFreq.put(api, 1);
				
				if(modToRegion.containsKey(target))
				{
					dbAPIInBeginAndLoop++;
					if(apiInLoopFreq.containsKey(api))
						apiInLoopFreq.put(api, apiInLoopFreq.get(api)+1);
					else
						apiInLoopFreq.put(api, 1);
				}				
			}
						
		}
		*/
		
		
	}
	public boolean mayInUIThread(String sig)
	{
		Set<String> entries = App.getCallgraph().getEntryMethod(sig);
		for(String entry:entries)
		{
			String className = entry.split(":")[0].replace("<", "");
			SootClass sc = Scene.v().getSootClass(className);
			String secondHalf = entry.split(":")[1];
			String methodName = secondHalf.substring(1,secondHalf.length()-1);
			if(entryPoint.isEntry(sc, methodName))
				return true;
		}
		return false;
		
	}
	public boolean identifyAlias(InterReachingDefinition ird,Map<String,CFGInterface> rtoCFGMap,
			Map<NodeInterface,String> modToSig,Map<NodeInterface,CFGInterface> preStoreCFG,
			NodeInterface mod, int beginOffset, String beginSig)
	{			
			String sig = modToSig.get(mod).substring(0,modToSig.get(mod).indexOf(">")+1);
			//just in case modToSig starts with a constructor
			//e.g.,<com.andromo.dev21730.app21166.br: void <init>(android.content.Context)>-1,130
			if(sig.contains("<init>"))
			{
				int first = modToSig.get(mod).indexOf(">");
				sig = modToSig.get(mod).substring(0,modToSig.get(mod).indexOf(">",first+1)+1);
			}
			//System.out.println(sig);
			NodeInterface rdModNode = rtoCFGMap.get(sig).getNodeFromOffset(mod.getOffset());
			Unit actualNode = (Unit) ((Node)rdModNode).getActualNode();
			//caller is the jimpleLocalBox of the UseBoxes
			String modVar = null;
			for(ValueBox v:actualNode.getUseBoxes())
			{
				if(v instanceof JimpleLocalBox)
					modVar = v.getValue().toString();
			}
			NodeInterface rdBeginNode = rtoCFGMap.get(beginSig).getNodeFromOffset(beginOffset);
			Unit actualNode1 = (Unit) ((Node)rdBeginNode).getActualNode();
			//caller is the jimpleLocalBox of the UseBoxes
			String beginVar = null;
			for(ValueBox v:actualNode1.getUseBoxes())
			{
				if(v instanceof JimpleLocalBox)
					beginVar = v.getValue().toString();
			}
			//System.out.println(modVar);
			
			
			
			List<String> modFieldDefined= ird.getFieldSig(sig, rdModNode, modVar);
			if(modVar.contains("<"))
				modFieldDefined.add(modVar);
			List<String> beginFieldDefined= ird.getFieldSig(beginSig, rdBeginNode, beginVar);
			if(beginVar.contains("<"))
				beginFieldDefined.add(beginVar);
			modFieldDefined.retainAll(beginFieldDefined);
			if(!modFieldDefined.isEmpty())
				return true;
			
			List<String> modDeclarePos = ird.getDeclarePosition(sig,rdModNode, modVar,modToSig.get(mod),beginSig);
			//System.out.println(modToSig.get(mod));
			//System.out.println("mod:"+modDeclarePos);
			List<String> beginDeclarePos = ird.getDeclarePosition(beginSig,rdBeginNode, beginVar,null,beginSig);
			//System.out.println("begin:"+beginDeclarePos);
			modDeclarePos.retainAll(beginDeclarePos);
			
			if(!modDeclarePos.isEmpty())
				return true;
			
			
			
			return false;
			



			
			//System.out.println(modToSig.get(mod)+"\n"+modToCat.get(mod));
		
	}
	public boolean isDefine(InterReachingDefinition ird,Map<String,CFGInterface> rtoCFGMap,
			Map<NodeInterface,String> modToSig,Map<NodeInterface,CFGInterface> preStoreCFG,
			NodeInterface mod)
	{
		String sig = modToSig.get(mod).substring(0,modToSig.get(mod).indexOf(">")+1);
		//just in case modToSig starts with a constructor
		//e.g.,<com.andromo.dev21730.app21166.br: void <init>(android.content.Context)>-1,130
		if(sig.contains("<init>"))
		{
			int first = modToSig.get(mod).indexOf(">");
			sig = modToSig.get(mod).substring(0,modToSig.get(mod).indexOf(">",first+1)+1);
		}
		
		
		NodeInterface rdModNode = rtoCFGMap.get(sig).getNodeFromOffset(mod.getOffset());
		Unit actualNode = (Unit) ((Node)rdModNode).getActualNode();
		//caller is the jimpleLocalBox of the UseBoxes
		String modVar = null;
		for(ValueBox v:actualNode.getUseBoxes())
		{
			if(v instanceof JimpleLocalBox)
				modVar = v.getValue().toString();
		}
		List<String> modFieldDefined= ird.getFieldSig(sig, rdModNode, modVar);
		
		String[] pathCp = modToSig.get(mod).split("@");
		boolean inSideLoop = false;
		//isDefined for fields
		for(String f:modFieldDefined)
		{
			//for each method on the call path from the msa method to the loop method


			for(int index = 1; index < pathCp.length;index++)
			{
				String currentSig = pathCp[index].substring(0,pathCp[index].lastIndexOf(">")+1);
			
				int offset = Integer.parseInt(pathCp[index].substring(pathCp[index].lastIndexOf(">")+1,pathCp[index].length()));
			
				//callee of the loop 
				if(!currentSig.equals(modToRegionNode.get(mod).getCFG().getSignature()))
				{
					for(Pair<String,String> defOffset:ird.getDefineLineNum(currentSig,rtoCFGMap.get(currentSig).getNodeFromOffset(offset), f, ""))
					{
						//exist initialization in the callee
						if(defOffset.getFirst().equals(currentSig))
							inSideLoop = true;
					}
				}
				//loop method
				else
				{					
					for(Pair<String,String> defOffset:ird.getDefineLineNum(currentSig,rtoCFGMap.get(currentSig).getNodeFromOffset(offset), f, ""))
					{
						if(defOffset.getFirst().equals(currentSig))
						{
							int i = Integer.parseInt(defOffset.getSecond());
							CFGInterface modCFG = null;
							for(CFGInterface temp:preStoreCFG.values())
								if(temp.getSignature().equals(defOffset.getFirst()))
								{	
									modCFG = temp;
									
									if(modToRegionNode.get(modCFG.getNodeFromOffset(i))!=null)
										inSideLoop = true;
								}
						}
					}
					//no need to analyze the parent of the loop method
					break;
				}

			}
		}
		//callee of the loop method including the loop method
		List<String> callee = new ArrayList<>();
		for(int index = 1; index < pathCp.length;index++)
		{
			String currentSig = pathCp[index].substring(0,pathCp[index].lastIndexOf(">")+1);
			callee.add(currentSig);
			if(currentSig.equals(modToRegionNode.get(mod).getCFG().getSignature()))
					break;
		}
		//isDefined for local variables
		for(Pair<String, String> defOffset:ird.getDefineLineNum(sig,rdModNode, modVar,modToSig.get(mod)))
		{
			if(callee.contains(defOffset.getFirst()))
			{
				//callee of the loop method
				if(!defOffset.getFirst().equals(modToRegionNode.get(mod).getCFG().getSignature()))
					inSideLoop = true;
				//loop method
				else
				{
					int i = Integer.parseInt(defOffset.getSecond());
					CFGInterface modCFG = null;
					for(CFGInterface temp:preStoreCFG.values())
						if(temp.getSignature().equals(defOffset.getFirst()))
						{	
							modCFG = temp;
							
							if(modToRegionNode.get(modCFG.getNodeFromOffset(i))!=null)
								inSideLoop = true;
						}
				}
			}
		}
		return inSideLoop;
	}
	
	public boolean mayDefine(String sig, int offset,NodeInterface mod,Map<NodeInterface,CFGInterface> preStoreCFG)
	{
		String loopSig = modToRegionNode.get(mod).getCFG().getSignature();
		String[] pathCp = modToSig.get(mod).split("@");
		//callee of the loop method
		List<String> callee = new ArrayList<>();
		for(int index = 1; index < pathCp.length;index++)
		{
			String currentSig = pathCp[index].substring(0,pathCp[index].lastIndexOf(">")+1);
			if(currentSig.equals(loopSig))
				break;		
			callee.add(currentSig);
	
		}
		if(callee.contains(sig))
			return true;
		else if(sig.equals(loopSig))
		{
			CFGInterface modCFG = null;
			for(CFGInterface temp:preStoreCFG.values())
				if(temp.getSignature().equals(sig))
				{	
					modCFG = temp;
					
					if(modToRegionNode.get(modCFG.getNodeFromOffset(offset))!=null)
						return true;
				}
			return false;
		}
		else
			return false;
	}
	public void printResult(String appName, String outputPath)
	{
		StringBuilder result = new StringBuilder();
		result.append(appName + "\n");
		result.append(this.result);
		for(String api: apiAllFreqInLoop.keySet())
			result.append("In loop all@"+api+"@"+apiAllFreqInLoop.get(api)+"\n");
		for(String api: apiFreqInLoop.keySet())
			result.append("In loop write@"+api+"@"+apiFreqInLoop.get(api) + "\n");
		for(String api: apiFreqInLoopInBegin.keySet())
			result.append("In loop write in begin@"+api+"@"+apiFreqInLoopInBegin.get(api) + "\n");
		for(String api: apiFreqInLoopNotInBegin.keySet())
			result.append("In loop write not in begin@"+api+"@"+apiFreqInLoopNotInBegin.get(api) + "\n");
				
		result.append("# of db invocations in loops:" + dbAllInLoop + "\n");
		result.append("# of writes in loops:" + dbWriteInLoop + "\n");
		result.append("# writes in loops and in begin:" + dbWriteInLoopInBegin + "\n");
		result.append("# writes in loops and not in begin:" + dbWriteInLoopNotInBegin + "\n");
		

	    try {
				PrintWriter pw = new PrintWriter(new FileWriter(outputPath, true));
				pw.println(result);
				pw.close();
	    }
	    catch(IOException e)
	    {
	    	
	    }
	}

}
