package net.funkyjava.gametheory.extensiveformgame;

public class ActionChancesData<NodeData> {

	public static interface DataProvider<NodeData> {
		NodeData getData(final Game game, final ActionNode node, final int chance);
	}

	private ActionChancesData() {

	}

	/**
	 * 
	 * Creates data in an array indexed successively by the round index, the
	 * player index, the chance index, and the player's round's node index
	 * 
	 * @param actionTree
	 *            the action tree
	 * @param game
	 *            the game
	 * @param provider
	 *            the data provider
	 * @return the four dimensions array
	 */
	@SuppressWarnings("unchecked")
	public static <NodeData> NodeData[][][][] createRoundPlayerChanceNodeData(final GameActionTree actionTree,
			final Game game, final DataProvider<NodeData> provider) {
		final int[][] chancesSize = game.roundChancesSizes();
		final int nbRounds = chancesSize.length;
		final int nbPlayers = game.getNbPlayers();
		final NodeData[][][][] data = (NodeData[][][][]) new Object[nbRounds][][][];
		for (int round = 0; round < nbRounds; round++) {
			final int[] roundChancesSize = chancesSize[round];
			final NodeData[][][] roundData = data[round] = (NodeData[][][]) new Object[nbPlayers][][];
			for (int player = 0; player < nbPlayers; player++) {
				final int nbChances = roundChancesSize[player];
				final ActionNode[] actionNodes = actionTree.actionNodes[round][player];
				final int nbNodes = actionNodes.length;
				final NodeData[][] playerData = roundData[player] = (NodeData[][]) new Object[nbChances][nbNodes];
				for (int chance = 0; chance < nbChances; chance++) {
					final NodeData[] chanceData = playerData[chance];
					for (int nodeIndex = 0; nodeIndex < nbNodes; nodeIndex++) {
						chanceData[nodeIndex] = provider.getData(game, actionNodes[nodeIndex], chance);
					}
				}
			}
		}
		return data;
	}

	/**
	 * 
	 * Creates data in an array indexed successively by the round index, the
	 * player index, the player's round's node index and the chance index
	 * 
	 * @param actionTree
	 *            the action tree
	 * @param game
	 *            the game
	 * @param provider
	 *            the data provider
	 * @return the four dimensions array
	 */
	@SuppressWarnings("unchecked")
	public static <NodeData> NodeData[][][][] createRoundPlayerNodeChanceData(final GameActionTree actionTree,
			final Game game, final DataProvider<NodeData> provider) {
		final int[][] chancesSize = game.roundChancesSizes();
		final int nbRounds = chancesSize.length;
		final int nbPlayers = game.getNbPlayers();
		final NodeData[][][][] data = (NodeData[][][][]) new Object[nbRounds][][][];
		for (int round = 0; round < nbRounds; round++) {
			final int[] roundChancesSize = chancesSize[round];
			final NodeData[][][] roundData = data[round] = (NodeData[][][]) new Object[nbPlayers][][];
			for (int player = 0; player < nbPlayers; player++) {
				final int nbChances = roundChancesSize[player];
				final ActionNode[] actionNodes = actionTree.actionNodes[round][player];
				final int nbNodes = actionNodes.length;
				final NodeData[][] playerData = roundData[player] = (NodeData[][]) new Object[nbNodes][nbChances];
				for (int nodeIndex = 0; nodeIndex < nbNodes; nodeIndex++) {
					final NodeData[] nodeData = playerData[nodeIndex];
					final ActionNode node = actionNodes[nodeIndex];
					for (int chance = 0; chance < nbChances; chance++) {
						nodeData[nodeIndex] = provider.getData(game, node, chance);
					}
				}
			}
		}
		return data;
	}

}
