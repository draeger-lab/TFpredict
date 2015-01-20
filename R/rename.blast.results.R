rename.blast.results <- function(blast.result.dir="/rascratch/user/eichner/tmp/", outfile.suffix="_hits") {

  outfile.type <- sub("_","", outfile.suffix)
  fasta.files <- sub(outfile.type, "fasta", list.files(blast.result.dir, pattern=outfile.type))

  for (i in 1:length(fasta.files)) {
    if (nchar(fasta.files[i]) < 30) {
      next
    }
    if (i %% 100 == 0) {
      cat(sprintf("%s / %s files were processed.\n", i, length(fasta.files)))
    }
    
    old.filename <- paste(blast.result.dir, fasta.files[i], sep="")
    header <- as.matrix(read.table(old.filename, sep="\n"))[1]
    uniprot.id <- gsub("\\|(NonTF|TF)\\|.*", "", gsub(">.*?\\|", "", header))

    rand.num <- sub("_fasta.txt", "", sub("psiblast_", "", fasta.files[i]))
    new.filename <- sub(rand.num, uniprot.id, old.filename)
    old.filename.suffix <- sub("_fasta", outfile.suffix, old.filename)
    new.filename.suffix <- sub("_fasta", outfile.suffix, new.filename)
    if (!file.exists(new.filename.suffix)) {
      file.rename(old.filename, new.filename)
      file.rename(old.filename.suffix, new.filename.suffix)
      
    } else {
      file.remove(old.filename)
      file.remove(old.filename.suffix)
    }
  }
}
