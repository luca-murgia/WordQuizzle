package client;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import javax.swing.*;

@SuppressWarnings("unchecked")
public class ReaderUdp implements Runnable
{

    //  param
    int PORT = 8888;
    private ControlClient controlClient;
    String user;

    //  constructor
    public ReaderUdp(ControlClient controlClient)
    {
        this.user=controlClient.userName;
        this.controlClient=controlClient;
    }

    public boolean isForMe(JSONObject messageJson){
        System.out.println(messageJson.toJSONString());
        return (messageJson.get("sender").equals(user)
                || messageJson.get("receiver").equals(user));
    }
    public boolean imReceiver(JSONObject messageJson){
        return messageJson.get("receiver").equals(user);
    }
    public boolean imSender(JSONObject messageJson){
        return messageJson.get("sender").equals(user);
    }

    //  run method
    public void run()
    {
        InetAddress inetAddress = null;
        try {
            String adr = "226.226.226.226";
            inetAddress = InetAddress.getByName(adr);
        } catch (UnknownHostException e)
        {
            e.printStackTrace();
        }

        //  bytebuffer creation
        byte[] buf = new byte[256];

        //  connection to service, reading messages and printing inside textArea
        try (MulticastSocket clientSocket = new MulticastSocket(PORT))
        {
            assert inetAddress != null;
            clientSocket.joinGroup(inetAddress);
            while (!Thread.interrupted())
            {
                DatagramPacket msgPacket = new DatagramPacket(buf, buf.length);
                clientSocket.receive(msgPacket);

                String message = new String(buf, 0, msgPacket.getLength());
                System.out.println("readerUdp: " + message);
                JSONObject messageJson = controlClient.parseMessage(message);

                if(isForMe(messageJson)) {

                    switch ((String)messageJson.get("ID")) {
                        case "FORWARD":
                            if(imReceiver(messageJson)) {
                                controlClient.setKey((String) messageJson.get("sender") + (String)messageJson.get("receiver"));
                                controlClient.showChallenge(messageJson);
                            }else
                                controlClient.setKey((String) messageJson.get("sender") + (String)messageJson.get("receiver"));
                            break;
                        case "CANCEL":
                                controlClient.showMessage("Challenge refused");
                                controlClient.setChallenge(true);
                                break;

                        case "BEGIN":
                            controlClient.showMessage("Begin!");
                            controlClient.questionMode();
                            break;

                        case "RESULT":
                            if(imSender(messageJson)) {
                                controlClient.showMessage((String) messageJson.get("text_sender"));
                            }else{
                                controlClient.showMessage((String) messageJson.get("text_receiver"));
                            }
                            controlClient.refresh();
                            break;
                        default:
                            System.out.println("ready!");
                    }
                    }
                }
            } catch (ParseException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
