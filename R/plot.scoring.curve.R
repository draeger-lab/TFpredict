plot.scoring.curve <- function(pred.scores, labels, curve.type="roc", plot.curve=TRUE, col="blue", outfile=NULL, print.auc=TRUE,
                               auc.barplot=TRUE, use.layout=TRUE, return.cutoffs=FALSE, title=NULL, add.roc.point=NULL, flip.below.random=TRUE, scale.prediction.scores=FALSE) {

  library(ROCR, cran)  
#import.package("ROCR", "cran")
  source("/home/jtyurkovich/git/TFPredict/R/get.colormap.R")
  #import.function("get.colormap.R")
  # don't need the bar plots on page 9 of Eichner et al. - JTY
  source("/home/jtyurkovich/git/TFPredict/R/plot.bars.R")  
  #import.function("plot.bars.R")
  # don't need this - JTY
  source("/home/jtyurkovich/git/TFPredict/R/trim.R")  
  #import.function("trim.R")
  source("/home/jtyurkovich/git/TFPredict/R/scale.linear.R")  
  #import.function("scale.linear.R")
  if (!is.null(outfile)) {
    if (auc.barplot) {
      pdf.width <- 12
    } else {
      pdf.width <- 8
    }
    pdf(file=outfile, width=pdf.width, height=8)
  }
  if (curve.type == "roc") {
    x.measure <- "fpr"
    y.measure <- "tpr"
  } else if (curve.type == "prc") {
    x.measure <- "tpr"
    y.measure <- "ppv"
  } else {
    print("Unknown curve type. This function supports only roc and prc curves.")
  }
  if (is.list(pred.scores)) {
    num.folds <- length(pred.scores)
  } else {
    num.folds <- 1
    pred.scores <- list(pred.scores)
    labels <- list(labels)
  }
  # convert all elements of pred.scores to matrices
  pred.scores <- lapply(pred.scores, function (x) {
    if (!is.matrix(x)) {
      return(matrix(x, nrow=1))
    } else {
      return(x)
    }
  })
  num.methods <- nrow(pred.scores[[1]])
  method.names <- rownames(pred.scores[[1]])
  if (is.null(method.names)) {
    method.names <- paste("Method", 1:num.methods, sep=".")
  }
  # prepare prediction scores for use with ROCR package
  pred.score.list <- list()
  for (m in 1:num.methods) {
    curr.meth.pred <- list()
    for (f in 1:num.folds) {
      curr.meth.pred[[f]] <- pred.scores[[f]][m,]
      if (scale.prediction.scores) curr.meth.pred[[f]] <- scale.linear(curr.meth.pred[[f]])
    }
    pred.score.list[[method.names[m]]] <- curr.meth.pred
  }
  # generate list of ROCR prediction objects 
  pred <- lapply(pred.score.list, function (x) ROCR::prediction(x, labels))
  # generate list of ROCR performance objects  
  perf <- lapply(pred, function (x) performance(x, y.measure, x.measure))

  # select threshold, corresponding to best tradeoff between sensitivity and specificity
  if (curve.type == "roc") {
    thresholds <- matrix(NA, nrow=num.methods, ncol=num.folds, dimnames=list(method=method.names, fold=paste("Fold", 1:num.folds, sep=".")))
    for (m in 1:num.methods) {
      for (f in 1:num.folds) {
        thres.idx <- which.min(abs(perf[[m]]@x.values[[f]] - (1-perf[[m]]@y.values[[f]])))
        thresholds[m,f] <- perf[[m]]@alpha.values[[f]][thres.idx]
      }
    }
  }
  
  # compute AUC
  auc <- matrix(0, nrow=num.methods, ncol=num.folds, dimnames=list(method=method.names, fold=paste("Fold", 1:num.folds, sep=".")))
  for (f in 1:num.folds) {
    x.values <- lapply(perf, function (x) x@x.values[[f]])
    y.values <- lapply(perf, function (x) x@y.values[[f]])

    for (m in 1:num.methods) {
      # fix origin of precision-recall curve
      if (curve.type == "prc" && is.na(y.values[[m]][1])) {
        y.values[[m]][1] <- 0
        perf[[m]]@y.values[[f]][1] <- 0
      }
      auc[m,f] <- calc.auc(x.values[[m]], y.values[[m]])
    }
  }
  
  # add additional threshold value if classifier always predicts same score
  for (m in 1:num.methods) {
    missing.thres.idx <- which(sapply(perf[[m]]@alpha.values, length) == 2)
    if (length(missing.thres.idx) > 0) {
      for (idx in missing.thres.idx) {
        perf[[m]]@alpha.values[[idx]] <- c(perf[[m]]@alpha.values[[idx]], min(perf[[m]]@alpha.values[[idx]])-1)
        perf[[m]]@x.values[[idx]] <- c(perf[[m]]@x.values[[idx]], max(perf[[m]]@x.values[[idx]]))
        perf[[m]]@y.values[[idx]] <- c(perf[[m]]@y.values[[idx]], max(perf[[m]]@y.values[[idx]]))
      }
    }
  }

  # flip labels if classification performance is weaker than random guessing
  if (curve.type == "roc" && flip.below.random) {
    mean.method.auc <- rowMeans(auc)
    if (any(mean.method.auc < 0.5)) {
      flipped.label.idx <- which(mean.method.auc < 0.5)
      auc[flipped.label.idx,] <- 1-auc[flipped.label.idx,]
      for (m in flipped.label.idx) {
        for (idx in 1:length(perf[[m]]@y.values)) {
          curr.x.values <- perf[[m]]@x.values[[idx]]
          perf[[m]]@x.values[[idx]] <- perf[[m]]@y.values[[idx]]
          perf[[m]]@y.values[[idx]] <- curr.x.values
          warning(paste("Labels of", method.names[m], "were flipped due to performance below random guessing."))
        }
      }
    }
  }

  # compute AUC of threshold-averaged curve
  if (print.auc) {
    avg.perf <- lapply(perf, get.thres.avg.perf)
    avg.auc <- sapply(avg.perf, function (x) calc.auc(x@x.values[[1]], x@y.values[[1]]))
  }
  
  # plot scoring curves
  if (plot.curve) {

    # add barplot showing AUC for each method
    if (auc.barplot) {
      if (use.layout) {
        layout(matrix(1:2, nrow=1), widths=c(2,1))
      }
      par(mar=c(6.1,4.1,4.1,2.1))
    }

    # set method to compute average ROC curve for multiple folds
    if (num.folds == 1) {
      curve.avg <- "none"
    } else {
      curve.avg <- "threshold"
    }
    
    if (curve.type == "roc") {
      score.name <- "ROC score"
      main <- "ROC curve"
    } else if (curve.type == "prc") {
      score.name <- "Precision-Recall score"
      main <- "Precision-Recall curve"
    }
    if (!is.null(title)) {
      main <- title
    }

    # plot performance curve
    col.map=get.colormap(col.scheme="classes", num.classes=num.methods)
    for (m in 1:num.methods) {
      plot(perf[[m]], col=col.map[m], add=m>1, xlim=c(0,1), ylim=c(0,1), cex.main=1.0, cex.lab=1.0, xaxis.cex.axis=1.0, yaxis.cex.axis=1.0,
           yaxis.las=1, lwd=3, main=main, avg=curve.avg)
    }
    
    # add random guessing line
    if (curve.type == "roc") {
      lines(0:1, 0:1, lty=3)
    }

    # add TPR/FPR point computed for method with binary prediction outcomes
    if (!is.null(add.roc.point)) {
      points(x=add.roc.point[1], y=add.roc.point[2], col="black", lwd=2, pch=8)
    }
    
    # plot legend
    if (print.auc) {
      legend.labels <- paste(names(avg.auc), ": AUC = ", format(avg.auc, digits=3), sep="")
    } else {
      legend.labels <- rownames(auc)
    }
    legend("bottomright", legend.labels, fill=col.map, cex=1.0, bg="white")
    
    # barplot with AUC for each method
    #if (auc.barplot == TRUE) {
    #  if (print.auc) {
    #    auc.mean <- avg.auc
    #    auc.std <- NULL
    #  } else {
    #    auc.mean <- apply(auc, 1, mean)
    #    auc.std <- apply(auc, 1, sd)
    #  }
    #  plot.bars(auc.mean, error.bars=auc.std, ylab="Area under the curve", col=col.map, ylim=c(0,1), cex=1.0, cex.lab=1.0, cex.axis=1.0, text.cex=0.7)
    #}
  }

  if (!is.null(outfile)) {
    dev.off()
  }
  if (return.cutoffs && curve.type == "roc") {
    return(list(auc=auc, avg.auc=avg.auc, cutoffs=thresholds))
  } else {
    return(auc)
  }
}

# Computes area under the curve based on trapezoidal rule
calc.auc <- function(x,y) {
  sum(diff(x)*(y[-1]+y[-length(y)]))/2
}

# computes threshold-averaged curves (code was taken from ROCR package)
get.thres.avg.perf <- function(perf) {

  # for infinite cutoff, assign maximal finite cutoff + mean difference between adjacent cutoff pairs
  if (length(perf@alpha.values)!=0)
    perf@alpha.values <- lapply(perf@alpha.values, function(x) {
      isfin <- is.finite(x)
      x[is.infinite(x)] <- max(x[isfin]) + mean(abs(x[isfin][-1] - x[isfin][-length(x[isfin])]))
      return(x)
    })

  # remove samples with x or y not finite
  for (i in 1:length(perf@x.values)) {
    ind.bool <- (is.finite(perf@x.values[[i]]) &
                 is.finite(perf@y.values[[i]]))
    
    if (length(perf@alpha.values)>0)
      perf@alpha.values[[i]] <- perf@alpha.values[[i]][ind.bool]
    
    perf@x.values[[i]] <- perf@x.values[[i]][ind.bool]
    perf@y.values[[i]] <- perf@y.values[[i]][ind.bool]
  }
  
  perf.sampled <- perf
  alpha.values <- rev(seq(min(unlist(perf@alpha.values)),
                          max(unlist(perf@alpha.values)),
                          length=max( sapply(perf@alpha.values, length))))
  
  for (i in 1:length(perf.sampled@y.values)) {
    perf.sampled@x.values[[i]] <-
      approxfun(perf@alpha.values[[i]],perf@x.values[[i]],
                rule=2, ties=mean)(alpha.values)
    perf.sampled@y.values[[i]] <-
      approxfun(perf@alpha.values[[i]], perf@y.values[[i]],
                rule=2, ties=mean)(alpha.values)
  }
  
  # compute average curve
  perf.avg <- perf.sampled
  perf.avg@x.values <- list( rowMeans( data.frame( perf.avg@x.values)))
  perf.avg@y.values <- list(rowMeans( data.frame( perf.avg@y.values)))
  perf.avg@alpha.values <- list( alpha.values )

  return(perf.avg)
}
