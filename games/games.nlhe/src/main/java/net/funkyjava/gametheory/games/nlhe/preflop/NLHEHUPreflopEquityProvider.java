package net.funkyjava.gametheory.games.nlhe.preflop;

import static com.google.common.base.Preconditions.checkArgument;

import net.funkyjava.gametheory.games.nlhe.NLHEEquityProvider;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HUPreflopEquityTables;

public class NLHEHUPreflopEquityProvider implements NLHEEquityProvider<PreflopChances> {

  final double[][][] table;

  public NLHEHUPreflopEquityProvider(final HUPreflopEquityTables tables) {
    checkArgument(tables.isComputed(), "HU preflop equity tables are not computed");
    final double[][] baseTable = tables.getReducedEquity();
    this.table = expand(baseTable);
  }

  private final double[][][] expand(final double[][] baseTable) {
    final double[][][] table = new double[169][169][];
    for (int i = 0; i < 169; i++) {
      final double[] baseRow = baseTable[i];
      final double[][] row = table[i];
      for (int j = 0; j < 169; j++) {
        final double equity = baseRow[j];
        row[j] = new double[] {equity, 1 - equity};
      }
    }
    return table;
  }

  @Override
  public double[] getEquity(final int betRoundIndex, final PreflopChances chances,
      boolean[] playersToConsider) {
    final int[] playersChances = chances.getPlayersChances()[0];
    return table[playersChances[0]][playersChances[1]];
  }

}
