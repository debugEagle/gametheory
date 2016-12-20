package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;

import lombok.Getter;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandRounds;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.SharedPot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundState;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundType;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayerData;

public class NLBetTreeNode<PlayerId> {

	public static final int NO_PLAYER_INDEX = -1;

	@Getter
	private final NLHandRounds<PlayerId> hand;
	@Getter
	private final Map<Move<PlayerId>, NLBetTreeNode<PlayerId>> children;
	@Getter
	private final List<Move<PlayerId>> orderedMoves;

	public final boolean isRoundFirstNode;
	public final int index;
	public final int playerIndex;
	public final int nbChildren;
	public final int betRoundIndex;
	public final NLBetTreeNode<PlayerId> orderedChildren[];
	public final RoundState roundState;

	@SuppressWarnings("unchecked")
	public NLBetTreeNode(final NLHandRounds<PlayerId> hand, final Map<Move<PlayerId>, NLBetTreeNode<PlayerId>> children,
			int index) {
		this.isRoundFirstNode = hand.getBetMoves(hand.getBetRoundIndex()).isEmpty();
		this.index = index;
		final List<PlayerId> players = hand.orderedPlayers();
		this.hand = hand;
		this.children = children;
		final List<Move<PlayerId>> moves = new ArrayList<>();
		moves.addAll(children.keySet());
		orderedMoves = Collections.unmodifiableList(moves);
		nbChildren = orderedMoves.size();
		orderedChildren = new NLBetTreeNode[nbChildren];
		int i = 0;
		for (Move<PlayerId> move : moves) {
			orderedChildren[i] = children.get(move);
			i++;
		}
		roundState = hand.getRoundState();
		betRoundIndex = hand.getBetRoundIndex();
		if (roundState == RoundState.END_NO_SHOWDOWN) {
			playerIndex = players.indexOf(hand.getNoShowdownWinningPlayer());
		} else if (roundState == RoundState.WAITING_MOVE && hand.getRoundType() == RoundType.BETS) {
			playerIndex = players.indexOf(hand.getBettingPlayer());
		} else {
			playerIndex = NO_PLAYER_INDEX;
		}
	}

	public boolean equalsForShowdown(NLBetTreeNode<PlayerId> node) {
		if (roundState != RoundState.SHOWDOWN || node.roundState != RoundState.SHOWDOWN) {
			return false;
		}
		final List<Pot<PlayerId>> pots = node.hand.getCurrentPots();
		final List<Pot<PlayerId>> pots1 = hand.getCurrentPots();
		if (pots1.size() != pots.size()) {
			return false;
		}
		for (final Pot<PlayerId> pot : pots) {
			if (!pots1.contains(pot)) {
				return false;
			}
		}
		return true;
	}

	public boolean equalsForNoShowdown(NLBetTreeNode<PlayerId> node) {
		if (roundState != RoundState.END_NO_SHOWDOWN || node.roundState != RoundState.END_NO_SHOWDOWN) {
			return false;
		}
		if (node.playerIndex != playerIndex) {
			return false;
		}
		final Optional<List<SharedPot<PlayerId>>> optPots = node.hand.getSharedPots();
		final Optional<List<SharedPot<PlayerId>>> optPots1 = hand.getSharedPots();
		if (!optPots.isPresent() || !optPots1.isPresent()) {
			return false;
		}
		final List<SharedPot<PlayerId>> pots = optPots.get();
		final List<SharedPot<PlayerId>> pots1 = optPots1.get();
		if (pots1.size() != pots.size()) {
			return false;
		}
		for (final SharedPot<PlayerId> pot : pots) {
			if (!pots1.contains(pot)) {
				return false;
			}
		}
		return true;
	}

	public boolean samePlayersData(NLHandRounds<PlayerId> hand) {
		final List<PlayerData<PlayerId>> p1 = hand.getPlayersData();
		final List<PlayerData<PlayerId>> p2 = this.hand.getPlayersData();
		if (p1.size() != p2.size()) {
			return false;
		}
		for (final PlayerData<PlayerId> data : p1) {
			if (!p2.contains(data)) {
				return false;
			}
		}
		return true;
	}
}
