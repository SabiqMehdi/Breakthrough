import java.util.ArrayList;

public class Board {
	public static final int SIZE = 8;

	private final Piece[][] grid;

	public Board() {
		grid = new Piece[SIZE][SIZE];
		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				grid[row][column] = Piece.EMPTY;
			}
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
		for (int row = 0; row < SIZE; row++) {
			System.arraycopy(copy.grid[row], 0, this.grid[row], 0, SIZE);
		}
	}

	public Board copy() {
		return new Board(this);
	}

	public boolean inBounds(int row, int column) {
		return ((row >= 0) && (row < SIZE) && (column >= 0) && (column < SIZE));
	}

	public Piece getAt(int row, int column) {
		return grid[row][column];
	}

	public void setAt(int row, int column, Piece piece) {
		grid[row][column] = piece;
	}

	public static Board fromServerGrid(int[][] clientBoard) {
		Board newBoard = new Board();

		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				newBoard.grid[row][column] = Piece.fromServerValue(clientBoard[row][column]);
			}
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

		if (distanceRow == 0) {
			return (destinationPiece == Piece.EMPTY);
		} else {
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
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][0] == Piece.RED) return true;
			}
		} else if (player == Piece.BLACK) {
			for (int row = 0; row < SIZE; row++) {
				if (grid[row][7] == Piece.BLACK) return true;
			}
		}
		return false;
	}

	public boolean hasGameEnded() {
		return hasWon(Piece.RED) || hasWon(Piece.BLACK);
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

		if (hasImmediateWinningMove(player)) score += 200_000;
		if (hasImmediateWinningMove(opponent)) score -= 260_000;

		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				Piece piece = grid[row][column];
				if (piece == Piece.EMPTY) continue;

				int sign = (piece == player) ? 1 : -1;
				int advancement = advancementFor(piece, column);

				boolean protectedPiece = isProtected(row, column, piece);
				boolean threatenedPiece = isThreatened(row, column, piece);
				boolean oneMoveFromGoal = isOneMoveFromGoal(row, column, piece);
				boolean twoMovesFromGoal = isTwoMovesFromGoal(row, column, piece);
				boolean clearLaneToGoal = hasClearLaneToGoal(row, column, piece);
				boolean breakthroughReady = isBreakthroughReady(row, column, piece);

				// Matériel
				score += sign * 280;

				// Progression
				score += sign * 90 * advancement;
				score += sign * 18 * advancement * advancement;

				// Centre
				if (row >= 2 && row <= 5) score += sign * 35;

				// Sécurité
				if (protectedPiece) score += sign * 130;
				if (threatenedPiece) score -= sign * 190;

				// Avancée protégée
				if (advancement >= 4 && protectedPiece) score += sign * 260;

				// Avancée fragile
				if (advancement >= 4 && threatenedPiece) score -= sign * 320;
				if (advancement >= 5 && threatenedPiece) score -= sign * 450;

				// Couloir / percée
				if (clearLaneToGoal) score += sign * (45 + 22 * advancement);
				if (breakthroughReady) score += sign * (80 + 30 * advancement);

				// Deux coups du but
				if (twoMovesFromGoal) {
					score += sign * 1300;
					if (protectedPiece) score += sign * 1300;
					if (clearLaneToGoal) score += sign * 700;
					if (threatenedPiece) score -= sign * 1400;
				}

				// Un coup du but
				if (oneMoveFromGoal) {
					score += sign * 7000;
					if (protectedPiece) score += sign * 3500;
					if (clearLaneToGoal) score += sign * 1800;
					if (!threatenedPiece) score += sign * 2400;
					if (threatenedPiece) score -= sign * 2600;
				}
			}
		}

		int mobilityScore = getLegalMoves(player).size() - getLegalMoves(opponent).size();
		score += 15 * mobilityScore;

		score += defensiveStructureScore(player);
		score -= defensiveStructureScore(opponent);

		// Petit biais défensif uniquement pour noir
		if (player == Piece.BLACK) {
			score -= 120 * countAdvancedThreats(Piece.RED);
			score += 60 * countAdvancedThreats(Piece.BLACK);
			score -= opponentBreakthroughDanger(player) / 2;
		}

		return score;
	}

	public boolean isThreatened(int row, int column, Piece piece) {
		Piece enemy = piece.opposite();
		int enemyForward = enemy.forwardDeltaY();
		int enemyColumn = column - enemyForward;

		if ((inBounds(row - 1, enemyColumn)) && (grid[row - 1][enemyColumn] == enemy)) return true;
		if ((inBounds(row + 1, enemyColumn)) && (grid[row + 1][enemyColumn] == enemy)) return true;

		return false;
	}

	public boolean isProtected(int row, int column, Piece piece) {
		int ownForward = piece.forwardDeltaY();
		int protectorColumn = column - ownForward;

		if ((inBounds(row - 1, protectorColumn)) && (grid[row - 1][protectorColumn] == piece)) return true;
		if ((inBounds(row + 1, protectorColumn)) && (grid[row + 1][protectorColumn] == piece)) return true;

		return false;
	}

	private int defensiveStructureScore(Piece player) {
		int score = 0;

		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				if (grid[row][column] != player) continue;

				int backColumn = column - player.forwardDeltaY();

				if ((inBounds(row - 1, backColumn)) && (grid[row - 1][backColumn] == player)) score += 25;
				if ((inBounds(row + 1, backColumn)) && (grid[row + 1][backColumn] == player)) score += 25;
			}
		}

		return score;
	}

	public boolean isCapturedMove(Move move, Piece player) {
		if (move == null) return false;
		if (move.getFromRow() == move.getToRow()) return false;
		return (getAt(move.getToRow(), move.getToColumn()) == player.opposite());
	}

	public boolean isSquareAttackedBy(int row, int column, Piece attacker) {
		int attackerForward = attacker.forwardDeltaY();
		int sourceColumn = column - attackerForward;

		if ((inBounds(row - 1, sourceColumn)) && (grid[row - 1][sourceColumn] == attacker)) return true;
		if ((inBounds(row + 1, sourceColumn)) && (grid[row + 1][sourceColumn] == attacker)) return true;

		return false;
	}

	private int advancementFor(Piece piece, int column) {
		if (piece == Piece.RED) return 7 - column;
		if (piece == Piece.BLACK) return column;
		return 0;
	}

	private boolean isOneMoveFromGoal(int row, int column, Piece piece) {
		if (piece == Piece.RED) return column == 1;
		if (piece == Piece.BLACK) return column == 6;
		return false;
	}

	private boolean isTwoMovesFromGoal(int row, int column, Piece piece) {
		if (piece == Piece.RED) return column == 2;
		if (piece == Piece.BLACK) return column == 5;
		return false;
	}

	private boolean hasClearLaneToGoal(int row, int column, Piece piece) {
		int dir = piece.forwardDeltaY();
		int c = column + dir;

		while (c >= 0 && c < SIZE) {
			if (grid[row][c] == piece.opposite()) return false;
			c += dir;
		}
		return true;
	}

	private boolean isBreakthroughReady(int row, int column, Piece piece) {
		int dir = piece.forwardDeltaY();
		int c = column + dir;

		while (c >= 0 && c < SIZE) {
			if (grid[row][c] == piece.opposite()) return false;
			if (inBounds(row - 1, c) && grid[row - 1][c] == piece.opposite()) return false;
			if (inBounds(row + 1, c) && grid[row + 1][c] == piece.opposite()) return false;
			c += dir;
		}

		return true;
	}

	public boolean hasImmediateWinningMove(Piece player) {
		ArrayList<Move> moves = getLegalMoves(player);

		for (Move move : moves) {
			Piece captured = applyMove(move);
			boolean win = hasWon(player);
			undoMove(move, captured);
			if (win) return true;
		}

		return false;
	}

	private int opponentBreakthroughDanger(Piece player) {
		Piece opponent = player.opposite();
		int danger = 0;

		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				if (grid[row][column] != opponent) continue;

				int advancement = advancementFor(opponent, column);
				boolean protectedPiece = isProtected(row, column, opponent);
				boolean threatenedPiece = isThreatened(row, column, opponent);
				boolean oneMoveFromGoal = isOneMoveFromGoal(row, column, opponent);
				boolean twoMovesFromGoal = isTwoMovesFromGoal(row, column, opponent);
				boolean clearLane = hasClearLaneToGoal(row, column, opponent);

				if (advancement >= 4) danger += 80;
				if (advancement >= 5) danger += 150;

				if (protectedPiece) danger += 120;
				if (!threatenedPiece) danger += 80;
				if (clearLane) danger += 120;

				if (twoMovesFromGoal) danger += 800;
				if (oneMoveFromGoal) danger += 3500;
			}
		}

		return danger;
	}

	private int countAdvancedThreats(Piece piece) {
		int count = 0;

		for (int row = 0; row < SIZE; row++) {
			for (int column = 0; column < SIZE; column++) {
				if (grid[row][column] != piece) continue;

				int advancement = advancementFor(piece, column);
				boolean protectedPiece = isProtected(row, column, piece);
				boolean threatenedPiece = isThreatened(row, column, piece);

				if (advancement >= 4) count++;
				if (advancement >= 4 && protectedPiece) count++;
				if (advancement >= 5 && !threatenedPiece) count += 2;
			}
		}

		return count;
	}
}