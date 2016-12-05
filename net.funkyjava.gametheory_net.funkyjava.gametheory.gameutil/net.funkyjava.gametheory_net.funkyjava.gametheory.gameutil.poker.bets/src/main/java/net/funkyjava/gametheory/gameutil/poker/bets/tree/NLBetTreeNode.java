package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import lombok.AllArgsConstructor;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandRounds;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.SharedPot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundState;

/**
 * 
 * A bet tree node represents a node in the bet sequences tree.
 * 
 * @author Pierre Mardon
 * 
 */
@AllArgsConstructor
public class NLBetTreeNode<PlayerId> {

	/**
	 * The children of this node when it's a bet node, null elsewhere
	 */
	private final NLBetTreeNode<PlayerId>[] children;

	/**
	 * The hand state for this node
	 */
	private final NLHandRounds<PlayerId> hand;

	/**
	 * Gets the hand state for this node
	 * 
	 * @return the hand state for this node
	 */
	public NLHandRounds<PlayerId> getHand() {
		return hand.clone();
	}

	/**
	 * Gets the child of this node for thie provided index. If this node is not
	 * a bet node, will result in a {@link NullPointerException}. There's no
	 * check on the index, it's assumed to be between 0 and the number of
	 * children minus one.
	 * 
	 * @param childIndex
	 * @return The child for the provided index
	 */
	public NLBetTreeNode<PlayerId> getChild(int childIndex) {
		return children[childIndex];
	}

	/**
	 * Gets the number of children of this bet node. If it's not a bet node,
	 * will result in a {@link NullPointerException}.
	 * 
	 * @return the number of children
	 */
	public int getChildrenCount() {
		return children.length;
	}

	/**
	 * Set the child for provided index. If this node is not a bet node, will
	 * result in a {@link NullPointerException}. There's no check on the index,
	 * it's assumed to be between 0 and the number of children minus one.
	 * 
	 * @param childIndex
	 * @param child
	 */
	public void setChild(int childIndex, NLBetTreeNode<PlayerId> child) {
		children[childIndex] = child;
	}

	/**
	 * Return the number of bets choices the player has. If this node is not a
	 * bet node, will result in a {@link NullPointerException};
	 * 
	 * @return the number of moves the player can do
	 */
	public int getPossibleBetsCount() {
		return children.length;
	}

	/**
	 * Gets this bet node's children as a list for convenience. Not intended to
	 * be used for fast tree walking.
	 * 
	 * @return the children nodes
	 */
	public List<NLBetTreeNode<PlayerId>> getChildren() {
		return Arrays.asList(children);
	}

	/**
	 * Gets this bet node's bet value / child map.
	 * 
	 * @return the bet value / child map
	 */
	public HashMap<Integer, NLBetTreeNode<PlayerId>> getBetsChildren() {
		final HashMap<Integer, NLBetTreeNode<PlayerId>> res = new HashMap<Integer, NLBetTreeNode<PlayerId>>();
		for (int i = 0; i < children.length; i++)
			res.put(values[i], children[i]);
		return res;
	}

	/**
	 * Creates a bet node.
	 * 
	 * @param hand
	 *            state of the hand for this node
	 * @param betChoices
	 *            all possible fold/call/bet/raise represented by integers
	 * @return the bet tree node
	 */
	public static <PlayerId> NLBetTreeNode<PlayerId> getBetNode(final NLHandRounds<PlayerId> hand,
			final List<Move<PlayerId>> betChoices) {
		checkArgument(hand.getRoundState() == RoundState.WAITING_MOVE, "Wrong round state for a bet node");
		return new NLBetTreeNode(new NLBetTreeNode[betChoices.length], hand.getBetRoundIndex(), -1, betChoices,
				hand.getPlayersData().getStacks(), null, hand.getBettingPlayer(), true, false, false, hand);
	}

	/**
	 * Creates a showdown node.
	 * 
	 * @param hand
	 *            state of the hand for this node
	 * @return the bet tree node
	 */
	public static <PlayerId> NLBetTreeNode<PlayerId> getShowdownNode(NLHandRounds<PlayerId> hand) {
		checkArgument(hand.getRoundState() == RoundState.SHOWDOWN, "Wrong round state for a showdown node");
		final List<Pot<PlayerId>> pots = hand.getCurrentPots();
		final int nbOfPots = pots.size();
		final int[] potsValues = new int[nbOfPots];
		final int[][] potsPlayers = new int[nbOfPots][];
		for (int i = 0; i < nbOfPots; i++) {
			final Pot<Integer> pot = pots.get(i);
			potsValues[i] = pot.getValue();
			final List<Integer> players = pot.getPlayers();
			final int nbPlayers = players.size();
			potsPlayers[i] = new int[nbPlayers];
			for (int j = 0; j < nbPlayers; j++)
				potsPlayers[i][j] = players.get(j);
		}
		return new NLBetTreeNode<>(null, hand.getBetRoundIndex(), -1, potsValues, hand.getPlayersData().getStacks(),
				potsPlayers, -1, false, true, false, hand);
	}

	/**
	 * Creates a no-showdown node.
	 * 
	 * @param hand
	 *            state of the hand for this node
	 * @return the bet tree node
	 */
	public static <PlayerId> NLBetTreeNode<PlayerId> getNoShowdownNode(NLHandRounds<PlayerId> hand) {
		checkArgument(hand.getRoundState() == RoundState.END_NO_SHOWDOWN, "Wrong round state for a no-showdown node");
		final List<SharedPot<Integer>> pots = hand.getSharedPots().get();
		checkArgument(!pots.isEmpty(), "There is no pot to create this showdown node");
		final int nbOfPots = pots.size();
		final int[] potsValues = new int[nbOfPots];
		int winningPlayer = -1;
		for (int i = 0; i < nbOfPots; i++) {
			final Pot<Integer> pot = pots.get(i).getPot();
			potsValues[i] = pot.getValue();
			winningPlayer = pot.getPlayers().get(0);
		}

		return new NLBetTreeNode<>(null, hand.getBetRoundIndex(), -1, potsValues, hand.getPlayersData().getStacks(),
				null, winningPlayer, false, false, true, hand);
	}

	public static <PlayerId> void walkTree(NLBetTreeNode<PlayerId> rootNode, SimpleNLBetTreeWalker walker) {
		walkTree(rootNode, walker, 0, 0);
	}

	private static <PlayerId> void walkTree(NLBetTreeNode<PlayerId> rootNode, SimpleNLBetTreeWalker walker, int depth,
			int lastBet) {
		if (walker.handleCurrentNode(rootNode, depth, lastBet) && rootNode.isBetNode())
			for (int i = 0; i < rootNode.getChildrenCount(); i++)
				walkTree(rootNode.getChild(i), walker, depth + 1, rootNode.getValues()[i]);
	}
}
