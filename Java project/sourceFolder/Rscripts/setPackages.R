requireAndInstall = function(package){
    if (!require(package, character.only = T))
        install.packages(package, repos = "http://cran.us.r-project.org")  
    require(package, character.only = T)
}
requireAndInstall("scales")
requireAndInstall("ggplot2")
requireAndInstall("RColorBrewer")
requireAndInstall("gridExtra")
requireAndInstall("grid")
requireAndInstall("directlabels")
requireAndInstall("viridis")
requireAndInstall("data.table")
