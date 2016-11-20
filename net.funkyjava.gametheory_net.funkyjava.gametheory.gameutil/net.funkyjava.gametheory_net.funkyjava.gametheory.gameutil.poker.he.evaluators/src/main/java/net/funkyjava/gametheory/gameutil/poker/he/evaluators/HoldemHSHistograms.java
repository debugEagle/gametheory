package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.lang3.mutable.MutableLong;

import net.funkyjava.gametheory.gameutil.cards.CardsGroupsDrawingTask;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

public class HoldemHSHistograms {

	public enum HoldemHSHistogramsStreets {
		PREFLOP, FLOP, TURN
	}

	public enum HoldemHSHistogramsValues {
		HS, EHS, EHS2
	}

	public static double[][] generateHistograms(final HoldemHSHistogramsStreets street,
			final HoldemHSHistogramsValues nextStreetValue, final int numberOfBars) {
		if (!AllHoldemHSTables.isFilled()) {
			throw new IllegalArgumentException("AllHoldemHSTables must be filled before");
		}
		WaughIndexer streetIndexer;
		WaughIndexer nextStreetIndexer;
		double[] nextStreetValues;
		int numberOfCardsToAddForNextStreet;
		switch (street) {
		case FLOP:
			streetIndexer = new WaughIndexer(new int[] { 2, 3 });
			nextStreetIndexer = new WaughIndexer(new int[] { 2, 4 });
			numberOfCardsToAddForNextStreet = 1;
			switch (nextStreetValue) {
			case EHS:
				nextStreetValues = AllHoldemHSTables.getTurnEHSTable();
				break;
			case EHS2:
				nextStreetValues = AllHoldemHSTables.getTurnEHS2Table();
				break;
			case HS:
				nextStreetValues = AllHoldemHSTables.getTurnHSTable();
				break;
			default:
				throw new IllegalArgumentException("Impossible case");

			}
			break;
		case PREFLOP:
			streetIndexer = new WaughIndexer(new int[] { 2 });
			nextStreetIndexer = new WaughIndexer(new int[] { 2, 3 });
			numberOfCardsToAddForNextStreet = 3;
			switch (nextStreetValue) {
			case EHS:
				nextStreetValues = AllHoldemHSTables.getFlopEHSTable();
				break;
			case EHS2:
				nextStreetValues = AllHoldemHSTables.getFlopEHS2Table();
				break;
			case HS:
				nextStreetValues = AllHoldemHSTables.getFlopHSTable();
			default:
				throw new IllegalArgumentException("Impossible case");
			}
			break;
		case TURN:
			streetIndexer = new WaughIndexer(new int[] { 2, 4 });
			nextStreetIndexer = new WaughIndexer(new int[] { 2, 5 });
			numberOfCardsToAddForNextStreet = 1;
			switch (nextStreetValue) {
			case HS:
				nextStreetValues = AllHoldemHSTables.getRiverHSTable();
				break;
			case EHS:
			case EHS2:
			default:
				throw new IllegalArgumentException("Impossible case");
			}
			break;
		default:
			throw new IllegalArgumentException("Impossible case");
		}
		return generateHistograms(streetIndexer, nextStreetIndexer, nextStreetValues, numberOfCardsToAddForNextStreet,
				numberOfBars);
	}

	private static double[][] generateHistograms(final WaughIndexer streetIndexer, final WaughIndexer nextStreetIndexer,
			final double[] nextStreetValues, final int numberOfCardsToAddForNextStreet, final int numberOfBars) {
		final int streetSize = streetIndexer.getIndexSize();
		checkArgument(nextStreetValues.length == nextStreetIndexer.getIndexSize(),
				"Next street values count != next street indexer size");
		final long[] streetCards = new long[2];
		final long[] nextStreetCards = new long[2];
		final double[][] histograms = new double[streetSize][numberOfBars];
		final Deck52Cards deck = new Deck52Cards(0);
		for (int streetIdx = 0; streetIdx < streetSize; streetIdx++) {
			final double[] vector = histograms[streetIdx];
			streetIndexer.unindex(streetIdx, streetCards);
			nextStreetCards[0] = streetCards[0];
			final long deckMask = streetCards[0] | streetCards[1];
			final MutableLong nbHits = new MutableLong();
			deck.drawAllGroupsCombinations(new int[] { numberOfCardsToAddForNextStreet }, new CardsGroupsDrawingTask() {

				@Override
				public boolean doTask(int[][] cardsGroups) {
					final int[] cards = cardsGroups[0];
					long nextStreetMask = 0l;
					for (int i = 0; i < numberOfCardsToAddForNextStreet; i++) {
						final int card = cards[i];
						nextStreetMask |= 0x1l << (16 * (card / 13) + card % 13);
						if ((nextStreetMask & deckMask) != 0) {
							return true;
						}
					}
					nextStreetCards[1] = nextStreetMask | streetCards[1];
					final int nextStreetIndex = nextStreetIndexer.index(nextStreetCards);
					vector[(int) Math.round(nextStreetValues[nextStreetIndex] * (numberOfBars - 1))]++;
					nbHits.increment();
					return true;
				}
			});
			final double hits = nbHits.doubleValue();
			for (int i = 0; i < numberOfBars; i++) {
				vector[i] /= hits;
			}
		}
		return histograms;
	}
}
