
#------- createHeatPlot function -----------------------------------------------------------------------------------------------------------
createHeatPlot = function(
  dataframe,                                            # Required: The dataframe that contains all the to-be-plotted data
  outputFile,                                           # Required: The file to save the plot to (required affix like .png or .pdf)
  
  dimensions,                                           # Required: A list where all elements denote a single plotting dimension.
  # A dimension is itself a list, containing the following three named elements:
  #   "var": another list containing (1) "varName", the variable name in the dataframe, 
  #                                  (2) "longName", the full name of the variable shown on the x and y axes, 
  #                                  (3) "shortName", the name shown on the legend if this name is a dependent variable
  #   "role": the role of this variable in the plot. Options include:
  #           "xAxis":   Plot the variable on the x axis
  #           "yAxis":   Plot the variable on the y axis
  #           "row":     Keep this variable constant at a specific value, create multiple panels in the plot, layered by rows, where each panel containts a different constant specific value. Requires "levels".
  #           "column":  Keep this variable constant at a specific value, create multiple panels in the plot, layered by column, where each panels contains a different constant specific value. Requires "levels".
  #           "constant":   Keep this variable constant at a all times at the specified level. "levels".
  #           "highPass":   Keep all values of this variable HIGHER than the specified value and remove  all values of this variable LOWER than the specified level. Requires "levels".
  #           "lowPass":   Keep all values of this variable LOWER than the specified value and remove  all values of this variable HIGHER than the specified level. Requires "levels".
  #           "color":   Assign this variable as the dependent variable on the heat plot - specifically, use colors to represent values of this variable. 
  #           "contour":   Assign this variable as the dependent variable on the contour plots - specifically, add contour lines to the plots to represent values of this variable. Requires "levels".
  #   "levels": optional, depending on the role of the variable. Provides an additional parameter to the role.
  
  # Color and contour line settings
  maxColor=NULL,                                        # Optional: The maximum value that the color variable can take. All values higher in this variable are set to maxColor. Default is to not winsorize. 
  minColor=NULL,                                        # Optional: The minimum value that the color variable can take. All values lower in this variable are set to minColor. Default is to not winsorize. 
  colorFactorLevels = NULL,                             # Optional: a numeric vector. If specified, the color variable is shown as a factor (rather than as a continuous score). Values in the color variable are rounded to the nearest factor level.
  colorFactorLabels = NULL,                             # Optional: a string vector. If colorFactorLevels has been specified, this vector provides optional names for each factor.
  aggregationFunction = NULL,                           # Optional: a function. If there are multiple data points per pixel, how should these data points be aggregated? Default is mean. Note that if an
                                                        #   aggregation function is specified, the maxColor and minColor variables no longer refer to individual data points, but to the result of the aggregation function.
  colorPalette = viridis(12),                           # Optional: A list of colors specifying the legend of the color variable. Default is the viridis color palette.
  contourPalette = viridis(12),
  missingColor = "pink",                                # Optional: The color used for missing values. Default is "pink".
  firstContourArgumentDenotesNumberOfGroups=F,          # Optional: If there is a contour variable, does the levels parameter specify how many lines should be drawn (F) or at values the lines should be drawn (T)? Default is FALSE.
 
  # Plot size settings (all in cm)
  width=21.0-2.5,                                       # Optional: Width in centimeter of the complete plot. This includes a margin on all sides. Default is 21.0-2.5 (A4 format including margin for most word processors)).
  height=29.7-2.5,                                      # Optional: Height in centimeter of the complete plot (including title and description). This includes a margin on all sides. Default is 29.7-2.5 (A4 format including margin for most word processors).
  plotMarginTopBottom = 0.5,                            # The size (in cm) of the white space above and below the total plot. 
  plotMarginLeftRight = 0.5,                            # The size (in cm) of the white space to the left and right of the total plot. 
  
  # Legend settings
  noLegend = F,                                         # Optional: a boolean. If true, no legend is shown.
  legendTitle = NULL,                                   # Optional: a string. If specified this string will be used as the variable name, instead of the name specified in the dimensions.
  legendHeightMaximum = 12.5,                           # Optional: The maximum height (in cm) that the legend is allowed to be. Default is 12.5
  legendWidthMaximum = NULL,                            # Optional: The maximum width (in cm) that the legend is allowed to be. Default depends on whether a density legend or a default legend is used.
  legendAsDensity=F ,                                   # Optional: if true, use a density plot as the legend for the color variable
  densityTransformationFunction=NULL,                   # Optional: a function. If specified, the values on the density legend are transformed using this function (e.g., values can be plotted on a kwadratic or logarithmic scale)
  legendBreaks=NULL,                                    # Optional: a numeric vector. If not null, break lines in the legend are drawn on each specified value. 
  legendBreakLabels=NULL,                               # Optional: a string vector. If legendBreaks is specified, this vector specifies the labels used on the tick marks specified by legendBreaks
  
  
  # Text and text size settings
  title = "",                                           # Optional: The title of the plot, shown in the top level. Default is absent. 
  titleSize = 0.5,                                      # Optional: The height in centimeters dedicated to the title 
  description = NULL,                                   # Optional: The discription of the plot. If the discription is specified, the title and discription are displayed in the lower left side (using APA style)
  descriptionSize = 2.5,                                # Optional: Number of centimeters dedicated to the description block.
  useLongNameOnXYAxes=T,                                # Optional: a logical. If true the long name is used on the x and y axes. If false, the short name is used.
  textSizeTitle             = 11,                       # Optional: The size of the title (if applicable)
  textSizeLegendTitle       = 11,                       # Optional: The size of the legend's title
  textSizeDescription       = 9,                        # Optional: The size of the discription (if applicable)
  textSizeHeaders           = 11,                       # Optional: The size of the column and row labels (i.e., names)
  textSizeRowColumnLevels   = 11,                       # Optional: The size of the individual levels of the columns and rows
  textSizeLabels            = 10,                       # Optional: The size of all the text (axes) in the plot
  textSizeNumbers           = 9,                       # Optional: The size of all the numbers (axes, legend) in the plot
  
  # Other widths/heights
  sizeDividerLegend         = .50 ,                     # Optional: a numeric. Specifies the width between the heat plots and the legend
  sizeDividerAxes           = 0.30,                     # Optional: a numeric. Specifies the width between the x and y axis label and the  and row/column label
  sizeRowColumnLevels       = 0.75,                     # Optional: a numeric. Specifies the size of the row/column labels grobs (i.e., individual levels, eg., SD = 1)
  sizeColumnLabel           = 0.75,                     # Optional: a numeric. Specifies the size of the column labels grob (i.e., the variable name). This is only used if the columns are specified with labels
  sizeRowLabel              = 0.75,                     # Optional: a numeric. Specifies the size of the row labels grob (i.e., the variable name). This is only used if the rows are specified with labels
  widthYAxis                = 1.3,                      # Optional: a numeric. Specifies the width of the Y axis in cm
  heightXAxis               = 0.75,                     # Optional: a numeric. Specifies the height of the X axis in cm

  # Other settings
  transformation = NULL,                                # Optional: A transformation that specifies the value-to-color mapping. Currently not implemented.
  DPI=600,                                              # Optional: DPI of resulting plot. Default is 600.
  numberedLabels = T,                                   # Optional: Include values for each column (1-n) and rows (A-Z). Default is true. 
  save = T,                                             # Optional: Save plot to file? Default is true.
  returnPlot = F                                        # Optional: return the resulting plot after this function call is completed? Default is false.
 ){
  
  # Provide some feedback:
  if (save)
    cat(paste("\nPlotting heatplot with filename: \"", outputFile, "...",  sep = "" ))
  if (!save)
    cat(paste("\nPlotting heatplot with title: \"", title, "...",  sep = "" ))
  
  # Set the data frame df
  df = dataframe
  
  # Make sure that the data frame is indeed a data frame - and not a data table 
  if (is(df, 'data.table')){
    warning("The data is specified in a data table format. This function transforms the data table to a data frame (which might be a slow process.")
    setDF(df)
  }
  
  # if colorFactorLevels is specified: check if there is a color in the color palette for each factor and visa versa
  # If the color has a factorLevels argument, the color should be plotted in discrete colors. If so, make sure that the colorPalette has exactly one color for each factor level
  if (!is.null(colorFactorLevels))
    if (length(colorFactorLevels) != length(colorPalette))
      stop("The number of discrete levels specified for the color does not match the number of colors specified in the color palette.")
  
  if (!is.null(colorFactorLevels) & !is.null(colorFactorLabels))
    if (length(colorFactorLevels) != length(colorFactorLabels))
      stop("The number of discrete levels does not match the number of labels specified for the color dimension. ")
  
  ###############################################################
  ######################  Figure out dimensions #################
  ###############################################################
  # Determine which variables go to which roles (e.g., x axis, y axis etc)
  # Note that roles may be unused
  
  
  ### Independent variables
  # Determine x axis
  xAxis = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "xAxis")
      xAxis = dimensions[[i]]
  
  if (is.null(xAxis))
    stop("Error: no x axis specified.")
  
  # Determine y axis
  yAxis = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "yAxis")
      yAxis = dimensions[[i]]
  
  if (is.null(yAxis))
    stop("Error: no y axis specified.")
  
  
  # Determine the rows
  rows = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "row")
      rows = dimensions[[i]]
  
  
  # Determine the column
  columns = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "column")
      columns = dimensions[[i]]
  
  
  ### Dependent variables
  # Determine the color
  color = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "color")
      color = dimensions[[i]]
  
  # Determine the contour
  contour = NULL;
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "contour")
      contour = dimensions[[i]]
  
  ### Determine constants
  constants = list();
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "constant")
      constants[[length(constants)+1]] = dimensions[[i]]
  
  
  # High pass filter(s): exclude all values lower than the first argument
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "highPass"){
      df[,dimensions[[i]]$var$varName] = as.numeric(as.character(df[,dimensions[[i]]$var$varName]))
      #df[df[,dimensions[[i]]$var$varName] < dimensions[[i]]$levels[1] ,color$var$varName] = NA
      df = df[df[,dimensions[[i]]$var$varName] >= dimensions[[i]]$levels[1],]
    }
  
  
  # Low pass filter(s): exclude all values higher than then first argument
  for (i in 1:length(dimensions))
    if (dimensions[[i]]$role == "lowPass"){
      df[,dimensions[[i]]$var$varName] = as.numeric(as.character(df[,dimensions[[i]]$var$varName]))
      #df[df[,dimensions[[i]]$var$varName] > dimensions[[i]]$levels[1] ,color$var$varName] = NA
      df = df[df[,dimensions[[i]]$var$varName] <= dimensions[[i]]$levels[1],]
    }
  
  
  # If required, winsorize the values
  # If an aggregation function is specified, no windoring is required (as the values to plot are not actually these values)
  if (!is.null(maxColor) & is.null(aggregationFunction)){ 
    df[,color$var$varName] =  (1-(df[,color$var$varName]  > maxColor))*df[,color$var$varName] + ((df[,color$var$varName] > maxColor)*maxColor)
  } else if (is.null(maxColor)){
    maxColor = max(df[,color$var$varName]) }
  
  if (!is.null(minColor) & is.null(aggregationFunction)){ 
    df[,color$var$varName] =  (1-(df[,color$var$varName]  < minColor))*df[,color$var$varName] + ((df[,color$var$varName] < minColor)*minColor)
  } else if (is.null(minColor)){
    minColor = min(df[,color$var$varName]) }
  
  
  
  ###########################################################
  ######################  Determine layout  #################
  ###########################################################
  # Constants that determine the size, height and width of various components in the plot 
  # These values can easily be tweaked manually
  titleMargin     = 0.4         # The margin between the plot and the title (and, if applicable, the description)
  
  W               = width - plotMarginLeftRight*2      # width of image plot
  H               = height - plotMarginTopBottom*2 - titleSize -titleMargin    # height of image plot
  # Does the plot include a description? If so, reduce the height of the image plot a bit
  if (!is.null(description))
    H = H - descriptionSize    
  
  # There is a legend area (the total area allocated to the legend) and the actual legend (the real estate in the legend area
  # used up by the legend). The height of the area is determined during run time, as it is equal to the height of all
  # heat plot panels. Note that for density plots as legends we have to allocate a little bit more space
  if (legendAsDensity){
    legendAreaWidth = 2.5 # in cm
    if (is.null(legendWidthMaximum))
      legendWidthMaximum  = 2.5 # in cm
  } else {
    legendAreaWidth = 2 # in cm
    if (is.null(legendWidthMaximum))
      legendWidthMaximum  = 0.5 # in cm
  }
 
  legendWidth = min(legendAreaWidth, legendWidthMaximum)
  
  # If the row object specifies labels, we have to plot the row variable name on the row label section. 
  # If the row object does not specify labels, no row label section has to be created
  # Figure out if the rows object has a 'labels' vector that we have to use.
  # Does the xAxis object contain labels that we have to place at the tick marks?
  if (!'labels' %in% names(rows)){
    rowLabelsSpecified = F
  } else {
    if (is.null(rows$labels)){
      rowLabelsSpecified = F
    } else  if (length(rows$labels) != length(rows$levels)){
      warning("Specified row levels and labels. However, the number of labels and levels do not match.")
      rowLabelsSpecified = F
    } else {
      rowLabelsSpecified = T
    }
  }
  
  if (!rowLabelsSpecified)
    sizeRowLabel = 0
  
  # If the column object specifies labels, we have to plot the column variable name on the column label section. 
  # If the column object does not specify labels, no column label section has to be created
  # Figure out if the rows object has a 'labels' vector that we have to use.
  # Does the xAxis object contain labels that we have to place at the tick marks?
  if (!'labels' %in% names(columns)){
    columnLabelsSpecified = F
  } else {
    if (is.null(columns$labels)){
      columnLabelsSpecified = F
    } else  if (length(columns$labels) != length(columns$levels)){
      warning("Specified column levels and labels. However, the number of labels and levels do not match.")
      columnLabelsSpecified = F
    } else {
      columnLabelsSpecified = T
    }
  }
  
  if (!columnLabelsSpecified)
    sizeColumnLabel = 0
  
  
  # If noLegend is true, do not allocate width to the legend
  if (noLegend){
    sizeDividerLegend = 0
    legendAreaWidth = 0
  }
  
  # Determine the width and height of the heat plot panels
  if (is.null(rows)) { nrow = 1 } else { nrow = length(rows$levels) }
  if (is.null(columns)) { ncol = 1 } else { ncol = length(columns$levels) }
  
  heatPlotHeight = (H - heightXAxis - ( sizeColumnLabel + sizeRowColumnLevels + sizeDividerAxes)*(ncol>1))/ nrow
  heatPlotWidth  = (W - widthYAxis -  sizeDividerLegend - legendAreaWidth - ( sizeRowColumnLevels + sizeRowLabel +  sizeDividerAxes) * (nrow>1))/ncol
  
  #Now that we kknow the heat plot heights, we can compute the height of the legend
  legendAreaHeight = heatPlotHeight * nrow
  legendHeight = min(legendAreaHeight, legendHeightMaximum)
  

  
  ###############################################################
  ######################  Clean the data frame  #################
  ###############################################################
  ### For the next step, clean out all data we will not use - constants, unused row/column levels
  # Constants
  if (length(constants) > 0)
    for (c in 1:length(constants)){
      if (is.null(constants[[c]]$levels))
        stop(paste("The variable ", constants[[c]]$var, " is marked at a constant, but no level to keep it constant is provided.", sep=""))
      df = subset(df, df[,constants[[c]]$var$varName] == constants[[c]]$levels )
    }
  
  # Rows
  df$groupByRows = 1
  if (!is.null(rows)){
    temp = df[0,]
    
    # If the parameter of the rows is a list, the user specified a number c("level name", "minimum of level", "maximum of level"), one for
    # each group level/label, and we have to subset the data accordingly
    if (typeof(rows$levels) == "list"){
      
      # The row levels should have three variables
      for (level in 1:length(rows$levels)){ 
        if (length(rows$levels[[level]]) != 3)
          stop(paste("Error when parsing the row variable: the argument is a list, indicating a grouping variable. 
                     However, at the ", level, "th level there is an incorrect number of argument specified. 
                     The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        
        minValue = as.numeric(as.character(rows$levels[[level]][[2]]))
        maxValue = as.numeric(as.character(rows$levels[[level]][[3]]))
        
        if (is.na(minValue) | is.na(maxValue))
          stop(paste("Error when parsing the row variable: the argument is a list, indicating a grouping variable. 
                     However, at the ", level, "th level there is a non-numeric variable in the 2nd or 3rd place. 
                     The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        s = subset(df, df[,rows$var$varName] >= minValue & df[,rows$var$varName] < maxValue  )
        
        # Set group ownership
        s$groupByRows= level
        
        if (nrow(s)==0)
          warning(paste("Warning when parsing the row variable: the argument is a list, indicating a grouping variable. 
                        However, at the ", level, "th level there are no selected cases. Note that 
                        the argument syntax is list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        temp = rbind(temp, s)
      }
      
    }
    
    # If the parameter of the rows is a double (i.e., a vector of doubles), select only the entries where the row variable's value is
    # one of the values specified
    
    if (typeof(rows$levels) == "double")
      for (level in 1:length(rows$levels)){
        s= subset(df, df[,rows$var$varName] == rows$levels[[level]] )
        s$groupByRows = level
        temp = rbind(temp, s)
      }
    df = temp
  }
  
  
  # Columns
  df$groupByColumns = 1
  if (!is.null(columns)){
    temp = df[0,]
    
    # If the parameter of the columns is a list, the user specified a number c("level name", "minimum of level", "maximum of level"), one for
    # each group level/label, and we have to subset the data accordingly
    if (typeof(columns$levels) == "list"){
      
      for (level in 1:length(columns$levels)){ 
        if (length(columns$levels[[level]]) != 3)
          stop(paste("Error when parsing the column variable: the argument is a list, indicating a grouping variable. 
                     However, at the ", level, "th level there is an incorrect number of argument specified. 
                     The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        
        minValue = as.numeric(as.character(columns$levels[[level]][[2]]))
        maxValue = as.numeric(as.character(columns$levels[[level]][[3]]))
        
        if (is.na(minValue) | is.na(maxValue))
          stop(paste("Error when parsing the column variable: the argument is a list, indicating a grouping variable. 
                     However, at the ", level, "th level there is a non-numeric variable in the 2nd or 3rd place. 
                     The argument should be list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        s = subset(df, df[,columns$var$varName] >= minValue & df[,columns$var$varName] < maxValue  )
        
        # Set group ownership
        s$groupByColumns= level
        
        if (nrow(s)==0)
          warning(paste("Warning when parsing the column variable: the argument is a list, indicating a grouping variable. 
                        However, at the ", level, "th level there are no selected cases. Note that 
                        the argument syntax is list(\"Label name\", minimum value (inclusive), maximum value (exclusive) ). ", sep = ""))
        temp = rbind(temp, s)
      }
      
    }
    
    # If the parameter of the columns is a double (i.e., a vector of doubles), select only the entries where the column variable's value is
    # one of the values specified
    
    if (typeof(columns$levels) == "double")
      for (level in 1:length(columns$levels)){
        s= subset(df, df[,columns$var$varName] == columns$levels[[level]] )
        s$groupByColumns = level
        temp = rbind(temp, s)
      }
    df = temp
  }
  
  
  
  
  
  ##################################################################################
  ######################  IF 1 ROW, 1 COLUMN: STOP: not supported  #################
  ##################################################################################
  if (is.null(rows) && is.null(columns))
    stop("Error: no rows or columns specified. This function does not support single plots, as the standard ggplot2 library is sufficient to make such plots (and I'll be honest, making a single plot look pretty is really difficult this way - better to go with ggplot)")
    
  ##################################################################
  ######################  Create heat plot images  #################
  ################################################################## 
  # Create all the heat plots. These are the 'images' without legends, axes or titles 
  heatPlots = list()
  heatPlotData = list()
  
  nrows = 1
  if (!is.null(rows)) nrows = length(rows$levels)
  ncols = 1
  if (!is.null(columns)) ncols = length(columns$levels)
  
  for (r in 1:nrows)
  {
    # Select the required rows for this plot
    subsetRow = subset(df, groupByRows== r)
    
    # create a row
    rowPlots = list()
    
    for (c in 1:ncols){
      # Select the required column values for this plot
      subsetPlot = subset(subsetRow, groupByColumns == c)
      
      if (nrow(subsetPlot) == 0){
        rowPlots[[length(rowPlots)+1]] = missingPlot(missingColor = missingColor) 
      } else {
        singlePaneAndData = createSinglePane(
          dataframe=subsetPlot, 
          xAxis=xAxis, 
          yAxis=yAxis, 
          color=color, 
          aggregationFunction = aggregationFunction,
          textSizeLabels=textSizeLabels,
          textSizeNumbers=textSizeNumbers,
          minColor = minColor,
          maxColor = maxColor, 
          colorPalette = colorPalette,
          missingColor = missingColor,
          contour=contour,
          contourPalette = contourPalette,
          firstContourArgumentDenotesNumberOfGroups = firstContourArgumentDenotesNumberOfGroups
        )
        
        rowPlots[[length(rowPlots)+1]] = singlePaneAndData$plot
        heatPlotData[[length(heatPlotData)+1]] = singlePaneAndData$data
      }
    }   
    heatPlots[[length(heatPlots)+1]] = rowPlots
  }
  
  
  
  
  
  ###########################################################
  ######################  Create other plots ################
  ###########################################################
  
  # Create an empty plot, containing only the x axis (with and without labels)
  xAxisPlotLabel = getXAxisPlot(dataframe=df, xAxis=xAxis, showAxisLabel=T, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers, useLongName = useLongNameOnXYAxes)
  xAxisPlot = getXAxisPlot(df, xAxis, F, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers, useLongName = useLongNameOnXYAxes)
  
  # Create an empty plot, containing only the y axis  (with and without labels)
  yAxisPlotLabel = getYAxisPlot(dataframe=df, yAxis=yAxis, showAxisLabel=T, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers, useLongName = useLongNameOnXYAxes)
  yAxisPlot = getYAxisPlot(df, yAxis, F, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers, useLongName = useLongNameOnXYAxes)
  
  # create a completely empty plot, used for spacing
  nullPlot = emptyPlot()
  
  
  # Create the legend
   if (legendAsDensity)
    dataToPlotInLegend=heatPlotData
  
  if (!legendAsDensity)
    dataToPlotInLegend = df
 
  legendPlot = getLegend(dataToPlotInLegend, 
                         color, 
                         minColor, 
                         maxColor, 
                         colorFactorLevels,
                         colorFactorLabels,
                         legendAsDensity, 
                         legendAreaWidth, 
                         legendAreaHeight, 
                         legendWidth, 
                         legendHeight, 
                         legendTitle,
                         textSizeLegendTitle, 
                         textSizeNumbers, 
                         colorPalette, 
                         contour, 
                         contourPalette, 
                         firstContourArgumentDenotesNumberOfGroups,
                         legendBreaks, 
                         legendBreakLabels,
                         densityTransformationFunction)
  
  
  # Create, if applicable, the text grobs for the row labels and levels
  if (!is.null(rows)){
    # Create the row label
    textRowLabel = ""
    if (rowLabelsSpecified)
      textRowLabel = rows$var$longName
    rowTextLabel  = textGrob(textRowLabel, rot = 90, gp = gpar(fontsize = textSizeHeaders, fill = rgb(0,0,0,0))) 
    
    # Create a list that contains all the grobs for the row levels
    rowTextLevels = list()
    
    # Create the numbering (i.e., the '(1)', '(2)' etc)
    numbering = c()
    for (i in 1:length(rows$levels)){
      numbering[i] = ""
      if (numberedLabels) numbering[i] = paste("(",i, ") ", sep = "")
    }
    
    # If the labels are prespecified in the rows object, use these labels to make the text grobs
    if (rowLabelsSpecified)
      for (lvl in 1:length( rows$labels)){
        txt = splitString( paste0(numbering[lvl], rows$labels[lvl]), unit(heatPlotHeight*0.9, units = "cm"))
        lines = lengths(regmatches(txt, gregexpr("\n", txt)))+1
        rowTextLevels[[length(rowTextLevels)+1]] = textGrob(txt, rot = 90, gp = gpar(fontsize = textSizeRowColumnLevels/lines))
      }
    
    # If the labels are not specified yet, create them
    if (!rowLabelsSpecified){
      if (typeof(rows$levels) == "double")
        for (r in 1: length(rows$levels)) {
          # get the level name
          if (typeof(rows$levels) == "double")
            lvl = rows$levels[r]
          else 
            lvl = rows$levels[[r]][[1]]
          
          #Determine name of the level breaks. If the user specified a labels part in the row variable, the breaks are named after these labels.
          # if no labels are specified, use the actual row value 
          if (exists('rows$labels')){
            breakName = rows$labels[r]
          } else {
            breakName = rows$levels[r]
          }
          
          # Here is some nice hack-around. Sometimes a name might contain an expression that we have to evaluate
          # For instance, there might be greek letters.
          # To evaluate this, we first need to create a string that contains the to-be-evaluated string (i.e., a string that starts with "paste(")
          
          # First, check if the variable name contains "<>". If it does, the name is an expression we have to evaluate 
          if (!grepl("<>", rows$var$shortName)) 
            # If not, easy
            txt = paste("paste('",numbering[r], rows$var$shortName, "=",breakName, "')",  sep = "")
          else {
            # if yes, time to create a to-be-evaluated string
            # First, remove the diamonds
            expr = gsub("<>", "", rows$var$shortName)
            txt = paste("paste('", numbering[r] , " ', ", expr, ", ' = ', '", breakName,  "')", sep = "")
          }  
          
          # Parse the string, and use it to create a textGrob for the row names
          rowTextLevels[[length(rowTextLevels)+1]] = textGrob(parse(text=txt), rot = 90, gp = gpar(fontsize = textSizeRowColumnLevels, fontface = 'italic'))
        }
    }

  }
  
  
  # Create, if applicable, the text grobs for the column labels and levels
  if (!is.null(columns)){
    # Create the row label
    textColumnLabel = ""
    if (columnLabelsSpecified)
      textColumnLabel = columns$var$longName
    columnTextLabel  = textGrob(textColumnLabel, gp = gpar(fontsize = textSizeHeaders, fill = rgb(0,0,0,0))) 
    
    # Create a list that contains all the grobs for the column levels
    columnTextLevels = list()
    
    # Create the numbering (i.e., the '(A)', '(B)' etc)
    numbering = c()
    for (i in 1:length(columns$levels)){
      numbering[i] = ""
      if (numberedLabels) numbering[i] = paste("(",LETTERS[i], ") ", sep = "")
    }
    
    # If the labels are prespecified in the column object, use these labels to make the text grobs
    if (columnLabelsSpecified)
      for (lvl in 1:length( columns$labels)){
        txt = splitString( paste0(numbering[[lvl]], columns$labels[[lvl]]), unit(heatPlotWidth*0.9, units = "cm"))
        lines = lengths(regmatches(txt, gregexpr("\n", txt)))+1
        columnTextLevels[[length(columnTextLevels)+1]] = textGrob(txt, gp = gpar(fontsize = textSizeRowColumnLevels/lines))
      }
    
    # If the labels are not specified yet, create them
    if (!columnLabelsSpecified){
      if (typeof(columns$levels) == "double")
        for (c in 1: length(columns$levels)) {
          # get the level name
          if (typeof(columns$levels) == "double")
            lvl = columns$levels[c]
          else 
            lvl = columns$levels[[c]][[1]]
          
          #Determine name of the level breaks. If the user specified a labels part in the row variable, the breaks are named after these labels.
          # if no labels are specified, use the actual row value 
          if (exists('columns$labels')){
            breakName = columns$labels[c]
          } else {
            breakName = columns$levels[c]
          }
          
          # Here is some nice hack-around. Sometimes a name might contain an expression that we have to evaluate
          # For instance, there might be greek letters.
          # To evaluate this, we first need to create a string that contains the to-be-evaluated string (i.e., a string that starts with "paste(")
          
          # First, check if the variable name contains "<>". If it does, the name contains an expression we have to evaluate 
          if (!grepl("<>", columns$var$shortName)) 
            # If not, easy
            txt = paste("paste('",numbering[c], columns$var$shortName, "=",breakName, "')",  sep = "")
          else {
            # if yes, time to create a to-be-evaluated string
            expr = gsub("<>", "", columns$var$shortName)
            txt = paste("paste('", numbering[c] , " ', ", expr, ", ' = ', '", breakName,  "')", sep = "")
          }  
          
          # Parse the string, and use it to create a textGrob for the row names
          columnTextLevels[[length(columnTextLevels)+1]] = textGrob(parse(text=txt), gp = gpar(fontsize = textSizeRowColumnLevels, fontface = 'italic'))
        }
    }
  }
  
  
  
  
  ###########################################################
  ######################  Merge all plots   #################
  ###########################################################
  # Determine the number of rows in the total plot (bottom to top)
  #             heat plots     x axis     optimal rows to display the column levels and column name
  nrowsInPlot = nrows         +  1        + (!is.null(columns))*3
  
  # Determine the number of column in the total plot (left to right)
  #             legend         heat plots     y axis   +   divider +    optimal columns to display the row levels and row name
  ncolsInPlot = 1 +            ncols       +  1        +    1      +  (!is.null(rows))*3
  
  # Now we have to split up the flow into three remaining possible options (remember, we already made the plot if there are no columns nor rows):
  #       case 1: rows, but not columns, have been specified
  #       case 2: columns, but not rows, have been specified
  #       case 3: both colums and rows have been specified
  
  
  # Case 1: rows, but not columns, have been specified
  # In a matrix, this plot has the following structure
  # 
  #   width=sizeRowLabel       width=sizeRowColumnLevels     width=sizeDividerAxes    width=widthYAxis width=heatPlotWidth         width=sizeDividerLegend    width=A
  #
  # _______________________________________________________________________________________________________________
  # [Row label           | Row = level 1    |           | y axis    | heatPlots[[1]]    |             | legend   ]      Row 1. height=(H-C)/n
  # [Row label           | Row = level 2    |           | y axis    | heatPlots[[2]]    |             | legend   ]      Row 2. height=(H-C)/n
  # [...                 | ...              |           | ...       | ...               |             | ...      ]
  # [Row label           | Row = level n    |           | y axis    | heatPlots[[n]]    |             | legend   ]      Row n. height=(H-C)/n
  # [                    |                  |           |           | X axis            |             |          ]             height=C
  # ______________________________________________________________________________________________________________
  #
  # Note that some elements (legend) refer to the same element stretched over multiple rows/columns,
  # while other elements are contained with each cell. If we give each individual entry an unique
  # number, we get the following layout for three rows
  #
  # [ 1  |  3 |  NA |  4 |  5 |  NA |   2 ]
  # [ 1  |  6 |  NA |  7 |  8 |  NA |   2 ]
  # [ 1  |  9 |  NA | 10 | 11 |  NA |   2 ]
  # [NA  | NA |  NA | NA | 12 |  NA |  NA ]
  #
  # Or, in the universal case (r indicating the row number, n the number of rows)
  # [ 1   |  3r  | NA |  3r+1 |  3r+2 | NA |   2 ]     (r=1)
  # [ 1   |  3r  | NA |  3r+1 |  3r+2 | NA |   2 ]     (r=2)
  # [ ..  | ...  | NA |  ...  |   ... | NA | ... ]
  # [ 1   |  3r  | NA |  3r+1 |  3r+2 | NA |   2 ]     (r=n-1)
  # [ NA  |  NA  | NA |  NA   |  3r   | NA |   2 ]     (r=n)
  if (!is.null(rows) && is.null(columns)){
    layout = c()
    for (r in 1:(nrowsInPlot-1))
      layout = rbind(layout, c(1,3*r,NA, 3*r+1,3*r+2,NA, 2)) # set all the rows
    layout = rbind(layout, c(NA, NA, NA, NA, (nrowsInPlot*3), NA, 2))
    
    # Give all NA's a new number
    for (r in 1:nrow(layout)) for (c in 1:ncol(layout)) if (is.na(layout[r,c])) layout[r,c] = max(layout, na.rm=T)+1
    
    grobs = list()
    grobs[[1]]    =    rowTextLabel         # Add row label @ 1
    grobs[[2]]    =    legendPlot           # Add legend @ 2
    
    # for all rows: add the new grobs to the grobs list
    for (r in 1:(nrowsInPlot-1)){
      grobs[[length(grobs)+1]]    =   rowTextLevels[[r]]
      if (r == nrowsInPlot-1) { grobs[[length(grobs)+1]]    =   yAxisPlotLabel} else { grobs[[length(grobs)+1]]    =   yAxisPlot}
      grobs[[length(grobs)+1]]    =   heatPlots[[r]][[1]]
    }
    
    # Add the x axis
    grobs[[length(grobs)+1]]      =   xAxisPlotLabel
    
    # Fill up the rest of the NA's with nullPlots
    while (length(grobs) != max(layout))
      grobs[[length(grobs)+1]]    =   nullPlot
    
    #Determine widths and heights
    widths = c(sizeRowLabel, sizeRowColumnLevels, sizeDividerAxes, widthYAxis, heatPlotWidth, sizeDividerLegend, legendAreaWidth)
    heights = c()
    for (r in 1:length(rows$levels))
      heights = c(heights, heatPlotHeight)
    heights = c(heights, heightXAxis)
    # Make the total plot
    p = grid.arrange(grobs=grobs, layout_matrix=layout, widths=unit(widths, "cm"), heights=unit(heights, "cm"))
  }
  
  
  # Case 2: columns, but not rows, have been specified
  # In a matrix, this plot has the following structure
  #   x = (W-widthYAxis-sizeDividerAxes-legendAreaWidth)/ncol
  
  #   w=widhtYAxis      w=x                w=x               ...    w=x                sizeDividerLegend        legendAreaWidth           
  # ___________________________________________________________________________________________
  # [          | COLUMN LABEL      | COLUMN LABEL   | ...  | COLUMN LABEL     |      | legend   ]  height = sizeColumnLabel
  # [          | Col = level 1     | Col = level 2  | ...  | Col = level n    |      | legend   ]  height = sizeRowColumnLevels
  # [          |                   |                | ...  |                  |      | legend   ]  height = sizeDividerAxes
  # [ Y axis   | heatPlots[[1]]    | heatPlots[[2]] | ...  | heatPlots[[n]]   |      | legend   ]  height = heatPlotHeight
  # [          | x axis label      | x axis         | ...  | x axis           |      |          ]  height = heightXAxis
  # ___________________________________________________________________________________________
  #
  # Note that some elements (legend) refer to the same element stretched over multiple rows/columns,
  # while other elements are contained with each cell. If we give each individual entry an unique
  # number, we get the following layout for three columns
  #
  # [ NA |  1 |  1 |  1 | NA | 2 ]    (column label)
  # [ NA |  3 |  4 |  5 | NA | 2 ]    (column levels)
  # [ NA | NA | NA | NA | NA | 2 ]    (divider)
  # [ 12 |  6 |  7 |  8 | NA | 2 ]    (heat plots)
  # [ NA |  9 | 10 | 11 | NA | 2 ]    (x axes)
  #
  # Or, in the universal case (c indicating the column number, ncol the number of columns)
  #              c=1        c=2      c=.   c=n
  # [ NA      |  1        | 1      | .. |  1      | NA | 2 ]    (column label)
  # [ NA      |  c+2      | c+2    | .. |  c+2    | NA | 2 ]    (column levels)
  # [ NA      |  NA       | NA     | .. |  NA     | NA | 2 ]    (divider)
  # [ c+2n+3  |  c+n+2    | c+n+2  | .. |  c+n+2  | NA | 2 ]    (heat plots)
  # [ NA      |  c+2n+2   | c+2n+2 | .. |  c+2n+2 | NA | 2 ]    (x axes)
  if (is.null(rows) && !is.null(columns)){
    
    # Determine the layout matrix. Start to make a matrix that does not yet include the legend divider and legend
    layout= c()
    
    # Row 1
    row = c(NA)
    for (c in 1:length(columns$levels))
      row = c(row, 1)
    layout=rbind(layout, row)
    
    # Row 2
    row = c(NA)
    for (c in 1:length(columns$levels))
      row = c(row, c+2)
    layout=rbind(layout, row)
    
    # Row 3
    layout = rbind(layout, rep(NA, ncolsInPlot-2))
    
    # Row 4
    row = c(c+2*length(columns$levels)+3)
    for (c in 1:length(columns$levels))
      row = c(row, c+length(columns$levels)+2)
    layout=rbind(layout, row)
    
    # Row 5
    row = c(NA)
    for (c in 1:length(columns$levels))
      row = c(row, c+2*length(columns$levels)+2)
    layout=rbind(layout, row)
    
    # Add legend divider and legend
    layout = cbind(layout, rep(NA, nrowsInPlot), rep(2, nrowsInPlot))
    
    # Give all NA's a new number
    for (r in 1:nrow(layout)) for (c in 1:ncol(layout)) if (is.na(layout[r,c])) layout[r,c] = max(layout, na.rm=T)+1
    
    # Create the grobs list
    grobs = list()
    grobs[[1]]    =    columnTextLabel          # Add column label @ 1
    grobs[[2]]    =    legendPlot               # Add legend @ 2
    
    # Add all the columns levels 
    for (c in 1:length(columns$levels)) grobs[[length(grobs)+1]] = columnTextLevels[[c]]
    # Add all the heat plots
    for (c in 1:length(columns$levels)) grobs[[length(grobs)+1]] = heatPlots[[1]][[c]]
    # Add the axes
    grobs[[length(grobs)+1]] = xAxisPlotLabel
    for (c in 2:length(columns$levels)) grobs[[length(grobs)+1]] = xAxisPlot
    grobs[[length(grobs)+1]] = yAxisPlotLabel
    
    # Fill up the rest of the NA's with nullPlots
    while (length(grobs) != max(layout))
      grobs[[length(grobs)+1]]    =   nullPlot
    
    #Determine widths and heights
    heights = c(sizeColumnLabel, sizeRowColumnLevels, sizeDividerAxes, heatPlotHeight, heightXAxis)
    widths = c(widthYAxis)
    for (c in 1:length(columns$levels)) widths = c(widths, heatPlotWidth)
    widths = c(widths, sizeDividerLegend, legendAreaWidth)
    
    # Make the total plot
    p = grid.arrange(grobs=grobs, layout_matrix=layout, widths=unit(widths, "cm"), heights=unit(heights, "cm"))
    
  }
  
  
  
  # Case 3: columns and rows have been specified
  # In a matrix, this plot has the following structure
  #   x = (W-widthYAxis-sizeDividerAxes-legendAreaWidth)/ncol
  # ____________________________________________________________________________________
  # [         |     |   |        | col lbl | col lbl | col lbl | col lbl  |   |        ] 
  # [         |     |   |        | c=1     | c=2     | c=...   | c=n      |   |        ] 
  # [         |     |   |        |         |         |         |          |   |        ]
  # [ row lbl | r=1 |   | yaxis  | H[1,1]  | H[1,2]  | H[1,.]  | H[1,n]   |   | legend ] 
  # [ row lbl | r=2 |   | yaxis  | H[2,1]  | H[2,2]  | H[2,.]  | H[2,n]   |   | legend ] 
  # [ row lbl | r=. |   | yaxis  | H[.,1]  | H[.,2]  | H[.,.]  | H[.,n]   |   | legend ] 
  # [ row lbl | r=n |   | yaxis  | H[n,1]  | H[n,2]  | H[n,.]  | H[n,n]   |   | legend ] 
  # [         |     |   |        | xaxis   | xaxis   | xaxis   | xaxis    |   |        ] 
  #
  # Which have (universally) the following indices
  #
  #
  #  Row lbl        row lvl          sp         yaxis           
  # _____________________________________________________________________________________________________________________________________
  # [         |                     |   |                           | 1               | 1               | 1    | 1               |   |   ]    (Column label header)
  # [         |                     |   |                           | 3+c             | 3+c             | ...  | 3+c             |   | 3 ]    (Column levels)
  # [         |                     |   |                           |                 |                 |      |                 |   | 3 ]    (spacer)
  # [ 2       | 3+r*ncol+(r-1)*2+1  |   | 3+r*ncol+(r-1)*2+2        | 3+r*ncol+2*r+c  | 3+r*ncol+2*r+c  | ...  | 3+r*ncol+2*r+c] |   | 3 ]    (heat plots, repeated for each row)
  # [         |                     |   | 3+ncol + nrow*(ncol+2)+c  | <-              | <-              | ...  | <-              |   | 3 ]    (xAxis)
  # ______________________________________________________________________________________________________________________________________
  if (!is.null(rows) && !is.null(columns)){
    # Create the grobs list and add the first three elements (column label, row label, legend)
    grobs = list()
    grobs[[1]] = columnTextLabel
    grobs[[2]] = rowTextLabel
    grobs[[3]] = legendPlot
    
    # Set the layout matrix, and already add the grobs to the grobslist
    ncol = length(columns$levels)
    nrow = length(rows$levels)
    
    # add the first row, containing the column label, excluding the legend spacer and legend
    layout = c(NA, NA, NA, NA)
    for (c in 1:ncol) layout = c(layout, 1)
    
    # add the second row, containing the column levels
    line = c(NA, NA, NA, NA)
    for (c in 1: ncol){ 
      line = c(line, 3+c)
      grobs[[length(grobs)+1]] = columnTextLevels[[c]]
    }
    layout = rbind(layout, line)
    
    # Add the third row containing the spacer between the column label/levels and the heat plots
    line = c()
    for (c in 1:(ncolsInPlot-2)) line = c(line, NA)
    layout = rbind(layout, line)
    
    #Add all rows containing heat plots
    for (r in 1: nrow){
      # Add the space/grobs for the row label (no grob), row level, and y axis
      line = c(2, 3+r*ncol+(r-1)*2+1, NA, 3+r*ncol+(r-1)*2+2)
      grobs[[length(grobs)+1]] = rowTextLevels[[r]]
      if (r == nrow) grobs[[length(grobs)+1]] = yAxisPlotLabel
      if (r <  nrow)grobs [[length(grobs)+1]] = yAxisPlot
      
      # add the heat plots
      for (c in 1: ncol){
        line = c(line, 3+r*ncol+2*r+c)
        grobs[[length(grobs)+1]] = heatPlots[[r]][[c]]
      }
      layout = rbind(layout, line)
    }
    
    # Add the x axis row
    line = c(NA, NA, NA, NA, 3+ncol+nrow*(ncol+2)+ncol)
    
    for (c in (ncol-1):1){
      line = c(line, 3+ncol+nrow*(ncol+2)+c)
      grobs[[length(grobs)+1]] = xAxisPlot
    }
    grobs[[length(grobs)+1]] = xAxisPlotLabel
    layout = rbind(layout, line)
    
    # Add the legend divider and legend columns to the plot
    layout = cbind(layout, rep(NA, nrowsInPlot))
    layout = cbind(layout, c(rep(NA,3),rep( 3, nrowsInPlot-4), NA))
    
    # Fill in all the NA's with numbers and corresponding nullplots
    for (r in 1:nrow(layout)) for (c in 1:ncol(layout)) if (is.na(layout[r,c])) layout[r,c] = max(layout, na.rm=T)+1
    while (length(grobs) != max(layout))
      grobs[[length(grobs)+1]]    =   nullPlot
    
    # Determine the widths of the plot
    widths = c(sizeRowLabel, sizeRowColumnLevels, sizeDividerAxes, widthYAxis)
    for (c in 1:ncol) widths = c(widths, heatPlotWidth)
    widths = c(widths, sizeDividerLegend, legendAreaWidth)
    
    # Determine the heights of the plot
    heights = c(sizeColumnLabel, sizeRowColumnLevels, sizeDividerAxes)
    for (r in 1: nrow)
      heights = c(heights, heatPlotHeight)
    heights = c(heights, heightXAxis)
    
    
    p = grid.arrange(grobs=grobs, layout_matrix=layout, widths=unit(widths, "cm"), heights=unit(heights, "cm"))
  }
  
  ###########################################################################
  ######################  Add margins, title, description   #################
  ###########################################################################
  # If there is only a title (and no description), place the title on the top
  if (is.null(description)){
    # Layout:
    # nullPlot                        nullPlot              nullplot |   (height = plotMarginTopBottom)
    # nullPlot                        title                 nullPlot |   (height = titleSize)
    # nullPlot                        nullPlot              nullPlot |   (height = titleMargin)
    # nullPlot                        imagePlot (p)         nullPlot |   (height = H)
    # nullPlot                        nullPlot              nullplot |   (height = plotMarginTopBottom)
    #-----------------------------------------------------------------------------------------------------
    # (width = plotMarginLeftRight)   (width = W)           (width = plotMarginLeftRight)
    
    titleGrob=textGrob(title, gp=gpar(fontface="bold", fontsize=textSizeTitle ),  just = c("left", "bottom"))
    grobsList = list(titleGrob,p)
    
    # Code nullPlot with NA
    layout = matrix ( c(NA, NA, NA,
                        NA, 1,  NA, 
                        NA, NA, NA, 
                        NA, 2,  NA, 
                        NA, NA, NA), ncol = 3, byrow = T)
    
    # Fill in all the NA's with numbers and corresponding nullplots
    for (r in 1:nrow(layout)) for (c in 1:ncol(layout)) if (is.na(layout[r,c])) layout[r,c] = max(layout, na.rm=T)+1
    while (length(grobsList) != max(layout))
      grobsList[[length(grobsList)+1]]    =   nullPlot
    
    totalPlot=grid.arrange(grobs = grobsList,
                           layout_matrix = layout,
                           widths = unit.c(
                             unit(plotMarginLeftRight, "cm"), 
                             unit(W, "cm"),
                             unit(plotMarginLeftRight, "cm") ),
                           heights = unit.c(
                             unit(plotMarginTopBottom, "cm"),
                             unit(titleSize, "cm"),
                             unit(titleMargin, "cm"),
                             unit(H, "cm"),
                             unit(plotMarginTopBottom, "cm")))
    
    
  }
  
  # If there a description, place both the title and description at the bottom left
  if (!is.null(description)){
    # Layout:
    # nullPlot                        nullPlot              nullplot |   (height = plotMarginTopBottom)
    # nullPlot                        imagePlot (p)         nullPlot |   (height = H)
    # nullPlot                        nullPlot              nullPlot |   (height = titleMargin)
    # nullPlot                        title                 nullPlot |   (height = titleSize)
    # nullPlot                        description           nullPlot |   (height = descriptionSize)
    # nullPlot                        nullPlot              nullplot |   (height = plotMarginTopBottom)
    #-----------------------------------------------------------------------------------------------------
    # (width = plotMarginLeftRight)   (width = W)           (width = plotMarginLeftRight)
    titleGrob=textGrob(title, gp=gpar(fontface="bold", fontsize=textSizeTitle ), just = c("left", "bottom"), x = 0)
    
    formattedExpression = parseDescription(description, width = unit(W*0.9, "cm"), textSize = textSizeDescription)
    
    descriptionGrob = textGrob(parse(text =formattedExpression  ), gp=gpar(fontfamily = "mono", fontsize = textSizeDescription),  just = c("left", "top"),x = 0, y = unit(descriptionSize*1.1, "cm"))
    
    grobsList = list(p, titleGrob,  descriptionGrob)
    
    # Code nullPlot with NA
    layout = matrix ( c(NA, NA, NA,
                        NA, 1,  NA, 
                        NA, NA, NA, 
                        NA, 2,  NA, 
                        NA, 3,  NA,
                        NA, NA, NA), ncol = 3, byrow = T)
    
    # Fill in all the NA's with numbers and corresponding nullplots
    for (r in 1:nrow(layout)) for (c in 1:ncol(layout)) if (is.na(layout[r,c])) layout[r,c] = max(layout, na.rm=T)+1
    while (length(grobsList) != max(layout))
      grobsList[[length(grobsList)+1]]    =   nullPlot
    
    totalPlot=grid.arrange(grobs = grobsList,
                           layout_matrix = layout,
                           widths = unit.c(
                             unit(plotMarginLeftRight, "cm"), 
                             unit(W, "cm"),
                             unit(plotMarginLeftRight, "cm") ),
                           heights = unit.c(
                             unit(plotMarginTopBottom, "cm"),
                             unit(H, "cm"),
                             unit(titleMargin, "cm"),
                             unit(titleSize, "cm"),
                             unit(descriptionSize, "cm"),
                             unit(plotMarginTopBottom, "cm")))

  }

  # Save the plot to the specified file
  if (save)
    ggsave(plot=totalPlot, filename = outputFile, width = width, height = height, units = "cm", dpi = DPI)
  
  # Provide some feedback:
  cat(" Done." )
  
  if (returnPlot)
    return (totalPlot)
}








































##############################################################################
#////////////////////////////////////////////////////////////////////////////#
#////     Supporing function: create a single panel     /////////////////////#
#////////////////////////////////////////////////////////////////////////////#
##############################################################################
# This function creates an aggregate data set to determine what should be plotted. It returns both the plot and the data used in a list. 
createSinglePane = function(dataframe, xAxis, yAxis, color, aggregationFunction, textSizeLabels,textSizeNumbers, minColor, maxColor, colorPalette, missingColor, contour, contourPalette,  firstContourArgumentDenotesNumberOfGroups=F)
{
  # No non-integers allowed if firstContourArgumentDenotesNumberOfGroups = F!
  if (!is.null(contour)) {
    if (firstContourArgumentDenotesNumberOfGroups && contour$levels[[1]]%%1!=0 ) {
      firstContourArgumentDenotesNumberOfGroups=FALSE }
  }
  
  # Figure out what aggregation function to use (i.e., whether there was one specified)
  wasSpecified = !is.null(aggregationFunction)
  if (!wasSpecified)
    aggregationFunction = function(x) mean(as.numeric(as.character(x)), na.rm=T)
  
  # Create the aggregate data that we use for plotting
  if (is.null(contour)){

    # Create the aggregate
    agg = dataframe[, c(xAxis$var$varName, yAxis$var$varName, color$var$varName)]
    colnames(agg) = c("x", "y", "z")
    agg = aggregate(agg, by=list(agg$x,agg$y), FUN=aggregationFunction)
    agg = agg[, c("Group.1", "Group.2", "z")]
    colnames(agg) = c("x", "y", "z")
    
  } else {
    agg = dataframe[, c(xAxis$var$varName, yAxis$var$varName, color$var$varName, contour$var$varName)]
    colnames(agg) = c("x", "y", "z", "c")
    agg = aggregate(agg, by=list(agg$x,agg$y),FUN=aggregationFunction)
    agg = agg[, c("Group.1", "Group.2", "z", "c")]
    colnames(agg) = c("x", "y", "z", "c")
  }
  
  # make sure all values are within minColor and maxColor
  # If there is no aggregation function, this is always true as we forced the minimum and maximum to be true at the start of the code
  # However, if there is an aggregation function the minimum and maximum are not enforced yet (as this is the first time we actually know the values to plot)
  if (wasSpecified){
    agg[,"z"] =  (1-(agg[,'z']  > maxColor))*agg[,'z'] + ((agg[,"z"] > maxColor)*maxColor)
    agg[,"z"] =  (1-(agg[,"z"]  < minColor))*agg[,"z"] + ((agg[,"z"] < minColor)*minColor)}
  
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
    scale_y_continuous(breaks = c(minimumY, (minimumY+ maximumY)/2,maximumY), expand = c(0.001,0.001))+
    scale_x_continuous(breaks = c(minimumX, (minimumX+ maximumX)/2,maximumX), expand = c(0.001,0.001))+
    theme(panel.grid = element_blank(),
          panel.background = element_blank()
    ) 
  
  #Remove all the axes, text etc
  p = p + 
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
  
  
  # Do we need to plot some contour lines as well? If not, return immediately - we're done.
  if (is.null(contour))
    return (list(plot=p, data = agg))
  
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
    scale_color_gradientn(name = contour$var$shortName, colors=contourPalette, limits =limitsCont, guide = F)
  
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
  return (list(plot=p, data = agg))
}
# ----


###########################################################################################################
#/////////////////////////////////////////////////////////////////////////////////////////////////////////#
#////     Supporing function: create all the constant plots (axes, legend, etc)     //////////////////////#
#/////////////////////////////////////////////////////////////////////////////////////////////////////////#
###########################################################################################################
# Returns a ggplot object containing only the x-axis itself.
getXAxisPlot = function(dataframe,xAxis, showAxisLabel, textSizeLabels, textSizeNumbers, useLongName)
{
  # Does the xAxis object contain some levels that we have to use?
  if (!'levels' %in% names(xAxis)){
    levelsSpecified = F
  } else {
    if (is.null(xAxis$levels)){
      levelsSpecified = F
    } else {
      levelsSpecified = T
    }
  }
  
  # If there are already specified levels, create tick marks on those levels
  if (levelsSpecified){
    breaks = xAxis$levels
  }
  
  # Otherwise, figure out where the ticks should go
  if (!levelsSpecified){
    values = unique(as.numeric(as.character(dataframe[,xAxis$var$varName])))
    
    if (length(values) > 5){
      minimum = round(min(values), digits=3)
      maximum = round(max(values), digits=3)
      mid = round((maximum-abs(minimum))/2, digits = 3)
      breaks = c(minimum, (mid-abs(minimum))/2, mid,mid+(maximum-mid)/2, maximum)
      for (i in 1:length(breaks))
        breaks[i] = values[which(abs(breaks[i]-values)==min(abs(values-breaks[i])))][1]
    }
    
    if (length(values) <= 5)
      breaks = unique(as.numeric(as.character(dataframe[,xAxis$var$varName])))
  }
  
  # Does the xAxis object contain labels that we have to place at the tick marks?
  if (!levelsSpecified){
    labelsSpecified = F
  }  else if (!'labels' %in% names(xAxis)){
    labelsSpecified = F
  } else {
    if (is.null(xAxis$labels)){
      labelsSpecified = F
    } else  if (length(xAxis$labels) != length(xAxis$levels)){
      warning("Specified x axis levels and labels. However, the number of labels and levels do not match.")
    } else {
      labelsSpecified = T
    }
  }
  
  # If no labels are specified (or are incorrectly specified), use print the value of the breaks at the breaks
  if (labelsSpecified){
    breakLabels = xAxis$labels
  } else {
    breakLabels = breaks
  }
  # Plot the axis
  p=ggplot(data=dataframe, aes(x = as.factor(dataframe[,xAxis$var$varName]))) +
    xlab("")+
    scale_x_discrete(breaks = breaks, labels = breakLabels)+
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
  
  # Do we need to show label on the axis? 
  if (showAxisLabel) {
    # First, check if the name contains <> characters. If it does, it is a mathPlot expression that needs to be evaluated.
    if (useLongName) nameToUse = xAxis$var$longName
    if (!useLongName) nameToUse = xAxis$var$shortName
    
    # First, check if the variable name contains "<>". If it does, the name contains an expression we have to evaluate 
    if (!grepl("<>", nameToUse)) {
      p = p + theme(axis.title.x = element_text(hjust=-0))+  xlab(nameToUse)
    } else {
      # if yes, time to create a to-be-evaluated string
      p = p + theme(axis.title.x = element_text(hjust=-0))+  xlab(parse(text=gsub("<>", "", nameToUse)))
    }  
  } else {
    p = p + theme(axis.title.x = element_text(hjust=-0))+  xlab("")
  }
  
  return (p)
  
}

# Returns a ggplot object containing only the y-axis itself.
getYAxisPlot = function(dataframe,yAxis, showAxisLabel, textSizeLabels, textSizeNumbers, useLongName)
{
  # Does the yAxis object contain some levels that we have to use?
  if (!'levels' %in% names(yAxis)){
    levelsSpecified = F
  } else {
    if (is.null(yAxis$levels)){
      levelsSpecified = F
    } else {
      levelsSpecified = T
    }
  }
  
  # If there are already specified levels, create tick marks on those levels
  if (levelsSpecified){
    breaks = yAxis$levels
  }
  
  # Otherwise, figure out where the ticks should go
  if (!levelsSpecified){
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
  }

  # Does the yAxis object contain labels that we have to place at the tick marks?
  if (!levelsSpecified){
    labelsSpecified = F
  }  else if (!'labels' %in% names(yAxis)){
    labelsSpecified = F
  } else {
    if (is.null(yAxis$labels)){
      labelsSpecified = F
    } else  if (length(yAxis$labels) != length(yAxis$levels)){
      warning("Specified y axis levels and labels. However, the number of labels and levels do not match.")
    } else {
      labelsSpecified = T
    }
  }
  
  # If no labels are specified (or are incorrectly specified), use print the value of the breaks at the breaks
  if (labelsSpecified){
    breakLabels = yAxis$labels
    labelAngle = 90
  } else {
    breakLabels = breaks
    labelAngle = 0
  }

  # Create a plot with the y axis
  p=ggplot(data=dataframe, aes(y = as.factor(dataframe[,yAxis$var$varName]))) +
    ylab("")+ 
    scale_y_discrete(breaks = breaks, labels = breakLabels)+
    theme(
      axis.text.x=element_blank(),
      axis.ticks.x=element_blank(),
      axis.title.x=element_blank(),
      axis.line.y = element_line(),
      panel.grid.minor.x=element_blank(),
      panel.grid.major.x=element_blank(),
      panel.background = element_blank(),
      
      axis.text             = element_text(size=textSizeNumbers, angle = labelAngle),
      axis.title            = element_text(size=textSizeLabels),
      axis.line             = element_line())
  
  # Do we need to show label on the axis? 
  if (showAxisLabel) {
    # First, check if the name contains <> characters. If it does, it is a mathPlot expression that needs to be evaluated.
    if (useLongName) nameToUse = yAxis$var$longName
    if (!useLongName) nameToUse = yAxis$var$shortName
    
    # First, check if the variable name contains "<>". If it does, the name contains an expression we have to evaluate 
    if (!grepl("<>", nameToUse)) {
      p = p + theme(axis.title.y = element_text(hjust=-0))+  ylab(nameToUse)
    } else {
      # if yes, time to create a to-be-evaluated string
      p = p + theme(axis.title.y = element_text(hjust=-0))+  ylab(parse(text=gsub("<>", "", nameToUse)))
    }  
  }
  
  return (p)
}


# Return the legend plot with correct spacing
# If a density is requested, the data frame (first argument) has to be the data from the heat plots
# Otherwise, the first argument has to be the data frame used by the main code
getLegend = function(dataToPlot, 
                     color, 
                     minColor, 
                     maxColor, 
                     colorFactorLevels,
                     colorFactorLabels,
                     noLegend=F, 
                     legendAsDensity = F, 
                     legendAreaWidth, 
                     legendAreaHeight, 
                     legendWidth, 
                     legendHeight, 
                     legendTitle,
                     textSizeLegendTitle, 
                     textSizeNumbers, 
                     colorPalette, 
                     contour, 
                     contourPalette, 
                     firstContourArgumentDenotesNumberOfGroups,
                     legendBreaks,
                     legendBreakLabels,
                     densityTransformationFunction){
  
  # If noLegend is true, just return an emptyPlot
  if (noLegend)
    return ( emptyPlot())
  
  # First, get the correct legend plot
  if (legendAsDensity){
    if (!is.null(contour))
      stop("Error: user requested a density legend plot and contour. However, in the current version it is not possible to do both.")
    legend = getLegendAsDensity(dataToPlot, color, minColor, maxColor, legendTitle, legendAreaWidth, textSizeLegendTitle, textSizeNumbers, colorPalette,legendBreaks,legendBreakLabels,densityTransformationFunction)
  }
  
  # If the legend should NOT be a density plot:
  if (!legendAsDensity){
    if (is.null(colorFactorLevels)){
      # If colorFactorLevels is NOT specified: return the default legend for continuous color values
      legend =getDefaultLegendContinuous(dataframe, 
                                         color, 
                                         colorPalette,
                                         minColor, 
                                         maxColor,                            
                                         contour,
                                         contourPalette,
                                         firstContourArgumentDenotesNumberOfGroups,
                                         legendAreaWidth,legendWidth, legendHeight, textSizeLegendTitle, legendBreaks,legendBreakLabels, textSizeNumbers)
    } else {
      legend = getDefaultLegendDiscrete(dataframe, 
                                        color, 
                                        colorPalette,
                                        colorFactorLevels,
                                        colorFactorLabels,
                                        contour,
                                        contourPalette,
                                        firstContourArgumentDenotesNumberOfGroups,
                                        legendAreaWidth,legendWidth, legendHeight, textSizeLegendTitle, legendBreaks,legendBreakLabels, textSizeNumbers)
    }
  }
    
  # Add the required white spaces
  layoutMatrix = matrix(seq(1,9,1), ncol =3, byrow=T)
  
  leg = grid.arrange(grobs = list(emptyPlot(),emptyPlot(),emptyPlot(),emptyPlot(), legend, emptyPlot(),emptyPlot(),emptyPlot(),emptyPlot()),
               layout = layoutMatrix, 
               heights = unit.c(
                 unit((legendAreaHeight - legendHeight)/2, "cm"),
                 unit(legendHeight, "cm"),
                 unit((legendAreaHeight - legendHeight)/2, "cm") ),
               widths = unit.c(
                 unit((legendAreaWidth - legendWidth)/2, "cm"),
                 unit(legendWidth, "cm"),
                 unit((legendAreaWidth - legendWidth)/2, "cm") )
  )
  
  return (leg)
}

# Return the default (i.e., standard) legend grob when the color shows continuous values
getDefaultLegendContinuous = function(dataframe, 
                            color, 
                            colorPalette,
                            minColor, 
                            maxColor,  
                            contour,
                            contourPalette,
                            firstContourArgumentDenotesNumberOfGroups,
                            legendAreaWidth,legendWidth, legendHeight, textSizeLegendTitle, legendBreaks,legendBreakLabels, textSizeNumbers){
  x = seq(0,1,0.1)
  y = seq(0,1,0.1)
  d = as.data.frame(expand.grid(x,y))
  colnames(d)= c("x", "y")
  d$col = rnorm(nrow(d), 0, 1)
  d$con = rnorm(nrow(d), 0, 1)
  
  if (is.null(legendTitle)){
    legtitle = splitString(color$var$shortName, unit(legendAreaWidth, units = "cm"))
  } else {
    legtitle = splitString(legtitle, unit(legendAreaWidth, units='cm'))
  }
  limitsCol = c(minColor-0.025*abs(minColor), maxColor+0.025*abs(maxColor))
  
  p = ggplot(data=d, aes(x=x, y = y, fill = col))+
    geom_tile()+
    theme(legend.position       = "right",
          legend.background = element_rect(colour = 'black', fill = NA, linetype='solid'),
          legend.title.align=0.5,
          legend.title          = element_text(size=textSizeLegendTitle),
          legend.text           = element_text(size=textSizeNumbers))
  
  #Are the breaks specified by the user? If so, force the legend to have breaks. Otherwise, let ggplot figure out where the ticks go
  if (is.null(legendBreaks)){
    p=p+ scale_fill_gradientn(legtitle,limits = limitsCol, colours = colorPalette)
  } else if (is.null(legendBreakLabels)){
    p=p+ scale_fill_gradientn(legtitle,limits = limitsCol, colours = colorPalette, breaks = legendBreaks)
  } else {
    p=p+ scale_fill_gradientn(legtitle,limits = limitsCol, colours = colorPalette, breaks = legendBreaks, labels = legendBreakLabels)
  }
  
  if (!is.null(contour)){
    if (firstContourArgumentDenotesNumberOfGroups){
      width = (max(agg$c) - min(agg$c))/(contour$levels[[1]]+1)
      breaksContour = round(seq(0, contour$levels[[1]]-1)*width + min(agg$c), digits=3)
      
      # if firstContourArgumentDenotesNumberOfGroups==F: draw a line at each specified level  
    } else {
      breaksContour = contour$levels
    }
    
    contTitle = splitString(contour$var$shortName, unit(legendAreaWidth, units = "cm"))
    limitsCont = c(min(breaksContour), max(breaksContour))
    p = p + geom_contour(aes(z=con, colour = ..level..),  breaks = contour$levels,   size = 0.75) +
      scale_color_gradientn(name = contTitle, colors=contourPalette, limits =limitsCont)+
      
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


# Return the default (i.e., standard) legend grob when the color shows discrete values
getDefaultLegendDiscrete = function(dataframe, 
                                    color, 
                                    colorPalette,
                                    colorFactorLevels,
                                    colorFactorLabels,
                                    contour,
                                    contourPalette,
                                    firstContourArgumentDenotesNumberOfGroups,
                                    legendAreaWidth,legendWidth, legendHeight, textSizeLegendTitle, legendBreaks,legendBreakLabels, textSizeNumbers){
  x = seq(0,1,0.1)
  y = seq(0,1,0.1)
  d = as.data.frame(expand.grid(x,y))
  colnames(d)= c("x", "y")
  d$col = as.factor( rep(colorFactorLevels, ceiling(nrow(d)/length(colorFactorLevels)))[1:nrow(d)] )
  d$con = rnorm(nrow(d), 0, 1)
  
  if (is.null(legendTitle)){
    legtitle = splitString(color$var$shortName, unit(legendAreaWidth, units = "cm"))
  } else {
    legtitle = splitString(legtitle, unit(legendAreaWidth, units='cm'))
  }
  
  if (is.null(colorFactorLabels))
    colorFactorLabels = colorFactorLevels
  
  p = ggplot(data=d, aes(x=x, y = y, fill = col))+
    geom_tile()+
    scale_fill_manual(legtitle, values = colorPalette, labels = colorFactorLabels)+
    theme(
          legend.background = element_rect(colour = 'black', fill = NA, linetype='solid'),
          legend.title.align=0.5,
          legend.title          = element_text(size=textSizeLegendTitle),
          legend.text           = element_text(size=textSizeNumbers))
  
  # Extract the legend for the color values
  tmp = ggplot_gtable(ggplot_build(p))
  legendColor = tmp$grobs[[which(sapply(tmp$grobs, function(x) x$name) ==  "guide-box")]]
  
  # If there is no contour: we are done, return the legend
  if (is.null(contour))
    return (legendColor)
    
  # Else, add the contour plot to the legend
  if (firstContourArgumentDenotesNumberOfGroups){
    width = (max(agg$c) - min(agg$c))/(contour$levels[[1]]+1)
    breaksContour = round(seq(0, contour$levels[[1]]-1)*width + min(agg$c), digits=3)
    
    # if firstContourArgumentDenotesNumberOfGroups==F: draw a line at each specified level  
  } else {
    breaksContour = contour$levels
  }
  
  # We cannot place contour lines over p, as p has discrete color values
  # We can however, create a plot for the contour plot and transplot the legend from p on q's color legend
  
  contTitle = splitString(contour$var$shortName, unit(legendAreaWidth, units = "cm"))
  limitsCont = c(min(breaksContour), max(breaksContour))
  q = ggplot(data=d, aes(x=x, y = y, fill = con))+
    geom_tile() + 
    geom_contour(aes(z=con, colour = ..level..),  breaks = contour$levels,   size = 0.75) +
    scale_color_gradientn(name = contTitle, colors=contourPalette, limits =limitsCont)+
    theme(
      legend.background = element_rect(colour = 'black', fill = NA, linetype='solid'),
      legend.title.align=0.5,
      legend.title          = element_text(size=textSizeLegendTitle),
      legend.text           = element_text(size=textSizeNumbers))+
    guides(color= guide_colorbar(barheight=unit(legendHeight/2, "cm"), barwidth = unit(legendWidth, "cm"), order = 0),
           fill = guide_colorbar(barheight=unit(legendHeight/2, "cm"), barwidth = unit(legendWidth, "cm"), order = 1))
  
  # Grab the legend for the color
  tmp = ggplot_gtable(ggplot_build(q))
  legendContour = tmp$grobs[[ which(sapply(tmp$grobs, function(x) x$name) ==  "guide-box") ]]
  legendContour$grobs[[1]] = legendColor
  
  return(legendContour)
}


getLegendAsDensity = function(dataToPlot, color, minColor, maxColor, legendTitle, legendAreaWidth, textSizeLegendTitle, textSizeNumbers, colorPalette,legendBreaks,legendBreakLabels, densityTransformationFunction){
  # Get all the z values from all data frames in heatPlotData
  colorValues = c()
  for (i in 1:length(dataToPlot))
    colorValues = c(colorValues, dataToPlot[[i]]$z)
  
  # In the heat plots themselves the color limits run from 
  # limitsCol = c(minColor-0.025*abs(minColor), maxColor+0.025*abs(maxColor))
  # Here we have to add groups with these (impossible) values too, otherwise the span of the color palettes are unequal
  limitsCol = c(minColor-0.025*abs(minColor), maxColor+0.025*abs(maxColor))

  # compute density scores
  density = density(colorValues, adjust = 1, from = limitsCol[1], to = limitsCol[2], kernel = 'cosine')
  plotData = data.frame(x=density$x, y = density$y)
  
  # If specified, apply the densityTransformationFunction
  if (!is.null(densityTransformationFunction))
    plotData$y = densityTransformationFunction(plotData$y)

  # Devide the full range of the bar (the bar, not the observed data!) in 100 equal groups
  groups = 100
  groupLength = (maxColor - minColor)/groups
  plotData$group  = floor( (plotData$x-minColor) / groupLength)
  plotData$group = (plotData$group < 1) * 1 + (plotData$group>=1)*plotData$group
  plotData$group = (plotData$group > groups) * 100 + (plotData$group<=100)*plotData$group
  
  segmentData = data.frame(matrix(NA, ncol=3, nrow=0))
  colnames(segmentData) = c("x", "y", "group")

  # The problem with geom_ribbon is that white bars appear in between segments.
  # These white bars appear because there are no points between two segments.
  # To fill them up, figure out for each segment what the coordinates in between that segment
  # and its neighboring segments are, and add those mid points.
  for (i in 1:(groups+1)){

    # Get all the coordinates for the current segment (if that segment is not empty)
    subsetCurrent=subset(plotData, group == i) 
    if (nrow(subsetCurrent)>1){
      
      # Add the point between the point that lies in between the current and previous (if applicable) segment
      if (i != 1){
        subsetPrevious = subset(plotData, group == (i-1))
        if (nrow(subsetPrevious)>0){
          coordPrevious = subset(subsetPrevious, x == max(subsetPrevious$x))
          coordCurrent = subset(subsetCurrent, x == min(subsetCurrent$x))
          coordInBetweenX = (coordPrevious$x+coordCurrent$x)/2
          coordInBetweenY = (coordPrevious$y+coordCurrent$y)/2
          subsetCurrent[nrow(subsetCurrent)+1,] = c(coordInBetweenX,0,i)
          subsetCurrent[nrow(subsetCurrent)+1,] = c(coordInBetweenX,coordInBetweenY,i)
        }
      } else {
        subsetCurrent[nrow(subsetCurrent)+1,] = c(limitsCol[1]-0.000001,0,i)
      }
      
      # Add the point between the point that lies in between the current and next (if applicable) segment
      if (i != (groups+1)){
        subsetNext = subset(plotData, group == (i+1))
        if (nrow(subsetNext)>0){
          coordNext = subset(subsetNext, x == min(subsetNext$x))
          coordCurrent = subset(subsetCurrent, x == max(subsetCurrent$x))
          coordInBetweenX = (coordNext$x+coordCurrent$x)/2
          coordInBetweenY = (coordNext$y+coordCurrent$y)/2
          subsetCurrent[nrow(subsetCurrent)+1,] = c(coordInBetweenX,coordInBetweenY,i)
          subsetCurrent[nrow(subsetCurrent)+1,] = c(coordInBetweenX,0,i)
          
        }
        
      } else {
        subsetCurrent[nrow(subsetCurrent)+1,] = c(max(subsetCurrent$x),0,i)
      }
      
      #Order the data
      subsetCurrent = subsetCurrent[order(subsetCurrent$x), ]
      
      # Add all the normal points
      segmentData=rbind(segmentData, subsetCurrent)
    }

    
  }
 
  # get max density
  maxDensity = max(segmentData$y)
  
  # Make sure all the values in limitCol are in the plot
  segmentData[nrow(segmentData)+1,] = c(minColor-0.025*abs(minColor),0,0)
  
  if (nrow(subset(segmentData, group == (groups+1)))==0)
    segmentData[nrow(segmentData)+1,] = c(maxColor+0.025*abs(maxColor),0,groups+2)
  
  
  
  # if legendBreaks is NULL, determine where the breaks in the legend should be
  if (is.null(legendBreaks)){
    midBreak = (minColor+maxColor)/2
    breaks = c(minColor, (minColor+midBreak)/2, midBreak, (midBreak+maxColor)/2, maxColor)
  }else {
    breaks = legendBreaks
  }
  
  # Determine the break labels
  if (!is.null(legendBreakLabels)){
    breakLabels = legendBreakLabels
  } else {
    breakLabels = breaks
  }
  
  
  # Make a color key data frame to plot the left-hand side color spectrum
  colorKey = data.frame(matrix(NA, ncol=3, nrow=0))
  colnames(colorKey) = c("x", "y", "group")
  yStart = -maxDensity/10 
  yStop  = yStart -(maxDensity/10)
  for (i in 1:(groups+1)){
    colorKey[nrow(colorKey)+1,] = c(minColor+(groupLength*(i-1)),yStop,i)
    colorKey[nrow(colorKey)+1,] = c(minColor+(groupLength*(i-1)),yStart,i)
    colorKey[nrow(colorKey)+1,] = c(minColor+(groupLength*(i)),yStart,i)
    colorKey[nrow(colorKey)+1,] = c(minColor+(groupLength*(i)),yStop,i)
  }
  
  # Determine the title
  if (is.null(legendTitle)){
    legendTitle = splitString(color$var$shortName, unit(legendAreaWidth, units = "cm"))
  } else {
    legendTitle = splitString(legendTitle, unit(legendAreaWidth, units='cm'))
  }
  
  p=ggplot(segmentData, aes(x=x,y=y)) + 
    ggtitle(legendTitle)+
    
    geom_polygon(data = colorKey, aes(fill = group, group = group, x=x, y =y  ))+
    
    geom_segment(x=breaks[1], y=yStop, xend=breaks[1], yend = maxDensity, size = 0.7, linetype = "solid", color = "grey70")+
    geom_segment(x=breaks[2], y=yStop, xend=breaks[2], yend = maxDensity, size = 0.7, linetype = "solid", color = "grey70")+
    geom_segment(x=breaks[3], y=yStop, xend=breaks[3], yend = maxDensity, size = 0.7, linetype = "solid", color = "grey70")+
    geom_segment(x=breaks[4], y=yStop, xend=breaks[4], yend = maxDensity, size = 0.7, linetype = "solid", color = "grey70")+
    geom_segment(x=breaks[5], y=yStop, xend=breaks[5], yend = maxDensity, size = 0.7, linetype = "solid", color = "grey70")+
    
    geom_line(data=plotData, aes(x=x,y=y),size=0.7, alpha =0.8, color = "grey50")+
    geom_polygon(aes(fill = group, group = group))+
 
    geom_label(data = data.frame(x = breaks[1], y = maxDensity*0.7, label = format(round(breaks[1], digits = 2), nsmall=2)), aes(x = x, y = y, label = label), size=textSizeNumbers*(5/14), fill = "grey90", alpha = 0.5)+
    geom_label(data = data.frame(x = breaks[2], y = maxDensity*0.7, label = format(round(breaks[2], digits = 2), nsmall=2)), aes(x = x, y = y, label = label), size=textSizeNumbers*(5/14), fill = "grey90", alpha = 0.5)+
    geom_label(data = data.frame(x = breaks[3], y = maxDensity*0.7, label = format(round(breaks[3], digits = 2), nsmall=2)), aes(x = x, y = y, label = label), size=textSizeNumbers*(5/14), fill = "grey90", alpha = 0.5)+
    geom_label(data = data.frame(x = breaks[4], y = maxDensity*0.7, label = format(round(breaks[4], digits = 2), nsmall=2)), aes(x = x, y = y, label = label), size=textSizeNumbers*(5/14), fill = "grey90", alpha = 0.5)+
    geom_label(data = data.frame(x = breaks[5], y = maxDensity*0.7, label = format(round(breaks[5], digits = 2), nsmall=2)), aes(x = x, y = y, label = label), size=textSizeNumbers*(5/14), fill = "grey90", alpha = 0.5)+
    
    scale_fill_gradientn(colours = colorPalette)+
    coord_flip()+
    scale_x_continuous(limits = c(limitsCol[1]-0.0001, limitsCol[2]+0.0001 ), position = "top", breaks= breaks)+
    scale_y_continuous(position = "left")+
    theme(
      plot.background = element_rect(fill = NA, colour = "black"), 
      panel.grid.major = element_blank(),
      panel.grid.minor = element_blank(), 
      panel.border = element_blank(),
      panel.background = element_blank(),
      axis.title.x = element_blank(),
      axis.title.y = element_blank(),
 
      axis.ticks.y = element_blank(),
      axis.line.y = element_blank(),
      axis.text = element_blank(),
      axis.ticks.x = element_blank(),
      plot.title = element_text(hjust = 0.5, size=textSizeLegendTitle),
      legend.position = "none")

  return (p)
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
missingPlot = function(missingColor){
  return (
    ggplot()+geom_blank(aes(1,1))+
      theme(
        panel.background = element_rect(fill = missingColor, colour = "black"),
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



#########################################################################
#///////////////////////////////////////////////////////////////////////#
#////     Supporing function: other functions     //////////////////////#
#///////////////////////////////////////////////////////////////////////#
#########################################################################

# This function takes a raw string and transforms it into a formatted, multi-lined expression that
# can be parsed by a textGrob. It is possible to include Greek sign's and other commands from 
# plotMath. To include a command from plotMath, place the command between two '<>'s. For instance,
# to create a (mu_substript), use the command <>mu['subscript']<>. Make sure that there is no space 
# within the expression. This function only supports mono fontfamilies (otherwise the justification does
# not work). 
parseDescription= function(text, widthOfGrob, textSize, fontfamily = 'mono'){
  # Make sure that there is always a space between <>'s
  newlineString = gsub("<><>", "<> <>", text)
  
  # Get a new string that has all the required new line characters
  newlineString = splitString(text, widthOfGrob)
  
  # Break on the newline characters, creating a string for each line
  brokenStrings = strsplit(newlineString, "\n")
  maxCharPerBrokenString = 0
  for (s in brokenStrings)
    maxCharPerBrokenString = max(maxCharPerBrokenString, nchar(s))
  
  # For each line, create a subExpression in txt format that can be used to create the expressions
  exprs = list()
  for (i in 1:length(brokenStrings[[1]])){
    # Split on <>'s
    splittedSub = strsplit(brokenStrings[[1]][[i]], "<>")
    charactersInSplit = 0
    
    # Check how many splits we have - if there is only 1, we do not have to parse anything
    if (length(splittedSub[[1]]) == 1)
      subExpression = paste("textstyle(paste(", splittedSub[[1]][[1]], "'", ")) ", sep = "" )
    
    if (length(splittedSub[[1]]) >= 1){
      # If there are diamonds present, we need to split and parse expressions
      # Every even index is now a subExpression that we have to evaluate
      subExpression = "textstyle(paste("
      
      for (j in 1:length(splittedSub[[1]])){
        if ((j%%2)==1){
          subExpression = paste(subExpression, "'", splittedSub[[1]][[j]], "', ", sep = "" )
          charactersInSplit = charactersInSplit+nchar(splittedSub[[1]][[j]])
          
        }
        
        if ((j%%2)==0){ # This is where the parsing will take place
          subExpression = paste(subExpression, splittedSub[[1]][[j]], ", ", sep = "" )
          #Compute how many extra characters we roughly need to add in whitespace because of the formatting. 
          inchesForParsed = widthDetails(
            textGrob(parse(text=splittedSub[[1]][[j]]),  gp=gpar(fontfamily = fontfamily, fontsize = textSize))
          )
          inchesForRaw = widthDetails(
            textGrob(splittedSub[[1]][[j]],  gp=gpar(fontfamily = fontfamily, fontsize = 200))
          )
          dInch = as.numeric(inchesForRaw) - as.numeric(inchesForParsed)
          charactersInSplit = charactersInSplit+nchar(splittedSub[[1]][[j]]) - (dInch*14) #widthDetails does not care about the fontsize. If I assume it is 14 it seems to work sort of OK.
          
        }
      }
    }
    
    
    # Compute the extra number of white spaces we need to add to make up for the parsing
    extraChars = round(maxCharPerBrokenString - charactersInSplit)
    extraSpace = strrep(" ",extraChars)
    subExpression = paste(subExpression, "'", extraSpace, "',", " sep=''))", sep = '')
    exprs[[i]] = subExpression
  }
  
  # Combine all sub expression using nested  atop() commands (almost there!)
  expression = ""
  if (length(exprs) > 2){
    nestAtop = function(new, previous) { return ( paste("atop(", new, ",", previous, ")", sep = "")) }
    expression = nestAtop(exprs[[length(exprs)-1]], exprs[[length(exprs)]])
    for (i in rev(1:(length(exprs)-2)))
      expression = nestAtop(exprs[[i]], expression)
    
    # atop gives the first line a little more vertical space. So we have to make the first line blank
    expression = nestAtop("''", expression)
  }
  
  return (expression)
  
}

# Transform a text by adding a newline character after every 'width'
splitString <- function (text, widthOfGrob) {
  strings <- strsplit(text, " ")[[1]]
  newstring <- strings[1]
  linewidth <- stringWidth(newstring)
  gapwidth <- stringWidth(" ")
  availwidth <- convertWidth(widthOfGrob, "in", valueOnly = TRUE)
  if (length(strings) != 1){
    for (i in 2:length(strings)) {
      if (grepl("<>", strings[i])){
        adjustedString = gsub("<>", "", strings[i])
        adjustedString = gsub("\\['", "", adjustedString)
        adjustedString = gsub("'\\]", "", adjustedString)
        widthOfGrob <- stringWidth(adjustedString)*0.9
      }    else {
        widthOfGrob <- stringWidth(strings[i])
      }
      
      if (convertWidth(linewidth + gapwidth + widthOfGrob, "in", 
                       valueOnly = TRUE) < availwidth) {
        sep <- " "
        linewidth <- linewidth + gapwidth + widthOfGrob
      }    else {
        sep <- "\n"
        linewidth <- widthOfGrob
      }
      newstring <- paste(newstring, strings[i], sep = sep)
    }
  }
  newstring
}

