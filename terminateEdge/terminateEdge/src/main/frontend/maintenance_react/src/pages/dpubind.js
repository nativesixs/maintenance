import "../stylePage.css"
import {useState,useEffect} from 'react';
import postService from "../postService";



function Dpubind() {
    const [dpuckodField, setdpuckodField] = useState('');
    const [lvmField, setlvmField] = useState('');
    const [lvmField2, setlvmField2] = useState('');
    const [lvmField3, setlvmField3] = useState('');
    const [vyvodField, setvyvodField] = useState('');
    const [vyvod2Field, setvyvod2Field] = useState('');
    const [vyvod3Field, setvyvod3Field] = useState('');
    const [platoField, setplatoField] = useState('');
    const [placeckodField, setplaceckodField] = useState('');
    const [simckodField, setsimckodField] = useState('');
    const [ahsField, setahsField] = useState('');
    const [zdrojField, setzdrojField] = useState('');
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
        document.getElementById("dpubindDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);

    useEffect(() => {
        if (loadingbuttonid && lastbuttonname) {
            const source = new EventSource(runtimeMode+'/maintenance/dpubindlog');
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
                dpuckod: dpuckodField,
                lvmField: lvmField,
                lvmField2: lvmField2,
                lvmField3: lvmField3,
                vyvodField: vyvodField,
                vyvod2Field: vyvod2Field,
                vyvod3Field: vyvod3Field,
                platoField: platoField,
                placeckodField: placeckodField,
                simckodField: simckodField,
                ahsField: ahsField,
                zdrojField: zdrojField
              }),
        };
        postService(req,runtimeMode+'/maintenance/dpubind')
    }

    return (
            <div style={{display:"block", width:"100%",marginTop:50}}>
                {/* first column */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    {/* <p style={{display: "none"}} id="contextPath" name="contextPath" value="${contextPath}" text="${contextPath}"></p> */}

                    <div style={{ display: "flex", justifyContent: "space-evenly"}}>
                        <div>
                            <h3 style={{ textAlign: "center" }}>Zařízení</h3>
                            <label>dpu ckod:</label><input name="dpuckod" id="dpuckod" type="text" field="*{dpuckodField}" onChange={(e) => setdpuckodField(e.target.value)}/>
                            <label>lvm 1. ckod:</label><input name="lvmckod" id="lvmckod" type="text" field="*{lvmField}" onChange={(e) => setlvmField(e.target.value)}/>
                            <label>lvm 2. ckod:</label><input name="lvmckod" id="lvmckod2" type="text" field="*{lvmField2}" onChange={(e) => setlvmField2(e.target.value)}/>
                            <label>lvm 3. ckod:</label><input name="lvmckod" id="lvmckod3" type="text" field="*{lvmField3}" onChange={(e) => setlvmField3(e.target.value)}/>
                            <label>plato ckod:</label><input name="platockod" id="platockod" type="text" field="*{platoField}" onChange={(e) => setplatoField(e.target.value)}/>
                            <label>sim číslo:</label><input name="simckod" id="simckod" type="text" field="*{simckodField}" onChange={(e) => setsimckodField(e.target.value)}/>
                            <label>ahs ckod:</label><input name="ahsckod" id="ahsckod" type="text" field="*{ahsField}" onChange={(e) => setahsField(e.target.value)}/>
                            <label>zdroj ckod:</label><input name="zdrojckod" id="zdrojckod" type="text" field="*{zdrojField}" onChange={(e) => setzdrojField(e.target.value)}/>
                        </div>
                        <div>
                            <h3 style={{ textAlign: "center" }}>Umístění</h3>
                            <label>sekundarni vyvod pro lvm 1. ckod:</label><input name="vyvod" id="vyvod" type="text" field="*{vyvodField}" onChange={(e) => setvyvodField(e.target.value)}/>
                            <label>sekundarni vyvod pro lvm 2. ckod:</label><input name="vyvod" id="vyvod2" type="text" field="*{vyvod2Field}" onChange={(e) => setvyvod2Field(e.target.value)}/>
                            <label>sekundarni vyvod pro lvm 3. ckod:</label><input name="vyvod" id="vyvod3" type="text" field="*{vyvod3Field}" onChange={(e) => setvyvod3Field(e.target.value)}/>
                            <label>DTS ckod:</label><input name="placeckod" id="placeckod" type="text" field="*{placeckodField}" onChange={(e) => setplaceckodField(e.target.value)}/>
                        </div>
                        
                    </div>

                    <label htmlFor="submitbt"></label>
                    <input onClick={createSubmitPost} className="submit submit-button" type="submit" style={{width:"175px",marginLeft:"25%"}} id="submitbt" value="Start"/>
                    {/* <div id="loader" name="loader" class="loader" style={{display:"none"}}></div> */}
                    
                </div>



                {/* second column */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                <ul id="dpubindDisplay" style={{listStyleType:"none"}}></ul>
                </div>
            </div>

    );
}
export default Dpubind;