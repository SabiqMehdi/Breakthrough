
public enum Piece {
	EMPTY(0),
	BLACK(2),
	RED(4);
	
	private final int serverValue;
	
	Piece(int serverValue) {
		this.serverValue = serverValue;
	}
	
	public int toServerValue() {
		return this.serverValue;
	}
	
	public static Piece fromServerValue(int v) {
		if (v == 2) return BLACK;
		if (v == 4) return RED;
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
