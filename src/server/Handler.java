package server;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

@SuppressWarnings("all")
public class Handler implements Runnable
{
    //  param
    private ControlServer controlServer;
    private String userName;
    ObjectInputStream in;
    ObjectOutputStream out;

    //	constructor
    public Handler(Socket client, ControlServer controlServer) throws IOException {
        this.controlServer=controlServer;

        //  initialization writer-reader
        in= new ObjectInputStream(
                client.getInputStream());
        out= new ObjectOutputStream(
                client.getOutputStream());
    }

    //  sends a message to the handled client
    public void answer(JSONObject response) throws IOException {
        out.writeObject(response.toJSONString());
        out.flush();
    }

    //  handles a login request
    public JSONObject handleLogin(JSONObject loginRequest){
        String userNameRequested = (String)loginRequest.get("username");
        String passWordRequested = (String)loginRequest.get("password");

        JSONObject badResult = new JSONObject();

        //  case: non registered user
        if(!controlServer.containsUserKey(userNameRequested)){
            badResult.put("ID","LOGIN_ERROR");
            badResult.put("error","User " + userNameRequested + " is not registered to the service");
            return badResult;
        }

        //  case: registered user
        JSONObject userData =  controlServer.getUserData(userNameRequested);

        //  compare userName & passWord with the ones in the user register
        if(userData.get("password").equals(passWordRequested)){
            userData.put("ID","LOGIN_OK");

            if(controlServer.isOnline(userNameRequested)) {
                badResult.put("ID", "LOGIN_ERROR");
                badResult.put("error","User " + userNameRequested + " is already online");
                return badResult;
            }
            else {
                controlServer.amOnline(userNameRequested);
                userName = (String) userData.get("username");
                controlServer.showMessage("User '" + userName + "' just logged in \n");
                return userData;
            }
        }

        badResult.put("ID","LOGIN_ERROR");
        badResult.put("error","Incorrect Password");
        return badResult;
    }

    //  generic request handle method
    public JSONObject handleRequest(JSONObject request) throws IOException {

        JSONObject error = new JSONObject();
        error.put("ID","ERROR");

        switch ((String)request.get("ID")){

            case "LOGIN":
                return handleLogin(request);

            case "LOGOUT":
                String user = (String)request.get("sender");
                controlServer.amOffline(user);
                controlServer.showMessage("User '" + user + "' just logged out \n");

                Thread.currentThread().interrupt();
                return request;

            case "GET_USER_DATA":
                String username = (String) request.get("username");
                request = controlServer.getUserData(username);
                request.put("ID","UPDATED");

                // fino a qui è corretto
                return request;

            case "FRIEND_REQUEST":
                String id = controlServer.addFriend((String)request.get("username"),(String)request.get("friend"));
                request.put("ID",id);
                return request;

            case "CHALLENGE":
                if(!controlServer.isAvailable((String)request.get("sender"))){
                    request.put("ID","ERROR");
                    request.put("text","please wait...");
                }
                else {
                    if (controlServer.isAvailable((String) request.get("receiver"))) {

                        controlServer.amUnavailable((String) request.get("sender"));
                        controlServer.amUnavailable((String) request.get("receiver"));

                        request.put("ID", "CANCEL");
                        controlServer.addChallenge(request);

                        request.put("ID", "FORWARD");
                        controlServer.sendDatagram(request);

                        return request;
                    } else
                        request.put("ID", "ERROR");
                    request.put("text", "User '" + request.get("receiver") + "' is unavailable right now");
                }
                return request;

            case "CANCEL":
                if(controlServer.isActive((String) request.get("sender") + (String)request.get("receiver"))) {
                    controlServer.stopChallenge((String) request.get("sender") + (String) request.get("receiver"));

                    controlServer.amAvailable((String) request.get("sender"));
                    controlServer.amAvailable((String) request.get("receiver"));

                    controlServer.sendDatagram(request);
                    break;
                }
                else{
                    request.put("ID","ERROR");
                    return request;
                }

            case "READY":
                request.put("ID","BEGIN");

                if(controlServer.isActive(
                        (String) request.get("sender") + (String)request.get("receiver")))
                {
                    controlServer.beginChallenge((String) request.get("sender") + (String)request.get("receiver"));

                    controlServer.sendDatagram(request);
                }

                else{
                    request.put("ID","ERROR");
                    return request;
                }

                break;

            case "QUESTION":
                int questionNumber = Integer.parseInt((String) request.get("number"));

                if(controlServer.isActive((String) request.get("key"))) {
                    String question = controlServer.getQuestion((String) request.get("key"), questionNumber);
                    request.put("text", question);
                    return request;
                }
                else{
                    request.put("ID","ERROR");
                    request.put("text","Time is up");
                }

            case "ANSWER":
                /*verifico che la risposta sia corretta, aggiungo i punti al client specifico*/
                if(controlServer.isActive((String)request.get("key"))) {

                    if(request.get("answer")==null)
                        return request;

                    if (((String) request.get("answer")).toLowerCase().equals(
                            controlServer.getAnswer((String) request.get("key"), Integer.parseInt((String) request.get("number")))))
                        controlServer.addPoints_challenge((String) request.get("key"), userName, 3);
                    else
                        controlServer.addPoints_challenge((String) request.get("key"),userName,-2);
                }
                else{
                    request.put("ID","ERROR");
                    request.put("text","Time is up");
                }
                if (request.get("number").equals("2")) {
                    controlServer.finish((String) request.get("key"));
                }
                return request;

            case "AVAILABLE":
                controlServer.amAvailable((String) request.get("sender"));
                break;

            case "UNAVAILABLE":
                controlServer.amUnavailable((String) request.get("sender"));
                break;

            case "RANKING":
                return controlServer.getRanking((String)request.get("sender"));

            default:
                controlServer.showMessage("unknown request");
                break;
        }return error;
    }


    //	handle loop
    @Override
    public void run()
    {
        JSONParser parser = new JSONParser();
        try {
            while(!Thread.currentThread().isInterrupted()) {
                JSONObject request = (JSONObject) parser.parse((String) in.readObject());
                JSONObject response = handleRequest(request);
                answer(response);
            }
            controlServer.showMessage("thread " + Thread.currentThread() + " is done handling \n");
        }

        catch (IOException | ParseException e){
            controlServer.showMessage
                    ("IOException in handler \n");
            e.printStackTrace();
        } catch (ClassNotFoundException cnf){
            cnf.printStackTrace();}
        catch (NullPointerException nul){
            controlServer.showMessage("Handler " + Thread.currentThread().getId() + " : NPE \n");
            nul.printStackTrace();
        }
    }
}
