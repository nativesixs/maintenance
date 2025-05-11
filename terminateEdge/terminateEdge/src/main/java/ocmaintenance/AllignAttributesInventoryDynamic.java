package ocmaintenance;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.Input;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ocmaintenance.controllers.PagesController;
import ocmaintenance.controllers.SharedMethods;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ocmaintenance.AllignAttributesDynamic.createZonedTimestamp;
import static ocmaintenance.controllers.SharedMethods.createEventData;

public class AllignAttributesInventoryDynamic {
    private static final String module = "AllignAttributesInventory";
    @Deprecated
    public static ArrayList<ArrayList<String>> getMissingAttributes(ArrayList<String> ckod, CompletableFuture<GetAllAttributesQuery.Data> allAttributes) throws ExecutionException, InterruptedException {
        ArrayList<ArrayList<String>> missing = new ArrayList<>();
        for(int j=0;j<ckod.size();j++) {
            ArrayList<String> attributes = new ArrayList<>();
            ArrayList<String> inner = new ArrayList<>();
            inner.add(ckod.get(j));
            for (int i = 0; i < allAttributes.get().devices.get(j).attributes.size(); i++) {
                attributes.add(allAttributes.get().devices.get(j).attributes.get(i).did.id);
            }
            for (int i = 0; i < AllignAttributesDynamic.potentialAttributes.size(); i++) {
                if (!attributes.contains(AllignAttributesDynamic.potentialAttributes.get(i))) {
                    inner.add(AllignAttributesDynamic.potentialAttributes.get(i));
                }
            }
            missing.add(inner);
        }

        return missing;
    }
//    @Deprecated
//    private static void compareDios(ApolloClient apolloClient, CompletableFuture<GetAllAttributesQuery.Data> allAttributes, String attributesType, String timestampTo, String timestampFrom,ArrayList<String> devicekinds, String inventory ,boolean update,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
//        ArrayList<String> dioatributy = new ArrayList<>();
//        ArrayList<ArrayList<String>> lstSnmp = new ArrayList<>();
//        for(int i=0;i<devicekinds.size();i++){
//            List<String> kind = new ArrayList<>(Collections.singleton(devicekinds.get(i)));
//            try {
//                CompletableFuture<DiosSetNewestDataGlobalQuery.Data> diosset = QueryHandler.execute(new DiosSetNewestDataGlobalQuery(kind, timestampTo, timestampFrom), apolloClient);
//                //todo comment before flight
//                while (diosset.isDone() == false) {
//                    Thread.sleep(100);
//                }
//
//                for (int j = 0; j < diosset.get().diosSet.oldvalues.items.size(); j++) {
//                    dioatributy.add(diosset.get().diosSet.oldvalues.items.get(j).id.value);
//                }
//                for (int j = 0; j < diosset.get().diosSet.groupByDid.size(); j++) {
//                    ArrayList<String> all = new ArrayList<>();
//                    all.add(diosset.get().diosSet.groupByDid.get(j).set.newvalues.get(0).object.id.value);
//                    all.add(diosset.get().diosSet.groupByDid.get(j).set.newvalues.get(0).did.id);
//                    all.add(diosset.get().diosSet.groupByDid.get(j).set.newvalues.get(0).value.asText);
//                    lstSnmp.add(all);
//                }
//            }catch (Exception e){
//                emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//                emitter.send(SseEmitter.event().data(createEventData("AllignAttributesInventoryScale - compareDios function exception - diossetnewestdataglobal query failed")));
//                SharedMethods.exceptionInServerLog(e,module,"compareDios", DiosSetNewestDataGlobalQuery.QUERY_DOCUMENT);
//            }
//        }
//
//
//        for(int i=0;i<allAttributes.get().devices.size();i++){
//            ArrayList<String> uptodate = new ArrayList<>();
//            ArrayList<String> outdated = new ArrayList<>();
//            ArrayList<String> errors = new ArrayList<>();
//            emitter.send(SseEmitter.event().data(createEventData("Nalezené atributy pro: "+allAttributes.get().devices.get(i).id.value)));
//            for (int j=0;j<allAttributes.get().devices.get(i).attributes.size();j++){
//
//                String snmpdid="";
//                if (AllignAttributesDynamic.pkgFsInventoryValueDids.contains(allAttributes.get().devices.get(i).attributes.get(j).did.id)) {
//                    snmpdid = AllignAttributesDynamic.pkgFsSnmpDids.get(AllignAttributesDynamic.pkgFsInventoryValueDids.indexOf(allAttributes.get().devices.get(i).attributes.get(j).did.id));
//                }else{
//                    snmpdid=allAttributes.get().devices.get(i).attributes.get(j).did.id;
//                }
//
//                for(int k=0;k<lstSnmp.size();k++){
//                    //porovnavaci logika
//                    if(lstSnmp.get(k).get(0).equals(allAttributes.get().devices.get(i).id.value)){ //ckod
//                        String valueSnmp = "";
//                        String valueInv = "";
//                        boolean didNotFound = false;
//                        //figuring if is .pkg or not
//
//                        if(AllignAttributesDynamic.pkgFsInventoryValueDids.contains(allAttributes.get().devices.get(i).attributes.get(j).did.id) && lstSnmp.get(k).get(1).equals(AllignAttributesDynamic.pkgFsSnmpDids.get(AllignAttributesDynamic.pkgFsInventoryValueDids.indexOf(allAttributes.get().devices.get(i).attributes.get(j).did.id))) && lstSnmp.get(k).get(2)!=null){
//                            String newdid;
//                            newdid = AllignAttributesDynamic.pkgFsInventoryValueDids.get(AllignAttributesDynamic.pkgFsSnmpDids.indexOf(snmpdid));
//                            Input<String> file = new Input<>(lstSnmp.get(k).get(2), true);
//                            try {
//                                CompletableFuture<GetFilestorageFileMetadataQuery.Data> filestorage = QueryHandler.execute(new GetFilestorageFileMetadataQuery(file), apolloClient);
//                                Object didv = filestorage.get().filesSet.items.get(0).metadata.toString();
//                                try {
//                                    JsonObject jsonMeta = JsonParser.parseString((String) didv).getAsJsonObject();
//                                    JsonObject jsonVersion = JsonParser.parseString(String.valueOf(jsonMeta.get("metadata"))).getAsJsonObject();
//                                    String version = String.valueOf(jsonVersion.get("version"));
//                                    if (version.equals("null")) {
//                                        //nenalezena verze souboru v metadatech
//                                        emitter.send(SseEmitter.event().data(createEventData("V metadatech souboru: "+lstSnmp.get(k).get(2)+" nebyla nalezena verze")));
//                                    } else {
//                                        //verze nalezena - do smth
//                                        valueSnmp = version;
//                                        //get verzi na inv z prislusneho didu
//                                        int index = allAttributes.get().devices.get(i).attributes.indexOf(newdid);
//                                        valueInv=allAttributes.get().devices.get(i).attributes.get(index).normalizedValue.toString();
//                                    }
//                                } catch (java.lang.IllegalStateException e) {
//                                    errors.add("Metadata pro: " + lstSnmp.get(k).get(2) + " se nepodařilo přečíst");
//                                }
//                            } catch (java.lang.IndexOutOfBoundsException e) {
//                                // .pkg nema metadata
//                                errors.add("pkg: " + lstSnmp.get(k).get(2) + " nenalezeno ve filestorage");
//                            }
//
//                        } else if (lstSnmp.get(k).get(1).equals(allAttributes.get().devices.get(i).attributes.get(j).did.id)) { //did
//                            valueSnmp = lstSnmp.get(k).get(2);
//                            if (valueSnmp == null) {
//                                valueSnmp = "null";
//                            }
//                            valueInv = allAttributes.get().devices.get(i).attributes.get(j).normalizedValue.toString();
//                        }
//
//                        if(!valueSnmp.equals(valueInv)){ //value
//                            //new and old are NOT equal
//                            if(update){
////                                String updateQ="UPDATE_INSTANCE:\n{\"id\":\"" + allAttributes.get().devices.get(i).internalId + "\",\"" +allAttributes.get().devices.get(i).attributes.get(j).did.id+ "\":\"" +lstSnmp.get(k).get(2)+ "\"}";
//                                SharedMethods.updateInstance(allAttributes.get().devices.get(i).internalId,allAttributes.get().devices.get(i).attributes.get(j).did.id,lstSnmp.get(k).get(2),inventory,emitter);
////                                addAttributes(updateQ, inventory, apolloClient, emitter);
//
//                            }
//                            outdated.add(lstSnmp.get(k).get(1));
//                            outdated.add("Nová hodnota: " + lstSnmp.get(k).get(2));
//                            outdated.add("Aktuální hodnota: " + allAttributes.get().devices.get(i).attributes.get(j).normalizedValue.toString());
//                        }else if(!didNotFound && valueSnmp.equals(valueInv) && lstSnmp.get(k).get(1).equals(allAttributes.get().devices.get(i).attributes.get(j).did.id)){
//                            //new and old are equal
//                            uptodate.add(lstSnmp.get(k).get(1));
//                            uptodate.add(lstSnmp.get(k).get(2));
//                        }
//                    }else{
//                        // ckod not found
//                    }
//                }
//                emitter.send(SseEmitter.event().data(createEventData(allAttributes.get().devices.get(i).attributes.get(j).did.id)));
//                emitter.send(SseEmitter.event().data(createEventData("Hodnota: "+allAttributes.get().devices.get(i).attributes.get(j).normalizedValue.toString())));
//            }
//
//            if(uptodate.size()>=1 || outdated.size()>=1 || errors.size()>=1) {
//                emitter.send(SseEmitter.event().data(createEventData("Naposledy aktualizované atributy v SNMP: ")));
//                for (int j = 0; j < uptodate.size(); j += 2) {
//                    emitter.send(SseEmitter.event().data(createEventData(uptodate.get(j)+"Hodnota: "+uptodate.get(j + 1))));
//                }
//                emitter.send(SseEmitter.event().data(createEventData("Neaktuální atributy: ")));
//                for(int j=0;j<outdated.size();j++){
//                    emitter.send(SseEmitter.event().data(createEventData(outdated.get(j))));
//                }
//            }
//            emitter.send(SseEmitter.event().data(createEventData("Errors: ")));
//            for (int j = 0; j < errors.size(); j += 3) {
//                emitter.send(SseEmitter.event().data(createEventData(errors.get(j))));
//            }
//
//        }
//        dioatributy.clear();
//
//
//    }
//    @Deprecated
//    public static void getAttributesValues(ApolloClient apolloClient, CompletableFuture<GetAllAttributesQuery.Data> allAttributes, String attributesType,ArrayList<String> devicekinds, String inventory, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
//        String timestampTo = String.valueOf(ZonedDateTime.now());
//        String timestampFrom= createZonedTimestamp(LocalDate.now().minusDays(fromTimestampSubtractDays));
//        compareDios(apolloClient,allAttributes,attributesType,timestampTo,timestampFrom,devicekinds,inventory,false,emitter);
//    }

    public static void getAttributesValues(String attributesType, int size, int offset,String inventory, ApolloClient apolloClient, SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
//        updateFromSNMP(attributesType,size,offset,false,true,true,inventory,apolloClient,emitter);
        updateFromSNMP2(attributesType,size,offset,false,true,true,false,inventory,apolloClient,emitter);
    }


    public static void getOnlyMissingAttributes(String attributesType, int size, int offset, String inventory, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        ArrayList<ArrayList<String>> missing = getMissingAttributes(attributesType,size,offset,inventory,apolloClient,emitter);
        emitter.send(SseEmitter.event().data(createEventData("Chybějící atributy: \n\n")));
        for(int i=0;i<missing.size();i++){
            if(missing.get(i).size()>=3) {
                for (int j = 2; j < missing.get(i).size(); j++) {
                    try {
                        emitter.send(SseEmitter.event().data(createEventData("id: " + missing.get(i).get(1))));
                        emitter.send(SseEmitter.event().data(createEventData("did: " + missing.get(i).get(j))));
                    } catch (java.lang.IndexOutOfBoundsException e) {
                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty missing se nepodařilo přečíst", "error")));
                    }
                }
            }
        }
//        updateFromSNMP(attributesType,size,offset,false,false,false,true,inventory,apolloClient,emitter);
    }
    public static void getOnlyOutdatedAttributes(String attributesType, int size, int offset, String inventory, ApolloClient apolloClient, SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
        updateFromSNMP2(attributesType,size,offset,false,true,false,false,inventory,apolloClient,emitter);
    }
//    @Deprecated
//    public static void getOnlyMissingAttributes(CompletableFuture<GetAllAttributesQuery.Data> allAttributes, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
//        ArrayList<ArrayList<String>> missing = new ArrayList<>();
//        ArrayList<String> ckod = new ArrayList<>();
//        for(int i=0;i<allAttributes.get().devices.size();i++){
//            ckod.add(allAttributes.get().devices.get(i).id.value);
//        }
//        missing=getMissingAttributes(ckod,allAttributes);
//        for(int j=0;j<ckod.size();j++) {
//            emitter.send(SseEmitter.event().data(createEventData("Chybějící atributy pro: "+ckod.get(j))));
//            for (int i = 1; i < missing.get(j).size(); i++) {
//                emitter.send(SseEmitter.event().data(createEventData(missing.get(j).get(i))));
//            }
//        }
//    }
//    @Deprecated
//    public static void getOnlyAttributes(CompletableFuture<GetAllAttributesQuery.Data> allAttributes, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
//        for(int j=0;j<allAttributes.get().devices.size();j++) {
//            emitter.send(SseEmitter.event().data(createEventData("Nalezené atributy pro: "+allAttributes.get().devices.get(j).id.value)));
//            for (int i = 0; i < allAttributes.get().devices.get(j).attributes.size(); i++) {
//                emitter.send(SseEmitter.event().data(createEventData(allAttributes.get().devices.get(j).attributes.get(i).did.id)));
//            }
//        }
//    }

//    @Deprecated
//    public static String addAttributes(String update,ArrayList<String> deviceToAdd, ArrayList<String> attributesToAdd, String inventory, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
//        //todo lepsi overovaci logika nez rep == 200 => projde skoro vzdycky
////        Response rep= Request.Post(inventory).bodyString(update, ContentType.DEFAULT_TEXT).execute();
////        String status = rep.returnContent().asString();
//
////        String lines[] = status.split("\\r?\\n");
////        emitter.send("Odeslano: \n");
////        for(String line:lines){
////            emitter.send(line);
////        }
//
////        if(status.contains("Error:")){
////            emitter.send(SseEmitter.event().data(createEventData("Odeslání nok")));
////        }else {
////            emitter.send(SseEmitter.event().data(createEventData("Odeslání ok")));
////        }
//        Response res= Request.Post(inventory).bodyString(update, ContentType.DEFAULT_TEXT).execute();
//        String status = res.returnContent().asString();
//        String lines[] = status.split("\\r?\\n");
//        emitter.send(SseEmitter.event().data(createEventData("Odeslano: ")));
//        for(String line:lines){
//            emitter.send(line);
//        }
//        if(status.contains("Error:")){
//            emitter.send(SseEmitter.event().data(createEventData("Error: Atributy: se nepodařilo přidat/aktualizovat")));
//            return "nok";
//        }else{
//            emitter.send(SseEmitter.event().data(createEventData("Atributy pro zařízení: ")));
//            for (int i=0;i<deviceToAdd.size();i++){
//                emitter.send(SseEmitter.event().data(createEventData(deviceToAdd.get(i))));
//            }
//            emitter.send(SseEmitter.event().data(createEventData("byly přidány")));
//        }
//        return "ok";
//
//    }

//    @Deprecated
//    public static void updateFromDiosSetData(ApolloClient apolloClient, CompletableFuture<GetAllAttributesQuery.Data> allAttributes,ArrayList<String> devicekinds, String inventory, String attributesType, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
//        String timestampTo = String.valueOf(ZonedDateTime.now());
//        String timestampFrom= createZonedTimestamp(LocalDate.now().minusDays(fromTimestampSubtractDays));
//        compareDios(apolloClient,allAttributes,attributesType,timestampTo,timestampFrom,devicekinds,inventory,true,emitter);
//    }
    private static ArrayList<ArrayList<String>> pkgList = new ArrayList<>();
    private static ArrayList<ArrayList<String>> oldValuesList = new ArrayList<>();
    private static ArrayList<String> oldIds = new ArrayList<>();
    private static ArrayList<ArrayList<String>> oldValuesListFS = new ArrayList<>();
    private static ArrayList<String> oldIdsFS = new ArrayList<>();

    private static ArrayList<String> getDeviceCkods(int size, int offset, int p, Input<List<String>> type, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        ArrayList<String> ckods = new ArrayList<>();
        try{
            CompletableFuture<GetCkodsOfTypeQuery.Data> getckods = QueryHandler.execute(new GetCkodsOfTypeQuery(type, size, offset*p), apolloClient);
            for(int i=0;i<getckods.get().instancesSet.items.size();i++){
                ckods.add(getckods.get().instancesSet.items.get(i).id.value);
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - getdeviceckods function exception - getckodsoftype query failed")));
            SharedMethods.exceptionInServerLog(e, module, "getdeviceckods", GetCkodsOfTypeQuery.QUERY_DOCUMENT);
        }
        return ckods;
    }
    public static void updateFromSNMP2(String attributesType, int size, int offset, boolean update, boolean outputUnchanged, boolean outputUpToDate,boolean sendUpdate, String inventory, ApolloClient apolloClient, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        String timestampTo = String.valueOf(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")));
        String timestampFrom = createZonedTimestamp(LocalDate.now().minusDays(AllignAttributesDynamic.fromTimestampSubtractDays));
        Input<List<String>> type = new Input<List<String>>(Collections.singletonList(attributesType), true);
        CompletableFuture<GetCkodsOfTypeQuery.Data> getckod = QueryHandler.execute(new GetCkodsOfTypeQuery(type, 1, 0), apolloClient);
        int count = getckod.get().instancesSet.count;
            int limit = count / offset;
            if (count % offset != 0) {
                limit = limit + 1;
            }
            int offset2=offset;
            for (int p = 0; p < limit; p++) {
                String updateInstance = "";
                String updateInstanceFS = "";
                if (p == limit - 1) {
                    size = count - ((limit - 1) * size);
                }
                offset = 0;
                ArrayList<String> ckods = getDeviceCkods(size, offset2, p, type, apolloClient, emitter);
                try {
                    CompletableFuture<DiosSetUpdatesQuery.Data> diosupdate = QueryHandler.execute(new DiosSetUpdatesQuery(timestampTo, timestampFrom, ckods, AllignAttributesDynamic.potentialAttributes, size, offset * p), apolloClient);
                    getOldValues(ckods, size, offset, p, apolloClient, emitter);
                    ArrayList<String> updated = new ArrayList<>();
                    ArrayList<String> upToDate = new ArrayList<>();
                    for (int i = 0; i < diosupdate.get().instancesSet.items.size(); i++) {
                        if (diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.size() >= 1) { // filter out empty arrays
                            for (int j = 0; j < diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.size(); j++) {
//                            if(oldIds.contains(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object.internalId)){ //old intid = new intid
                                if (oldIds.contains(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object is null in updatefromSNMP - newvalues compare to allvalues old").internalId)) { //old intid = new intid
//                                int indexOfNewInOld = oldIds.indexOf(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object.internalId);
                                    int indexOfNewInOld = oldIds.indexOf(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object is null in updatefromSNMP - newvalues compare to allvalues old").internalId);
                                    if (oldValuesList.get(indexOfNewInOld).contains(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id)) {
                                        int indexOfNewDidInOld = oldValuesList.get(indexOfNewInOld).indexOf(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id);
//                                    if(!oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld+1).equals(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText)){
                                        if (!oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld + 1).equals(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value is null in updatefromSNMP - newvalues compare to allvalues old").asText)) {
                                            if (update) {
                                                //send to fix
//                                            String internalId = diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object.internalId;
                                                String internalId = Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object is null in updatefromSNMP - newvalues compare to allvalues old").internalId;
                                                String did = diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id;
                                                String didValue = diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText;
                                                updateInstance = updateInstance + "UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + did + "\":\"" + didValue + "\"}" + "\n";
                                            }
//                                        System.out.println("rozdilne:");
//                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld));
//                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld+1));
//                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id);
//                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText);
                                            try {
//                                            updated.add(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id.value); //id
                                                updated.add(Objects.requireNonNull(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object is null in updatefromSNMP - updated.add").id, "Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id is null in updatefromSNMP - updated.add").value); //id
                                                updated.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld)); //did
                                                updated.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld + 1)); //oldvalue
//                                            updated.add(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText); //newvalue
                                                updated.add(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value is null in updated.add").asText); //newvalue
                                            } catch (Exception e) {
                                                emitter.send("Atributy do listu neaktualnich hodnot SNMP se nepovedlo pridat");
                                            }
                                        } else { // hodnoty jsou stejne
//                                        System.out.println("stejne:");
//                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld));
//                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld+1));
//                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id);
//                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText);
                                            try {
//                                            upToDate.add(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id.value); //id
                                                upToDate.add(Objects.requireNonNull(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object is null in updateFromSNMP uptodate.add").id, "Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id is null in updateFromSNMP uptodate.add").value); //id
                                                upToDate.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld)); //did
                                                upToDate.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld + 1)); //value
                                            } catch (Exception e) {
                                                emitter.send("Atributy do listu aktualnich hodnot SNMP se nepovedlo pridat");
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    updateFromSNMPlog(updated, upToDate, update, outputUpToDate, outputUnchanged, p, limit, emitter);
                    getOldValuesFS(ckods, size, offset, p, apolloClient, emitter);

                    ArrayList<String> updatedFS = new ArrayList<>();
                    ArrayList<String> upToDateFS = new ArrayList<>();
                    for (int i = 0; i < oldValuesListFS.size(); i++) {
                        if (oldValuesListFS.get(i).size() == 3 && AllignAttributesDynamic.pkgFsInventoryValueDids.contains(oldValuesListFS.get(i).get(1))) {
                            int indexpkgFsInv = AllignAttributesDynamic.pkgFsInventoryValueDids.indexOf(oldValuesListFS.get(i).get(1));
                            String snmpdid = AllignAttributesDynamic.pkgFsSnmpDids.get(indexpkgFsInv);
                            String valueSnmp = "";
                            String valueInv = "";
                            for (int j = 0; j < pkgList.size(); j++) {
                                if (pkgList.get(j).get(0).equals(oldValuesListFS.get(i).get(0)) && pkgList.get(j).get(1).equals(snmpdid) && pkgList.get(j).get(2) != null) {
                                    try {
                                        Input<String> file = new Input<>(pkgList.get(j).get(2), true);
                                        CompletableFuture<GetFilestorageFileMetadataQuery.Data> filestorage = QueryHandler.execute(new GetFilestorageFileMetadataQuery(file), apolloClient);
//                                    Object didv = filestorage.get().filesSet.items.get(0).metadata.toString();
                                        Object didv = Objects.requireNonNull(Objects.requireNonNull(filestorage.get().filesSet, "filestorage.get().filesSet is null in updateFromSNMP newvalues compare to oldvalues .pkg").items.get(0).metadata, "Objects.requireNonNull(filestorage.get().filesSet.items.get(0).metadata is null in updateFromSNMP newvalues compare to oldvalues.pkg").toString();
                                        try {
                                            JsonObject jsonMeta = JsonParser.parseString((String) didv).getAsJsonObject();
                                            JsonObject jsonVersion = JsonParser.parseString(String.valueOf(jsonMeta.get("metadata"))).getAsJsonObject();
                                            String version = String.valueOf(jsonVersion.get("version").getAsString());
                                            if (version.equals("null")) {
                                                //nenalezena verze souboru v metadatech
                                                emitter.send(SseEmitter.event().data(createEventData("V metadatech souboru: " + pkgList.get(j).get(2) + " nebyla nalezena verze")));
                                            } else {
                                                //verze nalezena
                                                valueSnmp = version;
                                                valueInv = oldValuesListFS.get(i).get(2);
//                                                if (!valueSnmp.equals(valueInv)) { //new and old are NOT equal
                                                if (!valueSnmp.equals(valueInv) || Integer.parseInt(valueSnmp) != (Integer.parseInt(valueInv))) { //new and old are NOT equal
                                                    if (update) {
                                                        updateInstanceFS = updateInstanceFS + "UPDATE_INSTANCE:\n{\"id\":\"" + oldValuesListFS.get(i).get(0) + "\",\"" + oldValuesListFS.get(i).get(1) + "\":\"" + valueSnmp + "\"}" + "\n";
//                                                    SharedMethods.updateInstance(oldValuesListFS.get(i).get(0),oldValuesListFS.get(i).get(1),valueSnmp,inventory,emitter);
                                                    }
                                                    try {
                                                        updatedFS.add(oldIdsFS.get(i)); //id
                                                        updatedFS.add(oldValuesListFS.get(i).get(1)); //did
                                                        updatedFS.add(valueInv); //oldvalue
                                                        updatedFS.add(valueSnmp); //newvalue
                                                    } catch (Exception e) {
                                                        emitter.send("Atributy do listu neaktualnich hodnot FS se nepovedlo pridat");
                                                    }
                                                } else if (valueSnmp.equals(valueInv)) { //values are same
                                                    try {
                                                        upToDateFS.add(oldIdsFS.get(i)); //id
                                                        upToDateFS.add(oldValuesListFS.get(i).get(1)); //did
                                                        upToDateFS.add(valueInv); //value
                                                    } catch (Exception e) {
                                                        emitter.send("Atributy do listu aktualnich hodnot FS se nepovedlo pridat");
                                                    }
                                                }
                                            }
                                        } catch (java.lang.IllegalStateException e) {
                                            emitter.send(SseEmitter.event().data(createEventData("Metadata pro" + pkgList.get(j).get(2) + " se nepovedlo přečíst", "warning")));
//                                errors.add("Metadata pro: " + pkgList.get(j).get(2) + " se nepodařilo přečíst");
                                        }
                                    } catch (Exception e) {
//                            errors.add("pkg: " + pkgList.get(j).get(2) + " nenalezeno ve filestorage");
                                        emitter.send(SseEmitter.event().data(createEventData("pkg: "+pkgList.get(j).get(2)+" nenalezeno ve filestorage", "warning")));
                                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
                                        emitter.send(SseEmitter.event().data(createEventData("AllignAttributesInventoryScale - updateFromSNMP function exception - getfilestoragemetadata query failed")));
                                        SharedMethods.exceptionInServerLog(e, module, "updateFromSNMP", GetFilestorageFileMetadataQuery.QUERY_DOCUMENT);
                                    }
                                }
                            }
                        } else {
                            // pkgfsinvvaluedids neobsahuji did v poli .get(i)
                        }

                    }
                updateFromFSlog(updatedFS, upToDateFS, update, outputUpToDate, outputUnchanged, p, limit, emitter);

                    if (update) {
                        if (updateInstance.length() >= 2) {
                            emitter.send(SseEmitter.event().data(createEventData("Posílání žádosti o update atributů SNMP pro várku " + (p + 1) + " / " + limit + "\n\n")));
                            if(sendUpdate) {
                                SharedMethods.updateInstanceParsed(updateInstance, inventory, emitter);
                            }else{
                                String lines[] = updateInstance.split("\\r?\\n");
                                for(String line:lines){
                                    emitter.send(SseEmitter.event().data(createEventData(line)));
                                }
                            }
                        }
                        if (updateInstanceFS.length() >= 2) {
                            emitter.send(SseEmitter.event().data(createEventData("Posílání žádosti o update atributů FS pro várku " + (p + 1) + " / " + limit + "\n\n")));
                            if(sendUpdate) {
                                SharedMethods.updateInstanceParsed(updateInstanceFS, inventory, emitter);
                            }else{
                                String lines[] = updateInstanceFS.split("\\r?\\n");
                                for(String line:lines){
                                    emitter.send(SseEmitter.event().data(createEventData(line)));
                                }
                            }
                        }
                }


            }catch (Exception e){
                emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
                emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - updateFromSNMP2 function exception - DiosSetUpdatesQuery query failed")));
                SharedMethods.exceptionInServerLog(e, module, "updateFromSNMP2", DiosSetUpdatesQuery.QUERY_DOCUMENT);
            }


            }

    }
    private static void getOldValuesFS(ArrayList<String> ckods,int size,int offset,int p,ApolloClient apolloClient,SseEmitter emitter) throws IOException {
        //old value .pkg add
        oldValuesListFS.clear();
        oldIdsFS.clear();
        try {
            CompletableFuture<DiosSetUpdateOldValuesQuery.Data> diosupdateOldFS = QueryHandler.execute(new DiosSetUpdateOldValuesQuery(ckods, AllignAttributesDynamic.pkgFsInventoryValueDids, size, offset * p), apolloClient);
//        ArrayList<ArrayList<String>> oldValuesListFS = new ArrayList<>();
//        ArrayList<String> oldIdsFS = new ArrayList<>();
            for (int i = 0; i < diosupdateOldFS.get().instancesSet.items.size(); i++) {
//                oldIdsFS.add(diosupdateOldFS.get().instancesSet.items.get(i).id.value);
                ArrayList<String> oldValuesFS = new ArrayList<>();
                String oldIdsTemp = Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).id).value;
                oldValuesFS.add(Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).internalId,"oldValuesFS null"));
                for (int j = 0; j < diosupdateOldFS.get().instancesSet.items.get(i).attributes.size(); j++) {
                    if(AllignAttributesDynamic.pkgFsInventoryValueDids.contains(diosupdateOldFS.get().instancesSet.items.get(i).attributes.get(j).did.id)) {
                        oldValuesFS.add(Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).attributes.get(j).did.id, "oldValuesFS.add"));
                        oldValuesFS.add(Objects.requireNonNull(Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).attributes, "diosupdateOldFS.get().instancesSet.items.get(i).attributes is null in getOldValuesFS").get(j).normalizedValue, "Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).attributes is null in getoldvaluesFS").toString());
                    }
                }
                if(oldValuesFS.size()>=2) {
                    oldValuesListFS.add(oldValuesFS);
                    oldIdsFS.add(oldIdsTemp);
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - getoldvaluesFS function exception - diossetupdateoldvalues query failed")));
            SharedMethods.exceptionInServerLog(e, module, "getoldvaluesFS", DiosSetUpdateOldValuesQuery.QUERY_DOCUMENT);
        }
    }

    private static void getOldValues(ArrayList<String> ckods, int size, int offset, int p, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<DiosSetUpdateOldValuesQuery.Data> diosupdateOld = QueryHandler.execute(new DiosSetUpdateOldValuesQuery(ckods, AllignAttributesDynamic.potentialAttributes, size, offset * p), apolloClient);
//            pkgList.clear();
            oldValuesList.clear();
            oldIds.clear();
            for (int i = 0; i < diosupdateOld.get().instancesSet.items.size(); i++) {
//                ArrayList<String> pkgListTemp = new ArrayList<>();
                ArrayList<String> oldValues = new ArrayList<>();
//                pkgListTemp.add(Objects.requireNonNull(Objects.requireNonNull(diosupdateOld.get().instancesSet,"1. pkgListTemp is null").items.get(i).internalId,"2. pkgListTemp is null"));
                String oldIdsTemp = Objects.requireNonNull(diosupdateOld.get().instancesSet.items.get(i).internalId);
//                oldIds.add(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(diosupdateOld.get().instancesSet, "1. oldIds is null").items, "2. oldIds is null").get(i).internalId,"3. oldIds is null"));
                for (int j = 0; j < diosupdateOld.get().instancesSet.items.get(i).attributes.size(); j++) {
                    if(AllignAttributesDynamic.potentialAttributes.contains(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).did.id)) {
                        oldValues.add(Objects.requireNonNull(Objects.requireNonNull(diosupdateOld.get().instancesSet, "oldValues 1 is null").items, "2. oldValues is null").get(i).attributes.get(j).did.id);
                        oldValues.add(Objects.requireNonNull(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue, "diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue is null in getOldValues").toString());
                    }
//                    if(AllignAttributesDynamic.pkgFsSnmpDids.contains(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).did.id)) {
//                        pkgListTemp.add(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).did.id);
//                        pkgListTemp.add(Objects.requireNonNull(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue, "diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue is null in getOldValues").toString());
//                    }
                }
                if(oldValues.size()>=2) {
                    oldValuesList.add(oldValues);
                    oldIds.add(oldIdsTemp);
                }
//                if (pkgListTemp.size()>=2) {
//                    pkgList.add(pkgListTemp); //[ [internalId,did,value],[...] ]
//                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - getoldvalues function exception - diossetupdateoldvalues query failed")));
            SharedMethods.exceptionInServerLog(e, module, "getoldvalues", DiosSetUpdateOldValuesQuery.QUERY_DOCUMENT);
        }
        try {
            CompletableFuture<DiosSetUpdateOldValuesQuery.Data> diosupdateOld = QueryHandler.execute(new DiosSetUpdateOldValuesQuery(ckods, AllignAttributesDynamic.pkgFsSnmpDids, size, offset * p), apolloClient);
            pkgList.clear();
            for (int i = 0; i < diosupdateOld.get().instancesSet.items.size(); i++) {
                ArrayList<String> pkgListTemp = new ArrayList<>();
                pkgListTemp.add(Objects.requireNonNull(Objects.requireNonNull(diosupdateOld.get().instancesSet,"1. pkgListTemp is null").items.get(i).internalId,"2. pkgListTemp is null"));
                for (int j = 0; j < diosupdateOld.get().instancesSet.items.get(i).attributes.size(); j++) {
                    if(AllignAttributesDynamic.pkgFsSnmpDids.contains(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).did.id)) {
                        pkgListTemp.add(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).did.id);
                        pkgListTemp.add(Objects.requireNonNull(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue, "diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue is null in getOldValues").toString());
                    }
                }
                if (pkgListTemp.size()>=2) {
                    pkgList.add(pkgListTemp); //[ [internalId,did,value],[...] ]
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - getoldvalues function exception - diossetupdateoldvalues .pkg query failed")));
            SharedMethods.exceptionInServerLog(e, module, "getoldvalues .pkg", DiosSetUpdateOldValuesQuery.QUERY_DOCUMENT);
        }
    }

        private static void updateFromSNMPlog(ArrayList<String> updated,ArrayList<String> upToDate,boolean update, boolean outputUpToDate, boolean outputUnchanged,int p, int limit,SseEmitter emitter) throws IOException {
        // log SNMP
        try {
            if (updated.size() >= 1 && outputUnchanged) {
                emitter.send(SseEmitter.event().data(createEventData("Neaktuální atributy SNMP pro várku " + (p + 1) + " / " + limit + "\n\n")));
                for (int i = 0; i < updated.size(); i += 4) {
                    try {
                        emitter.send(SseEmitter.event().data(createEventData("id: " + updated.get(i))));
                        emitter.send(SseEmitter.event().data(createEventData("did: " + updated.get(i + 1))));
                        emitter.send(SseEmitter.event().data(createEventData("Neaktuální hodnota v inventory: " + updated.get(i + 2))));
                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota ze SNMP: " + updated.get(i + 3) + "\n\n")));
                    } catch (IndexOutOfBoundsException | IOException e) {
                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty SNMP se nepodařilo přečíst", "error")));
                    }
                }
            }
            if (upToDate.size() >= 1 && !update && outputUpToDate) {
                emitter.send(SseEmitter.event().data(createEventData("Aktuální atributy SNMP pro várku " + (p + 1) + " / " + limit + "\n\n")));
                for (int i = 0; i < upToDate.size(); i += 3) {
                    try {
                        emitter.send(SseEmitter.event().data(createEventData("id: " + upToDate.get(i))));
                        emitter.send(SseEmitter.event().data(createEventData("did: " + upToDate.get(i + 1))));
                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota na inventory a SNMP: " + upToDate.get(i + 2) + "\n\n")));
                    } catch (IndexOutOfBoundsException | IOException e) {
                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty SNMP se nepodařilo přečíst", "error")));
                    }
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - updateFromSNMPlog function exception")));
            SharedMethods.exceptionInServerLog(e, module, "updateFromSNMPlog");
        }
    }
    private static void updateFromFSlog(ArrayList<String> updatedFS,ArrayList<String> upToDateFS,boolean update, boolean outputUpToDate, boolean outputUnchanged,int p,int limit,SseEmitter emitter) throws IOException {
        //log FS
        try {
            if (updatedFS.size() >= 1 && outputUnchanged) {
                emitter.send(SseEmitter.event().data(createEventData("Neaktuální atributy FS pro várku " + (p + 1) + " / " + limit + "\n\n")));
                for (int i = 0; i < updatedFS.size(); i += 4) {
                    try {
                        emitter.send(SseEmitter.event().data(createEventData("id: " + updatedFS.get(i))));
                        emitter.send(SseEmitter.event().data(createEventData("did: " + updatedFS.get(i + 1))));
                        emitter.send(SseEmitter.event().data(createEventData("Neaktuální hodnota na inventory: " + updatedFS.get(i + 2))));
                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota ve FS: " + updatedFS.get(i + 3) + "\n\n")));
                    } catch (IndexOutOfBoundsException | IOException e) {
                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty FS se nepodařilo přečíst", "error")));
                    }
                }
            }
            if (upToDateFS.size() >= 1 && !update && outputUpToDate) {
                emitter.send(SseEmitter.event().data(createEventData("Aktuální atributy FS pro várku " + (p + 1) + " / " + limit + "\n\n")));
                for (int i = 0; i < upToDateFS.size(); i += 3) {
                    try {
                        emitter.send(SseEmitter.event().data(createEventData("id: " + upToDateFS.get(i))));
                        emitter.send(SseEmitter.event().data(createEventData("did: " + upToDateFS.get(i + 1))));
                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota ve FS a inventory: " + upToDateFS.get(i + 2) + "\n\n")));
                    } catch (IndexOutOfBoundsException e) {
                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty FS se nepodařilo přečíst", "error")));
                    }
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - updateFromFSlog function exception")));
            SharedMethods.exceptionInServerLog(e, module, "updateFromFSlog");
        }
    }



//    public static void getOldValues(Input<List<String>> type,int size,int offset,int p,ApolloClient apolloClient,SseEmitter emitter) throws IOException {
//        try {
//            System.out.println(size);
//            System.out.println(offset*p);
//            CompletableFuture<DiosSetUpdateOldValuesQuery.Data> diosupdateOld = QueryHandler.execute(new DiosSetUpdateOldValuesQuery(type, AllignAttributesDynamic.potentialAttributes, size, offset * p), apolloClient);
//            //add for .pkg update needs
////            ArrayList<ArrayList<String>> pkgList = new ArrayList<>();
//            //OLD VALUES add
////            ArrayList<ArrayList<String>> oldValuesList = new ArrayList<>();
////            ArrayList<String> oldIds = new ArrayList<>();
//            pkgList.clear();
//            oldValuesList.clear();
//            oldIds.clear();
//
//            for (int i = 0; i < diosupdateOld.get().instancesSet.items.size(); i++) {
//                ArrayList<String> pkgListTemp = new ArrayList<>();
//                ArrayList<String> oldValues = new ArrayList<>();
//                pkgListTemp.add(Objects.requireNonNull(Objects.requireNonNull(diosupdateOld.get().instancesSet,"1. pkgListTemp is null").items.get(i).internalId,"2. pkgListTemp is null"));
//                oldIds.add(Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(diosupdateOld.get().instancesSet, "1. oldIds is null").items, "2. oldIds is null").get(i).internalId,"3. oldIds is null"));
//                for (int j = 0; j < diosupdateOld.get().instancesSet.items.get(i).attributes.size(); j++) {
//                    oldValues.add(Objects.requireNonNull(Objects.requireNonNull(diosupdateOld.get().instancesSet,"oldValues 1 is null").items,"2. oldValues is null").get(i).attributes.get(j).did.id);
////                    oldValues.add(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue.toString());
//                    oldValues.add(Objects.requireNonNull(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue,"diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue is null in getOldValues").toString());
//                    pkgListTemp.add(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).did.id);
////                    pkgListTemp.add(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue.toString());
//                    pkgListTemp.add(Objects.requireNonNull(diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue,"diosupdateOld.get().instancesSet.items.get(i).attributes.get(j).normalizedValue is null in getOldValues").toString());
//                }
//                oldValuesList.add(oldValues);
//                if (AllignAttributesDynamic.pkgFsSnmpDids.contains(pkgListTemp.get(1))) {
//                    pkgList.add(pkgListTemp);
//                }
//            }
//        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - getoldvalues function exception - diossetupdateoldvalues query failed")));
//            SharedMethods.exceptionInServerLog(e, module, "getoldvalues", DiosSetUpdateOldValuesQuery.QUERY_DOCUMENT);
//        }
//
//    }
//
//    private static ArrayList<ArrayList<String>> oldValuesListFS = new ArrayList<>();
//    private static ArrayList<String> oldIdsFS = new ArrayList<>();
//    private static void getOldValuesFS(Input<List<String>> type,int size,int offset,int p,ApolloClient apolloClient,SseEmitter emitter) throws IOException {
//        //old value .pkg add
//        oldValuesListFS.clear();
//        oldIdsFS.clear();
//        try {
//            CompletableFuture<DiosSetUpdateOldValuesQuery.Data> diosupdateOldFS = QueryHandler.execute(new DiosSetUpdateOldValuesQuery(type, AllignAttributesDynamic.pkgFsInventoryValueDids, size, offset * p), apolloClient);
////        ArrayList<ArrayList<String>> oldValuesListFS = new ArrayList<>();
////        ArrayList<String> oldIdsFS = new ArrayList<>();
//            for (int i = 0; i < diosupdateOldFS.get().instancesSet.items.size(); i++) {
//                oldIdsFS.add(Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).id,"diosupdateOldFS.get().instancesSet.items.get(i).id is null in GetOldValuesFS").value);
////                oldIdsFS.add(diosupdateOldFS.get().instancesSet.items.get(i).id.value);
//                ArrayList<String> oldValuesFS = new ArrayList<>();
//                oldValuesFS.add(Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).internalId,"oldValuesFS null"));
//                for (int j = 0; j < diosupdateOldFS.get().instancesSet.items.get(i).attributes.size(); j++) {
//                    oldValuesFS.add(Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).attributes.get(j).did.id,"oldValuesFS.add"));
////                    oldValuesFS.add(diosupdateOldFS.get().instancesSet.items.get(i).attributes.get(j).normalizedValue.toString());
//                    oldValuesFS.add(Objects.requireNonNull(Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).attributes,"diosupdateOldFS.get().instancesSet.items.get(i).attributes is null in getOldValuesFS").get(j).normalizedValue,"Objects.requireNonNull(diosupdateOldFS.get().instancesSet.items.get(i).attributes is null in getoldvaluesFS").toString());
//                }
//                oldValuesListFS.add(oldValuesFS);
//            }
//        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - getoldvaluesFS function exception - diossetupdateoldvalues query failed")));
//            SharedMethods.exceptionInServerLog(e, module, "getoldvaluesFS", DiosSetUpdateOldValuesQuery.QUERY_DOCUMENT);
//        }
//    }
//
//
//    public static void updateFromSNMP(String attributesType,int size,int offset,boolean update,boolean outputUnchanged,boolean outputUpToDate, String inventory, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
//        String timestampTo = String.valueOf(ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")));
//        String timestampFrom = createZonedTimestamp(LocalDate.now().minusDays(AllignAttributesDynamic.fromTimestampSubtractDays));
//        Input<List<String>> type = new Input<List<String>>(Collections.singletonList(attributesType), true);
//
//        try {
////            CompletableFuture<DiosSetUpdateOldValuesQuery.Data> diosupdateOlds = QueryHandler.execute(new DiosSetUpdateOldValuesQuery(type,AllignAttributesDynamic.potentialAttributes,1,0), apolloClient);
//            CompletableFuture<DiosSetUpdatesQuery.Data> diosupdateOlds = QueryHandler.execute(new DiosSetUpdatesQuery(timestampTo, timestampFrom, type, AllignAttributesDynamic.potentialAttributes, 1,0), apolloClient);
//            int count = diosupdateOlds.get().instancesSet.count;
//            int limit = count / offset;
//            if (count % offset != 0) {
//                limit = limit + 1;
//            }
//            for (int p = 0; p < limit; p++) {
//                String updateInstance="";
//                String updateInstanceFS="";
//                if (p == limit - 1) {
//                    size = count - ((limit - 1) * size);
//                }
//                CompletableFuture<DiosSetUpdatesQuery.Data> diosupdate = QueryHandler.execute(new DiosSetUpdatesQuery(timestampTo, timestampFrom, type, AllignAttributesDynamic.potentialAttributes, size, offset * p), apolloClient);
//
//                System.out.println("getOldValues started batch: "+(p+1));
//                getOldValues(type,size,offset,p,apolloClient,emitter);
//                System.out.println("getOldValues done batch: "+(p+1));
//                //new values compare to allvalues old
//                ArrayList<String> updated = new ArrayList<>();
//                ArrayList<String> upToDate = new ArrayList<>();
//                try {
//                    for (int i = 0; i < diosupdate.get().instancesSet.items.size(); i++) {
//                        if (diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.size() >= 1) {
//                            System.out.println("Checking SNMP: " + Objects.requireNonNull(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object checking is null").id, "Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id is null in checking id..").value);
//                            for (int j = 0; j < diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.size(); j++) {
////                            if(oldIds.contains(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object.internalId)){ //old intid = new intid
//                                if (oldIds.contains(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object is null in updatefromSNMP - newvalues compare to allvalues old").internalId)) { //old intid = new intid
////                                int indexOfNewInOld = oldIds.indexOf(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object.internalId);
//                                    int indexOfNewInOld = oldIds.indexOf(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object is null in updatefromSNMP - newvalues compare to allvalues old").internalId);
//                                    if (oldValuesList.get(indexOfNewInOld).contains(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id)) {
//                                        int indexOfNewDidInOld = oldValuesList.get(indexOfNewInOld).indexOf(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id);
////                                    if(!oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld+1).equals(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText)){
//                                        if (!oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld + 1).equals(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value is null in updatefromSNMP - newvalues compare to allvalues old").asText)) {
//                                            if (update) {
//                                                //send to fix
////                                            String internalId = diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object.internalId;
//                                                String internalId = Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).object is null in updatefromSNMP - newvalues compare to allvalues old").internalId;
//                                                String did = diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id;
//                                                String didValue = oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld + 1);
//                                                updateInstance = updateInstance + "UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\"" + did + "\":\"" + didValue + "\"}" + "\n";
//                                            }
////                                        System.out.println("rozdilne:");
////                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld));
////                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld+1));
////                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id);
////                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText);
//                                            try {
////                                            updated.add(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id.value); //id
//                                                updated.add(Objects.requireNonNull(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object is null in updatefromSNMP - updated.add").id, "Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id is null in updatefromSNMP - updated.add").value); //id
//                                                updated.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld)); //did
//                                                updated.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld + 1)); //oldvalue
////                                            updated.add(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText); //newvalue
//                                                updated.add(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value is null in updated.add").asText); //newvalue
//                                            } catch (Exception e) {
//                                                emitter.send("Atributy do listu neaktualnich hodnot SNMP se nepovedlo pridat");
//                                            }
//                                        } else { // hodnoty jsou stejne
////                                        System.out.println("stejne:");
////                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld));
////                                        System.out.println(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld+1));
////                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).did.id);
////                                        System.out.println(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(j).set.newvalues.get(0).value.asText);
//                                            try {
////                                            upToDate.add(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id.value); //id
//                                                upToDate.add(Objects.requireNonNull(Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object, "diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object is null in updateFromSNMP uptodate.add").id, "Objects.requireNonNull(diosupdate.get().instancesSet.items.get(i).diosObject.groupByDid.get(0).set.newvalues.get(0).object.id is null in updateFromSNMP uptodate.add").value); //id
//                                                upToDate.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld)); //did
//                                                upToDate.add(oldValuesList.get(indexOfNewInOld).get(indexOfNewDidInOld + 1)); //value
//                                            } catch (Exception e) {
//                                                emitter.send("Atributy do listu aktualnich hodnot SNMP se nepovedlo pridat");
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }catch (Exception e){
//                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//                    emitter.send(SseEmitter.event().data(createEventData("AllignAttributesInventoryScale - updateFromSNMP function exception -compareSNMP")));
//                    SharedMethods.exceptionInServerLog(e, module, "updateFromSNMP - compareSNMP");
//                }
//                System.out.println("updateFromSNMPlog started batch: "+(p+1));
//                updateFromSNMPlog(updated,upToDate,update,outputUpToDate,outputUnchanged,p,limit,emitter);
//                System.out.println("updateFromSNMPlog done batch: "+(p+1));
//                System.out.println("getOldValuesFS started batch: "+(p+1));
//                getOldValuesFS(type,size,offset,p,apolloClient,emitter);
//                System.out.println("getOldValuesFS done batch: "+(p+1));
//                //new values compare to old values .pkg
//
//                ArrayList<String> updatedFS = new ArrayList<>();
//                ArrayList<String> upToDateFS = new ArrayList<>();
//                try {
//                    for (int i = 0; i < oldValuesListFS.size(); i++) {
//                        if (oldValuesListFS.get(i).size() == 3 && AllignAttributesDynamic.pkgFsInventoryValueDids.contains(oldValuesListFS.get(i).get(1))) {
//                            try {
//                                System.out.println("Checking FS: " + oldIdsFS.get(i));
//                            }catch (Exception e){
//                                emitter.send(SseEmitter.event().data(createEventData("checkingfs log err","error")));
//                            }
//                            int indexpkgFsInv = AllignAttributesDynamic.pkgFsInventoryValueDids.indexOf(oldValuesListFS.get(i).get(1));
//                            String snmpdid = AllignAttributesDynamic.pkgFsSnmpDids.get(indexpkgFsInv);
//                            String valueSnmp = "";
//                            String valueInv = "";
//                            for (int j = 0; j < pkgList.size(); j++) {
//                                if (pkgList.get(j).get(0).equals(oldValuesListFS.get(i).get(0)) && pkgList.get(j).get(1).equals(snmpdid) && pkgList.get(j).get(2) != null) {
//                                    try {
//                                        Input<String> file = new Input<>(pkgList.get(j).get(2), true);
//                                        CompletableFuture<GetFilestorageFileMetadataQuery.Data> filestorage = QueryHandler.execute(new GetFilestorageFileMetadataQuery(file), apolloClient);
////                                    Object didv = filestorage.get().filesSet.items.get(0).metadata.toString();
//                                        Object didv = Objects.requireNonNull(Objects.requireNonNull(filestorage.get().filesSet, "filestorage.get().filesSet is null in updateFromSNMP newvalues compare to oldvalues .pkg").items.get(0).metadata, "Objects.requireNonNull(filestorage.get().filesSet.items.get(0).metadata is null in updateFromSNMP newvalues compare to oldvalues.pkg").toString();
//                                        try {
//                                            JsonObject jsonMeta = JsonParser.parseString((String) didv).getAsJsonObject();
//                                            JsonObject jsonVersion = JsonParser.parseString(String.valueOf(jsonMeta.get("metadata"))).getAsJsonObject();
//                                            String version = String.valueOf(jsonVersion.get("version").getAsString());
//                                            if (version.equals("null")) {
//                                                //nenalezena verze souboru v metadatech
//                                                emitter.send(SseEmitter.event().data(createEventData("V metadatech souboru: " + pkgList.get(j).get(2) + " nebyla nalezena verze")));
//                                            } else {
//                                                //verze nalezena
//                                                valueSnmp = version;
//                                                valueInv = oldValuesListFS.get(i).get(2);
//                                                if (!valueSnmp.equals(valueInv) || Integer.parseInt(valueSnmp) != (Integer.parseInt(valueInv))) { //new and old are NOT equal
//                                                    if (update) {
//                                                        updateInstanceFS = updateInstanceFS + "UPDATE_INSTANCE:\n{\"id\":\"" + oldValuesListFS.get(i).get(0) + "\",\"" + oldValuesListFS.get(i).get(1) + "\":\"" + valueSnmp + "\"}" + "\n";
////                                                    SharedMethods.updateInstance(oldValuesListFS.get(i).get(0),oldValuesListFS.get(i).get(1),valueSnmp,inventory,emitter);
//                                                    }
//                                                    try {
//                                                        updatedFS.add(oldIdsFS.get(i)); //id
//                                                        updatedFS.add(oldValuesListFS.get(i).get(1)); //did
//                                                        updatedFS.add(valueInv); //oldvalue
//                                                        updatedFS.add(valueSnmp); //newvalue
//                                                    } catch (Exception e) {
//                                                        emitter.send("Atributy do listu neaktualnich hodnot FS se nepovedlo pridat");
//                                                    }
//                                                } else if (valueSnmp.equals(valueInv)) { //values are same
//                                                    try {
//                                                        upToDateFS.add(oldIdsFS.get(i)); //id
//                                                        upToDateFS.add(oldValuesListFS.get(i).get(1)); //did
//                                                        upToDateFS.add(valueInv); //value
//                                                    } catch (Exception e) {
//                                                        emitter.send("Atributy do listu aktualnich hodnot FS se nepovedlo pridat");
//                                                    }
//                                                }
//                                            }
//                                        } catch (java.lang.IllegalStateException e) {
//                                            emitter.send(SseEmitter.event().data(createEventData("Metadata pro" + pkgList.get(j).get(2) + " se nepovedlo přečíst", "warning")));
////                                errors.add("Metadata pro: " + pkgList.get(j).get(2) + " se nepodařilo přečíst");
//                                        }
//                                    } catch (Exception e) {
////                            errors.add("pkg: " + pkgList.get(j).get(2) + " nenalezeno ve filestorage");
//                                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//                                        emitter.send(SseEmitter.event().data(createEventData("AllignAttributesInventoryScale - updateFromSNMP function exception - getfilestoragemetadata query failed")));
//                                        SharedMethods.exceptionInServerLog(e, module, "updateFromSNMP", GetFilestorageFileMetadataQuery.QUERY_DOCUMENT);
//                                    }
//                                }
//                            }
//                        } else {
//                            // pkgfsinvvaluedids neobsahuji did v poli .get(i)
//                        }
//                    }
//                }catch (Exception e){
//                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//                    emitter.send(SseEmitter.event().data(createEventData("AllignAttributesInventoryScale - updateFromSNMP function exception -compare FS")));
//                    SharedMethods.exceptionInServerLog(e, module, "updateFromSNMP - compareFS");
//                }
//
//                System.out.println("compareFSvalues done batch: "+(p+1));
//                System.out.println("updateFromFSlog started batch: "+(p+1));
//                updateFromFSlog(updatedFS,upToDateFS,update,outputUpToDate,outputUnchanged,p,limit,emitter);
//                System.out.println("updateFromFSlog done, batch: "+(p+1));
//
//                if(update){
//                    if(updateInstance.length()>=2){
//                        emitter.send(SseEmitter.event().data(createEventData("Posílání žádosti o update atributů SNMP pro várku "+(p+1)+" / "+limit+"\n\n")));
//                        SharedMethods.updateInstanceParsed(updateInstance,inventory,emitter);
//                    }
//                    if(updateInstanceFS.length()>=2){
//                        emitter.send(SseEmitter.event().data(createEventData("Posílání žádosti o update atributů FS pro várku "+(p+1)+" / "+limit+"\n\n")));
//                        SharedMethods.updateInstanceParsed(updateInstanceFS,inventory,emitter);
//                    }
//                }
//
//            }
//        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - updateFromSNMP function exception - diossetupdateoldvalues query failed")));
//            SharedMethods.exceptionInServerLog(e, module, "updateFromSNMP", DiosSetUpdateOldValuesQuery.QUERY_DOCUMENT);
//        }
//
//    }
//
//    private static void updateFromSNMPlog(ArrayList<String> updated,ArrayList<String> upToDate,boolean update, boolean outputUpToDate, boolean outputUnchanged,int p, int limit,SseEmitter emitter) throws IOException {
//        // log SNMP
//        try {
//            if (updated.size() >= 1 && outputUnchanged) {
//                emitter.send(SseEmitter.event().data(createEventData("Neaktuální atributy SNMP pro várku " + (p + 1) + " / " + limit + "\n\n")));
//                for (int i = 0; i < updated.size(); i += 4) {
//                    try {
//                        emitter.send(SseEmitter.event().data(createEventData("id: " + updated.get(i))));
//                        emitter.send(SseEmitter.event().data(createEventData("did: " + updated.get(i + 1))));
//                        emitter.send(SseEmitter.event().data(createEventData("Neaktuální hodnota v inventory: " + updated.get(i + 2))));
//                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota ze SNMP: " + updated.get(i + 3) + "\n\n")));
//                    } catch (IndexOutOfBoundsException | IOException e) {
//                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty SNMP se nepodařilo přečíst", "error")));
//                    }
//                }
//            }
//            if (upToDate.size() >= 1 && !update && outputUpToDate) {
//                emitter.send(SseEmitter.event().data(createEventData("Aktuální atributy SNMP pro várku " + (p + 1) + " / " + limit + "\n\n")));
//                for (int i = 0; i < upToDate.size(); i += 3) {
//                    try {
//                        emitter.send(SseEmitter.event().data(createEventData("id: " + upToDate.get(i))));
//                        emitter.send(SseEmitter.event().data(createEventData("did: " + upToDate.get(i + 1))));
//                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota na inventory a SNMP: " + upToDate.get(i + 2) + "\n\n")));
//                    } catch (IndexOutOfBoundsException | IOException e) {
//                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty SNMP se nepodařilo přečíst", "error")));
//                    }
//                }
//            }
//        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - updateFromSNMPlog function exception")));
//            SharedMethods.exceptionInServerLog(e, module, "updateFromSNMPlog");
//        }
//    }
//    private static void updateFromFSlog(ArrayList<String> updatedFS,ArrayList<String> upToDateFS,boolean update, boolean outputUpToDate, boolean outputUnchanged,int p,int limit,SseEmitter emitter) throws IOException {
//        //log FS
//        try {
//            if (updatedFS.size() >= 1 && outputUnchanged) {
//                emitter.send(SseEmitter.event().data(createEventData("Neaktuální atributy FS pro várku " + (p + 1) + " / " + limit + "\n\n")));
//                for (int i = 0; i < updatedFS.size(); i += 4) {
//                    try {
//                        emitter.send(SseEmitter.event().data(createEventData("id: " + updatedFS.get(i))));
//                        emitter.send(SseEmitter.event().data(createEventData("did: " + updatedFS.get(i + 1))));
//                        emitter.send(SseEmitter.event().data(createEventData("Neaktuální hodnota na inventory: " + updatedFS.get(i + 2))));
//                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota ve FS: " + updatedFS.get(i + 3) + "\n\n")));
//                    } catch (IndexOutOfBoundsException | IOException e) {
//                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty FS se nepodařilo přečíst", "error")));
//                    }
//                }
//            }
//            if (upToDateFS.size() >= 1 && !update && outputUpToDate) {
//                emitter.send(SseEmitter.event().data(createEventData("Aktuální atributy FS pro várku " + (p + 1) + " / " + limit + "\n\n")));
//                for (int i = 0; i < upToDateFS.size(); i += 3) {
//                    try {
//                        emitter.send(SseEmitter.event().data(createEventData("id: " + upToDateFS.get(i))));
//                        emitter.send(SseEmitter.event().data(createEventData("did: " + upToDateFS.get(i + 1))));
//                        emitter.send(SseEmitter.event().data(createEventData("Aktuální hodnota ve FS a inventory: " + upToDateFS.get(i + 2) + "\n\n")));
//                    } catch (IndexOutOfBoundsException e) {
//                        emitter.send(SseEmitter.event().data(createEventData("Hodnoty FS se nepodařilo přečíst", "error")));
//                    }
//                }
//            }
//        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
//            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - updateFromFSlog function exception")));
//            SharedMethods.exceptionInServerLog(e, module, "updateFromFSlog");
//        }
//    }

    public static ArrayList<ArrayList<String>> getMissingAttributes(String attributesType,int size,int offset,String inventory, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        ArrayList<ArrayList<String>> missing = new ArrayList<>();
        try {
            CompletableFuture<GetAllAttributesQuery.Data> res = QueryHandler.execute(new GetAllAttributesQuery(attributesType,1,0), apolloClient);
        int count = res.get().instancesSet.count;
            int limit = count / offset;
            if (count % offset != 0) {
                limit = limit + 1;
            }
            for (int p = 0; p < limit; p++) {
                if (p == limit - 1) {
                    size = count - ((limit - 1) * size);
                }
                CompletableFuture<GetAllAttributesQuery.Data> allAttributes = QueryHandler.execute(new GetAllAttributesQuery(attributesType,size,offset*p), apolloClient);

                for(int i=0;i<allAttributes.get().devices.size();i++){
                    ArrayList<String> temp = new ArrayList<>();
                    temp.add(allAttributes.get().devices.get(i).internalId);
//                    temp.add(allAttributes.get().devices.get(i).id.value);
                    temp.add(Objects.requireNonNull(allAttributes.get().devices.get(i).id,"allAttributes.get().devices.get(i).id is null in getMissingAttributes").value);
                    ArrayList<String> searchForSNMP = AllignAttributesDynamic.potentialAttributes;
                    ArrayList<String> searchForFS = AllignAttributesDynamic.pkgFsInventoryValueDids;
                    for(int j=0;j<allAttributes.get().devices.get(i).attributes.size();j++) {
                        if(AllignAttributesDynamic.potentialAttributes.contains(allAttributes.get().devices.get(i).attributes.get(j).did.id)) {
                            searchForSNMP.remove(allAttributes.get().devices.get(i).attributes.get(j).did.id);
//                            temp.add(allAttributes.get().devices.get(i).attributes.get(j).did.id);
                        }
                        if(AllignAttributesDynamic.pkgFsInventoryValueDids.contains(allAttributes.get().devices.get(i).attributes.get(j).did.id)) {
                            searchForFS.remove(allAttributes.get().devices.get(i).attributes.get(j).did.id);
//                            temp.add(allAttributes.get().devices.get(i).attributes.get(j).did.id);
                        }
                    }
                    if(searchForSNMP.size()>=1) {
                        temp.addAll(searchForSNMP);
                    }
                    if(searchForFS.size()>=1) {
                        temp.addAll(searchForFS);
                    }
                    if(temp.size()>=3) {
                        missing.add(temp);
                    }
                }

            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributesDynamic - getMissingAttributes function exception - getallattributes query failed")));
            SharedMethods.exceptionInServerLog(e, module, "getMissingAttributes", GetAllAttributesQuery.QUERY_DOCUMENT);
        }
        return missing;
    }

    public static void addAttributes(ArrayList<ArrayList<String>> mis,int size,int offset,String inventory, SseEmitter emitter) throws IOException {
        int c = 0;
        String update="";
        for(int i=0;i<mis.size();i++){
            for(int j=2;j<mis.get(i).size();j++){ //pos 0 reserved internalId, pos 1 reserved ckod
                update = update + "UPDATE_INSTANCE:\n{\"id\":\"" + mis.get(i).get(0) + "\",\"" + mis.get(i).get(j) + "\":\"" + "" + "\"}" + "\n";
                if(c==size){
                    //send
                    SharedMethods.updateInstanceParsed(update,inventory,emitter);
                    update="";
                    c=0;
                }
                c++;
            }
        }
        if(c<size && update.length()>=1){
            SharedMethods.updateInstanceParsed(update,inventory,emitter);
        }
    }


}
