package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.LinkedList;
import java.util.List;

import net.funkyjava.gametheory.gameutil.poker.bets.NLHand;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetChoice;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.BetRange;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.CallValue;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.betround.RaiseRange;

public class NLPushFoldBetTreeAbstractor<PlayerId> implements NLBetTreeAbstractor<PlayerId> {

  @Override
  public List<Move<PlayerId>> movesForHand(final NLHand<PlayerId> hand) {
    final List<Move<PlayerId>> moves = new LinkedList<>();
    final BetChoice<PlayerId> choice = hand.getBetChoice();
    final PlayerId player = choice.getPlayer();
    moves.add(Move.getFold(player));
    final BetRange betRange = choice.getBetRange();
    final RaiseRange raiseRange = choice.getRaiseRange();
    final CallValue callValue = choice.getCallValue();
    if (betRange.exists()) {
      moves.add(Move.getBet(player, betRange.getMax()));
    } else if (raiseRange.exists()) {
      moves.add(Move.getRaise(player, raiseRange.getMax(), raiseRange.getOldBet()));
    } else if (callValue.exists()) {
      moves.add(Move.getCall(player, callValue.getValue(), callValue.getOldBet()));
    }
    return moves;
  }

}
