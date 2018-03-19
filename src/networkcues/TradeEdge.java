package networkcues;

import java.util.Arrays;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.graph.RepastEdge;

public class TradeEdge<T> extends RepastEdge<T> {

	public static enum TradeResult { CC, CD, DC, DD };
	public static final double STRATEGY_MULTIPLIER = 1.5;
	private double weight;
	private double [] boosts;
	
	public TradeEdge(T source, T target, boolean directed, double weight) {
		this.source = source;
		this.target = target;
		this.directed = directed;
		this.weight = weight;
		this.boosts = null;
		
		// Make the trade
		this.makeTrade();
		
	}
	
	private double [] calculatePayOffMatrix(Agent agent1, Agent agent2, boolean applyboosts) {
		
		// Get the boosts
		// If appliedBoosts is null, then this is source -> target, else it's target -> source
		double [] appliedBoosts = new double [4];
		if (applyboosts) {
			appliedBoosts = agent1.getBoosts(agent2);
			if (this.boosts == null) {
				this.boosts = new double [8];
				this.boosts[0] = appliedBoosts[0];
				this.boosts[1] = appliedBoosts[1];
				this.boosts[2] = appliedBoosts[2];
				this.boosts[3] = appliedBoosts[3];
			} else {
				this.boosts[4] = appliedBoosts[0];
				this.boosts[5] = appliedBoosts[1];
				this.boosts[6] = appliedBoosts[2];
				this.boosts[7] = appliedBoosts[3];			
			}			
		}
		
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
			double h = k > 2 ? ((b_c) * k + 2 * c) / ((k + 1) * (k - 2)) : b_c;
			
			if(h > 5) {
				h = 5;
			}

			// apply the boost
			normalizedDistance += 0.75 * appliedBoosts[0];
			normalizedDistance = normalizedDistance > 1 ? 1 : normalizedDistance;
			
			// Update the cost and benefit
			b -= h * normalizedDistance * (applyboosts ? 2 : 1);
			c += h * normalizedDistance * (applyboosts ? 2 : 1);
		}
		
		// Kinship selection
		if(agent1.profile.useKinshipSelection()) {
			// Get the kinship
			double r = agent1.getKinshipCoefficientTo(agent2);
			
			// apply the boost
			r += (r == 0 ? 0.2 : 0.5) * appliedBoosts[1];
			r = r > 1 ? 1 : r;
			
			// Update the cost and benefit
			b += r * c;
			c += r * b;
			b_c += r * b_c;
		}
		
		// Group selection
		if(agent1.profile.useGroupSelection()) {
			// Get the kinship
			double a = agent1.getGroupAffinityWith(agent2);
			
			// apply the boost
//			System.out.print("a: " + String.format("%.2f", a) +  ">" +  String.format("%.2f", a + (0.75 * appliedBoosts[2])) );
			a += 0.75 * appliedBoosts[2];
			a = a > 1 ? 1 : a;
//			System.out.println(">" + String.format("%.2f", a) + " ~ " + String.format("%.2f", (a *b_c)));

			// Update the cost and benefit
			c += a * b_c;
			b_c += a * b_c;
		}
		
		// Indirect reciprocity
		if(agent1.profile.useIndirectReciprocity()) {
			// Get the reputation of the other agent
			double q = agent1.getReputationOf(agent2);
			
			// apply the boost
//			System.out.print("q: " + String.format("%.2f", q) +  ">" +  String.format("%.2f", q + (0.75 * appliedBoosts[2])) );
			q += 0.75 * appliedBoosts[3];
			q = q > 1 ? 1 : q;
//			System.out.println(">" + String.format("%.2f", q) + " ~ " + String.format("%.2f", (1-q)));

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
	}

	public String[] chooseAction(Agent a, double [] payoffMatrix, TradeEdge.TradeResult lastTradeResult) {
		String [] logOutput = new String [5];
		
		if(a != null && payoffMatrix != null && payoffMatrix.length == 4) {

			// Determine the expected return for the agent
			double a_c1 = (payoffMatrix[0] + payoffMatrix[1] + (0.85 * a.selling.cost)) / 2;
			double a_d1 = (payoffMatrix[2] + payoffMatrix[3] - (0.85 * a.selling.cost)) / 2;

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
			boolean a_cooperate = (a_c2 > a_d2 ? true : a_c2 == a_d2 ? true : false);
//			boolean a_cooperate = (a_c2 > a_d2 ? true : a_c2 == a_d2 ? RandomHelper.nextDouble() > 0.55 : false);

			// Log output
			String a_profile = (a.profile.useNetworkReciprocity() ? "1": "0");
			a_profile += (a.profile.useKinshipSelection() ? "1": "0");
			a_profile += (a.profile.useGroupSelection() ? "1": "0");
			a_profile += (a.profile.useIndirectReciprocity() ? "1": "0"); 
			a_profile += a.profile.getStrategy() == Profile.Strategy.COOPERATE ? "C" : a.profile.getStrategy() == Profile.Strategy.DEFECT ? "D" : a.profile.getStrategy() == Profile.Strategy.TITFORTAT ? "T" : "N";
			
			String matrix = "[" + String.format("%3.2f", payoffMatrix[0]) + "|" + String.format("%.2f", payoffMatrix[1]) + "|" + String.format("%.2f", payoffMatrix[2]) + "|" + String.format("%.2f", payoffMatrix[3]) + "]";
			String expected1 =  String.format("%.2f", a_c1) + "|" + String.format("%.2f", a_d1);
			String expected2 =  String.format("%.2f", a_c2) + "|" + String.format("%.2f", a_d2);
			
			logOutput[0] = String.format("%03d", a.id) + "(" + String.format("%03d", a.profile.getGroupID()) + ") " + a_profile;
			logOutput[1] = a_cooperate ? "C" : "D";
			logOutput[2] = matrix;
			logOutput[3] = expected1;
			logOutput[4] = expected2;
		}
		
		return logOutput;
	} 
	
	public void makeTrade() {
		// Calculate the pay off matrix
		if (Agent.class.isInstance(this.source) && Agent.class.isInstance(this.target)) { 

			Agent a = (Agent)this.source;
			Agent b = (Agent)this.target;
			
			// Determine the node choices
			String [] logOutput1 = this.chooseAction(a, calculatePayOffMatrix(a, b, true), a.getLastTradeResultWithAgent(b));
			String [] logOutput2 = this.chooseAction(b, calculatePayOffMatrix(b, a, true), b.getLastTradeResultWithAgent(a));
			String [] logOutputWithoutBoost1 = this.chooseAction(a, calculatePayOffMatrix(a, b, false), a.getLastTradeResultWithAgent(b));
			String [] logOutputWithoutBoost2 = this.chooseAction(b, calculatePayOffMatrix(b, a, false), b.getLastTradeResultWithAgent(a));
						
			// Determine the trade results
			TradeEdge.TradeResult a_tradeResult = convertToTradeResult(logOutput1[1], logOutput2[1]);
			TradeEdge.TradeResult b_tradeResult = convertToTradeResult(logOutput2[1], logOutput1[1]);
			TradeEdge.TradeResult a_tradeResultWithoutBoost = convertToTradeResult(logOutputWithoutBoost1[1], logOutputWithoutBoost2[1]);
			TradeEdge.TradeResult b_tradeResultWithoutBoost = convertToTradeResult(logOutputWithoutBoost2[1], logOutputWithoutBoost1[1]);
			
			// Complete the trade
			// The first 4 elements of appliedBoosts are for source->target, the last 4 elements are for target->source
			a.completeTrade(b, a_tradeResult, a_tradeResultWithoutBoost, Arrays.copyOfRange(this.boosts, 0, 4));
			b.completeTrade(a, b_tradeResult, b_tradeResultWithoutBoost, Arrays.copyOfRange(this.boosts, 4, 8));
			
			// Log the node choice
//			if(AgentController.logging_record > AgentController.LOGGING_START) {
//				String boostString = "|";
//				for (int i=0; i < this.boosts.length; i++) { boostString += String.format("%.2f", boosts[i]) + "|" ; }
//				System.out.print(logOutput1[0] + " <-> " + logOutput2[0] + " " + logOutput1[1] + logOutput2[1] + " => " + logOutputWithoutBoost1[2] + "<>" + logOutputWithoutBoost2[2]);
//				System.out.print( " ~ " + logOutput1[2] + "<>" + logOutput2[2]);
//				System.out.println(" ~ " + logOutput1[3] + "<>" + logOutput2[3] + " ~ " + logOutput1[4] + "<>" + logOutput2[4] + " ~ " + boostString);
//				
//			}
			
		} else {
			System.out.println("One or more of the nodes is not an agent.");
		}
	}
	
	private TradeEdge.TradeResult convertToTradeResult(String a_action, String b_action){
		
		TradeEdge.TradeResult tradeResult;
		
		if("C".equals(a_action)) {
			tradeResult = "C".equals(b_action) ? TradeEdge.TradeResult.CC : TradeEdge.TradeResult.CD;
		} else {
			tradeResult = "C".equals(b_action) ? TradeEdge.TradeResult.DC : TradeEdge.TradeResult.DD;
		}
		
		return tradeResult;
		
	}
	
	public double getWeight() {
		return this.weight;
	}
}
