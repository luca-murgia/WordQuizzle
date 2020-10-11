package server;

import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.rmi.NotBoundException;

public class ServerGUI implements Runnable {

    private JTextArea log;

    //  show message method
    public void showMessage(String toShow) {
        log.append(toShow);
    }

    public void run(){

        //  server log

        JTextArea log = new JTextArea();
        log.setEditable(false);
        this.log=log;

        JScrollPane scrollPane = new JScrollPane(log);
        scrollPane.setBounds(13,10,260,230);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));



        ControlServer controlServer = new ControlServer(this);
        Thread server = new Thread(controlServer);
        server.start();

        // button stop
        JButton butStop = new JButton("STOP");
        butStop.setBounds(300,340,100,50);
        butStop.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //  stop actionListener
        butStop.addActionListener(actionEvent -> {
            showMessage("Interrupting execution... \n");
            showMessage("Saving data... \n");
            SaveLibrary.saveUserData(ControlServer.getUserRegistry());
            showMessage("Data saved \n");
            server.interrupt();
            try {
                controlServer.close();
            } catch (IOException e) {
                showMessage("Accept interrupted \n");
            }
            catch (NullPointerException nul){showMessage("Server not found");} catch (NotBoundException e) {
                e.printStackTrace();
            }
        });


        //  frame details
        JFrame fs = new JFrame("WordQuizzle Server");
        fs.setSize(300,300);
        fs.setLayout(null);
        fs.setVisible(true);
        fs.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        fs.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    controlServer.close();
                } catch (IOException | NotBoundException e) { e.printStackTrace(); }
            }
        });



        //  frame components
        fs.add(scrollPane);
    }
}
