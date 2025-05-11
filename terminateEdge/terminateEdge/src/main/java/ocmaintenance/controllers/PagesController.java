package ocmaintenance.controllers;


import static ocmaintenance.controllers.SharedMethods.createEventData;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import ocmaintenance.AllignAttributesDynamic;
import ocmaintenance.BinderINST;
import ocmaintenance.DPUBinderDynamic;
import ocmaintenance.DPUBinderINST;
import ocmaintenance.Didswitcher;
import ocmaintenance.DuplicatesDynamic;
import ocmaintenance.IPsetSwitchDynamic;
import ocmaintenance.InventoryImport;
import ocmaintenance.IpsetIPfixDynamic;
import ocmaintenance.TerminateDynamic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class PagesController {
    @Value("${graphql.url:none}")
    private String url;
    @Value("${server.port:8080}")
    private String port;
    @Value("${graphql.inventory:none}")
    private String inventory;
    @Value("${graphql.safe:false}")
    private boolean safe;
    @Value("${max_lvmcount:3}")
    private int MAX_LVMCOUNT;
    @Value("${max_platocount:1}")
    private int MAX_PLATOCOUNT;
    @Value("${max_placecount:1}")
    private int MAX_PLACECOUNT;
    @Value("${max_simcount:1}")
    private int MAX_SIMCOUNT;
    @Value("${snmp_maxhistory_days:7}")
    private int SNMP_MAXHISTORY_DAYS;
    // @Value("${server.servlet.context-path:}")
    // private String contextPath;
    private ExecutorService nonBlockingService = Executors.newCachedThreadPool();

    /* Emitter types:
        message - default message to be shown in log
        messageTop - default message to be shown ON TOP of log (priority message) - only if priority <ul> is defined in frontend
        warning - warning to be shown in log
        error - error to be shown in log
        success - green success message
        fill - sends array of values to FE to add as options
        startSpin - signal to change css of button/page to signal required function start, not shown in log/console
        endSpin - signal to change css back to default after function ended, not shown in log/console
    */

    @RequestMapping(value = {"/", "/home",
            "/maintenance/terminateEdge", "/terminateEdge",
            "/maintenance/ipsetswitch", "/ipsetswitch",
            "/maintenance/dpubind", "/dpubind",
            "/maintenance/inventoryimport", "/inventoryimport",
            "/maintenance/duplicateEdges", "/duplicateEdges",
            "/maintenance/attributeallign", "/attributeallign",
            "/maintenance/ipsetipfix", "/ipsetipfix",
            "/maintenance/binder", "/binder",
            "/maintenance/dpubindinst", "/dpubindinst",
            "/maintenance/binderinst", "/binderinst",
            "/maintenance/didswitcher", "/didswitcher",
    }, method = RequestMethod.GET)
    public ResponseEntity<Resource> getPage() {
        try {
            Resource resource = new ClassPathResource("static/build/index.html");
            return ResponseEntity.ok()
                                 .contentType(MediaType.TEXT_HTML)
                                 .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @RequestMapping(value = {"/maintenance/terminateEdgelog", "/terminateEdgelog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter terminateEdgelog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/terminateEdge", "/terminateEdge"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void terminateEdgepost(@RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        nonBlockingService.execute(() -> {
            try {
                Thread.sleep(500);
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    TerminateDynamic.main(body.get("ckod").textValue(), body.get("mode").asInt(), inventory, url, safe, emitter);
                    emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @RequestMapping(value = {"/maintenance/ipsetswitchlog", "/ipsetswitchlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter ipsetswitchlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/ipsetswitch", "/ipsetswitch"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void ipsetswitchpost(@RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        nonBlockingService.execute(() -> {
            try {
                Thread.sleep(500);
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        IPsetSwitchDynamic.main(body.get("simckod").textValue(), body.get("dpuckod").textValue(), body.get("mode").asInt(), inventory, url, safe, emitter);
                        emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                    } catch (ExecutionException e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @RequestMapping(value = {"/maintenance/dpubindlog", "/dpubindlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter dpubindlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/dpubind", "/dpubind"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void dpubindpost(@RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        nonBlockingService.execute(() -> {
            try {
                Thread.sleep(500);
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        DPUBinderDynamic.main(body.get("dpuckod").textValue(), body.get("platoField").textValue(), body.get("lvmField").textValue(), body.get("lvmField2").textValue(), body.get("lvmField3").textValue(), body.get("simckodField").textValue(), body.get("placeckodField").textValue(), body.get("ahsField").textValue(), body.get("zdrojField").textValue(), inventory, url, MAX_LVMCOUNT, MAX_PLATOCOUNT, MAX_PLACECOUNT, MAX_SIMCOUNT, emitter, body.get("vyvodField").textValue(), body.get("vyvod2Field").textValue(), body.get("vyvod3Field").textValue());
                        emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                    } catch (ExecutionException e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @RequestMapping(value = {"/maintenance/inventoryimportlog", "/inventoryimportlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter inventoryimportlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/inventoryimport", "/inventoryimport"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void inventoryimportPost(@RequestBody JsonNode body, HttpServletRequest request) throws InterruptedException {
        Map<String, String> selectData = new HashMap<>();
        body.fields().forEachRemaining(entry -> {
            String selectId = entry.getKey();
            String selectedOption = entry.getValue().asText();
            selectData.put(selectId, selectedOption);
        });
        String header1 = request.getHeader("submitbt");
        String header2 = request.getHeader("analyze");
        String header3 = request.getHeader("parsed");

        Thread.sleep(500);
        nonBlockingService.execute(() -> {
            try {
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        if (header1 != null && header1.equals("submitbt")) {
                            InventoryImport.main(body.get("task").textValue(), inventory, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header2 != null && header2.equals("analyze")) {
                            InventoryImport.analyze(body.get("task").textValue(), emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header3 != null && header3.equals("parsed")) {
                            InventoryImport.sendParsed(body.get("task").textValue(), body.get("size").asInt(), inventory, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        }
                    } catch (Exception e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @RequestMapping(value = {"/maintenance/duplicateEdgeslog", "/duplicateEdgeslog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter duplicateEdgeslog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/duplicateEdges", "/duplicateEdges"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void duplicateEdgesPost(@RequestBody JsonNode body, HttpServletRequest request) throws InterruptedException {
        Map<String, String> selectData = new HashMap<>();
        body.fields().forEachRemaining(entry -> {
            String selectId = entry.getKey();
            String selectedOption = entry.getValue().asText();
            selectData.put(selectId, selectedOption);
        });
        String header1 = request.getHeader("submitbt");
        String header2 = request.getHeader("delbt");
        String header3 = request.getHeader("delall");
        String header4 = request.getHeader("modbt");
        String header5 = request.getHeader("edgefigure");
        Thread.sleep(500);
        nonBlockingService.execute(() -> {
            try {
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        if (header5 != null && header5.equals("edgefigure")) {
                            DuplicatesDynamic.figureEdges(body.get("edgefigure").textValue(), url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header4 != null && header4.equals("modbt")) {
                            DuplicatesDynamic.changeModbus(body.get("modbus").asInt(), body.get("lvm").textValue(), inventory, url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header3 != null && header3.equals("delall")) {
                            DuplicatesDynamic.delAll(inventory, url, body.get("delAll").asInt(), emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header1 != null && header1.equals("submitbt")) {
                            DuplicatesDynamic.main(body.get("ckod").textValue(), url, body.get("mode").asInt(), body.get("log").asInt(), body.get("size").asInt(), emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header2 != null && header2.equals("delbt")) {
                            DuplicatesDynamic.delIndividual(body.get("del").textValue(), inventory, url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        }
                    } catch (Exception e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @RequestMapping(value = {"/maintenance/attributeallignlog", "/attributeallignlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter attributeallignlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/attributeallign", "/attributeallign"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void attributeallignpost(@RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        nonBlockingService.execute(() -> {
            try {
                Thread.sleep(500);
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        AllignAttributesDynamic.main(body.get("ckod").textValue(), body.get("log").asInt(), body.get("mode").asInt(), body.get("rozsah").asInt(), body.get("zarizeni").asInt(), Integer.parseInt(body.get("size").textValue()), inventory, url, SNMP_MAXHISTORY_DAYS, emitter);
                        emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                    } catch (ExecutionException e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    Inputs ipsetfix = new Inputs();

    public static List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @RequestMapping(value = {"/maintenance/ipsetipfixlog", "/ipsetipfixlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter ipsetipfixlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/ipsetipfix", "/ipsetipfix"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void ipsetipfixpost(@RequestBody com.fasterxml.jackson.databind.JsonNode body) {
        ipsetfix.setMode(body.get("mode").asInt());
        ipsetfix.setDelset(body.get("delset").asInt());
        ipsetfix.setSizeField(body.get("size").asInt());
        nonBlockingService.execute(() -> {
            try {
                Thread.sleep(500);
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        IpsetIPfixDynamic.main(inventory, url, ipsetfix.getMode(), ipsetfix.getDelset(), ipsetfix.getSizeField(), emitter);
                        emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                    } catch (ExecutionException e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @RequestMapping(value = {"/maintenance/dpubindinstlog", "/dpubindinstlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter dpubindinstlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/dpubindinst", "/dpubindinst"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void dpubindinstpost(@ModelAttribute Inputs form, Model model) {
        model.addAttribute("data", form);
        // model.addAttribute("contextPath", contextPath);
        nonBlockingService.execute(() -> {
            try {
                Thread.sleep(500);
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        DPUBinderINST.main(form.getDpuckodField(), form.getPlatoField(), form.getLvmField(), form.getLvmField2(), form.getLvmField3(), form.getSimckodField(), form.getPlaceckodField(), form.getAhsField(), form.getZdrojField(), form.getVyvodField(), form.getVyvod2Field(), form.getVyvod3Field(), inventory, url, MAX_LVMCOUNT, MAX_PLATOCOUNT, MAX_PLACECOUNT, MAX_SIMCOUNT, emitter);
                        emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                    } catch (Exception e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Map<String, List<String>> binderLvms = new HashMap<>();

    @RequestMapping(value = {"/maintenance/binderinst", "/binderinst"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void binderinstPost(@RequestBody JsonNode body, HttpServletRequest request) {
        Map<String, String> selectData = new HashMap<>();
        body.fields().forEachRemaining(entry -> {
            String selectId = entry.getKey();
            String selectedOption = entry.getValue().asText();
            selectData.put(selectId, selectedOption);
        });
        String header1 = request.getHeader("addvyvodytolvmunbind");
        String header2 = request.getHeader("addvyvodytolvm");
        String header3 = request.getHeader("analyze");
        String header4 = request.getHeader("prevazatPlatoDTS");
        String header5 = request.getHeader("navazatPlatoDts");
        nonBlockingService.execute(() -> {
            try {
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                    try {
                        if (header5 != null && header5.equals("navazatPlatoDts")) {
                            BinderINST.bindPlatoDTS(selectData.get("plato"), selectData.get("place"), inventory, url, emitter);
                            BinderINST.checkLvmVyvody(selectData, inventory, url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header4 != null && header4.equals("prevazatPlatoDTS")) {
                            BinderINST.unbindPlatoDTS(selectData.get("plato"), inventory, url, emitter);
                            BinderINST.bindPlatoDTS(selectData.get("plato"), selectData.get("place"), inventory, url, emitter);
                            BinderINST.checkLvmVyvody(selectData, inventory, url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header3 != null && header3.equals("analyze")) {
                            BinderINST.analyzeInput(body.get("plato").textValue(), body.get("place").textValue(), inventory, url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header1 != null && header1.equals("addvyvodytolvmunbind")) {
                            BinderINST.checkLvmVyvody(selectData, inventory, url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        } else if (header2 != null && header2.equals("addvyvodytolvm")) {
                            BinderINST.checkLvmVyvody(selectData, inventory, url, emitter);
                            emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        }
                    } catch (Exception e) {
                        emitter.send(e);
                    }
                    emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @RequestMapping(value = {"/maintenance/binderinstlog", "/binderinstlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter binderinstlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/didswitcherlog", "/didswitcherlog"}, consumes = MediaType.ALL_VALUE, method = RequestMethod.GET)
    @CrossOrigin(origins = "http://localhost:3000")
    public SseEmitter didswitcherlog() {
        SseEmitter sseEmitter = new SseEmitter(-1L);
        try {
            sseEmitter.send(SseEmitter.event().name("init"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        sseEmitter.onCompletion(() -> emitters.remove(sseEmitter));
        emitters.add(sseEmitter);
        return sseEmitter;
    }

    @RequestMapping(value = {"/maintenance/didswitcher", "/didswitcher"}, method = RequestMethod.POST)
    @CrossOrigin(origins = "http://localhost:3000")
    public void didswitcherpost(@RequestBody com.fasterxml.jackson.databind.JsonNode body, HttpServletRequest request) {
        String getElementHeader = request.getHeader("coreElement");
        String getSubmit = request.getHeader("submitbt");
        nonBlockingService.execute(() -> {
            try {
                Thread.sleep(500);
                SseEmitter emitter = emitters.get(emitters.size() - 1);
                try {
                    if(getElementHeader!=null){
                        emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                        Didswitcher.loadAvailableCoreElements(url,emitter);
                        emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                    }
                    if(getSubmit!=null) {
                        emitter.send(SseEmitter.event().data(createEventData("", "startSpin")));
                        // Didswitcher.main(body.get("ckod").textValue(), body.get("didOne").textValue(), body.get("didTwo").textValue(), body.get("value").textValue(), body.get("mode").asInt(), body.get("devicetype").textValue(), inventory, url, emitter);
                        Didswitcher.main(body.get("didOne").textValue(), body.get("didTwo").textValue(), body.get("mode").asInt(), body.get("size").asInt(), body.get("requireNull").asBoolean(), body.get("coreElementId").textValue(), inventory, url, emitter);
                        emitter.send(SseEmitter.event().data(createEventData("---Konec logu---")));
                        emitter.send(SseEmitter.event().data(createEventData("", "endSpin")));
                    }
                } catch (IOException e) {
                    emitters.remove(emitter);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }


}


