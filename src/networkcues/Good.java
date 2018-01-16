package networkcues;

public class Good {
	
	public static final int LEN_TYPE = 10;
	public int type;
	public int cost;
	public int benefit;
	
	public Good (int type) {
		
		this.type = type;
		this.cost = 5;
		this.benefit = 10;
		
	}

	public Good (int type, int cost, int benefit) {
		
		this.type = type;
		this.cost = cost;
		this.benefit = benefit;
		
	}

}
