tf.names <- c("SP1_HUMAN", "KLF12_HUMAN", "ZBTB5_HUMAN", "IKZF1_HUMKAN", "TOX_HUMAN", "Q05CW4", "O96028", "UBF1_HUMAN", "NFYA_HUMAN", "DEAF1_HUMAN", "TFCP2_HUMAN", "GRHL1_HUMAN", "RERE_HUMAN")
url.prefix <- "http://www.uniprot.org/uniprot/"
download.dir <- "/rahome/eichner/projects/tfpredict/failed_inputs/"

tf.fasta.urls <- paste(url.prefix, tf.names, ".fasta", sep="")
tf.fasta.dests <- paste(download.dir, tf.names, ".fasta", sep="")

for (i in 1:length(tf.names)) {
  download.file(tf.fasta.urls[i], tf.fasta.dests[i])
}
