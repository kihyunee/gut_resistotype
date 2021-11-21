# About

**This is the repository for the analysis scripts and data files used in the following study:**

Population-level impacts of antibiotic usage on the human gut microbiome. *Unpublished.* (2021) Kihyun Lee, Sebastien Raguideau, Kimmo Sir`en, Francesco Asnicar, Fabio Cumbo, Falk Hildebrand, Nicola Segata, Chang-Jun Cha, Christopher Quince





# Data files

### Human microbiome metagenome assemblies

Human microbiome metagenome assemblies previously published in [Pasolli et al., 2019, Cell 176, 649–662](https://doi.org/10.1016/j.cell.2019.01.001)**

Fasta files can be accessed following the instructions provided in http://segatalab.cibio.unitn.it/data/Pasolli_et_al.html

Sample metadata provided in Pasolli et al. 2019 was additionally curated with respect to (a) health and disease status of the subject and (b) antibiotic usage preceding the sampling. The final metadata used in our study is provided in this link: [microbiome sample metadata](https://github.com/kihyunee/gut_resistotype/blob/data/sfile-microbiome-sample-detail.tsv)

### NCBI RefSeq prokaryotic genome assemblies 

We accessed all prokaryotic genomes available in NCBI RefSeq at 2019/04/19.

Assembly accession numbers (RefSeq GCF_* / GenBank GCA_*) of the prokaryotic genomes analyzed in our study is provided in this link: [genome accession list](https://github.com/kihyunee/gut_resistotype/blob/data/refseq_genbank_assembly_acc_pair.nover.tsv)

### Modified version of CARD used as the references for annotation of antibiotic resistance genes

We used the database of protein sequences for known ARGs, which was created by modifying the October 2017 version of [CARD](https://card.mcmaster.ca/), cite [Jia et al. 2017](https://doi.org/10.1093/nar/gkw1004)

For this modified version of CARD, the following files are provided in this repository.

(1) [fasta file of protein homolog model reference sequences after filtering out variant/mutation-dependent resistance genes](https://github.com/kihyunee/gut_resistotype/blob/data/protein_fasta_protein_homolog_model.refined.legit.fasta)

(2) [protein accessions to ARG families map](https://github.com/kihyunee/gut_resistotype/blob/data/protein_fasta_protein_homolog_model.refined.accession_2_ARG_cluster_assign.tab)

(3) [ARG families to antibiotics map](https://github.com/kihyunee/gut_resistotype/blob/data/ClusterAssign_to_Revised_Antibiotic.map)

(4) [ARG families to mechanisms map](https://github.com/kihyunee/gut_resistotype/blob/data/ClusterAssign_to_Revised_Mechanism.map)

(5) [list of 752 ARG family names covered in our annotation DB](https://github.com/kihyunee/gut_resistotype/blob/data/reference_ARG_assigns.txt)


### COG protein sequences

We used COG, the 2014 update version, accessed through NCBI FTP at December 2018 using the following link [COG 2014 protein fasta](https://ftp.ncbi.nih.gov/pub/COG/COG2014/data/prot2003-2014.fa.gz)

Our analysis using COG is focused at the list of [40 universally single-copy orthologs, SCGs](https://github.com/kihyunee/gut_resistotype/blob/data/list_speci_universal_single_copy_genes.cogs)

Mapping from gi numbers (used in COG protein sequence headers) to COG numbers is needed and provided in this link:[gi-to-COG mapping](https://www.dropbox.com/s/srr6tzim5d4f324/gi_to_cog.tab?dl=0)

shorter version for [gi-to-COG mapping for the SCGs](https://github.com/kihyunee/gut_resistotype/blob/data/gi_to_cog.tab.scg_only)








# Tools used in our computational analysis steps

Below are the commands and scripts used in our main analytical steps.

Script files called in the commands below can be found in the script section ([scripts](https://github.com/kihyunee/gut_resistotype/tree/main/scripts)) of this repository.

External tools called in the commands are:
- diamond: [diamond](https://github.com/bbuchfink/diamond)
- prodigal: [prodigal](https://github.com/hyattpd/Prodigal)

Databases and fixed parameter files (such as list of SCGs, list of ARG families) can be found in the data branch of this repository.

_Note_ that before using the .fasta database files in blastp searches you have to create diamond database _locally_ using your version of diamond. 







### From assembled metagenomes to annotated ORFs

**STEP 1. _from_ input metagenome assemblies** {SAMPLE}.fasta

**_create_ ORF nucleotide/protein sequence fasta and gff files** {SAMPLE}.fna {SAMPLE}.faa {SAMPLE}.gff

```
prodigal -p meta -q -f gff -i {SAMPLE}.fasta -o {SAMPLE}.gff -a {SAMPLE}.faa -d {SAMPLE}.fna 
```

**STEP 2. _from_ ORF protein fasta** {SAMPLE}.faa

**_search_ blastp hits in CARD, _filter_ hits and _annotate_ gene families to the ORFs** {SAMPLE}.CARD2017.blastp.i80d80.assign

```
diamond blastp -d protein_fasta_protein_homolog_model.refined.legit.fasta.dmnd -p {threads} -e 1e-20 -k 1 -q {SAMPLE}.faa -o {SAMPLE}.CARD2017.blastp

java BlastResultFilterLkh -in {SAMPLE}.CARD2017.blastp -idcut 80 -dfasta protein_fasta_protein_homolog_model.refined.legit.fasta -dcovCut 80 -out {SAMPLE}.CARD2017.blastp.i80d80

java TableColumnReformatVerticalSepField -in {SAMPLE}.CARD2017.blastp.i80d80 -col 2 -field 2 -header F -out {SAMPLE}.CARD2017.blastp.i80d80.acctmp

java TableTranslateColumnByDictionaryLkh -i {SAMPLE}.CARD2017.blastp.i80d80.acctmp -icol 2 -iheader F -d protein_fasta_protein_homolog_model.refined.accession_2_ARG_cluster_assign.tab -dheader T -q 1 -t 2 -o {SAMPLE}.CARD2017.blastp.i80d80.assign
```

**_search_ blastp hits in COG, _filter_ SCG hits and _annotate_ SCG COG numbers to the ORFs** {SAMPLE}.COG.blastp.cog.scg
```
diamond blastp -d prot2003-2014.fa.dmnd -p {threads} -e 1e-7 -k 1 -q {SAMPLE}.faa -o {SAMPLE}.COG.blastp

java TableTranslateColumnByDictionaryLkh -i {SAMPLE}.COG.blastp -icol 2 -iheader F -d gi_to_cog.tab -dheader F -q 1 -t 2 -o {SAMPLE}.COG.blastp.cog

java TableFilteringRowsLkh -tbl {SAMPLE}.COG.blastp.cog -sep t -headerRow F -col 2 -targetList list_speci_universal_single_copy_genes.cogs -out {SAMPLE}.COG.blastp.cog.scg
```

Snakemake that we used to predict and annotate ORFs in metagenome assemblies is available here [metagenome ORF snake](https://github.com/kihyunee/gut_resistotype/blob/main/scripts/metagenome_annotation.snake)

Note that file paths in the snakemake should be adjusted to fit with yours.




### From reference genomes to annotated ORFs

For genome assemblies, we used the set of commands that are same with what was used for metagenom assemlbies.

Snakemake that we used to predict and annotate ORFs in metagenome assemblies is available here [genome ORF snake](https://github.com/kihyunee/gut_resistotype/blob/main/scripts/refgenome_annotation.snake)

Note that file paths in the snakemake should be adjusted to fit with yours.






### Creating nomalized abundance profiles of ARG families (copies per genome, cpg)

**Case A, where you want to use contig coverage depth calculated by yourself through aligning reads to the contigs.**

Run the following steps for each/every sample individually.

_starting_ with **assembled contigs** in {SAMPLE}.fasta and **raw reads** in {SAMPLE}\_1.fastq.gz and {SAMPLE}\_2.fastq.gz

and **ORF to SCG, ORF to CARD annotation outputs**  {SAMPLE}.COG.blastp.cog.scg  {SAMPLE}.CARD.blastp.i80d80.assign

```
python fasta_filter_by_length.py --fasta {SAMPLE}.fasta --min_length 500 --out_tab {SAMPLE}.length.tab --out_bed {SAMPLE}.filt_500bp.bed --out_fasta {SAMPLE}.filt_500bp.fasta

bowtie2-build --quiet {SAMPLE}.filt_500bp.fasta {SAMPLE}.filt_500bp

bowtie2 -q --very-fast -x {SAMPLE}.filt_500bp -1 {SAMPLE}_1.fastq.gz -2 {SAMPLE}_2.fastq.gz --no-unal -p 2 -S {SAMPLE}.filt_500bp.sam

samtools view -b --threads 3 -o {SAMPLE}.filt_500bp.bam {SAMPLE}.filt_500bp.sam

samtools sort --threads 3 -o {SAMPLE}.filt_500bp.sort.bam {SAMPLE}.filt_500bp.bam

samtools index {SAMPLE}.filt_500bp.sort.bam

samtools depth -b {SAMPLE}.filt_500bp.bed -m 15000 {SAMPLE}.filt_500bp.sort.bam > {SAMPLE}.filt_500bp.depth

python samtools_depth_to_contig_stat.py --bed {SAMPLE}.filt_500bp.bed --depth {SAMPLE}.filt_500bp.depth --out {SAMPLE}.filt_500bp.cov_stat

python cpg_profile_from_contig_orf_annotation.py --cov_stat {SAMPLE}.filt_500bp.cov_stat --depth_col 2 --scg_annot {SAMPLE}.COG.blastp.cog.scg --scg_panel list_speci_universal_single_copy_genes.cogs --target_annot {SAMPLE}.CARD.blastp.i80d80.assign --target_panel reference_ARG_assigns.txt --out {SAMPLE}.ARG_panel.cpg
```
 
**Case B, where you want to use the coverage information stored in the _metaSpades_ contigs header lines**

Run the following steps for each/every sample individually.

_starting_ with **assembled contigs** in {SAMPLE}.fasta 

and **ORF to SCG, ORF to CARD annotation outputs**  {SAMPLE}.COG.blastp.cog.scg  {SAMPLE}.CARD.blastp.i80d80.assign

```
python cpg_profile_from_contig_orf_annotation.py --cov_stat {SAMPLE}.fasta --depth_col 0 --scg_annot {SAMPLE}.COG.blastp.cog.scg --scg_panel list_speci_universal_single_copy_genes.cogs --target_annot {SAMPLE}.CARD.blastp.i80d80.assign --target_panel reference_ARG_assigns.txt --out {SAMPLE}.ARG_panel.cpg
```


### Resulting files that we produced at this point
- Normalized abundance of ARG families (cpgs; samples X ARG families matrix) in 6104 adult stool metagenomes: [tsv file](https://www.dropbox.com/s/qyudnh2cmm7unup/DS3.SCG_normalized_ARG_abund.columns_CARD_ref.n_6104.tsv?dl=0)
- Normalized abundance of ARG families (cpgs; samples X ARG families matrix) in 6006 adult stool metagenomes after filtering out outliers: [tsv file](https://www.dropbox.com/s/i84y6xthebd1cvx/DS4b.SCG_normalized_ARG_abund.columns_CARD_ref.n_6006.tsv?dl=0)
* Note that sample names are given as row names and ARG family names are given as column names.




### Creating the catalogue of ARG sequences from the metagenomes and the reference genomes

We first collected the nucleotide sequences of the ORFs annotated as ARGs, from both metagenomes and genomes.

Each metagenome sample was processed with the following command.

```
# SAMPLE = metagenome sample ID

java PrepRichTitledFeatureFastaForDBHitORFs -s {SAMPLE} -d gut -f {SAMPLE}.fna -annot {SAMPLE}.CARD.blastp.i80d80.assign -o {SAMPLE}.CARD_hit_i80d80.faa -cgb {SAMPLE}.contig_bin.map -splabel {SAMPLE}.bin_SGB.map -sptax SGB.SGB_taxonomy.tsv -annotTitle CARD -genometype genomeBin -sptype SGB
```

Each refseq genome assembly was processed with the following command.

```
# GACC = genome assembly accession

java PrepRichTitledFeatureFastaForDBHitORFs -s refseq -d refseq -f {GACC}.fna -annot {GACC}.CARD.blastp.i80d80.assign -o {GACC}.CARD_hit_i80d80.fna -wg {GACC} -gtax {GACC}.taxonomy_kpcofgst -annotTitle CARD -genometype refGenome -sptype refSpecies
```

The resulting fasta files, created for each metagenome/genome contains information-rich header lines which will be later used in the analyses downstream of clustering step.

Header lines for metagenomic ORFs look like:
> \>NODE_1_length_562825_cov_57.8364_1;sample=MV_FEI1_t1Q14;genomeBin=AsnicarF_2017__MV_FEI1_t1Q14__bin.8;SGB=10068;taxonomy=k__Bacteria|p__Proteobacteria|c__Gammaproteobacteria|o__Enterobacterales|f__Enterobacteriaceae|g__Escherichia|s__Escherichia_coli|t__SGB10068;dataset=gut;COG=COG1215

Header lines for genomic ORFs look like:
> \>NZ_AYGS01000026.1_2;sample=refseq;refGenome=GCF_000513795.2;refSpecies=TBD;taxonomy=k__Bacteria|p__Proteobacteria|c__Gammaproteobacteria|o__Pseudomonadales|f__Moraxellaceae|g__Acinetobacter|s__Acinetobacter baumannii|t__Acinetobacter baumannii UH0207;dataset=refseq;CARD=mphD

Next, the ORF nucleotide fasta files of all metagenomes and genomes were concatenated into a single large fasta file, subjected to clustering.


### Resulting files that we produced at this point
- Nucleotide sequences of all 2,566,577 ARG ORFs pooled from metagenomes and reference genomes, before clustering: [Fasta file](https://www.dropbox.com/s/7zl4h7lxubbcwjs/AMR_genes.gut_and_refseq.pool.corrected.s2_.fna?dl=0)
- Protein sequences of all 2,566,577 ARG ORFs pooled from metagenomes and reference genomes, before clustering: [Fasta file](https://www.dropbox.com/s/wrveigfi7opyiyf/AMR_genes.gut_and_refseq.pool.corrected.s2_.faa?dl=0)





Then perform clustering on the nucleotides sequences (rather than amino acid sequences) at several identity cutoffs,\
using the following clustering commands:

```
\# fna = {single fasta file containing all ARG ORF nucleotide sequences with information-rich header lines}
mmseqs createdb ${fna} ${fna}.mmdb

# 100% identity 90% coverage clustering
mkdir nt_cluster_100_tmp
mmseqs cluster ${fna}.mmdb nt_clusters_not_linc_i100_c90/nt_cluster_100 nt_cluster_100_tmp --threads 12 --min-seq-id 1.0 -c 0.90 --cov-mode 0
mmseqs createtsv ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i100_c90/nt_cluster_100 nt_clusters_not_linc_i100_c90/nt_cluster_100.tsv
mmseqs result2repseq ${fna}.mmdb nt_clusters_not_linc_i100_c90/nt_cluster_100 nt_clusters_not_linc_i100_c90/nt_cluster_100_rep
mmseqs result2flat ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i100_c90/nt_cluster_100_rep nt_clusters_not_linc_i100_c90/nt_cluster_100_rep.fasta

# 99% identity 90% coverage clustering
mkdir nt_cluster_99_tmp
mmseqs cluster ${fna}.mmdb nt_clusters_not_linc_i99_c90/nt_cluster_99 nt_cluster_99_tmp --threads 12 --min-seq-id 0.99 -c 0.90 --cov-mode 0
mmseqs createtsv ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i99_c90/nt_cluster_99 nt_clusters_not_linc_i99_c90/nt_cluster_99.tsv
mmseqs result2repseq ${fna}.mmdb nt_clusters_not_linc_i99_c90/nt_cluster_99 nt_clusters_not_linc_i99_c90/nt_cluster_99_rep
mmseqs result2flat ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i99_c90/nt_cluster_99_rep nt_clusters_not_linc_i99_c90/nt_cluster_99_rep.fasta

# 95% identity 80% coverage clustering
mkdir nt_cluster_95_tmp
mmseqs cluster ${fna}.mmdb nt_clusters_not_linc_i95_c80/nt_cluster_95 nt_cluster_95_tmp --threads 12 --min-seq-id 0.95 -c 0.80 --cov-mode 0
mmseqs createtsv ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i95_c80/nt_cluster_95 nt_clusters_not_linc_i95_c80/nt_cluster_95.tsv
mmseqs result2repseq ${fna}.mmdb nt_clusters_not_linc_i95_c80/nt_cluster_95 nt_clusters_not_linc_i95_c80/nt_cluster_95_rep
mmseqs result2flat ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i95_c80/nt_cluster_95_rep nt_clusters_not_linc_i95_c80/nt_cluster_95_rep.fasta

# 90% identity 80% coverage clustering
mkdir nt_cluster_90_tmp
mmseqs cluster ${fna}.mmdb nt_clusters_not_linc_i90_c80/nt_cluster_90 nt_cluster_90_tmp --threads 12 --min-seq-id 0.90 -c 0.80 --cov-mode 0
mmseqs createtsv ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i90_c80/nt_cluster_90 nt_clusters_not_linc_i90_c80/nt_cluster_90.tsv
mmseqs result2repseq ${fna}.mmdb nt_clusters_not_linc_i90_c80/nt_cluster_90 nt_clusters_not_linc_i90_c80/nt_cluster_90_rep
mmseqs result2flat ${fna}.mmdb ${faa}.mmdb nt_clusters_not_linc_i90_c80/nt_cluster_90_rep nt_clusters_not_linc_i90_c80/nt_cluster_90_rep.fasta

```

### Resulting files that we produced at this point
- ORF-by-ORF attributes from 99%-level clustering and plasmid analyses: [tsv file](https://www.dropbox.com/s/nnqwoixvx7tygw9/nt_cluster_99.per_ORF_integrated_result.all_ORFs.tsv?dl=0)



### Assign LCA to the clusters of ARGs

We performed assignment of LCA taxon to each ARG cluster defined at 99% identity.
- LCA for each cluster derived from the cluster member ORFs that came from either a RefSeq genome or a high-quality MAG.
- Every single RefSeq genome and high-quality MAG has pre-defined species-level genomic bin (SGB) affiliation and full-rank taxonomy. See the files linked below.
- Clusters without any such genome-resolved ORF becomes 'unclassified'

Mapping files used at this step:
- Metagenome bin ID to SGB ID [tsv file]()  <-- SGB.2019Sep_update.SGBs.reconstructed_genome_ID_map.hq_subset.txt
- RefSeq genome assembly accession to SGB ID [tsv file]()   <--  our_refseq_analyzed.in_updated_SGB_system.map_to_SGB
- SGB ID to full rank taxonomy [tsv file]()     <-- SGB.2019Sep_update.SGBs.SGBID_to_taxonomy.lca_style_kp2SGB.tab
- List of high-quality MAGs [txt file]()       <-- MAG_HQ_only.list


Now LCA assignment was performed using the following command,\
where
> {CLUSTER_TSV} = the output tsv file from *mmseqs createtsv* in the previous step
> {HQMAG_LIST} = the file containing list of high-quality MAGs
> {HQMAG_SGB_MAP} = the file mapping from metagenome bin ID to SGB ID
> {REFSEQ_SGB_MAP} = the file mapping from RefSeq genome assembly accession to SGB ID
> {SGB_TAX_MAP} = the file mapping from SGB ID to the full rank taxonomy
run\
```
java GeneClusterTsvSubjectToSimpleLCAClassifier -i {CLUSTER_TSV} -hq {HQMAG_LIST} -r2s {REFSEQ_SGB_MAP} -h2s {HQMAG_SGB_MAP} -s2t {SGB_TAX_MAP} -o LCA_analysis_for_ARG_cl99.kp2SGB

lca.py -i LCA_analysis_for_ARG_cl99.kp2SGB.LCAsub_hqPlusRef -o LCA_analysis_for_ARG_cl99.kp2SGB.result_using_HQPlusRef -b 999 -id 0 -cov 0 -tid 0 -tcov 0 -t no -fh "env" -flh "unknown"
```

