package networkcues;

import java.util.HashMap;

import repast.simphony.random.RandomHelper;

public class AgentController {

	protected HashMap<Integer, Integer> groups;
	protected HashMap<Integer, Double> groupAffinities;

	public AgentController() {
		groups = new HashMap<Integer, Integer> ();
		groupAffinities = new HashMap<Integer, Double> ();
		
	}

	public void addAgentToGroup(int groupID) {
		
		if (this.groups.containsKey(groupID)) {
			this.groups.put(groupID, this.groups.get(groupID) + 1);
		} else {
			this.groups.put(groupID, 1);
			this.groupAffinities.put(groupID, RandomHelper.nextDouble());
		}
		
	}
	
	public void removeAgentFromGroup(int groupID) {
		if (this.groups.containsKey(groupID)) {
			if (this.groups.get(groupID) <= 1) {
				this.groups.remove(groupID);
				this.groupAffinities.remove(groupID);
			} else {
				this.groups.put(groupID, this.groups.get(groupID) - 1);				
			}
		}
	}

}
