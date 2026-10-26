import java.awt.Image;
import javax.swing.ImageIcon;

public class Item {

    public final int x;
    public volatile int y;
    public final int type;
    public final int points;
    public final int size = 26;

    public final Image image;

    public Item(int x, int y, int type, int points) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.points = points;

        String fish;

        if (type == 0) {
            fish = "images/fish_gray.png";
        } else if (type == 1) {
            fish = "images/fish_cream.png";
        } else if (type == 2) {
            fish = "images/fish_orange.png";
        } else if (type == 3) {
            fish = "images/fish_brown.png";
        } else {
            fish = "images/bone.png";
        }

        image = new ImageIcon(fish).getImage();
    }
}
