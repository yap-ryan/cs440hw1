package hw2.agents.heuristics;

import java.util.*;

import edu.cwru.sepia.util.Direction;
import hw2.agents.AlphaBetaAgent;
import hw2.agents.heuristics.DefaultHeuristics.DefensiveHeuristics;
import hw2.chess.game.history.History;
import hw2.chess.game.move.CaptureMove;
import hw2.chess.game.move.Move;
import hw2.chess.game.move.MoveType;
import hw2.chess.game.move.MovementMove;
import hw2.chess.game.move.PromotePawnMove;
import hw2.chess.game.piece.Piece;
import hw2.chess.game.piece.PieceType;
import hw2.chess.game.player.Player;
import hw2.chess.search.DFSTreeNode;
import hw2.chess.utils.Coordinate;

public class CustomHeuristics
{	
	
	/**
	 * Get the max player from a node
	 * @param node
	 * @return
	 */
	public static Player getMaxPlayer(DFSTreeNode node)
	{
		return node.getMaxPlayer();
	}

	/**
	 * Get the min player from a node
	 * @param node
	 * @return
	 */
	public static Player getMinPlayer(DFSTreeNode node)
	{
		return CustomHeuristics.getMaxPlayer(node).equals(node.getGame().getCurrentPlayer()) ? node.getGame().getOtherPlayer() : node.getGame().getCurrentPlayer();
	}
	
	public static class OffensiveHeuristics extends Object
	{
		public static int getNumberOfPiecesThreateningMinPlayer(DFSTreeNode node)
		{
			int numPiecesWeAreThreatening = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(getMaxPlayer(node)))
			{
				numPiecesWeAreThreatening += piece.getAllCaptureMoves(node.getGame()).size();
			}
						
			return numPiecesWeAreThreatening;
		}
	}

	public static class DefensiveHeuristics extends Object
	{
		
		public static int getNumberOfMaxPlayersAlivePieces(DFSTreeNode node)
		{
			int numMaxPlayersPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numMaxPlayersPiecesAlive += node.getGame().getNumberOfAlivePieces(getMaxPlayer(node), pieceType);
			}
			return numMaxPlayersPiecesAlive;
		}

		public static int getNumberOfMinPlayersAlivePieces(DFSTreeNode node)
		{
			int numMaxPlayersPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numMaxPlayersPiecesAlive += node.getGame().getNumberOfAlivePieces(getMinPlayer(node), pieceType);
			}
			return numMaxPlayersPiecesAlive;
		}
		
		public static int getClampedPieceValueTotalSurroundingMaxPlayersKing(DFSTreeNode node)
		{
			// what is the state of the pieces next to the king? add up the values of the neighboring pieces
			// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
			int maxPlayerKingSurroundingPiecesValueTotal = 0;

			Piece kingPiece = node.getGame().getBoard().getPieces(getMaxPlayer(node), PieceType.KING).iterator().next();
			Coordinate kingPosition = node.getGame().getCurrentPosition(kingPiece);
			for(Direction direction : Direction.values())
			{
				Coordinate neightborPosition = kingPosition.getNeighbor(direction);
				if(node.getGame().getBoard().isInbounds(neightborPosition) && node.getGame().getBoard().isPositionOccupied(neightborPosition))
				{
					Piece piece = node.getGame().getBoard().getPieceAtPosition(neightborPosition);
					int pieceValue = Piece.getPointValue(piece.getType());
					if(piece != null && kingPiece.isEnemyPiece(piece))
					{
						maxPlayerKingSurroundingPiecesValueTotal -= pieceValue;
					} else if(piece != null && !kingPiece.isEnemyPiece(piece))
					{
						maxPlayerKingSurroundingPiecesValueTotal += pieceValue;
					}
				}
			}
			// kingSurroundingPiecesValueTotal cannot be < 0 b/c the utility of losing a game is 0, so all of our utility values should be at least 0
			maxPlayerKingSurroundingPiecesValueTotal = Math.max(maxPlayerKingSurroundingPiecesValueTotal, 0);
			return maxPlayerKingSurroundingPiecesValueTotal;
		}

		public static int getNumberOfPiecesThreateningMaxPlayer(DFSTreeNode node)
		{
			// how many pieces are threatening us?
			int numPiecesThreateningMaxPlayer = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(getMinPlayer(node)))
			{
				numPiecesThreateningMaxPlayer += piece.getAllCaptureMoves(node.getGame()).size();
			}
			return numPiecesThreateningMaxPlayer;
		}
		
		// Calculate the points of alive pieces to get a more accurate reading of the value of our board
		public static int getPointsOfAlivePieces(DFSTreeNode node)
		{
			int ptsPiecesAlive = 0;
						
			for(Piece p : node.getGame().getBoard().getPieces(getMaxPlayer(node)))
			{
				ptsPiecesAlive += Piece.getPointValue(p.getType());
			}
			
			return ptsPiecesAlive;
		}
	}

	public static double getOffensiveHeuristicValue(DFSTreeNode node)
	{
		// remember the action has already taken affect at this point, so capture moves have already resolved
		// and the targeted piece will not exist inside the game anymore.
		// however this value was recorded in the amount of points that the player has earned in this node
		double damageDealtInThisNode = node.getGame().getBoard().getPointsEarned(getMaxPlayer(node));

		switch(node.getMove().getType())
		{
		case PROMOTEPAWNMOVE:
			PromotePawnMove promoteMove = (PromotePawnMove)node.getMove();
			damageDealtInThisNode += 2*Piece.getPointValue(promoteMove.getPromotedPieceType());
			break;
		default:
			break;
		}
		
		// offense can typically include the number of pieces that our pieces are currently threatening
		int numPiecesWeAreThreatening = OffensiveHeuristics.getNumberOfPiecesThreateningMinPlayer(node);
		
		double numMinPieces = DefensiveHeuristics.getNumberOfMinPlayersAlivePieces(node);

		double nPWARWeight = 2;
		double dDITNWeight = numMinPieces < 6 ? 1.5 : 1.2;
		// Increase "damage done" weight depending on if the enemy is low on pieces (go offensive when the enemy when they are weak)
		
//		System.out.println("Damage Dealt: " + damageDealtInThisNode + " weighted: " + damageDealtInThisNode*dDITNWeight);
//		System.out.println("# Pieces we are threatening: " + numPiecesWeAreThreatening + " weighted: " + numPiecesWeAreThreatening*nPWARWeight);

		return (damageDealtInThisNode*dDITNWeight) + (numPiecesWeAreThreatening*nPWARWeight);
	}

	public static double getDefensiveHeuristicValue(DFSTreeNode node)
	{		
		double numMinPieces = DefensiveHeuristics.getNumberOfMinPlayersAlivePieces(node);
		double numMaxPieces = DefensiveHeuristics.getNumberOfMaxPlayersAlivePieces(node);
		
		int ptsPiecesAlive = DefensiveHeuristics.getPointsOfAlivePieces(node);
		double pPAWeight = 0.7;

		// what is the state of the pieces next to the king? add up the values of the neighboring pieces
		// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
		int kingSurroundingPiecesValueTotal = DefensiveHeuristics.getClampedPieceValueTotalSurroundingMaxPlayersKing(node);
		// Let the king defenders focus on attacking instead when we have more pieces!
		double kSPVTWeight = numMaxPieces-numMinPieces > 4 ? 0.5 : 1.5;

		// how many pieces are threatening us?
		// More pieces threatening us ==> WORSE, we want to minimize this!
		int numPiecesThreateningUs = DefensiveHeuristics.getNumberOfPiecesThreateningMaxPlayer(node);
		double nPTUWeight = -10;
				
//		System.out.println("PtsPiecesAlive: " + ptsPiecesAlive + " weighted: " + pPAWeight*ptsPiecesAlive);
//		System.out.println("kingSurroundingPiecesValueTotal: " +kingSurroundingPiecesValueTotal + " weighted: " + kSPVTWeight*kingSurroundingPiecesValueTotal);
//		System.out.println("numPiecesThreateningUs: " + numPiecesThreateningUs + " weighted: " + nPTUWeight*numPiecesThreateningUs);
		
		return (pPAWeight * ptsPiecesAlive) + (kSPVTWeight * kingSurroundingPiecesValueTotal) + (nPTUWeight * numPiecesThreateningUs);

	}

	public static double getNonlinearPieceCombinationHeuristicValue(DFSTreeNode node)
	{
		// both bishops are worth more together than a single bishop alone
		// same with knights...we want to encourage keeping pairs of elements
		double multiPieceValueTotal = 0.0;

		double exponent = 1.5; // f(numberOfKnights) = (numberOfKnights)^exponent

		// go over all the piece types that have more than one copy in the game (including pawn promotion)
		for(PieceType pieceType : new PieceType[] {PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK, PieceType.QUEEN})
		{
			multiPieceValueTotal += Math.pow(node.getGame().getNumberOfAlivePieces(getMaxPlayer(node), pieceType), exponent);
		}

		return multiPieceValueTotal;
	}
	
	// Return negative heuristic value if movement move was repeated recently
	public static double getRepeatMoveHeuristic(DFSTreeNode node) 
	{		
		double heuristic = 0;
		
		History h = History.getHistory();
		int numPastStates = 16;
		int startIdx = h.size() >= numPastStates ? h.size()-numPastStates : 0;
		Stack<Move> recentMoves = h.getPastMoves(startIdx,numPastStates);
		
		int pieceFreq = 0;
		int repFreq = 0;
		
		Move currMove = node.getMove();
		
		if (currMove.getType() == MoveType.MOVEMENTMOVE) {

			for (Move m : recentMoves) {				
				// Recent move is made by same player & Moves are movement moves
				if (m.getActorPlayer() == node.getMaxPlayer() && m.getType() == MoveType.MOVEMENTMOVE) {					
//				System.out.println(m);
					
					if (((MovementMove)currMove).getActorPieceID() == ((MovementMove)m).getActorPieceID()) {
						// A recent move has been made by the same piece!
						pieceFreq ++;
						if (((MovementMove)currMove).getTargetPosition().equals(((MovementMove)m).getTargetPosition())) {
							// A recent move has been made by the same piece & to the same position
							repFreq ++;
						}
					}
				}
			}
		}
		
		// If a move was made the same position, heavily tax the heuristic
		if (repFreq > 0) {
//			System.out.println("Repeat Freq: " + repFreq);
			heuristic = - (Math.pow(4, repFreq));
		}
		
		// If a move was made by same piece, tax the heuristic
		if (pieceFreq > 0) {
			heuristic -= Math.pow(2.7,pieceFreq);
		}
		
//		System.out.println("REPEAT HEURISTIC: " + heuristic);
		return heuristic;
	}
	
	// Heuristic to encourage pawns to advance when we have more pieces
	public static double getPawnHeuristic(DFSTreeNode node) {
		double heuristic = 0;
		
		Set<Piece> pawns = node.getGame().getBoard().getPieces(getMaxPlayer(node), PieceType.ROOK);
		
		if (pawns.size() > 0) {	
			
			// If we have more pieces, advance!
			if (DefensiveHeuristics.getNumberOfMaxPlayersAlivePieces(node) > DefensiveHeuristics.getNumberOfMinPlayersAlivePieces(node)) {
				double totalYPos = 0;
				double pawnStartYPos = node.getGame().getBoard().getPawnStartingRowIdx(getMaxPlayer(node));
				for (Piece p : pawns) {
					totalYPos += p.getCurrentPosition(node.getGame().getBoard()).getYPosition();
				}
				
				double avgYPos = totalYPos / pawns.size();
				double avgVsStart =  Math.abs(avgYPos - pawnStartYPos);
				heuristic = avgVsStart * 17;
//				System.out.println("PAWN H: " + heuristic);
			}
		}
		
		return heuristic;
	}

	/**
	 * TODO: implement me! The heuristics that I wrote are useful, but not very good for a good chessbot.
	 * Please use this class to add your heuristics here! I recommend taking a look at the ones I provided for you
	 * in DefaultHeuristics.java (which is in the same directory as this file)
	 * 
	 * 
	 * In general, our heuristic will play more defensive at first, then we become more offensive as more opponent pieces are taken
	 * 
	 */
	public static double getHeuristicValue(DFSTreeNode node)
	{
		
		double numMinPieces = DefensiveHeuristics.getNumberOfMinPlayersAlivePieces(node);
		double numMaxPieces = DefensiveHeuristics.getNumberOfMaxPlayersAlivePieces(node);
		
		double offenseHeuristicValue = CustomHeuristics.getOffensiveHeuristicValue(node);
		double defenseHeuristicValue = CustomHeuristics.getDefensiveHeuristicValue(node);
		double nonlinearHeuristicValue = CustomHeuristics.getNonlinearPieceCombinationHeuristicValue(node);
		double repeatMoveHeuristic = getRepeatMoveHeuristic(node);
		double pawnHeuristic = getPawnHeuristic(node);
		
		
		
		double offWeight = numMaxPieces-numMinPieces > 5 ? 2.7 : 2.2;
		double defWeight = 1;
		double nonLinWeight = 2;
		
		System.out.println("Total Off Heuristic: " + offenseHeuristicValue*offWeight);
		System.out.println("Total Def Heuristic: " + defenseHeuristicValue*defWeight);
		System.out.println("Total Repeat Heuristic: " + repeatMoveHeuristic);
//		System.out.println("Total nonLin Heuristic: " + nonlinearHeuristicValue*nonLinWeight);
		
		return offenseHeuristicValue*offWeight + defenseHeuristicValue*defWeight + nonlinearHeuristicValue*nonLinWeight + repeatMoveHeuristic + pawnHeuristic;
	}

}
