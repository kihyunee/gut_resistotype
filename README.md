# gut_resistotype

## About
**This is the repository for the analysis scripts and data files used in the following study:**

Population-level impacts of antibiotic usage on the human gut microbiome. *Unpublished.* (2021) Kihyun Lee, Sebastien Raguideau, Kimmo Sir`en, Francesco Asnicar, Fabio Cumbo, Falk Hildebrand, Nicola Segata, Chang-Jun Cha, Christopher Quince


# Data files

## Databases used

**1. Human microbiome metagenome assemblies previously published in [Pasolli et al., 2019, Cell 176, 649–662](https://doi.org/10.1016/j.cell.2019.01.001)**

Fasta files can be accessed following the instructions provided in http://segatalab.cibio.unitn.it/data/Pasolli_et_al.html

Sample metadata provided in Pasolli et al. 2019 was additionally curated with respect to (a) health and disease status of the subject and (b) antibiotic usage preceding the sampling. The final metadata used in our study is provided in this link: [microbiome sample metadata](https://github.com/kihyunee/gut_resistotype/blob/data/sfile-microbiome-sample-detail.tsv)

**2. NCBI RefSeq prokaryotic genome assemblies accessed at 2019/04/19**

Assembly accession numbers (RefSeq/GenBank) of the prokaryotic genomes analyzed in our study is provided in this link: [genome accession list](https://github.com/kihyunee/gut_resistotype/blob/data/refseq_genbank_assembly_acc_pair.nover.tsv)

**3. Modified version of CARD**

We used the database of protein sequences for known ARGs, which was created by modifying the October 2017 version of [CARD](https://card.mcmaster.ca/), cite [Jia et al. 2017](https://doi.org/10.1093/nar/gkw1004)

For this modified version of CARD, the following files are provided in this repository.

(1) [fasta file of protein homolog model reference sequences after filtering out variant/mutation-dependent resistance genes](https://github.com/kihyunee/gut_resistotype/blob/data/protein_fasta_protein_homolog_model.refined.legit.fasta)

(2) [protein accessions to ARG families map](https://github.com/kihyunee/gut_resistotype/blob/data/protein_fasta_protein_homolog_model.refined.accession_2_ARG_cluster_assign.tab)

(3) [ARG families to antibiotics map](https://github.com/kihyunee/gut_resistotype/blob/data/ClusterAssign_to_Revised_Antibiotic.map)

(4) [ARG families to mechanisms map](https://github.com/kihyunee/gut_resistotype/blob/data/ClusterAssign_to_Revised_Mechanism.map)

**4. COG protein reference sequences**

We used COG, the 2014 update version, accessed through NCBI FTP at December 2018 using the following link [COG 2014 protein fasta](https://ftp.ncbi.nih.gov/pub/COG/COG2014/data/prot2003-2014.fa.gz)

Our analysis using COG is focused at the list of [40 universally single-copy orthologs, SCGs](https://github.com/kihyunee/gut_resistotype/blob/data/list_speci_universal_single_copy_genes.cogs)

Mapping from gi numbers (used in COG protein sequence headers) to COG numbers is needed and provided in this link:[gi-to-COG mapping](https://www.dropbox.com/s/srr6tzim5d4f324/gi_to_cog.tab?dl=0)

shorter version for [gi-to-COG mapping for the SCGs](https://github.com/kihyunee/gut_resistotype/blob/data/gi_to_cog.tab.scg_only)


## Clustered catalogue of ARG ORFs from human microbiome metagenomes and prokaryotic reference genomes
Nucleotide sequences of all 2,566,577 ARG ORFs pooled from metagenomes and reference genomes: [Fasta file]()

Protein sequences of all 2,566,577 ARG ORFs pooled from metagenomes and reference genomes: [Fasta file]()

- Fasta header lines contain several information including sample origin, MAG affiliation, SGB (species) affiliation, taxonomy, and ARG family annotation
ORF-by-ORF attributes from 99%-level clustering and plasmid analyses: [tsv file]()


## Normalized resistance gene profiles (cpg, samples X ARG families matrix) of adult stool metagenomes
[abundance cpg of ARG families in 6104 adult stool metagenomes](https://www.dropbox.com/s/qyudnh2cmm7unup/DS3.SCG_normalized_ARG_abund.columns_CARD_ref.n_6104.tsv?dl=0)
[abundance cpg of ARG families in 6006 adult stool metagenomes after filtering out outliers](https://www.dropbox.com/s/i84y6xthebd1cvx/DS4b.SCG_normalized_ARG_abund.columns_CARD_ref.n_6006.tsv?dl=0)
- Sample names are given as row names.
- ARG family names are given as column names.



# Analytical steps

Below are the commands and scripts used in our main analytical steps.

Script files called in the commands below can be found in the script section ([scripts](https://github.com/kihyunee/gut_resistotype/tree/main/scripts)) of this repository.

External tools called in the commands are:
- diamond: [diamond](https://github.com/bbuchfink/diamond)
- prodigal: [prodigal](https://github.com/hyattpd/Prodigal)

Databases and fixed parameter files (such as list of SCGs, list of ARG families) can be found in the data branch of this repository.

_Note_ that before using the .fasta database files in blastp searches you have to create diamond database _locally_ using your version of diamond. 


## From assembled metagenomes to annotated ORFs

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


## From reference genomes to annotated ORFs

For genome assemblies, we used the set of commands that are same with what was used for metagenom assemlbies.

Snakemake that we used to predict and annotate ORFs in metagenome assemblies is available here [genome ORF snake](https://github.com/kihyunee/gut_resistotype/blob/main/scripts/refgenome_annotation.snake)

Note that file paths in the snakemake should be adjusted to fit with yours.


## From annotated metagenomic ORFs to nomalized abundance (copies per genome, cpg) profile of ARG families in samples

## From annotated ORFs from metagenomes and reference genomes to the catalogue of ARG ORFs

## From catalogue of ARG ORFs to the clustered catalogues of ARGs

## Assign LCA to the clusters of ARGs

## Calculating resistotype scale index to the samples outside the original Pasolli et al. 2019 dataset

