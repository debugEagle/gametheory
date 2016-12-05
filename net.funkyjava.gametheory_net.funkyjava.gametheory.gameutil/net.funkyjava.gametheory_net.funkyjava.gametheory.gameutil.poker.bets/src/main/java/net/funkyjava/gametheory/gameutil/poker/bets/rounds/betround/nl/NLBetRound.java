/**
 * 
 */
package net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.nl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import lombok.NonNull;
import lombok.ToString;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BetRoundSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundState;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetChoice;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetRange;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.CallValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.RaiseRange;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayerData;

/**
 * No-limit bet round state machine
 * 
 * @author Pierre Mardon
 * 
 */
@ToString
public class NLBetRound<PlayerId> implements Cloneable {

	private final BetRoundSpec<PlayerId> spec;
	private final List<PlayerData<PlayerId>> playersData;
	private final boolean[] inHand;
	private final boolean[] played;
	private final int[] stacks;
	private final int[] bets;
	private final int[] playersBetSubRound;
	private final int bigBlind;
	private final int nbPlayers;
	private int player;
	private int highestBet, lastRaise;
	private final int firstBetSubRound;
	private int betSubRound;
	private RoundState state;
	private final List<Move<PlayerId>> seq = new LinkedList<>();

	/**
	 * Constructor
	 * 
	 * @param startRoundData
	 *            data to start the bet round
	 */
	public NLBetRound(@NonNull final List<PlayerData<PlayerId>> playersData, @NonNull BetRoundSpec<PlayerId> spec) {
		this.spec = spec;
		seq.clear();
		this.playersData = playersData;
		nbPlayers = playersData.size();
		inHand = new boolean[nbPlayers];
		stacks = new int[nbPlayers];
		bets = new int[nbPlayers];
		playersBetSubRound = new int[nbPlayers];
		played = new boolean[nbPlayers];
		bigBlind = spec.getBigBlindValue();
		checkArgument(bigBlind > 0, "Big blind value should be > 0");
		int firstPlayerIndex = -1;
		int inHandPl = 0;
		for (int i = 0; i < nbPlayers; i++) {
			final PlayerData<PlayerId> pData = playersData.get(i);
			checkArgument(pData.isInHand(), "First player must be in hand");
			checkArgument(pData.getStack() > 0, "In hand player %s invalid stack %s ", i, pData.getStack());
			if (pData.getPlayerId() == spec.getFirstPlayerId()) {
				checkArgument(firstPlayerIndex < 0, "Multiple players considered as first player");
				firstPlayerIndex = i;
			}
			if (pData.isInHand()) {
				inHandPl++;
			}
			inHand[i] = pData.isInHand();
			stacks[i] = pData.getStack();
			checkArgument(stacks[i] >= 0, "Player %s has a negative stack", pData.getPlayerId());
			checkArgument(bets[i] >= 0, "Blinds for player %s are negative", pData.getPlayerId());
			checkArgument(bigBlind >= bets[i], "Player %s has a bet > bigblind at the beginning of the round",
					pData.getPlayerId());
			checkArgument(inHand[i] || bets[i] == 0,
					"Player %s cannot have bets at the beginning of the round as he is not in hand", i);
		}
		checkArgument(inHandPl > 1, "Not enough players in hand");
		checkArgument(firstPlayerIndex >= 0, "No player considered as first player");
		checkArgument(stacks[firstPlayerIndex] > 0, "First player must not be all-in");
		highestBet = 0;
		for (int i = 0; i < nbPlayers; i++)
			if (bets[i] > highestBet)
				highestBet = bets[i];
		firstBetSubRound = betSubRound = highestBet > 0 ? 1 : 0;
		if (highestBet > 0)
			highestBet = Math.max(highestBet, spec.getBigBlindValue());
		checkArgument(inHandPl > 1 || bets[firstPlayerIndex] < highestBet, "It seems like a direct showdown...");
		player = firstPlayerIndex - 1;
		if (player < 0)
			player += nbPlayers;
		goToNextState();
	}

	private NLBetRound(NLBetRound<PlayerId> source) {
		this.spec = source.spec;
		this.playersData = source.playersData;
		this.seq.addAll(source.seq);
		this.bets = source.bets.clone();
		this.betSubRound = source.betSubRound;
		this.firstBetSubRound = source.firstBetSubRound;
		this.bigBlind = source.bigBlind;
		this.highestBet = source.highestBet;
		this.inHand = source.inHand.clone();
		this.lastRaise = source.lastRaise;
		this.nbPlayers = source.nbPlayers;
		this.played = source.played.clone();
		this.player = source.player;
		this.playersBetSubRound = source.playersBetSubRound.clone();
		this.stacks = source.stacks.clone();
		this.state = source.state;
	}

	/**
	 * Get the current {@link PlayerData}s
	 * 
	 * @return the players data
	 */
	public List<PlayerData<PlayerId>> getData() {
		final List<PlayerData<PlayerId>> res = new ArrayList<>();
		for (int i = 0; i < nbPlayers; i++) {
			res.add(new PlayerData<>(playersData.get(i).getPlayerId(), stacks[i], inHand[i], bets[i]));
		}

		return res;
	}

	/**
	 * Get the current {@link PlayerData}s with bets set to zero
	 * 
	 * @return the players data
	 */
	public List<PlayerData<PlayerId>> getBetZeroData() {
		final List<PlayerData<PlayerId>> res = new ArrayList<>();
		for (int i = 0; i < nbPlayers; i++) {
			res.add(new PlayerData<>(playersData.get(i).getPlayerId(), stacks[i], inHand[i], 0));
		}

		return res;
	}

	/**
	 * Get the number of performed moves in this round
	 * 
	 * @return the number of moves
	 */
	public int getNbMoves() {
		return seq.size();
	}

	/**
	 * Get the performed moves in this round
	 * 
	 * @return the moves
	 */
	public List<Move<PlayerId>> getMoves() {
		return Collections.unmodifiableList(seq);
	}

	/**
	 * Get the current state of the round
	 * 
	 * @return the current state of the round
	 */
	public RoundState getState() {
		return state;
	}

	/**
	 * Get the player expected to do a move
	 * 
	 * @return the active player
	 */
	public int getCurrentPlayer() {
		checkState(state == RoundState.WAITING_MOVE, "Wrong state %s to ask for active player", state);
		checkState(player >= 0 && player < nbPlayers, "Internal error : Invalid player index %s, nbPlayers ", player,
				nbPlayers);
		return player;
	}

	/**
	 * Get the raise range for the active player
	 * 
	 * @return active player's raise range
	 */
	public RaiseRange getRaiseRange() {
		checkState(state == RoundState.WAITING_MOVE, "Wrong state %s to ask for possible moves", state);
		int fullStack = bets[player] + stacks[player];
		if (fullStack <= highestBet || playersBetSubRound[player] == betSubRound)
			return RaiseRange.getNoRange();
		if (fullStack <= highestBet + lastRaise)
			return RaiseRange.getSingleton(bets[player], fullStack);
		return new RaiseRange(bets[player], highestBet + lastRaise, fullStack);
	}

	/**
	 * Get the call value for the active player
	 * 
	 * @return the call value
	 */
	public CallValue getCallValue() {
		checkState(state == RoundState.WAITING_MOVE, "Wrong state %s to ask for possible moves", state);
		int call = Math.min(stacks[player] + bets[player], highestBet);
		return new CallValue(call, call - bets[player]);
	}

	/**
	 * Get the bet range for the active player
	 * 
	 * @return the bet range
	 */
	public BetRange getBetRange() {
		if (betSubRound > 0)
			return BetRange.getNoRange();
		return new BetRange(Math.min(stacks[player], bigBlind), stacks[player]);
	}

	public BetRoundSpec<PlayerId> getSpec() {
		return spec;
	}

	/**
	 * Perform a move
	 * 
	 * @param m
	 *            the move to perform
	 */
	public void doMove(Move<PlayerId> m) {
		checkState(state == RoundState.WAITING_MOVE, "Round state is %s, cannot do any move.", state);
		final PlayerId playerId = playersData.get(player).getPlayerId();
		checkArgument(m.getPlayerId() == playerId, "Wrong player %s for this move, expected %s", m.getPlayerId(),
				playerId);
		int val = m.getValue();
		switch (m.getType()) {
		case BET:
			checkState(betSubRound == 0, "Can't bet, maybe you mean call or raise");
			checkState(bets[player] == 0, "This player has already betted");
			checkArgument(val >= bigBlind || val == stacks[player], "Incorrect value for player %s bet of %s, stack %s",
					player, val, stacks[player]);
			checkArgument(m.getOldBet() == bets[player]);
			doBet(val);
			break;
		case CALL:
			checkArgument(highestBet == val || (stacks[player] + bets[player] == val && val < highestBet),
					"Wrong call value %s", val);
			checkArgument(m.getOldBet() == bets[player]);
			doCall(val);
			break;
		case RAISE:
			RaiseRange raiseTo = getRaiseRange();
			checkState(raiseTo.exists(), "Player %s can't raise !", player);
			checkArgument(raiseTo.getMin() <= val && raiseTo.getMax() >= val,
					"Raise %s is invalid, expected between %s and %s", raiseTo.getMin(), raiseTo.getMax());
			checkArgument(m.getOldBet() == bets[player]);
			doRaise(val);
			break;
		case FOLD:
			inHand[player] = false;
			break;
		default:
			throw new IllegalArgumentException("Unauthorized move " + m);
		}
		seq.add(m);
		played[player] = true;
		goToNextState();
	}

	private boolean isFullRaise(int val) {
		return val >= highestBet + lastRaise;
	}

	private void doBet(int val) {
		stacks[player] -= val;
		bets[player] = val;
		betSubRound = 1;
		playersBetSubRound[player] = 1;
		lastRaise = highestBet = Math.max(val, bigBlind);
	}

	private void doRaise(int val) {
		stacks[player] -= val - bets[player];
		bets[player] = val;
		if (isFullRaise(val)) {
			playersBetSubRound[player] = ++betSubRound;
			lastRaise = val - highestBet;
			highestBet = val;
			return;
		}
		playersBetSubRound[player] = betSubRound;
		lastRaise += val - highestBet;
		highestBet = val;
	}

	private void doCall(int val) {
		playersBetSubRound[player] = betSubRound;
		stacks[player] -= val - bets[player];
		bets[player] = val;
	}

	private void goToNextState() {
		int p, i;
		int nextPlayer = -1;
		int nbInHand = 0;
		int nbNotAllIn = 0;
		int nbCanPlay = 0;
		for (i = 0; i < nbPlayers; i++) {
			p = (player + i + 1) % nbPlayers;
			if (!inHand[p])
				continue;
			nbInHand++;
			if (stacks[p] == 0)
				continue;
			nbNotAllIn++;
			if (playersBetSubRound[p] < betSubRound

					||

					(playersBetSubRound[p] == betSubRound && bets[p] < highestBet)

					||

					(!played[p]))

			{
				nbCanPlay++;
				if (nextPlayer < 0)
					nextPlayer = p;
			}
		}
		if (nbInHand == 1) {
			state = RoundState.END_NO_SHOWDOWN;
			return;
		}
		if (nbCanPlay > 0) {
			state = RoundState.WAITING_MOVE;
			player = nextPlayer;
			return;
		}
		if (nbNotAllIn <= 1) {
			state = RoundState.SHOWDOWN;
			return;
		}
		state = RoundState.NEXT_ROUND;
	}

	@Override
	public NLBetRound<PlayerId> clone() {
		return new NLBetRound<>(this);
	}

	/**
	 * Get the {@link BetChoice} of the active player
	 * 
	 * @return the bet choice
	 */
	public BetChoice getBetChoice() {
		checkState(state == RoundState.WAITING_MOVE, "Wrong state %s to ask for active player bet choice", state);
		return new BetChoice(getBetRange(), getCallValue(), getRaiseRange(), getCurrentPlayer());
	}
}
