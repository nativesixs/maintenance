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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ocmaintenance.controllers.SharedMethods.createEventData;


public class AllignAttributesDynamic {
    public static ArrayList<String> pkgFsInventoryValueDids = new ArrayList<>();
    public static ArrayList<String> pkgFsSnmpDids = new ArrayList<>();
    public static ArrayList<String> potentialAttributes = new ArrayList<>();
    public static int fromTimestampSubtractDays;
    private static final String module = "AllignAttributes";

    private static ArrayList<String> getMissingCkodAttributes(String ckod, ApolloClient apolloClient,SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<GetCkodAttributesQuery.Data> response = QueryHandler.execute(new GetCkodAttributesQuery(Collections.singletonList(ckod)), apolloClient);
            ArrayList<String> foundAttrs = new ArrayList<>();
            for (int i = 0; i < response.get().devices.get(0).attributes.size(); i++) {
                foundAttrs.add(response.get().devices.get(0).attributes.get(i).did.id);
            }
            ArrayList<String> missing = new ArrayList<>();
            for (String potentialAttribute : potentialAttributes) {
                if (!foundAttrs.contains(potentialAttribute)) {
                    missing.add(potentialAttribute);
                }
            }
            foundAttrs.clear();
            return missing;
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributes - getMissingCkodAttributes function exception - getCkodAttributes query failed")));
            SharedMethods.exceptionInServerLog(e,module,"getMissingCkodAttributes", GetCkodAttributesQuery.QUERY_DOCUMENT);

        }
        return null;
    }

    private static void compareDioValues(ArrayList<String> attributesToUpdate, CompletableFuture<GetCkodAttributesQuery.Data> attributesquery, ApolloClient apolloClient, Input<List<String>> internalId, List<String> kind, String format_, String timestampTo, String timestampFrom, String inventory,boolean update,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ArrayList<String> samevalue = new ArrayList<>();
        ArrayList<String> invalid = new ArrayList<>();
        ArrayList<String> valid = new ArrayList<>();
        ArrayList<String> pkg = new ArrayList<>();

        ArrayList<ArrayList<String>> lstSnmp = new ArrayList<>();
        try {
            CompletableFuture<DiosSetNewestDataQuery.Data> diosset = QueryHandler.execute(new DiosSetNewestDataQuery(internalId, kind, timestampTo, timestampFrom), apolloClient);
            //todo najit lepsi reseni nez force wait
            while (!diosset.isDone()) {
                Thread.sleep(100);
            }

            for (int i = 0; i < diosset.get().diosSet.groupByDid.size(); i++) {
                ArrayList<String> all = new ArrayList<>();
                all.add(diosset.get().diosSet.groupByDid.get(i).set.items.get(0).object.id.value);
                all.add(diosset.get().diosSet.groupByDid.get(i).set.items.get(0).did.id);
                all.add(diosset.get().diosSet.groupByDid.get(i).set.items.get(0).value.asText);
                lstSnmp.add(all);
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributes - getMissingCkodAttributes function exception - getCkodAttributes query failed")));
            SharedMethods.exceptionInServerLog(e,module,"getMissingCkodAttributes", GetCkodAttributesQuery.QUERY_DOCUMENT);
        }

        for (int i = 0; i < attributesToUpdate.size(); i++) {
            String snmpdid="";
            if (pkgFsInventoryValueDids.contains(attributesToUpdate.get(i))) {
                format_ = ".pkg";
                snmpdid = pkgFsSnmpDids.get(pkgFsInventoryValueDids.indexOf(attributesToUpdate.get(i)));
            }else{
                format_ = ".*";
                snmpdid=attributesToUpdate.get(i);
            }
            for(int j=0;j<lstSnmp.size();j++) {
                if (lstSnmp.get(j).get(2) != null) {
                    String snmpdidvalue = null;
                    // .pkg
                    if (pkgFsInventoryValueDids.contains(attributesToUpdate.get(i)) && lstSnmp.get(j).get(1).equals(pkgFsSnmpDids.get(pkgFsInventoryValueDids.indexOf(attributesToUpdate.get(i))))) {
                        String newdid;
                        newdid = pkgFsInventoryValueDids.get(pkgFsSnmpDids.indexOf(snmpdid));
                        Input<String> file = new Input<>(lstSnmp.get(j).get(2), true);
                        try {
                            //todo vyzkouset spolecne s filestorage uncommentem nahore
//                            Object didv = "";
//                            for (int k = 0; k < filestorage.get().filesSet.items.size(); k++) {
//                                if (filestorage.get().filesSet.items.get(k).key.contains(lstSnmp.get(j).get(2))) {
//                                    didv = filestorage.get().filesSet.items.get(k).metadata;
//                                }
//                            }
                            CompletableFuture<GetFilestorageFileMetadataQuery.Data> filestorage = QueryHandler.execute(new GetFilestorageFileMetadataQuery(file), apolloClient);
                            Object didv = filestorage.get().filesSet.items.get(0).metadata.toString();

                            try {
                                JsonObject jsonMeta = JsonParser.parseString((String) didv).getAsJsonObject();
                                JsonObject jsonVersion = JsonParser.parseString(String.valueOf(jsonMeta.get("metadata"))).getAsJsonObject();
                                String version = String.valueOf(jsonVersion.get("version"));
                                if (version.equals("null")) {
                                    emitter.send(SseEmitter.event().data(createEventData("V metadatech souboru: "+lstSnmp.get(j).get(2)+" nebyla nalezena verze")));
                                } else {
                                    int index = attributesquery.get().devices.get(0).attributes.indexOf(newdid);
                                    if (!version.equals(attributesquery.get().devices.get(0).attributes.get(index).normalizedValue)) {
                                        snmpdidvalue = version;
                                        if (update) {
                                            SharedMethods.updateInstance(attributesquery.get().devices.get(0).internalId, newdid, snmpdidvalue, inventory,emitter);
                                        } else {
                                            invalid.add(newdid);
                                            invalid.add("Nová hodnota: " + version);
                                            invalid.add("Aktuální hodnota: " + attributesquery.get().devices.get(0).attributes.get(index).normalizedValue);
                                        }
                                    } else {
                                        if (!update) {
                                            valid.add(attributesquery.get().devices.get(0).attributes.get(index).did.id);
                                            valid.add("Hodnota: " + lstSnmp.get(j).get(2));
                                        }
                                    }
                                }
                            } catch (IllegalStateException | IOException e) {
                                pkg.add("Metadata pro: " + lstSnmp.get(j).get(2) + " se nepodařilo přečíst");
                            }

                        } catch (IndexOutOfBoundsException e) {
                            if (update) {
                                emitter.send(SseEmitter.event().data(createEventData("Hodnota metadat z: "+lstSnmp.get(j).get(2)+" nebyla přečtena úspěšně, atribut: "+attributesToUpdate.get(i)+" nebude přidán/updatován")));
                            } else {
                                pkg.add("Hodnota metadat z: "+lstSnmp.get(j).get(2) + " pro: ");
                                pkg.add(attributesToUpdate.get(i)+" nebyla přečtena úspěšně");
                            }
                        }

                    } else if(lstSnmp.get(j).get(1).equals(attributesquery.get().devices.get(0).attributes.get(i).did.id)){
                        // .*
                        try {
                            if (!lstSnmp.get(j).get(2).equals(attributesquery.get().devices.get(0).attributes.get(i).normalizedValue)) {
                                snmpdidvalue = lstSnmp.get(j).get(2);
                                if (update) {
                                    SharedMethods.updateInstance(attributesquery.get().devices.get(0).internalId, lstSnmp.get(j).get(1), snmpdidvalue, inventory,emitter);
                                } else {
                                    invalid.add(lstSnmp.get(j).get(1));
                                    invalid.add("Nová hodnota: " + lstSnmp.get(j).get(2));
                                    invalid.add("Aktuální hodnota: " + attributesquery.get().devices.get(0).attributes.get(i).normalizedValue);
                                }
                            } else {
                                if (update) {
                                    samevalue.add("Atribut: " + attributesToUpdate.get(i) + " má přiřazenou aktuální hodnotu: " + lstSnmp.get(j).get(2));
                                } else {
                                    valid.add(attributesquery.get().devices.get(0).attributes.get(i).did.id);
                                    valid.add("Hodnota: " + lstSnmp.get(j).get(2));
                                }
                            }
                        } catch (java.lang.NullPointerException e) {
                            //lstSnmp value == null do smth
                        }
                    }else{
                        //did not found
                    }
                }
            }
        }

        if(update) {
            if (samevalue.size() >= 1) {
                emitter.send(SseEmitter.event().data(createEventData(""+"Nezměněné atributy: ")));
                for (String s : samevalue) {
                    emitter.send(SseEmitter.event().data(createEventData(s)));
                }
            }
        }else{

            if(valid.size()>=1) {
                emitter.send(SseEmitter.event().data(createEventData("Naposledy aktualizované atributy: ")));
                for (String value : valid) {
                    emitter.send(SseEmitter.event().data(createEventData(value)));
                }
            }
            if(invalid.size()>=1) {
                emitter.send(SseEmitter.event().data(createEventData("Neaktuální atributy: ")));
                for (String s : invalid) {
                    emitter.send(SseEmitter.event().data(createEventData(s)));
                }
            }
            if(pkg.size()>=1) {
                emitter.send(SseEmitter.event().data(createEventData("Nezkontrolovatelná .pkg: ")));
                for (String s : pkg) {
                    emitter.send(SseEmitter.event().data(createEventData(s)));
                }
            }
        }
        pkg.clear();
        valid.clear();
        invalid.clear();
        samevalue.clear();

    }

    private static void updateFromDiosSetData(ApolloClient apolloClient, ArrayList<String> attributesToUpdate, String internalId_, String kind_, CompletableFuture<GetCkodAttributesQuery.Data> attributesquery, String timestampFrom, String timestampTo, String inventory,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        Input<List<String>> internalId = new Input<List<String>>(new ArrayList<>(Collections.singleton(internalId_)), true);
        List<String> kind = new ArrayList<>(Collections.singleton(kind_));
        String format_ = ".*";
        compareDioValues(attributesToUpdate,attributesquery,apolloClient,internalId,kind,format_,timestampTo,timestampFrom,inventory,true,emitter);
    }

    private static void addAttributes(String ckod,String internalId, ArrayList<String> missing, String didvalue, String inventory, ApolloClient apolloClient,SseEmitter emitter) throws IOException {
        for (int i=0;i<missing.size();i++){
            SharedMethods.updateInstance(internalId,missing.get(i),didvalue,inventory,emitter);
//            String update = "UPDATE_INSTANCE:\n{\"id\":\"" + internalId + "\",\""+missing.get(i)+"\":\"" + didvalue + "\"}";
//            String rep = String.valueOf(Request.Post(inventory).bodyString(update, ContentType.DEFAULT_TEXT).execute().returnResponse().getStatusLine().getStatusCode());
        }
//        try {
//            CompletableFuture<GetCkodAttributesQuery.Data> attributesquery = QueryHandler.execute(new GetCkodAttributesQuery(Collections.singletonList(ckod)), apolloClient);
//            for (int i = 0; i < missing.size(); i++) {
//                if (attributesquery.get().devices.get(0).attributes.contains(missing.get(i))) {
//                    emitter.send(SseEmitter.event().data(createEventData("Přidán atribut: "+missing.get(i))));
//                } else {
//                    emitter.send(SseEmitter.event().data(createEventData(""+"Error: Atribut: "+missing.get(i)+" se nepodařilo přidat")));
//                }
//            }
//        }catch (Exception e){
//            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
//            emitter.send(SseEmitter.event().data(createEventData("AllignAttributes - addAttributes function exception - getckodattributes query failed")));
//            SharedMethods.exceptionInServerLog(e,module,"addAttributes", GetCkodAttributesQuery.QUERY_DOCUMENT);
//        }
    }

    public static ArrayList<String> getInvalidAttributeValues(ArrayList<String> attributesToUpdate, String internalId_, String kind_, ApolloClient apolloClient, CompletableFuture<GetCkodAttributesQuery.Data> response, String timestampTo, String timestampFrom, String inventory,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        String format_ = ".*";
        Input<List<String>> internalId = new Input<>(new ArrayList<>(Collections.singleton(internalId_)), true);
        List<String> kind = new ArrayList<>(Collections.singleton(kind_));
        compareDioValues(attributesToUpdate,response,apolloClient,internalId,kind,format_,timestampTo,timestampFrom,inventory,false,emitter);
        return null;
    }


    public static String createZonedTimestamp(LocalDate date){
        return ZonedDateTime.of(date.getYear(),date.getMonthValue(),date.getDayOfMonth(),00,00,00,00, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'")).toString();
    }

    private static void getOnlyAttributes(ArrayList<String> ckod, CompletableFuture<GetCkodAttributesQuery.Data> response,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        for(int j=0;j<ckod.size();j++) {
            emitter.send(SseEmitter.event().data(createEventData("Nalezené atributy pro: "+ckod.get(j))));
            for (int i = 0; i < response.get().devices.get(0).attributes.size(); i++) {
                emitter.send(SseEmitter.event().data(createEventData(response.get().devices.get(0).attributes.get(i).did.id)));
            }
        }
    }
    private static void getAttributeValues(String ckod, ApolloClient apolloClient, CompletableFuture<GetCkodAttributesQuery.Data> response, String inventory,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        emitter.send(SseEmitter.event().data(createEventData("Nalezené atributy pro: "+ckod)));
        ArrayList<String> attributes = new ArrayList<>();
        for (int i = 0; i < response.get().devices.get(0).attributes.size(); i++) {
            attributes.add(response.get().devices.get(0).attributes.get(i).did.id);
            emitter.send(SseEmitter.event().data(createEventData(response.get().devices.get(0).attributes.get(i).did.id)));
            emitter.send(SseEmitter.event().data(createEventData("Hodnota: "+response.get().devices.get(0).attributes.get(i).normalizedValue)));
        }
        emitter.send(SseEmitter.event().data(createEventData("")));
        String timestampTo = String.valueOf(ZonedDateTime.now());
        String timestampFrom=createZonedTimestamp(LocalDate.now().minusDays(fromTimestampSubtractDays));
        getInvalidAttributeValues(attributes, response.get().devices.get(0).internalId, response.get().devices.get(0).kind.id, apolloClient, response,timestampTo,timestampFrom,inventory,emitter);
    }

    private static void getOnlyMissingAttributes(ArrayList<String> ckod, ArrayList<String> missing,SseEmitter emitter) throws IOException {
        for(int j=0;j<ckod.size();j++) {
            emitter.send(SseEmitter.event().data(createEventData("Chybějící atributy pro: "+ckod.get(j))));
            for (int i = 0; i < missing.size(); i++) {
                emitter.send(SseEmitter.event().data(createEventData(missing.get(i))));
            }
        }
    }

    private static void getOnlyOutdatedAttributes(ApolloClient apolloClient, ArrayList<String> attributesToUpdate, String internalId_, String kind_, CompletableFuture<GetCkodAttributesQuery.Data> attributesquery, String timestampFrom, String timestampTo, String inventory,SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
        Input<List<String>> internalId = new Input<List<String>>(new ArrayList<>(Collections.singleton(internalId_)), true);
        List<String> kind = new ArrayList<>(Collections.singleton(kind_));
        String format_ = ".*";
        compareDioValues(attributesToUpdate,attributesquery,apolloClient,internalId,kind,format_,timestampTo,timestampFrom,inventory,false,emitter);
    }

    private static ArrayList<String> getDeviceKinds(String attributesType, ApolloClient apolloClient, SseEmitter emitter) throws IOException {
        ArrayList<String> devicekinds = new ArrayList<>();
        try {
            CompletableFuture<GetDeviceKindsQuery.Data> devicekindsresponse = QueryHandler.execute(new GetDeviceKindsQuery(attributesType), apolloClient);
            for (int i = 0; i < devicekindsresponse.get().deviceKinds.size(); i++) {
                devicekinds.add(devicekindsresponse.get().deviceKinds.get(i).id);
            }
            return devicekinds;
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("AllignAttributes - getDeviceKinds function exception - getdevicekinds query failed")));
            SharedMethods.exceptionInServerLog(e,module,"getDeviceKinds", GetDeviceKindsQuery.QUERY_DOCUMENT);
        }
        return null;
    }
    private static void initDpus(){ //aktualizuje seznam atributu po kterych se bude divat pro dpu
        potentialAttributes.clear();
        pkgFsInventoryValueDids.clear();
        pkgFsSnmpDids.clear();
//zaloha
//        potentialAttributes.addAll(Arrays.asList("information:attribute.balicek_aplikace_dpu","information:attribute.balicek_firmware_dpu","information:attribute.balicek_konfigurace_nezavisle_casti","information:attribute.balicek_konfigurace_zavisle_casti","information:attribute.verze_firmware_routeru"));
//        pkgFsSnmpDids.addAll(Arrays.asList("information:attribute.balicek_konfigurace_nezavisle_casti", "information:attribute.balicek_konfigurace_zavisle_casti"));
//        pkgFsInventoryValueDids.addAll(Arrays.asList("information:attribute.verze_konfigurace_dpu", "information:attribute.verze_konfigurace_zavisle_casti"));

        potentialAttributes.addAll(Arrays.asList(
                "information:attribute.verze_firmware_dpu",
                "information:attribute.verze_firmware_routeru",
                "information:attribute.balicek_konfigurace_zavisle_casti",
                "information:attribute.balicek_konfigurace_dpu"
        ));
        pkgFsSnmpDids.addAll(Arrays.asList("information:attribute.balicek_konfigurace_dpu", "information:attribute.balicek_konfigurace_zavisle_casti"));
        pkgFsInventoryValueDids.addAll(Arrays.asList("information:attribute.verze_konfigurace_dpu", "information:attribute.verze_konfigurace_zavisle_casti"));
    }
    private static void initLvms(){ //aktualizuje seznam atributu po kterych se bude divat pro lvm
        potentialAttributes.clear();
        pkgFsInventoryValueDids.clear();
        pkgFsSnmpDids.clear();

        potentialAttributes.addAll(Arrays.asList("information:attribute.verze_firmware_lvm"));
    }

    public static int getParsingSize(int size,int offset,int count) {
//        int count = res.get().instancesSet.count;
        int limit = count / offset;
        if (count % offset != 0) {
            limit = limit + 1;
        }
        for (int p = 0; p < limit; p++) {
            if (p == limit - 1) {
                size = count - ((limit - 1) * size);
            }
        }
        return size;
    }

    public static void main(String ckod, int logset, int mode, int rozsah, int zarizeni, int sizeField, String inventory, String url, int SNMP_MAXHISTORY_DAYS, SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
        emitter.send(SseEmitter.event().data(createEventData("")));
        fromTimestampSubtractDays=SNMP_MAXHISTORY_DAYS;
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        ArrayList<String> deviceCkodList = new ArrayList<>();
        ArrayList<String> internalIdList = new ArrayList<>();
        ArrayList<String> missing = new ArrayList<>();
        ArrayList<String> foundAttrs = new ArrayList<>();

        // rozsah = 0 -> pouze 1 zarizeni
        // rozsah = 1 -> vsechna zarizeni na inventory

        if (rozsah == 0) {
            if (SharedMethods.validateCkod(ckod, "dpu", "dpu", apolloClient).equals("ok")) {
                initDpus();
            } else if (SharedMethods.validateCkod(ckod, "lvm", "lvm", apolloClient).equals("ok")) {
                initLvms();
            } else {
                emitter.send(SseEmitter.event().data(createEventData("Zadaný ckod není ckod dpu nebo lvm")));
                return;
            }
            try {
                CompletableFuture<GetCkodAttributesQuery.Data> attributesquery = QueryHandler.execute(new GetCkodAttributesQuery(Collections.singletonList(ckod)), apolloClient);
                for (int i = 0; i < attributesquery.get().devices.get(0).attributes.size(); i++) {
                    foundAttrs.add(attributesquery.get().devices.get(0).attributes.get(i).did.id);
                }
                internalIdList.add(attributesquery.get().devices.get(0).internalId);
                deviceCkodList.add(ckod);
                missing = getMissingCkodAttributes(ckod, apolloClient, emitter);

                switch (mode) {
                    case 0: // vypsat atributy
                        if (logset == 0) {
                            getAttributeValues(deviceCkodList.get(0), apolloClient, attributesquery, inventory, emitter);
                        } else if (logset == 1) {
                            getOnlyAttributes(deviceCkodList, attributesquery, emitter);
                        } else if (logset == 2) {
                            getOnlyMissingAttributes(deviceCkodList, missing, emitter);
                        } else if (logset == 3) {
                            String timestampTo = String.valueOf(ZonedDateTime.now());
                            String timestampFrom = createZonedTimestamp(LocalDate.now().minusDays(fromTimestampSubtractDays));
                            getOnlyOutdatedAttributes(apolloClient, foundAttrs, attributesquery.get().devices.get(0).internalId, attributesquery.get().devices.get(0).kind.id, attributesquery, timestampFrom, timestampTo, inventory, emitter);
                        }
                        break;
                    case 1: // pridat chybejici atributy
                        String timestampTo = String.valueOf(ZonedDateTime.now());
                        String timestampFrom = createZonedTimestamp(LocalDate.now().minusDays(fromTimestampSubtractDays));
                        addAttributes(deviceCkodList.get(0), internalIdList.get(0), missing, "", inventory, apolloClient, emitter);
                        break;
                    case 2: // aktualizovat atributy
                        timestampTo = String.valueOf(ZonedDateTime.now());
                        timestampFrom = createZonedTimestamp(LocalDate.now().minusDays(fromTimestampSubtractDays));
                        updateFromDiosSetData(
                                apolloClient,
                                foundAttrs,
                                attributesquery.get().devices.get(0).internalId,
                                attributesquery.get().devices.get(0).kind.id,
                                attributesquery,
                                timestampFrom,
                                timestampTo,
                                inventory,
                                emitter);
                        break;
                }
            } catch (Exception e) {
                emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e), "error")));
                emitter.send(SseEmitter.event().data(createEventData("AllignAttributes - main function exception - getCkodAttributes query failed")));
                SharedMethods.exceptionInServerLog(e, module, "main", GetCkodAttributesQuery.QUERY_DOCUMENT);
            }

        } else if (rozsah == 1){

            String attributesType = null;
            if(zarizeni == 0){ //pro vsechna DPU
                initDpus();
                attributesType = "dpu";
            } else if (zarizeni == 1) { // pro vsechna LVM
                initLvms();
                attributesType = "lvm";
            } else{
                emitter.send(SseEmitter.event().data(createEventData("Neznámý typ zařízení: "+zarizeni+" očekáváno <0(dpu),1(lvm)>","error")));
                return;
            }

            if(sizeField==0){
                sizeField=999999999;
            } else if (sizeField<0) {
                emitter.send(SseEmitter.event().data(createEventData("Query size nemuze mit zapornou velikost")));
                return;
            }
            int size = sizeField, offset = size;

            switch (mode) {
                case 0: // vypsat atributy
                    if (logset == 0) {
                        AllignAttributesInventoryDynamic.getAttributesValues(attributesType,size,offset,inventory,apolloClient,emitter);
//                                AllignAttributesInventoryDynamic.getAttributesValues(apolloClient, allAttributes, attributesType, deviceKinds, inventory, emitter);
                    } else if (logset == 1) {
                        AllignAttributesInventoryDynamic.getAttributesValues(attributesType,size,offset,inventory,apolloClient,emitter);
//                        emitter.send(SseEmitter.event().data(createEventData("Neimplementovano","error")));
//                                AllignAttributesInventoryDynamic.getOnlyAttributes(allAttributes, emitter);
                    } else if (logset == 2) {
                        AllignAttributesInventoryDynamic.getOnlyMissingAttributes(attributesType,size,offset,inventory,apolloClient,emitter);
//                                AllignAttributesInventoryDynamic.getOnlyMissingAttributes(allAttributes, emitter);
                    } else if (logset == 3) {
                        AllignAttributesInventoryDynamic.getOnlyOutdatedAttributes(attributesType,size,offset,inventory,apolloClient,emitter);
//                                AllignAttributesInventoryDynamic.getAttributesValues(apolloClient, allAttributes, attributesType, deviceKinds, inventory, emitter);
                    }
                    break;
                case 1: // pridat chybejici atributy
                    ArrayList<ArrayList<String>> mis = AllignAttributesInventoryDynamic.getMissingAttributes(attributesType,size,offset,inventory,apolloClient,emitter);
                    AllignAttributesInventoryDynamic.addAttributes(mis,size,offset,inventory, emitter);
                    break;
                case 2: // aktualizovat atributy
//                    AllignAttributesInventoryDynamic.updateFromSNMP(attributesType,size,offset,true,false,true,inventory,apolloClient,emitter);
                    AllignAttributesInventoryDynamic.updateFromSNMP2(attributesType,size,offset,true,false,true,true,inventory,apolloClient,emitter);
                break;
                case 3: // vypsat jak by byly atributy aktualizovany
//                    AllignAttributesInventoryDynamic.updateFromSNMP(attributesType,size,offset,true,false,true,inventory,apolloClient,emitter);
                    AllignAttributesInventoryDynamic.updateFromSNMP2(attributesType,size,offset,true,false,true,false,inventory,apolloClient,emitter);
                break;
            }
        }

    }



}
