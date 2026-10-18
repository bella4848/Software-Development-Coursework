import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

final class TestSupport {

    interface Action {
        void run() throws Exception;
    }

    private TestSupport() { }

    static void deleteOutputFiles() {
        for (int i = 1; i <= 10; i++) {
            new File("player" + i + "_output.txt").delete();
            new File("deck" + i + "_output.txt").delete();
        }
    }

    static List<String> lines(String fileName) throws IOException {
        return Files.readAllLines(Paths.get(fileName));
    }

    static List<Card> cards(int... values) {
        List<Card> list = new ArrayList<>();
        for (int v : values) {
            list.add(new Card(v));
        }
        return list;
    }

    static List<Integer> values(List<Card> cards) {
        List<Integer> list = new ArrayList<>();
        for (Card c : cards) {
            list.add(c.getValue());
        }
        return list;
    }


    static List<Card> winnablePack(int n, long seed) {
        List<Integer> vals = new ArrayList<>();
        for (int v = 1; v <= n; v++) {
            for (int k = 0; k < 8; k++) {
                vals.add(v);
            }
        }
        Collections.shuffle(vals, new Random(seed));
        List<Card> pack = new ArrayList<>();
        for (int v : vals) {
            pack.add(new Card(v));
        }
        return pack;
    }

    static List<Integer> numbersAfter(String line, String marker) {
        int at = line.indexOf(marker);
        if (at < 0) {
            throw new AssertionError("expected '" + marker + "' in line: " + line);
        }
        String rest = line.substring(at + marker.length()).trim();
        List<Integer> out = new ArrayList<>();
        if (!rest.isEmpty()) {
            for (String token : rest.split("\\s+")) {
                out.add(Integer.parseInt(token));
            }
        }
        return out;
    }

    static String captureStdout(Action action) throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer, true));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }
}