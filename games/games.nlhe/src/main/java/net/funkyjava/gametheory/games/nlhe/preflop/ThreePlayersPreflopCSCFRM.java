package net.funkyjava.gametheory.games.nlhe.preflop;

import static net.funkyjava.gametheory.io.ProgramArguments.getArgument;
import static net.funkyjava.gametheory.io.ProgramArguments.getPositiveIntArgument;
import static net.funkyjava.gametheory.io.ProgramArguments.getStrictlyPositiveIntArgument;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.google.common.base.Optional;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.cscfrm.CSCFRMChancesSynchronizer;
import net.funkyjava.gametheory.cscfrm.CSCFRMData;
import net.funkyjava.gametheory.cscfrm.CSCFRMMutexChancesSynchronizer;
import net.funkyjava.gametheory.cscfrm.CSCFRMRunner;
import net.funkyjava.gametheory.games.nlhe.NoLimitHoldEm;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHand;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BetRoundSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec.BlindsAnteSpecBuilder;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.NoBetPlayerData;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLAbstractedBetTree;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLBetTreeAbstractor;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLBetTreeNode;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLPushFoldBetTreeAbstractor;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.ThreePlayersPreflopReducedEquityTable;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

@Slf4j
public class ThreePlayersPreflopCSCFRM {

	private static final String equityPathPrefix = "equity=";
	private static final String bbPrefix = "bb=";
	private static final String sbPrefix = "sb=";
	private static final String antePrefix = "ante=";
	private static final String p1StackPrefix = "p1Stack=";
	private static final String p2StackPrefix = "p2Stack=";
	private static final String p3StackPrefix = "p3Stack=";
	private static final String svgPathPrefix = "svg=";
	private static final String interactiveArg = "-i";

	private static ThreePlayersPreflopReducedEquityTable getTables(final String path) throws IOException {
		try (final FileInputStream fis = new FileInputStream(Paths.get(path).toFile())) {
			final ThreePlayersPreflopReducedEquityTable res = new ThreePlayersPreflopReducedEquityTable();
			res.fill(fis);
			res.expand();
			return res;
		}
	}

	private static Optional<NLHand<Integer>> getHand(String[] args) {
		final Optional<Integer> bbOpt = getStrictlyPositiveIntArgument(args, bbPrefix);
		if (!bbOpt.isPresent()) {
			return Optional.absent();
		}
		Optional<Integer> sbOpt = getPositiveIntArgument(args, sbPrefix);
		if (!sbOpt.isPresent()) {
			log.info("No SB specified, considering 0 as SB value");
			sbOpt = Optional.of(new Integer(0));
		}
		Optional<Integer> anteOpt = getPositiveIntArgument(args, antePrefix);
		if (!anteOpt.isPresent()) {
			log.info("No ante specified, considering 0 as ante value");
			anteOpt = Optional.of(new Integer(0));
		}
		final Optional<Integer> p1Opt = getStrictlyPositiveIntArgument(args, p1StackPrefix);
		if (!p1Opt.isPresent()) {
			return Optional.absent();
		}
		final Optional<Integer> p2Opt = getStrictlyPositiveIntArgument(args, p2StackPrefix);
		if (!p2Opt.isPresent()) {
			return Optional.absent();
		}
		final Optional<Integer> p3Opt = getStrictlyPositiveIntArgument(args, p3StackPrefix);
		if (!p3Opt.isPresent()) {
			return Optional.absent();
		}
		final NoBetPlayerData<Integer> p1Data = new NoBetPlayerData<Integer>(0, p1Opt.get(), true);
		final NoBetPlayerData<Integer> p2Data = new NoBetPlayerData<Integer>(1, p2Opt.get(), true);
		final NoBetPlayerData<Integer> p3Data = new NoBetPlayerData<Integer>(2, p3Opt.get(), true);
		final List<NoBetPlayerData<Integer>> playersData = new LinkedList<>();
		playersData.add(p1Data);
		playersData.add(p2Data);
		playersData.add(p3Data);
		final BlindsAnteSpecBuilder<Integer> specsBuilder = BlindsAnteSpec.builder();
		specsBuilder.bbPlayer(1);
		specsBuilder.sbPlayer(0);
		specsBuilder.anteValue(anteOpt.isPresent() ? anteOpt.get() : 0);
		specsBuilder.enableBlinds(true);
		specsBuilder.enableAnte(anteOpt.isPresent() && anteOpt.get() > 0);
		specsBuilder.sbValue(sbOpt.isPresent() ? sbOpt.get() : 0);
		specsBuilder.bbValue(bbOpt.get());
		specsBuilder.isCash(false);
		specsBuilder.playersHavingToPayEnteringBB(Collections.<Integer>emptyList());
		final int nbBetRounds = 1;
		final BetRoundSpec<Integer> betSpecs = new BetRoundSpec<Integer>(new Integer(2), bbOpt.get());
		return Optional.of(new NLHand<Integer>(playersData, specsBuilder.build(), betSpecs, nbBetRounds));
	}

	public static void main(String[] args) {
		final Optional<NLHand<Integer>> handOpt = getHand(args);
		if (!handOpt.isPresent()) {
			log.error("Unable to parse hand settings");
			return;
		}
		final Optional<String> eqOpt = getArgument(args, equityPathPrefix);
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
		final Optional<String> svgOpt = getArgument(args, svgPathPrefix);
		log.info("Creating CSCFRM environment");
		final ThreePlayersPreflopCSCFRM cfrm = new ThreePlayersPreflopCSCFRM(handOpt.get(), tables, svgOpt.orNull());
		try {
			cfrm.load();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		log.info("Adding shutdown hook to save the data on gentle kill");
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

			@Override
			public void run() {
				log.info("Shutting down");
				try {
					if (cfrm.runner.isRunning()) {
						log.info("Waiting runner termination");
						cfrm.runner.stopAndAwaitTermination();
						log.info("Saving...");
						cfrm.save();
					}
					cfrm.printStrategies();
				} catch (InterruptedException | IOException e) {
					e.printStackTrace();
				}
			}
		}));
		if (getArgument(args, interactiveArg).isPresent()) {
			log.info("Interactive mode");
			try {
				interactive(cfrm);
			} catch (Exception e) {

			}
		} else {
			log.info("Non-Interactive mode, running CSCFRM");
			cfrm.runner.start();
			try {
				log.info(
						"Trying to read on standard input. Failure will let run, on success hitting Enter will stop and save.");
				System.in.read();
				System.exit(0);
			} catch (Exception e) {

			}
		}
	}

	private static final void interactive(final ThreePlayersPreflopCSCFRM cscfrm)
			throws InterruptedException, IOException {
		try (final Scanner scan = new Scanner(System.in);) {
			while (true) {
				log.info("Enter one of those commands : run | stop | print | exit");
				final String line = scan.nextLine();
				switch (line) {
				case "run":
					if (cscfrm.runner.isRunning()) {
						log.info("Already running");
						continue;
					}
					cscfrm.runner.start();
					break;
				case "stop":
					if (!cscfrm.runner.isRunning()) {
						log.info("Not running");
						continue;
					}
					cscfrm.runner.stopAndAwaitTermination();
					cscfrm.save();
					break;
				case "print":
					if (cscfrm.runner.isRunning()) {
						log.info("Can't print strategies while running");
						continue;
					}
					cscfrm.printStrategies();
					break;
				case "exit":
					System.exit(0);
					return;
				default:
					log.info("Unknown command \"{}\"", line);
				}
			}
		}
	}

	private final CSCFRMData<NLBetTreeNode<Integer>> data;
	private final CSCFRMRunner runner;
	private final String svgPath;
	private final WaughIndexer holeCardsIndexer;

	public ThreePlayersPreflopCSCFRM(final NLHand<Integer> hand, final NLBetTreeAbstractor<Integer> betTreeAbstractor,
			final ThreePlayersPreflopReducedEquityTable tables, final String svgPath) {
		this.svgPath = svgPath;
		this.holeCardsIndexer = tables.getHoleCardsIndexer();
		final NLHE3PlayersPreflopEquityProvider equityProvider = new NLHE3PlayersPreflopEquityProvider(tables);
		final NLAbstractedBetTree<Integer> tree = new NLAbstractedBetTree<Integer>(hand, betTreeAbstractor, true);
		final NoLimitHoldEm<Integer> game = new NoLimitHoldEm<Integer>(tree, new int[] { 169 }, equityProvider);
		final NLHEPreflopChancesProducer chancesProducer = new NLHEPreflopChancesProducer(3);
		final int[][] chancesSizes = new int[][] { { 169, 169, 169 } };
		final CSCFRMChancesSynchronizer synchronizer = new CSCFRMMutexChancesSynchronizer(chancesProducer,
				chancesSizes);
		final CSCFRMData<NLBetTreeNode<Integer>> data = this.data = new CSCFRMData<>(game);
		final int nbTrainerThreads = Math.max(Runtime.getRuntime().availableProcessors(), 1);
		this.runner = new CSCFRMRunner(data, synchronizer, nbTrainerThreads);
	}

	public ThreePlayersPreflopCSCFRM(final NLHand<Integer> hand, final ThreePlayersPreflopReducedEquityTable tables,
			final String svgPath) {
		this(hand, new NLPushFoldBetTreeAbstractor<Integer>(), tables, svgPath);
	}

	private void load() throws IOException {
		if (svgPath == null) {
			log.warn("No svg path provided, not loading");
			return;
		}
		final File file = Paths.get(svgPath).toFile();
		if (!file.exists()) {
			log.warn("No file at path {}, may be initial run", svgPath);
			return;
		}
		try (final FileInputStream fis = new FileInputStream(file)) {
			data.fill(fis);
		} catch (IOException e) {
			log.error("Failed to load file at path {}", svgPath);
			throw e;
		}
	}

	private void save() throws IOException {
		if (svgPath == null) {
			log.warn("No svg path provided, not saving");
			return;
		}
		final File file = Paths.get(svgPath).toFile();
		if (file.exists()) {
			if (!file.delete()) {
				log.error("Failed to delete file at path {}, cannot save", svgPath);
				return;
			}
		}
		try (final FileOutputStream fos = new FileOutputStream(file)) {
			data.write(fos);
		} catch (IOException e) {
			log.error("Failed to save file at path {}", svgPath);
			throw e;
		}
	}

	private void printStrategies() {
		HEPreflopHelper.printStrategies(data, holeCardsIndexer);
	}

}
