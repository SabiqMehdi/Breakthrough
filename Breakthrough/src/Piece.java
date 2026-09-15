
public enum Piece {
	EMPTY, //EMPTY(0)
	BLACK, //BLACK(2)
	RED; //RED(4)
	
	//private final int serverValue;
	
	/*
	Piece(int sv) {
		this.serverValue = sv;
	}
	*/
	
	/*
	public int toServerValue() {
		return this.serverValue;
	}
	*/
	
	public static Piece fromServerValue(int value) {
		if (value == 2) return BLACK;
		if (value == 4) return RED;
		return EMPTY;
	}
	
	public Piece opposite() {
		if (this == RED) return BLACK;
		if (this == BLACK) return RED;
		return EMPTY;
	}
	
	public int forwardDeltaY() {
		if (this == RED) return -1;
		if (this == BLACK) return +1;
		return 0;
	}
	
	public boolean isPlayer() {
		return this != EMPTY;
	}
}