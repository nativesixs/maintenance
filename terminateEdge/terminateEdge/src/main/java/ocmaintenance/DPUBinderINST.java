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

public class DPUBinderINST {
    private static String inventory=null;
    private static String dpuckod=null;
    private static String platockod=null;
    private static String sim=null;
    private static String placeckod=null;
    private static ArrayList<String> vyvodckod= new ArrayList<>();
    private static ArrayList<String> lvmckod= new ArrayList<>();
    private static String ahsckod=null;
    private static String zdrojckod=null;
    private static final String module = "DPUBinderINST";

    private static boolean handleLvmInput(String dpuField,String lvmField,String lvmField2,String lvmField3,ApolloClient apolloClient,int MAX_LVMCOUNT,SseEmitter emitter) throws IOException {
        ArrayList<String> lvmList=new ArrayList<>();
            try {
            lvmList.add((String) SharedMethods.checkIfInputEmpty(lvmField,""));
            lvmList.add((String) SharedMethods.checkIfInputEmpty(lvmField2,""));
            lvmList.add((String) SharedMethods.checkIfInputEmpty(lvmField3,""));
            for(int i=0;i<lvmList.size();i++){
                if(!lvmList.get(i).equals("")) {
                    if (Collections.frequency(lvmList, lvmList.get(i)) > 1) {
                        emitter.send(SseEmitter.event().data(createEventData("lvm: "+lvmList.get(i)+" bylo zadáno duplicitně, lvm ckod nok")));
                        return false;
                    }
                }
            }

            for(int i=0;i<lvmList.size();i++){
                if(!lvmList.get(i).equals("")) {
                    try {
                        if (!SharedMethods.validateModbus(lvmList.get(i), String.valueOf(i + 1), apolloClient).equals("ok")) {
                            emitter.send(SseEmitter.event().data(createEventData(SharedMethods.validateModbus(lvmList.get(i), String.valueOf(i + 1), apolloClient))));
                            return false;
                        }
                    }catch (java.lang.IndexOutOfBoundsException e){
                        emitter.send(SseEmitter.event().data(createEventData("spatny modbus")));
                        return false;
                    }
                }
            }

            lvmList.removeAll(Collections.singleton(""));
            CompletableFuture<CheckEdgesExistQuery.Data> response = QueryHandler.execute(new CheckEdgesExistQuery(Collections.singletonList(dpuField)), apolloClient);
            ArrayList<String> comp = new ArrayList<>();
            for (int j = 0; j < lvmList.size(); j++) {
                if (SharedMethods.validateCkod(lvmList.get(j), "lvm", "lvm", apolloClient).equals("ok")) {
                    for (int i = 0; i < response.get().instances.get(0).lvm.size(); i++) {
                        comp.add(Objects.requireNonNull(response.get().instances.get(0).lvm.get(i).id).value);
                    }
                    if (comp.contains(lvmList.get(j))) {
                        emitter.send(SseEmitter.event().data(createEventData("Vazba mezi DPU a "+"LVM již existuje")));
                        emitter.send(SseEmitter.event().data(createEventData("lvm ckod nok")));
                        lvmckod.add(null);
                    } else if (response.get().instances.get(0).lvm_idlist.size() >= MAX_LVMCOUNT) {
                        lvmckod.add(null);
                        emitter.send(SseEmitter.event().data(createEventData("překročen maximální počet navazbitelných lvm")));
                    } else {
                        lvmckod.add(lvmList.get(j));
                        emitter.send(SseEmitter.event().data(createEventData("lvm ckod ok")));
                    }
                    comp.clear();
    //                lvmList.clear();
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("DPUBinder - handleLvmInput function exception - checkedgesexist query failed")));
                SharedMethods.exceptionInServerLog(e,module,"handleLvmInput", CheckEdgesExistQuery.QUERY_DOCUMENT);
        }

        return true;
    }


    private static boolean handleInput(String dpuField,String platoField,String lvmField,String lvmField2,String lvmField3,String simField,String placeField,String ahsField,String zdrojField,String vyvodField,String vyvod2Field,String vyvod3Field,String inventoryField,ApolloClient apolloClient,int MAX_LVMCOUNT, int MAX_PLACECOUNT, int MAX_PLATOCOUNT, int MAX_SIMCOUNT,SseEmitter emitter) throws IOException {
        if(dpuField.isEmpty()){
            emitter.send(SseEmitter.event().data(createEventData("Nutno zadat alespoň ckod DPU a ckod umístění/zařízení k vytvoření vazby.")));
            return false;
        }
        try {
            CompletableFuture<CheckEdgesExistQuery.Data> response = QueryHandler.execute(new CheckEdgesExistQuery(Collections.singletonList(dpuField)), apolloClient);
            ArrayList<String> comp = new ArrayList<>();

            if (SharedMethods.validateCkod(dpuField, "dpu", "dpu", apolloClient).equals("ok")) {
                dpuckod = dpuField;
                emitter.send(SseEmitter.event().data(createEventData("dpu ckod ok")));
            } else {
                emitter.send(SseEmitter.event().data(createEventData("dpu ckod nok")));
                return false;
            }

            if (!handleLvmInput(dpuckod, lvmField, lvmField2, lvmField3, apolloClient, MAX_LVMCOUNT,emitter)) {
                return false;
            }


            if (SharedMethods.validateCkod(platoField, "plato", "plato", apolloClient).equals("ok")) {
                for (int i = 0; i < response.get().instances.get(0).plato.size(); i++) {
                    comp.add(Objects.requireNonNull(response.get().instances.get(0).plato.get(i).id).value);
                }
                if (comp.contains(platoField)) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazba mezi DPU a: "+"platem již existuje")));
                    emitter.send(SseEmitter.event().data(createEventData("plato ckod nok")));
                    platockod = null;
                } else if (response.get().instances.get(0).plato_idlist.size() >= MAX_PLATOCOUNT) {
                    platockod = null;
                    emitter.send(SseEmitter.event().data(createEventData("překročen maximální počet navazbitelných plat")));
                } else {
                    platockod = platoField;
                    emitter.send(SseEmitter.event().data(createEventData("plato ckod ok")));
                }
                comp.clear();
            }
            if (SharedMethods.validateCkod(placeField, "place", "place", apolloClient).equals("ok")) {
                for (int i = 0; i < response.get().instances.get(0).place.size(); i++) {
                    comp.add(Objects.requireNonNull(response.get().instances.get(0).place.get(i).id).value);
                }
                if (comp.contains(placeField)) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazba mezi DPU a "+"umístěním již existuje")));
                    emitter.send(SseEmitter.event().data(createEventData("place ckod nok")));
                    placeckod = null;
                } else if (response.get().instances.get(0).place_idlist.size() >= MAX_PLACECOUNT) {
                    placeckod = null;
                    emitter.send(SseEmitter.event().data(createEventData("překročen maximální počet navazbitelných umístění")));
                } else {
                    placeckod = placeField;
                    emitter.send(SseEmitter.event().data(createEventData("trafo ckod ok")));
                }
                comp.clear();
            }
            if (SharedMethods.validateCkod(vyvodField, "place", "place", apolloClient).equals("ok")) {
                for (int i = 0; i < response.get().instances.get(0).place.size(); i++) {
                    comp.add(Objects.requireNonNull(response.get().instances.get(0).place.get(i).id).value);
                }
                if (comp.contains(vyvodField)) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazba mezi DPU a "+"vyvodem1 již existuje")));
                    emitter.send(SseEmitter.event().data(createEventData("place ckod nok")));
                    vyvodckod.add(null);
                } else if (response.get().instances.get(0).place_idlist.size() >= MAX_PLACECOUNT) {
                    vyvodckod.add(null);
                    emitter.send(SseEmitter.event().data(createEventData("překročen maximální počet navazbitelných umístění - vyvod1ckod")));
                } else {
                    vyvodckod.add(vyvodField);
                    emitter.send(SseEmitter.event().data(createEventData("vyvod1 ckod ok")));
                }
                comp.clear();
            }
            if (SharedMethods.validateCkod(vyvod2Field, "place", "place", apolloClient).equals("ok")) {
                for (int i = 0; i < response.get().instances.get(0).place.size(); i++) {
                    comp.add(Objects.requireNonNull(response.get().instances.get(0).place.get(i).id).value);
                }
                if (comp.contains(vyvod2Field)) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazba mezi DPU a "+"vyvodem2 již existuje")));
                    emitter.send(SseEmitter.event().data(createEventData("place ckod nok")));
                    vyvodckod.add(null);
                } else if (response.get().instances.get(0).place_idlist.size() >= MAX_PLACECOUNT) {
                    vyvodckod.add(null);
                    emitter.send(SseEmitter.event().data(createEventData("překročen maximální počet navazbitelných umístění - vyvod2ckod")));
                } else {
                    vyvodckod.add(vyvod2Field);
                    emitter.send(SseEmitter.event().data(createEventData("vyvod2 ckod ok")));
                }
                comp.clear();
            }
            if (SharedMethods.validateCkod(vyvod3Field, "place", "place", apolloClient).equals("ok")) {
                for (int i = 0; i < response.get().instances.get(0).place.size(); i++) {
                    comp.add(Objects.requireNonNull(response.get().instances.get(0).place.get(i).id).value);
                }
                if (comp.contains(vyvod3Field)) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazba mezi DPU a "+"vyvodem3 již existuje")));
                    emitter.send(SseEmitter.event().data(createEventData("place ckod nok")));
                    vyvodckod.add(null);
                } else if (response.get().instances.get(0).place_idlist.size() >= MAX_PLACECOUNT) {
                    vyvodckod.add(null);
                    emitter.send(SseEmitter.event().data(createEventData("překročen maximální počet navazbitelných umístění - vyvod3ckod")));
                } else {
                    vyvodckod.add(vyvod3Field);
                    emitter.send(SseEmitter.event().data(createEventData("vyvod3 ckod ok")));
                }
                comp.clear();
            }
            if (SharedMethods.validateCkod(simField, "simcard", "simcard", apolloClient).equals("ok")) {
                for (int i = 0; i < response.get().instances.get(0).sim.size(); i++) {
                    comp.add(Objects.requireNonNull(response.get().instances.get(0).sim.get(i).id).value);
                }
                if (comp.contains(simField)) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazba mezi DPU a "+"SIM již existuje")));
                    emitter.send(SseEmitter.event().data(createEventData("sim číslo nok")));
                    sim = null;
                } else if (response.get().instances.get(0).sim_idlist.size() >= MAX_SIMCOUNT) {
                    sim = null;
                    emitter.send(SseEmitter.event().data(createEventData("překročen maximální počet navazbitelných sim")));
                } else {
                    sim = simField;
                    emitter.send(SseEmitter.event().data(createEventData("sim číslo ok")));
                }
                comp.clear();
            }
            if (!ahsField.isEmpty()) {
                ahsckod = ahsField;
                emitter.send(SseEmitter.event().data(createEventData("ahs ckod zaznamenán")));
            } else {
                ahsckod = null;
            }
            if (!zdrojField.isEmpty()) {
                zdrojckod = zdrojField;
                emitter.send(SseEmitter.event().data(createEventData("zdroj ckod zaznamenán")));
            } else {
                zdrojckod = null;
            }
            inventory = inventoryField;
            return true;
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("DPUBinder - handleInput function exception - checkedgesexist query failed")));
            SharedMethods.exceptionInServerLog(e,module,"handleInput", CheckEdgesExistQuery.QUERY_DOCUMENT);
        }
        return false;
    }

    public static void main(
            String dpuField, String platoField, String lvmField, String lvmField2, String lvmField3,
            String simField, String placeField, String ahsField, String zdrojField,String vyvodField,String vyvod2Field,String vyvod3Field, String inventory,
            String url, int MAX_LVMCOUNT, int MAX_PLATOCOUNT, int MAX_PLACECOUNT, int MAX_SIMCOUNT, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        emitter.send(SseEmitter.event().data(createEventData("")));

        if (!handleInput(dpuField, platoField, lvmField, lvmField2, lvmField3, simField, placeField, ahsField, zdrojField,vyvodField,vyvod2Field,vyvod3Field, inventory, apolloClient, MAX_LVMCOUNT,MAX_PLACECOUNT,MAX_PLATOCOUNT,MAX_SIMCOUNT,emitter)) {
            return;
        }

        for (int i = 0; i < lvmckod.size(); i++) {
            if (!(lvmckod.get(i) == null)) {
//                assignLvm(dpuckod, lvmckod.get(i),emitter);
                SharedMethods.createEdge(lvmckod.get(i),dpuckod,"accessible_by","information:attribute.ckod","information:attribute.ckod",inventory,emitter);
                try {
                    if (!(vyvodckod.get(i) == null)) {
//                        assignVyvod(lvmckod.get(i), vyvodckod.get(i), emitter);
                        SharedMethods.createEdge(lvmckod.get(i),vyvodckod.get(i),"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
                    }
                }catch (java.lang.IndexOutOfBoundsException e){

                }
            }
        }
        lvmckod.clear();
        vyvodckod.clear();
        if (!(platockod == null)) {
//            assignToPlato(dpuckod, platockod,emitter);
            SharedMethods.createEdge(platockod,dpuckod,"contains","information:attribute.ckod","information:attribute.ckod",inventory,emitter);
        }
        if (!(placeckod == null)) {
//            assignDpuToPlace(dpuckod, placeckod,emitter);
            SharedMethods.createEdge(dpuckod,placeckod,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
        }
        if (!(placeckod == null) && !(platockod == null)) {
//            assignDpuToPlace(platockod, placeckod,emitter);
            SharedMethods.createEdge(platockod,placeckod,"installed_at","information:attribute.ckod","information:attribute.uplne_sjz",inventory,emitter);
        }
        if (!(sim == null)) {
//            assignSim(dpuckod, sim,emitter);
            SharedMethods.createEdge(dpuckod,sim,"contains","information:attribute.ckod","information:attribute.telefonni_cislo",inventory,emitter);
        }
        if (!(ahsckod == null)) {
//            assignLvm(dpuckod, ahsckod,emitter);
            SharedMethods.createEdge(ahsckod,platockod,"accessible_by","information:attribute.ckod","information:attribute.ckod",inventory,emitter);
        }
        if (!(zdrojckod == null)) {
//            assignLvm(dpuckod, zdrojckod,emitter);
            SharedMethods.createEdge(zdrojckod,platockod,"accessible_by","information:attribute.ckod","information:attribute.ckod",inventory,emitter);
        }
    }

}