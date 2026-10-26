import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class GamePanel extends JPanel {

    public static final int W = 800, H = 450;

    private final Image bg = new ImageIcon("images/bg.gif").getImage();
    private Player[] players = new Player[5];
    private List<Item> items = new ArrayList<>();
    private int timeLeft = 180;
    private int myId = -1;

    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
    }

    public void setMyId(int id) { myId = id; }

    public void update(Player[] players, List<Item> items, int timeLeft) {
        this.players = players;
        this.items = items;
        this.timeLeft = timeLeft;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.drawImage(bg, 0, 0, W, H, this);

        for (Item it : items) g.drawImage(it.image, it.x, it.y, 40, 26, this);

        g.setFont(new Font("Courier New", Font.BOLD, 12));
        for (Player p : players) {
            if (p == null) continue;
            Composite old = g.getComposite();
            if (!p.alive) g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g.drawImage(p.image, p.x, p.y, 70, 70, this);
            g.setComposite(old);
            g.setColor(p.id == myId ? new Color(200, 30, 30) : Color.BLACK);
            String label = p.id == myId ? p.name + " (YOU)" : p.name;
            int sw = g.getFontMetrics().stringWidth(label);
            g.drawString(label, p.x + 35 - sw / 2, p.y - 4);
        }

        g.setColor(Color.WHITE);
        g.fillRoundRect(15, 15, 110, 36, 30, 30);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Courier New", Font.BOLD, 16));
        g.drawString("Time:" + timeLeft, 28, 39);

        g.setColor(Color.WHITE);
        g.fillRoundRect(W - 245, 15, 230, 125, 30, 30);
        g.setColor(Color.BLACK);
        g.setFont(new Font("Courier New", Font.BOLD, 20));
        g.drawString("Score", W - 160, 38);
        g.setFont(new Font("Dialog", Font.BOLD, 11));
        int row = 0;
        for (Player p : players) {
            if (p == null) continue;
            StringBuilder hearts = new StringBuilder();
            for (int i = 0; i < p.lives; i++) hearts.append("\u2665");
            g.drawString(p.name + ": " + p.score + " " + hearts, W - 232, 58 + row * 18);
            row++;
        }
    }
}
