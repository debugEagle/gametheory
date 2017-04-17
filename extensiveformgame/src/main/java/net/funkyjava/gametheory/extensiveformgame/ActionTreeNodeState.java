package net.funkyjava.gametheory.extensiveformgame;

import lombok.Getter;

public class ActionTreeNodeState<Id, Chances> {
  /**
   * The action tree node type enum
   * 
   * @author Pierre Mardon
   *
   */
  public static enum NodeType {
    /**
     * A player type node is a node at which a player should make a decision
     */
    PLAYER,
    /**
     * Terminal node type with constant utility
     */
    PAYOUTS_NO_CHANCE,
    /**
     * Terminal node type with utility depending on chances
     */
    CHANCES_PAYOUTS
  }

  @Getter
  private final NodeType nodeType;
  @Getter
  private final PlayerNode<Id> playerNode;
  @Getter
  private final double[] payoutsNoChance;
  @Getter
  private final ChancesPayouts<Chances> chancesPayouts;

  public ActionTreeNodeState(final PlayerNode<Id> playerNode) {
    this.nodeType = NodeType.PLAYER;
    this.playerNode = playerNode;
    this.payoutsNoChance = null;
    this.chancesPayouts = null;
  }

  public ActionTreeNodeState(final double[] payoutsNoChance) {
    this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
    this.playerNode = null;
    this.payoutsNoChance = payoutsNoChance;
    this.chancesPayouts = null;
  }

  public ActionTreeNodeState(final ChancesPayouts<Chances> chancesPayouts) {
    this.nodeType = NodeType.CHANCES_PAYOUTS;
    this.playerNode = null;
    this.payoutsNoChance = null;
    this.chancesPayouts = chancesPayouts;
  }
}
