package net.funkyjava.gametheory.games.nlhe.preflop;

import net.funkyjava.gametheory.games.nlhe.NLHEEquityProvider;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopEquityTable;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopReducedEquityTable;

public class NLHE3PlayersPreflopEquityProvider implements NLHEEquityProvider<PreflopChances> {

  private final ThreePlayersPreflopReducedEquityTable table;

  public NLHE3PlayersPreflopEquityProvider(final ThreePlayersPreflopReducedEquityTable table) {
    this.table = table;
  }

  @Override
  public double[] getEquity(final int betRoundIndex, final PreflopChances chances,
      boolean[] playersToConsider) {
    int index = ThreePlayersPreflopEquityTable.heroVilain1Vilain2Index;
    if (!playersToConsider[0]) {
      index = ThreePlayersPreflopEquityTable.vilain1Vilain2Index;
    } else if (!playersToConsider[1]) {
      index = ThreePlayersPreflopEquityTable.heroVilain2Index;
    } else if (!playersToConsider[2]) {
      index = ThreePlayersPreflopEquityTable.heroVilain1Index;
    }
    final int[] pChances = chances.getPlayersChances()[0];
    return table.getEquities(pChances[0], pChances[1], pChances[2])[index];
  }

}
