import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class DeckTest {

    @Test
    public void cardsComeOutInTheOrderTheyWentIn() {
        Deck deck = new Deck(1);
        deck.addCard(new Card(5));
        deck.addCard(new Card(2));
        deck.addCard(new Card(9));
        assertEquals(5, deck.removeCard().getValue());
        assertEquals(2, deck.removeCard().getValue());
        assertEquals(9, deck.removeCard().getValue());
    }

    @Test(expected = NoSuchElementException.class)
    public void removingFromAnEmptyDeckThrows() {
        new Deck(1).removeCard();
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingNullIsRejected() {
        new Deck(1).addCard(null);
    }

    @Test
    public void sizeAndEmptinessTrackAddsAndRemoves() {
        Deck deck = new Deck(1);
        assertTrue(deck.isEmpty());
        deck.addCard(new Card(1));
        deck.addCard(new Card(1));
        assertEquals(2, deck.getSize());
        assertFalse(deck.isEmpty());
        deck.removeCard();
        deck.removeCard();
        assertTrue(deck.isEmpty());
    }

    @Test
    public void contentsIsASnapshotNotALiveView() {
        Deck deck = new Deck(1);
        deck.addCard(new Card(4));
        List<Card> snapshot = deck.getContents();
        snapshot.clear();
        deck.addCard(new Card(6));
        assertEquals(2, deck.getSize());
        assertEquals(0, snapshot.size());
    }

    @Test
    public void describeMatchesTheRequiredOutputFormat() {
        Deck deck = new Deck(2);
        assertEquals("deck2 contents:", deck.describe());
        for (int v : new int[] {1, 3, 3, 7}) {
            deck.addCard(new Card(v));
        }
        assertEquals("deck2 contents: 1 3 3 7", deck.describe());
    }


    @Test(timeout = 30000)
    public void concurrentProducersAndConsumersNeverLoseOrDuplicateCards() throws Exception {
        final Deck deck = new Deck(1);
        final int producers = 4;
        final int perProducer = 1000;
        final int total = producers * perProducer;
        final AtomicInteger removed = new AtomicInteger();
        final Queue<Integer> taken = new ConcurrentLinkedQueue<>();
        final CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();

        for (int p = 0; p < producers; p++) {
            final int base = p * perProducer;
            threads.add(new Thread(() -> {
                awaitQuietly(start);
                for (int i = 0; i < perProducer; i++) {
                    deck.addCard(new Card(base + i));
                }
            }));
        }
        for (int c = 0; c < 4; c++) {
            threads.add(new Thread(() -> {
                awaitQuietly(start);
                while (removed.get() < total) {
                    try {
                        taken.add(deck.removeCard().getValue());
                        removed.incrementAndGet();
                    } catch (NoSuchElementException empty) {
                        Thread.yield();
                    }
                }
            }));
        }

        for (Thread t : threads) {
            t.start();
        }
        start.countDown();
        for (Thread t : threads) {
            t.join();
        }

        assertEquals(total, taken.size());
        assertEquals("a card was removed twice", total, new HashSet<>(taken).size());
        assertTrue(deck.isEmpty());
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void getContentsPreservesOrder() {
        Deck deck = new Deck(1);
        for (int v : new int[] {3, 1, 2}) {
            deck.addCard(new Card(v));
        }
        assertEquals(Arrays.asList(3, 1, 2), TestSupport.values(deck.getContents()));
    }
}