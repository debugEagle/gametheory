package net.funkyjava.gametheory.games.nlhe;

public interface NLHEEquityProvider<Chances> {

  double[] getEquity(final int betRoundIndex, final Chances chances,
      final boolean[] playersToConsider);

}
