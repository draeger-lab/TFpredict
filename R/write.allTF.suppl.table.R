pred.file <- "/rahome/eichner/projects/tfpredict/failed_inputs/allTFs_withTFDomains.pred"
map.file <- "/rahome/eichner/projects/tfpredict/failed_inputs/transfac2uniprot.txt"
anno.file <- "/rahome/eichner/projects/tfpredict/failed_inputs/allTFs_anno_table.txt"

pred.table <- as.vector(as.matrix(read.table(pred.file, sep="\n")))

tf.ids <- sub("^NA  ", "", sub("\\|.*", "", grep("^NA  ", pred.table, value=TRUE)))
uniprot.ids <- sub("NA  .*", "", sub(".*\\|", "", sub("\\|TF\\|.*", "", grep("^NA  ", pred.table, value=TRUE))))
source.database <- sub(".*\\|", "", grep("^NA  ", pred.table, value=TRUE))
anno.classes <- sub("\\|.*", "", sub(".*\\|TF\\|", "", grep("^NA  ", pred.table, value=TRUE)))
super.names <- c("Other", "Basic domain", "Zinc finger", "Helix-turn-helix", "Beta scaffold")
anno.super <- super.names[as.numeric(substring(anno.classes, 1,1))+1]
pred.classes <- as.numeric(substring(sub("^CL  ", "", grep("^CL  ", pred.table, value=TRUE)), 1,1))
pred.super <- super.names[pred.classes + 1]
pred.correct <- c("False", "True")[as.numeric(anno.super == pred.super)+1]
pred.correct[is.na(pred.correct)] <- "False"


transfac2uniprot <- as.matrix(read.table(map.file, sep="\t"))
rownames(transfac2uniprot) <- transfac2uniprot[,1]
transfac2uniprot <- transfac2uniprot[,2]
transfac2uniprot <- sub("\\-.*", "", transfac2uniprot)
transfac2uniprot <- transfac2uniprot[-which(is.na(transfac2uniprot))]
mapped.uniprot.ids <- transfac2uniprot[tf.ids]
mapped.uniprot.ids[is.na(mapped.uniprot.ids)] <- ""
names(mapped.uniprot.ids) <- NULL
new.uniprot.ids <- paste(mapped.uniprot.ids, uniprot.ids, sep="")


anno.table <- as.matrix(read.table(anno.file, sep="\t", header=TRUE))
gene.symbols <- anno.table[match(new.uniprot.ids, anno.table[,1]),2]
gene.symbols[is.na(gene.symbols)] <- ""

suppl.table <- cbind(tf.ids, new.uniprot.ids, gene.symbols, source.database, anno.classes, anno.super, pred.super, pred.correct)


num.failed.step1 <- length(which(is.na(pred.super)))
num.failed.step2 <- sum(pred.correct == "False") - num.failed.step1

cat(sprintf("Number of failed inputs for TF prediction: %i / %i (%.2f%%)\n", num.failed.step1, length(pred.super),
            num.failed.step1/length(pred.super)*100))

cat(sprintf("Number of failed inputs for superclass prediction: %i / %i (%.2f%%)\n", num.failed.step2, length(pred.super)-num.failed.step1,
            num.failed.step2/(length(pred.super)-num.failed.step1)*100))

write.table(suppl.table, file="/rahome/eichner/projects/tfpredict/failed_inputs/allTFs_withNewDomains_TableS1.txt", sep="\t", col.names=F, row.names=F, quote=F)

