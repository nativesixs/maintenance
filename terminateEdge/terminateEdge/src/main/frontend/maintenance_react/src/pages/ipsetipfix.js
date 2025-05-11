import "../stylePage.css"
import {useState,useEffect} from 'react';

import postService from "../postService";



function Ipsetipfix() {
    const [modeField, setModeField] = useState('0');
    const [sizeField, setsizeField] = useState('0');
    const [delset, setdelset] = useState('0');
    const [loadingbuttonid, setLoadingbuttonid] = useState(null);
    const node = process.env.NODE_ENV;
    const [runtimeMode, setRuntimeMode] = useState('');
    var [topCounter, setTopCounter] = useState(0);


    useEffect(() => {
        if(node === 'development'){
            setRuntimeMode('http://localhost:8080');
        }else{
            setRuntimeMode('');
        }
      }, []);

    function addToTop(message,type){
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
        document.getElementById("ipsetIpDisplayPriority").appendChild(li);
    }

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
        document.getElementById("ipsetIpDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);

    useEffect(() => {
        if (loadingbuttonid) {
            const source = new EventSource(runtimeMode+'/maintenance/ipsetipfixlog');
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
                    case 'messageTop':
                        addToTop(data,type);
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
                mode: modeField,
                size: sizeField,
                delset: delset
              }),
        };
        postService(req,runtimeMode+'/maintenance/ipsetipfix')
    }
    

    return (
            <div style={{display:"block", width:"90%",marginTop:50}}>
                {/* <!--first column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    <label htmlFor="mode">Mode:</label>
                    <select id="mode" name="mode" onChange={(e) => setModeField(e.target.value)}>
                        <option value="0">vypsat nikam nenavazbene ipsety</option>
                        <option value="1">migrace na nove ipset-test</option>
                        <option value="4">nalezeni duplicitnich adres ipsetu</option>
                        <option value="2">oprava vazby apn na sim test-prod if ipset-prod</option>
                        <option value="3">priradit HE_Groups na Ipset Prod</option>
                    </select><br/><br/>

                    <label htmlFor="sizefield">Query size:</label><input style={{width:"175px"}} value={sizeField} onChange={(e) => setsizeField(e.target.value)} name="sizefield" id="sizefield" type="number" min="0" field="*{sizeField}"/>
                    {/* block only sometimes */}
                    <label id="delsetlabel" htmlFor="delset" style={{display:"block"}}>Mod:</label>
                    <select id="delset" name="delset" style={{display:"block"}} onChange={(e) => setdelset(e.target.value)}>
                    <option value="0">pouze vypsat</option>
                    <option value="1">provest akci</option>
                    </select><br/><br/>

                    <label htmlFor="submitbt"></label>
                    <input onClick={createSubmitPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="submitbt" name="Start" value="Start"/>
                    {/* <div id="loader" name="loader" class="loader" style={{display:"none"}}></div> */}
                </div>


                {/* <!--second column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    <ul id="ipsetIpDisplayPriority" style={{listStyleType:"none"}}></ul>
                    <ul id="ipsetIpDisplay" style={{listStyleType:"none"}}></ul>
                </div>


            </div>
    );
}
export default Ipsetipfix;