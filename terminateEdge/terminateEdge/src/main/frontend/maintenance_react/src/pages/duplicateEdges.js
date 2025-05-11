import "../stylePage.css"
import {useState,useEffect} from 'react';

import postService from "../postService";



function DuplicateEdges() {
    const [ckodField, setCkodField] = useState('');
    const [modeField, setModeField] = useState('0');
    const [logField, setLogField] = useState('');
    const [delField, setDelField] = useState('');
    const [edgesfigureField, setEdgesfigureField] = useState('');
    const [delAll, setDelAll] = useState('');
    const [lvmField, setLvmField] = useState('');
    const [modbusField, setModbusField] = useState('0');
    const [sizeField, setSizeField] = useState('0');
    const [loadingbuttonid, setLoadingbuttonid] = useState(null);
    const [lastbuttonname, setLastbuttonname] = useState(null);
    const node = process.env.NODE_ENV;
    const [runtimeMode, setRuntimeMode] = useState('');


    useEffect(() => {
        if(node === 'development'){
            setRuntimeMode('http://localhost:8080');
        }else{
            setRuntimeMode('');
        }
      }, []);


    function add(message,type) {
        const li = document.createElement("li");
        li.innerText = message;
        if(type === 'warning'){
            li.style.color = 'orange';
            li.className = 'warning';
        }
        if(type === 'error'){
            li.style.color = 'red';
            li.className = 'error';
        }
        document.getElementById("duplicateEdgesDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);


    useEffect(() => {
        if (loadingbuttonid && lastbuttonname) {
            const source = new EventSource(runtimeMode+'/maintenance/duplicateEdgeslog');

            source.onmessage = (e) => {
                const eventData = JSON.parse(e.data);
                const { id, type, data } = eventData;
                let loadingbutton;
                switch(type){
                    case 'startSpin':
                        loadingbutton = document.getElementById(loadingbuttonid);
                        loadingbutton.value = "Akce probíhá..";
                        loadingbutton.classList.toggle("active", true);
                    break;
                    case 'endSpin':
                        loadingbutton = document.getElementById(loadingbuttonid);
                        loadingbutton.value = lastbuttonname;
                        loadingbutton.classList.toggle("active", false);
                    break;
                    case 'message':
                        add(data,type);
                    break;
                    case 'warning':
                        add(data,type);
                    break;
                    case 'error':
                        add(data,type);
                    break;
                }

            };

            setEventSource(source);
        }

        return () => {
            if (eventSource) {
                eventSource.close();
            }
        };
    }, [loadingbuttonid,lastbuttonname]);

    function createSubmitPost(){
        setLoadingbuttonid('submitbt');
        setLastbuttonname('Start');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'submitbt':'submitbt' },
            body: JSON.stringify({
                ckod: ckodField,
                log: logField,
                mode: modeField,
                size: sizeField
            }),
        };
        postService(req,runtimeMode+'/maintenance/duplicateEdges')
    }
    function createDelPost(){
        setLoadingbuttonid('delbt');
        setLastbuttonname('Zrušit vazbu');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'delbt':'delbt' },
            body: JSON.stringify({
                del: delField
              }),
        };
        postService(req,runtimeMode+'/maintenance/duplicateEdges')
    }
    function createDelAllPost(){
        setLoadingbuttonid('delall');
        setLastbuttonname('Zrušit duplicitní vazby');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'delall':'delall' },
            body: JSON.stringify({
                delAll: delAll
              }),
        };
        postService(req,runtimeMode+'/maintenance/duplicateEdges')
    }
    function createModPost(){
        setLoadingbuttonid('modbt');
        setLastbuttonname('Změnit modbus');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'modbt':'modbt' },
            body: JSON.stringify({
                lvm: lvmField,
                modbus: modbusField
              }),
        };
        postService(req,runtimeMode+'/maintenance/duplicateEdges')
    }
    function createEdgefingerPost(){
        setLoadingbuttonid('edgefigure');
        setLastbuttonname('Vypsat ID vazeb');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'edgefigure':'edgefigure' },
            body: JSON.stringify({
                edgefigure: edgesfigureField,
              }),
        };
        postService(req,runtimeMode+'/maintenance/duplicateEdges')
    }

    return (
            <div style={{display:"block", width:"90%",marginTop:50}}>
                {/* <!--first column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                
                    {/* <p style="display: none;" id="contextPath" name="contextPath" th:value="${contextPath}" th:text="${contextPath}"></p> */}

                    <label>ckod:</label><input name="ckod" id="ckod" type="text" field={ckodField} onChange={(e) => setCkodField(e.target.value)}/>
                    <label htmlFor="mode">Mode:</label>
                    <select id="mode" name="mode"  onChange={(e) => setModeField(e.target.value)}>
                        <option value="0">zkontrolovat ckod</option>
                        <option value="1">zkontrolovat navázaná zařízení</option>
                        <option value="2">zkontrolovat navázaná umístění</option>
                        <option value="3">zkontrolovat zařízení tohoto typu na inventory</option>
                    </select><br/><br/>

                    <label htmlFor="logset">Log:</label>
                    <select id="logset" name="logset" onChange={(e) => setLogField(e.target.value)}>
                        <option value="0">zobrazit pouze vazby mimo limit</option>
                        <option value="1">zobrazit kompletní log</option>
                    </select><br/><br/>

                    <label id="sizefieldlabel" style={{display:"none"}}>Query size:</label><input name="sizefield" id="sizefield" onChange={(e) => setSizeField(e.target.value)} type="number" min="0" field="*{sizeField}" style={{display:"none"}}/>
                    <label htmlFor="submitbt"></label>
                    <input onClick={createSubmitPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="submitbt" name="submitbt" value="Start"/>

                    <label style={{marginTop:"50px"}}>internalID:</label><input style={{marginTop:"50px"}} name="del" id="del" type="text" field="*{delField}" onChange={(e) => setDelField(e.target.value)}/>
                    <label htmlFor="delbt"></label>
                    <input onClick={createDelPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="delbt" name="delbt" value="Zrušit vazbu"/>

                    <label style={{marginTop:"50px"}} htmlFor="delset">Odvazbit:</label>
                    <select style={{marginTop:"50px"}} id="delset" name="delset" onChange={(e) => setDelAll(e.target.value)}>
                        <option value="0">smazat všechny duplicitní vazby</option>
                        <option value="1">ponechat jednu nejnovější vazbu</option>
                        <option value="2">ponechat jednu nejstarší vazbu</option>
                    </select><br/><br/>

                    <label htmlFor="delall"></label>
                    <input onClick={createDelAllPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="delall" name="delall" value="Zrušit duplicitní vazby"/>



                    <label style={{marginTop:"50px"}}>lvm ckod:</label><input style={{marginTop:"50px"}} name="mod" id="mod" type="text" field="*{modIDField}" onChange={(e) => setLvmField(e.target.value)}/>
                    <label style={{marginTop:"1px"}}>Modbus:</label><input style={{marginTop:"1px"}} name="modpos" id="modpos" type="number" field="*{modField}" onChange={(e) => setModbusField(e.target.value)}/>
                    <label htmlFor="modbt"></label>
                    <input onClick={createModPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="modbt" name="modbt" value="Změnit modbus"/>

                    <label style={{marginTop:"50px"}}>ckod:</label><input style={{marginTop:"50px"}} name="mod" id="mod" type="text" field="*{modIDField}" onChange={(e) => setEdgesfigureField(e.target.value)}/>
                    <label htmlFor="edgefigure"></label>
                    <input onClick={createEdgefingerPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="edgefigure" name="edgefigure" value="Vypsat ID vazeb"/>
                    

                </div>
                {/* <!--second column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    <ul id="duplicateEdgesDisplay" style={{listStyleType:"none"}}></ul>
                </div>


            </div>
    );
}
export default DuplicateEdges;