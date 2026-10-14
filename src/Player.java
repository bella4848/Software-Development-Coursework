import java.util.ArrayList;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Player implements Runnable {

    public static final int HAND_SIZE = 4;

    private final int playerIndex;
    private final List<Card> hand = new ArrayList<>();
    private final Deck drawDeck;
    private final Deck discardDeck;
    private final AtomicInteger winner;
    private PrintWriter out;

    public Player(int playerIndex, Deck drawDeck, Deck discardDeck, AtomicInteger winner) {
        this.playerIndex = playerIndex;
        this.drawDeck = drawDeck;
        this.discardDeck = discardDeck;
        this.winner = winner;
    }

    public void setOutput(PrintWriter writer) {
        this.out = writer;
    }

    public int getIndex() {
        return playerIndex;
    }

    public int preferredValue() {
        return playerIndex;
    }

    public void addCard(Card card) {
        if (hand.size() >= HAND_SIZE) {
            throw new IllegalStateException("Hand already has " + HAND_SIZE + " cards");
        }
        hand.add(card);
    }

    public List<Card> getHand() {
        return new ArrayList<>(hand);
    }

    public boolean checkHand() {
        if (hand.size() != HAND_SIZE) {
            return false;
        }
        int first = hand.get(0).getValue();
        for (int i = 1; i < HAND_SIZE; i++) {
            if (hand.get(i).getValue() != first) {
                return false;
            }
        }
        return true;
    }

    private int chooseDiscardIndex() {
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).getValue() != preferredValue()) {
                return i;
            }
        }
        throw new IllegalStateException("No non-preferred card to discard");
    }

    public boolean takeTurn() {
        Deck first = drawDeck.getId() <= discardDeck.getId() ? drawDeck : discardDeck;
        Deck second = (first == drawDeck) ? discardDeck : drawDeck;

        Card drawn;
        Card discarded;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                if (drawDeck.isEmpty()) {
                    return false;
                }
                drawn = drawDeck.removeCard();
                hand.add(drawn);
                discarded = hand.remove(chooseDiscardIndex());
                discardDeck.addCard(discarded);
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }

        out.println("player " + playerIndex + " draws a " + drawn.getValue() + " from deck " + drawDeck.getId());
        out.println("player " + playerIndex + " discards a " + discarded.getValue() + " to deck " + discardDeck.getId());
        out.println("player " + playerIndex + " current hand is " + handString());
        return true;
    }

    private String handString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(hand.get(i).getValue());
        }
        return sb.toString();
    }

    @Override
    public void run() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("player" + playerIndex + "_output.txt"))) {
            out = writer;
            out.println("player " + playerIndex + " initial hand " + handString());

            while (winner.get() == 0) {
                if (!takeTurn()) {
                    Thread.yield();
                    continue;
                }
                if (checkHand() && winner.compareAndSet(0, playerIndex)) {
                    System.out.println("player " + playerIndex + " wins");
                }
            }
            writeEnding(winner.get());
        } catch (IOException e) {
            System.err.println("player " + playerIndex + " could not write output: " + e.getMessage());
        }
    }

    private void writeEnding(int winnerIndex) {
        if (winnerIndex == playerIndex) {
            out.println("player " + playerIndex + " wins");
            out.println("player " + playerIndex + " exits");
            out.println("player " + playerIndex + " final hand: " + handString());
        } else {
            out.println("player " + winnerIndex + " has informed player " + playerIndex + " that player " + winnerIndex + " has won");
            out.println("player " + playerIndex + " exits");
            out.println("player " + playerIndex + " hand: " + handString());
        }
    }
}