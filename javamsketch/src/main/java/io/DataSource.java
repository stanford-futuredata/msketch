package io;

import java.io.IOException;

public interface DataSource {
    double[] get() throws IOException;
}