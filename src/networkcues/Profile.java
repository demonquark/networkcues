package networkcues;

import repast.simphony.random.RandomHelper;

public class Profile {

	public static enum Strategy { COOPERATE, DEFECT, TITFORTAT, NONE }; 
	public static final double TWO_THIRDS = 0.67; 
	private boolean kinship;
	private boolean network;
	private boolean indirect;
	private boolean group;
	private double certainty;
	private int groupID;
	private Strategy strategy;
	private double talkability;
	
	public Profile() {
		
		this.kinship = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.network = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.indirect = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.group = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.certainty = Profile.TWO_THIRDS;
		this.groupID = RandomHelper.nextIntFromTo(0, NetworkCuesBuilder.COUNT_GROUPS);
		this.strategy = this.chooseARandomStrategy();
		this.talkability = RandomHelper.nextDouble();
	}	
	
	private Strategy chooseARandomStrategy() {
		Strategy [] strategies = Strategy.values();
		return strategies[RandomHelper.nextIntFromTo(0, strategies.length - 1)];
	}

	public boolean useNetworkReciprocity() {
		return this.network;
	}

	public boolean useKinshipSelection() {
		return this.kinship;
	}

	public boolean useIndirectReciprocity() {
		return this.indirect;
	}

	public boolean useGroupSelection() {
		return this.group;
	}
	
	public double getCertainty () {
		return this.certainty;
	}
	
	public int getGroupID() {
		return this.groupID;
	}

	public Strategy getStrategy() {
		return this.strategy;
	}
	
	public double getTalkability() {
		return this.talkability;
	}

	public boolean shouldIRate() {
		
		return RandomHelper.nextDouble() > this.talkability;
	}
}
