import "../stylePage.css"
import {useState, useEffect} from 'react';

function TerminateEdge() {
    const [ckodField, setCkodField] = useState('');
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


    function add(message, type) {
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
        document.getElementById("terminateDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);

    useEffect(() => {
        if (loadingbuttonid) {
            const source = new EventSource(runtimeMode+'/maintenance/terminateEdgelog');
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

    const handleClick = async (e) => {
        e.preventDefault();
        setLoadingbuttonid('submitbt');
        try {
          const response = await fetch(runtimeMode+'/maintenance/terminateEdge', {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                ckod: ckodField,
                mode: modeField
            }),
          });
    
          const responseData = await response.json();
          console.log('Data sent successfully:', responseData);
        } catch (error) {
        }
      };
    
    return (
                <div style={{display:"block", width:"100%",marginTop:50 }}>
                    {/* firstcolumn */}
                        <div style={{width:"50%", float: "left", display: "inline-block"}}>
                                <label>plato ckod:</label><input name="ckod" id="ckod" type="text" onChange={(e) => setCkodField(e.target.value)} />
                                <label htmlFor="mode">Mode:</label>
                                <select id="mode" name="mode" onChange={(e) => setModeField(e.target.value)}>
                                    <option defaultValue={"selected"} value="0">vypsat vazby</option>
                                    <option value="4">vypsat odvazbovaci requesty bez odeslání</option>
                                    <option value="3">odvazbit vše</option>
                                    <option value="2">odvazbit zařízení</option>
                                    <option value="1">odvazbit umístění</option>
                                </select><br></br>
                                <label htmlFor="submitbt"></label>
                                {/* <input onClick={handleClick}  className="submit submit-button" type="submit" style={{width:"175px"}} id="submitbt" value="Start"/> */}
                                <input onClick={handleClick}  className="submit submit-button" type="submit" style={{width:"175px"}} id="submitbt" value="Start"/>
                        </div>
                    {/* secondcolumn */}
                        <div style={{width:"50%", float: "left", display: "inline-block"}}>
                        <meta httpEquiv="Content-Type" content="text/html; charset=UTF-8" />
                        <ul id="terminateDisplay" style={{listStyleType:"none"}}></ul>
                        </div>
                </div>
    );
}
export default TerminateEdge;