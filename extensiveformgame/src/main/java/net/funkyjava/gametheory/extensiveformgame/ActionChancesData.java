package net.funkyjava.gametheory.extensiveformgame;

import java.lang.reflect.Array;

public class ActionChancesData<NodeData> {

  public static interface DataProvider<NodeData, Id> {
    Class<NodeData> getDataClass();

    NodeData getData(final Game<Id, ?> game, final ActionNode<Id, ?> node, final int chance);
  }

  private ActionChancesData() {

  }

  /**
   *
   * Creates data in an array indexed successively by the round index, the player index, the chance
   * index, and the player's round's node index
   *
   * @param actionTree the action tree
   * @param game the game
   * @param provider the data provider
   * @return the four dimensions array
   */
  @SuppressWarnings("unchecked")
  public static <NodeData, Id> NodeData[][][][] createRoundPlayerChanceNodeData(
      final GameActionTree<Id, ?> actionTree, final Game<Id, ?> game,
      final DataProvider<NodeData, Id> provider) {
    final Class<NodeData> dataClass = provider.getDataClass();
    final Class<?> oneDimensionArrayClass = Array.newInstance(dataClass, 0).getClass();
    final Class<?> twoDimensionsArrayClass =
        Array.newInstance(oneDimensionArrayClass, 0).getClass();
    final Class<?> threeDimensionsArrayClass =
        Array.newInstance(twoDimensionsArrayClass, 0).getClass();
    final int[][] chancesSize = game.roundChancesSizes();
    final int nbRounds = chancesSize.length;
    final int nbPlayers = game.getNbPlayers();
    final NodeData[][][][] data =
        (NodeData[][][][]) Array.newInstance(threeDimensionsArrayClass, nbRounds);
    for (int round = 0; round < nbRounds; round++) {
      final int[] roundChancesSize = chancesSize[round];
      final NodeData[][][] roundData =
          data[round] = (NodeData[][][]) Array.newInstance(twoDimensionsArrayClass, nbPlayers);
      for (int player = 0; player < nbPlayers; player++) {
        final int nbChances = roundChancesSize[player];
        final ActionNode<Id, ?>[] actionNodes = actionTree.actionNodes[round][player];
        final int nbNodes = actionNodes.length;
        final NodeData[][] playerData =
            roundData[player] = (NodeData[][]) Array.newInstance(oneDimensionArrayClass, nbChances);
        for (int chance = 0; chance < nbChances; chance++) {
          final NodeData[] chanceData =
              playerData[chance] = (NodeData[]) Array.newInstance(dataClass, nbNodes);
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
   * Creates data in an array indexed successively by the round index, the player index, the
   * player's round's node index and the chance index
   *
   * @param actionTree the action tree
   * @param game the game
   * @param provider the data provider
   * @return the four dimensions array
   */
  @SuppressWarnings("unchecked")
  public static <NodeData, Id> NodeData[][][][] createRoundPlayerNodeChanceData(
      final GameActionTree<Id, ?> actionTree, final Game<Id, ?> game,
      final DataProvider<NodeData, Id> provider) {
    final Class<NodeData> dataClass = provider.getDataClass();
    final Class<?> oneDimensionArrayClass = Array.newInstance(dataClass, 0).getClass();
    final Class<?> twoDimensionsArrayClass =
        Array.newInstance(oneDimensionArrayClass, 0).getClass();
    final Class<?> threeDimensionsArrayClass =
        Array.newInstance(twoDimensionsArrayClass, 0).getClass();
    final int[][] chancesSize = game.roundChancesSizes();
    final int nbRounds = chancesSize.length;
    final int nbPlayers = game.getNbPlayers();
    final NodeData[][][][] data =
        (NodeData[][][][]) Array.newInstance(threeDimensionsArrayClass, nbRounds);
    for (int round = 0; round < nbRounds; round++) {
      final int[] roundChancesSize = chancesSize[round];
      final NodeData[][][] roundData =
          data[round] = (NodeData[][][]) Array.newInstance(twoDimensionsArrayClass, nbPlayers);
      for (int player = 0; player < nbPlayers; player++) {
        final int nbChances = roundChancesSize[player];
        final ActionNode<Id, ?>[] actionNodes = actionTree.actionNodes[round][player];
        final int nbNodes = actionNodes.length;
        final NodeData[][] playerData =
            roundData[player] = (NodeData[][]) Array.newInstance(oneDimensionArrayClass, nbNodes);
        for (int nodeIndex = 0; nodeIndex < nbNodes; nodeIndex++) {
          final NodeData[] nodeData =
              playerData[nodeIndex] = (NodeData[]) Array.newInstance(dataClass, nbChances);
          final ActionNode<Id, ?> node = actionNodes[nodeIndex];
          for (int chance = 0; chance < nbChances; chance++) {
            nodeData[nodeIndex] = provider.getData(game, node, chance);
          }
        }
      }
    }
    return data;
  }

}
