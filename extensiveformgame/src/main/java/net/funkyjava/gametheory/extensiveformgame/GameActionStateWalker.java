package net.funkyjava.gametheory.extensiveformgame;

import lombok.Data;

/**
 * This class has to be implemented for each {@link Game} to describe its action tree. It is a
 * polymorphic representation
 * 
 * @author Pierre Mardon
 *
 * @param <Id> the action node id class
 * @param <Chances> the chances class
 */
@Data
public abstract class GameActionStateWalker<Id, Chances> {
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

  private final NodeType nodeType;
  private final PlayerNode<Id> playerNode;
  private final boolean playerNodeHasMultipleParents;
  private final double[] payoutsNoChance;
  private final ChancesPayouts<Chances> chancesPayouts;

  public GameActionStateWalker(final PlayerNode<Id> playerNode, final boolean hasMultipleParents) {
    this.nodeType = NodeType.PLAYER;
    this.playerNode = playerNode;
    this.payoutsNoChance = null;
    this.chancesPayouts = null;
    this.playerNodeHasMultipleParents = hasMultipleParents;
  }

  public GameActionStateWalker(final double[] payoutsNoChance) {
    this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
    this.playerNode = null;
    this.payoutsNoChance = payoutsNoChance;
    this.chancesPayouts = null;
    this.playerNodeHasMultipleParents = false;
  }

  public GameActionStateWalker(final ChancesPayouts<Chances> chancesPayouts) {
    this.nodeType = NodeType.CHANCES_PAYOUTS;
    this.playerNode = null;
    this.payoutsNoChance = null;
    this.chancesPayouts = chancesPayouts;
    this.playerNodeHasMultipleParents = false;
  }

  public abstract GameActionStateWalker<Id, Chances> stateForPlayerAction(int actionIndex);
}
