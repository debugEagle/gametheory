package net.funkyjava.gametheory.cscfrm.games.poker.nlhe;

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Deck52Cards;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

@Slf4j
public class NLHE3PlayersPreflopChances {

	private final WaughIndexer indexer = new WaughIndexer(new int[] { 2 });
	private final int indexSize = indexer.getIndexSize();
	private final boolean[] p1Reserved = new boolean[indexSize];
	private final boolean[] p2Reserved = new boolean[indexSize];
	private final boolean[] p3Reserved = new boolean[indexSize];

	private boolean stop = false;

	private final List<int[]> chances = new LinkedList<>();
	private int nbOfChances;
	private final int minChances;
	private final int maxChances;

	@Getter
	private final Runnable fillRunnable;

	public NLHE3PlayersPreflopChances(final int minNbOfChances, final int maxNbOfChances) {
		this.minChances = minNbOfChances;
		this.maxChances = maxNbOfChances;
		fillRunnable = new Runnable() {

			@Override
			public void run() {
				try {
					fill();
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		};
	}

	private void fill() throws InterruptedException {
		final Deck52Cards deck = new Deck52Cards(indexer.getCardsSpec());
		final int[][] cards = new int[3][2];
		final int[][] p1Cards = new int[][] { cards[0] };
		final int[][] p2Cards = new int[][] { cards[1] };
		final int[][] p3Cards = new int[][] { cards[2] };
		final List<int[]> chances = this.chances;
		final WaughIndexer indexer = this.indexer;
		while (!stop) {
			while (nbOfChances >= minChances) {
				this.wait();
				if (stop) {
					break;
				}
			}
			for (int i = maxChances - nbOfChances; i > 0; i--) {
				deck.oneShotDeckDraw(cards);
				final int[] res = new int[3];
				res[0] = indexer.indexOf(p1Cards);
				res[1] = indexer.indexOf(p2Cards);
				res[2] = indexer.indexOf(p3Cards);
				synchronized (this) {
					chances.add(res);
					nbOfChances++;
				}
			}
			synchronized (this) {
				this.notifyAll();
			}
		}
	}

	public void stop() {
		synchronized (this) {
			stop = true;
			this.notifyAll();
		}
	}

	public int[] getChances(final int[] freeChances) throws InterruptedException {
		final List<int[]> chances = this.chances;
		final boolean[] p1Reserved = this.p1Reserved;
		final boolean[] p2Reserved = this.p2Reserved;
		final boolean[] p3Reserved = this.p3Reserved;
		synchronized (this) {
			if (freeChances != null) {
				p1Reserved[freeChances[0]] = false;
				p2Reserved[freeChances[1]] = false;
				p3Reserved[freeChances[2]] = false;
			}
			while (!stop) {

				final int nbOfChances = this.nbOfChances;
				for (int i = 0; i < nbOfChances; i++) {
					final int[] res = chances.get(i);
					if (p1Reserved[res[0]] || p2Reserved[res[1]] || p3Reserved[res[2]]) {
						continue;
					}
					p1Reserved[res[0]] = true;
					p2Reserved[res[1]] = true;
					p3Reserved[res[2]] = true;
					chances.remove(i);
					if ((--this.nbOfChances) < minChances) {
						this.notifyAll();
					}
					return res;
				}
				log.error("Starving");
				this.wait();
			}
		}
		return new int[3];
	}

}
