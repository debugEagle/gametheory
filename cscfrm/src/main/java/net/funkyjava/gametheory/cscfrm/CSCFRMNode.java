package net.funkyjava.gametheory.cscfrm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.funkyjava.gametheory.io.Fillable;

public class CSCFRMNode implements Fillable {

  public final double[] regretSum;
  public final double[] strategySum;

  public CSCFRMNode(final int nbActions) {
    regretSum = new double[nbActions];
    strategySum = new double[nbActions];
  }

  public double[] getAvgStrategy() {
    final int nbActions = strategySum.length;
    final double[] strategySum = this.strategySum;
    double tot = 0;
    for (int i = 0; i < nbActions; i++)
      tot += strategySum[i];
    final double[] res = new double[nbActions];
    for (int i = 0; i < nbActions; i++)
      res[i] = strategySum[i] / tot;
    return res;
  }

  @Override
  public void fill(InputStream is) throws IOException {
    final double[] regretSum = this.regretSum;
    final double[] strategySum = this.strategySum;
    final int nbActions = strategySum.length;
    final DataInputStream dis = new DataInputStream(is);
    for (int i = 0; i < nbActions; i++) {
      regretSum[i] = dis.readDouble();
    }
    for (int i = 0; i < nbActions; i++) {
      strategySum[i] = dis.readDouble();
    }
  }

  @Override
  public void write(OutputStream os) throws IOException {
    final DataOutputStream dos = new DataOutputStream(os);
    final double[] regretSum = this.regretSum;
    final double[] strategySum = this.strategySum;
    final int nbActions = strategySum.length;
    for (int i = 0; i < nbActions; i++) {
      dos.writeDouble(regretSum[i]);
    }
    for (int i = 0; i < nbActions; i++) {
      dos.writeDouble(strategySum[i]);
    }
  }
}
