package networkcues;

import repast.simphony.space.graph.EdgeCreator;

public class TradeEdgeCreator<T> implements EdgeCreator<TradeEdge<T>, T> {

	@Override
	public Class<TradeEdge> getEdgeType() {
		return TradeEdge.class;
	}

	@Override
	public TradeEdge<T> createEdge(T source, T target, boolean isDirected, double weight) {
		
		return new TradeEdge<T>(source, target, isDirected, weight);
	}

}
