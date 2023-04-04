package hw2.agents.moveorder;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import hw2.agents.heuristics.CustomHeuristics;
import hw2.chess.game.move.MoveType;
import hw2.chess.search.DFSTreeNode;

public class CustomMoveOrderer
{
	
//	static Comparator<DFSTreeNode> CaptureComparator = new Comparator<DFSTreeNode>() {
//		public int compare(DFSTreeNode node1, DFSTreeNode node2) {
//			double value1 = CustomHeuristics.OffensiveHeuristics.getNumberOfPiecesThreateningMinPlayer(node1);
//			double value2 = CustomHeuristics.OffensiveHeuristics.getNumberOfPiecesThreateningMinPlayer(node2);
//			return Double.compare(value2, value1);
//		}
//	};



	static Comparator<DFSTreeNode> heuristicComparator = new Comparator<DFSTreeNode>() {
		public int compare(DFSTreeNode node1, DFSTreeNode node2) {
			double value1 = CustomHeuristics.getHeuristicValue(node1);
			double value2 = CustomHeuristics.getHeuristicValue(node2);
			return Double.compare(value2, value1); // sort in descending order
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
	 * IDEA: Look at heuristics, 
	 * - Maybe prioritize MOVING first (when equal/lower piece values), when we get more board control, prioritize CAPTURE
	 * 
	 */
	public static List<DFSTreeNode> order(List<DFSTreeNode> nodes)
	{
			// Implement first case is controlling the board// by default get the CaptureMoves first
			List<DFSTreeNode> moveOrder = new LinkedList<DFSTreeNode>(); 
			
			List<DFSTreeNode> defendNodes = new LinkedList<DFSTreeNode>(); 
			List<DFSTreeNode> captureNodes = new LinkedList<DFSTreeNode>();
			List<DFSTreeNode> otherNodes = new LinkedList<DFSTreeNode>();

			for(DFSTreeNode node : nodes)
			{
				if(node.getMove() != null)
				{
					switch(node.getMove().getType())
					{
					case CAPTUREMOVE:
						captureNodes.add(node);
						break;
					default:
						otherNodes.add(node);
						break;
					}
				} else
				{
					otherNodes.add(node);
				}
			}


			captureNodes.sort(heuristicComparator);
			defendNodes.sort(heuristicComparator);
			otherNodes.sort(heuristicComparator);

			moveOrder.addAll(captureNodes);
			moveOrder.addAll(defendNodes);
			moveOrder.addAll(otherNodes);

			return moveOrder;		
	}	

}
