createLinePlot = function(
  dataframe, 
  outputFolder, 
  dimensions, 
  title, 
  
  minDV=NA,
  maxDV=NA, 
  ncol = 2,
  
  displaySDs = T,
  SDToPlot = 0.5,
  SDAlpha = 0.15,
  
  colVec=NA,
  shapeVec=NA,
  displayShapes = T,
  
  width=21.0, 
  height=29.7, 
  arrangeGrobHeights = NULL,
  arrangeGrobWidths = NULL,
  
  fileType= ".png",
  DPI=600, 
  save = T, 
  returnPlot = T){
  
  # Provide some feedback:
  cat(paste("\nPlotting heatplot with name: \"", title, "\"\n", sep = "" ))
  
  # Constants that determine the size, height and width of various components in the plot 
  # These values can easily be tweaked manually
  textSizeTitle   = 16          # Size of the title (if applicable)
  textSizeLabels  = 14          # Size of all the text (labels, axes) in the plot
  textSizeNumbers = 12          # Size of all the numbers (axes, legend) in the plot
  lineWidth       = 1
  pointSizeColor  = 3
  
  
  ##############################################################################
  ##//////////////////////////////////////////////////////////////////////////##
  ##/////////////////////        Start of code       /////////////////////////##
  ##//////////////////////////////////////////////////////////////////////////##
  ##############################################################################
  df = dataframe
  
  ###############################################################
  ######################  Figure out dimensions #################
  ###############################################################
  # Determine which variables go to which roles (e.g., x axis, y axis etc)
  # Note that roles may be unused 
  
  # Determine x axis
  xAxis = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "xAxis")
      xAxis = dimensions[[i]]
  
  if (is.null(xAxis))
    stop("Error: no x axis specified.")
  df[,xAxis$var$varName] = as.factor(df[,xAxis$var$varName])
 
  # Determine what variable should be plotted per line
  lines = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "lines")
      lines = dimensions[[i]]
  
  # Determine on what the images should be split (splitBy)
  splitBy = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "splitBy")
      splitBy = dimensions[[i]]
  
  # Determine the DV
  dv = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "dv")
      dv = dimensions[[i]]
  
  
  # Set the colVec if required
  if (is.na(colVec)){
    
    if (is.null(lines)){
      colVec = "black"
    } else if (length(lines$levels) < 3){
      colVec = c("black", "grey50")
    } else {
      colVec = brewer.pal(n=length(lines$levels), name = "RdYlGn")
    }
  }
  
  # Set the shapeVec if required
  if (is.na(shapeVec)){
    if (is.null(lines))
      shapeVec = 1
    else
      shapeVec = seq(1, length(lines$levels), 1)
  }

  # figure out how many rows the total plot should have (keeping in mind that the legend takes 1 space as well, if there is one)
  if (is.null(splitBy)){
    if (!is.null(lines)){
      nPlots = 2
      nrow = nPlots %/% ncol + (nPlots %% ncol != 0)
    }else {
      nPlots= 1
      nrow = 1
      ncol=1
    }
    
  } else{
    nPlots = length(splitBy$levels)+1
    nrow = nPlots %/% ncol + (nPlots %% ncol != 0)
  }
 
  ##### Filters & constants
  ### Determine constants
  constants = list();
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "constant")
      constants[[length(constants)+1]] = dimensions[[i]]
  
  # High pass filter(s): exclude all values lower than the first argument
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "highPass"){
      df[,dimensions[[i]]$var$varName] = as.numeric(as.character(df[,dimensions[[i]]$var$varName]))
      df = df[df[,dimensions[[i]]$var$varName] >= dimensions[[i]]$levels[1],]
    }
  
  
  # Low pass filter(s): exclude all values higher than then first argument
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "lowPass"){
      df[,dimensions[[i]]$var$varName] = as.numeric(as.character(df[,dimensions[[i]]$var$varName]))
      df = df[df[,dimensions[[i]]$var$varName] <= dimensions[[i]]$levels[1],]
    }
  
  # If required, winsorize the values
  if (!is.na(maxDV)){ 
    df[,dv$var$varName] =  (1-(df[,dv$var$varName]  > maxDV))*df[,dv$var$varName] + ((df[,dv$var$varName] > maxDV)*maxDV)
  } else {
    maxDV = max(df[,dv$var$varName]) }
  
  if (!is.na(minDV)){ 
    df[,dv$var$varName] =  (1-(df[,dv$var$varName]  < minDV))*df[,dv$var$varName] + ((df[,dv$var$varName] < minDV)*minDV)
  } else {
    minDV = min(df[,dv$var$varName]) }
  
  
  
  
  
  
  
  ###############################################################
  ######################  Clean the data frame  #################
  ###############################################################
  ### For the next step, clean out all data we will not use - remove constants 
  # Constants
  if (length(constants) > 0)
    for (c in 1:length(constants)){
      if (is.null(constants[[c]]$levels))
        stop(paste("The variable ", constants[[c]]$var, " is marked at a constant, but no level to keep it constant is provided.", sep=""))
      df = subset(df, df[,constants[[c]]$var$varName] == constants[[c]]$levels )
    }
  
  
  ####################################################################
  ######################  Creating aggregate groups  #################
  ####################################################################
  # Creating groups for splitBy (different plots) 
  df$splitByGroup = 1

  if (!is.null(splitBy)){
    
    # If the parameter of the splitBy is a list, the user specified a number c("level name", "minimum of level", "maximum of level"), one for
    # each group level/label, and we have to subset the data accordingly:
    #override the original values by their group number membership
    # for instance, if the original array is [0,1,2,4,5,6,0] and the split is such that group 1 = [0,1,2]
    # and group 2 = [3,4,5,6], then the array will be [1,1,1,2,2,2,2,1]
    temp = df[0,]
    
    if (typeof(splitBy$levels) == "list"){
      # For each group: the dimensions for splitBy should have 3 values per level (name, min, max)
      for (level in 1:length(splitBy$levels)){ 
        if (length(splitBy$levels[[level]]) != 3)
          stop(paste("Error when parsing the splitBy variable: the argument is a list, indicating a grouping variable. 
                 However, at the ", level, "th level there is an incorrect number of argument specified. 
                   The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        
        minValue = as.numeric(as.character(splitBy$levels[[level]][[2]]))
        maxValue = as.numeric(as.character(splitBy$levels[[level]][[3]]))
        
        if (is.na(minValue) | is.na(maxValue))
          stop(paste("Error when parsing the splitBy variable: at the ", level, "th level there is a non-numeric variable in the 2nd or 3rd place. 
                     The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        
        s = subset(df, df[,splitBy$var$varName] >= minValue & df[,splitBy$var$varName] < maxValue  )
        
        # Set group ownership
        s$splitByGroup= level
        
        if (nrow(s)==0)
          warning(paste("Warning when parsing the splitBy variable: at the ", level, "th level there are no selected cases. Note that 
                     the argument syntax is list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        temp = rbind(temp, s)
      }
    }
    
    # If splitBy is a vector of doubles, include only those values specified
    if (typeof(splitBy$levels) == "double")
      for (level in 1:length(splitBy$levels)){
        s= subset(df, df[,splitBy$var$varName] == splitBy$levels[[level]] )
        s$splitByGroup = level
        temp = rbind(temp, s)
      }
    
    df = temp
  }

  
  # Creating groups for the lines (within plots)
  df$linesGroup = 1
  if (!is.null(lines)){
    temp = df[0,]
    
    # If the parameter of lines is a list, the user specified a number c("level name", "minimum of level", "maximum of level"), one for
    # each group level/label, and we have to subset the data accordingly:
    # override the original values by their group number membership
    # for instance, if the original array is [0,1,2,4,5,6,0] and lines is such that group 1 = [0,1,2]
    # and group 2 = [3,4,5,6], then the array will be [1,1,1,2,2,2,2,1]
    
    if (typeof(lines$levels) == "list"){
      # For each group: the dimensions for lines should have 3 values per level (name, min, max)
      for (level in 1:length(lines$levels)){ 
        if (length(lines$levels[[level]]) != 3)
          stop(paste("Error when parsing the lines variable: the argument is a list, indicating a grouping variable. 
                 However, at the ", level, "th level there is an incorrect number of argument specified. 
                   The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        
        minValue = as.numeric(as.character(lines$levels[[level]][[2]]))
        maxValue = as.numeric(as.character(lines$levels[[level]][[3]]))
        
        if (is.na(minValue) | is.na(maxValue))
          stop(paste("Error when parsing the lines variable: at the ", level, "th level there is a non-numeric variable in the 2nd or 3rd place. 
                     The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        
        s = subset(df, df[,lines$var$varName] >= minValue & df[,lines$var$varName] < maxValue  )
        
        # Set group ownership
        s$linesGroup = level
        
        if (nrow(s)==0)
          warning(paste("Warning when parsing the lines variable: at the ", level, "th level there are no selected cases. Note that 
                     the argument syntax is list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        temp = rbind(temp, s)
      }
    }
    
    # If lines is a vector of doubles, include only those values specified
    if (typeof(lines$levels) == "double")
      for (level in 1:length(lines$levels)){
        s = subset(df, df[,lines$var$varName] == lines$levels[[level]] )
        s$linesGroup = level
        temp = rbind(temp, s)
      }
    
    df = temp
  }
  
  
  ################################################################
  ######################  Create the line plots  #################
  ################################################################
  legend = NULL         # To save the legend in
  plots = list()
  
  labels = c("filler")
  if (!is.null(lines)){
    labels = c()
    for (level in 1:length(lines$levels))
      labels = c(labels, lines$levels[[level]][[1]])
  }
  
  
  for (level in 1:length(unique(df$splitByGroup))){
   
    subsetData = subset(df, splitByGroup == level )
    
    # create the aggregate for means
    subsetData = subsetData[, c(xAxis$var$varName, dv$var$varName, "linesGroup")]
    colnames(subsetData) = c("x", "y", "lines")
    agg = aggregate(subsetData, by=list(subsetData$x,subsetData$lines), FUN=function(x) mean(as.numeric(as.character(x)), na.rm=T))
   
    
    # calculate where the ribbon should go: it is the mean y +/- [SDToPlot] * sd. However,
    # if these values are larger than the plot range, they should be winsorized to fit in the plot
    aggSD = aggregate(subsetData, by=list(subsetData$x,subsetData$lines), FUN=function(x) sd(as.numeric(as.character(x)), na.rm=T))
    agg$ySD = aggSD$y
    agg$ySDMin = minDV + ( agg$y - SDToPlot * agg$ySD ) * (! ( agg$y - SDToPlot*  agg$ySD ) < minDV  )
    agg$ySDMax = ( agg$y + SDToPlot * agg$ySD ) * (  agg$y + SDToPlot * agg$ySD < maxDV  ) +
                  maxDV                         * (  agg$y + SDToPlot * agg$ySD > maxDV  )
    agg$lines = as.factor(agg$lines)
    
    # Create the plot, leaving the legend in for now
    plotTitle = ""
    if (!is.null(splitBy)) plotTitle = paste(splitBy$var$shortName, " = ", splitBy$levels[[level]][[1]])
    p=ggplot(agg, aes(x=x, y=y)) +
      ggtitle (plotTitle) +  
    
      ylim(c(minDV,maxDV))+
      
      ylab(dv$var$longName) +
      xlab(xAxis$var$longName)+
      theme_bw()+
      theme( panel.grid.minor = element_blank(),
             legend.title          = element_text(size=textSizeLabels),
             legend.text           = element_text(size=textSizeNumbers))
    
    # Layer 1: the ribbons
    if (displaySDs){
      p=p+geom_ribbon(aes(ymin = ySDMin, ymax = ySDMax, fill=lines), alpha = SDAlpha)+
        scale_fill_manual(lines$var$shortName, values = colVec, labels =labels) 
    }
    
    # Layer 2: the lines
    p = p + geom_line(aes(color = lines), size = lineWidth)+
            scale_color_manual(lines$var$shortName, values = colVec, labels =labels)
    
    # Layer 3: the shapes
    if (displayShapes){
      p=p+geom_point(color='white', size = pointSizeColor) + 
        geom_point(aes(color = lines, shape = lines), size = pointSizeColor)+
        scale_shape_manual(lines$var$shortName, values=shapeVec, labels = labels)
    } 
    
  
     
    # If there is not ready a legend defined: change the legend and extract the grob for the legend
    if (is.null(legend) & !is.null(lines)){
      p=p+guides(fill= guide_colorbar(barheight=unit((height-2.5)/nrow, "cm"), barwidth = unit((width-2.5)/ncol, "cm")))
      
      # This part is created by stackoverflow user "dickoa" at:
      # https://stackoverflow.com/questions/11883844/inserting-a-table-under-the-legend-in-a-ggplot2-histogram
      tmp <- ggplot_gtable(ggplot_build(p))
      leg <- which(sapply(tmp$grobs, function(x) x$name) ==  "guide-box")
      legend = tmp$grobs[[leg]]
      
    }
    
    # Remove the legend from the plot
    p = p + theme(legend.position       = "none")
    plots[[length(plots)+1]] = p
  }
  
  # create the complete plot 
  # Total plot
  if (!is.null(lines))
    plots[[length(plots)+1]] = legend
  layoutMatrix = matrix(seq(1,nrow*ncol,1), ncol=ncol, nrow = nrow, byrow=T)
  top=textGrob(title, gp=gpar(fontface="bold", fontsize=textSizeTitle ))
  p=arrangeGrob(grobs = plots,  layout_matrix = layoutMatrix, top=top, heights = arrangeGrobHeights, widths = arrangeGrobWidths)

  # save if required
  if (save)
    ggsave(plot=p, filename =  paste(outputFolder, gsub("\\.", "-", title), fileType, sep=""), 
           width = width, height = height, units = "cm", dpi = DPI)
  
  if (returnPlot)
    return (p)
}




  