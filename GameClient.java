import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;
import javax.swing.*;
public class GameClient extends JFrame {

    static final int W = 800, H = 450;
    static final int PORT = 5000;

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final GamePanel gamePanel = new GamePanel();
    private final JLabel lobbyLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton startGameBtn = new JButton("START GAME");
    private final ResultPanel resultPanel = new ResultPanel();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private String myName = "CAT";
    private String host = "localhost";
    private int myId = -1;
    private boolean showingResult = false;

    private final Player[] cache = new Player[5];

    private boolean left, right;
    private int lastDir = 0;

    public GameClient() {
        super("FatCat Online");
        root.setPreferredSize(new Dimension(W, H));

        root.add(buildStart(), "start");
        root.add(buildHow(), "how");
        root.add(buildLobby(), "lobby");
        root.add(gamePanel, "game");
        root.add(buildResult(), "result");

        setupKeys();

        setContentPane(root);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    static class BgPanel extends JPanel {
        private final Image bg = new ImageIcon("images/bg.gif").getImage();
        BgPanel() { setLayout(null); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(bg, 0, 0, W, H, this);
        }
    }

    static class ImagePanel extends JPanel {
        private final Image img;
        ImagePanel(String path) {
            setLayout(null);
            img = new ImageIcon(path).getImage();
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(img, 0, 0, W, H, this);
        }
    }

    private JButton invisibleButton(int x, int y, int w, int h) {
        JButton b = new JButton();
        b.setBounds(x, y, w, h);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton styledButton(String text, int x, int y, int w, int h) {
        JButton b = new JButton(text);
        b.setBounds(x, y, w, h);
        b.setFont(new Font("Courier New", Font.BOLD, 20));
        b.setBackground(new Color(230, 228, 218));
        b.setFocusPainted(false);
        return b;
    }

    private JPanel buildStart() {
        ImagePanel p = new ImagePanel("images/start.png");
        JButton start = invisibleButton(348, 252, 104, 48);   // ทับปุ่ม START ในรูป
        start.addActionListener(e -> askNameAndHost());
        p.add(start);
        return p;
    }

    private void askNameAndHost() {
        JTextField nameField = new JTextField(myName);
        JTextField hostField = new JTextField(host);
        Object[] msg = { "ชื่อของคุณ:", nameField, "IP ของ Server:", hostField };
        int r = JOptionPane.showConfirmDialog(this, msg, "FatCat Online",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        myName = nameField.getText().trim();
        host = hostField.getText().trim();
        if (host.isEmpty()) host = "localhost";
        cards.show(root, "how");
    }

    private JPanel buildHow() {
        ImagePanel p = new ImagePanel("images/how.png");
        JButton next = styledButton("NEXT", 620, 340, 100, 44);
        next.setBackground(new Color(143, 138, 106));
        next.setOpaque(true);
        next.setBorderPainted(false);
        next.addActionListener(e -> connect());
        p.add(next);
        return p;
    }

    private JPanel buildLobby() {
        BgPanel p = new BgPanel();
        lobbyLabel.setFont(new Font("Courier New", Font.BOLD, 28));
        lobbyLabel.setOpaque(true);
        lobbyLabel.setBackground(new Color(230, 228, 218));
        lobbyLabel.setBounds(150, 150, 500, 80);
        p.add(lobbyLabel);

        startGameBtn.setBounds(300, 260, 200, 55);
        startGameBtn.setFont(new Font("Courier New", Font.BOLD, 20));
        startGameBtn.setEnabled(false);
        startGameBtn.addActionListener(e -> send("START"));
        p.add(startGameBtn);
        return p;
    }

    private JPanel buildResult() {
        JPanel p = new JPanel(null);
        resultPanel.setBounds(0, 0, W, H);
        JButton back = styledButton("BACK", 350, 395, 100, 36);
        back.addActionListener(e -> {
            showingResult = false;
            cards.show(root, "lobby");
        });
        p.add(back);
        p.add(resultPanel);
        return p;
    }
    
    static class ResultPanel extends JPanel {
        private final Image bg = new ImageIcon("images/winner.png").getImage();
        String winnerName = "";
        int winnerId = 0;
        int winnerScore = 0;
        List<Player> ranking = new ArrayList<>();

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(bg, 0, 0, W, H, this);

            Image cat = new ImageIcon("images/cat" + (winnerId + 1) + ".png").getImage();
            g.drawImage(cat, 340, 255, 120, 120, this);

            g.setColor(Color.BLACK);
            g.setFont(new Font("Courier New", Font.BOLD, 28));
            int w = g.getFontMetrics().stringWidth(winnerName);
            g.drawString(winnerName, (W - w) / 2, 205);
            g.setFont(new Font("Courier New", Font.BOLD, 18));
            String sc = winnerScore + " pts";
            w = g.getFontMetrics().stringWidth(sc);
            g.drawString(sc, (W - w) / 2, 232);
        }
    }


    private void setupKeys() {
        gamePanel.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT) left = true;
                if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) right = true;
                sendDir();
            }
            @Override public void keyReleased(KeyEvent e) {
                int k = e.getKeyCode();
                if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT) left = false;
                if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) right = false;
                sendDir();
            }
        });
    }

    private void sendDir() {
        int dir = (right ? 1 : 0) - (left ? 1 : 0);
        if (dir != lastDir) {
            lastDir = dir;
            send("MOVE|" + dir);
        }
    }

    private void connect() {
        try {
            socket = new Socket(host, PORT);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            send("JOIN|" + myName);
            Thread t = new Thread(this::listen);
            t.setDaemon(true);
            t.start();
            lobbyLabel.setText("Connecting...");
            cards.show(root, "lobby");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "เชื่อมต่อ Server ไม่ได้: " + host + ":" + PORT);
            cards.show(root, "start");
        }
    }

    private void send(String msg) {
        if (out != null) out.println(msg);
    }

    private void listen() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                final String l = line;
                SwingUtilities.invokeLater(() -> handle(l));
            }
        } catch (IOException ignored) {
        }
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "การเชื่อมต่อกับ Server หลุด");
            showingResult = false;
            cards.show(root, "start");
        });
    }

    private void handle(String line) {
        String[] t = line.split("\\|", -1);
        switch (t[0]) {
            case "ID":
                myId = Integer.parseInt(t[1]);
                gamePanel.setMyId(myId);
                break;
            case "LOBBY": {
                int n = Integer.parseInt(t[1]);
                lobbyLabel.setText("Waiting for players... " + n + "/5");
                startGameBtn.setEnabled(n >= 3);
                if (!showingResult) cards.show(root, "lobby");
                break;
            }
            case "GO":
                showingResult = false;
                left = right = false;
                lastDir = 0;
                cards.show(root, "game");
                gamePanel.requestFocusInWindow();
                break;
            case "STATE":
                gamePanel.update(parsePlayers(t[2]), parseItems(t[3]), Integer.parseInt(t[1]));
                break;
            case "END": {
                Player[] ps = parsePlayers(t[2]);
                List<Player> list = new ArrayList<>();
                for (Player p : ps) if (p != null) list.add(p);
                list.sort((a, b) -> b.score - a.score);
                int wid = Integer.parseInt(t[1]);
                resultPanel.ranking = list;
                resultPanel.winnerId = wid < 0 ? 0 : wid;
                resultPanel.winnerName = (wid >= 0 && ps[wid] != null) ? ps[wid].name : "-";
                resultPanel.winnerScore = (wid >= 0 && ps[wid] != null) ? ps[wid].score : 0;
                showingResult = true;
                cards.show(root, "result");
                resultPanel.repaint();
                break;
            }
            case "BUSY":
            case "FULL":
                JOptionPane.showMessageDialog(this,
                        t[0].equals("BUSY") ? "ตอนนี้กำลังเล่นอยู่ รอจบเกมก่อน" : "ห้องเต็มแล้ว (5 คน)");
                closeSocket();
                cards.show(root, "start");
                break;
        }
    }

    private void closeSocket() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private Player[] parsePlayers(String s) {
        Player[] arr = new Player[5];
        if (s.isEmpty()) return arr;
        for (String part : s.split(";")) {
            String[] f = part.split(",", -1);
            int id = Integer.parseInt(f[0]);
            String name = f[5];
            if (cache[id] == null || !cache[id].name.equals(name)) cache[id] = new Player(id, name);
            Player p = cache[id];
            p.x = Integer.parseInt(f[1]);
            p.score = Integer.parseInt(f[2]);
            p.lives = Integer.parseInt(f[3]);
            p.alive = f[4].equals("1");
            arr[id] = p;
        }
        return arr;
    }

    private List<Item> parseItems(String s) {
        List<Item> list = new ArrayList<>();
        if (s.isEmpty()) return list;
        for (String part : s.split(";")) {
            String[] f = part.split(",");
            list.add(new Item(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), 0));
        }
        return list;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameClient().setVisible(true));
    }
}
