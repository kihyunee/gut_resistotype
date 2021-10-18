import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;


public class TableTranslateColumnByDictionaryLkh {
	
	
	//input arguments
	//	-i [input table tab delimited]
	//	-icol [column index, 1-based, of the input table column to be translated]
	//	-iheader [T/F, whether or not the header row present in the input table]
	
	//dictionary arguments
	//	-d [a two column table, tab delimited]
	//	-dheader [T/F, whether or not the header row present in the dictionary table]
	//	-currentcol [1-based index of the column containing the current words]
	//	-destinycol [1-based index of the column containing the destiny words]
	
	//	-o [output table name]
	
	private static void PrintUsage(){
		System.out.println("java TableTranslateColumnByDictionaryLkh");
		System.out.println("input arguments");
		System.out.println("	-i [input table tab delimited]");
		System.out.println("	-icol [column index, 1-based, of the input table column to be translated]");
		System.out.println("	-wordsep [if present. c(omma) s(emicolon) v(ertical) the separator used within the single column, to separate multiple words]");
		System.out.println("	-iheader [T/F, whether or not the header row present in the input table]");
		
		System.out.println("dictionary arguments");
		System.out.println("	-d [table containing two columns (or more), tab delimited.  You can use COMMA to set more than one dictionary!! ]");
		System.out.println("	-dheader [T/F, whether or not the header row present in the dictionary table]");
		System.out.println("	-q [1-based index of the column containing the query words]");
		System.out.println("	-t [1-based index of the column containing the target words]");
		
		System.out.println("	-o [output table name]");
		System.out.println("	-na [(optional) specific value for non-translated cells : by default they are left as it was in input]");
		System.out.println("	-exna [T (optional) remove non-translated rows (which are absent in the dictionary)]");
		System.out.println("	-dcase [(only applied for special cases) 1=trim *filepath and space* from dictionary; ]");
	}
	
	static String input;
	static String output;
	static ArrayList<String> dictionaryFile;
	
	static String wordsep;
	static boolean wordsepPresent = false;
	
	static int icol;
	static int currentcol;
	static int destinycol;
	
	static boolean iheader;
	static boolean dheader;
	static boolean nagiven=false;
	static String na;
	static boolean exna=false;
	static int dcase=0;
	
	private static boolean GetInputFromArgs(String[] inputArgs) throws IOException{
		ArgumentBean ab = new ArgumentBean();
		ab.getArguments(inputArgs);
		
		if(ab.doesHave("-i")){
			input = ab.returnValueOf("-i");
		}else return false;
		
		if(ab.doesHave("-wordsep")){
			String s = ab.returnValueOf("-wordsep");
			wordsepPresent = true;
			if(s.startsWith("c"))wordsep = ",";
			if(s.startsWith("s"))wordsep = ";";
			if(s.startsWith("v"))wordsep = "|";
		
		}
		
		if(ab.doesHave("-o")){
			output = ab.returnValueOf("-o");
		}else return false;
		if(ab.doesHave("-na")){
			na = ab.returnValueOf("-na");
			nagiven=true;
		}
		if(ab.doesHave("-exna")){
			String s = ab.returnValueOf("-exna");
			if(s.startsWith("T"))exna=true;
		}
		if(ab.doesHave("-dcase")){
			String s = ab.returnValueOf("-dcase");
			dcase=Integer.parseInt(s);
		}
		
		if(ab.doesHave("-d")){
			dictionaryFile = new ArrayList<String>();
			String s = ab.returnValueOf("-d");
			if(s.indexOf(",")==-1){
				dictionaryFile.add(s);
			}else{
				String[] split = s.split(",");
				for(int i=0;i<split.length;i++)dictionaryFile.add(split[i]);
			}
		}else return false;
		
		if(ab.doesHave("-icol")){
			String s = ab.returnValueOf("-icol");
			icol = Integer.parseInt(s);
		}else return false;
		
		if(ab.doesHave("-q")){
			String s = ab.returnValueOf("-q");
			currentcol = Integer.parseInt(s);
		}else return false;
		
		if(ab.doesHave("-t")){
			String s = ab.returnValueOf("-t");
			destinycol = Integer.parseInt(s);
		}else return false;
		
		if(ab.doesHave("-iheader")){
			String s = ab.returnValueOf("-iheader");
			if(s.startsWith("T")){
				iheader = true;
			}else{
				iheader = false;
			}
		}else return false;

		if(ab.doesHave("-dheader")){
			String s = ab.returnValueOf("-dheader");
			if(s.startsWith("T")){
				dheader = true;
			}else{
				dheader = false;
			}
		}else return false;
	
		return true;
	}
	

	public static void main(String[] args) throws IOException {
		
		if(!GetInputFromArgs(args)){
			PrintUsage();	return;
		}
		
		Hashtable<String, String> dictionary = Dictionaries();
		
		BufferedReader br = GiveMeBr(input);
		FileWriter fw = new FileWriter(new File(output));
		int countTranslation = 0;
		int countExclusion=0;
		int wordless = 0;
		
		String line = br.readLine();
		
		if(iheader){
			fw.write(line+"\n");
			line = br.readLine();
		}
		
		while(line!=null){
			
			String[] split = line.split("\t");
			
			String orivalue = split[icol-1];
			String desvalue = orivalue;
			boolean writeit=true;

			if(orivalue.length()<1){
				wordless++;
			}else if(wordsepPresent){
				ArrayList<String> orivalues = new ArrayList<String>();
				ArrayList<String> desvalues = new ArrayList<String>();

				if(orivalue.indexOf(wordsep)==-1){
					orivalues.add(orivalue);
				}else{
					String[] wordsplit = orivalue.split(wordsep);
					for(int w=0; w<wordsplit.length; w++){
						orivalues.add(wordsplit[w]);
					}
				}
				
				if(exna)writeit=false;
				for(int w=0; w<orivalues.size(); w++){
					if(dictionary.containsKey(orivalues.get(w))){
						desvalues.add(dictionary.get(orivalues.get(w)));
						countTranslation++;
						writeit=true;
					}else{
						if(nagiven)desvalues.add(na);
						//System.out.println("Original Value out of dictionary  : "+orivalues.get(w));
					}
				}
				
				StringBuilder sb = new StringBuilder();
				for(int w=0; w<orivalues.size(); w++){
					if(w==0)sb.append(desvalues.get(w));
					else	sb.append(wordsep+desvalues.get(w));
				}
				
				desvalue = sb.toString();
			}else{
				if(exna)writeit=false;
				if(dictionary.containsKey(orivalue)){
					desvalue = dictionary.get(orivalue);
					countTranslation++;
					writeit=true;
				}else{
					if(nagiven)desvalue=na;
					//System.out.println("Original Value out of dictionary  : "+orivalue);
				}
			}
			
			if(writeit) {
				for(int i=0; i<split.length; i++){
					if(i>0)fw.write("\t");
					if(i==icol-1){
						fw.write(desvalue);
					}else {
						fw.write(split[i]);
					}
				}
				fw.write("\n");
			}else {
				countExclusion++;
			}
			line = br.readLine();
		}
		
		br.close();
		fw.close();
		System.out.println("Translated "+countTranslation);
		System.out.println("Excluded "+countExclusion);
		System.out.println("Wordless rows = "+wordless);
	}
	
	
	
	private static Hashtable<String, String> Dictionaries() throws IOException {
		Hashtable<String, String> dictionary = new Hashtable<String, String>();
		
		for(int i=0;i<dictionaryFile.size(); i++){
			Hashtable<String, String> dicA = HashTableColumns(dictionaryFile.get(i), dheader, currentcol, destinycol, "\t");
			Iterator<String> keys = dicA.keySet().iterator();
			if(dcase==1) {
				while(keys.hasNext()){
					String key = keys.next();
					String leankey=TrimFilePathSpace(key);
					String leanval=TrimFilePathSpace(dicA.get(key));
					if(!dictionary.containsKey(leankey))dictionary.put(leankey, leanval);
				}
			}else {
				while(keys.hasNext()){
					String key = keys.next();
					if(!dictionary.containsKey(key))dictionary.put(key, dicA.get(key));
				}
			}
		}
		
		return dictionary;
	}


	private static String TrimFilePathSpace(String s) {
		int from=0;
		int to=s.length();
		for(int i=0;i<to;i++) {
			if(s.charAt(i)==' ') {
				to=i;	break;
			}
		}
		for(int i=0;i<to;i++) {
			if(s.charAt(i)=='/')from=i+1;
		}
		return s.substring(from,to);
	}


	private static Hashtable<String, String> HashTableColumns(String tbl, boolean header, int keycol, int valcol, String sep) 
			throws IOException {
		Hashtable<String, String> hash = new Hashtable<String, String>();
		
		BufferedReader br = GiveMeBr(tbl);
		String line = br.readLine();
		if(header)line = br.readLine();
		
		while(line!=null){
			String[] split = line.split(sep);
			String key = split[keycol-1];
			String val = split[valcol-1];
			hash.put(key, val);
			
			line = br.readLine();
		}
		br.close();
		
		return hash;
	}
	
	
	
	
	private static BufferedReader GiveMeBr(String s) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(s)));
		return br;
	}


}
