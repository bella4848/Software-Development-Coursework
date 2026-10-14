import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReentrantLock;

public class Deck {

    private final int id;
    private final Deque<Card> cards = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();


    public Deck(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card must not be null");
        }
        lock.lock();
        try {
            cards.addLast(card);
        } finally {
            lock.unlock();
        }
    }

    public Card removeCard() {
        lock.lock();
        try {
            if (cards.isEmpty()) {
                throw new NoSuchElementException("Deck is empty");
            }
            return cards.removeFirst();
        } finally {
            lock.unlock();
        }
    }
    public boolean isEmpty() {
        lock.lock();
        try {
            return cards.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public int getSize() {
        lock.lock();
        try {
            return cards.size();
        } finally {
            lock.unlock();
        }
    }

    public List<Card> getContents() {
        lock.lock();
        try {
            return new ArrayList<>(cards);
        } finally {
            lock.unlock();
        }
    }

    public String describe() {
        StringBuilder sb = new StringBuilder("deck" + id + " contents:");
        for (Card c : getContents()) {
            sb.append(' ').append(c.getValue());
        }
        return sb.toString();
    }
}
