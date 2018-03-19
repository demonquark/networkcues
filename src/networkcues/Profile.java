package networkcues;

import repast.simphony.random.RandomHelper;

public class Profile {

	public static enum Strategy { COOPERATE, DEFECT, TITFORTAT, NONE }; 
	public static final double TWO_THIRDS = 0.67; 
	public static final double COOPERATION_PERCENTAGE = -1;

	// Strategy and characteristics that influence payOffMatrix
	private boolean kinship;
	private boolean network;
	private boolean indirect;
	private boolean group;
	private Strategy strategy;
	
	// Characteristics that don't influence payOffMatrix
	private int groupID;
	private double talkability;
	private double certainty;
	
	// Public profile features
	protected double profileFeature1;
	protected double profileFeature2;
	protected double profileFeature3;
	protected double profileFeature4;
	protected double profileFeature5;
	protected double profileFeature6;
	protected double profileFeature7;
	
	public Profile() {

		// Create a profile
		this.kinship = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.network = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.indirect = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.group = RandomHelper.nextDouble() > Profile.TWO_THIRDS;
		this.strategy = this.chooseARandomStrategy(COOPERATION_PERCENTAGE >= 0);

//		this.customProfile();
		
		this.groupID = RandomHelper.nextIntFromTo(0, NetworkCuesBuilder.COUNT_GROUPS);
		this.certainty = Profile.TWO_THIRDS;
		this.talkability = RandomHelper.nextDouble();
		
		this.profileFeature1 = RandomHelper.nextDoubleFromTo(this.kinship ? 0 : 0.8, this.kinship ? 0.2 : 1);
		this.profileFeature2 = RandomHelper.nextDoubleFromTo(this.network ? 0 : 0.8, this.network ? 0.2 : 1);
		this.profileFeature3 = RandomHelper.nextDoubleFromTo(this.indirect ? 0 : 0.8, this.indirect ? 0.2 : 1);
		this.profileFeature4 = RandomHelper.nextDoubleFromTo(this.group ? 0 : 0.8, this.group ? 0.2 : 1);
		this.profileFeature5 = RandomHelper.nextDoubleFromTo(this.strategy != Strategy.COOPERATE  ? 0 : 0.8, this.strategy != Strategy.COOPERATE ? 0.2 : 1);
		this.profileFeature6 = RandomHelper.nextDoubleFromTo(this.strategy != Strategy.DEFECT ? 0 : 0.8, this.strategy != Strategy.DEFECT ? 0.2 : 1);
		this.profileFeature7 = RandomHelper.nextDoubleFromTo(this.strategy != Strategy.TITFORTAT ? 0 : 0.8, this.strategy != Strategy.TITFORTAT ? 0.2 : 1);
		
	}	
	
	private void customProfile() {
		this.kinship = false;
		this.network = false;
		this.indirect = RandomHelper.nextDouble() < 0.9;
		this.group = false;
		
	}
	
	private Strategy chooseARandomStrategy(boolean useCooperationPercentage) {
		Strategy [] strategies = Strategy.values();
		
		Strategy chosenStrategy = null;
		if (!useCooperationPercentage) {
			chosenStrategy = strategies[RandomHelper.nextIntFromTo(0, strategies.length - 1)];
		}else if (RandomHelper.nextDouble() < COOPERATION_PERCENTAGE) {
			chosenStrategy = Strategy.COOPERATE;
		} else {
			chosenStrategy = strategies[RandomHelper.nextIntFromTo(1, strategies.length - 1)]; 
		}
		
		return chosenStrategy;
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

	public boolean shouldConsiderDistanceWhenPartnering() {
		return RandomHelper.nextDouble() > Profile.TWO_THIRDS;
	}

	public boolean shouldIRate() {		
		return RandomHelper.nextDouble() > this.talkability;
	}
	
}
