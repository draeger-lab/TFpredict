domain.names.file <- "/rahome/eichner/projects/tfpredict/data/tf_pred/feature_files/latest/relevant_domains.txt"
domain.feat.file <- "/rahome/eichner/projects/tfpredict/data/tf_pred/feature_files/latest/domain_features.txt"
domain.feat.names.file <- "/rahome/eichner/projects/tfpredict/data/tf_pred/feature_files/latest/domain_features_names.txt"

# domain.names.file <- "/rahome/eichner/projects/tfpredict/data/super_pred/feature_files/latest/relevant_domains.txt"
# domain.feat.file <- "/rahome/eichner/projects/tfpredict/data/super_pred/feature_files/latest/domain_features.txt"
# domain.feat.names.file <- "/rahome/eichner/projects/tfpredict/data/super_pred/feature_files/latest/domain_features_names.txt"

get.interpro2class.map <- function(domain.feat.file, domain.feat.names.file, domain.names.file, feat.offset=10) {

  import.function("trim.R")
    
  # read domain features
  domain.names <- as.vector(as.matrix(read.table(domain.names.file, sep="\n")))
  domain.feat.libsvm <- as.vector(as.matrix(read.table(domain.feat.file, sep="\n")))
  labels <- as.numeric(sapply(domain.feat.libsvm, function (x) substr(x,1,2)))
  domain.features <- lapply(sapply(domain.feat.libsvm, function (x) trim(gsub(":1", "", substr(x,3,nchar(x))))), function (x) domain.names[as.numeric(strsplit(x, " ")[[1]]) - feat.offset])

  labels2domains <- lapply(sort(unique(labels)), function (x) unique(unlist(domain.features[which(labels == x)])))
  unique.domains <- lapply(sort(unique(labels)), function (x) setdiff(labels2domains[[max(x,0)+1]], unique(unlist(labels2domains[-(max(x,0)+1)]))))

  domain.feat.names <- as.vector(as.matrix(read.table(domain.feat.names.file, sep="\n")))
  feat2uniprot <- lapply(domain.feat.names, function (x) sub("_.*", "", sub(".*?_", "", strsplit(x, "\t")[[1]])))
  
  
}



iprscan.outfile <- "/rahome/eichner/projects/tfpredict/failed_inputs/allTFs_iprscan_output.txt"
get.characteristic.domains <- function(iprscan.outfile) {

  ipr.output <- as.vector(as.matrix(read.table(iprscan.outfile, sep="\n")))
  superclasses <- as.numeric(substring(sub(".*_", "", sub("_TransFac.*", "", sub("_MatBase.*", "", ipr.output))), 1,2))
  domain.ids <- as.vector(sapply(ipr.output, function (x) strsplit(x, "\t")[[1]][12]))
  null.ids <- which(domain.ids == "NULL")
  superclasses <- superclasses[-null.ids]
  domain.ids <- domain.ids[-null.ids]

  labels2domains <- lapply(sort(unique(superclasses)), function (x) unique(domain.ids[which(superclasses == x)]))
  unique.domains <- lapply(sort(unique(superclasses)), function (x) setdiff(labels2domains[[max(x,0)+1]], unique(unlist(labels2domains[-(max(x,0)+1)]))))

  domain.files <- list.files("/rahome/eichner/workspace/TFpredict/src/resources/", pattern="domainsClass", full.names=TRUE)
  old.domains <- lapply(domain.files, function (domain.file) as.vector(as.matrix(read.table(domain.file, sep="\n"))))
  new.domains <- lapply(1:5, function (i) union(old.domains[[i]], unique.domains[[i]]))
  for (i in 1:5) {
    write.table(unique.domains[[i]], file=domain.files[[i]], sep="\n", quote=F, row.names=F, col.names=F)
  }
}
