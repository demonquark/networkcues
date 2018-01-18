package networkcues;

import networkcues.TradeEdge.TradeResult;
import repast.simphony.context.Context;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.RepastEdge;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;

public class Agent {
	
	private final static int LEN_BUYING = Good.LEN_TYPE / 2 + 1;
	
	public final int id;
	public final Profile profile;
	public final NdPoint location;

	protected int neighborhoodSize;
	protected int numOfRatingsReceived;
	protected double averageRating;
	protected Good selling;
	protected Good [] buying;
	protected TradeEdge.TradeResult lastTradeResult;

	private AgentController supervisor;
	private Network <Object> commNetwork;
	private Network <Object> tradeNetwork;
	
	public Agent(int id, NdPoint location, Network <Object> commNetwork, Network <Object> tradeNetwork, AgentController supervisor) {
		
		// fixed values (these will never change)
		this.id = id;
		this.profile = new Profile();
		this.location = location;

		// default initialization values
		this.neighborhoodSize = 0;
		this.numOfRatingsReceived = 0;
		this.averageRating = 0;
		this.lastTradeResult = TradeResult.CC;

		// References to the the context objects
		this.supervisor = supervisor;
		this.commNetwork = commNetwork;
		this.tradeNetwork = tradeNetwork;
		
		// Add the agent to a group
		supervisor.addAgentToGroup(this.profile.getGroupID());
	}
	
	@ScheduledMethod(start = 1, interval = 2)
	public void prepareForTrade() {
		
		// reset the trade network
		this.tradeNetwork.removeEdges();
		
		// sell a random type
		int good_type = RandomHelper.nextIntFromTo(0, Good.LEN_TYPE - 1);
		this.selling = new Good(good_type);
		
		// create a new array of items to buy
		this.buying = new Good [Agent.LEN_BUYING];
		
		// populate the buying array with random goods
		for (int i = 0; i < Agent.LEN_BUYING; i++) {
			
			// assume that it's not a valid type
			boolean valid_type = false;
			
			while (!valid_type){
				
				// choose a random type
				good_type = RandomHelper.nextIntFromTo(0, Good.LEN_TYPE - 1);

				// Don't add good of the same type of this.selling
				valid_type = good_type != this.selling.type;
				
				// Don't add good of the same type of this.buying
				for (int j = 0; valid_type && j < i - 1; j++) {
					valid_type = good_type != this.buying[j].type;
				}
				
				// add a new good to this.buy
				if (valid_type) {
					this.buying[i] = new Good(good_type);
				}
			}
		}
	}
	
	@ScheduledMethod(start = 2, interval = 2)
	public void findTradingPartner() {

		// Determine if the agent already has a partner
		@SuppressWarnings("unchecked")
		Context <Object> context = ContextUtils.getContext(this);
		int degree = tradeNetwork.getDegree(this);
		double distanceToPartner = Double.MAX_VALUE;
		
		Agent bestPartner = null;
		
		// If the agent has no partner find a partner
		if(degree == 0) {
			// list all the agents from the context
			IndexedIterable<Object> collection = context.getObjects(Agent.class);
			for (Object o : collection) {
				
				// Only consider agents who have not yet partnered
				Agent other = (Agent) o;
				if (tradeNetwork.getDegree(other) > 0) {
					continue;
				}
				
				// Make sure that other agent can buy this agent's selling good
				boolean wantsWhatImSelling = false;
				for (int i = 0; i < other.buying.length; i++) {
					if(this.selling.type == other.buying[i].type) {
						wantsWhatImSelling = true;
						break;
					}
				}
				
				// Make sure that this agent can buy other agent's selling good
				boolean hasWhatImBuying = false;
				for (int i = 0; i < this.buying.length; i++) {
					if(other.selling.type == this.buying[i].type) {
						hasWhatImBuying = true;
						break;
					}
				}
				
				// choose the closest valid partner
				if (wantsWhatImSelling && hasWhatImBuying) { 
					RepastEdge<Object> edge = commNetwork.getEdge(this, other);
					if (edge != null && edge.getWeight() < distanceToPartner) {
						distanceToPartner = edge.getWeight();
						bestPartner = other;			
						
						//Immediately exit for agents that don't care about the partner's distance
						if(!this.profile.shouldConsiderDistanceWhenPartnering())
							break;
					}
				}
			}
		
			// Establish a trade connection between the agents
			if (bestPartner != null) {
				tradeNetwork.addEdge(this, bestPartner);
			}
		}
	}

	public void completeTrade(Agent agent2, TradeEdge.TradeResult tradeResult) {
		
		// Record that the trade happened
		TradeEdge.TradeResult previousTradeResult = this.lastTradeResult;
		RepastEdge<Object> edge = this.commNetwork.getEdge(this, agent2);
		if (edge != null && CommunicationEdge.class.isInstance(edge)) {
			previousTradeResult = ((CommunicationEdge<Object>) edge).getLastTradeResult(this);
			((CommunicationEdge<Object>) edge).addTrade();
			((CommunicationEdge<Object>) edge).setLastTradeResult(this, tradeResult);;
		}

		// Update the last trade result
		this.lastTradeResult = tradeResult;
		
		// Rate the other agent
		if (this.profile.shouldIRate()) {
			double singleRating = 0;
			switch(tradeResult) {
				case CC:
				case DC:
					singleRating = 1;
					break;
				case CD:
				case DD:
					singleRating = -1;
					break;
				default:
					singleRating = 0;
					break;
			}
			agent2.rate(singleRating);
		}
		
		// Prep the trade data for training
		double [][] inputArray = new double[1][20];
		double [][] desiredOutputArray = new double[1][1];
		
		// profile features
		inputArray[0][0] = this.profile.profileFeature1;
		inputArray[0][1] = this.profile.profileFeature2;
		inputArray[0][2] = this.profile.profileFeature3;
		inputArray[0][3] = this.profile.profileFeature4;
		inputArray[0][4] = this.profile.profileFeature5;
		inputArray[0][5] = this.profile.profileFeature6;
		inputArray[0][6] = this.profile.profileFeature7;
		
		// partner profile features
		inputArray[0][7] = agent2.profile.profileFeature1;
		inputArray[0][8] = agent2.profile.profileFeature2;
		inputArray[0][9] = agent2.profile.profileFeature3;
		inputArray[0][10] = agent2.profile.profileFeature4;
		inputArray[0][11] = agent2.profile.profileFeature5;
		inputArray[0][12] = agent2.profile.profileFeature6;
		inputArray[0][13] = agent2.profile.profileFeature7;
		
		// signals sent during the trade
		inputArray[0][14] = this.profile.getGroupID() == agent2.profile.getGroupID() ? 1 : 0;
		inputArray[0][15] = previousTradeResult == TradeEdge.TradeResult.CC || previousTradeResult == TradeEdge.TradeResult.DC ? 1 : 0;

		// Applied boosts
		inputArray[0][16] = 0;
		inputArray[0][17] = 0;
		inputArray[0][18] = 0;
		inputArray[0][19] = 0;
		
		// 1 is cooperated and 0 is defected
		desiredOutputArray[0][0] = tradeResult == TradeEdge.TradeResult.CC || tradeResult == TradeEdge.TradeResult.CD ? 1 : 0;
		
		// Use the trade data to train the AgentController
//		this.supervisor.train(inputArray, desiredOutputArray);
//		this.supervisor.interrogate(inputArray, desiredOutputArray);
		this.supervisor.act(inputArray, desiredOutputArray);
		
	}

	public void rate(double singleRating) {
		this.averageRating = ((this.averageRating * this.numOfRatingsReceived ) + singleRating) / (this.numOfRatingsReceived + 1);
		this.numOfRatingsReceived++;
	}
	
	public double getKinshipCoefficientTo(Agent agent2) {
		double kinship = Double.POSITIVE_INFINITY;
		RepastEdge<Object> edge = this.commNetwork.getEdge(this, agent2);
		
		if (edge != null && CommunicationEdge.class.isInstance(edge)) {
			kinship = ((CommunicationEdge<Object>) edge).getKinship();
		}
		
		double kinshipCoeficient = 0;
		if (kinship < NetworkCuesBuilder.LEN_FAMILY) {
			kinshipCoeficient = 1 / Math.pow(2, kinship);
		}
		
		return kinshipCoeficient;
	}

	public double getNormalizedDistanceTo(Agent agent2) {
		double normalizedDistance = 0;
		RepastEdge<Object> edge = this.commNetwork.getEdge(this, agent2);
		
		if (edge != null && CommunicationEdge.class.isInstance(edge)) {
			normalizedDistance = ((CommunicationEdge<Object>) edge).getNormalizedDistance();
		}
		
		return normalizedDistance;
	}
	
	public double getReputationOf(Agent agent2) {
		
		double certainty = this.profile.getCertainty();
		RepastEdge<Object> edge = this.commNetwork.getEdge(this, agent2);
	
		if (edge != null && CommunicationEdge.class.isInstance(edge) && ((CommunicationEdge<Object>) edge).getNumberOfTrades() > 0) {
			certainty = 1;
		}
		
		return certainty * agent2.averageRating;
	}

	public double getGroupAffinityWith(Agent agent2) {
		
		double affinity = agent2.profile.getGroupID() == this.profile.getGroupID() ? 1 : -1;
		affinity = affinity * this.supervisor.groupAffinities.get(this.profile.getGroupID());

		return affinity;
	}

	public TradeEdge.TradeResult getLastTradeResultWithAgent(Agent agent2){

		TradeEdge.TradeResult tradeResult = TradeEdge.TradeResult.CC;
		RepastEdge<Object> edge = this.commNetwork.getEdge(this, agent2);
	
		if (edge != null && CommunicationEdge.class.isInstance(edge) ) {
			tradeResult = ((CommunicationEdge<Object>) edge).getLastTradeResult(this);
		}
		
		return tradeResult;
		
	}
	
	public double getDistanceTo(Agent agent2) {
		
		NdPoint point1 = this.location;
		NdPoint point2 = agent2.location;
		
		// Make sure we're working in the same space here
		if (point1.dimensionCount() != point2.dimensionCount()) return Double.NaN;
		
		double sum = 0;
		for (int i = 0, n = point1.dimensionCount(); i < n; i++) {
			
			// Get the difference in this dimension
			double absDiff = Math.abs(point1.getCoord(i) - point2.getCoord(i));
			
			// The world is continuous, so make sure the difference is within our dimension length
			while (absDiff > NetworkCuesBuilder.LEN_SPACE){
				absDiff -= NetworkCuesBuilder.LEN_SPACE;
			}
			
			// The world is continuous, so check the wrap around distance 
			if (absDiff > NetworkCuesBuilder.LEN_SPACE / 2){
				absDiff = NetworkCuesBuilder.LEN_SPACE - absDiff;
			}
			
			sum += absDiff * absDiff;
		}
		
		// return the Euclidian distance (i.e. the square root of the sum)
		return Math.sqrt(sum);
	}
	
}
