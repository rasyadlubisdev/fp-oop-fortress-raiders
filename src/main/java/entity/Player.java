package entity;

import main.GamePanel;
import main.KeyHandler;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Player extends Entity {
    public GamePanel gp;
    public KeyHandler keyH;
    public boolean isPlayer2;
    public String playerName;
    public String team;

    public BufferedImage shieldIcon, speedIcon, freezeIcon;
    public String heldPowerUp = null;
    public long powerUpDuration = 0;
    public boolean shielded = false;
    public boolean speedActive = false;
    public boolean invisible = false;
    public boolean frozen = false;
    public boolean invisibilityActive = false;

    public boolean collisionOn = false;
    public boolean caught = false;

    // Interpolasi -> pergerakan mulus
    public int targetX, targetY;
    public boolean isFirstSync = true;
    private int lastX, lastY;

    public Player(GamePanel gp, KeyHandler keyH, boolean isPlayer2, String playerName, String team){
        this.gp = gp;
        this.keyH = keyH;
        this.isPlayer2 = isPlayer2;
        this.playerName = playerName;
        this.team = team;
        setDefaultValues();
        getPlayerImage();
        loadSkillIcons();
    }
    
    public void setDefaultValues(){
        if(!isPlayer2){
            if(team.equals("TeamA")){
                x = gp.tileSize * 2;
                y = gp.tileSize * 2;
            } else { // TeamB
                x = gp.worldWidth - gp.tileSize * 3;
                y = gp.worldHeight - gp.tileSize * 3;
            }
        } else{
            x = gp.tileSize * 2;
            y = gp.tileSize * 2;
        }
        speed = 4;
        direction = "down";
        targetX = x;
        targetY = y;
        powerUpDuration = 0;
        isFirstSync = true;
    }
    
    public void getPlayerImage() {
        try{
        	up1 = ImageIO.read(getClass().getResourceAsStream("/player/boy_up_1.png"));
            up2 = ImageIO.read(getClass().getResourceAsStream("/player/boy_up_2.png"));
            down1 = ImageIO.read(getClass().getResourceAsStream("/player/boy_down_1.png"));
            down2 = ImageIO.read(getClass().getResourceAsStream("/player/boy_down_2.png"));
            left1 = ImageIO.read(getClass().getResourceAsStream("/player/boy_left_1.png"));
            left2 = ImageIO.read(getClass().getResourceAsStream("/player/boy_left_2.png"));
            right1 = ImageIO.read(getClass().getResourceAsStream("/player/boy_right_1.png"));
            right2 = ImageIO.read(getClass().getResourceAsStream("/player/boy_right_2.png"));
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void loadSkillIcons(){
        try{
            shieldIcon = ImageIO.read(getClass().getResourceAsStream("/powerups/shield.png"));
            speedIcon  = ImageIO.read(getClass().getResourceAsStream("/powerups/thunder.png"));
            freezeIcon = ImageIO.read(getClass().getResourceAsStream("/powerups/ice.png"));
        } catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public void update(){
        if(caught){
            System.out.println("Pemain " + playerName + " ditangkap dan berada di penjara.");
            return;
        }
        if(frozen){
            System.out.println("Pemain " + playerName + " dibekukan.");
            return; 
        }

        if(powerUpDuration != 0 && System.currentTimeMillis() > powerUpDuration){
            if(speedActive){
                speedActive = false;
                speed -= 2;
                System.out.println("Speed Boost habis untuk pemain " + playerName);
            }
            if(invisibilityActive){
                invisibilityActive = false;
                invisible = false;
                System.out.println("Invisibility habis untuk pemain " + playerName);
            }
            if(shielded){
                shielded = false;
                System.out.println("Shield habis untuk pemain " + playerName);
            }
            powerUpDuration = 0;
            syncState();
        }

        if(!isPlayer2){
            // Local player
            boolean upP   = keyH.upPressed;
            boolean downP = keyH.downPressed;
            boolean leftP = keyH.leftPressed;
            boolean rightP= keyH.rightPressed;
            boolean rP    = keyH.rPressed;

            boolean moved = false;
            collisionOn = false;

            if(upP){
                direction = "up";
                gp.cChecker.checkTile(this);
                if(!collisionOn){ y -= speed; moved = true; }
            } else if(downP){
                direction = "down";
                gp.cChecker.checkTile(this);
                if(!collisionOn){ y += speed; moved = true; }
            } else if(leftP){
                direction = "left";
                gp.cChecker.checkTile(this);
                if(!collisionOn){ x -= speed; moved = true; }
            } else if(rightP){
                direction = "right";
                gp.cChecker.checkTile(this);
                if(!collisionOn){ x += speed; moved = true; }
            }

            if(upP || downP || leftP || rightP){
                spriteCounter++;
                if(spriteCounter > 12){
                    spriteNum = (spriteNum == 1) ? 2 : 1;
                    spriteCounter = 0;
                }
            }

            if(rP && heldPowerUp != null){
                usePowerUp();
                keyH.rPressed = false;
            }

            // Cek apakah di atas air untuk invisibility
            int col = x / gp.tileSize;
            int row = y / gp.tileSize;
            boolean onWater = (gp.tileM.mapTileNum[col][row] == 2);
            invisible = invisibilityActive || onWater;

            // Sinkronisasi ke Firebase jika ada perubahan state
            if(moved || speedActive || shielded || frozen || invisibilityActive){
                syncState();
            }
        } else {
            if(caught){
                System.out.println("Pemain remote " + playerName + " berada di penjara. Tidak bisa bergerak.");
                return;
            }

            lastX = x;
            lastY = y;

            x += (targetX - x) * 0.2f;
            y += (targetY - y) * 0.2f;

            // Animasi sprite remote
            double distMove = distance(lastX, lastY, x, y);
            if(distMove > 0.1){
                spriteCounter++;
                if(spriteCounter > 12){
                    spriteNum = (spriteNum == 1) ? 2 : 1;
                    spriteCounter = 0;
                }
            }
        }
    }
    
    public void usePowerUp(){
        long now = System.currentTimeMillis();
        switch(heldPowerUp){
            case "Speed Boost":
                speedActive = true;
                speed += 2;
                powerUpDuration = now + 7000;
                break;
            case "Invisibility":
                invisibilityActive = true;
                invisible = true;
                powerUpDuration = now + 7000;
                break;
            case "Shield":
                shielded = true;
                powerUpDuration = now + 7000;
                break;
            case "Freeze Ray":
                freezeOthers();
                break;
        }
        heldPowerUp = null;
        syncState();
    }

    private void syncState(){
        gp.fbManager.updatePlayerState(
            gp.roomCode, playerName, x, y, direction,
            invisible, shielded, speedActive, frozen, caught
        );
    }

    public void freezeOthers(){
        // Freeze skill radius 3 tile
        for(Player remoteP : gp.otherPlayers.values()){
            double dist = distance(x, y, remoteP.x, remoteP.y);
            if(dist < gp.tileSize * 3 && !remoteP.frozen && !remoteP.caught){
                remoteP.frozen = true;
                gp.fbManager.updatePlayerState(
                    gp.roomCode, remoteP.playerName,
                    remoteP.x, remoteP.y, remoteP.direction,
                    remoteP.invisible, remoteP.shielded, remoteP.speedActive,
                    true, remoteP.caught
                );
                // Unfreeze setelah 7 detik
                new java.util.Timer().schedule(new java.util.TimerTask(){
                    @Override
                    public void run(){
                        remoteP.frozen = false;
                        gp.fbManager.updatePlayerState(
                            gp.roomCode, remoteP.playerName,
                            remoteP.x, remoteP.y, remoteP.direction,
                            remoteP.invisible, remoteP.shielded, remoteP.speedActive,
                            false, remoteP.caught
                        );
                    }
                }, 7000);
            }
        }
        syncState();
    }

    private double distance(int x1, int y1, int x2, int y2){
        return Math.sqrt((x1 - x2)*(x1 - x2) + (y1 - y2)*(y1 - y2));
    }

    public void draw(Graphics2D g2){
        // Invisibiltiy
        float alpha = (invisible) ? 0.0f : 1.0f;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        BufferedImage image = null;
        switch(direction){
            case "up":
                image = (spriteNum == 1) ? up1 : up2;
                break;
            case "down":
                image = (spriteNum == 1) ? down1 : down2;
                break;
            case "left":
                image = (spriteNum == 1) ? left1 : left2;
                break;
            case "right":
                image = (spriteNum == 1) ? right1 : right2;
                break;
        }

        int screenX = x - gp.cameraX;
        int screenY = y - gp.cameraY;
        g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

        // Overlay skill dan caught
        if(shielded){
            float overlayAlpha = 0.3f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
            g2.drawImage(shieldIcon, screenX, screenY, gp.tileSize, gp.tileSize, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        if(speedActive){
            float overlayAlpha = 0.3f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
            g2.drawImage(speedIcon, screenX, screenY, gp.tileSize, gp.tileSize, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        if(frozen){
            float overlayAlpha = 0.5f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
            g2.drawImage(freezeIcon, screenX, screenY, gp.tileSize, gp.tileSize, null);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        if(caught){
            float overlayAlpha = 0.4f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayAlpha));
            g2.setColor(Color.BLACK);
            g2.fillRect(screenX, screenY, gp.tileSize, gp.tileSize);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
        }
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        String displayName = "[" + (team.equals("TeamA") ? "A" : "B") + "] " + playerName;
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(displayName);
        g2.setColor(Color.WHITE);
        g2.drawString(displayName, screenX + (gp.tileSize - textWidth) / 2, screenY - 10);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }
}
