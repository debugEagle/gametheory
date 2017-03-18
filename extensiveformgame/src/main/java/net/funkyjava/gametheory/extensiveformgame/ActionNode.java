package net.funkyjava.gametheory.extensiveformgame;

import net.funkyjava.gametheory.extensiveformgame.Game.NodeType;

public class ActionNode {
	public final NodeType nodeType;
	public final int player;
	public final int round;
	public final double[] payoutsNoChance;
	public final ChancesPayouts chancesPayouts;
	public final ActionNode[] children;
	public final int nbChildren;
	public final int index;

	public ActionNode(final double[] payoutsNoChance) {
		this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
		this.payoutsNoChance = payoutsNoChance;
		this.player = 0;
		this.round = 0;
		this.chancesPayouts = null;
		this.children = null;
		this.nbChildren = 0;
		this.index = 0;
	}

	public ActionNode(final PlayerNode playerNode, final ActionNode[] children, int index) {
		this.nodeType = NodeType.PLAYER;
		this.payoutsNoChance = null;
		this.player = playerNode.player;
		this.round = playerNode.round;
		this.chancesPayouts = null;
		this.children = children;
		this.nbChildren = children.length;
		this.index = index;
	}

	public ActionNode(final ChancesPayouts chancesPayouts) {
		this.nodeType = NodeType.CHANCES_PAYOUTS;
		this.payoutsNoChance = null;
		this.player = 0;
		this.round = 0;
		this.chancesPayouts = chancesPayouts;
		this.children = null;
		this.nbChildren = 0;
		this.index = 0;
	}
}
