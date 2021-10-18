import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;


public class TableFilteringRowsLkh {
	
	private static void PrintUsage(){
		System.out.println("java TableFilteringRowsLkh");
		System.out.println("\t-tbl [tab-delimited table file]");
		System.out.println("\t-sep [t or c (tab or comma)]");
		//assume tab-delimited table
		System.out.println("\t-headerRow [T/F]");
		System.out.println("\t-col [column index 1-based]");
		System.out.println("\tOne of the two ways to specify the tarets");
		System.out.println("\t-target [target value that should be in the specified column]");
		System.out.println("\t-targetList [a file containing the list of target values]");
		System.out.println("\t-targetListHeader [(optional) if target List file contains header row]");
		System.out.println("\t-out [output]");
		System.out.println("\t-ex [(optional) T to exclude rather than include the targets]");
		System.out.println("\t-nr [(optional) T to include only the first encountered row matched for each target value; Not applicable if -ex T]");
		System.out.println("\t-ubex [(optional) T in special case: values have underbar extenstion, so {target}_{XX} should be searched for ]");
	}
	
	static String tbl;
	static String sep;
	static String target;
	static String targetList;
	static boolean targetListHeader = false;
	static boolean list = false;
	static String out;
	static int col;
	static boolean headerRow = false;
	static boolean ex=false;
	static boolean nr=false;
	static boolean ubex=false;
	
	private static boolean GetInputFromArgs(String[] inputArgs){
		ArgumentBean ab = new ArgumentBean();
		ab.getArguments(inputArgs);
		
		if(ab.doesHave("-tbl")){
			tbl = ab.returnValueOf("-tbl");
		}else	return false;
		
		if(ab.doesHave("-sep")){
			String s = ab.returnValueOf("-sep");
			if(s.startsWith("c")){
				sep = ",";
			}else if(s.startsWith("t")){
				sep = "\t";
			}else{
				return false;
			}
		}else	return false;
		
		if(ab.doesHave("-col")){
			String s = ab.returnValueOf("-col");
			col = Integer.parseInt(s);
		}else	return false;
		

		if(ab.doesHave("-headerRow")){
			String s = ab.returnValueOf("-headerRow");
			if(s.startsWith("T")){
				headerRow = true;
			}
		}else	return false;
		
		if(ab.doesHave("-targetList")){
			targetList = ab.returnValueOf("-targetList");
			list = true;
		}
		else if(ab.doesHave("-target")){
			target = ab.returnValueOf("-target");
		}else	return false;
		
		if(ab.doesHave("-targetListHeader")){
			String s = ab.returnValueOf("-targetListHeader");
			if(s.startsWith("T"))targetListHeader = true;
		}
		
		if(ab.doesHave("-out")){
			out = ab.returnValueOf("-out");
		}else	return false;
		
		if(ab.doesHave("-ex")){
			String s= ab.returnValueOf("-ex");
			if(s.startsWith("T"))ex=true;
		}
		if(ab.doesHave("-nr")){
			String s= ab.returnValueOf("-nr");
			if(s.startsWith("T"))nr=true;
		}
		if(ab.doesHave("-ubex")){
			String s= ab.returnValueOf("-ubex");
			if(s.startsWith("T"))ubex=true;
		}
		return true;
	}
	
	static Hashtable<String, Boolean> targetsHash = new Hashtable<String, Boolean>();
	static Hashtable<String, Boolean> redundanceMap = new Hashtable<String, Boolean>();
	
	public static void main(String[] args) throws IOException {
		
		if(!GetInputFromArgs(args)){
			PrintUsage();	return;
		}
		
		if(list){
			targetsHash = ColumnHashTrue(targetList, 1, targetListHeader, sep);
		}else{
			targetsHash.put(target, true);
		}
		if(nr) {
			Iterator<String> targetIter = targetsHash.keySet().iterator();
			while(targetIter.hasNext()) {
				redundanceMap.put(targetIter.next(), false);
			}
		}
		
		
		FileWriter fw = new FileWriter(new File(out));
		BufferedReader br = new BufferedReader(new FileReader(new File(tbl)));
		
		String line = br.readLine();
		if(headerRow){
			fw.write(line+"\n");
			line = br.readLine();
		}
		
		while(line!=null){
			String colval="";
			String colvalpro = line.split(sep)[col-1];
			if(ubex) {
				colval=RemoveUnderbarExtension(colvalpro);
			}else {
				colval=colvalpro;
			}
			if(!ex && targetsHash.containsKey(colval)){
				if(nr) {
					if(!redundanceMap.get(colval))fw.write(line+"\n");
				}else {
					fw.write(line+"\n");
				}
			}
			if(ex && !targetsHash.containsKey(colval)) {
				fw.write(line+"\n");
			}
			
			if(nr)redundanceMap.put(colval, true);
			line = br.readLine();
		}
		
		
		fw.close();
		br.close();
	}
	
	
	private static String RemoveUnderbarExtension(String s) {
		int to=0;
		for(int i=0;i<s.length();i++) {
			if(s.charAt(i)=='_')to=i;
		}
		return s.substring(0,to);
	}


	private static Hashtable<String, Boolean> ColumnHashTrue(String filename, int colIndex, boolean headerRow, String sep) throws IOException{
		Hashtable<String, Boolean> hash = new Hashtable<String, Boolean>();
		
    	BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
    	String s = br.readLine();
    	if(headerRow)s = br.readLine();
    	
    	while(s!=null){
    		if(s.indexOf(sep)==-1){
    			hash.put(s, true);
    		}else{
    			String[] split = s.split(sep);
    			hash.put(split[colIndex-1], true);
    		}
    		
    		s = br.readLine();
    	}
    	br.close();
		return hash;
	}


	private static ArrayList<String> ColumnAsList(String filename, int colIndex, boolean headerRow, String sep) throws IOException{
	    	
	    	ArrayList<String> list = new ArrayList<String>();
	    	BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
	    	String s = br.readLine();
	    	if(headerRow)s = br.readLine();
	    	
	    	while(s!=null){
	    		if(s.indexOf(sep)==-1){
	    			list.add(s);
	    		}else{
	    			String[] split = s.split(sep);
	    			list.add(split[colIndex-1]);
	    		}
	    		
	    		s = br.readLine();
	    	}
	    	br.close();
	    	
	    	return list;
	    }
}
