
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
	
	public Move(int fromRow, int fromColumn, int toRow, int toColumn) {
		this.fromRow = fromRow;
		this.fromColumn = fromColumn;
		this.toRow = toRow;
		this.toColumn = toColumn;
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
	
	public void setFromRow(int fromRow) {
		this.fromRow = fromRow;
	}
	
	public void setFromColumn(int fromColumn) {
		this.fromColumn = fromColumn;
	}
	
	public void setToRow(int toRow) {
		this.toRow = toRow;
	}
	
	public void setToColumn(int toColumn) {
		this.toColumn = toColumn;
	}
	
	public static boolean inBounds(int row, int column) {
		return ((row >= 0) && (row < 8) && (column >= 0) && (column < 8));
	}
	
	public static String toSquare(int row, int column) {
		char file = (char)('A' + row);
		int rank = 8 - column;
		return "" + file + rank;
	}
	
	public static int[] fromSquare(String square) {
		String finalSquare = square.trim().toUpperCase();
		if (finalSquare.length() != 2) throw new IllegalArgumentException("Square invalid: " + square);
		int row = finalSquare.charAt(0) - 'A';
		int rank = finalSquare.charAt(1) - '0';
		int column = 8 - rank;
		if (!inBounds(row, column)) throw new IllegalArgumentException("Square out of bonds: " + square);
		return new int[] {row, column};
	}
	
	public String convertMoveToStringForServer(boolean withHyphen) {
		String a = toSquare(fromRow, fromColumn);
		String b = toSquare(toRow, toColumn);
		return withHyphen ? (a + "-" + b) : (a + b);
	}
	
	public String toString() {
		return "Move{" + toSquare(fromRow, fromColumn) + " -> " + toSquare(toRow, toColumn) + "}";
	}
}
