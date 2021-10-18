import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;

public class TableColumnReformatVerticalSepField {

	
	private static void PrintUsage(){
		System.out.println("java TableColumnReformatVerticalSepField");
		System.out.println("-in [input table containing a column which you want to re-shape, because it contains vertical-separated values (like X|01|Y|99) ]");
		System.out.println("-out [output table containing the values that you need instead of vertical-separated format]");
		System.out.println("-col [1-based index of target column]");
		System.out.println("-field [1-based index of target field within that column]");
		System.out.println("-header [T/F header row presence]");
	}
	
	static String input;
	static String output;
	static int col=0;
	static int field=0;
	static boolean header=false;
	
	private static boolean GetInputFromArgs(String[] inputArgs){
		ArgumentBean ab = new ArgumentBean();
		ab.getArguments(inputArgs);
		
		if(ab.doesHave("-in")){
			input = ab.returnValueOf("-in");
		}else return false;
		if(ab.doesHave("-out")){
			output = ab.returnValueOf("-out");
		}else return false;
		
		if(ab.doesHave("-col")){
			String s = ab.returnValueOf("-col");
			col = Integer.parseInt(s);
		}else return false;

		if(ab.doesHave("-field")){
			String s = ab.returnValueOf("-field");
			field = Integer.parseInt(s);
		}else return false;
		
		if(ab.doesHave("-header")){
			String s = ab.returnValueOf("-header");
			if(s.startsWith("T"))header = true;
		}
		return true;
	}
	
	public static void main(String[] args) throws IOException, InterruptedException, SQLException {
		if(!GetInputFromArgs(args)){
			PrintUsage();	return;
		}
		
		FileWriter fw= GiveMeFw(output);
		BufferedReader br = GiveMeBr(input);
		String line = br.readLine();
		if(header){
			fw.write(line+"\n");
			line = br.readLine();
		}
		
		while(line!=null){
			String[] split = line.split("\t");
			for(int i=0;i<split.length;i++){
				if(i>0)fw.write("\t");
				if(i==col-1){
					String dirty = split[i];
					String clean = "";
					if(dirty.length()<3){
						clean=dirty;
					}else{
						clean = EssentialPart(dirty, field);
					}
					fw.write(clean);
				}else{
					fw.write(split[i]);
				}
			}
			fw.write("\n");
			line = br.readLine();
		}
		
		br.close();
		fw.close();
		
	}
	
	private static String EssentialPart(String dirty, int index) {
		int sp=0;
		int from=0;
		for(int i=0;i<dirty.length();i++){
			if(dirty.charAt(i)=='|'){
				sp++;
				if(sp==index-1) {
					from=i+1;	break;
				}
			}
		}
		sp=0;
		int to=dirty.length();
		for(int i=0;i<dirty.length();i++){
			if(dirty.charAt(i)=='|'){
				sp++;
				if(sp==index) {
					to=i;	break;
				}
			}
		}
		return dirty.substring(from, to);
	}


	private static BufferedReader GiveMeBr(String s) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(s));
		return br;
	}
	
	private static FileWriter GiveMeFw(String s) throws IOException {
		FileWriter fw = new FileWriter(new File(s));
		return fw;
	}
}
