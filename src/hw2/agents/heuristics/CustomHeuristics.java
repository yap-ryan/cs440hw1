package hw2.agents.heuristics;

import java.util.*;

import edu.cwru.sepia.util.Direction;
import hw2.chess.game.move.CaptureMove;
import hw2.chess.game.move.Move;
import hw2.chess.game.move.PromotePawnMove;
import hw2.chess.game.piece.Piece;
import hw2.chess.game.piece.PieceType;
import hw2.chess.game.player.Player;
import hw2.chess.search.DFSTreeNode;
import hw2.chess.utils.Coordinate;

public class CustomHeuristics
{
	
	// IDEAS: 
	// - Consider the point value of pieces when count the # of pieces
	// - Change the weights of the types of heuristics
	
	public static class OffensiveHeuristics extends Object
	{
		public static int getNumberOfPiecesWeAreThreatening(DFSTreeNode node)
		{
			int numPiecesWeAreThreatening = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer()))
			{
				numPiecesWeAreThreatening = piece.getAllCaptureMoves(node.getGame()).size();
			}
						
			return numPiecesWeAreThreatening;
		}
		
	}

	public static class DefensiveHeuristics extends Object
	{
		public static int getNumberOfAlivePieces(DFSTreeNode node)
		{
			int numPiecesAlive = 0;
			for(PieceType pieceType : PieceType.values())
			{
				numPiecesAlive += node.getGame().getNumberOfAlivePieces(node.getGame().getCurrentPlayer(), pieceType);
			}
			
			return numPiecesAlive;
		}

		public static int getPointsOfAlivePieces(DFSTreeNode node)
		{
			int ptsPiecesAlive = 0;
						
			for(Piece p : node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer()))
			{
				ptsPiecesAlive += Piece.getPointValue(p.getType());
			}
			
			return ptsPiecesAlive;
		}

		public static int getClampedPieceValueTotalSurroundingKing(DFSTreeNode node)
		{
			// what is the state of the pieces next to the king? add up the values of the neighboring pieces
			// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
			int kingSurroundingPiecesValueTotal = 0;

			Piece kingPiece = node.getGame().getBoard().getPieces(node.getGame().getCurrentPlayer(), PieceType.KING).iterator().next();
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
						kingSurroundingPiecesValueTotal -= pieceValue;
					} else if(piece != null && !kingPiece.isEnemyPiece(piece))
					{
						kingSurroundingPiecesValueTotal += pieceValue;
					}
				}
			}
			// kingSurroundingPiecesValueTotal cannot be < 0 b/c the utility of losing a game is 0, so all of our utility values should be at least 0
			kingSurroundingPiecesValueTotal = Math.max(kingSurroundingPiecesValueTotal, 0);
			return kingSurroundingPiecesValueTotal;
		}

		public static int getNumberOfPiecesThreateningUs(DFSTreeNode node)
		{
			// how many pieces are threatening us?
			int numPiecesThreateningUs = 0;
			for(Piece piece : node.getGame().getBoard().getPieces(node.getGame().getOtherPlayer()))
			{
				numPiecesThreateningUs = piece.getAllCaptureMoves(node.getGame()).size();
			}
			return numPiecesThreateningUs;
		}
	}

	public static double getOffensiveHeuristicValue(DFSTreeNode node)
	{
		// remember the action has already taken affect at this point, so capture moves have already resolved
		// and the targeted piece will not exist inside the game anymore.
		// however this value was recorded in the amount of points that the player has earned in this node
		double damageDealtInThisNode = node.getGame().getBoard().getPointsEarned(node.getGame().getCurrentPlayer());

		switch(node.getMove().getType())
		{
		case PROMOTEPAWNMOVE:
			PromotePawnMove promoteMove = (PromotePawnMove)node.getMove();
			damageDealtInThisNode += Piece.getPointValue(promoteMove.getPromotedPieceType());
			break;
		default:
			break;
		}
		// offense can typically include the number of pieces that our pieces are currently threatening
		int numPiecesWeAreThreatening = OffensiveHeuristics.getNumberOfPiecesWeAreThreatening(node);
		
		double nPWARWeight = 5;
		double dDITNWeight = 1;
		
		
//		System.out.println("Damage Dealt: " + damageDealtInThisNode + " weighted: " + damageDealtInThisNode*dDITNWeight);
//		System.out.println("# Pieces we are threatening: " + numPiecesWeAreThreatening + " weighted: " + numPiecesWeAreThreatening*nPWARWeight);

		return damageDealtInThisNode + numPiecesWeAreThreatening;
	}

	public static double getDefensiveHeuristicValue(DFSTreeNode node)
	{
		// how many pieces exist on our team?
		int numPiecesAlive = DefensiveHeuristics.getNumberOfAlivePieces(node);
		
		int ptsPiecesAlive = DefensiveHeuristics.getPointsOfAlivePieces(node);
		double pPAWeight = 0.7;

		// what is the state of the pieces next to the king? add up the values of the neighboring pieces
		// positive value for friendly pieces and negative value for enemy pieces (will clamp at 0)
		int kingSurroundingPiecesValueTotal = DefensiveHeuristics.getClampedPieceValueTotalSurroundingKing(node);
		double kSPVTWeight = 1.5;

		// how many pieces are threatening us?
		// More pieces threatening us ==> WORSE, we want to minimize this!
		int numPiecesThreateningUs = DefensiveHeuristics.getNumberOfPiecesThreateningUs(node);
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
			multiPieceValueTotal += Math.pow(node.getGame().getNumberOfAlivePieces(node.getGame().getCurrentPlayer(), pieceType), exponent);
		}

		return multiPieceValueTotal;
	}

	/**
	 * TODO: implement me! The heuristics that I wrote are useful, but not very good for a good chessbot.
	 * Please use this class to add your heuristics here! I recommend taking a look at the ones I provided for you
	 * in DefaultHeuristics.java (which is in the same directory as this file)
	 */
	public static double getHeuristicValue(DFSTreeNode node)
	{
		double offenseHeuristicValue = CustomHeuristics.getOffensiveHeuristicValue(node);
		double defenseHeuristicValue = CustomHeuristics.getDefensiveHeuristicValue(node);
		double nonlinearHeuristicValue = CustomHeuristics.getNonlinearPieceCombinationHeuristicValue(node);
		
		double offWeight = 3;
		double defWeight = 1;
		double nonLinWeight = 2.5;
		
		
		System.out.println("Total Off Heuristic: " + offenseHeuristicValue*offWeight);
		System.out.println("Total Def Heuristic: " + defenseHeuristicValue*defWeight);
		System.out.println("Total nonLin Heuristic: " + nonlinearHeuristicValue*nonLinWeight);

		
		return offenseHeuristicValue*offWeight + defenseHeuristicValue*defWeight + nonlinearHeuristicValue*nonLinWeight;
//		return DefaultHeuristics.getHeuristicValue(node);
	}

}
