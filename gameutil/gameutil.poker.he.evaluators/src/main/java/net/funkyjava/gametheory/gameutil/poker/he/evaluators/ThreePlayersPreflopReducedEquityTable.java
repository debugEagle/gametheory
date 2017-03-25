package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
	@Getter
	private final int nbHoleCards = holeCardsIndexer.getIndexSize();

	@Getter
	private final double[][][][][] reducedEquities;
	@Getter
	private final int[][][] counts;

	public ThreePlayersPreflopReducedEquityTable() {
		reducedEquities = new double[nbHoleCards][nbHoleCards][nbHoleCards][4][3];
		counts = new int[nbHoleCards][nbHoleCards][nbHoleCards];
	}

	public ThreePlayersPreflopReducedEquityTable(final ThreePlayersPreflopEquityTables tables) {
		this.reducedEquities = tables.getReducedEquities();
		this.counts = tables.getReducedCounts();
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
		checkArgument(args.length == 2, "3 Players Preflop Tables writing misses a path argument");
		final String srcPathStr = args[0];
		final Path srcPath = Paths.get(srcPathStr);
		checkArgument(Files.exists(srcPath), "File " + srcPath.toAbsolutePath().toString() + " doesnt exists");
		final String destPathStr = args[1];
		final Path destPath = Paths.get(destPathStr);
		checkArgument(!Files.exists(destPath), "File " + destPath.toAbsolutePath().toString() + " already exists");
		try (final FileInputStream fis = new FileInputStream(srcPath.toFile());
				final ObjectInputStream ois = new ObjectInputStream(fis);
				final FileOutputStream fos = new FileOutputStream(destPath.toFile())) {
			log.info("Reading...");
			final ThreePlayersPreflopEquityTables srcTables = (ThreePlayersPreflopEquityTables) ois.readObject();
			final ThreePlayersPreflopReducedEquityTable destTables = new ThreePlayersPreflopReducedEquityTable(
					srcTables);
			log.info("Writing...");
			destTables.write(fos);
			log.info("Done");
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
