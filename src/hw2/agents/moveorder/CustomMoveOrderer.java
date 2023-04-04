package hw2.agents.moveorder;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import hw2.agents.heuristics.CustomHeuristics;
import hw2.agents.heuristics.CustomHeuristics.DefensiveHeuristics;
import hw2.chess.game.move.MoveType;
import hw2.chess.search.DFSTreeNode;

public class CustomMoveOrderer
{

	// Comparator used to sort nodes by heuristic 
	public static Comparator<DFSTreeNode> heuristicComparator = new Comparator<DFSTreeNode>() {
		public int compare(DFSTreeNode n1, DFSTreeNode n2) {
			return Double.compare(CustomHeuristics.getHeuristicValue(n2), CustomHeuristics.getHeuristicValue(n1)); 
		}
	};

	/**
	 * TODO: implement me!
	 * This method should perform move ordering. Remember, move ordering is how alpha-beta pruning gets part of its power from.
	 * You want to see nodes which are beneficial FIRST so you can prune as much as possible during the search (i.e. be faster)
	 * @param nodes. The nodes to order (these are children of a DFSTreeNode) that we are about to consider in the search.
	 * @return The ordered nodes.
	 * 
	 * Do we use heuristics? Since we are looking at direct child nodes, and at any current node, the player is trying to maximize score.
	 * Should we put the nodes w/ lowest heuristics first? 
	 * 
	 * IDEA:  
	 * - Maybe prioritize MOVING first (when equal/lower piece values), when we get more board control, prioritize CAPTURE
	 * 
	 */
	public static List<DFSTreeNode> order(List<DFSTreeNode> nodes)
	{
		
			// Implement first case is controlling the board// by default get the CaptureMoves first
			List<DFSTreeNode> moveOrder = new LinkedList<DFSTreeNode>(); 
			List<DFSTreeNode> movementNodes = new LinkedList<DFSTreeNode>(); 
			List<DFSTreeNode> captureNodes = new LinkedList<DFSTreeNode>();
			List<DFSTreeNode> otherNodes = new LinkedList<DFSTreeNode>(); // Low priority

			for(DFSTreeNode node : nodes)
			{
				int numMinPieces = DefensiveHeuristics.getNumberOfMinPlayersAlivePieces(node);
				int numMaxPieces = DefensiveHeuristics.getNumberOfMaxPlayersAlivePieces(node);
				
				if(node.getMove() != null)
				{
					// Prioritize check mating
					if (node.getGame().isInCheckmate()) {
					    moveOrder.add(node);
					    
					// Next prioritize checking
					} else if (node.getGame().isInCheck(CustomHeuristics.getMinPlayer(node))) {
					    moveOrder.add(node);
					    
					// We are ahead, prioritize capture 
					} else if (numMaxPieces > numMinPieces) {
						if (node.getMove().getType() == MoveType.CAPTUREMOVE) {
							captureNodes.add(node);
						} else {
							otherNodes.add(node);
						}
						
					// We are behind or equal, prioritize movement (take more space)
					} else if (numMaxPieces <= numMinPieces) {
						if (node.getMove().getType() == MoveType.MOVEMENTMOVE) {
							movementNodes.add(node);
						} else {
							otherNodes.add(node);
						}
					}
					
				} else
				{
					otherNodes.add(node);
				}
			}

			moveOrder.sort(heuristicComparator);
			captureNodes.sort(heuristicComparator);
			movementNodes.sort(heuristicComparator);
			otherNodes.sort(heuristicComparator);

			moveOrder.addAll(captureNodes);
			moveOrder.addAll(movementNodes);
			moveOrder.addAll(otherNodes);

			return moveOrder;		
	}	

}
