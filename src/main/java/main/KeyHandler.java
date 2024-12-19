package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean upPressed, downPressed, leftPressed, rightPressed, ePressed, rPressed;
    
    public boolean attack1Pressed = false;
    
    private boolean enabled = true;

    public void setEnabled(boolean enabled){
        this.enabled = enabled;
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        if(!enabled) return;
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) upPressed = true;
        if (code == KeyEvent.VK_S) downPressed = true;
        if (code == KeyEvent.VK_A) leftPressed = true;
        if (code == KeyEvent.VK_D) rightPressed = true;
        if (code == KeyEvent.VK_E) ePressed = true;
        if (code == KeyEvent.VK_R) rPressed = true;
        if (code == KeyEvent.VK_F) attack1Pressed = true;
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if(!enabled) return;
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_W) upPressed = false;
        if (code == KeyEvent.VK_S) downPressed = false;
        if (code == KeyEvent.VK_A) leftPressed = false;
        if (code == KeyEvent.VK_D) rightPressed = false;
        if (code == KeyEvent.VK_E) ePressed = false;
        if (code == KeyEvent.VK_R) rPressed = false;
        if (code == KeyEvent.VK_F) attack1Pressed = false;
    }
}
