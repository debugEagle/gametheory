package net.funkyjava.gametheory.games.nlhe;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import net.funkyjava.gametheory.extensiveformgame.ChancesPayouts;
import net.funkyjava.gametheory.extensiveformgame.Game;
import net.funkyjava.gametheory.extensiveformgame.GameActionStateWalker;
import net.funkyjava.gametheory.extensiveformgame.PlayerNode;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHand;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandParser;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.SharedPot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.NoBetPlayerData;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayerData;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLAbstractedBetTree;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLBetTreeNode;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLFormalBetTreeAbstractor;

public class NoLimitHoldEm<PlayerId, Chances> implements Game<NLBetTreeNode<PlayerId>, Chances> {

  private final int nbRounds;
  private final int nbPlayers;
  private final NLAbstractedBetTree<PlayerId> betTree;
  private final NLHEEquityProvider<Chances> equityProvider;
  private final int[][] roundChancesSizes;

  public NoLimitHoldEm(final NLAbstractedBetTree<PlayerId> betTree, final int[] roundChancesSizes,
      final NLHEEquityProvider<Chances> equityProvider) {
    this.equityProvider = equityProvider;
    this.betTree = betTree;
    final int nbRounds = this.nbRounds = betTree.nbOfBetRounds;
    checkArgument(this.nbRounds == roundChancesSizes.length,
        "The number of rounds is not consistant between the bet tree and the round chances sizes");
    final int nbPlayers = this.nbPlayers = betTree.getNbPlayers();
    final int[][] roundsPlayersChancesSizes = this.roundChancesSizes = new int[nbRounds][nbPlayers];
    for (int i = 0; i < nbRounds; i++) {
      for (int j = 0; j < nbPlayers; j++) {
        roundsPlayersChancesSizes[i][j] = roundChancesSizes[i];
      }
    }
  }

  public static <Chances> NoLimitHoldEm<Integer, Chances> get(final String formalBetTreePath,
      final String handString, final int[] roundChancesSizes,
      final NLHEEquityProvider<Chances> equityProvider, final boolean perfectRecall)
      throws FileNotFoundException, IOException {
    final NLHand<Integer> hand = NLHandParser.parse(handString, roundChancesSizes.length);
    final NLFormalBetTreeAbstractor<Integer> abstractor =
        NLFormalBetTreeAbstractor.read(formalBetTreePath);
    final NLAbstractedBetTree<Integer> betTree =
        new NLAbstractedBetTree<>(hand, abstractor, perfectRecall);
    return new NoLimitHoldEm<>(betTree, roundChancesSizes, equityProvider);
  }

  @Override
  public int[][] roundChancesSizes() {
    return roundChancesSizes;
  }

  @Override
  public int getNbPlayers() {
    return nbPlayers;
  }

  @Override
  public GameActionStateWalker<NLBetTreeNode<PlayerId>, Chances> rootGameStateWalker() {
    return getWalker(betTree.getRootNode());
  }

  private NLHEWalker getWalker(final NLBetTreeNode<PlayerId> node) {
    switch (node.roundState) {
      case END_NO_SHOWDOWN:
        return new NLHEWalker(getPayouts(node));
      case SHOWDOWN:
        return new NLHEWalker(getChancesPayouts(node));
      case WAITING_MOVE:
        return new NLHEWalker(getPlayerNode(node),
            node.isRoundFirstNode && !betTree.isPerfectRecall());
      default:
        throw new IllegalArgumentException();

    }
  }

  private static final <PlayerId> double[] getPayouts(final NLBetTreeNode<PlayerId> node) {
    final NLHand<PlayerId> hand = node.getHand();
    final List<NoBetPlayerData<PlayerId>> initialData = hand.getInitialPlayersData();
    final List<PlayerId> players = hand.getOrderedPlayers();
    final int nbPlayers = players.size();
    final List<PlayerData<PlayerId>> endData = hand.getPlayersData();
    final double[] payouts = new double[nbPlayers];
    for (int i = 0; i < nbPlayers; i++) {
      payouts[i] = endData.get(i).getStack() - initialData.get(i).getStack();
    }
    final List<SharedPot<PlayerId>> pots = hand.getSharedPots().get();
    final PlayerId winningPlayer = hand.getNoShowdownWinningPlayer();
    final int playerIndex = players.indexOf(winningPlayer);
    int addToWinner = 0;
    for (SharedPot<PlayerId> pot : pots) {
      addToWinner += pot.getPot().getValue();
    }
    payouts[playerIndex] += addToWinner;
    return payouts;
  }

  private final ChancesPayouts<Chances> getChancesPayouts(final NLBetTreeNode<PlayerId> node) {
    return new NLHEChancesPayouts<>(node.getHand(), equityProvider);
  }

  private static final <PlayerId> PlayerNode<NLBetTreeNode<PlayerId>> getPlayerNode(
      final NLBetTreeNode<PlayerId> node) {
    return new PlayerNode<>(node.playerIndex, node.betRoundIndex, node.nbChildren, node);
  }

  private class NLHEWalker extends GameActionStateWalker<NLBetTreeNode<PlayerId>, Chances> {

    private final NLBetTreeNode<PlayerId> node;

    public NLHEWalker(final double[] payouts) {
      super(payouts);
      this.node = null;
    }

    public NLHEWalker(final ChancesPayouts<Chances> chancesPayouts) {
      super(chancesPayouts);
      this.node = null;
    }

    public NLHEWalker(final PlayerNode<NLBetTreeNode<PlayerId>> playerNode,
        final boolean hasMultipleParents) {
      super(playerNode, hasMultipleParents);
      this.node = playerNode.getId();
    }

    @Override
    public GameActionStateWalker<NLBetTreeNode<PlayerId>, Chances> stateForPlayerAction(
        int actionIndex) {
      if (node == null) {
        return null;
      }
      return getWalker(node.orderedChildren[actionIndex]);
    }
  }

}
