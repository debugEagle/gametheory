package net.funkyjava.gametheory.extensiveformgame;

import lombok.Getter;

/**
 * Node of a fully built tree. Children are directly accessible and stored in an array
 * 
 * @author Pierre Mardon
 *
 * @param <Id> the player nodes id type
 * @param <Chances> the chances type
 */
public class LinkedActionTreeNode<Id, Chances> extends ActionTreeNodeState<Id, Chances> {

  /**
   * Children of the player node or null
   */
  @Getter
  private final LinkedActionTreeNode<Id, Chances>[] children;
  /**
   * The arbitrary node index among active player's nodes in its round
   */
  @Getter
  private final int playerRoundActionIndex;

  /**
   * @see ActionTreeNodeState#ActionTreeNodeState(ChancesPayouts)
   * @param chancesPayouts
   */
  public LinkedActionTreeNode(ChancesPayouts<Chances> chancesPayouts) {
    super(chancesPayouts);
    this.children = null;
    playerRoundActionIndex = -1;
  }

  /**
   * @see ActionTreeNodeState#ActionTreeNodeState(double[])
   * @param payoutsNoChance
   */
  public LinkedActionTreeNode(final double[] payoutsNoChance) {
    super(payoutsNoChance);
    this.children = null;
    playerRoundActionIndex = -1;
  }

  /**
   * @see ActionTreeNodeState#ActionTreeNodeState(PlayerNode)
   * @param playerNode the player node
   * @param children the node's children
   * @param playerRoundActionIndex the arbitrary node index among active player's nodes in its round
   */
  public LinkedActionTreeNode(final PlayerNode<Id> playerNode,
      final LinkedActionTreeNode<Id, Chances>[] children, final int playerRoundActionIndex) {
    super(playerNode);
    this.children = children;
    this.playerRoundActionIndex = playerRoundActionIndex;
  }
}
