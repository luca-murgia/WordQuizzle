package client;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import server.SaveLibrary;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Vector;

@SuppressWarnings("unchecked")
public class MainMenuGUI implements Runnable {

    //  param
    private JSONObject userData;
    private JFrame frame;
    private JList<String> friends;
    private ControlClient controlClient;
    private JButton butChallenge,butQuestion;
    private JLabel labName;
    private int questionNumber,points;


    //  STARTER FUNCTIONS

    //  constructor
    public MainMenuGUI(ControlClient controlClient) throws IOException, ClassNotFoundException, ParseException {
        this.userData = controlClient.getUserData();
        this.controlClient = controlClient;
        questionNumber=0;
    }
    //  run method
    public void run() {

        String username = (String)userData.get("username");
        points = Integer.parseInt(userData.get("points").toString());

        //  frame creation
        frame = new JFrame("Word Quizzle ");

        //  name label
        labName = new JLabel(username + ": " + points);
        labName.setBounds(150,15,100,25);

        JLabel labFriends = new JLabel("Friend List");
        labFriends.setBounds(25,15,100,25);

        //  friendList
        friends = new JList<>();
        JScrollPane paneFriends = new JScrollPane(friends);
        paneFriends.setBorder((BorderFactory.createLineBorder(Color.BLACK)));
        paneFriends.setBounds(25,50,100,275);

        //  updateList()
        try {
            updateList();
        } catch (IOException | ClassNotFoundException | ParseException e) {
            e.printStackTrace();
        }


        //  button add
        JButton butAdd = new JButton("Add friend");
        butAdd.setBounds(150,50, 100,50);
        butAdd.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //  actionlistener di add
        butAdd.addActionListener(e->{
            String friend = JOptionPane.showInputDialog(frame,"Input friend's username");
            try {
                controlClient.addFriend(friend);
                updateList();
            } catch (IOException | ClassNotFoundException | ParseException ex) {
                ex.printStackTrace();
            }
        });

        //  button challenge
        butChallenge = new JButton("Challenge");
        butChallenge.setBounds(150,125, 100,50);
        butChallenge.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        butChallenge.addActionListener(e->{
            if(friends.isSelectionEmpty())
                showMessage("Select a friend to challenge");
            else{
                String receiver = friends.getSelectedValue();
                try {
                    controlClient.challenge(receiver);
                } catch (ParseException | IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        });


        //  button question
        butQuestion = new JButton("Question");
        butQuestion.setBounds(150,125, 100,50);
        butQuestion.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        butQuestion.addActionListener(e->{
            if(questionNumber<3) {
                try {
                    JSONObject question = controlClient.question(questionNumber);
                    if(question.get("ID").equals("ERROR")){
                        showMessage((String) question.get("text"));
                        questionMode(false);
                    }
                    else {
                        showQuestion(controlClient.question(questionNumber));
                        questionNumber++;
                    }
                } catch (ParseException | IOException | ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }else {
                questionMode(false);
                questionNumber=0;
            }
        });

        //  button ranking
        JButton butRanking = new JButton("Ranking");
        butRanking.setBounds(150,200, 100,50);
        butRanking.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //  actionlistener ranking
        butRanking.addActionListener(e->{
            JSONObject request = new JSONObject();
            request.put("ID","RANKING");
            request.put("sender",username);
            try {
                JSONObject ranking = controlClient.request(request);
                JOptionPane.showMessageDialog(frame,SaveLibrary.sort(ranking));
            } catch (IOException | ClassNotFoundException | ParseException ex) {
                ex.printStackTrace();
            }
        });

        //  button update
        JButton butUpdate = new JButton("Refresh");
        butUpdate.setBounds(150,275, 100,50);
        butUpdate.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //  actionlistener update
        butUpdate.addActionListener(e-> {
            try {
                updateList();
                labName.setText(username + ": " + points);

            } catch (IOException | ClassNotFoundException | ParseException ex) {
                ex.printStackTrace();
            }
        });

        //  update timer
        /*Timer timer = new Timer(3000,e->{
            try {
                updateList();
            } catch (IOException | ClassNotFoundException ex) {
                ex.printStackTrace();
            }
        });
        timer.start();
*/

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    controlClient.logout(username);
                    frame.dispose();
                } catch (IOException | ClassNotFoundException | ParseException e) { e.printStackTrace(); }
            }
        });


        //  frame details
        frame.setSize(290, 400);
        frame.setLayout(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        //  frame elements
        frame.add(butQuestion);
        frame.add(labName);
        frame.add(labFriends);
        frame.add(paneFriends);
        frame.add(butAdd);
        frame.add(butChallenge);
        frame.add(butUpdate);
        frame.add(butRanking);
        questionMode(false);
    }


    //  AUXILIARY FUNCTIONS

    //  shows a message
    public void showMessage(String message){
        JOptionPane.showMessageDialog(frame,message);
    }
    //  Shows a forwarded challenge request
    public void showChallenge(JSONObject forward) throws IOException, ParseException, ClassNotFoundException {
        if(
            JOptionPane.showConfirmDialog(frame,
                "Begin game with '" + forward.get("sender") + "'?",
                "You've been challenged",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
                    forward.put("ID","READY"); }
        else{forward.put("ID","CANCEL");}
        controlClient.request(forward);

    }

    public void showQuestion(JSONObject question) throws IOException, ParseException, ClassNotFoundException {
        question.put("answer",JOptionPane.showInputDialog(frame,question.get("text")));
        question.put("ID","ANSWER");

        JSONObject answer = controlClient.request(question);

        if(answer.get("ID").equals("ERROR"))
            showMessage((String) answer.get("text"));

    }
    //  update friendList
    public void updateList() throws IOException, ClassNotFoundException, ParseException {
        JSONObject userData = controlClient.getUserData();
        JSONArray freunds = (JSONArray) userData.get("friends");
        points = Integer.parseInt(userData.get("points").toString());
        Vector<String> frendos = new Vector<>();
        for(Object f:freunds){
            String ff = f.toString();
            frendos.add(ff);
        }
        friends.setListData(frendos);
    }

    public void setChallenge (boolean value){
        butChallenge.setEnabled(value);
    }

    public void questionMode(boolean toggle){
        if(toggle){
            butChallenge.setVisible(false);
            butQuestion.setVisible(true);
        }else{
            butChallenge.setVisible(true);
            setChallenge(true);
            butQuestion.setVisible(false);
        }

    }
}
