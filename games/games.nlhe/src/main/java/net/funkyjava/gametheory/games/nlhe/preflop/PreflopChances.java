package net.funkyjava.gametheory.games.nlhe.preflop;

import lombok.AllArgsConstructor;
import net.funkyjava.gametheory.cscfrm.CSCFRMChances;

@AllArgsConstructor
public class PreflopChances implements CSCFRMChances {

  private final int[][] playersChances;

  @Override
  public int[][] getPlayersChances() {
    return playersChances;
  }

}
