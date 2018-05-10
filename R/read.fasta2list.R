read.fasta2list <- function(fasta.file) {

  fasta.content <- as.matrix(read.table(fasta.file, sep="\n"))
  empty.line.idx <- grep("^$", fasta.content)
  if (length(empty.line.idx) > 0) {
    fasta.content <- fasta.content[-empty.line.idx]
  }
  header.idx <- grep(">", fasta.content)
  seq.start.idx <- header.idx+1
  seq.end.idx <- c(header.idx[-1]-1, length(fasta.content))
  
  fasta.seqs <- vector("list", length(header.idx))
  for (i in 1:length(header.idx)) {
    fasta.seqs[[i]] <- paste(fasta.content[seq.start.idx[i]:seq.end.idx[i]], collapse="")
  }
  names(fasta.seqs) <- sub(">", "", fasta.content[header.idx])

  return(fasta.seqs)
}
