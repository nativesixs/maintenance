import "../stylePage.css"
import {useState,useEffect} from 'react';
import postService from "../postService";

import * as React from 'react';
import { styled } from '@mui/material/styles';
import Button from '@mui/material/Button';
import Tooltip, { tooltipClasses } from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

const HtmlTooltip = styled(({ className, ...props }) => (
    <Tooltip {...props} classes={{ popper: className }} />
  ))(({ theme }) => ({
    [`& .${tooltipClasses.tooltip}`]: {
      backgroundColor: '#f5f5f9',
      color: 'rgba(0, 0, 0, 0.87)',
      maxWidth: 520,
      fontSize: theme.typography.pxToRem(12),
      border: '1px solid #dadde9',
    },
  }));

function Inventoryimport() {
    const [taskField, settaskField] = useState('');
    const [sizeField, setsizeField] = useState('0');
    // const contextPath = process.env.REACT_APP_BACKEND_URL;
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
        document.getElementById("inventoryImportDisplay").appendChild(li);
    }

    const [eventSource, setEventSource] = useState(null);

    useEffect(() => {
        if (loadingbuttonid && lastbuttonname) {
            const source = new EventSource(runtimeMode+'/maintenance/inventoryimportlog');
            // const source = new EventSource(contextPath+'/maintenance/inventoryimportlog');
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
        setLastbuttonname('Odeslat');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'submitbt':'submitbt' },
            body: JSON.stringify({
                task: taskField,
                size: sizeField
              }),
        };
        postService(req,runtimeMode+'/maintenance/inventoryimport')
    }
    function createAnalyzePost(){
        setLoadingbuttonid('analyzebt');
        setLastbuttonname('Analyze');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'analyze':'analyze' },
            body: JSON.stringify({
                task: taskField,
                size: sizeField
              }),
        };
        postService(req,runtimeMode+'/maintenance/inventoryimport')
    }
    function createParsedPost(){
        setLoadingbuttonid('sendparsed');
        setLastbuttonname('Odeslat ve varce');
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'parsed':'parsed' },
            body: JSON.stringify({
                task: taskField,
                size: sizeField
              }),
        };
        postService(req,runtimeMode+'/maintenance/inventoryimport')
    }
    return (
            <div className="container">
                        {/* both columns */}
            <div style={{display:"block", width:"90%",marginTop:50}}>
                {/* <!--first column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                        {/* <p style={{display: "none"}} id="contextPath" name="contextPath" value="${contextPath}" text="${contextPath}"></p> */}

                        <textarea className="textar" rows="20" field="*{taskField}" onChange={(e) => settaskField(e.target.value)}></textarea>

                        <HtmlTooltip className="button-tooltip"
                        title={
                        <React.Fragment>
                            <Typography color="inherit">Tlačítko Odeslat</Typography>
                            {"Odešle všechny příkazy k provedení najednou, vyžaduje tvar:"}<br/>{"UPDATE_INSTANCE:"}<br/>{`{"id":"MTIxNzc=","information:attribute.ckod":"9057030000000507","information:attribute.modbus_pozice":2}`}
                        </React.Fragment>
                        }>
                        <label htmlFor="submitbt"></label>
                        <input onClick={createSubmitPost} className="submit submit-button" type="submit" style={{width:"175px"}} id="submitbt" name="submitbt" value="Odeslat"/>
                        {/* <div id="loader" name="loader" className="loader" style={{display:"none"}}></div> */}
                        </HtmlTooltip>

                        <HtmlTooltip className="button-tooltip"
                        title={
                        <React.Fragment>
                            <Typography color="inherit">Tlačítko Analyze</Typography>
                            {"Vypíše počet příkazů v okně, známé příkazy:"}<br/>{"CREATE_INSTANCE:"}<br/>{"CREATE_DEVICE:"}<br/>{"CREATE_PLACE:"}<br/>{"CREATE_EDGE:"}<br/>{"DELETE_INSTANCE:"}<br/>{"DELETE_EDGE:"}<br/>{"TERMINATE_EDGE:"}<br/>{"UPDATE_INSTANCE"}
                        </React.Fragment>
                        }>
                        <label htmlFor="analyzebt"></label>
                        <input onClick={createAnalyzePost} className="submit submit-button" type="submit" style={{width:"175px",marginBottom:"20px"}} name="analyzebt" id="analyzebt"  value="Analyze"/>

                        </HtmlTooltip>

                        {/* <div id="loader2" class="loader" style={{display:"none"}}></div> */}

                        <label htmlFor="sizefield">Velikost várky</label><input style={{width:"175px"}} value={sizeField} onChange={(e) => setsizeField(e.target.value)} name="sizefield" id="sizefield" type="number" min="0" field="*{sizeField}"/>

                        <HtmlTooltip className="button-tooltip"
                        title={
                        <React.Fragment>
                            <Typography color="inherit">Tlačítko Odeslat varky</Typography>
                            {"Rozdělí příkazy v okně na várky dle zadané velikosti které odešle. př:"}<br/>
                            {"Zadáno: velikost várky = 2 s příkazy:"}<br/><br/>
                            {"UPDATE_INSTANCE:"}<br/>{`{"id":"1","information:attribute.ckod":"1","information:attribute.modbus_pozice":2}`}<br/>
                            {"UPDATE_INSTANCE:"}<br/>{`{"id":"2","information:attribute.ckod":"2","information:attribute.modbus_pozice":2}`}<br/>
                            {"UPDATE_INSTANCE:"}<br/>{`{"id":"3","information:attribute.ckod":"3","information:attribute.modbus_pozice":2}`}<br/><br/>
                            {"Odesláno jako:"}<br/><br/>
                            {"Varka 1:"}<br/>
                            {"UPDATE_INSTANCE:"}<br/>{`{"id":"1","information:attribute.ckod":"1","information:attribute.modbus_pozice":2}`}<br/>
                            {"UPDATE_INSTANCE:"}<br/>{`{"id":"2","information:attribute.ckod":"2","information:attribute.modbus_pozice":2}`}<br/>
                            {"Varka 2:"}<br/>
                            {"UPDATE_INSTANCE:"}<br/>{`{"id":"3","information:attribute.ckod":"3","information:attribute.modbus_pozice":2}`}<br/>
                        </React.Fragment>
                        }>
                        <label htmlFor="sendparsed"></label>
                        <input onClick={createParsedPost} className="submit submit-button" type="submit" style={{width:"175px"}} name="sendparsed" id="sendparsed"  value="Odeslat ve varce"/>
                        {/* <div id="loader3" class="loader" style={{display:"none"}}></div> */}
                        </HtmlTooltip>

                </div>
                {/* <!--second column--> */}
                <div style={{width:"50%", float: "left", display: "inline-block"}}>
                    <ul id="inventoryImportDisplay" style={{listStyleType:"none"}}></ul>
                </div>


            </div>
            </div>


    );
}
export default Inventoryimport;