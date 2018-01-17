package networkcues;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cern.jet.random.ChiSquare;
import cern.jet.random.Uniform;
import repast.simphony.context.Context;
import repast.simphony.context.space.graph.NetworkFactory;
import repast.simphony.context.space.graph.NetworkFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.graph.Network;
import repast.simphony.space.graph.ShortestPath;
import repast.simphony.util.collections.IndexedIterable;

public class NetworkCuesBuilder implements ContextBuilder<Object> {
	
	public static final int COUNT_AGENT = 200;
	public static final int COUNT_GROUPS = 10;
	public static final int LEN_SPACE = 50;
	public static final int LEN_FAMILY = 8;
	public static final double LEN_NEIGHBORHOOD = 5;

	@Override
	public Context<Object> build(Context<Object> context) {
		context.setId("networkcues");
		
		// Create a controller
		AgentController agentController = new AgentController(null);

		// Create a network factory
		NetworkFactory networkFactory = NetworkFactoryFinder.createNetworkFactory(null);
		
		// Define a communication projection
		Network <Object> commNetwork = networkFactory.createNetwork("communication network", context, false, new CommunicationEdgeCreator<Object>());

		// Define a trade network projection
		Network <Object> tradeNetwork = networkFactory.createNetwork("trade network", context, true, new TradeEdgeCreator<Object>());

		// Define a kinship network projection
		Network <Object> kinNetwork = networkFactory.createNetwork("kinship network", context, false);
		
		// Create a set of agents
		for (int i = 0; i < COUNT_AGENT; i++) {
			NdPoint location = new NdPoint(RandomHelper.nextDouble() * LEN_SPACE, RandomHelper.nextDouble() * LEN_SPACE);
			Agent x = new Agent(i, location, commNetwork, tradeNetwork, agentController);
			context.add(x);
		}

		// Populate the communication network
		this.buildCommunicationNetwork(context, commNetwork, kinNetwork);
		
		return context;
	}

	private void buildCommunicationNetwork(Context<Object> context, Network<Object> commNetwork, Network <Object> kinNetwork) {
		

		// Build a kinship path
		Network<Object> kinshipNetwork = buildKinShipNetwork(context, kinNetwork); 
		ShortestPath <Object> paths = new ShortestPath <Object>(kinshipNetwork); 

		// List all the agents from the context
		IndexedIterable<Object> collection = context.getObjects(Agent.class);

		// Some debug output
		HashMap<Double, Integer> counts1 = new HashMap <Double, Integer>();
		HashMap<Double, Integer> counts2 = new HashMap <Double, Integer>();
		HashMap<Double, Integer> counts3 = new HashMap <Double, Integer>();
		
		for (Object current : collection) {
			
			// Get the current agent
			Agent currentAgent = (Agent) current;

			for (Object other : collection) {
				Agent otherAgent = (Agent) other;
				
				// Create an edge between the current and agent and every other agent
				if (currentAgent != otherAgent && commNetwork.getEdge(otherAgent, currentAgent) == null) {
					
					// Create the edge
					CommunicationEdge<Object> edge = (CommunicationEdge<Object>) commNetwork.addEdge(currentAgent, otherAgent);
					
					// Define the kinship
					edge.setKinship(paths.getPathLength(currentAgent, otherAgent));
					
					// Determine the neighborhood distance and neighborhood size
					double distance = currentAgent.getDistanceTo(otherAgent);
					if (distance < NetworkCuesBuilder.LEN_NEIGHBORHOOD) {
						edge.setNormalizedDistance(1 - (distance / NetworkCuesBuilder.LEN_NEIGHBORHOOD));
						currentAgent.neighborhoodSize++;
						otherAgent.neighborhoodSize++;
					} else {
						edge.setNormalizedDistance((NetworkCuesBuilder.LEN_NEIGHBORHOOD - distance) / (NetworkCuesBuilder.LEN_SPACE - NetworkCuesBuilder.LEN_NEIGHBORHOOD));
					}
					
					// Determine the group relationship
					edge.setSameGroup((currentAgent.profile.getGroupID() == otherAgent.profile.getGroupID()));
					
					// Some debugging data
					double possibleKey = edge.getKinship();
					if (counts1.containsKey(possibleKey)) {
						counts1.put(possibleKey, counts1.get(possibleKey) + 1);
					} else {
						counts1.put(possibleKey, 1);
					}

					possibleKey = (double) (Math.round(edge.getNormalizedDistance() * 4)) / 4d;
					if (counts2.containsKey(possibleKey)) {
						counts2.put(possibleKey, counts2.get(possibleKey) + 1);
					} else {
						counts2.put(possibleKey, 1);
					}
				}
			}
		}
		
		for (Object current : collection) {
			// Get the current agent
			Agent countAgent = (Agent) current;
			double neighborKey = (double) countAgent.neighborhoodSize;
			if (counts3.containsKey(neighborKey)) {
				counts3.put(neighborKey, counts3.get(neighborKey) + 1);
			} else {
				counts3.put(neighborKey, 1);
			}
		}
		
	    System.out.println("----------kinship------------");
	    Iterator<Map.Entry<Double, Integer> > it1 = counts1.entrySet().iterator();
	    while (it1.hasNext()) {
	        Map.Entry<Double, Integer> pair = (Map.Entry<Double, Integer>)it1.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        it1.remove(); // avoids a ConcurrentModificationException
	    }
	    System.out.println("----------distance------------");
	    Iterator<Map.Entry<Double, Integer> > it2 = counts2.entrySet().iterator();
	    while (it2.hasNext()) {
	        Map.Entry<Double, Integer> pair = (Map.Entry<Double, Integer>)it2.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        it2.remove(); // avoids a ConcurrentModificationException
	    }
	    System.out.println("-----------neighborhood-----------");
	    Iterator<Map.Entry<Double, Integer> > it3 = counts3.entrySet().iterator();
	    while (it3.hasNext()) {
	        Map.Entry<Double, Integer> pair = (Map.Entry<Double, Integer>)it3.next();
	        System.out.println(pair.getKey() + " = " + pair.getValue());
	        it3.remove(); // avoids a ConcurrentModificationException
	    }

	}
	
	private Network<Object> buildKinShipNetwork (Context<Object> context, Network <Object> kinNetwork) {
		
		int minFamilySize = NetworkCuesBuilder.LEN_FAMILY * 3;
		int sumConnections =  0;
		Uniform randomUniform = RandomHelper.createUniform();
		ChiSquare randomStart = RandomHelper.createChiSquare(NetworkCuesBuilder.LEN_FAMILY / 2);
		ChiSquare randomMaxim = RandomHelper.createChiSquare(NetworkCuesBuilder.LEN_FAMILY);

		// Create a kinship network
		Network <Object> kinshipNetwork = kinNetwork;

		ArrayList<SimpleEntry<Integer, Integer>> possibleKin = new ArrayList<SimpleEntry<Integer, Integer>> ();
		ArrayList<Integer> maxConnections = new ArrayList<Integer> ();
		
		// list all the agents from the context
		IndexedIterable<Object> collection = context.getObjects(Agent.class);
		int numberOfAgents = collection.size();
		
		// Calculate the maximum number of connections for each node
		for (int i = 0; i < numberOfAgents; i++) {
			maxConnections.add((int) (randomMaxim.nextInt() * 3));
		}

		for (int i =0; i < numberOfAgents; i++) {
			
			// Create a new family
			boolean startNewFamily = RandomHelper.nextDouble() > 0.75;
			if (i < minFamilySize || (startNewFamily && i + minFamilySize < numberOfAgents)) {
				System.out.println("Create a new network");
				
				// Create a ring network
				kinshipNetwork.addEdge(collection.get(i), collection.get(i + minFamilySize - 1));
				for (int j = i; j < i + minFamilySize - 1; j++) {
					kinshipNetwork.addEdge(collection.get(j), collection.get(j + 1));
				}
				
				// Second set of ring network connections
				for (int j = i; j < i + (minFamilySize / 2); j++) {
					kinshipNetwork.addEdge(collection.get(j), collection.get(j + (minFamilySize / 2)));
				}
				
				// update the newly created nodes to the list of possible kin
				for (int j = i; j < i + minFamilySize; j++) {
					int nodeConnections = kinshipNetwork.getDegree(collection.get(j));
					
					if(nodeConnections < maxConnections.get(i)) {
						possibleKin.add(new SimpleEntry<Integer, Integer> (j, nodeConnections));
						sumConnections += nodeConnections + 1;
					}
				}

				i += minFamilySize;
				
				if (i >= numberOfAgents ) {
					break;					
				}
			}
			
			// Add the agent to one of the existing families
			SimpleEntry<Integer, Integer> kin = new SimpleEntry<Integer, Integer> (i, 0);
			int newConnections = randomStart.nextInt();
			
//			System.out.println(i + ". Add " + newConnections + " (" + maxConnections.get(i) + ") | " + possibleKin.size() + " (" + sumConnections + ")");
			for (int j = 0; j < newConnections; j++) {
				int count = 0;
				int numberOfPossibleKin = possibleKin.size();
				int choice = randomUniform.nextIntFromTo(0, sumConnections);
				
//				System.out.println("> " + choice + " | " + sumConnections);
				for(int k = 0; k < numberOfPossibleKin && count < sumConnections; k++) {
					count += possibleKin.get(k).getValue();
					if (choice < count) {
						// Connect to this node
						kinshipNetwork.addEdge(collection.get(i), collection.get(possibleKin.get(k).getKey()));
						sumConnections++;
						
						// Update the number of connections
						kin.setValue(kin.getValue() + 1);
						possibleKin.get(k).setValue(possibleKin.get(k).getValue() + 1);
						if(possibleKin.get(k).getValue() >= maxConnections.get(possibleKin.get(k).getKey())) {
							sumConnections -= possibleKin.get(k).getValue();
							possibleKin.remove(k);
							k--;
						}
						
						// escape the loop
						break;
					}
				}
			}
			
			// Add this agent to the possible kin for the next agent
			possibleKin.add(kin);
		}
		return kinNetwork;
	}

}
