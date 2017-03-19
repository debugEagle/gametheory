package net.funkyjava.gametheory.games.nlhe.preflop;

import static com.google.common.base.Preconditions.checkArgument;

import net.funkyjava.gametheory.games.nlhe.NLHEEquityProvider;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopEquityTables;

public class NLHE3PlayersPreflopEquityProvider implements NLHEEquityProvider {

	private final double[][][][][] table;

	public NLHE3PlayersPreflopEquityProvider(final ThreePlayersPreflopEquityTables tables) {
		checkArgument(tables.isComputed(), "Equity tables are not computed");
		this.table = tables.getReducedEquities();
	}

	@Override
	public double[] getEquity(int betRoundIndex, int[][] roundsPlayersChances, boolean[] playersToConsider) {
		int index = ThreePlayersPreflopEquityTables.heroVilain1Vilain2Index;
		if (!playersToConsider[0]) {
			index = ThreePlayersPreflopEquityTables.vilain1Vilain2Index;
		} else if (!playersToConsider[1]) {
			index = ThreePlayersPreflopEquityTables.heroVilain2Index;
		} else if (!playersToConsider[2]) {
			index = ThreePlayersPreflopEquityTables.heroVilain1Index;
		}
		final int[] chances = roundsPlayersChances[0];
		return table[chances[0]][chances[1]][chances[2]][index];
	}

}
