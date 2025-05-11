import "../stylePage.css"
import {useState,useEffect} from 'react';

import postService from "../postService";



function AttributeAllign() {
    const [ckodField, setCkodField] = useState('');
    const [modeField, setModeField] = useState('0');
    const [logField, setLogField] = useState('0');
    const [rozsahField, setRozsahField] = useState('0');
    const [zarizeniField, setZarizeniField] = useState('0');
    const [loadingbuttonid, setLoadingbuttonid] = useState(null);
    const [lastbuttonname, setLastbuttonname] = useState(null);
    const node = process.env.NODE_ENV;
    const [runtimeMode, setRuntimeMode] = useState('');
    const [sizeField, setsizeField] = useState('0');


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
        document.getElementById("attributeallignDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);

    useEffect(() => {
        if (loadingbuttonid && lastbuttonname) {
            const source = new EventSource(runtimeMode+'/maintenance/attributeallignlog');
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
                        loadingbutton.value = "Start";
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
                        'submitbt':'submit' },
            body: JSON.stringify({
                ckod: ckodField,
                mode: modeField,
                log: logField,
                rozsah: rozsahField,
                zarizeni: zarizeniField,
                size: sizeField
              }),
        };
        postService(req,runtimeMode+'/maintenance/attributeallign')
    }

    function handleModeChange(e) {
        setModeField(e.target.value);
        let modl = document.getElementById("logsetlabel");
        let mod = document.getElementById("logset");
        if(e.target.value === "0"){
            modl.style.display = "block";
            mod.style.display = "block";
        }else{
            modl.style.display = "none";
            mod.style.display = "none";
        }
    }

    function handleRozsahChange(e) {
        setRozsahField(e.target.value);
        let modl = document.getElementById("devicelabel");
        let mod = document.getElementById("device1");
        let sz = document.getElementById("sizefield");
        let szl = document.getElementById("sizefieldlabel");
        if(e.target.value === "0"){
            modl.style.display = "none";
            mod.style.display = "none";
            sz.style.display = "none";
            szl.style.display = "none";
        }else{
            modl.style.display = "block";
            mod.style.display = "block";
            sz.style.display = "block";
            szl.style.display = "block";
        }
    }

    return (
            <div style={{display:"block", width:"90%",marginTop:50}}>
                {/* <!--first column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    <label>ckod zarizeni:</label><input name="dpuckod" id="dpuckod" type="text" field="*{dpuckodField}" onChange={(e) => setCkodField(e.target.value)}/>
                    <label htmlFor="mode">Mode:</label>
                    {/* <select id="mode" name="mode" onChange={(e) => setModeField(e.target.value)}> */}
                    <select id="mode" name="mode" onChange={(e) => handleModeChange(e)}>
                        <option value="0">vypsat atributy</option>
                        <option value="1">přidat chybějící atributy</option>
                        <option value="2">aktualizovat nalezené atributy</option>
                        <option value="3">vypsat aktualizacni requesty bez odeslani</option>
                    </select><br/><br/>

                    <label id="logsetlabel" htmlFor="logset" style={{display:"block"}}>Log:</label>
                    <select id="logset" style={{display:"block"}} name="logset" onChange={(e) => setLogField(e.target.value)}>
                        <option value="0">vypsat atributy a jejich hodnoty</option>
                        <option value="1">vypsat pouze atributy</option>
                        <option value="2">vypsat pouze chybějící atributy</option>
                        <option value="3">vypsat pouze neaktuální atributy</option>
                    </select><br/><br/>

                    <label htmlFor="delset">Rozsah:</label>
                    {/* <select id="delset" name="delset" onChange={(e) => setRozsahField(e.target.value)}> */}
                    <select id="delset" name="delset" onChange={(e) => handleRozsahChange(e)}>
                        <option value="0">pouze pro tohle zařízení</option>
                        <option value="1">pro všechna zařízení na inventory</option>
                    </select><br/><br/>
                    
                    <label id="devicelabel" htmlFor="device1" style={{display:"none"}}>Zarizeni:</label>
                    <select id="device1" name="device1" style={{display:"none"}} onChange={(e) => setZarizeniField(e.target.value)}>
                        <option value="0">dpu</option>
                        <option value="1">lvm</option>
                    </select><br/><br/>

                    <label id="sizefieldlabel" name="sizefieldlabel" htmlFor="sizefield" style={{display:"none"}}>Query size:</label>
                    <input style={{width:"175px",display:"none"}} onChange={(e) => setsizeField(e.target.value)} name="sizefield" id="sizefield" type="number" min="0"/>

                    <label htmlFor="submitbt"></label>
                    <input onClick={createSubmitPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="submitbt" value="Start"/>
                </div>
                {/* <!--second column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    <ul id="attributeallignDisplay" style={{listStyleType:"none"}}></ul>
                </div>


            </div>
    );
}
export default AttributeAllign;