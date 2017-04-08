package net.funkyjava.gametheory.cscfrm;

import net.funkyjava.gametheory.extensiveformgame.ActionChancesData.DataProvider;
import net.funkyjava.gametheory.extensiveformgame.ActionNode;
import net.funkyjava.gametheory.extensiveformgame.Game;

public class CSCFRMNodeProvider<Id> implements DataProvider<CSCFRMNode, Id> {

  @Override
  public CSCFRMNode getData(final Game<Id, ?> game, final ActionNode<Id, ?> node,
      final int chance) {
    return new CSCFRMNode(node.nbChildren);
  }

  @Override
  public Class<CSCFRMNode> getDataClass() {
    return CSCFRMNode.class;
  }

}
