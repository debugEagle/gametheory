package net.funkyjava.gametheory.games.nlhe.javafx;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;

public class MoveSequenceSelectorViewController implements Initializable {

  public static interface MoveSequenceSelectionListener {
    void selectionChanged(final int index, final String sequenceString);
  }

  @FXML
  private ListView<String> list;
  private List<MoveSequenceSelectionListener> listeners = new LinkedList<>();


  public void addListener(final MoveSequenceSelectionListener listener) {
    listeners.add(listener);
  }

  public void setMoveSequencesStrings(List<String> sequencesStrings) {
    list.setItems(FXCollections.observableArrayList(sequencesStrings));
    if (!sequencesStrings.isEmpty()) {
      list.selectionModelProperty().get().clearAndSelect(0);
    }
  }

  public void clearSequences() {
    list.setItems(FXCollections.observableArrayList());
    list.selectionModelProperty().get().clearSelection();
  }

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    list.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    list.getSelectionModel().selectedIndexProperty().addListener((event, old, newIndex) -> {
      if (newIndex == null || newIndex.intValue() < 0) {
        return;
      }
      selectionChanged(newIndex.intValue());
    });
  }

  private void selectionChanged(final int index) {
    final String sequence = list.getItems().get(index);
    listeners.forEach(listener -> {
      listener.selectionChanged(index, sequence);
    });
  }


}
