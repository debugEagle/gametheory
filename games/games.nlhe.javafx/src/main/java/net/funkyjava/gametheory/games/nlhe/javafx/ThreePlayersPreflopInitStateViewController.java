package net.funkyjava.gametheory.games.nlhe.javafx;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.lang3.mutable.MutableInt;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

public class ThreePlayersPreflopInitStateViewController implements Initializable {

  @AllArgsConstructor
  @Data
  @EqualsAndHashCode
  public static class State {
    private final int sbVal, bbVal, initStack, btStack, sbStack, bbStack;

    public boolean isValid() {
      if (sbVal < 0 || bbVal <= 0 || bbVal < sbVal || initStack < 3 || btStack < 0 || sbStack <= 0
          || bbStack <= 0) {
        return false;
      }
      if (btStack + sbStack + bbStack != initStack * 3) {
        return false;
      }
      return true;
    }
  }

  public static interface StateChangeListener {
    void handleChange(final State state);
  }

  @FXML
  private NumericTextField txtSBVal;
  @FXML
  private NumericTextField txtBBVal;
  @FXML
  private NumericTextField txtInitStack;
  @FXML
  private NumericTextField txtBTStack;
  @FXML
  private NumericTextField txtSBStack;
  @FXML
  private NumericTextField txtBBStack;

  private final List<StateChangeListener> listeners = new LinkedList<>();

  private final LinkedList<NumericTextField> stacksFields = new LinkedList<>();
  private final LinkedList<NumericTextField> lastStackFieldsChanged = new LinkedList<>();

  private State lastState;
  private boolean stackIsChanging = false;

  @Override
  public void initialize(URL location, ResourceBundle resources) {
    stacksFields.add(txtBTStack);
    stacksFields.add(txtSBStack);
    stacksFields.add(txtBBStack);
    txtBTStack.textProperty().addListener((event) -> {
      stackFieldChanged(txtBTStack);
    });
    txtBTStack.focusedProperty().addListener((event, old, newVal) -> {
      if (!old && newVal) {
        Platform.runLater(() -> txtBTStack.selectAll());
      }
    });
    txtSBStack.textProperty().addListener((event) -> {
      stackFieldChanged(txtSBStack);
    });
    txtSBStack.focusedProperty().addListener((event, old, newVal) -> {
      if (!old && newVal) {
        Platform.runLater(() -> txtSBStack.selectAll());
      }
    });
    txtBBStack.textProperty().addListener((event) -> {
      stackFieldChanged(txtBBStack);
    });
    txtBBStack.focusedProperty().addListener((event, old, newVal) -> {
      if (!old && newVal) {
        Platform.runLater(() -> txtBBStack.selectAll());
      }
    });
    txtInitStack.textProperty().addListener((event) -> {
      initStackChanged();
    });
    txtInitStack.focusedProperty().addListener((event, old, newVal) -> {
      if (!old && newVal) {
        Platform.runLater(() -> txtInitStack.selectAll());
      }
    });
    txtSBVal.textProperty().addListener((event) -> {
      stateChanged();
    });
    txtSBVal.focusedProperty().addListener((event, old, newVal) -> {
      if (!old && newVal) {
        Platform.runLater(() -> txtSBVal.selectAll());
      }
    });
    txtBBVal.textProperty().addListener((event) -> {
      stateChanged();
    });
    txtBBVal.focusedProperty().addListener((event, old, newVal) -> {
      if (!old && newVal) {
        Platform.runLater(() -> txtBBVal.selectAll());
      }
    });
    lastState = getState();
  }

  public void addListener(final StateChangeListener listener) {
    listeners.add(listener);
  }

  public void removeListener(final StateChangeListener listener) {
    listeners.remove(listener);
  }

  public void focus() {
    txtBTStack.requestFocus();
  }

  public State getState() {
    int sbVal = 0;
    int bbVal = 0;
    int btStack = 0;
    int sbStack = 0;
    int bbStack = 0;
    int initStack = 0;

    try {
      sbVal = Integer.parseInt(this.txtSBVal.getText());
    } catch (Exception e) {
    }
    try {
      bbVal = Integer.parseInt(this.txtBBVal.getText());
    } catch (Exception e) {
    }
    try {
      btStack = Integer.parseInt(this.txtBTStack.getText());
    } catch (Exception e) {
    }
    try {
      sbStack = Integer.parseInt(this.txtSBStack.getText());
    } catch (Exception e) {
    }
    try {
      bbStack = Integer.parseInt(this.txtBBStack.getText());
    } catch (Exception e) {
    }
    try {
      initStack = Integer.parseInt(this.txtInitStack.getText());
    } catch (Exception e) {
    }
    return new State(sbVal, bbVal, initStack, btStack, sbStack, bbStack);
  }

  private void stackFieldChanged(final NumericTextField stackField) {
    if (stackIsChanging) {
      return;
    }
    lastStackFieldsChanged.remove(stackField);
    lastStackFieldsChanged.add(stackField);
    if (lastStackFieldsChanged.size() > 2) {
      lastStackFieldsChanged.remove(0);
    }
    try {
      final int initStack = Integer.parseInt(txtInitStack.getText());
      final int stacksSum = initStack * 3;
      final NumericTextField toAdjust =
          stacksFields.stream().filter(field -> !lastStackFieldsChanged.contains(field))
              .collect(Collectors.toList()).get(0);
      final MutableInt totalStack = new MutableInt();
      stacksFields.stream().filter(field -> field != toAdjust).collect(Collectors.toList())
          .forEach(field -> {
            int val = 0;
            try {
              val = Math.max(0, Integer.parseInt(field.getText()));
            } catch (Exception e) {

            }
            totalStack.add(val);
          });

      if (totalStack.getValue() < stacksSum) {
        final String text = (stacksSum - totalStack.getValue()) + "";
        if (!text.equals(toAdjust.getText())) {
          stackIsChanging = true;
          toAdjust.setText(text);
          stackIsChanging = false;
        }
      }
    } catch (Exception e) {
    }
    stateChanged();
  }

  private void initStackChanged() {
    final String stack = txtInitStack.getText();
    txtBTStack.setText(stack);
    txtSBStack.setText(stack);
    txtBBStack.setText(stack);
    stateChanged();
  }

  private void stateChanged() {
    final State state = getState();
    if (lastState != null && lastState.equals(state)) {
      return;
    }
    lastState = state;
    listeners.forEach(listener -> listener.handleChange(state));
  }
}
