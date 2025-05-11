# oc-maintenance
## Build requirements
gradle 8.1<br>
java 17

### to build:
```
npm install in: terminateEdge/src/main/frontend/maintenance_react<br>
gradle build in terminateEdge/
```


[//]: # (## Config)

[//]: # (Config should be *.properties file and should contain at least:)

[//]: # (**graphql.url** and **graphql.inventory** parameters, parameter **server.port** is optional, if not specified, default port will be set to 8080.<br>)

## Config
Config should be *.properties file and should contain at least:
**server.addr=http://localhost:8080** - specifies server address for frontend event listener and poster.<br>
**graphql.url** and **graphql.inventory** parameters.<br><br>Other optional parameters:<br>
**server.port** - if not specified, default port will be set to 8080.<br> 
**graphql.safe** - if not specified, set to false, parameter determines if GQL querries terminating edges get their termination verified via recursive verification query, this may impact performance if large amount of edges is set to be terminated.<br>
**max_lvmcount** - determines the maximum number of edges that can exist between LVM and DPU, default set to 1.<br>
**max_platocount** - determines the maximum number of edges that can exist between PLATO and DPU, default set to 1.<br>
**max_placecount** - determines the maximum number of edges that can exist between PLACE and DPU, default set to 1.<br>
**max_simcount** - determines the maximum number of edges that can exist between SIM and DPU, default set to 1.<br>
**snmp_maxhistory_days** - determines how many days into the past are being examined by Allign Attributes in snmp<br>
**spring servlet/tomcat size commands** - rising default max http response size for Inventory Import to be able to handle large requests<br>

Example:
```
graphql.url = http://<address>:<port>/graphql
graphql.inventory = http://<address>:<port>/load
server.port = 8080
graphql.safe = true
max_lvmcount = 3
max_platocount = 1
max_placecount = 1
max_simcount = 1
snmp_maxhistory_days = 60

spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB
server.tomcat.max-http-form-post-size=20MB
server.tomcat.max-http-response-header-size=20MB
server.addr=http://localhost:8080
```

[//]: # (## Building JAR with server.addr)

[//]: # (To specify the **server.addr** when building the config path should be specified, if not specified the config.properties from root directory is used.<br>)

[//]: # (Example:)

[//]: # (```)

[//]: # (gradle build generateContextPath -PconfigFile=<path>\config.properties )

[//]: # (```)


## Running JAR
Config file should be specified to run correctly, use parameter: <br>**--spring.config.location=config.properties**<br>
Example:
```
java -jar .\oc-maintenance.jar --spring.config.location=config.properties
```

## Deploy dev
For dev deploy specify the **server.addr** as localhost in your config file and build.<br>
At src/main/frontend/maintenance_react run:
```
npm start
```
Ensure youre running at port 3000 to avoid CORS errors.


## TerminateEdge
Found at: **/terminateEdge**
### Modes
**list all** - lists all device and place edges. <br>
**list terminate requests** - lists terminate requests which would be sent in 'terminate all' mode but doesnt send them <br>
**terminate all** - finds all device and place edges and terminates them, SIM edge is always preserved.<br>
- terminates edges:
- Plato -> DTS, LVM, DPU, AHS, ZDROJ
- DTS (navázané na zadané plato) -> AHS, ZDROJ
- DPU (navázané na zadané plato) -> DTS, LVM
- LVM (navázané na zadané plato) -> Sek.Vývod

**terminate devices** - finds all device edges and terminates them, each device (component) place assignment will be preserved.<br>
- terminates edges:
- Plato -> LVM, DPU, AHS, ZDROJ
- DPU (navázané na zadané plato) -> LVM

**terminate places** - finds all place edges and terminates them, devices (components) will remain bound together.<br>
- terminates edges:
- Plato -> DTS
- DTS (navázané na zadané plato) -> AHS, ZDROJ
- DPU (navázané na zadané plato) -> DTS
- LVM (navázané na zadané plato) -> Sek.Vývod

## IPSet Switcher
Found at: **/ipsetswitch**<br>

Switching IPSet tunnels or active APNs from test -> prod / prod -> test using SIM number or ckod of DPU containing SIM.<br>
### Modes
**List current** - returns current state of tunnel / apn.<br>
**List assigned** - returns list of assigned edges <br>
**Switch to prod** - switches active tunnel / apn to prod, ignored if trying to switch prod -> prod.<br>
**Switch to test** - switches active tunnel / apn to test, ignored if trying to switch test -> test.<br><br>

## DPU Binder
Found at **/dpubind**<br>

Creates edges between given DPU and at least one or more of the following components: plato, place, sim, lvm, ahs, psu.<br>
## Inventory Import
Found at **/inventoryimport**<br>

Allows direct query import to inventory/load as text.

## Duplicate Edges
Found at **/duplicateEdges**<br>

Takes device ckod (plato,lvm,dpu,sim) as input and checks its edges (based on chosen mode), edges over the maximum allowed amount will be returned and can be deleted by the user using attached deletion tools.<br>
### Modes
#### Mode
**check ckod** - checks edges of the provided ckod only<br>
**check binded devices** - checks edges of the provided ckod and every device its bound to including their neighbours<br>
**check binded places** - not implemented yet
**check devices of this type on inventory** - checks the type of device by provided ckod (eg. plato,sim etc..), fetches every device of that type in inventory ale checks them for duplicate edges<br>
#### Log mode
**show edges over the limit** - log will only show edges that are over the allowed amount<br>
**show complete log** - log shows all checked edges regardless of their status
#### Deletion tool mode
**terminate all duplicite edges** - all edges returned as duplicite will be terminated<br>
**leave newest edge** - all edges returned as duplicite will be terminated except the newest created<br>
**leave oldest edge** - all edges returned as duplicite will be terminated except the oldest created<br>

## Allign Attributes
Found at **/attributeallign**<br>

Checks attributes of devices in inventory against list of attributes they should have aswell as newest values of those attributes taken from SNMP, allows to add/update them.<br>
### Modes
#### Mode
**List attributes** - Lists attributes of device with information based on Log and Range filters
**Update missing attributes** - Checks devices attributes against a list of attributes it should contain, if missing, adds this attribute to device with default value 'defaultValue'<br>
**Update found attributes** - Checks attribute values found for this device against most recent updates to those attributes in SNMP, if SNMP contains more recent values, updates the values in inventory<br>
#### Log
Determines what kinds of information is being shown for each mode<br>
#### Range
Determines whether the tool affects one device or whole inventory<br>

## BinderINST
Found at **/binderinst**<br>
### Functionality
Mimics installation test -> lists LVMs with edges to given plato ckod, lists vyvody of DTS bound to given plato, allows to bind those to plato LVMs or unbind.<br>
If plato isnt bound to DTS, creates the edge.

## Maintenance

### Rebuilding / migrating schema
In case of large schema changes (deprecated types etc.) the jarfile will need to be rebuilt for ApolloClient to fetch correct data.<br>
Update **schema.json** at **src/main/graphql/ocmaintenance** Alternatively update individual GQL querries in same directory, desired data are being fetched by **Terminate.java** located at **src/main/java/ocmaintenance**. Update desired data to be fetched/deleted if needed.<br>
Rebuild JAR and run as explained above.