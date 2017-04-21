package net.funkyjava.gametheory.extensiveformgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

public class ActionTree<Id, Chances> {

  private final LinkedActionTreeNode<Id, Chances> root;
  private final int maxNbActions;
  private final int maxDepth;
  private final LinkedActionTreeNode<Id, Chances>[][][] actionNodes;

  @SuppressWarnings("unchecked")
  public ActionTree(final Game<Id, Chances> game) {
    final MutableInt maxNbActions = new MutableInt();
    final MutableInt depth = new MutableInt();
    final MutableInt maxDepth = new MutableInt();
    final int nbPlayers = game.getNbPlayers();
    final int[][] roundsSizes = game.roundChancesSizes();
    final int nbRounds = roundsSizes.length;
    final List<LinkedActionTreeNode<Id, Chances>>[][] nodes = new List[nbRounds][nbPlayers];
    for (int i = 0; i < nbRounds; i++) {
      for (int j = 0; j < nbPlayers; j++) {
        nodes[i][j] = new ArrayList<>();
      }
    }
    this.root = buildActionTreeRec(maxNbActions, depth, maxDepth, game.rootNode(), nodes,
        new LinkedList<double[]>());
    this.maxNbActions = maxNbActions.intValue();
    this.maxDepth = maxDepth.intValue();
    this.actionNodes = new LinkedActionTreeNode[nbRounds][nbPlayers][];
    for (int i = 0; i < nbRounds; i++) {
      for (int j = 0; j < nbPlayers; j++) {
        this.actionNodes[i][j] = nodes[i][j].toArray(new LinkedActionTreeNode[0]);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static final <Id, Chances> LinkedActionTreeNode<Id, Chances> buildActionTreeRec(
      final MutableInt maxNbActions, final MutableInt depth, final MutableInt maxDepth,
      final ActionTreeNode<Id, Chances> state,
      final List<LinkedActionTreeNode<Id, Chances>>[][] nodes, final List<double[]> payoutsList) {
    depth.increment();
    switch (state.getNodeType()) {
      case PAYOUTS_NO_CHANCE:
        double[] payouts = state.getPayoutsNoChance();
        for (double[] p : payoutsList) {
          if (Arrays.equals(p, payouts)) {
            payouts = p;
            break;
          }
        }
        maxDepth.setValue(Math.max(maxDepth.intValue(), depth.intValue()));
        depth.decrement();
        return new LinkedActionTreeNode<>(payouts);
      case CHANCES_PAYOUTS:
        maxDepth.setValue(Math.max(maxDepth.intValue(), depth.intValue()));
        depth.decrement();
        return new LinkedActionTreeNode<>(state.getChancesPayouts());
      case PLAYER:
        final PlayerNode<Id> playerNode = state.getPlayerNode();
        final int round = playerNode.getRound();
        final int player = playerNode.getPlayer();
        if (state.isPlayerNodeHasMultipleParents() && playerNode.getId() != null) {
          // Check if we already built this node
          for (LinkedActionTreeNode<Id, Chances> node : nodes[round][player]) {
            final Id id = playerNode.getId();
            if (id != null && id.equals(playerNode.getId())) {
              return node;
            }
          }
        }
        final int nbChildren = playerNode.getNbActions();

        maxNbActions.setValue(Math.max(nbChildren, maxNbActions.intValue()));
        final LinkedActionTreeNode<Id, Chances>[] children = new LinkedActionTreeNode[nbChildren];
        for (int i = 0; i < nbChildren; i++) {
          children[i] = buildActionTreeRec(maxNbActions, depth, maxDepth,
              state.getTransition().nodeForAction(i), nodes, payoutsList);
        }
        final List<LinkedActionTreeNode<Id, Chances>> roundPlayerNodes = nodes[round][player];
        final int index = roundPlayerNodes.size();
        final LinkedActionTreeNode<Id, Chances> node =
            new LinkedActionTreeNode<>(playerNode, children, index);
        roundPlayerNodes.add(node);
        depth.decrement();
        return node;
      default:
        return null;
    }
  }

  public LinkedActionTreeNode<Id, Chances> getRoot() {
    return root;
  }

  public int getMaxNbActions() {
    return maxNbActions;
  }

  public int getMaxDepth() {
    return maxDepth;
  }

  public LinkedActionTreeNode<Id, Chances>[][][] getActionNodes() {
    return actionNodes;
  }
}
