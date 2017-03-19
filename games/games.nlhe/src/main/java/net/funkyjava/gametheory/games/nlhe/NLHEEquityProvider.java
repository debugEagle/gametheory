package net.funkyjava.gametheory.games.nlhe;

public interface NLHEEquityProvider {

	double[] getEquity(final int betRoundIndex, final int[][] roundsPlayersChances, final boolean[] playersToConsider);

}
