package net.funkyjava.gametheory.extensiveformgame;

import lombok.Data;
import net.funkyjava.gametheory.extensiveformgame.Game.NodeType;

@Data
public abstract class GameStateWalker {

	public final NodeType nodeType;
	public final PlayerNode playerNode;
	public final double[] payoutsNoChance;
	public final ChancesPayouts chancesPayouts;

	public GameStateWalker(final PlayerNode playerNode) {
		this.nodeType = NodeType.PLAYER;
		this.playerNode = playerNode;
		this.payoutsNoChance = null;
		this.chancesPayouts = null;
	}

	public GameStateWalker(final double[] payoutsNoChance) {
		this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
		this.playerNode = null;
		this.payoutsNoChance = payoutsNoChance;
		this.chancesPayouts = null;
	}

	public GameStateWalker(final ChancesPayouts chancesPayouts) {
		this.nodeType = NodeType.CHANCES_PAYOUTS;
		this.playerNode = null;
		this.payoutsNoChance = null;
		this.chancesPayouts = chancesPayouts;
	}

	public abstract GameStateWalker stateForPlayerAction(int actionIndex);
}