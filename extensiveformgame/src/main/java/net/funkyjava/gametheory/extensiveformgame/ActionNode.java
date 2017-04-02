package net.funkyjava.gametheory.extensiveformgame;

import net.funkyjava.gametheory.extensiveformgame.Game.NodeType;

public class ActionNode<Id, Chances> {
	public final NodeType nodeType;
	public final int player;
	public final int round;
	public final double[] payoutsNoChance;
	public final ChancesPayouts<Chances> chancesPayouts;
	public final ActionNode<Id, Chances>[] children;
	public final int nbChildren;
	public final int index;
	public final Id id;

	public ActionNode(final double[] payoutsNoChance) {
		this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
		this.payoutsNoChance = payoutsNoChance;
		this.player = 0;
		this.round = 0;
		this.chancesPayouts = null;
		this.children = null;
		this.nbChildren = 0;
		this.index = 0;
		this.id = null;
	}

	public ActionNode(final PlayerNode playerNode, final Id id, final ActionNode<Id, Chances>[] children, int index) {
		this.nodeType = NodeType.PLAYER;
		this.payoutsNoChance = null;
		this.player = playerNode.player;
		this.round = playerNode.round;
		this.chancesPayouts = null;
		this.children = children;
		this.nbChildren = children.length;
		this.index = index;
		this.id = id;
	}

	public ActionNode(final ChancesPayouts<Chances> chancesPayouts) {
		this.nodeType = NodeType.CHANCES_PAYOUTS;
		this.payoutsNoChance = null;
		this.player = 0;
		this.round = 0;
		this.chancesPayouts = chancesPayouts;
		this.children = null;
		this.nbChildren = 0;
		this.index = 0;
		this.id = null;
	}
}
