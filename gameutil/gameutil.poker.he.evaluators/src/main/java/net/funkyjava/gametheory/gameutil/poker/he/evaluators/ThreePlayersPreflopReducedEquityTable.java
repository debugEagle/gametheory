package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.CardsGroupsDrawingTask;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;
import net.funkyjava.gametheory.io.Fillable;

@Slf4j
public class ThreePlayersPreflopReducedEquityTable implements Fillable {

	public static final int heroVilain1Vilain2Index = 0;
	public static final int heroVilain1Index = 1;
	public static final int heroVilain2Index = 2;
	public static final int vilain1Vilain2Index = 3;

	private static final int[] onePlayerGroupsSize = { 2 };
	private static final int[] threePlayersGroupsSize = { 2, 2, 2 };

	private final WaughIndexer threePlayersIndexer = new WaughIndexer(threePlayersGroupsSize);
	@Getter
	private final WaughIndexer holeCardsIndexer = new WaughIndexer(onePlayerGroupsSize);
	@Getter
	private final int nbHoleCards = holeCardsIndexer.getIndexSize();

	private boolean computed = false;

	@Getter
	private final double[][][][][] reducedEquities = new double[nbHoleCards][nbHoleCards][nbHoleCards][4][3];

	public ThreePlayersPreflopReducedEquityTable() {

	}

	@AllArgsConstructor
	private static final class CardsPermutator {

		private final int sourceIndex1, sourceIndex2, sourceIndex3;

		public final void permute(int[][] source, int[][] dest) {
			dest[0] = source[sourceIndex1];
			dest[1] = source[sourceIndex2];
			dest[2] = source[sourceIndex3];
		}

		private final void permuteInverse(final double[] source, final double[] dest) {
			dest[sourceIndex1] = source[0];
			dest[sourceIndex2] = source[1];
			dest[sourceIndex3] = source[2];
		}

		public final void permuteInverse(final double[][] source, final double[][] destination) {
			permuteInverse(source[0], destination[0]);
			permuteInverse(source[1], destination[1]);
			permuteInverse(source[2], destination[2]);
			permuteInverse(source[3], destination[3]);
		}
	}

	private static final CardsPermutator permutator = new CardsPermutator(0, 1, 2);
	private static final CardsPermutator permutator1 = new CardsPermutator(0, 2, 1);
	private static final CardsPermutator permutator2 = new CardsPermutator(1, 2, 0);
	private static final CardsPermutator permutator3 = new CardsPermutator(1, 0, 2);
	private static final CardsPermutator permutator4 = new CardsPermutator(2, 0, 1);
	private static final CardsPermutator permutator5 = new CardsPermutator(2, 1, 0);

	private static final CardsPermutator getPermutator(final int h1, final int h2, final int h3) {
		if (h1 <= h2) {
			if (h2 <= h3) {
				return permutator;
			}
			if (h1 <= h3) {
				return permutator1;
			} else {
				// h3 < h1 <= h2
				return permutator4;
			}
		}
		// h3 <= h2 < h1
		if (h3 <= h2) {
			return permutator5;
		}
		// h2 < h1 <= h3
		if (h1 <= h3) {
			return permutator3;
		} else {
			// h2 < h3 < h1
			return permutator2;
		}
	}

	private void getEquities(final double[][][] equities, final int[][] holeCards, final int heroIndex,
			final int vilain1Index, final int vilain2Index, final int[][] tmpHoleCards, final double[][] destEquities) {
		if (heroIndex <= vilain1Index && vilain1Index <= vilain2Index) {
			final int indexInTables = threePlayersIndexer.indexOf(holeCards);
			final double[][] eq = equities[indexInTables];
			for (int i = 0; i < 4; i++) {
				System.arraycopy(eq[i], 0, destEquities[i], 0, 3);
			}
			return;
		}
		final CardsPermutator permutator = getPermutator(heroIndex, vilain1Index, vilain2Index);
		permutator.permute(holeCards, tmpHoleCards);
		final int indexInTables = threePlayersIndexer.indexOf(tmpHoleCards);
		permutator.permuteInverse(equities[indexInTables], destEquities);
	}

	public void compute(final ThreePlayersPreflopEquityTables tables) {
		final double[][][] equities = tables.getEquities();
		final int nbHoleCards = this.nbHoleCards;
		final double[][][][][] reducedEquities = this.reducedEquities;
		final int[][][] counts = new int[nbHoleCards][nbHoleCards][nbHoleCards];
		final IntCardsSpec indexSpecs = holeCardsIndexer.getCardsSpec();
		final Deck52Cards deck = new Deck52Cards(indexSpecs);
		final WaughIndexer onePlayerIndexer = new WaughIndexer(onePlayerGroupsSize);
		final int[][] tmpHoleCards = new int[3][2];
		final double[][] tmpEquities = new double[4][3];
		deck.drawAllGroupsCombinations(threePlayersGroupsSize, new CardsGroupsDrawingTask() {

			@Override
			public boolean doTask(int[][] cardsGroups) {
				final int heroIndex = onePlayerIndexer.indexOf(new int[][] { cardsGroups[0] });
				final int vilain1Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[1] });
				final int vilain2Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[2] });
				getEquities(equities, cardsGroups, heroIndex, vilain1Index, vilain2Index, tmpHoleCards, tmpEquities);

				final double[][] dest = reducedEquities[heroIndex][vilain1Index][vilain2Index];
				for (int i = 0; i < 4; i++) {
					final double[] destI = dest[i];
					final double[] resultsI = tmpEquities[i];
					for (int j = 0; j < 3; j++) {
						destI[j] += resultsI[j];
					}
				}
				counts[heroIndex][vilain1Index][vilain2Index]++;
				return true;
			}
		});
		for (int i = 0; i < nbHoleCards; i++) {
			final double[][][][] heroEquities = reducedEquities[i];
			final int[][] heroCounts = counts[i];
			for (int j = 0; j < nbHoleCards; j++) {
				final double[][][] heroVilain1Equities = heroEquities[j];
				final int[] heroVilain1Counts = heroCounts[j];
				for (int k = 0; k < nbHoleCards; k++) {
					final double[][] heroVilain1Vilain2Equities = heroVilain1Equities[k];
					final int count = heroVilain1Counts[k];
					for (int l = 0; l < 4; l++) {
						final double[] eq = heroVilain1Vilain2Equities[l];
						for (int m = 0; m < 3; m++) {
							eq[m] /= count;
						}
					}
				}
			}
		}
	}

	@Override
	public void fill(InputStream is) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void write(OutputStream os) throws IOException {
		// TODO Auto-generated method stub

	}
}