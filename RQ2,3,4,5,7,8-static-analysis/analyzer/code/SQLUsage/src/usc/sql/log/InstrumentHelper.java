package usc.sql.log;


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class InstrumentHelper {

	public static InputStream getInputStream1(HttpURLConnection urlCon) throws IOException
	{
		String a="";
		byte[] b1 = new byte[1];
		InputStream in = new BufferedInputStream(urlCon.getInputStream());
		BufferedReader reader = null;
		reader = new BufferedReader(new InputStreamReader(in));
		int cnt=0;
		String line = null;
		while ( (line = reader.readLine()) != null){
			a+=line;
		}
		System.out.println("Test0 response size (KB): " + a.getBytes().length);
		//note that StandardCharsets is included in java.nio, which is not supported by android sdk in default
		//InputStream stream = new ByteArrayInputStream(a.getBytes(StandardCharsets.UTF_8));
		InputStream stream = new ByteArrayInputStream(a.getBytes());
		return stream;
	}
	
	public static void printTime(){
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH:mm:ss").format(new Date());
		System.out.println("time: " + timeStamp);
	}
	/*
	public static void printFinerTimeBefore(){
		System.out.println("<start> <ThreadID: " + Thread.currentThread().getId() + "> <millisecond: " + System.currentTimeMillis() + "> <nanosecond: " + System.nanoTime() + ">");
		//System.out.println("success");
	}

	public static void printFinerTimeEnd(String sig, String para){
		Log.d("USCSQL","<end> <nanosecond: " + System.nanoTime() + "> <millisecond: " + System.currentTimeMillis() + ">"+ " <ThreadID: " + Thread.currentThread().getId() + ">");
		//System.out.println("success");
	}
	*/
	
	public static void printFinerTimeEnd(String sig){
		System.out.println("<end> <nanosecond: " + System.nanoTime() + "> <millisecond: " + System.currentTimeMillis() + ">"+
				" <Sig: " + sig + ">"+
				" <ThreadID: " + Thread.currentThread().getId() + ">");
		//System.out.println("success");
	}
	
	public static void printFinerTimeBefore(String sig,String para){
		System.out.println("<start> <ThreadID: " + Thread.currentThread().getId() + ">"+
	" <Sig: "+ sig + ">" + " <Para: "+para+">"+
	" <millisecond: " + System.currentTimeMillis() + "> <nanosecond: " + System.nanoTime() + ">");
		//System.out.println("success");
	}

	
	public static void main(String[] args){
		//<start> <ThreadID: 1> <millisecond: 1488581454069> <nanosecond: 1207574670102134>
		//<end> <ThreadID: 1> <nanosecond: 1207574670864549> <millisecond: 1488581454070>

		
		
		//printFinerTimeBefore("android.sqlite void insert()","No");
		printFinerTimeEnd("android.sqlite void insert()");
		System.out.println(TimeUnit.MILLISECONDS.convert(Long.valueOf("1207574670102134"), TimeUnit.NANOSECONDS));
		System.out.println(System.getProperty("java.class.path"));
		System.out.println(System.getProperty("java.class.path").split(":")[0]);
	}

}