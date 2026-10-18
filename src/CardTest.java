import static org.junit.Assert.assertEquals;
 
import org.junit.Test;
 
public class CardTest {
 
    @Test(expected = IllegalArgumentException.class)
    public void negativeValueIsRejected() {
        new Card(-1);
    }
 
    @Test
    public void zeroAndVeryLargeValuesAreLegal() {
        assertEquals(0, new Card(0).getValue());
        assertEquals(Integer.MAX_VALUE, new Card(Integer.MAX_VALUE).getValue());
    }
}