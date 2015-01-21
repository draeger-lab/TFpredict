# replace with source commands with full paths to R functions - JTY
# source <filename> instead of import

read.inst.names.file <- function(subdirs, tf.pred=FALSE) {

  # read mapping from feature vectors to sequence IDs
  feat2seqID <- list()
  for (subdir in subdirs) {
    inst.names.file <- paste(sub("class_files", "feature_files", subdir), "names.txt", sep="_")
    inst.names.table <- as.matrix(read.csv(inst.names.file, header=FALSE)[[1]])
    
    if (tf.pred) {
      inst.names.table <- gsub("(^\\t|\\t$)", "", gsub("\\|.*?(\\t|$)", "\t", gsub("(^|\\t).*?\\|", "\t", sub("|41817|", "|P41817|", gsub("_", "|", inst.names.table), fixed=TRUE))))
    } else {
      inst.names.table <- gsub("(NonTF|TF)\\|.*?\\|", "", gsub("_", "|", inst.names.table))
    }

    # permute order of feature vector names according to cv-split from WEKA to match order with generated prediction scores
    cv.split.file <- paste(subdir, "cvSplit.txt", sep="/")
    cv.split <- as.matrix(read.table(cv.split.file, sep="\n"))
    cv.split <- apply(as.matrix(cv.split), 1, function (x) unlist(strsplit(x, " ")))
    cv.perm <- unlist(lapply(cv.split, function (x) as.numeric(x)+1))
    inst.names.table <- inst.names.table[cv.perm,,drop=FALSE]
    
    feat.name <- sub("_features", "", sub(".*/", "", subdir))
    feat2seqID[[feat.name]] <- apply(inst.names.table, 1, function (x) unlist(strsplit(x, split="\t")))
  }
  seq.names <- sort(unique(unlist(feat2seqID)))

  # generate mapping from feature vectors to sequence names
  feat2seq <- list()
  for (i in 1:length(feat2seqID)) {
    feat2seq[[i]] <- rep(NA, length(seq.names))
    names(feat2seq[[i]]) <- seq.names
    for (j in 1:length(feat2seqID[[i]])) {
      feat2seq[[i]][match(feat2seqID[[i]][[j]], seq.names)] <- j
    }
  }
  
  return(feat2seq)
}
