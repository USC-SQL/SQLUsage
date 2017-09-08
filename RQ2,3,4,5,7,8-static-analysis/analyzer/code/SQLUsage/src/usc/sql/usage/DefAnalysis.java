package usc.sql.usage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import soot.Value;
import soot.ValueBox;
import soot.jimple.Constant;
import soot.jimple.InvokeStmt;
import soot.jimple.Stmt;
import soot.jimple.internal.JimpleLocalBox;
import usc.sql.string.ReachingDefinition;
import edu.usc.sql.graphs.Node;
import edu.usc.sql.graphs.NodeInterface;
import edu.usc.sql.graphs.cfg.CFGInterface;

public class DefAnalysis {

	private CFGInterface cfg;
	private List<NodeInterface> nodeListInOrder;
	private Set<String> targetSignature;
	private ReachingDefinition rd;
	public DefAnalysis(CFGInterface cfg, ReachingDefinition rd, Set<String> targetSignature)
	{
		this.cfg = cfg;
		this.rd = rd;
		this.targetSignature = targetSignature;
	}
	
	public void findTargetVariable(String targetType)
	{
		Set<String> targetCVDefine = new HashSet<>();
		
		//find the nodes that match target signatures and get where the variable matching the target type is defined
		for(NodeInterface n:cfg.getAllNodes())
		{
			Stmt actualNode = (Stmt) ((Node)n).getActualNode();	
			
			
			if(actualNode!=null)
			{

				if(actualNode.containsInvokeExpr())
				{
					String signature = actualNode.getInvokeExpr().getMethod().getSignature();
					if(targetSignature.contains(signature))
					{
						for(Value para: actualNode.getInvokeExpr().getArgs())
						{
							if(para.getType().toString().equals(targetType))
							{
								String varName = para.toString();
								//System.out.println("Target Found:" + actualNode + " "+  varName);
								targetCVDefine.addAll(rd.getLineNumForUse(n, varName));
														
							}
								
						}
					}
				}
			}
			
		}
		
		//find what is put in the methods 
		for(NodeInterface n:cfg.getAllNodes())
		{
			Stmt actualNode = (Stmt) ((Node)n).getActualNode();	
			
			
			if(actualNode!=null)
			{

				if(actualNode.containsInvokeExpr())
				{
					String signature = actualNode.getInvokeExpr().getMethod().getSignature();

					
					if(signature.contains("<android.content.ContentValues: void put"))
					{
						Value invoker = null;
						for(ValueBox vb : actualNode.getInvokeExpr().getUseBoxes())
						{
							if(vb instanceof JimpleLocalBox)
								invoker = vb.getValue();
						}
						boolean checkRD = false;
						if(invoker !=null)
						{
							for(String targetDefLine:targetCVDefine)
							{
								if(rd.getInSet(n).keySet().contains(targetDefLine))
									checkRD = true;
								
							}
							
						}
						if(checkRD)
						{
							Value target =actualNode.getInvokeExpr().getArg(1);
							Map<NodeInterface, String> NodeToVarName = new HashMap<>();
							NodeToVarName.put(n, target.toString());
							System.out.println("Target: "+ n.getOffset() + " "+ target + " "+actualNode);
							for(String line : rd.getUltimateLineNumForUse(cfg, NodeToVarName))
							{
								if(line.contains("-"))
									System.out.println("Parameter:"+line);
								else
								{
									NodeInterface def = cfg.getNodeFromOffset(Integer.parseInt(line));
									Stmt actualDef = (Stmt) ((Node)def).getActualNode();
									if(actualDef.containsInvokeExpr())
										System.out.println("Method:"+actualDef.getInvokeExpr().getMethod().getSignature());
									else
										System.out.println("Other:"+actualDef);
									
								}
								
								
							}
							System.out.println();
						}
						//if(target instanceof Constant)
							//
						
					}
				}
			}
		}
		

	}
}
