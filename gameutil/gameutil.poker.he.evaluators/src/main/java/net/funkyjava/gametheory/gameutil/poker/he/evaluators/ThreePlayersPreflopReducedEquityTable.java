package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Cards52Strings;
import net.funkyjava.gametheory.gameutil.cards.CardsGroupsDrawingTask;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;
import net.funkyjava.gametheory.io.Fillable;
import net.funkyjava.gametheory.io.IOUtils;

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
	private static final class Permutator {

		private final int sourceIndex1, sourceIndex2, sourceIndex3;

		private final void inverseOrderEquities(final double[] source, final double[] dest) {
			dest[sourceIndex1] = source[0];
			dest[sourceIndex2] = source[1];
			dest[sourceIndex3] = source[2];
		}

		public final void permuteInverse(final double[][] source, final double[][] destination) {
			inverseOrderEquities(source[0], destination[0]);
			inverseOrderEquities(source[1], destination[1]);
			inverseOrderEquities(source[2], destination[2]);
			inverseOrderEquities(source[3], destination[3]);
		}
	}

	private static final Permutator permutator123 = new Permutator(0, 1, 2);
	private static final Permutator permutator132 = new Permutator(0, 2, 1);
	private static final Permutator permutator231 = new Permutator(1, 2, 0);
	private static final Permutator permutator213 = new Permutator(1, 0, 2);
	private static final Permutator permutator312 = new Permutator(2, 0, 1);
	private static final Permutator permutator321 = new Permutator(2, 1, 0);

	private static final Permutator getPermutator(final int h1, final int h2, final int h3) {
		if (h1 <= h2 && h2 <= h3) {
			return permutator123;
		} else if (h1 <= h3 && h3 <= h2) {
			return permutator132;
		} else if (h2 <= h3 && h3 <= h1) {
			return permutator231;
		} else if (h2 <= h1 && h1 <= h3) {
			return permutator213;
		} else if (h3 <= h1 && h1 <= h2) {
			return permutator312;
		} else if (h3 <= h2 && h2 <= h1) {
			return permutator321;
		}

		throw new IllegalArgumentException();
	}

	public void compute(final ThreePlayersPreflopEquityTables tables) {
		checkArgument(!computed, "Already computed");
		final double[][][] equities = tables.getEquities();
		final int nbHoleCards = this.nbHoleCards;
		final double[][][][][] reducedEquities = this.reducedEquities;
		final IntCardsSpec indexSpecs = holeCardsIndexer.getCardsSpec();
		final Deck52Cards deck = new Deck52Cards(indexSpecs);
		final WaughIndexer onePlayerIndexer = new WaughIndexer(onePlayerGroupsSize);
		deck.drawAllGroupsCombinations(threePlayersGroupsSize, new CardsGroupsDrawingTask() {

			@Override
			public boolean doTask(int[][] cardsGroups) {
				final int heroIndex = onePlayerIndexer.indexOf(new int[][] { cardsGroups[0] });
				final int vilain1Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[1] });
				if (vilain1Index < heroIndex) {
					return true;
				}
				final int vilain2Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[2] });
				if (vilain2Index < vilain1Index) {
					return true;
				}
				final int indexInTables = threePlayersIndexer.indexOf(cardsGroups);
				final double[][] eq = equities[indexInTables];
				final double[][] dest = reducedEquities[heroIndex][vilain1Index][vilain2Index];
				for (int i = 0; i < 4; i++) {
					final double[] destI = dest[i];
					final double[] resultsI = eq[i];
					for (int j = 0; j < 3; j++) {
						destI[j] += resultsI[j];
					}
				}
				return true;
			}
		});
		for (int i = 0; i < nbHoleCards; i++) {
			final double[][][][] heroEquities = reducedEquities[i];
			for (int j = i; j < nbHoleCards; j++) {
				final double[][][] heroVilain1Equities = heroEquities[j];
				for (int k = j; k < nbHoleCards; k++) {
					final double[][] heroVilain1Vilain2Equities = heroVilain1Equities[k];
					for (int l = 0; l < 4; l++) {
						final double[] eq = heroVilain1Vilain2Equities[l];
						final double total = eq[0] + eq[1] + eq[2];
						for (int m = 0; m < 3; m++) {
							eq[m] /= total;
						}
					}
				}
			}
		}
		for (int i = 0; i < nbHoleCards; i++) {
			final double[][][][] heroEquities = reducedEquities[i];
			for (int j = 0; j < nbHoleCards; j++) {
				final double[][][] heroVilain1Equities = heroEquities[j];
				for (int k = 0; k < nbHoleCards; k++) {
					if (i <= j && j <= k) {
						continue;
					}
					final int[] ordered = getOrdered(i, j, k);
					final Permutator permutator = getPermutator(i, j, k);
					final double[][] heroVilain1Vilain2Equities = heroVilain1Equities[k];
					permutator.permuteInverse(reducedEquities[ordered[0]][ordered[1]][ordered[2]],
							heroVilain1Vilain2Equities);
				}
			}
		}
	}

	private static final int[] getOrdered(final int i, final int j, final int k) {
		if (i <= j && j <= k) {
			return new int[] { i, j, k };
		}
		if (i <= k && k <= j) {
			return new int[] { i, k, j };
		}
		if (j <= i && i <= k) {
			return new int[] { j, i, k };
		}
		if (j <= k && k <= i) {
			return new int[] { j, k, i };
		}
		if (k <= i && i <= j) {
			return new int[] { k, i, j };
		}
		if (k <= j && j <= i) {
			return new int[] { k, j, i };
		}
		throw new IllegalArgumentException();
	}

	@Override
	public void fill(InputStream is) throws IOException {
		IOUtils.fill(is, reducedEquities);

	}

	@Override
	public void write(OutputStream os) throws IOException {
		IOUtils.write(os, reducedEquities);
	}

	public static void testPermut(int i, int j, int k) {
		final double[] orderedEq = new double[] { 1, 2, 3 };
		final double[] dest = new double[3];
		log.info("# {} - {} - {}", i, j, k);
		final int[] ordered = getOrdered(i, j, k);
		log.info("# ordered {}", ordered);

		final Permutator permutator = getPermutator(i, j, k);
		permutator.inverseOrderEquities(orderedEq, dest);
		log.info("Permuted back equities : {}", dest);
	}

	private void interactive() {
		final Cards52Strings strs = new Cards52Strings(holeCardsIndexer.getCardsSpec());
		try (final Scanner scanner = new Scanner(System.in)) {
			while (true) {
				final String line = scanner.nextLine();
				if (line.equals("exit")) {
					return;
				}
				try {
					final String[] handsStr = line.split(" ");
					final int[][] c1 = new int[][] { strs.getCards(handsStr[0]) };
					final int[][] c2 = new int[][] { strs.getCards(handsStr[1]) };
					final int[][] c3 = new int[][] { strs.getCards(handsStr[2]) };
					final int i1 = holeCardsIndexer.indexOf(c1);
					final int i2 = holeCardsIndexer.indexOf(c2);
					final int i3 = holeCardsIndexer.indexOf(c3);
					log.info("{}", (Object[]) reducedEquities[i1][i2][i3]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) throws FileNotFoundException, IOException {

		checkArgument(args.length == 2,
				"3 Players Preflop Tables writing misses a path argument, expected source and destination");
		final ThreePlayersPreflopReducedEquityTable table = new ThreePlayersPreflopReducedEquityTable();
		try (final FileInputStream fis = new FileInputStream(args[1])) {
			table.fill(fis);
			table.interactive();
		}

		// final String pathStr = args[0];
		// final Path srcPath = Paths.get(pathStr);
		// checkArgument(Files.exists(srcPath), "File " +
		// srcPath.toAbsolutePath().toString() + " doesn't exist");
		// final Path destPath = Paths.get(args[1]);
		// final ThreePlayersPreflopEquityTables fullTables = new
		// ThreePlayersPreflopEquityTables();
		// log.info("Filling exact equities table");
		// try (final FileInputStream fis = new
		// FileInputStream(srcPath.toFile())) {
		// fullTables.fill(fis);
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// System.exit(-1);
		// }
		// log.info("Computing reduced equities");
		// final ThreePlayersPreflopReducedEquityTable table = new
		// ThreePlayersPreflopReducedEquityTable();
		// table.compute(fullTables);
		// log.info("Writing reduced equities");
		// try (final FileOutputStream fos = new
		// FileOutputStream(destPath.toFile())) {
		// table.write(fos);
		// } catch (Exception e) {
		// e.printStackTrace();
		// System.exit(-1);
		// }
	}
}