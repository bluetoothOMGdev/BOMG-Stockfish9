/*
  GPL License and Copyright Notice

  BOMG-Stockfish9, a chess engine derived from Stockfish9
  Copyright (C) 2020 The BOMG-Stockfish9 development team
  
  This file is part of BOMG-Stockfish9.

  BOMG-Stockfish9 is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  BOMG-Stockfish9 is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with BOMG-Stockfish9.  If not, see <http://www.gnu.org/licenses/>.
*/



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

public class ChessAlgorithm {

    public static final int EASY = 1;
    public static final int MEDIUM = 2;
    public static final int HARD = 3;

    public static final int PLAYER_MAXIMIZER = 1;
    public static final int PLAYER_MINIMIZER = 2;


    public ChessAlgorithm() {}


    public void main() {
        UCI.init(UCI.Options);
        setWheights();
        tt.TT.resize(UCI.Options.get("Hash").convertToInteger());
        thread.Threads.set();
        Search.clear();
        thread.Threadd.clearForTheFirstTime = false;
    }

    public void stopThinking() {
        thread.Threads.stop = true;
    }

    private void setWheights() {
        PSQT.PieceValue = WeightsInitializer.Weights_Other_Coefficients.PSQT_PieceValue;
        PSQT.psq = WeightsInitializer.Weights_Other_Coefficients.PSQT_psq;
        Bitboards.SquareBB = WeightsInitializer.Weights_Other_Coefficients.Bitboards_SquareBB;
        Bitboards.FileBB = WeightsInitializer.Weights_Other_Coefficients.Bitboards_FileBB;
        Bitboards.RankBB = WeightsInitializer.Weights_Other_Coefficients.Bitboards_RankBB;
        Bitboards.AdjacentFilesBB = WeightsInitializer.Weights_Other_Coefficients.Bitboards_AdjacentFilesBB;
        Bitboards.ForwardRanksBB = WeightsInitializer.Weights_Other_Coefficients.Bitboards_ForwardRanksBB;
        Bitboards.ForwardFileBB = WeightsInitializer.Weights_Other_Coefficients.Bitboards_ForwardFileBB;
        Bitboards.PawnAttackSpan = WeightsInitializer.Weights_Other_Coefficients.Bitboards_PawnAttackSpan;
        Bitboards.PassedPawnMask = WeightsInitializer.Weights_Other_Coefficients.Bitboards_PassedPawnMask;
        Bitboards.SquareDistance = WeightsInitializer.Weights_Bitboards_SquareDistance.Bitboards_SquareDistance;
        Bitboards.DistanceRingBB = WeightsInitializer.Weights_Bitboards_DistanceRingBB.Bitboards_DistanceRingBB;
        Bitboards.PawnAttacks = WeightsInitializer.Weights_Other_Coefficients.Bitboards_PawnAttacks;
        Bitboards.PseudoAttacks = WeightsInitializer.Weights_Other_Coefficients.Bitboards_PseudoAttacks;
        Bitboards.RookTable = WeightsInitializer.Weights_Bitboards_RookTable.Bitboards_RookTable;
        Bitboards.BishopTable = WeightsInitializer.Weights_Bitboards_BishopTable.Bitboards_BishopTable;
        Bitboards.RookMagics = WeightsInitializer.Weights_Bitboards_RookTable.RookMagics;
        Bitboards.BishopMagics = WeightsInitializer.Weights_Bitboards_BishopTable.BishopMagics;
        Bitboards.LineBB = WeightsInitializer.Weights_Bitboards_LineBB.Bitboards_LineBB;
        Bitboards.BetweenBB = WeightsInitializer.Weights_Bitboards_BetweenBB.Bitboards_BetweenBB;
        Position.Zobrist.psq = WeightsInitializer.Weights_Position_Zobrist_psq.Position_Zobrist_psq;
        Position.Zobrist.enpassant = WeightsInitializer.Weights_Other_Coefficients.Position_Zobrist_enpassant;
        Position.Zobrist.castling = WeightsInitializer.Weights_Other_Coefficients.Position_Zobrist_castling;
        Position.Zobrist.side = WeightsInitializer.Weights_Other_Coefficients.Position_Zobrist_side;
        Position.Zobrist.noPawns = WeightsInitializer.Weights_Other_Coefficients.Position_Zobrist_noPawns;
        Bitbases.KPKBitbase = WeightsInitializer.Weights_Bitbases_KPKBitbase.Bitbases_KPKBitbase;
        Search.FutilityMoveCounts = WeightsInitializer.Weights_Other_Coefficients.Search_FutilityMoveCounts;
        Search.Reductions = WeightsInitializer.Weights_Search_Reductions.Search_Reductions;
        Pawns.Connected = WeightsInitializer.Weights_Other_Coefficients.Pawns_Connected;
    }

    public static int[] getBoardPieces(Position position) {
        int[] boardPieces = new int[64];
        for (int i = 0; i < 64; i++) {
            boardPieces[i] = position.board[i];
        }
        return boardPieces;
    }

    public static List<Integer> getCheckerSquaresList(Position position) {
        List<Integer> checkerSquaresList = new ArrayList<>();
        long checkers = position.st.checkersBB;
        for (int i = 0; i < 64; i++) {
            if (((checkers >>> i) & 1) == 1) {
                checkerSquaresList.add(i);
            }
        }
        return checkerSquaresList;
    }

    public static int getCheckedKingSquare(Position position) {
        int kingColor = position.sideToMove;
        if (kingColor == Types.Color.WHITE) {
            for (int index = 0; index < 64; index++) {
                if (position.board[index] == Types.Piece.W_KING) {
                    return index;
                }
            }
        } else {
            for (int index = 0; index < 64; index++) {
                if (position.board[index] == Types.Piece.B_KING) {
                    return index;
                }
            }
        }
        return -1;
    }

    public static Set<Integer> getCandidatePiecesToMoveSet(Position position) {
        return (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).squaresThatCanMove();
    }

    public static Set<Integer> getCandidateRegionsToGoToSet(Position position, int fromSquare) {
        return (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).squaresThatAPieceCanGoTo(fromSquare);
    }

    public static int getMoveByFromAndToSquares(Position position, int fromSquare, int toSquare) {
        if ((position.board[fromSquare] == Types.Piece.W_KING || position.board[fromSquare] == Types.Piece.B_KING) && Bitboards.SquareDistance[fromSquare][toSquare] > 1) {
            if (toSquare > fromSquare) {
                return (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).getMoveByFromAndToSquares(fromSquare, (fromSquare + 3));
            } else {
                return (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).getMoveByFromAndToSquares(fromSquare, (fromSquare - 4));
            }
        } else {
            return (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).getMoveByFromAndToSquares(fromSquare, toSquare);
        }
    }

    public static int getMoveByColorAndPromotionAndFromAndToSquares(int ourColor, int promotion, int from, int to) {
        switch (ourColor) {
            case Types.Color.WHITE: {
                if (promotion == to || promotion == (to - 8) || promotion == (to - 16) || promotion == (to - 24)) {
                    if (promotion == to) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.QUEEN);
                    } else if (promotion == (to - 8)) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.ROOK);
                    } else if (promotion == (to - 16)) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.BISHOP);
                    } else if (promotion == (to - 24)) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.KNIGHT);
                    }
                }
            }
            case Types.Color.BLACK: {
                if (promotion == to || promotion == (to + 8) || promotion == (to + 16) || promotion == (to + 24)) {
                    if (promotion == to) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.QUEEN);
                    } else if (promotion == (to + 8)) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.ROOK);
                    } else if (promotion == (to + 16)) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.BISHOP);
                    } else if (promotion == (to + 24)) {
                        return Types.make(Types.MoveType.PROMOTION, from, to, Types.PieceType.KNIGHT);
                    }
                }
            }
        }
        return Types.Move.MOVE_NONE;
    }


    public static Position getNewPosition() {
        Position pos = new Position();
        Position.StateListPtr[] states = {new Position.StateListPtr()};
        states[0].add(new Position.StateInfo());

        pos.set(UCI.StartFEN, false, states[0].getLast(), thread.Threads.main());
        return pos;
    }


    public static int findTheBestMove(Position position, int gameDifficultyLevel) {
        if (position.st.previous == null || position.st.previous.previous == null) {
            Movegen.MoveList movesList = new Movegen.MoveList(Movegen.GenType.LEGAL, position);
            Map<Integer, Integer> movesMap = new HashMap<>();
            for (int i = 0; i < movesList.size(); i++) {
                position.do_move(movesList.moveList[i].move);
                movesMap.put(i, Eval.evaluate(position));
                position.undo_move(movesList.moveList[i].move);
            }
            List<Map.Entry<Integer, Integer>> sortedList = sortIndicesOfMapByValueToAList(movesMap, true);
            int random = new Random().nextInt(6);
            return movesList.moveList[sortedList.get(random).getKey()].move;
        }
        Scanner lineScanner = null;
        switch (gameDifficultyLevel) {
            case EASY: {
                Movegen.MoveList movesList = new Movegen.MoveList(Movegen.GenType.LEGAL, position);
                if (movesList.nextIndexOfMoveList == 1) {
                    return movesList.moveList[0].move;
                } else {
                    Map<Integer, Integer> movesMap = new HashMap<>();
                    for (int i = 0; i < movesList.size(); i++) {
                        position.do_move(movesList.moveList[i].move);
                        movesMap.put(i, Eval.evaluate(position));
                        position.undo_move(movesList.moveList[i].move);
                    }
                    List<Map.Entry<Integer, Integer>> sortedList = sortIndicesOfMapByValueToAList(movesMap, true);
                    int random = new Random().nextInt(2);
                    return movesList.moveList[sortedList.get(random).getKey()].move;
                }
            }
            case MEDIUM: {
                lineScanner = new Scanner("depth 4");
                break;
            }
            case HARD: {
                lineScanner = new Scanner("depth 10");
                break;
            }
        }
        Position.StateListPtr states = new Position.StateListPtr();
        states.add(position.st);
        return UCI.go(position, lineScanner, states);
    }


    public static List<Map.Entry<Integer, Integer>> sortIndicesOfMapByValueToAList(Map<Integer, Integer> unsortedMap, final boolean ascending) {
        List<Map.Entry<Integer, Integer>> sortedList = new LinkedList<>(unsortedMap.entrySet());
        Collections.sort(sortedList, new Comparator<Map.Entry<Integer, Integer>>() {
            @Override
            public int compare(Map.Entry<Integer, Integer> e1, Map.Entry<Integer, Integer> e2) {
                if (ascending) {
                    if (e1.getValue() > e2.getValue()) {
                        return 1;
                    } else if (e1.getValue() < e2.getValue()) {
                        return -1;
                    } else {
                        return 0;
                    }
                } else {
                    if (e1.getValue() > e2.getValue()) {
                        return -1;
                    } else if (e1.getValue() < e2.getValue()) {
                        return 1;
                    } else {
                        return 0;
                    }
                }
            }
        });
        return sortedList;
    }


    public static final int NOT_DRAW = 0;
    public static final int FIFTY_MOVE_RULE_DRAW = 1;
    public static final int STALEMATE_DRAW = 2;
    public static final int REPETITION_DRAW = 3;
    public static final int INSUFFICIENT_MATERIAL_DRAW = 4;
    public static int is_draw_for_GUI(Position position) {
        if (position.st.rule50 > 99 && (position.st.checkersBB == 0 || (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).size() != 0)) {
            return FIFTY_MOVE_RULE_DRAW;
        }

        if (position.st.checkersBB == 0 && (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).size() == 0) {
            return STALEMATE_DRAW;
        }

        int positionCounter = 1;
        Position.StateInfo previousStateInfo = position.st;
        while ((previousStateInfo = previousStateInfo.previous) != null && (previousStateInfo = previousStateInfo.previous) != null) {
            if (previousStateInfo.key == position.st.key) {
                positionCounter++;
                if (positionCounter == 3) {
                    return REPETITION_DRAW;
                }
            }
        }

        long[] piecesOnBoardByType = position.byTypeBB;
        if (piecesOnBoardByType[1] == 0 && piecesOnBoardByType[4] == 0 && piecesOnBoardByType[5] == 0
                && ((Long.bitCount(piecesOnBoardByType[2]) < 2 && piecesOnBoardByType[3] == 0)
                    || (piecesOnBoardByType[2] == 0 && ((Bitboards.DarkSquares & piecesOnBoardByType[3]) == 0 || (~Bitboards.DarkSquares & piecesOnBoardByType[3]) == 0)))) {
            return INSUFFICIENT_MATERIAL_DRAW;
        }

        return NOT_DRAW;
    }


    public static boolean is_mated_for_GUI(Position position) {
        if (position.st.checkersBB != 0 && (new Movegen.MoveList(Movegen.GenType.LEGAL, position)).size() == 0) {
            return true;
        } else {
            return false;
        }
    }


    public static class Bitbases {

        public static final int MAX_INDEX = 2*24*64*64;

        public static int[] KPKBitbase = new int[MAX_INDEX / 32];

        public static int index(int us, int bksq, int wksq, int psq) {
            return (wksq | (bksq << 6) | (us << 12) | ((psq & 7) << 13) | ((Types.Rank.RANK_7 - (psq >>> 3)) << 15));
        }

        public static class Result {
            public static final int INVALID = 0;
            public static final int UNKNOWN = 1;
            public static final int DRAW = 2;
            public static final int WIN = 4;
        }

        public static class KPKPosition {
            public int us;
            public int[] ksq = new int[Types.Color.COLOR_NB];
            public int psq;
            public int result;

            public KPKPosition(int idx) {
                int ksqWhite = (idx >>> 0) & 0x3F;
                int ksqBlack = (idx >>> 6) & 0x3F;
                int usTemp = (idx >>> 12) & 0x01;
                int psqTemp = (((Types.Rank.RANK_7 - ((idx >>> 15) & 0x7)) << 3) + ((idx >>> 13) & 0x3));
                ksq[Types.Color.WHITE] = ksqWhite;
                ksq[Types.Color.BLACK] = ksqBlack;
                us = usTemp;
                psq = psqTemp;

                if (Bitboards.SquareDistance[ksqWhite][ksqBlack] <= 1 ||
                        ksqWhite == psqTemp ||
                        ksqBlack == psqTemp ||
                        (usTemp == Types.Color.WHITE && (Bitboards.PawnAttacks[Types.Color.WHITE][psqTemp] & Bitboards.SquareBB[ksqBlack]) != 0)) {
                    result = Result.INVALID;
                } else if (usTemp == Types.Color.WHITE &&
                        (psqTemp >>> 3) == Types.Rank.RANK_7 &&
                        ksqWhite != (psqTemp + Types.Direction.NORTH) &&
                        (Bitboards.SquareDistance[ksqBlack][(psqTemp + Types.Direction.NORTH)] > 1 ||
                                (Bitboards.PseudoAttacks[Types.PieceType.KING][ksqWhite] & Bitboards.SquareBB[(psqTemp + Types.Direction.NORTH)]) != 0)) {
                    result = Result.WIN;
                } else if (usTemp == Types.Color.BLACK &&
                        ((Bitboards.PseudoAttacks[Types.PieceType.KING][ksqBlack] & ~(Bitboards.PseudoAttacks[Types.PieceType.KING][ksqWhite] | Bitboards.PawnAttacks[(usTemp ^ Types.Color.BLACK)][psqTemp])) == 0 ||
                                ((Bitboards.PseudoAttacks[Types.PieceType.KING][ksqBlack] & Bitboards.SquareBB[psqTemp]) & ~Bitboards.PseudoAttacks[Types.PieceType.KING][ksqWhite]) != 0)) {
                    result = Result.DRAW;
                } else {
                    result = Result.UNKNOWN;
                }
            }


            public int classifyWhite(KPKPosition[] db) {
                int Us = Types.Color.WHITE;
                int Them = Types.Color.BLACK;
                int Good = Result.WIN;
                int Bad = Result.DRAW;
                int psqTemp = psq;
                int ksqUs = ksq[Us];
                int ksqThem = ksq[Them];


                int r = Result.INVALID;
                long b = Bitboards.PseudoAttacks[Types.PieceType.KING][ksqUs];

                while (b != 0) {
                    int temp_lsb = Long.numberOfTrailingZeros(b);
                    b &= b - 1;
                    r |= db[(temp_lsb | (ksqThem << 6) | (Them << 12) | ((psqTemp & 7) << 13) | ((Types.Rank.RANK_7 - (psqTemp >>> 3)) << 15))].result;
                }

                if ((psqTemp >>> 3) < Types.Rank.RANK_7) {
                    r |= db[(ksqUs | (ksqThem << 6) | (Them << 12) | (((psqTemp + Types.Direction.NORTH) & 7) << 13) | ((Types.Rank.RANK_7 - ((psqTemp + Types.Direction.NORTH) >>> 3)) << 15))].result;
                }
                if (((psqTemp >>> 3) == Types.Rank.RANK_2) &&
                        ((psqTemp + Types.Direction.NORTH) != ksqUs) &&
                        ((psqTemp + Types.Direction.NORTH) != ksqThem)) {
                    r |= db[(ksqUs | (ksqThem << 6) | (Them << 12) | (((psqTemp + Types.Direction.NORTH) & 7) << 13) | ((Types.Rank.RANK_7 - ((psqTemp + Types.Direction.NORTH + Types.Direction.NORTH) >>> 3)) << 15))].result;
                }

                result = (((r & Good) != 0) ? Good : (((r & Result.UNKNOWN) != 0) ? Result.UNKNOWN : Bad));
                return result;
            }

            public int classifyBlack(KPKPosition[] db) {
                int Us = Types.Color.BLACK;
                int Them = Types.Color.WHITE;
                int Good = Result.DRAW;
                int Bad = Result.WIN;
                int psqTemp = psq;
                int ksqUs = ksq[Us];
                int ksqThem = ksq[Them];

                int r = Result.INVALID;
                long b = Bitboards.PseudoAttacks[Types.PieceType.KING][ksqUs];

                while (b != 0) {
                    int temp_lsb = Long.numberOfTrailingZeros(b);
                    b &= b - 1;
                    r |= db[(ksqThem | (temp_lsb << 6) | (Them << 12) | ((psqTemp & 7) << 13) | ((Types.Rank.RANK_7 - (psqTemp >>> 3)) << 15))].result;
                }

                result = (((r & Good) != 0) ? Good : (((r & Result.UNKNOWN) != 0) ? Result.UNKNOWN : Bad));
                return result;
            }
        }

        public static boolean probe(int wksq, int wpsq, int bksq, int us) {
            int idx = index(us, bksq, wksq, wpsq);
            return ((KPKBitbase[idx / 32] & (1 << (idx & 0x1F))) != 0);
        }

        public static void init() {
            int MAX_INDEX = 2*24*64*64;
            KPKPosition[] db = new KPKPosition[MAX_INDEX];
            int idx, repeat = 1;

            for (idx = 0; idx < MAX_INDEX; ++idx) {
                db[idx] = new KPKPosition(idx);
            }

            while (repeat != 0) {
                repeat = 0;
                for (idx = 0; idx < MAX_INDEX; ++idx) {
                    KPKPosition kPosition = db[idx];
                    repeat |= ((kPosition.result == Result.UNKNOWN && ((kPosition.us == Types.Color.WHITE) ? kPosition.classifyWhite(db) : kPosition.classifyBlack(db)) != Result.UNKNOWN) ? 1 : 0);
                }
            }

            for (idx = 0; idx < MAX_INDEX; ++idx) {
                if (db[idx].result == Result.WIN) {
                    KPKBitbase[idx / 32] |= 1 << (idx & 0x1F);
                }
            }
        }
    }


    public static class Bitboards {

        public static final long AllSquares = ~0L;
        public static final long DarkSquares = 0xAA55AA55AA55AA55L;

        public static final long FileABB = 0x0101010101010101L;
        public static final long FileBBB = FileABB << 1;
        public static final long FileCBB = FileABB << 2;
        public static final long FileDBB = FileABB << 3;
        public static final long FileEBB = FileABB << 4;
        public static final long FileFBB = FileABB << 5;
        public static final long FileGBB = FileABB << 6;
        public static final long FileHBB = FileABB << 7;

        public static final long Rank1BB = 0xFF;
        public static final long Rank2BB = Rank1BB << (8 * 1);
        public static final long Rank3BB = Rank1BB << (8 * 2);
        public static final long Rank4BB = Rank1BB << (8 * 3);
        public static final long Rank5BB = Rank1BB << (8 * 4);
        public static final long Rank6BB = Rank1BB << (8 * 5);
        public static final long Rank7BB = Rank1BB << (8 * 6);
        public static final long Rank8BB = Rank1BB << (8 * 7);

        public static int[][] SquareDistance = new int[Types.Square.SQUARE_NB][Types.Square.SQUARE_NB];

        public static long[] SquareBB = new long[Types.Square.SQUARE_NB];
        public static long[] FileBB = new long[Types.File.FILE_NB];
        public static long[] RankBB = new long[Types.Rank.RANK_NB];
        public static long[] AdjacentFilesBB = new long[Types.File.FILE_NB];
        public static long[][] ForwardRanksBB = new long[Types.Color.COLOR_NB][Types.Rank.RANK_NB];
        public static long[][] BetweenBB = new long[Types.Square.SQUARE_NB][Types.Square.SQUARE_NB];
        public static long[][] LineBB = new long[Types.Square.SQUARE_NB][Types.Square.SQUARE_NB];
        public static long[][] DistanceRingBB = new long[Types.Square.SQUARE_NB][8];
        public static long[][] ForwardFileBB = new long[Types.Color.COLOR_NB][Types.Square.SQUARE_NB];
        public static long[][] PassedPawnMask = new long[Types.Color.COLOR_NB][Types.Square.SQUARE_NB];
        public static long[][] PawnAttackSpan = new long[Types.Color.COLOR_NB][Types.Square.SQUARE_NB];
        public static long[][] PseudoAttacks = new long[Types.PieceType.PIECE_TYPE_NB][Types.Square.SQUARE_NB];
        public static long[][] PawnAttacks = new long[Types.Color.COLOR_NB][Types.Square.SQUARE_NB];

        public static Magic[] RookMagics = new Magic[Types.Square.SQUARE_NB];
        public static Magic[] BishopMagics = new Magic[Types.Square.SQUARE_NB];

        public static long[] RookTable = new long[0x19000];
        public static long[] BishopTable = new long[0x1480];

        static {
            for (int i = 0; i < Types.Square.SQUARE_NB; i++) {
                RookMagics[i] = new Magic();
                BishopMagics[i] = new Magic();
            }
        }


        public static class Magic {
            public long mask;
            public long magic;
            public long[] attacks;
            public int shift;
            public int attacksArrayFirstIndex = 0;

            public Magic() {}

            public Magic(long mask, long magic, long[] attacks, int shift, int attacksArrayFirstIndex) {
                this.mask = mask;
                this.magic = magic;
                this.attacks = attacks;
                this.shift = shift;
                this.attacksArrayFirstIndex = attacksArrayFirstIndex;
            }

            public int index(long occupied) {
                int lo = ((int) occupied) & ((int) mask);
                int hi = ((int) (occupied >>> 32)) & ((int) (mask >>> 32));
                return (lo * ((int) magic) ^ hi * ((int) (magic >>> 32))) >>> shift;
            }
        }

        public static boolean more_than_one(long b) {
            return (b & (b - 1)) != 0;
        }

        public static long shift(int D, long b) {
            return  D == Types.Direction.NORTH ? b << 8 : D == Types.Direction.SOUTH ? b >>> 8
                    : D == Types.Direction.NORTH_EAST ? (b & ~FileHBB) << 9 : D == Types.Direction.SOUTH_EAST ? (b & ~FileHBB) >>> 7
                    : D == Types.Direction.NORTH_WEST ? (b & ~FileABB) << 7 : D == Types.Direction.SOUTH_WEST ? (b & ~FileABB) >>> 9
                    : 0;
        }

        public static int distance_as_a_function_of_TemplateFile(int x, int y) {
            return (((x & 7) < (y & 7)) ? ((y & 7) - (x & 7)) : ((x & 7) - (y & 7)));
        }

        public static int distance_as_a_function_of_TemplateRank(int x, int y) {
            return (((x >>> 3) < (y >>> 3)) ? ((y >>> 3) - (x >>> 3)) : ((x >>> 3) - (y >>> 3)));
        }

        public static long attacks_bb_as_a_function_of_TemplatePieceType(int Pt, int s, long occupied) {
            Magic m = Pt == Types.PieceType.ROOK ? RookMagics[s] : BishopMagics[s];
            return m.attacks[m.index(occupied) + m.attacksArrayFirstIndex];
        }

        public static long attacks_bb(int pt, int s, long occupied) {
            switch (pt) {
                case Types.PieceType.BISHOP:
                    return attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, s, occupied);
                case Types.PieceType.ROOK:
                    return attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, s, occupied);
                case Types.PieceType.QUEEN:
                    return attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, s, occupied) | attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, s, occupied);
                default:
                    return PseudoAttacks[pt][s];
            }
        }

        public static int backmost_sq(int c, long b) {
            return c == Types.Color.WHITE ? Long.numberOfTrailingZeros(b) : (63 ^ Long.numberOfLeadingZeros(b));
        }


        public static void init() {
            for (int s = Types.Square.SQ_A1; s <= Types.Square.SQ_H8; ++s) {
                SquareBB[s] = 1L << s;
            }

            for (int f = Types.File.FILE_A; f <= Types.File.FILE_H; ++f) {
                FileBB[f] = f > Types.File.FILE_A ? FileBB[f - 1] << 1 : FileABB;
            }

            for (int r = Types.Rank.RANK_1; r <= Types.Rank.RANK_8; ++r) {
                RankBB[r] = r > Types.Rank.RANK_1 ? RankBB[r - 1] << 8 : Rank1BB;
            }

            for (int f = Types.File.FILE_A; f <= Types.File.FILE_H; ++f) {
                AdjacentFilesBB[f] = (f > Types.File.FILE_A ? FileBB[f - 1] : 0) | (f < Types.File.FILE_H ? FileBB[f + 1] : 0);
            }

            for (int r = Types.Rank.RANK_1; r < Types.Rank.RANK_8; ++r) {
                ForwardRanksBB[Types.Color.WHITE][r] = ~(ForwardRanksBB[Types.Color.BLACK][r + 1] = ForwardRanksBB[Types.Color.BLACK][r] | RankBB[r]);
            }

            for (int c = Types.Color.WHITE; c <= Types.Color.BLACK; ++c) {
                for (int s = Types.Square.SQ_A1; s <= Types.Square.SQ_H8; ++s) {
                    ForwardFileBB [c][s] = ForwardRanksBB[c][(s >>> 3)] & FileBB[(s & 7)];
                    PawnAttackSpan[c][s] = ForwardRanksBB[c][(s >>> 3)] & AdjacentFilesBB[(s & 7)];
                    PassedPawnMask[c][s] = ForwardFileBB [c][s] | PawnAttackSpan[c][s];
                }
            }

            for (int s1 = Types.Square.SQ_A1; s1 <= Types.Square.SQ_H8; ++s1) {
                for (int s2 = Types.Square.SQ_A1; s2 <= Types.Square.SQ_H8; ++s2) {
                    if (s1 != s2) {
                        SquareDistance[s1][s2] = Math.max(distance_as_a_function_of_TemplateFile(s1, s2), distance_as_a_function_of_TemplateRank(s1, s2));
                        DistanceRingBB[s1][SquareDistance[s1][s2] - 1] = (DistanceRingBB[s1][SquareDistance[s1][s2] - 1] | SquareBB[s2]);
                    }
                }
            }

            int[][] steps = {
                    {0, 0, 0, 0, 0},
                    {7, 9, 0, 0, 0},
                    {6, 10, 15, 17, 0},
                    {0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0},
                    {0, 0, 0, 0, 0},
                    {1, 7, 8, 9, 0}};

            for (int c = Types.Color.WHITE; c <= Types.Color.BLACK; ++c) {
                for (int pt : new int[] {Types.PieceType.PAWN, Types.PieceType.KNIGHT, Types.PieceType.KING}) {
                    for (int s = Types.Square.SQ_A1; s <= Types.Square.SQ_H8; ++s) {
                        for (int i = 0; steps[pt][i] != 0; ++i) {
                            int to = s + (c == Types.Color.WHITE ? steps[pt][i] : -steps[pt][i]);
                            if (((to >= Types.Square.SQ_A1) && (to <= Types.Square.SQ_H8)) && SquareDistance[s][to] < 3) {
                                if (pt == Types.PieceType.PAWN) {
                                    PawnAttacks[c][s] = (PawnAttacks[c][s] | SquareBB[to]);
                                } else {
                                    PseudoAttacks[pt][s] = (PseudoAttacks[pt][s] | SquareBB[to]);
                                }
                            }
                        }
                    }
                }
            }

            int[] RookDirections = {Types.Direction.NORTH, Types.Direction.EAST, Types.Direction.SOUTH, Types.Direction.WEST};
            int[] BishopDirections = {Types.Direction.NORTH_EAST, Types.Direction.SOUTH_EAST, Types.Direction.SOUTH_WEST, Types.Direction.NORTH_WEST};

            init_magics(RookTable, RookMagics, RookDirections);
            init_magics(BishopTable, BishopMagics, BishopDirections);

            for (int s1 = Types.Square.SQ_A1; s1 <= Types.Square.SQ_H8; ++s1) {
                PseudoAttacks[Types.PieceType.QUEEN][s1] = PseudoAttacks[Types.PieceType.BISHOP][s1] = attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, s1, 0);
                PseudoAttacks[Types.PieceType.QUEEN][s1] |= PseudoAttacks[Types.PieceType.ROOK][s1] = attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, s1, 0);

                for (int pt : new int[] {Types.PieceType.BISHOP, Types.PieceType.ROOK}) {
                    for (int s2 = Types.Square.SQ_A1; s2 <= Types.Square.SQ_H8; ++s2) {
                        if ((PseudoAttacks[pt][s1] & SquareBB[s2]) == 0) {
                            continue;
                        }
                        LineBB[s1][s2] = (((attacks_bb(pt, s1, 0) & attacks_bb(pt, s2, 0)) | SquareBB[s1]) | SquareBB[s2]);
                        BetweenBB[s1][s2] = attacks_bb(pt, s1, SquareBB[s2]) & attacks_bb(pt, s2, SquareBB[s1]);
                    }
                }
            }
        }

        public static long sliding_attack(int[] directions, int sq, long occupied) {
            long attack = 0L;

            for (int i = 0; i < 4; ++i) {
                for (int s = sq + directions[i]; ((s >= Types.Square.SQ_A1) && (s <= Types.Square.SQ_H8)) && SquareDistance[s][s - directions[i]] == 1; s += directions[i]) {
                    attack = (attack | SquareBB[s]);

                    if ((occupied & SquareBB[s]) != 0) {
                        break;
                    }
                }
            }
            return attack;
        }


        public static void init_magics(long[] table, Magic[] magics, int[] directions) {
            long t1 = System.currentTimeMillis();
            int[][] seeds = {{8977, 44560, 54343, 38998, 5731, 95205, 104912, 17020}, {728, 10316, 55013, 32803, 12281, 15100, 16645, 255}};

            long[] occupancy = new long[4096];
            long[] reference = new long[4096];
            long edges;
            long b;

            int[] epoch = new int[4096];
            int cnt = 0;
            int size = 0;

            for (int s = Types.Square.SQ_A1; s <= Types.Square.SQ_H8; ++s) {
                edges = ((Rank1BB | Rank8BB) & ~RankBB[(s >>> 3)]) | ((FileABB | FileHBB) & ~FileBB[(s & 7)]);

                long mmask;
                long mmagic = 0;
                int mshift;
                int mattacksArrayFirstIndex;

                Magic m = magics[s];
                mmask  = sliding_attack(directions, s, 0) & ~edges;
                mshift = 32 - Long.bitCount(mmask);

                if (s == Types.Square.SQ_A1) {
                    m.attacks = table;
                    mattacksArrayFirstIndex = 0;
                } else {
                    m.attacks = magics[s - 1].attacks;
                    mattacksArrayFirstIndex = magics[s - 1].attacksArrayFirstIndex + size;
                }

                b = size = 0;
                do {
                    occupancy[size] = b;
                    reference[size] = sliding_attack(directions, s, b);

                    size++;
                    b = (b - mmask) & mmask;
                } while (b != 0);


                Misc.PRNG rng = new Misc.PRNG(seeds[0][(s >>> 3)]);

                for (int i = 0; i < size; ) {
                    for (mmagic = 0; Long.bitCount((mmagic * mmask) >>> 56) < 6; ) {
                        mmagic = rng.sparse_rand();
                    }
                    for (++cnt, i = 0; i < size; ++i) {
                        long occupied = occupancy[i];
                        int lo = ((int) occupied) & ((int) mmask);
                        int hi = ((int) (occupied >>> 32)) & ((int) (mmask >>> 32));
                        int idx = (lo * ((int) mmagic) ^ hi * ((int) (mmagic >>> 32))) >>> mshift;
                        if (epoch[idx] < cnt) {
                            epoch[idx] = cnt;
                            m.attacks[idx + mattacksArrayFirstIndex] = reference[i];
                        } else if (m.attacks[idx + mattacksArrayFirstIndex] != reference[i]) {
                            break;
                        }
                    }
                }
                m.mask = mmask;
                m.magic = mmagic;
                m.shift = mshift;
                m.attacksArrayFirstIndex = mattacksArrayFirstIndex;
            }
            long t2 = System.currentTimeMillis();
//            System.out.println((t2 - t1));
        }


        public static String pretty(long b) {
            String s = "+---+---+---+---+---+---+---+---+\n";

            for (int r = Types.Rank.RANK_8; r >= Types.Rank.RANK_1; --r) {
                for (int f = Types.File.FILE_A; f <= Types.File.FILE_H; ++f) {
                    s += (b & SquareBB[((r << 3) + f)]) != 0 ? "| X " : "|   ";
                }
                s += "|\n+---+---+---+---+---+---+---+---+\n";
            }
            return s;
        }
    }


    public static class endgame {

        public static class EndgameCode {
            public static final int EVALUATION_FUNCTIONS = 0;
            public static final int KNNK = 1;
            public static final int KXK = 2;
            public static final int KBNK = 3;
            public static final int KPK = 4;
            public static final int KRKP = 5;
            public static final int KRKB = 6;
            public static final int KRKN = 7;
            public static final int KQKP = 8;
            public static final int KQKR = 9;

            public static final int SCALING_FUNCTIONS = 10;
            public static final int KBPsK = 11;
            public static final int KQKRPs = 12;
            public static final int KRPKR = 13;
            public static final int KRPKB = 14;
            public static final int KRPPKRP = 15;
            public static final int KPsK = 16;
            public static final int KBPKB = 17;
            public static final int KBPPKB = 18;
            public static final int KBPKN = 19;
            public static final int KNPK = 20;
            public static final int KNPKB = 21;
            public static final int KPKP =22;
        }

        public static final int[] PushToEdges = {
                100, 90, 80, 70, 70, 80, 90, 100,
                90, 70, 60, 50, 50, 60, 70,  90,
                80, 60, 40, 30, 30, 40, 60,  80,
                70, 50, 30, 20, 20, 30, 50,  70,
                70, 50, 30, 20, 20, 30, 50,  70,
                80, 60, 40, 30, 30, 40, 60,  80,
                90, 70, 60, 50, 50, 60, 70,  90,
                100, 90, 80, 70, 70, 80, 90, 100 };

        public static final int[] PushToCorners = {
                200, 190, 180, 170, 160, 150, 140, 130,
                190, 180, 170, 160, 150, 140, 130, 140,
                180, 170, 155, 140, 140, 125, 140, 150,
                170, 160, 140, 120, 110, 140, 150, 160,
                160, 150, 140, 110, 120, 140, 160, 170,
                150, 140, 125, 140, 140, 155, 170, 180,
                140, 130, 140, 150, 160, 170, 180, 190,
                130, 140, 150, 160, 170, 180, 190, 200 };

        public static final int[] PushClose = {0, 0, 100, 80, 60, 40, 20, 10};
        public static final int[] PushAway = {0, 5, 20, 40, 60, 80, 90, 100};
        public static final int[] KRPPKRPScaleFactors = {0, 9, 10, 14, 21, 44, 0, 0};

        public static int normalize(Position pos, int strongSide, int sq) {
            if ((pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0] & 7) >= Types.File.FILE_E) {
                sq = sq ^ 7;
            }
            if (strongSide == Types.Color.BLACK) {
                sq = (sq ^ Types.Square.SQ_A8);
            }
            return sq;
        }


        public static abstract class EndgameBase {
            public int strongSide;
            public int weakSide;

            public boolean isInstanceOf_Value;

            public EndgameBase(boolean isInstanceOf_Value, int c) {
                strongSide = c;
                weakSide = (c ^ Types.Color.BLACK);
                this.isInstanceOf_Value = isInstanceOf_Value;
            }

            public abstract int convertToInteger(Position pos);
        }


        public static abstract class Endgame extends EndgameBase {
            public int E;
            public Endgame(int E, int c) {
                super(E < EndgameCode.SCALING_FUNCTIONS, c);
                this.E = E;
            }

            public abstract int convertToInteger(Position pos);
        }


        public static class Endgames {

            public Endgames() {
                add(EndgameCode.KPK, "KPK", new Endgame_KPK(EndgameCode.KPK, Types.Color.WHITE), new Endgame_KPK(EndgameCode.KPK, Types.Color.BLACK));
                add(EndgameCode.KNNK, "KNNK", new Endgame_KNNK(EndgameCode.KNNK, Types.Color.WHITE), new Endgame_KNNK(EndgameCode.KNNK, Types.Color.BLACK));
                add(EndgameCode.KBNK, "KBNK", new Endgame_KBNK(EndgameCode.KBNK, Types.Color.WHITE), new Endgame_KBNK(EndgameCode.KBNK, Types.Color.BLACK));
                add(EndgameCode.KRKP, "KRKP", new Endgame_KRKP(EndgameCode.KRKP, Types.Color.WHITE), new Endgame_KRKP(EndgameCode.KRKP, Types.Color.BLACK));
                add(EndgameCode.KRKB, "KRKB", new Endgame_KRKB(EndgameCode.KRKB, Types.Color.WHITE), new Endgame_KRKB(EndgameCode.KRKB, Types.Color.BLACK));
                add(EndgameCode.KRKN, "KRKN", new Endgame_KRKN(EndgameCode.KRKN, Types.Color.WHITE), new Endgame_KRKN(EndgameCode.KRKN, Types.Color.BLACK));
                add(EndgameCode.KQKP, "KQKP", new Endgame_KQKP(EndgameCode.KQKP, Types.Color.WHITE), new Endgame_KQKP(EndgameCode.KQKP, Types.Color.BLACK));
                add(EndgameCode.KQKR, "KQKR", new Endgame_KQKR(EndgameCode.KQKR, Types.Color.WHITE), new Endgame_KQKR(EndgameCode.KQKR, Types.Color.BLACK));

                add(EndgameCode.KNPK, "KNPK", new Endgame_KNPK(EndgameCode.KNPK, Types.Color.WHITE), new Endgame_KNPK(EndgameCode.KNPK, Types.Color.BLACK));
                add(EndgameCode.KNPKB, "KNPKB", new Endgame_KNPKB(EndgameCode.KNPKB, Types.Color.WHITE), new Endgame_KNPKB(EndgameCode.KNPKB, Types.Color.BLACK));
                add(EndgameCode.KRPKR, "KRPKR", new Endgame_KRPKR(EndgameCode.KRPKR, Types.Color.WHITE), new Endgame_KRPKR(EndgameCode.KRPKR, Types.Color.BLACK));
                add(EndgameCode.KRPKB, "KRPKB", new Endgame_KRPKB(EndgameCode.KRPKB, Types.Color.WHITE), new Endgame_KRPKB(EndgameCode.KRPKB, Types.Color.BLACK));
                add(EndgameCode.KBPKB, "KBPKB", new Endgame_KBPKB(EndgameCode.KBPKB, Types.Color.WHITE), new Endgame_KBPKB(EndgameCode.KBPKB, Types.Color.BLACK));
                add(EndgameCode.KBPKN, "KBPKN", new Endgame_KBPKN(EndgameCode.KBPKN, Types.Color.WHITE), new Endgame_KBPKN(EndgameCode.KBPKN, Types.Color.BLACK));
                add(EndgameCode.KBPPKB, "KBPPKB", new Endgame_KBPPKB(EndgameCode.KBPPKB, Types.Color.WHITE), new Endgame_KBPPKB(EndgameCode.KBPPKB, Types.Color.BLACK));
                add(EndgameCode.KRPPKRP, "KRPPKRP", new Endgame_KRPPKRP(EndgameCode.KRPPKRP, Types.Color.WHITE), new Endgame_KRPPKRP(EndgameCode.KRPPKRP, Types.Color.BLACK));
            }

            public static class Map extends HashMap<Long, EndgameBase> {
                private static final long serialVersionUID = 5577027287308552388L;
            }

            public class PairOfMaps {
                Map map_of_Value = new Map();
                Map map_of_ScaleFactor = new Map();
            }

            public PairOfMaps maps = new PairOfMaps();

            public Map map(boolean isInstanceOf_Value) {
                return isInstanceOf_Value ? maps.map_of_Value : maps.map_of_ScaleFactor;
            }

            public void add(int E, String code, Endgame whiteEndgameObject, Endgame blackEndgameObject) {
                boolean isInstanceOf_Value = E < EndgameCode.SCALING_FUNCTIONS;
                Position.StateInfo st = new Position.StateInfo();
                map(isInstanceOf_Value).put(new Position().set(code, Types.Color.WHITE, st).st.materialKey, whiteEndgameObject);
                map(isInstanceOf_Value).put(new Position().set(code, Types.Color.BLACK, st).st.materialKey, blackEndgameObject);
            }

            public EndgameBase probe(boolean isInstanceOf_Value, long key) {
                return map(isInstanceOf_Value).get(key);
            }
        }



        public static class Endgame_KXK extends Endgame {
            public Endgame_KXK(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                if (pos.sideToMove == weakSide && new Movegen.MoveList(Movegen.GenType.LEGAL, pos).size() == 0) {
                    return Types.Value.VALUE_DRAW;
                }

                int winnerKSq = pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0];
                int loserKSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];

                int result = pos.st.nonPawnMaterial[strongSide]
                        + pos.pieceCount[((strongSide << 3) + Types.PieceType.PAWN)] * Types.Value.PawnValueEg
                        + PushToEdges[loserKSq]
                        + PushClose[Bitboards.SquareDistance[winnerKSq][loserKSq]];

                if (pos.pieceCount[((strongSide << 3) + Types.PieceType.QUEEN)] != 0 ||
                        pos.pieceCount[((strongSide << 3) + Types.PieceType.ROOK)] != 0 ||
                        (pos.pieceCount[((strongSide << 3) + Types.PieceType.BISHOP)] != 0 && pos.pieceCount[((strongSide << 3) + Types.PieceType.KNIGHT)] != 0) ||
                        (((pos.byColorBB[strongSide] & pos.byTypeBB[Types.PieceType.BISHOP]) & ~Bitboards.DarkSquares) != 0 && ((pos.byColorBB[strongSide] & pos.byTypeBB[Types.PieceType.BISHOP]) & Bitboards.DarkSquares) != 0)) {
                    result = Math.min(result + Types.Value.VALUE_KNOWN_WIN, Types.Value.VALUE_MATE_IN_MAX_PLY - 1);
                }

                return strongSide == pos.sideToMove ? result : -result;
            }
        }



        public static class Endgame_KBNK extends Endgame {
            public Endgame_KBNK(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int winnerKSq = pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0];
                int loserKSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                int bishopSq = pos.pieceList[((strongSide << 3) + Types.PieceType.BISHOP)][0];

                int sss = bishopSq ^ Types.Square.SQ_A1;
                if ((((sss >>> 3) ^ sss) & 1) != 0) {
                    winnerKSq = (winnerKSq ^ Types.Square.SQ_A8);
                    loserKSq  = (loserKSq ^ Types.Square.SQ_A8);
                }

                int result = Types.Value.VALUE_KNOWN_WIN + PushClose[Bitboards.SquareDistance[winnerKSq][loserKSq]] + PushToCorners[loserKSq];

                return strongSide == pos.sideToMove ? result : -result;
            }
        }



        public static class Endgame_KPK extends Endgame {
            public Endgame_KPK(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int wksq = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0]);
                int bksq = normalize(pos, strongSide, pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0]);
                int psq  = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0]);

                int us = strongSide == pos.sideToMove ? Types.Color.WHITE : Types.Color.BLACK;

                if (!Bitbases.probe(wksq, psq, bksq, us)) {
                    return Types.Value.VALUE_DRAW;
                }

                int result = Types.Value.VALUE_KNOWN_WIN + Types.Value.PawnValueEg + (psq >>> 3);

                return strongSide == pos.sideToMove ? result : -result;
            }
        }



        public static class Endgame_KRKP extends Endgame {
            public Endgame_KRKP(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int wksq = (pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0] ^ (strongSide * 56));
                int bksq = (pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0] ^ (strongSide * 56));
                int rsq  = (pos.pieceList[((strongSide << 3) + Types.PieceType.ROOK)][0] ^ (strongSide * 56));
                int psq  = (pos.pieceList[((weakSide << 3) + Types.PieceType.PAWN)][0] ^ (strongSide * 56));

                int queeningSq = ((Types.Rank.RANK_1 << 3) + (psq & 7));
                int result;

                if (wksq < psq && (wksq & 7) == (psq & 7)) {
                    result = Types.Value.RookValueEg - Bitboards.SquareDistance[wksq][psq];
                } else if (Bitboards.SquareDistance[bksq][psq] >= 3 + (pos.sideToMove == weakSide ? 1 : 0) &&
                        Bitboards.SquareDistance[bksq][rsq] >= 3) {
                    result = Types.Value.RookValueEg - Bitboards.SquareDistance[wksq][psq];
                } else if ((bksq >>> 3) <= Types.Rank.RANK_3 && Bitboards.SquareDistance[bksq][psq] == 1 &&
                        (wksq >>> 3) >= Types.Rank.RANK_4 && Bitboards.SquareDistance[wksq][psq] > 2 + (pos.sideToMove == strongSide ? 1 : 0)) {
                    result = 80 - 8 * Bitboards.SquareDistance[wksq][psq];
                } else {
                    result =  200 - 8 * (Bitboards.SquareDistance[wksq][psq + Types.Direction.SOUTH]
                            - Bitboards.SquareDistance[bksq][psq + Types.Direction.SOUTH]
                            - Bitboards.SquareDistance[psq][queeningSq]);
                }

                return strongSide == pos.sideToMove ? result : -result;
            }
        }



        public static class Endgame_KRKB extends Endgame {
            public Endgame_KRKB(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int result = PushToEdges[pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0]];
                return strongSide == pos.sideToMove ? result : -result;
            }
        }


        public static class Endgame_KRKN extends Endgame {
            public Endgame_KRKN(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int bksq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                int bnsq = pos.pieceList[((weakSide << 3) + Types.PieceType.KNIGHT)][0];
                int result = PushToEdges[bksq] + PushAway[Bitboards.SquareDistance[bksq][bnsq]];
                return strongSide == pos.sideToMove ? result : -result;
            }
        }



        public static class Endgame_KQKP extends Endgame {
            public Endgame_KQKP(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int winnerKSq = pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0];
                int loserKSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                int pawnSq = pos.pieceList[((weakSide << 3) + Types.PieceType.PAWN)][0];

                int result = PushClose[Bitboards.SquareDistance[winnerKSq][loserKSq]];

                if (((pawnSq >>> 3) ^ (weakSide * 7)) != Types.Rank.RANK_7 ||
                        Bitboards.SquareDistance[loserKSq][pawnSq] != 1 ||
                        ((Bitboards.FileABB | Bitboards.FileCBB | Bitboards.FileFBB | Bitboards.FileHBB) & Bitboards.SquareBB[pawnSq]) == 0) {
                    result += Types.Value.QueenValueEg - Types.Value.PawnValueEg;
                }

                return strongSide == pos.sideToMove ? result : -result;
            }
        }



        public static class Endgame_KQKR extends Endgame {
            public Endgame_KQKR(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int winnerKSq = pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0];
                int loserKSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];

                int result = Types.Value.QueenValueEg - Types.Value.RookValueEg + PushToEdges[loserKSq] + PushClose[Bitboards.SquareDistance[winnerKSq][loserKSq]];

                return strongSide == pos.sideToMove ? result : -result;
            }
        }



        public static class Endgame_KNNK extends Endgame {
            public Endgame_KNNK(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                return Types.Value.VALUE_DRAW;
            }
        }



        public static class Endgame_KBPsK extends Endgame {
            public Endgame_KBPsK(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                long pawns = (pos.byColorBB[strongSide] & pos.byTypeBB[Types.PieceType.PAWN]);
                int pawnsFile = (Long.numberOfTrailingZeros(pawns) & 7);

                if ((pawnsFile == Types.File.FILE_A || pawnsFile == Types.File.FILE_H) && (pawns & ~Bitboards.FileBB[pawnsFile]) == 0) {
                    int bishopSq = pos.pieceList[((strongSide << 3) + Types.PieceType.BISHOP)][0];
                    int queeningSq = (((Types.Rank.RANK_8 << 3) + pawnsFile) ^ (strongSide * 56));
                    int kingSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];

                    int sss = queeningSq ^ bishopSq;
                    if (((((sss >>> 3) ^ sss) & 1) != 0) && Bitboards.SquareDistance[queeningSq][kingSq] <= 1) {
                        return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                    }
                }

                if ((pawnsFile == Types.File.FILE_B || pawnsFile == Types.File.FILE_G) &&
                        (pos.byTypeBB[Types.PieceType.PAWN] & ~Bitboards.FileBB[pawnsFile]) == 0 &&
                        pos.st.nonPawnMaterial[weakSide] == 0 &&
                        pos.pieceCount[((weakSide << 3) + Types.PieceType.PAWN)] >= 1) {

                    int weakPawnSq = Bitboards.backmost_sq(weakSide, (pos.byColorBB[weakSide] & pos.byTypeBB[Types.PieceType.PAWN]));
                    int strongKingSq = pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0];
                    int weakKingSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                    int bishopSq = pos.pieceList[((strongSide << 3) + Types.PieceType.BISHOP)][0];

                    int sss = bishopSq ^ weakPawnSq;
                    if (((weakPawnSq >>> 3) ^ (strongSide * 7)) == Types.Rank.RANK_7 &&
                            ((pos.byColorBB[strongSide] & pos.byTypeBB[Types.PieceType.PAWN]) & Bitboards.SquareBB[(weakPawnSq + ((weakSide == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH))]) != 0 &&
                            (((((sss >>> 3) ^ sss) & 1) != 0) || pos.pieceCount[((strongSide << 3) + Types.PieceType.PAWN)] == 1)) {

                        int strongKingDist = Bitboards.SquareDistance[weakPawnSq][strongKingSq];
                        int weakKingDist = Bitboards.SquareDistance[weakPawnSq][weakKingSq];

                        if (((weakKingSq >>> 3) ^ (strongSide * 7)) >= Types.Rank.RANK_7 && weakKingDist <= 2 && weakKingDist <= strongKingDist) {
                            return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                        }
                    }
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KQKRPs extends Endgame {
            public Endgame_KQKRPs(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int kingSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                int rsq = pos.pieceList[((weakSide << 3) + Types.PieceType.ROOK)][0];

                if (((kingSq >>> 3) ^ (weakSide * 7)) <= Types.Rank.RANK_2 &&
                        ((pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0] >>> 3) ^ (weakSide * 7)) >= Types.Rank.RANK_4 &&
                        ((rsq >>> 3) ^ (weakSide * 7)) == Types.Rank.RANK_3 &&
                        ((pos.byColorBB[weakSide] & pos.byTypeBB[Types.PieceType.PAWN]) & pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KING, kingSq) & Bitboards.PawnAttacks[strongSide][rsq]) != 0) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KRPKR extends Endgame {
            public Endgame_KRPKR(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int wksq = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0]);
                int bksq = normalize(pos, strongSide, pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0]);
                int wrsq = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.ROOK)][0]);
                int wpsq = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0]);
                int brsq = normalize(pos, strongSide, pos.pieceList[((weakSide << 3) + Types.PieceType.ROOK)][0]);

                int f = (wpsq & 7);
                int r = (wpsq >>> 3);
                int queeningSq = ((Types.Rank.RANK_8 << 3) + f);
                int tempo = (pos.sideToMove == strongSide ? 1 : 0);

                if (r <= Types.Rank.RANK_5 &&
                        Bitboards.SquareDistance[bksq][queeningSq] <= 1 &&
                        wksq <= Types.Square.SQ_H5 &&
                        ((brsq >>> 3) == Types.Rank.RANK_6 || (r <= Types.Rank.RANK_3 && (wrsq >>> 3) != Types.Rank.RANK_6))) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                if (r == Types.Rank.RANK_6 &&
                        Bitboards.SquareDistance[bksq][queeningSq] <= 1 &&
                        (wksq >>> 3) + tempo <= Types.Rank.RANK_6 &&
                        ((brsq >>> 3) == Types.Rank.RANK_1 || (tempo == 0 && Bitboards.distance_as_a_function_of_TemplateFile(brsq, wpsq) >= 3))) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                if (r >= Types.Rank.RANK_6 &&
                        bksq == queeningSq &&
                        (brsq >>> 3) == Types.Rank.RANK_1 &&
                        (tempo == 0 || Bitboards.SquareDistance[wksq][wpsq] >= 2)) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                if (wpsq == Types.Square.SQ_A7 &&
                        wrsq == Types.Square.SQ_A8 &&
                        (bksq == Types.Square.SQ_H7 || bksq == Types.Square.SQ_G7) &&
                        (brsq & 7) == Types.File.FILE_A &&
                        ((brsq >>> 3) <= Types.Rank.RANK_3 || (wksq & 7) >= Types.File.FILE_D || (wksq >>> 3) <= Types.Rank.RANK_5)) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                if (r <= Types.Rank.RANK_5 &&
                        bksq == wpsq + Types.Direction.NORTH &&
                        Bitboards.SquareDistance[wksq][wpsq] - tempo >= 2 &&
                        Bitboards.SquareDistance[wksq][brsq] - tempo >= 2) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                if (r == Types.Rank.RANK_7 &&
                        f != Types.File.FILE_A &&
                        (wrsq & 7) == f &&
                        wrsq != queeningSq &&
                        (Bitboards.SquareDistance[wksq][queeningSq] < Bitboards.SquareDistance[bksq][queeningSq] - 2 + tempo) &&
                        (Bitboards.SquareDistance[wksq][queeningSq] < Bitboards.SquareDistance[bksq][wrsq] + tempo)) {
                    return Types.ScaleFactor.SCALE_FACTOR_MAX - 2 * Bitboards.SquareDistance[wksq][queeningSq];
                }

                if (f != Types.File.FILE_A &&
                        (wrsq & 7) == f &&
                        wrsq < wpsq &&
                        (Bitboards.SquareDistance[wksq][queeningSq] < Bitboards.SquareDistance[bksq][queeningSq] - 2 + tempo) &&
                        (Bitboards.SquareDistance[wksq][wpsq + Types.Direction.NORTH] < Bitboards.SquareDistance[bksq][wpsq + Types.Direction.NORTH] - 2 + tempo) &&
                        (Bitboards.SquareDistance[bksq][wrsq] + tempo >= 3 || (Bitboards.SquareDistance[wksq][queeningSq] < Bitboards.SquareDistance[bksq][wrsq] + tempo && (Bitboards.SquareDistance[wksq][wpsq + Types.Direction.NORTH] < Bitboards.SquareDistance[bksq][wrsq] + tempo)))) {
                    return Types.ScaleFactor.SCALE_FACTOR_MAX - 8 * Bitboards.SquareDistance[wpsq][queeningSq] - 2 * Bitboards.SquareDistance[wksq][queeningSq];
                }

                if (r <= Types.Rank.RANK_4 && bksq > wpsq) {
                    if ((bksq & 7) == (wpsq & 7)) {
                        return 10;
                    }
                    if (Bitboards.distance_as_a_function_of_TemplateFile(bksq, wpsq) == 1 && Bitboards.SquareDistance[wksq][bksq] > 2) {
                        return 24 - 2 * Bitboards.SquareDistance[wksq][bksq];
                    }
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KRPKB extends Endgame {
            public Endgame_KRPKB(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                if ((pos.byTypeBB[Types.PieceType.PAWN] & (Bitboards.FileABB | Bitboards.FileHBB)) != 0) {
                    int ksq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                    int bsq = pos.pieceList[((weakSide << 3) + Types.PieceType.BISHOP)][0];
                    int psq = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0];
                    int rk = ((psq >>> 3) ^ (strongSide * 7));
                    int push = ((strongSide == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH);

                    int sss = bsq ^ psq;
                    if (rk == Types.Rank.RANK_5 && !((((sss >>> 3) ^ sss) & 1) != 0)) {
                        int d = Bitboards.SquareDistance[psq + 3 * push][ksq];

                        if (d <= 2 && !(d == 0 && ksq == pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0] + 2 * push)) {
                            return 24;
                        } else {
                            return 48;
                        }
                    }

                    if (rk == Types.Rank.RANK_6 &&
                            Bitboards.SquareDistance[psq + 2 * push][ksq] <= 1 &&
                            (Bitboards.PseudoAttacks[Types.PieceType.BISHOP][bsq] & Bitboards.SquareBB[(psq + push)]) != 0 &&
                            Bitboards.distance_as_a_function_of_TemplateFile(bsq, psq) >= 2) {
                        return 8;
                    }
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KRPPKRP extends Endgame {
            public Endgame_KRPPKRP(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int wpsq1 = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0];
                int wpsq2 = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][1];
                int bksq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];

                if (pos.pawn_passed(strongSide, wpsq1) || pos.pawn_passed(strongSide, wpsq2)) {
                    return Types.ScaleFactor.SCALE_FACTOR_NONE;
                }

                int r = Math.max(((wpsq1 >>> 3) ^ (strongSide * 7)), ((wpsq2 >>> 3) ^ (strongSide * 7)));

                if (Bitboards.distance_as_a_function_of_TemplateFile(bksq, wpsq1) <= 1 &&
                        Bitboards.distance_as_a_function_of_TemplateFile(bksq, wpsq2) <= 1 &&
                        ((bksq >>> 3) ^ (strongSide * 7)) > r) {
                    return KRPPKRPScaleFactors[r];
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KPsK extends Endgame {
            public Endgame_KPsK(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int ksq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                long pawns = (pos.byColorBB[strongSide] & pos.byTypeBB[Types.PieceType.PAWN]);

                if ((pawns & ~Bitboards.ForwardRanksBB[weakSide][(ksq >>> 3)]) == 0 &&
                        !((pawns & ~Bitboards.FileABB) != 0 && (pawns & ~Bitboards.FileHBB) != 0) &&
                        Bitboards.distance_as_a_function_of_TemplateFile(ksq, Long.numberOfTrailingZeros(pawns)) <= 1) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KBPKB extends Endgame {
            public Endgame_KBPKB(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int pawnSq = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0];
                int strongBishopSq = pos.pieceList[((strongSide << 3) + Types.PieceType.BISHOP)][0];
                int weakBishopSq = pos.pieceList[((weakSide << 3) + Types.PieceType.BISHOP)][0];
                int weakKingSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];

                int sss = weakKingSq ^ strongBishopSq;
                if ((weakKingSq & 7) == (pawnSq & 7) &&
                        ((pawnSq >>> 3) ^ (strongSide * 7)) < ((weakKingSq >>> 3) ^ (strongSide * 7)) &&
                        (((((sss >>> 3) ^ sss) & 1) != 0) || ((weakKingSq >>> 3) ^ (strongSide * 7)) <= Types.Rank.RANK_6)) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                int sssss = strongBishopSq ^ weakBishopSq;
                if (((((sssss >>> 3) ^ sssss) & 1) != 0)) {
                    if (((pawnSq >>> 3) ^ (strongSide * 7)) <= Types.Rank.RANK_5) {
                        return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                    }

                    long path = Bitboards.ForwardFileBB[strongSide][pawnSq];

                    if ((path & (pos.byColorBB[weakSide] & pos.byTypeBB[Types.PieceType.KING])) != 0) {
                        return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                    }

                    if ((pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, weakBishopSq) & path) != 0 && Bitboards.SquareDistance[weakBishopSq][pawnSq] >= 3) {
                        return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                    }
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KBPPKB extends Endgame {
            public Endgame_KBPPKB(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int wbsq = pos.pieceList[((strongSide << 3) + Types.PieceType.BISHOP)][0];
                int bbsq = pos.pieceList[((weakSide << 3) + Types.PieceType.BISHOP)][0];

                int sssss = wbsq ^ bbsq;
                if (!((((sssss >>> 3) ^ sssss) & 1) != 0)) {
                    return Types.ScaleFactor.SCALE_FACTOR_NONE;
                }

                int ksq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];
                int psq1 = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0];
                int psq2 = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][1];
                int r1 = (psq1 >>> 3);
                int r2 = (psq2 >>> 3);
                int blockSq1, blockSq2;

                if (((psq1 >>> 3) ^ (strongSide * 7)) > ((psq2 >>> 3) ^ (strongSide * 7))) {
                    blockSq1 = psq1 + ((strongSide == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH);
                    blockSq2 = (((psq1 >>> 3) << 3) + (psq2 & 7));
                } else {
                    blockSq1 = psq2 + ((strongSide == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH);
                    blockSq2 = (((psq2 >>> 3) << 3) + (psq1 & 7));
                }

                switch (Bitboards.distance_as_a_function_of_TemplateFile(psq1, psq2)) {
                    case 0: {
                        int ssssss = ksq ^ wbsq;
                        if ((ksq & 7) == (blockSq1 & 7) &&
                                ((ksq >>> 3) ^ (strongSide * 7)) >= ((blockSq1 >>> 3) ^ (strongSide * 7)) &&
                                ((((ssssss >>> 3) ^ ssssss) & 1) != 0)) {
                            return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                        } else {
                            return Types.ScaleFactor.SCALE_FACTOR_NONE;
                        }
                    }
                    case 1: {
                        int ssssss = ksq ^ wbsq;
                        if (ksq == blockSq1 &&
                                ((((ssssss >>> 3) ^ ssssss) & 1) != 0) &&
                                (bbsq == blockSq2 || (pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, blockSq2) & (pos.byColorBB[weakSide] & pos.byTypeBB[Types.PieceType.BISHOP])) != 0 || ((r1 < r2) ? (r2 - r1) : (r1 - r2)) >= 2)) {
                            return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                        } else if (ksq == blockSq2 &&
                                ((((ssssss >>> 3) ^ ssssss) & 1) != 0) &&
                                (bbsq == blockSq1 || (pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, blockSq1) & (pos.byColorBB[weakSide] & pos.byTypeBB[Types.PieceType.BISHOP])) != 0)) {
                            return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                        } else {
                            return Types.ScaleFactor.SCALE_FACTOR_NONE;
                        }
                    }
                    default: {
                        return Types.ScaleFactor.SCALE_FACTOR_NONE;
                    }
                }
            }
        }



        public static class Endgame_KBPKN extends Endgame {
            public Endgame_KBPKN(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int pawnSq = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0];
                int strongBishopSq = pos.pieceList[((strongSide << 3) + Types.PieceType.BISHOP)][0];
                int weakKingSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];

                int ssssss = weakKingSq ^ strongBishopSq;
                if ((weakKingSq & 7) == (pawnSq & 7) &&
                        ((pawnSq >>> 3) ^ (strongSide * 7)) < ((weakKingSq >>> 3) ^ (strongSide * 7)) &&
                        (((((ssssss >>> 3) ^ ssssss) & 1) != 0) || ((weakKingSq >>> 3) ^ (strongSide * 7)) <= Types.Rank.RANK_6)) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KNPK extends Endgame {
            public Endgame_KNPK(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int pawnSq = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0]);
                int weakKingSq = normalize(pos, strongSide, pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0]);

                if (pawnSq == Types.Square.SQ_A7 && Bitboards.SquareDistance[Types.Square.SQ_A8][weakKingSq] <= 1) {
                    return Types.ScaleFactor.SCALE_FACTOR_DRAW;
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KNPKB extends Endgame {
            public Endgame_KNPKB(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int pawnSq = pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0];
                int bishopSq = pos.pieceList[((weakSide << 3) + Types.PieceType.BISHOP)][0];
                int weakKingSq = pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0];

                if ((Bitboards.ForwardFileBB[strongSide][pawnSq] & pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, bishopSq)) != 0) {
                    return Bitboards.SquareDistance[weakKingSq][pawnSq];
                }

                return Types.ScaleFactor.SCALE_FACTOR_NONE;
            }
        }



        public static class Endgame_KPKP extends Endgame {
            public Endgame_KPKP(int E, int c) {
                super(E, c);
            }

            @Override
            public int convertToInteger(Position pos) {
                int wksq = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.KING)][0]);
                int bksq = normalize(pos, strongSide, pos.pieceList[((weakSide << 3) + Types.PieceType.KING)][0]);
                int psq = normalize(pos, strongSide, pos.pieceList[((strongSide << 3) + Types.PieceType.PAWN)][0]);

                int us = strongSide == pos.sideToMove ? Types.Color.WHITE : Types.Color.BLACK;

                if ((psq >>> 3) >= Types.Rank.RANK_5 && (psq & 7) != Types.File.FILE_A) {
                    return Types.ScaleFactor.SCALE_FACTOR_NONE;
                }

                return Bitbases.probe(wksq, psq, bksq, us) ? Types.ScaleFactor.SCALE_FACTOR_NONE : Types.ScaleFactor.SCALE_FACTOR_DRAW;
            }
        }
    }


    public static class Eval {

        public static final int Tempo = 20;

        public static int Contempt = Types.Score.SCORE_ZERO;

        public static int S(int mg, int eg) {
            return ((eg << 16) | (mg & 0xffff));
        }

        public static final int[][] MobilityBonus = {
                { S(-75,-76), S(-57,-54), S( -9,-28), S( -2,-10), S(  6,  5), S( 14, 12), S( 22, 26), S( 29, 29), S( 36, 29), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                { S(-48,-59), S(-20,-23), S( 16, -3), S( 26, 13), S( 38, 24), S( 51, 42), S( 55, 54), S( 63, 57), S( 63, 65), S( 68, 73), S( 81, 78), S( 81, 86), S( 91, 88), S( 98, 97), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                { S(-58,-76), S(-27,-18), S(-15, 28), S(-10, 55), S( -5, 69), S( -2, 82), S(  9,112), S( 16,118), S( 30,132), S( 29,142), S( 32,155), S( 38,165), S( 46,166), S( 48,169), S( 58,171), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                { S(-39,-36), S(-21,-15), S(  3,  8), S(  3, 18), S( 14, 34), S( 22, 54), S( 28, 61), S( 41, 73), S( 43, 79), S( 48, 92), S( 56, 94), S( 60,104), S( 60,113), S( 66,120), S( 67,123), S( 70,126), S( 71,133), S( 73,136), S( 79,140), S( 88,143), S( 88,148), S( 99,166), S(102,170), S(102,175), S(106,184), S(109,191), S(113,206), S(116,212), 0, 0, 0, 0}};

        public static final int[][] Outpost = {
                { S(22, 6), S(36,12)},
                { S( 9, 2), S(15, 5)}};

        public static final int[] RookOnFile = {S(20, 7), S(45, 20)};

        public static final int[] ThreatByMinor = {S(0, 0), S(0, 33), S(45, 43), S(46, 47), S(72, 107), S(48, 118), 0, 0};

        public static final int[] ThreatByRook = {S(0, 0), S(0, 25), S(40, 62), S(40, 59), S(0, 34), S(35, 48), 0, 0};

        public static final int[] ThreatByKing = {S(3, 62), S(9, 138)};

        public static final int[][] Passed = {{0, 5, 5, 31, 73, 166, 252, 0}, {0, 7, 14, 38, 73, 166, 252, 0}};

        public static final int[] PassedFile = {S(  9, 10), S( 2, 10), S( 1, -8), S(-20,-12), S(-20,-12), S( 1, -8), S( 2, 10), S(  9, 10)};

        public static final int[] RankFactor = {0, 0, 0, 2, 6, 11, 16, 0};

        public static final int[] KingProtector = {S(-3, -5), S(-4, -3), S(-3, 0), S(-1, 1)};

        public static final int MinorBehindPawn       = S( 16,  0);
        public static final int BishopPawns           = S(  8, 12);
        public static final int LongRangedBishop      = S( 22,  0);
        public static final int RookOnPawn            = S(  8, 24);
        public static final int TrappedRook           = S( 92,  0);
        public static final int WeakQueen             = S( 50, 10);
        public static final int CloseEnemies          = S(  7,  0);
        public static final int PawnlessFlank         = S( 20, 80);
        public static final int ThreatBySafePawn      = S(192,175);
        public static final int ThreatByRank          = S( 16,  3);
        public static final int Hanging               = S( 48, 27);
        public static final int WeakUnopposedPawn     = S(  5, 25);
        public static final int ThreatByPawnPush      = S( 38, 22);
        public static final int ThreatByAttackOnQueen = S( 38, 22);
        public static final int HinderPassedPawn      = S(  7,  0);
        public static final int TrappedBishopA1H1     = S( 50, 50);

        public static final int[] KingAttackWeights = {0, 0, 78, 56, 45, 11, 0, 0};

        public static final int QueenSafeCheck  = 780;
        public static final int RookSafeCheck   = 880;
        public static final int BishopSafeCheck = 435;
        public static final int KnightSafeCheck = 790;

        public static final int LazyThreshold  = 1500;
        public static final int SpaceThreshold = 12222;


        public static int evaluate(Position pos) {
            return (new Evaluation(pos)).value() + Tempo;
        }


        public static final long Center = (Bitboards.FileDBB | Bitboards.FileEBB) & (Bitboards.Rank4BB | Bitboards.Rank5BB);
        public static final long QueenSide = Bitboards.FileABB | Bitboards.FileBBB | Bitboards.FileCBB | Bitboards.FileDBB;
        public static final long CenterFiles = Bitboards.FileCBB | Bitboards.FileDBB | Bitboards.FileEBB | Bitboards.FileFBB;
        public static final long KingSide = Bitboards.FileEBB | Bitboards.FileFBB | Bitboards.FileGBB | Bitboards.FileHBB;

        public static final long[] KingFlank = {QueenSide, QueenSide, QueenSide, CenterFiles, CenterFiles, KingSide, KingSide, KingSide};


        public static class Term {
            public static final int MATERIAL = 8;
            public static final int IMBALANCE = 9;
            public static final int MOBILITY = 10;
            public static final int THREAT = 11;
            public static final int PASSED = 12;
            public static final int SPACE = 13;
            public static final int INITIATIVE = 14;
            public static final int TOTAL = 15;
            public static final int TERM_NB = 16;
        }

        public static double[][][] scores = new double[Term.TERM_NB][Types.Color.COLOR_NB][Types.Phase.PHASE_NB];

        public static double to_cp(int v) {
            return ((double) v) / Types.Value.PawnValueEg;
        }

        public static void add_as_a_function_of_Int_Color_Score(int idx, int c, int s) {
            scores[idx][c][Types.Phase.MG] = to_cp(((short) s));
            scores[idx][c][Types.Phase.EG] = to_cp(s >> 16);
        }

        public static void add_as_a_function_of_Int_Score_Score(int idx, int w) {
            int b = Types.Score.SCORE_ZERO;
            add_as_a_function_of_Int_Color_Score(idx, Types.Color.WHITE, w);
            add_as_a_function_of_Int_Color_Score(idx, Types.Color.BLACK, b);
        }

        public static void add_as_a_function_of_Int_Score_Score(int idx, int w, int b) {
            add_as_a_function_of_Int_Color_Score(idx, Types.Color.WHITE, w);
            add_as_a_function_of_Int_Color_Score(idx, Types.Color.BLACK, b);
        }

        public static String operatorInsertion(int t) {
            StringBuilder stringBuilder = new StringBuilder();
            if (t == Term.MATERIAL || t == Term.IMBALANCE || t == Types.PieceType.PAWN || t == Term.INITIATIVE || t == Term.TOTAL) {
                stringBuilder.append("  ---   --- |   ---   --- | ");
            } else {
                stringBuilder.append(String.format("%5.2f", scores[t][Types.Color.WHITE][Types.Phase.MG]) + " ");
                stringBuilder.append(String.format("%5.2f", scores[t][Types.Color.WHITE][Types.Phase.EG]) + " | ");
                stringBuilder.append(String.format("%5.2f", scores[t][Types.Color.BLACK][Types.Phase.MG]) + " ");
                stringBuilder.append(String.format("%5.2f", scores[t][Types.Color.BLACK][Types.Phase.EG]) + " | ");
            }

            stringBuilder.append(String.format("%5.2f", scores[t][Types.Color.WHITE][Types.Phase.MG] - scores[t][Types.Color.BLACK][Types.Phase.MG]) + " ");
            stringBuilder.append(String.format("%5.2f", scores[t][Types.Color.WHITE][Types.Phase.EG] - scores[t][Types.Color.BLACK][Types.Phase.EG]) + " \n");

            return stringBuilder.toString();
        }


        public static class Evaluation {

            public final Position pos;
            public Material.Entry me;
            public Pawns.Entry pe;
            public long[] mobilityArea = new long[Types.Color.COLOR_NB];
            public int[] mobility = {Types.Score.SCORE_ZERO, Types.Score.SCORE_ZERO};

            public long[][] attackedBy = new long[Types.Color.COLOR_NB][Types.PieceType.PIECE_TYPE_NB];
            public long[] attackedBy2 = new long[Types.Color.COLOR_NB];
            public long[] kingRing = new long[Types.Color.COLOR_NB];
            public int[] kingAttackersCount = new int[Types.Color.COLOR_NB];
            public int[] kingAttackersWeight = new int[Types.Color.COLOR_NB];
            public int[] kingAdjacentZoneAttacksCount = new int[Types.Color.COLOR_NB];


            public Evaluation(Position p) {
                pos = p;
            }


            public int evaluate_pieces(int Us, int Pt) {
                final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);
                final long OutpostRanks = (Us == Types.Color.WHITE ? Bitboards.Rank4BB | Bitboards.Rank5BB | Bitboards.Rank6BB : Bitboards.Rank5BB | Bitboards.Rank4BB | Bitboards.Rank3BB);
                final int[] pl = pos.pieceList[((Us << 3) + Pt)];
                int pl_Index = 0;

                long b, bb;
                int s;
                int score = Types.Score.SCORE_ZERO;

                attackedBy[Us][Pt] = 0;

                if (Pt == Types.PieceType.QUEEN) {
                    attackedBy[Us][Types.PieceType.QUEEN_DIAGONAL] = 0;
                }

                while ((s = pl[pl_Index++]) != Types.Square.SQ_NONE) {
                    b = Pt == Types.PieceType.BISHOP	?	Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, s, pos.byTypeBB[Types.PieceType.ALL_PIECES] ^ pos.byTypeBB[Types.PieceType.QUEEN]) :
                            Pt == Types.PieceType.ROOK		?	Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, s, pos.byTypeBB[Types.PieceType.ALL_PIECES] ^ pos.byTypeBB[Types.PieceType.QUEEN] ^ (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.ROOK])) :
                                    pos.attacks_from_as_a_function_of_TemplatePieceType(Pt, s);

                    if (((pos.st.blockersForKing[Us] & pos.byColorBB[Us]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[((Us << 3) + Types.PieceType.KING)][0]][s];
                    }

                    attackedBy2[Us] |= attackedBy[Us][Types.PieceType.ALL_PIECES] & b;
                    attackedBy[Us][Types.PieceType.ALL_PIECES] |= attackedBy[Us][Pt] |= b;

                    if (Pt == Types.PieceType.QUEEN) {
                        attackedBy[Us][Types.PieceType.QUEEN_DIAGONAL] |= b & Bitboards.PseudoAttacks[Types.PieceType.BISHOP][s];
                    }

                    if ((b & kingRing[Them]) != 0) {
                        kingAttackersCount[Us]++;
                        kingAttackersWeight[Us] += KingAttackWeights[Pt];
                        kingAdjacentZoneAttacksCount[Us] += Long.bitCount(b & attackedBy[Them][Types.PieceType.KING]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[Us]);

                    mobility[Us] += MobilityBonus[Pt - 2][mob];

                    score += KingProtector[Pt - 2] * Bitboards.SquareDistance[s][pos.pieceList[((Us << 3) + Types.PieceType.KING)][0]];

                    if (Pt == Types.PieceType.BISHOP || Pt == Types.PieceType.KNIGHT) {
                        bb = OutpostRanks & ~pe.pawn_attacks_span(Them);
                        if ((bb & Bitboards.SquareBB[s]) != 0) {
                            score += Outpost[Pt == Types.PieceType.BISHOP ? 1 : 0][(attackedBy[Us][Types.PieceType.PAWN] & Bitboards.SquareBB[s]) != 0 ? 1 : 0] * 2;
                        } else {
                            bb &= b & ~pos.byColorBB[Us];
                            if (bb != 0) {
                                score += Outpost[Pt == Types.PieceType.BISHOP ? 1 : 0][(attackedBy[Us][Types.PieceType.PAWN] & bb) != 0 ? 1 : 0];
                            }
                        }

                        if (((s >>> 3) ^ (Us * 7)) < Types.Rank.RANK_5 &&
                                (pos.byTypeBB[Types.PieceType.PAWN] & Bitboards.SquareBB[(s + ((Us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH))]) != 0) {
                            score += MinorBehindPawn;
                        }

                        if (Pt == Types.PieceType.BISHOP) {
                            score -= BishopPawns * pe.pawns_on_same_color_squares(Us, s);

                            if (Bitboards.more_than_one(Center & (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, s, pos.byTypeBB[Types.PieceType.PAWN]) | Bitboards.SquareBB[s]))) {
                                score += LongRangedBishop;
                            }
                        }

                        if (Pt == Types.PieceType.BISHOP &&
                                pos.chess960 &&
                                (s == (Types.Square.SQ_A1 ^ (Us * 56)) || s == (Types.Square.SQ_H1 ^ (Us * 56)))) {
                            int d = ((Us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH) + ((s & 7) == Types.File.FILE_A ? Types.Direction.EAST : Types.Direction.WEST);
                            if (pos.board[s + d] == ((Us << 3) + Types.PieceType.PAWN)) {
                                score -= pos.board[s + d + ((Us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH)] != Types.Piece.NO_PIECE   ?   TrappedBishopA1H1 * 4 :
                                        pos.board[s + d + d] == ((Us << 3) + Types.PieceType.PAWN)	?	TrappedBishopA1H1 * 2 : TrappedBishopA1H1;
                            }
                        }
                    }

                    if (Pt == Types.PieceType.ROOK) {
                        if (((s >>> 3) ^ (Us * 7)) >= Types.Rank.RANK_5) {
                            score += RookOnPawn * Long.bitCount((pos.byColorBB[Them] & pos.byTypeBB[Types.PieceType.PAWN]) & Bitboards.PseudoAttacks[Types.PieceType.ROOK][s]);
                        }

                        if (pe.semiopen_file(Us, (s & 7)) != 0) {
                            score += RookOnFile[pe.semiopen_file(Them, (s & 7)) != 0 ? 1 : 0];
                        } else if (mob <= 3) {
                            int ksq = pos.pieceList[((Us << 3) + Types.PieceType.KING)][0];

                            if ((((ksq & 7) < Types.File.FILE_E) == ((s & 7) < (ksq & 7))) &&
                                    pe.semiopen_side(Us, (ksq & 7), (s & 7) < (ksq & 7)) == 0) {
                                score -= (TrappedRook - ((mob * 22) & 0xffff)) * (1 + ((pos.st.castlingRights & ((Types.CastlingRight.WHITE_OO | Types.CastlingRight.WHITE_OOO) << (2 * Us))) == 0 ? 1 : 0));
                            }
                        }
                    }

                    if (Pt == Types.PieceType.QUEEN) {
                        long[] pinners = {0};
                        if (pos.slider_blockers((pos.byColorBB[Them] & (pos.byTypeBB[Types.PieceType.ROOK] | pos.byTypeBB[Types.PieceType.BISHOP])), s, pinners) != 0) {
                            score -= WeakQueen;
                        }
                    }
                }

                return score;
            }

            public int evaluate_pieces_White_Knight() {
                final int[] pl = pos.pieceList[2];
                int pl_Index = 0;

                long b, bb;
                int s;
                int score = 0;

                attackedBy[0][2] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    b = Bitboards.PseudoAttacks[2][s];

                    if (((pos.st.blockersForKing[0] & pos.byColorBB[0]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[6][0]][s];
                    }

                    attackedBy2[0] |= attackedBy[0][0] & b;
                    attackedBy[0][0] |= attackedBy[0][2] |= b;

                    if ((b & kingRing[1]) != 0) {
                        kingAttackersCount[0]++;
                        kingAttackersWeight[0] += KingAttackWeights[2];
                        kingAdjacentZoneAttacksCount[0] += Long.bitCount(b & attackedBy[1][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[0]);

                    mobility[0] += MobilityBonus[0][mob];

                    score += KingProtector[0] * Bitboards.SquareDistance[s][pos.pieceList[6][0]];

                    bb = 281474959933440L & ~pe.pawn_attacks_span(1);
                    if ((bb & Bitboards.SquareBB[s]) != 0) {
                        score += Outpost[0][(attackedBy[0][1] & Bitboards.SquareBB[s]) != 0 ? 1 : 0] * 2;
                    } else {
                        bb &= b & ~pos.byColorBB[0];
                        if (bb != 0) {
                            score += Outpost[0][(attackedBy[0][1] & bb) != 0 ? 1 : 0];
                        }
                    }

                    if ((s >>> 3) < 4 && (pos.byTypeBB[1] & Bitboards.SquareBB[s + 8]) != 0) {
                        score += MinorBehindPawn;
                    }
                }
                return score;
            }

            public int evaluate_pieces_Black_Knight() {
                final int[] pl = pos.pieceList[10];
                int pl_Index = 0;

                long b, bb;
                int s;
                int score = 0;

                attackedBy[1][2] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    b = Bitboards.PseudoAttacks[2][s];

                    if (((pos.st.blockersForKing[1] & pos.byColorBB[1]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[14][0]][s];
                    }

                    attackedBy2[1] |= attackedBy[1][0] & b;
                    attackedBy[1][0] |= attackedBy[1][2] |= b;

                    if ((b & kingRing[0]) != 0) {
                        kingAttackersCount[1]++;
                        kingAttackersWeight[1] += KingAttackWeights[2];
                        kingAdjacentZoneAttacksCount[1] += Long.bitCount(b & attackedBy[0][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[1]);

                    mobility[1] += MobilityBonus[0][mob];

                    score += KingProtector[0] * Bitboards.SquareDistance[s][pos.pieceList[14][0]];

                    bb = 1099511562240L & ~pe.pawn_attacks_span(0);
                    if ((bb & Bitboards.SquareBB[s]) != 0) {
                        score += Outpost[0][(attackedBy[1][1] & Bitboards.SquareBB[s]) != 0 ? 1 : 0] * 2;
                    } else {
                        bb &= b & ~pos.byColorBB[1];
                        if (bb != 0) {
                            score += Outpost[0][(attackedBy[1][1] & bb) != 0 ? 1 : 0];
                        }
                    }

                    if (((s >>> 3) ^ 7) < 4 && (pos.byTypeBB[1] & Bitboards.SquareBB[s - 8]) != 0) {
                        score += MinorBehindPawn;
                    }
                }
                return score;
            }

            public int evaluate_pieces_White_Bishop() {
                final int[] pl = pos.pieceList[3];
                int pl_Index = 0;

                long b, bb;
                int s;
                int score = 0;

                attackedBy[0][3] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    Bitboards.Magic m = Bitboards.BishopMagics[s];
                    b = m.attacks[m.index(pos.byTypeBB[0] ^ pos.byTypeBB[5]) + m.attacksArrayFirstIndex];

                    if (((pos.st.blockersForKing[0] & pos.byColorBB[0]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[6][0]][s];
                    }

                    attackedBy2[0] |= attackedBy[0][0] & b;
                    attackedBy[0][0] |= attackedBy[0][3] |= b;

                    if ((b & kingRing[1]) != 0) {
                        kingAttackersCount[0]++;
                        kingAttackersWeight[0] += KingAttackWeights[3];
                        kingAdjacentZoneAttacksCount[0] += Long.bitCount(b & attackedBy[1][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[0]);

                    mobility[0] += MobilityBonus[1][mob];

                    score += KingProtector[1] * Bitboards.SquareDistance[s][pos.pieceList[6][0]];

                    bb = 281474959933440L & ~pe.pawn_attacks_span(1);
                    if ((bb & Bitboards.SquareBB[s]) != 0) {
                        score += Outpost[1][(attackedBy[0][1] & Bitboards.SquareBB[s]) != 0 ? 1 : 0] * 2;
                    } else {
                        bb &= b & ~pos.byColorBB[0];
                        if (bb != 0) {
                            score += Outpost[1][(attackedBy[0][1] & bb) != 0 ? 1 : 0];
                        }
                    }

                    if ((s >>> 3) < 4 && (pos.byTypeBB[1] & Bitboards.SquareBB[s + 8]) != 0) {
                        score += MinorBehindPawn;
                    }

                    score -= BishopPawns * pe.pawns_on_same_color_squares(0, s);

                    if (Bitboards.more_than_one(Center & (m.attacks[m.index(pos.byTypeBB[1]) + m.attacksArrayFirstIndex] | Bitboards.SquareBB[s]))) {
                        score += LongRangedBishop;
                    }
                }
                return score;
            }

            public int evaluate_pieces_Black_Bishop() {
                final int[] pl = pos.pieceList[11];
                int pl_Index = 0;

                long b, bb;
                int s;
                int score = 0;

                attackedBy[1][3] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    Bitboards.Magic m = Bitboards.BishopMagics[s];
                    b = m.attacks[m.index(pos.byTypeBB[0] ^ pos.byTypeBB[5]) + m.attacksArrayFirstIndex];

                    if (((pos.st.blockersForKing[1] & pos.byColorBB[1]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[14][0]][s];
                    }

                    attackedBy2[1] |= attackedBy[1][0] & b;
                    attackedBy[1][0] |= attackedBy[1][3] |= b;

                    if ((b & kingRing[0]) != 0) {
                        kingAttackersCount[1]++;
                        kingAttackersWeight[1] += KingAttackWeights[3];
                        kingAdjacentZoneAttacksCount[1] += Long.bitCount(b & attackedBy[0][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[1]);

                    mobility[1] += MobilityBonus[1][mob];

                    score += KingProtector[1] * Bitboards.SquareDistance[s][pos.pieceList[14][0]];

                    bb = 1099511562240L & ~pe.pawn_attacks_span(0);
                    if ((bb & Bitboards.SquareBB[s]) != 0) {
                        score += Outpost[1][(attackedBy[1][1] & Bitboards.SquareBB[s]) != 0 ? 1 : 0] * 2;
                    } else {
                        bb &= b & ~pos.byColorBB[1];
                        if (bb != 0) {
                            score += Outpost[1][(attackedBy[1][1] & bb) != 0 ? 1 : 0];
                        }
                    }

                    if (((s >>> 3) ^ 7) < 4 && (pos.byTypeBB[1] & Bitboards.SquareBB[s - 8]) != 0) {
                        score += MinorBehindPawn;
                    }

                    score -= BishopPawns * pe.pawns_on_same_color_squares(1, s);

                    if (Bitboards.more_than_one(Center & (m.attacks[m.index(pos.byTypeBB[1]) + m.attacksArrayFirstIndex] | Bitboards.SquareBB[s]))) {
                        score += LongRangedBishop;
                    }
                }
                return score;
            }

            public int evaluate_pieces_White_Rook() {
                final int[] pl = pos.pieceList[4];
                int pl_Index = 0;

                long b;
                int s;
                int score = 0;

                attackedBy[0][4] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    Bitboards.Magic m = Bitboards.RookMagics[s];
                    b = m.attacks[m.index(pos.byTypeBB[0] ^ pos.byTypeBB[5] ^ (pos.byColorBB[0] & pos.byTypeBB[4])) + m.attacksArrayFirstIndex];

                    if (((pos.st.blockersForKing[0] & pos.byColorBB[0]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[6][0]][s];
                    }

                    attackedBy2[0] |= attackedBy[0][0] & b;
                    attackedBy[0][0] |= attackedBy[0][4] |= b;

                    if ((b & kingRing[1]) != 0) {
                        kingAttackersCount[0]++;
                        kingAttackersWeight[0] += KingAttackWeights[4];
                        kingAdjacentZoneAttacksCount[0] += Long.bitCount(b & attackedBy[1][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[0]);

                    mobility[0] += MobilityBonus[2][mob];

                    score += KingProtector[2] * Bitboards.SquareDistance[s][pos.pieceList[6][0]];

                    if ((s >>> 3) >= 4) {
                        score += RookOnPawn * Long.bitCount((pos.byColorBB[1] & pos.byTypeBB[1]) & Bitboards.PseudoAttacks[4][s]);
                    }

                    if (pe.semiopen_file(0, (s & 7)) != 0) {
                        score += RookOnFile[pe.semiopen_file(1, (s & 7)) != 0 ? 1 : 0];
                    } else if (mob <= 3) {
                        int ksq = pos.pieceList[6][0];

                        if ((((ksq & 7) < 4) == ((s & 7) < (ksq & 7))) &&
                                pe.semiopen_side(0, (ksq & 7), (s & 7) < (ksq & 7)) == 0) {
                            score -= (TrappedRook - ((mob * 22) & 0xffff)) * (1 + ((pos.st.castlingRights & 3) == 0 ? 1 : 0));
                        }
                    }
                }
                return score;
            }

            public int evaluate_pieces_Black_Rook() {
                final int[] pl = pos.pieceList[12];
                int pl_Index = 0;

                long b;
                int s;
                int score = 0;

                attackedBy[1][4] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    Bitboards.Magic m = Bitboards.RookMagics[s];
                    b = m.attacks[m.index(pos.byTypeBB[0] ^ pos.byTypeBB[5] ^ (pos.byColorBB[1] & pos.byTypeBB[4])) + m.attacksArrayFirstIndex];

                    if (((pos.st.blockersForKing[1] & pos.byColorBB[1]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[14][0]][s];
                    }

                    attackedBy2[1] |= attackedBy[1][0] & b;
                    attackedBy[1][0] |= attackedBy[1][4] |= b;

                    if ((b & kingRing[0]) != 0) {
                        kingAttackersCount[1]++;
                        kingAttackersWeight[1] += KingAttackWeights[4];
                        kingAdjacentZoneAttacksCount[1] += Long.bitCount(b & attackedBy[0][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[1]);

                    mobility[1] += MobilityBonus[2][mob];

                    score += KingProtector[2] * Bitboards.SquareDistance[s][pos.pieceList[14][0]];

                    if (((s >>> 3) ^ 7) >= 4) {
                        score += RookOnPawn * Long.bitCount((pos.byColorBB[0] & pos.byTypeBB[1]) & Bitboards.PseudoAttacks[4][s]);
                    }

                    if (pe.semiopen_file(1, (s & 7)) != 0) {
                        score += RookOnFile[pe.semiopen_file(0, (s & 7)) != 0 ? 1 : 0];
                    } else if (mob <= 3) {
                        int ksq = pos.pieceList[14][0];

                        if ((((ksq & 7) < 4) == ((s & 7) < (ksq & 7))) &&
                                pe.semiopen_side(1, (ksq & 7), (s & 7) < (ksq & 7)) == 0) {
                            score -= (TrappedRook - ((mob * 22) & 0xffff)) * (1 + ((pos.st.castlingRights & 12) == 0 ? 1 : 0));
                        }
                    }
                }
                return score;
            }

            public int evaluate_pieces_White_Queen() {
                final int[] pl = pos.pieceList[5];
                int pl_Index = 0;

                long b;
                int s;
                int score = 0;

                attackedBy[0][5] = 0;

                attackedBy[0][7] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    Bitboards.Magic m = Bitboards.RookMagics[s];
                    b = m.attacks[m.index(pos.byTypeBB[0]) + m.attacksArrayFirstIndex];
                    m = Bitboards.BishopMagics[s];
                    b |= m.attacks[m.index(pos.byTypeBB[0]) + m.attacksArrayFirstIndex];

                    if (((pos.st.blockersForKing[0] & pos.byColorBB[0]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[6][0]][s];
                    }

                    attackedBy2[0] |= attackedBy[0][0] & b;
                    attackedBy[0][0] |= attackedBy[0][5] |= b;

                    attackedBy[0][7] |= b & Bitboards.PseudoAttacks[3][s];

                    if ((b & kingRing[1]) != 0) {
                        kingAttackersCount[0]++;
                        kingAttackersWeight[0] += KingAttackWeights[5];
                        kingAdjacentZoneAttacksCount[0] += Long.bitCount(b & attackedBy[1][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[0]);

                    mobility[0] += MobilityBonus[3][mob];

                    score += KingProtector[3] * Bitboards.SquareDistance[s][pos.pieceList[6][0]];

                    long[] pinners = {0};
                    if (pos.slider_blockers((pos.byColorBB[1] & (pos.byTypeBB[4] | pos.byTypeBB[3])), s, pinners) != 0) {
                        score -= WeakQueen;
                    }
                }
                return score;
            }

            public int evaluate_pieces_Black_Queen() {
                final int[] pl = pos.pieceList[13];
                int pl_Index = 0;

                long b;
                int s;
                int score = 0;

                attackedBy[1][5] = 0;

                attackedBy[1][7] = 0;

                while ((s = pl[pl_Index++]) != 64) {
                    Bitboards.Magic m = Bitboards.RookMagics[s];
                    b = m.attacks[m.index(pos.byTypeBB[0]) + m.attacksArrayFirstIndex];
                    m = Bitboards.BishopMagics[s];
                    b |= m.attacks[m.index(pos.byTypeBB[0]) + m.attacksArrayFirstIndex];

                    if (((pos.st.blockersForKing[1] & pos.byColorBB[1]) & Bitboards.SquareBB[s]) != 0) {
                        b &= Bitboards.LineBB[pos.pieceList[14][0]][s];
                    }

                    attackedBy2[1] |= attackedBy[1][0] & b;
                    attackedBy[1][0] |= attackedBy[1][5] |= b;

                    attackedBy[1][7] |= b & Bitboards.PseudoAttacks[3][s];

                    if ((b & kingRing[0]) != 0) {
                        kingAttackersCount[1]++;
                        kingAttackersWeight[1] += KingAttackWeights[5];
                        kingAdjacentZoneAttacksCount[1] += Long.bitCount(b & attackedBy[0][6]);
                    }

                    int mob = Long.bitCount(b & mobilityArea[1]);

                    mobility[1] += MobilityBonus[3][mob];

                    score += KingProtector[3] * Bitboards.SquareDistance[s][pos.pieceList[14][0]];

                    long[] pinners = {0};
                    if (pos.slider_blockers((pos.byColorBB[0] & (pos.byTypeBB[4] | pos.byTypeBB[3])), s, pinners) != 0) {
                        score -= WeakQueen;
                    }
                }
                return score;
            }


            public int evaluate_king(int Us) {
                final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);
                final long Camp = (Us == Types.Color.WHITE ? Bitboards.AllSquares ^ Bitboards.Rank6BB ^ Bitboards.Rank7BB ^ Bitboards.Rank8BB :
                        Bitboards.AllSquares ^ Bitboards.Rank1BB ^ Bitboards.Rank2BB ^ Bitboards.Rank3BB);

                final int ksq = pos.pieceList[((Us << 3) + Types.PieceType.KING)][0];
                long weak, b, b1, b2, safe, unsafeChecks;

                int score = pe.king_safety_as_a_function_of_TemplateColor(Us, pos, ksq);

                if (kingAttackersCount[Them] > (1 - pos.pieceCount[((Them << 3) + Types.PieceType.QUEEN)])) {
                    weak =  attackedBy[Them][Types.PieceType.ALL_PIECES]
                            & ~attackedBy2[Us]
                            & (attackedBy[Us][Types.PieceType.KING] | attackedBy[Us][Types.PieceType.QUEEN] | ~attackedBy[Us][Types.PieceType.ALL_PIECES]);

                    unsafeChecks = 0;
                    int kingDanger = 0;

                    safe = ~pos.byColorBB[Them];
                    safe &= ~attackedBy[Us][Types.PieceType.ALL_PIECES] | (weak & attackedBy2[Them]);

                    b1 = Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, ksq, pos.byTypeBB[Types.PieceType.ALL_PIECES] ^ (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.QUEEN]));
                    b2 = Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, ksq, pos.byTypeBB[Types.PieceType.ALL_PIECES] ^ (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.QUEEN]));

                    if (((b1 | b2) & attackedBy[Them][Types.PieceType.QUEEN] & safe & ~attackedBy[Us][Types.PieceType.QUEEN]) != 0) {
                        kingDanger += QueenSafeCheck;
                    }

                    b1 &= attackedBy[Them][Types.PieceType.ROOK];
                    b2 &= attackedBy[Them][Types.PieceType.BISHOP];

                    if ((b1 & safe) != 0) {
                        kingDanger += RookSafeCheck;
                    } else {
                        unsafeChecks |= b1;
                    }

                    if ((b2 & safe) != 0) {
                        kingDanger += BishopSafeCheck;
                    } else {
                        unsafeChecks |= b2;
                    }

                    b = pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KNIGHT, ksq) & attackedBy[Them][Types.PieceType.KNIGHT];
                    if ((b & safe) != 0) {
                        kingDanger += KnightSafeCheck;
                    } else {
                        unsafeChecks |= b;
                    }

                    unsafeChecks &= mobilityArea[Them];

                    kingDanger +=        kingAttackersCount[Them] * kingAttackersWeight[Them]
                            + 102 * kingAdjacentZoneAttacksCount[Them]
                            + 191 * Long.bitCount(kingRing[Us] & weak)
                            + 143 * Long.bitCount((pos.st.blockersForKing[Us] & pos.byColorBB[Us]) | unsafeChecks)
                            - 848 * (pos.pieceCount[((Them << 3) + Types.PieceType.QUEEN)] == 0 ? 1 : 0)
                            -   9 * ((short) score) / 8
                            +  40;

                    if (kingDanger > 0) {
                        int mobilityDanger = ((short) (mobility[Them] - mobility[Us]));
                        kingDanger = Math.max(0, kingDanger + mobilityDanger);
                        score -= ((kingDanger / 16) << 16) | ((kingDanger * kingDanger / 4096) & 0xffff);
                    }
                }

                int kf = (ksq & 7);
                b = attackedBy[Them][Types.PieceType.ALL_PIECES] & KingFlank[kf] & Camp;

                b =  (Us == Types.Color.WHITE ? b << 4 : b >>> 4) | (b & attackedBy2[Them] & ~attackedBy[Us][Types.PieceType.PAWN]);

                score -= CloseEnemies * Long.bitCount(b);

                if ((pos.byTypeBB[Types.PieceType.PAWN] & KingFlank[kf]) == 0) {
                    score -= PawnlessFlank;
                }

                return score;
            }

            public int evaluate_king_White() {
                int Us = 0;
                final int Them = 1;
                final long Camp = 1099511627775L;

                final int ksq = pos.pieceList[6][0];
                long weak, b, b1, b2, safe, unsafeChecks;

                int score = pe.king_safety_as_a_function_of_TemplateColor(Us, pos, ksq);

                if (kingAttackersCount[Them] > (1 - pos.pieceCount[((Them << 3) + Types.PieceType.QUEEN)])) {
                    weak =  attackedBy[Them][Types.PieceType.ALL_PIECES]
                            & ~attackedBy2[Us]
                            & (attackedBy[Us][Types.PieceType.KING] | attackedBy[Us][Types.PieceType.QUEEN] | ~attackedBy[Us][Types.PieceType.ALL_PIECES]);

                    unsafeChecks = 0;
                    int kingDanger = 0;

                    safe = ~pos.byColorBB[Them];
                    safe &= ~attackedBy[Us][Types.PieceType.ALL_PIECES] | (weak & attackedBy2[Them]);

                    b1 = Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, ksq, pos.byTypeBB[Types.PieceType.ALL_PIECES] ^ (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.QUEEN]));
                    b2 = Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, ksq, pos.byTypeBB[Types.PieceType.ALL_PIECES] ^ (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.QUEEN]));

                    if (((b1 | b2) & attackedBy[Them][Types.PieceType.QUEEN] & safe & ~attackedBy[Us][Types.PieceType.QUEEN]) != 0) {
                        kingDanger += QueenSafeCheck;
                    }

                    b1 &= attackedBy[Them][Types.PieceType.ROOK];
                    b2 &= attackedBy[Them][Types.PieceType.BISHOP];

                    if ((b1 & safe) != 0) {
                        kingDanger += RookSafeCheck;
                    } else {
                        unsafeChecks |= b1;
                    }

                    if ((b2 & safe) != 0) {
                        kingDanger += BishopSafeCheck;
                    } else {
                        unsafeChecks |= b2;
                    }

                    b = pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KNIGHT, ksq) & attackedBy[Them][Types.PieceType.KNIGHT];
                    if ((b & safe) != 0) {
                        kingDanger += KnightSafeCheck;
                    } else {
                        unsafeChecks |= b;
                    }

                    unsafeChecks &= mobilityArea[Them];

                    kingDanger +=        kingAttackersCount[Them] * kingAttackersWeight[Them]
                            + 102 * kingAdjacentZoneAttacksCount[Them]
                            + 191 * Long.bitCount(kingRing[Us] & weak)
                            + 143 * Long.bitCount((pos.st.blockersForKing[Us] & pos.byColorBB[Us]) | unsafeChecks)
                            - 848 * (pos.pieceCount[((Them << 3) + Types.PieceType.QUEEN)] == 0 ? 1 : 0)
                            -   9 * ((short) score) / 8
                            +  40;

                    if (kingDanger > 0) {
                        int mobilityDanger = ((short) (mobility[Them] - mobility[Us]));
                        kingDanger = Math.max(0, kingDanger + mobilityDanger);
                        score -= ((kingDanger / 16) << 16) | ((kingDanger * kingDanger / 4096) & 0xffff);
                    }
                }

                int kf = (ksq & 7);
                b = attackedBy[Them][Types.PieceType.ALL_PIECES] & KingFlank[kf] & Camp;

                b =  (Us == Types.Color.WHITE ? b << 4 : b >>> 4) | (b & attackedBy2[Them] & ~attackedBy[Us][Types.PieceType.PAWN]);

                score -= CloseEnemies * Long.bitCount(b);

                if ((pos.byTypeBB[Types.PieceType.PAWN] & KingFlank[kf]) == 0) {
                    score -= PawnlessFlank;
                }

                return score;
            }


            public int evaluate_threats(int Us) {
                final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);
                final int Up = (Us == Types.Color.WHITE ? Types.Direction.NORTH : Types.Direction.SOUTH);
                final int Left = (Us == Types.Color.WHITE ? Types.Direction.NORTH_WEST : Types.Direction.SOUTH_EAST);
                final int Right = (Us == Types.Color.WHITE ? Types.Direction.NORTH_EAST : Types.Direction.SOUTH_WEST);
                final long TRank3BB = (Us == Types.Color.WHITE ? Bitboards.Rank3BB : Bitboards.Rank6BB);

                long b, weak, defended, stronglyProtected, safeThreats;
                int score = Types.Score.SCORE_ZERO;

                weak = (pos.byColorBB[Them] ^ (pos.byColorBB[Them] & pos.byTypeBB[Types.PieceType.PAWN])) & attackedBy[Us][Types.PieceType.PAWN];

                if (weak != 0) {
                    b = (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN]) & (~attackedBy[Them][Types.PieceType.ALL_PIECES] | attackedBy[Us][Types.PieceType.ALL_PIECES]);

                    safeThreats = (Bitboards.shift(Right, b) | Bitboards.shift(Left, b)) & weak;

                    score += ThreatBySafePawn * Long.bitCount(safeThreats);
                }

                stronglyProtected = attackedBy[Them][Types.PieceType.PAWN] | (attackedBy2[Them] & ~attackedBy2[Us]);

                defended = (pos.byColorBB[Them] ^ (pos.byColorBB[Them] & pos.byTypeBB[Types.PieceType.PAWN])) & stronglyProtected;

                weak = pos.byColorBB[Them] & ~stronglyProtected & attackedBy[Us][Types.PieceType.ALL_PIECES];

                if ((defended | weak) != 0) {
                    b = (defended | weak) & (attackedBy[Us][Types.PieceType.KNIGHT] | attackedBy[Us][Types.PieceType.BISHOP]);
                    while (b != 0) {
                        int s = Long.numberOfTrailingZeros(b);
                        b &= b - 1;
                        score += ThreatByMinor[(pos.board[s] & 7)];
                        if ((pos.board[s] & 7) != Types.PieceType.PAWN) {
                            score += ThreatByRank * ((s >>> 3) ^ (Them * 7));
                        }
                    }

                    b = ((pos.byColorBB[Them] & pos.byTypeBB[Types.PieceType.QUEEN]) | weak) & attackedBy[Us][Types.PieceType.ROOK];
                    while (b != 0) {
                        int s = Long.numberOfTrailingZeros(b);
                        b &= b - 1;
                        score += ThreatByRook[(pos.board[s] & 7)];
                        if ((pos.board[s] & 7) != Types.PieceType.PAWN) {
                            score += ThreatByRank * ((s >>> 3) ^ (Them * 7));
                        }
                    }

                    score += Hanging * Long.bitCount(weak & ~attackedBy[Them][Types.PieceType.ALL_PIECES]);

                    b = weak & attackedBy[Us][Types.PieceType.KING];
                    if (b != 0) {
                        score += ThreatByKing[((b & (b - 1)) != 0) ? 1 : 0];
                    }
                }

                if ((pos.byColorBB[Us] & (pos.byTypeBB[Types.PieceType.ROOK] | pos.byTypeBB[Types.PieceType.QUEEN])) != 0) {
                    score += WeakUnopposedPawn * pe.weak_unopposed(Them);
                }

                b = Bitboards.shift(Up, (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN])) & ~pos.byTypeBB[Types.PieceType.ALL_PIECES];
                b |= Bitboards.shift(Up, b & TRank3BB) & ~pos.byTypeBB[Types.PieceType.ALL_PIECES];

                b &= ~attackedBy[Them][Types.PieceType.PAWN] & (attackedBy[Us][Types.PieceType.ALL_PIECES] | ~attackedBy[Them][Types.PieceType.ALL_PIECES]);

                b = (Bitboards.shift(Left, b) | Bitboards.shift(Right, b))
                        &  pos.byColorBB[Them]
                        & ~attackedBy[Us][Types.PieceType.PAWN];

                score += ThreatByPawnPush * Long.bitCount(b);

                safeThreats = ~pos.byColorBB[Us] & ~attackedBy2[Them] & attackedBy2[Us];
                b = (attackedBy[Us][Types.PieceType.BISHOP] & attackedBy[Them][Types.PieceType.QUEEN_DIAGONAL])
                        | (attackedBy[Us][Types.PieceType.ROOK] & attackedBy[Them][Types.PieceType.QUEEN] & ~attackedBy[Them][Types.PieceType.QUEEN_DIAGONAL]);

                score += ThreatByAttackOnQueen * Long.bitCount(b & safeThreats);

                return score;
            }


            public int king_distance(int c, int s) {
                return Math.min(Bitboards.SquareDistance[pos.pieceList[((c << 3) + Types.PieceType.KING)][0]][s], 5);
            }


            public int evaluate_passed_pawns(int Us) {
                final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);
                final int Up = (Us == Types.Color.WHITE ? Types.Direction.NORTH : Types.Direction.SOUTH);

                long b, bb, squaresToQueen, defendedSquares, unsafeSquares;
                int score = Types.Score.SCORE_ZERO;

                b = pe.passed_pawns(Us);

                while (b != 0) {
                    int s = Long.numberOfTrailingZeros(b);
                    b &= b - 1;

                    bb = Bitboards.ForwardFileBB[Us][s] & (attackedBy[Them][Types.PieceType.ALL_PIECES] | pos.byColorBB[Them]);
                    score -= HinderPassedPawn * Long.bitCount(bb);

                    int r = ((s >>> 3) ^ (Us * 7));
                    int rr = RankFactor[r];

                    int mbonus = Passed[Types.Phase.MG][r];
                    int ebonus = Passed[Types.Phase.EG][r];

                    if (rr != 0) {
                        int blockSq = s + Up;

                        ebonus += (king_distance(Them, blockSq) * 5 - king_distance(Us, blockSq) * 2) * rr;

                        if (r != Types.Rank.RANK_7) {
                            ebonus -= king_distance(Us, blockSq + Up) * rr;
                        }

                        if (pos.board[blockSq] == Types.Piece.NO_PIECE) {
                            defendedSquares = unsafeSquares = squaresToQueen = Bitboards.ForwardFileBB[Us][s];

                            bb = Bitboards.ForwardFileBB[Them][s] & (pos.byTypeBB[Types.PieceType.ROOK] | pos.byTypeBB[Types.PieceType.QUEEN]) & pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, s);

                            if ((pos.byColorBB[Us] & bb) == 0) {
                                defendedSquares &= attackedBy[Us][Types.PieceType.ALL_PIECES];
                            }

                            if ((pos.byColorBB[Them] & bb) == 0) {
                                unsafeSquares &= attackedBy[Them][Types.PieceType.ALL_PIECES] | pos.byColorBB[Them];
                            }

                            int k = unsafeSquares == 0 ? 18 : (unsafeSquares & Bitboards.SquareBB[blockSq]) == 0 ? 8 : 0;

                            if (defendedSquares == squaresToQueen) {
                                k += 6;
                            } else if ((defendedSquares & Bitboards.SquareBB[blockSq]) != 0) {
                                k += 4;
                            }

                            mbonus += k * rr;
                            ebonus += k * rr;
                        } else if ((pos.byColorBB[Us] & Bitboards.SquareBB[blockSq]) != 0) {
                            mbonus += rr + r * 2;
                            ebonus += rr + r * 2;
                        }
                    }

                    if (!pos.pawn_passed(Us, s + Up) || (pos.byTypeBB[Types.PieceType.PAWN] & Bitboards.ForwardFileBB[Us][s]) != 0) {
                        mbonus /= 2;
                        ebonus /= 2;
                    }

                    score += ((ebonus << 16) | (mbonus & 0xffff)) + PassedFile[(s & 7)];
                }

                return score;
            }


            public int evaluate_space(int Us) {
                final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);
                final long SpaceMask = Us == Types.Color.WHITE ? CenterFiles & (Bitboards.Rank2BB | Bitboards.Rank3BB | Bitboards.Rank4BB) :
                        CenterFiles & (Bitboards.Rank7BB | Bitboards.Rank6BB | Bitboards.Rank5BB);

                long safe = SpaceMask &
                        ~(pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN]) &
                        ~attackedBy[Them][Types.PieceType.PAWN] &
                        (attackedBy[Us][Types.PieceType.ALL_PIECES] | ~attackedBy[Them][Types.PieceType.ALL_PIECES]);

                long behind = (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN]);
                behind |= (Us == Types.Color.WHITE ? behind >>>  8 : behind <<  8);
                behind |= (Us == Types.Color.WHITE ? behind >>> 16 : behind << 16);

                int bonus = Long.bitCount((Us == Types.Color.WHITE ? safe << 32 : safe >>> 32) | (behind & safe));
                int weight = pos.pieceCount[((Us << 3) + Types.PieceType.ALL_PIECES)] - 2 * pe.open_files();

                return ((bonus * weight * weight / 16) & 0xffff);
            }


            public int evaluate_initiative(int eg) {
                int kingDistance = Bitboards.distance_as_a_function_of_TemplateFile(pos.pieceList[((Types.Color.WHITE << 3) + Types.PieceType.KING)][0], pos.pieceList[((Types.Color.BLACK << 3) + Types.PieceType.KING)][0])
                        - Bitboards.distance_as_a_function_of_TemplateRank(pos.pieceList[((Types.Color.WHITE << 3) + Types.PieceType.KING)][0], pos.pieceList[((Types.Color.BLACK << 3) + Types.PieceType.KING)][0]);
                boolean bothFlanks = (pos.byTypeBB[Types.PieceType.PAWN] & QueenSide) != 0 && (pos.byTypeBB[Types.PieceType.PAWN] & KingSide) != 0;

                int initiative = 8 * (pe.pawn_asymmetry() + kingDistance - 17) + 12 * (pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.PAWN)] + pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.PAWN)]) + 16 * (bothFlanks ? 1 : 0);

                int v = ((eg > 0 ? 1 : 0) - (eg < 0 ? 1 : 0)) * Math.max(initiative, -Math.abs(eg));

                return (v << 16);
            }


            public int evaluate_scale_factor(int eg) {
                int strongSide = eg > Types.Value.VALUE_DRAW ? Types.Color.WHITE : Types.Color.BLACK;
                int sf = me.scale_factor(pos, strongSide);

                if (sf == Types.ScaleFactor.SCALE_FACTOR_NORMAL || sf == Types.ScaleFactor.SCALE_FACTOR_ONEPAWN) {
                    if (pos.opposite_bishops()) {
                        if (pos.st.nonPawnMaterial[Types.Color.WHITE] == Types.Value.BishopValueMg &&
                                pos.st.nonPawnMaterial[Types.Color.BLACK] == Types.Value.BishopValueMg) {
                            return Bitboards.more_than_one(pos.byTypeBB[Types.PieceType.PAWN]) ? 31 : 9;
                        }
                        return 46;
                    } else if (Math.abs(eg) <= Types.Value.BishopValueEg &&
                            pos.pieceCount[((strongSide << 3) + Types.PieceType.PAWN)] <= 2 &&
                            !pos.pawn_passed((strongSide ^ Types.Color.BLACK), pos.pieceList[(((strongSide ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0])) {
                        return (37 + 7 * pos.pieceCount[((strongSide << 3) + Types.PieceType.PAWN)]);
                    }
                }

                return sf;
            }


            public int value() {
                me = Material.probe(pos);

                if (me.specialized_eval_exists()) {
                    return me.evaluate(pos);
                }

                int score = pos.st.psq + me.imbalance() + Contempt;

                pe = Pawns.probe(pos);
                score += pe.pawns_score();

                int v = (((short) score) + (score >> 16)) / 2;
                if (Math.abs(v) > LazyThreshold) {
                    return pos.sideToMove == Types.Color.WHITE ? v : -v;
                }




                long b = (pos.byColorBB[0] & pos.byTypeBB[1]) & ((pos.byTypeBB[0] >>> 8) | 16776960);

                mobilityArea[0] = ~(b | Bitboards.SquareBB[pos.pieceList[6][0]] | pe.pawn_attacks(1));

                b = attackedBy[0][6] = pos.attacks_from_as_a_function_of_TemplatePieceType(6, pos.pieceList[6][0]);
                attackedBy[0][1] = pe.pawn_attacks(0);

                attackedBy2[0] = b & attackedBy[0][1];
                attackedBy[0][0] = b | attackedBy[0][1];

                if (pos.st.nonPawnMaterial[1] >= 2046) {
                    kingRing[0] = b;
                    if ((pos.pieceList[6][0] >>> 3) == 0) {
                        kingRing[0] |= (b << 8);
                    }

                    kingAttackersCount[1] = Long.bitCount(b & pe.pawn_attacks(1));
                    kingAdjacentZoneAttacksCount[1] = kingAttackersWeight[1] = 0;
                } else {
                    kingRing[0] = kingAttackersCount[1] = 0;
                }

                b = (pos.byColorBB[1] & pos.byTypeBB[1]) & ((pos.byTypeBB[0] << 8) | 72056494526300160L);

                mobilityArea[1] = ~((b | Bitboards.SquareBB[pos.pieceList[14][0]]) | pe.pawn_attacks(0));

                b = attackedBy[1][6] = pos.attacks_from_as_a_function_of_TemplatePieceType(6, pos.pieceList[14][0]);
                attackedBy[1][1] = pe.pawn_attacks(1);

                attackedBy2[1] = b & attackedBy[1][1];
                attackedBy[1][0] = b | attackedBy[1][1];

                if (pos.st.nonPawnMaterial[0] >= 2046) {
                    kingRing[1] = b;
                    if (((pos.pieceList[14][0] >>> 3) ^ 7) == 0) {
                        kingRing[1] |= (b >>> 8);
                    }

                    kingAttackersCount[0] = Long.bitCount(b & pe.pawn_attacks(0));
                    kingAdjacentZoneAttacksCount[0] = kingAttackersWeight[0] = 0;
                } else {
                    kingRing[1] = kingAttackersCount[0] = 0;
                }

                score += evaluate_pieces_White_Knight() - evaluate_pieces_Black_Knight();
                score += evaluate_pieces_White_Bishop() - evaluate_pieces_Black_Bishop();
                score += evaluate_pieces_White_Rook() - evaluate_pieces_Black_Rook();
                score += evaluate_pieces_White_Queen() - evaluate_pieces_Black_Queen();

                score += mobility[0] - mobility[1];

                score +=  evaluate_king(Types.Color.WHITE)
                        - evaluate_king(Types.Color.BLACK);

                score +=  evaluate_threats(Types.Color.WHITE)
                        - evaluate_threats(Types.Color.BLACK);

                score +=  evaluate_passed_pawns(Types.Color.WHITE)
                        - evaluate_passed_pawns(Types.Color.BLACK);

                if ((pos.st.nonPawnMaterial[Types.Color.WHITE] + pos.st.nonPawnMaterial[Types.Color.BLACK]) >= SpaceThreshold) {
                    score += evaluate_space(Types.Color.WHITE) - evaluate_space(Types.Color.BLACK);
                }

                score += evaluate_initiative(score >> 16);

                int sf = evaluate_scale_factor(score >> 16);
                v =  ((short) score) * me.game_phase() + (score >> 16) * (Types.Phase.PHASE_MIDGAME - me.game_phase()) * sf / Types.ScaleFactor.SCALE_FACTOR_NORMAL;

                v /= Types.Phase.PHASE_MIDGAME;

                return pos.sideToMove == Types.Color.WHITE ? v : -v;
            }
        }
    }


    public static class Material {

        public static final int[][] QuadraticOurs = {
                {1667,		0,		0,		0,		0,		0,		0,		0},
                {40,		0,		0,		0,		0,		0,		0,		0},
                {32,		255,	-3,		0,		0,		0,		0,		0},
                {0,			104,	4,		0,		0,		0,		0,		0},
                {-26,		-2,		47,		105,	-149,	0,		0,		0},
                {-189,		24,		117,	133,	-134,	-10,	0,		0}};

        public static final int[][] QuadraticTheirs = {
                {0,			0,		0,		0,		0,		0,		0,		0},
                {36,		0,		0,		0,		0,		0,		0,		0},
                {9,			63,		0,		0,		0,		0,		0,		0},
                {59,		65,		42,		0,		0,		0,		0,		0},
                {46,		39,		24,		-24,	0,		0,		0,		0},
                {97,		100,	-42,	137,	268,	0,		0,		0}};

        public static endgame.Endgame_KXK[] EvaluateKXK = {new endgame.Endgame_KXK(endgame.EndgameCode.KXK, Types.Color.WHITE), new endgame.Endgame_KXK(endgame.EndgameCode.KXK, Types.Color.BLACK)};

        public static endgame.Endgame_KBPsK[]  ScaleKBPsK = {new endgame.Endgame_KBPsK(endgame.EndgameCode.KBPsK, Types.Color.WHITE), new endgame.Endgame_KBPsK(endgame.EndgameCode.KBPsK, Types.Color.BLACK)};
        public static endgame.Endgame_KQKRPs[] ScaleKQKRPs = {new endgame.Endgame_KQKRPs(endgame.EndgameCode.KQKRPs, Types.Color.WHITE), new endgame.Endgame_KQKRPs(endgame.EndgameCode.KQKRPs, Types.Color.BLACK)};
        public static endgame.Endgame_KPsK[] ScaleKPsK = {new endgame.Endgame_KPsK(endgame.EndgameCode.KPsK, Types.Color.WHITE), new endgame.Endgame_KPsK(endgame.EndgameCode.KPsK, Types.Color.BLACK)};
        public static endgame.Endgame_KPKP[] ScaleKPKP = {new endgame.Endgame_KPKP(endgame.EndgameCode.KPKP, Types.Color.WHITE), new endgame.Endgame_KPKP(endgame.EndgameCode.KPKP, Types.Color.BLACK)};

        public static class Entry {
            public long key = 0;
            public endgame.EndgameBase evaluationFunction;
            public endgame.EndgameBase[] scalingFunction = new endgame.EndgameBase[Types.Color.COLOR_NB];

            public int value;
            public int[] factor = new int[Types.Color.COLOR_NB];
            public int gamePhase;

            public int imbalance() {
                return ((value << 16) | (value & 0xffff));
            }

            public int game_phase() {
                return gamePhase;
            }

            public boolean specialized_eval_exists() {
                return evaluationFunction != null;
            }

            public int evaluate(Position pos) {
                return evaluationFunction.convertToInteger(pos);
            }

            public int scale_factor(Position pos, int c) {
                int sf = scalingFunction[c] != null ? scalingFunction[c].convertToInteger(pos) : Types.ScaleFactor.SCALE_FACTOR_NONE;
                return sf != Types.ScaleFactor.SCALE_FACTOR_NONE ? sf : factor[c];
            }
        }

        public static void resetEntryFields(Entry entry) {
            entry.key = 0;
            entry.evaluationFunction = null;
            for (int i = 0; i < entry.scalingFunction.length; i++) {
                entry.scalingFunction[i] = null;
            }
            entry.value = 0;
            for (int i = 0; i < entry.factor.length; i++) {
                entry.factor[i] = 0;
            }
            entry.gamePhase = 0;
        }


        public static class Table {
            private int Size = 8192;
            private List<Entry> table = new ArrayList<Entry>(Size);

            public Table() {
                for (int i = 0; i < Size; i++) {
                    table.add(new Entry());
                }
            }

            public Entry get(long key) {
                return table.get((int) (key & (Size - 1)));
            }
        }


        public static Entry probe(Position pos) {
            long key = pos.st.materialKey;
            Entry e = pos.thisThread.materialTable.get(key);

            if (e.key == key) {
                return e;
            }

            resetEntryFields(e);
            e.key = key;
            e.factor[Types.Color.WHITE] = e.factor[Types.Color.BLACK] = Types.ScaleFactor.SCALE_FACTOR_NORMAL;

            int npm_w = pos.st.nonPawnMaterial[Types.Color.WHITE];
            int npm_b = pos.st.nonPawnMaterial[Types.Color.BLACK];
            int npm = Math.max(Types.Value.EndgameLimit, Math.min(npm_w + npm_b, Types.Value.MidgameLimit));

            e.gamePhase = ((npm - Types.Value.EndgameLimit) * Types.Phase.PHASE_MIDGAME) / (Types.Value.MidgameLimit - Types.Value.EndgameLimit);

            if ((e.evaluationFunction = pos.thisThread.endgames.probe(true, key)) != null) {
                return e;
            }

            for (int c = Types.Color.WHITE; c <= Types.Color.BLACK; ++c) {
                if (is_KXK(pos, c)) {
                    e.evaluationFunction = EvaluateKXK[c];
                    return e;
                }
            }

            endgame.EndgameBase sf;

            if ((sf = pos.thisThread.endgames.probe(false, key)) != null) {
                e.scalingFunction[sf.strongSide] = sf;
                return e;
            }

            for (int c = Types.Color.WHITE; c <= Types.Color.BLACK; ++c) {
                if (is_KBPsKs(pos, c)) {
                    e.scalingFunction[c] = ScaleKBPsK[c];
                } else if (is_KQKRPs(pos, c)) {
                    e.scalingFunction[c] = ScaleKQKRPs[c];
                }
            }

            if (npm_w + npm_b == Types.Value.VALUE_ZERO && pos.byTypeBB[Types.PieceType.PAWN] != 0) {
                if (pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.PAWN)] == 0) {
                    e.scalingFunction[Types.Color.WHITE] = ScaleKPsK[Types.Color.WHITE];
                } else if (pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.PAWN)] == 0) {
                    e.scalingFunction[Types.Color.BLACK] = ScaleKPsK[Types.Color.BLACK];
                } else if (pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.PAWN)] == 1 && pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.PAWN)] == 1) {
                    e.scalingFunction[Types.Color.WHITE] = ScaleKPKP[Types.Color.WHITE];
                    e.scalingFunction[Types.Color.BLACK] = ScaleKPKP[Types.Color.BLACK];
                }
            }

            if (pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.PAWN)] == 0 && npm_w - npm_b <= Types.Value.BishopValueMg) {
                e.factor[Types.Color.WHITE] = npm_w < Types.Value.RookValueMg ? Types.ScaleFactor.SCALE_FACTOR_DRAW :
                        npm_b <= Types.Value.BishopValueMg ? 4 : 14;
            }

            if (pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.PAWN)] == 0 && npm_b - npm_w <= Types.Value.BishopValueMg) {
                e.factor[Types.Color.BLACK] = npm_b < Types.Value.RookValueMg ? Types.ScaleFactor.SCALE_FACTOR_DRAW :
                        npm_w <= Types.Value.BishopValueMg ? 4 : 14;
            }

            if (pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.PAWN)] == 1 && npm_w - npm_b <= Types.Value.BishopValueMg) {
                e.factor[Types.Color.WHITE] = Types.ScaleFactor.SCALE_FACTOR_ONEPAWN;
            }

            if (pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.PAWN)] == 1 && npm_b - npm_w <= Types.Value.BishopValueMg) {
                e.factor[Types.Color.BLACK] = Types.ScaleFactor.SCALE_FACTOR_ONEPAWN;
            }

            final int[][] PieceCount = {
                    {(pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.BISHOP)] > 1 ? 1 : 0), pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.PAWN)], pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.KNIGHT)],
                            pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.BISHOP)], pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.ROOK)], pos.pieceCount[((Types.Color.WHITE << 3) + Types.PieceType.QUEEN)], 0, 0},
                    {(pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.BISHOP)] > 1 ? 1 : 0), pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.PAWN)], pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.KNIGHT)],
                            pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.BISHOP)], pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.ROOK)], pos.pieceCount[((Types.Color.BLACK << 3) + Types.PieceType.QUEEN)], 0, 0}};

            e.value = (imbalance_as_a_function_of_TemplateColor(Types.Color.WHITE, PieceCount) - imbalance_as_a_function_of_TemplateColor(Types.Color.BLACK, PieceCount)) / 16;
            return e;
        }


        public static boolean is_KXK(Position pos, int us) {
            return !Bitboards.more_than_one(pos.byColorBB[us ^ Types.Color.BLACK]) &&
                    pos.st.nonPawnMaterial[us] >= Types.Value.RookValueMg;
        }

        public static boolean is_KBPsKs(Position pos, int us) {
            return pos.st.nonPawnMaterial[us] == Types.Value.BishopValueMg &&
                    pos.pieceCount[((us << 3) + Types.PieceType.BISHOP)] == 1 &&
                    pos.pieceCount[((us << 3) + Types.PieceType.PAWN)] >= 1;
        }

        public static boolean is_KQKRPs(Position pos, int us) {
            return pos.pieceCount[((us << 3) + Types.PieceType.PAWN)] == 0 &&
                    pos.st.nonPawnMaterial[us] == Types.Value.QueenValueMg &&
                    pos.pieceCount[((us << 3) + Types.PieceType.QUEEN)] == 1 &&
                    pos.pieceCount[(((us ^ Types.Color.BLACK) << 3) + Types.PieceType.ROOK)] == 1 &&
                    pos.pieceCount[(((us ^ Types.Color.BLACK) << 3) + Types.PieceType.PAWN)] >= 1;
        }



        public static int imbalance_as_a_function_of_TemplateColor(int Us, int[][] pieceCount) {
            final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);

            int bonus = 0;

            for (int pt1 = Types.PieceType.NO_PIECE_TYPE; pt1 <= Types.PieceType.QUEEN; ++pt1) {
                if (pieceCount[Us][pt1] == 0) {
                    continue;
                }
                int v = 0;
                for (int pt2 = Types.PieceType.NO_PIECE_TYPE; pt2 <= pt1; ++pt2) {
                    v += QuadraticOurs[pt1][pt2] * pieceCount[Us][pt2] + QuadraticTheirs[pt1][pt2] * pieceCount[Them][pt2];
                }
                bonus += pieceCount[Us][pt1] * v;
            }
            return bonus;
        }
    }


    public static class Misc {

        public static class PRNG {
            public long s;

            public PRNG(long seed) {
                s = seed;
            }

            public long rand64() {
                s ^= s >>> 12;
                s ^= s << 25;
                s ^= s >>> 27;
                return s * 2685821657736338717L;
            }

            public long rand() {
                return rand64();
            }

            public long sparse_rand() {
                return (rand64() & rand64() & rand64());
            }
        }
    }


    public static class Movegen {

        public static class GenType {
            public static final int CAPTURES = 0;
            public static final int QUIETS = 1;
            public static final int QUIET_CHECKS = 2;
            public static final int EVASIONS = 3;
            public static final int NON_EVASIONS = 4;
            public static final int LEGAL = 5;
        }

        public static class ExtMove {
            public int move;
            public int value;

            public ExtMove(int move) {
                this.move = move;
            }

            public int convertToMove() {
                return move;
            }

            public void operatorAssignment(int m) {
                move = m;
            }

            public String toString() {
                return UCI.move(move, false);
            }
        }

        public static class MoveList {
            public ExtMove[] moveList = new ExtMove[Types.MAX_MOVES];
            private int nextIndexOfMoveList = 0;

            public MoveList(int T, Position pos) {
                nextIndexOfMoveList = T == GenType.QUIET_CHECKS		?	generate_as_a_function_of_TemplateQUIET_CHECKS(pos, moveList, 0) :
                        T == GenType.EVASIONS			?	generate_as_a_function_of_TemplateEVASIONS(pos, moveList, 0) :
                                T == GenType.LEGAL			?	generate_as_a_function_of_TemplateLEGAL(pos, moveList, 0) : generate_as_a_function_of_TemplateGenType(T, pos, moveList, 0);
            }

            public int size() {
                return nextIndexOfMoveList;
            }

            public boolean contains(int move) {
                for (int i = 0; i < nextIndexOfMoveList; i++) {
                    if (move == moveList[i].convertToMove()) {
                        return true;
                    }
                }
                return false;
            }

            public Set<Integer> squaresThatCanMove() {
                if (moveList != null) {
                    Set<Integer> squares = new HashSet<>();
                    for (int i = 0; i < nextIndexOfMoveList; i++) {
                        squares.add(Types.from_sq(moveList[i].move));
                    }
                    return squares;
                } else {
                    return null;
                }
            }

            public Set<Integer> squaresThatAPieceCanGoTo(int piece) {
                if (moveList != null) {
                    Set<Integer> squares = new HashSet<>();
                    for (int i = 0; i < nextIndexOfMoveList; i++) {
                        int move = moveList[i].move;
                        if (piece == Types.from_sq(move)) {
                            if (Types.moveType(move) == Types.MoveType.CASTLING) {
                                if (Types.to_sq(move) > Types.from_sq(move)) {
                                    squares.add(Types.from_sq(move) + 2);
                                } else {
                                    squares.add(Types.from_sq(move) - 2);
                                }
                            } else {
                                squares.add(Types.to_sq(move));
                            }
                        }
                    }
                    return squares;
                } else {
                    return null;
                }
            }

            public int getMoveByFromAndToSquares(int from, int to) {
                if (moveList != null) {
                    for (int i = 0; i < nextIndexOfMoveList; i++) {
                        if (from == Types.from_sq(moveList[i].move) && to == Types.to_sq(moveList[i].move)) {
                            return moveList[i].move;
                        }
                    }
                }
                return Types.Move.MOVE_NONE;
            }
        }


        public static int generate_castling(int Cr, boolean Checks, boolean Chess960, Position pos, ExtMove[] moveList, int nextIndexOfMoveList, int us) {
            final boolean KingSide = (Cr == Types.CastlingRight.WHITE_OO || Cr == Types.CastlingRight.BLACK_OO);

            if (((pos.byTypeBB[Types.PieceType.ALL_PIECES] & pos.castlingPath[Cr]) != 0) || (pos.st.castlingRights & Cr) == 0) {
                return nextIndexOfMoveList;
            }

            int kfrom = pos.pieceList[((us << 3) + Types.PieceType.KING)][0];
            int rfrom = pos.castlingRookSquare[Cr];
            int kto = ((KingSide ? Types.Square.SQ_G1 : Types.Square.SQ_C1) ^ (us * 56));
            long enemies = pos.byColorBB[us ^ Types.Color.BLACK];

            final int K = Chess960 ? kto > kfrom ? Types.Direction.WEST : Types.Direction.EAST
                    : KingSide   ? Types.Direction.WEST	: Types.Direction.EAST;

            for (int s = kto; s != kfrom; s += K) {
                if ((pos.attackers_to_as_a_function_of_Square_Bitboard(s, pos.byTypeBB[Types.PieceType.ALL_PIECES]) & enemies) != 0) {
                    return nextIndexOfMoveList;
                }
            }

            if (Chess960 && (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, kto, (pos.byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[rfrom])) & (pos.byColorBB[(us ^ Types.Color.BLACK)] & (pos.byTypeBB[Types.PieceType.ROOK] | pos.byTypeBB[Types.PieceType.QUEEN]))) != 0) {
                return nextIndexOfMoveList;
            }

            int m = (Types.MoveType.CASTLING + (kfrom << 6) + rfrom);

            if (Checks && !pos.gives_check(m)) {
                return nextIndexOfMoveList;
            }

            moveList[nextIndexOfMoveList++] = new ExtMove(m);
            return nextIndexOfMoveList;
        }


        public static int make_promotions(int Type, int D, ExtMove[] moveList, int nextIndexOfMoveList, int to, int ksq) {
            if (Type == GenType.CAPTURES || Type == GenType.EVASIONS || Type == GenType.NON_EVASIONS) {
                moveList[nextIndexOfMoveList++] = new ExtMove((Types.MoveType.PROMOTION + ((Types.PieceType.QUEEN - Types.PieceType.KNIGHT) << 12) + ((to - D) << 6) + to));
            }

            if (Type == GenType.QUIETS || Type == GenType.EVASIONS || Type == GenType.NON_EVASIONS) {
                moveList[nextIndexOfMoveList++] = new ExtMove((Types.MoveType.PROMOTION + ((Types.PieceType.ROOK - Types.PieceType.KNIGHT) << 12) + ((to - D) << 6) + to));
                moveList[nextIndexOfMoveList++] = new ExtMove((Types.MoveType.PROMOTION + ((Types.PieceType.BISHOP - Types.PieceType.KNIGHT) << 12) + ((to - D) << 6) + to));
                moveList[nextIndexOfMoveList++] = new ExtMove((Types.MoveType.PROMOTION + ((Types.PieceType.KNIGHT - Types.PieceType.KNIGHT) << 12) + ((to - D) << 6) + to));
            }

            if (Type == GenType.QUIET_CHECKS && (Bitboards.PseudoAttacks[Types.PieceType.KNIGHT][to] & Bitboards.SquareBB[ksq]) != 0) {
                moveList[nextIndexOfMoveList++] = new ExtMove((Types.MoveType.PROMOTION + ((Types.PieceType.KNIGHT - Types.PieceType.KNIGHT) << 12) + ((to - D) << 6) + to));
            }

            return nextIndexOfMoveList;
        }


        public static int generate_pawn_moves(int Us, int Type, Position pos, ExtMove[] moveList, int nextIndexOfMoveList, long target) {
            final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);
            final long TRank8BB = (Us == Types.Color.WHITE ? Bitboards.Rank8BB : Bitboards.Rank1BB);
            final long TRank7BB = (Us == Types.Color.WHITE ? Bitboards.Rank7BB : Bitboards.Rank2BB);
            final long TRank3BB = (Us == Types.Color.WHITE ? Bitboards.Rank3BB : Bitboards.Rank6BB);
            final int Up = (Us == Types.Color.WHITE ? Types.Direction.NORTH : Types.Direction.SOUTH);
            final int Right = (Us == Types.Color.WHITE ? Types.Direction.NORTH_EAST : Types.Direction.SOUTH_WEST);
            final int Left = (Us == Types.Color.WHITE ? Types.Direction.NORTH_WEST : Types.Direction.SOUTH_EAST);

            long emptySquares = 0;

            long pawnsOn7 = (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN]) & TRank7BB;
            long pawnsNotOn7 = (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN]) & ~TRank7BB;

            long enemies = (Type == GenType.EVASIONS ? pos.byColorBB[Them] & target :
                    Type == GenType.CAPTURES ? target : pos.byColorBB[Them]);

            if (Type != GenType.CAPTURES) {
                emptySquares = (Type == GenType.QUIETS || Type == GenType.QUIET_CHECKS ? target : ~pos.byTypeBB[Types.PieceType.ALL_PIECES]);

                long b1 = Bitboards.shift(Up, pawnsNotOn7) & emptySquares;
                long b2 = Bitboards.shift(Up, b1 & TRank3BB) & emptySquares;

                if (Type == GenType.EVASIONS) {
                    b1 &= target;
                    b2 &= target;
                }

                if (Type == GenType.QUIET_CHECKS) {
                    int ksq = pos.pieceList[((Them << 3) + Types.PieceType.KING)][0];

                    b1 &= Bitboards.PawnAttacks[Them][ksq];
                    b2 &= Bitboards.PawnAttacks[Them][ksq];

                    long dcCandidates = (pos.st.blockersForKing[(pos.sideToMove ^ Types.Color.BLACK)] & pos.byColorBB[pos.sideToMove]);
                    if ((pawnsNotOn7 & dcCandidates) != 0) {
                        long dc1 = Bitboards.shift(Up, pawnsNotOn7 & dcCandidates) & emptySquares & ~Bitboards.FileBB[(ksq & 7)];
                        long dc2 = Bitboards.shift(Up, dc1 & TRank3BB) & emptySquares;

                        b1 |= dc1;
                        b2 |= dc2;
                    }
                }

                while (b1 != 0) {
                    int to = Long.numberOfTrailingZeros(b1);
                    b1 &= b1 - 1;
                    moveList[nextIndexOfMoveList++] = new ExtMove((((to - Up) << 6) + to));
                }

                while (b2 != 0) {
                    int to = Long.numberOfTrailingZeros(b2);
                    b2 &= b2 - 1;
                    moveList[nextIndexOfMoveList++] = new ExtMove((((to - Up - Up) << 6) + to));
                }
            }

            if (pawnsOn7 != 0 && (Type != GenType.EVASIONS || (target & TRank8BB) != 0)) {
                if (Type == GenType.CAPTURES) {
                    emptySquares = ~pos.byTypeBB[Types.PieceType.ALL_PIECES];
                }

                if (Type == GenType.EVASIONS) {
                    emptySquares &= target;
                }

                long b1 = Bitboards.shift(Right, pawnsOn7) & enemies;
                long b2 = Bitboards.shift(Left, pawnsOn7) & enemies;
                long b3 = Bitboards.shift(Up, pawnsOn7) & emptySquares;

                int ksq = pos.pieceList[((Them << 3) + Types.PieceType.KING)][0];

                while (b1 != 0) {
                    nextIndexOfMoveList = make_promotions(Type, Right, moveList, nextIndexOfMoveList, Long.numberOfTrailingZeros(b1), ksq);
                    b1 &= b1 - 1;
                }

                while (b2 != 0) {
                    nextIndexOfMoveList = make_promotions(Type, Left, moveList, nextIndexOfMoveList, Long.numberOfTrailingZeros(b2), ksq);
                    b2 &= b2 - 1;
                }

                while (b3 != 0) {
                    nextIndexOfMoveList = make_promotions(Type, Up, moveList, nextIndexOfMoveList, Long.numberOfTrailingZeros(b3), ksq);
                    b3 &= b3 - 1;
                }
            }

            if (Type == GenType.CAPTURES || Type == GenType.EVASIONS || Type == GenType.NON_EVASIONS) {
                long b1 = Bitboards.shift(Right, pawnsNotOn7) & enemies;
                long b2 = Bitboards.shift(Left, pawnsNotOn7) & enemies;

                while (b1 != 0) {
                    int to = Long.numberOfTrailingZeros(b1);
                    b1 &= b1 - 1;
                    moveList[nextIndexOfMoveList++] = new ExtMove((((to - Right) << 6) + to));
                }

                while (b2 != 0) {
                    int to = Long.numberOfTrailingZeros(b2);
                    b2 &= b2 - 1;
                    moveList[nextIndexOfMoveList++] = new ExtMove((((to - Left) << 6) + to));
                }

                if (pos.st.epSquare != Types.Square.SQ_NONE) {
                    if (Type == GenType.EVASIONS && (target & Bitboards.SquareBB[(pos.st.epSquare - Up)]) == 0) {
                        return nextIndexOfMoveList;
                    }

                    b1 = pawnsNotOn7 & Bitboards.PawnAttacks[Them][pos.st.epSquare];

                    while (b1 != 0) {
                        moveList[nextIndexOfMoveList++] = new ExtMove((Types.MoveType.ENPASSANT + (Long.numberOfTrailingZeros(b1) << 6) + pos.st.epSquare));
                        b1 &= b1 - 1;
                    }
                }
            }

            return nextIndexOfMoveList;
        }


        public static int generate_moves(int Pt, boolean Checks, Position pos, ExtMove[] moveList, int nextIndexOfMoveList, int us, long target) {
            final int[] pl = pos.pieceList[((us << 3) + Pt)];
            int pl_Index = 0;

            for (int from = pl[pl_Index]; from != Types.Square.SQ_NONE; from = pl[++pl_Index]) {
                if (Checks) {
                    if ((Pt == Types.PieceType.BISHOP || Pt == Types.PieceType.ROOK || Pt == Types.PieceType.QUEEN) &&
                            (Bitboards.PseudoAttacks[Pt][from] & target & pos.st.checkSquares[Pt]) == 0) {
                        continue;
                    }

                    if (((pos.st.blockersForKing[(pos.sideToMove ^ Types.Color.BLACK)] & pos.byColorBB[pos.sideToMove]) & Bitboards.SquareBB[from]) != 0) {
                        continue;
                    }
                }

                long b = pos.attacks_from_as_a_function_of_TemplatePieceType(Pt, from) & target;

                if (Checks) {
                    b &= pos.st.checkSquares[Pt];
                }

                while (b != 0) {
                    moveList[nextIndexOfMoveList++] = new ExtMove(((from << 6) + Long.numberOfTrailingZeros(b)));
                    b &= b - 1;
                }
            }

            return nextIndexOfMoveList;
        }


        public static int generate_all(int Us, int Type, Position pos, ExtMove[] moveList, int nextIndexOfMoveList, long target) {
            final boolean Checks = Type == GenType.QUIET_CHECKS;

            nextIndexOfMoveList = generate_pawn_moves(Us, Type, pos, moveList, nextIndexOfMoveList, target);
            nextIndexOfMoveList = generate_moves(Types.PieceType.KNIGHT, Checks, pos, moveList, nextIndexOfMoveList, Us, target);
            nextIndexOfMoveList = generate_moves(Types.PieceType.BISHOP, Checks, pos, moveList, nextIndexOfMoveList, Us, target);
            nextIndexOfMoveList = generate_moves(Types.PieceType.ROOK, Checks, pos, moveList, nextIndexOfMoveList, Us, target);
            nextIndexOfMoveList = generate_moves(Types.PieceType.QUEEN, Checks, pos, moveList, nextIndexOfMoveList, Us, target);

            if (Type != GenType.QUIET_CHECKS && Type != GenType.EVASIONS) {
                int ksq = pos.pieceList[((Us << 3) + Types.PieceType.KING)][0];
                long b = pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KING, ksq) & target;
                while (b != 0) {
                    moveList[nextIndexOfMoveList++] = new ExtMove(((ksq << 6) + Long.numberOfTrailingZeros(b)));
                    b &= b - 1;
                }
            }

            if (Type != GenType.CAPTURES && Type != GenType.EVASIONS && (pos.st.castlingRights & ((Types.CastlingRight.WHITE_OO | Types.CastlingRight.WHITE_OOO) << (2 * Us))) != 0) {
                if (pos.chess960) {
                    nextIndexOfMoveList = generate_castling(Types.MakeCastling.getRight(Us, Types.CastlingSide.KING_SIDE), Checks, true, pos, moveList, nextIndexOfMoveList, Us);
                    nextIndexOfMoveList = generate_castling(Types.MakeCastling.getRight(Us, Types.CastlingSide.QUEEN_SIDE), Checks, true, pos, moveList, nextIndexOfMoveList, Us);
                } else {
                    nextIndexOfMoveList = generate_castling(Types.MakeCastling.getRight(Us, Types.CastlingSide.KING_SIDE), Checks, false, pos, moveList, nextIndexOfMoveList, Us);
                    nextIndexOfMoveList = generate_castling(Types.MakeCastling.getRight(Us, Types.CastlingSide.QUEEN_SIDE), Checks, false, pos, moveList, nextIndexOfMoveList, Us);
                }
            }

            return nextIndexOfMoveList;
        }


        public static int generate_as_a_function_of_TemplateGenType(int Type, Position pos, ExtMove[] moveList, int nextIndexOfMoveList) {
            int us = pos.sideToMove;

            long target = Type == GenType.CAPTURES		?	pos.byColorBB[us ^ Types.Color.BLACK] :
                    Type == GenType.QUIETS		?	~pos.byTypeBB[Types.PieceType.ALL_PIECES] :
                            Type == GenType.NON_EVASIONS	?	~pos.byColorBB[us] : 0;

            return us == Types.Color.WHITE ? generate_all(Types.Color.WHITE, Type, pos, moveList, nextIndexOfMoveList, target) :
                    generate_all(Types.Color.BLACK, Type, pos, moveList, nextIndexOfMoveList, target);
        }


        public static int generate_as_a_function_of_TemplateQUIET_CHECKS(Position pos, ExtMove[] moveList, int nextIndexOfMoveList) {
            int us = pos.sideToMove;
            long dc = (pos.st.blockersForKing[(pos.sideToMove ^ Types.Color.BLACK)] & pos.byColorBB[pos.sideToMove]);

            while (dc != 0) {
                int from = Long.numberOfTrailingZeros(dc);
                dc &= dc - 1;
                int pt = (pos.board[from] & 7);

                if (pt == Types.PieceType.PAWN) {
                    continue;
                }

                long b = Bitboards.attacks_bb(pt, from, pos.byTypeBB[Types.PieceType.ALL_PIECES]) & ~pos.byTypeBB[Types.PieceType.ALL_PIECES];

                if (pt == Types.PieceType.KING) {
                    b &= ~Bitboards.PseudoAttacks[Types.PieceType.QUEEN][pos.pieceList[(((us ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0]];
                }

                while (b != 0) {
                    moveList[nextIndexOfMoveList++] = new ExtMove(((from << 6) + Long.numberOfTrailingZeros(b)));
                    b &= b - 1;
                }
            }

            return us == Types.Color.WHITE ? generate_all(Types.Color.WHITE, GenType.QUIET_CHECKS, pos, moveList, nextIndexOfMoveList, ~pos.byTypeBB[Types.PieceType.ALL_PIECES]) :
                    generate_all(Types.Color.BLACK, GenType.QUIET_CHECKS, pos, moveList, nextIndexOfMoveList, ~pos.byTypeBB[Types.PieceType.ALL_PIECES]);
        }


        public static int generate_as_a_function_of_TemplateEVASIONS(Position pos, ExtMove[] moveList, int nextIndexOfMoveList) {
            int us = pos.sideToMove;
            int ksq = pos.pieceList[((us << 3) + Types.PieceType.KING)][0];
            long sliderAttacks = 0;
            long sliders = pos.st.checkersBB & ~(pos.byTypeBB[Types.PieceType.KNIGHT] | pos.byTypeBB[Types.PieceType.PAWN]);

            while (sliders != 0) {
                int checksq = Long.numberOfTrailingZeros(sliders);
                sliders &= sliders - 1;
                sliderAttacks |= (Bitboards.LineBB[checksq][ksq] ^ Bitboards.SquareBB[checksq]);
            }

            long b = pos.attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KING, ksq) & ~pos.byColorBB[us] & ~sliderAttacks;
            while (b != 0) {
                moveList[nextIndexOfMoveList++] = new ExtMove(((ksq << 6) + Long.numberOfTrailingZeros(b)));
                b &= b - 1;
            }

            if (Bitboards.more_than_one(pos.st.checkersBB)) {
                return nextIndexOfMoveList;
            }

            int checksq = Long.numberOfTrailingZeros(pos.st.checkersBB);
            long target = (Bitboards.BetweenBB[checksq][ksq] | Bitboards.SquareBB[checksq]);

            return us == Types.Color.WHITE ? generate_all(Types.Color.WHITE, GenType.EVASIONS, pos, moveList, nextIndexOfMoveList, target) :
                    generate_all(Types.Color.BLACK, GenType.EVASIONS, pos, moveList, nextIndexOfMoveList, target);
        }


        public static int generate_as_a_function_of_TemplateLEGAL(Position pos, ExtMove[] moveList, int nextIndexOfMoveList) {
            long pinned = (pos.st.blockersForKing[pos.sideToMove] & pos.byColorBB[pos.sideToMove]);
            int ksq = pos.pieceList[((pos.sideToMove << 3) + Types.PieceType.KING)][0];
            int cur = nextIndexOfMoveList;

            nextIndexOfMoveList = pos.st.checkersBB != 0 ? generate_as_a_function_of_TemplateEVASIONS(pos, moveList, nextIndexOfMoveList) :
                    generate_as_a_function_of_TemplateGenType(GenType.NON_EVASIONS, pos, moveList, nextIndexOfMoveList);

            while (cur != nextIndexOfMoveList) {
                if ((pinned != 0 || ((moveList[cur].convertToMove() >>> 6) & 0x3F) == ksq || (moveList[cur].convertToMove() & (3 << 14)) == Types.MoveType.ENPASSANT) &&
                        !pos.legal(moveList[cur].convertToMove())) {
                    moveList[cur].move = moveList[--nextIndexOfMoveList].move;
                } else {
                    ++cur;
                }
            }
            return nextIndexOfMoveList;
        }
    }


    public static class Movepick {

        public static class ButterflyHistory {
            public int[][] array = new int[Types.Color.COLOR_NB][Types.Square.SQUARE_NB * Types.Square.SQUARE_NB];

            public void fill(int v) {
                array = new int[Types.Color.COLOR_NB][Types.Square.SQUARE_NB * Types.Square.SQUARE_NB];
            }

            public void update(int c, int m, int bonus) {
                array[c][(m & 0xFFF)] += bonus * 32 - array[c][(m & 0xFFF)] * Math.abs(bonus) / 324;
            }
        }


        public static class PieceToHistory {
            public int[][] array = new int[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB];

            public void fillWithZeros() {
                array = new int[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB];
            }

            public void fill(int v) {
                for (int i = 0; i < array.length; i++) {
                    for (int j = 0; j < array[i].length; j++) {
                        array[i][j] = v;
                    }
                }
            }

            public void update(int pc, int to, int bonus) {
                array[pc][to] += bonus * 32 - array[pc][to] * Math.abs(bonus) / 936;
            }
        }


        public static class CapturePieceToHistory {
            public int[][][] array = new int[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB][Types.PieceType.PIECE_TYPE_NB];

            public void fill(int v) {
                array = new int[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB][Types.PieceType.PIECE_TYPE_NB];
            }

            public void update(int pc, int to, int captured, int bonus) {
                array[pc][to][captured] += bonus * 2 - array[pc][to][captured] * Math.abs(bonus) / 324;
            }
        }


        public static class CounterMoveHistory {
            public int[][] array = new int[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB];

            public void fill(int v) {
                array = new int[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB];
            }
        }


        public static class ContinuationHistory {
            public PieceToHistory[][] array = new PieceToHistory[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB];

            public ContinuationHistory() {
                for (int i = 0; i < array.length; i++) {
                    for (int j = 0; j < array[i].length; j++) {
                        array[i][j] = new PieceToHistory();
                    }
                }
            }

            public void fill(PieceToHistory v) {
                for (int i = 0; i < array.length; i++) {
                    for (int j = 0; j < array[i].length; j++) {
                        array[i][j] = v;
                    }
                }
            }
        }


        public static class Stages {
            public static final int MAIN_SEARCH = 0;
            public static final int CAPTURES_INIT = 1;
            public static final int GOOD_CAPTURES = 2;
            public static final int KILLERS = 3;
            public static final int COUNTERMOVE = 4;
            public static final int QUIET_INIT = 5;
            public static final int QUIET = 6;
            public static final int BAD_CAPTURES = 7;
            public static final int EVASION = 8;
            public static final int EVASIONS_INIT = 9;
            public static final int ALL_EVASIONS = 10;
            public static final int PROBCUT = 11;
            public static final int PROBCUT_INIT = 12;
            public static final int PROBCUT_CAPTURES = 13;
            public static final int QSEARCH_WITH_CHECKS = 14;
            public static final int QCAPTURES_1_INIT = 15;
            public static final int QCAPTURES_1 = 16;
            public static final int QCHECKS = 17;
            public static final int QSEARCH_NO_CHECKS = 18;
            public static final int QCAPTURES_2_INIT = 19;
            public static final int QCAPTURES_2 = 20;
            public static final int QSEARCH_RECAPTURES = 21;
            public static final int QRECAPTURES = 22;
        }


        public static void partial_insertion_sort(Movegen.ExtMove[] extMovesArray, int begin, int end, int limit) {
            if (begin >= end) {
                return;
            }

            int sortedEnd = begin;
            for (int p = begin + 1; p < end; ++p) {
                if (extMovesArray[p].value >= limit) {
                    Movegen.ExtMove tmp = extMovesArray[p];
                    int q;
                    extMovesArray[p] = extMovesArray[++sortedEnd];
                    for (q = sortedEnd; q != begin && extMovesArray[q - 1].value < tmp.value; --q) {
                        extMovesArray[q] = extMovesArray[q - 1];
                    }
                    extMovesArray[q] = tmp;
                }
            }
        }


        public static int pick_best(Movegen.ExtMove[] extMovesArray, int begin, int end) {
            if (begin >= end) {
                return 0;
            }

            int maxElmentIndex = begin;
            for (int i = (begin + 1); i < end; i++) {
                if (extMovesArray[maxElmentIndex].value < extMovesArray[i].value) {
                    maxElmentIndex = i;
                }
            }

            Movegen.ExtMove temp = extMovesArray[begin];
            extMovesArray[begin] = extMovesArray[maxElmentIndex];
            extMovesArray[maxElmentIndex] = temp;

            return extMovesArray[begin].move;
        }


        public static class MovePicker {
            private Position pos;
            private ButterflyHistory mainHistory;
            private CapturePieceToHistory captureHistory;
            private PieceToHistory[] contHistory;
            private int ttMove;
            private int countermove;
            private int[] killers = new int[2];
            private int cur;
            private int endMoves;
            private int endBadCaptures;
            private int stage;
            private int recaptureSquare;
            private int threshold;
            private int depth;
            private Movegen.ExtMove[] moves = new Movegen.ExtMove[Types.MAX_MOVES];

            public MovePicker(Position p, int ttm, int d, ButterflyHistory mh, CapturePieceToHistory cph, PieceToHistory[] ch, int cm, int[] killers_p) {
                pos = p;
                mainHistory = mh;
                captureHistory = cph;
                contHistory = ch;
                countermove = cm;
                killers = new int[] {killers_p[0], killers_p[1]};
                depth = d;
                stage = pos.st.checkersBB != 0 ? Stages.EVASION : Stages.MAIN_SEARCH;
                ttMove = ttm != 0 && pos.pseudo_legal(ttm) ? ttm : Types.Move.MOVE_NONE;
                stage += (ttMove == Types.Move.MOVE_NONE ? 1 : 0);
            }

            public MovePicker(Position p, int ttm, int d, ButterflyHistory mh, CapturePieceToHistory cph, int s) {
                pos = p;
                mainHistory = mh;
                captureHistory = cph;
                if (pos.st.checkersBB != 0) {
                    stage = Stages.EVASION;
                } else if (d > Types.Depth.DEPTH_QS_NO_CHECKS) {
                    stage = Stages.QSEARCH_WITH_CHECKS;
                } else if (d > Types.Depth.DEPTH_QS_RECAPTURES) {
                    stage = Stages.QSEARCH_NO_CHECKS;
                } else {
                    stage = Stages.QSEARCH_RECAPTURES;
                    recaptureSquare = s;
                    return;
                }
                ttMove = ttm != 0 && pos.pseudo_legal(ttm) ? ttm : Types.Move.MOVE_NONE;
                stage += (ttMove == Types.Move.MOVE_NONE ? 1 : 0);
            }

            public MovePicker(Position p, int ttm, int th, CapturePieceToHistory cph) {
                pos = p;
                captureHistory = cph;
                threshold = th;
                stage = Stages.PROBCUT;
                ttMove = ttm != 0 && pos.pseudo_legal(ttm) && pos.capture(ttm) && pos.see_ge(ttm, threshold) ? ttm : Types.Move.MOVE_NONE;
                stage += (ttMove == Types.Move.MOVE_NONE ? 1 : 0);
            }

            public void score(int Type) {
                for (int i = cur; i < endMoves; i++) {
                    Movegen.ExtMove m = moves[i];
                    if (Type == Movegen.GenType.CAPTURES) {
                        m.value = PSQT.PieceValue[Types.Phase.MG][pos.board[(m.move & 0x3F)]] +
                                captureHistory.array[pos.board[((m.move >>> 6) & 0x3F)]][(m.move & 0x3F)][(pos.board[(m.move & 0x3F)] & 7)];
                    } else if (Type == Movegen.GenType.QUIETS) {
                        m.value = mainHistory.array[pos.sideToMove][(m.move & 0xFFF)] +
                                contHistory[0].array[pos.board[((m.move >>> 6) & 0x3F)]][(m.move & 0x3F)] +
                                contHistory[1].array[pos.board[((m.move >>> 6) & 0x3F)]][(m.move & 0x3F)] +
                                contHistory[3].array[pos.board[((m.move >>> 6) & 0x3F)]][(m.move & 0x3F)];
                    } else {
                        if (pos.capture(m.move)) {
                            m.value =  PSQT.PieceValue[Types.Phase.MG][pos.board[(m.move & 0x3F)]] - (pos.board[((m.move >>> 6) & 0x3F)] & 7);
                        } else {
                            m.value = mainHistory.array[pos.sideToMove][(m.move & 0xFFF)] - (1 << 28);
                        }
                    }
                }
            }


            public int next_move(boolean skipQuiets) {
                int move;

                switch (stage) {
                    case Stages.MAIN_SEARCH: case Stages.EVASION: case Stages.QSEARCH_WITH_CHECKS: case Stages.QSEARCH_NO_CHECKS: case Stages.PROBCUT: {
                        ++stage;
                        return ttMove;
                    }
                    case Stages.CAPTURES_INIT: {
                        endBadCaptures = cur = 0;
                        endMoves = Movegen.generate_as_a_function_of_TemplateGenType(Movegen.GenType.CAPTURES, pos, moves, cur);
                        score(Movegen.GenType.CAPTURES);
                        ++stage; /* fallthrough */
                    }
                    case Stages.GOOD_CAPTURES: {
                        while (cur < endMoves) {
                            move = pick_best(moves, cur++, endMoves);
                            if (move != ttMove) {
                                if (pos.see_ge(move, -55 * moves[cur - 1].value / 1024)) {
                                    return move;
                                }
                                moves[endBadCaptures++].move = move;
                            }
                        }
                        ++stage;
                        move = killers[0];
                        if (move != Types.Move.MOVE_NONE && move != ttMove && pos.pseudo_legal(move) && !pos.capture(move)) {
                            return move;
                        } /* fallthrough */
                    }
                    case Stages.KILLERS: {
                        ++stage;
                        move = killers[1];
                        if (move != Types.Move.MOVE_NONE && move != ttMove && pos.pseudo_legal(move) && !pos.capture(move)) {
                            return move;
                        } /* fallthrough */
                    }
                    case Stages.COUNTERMOVE: {
                        ++stage;
                        move = countermove;
                        if (move != Types.Move.MOVE_NONE && move != ttMove && move != killers[0] && move != killers[1] && pos.pseudo_legal(move) && !pos.capture(move)) {
                            return move;
                        } /* fallthrough */
                    }
                    case Stages.QUIET_INIT: {
                        cur = endBadCaptures;
                        endMoves = Movegen.generate_as_a_function_of_TemplateGenType(Movegen.GenType.QUIETS, pos, moves, cur);
                        score(Movegen.GenType.QUIETS);
                        partial_insertion_sort(moves, cur, endMoves, -4000 * depth / Types.Depth.ONE_PLY);
                        ++stage; /* fallthrough */
                    }
                    case Stages.QUIET: {
                        while (cur < endMoves && (!skipQuiets || moves[cur].value >= Types.Value.VALUE_ZERO)) {
                            move = moves[cur++].move;
                            if (move != ttMove && move != killers[0] && move != killers[1] && move != countermove) {
                                return move;
                            }
                        }
                        ++stage;
                        cur = 0; /* fallthrough */
                    }
                    case Stages.BAD_CAPTURES: {
                        if (cur < endBadCaptures) {
                            return moves[cur++].move;
                        }
                        break;
                    }
                    case Stages.EVASIONS_INIT: {
                        cur = 0;
                        endMoves = Movegen.generate_as_a_function_of_TemplateEVASIONS(pos, moves, cur);
                        score(Movegen.GenType.EVASIONS);
                        ++stage; /* fallthrough */
                    }
                    case Stages.ALL_EVASIONS: {
                        while (cur < endMoves) {
                            move = pick_best(moves, cur++, endMoves);
                            if (move != ttMove) {
                                return move;
                            }
                        }
                        break;
                    }
                    case Stages.PROBCUT_INIT: {
                        cur = 0;
                        endMoves = Movegen.generate_as_a_function_of_TemplateGenType(Movegen.GenType.CAPTURES, pos, moves, cur);
                        score(Movegen.GenType.CAPTURES);
                        ++stage; /* fallthrough */
                    }
                    case Stages.PROBCUT_CAPTURES: {
                        while (cur < endMoves) {
                            move = pick_best(moves, cur++, endMoves);
                            if (move != ttMove && pos.see_ge(move, threshold)) {
                                return move;
                            }
                        }
                        break;
                    }
                    case Stages.QCAPTURES_1_INIT: case Stages.QCAPTURES_2_INIT: {
                        cur = 0;
                        endMoves = Movegen.generate_as_a_function_of_TemplateGenType(Movegen.GenType.CAPTURES, pos, moves, cur);
                        score(Movegen.GenType.CAPTURES);
                        ++stage; /* fallthrough */
                    }
                    case Stages.QCAPTURES_1: case Stages.QCAPTURES_2: {
                        while (cur < endMoves) {
                            move = pick_best(moves, cur++, endMoves);
                            if (move != ttMove) {
                                return move;
                            }
                        }
                        if (stage == Stages.QCAPTURES_2) {
                            break;
                        }
                        cur = 0;
                        endMoves = Movegen.generate_as_a_function_of_TemplateQUIET_CHECKS(pos, moves, cur);
                        ++stage; /* fallthrough */
                    }
                    case Stages.QCHECKS: {
                        while (cur < endMoves) {
                            move = moves[cur++].move;
                            if (move != ttMove) {
                                return move;
                            }
                        }
                        break;
                    }
                    case Stages.QSEARCH_RECAPTURES: {
                        cur = 0;
                        endMoves = Movegen.generate_as_a_function_of_TemplateGenType(Movegen.GenType.CAPTURES, pos, moves, cur);
                        score(Movegen.GenType.CAPTURES);
                        ++stage; /* fallthrough */
                    }
                    case Stages.QRECAPTURES: {
                        while (cur < endMoves) {
                            move = pick_best(moves, cur++, endMoves);
                            if ((move & 0x3F) == recaptureSquare) {
                                return move;
                            }
                        }
                        break;
                    }
                }
                return Types.Move.MOVE_NONE;
            }
        }
    }


    public static class Pawns {

        public static final int Isolated = ((18 << 16) | (13 & 0xffff));
        public static final int Backward = ((12 << 16) | (24 & 0xffff));
        public static int[][][][] Connected = new int[2][2][3][Types.Rank.RANK_NB];
        public static final int Doubled = ((38 << 16) | (18 & 0xffff));

        public static final int[][][] ShelterWeakness = {
                {{97,		17,		9,		44,		84,		87,		99,		0},
                        {106,		6,		33,		86,		87,		104,	112,	0},
                        {101,		2,		65,		98,		58,		89,		115,	0},
                        {73,		7,		54,		73,		84,		83,		111,	0}},
                {{104,		20,		6,		27,		86,		93,		82,		0},
                        {123,		9,		34,		96,		112,	88,		75,		0},
                        {120,		25,		65,		91,		66,		78,		117,	0},
                        {81,		2,		47,		63,		94,		93,		104,	0}}};

        public static final int[][][] StormDanger = {
                {{0,		-290,		-274,		57,		41,		0,		0,		0},
                        {0,		60,			144,		39,		13,		0,		0,		0},
                        {0,		65,			141,		41,		34,		0,		0,		0},
                        {0,		53,			127,		56,		14,		0,		0,		0}},
                {{4,		73,			132,		46,		31,		0,		0,		0},
                        {1,		64,			143,		26,		13,		0,		0,		0},
                        {1,		47,			110,		44,		24,		0,		0,		0},
                        {0,		72,			127,		50,		31,		0,		0,		0}},
                {{0,		0,			79,			23,		1,		0,		0,		0},
                        {0,		0,			148,		27,		2,		0,		0,		0},
                        {0,		0,			161,		16,		1,		0,		0,		0},
                        {0,		0,			171,		22,		15,		0,		0,		0}},
                {{22,		45,			104,		62,		6,		0,		0,		0},
                        {31,		30,			99,			39,		19,		0,		0,		0},
                        {23,		29,			96,			41,		15,		0,		0,		0},
                        {21,		23,			116,		41,		15,		0,		0,		0}}};

        public static final int MaxSafetyBonus = 258;

        public static class Entry {
            public long key = 0;
            public int score;
            public long[] passedPawns = new long[Types.Color.COLOR_NB];
            public long[] pawnAttacks = new long[Types.Color.COLOR_NB];
            public long[] pawnAttacksSpan = new long[Types.Color.COLOR_NB];
            public int[] kingSquares = new int[Types.Color.COLOR_NB];
            public int[] kingSafety = new int[Types.Color.COLOR_NB];
            public int[] weakUnopposed = new int[Types.Color.COLOR_NB];
            public int[] castlingRights = new int[Types.Color.COLOR_NB];
            public int[] semiopenFiles = new int[Types.Color.COLOR_NB];
            public int[][] pawnsOnSquares = new int[Types.Color.COLOR_NB][Types.Color.COLOR_NB];
            public int asymmetry;
            public int openFiles;

            public int pawns_score() {
                return score;
            }

            public long pawn_attacks(int c) {
                return pawnAttacks[c];
            }

            public long passed_pawns(int c) {
                return passedPawns[c];
            }

            public long pawn_attacks_span(int c) {
                return pawnAttacksSpan[c];
            }

            public int weak_unopposed(int c) {
                return weakUnopposed[c];
            }

            public int pawn_asymmetry() {
                return asymmetry;
            }

            public int open_files() {
                return openFiles;
            }

            public int semiopen_file(int c, int f) {
                return semiopenFiles[c] & (1 << f);
            }

            public int semiopen_side(int c, int f, boolean leftSide) {
                return semiopenFiles[c] & (leftSide ? (1 << f) - 1 : ~((1 << (f + 1)) - 1));
            }

            public int pawns_on_same_color_squares(int c, int s) {
                return pawnsOnSquares[c][(Bitboards.DarkSquares & Bitboards.SquareBB[s]) != 0 ? 1 : 0];
            }

            public int king_safety_as_a_function_of_TemplateColor(int Us, Position pos, int ksq) {
                return kingSquares[Us] == ksq && castlingRights[Us] == (pos.st.castlingRights & ((Types.CastlingRight.WHITE_OO | Types.CastlingRight.WHITE_OOO) << (2 * Us)))
                        ? kingSafety[Us] : (kingSafety[Us] = do_king_safety_as_a_function_of_TemplateColor(Us, pos, ksq));
            }

            public int do_king_safety_as_a_function_of_TemplateColor(int Us, Position pos, int ksq) {
                kingSquares[Us] = ksq;
                castlingRights[Us] = (pos.st.castlingRights & ((Types.CastlingRight.WHITE_OO | Types.CastlingRight.WHITE_OOO) << (2 * Us)));
                int minKingPawnDistance = 0;

                long pawns = (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN]);
                if (pawns != 0) {
                    while ((Bitboards.DistanceRingBB[ksq][minKingPawnDistance++] & pawns) == 0) {}
                }

                int bonus = shelter_storm_as_a_function_of_TemplateColor(Us, pos, ksq);

                if ((pos.st.castlingRights & Types.MakeCastling.getRight(Us, Types.CastlingSide.KING_SIDE)) != 0) {
                    bonus = Math.max(bonus, shelter_storm_as_a_function_of_TemplateColor(Us, pos, (Types.Square.SQ_G1 ^ (Us * 56))));
                }

                if ((pos.st.castlingRights & Types.MakeCastling.getRight(Us, Types.CastlingSide.QUEEN_SIDE)) != 0) {
                    bonus = Math.max(bonus, shelter_storm_as_a_function_of_TemplateColor(Us, pos, (Types.Square.SQ_C1 ^ (Us * 56))));
                }

                return (((-16 * minKingPawnDistance) << 16) | (bonus & 0xffff));
            }

            public int shelter_storm_as_a_function_of_TemplateColor(int Us, Position pos, int ksq) {
                final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);

                int BlockedByKing = 0, Unopposed = 1, BlockedByPawn = 2, Unblocked = 3;

                long b = pos.byTypeBB[Types.PieceType.PAWN] & (Bitboards.ForwardRanksBB[Us][(ksq >>> 3)] | Bitboards.RankBB[(ksq >>> 3)]);
                long ourPawns = b & pos.byColorBB[Us];
                long theirPawns = b & pos.byColorBB[Them];
                int safety = MaxSafetyBonus;
                int center = Math.max(Types.File.FILE_B, Math.min(Types.File.FILE_G, (ksq & 7)));

                for (int f = (center - 1); f <= (center + 1); ++f) {
                    b = ourPawns & Bitboards.FileBB[f];
                    int rkUs = b != 0 ? ((Bitboards.backmost_sq(Us, b) >>> 3) ^ (Us * 7)) : Types.Rank.RANK_1;

                    b = theirPawns & Bitboards.FileBB[f];
                    int rkThem = b != 0 ? (((Them == Types.Color.WHITE ? (63 ^ Long.numberOfLeadingZeros(b)) : Long.numberOfTrailingZeros(b)) >>> 3) ^ (Us * 7)) : Types.Rank.RANK_1;

                    int d = Math.min(f, (f ^ Types.File.FILE_H));
                    safety -=  ShelterWeakness[f == (ksq & 7) ? 1 : 0][d][rkUs]
                            + StormDanger[f == (ksq & 7) && rkThem == ((ksq >>> 3) ^ (Us * 7)) + 1 ?
                            BlockedByKing : rkUs == Types.Rank.RANK_1 ? Unopposed : rkThem == rkUs + 1 ? BlockedByPawn : Unblocked][d][rkThem];
                }

                return safety;
            }
        }


        public static class Table {
            private int Size = 16384;
            private List<Entry> table = new ArrayList<Entry>(Size);

            public Table() {
                for (int i = 0; i < Size; i++) {
                    table.add(new Entry());
                }
            }

            public Entry get(long key) {
                return table.get((int) (key & (Size - 1)));
            }
        }


        private static final int[] Seed_For_init = {0, 13, 24, 18, 76, 100, 175, 330};
        public static void init() {
            for (int opposed = 0; opposed <= 1; ++opposed) {
                for (int phalanx = 0; phalanx <= 1; ++phalanx) {
                    for (int support = 0; support <= 2; ++support) {
                        for (int r = Types.Rank.RANK_2; r < Types.Rank.RANK_8; ++r) {
                            int v = 17 * support;
                            v += (Seed_For_init[r] + (phalanx == 1 ? (Seed_For_init[r + 1] - Seed_For_init[r]) / 2 : 0)) >> opposed;
                            Connected[opposed][phalanx][support][r] = (((v * (r - 2) / 4) << 16) | (v & 0xffff));
                        }
                    }
                }
            }
        }


        public static Entry probe(Position pos) {
            long key = pos.st.pawnKey;
            Entry e = pos.thisThread.pawnsTable.get(key);

            if (e.key == key) {
                return e;
            }

            e.key = key;
            e.score = evaluate_as_a_function_of_TemplateColor(Types.Color.WHITE, pos, e) - evaluate_as_a_function_of_TemplateColor(Types.Color.BLACK, pos, e);
            e.asymmetry = Long.bitCount(e.semiopenFiles[Types.Color.WHITE] ^ e.semiopenFiles[Types.Color.BLACK]);
            e.openFiles = Long.bitCount(e.semiopenFiles[Types.Color.WHITE] & e.semiopenFiles[Types.Color.BLACK]);
            return e;
        }


        public static int evaluate_as_a_function_of_TemplateColor(int Us, Position pos, Entry e) {
            final int Them = (Us == Types.Color.WHITE ? Types.Color.BLACK : Types.Color.WHITE);
            final int Up = (Us == Types.Color.WHITE ? Types.Direction.NORTH : Types.Direction.SOUTH);
            final int Right = (Us == Types.Color.WHITE ? Types.Direction.NORTH_EAST : Types.Direction.SOUTH_WEST);
            final int Left = (Us == Types.Color.WHITE ? Types.Direction.NORTH_WEST : Types.Direction.SOUTH_EAST);

            long b, neighbours, stoppers, doubled, supported, phalanx;
            long lever, leverPush;
            int s;
            boolean opposed, backward;
            int score = Types.Score.SCORE_ZERO;
            final int[] pl = pos.pieceList[((Us << 3) + Types.PieceType.PAWN)];
            int pl_Index = 0;

            long ourPawns = (pos.byColorBB[Us] & pos.byTypeBB[Types.PieceType.PAWN]);
            long theirPawns = (pos.byColorBB[Them] & pos.byTypeBB[Types.PieceType.PAWN]);

            e.passedPawns[Us] = e.pawnAttacksSpan[Us] = e.weakUnopposed[Us] = 0;
            e.semiopenFiles[Us] = 0xFF;
            e.kingSquares[Us] = Types.Square.SQ_NONE;
            e.pawnAttacks[Us] = Bitboards.shift(Right, ourPawns) | Bitboards.shift(Left, ourPawns);
            e.pawnsOnSquares[Us][Types.Color.BLACK] = Long.bitCount(ourPawns & Bitboards.DarkSquares);
            e.pawnsOnSquares[Us][Types.Color.WHITE] = pos.pieceCount[((Us << 3) + Types.PieceType.PAWN)] - e.pawnsOnSquares[Us][Types.Color.BLACK];

            while ((s = pl[pl_Index++]) != Types.Square.SQ_NONE) {
                int f = (s & 7);

                e.semiopenFiles[Us] &= ~(1 << f);
                e.pawnAttacksSpan[Us] |= Bitboards.PawnAttackSpan[Us][s];

                opposed = (theirPawns & Bitboards.ForwardFileBB[Us][s]) != 0;
                stoppers = theirPawns & Bitboards.PassedPawnMask[Us][s];
                lever = theirPawns & Bitboards.PawnAttacks[Us][s];
                leverPush = theirPawns & Bitboards.PawnAttacks[Us][s + Up];
                doubled = (ourPawns & Bitboards.SquareBB[(s - Up)]);
                neighbours = ourPawns & Bitboards.AdjacentFilesBB[f];
                phalanx = neighbours & Bitboards.RankBB[(s >>> 3)];
                supported = neighbours & Bitboards.RankBB[((s - Up) >>> 3)];

                if (neighbours == 0 || lever != 0 || ((s >>> 3) ^ (Us * 7)) >= Types.Rank.RANK_5) {
                    backward = false;
                } else {
                    b = Bitboards.RankBB[((Bitboards.backmost_sq(Us, neighbours | stoppers)) >>> 3)];
                    backward = ((b | Bitboards.shift(Up, b & Bitboards.AdjacentFilesBB[f])) & stoppers) != 0;
                }

                if ((stoppers ^ lever ^ leverPush) == 0 &&
                        (ourPawns & Bitboards.ForwardFileBB[Us][s]) == 0 &&
                        Long.bitCount(supported) >= Long.bitCount(lever) &&
                        Long.bitCount(phalanx) >= Long.bitCount(leverPush)) {
                    e.passedPawns[Us] = (e.passedPawns[Us] | Bitboards.SquareBB[s]);
                } else if (stoppers == Bitboards.SquareBB[s + Up] && ((s >>> 3) ^ (Us * 7)) >= Types.Rank.RANK_5) {
                    b = Bitboards.shift(Up, supported) & ~theirPawns;
                    while (b != 0) {
                        if (!Bitboards.more_than_one(theirPawns & Bitboards.PawnAttacks[Us][Long.numberOfTrailingZeros(b)])) {
                            e.passedPawns[Us] = (e.passedPawns[Us] | Bitboards.SquareBB[s]);
                        }
                        b &= b - 1;
                    }
                }

                if ((supported | phalanx) != 0) {
                    score += Connected[opposed ? 1 : 0][phalanx != 0 ? 1 : 0][Long.bitCount(supported)][((s >>> 3) ^ (Us * 7))];
                } else if (neighbours == 0) {
                    score -= Isolated;
                    e.weakUnopposed[Us] += !opposed ? 1 : 0;
                } else if (backward) {
                    score -= Backward;
                    e.weakUnopposed[Us] += !opposed ? 1 : 0;
                }

                if (doubled != 0 && supported == 0) {
                    score -= Doubled;
                }
            }
            return score;
        }
    }


    public static class Position {
        // Data members
        public int[] board = new int[Types.Square.SQUARE_NB];
        public long[] byTypeBB = new long[Types.PieceType.PIECE_TYPE_NB];
        public long[] byColorBB = new long[Types.Color.COLOR_NB];
        public int[] pieceCount = new int[Types.Piece.PIECE_NB];
        public int[][] pieceList = new int[Types.Piece.PIECE_NB][16];
        private int[] index = new int[Types.Square.SQUARE_NB];
        private int[] castlingRightsMask = new int[Types.Square.SQUARE_NB];
        public int[] castlingRookSquare = new int[Types.CastlingRight.CASTLING_RIGHT_NB];
        public long[] castlingPath = new long[Types.CastlingRight.CASTLING_RIGHT_NB];
        public int gamePly;
        public int sideToMove;
        public thread.Threadd thisThread;
        public StateInfo st;
        public boolean chess960;

        public static class StateInfo {
            // Copied when making a move
            public long pawnKey = 0;
            public long materialKey = 0;;
            public int[] nonPawnMaterial = new int[Types.Color.COLOR_NB];
            public int castlingRights;
            public int rule50;
            public int pliesFromNull;
            public int psq;
            public int epSquare;
            // Not copied when making a move (will be recomputed anyhow)
            public long key = 0;
            public long checkersBB;
            public int capturedPiece;
            public StateInfo previous;
            public long[] blockersForKing = new long[Types.Color.COLOR_NB];
            public long[] pinnersForKing = new long[Types.Color.COLOR_NB];
            public long[] checkSquares = new long[Types.PieceType.PIECE_TYPE_NB];
        }

        public static void copyCopyableMembersOfStateInfo(StateInfo newStateInfo, StateInfo oldStateInfo) {
            newStateInfo.pawnKey = oldStateInfo.pawnKey;
            newStateInfo.materialKey = oldStateInfo.materialKey;
            for (int i = 0; i < oldStateInfo.nonPawnMaterial.length; i++) {
                newStateInfo.nonPawnMaterial[i] = oldStateInfo.nonPawnMaterial[i];
            }
            newStateInfo.castlingRights = oldStateInfo.castlingRights;
            newStateInfo.rule50 = oldStateInfo.rule50;
            newStateInfo.pliesFromNull = oldStateInfo.pliesFromNull;
            newStateInfo.psq = oldStateInfo.psq;
            newStateInfo.epSquare = oldStateInfo.epSquare;
        }

        public static void copyEntireMembersOfStateInfo(StateInfo newStateInfo, StateInfo oldStateInfo) {
            newStateInfo.pawnKey = oldStateInfo.pawnKey;
            newStateInfo.materialKey = oldStateInfo.materialKey;
            for (int i = 0; i < oldStateInfo.nonPawnMaterial.length; i++) {
                newStateInfo.nonPawnMaterial[i] = oldStateInfo.nonPawnMaterial[i];
            }
            newStateInfo.castlingRights = oldStateInfo.castlingRights;
            newStateInfo.rule50 = oldStateInfo.rule50;
            newStateInfo.pliesFromNull = oldStateInfo.pliesFromNull;
            newStateInfo.psq = oldStateInfo.psq;
            newStateInfo.epSquare = oldStateInfo.epSquare;
            newStateInfo.key = oldStateInfo.key;
            newStateInfo.checkersBB = oldStateInfo.checkersBB;
            newStateInfo.capturedPiece = oldStateInfo.capturedPiece;
            newStateInfo.previous = oldStateInfo.previous;
            for (int i = 0; i < oldStateInfo.blockersForKing.length; i++) {
                newStateInfo.blockersForKing[i] = oldStateInfo.blockersForKing[i];
            }
            for (int i = 0; i < oldStateInfo.pinnersForKing.length; i++) {
                newStateInfo.pinnersForKing[i] = oldStateInfo.pinnersForKing[i];
            }
            for (int i = 0; i < oldStateInfo.checkSquares.length; i++) {
                newStateInfo.checkSquares[i] = oldStateInfo.checkSquares[i];
            }
        }

        public static class Zobrist {
            public static long[][] psq = new long[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB];
            public static long[] enpassant = new long[Types.File.FILE_NB];
            public static long[] castling = new long[Types.CastlingRight.CASTLING_RIGHT_NB];
            public static long side;
            public static long noPawns;
        }

        public static void resetPositionFields(Position position) {
            position.board = new int[Types.Square.SQUARE_NB];
            position.byTypeBB = new long[Types.PieceType.PIECE_TYPE_NB];
            position.byColorBB = new long[Types.Color.COLOR_NB];
            position.pieceCount = new int[Types.Piece.PIECE_NB];
            position.pieceList = new int[Types.Piece.PIECE_NB][16];
            position.index = new int[Types.Square.SQUARE_NB];
            position.castlingRightsMask = new int[Types.Square.SQUARE_NB];
            position.castlingRookSquare = new int[Types.CastlingRight.CASTLING_RIGHT_NB];
            position.castlingPath = new long[Types.CastlingRight.CASTLING_RIGHT_NB];
            position.gamePly = 0;
            position.sideToMove = 0;
            position.thisThread = null;
            position.st = null;
            position.chess960 = false;
        }

        public static void resetStateInfoFields(StateInfo si) {
            si.pawnKey = 0;
            si.materialKey = 0;
            si.nonPawnMaterial = new int[Types.Color.COLOR_NB];
            si.castlingRights = 0;
            si.rule50 = 0;
            si.pliesFromNull = 0;
            si.psq = 0;
            si.epSquare = 0;
            si.key = 0;
            si.checkersBB = 0;
            si.capturedPiece = 0;
            si.previous = null;
            si.blockersForKing = new long[Types.Color.COLOR_NB];
            si.pinnersForKing = new long[Types.Color.COLOR_NB];
            si.checkSquares = new long[Types.PieceType.PIECE_TYPE_NB];
        }

        public static final String PieceToChar = " PNBRQK  pnbrqk";

        public static final int[] Pieces = {Types.Piece.W_PAWN, Types.Piece.W_KNIGHT, Types.Piece.W_BISHOP, Types.Piece.W_ROOK, Types.Piece.W_QUEEN, Types.Piece.W_KING,
                Types.Piece.B_PAWN, Types.Piece.B_KNIGHT, Types.Piece.B_BISHOP, Types.Piece.B_ROOK, Types.Piece.B_QUEEN, Types.Piece.B_KING};


        public static int min_attacker(int Pt, long[] bb, int to, long stmAttackers, long[] occupied, long[] attackers) {
            if (Pt == Types.PieceType.KING) {
                return Pt;
            } else {
                long b = stmAttackers & bb[Pt];
                if (b == 0) {
                    return min_attacker((Pt + 1), bb, to, stmAttackers, occupied, attackers);
                }
                occupied[0] ^= b & ~(b - 1);
                if (Pt == Types.PieceType.PAWN || Pt == Types.PieceType.BISHOP || Pt == Types.PieceType.QUEEN) {
                    attackers[0] |= Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, to, occupied[0]) & (bb[Types.PieceType.BISHOP] | bb[Types.PieceType.QUEEN]);
                }
                if (Pt == Types.PieceType.ROOK || Pt == Types.PieceType.QUEEN) {
                    attackers[0] |= Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, to, occupied[0]) & (bb[Types.PieceType.ROOK] | bb[Types.PieceType.QUEEN]);
                }
                attackers[0] &= occupied[0];
                return Pt;
            }
        }

        public static class StateListPtr extends LinkedList<StateInfo> {
            private static final long serialVersionUID = 3678979451572571783L;
        }

        public static void init() {
            Misc.PRNG rng = new Misc.PRNG(1070372);

            for (int pc : Pieces) {
                for (int s = Types.Square.SQ_A1; s <= Types.Square.SQ_H8; ++s) {
                    Zobrist.psq[pc][s] = rng.rand();
                }
            }

            for (int f = Types.File.FILE_A; f <= Types.File.FILE_H; ++f) {
                Zobrist.enpassant[f] = rng.rand();
            }

            for (int cr = Types.CastlingRight.NO_CASTLING; cr <= Types.CastlingRight.ANY_CASTLING; ++cr) {
                Zobrist.castling[cr] = 0;
                long b = cr;
                while (b != 0) {
                    long k = Zobrist.castling[(int) (1L << Long.numberOfTrailingZeros(b))];
                    b &= b - 1;
                    Zobrist.castling[cr] = (k != 0) ? Zobrist.castling[cr] ^ k : Zobrist.castling[cr] ^ rng.rand();
                }
            }
            Zobrist.side = rng.rand();
            Zobrist.noPawns = rng.rand();
        }

        public Position() {}

        // FEN string input/output
        public Position set(String fenStr, boolean isChess960, StateInfo si, thread.Threadd th) {
            char col, row;
            String token;
            int idx;
            int sq = Types.Square.SQ_A8;
            Scanner ss = new Scanner(fenStr);

            resetPositionFields(this);
            resetStateInfoFields(si);
            for (int i = 0; i < pieceList.length; i++) {
                for (int j = 0; j < pieceList[0].length; j++) {
                    pieceList[i][j] = Types.Square.SQ_NONE;
                }
            }
            st = si;

            // 1. Piece placement
            token = ss.next();
            for (int i = 0; i < token.length(); i++) {
                char tokenCharacter = token.charAt(i);
                if (Character.isDigit(tokenCharacter)) {
                    sq += Integer.parseInt("" + tokenCharacter) * Types.Direction.EAST;
                } else if (tokenCharacter == '/') {
                    sq += 2 * Types.Direction.SOUTH;
                } else if ((idx = PieceToChar.indexOf(tokenCharacter)) != -1) {
                    put_piece(idx, sq);
                    ++sq;
                }
            }

            // 2. Active color
            token = ss.next();
            sideToMove = (token.charAt(0) == 'w' ? Types.Color.WHITE : Types.Color.BLACK);

            // 3. Castling availability.
            token = ss.next();
            for (int i = 0; i < token.length(); i++) {
                char tokenCharacter = token.charAt(i);
                int rsq;
                int c = Character.isLowerCase(tokenCharacter) ? Types.Color.BLACK : Types.Color.WHITE;
                int rook = ((c << 3) + Types.PieceType.ROOK);

                char toUpperChangedCharacter = Character.toUpperCase(tokenCharacter);
                if (toUpperChangedCharacter == 'K') {
                    for (rsq = (Types.Square.SQ_H1 ^ (c * 56)); board[rsq] != rook; --rsq) {}
                } else if (toUpperChangedCharacter == 'Q') {
                    for (rsq = (Types.Square.SQ_A1 ^ (c * 56)); board[rsq] != rook; ++rsq) {}
                } else if (toUpperChangedCharacter >= 'A' && toUpperChangedCharacter <= 'H') {
                    rsq = (((Types.Rank.RANK_1 ^ (c * 7)) << 3) + (toUpperChangedCharacter - 'A'));
                } else {
                    continue;
                }
                set_castling_right(c, rsq);
            }

            // 4. En passant square. Ignore if no pawn capture is possible
            token = ss.next();
            if (token.length() > 1) {
                col = token.charAt(0);
                row = token.charAt(1);
                st.epSquare = (((row - '1') << 3) + (col - 'a'));
                if ((attackers_to_as_a_function_of_Square_Bitboard(st.epSquare, byTypeBB[Types.PieceType.ALL_PIECES]) & (byColorBB[sideToMove] & byTypeBB[Types.PieceType.PAWN])) == 0 ||
                        (((byColorBB[(sideToMove ^ Types.Color.BLACK)] & byTypeBB[Types.PieceType.PAWN]) & Bitboards.SquareBB[(st.epSquare + (((sideToMove ^ Types.Color.BLACK) == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH))])) == 0) {
                    st.epSquare = Types.Square.SQ_NONE;
                }
            } else {
                st.epSquare = Types.Square.SQ_NONE;
            }

            // 5-6. Halfmove clock and fullmove number
            st.rule50 = ss.nextInt();
            gamePly = ss.nextInt();
            ss.close();

            // Convert from fullmove starting from 1 to gamePly starting from 0,
            // handle also common incorrect FEN with fullmove = 0.
            gamePly = Math.max(2 * (gamePly - 1), 0) + (sideToMove == Types.Color.BLACK ? 1 : 0);

            chess960 = isChess960;
            thisThread = th;
            set_state(st);

            return this;
        }

        public Position set(String code, int c, StateInfo si) {
            String[] sides = {code.substring(code.indexOf('K', 1)), code.substring(0, code.indexOf('K', 1))};

            sides[c] = sides[c].toLowerCase();

            String fenStr = "8/" + sides[0] + ((char) (8 - sides[0].length() + '0')) + "/8/8/8/8/"
                    + sides[1] + ((char) (8 - sides[1].length() + '0')) + "/8 w - - 0 10";

            return set(fenStr, false, si, null);
        }

        public String fen() {
            int emptyCnt;
            StringBuilder ss = new StringBuilder();

            for (int r = Types.Rank.RANK_8; r >= Types.Rank.RANK_1; --r) {
                for (int f = Types.File.FILE_A; f <= Types.File.FILE_H; ++f) {
                    for (emptyCnt = 0; f <= Types.File.FILE_H && (board[((r << 3) + f)] == Types.Piece.NO_PIECE) ; ++f) {
                        ++emptyCnt;
                    }
                    if (emptyCnt != 0) {
                        ss.append(emptyCnt);
                    }
                    if (f <= Types.File.FILE_H) {
                        ss.append(PieceToChar.charAt(board[((r << 3) + f)]));
                    }
                }
                if (r > Types.Rank.RANK_1) {
                    ss.append('/');
                }
            }

            ss.append(sideToMove == Types.Color.WHITE ? " w " : " b ");

            if ((st.castlingRights & Types.CastlingRight.WHITE_OO) != 0) {
                ss.append(chess960 ? ((char) ('A' + (castlingRookSquare[Types.operatorORForColorCastlingSide(Types.Color.WHITE, Types.CastlingSide.KING_SIDE)] & 7))) : 'K');
            }

            if ((st.castlingRights & Types.CastlingRight.WHITE_OOO) != 0) {
                ss.append(chess960 ? ((char) ('A' + (castlingRookSquare[Types.operatorORForColorCastlingSide(Types.Color.WHITE, Types.CastlingSide.QUEEN_SIDE)] & 7))) : 'Q');
            }

            if ((st.castlingRights & Types.CastlingRight.BLACK_OO) != 0) {
                ss.append(chess960 ? ((char) ('a' + (castlingRookSquare[Types.operatorORForColorCastlingSide(Types.Color.BLACK, Types.CastlingSide.KING_SIDE)] & 7))) : 'k');
            }

            if ((st.castlingRights & Types.CastlingRight.BLACK_OOO) != 0) {
                ss.append(chess960 ? ((char) ('a' + (castlingRookSquare[Types.operatorORForColorCastlingSide(Types.Color.BLACK, Types.CastlingSide.QUEEN_SIDE)] & 7))) : 'q');
            }

            if ((st.castlingRights & ((Types.CastlingRight.WHITE_OO | Types.CastlingRight.WHITE_OOO) << (2 * Types.Color.WHITE))) == 0 && (st.castlingRights & ((Types.CastlingRight.WHITE_OO | Types.CastlingRight.WHITE_OOO) << (2 * Types.Color.BLACK))) == 0) {
                ss.append('-');
            }

            ss.append(st.epSquare == Types.Square.SQ_NONE ? " - " : " " + UCI.square(st.epSquare) + " ");
            ss.append(st.rule50 + " " + (1 + (gamePly - (sideToMove == Types.Color.BLACK ? 1 : 0)) / 2));

            return ss.toString();
        }

        public long attackers_to_as_a_function_of_Square_Bitboard(int s, long occupied) {
            return    (Bitboards.PawnAttacks[Types.Color.BLACK][s] & (byColorBB[Types.Color.WHITE] & byTypeBB[Types.PieceType.PAWN]))
                    | (Bitboards.PawnAttacks[Types.Color.WHITE][s] & (byColorBB[Types.Color.BLACK] & byTypeBB[Types.PieceType.PAWN]))
                    | (attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KNIGHT, s) & byTypeBB[Types.PieceType.KNIGHT])
                    | (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, s, occupied) & (byTypeBB[Types.PieceType.ROOK] | byTypeBB[Types.PieceType.QUEEN]))
                    | (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, s, occupied) & (byTypeBB[Types.PieceType.BISHOP] | byTypeBB[Types.PieceType.QUEEN]))
                    | (attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KING, s) & byTypeBB[Types.PieceType.KING]);
        }

        public long attacks_from_as_a_function_of_TemplatePieceType(int Pt, int s) {
            return  Pt == Types.PieceType.BISHOP || Pt == Types.PieceType.ROOK ? Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Pt, s, byTypeBB[Types.PieceType.ALL_PIECES])
                    : Pt == Types.PieceType.QUEEN  ? attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, s) | attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, s)
                    : Bitboards.PseudoAttacks[Pt][s];
        }

        public long slider_blockers(long sliders, int s, long[] pinners) { //Should do so many things after this method is called
            long result = 0;
            pinners[0] = 0;

            long snipers = ((Bitboards.PseudoAttacks[Types.PieceType.ROOK][s] & (byTypeBB[Types.PieceType.QUEEN] | byTypeBB[Types.PieceType.ROOK]))
                    | (Bitboards.PseudoAttacks[Types.PieceType.BISHOP][s] & (byTypeBB[Types.PieceType.QUEEN] | byTypeBB[Types.PieceType.BISHOP]))) & sliders;

            while (snipers != 0) {
                int sniperSq = Long.numberOfTrailingZeros(snipers);
                snipers &= snipers - 1;
                long b = Bitboards.BetweenBB[s][sniperSq] & byTypeBB[Types.PieceType.ALL_PIECES];

                if (!((b & (b - 1)) != 0)) {
                    result |= b;
                    if ((b & byColorBB[(board[s] >>> 3)]) != 0) {
                        pinners[0] = (pinners[0] | Bitboards.SquareBB[sniperSq]);
                    }
                }
            }
            return result;
        }

        // Properties of moves
        public boolean legal(int m) {
            int us = sideToMove;
            int from = ((m >>> 6) & 0x3F);

            if ((m & (3 << 14)) == Types.MoveType.ENPASSANT) {
                int ksq = pieceList[((us << 3) + Types.PieceType.KING)][0];
                int to = (m & 0x3F);
                int capsq = to - ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH);
                long occupied = (((byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[from]) ^ Bitboards.SquareBB[capsq]) | Bitboards.SquareBB[to]);

                return (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, ksq, occupied) & (byColorBB[(us ^ Types.Color.BLACK)] & (byTypeBB[Types.PieceType.QUEEN] | byTypeBB[Types.PieceType.ROOK]))) == 0 &&
                        (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, ksq, occupied) & (byColorBB[(us ^ Types.Color.BLACK)] & (byTypeBB[Types.PieceType.QUEEN] | byTypeBB[Types.PieceType.BISHOP]))) == 0;
            }

            if ((board[from] & 7) == Types.PieceType.KING) {
                return (m & (3 << 14)) == Types.MoveType.CASTLING || (attackers_to_as_a_function_of_Square_Bitboard((m & 0x3F), byTypeBB[Types.PieceType.ALL_PIECES]) & byColorBB[(us ^ Types.Color.BLACK)]) == 0;
            }

            return (((st.blockersForKing[us] & byColorBB[us]) & Bitboards.SquareBB[from])) == 0 || ((Bitboards.LineBB[from][(m & 0x3F)] & Bitboards.SquareBB[pieceList[((us << 3) + Types.PieceType.KING)][0]]) != 0);
        }


        public boolean pseudo_legal(int m) {
            int us = sideToMove;
            int from = ((m >>> 6) & 0x3F);
            int to = (m & 0x3F);
            int pc = board[((m >>> 6) & 0x3F)];

            if ((m & (3 << 14)) != Types.MoveType.NORMAL) {
                return (new Movegen.MoveList(Movegen.GenType.LEGAL, this)).contains(m);
            }

            if ((((m >>> 12) & 3) + Types.PieceType.KNIGHT) - Types.PieceType.KNIGHT != Types.PieceType.NO_PIECE_TYPE) {
                return false;
            }

            if (pc == Types.Piece.NO_PIECE || (pc >>> 3) != us) {
                return false;
            }

            if ((byColorBB[us] & Bitboards.SquareBB[to]) != 0) {
                return false;
            }

            if ((pc & 7) == Types.PieceType.PAWN) {
                if ((to >>> 3) == (Types.Rank.RANK_8 ^ (us * 7))) {
                    return false;
                }

                if ((((Bitboards.PawnAttacks[us][from] & byColorBB[(us ^ Types.Color.BLACK)]) & Bitboards.SquareBB[to])) == 0 &&
                        !((from + ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH) == to) && (board[to] == Types.Piece.NO_PIECE)) &&
                        !((from + 2 * ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH) == to) && ((from >>> 3) == (Types.Rank.RANK_2 ^ (us * 7))) && (board[to] == Types.Piece.NO_PIECE) && (board[to - ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH)] == Types.Piece.NO_PIECE))) {
                    return false;
                }
            } else if ((Bitboards.attacks_bb((pc & 7), from, byTypeBB[Types.PieceType.ALL_PIECES]) & Bitboards.SquareBB[to]) == 0) {
                return false;
            }

            if (st.checkersBB != 0) {
                if ((pc & 7) != Types.PieceType.KING) {
                    if (Bitboards.more_than_one(st.checkersBB)) {
                        return false;
                    }
                    if ((((Bitboards.BetweenBB[Long.numberOfTrailingZeros(st.checkersBB)][pieceList[((us << 3) + Types.PieceType.KING)][0]] | st.checkersBB) & Bitboards.SquareBB[to])) == 0) {
                        return false;
                    }
                } else if ((attackers_to_as_a_function_of_Square_Bitboard(to, (byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[from])) & byColorBB[(us ^ Types.Color.BLACK)]) != 0) {
                    return false;
                }
            }

            return true;
        }

        public boolean capture(int m) {
            return (board[(m & 0x3F)] != Types.Piece.NO_PIECE && (m & (3 << 14)) != Types.MoveType.CASTLING) || (m & (3 << 14)) == Types.MoveType.ENPASSANT;
        }

        public boolean capture_or_promotion(int m) {
            return (m & (3 << 14)) != Types.MoveType.NORMAL ? (m & (3 << 14)) != Types.MoveType.CASTLING : board[(m & 0x3F)] != Types.Piece.NO_PIECE;
        }


        public boolean gives_check(int m) {
            int from = ((m >>> 6) & 0x3F);
            int to = (m & 0x3F);

            if ((st.checkSquares[(board[from] & 7)] & Bitboards.SquareBB[to]) != 0) {
                return true;
            }

            if (((st.blockersForKing[(sideToMove ^ Types.Color.BLACK)] & byColorBB[sideToMove]) & Bitboards.SquareBB[from]) != 0 &&
                    !((Bitboards.LineBB[from][to] & Bitboards.SquareBB[pieceList[(((sideToMove ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0]]) != 0)) {
                return true;
            }

            switch ((m & (3 << 14))) {
                case Types.MoveType.NORMAL:
                    return false;

                case Types.MoveType.PROMOTION:
                    return (Bitboards.attacks_bb((((m >>> 12) & 3) + Types.PieceType.KNIGHT), to, (byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[from])) & Bitboards.SquareBB[pieceList[(((sideToMove ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0]]) != 0;

                case Types.MoveType.ENPASSANT: {
                    int capsq = (((from >>> 3) << 3) + (to & 7));
                    long b = (((byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[from]) ^ Bitboards.SquareBB[capsq]) | Bitboards.SquareBB[to]);
                    return ((Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, pieceList[(((sideToMove ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0], b) & (byColorBB[sideToMove] & (byTypeBB[Types.PieceType.QUEEN] | byTypeBB[Types.PieceType.ROOK])))
                            | (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, pieceList[(((sideToMove ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0], b) & (byColorBB[sideToMove] & (byTypeBB[Types.PieceType.QUEEN] | byTypeBB[Types.PieceType.BISHOP])))) != 0;
                }
                case Types.MoveType.CASTLING: {
                    int kfrom = from;
                    int rfrom = to;
                    int kto = ((rfrom > kfrom ? Types.Square.SQ_G1 : Types.Square.SQ_C1) ^ (sideToMove * 56));
                    int rto = ((rfrom > kfrom ? Types.Square.SQ_F1 : Types.Square.SQ_D1) ^ (sideToMove * 56));
                    return (Bitboards.PseudoAttacks[Types.PieceType.ROOK][rto] & Bitboards.SquareBB[pieceList[(((sideToMove ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0]]) != 0 &&
                            (Bitboards.attacks_bb_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, rto, ((((byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[kfrom]) ^ Bitboards.SquareBB[rfrom]) | Bitboards.SquareBB[rto]) | Bitboards.SquareBB[kto])) & Bitboards.SquareBB[pieceList[(((sideToMove ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0]]) != 0;
                }
                default:
                    return false;
            }
        }

        public boolean advanced_pawn_push(int m) {
            return   (board[((m >>> 6) & 0x3F)] & 7) == Types.PieceType.PAWN
                    && ((((m >>> 6) & 0x3F) >>> 3) ^ (sideToMove * 7)) > Types.Rank.RANK_4;
        }

        // Piece specific
        public boolean pawn_passed(int c, int s) {
            return ((byColorBB[(c ^ Types.Color.BLACK)] & byTypeBB[Types.PieceType.PAWN]) & Bitboards.PassedPawnMask[c][s]) == 0;
        }

        public boolean opposite_bishops() {
            int sss = pieceList[((Types.Color.WHITE << 3) + Types.PieceType.BISHOP)][0] ^ pieceList[((Types.Color.BLACK << 3) + Types.PieceType.BISHOP)][0];
            return pieceCount[Types.Piece.W_BISHOP] == 1 &&
                    pieceCount[Types.Piece.B_BISHOP] == 1 &&
                    ((((sss >>> 3) ^ sss) & 1) != 0);
        }

        // Doing and undoing moves
        public void do_move(int m) {
            do_move(m, new StateInfo(), gives_check(m));
        }

        public void do_move(int m, StateInfo newSt) {
            do_move(m, newSt, gives_check(m));
        }

        public void do_move(int m, StateInfo newSt, boolean givesCheck) {
            thisThread.nodes += 1;
            long k = st.key ^ Zobrist.side;

            copyCopyableMembersOfStateInfo(newSt, st);
            newSt.previous = st;
            st = newSt;

            ++gamePly;
            ++st.rule50;
            ++st.pliesFromNull;

            int us = sideToMove;
            int them = (us ^ Types.Color.BLACK);
            int from = ((m >>> 6) & 0x3F);
            int to = (m & 0x3F);
            int pc = board[from];
            int captured = (m & (3 << 14)) == Types.MoveType.ENPASSANT ? ((them << 3) + Types.PieceType.PAWN) : board[to];

            if ((m & (3 << 14)) == Types.MoveType.CASTLING) {
                int rfrom, rto;

                boolean kingSide = to > from;
                rfrom = to;
                rto = ((kingSide ? Types.Square.SQ_F1 : Types.Square.SQ_D1) ^ (us * 56));
                to = ((kingSide ? Types.Square.SQ_G1 : Types.Square.SQ_C1) ^ (us * 56));
                remove_piece(((us << 3) + Types.PieceType.KING), from);
                remove_piece(((us << 3) + Types.PieceType.ROOK), rfrom);
                board[from] = board[rfrom] = Types.Piece.NO_PIECE;
                put_piece(((us << 3) + Types.PieceType.KING), to);
                put_piece(((us << 3) + Types.PieceType.ROOK), rto);

                st.psq += PSQT.psq[captured][rto] - PSQT.psq[captured][rfrom];
                k = k ^ Zobrist.psq[captured][rfrom] ^ Zobrist.psq[captured][rto];
                captured = Types.Piece.NO_PIECE;
            }

            if (captured != 0) {
                int capsq = to;

                if ((captured & 7) == Types.PieceType.PAWN) {
                    if ((m & (3 << 14)) == Types.MoveType.ENPASSANT) {
                        capsq -= ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH);
                        board[capsq] = Types.Piece.NO_PIECE;
                    }

                    st.pawnKey = st.pawnKey ^ Zobrist.psq[captured][capsq];
                } else {
                    st.nonPawnMaterial[them] -= PSQT.PieceValue[Types.Phase.MG][captured];
                }

                remove_piece(captured, capsq);

                k = k ^ Zobrist.psq[captured][capsq];
                st.materialKey = st.materialKey ^ Zobrist.psq[captured][pieceCount[captured]];

                st.psq -= PSQT.psq[captured][capsq];

                st.rule50 = 0;
            }

            k = k ^ Zobrist.psq[pc][from] ^ Zobrist.psq[pc][to];

            if (st.epSquare != Types.Square.SQ_NONE) {
                k = k ^ Zobrist.enpassant[(st.epSquare & 7)];
                st.epSquare = Types.Square.SQ_NONE;
            }

            if (st.castlingRights != 0 && (castlingRightsMask[from] | castlingRightsMask[to]) != 0) {
                int cr = castlingRightsMask[from] | castlingRightsMask[to];
                k = k ^ Zobrist.castling[st.castlingRights & cr];
                st.castlingRights &= ~cr;
            }

            if ((m & (3 << 14)) != Types.MoveType.CASTLING) {
                move_piece(pc, from, to);
            }

            if ((pc & 7) == Types.PieceType.PAWN) {
                if ((to ^ from) == 16 && (Bitboards.PawnAttacks[us][to - ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH)] & (byColorBB[them] & byTypeBB[Types.PieceType.PAWN])) != 0) {
                    st.epSquare = to - ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH);
                    k = k ^ Zobrist.enpassant[(st.epSquare & 7)];
                } else if ((m & (3 << 14)) == Types.MoveType.PROMOTION) {
                    int promotion = ((us << 3) + (((m >>> 12) & 3) + Types.PieceType.KNIGHT));

                    remove_piece(pc, to);
                    put_piece(promotion, to);

                    k = k ^ Zobrist.psq[pc][to] ^ Zobrist.psq[promotion][to];
                    st.pawnKey = st.pawnKey ^ Zobrist.psq[pc][to];
                    st.materialKey = st.materialKey ^ Zobrist.psq[promotion][pieceCount[promotion] - 1] ^ Zobrist.psq[pc][pieceCount[pc]];

                    st.psq += PSQT.psq[promotion][to] - PSQT.psq[pc][to];

                    st.nonPawnMaterial[us] += PSQT.PieceValue[Types.Phase.MG][promotion];
                }

                st.pawnKey = st.pawnKey ^ Zobrist.psq[pc][from] ^ Zobrist.psq[pc][to];

                st.rule50 = 0;
            }

            st.psq += PSQT.psq[pc][to] - PSQT.psq[pc][from];

            st.capturedPiece = captured;

            st.key = k;

            st.checkersBB = givesCheck ? attackers_to_as_a_function_of_Square_Bitboard((pieceList[((them << 3) + Types.PieceType.KING)][0]), byTypeBB[Types.PieceType.ALL_PIECES]) & byColorBB[us] : 0;

            sideToMove = (sideToMove ^ Types.Color.BLACK);

            set_check_info(st);
        }

        public void undo_move(int m) {
            sideToMove = (sideToMove ^ Types.Color.BLACK);

            int us = sideToMove;
            int from = ((m >>> 6) & 0x3F);
            int to = (m & 0x3F);
            int pc = board[to];

            if ((m & (3 << 14)) == Types.MoveType.PROMOTION) {
                remove_piece(pc, to);
                pc = ((us << 3) + Types.PieceType.PAWN);
                put_piece(pc, to);
            }

            if ((m & (3 << 14)) == Types.MoveType.CASTLING) {
                int rfrom, rto;

                boolean kingSide = to > from;
                rfrom = to;
                rto = ((kingSide ? Types.Square.SQ_F1 : Types.Square.SQ_D1) ^ (us * 56));
                to = ((kingSide ? Types.Square.SQ_G1 : Types.Square.SQ_C1) ^ (us * 56));
                remove_piece(((us << 3) + Types.PieceType.KING), to);
                remove_piece(((us << 3) + Types.PieceType.ROOK), rto);
                board[to] = board[rto] = Types.Piece.NO_PIECE;
                put_piece(((us << 3) + Types.PieceType.KING), from);
                put_piece(((us << 3) + Types.PieceType.ROOK), rfrom);
            } else {
                move_piece(pc, to, from);

                if (st.capturedPiece != 0) {
                    int capsq = to;

                    if ((m & (3 << 14)) == Types.MoveType.ENPASSANT) {
                        capsq -= ((us == Types.Color.WHITE) ? Types.Direction.NORTH : Types.Direction.SOUTH);
                    }

                    put_piece(st.capturedPiece, capsq);
                }
            }

            st = st.previous;
            --gamePly;
        }

        public void do_null_move(StateInfo newSt) {
            copyEntireMembersOfStateInfo(newSt, st);
            newSt.previous = st;
            st = newSt;

            if (st.epSquare != Types.Square.SQ_NONE) {
                st.key = st.key ^ Zobrist.enpassant[(st.epSquare & 7)];
                st.epSquare = Types.Square.SQ_NONE;
            }

            st.key = st.key ^ Zobrist.side;
            ++st.rule50;
            st.pliesFromNull = 0;

            sideToMove = (sideToMove ^ Types.Color.BLACK);

            set_check_info(st);
        }

        public void undo_null_move() {
            st = st.previous;
            sideToMove = (sideToMove ^ Types.Color.BLACK);
        }

        // Static Exchange Evaluation
        public boolean see_ge(int m) {
            return see_ge(m, Types.Value.VALUE_ZERO);
        }

        public boolean see_ge(int m, int threshold) {
            if ((m & (3 << 14)) != Types.MoveType.NORMAL) {
                return Types.Value.VALUE_ZERO >= threshold;
            }

            int from = ((m >>> 6) & 0x3F), to = (m & 0x3F);
            int nextVictim = (board[from] & 7);
            int stm = ((board[from] >>> 3) ^ Types.Color.BLACK);
            int balance;
            long occupied, stmAttackers;

            balance = PSQT.PieceValue[Types.Phase.MG][board[to]] - threshold;

            if (balance < Types.Value.VALUE_ZERO) {
                return false;
            }

            balance -= PSQT.PieceValue[Types.Phase.MG][nextVictim];

            if (balance >= Types.Value.VALUE_ZERO) {
                return true;
            }

            boolean opponentToMove = true;
            occupied = ((byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[from]) ^ Bitboards.SquareBB[to]);

            long attackers = attackers_to_as_a_function_of_Square_Bitboard(to, occupied) & occupied;

            while (true) {
                stmAttackers = attackers & byColorBB[stm];

                if ((st.pinnersForKing[stm] & ~occupied) == 0) {
                    stmAttackers &= ~st.blockersForKing[stm];
                }

                if (stmAttackers == 0) {
                    break;
                }

                long[] convertedToArrayOccupied = {occupied};
                long[] convertedToArrayAttackers = {attackers};
                nextVictim = min_attacker(Types.PieceType.PAWN, byTypeBB, to, stmAttackers, convertedToArrayOccupied, convertedToArrayAttackers);
                occupied = convertedToArrayOccupied[0];
                attackers = convertedToArrayAttackers[0];

                if (nextVictim == Types.PieceType.KING) {
                    if ((attackers & byColorBB[(stm ^ Types.Color.BLACK)]) == 0) {
                        opponentToMove = !opponentToMove;
                    }
                    break;
                }

                balance += PSQT.PieceValue[Types.Phase.MG][nextVictim];
                opponentToMove = !opponentToMove;

                if (balance < Types.Value.VALUE_ZERO) {
                    break;
                }

                balance = -balance - 1;
                stm = (stm ^ Types.Color.BLACK);
            }

            return opponentToMove;
        }

        public long key_after(int m) {
            int from = ((m >>> 6) & 0x3F);
            int to = (m & 0x3F);
            int pc = board[from];
            int captured = board[to];
            long k = st.key ^ Zobrist.side;

            if (captured != 0) {
                k = k ^ Zobrist.psq[captured][to];
            }

            return k ^ Zobrist.psq[pc][to] ^ Zobrist.psq[pc][from];
        }

        public boolean is_draw(int ply) {
            if (st.rule50 > 99 && (st.checkersBB == 0 || (new Movegen.MoveList(Movegen.GenType.LEGAL, this)).size() != 0)) {
                return true;
            }

            int end = Math.min(st.rule50, st.pliesFromNull);

            if (end < 4) {
                return false;
            }

            StateInfo stp = st.previous.previous;
            int cnt = 0;

            for (int i = 4; i <= end; i += 2) {
                stp = stp.previous.previous;

                if (stp.key == st.key && ++cnt + (ply > i ? 1 : 0) == 2) {
                    return true;
                }
            }
            return false;
        }

        public void flip() {
            StringBuilder f = new StringBuilder();
            String token;
            Scanner ss = new Scanner(fen());
            ss.useDelimiter("[/ ]");

            for (int r = Types.Rank.RANK_8; r >= Types.Rank.RANK_1; --r) {
                token = ss.next();
                f.insert(0, token + (f.length() == 0 ? " " : "/"));
            }

            token = ss.next();
            f.append(token == "w" ? "B " : "W ");

            token = ss.next();
            f.append(token + " ");

            for (int i = 0; i < f.length(); i++) {
                if (Character.isLowerCase(f.charAt(i))) {
                    f.setCharAt(i, Character.toUpperCase(f.charAt(i)));
                } else if (Character.isUpperCase(f.charAt(i))) {
                    f.setCharAt(i, Character.toLowerCase(f.charAt(i)));
                }
            }

            token = ss.next();
            f.append(token == "-" ? token : token.replace(token.charAt(1) == '3' ? '3' : '6', token.charAt(1) == '3' ? '6' : '3'));

            token = ss.nextLine();
            f.append(token);

            set(f.toString(), chess960, st, thisThread);
            ss.close();
        }


        // Initialization helpers (used while setting up a position)
        private void set_castling_right(int c, int rfrom) {
            int kfrom = pieceList[((c << 3) + Types.PieceType.KING)][0];
            int cs = kfrom < rfrom ? Types.CastlingSide.KING_SIDE : Types.CastlingSide.QUEEN_SIDE;
            int cr = Types.operatorORForColorCastlingSide(c, cs);

            st.castlingRights |= cr;
            castlingRightsMask[kfrom] |= cr;
            castlingRightsMask[rfrom] |= cr;
            castlingRookSquare[cr] = rfrom;

            int kto = ((cs == Types.CastlingSide.KING_SIDE ? Types.Square.SQ_G1 : Types.Square.SQ_C1) ^ (c * 56));
            int rto = ((cs == Types.CastlingSide.KING_SIDE ? Types.Square.SQ_F1 : Types.Square.SQ_D1) ^ (c * 56));

            for (int s = Math.min(rfrom, rto); s <= Math.max(rfrom, rto); ++s) {
                if (s != kfrom && s != rfrom) {
                    castlingPath[cr] = (castlingPath[cr] | Bitboards.SquareBB[s]);
                }
            }

            for (int s = Math.min(kfrom, kto); s <= Math.max(kfrom, kto); ++s) {
                if (s != kfrom && s != rfrom) {
                    castlingPath[cr] = (castlingPath[cr] | Bitboards.SquareBB[s]);
                }
            }
        }

        private void set_state(StateInfo si) {
            si.key = si.materialKey = 0;
            si.pawnKey = Zobrist.noPawns;
            si.nonPawnMaterial[Types.Color.WHITE] = si.nonPawnMaterial[Types.Color.BLACK] = Types.Value.VALUE_ZERO;
            si.psq = Types.Score.SCORE_ZERO;
            si.checkersBB = attackers_to_as_a_function_of_Square_Bitboard((pieceList[((sideToMove << 3) + Types.PieceType.KING)][0]), byTypeBB[Types.PieceType.ALL_PIECES]) & byColorBB[(sideToMove ^ Types.Color.BLACK)];

            set_check_info(si);

            for (long b = byTypeBB[Types.PieceType.ALL_PIECES]; b != 0; ) {
                int s = Long.numberOfTrailingZeros(b);
                b &= b - 1;
                int pc = board[s];
                si.key = si.key ^ Zobrist.psq[pc][s];
                si.psq += PSQT.psq[pc][s];
            }

            if (si.epSquare != Types.Square.SQ_NONE) {
                si.key = si.key ^ Zobrist.enpassant[(si.epSquare & 7)];
            }

            if (sideToMove == Types.Color.BLACK) {
                si.key = si.key ^ Zobrist.side;
            }

            si.key = si.key ^ Zobrist.castling[si.castlingRights];

            for (long b = byTypeBB[Types.PieceType.PAWN]; b != 0; ) {
                int s = Long.numberOfTrailingZeros(b);
                b &= b - 1;
                si.pawnKey = si.pawnKey ^ Zobrist.psq[board[s]][s];
            }

            for (int pc : Pieces) {
                if ((pc & 7) != Types.PieceType.PAWN && (pc & 7) != Types.PieceType.KING) {
                    si.nonPawnMaterial[(pc >>> 3)] += pieceCount[pc] * PSQT.PieceValue[Types.Phase.MG][pc];
                }
                for (int cnt = 0; cnt < pieceCount[pc]; ++cnt) {
                    si.materialKey = si.materialKey ^ Zobrist.psq[pc][cnt];
                }
            }
        }

        private void set_check_info(StateInfo si) {
            long[] pinnersWhite = {si.pinnersForKing[Types.Color.WHITE]};
            si.blockersForKing[Types.Color.WHITE] = slider_blockers(byColorBB[Types.Color.BLACK], pieceList[((Types.Color.WHITE << 3) + Types.PieceType.KING)][0], pinnersWhite);
            si.pinnersForKing[Types.Color.WHITE] = pinnersWhite[0];
            long[] pinnersBlack = {si.pinnersForKing[Types.Color.BLACK]};
            si.blockersForKing[Types.Color.BLACK] = slider_blockers(byColorBB[Types.Color.WHITE], pieceList[((Types.Color.BLACK << 3) + Types.PieceType.KING)][0], pinnersBlack);
            si.pinnersForKing[Types.Color.BLACK] = pinnersBlack[0];

            int ksq = pieceList[(((sideToMove ^ Types.Color.BLACK) << 3) + Types.PieceType.KING)][0];

            si.checkSquares[Types.PieceType.PAWN] = Bitboards.PawnAttacks[(sideToMove ^ Types.Color.BLACK)][ksq];
            si.checkSquares[Types.PieceType.KNIGHT] = attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.KNIGHT, ksq);
            si.checkSquares[Types.PieceType.BISHOP] = attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.BISHOP, ksq);
            si.checkSquares[Types.PieceType.ROOK] = attacks_from_as_a_function_of_TemplatePieceType(Types.PieceType.ROOK, ksq);
            si.checkSquares[Types.PieceType.QUEEN] = si.checkSquares[Types.PieceType.BISHOP] | si.checkSquares[Types.PieceType.ROOK];
            si.checkSquares[Types.PieceType.KING] = 0;
        }

        // Other helpers
        private void put_piece(int pc, int s) {
            board[s] = pc;
            byTypeBB[Types.PieceType.ALL_PIECES] = (byTypeBB[Types.PieceType.ALL_PIECES] | Bitboards.SquareBB[s]);
            byTypeBB[(pc & 7)] = (byTypeBB[(pc & 7)] | Bitboards.SquareBB[s]);
            byColorBB[(pc >>> 3)] = (byColorBB[(pc >>> 3)] | Bitboards.SquareBB[s]);
            index[s] = pieceCount[pc]++;
            pieceList[pc][index[s]] = s;
            pieceCount[(((pc >>> 3) << 3) + Types.PieceType.ALL_PIECES)]++;
        }

        private void remove_piece(int pc, int s) {
            byTypeBB[Types.PieceType.ALL_PIECES] = (byTypeBB[Types.PieceType.ALL_PIECES] ^ Bitboards.SquareBB[s]);
            byTypeBB[(pc & 7)] = (byTypeBB[(pc & 7)] ^ Bitboards.SquareBB[s]);
            byColorBB[(pc >>> 3)] = (byColorBB[(pc >>> 3)] ^ Bitboards.SquareBB[s]);

            int lastSquare = pieceList[pc][--pieceCount[pc]];
            index[lastSquare] = index[s];
            pieceList[pc][index[lastSquare]] = lastSquare;
            pieceList[pc][pieceCount[pc]] = Types.Square.SQ_NONE;
            pieceCount[(((pc >>> 3) << 3) + Types.PieceType.ALL_PIECES)]--;
        }

        private void move_piece(int pc, int from, int to) {
            long from_to_bb = Bitboards.SquareBB[from] ^ Bitboards.SquareBB[to];
            byTypeBB[Types.PieceType.ALL_PIECES] ^= from_to_bb;
            byTypeBB[(pc & 7)] ^= from_to_bb;
            byColorBB[(pc >>> 3)] ^= from_to_bb;
            board[from] = Types.Piece.NO_PIECE;
            board[to] = pc;
            index[to] = index[from];
            pieceList[pc][index[to]] = to;
        }

        public static String operatorInsertion(Position pos) {
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("\n +---+---+---+---+---+---+---+---+\n");
            for (int r = Types.Rank.RANK_8; r >= Types.Rank.RANK_1; --r) {
                for (int f = Types.File.FILE_A; f <= Types.File.FILE_H; ++f) {
                    stringBuilder.append(" | " + PieceToChar.charAt(pos.board[((r << 3) + f)]));
                }
                stringBuilder.append(" |\n +---+---+---+---+---+---+---+---+\n");
            }
            stringBuilder.append("\nFen: " + pos.fen() + "\nKey: ");
            stringBuilder.append(Long.toHexString(pos.st.key).toUpperCase());
            stringBuilder.append("\nCheckers: ");

            for (long b = pos.st.checkersBB; b != 0; ) {
                stringBuilder.append(UCI.square(Long.numberOfTrailingZeros(b)) + " ");
                b &= b - 1;
            }
            return stringBuilder.toString();
        }
    }



    public static class PSQT {

        public static int[][] PieceValue = {
                {Types.Value.VALUE_ZERO,
                        Types.Value.PawnValueMg,
                        Types.Value.KnightValueMg,
                        Types.Value.BishopValueMg,
                        Types.Value.RookValueMg,
                        Types.Value.QueenValueMg,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0},

                {Types.Value.VALUE_ZERO,
                        Types.Value.PawnValueEg,
                        Types.Value.KnightValueEg,
                        Types.Value.BishopValueEg,
                        Types.Value.RookValueEg,
                        Types.Value.QueenValueEg,
                        0, 0, 0, 0, 0, 0, 0, 0, 0, 0}};

        public static int ms(int mg, int eg) {
            return ((eg << 16) | (mg & 0xffff));
        }

        public static final int[][][] Bonus = {
                {
                        {0, 0, 0, 0},
                        {0, 0, 0, 0},
                        {0, 0, 0, 0},
                        {0, 0, 0, 0},
                        {0, 0, 0, 0},
                        {0, 0, 0, 0},
                        {0, 0, 0, 0},
                        {0, 0, 0, 0}
                },
                { // Pawn
                        { ms(  0, 0), ms(  0, 0), ms(  0, 0), ms( 0, 0) },
                        { ms(-11, 7), ms(  6,-4), ms(  7, 8), ms( 3,-2) },
                        { ms(-18,-4), ms( -2,-5), ms( 19, 5), ms(24, 4) },
                        { ms(-17, 3), ms( -9, 3), ms( 20,-8), ms(35,-3) },
                        { ms( -6, 8), ms(  5, 9), ms(  3, 7), ms(21,-6) },
                        { ms( -6, 8), ms( -8,-5), ms( -6, 2), ms(-2, 4) },
                        { ms( -4, 3), ms( 20,-9), ms( -8, 1), ms(-4,18) },
                        { 0,          0,          0,          0         }
                },
                { // Knight
                        { ms(-161,-105), ms(-96,-82), ms(-80,-46), ms(-73,-14) },
                        { ms( -83, -69), ms(-43,-54), ms(-21,-17), ms(-10,  9) },
                        { ms( -71, -50), ms(-22,-39), ms(  0, -7), ms(  9, 28) },
                        { ms( -25, -41), ms( 18,-25), ms( 43,  6), ms( 47, 38) },
                        { ms( -26, -46), ms( 16,-25), ms( 38,  3), ms( 50, 40) },
                        { ms( -11, -54), ms( 37,-38), ms( 56, -7), ms( 65, 27) },
                        { ms( -63, -65), ms(-19,-50), ms(  5,-24), ms( 14, 13) },
                        { ms(-195,-109), ms(-67,-89), ms(-42,-50), ms(-29,-13) }
                },
                { // Bishop
                        { ms(-44,-58), ms(-13,-31), ms(-25,-37), ms(-34,-19) },
                        { ms(-20,-34), ms( 20, -9), ms( 12,-14), ms(  1,  4) },
                        { ms( -9,-23), ms( 27,  0), ms( 21, -3), ms( 11, 16) },
                        { ms(-11,-26), ms( 28, -3), ms( 21, -5), ms( 10, 16) },
                        { ms(-11,-26), ms( 27, -4), ms( 16, -7), ms(  9, 14) },
                        { ms(-17,-24), ms( 16, -2), ms( 12,  0), ms(  2, 13) },
                        { ms(-23,-34), ms( 17,-10), ms(  6,-12), ms( -2,  6) },
                        { ms(-35,-55), ms(-11,-32), ms(-19,-36), ms(-29,-17) }
                },
                { // Rook
                        { ms(-25, 0), ms(-16, 0), ms(-16, 0), ms(-9, 0) },
                        { ms(-21, 0), ms( -8, 0), ms( -3, 0), ms( 0, 0) },
                        { ms(-21, 0), ms( -9, 0), ms( -4, 0), ms( 2, 0) },
                        { ms(-22, 0), ms( -6, 0), ms( -1, 0), ms( 2, 0) },
                        { ms(-22, 0), ms( -7, 0), ms(  0, 0), ms( 1, 0) },
                        { ms(-21, 0), ms( -7, 0), ms(  0, 0), ms( 2, 0) },
                        { ms(-12, 0), ms(  4, 0), ms(  8, 0), ms(12, 0) },
                        { ms(-23, 0), ms(-15, 0), ms(-11, 0), ms(-5, 0) }
                },
                { // Queen
                        { ms( 0,-71), ms(-4,-56), ms(-3,-42), ms(-1,-29) },
                        { ms(-4,-56), ms( 6,-30), ms( 9,-21), ms( 8, -5) },
                        { ms(-2,-39), ms( 6,-17), ms( 9, -8), ms( 9,  5) },
                        { ms(-1,-29), ms( 8, -5), ms(10,  9), ms( 7, 19) },
                        { ms(-3,-27), ms( 9, -5), ms( 8, 10), ms( 7, 21) },
                        { ms(-2,-40), ms( 6,-16), ms( 8,-10), ms(10,  3) },
                        { ms(-2,-55), ms( 7,-30), ms( 7,-21), ms( 6, -6) },
                        { ms(-1,-74), ms(-4,-55), ms(-1,-43), ms( 0,-30) }
                },
                { // King
                        { ms(267,  0), ms(320, 48), ms(270, 75), ms(195, 84) },
                        { ms(264, 43), ms(304, 92), ms(238,143), ms(180,132) },
                        { ms(200, 83), ms(245,138), ms(176,167), ms(110,165) },
                        { ms(177,106), ms(185,169), ms(148,169), ms(110,179) },
                        { ms(149,108), ms(177,163), ms(115,200), ms( 66,203) },
                        { ms(118, 95), ms(159,155), ms( 84,176), ms( 41,174) },
                        { ms( 87, 50), ms(128, 99), ms( 63,122), ms( 20,139) },
                        { ms( 63,  9), ms( 88, 55), ms( 47, 80), ms(  0, 90) }
                }
        };

        public static int[][] psq = new int[Types.Piece.PIECE_NB][Types.Square.SQUARE_NB];

        public static void init() {
            for (int pc = Types.Piece.W_PAWN; pc <= Types.Piece.W_KING; ++pc) {
                PieceValue[Types.Phase.MG][(pc ^ 8)] = PieceValue[Types.Phase.MG][pc];
                PieceValue[Types.Phase.EG][(pc ^ 8)] = PieceValue[Types.Phase.EG][pc];

                int v = ((PieceValue[Types.Phase.EG][pc] << 16) | (PieceValue[Types.Phase.MG][pc] & 0xffff));

                for (int s = Types.Square.SQ_A1; s <= Types.Square.SQ_H8; ++s) {
                    int f = Math.min((s & 7), ((s & 7) ^ Types.File.FILE_H));
                    psq[pc][s] = v + Bonus[pc][(s >>> 3)][f];
                    psq[(pc ^ 8)][(s ^ Types.Square.SQ_A8)] = -psq[pc][s];
                }
            }
        }
    }


    public static class Search {

        public static final int CounterMovePruneThreshold = 0;

        public static class Stack {
            public int[] pv;
            public Movepick.PieceToHistory contHistory = new Movepick.PieceToHistory();
            public int ply;
            public int currentMove;
            public int excludedMove;
            public int[] killers = new int[2];
            public int staticEval;
            public int statScore;
            public int moveCount;
        }

        public static class RootMove {
            public int score = -Types.Value.VALUE_INFINITE;
            public int previousScore = -Types.Value.VALUE_INFINITE;
            public int selDepth = 0;
            public List<Integer> pv = new ArrayList<Integer>();

            public RootMove(int m) {
                pv.add(m);
            }

            public boolean extract_ponder_from_tt(Position pos) {
                Position.StateInfo st = new Position.StateInfo();
                boolean ttHit;

                if (pv.get(0) == 0) {
                    return false;
                }

                pos.do_move(pv.get(0), st);
                boolean[] ttHitArray = new boolean[1];
                tt.TTEntry tte = tt.TT.probe(pos.st.key, ttHitArray);
                ttHit = ttHitArray[0];

                if (ttHit) {
                    int m = tte.move();
                    if (new Movegen.MoveList(Movegen.GenType.LEGAL, pos).contains(m)) {
                        pv.add(m);
                    }
                }

                pos.undo_move(pv.get(0));
                return pv.size() > 1;
            }

            public boolean operatorEquals(int m) {
                return pv.get(0) == m;
            }

            public static final Comparator<RootMove> RootMoveComparator = new Comparator<RootMove>() {
                @Override
                public int compare(RootMove rootMove1, RootMove rootMove2) {
                    return rootMove1.score != rootMove2.score ? rootMove2.score - rootMove1.score : rootMove2.previousScore - rootMove1.previousScore;
                }
            };

            public boolean operatorLess(RootMove m) {
                return m.score != score ? m.score < score : m.previousScore < previousScore;
            }
        }


        public static class RootMoves extends ArrayList<RootMove> {
            private static final long serialVersionUID = -2488917724189725446L;
        }


        public static class LimitsType {
            public List<Integer> searchmoves = new ArrayList<>();
            public int[] time = new int[Types.Color.COLOR_NB];
            public int[] inc = new int[Types.Color.COLOR_NB];
            public int npmsec, movestogo, depth, movetime, mate, perft, infinite;
            public long nodes;
            public long startTime;

            public LimitsType() {
                time[Types.Color.WHITE] = 0;
                time[Types.Color.BLACK] = 0;
                inc[Types.Color.WHITE] = 0;
                inc[Types.Color.BLACK] = 0;
                npmsec = 0;
                movestogo = 0;
                depth = 0;
                movetime = 0;
                mate = 0;
                perft = 0;
                infinite = 0;
                nodes = 0;
            }

            public boolean use_time_management() {
                return (mate | movetime | depth | nodes | perft | infinite) == 0;
            }
        }

        public static LimitsType Limits = new LimitsType();

        public static class NodeType {
            public static final int NonPV = 0;
            public static final int PV = 1;
        }

        public static final int[] skipSize = { 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4 };
        public static final int[] skipPhase = { 0, 1, 0, 1, 2, 3, 0, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 5, 6, 7 };

        public static final int razor_margin = 600;

        public static int futility_margin(int d) {
            return 150 * d / Types.Depth.ONE_PLY;
        }

        public static int[][] FutilityMoveCounts = new int[2][16];
        public static int[][][][] Reductions = new int[2][2][64][64];

        public static int reduction(boolean PvNode, boolean i, int d, int mn) {
            return Reductions[PvNode ? 1 : 0][i ? 1 : 0][Math.min(d / Types.Depth.ONE_PLY, 63)][Math.min(mn, 63)] * Types.Depth.ONE_PLY;
        }

        public static int stat_bonus(int depth) {
            int d = depth / Types.Depth.ONE_PLY;
            return d > 17 ? 0 : d * d + 2 * d - 2;
        }

        public static class Skill {
            public int level;
            public int best = Types.Move.MOVE_NONE;

            public Skill(int l) {
                level = l;
            }

            public boolean enabled() {
                return level < 20;
            }

            public boolean time_to_pick(int depth) {
                return depth / Types.Depth.ONE_PLY == 1 + level;
            }


            public static Misc.PRNG rng = new Misc.PRNG(System.currentTimeMillis());
            public int pick_best(int multiPV) {
                final RootMoves rootMoves = thread.Threads.main().rootMoves;

                int topScore = rootMoves.get(0).score;
                int delta = Math.min(topScore - rootMoves.get(multiPV - 1).score, Types.Value.PawnValueMg);
                int weakness = 120 - 2 * level;
                int maxScore = -Types.Value.VALUE_INFINITE;

                for (int i = 0; i < multiPV; ++i) {
                    int push = (weakness * (topScore - rootMoves.get(i).score) + delta * (((int) (rng.rand() & 0xffffffffL)) % weakness)) / 128;

                    if (rootMoves.get(i).score + push >= maxScore) {
                        maxScore = rootMoves.get(i).score + push;
                        best = rootMoves.get(i).pv.get(0);
                    }
                }

                return best;
            }
        }

        public static int search(int NT, Position pos, Stack[] stackForSS, int ss, int alpha, int beta, int depth, boolean cutNode, boolean skipEarlyPruning) {
            final boolean PvNode = NT == NodeType.PV;
            final boolean rootNode = PvNode && stackForSS[ss].ply == 0;

            int[] pv = new int[Types.MAX_PLY + 1];
            int[] capturesSearched = new int[32];
            int[] quietsSearched = new int[64];

            Position.StateInfo st = new Position.StateInfo();
            tt.TTEntry tte;
            long posKey;
            int ttMove, move, excludedMove, bestMove;
            int extension, newDepth;
            int bestValue, value, ttValue, eval, maxValue;
            boolean ttHit, inCheck, givesCheck, singularExtensionNode, improving;
            boolean captureOrPromotion, doFullDepthSearch, moveCountPruning, skipQuiets, ttCapture, pvExact;
            int movedPiece;
            int moveCount, captureCount, quietCount;

            // Step 1.
            thread.Threadd thisThread = pos.thisThread;
            inCheck = pos.st.checkersBB != 0;
            moveCount = captureCount = quietCount = stackForSS[ss].moveCount = 0;
            stackForSS[ss].statScore = 0;
            bestValue = -Types.Value.VALUE_INFINITE;
            maxValue = Types.Value.VALUE_INFINITE;

            if (thisThread == thread.Threads.main()) {
                ((thread.MainThread) thisThread).check_time();
            }

            if (PvNode && thisThread.selDepth < stackForSS[ss].ply + 1) {
                thisThread.selDepth = stackForSS[ss].ply + 1;
            }

            if (!rootNode) {
                // Step 2.
                if (thread.Threads.stop || pos.is_draw(stackForSS[ss].ply) || stackForSS[ss].ply >= Types.MAX_PLY) {
                    return stackForSS[ss].ply >= Types.MAX_PLY && !inCheck ? Eval.evaluate(pos) : Types.Value.VALUE_DRAW;
                }

                // Step 3.
                alpha = Math.max((-Types.Value.VALUE_MATE + stackForSS[ss].ply), alpha);
                beta = Math.min((Types.Value.VALUE_MATE - (stackForSS[ss].ply + 1)), beta);
                if (alpha >= beta) {
                    return alpha;
                }
            }

            stackForSS[ss + 1].ply = stackForSS[ss].ply + 1;
            stackForSS[ss].currentMove = stackForSS[ss + 1].excludedMove = bestMove = Types.Move.MOVE_NONE;
            stackForSS[ss].contHistory = thisThread.contHistory.array[Types.Piece.NO_PIECE][0];
            stackForSS[ss + 2].killers[0] = stackForSS[ss + 2].killers[1] = Types.Move.MOVE_NONE;
            int prevSq = (stackForSS[ss - 1].currentMove & 0x3F);

            // Step 4.
            excludedMove = stackForSS[ss].excludedMove;
            posKey = pos.st.key ^ (excludedMove << 16);
            boolean[] ttHitArray = new boolean[1];
            tte = tt.TT.probe(posKey, ttHitArray);
            ttHit = ttHitArray[0];
            ttValue = ttHit ? value_from_tt(tte.value(), stackForSS[ss].ply) : Types.Value.VALUE_NONE;
            ttMove = rootNode ? thisThread.rootMoves.get(thisThread.PVIdx).pv.get(0) : ttHit ? tte.move() : Types.Move.MOVE_NONE;

            if (!PvNode && ttHit && tte.depth() >= depth && ttValue != Types.Value.VALUE_NONE &&
                    (ttValue >= beta ? (tte.bound() & Types.Bound.BOUND_LOWER) : (tte.bound() & Types.Bound.BOUND_UPPER)) != 0) {
                if (ttMove != 0) {
                    if (ttValue >= beta) {
                        if (!pos.capture_or_promotion(ttMove)) {
                            update_stats(pos, stackForSS, ss, ttMove, null, 0, stat_bonus(depth));
                        }

                        if (stackForSS[ss - 1].moveCount == 1 && pos.st.capturedPiece == 0) {
                            update_continuation_histories(stackForSS, ss - 1, pos.board[prevSq], prevSq, -stat_bonus(depth + Types.Depth.ONE_PLY));
                        }
                    } else if (!pos.capture_or_promotion(ttMove)) {
                        int penalty = -stat_bonus(depth);
                        thisThread.mainHistory.update(pos.sideToMove, ttMove, penalty);
                        update_continuation_histories(stackForSS, ss, pos.board[((ttMove >>> 6) & 0x3F)], (ttMove & 0x3F), penalty);
                    }
                }
                return ttValue;
            }

            while (true) {
                // Step 5.
                if (inCheck) {
                    stackForSS[ss].staticEval = eval = Types.Value.VALUE_NONE;
                    break;
                } else if (ttHit) {
                    if ((stackForSS[ss].staticEval = eval = tte.eval()) == Types.Value.VALUE_NONE) {
                        eval = stackForSS[ss].staticEval = Eval.evaluate(pos);
                    }
                    if (ttValue != Types.Value.VALUE_NONE && (tte.bound() & (ttValue > eval ? Types.Bound.BOUND_LOWER : Types.Bound.BOUND_UPPER)) != 0) {
                        eval = ttValue;
                    }
                } else {
                    eval = stackForSS[ss].staticEval =
                            stackForSS[ss - 1].currentMove != Types.Move.MOVE_NULL ? Eval.evaluate(pos) : -stackForSS[ss - 1].staticEval + 2 * Eval.Tempo;

                    tte.save(posKey, Types.Value.VALUE_NONE, Types.Bound.BOUND_NONE, Types.Depth.DEPTH_NONE, Types.Move.MOVE_NONE, stackForSS[ss].staticEval, tt.TT.generation());
                }

                if (skipEarlyPruning || pos.st.nonPawnMaterial[pos.sideToMove] == 0) {
                    break;
                }

                // Step 6.
                if (!PvNode && depth < 4 * Types.Depth.ONE_PLY && eval + razor_margin <= alpha) {
                    if (depth <= Types.Depth.ONE_PLY) {
                        return qsearch(NodeType.NonPV, false, pos, stackForSS, ss, alpha, alpha + 1);
                    }

                    int ralpha = alpha - razor_margin;
                    int v = qsearch(NodeType.NonPV, false, pos, stackForSS, ss, ralpha, ralpha + 1);
                    if (v <= ralpha) {
                        return v;
                    }
                }

                // Step 7.
                if (!rootNode && depth < 7 * Types.Depth.ONE_PLY && eval - futility_margin(depth) >= beta && eval < Types.Value.VALUE_KNOWN_WIN) {
                    return eval;
                }

                // Step 8.
                if (!PvNode && eval >= beta && stackForSS[ss].staticEval >= beta - 36 * depth / Types.Depth.ONE_PLY + 225 && (stackForSS[ss].ply >= thisThread.nmp_ply || stackForSS[ss].ply % 2 != thisThread.nmp_odd)) {
                    int R = ((823 + 67 * depth / Types.Depth.ONE_PLY) / 256 + Math.min((eval - beta) / Types.Value.PawnValueMg, 3)) * Types.Depth.ONE_PLY;

                    stackForSS[ss].currentMove = Types.Move.MOVE_NULL;
                    stackForSS[ss].contHistory = thisThread.contHistory.array[Types.Piece.NO_PIECE][0];

                    pos.do_null_move(st);
                    int nullValue = depth - R < Types.Depth.ONE_PLY ? -qsearch(NodeType.NonPV, false, pos, stackForSS, ss + 1, -beta, -beta + 1) : -search(NodeType.NonPV, pos, stackForSS, ss + 1, -beta, -beta + 1, depth - R, !cutNode, true);
                    pos.undo_null_move();

                    if (nullValue >= beta) {
                        if (nullValue >= Types.Value.VALUE_MATE_IN_MAX_PLY) {
                            nullValue = beta;
                        }

                        if (Math.abs(beta) < Types.Value.VALUE_KNOWN_WIN && (depth < 12 * Types.Depth.ONE_PLY || thisThread.nmp_ply != 0)) {
                            return nullValue;
                        }

                        thisThread.nmp_ply = stackForSS[ss].ply + 3 * (depth - R) / 4;
                        thisThread.nmp_odd = stackForSS[ss].ply % 2;

                        int v = depth - R < Types.Depth.ONE_PLY ? qsearch(NodeType.NonPV, false, pos, stackForSS, ss, beta - 1, beta) : search(NodeType.NonPV, pos, stackForSS, ss, beta - 1, beta, depth - R, false, true);

                        thisThread.nmp_odd = thisThread.nmp_ply = 0;

                        if (v >= beta) {
                            return nullValue;
                        }
                    }
                }

                // Step 9.
                if (!PvNode && depth >= 5 * Types.Depth.ONE_PLY && Math.abs(beta) < Types.Value.VALUE_MATE_IN_MAX_PLY) {
                    int rbeta = Math.min(beta + 200, Types.Value.VALUE_INFINITE);

                    Movepick.MovePicker mp = new Movepick.MovePicker(pos, ttMove, rbeta - stackForSS[ss].staticEval, thisThread.captureHistory);

                    while ((move = mp.next_move(false)) != Types.Move.MOVE_NONE) {
                        if (pos.legal(move)) {
                            stackForSS[ss].currentMove = move;
                            stackForSS[ss].contHistory = thisThread.contHistory.array[pos.board[((move >>> 6) & 0x3F)]][(move & 0x3F)];

                            pos.do_move(move, st);
                            value = -search(NodeType.NonPV, pos, stackForSS, ss + 1, -rbeta, -rbeta + 1, depth - 4 * Types.Depth.ONE_PLY, !cutNode, false);
                            pos.undo_move(move);
                            if (value >= rbeta) {
                                return value;
                            }
                        }
                    }
                }

                // Step 10.
                if (depth >= 6 * Types.Depth.ONE_PLY && ttMove == 0 && (PvNode || stackForSS[ss].staticEval + 256 >= beta)) {
                    int d = (3 * depth / (4 * Types.Depth.ONE_PLY) - 2) * Types.Depth.ONE_PLY;
                    search(NT, pos, stackForSS, ss, alpha, beta, d, cutNode, true);

                    tte = tt.TT.probe(posKey, ttHitArray);
                    ttHit = ttHitArray[0];
                    ttMove = ttHit ? tte.move() : Types.Move.MOVE_NONE;
                }
                break;
            }

            // moves_loop:  When in check search starts from here
            Movepick.PieceToHistory[] contHist = new Movepick.PieceToHistory[] {stackForSS[ss - 1].contHistory, stackForSS[ss - 2].contHistory, null, stackForSS[ss - 4].contHistory};
            int countermove = thisThread.counterMoves.array[pos.board[prevSq]][prevSq];

            Movepick.MovePicker mp = new Movepick.MovePicker(pos, ttMove, depth, thisThread.mainHistory, thisThread.captureHistory, contHist, countermove, stackForSS[ss].killers);
            value = bestValue;
            improving = stackForSS[ss].staticEval >= stackForSS[ss - 2].staticEval || stackForSS[ss - 2].staticEval == Types.Value.VALUE_NONE;

            singularExtensionNode = !rootNode
                    &&  depth >= 8 * Types.Depth.ONE_PLY
                    &&  ttMove != Types.Move.MOVE_NONE
                    &&  ttValue != Types.Value.VALUE_NONE
                    && excludedMove == 0
                    && (tte.bound() & Types.Bound.BOUND_LOWER) != 0
                    &&  tte.depth() >= depth - 3 * Types.Depth.ONE_PLY;
            skipQuiets = false;
            ttCapture = false;
            pvExact = PvNode && ttHit && tte.bound() == Types.Bound.BOUND_EXACT;

            // Step 11.
            while ((move = mp.next_move(skipQuiets)) != Types.Move.MOVE_NONE) {
                if (move == excludedMove) {
                    continue;
                }

                boolean rootMovesHavingThisMove = false;
                for (int i = thisThread.PVIdx; i < thisThread.rootMoves.size(); i++) {
                    if (thisThread.rootMoves.get(i).operatorEquals(move)) {
                        rootMovesHavingThisMove = true;
                        break;
                    }
                }
                if (rootNode && !rootMovesHavingThisMove) {
                    continue;
                }

                stackForSS[ss].moveCount = ++moveCount;

                if (PvNode) {
                    stackForSS[ss + 1].pv = null;
                }

                extension = Types.Depth.DEPTH_ZERO;
                captureOrPromotion = pos.capture_or_promotion(move);
                movedPiece = pos.board[((move >>> 6) & 0x3F)];

                givesCheck = (move & (3 << 14)) == Types.MoveType.NORMAL && (pos.st.blockersForKing[(pos.sideToMove ^ Types.Color.BLACK)] & pos.byColorBB[pos.sideToMove]) == 0
                        ? (pos.st.checkSquares[(movedPiece & 7)] & Bitboards.SquareBB[(move & 0x3F)]) != 0
                        : pos.gives_check(move);

                moveCountPruning = depth < 16 * Types.Depth.ONE_PLY && moveCount >= FutilityMoveCounts[improving ? 1 : 0][depth / Types.Depth.ONE_PLY];

                // Step 12.
                if (singularExtensionNode && move == ttMove && pos.legal(move)) {
                    int rBeta = Math.max(ttValue - 2 * depth / Types.Depth.ONE_PLY, -Types.Value.VALUE_MATE);
                    int d = (depth / (2 * Types.Depth.ONE_PLY)) * Types.Depth.ONE_PLY;
                    stackForSS[ss].excludedMove = move;
                    value = search(NodeType.NonPV, pos, stackForSS, ss, rBeta - 1, rBeta, d, cutNode, true);
                    stackForSS[ss].excludedMove = Types.Move.MOVE_NONE;

                    if (value < rBeta) {
                        extension = Types.Depth.ONE_PLY;
                    }
                } else if (givesCheck && !moveCountPruning && pos.see_ge(move)) {
                    extension = Types.Depth.ONE_PLY;
                }

                newDepth = depth - Types.Depth.ONE_PLY + extension;

                // Step 13.
                if (!rootNode && pos.st.nonPawnMaterial[pos.sideToMove] != 0 && bestValue > Types.Value.VALUE_MATED_IN_MAX_PLY) {
                    if (!captureOrPromotion && !givesCheck && (!pos.advanced_pawn_push(move) || (pos.st.nonPawnMaterial[Types.Color.WHITE] + pos.st.nonPawnMaterial[Types.Color.BLACK]) >= 5000)) {
                        if (moveCountPruning) {
                            skipQuiets = true;
                            continue;
                        }

                        int lmrDepth = Math.max(newDepth - reduction(PvNode, improving, depth, moveCount), Types.Depth.DEPTH_ZERO) / Types.Depth.ONE_PLY;

                        if (lmrDepth < 3 && contHist[0].array[movedPiece][(move & 0x3F)] < CounterMovePruneThreshold && contHist[1].array[movedPiece][(move & 0x3F)] < CounterMovePruneThreshold) {
                            continue;
                        }

                        if (lmrDepth < 7 && !inCheck && stackForSS[ss].staticEval + 256 + 200 * lmrDepth <= alpha) {
                            continue;
                        }

                        if (lmrDepth < 8 && !pos.see_ge(move, -35 * lmrDepth * lmrDepth)) {
                            continue;
                        }
                    } else if (depth < 7 * Types.Depth.ONE_PLY && extension == 0 && !pos.see_ge(move, -Types.Value.PawnValueEg * (depth / Types.Depth.ONE_PLY))) {
                        continue;
                    }
                }

                if (!rootNode && !pos.legal(move)) {
                    stackForSS[ss].moveCount = --moveCount;
                    continue;
                }

                if (move == ttMove && captureOrPromotion) {
                    ttCapture = true;
                }

                stackForSS[ss].currentMove = move;
                stackForSS[ss].contHistory = thisThread.contHistory.array[movedPiece][(move & 0x3F)];

                // Step 14.
                pos.do_move(move, st, givesCheck);

                // Step 15.
                if (depth >= 3 * Types.Depth.ONE_PLY && moveCount > 1 && (!captureOrPromotion || moveCountPruning)) {
                    int r = reduction(PvNode, improving, depth, moveCount);

                    if (captureOrPromotion) {
                        r -= r != 0 ? Types.Depth.ONE_PLY : Types.Depth.DEPTH_ZERO;
                    } else {
                        if (stackForSS[ss - 1].moveCount > 15) {
                            r -= Types.Depth.ONE_PLY;
                        }

                        if (pvExact) {
                            r -= Types.Depth.ONE_PLY;
                        }

                        if (ttCapture) {
                            r += Types.Depth.ONE_PLY;
                        }

                        if (cutNode) {
                            r += 2 * Types.Depth.ONE_PLY;
                        } else if ((move & (3 << 14)) == Types.MoveType.NORMAL && !pos.see_ge((((move & 0x3F) << 6) + ((move >>> 6) & 0x3F)))) {
                            r -= 2 * Types.Depth.ONE_PLY;
                        }

                        stackForSS[ss].statScore = thisThread.mainHistory.array[((pos.sideToMove) ^ Types.Color.BLACK)][(move & 0xFFF)]
                                + contHist[0].array[movedPiece][(move & 0x3F)]
                                + contHist[1].array[movedPiece][(move & 0x3F)]
                                + contHist[3].array[movedPiece][(move & 0x3F)]
                                - 4000;

                        if (stackForSS[ss].statScore >= 0 && stackForSS[ss - 1].statScore < 0) {
                            r -= Types.Depth.ONE_PLY;
                        } else if (stackForSS[ss - 1].statScore >= 0 && stackForSS[ss].statScore < 0) {
                            r += Types.Depth.ONE_PLY;
                        }

                        r = Math.max(Types.Depth.DEPTH_ZERO, (r / Types.Depth.ONE_PLY - stackForSS[ss].statScore / 20000) * Types.Depth.ONE_PLY);
                    }

                    int d = Math.max(newDepth - r, Types.Depth.ONE_PLY);

                    value = -search(NodeType.NonPV, pos, stackForSS, ss + 1, -(alpha + 1), -alpha, d, true, false);

                    doFullDepthSearch = (value > alpha && d != newDepth);
                } else {
                    doFullDepthSearch = !PvNode || moveCount > 1;
                }

                // Step 16.
                if (doFullDepthSearch) {
                    value = newDepth < Types.Depth.ONE_PLY ? givesCheck ? -qsearch(NodeType.NonPV, true, pos, stackForSS, ss + 1, -(alpha + 1), -alpha) : -qsearch(NodeType.NonPV, false, pos, stackForSS, ss + 1, -(alpha + 1), -alpha)
                            : -search(NodeType.NonPV, pos, stackForSS, ss + 1, -(alpha + 1), -alpha, newDepth, !cutNode, false);
                }

                if (PvNode && (moveCount == 1 || (value > alpha && (rootNode || value < beta)))) {
                    stackForSS[ss + 1].pv = pv;
                    stackForSS[ss + 1].pv[0] = Types.Move.MOVE_NONE;

                    value = newDepth < Types.Depth.ONE_PLY ? givesCheck ? -qsearch(NodeType.PV, true, pos, stackForSS, ss + 1, -beta, -alpha) : -qsearch(NodeType.PV, false, pos, stackForSS, ss + 1, -beta, -alpha)
                            : -search(NodeType.PV, pos, stackForSS,  ss + 1, -beta, -alpha, newDepth, false, false);
                }

                // Step 17.
                pos.undo_move(move);

                // Step 18.
                if (thread.Threads.stop) {
                    return Types.Value.VALUE_ZERO;
                }

                if (rootNode) {
                    RootMove rm = null;
                    for (int i = 0; i < thisThread.rootMoves.size(); i++) {
                        if (thisThread.rootMoves.get(i).operatorEquals(move)) {
                            rm = thisThread.rootMoves.get(i);
                            break;
                        }
                    }

                    if (moveCount == 1 || value > alpha) {
                        rm.score = value;
                        rm.selDepth = thisThread.selDepth;
                        List<Integer> temp = new ArrayList<>();
                        temp.add(rm.pv.get(0));
                        rm.pv = temp;

                        for (int m = 0; m < stackForSS[ss + 1].pv.length && stackForSS[ss + 1].pv[m] != Types.Move.MOVE_NONE; ++m) {
                            rm.pv.add(stackForSS[ss + 1].pv[m]);
                        }

                        if (moveCount > 1 && thisThread == thread.Threads.main()) {
                            ++((thread.MainThread) thisThread).bestMoveChanges;
                        }
                    } else {
                        rm.score = -Types.Value.VALUE_INFINITE;
                    }
                }

                if (value > bestValue) {
                    bestValue = value;

                    if (value > alpha) {
                        bestMove = move;

                        if (PvNode && !rootNode) {
                            update_pv(stackForSS[ss].pv, move, stackForSS[ss + 1].pv);
                        }

                        if (PvNode && value < beta) {
                            alpha = value;
                        } else {
                            break;
                        }
                    }
                }

                if (!captureOrPromotion && move != bestMove && quietCount < 64) {
                    quietsSearched[quietCount++] = move;
                } else if (captureOrPromotion && move != bestMove && captureCount < 32) {
                    capturesSearched[captureCount++] = move;
                }
            }


            // Step 20.
            if (moveCount == 0) {
                bestValue = excludedMove != 0 ? alpha : inCheck ? (-Types.Value.VALUE_MATE + stackForSS[ss].ply) : Types.Value.VALUE_DRAW;
            } else if (bestMove != 0) {
                if (!pos.capture_or_promotion(bestMove)) {
                    update_stats(pos, stackForSS, ss, bestMove, quietsSearched, quietCount, stat_bonus(depth));
                } else {
                    update_capture_stats(pos, bestMove, capturesSearched, captureCount, stat_bonus(depth));
                }

                if (stackForSS[ss - 1].moveCount == 1 && pos.st.capturedPiece == 0) {
                    update_continuation_histories(stackForSS, ss - 1, pos.board[prevSq], prevSq, -stat_bonus(depth + Types.Depth.ONE_PLY));
                }
            } else if (depth >= 3 * Types.Depth.ONE_PLY && pos.st.capturedPiece == 0 && (((stackForSS[ss - 1].currentMove >>> 6) & 0x3F) != (stackForSS[ss - 1].currentMove & 0x3F))) {
                update_continuation_histories(stackForSS, ss - 1, pos.board[prevSq], prevSq, stat_bonus(depth));
            }

            if (PvNode) {
                bestValue = Math.min(bestValue, maxValue);
            }

            if (excludedMove == 0) {
                tte.save(posKey, value_to_tt(bestValue, stackForSS[ss].ply), (bestValue >= beta ? Types.Bound.BOUND_LOWER : PvNode && bestMove != 0 ? Types.Bound.BOUND_EXACT : Types.Bound.BOUND_UPPER), depth, bestMove, stackForSS[ss].staticEval, tt.TT.generation());
            }

            return bestValue;
        }


        public static int qsearch(int NT, boolean InCheck, Position pos, Stack[] stackForSS, int ss, int alpha, int beta, int depth) {
            final boolean PvNode = NT == NodeType.PV;

            int[] pv = new int[Types.MAX_PLY + 1];
            Position.StateInfo st = new Position.StateInfo();
            tt.TTEntry tte;
            long posKey;
            int ttMove, move, bestMove;
            int bestValue, value, ttValue, futilityValue, futilityBase, oldAlpha = 0;
            boolean ttHit, givesCheck, evasionPrunable;
            int ttDepth;
            int moveCount;

            if (PvNode) {
                oldAlpha = alpha;
                stackForSS[ss + 1].pv = pv;
                stackForSS[ss].pv[0] = Types.Move.MOVE_NONE;
            }

            stackForSS[ss].currentMove = bestMove = Types.Move.MOVE_NONE;
            stackForSS[ss + 1].ply = stackForSS[ss].ply + 1;
            moveCount = 0;

            if (pos.is_draw(stackForSS[ss].ply) || stackForSS[ss].ply >= Types.MAX_PLY) {
                return stackForSS[ss].ply >= Types.MAX_PLY && !InCheck ? Eval.evaluate(pos) : Types.Value.VALUE_DRAW;
            }

            ttDepth = InCheck || depth >= Types.Depth.DEPTH_QS_CHECKS ? Types.Depth.DEPTH_QS_CHECKS : Types.Depth.DEPTH_QS_NO_CHECKS;
            posKey = pos.st.key;
            boolean[] ttHitArray = new boolean[1];
            tte = tt.TT.probe(posKey, ttHitArray);
            ttHit = ttHitArray[0];
            ttMove = ttHit ? tte.move() : Types.Move.MOVE_NONE;
            ttValue = ttHit ? value_from_tt(tte.value(), stackForSS[ss].ply) : Types.Value.VALUE_NONE;

            if (!PvNode && ttHit && tte.depth() >= ttDepth && ttValue != Types.Value.VALUE_NONE && (ttValue >= beta ? (tte.bound() & Types.Bound.BOUND_LOWER) : (tte.bound() & Types.Bound.BOUND_UPPER)) != 0) {
                return ttValue;
            }

            if (InCheck) {
                stackForSS[ss].staticEval = Types.Value.VALUE_NONE;
                bestValue = futilityBase = -Types.Value.VALUE_INFINITE;
            } else {
                if (ttHit) {
                    if ((stackForSS[ss].staticEval = bestValue = tte.eval()) == Types.Value.VALUE_NONE) {
                        stackForSS[ss].staticEval = bestValue = Eval.evaluate(pos);
                    }

                    if (ttValue != Types.Value.VALUE_NONE && (tte.bound() & (ttValue > bestValue ? Types.Bound.BOUND_LOWER : Types.Bound.BOUND_UPPER)) != 0) {
                        bestValue = ttValue;
                    }
                } else {
                    stackForSS[ss].staticEval = bestValue = stackForSS[ss - 1].currentMove != Types.Move.MOVE_NULL ? Eval.evaluate(pos) : -stackForSS[ss - 1].staticEval + 2 * Eval.Tempo;
                }

                if (bestValue >= beta) {
                    if (!ttHit) {
                        tte.save(posKey, value_to_tt(bestValue, stackForSS[ss].ply), Types.Bound.BOUND_LOWER, Types.Depth.DEPTH_NONE, Types.Move.MOVE_NONE, stackForSS[ss].staticEval, tt.TT.generation());
                    }

                    return bestValue;
                }

                if (PvNode && bestValue > alpha) {
                    alpha = bestValue;
                }

                futilityBase = bestValue + 128;
            }

            Movepick.MovePicker mp = new Movepick.MovePicker(pos, ttMove, depth, pos.thisThread.mainHistory, pos.thisThread.captureHistory, (stackForSS[ss - 1].currentMove & 0x3F));

            while ((move = mp.next_move(false)) != Types.Move.MOVE_NONE) {
                givesCheck = (move & (3 << 14)) == Types.MoveType.NORMAL && (pos.st.blockersForKing[(pos.sideToMove ^ Types.Color.BLACK)] & pos.byColorBB[pos.sideToMove]) == 0 ?
                        (pos.st.checkSquares[(pos.board[((move >>> 6) & 0x3F)] & 7)] & Bitboards.SquareBB[(move & 0x3F)]) != 0 : pos.gives_check(move);

                moveCount++;

                if (!InCheck && !givesCheck && futilityBase > -Types.Value.VALUE_KNOWN_WIN && !pos.advanced_pawn_push(move)) {
                    futilityValue = futilityBase + PSQT.PieceValue[Types.Phase.EG][pos.board[(move & 0x3F)]];

                    if (futilityValue <= alpha) {
                        bestValue = Math.max(bestValue, futilityValue);
                        continue;
                    }

                    if (futilityBase <= alpha && !pos.see_ge(move, Types.Value.VALUE_ZERO + 1)) {
                        bestValue = Math.max(bestValue, futilityBase);
                        continue;
                    }
                }

                evasionPrunable = InCheck && (depth != Types.Depth.DEPTH_ZERO || moveCount > 2) && bestValue > Types.Value.VALUE_MATED_IN_MAX_PLY && !pos.capture(move);

                if ((!InCheck || evasionPrunable) && !pos.see_ge(move)) {
                    continue;
                }

                if (!pos.legal(move)) {
                    moveCount--;
                    continue;
                }

                stackForSS[ss].currentMove = move;

                pos.do_move(move, st, givesCheck);
                value = givesCheck ? -qsearch(NT, true, pos, stackForSS, ss + 1, -beta, -alpha, depth - Types.Depth.ONE_PLY) : -qsearch(NT, false, pos, stackForSS, ss + 1, -beta, -alpha, depth - Types.Depth.ONE_PLY);
                pos.undo_move(move);

                if (value > bestValue) {
                    bestValue = value;

                    if (value > alpha) {
                        if (PvNode) {
                            update_pv(stackForSS[ss].pv, move, stackForSS[ss + 1].pv);
                        }

                        if (PvNode && value < beta) {
                            alpha = value;
                            bestMove = move;
                        } else {
                            tte.save(posKey, value_to_tt(value, stackForSS[ss].ply), Types.Bound.BOUND_LOWER, ttDepth, move, stackForSS[ss].staticEval, tt.TT.generation());

                            return value;
                        }
                    }
                }
            }

            if (InCheck && bestValue == -Types.Value.VALUE_INFINITE) {
                return (-Types.Value.VALUE_MATE + stackForSS[ss].ply);
            }

            tte.save(posKey, value_to_tt(bestValue, stackForSS[ss].ply), PvNode && bestValue > oldAlpha ? Types.Bound.BOUND_EXACT : Types.Bound.BOUND_UPPER, ttDepth, bestMove, stackForSS[ss].staticEval, tt.TT.generation());

            return bestValue;
        }


        public static int qsearch(int NT, boolean InCheck, Position pos, Stack[] stackForSS, int ss, int alpha, int beta) {
            return qsearch(NT, InCheck, pos, stackForSS, ss, alpha, beta, Types.Depth.DEPTH_ZERO);
        }


        public static int value_to_tt(int v, int ply) {
            return v >= Types.Value.VALUE_MATE_IN_MAX_PLY ? v + ply : v <= Types.Value.VALUE_MATED_IN_MAX_PLY ? v - ply : v;
        }


        public static int value_from_tt(int v, int ply) {
            return v == Types.Value.VALUE_NONE ? Types.Value.VALUE_NONE : v >= Types.Value.VALUE_MATE_IN_MAX_PLY ? v - ply : v <= Types.Value.VALUE_MATED_IN_MAX_PLY ? v + ply : v;
        }


        public static void update_pv(int[] pv, int move, int[] childPv) {
            int i = 0;
            for (pv[i++] = move; childPv != null && i < childPv.length && childPv[i] != Types.Move.MOVE_NONE; ) {
                pv[i] = childPv[i - 1];
                i++;
            }
            pv[i] = Types.Move.MOVE_NONE;
        }


        public static void update_continuation_histories(Stack[] stackForSS, int ss, int pc, int to, int bonus) {
            for (int i : new int[] {1, 2, 4}) {
                if ((((stackForSS[ss - i].currentMove >>> 6) & 0x3F) != (stackForSS[ss - i].currentMove & 0x3F))) {
                    stackForSS[ss - i].contHistory.update(pc, to, bonus);
                }
            }
        }


        public static void update_stats(Position pos, Stack[] stackForSS, int ss, int move, int[] quiets, int quietsCnt, int bonus) {
            if (stackForSS[ss].killers[0] != move) {
                stackForSS[ss].killers[1] = stackForSS[ss].killers[0];
                stackForSS[ss].killers[0] = move;
            }

            int c = pos.sideToMove;
            thread.Threadd thisThread = pos.thisThread;
            thisThread.mainHistory.update(c, move, bonus);
            update_continuation_histories(stackForSS, ss, pos.board[((move >>> 6) & 0x3F)], (move & 0x3F), bonus);

            if ((((stackForSS[ss - 1].currentMove >>> 6) & 0x3F) != (stackForSS[ss - 1].currentMove & 0x3F))) {
                int prevSq = (stackForSS[ss - 1].currentMove & 0x3F);
                thisThread.counterMoves.array[pos.board[prevSq]][prevSq] = move;
            }

            for (int i = 0; i < quietsCnt; ++i) {
                thisThread.mainHistory.update(c, quiets[i], -bonus);
                update_continuation_histories(stackForSS, ss, pos.board[((quiets[i] >>> 6) & 0x3F)], (quiets[i] & 0x3F), -bonus);
            }
        }


        public static void update_capture_stats(Position pos, int move, int[] captures, int captureCnt, int bonus) {
            Movepick.CapturePieceToHistory captureHistory =  pos.thisThread.captureHistory;
            int moved_piece = pos.board[((move >>> 6) & 0x3F)];
            int captured = (pos.board[(move & 0x3F)] & 7);
            captureHistory.update(moved_piece, (move & 0x3F), captured, bonus);

            for (int i = 0; i < captureCnt; ++i) {
                moved_piece = pos.board[((captures[i] >>> 6) & 0x3F)];
                captured = (pos.board[(captures[i] & 0x3F)] & 7);
                captureHistory.update(moved_piece, (captures[i] & 0x3F), captured, -bonus);
            }
        }


        public static boolean pv_is_draw(Position pos) {
            Position.StateInfo[] st = new Position.StateInfo[Types.MAX_PLY];
            for (int i = 0; i < st.length; i++) {
                st[i] = new Position.StateInfo();
            }
            List<Integer> pv = pos.thisThread.rootMoves.get(0).pv;

            for (int i = 0; i < pv.size(); ++i) {
                pos.do_move(pv.get(i), st[i]);
            }

            boolean isDraw = pos.is_draw(pv.size());

            for (int i = pv.size(); i > 0; --i) {
                pos.undo_move(pv.get(i - 1));
            }

            return isDraw;
        }


        public static long perft(boolean Root, Position pos, int depth) {
            Position.StateInfo st = new Position.StateInfo();
            long cnt = 0;
            long nodes = 0;
            final boolean leaf = (depth == 2 * Types.Depth.ONE_PLY);

            for (Movegen.ExtMove m : new Movegen.MoveList(Movegen.GenType.LEGAL, pos).moveList) {
                if (Root && depth <= Types.Depth.ONE_PLY) {
                    cnt = 1;
                    nodes++;
                } else {
                    pos.do_move(m.move, st);
                    cnt = leaf ? new Movegen.MoveList(Movegen.GenType.LEGAL, pos).size() : perft(false, pos, depth - Types.Depth.ONE_PLY);
                    nodes += cnt;
                    pos.undo_move(m.move);
                }
                if (Root) {
//                    System.out.println(UCI.move(m.move, pos.chess960) + ": " + cnt);
                }
            }
            return nodes;
        }


        public static void init() {
            for (int imp = 0; imp <= 1; ++imp) {
                for (int d = 1; d < 64; ++d) {
                    for (int mc = 1; mc < 64; ++mc) {
                        double r = Math.log(d) * Math.log(mc) / 1.95;

                        Reductions[NodeType.NonPV][imp][d][mc] = (int) Math.round(r);
                        Reductions[NodeType.PV][imp][d][mc] = Math.max(Reductions[NodeType.NonPV][imp][d][mc] - 1, 0);

                        if (imp == 0 && Reductions[NodeType.NonPV][imp][d][mc] >= 2) {
                            Reductions[NodeType.NonPV][imp][d][mc]++;
                        }
                    }
                }
            }

            for (int d = 0; d < 16; ++d) {
                FutilityMoveCounts[0][d] = (int) (2.4 + 0.74 * Math.pow(d, 1.78));
                FutilityMoveCounts[1][d] = (int) (5.0 + 1.00 * Math.pow(d, 2.00));
            }
        }


        public static void clear() {
            Timeman.Time.availableNodes = 0;
            tt.TT.clear();
            thread.Threads.clear();
        }
    }



    public static class Tablebases {

        public static int Cardinality = 0;
        public static boolean RootInTB = false;
        public static boolean UseRule50 = true;
        public static int ProbeDepth = 0;
        public static int Score;

        public static int MaxCardinality = 0;

        public static class ProbeState {
            public static final int FAIL				= 0;
            public static final int OK					= 1;
            public static final int CHANGE_STM			= -1;
            public static final int ZEROING_BEST_MOVE	= 2;
        }

        public static void init(String paths) {

        }

        public static void filter_root_moves(Position pos, Search.RootMoves rootMoves) {}

    }


    public static class thread {

        public static ThreadPool Threads = new ThreadPool();

        public static class Threadd {
            public int idx;
            public boolean searching = true;

            public Pawns.Table pawnsTable = new Pawns.Table();
            public Material.Table materialTable = new Material.Table();
            public endgame.Endgames endgames = new endgame.Endgames();
            public int PVIdx;
            public int selDepth, nmp_ply, nmp_odd;
            public long nodes, tbHits;

            public Position rootPos = new Position();
            public Search.RootMoves rootMoves = new Search.RootMoves();
            public int rootDepth, completedDepth;
            public Movepick.CounterMoveHistory counterMoves = new Movepick.CounterMoveHistory();
            public Movepick.ButterflyHistory mainHistory = new Movepick.ButterflyHistory();
            public Movepick.CapturePieceToHistory captureHistory = new Movepick.CapturePieceToHistory();
            public Movepick.ContinuationHistory contHistory = new Movepick.ContinuationHistory();

            public Threadd(int n) {
                idx = n;
            }

            public int search() {
                Search.Stack[] stack = new Search.Stack[Types.MAX_PLY + 7];
                for (int i = 0; i < stack.length; i++) {
                    stack[i] = new Search.Stack();
                }
                int ss = 4;
                int bestValue, alpha, beta, delta;
                int lastBestMove = Types.Move.MOVE_NONE;
                int lastBestMoveDepth = Types.Depth.DEPTH_ZERO;
                MainThread mainThread = (this == Threads.main() ? Threads.main() : null);
                double timeReduction = 1.0;

                for (int i = 4; i > 0; i--) {
                    stack[ss-i].contHistory = contHistory.array[Types.Piece.NO_PIECE][0];
                }

                bestValue = delta = alpha = -Types.Value.VALUE_INFINITE;
                beta = Types.Value.VALUE_INFINITE;

                if (mainThread != null) {
                    mainThread.failedLow = false;
                    mainThread.bestMoveChanges = 0;
                }

                int multiPV = UCI.Options.get("MultiPV").convertToInteger();
                Search.Skill skill = new Search.Skill(UCI.Options.get("Skill Level").convertToInteger());

                if (skill.enabled()) {
                    multiPV = Math.max(multiPV, 4);
                }

                multiPV = Math.min(multiPV, rootMoves.size());

                while ((rootDepth += Types.Depth.ONE_PLY) < Types.Depth.DEPTH_MAX &&
                        !Threads.stop &&
                        !(Search.Limits.depth != 0 && mainThread != null && rootDepth / Types.Depth.ONE_PLY > Search.Limits.depth)) {

                    if (idx != 0) {
                        int i = (idx - 1) % 20;
                        if (((rootDepth / Types.Depth.ONE_PLY + rootPos.gamePly + Search.skipPhase[i]) / Search.skipSize[i]) % 2 != 0) {
                            continue;
                        }
                    }

                    if (mainThread != null) {
                        mainThread.bestMoveChanges *= 0.505;
                        mainThread.failedLow = false;
                    }

                    for (Search.RootMove rm : rootMoves) {
                        rm.previousScore = rm.score;
                    }

                    for (PVIdx = 0; PVIdx < multiPV && !Threads.stop; ++PVIdx) {
                        selDepth = 0;

                        if (rootDepth >= 5 * Types.Depth.ONE_PLY) {
                            delta = 18;
                            alpha = Math.max(rootMoves.get(PVIdx).previousScore - delta, -Types.Value.VALUE_INFINITE);
                            beta = Math.min(rootMoves.get(PVIdx).previousScore + delta, Types.Value.VALUE_INFINITE);
                        }

                        while (true) {
                            bestValue = Search.search(Search.NodeType.PV, rootPos, stack, ss, alpha, beta, rootDepth, false, false);

                            Collections.sort(rootMoves, Search.RootMove.RootMoveComparator);

                            if (Threads.stop) {
                                break;
                            }

                            if (mainThread != null && multiPV == 1 && (bestValue <= alpha || bestValue >= beta) && Timeman.Time.elapsed() > 3000) {
//                                System.out.println(UCI.pv(rootPos, rootDepth, alpha, beta));
                            }

                            if (bestValue <= alpha) {
                                beta = (alpha + beta) / 2;
                                alpha = Math.max(bestValue - delta, -Types.Value.VALUE_INFINITE);

                                if (mainThread != null) {
                                    mainThread.failedLow = true;
                                    Threads.stopOnPonderhit = false;
                                }
                            } else if (bestValue >= beta) {
                                beta = Math.min(bestValue + delta, Types.Value.VALUE_INFINITE);
                            } else {
                                break;
                            }

                            delta += delta / 4 + 5;
                        }

                        if (mainThread != null && (Threads.stop || PVIdx + 1 == multiPV || Timeman.Time.elapsed() > 3000)) {
//                            System.out.println(UCI.pv(rootPos, rootDepth, alpha, beta));
                        }
                    }

                    if (!Threads.stop) {
                        completedDepth = rootDepth;
                    }

                    if (rootMoves.get(0).pv.get(0) != lastBestMove) {
                        lastBestMove = rootMoves.get(0).pv.get(0);
                        lastBestMoveDepth = rootDepth;
                    }

                    if (Search.Limits.mate != 0 &&
                            bestValue >= Types.Value.VALUE_MATE_IN_MAX_PLY &&
                            Types.Value.VALUE_MATE - bestValue <= 2 * Search.Limits.mate) {
                        Threads.stop = true;
                    }

                    if (mainThread == null) {
                        continue;
                    }

                    if (skill.enabled() && skill.time_to_pick(rootDepth)) {
                        skill.pick_best(multiPV);
                    }

                    if (Search.Limits.use_time_management()) {
                        if (!Threads.stop && !Threads.stopOnPonderhit) {
                            final int[] F = {(mainThread.failedLow ? 1 : 0), (bestValue - mainThread.previousScore)};
                            int improvingFactor = Math.max(229, Math.min(715, 357 + 119 * F[0] - 6 * F[1]));

                            int us = rootPos.sideToMove;
                            boolean thinkHard = bestValue == Types.Value.VALUE_DRAW &&
                                    Search.Limits.time[us] - Timeman.Time.elapsed() > Search.Limits.time[(us ^ Types.Color.BLACK)] &&
                                    Search.pv_is_draw(rootPos);

                            double unstablePvFactor = 1 + mainThread.bestMoveChanges + (thinkHard ? 1 : 0);

                            timeReduction = 1;
                            for (int i : new int[] {3, 4, 5}) {
                                if (lastBestMoveDepth * i < completedDepth && !thinkHard) {
                                    timeReduction *= 1.3;
                                }
                            }

                            unstablePvFactor *= Math.pow(mainThread.previousTimeReduction, 0.51) / timeReduction;

                            if (rootMoves.size() == 1 || Timeman.Time.elapsed() > Timeman.Time.optimum() * unstablePvFactor * improvingFactor / 628) {
                                if (Threads.ponder) {
                                    Threads.stopOnPonderhit = true;
                                } else {
                                    Threads.stop = true;
                                }
                            }
                        }
                    }
                }

                if (mainThread == null) {
                    return Types.Move.MOVE_NONE;
                }

                mainThread.previousTimeReduction = timeReduction;

                if (skill.enabled()) {
                    int swapIndex = 0;
                    int moveToSwap = skill.best != 0 ? skill.best : skill.pick_best(multiPV);
                    for (int i = 1; i < rootMoves.size(); i++) {
                        if (rootMoves.get(i).operatorEquals(moveToSwap)) {
                            swapIndex = i;
                            break;
                        }
                    }
                    Collections.swap(rootMoves, 0, swapIndex);
                }
                return Types.Move.MOVE_NONE;
            }


            public static boolean clearForTheFirstTime = true;
            public void clear() {
                if (!clearForTheFirstTime) {
                    counterMoves.fill(Types.Move.MOVE_NONE);
                    mainHistory.fill(0);
                    captureHistory.fill(0);

                    for (Movepick.PieceToHistory[] to : contHistory.array) {
                        for (Movepick.PieceToHistory h : to) {
                            h.fillWithZeros();
                        }
                    }
                }
                contHistory.array[Types.Piece.NO_PIECE][0].fill(Search.CounterMovePruneThreshold - 1);
            }


            public int start_searching() {
                searching = true;
                return search();
            }
        }


        public static class MainThread extends Threadd {
            public boolean failedLow;
            public double bestMoveChanges, previousTimeReduction;
            public int previousScore;
            public int callsCnt;

            public MainThread(int n) {
                super(n);
            }

            @Override
            public int search() {
                if (Search.Limits.perft != 0) {
                    nodes = Search.perft(true, rootPos, Search.Limits.perft * Types.Depth.ONE_PLY);
//                    System.out.println("\nNodes searched: " + nodes + "\n");
                    return Types.Move.MOVE_NONE;
                }

                int us = rootPos.sideToMove;
                Timeman.Time.init(Search.Limits, us, rootPos.gamePly);
                tt.TT.new_search();

                int contempt = UCI.Options.get("Contempt").convertToInteger() * Types.Value.PawnValueEg / 100;

                Eval.Contempt = (us == Types.Color.WHITE ? (((contempt / 2) << 16) | (contempt & 0xffff)) : -(((contempt / 2) << 16) | (contempt & 0xffff)));

                if (rootMoves.isEmpty()) {
                    rootMoves.add(new Search.RootMove(Types.Move.MOVE_NONE));
//                    System.out.println("info depth 0 score " + UCI.value(rootPos.st.checkersBB != 0 ? -Types.Value.VALUE_MATE : Types.Value.VALUE_DRAW));
                } else {
                    super.search();
                }

                Threads.stopOnPonderhit = true;

                while (!Threads.stop && (Threads.ponder || Search.Limits.infinite != 0)) {}

                Threads.stop = true;

                if (Search.Limits.npmsec != 0) {
                    Timeman.Time.availableNodes += Search.Limits.inc[us] - Threads.nodes_searched();
                }

                Threadd bestThread = this;
                if (UCI.Options.get("MultiPV").convertToInteger() == 1 &&
                        Search.Limits.depth == 0 &&
                        !(new Search.Skill(UCI.Options.get("Skill Level").convertToInteger())).enabled() &&
                        rootMoves.get(0).pv.get(0) != Types.Move.MOVE_NONE) {

                    for (Threadd th : Threads) {
                        int depthDiff = th.completedDepth - bestThread.completedDepth;
                        int scoreDiff = th.rootMoves.get(0).score - bestThread.rootMoves.get(0).score;

                        if (scoreDiff > 0 && (depthDiff >= 0 || th.rootMoves.get(0).score >= Types.Value.VALUE_MATE_IN_MAX_PLY)) {
                            bestThread = th;
                        }
                    }
                }

                previousScore = bestThread.rootMoves.get(0).score;

                if (bestThread != this) {
//                    System.out.println(UCI.pv(bestThread.rootPos, bestThread.completedDepth, -Types.Value.VALUE_INFINITE, Types.Value.VALUE_INFINITE));
                }

//                controller.playTheBestMove(bestThread.rootMoves.get(0).pv.get(0));
                return bestThread.rootMoves.get(0).pv.get(0);
//                System.out.println("bestmove " + UCI.move(bestThread.rootMoves.get(0).pv.get(0), rootPos.chess960));

//                if (bestThread.rootMoves.get(0).pv.size() > 1 || bestThread.rootMoves.get(0).extract_ponder_from_tt(rootPos)) {
//                    System.out.println("ponder " + UCI.move(bestThread.rootMoves.get(0).pv.get(1), rootPos.chess960));
//                }
            }


            public static long lastInfoTime = System.currentTimeMillis();;
            public void check_time() {
                if (--callsCnt > 0) {
                    return;
                }

                callsCnt = Search.Limits.nodes != 0 ? Math.min(4096, ((int) (Search.Limits.nodes / 1024))) : 4096;

                int elapsed = Timeman.Time.elapsed();
                long tick = Search.Limits.startTime + elapsed;

                if (tick - lastInfoTime >= 1000) {
                    lastInfoTime = tick;
                }

                if (Threads.ponder) {
                    return;
                }

                if ((Search.Limits.use_time_management() && elapsed > Timeman.Time.maximum() - 10) ||
                        (Search.Limits.movetime != 0 && elapsed >= Search.Limits.movetime) ||
                        (Search.Limits.nodes != 0 && Threads.nodes_searched() >= Search.Limits.nodes)) {
                    Threads.stop = true;
                }
            }
        }


        public static class ThreadPool extends ArrayList<MainThread> {

            private static final long serialVersionUID = -7673929655024240259L;

            public boolean stop, ponder, stopOnPonderhit;

            private Position.StateListPtr setupStates = new Position.StateListPtr();

            public void start_thinking(Position pos, Position.StateListPtr states, Search.LimitsType limits) {
                start_thinking(pos, states, limits, false);
            }

            public int start_thinking(Position pos, Position.StateListPtr states, Search.LimitsType limits, boolean ponderMode) {
                stopOnPonderhit = stop = false;
                ponder = ponderMode;
                Search.Limits = limits;
                Search.RootMoves rootMoves = new Search.RootMoves();

                Movegen.MoveList moveListObject = new Movegen.MoveList(Movegen.GenType.LEGAL, pos);
                for (int i = 0; i < moveListObject.size(); i++) {
                    Movegen.ExtMove m = moveListObject.moveList[i];
                    if (limits.searchmoves.isEmpty() || limits.searchmoves.contains(m.move)) {
                        rootMoves.add(new Search.RootMove(m.move));
                    }
                }

                if (!rootMoves.isEmpty()) {
                    Tablebases.filter_root_moves(pos, rootMoves);
                }

                if (states != null) {
                    setupStates = states;
                }

                Position.StateInfo tmp = setupStates.getLast();

                for (Threadd th : this) {
                    th.nodes = 0;
                    th.tbHits = 0;
                    th.nmp_ply = 0;
                    th.nmp_odd = 0;
                    th.rootDepth = Types.Depth.DEPTH_ZERO;
                    th.completedDepth = Types.Depth.DEPTH_ZERO;
                    th.rootMoves = rootMoves;
//                    th.rootPos.set(pos.fen(), pos.chess960, setupStates.getLast(), th);
                    th.rootPos = pos;
                }

                setupStates.removeLast();
                setupStates.add(tmp);

                return main().start_searching();
            }


            public void set() {
                add(new MainThread(0));
                clear();
            }


            public void clear() {
                main().clear();

                main().callsCnt = 0;
                main().previousScore = Types.Value.VALUE_INFINITE;
                main().previousTimeReduction = 1;
            }


            public MainThread main() {
                return get(0);
            }

            public long nodes_searched() {
                long sum = 0;
                for (Threadd th : this) {
                    sum += th.nodes;
                }
                return sum;
            }

            public long tb_hits() {
                long sum = 0;
                for (Threadd th : this) {
                    sum += th.tbHits;
                }
                return sum;
            }
        }
    }



    public static class Timeman {

        public static TimeManagement Time = new TimeManagement();

        public static class TimeManagement {
            private long startTime;
            private int optimumTime;
            private int maximumTime;

            public long availableNodes = 0;

            public void init(Search.LimitsType limits, int us, int ply) {
                int minThinkingTime = UCI.Options.get("Minimum Thinking Time").convertToInteger();
                int moveOverhead    = UCI.Options.get("Move Overhead").convertToInteger();
                int slowMover       = UCI.Options.get("Slow Mover").convertToInteger();
                int npmsec          = UCI.Options.get("nodestime").convertToInteger();

                if (npmsec != 0) {
                    if (availableNodes == 0) {
                        availableNodes = npmsec * limits.time[us];
                    }

                    limits.time[us] = (int) availableNodes;
                    limits.inc[us] *= npmsec;
                    limits.npmsec = npmsec;
                }

                startTime = limits.startTime;
                optimumTime = maximumTime = Math.max(limits.time[us], minThinkingTime);

                final int MaxMTG = limits.movestogo != 0 ? Math.min(limits.movestogo, MoveHorizon) : MoveHorizon;

                for (int hypMTG = 1; hypMTG <= MaxMTG; ++hypMTG) {
                    int hypMyTime = limits.time[us] + limits.inc[us] * (hypMTG - 1) - moveOverhead * (2 + Math.min(hypMTG, 40));

                    hypMyTime = Math.max(hypMyTime, 0);

                    int t1 = minThinkingTime + remaining(TimeType.OptimumTime, hypMyTime, hypMTG, ply, slowMover);
                    int t2 = minThinkingTime + remaining(TimeType.MaxTime, hypMyTime, hypMTG, ply, slowMover);

                    optimumTime = Math.min(t1, optimumTime);
                    maximumTime = Math.min(t2, maximumTime);
                }

                if (UCI.Options.get("Ponder").convertToInteger() != 0) {
                    optimumTime += optimumTime / 4;
                }
            }

            public int optimum() {
                return optimumTime;
            }

            public int maximum() {
                return maximumTime;
            }

            public int elapsed() {
                return (int) (Search.Limits.npmsec != 0 ? thread.Threads.nodes_searched() : (System.currentTimeMillis() - startTime));
            }
        }

        public static class TimeType {
            public static final int OptimumTime = 0;
            public static final int MaxTime = 1;
        }

        public static final int MoveHorizon = 50;
        public static final double MaxRatio = 7.09;
        public static final double StealRatio = 0.35;

        public static final double XScale = 7.64;
        public static final double XShift = 58.4;
        public static final double Skew = 0.183;
        public static double move_importance(int ply) {
            return Math.pow((1 + Math.exp((ply - XShift) / XScale)), -Skew) + Double.MIN_NORMAL;
        }

        public static int remaining(int T, int myTime, int movesToGo, int ply, int slowMover) {
            final double TMaxRatio   = (T == TimeType.OptimumTime ? 1 : MaxRatio);
            final double TStealRatio = (T == TimeType.OptimumTime ? 0 : StealRatio);

            double moveImportance = (move_importance(ply) * slowMover) / 100;
            double otherMovesImportance = 0;

            for (int i = 1; i < movesToGo; ++i) {
                otherMovesImportance += move_importance(ply + 2 * i);
            }

            double ratio1 = (TMaxRatio * moveImportance) / (TMaxRatio * moveImportance + otherMovesImportance);
            double ratio2 = (moveImportance + TStealRatio * otherMovesImportance) / (moveImportance + otherMovesImportance);

            return (int) (myTime * Math.min(ratio1, ratio2));
        }
    }



    public static class tt {

        public static TranspositionTable TT = new TranspositionTable();


        public static class TranspositionTable {
            public static final int CacheLineSize = 64;
            public static final int ClusterSize = 3;

            private int clusterCount;
            private Cluster[] table;
            private int generation8;

            public static class Cluster {
                public TTEntry[] entry = new TTEntry[ClusterSize];
                public char[] padding = new char[2];
                public Cluster() {
                    for (int i = 0; i < entry.length; i++) {
                        entry[i] = new TTEntry();
                    }
                }
            }

            public void resize(int mbSize) {
                int newClusterCount = 2 * 1024 * 1024 / 32;

                if (newClusterCount == clusterCount) {
                    return;
                }

                clusterCount = newClusterCount;
//                table = new Cluster[clusterCount];
//                for (int i = 0; i < table.length; i++) {
//                    table[i] = new Cluster();
//                }
            }

            public void clear() {
                table = new Cluster[clusterCount];
                for (int i = 0; i < table.length; i++) {
                    table[i] = new Cluster();
                }
            }

            public void new_search() {
                generation8 += 4;
                generation8 = generation8 & 0xff;
            }

            public int generation() {
                return generation8;
            }

            public TTEntry probe(long key, boolean[] found) {
                TTEntry[] tte = first_entry(key);
                final int key16 = (int) (key >>> 48);

                for (int i = 0; i < ClusterSize; ++i) {
                    if (tte[i].key16 == 0 || tte[i].key16 == key16) {
                        if ((tte[i].genBound8 & 0xFC) != generation8 && tte[i].key16 != 0) {
                            tte[i].genBound8 = 0xff & (generation8 | tte[i].bound());
                        }

                        found[0] = tte[i].key16 != 0;
                        return  tte[i];
                    }
                }

                TTEntry replace = tte[0];
                for (int i = 1; i < ClusterSize; ++i) {
                    if (replace.depth8 - ((259 + generation8 - replace.genBound8) & 0xFC) * 2 > tte[i].depth8 - ((259 + generation8 -   tte[i].genBound8) & 0xFC) * 2) {
                        replace = tte[i];
                    }
                }
                found[0] = false;
                return replace;
            }

            public int hashfull() {
                int cnt = 0;
                for (int i = 0; i < 1000 / ClusterSize; i++) {
                    final TTEntry[] tte = table[i].entry;
                    for (int j = 0; j < ClusterSize; j++) {
                        if ((tte[j].genBound8 & 0xFC) == generation8) {
                            cnt++;
                        }
                    }
                }
                return cnt;
            }

            public TTEntry[] first_entry(long key) {
                return table[(int) (((key & 0xffffffffL) * clusterCount) >>> 32)].entry;
            }
        }


        public static class TTEntry {
            private int key16;
            private int move16;
            private int value16;
            private int eval16;
            private int genBound8;
            private int depth8;

            public int move() {
                return move16;
            }

            public int value() {
                return value16;
            }

            public int eval() {
                return eval16;
            }

            public int depth() {
                return depth8 * Types.Depth.ONE_PLY;
            }

            public int bound() {
                return genBound8 & 0x3;
            }

            public void save(long k, int v, int b, int d, int m, int ev, int g) {
                if (m != 0 || (k >>> 48) != key16) {
                    move16 = m;
                }

                if ((k >>> 48) != key16 || d / Types.Depth.ONE_PLY > depth8 - 4 || b == Types.Bound.BOUND_EXACT) {
                    key16     = (int) (k >>> 48);
                    value16   = v;
                    eval16    = ev;
                    genBound8 = 0xff & (g | b);
                    depth8    = d / Types.Depth.ONE_PLY;
                }
            }
        }
    }


    public static class Types {

        static final boolean Is64Bit = false;

        static final int MAX_MOVES = 256;
        static final int MAX_PLY = 128;


        public static class Move {
            public static final int MOVE_NONE = 0;
            static final int MOVE_NULL = 65;
        }

        public static class MoveType {
            public static final int NORMAL = 0;
            public static final int PROMOTION = 1 << 14;
            public static final int ENPASSANT = 2 << 14;
            public static final int CASTLING = 3 << 14;
        }

        public static class Color {
            public static final int WHITE = 0;
            public static final int BLACK = 1;
            static final int COLOR_NB = 2;
        }

        public static class CastlingSide {
            static final int KING_SIDE = 0;
            static final int QUEEN_SIDE = 1;
            public static final int CASTLING_SIDE_NB = 2;
        }

        static class CastlingRight {
            static final int NO_CASTLING = 0;
            static final int WHITE_OO = 1;
            static final int WHITE_OOO = WHITE_OO << 1;
            static final int BLACK_OO  = WHITE_OO << 2;
            static final int BLACK_OOO = WHITE_OO << 3;
            static final int ANY_CASTLING = WHITE_OO | WHITE_OOO | BLACK_OO | BLACK_OOO;
            static final int CASTLING_RIGHT_NB = 16;
        }

        static class MakeCastling {
            static int getRight(int C, int S) {
                return (C == Color.WHITE) ? (S == CastlingSide.QUEEN_SIDE) ? CastlingRight.WHITE_OOO : CastlingRight.WHITE_OO
                        : (S == CastlingSide.QUEEN_SIDE) ? CastlingRight.BLACK_OOO : CastlingRight.BLACK_OO;
            }
        }

        public static class Phase {
            public static final int PHASE_ENDGAME = 0;
            static final int PHASE_MIDGAME = 128;
            static final int MG = 0;
            public static final int EG  = 1;
            static final int PHASE_NB = 2;
        }

        static class ScaleFactor {
            static final int SCALE_FACTOR_DRAW = 0;
            static final int SCALE_FACTOR_ONEPAWN = 48;
            static final int SCALE_FACTOR_NORMAL = 64;
            static final int SCALE_FACTOR_MAX  = 128;
            static final int SCALE_FACTOR_NONE = 255;
        }

        static class Bound {
            static final int BOUND_NONE = 0;
            static final int BOUND_UPPER = 1;
            static final int BOUND_LOWER = 2;
            static final int BOUND_EXACT = BOUND_UPPER | BOUND_LOWER;
        }

        public static class Value {
            static final int VALUE_ZERO = 0;
            static final int VALUE_DRAW = 0;
            static final int VALUE_KNOWN_WIN = 10000;
            static final int VALUE_MATE = 32000;
            static final int VALUE_INFINITE = 32001;
            static final int VALUE_NONE = 32002;

            static final int VALUE_MATE_IN_MAX_PLY = VALUE_MATE - 2 * MAX_PLY;
            static final int VALUE_MATED_IN_MAX_PLY = -VALUE_MATE + 2 * MAX_PLY;

            static final int PawnValueMg = 171;
            static final int PawnValueEg = 240;
            static final int KnightValueMg = 764;
            static final int KnightValueEg = 848;
            static final int BishopValueMg = 826;
            static final int BishopValueEg = 891;
            static final int RookValueMg = 1282;
            static final int RookValueEg = 1373;
            static final int QueenValueMg = 2526;
            static final int QueenValueEg = 2646;
            static final int MidgameLimit = 15258;
            static final int EndgameLimit = 3915;
        }

        public static class PieceType {
            public static final int NO_PIECE_TYPE = 0;
            public static final int PAWN = 1;
            public static final int KNIGHT = 2;
            public static final int BISHOP = 3;
            public static final int ROOK = 4;
            public static final int QUEEN = 5;
            public static final int KING = 6;
            public static final int ALL_PIECES = 0;
            public static final int QUEEN_DIAGONAL = 7;
            public static final int PIECE_TYPE_NB = 8;
        }

        public static class Piece {
            public static final int NO_PIECE = 0;

            public static final int W_PAWN = 1;
            public static final int W_KNIGHT = 2;
            public static final int W_BISHOP = 3;
            public static final int W_ROOK = 4;
            public static final int W_QUEEN = 5;
            public static final int W_KING = 6;

            public static final int B_PAWN = 9;
            public static final int B_KNIGHT = 10;
            public static final int B_BISHOP = 11;
            public static final int B_ROOK = 12;
            public static final int B_QUEEN = 13;
            public static final int B_KING = 14;

            static final int PIECE_NB = 16;
        }

        static class Depth {
            static final int ONE_PLY = 1;

            static final int DEPTH_ZERO = 0;
            static final int DEPTH_QS_CHECKS = 0;
            static final int DEPTH_QS_NO_CHECKS = -1 * ONE_PLY;
            static final int DEPTH_QS_RECAPTURES = -5 * ONE_PLY;

            static final int DEPTH_NONE = -6 * ONE_PLY;
            static final int DEPTH_MAX = MAX_PLY * ONE_PLY;
        }

        public static class Square {
            private static int Counter = 0;

            public static final int SQ_A1 = Counter++;
            static final int SQ_B1 = Counter++;
            public static final int SQ_C1 = Counter++;
            public static final int SQ_D1 = Counter++;
            public static final int SQ_E1 = Counter++;
            public static final int SQ_F1 = Counter++;
            public static final int SQ_G1 = Counter++;
            public static final int SQ_H1 = Counter++;
            public static final int SQ_A2 = Counter++, SQ_B2 = Counter++, SQ_C2 = Counter++, SQ_D2 = Counter++, SQ_E2 = Counter++, SQ_F2 = Counter++, SQ_G2 = Counter++, SQ_H2 = Counter++;
            public static final int SQ_A3 = Counter++, SQ_B3 = Counter++, SQ_C3 = Counter++, SQ_D3 = Counter++, SQ_E3 = Counter++, SQ_F3 = Counter++, SQ_G3 = Counter++, SQ_H3 = Counter++;
            public static final int SQ_A4 = Counter++, SQ_B4 = Counter++, SQ_C4 = Counter++, SQ_D4 = Counter++, SQ_E4 = Counter++, SQ_F4 = Counter++, SQ_G4 = Counter++, SQ_H4 = Counter++;
            static final int SQ_A5 = Counter++, SQ_B5 = Counter++, SQ_C5 = Counter++, SQ_D5 = Counter++, SQ_E5 = Counter++, SQ_F5 = Counter++, SQ_G5 = Counter++, SQ_H5 = Counter++;
            public static final int SQ_A6 = Counter++, SQ_B6 = Counter++, SQ_C6 = Counter++, SQ_D6 = Counter++, SQ_E6 = Counter++, SQ_F6 = Counter++, SQ_G6 = Counter++, SQ_H6 = Counter++;
            static final int SQ_A7 = Counter++;
            public static final int SQ_B7 = Counter++;
            static final int SQ_C7 = Counter++;
            static final int SQ_D7 = Counter++;
            static final int SQ_E7 = Counter++;
            static final int SQ_F7 = Counter++;
            static final int SQ_G7 = Counter++;
            static final int SQ_H7 = Counter++;
            public static final int SQ_A8 = Counter++;
            static final int SQ_B8 = Counter++;
            public static final int SQ_C8 = Counter++;
            public static final int SQ_D8 = Counter++;
            public static final int SQ_E8 = Counter++;
            public static final int SQ_F8 = Counter++;
            public static final int SQ_G8 = Counter++;
            public static final int SQ_H8 = Counter++;
            static final int SQ_NONE = Counter++;

            static final int SQUARE_NB = 64;
        }

        public static class Direction {
            static final int NORTH = 8;
            static final int EAST = 1;
            static final int SOUTH = -NORTH;
            static final int WEST = -EAST;
            static final int NORTH_EAST = NORTH + EAST;
            static final int SOUTH_EAST = SOUTH + EAST;
            static final int SOUTH_WEST = SOUTH + WEST;
            static final int NORTH_WEST = NORTH + WEST;
        }

        public static class File {
            private static int Counter = 0;

            static final int FILE_A = Counter++;
            static final int FILE_B = Counter++;
            static final int FILE_C = Counter++;
            static final int FILE_D = Counter++;
            static final int FILE_E = Counter++;
            public static final int FILE_F = Counter++;
            static final int FILE_G = Counter++;
            static final int FILE_H = Counter++;

            static final int FILE_NB = Counter++;
        }

        public static class Rank {
            private static int Counter = 0;

            static final int RANK_1 = Counter++;
            static final int RANK_2 = Counter++;
            static final int RANK_3 = Counter++;
            static final int RANK_4 = Counter++;
            static final int RANK_5 = Counter++;
            static final int RANK_6 = Counter++;
            static final int RANK_7 = Counter++;
            static final int RANK_8 = Counter++;

            static final int RANK_NB = Counter++;
        }

        public static class Score {
            static int SCORE_ZERO = 0;
        }

        static int operatorORForColorCastlingSide(int c, int s) {
            return (CastlingRight.WHITE_OO << (((s == CastlingSide.QUEEN_SIDE) ? 1 : 0) + 2 * c));
        }

        public static int file_of(int s) {
            return (s & 7);
        }

        public static int rank_of(int s) {
            return (s >>> 3);
        }

        public static int make_square(int f, int r) {
            return ((r << 3) + f);
        }

        public static int color_of(int pc) {
            return (pc >>> 3);
        }

        public static int from_sq(int m) {
            return ((m >>> 6) & 0x3F);
        }

        public static int to_sq(int m) {
            return (m & 0x3F);
        }

        public static int from_to(int m) {
            return (m & 0xFFF);
        }

        public static int moveType(int m) {
            return (m & (3 << 14));
        }

        public static int promotionType(int m) {
            return (((m >>> 12) & 3) + PieceType.KNIGHT);
        }

        public static int make_move(int from, int to) {
            return ((from << 6) + to);
        }

        public static int make(int T, int from, int to, int pt) {
            return (T + ((pt - PieceType.KNIGHT) << 12) + (from << 6) + to);
        }

        public static int make(int T, int from, int to) {
            return (T + (from << 6) + to);
        }
    }


    public static class UCI {

        static final String StartFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

        public static OptionsMap Options = new OptionsMap();

        static class OptionsMap extends HashMap<String, Option> {
            private static final long serialVersionUID = 10001L;
        }

        public static class Option {
            String defaultValue = "", currentValue = "", type = "";
            public int min, max;
            int idx;
            OnChange on_change;


            public interface OnChange {
                void method(Option o);
            }

            static OnChange on_clear_hash = new OnChange() {
                @Override
                public void method(Option o) {
                    Search.clear();
                }
            };

            static OnChange on_hash_size = new OnChange() {
                @Override
                public void method(Option o) {
                    tt.TT.resize(o.convertToInteger());
                }
            };

            static OnChange on_logger = new OnChange() {
                @Override
                public void method(Option o) {}
            };

            static OnChange on_threads = new OnChange() {
                @Override
                public void method(Option o) {}
            };

            static OnChange on_tb_path = new OnChange() {
                @Override
                public void method(Option o) {
                    Tablebases.init(o.convertToString());
                }
            };



            Option(OnChange f) {
                type = "button"; min = 0; max = 0; on_change = f;
            }

            Option(boolean v, OnChange f) {
                type = "check"; min = 0; max = 0; on_change = f;
                if (v) {
                    defaultValue = "true"; currentValue = "true";
                } else {
                    defaultValue = "false"; currentValue = "false";
                }
            }

            Option(String v, OnChange f) {
                type = "string"; min = 0; max = 0; on_change = f;
                defaultValue = v; currentValue = v;
            }

            Option(int v, int minv, int maxv, OnChange f) {
                type = "spin"; min = minv; max = maxv; on_change = f;
                defaultValue = Integer.toString(v); currentValue = Integer.toString(v);
            }


            static int insert_order_in_operatorInsertion = 0;
            static void operatorInsertion(OptionsMap optionsMap, String key, Option option) {
                optionsMap.put(key, option);
                option.idx = insert_order_in_operatorInsertion++;
            }



            int convertToInteger() {
                if (type.equals("spin")) {
                    return Integer.parseInt(currentValue);
                } else {
                    return (currentValue.equals("true") ? 1 : 0);
                }
            }

            String convertToString() {
                return currentValue;
            }
        }



        static void init(OptionsMap o) {
            final int MaxHashMB = Types.Is64Bit ? 131072 : 2048;

            Option.operatorInsertion(o, "Debug Log File", new Option("", Option.on_logger));
            Option.operatorInsertion(o, "Contempt", new Option(20, -100, 100, null));
            Option.operatorInsertion(o, "Threads", new Option(1, 1, 512, Option.on_threads));
            Option.operatorInsertion(o, "Hash", new Option(16, 1, MaxHashMB, Option.on_hash_size));
            Option.operatorInsertion(o, "Clear Hash", new Option(Option.on_clear_hash));
            Option.operatorInsertion(o, "Ponder", new Option(false, null));
            Option.operatorInsertion(o, "MultiPV", new Option(1, 1, 500, null));
            Option.operatorInsertion(o, "Skill Level", new Option(20, 0, 20, null));
            Option.operatorInsertion(o, "Move Overhead", new Option(30, 0, 5000, null));
            Option.operatorInsertion(o, "Minimum Thinking Time", new Option(20, 0, 5000, null));
            Option.operatorInsertion(o, "Slow Mover", new Option(89, 10, 1000, null));
            Option.operatorInsertion(o, "nodestime", new Option(0, 0, 10000, null));
            Option.operatorInsertion(o, "UCI_Chess960", new Option(false, null));
            Option.operatorInsertion(o, "SyzygyPath", new Option("<empty>", Option.on_tb_path));
            Option.operatorInsertion(o, "SyzygyProbeDepth", new Option(1, 1, 100, null));
            Option.operatorInsertion(o, "Syzygy50MoveRule", new Option(true, null));
            Option.operatorInsertion(o, "SyzygyProbeLimit", new Option(6, 0, 6, null));
        }


//        public static void loop(Controller controller) {
//            Position pos = new Position();
//            Position.StateListPtr[] states = {new Position.StateListPtr()};
//            states[0].add(new Position.StateInfo());
////            thread.Threadd uiThread = new thread.Threadd(0);
//
//            pos.set(StartFEN, false, states[0].getLast(), thread.Threads.main());
//            controller.getNewPosition(pos);
//            Scanner lineScanner = new Scanner("depth 20");
//            go(pos, lineScanner, states[0]);
//
//            Scanner scanner = new Scanner(System.in);
//            String token = "";
//            String cmd = "";
//
//            do {
//                cmd = scanner.nextLine();
//                Scanner lineScanner = new Scanner(cmd);
//                token = lineScanner.next();
//
//                if (token.equals("quit") || token.equals("stop") || (token.equals("ponderhit") && thread.Threads.stopOnPonderhit)) {
//                    thread.Threads.stop = true;
//                } else if (token.equals("ponderhit")) {
//                    thread.Threads.ponder = false;
//                } else if (token.equals("uci")) {
//                    System.out.println("id name " + Misc.engine_info() + "\n" + operatorInsertion(Options) + "\nuciok");
//                } else if (token.equals("setoption")) {
//                    setoption(lineScanner);
//                } else if (token.equals("go")) {
//                    go(pos, lineScanner, states[0]);
//                } else if (token.equals("position")) {
//                    position(pos, lineScanner, states);
//                } else if (token.equals("ucinewgame")) {
//                    Search.clear();
//                } else if (token.equals("isready")) {
//                    System.out.println("readyok");
//                } else if (token.equals("flip")) {
//                    pos.flip();
//                } else if (token.equals("bench")) {
//                    bench(pos, lineScanner, states);
//                } else if (token.equals("d")) {
//                    System.out.println(Position.operatorInsertion(pos));
//                } else if (token.equals("eval")) {
//                    System.out.println(Eval.trace(pos));
//                } else {
//                    System.out.println("Unknown command: " + cmd);
//                }
//
//                lineScanner.close();
//            } while (token != "quit");
//
//            scanner.close();
//        }


        public static int go(Position pos, Scanner is, Position.StateListPtr states) {
            Search.LimitsType limits = new Search.LimitsType();
            String token;
            boolean ponderMode = false;

            limits.startTime = System.currentTimeMillis();

            while (is.hasNext() && (token = is.next()) != null) {
                if (token.equals("searchmoves")) {
                    while (is.hasNext() && (token = is.next()) != null) {
                        limits.searchmoves.add(to_move(pos, token));
                    }
                } else if (token.equals("wtime")) {
                    limits.time[Types.Color.WHITE] = is.nextInt();
                } else if (token.equals("btime")) {
                    limits.time[Types.Color.BLACK] = is.nextInt();
                } else if (token.equals("winc")) {
                    limits.inc[Types.Color.WHITE] = is.nextInt();
                } else if (token.equals("binc")) {
                    limits.inc[Types.Color.BLACK] = is.nextInt();
                } else if (token.equals("movestogo")) {
                    limits.movestogo = is.nextInt();
                } else if (token.equals("depth")) {
                    limits.depth = is.nextInt();
                } else if (token.equals("nodes")) {
                    limits.nodes = is.nextLong();
                } else if (token.equals("movetime")) {
                    limits.movetime = is.nextInt();
                } else if (token.equals("mate")) {
                    limits.mate = is.nextInt();
                } else if (token.equals("perft")) {
                    limits.perft = is.nextInt();
                } else if (token.equals("infinite")) {
                    limits.infinite = 1;
                } else if (token.equals("ponder")) {
                    ponderMode = true;
                }
            }

            return thread.Threads.start_thinking(pos, states, limits, ponderMode);
        }


        public static String square(int s) {
            return "" + (char) (((int) 'a') + (s & 7)) + (char) (((int) '1') + (s >>> 3));
        }


        public static String pv(Position pos, int depth, int alpha, int beta) {
            StringBuilder ss = new StringBuilder();
            int elapsed = Timeman.Time.elapsed() + 1;
            final Search.RootMoves rootMoves = pos.thisThread.rootMoves;
            int PVIdx = pos.thisThread.PVIdx;
            int multiPV = Math.min(1, rootMoves.size());
            long nodesSearched = thread.Threads.nodes_searched();
            long tbHits = thread.Threads.tb_hits() + (Tablebases.RootInTB ? rootMoves.size() : 0);

            for (int i = 0; i < multiPV; ++i) {
                boolean updated = (i <= PVIdx && rootMoves.get(i).score != -Types.Value.VALUE_INFINITE);

                if (depth == Types.Depth.ONE_PLY && !updated) {
                    continue;
                }

                int d = updated ? depth : depth - Types.Depth.ONE_PLY;
                int v = updated ? rootMoves.get(i).score : rootMoves.get(i).previousScore;

                boolean tb = Tablebases.RootInTB && Math.abs(v) < Types.Value.VALUE_MATE - Types.MAX_PLY;
                v = tb ? Tablebases.Score : v;

                ss.append("info")
                        .append(" depth " + (d / Types.Depth.ONE_PLY))
                        .append(" seldepth " + rootMoves.get(i).selDepth)
                        .append(" multipv " + (i + 1))
                        .append(" score " + value(v));

                if (!tb && i == PVIdx) {
                    ss.append((v >= beta ? " lowerbound" : v <= alpha ? " upperbound" : ""));
                }

                ss.append(" nodes " + nodesSearched + " nps " + (nodesSearched * 1000 / elapsed));

                if (elapsed > 1000) {
                    ss.append(" hashfull " + tt.TT.hashfull());
                }

                ss.append(" tbhits " + tbHits + " time " + elapsed + " pv");

                for (int m : rootMoves.get(i).pv) {
                    ss.append(" " + move(m, pos.chess960));
                }
            }

            return ss.toString();
        }


        public static int to_move(Position pos, String str) {
            for (Movegen.ExtMove m : (new Movegen.MoveList(Movegen.GenType.LEGAL, pos)).moveList) {
                if (str.equals(move(m.move, pos.chess960))) {
                    return m.move;
                }
            }

            return Types.Move.MOVE_NONE;
        }


        public static void position(Position pos, Scanner is, Position.StateListPtr[] states) {
            int m;
            String token, fen = null;

            token = is.next();

            if (token.equals("startpos")) {
                fen = StartFEN;
                if(is.hasNext()) {
                    token = is.next();
                }
            } else if (token.equals("fen")) {
                while (is.hasNext() && (token = is.next()) != null && !token.equals("moves")) {
                    fen += token + " ";
                }
            } else {
                is.close();
                return;
            }

            states[0] = new Position.StateListPtr();
            states[0].addLast(new Position.StateInfo());
            pos.set(fen, false, states[0].getLast(), thread.Threads.main());

            while (is.hasNext() && (token = is.next()) != null && (m = to_move(pos, token)) != Types.Move.MOVE_NONE) {
                states[0].addLast(new Position.StateInfo());
                pos.do_move(m, states[0].getLast());
            }
        }


        public static String move(int m, boolean chess960) {
            int from = ((m >>> 6) & 0x3F);
            int to = (m & 0x3F);

            if (m == Types.Move.MOVE_NONE) {
                return "(none)";
            }

            if (m == Types.Move.MOVE_NULL) {
                return "0000";
            }

            if ((m & (3 << 14)) == Types.MoveType.CASTLING && !chess960) {
                to = (((from >>> 3) << 3) + (to > from ? Types.File.FILE_G : Types.File.FILE_C));
            }

            String move = square(from) + square(to);

            if ((m & (3 << 14)) == Types.MoveType.PROMOTION) {
                move += " pnbrqk".charAt((((m >>> 12) & 3) + Types.PieceType.KNIGHT));
            }

            return move;
        }


        public static String value(int v) {
            if (Math.abs(v) < Types.Value.VALUE_MATE - Types.MAX_PLY) {
                return ("cp " + (v * 100 / Types.Value.PawnValueEg));
            } else {
                return ("mate " + ((v > 0 ? Types.Value.VALUE_MATE - v + 1 : -Types.Value.VALUE_MATE - v) / 2));
            }
        }
    }
}
