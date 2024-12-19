package entity;

import main.GamePanel;
import main.Quiz;
import main.Question;
import javax.swing.JOptionPane;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;

public class Chest extends Entity {
    GamePanel gp;
    Quiz quiz;
    public boolean opened=false;
    BufferedImage closedChest, openChest;

    public Chest(GamePanel gp, int x, int y){
        this.gp=gp;
        this.x=x;
        this.y=y;
        quiz=new Quiz();
        getChestImage();
    }

    private void getChestImage(){
        try{
            closedChest=ImageIO.read(getClass().getResourceAsStream("/chest/chest_closed.png"));
            openChest=ImageIO.read(getClass().getResourceAsStream("/chest/chest_open.png"));
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void update(){}

    public void openChest(Player p){
        if(!opened){
            opened=true; 
            Question q=quiz.getRandomQuestion();
            String[] options={
                "A. "+ q.getOptionA(),
                "B. "+ q.getOptionB(),
                "C. "+ q.getOptionC(),
                "D. "+ q.getOptionD()
            };
            int response=JOptionPane.showOptionDialog(
                null,q.getQuestion(),"Quiz",JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,null,options,options[0]
            );
            String selectedOption="";
            switch(response){
                case 0: selectedOption="A";break;
                case 1: selectedOption="B";break;
                case 2: selectedOption="C";break;
                case 3: selectedOption="D";break;
                default: selectedOption="A";
            }
            if(selectedOption.equals(q.getCorrectOption())){
                String[] pwOptions={"Speed Boost","Invisibility","Shield","Freeze Ray"};
                int pwResponse=new java.util.Random().nextInt(pwOptions.length);
                String awardedPW= pwOptions[pwResponse];

                p.heldPowerUp=awardedPW;
                p.powerUpDuration=System.currentTimeMillis()+7000; 
                JOptionPane.showMessageDialog(null,"Benar! Anda mendapatkan: "+awardedPW+". Tekan R untuk menggunakan.");
            }else{
                JOptionPane.showMessageDialog(null,"Jawaban Salah!");
            }
        }
    }

    public void draw(Graphics2D g2){
        int screenX=x-gp.cameraX;
        int screenY=y-gp.cameraY;
        if(opened){
            g2.drawImage(openChest,screenX,screenY,gp.tileSize,gp.tileSize,null);
        }else{
            g2.drawImage(closedChest,screenX,screenY,gp.tileSize,gp.tileSize,null);
        }
    }
}
