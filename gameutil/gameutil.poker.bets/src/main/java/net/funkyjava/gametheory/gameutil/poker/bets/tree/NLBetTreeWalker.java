package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.List;

import com.google.common.base.Optional;

import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;

public interface NLBetTreeWalker<PlayerId> {

	boolean handleCurrentNode(final NLBetTreeNode<PlayerId> node, final List<NLBetTreeNode<PlayerId>> parents,
			final Optional<Move<PlayerId>> lastMove);

}
