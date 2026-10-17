import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class CardGame {
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
 
        int n = promptForPlayers(in);
        if (n < 0) {
            return;
        }
        List<Card> pack = promptForPack(in, n);
        if (pack == null) {
            return;
        }
 
        try {
            play(n, pack);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Game interrupted.");
        }
    }

    private static int promptForPlayers(Scanner in) {
        while (true) {
            System.out.println("Please enter the number of players:");
            if (!in.hasNextLine()) {
                return -1;
            }
            String line = in.nextLine().trim();
            try {
                int n = Integer.parseInt(line);
                if (n > 0) {
                    return n;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Invalid input: the number of players must be a positive whole number.");
        }
    }
 
    private static List<Card> promptForPack(Scanner in, int n) {
        while (true) {
            System.out.println("Please enter location of pack to load:");
            if (!in.hasNextLine()) {
                return null;
            }
            String path = in.nextLine().trim();
            try {
                return readPack(path, n);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid pack: " + e.getMessage());
            }
        }
    }

    public static List<Card> readPack(String path, int n) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(path));
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("cannot read file '" + path + "'.");
        }
 
        int expected = 8 * n;
        if (lines.size() != expected) {
            throw new IllegalArgumentException("the pack must contain exactly " + expected
                    + " lines for " + n + " players but has " + lines.size() + ".");
        }
 
        List<Card> pack = new ArrayList<>(expected);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            try {
                pack.add(new Card(Integer.parseInt(line)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("line " + (i + 1) + " ('" + line
                        + "') is not a non-negative integer.");
            }
        }
        return pack;
    }

    public static int play(int n, List<Card> pack) throws InterruptedException {
        if (pack.size() != 8 * n) {
            throw new IllegalArgumentException("pack must contain 8n cards");
        }

        List<Deck> decks = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            decks.add(new Deck(i));
        }
        AtomicInteger winner = new AtomicInteger(0);
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            players.add(new Player(i, decks.get(i - 1), decks.get(i % n), winner));
        }
 
        int next = 0;
        for (int round = 0; round < Player.HAND_SIZE; round++) {
            for (Player p : players) {
                p.addCard(pack.get(next++));
            }
        }

        int deckIndex = 0;
        while (next < pack.size()) {
            decks.get(deckIndex % n).addCard(pack.get(next++));
            deckIndex++;
        }
 
        for (Player p : players) {
            if (p.checkHand()) {
                winner.set(p.getIndex());
                System.out.println("player " + p.getIndex() + " wins");
                break;
            }
        }
 
        List<Thread> threads = new ArrayList<>();
        for (Player p : players) {
            Thread t = new Thread(p, "player-" + p.getIndex());
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
 
        writeDeckFiles(decks);
        return winner.get();
    }

    private static void writeDeckFiles(List<Deck> decks) {
        for (Deck d : decks) {
            try (PrintWriter w = new PrintWriter(new FileWriter("deck" + d.getId() + "_output.txt"))) {
                w.println(d.describe());
            } catch (IOException e) {
                System.err.println("deck " + d.getId() + " could not write output: " + e.getMessage());
            }
        }
    }
}
