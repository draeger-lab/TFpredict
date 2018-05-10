# removes whitespace characters at the beginning and end of a given string 
trim <- function(x) {

  # replace non-breaking space characters with normal space characters
  x <- gsub("\u00a0", " ", x)
  x <- gsub("^ *", "", x);
  x <- gsub(" *$", "", x);
  
  x
}
                           
