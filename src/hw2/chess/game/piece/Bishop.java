package hw2.chess.game.piece;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.util.Direction;
import hw2.chess.game.Board;
import hw2.chess.game.Game;
import hw2.chess.game.move.Move;
import hw2.chess.game.player.Player;
import hw2.chess.utils.Coordinate;

public class Bishop extends Piece
{

//	Bishop(int pieceID, int unitID, Player player)
//	{
//		super(pieceID, unitID, player, PieceType.BISHOP);
//	}
//
//	Bishop(int pieceID, int unitID, Player player, Coordinate pos)
//	{
//		super(pieceID, unitID, player, PieceType.BISHOP, pos);
//	}

	Bishop(int pieceID, Player player)
	{
		super(pieceID, player, PieceType.BISHOP);
	}

	@Override
	public List<Move> getAllCaptureMoves(Game game)
	{
		List<Move> captureMoves = new ArrayList<Move>(4);
		for(Direction direction : new Direction[]{Direction.NORTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST, Direction.SOUTHEAST})
		{
			// try to go from the current position all the way down until we either go off the board or the square is occupied
			Coordinate newPosition = this.getCurrentPosition(game.getBoard()).getNeighbor(direction);
			while(game.getBoard().isInbounds(newPosition) && !game.getBoard().isPositionOccupied(newPosition))
			{
				newPosition = newPosition.getNeighbor(direction);
			}

			Piece enemyPiece = game.getBoard().getPieceAtPosition(newPosition);
			if(game.getBoard().isPositionOccupied(newPosition) && this.isEnemyPiece(enemyPiece))
			{
				captureMoves.add(Move.createCaptureMove(this, enemyPiece));
			}
		}
		return captureMoves;
	}

	@Override
	public List<Move> getAllMoves(Game game)
	{
		// bishops move along diagonals, so the longest diagonals are the hypotenuse of the board
		// for a chessboard, which is always square, the number of squares on the diagonal = a dim
		List<Move> allMoves = new ArrayList<Move>(Board.Constants.NROWS + Board.Constants.NCOLS - 2);

		for(Direction direction : new Direction[]{Direction.NORTHEAST, Direction.SOUTHWEST, Direction.NORTHWEST, Direction.SOUTHEAST})
		{
			// try to go from the current position all the way down until we either go off the board or the square is occupied
			Coordinate newPosition = this.getCurrentPosition(game.getBoard()).getNeighbor(direction);
			while(game.getBoard().isInbounds(newPosition) && !game.getBoard().isPositionOccupied(newPosition))
			{
				allMoves.add(Move.createMovementMove(this, newPosition));
				newPosition = newPosition.getNeighbor(direction);
			}

			Piece enemyPiece = game.getBoard().getPieceAtPosition(newPosition);
			if(game.getBoard().isPositionOccupied(newPosition) && this.isEnemyPiece(enemyPiece))
			{
				allMoves.add(Move.createCaptureMove(this, enemyPiece));
			}
		}

		return allMoves;
	}

}
