import java.util.ArrayList;

public class CPUPlayer {
	private final Piece player;
	private final Piece opponent;
	
	private int numExploredNodes;
	
	private static final long TIME_LIMIT_MS = 4900;
	
	public CPUPlayer(Piece player) {
		if ((player == null) || (!player.isPlayer())) throw new IllegalArgumentException("Invalid player piece");
		this.player = player;
		this.opponent = player.opposite();
	}
	
	public int getNumExploredNodes() {
		return this.numExploredNodes;
	}
	
	public Move getNextMoveMinMax(Board board) {
		this.numExploredNodes = 0;
		long searchStartTime = System.currentTimeMillis();
		Move bestMove = null;
		for (int depth = 1; depth <= 30; depth++) {
			if (System.currentTimeMillis() - searchStartTime > TIME_LIMIT_MS) break;
			SearchResult result = minimaxRoot(board, depth, searchStartTime);
			if (result.timedOut) break;
			bestMove = result.bestMove;
		}
		if (bestMove == null) {
			ArrayList<Move> moves = board.getLegalMoves(player);
			return (moves.isEmpty() ? null : moves.get(0));
		}
		return bestMove;
	}
	
	public Move getNextMoveAB(Board board) {
		this.numExploredNodes = 0;
		long searchStartTime = System.currentTimeMillis();
		Move bestMove = null;
		for (int depth = 1; depth <= 30; depth++) {
			if (System.currentTimeMillis() - searchStartTime > TIME_LIMIT_MS) break;
			SearchResult result = alphabetaRoot(board, depth, searchStartTime);
			if (result.timedOut) break;
			bestMove = result.bestMove;
		}
		if (bestMove == null) {
			ArrayList<Move> moves = board.getLegalMoves(player);
			return (moves.isEmpty() ? null : moves.get(0));
		}
		return bestMove;
	}
	
	private static class SearchResult {
		final Move bestMove;
		final int value;
		final boolean timedOut;
		
		SearchResult(Move bestMove, int value, boolean timedOut) {
			this.bestMove = bestMove;
			this.value = value;
			this.timedOut = timedOut;
		}
	}
	
	private SearchResult minimaxRoot(Board board, int depth, long searchStartTime) {
		ArrayList<Move> moves = board.getLegalMoves(player);
		if (moves.isEmpty()) return new SearchResult(null, Integer.MIN_VALUE, false);
		Move bestMove = null;
		int bestValue = Integer.MIN_VALUE;
		for (Move move : moves) {
			if (System.currentTimeMillis() - searchStartTime > TIME_LIMIT_MS) return new SearchResult(bestMove, bestValue, true);
			Piece capturedPiece = board.applyMove(move);
			int value = minimax(board, depth-1, opponent, searchStartTime);
			board.undoMove(move, capturedPiece);
			if ((bestMove == null) || (value > bestValue)) {
				bestValue = value;
				bestMove = move;
			}
		}
		return new SearchResult(bestMove, bestValue, false);
	}
	
	private int minimax(Board board, int depth, Piece pieceToMove, long searchStartTime) {
		if (System.currentTimeMillis() - searchStartTime > TIME_LIMIT_MS) return board.evaluate(player);
		this.numExploredNodes++;
		if ((depth == 0) || (board.hasGameEnded())) return board.evaluate(player);
		ArrayList<Move> moves = board.getLegalMoves(pieceToMove);
		if (moves.isEmpty()) return board.evaluate(player);
		boolean maximizing = (pieceToMove == player);
		int bestValue = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		for (Move move : moves) {
			Piece captured = board.applyMove(move);
			int value = minimax(board, depth-1, pieceToMove.opposite(), searchStartTime);
			board.undoMove(move, captured);
			if (maximizing) bestValue = Math.max(bestValue, value);
			else bestValue = Math.min(bestValue, value);
		}
		return bestValue;
	}
	
	private SearchResult alphabetaRoot(Board board, int depth, long searchStartTime) {
		ArrayList<Move> moves = board.getLegalMoves(player);
		if (moves.isEmpty()) return new SearchResult(null, Integer.MIN_VALUE, false);
		moves.sort((firstMove, secondMove) -> {
			boolean firstMoveIsCaptured = ((firstMove.getFromRow() != firstMove.getToRow()) && (board.getAt(firstMove.getToRow(), firstMove.getToColumn()) == opponent));
			boolean secondMoveIsCaptured = ((secondMove.getFromRow() != secondMove.getToRow()) && (board.getAt(secondMove.getToRow(), secondMove.getToColumn()) == opponent));
			return Boolean.compare(secondMoveIsCaptured, firstMoveIsCaptured);
		});
		int alpha = Integer.MIN_VALUE;
		int beta = Integer.MAX_VALUE;
		Move bestMove = null;
		int bestValue = Integer.MIN_VALUE;
		for (Move move : moves) {
			if (System.currentTimeMillis() - searchStartTime > TIME_LIMIT_MS) return new SearchResult(bestMove, bestValue, true);
			Piece captured = board.applyMove(move);
			int value = alphabeta(board, depth-1, alpha, beta, opponent, searchStartTime);
			board.undoMove(move, captured);
			if ((bestMove == null) || (value > bestValue)) {
				bestValue = value;
				bestMove = move;
			}
			alpha = Math.max(alpha, bestValue);
		}
		return new SearchResult(bestMove, bestValue, false);
	}
	
	private int alphabeta(Board board, int depth, int alpha, int beta, Piece pieceToMove, long searchStartTime) {
		if (System.currentTimeMillis() - searchStartTime > TIME_LIMIT_MS) return board.evaluate(player);
		this.numExploredNodes++;
		if ((depth == 0) || (board.hasGameEnded())) return board.evaluate(player);
		ArrayList<Move> moves = board.getLegalMoves(pieceToMove);
		if (moves.isEmpty()) return board.evaluate(player);
		Piece enemy = pieceToMove.opposite();
		moves.sort((firstMove, secondMove) -> {
			boolean firstMoveIsCaptured = ((firstMove.getFromRow() != firstMove.getToRow()) && (board.getAt(firstMove.getToRow(), firstMove.getToColumn()) == enemy));
			boolean secondMoveIsCaptured = ((secondMove.getFromRow() != secondMove.getToRow()) && (board.getAt(secondMove.getToRow(), secondMove.getToColumn()) == enemy));
			return Boolean.compare(secondMoveIsCaptured, firstMoveIsCaptured);
		});
		boolean maximizing = (pieceToMove == player);
		if (maximizing) {
			int value = Integer.MIN_VALUE;
			for (Move move : moves) {
				Piece captured = board.applyMove(move);
				value = Math.max(value, alphabeta(board, depth-1, alpha, beta, pieceToMove.opposite(), searchStartTime));
				board.undoMove(move, captured);
				alpha = Math.max(alpha, value);
				if (alpha >= beta) break;
			}
			return value;
		} else {
			int value = Integer.MAX_VALUE;
			for (Move move : moves) {
				Piece captured = board.applyMove(move);
				value = Math.min(value, alphabeta(board, depth-1, alpha, beta, pieceToMove.opposite(), searchStartTime));
				board.undoMove(move, captured);
				beta = Math.min(beta, value);
				if (beta <= alpha) break;
			}
			return value;
		}
	}
}
