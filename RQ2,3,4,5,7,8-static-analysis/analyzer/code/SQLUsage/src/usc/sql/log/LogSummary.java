package usc.sql.log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.management.Query;

import usc.sql.usage.QueryParser;

public class LogSummary {

	
	public static void main(String[] args)
	{
		if(args.length != 2)
			System.out.println("Require two arguments, RQ number and input file");
		else
		{
			String rq = args[0].toLowerCase();
			if(rq.equals("rq2"))
				analyzeAPIFreq(args[1]);
			else if(rq.equals("rq3-5"))
				analyzeString(args[1]);
			else if(rq.equals("rq7"))
				analyzeAPIInLoopFreq(args[1]);
			else if(rq.equals("rq8"))
				analyzeTransaction(args[1]);
			
		}
		
		//analyzeAPIInLoopFreq();
 		//analyzeString();
		//parseStringFile();
 		//analyzeTransaction();
		//rankAppForDynamicAnalysis();
	}
	public static Set<String> source = new HashSet<String>();
	
	static Map<String,String> sourceToCata = new HashMap<>();
	static String[] prefixes = {"create","drop","insert","update","replace","delete","select","alter",
			"pragma","vacuum","end","commit"};
    static Map<String,Set<String>> idToTypes = new HashMap<>();

	public static void analyzeString(String filePath)
	{
		 try{
		        BufferedReader br = null;
		        String line;
		        br = new BufferedReader(new FileReader("susi_source.txt"));
		        while ((line = br.readLine()) != null) {
		        	String s = line.split(">")[0]+">";
		        	if(s.contains("<"))
		        	{
		        		source.add(s);
		        		
		        		int s1 = line.lastIndexOf("(");
		        		int s2 = line.lastIndexOf(")");
		        		String cata = line.substring(s1+1,s2);
		        		sourceToCata.put(s, cata);
		        		//System.out.println(cata);
		        		//sourceToCata.
		        	}
		        }
		        br.close();
		  }
	 
        catch(IOException ex)
        {
        	
        }

		
        BufferedReader br = null;
        String line;
        int total = 0;
        int no = 0;
        int yes = 0;
        
        int unknownMethod = 0,unknownField = 0, unknownPara = 0;
        Map<String,Integer> queryFrequency = new HashMap<>();
        Map<String,Integer> compileFrequency = new HashMap<>();
        Map<String,Integer> execTotalFrequency = new HashMap<>();
        Map<String,Integer> execFrequency = new HashMap<>();
        Map<String,Integer> execOverFrequency = new HashMap<>();
        Map<String,Integer> rawFrequency = new HashMap<>();
        
        Map<String,Integer> queryQuestionFrequency = new HashMap<>();
        Map<String,Integer> compileQuestionFrequency = new HashMap<>();
        Map<String,Integer> execQuestionFrequency = new HashMap<>();
        Map<String,Integer> execOverQuestionFrequency = new HashMap<>();
        Map<String,Integer> rawQuestionFrequency = new HashMap<>();
        
        
        Map<String,Integer> RQAParameterizableFrequency = new HashMap<>();
        Map<String,Integer> RQAHardCodedFrequency = new HashMap<>();
        //Map<String,Integer> queryConcatUnknownFrequency = new HashMap<>();
        Map<String,Integer> queryTaintFrequency = new HashMap<>();
        Map<String,Integer> compileTaintFrequency = new HashMap<>();
        Map<String,Integer> execTaintFrequency = new HashMap<>();
        Map<String,Integer> execOverTaintFrequency = new HashMap<>();
        Map<String,Integer> rawTaintFrequency = new HashMap<>();
        
        Map<String,Integer> taintedAPIFrequency = new HashMap<>();
        Map<String,Integer> RQAFixableFrequency = new HashMap<>();
        Map<Integer,Integer> queryNum = new HashMap<>();
        List<String> unknownMethods = new ArrayList<>();
        int parameterizedTaintStatement = 0;
        for(String pre: prefixes)
        {
        	queryFrequency.put(pre, 0);
        	compileFrequency.put(pre, 0);
        	rawFrequency.put(pre, 0);
        	execTotalFrequency.put(pre, 0);
        	execFrequency.put(pre, 0);
        	execOverFrequency.put(pre, 0);
        	queryQuestionFrequency.put(pre, 0);
        	compileQuestionFrequency.put(pre, 0);
        	rawQuestionFrequency.put(pre, 0);
        	execQuestionFrequency.put(pre, 0);
        	execOverQuestionFrequency.put(pre, 0);
        	
        	//queryConcatUnknownFrequency.put(pre, 0);
        	
        	queryTaintFrequency.put(pre, 0);
        	compileTaintFrequency.put(pre, 0);
        	rawTaintFrequency.put(pre, 0);
        	execTaintFrequency.put(pre, 0);
        	execOverTaintFrequency.put(pre, 0);
        }
        //the entire query is from a tainted source
        queryTaintFrequency.put("whole", 0);
        queryTaintFrequency.put("unknown", 0);
        
        try{
	        br = new BufferedReader(new FileReader(filePath));
	        String ir = null;
	        String jimple = null;
	        String appName = null;
	        String operation = null;
	        boolean containOp = false;
	        int numOfNonQuestion = 0;
	        int numOfNonQuestionOneOp = 0; 
	        int numOfNonQuestionOneOpNonDDL = 0;
	        int numOfQuestion = 0;
	        int numOfQuestionOneOp = 0; 
	        Set<String> violateSingleApp = new HashSet<>();
	        Set<String> taintedApp  = new HashSet<>();
	        Set<String> selectInExecSQLApp = new HashSet<>();
	        Set<String> writeInExecSQLApp = new HashSet<>();
	        Set<String> parameterizeApp = new HashSet<>();
	        int violateSingle = 0;
	        int violateSingleDiscard = -3;
	        int violateSingleImportant = 0;
	        Map<String,Integer> opFreq = new HashMap<>();
	        while ((line = br.readLine()) != null) {
	        	if(line.startsWith("IR:"))
	        	{
	        		ir = line;
	        		opFreq.clear();
	        		if(line.startsWith("IR:\"!!!"))
	        		{
	        			//unknownMethod++;
	        			//System.out.println(line);	        			
	        		}
	        			
	        	}
	        	else if(line.startsWith("/App"))
	        		appName = line;
	        	else if(line.startsWith("Jimple:"))
	        	{
	        		jimple = line;
	        	}
	        	else if(line.startsWith("Operations:"))
	        	{
	        		operation = line;
	        		if(!line.startsWith("Operations:{}"))
	        		{
	        			containOp = true;
	        			int i1 = line.indexOf("{")+1;
	        			int i2 = line.indexOf("}");
	        			String[] ops = line.substring(i1,i2).split(", ");
	        			for(String opToFreq : ops)
	        			{
	        				
	        				String op = opToFreq.split("=")[0].trim();
	        				int freq = Integer.parseInt(opToFreq.split("=")[1].trim());
	        				opFreq.put(op, freq);	        				
	        			}	        			
	        		}
	        		else
	        			containOp = false;
	        	}
	        	else if(line.startsWith("Value"))
	        	{
	        		total++;
	        		String value = line.substring(line.indexOf(":")+1);
	        		//if(value.trim().equals(""))
	        		//	continue;
	        		String[] querys = null;
	        		if(value.replaceAll("'.*'", "").contains(";"))
	        			querys = value.split(";");
	        		else
	        		{
	        			querys = new String[1];
	        			querys[0] = value;
	        		}
	        		if(querys.length>1)
	        		{

	        			violateSingleApp.add(appName);
	        			violateSingle++; 
	        			if(value.toLowerCase().trim().contains("; end")
	        					||value.toLowerCase().trim().contains(";end")||value.toLowerCase().trim().contains(";commit")
	        					||value.toLowerCase().contains("end;")||value.toLowerCase().endsWith(";\\n"))
	        				violateSingleDiscard++;
	        			
	        			if(!value.toUpperCase().startsWith("CREATE"))
	        			{
	        				//System.out.println(value);
	        				//System.out.println(appName);
	        				//if(value.startsWith("insert"))
	        				//	System.out.println(querys.length);
	        			}
	        		
	        		}
	        		//if(jimple.contains("<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteStatement compileStatement(java.lang.String)>"))
	        		//	System.out.println(value);
	        		if(queryNum.containsKey(querys.length))
	        			queryNum.put(querys.length, queryNum.get(querys.length)+1);
	        		else
	        			queryNum.put(querys.length, 1);
	        		boolean canParse = false;

	        		boolean useQuestionMark = false;
	        		//for(String query:querys)
	        		//{
	        			String query = null;
	        			if(!value.equals(";"))
	        				query = querys[0];
	        			else
	        				query = value;
	        			String originalQuery = query;
	        			
	        			//if(query.trim().toLowerCase().contains("insert")&&query.trim().toLowerCase().contains("select"))
	        			//	System.out.println(query);
		        		for(String prefix:prefixes)
		        		{	        			
		        			query = query.trim().toLowerCase();
		        			//while(query.startsWith("null"))
		        			//	query = query.replaceFirst("null", "");
		        			if(query.startsWith(prefix))
		        			{

		        				if((query.startsWith("insert")||query.startsWith("update")
	        							||query.startsWith("delete")||query.startsWith("replace"))&&jimple.contains("execSQL"))
		        				{
		        					writeInExecSQLApp.add(appName);
		        				}
		        				if(query.startsWith("select")&&jimple.contains("execSQL"))
		        				{
		        					//System.out.println(appName);
		        					//System.out.println("Got you:"+query);
		        					selectInExecSQLApp.add(appName);
		        				}
		        				canParse = true;
		        				queryFrequency.put(prefix, queryFrequency.get(prefix)+1);
		        				if(jimple.contains("rawQuery"))
		        					rawFrequency.put(prefix, rawFrequency.get(prefix)+1);
		        				else if(jimple.contains("execSQL(java.lang.String)"))
		        				{	
		        					execFrequency.put(prefix, execFrequency.get(prefix)+1);
		        					execTotalFrequency.put(prefix, execTotalFrequency.get(prefix)+1);
		        				}
		        				else if(jimple.contains("execSQL(java.lang.String,java.lang.Object[])"))
		        				{
		        					execOverFrequency.put(prefix, execOverFrequency.get(prefix)+1);
		        					execTotalFrequency.put(prefix, execTotalFrequency.get(prefix)+1);
		        				}
		        				else if(jimple.contains("compile"))
		        					compileFrequency.put(prefix, compileFrequency.get(prefix)+1);
		        				
		        				
		        				
		        				if(query.replaceAll("'.*'", "").contains("?"))
		        				{
		        					numOfQuestion++;
		        					if(operation.contains("append"))
		        						numOfQuestionOneOp++;
		        					//System.out.println(query);
		        					//System.out.println(opFreq);
		        					useQuestionMark = true;
		        					queryQuestionFrequency.put(prefix, queryQuestionFrequency.get(prefix)+1);
			        				if(jimple.contains("rawQuery"))
			        					rawQuestionFrequency.put(prefix, rawQuestionFrequency.get(prefix)+1);
			        				else if(jimple.contains("execSQL(java.lang.String)"))
			        				{
			        					execQuestionFrequency.put(prefix, execQuestionFrequency.get(prefix)+1);
			        				}
			        				else if(jimple.contains("execSQL(java.lang.String,java.lang.Object[])"))
			        					execOverQuestionFrequency.put(prefix, execOverQuestionFrequency.get(prefix)+1);
			        				else if(jimple.contains("compile"))
			        					compileQuestionFrequency.put(prefix, compileQuestionFrequency.get(prefix)+1);
		        				}
		        				else
		        				{
		        					numOfNonQuestion++;
		        					if(!operation.startsWith("Operations:{}"))
		        					//if(operation.contains("append"))
		        						numOfNonQuestionOneOp++;
		        				}
		        				if(!operation.startsWith("Operations:{}"))
		        				{
		        					if(query.startsWith("select")||query.startsWith("insert")||query.startsWith("update")
		        							||query.startsWith("delete")||query.startsWith("replace"))
		        						numOfNonQuestionOneOpNonDDL++;
		        				}
		        				//if(query.contains("unknown@"))
		        				//	queryConcatUnknownFrequency.put(prefix, queryConcatUnknownFrequency.get(prefix)+1);
		        				boolean tainted = false;

	        					String subQuery = originalQuery;
	        					while(subQuery.contains("Unknown@METHOD@"))
	        					{
	        						int index = subQuery.indexOf("Unknown@METHOD@");
	        						subQuery = subQuery.substring(index+1);
	        						int a1 = subQuery.indexOf("<");
	        						int a2 = subQuery.indexOf(">");
	        						
	        						String api = subQuery.substring(a1,a2+1);
	        						//System.out.println(api);
	        						if(source.contains(api))
	        						{	 
	        							tainted = true;
	        							//if(api.contains("net"))     								
	        							//	System.out.println(value);
	        							int taintedCount = taintedAPIFrequency.containsKey(api)? taintedAPIFrequency.get(api):0;
	        							taintedAPIFrequency.put(api, taintedCount+1);
	        						}
	        						unknownMethods.add(api);
	        					}
	        					
		        				if(tainted)
		        				{
		        					queryTaintFrequency.put(prefix,queryTaintFrequency.get(prefix)+1);
			        				if(jimple.contains("rawQuery"))
			        					rawTaintFrequency.put(prefix, rawTaintFrequency.get(prefix)+1);
			        				else if(jimple.contains("execSQL(java.lang.String)"))
			        					execTaintFrequency.put(prefix, execTaintFrequency.get(prefix)+1);
			        				else if(jimple.contains("execSQL(java.lang.String,java.lang.Object[])"))
			        					execOverTaintFrequency.put(prefix, execOverTaintFrequency.get(prefix)+1);
			        				else if(jimple.contains("compile"))
			        					compileTaintFrequency.put(prefix, compileTaintFrequency.get(prefix)+1);
			        				
			        				taintedApp.add(appName);
			        				if(QueryParser.parseTaintSQL(query))
			        				{
			        					//System.out.println(appName+"\n"+ir+"\n"+query+"\n");
			        					String rqa = null;
				        				if(jimple.contains("rawQuery"))
				        					rqa = "rawQuery";
				        				else if(jimple.contains("execSQL(java.lang.String)"))
				        					rqa = "execSQL(java.lang.String)";
				        				else if(jimple.contains("execSQL(java.lang.String,java.lang.Object[])"))
				        					rqa = "execSQL(java.lang.String,java.lang.Object[])";
				        				else if(jimple.contains("compile"))
				        					rqa = "compileStatement";
				        				int count = RQAFixableFrequency.containsKey(rqa)? RQAFixableFrequency.get(rqa):0;
				        				RQAFixableFrequency.put(rqa, count+1);
			        				}
			        				else
			        				{
			        					//if(value.contains("replace"))
			        					//System.out.println(value);
			        				}
			        					
		        				
		        				}
        						if(tainted&&query.replaceAll("'.*'", "").contains("?"))
        						{
        						////	System.out.println("App:"+appName);
        						//	System.out.println("Jimple"+jimple);
        						//	System.out.println("WOW:"+query);

        						}
		        				//if(tainted&&useQuestionMark)
		        				//	System.out.println("YOU ARE FOOL"+query);
		        			}

		        		}
		        		if(!canParse&&!originalQuery.startsWith("Unknown@METHOD")&&originalQuery.contains("Unknown@METHOD"))
		        		{
        					String subQuery = originalQuery;
        					boolean tainted = false;
        					while(subQuery.contains("Unknown@METHOD@"))
        					{
        						int index = subQuery.indexOf("Unknown@METHOD@");
        						subQuery = subQuery.substring(index+1);
        						int a1 = subQuery.indexOf("<");
        						int a2 = subQuery.indexOf(">");
        						
        						String api = subQuery.substring(a1,a2+1);
        						//System.out.println(api);
        						if(source.contains(api))
        						{	 
        							tainted = true;
        							int taintedCount = taintedAPIFrequency.containsKey(api)? taintedAPIFrequency.get(api):0;
        							taintedAPIFrequency.put(api, taintedCount+1);
        						}
        						unknownMethods.add(api);
        					}
        					if(tainted)
        					{
        						taintedApp.add(appName);
        						queryTaintFrequency.put("unknown",queryTaintFrequency.get("unknown")+1);
        						if(query.replaceAll("'.*'", "").contains("?"))
        						{
        							//System.out.println("WOW:"+query);
        							//System.out.println("App:"+appName);
        						}
        					}
		        			//System.out.println(originalQuery);
		        		}
	        		//}
	        		if(canParse)
	        		{
	        			yes++;
        				boolean parameterize = QueryParser.parseSQL(query);
        				if(parameterize)
        					parameterizeApp.add(appName);
        				
	        			//not a parameterized query
	        			if(!useQuestionMark)
	        			{
	        				String rqa = null;
	        				if(jimple.contains("rawQuery"))
	        					rqa = "rawQuery";
	        				else if(jimple.contains("execSQL(java.lang.String)"))
	        					rqa = "execSQL(java.lang.String)";
	        				else if(jimple.contains("execSQL(java.lang.String,java.lang.Object[])"))
	        					rqa = "execSQL(java.lang.String,java.lang.Object[])";
	        				else if(jimple.contains("compileStatement"))
	        					rqa = "compileStatement";
	        				//1. hard coded string
	        				if(operation.startsWith("Operations:{}"))
	        				{	
	        					int count = RQAHardCodedFrequency.containsKey(rqa)? RQAHardCodedFrequency.get(rqa):0;	        					
	        					RQAHardCodedFrequency.put(rqa, count+1);	        				
	        				}
	        				//2. not a hard coded string, parameterizable 
	        				else
	        				{
	        					if(parameterize)
	        					{
	        						int count = RQAParameterizableFrequency.containsKey(rqa)? RQAParameterizableFrequency.get(rqa):0;
	        						RQAParameterizableFrequency.put(rqa, count+1);
	        					}
	        					
	        				}

	        			}
	        			
	        	
	        		}
	        		
	        		else
	        		{
	        			//if(!value.startsWith("Unknown@FIELD"))
	        			no++;

	        			if(value.startsWith("Unknown@METHOD"))
	        			{
	        				unknownMethod++;
    						int a1 = value.indexOf("<");
    						int a2 =value.indexOf(">");
    						String api = value.substring(a1,a2+1);
    						if(source.contains(api))
    						{
    							//System.out.println(api);
    							queryTaintFrequency.put("whole",queryTaintFrequency.get("whole")+1);
    						}
    						unknownMethods.add(api);
    							
	        			}
	        			else if(value.startsWith("Unknown@FIELD"))
	        			{
	        				unknownField++;
	        			}
	        			else if(value.startsWith("Unknown@PARA"))
	        				unknownPara++;
	        			else
	        			{
	        				//System.out.println(value);
	        			}
	        		}
	        	}

	        }
	        
	        //System.out.println(queryNum+" Single Statement:");
	        //System.out.println("Total");
	        //System.out.println(queryFrequency);
	        System.out.println("--------------RQ 3---------------");
	        System.out.println("Percentage of queries that can be parsed:"+yes +"/"+total+"=" + Math.rint(yes*100.0/total)+"%");
	        System.out.println("Number of different types of queries:");
	        System.out.println("API rawQuery:"+rawFrequency);
	        //System.out.println(rawFrequency);
	        //printToExcel(rawFrequency);
	        System.out.println("SELECT query percentage:"+rawFrequency.get("select")*100/sum((Collection<Integer>) rawFrequency.values())+"%");
	        
	        //System.out.println("execSQL(java.lang.String)");
	        //System.out.println(execFrequency);
	        //printToExcel(execFrequency);
	        //System.out.println("execSQL(java.lang.String,java.lang.Object[])");
	        //System.out.println(execOverFrequency);
	        //printToExcel(execOverFrequency);
	        
	        System.out.println("\nAPI execSQL:"+execTotalFrequency);
	        System.out.print("Query percentage (Fig. 1):");
	        for(String type : execTotalFrequency.keySet())
	        {
	        	System.out.print(type.toUpperCase()+":"+ Math.rint(execTotalFrequency.get(type)*100.0/sum((Collection<Integer>) execTotalFrequency.values()))+"% ");
	        }
	        System.out.println();
	        int DMLFreq = execFrequency.get("insert")+execFrequency.get("update")+execFrequency.get("delete")+execFrequency.get("replace");
	        DMLFreq += execOverFrequency.get("insert")+execOverFrequency.get("update")+execOverFrequency.get("delete")+execOverFrequency.get("replace");
	        //System.out.println("DML percentage:"+DMLFreq*100/(sum((Collection<Integer>) execFrequency.values())+sum((Collection<Integer>) execOverFrequency.values()))+"%");
	        //System.out.println("DML in execSQL number of apps:"+writeInExecSQLApp.size());
	        
	        System.out.println("\nAPI compileStatement:"+compileFrequency);
	        System.out.println("Major query percentage:" + 
	        "INSERT:"+Math.rint(compileFrequency.get("insert")*100.0/(sum((Collection<Integer>) compileFrequency.values())))+"% "+
	        "SELECT:"+Math.rint(compileFrequency.get("select")*100.0/(sum((Collection<Integer>) compileFrequency.values())))+"% "+
	        "DELETE:"+Math.rint(compileFrequency.get("delete")*100.0/(sum((Collection<Integer>) compileFrequency.values())))+"% "+
	        "UPDATE:"+Math.rint(compileFrequency.get("update")*100.0/(sum((Collection<Integer>) compileFrequency.values())))+"% ");
	        	//	compileFrequency.get("select")++compileFrequency.get("update")+compileFrequency.get("delete"););
	        int majorFreq = compileFrequency.get("select")+compileFrequency.get("insert")+compileFrequency.get("update")+compileFrequency.get("delete");
	        //System.out.println("Major percentage:"+majorFreq*100/(sum((Collection<Integer>) compileFrequency.values()))+"%\n");
	        //printToExcel(compileFrequency);
	        System.out.println("\nString values represent multiple queries appear in "+violateSingleApp.size() +" apps and "+violateSingle+" instances");
	        System.out.println("Percentage of instances whose semantics could be safely discarded:"+violateSingleDiscard*100/violateSingle+"%");
	        
	 	    //this part is calculated manually
	        System.out.println("New rank in decending order: SELECT, INSERT, getWritableDatabase, endTransaction, CREATE, close, DELETE, UPDATE, getReadableDatabase, DROP");
	        
	        System.out.print("\nViolation of the first best practice (INSERT, UPDATE, DELTE query percentage in total):");
	        System.out.println( (execTotalFrequency.get("insert")+execTotalFrequency.get("update")+execTotalFrequency.get("delete"))*100/sum((Collection<Integer>) execTotalFrequency.values()) +"%");
	        System.out.println("\nViolation of the second best practice (Number of instances and percentage): "
	        +execTotalFrequency.get("select")+ " instances " + execTotalFrequency.get("select")*100.0/ yes +"%");
	        
	        System.out.println("\nViolation of the third best practice: "+violateSingleApp.size() +" apps and "+violateSingle+" instances\n");
	        
	        System.out.println("Percentage of instances whose semantics could be safely discarded:"+violateSingleDiscard*100/violateSingle+"%");
	        
	        
	        System.out.println();
	        System.out.println("--------------RQ 4---------------");
	        //System.out.println("concat ?");
	        System.out.println("Fig. 2");
	        String rqa = "rawQuery";
	        //System.out.println("Total Parameterized Unparameterized-hard-coded Unparameterized-non-hard-coded-parameterizable");
	        int apiTotal = sum((Collection<Integer>) rawFrequency.values());
	        int parameterized = sum((Collection<Integer>) rawQuestionFrequency.values());
	        int hard = RQAHardCodedFrequency.get(rqa);
	        int parameterizable = RQAParameterizableFrequency.get(rqa);
	        System.out.println("API " + rqa+": Parameterized:"+ parameterized*100/apiTotal +"%"
	        		+" Hard Coded:" + hard*100/apiTotal+"%" 
	        		 +" Parameterizable:"+parameterizable*100/apiTotal+"%"
	        		 +" Others:"+ (apiTotal-parameterized-hard-parameterizable)*100/apiTotal+"%");
	        
	        rqa = "execSQL(java.lang.String)";
	        apiTotal = sum((Collection<Integer>) execFrequency.values());
	        parameterized = sum((Collection<Integer>) execQuestionFrequency.values());
	        hard = RQAHardCodedFrequency.get(rqa);
	        parameterizable = RQAParameterizableFrequency.get(rqa);
	        System.out.println("API execSQL_UnPara: Parameterized:"+ parameterized*100/apiTotal +"%"
	        		+" Hard Coded:" + hard*100/apiTotal+"%" 
	        		 +" Parameterizable:"+parameterizable*100/apiTotal+"%"
	        		 +" Others:"+ (apiTotal-parameterized-hard-parameterizable)*100/apiTotal+"%");
	        
	        rqa = "execSQL(java.lang.String,java.lang.Object[])";
	        apiTotal = sum((Collection<Integer>) execOverFrequency.values());
	        parameterized = sum((Collection<Integer>) execOverQuestionFrequency.values());
	        if(RQAHardCodedFrequency.get(rqa)==null)
	        	hard = 0;
	        else
	        	hard = RQAHardCodedFrequency.get(rqa);
	        if(RQAParameterizableFrequency.get(rqa)==null)
	        	parameterizable = 0;
	        else
	        	parameterizable = RQAParameterizableFrequency.get(rqa);
	        System.out.println("API execSQL_Para: Parameterized:"+ parameterized*100/apiTotal +"%"
	        		+" Hard Coded:" + hard*100/apiTotal+"%" 
	        		 +" Parameterizable:"+parameterizable*100/apiTotal+"%"
	        		 +" Others:"+ (apiTotal-parameterized-hard-parameterizable)*100/apiTotal+"%");
	        
	        rqa = "compileStatement";
	        apiTotal = sum((Collection<Integer>) compileFrequency.values());
	        parameterized = sum((Collection<Integer>) compileQuestionFrequency.values());
	        hard = RQAHardCodedFrequency.get(rqa);
	        parameterizable = RQAParameterizableFrequency.get(rqa);
	        System.out.println("API compileStatament: Parameterized:"+ parameterized*100/apiTotal +"%"
	        		+" Hard Coded:" + hard*100/apiTotal+"%" 
	        		 +" Parameterizable:"+parameterizable*100/apiTotal+"%"
	        		 +" Others:"+ (apiTotal-parameterized-hard-parameterizable)*100/apiTotal+"%");
	        
	        /*
	        System.out.println("break down by types");
	        
	        System.out.println(queryQuestionFrequency);
	        System.out.println("rawQuery");
	        System.out.println(rawQuestionFrequency);
	        //printToExcel(rawQuestionFrequency);
	        printPercentage(rawFrequency, rawQuestionFrequency);		        
	        System.out.println("execSQL(java.lang.String)");
	        System.out.println(execQuestionFrequency);
	        printPercentage(execFrequency, execQuestionFrequency);
	        System.out.println("execSQL(java.lang.String,java.lang.Object[])");
	        System.out.println(execOverQuestionFrequency);
	        //printToExcel(execOverQuestionFrequency);
	        printPercentage(execOverFrequency, execOverQuestionFrequency);

	        printPercentage(execTotalFrequency, execOverQuestionFrequency);
	        System.out.println("compileStatement");
	        System.out.println(compileQuestionFrequency);
	        printPercentage(compileFrequency, compileQuestionFrequency);
	        //printToExcel(compileQuestionFrequency);
	        //System.out.println(sum((Collection<Integer>) queryQuestionFrequency.values()));
	        
	        System.out.println("No append/total parameterized statement:"+(numOfQuestion-numOfQuestionOneOp)*100.0/numOfQuestion+"%");
	        
	        System.out.println("At least one append/total non parameterized statement:"+numOfNonQuestionOneOp*100.0/numOfNonQuestion+"%");
	        */
	        int numPApp = parameterizeApp.size()-7;
	 
	        System.out.println("\nNum of parameterizable apps:"+ numPApp+" Percentage over apps that use SQLite:"+numPApp*100/583+"%");
	        
	        //System.out.println(QueryParser.total+" "+QueryParser.parse);
	        int par = QueryParser.select+QueryParser.update+QueryParser.insert+QueryParser.replace+QueryParser.delete-9;
	        System.out.println("Num of parameterizable statement instances:"+par);
	        System.out.println("Percentage of parameterizable statements by types:\n"+
	        "Select:"+Math.rint(QueryParser.select*100.0/queryFrequency.get("select"))+"%\n"+
	        "Insert:"+Math.rint(QueryParser.insert*100.0/queryFrequency.get("insert"))+"%\n"+
	        "Update:"+Math.rint(QueryParser.update*100.0/queryFrequency.get("update"))+"%\n"+
	        "Delete:"+Math.rint(QueryParser.delete*100.0/queryFrequency.get("delete"))+"%\n"+
	        "Replace:"+Math.rint(QueryParser.replace*100.0/queryFrequency.get("replace"))+"%\n");
	        //System.out.println("concat unknown");
	        //System.out.println(queryConcatUnknownFrequency);
	        System.out.println("--------------RQ 5---------------");
	        
	        System.out.println("Concat tainted in "+taintedApp.size()+" apps ("+ Math.rint(taintedApp.size()*100.0/583)+"%) "+"and "+
	        sum((Collection<Integer>) queryTaintFrequency.values())+" instances");
	        
	        //System.out.println(sum((Collection<Integer>) queryConcatUnknownFrequency.values()));
	        //System.out.println("concat tainted");
	        int rawTaint = sum((Collection<Integer>) rawTaintFrequency.values());
	        int execTaint = sum((Collection<Integer>) execTaintFrequency.values());
	        int execOverTaint = sum((Collection<Integer>) execOverTaintFrequency.values());
	        int compileTaint = sum((Collection<Integer>) compileTaintFrequency.values());
	        int taintTotal = rawTaint + execTaint + execOverTaint + compileTaint;
	        System.out.println("Percentage of tainted queries executed by API rawQuery:"
	        		+ Math.rint(rawTaint*100.0/taintTotal)+"% "+"execSQL_UnPara:"+Math.rint(execTaint*100.0/taintTotal)+"% "
	        	+"execSQL_Para:"+Math.rint(execOverTaint*100.0/taintTotal)+"% "+"compileStatement:"+
	        	Math.rint(compileTaint*100.0/taintTotal)+"%" );
	        
	        System.out.println("Fig. 3");
	        System.out.println("Number of tainted queries executed by API\n"
	        	+"rawQuery:"+ rawTaint +" Fixable percentage:"+ Math.rint(RQAFixableFrequency.get("rawQuery")*100.0/rawTaint)+"%\n"
	        	+"execSQL_UnPara:"+ execTaint +" Fixable percentage:"+ Math.rint(RQAFixableFrequency.get("execSQL(java.lang.String)")*100.0/execTaint)+"%\n"
	        	+"execSQL_Para:"+ execOverTaint +" Fixable percentage:" + Math.rint(RQAFixableFrequency.get("execSQL(java.lang.String,java.lang.Object[])")*100.0/execOverTaint)+"%\n"
	        	+"compile:"+ compileTaint +" Fixable percentage:NA");
	        	

		       
	        /*
	        System.out.println(queryTaintFrequency);
	        System.out.println("rawQuery:"+sum((Collection<Integer>) rawTaintFrequency.values())*100/sum((Collection<Integer>) queryTaintFrequency.values())+"%");
	        System.out.println(rawTaintFrequency);
	        //printPercentage(rawFrequency, rawTaintFrequency);		        
	        System.out.println("execSQL(java.lang.String):"+sum((Collection<Integer>) execTaintFrequency.values())*100/sum((Collection<Integer>) queryTaintFrequency.values())+"%");
	        System.out.println(execTaintFrequency);
	        //printPercentage(execFrequency, execTaintFrequency);
	        System.out.println("execSQL(java.lang.String,java.lang.Object[]):"+sum((Collection<Integer>) execOverTaintFrequency.values())*100/sum((Collection<Integer>) queryTaintFrequency.values())+"%");
	        System.out.println(execOverTaintFrequency);
	        //printPercentage(execOverFrequency, execOverTaintFrequency);
	        System.out.println("compileStatement:"+sum((Collection<Integer>) compileTaintFrequency.values())*100/sum((Collection<Integer>) queryTaintFrequency.values())+"%");
	        System.out.println(compileTaintFrequency);
	        //printPercentage(compileFrequency, compileTaintFrequency);
	        */
	        

	        //System.out.println("Parameterizable:"+parameterizedTaintStatement+" "+parameterizedTaintStatement*100/sum((Collection<Integer>) queryTaintFrequency.values())+"%");
	        //System.out.println(taintedAPIFrequency);
	        for(String api : taintedAPIFrequency.keySet())
	        {
	        	//System.out.println(api+" " + taintedAPIFrequency.get(api)+" "+sourceToCata.get(api));
	        }
	        //System.out.println("Fail to parse:"+no+" "+total+" "+no*1.0/total);
	        //System.out.println("Fail to parse because of the value starts with an unknown method:"+unknownMethod+" field:"+unknownField+" para:"+unknownPara);
	        
	        //System.out.println("Entire Query is from an unknown method:"+unknownMethod+" "+total+" "+unknownMethod*1.0/total);
	        
	        /*
	        Set<String> noDup = new HashSet<>(unknownMethods);
	        for(String m : noDup)
	        {
	        	if(!source.contains(m))
	        	{
	        		if(m.startsWith("<java"))
	        		{
	        			//System.out.println(m);
	        		}
	        		else if(m.startsWith("<android"))
	        		{
	        			
	        		}
	        		else
	        		{
	        			
	        		}
	        	}
	        	
	        
	        }
	        */
        }
 
        catch(IOException ex)
        {
        	
        }
       
	}
	private static void printToExcel(Map<String,Integer> queryFrequency)
	{
		System.out.println("START");
		System.out.println(queryFrequency.get("select"));
		System.out.println(queryFrequency.get("insert"));
		System.out.println(queryFrequency.get("update"));
		System.out.println(queryFrequency.get("delete"));
		System.out.println(queryFrequency.get("replace"));
		System.out.println(queryFrequency.get("create"));
		System.out.println(queryFrequency.get("drop"));
		System.out.println(queryFrequency.get("alter"));
		System.out.println(queryFrequency.get("vacuum"));
		System.out.println(queryFrequency.get("pragma"));
		if(queryFrequency.containsKey("whole"))
		{
			System.out.println(queryFrequency.get("whole"));
			System.out.println(queryFrequency.get("unknown"));
		}
	}
	private static void printPercentage(Map<String, Integer> queryFrequency,
			Map<String, Integer> rawQuestionFrequency) {
		System.out.println("select:"+rawQuestionFrequency.get("select")*100.0/queryFrequency.get("select")+"% "
				+"replace:"+rawQuestionFrequency.get("replace")*100.0/queryFrequency.get("replace")+"% "
				+"insert:"+rawQuestionFrequency.get("insert")*100.0/queryFrequency.get("insert")+"% "
				+"update:"+rawQuestionFrequency.get("update")*100.0/queryFrequency.get("update")+"% "
				+"delete:"+rawQuestionFrequency.get("delete")*100.0/queryFrequency.get("delete")+"%");
	}

	private static Integer sum(Collection<Integer> values)
	{
		int sum = 0;
		for(int value : values)
			sum += value;
		return sum;
	}
	public static void parseStringFile()
	{
        try{
        	BufferedReader br = new BufferedReader(new FileReader("/home/yingjun/Documents/appset/string1.txt"));
	        String ir = null;
	        String jimple = null;
	        String appName = null;
	        String operation = null;
	        String line = null;
	        String identifier = null;

	        while ((line = br.readLine()) != null) {
	        	if(line.startsWith("Method Name:"))
	        	{
	        		identifier = line.substring(line.indexOf("<"));
	        	}
	        	else if(line.startsWith("Bytecode Offset: "))
	        	{
	        		String offset = line.replace("Bytecode Offset: ", "");
	        		identifier += offset;
	        	
	        	}
	        	else if(line.startsWith("Jimple:"))
	        		jimple = line;
	        	else if(line.startsWith("Value"))
	        	{
	        		//System.out.println(line);
	        		if(jimple.contains("execSQL"))
	        		{
		        		String query = line.substring(line.indexOf(":")+1).trim().toLowerCase();
		        		for(String prefix : prefixes)
		        		{
		        			if(query.startsWith(prefix))
		        			{
		        				if(idToTypes.containsKey(identifier))
		        					idToTypes.get(identifier).add(prefix);
		        				else
		        				{
		        					Set<String> type = new HashSet<>();
		        					type.add(prefix);
		        					idToTypes.put(identifier, type);
		        				}
		        			}
		        		}
	        		}
	        	}
	        		
	        }
	        for(Entry<String,Set<String>> en : idToTypes.entrySet())
	        {
	        	//if(en.getValue().size()>1)
	        	//	System.out.println(en.getValue());
	        }
	        //System.out.println(idToTypes);
        }
        catch(IOException ex)
        {
        	
        }
    	
	}
	public static void analyzeTransaction(String filePath)
	{
		 try{
			 
	        BufferedReader br = null;
	        
	        String line;
	        String appName = null;
	        int numOfApp = 0;
	        int canIdentify = 0;
	        Instance instance = null;
	        Map<String,List<Instance>> appToSta= new HashMap<>();
	        Map<String,Integer> apiInLoopFreq = new HashMap<>();

	        List<String> apiInLoopInBegin = new ArrayList<>();
	        Map<String,Integer> apiInLoopNotInBeginCount = new HashMap<>();
	 
	        Set<String> appContainsNotAlias = new HashSet<>();
 	        br = new BufferedReader(new FileReader(filePath));
	        while ((line = br.readLine()) != null) {
	        	
	        	if(line.startsWith("/App"))
	        	{	
	        		appName = line;
	        		apiInLoopFreq.clear();
	        		apiInLoopInBegin.clear();
	        	}
	        	else if(line.startsWith("<"))
	        	{
	        		String api = line.split("@")[0];
	        		boolean inUI = line.contains("LoopInUI:true");
	        		instance = new Instance(api,inUI);
	        	}
	        	else if(line.startsWith("InBegin:"))
	        	{
	        		instance.inBegin = line.contains("InBegin:true");
	        		if(instance.inBegin)
	        		{
	        			String[] data  = line.split("@");
	        			instance.identifier = data[1];
	        			instance.lengthModToBegin = Integer.parseInt(data[2].split(":")[1]);
	        			instance.mayAlias = data[3].contains("MayAlias:true");
	        			if(data[3].contains("MayAlias:false"))
	        				appContainsNotAlias.add(appName);
	        		}
	        	}
	        	else if(line.startsWith("NotInBegin:"))
	        	{
	        		instance.notInBegin = line.contains("NotInBegin:true");
	        		if(instance.notInBegin)
	        		{
	        			String[] data  = line.split("@");
	        			instance.identifier = data[1];
	        			instance.lengthModToLoop = Integer.parseInt(data[2].split(":")[1]);
	        			instance.lengthModToEntry = Integer.parseInt(data[3].split(":")[1]);
	        			instance.defineInLoop = line.contains("MayDefineInLoop:true");
	        			instance.initInLoop = line.contains("MayInitializeInLoop:true");
	        		}
	        		if(appToSta.get(appName)==null)
	        		{
	        			List<Instance> ins = new ArrayList<>();
	        			ins.add(instance);
	        			appToSta.put(appName, ins);
	        		}
	        		else
	        			appToSta.get(appName).add(instance);
	        	}
	        	else if(line.startsWith("In loop@"))
	        	{
	        		String[] log = line.split("@");
	        		String api = log[1];
	        		int freq = Integer.parseInt(log[2]);
	        		apiInLoopFreq.put(api, freq);
	        	}
	        	else if(line.startsWith("In loop in begin@"))
	        	{
	        		String[] log = line.split("@");
	        		String api = log[1];
	        		int freq = Integer.parseInt(log[2]);
	        		apiInLoopInBegin.add(api);
	        		
	        	}
	        	else if(line.startsWith("In loop not in begin@"))
	        	{
	        		String api = line.split("@")[1];
	        		
	        		if(!apiInLoopInBegin.isEmpty()&&!apiInLoopInBegin.contains(api))
	        		{	        			
	        			int count = apiInLoopNotInBeginCount.containsKey(api)? apiInLoopNotInBeginCount.get(api) : 0;
	        			apiInLoopNotInBeginCount.put(api, count+1);
	        		}
	        	}

	        	
	        	
	        }
	        br.close();
	        
	        int numOfAppWithMSALoop = appToSta.keySet().size();
	        
	        Set<String> appWithMSALoop = appToSta.keySet();
	        Set<String> appWithMSALoopInUI = new HashSet<>();
	        Set<String> appWithMSALoopInBegin = new HashSet<>();
	        Set<String> appWithMSALoopNotBegin = new HashSet<>();
	        Set<String> appWithMSALoopNotBeginInUI = new HashSet<>();
	        int numOfMSALoopNotBeginInUI = 0;
	        Map<String,Integer> appToMSALoopNotBegin = new HashMap<>();
	        
	        Map<String,Integer> apiInLoop = new HashMap<>();
	        Map<String,Integer> apiInLoopInUI = new HashMap<>();
	        Map<String,Integer> apiInLoopBegin = new HashMap<>();
	        Map<String,Integer> apiInLoopNotBegin = new HashMap<>();
	        Map<Integer,Integer> beginLengthCount = new HashMap<>();
	        Map<Integer,Integer> notBeginLoopLengthCount = new HashMap<>();
	        Map<Integer,Integer> notBeginEntryLengthCount = new HashMap<>();
	        Map<Integer,Integer> defineLoopLengthCount = new HashMap<>();
	        Map<Integer,Integer> initLoopLengthCount = new HashMap<>();
	        int bothInBeginNotInBegin = 0;
	        Set<String> appBoth = new HashSet<>();

	        for(String app: appToSta.keySet())
	        {
	        	List<Instance> ins = appToSta.get(app);
	        	for(Instance in : ins)
	        	{
	        		
	        		String api = null;
	        		if(in.api.toLowerCase().contains("insert"))
	        			api = "insert";
	        		else if(in.api.toLowerCase().contains("update"))
	        			api = "update";
	        		else if(in.api.toLowerCase().contains("delete"))
	        			api = "delete";
	        		else if(in.api.contains("replace"))
	        			api = "replace";
	        		else if(in.api.contains("execSQL"))
	        		{
	        			api = "execSQL";
	        			//if(!idToTypes.containsKey(in.identifier))
	        			//{
	        			//	canIdentify++;
	        			//}
	        		}
	        		else if(in.api.contains("execute"))
	        		{
	        			api = "execute";
	        		}
	        		
	        		//String api = in.api;
	        		int count = apiInLoop.containsKey(api)? apiInLoop.get(api) : 0;
	        		apiInLoop.put(api, count+1);
	        		
	        		if(in.inBegin&&in.notInBegin)
	        		{
	        		    bothInBeginNotInBegin++;
	        		    appBoth.add(app);
	        			//System.out.println(app);
	        		}
	        			
	        		if(in.inUI)
	        		{
	        			appWithMSALoopInUI.add(app);
		        		int countUI = apiInLoopInUI.containsKey(api)? apiInLoopInUI.get(api) : 0;
		        		apiInLoopInUI.put(api, countUI+1);
		        		
		        		if(in.notInBegin)
		        		{
		        			appWithMSALoopNotBeginInUI.add(app);
		        			numOfMSALoopNotBeginInUI++;
		        		}
	        		}
	        		//all of them alias after manual confirmation
	        		if(in.inBegin)
	        		{
	        			appWithMSALoopInBegin.add(app);
	        			int countBegin = apiInLoopBegin.containsKey(api)? apiInLoopBegin.get(api) : 0;
	        			apiInLoopBegin.put(api, countBegin+1);
	        			
	        			int beginLength = in.lengthModToBegin;
	        			int countLength = beginLengthCount.containsKey(beginLength)? beginLengthCount.get(beginLength):0;
	        			beginLengthCount.put(beginLength, countLength+1);
	        			
	        			
	        		}
	        		
	        		if(in.notInBegin)
	        		{
	        			appWithMSALoopNotBegin.add(app);
	        			
	        			int countNumNotBegin = appToMSALoopNotBegin.containsKey(app)? appToMSALoopNotBegin.get(app) : 0;
	        			appToMSALoopNotBegin.put(app, countNumNotBegin+1);
	        			
	        			int countNotBegin = apiInLoopNotBegin.containsKey(api)? apiInLoopNotBegin.get(api) : 0;
	        			apiInLoopNotBegin.put(api, countNotBegin+1);
	        			
	        			int loopLength = in.lengthModToLoop;
	        			int countLoopLength = notBeginLoopLengthCount.containsKey(loopLength)? notBeginLoopLengthCount.get(loopLength):0;
	        			notBeginLoopLengthCount.put(loopLength, countLoopLength+1);
	        			
	        			//!!! Different from the loop length, 1 means in the same method.
	           			int entryLength = in.lengthModToEntry;
	        			int countEntryLength = notBeginEntryLengthCount.containsKey(entryLength)? notBeginEntryLengthCount.get(entryLength):0;
	        			notBeginEntryLengthCount.put(entryLength, countEntryLength+1);
	        			
	        			if(in.defineInLoop)
	        			{
	        				int countDefineLoopLength = defineLoopLengthCount.containsKey(loopLength)? defineLoopLengthCount.get(loopLength):0;
	        				defineLoopLengthCount.put(loopLength, countDefineLoopLength+1);
	        			}
	        			if(in.initInLoop)
	        			{
	        				int countInitLoopLength = initLoopLengthCount.containsKey(loopLength)? initLoopLengthCount.get(loopLength):0;
	        				initLoopLengthCount.put(loopLength, countInitLoopLength+1);
	        			}
	        			
	        		}
	        		
	        	}
	        }
	        /*
	        System.out.println(appContainsNotAlias.size());
	        List<String> apps = new ArrayList<>(appContainsNotAlias);
	        Collections.sort(apps);
	        for(String app: apps)
	        	System.out.println(app);
	        */
	        
	        //verifiedTransaction(appToSta);
	        System.out.println("1) Batched MSAs:");
	        System.out.println("Num of apps with MSA in loops:"+numOfAppWithMSALoop); //+ " Percentage(appWithMSALoop/appWithSQL):"+numOfAppWithMSALoop*100/583+"%");
	        //System.out.println("Num of apps with MSA in loops in UI:"+appWithMSALoopInUI.size()+ " Percentage(appWithMSALoopUI/appWithMSALoop):"+appWithMSALoopInUI.size()*100/numOfAppWithMSALoop+"%");
	        System.out.println("Num of apps with batched MSA:"+appWithMSALoopInBegin.size()+ " Percentage(appWithBatchedMSA/appWithMSAInLoop):"+appWithMSALoopInBegin.size()*100/numOfAppWithMSALoop+"%");
	        
	        
	        int total = 0;
	        int totalBegin = 0;
	        for(int freq : apiInLoop.values())
	        	total += freq;
	        for(int freq: apiInLoopBegin.values())
	        	totalBegin += freq;
	        //msa in loop in begin
	        
	        System.out.println("Num of intances:"+totalBegin+" Percentage:"+totalBegin*100/total+"%");
	        //System.out.println("Begin"+apiInLoopBegin);
	        System.out.println("Percentage of the batched MSAs over all the MSAs in loops (Fig. 5):");
	        for(String api : apiInLoopBegin.keySet())
	        {
	        	totalBegin += apiInLoopBegin.get(api);
	        	System.out.print(api+":"+apiInLoopBegin.get(api)*100/apiInLoop.get(api)+"% ");
	        	
	        }
	        System.out.println("");
	        
	        System.out.println();
	        System.out.println("2) Unbatched MSAs:");
	        System.out.println("Num of apps with unbatched MSA:"+
	        appWithMSALoopNotBegin.size()+ "\nPercentage(appWithUnbatchedMSALoop/appWithMSAInLoop):"+appWithMSALoopNotBegin.size()*100/numOfAppWithMSALoop+"%\n"+
	        		"Percentage(appWithUnbatchedMSA/appWithSQL):"+appWithMSALoopNotBegin.size()*100/583+"%");

	        System.out.println();
	        //System.out.println("Num of apps with MSA in loop in begin and not in begin:"+appBoth.size()
	        //		+" Num of instances:"+bothInBeginNotInBegin);
	        int totalNotBegin = 0;
	        for(int freq : apiInLoopNotBegin.values())
	        	totalNotBegin += freq;
	        System.out.println("Average MSA in loop not begin per app:"+totalNotBegin/ appWithMSALoopNotBegin.size());
	        List<Integer> median = new ArrayList<>();
	        for(String app: appToMSALoopNotBegin.keySet())
	        	median.add(appToMSALoopNotBegin.get(app));
	        Collections.sort(median);
	        System.out.println("Median MSA in loop not begin per app:"+median.get(median.size()/2));
	        
	        System.out.println();
	        System.out.println("Percentage of MSA in loop not begin in UI/ MSA in loop not begin:"+
	        numOfMSALoopNotBeginInUI*100/totalNotBegin+"%");
	        System.out.println();
	        
	        System.out.println("Length of call path between msa and loop (Length to Num of instances):"+notBeginLoopLengthCount);
	
	        int totalHP = 0;
	        int zeroHP = notBeginLoopLengthCount.get(0);
	        int zeroAndOneHP = notBeginLoopLengthCount.get(0)+notBeginLoopLengthCount.get(1);
	        	//	+notBeginLoopLengthCount.get(2);
	        //int XHP = notBeginLoopLengthCount.get(5)+notBeginLoopLengthCount.get(6)+beginLengthCount.get(7)+
	       // 		beginLengthCount.get(7)+beginLengthCount.get(8)+beginLengthCount.get(9)+
	        //		beginLengthCount.get(10)+beginLengthCount.get(11)+beginLengthCount.get(12);
	        int maxLength = 0;
	        for(Entry<Integer,Integer> en : notBeginLoopLengthCount.entrySet())
	        {
	        	if(en.getKey()>maxLength)
	        		maxLength = en.getKey();
	        	totalHP += en.getValue();
	        }
	        for(int i = 0; i <= maxLength; i++)
	        {
	        	System.out.print(i+":"+notBeginLoopLengthCount.get(i)*100/totalHP+"% ");
	        }
	        System.out.println();
	        System.out.println("MSA and Begin with call path length > 0:"+ (totalHP-zeroHP)*100/totalHP+"%");
	        System.out.println("MSA and Begin with call path length >= 2:"+ (totalHP-zeroAndOneHP)*100/totalHP+"%\n");
	        //System.out.println("MSA and Begin with call path length >= 5:"+XMSA*100/totalMSA+"%");
	        
	        //Define in loop
	        //System.out.println("Define maybe in the loop ():"+defineLoopLengthCount);
	        int totalDefine = 0;
	        for(int freq: defineLoopLengthCount.values())
	        	totalDefine += freq;
	        System.out.println("Percentage of unbatched MSAs that have the database object defined inside the loop:"+totalDefine*100/totalHP+"%\n");
	   
	        System.out.println("Length of call path length to the percentage of unbatched MSAs"
	        		+ "that have the database object defined in loop:");
	        for(int length : defineLoopLengthCount.keySet())
	        {
	        	totalDefine += defineLoopLengthCount.get(length);
	        	System.out.print("Length "+length+":"+defineLoopLengthCount.get(length)*100/notBeginLoopLengthCount.get(length)+"% ");
	        	
	        }
	        System.out.println();
	        
	        
	        /*
	        System.out.println("Num of apps with MSA in loops not in begin in UI:"+
	        		appWithMSALoopNotBeginInUI.size()+" Percentage(appWithMSALoopNotBeginInUI/appWithMSALoop):"+
	        		appWithMSALoopNotBeginInUI.size()*100/numOfAppWithMSALoop+"%");
	       
	        
	       
	        System.out.println("Loop"+apiInLoop);
	        System.out.println("Loop UI"+apiInLoopInUI);

	        //msa in loop (in UI)
	        int totalInUI = 0;
	        for(String api : apiInLoopInUI.keySet())
	        {
	        	totalInUI += apiInLoopInUI.get(api);
	        	
	        	System.out.print(api+":"+apiInLoopInUI.get(api)*100/apiInLoop.get(api)+"% ");
	        }
	        System.out.println("Total:"+totalInUI*100/total+"%\n");
	        

	        //call path length between msa and beginTransaction
	        System.out.println("Length of call path between msa and begin:"+beginLengthCount);
	        int totalMSA = 0;
	        int zeroMSA = beginLengthCount.get(0);
	        int zeroAndOneMSA = beginLengthCount.get(0)+beginLengthCount.get(1);
	        int XMSA = beginLengthCount.get(5)+beginLengthCount.get(6)+beginLengthCount.get(7)+
	        		beginLengthCount.get(7)+beginLengthCount.get(8)+beginLengthCount.get(9)+
	        		beginLengthCount.get(10)+beginLengthCount.get(11)+beginLengthCount.get(12);
	        for(Entry<Integer,Integer> en : beginLengthCount.entrySet())
	        {
	        	totalMSA += en.getValue();
	        }
	        System.out.println("MSA and Begin with call path length == 0:"+zeroMSA*100/totalMSA+"%");
	        System.out.println("MSA and Begin with call path length <= 1:"+zeroAndOneMSA*100/totalMSA+"%");
	        System.out.println("MSA and Begin with call path length >= 5:"+XMSA*100/totalMSA+"%\n");
	        
	        
	        //msa in loop not in begin
	        
	        System.out.println("Not Begin"+ apiInLoopNotBegin);
	        for(String api : apiInLoopNotBegin.keySet())
	        {

	        	System.out.print(api+":"+apiInLoopNotBegin.get(api)*100/apiInLoop.get(api)+"% ");
	        	
	        }
	        System.out.println("Total:"+totalNotBegin*100/total+"%\n");



	        
	        //System.out.println("Init maybe in the loop:"+initLoopLengthCount);
	        
	        System.out.println("Length of call path between msa and entry:"+notBeginEntryLengthCount);
	        //Average
	        int totalLength = 0; 
	        for(int length : notBeginEntryLengthCount.keySet())
	        {
	        	totalLength += length*notBeginEntryLengthCount.get(length);
	        	
	        }
	        System.out.println("Interprocedural:"+(100-notBeginEntryLengthCount.get(1)*100/totalHP)+"%");
	        System.out.println("Average length:"+totalLength*1.0/totalHP);
	        */
		  }
	 
	     catch(IOException ex)
	     {
	     	
	     }
		
	}
	public static void verifiedTransaction(Map<String,List<Instance>> appToSta)
	{
		for(String app: appToSta.keySet())
		{
			int inLoop = 0;
			int inBegin = 0;
			int notInBegin = 0;
			for(Instance in : appToSta.get(app))
			{
				inLoop ++;
				if(in.inBegin)
					inBegin++;
				if(in.notInBegin)
					notInBegin++;
			}
			if(inLoop!=(inBegin + notInBegin))
				System.out.println(app);
		}
	}
	public static void rankAppForDynamicAnalysis()
	{
		Map<String,Integer> appSQLFreq = new HashMap<>();
		Map<String,Boolean> appHasDBInUserClass = new HashMap<>();
		Map<String,Boolean> appHasDBInUserEvent = new HashMap<>();
        try{
        	String line = null;
        	String appName = null;
        	BufferedReader br = new BufferedReader(new FileReader("/home/yingjun/Documents/appset/1000apps-for-instrument.txt"));
        	while ((line = br.readLine()) != null) {
        		appSQLFreq.put(line, 0);
        	}
        	br.close();
        	br = new BufferedReader(new FileReader("/home/yingjun/Documents/appset/dynamic.txt"));
	        while ((line = br.readLine()) != null) {
	        	if(line.startsWith("App"))
	        	{
	        		appName = line.substring(line.indexOf("/")+1);
	        	}
	        	else if(line.startsWith("android.database.sqlite:"))
	        	{
	        		int freq = Integer.parseInt(line.split(":")[1]);
	        		if(appSQLFreq.containsKey(appName))
	        			appSQLFreq.put(appName, freq);
	        	}
	        	else if(line.startsWith("Invoke SQLite in user code very likely:"))
	        	{

	        		if(line.startsWith("Invoke SQLite in user code very likely:true"))
	        		{
	        			appHasDBInUserClass.put(appName, true);
	        		}
	        		else
	        			appHasDBInUserClass.put(appName, false);
	        	}
	        	else if(line.startsWith("Invoke SQLite in user event:"))
	        	{
	        		if(line.startsWith("Invoke SQLite in user event:true"))
	        			appHasDBInUserEvent.put(appName, true);
	        		else
	        			appHasDBInUserEvent.put(appName, false);
	        	}
	        }
        }
        catch(IOException ex)
        {
        	
        }
        
        Map<Integer,List<String>> score = new HashMap<>();
        for(String appName: appSQLFreq.keySet())
        {
        	int s = 0;
        	s += appSQLFreq.get(appName);
        	if(appSQLFreq.get(appName)==0)
        		continue;
        	if(appHasDBInUserClass.get(appName))
        		s += 200;
        	if(appHasDBInUserEvent.get(appName))
        		s += 50;
        	if(score.containsKey(s))
        		score.get(s).add(appName);
        	else
        	{
        		List<String> list = new ArrayList<>();
        		list.add(appName);
        		score.put(s, list);
        	}
        }
        
        List<Integer> sort = new ArrayList<>();
        sort.addAll(score.keySet());
        Collections.sort(sort);
        StringBuilder output = new StringBuilder();
        for(int i = sort.size()-1; i >= 0; i--)
        {
        	for(String appName : score.get(sort.get(i)))
        		output.append(appName+"\n");
        }
        System.out.println(output);
        
	}
	
	public static void analyzeAPIInLoopFreq(String filePath)
	{
		Set<String> appWithOpenDatabaseInLoop = new HashSet<>();
		Map<String,Integer> apiToFreq = new HashMap<>();

        try{
        	BufferedReader br = new BufferedReader(new FileReader(filePath));
        	String line;
        	String app = null;
        	
	        while ((line = br.readLine()) != null) {
	        	if(line.startsWith("In loop all@"))
	        	{
	        		String[] items = line.split("@");
	        		String api = items[1];
	        		int freq = Integer.parseInt(items[2]);
	        		int count = apiToFreq.containsKey(api)? apiToFreq.get(api):0;
	        		apiToFreq.put(api, count+freq);
	        		
	        		if(line.contains("<android.database.sqlite.SQLiteDatabase: android.database.sqlite.SQLiteDatabase openDatabase(java.lang.String,android.database.sqlite.SQLiteDatabase$CursorFactory,int)>"))
	        			appWithOpenDatabaseInLoop.add(app);
	        	}
	        	else if(line.startsWith("/App"))
	        	{
	        		app = line;
	        	}
	        }
	        br.close();
	        
	        Map<Integer,List<String>> freqToAPI = new HashMap<>();
	        for(Entry<String,Integer> apiFreq : apiToFreq.entrySet())
	        {
	        	int freq = apiFreq.getValue();
	        	String api = apiFreq.getKey();
	        	if(freqToAPI.containsKey(freq))
	        		freqToAPI.get(freq).add(api);
	        	else
	        	{
	        		List<String> apis  = new ArrayList<>();
	        		apis.add(api);
	        		freqToAPI.put(freq, apis);
	        	}
	        }
	        List<Integer> freqs = new ArrayList<>(freqToAPI.keySet());
	        Collections.sort(freqs,Collections.reverseOrder());
	        //System.out.println(freqs);
	        int rank = 1;
	        for(int i = 0 ; i < freqs.size(); i++)
	        {

	        	for(String api : freqToAPI.get(freqs.get(i)))
	        		//System.out.println(freqs.get(i));
	        	{
	        		System.out.println("Rank "+ rank + " : "+api);
	        		rank++;
	        	}
	        }
	        
	   
        }
        catch(IOException e)
        {
        	e.printStackTrace();
        }
	}
	public static void analyzeAPIFreq(String filePath)
	{
		int total = 0;
		Map<String,Integer> apiToFreq = new HashMap<>();
        try{
        	BufferedReader br = new BufferedReader(new FileReader(filePath));
        	String line;
	        while ((line = br.readLine()) != null) {
	        	if(line.startsWith("<android.database.sqlite"))
	        	{

	        	
	        		String api = line.substring(0,line.lastIndexOf(">")+1);
	        		int freq = Integer.parseInt(line.substring(line.lastIndexOf(":")+1));
	        		total += freq;
	        		int count = apiToFreq.containsKey(api)? apiToFreq.get(api):0;
	        		apiToFreq.put(api, count+freq);
	        		
	        	}
	        }
	        br.close();
	        //System.out.println(apiToFreq);
	        Map<Integer,List<String>> freqToAPI = new HashMap<>();
	        for(Entry<String,Integer> apiFreq : apiToFreq.entrySet())
	        {
	        	int freq = apiFreq.getValue();
	        	String api = apiFreq.getKey();
	        	if(freqToAPI.containsKey(freq))
	        		freqToAPI.get(freq).add(api);
	        	else
	        	{
	        		List<String> apis  = new ArrayList<>();
	        		apis.add(api);
	        		freqToAPI.put(freq, apis);
	        	}
	        }
	        List<Integer> freqs = new ArrayList<>(freqToAPI.keySet());
	        Collections.sort(freqs,Collections.reverseOrder());
	        System.out.println("Num of distinct invoked APIs:" + apiToFreq.keySet().size());
	        System.out.println("Total num of invocation:" + total);
	        int rank = 1;
	        for(int i = 0 ; i < freqs.size(); i++)
	        {

	        	for(String api : freqToAPI.get(freqs.get(i)))
	        	{	
	        		System.out.println("Rank "+rank+": API:"+api + " Num of invocation:" + freqs.get(i) +" Percentage:"+ Math.rint(freqs.get(i)*100.0/total)+"%");
	        		rank++;
	        	}
	        	//System.out.println(freqs.get(i) + " "+freqToAPI.get(freqs.get(i)));
	        }
        }
        catch(IOException e)
        {
        	
        }
	}
	public static void analyzeFrequency()
	{
        BufferedReader br = null;
        String line;
        Map<String,List<String>> appDB = new HashMap<>();
        List<Integer> apps = new ArrayList<>();
        try{
	        br = new BufferedReader(new FileReader("/home/yingjun/Documents/appset/rq.txt"));
	        String appName = null;
	        boolean db = false;
	        boolean sqlite = false;
	        while ((line = br.readLine()) != null) {
	        	if(line.startsWith("App"))
	        	{
	        		appName = line;
	        		appDB.put(appName, new ArrayList<String>());
	        		apps.add(Integer.parseInt(line.split("/")[0].replaceAll("App", "")));
	        	}
	        	else if(line.startsWith("DB Freq"))
	        	{
	        		db = true;
	        		sqlite = false;
	        	}
	        	else if(line.startsWith("SQLite API"))
	        	{
	        		db = false;
	        		sqlite = true;
	        	}
	        	else if(line.equals("\n"))
	        	{
	        		
	        	}
	        	else
	        	{
	        		if(db&&!sqlite)
	        			appDB.get(appName).add(line);
	        	}
	        }
	        br.close();
        }
        catch(IOException ex)
        {
        	
        }
        
        
        int containDB = 0;
        int containOracle = 0;
        for(String app: appDB.keySet())
        {
        	System.out.println(app + " " + appDB.get(app));
        	if(!appDB.get(app).isEmpty())
        		containDB ++;
        	if(appDB.get(app).toString().contains("java.sql"))
        		containOracle++;
        }
        System.out.println("APPs contain db:"+ containDB + "/"+ appDB.size()+ " "+ containDB*1.0/appDB.size()*100+"%");
        System.out.println("java.sql:"+containOracle*1.0/appDB.size()*100+"%");
        
        for(int i =1;i<=1000;i++)
        {
        	if(!apps.contains(i))
        		System.out.println("App missing:"+i);
        }
	}

}
