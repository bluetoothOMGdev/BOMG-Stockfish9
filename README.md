
The chess engine used in the BluetoothOMG package of games is called BOMG-Stockfish9. This chess engine is a converted version of Stockfish9 (under license GPL3) from C++ into Java.
Along with converted programming language, there are some other modifications, including:
1.	BOMG-Stockfish9 cannot use more than one thread to find the best move.
2.	BOMG-Stockfish9 does not have, and does not use any opening book.
3.	Time control is not enforced for BOMG-Stockfish9, and will not have an impact on the output of this chess engine.
4.	BOMG-Stockfish9 does not employ ponderMode.
5.	In order to accelerate initialization of the engine, almost all the time-consuming initializations of variables (Bitboards.RookTable, Bitboards.BishopTable, Search.Reductions, …) have been performed, in advance, and have been saved as constants in a separate class, called WeightsInitializer.java;
How each difficulty level works:
For the first move, BOMG-Stockfish9 picks randomly a move from its top 6 moves (evaluated by depth 0). This move tries to ensure diversity for the positions created during the game. Afterwards, based on difficulty level, it uses one of the following scenarios:
EASY: BOMG-Stockfish9 picks randomly a move from its top 2 moves (evaluated by depth 0).
MEDIUM: BOMG-Stockfish9 picks the top move evaluated by depth 4.
HARD: BOMG-Stockfish9 picks the top move evaluated by depth 10.
You can find the source code for BOMG-Stockfish9 on GitHub: 
How to use BOMG-Stockfish9:
1.	Instantiate Algorithm: ChessAlgorithm algorithm = new ChessAlgorithm();
2.	Initialize parameters: algorithm.main();
3.	Get a new position: ChessAlgorithm.Position position = ChessAlgorithm.getNewPosition();
4.	Find the best move in int: int bestMove = ChessAlgorithm.findTheBestMove(position, difficultyLevel); difficultyLevel can be ChessAlgorithm.EASY, ChessAlgorithm.Medium, or ChessAlgorithm.Hard.
5.	Play the move and change the position: position.do_move(move);
6.	Convert int move to String format: String stringMove = UCI.move(intMove, false);
7.	Convert String move to int format: int intMove = UCI.to_move (position, stringMove);
8.	Undo the last move and change the position: position.undo_move(move);
