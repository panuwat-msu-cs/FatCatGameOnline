import java.awt.Image;
import javax.swing.ImageIcon;

public class Player {

    public final int id;
    public volatile String name;

    public volatile int x = 375;
    public final int y = 315;

    public volatile int score = 0;
    public volatile int lives = 9;
    public volatile boolean alive = true;

    public final Image image;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;

        String path = "images/cat" + (id + 1) + ".png";

        image = new ImageIcon(path).getImage();
    }
}
