calc.tpr <- function(pred.classes, labels) {

  import.package("ROCR")

  pred <- prediction(pred.classes, labels)
  tpr <- performance(pred, "tpr")@y.values[[1]][2]

  return(tpr)
}
