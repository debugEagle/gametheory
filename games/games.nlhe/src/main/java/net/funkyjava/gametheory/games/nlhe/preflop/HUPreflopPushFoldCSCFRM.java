package net.funkyjava.gametheory.games.nlhe.preflop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.google.common.base.Optional;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.cscfrm.CSCFRMChancesSynchronizer;
import net.funkyjava.gametheory.cscfrm.CSCFRMData;
import net.funkyjava.gametheory.cscfrm.CSCFRMMutexChancesSynchronizer;
import net.funkyjava.gametheory.cscfrm.CSCFRMNode;
import net.funkyjava.gametheory.cscfrm.CSCFRMRunner;
import net.funkyjava.gametheory.extensiveformgame.ActionNode;
import net.funkyjava.gametheory.games.nlhe.NoLimitHoldEm;
import net.funkyjava.gametheory.gameutil.cards.indexing.CardsGroupsIndexer;
import net.funkyjava.gametheory.gameutil.poker.bets.NLHandRounds;
import net.funkyjava.gametheory.gameutil.poker.bets.moves.Move;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BetRoundSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.BlindsAnteSpec.BlindsAnteSpecBuilder;
import net.funkyjava.gametheory.gameutil.poker.bets.rounds.data.NoBetPlayerData;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLAbstractedBetTree;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLBetTreeNode;
import net.funkyjava.gametheory.gameutil.poker.bets.tree.NLPushFoldBetTreeAbstractor;
import net.funkyjava.gametheory.gameutil.poker.he.evaluators.HUPreflopEquityTables;

@Slf4j
public class HUPreflopPushFoldCSCFRM {

	private static final String equityPathPrefix = "equity=";
	private static final String bbPrefix = "bb=";
	private static final String sbPrefix = "sb=";
	private static final String antePrefix = "ante=";
	private static final String p1StackPrefix = "p1Stack=";
	private static final String p2StackPrefix = "p2Stack=";
	private static final String svgPathPrefix = "svg=";
	private static final String interactiveArg = "-i";

	private static HUPreflopEquityTables getTables(final String path) throws IOException, ClassNotFoundException {
		try (final FileInputStream fis = new FileInputStream(Paths.get(path).toFile());
				final ObjectInputStream objectInputStream = new ObjectInputStream(fis)) {
			final HUPreflopEquityTables tables = (HUPreflopEquityTables) objectInputStream.readObject();
			return tables;
		}
	}

	private static Optional<String> getArgument(String[] args, String prefix) {
		for (String arg : args) {
			if (arg.startsWith(prefix)) {
				return Optional.of(arg.substring(prefix.length(), arg.length()));
			}
		}
		log.error("Argument {} not found", prefix);
		return Optional.absent();
	}

	private static Optional<Integer> getIntArgument(String[] args, String prefix) {
		final Optional<String> strOpt = getArgument(args, prefix);
		if (!strOpt.isPresent()) {
			return Optional.absent();
		}
		try {
			final Integer res = Integer.parseInt(strOpt.get());
			return Optional.of(res);
		} catch (NumberFormatException e) {
			log.error("Unable to parse integer arg {}", prefix);
			return Optional.absent();
		}
	}

	private static Optional<Integer> getStrictlyPositiveIntArgument(String[] args, String prefix) {
		final Optional<Integer> intOpt = getIntArgument(args, prefix);
		if (intOpt.isPresent()) {
			if (intOpt.get() <= 0) {
				log.error("Argument {} expected to be strictly positive", prefix);
				return Optional.absent();
			}
		}
		return intOpt;
	}

	private static Optional<Integer> getPositiveIntArgument(String[] args, String prefix) {
		final Optional<Integer> intOpt = getIntArgument(args, prefix);
		if (intOpt.isPresent()) {
			if (intOpt.get() < 0) {
				log.error("Argument {} expected to be positive", prefix);
				return Optional.absent();
			}
		}
		return intOpt;
	}

	private static Optional<NLHandRounds<Integer>> getHand(String[] args) {
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
		final NoBetPlayerData<Integer> p1Data = new NoBetPlayerData<Integer>(0, p1Opt.get(), true);
		final NoBetPlayerData<Integer> p2Data = new NoBetPlayerData<Integer>(1, p2Opt.get(), true);
		final List<NoBetPlayerData<Integer>> playersData = new LinkedList<>();
		playersData.add(p1Data);
		playersData.add(p2Data);
		final BlindsAnteSpecBuilder<Integer> specsBuilder = BlindsAnteSpec.builder();
		specsBuilder.bbPlayer(1);
		specsBuilder.sbPlayer(0);
		specsBuilder.anteValue(anteOpt.isPresent() ? anteOpt.get() : 0);
		specsBuilder.enableBlinds(true);
		specsBuilder.enableAnte(anteOpt.isPresent() && anteOpt.get() > 0);
		specsBuilder.sbValue(sbOpt.isPresent() ? sbOpt.get() : 0);
		specsBuilder.bbValue(bbOpt.get());
		specsBuilder.isCash(false);
		specsBuilder.playersHavingToPayEnteringBB(Collections.<Integer> emptyList());
		final int nbBetRounds = 1;
		final BetRoundSpec<Integer> betSpecs = new BetRoundSpec<Integer>(new Integer(0), bbOpt.get());
		return Optional.of(new NLHandRounds<Integer>(playersData, specsBuilder.build(), betSpecs, nbBetRounds));
	}

	public static void main(String[] args) {
		final Optional<NLHandRounds<Integer>> handOpt = getHand(args);
		if (!handOpt.isPresent()) {
			log.error("Unable to parse hand settings");
			return;
		}
		final Optional<String> eqOpt = getArgument(args, equityPathPrefix);
		if (!eqOpt.isPresent()) {
			return;
		}
		log.info("Loading equity tables");
		HUPreflopEquityTables tables;
		try {
			tables = getTables(eqOpt.get());
		} catch (Exception e) {
			log.error("Unable to load HU preflop equity tables", e);
			return;
		}
		final Optional<String> svgOpt = getArgument(args, svgPathPrefix);
		log.info("Creating CSCFRM environment");
		final HUPreflopPushFoldCSCFRM cfrm = new HUPreflopPushFoldCSCFRM(handOpt.get(), tables, svgOpt.orNull());
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

	private static final void interactive(final HUPreflopPushFoldCSCFRM cscfrm)
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
				}
			}
		}
	}

	private final HUPreflopEquityTables tables;
	private final CSCFRMData<NLBetTreeNode<Integer>> data;
	private final CSCFRMRunner runner;
	private final String svgPath;

	public HUPreflopPushFoldCSCFRM(final NLHandRounds<Integer> hand, final HUPreflopEquityTables tables,
			final String svgPath) {
		this.tables = tables;
		this.svgPath = svgPath;
		final NLPushFoldBetTreeAbstractor<Integer> pushFoldAbstractor = new NLPushFoldBetTreeAbstractor<Integer>();
		final NLHEHUPreflopEquityProvider equityProvider = new NLHEHUPreflopEquityProvider(tables);
		final NLAbstractedBetTree<Integer> tree = new NLAbstractedBetTree<Integer>(hand, pushFoldAbstractor, true);
		final NoLimitHoldEm<Integer> game = new NoLimitHoldEm<Integer>(tree, new int[] { 169 }, equityProvider);
		final NLHEPreflopChancesProducer chancesProducer = new NLHEPreflopChancesProducer(2);
		final int[][] chancesSizes = new int[][] { { 169, 169 } };
		final CSCFRMChancesSynchronizer synchronizer = new CSCFRMMutexChancesSynchronizer(chancesProducer,
				chancesSizes);
		final CSCFRMData<NLBetTreeNode<Integer>> data = this.data = new CSCFRMData<>(game);
		final int nbTrainerThreads = Math.max(Runtime.getRuntime().availableProcessors() - 1, 1);
		this.runner = new CSCFRMRunner(data, synchronizer, nbTrainerThreads);
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
		Map<ActionNode<NLBetTreeNode<Integer>>, CSCFRMNode[]> allNodes = data.nodesForEachActionNode();
		for (ActionNode<NLBetTreeNode<Integer>> actionNode : allNodes.keySet()) {
			final CSCFRMNode[] nodes = allNodes.get(actionNode);
			printStrategy(actionNode, nodes, tables.getHoleCardsIndexer());
			log.info("");
		}
		final long iterations = data.iterations.longValue();
		log.info("Nb iterations : {}", iterations);
		log.info("Utility : {}", data.getUtilityAvg());
	}

	private static void printStrategy(final ActionNode<NLBetTreeNode<Integer>> node, final CSCFRMNode[] chanceNodes,
			final CardsGroupsIndexer preflopIndexer) {
		log.info("##################################################################");
		log.info(node.id.getHand().movesString());
		log.info("##################################################################");
		LinkedHashMap<Move<Integer>, NLBetTreeNode<Integer>> children = node.id.getChildren();
		int childIndex = 0;
		for (Move<Integer> move : children.keySet()) {
			log.info("### " + move + " strategy :");
			HEPreflopHelper.printMovePureStrategy(childIndex, chanceNodes, preflopIndexer);
			log.info("");
			childIndex++;
		}
	}

}
