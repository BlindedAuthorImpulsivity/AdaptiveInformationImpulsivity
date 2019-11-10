package start;

import org.junit.jupiter.api.Test;

import agent.ValueIterator.DecisionTree;
import agent.ValueIterator.PosteriorProbabilityTable;
import agent.ValueIterator.ValueIterator;
import agent.ValueIterator.ValueIteratorValueFunction;
import decimalNumber.DecimalNumber;
import defaults.Defaults;
import environment.Environment;
import environment.EnvironmentBuilderFull;
import estimationParameters.EstimationBuilder;
import estimationParameters.EstimationParameters;
import estimationParameters.Optimizer;
import helper.Helper;
import markovDecisionProcess.MarkovDecisionProcess;
import markovDecisionProcess.MarkovDecisionProcessBuilder;

class ModelTest {

	/*@Test
	void mdpConstruction() {
		try {
			System.out.println("\n\n\n" + Helper.repString("+=", 50) + "\nTEST OF MDP CONSTRUCTION\n");
			Defaults defaults = Defaults.mainDefaults;
			MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
			mdpBuilder.setDefaults(defaults);

			MarkovDecisionProcess mdp = new MarkovDecisionProcess(mdpBuilder);
			System.out.println(mdp);
		} catch (Exception e) { e.printStackTrace();}
	}*/

//	@Test
//	void environmentConstruction() {
//		try {
//			System.out.println("\n\n\n" + Helper.repString("+=", 50) + "\nTEST OF ENVIRONMENT CONSTRUCTION\n");
//			Defaults defaults = Defaults.mainDefaults;
//			MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
//			mdpBuilder.setDefaults(defaults);
//			MarkovDecisionProcess mdp = new MarkovDecisionProcess(mdpBuilder);
//		
//			EnvironmentBuilderFull ebf = defaults.startingEnvironmentPopulationFull.get(0);
//			ebf.setMDPBuilder(mdpBuilder);
//			ebf.update();
//			Environment e = ebf.toEnvironment();
//			System.out.println(e);
//		} catch (Exception e) {e.printStackTrace();}
//	}
//	
//	@Test
//	void posteriorProbabilityConstruction() {
//		try {
//			System.out.println("\n\n\n" + Helper.repString("+=", 50) + "\nTEST OF POSTERIOR PROBABILITY CONSTRUCTION\n");
//			Defaults defaults = Defaults.mainDefaults;
//			MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
//			mdpBuilder.setDefaults(defaults);
//			MarkovDecisionProcess mdp = new MarkovDecisionProcess(mdpBuilder);
//			System.out.println(mdp);
//			
//			EnvironmentBuilderFull ebf = defaults.startingEnvironmentPopulationFull.get(0);
//			ebf.setMDPBuilder(mdpBuilder);
//			ebf.update();
//			Environment e = ebf.toEnvironment();
//			System.out.println(e);
//			
//			PosteriorProbability pt = new PosteriorProbability(mdp, e, new Integer[] {0,0});
//			pt.run();
//			System.out.println("Posterior probability without cues: " + pt);
//			
//			PosteriorProbability pt2 = new PosteriorProbability(mdp, e, new Integer[] {1,0});
//			pt2.run();
//			System.out.println("Posterior probability 1 negative cues: " + pt2);
//			
//			PosteriorProbability pt3 = new PosteriorProbability(mdp, e, new Integer[] {0,1});
//			pt3.run();
//			System.out.println("Posterior probability 1 positive cues: " + pt3);
//			
//			PosteriorProbability pt4 = new PosteriorProbability(mdp, e, new Integer[] {5,0});
//			pt4.run();
//			System.out.println("Posterior probability 5 negative cues: " + pt4);
//			
//			PosteriorProbability pt5 = new PosteriorProbability(mdp, e, new Integer[] {0,5});
//			pt5.run();
//			System.out.println("Posterior probability 5 positive cues: " + pt5);
//			
//			PosteriorProbability pt6 = new PosteriorProbability(mdp, e, new Integer[] {10,10});
//			pt6.run();
//			System.out.println("Posterior probability 10 positive & 10 negative cues: " + pt6);
//			
//
//		} catch (Exception e) {e.printStackTrace();}
//	}
//	
	
//	@Test
//	void posteriorProbabilityTableConstruction() {
//		try {
//			System.out.println("\n\n\n" + Helper.repString("+=", 50) + "\nTEST OF POSTERIOR PROBABILITY ~~TABLE~~ CONSTRUCTION\n");
//			Defaults defaults = Defaults.mainDefaults;
//			System.out.println(defaults);
//			
//			MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
//			mdpBuilder.setDefaults(defaults);
//			MarkovDecisionProcess mdp = new MarkovDecisionProcess(mdpBuilder);
//			
//			EnvironmentBuilderFull ebf = defaults.startingEnvironmentPopulationFull.get(0);
//			ebf.setMDPBuilder(mdpBuilder);
//			ebf.update();
//			Environment e = ebf.toEnvironment();
//			
//			PosteriorProbabilityTable ppt = new PosteriorProbabilityTable(mdp, e);
//			System.out.println(ppt);
//			
//		} catch (Exception e) {e.printStackTrace();}
//	}
	
	
/*	@Test
	void decisionNodeConstruction() {
		try {
			System.out.println("\n\n\n" + Helper.repString("+=", 100) + "\nTEST OF DECISION NODE CONSTRUCTION\n");
			Defaults defaults = Defaults.mainDefaults;
			MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
			mdpBuilder.setDefaults(defaults);
			MarkovDecisionProcess mdp = new MarkovDecisionProcess(mdpBuilder);

			EnvironmentBuilderFull ebf = defaults.startingEnvironmentPopulationFull.get(0);
			ebf.setMDPBuilder(mdpBuilder);
			ebf.update();
			Environment e = ebf.toEnvironment();

			PosteriorProbabilityTable ppt = new PosteriorProbabilityTable(mdp, e);
			DecisionNode dn = new DecisionNode(mdp, ppt, e, new Integer[] {0,0}, new DecimalNumber(10),  new DecimalNumber(1));
			System.out.println(     "Root node:       " + dn);

			ArrayList<DecisionNode> children = dn.createChildNodes();
			for (DecisionNode c: children) 
				System.out.println( "Child Node:      " + c);

			ArrayList<DecisionNode> grandChildren = new ArrayList<>();
			for (DecisionNode c: children)
				grandChildren.addAll(c.createChildNodes());

			for (DecisionNode gc: grandChildren) 
				System.out.println( "Grandchild Node: " + gc);
		} catch (Exception e) {e.printStackTrace();}
	}*/

	
	@Test
	void decisionTreeConstruction() {
		try {
			System.out.println("\n\n\n" + Helper.repString("+=", 100) + "\nTEST OF DECISION ~~~TREE~~~ CONSTRUCTION\n");
			Defaults defaults = Defaults.mainDefaults;
			
			MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
			mdpBuilder.setDefaults(defaults);
			MarkovDecisionProcess mdp = new MarkovDecisionProcess(mdpBuilder);
			System.out.println(mdp);
			
			EnvironmentBuilderFull ebf = defaults.startingEnvironmentPopulationFull.get(0);
			ebf.setMDPBuilder(mdpBuilder);
			ebf.update();
			Environment e = ebf.toEnvironment();
			System.out.println(e);

			EstimationBuilder eb = new EstimationBuilder();
			eb.loadDefaults(defaults);
			EstimationParameters estimationParameters = new EstimationParameters(eb, null);
			
			PosteriorProbabilityTable ppt = new PosteriorProbabilityTable(mdp, e);
			System.out.println(ppt);
			
			ValueIteratorValueFunction vf = new ValueIteratorValueFunction(mdp, e, new DecimalNumber(0), Optimizer.Lossy);
			for (DecimalNumber b: vf.getBudgets())
				if (!b.equals(0))
					vf.setExpectedFutureValueOutcome(b, new DecimalNumber((int)(Math.random()*10)));
			//System.out.println(vf);
			
			DecisionTree dt = new DecisionTree(mdp, e, estimationParameters, ppt,  new DecimalNumber(5));
			dt.forwardPass();
			//System.out.println("\n" + Helper.repString("-", 100 ) + "\nForward pass: \n" + dt);
			
			dt.backwardPass(false, vf, false);
			//System.out.println("\n" + Helper.repString("-", 100 ) + "\nBackward pass: \n" + dt);
			
			dt.forwardPruningPass();
			//System.out.println("\n" + Helper.repString("-", 100 ) + "\nForward pruning pass: \n" + dt);
			
			System.err.println("\n\nTREE: \n\n" + dt);
		} catch (Exception e) {e.printStackTrace();}
	} 
	/*
		@Test
		void valueIterator() 
		{
			try {
				System.out.println("\n\n\n" + Helper.repString("+=", 100) + "\nTEST OF VALUE ITERATOR\n");

				Defaults defaults = Defaults.mainDefaults;
				MarkovDecisionProcessBuilder mdpBuilder = new MarkovDecisionProcessBuilder();
				mdpBuilder.setDefaults(defaults);
				MarkovDecisionProcess mdp = new MarkovDecisionProcess(mdpBuilder);
				System.out.println(mdp);
				
				EnvironmentBuilderFull ebf = defaults.startingEnvironmentPopulationFull.get(0);
				ebf.setMDPBuilder(mdpBuilder);
				ebf.update();
				Environment e = ebf.toEnvironment();
				System.out.println(e);
	
				EstimationBuilder eb = new EstimationBuilder();
				eb.loadDefaults(defaults);
				EstimationParameters estimationParameters = new EstimationParameters(eb, eb.getOutputFolder());
				
				ValueIterator vi = new ValueIterator(null, mdp, ebf, estimationParameters, null, 0);
			
				vi.run();
				
				DecimalNumber startingBudget = new DecimalNumber(1);
				PosteriorProbabilityTable ppt = new PosteriorProbabilityTable(mdp, e);
				DecisionTree dt = new DecisionTree(mdp, e, estimationParameters, ppt, startingBudget);
				dt.forwardPass();	
				dt.backwardPass(false, vi.valueFunction, true);
				dt.forwardPruningPass();
				System.out.println("\n" + Helper.repString("-", 100 ) + "\nForward pruning pass: \n" + dt);
				
				System.err.println("{cues sampled}  = " + dt.getCuesSampled());
				System.err.println("E[cues sampled] = "  + dt.getExpectedCuesSampled());
				System.err.println("Value function: " + vi.valueFunction);
	//	System.out.println(vi.optimalCuesToSample);
				
				PosteriorProbabilityTable ppt = new PosteriorProbabilityTable(mdp, e);
				DecisionTree dt = new DecisionTree(mdp, e, ppt, new DecimalNumber(2));
				dt.forwardPass();
				dt.backwardPass(false, vi.valueFunction);
			//dt.forwardPruningPass();
	
				System.out.println(dt);
				
				
				DecisionTree dt2 = new DecisionTree(mdp, e, ppt, new DecimalNumber(20));
				dt2.forwardPass();
				dt2.backwardPass(true, vi.valueFunction);
				//dt.forwardPruningPass();
	
				System.err.println("Based on value function: " + dt2);
				//System.err.println("\n\nDeltas:");
				//for(int i = 0;  i < vi.deltas.size(); i++)
				//	System.err.println(vi.deltas.get(i).getValue());
				
				}
			catch (Exception e) { e.printStackTrace();}
			
		}*/

}
