
# Function to call to create a heat plot/ heat map from a file directly (i.e., the function will read the specified file first) ----
createHeatPlotFromFile= function(
  inputFile, 
  outputFile, 
  dimensions, 
  maxColor, 
  useColor=T, 
  width=21.0, 
  height=29.7, 
  DPI=600, 
  title = "", 
  save = T, 
  returnPlot = T, 
  includeLegend = T, 
  firstContourArgumentDenotesNumberOfGroups=F)
{
  df=read.csv(inputFile, sep = ";", header = TRUE)
  createHeatPlot(
    df, 
    outputFile, 
    dimensions, 
    maxColor, 
    useColor, 
    width, 
    height, 
    DPI, 
    title, 
    save, 
    returnPlot, 
    includeLegend, 
    firstContourArgumentDenotesNumberOfGroups
  )
}  

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
  title = "",                                           # Optional: The title of the plot, shown in the top level. Default is absent.
  
  maxColor=NULL,                                        # Optional: The maximum value that the color variable can take. All values higher in this variable are set to maxColor. Default is to not winsorize. 
  minColor=NULL,                                        # Optional: The minimum value that the color variable can take. All values lower in this variable are set to minColor. Default is to not winsorize. 
  colorPalette = viridis(12),                           # Optional: A list of colors specifying the legend of the color variable. Default is the viridis color palette.
  missingColor = "pink",                                # Optional: The color used for missing values. Default is "burlywood1".
  
  firstContourArgumentDenotesNumberOfGroups=F,          # Optional: If there is a contour variable, does the levels parameter specify how many lines should be drawn (F) or at values the lines should be drawn (T)? Default is FALSE.
  transformation = NULL,                                # Optional: A transformation that specifies the value-to-color mapping. Currently not implemented.
  
  width=21.0,                                           # Optional: Width in centimeter of the total plot. This includes a margin of 2.5 cm on both sides. Default is 21.0 (A4 format).
  height=29.7,                                          # Optional: Height in centimeter of the total plot. This includes a margin of 2.5 cm on all sides. Default is 29.7 (A4 format).
  DPI=600,                                              # Optional: DPI of resulting plot. Default is 600.
  includeLegend = T,                                    # Optional: Should the plot include the legend for colors? Default is true.
  numberedLabels = T,                                   # Optional: Include values for each column (1-n) and rows (A-Z). Default is true. 
  
  save = T,                                             # Optional: Save plot to file? Default is true.
  returnPlot = F                                        # Optional: return the resulting plot after this function call is completed? Default is false.
){
  
  
  # Provide some feedback:
  if (save)
    cat(paste("\nPlotting heatplot with filename: \"", outputFile, "...",  sep = "" ))
  if (!save)
    cat(paste("\nPlotting heatplot with title: \"", title, "...",  sep = "" ))
  
  # Constants that determine the size, height and width of various components in the plot 
  # These values can easily be tweaked manually
  
  W               = width - 2.5      # width of whole plot
  H               = height - 2.5    # height of whole plot
  LH              = H * 0.6     # legend height
  LW              = 0.5         # legend width
  
  LG              = 1.00        # Size of Legend grob
  divL            = 1.00        # width between heat plots and legend
  divA            = 0.50        # width between Y/X axis text and row/column label
  RCLevel         = 0.75        # Size of row/col labels grobs (individual levels, eg., SD = 1)
  RCLabel         = 0.50        # Size of row/col labels grobs (individual levels, eg., SD = 1)
  XYA             = 1.3         # Size of x/y axis grobs
  
  textSizeTitle             = 14          # Size of the title (if applicable)
  textSizeHeaders           = 12          # Size of the column and row labels (i.e., names)
  textSizeRowColumnLevels   = 12          # Size of the individual levels of the columns and rows
  textSizeLabels            = 11          # Size of all the text (axes) in the plot
  textSizeNumbers           = 10          # Size of all the numbers (axes, legend) in the plot
  
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
  if (!is.null(maxColor)){ 
    df[,color$var$varName] =  (1-(df[,color$var$varName]  > maxColor))*df[,color$var$varName] + ((df[,color$var$varName] > maxColor)*maxColor)
  } else {
    maxColor = max(df[,color$var$varName]) }
  
  if (!is.null(minColor)){ 
    df[,color$var$varName] =  (1-(df[,color$var$varName]  < minColor))*df[,color$var$varName] + ((df[,color$var$varName] < minColor)*minColor)
  } else {
    minColor = min(df[,color$var$varName]) }
  
  
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
  
  
  
  
  
  ##########################################################################
  ######################  IF 1 ROW, 1 COLUMN: MAKE 1 PLOT  #################
  ##########################################################################
  # After this section we will create multiple heat plots, legends and axes and figure out how to 
  # combine all of this into a single plot. However, if there is only a single heat plot to make,
  # we don't have to go through this hassle - we can just make a single plot
  
  if (is.null(rows) && is.null(columns)){
    
    p1 = createSinglePane(dataframe=df, 
                          xAxis=xAxis, 
                          yAxis=yAxis, 
                          color=color, 
                          textSizeLabels=textSizeLabels,
                          textSizeNumbers=textSizeNumbers,
                          LW=LW,
                          LH=LH,
                          minColor=minColor,
                          maxColor = maxColor, 
                          colorPalette = colorPalette,
                          missingColor = missingColor,
                          contour=contour,
                          onlyPlot=T,
                          firstContourArgumentDenotesNumberOfGroups=firstContourArgumentDenotesNumberOfGroups ,
                          includeLegend=includeLegend)
    top=textGrob(title, gp=gpar(fontface="bold", fontsize=textSizeTitle ))
    p = grid.arrange(p1, layout_matrix=matrix(c(1)), widths=unit(W-2.5, "cm"), heights=unit(H-2.5, "cm"), top = top)
  }
  
  
  
  
  
  ##################################################################
  ######################  Create heat plot images  #################
  ################################################################## TODO: clean code below
  # Create all the heat plots. These are the 'images' without legends, axes or titles 
  heatPlots = list()
  
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
        rowPlots[[length(rowPlots)+1]] = missingPlot() 
      } else {
        
        rowPlots[[length(rowPlots)+1]] = createSinglePane(
          dataframe=subsetPlot, 
          xAxis=xAxis, 
          yAxis=yAxis, 
          color=color, 
          textSizeLabels=textSizeLabels,
          textSizeNumbers=textSizeNumbers,
          LW=LW,
          LH=LH,
          minColor = minColor,
          maxColor = maxColor, 
          colorPalette = colorPalette,
          missingColor = missingColor,
          contour=contour,
          firstContourArgumentDenotesNumberOfGroups = firstContourArgumentDenotesNumberOfGroups,
          includeLegend=includeLegend
        )
        
      }
    }   
    heatPlots[[length(heatPlots)+1]] = rowPlots
  }
  
  
  
  
  
  ###########################################################
  ######################  Create other plots ################
  ###########################################################
  
  # Create an empty plot, containing only the x axis (with and without labels)
  xAxisPlotLabel = getXAxisPlot(dataframe=df, xAxis=xAxis, showAxisLabel=T, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers)
  xAxisPlot = getXAxisPlot(df, xAxis, F, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers)
  
  # Create an empty plot, containing only the y axis  (with and without labels)
  yAxisPlotLabel = getYAxisPlot(dataframe=df, yAxis=yAxis, showAxisLabel=T, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers)
  yAxisPlot = getYAxisPlot(df, yAxis, F, textSizeLabels =  textSizeLabels, textSizeNumbers = textSizeNumbers)
  
  # create a completely empty plot, used for spacing
  nullPlot = emptyPlot()
  
  # Create the legend
  legendPlot = getLegend(df, color, minColor, maxColor, textSizeNumbers=textSizeNumbers, textSizeLabels = textSizeLabels, LH, LW, colorPalette, contour = contour)
  
  # Create, if applicable, the text grobs for the row and column labels
  if (!is.null(rows)){
    rowTextLabel  = textGrob(rows$var$longName, rot = 90, gp = gpar(fontsize = textSizeHeaders))
    
    labeling = c()
    for (i in 1:length(rows$levels)){
      labeling[i] = ""
      if (numberedLabels) labeling[i] = paste("(",LETTERS[i], ") ", sep = "")
    }
    rowTextLevels = list()
    if (typeof(rows$levels) == "double")
      for (r in 1: length(rows$levels)) 
        rowTextLevels[[length(rowTextLevels)+1]] = textGrob(paste(labeling[r], rows$var$shortName,"=", rows$levels[r], sep = " "), rot = 90, gp = gpar(fontsize = textSizeRowColumnLevels, fontface = 'italic'))
    if (typeof(rows$levels) == "list")
      for (r in 1: length(rows$levels)) 
        rowTextLevels[[length(rowTextLevels)+1]] = textGrob(paste(labeling[r], rows$var$shortName," in ", rows$levels[[r]][[1]], sep = " "), rot = 90, gp = gpar(fontsize = textSizeRowColumnLevels, fontface = "italic"))
  }
  
  if (!is.null(columns)){
    columnTextLabel  = textGrob(columns$var$longName, gp = gpar(fontsize = textSizeHeaders))
    
    labeling = c()
    for (i in 1:length(columns$levels)){
      labeling[i] = ""
      if (numberedLabels) labeling[i] = paste("(",i, ") ", sep = "")
    }
    columnTextLevels = list()
    if (typeof(columns$levels) == "double")
      for (c in 1: length(columns$levels)) 
        columnTextLevels[[length(columnTextLevels)+1]] = textGrob(paste(labeling[c],columns$var$shortName,"=", columns$levels[c], sep = " "), gp = gpar(fontsize = textSizeRowColumnLevels, fontface = "italic"))
    if (typeof(columns$levels) == "list")
      for (c in 1: length(columns$levels)) 
        columnTextLevels[[length(columnTextLevels)+1]] = textGrob(paste(labeling[c],columns$var$shortName,"=", columns$levels[[c]][[1]], sep = " "), gp = gpar(fontsize = textSizeRowColumnLevels, fontface = "italic"))
    
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
  #   width=RCLabel       width=RCLevel     width=divA    width=XYA width=height_HP         width=divL    width=A
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
    width_HP       = W - LG - divL - RCLevel - RCLabel - XYA - divA  # height of heat plots
    height_HP        = (H-XYA)/length(rows$levels)
    
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
    widths = c(RCLabel, RCLevel, divA, XYA, width_HP, divL, LG)
    heights = c()
    for (r in 1:length(rows$levels))
      heights = c(heights, height_HP)
    heights = c(heights, XYA)
    # Make the total plot
    top=textGrob(title, gp=gpar(fontface="bold", fontsize=textSizeTitle ))
    p = grid.arrange(grobs=grobs, layout_matrix=layout, widths=unit(widths, "cm"), heights=unit(heights, "cm"),top=top)
  }
  
  
  # Case 2: columns, but not rows, have been specified
  # In a matrix, this plot has the following structure
  #   x = (W-XYA-DIVA-LG)/ncol
  
  #   w=XYA      w=x                w=x               ...    w=x                divA        LG           
  # ___________________________________________________________________________________________
  # [          | COLUMN LABEL      | COLUMN LABEL   | ...  | COLUMN LABEL     |      | legend   ]  height = RCLabel
  # [          | Col = level 1     | Col = level 2  | ...  | Col = level n    |      | legend   ]  height = RCLevel
  # [          |                   |                | ...  |                  |      | legend   ]  height = divA
  # [ Y axis   | heatPlots[[1]]    | heatPlots[[2]] | ...  | heatPlots[[n]]   |      | legend   ]  height = height_HP
  # [          | x axis label      | x axis         | ...  | x axis           |      |          ]  height = XYA
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
    height_HP       = H - RCLabel - RCLevel - divA - XYA
    width_HP        = (W-XYA-divA-LG)/length(columns$levels)
    
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
    heights = c(RCLabel, RCLevel, divA, height_HP, XYA)
    widths = c(XYA)
    for (c in 1:length(columns$levels)) widths = c(widths, width_HP)
    widths = c(widths, divL, LG)
    
    # Make the total plot
    top=textGrob(title, gp=gpar(fontface="bold", fontsize=textSizeTitle ))
    p = grid.arrange(grobs=grobs, layout_matrix=layout, widths=unit(widths, "cm"), heights=unit(heights, "cm"), top=top)
    
  }
  
  
  
  # Case 3: columns and rows have been specified
  # In a matrix, this plot has the following structure
  #   x = (W-XYA-DIVA-LG)/ncol
  # ____________________________________________________________________________________
  # [         |     |   |        | col lbl | col lbl | col lbl | col lbl  |   | legend ] 
  # [         |     |   |        | c=1     | c=2     | c=...   | c=n      |   | legend ] 
  # [         |     |   |        |         |         |         |          |   | legend ]
  # [ row lbl | r=1 |   | yaxis  | H[1,1]  | H[1,2]  | H[1,.]  | H[1,n]   |   | legend ] 
  # [ row lbl | r=2 |   | yaxis  | H[2,1]  | H[2,2]  | H[2,.]  | H[2,n]   |   | legend ] 
  # [ row lbl | r=. |   | yaxis  | H[.,1]  | H[.,2]  | H[.,.]  | H[.,n]   |   | legend ] 
  # [ row lbl | r=n |   | yaxis  | H[n,1]  | H[n,2]  | H[n,.]  | H[n,n]   |   | legend ] 
  # [         |     |   |        | xaxis   | xaxis   | xaxis   | xaxis    |   | legend ] 
  #
  # Which have (universally) the following indices
  #
  #
  #  Row lbl        row lvl          sp         yaxis           
  # _____________________________________________________________________________________________________________________________________
  # [         |                     |   |                           | 1               | 1               | 1    | 1               |   | 3 ]    (Column label header)
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
    layout = cbind(layout, rep( 3, nrowsInPlot))
    
    # Fill in all the NA's with numbers and corresponding nullplots
    for (r in 1:nrow(layout)) for (c in 1:ncol(layout)) if (is.na(layout[r,c])) layout[r,c] = max(layout, na.rm=T)+1
    while (length(grobs) != max(layout))
      grobs[[length(grobs)+1]]    =   nullPlot
    
    # Determine the widths of the plot
    heatPlotWidth = (W-RCLabel- RCLevel-divA-XYA-divL-LG)/ncol
    widths = c(RCLabel, RCLevel, divA, XYA)
    for (c in 1:ncol) widths = c(widths, heatPlotWidth)
    widths = c(widths, divL, LG)
    
    # Determine the heights of the plot
    heatPlotHeight = (H-RCLabel - RCLevel - divA - XYA)/nrow
    heights = c(RCLabel, RCLevel, divA)
    for (r in 1: nrow)
      heights = c(heights, heatPlotHeight)
    heights = c(heights, XYA)
    
    top=textGrob(title, gp=gpar(fontface="bold", fontsize=textSizeTitle ))
    p = grid.arrange(grobs=grobs, layout_matrix=layout, widths=unit(widths, "cm"), heights=unit(heights, "cm"), top = top)
  }
  
  
  # Save the plot to the specified file
  if (save)
    ggsave(plot=p, filename = outputFile, width = W+2.5, height = H+2.5, units = "cm", dpi = DPI)
  
  # Provide some feedback:
  cat(" Done." )
  
  if (returnPlot)
    return (p)
}







##############################################################################
#////////////////////////////////////////////////////////////////////////////#
#////     Supporing function: create a single panel     /////////////////////#
#////////////////////////////////////////////////////////////////////////////#
##############################################################################
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
      scale_y_continuous(breaks = c(minimumY, (minimumY+ maximumY)/2,maximumY), expand = c(0.001,0.001))+
      scale_x_continuous(breaks = c(minimumX, (minimumX+ maximumX)/2,maximumX), expand = c(0.001,0.001))+
      theme(panel.grid = element_blank(),
            panel.background = element_blank()
      ) 
    
    # If there is only one heat plot
    if (onlyPlot){
      p = p + ylab(yAxis$var$longName)+
        xlab(xAxis$var$longName)+
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
# ----


###########################################################################################################
#/////////////////////////////////////////////////////////////////////////////////////////////////////////#
#////     Supporing function: create all the constant plots (axes, legend, etc)     //////////////////////#
#/////////////////////////////////////////////////////////////////////////////////////////////////////////#
###########################################################################################################
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

# ----


####################################################
#//////////////////////////////////////////////////#
#////     Wrapper function: heatPlot     //////////#
#//////////////////////////////////////////////////#
####################################################
# Finally, a wrapper function for createHeatPlot that takes as input the data frame and title, followed by all other parameters to be passed to createHeatPlot
# and automatically creates (based on activeDirectory) the output file names etc.
# This function also replaces all "."s to ","s in the file name (but not in the title).
# Before making a new plot, the function checks if a plot with a similar name already exists. If so, execution stops and a warning is provided.
heatplot = function(dataframe, title, ...){
  outputFile = getOutputFile(title)
  
  if (file.exists(outputFile)){
    message(paste("Warning: File with name \"", outputFile , "\" already exists. No new plot will be created.", sep = ""))
    return ()
  }
  
  return (
    createHeatPlot(dataframe=dataframe, outputFile = outputFile, title = title, ...)
  )
  
}

# ----
