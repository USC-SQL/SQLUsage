package usc.sql.usage;

import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import usc.sql.ir.Variable;
import usc.sql.log.LogSummary;
import usc.sql.string.JavaAndroid;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;

public class QueryParser {

	private String[] prefix = {"create","drop","insert","update","replace","delete","select","alter",
			"pragma","vacuum","end"};
	
	public static void parseStringResult(JavaAndroid ja, Map<String, Set<Variable>> stringResult,StringBuilder output)
	{
		for(String irSig : stringResult.keySet())
		{
			String hotspot[] = irSig.split("@");
			System.out.println("Method Name: "+ hotspot[0]);
			output.append("Method Name: "+ hotspot[0]+"\n");
			System.out.println("Source Line Number: "+ hotspot[1]);
			output.append("Source Line Number: "+ hotspot[1]+"\n");
			System.out.println("Bytecode Offset: "+hotspot[2]);
			output.append("Bytecode Offset: "+hotspot[2]+"\n");
			System.out.println("Nth String Parameter: " + hotspot[3]);
			output.append("Nth String Parameter: " + hotspot[3]+"\n");
			System.out.println("Jimple: "+ hotspot[4]);
			output.append("Jimple: "+ hotspot[4]+"\n");
			Set<Variable> ir = stringResult.get(irSig);
			//Set<String> value = new HashSet<>();
			for(Variable targetIR : ir)
			{
				System.out.println("IR:"+targetIR);
				output.append("IR:"+targetIR+"\n");
				Map<String,Integer> opFreq = new HashMap<>();
				ja.getOperations(targetIR, opFreq);
				System.out.println("Operations:"+opFreq);
				output.append("Operations:"+opFreq+"\n");
				for(String intpValue: targetIR.getInterpretedValueL())
				{
					String replace = intpValue.trim().replaceAll("\\\\'","'");
					System.out.println("Value:"+replace);
					output.append("Value:"+replace+"\n");
					//if(replace.toLowerCase().startsWith("insert")||replace.toLowerCase().startsWith("update"))
					
					//	QueryParser.parseSQL(replace);
					
				}
				//value.addAll(targetIR.getInterpretedValueL());
			}
				
			System.out.println();
		}
	}
	
	public static int total = 0, parse = 0, insert = 0, update = 0, select = 0, delete = 0, replace = 0;
	
	//public static CCJSqlParserManager parserManager = new CCJSqlParserManager();
	//public static void main(String[] args)
	//{
	//	parseSQL("select avg(count) as avg from reportssnoozecount where inactive = 0 and date between 'unknown@field' and 'unknown@field' and ( or alarmtype = 3)".toLowerCase());
	//}
	public static boolean parseSQL(String sql)
	{
		sql = sql.replaceAll("\"", "").replaceAll("\\\\","").replaceAll("insert or replace", "insert")
				.replaceAll("insert or ignore", "insert")
				.replaceAll("<.*>", "").split("union")[0].replaceAll("\\(  \\)", "");
		//System.out.println(sql);
		boolean parameterize = false;
		total++;
		try {
			//String insert = "INSERT INTO config (idconfig, data, screenshot) VALUES(NULL, \'\',\'true\');";
			//String update = "UPDATE keys set k2 = ? where k1 is not null;";
			
			//Statement statement = parserManager.parse(new StringReader(sql));
			Statement statement = (Statement) CCJSqlParserUtil.parse(sql);
			//System.out.println(statement.getClass());
			if(statement instanceof Insert)
			{
				Insert in = (Insert) statement;
				
				ExpressionList list = (ExpressionList) in.getItemsList();
				if(list.getExpressions().toString().contains("unknown"))
				{		
					parameterize = true;
					insert++;
				}
			}
			else if(statement instanceof Update)
			{
				Update up = (Update) statement;
				boolean containUnknown = false;
				if(up.getExpressions().toString().contains("unknown"))
				{
					containUnknown = true;
					parameterize = true;
					update++;
			
				}
				if(up.getWhere().toString().contains("unknown"))
				{					
					//String[] where = up.getWhere().toString().split("=")
					String where = up.getWhere().toString().toLowerCase();
					
					if(where.contains(" and "))
					{
						String[] clause = where.split(" and ");
						for(String exp : clause)
						{
							if(exp.contains("="))
							{
								String right = exp.split("=")[1];
								if(right.contains("unknown"))
								{
									if(!containUnknown)
									{
										containUnknown = true;
										update++;
										parameterize = true;
									}
								}
							}
						}
					}
				}
				//if(containUnknown)
				//	System.out.println(sql);
				
			}
			else if(statement instanceof Delete)
			{
				Delete de = (Delete) statement;
				String where = de.getWhere().toString().toLowerCase();
				boolean containUnknown = false;
				if(where.contains(" and "))
				{
					String[] clause = where.split(" and ");
					for(String exp : clause)
					{
						if(exp.contains("="))
						{
							String right = exp.split("=")[1];
							if(right.contains("unknown"))
							{
								if(!containUnknown)
								{
									containUnknown = true;
									delete++;
									parameterize = true;
								}
							}
						}
					}
				}
			}
			else if(statement instanceof Replace)
			{
				Replace re = (Replace) statement;
				ExpressionList list = (ExpressionList) re.getItemsList();
				if(list.getExpressions().toString().contains("unknown"))
				{
					//System.out.println(list.getExpressions());
					replace++;
					parameterize = true;
				}
			}
			else if(statement instanceof Select)
			{
				Select se = (Select) statement;
		
				PlainSelect pse = (PlainSelect) se.getSelectBody();
				
				String where = pse.getWhere().toString().toLowerCase();
				if(where.contains("unknown"))
				{
					//System.out.println(where);
					boolean containUnknown = false;
					if(where.contains(" and "))
					{
						String[] clause = where.split(" and ");
						for(String exp : clause)
						{
							if(exp.contains("="))
							{
								String right = exp.split("=")[1];
								if(right.contains("unknown"))
								{
									if(!containUnknown)
									{
										containUnknown = true;
										select++;
										parameterize = true;
									}
								}
							}
						}
					}
				}
				
			}

			
			parse++;
			
		} catch (Throwable e) {
			
			//System.out.println(sql);
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return parameterize;
	}
	
	public static boolean parseTaintSQL(String sql)
	{
		Set<String> source = LogSummary.source;
		boolean parameterize = false;
		//System.out.println(sql);
		sql = sql.replaceAll("\"", "").replaceAll("\\\\","").replaceAll("insert or replace", "insert")
				.replaceAll("insert or ignore", "insert")
				.split("union")[0].replaceAll("\\(  \\)", "");

		int id = 0;
		Map<Integer,String> idToAPI = new HashMap<>();
		while(sql.contains("unknown@method@<"))
		{
			String sub = sql.substring(sql.indexOf("unknown@method@<"));
			int a1 = sub.indexOf("<");
			int a2 = sub.indexOf(">")+1;
			
			String api = sub.substring(a1,a2);			
			idToAPI.put(id, api);
			sql = sql.replaceFirst("<.*>", ""+id);
			id ++;
			
		}
		//System.out.println(sql);
		//System.out.println(sql);
		total++;
		try {
			//String insert = "INSERT INTO config (idconfig, data, screenshot) VALUES(NULL, \'\',\'true\');";
			//String update = "UPDATE keys set k2 = ? where k1 is not null;";
			
			//Statement statement = parserManager.parse(new StringReader(sql));
			Statement statement = (Statement) CCJSqlParserUtil.parse(sql);
			//System.out.println(statement.getClass());
			if(statement instanceof Insert)
			{
				Insert in = (Insert) statement;
				
				ExpressionList list = (ExpressionList) in.getItemsList();
				if(list.getExpressions().toString().contains("unknown@method@"))
				{	
					parameterize = true;
					insert++;
				}
			}
			else if(statement instanceof Update)
			{
				Update up = (Update) statement;
				boolean containUnknown = false;
				if(up.getExpressions().toString().contains("unknown@method@"))
				{
					containUnknown = true;
					update++;
					parameterize = true;
				}
				if(up.getWhere().toString().contains("unknown@method@"))
				{					
					//String[] where = up.getWhere().toString().split("=")
					String where = up.getWhere().toString().toLowerCase();
					
					if(where.contains(" and "))
					{
						String[] clause = where.split(" and ");
						for(String exp : clause)
						{
							if(exp.contains("="))
							{
								String right = exp.split("=")[1];
								if(right.contains("unknown@method@"))
								{
									if(!containUnknown)
									{
										containUnknown = true;
										update++;
										parameterize = true;
									}
								}
							}
						}
					}
				}
				
			}
			else if(statement instanceof Delete)
			{
				Delete de = (Delete) statement;
				String where = de.getWhere().toString().toLowerCase();
				boolean containUnknown = false;
				if(where.contains(" and "))
				{
					String[] clause = where.split(" and ");
					for(String exp : clause)
					{
						if(exp.contains("="))
						{
							String right = exp.split("=")[1];
							if(right.contains("unknown@method@"))
							{
								if(!containUnknown)
								{
									containUnknown = true;
									delete++;
									parameterize = true;
								}
							}
						}
					}
				}
			}
			else if(statement instanceof Replace)
			{
				Replace re = (Replace) statement;
				ExpressionList list = (ExpressionList) re.getItemsList();
				if(list.getExpressions().toString().contains("unknown@method@"))
				{
					//System.out.println(list.getExpressions());
					replace++;
					parameterize = true;
				}
			}
			else if(statement instanceof Select)
			{
				Select se = (Select) statement;
		
				PlainSelect pse = (PlainSelect) se.getSelectBody();
				
				String where = pse.getWhere().toString().toLowerCase();
				if(where.contains("unknown@method@"))
				{
					//System.out.println(where);
					boolean containUnknown = false;
					if(where.contains(" and "))
					{
						String[] clause = where.split(" and ");
						for(String exp : clause)
						{
							if(exp.contains("="))
							{
								String right = exp.split("=")[1];
								if(right.contains("unknown@method@"))
								{
									if(!containUnknown)
									{
										
						
										containUnknown = true;
										select++;
										parameterize = true;
									}
								}
							}
						}
					}
				}
				
			}


			parse++;
			
		} catch (Throwable e) {
			
			//System.out.println(sql);
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		return parameterize;
	}
	
	
	
}
