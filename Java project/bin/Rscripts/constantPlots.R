# Returns a ggplot object containing only the x-axis itself.
getXAxisPlot = function(dataframe,xAxis, showAxisLabel, textSizeLabels, textSizeNumbers)
{
  values = unique(as.numeric(as.character(dataframe[,xAxis$var$varName])))
  if (length(values)> 5){
    minimum = round(min(values), digits=3)
    maximum = round(max(values), digits=3)
    mid = round((maximum-abs(minimum))/2, digits = 3)
    breaks = c(minimum, (mid-abs(minimum))/2, mid,mid+(maximum-mid)/2, maximum)
    for (i in 1:length(breaks))
      breaks[i] = values[which(abs(breaks[i]-values)==min(abs(values-breaks[i])))][1]
  }
  
  if (length(values) <= 5)
    breaks =  unique(as.numeric(as.character(dataframe[,xAxis$var$varName])))
  
  p=ggplot(data=dataframe, aes(x = as.factor(dataframe[,xAxis$var$varName]))) +
    xlab("")+
    scale_x_discrete(breaks = breaks)+
    theme(
      axis.text.y=element_blank(),
      axis.ticks.y=element_blank(),
      axis.title.y=element_blank(),
      axis.line.x = element_line(),
      panel.grid.minor.y=element_blank(),
      panel.grid.major.y=element_blank(),
      panel.background = element_blank(),
      
      axis.text             = element_text(size=textSizeNumbers),
      axis.title            = element_text(size=textSizeLabels),
      axis.line             = element_line()
    )
  
  if (showAxisLabel) {
    p = p + 
      theme(axis.title.x = element_text(hjust=-0))+
      xlab(xAxis$var$longName)
  }
  return (p)
  
}

# Returns a ggplot object containing only the y-axis itself.
getYAxisPlot = function(dataframe,yAxis, showAxisLabel, textSizeLabels, textSizeNumbers)
{
  values = unique(as.numeric(as.character(dataframe[,yAxis$var$varName])))
  
  if (length(values) > 5){
    minimum = round(min(values), digits=3)
    maximum = round(max(values), digits=3)
    mid = round((maximum-abs(minimum))/2, digits = 3)
    breaks = c(minimum, (mid-abs(minimum))/2, mid,mid+(maximum-mid)/2, maximum)
    for (i in 1:length(breaks))
      breaks[i] = values[which(abs(breaks[i]-values)==min(abs(values-breaks[i])))][1]
  }
  
  if (length(values) <= 5)
    breaks = unique(as.numeric(as.character(dataframe[,yAxis$var$varName])))
  
  p=ggplot(data=dataframe, aes(y = as.factor(dataframe[,yAxis$var$varName]))) +
    ylab("")+ 
    scale_y_discrete(breaks = breaks)+
    theme(
      axis.text.x=element_blank(),
      axis.ticks.x=element_blank(),
      axis.title.x=element_blank(),
      axis.line.y = element_line(),
      panel.grid.minor.x=element_blank(),
      panel.grid.major.x=element_blank(),
      panel.background = element_blank(),
      
      axis.text             = element_text(size=textSizeNumbers),
      axis.title            = element_text(size=textSizeLabels),
      axis.line             = element_line())
  
  if (showAxisLabel) {
    p = p + 
      theme(axis.title.y = element_text(hjust=0))+
      ylab(yAxis$var$longName)
  }
  return (p)
}

# Return the legend grob
getLegend = function(dataframe, color, minColor, maxColor, textSizeLabels, textSizeNumbers, legendHeight, legendWidth, colorPalette, contour){
  x = seq(0,1,0.1)
  y = seq(0,1,0.1)
  d = as.data.frame(expand.grid(x,y))
  colnames(d)= c("x", "y")
  d$col = rnorm(nrow(d), 0, 1)
  d$con = rnorm(nrow(d), 0, 1)
  
  limitsCol = c(minColor-0.025*abs(minColor), maxColor+0.025*abs(maxColor))
  
  p = ggplot(data=d, aes(x=x, y = y, fill = col))+
    geom_tile()+
    scale_fill_gradientn(paste(color$var$shortName, ""),limits = limitsCol, colours = colorPalette)+
    theme(legend.position       = "right",
          legend.title          = element_text(size=textSizeLabels),
          legend.text           = element_text(size=textSizeNumbers))
  
  if (!is.null(contour)){
    limitsCont = c(min(dataframe[,contour$var$varName]), 1.025*max(dataframe[,contour$var$varName]))
    p = p + geom_contour(aes(z=con, colour = ..level..),  breaks = contour$levels,   size = 0.75) +
      scale_color_gradientn(name = paste(contour$var$shortName, ""), colors=colorPalette, limits =limitsCont)+
      
      guides(color= guide_colorbar(barheight=unit(legendHeight/2, "cm"), barwidth = unit(legendWidth, "cm"), order = 0),
             fill = guide_colorbar(barheight=unit(legendHeight/2, "cm"), barwidth = unit(legendWidth, "cm"), order = 1))
    
  } else {
    p=p+guides(fill= guide_colorbar(barheight=unit(legendHeight, "cm"), barwidth = unit(legendWidth, "cm")))
  }
  
  # This part is created by stackoverflow user "dickoa" at:
  # https://stackoverflow.com/questions/11883844/inserting-a-table-under-the-legend-in-a-ggplot2-histogram
  tmp <- ggplot_gtable(ggplot_build(p))
  leg <- which(sapply(tmp$grobs, function(x) x$name) ==  "guide-box")
  legend <- tmp$grobs[[leg]]
  
  return(legend)
}

# Creates and returns a completely empty plot. Useful for spacing.
emptyPlot = function(){
  return (
    ggplot()+geom_blank(aes(1,1))+
      theme(
        plot.background = element_blank(), 
        panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(), 
        panel.border = element_blank(),
        panel.background = element_blank(),
        axis.title.x = element_blank(),
        axis.title.y = element_blank(),
        axis.text.x = element_blank(), 
        axis.text.y = element_blank(),
        axis.ticks = element_blank(),
        axis.line = element_blank()))
}

# Creates and returns a completely empty plot with a missingColor background. Useful for indicating missing rows/columns.
missingPlot = function(){
  return (
    ggplot()+geom_blank(aes(1,1))+
      theme(
        panel.background = element_rect(fill = missingColor(), colour = "black"),
        panel.grid.major = element_blank(),
        panel.grid.minor = element_blank(), 
        axis.title.x = element_blank(),
        axis.title.y = element_blank(),
        axis.text.x = element_blank(), 
        axis.text.y = element_blank(),
        axis.ticks = element_blank(),
        axis.line = element_blank())
  )
}
