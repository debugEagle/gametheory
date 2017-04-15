package net.funkyjava.gametheory.extensiveformgame;

import lombok.Getter;
import net.funkyjava.gametheory.extensiveformgame.GameActionStateWalker.NodeType;

/**
 * Polymorphic class that represents either an action node (a player has to make a move), a terminal
 * node with a constant utility ({@link #getPayoutsNoChance()} or a terminal node with chance
 * dependent utility
 * 
 * @author Pierre Mardon
 *
 * @param <Id> the id of the node
 * @param <Chances> the chances type
 */
public class GameNode<Id, Chances> {
  /**
   * The node's type
   */
  @Getter
  private final NodeType nodeType;
  /**
   * The payouts when constant terminal node
   */
  @Getter
  private final double[] payoutsNoChance;
  /**
   * The payouts when chance-dependent terminal node
   */
  @Getter
  private final ChancesPayouts<Chances> chancesPayouts;
  /**
   * The player node when action node
   */
  @Getter
  private final PlayerNode<Id> playerNode;
  /**
   * The children
   */
  @Getter
  private final GameNode<Id, Chances>[] children;

  /**
   * The index of the node in its player's round when action node
   */
  @Getter
  private final int index;

  /**
   * Constructor for constant terminal nodes
   * 
   * @param payoutsNoChance the utility
   */
  public GameNode(final double[] payoutsNoChance) {
    this.nodeType = NodeType.PAYOUTS_NO_CHANCE;
    this.payoutsNoChance = payoutsNoChance;
    this.chancesPayouts = null;
    this.children = null;
    this.playerNode = null;
    this.index = 0;
  }

  /**
   * Constructor for action node
   * 
   * @param playerNode the action node characteristics
   * @param children the children
   * @param index the index of this node in its player's round
   */
  public GameNode(final PlayerNode<Id> playerNode, final GameNode<Id, Chances>[] children,
      int index) {
    this.nodeType = NodeType.PLAYER;
    this.playerNode = playerNode;
    this.payoutsNoChance = null;
    this.chancesPayouts = null;
    this.children = children;
    this.index = index;
  }

  /**
   * Constructor for chances dependent terminal nodes
   * 
   * @param chancesPayouts the payouts
   */
  public GameNode(final ChancesPayouts<Chances> chancesPayouts) {
    this.nodeType = NodeType.CHANCES_PAYOUTS;
    this.payoutsNoChance = null;
    this.chancesPayouts = chancesPayouts;
    this.children = null;
    this.playerNode = null;
    this.index = -1;
  }
}
