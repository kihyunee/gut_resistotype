import argparse

parser = argparse.ArgumentParser(description = "Input specifications ")
parser.add_argument("--bed", dest="input_bed", type=str, required=True, help="input bed file defining contig subjects and their length")
parser.add_argument("--depth", dest="input_depth", type=str, required=True, help="depth file from 'samtools depth'; not necessarily run with -a (include all positions in output) ")
parser.add_argument("--out", dest="output_tab", type=str, required=True, help="output table: contig name, mean depth, length, prop covered position")

args = parser.parse_args()
input_bed = args.input_bed
input_depth = args.input_depth
output_tab = args.output_tab


list_chrom = []
dict_chrom_len = {}
dict_chrom_zbi = {}
chrom_zbi = 0
fr = open(input_bed, "r")
for line in fr:
    fields = line.strip().split("\t")
    chrom = fields[0]
    len_bp = int(fields[2])
    dict_chrom_len[chrom] = len_bp
    dict_chrom_zbi[chrom] = chrom_zbi
    list_chrom.append(chrom)
    #
    chrom_zbi += 1
fr.close()


num_chrom = len(dict_chrom_len)


list_chrom_covsite = [0]*num_chrom
list_chrom_basestack = [0]*num_chrom


fr = open(input_depth, "r")
for line in fr:
    fields = line.strip().split("\t")
    chrom = fields[0]
    depth_val = int(fields[2])
    chrom_zbi = dict_chrom_zbi[chrom]
    if depth_val > 0:
        list_chrom_covsite[chrom_zbi] += 1
        list_chrom_basestack[chrom_zbi] += depth_val
fr.close()


# 
fw = open(output_tab, "w")
fw.write("chrom\tmean_depth\tlength\tbreadth_coverage\n")
for chrom_zbi in range(num_chrom):
    chrom = list_chrom[chrom_zbi]
    chrom_length = dict_chrom_len[chrom]
    mean_depth = float(list_chrom_basestack[chrom_zbi])/float(chrom_length)
    breadth = float(list_chrom_covsite[chrom_zbi])/float(chrom_length)
    fw.write(chrom + "\t" + str(mean_depth) + "\t" + str(chrom_length) + "\t" + str(breadth) + "\n")
fw.close()
