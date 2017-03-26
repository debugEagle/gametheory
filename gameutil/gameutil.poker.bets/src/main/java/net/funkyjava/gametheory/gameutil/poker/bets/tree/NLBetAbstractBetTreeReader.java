package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;

import lombok.AllArgsConstructor;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandRounds;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;

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
	public List<Move<PlayerId>> movesForHand(NLHandRounds<PlayerId> hand) {
		// TODO Auto-generated method stub
		return null;
	}

	private final Node findNode(final NLHandRounds<PlayerId> hand) {

		throw new IllegalArgumentException("Couldn't find the proper node in parsed tree");
	}

	private final Node findChildNode(final Node node, final Move<PlayerId> move) {
		switch (move.getType()) {
		case SB:
		case NO_BLIND:
		case NO_ANTE:
		case ANTE:
		case BB:
			return node;
		case BET:
			return findBetNode(node, move.getValue());
		case CALL:
			return findCallNode(node, move.getValue());
		case FOLD:
			return findFoldNode(node, move.getValue());
		case RAISE:
			return findRaiseNode(node, move.getValue());
		}
		throw new IllegalStateException();
	}

	private final Node findBetNode(final Node node, final int betValue) {
		// TODO
		return null;
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

	private final Node findRaiseNode(final Node node, final int callValue) {
		// TODO
		return null;
	}
}
