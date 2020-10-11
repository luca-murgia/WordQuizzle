package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {

    //  remote function register
    String register(String userName, String passWord)
            throws RemoteException;
}
