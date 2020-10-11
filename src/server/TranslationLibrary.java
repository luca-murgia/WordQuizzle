package server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class TranslationLibrary {
    static JSONParser parser = new JSONParser();


    public static String translate(String word) {
        try {

            //Richiesta GET al server per la traduzione
            URL url = new URL("https://api.mymemory.translated.net/get?q=" + word + "!&langpair=it|en");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            inputLine = in.readLine();

            JSONObject parsed = (JSONObject) parser.parse(inputLine);
            JSONObject jsonChild = (JSONObject) parsed.get("responseData");
            in.close();
            return (String) (jsonChild.get("translatedText"));

        } catch (IOException | ParseException ex) {
            ex.printStackTrace();
        }
        return "ERROR";
    }

    public static ArrayList<String> getDictionary() {
        try {
            //Lettura del dictionary dal file
            BufferedReader bufferedReader = new BufferedReader(
                    new FileReader(new File("dictionary")));

            ArrayList<String> dictionary = new ArrayList<>();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                dictionary.add(line);
            }

            bufferedReader.close();
            return dictionary;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static String[] getQuestions(ArrayList<String> dictionary){
        int random = (int)(Math.random()*dictionary.size())%dictionary.size();

        String[] questions = new String[3];

        for(int i=0;i<3;i++) {
            String word = (String) dictionary.get((random + i)%dictionary.size()).toLowerCase();
            questions[i]=word;
        }

        return questions;
    }

    public static String[] getAnswers(String[] questions){
        String[] answers = new String[questions.length];
        for(int i=0;i<questions.length;i++)
            answers[i] = translate(questions[i])
                    .toLowerCase()
                    .replaceAll("!", "")
                    .replaceAll("\\.", "")
                    .replaceAll("-", "");
        return answers;
    }

    public static void main(String[] args){

        ArrayList<String> dictionary = getDictionary();
        String[] questions = getQuestions(dictionary);
        String[] answers = getAnswers(questions);
        System.out.println(Arrays.toString(questions));
        System.out.println(Arrays.toString(answers));


    }


}


