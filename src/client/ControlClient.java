package client;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import server.ServerInterface;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

@SuppressWarnings("unchecked")
public class ControlClient implements Runnable {

    //  param
    String userName;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private LoginGUI loginGUI;
    private MainMenuGUI mainMenuGUI;
    private ServerInterface server;
    private JSONParser parser;
    private String key;


    //  STARTER FUNCTIONS

    //  run method: starts login interface
    public void run() {
        parser=new JSONParser();
        startLoginGUI();
    }
    //  login starter
    public void startLoginGUI() {
        try {

            //  Starts loginGUI as a new Thread
            LoginGUI loginGUI = new LoginGUI(this);
            this.loginGUI = loginGUI;
            Thread loginThread = new Thread(loginGUI);
            loginThread.start();

            //  initialize socket, binding to port
            Socket socket = new Socket(
                    InetAddress.getLocalHost(), 1500);

            //  writer creation - reader creation
            out = new ObjectOutputStream(
                    socket.getOutputStream());
            in = new ObjectInputStream(
                    socket.getInputStream());
        } catch (IOException ex) {
            loginGUI.show("Unable to reach server");
            loginGUI.close();
        }
    }
    //  main menu starter
    public void startMainMenuGUI() throws IOException, ClassNotFoundException, ParseException {
        mainMenuGUI = new MainMenuGUI(this);
        Thread mainMenu = new Thread(mainMenuGUI);
        mainMenu.start();

        ReaderUdp readerUdp = new ReaderUdp(this);
        Thread readerUdpThread = new Thread(readerUdp);
        readerUdpThread.start();
    }


    //  LOGIN/REGISTRATION

    //  register method
    public String register(String userName, String passWord) {
        try {

            if (server == null) {
                //  registry access
                Registry registry =
                        LocateRegistry.getRegistry(5252);

                //  remote object server lookup
                server = (ServerInterface) registry.lookup(
                        "REGISTER");
            }

            //  registration via username/password
            return server.register(userName, passWord);

        } catch (RemoteException e) {
            System.out.println
                    ("Remote exception " +
                            "nella funzione register nel client");
            return "Remote exception: Registry not found \n";

        } catch (NotBoundException nb) {
            System.out.println("not bound exception in" +
                    " register (client)");
            return "NotBoundException: not corresponding name \n";
        }
    }
    //  login method
    public void login(String userName, String passWord) throws IOException, ClassNotFoundException, ParseException {
        //  request login
        JSONObject loginRequest = new JSONObject();
        loginRequest.put("ID", "LOGIN");
        loginRequest.put("username", userName);
        loginRequest.put("password", passWord);

        JSONObject response = request(loginRequest);

        switch((String)response.get("ID")) {
            case "LOGIN_OK":
                loginGUI.close();
                this.userName = (String)response.get("username");
                startMainMenuGUI();
                break;

            case "LOGIN_ERROR":
                loginGUI.show((String)response.get("error"));
                break;

            default:
                System.out.println("Something went wrong while handling " + response.get("ID"));
        }
    }
    //  logout method
    public void logout(String userName) throws ParseException, IOException, ClassNotFoundException {
        JSONObject request = new JSONObject();
        request.put("ID","LOGOUT");
        request.put("sender",userName);
        request(request);
    }


    //  MAIN FUNCTIONS

    //  request method: sends request to server, returns answer
    public JSONObject request(JSONObject request) throws IOException, ClassNotFoundException, ParseException {
        String requestString = request.toJSONString();
        out.writeObject(requestString);
        out.flush();
        String responseString = ((String) in.readObject());
        return parseMessage(responseString);
    }

    public void requestUdp (JSONObject request) {
        try (DatagramSocket serverSocket = new DatagramSocket()) {
            String msg = request.toJSONString();
            DatagramPacket msgPacket =
                    new DatagramPacket(msg.getBytes(), msg.getBytes().length, InetAddress.getByName("226.226.226.226"), 8888);
            serverSocket.send(msgPacket);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //  update userData
    public JSONObject getUserData() throws IOException, ClassNotFoundException, ParseException {

        JSONObject getUserRequest = new JSONObject();
        getUserRequest.put("ID","GET_USER_DATA");
        getUserRequest.put("username",userName);

        return request(getUserRequest);
    }
    //  add friend
    public void addFriend(String friend) throws IOException, ClassNotFoundException, ParseException {
        if(friend.equals(userName))
            mainMenuGUI.showMessage("You cannot add yourself as a friend");
        else {

            JSONObject friendRequest = new JSONObject();
            friendRequest.put("ID", "FRIEND_REQUEST");
            friendRequest.put("username", userName);
            friendRequest.put("friend", friend);

            JSONObject response = request(friendRequest);

            switch ((String) response.get("ID")) {
                case "ADD_FRIEND_OK":
                    mainMenuGUI.showMessage("You and " + response.get("friend") + " are now friends");
                    break;

                case "USER_NOT_FOUND":
                    mainMenuGUI.showMessage("User not found");
                    break;

                case "ALREADY_FRIENDS":
                    mainMenuGUI.showMessage("You and " + response.get("friend") + " are already friends");
                    break;

                default:
                    System.out.println("Error in addFriend while handling: " + response.get("ID"));
            }
        }
    }
    //  challenge a friend
    public void challenge(String receiver) throws ParseException, IOException, ClassNotFoundException {

        JSONObject challenge = new JSONObject();
        challenge.put("ID","CHALLENGE");
        challenge.put("sender",userName);
        challenge.put("receiver",receiver);

        JSONObject response = request(challenge);

        if(response.get("ID").equals("ERROR"))
            mainMenuGUI.showMessage((String)response.get("text"));
        else
            setChallenge(false);
    }

    public JSONObject question(int questionNumber) throws ParseException, IOException, ClassNotFoundException {
        JSONObject request = new JSONObject();
        request.put("ID","QUESTION");
        request.put("number",String.valueOf(questionNumber));
        request.put("key",key);

        return request(request);
    }

    //  AUXILIARY FUNCTIONS
    public JSONObject parseMessage(String message) throws ParseException {
        return (JSONObject)parser.parse(message);
    }
    //  sends a JsonObject to the UDP pool
    public void sendDatagram(JSONObject toSend) throws IOException {
        String toSendString = toSend.toString();
        try (DatagramSocket serverSocket = new DatagramSocket()) {

            DatagramPacket msgPacket = new DatagramPacket(
                    toSendString.getBytes(),
                    toSendString.getBytes().length,
                    InetAddress.getByName("226.226.226.226"),
                    8888);

            serverSocket.send(msgPacket);
        } catch (SocketException e) { e.printStackTrace(); }
    }

    public void showMessage(String message){
        mainMenuGUI.showMessage(message);
    }
    public void showChallenge(JSONObject forward) throws IOException, ParseException, ClassNotFoundException {
        mainMenuGUI.showChallenge(forward);
    }
    public void setChallenge(boolean value){
        mainMenuGUI.setChallenge(value);
    }
    public void questionMode(){
        mainMenuGUI.questionMode(true);
    }
    public void challengeMode(){
        mainMenuGUI.questionMode(false);
    }
    public void setKey(String newKey){
        key=newKey;
    }
    public void refresh() throws ParseException, IOException, ClassNotFoundException {
        mainMenuGUI.updateList();
    }

}

