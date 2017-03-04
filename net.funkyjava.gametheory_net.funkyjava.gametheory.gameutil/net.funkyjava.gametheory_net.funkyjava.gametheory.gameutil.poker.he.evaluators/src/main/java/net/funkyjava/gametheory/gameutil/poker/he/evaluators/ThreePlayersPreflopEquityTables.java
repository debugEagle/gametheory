package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
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

	public static final int heroVilain1Vilain2_heroIndex = 0;
	public static final int heroVilain1Vilain2_vilain1Index = 1;
	public static final int heroVilain1_heroIndex = 2;
	public static final int heroVilain2_heroIndex = 3;
	public static final int vilain1Vilain2_vilain1Index = 4;

	@Getter
	private double[][] equities;
	@Getter
	private double[][][][] reducedEquities;
	@Getter
	private int[][][] reducedCounts;

	private long total = 0;

	public ThreePlayersPreflopEquityTables() {
		reducedEquities = new double[nbHoleCards][nbHoleCards][nbHoleCards][5];
		reducedCounts = new int[nbHoleCards][nbHoleCards][nbHoleCards];
		equities = new double[nbPreflopThreePlayers][];
	}

	private ThreePlayersPreflopEquityTables(final double[][] equities, final double[][][][] reducedEquities,
			final int[][][] reducedCounts) {
		this.reducedEquities = reducedEquities;
		this.reducedCounts = reducedCounts;
		this.equities = equities;
	}

	public boolean isComputed() {
		return equities[0] != null;
	}

	public void compute() throws InterruptedException {
		checkState(!isComputed(), "Tables have already been computed");
		computeAccurateEquities();
		computeReducedEquities();
	}

	@AllArgsConstructor
	private static abstract class Permutator {

		private final int sourceIndex1, sourceIndex2, sourceIndex3;

		public final int[][] permute(int[][] source) {
			final int[][] dest = new int[3][];
			dest[0] = source[sourceIndex1];
			dest[1] = source[sourceIndex2];
			dest[2] = source[sourceIndex3];
			return dest;
		}

		public abstract void permuteEquities(final double[] source, final double[] dest);

	}

	private final void computeAccurateEquities() throws InterruptedException {
		new TwoPlusTwoEvaluator(); // Just to load it before we get started
		final long start = System.currentTimeMillis();
		final MutableLong done = new MutableLong();
		final MutableLong enqueued = new MutableLong();
		final ExecutorService exe = Executors
				.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		final WaughIndexer threePlayersIndexer = this.threePlayersIndexer;
		final Permutator permutator1 = new Permutator(0, 2, 1) {

			// Hero is hero
			// Vilain1 is Vilain2
			// Vilain2 is Vilain1
			@Override
			public void permuteEquities(double[] source, double[] dest) {
				dest[heroVilain1Vilain2_heroIndex] = source[heroVilain1Vilain2_heroIndex];
				dest[heroVilain1Vilain2_vilain1Index] = (1 - source[heroVilain1Vilain2_heroIndex]
						- source[heroVilain1Vilain2_vilain1Index]);
				dest[heroVilain1_heroIndex] = source[heroVilain2_heroIndex];
				dest[heroVilain2_heroIndex] = source[heroVilain1_heroIndex];
				dest[vilain1Vilain2_vilain1Index] = 1 - source[vilain1Vilain2_vilain1Index];
			}

		};
		final Permutator permutator2 = new Permutator(1, 2, 0) {

			// Hero is Vilain1
			// Vilain1 is Vilain2
			// Vilain2 is Hero
			@Override
			public void permuteEquities(double[] source, double[] dest) {
				dest[heroVilain1Vilain2_heroIndex] = source[heroVilain1Vilain2_vilain1Index];
				dest[heroVilain1Vilain2_vilain1Index] = (1 - source[heroVilain1Vilain2_heroIndex]
						- source[heroVilain1Vilain2_vilain1Index]);
				dest[heroVilain1_heroIndex] = source[vilain1Vilain2_vilain1Index];
				dest[heroVilain2_heroIndex] = 1 - source[heroVilain1_heroIndex];
				dest[vilain1Vilain2_vilain1Index] = 1 - source[heroVilain2_heroIndex];
			}

		};
		final Permutator permutator3 = new Permutator(1, 0, 2) {

			// Hero is Vilain1
			// Vilain1 is Hero
			// Vilain2 is Vilain2
			@Override
			public void permuteEquities(double[] source, double[] dest) {
				dest[heroVilain1Vilain2_heroIndex] = source[heroVilain1Vilain2_vilain1Index];
				dest[heroVilain1Vilain2_vilain1Index] = source[heroVilain1Vilain2_heroIndex];
				dest[heroVilain1_heroIndex] = 1 - source[heroVilain1_heroIndex];
				dest[heroVilain2_heroIndex] = source[vilain1Vilain2_vilain1Index];
				dest[vilain1Vilain2_vilain1Index] = source[heroVilain2_heroIndex];
			}

		};
		final Permutator permutator4 = new Permutator(2, 0, 1) {

			// Hero is Vilain2
			// Vilain1 is Hero
			// Vilain2 is Vilain1
			@Override
			public void permuteEquities(double[] source, double[] dest) {
				dest[heroVilain1Vilain2_heroIndex] = (1 - source[heroVilain1Vilain2_heroIndex]
						- source[heroVilain1Vilain2_vilain1Index]);
				dest[heroVilain1Vilain2_vilain1Index] = source[heroVilain1Vilain2_heroIndex];
				dest[heroVilain1_heroIndex] = 1 - source[heroVilain2_heroIndex];
				dest[heroVilain2_heroIndex] = 1 - source[vilain1Vilain2_vilain1Index];
				dest[vilain1Vilain2_vilain1Index] = source[heroVilain1_heroIndex];
			}

		};
		final Permutator permutator5 = new Permutator(2, 1, 0) {
			// Hero is Vilain2
			// Vilain1 is Vilain1
			// Vilain2 is Hero
			@Override
			public void permuteEquities(double[] source, double[] dest) {
				dest[heroVilain1Vilain2_heroIndex] = (1 - source[heroVilain1Vilain2_heroIndex]
						- source[heroVilain1Vilain2_vilain1Index]);
				dest[heroVilain1Vilain2_vilain1Index] = source[heroVilain1Vilain2_vilain1Index];
				dest[heroVilain1_heroIndex] = 1 - source[vilain1Vilain2_vilain1Index];
				dest[heroVilain2_heroIndex] = 1 - source[heroVilain2_heroIndex];
				dest[vilain1Vilain2_vilain1Index] = 1 - source[heroVilain1_heroIndex];
			}
		};
		final double[][] equities = this.equities;
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

			final double[] handEquities = new double[5];
			final double[] reversedEquities1 = new double[5];
			final double[] reversedEquities2 = new double[5];
			final double[] reversedEquities3 = new double[5];
			final double[] reversedEquities4 = new double[5];
			final double[] reversedEquities5 = new double[5];

			equities[finalIndex] = handEquities;
			equities[reversedIndex1] = reversedEquities1;
			equities[reversedIndex2] = reversedEquities2;
			equities[reversedIndex3] = reversedEquities3;
			equities[reversedIndex4] = reversedEquities4;
			equities[reversedIndex5] = reversedEquities5;

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
							} else if (vilain1BeatsHero && vilain1BeatsVilain2) {
								threePlayers[1]++;
							} else if (vilain2BeatsHero && vilain2BeatsVilain1) {
								threePlayers[2]++;
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

					// public static final int heroVilain1Vilain2_heroIndex = 0;
					// public static final int heroVilain1Vilain2_vilain1Index =
					// 1;
					// public static final int heroVilain1_heroIndex = 2;
					// public static final int heroVilain2_heroIndex = 2;
					// public static final int vilain1Vilain2_vilain1Index = 4;

					// Fill equity when 3 players in showdown
					// 0: hero wins
					// 1: vilain1 wins
					// 2: vilain2 wins
					// 3: split 3 player
					// 4: split hero/vilain1
					// 5: split hero/vilain2,
					// 6: split vilain1/vilain2
					handEquities[heroVilain1Vilain2_heroIndex] = threePlayers[0] + threePlayers[3] / 3d
							+ (threePlayers[4] + threePlayers[5]) / 2d;
					handEquities[heroVilain1Vilain2_vilain1Index] = threePlayers[1] + threePlayers[3] / 3d
							+ (threePlayers[4] + threePlayers[6]) / 2d;
					final int hV1Win = heroVilain1[0];
					final int hV1Lose = heroVilain1[1];
					final int hV1Tie = heroVilain1[2];

					handEquities[heroVilain1_heroIndex] = (hV1Win + hV1Tie / 2d) / (hV1Win + hV1Tie + hV1Lose);

					final int hV2Win = heroVilain2[0];
					final int hV2Lose = heroVilain2[1];
					final int hV2Tie = heroVilain2[2];
					handEquities[heroVilain2_heroIndex] = (hV2Win + hV2Tie / 2d) / (hV2Win + hV2Tie + hV2Lose);

					final int v1V2Win = vilain1Vilain2[0];
					final int v1V2Lose = vilain1Vilain2[1];
					final int v1V2Tie = vilain1Vilain2[2];
					handEquities[vilain1Vilain2_vilain1Index] = (v1V2Win + v1V2Tie / 2d)
							/ (v1V2Win + v1V2Tie + v1V2Lose);

					permutator1.permuteEquities(handEquities, reversedEquities1);
					permutator2.permuteEquities(handEquities, reversedEquities2);
					permutator3.permuteEquities(handEquities, reversedEquities3);
					permutator4.permuteEquities(handEquities, reversedEquities4);
					permutator5.permuteEquities(handEquities, reversedEquities5);
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
		final double[][][][] reducedEquities = this.reducedEquities;
		final int[][][] counts = this.reducedCounts;
		final IntCardsSpec indexSpecs = holeCardsIndexer.getCardsSpec();
		final double[][] equities = this.equities;
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
				final double[] results = equities[indexInTables];
				final double[] dest = reducedEquities[heroIndex][vilain1Index][vilain2Index];
				for (int i = 0; i < 5; i++) {
					dest[i] += results[i];
				}
				counts[heroIndex][vilain1Index][vilain2Index]++;
				return true;
			}
		});
		for (int i = 0; i < nbHoleCards; i++) {
			final double[][][] heroEquities = reducedEquities[i];
			final int[][] heroCounts = counts[i];
			for (int j = 0; j < nbHoleCards; j++) {
				final double[][] heroVilain1Equities = heroEquities[j];
				final int[] heroVilain1Counts = heroCounts[j];
				for (int k = 0; k < nbHoleCards; k++) {
					final double[] heroVilain1Vilain2Equities = heroVilain1Equities[k];
					final int count = heroVilain1Counts[k];
					for (int l = 0; l < 5; l++) {
						heroVilain1Vilain2Equities[i] /= count;
					}
				}
			}
		}
	}

	public double[] getEquities(int[] heroCards, int[] vilain1Cards, int[] vilain2Cards) {
		return equities[threePlayersIndexer.indexOf(new int[][] { heroCards, vilain1Cards, vilain2Cards })];
	}

	public double[] getReducedEquities(final int[] heroCards, final int[] vilain1Cards, final int[] vilain2Cards) {
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

	private void writeObject(ObjectOutputStream out) throws IOException {
		checkState(isComputed(), "Won't write equities when not computed");
		final ByteBuffer buf = ByteBuffer.allocate(8 * 5);
		final int nbPreflopThreePlayers = this.nbPreflopThreePlayers;
		final double[][] equities = this.equities;
		for (int i = 0; i < nbPreflopThreePlayers; i++) {
			write(out, buf, equities[i]);
		}

		final int nbHoleCards = this.nbHoleCards;
		final double[][][][] reducedEquities = this.reducedEquities;
		for (int i = 0; i < nbHoleCards; i++) {
			final double[][][] ri = reducedEquities[i];
			for (int j = 0; j < nbHoleCards; j++) {
				final double[][] rj = ri[j];
				for (int k = 0; k < nbHoleCards; k++) {
					write(out, buf, rj[k]);
				}
			}
		}

		final int[][][] reducedCounts = this.reducedCounts;
		for (int i = 0; i < nbHoleCards; i++) {
			final int[][] ri = reducedCounts[i];
			for (int j = 0; j < nbHoleCards; j++) {
				write(out, buf, ri[j]);
			}
		}
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		final ByteBuffer buf = ByteBuffer.allocate(8 * 5);
		final int nbHoleCards = new WaughIndexer(onePlayerGroupsSize).getIndexSize();
		final int nbPreflopThreePlayers = new WaughIndexer(threePlayersGroupsSize).getIndexSize();
		equities = new double[nbPreflopThreePlayers][5];
		final double[][] equities = this.equities;
		for (int i = 0; i < nbPreflopThreePlayers; i++) {
			read(stream, buf, equities[i]);
		}
		reducedEquities = new double[nbHoleCards][nbHoleCards][nbHoleCards][5];
		final double[][][][] reducedEquities = this.reducedEquities;
		for (int i = 0; i < nbHoleCards; i++) {
			final double[][][] ri = reducedEquities[i];
			for (int j = 0; j < nbHoleCards; j++) {
				final double[][] rj = ri[j];
				for (int k = 0; k < nbHoleCards; k++) {
					read(stream, buf, rj[k]);
				}
			}
		}
		reducedCounts = new int[nbHoleCards][nbHoleCards][nbHoleCards];
		final int[][][] reducedCounts = this.reducedCounts;
		for (int i = 0; i < nbHoleCards; i++) {
			final int[][] ri = reducedCounts[i];
			for (int j = 0; j < nbHoleCards; j++) {
				read(stream, buf, ri[j]);
			}
		}
	}

	// Just to have all fields final and all initialized fields
	private Object readResolve() {
		return new ThreePlayersPreflopEquityTables(equities, reducedEquities, reducedCounts);
	}

	private static void write(OutputStream out, ByteBuffer buf, double[] src) throws IOException {
		buf.clear();
		final int size = src.length;
		int totalWrote = 0;
		final int bufCapacity = buf.asDoubleBuffer().capacity();
		int wrote;
		while (totalWrote < size) {
			buf.clear();
			wrote = Math.min(bufCapacity, size - totalWrote);
			buf.asDoubleBuffer().put(src, totalWrote, wrote);
			out.write(buf.array(), 0, wrote * 8);
			totalWrote += wrote;
		}
	}

	private static void write(OutputStream out, ByteBuffer buf, int[] src) throws IOException {
		buf.clear();
		final int size = src.length;
		int totalWrote = 0;
		final int bufCapacity = buf.asIntBuffer().capacity();
		int wrote;
		while (totalWrote < size) {
			buf.clear();
			wrote = Math.min(bufCapacity, size - totalWrote);
			buf.asIntBuffer().put(src, totalWrote, wrote);
			out.write(buf.array(), 0, wrote * 4);
			totalWrote += wrote;
		}
	}

	private static void read(InputStream stream, ByteBuffer buffer, double[] dest) throws IOException {
		buffer.clear();
		final int bufCapacity = buffer.capacity();
		final int size = dest.length * 8;
		int read = 0;
		int lastRead;
		while (read < size) {
			buffer.clear();
			final int toRead = Math.min(bufCapacity, size - read);
			int readForThisBuffferLoop = 0;
			while (readForThisBuffferLoop < toRead) {
				readForThisBuffferLoop += lastRead = stream.read(buffer.array(), readForThisBuffferLoop,
						toRead - readForThisBuffferLoop);

				if (lastRead < 0)
					throw new IOException("File is too short, it may be corrupted");
			}
			buffer.clear();
			buffer.asDoubleBuffer().get(dest, read / 8, toRead / 8);
			read += toRead;
		}

	}

	private static void read(InputStream stream, ByteBuffer buffer, int[] dest) throws IOException {
		buffer.clear();
		final int bufCapacity = buffer.capacity();
		final int size = dest.length * 4;
		int read = 0;
		int lastRead;
		while (read < size) {
			buffer.clear();
			final int toRead = Math.min(bufCapacity, size - read);
			int readForThisBuffferLoop = 0;
			while (readForThisBuffferLoop < toRead) {
				readForThisBuffferLoop += lastRead = stream.read(buffer.array(), readForThisBuffferLoop,
						toRead - readForThisBuffferLoop);

				if (lastRead < 0)
					throw new IOException("File is too short, it may be corrupted");
			}
			buffer.clear();
			buffer.asIntBuffer().get(dest, read / 4, toRead / 4);
			read += toRead;
		}

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
