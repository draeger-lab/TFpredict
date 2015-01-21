calc.fpr <- function(pred.classes, labels) {

  import.package("ROCR")

  pred <- prediction(pred.classes, labels)
  fpr <- performance(pred, "fpr")@y.values[[1]][2]

  return(fpr)
}
