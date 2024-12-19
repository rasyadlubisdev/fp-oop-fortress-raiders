package main;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LobbyPanel extends JPanel {

    private JTextField nameField;
    private JTextField roomField;
    private JComboBox<String> teamCombo;
    private JButton startButton;
    private JLabel statusLabel;

    private FirebaseManager fbManager;
    private JFrame window;

    public LobbyPanel(JFrame window) throws IOException {
        this.window = window;
        this.setLayout(null);
        this.setPreferredSize(new Dimension(400, 300));

        fbManager = new FirebaseManager();

        JLabel nameLabel = new JLabel("Nama Player:");
        nameLabel.setBounds(50, 20, 100, 30);
        add(nameLabel);

        nameField = new JTextField();
        nameField.setBounds(160, 20, 180, 30);
        add(nameField);

        JLabel roomLabel = new JLabel("Kode Room:");
        roomLabel.setBounds(50, 70, 100, 30);
        add(roomLabel);

        roomField = new JTextField();
        roomField.setBounds(160, 70, 180, 30);
        add(roomField);

        JLabel teamLabel = new JLabel("Pilih Tim:");
        teamLabel.setBounds(50, 120, 100, 30);
        add(teamLabel);

        teamCombo = new JComboBox<>(new String[]{"A", "B"});
        teamCombo.setBounds(160, 120, 180, 30);
        add(teamCombo);

        startButton = new JButton("Start Game");
        startButton.setBounds(100, 180, 200, 40);
        add(startButton);

        statusLabel = new JLabel("");
        statusLabel.setBounds(50, 240, 300, 30);
        statusLabel.setForeground(Color.RED);
        add(statusLabel);

        startButton.addActionListener(e -> {
            String playerName = nameField.getText().trim();
            String roomCode = roomField.getText().trim();
            String selectedTeam = teamCombo.getSelectedItem().toString().equals("A") ? "TeamA" : "TeamB";
            System.out.println("Start Button clicked. Player: " + playerName + " Room: " + roomCode + " Team: " + selectedTeam);

            if (playerName.isEmpty() || roomCode.isEmpty()) {
                statusLabel.setText("Nama dan Kode Room wajib diisi!");
                return;
            }

            fbManager.getTeamCount(roomCode, selectedTeam, teamCount -> {
                System.out.println("Firebase getTeamCount callback. teamCount = " + teamCount);
                if (teamCount >= 5) {
                    statusLabel.setText("Tim " + selectedTeam + " sudah penuh!");
                } else {
                    System.out.println("Join room...");
                    fbManager.joinRoom(roomCode, playerName, selectedTeam);

                    System.out.println("Creating GamePanel...");
                    GamePanel gp = new GamePanel(playerName, roomCode, selectedTeam, window);

                    System.out.println("Remove LobbyPanel & add gp");
                    window.getContentPane().removeAll();
                    window.add(gp);
                    window.revalidate(); // Revalidate -> memperbarui layout
                    window.repaint(); // Repaint -> menggambar ulang
                    window.pack();
                    window.setLocationRelativeTo(null);

                    System.out.println("startGameThread() dipanggil");
                    gp.startGameThread();
                }
            });
        });


    }
}
