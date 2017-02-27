package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkState;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
public class HUPreflopEquityTables implements Serializable {

	private static final long serialVersionUID = 1808572853794466312L;
	private static final int[] onePlayerGroupsSize = { 2 };
	private static final int[] twoPlayersGroupsSize = { 2, 2 };

	@Getter
	private final WaughIndexer twoPlayersIndexer = new WaughIndexer(twoPlayersGroupsSize);
	@Getter
	private final int nbPreflopTwoPlayers = twoPlayersIndexer.getIndexSize();
	@Getter
	private final WaughIndexer holeCardsIndexer = new WaughIndexer(onePlayerGroupsSize);
	@Getter
	private final int nbHoleCards = holeCardsIndexer.getIndexSize();
	@Getter
	private final int[][][] reducedWinLoseTie = new int[nbHoleCards][nbHoleCards][3];
	@Getter
	private final int[][] winLoseTie = new int[nbPreflopTwoPlayers][];

	private long total = 0;
	private long done = 0;
	private long start;

	public boolean isComputed() {
		return winLoseTie[0] != null;
	}

	public synchronized void compute() throws InterruptedException {
		checkState(!isComputed(), "Tables have already been computed");
		computeAccurateWLT();
		computeReducedWLT();
	}

	private final void computeAccurateWLT() throws InterruptedException {
		start = System.currentTimeMillis();
		final ExecutorService exe = Executors
				.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));
		final WaughIndexer holeCardsIndexer = new WaughIndexer(new int[] { 2, 2 });

		final int nbHoleCards = this.nbPreflopTwoPlayers;

		start = System.currentTimeMillis();
		for (int index = 0; index < nbHoleCards; index++) {
			if (winLoseTie[index] != null) {
				continue;
			}
			final TwoPlusTwoEvaluator eval = new TwoPlusTwoEvaluator();
			final Cards52SpecTranslator translateToEval = new Cards52SpecTranslator(holeCardsIndexer.getCardsSpec(),
					eval.getCardsSpec());
			final int[][] holeCards = new int[2][2];
			holeCardsIndexer.unindex(index, holeCards);
			final int[][] reversedHoleCards = new int[][] { holeCards[1], holeCards[0] };
			final int reversedIndex = holeCardsIndexer.indexOf(reversedHoleCards);
			translateToEval.translate(holeCards);
			final int[] heroCards = new int[7];
			heroCards[0] = holeCards[0][0];
			heroCards[1] = holeCards[0][1];
			final int[] vilainCards = new int[7];
			vilainCards[0] = holeCards[1][0];
			vilainCards[1] = holeCards[1][1];

			final Deck52Cards evalDeck = new Deck52Cards(eval.getCardsSpec());
			final int[] WLT = new int[3];
			final int[] reversedWLT = new int[3];
			winLoseTie[index] = WLT;
			winLoseTie[reversedIndex] = reversedWLT;
			total++;
			exe.execute(new Runnable() {
				@Override
				public void run() {
					evalDeck.drawAllGroupsCombinations(new int[] { 5 }, new CardsGroupsDrawingTask() {

						@Override
						public boolean doTask(int[][] cardsGroups) {
							final int[] board = cardsGroups[0];
							System.arraycopy(board, 0, heroCards, 2, 5);
							System.arraycopy(board, 0, vilainCards, 2, 5);
							final int heroVal = eval.get7CardsEval(heroCards);
							final int vilainVal = eval.get7CardsEval(vilainCards);
							if (heroVal > vilainVal) {
								WLT[0]++;
							} else if (heroVal < vilainVal) {
								WLT[1]++;
							} else {
								WLT[2]++;
							}
							return true;
						}
					}, holeCards[0], holeCards[1]);
					reversedWLT[0] = WLT[1];
					reversedWLT[1] = WLT[0];
					reversedWLT[2] = WLT[2];
					done++;
					final double ratioDone = done / (double) total;
					if (done % 1000 == 0 && ratioDone != 0 && ratioDone != 1) {
						final long elapsed = System.currentTimeMillis() - start;
						log.info("Remaining operations {}/{}, time {}s", total - done, total,
								(int) (elapsed * (1 - ratioDone) / (1000 * ratioDone)));
					}
				}
			});
		}

		log.info("Put {} runnables", total);
		exe.shutdown();
		exe.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
	}

	private void computeReducedWLT() {
		final IntCardsSpec indexSpecs = holeCardsIndexer.getCardsSpec();
		final int[][] tables = this.winLoseTie;
		final Deck52Cards deck = new Deck52Cards(indexSpecs);
		final WaughIndexer onePlayerIndexer = new WaughIndexer(onePlayerGroupsSize);
		final WaughIndexer twoPlayersIndexer = new WaughIndexer(twoPlayersGroupsSize);
		deck.drawAllGroupsCombinations(twoPlayersGroupsSize, new CardsGroupsDrawingTask() {

			@Override
			public boolean doTask(int[][] cardsGroups) {

				final int indexInTables = twoPlayersIndexer.indexOf(cardsGroups);
				final int heroIndex = onePlayerIndexer.indexOf(new int[][] { cardsGroups[0] });
				final int vilainIndex = onePlayerIndexer.indexOf(new int[][] { cardsGroups[1] });
				final int[] results = tables[indexInTables];
				final int[] dest = reducedWinLoseTie[heroIndex][vilainIndex];
				for (int i = 0; i < 3; i++) {
					dest[i] += results[i];
				}
				return true;
			}
		});
	}

	public int[] getWinLoseTie(int[] heroCards, int[] opponentCards) {
		return winLoseTie[twoPlayersIndexer.indexOf(new int[][] { heroCards, opponentCards })];
	}

	public double getEquity(int[] heroCards, int[] opponentCards) {
		final int[] wlt = winLoseTie[twoPlayersIndexer.indexOf(new int[][] { heroCards, opponentCards })];
		final double w = wlt[0];
		final double l = wlt[1];
		final double t = wlt[2];
		return (w + t / 2) / (w + l + t);
	}

	public int[] getReducedWinLoseTie(final int[] heroCards, final int[] vilainCards) {
		return reducedWinLoseTie[holeCardsIndexer.indexOf(new int[][] { heroCards })][holeCardsIndexer
				.indexOf(new int[][] { vilainCards })];
	}

	public double getReducedEquity(final int[] heroCards, final int[] vilainCards) {
		final int[] wlt = getWinLoseTie(heroCards, vilainCards);
		final int w = wlt[0];
		final int l = wlt[1];
		final int t = wlt[2];
		return (w + t / 2.0d) / (double) (w + l + t);
	}

	public IntCardsSpec getCardsSpec() {
		return DefaultIntCardsSpecs.getDefault();
	}

}
