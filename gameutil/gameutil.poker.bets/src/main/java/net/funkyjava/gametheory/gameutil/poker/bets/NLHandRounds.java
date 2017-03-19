/**
 * 
 */
package net.funkyjava.gametheory.gameutil.poker.bets;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.SharedPot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BetRoundSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundState;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.RoundType;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.anteround.AnteRound;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.anteround.AnteValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetChoice;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.nl.NLBetRound;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.blindsround.BlindValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.blindsround.BlindsRound;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.NoBetPlayerData;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayerData;

/**
 * 
 * State machine to walk a no-limit hold'em poker hand. Intends to be used in a
 * monothread way
 * 
 * @author Pierre Mardon
 * 
 */
@Slf4j
public class NLHandRounds<PlayerId> implements Cloneable {

	@Getter
	private final List<NoBetPlayerData<PlayerId>> initialPlayersData;
	@Getter
	private final List<PlayerId> orderedPlayers;
	private AnteRound<PlayerId> anteRound;
	private BlindsRound<PlayerId> blindsRound;
	private final NLBetRound<PlayerId> betRounds[];
	private final boolean hasAnte;
	private final boolean hasBlinds;
	private final boolean isCash;
	private final int roundOffset;
	@Getter
	private final int nbRounds;
	@Getter
	private final int nbBetRounds;
	private RoundType rType;
	private int round = -1;
	private final BlindsAnteSpec<PlayerId> blindsSpec;

	@SuppressWarnings("unchecked")
	private NLHandRounds(NLHandRounds<PlayerId> src) {
		this.orderedPlayers = src.orderedPlayers;
		this.initialPlayersData = src.initialPlayersData;
		anteRound = cloneOrNull(src.anteRound);
		blindsRound = cloneOrNull(src.blindsRound);
		betRounds = new NLBetRound[src.betRounds.length];
		for (int i = 0; i < src.betRounds.length; i++)
			betRounds[i] = cloneOrNull(src.betRounds[i]);
		hasAnte = src.hasAnte;
		hasBlinds = src.hasBlinds;
		isCash = src.isCash;
		roundOffset = src.roundOffset;
		nbRounds = src.nbRounds;
		nbBetRounds = src.nbBetRounds;
		rType = src.rType;
		round = src.round;
		blindsSpec = src.blindsSpec;
	}

	@SuppressWarnings("unchecked")
	private <C extends Object> C cloneOrNull(C obj) {
		if (obj == null)
			return null;
		try {
			return (C) obj.getClass().getMethod("clone").invoke(obj);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Constructor.
	 * 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public NLHandRounds(final List<NoBetPlayerData<PlayerId>> playersData, final BlindsAnteSpec<PlayerId> blindsSpec,
			final BetRoundSpec<PlayerId> betsSpec, final int nbBetRounds) {
		checkArgument(nbBetRounds > 0, "You must have at least one bet round");
		checkArgument(!blindsSpec.isEnableAnte() || blindsSpec.getAnteValue() > 0, "Ante value {} is invalid",
				blindsSpec.getAnteValue());
		this.initialPlayersData = Collections.unmodifiableList(playersData);
		this.orderedPlayers = new ArrayList<>();
		final int nbPlayers = playersData.size();
		for (int i = 0; i < nbPlayers; i++) {
			orderedPlayers.add(playersData.get(i).getPlayerId());
		}
		hasAnte = blindsSpec.isEnableAnte();
		hasBlinds = blindsSpec.isEnableBlinds();
		this.isCash = blindsSpec.isCash();
		this.blindsSpec = blindsSpec;
		roundOffset = (hasAnte ? 1 : 0) + (hasBlinds ? 1 : 0);
		this.nbBetRounds = nbBetRounds;
		nbRounds = roundOffset + nbBetRounds;

		betRounds = new NLBetRound[nbBetRounds];
		if (hasAnte) {
			this.anteRound = new AnteRound<PlayerId>(this.initialPlayersData, blindsSpec);
			rType = RoundType.ANTE;
		} else if (hasBlinds) {
			this.blindsRound = new BlindsRound<PlayerId>(this.initialPlayersData, blindsSpec);
			rType = RoundType.BLINDS;
		} else {
			final List<PlayerData<PlayerId>> data = new ArrayList<>();
			for (NoBetPlayerData<PlayerId> pData : playersData) {
				data.add(pData.getPlayerData());
			}
			this.betRounds[0] = new NLBetRound<PlayerId>(data, betsSpec);
			rType = RoundType.BETS;
		}
		round = 0;
	}

	private boolean isAnteRound() {
		return hasAnte && round == 0;
	}

	private boolean isBlindsRound() {
		return hasBlinds && ((hasAnte && round == 1) || (!hasAnte && round == 0));
	}

	private boolean isBetRound() {
		return round - roundOffset >= 0;
	}

	/**
	 * Must be called only when in a bet round and the round state is
	 * {@link RoundState#WAITING_MOVE}
	 * 
	 * @return the index of the player expected to do the next move
	 */
	public PlayerId getBettingPlayer() {
		checkArgument(isBetRound(), "Doesn't seem to be in a bet round");
		checkArgument(betRounds[round - roundOffset].getState() == RoundState.WAITING_MOVE,
				"Bet round is in wrong state %s expected %s", betRounds[round - roundOffset].getState(),
				RoundState.WAITING_MOVE);
		return betRounds[round - roundOffset].getCurrentPlayer();
	}

	/**
	 * Must be called only when in a bet round and the round state is
	 * {@link RoundState#WAITING_MOVE}
	 * 
	 * @return the {@link BetChoice} describing possible player's moves
	 */
	public BetChoice<PlayerId> getBetChoice() {
		checkArgument(isBetRound(), "Doesn't seem to be in a bet round");
		checkArgument(betRounds[round - roundOffset].getState() == RoundState.WAITING_MOVE,
				"Bet round is in wrong state %s expected %s", betRounds[round - roundOffset].getState(),
				RoundState.WAITING_MOVE);
		return betRounds[round - roundOffset].getBetChoice();
	}

	/**
	 * Get current round's {@link PlayerData}s containing the stacks, bets and
	 * "in-hand" state of each player
	 * 
	 * @return current {@link PlayerData}s
	 */
	public List<PlayerData<PlayerId>> getPlayersData() {
		switch (rType) {
		case ANTE:
			return anteRound.getData();
		case BETS:
			return betRounds[round - roundOffset].getData();
		case BLINDS:
			return blindsRound.getData();
		default:
			break;
		}
		return null;
	}

	public List<PlayerId> orderedPlayers() {
		final List<PlayerId> res = new ArrayList<>();
		for (final PlayerData<PlayerId> data : getPlayersData()) {
			res.add(data.getPlayerId());
		}
		return res;
	}

	/**
	 * Do a move. Only valid moves according to the state will be performed.
	 * 
	 * @param move
	 *            the move to perform
	 * @return true when the move was validated and done
	 */
	public boolean doMove(Move<PlayerId> move) {
		if (isAnteRound())
			return doAnteMove(move);
		if (isBlindsRound())
			return doBlindsMove(move);
		if (isBetRound())
			return doBetMove(move);
		log.warn("Can't do move {}, seems like there's no hand going on", move);
		return false;
	}

	private boolean doBetMove(Move<PlayerId> move) {
		switch (move.getType()) {
		case BET:
		case CALL:
		case FOLD:
		case RAISE:
			try {
				betRounds[round - roundOffset].doMove(move);
				return true;
			} catch (Exception e) {
				log.warn("Invalid bet-round move {}", move, e);
			}
			return false;
		default:
			log.warn("Invalid move in round {} ({}) : {}", round, getRoundType(), move);
			return false;
		}
	}

	private boolean doBlindsMove(Move<PlayerId> move) {
		switch (move.getType()) {
		case NO_BLIND:
			if (!blindsSpec.isCash()) {
				log.warn("Can't refuse to pay blinds in CG");
				return false;
			}
		case SB:
		case BB:
			try {
				blindsRound.doMove(move);
				return true;
			} catch (Exception e) {
				log.warn("Invalid blinds move {}", move, e);
			}
			return false;
		default:
			log.warn("Invalid move in round {} ({}) : {}", round, getRoundType(), move);
			return false;
		}
	}

	private boolean doAnteMove(Move<PlayerId> move) {
		switch (move.getType()) {
		case NO_ANTE:
			checkState(isCash, "Move %s is invalid because player can only refuse to pay ante in cash game", move);
		case ANTE:
			try {
				anteRound.doMove(move);
				return true;
			} catch (Exception e) {
				log.warn("Invalid ante move {}", move, e);
			}
			return false;
		default:
			log.warn("Invalid move in round {} ({}) : {}", round, getRoundType(), move);
			return false;
		}
	}

	/**
	 * Get the moves of the ante round
	 * 
	 * @return the ordered list of moves
	 */
	public List<Move<PlayerId>> getAnteMoves() {
		return anteRound == null ? Collections.<Move<PlayerId>> emptyList() : anteRound.getMoves();
	}

	/**
	 * Get the blinds moves.
	 * 
	 * @return the moves performed for paying blinds
	 */
	public List<Move<PlayerId>> getBlindsMoves() {
		return blindsRound == null ? Collections.<Move<PlayerId>> emptyList() : blindsRound.getMoves();
	}

	/**
	 * Get the bet moves of a particular bet round
	 * 
	 * @param betRoundIndex
	 *            the index of the bet round (0 = preflop,..., 3 = river)
	 * @return the list of moves performed during the target bet round
	 */
	public List<Move<PlayerId>> getBetMoves(int betRoundIndex) {
		return (betRoundIndex < 0 || betRoundIndex >= nbBetRounds || betRounds[betRoundIndex] == null)
				? Collections.<Move<PlayerId>> emptyList() : betRounds[betRoundIndex].getMoves();
	}

	public List<Move<PlayerId>> getCurrentRoundBetMoves() {
		return getBetMoves(round - roundOffset);
	}

	/**
	 * Get the bet moves of a all bet rounds
	 * 
	 * @return the list of moves performed during all bet rounds. Each sublist
	 *         represents a bet round.
	 */
	public List<List<Move<PlayerId>>> getBetMoves() {
		List<List<Move<PlayerId>>> res = new LinkedList<>();
		List<Move<PlayerId>> list;
		for (int i = 0; i < nbBetRounds; i++) {
			if ((list = getBetMoves(i)).isEmpty())
				break;
			res.add(list);
		}
		return res;
	}

	/**
	 * Check if all players payed the antes
	 * 
	 * @return false when not in ante round or not all players payed the antes,
	 *         true otherwise
	 */
	public boolean allAntePayed() {
		if (!isAnteRound())
			return false;
		return anteRound.finished();
	}

	/**
	 * Get missing ante payments
	 * 
	 * @return a map with keys = players, and values = ante values
	 */
	public Map<PlayerId, AnteValue> getMissingAnte() {
		if (!isAnteRound())
			return new HashMap<>();
		Map<PlayerId, AnteValue> res = new TreeMap<>();
		for (PlayerId p : anteRound.getMissingAntePlayers())
			res.put(p, anteRound.getAnteValueForPlayer(p));
		return res;
	}

	/**
	 * Get missing blinds payments
	 * 
	 * @return a map with keys = players indexes, and values = blinds values
	 */
	public Map<PlayerId, BlindValue> getMissingBlinds() {
		if (!isBlindsRound())
			return new HashMap<>();
		Map<PlayerId, BlindValue> res = new TreeMap<>();
		for (PlayerId p : blindsRound.getMissingEnteringBbPlayers())
			res.put(p, blindsRound.getBlindValueForPlayer(p));
		if (!res.containsKey(blindsSpec.getBbPlayer()) && !blindsRound.hasBbPayed())
			res.put(blindsSpec.getBbPlayer(), blindsRound.getBlindValueForPlayer(blindsSpec.getBbPlayer()));
		if (blindsSpec.getSbPlayer() != null && !blindsRound.hasSbPayed())
			res.put(blindsSpec.getSbPlayer(), blindsRound.getBlindValueForPlayer(blindsSpec.getSbPlayer()));
		return res;
	}

	/**
	 * Make ante payments expire.
	 * 
	 * @return true when in cash game and in ante round, false otherwise
	 */
	public boolean doAnteExpiration() {
		if (!isCash) {
			log.warn("You cannot make ante payment expire when not in cash game");
			return false;
		}
		if (!isAnteRound()) {
			log.warn("Not in ante round, current round type {}, round index {}", rType, round);
			return false;
		}
		anteRound.expiration();
		return true;
	}

	/**
	 * Make blinds payments expire.
	 * 
	 * @return true when in cash game and in blinds round, false otherwise
	 */
	public boolean doBlindsExpiration() {
		if (!isCash) {
			log.warn("You cannot make blinds payment expire when not in cash game");
			return false;
		}
		if (!isBlindsRound()) {
			log.warn("Not in blinds round, current round type {}, round index {}", rType, round);
			return false;
		}
		blindsRound.expiration();
		return true;
	}

	/**
	 * Go to the next round after antes.
	 * 
	 * @return true when in ante round and it is finished
	 */
	public boolean nextRoundAfterAnte() {
		if (!isAnteRound()) {
			log.warn("Not in ante round, current round type {}, round index {}", rType, round);
			return false;
		}
		if (anteRound.getState() != RoundState.NEXT_ROUND) {
			log.warn("Wrong ante round state to go to next round {}, expected {}", anteRound.getState(),
					RoundState.NEXT_ROUND);
			return false;
		}
		if (hasBlinds) {
			try {
				blindsRound = new BlindsRound<PlayerId>(anteRound.getNoBetData(), blindsSpec);
			} catch (Exception e) {
				log.warn("Can't start blinds round", e);
				return false;
			}
			round = 1;
			rType = RoundType.BLINDS;
			return true;
		}
		final List<PlayerData<PlayerId>> playersData = anteRound.getBetZeroData();
		PlayerId firstPlayer = firstInHandPlayer(playersData);
		return nextBetRound(playersData, new BetRoundSpec<>(firstPlayer, blindsSpec.getBbValue()));
	}

	private PlayerId firstInHandPlayer(final List<PlayerData<PlayerId>> playersData) {
		final int size = playersData.size();
		for (int i = 0; i < size; i++) {
			if (playersData.get(i).isInHand()) {
				return playersData.get(i).getPlayerId();
			}
		}
		throw new IllegalStateException("Couldn't find next player to play");
	}

	private PlayerId firstInHandPlayer(final List<PlayerData<PlayerId>> playersData, final PlayerId startPlayer) {
		final int size = playersData.size();
		for (int i = 0; i < size; i++) {
			if (playersData.get(i).isInHand()) {
				return playersData.get(i).getPlayerId();
			}
		}
		throw new IllegalStateException("Couldn't find next player to play");
	}

	/**
	 * Go to the next round after blinds.
	 * 
	 * @return true when in blinds round and it is finished
	 */
	public boolean betRoundAfterBlinds() {
		if (!isBlindsRound()) {
			log.warn("Not in blinds round, current round type {}, round index {}", rType, round);
			return false;
		}
		if (blindsRound.getState() != RoundState.NEXT_ROUND) {
			log.warn("Wrong blinds round state to go to bet round {}, expected {}", blindsRound.getState(),
					RoundState.NEXT_ROUND);
			return false;
		}
		final List<PlayerData<PlayerId>> playersData = blindsRound.getData();
		final PlayerId firstPlayer = nextInHandPlayerAfter(blindsSpec.getBbPlayer(), playersData);
		return nextBetRound(playersData, new BetRoundSpec<>(firstPlayer, blindsSpec.getBbValue()));
	}

	private PlayerId nextInHandPlayerAfter(final PlayerId startingPlayerId,
			final List<PlayerData<PlayerId>> playersData) {
		int p = 0;
		final int size = playersData.size();
		while (p < size && playersData.get(p).getPlayerId() != startingPlayerId) {
			p++;
		}
		checkState(p != size, "Couldn't find player in playersData");
		p++;
		if (p == size) {
			p = 0;
		}
		for (int i = 0; i < size; i++) {
			if (playersData.get((i + p) % size).isInHand()) {
				return playersData.get((i + p) % size).getPlayerId();
			}
		}
		throw new IllegalStateException("Couldn't find next player to play");
	}

	/**
	 * Go to the next bet round
	 * 
	 * @return true when in bet round, the current one is finished and there is
	 *         a next bet round
	 */
	public boolean nextBetRound() {
		if (!isBetRound()) {
			log.warn("Not in bets round, current round type {}, round index {}", rType, round);
			return false;
		}
		if (betRounds[round - roundOffset].getState() != RoundState.NEXT_ROUND) {
			log.warn("Wrong bets round state to go to next bet round {}, expected {}", blindsRound.getState(),
					RoundState.NEXT_ROUND);
			return false;
		}
		if (round == nbRounds - 1) {
			log.warn("There is no next bet round, current round index {}, bet round index {}", round,
					round - roundOffset);
			return false;
		}
		final NLBetRound<PlayerId> previousRound = betRounds[round - roundOffset];
		final PlayerId lastFirstPlayer = previousRound.getSpec().getFirstPlayerId();
		final List<PlayerData<PlayerId>> pData = previousRound.getBetZeroData();
		final PlayerId firstPlayer = firstInHandPlayer(pData, lastFirstPlayer);
		return nextBetRound(previousRound.getBetZeroData(),
				new BetRoundSpec<>(firstPlayer, previousRound.getSpec().getBigBlindValue()));
	}

	private boolean nextBetRound(final List<PlayerData<PlayerId>> playersData, final BetRoundSpec<PlayerId> spec) {
		try {
			betRounds[round + 1 - roundOffset] = new NLBetRound<PlayerId>(playersData, spec);
		} catch (Exception e) {
			log.warn("Can't start next bet round", e);
			return false;
		}
		round++;
		rType = RoundType.BETS;
		return true;
	}

	/**
	 * Get the current {@link RoundType}
	 * 
	 * @return the current {@link RoundType}
	 */
	public RoundType getRoundType() {
		return rType;
	}

	/**
	 * Get the current {@link RoundState}
	 * 
	 * @return the current {@link RoundState}
	 */
	public RoundState getRoundState() {
		if (isAnteRound())
			return anteRound.getState();
		if (isBlindsRound())
			return blindsRound.getState();
		if (round == nbRounds - 1 && betRounds[round - roundOffset].getState() == RoundState.NEXT_ROUND)
			return RoundState.SHOWDOWN;
		return betRounds[round - roundOffset].getState();
	}

	/**
	 * Get the current round index. Ante and blinds each count as a round
	 * 
	 * @return the round index
	 */
	public int getRoundIndex() {
		return round;
	}

	/**
	 * Get the current bet round index. Ante and blinds each count as a round so
	 * it will be negative when calling before the first bet round.
	 * 
	 * @return the bet round index
	 */
	public int getBetRoundIndex() {
		return round - roundOffset;
	}

	public PlayerId getNoShowdownWinningPlayer() {
		if (getRoundState() != RoundState.END_NO_SHOWDOWN) {
			return null;
		}
		switch (getRoundType()) {
		case BETS:
			return betRounds[round - roundOffset].getNoShowdownWinningPlayer();
		case BLINDS:
			return blindsRound.getNoShowdownWinningPlayer();
		default:
			break;
		}
		return null;
	}

	public List<PlayerId> getShowdownPlayers() {
		if (getRoundState() != RoundState.SHOWDOWN) {
			return Collections.emptyList();
		}
		switch (getRoundType()) {
		case BETS:
			return betRounds[round - roundOffset].getShowdownPlayers();
		case BLINDS:
			return blindsRound.getShowdownPlayers();
		default:
			break;
		}
		return Collections.emptyList();
	}

	/**
	 * Build and get the list of pots for finished rounds. Players that were
	 * always in the hand (didn't fold) at the end of each round are added to
	 * the list of players for each pot. So at the time you call this method,
	 * there may be players that are no longer in hand in previous rounds pots
	 * players.
	 * 
	 * @return the list of pots
	 */
	public List<Pot<PlayerId>> getCurrentPots() {
		final List<Pot<PlayerId>> pots = new LinkedList<>();
		List<PlayerData<PlayerId>> data;
		if (hasAnte && round > 0) {
			data = anteRound.getData();
			pots.addAll(Pot.getPots(data));
		}
		for (int i = 0; i <= round - roundOffset; i++) {
			switch (betRounds[i].getState()) {
			case CANCELED:
				log.warn("Last round was canceled");
				return pots;
			case END_NO_SHOWDOWN:
			case NEXT_ROUND:
			case SHOWDOWN:
				data = betRounds[i].getData();
				if (pots.isEmpty())
					pots.addAll(Pot.getPots(data));
				else
					pots.addAll(Pot.getPots(pots.get(pots.size() - 1), data));
				break;
			case WAITING_MOVE:
				// Don't add last round pots as it's not finished
				break;
			}
		}
		return pots;
	}

	public int getTotalPotsValue() {
		int res = 0;
		for (Pot<PlayerId> pot : getCurrentPots()) {
			res += pot.getValue();
		}
		return res;
	}

	/**
	 * Builds and get the shared pots at the end of a hand with no showdown.
	 * There's only one winner expected.
	 * 
	 * @return The list of shared pots or {@link Optional#absent()} when not in
	 *         a valid state.
	 */
	public Optional<List<SharedPot<PlayerId>>> getSharedPots() {
		final List<SharedPot<PlayerId>> res = new LinkedList<>();
		switch (getRoundState()) {
		case CANCELED:
			log.warn("Current round was canceled, can't share pots");
			return Optional.absent();
		case WAITING_MOVE:
			log.warn("Current round isn't finished, can't share pots");
			return Optional.absent();
		case SHOWDOWN:
			log.warn("For showdown, can't share pots without the ordered winners");
			return Optional.absent();
		case NEXT_ROUND:
			log.warn("Can't share pots, there is another round to play");
			return Optional.absent();
		case END_NO_SHOWDOWN:
			break;
		}
		final List<PlayerData<PlayerId>> data = betRounds[round - roundOffset].getData();
		final int nbPlayers = data.size();
		PlayerId winner = null;
		for (int i = 0; i < nbPlayers; i++)
			if (data.get(i).isInHand())
				if (winner != null) {
					log.error("There is more than one player in hand, this shouldn't happen when in state {}",
							getRoundState());
					return Optional.absent();
				} else
					winner = data.get(i).getPlayerId();
		if (winner == null) {
			log.error("Did not find any player in hand");
			return Optional.absent();
		}
		final List<Pot<PlayerId>> pots = getCurrentPots();
		@SuppressWarnings("unchecked")
		final ArrayList<PlayerId> winners = Lists.newArrayList(winner);
		for (int i = 0; i < pots.size(); i++)
			res.add(SharedPot.sharePot(pots.get(i), winners, winner));
		return Optional.of(res);
	}

	/**
	 * Builds and get the shared pots at the end of a hand with or without a
	 * showdown.
	 * 
	 * @param orderedWinnersPartition
	 *            the partitions of winners
	 * @return The list of shared pots or {@link Optional#absent()} when not in
	 *         a valid state.
	 */
	public Optional<List<SharedPot<PlayerId>>> getSharedPots(final List<List<PlayerId>> orderedWinnersPartition) {

		switch (getRoundState()) {
		case CANCELED:
			log.warn("Current round was canceled, can't share pots");
			return Optional.absent();
		case WAITING_MOVE:
			log.warn("Current round isn't finished, can't share pots");
			return Optional.absent();
		case NEXT_ROUND:
			log.warn("Can't share pots, there is another round to play");
			return Optional.absent();
		case SHOWDOWN:
		case END_NO_SHOWDOWN:
			break;
		}
		final List<List<PlayerId>> filteredWinners = new LinkedList<>();
		final List<PlayerData<PlayerId>> data = betRounds[round - roundOffset].getData();
		final int nbPlayers = data.size();
		final Map<PlayerId, PlayerData<PlayerId>> perIdData = new HashMap<>();
		for (int i = 0; i < nbPlayers; i++) {
			final PlayerData<PlayerId> pData = data.get(i);
			perIdData.put(pData.getPlayerId(), pData);
		}
		for (int i = 0; i < orderedWinnersPartition.size(); i++) {
			List<PlayerId> winners = new LinkedList<>();
			for (PlayerId p : orderedWinnersPartition.get(i))
				if (perIdData.get(p).isInHand())
					winners.add(p);
			filteredWinners.add(winners);
		}
		if (filteredWinners.isEmpty()) {
			log.error("Can't find winners from the provided players partition {}", orderedWinnersPartition);
			return Optional.absent();
		}
		missingPlayerLoop: for (int i = 0; i < nbPlayers; i++) {
			final PlayerData<PlayerId> pData = data.get(i);
			if (pData.isInHand()) {
				final PlayerId pId = pData.getPlayerId();
				for (List<PlayerId> winners : filteredWinners)
					if (winners.contains(pId))
						continue missingPlayerLoop;
				log.error("Can't find in hand player {} from the provided players partition {}", i,
						orderedWinnersPartition);
				return Optional.absent();
			}
		}
		final List<SharedPot<PlayerId>> res = new LinkedList<>();
		potsLoop: for (Pot<PlayerId> pot : getCurrentPots()) {
			for (List<PlayerId> winners : filteredWinners)
				if (joinNotEmpty(winners, pot.getPlayers())) {
					// TODO odd chips winner explicit declaration ??
					res.add(SharedPot.sharePot(pot, winners, winners.get(0)));
					continue potsLoop;
				}
			log.error("Didn't find winners for pot {} with provided winners partition {}, filtered winners {}", pot,
					orderedWinnersPartition, filteredWinners);
			return Optional.absent();
		}
		return Optional.of(res);
	}

	private static <Id> boolean joinNotEmpty(List<Id> list1, List<Id> list2) {
		for (Id i : list1)
			if (list2.contains(i))
				return true;
		return false;
	}

	@Override
	public NLHandRounds<PlayerId> clone() {
		return new NLHandRounds<>(this);
	}

}
