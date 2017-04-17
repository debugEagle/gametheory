package net.funkyjava.gametheory.extensiveformgame;

import lombok.Getter;

public class ActionTreeNode<Id, Chances> extends ActionTreeNodeState<Id, Chances> {

  @Getter
  private final ActionTreePlayerChoiceTransition<Id, Chances> transition;
  @Getter
  private final boolean playerNodeHasMultipleParents;

  public ActionTreeNode(ChancesPayouts<Chances> chancesPayouts) {
    super(chancesPayouts);
    transition = null;
    playerNodeHasMultipleParents = false;
  }

  public ActionTreeNode(final double[] payoutsNoChance) {
    super(payoutsNoChance);
    transition = null;
    playerNodeHasMultipleParents = false;
  }

  public ActionTreeNode(final PlayerNode<Id> playerNode, final boolean hasMultipleParents,
      final ActionTreePlayerChoiceTransition<Id, Chances> transition) {
    super(playerNode);
    playerNodeHasMultipleParents = hasMultipleParents;
    this.transition = transition;
  }
}
