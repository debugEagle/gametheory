package net.funkyjava.gametheory.gameutil.poker.bets.tree;

import java.util.List;

import lombok.Getter;

public class NLBetTreeDepthCounter<PlayerId> implements NLBetTreeWalker<PlayerId> {

	@Getter
	private int depth = 0;

	public NLBetTreeDepthCounter() {
	}

	@Override
	public boolean handleCurrentNode(NLBetTreeNode<PlayerId> node, List<NLBetTreeNode<PlayerId>> parents) {
		depth = Math.max(depth, 1 + parents.size());
		return true;
	}

}
