package server;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;

public class RegistrationServer extends RemoteServer
        implements ServerInterface {

    //  param
    private static final int REGPORT = 5252;
    private ServerGUI serverGUI;
    Registry registry;
    ServerInterface stub;
    ConcurrentHashMap<String,JSONObject> userRegistry;

    //  constructor
    public RegistrationServer(ConcurrentHashMap<String, JSONObject> userRegistry, ServerGUI serverGUI) {
        this.userRegistry = userRegistry;
        this.serverGUI=serverGUI;
    }

    //  remote method register
    @SuppressWarnings("unchecked")
    public String register(String userName, String passWord) {
        if (!userRegistry.containsKey(userName)) {

            //  setup user data
            JSONObject userData = new JSONObject();
            userData.put("username",userName);
            userData.put("password",passWord);
            userData.put("friends",new JSONArray());
            userData.put("points",0);

            //  input data
            userRegistry.put(userName, userData);

            serverGUI.showMessage(
                    "user " + userName + " is now registered to the service \n"
            );
            return "Registration successful \n";
        }

        serverGUI.showMessage("User " + userName + " attempted " +
                "to register a second time \n");
        return ("Error: User already registered \n");
    }

    //  create registry, export, bind to port
    public void activateRegistration() {
        try {
            registry = LocateRegistry.createRegistry(REGPORT);
            stub = (ServerInterface) UnicastRemoteObject.exportObject(this, REGPORT);
            registry.rebind("REGISTER", stub);

        } catch (RemoteException rem) {serverGUI.showMessage("Remote exception in ActivateRegistration \n");}
        serverGUI.showMessage(
                "Registration service running at port: " + REGPORT + "\n"
        );
    }

    public void deactivateRegistration() throws RemoteException, NotBoundException {
        UnicastRemoteObject.unexportObject(stub,true);
        UnicastRemoteObject.unexportObject(registry,true);
        registry.unbind("REGISTER");
        serverGUI.showMessage("Remote Objects have been unexported");
    }
}
