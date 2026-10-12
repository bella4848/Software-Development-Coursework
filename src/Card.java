public final class Card {
    private final int cardValue;

    public Card(int cardValue){
        if (cardValue < 0){
            throw new IllegalArgumentException("Negative int card");
        }
        this.cardValue = cardValue;
    }

    public int getValue(){
        return cardValue;
    }

    @Override
    public String toString() {
        return String.valueOf(cardValue);
    }


}
