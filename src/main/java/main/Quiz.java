package main;

import java.util.ArrayList;
import java.util.Random;

public class Quiz {
    private ArrayList<Question> questions;
    private Random rand;
    
    public Quiz() {
        questions = new ArrayList<>();
        rand = new Random();
        loadQuestions();
    }
    
    private void loadQuestions() {
        questions.add(new Question("Berapa hasil 2+2?", "4", "3", "5", "1", "A"));
        questions.add(new Question("Ibu kota Indonesia?", "Jakarta", "Bandung", "Surabaya", "Medan", "A"));
        questions.add(new Question("Planet terdekat dari Matahari?", "Mars", "Venus", "Merkurius", "Jupiter", "C"));
    }
    
    public Question getRandomQuestion() {
        int index = rand.nextInt(questions.size());
        return questions.get(index);
    }
}
