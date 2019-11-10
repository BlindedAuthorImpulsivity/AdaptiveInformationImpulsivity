# Given an array of resource values, the mean of the cue emission probability, and the cue reliability (sd),
# returns the probability of receiving a cue of this value for each possible resource value. These probabilities
# are normalized to sum to 1.
# If the standard deviation is 0, a probability distribution is returned that has a probability of 0 for all values, except for the
# value that is closest to the mean. This value has a probability of 1.
getNormalDistributionProbabilities = function(resourceValues, mean, sd)
{
  if (sd == 0){
    p=rep(0, length(resourceValues))
    p[which(abs(mean-resourceValues)==min(abs(mean-resourceValues)))]=1
    return (p/sum(p))
  }
  
  options(scipen=999)
  unnormalizedProbabilities = exp((-(resourceValues-mean)^2)/(2*sd^2)) 
  probabilities = unnormalizedProbabilities/sum(unnormalizedProbabilities)
  return ( probabilities)
  
}
