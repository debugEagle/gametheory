package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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

	@Getter
	private final int[][][][][] reducedWinLoseTie2Tie3 = new int[nbHoleCards][nbHoleCards][nbHoleCards][3][4];
	@Getter
	private final int[][][] winLoseTie2Tie3 = new int[nbPreflopThreePlayers][][];

	private long total = 0;
	private long start;

	public boolean isComputed() {
		return winLoseTie2Tie3[0] != null;
	}

	public void compute() throws InterruptedException {
		checkState(!isComputed(), "Tables have already been computed");
		computeAccurateWLT();
		computeReducedWLT();
	}

	@AllArgsConstructor
	private static final class GainsPermutator {

		private final int sourceIndex1, sourceIndex2, sourceIndex3;

		public final int[][] permute(int[][] source) {
			final int[][] dest = new int[3][];
			dest[0] = source[sourceIndex1];
			dest[1] = source[sourceIndex2];
			dest[2] = source[sourceIndex3];
			return dest;
		}
	}

	private final void computeAccurateWLT() throws InterruptedException {
		start = System.currentTimeMillis();
		final MutableLong done = new MutableLong();
		final MutableLong enqueued = new MutableLong();
		final ExecutorService exe = Executors
				.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		final WaughIndexer threePlayersIndexer = this.threePlayersIndexer;
		final GainsPermutator permutator1 = new GainsPermutator(0, 2, 1);
		final GainsPermutator permutator2 = new GainsPermutator(1, 2, 0);
		final GainsPermutator permutator3 = new GainsPermutator(1, 0, 2);
		final GainsPermutator permutator4 = new GainsPermutator(2, 0, 1);
		final GainsPermutator permutator5 = new GainsPermutator(2, 1, 0);
		final int[][][] winLoseTie2Tie3 = this.winLoseTie2Tie3;
		final int nbPreflopThreePlayers = this.nbPreflopThreePlayers;
		final int nbTasks = nbPreflopThreePlayers / 6;
		for (int index = 0; index < nbPreflopThreePlayers; index++) {
			if ((index % 100_000) == 0) {
				log.info("Already put {}/{} runnables in queue, at index {}/{}", total, nbTasks, index,
						nbPreflopThreePlayers);
			}
			if (winLoseTie2Tie3[index] != null) {
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
					final int[][] gains = new int[3][4];

					winLoseTie2Tie3[finalIndex] = gains;
					winLoseTie2Tie3[reversedIndex1] = permutator1.permute(gains);
					winLoseTie2Tie3[reversedIndex2] = permutator2.permute(gains);
					winLoseTie2Tie3[reversedIndex3] = permutator3.permute(gains);
					winLoseTie2Tie3[reversedIndex4] = permutator4.permute(gains);
					winLoseTie2Tie3[reversedIndex5] = permutator5.permute(gains);

					final int[] heroGains = gains[0];
					final int[] vilain1Gains = gains[1];
					final int[] vilain2Gains = gains[2];

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
								heroGains[0]++;
								vilain1Gains[1]++;
								vilain2Gains[1]++;
							} else if (vilain2BeatsHero && vilain2BeatsVilain1) {
								heroGains[1]++;
								vilain1Gains[1]++;
								vilain2Gains[0]++;
							} else if (vilain1BeatsHero && vilain1BeatsVilain2) {
								heroGains[1]++;
								vilain1Gains[0]++;
								vilain2Gains[1]++;
							} else {
								// we have an equality, three players will tie
								if (heroVal == vilain1Val && heroVal == vilain2Val) {
									heroGains[3]++;
									vilain1Gains[3]++;
									vilain2Gains[3]++;
								} else {
									// Nope, two players will tie, one will lose
									if (heroVal == vilain1Val) {
										heroGains[2]++;
										vilain1Gains[2]++;
										vilain2Gains[1]++;
									} else if (heroVal == vilain2Val) {
										heroGains[2]++;
										vilain1Gains[1]++;
										vilain2Gains[2]++;
									} else {
										heroGains[1]++;
										vilain1Gains[2]++;
										vilain2Gains[2]++;
									}
								}
							}
							return true;
						}
					}, holeCards[0], holeCards[1], holeCards[2]);
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

	private void computeReducedWLT() {
		final int[][][][][] reducedWinLoseTie2Tie3 = this.reducedWinLoseTie2Tie3;
		final IntCardsSpec indexSpecs = holeCardsIndexer.getCardsSpec();
		final int[][][] tables = this.winLoseTie2Tie3;
		final Deck52Cards deck = new Deck52Cards(indexSpecs);
		final WaughIndexer onePlayerIndexer = new WaughIndexer(onePlayerGroupsSize);
		final WaughIndexer twoPlayersIndexer = new WaughIndexer(threePlayersGroupsSize);
		deck.drawAllGroupsCombinations(threePlayersGroupsSize, new CardsGroupsDrawingTask() {

			@Override
			public boolean doTask(int[][] cardsGroups) {

				final int indexInTables = twoPlayersIndexer.indexOf(cardsGroups);
				final int heroIndex = onePlayerIndexer.indexOf(new int[][] { cardsGroups[0] });
				final int vilain1Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[1] });
				final int vilain2Index = onePlayerIndexer.indexOf(new int[][] { cardsGroups[2] });
				final int[][] results = tables[indexInTables];
				final int[][] dest = reducedWinLoseTie2Tie3[heroIndex][vilain1Index][vilain2Index];
				for (int i = 0; i < 3; i++) {
					final int[] destI = dest[i];
					final int[] resultsI = results[i];
					for (int j = 0; j < 4; j++) {
						destI[j] += resultsI[j];
					}
				}
				return true;
			}
		});
	}

	public int[][] getWinLoseTie2Tie3(int[] heroCards, int[] opponentCards) {
		return winLoseTie2Tie3[threePlayersIndexer.indexOf(new int[][] { heroCards, opponentCards })];
	}

	public int[][] getReducedWinLoseTie2Tie3(final int[] heroCards, final int[] vilain1Cards,
			final int[] vilain2Cards) {
		return reducedWinLoseTie2Tie3[holeCardsIndexer.indexOf(new int[][] { heroCards })][holeCardsIndexer
				.indexOf(new int[][] { vilain1Cards })][holeCardsIndexer.indexOf(new int[][] { vilain2Cards })];
	}

	public IntCardsSpec getCardsSpec() {
		return DefaultIntCardsSpecs.getDefault();
	}

	public static void main(String[] args) {
		checkArgument(args.length == 1, "3 Players Preflop Tables writing misses a path argument");
		final String path = args[0];
		try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(Paths.get(path).toFile()))) {
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
