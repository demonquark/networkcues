package networkcues;

import java.util.HashMap;

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
	
	protected int id;
	protected int neighborhoodSize;
	protected int numOfRatingsReceived;
	protected double averageRating;
	protected TradeEdge.TradeResult lastTradeResult;
	public Profile profile;
	public Good selling;
	public Good [] buying;

	public NdPoint location;
	private Network <Object> commNetwork;
	private Network <Object> tradeNetwork;
	private HashMap<Integer, Integer> groups;
	private HashMap<Integer, Double> groupAffinity;

	
	public Agent(int id, NdPoint location, Network <Object> commNetwork, Network <Object> tradeNetwork, 
			HashMap<Integer, Integer> groups, HashMap<Integer, Double> groupAffinity) {
		this.id = id;
		this.neighborhoodSize = 0;
		this.location = location;
		this.commNetwork = commNetwork;
		this.tradeNetwork = tradeNetwork;
		this.groups = groups;
		this.groupAffinity = groupAffinity;
		this.numOfRatingsReceived = 0;
		this.averageRating = 0;
		this.lastTradeResult = TradeResult.CC;
		
		this.profile = new Profile();
		
		// Add the agent to a group
		if (this.groups.containsKey(this.profile.getGroupID())) {
			this.groups.put(this.profile.getGroupID(), this.groups.get(this.profile.getGroupID()) + 1);
		} else {
			this.groups.put(this.profile.getGroupID(), 1);
			this.groupAffinity.put(this.profile.getGroupID(), RandomHelper.nextDouble());
		}
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
				
				// Make sure that other can buy this' selling good
				boolean wantsWhatImSelling = false;
				for (int i = 0; i < other.buying.length; i++) {
					if(this.selling.type == other.buying[i].type) {
						wantsWhatImSelling = true;
						break;
					}
				}
				
				// Make sure that this can buy other's selling good
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
					}
				}
			}
		
			// Establish a trade connection between the agents
			if (bestPartner != null) {
				tradeNetwork.addEdge(this, bestPartner);
			}
		}
	}

//	@ScheduledMethod(start = 3, interval = 3)
//	public void makeTrade() {
//		Iterable<RepastEdge<Object>> collection = this.tradeNetwork.getEdges(this);
//		for (Object current : collection) {
//		// Get the current agent
//			@SuppressWarnings("unchecked")
//			TradeEdge<Object> edge = (TradeEdge<Object>) current;
//			if (!edge.isTradeComplete()) {
//				edge.makeTrade();
//				
//				Object other = edge.getSource() == this ? edge.getTarget() : edge.getSource();
//				RepastEdge<Object> commEdge = this.commNetwork.getEdge(this, other);
//				
//				if (commEdge != null && CommunicationEdge.class.isInstance(commEdge)) {
//					((CommunicationEdge<Object>) commEdge).addTrade();
//				}
//			}
//		}
//	}

	public void completeTrade(Agent agent2, TradeEdge.TradeResult tradeResult) {
		
		// Record that the trade happened
		RepastEdge<Object> edge = this.commNetwork.getEdge(this, agent2);
		if (edge != null && CommunicationEdge.class.isInstance(edge)) {
			((CommunicationEdge<Object>) edge).addTrade();
			((CommunicationEdge<Object>) edge).setLastTradeResult(tradeResult);;
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

	public void rate(double singleRating) {
		this.averageRating = ((this.averageRating * this.numOfRatingsReceived ) + singleRating) / (this.numOfRatingsReceived + 1);
		this.numOfRatingsReceived++;
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
		affinity = affinity * this.groupAffinity.get(this.profile.getGroupID());

		return affinity;
	}

	public TradeEdge.TradeResult getLastTradeResultWithAgent(Agent agent2){

		TradeEdge.TradeResult tradeResult = TradeEdge.TradeResult.CC;
		RepastEdge<Object> edge = this.commNetwork.getEdge(this, agent2);
	
		if (edge != null && CommunicationEdge.class.isInstance(edge) ) {
			tradeResult = ((CommunicationEdge<Object>) edge).getLastTradeResult();
		}
		
		return tradeResult;
		
	}

}
