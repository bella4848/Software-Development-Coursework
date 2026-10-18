import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

public class PlayerTest {

    private Deck drawDeck;
    private Deck discardDeck;
    private StringWriter log;

    @Before
    public void setUp() {
        drawDeck = new Deck(1);
        discardDeck = new Deck(2);
    }

    private Player player(int index, int... hand) {
        Player p = new Player(index, drawDeck, discardDeck, new AtomicInteger(0));
        for (int v : hand) {
            p.addCard(new Card(v));
        }
        log = new StringWriter();
        p.setOutput(new PrintWriter(log, true));
        return p;
    }

    private static void fill(Deck deck, int... values) {
        for (int v : values) {
            deck.addCard(new Card(v));
        }
    }

    @Test
    public void aTurnMovesOneCardBetweenDecksAndKeepsTheHandAtFour() {
        Player p = player(1, 1, 1, 2, 3);
        fill(drawDeck, 4, 5);

        assertTrue(p.takeTurn());

        assertEquals(4, p.getHand().size());
        assertEquals(1, drawDeck.getSize());
        assertEquals(1, discardDeck.getSize());

        List<Integer> everything = new ArrayList<>(TestSupport.values(p.getHand()));
        everything.addAll(TestSupport.values(drawDeck.getContents()));
        everything.addAll(TestSupport.values(discardDeck.getContents()));
        Collections.sort(everything);
        assertEquals(Arrays.asList(1, 1, 2, 3, 4, 5), everything);
    }

    @Test
    public void drawsFromTheTopOfTheDrawDeckAndDiscardsToTheBottomOfTheDiscardDeck() {
        Player p = player(1, 1, 1, 2, 3);
        fill(drawDeck, 4, 5);
        fill(discardDeck, 9);

        p.takeTurn();

        assertEquals(Arrays.asList(5), TestSupport.values(drawDeck.getContents()));
        assertTrue(TestSupport.values(p.getHand()).contains(4));
        assertEquals(Arrays.asList(9, 2), TestSupport.values(discardDeck.getContents()));
    }

    @Test
    public void aTurnOnAnEmptyDrawDeckDoesNothingAndReportsFailure() {
        Player p = player(1, 1, 1, 2, 3);

        assertFalse(p.takeTurn());

        assertEquals(Arrays.asList(1, 1, 2, 3), TestSupport.values(p.getHand()));
        assertTrue(discardDeck.isEmpty());
        assertEquals("", log.toString());
    }

    @Test
    public void aTurnIsLoggedInTheRequiredFormat() {
        Player p = player(1, 1, 1, 2, 3);
        fill(drawDeck, 4);

        p.takeTurn();

        assertEquals("player 1 draws a 4 from deck 1" + System.lineSeparator()
                + "player 1 discards a 2 to deck 2" + System.lineSeparator()
                + "player 1 current hand is 1 1 3 4" + System.lineSeparator(), log.toString());
    }

    @Test
    public void theDiscardIsTheOldestNonPreferredCard_soNoCardIsHeldForever() {
        Player p = player(1, 2, 3, 1, 1);
        fill(drawDeck, 5, 7);

        p.takeTurn();
        p.takeTurn();

        assertEquals(Arrays.asList(2, 3), TestSupport.values(discardDeck.getContents()));
        assertEquals(Arrays.asList(1, 1, 5, 7), TestSupport.values(p.getHand()));
    }

    @Test
    public void aPreferredCardIsNeverDiscardedOverManyTurns() {
        Deck ring = new Deck(1);
        fill(ring, 1, 2, 3, 1, 4, 5);
        Player p = new Player(1, ring, ring, new AtomicInteger(0));
        for (int v : new int[] {1, 2, 3, 4}) {
            p.addCard(new Card(v));
        }
        log = new StringWriter();
        p.setOutput(new PrintWriter(log, true));

        for (int turn = 0; turn < 300; turn++) {
            assertTrue(p.takeTurn());
            assertEquals("hand must be four cards after every completed turn", 4, p.getHand().size());
        }

        int discards = 0;
        for (String line : log.toString().split("\\R")) {
            if (line.contains(" discards a ")) {
                discards++;
                int value = Integer.parseInt(line.split(" ")[4]);
                assertTrue("discarded the preferred value: " + line, value != 1);
            }
        }
        assertEquals(300, discards);
    }

    @Test
    public void fourEqualCardsWin_evenIfTheyAreNotThePreferredValue() {
        assertTrue(player(1, 3, 3, 3, 3).checkHand());
        assertTrue(player(1, 1, 1, 1, 1).checkHand());
    }

    @Test
    public void aMixedHandOrAShortHandDoesNotWin() {
        assertFalse(player(1, 1, 1, 1, 2).checkHand());
        assertFalse(player(1, 1, 1, 1).checkHand());
        assertFalse(player(1).checkHand());
    }

    @Test(expected = IllegalStateException.class)
    public void aHandCanNeverBeDealtMoreThanFourCards() {
        player(1, 1, 2, 3, 4, 5);
    }


    @Test(timeout = 60000)
    public void concurrentTurnsAroundTheRing_doNotDeadlockOrLoseCards() throws Exception {
        final int n = 4;
        List<Integer> all = new ArrayList<>();
        for (int v = 1; v <= 2 * n; v++) {
            for (int k = 0; k < 4; k++) {
                all.add(v);
            }
        }
        Collections.shuffle(all, new Random(7));

        List<Deck> decks = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            decks.add(new Deck(i));
        }
        final AtomicInteger noWinner = new AtomicInteger(0);
        final List<Player> players = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            players.add(new Player(i, decks.get(i - 1), decks.get(i % n), noWinner));
        }
        int next = 0;
        for (int round = 0; round < Player.HAND_SIZE; round++) {
            for (Player p : players) {
                p.addCard(new Card(all.get(next++)));
            }
        }
        for (int d = 0; next < all.size(); d++) {
            decks.get(d % n).addCard(new Card(all.get(next++)));
        }

        final AtomicBoolean stop = new AtomicBoolean(false);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Thread> threads = new ArrayList<>();
        for (final Player p : players) {
            p.setOutput(new PrintWriter(new OutputStream() {
                @Override
                public void write(int b) {}
            }));
            Thread t = new Thread(() -> {
                while (!stop.get()) {
                    if (!p.takeTurn()) {
                        Thread.yield();
                    }
                }
            });
            t.setUncaughtExceptionHandler((thread, error) -> failure.compareAndSet(null, error));
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }
        Thread.sleep(500);
        stop.set(true);
        for (Thread t : threads) {
            t.join(10000);
            if (t.isAlive()) {
                fail("a player thread is stuck: deadlock");
            }
        }
        assertNull("a player thread crashed: " + failure.get(), failure.get());

        List<Integer> after = new ArrayList<>();
        for (Player p : players) {
            assertEquals(4, p.getHand().size());
            after.addAll(TestSupport.values(p.getHand()));
        }
        for (Deck d : decks) {
            after.addAll(TestSupport.values(d.getContents()));
        }
        Collections.sort(after);
        List<Integer> expected = new ArrayList<>(all);
        Collections.sort(expected);
        assertEquals("cards were lost or duplicated", expected, after);
    }
}