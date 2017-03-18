package net.funkyjava.gametheory.cscfrm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.util.concurrent.AtomicDoubleArray;

import net.funkyjava.gametheory.extensiveformgame.ActionChancesData;
import net.funkyjava.gametheory.extensiveformgame.ActionNode;
import net.funkyjava.gametheory.extensiveformgame.Game;
import net.funkyjava.gametheory.extensiveformgame.GameActionTree;
import net.funkyjava.gametheory.io.Fillable;
import net.funkyjava.gametheory.io.IOUtils;

public class CSCFRMData implements Fillable {

	public final int maxDepth;
	public final int maxNbActions;
	public final ActionNode rootNode;
	public final AtomicLong iterations = new AtomicLong();
	public final AtomicDoubleArray utilitySum;
	public final CSCFRMNode[][][][] nodes;
	public final int[][] roundChancesSizes;
	public final int nbPlayers;

	public CSCFRMData(final Game game) {
		this.nbPlayers = game.getNbPlayers();
		this.roundChancesSizes = game.roundChancesSizes();
		final GameActionTree actionTree = new GameActionTree(game);
		this.maxDepth = actionTree.maxDepth;
		this.maxNbActions = actionTree.maxNbActions;
		this.rootNode = actionTree.root;
		this.nodes = ActionChancesData.createRoundPlayerChanceNodeData(actionTree, game, new CSCFRMNodeProvider());
		final int nbPlayers = game.getNbPlayers();
		this.utilitySum = new AtomicDoubleArray(nbPlayers);
	}

	@Override
	public void fill(InputStream is) throws IOException {
		final DataInputStream dis = new DataInputStream(is);
		iterations.set(dis.readLong());
		final int nbPlayers = this.nbPlayers;
		final AtomicDoubleArray utilitySum = this.utilitySum;
		for (int i = 0; i < nbPlayers; i++) {
			utilitySum.set(i, dis.readDouble());
		}
		IOUtils.fill(is, nodes);

	}

	@Override
	public void write(OutputStream os) throws IOException {
		final DataOutputStream dos = new DataOutputStream(os);
		dos.writeLong(iterations.longValue());
		final AtomicDoubleArray utilitySum = this.utilitySum;
		for (int i = 0; i < nbPlayers; i++) {
			dos.writeDouble(utilitySum.get(i));
		}
		IOUtils.write(os, nodes);
	}
}
