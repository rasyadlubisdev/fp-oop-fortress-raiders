/*
REFERENSI:
https://youtube.com/playlist?list=PL_QPQmz5C6WUF-pOQDsbsKbaBZqXj4qSq&si=GnLASAmMvq9N-pCg

Dari playlist tersebut kami mempelajari dasar-dasar dalam membuat game 2D menggunakan Java.
Tidak semua video kami tonton, kami hanya menonton beberapa untuk mengambil key insight nya saja.
Asset yang kami pakai dari video tersebut:
- sprite move untuk player
- tile grass
- tile wall
- tile water

Hal yang kami kembangkan:
- Multiplayer menggunakan Firebase
- Chest -> dibuat random posisinya dan dispawn random lagi setiap 1 menit setelah dibuka.
			Chest tersebut berisi powerups secara random.
- Quiz -> agar bisa mendapatkan powerups dari chest, player harus tepat dalam menjawab kuis dari chest yang dibuka.
- Powerups: invisible, freeze, speed boost, shield
- Game benteng-benteng an -> terinspirasi dari permainan tradisional bentengan.
			Di mana player harus menyentuh lawan agar bisa memasukkan ke penjara tim.
- Map yang unik -> tile wall sebagai obstacle seperti labirin dan tile water sebagai tempat persembunyian.
  			Jadi, jika player menyentuh tile water, maka opacitynya akan menjadi nol. Ini bisa menjadi strategi untuk mengepung lawan atau bersembunyi dari kejaran.
*/

package main;

import java.io.IOException;

import javax.swing.JFrame;

public class Main {
    public static void main(String[] args) throws IOException {
        JFrame window = new JFrame("Fortress Raiders - Firebase Multiplayer");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        LobbyPanel lobby = new LobbyPanel(window);
        window.add(lobby);

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
