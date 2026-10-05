public class Card {
    private int cardValue;

    public Card(int cardValue){
        this.cardValue = cardValue;
        if (cardValue < 0){
            throw new IllegalArgumentException("Negative int card");
        }
    }

    public int getValue(){
        return cardValue;
    }




}
