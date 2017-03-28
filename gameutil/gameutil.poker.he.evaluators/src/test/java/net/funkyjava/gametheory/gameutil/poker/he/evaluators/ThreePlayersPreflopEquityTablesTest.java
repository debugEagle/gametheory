package net.funkyjava.gametheory.gameutil.poker.he.evaluators;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class ThreePlayersPreflopEquityTablesTest {

	static final Path path = Paths.get("/Users/pitt/HE_3PLAYERS_EQUITY.dat");

	private static final boolean testWrite = false;
	private static final boolean testRead = false;

	@Test
	public void testWriteRead() throws IOException, InterruptedException, ClassNotFoundException {
		if (testWrite) {
			try (final FileOutputStream fos = new FileOutputStream(path.toFile())) {
				final ThreePlayersPreflopEquityTables tables = new ThreePlayersPreflopEquityTables();
				tables.compute();
				tables.write(fos);
			}
		}
		if (testRead) {
			try (final FileInputStream fis = new FileInputStream(path.toFile())) {
				final ThreePlayersPreflopEquityTables tables = new ThreePlayersPreflopEquityTables();
				tables.fill(fis);
			}
		}
	}

}
