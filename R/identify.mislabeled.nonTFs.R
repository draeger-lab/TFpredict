# fasta.input.file <- "/rahome/eichner/projects/tfpredict/data/tf_pred/fasta_files/20.03.2013/NonTF.fasta"
# uniprot.fasta <- "/rahome/eichner/projects/tfpredict/data/tf_pred/fasta_files/21.08.2013/uniprot_sprot.fasta"
# fasta.output.file <- "/rahome/eichner/projects/tfpredict/data/tf_pred/fasta_files/21.08.2013/NonTF.fasta"

# sapply(put.mis.nontfs, function (x) paste(grep(x, list(dnaBind=dna.binding.ids, transReg=transcription.reg.ids, transFac=transfac.ids, go=go.ids)), collapse="_"))

identify.mislabeled.nonTFs <- function(fasta.input.file, uniprot.fasta, fasta.output.file) {

  import.function("read.fasta2list.R")
  import.function("write.list2fasta.R")
  import.function("trim.R")
  import.package("UniProt.ws")

  # read species from UniProt flat file
  all.seqs <- read.fasta2list(uniprot.fasta)
  headers <- names(all.seqs)
  all.ids <- sub("\\|.*", "", sub(".*?\\|", "", headers))
  uniprot.id2species <- sub(" PE=.*", "", sub(" GN=.*", "", sub(".* OS=", "", headers)))
  names(uniprot.id2species) <- all.ids
  
  fasta.seqs <- read.fasta2list(fasta.input.file)
  uniprot.ids <- sapply(strsplit(names(fasta.seqs), "\\|"), function (x) x[2])
  uniprot.ids <- sub("P29512", "Q56YW9", sub("P05512", "P12294", uniprot.ids))
  rel.species <- uniprot.id2species[uniprot.ids]
  species2id <- lapply(unique(rel.species), function (x) which(rel.species == x))
  names(species2id) <- unique(rel.species)
   
  available.species <- availableUniprotSpecies()[,1]
  names(available.species) <- availableUniprotSpecies()[,2]
  rel.avail.species <- intersect(names(available.species), names(species2id))
  available.species <- available.species[rel.avail.species]
  available.species <- available.species[-which(available.species == "622")]
  
  keywords <- list()
  for (i in 1:length(available.species)) {
    taxon <- available.species[i]
    taxId(UniProt.ws) <- taxon
    curr.ids <- names(species2id[[names(taxon)]])
    keywords <- c(keywords, select(UniProt.ws, keys=curr.ids, cols="KEYWORDS", keytype="UNIPROTKB")$KEYWORDS)
  }
  keywords <- lapply(keywords, function (x) trim(strsplit(x, ";")[[1]]))
  names(keywords) <- sub(".*\\.", "", names(unlist(species2id[names(available.species)])))
  
  dna.binding.ids <- names(keywords)[grep("DNA-binding", keywords)]
  transcription.reg.ids <- names(keywords)[grep("Transcription regulation", keywords)]

  # read UniProt-IDs contained in current TRANSFAC release
  transfac.uniprot.ids <- as.vector(as.matrix(read.table("/rahome/eichner/projects/tfpredict/data/tf_pred/fasta_files/21.08.2013/transfac_2013.2_uniprotIDs.txt")))
  transfac.ids <- intersect(transfac.uniprot.ids, uniprot.ids)
  
  # read UniProt-IDs of GO-annotated transcription factors
  go.uniprot.ids <- as.matrix(read.csv("/rahome/eichner/projects/tfpredict/data/tf_pred/fasta_files/21.08.2013/transcription_factors_GO.txt", sep="\t"))
  go.ids <- intersect(go.uniprot.ids[,2], uniprot.ids)
  
  
  if (length(dna.binding.idx) > 0) {
    fasta.seqs <- fasta.seqs[-dna.binding.idx]
  }
  write.list2fasta(fasta.seqs, fasta.output.file)
}


