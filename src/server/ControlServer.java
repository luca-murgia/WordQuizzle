package server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.*;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings({"unchecked","all"})
public class ControlServer implements Runnable {

    //  param
    private ArrayList<String> dictionary;
    private static ConcurrentHashMap<String,Boolean> onlineUsers;
    private static ConcurrentHashMap<String, JSONObject> userRegistry;
    private static ConcurrentHashMap<String,Challenge> challengeRegistry;
    private static final Object mutexFriends = new Object();
    private ServerGUI serverGUI;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private RegistrationServer registrationServer;



    //  STARTERS

    //  constructor
    public ControlServer(ServerGUI serverGUI){
        userRegistry = SaveLibrary.loadUserData();
        challengeRegistry = new ConcurrentHashMap<>();
        this.serverGUI=serverGUI;
        onlineUsers = new ConcurrentHashMap<>();
        dictionary = TranslationLibrary.getDictionary();
    }
    // run method
    public void run(){
        //startAutoSave();
        activateRegistration();
        activateLogin();
    }
    //  create reg server, activate registry
    public void activateRegistration(){
        registrationServer = new RegistrationServer(userRegistry,serverGUI);
        registrationServer.activateRegistration();
    }
    //  opening serverSocket, bind on port 1500, threadpool init
    public void activateLogin(){
        try (ServerSocket server = new ServerSocket()) {
            this.serverSocket=server;
            server.setReceiveBufferSize(100);
            server.bind(
                    new InetSocketAddress(
                            InetAddress.getLocalHost(), 1500));

            //  threadpool creation
            ExecutorService es =
                    Executors.newFixedThreadPool(500);
            serverGUI.showMessage("Login service active at port 1500 \n");
            this.threadPool=es;

            //  loop: handler execution
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket client = server.accept();
                    Handler handler = new Handler(client,this);
                    es.execute(handler);

                } catch (IOException e) {
                    es.shutdownNow();
                    registrationServer.deactivateRegistration();
                    //timer.interrupt();
                }
            }
            es.shutdownNow();
            registrationServer.deactivateRegistration();
            //timer.interrupt();
            showMessage("Threadpool is shutting down... \n");
        } catch (IOException | NotBoundException e) {e.printStackTrace();}
    }


    //  MAIN FUNCTIONS

    //  Prints a message inside the server
    public void showMessage(String toShow){
        serverGUI.showMessage(toShow);
    }
    //  close sockets
    public void close() throws IOException, NotBoundException {
        SaveLibrary.saveUserData(userRegistry);
        serverSocket.close();
        registrationServer.deactivateRegistration();
        threadPool.shutdownNow();
    }



    //  ONLINE USERS HANDLING

    //  checks if a specific user is online
    public boolean isOnline(String user){
        return onlineUsers.containsKey(user);
    }
    //  checks if a specific user is available
    public boolean isAvailable(String user){
            return onlineUsers.getOrDefault(user,false);
    }
    //  tells the system that the user is now online and available
    public void amOnline(String user){
        amAvailable(user);
    }
    //  tells the system that the user is now offline
    public void amOffline(String user){
        onlineUsers.remove(user);
    }
    //  tells the system that an user is now unavailable
    public void amUnavailable(String user){
        onlineUsers.put(user,false);
    }
    //  tells the system that an user is now available
    public void amAvailable(String user){
        onlineUsers.put(user,true);
    }


    //  GETTERS/WATCHERS

    //  returns the whole userRegistry
    public static ConcurrentHashMap<String, JSONObject> getUserRegistry() {
        return userRegistry;
    }
    //  returns the requested userData
    public JSONObject getUserData(String userName){
        return userRegistry.get(userName);
    }
    //  checks if user is present inside the user registry
    public boolean containsUserKey(String userName){
        return userRegistry.containsKey(userName);
    }


    //  MODIFIERS

    //  modifies userData adding a friend
    public String addFriend(String userName, String friend){
        if(!userRegistry.containsKey(friend))
            return "USER_NOT_FOUND";
        else {
            synchronized (mutexFriends) {

                //  retrieve userdata of sender/reciever
                JSONObject userData = getUserData(userName);
                JSONObject friendUserData = getUserData(friend);

                //  retrieve friends-array of sender/reciever
                JSONArray friendArray = (JSONArray) userData.get("friends");
                JSONArray freundsArray = (JSONArray) friendUserData.get("friends");

                //  case: array contains friend already
                if (friendArray.contains(friend) || freundsArray.contains(userName))
                    return "ALREADY_FRIENDS";

                else {
                    friendArray.add(friend);
                    freundsArray.add(userName);

                    getUserData(userName).put("friends",friendArray);
                    getUserData(friend).put("friends",freundsArray);

                    return "ADD_FRIEND_OK";
                }
            }
        }
    }

    //  adds a challenge to the challenge registry
    public void addChallenge(JSONObject request){
        String key = (String)request.get("sender") + (String)request.get("receiver");
        Challenge challenge = new Challenge(this, request);
        challengeRegistry.put(key,challenge);
    }

    public void addPoints_user(String username, int toAdd){
        int points = (int)userRegistry.get(username).get("points");
        points += toAdd;
        userRegistry.get(username).put("points",points);

    }

    public void addPoints_challenge(String key, String userName, int toAdd){
        challengeRegistry.get(key).addPoints(userName,toAdd);
    }

    public void beginChallenge(String key) {
        challengeRegistry.get(key).beginChallenge();
    }

    public boolean isActive(String key){
        return ((Challenge)challengeRegistry.get(key)).isActive();
    }

    public void stopChallenge(String key){
        challengeRegistry.get(key).stopChallenge();
    }

    public void finish(String key){
        challengeRegistry.get(key).finish();
    }

    public String getQuestion(String key, int questionNumber){
        return challengeRegistry.get(key).getQuestion(questionNumber);
    }

    public String getAnswer(String key, int questionNumber){
        return challengeRegistry.get(key).getAnswer(questionNumber);
    }

    public JSONObject getRanking(String sender){
        JSONObject result = new JSONObject();

        JSONArray friendList = (JSONArray) userRegistry.get(sender).get("friends");

        for (String next : (Iterable<String>) friendList) {
            result.put(next, getPoints(next));
        }
        result.put(sender,getPoints(sender));

        return result;
    }
       /*
    //  save timer activation method
    public void startAutoSave(){
        SaveTimer saveTimer = new SaveTimer();
        timer = new Thread(saveTimer);
        timer.start();
        showMessage("Autosave: ON \n");
    }
    */


    //   AUXILIARY FUNCTIONS

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
    public ArrayList<String> getDictionary(){return dictionary;}

    public int getPoints(String user){
        return (Integer) userRegistry.get(user).get("points");
    }

}
