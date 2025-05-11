package ocmaintenance;
import com.apollographql.apollo.ApolloClient;
import ocmaintenance.controllers.SharedMethods;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static ocmaintenance.controllers.SharedMethods.createEventData;
import static ocmaintenance.controllers.SharedMethods.createException;

public class DPUBinderDynamic {
    private static String inventory=null;
    private static String dpuckod=null;
    private static String platockod=null;
    private static String sim=null;
    private static String placeckod=null;
    private static ArrayList<String> lvmckod= new ArrayList<>();
    private static ArrayList<String> vyvodckod= new ArrayList<>();
    private static String ahsckod=null;
    private static String zdrojckod=null;
    private static final String module = "DPUBinder";

    private static boolean handleLvmInput(String lvmField,String lvmField2,String lvmField3,ApolloClient apolloClient,int MAX_LVMCOUNT,SseEmitter emitter, String vyvodField,String vyvod2Field,String vyvod3Field) throws ExecutionException, InterruptedException, IOException {
        ArrayList<String> lvmList = new ArrayList<>();
        lvmList.add((String) SharedMethods.checkIfInputEmpty(lvmField,null));
        lvmList.add((String) SharedMethods.checkIfInputEmpty(lvmField2,null));
        lvmList.add((String) SharedMethods.checkIfInputEmpty(lvmField3,null));
        int c=1;
        for (int i = 0; i < lvmList.size(); i++) {
            if (lvmList.get(i)!=null) {
                if (Collections.frequency(lvmList, lvmList.get(i)) > 1) {
                    emitter.send(SseEmitter.event().data(createEventData("lvm: " + lvmList.get(i) + " bylo zadáno duplicitně, lvm ckod nok")));
                    return false;
                }
                try{
                    if(SharedMethods.validateCkod(lvmList.get(i), "lvm", "lvm", apolloClient).equals("ok")) {
                        lvmckod.add(lvmList.get(i));
                        emitter.send(SseEmitter.event().data(createEventData("lvm"+c+" ckod type ok")));
                    }else{
                        emitter.send(SseEmitter.event().data(createEventData("lvm"+c+" ckod type nok")));
                    }
                }catch (Exception e){
//                    emitter.send(SseEmitter.event().data(createEventData("DPUBinder handleLVM validate lvm"+e)));
//                    SharedMethods.exceptionInServerLog(e,module,"handleLVM", "validate ckod function failed");
                    createException(e,"handleLvmInput","DPUBinder - handleLvmInput function exception, line: ",module,emitter);
                }
                try {
                    if (!SharedMethods.validateModbus(lvmList.get(i), String.valueOf(i + 1), apolloClient).equals("ok")) {
                        emitter.send(SseEmitter.event().data(createEventData(SharedMethods.validateModbus(lvmList.get(i), String.valueOf(i + 1), apolloClient))));
                        return false;
                    }
                } catch (java.lang.IndexOutOfBoundsException e) {
                    emitter.send(SseEmitter.event().data(createEventData("Zadán nesprávný modbus")));
                    return false;
                }

            }else{
                lvmckod.add(null);
            }
            c++;
        }
                return true;
        }




    private static boolean handleInput(String dpuField,String platoField,String lvmField,String lvmField2,String lvmField3,String simField,String placeField,String ahsField,String zdrojField,String vyvodField,String vyvod2Field,String vyvod3Field,String inventoryField,ApolloClient apolloClient,int MAX_LVMCOUNT, int MAX_PLACECOUNT, int MAX_PLATOCOUNT, int MAX_SIMCOUNT,SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
        int MAXCOUNTER = 0;
        int count;
        int c=1;
        inventory = inventoryField;

        //LVM CHECK
        if (!handleLvmInput(lvmField, lvmField2, lvmField3, apolloClient, MAX_LVMCOUNT, emitter,vyvodField,vyvod2Field,vyvod3Field)) {
            return false;
        }

        //VYVOD CHECK
        if(!vyvodField.isEmpty()){
            if (SharedMethods.validateCkod(vyvodField, "place", "vyvod", apolloClient).equals("ok")) {
                vyvodckod.add(vyvodField);
                emitter.send(SseEmitter.event().data(createEventData("vyvodckod1 type ok")));
                if (lvmckod.get(0) != null) {
                    MAXCOUNTER = 1;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(vyvodField), Collections.singletonList(lvmckod.get(0)), "information:attribute.uplne_sjz", "information:attribute.ckod"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("vyvod1 a lvm1 sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("lvm")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet vyvodu na lvm, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - vyvod1-lvm1 exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput function exception query ExistingEdgesTest, line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);

                    }
                }
            }else {
                vyvodckod.add(null);
            }
        }else{
            vyvodckod.add(null);
        }
        if(!vyvod2Field.isEmpty()){
            if (SharedMethods.validateCkod(vyvod2Field, "place", "vyvod", apolloClient).equals("ok")) {
                vyvodckod.add(vyvod2Field);
                emitter.send(SseEmitter.event().data(createEventData("vyvodckod2 type ok")));
                if (lvmckod.get(1) != null) {
                    MAXCOUNTER = 1;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(vyvod2Field), Collections.singletonList(lvmckod.get(1)), "information:attribute.uplne_sjz", "information:attribute.ckod"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("vyvod2 a lvm2 sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("lvm")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet vyvodu2 na lvm2, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - vyvod2-lvm2 exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query exception ExistingEdgesTest vyvod2-lvm2, line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);

                    }
                }
            }else {
                vyvodckod.add(null);
            }
        }else{
            vyvodckod.add(null);
        }
        if(!vyvod3Field.isEmpty()){
            if (SharedMethods.validateCkod(vyvod3Field, "place", "vyvod", apolloClient).equals("ok")) {
                vyvodckod.add(vyvod3Field);
                emitter.send(SseEmitter.event().data(createEventData("vyvodckod3 type ok")));
                if (lvmckod.get(2) != null) {
                    MAXCOUNTER = 1;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(vyvod3Field), Collections.singletonList(lvmckod.get(2)), "information:attribute.uplne_sjz", "information:attribute.ckod"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("vyvod3 a lvm3 sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("lvm")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet vyvodu na lvm, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - vyvod3-lvm3 exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
            }else {
                vyvodckod.add(null);
            }
        }else{
            vyvodckod.add(null);
        }

        //PLATO CHECK
        if (!platoField.isEmpty()) {
            if (SharedMethods.validateCkod(platoField, "plato", "plato", apolloClient).equals("ok")) {
                platockod = platoField;
                emitter.send(SseEmitter.event().data(createEventData("platockod type ok")));
                if (!dpuField.isEmpty()) {
                    MAXCOUNTER = 1;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(platoField), Collections.singletonList(dpuField), "information:attribute.ckod", "information:attribute.ckod"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("plato a dpu sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("dpu")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet DPU na plato, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - plato-dpu exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTEst line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
                if (!placeField.isEmpty()) {
                    MAXCOUNTER = MAX_PLACECOUNT;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(platoField), Collections.singletonList(placeField), "information:attribute.ckod", "information:attribute.uplne_sjz"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("plato a place sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("place")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet place na plato, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - plato-place exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
                for(int j=0;j<lvmckod.size();j++) {
                    if (lvmckod.get(j) != null) {
                        MAXCOUNTER = MAX_LVMCOUNT;
                        count = 0;
                        try {
                            CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(platoField), Collections.singletonList(lvmckod.get(j)), "information:attribute.ckod", "information:attribute.ckod"), apolloClient);
                            try {
                                if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(createEventData("plato a lvm" + c + " sdílí vazby: ")));
                                    for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                        emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                    }
                                    emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                    return false;
                                }
                                for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                    if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("lvm")) {
                                        MAXCOUNTER++;
                                        if (count >= MAXCOUNTER) {
                                            emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet lvm na plato, navazbení neproběhne", "warning")));
                                            return false;
                                        }
                                    }
                                }
                            } catch (Exception e){
                                createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                            }
                        } catch (Exception e) {
//                            emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - plato-lvm"+c+" exception: " + e)));
//                            SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                            createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                        }
                    }
                    c++;
                }
            } else {
                emitter.send(SseEmitter.event().data(createEventData("platockod type nok")));
                platockod = null;
                return false;
            }
        }
        //DPU CHECK
        if (!dpuField.isEmpty()) {
            if (SharedMethods.validateCkod(dpuField, "dpu", "dpu", apolloClient).equals("ok")) {
                dpuckod = dpuField;
                emitter.send(SseEmitter.event().data(createEventData("dpuckod type ok")));
                if (!placeField.isEmpty()) {
                    MAXCOUNTER = MAX_PLACECOUNT;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(dpuField), Collections.singletonList(placeField), "information:attribute.ckod", "information:attribute.uplne_sjz"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("dpu a place sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("place")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet DPU na place, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - dpu-place exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
                if (!simField.isEmpty()) {
                    MAXCOUNTER = MAX_SIMCOUNT;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(dpuField), Collections.singletonList(simField), "information:attribute.ckod", "information:attribute.telefonni_cislo"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("dpu a sim sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("simcard")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet DPU na sim, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - dpu-place exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
                c=1;
                for(int j=0;j<lvmckod.size();j++) {
                    if (lvmckod.get(j) != null) {
                        MAXCOUNTER = MAX_LVMCOUNT;
                        count = 0;
                        try {
                            CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(dpuField), Collections.singletonList(lvmckod.get(j)), "information:attribute.ckod", "information:attribute.ckod"), apolloClient);
                            try {
                                if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                    emitter.send(SseEmitter.event().data(createEventData("dpu a lvm" + c + " sdílí vazby: ")));
                                    for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                        emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                    }
                                    emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                    return false;
                                }
                                for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                    if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("lvm")) {
                                        MAXCOUNTER++;
                                        if (count >= MAXCOUNTER) {
                                            emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet lvm na dpu, navazbení neproběhne", "warning")));
                                            return false;
                                        }
                                    }
                                }
                            }catch (Exception e){
                                createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                            }
                        } catch (Exception e) {
//                            emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - plato-lvm"+c+" exception: " + e)));
//                            SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                            createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                        }
                    }
                    c++;
                }
            } else {
                emitter.send(SseEmitter.event().data(createEventData("dpuckod type nok")));
                dpuckod = null;
                return false;
            }
        }


        //PLACE CHECK
        if (!placeField.isEmpty()) {
            if (SharedMethods.validateCkod(placeField, "place", "place", apolloClient).equals("ok")) {
                placeckod = placeField;
                emitter.send(SseEmitter.event().data(createEventData("placeckod type ok")));
            } else {
                placeckod = null;
                emitter.send(SseEmitter.event().data(createEventData("placeckod type nok")));
            }
        }
        // AHS
        if (!ahsField.isEmpty()) {
            if (SharedMethods.validateCkod(ahsField, "equipment", "ahs", apolloClient).equals("ok")) {
                ahsckod = ahsField;
                emitter.send(SseEmitter.event().data(createEventData("ahs type ok")));
                if (!placeField.isEmpty()) {
                    MAXCOUNTER = MAX_PLACECOUNT;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(ahsField), Collections.singletonList(placeField), "information:attribute.ckod", "information:attribute.uplne_sjz"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("ahs a place sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("place")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet AHS na place, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - ahs-place exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
                if (!platoField.isEmpty()) {
                    MAXCOUNTER = MAX_PLATOCOUNT;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(ahsField), Collections.singletonList(platoField), "information:attribute.ckod", "information:attribute.ckod"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("ahs a plato sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("plato")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet AHS na plato, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - ahs-plato exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
            } else {
                emitter.send(SseEmitter.event().data(createEventData("ahs type nok")));
                ahsckod = null;
                return false;
            }
        }

        //ZDROJ
        if (!zdrojField.isEmpty()) {
            if (SharedMethods.validateCkod(zdrojField, "equipment", "zdroj", apolloClient).equals("ok")) {
                zdrojckod = zdrojField;
                emitter.send(SseEmitter.event().data(createEventData("zdroj type ok")));
                if (!placeField.isEmpty()) {
                    MAXCOUNTER = MAX_PLACECOUNT;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(zdrojField), Collections.singletonList(placeField), "information:attribute.ckod", "information:attribute.uplne_sjz"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("ahs a place sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("place")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet AHS na place, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - ahs-place exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
                if (!platoField.isEmpty()) {
                    MAXCOUNTER = MAX_PLATOCOUNT;
                    count = 0;
                    try {
                        CompletableFuture<ExistingEdgesTestQuery.Data> response = QueryHandler.execute(new ExistingEdgesTestQuery(Collections.singletonList(dpuField), Collections.singletonList(platoField), "information:attribute.ckod", "information:attribute.ckod"), apolloClient);
                        try {
                            if (!response.get().existingEdge.get(0).edgeInstance.isEmpty()) {
                                emitter.send(SseEmitter.event().data(createEventData("zdroj a plato sdílí vazby: ")));
                                for (int i = 0; i < response.get().existingEdge.get(0).edgeInstance.size(); i++) {
                                    emitter.send(SseEmitter.event().data(createEventData(response.get().existingEdge.get(0).edgeInstance.get(i).internalId)));
                                }
                                emitter.send(SseEmitter.event().data(createEventData("Navazbení neproběhne..")));
                                return false;
                            }
                            for (int i = 0; i < response.get().instances.get(0).edgeInstance.size(); i++) {
                                if (response.get().instances.get(0).edgeInstance.get(i).edgeEndPoint.element.id.contains("plato")) {
                                    MAXCOUNTER++;
                                    if (count >= MAXCOUNTER) {
                                        emitter.send(SseEmitter.event().data(createEventData("Ja navazbeno maximální počet zdroj na plato, navazbení neproběhne", "warning")));
                                        return false;
                                    }
                                }
                            }
                        }catch (Exception e){
                            createException(e,"handleInput","DPUBinder - handleInput function exception, line: ",module,emitter);
                        }
                    } catch (Exception e) {
//                        emitter.send(SseEmitter.event().data(createEventData("DPUbinder handleInput - ExistingEdgesTest - zdroj-plato exception: " + e)));
//                        SharedMethods.exceptionInServerLog(e,module,"handleInput", ExistingEdgesTestQuery.QUERY_DOCUMENT);
                        createException(e,"handleInput","DPUBinder - handleInput query ExistingEdgesTest line: ",module,ExistingEdgesTestQuery.QUERY_DOCUMENT,emitter);
                    }
                }
            } else {
                emitter.send(SseEmitter.event().data(createEventData("zdroj type nok")));
                zdrojckod = null;
                return false;
            }
        }
        return true;
    }

    public static void main(
            String dpuField, String platoField, String lvmField, String lvmField2, String lvmField3,
            String simField, String placeField, String ahsField, String zdrojField, String inventory,
            String url, int MAX_LVMCOUNT, int MAX_PLATOCOUNT, int MAX_PLACECOUNT, int MAX_SIMCOUNT, SseEmitter emitter,
            String vyvodField, String vyvodField2, String vyvodField3) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        emitter.send(SseEmitter.event().data(createEventData("Kontrola typu zadanych ckodu:\n")));
        if (handleInput(dpuField, platoField, lvmField, lvmField2, lvmField3, simField, placeField, ahsField, zdrojField, vyvodField, vyvodField2, vyvodField3, inventory, apolloClient, MAX_LVMCOUNT, MAX_PLACECOUNT, MAX_PLATOCOUNT, MAX_SIMCOUNT, emitter)) {
            emitter.send(SseEmitter.event().data(createEventData("")));
            int c=1;
            for (int i = 0; i < lvmckod.size(); i++) {
                //dpu -> lvm
                if (lvmckod.get(i)!=null && dpuckod!=null) {
                    emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: DPU-LVM"+c+"\n")));
//                    assignLvm(dpuckod, lvmckod.get(i), emitter);
                    SharedMethods.createEdge(lvmckod.get(i),dpuckod,"accessible_by","information:attribute.ckod","information:attribute.ckod",inventory,emitter);

                }
                c++;
            }
            c=1;
            for (int i = 0; i < lvmckod.size(); i++) {
                //lvm -> vyvod
                try {
                    if (vyvodckod.get(i)!=null && lvmckod.get(i) != null) {
                        emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: LVM"+c+"-vyvod"+c+"\n")));
//                        assignVyvod(lvmckod.get(i), vyvodckod.get(i), emitter);
                        SharedMethods.createEdge(lvmckod.get(i),vyvodckod.get(i),"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
                    }
                } catch (java.lang.IndexOutOfBoundsException e) {

                }
                c++;
            }
            c=1;
            for (int i = 0; i < lvmckod.size(); i++) {
                //plato -> lvm
                if (lvmckod.get(i)!=null && platockod!=null) {
                    emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: Plato-LVM"+c+"\n")));
                    SharedMethods.createEdge(platockod,lvmckod.get(i),"contains","information:attribute.ckod","information:attribute.ckod",inventory,emitter);
//                    assignLvmToPlato(platockod, lvmckod.get(i), emitter);
                }
                c++;
            }
            c=1;
            lvmckod.clear();
            vyvodckod.clear();
            //plato -> dpu
            if (platockod!=null && dpuckod!=null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: DPU-Plato"+"\n")));
//                assignToPlato(dpuckod, platockod, emitter);
                SharedMethods.createEdge(platockod,dpuckod,"contains","information:attribute.ckod","information:attribute.ckod",inventory,emitter);

            }
            //place -> dpu
            if (placeckod!=null && dpuckod!= null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: DPU-Place"+"\n")));
//                assignDpuToPlace(dpuckod, placeckod, emitter);
                SharedMethods.createEdge(dpuckod,placeckod,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
            }
            //plato -> place
            if (placeckod!=null && platockod!= null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: Plato-Place"+"\n")));
//                assignDpuToPlace(platockod, placeckod, emitter);
                SharedMethods.createEdge(platockod,placeckod,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
            }
            // sim -> dpu
            if (sim!=null && dpuckod!=null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: DPU-SIM"+"\n")));
//                assignSim(dpuckod, sim, emitter);
                SharedMethods.createEdge(dpuckod,sim,"contains","information:attribute.ckod","information:attribute.telefonni_cislo",inventory,emitter);

            }
            // ahs -> plato
            if (ahsckod!=null && platockod!=null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: Plato-AHS"+"\n")));
                SharedMethods.createEdge(platockod,ahsckod,"contains","information:attribute.ckod","information:attribute.ckod",inventory,emitter);
            }
            // ahs -> DTS
            if (ahsckod!=null && platockod!=null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: AHS-DTS"+"\n")));
                SharedMethods.createEdge(ahsckod,placeckod,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
            }
            // zdroj -> plato
            if (zdrojckod!=null && platockod!=null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: Plato-zdroj"+"\n")));
                SharedMethods.createEdge(platockod,zdrojckod,"contains","information:attribute.ckod","information:attribute.ckod",inventory,emitter);
            }
            // zdroj -> DTS
            if (zdrojckod!=null && platockod!=null) {
                emitter.send(SseEmitter.event().data(createEventData("Probiha navázání: zdroj-DTS"+"\n")));
                SharedMethods.createEdge(zdrojckod,placeckod,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
            }

        }
        platockod=null;
        dpuckod=null;
        placeckod=null;
        ahsckod=null;
        zdrojckod=null;
        lvmckod.clear();
        vyvodckod.clear();
    }

}