import java.util.Queue;
import java.util.LinkedList;
import java.util.NoSuchElementException;
public class Deck {

    private Queue<Card> cards = new LinkedList<>();

    public Deck(){
        cards = new LinkedList<>();
    }

    public void AddCard(Card card){
        cards.add(card);

    }

    public Card RemoveCard(){
        if (cards.size() == 0){
            throw new NoSuchElementException("Deck is empty");
        }
        else{
            Card firstCard = cards.element();
            cards.remove(firstCard);
            return firstCard;
        }
    }
    public int getSize(){
        return cards.size();
    }  
}
