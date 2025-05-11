package ocmaintenance.controllers;

import com.apollographql.apollo.ApolloClient;
import com.google.gson.Gson;
import java.io.Serializable;
import ocmaintenance.*;
import org.apache.http.annotation.Experimental;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.Map.entry;

public class SharedMethods {

    public static String terminateRequestDynamic(ArrayList<String> data, String type, String inventory, ApolloClient apolloClient, SseEmitter emitter, boolean noseg) throws ExecutionException, InterruptedException, IOException {
        int dataSize=data.size();
        int deletedSize=0;
        //check if internalID even exists
        for(int i=0;i<dataSize;i++){
            CompletableFuture<VerifyEdgeQuery.Data> respons = QueryHandler.execute(new VerifyEdgeQuery(Collections.singletonList(data.get(i))), apolloClient);
            ArrayList<String> temp = new ArrayList<>();
            for(int j=0;j<respons.get().instancesSet().edgeInstance().size();j++){
                try {
                    temp.add(respons.get().instancesSet().edgeInstance().get(j).internalId());
                }catch (java.lang.IndexOutOfBoundsException e){}
            }
            if(!temp.contains(data.get(i))){
                emitter.send(SseEmitter.event().data(createEventData("Vazba: "+data.get(i)+" nebyla nalezena jako aktivní vazba, odvazbení neproběhne.")));
                return "nok";
            }
            temp.clear();
            String deledge="TERMINATE_EDGE:\n{ \"id\": \"" +data.get(i)+ "\" ,\"exists_to\": \"now\"}";
            temp.clear();

            Response res= Request.Post(inventory).bodyString(deledge, ContentType.DEFAULT_TEXT).execute();
            String status = res.returnContent().asString();
            String lines[] = status.split("\\r?\\n");
            emitter.send(SseEmitter.event().data(createEventData("Odeslano: \n")));
            for(String line:lines){
                emitter.send(SseEmitter.event().data(createEventData(line)));
            }
            emitter.send(SseEmitter.event().data(createEventData("\n\n")));
            if(status.contains("Error:")){
                emitter.send(SseEmitter.event().data(createEventData("Vazba: "+data.get(i)+" nebyla úspěšně ukončena")));
                return "nok";
            }else {
                emitter.send(SseEmitter.event().data(createEventData("Ukončena vazba: "+data.get(i))));
            }
        }
        if(deletedSize==dataSize && dataSize!=0 && noseg){
            emitter.send(SseEmitter.event().data(createEventData("Segment: "+type+" úspěšně ukončen")));
        }
        return "ok";
    }

    public static StringBuilder terminateRequest(ArrayList<String> data, String type, String inventory,ApolloClient apolloClient,boolean safe,boolean noseg) throws IOException, ExecutionException, InterruptedException {
        StringBuilder log = new StringBuilder();
        int dataSize=data.size();
        int deletedSize=0;
        //check if internalID even exists
        for(int i=0;i<dataSize;i++){
            CompletableFuture<VerifyEdgeQuery.Data> respons = QueryHandler.execute(new VerifyEdgeQuery(Collections.singletonList(data.get(i))), apolloClient);
            ArrayList<String> temp = new ArrayList<>();
            for(int j=0;j<respons.get().instancesSet().edgeInstance().size();j++){
                try {
                    temp.add(respons.get().instancesSet().edgeInstance().get(j).internalId());
                }catch (java.lang.IndexOutOfBoundsException e){}
            }
            if(!temp.contains(data.get(i))){
                log.append("Vazba: ").append(data.get(i)).append(" nebyla nalezena jako aktivní vazba, odvazbení neproběhne.");
                return log;
            }
            temp.clear();

            String deledge="TERMINATE_EDGE:\n{ \"id\": \"" +data.get(i)+ "\" ,\"exists_to\": \"now\"}";
//            log.append("\n\n");
//            String rep = String.valueOf(Request.Post(inventory).bodyString(deledge, ContentType.DEFAULT_TEXT).execute().returnResponse().getStatusLine().getStatusCode());
            Response rep= Request.Post(inventory).bodyString(deledge, ContentType.DEFAULT_TEXT).execute();
            temp.clear();

            String status = rep.returnContent().asString();
            String lines[] = status.split("\\r?\\n");
            if(status.contains("Error:")){
                log.append("Vazba: ").append(data.get(i)).append(" nebyla úspěšně ukončena.\n");
            }else{
                log.append("Ukončena vazba: ").append(data.get(i));
            }

            //todo bool safe temp disabled, fix if needed (not likely)
//            if(!safe) {
//                if (rep.equals("200")) {
//                    deletedSize++;
//                } else {
//                    log.append("Vazba: ").append(data.get(i)).append(" nebyla úspěšně ukončena. Status code: ").append(rep);
//                }
//                log.append("Ukončena vazba: ").append(data.get(i));
//            }else {
//                //EXTRA SAFE, slower?
//                CompletableFuture<VerifyEdgeQuery.Data> response = QueryHandler.execute(new VerifyEdgeQuery(Collections.singletonList(Collections.singletonList(data.get(i)).toString())), apolloClient);
//                for(int j=0;j<response.get().instancesSet().edgeInstance().size();j++){
//                    try {
//                        temp.add(response.get().instancesSet().edgeInstance().get(j).internalId());
//                    }catch (java.lang.IndexOutOfBoundsException e){}
//                }
//                if(!temp.contains(data.get(i))){
//                    deletedSize++;
//                    log.append("Ukončena vazba: ").append(data.get(i));
//                }else {
//                    log.append("Vazba: ").append(data.get(i)).append(" nebyla úspěšně ukončena.");
//                }
//                temp.clear();
//            }

        }
        if(deletedSize==dataSize && dataSize!=0 && noseg){
            log.append(" / Segment: ").append(type).append(" úspěšně ukončen");
        }
        return log;
    }

    public static String validateModbus(String ckod, String expectedModbus, ApolloClient apolloClient) throws ExecutionException, InterruptedException {
        CompletableFuture<GetModbusQuery.Data> response = QueryHandler.execute(new GetModbusQuery(Collections.singletonList(ckod)), apolloClient);
        if(!response.get().instances().get(0).modbus().get(0).normalizedValue().toString().equals(expectedModbus)){
            return "vložen ckod: "+ckod+" s přiřazeným modbusem: "+response.get().instances().get(0).modbus().get(0).normalizedValue()+" v poli pro lvm s modbusem: "+expectedModbus+", lvm ckod nok, přiřazení neproběhne\n";
        }else {
            return "ok";
        }
    }

    public static String validateCkod(String ckod, String expectedType, String showAs, ApolloClient apolloClient) throws ExecutionException, InterruptedException {
        try {
            CompletableFuture<CheckDeletedCkodQuery.Data> response = QueryHandler.execute(new CheckDeletedCkodQuery(Collections.singletonList(ckod)), apolloClient);
            try {
                CompletableFuture<IdentifyQuery.Data> res = QueryHandler.execute(new IdentifyQuery(Collections.singletonList(ckod)), apolloClient);
                switch (expectedType) {
                    case "lvm":
                        try {
                            if (res.get().devices().get(0).element().parent().id().equals("device:device.lvm")) {
                                return "ok";
                            } else {
                                return "nok";
                            }
                        } catch (java.lang.IndexOutOfBoundsException e) {
                            return showAs + " ckod nebyl nalezen.\n";
                        }
                    case "dpu":
                        try {
                            if (res.get().devices().get(0).element().parent().id().equals("device:device.dpu")) {
                                return "ok";
                            } else {
                                return "nok";
                            }
                        } catch (java.lang.IndexOutOfBoundsException e) {
                            return showAs + " ckod nebyl nalezen.\n";
                        }

                    case "plato":
                        try {
                            response.get().ckod().get(0).coreElement().id().equals(expectedType);
                            if (res.get().instances().get(0).element().coreElement().id().equals("plato")) {
                                return "ok";
                            } else {
                                return "nok";
                            }
                        } catch (java.lang.IndexOutOfBoundsException e) {
                            return showAs + " ckod nebyl nalezen.\n";
                        }
                    case "simcard":
                        try {
                            response.get().simcard().get(0).coreElement().id().equals(expectedType);
                            if (res.get().sim().get(0).element().coreElement().id().equals("simcard")) {
                                return "ok";
                            } else {
                                return "nok";
                            }
                        } catch (java.lang.IndexOutOfBoundsException e) {
                            return showAs + " ckod nebyl nalezen.\n";
                        }
                    case "place":
                        try {
                            response.get().place().get(0).coreElement().id().equals(expectedType);
                            if (res.get().places().get(0).element().coreElement().id().equals("place")) {
                                return "ok";
                            } else {
                                return "nok";
                            }
                        } catch (java.lang.IndexOutOfBoundsException e) {
                            return showAs + " ckod nebyl nalezen.\n";
                        }
                    case "equipment":
                        try {
                            response.get().ckod().get(0).coreElement().id().equals(expectedType);
                            if (res.get().instances().get(0).element().coreElement().id().equals("equipment")) {
                                return "ok";
                            } else {
                                return "nok";
                            }
                        } catch (java.lang.IndexOutOfBoundsException e) {
                            return showAs + " ckod nebyl nalezen.\n";
                        }

                }
            }catch (Exception e){
                System.out.println(e);
            }
        }catch (Exception e){
            System.out.println(e);
        }
        return "Neznámá chyba\n";
    }

    public static String updateInstance(String internalId, String did, String didValue,String inventory,SseEmitter emitter) throws IOException {
        String update = "UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\""+did+"\":\"" + didValue + "\"}";
        Response res= Request.Post(inventory).bodyString(update, ContentType.DEFAULT_TEXT).execute();
        String status = res.returnContent().asString();
        String lines[] = status.split("\\r?\\n");
        emitter.send(SseEmitter.event().data(createEventData("Odeslano: \n")));
        for(String line:lines){
            emitter.send(SseEmitter.event().data(createEventData(line)));
        }
        emitter.send(SseEmitter.event().data(createEventData("\n\n")));
        if(status.contains("Error:")){
            emitter.send(SseEmitter.event().data(createEventData("\n"+"Error: Atribut: "+did+" se nepodařilo přidat/aktualizovat\n","error")));
            return "nok";
        }else{
            emitter.send(SseEmitter.event().data(createEventData("Hodnota atributu: "+did+" aktualizována na: "+didValue+"\n")));
        }
        return "ok";
    }
    public static String updateInstanceParsed(String update, String inventory, SseEmitter emitter) throws IOException {
        Response res= Request.Post(inventory).bodyString(update, ContentType.DEFAULT_TEXT).execute();
        String status = res.returnContent().asString();
        String lines[] = status.split("\\r?\\n");
        emitter.send(SseEmitter.event().data(createEventData("Odeslano: ")));
        for(String line:lines){
            emitter.send(SseEmitter.event().data(createEventData(line)));
        }
        if(status.contains("Error:")){
            emitter.send(SseEmitter.event().data(createEventData("Error: Atributy se nepodařilo přidat/aktualizovat","error")));
            return "nok";
        }else{
            emitter.send(SseEmitter.event().data(createEventData("Atributy aktualizovány")));
        }
        return "ok";
    }

    public static Object checkIfInputEmpty(String input,Object ifEmptyReturnValue){
        if(!input.isEmpty()){
            return input;
        }else {
            return ifEmptyReturnValue;
        }
    }

    public static void createEdge(String ckodFrom, String ckodTo, String edgeType, String idDidFrom, String idDidTo,String inventory,SseEmitter emitter) throws IOException {
        String edge="CREATE_EDGE: {\"type\": \""+edgeType+"\"}\n{\"from\":{\"ext_id_did\":\""+idDidFrom+"\",\""+idDidFrom+"\":\""+ckodFrom+"\"},\"to\":{\"ext_id_did\":\""+idDidTo+"\",\""+idDidTo+"\":\""+ckodTo+"\"},\"exists_from\":\"now\"}";
        Response rep = Request.Post(inventory).bodyString(edge, ContentType.DEFAULT_TEXT).execute();
        String status = rep.returnContent().asString();
        String lines[] = status.split("\\r?\\n");
        emitter.send(SseEmitter.event().data(createEventData("Odeslano: \n")));
        for(String line:lines){
            emitter.send(SseEmitter.event().data(createEventData(line)));
        }
        if(status.contains("Error:")){
            emitter.send(SseEmitter.event().data(createEventData("Vazba mezi: "+ckodFrom+" a: "+ckodTo+" typu: "+edgeType+" nebyla vytvořena\n")));
        }else {
            emitter.send(SseEmitter.event().data(createEventData("Vazba mezi: "+ckodFrom+" a: "+ckodTo+" typu: "+edgeType+" byla úspěšně vytvořena\n")));
        }
    }
    private static String removeFirst(char ch, String s) {
        int charPos = s.indexOf(ch);
        if (charPos < 0) {
            return s;
        }
        return new StringBuilder(s).deleteCharAt(charPos).toString();
    }
    public static void exceptionInServerLog(Exception e, String module, String function, String query){
        int freq = Collections.frequency(Collections.singleton(query),"__typename");
        query=query.replaceAll("__typename","");
        for(int i=0;i<freq;i++){
            query = removeFirst('}',query);
            query = removeFirst('{',query);
        }
        System.out.println("Exception: "+e+"\n");
        System.out.println("Module: "+module);
        System.out.println("Function: "+function+"\n");
        System.out.println("Query: \n"+query+"\n");
    }
    public static void exceptionInServerLog(Exception e, String module, String function){
        System.out.println("Exception: "+e+"\n");
        System.out.println("Module: "+module);
        System.out.println("Function: "+function+"\n");
    }

    public static String createEventData(String data, String id, String type){
        Gson gson = new Gson();
        Map<String,String> obj = Map.ofEntries(entry("id",id), entry("type",type), entry("data",data));
        String json = gson.toJson(obj);
        return json;
    }
    @Experimental
    public static String createEventData(ArrayList<String> data, String id, String type){
        Gson gson = new Gson();
        // Map<String,String> obj = Map.ofEntries(entry("id",id), entry("type",type), entry("data",data));
        Map<String, Serializable> obj = Map.ofEntries(entry("id", id), entry("type", type), entry("data", data));
        String json = gson.toJson(obj);
        return json;
    }

    public static String createEventData(ArrayList<String> data, String type){
        return createEventData(data,"none",type);
    }

    public static String createEventData(String data, String type){
        return createEventData(data,"none",type);
    }
    public static String createEventData(String data){
        return createEventData(data,"none","message");
    }


    public static void createException(Exception e,String function,String data,String module,String query,SseEmitter emitter) throws IOException {
        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
        emitter.send(SseEmitter.event().data(createEventData(data+getExceptionLine(e,function),"error")));
        SharedMethods.exceptionInServerLog(e,module,function, query);
    }

    public static String getExceptionLine(Exception e, String function) {
        for(int i=0;i<e.getStackTrace().length;i++){
            if(e.getStackTrace()[i].getMethodName().equals(function)){
                return String.valueOf(e.getStackTrace()[i].getLineNumber());
            }
        }
        return "unknown";
    }

    public static void createException(Exception e,String function, String data, String module, SseEmitter emitter) throws IOException {
        createException(e,function,data,module,"",emitter);
    }
}
