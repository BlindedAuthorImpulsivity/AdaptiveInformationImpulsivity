package agent.ValueIterator;

import java.io.IOException;
import java.util.ArrayList;

import agent.Agent;
import agent.ValueFunction;
import decimalNumber.DecimalNumber;
import decimalNumber.DecimalNumberArray;
import decimalNumber.DecimalNumberMatrix;
import environment.AbstractEnvironmentBuilder;
import environment.EnvironmentBuilderLazy;
import estimationParameters.EstimationParameters;
import externalPrograms.RserveManager;
import helper.Helper;
import helper.Helper.MisspecifiedException;
import markovDecisionProcess.MarkovDecisionProcess;
import start.Model;
import staticManagers.ObserverManager;

//TODO: add discounting, update the stuff that has changed w.r.t. saving
/** <pre>
 * A value iteration agent, (loosely) based on Sutton and Barto's REINFORCEMENT LEARNING (2015 book, chapter 4.4).
 * Below I give a condensed, and hopefully easy to read, explanation. This explanation is however a simplification -
 * I do not explain all aspects of the decision problem and avoid any tedious-to-explain boundary conditions.
 *
 * The value iterator algorithm finds an answer to the following question: 
 * 
 * 		"GIVEN (a) a known environmental distribution of resource values and extrinsic events (priors), 
 * 		(b) a known Markov Decision Process (MDP), (c) a starting budget, 
 * 		and (d) a resource encounter where the resource has an unknown value,
 * 
 * 		how many cues should an agent sample during this resource encounter before deciding to either eat or 
 * 		discard the resource? Specifically, how many cues should the agent sample to maximize the end-of-the lifetime
 * 		(terminal) fitness?"
 * 
 * Before I can describe how the value iterator solves this problem, I need to flesh out the problem a bit more. For shorthand, 
 * let's call a resource encounter and extrinsic event pair a cycle. Suppose the agent goes through a grand total of n cycles in 
 * its lifetime (*note: if we run until convergence, this is not a fixed number). After the n'th cycle the agent
 * gets a fitness pay-out based on its budget. Schematically:
 * 
 *  	starting budget -> Cycle 1 -> Cycle 2 -> ... -> Cycle n -> Terminal fitness outcome
 *  
 *  What we want to know is how many cues the agent should sample prior to making a decision to eat or discard the resource
 *  in the first cycle, taking into account how the outcome of this decision (cost of sampling, probability distribution over
 *  resource values and subsequent starting budgets in next encounters) in the first cycle effects the outcome of cycles 2 to n 
 *  (assuming the agent will act rational in those cycles), and how this will ultimately affect the terminal fitness outcome.
 *  
 *  
 *  1. DETERMINING WHAT AMOUNT OF CUE SAMPLING ACHIEVES THE HIGHEST FITNESS
 *  
 *  1. 1. Constructing the decision tree: the forward pass
 *  So what does the decision in the first cycle look like? Let's consider the relatively simple decision where the agent
 *  has a starting budget of 4, can sample a maximum of 2 cues, and cues cost 1 unit each. Each cue can have one of two labels (cue values),
 *  indicating that the resource has a positive (+) or negative (-) value. In this first cycle the agent first encounters a resource. 
 *  During this encounter the agent can repeatedly decide to sample one additional cue. If the agent samples, its budget is set back with 
 *  1 (sampling is costly) and it receives a cue. Receiving a cue changes the agents posterior belief in the resource value. Accepting
 *  and discarding ends the resource encounter. Let's first consider all possible states the agent can be in during an encounter. That
 *  is, what states the agent can be in after sampling. We can represent this as a tree:
 *  
 *  							     	   |---(D, 3, 2+, 1-)
 *  		         |---(B, 4, 1+, 0-)----|
 *  		 	     |				       |
 *  (A, 5, 0+, 0-)---|				 	   |---(E, 3, 1+, 1-)
 *  			  	 |				 	   |
 *  			  	 |---(C, 4, 0+, 1-)----|
 *  							 	       |---(F, 3, 1+, 2-)
 *  
 *  Where (a, x,y+,z-) is a node (state) with name a where the agent has a budget x, received y times a positive cues, and z times a negative cue.
 *  The agent traverses the tree from the left hand side (root, node A) to the right hand side. In each node the agent has to decide to eat or
 *  discard the resource (thereby ending the encounter) or sampling one more cue (thereby moving one step to the right). At the 
 *  ultimate level (right hand side), no sampling can be done - the maximum number of 2 cues has already been reached. Building
 *  this tree is called the forward pass of a tree. Note, however, that the probability of the next cue having a positive and
 *  negative label is different in each node - this probability distribution depends on the agents belief about what the value of
 *  the resource value is. For instance, if the agent has 1 positive and 0 negative cues, the prior distribution is uniform, and the
 *  cue emission matrix specified in the MDP is symmetric (i.e., p(positive cue| resource value > 0) equals p(negative cue| resource value
 *  < 0), it is more likely that the next cue will have a positive label.
 *  
 *  
 *  1.2. Finding the best action per node: the backward pass
 *  After the forward pass, the agent starts at the right hand side of the tree, and works backwards towards the root (sequence F,E,D,B,C,A). At each node
 *  it first computes the expected FITNESS outcome of each action (accepting, discarding, or if applicable, sampling), selects the action with the
 *  highest expected FITNESS, and sets the nodes expected outcome to the expected Fitness of this action. It might be that two or more actions result
 *  in the same expected fitness. In this case, the agent will select 'randomly' between each highest action. The fitness outcome is the 
 *  terminal fitness outcome - which is a transformation over the agent's budget at the last cycle. This is something different than the VALUE
 *  outcome - which is the expected value outcome of a cycle (i.e., change in budget). The expected fitness of an action is computed differently
 *  per action.
 *  
 *  The expected fitness value of sampling is the average of the expected fitness of its child nodes, that is, the nodes in which the agent might come after 
 *  sampling one more cue. This average is weighted for the probability of the next cue having a positive or negative label. To reiterate, the expected fitness
 *  of a child node is the expected fitness of the best action the agent can perform when in that child node.
 *  
 *  The expected fitness of accepting is the fitness that the agent will have when finishing the last cycle (not this cycle!) with a budget that results 
 *  from accepting the resource now, experiencing the extrinsic event of this cycle, starting the second cycle with the resulting budget. That is, it is
 *  the fitness associated with the budget that results from this cycle, and all expected changes in budget from now onward to cycle n. To make 
 *  this a little clearer, we can formalize it as follows. 
 *  If we let 	IM 		be the all possible immediate outcomes of the resource encounter if the agent eats plus the expected extrinsic event value, 
 *  			pIM 	be the probabilities of all immediate outcomes,
 *  			EFVO(x) be the expected future VALUE outcome (i.e., changes in budget) of starting the second cycle with budget x 
 *  					(i.e., EFVO is the (possibly discounted) sum of all resource encounters/extrinsic event outcomes of cycle 2 to cycle n), and 
 *  			f(x) 	be the fitness value associated with having budget x after the last cycle, 
 *  then the 
 *  
 *  	expected FITNESS outcome of accepting 
 *  	=		f(budget AFTER the n'th cycle)
 * 		=		sum over all im:IM [ f( current budget + IM + discounted EFVO(current budget + IM) ) * pIM,   ) ]
 *  
 *  That is, the fitness value of having the current budget and the expected outcome of all cycles. (Note that this is a simplification: I 
 *  am not taking into account the discount rate or the interruption rate - see the javadoc above DecisionTree.expectedOutcomeAccepting() for more info).
 *  For now, let's assume that we already know what EFVO(x) is (this is actually what the value iterator computes - more on that in section 2). 
 *  
 *  The expected fitness of discarding is almost the same as the expected fitness of accepting, but then the immediate outcome if the first cycle is just
 *  the outcome of the extrinsic event (i.e., the resource encounter results in a value of 0). 
 *  
 *  After the backward pass the decision tree may look something like this:
 *    					    		     	     |---(D, 3, 2+, 1-, E)
 *  		            |---(B, 4, 1+, 0-, S)----|
 *  		 	        |				         |
 *  (A, 5, 0+, 0-, S)---|				 	     |---(E, 3, 1+, 1-, D)
 *  			  	    |				 	     |
 *  			  	    |---(C, 4, 0+, 1-, D)----|
 *  					    		 	         |---(F, 3, 1+, 2-, D)
 *  
 *  Where a node now has a best action appended to it - S means that the best action is to sample, D means that the best action is to discard, and
 *  E means that the best action is to eat. 
 *  
 *  1. 3. Constructing the decision tree: the forward pruning pass
 *  We're almost there, but the tree above is slightly off: it still contain a node (node F) that cannot ever be reached. The only
 *  way the agent might find itself in the state of node F would be if it samples a cue when in state node C, and the sampled cue has
 *  a negative label. However, when in node C, the agent will not sample, but will discard the resource. Another thing that is off, but
 *  not visible in the plot, is that the probability of ending up in node E is smaller than it was above - again, because the agent
 *  samples a cue in node C, it cannot reach node E via this way. To have a 'good' decision tree, we have to remove impossible pathways
 *  from the tree. This is done in the forward pruning pass. Since the procedure for this isn't terribly complicated, I'll leave it
 *  as an exercise for the reader to figure out how to do this (or, you know, write in in the javadoc above DecisionTree.forwardPruningPass).
 *  
 *  After the forward pruning pass, the tree may look something like this:
 *    					    		     	     |---(D, 3, 2+, 1-, E)
 *  		            |---(B, 4, 1+, 0-, S)----|
 *  		 	        |				         |
 *  (A, 5, 0+, 0-, S)---|				 	     |---(E, 3, 1+, 1-, D)
 *  			  	    |				 	     
 *  			  	    |---(C, 4, 0+, 1-, D)
 *  					    		 	         
 *  
 *  1.4. Computing the expected number of sampled cues.
 *  We can now determine the optimal number of cues to sample by summing the product of the number of cues sampled in each
 *  node that has a best action of accepting or discarding and the probability of reaching that node. 
 *  
 *  
 *  
 *  
 *  2. COMPUTING EFVO(x): THE EXPECTED FUTURE VALUE OUTCOME OF STARTING CYCLE 2 WITH BUDGET X.
 *  
 *  2.1 Value iterator
 *  
 *  Above I assumed that we already know EFVO(x), the function that takes an input a starting budget at 
 *  cycle 2, and returns as output the sum of all future outcome cycles (these outcomes are VALUES, not 
 *  FITNESSES - that is, they represent the change in budget from 2 to n). Calculating this function - 
 *  stored in a table format - isn't that straight forward and involves the use of a VALUE ITERATION
 *  procedure (yes, the agent is called a value iterator, even though the value iteration algorithm is 
 *  only used in one part of the agent, but hey, its by far the most computation heavy part). 
 *  
 *  So, how do we compute the expected future value outcomes of all future cycles, starting cycle 2 with a budget b?  
 *  If we let O(t,b) be a set containing all possible outcomes of starting cycle t with budget b, and p(x) be the probability of x,
 *  the expected future value outcome for the second cycle is:
 *  
 *  future value outcome for cycle 2 given budget b 
 *  = sum forall o2:O(2,b) { p(o2)* (  o2 + 
 *  				sum forall o3:O(3,b+o2) {  p(o3) * ( o2 + o3  + 
 *  							sum forall o4:O(4,b+o2+o3) { p(o4) * (o2+o3+o4+ ...
 *  							}
 *  				}
 *    }
 *  
 *  Since this is a Javadoc, I cannot use a summation sign here. This makes the statement above unnecessarily difficult to read.
 *  If we let S_[set]{} be the average weighted sum over all elements in a set xx (weighed for the probability of each element), 
 *  and the elements we iterate over still be represented by o, we can write the equation above in a slightly more readable format:
 *  
 *    future value outcome for cycle 2 given budget b 
 *  	= S_O(2,b) { o2 + 	S_O(3,b+o2) { o2 + o3  + S_O(4,b+o2+o3) { (o2+o3+o4+ ...}}}
 *    
 * Or, in natural language: we can compute the future value for cycle 2 given the starting budget b as the outcome 
 * of cycle 2 plus the outcome of cycle 3 given the outcome of cycle 2 plus the outcome of cycle 4
 * given the outcomes of cycles 2 and 3, up to cycle n. This is equation is (at least to me) completely unintuitive - we need
 * to simplify matters here. 
 * 
 * One thing to notice is that the expected value outcome of the fourth cycle depends on the outcomes of both the second and third cycle.
 * Do we, strictly speaking, need to know the outcomes of all previous cycles to determine the expected outcome of a current cycle? 
 * For instance, do we need to know the outcomes of cycles 2 and 3 to compute the outcome of cycle 4? The answer is no, we only need to know the
 * starting budget of the t'th (4th) cycle, which is the expected outcome of the previous cycle (2 and 3). If we already know what the
 * outcome is of cycle t-1, we do not need any information on the outcome of cycle t-2, t-3, ... t0. This is the Markov Property, 
 * and it means we can severely reduce the complexity of our problem. Specifically, rather than writing a very long sum over all possible
 * cycles (as above), we can say that the expected future value outcome of a cycle (any cycle, not just the first) is the expected outcome
 * of that cycle, plus the expected future value outcome of starting the next cycle with the resulting budget. Let EFVO(x|t) be the expected 
 * outcomes of cycles t to T, starting with budget x. Rather than writing out the whole sum as above, we can write:
 * 
 * EFVO(b|2) 	= S_O(2,b) 		{ o + EFVO(b+o|3) }
 * EFVO(b|3) 	= S_O(3,b) 		{ o + EFVO(b+o|4) }
 * ...
 * EFVO(b|T-1) 	= S_O(T-1,b) 	{ o + EFVO(b+o|T) }
 * EFVO(b|T) 	= S_O(T,b) 		{ o }
 * 
 * Or, in general:
 * EFVO(b|t) 	= S_O(t,b) 		{ o + EFVO(b+o|t+1) }		if t < T
 * EFVO(b|T) 	= S_O(T,b) 		{ o }
 * 
 * The last line is interesting: in the final time point T the expected future value outcome of a cycle is simply the weighted sum of all
 * possible possible outcomes of that cycle. However, if we know EFVO(b|T) for all possible starting budgets b, we can solve 
 * EFVO(b|T-1) by plugging in the values of the final cycle. And, if we know the second-to-last cycle, we can also solve the third-to-last
 * cycle. Etcetera, etcetera. 
 * 
 * Since we do not (really) care about the EFVO(b|t)'s for other t's than 2, we can solve this problem using a value iterator. Consider
 * a table, called the valueFunction, that contains a row for each possible budget, and has one column representing the expected value outcome 
 * for that budget. Let ValueFunction(b) be the stored value for budget b. There will be multiple iterations of this table, so i'll append the name with 
 * "_[iteration number]". Each iteration of this table holds the EFVO's for all cycles that we have computed thus far, starting at the last cycle 
 * and working backwards in time. Hence, we iterate T-1 times over this table (the -1 is from the fact that we do not start at the first, but at the 
 * second cycle). In the first iteration we set all the values to 0:
 * 
 * ValueFunction_0(b) <-	0		for all budgets b
 * 
 * Next, we iterative from 1 to T-1, updating the ValueFunction as follows:
 * 		ValueFunction_t(b) <- weighted sum over:
 * 				 [immediate outcome o, given the of starting a cycle with budget b] + [expected future value outcome of ending a cycle with budget b + immediate outcome], 
 * 				 for all possible outcomes of that cycle. (sum weighted by the probability of each outcome)
 * 
 * Which is the same as:
 * 		ValueFunction_t(b) <- sum over all immediate cycle outcomes o: p(o) * o + [expected future value outcome of ending a cycle with budget b + immediate outcome]
 * 
 * or:
 * 		ValueFunction_t(b) <- sum over all immediate cycle outcomes o: p(o) * o + ValueFunction_t-1(b+o)
 * 
 * After T-1 iterations, the ValueFunction contains the EFVO(b) for cycle 2, for all possible budgets b.
 * 
 * 2.2: Computing o, the immediate outcome of a cycle given a starting budget b.
 * Hence, using the value iteration algorithm, we can iterate backwards over the cycles T to 2, and find the EFVO's. However, I did not mention yet
 * how we would find the immediate outcome of a cycle, o. This subsection is, surprisingly, the easiest to answer: it is the same as section 1. To reiterate,
 * in section 1 the agent computed the optimal number of cues to sample in the first cycle, given that the EFVOs were already known. It did so by constructing
 * a decision tree in a forward pass, deciding on the best action in each node in the backward pass, and pruning the tree in the forward pruning pass. The
 * fitness of a sampling action is the weighted sum of all possible successor nodes. The expected fitness of accepting and discarding a resource is 
 * 
 *  sum over all im:IM [ f( current budget + IM + ValueFunction(current budget + IM) ) * pIM,   ) ]
 * 
 * What we want to compute for the value iterator is the expected value outcome of starting a cycle - i.e., the expected value outcome of the root node.
 * We can use the same procedure for the other cycles, with the difference that we do not use the fitness function f() to evaluate accepting and discarding 
 * decisions. Rather, we just use the values. In addition, since we are interested in the expected value of the root node, we do not have to go through a
 * forward pruning pass.
 * 
 * 
 * 
 * 3. AN INFINITE NUMBER OF CYCLES: USING CONVERGENCE
 * Thus far I have assumed that we are interested in an agent that will go through exactly n cycles in its lifetime. However, often we might be interested
 * in the more general case where the number of cycles might be infinite. Although it does not seem like it, the methods describe thus far can deal
 * with an infinite number as cycles as well, as long as some restrictions are met (most importantly: the mean of the resource encounters shouldn't be
 * too high or the future is discounted). In these cases the value iterator's estimates of EFVO over iterations change less and less - until finally, they
 * converge to a stable set of numbers (for why this is, I refer to Sutton and Barto's book). This will, however, take some time: perfect convergence
 * can only be guaranteed with an infinite number of iterations. However, as the number of iterations increases, the difference between the most recent
 * iteration and the iteration before that decreases (i.e., it asymptotically converges to a stable set). This change from one iteration to the next, called
 * the delta, always decreases. Hence, rather than running an infinite number of iterations, we can continue the iterations until the delta falls below
 * some pre-specified level. This level is called the epsilon, and is user specified. For more information, see the Sutton and Barto book, and 
 * https://en.wikipedia.org/wiki/(%CE%B5,_%CE%B4)-definition_of_limit. 
 * 
 * 
 * 
 * 4. BATCH UPDATING OR ONLINE UPDATING
 * Above I assume we are using batch updating. That is, that we update the value function only after a full iteration over 
 * all possible budgets. In practice this means that in each iteration of the value iteration algorithm we store the new (left-hand side) values in a
 * separate ValueFunction:
 * 
 * 		NewValueFunction(b) <- sum over all immediate cycle outcomes o: p(o) * o + lambda * OldValueFunction(b+o)
 * 
 *
 * After each iteration we swap the newValueFunction with the OldValueFunction.  As a result, when we estimate the expected value outcome of a budget we use the 
 * best estimates of the previous iteration (cycle). This corresponds with the notion that the agent goes through seperate cycles. Indeed, if we are interested
 * in the case where the agent will go through a limited number of cycles, this method HAS TO BE USED.
 * 
 * If, however, we are interested in an infinite number of cycles, we can also used online updating. In online updating we only have 1 value function, 
 * and old estimates are directly overwritten:
 * 
 * 		ValueFunction(b) <- sum over all immediate cycle outcomes o: p(o) * o + lambda * ValueFunction(b+o)
 * 
 * Hence, when computing a new estimate for the expected future value outcome of a budget we use the latest and best estimates of the expected future value 
 * outcomes of all other budgets. Online updating will convergence to the same value function as batch updating, but will do so much faster. However,
 * the intermediate values of online updating need not match the intermediate values of batch updating. Thus, online updating should be used when
 * we want to study the agent going through an INFINITE NUMBER OF CYCLES ONLY.
 * 
 * 
 * 
 * 5. EFFICIENTLY COMPUTING FITNESS OUTCOMES IN THE FIRST CYCLE
 * During the last step of the algorithm, described in section 1, the agent creates the decision trees for the first cycle - one
 * tree for each possible budget the agent can have as a starting budget. In each decision tree there are many decision nodes,
 * and each node needs to evaluate the expected fitness of accepting and discarding. As defined in section 1, this expected
 * fitness function is: 
 * 		expected FITNESS outcome of accepting 
 *  	=		fitness(discounted budget AFTER the n'th cycle)
 * 		=		fitness(starting budget + IM + discounted EFVO(starting budget + IM) ), for all IM that might result from accepting
 * 
 * (Or, in natural language: the expected fitness of accepting is the fitness that results from having a budget equal to 
 * the current budget plus the immediate reward plus all future values, at the final iteration).
 * The EFVO(x) function is computed by the value iterator, described in section 2:
 *
 *   	expected FITNESS outcome of accepting 
 * 		=		fitness(starting budget + IM + lambda * ValueFunction(starting budget + IM) ), for all IM that might result from accepting
 * 
 * Since we sum over all possible immediate outcomes (the discarding decision is the same decision, but then with an outcome
 * of 0 for the resource encounter), we have to evaluate the fitness function many (sum), many (decision node), many (decision
 * tree) times. However, computing f(x) is an expensive operation - this function is evaluated in R, not JAVA. 
 * 	
 * There is, fortunately, an insight that reduces the computation time significantly. Notice that starting budget plus IM in
 * the equation above gives us the starting budget of the second cycle. Hence, we can rewrite the equation as:
 * 		expected FITNESS outcome of accepting 
 * 		=		fitness( budget starting second cycle + lambda * ValueFunction ( budget starting second cycle) ), for all possible 2nd cycle starting budgets
 * 
 * Since there is only a limited number of budgets the agent can start the second cycle with, we can precompute this fitness outcome. If 
 * x is the starting budget of the second cycle: (for convenience, we can store this is the value function):
 *      FitnessValueFunction(x)
 *      =		fitness( x + lambda * ValueFunction (x) )
 * 
 * We only have to compute this function once, and can use it to efficiently evaluate the fitness outcomes of accepting and discarding
 * during the first cycle.  
 * 
 * One additional note: if (x +  lambda * ValueFunction (x) ) is smaller than 0, the associated fitness should be zero as well (dead things
 * hold no fitness).
 * 
 *      
 *      
 * 6. CHRONOLOGICAL SUMMARY 
 * Notice that in run time, section 2 has to be done before section 1. To summarize, we need to do the following steps: first,
 * we need to compute EFVO(b), the expected future value outcomes from cycle 2 to cycle n. This is done with the use
 * of a value iterator that repeatedly makes (forward pass) and evaluates (backward pass) decision trees. The value iterator
 * will continue until either the value function converges, or until a maximum number of cycles has been reached.
 * 
 * After the value iterator reached a set number of iterations or converged (section 3 & 4) we have an estimate for the expected future value 
 * outcome for each budget level in the second cycle. Using this estimate we can create the FitnessValueFunction(x), which 
 * takes the starting budget of the second cycle as an input, and returns the expected terminal fitness (section 5). Using this
 * estimate we can create one final decision tree (section 1) where we directly compute expected fitness values and determine the
 * optimal number of cues to sample.
 * 
 * 
 * 
 * 7. PSUEDOCODE FOR THE ALGORITHM
 * 0. Calculate the probability distributions for all resource encounters, for all set of cues the agent
 * can possibly encounter. This calculation is done by creating a new PosteriorProbabilityTable object,
 * which contains all these posterior probabilities given agent's environment.
 *
 * **** Section 2: estimating the EFVO's for cycles 2-n, or convergence****
 * 0. <User specifies starting estimates, epsilon, maximumIterations, batchUpdate, MDP, environment>
 * 
 * 1. Initialize the posterior probability table that corresponds to the agent's environment
 * 2. Initialize ValueFunction (with values equal to the startingEstimate).
 * 3. delta 									<- epsilon +1
 * 4. iteration			 						<- 0
 * 5. Initialize array sumOfOutcomes, which will contain the total sum of all values in the value function
 * 6. terminate									<- false			if maximumIterations < 2
 * 												   true				if maximumIterations == 1
 * 7. while terminate is false:
 * 		a. delta 								<- 0
 * 		b. iteration							+= 1
 * 		c. if batchUpdating:
 * 				oldValueFunction				<- valueFunction (deep clone)
 * 		d. sum 									<- 0
 * 		e. forall budget in the value function: derive a new estimate based on the newValueFunction, i.e.:
 * 			i:   temp 							<- ValueFunction(b) 
 * 			ii:  Grow a new DecisionTree with b as the starting nodes value
 * 			iii: Grow this tree fully (forward pass): branch every node until the maximum number of cues has been
 * 					sampled or the budget has reached a non-positive value. Each Node consists of a probability
 * 					distribution over resource values, which is conditioned on a set of received cues. Edges
 * 					between nodes represent the agent receiving one additional cue.	Each node has n branches,
 * 					where n is the possible number of cue values. Note that the probability of each branch is the
 * 					probability	of receiving that cue conditioned on the probability distribution of the parent.
 * 					Posterior probability distributions for each node are retrieved from the calculations performed
 * 					at step 0.
 * 			iv:  Working backwards from the final nodes (backwards pass), calculate the expected outcome for each
 * 					action in each node. The expected outcome of accepting or discarding is the immediate outcome of
 * 					the resource encounter (0 for discarding), plus the expected outcome of the extrinsic event,
 * 					plus the expected future outcome value of starting the next encounter with the budget resulting
 * 					the resource encounter and extrinsic event.
 * 						The immediate reward is given by the node's probability distribution over the resource value
 * 						discounted by the inverse probability of the interruption rate. The expected outcome of the
 * 						next encounter is the budget that result plus or minus the extrinsic event that takes place
 * 						between encounters. If using batch updating the future outcomes are found in the OLD ValueFunction.
 * 						Otherwise, use the valueFunction.
 * 					After computing the expected fitness of each action, set the bestAction to the action with the
 * 						highest expected outcome (when tied, select multiple).
 * 			v:   The expected outcome of starting an encounter with a budget of b is the expected outcome of the root node.
 * 					This value is stored in the ValueFunction:
 * 					valueFunction(b)			<- 	expectedOutcome(root)
 * 			vi:  delta 							<- max(delta, 	abs(temp - valueFunction(b) ) )
 *			vii: sum							+= valueFunction(b)
 * 		f. sumofOutcomes[iteration]				<- sum
 * 		g. if iteration = maxNumberOfIterations-1 OR delta < epsilon OR maximum run time exceeded:
 * 			terminate 							<- true 
 *
 * **** Section 5: computing the fitness value function****
 * 8. forall budget in the value function:
 * 	  if (x + lambda * ValueFunction (b)) < 0: 		fitnessValueFunction(budget) = 0
 * 	  else 											fitnessValueFunction(b) = fitness(x + lambda * ValueFunction (b))
 * 
 * **** Section 1: building the decision tree for cycle 1, using the FitnessValueFunction and determine number of cues sampled****
 * 9. Initialize cuesSampled, a DecimalNumberArray
 * 10. forall budgets in the value function: 
 *    Grow a new DecisionTree with b as the starting nodes value, where all accepting and discarding actions are
 *    evaluated on the basis of their FITNESS effects
 * 		a. Grow this tree fully (forward pass)
 * 		b. Backward pass: calculate the expected outcome for each action in each node. The expected outcome of accepting or discarding 
 *         is found in the FitnessValueFunction. 
 *      c. Make tree go through forward pruning pass.
 *      d. cuesSampled[b]						<- tree.cuesSampled (see section 1.4).
 *       
 * 11. Save results to output folder and inform the model that this agent is finished.
 *
 */
public class ValueIterator extends Agent  {
	private static final long serialVersionUID = Helper.programmeVersion;

	public ArrayList<DecimalNumber> sumOfOutcomes;
	public ArrayList<DecimalNumber> deltas;
	public ValueIteratorValueFunction valueFunction;

	public ValueIterator(Model model, MarkovDecisionProcess mdp, AbstractEnvironmentBuilder environmentBuilder, EstimationParameters estimationParameters, ValueFunction startingValueFunction, int startingIteration) throws IOException, MisspecifiedException {
		super(model, mdp, environmentBuilder, estimationParameters, startingValueFunction, startingIteration);
	}

	/** See the long comment in the ValueIterator class. */
	public void run() 
	{
		boolean stopRun = false;
		boolean agentTerminatedSuccessfully = false;
		DecimalNumber epsilon = estimationParameters.stoppingCriteriaConvergenceEpsilon;
		
		try {
			// 0. Turn the EnvironmentBuilder into an Environment
			if (environmentBuilder instanceof EnvironmentBuilderLazy)
				((EnvironmentBuilderLazy) environmentBuilder).setMDP(mdp);
			environment = environmentBuilder.toEnvironment();
			// The environment should be fully specified
//			DecimalNumberArray values = mdp.POSSIBLE_VALUES;
//			for (int i = 0; i < values.length(); i++)
//				if (!environment.getMatrix().getRow("Value").get(0).equals(values.get(i)))
//					throw new MisspecifiedException("Exception in Environment constructor: when creating an environment from an environment builder: the matrix is not fully specified");

			
			// 1. Initialize the posterior probability table that corresponds to the agent's environment
			PosteriorProbabilityTable posteriorProbabilityTable = new PosteriorProbabilityTable(mdp, environment);

			// 2. If there is a value function specified (Agent's startingValueFunction): cast it to a ValueIteratorValueFunction. 
			//Else create a new value function in which we will store the updates in.
			if (startingValueFunction != null) 
				if (startingValueFunction instanceof ValueIteratorValueFunction) {
					valueFunction = (ValueIteratorValueFunction) startingValueFunction;
					System.out.println(Helper.timestamp() +" Agent " + this.ID + " is initialized with an existing value function");
				} else 
					throw new IllegalArgumentException("Exception in ValueIterator.run(): value function type is not ValueIteratorValueFunction");


			else {
				valueFunction = new ValueIteratorValueFunction(mdp, environment, estimationParameters.startingEstimates, estimationParameters.optimizer);
				System.out.println(Helper.timestamp() +" Agent " + this.ID + " is initialized WITHOUT an existing value function");
			}

			// 3. Set delta to epsilon + 1. This makes sure it doesn't terminate before the first iteration
			DecimalNumber delta = epsilon.add(1);

			// 4. Set iteration to startingIteration
			int iteration = startingIteration;

			// 5. Initialize array sumOfOutcomes, which will contain the total sum of all values in the value function
			sumOfOutcomes = new ArrayList<>(); //used for convergence checking
			deltas = new ArrayList<>();
			
			// 6. terminate									<- false			if maxNumberOfIterations < 2
			// 												   true				if maxNumberOfiterations == 1 || iterations == maxNumberOfiterations
			boolean terminate = false;
			if (estimationParameters.useStoppingCriteriaIterations && 
					(estimationParameters.stoppingCriteriaIterationsMaximum ==1 || iteration == estimationParameters.stoppingCriteriaIterationsMaximum)) 
				terminate = true;
			
			// 7. Compute the optimal value function
			System.out.println(Helper.timestamp() + " STARTING RUN:\t\t agent " + this.ID + " has started its run...");

			while (!terminate && !stopRun)
			{
				if (Thread.interrupted())
					stopRun = true;
				
				// 7.a. 
				delta.set(0);

				// 7.b.
				iteration++;
				
				// 7.c.
				ValueIteratorValueFunction oldValueFunction = null;
				if (estimationParameters.batchUpdating)
					oldValueFunction = ((ValueIteratorValueFunction) valueFunction).clone();
				
				// 7.d.
				DecimalNumber sum = new DecimalNumber(0);			
				//7. e.
				for (DecimalNumber budget: valueFunction.getBudgets() )
				{
					// 7.c.i. Get current value of the budget's fitness
					DecimalNumber temp = valueFunction.getExpectedFutureValueOutcome(budget);

					// 7.c.ii.
					DecisionTree decisionTree = new DecisionTree(mdp, environment, estimationParameters, posteriorProbabilityTable, budget);

					// 7.c.iii.
					decisionTree.forwardPass();

					// 7.c.iv.
					if (estimationParameters.batchUpdating)  decisionTree.backwardPass(false, oldValueFunction, false);
					else								     decisionTree.backwardPass(false, valueFunction, false);

					// 7.c.v.
					valueFunction.setExpectedFutureValueOutcome(budget, decisionTree.root.expectedOutcomes().max());

					// 7.c.vi. 
					delta.set(DecimalNumber.max(delta, temp.subtract(valueFunction.getExpectedFutureValueOutcome(budget),false).abs()));

					// 7.c.vii. 
					sum.add(valueFunction.getExpectedFutureValueOutcome(budget));
				}

				// 7.f. 
				sumOfOutcomes.add(sum);
				deltas.add(delta.clone());

				// 7.g.
				if (isDone(delta, iteration, startTime))
					terminate = true;
				
				//System.err.println(valueFunction);
				System.out.println(Helper.timestamp() + "\tITERATION COMPLETE: \t  Agent " + this.ID + " has finished iteration " + iteration + ". (Delta: " + delta + ")");
					
			}
			System.out.println(Helper.timestamp() + " TERMINATION: \t  Agent " + this.ID + " has met it's stopping criteria on iteration " + iteration + " with a delta of " + delta + ". (Thread interruption: " +Thread.interrupted() + "). Computing fitness..." );

			// 8. computing the fitness value function
			DecimalNumberArray expectedFitnessOutcomes = valueFunction.getBudgets().clone();
			DecimalNumberArray discountedValueFunction = valueFunction.toDecimalNumberMatrix().getColumn("Expected value").clone().scale(mdp.DISCOUNT_RATE);		
			for (int i = 0; i < expectedFitnessOutcomes.length(); i++) {
				expectedFitnessOutcomes.get(i).add(discountedValueFunction.get(i));
				if (expectedFitnessOutcomes.get(i).compareTo(0) <= 0)
					expectedFitnessOutcomes.set(i, 0);
			}
			expectedFitnessOutcomes = RserveManager.evaluateFunction(mdp.FITNESS_FUNCTION, expectedFitnessOutcomes);
			for (int b = 0; b < valueFunction.getBudgets().length(); b++ )
				valueFunction.setTerminalFitnessOutcome(valueFunction.getBudgets().get(b), expectedFitnessOutcomes.get(b));
			valueFunction.toDecimalNumberMatrix().setColumn("Expected fitness", expectedFitnessOutcomes);
	
			// 10. Create the output object. Specifically, create 4 DecimalNumberMatrices and 3 DecimalNumberArrays, each with b rows/lenghts, 
			// where b is the number of possible budget states. In these objects we store, for each possible budget state:
			// 1) an (optimal) distribution of cues sampled (i.e., {p(sampled 0 cues), p(sampled 1 cue), ..., p(sampled n cues)}, 
			// 2) a distribution of the proportion of agents accepting, for each possible number of cues sampled:
			//		{p(accepting|0 cues), p(accepting|1 cue), p(accepting|2 cues), ... }
			// 3) a distribution of the proportion of agents discarding, for each possible number of cues sampled:
			//		{p(discarding|0 cues), p(discarding|1 cue), p(discarding|2 cues), ... }
			// 4) the expected number of cues sampled
			// 5) the expected proportion of accepting (i.e., the mean of 2)
			// 6) the expected proportion of discarding (i.e., the mean of 3)
			// 7) an expected fitness outcome for all possible actions (except dead-on-start) at the root node,
			// See the javadoc of the ValueIteratorOutput constructor for more info.
			System.out.println(Helper.timestamp() + " TERMINATION: \t  Agent " + this.ID + " is creating optimal policy decision trees for the first cycle (fitness)... ");

			int budgetStates							= valueFunction.getBudgets().length();
		
			// Create the matrices  
			DecimalNumberMatrix cuesSampled						= new DecimalNumberMatrix(budgetStates, mdp.MAXIMUM_CUES+1);
			DecimalNumberMatrix	proportionAccepting    			= new DecimalNumberMatrix(budgetStates, mdp.MAXIMUM_CUES+1);
			DecimalNumberMatrix proportionDiscarding 			= new DecimalNumberMatrix(budgetStates, mdp.MAXIMUM_CUES+1);
			DecimalNumberMatrix	cueDominanceEating    			= new DecimalNumberMatrix(budgetStates, 2);
			DecimalNumberMatrix cueDominanceDiscarding 			= new DecimalNumberMatrix(budgetStates, 2);
			DecimalNumberMatrix expectedFitnessOutcomesRoot		= new DecimalNumberMatrix(budgetStates, 3);
			DecimalNumberMatrix expectedImmediateOutcomesRoot	= new DecimalNumberMatrix(budgetStates, 3);
			
			// Create the arrays
			DecimalNumberArray expectedCuesSampled 					= new DecimalNumberArray(budgetStates);
			DecimalNumberArray cuesSampledConditionalOnAccepting 	= new DecimalNumberArray(budgetStates);
			DecimalNumberArray cuesSampledConditionalOnDiscarding 	= new DecimalNumberArray(budgetStates);
			DecimalNumberArray totalProportionAccepting 			= new DecimalNumberArray(budgetStates);
			DecimalNumberArray totalProportionDiscarding 			= new DecimalNumberArray(budgetStates);
		
			// Set column names on the expected outcome matrix
			expectedFitnessOutcomesRoot.setColumnNames("Sampling", "Accepting", "Discarding");
			expectedImmediateOutcomesRoot.setColumnNames("Sampling", "Accepting", "Discarding");
			
			int nBudgets = valueFunction.getBudgets().length();
			DecimalNumberArray cuesSequence = DecimalNumberArray.sequence(0, mdp.MAXIMUM_CUES, 1);
			for (int b=0;b<nBudgets;b++)
			{

				if (Thread.interrupted()) 
					stopRun = true;
				if (stopRun)
					break;
				
				// Create a new tree for each budget, and make it into a policy by putting it through a forward pruning pass
				DecisionTree dt = new DecisionTree(mdp,environment, estimationParameters, posteriorProbabilityTable, valueFunction.getBudgets().get(b));
				dt.forwardPass();
				dt.backwardPass(true, valueFunction, true);
				dt.forwardPruningPass();
				

				// This tree contains all the information that we need, although we sometimes need to do some additional manipulations
				cuesSampled.getRow(b).setAll(dt.getCuesSampled());
				
				proportionAccepting.getRow(b).setAll(dt.getProportionEating());
				proportionDiscarding.getRow(b).setAll(dt.getProportionDiscarding());
				
				cueDominanceEating.getRow(b).setAll(dt.getDominancesWhenEating());
				cueDominanceDiscarding.getRow(b).setAll(dt.getDominancesWhenDiscarding());
				
				expectedFitnessOutcomesRoot.getRow(b).setAll(dt.getExpectedOutcomes());
		
				// Compute the immediate outcomes (p(benefit)*benefit + p(cost)*cost) for all possible action
				DecimalNumber expectedImmediateOutcomeAccepting;
				DecimalNumber expectedImmediateOutcomeDiscarding;
				if (b != 0) {
					DecimalNumber expectedImmediateBenefitsAccepting = dt.root.acceptingExpectedImmediateBenefits.multiply(dt.root.acceptingProbabilityBenefits, false);
					DecimalNumber expectedImmediateCostsAccepting = dt.root.acceptingExpectedImmediateCosts.multiply(dt.root.acceptingProbabilityCosts, false);
					expectedImmediateOutcomeAccepting = expectedImmediateBenefitsAccepting.add(expectedImmediateCostsAccepting);

					DecimalNumber expectedImmediateBenefitsDiscarding = dt.root.discardingExpectedImmediateBenefits.multiply(dt.root.discardingProbabilityBenefits, false);
					DecimalNumber expectedImmediateCostsDiscarding = dt.root.discardingExpectedImmediateCosts.multiply(dt.root.discardingProbabilityCosts, false);
					expectedImmediateOutcomeDiscarding= expectedImmediateBenefitsDiscarding.add(expectedImmediateCostsDiscarding);
				} else {
					expectedImmediateOutcomeAccepting = new DecimalNumber(0);
					expectedImmediateOutcomeDiscarding = new DecimalNumber(0);
				}
				DecimalNumber expectedImmediateOutcomeSampling = mdp.COST_OF_SAMPLING;

				expectedImmediateOutcomesRoot.getRow(b).setAll(new DecimalNumberArray( expectedImmediateOutcomeSampling, expectedImmediateOutcomeAccepting, expectedImmediateOutcomeDiscarding ));
				
				// Retrieve the expected number of cues to sample from the root node
				expectedCuesSampled.set(b, dt.getExpectedCuesSampled());
				
				// Compute the number of cues sampled conditional on ending up in an 'accepting' or 'discarding' node
				DecimalNumberArray proportionAcceptingArray= dt.getProportionEating().clone().scaleToSumToOne();
				DecimalNumberArray proportionDiscardingArray= dt.getProportionDiscarding().clone().scaleToSumToOne();
				if (!dt.getTotalProportionEating().equals(0, true))
					cuesSampledConditionalOnAccepting.set(b,proportionAcceptingArray.multiply(cuesSequence).sum());
				else
					cuesSampledConditionalOnAccepting.set(b, 0);
				
				if (!dt.getTotalProportionDiscarding().equals(0, true))
					cuesSampledConditionalOnDiscarding.set(b,proportionDiscardingArray.multiply(cuesSequence).sum());
				else
					cuesSampledConditionalOnDiscarding.set(b,  0);
				
				
				totalProportionAccepting.set(b, dt.getTotalProportionEating());
				totalProportionDiscarding.set(b, dt.getTotalProportionDiscarding());
				if (b % 50 == 0)
					System.out.println(Helper.timestamp() + " TERMINATION: \t  Agent " + this.ID + " is creating optimal policy decision trees for the first cycle (fitness)...  Completed " + b + " out of " + nBudgets + " (" + Math.round((double) b / (double) nBudgets * 100) + "%)");
				
			}
		
			if (stopRun) {
				System.out.println(Helper.timestamp() + " INTERRUPTION: \t  Agent " + this.ID + " has been interrupted, likely due to user request. Stopping all computations now. There will be nothing saved and all intermediate results are deleted.");
				return;
			}
				
			agentTerminatedSuccessfully = true;
			System.out.println(Helper.timestamp() + " TERMINATION: \t  Agent " + this.ID + " is done creating optimal policy decision trees and is now preparing to save its result to the disk... ");
			
			ValueIteratorOutput output =  new ValueIteratorOutput(
					environment.toFilenameString() + " ID="+this.ID,
					mdp, 
					environment,
					estimationParameters,
					valueFunction, 
					posteriorProbabilityTable, 
					
					cuesSampled, 
					proportionAccepting,
					proportionDiscarding,
					expectedFitnessOutcomesRoot,
					expectedImmediateOutcomesRoot,
					
					expectedCuesSampled,
					cuesSampledConditionalOnAccepting,
					cuesSampledConditionalOnDiscarding,
					
					cueDominanceEating,
					cueDominanceDiscarding,
					
					totalProportionAccepting,
					totalProportionDiscarding,
					
					delta, 
					iteration + 1 ); // The +1 is for the fitness step
			
			System.out.println(Helper.timestamp() + " TERMINATION: \t  Agent " + this.ID + " is saving the data to the .out file...." );

			if (estimationParameters.outputFolder != null)
				this.writeOutput(output);



	} catch (Exception e) {
		ObserverManager.notifyObserversOfError(e);
		} finally {
		// Notify the model that the agent has stopped
		if (model != null)
			model.notifyAgentIsDone(this, agentTerminatedSuccessfully);
		

	}
	}
	
}
