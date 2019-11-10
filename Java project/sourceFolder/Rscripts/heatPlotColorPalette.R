heatPlotColorPalette = function(useColor)
{
  #if (useColor) return (c("#be311a", "white","steelblue4", "black"))
  if (useColor) return (viridis(12))
  #if (useColor) return (inferno(12))
  return (c("steelblue4", "white", "#be311a"))
}

heatPlotContourPalette = function(useColor){
  if (useColor)
    return (list("black", "red", "white"))
  else
    return (list("black", "red", "white"))
    #return (list(low = "grey50", high = "black"))
}

missingColor = function(){
  return ("burlywood1")
}