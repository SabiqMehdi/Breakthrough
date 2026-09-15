
public class Move {
	private int fromRow;
	private int fromColumn;
	private int toRow;
	private int toColumn;
	
	public Move() {
		this.fromRow = -1;
		this.fromColumn = -1;
		this.toRow = -1;
		this.toColumn = -1;
	}
	
	public Move(int fr, int fc, int tr, int tc) {
		this.fromRow = fr;
		this.fromColumn = fc;
		this.toRow = tr;
		this.toColumn = tc;
	}
	
	public int getFromRow() {
		return this.fromRow;
	}
	
	public int getFromColumn() {
		return this.fromColumn;
	}
	
	public int getToRow() {
		return this.toRow;
	}
	
	public int getToColumn() {
		return this.toColumn;
	}
	
	public void setFromRow(int fr) {
		this.fromRow = fr;
	}
	
	public void setFromColumn(int fc) {
		this.fromColumn = fc;
	}
	
	public void setToRow(int tr) {
		this.toRow = tr;
	}
	
	public void setToColumn(int tc) {
		this.toColumn = tc;
	}
	
	public static boolean inBounds(int row, int column) {
		return ((row >= 0) && (row < 8) && (column >= 0) && (column < 8));
	}
	
	public static String toSquare(int row, int column) {
		char file = (char)('A' + row);
		int rank = 8-column;
		return "" + file + rank;
	}
	
	public static int[] fromSquare(String square) {
		String finalSquare = square.trim().toUpperCase();
		if (finalSquare.length() != 2) throw new IllegalArgumentException("Square invalid: " + square);
		
		int row = finalSquare.charAt(0) - 'A';
		int rank = finalSquare.charAt(1) - '0';
		int column = 8-rank;
		if (!inBounds(row, column)) throw new IllegalArgumentException("Square out of bonds: " + square);
		
		return new int[] {row, column};
	}
	
	public String convertMoveToStringFromServer(boolean withHyphen) {
		String start = toSquare(this.fromRow, this.fromColumn);
		String end = toSquare(this.toRow, this.toColumn);
		return (withHyphen ? (start + "-" + end) : (start + end));
	}
	
	public String toString() {
		return ("Move {" + toSquare(this.fromRow, this.fromColumn) + " -> " + toSquare(this.toRow, this.toColumn) + "}");
	}
}