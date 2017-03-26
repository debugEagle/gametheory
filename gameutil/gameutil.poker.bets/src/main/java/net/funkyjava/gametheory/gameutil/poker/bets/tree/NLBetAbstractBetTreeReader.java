package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import lombok.AllArgsConstructor;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandRounds;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayerData;

// Check : size of the previous bet
public class NLBetAbstractBetTreeReader<PlayerId> implements NLBetTreeAbstractor<PlayerId> {

	private static enum ParsedMoveType {
		FOLD, CALL, MIN_BET_RAISE, ALL_IN, NUMERIC, POT_MULTIPLIER, MAX_BET_MULTIPLIER
	}

	@AllArgsConstructor
	private static final class ParsedMove {

		private final ParsedMoveType type;
		private final int numericValue;
		private final double multiplier;

		private static final ParsedMove parse(String val) {
			final String lower = val.toLowerCase();
			switch (lower) {
			case "c":
			case "call":
			case "check":
				return new ParsedMove(ParsedMoveType.CALL, 0, 0);
			case "mb":
			case "minbet":
			case "mr":
			case "minraise":
			case "min":
				return new ParsedMove(ParsedMoveType.MIN_BET_RAISE, 0, 0);
			case "allin":
			case "ai":
			case "push":
			case "p":
			case "shove":
			case "s":
				return new ParsedMove(ParsedMoveType.ALL_IN, 0, 0);
			}
			if (lower.startsWith("x")) {
				final Double multiplier = Double.parseDouble(lower.substring(1, lower.length()));
				checkArgument(multiplier >= 0, "Max bet raise multiplier can not be negative");
				return new ParsedMove(ParsedMoveType.MAX_BET_MULTIPLIER, 0, multiplier);
			}
			String postFix = null;
			if (lower.startsWith("px")) {
				postFix = lower.substring(2);
			}
			if (lower.startsWith("potx")) {
				postFix = lower.substring(4);
			}
			if (postFix != null) {
				final Double multiplier = Double.parseDouble(postFix);
				checkArgument(multiplier >= 0, "Pot bet multiplier can not be negative");
				return new ParsedMove(ParsedMoveType.POT_MULTIPLIER, 0, multiplier);
			}
			final Integer num = Integer.parseInt(lower);
			checkArgument(num >= 0, "Numeric moves cant be negative");
			return new ParsedMove(ParsedMoveType.NUMERIC, num, 0);
		}
	}

	private static class Node {
		private final LinkedHashMap<ParsedMove, Node> children = new LinkedHashMap<>();
	}

	private final Node root = new Node();

	public NLBetAbstractBetTreeReader(final InputStream is) {
		try (final Scanner scanner = new Scanner(is);) {
			List<ParsedMove> lastSequence = new ArrayList<>();
			int lineNb = 0;
			int lastLineParsed = 0;
			while (scanner.hasNextLine()) {
				final String line = scanner.nextLine();
				if (line.trim().isEmpty()) {
					lineNb++;
					continue;
				}
				final String[] splitted = line.split("\t");
				final int counts = splitted.length;
				List<ParsedMove> newSequence = new ArrayList<>();
				boolean sawFirstValue = false;
				for (int i = 0; i < counts; i++) {
					final String val = splitted[i].trim();
					if (val.isEmpty()) {
						checkArgument(!sawFirstValue,
								"Unexpected empty value tabulation index " + i + " on line index " + lineNb);
						checkArgument(lastSequence.size() > i, "Unable to infer value from last parsed line "
								+ lastLineParsed + " in line " + lineNb + " because it is too short");
						newSequence.add(lastSequence.get(i));
					} else {
						sawFirstValue = true;
						try {
							newSequence.add(ParsedMove.parse(val));
						} catch (Exception e) {
							throw new IllegalArgumentException("Unable to parse value on tabulation index " + i
									+ " for line index " + lineNb + " from string \"" + val + "\"", e);
						}
					}
				}
				putSequence(newSequence);
				lastLineParsed = lineNb;
				lineNb++;
				lastSequence = newSequence;
			}
		}
	}

	private final void putSequence(final List<ParsedMove> sequence) {
		Node node = root;
		for (ParsedMove val : sequence) {
			if (!node.children.containsKey(val)) {
				node.children.put(val, new Node());
			}
			node = node.children.get(val);
		}
	}

	@Override
	public List<Move<PlayerId>> movesForHand(final NLHandRounds<PlayerId> hand) {
		final Node node = findNode(hand);
		final List<Move<PlayerId>> result = new LinkedList<>();
		for (ParsedMove parsedMove : node.children.keySet()) {
			result.add(moveFrom(hand, parsedMove));
		}
		return result;
	}

	private final Move<PlayerId> moveFrom(final NLHandRounds<PlayerId> hand, final ParsedMove move) {
		switch (move.type) {
		case ALL_IN:
			break;
		case CALL:
			break;
		case FOLD:
			break;
		case MAX_BET_MULTIPLIER:
			break;
		case MIN_BET_RAISE:
			break;
		case NUMERIC:
			break;
		case POT_MULTIPLIER:
			break;

		}
		return null;
	}

	private final Node findNode(final NLHandRounds<PlayerId> hand) {
		final List<List<Move<PlayerId>>> moves = hand.getBetMoves();
		Node node = root;
		for (List<Move<PlayerId>> roundMoves : moves) {
			for (Move<PlayerId> move : roundMoves) {
				node = findChildNode(hand, node, move);
			}
		}
		return node;
	}

	private final Node findChildNode(final NLHandRounds<PlayerId> hand, final Node node, final Move<PlayerId> move) {
		switch (move.getType()) {
		case SB:
		case NO_BLIND:
		case NO_ANTE:
		case ANTE:
		case BB:
			return node;
		case BET:
			return findBetNode(hand, node, move.getValue());
		case CALL:
			return findCallNode(node, move.getValue());
		case FOLD:
			return findFoldNode(node, move.getValue());
		case RAISE:
			return findRaiseNode(hand, node, move.getValue());
		}
		throw new IllegalStateException();
	}

	private final int getPotBet(final NLHandRounds<PlayerId> hand, final double multiplier) {
		int res = 0;
		final List<Pot<PlayerId>> pots = hand.getCurrentPots();
		for (Pot<PlayerId> pot : pots) {
			res += pot.getValue();
		}
		return (int) multiplier * res;
	}

	private final int getMaxBetBet(final NLHandRounds<PlayerId> hand, final double multiplier) {
		int maxBet = 0;
		final List<PlayerData<PlayerId>> players = hand.getPlayersData();
		for (PlayerData<PlayerId> player : players) {
			maxBet = Math.max(player.getBet(), maxBet);
		}
		return (int) multiplier * maxBet;
	}

	private final Node findBetNode(final NLHandRounds<PlayerId> hand, final Node node, final int betValue) {
		for (ParsedMove move : node.children.keySet()) {
			if (move.type == ParsedMoveType.NUMERIC && move.numericValue == betValue) {
				return node.children.get(move);
			}
			if (move.type == ParsedMoveType.POT_MULTIPLIER && getPotBet(hand, move.multiplier) == betValue) {
				return node.children.get(move);
			}
		}
		throw new IllegalStateException("Couldn't find a bet node");
	}

	private final Node findCallNode(final Node node, final int callValue) {
		for (ParsedMove move : node.children.keySet()) {
			if (move.type == ParsedMoveType.CALL) {
				return node.children.get(move);
			}
			if (move.type == ParsedMoveType.NUMERIC && callValue == move.numericValue) {
				return node.children.get(move);
			}
		}
		throw new IllegalStateException("Couldn't find a call node");
	}

	private final Node findFoldNode(final Node node, final int callValue) {
		for (ParsedMove move : node.children.keySet()) {
			if (move.type == ParsedMoveType.FOLD) {
				return node.children.get(move);
			}
		}
		throw new IllegalStateException("Couldn't find a fold node");
	}

	private final Node findRaiseNode(final NLHandRounds<PlayerId> hand, final Node node, final int callValue) {
		for (ParsedMove move : node.children.keySet()) {
			if (move.type == ParsedMoveType.NUMERIC && move.numericValue == callValue) {
				return node.children.get(move);
			}
			if (move.type == ParsedMoveType.MAX_BET_MULTIPLIER && getMaxBetBet(hand, move.multiplier) == callValue) {
				return node.children.get(move);
			}
		}
		throw new IllegalStateException("Couldn't find a raise node");
	}
}
