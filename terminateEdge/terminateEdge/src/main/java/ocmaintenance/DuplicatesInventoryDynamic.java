package ocmaintenance;

import com.apollographql.apollo.ApolloClient;
import ocmaintenance.controllers.SharedMethods;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static ocmaintenance.controllers.SharedMethods.createEventData;

//this class makes the Duplicate Edges mode 3 work, ensures Duplicates is clean
public class DuplicatesInventoryDynamic {
    private final static String module = "DuplicatesInventory";
    private static void figureDLvm(List<GetAllDpusQuery.Lvm> lvmlist, List<GetAllDpusQuery.Lvm_modbus> lvm_modbus, SseEmitter emitter) throws IOException {
        for(int i=0;i<lvm_modbus.size();i++) {
            if(Collections.frequency(lvm_modbus, lvm_modbus.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("modbus: "+lvm_modbus.get(i).modbus.get(0).normalizedValue+" je duplicitně přiřazen na: "+Objects.requireNonNull(lvmlist.get(i).edgeEndPoint.id).value)));
            }
        }
        for(int i=0;i<lvmlist.size();i++){
            if(Collections.frequency(lvmlist, lvmlist.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("lvm: "+lvmlist.get(i).internalId+" je navazbeno více než 1x")));
            }
        }
    }
    private static void figurePLvm(List<GetAllPlatosQuery.Lvm> lvmlist, List<GetAllPlatosQuery.Lvm_modbus> lvm_modbus,SseEmitter emitter) throws IOException {
        for(int i=0;i<lvm_modbus.size();i++) {
            if(Collections.frequency(lvm_modbus, lvm_modbus.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("modbus: "+lvm_modbus.get(i).modbus.get(0).normalizedValue+" je duplicitně přiřazen na lvm: "+Objects.requireNonNull(lvmlist.get(i).edgeEndPoint.id).value)));
            }
        }
        for(int i=0;i<lvmlist.size();i++){
            if(Collections.frequency(lvmlist, lvmlist.get(i))>1){
                emitter.send(SseEmitter.event().data(createEventData("lvm: "+lvmlist.get(i).internalId+" je navazbeno více než 1x")));
            }
        }
    }

    public static void getDevicePlato(String type, int logset, GetAllPlatosQuery.Plato response, SseEmitter emitter) throws IOException {
        try {
            emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+response.id.value)));
            figurePLvm(response.lvm, response.lvm_modbus,emitter);
            if (response.lvm.size() > 3) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.lvm.size(); i++) {
                    assert response.lvm.get(i).edgeEndPoint.id != null;
                    assert response.lvm.get(i).edgeEndPoint.id != null;
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.lvm.get(i).internalId+" lvm modbus: "+response.lvm_modbus.get(i).modbus.get(0).normalizedValue+" vazba: plato-lvm ")));
                    emitter.send(SseEmitter.event().data(createEventData(response.id.value+" -> "+response.lvm.get(i).edgeEndPoint.id.value)));
                    arr.add(response.lvm.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);
            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("plato-lvm: nebyly nalezeny vazby mimo limit")));
            }

            if (response.place.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.place.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.place.get(i).internalId+" vazba: plato-place "+response.id.value+" -> "+response.place.get(i).edgeEndPoint.id.value)));
                    arr.add(response.place.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("plato-place: nebyly nalezeny vazby mimo limit")));
            }
            if (response.dpu.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.dpu.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.dpu.get(i).internalId+" vazba: plato-dpu "+response.id.value+" -> "+response.dpu.get(i).edgeEndPoint.id.value)));
                    arr.add(response.dpu.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("plato-dpu: nebyly nalezeny vazby mimo limit")));
            }
        }catch (Exception e){
                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));

            emitter.send(SseEmitter.event().data(createEventData("DuplicatesInvScale - getDevicePlato function exception")));
            SharedMethods.exceptionInServerLog(e,module,"getDevicePlato", "function failed "+e);
        }
    }

    public static void getDeviceDpu(String type, ApolloClient apolloClient, int logset, GetAllDpusQuery.Dpu response,SseEmitter emitter) throws IOException {
        try {
            emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+response.id.value)));
            if (response.sim.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.sim.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.sim.get(i).internalId+" vazba: dpu-sim "+response.id.value+" -> "+response.sim.get(i).edgeEndPoint.id.value)));
                    arr.add(response.sim.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("dpu-sim: nebyly nalezeny vazby mimo limit")));
            }
            figureDLvm(response.lvm, response.lvm_modbus,emitter);
            if (response.lvm.size() > 3) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.lvm.size(); i++) {
                    try {
                        CompletableFuture<GetModbusQuery.Data> modbusresponse = QueryHandler.execute(new GetModbusQuery(Collections.singletonList(response.lvm.get(i).edgeEndPoint.id.value)), apolloClient);
                        emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.lvm.get(i).internalId+" lvm modbus: "+modbusresponse.get().instances.get(0).modbus.get(0).normalizedValue+" vazba: dpu-lvm ")));
                    } catch (java.lang.NullPointerException | java.lang.IndexOutOfBoundsException e) {
                        emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.lvm.get(i).internalId+" vazba: dpu-lvm ")));
                    }
                    emitter.send(SseEmitter.event().data(createEventData(""+response.id.value+" -> "+response.lvm.get(i).edgeEndPoint.id.value)));
                    arr.add(response.lvm.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);
            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("dpu-lvm: nebyly nalezeny vazby mimo limit")));
            }

            if (response.plato.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.plato.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.plato.get(i).internalId+" vazba: dpu-plato "+response.id.value+" -> "+response.plato.get(i).edgeEndPoint.id.value)));
                    arr.add(response.plato.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("dpu-plato: nebyly nalezeny vazby mimo limit")));
            }
            if (response.place.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.place.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.place.get(i).internalId+" vazba: dpu-place "+response.id.value+" -> "+response.place.get(i).edgeEndPoint.id.value)));
                    arr.add(response.place.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("dpu-place: nebyly nalezeny vazby mimo limit")));
            }
        }catch (Exception e){
                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));

            emitter.send(SseEmitter.event().data(createEventData("DuplicatesInvScale - getDeviceDpu function exception")));
            SharedMethods.exceptionInServerLog(e,module,"getDeviceDpu", "function exception "+e);
        }
    }

    public static void getDeviceLvm(String type, int logset, GetAllLvmsQuery.Lvm response, SseEmitter emitter) throws IOException {
        try {
            emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+response.id.value)));
            if (response.place.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.place.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.place.get(i).internalId+" vazba: lvm-place "+response.id.value+" -> "+response.place.get(i).edgeEndPoint.id.value)));
                    arr.add(response.place.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("lvm-place: nebyly nalezeny vazby mimo limit")));
            }
            if (response.plato.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.plato.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.plato.get(i).internalId+" vazba: lvm-plato "+response.id.value+" -> "+response.plato.get(i).edgeEndPoint.id.value)));
                    arr.add(response.plato.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("lvm-plato: nebyly nalezeny vazby mimo limit")));
            }
            if (response.dpu.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.dpu.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.dpu.get(i).internalId+" vazba: lvm-dpu "+response.id.value+" -> "+response.dpu.get(i).edgeEndPoint.id.value)));
                    arr.add(response.dpu.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("lvm-dpu: nebyly nalezeny vazby mimo limit")));
            }
        }catch (Exception e){
                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));

            emitter.send(SseEmitter.event().data(createEventData("DuplicatesInvScale - getDeviceLvm function exception")));
            SharedMethods.exceptionInServerLog(e,module,"getDeviceLvm", "function exception"+e);
        }
    }

    public static void getDeviceSim(String type, int logset, GetAllSimsQuery.Sim response, SseEmitter emitter) throws IOException {
        try {
            emitter.send(SseEmitter.event().data(createEventData("kontrola: "+type+" "+response.id.value)));
            if (response.dpu.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.dpu.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.dpu.get(i).internalId+" vazba: sim-dpu "+response.id.value+" -> "+response.dpu.get(i).edgeEndPoint.id.value)));
                    arr.add(response.dpu.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("sim-dpu: nebyly nalezeny vazby mimo limit")));
            }


            if (response.apn_wan.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.apn_wan.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.apn_wan.get(i).internalId+" vazba: sim-apn_wan")));
                    arr.add(response.apn_wan.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("sim-apn_wan: nebyly nalezeny vazby mimo limit")));
            }

            if (response.ipset_tunnel.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.ipset_tunnel.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.ipset_tunnel.get(i).internalId+" vazba: sim-ipset_tunnel")));
                    arr.add(response.ipset_tunnel.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("sim-ipset_tunnel: nebyly nalezeny vazby mimo limit")));
            }

            if (response.apn_test.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.apn_test.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.apn_test.get(i).internalId+" vazba: sim-apn_test")));
                    arr.add(response.apn_test.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("sim-apn_test: nebyly nalezeny vazby mimo limit")));
            }

            if (response.apn_prod.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.apn_prod.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.apn_prod.get(i).internalId+" vazba: sim-apn_prod")));
                    arr.add(response.apn_prod.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("sim-apn_prod: nebyly nalezeny vazby mimo limit")));
            }

            if (response.ipset_test.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.ipset_test.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.ipset_test.get(i).internalId+" vazba: sim-ipset_test")));
                    arr.add(response.ipset_test.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("sim-ipset_test: nebyly nalezeny vazby mimo limit")));
            }

            if (response.ipset_prod.size() > 1) {
                ArrayList<String> arr = new ArrayList<>();
                for (int i = 0; i < response.ipset_prod.size(); i++) {
                    emitter.send(SseEmitter.event().data(createEventData("vazby mimo limit: "+response.ipset_prod.get(i).internalId+" vazba: sim-ipset_prod")));
                    arr.add(response.ipset_prod.get(i).internalId);
                }
                DuplicatesDynamic.dupe.add(arr);

            } else if (logset == 1) {
                emitter.send(SseEmitter.event().data(createEventData("sim-ipset_prod: nebyly nalezeny vazby mimo limit")));
            }

        }catch (Exception e){
                        emitter.send(SseEmitter.event().data(createEventData(String.valueOf(e),"error")));

            emitter.send(SseEmitter.event().data(createEventData("DuplicatesInvScale - getDeviceSim function exception")));
            SharedMethods.exceptionInServerLog(e,module,"getDeviceSim", "function exception "+e);
        }
    }




}
