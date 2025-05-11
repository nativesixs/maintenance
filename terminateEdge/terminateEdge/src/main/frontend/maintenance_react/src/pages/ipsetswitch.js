import "../stylePage.css"
import {useState,useEffect} from 'react';

import postService from "../postService";



function Ipsetswitch() {
    const [simckodField, setsimckodField] = useState('');
    const [dpuckodField, setdpuckodField] = useState('');
    const [modeField, setModeField] = useState('');
    const [loadingbuttonid, setLoadingbuttonid] = useState(null);
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
        document.getElementById("ipsetSwitchDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);

    useEffect(() => {
        if (loadingbuttonid) {
            const source = new EventSource(runtimeMode+'/maintenance/ipsetswitchlog');
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
    }, [loadingbuttonid]);

    function createSubmitPost(){
        setLoadingbuttonid('submitbt');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'submitbt':'submit' },
            body: JSON.stringify({
                simckod: simckodField,
                dpuckod: dpuckodField,
                mode: modeField
              }),
        };
        postService(req,runtimeMode+'/maintenance/ipsetswitch')
    }

    return (
            <div style={{display:"block", width:"90%",marginTop:50}}>
                {/* <!--first column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    {/* <p style={{display: "none"}} id="contextPath" name="contextPath" value="${contextPath}" text="${contextPath}"></p> */}

                    <label>sim číslo:</label><input name="sim number" id="simckod" type="text" field="*{simckodField}" onChange={(e) => setsimckodField(e.target.value)}/>
                    <label>dpu ckod:</label><input name="dpuckod" id="dpuckod" type="text" field="*{dpuckodField}" onChange={(e) => setdpuckodField(e.target.value)}/>

                    <label htmlFor="mode">Mode:</label>
                    <select id="mode" name="mode" onChange={(e) => setModeField(e.target.value)}>
                        <option value="0">vypsat aktivní</option>
                        <option value="5">vypsat assigned vazby</option>
                        <option value="1">přepnout IPSet na prod</option>
                        <option value="2">přepnout IPSet na test</option>
                        <option value="3">přepnout APN na prod</option>
                        <option value="4">přepnout APN na test</option>
                    </select><br/><br/>


                    <label htmlFor="submitbt"></label>
                    <input onClick={createSubmitPost} className="submit submit-button" type="submit"style={{width:"175px"}} id="submitbt" value="Start"/>
                    <div id="loader" name="loader" className="loader" style={{display:"none"}}></div>
                </div>
                {/* <!--second column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    <ul id="ipsetSwitchDisplay" style={{listStyleType:"none"}}></ul>
                </div>
            </div>

    );
}
export default Ipsetswitch;