package net.funkyjava.gametheory.extensiveformgame;

import lombok.Data;
import net.funkyjava.gametheory.extensiveformgame.Game.NodeType;

@Data
public abstract class GameActionStateWalker<Id, Chances> {

	public final NodeType nodeType;
	public final PlayerNode playerNode;
	public final boolean playerNodeHasMultipleParents;
	public final Id id;
	public final double[] payoutsNoChance;
	public final ChancesPayouts<Chances> chancesPayouts;

	public GameActionStateWalker(final PlayerNode playerNode, final Id id, final boolean hasMultipleParents) {
		this.nodeType = NodeType.PLAYER;
		this.playerNode = playerNode;
		this.payoutsNoChance = null;
		this.chancesPayouts = null;
		this.id = id;
		this.playerNodeHasMultipleParents = hasMultipleParents;
	}

	public GameActionStateWalker(final double[] payoutsNoChance) {
		this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
		this.playerNode = null;
		this.payoutsNoChance = payoutsNoChance;
		this.chancesPayouts = null;
		this.id = null;
		this.playerNodeHasMultipleParents = false;
	}

	public GameActionStateWalker(final ChancesPayouts<Chances> chancesPayouts) {
		this.nodeType = NodeType.CHANCES_PAYOUTS;
		this.playerNode = null;
		this.payoutsNoChance = null;
		this.chancesPayouts = chancesPayouts;
		this.id = null;
		this.playerNodeHasMultipleParents = false;
	}

	public abstract GameActionStateWalker<Id, Chances> stateForPlayerAction(int actionIndex);
}
