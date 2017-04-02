package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.List;

import net.funkyjava.gametheory.gameutil.poker.bets.NLHand;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;

public interface NLBetTreeAbstractor<PlayerId> {

	List<Move<PlayerId>> movesForHand(NLHand<PlayerId> hand);
}
