package entity;

import main.GamePanel;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class PowerUp extends Entity {
    GamePanel gp;
    public String type;
    public boolean active = true;
    BufferedImage image;
    
    public PowerUp(GamePanel gp, int x, int y, String type) {
        this.gp = gp;
        this.x = x;
        this.y = y;
        this.type = type;
        loadImage();
    }
    
    public void loadImage() {
        try {
            switch(type) {
                case "Speed Boost":
                    image = ImageIO.read(getClass().getResourceAsStream("/powerups/thunder.png"));
                    break;
                case "Shield":
                    image = ImageIO.read(getClass().getResourceAsStream("/powerups/shield.png"));
                    break;
                case "Freeze Ray":
                    image = ImageIO.read(getClass().getResourceAsStream("/powerups/ice.png"));
                    break;
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    public void update() {}
    
    public void draw(Graphics2D g2) {
        if(active) {
            int screenX = x - gp.cameraX;
            int screenY = y - gp.cameraY;
            g2.drawImage(image, screenX, screenY, gp.tileSize, gp.tileSize, null);
        }
    }
}
