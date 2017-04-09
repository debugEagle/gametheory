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

import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.funkyjava.gametheory.gameutil.cards.Cards52SpecTranslator;
import net.funkyjava.gametheory.gameutil.cards.DefaultIntCardsSpecs;
import net.funkyjava.gametheory.gameutil.cards.IntCardsSpec;
import net.funkyjava.gametheory.gameutil.cards.indexing.CardsGroupsIndexer;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.HoldemFullEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.handeval.twoplustwo.TwoPlusTwoEvaluator;
import net.funkyjava.gametheory.gameutil.poker.he.indexing.waugh.WaughIndexer;

@Slf4j
public class AllHoldemHSTables<PreflopIndexer extends CardsGroupsIndexer, FlopIndexer extends CardsGroupsIndexer, TurnIndexer extends CardsGroupsIndexer, RiverIndexer extends CardsGroupsIndexer> {

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

  @Getter
  private boolean isFilled = false;
  @Getter
  private final PreflopIndexer holeCardsIndexer;
  @Getter
  private final FlopIndexer flopCardsIndexer;
  @Getter
  private final TurnIndexer turnCardsIndexer;
  @Getter
  private final RiverIndexer riverCardsIndexer;
  @Getter
  private final HoldemFullEvaluator eval;
  @Getter
  private final int nbHoleCards;
  @Getter
  private final int nbFlops;
  @Getter
  private final int nbTurns;
  @Getter
  private final int nbRivers;

  @Getter
  private final double[] preflopEHSTable;
  @Getter
  private final double[] preflopEHS2Table;
  @Getter
  private final double[] flopHSTable;
  @Getter
  private final double[] flopEHSTable;
  @Getter
  private final double[] flopEHS2Table;
  @Getter
  private final double[] turnHSTable;
  @Getter
  private final double[] turnEHSTable;
  @Getter
  private final double[] turnEHS2Table;
  @Getter
  private final double[] riverHSTable;

  public AllHoldemHSTables(@NonNull final HoldemFullEvaluator eval,
      @NonNull final PreflopIndexer holeCardsIndexer, @NonNull final FlopIndexer flopCardsIndexer,
      @NonNull final TurnIndexer turnCardsIndexer, @NonNull final RiverIndexer riverCardsIndexer) {
    this.eval = eval;
    this.holeCardsIndexer = holeCardsIndexer;
    this.flopCardsIndexer = flopCardsIndexer;
    this.turnCardsIndexer = turnCardsIndexer;
    this.riverCardsIndexer = riverCardsIndexer;
    this.nbHoleCards = holeCardsIndexer.getIndexSize();
    this.nbFlops = flopCardsIndexer.getIndexSize();
    this.nbTurns = turnCardsIndexer.getIndexSize();
    this.nbRivers = riverCardsIndexer.getIndexSize();
    preflopEHSTable = new double[nbHoleCards];
    preflopEHS2Table = new double[nbHoleCards];
    flopHSTable = new double[nbFlops];
    flopEHSTable = new double[nbFlops];
    flopEHS2Table = new double[nbFlops];
    turnHSTable = new double[nbTurns];
    turnEHSTable = new double[nbTurns];
    turnEHS2Table = new double[nbTurns];
    riverHSTable = new double[nbRivers];
  }

  public synchronized void compute() {
    final IntCardsSpec cardsSpec = DefaultIntCardsSpecs.getDefault();
    final Cards52SpecTranslator translateToEval =
        new Cards52SpecTranslator(cardsSpec, eval.getCardsSpec());
    final Cards52SpecTranslator preflopTranslator =
        new Cards52SpecTranslator(cardsSpec, holeCardsIndexer.getCardsSpec());
    final Cards52SpecTranslator flopTranslator =
        new Cards52SpecTranslator(cardsSpec, flopCardsIndexer.getCardsSpec());
    final Cards52SpecTranslator turnTranslator =
        new Cards52SpecTranslator(cardsSpec, turnCardsIndexer.getCardsSpec());
    final Cards52SpecTranslator riverTranslator =
        new Cards52SpecTranslator(cardsSpec, riverCardsIndexer.getCardsSpec());
    final double[] preflopEHS = this.preflopEHSTable;
    final double[] preflopEHS2 = this.preflopEHS2Table;
    final long[] preflopSd = new long[nbHoleCards];
    final boolean[] preflopHits = new boolean[nbHoleCards];

    final boolean[] flopHits = new boolean[nbFlops];
    final double[] flopHSTable = this.flopHSTable;
    final double[] flopEHSTable = this.flopEHSTable;
    final double[] flopEHS2Table = this.flopEHS2Table;
    final long[] flopSd = new long[nbFlops];

    final boolean[] turnHits = new boolean[nbTurns];
    final double[] turnHSTable = this.turnHSTable;
    final double[] turnEHSTable = this.turnEHSTable;
    final double[] turnEHS2Table = this.turnEHS2Table;
    final long[] turnSd = new long[nbTurns];

    final boolean[] riverHits = new boolean[nbRivers];
    final double[] riverHSTable = this.riverHSTable;

    final CardsGroupsIndexer holeCardsIndexer = this.holeCardsIndexer;
    final CardsGroupsIndexer flopCardsIndexer = this.flopCardsIndexer;
    final CardsGroupsIndexer turnCardsIndexer = this.turnCardsIndexer;
    final CardsGroupsIndexer riverCardsIndexer = this.riverCardsIndexer;

    int h1, h2, o1, o2, f1, f2, f3, t, r;

    final int[] holeCardsForPreflopIndexing = new int[2];
    final int[] holeCardsForFlopIndexing = new int[2];
    final int[] holeCardsForTurnIndexing = new int[2];
    final int[] holeCardsForRiverIndexing = new int[2];

    final int[] boardCardsForFlopIndexing = new int[3];
    final int[] boardCardsForTurnIndexing = new int[4];
    final int[] boardCardsForRiverIndexing = new int[5];

    // To index
    final int[][] holeCards = {holeCardsForPreflopIndexing};
    final int[][] hFlopCards = {holeCardsForFlopIndexing, boardCardsForFlopIndexing};
    final int[][] hTurnCards = {holeCardsForTurnIndexing, boardCardsForTurnIndexing};
    final int[][] hRiverCards = {holeCardsForRiverIndexing, boardCardsForRiverIndexing};

    // To evaluate hands with 2+2 evaluator
    final int[] hCards = new int[7];
    final int[] oCards = new int[7];
    double ehs;
    long deck = 0l;
    for (h1 = 0; h1 < 51; h1++) {
      hCards[0] = translateToEval.translate(h1);
      holeCardsForPreflopIndexing[0] = preflopTranslator.translate(h1);
      holeCardsForFlopIndexing[0] = flopTranslator.translate(h1);
      holeCardsForTurnIndexing[0] = turnTranslator.translate(h1);
      holeCardsForRiverIndexing[0] = riverTranslator.translate(h1);
      for (h2 = h1 + 1; h2 < 52; h2++) {
        hCards[1] = translateToEval.translate(h2);
        holeCardsForPreflopIndexing[1] = preflopTranslator.translate(h2);
        holeCardsForFlopIndexing[1] = flopTranslator.translate(h2);
        holeCardsForTurnIndexing[1] = turnTranslator.translate(h2);
        holeCardsForRiverIndexing[1] = riverTranslator.translate(h2);
        final int holeIndex = holeCardsIndexer.indexOf(holeCards);
        if (preflopHits[holeIndex]) {
          // Already done for those hole cards
          continue;
        }
        deck |= (0x1l << h1) | (0x1l << h2);
        preflopHits[holeIndex] = true;
        for (f1 = 0; f1 < 50; f1++) {
          if (((0x1l << f1) & deck) != 0l) {
            continue;
          }
          hCards[2] = oCards[2] = translateToEval.translate(f1);
          boardCardsForFlopIndexing[0] = flopTranslator.translate(f1);
          boardCardsForTurnIndexing[0] = turnTranslator.translate(f1);
          boardCardsForRiverIndexing[0] = riverTranslator.translate(f1);
          for (f2 = f1 + 1; f2 < 51; f2++) {
            if (((0x1l << f2) & deck) != 0l) {
              continue;
            }
            hCards[3] = oCards[3] = translateToEval.translate(f2);
            boardCardsForFlopIndexing[1] = flopTranslator.translate(f2);
            boardCardsForTurnIndexing[1] = turnTranslator.translate(f2);
            boardCardsForRiverIndexing[1] = riverTranslator.translate(f2);
            for (f3 = f2 + 1; f3 < 52; f3++) {
              if (((0x1l << f3) & deck) != 0l) {
                continue;
              }
              hCards[4] = oCards[4] = translateToEval.translate(f3);
              boardCardsForFlopIndexing[2] = flopTranslator.translate(f3);
              boardCardsForTurnIndexing[2] = turnTranslator.translate(f3);
              boardCardsForRiverIndexing[2] = riverTranslator.translate(f3);
              final int flopIndex = flopCardsIndexer.indexOf(hFlopCards);
              if (!flopHits[flopIndex]) {
                deck |= (0x1l << f1) | (0x1l << f2) | (0x1l << f3);
                flopHits[flopIndex] = true;
                for (t = 0; t < 52; t++) {
                  if (((0x1l << t) & deck) != 0l) {
                    continue;
                  }
                  hCards[5] = oCards[5] = translateToEval.translate(t);
                  boardCardsForTurnIndexing[3] = turnTranslator.translate(t);
                  boardCardsForRiverIndexing[3] = riverTranslator.translate(t);
                  final int turnIndex = turnCardsIndexer.indexOf(hTurnCards);
                  if (!turnHits[turnIndex]) {
                    deck |= (0x1l << t);
                    turnHits[turnIndex] = true;
                    long turnTotal = 0;
                    double turnEHS2 = 0, turnEHS = 0;

                    for (r = 0; r < 52; r++) {
                      if (((0x1l << r) & deck) != 0l) {
                        continue;
                      }
                      deck |= (0x1l << r);
                      hCards[6] = oCards[6] = translateToEval.translate(r);
                      boardCardsForRiverIndexing[4] = riverTranslator.translate(r);
                      final int riverIndex = riverCardsIndexer.indexOf(hRiverCards);
                      if (!riverHits[riverIndex]) {
                        riverHits[riverIndex] = true;

                        final int hVal = eval.get7CardsEval(hCards);
                        int rWin = 0, rLose = 0, rTie = 0;
                        for (o1 = 0; o1 < 51; o1++) {
                          if (((0x1l << o1) & deck) != 0l) {
                            continue;
                          }
                          deck |= (0x1l << o1);
                          oCards[0] = translateToEval.translate(o1);
                          for (o2 = o1 + 1; o2 < 52; o2++) {
                            if (((0x1l << o2) & deck) != 0l) {
                              continue;
                            }
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
                      if (((0x1l << o1) & deck) != 0l) {
                        continue;
                      }
                      deck |= (0x1l << o1);
                      oCards[0] = translateToEval.translate(o1);
                      for (o2 = o1 + 1; o2 < 52; o2++) {
                        if (((0x1l << o2) & deck) != 0l) {
                          continue;
                        }
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
                  if (((0x1l << o1) & deck) != 0l) {
                    continue;
                  }
                  deck |= (0x1l << o1);
                  oCards[0] = translateToEval.translate(o1);
                  for (o2 = o1 + 1; o2 < 52; o2++) {
                    if (((0x1l << o2) & deck) != 0l) {
                      continue;
                    }
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

  private void checkFilled() {
    checkState(isFilled, "AllHoldemHSTable must be filled first");
  }

  public double[] getTable(final Streets street, final HSType hsType) {
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
    throw new IllegalArgumentException(
        "Wrong street(" + street + ") / HSType (" + hsType + ") combination");
  }

  public static AllHoldemHSTables<WaughIndexer, WaughIndexer, WaughIndexer, WaughIndexer> getTablesWithWaughIndexersTwoPlusTwoEval() {
    return new AllHoldemHSTables<>(
        new TwoPlusTwoEvaluator(), new WaughIndexer(new int[] {2}),
        new WaughIndexer(new int[] {2, 3}), new WaughIndexer(new int[] {2, 4}),
        new WaughIndexer(new int[] {2, 5}));
  }

  public synchronized void writeTo(Path path) throws IOException {
    checkFilled();
    checkArgument(!Files.exists(path),
        "File " + path.toAbsolutePath().toString() + " already exists");
    checkArgument(Files.exists(path.getParent()) && Files.isDirectory(path.getParent()),
        "File " + path.toAbsolutePath().toString() + " parent folder doesn't exist");
    final Set<OpenOption> options = new HashSet<>();
    options.add(StandardOpenOption.WRITE);
    options.add(StandardOpenOption.READ);
    options.add(StandardOpenOption.CREATE);

    final File f = path.toFile();
    try (final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f))) {
      final ZipEntry e = new ZipEntry(fileName);
      out.putNextEntry(e);

      final ByteBuffer buffer = ByteBuffer.allocate(800_000);

      write(out, buffer, preflopEHSTable);
      write(out, buffer, preflopEHS2Table);
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

  public synchronized void readFrom(Path path) throws IOException {
    checkArgument(Files.exists(path),
        "File " + path.toAbsolutePath().toString() + " doesn't exists");
    checkArgument(!Files.isDirectory(path),
        "File " + path.toAbsolutePath().toString() + " is a folder");
    log.info("Reading holdem HS tabels from {}", path);
    final Set<OpenOption> options = new HashSet<>();
    options.add(StandardOpenOption.WRITE);
    options.add(StandardOpenOption.READ);
    options.add(StandardOpenOption.CREATE);

    try (final ZipFile zipFile = new ZipFile(path.toFile())) {
      final Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        final ZipEntry entry = entries.nextElement();
        if (!fileName.equals(entry.getName())) {
          continue;
        }
        final InputStream stream = zipFile.getInputStream(entry);

        final ByteBuffer buffer = ByteBuffer.allocate(800_000);
        log.info("Reading preflop EHS");
        read(stream, buffer, preflopEHSTable);
        log.info("Reading preflop EHS2");
        read(stream, buffer, preflopEHS2Table);
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

  private static void read(InputStream stream, ByteBuffer buffer, double[] dest)
      throws IOException {
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
        readForThisBuffferLoop += lastRead =
            stream.read(buffer.array(), readForThisBuffferLoop, toRead - readForThisBuffferLoop);

        if (lastRead < 0) {
          throw new IOException("File is too short, it may be corrupted");
        }
      }
      buffer.clear();
      buffer.asDoubleBuffer().get(dest, read / 8, toRead / 8);
      read += toRead;
    }

  }
}
