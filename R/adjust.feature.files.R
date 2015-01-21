
# removes mislabeled non-TFs in feature files and names files
adjust.feature.files <- function(base.dir="/rahome/eichner/projects/tfpredict/data/tf_pred/") {

  old.subdir <- "12.02.2013/"
  new.subdir <- "20.03.2013/"
  
  # determine mislabeled non-TFs
  fasta.dir <- paste(base.dir, "fasta_files/", sep="")

  tf.fasta.file <- paste(fasta.dir, old.subdir, "TF.fasta", sep="")
  nontf.fasta.file <- paste(fasta.dir, old.subdir, "NonTF.fasta", sep="")

  tf.seq <- as.matrix(read.table(tf.fasta.file, sep="\n"))
  nontf.seq <- as.matrix(read.table(nontf.fasta.file, sep="\n"))

  tf.header <- grep(">", tf.seq, value=TRUE)
  nontf.header <- grep(">", nontf.seq, value=TRUE) 
  
  tf.uniprot <- as.matrix(sapply(tf.header, function (x) strsplit(x, "\\|")[[1]][2]))
  nontf.uniprot <- as.matrix(sapply(nontf.header, function (x) strsplit(x, "\\|")[[1]][2]))
  
  mislab.nontf.uniprot <- intersect(tf.uniprot, nontf.uniprot)
  mislab.nontf.ids <- sub(">", "", nontf.header[match(mislab.nontf.uniprot, nontf.uniprot)])

  # remove mislabeled non-TFs in feature files
  feat.dir <- paste(base.dir, "feature_files/", sep="")
  feat.types <- c("kmer", "naive", "pssm", "pseudo", "percentile")
  
  for (i in 1:length(feat.types)) {

    old.feat.file <- paste(feat.dir, old.subdir, feat.types[i], "_features.txt", sep="")
    old.names.file <- sub(".txt", "_names.txt", old.feat.file)
    new.feat.file <- paste(feat.dir, new.subdir, feat.types[i], "_features.txt", sep="")
    new.names.file <- sub(".txt", "_names.txt", new.feat.file)
    
    feat.vectors <- as.matrix(read.table(old.feat.file, sep="\n"))
    feat.names <- as.matrix(read.table(old.names.file, sep="\n"))

    stopifnot(length(feat.vectors) == length(feat.names) & (length(feat.vectors) == 16333 || length(feat.vectors) == 16319))
    
    rm.row.idx <- match(mislab.nontf.ids, feat.names)
    feat.vectors <- feat.vectors[-rm.row.idx,]
    feat.names <- feat.names[-rm.row.idx,]
    
    write.table(feat.vectors, file=new.feat.file, sep="\n", row.names=FALSE, col.names=FALSE, quote=FALSE)
    write.table(feat.names, file=new.names.file , sep="\n", row.names=FALSE, col.names=FALSE, quote=FALSE)
  }
}
