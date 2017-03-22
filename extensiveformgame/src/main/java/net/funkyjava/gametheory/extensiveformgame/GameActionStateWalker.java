package net.funkyjava.gametheory.extensiveformgame;

import lombok.Data;
import net.funkyjava.gametheory.extensiveformgame.Game.NodeType;

@Data
public abstract class GameActionStateWalker<Id> {

	public final NodeType nodeType;
	public final PlayerNode playerNode;
	public final Id id;
	public final double[] payoutsNoChance;
	public final ChancesPayouts chancesPayouts;

	public GameActionStateWalker(final PlayerNode playerNode, final Id id) {
		this.nodeType = NodeType.PLAYER;
		this.playerNode = playerNode;
		this.payoutsNoChance = null;
		this.chancesPayouts = null;
		this.id = id;
	}

	public GameActionStateWalker(final double[] payoutsNoChance) {
		this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
		this.playerNode = null;
		this.payoutsNoChance = payoutsNoChance;
		this.chancesPayouts = null;
		this.id = null;
	}

	public GameActionStateWalker(final ChancesPayouts chancesPayouts) {
		this.nodeType = NodeType.CHANCES_PAYOUTS;
		this.playerNode = null;
		this.payoutsNoChance = null;
		this.chancesPayouts = chancesPayouts;
		this.id = null;
	}

	public abstract GameActionStateWalker<Id> stateForPlayerAction(int actionIndex);
}
