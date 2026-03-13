import java.io.*;
import java.net.*;


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
			MyClient = new Socket("localhost", 8888);

			input = new BufferedInputStream(MyClient.getInputStream());
			output = new BufferedOutputStream(MyClient.getOutputStream());
			//BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
			while (true) {
				char cmd = (char)input.read();
				if (cmd == 65535) break;
				System.out.println("Commande reçue: " + cmd);
				if (cmd == '1') {
					readBoardPayload(input, board);
					gameBoard = Board.fromClientIntGrid(board);
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
					gameBoard = Board.fromClientIntGrid(board);
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
						if (trimmed.contains("-")) useHyphenFormat = true;
						else useHyphenFormat = false;
						
						if ((!trimmed.equalsIgnoreCase("A8-A8")) && (!trimmed.equalsIgnoreCase("A8A8"))) {
							try {
								Move opponentMove = parseServerMove(trimmed);
								gameBoard.applyMove(opponentMove);
							} catch (Exception e) {
								System.out.println("Impossible de parser le dernier coup reçue: " + trimmed);
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
					System.out.println("Coup invalide, entrez un nouveau coup : ");
					if ((gameBoard != null) && (lastMoveSent != null)) gameBoard.undoMove(lastMoveSent, lastCapturedPiece);
					Move retryMove = cpu.getNextMoveAB(gameBoard);
					if (retryMove == null) {
						System.out.println("Aucun coup légal après invalidation!");
						continue;
					}
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
		int size = input.available();
		input.read(aBuffer, 0, size);
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
		byte[] aBuffer = new byte[64];
		int size = input.available();
		input.read(aBuffer, 0, size);
		return new String(aBuffer, 0, size).trim();
	}
	
	public static Move parseServerMove(String stringMove) {
		String cleaned = stringMove.trim().toUpperCase().replace("-", "");
		if (cleaned.length() != 4) {
			throw new IllegalArgumentException("Format de coup invalide: " + cleaned);
		}
		int[] from = Move.fromSquare(cleaned.substring(0, 2));
		int[] to = Move.fromSquare(cleaned.substring(0, 4));
		return new Move(from[0], from[1], to[0], to[1]);
	}
	
	private static void sendMove(Move move, BufferedOutputStream output) throws IOException {
		lastCapturedPiece = gameBoard.applyMove(move);
		lastMoveSent = move;
		String stringMove = move.convertMoveToStringForServer(useHyphenFormat);
		System.out.println("Coup envoyé: " + stringMove);
		output.write(stringMove.getBytes(), 0, stringMove.length());
		output.flush();
	}
}
