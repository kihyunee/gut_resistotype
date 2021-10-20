import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Hashtable;

public class PrepRichTitledFeatureFastaForDBHitORFs {

	
	private static void PrintUsage(){
		System.out.println("java PrepRichTitledFeatureFastaForDBHitORFs");
		System.out.println("\tThis is used to prepare the sequences of DB-annotated ORFs in either genomic or metagenomic contigs");
		System.out.println("\twhen you want to put a lot of information in the title lines");
		System.out.println("\tThe code will extract feature sequences for one sample at a time, while sample could mean a metagenome sample or a genome strain");
		System.out.println("");
		System.out.println("\tWriten in the title lines: ORF ID, Sample name, Genome ID, Species, Taxonomy, Dataset tag, annotation");
		System.out.println("\tInformation headers (all info is writen in {header}={value} format. except for ORF ID which is the first field )");
		System.out.println("\tInformation headers used = sample, dataset, genomeBin or refGenome, SGB or refSpecies, taxonomy, any annotation DB title");
		System.out.println("");
		System.out.println("\tOf course, in many cases not all of these field values are available. When not available 'TBD' is recorded");
		System.out.println("\tBasic invariable inputs");
		System.out.println("\t	-s [(text label) sample name]");
		System.out.println("\t	-d [(text label) labeling on data set level -- as you might merge the feature sequences from different data sets]");
		System.out.println("\t	-f [(file path) ORF fasta; fna or faa or whatever depending on your need]");
		System.out.println("\t	-annot [(file path) target DB blast hit file]");
		System.out.println("\t	-annotTitle [(text label) annotation target DB name; ex. CARD, COG, ...; to be appended before annotation value (like. {DBNAME}={ANNOTVALUE})]");
		System.out.println("\t	-o [output]");
		System.out.println("\t	-genometype [(text label) can be genomeBin or refGenome]");
		System.out.println("\t	-sptype [(text label) can be SGB or refSpecies]");
		System.out.println("");
		System.out.println("\tVariable inputs");
		System.out.println("only one of -wg and -cgb should be used and one of them must be provided");
		System.out.println("\t	-wg [(text label) genome ID/accession; when the sample is a single isolate genome ]");
		System.out.println("\t	-cgb [(file path) contig to genomic bin mapping file]");
		System.out.println("\tOther variable input: for these, TBD can be given when some of them are not specified");
		System.out.println("\t	-splabel [(optional)(file path) column 1 = genome / genomic bin ID;  columne 2 = species level label (I specifically want SGB ID for MAGs)]");
		System.out.println("\t	-sptax [(optional; overrided by -gtax)(file path) column 1 = species label;  columne 2 = full taxonomy, correctly ranked]");
		System.out.println("\t	-gtax [(optional; override -sptax)(file path) column 1 = genome / genomic bin ID;  columne 2 = full taxonomy, correctly ranked]");
	}
	
	static String sep=";";	// separator to be used to delimit the fields in output fasta title
	
	static String sample = "";
	static String orfFasta = "";
	static String genomeIdLabel;
	static String contigGenomebinMap;
	static String genometype;
	static String genomeTaxonomyMap;
	static String splabelTaxonomyMap;
	static String genomeSplabelMap;
	static String sptype;
	static String datasetLabel;
	static String annotBlastFile;
	static String annotTitle;
	static String out;
	
	static boolean isContigGenomeMapped=false;	//if false (==if -cgb is not given), it means that the data is from isolate genome and -wg label was provided;  
	//if true (-cgb specified), then each contig must be mapped to a genome or left as UNBINNED
	static boolean isContigGenomeMapPresent=false;
	//because in some cases none of the contigs in the sample had been binned
	//	in which case contig-bin mapping file would not be present
	
	static boolean isGenomeTaxonomyMapped=false;	
	static boolean isSplabelTaxonomyMapped=false;
	//if both are false, that means no taxonomy for genome provided; taxonomy become "TBD"
	//if one of them present; taxonomy is mapped from genome or species label
	//if both present; genome-taxonomy mapping is used rather than species label-taxonomy mapping
	static boolean isGenomeTaxonomyMapPresent=false;
	static boolean isSplabelTaxonomyMapPresent=false;
	//because in some cases none of the contigs in the sample had been binned
	//	in which case neither genome taxonomy map or species taxonomy map would be present
	
	static boolean isGenomeSplabelMapped=false;	//if false, that means no species label (SGB or so) given genomes; species label become "TBD"
	static boolean isGenomeSplabelMapPresent=false;
	//because in some cases none of the contigs in the sample had been binned
	//	in which case genome species map would not be present

	private static boolean GetInputFromArgs(String[] inputArgs){
		ArgumentBean ab = new ArgumentBean();
		ab.getArguments(inputArgs);

		if(ab.doesHave("-s")){
			sample = ab.returnValueOf("-s");
		}else return false;
		if(ab.doesHave("-f")){
			orfFasta = ab.returnValueOf("-f");
		}else return false;
		if(ab.doesHave("-annot")){
			annotBlastFile = ab.returnValueOf("-annot");
		}else return false;
		if(ab.doesHave("-annotTitle")){
			annotTitle = ab.returnValueOf("-annotTitle");
		}else return false;
		if(ab.doesHave("-o")){
			out = ab.returnValueOf("-o");
		}else return false;
		if(ab.doesHave("-d")){
			datasetLabel = ab.returnValueOf("-d");
		}else return false;
		if(ab.doesHave("-genometype")){
			genometype = ab.returnValueOf("-genometype");
			if(!genometype.startsWith("genomeBin") && !genometype.startsWith("refGenome")) {
				System.out.println("-genometype should be one of these two:  genomeBin or refGenome");
				return false;
			}
		}else return false;
		if(ab.doesHave("-sptype")){
			sptype = ab.returnValueOf("-sptype");
			if(!sptype.startsWith("SGB") && !sptype.startsWith("refSpecies")) {
				System.out.println("-sptype should be one of these two:  SGB or refSpecies");
				return false;
			}
		}else return false;

		
		if(ab.doesHave("-cgb")){
			contigGenomebinMap = ab.returnValueOf("-cgb");
			isContigGenomeMapped=true;
			File cgbF = new File(contigGenomebinMap);
			if(cgbF.exists()) {
				isContigGenomeMapPresent = true;
			}else {
				isContigGenomeMapPresent = false;
			}
		}
		else if(ab.doesHave("-wg")){
			genomeIdLabel = ab.returnValueOf("-wg");
			isContigGenomeMapped=false;
		}else return false;
		if(ab.doesHave("-cgb") && ab.doesHave("-wg")){
			System.out.println("Only one of -wg and -cgb can be defined. Can't use both of them.");
			return false;
		}
		
		if(ab.doesHave("-gtax")){
			genomeTaxonomyMap = ab.returnValueOf("-gtax");
			isGenomeTaxonomyMapped=true;
			File cgbF = new File(genomeTaxonomyMap);
			if(cgbF.exists()) {
				isGenomeTaxonomyMapPresent = true;
			}else {
				isGenomeTaxonomyMapPresent = false;
			}
		}else if(ab.doesHave("-sptax")){
			splabelTaxonomyMap = ab.returnValueOf("-sptax");
			isSplabelTaxonomyMapped=true;
			File cgbF = new File(splabelTaxonomyMap);
			if(cgbF.exists()) {
				isSplabelTaxonomyMapPresent = true;
			}else {
				isSplabelTaxonomyMapPresent = false;
			}
		}
		
		if(ab.doesHave("-splabel")){
			genomeSplabelMap = ab.returnValueOf("-splabel");
			isGenomeSplabelMapped=true;
			File cgbF = new File(genomeSplabelMap);
			if(cgbF.exists()) {
				isGenomeSplabelMapPresent = true;
			}else {
				isGenomeSplabelMapPresent = false;
			}
		}else {
			isGenomeSplabelMapped=false;
		}
		
		
		return true;
	}		
	
	
	public static void main(String[] args) throws IOException, InterruptedException, SQLException {
		if(!GetInputFromArgs(args)){
			PrintUsage();	return;
		}
		
		//	make directory for output file if there is absent directory in the path to the output
		ArrayList<String> requiredDirs = PathDirList(out);
		for(int i=0;i<requiredDirs.size();i++) {
			File dirF = new File(requiredDirs.get(i));
			if(!dirF.exists()) {
				dirF.mkdir();
			}
		}
		
		
		// annotation of ORFs
		Hashtable<String, String> orfHitHash = HashTableColumns(annotBlastFile, 1, 2, false, "\t");
		
		
		// now we have a list of ORFs that are to be collected (keys of orfHitHash)
		//	and their annotations (values of orfHitHash)
		//	and we know which ORFs are not to be collected (keys not found in orfHitHash)
		// some optional inputs should be mapped through hashtable
		Hashtable<String, String> contigBinHash = new Hashtable<String, String>();
		if(isContigGenomeMapped && isContigGenomeMapPresent) {
			contigBinHash = HashTableColumns(contigGenomebinMap, 1, 2, false, "\t");
		}
		
		Hashtable<String, String> genomeSplabelHash = new Hashtable<String, String>();
		if(isGenomeSplabelMapped && isGenomeSplabelMapPresent) {
			genomeSplabelHash = HashTableColumns(genomeSplabelMap, 1, 2, false, "\t");
		}
		
		Hashtable<String, String> genomeTaxonomyHash = new Hashtable<String, String>();
		Hashtable<String, String> splabelTaxonomyHash = new Hashtable<String, String>();
		
		if(isGenomeTaxonomyMapped) {
			if(isGenomeTaxonomyMapPresent)genomeTaxonomyHash = HashTableColumns(genomeTaxonomyMap, 1, 2, false, "\t");
		}else if(isSplabelTaxonomyMapped) {
			if(isSplabelTaxonomyMapPresent)splabelTaxonomyHash = HashTableColumns(splabelTaxonomyMap, 1, 2, false, "\t");
		}
		
		
		// simply read through ORF fasta file; write when an ORF should be writen in output
		FileWriter writeFasta = GiveMeFw(out);
		BufferedReader readFasta = GiveMeBr(orfFasta);
		String line = readFasta.readLine();
		while(line!=null) {
			if (line.startsWith(">")) {
				String orfid = line.substring(1).split(" ")[0];
				line = readFasta.readLine();
				StringBuilder seq = new StringBuilder();
				while(line!=null && !line.startsWith(">")) {
					seq.append(line);
					line = readFasta.readLine();
				}
				
				if(orfHitHash.containsKey(orfid)) {
					
					//	write in output?
					//	>{ORF ID};{Sample name};{Genome ID};{Species or SGB};{Taxonomy};{Dataset tag};{annotation}
					
					//1.	ORF ID: got it here as String orfid

					//2.	Sample name: got it from args as String sample

					//3.	Genome ID:	need mapping from contig to bin / or got it already from args as String genomeIdLabel
					String genomeLabel="TBD";
					if(isContigGenomeMapped) {
						// Genome ID label is mapped from contig to bin; contigs can be UNBINNED   
						genomeLabel = "UNBINNED";
						if(isContigGenomeMapPresent) {
							String contigid = CutOrfIdContigPart(orfid);
							if(contigBinHash.containsKey(contigid))genomeLabel = contigBinHash.get(contigid);
						}
						
					}else {
						// Genome ID: got it already from args as String genomeIdLabel
						genomeLabel = genomeIdLabel;
					}
					
					//4.	Species or SGB: mapping FROM bin or genome ACC TO sgb id or other label
					String speciesLabel = "TBD";
					if(isGenomeSplabelMapped) {
						//	genome-level UNBINNED contigs will remain as UNBINNED at the level of species / SGB label too
						if(genomeLabel.startsWith("UNBINNED")) {
							speciesLabel = "UNBINNED";
						}else {
							if(isGenomeSplabelMapPresent) {
								speciesLabel = genomeSplabelHash.get(genomeLabel);
							}else {
								speciesLabel="UNBINNED";
							}
						}
					}
					
					//5.	taxonomy: need it <-- need mapping from genome ACC or bin to fully ranked taxonomy
					String taxonomyString = "TBD";
					if(isGenomeTaxonomyMapped) {
						//	genome-level UNBINNED contigs will remain as TBD at taxonomy level too
						if(genomeLabel.startsWith("UNBINNED")) {
							taxonomyString = "UNBINNED";
						}else {
							if(isGenomeTaxonomyMapPresent) {
								taxonomyString = genomeTaxonomyHash.get(genomeLabel);
							}else {
								taxonomyString = "UNBINNED";
							}
						}
					}else if(isSplabelTaxonomyMapped) {
						// species-level TBD contigs will remain as TBD at taxonomy level too
						if(speciesLabel.startsWith("TBD")) {
							taxonomyString = "TBD";
						}else if(speciesLabel.startsWith("UNBINNED")) {
							taxonomyString = "UNBINNED";
						}else {
							if(isSplabelTaxonomyMapPresent) {
								taxonomyString = splabelTaxonomyHash.get(speciesLabel);
							}else {
								taxonomyString = "UNBINNED";
							}						}
					}
					
					
					//	dataset tag: got it from args as String datasetLabel
					//	annotation; got it here as orfHitHash.get(orfid)
					
					writeFasta.write(">"+orfid+sep+"sample="+sample+sep+genometype+"="+genomeLabel+sep+sptype+"="+speciesLabel+sep+"taxonomy="+taxonomyString+sep+"dataset="+datasetLabel+sep+annotTitle+"="+orfHitHash.get(orfid)+"\n");
					writeFasta.write(seq.toString()+"\n");
				}
				
			}
			else {
				line = readFasta.readLine();
			}
			
		}
		readFasta.close();
		writeFasta.close();
		
		
	}
	
	
	
	private static ArrayList<String> PathDirList(String fpath) {
		ArrayList<Integer> tolist = new ArrayList<Integer>();
		ArrayList<String> dirlist = new ArrayList<String>();
		int len=fpath.length();
		for(int i=0;i<len;i++) {
			if(fpath.charAt(i)=='/')tolist.add(i);
		}
		for(int i=0;i<tolist.size();i++) {
			dirlist.add(fpath.substring(0, tolist.get(i)));
		}
		return dirlist;
	}


	private static String CutOrfIdContigPart(String orf) {
		int to=0;
		int len=orf.length();
		for(int i=len-1;i>-1;i--) {
			if(orf.charAt(i)=='_') {
				to=i;
				break;
			}
		}
		return orf.substring(0, to);
	}


	private static Hashtable<String, String> HashTableColumns(String tbl, int key, int val, boolean header, String sep) throws IOException {
		Hashtable<String, String> hash = new Hashtable<String, String>();
		BufferedReader br = GiveMeBr(tbl);
		String line = br.readLine();
		if(header)line = br.readLine();
		while(line!=null){
			if(line.startsWith("#")) {
				line = br.readLine(); 	continue;
			}
			String[] split = line.split(sep);
			if(split.length<val) hash.put(split[key-1], "NA");
			else	hash.put(split[key-1], split[val-1]);
			line = br.readLine();
		}
		return hash;
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



