package ocmaintenance;

import com.apollographql.apollo.ApolloClient;
import ocmaintenance.controllers.SharedMethods;
import org.apache.http.client.fluent.Request;
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

public class DuplicatesDynamic {
    public static ArrayList<ArrayList<String>> dupe = new ArrayList<>();
    private static final String module = "Duplicates";
    public static String identify(String ckod, ApolloClient apolloClient, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        String type = "";
        String devices = "";
        String places="";
        try {
            CompletableFuture<IdentifyQuery.Data> response = QueryHandler.execute(new IdentifyQuery(Collections.singletonList(ckod)), apolloClient);
            try {
                type = response.get().instances.get(0).element.coreElement.id;
            }catch (java.lang.IndexOutOfBoundsException e){}
            try {
                devices = response.get().devices.get(0).element.parent.id;
            }catch (java.lang.IndexOutOfBoundsException e){}
            try {
                type = response.get().sim.get(0).element.coreElement.id;
            }catch (java.lang.IndexOutOfBoundsException e){}
            try{
                type = response.get().places.get(0).element.coreElement.id;
            }catch (java.lang.IndexOutOfBoundsException e){
                try{
                    type = response.get().places_gis.get(0).element.coreElement.id;
                }catch (java.lang.IndexOutOfBoundsException s){

                }
            }
            try{
                places = response.get().places.get(0).element.id;
            }catch (java.lang.IndexOutOfBoundsException e){
                try{
                    places = response.get().places_gis.get(0).element.id;
                }catch (java.lang.IndexOutOfBoundsException | java.lang.NullPointerException s){

                }
            }
            switch (type){
                case "plato":
                    return "plato";
                case "device":
                    if(devices.equals("device:device.lvm")){
                        return "lvm";
                    } else if (devices.equals("device:device.dpu")) {
                        return "dpu";
                    }
                    break;
                case "simcard":
                    return "sim";
                case "place":
                    switch(places){
                        case "place:place.distribucni_trafostanice":
                            return "dist_trafo";
                        case "place:place.rozvadec_nn":
                            return "rozvadec";
                        case "place:place.pozice_zarizeni":
                            return "pozice_zar";
                        case "place:place.vyvod_sekundarni_strana_trafa":
                            return "vyvod";
                        case "place:place.kobka_vn":
                            return "kobka";
                        case "place:place.pozice_trafa":
                            return "pozice_trafa";
                        case "place:place.trafo":
                            return "trafo";
                        default:
                            break;
                    }
                    break;
            }

        }catch (java.lang.IndexOutOfBoundsException | java.lang.NullPointerException e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("Duplicates - identify function exception")));
            SharedMethods.exceptionInServerLog(e,module,"identify", IdentifyQuery.QUERY_DOCUMENT);
            return null;
        }

        return null;
    }

    //checks modbus duplicates for plato->lvm
    private static void figurePLvm(List<GetDeviceEdgesQuery.Lvm1> lvmlist, List<GetDeviceEdgesQuery.Lvm_modbus1> lvm_modbus,SseEmitter emitter) throws IOException {
        for(int i=0;i<lvm_modbus.size();i++) {
            if(Collections.frequency(lvm_modbus, lvm_modbus.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("modus: "+lvm_modbus.get(i).modbus.get(0).normalizedValue+" je duplicitně přiřazen na lvm: "+Objects.requireNonNull(lvmlist.get(i).edgeEndPoint.id).value+"")));
            }
        }
        for(int i=0;i<lvmlist.size();i++){
            if(Collections.frequency(lvmlist, lvmlist.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("lvm: "+lvmlist.get(i).internalId+" je navazbeno více než 1x")));
            }
        }
    }
    //checks modbus duplicates for dpu->lvm
    private static void figureDLvm(List<GetDeviceEdgesQuery.Lvm> lvmlist, List<GetDeviceEdgesQuery.Lvm_modbus> lvm_modbus,SseEmitter emitter) throws IOException {
        for(int i=0;i<lvm_modbus.size();i++) {
            if(Collections.frequency(lvm_modbus, lvm_modbus.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("modbus: "+lvm_modbus.get(i).modbus.get(0).normalizedValue+" je duplicitně přiřazen na: "+Objects.requireNonNull(lvmlist.get(i).edgeEndPoint.id).value+"")));
            }
        }

        for(int i=0;i<lvmlist.size();i++){
            if(Collections.frequency(lvmlist, lvmlist.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("lvm: "+lvmlist.get(i).internalId+" je navazbeno více než 1x")));
            }
        }
    }




    private static void getDevice(String ckod, String type, ApolloClient apolloClient, int logset, SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<GetDeviceEdgesQuery.Data> response = QueryHandler.execute(new GetDeviceEdgesQuery(Collections.singletonList(ckod)), apolloClient);
            switch (type) {
                case "plato":
                    emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod)));
                    figurePLvm(response.get().plato.get(0).lvm, response.get().plato.get(0).lvm_modbus,emitter);
                    if (response.get().plato.get(0).lvm.size() > 3) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().plato.get(0).lvm.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().plato.get(0).lvm.get(i).internalId+" lvm modbus: "+response.get().plato.get(0).lvm_modbus.get(i).modbus.get(0).normalizedValue+" vazba: plato-lvm "+""+response.get().plato.get(0).id.value+" -> "+response.get().plato.get(0).lvm.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().plato.get(0).lvm.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("plato-lvm: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().plato.get(0).place.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().plato.get(0).place.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().plato.get(0).place.get(i).internalId+" vazba: plato-place "+response.get().plato.get(0).id.value+" -> "+response.get().plato.get(0).place.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().plato.get(0).place.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("plato-place: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().plato.get(0).dpu.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().plato.get(0).dpu.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().plato.get(0).dpu.get(i).internalId+" vazba: plato-dpu "+response.get().plato.get(0).id.value+" -> "+response.get().plato.get(0).dpu.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().plato.get(0).dpu.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("plato-dpu: nebyly nalezeny vazby mimo limit")));
                    }
                    break;
                case "lvm":
                    emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod)));
                    if (response.get().lvm.get(0).place.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().lvm.get(0).place.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().lvm.get(0).place.get(i).internalId+" vazba: lvm-place "+response.get().lvm.get(0).id.value+" -> "+response.get().lvm.get(0).place.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().lvm.get(0).place.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("lvm-place: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().lvm.get(0).plato.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().lvm.get(0).plato.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().lvm.get(0).plato.get(i).internalId+" vazba: lvm-plato "+response.get().lvm.get(0).id.value+" -> "+response.get().lvm.get(0).plato.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().lvm.get(0).plato.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("lvm-plato: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().lvm.get(0).dpu.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().lvm.get(0).dpu.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().lvm.get(0).dpu.get(i).internalId+" vazba: lvm-dpu "+response.get().lvm.get(0).id.value+" -> "+response.get().lvm.get(0).dpu.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().lvm.get(0).dpu.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("lvm-dpu: nebyly nalezeny vazby mimo limit")));
                    }
                    break;
                case "dpu":
                    emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod)));
                    if (response.get().dpu.get(0).sim.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().dpu.get(0).sim.size(); i++) {

                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().dpu.get(0).sim.get(i).internalId+" vazba: dpu-sim "+response.get().dpu.get(0).id.value+" -> "+response.get().dpu.get(0).sim.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().dpu.get(0).sim.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("dpu-sim: nebyly nalezeny vazby mimo limit")));
                    }

                    figureDLvm(response.get().dpu.get(0).lvm, response.get().dpu.get(0).lvm_modbus,emitter);
                    if (response.get().dpu.get(0).lvm.size() > 3) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().dpu.get(0).lvm.size(); i++) {
                            try {
                                CompletableFuture<GetModbusQuery.Data> modbusresponse = QueryHandler.execute(new GetModbusQuery(Collections.singletonList(response.get().dpu.get(0).lvm.get(i).edgeEndPoint.id.value)), apolloClient);
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().dpu.get(0).lvm.get(i).internalId+" lvm modbus: "+modbusresponse.get().instances.get(0).modbus.get(0).normalizedValue+" vazba: dpu-lvm ")));
                            } catch (java.lang.NullPointerException | java.lang.IndexOutOfBoundsException e) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().dpu.get(0).lvm.get(i).internalId+" vazba: dpu-lvm ")));
                            }
                            emitter.send(SseEmitter.event().data(createEventData(""+response.get().dpu.get(0).id.value+" -> "+response.get().dpu.get(0).lvm.get(i).edgeEndPoint.id.value)));
                            arr.add(response.get().dpu.get(0).lvm.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("dpu-lvm: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().dpu.get(0).plato.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().dpu.get(0).plato.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().dpu.get(0).plato.get(i).internalId+" vazba: dpu-plato "+response.get().dpu.get(0).id.value+" -> "+response.get().dpu.get(0).plato.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().dpu.get(0).plato.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("dpu-plato: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().dpu.get(0).place.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().dpu.get(0).place.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().dpu.get(0).place.get(i).internalId+" vazba: dpu-place "+response.get().dpu.get(0).id.value+" -> "+response.get().dpu.get(0).place.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().dpu.get(0).place.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("dpu-place: nebyly nalezeny vazby mimo limit")));
                    }
                    break;
                case "sim":
                    emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod)));
                    if (response.get().sim.get(0).dpu.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().sim.get(0).dpu.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().sim.get(0).dpu.get(i).internalId+" vazba: sim-dpu "+response.get().sim.get(0).id.value+" -> "+response.get().sim.get(0).dpu.get(i).edgeEndPoint.id.value+"")));
                            arr.add(response.get().sim.get(0).dpu.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("sim-dpu: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().sim.get(0).apn_wan.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().sim.get(0).apn_wan.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().sim.get(0).apn_wan.get(i).internalId+" vazba: sim-apn_wan")));
                            arr.add(response.get().sim.get(0).apn_wan.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("sim-apn_wan: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().sim.get(0).ipset_tunnel.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().sim.get(0).ipset_tunnel.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().sim.get(0).ipset_tunnel.get(i).internalId+" vazba: sim-ipset_tunnel")));
                            arr.add(response.get().sim.get(0).ipset_tunnel.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("sim-ipset_tunnel: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().sim.get(0).apn_test.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().sim.get(0).apn_test.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().sim.get(0).apn_test.get(i).internalId+" vazba: sim-apn_test")));
                            arr.add(response.get().sim.get(0).apn_test.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("sim-apn_test: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().sim.get(0).apn_prod.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().sim.get(0).apn_prod.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().sim.get(0).apn_prod.get(i).internalId+" vazba: sim-apn_prod")));
                            arr.add(response.get().sim.get(0).apn_prod.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("sim-apn_prod: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().sim.get(0).ipset_test.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().sim.get(0).ipset_test.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().sim.get(0).ipset_test.get(i).internalId+" vazba: sim-ipset_test")));
                            arr.add(response.get().sim.get(0).ipset_test.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("sim-ipset_test: nebyly nalezeny vazby mimo limit")));
                    }

                    if (response.get().sim.get(0).ipset_prod.size() > 1) {
                        ArrayList<String> arr = new ArrayList<>();
                        for (int i = 0; i < response.get().sim.get(0).ipset_prod.size(); i++) {
                            emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().sim.get(0).ipset_prod.get(i).internalId+" vazba: sim-ipset_prod")));
                            arr.add(response.get().sim.get(0).ipset_prod.get(i).internalId);
                        }
                        dupe.add(arr);
                    } else if (logset == 1) {
                        emitter.send(SseEmitter.event().data(createEventData("sim-ipset_prod: nebyly nalezeny vazby mimo limit")));
                    }
                    break;
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getDevice function exception")));
            SharedMethods.exceptionInServerLog(e,module,"getDevice", GetDeviceEdgesQuery.QUERY_DOCUMENT);
        }
    }




    private static void getPlace(String ckod, String type, ApolloClient apolloClient, int logset, SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<GetDeviceEdgesQuery.Data> response = QueryHandler.execute(new GetDeviceEdgesQuery(Collections.singletonList(ckod)), apolloClient);
            try {
                switch (type) {
                    case "dist_trafo":
                        emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod+"")));
                        if (response.get().dist_trafo.get(0).kobka.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().dist_trafo.get(0).kobka.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().dist_trafo.get(0).kobka.get(i).internalId+" vazba: dist_trafo-kobka"+"")));
                                arr.add(response.get().dist_trafo.get(0).kobka.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("dist_trafo-kobka: nebyly nalezeny vazby mimo limit")));
                        }

                        if (response.get().dist_trafo.get(0).rozvadec.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().dist_trafo.get(0).rozvadec.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().dist_trafo.get(0).rozvadec.get(i).internalId+" vazba: dist_trafo-kobka")));
                                arr.add(response.get().dist_trafo.get(0).rozvadec.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("dist_trafo-rozvadec: nebyly nalezeny vazby mimo limit")));
                        }
                        break;
                    case "rozvadec":
                        emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod+"")));
                        if (response.get().rozvadec.get(0).vyvod.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().rozvadec.get(0).vyvod.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().rozvadec.get(0).vyvod.get(i).internalId+" vazba: rozvadec-vyvod sek. strany")));
                                arr.add(response.get().rozvadec.get(0).vyvod.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("rozvadec-vyvod sek. strany: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().rozvadec.get(0).pozice_zar.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().rozvadec.get(0).pozice_zar.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().rozvadec.get(0).pozice_zar.get(i).internalId+" vazba: rozvadec-pozice_zar")));
                                arr.add(response.get().rozvadec.get(0).pozice_zar.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("rozvadec-pozice_zar: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().rozvadec.get(0).dist_trafo.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().rozvadec.get(0).dist_trafo.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().rozvadec.get(0).dist_trafo.get(i).internalId+" vazba: rozvadec-dist_trafo")));
                                arr.add(response.get().rozvadec.get(0).dist_trafo.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("rozvadec-dist_trafo: nebyly nalezeny vazby mimo limit")));
                        }
                        break;
                    case "pozice_zar":
                        emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod+"")));
                        if (response.get().pozice_zar.get(0).vyvod.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().pozice_zar.get(0).vyvod.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().pozice_zar.get(0).vyvod.get(i).internalId+" vazba: pozice_zar-vyvod sek. strany")));
                                arr.add(response.get().pozice_zar.get(0).vyvod.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("pozice_zar-vyvod: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().pozice_zar.get(0).rozvadec.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().pozice_zar.get(0).rozvadec.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().pozice_zar.get(0).rozvadec.get(i).internalId+" vazba: pozice_zar-rozvadec sek. strany")));
                                arr.add(response.get().pozice_zar.get(0).rozvadec.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("pozice_zar-rozvadec: nebyly nalezeny vazby mimo limit")));
                        }
                        break;
                    case "vyvod":
                        emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod+"")));
                        if (response.get().vyvod.get(0).rozvadec.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().vyvod.get(0).rozvadec.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().vyvod.get(0).rozvadec.get(i).internalId+" vazba: vyvod-rozvadec")));
                                arr.add(response.get().vyvod.get(0).rozvadec.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("vyvod-rozvadec: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().vyvod.get(0).pozice_zar.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().vyvod.get(0).pozice_zar.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().vyvod.get(0).pozice_zar.get(i).internalId+" vazba: vyvod-pozice_zar")));
                                arr.add(response.get().vyvod.get(0).pozice_zar.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("vyvod-pozice_zar: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().vyvod.get(0).pozice_trafa.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().vyvod.get(0).pozice_trafa.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().vyvod.get(0).pozice_trafa.get(i).internalId+" vazba: vyvod-pozice_trafa")));
                                arr.add(response.get().vyvod.get(0).pozice_trafa.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("vyvod-pozice_trafa: nebyly nalezeny vazby mimo limit")));
                        }
                        break;
                    case "kobka":
                        emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod+"")));
                        if (response.get().kobka.get(0).dist_trafo.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().kobka.get(0).dist_trafo.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().kobka.get(0).dist_trafo.get(i).internalId+" vazba: kobka-dist_trafo")));
                                arr.add(response.get().kobka.get(0).dist_trafo.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("kobka-dist_trafo: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().kobka.get(0).pozice_trafa.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().kobka.get(0).pozice_trafa.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().kobka.get(0).pozice_trafa.get(i).internalId+" vazba: kobka-pozice_trafa")));
                                arr.add(response.get().kobka.get(0).pozice_trafa.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("kobka-pozice_trafa: nebyly nalezeny vazby mimo limit")));
                        }
                        break;
                    case "pozice_trafa":
                        emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod+"")));
                        if (response.get().pozice_trafa.get(0).kobka.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().pozice_trafa.get(0).kobka.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().pozice_trafa.get(0).kobka.get(i).internalId+" vazba: pozice_trafa-kobka")));
                                arr.add(response.get().pozice_trafa.get(0).kobka.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("pozice_trafa-kobka: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().pozice_trafa.get(0).trafo.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().pozice_trafa.get(0).trafo.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().pozice_trafa.get(0).trafo.get(i).internalId+" vazba: pozice_trafa-trafo")));
                                arr.add(response.get().pozice_trafa.get(0).trafo.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("pozice_trafa-trafo: nebyly nalezeny vazby mimo limit")));
                        }
                        if (response.get().pozice_trafa.get(0).vyvod.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().pozice_trafa.get(0).vyvod.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().pozice_trafa.get(0).vyvod.get(i).internalId+" vazba: pozice_trafa-vyvod")));
                                arr.add(response.get().pozice_trafa.get(0).vyvod.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("pozice_trafa-vyvod: nebyly nalezeny vazby mimo limit")));
                        }
                        break;
                    case "trafo":
                        emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+ckod+"")));
                        if (response.get().trafo.get(0).pozice_trafa.size() > 1) {
                            ArrayList<String> arr = new ArrayList<>();
                            for (int i = 0; i < response.get().trafo.get(0).pozice_trafa.size(); i++) {
                                emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.get().trafo.get(0).pozice_trafa.get(i).internalId+" vazba: trafo-pozice_trafa")));
                                arr.add(response.get().trafo.get(0).pozice_trafa.get(i).internalId);
                            }
                            dupe.add(arr);
                        } else if (logset == 1) {
                            emitter.send(SseEmitter.event().data(createEventData("trafo-pozice_trafa: nebyly nalezeny vazby mimo limit")));
                        }
                        break;
                }
            } catch (java.lang.NullPointerException e) {

            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getPlace function exception, getdevicequery exception")));
            SharedMethods.exceptionInServerLog(e,module,"getPlace", GetDeviceEdgesQuery.QUERY_DOCUMENT);
        }
    }

    private static void getConnectedDeviceDuplicates(String ckod, int logset, ApolloClient apolloClient, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ArrayList<String> ckodlist=new ArrayList<>();
        ArrayList<String> ckodtypes=new ArrayList<>();
        ArrayList<String> plato=new ArrayList<>();
        ArrayList<String> dpu=new ArrayList<>();
        ArrayList<String> lvm=new ArrayList<>();
        ArrayList<String> sim=new ArrayList<>();
        ArrayList<ArrayList<String>> list = new ArrayList<>();

        ckodlist.add(ckod);
        listDev(ckod, apolloClient, ckodlist, 0,emitter);
        for(int i=0;i<ckodlist.size();i++){
            ckodtypes.add(identify(ckodlist.get(i),apolloClient, emitter));
        }
        for(int i=0;i<ckodtypes.size();i++) {
            switch(ckodtypes.get(i)){
                case "plato":
                    plato.add(ckodlist.get(i));
                    break;
                case "dpu":
                    dpu.add(ckodlist.get(i));
                    break;
                case "lvm":
                    lvm.add(ckodlist.get(i));
                    break;
                case "sim":
                    sim.add(ckodlist.get(i));
                    break;
            }
        }
        list.add(plato);
        list.add(dpu);
        list.add(lvm);
        list.add(sim);

        for(int i=0;i<list.size();i++){
            if(list.get(i).size()!=0){
                switch (i) {
                    case 0:
                        try {
                            //todo revert if needed, if not check correct functionality since getallplatosquery got modified to parse query
//                            CompletableFuture<GetAllPlatosQuery.Data> responsep = QueryHandler.execute(new GetAllPlatosQuery(), apolloClient);
                            CompletableFuture<GetAllPlatosQuery.Data> responsep = QueryHandler.execute(new GetAllPlatosQuery(999999999,0), apolloClient);
                            for (int j = 0; j < list.get(i).size(); j++) {
                                for (int k = 0; k < responsep.get().plato.size(); k++) {
                                    if (responsep.get().plato.get(k).id.value.equals(list.get(i).get(j))) {
                                        DuplicatesInventoryDynamic.getDevicePlato("plato", logset, responsep.get().plato.get(k),emitter);
                                    }
                                }
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getConnectedDeviceDuplicates function exception - get all platos query failed")));
                            SharedMethods.exceptionInServerLog(e,module,"getConnectedDeviceDuplicates", GetAllPlatosQuery.QUERY_DOCUMENT);
                        }
                        break;
                    case 1:
                        try {
                            //todo revert if needed like plato
//                            CompletableFuture<GetAllDpusQuery.Data> responsed = QueryHandler.execute(new GetAllDpusQuery(), apolloClient);
                            CompletableFuture<GetAllDpusQuery.Data> responsed = QueryHandler.execute(new GetAllDpusQuery(999999999,0), apolloClient);
                            for (int j = 0; j < list.get(i).size(); j++) {
                                for (int k = 0; k < responsed.get().dpu.size(); k++) {
                                    if (responsed.get().dpu.get(k).id.value.equals(list.get(i).get(j))) {
                                        DuplicatesInventoryDynamic.getDeviceDpu("dpu", apolloClient, logset, responsed.get().dpu.get(k),emitter);
                                    }
                                }
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getConnectedDeviceDuplicates function exception - get all dpus query failed")));
                            SharedMethods.exceptionInServerLog(e,module,"getConnectedDeviceDuplicates", GetAllDpusQuery.QUERY_DOCUMENT);
                        }
                        break;
                    case 2:
                        try {
//                            CompletableFuture<GetAllLvmsQuery.Data> responsel = QueryHandler.execute(new GetAllLvmsQuery(), apolloClient);
                            //todo revert if neede same as plato
                            CompletableFuture<GetAllLvmsQuery.Data> responsel = QueryHandler.execute(new GetAllLvmsQuery(999999999,0), apolloClient);
                            for (int j = 0; j < list.get(i).size(); j++) {
                                for (int k = 0; k < responsel.get().lvm.size(); k++) {
                                    if (responsel.get().lvm.get(k).id.value.equals(list.get(i).get(j))) {
                                        DuplicatesInventoryDynamic.getDeviceLvm("lvm", logset, responsel.get().lvm.get(k),emitter);
                                    }
                                }
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getConnectedDeviceDuplicates function exception - get all lvms query failed")));
                            SharedMethods.exceptionInServerLog(e,module,"getConnectedDeviceDuplicates", GetAllLvmsQuery.QUERY_DOCUMENT);
                        }
                        break;
                    case 3:
                        try {
                            //todo revert if neede same as plato
                            CompletableFuture<GetAllSimsQuery.Data> responses = QueryHandler.execute(new GetAllSimsQuery(999999999,0), apolloClient);
//                            CompletableFuture<GetAllSimsQuery.Data> responses = QueryHandler.execute(new GetAllSimsQuery(), apolloClient);
                            for (int j = 0; j < list.get(i).size(); j++) {
                                for (int k = 0; k < responses.get().sim.size(); k++) {
                                    if (responses.get().sim.get(k).id.value.equals(list.get(i).get(j))) {
                                        DuplicatesInventoryDynamic.getDeviceSim("sim", logset, responses.get().sim.get(k),emitter);
                                    }
                                }
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getConnectedDeviceDuplicates function exception - get all sims query failed")));
                            SharedMethods.exceptionInServerLog(e,module,"getConnectedDeviceDuplicates", GetAllSimsQuery.QUERY_DOCUMENT);
                        }
                        break;
                }
            }
        }
    }

    private static void getInventoryDeviceDuplicates(String type, int logset, ApolloClient apolloClient, int size, int offset, SseEmitter emitter) throws IOException {
        switch(type){
            case "plato":
                try {
                    CompletableFuture<GetAllPlatosQuery.Data> responsep = QueryHandler.execute(new GetAllPlatosQuery(1,0), apolloClient);
                    int count = responsep.get().instancesSet.count;
                    int limit = count / offset;
                    if (count % offset != 0) {
                        limit = limit + 1;
                    }
                    emitter.send(SseEmitter.event().data(createEventData("Plat na inv: "+count)));
                    for (int p = 0; p < limit; p++) {
                        if(p==limit-1){
                            size = count-((limit-1)*size);
                        }
                        try {
                            CompletableFuture<GetAllPlatosQuery.Data> response = QueryHandler.execute(new GetAllPlatosQuery(size, offset * p), apolloClient);
                            while (!response.isDone()) {
                                Thread.sleep(100);
                            }
//                            for (int j = 0; j < response.get().plato.size(); j++) {
                            for (int j = 0; j < size; j++) {
                                DuplicatesInventoryDynamic.getDevicePlato(type, logset, response.get().plato.get(j),emitter);
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - getAllPlatos query failed run: "+p+" / "+limit)));
                            SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllPlatosQuery.QUERY_DOCUMENT);
                        }
                    }
                }catch (Exception e){
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                    emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - get all platos query failed")));
                    SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllPlatosQuery.QUERY_DOCUMENT);
                }
                break;
            case "dpu":
                try {
                    CompletableFuture<GetAllDpusQuery.Data> responsed = QueryHandler.execute(new GetAllDpusQuery(1,0), apolloClient);
                    int count = responsed.get().instancesSet.count;
                    int limit = count / offset;
                    if (count % offset != 0) {
                        limit = limit + 1;
                    }
                    emitter.send(SseEmitter.event().data(createEventData("Dpu na inv: "+count)));
                    for (int p = 0; p < limit; p++) {
                        if(p==limit-1){
                            size = count-((limit-1)*size);
                        }
                        try {
                            CompletableFuture<GetAllDpusQuery.Data> response = QueryHandler.execute(new GetAllDpusQuery(size, offset * p), apolloClient);
                            while (!response.isDone()) {
                                Thread.sleep(100);
                            }
//                            for (int j = 0; j < response.get().dpu.size(); j++) {
                            for (int j = 0; j < size; j++) {
                                DuplicatesInventoryDynamic.getDeviceDpu(type, apolloClient, logset, response.get().dpu.get(j),emitter);
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - getAllDpus query failed run: "+p+" / "+limit)));
                            SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllDpusQuery.QUERY_DOCUMENT);
                        }
                    }
                }catch (Exception e){
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                    emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - get all dpus query failed")));
                    SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllDpusQuery.QUERY_DOCUMENT);
                }
                break;
            case "lvm":
                try {
                    CompletableFuture<GetAllLvmsQuery.Data> responsel = QueryHandler.execute(new GetAllLvmsQuery(1,0), apolloClient);
                    int count = responsel.get().instancesSet.count;
                    int limit = count / offset;
                    if (count % offset != 0) {
                        limit = limit + 1;
                    }
                    emitter.send(SseEmitter.event().data(createEventData("Lvm na inv: "+count)));
                    for (int p = 0; p < limit; p++) {
                        if(p==limit-1){
                            size = count-((limit-1)*size);
                        }
                        try {
                            CompletableFuture<GetAllLvmsQuery.Data> response = QueryHandler.execute(new GetAllLvmsQuery(size, offset * p), apolloClient);
                            while (!response.isDone()) {
                                Thread.sleep(100);
                            }
//                            for (int j = 0; j < response.get().lvm.size(); j++) {
                            for (int j = 0; j < size; j++) {
                                DuplicatesInventoryDynamic.getDeviceLvm(type, logset, response.get().lvm.get(j),emitter);
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - getAllLvms query failed run: "+p+" / "+limit)));
                            SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllLvmsQuery.QUERY_DOCUMENT);
                        }
                    }
                }catch (Exception e){
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                    emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - get all lvms query failed")));
                    SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllLvmsQuery.QUERY_DOCUMENT);
                }
                break;
            case "sim":
                try {
                    CompletableFuture<GetAllSimsQuery.Data> responses = QueryHandler.execute(new GetAllSimsQuery(1,0), apolloClient);
                    int count = responses.get().instancesSet.count;
                    int limit = count / offset;
                    if (count % offset != 0) {
                        limit = limit + 1;
                    }
                    emitter.send(SseEmitter.event().data(createEventData("Sim na inv: "+count)));
                    for (int p = 0; p < limit; p++) {
                        if(p==limit-1){
                            size = count-((limit-1)*size);
                        }
                        try {
                            CompletableFuture<GetAllSimsQuery.Data> response = QueryHandler.execute(new GetAllSimsQuery(size, offset * p), apolloClient);
                            while (!response.isDone()) {
                                Thread.sleep(100);
                            }
//                            for (int j = 0; j < response.get().sim.size(); j++) {
                            for (int j = 0; j < size; j++) {
                                DuplicatesInventoryDynamic.getDeviceSim(type, logset, response.get().sim.get(j),emitter);
                            }
                        }catch (Exception e){
                            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                            emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - getAllSims query failed run: "+p+" / "+limit)));
                            SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllSimsQuery.QUERY_DOCUMENT);

                        }
                    }
                }catch (Exception e){
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
                    emitter.send(SseEmitter.event().data(createEventData("Duplicates - getInventoryDeviceDuplicates function exception - get all sim query failed")));
                    SharedMethods.exceptionInServerLog(e,module,"getInventoryDeviceDuplicates", GetAllSimsQuery.QUERY_DOCUMENT);
                }
                break;
        }
    }

    //gets list of all connected devices recursively
    private static void listDev(String ckod, ApolloClient apolloClient, ArrayList<String> ckodlist,int startindex,SseEmitter emitter) throws IOException {
        try {
            CompletableFuture<GetConnectedCkodsQuery.Data> response = QueryHandler.execute(new GetConnectedCkodsQuery(Collections.singletonList(ckod)), apolloClient);
            try {
                for (int i = 0; i < response.get().devices.get(0).relInstance.size(); i++) {
                    if (!ckodlist.contains(Objects.requireNonNull(response.get().devices.get(0).relInstance.get(i).id).value) && !response.get().devices.get(0).relInstance.get(i).element.coreElement.id.equals("place")) {
                        ckodlist.add(Objects.requireNonNull(response.get().devices.get(0).relInstance.get(i).id).value);
                    }
                }
                startindex++;
                listDev(ckodlist.get(startindex), apolloClient, ckodlist, startindex,emitter);
            } catch (java.lang.IndexOutOfBoundsException e) {
                try {
                    for (int i = 0; i < response.get().sim.get(0).relInstance.size(); i++) {
                        if (!ckodlist.contains(Objects.requireNonNull(response.get().sim.get(0).relInstance.get(i).id).value)
                                && !response.get().sim.get(0).relInstance.get(i).element.coreElement.id.equals("place")
                                && !response.get().sim.get(0).relInstance.get(i).element.coreElement.id.equals("ipset")
                                && !response.get().sim.get(0).relInstance.get(i).element.coreElement.id.equals("apn")) {
                            ckodlist.add(Objects.requireNonNull(response.get().sim.get(0).relInstance.get(i).id).value);
                        }
                    }
                    startindex++;
                    listDev(ckodlist.get(startindex), apolloClient, ckodlist, startindex,emitter);
                } catch (java.lang.IndexOutOfBoundsException s) {
                }
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("Duplicates - listDev function exception, getconnectedckods query failed")));
            SharedMethods.exceptionInServerLog(e,module,"listDev", GetConnectedCkodsQuery.QUERY_DOCUMENT);
        }
    }

    private static void listPlace(String ckod, ApolloClient apolloClient, ArrayList<String> ckodlist, int startindex, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        try {
            CompletableFuture<GetConnectedCkodsQuery.Data> response = QueryHandler.execute(new GetConnectedCkodsQuery(Collections.singletonList(ckod)), apolloClient);
            try {
                for (int i = 0; i < response.get().place.get(0).relInstance.size(); i++) {
                    if (!ckodlist.contains(response.get().place.get(0).relInstance.get(i).id.value) && response.get().place.get(0).relInstance.get(i).element.coreElement.id.equals("place")) {
                        ckodlist.add(response.get().place.get(0).relInstance.get(i).id.value);
                    }
                }
                startindex++;
                listPlace(ckodlist.get(startindex), apolloClient, ckodlist, startindex, emitter);
            } catch (IndexOutOfBoundsException | InterruptedException | ExecutionException e) {
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("Duplicates - listPlace function exception - getconnectedckods query failed")));
            SharedMethods.exceptionInServerLog(e,module,"listPlace", GetConnectedCkodsQuery.QUERY_DOCUMENT);
        }
    }

    //deleting individual edges
    public static void delIndividual(String ckod, String inventory, String url,SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
        emitter.send(SseEmitter.event().data(createEventData("")));
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        ArrayList<String> del = new ArrayList<>();
        del.add(ckod);
        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(del,"vazba",inventory,apolloClient,true,true)))));
    }

    public static void delAll(String inventory, String url, int delset,SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
        emitter.send(SseEmitter.event().data(createEventData("")));
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        ArrayList<String> temp;
        switch(delset){
            case 0:
                if(dupe.size()==0){
                    emitter.send(SseEmitter.event().data(createEventData("Nebyly nalezeny duplicitní vazby")));
                }else{
                    for(int i=0;i<dupe.size();i++) {
                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(dupe.get(i), "vazby", inventory, apolloClient, true,false)))));
                    }
                }
                dupe.clear();
                break;
            case 1://nechat nejnovejsi (posledni prvek)
                for(int i=0;i<dupe.size();i++) {
                    temp=dupe.get(i);
                    temp.remove(temp.size()-1);
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(temp, "vazby", inventory, apolloClient, true,false)))));
                    temp.clear();
                }
                dupe.clear();
                break;
            case 2://nechat nejstarsi (prvni prvek)
                for(int i=0;i<dupe.size();i++) {
                    temp=dupe.get(i);
                    temp.remove(temp.get(0));
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(temp, "vazby", inventory, apolloClient, true,false)))));
                    temp.clear();
                }
                dupe.clear();
                break;
        }
//        return log;
    }

    //modbus changer, contains ckod validity checks
    public static void changeModbus(int modbusField,String idField,  String inventory, String url,SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        emitter.send(SseEmitter.event().data(createEventData("")));
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        if(!SharedMethods.validateCkod(idField,"lvm","lvm",apolloClient).equals("ok")){
            emitter.send(SseEmitter.event().data(createEventData("Zadaný ckod není ckod lvm")));
            return;
        }
        CompletableFuture<CheckEdgesExistQuery.Data> respons = QueryHandler.execute(new CheckEdgesExistQuery(Collections.singletonList(idField)), apolloClient);
        try {
            if (modbusField == (int) modbusField) {
                String update = "UPDATE_INSTANCE:{\"id\":\"" + respons.get().kind.get(0).internalId + "\",\"information:attribute.modbus_pozice\":" + modbusField + "}";
                String rep = String.valueOf(Request.Post(inventory).bodyString(update, ContentType.DEFAULT_TEXT).execute().returnResponse().getStatusLine().getStatusCode());

                if (rep.equals("200")) {
                    emitter.send(SseEmitter.event().data(createEventData("modbus pro: "+idField+" změněn na: "+modbusField)));
                }
            }
        }catch (java.lang.IndexOutOfBoundsException e){
            //exit
        }
    }
    //takes ckod as an argument and prints existing edges and their IDs
    public static void figureEdges(String edgefigure, String url, SseEmitter emitter) throws IOException, ExecutionException, InterruptedException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        String type = identify(edgefigure,apolloClient,emitter);
        if(!SharedMethods.validateCkod(edgefigure,type,type,apolloClient).equals("ok")){
            emitter.send(SseEmitter.event().data(createEventData("Zadaný ckod není ckod "+type)));
            return;
        }
        try {
            CompletableFuture<GetEdgesInternalIDSQuery.Data> response = QueryHandler.execute(new GetEdgesInternalIDSQuery(Collections.singletonList(edgefigure)), apolloClient);
            for(int i=0;i<response.get().instances.get(0).plato.size();i++){
                emitter.send(SseEmitter.event().data(createEventData("Plato: "+response.get().instances.get(0).plato.get(i).id.value)));
                emitter.send(SseEmitter.event().data(createEventData("internalID: "+response.get().instances.get(0).platoEdges.get(i).internalId)));
            }
            for(int i=0;i<response.get().instances.get(0).lvm.size();i++){
                emitter.send(SseEmitter.event().data(createEventData("LVM: "+response.get().instances.get(0).lvm.get(i).id.value)));
                emitter.send(SseEmitter.event().data(createEventData("internalID: "+response.get().instances.get(0).lvmEdges.get(i).internalId)));
            }
            for(int i=0;i<response.get().instances.get(0).dpu.size();i++){
                emitter.send(SseEmitter.event().data(createEventData("DPU: "+response.get().instances.get(0).dpu.get(i).id.value)));
                emitter.send(SseEmitter.event().data(createEventData("internalID: "+response.get().instances.get(0).dpuEdges.get(i).internalId)));
            }
            for(int i=0;i<response.get().instances.get(0).sim.size();i++){
                emitter.send(SseEmitter.event().data(createEventData("SIM: "+response.get().instances.get(0).sim.get(i).id.value)));
                emitter.send(SseEmitter.event().data(createEventData("internalID: "+response.get().instances.get(0).simEdges.get(i).internalId)));
            }
            for(int i=0;i<response.get().instances.get(0).place.size();i++){
                emitter.send(SseEmitter.event().data(createEventData("Place: "+response.get().instances.get(0).place.get(i).id.value)));
                emitter.send(SseEmitter.event().data(createEventData("internalID: "+response.get().instances.get(0).placeEdges.get(i).internalId)));
            }
        }catch (Exception e){
            emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));
            emitter.send(SseEmitter.event().data(createEventData("Duplicates - figureEdges function exception - GetEdgesInternalIDS query failed")));
            SharedMethods.exceptionInServerLog(e,module,"figureEdges", GetEdgesInternalIDSQuery.QUERY_DOCUMENT);
        }


    }

    public static void main(String ckod, String url, int mode, int logset, int sizeField, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();

        if(sizeField==0){
            sizeField=999999999;
        } else if (sizeField<0) {
            emitter.send(SseEmitter.event().data(createEventData("Query size nemuze mit zapornou velikost")));
            return;
        }
        int size = sizeField, offset = size;

        String type = identify(ckod,apolloClient, emitter);
        if(type==null){
            return;
        }
        ArrayList<String> ckodlist=new ArrayList<>();

        switch(mode){
            case 0: //get dupes of ckod
                if(type.equals("plato") || type.equals("lvm") || type.equals("dpu") || type.equals("sim")){
                    getDevice(ckod, type, apolloClient, logset,emitter);
                }else {
                    getPlace(ckod,type,apolloClient,logset,emitter);
                }
                break;
            case 1: //get dupes of ckod + neighbours recursive
                getConnectedDeviceDuplicates(ckod,logset,apolloClient,emitter);
//                    getConnectedDeviceDuplicatesSafe(ckod,type,logset,apolloClient);
                break;
            case 2: //get dupes of ckod + neighbours recursive - places not devices
                ckodlist.clear();
                ckodlist.add(ckod);
                listPlace(ckod,apolloClient,ckodlist,0,emitter);
                for(int i=0;i<ckodlist.size();i++){
                    type=identify(ckodlist.get(i),apolloClient,emitter);
                    if(type==null){
                        emitter.send(SseEmitter.event().data(createEventData("identify == null")));
                        return;
                    }
                    getPlace(ckodlist.get(i),type,apolloClient,logset,emitter);
                }
                break;
            case 3: //get dupes of this kind on whole inv
                getInventoryDeviceDuplicates(type,logset,apolloClient,size,offset,emitter);
                break;

        }



    }
}
