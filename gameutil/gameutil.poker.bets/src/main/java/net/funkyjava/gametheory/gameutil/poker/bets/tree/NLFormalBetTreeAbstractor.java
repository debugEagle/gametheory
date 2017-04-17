package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.google.common.base.Optional;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHand;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.pots.Pot;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetChoice;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetRange;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.CallValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.RaiseRange;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.PlayerData;

@Slf4j
public class NLFormalBetTreeAbstractor<PlayerId> implements NLBetTreeAbstractor<PlayerId> {

  private static enum ParsedMoveType {
    FOLD, CALL, MIN_BET_RAISE, ALL_IN, NUMERIC, POT_MULTIPLIER, MAX_BET_MULTIPLIER
  }

  @AllArgsConstructor
  @EqualsAndHashCode
  @ToString
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
        case "fold":
        case "f":
          return new ParsedMove(ParsedMoveType.FOLD, 0, 0);
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

  public NLFormalBetTreeAbstractor(final InputStream is) {
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
        final String[] splitted = line.split("[\t ]*-[\t ]*");
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

  public static <PlayerId> NLFormalBetTreeAbstractor<PlayerId> read(final Path path)
      throws FileNotFoundException, IOException {
    checkArgument(Files.exists(path), "No file at path " + path);
    try (final FileInputStream fis = new FileInputStream(path.toFile())) {
      return new NLFormalBetTreeAbstractor<>(fis);
    }
  }

  public static <PlayerId> NLFormalBetTreeAbstractor<PlayerId> read(final String pathStr)
      throws FileNotFoundException, IOException {
    final Path path = Paths.get(pathStr);
    return read(path);
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
  public List<Move<PlayerId>> movesForHand(final NLHand<PlayerId> hand) {
    final Node node = findNode(hand);
    final List<Move<PlayerId>> result = new LinkedList<>();
    for (ParsedMove parsedMove : node.children.keySet()) {
      final Optional<Move<PlayerId>> moveOpt = moveFrom(hand, parsedMove);
      if (moveOpt.isPresent()) {
        result.add(moveOpt.get());
      }
    }
    return result;
  }

  private final Optional<Move<PlayerId>> moveFrom(final NLHand<PlayerId> hand,
      final ParsedMove move) {
    switch (move.type) {
      case ALL_IN:
        return allInMoveFrom(hand);
      case CALL:
        return callMoveFrom(hand);
      case FOLD:
        return foldMoveFrom(hand);
      case MAX_BET_MULTIPLIER:
        return maxBetMultiplierMoveFrom(hand, move.multiplier);
      case MIN_BET_RAISE:
        return minBetRaiseMoveFrom(hand);
      case NUMERIC:
        return numericMoveFrom(hand, move.numericValue);
      case POT_MULTIPLIER:
        return potMultiplierMoveFrom(hand, move.multiplier);

    }
    return Optional.absent();
  }

  private final Optional<Move<PlayerId>> potMultiplierMoveFrom(final NLHand<PlayerId> hand,
      final double multiplier) {
    final int totPots = hand.getTotalPotsValue();
    final int betValue = (int) (totPots * multiplier);
    final BetChoice<PlayerId> betChoice = hand.getBetChoice();
    final RaiseRange raiseRange = betChoice.getRaiseRange();
    final PlayerId player = hand.getBettingPlayer();
    if (raiseRange.exists()) {
      return Optional.of(Move.getRaise(player,
          Math.max(Math.min(raiseRange.getMax(), betValue), raiseRange.getMin()),
          raiseRange.getOldBet()));
    }
    final BetRange betRange = betChoice.getBetRange();
    if (betRange.exists()) {
      return Optional.of(
          Move.getBet(player, Math.max(Math.min(betRange.getMax(), betValue), betRange.getMin())));
    }
    return Optional.absent();
  }

  private final Optional<Move<PlayerId>> numericMoveFrom(final NLHand<PlayerId> hand,
      final int numValue) {
    final BetChoice<PlayerId> betChoice = hand.getBetChoice();
    final RaiseRange raiseRange = betChoice.getRaiseRange();
    final PlayerId player = hand.getBettingPlayer();
    if (raiseRange.exists() && numValue <= raiseRange.getMax() && numValue >= raiseRange.getMin()) {
      return Optional.of(Move.getRaise(player, numValue, raiseRange.getOldBet()));
    }
    final BetRange betRange = betChoice.getBetRange();
    if (betRange.exists() && numValue <= betRange.getMax() && numValue >= betRange.getMin()) {
      return Optional.of(Move.getBet(player, numValue));
    }
    final CallValue callVal = betChoice.getCallValue();
    if (callVal.exists() && callVal.getValue() == numValue) {
      return Optional.of(Move.getCall(player, numValue, callVal.getOldBet()));
    }
    return Optional.absent();
  }

  private final Optional<Move<PlayerId>> allInMoveFrom(final NLHand<PlayerId> hand) {
    final BetChoice<PlayerId> betChoice = hand.getBetChoice();
    final RaiseRange raiseRange = betChoice.getRaiseRange();
    final PlayerId player = hand.getBettingPlayer();
    if (raiseRange.exists()) {
      return Optional.of(Move.getRaise(player, raiseRange.getMax(), raiseRange.getOldBet()));
    }
    final BetRange betRange = betChoice.getBetRange();
    if (betRange.exists()) {
      return Optional.of(Move.getBet(player, betRange.getMax()));
    }
    return callMoveFrom(hand);
  }

  private final Optional<Move<PlayerId>> callMoveFrom(final NLHand<PlayerId> hand) {
    final BetChoice<PlayerId> betChoice = hand.getBetChoice();
    final PlayerId player = hand.getBettingPlayer();
    final CallValue callVal = betChoice.getCallValue();
    if (callVal.exists()) {
      return Optional.of(Move.getCall(player, callVal.getValue(), callVal.getOldBet()));
    }
    return Optional.absent();
  }

  private final Optional<Move<PlayerId>> foldMoveFrom(final NLHand<PlayerId> hand) {
    final PlayerId player = hand.getBettingPlayer();
    return Optional.of(Move.getFold(player));
  }

  private final Optional<Move<PlayerId>> maxBetMultiplierMoveFrom(final NLHand<PlayerId> hand,
      final double multiplier) {
    final List<PlayerData<PlayerId>> data = hand.getPlayersData();
    int maxBet = 0;
    for (PlayerData<PlayerId> pData : data) {
      maxBet = Math.max(maxBet, pData.getBet());
    }
    final int raiseBet = (int) (maxBet * multiplier);
    final BetChoice<PlayerId> betChoice = hand.getBetChoice();
    final RaiseRange raiseRange = betChoice.getRaiseRange();
    final PlayerId player = hand.getBettingPlayer();
    if (raiseRange.exists()) {
      return Optional.of(Move.getRaise(player,
          Math.max(Math.min(raiseRange.getMax(), raiseBet), raiseRange.getMin()),
          raiseRange.getOldBet()));
    }
    final BetRange betRange = betChoice.getBetRange();
    if (betRange.exists()) {
      return Optional.of(
          Move.getBet(player, Math.max(Math.min(betRange.getMax(), raiseBet), betRange.getMin())));
    }
    return Optional.absent();
  }

  private final Optional<Move<PlayerId>> minBetRaiseMoveFrom(final NLHand<PlayerId> hand) {
    final List<PlayerData<PlayerId>> data = hand.getPlayersData();
    int maxBet = 0;
    for (PlayerData<PlayerId> pData : data) {
      maxBet = Math.max(maxBet, pData.getBet());
    }
    final BetChoice<PlayerId> betChoice = hand.getBetChoice();
    final RaiseRange raiseRange = betChoice.getRaiseRange();
    final PlayerId player = hand.getBettingPlayer();
    if (raiseRange.exists()) {
      return Optional.of(Move.getRaise(player, raiseRange.getMin(), raiseRange.getOldBet()));
    }
    final BetRange betRange = betChoice.getBetRange();
    if (betRange.exists()) {
      return Optional.of(Move.getBet(player, betRange.getMin()));
    }
    return Optional.absent();
  }

  private final Node findNode(final NLHand<PlayerId> hand) {
    final List<List<Move<PlayerId>>> roundMoves = hand.getBetMoves();
    final List<Move<PlayerId>> moves = new ArrayList<>();
    for (List<Move<PlayerId>> rMoves : roundMoves) {
      moves.addAll(rMoves);
    }
    final List<NLHand<PlayerId>> hands = hand.getPreviousPlayersBetMoveStates();
    checkArgument(hands.size() == moves.size(), "Not same number of hands before bet ("
        + hands.size() + ")/ bet moves (" + moves.size() + ")");
    final int length = hands.size();
    Node node = root;
    for (int i = 0; i < length; i++) {
      node = findChildNode(hands.get(i), node, moves.get(i));
    }
    return node;
  }

  private final Node findChildNode(final NLHand<PlayerId> hand, final Node node,
      final Move<PlayerId> move) {
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

  private final int getPotBet(final NLHand<PlayerId> hand, final double multiplier) {
    int res = 0;
    final List<Pot<PlayerId>> pots = hand.getCurrentPots();
    for (Pot<PlayerId> pot : pots) {
      res += pot.getValue();
    }
    return (int) multiplier * res;
  }

  private final int getMaxBetBet(final NLHand<PlayerId> hand, final double multiplier) {
    int maxBet = 0;
    final List<PlayerData<PlayerId>> players = hand.getPlayersData();
    for (PlayerData<PlayerId> player : players) {
      maxBet = Math.max(player.getBet(), maxBet);
    }
    return (int) multiplier * maxBet;
  }

  private final Node findBetNode(final NLHand<PlayerId> hand, final Node node, final int betValue) {
    for (ParsedMove move : node.children.keySet()) {
      if (move.type == ParsedMoveType.NUMERIC && move.numericValue == betValue) {
        return node.children.get(move);
      }
      if (move.type == ParsedMoveType.POT_MULTIPLIER
          && getPotBet(hand, move.multiplier) == betValue) {
        return node.children.get(move);
      }
    }
    throw new IllegalStateException("Couldn't find a bet node");
  }

  private final static Node findCallNode(final Node node, final int callValue) {
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

  private final static Node findFoldNode(final Node node, final int callValue) {
    for (ParsedMove move : node.children.keySet()) {
      if (move.type == ParsedMoveType.FOLD) {
        return node.children.get(move);
      }
    }
    throw new IllegalStateException("Couldn't find a fold node");
  }

  private final Node findRaiseNode(final NLHand<PlayerId> hand, final Node node,
      final int raiseValue) {
    log.info("Looking for raise node with value {}", raiseValue);
    for (ParsedMove move : node.children.keySet()) {
      if (move.type == ParsedMoveType.NUMERIC && move.numericValue == raiseValue) {
        return node.children.get(move);
      }
      if (move.type == ParsedMoveType.MAX_BET_MULTIPLIER
          && getMaxBetBet(hand, move.multiplier) == raiseValue) {
        return node.children.get(move);
      }
      if (move.type == ParsedMoveType.ALL_IN) {
        final RaiseRange raiseRange = hand.getBetChoice().getRaiseRange();
        if (raiseRange.exists() && raiseRange.getMax() == raiseValue) {
          return node.children.get(move);
        }
      }
    }
    throw new IllegalStateException("Couldn't find a raise node");
  }

  public final void print() {
    print(root, 0);
  }

  private final void print(final Node node, final int depth) {
    final StringBuilder str = new StringBuilder();
    for (int i = 0; i < depth; i++) {
      str.append('\t');
    }
    for (final ParsedMove move : node.children.keySet()) {
      log.info(str.toString() + move);
      print(node.children.get(move), depth + 1);
    }

  }

  public static void main(String[] args) {
    checkArgument(args.length == 1, "Expected exactly one argument (path of the file to parse)");
    final String path = args[0];
    try {
      final NLFormalBetTreeAbstractor<Integer> tree = read(path);
      tree.print();
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
