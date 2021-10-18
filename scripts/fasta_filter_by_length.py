import argparse

parser = argparse.ArgumentParser(description = "Input specifications ")
parser.add_argument("--fasta", dest="input_fasta", type=str, required=True, help="input fasta")
parser.add_argument("--min_length", dest="min_length", type=int, required=True, help="minimum required length to pass")
parser.add_argument("--fail", dest="collect_the_fail", action="store_true", required=False, default=False, help="do this if you want to collect the seqs who FAIL (not PASS) the length cutoff")
parser.add_argument("--out_tab", dest="output_table", type=str, required=True, help="simply two column list of the length of input sequences")
parser.add_argument("--out_bed", dest="output_bed", type=str, required=True, help="bed format of filtered sequences / contigs")
parser.add_argument("--out_fasta", dest="output_fasta", type=str, required=True, help="fasta file of the length passed sequences")


args = parser.parse_args()
input_fasta = args.input_fasta
min_length = args.min_length
output_table = args.output_table
output_fasta = args.output_fasta
output_bed = args.output_bed
collect_the_fail = args.collect_the_fail


fwt = open(output_table, "w")
fwf = open(output_fasta, "w")
fwb = open(output_bed, "w")
fr = open(input_fasta, "r")

line = fr.readline()
while line != '':
    if line.strip().startswith('>'):
        seqid = line.strip()[1:].split(' ')[0]
        seq_list = []
        line = fr.readline()
        while line != '':
            seq_list.append(line.strip())
            line = fr.readline()
            if line.strip().startswith('>'):
                break
        #
        seq = "".join(seq_list)
        seq_length = len(seq)
        fwt.write(seqid + "\t" + str(seq_length) + "\n")
        #
        if collect_the_fail:
            if seq_length < min_length:
                fwf.write(">" + seqid + "\n" + seq + "\n")    
                fwb.write(seqid + "\t" + "0" + "\t" + str(seq_length) + "\t" + seqid + "\n")
        elif seq_length >= min_length:
            fwf.write(">" + seqid + "\n" + seq + "\n")
            fwb.write(seqid + "\t" + "0" + "\t" + str(seq_length) + "\t" + seqid + "\n")
    else:
        line = fr.readline()
fr.close()

fwt.close()
fwf.close()
fwb.close()
