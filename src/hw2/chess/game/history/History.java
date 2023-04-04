package hw2.chess.game.history;

import java.util.Iterator;
import java.util.Stack;

import hw2.chess.game.Game;
import hw2.chess.game.move.Move;
import hw2.chess.utils.Pair;

public class History extends Object
{

	private Stack<Pair<Move, Game> > history;

	private static History singletonInstance = null;

	private History()
	{
		this.history = new Stack<Pair<Move, Game> >();
	}

	private Stack<Pair<Move, Game> > getStack() { return this.history; }

	public int size() { return this.getStack().size(); }

	public Pair<Move, Game> getPastState(int pastIdx){ return this.getStack().get(pastIdx); }
	public Move getPastMove(int pastIdx) { return this.getPastState(pastIdx).getFirst(); }
	public Game getPastGame(int pastIdx) { return this.getPastState(pastIdx).getSecond(); }

	public void addState(Move move, Game game) { this.getStack().add(new Pair<>(move, game)); }

	public Stack<Pair<Move, Game> > getPastStates(int firstPastStateIdx,
			int numPastStatesToGet)
	{
		Stack<Pair<Move, Game> > states = new Stack<Pair<Move, Game> >();

		// listIterator starts at the specified index
		Iterator<Pair<Move, Game> > stateIterator = this.getStack().listIterator(firstPastStateIdx);
		while(numPastStatesToGet > 0 && stateIterator.hasNext())
		{
			states.add(stateIterator.next());
		}

		return states;
	}
	public Stack<Move> getPastMoves(int firstPastStateIdx,
			int numPastStatesToGet)
	{
		Stack<Move> moves = new Stack<Move>();

		// listIterator starts at the specified index
		Iterator<Pair<Move, Game> > stateIterator = this.getStack().listIterator(firstPastStateIdx);
		while(numPastStatesToGet > 0 && stateIterator.hasNext())
		{
			moves.add(stateIterator.next().getFirst());
			numPastStatesToGet--;
		}

		return moves;
	}
	public Stack<Game> getPastGames(int firstPastStateIdx,
			int numPastStatesToGet)
	{
		Stack<Game> games = new Stack<Game>();

		// listIterator starts at the specified index
		Iterator<Pair<Move, Game> > stateIterator = this.getStack().listIterator(firstPastStateIdx);
		while(numPastStatesToGet > 0 && stateIterator.hasNext())
		{
			games.add(stateIterator.next().getSecond());
		}

		return games;
	}

	public static synchronized History getHistory()
	{
		if(History.singletonInstance == null)
		{
			synchronized(History.class)
			{
				if(History.singletonInstance == null)
				{
					History.singletonInstance = new History();
				}
			}
		}
		return History.singletonInstance;
	}
}
