write.list2fasta <- function(fasta.list, fasta.file, line.length=60) {

  import.function("wrap.string.R")

  num.seq <- length(fasta.list)
  wrapped.list <- vector("list", num.seq)
  for (i in 1:num.seq) {
    wrapped.list[[i]] <- wrap.string(fasta.list[[i]], line.length)
  }
  names(wrapped.list) <- names(fasta.list)

  num.lines <- num.seq + sum(sapply(wrapped.list, length))
  fasta.content <- vector("character", num.lines)
  header.idx <- 1
  for (i in 1:num.seq) {
    
    start.idx <- header.idx + 1
    stop.idx <- start.idx + length(wrapped.list[[i]]) - 1
    fasta.content[header.idx] <- paste(">", names(wrapped.list)[i], sep="")
    fasta.content[start.idx:stop.idx] <- wrapped.list[[i]]
    header.idx <- stop.idx + 1
  }

  write.table(fasta.content, file=fasta.file, sep="\n", row.names=FALSE, col.names=FALSE, quote=FALSE)
}
