import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;


public class BlastResultFilterLkh {
	
	//-in [blast result outfmt 6]
	//-nonself [T to exclude self hit]
	//-idcut [minimum identity cutoff]
	
	
	private static void PrintUsage(){
		System.out.println("java BlastResultFilterLkh");
		System.out.println("\t-in [blast result outfmt 6]");
		System.out.println("\t-nonself [T to exclude self hit]");
		System.out.println("\t-idcut [minimum identity cutoff]");
		System.out.println("\t-idcutMax [maximum alloowable identity cutoff]");
		System.out.println("\t-lengthCut [minimum alignment length]");
		System.out.println("\t-qcovCut [minimum query coverage percent]");
		System.out.println("\t-qfasta [query fasta (required if qcovCut was given)]");
		System.out.println("\t-dcovCut [minimum DB coverage percent]");
		System.out.println("\t-dfasta [DB fasta (required if dcovCut was given)]");
		System.out.println("\t-maxEvalue [maximum Evalue as String]");
		System.out.println("\t-out [output]");
	}
	
	static String inblast = "";
	static boolean excludeSelf = false;
	static float idcut = 0;
	static float idcutMax=110;
	static String outblast = "";
	static int alnlengthCut = 0;
	static float qcovCut = 0;
	static String qfasta = "";
	static float dcovCut = 0;
	static float maxEvalue = 1;
	static String dfasta = "";
	
	private static boolean GetInputFromArgs(String[] inputArgs){
		ArgumentBean ab = new ArgumentBean();
		ab.getArguments(inputArgs);
		
		if(ab.doesHave("-in")){
			inblast = ab.returnValueOf("-in");
		}else return false;
		
		if(ab.doesHave("-nonself")){
			String s = ab.returnValueOf("-nonself");
			if(s.startsWith("T")){
				excludeSelf = true;
			}
		}
		if(ab.doesHave("-maxEvalue")){
			String s = ab.returnValueOf("-maxEvalue");
			maxEvalue = Float.parseFloat(s);
		}
		if(ab.doesHave("-idcut")){
			String s = ab.returnValueOf("-idcut");
			idcut = Float.parseFloat(s);
		}
		if(ab.doesHave("-idcutMax")){
			String s = ab.returnValueOf("-idcutMax");
			idcutMax = Float.parseFloat(s);
		}
		if(ab.doesHave("-lengthCut")){
			String s = ab.returnValueOf("-lengthCut");
			alnlengthCut = Integer.parseInt(s);
		}
		
		if(ab.doesHave("-qcovCut")){
			String s = ab.returnValueOf("-qcovCut");
			qcovCut = Float.parseFloat(s);
			
			if(ab.doesHave("-qfasta")){
				qfasta = ab.returnValueOf("-qfasta");
			}else	return false;
		}
		
		if(ab.doesHave("-dcovCut")){
			String s = ab.returnValueOf("-dcovCut");
			dcovCut = Float.parseFloat(s);
			
			if(ab.doesHave("-dfasta")){
				dfasta = ab.returnValueOf("-dfasta");
			}else	return false;
		}

		if(ab.doesHave("-out")){
			outblast = ab.returnValueOf("-out");
		}else return false;
		
		return true;
	}		
	
	
	static Hashtable<String, Integer> queryLengthHash;
	static Hashtable<String, Integer> dbLengthHash;
	
	
	public static void main(String[] args) throws IOException, SQLException, InterruptedException {
		
		if(!GetInputFromArgs(args)){
			PrintUsage();	return;
		}
		
		System.out.println("Filters:");
		System.out.println("Identity cut = "+idcut);
		System.out.println("Identity cut max = "+idcutMax);
		System.out.println("Alignment length cut = "+alnlengthCut);
		System.out.println("query coverage percent cut = "+qcovCut);
		System.out.println("DB (ref) coverage percent cut = "+dcovCut);
		
		System.out.println("collect queries in result");
//		ArrayList<String> queries = CollectQueriesInBlastResult(inblast);
		
		if(qcovCut>0){
			System.out.println("query id length hash");
//			queryLengthHash = IdToLengthHash(qfasta, true, queries);
			queryLengthHash = IdToLengthHash(qfasta, true);
		}
		if(dcovCut>0){
			System.out.println("DB id length hash");
			dbLengthHash = IdToLengthHash(dfasta, true);
		}
		
		
		System.out.println("read blast file line by line");
		FileWriter fw = new FileWriter(new File(outblast));
		BufferedReader br = GiveMeBr(inblast);
		
		String line = br.readLine();
		while(line!=null){
			boolean exclude = false;
			
			String[] result = line.split("\t");
			String query = result[0];
			String hit = result[1];
			float id = Float.parseFloat(result[2]);
			int alnlength = Integer.parseInt(result[3]);
			float evalue = Float.parseFloat(result[10]);
			
			if(excludeSelf){
				if(query.startsWith(hit) && hit.startsWith(query))exclude = true;
			}
			
			if(id < idcut)exclude = true;
			if(id > idcutMax)exclude = true;
			
			if(alnlength < alnlengthCut)exclude = true;
			
			if(qcovCut>0){
				float qcov = 100*(float)alnlength/(float)queryLengthHash.get(query);
				if(qcov < qcovCut)exclude = true;
			}
			
			if(dcovCut>0){
				float dcov = 100*(float)alnlength/(float)dbLengthHash.get(hit);
				if(dcov < dcovCut)exclude = true;
			}
			
			if(evalue > maxEvalue)exclude = true;
			
			if(!exclude){
				fw.write(line+"\n");
			}
			
			line = br.readLine();
		}
		
		br.close();
		fw.close();
	}
	
	private static ArrayList<String> CollectQueriesInBlastResult(String blast) throws IOException {
		
		ArrayList<String> queryList = new ArrayList<String>();
		BlastTabularReaderLkh btr = new BlastTabularReaderLkh(blast);
		while(btr.readNextHit()){
			queryList.add(btr.getQseqid());
		}
		btr.close();
		
		return queryList;
	}

	private static Hashtable<String, Integer> IdToLengthHash(String fasta, boolean stripSpace, ArrayList<String> ids) throws IOException {
		Hashtable<String, Integer> hash = new Hashtable<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(new File(fasta)));
		String line = br.readLine();
		while(line!=null){
			if(line.startsWith(">")){
				String id = line.substring(1);
				int length = 0;
				line = br.readLine();
				while(line!=null && !line.startsWith(">")){
					length += line.length();
					line = br.readLine();
				}
				if(stripSpace && id.indexOf(" ")>0){
					String leanid = id.substring(0, id.indexOf(" "));
					if(ids.contains(leanid))hash.put(leanid, length);
				}else{
					if(ids.contains(id))hash.put(id, length);
				}
				
			}else{
				line = br.readLine();
			}
		}
		br.close();
		return hash;
	}
	
	
	private static Hashtable<String, Integer> IdToLengthHash(String fasta, boolean stripSpace) throws IOException {
		Hashtable<String, Integer> hash = new Hashtable<String, Integer>();
		BufferedReader br = new BufferedReader(new FileReader(new File(fasta)));
		String line = br.readLine();
		while(line!=null){
			if(line.startsWith(">")){
				String id = line.substring(1);
				int length = 0;
				line = br.readLine();
				while(line!=null && !line.startsWith(">")){
					length += line.length();
					line = br.readLine();
				}
				if(stripSpace && id.indexOf(" ")>0){
					String leanid = id.substring(0, id.indexOf(" "));
					hash.put(leanid, length);
				}else{
					hash.put(id, length);
				}
				
			}else{
				line = br.readLine();
			}
		}
		br.close();
		return hash;
	}
	
	
	private static BufferedReader GiveMeBr(String s) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(new File(s)));
		return br;
	}
	
}
