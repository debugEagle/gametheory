package net.funkyjava.gametheory.extensiveformgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

public class GameActionTree {

	public final ActionNode root;
	public final int maxNbActions;
	public final int maxDepth;
	public final ActionNode[][][] actionNodes;

	public GameActionTree(final Game game) {
		final MutableInt maxNbActions = new MutableInt();
		final MutableInt depth = new MutableInt();
		final MutableInt maxDepth = new MutableInt();
		final int nbPlayers = game.getNbPlayers();
		final int[][] roundsSizes = game.roundChancesSizes();
		final int nbRounds = roundsSizes.length;
		@SuppressWarnings("unchecked")
		final List<ActionNode>[][] nodes = (List<ActionNode>[][]) new Object[nbRounds][nbPlayers];
		for (int i = 0; i < nbRounds; i++) {
			for (int j = 0; j < nbPlayers; j++) {
				nodes[i][j] = new ArrayList<ActionNode>();
			}
		}
		this.root = buildActionTreeRec(maxNbActions, depth, maxDepth, game.rootGameStateWalker(), nodes,
				new LinkedList<double[]>());
		this.maxNbActions = maxNbActions.intValue();
		this.maxDepth = maxDepth.intValue();
		this.actionNodes = new ActionNode[nbRounds][nbPlayers][];
		for (int i = 0; i < nbRounds; i++) {
			for (int j = 0; j < nbPlayers; j++) {
				this.actionNodes[i][j] = (ActionNode[]) nodes[i][j].toArray();
			}
		}
	}

	private static final ActionNode buildActionTreeRec(final MutableInt maxNbActions, final MutableInt depth,
			final MutableInt maxDepth, final GameStateWalker state, final List<ActionNode>[][] nodes,
			final List<double[]> payoutsList) {
		depth.increment();
		switch (state.nodeType) {
		case PAYOUTS_NO_CHANCE:
			double[] payouts = state.payoutsNoChance;
			for (double[] p : payoutsList) {
				if (Arrays.equals(p, payouts)) {
					payouts = p;
					break;
				}
			}
			maxDepth.setValue(Math.max(maxDepth.intValue(), depth.intValue()));
			depth.decrement();
			return new ActionNode(payouts);
		case CHANCES_PAYOUTS:
			maxDepth.setValue(Math.max(maxDepth.intValue(), depth.intValue()));
			depth.decrement();
			return new ActionNode(state.chancesPayouts);
		case PLAYER:
			final PlayerNode playerNode = state.playerNode;
			final int nbChildren = playerNode.getNumberOfActions();
			final int round = playerNode.getRound();
			final int player = playerNode.getPlayer();
			maxNbActions.setValue(Math.max(nbChildren, maxNbActions.intValue()));
			final ActionNode[] children = new ActionNode[nbChildren];
			for (int i = 0; i < nbChildren; i++) {
				children[i] = buildActionTreeRec(maxNbActions, depth, maxDepth, state.stateForPlayerAction(i), nodes,
						payoutsList);
			}
			final List<ActionNode> roundPlayerNodes = nodes[round][player];
			final int index = roundPlayerNodes.size();
			final ActionNode node = new ActionNode(playerNode, children, index);
			roundPlayerNodes.add(node);
			depth.decrement();
			return node;
		default:
			return null;
		}
	}
}
