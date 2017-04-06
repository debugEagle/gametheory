package net.funkyjava.gametheory.games.nlhe.preflop;

import static net.funkyjava.gametheory.io.ProgramArguments.getArgument;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Optional;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHand;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BetRoundSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.NoBetPlayerData;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopReducedEquityTable;

@Slf4j
public class WildTwisterPreflop {

	final private int sb = 5;
	final private int bb = 10;
	final private int initStack = 50;
	final private int totalChips = initStack * 3;
	final private int nbStacks = totalChips / sb - 1;
	final ThreePlayersPreflopCSCFRM[][][] cscfrms = new ThreePlayersPreflopCSCFRM[nbStacks][nbStacks][nbStacks];

	private final int createRunners(final ThreePlayersPreflopReducedEquityTable table) {
		int res = 0;
		final BetRoundSpec<Integer> betsSpec = new BetRoundSpec<Integer>(0, bb);
		final BlindsAnteSpec<Integer> blindsSpecs = new BlindsAnteSpec<Integer>(false, true, false, sb, bb, 0,
				Collections.<Integer>emptyList(), 0, 1);
		final int sb = this.sb;
		final int nbStacks = this.nbStacks;
		final int totalChips = this.totalChips;
		for (int i = 1; i < nbStacks; i++) {
			final int iStack = i * sb;
			final NoBetPlayerData<Integer> sbData = new NoBetPlayerData<Integer>(0, iStack, true);
			for (int j = 1; j < nbStacks; j++) {
				final int jStack = j * sb;
				final NoBetPlayerData<Integer> bbData = new NoBetPlayerData<Integer>(1, jStack, true);
				for (int k = 1; k < nbStacks; k++) {
					final int kStack = k * sb;
					if (iStack + jStack + kStack != totalChips) {
						continue;
					}
					res++;
					final List<NoBetPlayerData<Integer>> playersData = new LinkedList<>();

					final NoBetPlayerData<Integer> btData = new NoBetPlayerData<Integer>(2, kStack, true);
					playersData.add(sbData);
					playersData.add(bbData);
					playersData.add(btData);
					final NLHand<Integer> hand = new NLHand<>(playersData, blindsSpecs, betsSpec, 1);
					cscfrms[i][j][k] = new ThreePlayersPreflopCSCFRM(hand, table, null);
				}
			}
		}
		return res;
	}

	private final void runEachFor(final long milliseconds) throws InterruptedException {
		synchronized (this) {
			final int nbStacks = this.nbStacks;
			final int totalChips = this.totalChips;
			for (int i = 1; i < nbStacks; i++) {
				final int iStack = i * sb;
				for (int j = 1; j < nbStacks; j++) {
					final int jStack = j * sb;
					for (int k = 1; k < nbStacks; k++) {
						final int kStack = k * sb;
						if (iStack + jStack + kStack != totalChips) {
							continue;
						}
						final ThreePlayersPreflopCSCFRM cscfrm = cscfrms[i][j][k];
						cscfrm.getRunner().start();
						this.wait(milliseconds);
						cscfrm.getRunner().stopAndAwaitTermination();
						log.info("\n\n");
						log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
						log.info("{} - {} - {}", iStack, jStack, kStack);
						log.info("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
						cscfrm.printStrategies();
					}
				}
			}
		}
	}

	private static ThreePlayersPreflopReducedEquityTable getTables(final String path) throws IOException {
		try (final FileInputStream fis = new FileInputStream(Paths.get(path).toFile())) {
			final ThreePlayersPreflopReducedEquityTable res = new ThreePlayersPreflopReducedEquityTable();
			res.fill(fis);
			res.expand();
			return res;
		}
	}

	public static void main(String[] args) throws InterruptedException {
		final Optional<String> eqOpt = getArgument(args, ThreePlayersPreflopCSCFRM.equityPathPrefix);
		if (!eqOpt.isPresent()) {
			return;
		}
		log.info("Loading equity tables");
		ThreePlayersPreflopReducedEquityTable tables;
		try {
			tables = getTables(eqOpt.get());
		} catch (Exception e) {
			log.error("Unable to load 3 players preflop equity tables", e);
			return;
		}
		final WildTwisterPreflop wtp = new WildTwisterPreflop();
		final int nbHands = wtp.createRunners(tables);
		log.info("Nb hands : {}", nbHands);
		wtp.runEachFor(3000);

	}
}
