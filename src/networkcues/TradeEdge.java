package networkcues;

import java.util.ArrayList;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;

public class TradeEdge<T> extends RepastEdge<T> {

	public static enum TradeResult { CC, CD, DC, DD };
	public static final double STRATEGY_MULTIPLIER = 1.5;

	public double ratingReliability;
	public ArrayList<Double> ratings;
	private double weight;
	private double sourcePayOffMatrix [];
	private double targetPayOffMatrix [];
	
	public TradeEdge(T source, T target, boolean directed, double weight) {
		this.source = source;
		this.target = target;
		this.directed = directed;
		this.weight = weight;
		
		// Calculate the pay off matrix
		if (Agent.class.isInstance(source) && Agent.class.isInstance(target)) { 
			this.sourcePayOffMatrix = calculatePayOffMatrix((Agent)source, (Agent)target);
			this.targetPayOffMatrix = calculatePayOffMatrix((Agent)target, (Agent)source);			
		} else {
			this.sourcePayOffMatrix = new double [4];
			this.targetPayOffMatrix = new double [4];			
		}
		
		// Make the trade
		this.makeTrade();

	}

	
	private double [] calculatePayOffMatrix(Agent agent1, Agent agent2) {
		
		// Calculate the default pay off matrices
		double b_c = agent1.selling.benefit - agent1.selling.cost;
		double c = 0 - agent1.selling.cost;
		double b = agent1.selling.benefit;

		// Network reciprocity (neighborhood)
		if(agent1.profile.useNetworkReciprocity()) {
			// Get the number of neighbors and the distance to the agent
			int k = agent1.neighborhoodSize;
			double normalizedDistance = agent1.getNormalizedDistanceTo(agent2);
			
			// Calculate H
			double h = k > 2 ? ((b_c) * k + 2 * c) / ((k + 1) * (k - 2)) : 1;
			
			// Update the cost and benefit
			b += h * normalizedDistance;
			c += h * normalizedDistance;			
		}
		
		// Kinship selection
		if(agent1.profile.useKinshipSelection()) {
			// Get the kinship
			double r = agent1.getKinshipCoefficientTo(agent2);
			
			// Update the cost and benefit
			b += r * c;
			c += r * b;
			b_c += r * b_c;
		}
		
		// Group selection
		if(agent1.profile.useGroupSelection()) {
			// Get the kinship
			double a = agent1.getGroupAffinityWith(agent2);
			
			// Update the cost and benefit
			c += a * b_c;
			b_c += a * b_c;
		}
		
		// Indirect reciprocity
		if(agent1.profile.useIndirectReciprocity()) {
			// Get the reputation of the other agent
			double q = agent1.getReputationOf(agent2);
			
			// Update the cost and benefit
			b = b * (1 - q);
			c = c * (1 - q);			
		}

		// Update the payoffMatrix
		double [] payoffMatrix = new double [4]; 
		payoffMatrix[0] = b_c;
		payoffMatrix[1] = c;
		payoffMatrix[2] = b;
		payoffMatrix[3] = 0;
		
		return payoffMatrix;
		

		// Kinship selection
		// this.kinship = (this is r) GET FROM kinship network 
		
		// Indirect reciprocity
		// this.rating = (this is q) GET FROM partner's ratings
		// this.ratingReliability = ratings.size() / NetworkCuesBuilder.COUNT_AGENT;
		
		// Network reciprocity (neighborhood)
		// this.neighborhoodsize =  (this is k) GET FROM communication network
		// this.neighbornormalizeddistance = GET FROM communication network (i.e. k)
		// this.neighborlyness = (this is H) GET FROM formula
		
		// Group selection
		// this.membersofsamegroup = True/False GET FROM partner's groupID
		// this.groupsize = GET FROM context (?)
		// this.numberofGroups = GET FROM context (?)
		
		// 1. Determine from profile which mechanisms the agent wants to use
		// 2. Calculate the pay off matrix using the chosen mechanisms
		// 3. Subtract cost from Defection and Add cost to Cooperation 
		// 4. Determine from profile which strategy the agent prefers
		// 5. Adjust the pay off matrix with strategy preference
		// 6. Make decision based on Greedy mentality (i.e. choice with highest average return)
	}

	public String[] chooseAction(Agent a, double [] payoffMatrix, TradeEdge.TradeResult lastTradeResult) {
		String [] logOutput = new String [5];
		
		if(a != null && payoffMatrix != null && payoffMatrix.length == 4) {

			// Determine the expected return for the agent
			double a_c1 = (payoffMatrix[0] + payoffMatrix[1] + a.selling.cost) / 2;
			double a_d1 = (payoffMatrix[2] + payoffMatrix[3] - a.selling.cost) / 2;

			// Apply the strategy preference to the expected return
			double a_c2 = a_c1;
			double a_d2 = a_d1;
			
			switch(a.profile.getStrategy()) {
				case COOPERATE:
					a_c2 = a_c1 > 0 ? a_c1 * TradeEdge.STRATEGY_MULTIPLIER : a_c1 / TradeEdge.STRATEGY_MULTIPLIER;
					break;
				case DEFECT:
					a_d2 = a_d1 > 0 ? a_d1 * TradeEdge.STRATEGY_MULTIPLIER : a_d1 / TradeEdge.STRATEGY_MULTIPLIER;
					break;
				case TITFORTAT:
					if (lastTradeResult == TradeEdge.TradeResult.CC || lastTradeResult == TradeEdge.TradeResult.DC) {
						a_c2 = a_c1 > 0 ? a_c1 * TradeEdge.STRATEGY_MULTIPLIER : a_c1 / TradeEdge.STRATEGY_MULTIPLIER;						
					} else {
						a_d2 = a_d1 > 0 ? a_d1 * TradeEdge.STRATEGY_MULTIPLIER : a_d1 / TradeEdge.STRATEGY_MULTIPLIER;						
					}
				default:
					break;
			}
			
			// Determine whether or not to cooperate based on Greedy mentality (i.e. choose highest expected return)
			boolean a_cooperate = (a_c2 > a_d2 ? true : a_c2 == a_d2 ? RandomHelper.nextDouble() >= 0.5 : false);

			// Log output
			String a_profile = (a.profile.useNetworkReciprocity() ? "1": "0");
			a_profile += (a.profile.useKinshipSelection() ? "1": "0");
			a_profile += (a.profile.useGroupSelection() ? "1": "0");
			a_profile += (a.profile.useIndirectReciprocity() ? "1": "0"); 
			a_profile += a.profile.getStrategy() == Profile.Strategy.COOPERATE ? "C" : a.profile.getStrategy() == Profile.Strategy.DEFECT ? "D" : a.profile.getStrategy() == Profile.Strategy.TITFORTAT ? "T" : "N";
			
			String matrix = "[" + String.format("%.2f", payoffMatrix[0]) + "|" + String.format("%.2f", payoffMatrix[1]) + "|" + String.format("%.2f", payoffMatrix[2]) + "|" + String.format("%.2f", payoffMatrix[3]) + "]";
			String expected1 =  String.format("%.2f", a_c1) + "|" + String.format("%.2f", a_d1);
			String expected2 =  String.format("%.2f", a_c2) + "|" + String.format("%.2f", a_d2);
			
			logOutput[0] = String.format("% 3d", a.id) + " " + a_profile;
			logOutput[1] = matrix;
			logOutput[2] = expected1;
			logOutput[3] = expected2;
			logOutput[4] = a_cooperate ? "C" : "D";
		}
		
		return logOutput;
	} 
	
	public void makeTrade() {
		// Calculate the pay off matrix
		if (Agent.class.isInstance(this.source) && Agent.class.isInstance(this.target)) { 

			Agent a = (Agent)this.source;
			Agent b = (Agent)this.target;
			
			String [] logOutput1 = this.chooseAction(a, this.sourcePayOffMatrix, a.getLastTradeResultWithAgent(b));
			String [] logOutput2 = this.chooseAction(b, this.targetPayOffMatrix, b.getLastTradeResultWithAgent(a));
			
			System.out.println(logOutput1[0] + "<->" + logOutput2[0] + logOutput1[1] + "~" + logOutput2[1] + " => " + logOutput1[2] + "~" + logOutput2[2]);
			System.out.println(" => " + logOutput1[3] + "~" + logOutput2[3] + " => " + logOutput1[4] + "~" + logOutput2[4]);
			
		} else {
			System.out.println("At least one of the nodes is not an agent.");
		}
	}
	
	public double getWeight() {
		return this.weight;
	}
}
