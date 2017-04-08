package net.funkyjava.gametheory.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Fillable {

  public void fill(InputStream is) throws IOException;

  public void write(OutputStream os) throws IOException;
}
