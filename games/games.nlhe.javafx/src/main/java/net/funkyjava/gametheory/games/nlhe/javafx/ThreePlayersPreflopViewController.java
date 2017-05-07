package net.funkyjava.gametheory.games.nlhe.javafx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import lombok.Getter;

public class ThreePlayersPreflopViewController {

  @FXML
  @Getter
  private ThreePlayersPreflopInitStateViewController initStateViewController;
  @FXML
  @Getter
  private MoveSequenceSelectorViewController sequenceSelectorViewController;
  @FXML
  @Getter
  private PreflopGridController preflopGridPaneController;
  @FXML
  @Getter
  private Button computeMore;
  @FXML
  @Getter
  private NumericTextField computeTime;
}
