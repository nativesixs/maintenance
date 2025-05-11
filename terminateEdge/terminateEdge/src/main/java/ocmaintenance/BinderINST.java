package ocmaintenance;

import com.apollographql.apollo.ApolloClient;
import ocmaintenance.controllers.PagesController;
import ocmaintenance.controllers.SharedMethods;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ocmaintenance.controllers.SharedMethods.createEventData;

public class BinderINST {
    private static final String module = "BinderINST";

    public static StringBuilder vypsatLvm(String platoCkod, String url, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        PagesController.binderLvms.clear();
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        if (SharedMethods.validateCkod(platoCkod, "plato", "plato", apolloClient).equals("ok")) {
            try {
                Map<String, List<String>> lvmData = new HashMap<>();
                ArrayList<String> lvms = new ArrayList<>();
                CompletableFuture<GetPlatoEdgesQuery.Data> response = QueryHandler.execute(new GetPlatoEdgesQuery(Collections.singletonList(platoCkod)), apolloClient);
                if(response.get().plato.get(0).place.size()<1){ // plato nema prirazenou DTS
                    emitter.send(SseEmitter.event().data(createEventData("Plato nemá přiřazenou DTS")));
                    return null;
                }
                for(int i=0;i<response.get().plato.get(0).lvm.size();i++){
                    try {
                        CompletableFuture<GetLvmEdgesQuery.Data> res = QueryHandler.execute(new GetLvmEdgesQuery(Collections.singletonList(response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value)), apolloClient);
                        if(res.get().lvm.get(0).place.size()>0) {
                            for (int j = 0; j < res.get().lvm.get(0).place.size(); j++) {
                                lvmData.put(response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value, Collections.singletonList(res.get().lvm.get(0).place.get(j).edgeEndPoint.id.value));
                                PagesController.binderLvms.put(res.get().lvm.get(0).id.value, Collections.singletonList(res.get().lvm.get(0).place.get(j).internalId));
                            }
                        }else {
                            emitter.send(SseEmitter.event().data(createEventData("LVM: "+response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value.toString()+" nemá přiřazený vývod")));
                            lvmData.put(response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value,Collections.singletonList(null));
                            PagesController.binderLvms.put(res.get().lvm.get(0).id.value, Collections.singletonList(null));
                        }
                    }catch (java.lang.IndexOutOfBoundsException e){
                        emitter.send(SseEmitter.event().data(createEventData("LVM: "+lvms.get(i)+" nemá přiřazený vývod")));
                        lvmData.put(response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value,Collections.singletonList(null));
                    }

                }
                // prints information about LVMs and vyvods to log
                for (Map.Entry<String, List<String>> lvm : lvmData.entrySet()) {
                    emitter.send(SseEmitter.event().data(createEventData("LVM: "+lvm.getKey())));

                    for(int i=0;i<lvm.getValue().size();i++) {
                        emitter.send(SseEmitter.event().data(createEventData("Vyvod("+(i+1)+"/"+lvm.getValue().size()+"): " + lvm.getValue().get(i))));
                    }
                }

                //sends data to frontend - create & fill comboboxes
                Map<String, List<String>> lvmChangeOptions = getLvmChangeOptions(lvmData,getVyvody(platoCkod,url,emitter));
                int c=1;
                for (Map.Entry<String, List<String>> lvm : lvmChangeOptions.entrySet()) {
                    emitter.send(SseEmitter.event().data(createEventData("createLVM"+lvm.getKey())));
                    for(int i=0;i<lvm.getValue().size();i++) {
                        emitter.send(SseEmitter.event().data(createEventData("addOption"+lvm.getKey()+"separator"+lvm.getValue().get(i))));
                    }
                    c=c+1;
                }

                if(response.get().plato.get(0).lvm.size()==0){
                    emitter.send(SseEmitter.event().data(createEventData("Na plato nejsou přivazbeny LVM")));
                    return null;
                }

            }catch (Exception e){
                emitter.send(SseEmitter.event().data(createEventData("BinderINST exception - vypsatLvm - GetPlatoEdgesQuery "+e)));
                SharedMethods.exceptionInServerLog(e,module,"vypsatLvm", GetPlatoEdgesQuery.QUERY_DOCUMENT);
            }
        }else {
            emitter.send(SseEmitter.event().data(createEventData("Zadaný ckod není ckod plata")));
            return null;
        }
        return null;
    }

    public static void vypsatDTS(String place, String inventory, String url, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        if (SharedMethods.validateCkod(place, "place", "DTS", apolloClient).equals("ok")) {
            try {
                CompletableFuture<GetDTSrozvadecQuery.Data> res = QueryHandler.execute(new GetDTSrozvadecQuery(Collections.singletonList(place)), apolloClient);

//                if (res.get().trafostanice.items.get(0).platos.size()>=1) {
//                    for (int j = 0; j < res.get().trafostanice.items.get(0).platos.size();j++) {
//                        emitter.send("Nalezena plata navazbena na DTS: "); //vypis plat
//                        emitter.send("Plato: "+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.id.value);
//                        for(int k=0;k<res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.size();k++){
//                            emitter.send("Nalezene LVM pro plato: "+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.id.value); //vypis lvm
//                            emitter.send("LVM: "+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value);
//                            emitter.send("createLVM"+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value);
//                            if(res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.size()>=1) {
//                                emitter.send("addOption" +res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value+ "separator" + "ponechat aktualni");
//                                emitter.send("addOption" +res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value+ "separator" + "odvazbit");
//                                for (int l = 0; l < res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.size(); l++) {
//                                    emitter.send("Aktualni prirazeny vyvod: "+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.get(l).edgeEndPoint.id.value);
//                                    emitter.send("addOption"+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value+ "separator" + res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.get(l).edgeEndPoint.id.value);
//                                }
//                            }
//                        }
//                    }
//                }

                for(int i=0;i<res.get().trafostanice.items.get(0).edgeInstance.size();i++){
                    try {
                        String assignedVyvod = null;
                        CompletableFuture<GetRozvadecVyvodyQuery.Data> re = QueryHandler.execute(new GetRozvadecVyvodyQuery(Collections.singletonList(res.get().trafostanice.items.get(0).edgeInstance.get(i).edgeEndPoint.id.value)), apolloClient);
                        for(int j=0;j<re.get().trafostanice.items.get(0).edgeInstance.size();j++){
                            emitter.send(SseEmitter.event().data(createEventData("Nalezene vyvody DTS: ")));
                            emitter.send(SseEmitter.event().data(createEventData("Vyvod("+(j+1)+"/"+re.get().trafostanice.items.get(0).edgeInstance.size()+"): "+re.get().trafostanice.items.get(0).edgeInstance.get(j).edgeEndPoint.id.value)));
                            assignedVyvod = re.get().trafostanice.items.get(0).edgeInstance.get(j).edgeEndPoint.id.value;
                        }
                        if (res.get().trafostanice.items.get(0).platos.size()>=1) {
                            for (int j = 0; j < res.get().trafostanice.items.get(0).platos.size();j++) {
                                emitter.send(SseEmitter.event().data(createEventData("Nalezena plata navazbena na DTS: "))); //vypis plat
                                emitter.send(SseEmitter.event().data(createEventData(res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.id.value)));
                                for(int k=0;k<res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.size();k++){
                                    emitter.send(SseEmitter.event().data(createEventData("Nalezene LVM pro plato: "+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.id.value))); //vypis lvm
                                    emitter.send(SseEmitter.event().data(createEventData("LVM: "+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value)));
                                    emitter.send(SseEmitter.event().data(createEventData("createLVM"+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value)));
                                    if(res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.size()>=1) {
                                        emitter.send(SseEmitter.event().data(createEventData("addOption" +res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value+ "separator" + "ponechat aktualni")));
                                        emitter.send(SseEmitter.event().data(createEventData("addOption" +res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value+ "separator" + "odvazbit")));
                                        for (int l = 0; l < res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.size(); l++) {
                                            emitter.send(SseEmitter.event().data(createEventData("Aktualni prirazeny vyvod: "+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.get(l).edgeEndPoint.id.value)));
                                            if(!assignedVyvod.equals(res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.get(l).edgeEndPoint.id.value)){
                                                emitter.send(SseEmitter.event().data(createEventData("addOption"+res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.id.value+ "separator" + res.get().trafostanice.items.get(0).platos.get(j).edgeEndPoint.platosLvms.get(k).edgeEndPoint.platosLvmsVyvody.get(l).edgeEndPoint.id.value)));
                                            }

                                        }
                                    }
                                }
                            }
                        }
                    }catch (Exception e){
                        emitter.send(SseEmitter.event().data(createEventData("BinderINST exception - getVyvody - GetRozvadecVyvodyQuery " + e)));
                        SharedMethods.exceptionInServerLog(e,module,"getVyvody", GetRozvadecVyvodyQuery.QUERY_DOCUMENT);

                    }
                }

            }catch (Exception e){
                emitter.send(SseEmitter.event().data(createEventData("BinderINST exception - getVyvody - GetDTSrozvadecQuery " + e)));
                SharedMethods.exceptionInServerLog(e,module,"getVyvody", GetDTSrozvadecQuery.QUERY_DOCUMENT);
            }
        }
    }

    public static ArrayList<String> getVyvody(String platoCkod, String url, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ArrayList<String> vyvody = new ArrayList<>();
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        //todo - najit elegantnejsi reseni nez 2x query na zjisteni vyvodu z DTS
        if (SharedMethods.validateCkod(platoCkod, "plato", "plato", apolloClient).equals("ok")) {
            try {
                CompletableFuture<GetPlatoEdgesQuery.Data> response = QueryHandler.execute(new GetPlatoEdgesQuery(Collections.singletonList(platoCkod)), apolloClient);
                if (response.get().plato.get(0).place.size() < 1) { // plato nema prirazenou DTS
                    return null;
                }else {
                    try {
                        CompletableFuture<GetDTSrozvadecQuery.Data> res = QueryHandler.execute(new GetDTSrozvadecQuery(Collections.singletonList(response.get().plato.get(0).place.get(0).edgeEndPoint.id.value)), apolloClient);
                        for(int i=0;i<res.get().trafostanice.items.get(0).edgeInstance.size();i++){
                            try {
                                CompletableFuture<GetRozvadecVyvodyQuery.Data> re = QueryHandler.execute(new GetRozvadecVyvodyQuery(Collections.singletonList(res.get().trafostanice.items.get(0).edgeInstance.get(i).edgeEndPoint.id.value)), apolloClient);
                                for(int j=0;j<re.get().trafostanice.items.get(0).edgeInstance.size();j++){
                                    vyvody.add(re.get().trafostanice.items.get(0).edgeInstance.get(j).edgeEndPoint.id.value);
                                }
                            }catch (Exception e){
                                emitter.send(SseEmitter.event().data(createEventData("BinderINST exception - getVyvody - GetRozvadecVyvodyQuery " + e)));
                                SharedMethods.exceptionInServerLog(e,module,"getVyvody", GetDTSrozvadecQuery.QUERY_DOCUMENT);
                            }
                        }
                    }catch (Exception e){
                        emitter.send(SseEmitter.event().data(createEventData("BinderINST exception - getVyvody - GetDTSrozvadecQuery " + e)));
                        SharedMethods.exceptionInServerLog(e,module,"getVyvody", GetDTSrozvadecQuery.QUERY_DOCUMENT);
                    }
                }
            }catch(Exception e){
                emitter.send(SseEmitter.event().data(createEventData("BinderINST exception - getVyvody - GetPlatoEdgesQuery " + e)));
                SharedMethods.exceptionInServerLog(e,module,"getVyvody", GetPlatoEdgesQuery.QUERY_DOCUMENT);
            }
        }
        return vyvody;
    }


    public static Map<String, List<String>> getLvmChangeOptions(Map<String, List<String>> lvmData, ArrayList<String> vyvody) {
        Map<String, List<String>> possibleCombinations = new HashMap<>();
        for (Map.Entry<String, List<String>> lvm : lvmData.entrySet()) {
            List<String> possibleValues = new ArrayList<>();
            for(int i=0;i<vyvody.size();i++) {
                if (lvm.getValue().contains(vyvody.get(i))) {
                    possibleValues.add("ponechat aktualni");
                    possibleValues.add("odvazbit");
                } else {
                    possibleValues.add(vyvody.get(i));
                }

            }
            possibleCombinations.put(lvm.getKey(),possibleValues);
        }
        return possibleCombinations;
    }

    public static void main(){

    }

//    private static boolean terminateEdge(SseEmitter emitter,String currentVyvod,String inventory, ApolloClient apolloClient) throws IOException, ExecutionException, InterruptedException {
//        if(currentVyvod!=null) {
//            ArrayList<String> v = new ArrayList<>();
//            v.clear();
//            v.add(currentVyvod);
//            if(SharedMethods.terminateRequestDynamic(v, "installed_at", inventory, apolloClient, emitter, true).equals("ok")){
//                return true;
//            }else {
//                return false;
//            }
//        }else {
//            return true;
//            // nema vazby -> return true -> muze se navazat
//        }
//    }

//    @Deprecated
//    private static void createEdge(SseEmitter emitter, String inventory, String lvmCkod, String vyvodTarget) throws IOException {
//        String edge="CREATE_EDGE: {\"type\": \"installed_at\", \"from\": { \"ext_id_did\": \"information:attribute.ckod\"}, \"to\": { \"ext_id_did\": \"information:attribute.uplne_sjz\"}}\n{\"from\": {\"information:attribute.ckod\": \""+lvmCkod+"\"}, \"to\": {\"information:attribute.uplne_sjz\": \""+vyvodTarget+"\"}}";
//        Response res= Request.Post(inventory).bodyString(edge, ContentType.DEFAULT_TEXT).execute();
//        String status = res.returnContent().asString();
//        String lines[] = status.split("\\r?\\n");
//        emitter.send("Odeslano: \n");
//        for(String line:lines){
//            emitter.send(line);
//        }
//        emitter.send("\n\n");
//    }

//    private static void switchVyvod(String lvmCkod, String vyvodTarget, String currentVyvod, SseEmitter emitter, ApolloClient apolloClient, String inventory) throws IOException, ExecutionException, InterruptedException {
//        if(terminateEdge(emitter,currentVyvod,inventory,apolloClient)){
//            //replace with sharedmethods
////            createEdge(emitter, inventory,lvmCkod,vyvodTarget);
//        }else{
//            emitter.send("Odvazbení: "+lvmCkod+" od: "+currentVyvod+" se nezdařilo, navazbení na: "+vyvodTarget+" neproběhne");
//        }
//    }

//    @Deprecated
//    private static void createEdgePlatoDTS(String plato, String DTS, String inventory, SseEmitter emitter) throws IOException {
//        String edge="CREATE_EDGE: {\"type\": \"installed_at\", \"from\": { \"ext_id_did\": \"information:attribute.ckod\"}, \"to\": { \"ext_id_did\": \"information:attribute.uplne_sjz\"}}\n{\"from\": {\"information:attribute.ckod\": \""+plato+"\"}, \"to\": {\"information:attribute.uplne_sjz\": \""+DTS+"\"}}";
//        Response res= Request.Post(inventory).bodyString(edge, ContentType.DEFAULT_TEXT).execute();
//        String status = res.returnContent().asString();
//        emitter.send(status);
//    }
//    private static void displayLVMvyvody(ArrayList<String> platoLVMckods, ArrayList<String> vyvody, SseEmitter emitter) throws IOException {
//        int c=1;
//        for (int i=0;i<platoLVMckods.size();i++) {
////            model.addAttribute("lvm"+c,"LVM: "+platoLVMckods.get(i));
////            model.addAttribute("showElement"+c,true);
//            emitter.send("createLVM "+"LVM: "+platoLVMckods.get(i));
////            ArrayList<String> options = new ArrayList<>();
//            for(int j=0;j<vyvody.size();j++) {
////                options.add(vyvody.get(j));
//                emitter.send("addOption"+vyvody.get(j));
//            }
////            model.addAttribute("options"+c,options);
//            c=c+1;
//        }
//    }

//    private static void deleteAllLVMplaceEdges(ArrayList<String> platoLVMckods, String inventory, ApolloClient apolloClient,SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
////        log.append(SharedMethods.terminateRequest(platoLVMckods,"installed_at",inventory,apolloClient,true,true));
////        emitter.send(SharedMethods.terminateRequest(platoLVMckods,"installed_at",inventory,apolloClient,true,true));
//    }
    public static void checkLvmVyvody(Map<String, String> selectData, String inventory, String url, SseEmitter emitter) throws IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        ArrayList<String> lvms = getLvmsFromJson(selectData);
        for(int i=0;i<lvms.size();i++){
            try {
                String expectedPlace = selectData.get(lvms.get(i));
                CompletableFuture<GetLvmEdgesQuery.Data> response = QueryHandler.execute(new GetLvmEdgesQuery(Collections.singletonList(lvms.get(i))), apolloClient);
                if(response.get().lvm.get(0).place.size()==0 || response.get().lvm.get(0).place.get(0).edgeEndPoint.id.value == null){ // jestli lvm nema vyvod - vyvorit
                    SharedMethods.createEdge(lvms.get(i),expectedPlace,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
                } else if (!response.get().lvm.get(0).place.get(0).edgeEndPoint.id.value.equals(expectedPlace)) { // jestli ma DTS vyvod ktery neni spravny, ukoncit tento vyvod a pak vtvorit expected vyvod
                    boolean canCreate = true;
                    for(int j=0;j<response.get().lvm.get(0).place.size();j++) {
                        if(!SharedMethods.terminateRequestDynamic(new ArrayList<>(Collections.singletonList(response.get().lvm.get(0).place.get(j).internalId)),"installed_at",inventory,apolloClient,emitter,true).equals("ok")){
                            emitter.send(SseEmitter.event().data(createEventData("vazbu: "+response.get().lvm.get(0).id.value+" nebylo mozno ukoncit")));
                            canCreate = false;
                        }
                    }
                    if(canCreate) {
                        SharedMethods.createEdge(lvms.get(i), expectedPlace, "installed_at", "information:attribute.ckod", "information:attribute.uplne_sjz", inventory, emitter);
                    }
                }
            }catch (Exception e){
                emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                emitter.send(SseEmitter.event().data(createEventData("BinderINST - checkLVMvyvody function exception - GetLvmEdges query failed")));
                SharedMethods.exceptionInServerLog(e,module,"checklvmvyvody", GetLvmEdgesQuery.QUERY_DOCUMENT);
            }
        }

    }

    public static void bindPlatoDTS(String platoField, String placeckodField, String inventory, String url, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        if (SharedMethods.validateCkod(platoField, "plato", "plato", apolloClient).equals("ok")) {
            emitter.send(SseEmitter.event().data(createEventData("plato ckod ok")));
            if (SharedMethods.validateCkod(placeckodField, "place", "DTS", apolloClient).equals("ok")) {
                emitter.send(SseEmitter.event().data(createEventData("DTS ckod ok")));
                try {
                    CompletableFuture<GetPlatoEdgesQuery.Data> response = QueryHandler.execute(new GetPlatoEdgesQuery(Collections.singletonList(platoField)), apolloClient);
                    if(response.get().plato.get(0).place.size()<1){ // check plato isnt binded to place - if is -> error
                        emitter.send(SseEmitter.event().data(createEventData("plato nemá vazbu na DTS, proběhne navazbení na: "+placeckodField)));
//                        createEdgePlatoDTS(platoField,placeckodField,inventory,emitter);
                        SharedMethods.createEdge(platoField,placeckodField,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
                        if(response.get().plato.get(0).lvm.size()>0){ // get LVMs from plato
                            ArrayList<String> platoLVMckods = new ArrayList<>();
                            for(int i=0;i<response.get().plato.get(0).lvm.size();i++){
                                platoLVMckods.add(response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value);
                            }
//                            ArrayList<String> vyvody = getVyvody(platoField,url,emitter);
//                            displayLVMvyvody(platoLVMckods,vyvody,emitter);
                            //deleteAllLVMplaceEdges(platoLVMckods,inventory,apolloClient,emitter);
                            //end of good run
                        }else{
                            emitter.send(SseEmitter.event().data(createEventData("plato nema navazbene LVM")));
                        }
                    }else{
                        for(int i=0;i<response.get().plato.get(0).place.size();i++) {
                            emitter.send(SseEmitter.event().data(createEventData("plato má vazbu na DTS: " + response.get().plato.get(0).place.get(i).edgeEndPoint.id.value + " ID: " + response.get().plato.get(0).place.get(i).internalId)));
                        }
                    }
                }catch (Exception e){
                    emitter.send(SseEmitter.event().data(createEventData("Exception BinderINST - bindPlatoDTS - GetPlatoEdges query "+e)));
                    SharedMethods.exceptionInServerLog(e,module,"bindPlatoDTS", GetPlatoEdgesQuery.QUERY_DOCUMENT);
                }
            }else {
                emitter.send(SseEmitter.event().data(createEventData("DTS ckod nok")));
            }
        }else{
            emitter.send(SseEmitter.event().data(createEventData("plato ckod nok")));
        }
    }


//    public static void switchVyvody(Map<String, String> selectData, String inventory, String url, SseEmitter emitter) {
//        for (Map.Entry<String, String> entry : selectData.entrySet()) {
//            String selectId = entry.getKey();
//            String selectedOption = entry.getValue();
//            System.out.println("lvm: "+selectId);
//            System.out.println("opt: "+selectedOption);
//        }
//    }

    private static void createSelectOptionsElementsForLvm(CompletableFuture<GetPlatoEdgesQuery.Data> response, String placeField, SseEmitter emitter, ApolloClient apolloClient, String function) throws IOException {
        try {
            CompletableFuture<GetDTSrozvadecQuery.Data> res = QueryHandler.execute(new GetDTSrozvadecQuery(Collections.singletonList(placeField)), apolloClient);
            for (int i = 0; i < response.get().plato.get(0).lvm.size(); i++) { // vytvori comboboxes pro LVM plata
                if (response.get().plato.get(0).lvm.size() < 1) {
                    emitter.send(SseEmitter.event().data(createEventData("Plato nema prirazene LVM")));
                } else {
//                    emitter.send("LVM zadaneho plata: ");
                    emitter.send(SseEmitter.event().data(createEventData("createLVM" + response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value)));
                    try {
                        CompletableFuture<GetRozvadecVyvodyQuery.Data> re = QueryHandler.execute(new GetRozvadecVyvodyQuery(Collections.singletonList(res.get().trafostanice.items.get(0).edgeInstance.get(0).edgeEndPoint.id.value)), apolloClient);
                        for (int j = 0; j < re.get().trafostanice.items.get(0).edgeInstance.size(); j++) { // naplni comboboxy LVM plata vyvody nove zadane DTS
                            emitter.send(SseEmitter.event().data(createEventData("addOption" + response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value + "separator" + re.get().trafostanice.items.get(0).edgeInstance.get(j).edgeEndPoint.id.value)));
                        }
                    } catch (Exception e) {
                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                        emitter.send(SseEmitter.event().data(createEventData("BinderINST - " + function + "function exception - getrozvadecvyvody query failed")));
                        SharedMethods.exceptionInServerLog(e, module, function, GetRozvadecVyvodyQuery.QUERY_DOCUMENT);
                    }
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("BinderINST - "+function+" function exception - getDTSrozvadec query failed")));
            SharedMethods.exceptionInServerLog(e,module,function, GetDTSrozvadecQuery.QUERY_DOCUMENT);
        }
    }

    private static ArrayList<String> getDTSvyvody(String place ,SseEmitter emitter, ApolloClient apolloClient) throws IOException {
        ArrayList<String> vyvody = new ArrayList<>();
        try {
            CompletableFuture<GetDTSrozvadecQuery.Data> res = QueryHandler.execute(new GetDTSrozvadecQuery(Collections.singletonList(place)), apolloClient);
            try {
                CompletableFuture<GetRozvadecVyvodyQuery.Data> re = QueryHandler.execute(new GetRozvadecVyvodyQuery(Collections.singletonList(res.get().trafostanice.items.get(0).edgeInstance.get(0).edgeEndPoint.id.value)), apolloClient);
                for(int i=0;i<re.get().trafostanice.items.get(0).edgeInstance.size();i++){
                    vyvody.add(re.get().trafostanice.items.get(0).edgeInstance.get(i).edgeEndPoint.id.value);
                }
            }catch (Exception e){
                emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                emitter.send(SseEmitter.event().data(createEventData("BinderINST - getDTSvyvody function exception - getrozvadecvyvody query failed")));
                SharedMethods.exceptionInServerLog(e, module, "getDTSvyvody", GetRozvadecVyvodyQuery.QUERY_DOCUMENT);
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("BinderINST - getDTSvyvody function exception - getDTSrozvadec query failed")));
            SharedMethods.exceptionInServerLog(e,module,"getDTSvyvody", GetDTSrozvadecQuery.QUERY_DOCUMENT);
        }
        return vyvody;
    }
    public static void getBothFieldsData(String platoField, String placeField, String url, SseEmitter emitter) throws IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        emitter.send(SseEmitter.event().data(createEventData("deletePreviouslyCreatedElements")));
        try{
            CompletableFuture<GetPlatoEdgesQuery.Data> response = QueryHandler.execute(new GetPlatoEdgesQuery(Collections.singletonList(platoField)), apolloClient);
            if(response.get().plato.get(0).place.size()==0){ // plato nema prirazenou dts, nabidnout privazbeni dts a vyvodu
                emitter.send(SseEmitter.event().data(createEventData("Plato nema prirazenou zadnou DTS")));
                emitter.send(SseEmitter.event().data(createEventData("privazatK")));
                createSelectOptionsElementsForLvm(response,placeField,emitter,apolloClient,"getBothFieldsData");
                emitter.send(SseEmitter.event().data(createEventData("createElementNavazatPlato")));
            } else if (response.get().plato.get(0).place.size()>=1) { //plato ma prirazenou dts
                try{
                    String dtsPlata = response.get().plato.get(0).place.get(0).edgeEndPoint.id.value;
                    if(!dtsPlata.equals(placeField)){ // dts nejsou stejne -> odvazat plato od dtsPlata, privazbit novouDTS (placeField) na platoField
                        emitter.send(SseEmitter.event().data(createEventData("DTS zadaneho plata: "+dtsPlata+" neni shodna se zadanou DTS: "+placeField)));
                        emitter.send(SseEmitter.event().data(createEventData("odvazatOd"+dtsPlata)));
                        emitter.send(SseEmitter.event().data(createEventData("privazatK")));
                        createSelectOptionsElementsForLvm(response,placeField,emitter,apolloClient,"getBothFieldsData");
                        emitter.send(SseEmitter.event().data(createEventData("createElementNewDTSvyvody")));
//                        emitter.send("Akce: Privazat plato na nove zadanou DTS, privazat vyvody nove DTS k LVM");
                    }
                    if(dtsPlata.equals(placeField)){ // dts jsou stejne, lvm plata nemaji prirazene zadne vyvody nebo maji prirazene jine vyvody nez teto dts
                        emitter.send(SseEmitter.event().data(createEventData("DTS zadaneho plata: "+dtsPlata+" je shodna se zadanou DTS")));
                        for(int i=0;i<response.get().plato.get(0).lvm.size();i++) {
                            if(response.get().plato.get(0).lvm.get(i).edgeEndPoint.lvm_vyvody.size()==0){ // pokud lvm nema vyvod nabidnout prirazeni vyvodu dts
                                createSelectOptionsElementsForLvm(response,placeField,emitter,apolloClient,"getBothFieldsData");
                                emitter.send(SseEmitter.event().data(createEventData("Mozno priradit nasledujici vyvody pro LVM: ")));
                                emitter.send(SseEmitter.event().data(createEventData("createElementAddVyvodyToLvm")));
//                                emitter.send("Akce: Privazat vyvody DTS k LVM");
                            }else{
                                ArrayList<String> vyvody = getDTSvyvody(placeField,emitter,apolloClient);
                                for(int j=0;j<response.get().plato.get(0).lvm.get(i).edgeEndPoint.lvm_vyvody.size();j++) {
                                    if (!vyvody.contains(response.get().plato.get(0).lvm.get(i).edgeEndPoint.lvm_vyvody.get(j).edgeEndPoint.id.value)) { // pokud jsou na lvm prirazeny vyvody jine DTS nez te zadane a privazane na plato nabidnout prevazani
                                        emitter.send(SseEmitter.event().data(createEventData("LVM: "+response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value+" ma prirazeny vyvod: "+response.get().plato.get(0).lvm.get(i).edgeEndPoint.lvm_vyvody.get(j).edgeEndPoint.id.value+" ktery nenalezi DTS prirazene platu")));
                                        emitter.send(SseEmitter.event().data(createEventData("odvazatVyvod"+response.get().plato.get(0).lvm.get(i).edgeEndPoint.lvm_vyvody.get(j).edgeEndPoint.id.value)));
                                        createSelectOptionsElementsForLvm(response,placeField,emitter,apolloClient,"getBothFieldsData");
                                        emitter.send(SseEmitter.event().data(createEventData("createElementUnbindVyvodBindCorrect")));
//                                        emitter.send("Akce: Odvazat aktualni vyvody LVM a navazat nove zvolene vyvody");

                                    }else{
                                        emitter.send(SseEmitter.event().data(createEventData("LVM: "+response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value+" je prirazen vyvod: "+response.get().plato.get(0).lvm.get(i).edgeEndPoint.lvm_vyvody.get(j).edgeEndPoint.id.value+" patrici DTS: "+dtsPlata)));
                                    }
                                }
                            }
                        }
                    }


                }catch (Exception e){
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                    emitter.send(SseEmitter.event().data(createEventData("BinderINST - getBothFieldsData function exception - getDTSrozvadec query failed")));
                    SharedMethods.exceptionInServerLog(e,module,"getBothFieldsData", GetDTSrozvadecQuery.QUERY_DOCUMENT);
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("BinderINST - getBothFieldsData function exception - getplatoedges query failed")));
            SharedMethods.exceptionInServerLog(e,module,"getBothFieldsData", GetPlatoEdgesQuery.QUERY_DOCUMENT);
        }
    }

    public static void analyzeInput(String platoField, String placeField, String inventory, String url, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        if(SharedMethods.validateCkod(platoField, "plato", "plato", apolloClient).equals("ok")
                && SharedMethods.validateCkod(placeField, "place", "DTS", apolloClient ).equals("ok")){
            // both ok = getBoth
            getBothFieldsData(platoField,placeField, url,emitter);
        } else if (SharedMethods.validateCkod(platoField, "plato", "plato", apolloClient).equals("ok")) {
            // plato ok, place nok
            vypsatLvm(platoField, url,emitter);
        } else if (SharedMethods.validateCkod(placeField, "place", "DTS", apolloClient ).equals("ok")) {
            // place ok, plato nok
            vypsatDTS(placeField,inventory,url,emitter);
        }
    }

    public static ArrayList<String> getLvmsFromJson(Map<String, String> selectData) {
        ArrayList<String> lvms = new ArrayList<>();
        for (Map.Entry<String, String> entry : selectData.entrySet()) {
            String key = entry.getKey();
            if (!key.equals("place") && !key.equals("plato") && !key.equals("platotodts") && !key.equals("odvazatvyvod") && !key.equals("odvazatplato")) {
                lvms.add(key);
            }
        }
        return lvms;
    }

    public static void unbindPlatoDTS(String plato, String inventory, String url, SseEmitter emitter) throws IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        try {
            CompletableFuture<GetPlatoEdgesQuery.Data> response = QueryHandler.execute(new GetPlatoEdgesQuery(Collections.singletonList(plato)), apolloClient);
            for(int i=0;i<response.get().plato.get(0).place.size();i++) {
                if (SharedMethods.terminateRequestDynamic(new ArrayList<>(Collections.singletonList(response.get().plato.get(0).place.get(i).internalId)), "installed_at", inventory, apolloClient, emitter, true).equals("ok")) {
                    emitter.send(SseEmitter.event().data(createEventData("plato odvazano od DTS")));
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("BinderINST - unbindPlatoDTS function exception - getplatoedges query failed")));
            SharedMethods.exceptionInServerLog(e,module,"unbindPlatoDTS", GetPlatoEdgesQuery.QUERY_DOCUMENT);
        }
    }
}
