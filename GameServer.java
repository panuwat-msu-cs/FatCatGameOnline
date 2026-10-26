import java.awt.Rectangle;
import java.io.*;
import java.net.*;
import java.util.*;
public class GameServer {

    static final int PORT = 5000;
    static final int WIDTH = 800, HEIGHT = 450;
    static final int MAX_PLAYERS = 5, MIN_PLAYERS = 3;
    static final int GAME_SECONDS = 180;
    static final int TICK_MS = 30;
    static final int CAT_W = 70, CAT_H = 70, CAT_SPEED = 7;
    static final int ITEM_W = 40, ITEM_H = 26;
    static final int HEAL_EVERY_SEC = 30;
    static final int MAX_LIVES = 9;

    private final Object lock = new Object();
    private final Player[] players = new Player[MAX_PLAYERS];
    private final ClientHandler[] handlers = new ClientHandler[MAX_PLAYERS];
    private final int[] dirs = new int[MAX_PLAYERS];
    private final List<Item> items = new ArrayList<>();
    private final Random rnd = new Random();

    private boolean playing = false;
    private long startTime, lastSpawn;
    private int healStep = 0;

    public static void main(String[] args) throws IOException {
        new GameServer().start();
    }

    void start() throws IOException {
        // Thread ลูปเกม
        Thread loop = new Thread(() -> {
            while (true) {
                try { Thread.sleep(TICK_MS); } catch (InterruptedException e) { return; }
                synchronized (lock) {
                    if (playing) update();
                }
            }
        });
        loop.setDaemon(true);
        loop.start();

        // รับ Client
        ServerSocket server = new ServerSocket(PORT);
        System.out.println("FatCat Server started on port " + PORT);
        while (true) {
            Socket s = server.accept();
            new ClientHandler(s).start();
        }
    }

    // ---------------- Game logic (เรียกใน lock เท่านั้น) ----------------

    private int playerCount() {
        int n = 0;
        for (Player p : players) if (p != null) n++;
        return n;
    }

    private void resetPlayers() {
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null) {
                players[i].score = 0;
                players[i].lives = MAX_LIVES;
                players[i].alive = true;
                players[i].x = 375;
            }
            dirs[i] = 0;
        }
        items.clear();
    }

    private void startGame() {
        resetPlayers();
        playing = true;
        startTime = System.currentTimeMillis();
        lastSpawn = startTime;
        healStep = 0;
        broadcast("GO");
    }

    private void update() {
        long now = System.currentTimeMillis();
        int elapsedSec = (int) ((now - startTime) / 1000);
        int timeLeft = Math.max(0, GAME_SECONDS - elapsedSec);

        //เลื่อนแมว
        for (int i = 0; i < MAX_PLAYERS; i++) {
            Player p = players[i];
            if (p != null && p.alive) {
                p.x = Math.max(0, Math.min(WIDTH - CAT_W, p.x + dirs[i] * CAT_SPEED));
            }
        }

        //สร้างปลา (ยิ่งนานยิ่งถี่)
        long interval = Math.max(250, 800 - elapsedSec * 3L);
        if (now - lastSpawn >= interval) {
            lastSpawn = now;
            items.add(new Item(rnd.nextInt(WIDTH - ITEM_W), -ITEM_H, randomType(), 0));
        }

        //ปลา/ก้างตก (ยิ่งนานยิ่งเร็ว) + ตรวจการชน
        int fall = 3 + elapsedSec / 25;
        Iterator<Item> it = items.iterator();
        while (it.hasNext()) {
            Item item = it.next();
            item.y += fall;
            Rectangle ir = new Rectangle(item.x, item.y, ITEM_W, ITEM_H);
            boolean caught = false;
            for (Player p : players) {
                if (p == null || !p.alive) continue;
                Rectangle cr = new Rectangle(p.x + 10, p.y + 10, CAT_W - 20, 40);
                if (cr.intersects(ir)) {
                    if (item.points > 0) {
                        p.score += item.points;
                    } else {
                        p.lives--;
                        if (p.lives <= 0) { p.lives = 0; p.alive = false; }
                    }
                    caught = true;
                    break;
                }
            }
            if (caught || item.y > HEIGHT) it.remove();
        }

        //เพิ่มพลังชีวิตทุก 30 วินาที
        int step = elapsedSec / HEAL_EVERY_SEC;
        if (step > healStep) {
            healStep = step;
            for (Player p : players)
                if (p != null && p.alive && p.lives < MAX_LIVES) p.lives++;
        }

        //จบเกม
        boolean anyAlive = false;
        for (Player p : players) if (p != null && p.alive) anyAlive = true;
        if (timeLeft == 0 || !anyAlive) {
            endGame();
            return;
        }

        broadcast("STATE|" + timeLeft + "|" + playersString() + "|" + itemsString());
    }

    private int randomType() {
        int r = rnd.nextInt(100);
        if (r < 32) return 0;   // เทา +10
        if (r < 55) return 1;   // ครีม +20
        if (r < 70) return 2;   // ส้ม +30
        if (r < 80) return 3;   // น้ำตาล +50
        return 4;               // ก้างปลา -1 ชีวิต
    }

    private void endGame() {
        int winner = -1, best = -1;
        for (int i = 0; i < MAX_PLAYERS; i++) {
            if (players[i] != null && players[i].score > best) {
                best = players[i].score;
                winner = i;
            }
        }
        broadcast("END|" + winner + "|" + playersString());
        playing = false;
        resetPlayers();
        broadcastLobby();
    }

    private String playersString() {
        StringBuilder sb = new StringBuilder();
        for (Player p : players) {
            if (p == null) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(p.id).append(',').append(p.x).append(',').append(p.score).append(',')
              .append(p.lives).append(',').append(p.alive ? 1 : 0).append(',').append(p.name);
        }
        return sb.toString();
    }

    private String itemsString() {
        StringBuilder sb = new StringBuilder();
        for (Item i : items) {
            if (sb.length() > 0) sb.append(';');
            sb.append(i.x).append(',').append(i.y).append(',').append(i.type);
        }
        return sb.toString();
    }

    private void broadcast(String msg) {
        for (ClientHandler h : handlers) if (h != null) h.send(msg);
    }

    private void broadcastLobby() {
        broadcast("LOBBY|" + playerCount());
    }

    // ---------------- Thread ต่อ Client 1 คน ----------------

    class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int id = -1;

        ClientHandler(Socket socket) { this.socket = socket; }

        void send(String msg) {
            if (out == null) return;
            synchronized (out) { out.println(msg); }
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
                String line;
                while ((line = in.readLine()) != null) handle(line);
            } catch (IOException e) {
                // client หลุด
            } finally {
                leave();
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void handle(String line) {
            String[] t = line.split("\\|", -1);
            synchronized (lock) {
                switch (t[0]) {
                    case "JOIN":
                        if (id >= 0) return;
                        if (playing) { send("BUSY"); return; }
                        int slot = -1;
                        for (int i = 0; i < MAX_PLAYERS; i++) if (players[i] == null) { slot = i; break; }
                        if (slot < 0) { send("FULL"); return; }
                        String name = t.length > 1 ? t[1].replaceAll("[|;,]", "").trim() : "";
                        if (name.length() > 10) name = name.substring(0, 10);
                        if (name.isEmpty()) name = "CAT" + (slot + 1);
                        players[slot] = new Player(slot, name);
                        handlers[slot] = this;
                        id = slot;
                        send("ID|" + slot);
                        System.out.println(name + " joined as slot " + slot);
                        broadcastLobby();
                        break;
                    case "MOVE":
                        if (id >= 0 && t.length > 1) {
                            try {
                                int d = Integer.parseInt(t[1]);
                                dirs[id] = Math.max(-1, Math.min(1, d));
                            } catch (NumberFormatException ignored) {}
                        }
                        break;
                    case "START":
                        if (id >= 0 && !playing && playerCount() >= MIN_PLAYERS) startGame();
                        break;
                }
            }
        }

        private void leave() {
            synchronized (lock) {
                if (id < 0) return;
                System.out.println("Player slot " + id + " left");
                players[id] = null;
                handlers[id] = null;
                dirs[id] = 0;
                id = -1;
                if (playerCount() == 0) {
                    playing = false;
                    items.clear();
                } else if (!playing) {
                    broadcastLobby();
                }
            }
        }
    }
}