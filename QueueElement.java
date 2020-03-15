/**
 * Represents one entry in the Queue that contains which links to open at which times
 * 
 * @author Jatin Kohli
 * @since 3/14/20
 */
public class QueueElement {
    public String time;
    public String link;

    public QueueElement(String time, String link) {
        this.time = time;
        this.link = link;
    }
}