import "../stylePage.css"
import {useState,useEffect} from 'react';

import postService from "../postService";
import { matchRoutes } from "react-router-dom";



function Didswitcher() {
    const [didoneField, setdidoneField] = useState('');
    const [didtwoField, setdidtwoField] = useState('');
    const [modeField, setModeField] = useState('');
    const [coreElementId, setCoreElementId] = useState('template');
    const [loadingbuttonid, setLoadingbuttonid] = useState(null);
    const [lastbuttonname, setLastbuttonname] = useState(null);
    const node = process.env.NODE_ENV;
    const [runtimeMode, setRuntimeMode] = useState('');
    const [sizeField, setsizeField] = useState('0');
    const [checked, setChecked] = useState(false); 


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
        if(type === 'success'){
            li.style.color = 'green';
            // li.className = 'warning';
        }
        if(type === 'warning'){
            li.style.color = 'orange';
            li.className = 'warning';
        }
        if(type === 'error'){
            li.style.color = 'red';
            li.className = 'error';
        }
        document.getElementById("didswitcherDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);

    useEffect(() => {
        if (loadingbuttonid && lastbuttonname) {
            const source = new EventSource(runtimeMode+'/maintenance/didswitcherlog');
            // const source = new EventSource(contextPath+'/maintenance/ipsetswitchlog');
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
                    case 'success':
                        add(data,type);
                    break;
                    case 'fill':
                        fillCoreElementId(data);
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

    function handleRequirenull(e) {  
        setChecked(e.target.checked); 
      }; 
    function deleteLog(){
        const myNode = document.getElementById("didswitcherDisplay");
        myNode.innerHTML = '';
    };

    function createSubmitPost(){
        setLoadingbuttonid('submitbt');
        setLastbuttonname('Start');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'submitbt':'submit' },
            body: JSON.stringify({
                didOne: didoneField,
                didTwo: didtwoField,
                mode: modeField,
                size: sizeField,
                requireNull: checked,
                coreElementId: coreElementId,
              }),
        };
        postService(req,runtimeMode+'/maintenance/didswitcher')
    }
    function loadCoreElementIds(){
        setLoadingbuttonid("loadElementsbt");
        setLastbuttonname('načíst dostupné id')
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'coreElement':'coreElement' },
            body: JSON.stringify({
                didOne: didoneField,
                didTwo: didtwoField,
                mode: modeField,
                size: sizeField,
                requireNull: checked,
                coreElementId: coreElementId,
              }),
        };
        postService(req,runtimeMode+'/maintenance/didswitcher')
    };
    function fillCoreElementId(data){
        let selectElement = document.getElementById("coreElementId");
        let dataSet = new Set(data);

        let changesMade = false;
        let changes = [];

        for (let i = selectElement.options.length - 1; i >= 0; i--) {
            let optionValue = selectElement.options[i].value;
            if (!dataSet.has(optionValue)) {
                selectElement.remove(i);
                changesMade = true;
                changes.push(`Odebráno: ${optionValue}`);
            }
        }   

        // add ids from data to options
        let currentOptions = Array.from(selectElement.options).map(option => option.value);
        data.forEach(value => {
            if (!currentOptions.includes(value)) {
                let newOption = new Option(value, value);
                selectElement.add(newOption);
                changesMade = true;
                changes.push(`Přidáno: ${value}`);
            }
        });

        if (changesMade) {
            for(var i=0;i<changes.length;i++){
                add(changes[i],"message");
            }
        } else {
            add("List coreElementIds je aktuální","message");
        }
    };


    return (
        <div style={{ display: "block", width: "90%",marginTop:50 }}>
            {/* <!--first column--> */}
            <div style={{ width: "50%", float: "left", display: "inline-block" }}>
                <label id="didoneLabel">did 1:</label>
                <input
                    name="didone"
                    id="didone"
                    type="text"
                    field="*{didoneField}"
                    onChange={(e) => setdidoneField(e.target.value)}
                    style={{ width: 250 }}
                />
                <label id="didtwoLabel">did 2:</label>
                <input
                    name="didtwo"
                    id="didtwo"
                    type="text"
                    field="*{didtwoField}"
                    onChange={(e) => setdidtwoField(e.target.value)}
                    style={{ width: 250 }}
                />

                <label htmlFor="coreElementId">coreElementId:</label>
                <select
                    id="coreElementId"
                    name="coreElementId"
                    onChange={(e) => setCoreElementId(e.target.value)}
                >
                    <option value="template">template</option>
                    <option value="ip_range">ip_range</option>
                    <option value="simcard">simcard</option>
                    <option value="ticket">ticket</option>
                    <option value="ipset">ipset</option>
                    <option value="equipment">equipment</option>
                    <option value="preset">preset</option>
                    <option value="parameters_set">parameters_set</option>
                    <option value="he_group">he_group</option>
                    <option value="system">system</option>
                    <option value="task">task</option>
                    <option value="information">information</option>
                    <option value="place">place</option>
                    <option value="tag">tag</option>
                    <option value="plato">plato</option>
                    <option value="device">device</option>
                    <option value="user">user</option>
                    <option value="apn">apn</option>
                </select>
                <input
                    type="submit"
                    className="submit-small submit-small-button"
                    id="loadElementsbt"
                    name="loadElementsbt"
                    onClick={loadCoreElementIds}
                    value="načíst dostupné id"
                />

                <label htmlFor="mode">Mode:</label>
                <select id="mode" name="mode" onChange={(e) => setModeField(e.target.value)}>
                    <option value="0">ověřit zadané didy</option>
                    <option value="2">
                        kopírovat hodnotu did1 do did2 pro všechna zařízení na inventory
                    </option>
                    <option value="3">
                        vypsat kopírování hodnot did1 do did2 všech zařízení bez provedení
                    </option>
                    <option value="4">
                        přesunout hodnotu did1 do did2 pro všechna zařízení na inventory
                    </option>
                    <option value="5">
                        vypsat přesunutí hodnot did1 do did2 všech zařízení bez provedení
                    </option>
                </select>
                <br />
                <br />

                <label style={{ width: "195px" }} htmlFor="requirenull">
                    did2 musí být null/empty
                </label>
                <input name="requirenull" id="requirenull" type="checkbox" onChange={handleRequirenull} />

                <label htmlFor="sizefield">Query size:</label>
                <input
                    style={{ width: "175px" }}
                    value={sizeField}
                    onChange={(e) => setsizeField(e.target.value)}
                    name="sizefield"
                    id="sizefield"
                    type="number"
                    min="0"
                    field="*{sizeField}"
                />

                <label htmlFor="submitbt"></label>
                <input
                    onClick={createSubmitPost}
                    className="submit submit-button"
                    type="submit"
                    style={{ width: "175px" }}
                    id="submitbt"
                    value="Start"
                />
                <div id="loader" name="loader" className="loader" style={{ display: "none" }}></div>
            </div>
            {/* <!--second column--> */}
            <div style={{ width: "50%", float: "left", display: "inline-block" }}>
                <button
                    className="submit"
                    style={{ width: "120px", marginLeft: "50%" }}
                    onClick={deleteLog}
                >
                    Smazat log
                </button>
                <ul id="didswitcherDisplay" style={{ listStyleType: "none", marginTop: "12%" }}></ul>
            </div>
        </div>
    );
}
export default Didswitcher;