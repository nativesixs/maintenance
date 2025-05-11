package ocmaintenance;

import static ocmaintenance.controllers.SharedMethods.createEventData;
import static ocmaintenance.controllers.SharedMethods.createException;

import com.apollographql.apollo.ApolloClient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import ocmaintenance.controllers.SharedMethods;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class TerminateDynamic {
    private static String url = null;
    private static String ckod = null;
    private static String inventory = null;
    private static int mode = 0;
    private static boolean safe;
    private static ArrayList<String> lvmList = new ArrayList<>();
    private static ArrayList<String> dpuList = new ArrayList<>();
    private static ArrayList<String> ahsList = new ArrayList<>();
    private static ArrayList<String> zdrojList = new ArrayList<>();
    private static ArrayList<String> placeIdList = new ArrayList<>();
    private static final String module = "TerminateEdges";


    public static boolean handleInput(String ckodField, String inventoryConf, int modeField, String urlConf, boolean safeIn, ApolloClient apolloClient, SseEmitter emitter) throws ExecutionException, InterruptedException, IOException {
        if (!SharedMethods.validateCkod(ckodField, "plato", "plato", apolloClient).equals("ok")) {
            emitter.send(SseEmitter.event().data(createEventData("Zadaný ckod není ckod plata", "warning")));
            return false;
        } else {
            ckod = ckodField;
        }
        if (inventoryConf.equals("none")) {
            emitter.send(SseEmitter.event().data(createEventData("Error: V konfiguračním souboru nebylo specifikováno inventory.", "error")));
            return false;
        } else {
            inventory = inventoryConf;
        }
        mode = modeField;
        safe = safeIn;
        return true;
    }

    public static void main(String ckodField, int modeField, String inventoryConf, String urlConf, boolean safe, SseEmitter emitter) throws IOException, InterruptedException, ExecutionException {
        //DEFAULT mode 0 = odvazbit vse
        //mode 1 = odvazbit zarizeni od sebe - nechat vazby na umisteni
        //mode 2 = odvazbit jen umisteni zarizeni, zarizeni samotna nechat navazbene na sebe
        //mode 3 = neodvazbi, jen vypise vazby
        placeIdList = new ArrayList<>();
        lvmList = new ArrayList<>();
        dpuList = new ArrayList<>();
        ahsList = new ArrayList<>();
        zdrojList = new ArrayList<>();
        url = urlConf;
        ApolloClient apolloClient = ApolloClient.builder().serverUrl(url).build();
        if (!handleInput(ckodField, inventoryConf, modeField, urlConf, safe, apolloClient, emitter)) {
            return;
        }

        switch (mode) {
            case 0: //vypsat
                terminateAll(ckod, apolloClient, emitter, false, false, false, false);
                return;
            case 1: //odvazbit umisteni
                terminateAll(ckod, apolloClient, emitter, false, false, true, false);
                break;
            case 2: //odvazbit zarizeni
                terminateAll(ckod, apolloClient, emitter, false, true, false, false);
                break;
            case 3: //odvazbit vse
                terminateAll(ckod, apolloClient, emitter, true, false, false, false);
                break;
            case 4: //vypsat terminate requesty
                terminateAll(ckod, apolloClient, emitter, false, false, false, true);
                break;
        }
    }


    private static void terminateAll(String ckod, ApolloClient apolloClient, SseEmitter emitter, boolean terminateAll, boolean terminateDevices, boolean terminatePlaces, boolean printRequests) throws IOException {
        try {
            CompletableFuture<GetPlatoUninstallTestQuery.Data> response = QueryHandler.execute(new GetPlatoUninstallTestQuery(Collections.singletonList(ckod)), apolloClient);
            ArrayList<String> terminateList = new ArrayList<>();
            ArrayList<String> terminateListPlaces = new ArrayList<>();
            ArrayList<String> terminateListDevices = new ArrayList<>();
            ArrayList<ArrayList<String>> platoVazby = new ArrayList<>();
            ArrayList<ArrayList<String>> dtsVazby = new ArrayList<>();
            ArrayList<ArrayList<String>> lvmVazby = new ArrayList<>();
            ArrayList<ArrayList<String>> dpuVazby = new ArrayList<>();
            ArrayList<String> places = new ArrayList<>();
            ArrayList<String> devices = new ArrayList<>();
            try {

                // DTS
                for (int i = 0; i < response.get().plato.get(0).placeEdge.size(); i++) {
                    String dtsckod = Objects.requireNonNull(response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.id).value;
                    String dtsInternalId = response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.internalId;
                    String platodtsvazba = response.get().plato.get(0).placeEdge.get(i).internalId;

                    ArrayList<String> temp = new ArrayList<>();
                    temp.add("DTS");
                    temp.add(dtsckod); //ckod dts
                    temp.add(dtsInternalId); //intid dts
                    temp.add(platodtsvazba); //intid vazby
                    platoVazby.add(temp);

                    places.add("Plato(" + ckod + ")-DTS(" + dtsckod + ")");
                    places.add("Vazba: " + platodtsvazba);

                    if (terminatePlaces || printRequests || terminateAll) {
                        terminateList.add(platodtsvazba);
                        terminateListPlaces.add(platodtsvazba);
                    }

                    // DTS -> AHS edges
                    for (int j = 0; j < response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.ahsEdge.size(); j++) {
                        String ahsCkod = Objects.requireNonNull(response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.ahsEdge.get(j).edgeEndPoint.id).value;
                        String ahsInternalId = response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.ahsEdge.get(j).edgeEndPoint.internalId;
                        String dtsahsvazba = response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.ahsEdge.get(j).internalId;

                        ArrayList<String> temp2 = new ArrayList<>();
                        temp2.add("AHS");
                        temp2.add(dtsckod); //dts ckod
                        temp2.add(ahsCkod); //ahs ckod
                        temp2.add(ahsInternalId); //ahs internalid
                        temp2.add(dtsahsvazba); //vazba
                        dtsVazby.add(temp2);

                        places.add("DTS(" + dtsckod + ")-AHS(" + ahsCkod + ")");
                        places.add("Vazba: " + dtsahsvazba);

                        if (terminatePlaces || printRequests || terminateAll) {
                            terminateList.add(dtsahsvazba);
                            terminateListPlaces.add(dtsahsvazba);
                        }
                    }
                    // DTS -> Zdroj edges
                    for (int j = 0; j < response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.zdrojEdge.size(); j++) {
                        String zdrojCkod = Objects.requireNonNull(response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.zdrojEdge.get(j).edgeEndPoint.id).value;
                        String zdrojInternalId = response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.zdrojEdge.get(j).edgeEndPoint.internalId;
                        String dtszdrojvazba = response.get().plato.get(0).placeEdge.get(i).edgeEndPoint.zdrojEdge.get(j).internalId;

                        ArrayList<String> temp2 = new ArrayList<>();
                        temp2.add("ZDROJ");
                        temp2.add(dtsckod); //dts ckod
                        temp2.add(zdrojCkod); //zdroj ckod
                        temp2.add(zdrojInternalId); //zdroj internalid
                        temp2.add(dtszdrojvazba); //vazba
                        dtsVazby.add(temp2);

                        places.add("DTS(" + dtsckod + ")-ZDROJ(" + zdrojCkod + ")");
                        places.add("Vazba: " + dtszdrojvazba);


                        if (terminatePlaces || printRequests || terminateAll) {
                            terminateList.add(dtszdrojvazba);
                            terminateListPlaces.add(dtszdrojvazba);
                        }
                    }
                }

                // DPU
                for (int i = 0; i < response.get().plato.get(0).dpuEdge.size(); i++) {
                    String dpuCkod = Objects.requireNonNull(response.get().plato.get(0).dpuEdge.get(i).edgeEndPoint.id).value;
                    String dpuInternalId = response.get().plato.get(0).dpuEdge.get(i).edgeEndPoint.internalId;
                    String platodpuvazba = response.get().plato.get(0).dpuEdge.get(i).internalId;

                    ArrayList<String> temp = new ArrayList<>();
                    temp.add("DPU");
                    temp.add(dpuCkod); //ckod dpu
                    temp.add(dpuInternalId); //intid dpu
                    temp.add(platodpuvazba); //intid vazby
                    platoVazby.add(temp);

                    devices.add("Plato(" + ckod + ")-DPU(" + dpuCkod + ")");
                    devices.add("Vazba: " + platodpuvazba);

                    if (terminateDevices || printRequests || terminateAll) {
                        terminateList.add(platodpuvazba);
                        terminateListDevices.add(platodpuvazba);
                    }
                    // DPU -> DTS edges
                    for (int j = 0; j < response.get().plato.get(0).dpuEdge.get(i).edgeEndPoint.dpuPlaceEdge.size(); j++) {
                        String dtsCkod = Objects.requireNonNull(response.get().plato.get(0).dpuEdge.get(i).edgeEndPoint.dpuPlaceEdge.get(j).edgeEndPoint.id).value;
                        String dtsInternalId = response.get().plato.get(0).dpuEdge.get(i).edgeEndPoint.dpuPlaceEdge.get(j).edgeEndPoint.internalId;
                        String dpudtsvazba = response.get().plato.get(0).dpuEdge.get(i).edgeEndPoint.dpuPlaceEdge.get(j).internalId;

                        ArrayList<String> temp2 = new ArrayList<>();
                        temp2.add("DTS");
                        temp2.add(dpuCkod); //dpu ckod
                        temp2.add(dtsCkod); //dts ckod
                        temp2.add(dtsInternalId); //dts internalid
                        temp2.add(dpudtsvazba); //vazba
                        dpuVazby.add(temp2);

                        places.add("DPU(" + dpuCkod + ")-DTS(" + dtsCkod + ")");
                        places.add("Vazba: " + dpudtsvazba);

                        if (terminatePlaces || printRequests || terminateAll) {
                            terminateList.add(dpudtsvazba);
                            terminateListPlaces.add(dpudtsvazba);
                        }
                    }
                }
                // LVM
                for (int i = 0; i < response.get().plato.get(0).lvmEdge.size(); i++) {
                    String lvmCkod = Objects.requireNonNull(response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.id).value;
                    String lvmInternalId = response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.internalId;
                    String platolvmvazba = response.get().plato.get(0).lvmEdge.get(i).internalId;

                    ArrayList<String> temp = new ArrayList<>();
                    temp.add("LVM");
                    temp.add(lvmCkod); //ckod lvm
                    temp.add(lvmInternalId); //intid lvm
                    temp.add(platolvmvazba); //vazba intid
                    platoVazby.add(temp);

                    devices.add("Plato(" + ckod + ")-LVM(" + lvmCkod + ")");
                    devices.add("Vazba: " + platolvmvazba);
                    if (terminateDevices || printRequests || terminateAll) {
                        terminateList.add(platolvmvazba);
                        terminateListDevices.add(platolvmvazba);
                    }
                    // LVM -> DPU edges
                    for (int j = 0; j < response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmDpuEdge.size(); j++) {
                        String dpuCkod = Objects.requireNonNull(response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmDpuEdge.get(j).edgeEndPoint.id).value;
                        String dpuInternalId = response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmDpuEdge.get(j).edgeEndPoint.internalId;
                        String lvmdpuvazba = response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmDpuEdge.get(j).internalId;

                        ArrayList<String> temp2 = new ArrayList<>();
                        temp2.add("DPU");
                        temp2.add(lvmCkod); //lvm ckod
                        temp2.add(dpuCkod); //dpu ckod
                        temp2.add(dpuInternalId); //dpu internalid
                        temp2.add(lvmdpuvazba); //vazba
                        lvmVazby.add(temp2);

                        devices.add("LVM(" + lvmCkod + ")-DPU(" + dpuCkod + ")");
                        devices.add("Vazba: " + lvmdpuvazba);

                        if (terminateDevices || printRequests || terminateAll) {
                            terminateList.add(lvmdpuvazba);
                            terminateListDevices.add(lvmdpuvazba);
                        }
                    }
                    // LVM -> Sek.Vyvod edges
                    for (int j = 0; j < response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmSekVyvod.size(); j++) {
                        String vyvodCkod = Objects.requireNonNull(response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmSekVyvod.get(j).edgeEndPoint.id).value;
                        String vyvodInternalId = response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmSekVyvod.get(j).edgeEndPoint.internalId;
                        String lvmvyvodvazba = response.get().plato.get(0).lvmEdge.get(i).edgeEndPoint.lvmSekVyvod.get(j).internalId;

                        ArrayList<String> temp2 = new ArrayList<>();
                        temp2.add("Sek.Vyvod");
                        temp2.add(lvmCkod); //lvmckod
                        temp2.add(vyvodCkod); //vyvod ckod
                        temp2.add(vyvodInternalId); //vyvod internalid
                        temp2.add(lvmvyvodvazba); //vazba
                        lvmVazby.add(temp2);

                        places.add("LVM(" + lvmCkod + ")-Sek.Vyvod(" + vyvodCkod + ")");
                        places.add("Vazba: " + lvmvyvodvazba);

                        if (terminatePlaces || printRequests || terminateAll) {
                            terminateList.add(lvmvyvodvazba);
                            terminateListPlaces.add(lvmvyvodvazba);
                        }
                    }
                }
                // AHS
                for (int i = 0; i < response.get().plato.get(0).ahsEdge.size(); i++) {
                    String ahsCkod = Objects.requireNonNull(response.get().plato.get(0).ahsEdge.get(i).edgeEndPoint.id).value;
                    String ahsInternalId = response.get().plato.get(0).ahsEdge.get(i).edgeEndPoint.internalId;
                    String platoahsvazba = response.get().plato.get(0).ahsEdge.get(i).internalId;

                    ArrayList<String> temp = new ArrayList<>();
                    temp.add("AHS");
                    temp.add(ahsCkod); //ckod ahs
                    temp.add(ahsInternalId); //intid ahs
                    temp.add(platoahsvazba); //intid vazby
                    platoVazby.add(temp);

                    devices.add("Plato(" + ckod + ")-AHS(" + ahsCkod + ")");
                    devices.add("Vazba: " + platoahsvazba);
                    if (terminateDevices || printRequests || terminateAll) {
                        terminateList.add(platoahsvazba);
                        terminateListDevices.add(platoahsvazba);
                    }
                }
                // Zdroj
                for (int i = 0; i < response.get().plato.get(0).zdrojEdge.size(); i++) {
                    String zdrojCkod = Objects.requireNonNull(response.get().plato.get(0).zdrojEdge.get(i).edgeEndPoint.id).value;
                    String zdrojInternalId = response.get().plato.get(0).zdrojEdge.get(i).edgeEndPoint.internalId;
                    String platozdrojvazba = response.get().plato.get(0).zdrojEdge.get(i).internalId;

                    ArrayList<String> temp = new ArrayList<>();
                    temp.add("ZDROJ");
                    temp.add(zdrojCkod); //ckod zdroj
                    temp.add(zdrojInternalId); //intid zdroj
                    temp.add(platozdrojvazba); //intid vazby
                    platoVazby.add(temp);

                    devices.add("Plato(" + ckod + ")-ZDROJ(" + zdrojCkod + ")");
                    devices.add("Vazba: " + platozdrojvazba);
                    if (terminateDevices || printRequests || terminateAll) {
                        terminateList.add(platozdrojvazba);
                        terminateListDevices.add(platozdrojvazba);
                    }
                }

                emitter.send(SseEmitter.event().data(createEventData("Nalezeny vazby pro plato: " + ckod)));
                emitter.send(SseEmitter.event().data(createEventData("Plato internalId: " + response.get().plato.get(0).internalId + "\n\n")));

                for (int i = 0; i < platoVazby.size(); i++) {
                    if (platoVazby.get(i).size() >= 4) {
                        for (int j = 1; j < platoVazby.get(i).size(); j += 3) {
                            emitter.send(SseEmitter.event().data(createEventData(platoVazby.get(i).get(0) + ": " + platoVazby.get(i).get(j))));
                            emitter.send(SseEmitter.event().data(createEventData(platoVazby.get(i).get(0) + " internalId: " + platoVazby.get(i).get(j + 1))));
                            emitter.send(SseEmitter.event().data(createEventData("Vazba Plato-" + platoVazby.get(i).get(0) + ": " + platoVazby.get(i).get(j + 2) + "\n\n")));
                        }
                    }
                }

                if (dtsVazby.size() >= 1 || dpuVazby.size() >= 1 || lvmVazby.size() >= 1) {
                    emitter.send(SseEmitter.event().data(createEventData("---------------------------- \n\n")));
                }

                if (dtsVazby.size() >= 1) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazby nalezených DTS: \n\n")));
                    for (int i = 0; i < dtsVazby.size(); i++) {
                        if (dtsVazby.get(i).size() >= 5) {
                            for (int j = 1; j < dtsVazby.get(i).size(); j += 4) {
                                emitter.send(SseEmitter.event().data(createEventData(dtsVazby.get(i).get(0) + ": " + dtsVazby.get(i).get(j + 1))));
                                emitter.send(SseEmitter.event().data(createEventData(dtsVazby.get(i).get(0) + " internalId: " + dtsVazby.get(i).get(j + 2))));
                                emitter.send(SseEmitter.event().data(createEventData("Vazba DTS(" + dtsVazby.get(i).get(j) + ")-" + dtsVazby.get(i).get(0) + ": " + dtsVazby.get(i).get(j + 3) + "\n\n")));
                            }
                        }
                    }
                }

                if (dpuVazby.size() >= 1) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazby nalezených DPU: \n\n")));
                    for (int i = 0; i < dpuVazby.size(); i++) {
                        if (dpuVazby.get(i).size() >= 5) {
                            for (int j = 1; j < dpuVazby.get(i).size(); j += 4) {
                                emitter.send(SseEmitter.event().data(createEventData(dpuVazby.get(i).get(0) + ": " + dpuVazby.get(i).get(j + 1))));
                                emitter.send(SseEmitter.event().data(createEventData(dpuVazby.get(i).get(0) + " internalId: " + dpuVazby.get(i).get(j + 2))));
                                emitter.send(SseEmitter.event().data(createEventData("Vazba DPU(" + dpuVazby.get(i).get(j) + ")-" + dpuVazby.get(i).get(0) + ": " + dpuVazby.get(i).get(j + 3) + "\n\n")));
                            }
                        }
                    }
                }

                if (lvmVazby.size() >= 1) {
                    emitter.send(SseEmitter.event().data(createEventData("Vazby nalezených LVM: \n\n")));
                    for (int i = 0; i < lvmVazby.size(); i++) {
                        if (lvmVazby.get(i).size() >= 5) {
                            for (int j = 1; j < lvmVazby.get(i).size(); j += 4) {
                                emitter.send(SseEmitter.event().data(createEventData(lvmVazby.get(i).get(0) + ": " + lvmVazby.get(i).get(j + 1))));
                                emitter.send(SseEmitter.event().data(createEventData(lvmVazby.get(i).get(0) + " internalId: " + lvmVazby.get(i).get(j + 2))));
                                emitter.send(SseEmitter.event().data(createEventData("Vazba LVM(" + lvmVazby.get(i).get(j) + ")-" + lvmVazby.get(i).get(0) + ": " + lvmVazby.get(i).get(j + 3) + "\n\n")));
                            }
                        }
                    }
                }

                if (places.size() >= 1) {
                    emitter.send(SseEmitter.event().data(createEventData("---------------------------- \n\n")));
                    emitter.send(SseEmitter.event().data(createEventData("Nalezené vazby dle kategorie: Umístění\n\n")));
                    for (int i = 0; i < places.size(); i++) {
                        emitter.send(SseEmitter.event().data(createEventData(places.get(i))));
                    }
                }
                if (devices.size() >= 1) {
                    emitter.send(SseEmitter.event().data(createEventData("---------------------------- \n\n")));
                    emitter.send(SseEmitter.event().data(createEventData("Nalezené vazby dle kategorie: Zařízení\n\n")));
                    for (int i = 0; i < devices.size(); i++) {
                        emitter.send(SseEmitter.event().data(createEventData(devices.get(i))));
                    }
                }


                if (terminateAll) {
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(terminateList, "terminateAll", inventory, apolloClient, safe, true)), "warning")));
                } else if (terminateDevices) {
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(terminateList, "plato devices", inventory, apolloClient, safe, true)), "warning")));
                } else if (terminatePlaces) {
                    emitter.send(SseEmitter.event().data(createEventData(String.valueOf(SharedMethods.terminateRequest(terminateList, "plato places", inventory, apolloClient, safe, true)), "warning")));
                }

                if (printRequests && terminateList.size() >= 1) {
                    emitter.send(SseEmitter.event().data(createEventData("\n\n=====================\n\n")));
                    emitter.send(SseEmitter.event().data(createEventData("Vazby zařízení:\n\n")));
                    if (terminateListDevices.size() >= 1) {
                        for (int i = 0; i < terminateListDevices.size(); i++) {
                            String deledge = "TERMINATE_EDGE:\n{ \"id\": \"" + terminateListDevices.get(i) + "\" ,\"exists_to\": \"now\"}";
                            emitter.send(SseEmitter.event().data(createEventData(deledge + "\n\n")));
                        }
                    }
                    if (terminateListPlaces.size() >= 1) {
                        emitter.send(SseEmitter.event().data(createEventData("Vazby umístění:\n\n")));
                        for (int i = 0; i < terminateListPlaces.size(); i++) {
                            String deledge = "TERMINATE_EDGE:\n{ \"id\": \"" + terminateListPlaces.get(i) + "\" ,\"exists_to\": \"now\"}";
                            emitter.send(SseEmitter.event().data(createEventData(deledge + "\n\n")));
                        }
                    }
                }

                terminateList.clear();
            } catch (Exception e) {
                createException(e, "terminateAll", "Terminate - terminateAll function exception, line: ", module, emitter);
            }
        } catch (Exception e) {
            createException(e, "terminateAll", "Terminate - terminateAll function exception - GetPlatoUninstallTestQuery failed, exception line: ", module, GetPlatoUninstallTestQuery.QUERY_DOCUMENT, emitter);
        }
    }

}

