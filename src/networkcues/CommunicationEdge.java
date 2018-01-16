package networkcues;

import repast.simphony.space.graph.RepastEdge;

public class CommunicationEdge<T> extends RepastEdge<T> {

	private double weight;
	private double kinship;
	private double normalizedDistance;
	private int numberOfTrades;
	private boolean sameGroup;
	private TradeEdge.TradeResult lastTradeResult;
	
	public CommunicationEdge(T source, T target, boolean isDirected, double weight) {
		this.source = source;
		this.target = target;
		this.directed = isDirected;
		this.weight = weight;
		this.normalizedDistance = 0;
		this.kinship = 0;
		this.numberOfTrades = 0;
		this.lastTradeResult = TradeEdge.TradeResult.CC;
	}
	
	public void setKinship(double kinship) {
		this.kinship = kinship;
	}
	
	public void setNormalizedDistance(double normalizedDistance) {
		this.normalizedDistance = normalizedDistance;
	}
	
	public void setSameGroup(boolean sameGroup) {
		this.sameGroup = sameGroup;
	}

	public void setLastTradeResult(Agent sourceAgent, TradeEdge.TradeResult tradeResult) {
		switch(tradeResult) {
			case CD:
				this.lastTradeResult = (sourceAgent == this.source) ? tradeResult : TradeEdge.TradeResult.DC;
			case DC:
				this.lastTradeResult = (sourceAgent == this.source) ? tradeResult : TradeEdge.TradeResult.CD;
			default:
				this.lastTradeResult = tradeResult;
		}
	}

	public void addTrade() {
		this.numberOfTrades++;
	}
	
	public double getKinship() {
		return this.kinship;
	}
	
	public double getWeight() {
		return weight;
	}
	
	public int getNumberOfTrades() {
		return numberOfTrades / 2;
	}

	public double getNormalizedDistance() {
		return normalizedDistance;
	}
	
	public TradeEdge.TradeResult getLastTradeResult(Agent sourceAgent) {
		switch(this.lastTradeResult) {
		case CD:
			return (sourceAgent == this.source) ? this.lastTradeResult : TradeEdge.TradeResult.DC;
		case DC:
			return (sourceAgent == this.source) ? this.lastTradeResult : TradeEdge.TradeResult.CD;
		default:
			return this.lastTradeResult;
		}
	}
	
	public boolean areNeigbhors() {
		return normalizedDistance > 0;
	}
	
	public boolean inSameGroup() {
		return this.sameGroup;
	}
}
