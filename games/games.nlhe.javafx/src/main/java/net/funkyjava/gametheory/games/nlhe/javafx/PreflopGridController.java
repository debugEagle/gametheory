package net.funkyjava.gametheory.games.nlhe.javafx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;

public class PreflopGridController {

  @FXML
  private GridPane gridPane;

  private static final String greenClass = "green-strat";
  private static final String yellowClass = "yellow-strat";
  private static final String orangeClass = "orange-strat";
  private static final String redClass = "red-strat";

  public void updateDisplayWithStrategy(final double[][] strategy, final String[][] handsStrs) {
    Platform.runLater(() -> {
      gridPane.getChildren().clear();
      for (int i = 0; i < 13; i++) {
        for (int j = 0; j < 13; j++) {
          final Node node = getStratNode(strategy[i][j], handsStrs[i][j]);
          GridPane.setColumnIndex(node, j);
          GridPane.setRowIndex(node, i);
          GridPane.setFillWidth(node, true);
          GridPane.setFillHeight(node, true);
          node.prefWidth(Double.POSITIVE_INFINITY);
          node.prefHeight(Double.POSITIVE_INFINITY);
          node.maxWidth(Double.POSITIVE_INFINITY);
          node.maxHeight(Double.POSITIVE_INFINITY);
          GridPane.setHalignment(node, HPos.CENTER);
          GridPane.setValignment(node, VPos.CENTER);
          GridPane.setVgrow(node, Priority.ALWAYS);
          gridPane.getChildren().add(node);
        }
      }
    });
  }

  public void clear() {
    Platform.runLater(() -> {
      gridPane.getChildren().clear();
    });
  }

  private static Node getStratNode(final double strat, final String hand) {
    final Label label = new Label(hand);
    final String style = getStyleClass(strat);
    final StackPane pane = new StackPane(label);
    pane.getStyleClass().add(style);
    pane.prefHeight(Double.POSITIVE_INFINITY);
    pane.prefWidth(Double.POSITIVE_INFINITY);
    pane.maxHeight(Double.POSITIVE_INFINITY);
    pane.maxWidth(Double.POSITIVE_INFINITY);
    return pane;
  }

  private static String getStyleClass(final double strat) {
    if (strat < 0.25) {
      return redClass;
    }
    if (strat < 0.5) {
      return orangeClass;
    }
    if (strat < 0.75) {
      return yellowClass;
    }
    return greenClass;
  }

}
