package server;
import org.json.simple.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

@SuppressWarnings("unchecked")
public class Challenge {

    //  param
    private JSONObject meta;
    private Timer timer;
    private boolean isActive,amDone;
    private ControlServer controlServer;
    private String[] questions,answers;
    private HashMap<String, Integer> pointRegister;
    private String sender,receiver;

    //  constructor
    public Challenge(ControlServer controlServer, JSONObject forward){
        this.meta = forward;
        this.isActive = true;
        this.amDone=false;
        this.timer = new Timer();
        this.controlServer=controlServer;
        this.questions = TranslationLibrary.getQuestions(controlServer.getDictionary());
        this.answers = TranslationLibrary.getAnswers(questions);
        this.sender=(String)meta.get("sender");
        this.receiver=(String)meta.get("receiver");

        this.pointRegister = new HashMap<>();
        pointRegister.put(sender,0);
        pointRegister.put(receiver,0);

        setCancelTimer(5000);

    }


    public void beginChallenge(){
        timer.cancel();
        setChallengeTimer(40000);
        setActive(true);
    }
    public void stopChallenge(){
        timer.cancel();
        setActive(false);

    }
    public void setCancelTimer(int delay){
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    setActive(false);
                    meta.put("ID","CANCEL");
                    controlServer.sendDatagram(meta);
                    controlServer.amAvailable(sender);
                    controlServer.amAvailable(receiver);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
        timer = new Timer();
        timer.schedule(task,delay);
    }
    public void finishChallenge(){
        timer.cancel();
        setActive(false);


        int pointsSender = pointRegister.get(sender);
        int pointsReceiver = pointRegister.get(receiver);

        JSONObject result = new JSONObject();
        result.put("ID","RESULT");
        result.put("sender",sender);
        result.put("receiver",receiver);

        if(!pointRegister.get(sender).equals(pointRegister.get(receiver))) {
            if (pointRegister.get(sender) > pointRegister.get(receiver)) {

                result.put("text_sender", "You won aganist " + receiver
                        + "\n" + pointsSender + " | " + pointsReceiver
                );

                result.put("text_receiver", "You lost aganist " + sender
                        + "\n" + pointsReceiver + " | " + pointsSender
                );

                addPoints(sender, 2);
            } else {
                result.put("text_receiver", "You won aganist " + sender
                        + "\n" + pointsReceiver + " | " + pointsSender
                );

                result.put("text_sender", "You lost aganist " + receiver
                        + "\n" + pointsSender + " | " + pointsReceiver
                );

                addPoints(receiver, 2);
            }
        }else{
            result.put("text_sender","It's a draw! (aganist " + sender + ")"
                    + "\n" + pointsReceiver + " | " + pointsSender);
            result.put("text_receiver","It's a draw! (aganist " + receiver + ")"
                    + "\n" + pointsReceiver + " | " + pointsSender);
        }

        controlServer.addPoints_user(sender,pointsSender);
        controlServer.addPoints_user(receiver,pointsReceiver);
        controlServer.amAvailable(sender);
        controlServer.amAvailable(receiver);

        try {
            controlServer.sendDatagram(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        reset();

    }
    public void setChallengeTimer(int delay){
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                finishChallenge();
            }
        };

        timer = new Timer();
        timer.schedule(task,delay);
    }
    public void setActive(boolean value){
        isActive = value;
    }
    public boolean isActive() {
        return isActive;
    }
    public String getQuestion(int index){
        return questions[index];
    }
    public String getAnswer(int index){
        return answers[index];
    }
    public void addPoints(String user, int points){
        pointRegister.put(user,pointRegister.get(user)+points);
    }
    public void reset(){
        pointRegister.put(sender,0);
        pointRegister.put(receiver,0);
        amDone=false;
    }
    public void finish(){
        if (amDone)
            finishChallenge();
        else
            amDone=true;
    }
}
