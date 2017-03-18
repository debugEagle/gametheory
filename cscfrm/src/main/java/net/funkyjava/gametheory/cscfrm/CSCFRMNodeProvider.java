package net.funkyjava.gametheory.cscfrm;

import net.funkyjava.gametheory.extensiveformgame.ActionChancesData.DataProvider;
import net.funkyjava.gametheory.extensiveformgame.ActionNode;
import net.funkyjava.gametheory.extensiveformgame.Game;

public class CSCFRMNodeProvider implements DataProvider<CSCFRMNode> {

	@Override
	public CSCFRMNode getData(final Game game, final ActionNode node, final int chance) {
		return new CSCFRMNode(node.nbChildren);
	}

}
