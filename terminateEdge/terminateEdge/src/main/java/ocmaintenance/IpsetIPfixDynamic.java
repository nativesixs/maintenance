package ocmaintenance;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Input;
import ocmaintenance.controllers.SharedMethods;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.entity.ContentType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ocmaintenance.controllers.SharedMethods.createEventData;
import static ocmaintenance.controllers.SharedMethods.createException;


public class IpsetIPfixDynamic {
    private static final String module = "IpsetIPfix";

    private static void findDupes(ApolloClient apolloClient, String inventory, int size, int offset, boolean pouzevypsat, SseEmitter emitter) throws IOException {
        try {
            Input<List<String>> ipsetKind = new Input<>(Collections.singletonList("ipset:ipset.ipset_test"),true);
            Input<List<String>> ipsetKind2 = new Input<>(Collections.singletonList("ipset:ipset.ipset_prod"),true);
            CompletableFuture<GetIpsetTestEdgesSimDupeQuery.Data> res = QueryHandler.execute(new GetIpsetTestEdgesSimDupeQuery(1, 0,ipsetKind,ipsetKind2), apolloClient);
            try {
                ArrayList<String> dupes = new ArrayList<>();
                int count = res.get().instancesSet.count;
                int limit = count / offset;
                if (count % offset != 0) {
                    limit = limit + 1;
                }
                for (int p = 0; p < limit; p++) {
                    if (p == limit - 1) {
                        size = count - ((limit - 1) * size);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("Várka: "+(p+1)+" / "+limit+"\n")));
                    ArrayList<String> deleteEdges = new ArrayList<>();
                    ArrayList<String> deleteIpsets = new ArrayList<>();
                    ArrayList<String> simckods = new ArrayList<>();
                    ArrayList<String> edgetypes = new ArrayList<>();
                    try {
                        CompletableFuture<GetIpsetTestEdgesSimDupeQuery.Data> response = QueryHandler.execute(new GetIpsetTestEdgesSimDupeQuery(size, offset * p,ipsetKind,ipsetKind2), apolloClient);
                        //prep data
                        ArrayList<ArrayList<String>> oldipset = new ArrayList<>();
                        ArrayList<ArrayList<String>> newipset = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            try {
                                for(int j=0;j<response.get().instancesSet.items.get(i).edgeInstance.size();j++) {
                                    if (response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.coreElement.id.equals("simcard")) {
                                        ArrayList<String> oneipset = new ArrayList<>();
                                        oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).snmp).normalizedValue);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).dlms).normalizedValue);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).iec104).normalizedValue);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).amm).normalizedValue);
                                        oneipset.add(response.get().instancesSet.items.get(i).ipsetId);
                                        oneipset.add(response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeId);
                                        oneipset.add(response.get().instancesSet.items.get(i).edgeInstance.get(j).type);
                                        oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.id).value);
                                        oldipset.add(oneipset); // ipset co ma vazbu na sim
                                    }
                                }
                            } catch (java.lang.IndexOutOfBoundsException s) {

                            }
                            if (Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value.contains("IPSET-TEST-")) {
                                ArrayList<String> oneipset = new ArrayList<>();
                                oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).snmp).normalizedValue);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).dlms).normalizedValue);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).iec104).normalizedValue);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).amm).normalizedValue);
                                oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value);
                                oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).ipsetId));
                                newipset.add(oneipset); // nove vytvoreny ipset co nemusi mit vazbu na sim
                            }
                        }
                        //compare for dupes
                        for (int i = 0; i < newipset.size(); i++) {
                            for (int j = 0; j < oldipset.size(); j++) {
//                                System.out.println(newipset.get(i).get(4)+" == "+ oldipset.get(j).get(4));
                                if (newipset.get(i).get(1).equals(oldipset.get(j).get(1))
                                        || newipset.get(i).get(2).equals(oldipset.get(j).get(2))
                                        || newipset.get(i).get(3).equals(oldipset.get(j).get(3))
                                        || newipset.get(i).get(4).equals(oldipset.get(j).get(4))) {
                                    if(!newipset.get(i).get(0).equals(oldipset.get(j).get(0))) {
                                        if (pouzevypsat) {
                                            String duplicate = "";
                                            if(newipset.get(i).get(1).equals(oldipset.get(j).get(1))){
                                                duplicate +="SNMP: "+newipset.get(i).get(1)+" == "+oldipset.get(j).get(1)+"\n";
                                            }
                                            if(newipset.get(i).get(2).equals(oldipset.get(j).get(2))){
                                                duplicate +="DLMS: "+newipset.get(i).get(2)+" == "+oldipset.get(j).get(2)+"\n";
                                            }
                                            if(newipset.get(i).get(3).equals(oldipset.get(j).get(3))){
                                                duplicate +="IEC104: "+newipset.get(i).get(3)+" == "+oldipset.get(j).get(3)+"\n";
                                            }
                                            if(newipset.get(i).get(4).equals(oldipset.get(j).get(4))){
                                                duplicate +="AMM: "+newipset.get(i).get(4)+" == "+oldipset.get(j).get(4)+"\n";
                                            }
                                            emitter.send(SseEmitter.event().data(createEventData("ipset: " + newipset.get(i).get(0) + " internalId: " + newipset.get(i).get(6) + "\nDuplicitní s ipset: " + oldipset.get(j).get(0) + " internalId: " + oldipset.get(j).get(5)+"\n"+duplicate)));
                                            dupes.add("ipset: " + newipset.get(i).get(0) + " internalId: " + newipset.get(i).get(6) + "\nDuplicitní s ipset: " + oldipset.get(j).get(0) + " internalId: " + oldipset.get(j).get(5) + "\n\n"+duplicate);
                                        } else {
                                            deleteEdges.add(oldipset.get(j).get(6));
                                            deleteIpsets.add(oldipset.get(j).get(5));
                                            simckods.add(newipset.get(i).get(8));
                                            edgetypes.add(oldipset.get(j).get(7));
                                        }
                                    }
                                }
                            }
                        }
                        if (pouzevypsat) {
                            for (int i = 0; i < size; i++) {
//                                if (response.get().instancesSet.items.get(i).edgeInstance.get(0).edgeEndPoint.coreElement.id.equals("simcard")) {
                                for(int j=0;response.get().instancesSet.items.get(i).edgeInstance.size()<j;j++) {
                                    if (response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.coreElement.id.equals("simcard")) {
                                        emitter.send(SseEmitter.event().data(createEventData("Nalezené ipset-test navazbene na sim: \n\n")));
                                        emitter.send(SseEmitter.event().data(createEventData("ipset: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value)));
                                        emitter.send(SseEmitter.event().data(createEventData("edge type: " + response.get().instancesSet.items.get(i).edgeInstance.get(0).type)));
//                                        emitter.send(SseEmitter.event().data(createEventData("edge to sim: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).edgeInstance.get(0).edgeEndPoint.id).value)));
                                        emitter.send(SseEmitter.event().data(createEventData("edge to sim: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.id).value)));
                                        emitter.send(SseEmitter.event().data(createEventData("snmp IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).snmp).normalizedValue)));
                                        emitter.send(SseEmitter.event().data(createEventData("dlms IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).dlms).normalizedValue)));
                                        emitter.send(SseEmitter.event().data(createEventData("iec104 IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).iec104).normalizedValue)));
                                        emitter.send(SseEmitter.event().data(createEventData("amm IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).amm).normalizedValue + "\n\n")));
                                    }
                                }
                            }
                        }

                        while (!response.isDone()) {
                            Thread.sleep(50);
                        }
                    } catch (java.lang.IndexOutOfBoundsException s) {
                        emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny zadne vysledky\n")));
                    }
//                    if (!pouzevypsat && deleteEdges.size() > 0 && deleteIpsets.size() > 0 && simckods.size() > 0 && edgetypes.size() > 0) {
//                        //oprava
//                        deleteEdge(inventory, deleteEdges, emitter); // mazani vazby ipset-HE group
//                        deleteIpset(inventory, deleteIpsets, emitter); // mazani ipsetu
//                        createEdgeToNewIpset(inventory, deleteIpsets, simckods, edgetypes, emitter); // vytvori vazbu sim-ipset co byl smazan v predchozim kroku
//                    } else if (!pouzevypsat) {
//                        emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny zadne duplicity\n")));
//                    }
                }
                if (pouzevypsat) {
                    emitter.send(SseEmitter.event().data(createEventData("Duplicitni ipset-test navazbene na sim: \n\n", "messageTop")));
                    ArrayList<String> eliminateCopies = new ArrayList<>();
                    for (int i = 0; i < dupes.size(); i++) {
                        if(!eliminateCopies.contains(dupes.get(i))) {
                            emitter.send(SseEmitter.event().data(createEventData(dupes.get(i), "messageTop")));
                            eliminateCopies.add(dupes.get(i));
                        }
                    }
                    eliminateCopies.clear();
                }
            }catch (Exception e){
                createException(e,"findDupes","IpsetIpfix - findDupes function exception, line: ",module,emitter);
            }
        }catch (Exception e){
            createException(e,"findDupes","Ipsetipfix - findDupes function query GetIpsetTestEdgesSimDupe, line: ",module,GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT,emitter);
        }
    }
    private static void fixDupes(ApolloClient apolloClient, String inventory, int size, int offset, boolean pouzevypsat, SseEmitter emitter) throws IOException {
        try {
            Input<List<String>> ipsetKind = new Input<>(Collections.singletonList("ipset:ipset.ipset_test"),true);
            CompletableFuture<GetIpsetTestEdgesSimDupeQuery.Data> res = QueryHandler.execute(new GetIpsetTestEdgesSimDupeQuery(1, 0,ipsetKind,ipsetKind), apolloClient);
            try {
                ArrayList<String> dupes = new ArrayList<>();
                int count = res.get().instancesSet.count;
                int limit = count / offset;
                if (count % offset != 0) {
                    limit = limit + 1;
                }
                for (int p = 0; p < limit; p++) {
                    if (p == limit - 1) {
                        size = count - ((limit - 1) * size);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("Várka: "+(p+1)+" / "+limit+"\n")));
                    ArrayList<String> deleteEdges = new ArrayList<>();
                    ArrayList<String> deleteIpsets = new ArrayList<>();
                    ArrayList<String> simckods = new ArrayList<>();
                    ArrayList<String> edgetypes = new ArrayList<>();
                    try {
                        CompletableFuture<GetIpsetTestEdgesSimDupeQuery.Data> response = QueryHandler.execute(new GetIpsetTestEdgesSimDupeQuery(size, offset * p,ipsetKind,ipsetKind), apolloClient);
                        //prep data
                        ArrayList<ArrayList<String>> oldipset = new ArrayList<>();
                        ArrayList<ArrayList<String>> newipset = new ArrayList<>();
                        for (int i = 0; i < size; i++) {
                            try {
                                for(int j=0;j<response.get().instancesSet.items.get(i).edgeInstance.size();j++) {
                                    if (response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.coreElement.id.equals("simcard")) {
                                        ArrayList<String> oneipset = new ArrayList<>();
                                        oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).snmp).normalizedValue);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).dlms).normalizedValue);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).iec104).normalizedValue);
                                        oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).amm).normalizedValue);
                                        oneipset.add(response.get().instancesSet.items.get(i).ipsetId);
                                        oneipset.add(response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeId);
                                        oneipset.add(response.get().instancesSet.items.get(i).edgeInstance.get(j).type);
                                        oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.id).value);
                                        oldipset.add(oneipset); // ipset co ma vazbu na sim
                                    }
                                }
                            } catch (java.lang.IndexOutOfBoundsException s) {

                            }
                            if (Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value.contains("IPSET-TEST-")) {
                                ArrayList<String> oneipset = new ArrayList<>();
                                oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).snmp).normalizedValue);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).dlms).normalizedValue);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).iec104).normalizedValue);
                                oneipset.add((String) Objects.requireNonNull(response.get().instancesSet.items.get(i).amm).normalizedValue);
                                oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value);
                                oneipset.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).ipsetId));
                                newipset.add(oneipset); // nove vytvoreny ipset co nemusi mit vazbu na sim
                            }
                        }
                        //compare for dupes
                        for (int i = 0; i < newipset.size(); i++) {
                            for (int j = 0; j < oldipset.size(); j++) {
//                                System.out.println(newipset.get(i).get(4)+" == "+ oldipset.get(j).get(4));
                                if (newipset.get(i).get(1).equals(oldipset.get(j).get(1))
                                        || newipset.get(i).get(2).equals(oldipset.get(j).get(2))
                                        || newipset.get(i).get(3).equals(oldipset.get(j).get(3))
                                        || newipset.get(i).get(4).equals(oldipset.get(j).get(4))) {
                                    if(!newipset.get(i).get(0).equals(oldipset.get(j).get(0))) {
                                        if (pouzevypsat) {
                                            emitter.send(SseEmitter.event().data(createEventData("ipset: " + newipset.get(i).get(0) + " internalId: " + newipset.get(i).get(6) + "\nDuplicitní s ipset: " + oldipset.get(j).get(0) + " internalId: " + oldipset.get(j).get(5))));
                                            dupes.add("ipset: " + newipset.get(i).get(0) + " internalId: " + newipset.get(i).get(6) + "\nDuplicitní s ipset: " + oldipset.get(j).get(0) + " internalId: " + oldipset.get(j).get(5) + "\n\n");
                                        } else {
                                            deleteEdges.add(oldipset.get(j).get(6));
                                            deleteIpsets.add(oldipset.get(j).get(5));
                                            simckods.add(newipset.get(i).get(8));
                                            edgetypes.add(oldipset.get(j).get(7));
                                        }
                                    }
                                }
                            }
                        }
                        if (pouzevypsat) {
                            for (int i = 0; i < size; i++) {
//                                if (response.get().instancesSet.items.get(i).edgeInstance.get(0).edgeEndPoint.coreElement.id.equals("simcard")) {
                                for(int j=0;response.get().instancesSet.items.get(i).edgeInstance.size()<j;j++) {
                                    if (response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.coreElement.id.equals("simcard")) {
                                        emitter.send(SseEmitter.event().data(createEventData("Nalezené ipset-test navazbene na sim: \n\n")));
                                        emitter.send(SseEmitter.event().data(createEventData("ipset: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value)));
                                        emitter.send(SseEmitter.event().data(createEventData("edge type: " + response.get().instancesSet.items.get(i).edgeInstance.get(0).type)));
//                                        emitter.send(SseEmitter.event().data(createEventData("edge to sim: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).edgeInstance.get(0).edgeEndPoint.id).value)));
                                        emitter.send(SseEmitter.event().data(createEventData("edge to sim: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.id).value)));
                                        emitter.send(SseEmitter.event().data(createEventData("snmp IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).snmp).normalizedValue)));
                                        emitter.send(SseEmitter.event().data(createEventData("dlms IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).dlms).normalizedValue)));
                                        emitter.send(SseEmitter.event().data(createEventData("iec104 IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).iec104).normalizedValue)));
                                        emitter.send(SseEmitter.event().data(createEventData("amm IP: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).amm).normalizedValue + "\n\n")));
                                    }
                                }
                            }
                        }

                        while (!response.isDone()) {
                            Thread.sleep(50);
                        }
                    } catch (java.lang.IndexOutOfBoundsException s) {
                        emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny zadne vysledky\n")));
                    }
                    if (!pouzevypsat && deleteEdges.size() > 0 && deleteIpsets.size() > 0 && simckods.size() > 0 && edgetypes.size() > 0) {
                        //oprava
                        deleteEdge(inventory, deleteEdges, emitter); // mazani vazby ipset-HE group
                        deleteIpset(inventory, deleteIpsets, emitter); // mazani ipsetu
                        createEdgeToNewIpset(inventory, deleteIpsets, simckods, edgetypes, emitter); // vytvori vazbu sim-ipset co byl smazan v predchozim kroku
                    } else if (!pouzevypsat) {
                        emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny zadne duplicity\n")));
                    }
                }
                if (pouzevypsat) {
                    emitter.send(SseEmitter.event().data(createEventData("Duplicitni ipset-test navazbene na sim: \n\n", "messageTop")));
                    ArrayList<String> eliminateCopies = new ArrayList<>();
                    for (int i = 0; i < dupes.size(); i++) {
                        if(!eliminateCopies.contains(dupes.get(i))) {
                            emitter.send(SseEmitter.event().data(createEventData(dupes.get(i), "messageTop")));
                            eliminateCopies.add(dupes.get(i));
                        }
                    }
                    eliminateCopies.clear();
                }
            }catch (Exception e){
                createException(e,"fixDupes","IpsetIpfix - fixDupes function exception, line: ",module,emitter);
            }
        }catch (Exception e){
            createException(e,"fixDupes","Ipsetipfix - fixDupes function query GetIpsetTestEdgesSimDupe, line: ",module,GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT,emitter);
        }
    }
    private static void deleteEdge(String inventory,ArrayList<String> edgeInternalIds,SseEmitter emitter) throws IOException {
        // edge == internalId
        String deledge="DELETE_EDGE:\n";
        for (String edgeInternalId : edgeInternalIds) {
            deledge = deledge + "{ \"id\": \"" + edgeInternalId + "\" ,\"exists_to\": \"now\"}";
        }
        try {
            String rep = String.valueOf(Request.Post(inventory).bodyString(deledge, ContentType.DEFAULT_TEXT).execute().returnResponse().getStatusLine());
            emitter.send(SseEmitter.event().data(createEventData("Mazani vazeb: "+rep)));
        } catch (Exception e) {
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            emitter.send(SseEmitter.event().data(createEventData("Ipsetipfix - deleteEdge function exception - deletion request failed")));
//            SharedMethods.exceptionInServerLog(e,module,"deleteEdge", GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT);
            createException(e,"deleteEdge","IpsetIpfix - deleteEdge function exception, line: ",module,emitter);
        }

    }

    private static void deleteIpset(String inventory,ArrayList<String> ipsetInternalIds,SseEmitter emitter) throws IOException {
        String deledge="DELETE_INSTANCE:\n";
        for (String ipsetInternalId : ipsetInternalIds) {
            deledge = deledge + "{ \"id\": \"" + ipsetInternalId + "\" ,\"exists_to\": \"now\"}";
        }
        try {
            String rep = String.valueOf(Request.Post(inventory).bodyString(deledge, ContentType.DEFAULT_TEXT).execute().returnResponse().getStatusLine());
            emitter.send(SseEmitter.event().data(createEventData("Mazani ipsetu: "+rep)));
        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            emitter.send(SseEmitter.event().data(createEventData("Ipsetipfix - deleteIpset function exception - delete request failed")));
//            SharedMethods.exceptionInServerLog(e,module,"deleteIpset", GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT);
            createException(e,"deleteIpset","IpsetIpfix - deleteIpset function exception, line: ",module,emitter);
        }
    }

    private static void createEdgeToNewIpset(String inventory, ArrayList<String> newIpsetCkods, ArrayList<String> simCkods, ArrayList<String> edgeTypes,SseEmitter emitter) throws IOException {
        String edge = "CREATE_EDGE:\n";
        for(int i=0;i<newIpsetCkods.size();i++) {
            edge = edge + "{\"type\": \"" + edgeTypes.get(i) + "\", \"from\": {\"ext_id_did\": \"information:attribute.telefonni_cislo\", \"information:attribute.telefonni_cislo\": \"" + simCkods.get(i) + "\"}, \"to\": {\"ext_id_did\": \"information:information_technology.network.protocols.ipset.ipset_id\", \"information:information_technology.network.protocols.ipset.ipset_id\": \"" + newIpsetCkods.get(i) + "\"}}";
        }
        try {
            String rep = String.valueOf(Request.Post(inventory).bodyString(edge, ContentType.DEFAULT_TEXT).execute().returnResponse().getStatusLine());
            emitter.send(SseEmitter.event().data(createEventData("Vyvoreni vazby novemu ipset: "+rep)));
        }catch ( Exception e){
//            emitter.send(SseEmitter.event().data(createEventData("Ipsetipfix - createEdgeToNewIpset function exception - create edge failed")));
//            SharedMethods.exceptionInServerLog(e,module,"createEdgeToNewIpset", GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT);
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            createException(e,"createEdgeToNewIpset","IpsetIpfix - createEdgeToNewIpset function exception, line: ",module,emitter);
        }
    }

    private static void listIpsetsWithoutEdges(ApolloClient apolloClient, int size, int offset, SseEmitter emitter) throws IOException {
        try {
            Input<List<String>> ipsetKind = new Input<>(Collections.singletonList("ipset:ipset.ipset_test"),true);
            CompletableFuture<GetIpsetTestEdgesSimDupeQuery.Data> res = QueryHandler.execute(new GetIpsetTestEdgesSimDupeQuery(1, 0,ipsetKind,ipsetKind), apolloClient);
            int count = res.get().instancesSet.count;
            int limit = count / offset;
            if (count % offset != 0) {
                limit = limit + 1;
            }
            emitter.send(SseEmitter.event().data(createEventData("Nalezeno ipsetu: "+count+"\n\n")));
            emitter.send(SseEmitter.event().data(createEventData("ipsety bez vazby na sim: \n\n")));
            for (int p = 0; p < limit; p++) {
                if(p==limit-1){
                    size = count-((limit-1)*size);
                }
                try {
                    CompletableFuture<GetIpsetTestEdgesSimDupeQuery.Data> response = QueryHandler.execute(new GetIpsetTestEdgesSimDupeQuery(size, offset * p,ipsetKind,ipsetKind), apolloClient);
                    try {
                        for (int i = 0; i < size; i++) {
                            boolean hasSim=false;
                            for(int j=0;j<response.get().instancesSet.items.get(i).edgeInstance.size();j++){
                                if (response.get().instancesSet.items.get(i).edgeInstance.get(j).edgeEndPoint.coreElement.id.equals("simcard")) {
                                    hasSim=true;
                                }
                            }
                            if(!hasSim) {
                                emitter.send(SseEmitter.event().data(createEventData("id: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value)));
                                emitter.send(SseEmitter.event().data(createEventData("internalId: " + response.get().instancesSet.items.get(i).ipsetId + "\n")));
                            }
                        }
                        while (!response.isDone()) {
                            Thread.sleep(50);
                        }
                    } catch (java.lang.IndexOutOfBoundsException s) {
//                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(s))));
//                        emitter.send(SseEmitter.event().data(createEventData("Ipsetipfix - listIpsetsWithoutEdges function exception - GetIpsetTestEdgesSimDupe query failed")));
//                        SharedMethods.exceptionInServerLog(s,module,"listIpsetsWithoutEdges", GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT);
                        createException(s,"listIpsetsWithoutEdges","IpsetIpfix - listIpsetsWithoutEdges function exception, line: ",module,emitter);
                    }
                } catch (java.lang.IndexOutOfBoundsException s) {
//                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(s))));
//                    emitter.send(SseEmitter.event().data(createEventData("Ipsetipfix - listIpsetsWithoutEdges function exception - GetIpsetTestEdgesSimDupe query failed")));
//                    SharedMethods.exceptionInServerLog(s,module,"listIpsetsWithoutEdges", GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT);
                    createException(s,"listIpsetsWithoutEdges","Ipsetipfix - listIpsetsWithoutEdges function query GetIpsetTestEdgesSimDupeQuery, line: ",module,GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT,emitter);
                }
            }
        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            emitter.send(SseEmitter.event().data(createEventData("Ipsetipfix - listIpsetsWithoutEdges function exception - GetIpsetTestEdgesSimDupe query failed")));
//            SharedMethods.exceptionInServerLog(e,module,"listIpsetsWithoutEdges", GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT);
            createException(e,"listIpsetsWithoutEdges","Ipsetipfix - listIpsetsWithoutEdges function query GetIpsetTestEdgesSimDupeQuery, line: ",module,GetIpsetTestEdgesSimDupeQuery.QUERY_DOCUMENT,emitter);

        }
    }

    private static void setActiveWanToApnProd(String inventory,ArrayList<String> simnumbers, ArrayList<String> apnprodids,SseEmitter emitter) throws IOException {
//        String edge = "CREATE_EDGE: {\"type\": \"active_wan\"}\n";
        String edge = "CREATE_EDGE: {\"type\": \"active_wan\"}";
        //todo otestovat na vice simkach najednou
        for(int i=0;i<simnumbers.size();i++) {
            edge = edge + "\n{\"from\":{\"ext_id_did\":\"information:attribute.telefonni_cislo\",\"information:attribute.telefonni_cislo\":\""+simnumbers.get(i)+"\"},\"to\":{\"ext_id_did\":\"information:information_technology.network.interfaces.mobile.apn.apn_id\",\"information:information_technology.network.interfaces.mobile.apn.apn_id\":\""+apnprodids.get(i)+"\"},\"exists_from\":\"now\"}";
        }

        try {
            String rep = String.valueOf(Request.Post(inventory).bodyString(edge, ContentType.DEFAULT_TEXT).execute().returnContent().asString());
            emitter.send(SseEmitter.event().data(createEventData(rep)));
        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            emitter.send(SseEmitter.event().data(createEventData("IpsetIPfix - setActiveWanToApnProd function exception - post request failed: "+edge)));
//            SharedMethods.exceptionInServerLog(e,module,"setActiveWanToApnProd", "create edge request failed");
            createException(e,"setActiveWanToApnProd","IpsetIpfix - setActiveWanToApnProd function exception, line: ",module,emitter);
        }
    }

    private static void fixActiveWan(ApolloClient apolloClient, String inventory, int size, int offset, boolean pouzevypsat, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        try {
            CompletableFuture<GetActiveTunnelsWithIpsetProdQuery.Data> res = QueryHandler.execute(new GetActiveTunnelsWithIpsetProdQuery(1, 0), apolloClient);
            int count = res.get().instancesSet.count;
            int limit = count / offset;
            if (count % offset != 0) {
                limit = limit + 1;
            }
            for (int p = 0; p < limit; p++) {
                if(p==limit-1){
                    size = count-((limit-1)*size);
                }
                ArrayList<String> simIdValues = new ArrayList<>();
                ArrayList<String> apnProdIdValues = new ArrayList<>();
                try {
                    CompletableFuture<GetActiveTunnelsWithIpsetProdQuery.Data> response = QueryHandler.execute(new GetActiveTunnelsWithIpsetProdQuery(size, offset * p), apolloClient);
                    try {
                        for (int i = 0; i < size; i++) {
                            try {
                                if (
                                        response.get().instancesSet.items.get(i).edgeInstance.get(0).edgeEndPoint.element.id.equals("ipset:ipset.ipset_prod")
                                                && !Objects.requireNonNull(response.get().instancesSet.items.get(i).currentApn.get(0).edgeEndPoint.id).value.equals(Objects.requireNonNull(response.get().instancesSet.items.get(i).apn_prod.get(0).id).value)
                                ) {
                                    ArrayList<String> internalIds = new ArrayList<>(Collections.singleton(response.get().instancesSet.items.get(i).currentApn.get(0).internalId));
                                    if (pouzevypsat) {
                                        emitter.send(SseEmitter.event().data(createEventData(Objects.requireNonNull(response.get().instancesSet.items.get(i).simId).value)));
                                        emitter.send(SseEmitter.event().data(createEventData("Aktualni APN: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).currentApn.get(0).edgeEndPoint.id).value + " priradit APN: " + Objects.requireNonNull(response.get().instancesSet.items.get(i).apn_prod.get(0).id).value)));
                                    } else {
                                        //vzit simID a APN_prodID
                                        simIdValues.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).simId).value);
                                        apnProdIdValues.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).apn_prod.get(0).id).value);
                                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(internalIds, "active_wan", inventory, apolloClient, false, true)))));
                                    }
                                } else {
                                    emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny sim s ipset-prod a apn-test.")));
                                    return;
                                }
                            } catch (Exception e) {
//                            emitter.send(e);
                            }
                            //uncomment to create apn prod and create edge to sim if none apn prod detected
//            try {
//                String sp = Objects.requireNonNull(response.get().instances.get(i).apn_prod.get(0).id).value;
//                System.out.println(sp);
//            }catch (java.lang.IndexOutOfBoundsException e){
//                try {
//                    if (
//                            response.get().instances.get(i).edgeInstance.get(0).edgeEndPoint.element.id.equals("ipset:ipset.ipset_prod")
//                    ) {
//                        setActiveWanToApnProd(inventory,response.get().instances.get(i).simId.value);
//                    }
//                }catch (IndexOutOfBoundsException er){
//
//                }
//            }
                        }
                        while (!response.isDone()) {
                            Thread.sleep(50);
                        }
                    }catch (Exception e){
                        createException(e,"fixActiveWan","IpsetIpfix - fixActiveWan function exception, line: ",module,emitter);
                    }
                } catch (Exception e){
//                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//                    emitter.send(SseEmitter.event().data(createEventData("IpsetIPfix - fixActiveWan function exception - GetActiveTunnelsWithIpsetProd query failed")));
//                    SharedMethods.exceptionInServerLog(e,module,"fixActiveWan", GetActiveTunnelsWithIpsetProdQuery.QUERY_DOCUMENT);
                    createException(e,"fixActiveWan","IpsetIpfix - fixActiveWan function query GetActiveTunnelsWithIpsetProdQuery, line: ",module,GetActiveTunnelsWithIpsetProdQuery.QUERY_DOCUMENT,emitter);
                }
                if(!pouzevypsat && simIdValues.size()>0 && apnProdIdValues.size()>0) {
                    setActiveWanToApnProd(inventory, simIdValues, apnProdIdValues,emitter);
                } else if (!pouzevypsat) {
                    emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny sim s ipset_prod && !apn_prod")));
                }
            }
        }catch (IndexOutOfBoundsException e){
//            emitter.send(SseEmitter.event().data(createEventData("Nebylo mozno ziskat alespon 1 vysledek query")));
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            SharedMethods.exceptionInServerLog(e,module,"fixActiveWan", GetActiveTunnelsWithIpsetProdQuery.QUERY_DOCUMENT);
            createException(e,"fixActiveWan","IpsetIpfix - fixActiveWan function query GetActiveTunnelsWithIpsetProdQuery, line: ",module,GetActiveTunnelsWithIpsetProdQuery.QUERY_DOCUMENT,emitter);
        }
    }

    private static String makeHEGroupProdName(int c){
        String hegroupNum = String.format("%02d", c);
        return "HE-GROUP-PROD-"+hegroupNum;
    }


    private static void createEdgeIpsetProdHEGroup(String inventory, ArrayList<String> ipsety, ArrayList<String> ipsetidvalue,SseEmitter emitter) throws IOException {
        String edge = "CREATE_EDGE:\n";
        for (String ipset : ipsety) {
            edge = edge + ipset + "\n";
        }
        try {
            Response r = Request.Post(inventory).bodyString(edge, ContentType.DEFAULT_TEXT).execute();
            String rr = r.returnContent().asString();
            emitter.send(SseEmitter.event().data(createEventData("Pairing: "+ipsetidvalue)));
            emitter.send(SseEmitter.event().data(createEventData(rr)));
        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            emitter.send(SseEmitter.event().data(createEventData("IpsetIPfix - createEdgeIpsetProdHEGroup function exception - create edge request failed")));
//            SharedMethods.exceptionInServerLog(e,module,"createEdgeIpsetProdHEGroup", "create edge request failed");
            createException(e,"createEdgeIpsetProdHEGroup","IpsetIpfix - createEdgeIpsetProdHEGroup function exception, line: ",module,emitter);
        }

    }


    private static void fixIpsetProdWithoutHEGroup(ApolloClient apolloClient, String inventory, int size, int offset, boolean pouzevypsat, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        try {
            CompletableFuture<GetIpsetProdWithoutHEGroupsAssignedQuery.Data> res = QueryHandler.execute(new GetIpsetProdWithoutHEGroupsAssignedQuery(1, 0), apolloClient);
            int count = res.get().instancesSet.count;
            int limit = count / offset;
            if (count % offset != 0) {
                limit = limit + 1;
            }
            emitter.send(SseEmitter.event().data(createEventData("Ipsety (prod) bez he_group: "+count)));
            for (int p = 0; p < limit; p++) {
                if(p==limit-1){
                    size = count-((limit-1)*size);
                }
                try {
                    CompletableFuture<GetIpsetProdWithoutHEGroupsAssignedQuery.Data> response = QueryHandler.execute(new GetIpsetProdWithoutHEGroupsAssignedQuery(size, offset * p), apolloClient);
                    try {
                        int c = 1;
                        ArrayList<String> ipsety = new ArrayList<>();
                        ArrayList<String> ipsetidvalue = new ArrayList<>();
                        emitter.send(SseEmitter.event().data(createEventData("\n")));
                        for (int i = 0; i < size; i++) {
                            String hegroupName = makeHEGroupProdName(c);
                            c += 1;
                            if (c == 25) {
                                c = 1;
                            }
                            if (pouzevypsat) {
                                emitter.send(SseEmitter.event().data(createEventData(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value + " priradit group: " + hegroupName)));
                            } else {
                                ipsety.add("{\"type\": \"assigned\", \"from\": {\"ext_id_did\": \"information:information_technology.network.protocols.ipset.ipset_id\", \"information:information_technology.network.protocols.ipset.ipset_id\": \"" + Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value + "\"}, \"to\": {\"ext_id_did\": \"information:information_technology.network.he_group.name\", \"information:information_technology.network.he_group.name\": \"" + hegroupName + "\"}}");
                                ipsetidvalue.add(Objects.requireNonNull(response.get().instancesSet.items.get(i).id).value + "\n");
                            }
                        }
                        while (!response.isDone()) {
                            Thread.sleep(50);
                        }
                        if (!pouzevypsat && ipsety.size() > 0 && ipsetidvalue.size() > 0) {
                            createEdgeIpsetProdHEGroup(inventory, ipsety, ipsetidvalue, emitter);
                        } else if (!pouzevypsat) {
                            emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny ipsety bez HE_Group")));
                        }
                    }catch (Exception e){
                        createException(e,"fixIpsetProdWithoutHEGroup","IpsetIpfix - fixIpsetProdWithoutHEGroup function exception, line: ",module,emitter);
                    }
                } catch (Exception e) {
//                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//                    emitter.send(SseEmitter.event().data(createEventData("IpsetIPfix - fixIpsetProdWithoutHEGroup function exception - GetIpsetProdWithoutHEGroupsAssignedQuery query failed")));
//                    SharedMethods.exceptionInServerLog(e,module,"fixIpsetProdWithoutHEGroup", GetIpsetProdWithoutHEGroupsAssignedQuery.QUERY_DOCUMENT);
                    createException(e,"fixIpsetProdWithoutHEGroup","IpsetIpfix - fixIpsetProdWithoutHEGroup query GetIpsetProdWithoutHEGroupsAssignedQuery, line: ",module,GetIpsetProdWithoutHEGroupsAssignedQuery.QUERY_DOCUMENT,emitter);
                }
            }
        }catch (java.lang.IndexOutOfBoundsException | java.lang.NullPointerException e) {
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            emitter.send(SseEmitter.event().data(createEventData("IpsetIPfix - fixIpsetProdWithoutHEGroup function exception - GetIpsetProdWithoutHEGroupsAssignedQuery query failed")));
//            SharedMethods.exceptionInServerLog(e,module,"fixIpsetProdWithoutHEGroup", GetIpsetProdWithoutHEGroupsAssignedQuery.QUERY_DOCUMENT);
            createException(e,"fixIpsetProdWithoutHEGroup","IpsetIpfix - fixIpsetProdWithoutHEGroup query GetIpsetProdWithoutHEGroupsAssignedQuery, line: ",module,GetIpsetProdWithoutHEGroupsAssignedQuery.QUERY_DOCUMENT,emitter);
        }
    }

    public static void main(String inventory, String url, int mode, int delset, int sizeField, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        if(sizeField==0){
            sizeField=999999999;
        } else if (sizeField<0) {
            emitter.send(SseEmitter.event().data(createEventData("Query size nemuze mit zapornou velikost")));
            return;
        }
        int size = sizeField, offset = size;

        switch (mode) {
            case 0: //vypsat nikam nenavazbene ipsety
                listIpsetsWithoutEdges(apolloClient,size,offset,emitter);
                break;
            case 1: //migrace na nove ipset-test
                if (delset == 0) {
                    fixDupes(apolloClient, inventory, size, offset, true, emitter);
                }else{
                    fixDupes(apolloClient, inventory, size, offset, false, emitter);
                }
                break;
            case 2: //oprava if ipset-prod, apn_test -> apn_prod
                if (delset == 0) {
                    fixActiveWan(apolloClient, inventory, size, offset, true, emitter);
                }else {
                    fixActiveWan(apolloClient, inventory, size, offset, false, emitter);
                }
                break;
            case 3: //oprava privazbeni HE_GROUPS 1-24 pro ipset.prod ktere jeste nemaji HE_GROUP privazbenou
                if (delset == 0) {
                    fixIpsetProdWithoutHEGroup(apolloClient, inventory, size, offset, true, emitter);
                }else {
                    fixIpsetProdWithoutHEGroup(apolloClient, inventory, size, offset, false, emitter);
                }
                break;
            case 4: // nalezeni duplicitnich adres ipsetu
                if (delset == 0) {
                    findDupes(apolloClient, inventory, size, offset, true, emitter);
                }else{
                    findDupes(apolloClient, inventory, size, offset, true, emitter);
                }
            break;
        }


    }


}
