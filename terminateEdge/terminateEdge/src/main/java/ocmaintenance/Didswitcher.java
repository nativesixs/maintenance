package ocmaintenance;

import static ocmaintenance.controllers.SharedMethods.createEventData;
import static ocmaintenance.controllers.SharedMethods.createException;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Input;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import ocmaintenance.controllers.SharedMethods;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class Didswitcher {
    private static String url = null;
    private static String ckod = null;
    private static String inventory = null;
    private static final String module = "Didswitcher";

    private static boolean verifyDids(String didOne, String didTwo,ApolloClient apolloClient,SseEmitter emitter) throws IOException {
        Input<List<String>> didOneIn = new Input<>(new ArrayList<>(Collections.singleton(didOne)), true);
        Input<List<String>> didTwoIn = new Input<>(new ArrayList<>(Collections.singleton(didTwo)), true);
        boolean one = false,two = false;
        try {
            CompletableFuture<VerifyDidExistsQuery.Data> didoneresult = QueryHandler.execute(new VerifyDidExistsQuery(didOneIn), apolloClient);
            try {
                if (didoneresult.get().dids.get(0).id.equals(didOne)) {
                    emitter.send(SseEmitter.event().data(createEventData("DID 1:" + didOne + " nalezen", "success")));
                    one = true;
                } else {
                    emitter.send(SseEmitter.event().data(createEventData("DID 1:" + didOne + " nenalezen", "error")));
                    one = false;
                }
            }catch (Exception e){
                emitter.send(SseEmitter.event().data(createEventData("DID 1:" + didOne + " nenalezen", "error")));
            }
        } catch (Exception e) {
            createException(e, "verifyDids", "Didswitcher - verifyDids function exception - VerifyDidExistsQuery failed, exception line: ", module, VerifyDidExistsQuery.QUERY_DOCUMENT, emitter);
        }
        try {
            CompletableFuture<VerifyDidExistsQuery.Data> didtworesult = QueryHandler.execute(new VerifyDidExistsQuery(didTwoIn), apolloClient);
            try {
                if (didtworesult.get().dids.get(0).id.equals(didTwo)) {
                    emitter.send(SseEmitter.event().data(createEventData("DID 2:" + didTwo + " nalezen", "success")));
                    two = true;
                } else {
                    emitter.send(SseEmitter.event().data(createEventData("DID 2:" + didTwo + " nenalezen", "error")));
                    two = false;
                }
            }catch (Exception e){
                emitter.send(SseEmitter.event().data(createEventData("DID 2:" + didTwo + " nenalezen", "error")));
            }
        } catch (Exception e) {
            createException(e, "verifyDids", "Didswitcher - verifyDids function exception - VerifyDidExistsQuery failed, exception line: ", module, VerifyDidExistsQuery.QUERY_DOCUMENT, emitter);
         }

        if(one && two){
            return true;
        }else{
            return false;
        }
    }

    private static ArrayList<ArrayList<String>> getListOfDids(String type, String didOne, String didTwo,int size, int offset,boolean requireDidTwoNull, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        Input<List<String>> typeIn = new Input<>(new ArrayList<>(Collections.singleton(type)), true);
        ArrayList<ArrayList<String>> transferList = new ArrayList<>();
        try{
            CompletableFuture<GetInstancesOfTypeToTransferDidsValueQuery.Data> ref = QueryHandler.execute(new GetInstancesOfTypeToTransferDidsValueQuery(typeIn,didOne,didTwo,1,0), apolloClient);
            int count = ref.get().instancesSet.count;
            int limit = count / offset;
            if (count % offset != 0) {
                limit = limit + 1;
            }
            for (int p = 0; p < limit; p++) {
                if (p == limit - 1) {
                    size = count - ((limit - 1) * size);
                }
                try{
                    CompletableFuture<GetInstancesOfTypeToTransferDidsValueQuery.Data> results = QueryHandler.execute(new GetInstancesOfTypeToTransferDidsValueQuery(typeIn,didOne,didTwo,size,offset*p), apolloClient);
                    try {
                        if(requireDidTwoNull){
                            for(int i=0;i<size;i++){
                                ArrayList<String> cells = new ArrayList<>();
                                Object didOneValue=results.get().instancesSet.items.get(i).didOne.normalizedValue;
                                Object didTwoValue=results.get().instancesSet.items.get(i).didTwo.normalizedValue;
                                // didOneValue needs to have value and didTwo needs to be null or empty
                                if (didOneValue!=null && didTwoValue==null && !ObjectUtils.isEmpty(didOneValue) && ObjectUtils.isEmpty(didTwoValue)) {
                                    cells.add(results.get().instancesSet.items.get(i).internalId);
                                    cells.add(results.get().instancesSet.items.get(i).id.value);
                                    cells.add(results.get().instancesSet.items.get(i).didOne.did.id);
                                    cells.add((String) results.get().instancesSet.items.get(i).didTwo.did.id);
                                    cells.add((String) results.get().instancesSet.items.get(i).didOne.normalizedValue);
                                    if (cells.size() >= 4) {
                                        transferList.add(cells);
                                    }
                                }
                            }
                        }else {
                            for (int i = 0; i < size; i++) {
                                ArrayList<String> cells = new ArrayList<>();
                                Object didOneValue=results.get().instancesSet.items.get(i).didOne.normalizedValue;
                                // didTwoValue can by anything but didOneValue needs to always be non null and not empty
                                if (didOneValue!=null && !ObjectUtils.isEmpty(didOneValue)) {
                                    cells.add(results.get().instancesSet.items.get(i).internalId);
                                    cells.add(results.get().instancesSet.items.get(i).id.value);
                                    cells.add(results.get().instancesSet.items.get(i).didOne.did.id);
                                    cells.add((String) results.get().instancesSet.items.get(i).didTwo.did.id);
                                    cells.add((String) results.get().instancesSet.items.get(i).didOne.normalizedValue);

                                    if (cells.size() >= 4) {
                                        transferList.add(cells);
                                    }
                                }
                            }
                        }
                    }catch (Exception e){
                        createException(e, "getListOfDids", "Didswitcher - getListOfDids function exception, exception line: ", module, emitter);
                    }

                }catch (Exception e){
                    createException(e, "getListOfDids", "Didswitcher - getListOfDids function exception - GetInstancesOfTypeToTransferDidsValueQuery failed, exception line: ", module, GetInstancesOfTypeToTransferDidsValueQuery.QUERY_DOCUMENT, emitter);
                }
            }

        }catch (Exception e){
            createException(e, "getListOfDids", "Didswitcher - getListOfDids function exception - GetInstancesOfTypeToTransferDidsValueQuery failed, exception line: ", module, GetInstancesOfTypeToTransferDidsValueQuery.QUERY_DOCUMENT, emitter);
        }
        return transferList;

    }

    private static void copyDidOneToDidTwo(ArrayList<ArrayList<String>> foundDids, ApolloClient apolloClient, int size, int offset, SseEmitter emitter) throws IOException {
        String updateInstance = "";
        int c = 0;
        for(int i=0;i<foundDids.size();i++){
            String internalId = foundDids.get(i).get(0);
            // String didOne = foundDids.get(i).get(2);
            String didTwo = foundDids.get(i).get(3);
            String didTwoValue = foundDids.get(i).get(4);
            updateInstance = updateInstance + "UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + didTwo + "\":\"" + didTwoValue + "\"}" + "\n";
            c++;
            if(c==size){
                c=0;
                // emitter.send(SseEmitter.event().data(createEventData(updateInstance)));
                SharedMethods.updateInstanceParsed(updateInstance, inventory, emitter);
                updateInstance="";
            }
        }
        if(updateInstance.length()>=2){
            // emitter.send(SseEmitter.event().data(createEventData(updateInstance)));
           SharedMethods.updateInstanceParsed(updateInstance, inventory, emitter);
            updateInstance="";
        }
    }

    private static void moveDidOneToDidTwo(ArrayList<ArrayList<String>> foundDids,String newDidOneValue, ApolloClient apolloClient, int size, int offset, SseEmitter emitter) throws IOException {
        String updateInstance = "";
        int c = 0;
        for(int i=0;i<foundDids.size();i++){
            String internalId = foundDids.get(i).get(0);
            String didOne = foundDids.get(i).get(2);
            String didTwo = foundDids.get(i).get(3);
            String didTwoValue = foundDids.get(i).get(4);
            updateInstance = updateInstance + "UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + didTwo + "\":\"" + didTwoValue + "\"}" + "\n";
            updateInstance = updateInstance + "UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + didOne + "\":\"" + newDidOneValue + "\"}" + "\n";
            c++;
            if(c==size){
                c=0;
                // emitter.send(SseEmitter.event().data(createEventData(updateInstance)));
                SharedMethods.updateInstanceParsed(updateInstance, inventory, emitter);
                updateInstance="";
            }
        }
        if(updateInstance.length()>=2){
            // emitter.send(SseEmitter.event().data(createEventData(updateInstance)));
            SharedMethods.updateInstanceParsed(updateInstance, inventory, emitter);
            updateInstance="";
        }
    }

    private static void printFoundWithoutAction(ArrayList<ArrayList<String>> foundDids, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        emitter.send(SseEmitter.event().data(createEventData("Nalezené změny pro fix: "+foundDids.size())));
        for(int i=0;i<foundDids.size();i++){
            String internalId = foundDids.get(i).get(0);
            String did = foundDids.get(i).get(3);
            String didValue = foundDids.get(i).get(4);
            emitter.send(SseEmitter.event().data(createEventData("UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + did + "\":\"" + didValue + "\"}")));
        }
    }

    private static void printFoundWithoutAction(ArrayList<ArrayList<String>> foundDids,String newDidOneValue, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        emitter.send(SseEmitter.event().data(createEventData("Nalezené změny pro fix: "+foundDids.size())));
        for(int i=0;i<foundDids.size();i++){
            String internalId = foundDids.get(i).get(0);
            String didOne = foundDids.get(i).get(2);
            String didTwo = foundDids.get(i).get(3);
            String didTwoValue = foundDids.get(i).get(4);
            emitter.send(SseEmitter.event().data(createEventData("UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + didTwo + "\":\"" + didTwoValue + "\"}")));
            emitter.send(SseEmitter.event().data(createEventData("UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + didOne + "\":\"" + newDidOneValue + "\"}")));
        }
    }

     public static void main(String didOne, String didTwo,Integer mode,Integer sizeField, boolean requireDidTwoNull, String coreElementId, String inventory, String urlConf, SseEmitter emitter) throws IOException {
        url = urlConf;
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();

        if(sizeField==0){
            sizeField=999999999;
        } else if (sizeField<0) {
            emitter.send(SseEmitter.event().data(createEventData("Query size nemuze mit zapornou velikost")));
            return;
        }
        int size = sizeField, offset = size;

        switch(mode){
            case 0: // overit didy
                verifyDids(didOne,didTwo,apolloClient,emitter);
            break;
            case 1:
                //todo
                break;
            case 2: // kopirovat hodnotu did1 do did2
                if(verifyDids(didOne,didTwo,apolloClient,emitter)) {
                    ArrayList<ArrayList<String>> foundDids = new ArrayList<>(getListOfDids(coreElementId, didOne, didTwo, size, offset,requireDidTwoNull, apolloClient, emitter));
                    copyDidOneToDidTwo(foundDids, apolloClient, size, offset, emitter);

                }
            break;
            case 3: // vypssat kopirovani hodnot did1 do did2 bez provedeni
                if(verifyDids(didOne,didTwo,apolloClient,emitter)) {
                    ArrayList<ArrayList<String>> foundDids = new ArrayList<>(getListOfDids(coreElementId, didOne, didTwo, size, offset,requireDidTwoNull, apolloClient, emitter));
                    printFoundWithoutAction(foundDids,apolloClient,emitter);
                }
            break;
            case 4: // presunout hodnotu did1 do did2
                if(verifyDids(didOne,didTwo,apolloClient,emitter)) {
                    ArrayList<ArrayList<String>> foundDids = new ArrayList<>(getListOfDids(coreElementId, didOne, didTwo, size, offset,requireDidTwoNull, apolloClient, emitter));
                    // null is new value to be added after move - can be modified
                    moveDidOneToDidTwo(foundDids,"null",apolloClient,size,offset,emitter);
                }
            break;
            case 5: // vypsat presunuti hodnot did1 do did2 bez provedeni
                if(verifyDids(didOne,didTwo,apolloClient,emitter)) {
                    ArrayList<ArrayList<String>> foundDids = new ArrayList<>(getListOfDids(coreElementId, didOne, didTwo, size, offset,requireDidTwoNull, apolloClient, emitter));
                    printFoundWithoutAction(foundDids,"null",apolloClient,emitter);
                }
            break;
        }

    }


    public static void loadAvailableCoreElements(String url, SseEmitter emitter) throws IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        try{
            CompletableFuture<GetAvailableCoreElementsQuery.Data> ref = QueryHandler.execute(new GetAvailableCoreElementsQuery(), apolloClient);
            ArrayList<String> foundIds = new ArrayList<>();
            for(int i=0;i<ref.get().coreElements.size();i++){
                foundIds.add(ref.get().coreElements.get(i).id);
            }
            if(foundIds.size()>=1) {
                emitter.send(SseEmitter.event().data(createEventData(foundIds, "fill")));
            }else{
                emitter.send(SseEmitter.event().data(createEventData("", "fill")));
            }

        }catch (Exception e){
            createException(e, "loadAvailableCoreElements", "Didswitcher - loadAvailableCoreElements function exception - GetAvailableCoreElementsQuery failed, exception line: ", module, GetAvailableCoreElementsQuery.QUERY_DOCUMENT, emitter);
        }

    }
}

