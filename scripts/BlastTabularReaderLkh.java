import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class BlastTabularReaderLkh {

	/*
	 Read blastp or blastn output with tabular format (-outfmt 6).
	  
	 outfmt 6 is :
	 qseqid sseqid pident length mismatch gapopen qstart qend sstart send evalue bitscore 
	 */
	
	static BufferedReader br;
	static String line;
	
	static String qseqid;
	static String sseqid;
	static float pident;
	static int length;
	static int mismatch;
	static int gapopen;
	static int qstart;
	static int qend;
	static int sstart;
	static int send;
	static float evalue;
	static float bitscore;
	static int sdirection;
	static int qdirection;
	
	static String currQseqid = null;
	static int currHitRank;
	
	public BlastTabularReaderLkh(String blastResult) throws IOException {
		br = new BufferedReader(new FileReader(new File(blastResult)));
	}

	public boolean readNextHit() throws IOException {
		line = br.readLine();
		while(line!=null && line.charAt(0)=='#') line = br.readLine();
		if(line==null)return false;
		
		String[] sp = line.split("\t");
		qseqid=sp[0];
		sseqid=sp[1];
		pident=Float.parseFloat(sp[2]);
		length=Integer.parseInt(sp[3]);
		mismatch=Integer.parseInt(sp[4]);
		gapopen=Integer.parseInt(sp[5]);
		qstart=Integer.parseInt(sp[6]);
		qend=Integer.parseInt(sp[7]);
		sstart=Integer.parseInt(sp[8]);
		send=Integer.parseInt(sp[9]);
		evalue=Float.parseFloat(sp[10]);
		bitscore=Float.parseFloat(sp[11]);
		if(sstart <= send)sdirection = 1;
		else	sdirection = -1;
		if(qstart <= qend)qdirection = 1;
		else	qdirection = -1;
		
		
		if(currQseqid==null){
			currQseqid = qseqid;
			currHitRank = 1;
		}
		else if(currQseqid.contentEquals(qseqid)){
			currHitRank++;
		}else{
			currQseqid = qseqid;
			currHitRank = 1;
		}
		
		return true;
	}
	

	public void close() throws IOException {
		br.close();
	}
	
	public String getQseqid(){
		return qseqid;
	}
	public String getSseqid(){
		return sseqid;
	}
	public float getPident(){
		return pident;
	}
	public int getLength(){
		return length;
	}
	public int getMismatch(){
		return mismatch;
	}
	public int getGapopen(){
		return gapopen;
	}
	public int getQstart(){
		return qstart;
	}
	public int getQend(){
		return qend;
	}
	public int getSstart(){
		return sstart;
	}
	public int getSend(){
		return send;
	}
	public float getEvalue(){
		return evalue;
	}
	public float getBitscore(){
		return bitscore;
	}
	public int getSdirection(){
		return sdirection;
	}
	public int getQdirection(){
		return qdirection;
	}
	public int getCurrHitRank(){
		return currHitRank;
	}

	public String getLine() {
		return line;
	}
	

	
}
