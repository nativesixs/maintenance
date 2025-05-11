package ocmaintenance;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.json.JSONObject;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ocmaintenance.controllers.SharedMethods.createEventData;

public class InventoryImport {
    private static final ArrayList<String> wordlist = new ArrayList<>(Arrays.asList(
            "CREATE_EDGE:",
            "CREATE_INSTANCE:",
            "CREATE_DEVICE",
            "CREATE_PLACE",
            "DELETE_EDGE:",
            "DELETE_INSTANCE:",
            "TERMINATE_EDGE:",
            "UPDATE_INSTANCE"
            ));

    public static List<Integer> countWordlist(String word,String data){
        List<Integer> indexes = new ArrayList<>();
        String lowerCaseTextString = data.toLowerCase();
        String lowerCaseWord = word.toLowerCase();
        int index = 0;
        while(index != -1){
            index = lowerCaseTextString.indexOf(lowerCaseWord, index);
            if (index != -1) {
                indexes.add(index);
                index++;
            }
        }
        return indexes;
    }


    public static void main(String data, String inventory, SseEmitter emitter) throws IOException {
        if(!data.isEmpty()){
            Response s= Request.Post(inventory).bodyString(data, ContentType.DEFAULT_TEXT).execute();
            String lines[] = s.returnContent().asString().split("\\r?\\n");
            for(String line:lines){
                emitter.send(SseEmitter.event().data(createEventData(line)));
                System.out.println(line);
            }
        }else{
            emitter.send(SseEmitter.event().data(createEventData("Nutno zadat prikaz.")));
        }
    }

    public static void analyze(String taskField, SseEmitter emitter) throws IOException {
        ArrayList<Integer> all = new ArrayList<>();
        ArrayList<String> type = new ArrayList<>();
        for (String word : wordlist) {
            List<Integer> temp = countWordlist(word, taskField);
            if (temp.size() > 0) {
                for (int j = 0; j < temp.size(); j++) {
                    all.add(temp.get(j));
                    type.add(word);
                }
                temp.clear();
            }
        }
        Collections.sort(all);
        emitter.send(SseEmitter.event().data(createEventData("Nalezeno: "+all.size()+" requests")));
        for (String word : wordlist) {
            int frequency = Collections.frequency(type, word);
            if (frequency > 0) {
                emitter.send(SseEmitter.event().data(createEventData(word+" "+frequency)));
            }
        }
    }

    public static void sendParsed(String taskField, int sizeField, String inventory, SseEmitter emitter) throws IOException {
        ArrayList<Integer> all = new ArrayList<>();
        for (String word : wordlist) {
            List<Integer> temp = countWordlist(word, taskField);
            if (temp.size() > 0) {
                all.addAll(temp);
                temp.clear();
            }
        }
        Collections.sort(all);

        ArrayList<String> requests = new ArrayList<>();
        for(int i=0;i<all.size();i++){
            if (i + 1 < all.size()) {
                requests.add(taskField.substring(all.get(i), all.get(i + 1)));
            } else {
                requests.add(taskField.substring(all.get(all.size() - 1), taskField.length()));
            }
        }


        if(sizeField==0){
            sizeField= requests.size();
        } else if (sizeField<0) {
            emitter.send(SseEmitter.event().data(createEventData("Request size nemuze mit zapornou velikost","error")));
            return;
        }

        int size = sizeField;
        int count = requests.size();
        int limit = count / size;
        if (count % size != 0) {
            limit = limit + 1;
        }

        ArrayList<Integer> errors = new ArrayList<>();
        for (int p = 0; p < limit; p++) {
            String s = "";
            for(int i=0;i<size;i++){
                try{
                    s = s + requests.get(0);
                    requests.remove(0);
                }catch (java.lang.IndexOutOfBoundsException e){

                }
            }
            emitter.send(SseEmitter.event().data(createEventData("Odesilani varky: "+p)));
            emitter.send(SseEmitter.event().data(createEventData(s)));
            Response res= Request.Post(inventory).bodyString(s, ContentType.DEFAULT_TEXT).execute();
            String status = res.returnContent().asString();
            String lines[] = status.split("\\r?\\n");
            emitter.send(SseEmitter.event().data(createEventData("Odeslano: ")));
            for(String line:lines){
                emitter.send(SseEmitter.event().data(createEventData(line)));
            }
            emitter.send(SseEmitter.event().data(createEventData("")));
            if(status.contains("Error:") || status.contains("Warning:")){
                errors.add(p);
            }
        }

        if(errors.size()>0){
            emitter.send(SseEmitter.event().data(createEventData("Nalezeny errory pro varky: ")));
            for (Integer error : errors) {
                emitter.send(SseEmitter.event().data(createEventData("Varka: " + error,"error")));
            }
        }else {
            emitter.send(SseEmitter.event().data(createEventData("Nenalezeny zadne errory varek.")));
        }

    }
}
