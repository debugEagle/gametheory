package net.funkyjava.gametheory.cscfrm.games.poker.nlhe;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.funkyjava.gametheory.cscfrm.model.game.CSCFRMGame;
import net.funkyjava.gametheory.cscfrm.model.game.nodes.Node;
import net.funkyjava.gametheory.cscfrm.model.game.nodes.PlayerNode;
import net.funkyjava.gametheory.cscfrm.model.game.nodes.TerminalNode;
import net.funkyjava.gametheory.cscfrm.model.game.nodes.provider.NodesProvider;
import net.funkyjava.gametheory.cscfrm.util.game.helpers.ArraysIterator;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandRounds;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.SharedPot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundState;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayerData;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLAbstractedBetTree;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLBetTreeDepthCounter;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLBetTreeNode;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopEquityTables;

public class NLHE3PlayersPreflop<PNode extends PlayerNode, PlayerId> implements CSCFRMGame<PNode> {

	private int[] chances;
	private final NLHE3PlayersPreflopChances drawer;
	private final int maxNbOfActions;
	private final NLBetTreeNode<PlayerId>[] betNodes;
	private final PNode[] playerNodes;
	private final int maxDepth;
	private final Node[] currentNodes;
	private final NLBetTreeNode<PlayerId>[] currentBetNodes;

	private final TerminalNode[][][][] showdownNodes;
	private TerminalNode[] currentShowdownNodes;
	private final TerminalNode[] noShowdownNodes;
	private int currentIndex = 0;

	@SuppressWarnings("unchecked")
	public NLHE3PlayersPreflop(final NodesProvider<PNode> nodesProvider, final NLHE3PlayersPreflopChances drawer,
			final NLAbstractedBetTree<PlayerId> betTree, final ThreePlayersPreflopEquityTables tables) {
		checkArgument(betTree.nbOfBetRounds == 1, "Expected only one round");
		checkArgument(tables.isComputed(), "Equity tables are not computed");
		this.drawer = drawer;
		this.maxNbOfActions = betTree.getMaxNbOfActions();
		final List<PNode> pNodes = new LinkedList<>();
		betNodes = betTree.betRoundsNodes[0];
		for (NLBetTreeNode<PlayerId> node : betNodes) {
			pNodes.add(nodesProvider.getPlayerNode(node.playerIndex, node.nbChildren));
		}
		playerNodes = (PNode[]) pNodes.toArray();

		final NLBetTreeDepthCounter<PlayerId> depthCounter = new NLBetTreeDepthCounter<>();
		betTree.walk(depthCounter);
		maxDepth = depthCounter.getDepth();
		currentNodes = new Node[maxDepth];
		currentNodes[0] = playerNodes[betTree.getRootNode().index];

		currentBetNodes = (NLBetTreeNode<PlayerId>[]) new Object[maxDepth];
		currentBetNodes[0] = betTree.getRootNode();

		final List<PlayerId> orderedPlayers = betTree.getOriginalHand().getOrderedPlayers();

		final double[] initStacks = getStacks(betTree.getOriginalHand().getPlayersData());
		final int nbShowdown = betTree.showdownNodes.length;
		showdownNodes = new TerminalNode[169][169][169][nbShowdown];
		for (int i = 0; i < 169; i++) {
			for (int j = 0; j < 169; j++) {
				for (int k = 0; k < 169; k++) {
					final double[][] equities = tables.getReducedEquities()[i][j][k];
					for (int l = 0; l < nbShowdown; l++) {
						final NLBetTreeNode<PlayerId> node = betTree.showdownNodes[l];
						final double[] payoffs = getStacks(node.getHand().getPlayersData());
						for (int m = 0; m < 3; m++) {
							payoffs[m] -= initStacks[m];
						}
						final List<Pot<PlayerId>> pots = node.getHand().getCurrentPots();
						for (Pot<PlayerId> pot : pots) {
							final List<PlayerId> potPlayers = pot.getPlayers();
							final double potVal = pot.getValue();
							double[] eq = null;
							if (potPlayers.size() == 3) {
								eq = equities[0];
							} else if (potPlayers.contains(orderedPlayers.get(0))
									&& potPlayers.contains(orderedPlayers.get(1))) {
								eq = equities[1];
							} else if (potPlayers.contains(orderedPlayers.get(0))
									&& potPlayers.contains(orderedPlayers.get(2))) {
								eq = equities[2];
							} else if (potPlayers.contains(orderedPlayers.get(0))
									&& potPlayers.contains(orderedPlayers.get(2))) {
								eq = equities[3];
							}
							for (int m = 0; m < 3; m++) {
								payoffs[m] += eq[m] * potVal;
							}
						}
						showdownNodes[i][j][k][l] = nodesProvider.getTerminalNode(payoffs);
					}
				}
			}
		}

		final NLBetTreeNode<PlayerId>[] noShowdownTreeNodes = betTree.noShowdownNodes;
		final int nbNoShowdown = noShowdownTreeNodes.length;
		noShowdownNodes = new TerminalNode[nbNoShowdown];
		for (int i = 0; i < nbNoShowdown; i++) {
			final NLBetTreeNode<PlayerId> node = noShowdownTreeNodes[i];
			final NLHandRounds<PlayerId> hand = node.getHand();
			final double[] payoffs = getStacks(hand.getPlayersData());
			for (int j = 0; j < 3; j++) {
				payoffs[j] -= initStacks[j];
			}
			final List<SharedPot<PlayerId>> pots = hand.getSharedPots().get();
			final int playerIndex = orderedPlayers.indexOf(hand.getNoShowdownWinningPlayer());
			for (SharedPot<PlayerId> pot : pots) {
				payoffs[playerIndex] += pot.getPot().getValue();
			}
			noShowdownNodes[i] = nodesProvider.getTerminalNode(payoffs);
		}

	}

	private double[] getStacks(final List<PlayerData<PlayerId>> playersData) {
		final double[] stacks = new double[3];
		for (int i = 0; i < 3; i++) {
			stacks[i] = playersData.get(i).getStack();
		}
		return stacks;
	}

	@Override
	public void onIterationStart() throws Exception {
		final int[] chances = this.chances = drawer.getChances(this.chances);
		currentShowdownNodes = showdownNodes[chances[0]][chances[1]][chances[2]];
	}

	@Override
	public void back() {
		currentIndex--;
	}

	@Override
	public Node getCurrentNode() {
		return currentNodes[currentIndex];
	}

	@Override
	public int getNbPlayers() {
		return 3;
	}

	@Override
	public int getMaxDepth() {
		return maxDepth;
	}

	@Override
	public int getMaxNbPlActions() {
		return maxNbOfActions;
	}

	@Override
	public String getUId() {
		return "NLHE3PlayersPreflop";
	}

	@Override
	public int choseChanceAction() {
		// Unnecessary, won't present any chance node
		return 0;
	}

	@Override
	public void onGameStart() {
		// Nothing to do
	}

	@Override
	public void onPlayerActionChosen(int actionIndex) {
		final NLBetTreeNode<PlayerId>[] currentBetNodes = this.currentBetNodes;
		final NLBetTreeNode<PlayerId> nextNode = currentBetNodes[currentIndex].orderedChildren[actionIndex];
		final RoundState state = nextNode.roundState;
		final int index = ++currentIndex;
		switch (state) {
		case CANCELED:
			throw new IllegalStateException();
		case END_NO_SHOWDOWN:
			currentNodes[index] = noShowdownNodes[nextNode.index];
			break;
		case NEXT_ROUND:
			throw new IllegalStateException();
		case SHOWDOWN:
			currentNodes[index] = currentShowdownNodes[nextNode.index];
			break;
		case WAITING_MOVE:
			currentBetNodes[index] = nextNode;
			currentNodes[index] = playerNodes[nextNode.index];
			break;
		}
	}

	@Override
	public Iterator<PNode> getPlayerNodesIterator() {
		return ArraysIterator.get(playerNodes);
	}

}
