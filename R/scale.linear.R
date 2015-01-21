scale.linear <- function(x) {
  # if all values are equal, no scaling is possible, just cut to 0 to 1 range
  if (all(x-min(x)==0)) sapply(x, function(i) max(0,min(1,i)))
  else x <- (x-min(x)) / max(x-min(x))
  return(x)
}
