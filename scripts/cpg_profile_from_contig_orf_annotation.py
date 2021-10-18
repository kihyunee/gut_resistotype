import argparse


"""
This code is intended to create SCG-median-normalized cpg (copies per genome) profile per single input sample!
Per-sample profiles can be later combined and subjected to all kind of analysis.
Your annotation input files are supposed to have gone through all filtering steps (we don't do filtering based on alignment quality or whatever)
and are supposed to have a unique annotation for each ORF (we don't consider second annotation for an ORF).
One more important assumption is that the ORFs you analyzed were created by prodigal; or more precisely, we expect the ORF sequence IDs to contain contig sequence IDs
in the format of [CONTIG]_[index] thereby the ORF can be mapped to the CONTIG just by removing the one last overhaning index
"""

parser = argparse.ArgumentParser(description = "Input specifications ")
parser.add_argument("--cov_stat", dest="contig_cov_stat_table", type=str, required=True, help="contig coverage stat table. header yes; column 1 must be the contig ID; column for mean coverage depth value should be provided in --depth_col. IF YOU WANT TO USE the metaSPADES depth information in the contig header lines, provide --cov_stat {CONTIG_FASTA_FILE} --depth_col 0")
parser.add_argument("--depth_col", dest="depth_col_obi", type=int, required=True, help="specify which (1-based index) column in --cov_stat is the mean read depth of the contig")
parser.add_argument("--scg_annot", dest="scg_annot_table", type=str, required=True, help="table, no header, column 1 ORF, column 2 COG, other columns free")
parser.add_argument("--scg_panel", dest="fixed_scgcog_listfile", type=str, required=True, help="provide the fixed list of COG numbers that are considered single-copy genes (SCGs)")
parser.add_argument("--target_annot", dest="target_annot_table", type=str, required=True, help="table, no header, column 1 ORF, column 2 target annotation, other columns free")
parser.add_argument("--target_panel", dest="fixed_target_panel_listfile", type=str, required=False, default = "_ns", help="(optional) provide the fixed list of target (annotation value) panel; in a single-columned text file without header row; Doing this will create entry/row-order consistency across many profiles created individually per sample")
parser.add_argument("--out", dest="output_tab", type=str, required=True, help="output table: header row yes; column 1 the annotation; column 2 the cpg value; column 3 the ORF number")

args = parser.parse_args()
contig_cov_stat_table = args.contig_cov_stat_table
depth_col_obi = args.depth_col_obi
scg_annot_table = args.scg_annot_table
fixed_scgcog_listfile = args.fixed_scgcog_listfile
target_annot_table = args.target_annot_table
fixed_target_panel_listfile = args.fixed_target_panel_listfile
output_tab = args.output_tab
output_scgrec_tab = output_tab + ".scg_record"

use_depth_in_header = False
contig_cov_stat_fasta = 'NA'
if depth_col_obi == 0:
    use_depth_in_header = True
    contig_cov_stat_fasta = contig_cov_stat_table



# list of SCG and list of target annotation
list_scg_name = []
dict_scg_name_scg_idx = {}
list_annot_val = []
dict_annot_val_annot_idx = {}

fr = open(fixed_scgcog_listfile, "r")
buff_zbi = 0
for line in fr:
    scgcog = line.strip()
    if scgcog not in dict_scg_name_scg_idx:
        dict_scg_name_scg_idx[scgcog] = buff_zbi
        list_scg_name.append(scgcog)
        buff_zbi += 1
fr.close()

if fixed_target_panel_listfile == '_ns':
    # collect from the annotation table
    fr = open(target_annot_table, "r")
    buff_zbi = 0
    for line in fr:
        fields = line.strip().split("\t")
        annot_val = fields[1]
        if annot_val not in dict_annot_val_annot_idx:
            dict_annot_val_annot_idx[annot_val] = buff_zbi
            list_annot_val.append(annot_val)
            buff_zbi += 1
    fr.close()
else:
    fr = open(fixed_target_panel_listfile, "r")
    buff_zbi = 0
    for line in fr:
        annot_val = line.strip()
        if annot_val not in dict_annot_val_annot_idx:
            dict_annot_val_annot_idx[annot_val] = buff_zbi
            list_annot_val.append(annot_val)
            buff_zbi += 1
    fr.close()

num_scg_name = len(list_scg_name)
num_annot_val = len(list_annot_val)




# contig coverage depth map
dict_ctg_mean_depth = {}
if use_depth_in_header:
    fr = open(contig_cov_stat_fasta, "r")
    for line in fr:
        if line.strip().startswith('>'):
            contig_id = line.strip()[1:]
            cov_float = float(contig_id[(contig_id.rfind('_cov_') + 5):])
            dict_ctg_mean_depth[contig_id] = cov_float
    fr.close()


else:
    depth_col_zbi = depth_col_obi - 1
    fr = open(contig_cov_stat_table, "r")
    line = fr.readline()
    for line in fr:
        fields = line.strip().split("\t")
        ctg = fields[0]
        depth = float(fields[depth_col_zbi])
        dict_ctg_mean_depth[ctg] = depth
    fr.close()



# per SCG depth sum
list_scg_orf_weight_sum = [0]*num_scg_name
list_scg_orf_num = [0]*num_scg_name
fr = open(scg_annot_table, "r")
for line in fr:
    fields = line.strip().split("\t")
    orf = fields[0]
    annot = fields[1]
    contig = orf[:orf.rfind('_')]
    #
    if annot not in dict_scg_name_scg_idx:
        continue
    #
    scg_name_idx = dict_scg_name_scg_idx[annot]
    contig_depth = dict_ctg_mean_depth[contig]
    #
    list_scg_orf_weight_sum[scg_name_idx] += contig_depth
    list_scg_orf_num[scg_name_idx] += 1
fr.close()



# per target annotation value depth sum
list_annot_val_orf_weight_sum = [0]*num_annot_val
list_annot_val_orf_num = [0]*num_annot_val
fr = open(target_annot_table, "r")
for line in fr:
    fields = line.strip().split("\t")
    orf = fields[0]
    annot = fields[1]
    contig = orf[:orf.rfind('_')]
    #
    if annot not in dict_annot_val_annot_idx:
        print("WARNING: there is target annotation value NOT FOUND in the panel of annotation value (" + annot + ")")
        continue
    #
    annot_val_idx = dict_annot_val_annot_idx[annot]
    contig_depth = dict_ctg_mean_depth[contig]
    #
    list_annot_val_orf_weight_sum[annot_val_idx] += contig_depth
    list_annot_val_orf_num[annot_val_idx] += 1
fr.close()



# record scg; 
fwscg = open(output_scgrec_tab, "w")
fwscg.write("SCG\tsummed_depth\tnum_orf\n")
for scg_name_idx in range(num_scg_name):
    fwscg.write(list_scg_name[scg_name_idx])
    fwscg.write("\t" + str(list_scg_orf_weight_sum[scg_name_idx]))
    fwscg.write("\t" + str(list_scg_orf_num[scg_name_idx]))
    fwscg.write("\n")
fwscg.close()

# get scg median
sorted_scg_orf_weight_sum = sorted(list_scg_orf_weight_sum)
scg_median = 0
half_idx = int(num_scg_name/2)
if (num_scg_name % 2) == 0:
    scg_median = (sorted_scg_orf_weight_sum[half_idx] + sorted_scg_orf_weight_sum[half_idx - 1])/float(2)
else:
    scg_median = sorted_scg_orf_weight_sum[half_idx]



# calculate cpg and record it
fwcpg = open(output_tab, "w")
fwcpg.write("annotation\tcpg\tnum_orf\n")
for annot_val_idx in range(num_annot_val):
    cpg = float(list_annot_val_orf_weight_sum[annot_val_idx])/float(scg_median)
    fwcpg.write(list_annot_val[annot_val_idx])
    fwcpg.write("\t" + str(cpg))
    fwcpg.write("\t" + str(list_annot_val_orf_num[annot_val_idx]))
    fwcpg.write("\n")
fwcpg.close()
