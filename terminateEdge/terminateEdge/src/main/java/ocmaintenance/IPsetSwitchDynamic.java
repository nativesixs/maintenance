package ocmaintenance;

import com.apollographql.apollo.ApolloClient;
import ocmaintenance.controllers.SharedMethods;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ocmaintenance.controllers.SharedMethods.createEventData;

public class IPsetSwitchDynamic {
    private static String ckod = null;
    private static boolean safe;
    private static String inventory = null;
    private static String ipsetP,ipsetT,apnP,apnT="";
    private static String activeAPN,activeIpset;
    private static final String module = "IPsetSwitch";


    private static void identify(String ckod,ApolloClient apolloClient,SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<GetAssignedObjectsIpsApnQuery.Data> response = QueryHandler.execute(new GetAssignedObjectsIpsApnQuery(Collections.singletonList(ckod)), apolloClient);
            try {
                apnP = Objects.requireNonNull(response.get().instances.get(0).apn_prod.get(0).id).value;
            } catch (IndexOutOfBoundsException e) {
                apnP = null;
            }
            try {
                apnT = Objects.requireNonNull(response.get().instances.get(0).apn_test.get(0).id).value;
            } catch (IndexOutOfBoundsException e) {
                apnT = null;
            }
            try {
                ipsetP = Objects.requireNonNull(response.get().instances.get(0).ipset_prod.get(0).id).value;
            } catch (IndexOutOfBoundsException e) {
                ipsetP = null;
            }
            try {
                ipsetT = Objects.requireNonNull(response.get().instances.get(0).ipset_test.get(0).id).value;
            } catch (IndexOutOfBoundsException e) {
                ipsetT = null;
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("IPsetSwitch - identify function exception - getassignedobjectsipsapn query failed")));
            SharedMethods.exceptionInServerLog(e,module,"identify", GetAssignedObjectsIpsApnQuery.QUERY_DOCUMENT);
        }
    }

    private static void getActiveType(String ckod,ApolloClient apolloClient,SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<GetActiveApnIpsetQuery.Data> resp = QueryHandler.execute(new GetActiveApnIpsetQuery(Collections.singletonList(ckod)), apolloClient);
            activeAPN = Objects.requireNonNull(resp.get().instances.get(0).apn.get(0).edgeEndPoint.id).value;
            activeIpset = Objects.requireNonNull(resp.get().instances.get(0).ipset.get(0).edgeEndPoint.id).value;
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("IPsetSwitch - getActiveType function exception - getactiveapnipset query failed")));
            SharedMethods.exceptionInServerLog(e,module,"getActiveType", GetActiveApnIpsetQuery.QUERY_DOCUMENT);
        }
    }

    private static String getSimBoundToDpu(String ckod,ApolloClient apolloClient) throws ExecutionException, InterruptedException {
        CompletableFuture<GetsimdataQuery.Data> response = QueryHandler.execute(new GetsimdataQuery(Collections.singletonList(ckod)),apolloClient);
        return Objects.requireNonNull(response.get().sim_on_dpu.get(0).relInstance.get(0).id.value);
    }

    private static boolean handleInput(String simckodField, String dpuckodField, ApolloClient apolloClient, String inventoryinput,boolean safeIn,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        if(simckodField.length()==0 && dpuckodField.length()==0 || simckodField.length()!=0 && dpuckodField.length()!=0){
            emitter.send(SseEmitter.event().data(createEventData("Error: Nutno zadat číslo SIM nebo ckod DPU.")));
            return false;
        }
        if(simckodField.length()==0 && SharedMethods.validateCkod(dpuckodField,"dpu","dpu",apolloClient).equals("ok")){
            ckod = getSimBoundToDpu(dpuckodField,apolloClient);

        } else if (dpuckodField.length()==0 && SharedMethods.validateCkod(simckodField,"simcard","simcard",apolloClient).equals("ok")) {
            ckod=simckodField;
        }else {
            emitter.send(SseEmitter.event().data(createEventData("Zadaný ckod není validní sim nebo dpu")));
            return false;
        }
        inventory=inventoryinput;
        safe=safeIn;
        return true;
    }

    private static void terminateActiveTunnels(String ckod, ApolloClient apolloClient,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        CompletableFuture<GetActiveApnIpsetQuery.Data> response = QueryHandler.execute(new GetActiveApnIpsetQuery(Collections.singletonList(ckod)),apolloClient);
        ArrayList<String> internalIds = new ArrayList<>();
        for(int i=0;i<response.get().instances.get(0).ipset.size();i++){
            internalIds.add(response.get().instances.get(0).ipset.get(i).internalId);
        }
        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(internalIds,"active_tunnels",inventory,apolloClient,safe,true)))));
    }

    private static void terminateActiveWan(String ckod,ApolloClient apolloClient,SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<GetActiveApnIpsetQuery.Data> response = QueryHandler.execute(new GetActiveApnIpsetQuery(Collections.singletonList(ckod)), apolloClient);
            ArrayList<String> internalIds = new ArrayList<>();
            for (int i = 0; i < response.get().instances.get(0).apn.size(); i++) {
                internalIds.add(response.get().instances.get(0).apn.get(i).internalId);
            }
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(internalIds, "active_wan", inventory, apolloClient, safe, true)))));
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("IPsetSwitch - terminateActiveWan function exception - getactiveapnipset query failed")));
            SharedMethods.exceptionInServerLog(e,module,"terminateActiveWan", GetActiveApnIpsetQuery.QUERY_DOCUMENT);
        }
    }
    private static void switchIPSet(String ckod, ApolloClient apolloClient, String createEdge,SseEmitter emitter) throws IOException {
        if(activeIpset.equals(createEdge)){
            emitter.send(SseEmitter.event().data(createEventData("IPSet je již nastaven na: "+createEdge+"")));
            return;
        }
        try {
            CompletableFuture<GetActiveApnIpsetQuery.Data> response = QueryHandler.execute(new GetActiveApnIpsetQuery(Collections.singletonList(ckod)), apolloClient);

            //pokud neni active_tunnel vytvori se
            if (response.get().instances.get(0).ipset.get(0).internalId.isEmpty()) {
//                createEdge(ckod, createEdge, "IPSet",emitter);
                SharedMethods.createEdge(ckod,createEdge,"active_tunnels","information:attribute.telefonni_cislo","information:information_technology.network.protocols.ipset.ipset_id",inventory,emitter);
                emitter.send(SseEmitter.event().data(createEventData("Nebyla nalezena vazba na active_tunnels, vazba byla vytvořena.")));
                //pokud je active_tunnel, smaze se aktualni a vytvori se novy
            } else {
                terminateActiveTunnels(ckod, apolloClient,emitter);
//                createEdge(ckod, createEdge, "IPSet",emitter);
                SharedMethods.createEdge(ckod,createEdge,"active_tunnels","information:attribute.telefonni_cislo","information:information_technology.network.protocols.ipset.ipset_id",inventory,emitter);

            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("IPsetSwitch - switchIPSet function exception - getactiveapnipset query failed")));
            SharedMethods.exceptionInServerLog(e,module,"switchIPSet", GetActiveApnIpsetQuery.QUERY_DOCUMENT);
        }
    }

    private static void switchAPN(String ckod, ApolloClient apolloClient, String createEdge,SseEmitter emitter) throws IOException {
        if (activeAPN.equals(createEdge)) {
            emitter.send(SseEmitter.event().data(createEventData( "APN je již nastavena na: " + createEdge)));
            return;
        }
        try {
            CompletableFuture<GetActiveApnIpsetQuery.Data> response = QueryHandler.execute(new GetActiveApnIpsetQuery(Collections.singletonList(ckod)), apolloClient);
            //pokud neni active_wan vytvori se
            if (response.get().instances.get(0).apn.get(0).internalId.isEmpty()) {
//                createEdge(ckod, createEdge, "APN",emitter);
                SharedMethods.createEdge(ckod,createEdge,"active_wan","information:attribute.telefonni_cislo","information:information_technology.network.interfaces.mobile.apn.apn_id",inventory,emitter);
                emitter.send(SseEmitter.event().data(createEventData("Nebyla nalezena vazba na active_wan, vazba byla vytvořena.")));
                //pokud je active_wan, smaze se aktualni a vytvori se nova
            } else {
                terminateActiveWan(ckod, apolloClient,emitter);
//                createEdge(ckod, createEdge, "APN",emitter);
                SharedMethods.createEdge(ckod,createEdge,"active_wan","information:attribute.telefonni_cislo","information:information_technology.network.interfaces.mobile.apn.apn_id",inventory,emitter);

            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("IPsetSwitch - switchAPN function exception - getactiveapnipset query failed")));
            SharedMethods.exceptionInServerLog(e,module,"switchAPN", GetActiveApnIpsetQuery.QUERY_DOCUMENT);
        }
    }

    private static void getAssignedEdges(String ckod,ApolloClient apolloClient,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ArrayList<String> apn_test = new ArrayList<>();
        ArrayList<String> apn_prod = new ArrayList<>();
        ArrayList<String> ipset_test = new ArrayList<>();
        ArrayList<String> ipset_prod = new ArrayList<>();
        try {
            CompletableFuture<GetAssignedObjectsIpsApnQuery.Data> response = QueryHandler.execute(new GetAssignedObjectsIpsApnQuery(Collections.singletonList(ckod)), apolloClient);
            emitter.send(SseEmitter.event().data(createEventData("Assigned vazby pro: "+ckod)));
            emitter.send(SseEmitter.event().data(createEventData("ipset_test:")));
            for (int i = 0; i < response.get().instances.size(); i++) {
                for (int j = 0; j < response.get().instances.get(i).edgeInstance.size(); j++) {
                    if(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id.equals("ipset:ipset.ipset_test")) {
                        emitter.send(SseEmitter.event().data(createEventData("internalId: "+response.get().instances.get(i).edgeInstance.get(j).internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("type: "+response.get().instances.get(i).edgeInstance.get(j).type)));
                        emitter.send(SseEmitter.event().data(createEventData("direction: "+response.get().instances.get(i).edgeInstance.get(j).direction)));
                        emitter.send(SseEmitter.event().data(createEventData("elementID: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id)));
                        emitter.send(SseEmitter.event().data(createEventData("existsFrom: "+response.get().instances.get(i).edgeInstance.get(j).existsFrom)));
                        emitter.send(SseEmitter.event().data(createEventData("existsTo: "+response.get().instances.get(i).edgeInstance.get(j).existsTo)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint internalId: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint id: "+ Objects.requireNonNull(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.id).value)));
                    }
                }
            }
            emitter.send(SseEmitter.event().data(createEventData("ipset_prod:")));
            for (int i = 0; i < response.get().instances.size(); i++) {
                for (int j = 0; j < response.get().instances.get(i).edgeInstance.size(); j++) {
                    if(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id.equals("ipset:ipset.ipset_prod")) {
                        emitter.send(SseEmitter.event().data(createEventData("internalId: "+response.get().instances.get(i).edgeInstance.get(j).internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("type: "+response.get().instances.get(i).edgeInstance.get(j).type)));
                        emitter.send(SseEmitter.event().data(createEventData("direction: "+response.get().instances.get(i).edgeInstance.get(j).direction)));
                        emitter.send(SseEmitter.event().data(createEventData("elementID: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id)));
                        emitter.send(SseEmitter.event().data(createEventData("existsFrom: "+response.get().instances.get(i).edgeInstance.get(j).existsFrom)));
                        emitter.send(SseEmitter.event().data(createEventData("existsTo: "+response.get().instances.get(i).edgeInstance.get(j).existsTo)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint internalId: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint id: "+ Objects.requireNonNull(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.id).value)));
                    }
                }
            }
            emitter.send(SseEmitter.event().data(createEventData("apn_test:")));
            for (int i = 0; i < response.get().instances.size(); i++) {
                for (int j = 0; j < response.get().instances.get(i).edgeInstance.size(); j++) {
                    if(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id.equals("apn:apn.apn_test")) {
                        emitter.send(SseEmitter.event().data(createEventData("internalId: "+response.get().instances.get(i).edgeInstance.get(j).internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("type: "+response.get().instances.get(i).edgeInstance.get(j).type)));
                        emitter.send(SseEmitter.event().data(createEventData("direction: "+response.get().instances.get(i).edgeInstance.get(j).direction)));
                        emitter.send(SseEmitter.event().data(createEventData("elementID: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id)));
                        emitter.send(SseEmitter.event().data(createEventData("existsFrom: "+response.get().instances.get(i).edgeInstance.get(j).existsFrom)));
                        emitter.send(SseEmitter.event().data(createEventData("existsTo: "+response.get().instances.get(i).edgeInstance.get(j).existsTo)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint internalId: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint id: "+ Objects.requireNonNull(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.id).value)));
                    }
                }
            }
            emitter.send(SseEmitter.event().data(createEventData("apn_prod:")));
            for (int i = 0; i < response.get().instances.size(); i++) {
                for (int j = 0; j < response.get().instances.get(i).edgeInstance.size(); j++) {
                    if(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id.equals("apn:apn.apn_prod")) {
                        emitter.send(SseEmitter.event().data(createEventData("internalId: "+response.get().instances.get(i).edgeInstance.get(j).internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("type: "+response.get().instances.get(i).edgeInstance.get(j).type)));
                        emitter.send(SseEmitter.event().data(createEventData("direction: "+response.get().instances.get(i).edgeInstance.get(j).direction)));
                        emitter.send(SseEmitter.event().data(createEventData("elementID: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.element.id)));
                        emitter.send(SseEmitter.event().data(createEventData("existsFrom: "+response.get().instances.get(i).edgeInstance.get(j).existsFrom)));
                        emitter.send(SseEmitter.event().data(createEventData("existsTo: "+response.get().instances.get(i).edgeInstance.get(j).existsTo)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint internalId: "+response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.internalId)));
                        emitter.send(SseEmitter.event().data(createEventData("endpoint id: "+ Objects.requireNonNull(response.get().instances.get(i).edgeInstance.get(j).edgeEndPoint.id).value)));
                    }
                }
            }
        }catch (java.lang.NullPointerException | java.lang.IndexOutOfBoundsException e){
            emitter.send(SseEmitter.event().data(createEventData("Vazby pro ckod: "+ckod+" nebyly nalezeny","warning")));
        }
    }


    public static void main(String simckodField, String dpuckodField, int mode, String inventory, String url, boolean safe, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        emitter.send(SseEmitter.event().data(createEventData("")));
        if(!handleInput(simckodField,dpuckodField, apolloClient, inventory,safe,emitter)){
            return;
        }
        //ziska aktivni ipset a apn - ulozi do activeapn/ipset
        getActiveType(ckod,apolloClient,emitter);
        //najde dostupne instance apn a ipset prod/test k dane sim, ulozi do patrinych promennych
        identify(ckod,apolloClient,emitter);

        switch(mode){
            case 0: // vypsat aktivni
                emitter.send(SseEmitter.event().data(createEventData("IPSet je již nastaven na: "+activeIpset)));
                emitter.send(SseEmitter.event().data(createEventData("APN je již nastavena na: " + activeAPN)));
                break;
            case 1: //prepnout IPSet na prod
                if(ipsetP==null){
                    emitter.send(SseEmitter.event().data(createEventData("Nelze přepnout na produkci, instance není vytvořena.","warning")));
                    return;
                }else {
                    switchIPSet(ckod,apolloClient,ipsetP,emitter);
                }
                break;
            case 2: // prepnout IPSet na test
                if(ipsetT==null){
                    emitter.send(SseEmitter.event().data(createEventData("Nelze přepnout na test, instance není vytvořena.","warning")));
                    return;
                }else {
                    switchIPSet(ckod,apolloClient,ipsetT,emitter);
                }
                break;
            case 3: // prepnout APN na prod
                if(apnP==null){
                    emitter.send(SseEmitter.event().data(createEventData("Nelze přepnout na produkci, instance není vytvořena.","warning")));
                    return;
                }else {
                    switchAPN(ckod,apolloClient,apnP,emitter);
                }
                break;
            case 4: //prepnout APN na test
                if(apnT==null){
                    emitter.send(SseEmitter.event().data(createEventData("Nelze přepnout na test, instance není vytvořena.","warning")));
                    return;
                }else {
                    switchAPN(ckod,apolloClient,apnT,emitter);
                }
                break;
            case 5: //vypsat assigned vazby
                getAssignedEdges(ckod,apolloClient,emitter);
                break;
        }

    }
}
