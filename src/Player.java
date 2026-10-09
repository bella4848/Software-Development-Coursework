import java.util.ArrayList;

public class Player {

    private int playerIndex;
    private ArrayList<Card> hand = new ArrayList<Card>();

    public Player(int playerIndex){
        this.playerIndex = playerIndex;

    }

    public int getIndex(){
        return playerIndex;
    }

    public void AddCard(Card card){
        hand.add(card);
    }

    public void RemoveCard(Card card){
        hand.remove(card);
    }

    public Boolean CheckHand(){
        if (hand.size() != 4){
            return false;
        }
        else{
            int firstCard = hand.get(0).getValue();
            for (int i = 1; i < 4; i++){
                if (firstCard != hand.get(i).getValue()){
                    return false;
                }

            }
        }

        return true;
    }

    public int PreferredValue(){
        return playerIndex;
    }

    public Boolean Discard(int discardCardIndex){

        Card discardCard = hand.get(discardCardIndex);

        if (discardCard.getValue() == PreferredValue()){

            return false;
        }
        else{
            hand.remove(discardCardIndex);
            return true;
        }

    }
}
