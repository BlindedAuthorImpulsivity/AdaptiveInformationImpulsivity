# Creates and returns a ggplot object containing a heatplot. 
# includeLegend only used when there is no row or column (or both) specified
# firstContourArgumentDenotesNumberOfGroups: if true creates x different contour lines, where x is the first argument of the contour dimensions. Lines are drawn at
#                                               at equidistant value within the range [contour minimum value, contour maximum value].
#                                               IMPORANT: if the first argument is not an integer value, this parameter is automatically set to False.
#                                            if false draws a contour line at each level specified.
createSinglePane = function(dataframe, xAxis, yAxis, color, textSizeLabels,textSizeNumbers, LW, LH, minColor, maxColor, colorPalette, missingColor, contour, onlyPlot=F, includeLegend=T, firstContourArgumentDenotesNumberOfGroups=F)
{
  # No non-integers allowed if firstContourArgumentDenotesNumberOfGroups = F!
  if (!is.null(contour))
    if (firstContourArgumentDenotesNumberOfGroups && contour$levels[[1]]%%1!=0 )
      firstContourArgumentDenotesNumberOfGroups=FALSE
    
    # Create the aggregate that we use for plotting
    if (is.null(contour)){
      agg = dataframe[, c(xAxis$var$varName, yAxis$var$varName, color$var$varName)]
      colnames(agg) = c("x", "y", "z")
      agg = aggregate(agg, by=list(agg$x,agg$y), FUN=function(x) mean(as.numeric(as.character(x)), na.rm=T))
    } else {
      agg = dataframe[, c(xAxis$var$varName, yAxis$var$varName, color$var$varName, contour$var$varName)]
      colnames(agg) = c("x", "y", "z", "c")
      agg = aggregate(agg, by=list(agg$x,agg$y),FUN=function(x) mean(as.numeric(as.character(x)), na.rm=T))
    }
    
    # Determine the minimum and maximum of the x and y variables
    minimumY = min(dataframe[,yAxis$var$varName])
    maximumY = max(dataframe[,yAxis$var$varName])
    minimumX = min(dataframe[,xAxis$var$varName])
    maximumX = max(dataframe[,xAxis$var$varName])
    
    # Determine the mimimum and maximum of the color range
    limitsCol = c(minColor-0.025*abs(minColor), maxColor+0.025*abs(maxColor))
    
    p = ggplot(agg, aes(x = x, y =y, fill = z))+
      annotate("rect", xmin = -Inf, xmax = Inf, ymin = -Inf, ymax=Inf, fill = missingColor )+
      geom_raster(interpolate = F)+
      scale_fill_gradientn("",limits = limitsCol, colours = colorPalette, na.value = missingColor)+
      theme(panel.grid = element_blank(),
            panel.background = element_blank()
      ) 
    
    # If there is only one heat plot
    if (onlyPlot){
      p = p + ylab(yAxis$var$longName)+
        xlab(xAxis$var$longName)+
        scale_y_continuous(breaks = c(minimumY, (minimumY+ maximumY)/2,maximumY), expand = c(0.01,0.01))+
        scale_x_continuous(breaks = c(minimumX, (minimumX+ maximumX)/2,maximumX), expand = c(0.01,0.01))+
        theme(axis.text             = element_text(size=textSizeNumbers, margin = margin(0,0,0,0,"cm")),
              axis.title            = element_text(size=textSizeLabels),
              panel.grid.major = element_blank(),
              panel.grid.minor = element_blank(),
              panel.background = element_blank(),
              axis.line.x = element_line(color="black", size = 0.5),
              axis.line.y = element_line(color="black", size = 0.5),
              legend.position="none"
        )
      
      # Plot the legend as well?
      if (includeLegend){
        p = p + theme(legend.position       = "right",
                      legend.title          = element_text(size=textSizeLabels),
                      legend.text           = element_text(size=textSizeNumbers))
        if (is.null(contour))
          p=p+guides(fill= guide_colorbar(barheight=LH, barwidth = LW, title = gsub(" ", "\n",color$var$shortName)))
        if (!is.null(contour))
          p=p+guides(fill= guide_colorbar(barheight=LH/2, barwidth = LW, title = gsub(" ", "\n",color$var$shortName), order = 1))
      }
      
      # If there are multiple rows and/or columns, remove the legend, axes and all stuff that is not the actual heat plot - they will be added later on
    } else { 
      p = p + scale_y_continuous(breaks = c(minimumY, (minimumY+ maximumY)/2,maximumY), expand = c(0,0))+
        scale_x_continuous(breaks = c(minimumX, (minimumX+ maximumX)/2,maximumX), expand = c(0,0))+
        theme(panel.grid.major = element_blank(),
              panel.grid.minor = element_blank(),
              axis.ticks.x=element_blank(),
              axis.ticks.y=element_blank(),
              axis.text.x=element_blank(),
              axis.text.y=element_blank(),
              
              #panel.border = element_blank(),
              panel.background = element_blank(),
              axis.title.x=element_blank(),
              axis.title.y=element_blank(),
              legend.position="none"
        )
    }
    
    # Do we need to plot some contour lines as well? If not, return immediately - we're done.
    if (is.null(contour))
      return (p)
    
    # Figure out where the contour lines should be
    # if firstContourArgumentDenotesNumberOfGroups==T: draw at equidistance between (min, max)
    if (firstContourArgumentDenotesNumberOfGroups){
      width = (max(agg$c) - min(agg$c))/(contour$levels[[1]]+1)
      breaks = round(seq(0, contour$levels[[1]]-1)*width + min(agg$c), digits=3)
      
      # if firstContourArgumentDenotesNumberOfGroups==F: draw a line at each specified level  
    } else {
      breaks = contour$levels
    }
    
    # Add the transparent line underneath the contours
    p = p + geom_contour(aes(z=c), color = "grey80", breaks=breaks, size = 1.25, alpha = 0.3)
    
    # Determine the limits of the contour colors (should be slightly larger than the actual range - otherwise the legend name and the highest tick mark sometimes overlap)
    
    limitsCont = c(min(dataframe[,contour$var$varName])-0.025*abs(min(dataframe[,contour$var$varName])), max(dataframe[,contour$var$varName])+0.025*abs(max(dataframe[,contour$var$varName])))
    
    #Draw the actual contour lines
    p = p + geom_contour(
      aes(z=c, colour = ..level..),
      breaks = breaks, 
      size = 0.35 
    )+
      scale_color_gradientn(name = contour$var$shortName, colors=colorPalette, limits =limitsCont, guide = F)
    
    # If this is the only plot and we want to include the legend: also add a guide for the contour lines
    if (onlyPlot && includeLegend)
      p=p+guides(color= guide_colorbar(barheight=LH/2, barwidth = LW, title = gsub(" ", "\n",contour$var$shortName), order = 0))
    
    # Labels on the left
    #    p = p + geom_dl(aes(z = c, label = paste(round(..level.., digits = (..level.. < 10)*1 + (..level..<1)*1),"  ", sep = "")), 
    #                    breaks = breaks,
    #                    stat = "contour", 
    #                    method =  list(
    #                      dl.trans(y = y - 0.3, x = x +0.3),
    #                      cex = 0.5,
    #                     fill = rgb(1,1,1,0.75),
    #                      box.color = "transparent",
    #                      "first.bumpup",
    #                      "calc.boxes", 
    #                      "enlarge.box",
    #                      "draw.rects"
    #                    ))
    #    
    #    # Labels on the right
    #    p = p + geom_dl(aes(z = c, label = paste("   ",round(..level.., digits = (..level.. < 10)*1 + (..level..<1)*1), sep = "")), 
    #                    breaks = breaks, 
    #                    stat = "contour", 
    #                    method =  list(
    #                      dl.trans(y = y - 0.3, x = x -0.3),
    #                      cex = 0.5,
    #                      fill = rgb(1,1,1,0.75),
    #                     box.color = "transparent",
    #                      "last.bumpup",
    #                     "calc.boxes", 
    #                      "enlarge.box",
    #                      "draw.rects"
    #                    ))
    return (p)
}