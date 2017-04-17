package net.funkyjava.gametheory.extensiveformgame;

import lombok.Getter;

public class LinkedActionTreeNode<Id, Chances> extends ActionTreeNodeState<Id, Chances> {

  @Getter
  private final LinkedActionTreeNode<Id, Chances>[] children;
  @Getter
  private final int playerRoundActionIndex;

  public LinkedActionTreeNode(ChancesPayouts<Chances> chancesPayouts) {
    super(chancesPayouts);
    this.children = null;
    playerRoundActionIndex = -1;
  }

  public LinkedActionTreeNode(final double[] payoutsNoChance) {
    super(payoutsNoChance);
    this.children = null;
    playerRoundActionIndex = -1;
  }

  public LinkedActionTreeNode(final PlayerNode<Id> playerNode,
      final LinkedActionTreeNode<Id, Chances>[] children, final int playerRoundActionIndex) {
    super(playerNode);
    this.children = children;
    this.playerRoundActionIndex = playerRoundActionIndex;
  }
}
