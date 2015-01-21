plot.bars <- function(heights, error.bars=NULL, xlab="", ylab="", main="", col.map=NULL, ylim=c(0, max(as.vector(heights))), plot.grid=TRUE,
                      las=2, cex=1.2, cex.lab=1.3, cex.axis=1.3, text.angle=45, text.font=1, text.cex=1.0, log="", plot.title=NULL) {

  import.function("get.colormap.R")
  import.package("gplots", load.silent=T)

  # obtain color map
  if (is.null(col.map)) {
    num.col <- length(heights)
    if (is.matrix(heights)) {
      num.col <- nrow(heights)
    }
    col.map <- get.colormap(col.scheme="classes", num.classes=num.col)
  }

  # increase length of y axis if error bars shall be added
  if (!is.null(error.bars) && !is.na(error.bars)) {
    ylim <- range(c(ylim, range(heights+error.bars)))
  }
  if (abs(ylim[2]) > 20) {
    ylim <- ceiling(ylim/10)*10
  }
  if (log == "y") {
    ylim[1] <- 1
  }

  # set plot parameters
  text.spacer <- ""
  text.adj <- c(0.5,1.5)
  if (text.angle > 20) {
    text.spacer <- "    "
    text.adj <- 1
  }
  if (is.matrix(heights)) {
    beside <- TRUE
    bar.names <- paste(colnames(heights), text.spacer, sep="")

  } else {
    beside <- FALSE
    bar.names <- paste(names(heights), text.spacer, sep="")
  }
  
  # generate bar plot
  bar.pos <-barplot2(heights, beside=TRUE, col=col.map, main=main, xlab=xlab, ylab=ylab, ylim=ylim, las=las, cex=cex,
                      cex.lab=cex.lab, cex.axis=cex.axis, axisnames=FALSE, names.arg=NULL, plot.grid=plot.grid, log=log, xpd=F,)
  abline(h=ylim[1], lwd=1)
  
  # add (rotated) labels to bars
  bar.text.pos <- bar.pos
  if (is.matrix(heights)) {
    bar.text.pos <- colMeans(bar.pos)+nrow(heights)/4
  }
  text(bar.text.pos, par("usr")[3], srt=text.angle, adj=text.adj, labels=bar.names, xpd=TRUE, font=text.font, cex=text.cex)

  # add error bars  (if desired)
  if (!is.null(error.bars)) {
    error.bar(bar.pos, heights, error.bars)
  }
  if (!is.null(plot.title)) title(plot.title)
}

# adds error bars to a bar plot
error.bar <- function(x, y, upper, lower=upper, length=0.1,...) {

  if (length(x) != length(y) | length(y) !=length(lower) | length(lower) != length(upper)) {
    stop("vectors must be same length")
  }
  
  arrows(x,y+upper, x, y-lower, angle=90, code=3, length=length/2, ...)
}
