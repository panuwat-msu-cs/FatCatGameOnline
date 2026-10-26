
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.util.ArrayList;

public class GamePanel extends JPanel {

    // รูปพื้นหลัง
    Image bg = new ImageIcon("images/bg.gif").getImage();

    // รูปปลา 4 ชนิด
    Image fish10 = new ImageIcon("images/fish_gray.png").getImage();
    Image fish20 = new ImageIcon("images/fish_cream.png").getImage();
    Image fish30 = new ImageIcon("images/fish_orange.png").getImage();
    Image fish50 = new ImageIcon("images/fish_brown.png").getImage();

    // รูปแมว 5 ตัว
    Image[] catImages = {
        new ImageIcon("images/cat1.png").getImage(),
        new ImageIcon("images/cat2.png").getImage(),
        new ImageIcon("images/cat3.png").getImage(),
        new ImageIcon("images/cat4.png").getImage(),
        new ImageIcon("images/cat5.png").getImage()
    };

    // รูปก้างปลา
    Image boneImage = new ImageIcon("images/bone.png").getImage();

    // ตัวสุ่ม
    Random random = new Random();

    // รายการปลาและก้างปลา
    ArrayList<Fish> fishes = new ArrayList<>();
    ArrayList<Bone> bones = new ArrayList<>();

    // จำนวนแมว
    final int CAT_COUNT = 5;

    // ตำแหน่งแมวทั้ง 5 ตัว
    int[] catX = {100, 320, 540, 760, 980};
    int[] catY = {600, 600, 600, 600, 600};

    // ขนาดแมว
    int catWidth = 80;
    int catHeight = 70;

    // ความเร็วแมว
    int catSpeed = 10;

    // ขนาดปลา
    int fishWidth = 60;
    int fishHeight = 30;

    public GamePanel() {

        // กำหนดขนาดพื้นที่เกม
        setPreferredSize(new Dimension(1280, 720));

        // ให้ JPanel รับคีย์บอร์ดได้
        setFocusable(true);

        // กด A ไปซ้าย กด D ไปขวา
        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_A) {

                    for (int i = 0; i < CAT_COUNT; i++) {
                        catX[i] -= catSpeed;

                        if (catX[i] < 0) {
                            catX[i] = 0;
                        }
                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_D) {

                    for (int i = 0; i < CAT_COUNT; i++) {
                        catX[i] += catSpeed;

                        if (catX[i] > getWidth() - catWidth) {
                            catX[i] = getWidth() - catWidth;
                        }
                    }
                }

                repaint();
            }
        });

        // สุ่มปลาใหม่ทุก 500 มิลลิวินาที
        Timer spawnTimer = new Timer(500, e -> {
            spawnFish();
        });

        spawnTimer.start();

        // สุ่มก้างปลาทุก 1.5 วินาที
        Timer boneTimer = new Timer(1500, e -> {
            spawnBone();
        });

        boneTimer.start();

        // ทำให้ปลาและก้างปลาตกลงมา
        Timer gameTimer = new Timer(20, e -> {

            // เคลื่อนที่ปลา
            for (Fish fish : fishes) {
                fish.y += 3;
            }

            // เคลื่อนที่ก้างปลา
            for (Bone bone : bones) {
                bone.y += 5;
            }

            // ลบปลาที่ตกพ้นหน้าจอ
            fishes.removeIf(fish -> fish.y > getHeight());

            // ลบก้างปลาที่ตกพ้นหน้าจอ
            bones.removeIf(bone -> bone.y > getHeight());

            repaint();
        });

        gameTimer.start();
    }

    // สุ่มชนิดปลา
    public void spawnFish() {

        int number = random.nextInt(100);

        Image selectedFish;
        int fishScore;

        if (number < 50) {
            selectedFish = fish10;
            fishScore = 10;
        }
        else if (number < 80) {
            selectedFish = fish20;
            fishScore = 20;
        }
        else if (number < 95) {
            selectedFish = fish30;
            fishScore = 30;
        }
        else {
            selectedFish = fish50;
            fishScore = 50;
        }

        // สุ่มตำแหน่งปลา
        int x = random.nextInt(
            Math.max(1, getWidth() - fishWidth)
        );

        fishes.add(new Fish(selectedFish, x, 0, fishScore));
    }

    // สร้างก้างปลา
    public void spawnBone() {

        int x = random.nextInt(
            Math.max(1, getWidth() - 40)
        );

        bones.add(new Bone(x, 0));
    }

    // วาดพื้นหลัง ปลา ก้างปลา และแมว
    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        // วาดพื้นหลัง
        g.drawImage(
            bg, 0, 0,
            getWidth(), getHeight(),
            this
        );

        // วาดปลา
        for (Fish fish : fishes) {

            g.drawImage(
                fish.image,
                fish.x, fish.y,
                fishWidth, fishHeight,
                this
            );
        }

        // วาดก้างปลา
        for (Bone bone : bones) {

            g.drawImage(
                boneImage,
                bone.x, bone.y,
                40, 30,
                this
            );
        }

        // วาดแมวทั้ง 5 ตัว
        for (int i = 0; i < CAT_COUNT; i++) {

            g.drawImage(
                catImages[i],
                catX[i], catY[i],
                catWidth, catHeight,
                this
            );
        }
    }

    // ข้อมูลปลา
    class Fish {

        Image image;
        int x;
        int y;
        int point;

        public Fish(Image image, int x, int y, int point) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.point = point;
        }
    }

    // ข้อมูลก้างปลา
    class Bone {

        int x;
        int y;

        public Bone(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // เปิดหน้าต่างเกม
    public static void main(String[] args) {

        JFrame frame = new JFrame("FatCat Game");

        GamePanel gamePanel = new GamePanel();

        frame.add(gamePanel);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // ให้คีย์บอร์ดควบคุมเกมได้
        SwingUtilities.invokeLater(() ->
            gamePanel.requestFocusInWindow()
        );
    }
}
