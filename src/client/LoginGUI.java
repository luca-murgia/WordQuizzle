package client;

import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LoginGUI implements Runnable {

    //  param
    private ControlClient controlClient;
    private JFrame frame;

    //  constructor
    public LoginGUI(ControlClient controlClient){
        this.controlClient = controlClient;
    }

    //  close method
    public void close(){
        frame.dispose();
    }

    //  show message method
    public void show (String message) {
        JOptionPane.showMessageDialog(frame,message);}

    //  run method
    @Override
    public void run() {

        //  frame creation
        frame = new JFrame("WQ Login");

        //  Label Username
        JLabel labUser = new JLabel("Username");
        labUser.setBounds(50, 30, 150, 20);

        //  Label Password
        JLabel labPass = new JLabel("Password");
        labPass.setBounds(50, 105, 150, 20);

        //  TextField UserName
        final JTextField tfUser = new JTextField();
        tfUser.setBounds(50, 55, 150, 20);
        tfUser.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        //  TextField Password
        final JPasswordField tfPass = new JPasswordField();
        tfPass.setBounds(50, 130, 150, 20);
        tfPass.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // button register
        JButton butReg = new JButton("Register");
        butReg.setBounds(75, 250, 95, 30);
        butReg.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //  register actionlistener
        butReg.addActionListener(e -> {
            String userName = tfUser.getText();
            String passWord = new String(tfPass.getPassword());
            if(userName.isEmpty())
                JOptionPane.showMessageDialog(
                        frame,"insert Username ");
            else
                if(passWord.isEmpty())
                    JOptionPane.showMessageDialog(
                            frame,"Insert Password ");
                else {
                    String result = controlClient.register(userName, passWord);
                    JOptionPane.showMessageDialog(frame, result);
                }
        });

        // button login
        JButton butLog = new JButton("Login");
        butLog.setBounds(75, 200, 95, 30);
        butLog.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        //  login actionlistener
        butLog.addActionListener(e -> {
            String userName = tfUser.getText();
            String passWord = new String(tfPass.getPassword());
            if(userName.isEmpty())
                JOptionPane.showMessageDialog(
                        frame,"insert Username ");
            else
            if(passWord.isEmpty())
                JOptionPane.showMessageDialog(
                        frame,"Insert Password ");
            else {
                try {
                    controlClient.login(userName, passWord);
                } catch (IOException | ClassNotFoundException | ParseException ex) {
                    ex.printStackTrace();
                }
            }
        });

        //  frame details
        frame.setSize(265, 360);
        frame.setLayout(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        //  frame elements
        frame.add(labUser);
        frame.add(labPass);
        frame.add(tfUser);
        frame.add(tfPass);
        frame.add(butLog);
        frame.add(butReg);
    }
}

