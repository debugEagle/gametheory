package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.MutableLong;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Cards52SpecTranslator;
import net.funkyjava.gametheory.gameutil.cards.CardsGroupsDrawingTask;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.cards.DefaultIntCardsSpecs;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.twoplustwo.TwoPlusTwoEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

@Slf4j
public class ThreePlayersPreflopEquityTables implements Serializable {

	private static final long serialVersionUID = 1808572853794466312L;
	private static final int[] onePlayerGroupsSize = { 2 };
	private static final int[] threePlayersGroupsSize = { 2, 2, 2 };

	@Getter
	private final WaughIndexer threePlayersIndexer = new WaughIndexer(threePlayersGroupsSize);
	@Getter
	private final int nbPreflopThreePlayers = threePlayersIndexer.getIndexSize();
	@Getter
	private final WaughIndexer holeCardsIndexer = new WaughIndexer(onePlayerGroupsSize);
	@Getter
	private final int nbHoleCards = holeCardsIndexer.getIndexSize();

	public static final int heroVilain1Vilain2Index = 0;
	public static final int heroVilain1Index = 1;
	public static final int heroVilain2Index = 2;
	public static final int vilain1Vilain2Index = 3;

	@Getter
	private final double[][][][][] reducedEquities = new double[nbHoleCards][nbHoleCards][nbHoleCards][4][3];
	@Getter
	private final int[][][] reducedCounts = new int[nbHoleCards][nbHoleCards][nbHoleCards];
	@Getter
	private final double[][][] equities = new double[nbPreflopThreePlayers][][];

	private long total = 0;

	public boolean isComputed() {
		return equities[0] != null;
	}

	public void compute() throws InterruptedException {
		checkState(!isComputed(), "Tables have already been computed");
		computeAccurateEquities();
		computeReducedEquities();
	}

	@AllArgsConstructor
	private static final class CardsPermutator {

		private final int sourceIndex1, sourceIndex2, sourceIndex3;

		public final int[][] permute(int[][] source) {
			final int[][] dest = new int[3][];
			dest[0] = source[sourceIndex1];
			dest[1] = source[sourceIndex2];
			dest[2] = source[sourceIndex3];
			return dest;
		}

		private final void permute(final double[] source, final double[] dest) {
			dest[0] = source[sourceIndex1];
			dest[1] = source[sourceIndex2];
			dest[2] = source[sourceIndex3];
		}

		public final void permute(final double[][] source, final double[][] destination) {
			permute(source[0], destination[0]);
			permute(source[1], destination[1]);
			permute(source[2], destination[2]);
			permute(source[3], destination[3]);
		}
	}

	private final void computeAccurateEquities() throws InterruptedException {
		new TwoPlusTwoEvaluator(); // Just to load it before we get started
		final long start = System.currentTimeMillis();
		final MutableLong done = new MutableLong();
		final MutableLong enqueued = new MutableLong();
		final ExecutorService exe = Executors
				.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		final WaughIndexer threePlayersIndexer = this.threePlayersIndexer;
		final CardsPermutator permutator1 = new CardsPermutator(0, 2, 1);
		final CardsPermutator permutator2 = new CardsPermutator(1, 2, 0);
		final CardsPermutator permutator3 = new CardsPermutator(1, 0, 2);
		final CardsPermutator permutator4 = new CardsPermutator(2, 0, 1);
		final CardsPermutator permutator5 = new CardsPermutator(2, 1, 0);
		final double[][][] equities = this.equities;
		final int nbPreflopThreePlayers = this.nbPreflopThreePlayers;
		final int nbTasks = nbPreflopThreePlayers / 6;
		for (int index = 0; index < nbPreflopThreePlayers; index++) {
			if (equities[index] != null) {
				continue;
			}
			final int finalIndex = index;
			final TwoPlusTwoEvaluator eval = new TwoPlusTwoEvaluator();
			final Cards52SpecTranslator translateToEval = new Cards52SpecTranslator(threePlayersIndexer.getCardsSpec(),
					eval.getCardsSpec());
			final int[][] holeCards = new int[3][2];
			threePlayersIndexer.unindex(index, holeCards);

			final int[][] reversedHoleCards1 = permutator1.permute(holeCards);
			final int[][] reversedHoleCards2 = permutator2.permute(holeCards);
			final int[][] reversedHoleCards3 = permutator3.permute(holeCards);
			final int[][] reversedHoleCards4 = permutator4.permute(holeCards);
			final int[][] reversedHoleCards5 = permutator5.permute(holeCards);

			final int reversedIndex1 = threePlayersIndexer.indexOf(reversedHoleCards1);
			final int reversedIndex2 = threePlayersIndexer.indexOf(reversedHoleCards2);
			final int reversedIndex3 = threePlayersIndexer.indexOf(reversedHoleCards3);
			final int reversedIndex4 = threePlayersIndexer.indexOf(reversedHoleCards4);
			final int reversedIndex5 = threePlayersIndexer.indexOf(reversedHoleCards5);

			translateToEval.translate(holeCards);
			total++;
			exe.execute(new Runnable() {
				@Override
				public void run() {
					final int[] heroCards = new int[7];
					heroCards[0] = holeCards[0][0];
					heroCards[1] = holeCards[0][1];
					final int[] vilain1Cards = new int[7];
					vilain1Cards[0] = holeCards[1][0];
					vilain1Cards[1] = holeCards[1][1];
					final int[] vilain2Cards = new int[7];
					vilain2Cards[0] = holeCards[2][0];
					vilain2Cards[1] = holeCards[2][1];

					final Deck52Cards evalDeck = new Deck52Cards(eval.getCardsSpec());
					// 0 : three players
					// 1 : hero + vilain1 (vilain2 fold)
					// 2 : hero + vilain2 (vilain1 fold)
					// 3 : vilain1 + vilain2 (hero fold)
					final double[][] handEquities = new double[4][3];
					final double[][] reversedEquities1 = new double[4][3];
					final double[][] reversedEquities2 = new double[4][3];
					final double[][] reversedEquities3 = new double[4][3];
					final double[][] reversedEquities4 = new double[4][3];
					final double[][] reversedEquities5 = new double[4][3];

					equities[finalIndex] = handEquities;
					equities[reversedIndex1] = reversedEquities1;
					equities[reversedIndex2] = reversedEquities2;
					equities[reversedIndex3] = reversedEquities3;
					equities[reversedIndex4] = reversedEquities4;
					equities[reversedIndex5] = reversedEquities5;
					final int[] heroVilain1 = new int[3];
					final int[] heroVilain2 = new int[3];
					final int[] vilain1Vilain2 = new int[3];
					final int[] threePlayers = new int[7];
					// 0: hero wins
					// 1: vilain1 wins
					// 2: vilain2 wins
					// 3: split 3 player
					// 4: split hero/vilain1
					// 5: split hero/vilain2,
					// 6: split vilain1/vilain2
					evalDeck.drawAllGroupsCombinations(new int[] { 5 }, new CardsGroupsDrawingTask() {

						@Override
						public boolean doTask(int[][] cardsGroups) {
							final int[] board = cardsGroups[0];
							System.arraycopy(board, 0, heroCards, 2, 5);
							System.arraycopy(board, 0, vilain1Cards, 2, 5);
							System.arraycopy(board, 0, vilain2Cards, 2, 5);
							final int heroVal = eval.get7CardsEval(heroCards);
							final int vilain1Val = eval.get7CardsEval(vilain1Cards);
							final int vilain2Val = eval.get7CardsEval(vilain2Cards);

							final boolean heroBeatsVilain1 = heroVal > vilain1Val;
							final boolean heroBeatsVilain2 = heroVal > vilain2Val;
							final boolean vilain1BeatsHero = vilain1Val > heroVal;
							final boolean vilain1BeatsVilain2 = vilain1Val > vilain2Val;
							final boolean vilain2BeatsHero = vilain2Val > heroVal;
							final boolean vilain2BeatsVilain1 = vilain2Val > vilain1Val;

							if (heroBeatsVilain1 && heroBeatsVilain2) {
								threePlayers[0]++;
							} else if (vilain2BeatsHero && vilain2BeatsVilain1) {
								threePlayers[2]++;
							} else if (vilain1BeatsHero && vilain1BeatsVilain2) {
								threePlayers[1]++;
							} else {
								// we have an equality, three players will tie
								if (heroVal == vilain1Val && heroVal == vilain2Val) {
									threePlayers[3]++;
								} else {
									// Nope, two players will tie, one will lose
									if (heroVal == vilain1Val) {
										threePlayers[4]++;
									} else if (heroVal == vilain2Val) {
										threePlayers[5]++;
									} else {
										threePlayers[6]++;
									}
								}
							}
							if (heroBeatsVilain1) {
								heroVilain1[0]++;
							} else if (vilain1BeatsHero) {
								heroVilain1[1]++;
							} else {
								heroVilain1[2]++;
							}
							if (heroBeatsVilain2) {
								heroVilain2[0]++;
							} else if (vilain2BeatsHero) {
								heroVilain2[1]++;
							} else {
								heroVilain2[2]++;
							}
							if (vilain1BeatsVilain2) {
								vilain1Vilain2[0]++;
							} else if (vilain2BeatsVilain1) {
								vilain1Vilain2[1]++;
							} else {
								vilain1Vilain2[2]++;
							}
							return true;
						}
					}, holeCards[0], holeCards[1], holeCards[2]);
					// Fill equity when 3 players in showdown
					// 0: hero wins
					// 1: vilain1 wins
					// 2: vilain2 wins
					// 3: split 3 player
					// 4: split hero/vilain1
					// 5: split hero/vilain2,
					// 6: split vilain1/vilain2
					final double[] threePlayersEq = handEquities[heroVilain1Vilain2Index];
					threePlayersEq[0] = threePlayers[0] + threePlayers[3] / 3d
							+ (threePlayers[4] + threePlayers[5]) / 2d;
					threePlayersEq[1] = threePlayers[1] + threePlayers[3] / 3d
							+ (threePlayers[4] + threePlayers[6]) / 2d;
					threePlayersEq[2] = threePlayers[2] + threePlayers[3] / 3d
							+ (threePlayers[5] + threePlayers[6]) / 2d;

					final double[] vilain2FoldsEq = handEquities[heroVilain1Index];
					final int hV1Win = heroVilain1[0];
					final int hV1Lose = heroVilain1[1];
					final int hV1Tie = heroVilain1[1];
					vilain2FoldsEq[1] = 1 - (vilain2FoldsEq[0] = (hV1Win + hV1Tie / 2d) / (hV1Win + hV1Tie + hV1Lose));

					final double[] vilain1FoldsEq = handEquities[heroVilain2Index];
					final int hV2Win = heroVilain2[0];
					final int hV2Lose = heroVilain2[1];
					final int hV2Tie = heroVilain2[1];
					vilain1FoldsEq[2] = 1 - (vilain1FoldsEq[0] = (hV2Win + hV2Tie / 2d) / (hV2Win + hV2Tie + hV2Lose));

					final double[] heroFoldsEq = handEquities[vilain1Vilain2Index];
					final int v1V2Win = vilain1Vilain2[0];
					final int v1V2Lose = vilain1Vilain2[1];
					final int v1V2Tie = vilain1Vilain2[1];
					heroFoldsEq[2] = 1 - (heroFoldsEq[1] = (v1V2Win + v1V2Tie / 2d) / (v1V2Win + v1V2Tie + v1V2Lose));

					permutator1.permute(handEquities, reversedEquities1);
					permutator2.permute(handEquities, reversedEquities2);
					permutator3.permute(handEquities, reversedEquities3);
					permutator4.permute(handEquities, reversedEquities4);
					permutator5.permute(handEquities, reversedEquities5);
					synchronized (enqueued) {
						done.increment();
						final long doneLong = done.longValue();
						final double ratioDone = doneLong / (double) nbTasks;
						if (doneLong % 1000 == 0 && ratioDone != 0 && ratioDone != 1) {
							final long elapsed = System.currentTimeMillis() - start;
							log.info("Remaining operations {}/{}, time {}s", nbTasks - doneLong, nbTasks,
									(int) (elapsed * (1 - ratioDone) / (1000 * ratioDone)));
						}
						enqueued.decrement();
						if (enqueued.getValue() < 100) {
							enqueued.notify();
						}
					}

				}
			});
			synchronized (enqueued) {
				enqueued.increment();
				if (enqueued.longValue() >= 1000) {
					log.info("Feeder : Reenqueued at least 1000 runnables, total enqueued {}", total);
					enqueued.wait();
					log.info("Feeder : waking up to enqueue more runnables");
				}
			}
		}
		log.info("Feeder : end enqueuing {} runnables, awaiting termination", nbTasks);
		exe.shutdown();
		exe.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		log.info("All runnables were executed");
	}

	private void computeReducedEquities() {
		final int nbHoleCards = this.nbHoleCards;
		final double[][][][][] reducedEquities = this.reducedEquities;
		final int[][][] counts = this.reducedCounts;
		final IntCardsSpec indexSpecs = holeCardsIndexer.getCardsSpec();
		final double[][][] equities = this.equities;
		final Deck52Cards deck = new Deck52Cards(indexSpecs);
		final WaughIndexer onePlayerIndexer = new WaughIndexer(onePlayerGroupsSize);
		final WaughIndexer threePlayersIndexer = new WaughIndexer(threePlayersGroupsSize);
		deck.drawAllGroupsCombinations(threePlayersGroupsSize, new CardsGroupsDrawingTask() {

			@Override
			public boolean doTask(int[][] cardsGroups) {

				final int indexInTables = threePlayersIndexer.indexOf(cardsGroups);
				final int heroIndex = onePlayerIndexer.indexOf(new int[][] { cardsGroups[0] });
				final int vilain1Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[1] });
				final int vilain2Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[2] });
				final double[][] results = equities[indexInTables];
				final double[][] dest = reducedEquities[heroIndex][vilain1Index][vilain2Index];
				for (int i = 0; i < 4; i++) {
					final double[] destI = dest[i];
					final double[] resultsI = results[i];
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

	public double[][] getEquities(int[] heroCards, int[] vilain1Cards, int[] vilain2Cards) {
		return equities[threePlayersIndexer.indexOf(new int[][] { heroCards, vilain1Cards, vilain2Cards })];
	}

	public double[][] getReducedEquities(final int[] heroCards, final int[] vilain1Cards, final int[] vilain2Cards) {
		return reducedEquities[holeCardsIndexer.indexOf(new int[][] { heroCards })][holeCardsIndexer
				.indexOf(new int[][] { vilain1Cards })][holeCardsIndexer.indexOf(new int[][] { vilain2Cards })];
	}

	public int getReducedCount(final int[] heroCards, final int[] vilain1Cards, final int[] vilain2Cards) {
		return reducedCounts[holeCardsIndexer.indexOf(new int[][] { heroCards })][holeCardsIndexer
				.indexOf(new int[][] { vilain1Cards })][holeCardsIndexer.indexOf(new int[][] { vilain2Cards })];
	}

	public IntCardsSpec getCardsSpec() {
		return DefaultIntCardsSpecs.getDefault();
	}

	public static void main(String[] args) {
		checkArgument(args.length == 1, "3 Players Preflop Tables writing misses a path argument");
		final String pathStr = args[0];
		final Path path = Paths.get(pathStr);
		checkArgument(!Files.exists(path), "File " + path.toAbsolutePath().toString() + " already exists");
		try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path.toFile()))) {
			final ThreePlayersPreflopEquityTables tables = new ThreePlayersPreflopEquityTables();
			tables.compute();
			oos.writeObject(tables);
			oos.flush();
			oos.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
