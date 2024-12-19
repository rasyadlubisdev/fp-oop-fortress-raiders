package main;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import entity.Player;
import entity.Chest;

public class GamePanel extends JPanel implements Runnable {

    final int originalTileSize = 16; 
    final int scale = 3;
    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16, maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;

    public int maxWorldCol = 60;
    public int maxWorldRow = 60;
    public int worldWidth = maxWorldCol * tileSize;
    public int worldHeight = maxWorldRow * tileSize;

    int FPS = 60;
    
    public String playerName;
    public String roomCode;
    public String teamSelected;

    public int cameraX, cameraY;
    KeyHandler keyH = new KeyHandler();
    Thread gameThread;
    
    public Player player;  // Local player
    public HashMap<String, Player> otherPlayers = new HashMap<>();

    public TileManager tileM = new TileManager(this);
    public CollisionChecker cChecker = new CollisionChecker(this);

    public Chest currentChest;
    boolean chestUsed = false;
    long chestSpawnTime = 0;

    public Rectangle fortressA, fortressB;
    public Rectangle prisonARect, prisonBRect;

    long matchStartTime;
    long roundDuration = 10 * 60 * 1000;

    long p1AttackCooldownEnd = 0;
    long ATTACK_COOLDOWN = 10000;

    public FirebaseManager fbManager;

    // Game state
    private boolean gameEnded = false;
    private String winner = "";

    public GamePanel(String playerName, String roomCode, String teamSelected, JFrame window) {
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.black);
        setDoubleBuffered(true);
        addKeyListener(keyH);
        setFocusable(true);
        
        fbManager = new FirebaseManager();

        this.playerName = playerName;
        this.roomCode = roomCode;
        this.teamSelected = teamSelected;

        initializeFortressAndPrison();

        // Initialize local player & spawn
        player = new Player(this, keyH, false, playerName, teamSelected);
        System.out.println("Local player initialized: " + player.playerName);

        fbManager.joinRoom(roomCode, playerName, teamSelected);
        spawnPlayer();

        fbManager.listenGameStartTime(roomCode, startTime -> {
            if(startTime != 0){
                matchStartTime = startTime;
                System.out.println("Game start time received: " + matchStartTime);
            }
        });

        fbManager.isGameStartTimeSet(roomCode, isSet -> {
            if(!isSet){
                matchStartTime = System.currentTimeMillis();
                fbManager.setGameStartTime(roomCode, matchStartTime);
                System.out.println("Game start time not set. Setting to current time: " + matchStartTime);
            } else {
                System.out.println("Game start time sudah diset.");
            }
        });

        fbManager.listenPlayerPositions(roomCode, data -> {
            for(Map.Entry<String, Map<String, Object>> entry : data.entrySet()) {
                String pName = entry.getKey();
                Map<String, Object> pData = entry.getValue();
                Player p;
                if(pName.equals(playerName)){
                    p = player; // Local player
                } else {
                    p = otherPlayers.get(pName);
                    if(p == null){
                        String team = (String)pData.getOrDefault("team", teamSelected);
                        p = new Player(this, keyH, true, pName, team);
                        otherPlayers.put(pName, p);
                        System.out.println("Membuat pemain remote baru: " + pName);
                    }
                }

                int rx = ((Long)pData.getOrDefault("x", 0L)).intValue();
                int ry = ((Long)pData.getOrDefault("y", 0L)).intValue();
                String dir = (String)pData.getOrDefault("direction","down");
                boolean inv = (boolean)pData.getOrDefault("invisible", false);
                boolean sh  = (boolean)pData.getOrDefault("shielded", false);
                boolean sp  = (boolean)pData.getOrDefault("speedActive", false);
                boolean frz = (boolean)pData.getOrDefault("frozen", false);
                boolean cgh = (boolean)pData.getOrDefault("caught", false);

                String team = (String)pData.getOrDefault("team", teamSelected);
                p.team = team;

                p.direction   = dir;
                p.invisible   = inv;
                p.shielded    = sh;
                p.speedActive = sp;
                p.frozen      = frz;
                p.caught      = cgh;

                if(p.isFirstSync){
                    p.x = rx; 
                    p.y = ry;
                    p.targetX = rx; 
                    p.targetY = ry;
                    p.isFirstSync = false;
                    System.out.println("First sync untuk player " + pName + " di posisi (" + rx + ", " + ry + ")");
                } else {
                    if(cgh) {
                        p.targetX = rx;
                        p.targetY = ry;
                        p.x = p.targetX;
                        p.y = p.targetY;
                        System.out.println("Player " + pName + " ditangkap dan dipindahkan ke penjara di posisi (" + p.x + ", " + p.y + ")");

                        if(pName.equals(playerName)){
                            disableKeyHandler();
                        }
                    } else {
                        p.targetX = rx; 
                        p.targetY = ry;
                        System.out.println("Update posisi player " + pName + " ke posisi (" + rx + ", " + ry + ")");
                    }
                }
            }
            // Remove players not in data
            Set<String> existing = data.keySet();
            ArrayList<String> toRemove = new ArrayList<>();
            for(String k : otherPlayers.keySet()) {
                if(!existing.contains(k)) {
                    toRemove.add(k);
                    System.out.println("Menghapus pemain remote: " + k);
                }
            }
            for(String rm : toRemove) {
                otherPlayers.remove(rm);
            }
        });

        fbManager.listenChestPosition(roomCode, chestPos -> {
            if(chestPos != null){
                currentChest = new Chest(this, chestPos[0], chestPos[1]);
                currentChest.opened = (chestPos[2] == 1);
                System.out.println("Chest position updated: (" + chestPos[0] + ", " + chestPos[1] + "), opened: " + currentChest.opened);
            }
        });

        fbManager.isChestPositionSet(roomCode, isSet -> {
            System.out.println("Chest position set? " + isSet);
            if(!isSet){
                System.out.println("Chest belum diset. Menetapkan posisi random.");
                spawnRandomChest();
            } else {
                System.out.println("Chest sudah diset. Tidak perlu menetapkan ulang.");
            }
        });

        fbManager.listenWinState(roomCode, winTeam -> {
            if(winTeam != null && !winTeam.isEmpty()){
                winner = "Tim " + winTeam;
                gameEnded = true;
                JOptionPane.showMessageDialog(this, winner + " Menang!");
                System.exit(0);
            }
        });
    }
    
    private void initializeFortressAndPrison(){
        fortressA = new Rectangle(0, screenHeight / 2 - tileSize * 3, tileSize * 3, tileSize * 6);
        prisonARect = new Rectangle(fortressA.x + fortressA.width, fortressA.y, tileSize * 3, fortressA.height);

        fortressB = new Rectangle(worldWidth - tileSize * 3, screenHeight / 2 - tileSize * 3, tileSize * 3, tileSize * 6);
        prisonBRect = new Rectangle(fortressB.x - tileSize * 3, fortressB.y, tileSize * 3, fortressB.height);
    }

    private void spawnPlayer(){
        if(teamSelected.equals("TeamA")){
            player.x = prisonARect.x + prisonARect.width / 2 - tileSize / 2;
            player.y = prisonARect.y + prisonARect.height / 2 - tileSize / 2;
        } else if(teamSelected.equals("TeamB")){
            player.x = prisonBRect.x + prisonBRect.width / 2 - tileSize / 2;
            player.y = prisonBRect.y + prisonBRect.height / 2 - tileSize / 2;
        }
    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }
    
    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while(gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if(delta >= 1){
                update();
                repaint();
                delta--;
            }
        }
    }
    
    public void update() {
        if(gameEnded){
            return;
        }

        // Update local player
        player.collisionOn = false;
        cChecker.checkTile(player);
        player.update();

        // Update remote players
        for(Player remoteP : otherPlayers.values()) {
            remoteP.update();
        }

        // Update chest
        if(currentChest != null) currentChest.update();

        // Handle chest spawning
        if(chestUsed) {
            if(chestSpawnTime == 0) {
                chestSpawnTime = System.currentTimeMillis() + (2 * 60_000); // 2 menit
            }
            if(System.currentTimeMillis() > chestSpawnTime) {
                spawnRandomChest();
                chestUsed = false;
                chestSpawnTime = 0;
            }
        }

        // Chest interaction (Local)
        if(currentChest != null && keyH.ePressed && !chestUsed) {
            if(distance(player.x, player.y, currentChest.x, currentChest.y) < tileSize) {
                currentChest.openChest(player);
                chestUsed = true;
                fbManager.updateChestPosition(roomCode, currentChest.x, currentChest.y, true);
                System.out.println("Chest dibuka oleh " + playerName);
            }
            keyH.ePressed = false;
        }

        updateCamera(player);

        checkWinCondition();
        checkRoundTimer();

        handleAttackLocal();
    }

    private void handleAttackLocal() {
        boolean canAttack = false;
        long currentTime = System.currentTimeMillis();

        if(keyH.attack1Pressed) {
            if(currentTime > p1AttackCooldownEnd) {
                p1AttackCooldownEnd = currentTime + ATTACK_COOLDOWN;
                canAttack = true;
                System.out.println(playerName + " melakukan serangan.");
            }
            keyH.attack1Pressed = false;
        }

        if(canAttack) {
            // Attack -> penjara + caught=true
            for(Player remoteP : otherPlayers.values()) {
                if(!remoteP.shielded && intersect(player, remoteP) && !remoteP.caught) {
                    // Set ke penjara tim lawan
                    Rectangle penjaraTimLocal = (teamSelected.equals("TeamA")) ? prisonARect : prisonBRect;
                    int px = penjaraTimLocal.x + penjaraTimLocal.width / 2 - tileSize / 2;
                    int py = penjaraTimLocal.y + penjaraTimLocal.height / 2 - tileSize / 2;

                    // Update Firebase: posisi ke penjara + caught=true
                    fbManager.setPlayerAttacked(roomCode, remoteP.playerName, px, py);
                    System.out.println(playerName + " menyerang " + remoteP.playerName + " dan mengirimnya ke penjara.");

                    break;
                }
            }
        }
    }

    // Tile 0 kan grass. Jadi, chest bakal muncul secara random cuma di GRASS. Kalo di wall, nanti gabisa diambil karena obstacle.
    public void spawnRandomChest() {
        ArrayList<int[]> grassTiles = new ArrayList<>();
        for(int col = 0; col < maxWorldCol; col++) {
            for(int row = 0; row < maxWorldRow; row++) {
                if(tileM.mapTileNum[col][row] == 0) {
                    grassTiles.add(new int[]{col, row});
                }
            }
        }
        if(grassTiles.isEmpty()) {
            System.out.println("Tidak ada tile grass untuk menempatkan chest.");
            return;
        }

        Random rand = new Random();
        int idx = rand.nextInt(grassTiles.size());
        int[] tilePos = grassTiles.get(idx);

        currentChest = new Chest(this, tilePos[0] * tileSize, tilePos[1] * tileSize);
        currentChest.opened = false;
        chestUsed = false;
        chestSpawnTime = 0;

        fbManager.updateChestPosition(roomCode, currentChest.x, currentChest.y, false);
        System.out.println("Chest ditempatkan secara random di posisi (" + currentChest.x + ", " + currentChest.y + ").");
    }

    private void updateCamera(Player centerPlayer) {
        int desiredCameraX = centerPlayer.x - screenWidth / 2 + tileSize / 2;
        int desiredCameraY = centerPlayer.y - screenHeight / 2 + tileSize / 2;

        if(desiredCameraX < 0) desiredCameraX = 0;
        if(desiredCameraY < 0) desiredCameraY = 0;

        int maxCameraX = worldWidth - screenWidth;
        int maxCameraY = worldHeight - screenHeight;

        if(desiredCameraX > maxCameraX) desiredCameraX = maxCameraX;
        if(desiredCameraY > maxCameraY) desiredCameraY = maxCameraY;

        cameraX = desiredCameraX;
        cameraY = desiredCameraY;
    }

    private void checkRoundTimer() {
        long elapsed = System.currentTimeMillis() - matchStartTime;
        if(elapsed > roundDuration) {
            endRound();
        }
    }

    private void checkWinCondition() {
        // Pemain lokal menang
        if(teamSelected.equals("TeamA")){
            if(player.x > fortressB.x && player.x < fortressB.x + fortressB.width &&
               player.y > fortressB.y && player.y < fortressB.y + fortressB.height) {
                fbManager.setWinState(roomCode, "A");
                System.out.println("Tim A menang karena menyentuh benteng Tim B.");
            }
        } else if(teamSelected.equals("TeamB")){
            if(player.x > fortressA.x && player.x < fortressA.x + fortressA.width &&
               player.y > fortressA.y && player.y < fortressA.y + fortressA.height) {
                fbManager.setWinState(roomCode, "B");
                System.out.println("Tim B menang karena menyentuh benteng Tim A.");
            }
        }

        // Cek kemenangan untuk pemain remote
        for(Player remoteP : otherPlayers.values()) {
            if(remoteP.team.equals("TeamA")){
                if(remoteP.x > fortressB.x && remoteP.x < fortressB.x + fortressB.width &&
                   remoteP.y > fortressB.y && remoteP.y < fortressB.y + fortressB.height) {
                    fbManager.setWinState(roomCode, "A");
                    System.out.println("Tim A menang karena pemain " + remoteP.playerName + " menyentuh benteng Tim B.");
                }
            } else if(remoteP.team.equals("TeamB")){
                if(remoteP.x > fortressA.x && remoteP.x < fortressA.x + fortressA.width &&
                   remoteP.y > fortressA.y && remoteP.y < fortressA.y + fortressA.height) {
                    fbManager.setWinState(roomCode, "B");
                    System.out.println("Tim B menang karena pemain " + remoteP.playerName + " menyentuh benteng Tim A.");
                }
            }
        }
    }

    public void endRound() {
        fbManager.setWinState(roomCode, "Draw");
        JOptionPane.showMessageDialog(null, "Time's up! Draw!");
        System.exit(0);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;

        tileM.draw(g2);

        drawFortresses(g2);

        drawPrisons(g2);

        if(currentChest != null) {
            currentChest.draw(g2);
        }

        drawPlayer(g2, player);
        for(Player remoteP : otherPlayers.values()) {
            drawPlayer(g2, remoteP);
        }

        drawHUD(g2);

        g2.dispose();
    }

    private void drawFortresses(Graphics2D g2){
        g2.setColor(Color.RED);
        g2.fillRect(fortressA.x - cameraX, fortressA.y - cameraY, fortressA.width, fortressA.height);

        g2.setColor(Color.BLUE);
        g2.fillRect(fortressB.x - cameraX, fortressB.y - cameraY, fortressB.width, fortressB.height);
    }

    private void drawPrisons(Graphics2D g2){
        g2.setColor(new Color(255, 0, 0, 150));
        g2.fillRect(prisonARect.x - cameraX, prisonARect.y - cameraY, prisonARect.width, prisonARect.height);

        g2.setColor(new Color(0, 0, 255, 150));
        g2.fillRect(prisonBRect.x - cameraX, prisonBRect.y - cameraY, prisonBRect.width, prisonBRect.height);
    }

    private void drawHUD(Graphics2D g2){
        long elapsed = System.currentTimeMillis() - matchStartTime;
        long timeLeft = (roundDuration - elapsed) / 1000;
        if(timeLeft < 0) timeLeft = 0;
        String timeStr = String.format("Time Left: %02d:%02d", timeLeft / 60, timeLeft % 60);
        g2.setColor(Color.WHITE);
        g2.drawString(timeStr, screenWidth - 120, 20);

        g2.drawString("P1 Skill: " + (player.heldPowerUp != null ? player.heldPowerUp : "None"), 20, 20);
        if(player.heldPowerUp != null) {
            g2.drawString("Press R to use skill (7s)", 20, 40);
        }
        long timeLeftP1 = (p1AttackCooldownEnd - System.currentTimeMillis());
        if(timeLeftP1 < 0) timeLeftP1 = 0;
        g2.drawString("P1 Attack Cooldown: " + (timeLeftP1 / 1000.0) + "s", 20, 60);
        
        String positionStr = String.format("Position: (%d, %d)", player.x, player.y);
        g2.drawString(positionStr, 20, 105);
    }

    private void drawPlayer(Graphics2D g2, Player p) {
        p.draw(g2);
    }

    private void disableKeyHandler(){
        keyH.setEnabled(false);
        System.out.println("KeyHandler dinonaktifkan karena pemain ditangkap.");
    }

    private boolean intersect(Player p1, Player p2) {
        Rectangle r1 = new Rectangle(p1.x, p1.y, tileSize, tileSize);
        Rectangle r2 = new Rectangle(p2.x, p2.y, tileSize, tileSize);
        return r1.intersects(r2);
    }

    private double distance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
    }
}
