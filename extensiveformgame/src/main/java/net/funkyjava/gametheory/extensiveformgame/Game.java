package net.funkyjava.gametheory.extensiveformgame;

public interface Game<Id, Chances> {

  public static enum NodeType {
    PLAYER, PAYOUTS_NO_CHANCE, CHANCES_PAYOUTS
  }

  /**
   * First index is the round, second index is the player, should contain the number of possible
   * chances for each player in each round
   * 
   * @return Arrays of rounds players chances count.
   */
  int[][] roundChancesSizes();

  /**
   * The number of players
   * 
   * @return number of players
   */
  int getNbPlayers();

  /**
   * Get the game state walker to walk the actions and payouts tree
   * 
   * @return The game state walker
   */
  GameActionStateWalker<Id, Chances> rootGameStateWalker();

}
