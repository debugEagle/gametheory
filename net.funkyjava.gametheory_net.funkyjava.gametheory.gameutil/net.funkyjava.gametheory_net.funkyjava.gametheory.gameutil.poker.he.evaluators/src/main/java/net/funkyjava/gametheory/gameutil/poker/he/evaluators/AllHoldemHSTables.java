package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Cards52SpecTranslator;
import net.funkyjava.gametheory.gameutil.cards.indexing.CardsGroupsIndexer;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.twoplustwo.TwoPlusTwoEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

@Slf4j
public class AllHoldemHSTables {

	public enum Streets {
		RIVER(null), TURN(RIVER), FLOP(TURN), PREFLOP(FLOP);

		private Streets nextStreet;

		private Streets(Streets nextStreet) {
			this.nextStreet = nextStreet;
		}

		public Streets getNextStreet() {
			return nextStreet;
		}
	}

	public enum HSType {
		HS, EHS, EHS2
	}

	private static final String fileName = "ALL_HE_HS.dat";
	private static boolean isFilled = false;
	private static final int nbHoleCards = new WaughIndexer(new int[] { 2 }).getIndexSize();
	private static final int nbFlops = new WaughIndexer(new int[] { 2, 3 }).getIndexSize();
	private static final int nbTurns = new WaughIndexer(new int[] { 2, 4 }).getIndexSize();
	private static final int nbRivers = new WaughIndexer(new int[] { 2, 5 }).getIndexSize();

	private static final double[] preflopEHS = new double[nbHoleCards];
	private static final double[] preflopEHS2 = new double[nbHoleCards];
	private static final double[] flopHSTable = new double[nbFlops];
	private static final double[] flopEHSTable = new double[nbFlops];
	private static final double[] flopEHS2Table = new double[nbFlops];
	private static final double[] turnHSTable = new double[nbTurns];
	private static final double[] turnEHSTable = new double[nbTurns];
	private static final double[] turnEHS2Table = new double[nbTurns];
	private static final double[] riverHSTable = new double[nbRivers];

	static {
		log.info("Hole cards : {} Flops {} Turn {} River {}", nbHoleCards, nbFlops, nbTurns, nbRivers);
	}

	public AllHoldemHSTables() {

	}

	public static synchronized void compute() {
		final CardsGroupsIndexer holeCardsIndexer = new WaughIndexer(new int[] { 2 });
		final CardsGroupsIndexer flopIndexer = new WaughIndexer(new int[] { 2, 3 });
		;
		final CardsGroupsIndexer turnIndexer = new WaughIndexer(new int[] { 2, 4 });
		;
		final CardsGroupsIndexer riverIndexer = new WaughIndexer(new int[] { 2, 5 });
		final TwoPlusTwoEvaluator eval = new TwoPlusTwoEvaluator();
		final int nbHoleCards = holeCardsIndexer.getIndexSize();
		final int nbFlops = flopIndexer.getIndexSize();
		final int nbTurns = turnIndexer.getIndexSize();
		final int nbRiver = riverIndexer.getIndexSize();

		final Cards52SpecTranslator translateToEval = new Cards52SpecTranslator(holeCardsIndexer.getCardsSpec(),
				eval.getCardsSpec());

		final double[] preflopEHS = AllHoldemHSTables.preflopEHS;
		final double[] preflopEHS2 = AllHoldemHSTables.preflopEHS2;
		final long[] preflopSd = new long[nbHoleCards];
		final boolean[] preflopHits = new boolean[nbHoleCards];

		final boolean[] flopHits = new boolean[nbFlops];
		final double[] flopHSTable = AllHoldemHSTables.flopHSTable;
		final double[] flopEHSTable = AllHoldemHSTables.flopEHSTable;
		final double[] flopEHS2Table = AllHoldemHSTables.flopEHS2Table;
		final long[] flopSd = new long[nbFlops];

		final boolean[] turnHits = new boolean[nbTurns];
		final double[] turnHSTable = AllHoldemHSTables.turnHSTable;
		final double[] turnEHSTable = AllHoldemHSTables.turnEHSTable;
		final double[] turnEHS2Table = AllHoldemHSTables.turnEHS2Table;
		final long[] turnSd = new long[nbTurns];

		final boolean[] riverHits = new boolean[nbRiver];
		final double[] riverHSTable = AllHoldemHSTables.riverHSTable;

		int h1, h2, o1, o2, f1, f2, f3, t, r;

		final int[] hole = new int[2];

		final int[] flop = new int[3];
		final int[] turn = new int[4];
		final int[] river = new int[5];

		// To index
		final int[][] holeCards = { hole };
		final int[][] hFlopCards = { hole, flop };
		final int[][] hTurnCards = { hole, turn };
		final int[][] hRiverCards = { hole, river };

		// To evaluate hands with 2+2 evaluator
		final int[] hCards = new int[7];
		final int[] oCards = new int[7];
		double ehs;
		long deck = 0l;
		for (h1 = 0; h1 < 51; h1++) {
			hCards[0] = translateToEval.translate(hole[0] = h1);
			for (h2 = h1 + 1; h2 < 52; h2++) {
				hCards[1] = translateToEval.translate(hole[1] = h2);
				final int holeIndex = holeCardsIndexer.indexOf(holeCards);
				if (preflopHits[holeIndex])
					// Already done for those hole cards
					continue;
				deck |= (0x1l << h1) | (0x1l << h2);
				preflopHits[holeIndex] = true;
				for (f1 = 0; f1 < 50; f1++) {
					if (((0x1l << f1) & deck) != 0l)
						continue;
					hCards[2] = oCards[2] = translateToEval.translate(flop[0] = river[0] = turn[0] = f1);
					for (f2 = f1 + 1; f2 < 51; f2++) {
						if (((0x1l << f2) & deck) != 0l)
							continue;
						hCards[3] = oCards[3] = translateToEval.translate(flop[1] = river[1] = turn[1] = f2);
						for (f3 = f2 + 1; f3 < 52; f3++) {
							if (((0x1l << f3) & deck) != 0l)
								continue;
							hCards[4] = oCards[4] = translateToEval.translate(flop[2] = river[2] = turn[2] = f3);
							final int flopIndex = flopIndexer.indexOf(hFlopCards);
							if (!flopHits[flopIndex]) {
								deck |= (0x1l << f1) | (0x1l << f2) | (0x1l << f3);
								flopHits[flopIndex] = true;
								for (t = 0; t < 52; t++) {
									if (((0x1l << t) & deck) != 0l)
										continue;
									hCards[5] = oCards[5] = translateToEval.translate(turn[3] = river[3] = t);
									final int turnIndex = turnIndexer.indexOf(hTurnCards);
									if (!turnHits[turnIndex]) {
										deck |= (0x1l << t);
										turnHits[turnIndex] = true;
										long turnTotal = 0;
										double turnEHS2 = 0, turnEHS = 0;

										for (r = 0; r < 52; r++) {
											if (((0x1l << r) & deck) != 0l)
												continue;
											deck |= (0x1l << r);
											hCards[6] = oCards[6] = translateToEval.translate(river[4] = r);
											final int riverIndex = riverIndexer.indexOf(hRiverCards);
											if (!riverHits[riverIndex]) {
												riverHits[riverIndex] = true;

												final int hVal = eval.get7CardsEval(hCards);
												int rWin = 0, rLose = 0, rTie = 0;
												for (o1 = 0; o1 < 51; o1++) {
													if (((0x1l << o1) & deck) != 0l)
														continue;
													deck |= (0x1l << o1);
													oCards[0] = translateToEval.translate(o1);
													for (o2 = o1 + 1; o2 < 52; o2++) {
														if (((0x1l << o2) & deck) != 0l)
															continue;
														oCards[1] = translateToEval.translate(o2);
														final int oVal = eval.get7CardsEval(oCards);
														if (hVal > oVal) {
															// Win
															rWin++;
														} else if (hVal < oVal) {
															// Lose
															rLose++;
														} else {
															// Tie
															rTie++;
														}
													}
													deck ^= (0x1l << o1);
												}
												riverHSTable[riverIndex] = (rWin + rTie / 2.0) / (rWin + rTie + rLose);
											} // End computing for one river
											deck ^= (0x1l << r);
											turnEHS2 += (ehs = riverHSTable[riverIndex]) * ehs;
											turnEHS += ehs;
											turnTotal++;
										} // All rivers computed
										turnEHSTable[turnIndex] = turnEHS;
										turnEHS2Table[turnIndex] = turnEHS2;
										turnSd[turnIndex] = turnTotal;
										// Compute Turn HS
										int win = 0, lose = 0, tie = 0;
										final int hVal = eval.get6CardsEval(hCards);
										for (o1 = 0; o1 < 51; o1++) {
											if (((0x1l << o1) & deck) != 0l)
												continue;
											deck |= (0x1l << o1);
											oCards[0] = translateToEval.translate(o1);
											for (o2 = o1 + 1; o2 < 52; o2++) {
												if (((0x1l << o2) & deck) != 0l)
													continue;
												oCards[1] = translateToEval.translate(o2);
												final int oVal = eval.get6CardsEval(oCards);
												if (hVal > oVal) {
													// Win
													win++;
												} else if (hVal < oVal) {
													// Lose
													lose++;
												} else {
													// Tie
													tie++;
												}
											}
											deck ^= (0x1l << o1);
										}
										turnHSTable[turnIndex] = (win + tie / 2.0) / (win + lose + tie);
										deck ^= (0x1l << t);

									} // Turn computed
									flopEHSTable[flopIndex] += turnEHSTable[turnIndex];
									flopEHS2Table[flopIndex] += turnEHS2Table[turnIndex];
									flopSd[flopIndex] += turnSd[turnIndex];
								} // End computing for one flop
									// Computing flop HS
								int win = 0, lose = 0, tie = 0;
								final int hVal = eval.get5CardsEval(hCards);
								for (o1 = 0; o1 < 51; o1++) {
									if (((0x1l << o1) & deck) != 0l)
										continue;
									deck |= (0x1l << o1);
									oCards[0] = translateToEval.translate(o1);
									for (o2 = o1 + 1; o2 < 52; o2++) {
										if (((0x1l << o2) & deck) != 0l)
											continue;
										oCards[1] = translateToEval.translate(o2);
										final int oVal = eval.get5CardsEval(oCards);
										if (hVal > oVal) {
											win++;
										} else if (hVal < oVal) {
											lose++;
										} else {
											tie++;
										}
									}
									deck ^= (0x1l << o1);
								}
								flopHSTable[flopIndex] = (win + tie / 2.0) / (win + lose + tie);
								deck ^= (0x1l << f1) | (0x1l << f2) | (0x1l << f3);
							}
							preflopEHS[holeIndex] += flopEHSTable[flopIndex];
							preflopEHS2[holeIndex] += flopEHS2Table[flopIndex];
							preflopSd[holeIndex] += flopSd[flopIndex];
						} // All flops computed
					}
				}
				deck ^= (0x1l << h1) | (0x1l << h2);
			}
		}

		for (int i = 0; i < nbHoleCards; i++) {
			preflopEHS[i] /= preflopSd[i];
			preflopEHS2[i] /= preflopSd[i];
		}
		for (int i = 0; i < nbFlops; i++) {
			flopEHSTable[i] /= flopSd[i];
			flopEHS2Table[i] /= flopSd[i];
		}
		for (int i = 0; i < nbTurns; i++) {
			turnEHSTable[i] /= turnSd[i];
			turnEHS2Table[i] /= turnSd[i];
		}
		isFilled = true;
	}

	public static double[] getPreflopEHSTable() {
		checkFilled();
		return preflopEHS;
	}

	public static double[] getPreflopEHS2Table() {
		checkFilled();
		return preflopEHS2;
	}

	public static double[] getFlopHSTable() {
		checkFilled();
		return flopHSTable;
	}

	public static double[] getFlopEHSTable() {
		checkFilled();
		return flopEHSTable;
	}

	public static double[] getFlopEHS2Table() {
		checkFilled();
		return flopEHS2Table;
	}

	public static double[] getTurnHSTable() {
		checkFilled();
		return turnHSTable;
	}

	public static double[] getTurnEHSTable() {
		checkFilled();
		return turnEHSTable;
	}

	public static double[] getTurnEHS2Table() {
		checkFilled();
		return turnEHS2Table;
	}

	public static double[] getRiverHSTable() {
		checkFilled();
		return riverHSTable;
	}

	public static boolean isFilled() {
		return isFilled;
	}

	private static void checkFilled() {
		checkState(isFilled, "AllHoldemHSTable must be filled first");
	}

	public CardsGroupsIndexer getHoleCardsIndexer() {
		return new WaughIndexer(new int[] { 2 });
	}

	public CardsGroupsIndexer getFlopIndexer() {
		return new WaughIndexer(new int[] { 2, 3 });
	}

	public CardsGroupsIndexer getTurnIndexer() {
		return new WaughIndexer(new int[] { 2, 4 });
	}

	public CardsGroupsIndexer getRiverIndexer() {
		return new WaughIndexer(new int[] { 2, 5 });
	}

	public static double[] getTable(final Streets street, final HSType hsType) {
		switch (street) {
		case FLOP:
			switch (hsType) {
			case EHS:
				return getFlopEHSTable();
			case EHS2:
				return getFlopEHS2Table();
			case HS:
				return getFlopHSTable();
			default:
				break;
			}
			break;
		case PREFLOP:
			switch (hsType) {
			case EHS:
				return getPreflopEHSTable();
			case EHS2:
				return getPreflopEHS2Table();
			default:
				break;
			}
			break;
		case TURN:
			switch (hsType) {
			case EHS:
				return getTurnEHSTable();
			case EHS2:
				return getTurnEHS2Table();
			case HS:
				return getTurnHSTable();
			default:
				break;
			}
			break;
		case RIVER:
			switch (hsType) {
			case HS:
				return getRiverHSTable();
			default:
				break;
			}
			break;
		default:
			break;
		}
		throw new IllegalArgumentException("Wrong street(" + street + ") / HSType (" + hsType + ") combination");
	}

	public static synchronized void writeTo(Path path) throws IOException {
		checkFilled();
		checkArgument(!Files.exists(path), "File " + path.toAbsolutePath().toString() + " already exists");
		checkArgument(Files.exists(path.getParent()) && Files.isDirectory(path.getParent()),
				"File " + path.toAbsolutePath().toString() + " parent folder doesn't exist");
		final Set<OpenOption> options = new HashSet<OpenOption>();
		options.add(StandardOpenOption.WRITE);
		options.add(StandardOpenOption.READ);
		options.add(StandardOpenOption.CREATE);

		final File f = path.toFile();
		try (final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
			final ZipEntry e = new ZipEntry(fileName);
			out.putNextEntry(e);

			final ByteBuffer buffer = ByteBuffer.allocate(800_000);

			write(out, buffer, preflopEHS);
			write(out, buffer, preflopEHS2);
			write(out, buffer, flopHSTable);
			write(out, buffer, flopEHSTable);
			write(out, buffer, flopEHS2Table);
			write(out, buffer, turnHSTable);
			write(out, buffer, turnEHSTable);
			write(out, buffer, turnEHS2Table);
			write(out, buffer, riverHSTable);
			out.closeEntry();
		}
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

	public static synchronized void readFrom(Path path) throws IOException {
		checkArgument(Files.exists(path), "File " + path.toAbsolutePath().toString() + " doesn't exists");
		checkArgument(!Files.isDirectory(path), "File " + path.toAbsolutePath().toString() + " is a folder");
		log.info("Reading holdem HS tabels from {}", path);
		final Set<OpenOption> options = new HashSet<OpenOption>();
		options.add(StandardOpenOption.WRITE);
		options.add(StandardOpenOption.READ);
		options.add(StandardOpenOption.CREATE);

		try (final ZipFile zipFile = new ZipFile(path.toFile())) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				if (!fileName.equals(entry.getName()))
					continue;
				final InputStream stream = zipFile.getInputStream(entry);

				final ByteBuffer buffer = ByteBuffer.allocate(800_000);
				log.info("Reading preflop EHS");
				read(stream, buffer, preflopEHS);
				log.info("Reading preflop EHS2");
				read(stream, buffer, preflopEHS2);
				log.info("Reading flop HS");
				read(stream, buffer, flopHSTable);
				log.info("Reading flop EHS");
				read(stream, buffer, flopEHSTable);
				log.info("Reading flop EHS2");
				read(stream, buffer, flopEHS2Table);
				log.info("Reading turn HS");
				read(stream, buffer, turnHSTable);
				log.info("Reading turn EHS");
				read(stream, buffer, turnEHSTable);
				log.info("Reading turn EHS2");
				read(stream, buffer, turnEHS2Table);
				log.info("Reading river HS");
				read(stream, buffer, riverHSTable);
				isFilled = true;
				return;
			}
		}
		throw new IllegalArgumentException("The zip file doesn't contain a file named " + fileName);
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
}
