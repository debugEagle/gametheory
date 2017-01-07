package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NonNull;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandRounds;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundType;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.anteround.AnteValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.blindsround.BlindValue;

public class NLAbstractedBetTree<PlayerId> {

	@Getter
	private final NLHandRounds<PlayerId> originalHand;
	@Getter
	private final NLBetTreeNode<PlayerId> rootNode;
	@Getter
	private final boolean perfectRecall;

	public final int nbOfBetRounds;
	public final NLBetTreeNode<PlayerId>[] showdownNodes;
	public final NLBetTreeNode<PlayerId>[] noShowdownNodes;
	public final NLBetTreeNode<PlayerId>[][] betRoundsNodes;
	public final NLBetTreeNode<PlayerId>[][] betRoundsFirstNodes;

	private List<NLBetTreeNode<PlayerId>> showdownNodesList = new ArrayList<>();
	private List<NLBetTreeNode<PlayerId>> noShowdownNodesList = new ArrayList<>();
	private List<List<NLBetTreeNode<PlayerId>>> betRoundsNodesList = new ArrayList<>();
	private List<List<NLBetTreeNode<PlayerId>>> betRoundsFirstNodesList = new ArrayList<>();

	public NLAbstractedBetTree(@NonNull final NLHandRounds<PlayerId> hand,
			@NonNull final NLBetTreeAbstractor<PlayerId> abstractor, final boolean perfectRecall) {
		this.perfectRecall = perfectRecall;
		originalHand = hand.clone();
		nbOfBetRounds = hand.getNbBetRounds();
		for (int i = 0; i < nbOfBetRounds; i++) {
			betRoundsFirstNodesList.add(new ArrayList<NLBetTreeNode<PlayerId>>());
			betRoundsNodesList.add(new ArrayList<NLBetTreeNode<PlayerId>>());
		}
		rootNode = nodeFor(hand, abstractor);
		showdownNodes = toArray(showdownNodesList);
		noShowdownNodes = toArray(noShowdownNodesList);
		betRoundsNodes = toDoubleArray(betRoundsNodesList);
		betRoundsFirstNodes = toDoubleArray(betRoundsFirstNodesList);
		showdownNodesList = null;
		noShowdownNodesList = null;
		betRoundsNodesList = null;
		betRoundsFirstNodesList = null;
	}

	private NLBetTreeNode<PlayerId> nodeFor(@NonNull final NLHandRounds<PlayerId> hand,
			@NonNull final NLBetTreeAbstractor<PlayerId> abstractor) {
		switch (hand.getRoundState()) {
		case CANCELED:
			throw new IllegalStateException("Hand state is CANCELED");
		case SHOWDOWN:
			return findShowdownMatchOrCreate(hand);
		case END_NO_SHOWDOWN:
			return findNoShowdownMatchOrCreate(hand);
		case NEXT_ROUND:
			switch (hand.getRoundType()) {
			case ANTE:
				checkState(hand.nextRoundAfterAnte());
				break;
			case BETS:
				checkState(hand.nextBetRound());
				break;
			case BLINDS:
				checkState(hand.betRoundAfterBlinds());
				break;
			}
			return nodeFor(hand, abstractor);
		case WAITING_MOVE:
			if (hand.getRoundType() == RoundType.ANTE) {
				final Map<PlayerId, AnteValue> antes = hand.getMissingAnte();
				for (final PlayerId antePlayer : antes.keySet()) {
					hand.doMove(Move.getAnte(antePlayer, antes.get(antePlayer).getValue()));
				}
				return nodeFor(hand, abstractor);
			}
			if (hand.getRoundType() == RoundType.BLINDS) {
				final Map<PlayerId, BlindValue> blinds = hand.getMissingBlinds();
				for (final PlayerId blindsPlayer : blinds.keySet()) {
					final BlindValue blind = blinds.get(blindsPlayer);
					switch (blind.getType()) {
					case BB:
						hand.doMove(Move.getBb(blindsPlayer, blind.getValue()));
						break;
					case SB:
						hand.doMove(Move.getSb(blindsPlayer, blind.getValue()));
						break;
					}
				}
				return nodeFor(hand, abstractor);
			}
			return findBetNodeMatchOrCreate(hand, abstractor);
		}
		return null;
	}

	private NLBetTreeNode<PlayerId> findShowdownMatchOrCreate(@NonNull final NLHandRounds<PlayerId> hand) {
		final int index = showdownNodesList.size();
		final NLBetTreeNode<PlayerId> tmpNode = new NLBetTreeNode<PlayerId>(hand,
				Collections.<Move<PlayerId>, NLBetTreeNode<PlayerId>> emptyMap(), index);
		for (int i = 0; i < index; i++) {
			final NLBetTreeNode<PlayerId> node = showdownNodesList.get(i);
			if (node.equalsForShowdown(tmpNode)) {
				return node;
			}
		}
		showdownNodesList.add(tmpNode);
		return tmpNode;
	}

	private NLBetTreeNode<PlayerId> findNoShowdownMatchOrCreate(@NonNull final NLHandRounds<PlayerId> hand) {
		final int index = noShowdownNodesList.size();
		final NLBetTreeNode<PlayerId> tmpNode = new NLBetTreeNode<PlayerId>(hand,
				Collections.<Move<PlayerId>, NLBetTreeNode<PlayerId>> emptyMap(), index);
		for (int i = 0; i < index; i++) {
			final NLBetTreeNode<PlayerId> node = noShowdownNodesList.get(i);
			if (node.equalsForNoShowdown(tmpNode)) {
				return node;
			}
		}
		noShowdownNodesList.add(tmpNode);
		return tmpNode;
	}

	private NLBetTreeNode<PlayerId> findBetNodeMatchOrCreate(@NonNull final NLHandRounds<PlayerId> hand,
			final NLBetTreeAbstractor<PlayerId> abstractor) {

		final List<Move<PlayerId>> moves = hand.getBetMoves(hand.getBetRoundIndex());
		boolean startingNode = false;
		if (moves.isEmpty()) {
			startingNode = true;
			if (!perfectRecall) {
				final List<NLBetTreeNode<PlayerId>> startingNodes = betRoundsFirstNodesList
						.get(hand.getBetRoundIndex());
				for (final NLBetTreeNode<PlayerId> node : startingNodes) {
					if (node.samePlayersData(hand)) {
						return node;
					}
				}
			}
		}
		final List<Move<PlayerId>> nextMoves = abstractor.movesForHand(hand);
		// We use a linked hash map to keep the insertion order on the keys
		final Map<Move<PlayerId>, NLBetTreeNode<PlayerId>> children = new LinkedHashMap<>();
		for (Move<PlayerId> move : nextMoves) {
			final NLHandRounds<PlayerId> newHand = hand.clone();
			checkState(newHand.doMove(move), "Move %s seems invalid", move);
			children.put(move, nodeFor(newHand, abstractor));
		}
		final int betRound = hand.getBetRoundIndex();
		final int index = betRoundsNodesList.get(betRound).size();
		final NLBetTreeNode<PlayerId> node = new NLBetTreeNode<>(hand, children, index);
		betRoundsNodesList.get(betRound).add(node);
		if (startingNode) {
			betRoundsFirstNodesList.get(betRound).add(node);
		}
		return node;
	}

	@SuppressWarnings("unchecked")
	private static <PlayerId> NLBetTreeNode<PlayerId>[] toArray(List<NLBetTreeNode<PlayerId>> list) {
		return list.toArray(new NLBetTreeNode[list.size()]);
	}

	@SuppressWarnings("unchecked")
	private static <PlayerId> NLBetTreeNode<PlayerId>[][] toDoubleArray(List<List<NLBetTreeNode<PlayerId>>> list) {
		final NLBetTreeNode<PlayerId>[][] res = new NLBetTreeNode[list.size()][];
		int i = 0;
		for (List<NLBetTreeNode<PlayerId>> subList : list) {
			res[i++] = toArray(subList);
		}
		return res;
	}

	public void walk(final NLBetTreeWalker<PlayerId> walker) {
		walkRec(walker, rootNode, new ArrayList<NLBetTreeNode<PlayerId>>());
	}

	private static <PlayerId> void walkRec(final NLBetTreeWalker<PlayerId> walker, final NLBetTreeNode<PlayerId> node,
			final List<NLBetTreeNode<PlayerId>> parents) {
		walker.handleCurrentNode(node, parents);
		parents.add(node);
		for (final NLBetTreeNode<PlayerId> child : node.getChildren().values()) {
			walkRec(walker, child, parents);
		}
		parents.remove(node);
	}
}
