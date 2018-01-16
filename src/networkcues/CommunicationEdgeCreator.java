package networkcues;

import repast.simphony.space.graph.EdgeCreator;

public class CommunicationEdgeCreator<T> implements EdgeCreator<CommunicationEdge<T>, T> {

	@Override
	public Class<CommunicationEdge> getEdgeType() {
		return CommunicationEdge.class;
	}

	@Override
	public CommunicationEdge<T> createEdge(T source, T target, boolean isDirected, double weight) {
		
		return new CommunicationEdge<T>(source, target, isDirected, weight);
	}

}
