package server;

import org.json.simple.JSONObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SaveLibrary {


    //  save object function (java IO)
    public static void save(Object serObj, String fileName){
        try{
            FileOutputStream fos = new FileOutputStream(fileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(serObj);
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //  load saved object
    public static Object retrieve(String fileName) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(fileName);
        ObjectInputStream ois = new ObjectInputStream(fis);
        return ois.readObject();
    }

    //  saveUserData inside userMemory
    public static void saveUserData(Object serObj){
        String fileName = "UserMemory";
        save(serObj,fileName);
        System.out.println("userdata saved");
    }

    @SuppressWarnings("unchecked")
    //  load userData from userMemory
    public static ConcurrentHashMap<String, JSONObject> loadUserData() {
        try {
            return (ConcurrentHashMap<String, JSONObject>)retrieve("UserMemory");
        }catch ( IOException io){
            return new ConcurrentHashMap<>();
        }catch (ClassNotFoundException cnf){
            System.out.println("class not found in retrieveUsers");
            return new ConcurrentHashMap<>();
        }
    }

    //  sorting JSON auxiliary function
    private static class MyModel {
        String key;
        int value;

        MyModel(String key, int value) {
            this.key = key;
            this.value = value;
        }
    }
    public static String sort(JSONObject ranking) {
        List<MyModel> list = new ArrayList<>();

        //  parsing json
        Iterator<?> keys = ranking.keySet().iterator();
        MyModel objnew;
        while (keys.hasNext()) {
            String key = (String) keys.next();
            objnew = new MyModel(key, Integer.parseInt(ranking.get(key).toString()) );
            list.add(objnew);
        }

        //sorting the values
        list.sort((o1, o2) -> Integer.compare(o2.value, o1.value));
        //print out put
        StringBuilder sortedRanking = new StringBuilder();
        for (MyModel m : list) {
            sortedRanking.append(m.key);
            sortedRanking.append(":   (");
            sortedRanking.append(m.value);
            sortedRanking.append(")");
            sortedRanking.append("\n");
        }

        return sortedRanking.toString();
    }

}
