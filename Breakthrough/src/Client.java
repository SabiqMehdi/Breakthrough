import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

class Client {
	public static final int SIZE = 8;
	public static Board gameBoard = null;
	public static CPUPlayer cpu = null;
	public static Piece myPiece = null;
	public static Move lastMoveSent = null;
	public static Piece lastCapturedPiece = Piece.EMPTY;
	public static boolean useHyphenFormat = true;
	
	public static void main(String[] args) {

		Socket MyClient;
		BufferedInputStream input;
		BufferedOutputStream output;
		int[][] board = new int[SIZE][SIZE];

		try {
			Scanner sc = new Scanner(System.in);
			System.out.println("Entrer address ip: ");
			String ip = sc.nextLine();
			MyClient = new Socket(ip, 8888);

			input = new BufferedInputStream(MyClient.getInputStream());
			output = new BufferedOutputStream(MyClient.getOutputStream());
			//BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				char cmd = (char)input.read();
				if (cmd == 65535) break;
				System.out.println("Commande reçue: " + cmd);
				if (cmd == '1') {
					readBoardPayload(input, board);
					gameBoard = Board.fromServerGrid(board);
					myPiece = Piece.RED;
					cpu = new CPUPlayer(myPiece);
					System.out.println("Nouvelle partie! Vous jouez en tant que Rouge. Calcul du premier coup...");
					Move bestMove = cpu.getNextMoveAB(gameBoard);
					if (bestMove == null) {
						System.out.println("Aucun coup légal trouvé!");
						continue;
					}
					sendMove(bestMove, output);
				}
				// Debut de la partie en joueur Noir
				if(cmd == '2'){
					readBoardPayload(input, board);
					gameBoard = Board.fromServerGrid(board);
					myPiece = Piece.BLACK;
					cpu = new CPUPlayer(myPiece);
					System.out.println("Nouvelle partie! Vous jouez en tant que Noir. En attente du coup de Rouge...");
				}
				// Le serveur demande le prochain coup
				// Le message contient aussi le dernier coup joue.
				if(cmd == '3'){
					String moveString = readShortPayload(input);
					System.out.println("Dernier coup joué: " + moveString);
					if ((moveString != null) && (!moveString.trim().isEmpty())) {
						String trimmed = moveString.trim();
						useHyphenFormat = trimmed.contains("-");
						if ((!trimmed.equalsIgnoreCase("A8-A8")) && (!trimmed.equalsIgnoreCase("A8A8"))) {
							try {
								Move opponentMove = parseServerMove(trimmed);
								if (gameBoard.isLegalMove(opponentMove, myPiece.opposite())) {
									gameBoard.applyMove(opponentMove);
									System.out.println("Cpup adverse appliqué: " + opponentMove);
								}
								else System.out.println("Coup adverse illégal: " + opponentMove);
							} catch (Exception e) {
								System.out.println("Impossible de parser le dernier coup reçu: " + trimmed);
								e.printStackTrace();
							}
						}
					}
					Move bestMove = cpu.getNextMoveAB(gameBoard);
					if (bestMove == null) {
						System.out.println("Aucun coup légal trouvé!");
						continue;
					}
					sendMove(bestMove, output);
				}
				// Le dernier coup est invalide
				if(cmd == '4'){
					System.out.println("Coup invalide!");
					if ((gameBoard != null) && (lastMoveSent != null)) gameBoard.undoMove(lastMoveSent, lastCapturedPiece);
					ArrayList<Move> legalMoves = gameBoard.getLegalMoves(myPiece);
					if (legalMoves.isEmpty()) {
						System.out.println("Aucun coup légal après invalidation!");
						continue;
					}
					
					Move retryMove = cpu.getNextMoveAB(gameBoard);
					if ((retryMove != null) && (lastMoveSent != null) && (retryMove.getFromRow() == lastMoveSent.getFromRow()) 
							&& (retryMove.getFromColumn() == lastMoveSent.getFromColumn()) && (retryMove.getToRow() == lastMoveSent.getToRow())
							&& (retryMove.getToColumn() == lastMoveSent.getToColumn())) {
						System.out.println("Le CPU repropose le même coup invalide, on choisit un autre coup légal.");
						retryMove = null;
						for (Move m : legalMoves) {
							boolean sameAsLast = ((m.getFromRow() == lastMoveSent.getFromRow()) && (m.getFromColumn() == lastMoveSent.getFromColumn())
									&& (m.getToRow() == lastMoveSent.getToRow()) && (m.getToColumn() == lastMoveSent.getToColumn()));
							if (!sameAsLast) {
								retryMove = m;
								break;
							}
						}
					}
					if (retryMove == null) retryMove = legalMoves.get(0);
					sendMove(retryMove, output);
				}
				// La partie est terminée
				if(cmd == '5'){
					String lastMove = readShortPayload(input);
					System.out.println("Partie Terminé. Le dernier coup joué est: " + lastMove);
					break;
				}
			}
			input.close();
			output.close();
			MyClient.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void readBoardPayload(BufferedInputStream input, int[][] board) throws IOException {
		byte[] aBuffer = new byte[1024];
		int size = input.read(aBuffer);
		if (size <= 0) throw new IOException("Impossible de lire le plateau!");
		String s = new String(aBuffer, 0, size).trim();
		System.out.println("Plateau reçue: " + s);
		String[] boardValues = s.split("\\s+");
		int x = 0;
		int y = 0;
		for (int i = 0; i < boardValues.length && y < SIZE; i++) {
			board[x][y] = Integer.parseInt(boardValues[i]);
			x++;
			if (x == SIZE) {
				x = 0;
				y++;
			}
		}
	}
	
	public static String readShortPayload(BufferedInputStream input) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis()-start < 50) {
			while (input.available() > 0) {
				int b = input.read();
				if (b == -1) break;
				buffer.write(b);
				start = System.currentTimeMillis();
			}
		}
		return buffer.toString().trim();
	}
	
	public static Move parseServerMove(String stringMove) {
		if (stringMove == null) throw new IllegalArgumentException("String move est null");
		String cleaned = stringMove.trim().toUpperCase().replaceAll("\\s+", "").replace("-", "");
		if (cleaned.length() != 4) throw new IllegalArgumentException("Format de coup invalide");
		int[] from = Move.fromSquare(cleaned.substring(0, 2));
		int[] to = Move.fromSquare(cleaned.substring(2, 4));
		return new Move(from[0], from[1], to[0], to[1]);
	}
	
	private static void sendMove(Move move, BufferedOutputStream output) throws IOException {
		lastCapturedPiece = gameBoard.applyMove(move);
		lastMoveSent = move;
		String stringMove = move.convertMoveToStringFromServer(useHyphenFormat);
		System.out.println("Coup envoyé: " + stringMove);
		output.write(stringMove.getBytes(), 0, stringMove.length());
		output.flush();
	}
}