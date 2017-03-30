package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.MutableLong;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Cards52SpecTranslator;
import net.funkyjava.gametheory.gameutil.cards.CardsGroupsDrawingTask;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.cards.DefaultIntCardsSpecs;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.twoplustwo.TwoPlusTwoEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;
import net.funkyjava.gametheory.io.Fillable;
import net.funkyjava.gametheory.io.IOUtils;

@Slf4j
public class ThreePlayersPreflopEquityTables implements Fillable {

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
	private final double[][][] equities = new double[nbPreflopThreePlayers][][];

	private boolean computed = false;

	public ThreePlayersPreflopEquityTables() {

	}

	public boolean isComputed() {
		return computed;
	}

	public void compute() throws InterruptedException {
		checkState(!isComputed(), "Tables have already been computed");
		computeAccurateEquities();
		computed = true;
	}

	private final void computeAccurateEquities() throws InterruptedException {
		new TwoPlusTwoEvaluator(); // Just to load it before we get started
		final long start = System.currentTimeMillis();
		final MutableLong enqueued = new MutableLong();
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final WaughIndexer threePlayersIndexer = this.threePlayersIndexer;
		final WaughIndexer holeCardsIndexer = this.holeCardsIndexer;
		final double[][][] equities = this.equities;
		final int nbPreflopThreePlayers = this.nbPreflopThreePlayers;
		for (int index = 0; index < nbPreflopThreePlayers; index++) {
			final int finalIndex = index;
			final TwoPlusTwoEvaluator eval = new TwoPlusTwoEvaluator();
			final Cards52SpecTranslator translateToEval = new Cards52SpecTranslator(threePlayersIndexer.getCardsSpec(),
					eval.getCardsSpec());
			final int[][] holeCards = new int[3][2];
			threePlayersIndexer.unindex(index, holeCards);
			final int h1Index = holeCardsIndexer.indexOf(new int[][] { holeCards[0] });
			final int h2Index = holeCardsIndexer.indexOf(new int[][] { holeCards[1] });
			if (h1Index > h2Index) {
				continue;
			}
			final int h3Index = holeCardsIndexer.indexOf(new int[][] { holeCards[2] });
			if (h2Index > h3Index) {
				continue;
			}
			// 0 : three players
			// 1 : hero + vilain1 (vilain2 fold)
			// 2 : hero + vilain2 (vilain1 fold)
			// 3 : vilain1 + vilain2 (hero fold)
			final double[][] handEquities = new double[4][3];
			equities[finalIndex] = handEquities;
			translateToEval.translate(holeCards);
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
					final int hV1Tie = heroVilain1[2];
					vilain2FoldsEq[1] = 1 - (vilain2FoldsEq[0] = (hV1Win + hV1Tie / 2d) / (hV1Win + hV1Tie + hV1Lose));

					final double[] vilain1FoldsEq = handEquities[heroVilain2Index];
					final int hV2Win = heroVilain2[0];
					final int hV2Lose = heroVilain2[1];
					final int hV2Tie = heroVilain2[2];
					vilain1FoldsEq[2] = 1 - (vilain1FoldsEq[0] = (hV2Win + hV2Tie / 2d) / (hV2Win + hV2Tie + hV2Lose));

					final double[] heroFoldsEq = handEquities[vilain1Vilain2Index];
					final int v1V2Win = vilain1Vilain2[0];
					final int v1V2Lose = vilain1Vilain2[1];
					final int v1V2Tie = vilain1Vilain2[2];
					heroFoldsEq[2] = 1 - (heroFoldsEq[1] = (v1V2Win + v1V2Tie / 2d) / (v1V2Win + v1V2Tie + v1V2Lose));

					synchronized (enqueued) {
						enqueued.decrement();
						if (enqueued.getValue() < 100) {
							enqueued.notify();
						}
					}

				}
			});
			synchronized (enqueued) {
				enqueued.increment();
				if (index != nbPreflopThreePlayers - 1 && enqueued.longValue() >= 1000) {
					enqueued.wait();
					final double ratioDone = index / (double) nbPreflopThreePlayers;
					final double elapsed = System.currentTimeMillis() - start;
					log.info("Remaining time {} minutes", (int) (elapsed * (1 - ratioDone) / (60 * 1000 * ratioDone)));
				}
			}
		}
		log.info("Feeder : end enqueuing runnables, awaiting termination");
		exe.shutdown();
		exe.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		log.info("All runnables were executed");
	}

	public IntCardsSpec getCardsSpec() {
		return DefaultIntCardsSpecs.getDefault();
	}

	private static final byte nullArray = 0;
	private static final byte filledArray = 1;

	@Override
	public void fill(InputStream is) throws IOException {
		final double[][][] equities = this.equities;
		final int nbPreflopThreePlayers = this.nbPreflopThreePlayers;
		try (final DataInputStream dis = new DataInputStream(is)) {
			for (int i = 0; i < nbPreflopThreePlayers; i++) {
				final byte b = dis.readByte();
				if (b == nullArray) {
					continue;
				}
				final double[][] handEquities = new double[4][3];
				equities[i] = handEquities;
				IOUtils.fill(dis, handEquities);
			}
		}
		computed = true;
	}

	@Override
	public void write(OutputStream os) throws IOException {
		final double[][][] equities = this.equities;
		final int nbPreflopThreePlayers = this.nbPreflopThreePlayers;
		try (final DataOutputStream dos = new DataOutputStream(os)) {
			for (int i = 0; i < nbPreflopThreePlayers; i++) {
				if (equities[i] == null) {
					dos.writeByte(nullArray);
					continue;
				}
				dos.writeByte(filledArray);
				final double[][] handEquities = equities[i];
				IOUtils.write(dos, handEquities);
			}
		}
	}

	public static void main(String[] args) {
		checkArgument(args.length == 1, "3 Players Preflop Tables writing misses a path argument");
		final String pathStr = args[0];
		final Path path = Paths.get(pathStr);
		checkArgument(!Files.exists(path), "File " + path.toAbsolutePath().toString() + " already exists");
		try (final FileOutputStream fos = new FileOutputStream(path.toFile())) {
			final ThreePlayersPreflopEquityTables tables = new ThreePlayersPreflopEquityTables();
			tables.compute();
			tables.write(fos);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
