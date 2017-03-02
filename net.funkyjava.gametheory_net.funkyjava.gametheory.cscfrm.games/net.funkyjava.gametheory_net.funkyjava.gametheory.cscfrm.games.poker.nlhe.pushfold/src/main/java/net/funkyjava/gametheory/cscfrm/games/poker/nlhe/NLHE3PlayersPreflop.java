package net.funkyjava.gametheory.cscfrm.games.poker.nlhe;

import java.util.Iterator;

import net.funkyjava.gametheory.cscfrm.model.game.CSCFRMGame;
import net.funkyjava.gametheory.cscfrm.model.game.nodes.Node;
import net.funkyjava.gametheory.cscfrm.model.game.nodes.PlayerNode;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLAbstractedBetTree;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopEquityTables;

public class NLHE3PlayersPreflop<PNode extends PlayerNode, PlayerId> implements CSCFRMGame<PNode> {

	private int[] chances;
	private final NLHE3PlayersPreflopChances drawer;

	public NLHE3PlayersPreflop(final NLHE3PlayersPreflopChances drawer, final NLAbstractedBetTree<PlayerId> betTree,
			final ThreePlayersPreflopEquityTables tables) {
		this.drawer = drawer;
	}

	@Override
	public void onIterationStart() throws Exception {
		chances = drawer.getChances(chances);
	}

	@Override
	public void back() {
		// TODO Auto-generated method stub

	}

	@Override
	public Node getCurrentNode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getNbPlayers() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxDepth() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getMaxNbPlActions() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getUId() {
		return "NLHE3PlayersPreflop";
	}

	@Override
	public int choseChanceAction() {
		// Unnecessary, won't present any chance node
		return 0;
	}

	@Override
	public void onGameStart() {
		// Nothing to do
	}

	@Override
	public void onPlayerActionChosen(int actionIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public Iterator<PNode> getPlayerNodesIterator() {
		// TODO Auto-generated method stub
		return null;
	}

}
