import java.util.ArrayList;

public class Board {
	public static final int SIZE = 8;
	
	private final Piece[][] grid;
	
	public Board() {
		grid = new Piece[SIZE][SIZE];
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) grid[row][column] = Piece.EMPTY;
		}
		
		for (int row = 0; row < SIZE; row++) {
			grid[row][6] = Piece.RED;
			grid[row][7] = Piece.RED;
			
			grid[row][0] = Piece.BLACK;
			grid[row][1] = Piece.BLACK;
		}
	}
	
	public Board(Board copy) {
		grid = new Piece[SIZE][SIZE];
		for (int row = 0; row < SIZE; row++) System.arraycopy(copy.grid[row], 0, this.grid[row], 0, SIZE);
	}
	
	public Board copy() {
		return new Board(this);
	}
	
	public boolean inBounds(int row, int column) {
		return ((row >= 0) && (row < SIZE) && (column >= 0) && (column < 8));
	}
	
	public Piece getAt(int row, int column) {
		return grid[row][column];
	}
	
	public void setAt(int row, int column, Piece piece) {
		grid[row][column] = piece;
	}
	
	public static Board fromClientIntGrid(int[][] clientBoard) {
		Board newBoard = new Board();
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) newBoard.grid[row][column] = Piece.EMPTY;
		}
		
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) newBoard.grid[row][column] = Piece.fromServerValue(clientBoard[row][column]); 
		}
		return newBoard;
	}
	
	public boolean isLegalMove(Move move, Piece player) {
		if ((move == null) || (player == null) || (!player.isPlayer())) return false;
		
		int fromRow = move.getFromRow();
		int fromColumn = move.getFromColumn();
		int toRow = move.getToRow();
		int toColumn = move.getToColumn();
		if ((!inBounds(fromRow, fromColumn)) || (!inBounds(toRow, toColumn))) return false;
		if (grid[fromRow][fromColumn] != player) return false;
		if ((fromRow == toRow) && (fromColumn == toColumn)) return false;
		
		int distanceRow = toRow - fromRow;
		int distanceColumn = toColumn - fromColumn;
		int forwardStep = player.forwardDeltaY();
		if (distanceColumn != forwardStep) return false;
		if (Math.abs(distanceRow) > 1) return false;
		
		Piece destinationPiece = grid[toRow][toColumn];
		if (distanceRow == 0) return (destinationPiece == Piece.EMPTY);
		else {
			if (destinationPiece == Piece.EMPTY) return true;
			return (destinationPiece == player.opposite());
		}
	}
	
	public ArrayList<Move> getLegalMoves(Piece player) {
		ArrayList<Move> moves = new ArrayList<>();
		if ((player == null) || (!player.isPlayer())) return moves;
		
		int forwardStep = player.forwardDeltaY();
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				if (grid[row][column] != player) continue;
				int newColumn = column + forwardStep;
				if ((newColumn < 0) || (newColumn >= SIZE)) continue;
				for (int distanceRow : new int[] {-1, 0, 1}) {
					int newRow = row + distanceRow;
					if (!inBounds(newRow, newColumn)) continue;
					Move move = new Move(row, column, newRow, newColumn);
					if (isLegalMove(move, player)) moves.add(move);
				}
			}
		}
		return moves;
	}
	
	public Piece applyMove(Move move) {
		int fromRow = move.getFromRow();
		int fromColumn = move.getFromColumn();
		int toRow = move.getToRow();
		int toColumn = move.getToColumn();
		Piece movingPiece = grid[fromRow][fromColumn];
		Piece capturedPiece = grid[toRow][toColumn];
		grid[fromRow][fromColumn] = Piece.EMPTY;
		grid[toRow][toColumn] = movingPiece;
		return capturedPiece;
	}
	
	public void undoMove(Move move, Piece captured) {
		int fromRow = move.getFromRow();
		int fromColumn = move.getFromColumn();
		int toRow = move.getToRow();
		int toColumn = move.getToColumn();
		Piece movingBack = grid[toRow][toColumn];
		grid[toRow][toColumn] = (captured == null ? Piece.EMPTY : captured);
		grid[fromRow][fromColumn] = movingBack;
	}
	
	public boolean hasWon(Piece player) {
		if (player == Piece.RED) {
			int toColumn = 0;
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][toColumn] == Piece.RED) return true;
			}
		} else if (player == Piece.BLACK) {
			int toColumn = 7;
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][toColumn] == Piece.BLACK) return true;
			}
		}
		return false;
	}
	
	public boolean hasGameEnded() {
		return ((hasWon(Piece.RED)) || (hasWon(Piece.BLACK)));
	}
	
	public int countPieces(Piece piece) {
		int count = 0;
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				if (grid[row][column] == piece) count++;
			}
		}
		return count;
	}
	
	public int evaluate(Piece player) {
		Piece opponent = player.opposite();
		if (hasWon(player)) return 1_000_000;
		if (hasWon(opponent)) return -1_000_000;
		int score = 0;
		score += 200*(countPieces(player)-countPieces(opponent)); //+ de pièces
		score += 100*(progressScore(player)-progressScore(opponent)); //pièces les + avancées vers la rangée de victoire
		score += 50*(getLegalMoves(player).size()-getLegalMoves(opponent).size()); //+ de mobilité
		score += 75*(captureMoves(player)-captureMoves(opponent)); //+ de possibilités de captures
		return score;
	}
	
	private int progressScore(Piece player) {
		int targetColumn = (player == Piece.RED) ? 0 : 7;
		int somme = 0;
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				if (grid[row][column] != player) continue;
				int distanceToTarget = Math.abs(targetColumn-column);
				somme += (7-distanceToTarget);
			}
		}
		return somme;
	}
	
	private int captureMoves(Piece player) {
		int count = 0;
		for (Move move : getLegalMoves(player)) {
			if (move.getFromRow() != move.getToRow()) if (grid[move.getToRow()][move.getToColumn()] == player.opposite()) count++;
		}
		return count;
	}
}
