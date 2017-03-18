package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.List;

public interface NLBetTreeWalker<PlayerId> {

	boolean handleCurrentNode(final NLBetTreeNode<PlayerId> node, final List<NLBetTreeNode<PlayerId>> parents);

}
