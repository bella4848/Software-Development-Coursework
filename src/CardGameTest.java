import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CardGameTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    @After
    public void cleanOutputFiles() {
        TestSupport.deleteOutputFiles();
    }


    private String packFile(String... lines) throws Exception {
        File f = tmp.newFile();
        Files.write(f.toPath(), Arrays.asList(lines));
        return f.getAbsolutePath();
    }

    private static String[] repeat(String value, int times) {
        String[] out = new String[times];
        Arrays.fill(out, value);
        return out;
    }

    private void assertInvalidPack(int n, String path, String why) {
        try {
            CardGame.readPack(path, n);
            fail("pack should have been rejected: " + why);
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void aValidPackIsReadInFileOrder() throws Exception {
        String path = packFile("5", "0", "3", "3", "1", "99", "2", "7");
        assertEquals(Arrays.asList(5, 0, 3, 3, 1, 99, 2, 7), TestSupport.values(CardGame.readPack(path, 1)));
    }

    @Test
    public void aMissingFileIsRejected() {
        assertInvalidPack(1, new File(tmp.getRoot(), "nope.txt").getAbsolutePath(), "no such file");
    }

    @Test
    public void aPackWithTheWrongNumberOfLinesIsRejected() throws Exception {
        assertInvalidPack(2, packFile(repeat("1", 15)), "15 lines for n=2");
        assertInvalidPack(2, packFile(repeat("1", 17)), "17 lines for n=2");
        assertInvalidPack(1, packFile(), "empty file");
    }

    @Test
    public void aPackWithABadValueIsRejected() throws Exception {
        String[] negative = repeat("1", 8);
        negative[3] = "-4";
        assertInvalidPack(1, packFile(negative), "negative value");

        String[] text = repeat("1", 8);
        text[7] = "abc";
        assertInvalidPack(1, packFile(text), "non-numeric value");

        String[] decimal = repeat("1", 8);
        decimal[0] = "2.5";
        assertInvalidPack(1, packFile(decimal), "decimal value");

        String[] blank = repeat("1", 8);
        blank[4] = "";
        assertInvalidPack(1, packFile(blank), "blank line");

        String[] tooBig = repeat("1", 8);
        tooBig[2] = "99999999999999999999";
        assertInvalidPack(1, packFile(tooBig), "value too large for an int");
    }

    private static List<Card> immediateWinPack() {
        return TestSupport.cards(
                1, 2, 3,
                1, 3, 2,
                1, 2, 3,
                1, 3, 2,
                10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
    }

    @Test(timeout = 30000)
    public void handsAreDealtRoundRobinAndDecksFilledRoundRobinFromTheRest() throws Exception {
        TestSupport.captureStdout(() -> CardGame.play(3, immediateWinPack()));

        assertEquals("player 2 initial hand 2 3 2 3", TestSupport.lines("player2_output.txt").get(0));
        assertEquals("player 3 initial hand 3 2 3 2", TestSupport.lines("player3_output.txt").get(0));
        assertEquals("deck1 contents: 10 13 16 19", TestSupport.lines("deck1_output.txt").get(0));
        assertEquals("deck2 contents: 11 14 17 20", TestSupport.lines("deck2_output.txt").get(0));
        assertEquals("deck3 contents: 12 15 18 21", TestSupport.lines("deck3_output.txt").get(0));
    }


    @Test(timeout = 30000)
    public void anImmediateWinIsAnnouncedOnceAndEveryPlayerStillWritesFourLines() throws Exception {
        final int[] winner = new int[1];
        String printed = TestSupport.captureStdout(() -> winner[0] = CardGame.play(3, immediateWinPack()));

        assertEquals(1, winner[0]);
        assertEquals("player 1 wins" + System.lineSeparator(), printed);

        assertEquals(Arrays.asList(
                "player 1 initial hand 1 1 1 1",
                "player 1 wins",
                "player 1 exits",
                "player 1 final hand: 1 1 1 1"), TestSupport.lines("player1_output.txt"));
        assertEquals(Arrays.asList(
                "player 2 initial hand 2 3 2 3",
                "player 1 has informed player 2 that player 1 has won",
                "player 2 exits",
                "player 2 hand: 2 3 2 3"), TestSupport.lines("player2_output.txt"));
        assertEquals(4, TestSupport.lines("player3_output.txt").size());
    }

    private void playAndVerify(final int n, long seed) throws Exception {
        TestSupport.deleteOutputFiles();
        final List<Card> pack = TestSupport.winnablePack(n, seed);
        final int[] winnerBox = new int[1];

        String printed = TestSupport.captureStdout(() -> winnerBox[0] = CardGame.play(n, pack));
        String context = " (n=" + n + ", seed=" + seed + ")";
        int winner = winnerBox[0];

        assertTrue("winner out of range" + context, winner >= 1 && winner <= n);
        assertEquals("terminal output" + context, "player " + winner + " wins" + System.lineSeparator(), printed);

        for (int i = 1; i <= n; i++) {
            assertTrue(new File("player" + i + "_output.txt").exists());
            assertTrue(new File("deck" + i + "_output.txt").exists());
        }
        assertFalse(new File("player" + (n + 1) + "_output.txt").exists());
        assertFalse(new File("deck" + (n + 1) + "_output.txt").exists());

        List<Integer> everything = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            List<String> lines = TestSupport.lines("player" + i + "_output.txt");
            assertTrue(lines.get(0).startsWith("player " + i + " initial hand "));
            assertEquals("player " + i + " exits", lines.get(lines.size() - 2));

            String last = lines.get(lines.size() - 1);
            List<Integer> finalHand = TestSupport.numbersAfter(last, ":");
            assertEquals("each player ends with four cards" + context, 4, finalHand.size());
            everything.addAll(finalHand);

            if (i == winner) {
                assertEquals("player " + i + " wins", lines.get(lines.size() - 3));
                assertEquals("player " + i + " final hand: " + finalHand.toString().replaceAll("[\\[\\],]", ""), last);
                assertEquals("the winner holds four equal cards" + context, 1, new java.util.HashSet<>(finalHand).size());
            } else {
                assertEquals("player " + winner + " has informed player " + i + " that player " + winner + " has won",
                        lines.get(lines.size() - 3));
            }

            List<String> actions = lines.subList(1, lines.size() - 3);
            assertEquals(0, actions.size() % 3);
            for (int a = 0; a < actions.size(); a += 3) {
                String[] draw = actions.get(a).split(" ");
                String[] discard = actions.get(a + 1).split(" ");
                assertEquals("player " + i + " draws a " + draw[4] + " from deck " + i, actions.get(a));
                assertTrue("player must discard a non-preferred card: " + actions.get(a + 1),
                        Integer.parseInt(discard[4]) != i);
                assertEquals("player " + i + " discards a " + discard[4] + " to deck " + (i % n + 1), actions.get(a + 1));
                assertEquals("hand must have four cards after every action: " + actions.get(a + 2),
                        4, TestSupport.numbersAfter(actions.get(a + 2), " is ").size());
            }
        }

        for (int i = 1; i <= n; i++) {
            List<String> lines = TestSupport.lines("deck" + i + "_output.txt");
            assertEquals(1, lines.size());
            assertTrue(lines.get(0).startsWith("deck" + i + " contents:"));
            everything.addAll(TestSupport.numbersAfter(lines.get(0), "contents:"));
        }

        List<Integer> expected = TestSupport.values(pack);
        Collections.sort(expected);
        Collections.sort(everything);
        assertEquals("cards were lost or duplicated" + context, expected, everything);

        for (Thread t : Thread.getAllStackTraces().keySet()) {
            assertFalse("thread still alive: " + t.getName() + context,
                    t.getName().startsWith("player-") && t.isAlive());
        }
    }

    @Test(timeout = 120000)
    public void manyShuffledGamesFinishWithAllInvariantsIntact() throws Exception {
        for (long seed = 0; seed < 25; seed++) {
            playAndVerify(4, seed);
        }
        for (long seed = 0; seed < 10; seed++) {
            playAndVerify(2, seed);
            playAndVerify(3, seed);
            playAndVerify(5, seed);
        }
        for (long seed = 0; seed < 4; seed++) {
            playAndVerify(8, seed);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void aPackOfTheWrongSizeIsRefusedByPlay() throws Exception {
        CardGame.play(2, TestSupport.cards(1, 2, 3));
    }

    private String runMainWithInput(String input) throws Exception {
        InputStream original = System.in;
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        try {
            return TestSupport.captureStdout(() -> CardGame.main(new String[0]));
        } finally {
            System.setIn(original);
        }
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + 1)) {
            count++;
        }
        return count;
    }

    @Test(timeout = 60000)
    public void invalidInputIsRepromptedUntilValidThenTheGameRuns() throws Exception {
        String good = new File(tmp.newFile().getAbsolutePath()).getAbsolutePath();
        List<String> goodLines = new ArrayList<>();
        for (Card c : TestSupport.winnablePack(2, 1)) {
            goodLines.add(String.valueOf(c.getValue()));
        }
        Files.write(new File(good).toPath(), goodLines);
        String shortPack = packFile(repeat("1", 15));

        String printed = runMainWithInput("0\nabc\n-1\n2\nmissing.txt\n" + shortPack + "\n" + good + "\n");

        assertEquals("three bad player counts", 3, count(printed, "Invalid input"));
        assertEquals("two bad packs", 2, count(printed, "Invalid pack"));
        assertTrue("the game should then run to a winner: " + printed, printed.matches("(?s).*player [12] wins.*"));
        assertTrue(new File("player1_output.txt").exists());
        assertTrue(new File("deck2_output.txt").exists());
    }

    @Test(timeout = 30000)
    public void noGameStartsIfTheInputEndsBeforeAValidPack() throws Exception {
        String printed = runMainWithInput("2\n");
        assertFalse(printed.contains("wins"));
        assertFalse(new File("player1_output.txt").exists());
    }
}