package util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

public final class StdoutTee {
    private static volatile Consumer<String> lineConsumer;

    private StdoutTee() {
    }

    public static void setLineConsumer(Consumer<String> consumer) {
        lineConsumer = consumer;
    }

    public static void install() {
        PrintStream original = System.out;
        OutputStream forward = new OutputStream() {
            private final StringBuilder buf = new StringBuilder(256);

            @Override
            public void write(int b) throws IOException {
                original.write(b);
                if (b == '\n') {
                    emit();
                } else if (b != '\r') {
                    buf.append((char) b);
                }
            }

            private void emit() {
                String s = buf.toString();
                buf.setLength(0);
                Consumer<String> c = lineConsumer;
                if (c != null && !s.isEmpty()) {
                    c.accept(s);
                }
            }
        };
        try {
            System.setOut(new PrintStream(forward, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            System.setOut(new PrintStream(forward, true));
        }
    }
}
