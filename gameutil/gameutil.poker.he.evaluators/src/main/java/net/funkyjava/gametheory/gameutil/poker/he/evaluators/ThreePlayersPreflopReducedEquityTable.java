package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;

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
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.mutable.MutableInt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.CardsGroupsDrawingTask;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.twoplustwo.TwoPlusTwoEvaluator;
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
	@Getter
	private final WaughIndexer holeCardsIndexer = new WaughIndexer(onePlayerGroupsSize);
	private final TwoPlusTwoEvaluator eval = new TwoPlusTwoEvaluator();
	private final IntCardsSpec indexSpecs = holeCardsIndexer.getCardsSpec();
	private final IntCardsSpec evalSpecs = eval.getCardsSpec();
	@Getter
	private final int nbHoleCards = holeCardsIndexer.getIndexSize();

	private boolean computed = false;

	@Getter
	private final double[][][][][] reducedEquities = new double[nbHoleCards][nbHoleCards][nbHoleCards][4][3];
	@Getter
	private final int[][][] counts = new int[nbHoleCards][nbHoleCards][nbHoleCards];

	public ThreePlayersPreflopReducedEquityTable() {

	}

	private static interface EnumAction {
		void action();
	}

	@AllArgsConstructor
	private static final class CardsPermutator {

		private final int sourceIndex1, sourceIndex2, sourceIndex3;

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

	public synchronized final void compute() throws InterruptedException {
		checkArgument(!computed, "Already computed");
		final ExecutorService exe = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final long start = System.currentTimeMillis();
		final int tot = 818805;
		final AtomicInteger c = new AtomicInteger();
		final double[][][][][] reducedEquities = this.reducedEquities;
		final CardsPermutator permutator1 = new CardsPermutator(0, 2, 1);
		final CardsPermutator permutator2 = new CardsPermutator(1, 2, 0);
		final CardsPermutator permutator3 = new CardsPermutator(1, 0, 2);
		final CardsPermutator permutator4 = new CardsPermutator(2, 0, 1);
		final CardsPermutator permutator5 = new CardsPermutator(2, 1, 0);

		final int[][][] counts = this.counts;

		final int nbHoleCards = this.nbHoleCards;
		for (int h1 = 0; h1 < nbHoleCards; h1++) {
			final int fh1 = h1;
			for (int h2 = h1; h2 < nbHoleCards; h2++) {
				final int fh2 = h2;
				for (int h3 = h2; h3 < nbHoleCards; h3++) {
					final int fh3 = h3;
					exe.execute(new Runnable() {

						@Override
						public void run() {
							final int[][] baseCards1 = new int[1][2];
							final int[][] baseCards2 = new int[1][2];
							final int[][] baseCards3 = new int[1][2];
							final int[][] cards1 = new int[1][2];
							final int[][] cards2 = new int[1][2];
							final int[][] cards3 = new int[1][2];
							final MutableInt count = new MutableInt();
							final int[] heroVilain1 = new int[3];
							final int[] heroVilain2 = new int[3];
							final int[] vilain1Vilain2 = new int[3];
							// 0: hero wins
							// 1: vilain1 wins
							// 2: vilain2 wins
							// 3: split 3 player
							// 4: split hero/vilain1
							// 5: split hero/vilain2,
							// 6: split vilain1/vilain2
							final int[] threePlayers = new int[7];
							enumAllHands(fh1, fh2, fh3, baseCards1, baseCards2, baseCards3, cards1, cards2, cards3,
									new EnumAction() {

										@Override
										public void action() {
											count.increment();
											addEval(heroVilain1, heroVilain2, vilain1Vilain2, threePlayers, cards1,
													cards2, cards3);
										}

									});
							counts[fh1][fh2][fh3] = count.getValue();
							final double[][] handEquities = reducedEquities[fh1][fh2][fh3];
							counts[fh1][fh3][fh2] = count.getValue();
							final double[][] reversedEquities1 = reducedEquities[fh1][fh3][fh2];
							counts[fh2][fh3][fh1] = count.getValue();
							final double[][] reversedEquities2 = reducedEquities[fh2][fh3][fh1];
							counts[fh2][fh1][fh3] = count.getValue();
							final double[][] reversedEquities3 = reducedEquities[fh2][fh1][fh3];
							counts[fh3][fh1][fh2] = count.getValue();
							final double[][] reversedEquities4 = reducedEquities[fh3][fh1][fh2];
							counts[fh3][fh2][fh1] = count.getValue();
							final double[][] reversedEquities5 = reducedEquities[fh3][fh2][fh1];
							computeEquity(heroVilain1, heroVilain2, vilain1Vilain2, threePlayers, handEquities);
							permutator1.permute(handEquities, reversedEquities1);
							permutator2.permute(handEquities, reversedEquities2);
							permutator3.permute(handEquities, reversedEquities3);
							permutator4.permute(handEquities, reversedEquities4);
							permutator5.permute(handEquities, reversedEquities5);
							final int iter = c.incrementAndGet();
							final double ratio = iter / (double) tot;
							final double elapsed = System.currentTimeMillis() - start;
							final double remaining = (1 - ratio) * elapsed / ratio;
							log.info("Remaining compute time {}", (long) (remaining / 1000));
						}
					});

				}
			}
		}
		exe.shutdown();
		exe.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		computed = true;
	}

	private final void computeEquity(final int[] heroVilain1, final int[] heroVilain2, final int[] vilain1Vilain2,
			final int[] threePlayers, final double[][] handEquities) {
		final double[] threePlayersEq = handEquities[heroVilain1Vilain2Index];
		threePlayersEq[0] = threePlayers[0] + threePlayers[3] / 3d + (threePlayers[4] + threePlayers[5]) / 2d;
		threePlayersEq[1] = threePlayers[1] + threePlayers[3] / 3d + (threePlayers[4] + threePlayers[6]) / 2d;
		threePlayersEq[2] = threePlayers[2] + threePlayers[3] / 3d + (threePlayers[5] + threePlayers[6]) / 2d;

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
	}

	private final void addEval(final int[] heroVilain1, final int[] heroVilain2, final int[] vilain1Vilain2,
			final int[] threePlayers, final int[][] cards1, final int[][] cards2, final int[][] cards3) {
		final int[] heroCards = new int[7];
		heroCards[0] = cards1[0][0];
		heroCards[1] = cards1[0][1];
		final int[] vilain1Cards = new int[7];
		vilain1Cards[0] = cards2[0][0];
		vilain1Cards[1] = cards2[0][1];
		final int[] vilain2Cards = new int[7];
		vilain2Cards[0] = cards3[0][0];
		vilain2Cards[1] = cards3[0][1];
		final TwoPlusTwoEvaluator eval = this.eval;
		final Deck52Cards evalDeck = new Deck52Cards(evalSpecs);
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

				// 0: hero wins
				// 1: vilain1 wins
				// 2: vilain2 wins
				// 3: split 3 player
				// 4: split hero/vilain1
				// 5: split hero/vilain2,
				// 6: split vilain1/vilain2
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
		}, cards1[0], cards2[0], cards3[0]);
	}

	private final void enumAllHands(final int h1, final int h2, final int h3, final int[][] baseCards1,
			final int[][] baseCards2, final int[][] baseCards3, final int[][] cards1, final int[][] cards2,
			final int[][] cards3, final EnumAction action) {

		final WaughIndexer holeCardsIndexer = new WaughIndexer(onePlayerGroupsSize);
		holeCardsIndexer.unindex(h1, baseCards1);
		holeCardsIndexer.unindex(h2, baseCards2);
		holeCardsIndexer.unindex(h3, baseCards3);
		enumerate(baseCards1[0], cards1[0], new EnumAction() {

			@Override
			public void action() {
				enumerate(baseCards2[0], cards2[0], new EnumAction() {

					@Override
					public void action() {
						enumerate(baseCards3[0], cards3[0], new EnumAction() {

							@Override
							public void action() {
								action.action();
							}

						}, cards1[0][0], cards1[0][1], cards2[0][0], cards2[0][1]);
					}

				}, cards1[0][0], cards1[0][1]);

			}

		});
	}

	private final void enumerate(final int[] baseCards, final int[] cards, final EnumAction action,
			final int... reservedCards) {
		final IntCardsSpec indexSpecs = this.indexSpecs;
		final int card1 = baseCards[0];
		final int card2 = baseCards[1];
		final int r1 = indexSpecs.getStandardRank(card1);
		final int r2 = indexSpecs.getStandardRank(card2);
		if (r1 == r2) {
			enumeratePair(r1, cards, action, reservedCards);
			return;
		}
		final boolean suited = indexSpecs.getStandardColor(card1) == indexSpecs.getStandardColor(card2);
		if (suited) {
			enumerateSuited(r1, r2, cards, action, reservedCards);
		} else {
			enumerateOffsuit(r1, r2, cards, action, reservedCards);
		}
	}

	private final void enumerateSuited(final int r1, final int r2, final int[] cards, final EnumAction action,
			final int... reservedCards) {
		final IntCardsSpec evalSpecs = this.evalSpecs;
		for (int color = 0; color < 4; color++) {
			final int card1 = evalSpecs.getCard(r1, color);
			if (hitsReserved(card1, reservedCards)) {
				continue;
			}
			final int card2 = evalSpecs.getCard(r2, color);
			if (hitsReserved(card2, reservedCards)) {
				continue;
			}
			cards[0] = card1;
			cards[1] = card2;
			action.action();
		}
	}

	private final void enumerateOffsuit(final int r1, final int r2, final int[] cards, final EnumAction action,
			final int... reservedCards) {
		final IntCardsSpec evalSpecs = this.evalSpecs;
		for (int color1 = 0; color1 < 4; color1++) {
			final int card1 = evalSpecs.getCard(r1, color1);
			if (hitsReserved(card1, reservedCards)) {
				continue;
			}
			cards[0] = card1;
			for (int color2 = 0; color2 < 4; color2++) {
				final int card2 = evalSpecs.getCard(r2, color2);
				if (hitsReserved(card2, reservedCards)) {
					continue;
				}
				cards[1] = card2;
				action.action();
			}
		}
	}

	private final void enumeratePair(final int r1, final int[] cards, final EnumAction action,
			final int... reservedCards) {
		final IntCardsSpec evalSpecs = this.evalSpecs;
		for (int color1 = 0; color1 < 3; color1++) {
			final int card1 = evalSpecs.getCard(r1, color1);
			if (hitsReserved(card1, reservedCards)) {
				continue;
			}
			cards[0] = card1;
			for (int color2 = color1 + 1; color2 < 4; color2++) {
				final int card2 = evalSpecs.getCard(r1, color2);
				if (hitsReserved(card2, reservedCards)) {
					continue;
				}
				cards[1] = card2;
				action.action();
			}
		}
	}

	private static final boolean hitsReserved(final int card, final int[] reservedCards) {
		final int length = reservedCards.length;
		for (int i = 0; i < length; i++) {
			if (reservedCards[i] == card) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void fill(InputStream is) throws IOException {
		IOUtils.fill(is, this.reducedEquities);
		IOUtils.fill(is, this.counts);
	}

	@Override
	public void write(OutputStream os) throws IOException {
		IOUtils.write(os, this.reducedEquities);
		IOUtils.write(os, this.counts);
	}

	public static void main(String[] args) {
		checkArgument(args.length == 1, "3 Players Preflop Tables writing misses a path argument");
		final String pathStr = args[0];
		final Path path = Paths.get(pathStr);
		checkArgument(!Files.exists(path), "File " + path.toAbsolutePath().toString() + " already exists");
		try (final FileOutputStream fos = new FileOutputStream(path.toFile())) {
			final ThreePlayersPreflopReducedEquityTable tables = new ThreePlayersPreflopReducedEquityTable();
			tables.compute();
			tables.write(fos);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
