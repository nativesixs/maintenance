import "../stylePage.css"
import {useState, useEffect, useRef} from 'react';
import postService from "../postService";
import { createButton } from "../createActionButtons";
let createdElements = [];

function Binderinst() {
    const [runtimeMode, setRuntimeMode] = useState('');

    const [placeField, setPlaceField] = useState('');
    const [platoField, setPlatoField] = useState('');
    const placeFieldRef = useRef(placeField);
    const platoFieldRef = useRef(platoField);
    // const contextPath = process.env.REACT_APP_BACKEND_URL;
    const node = process.env.NODE_ENV;


    useEffect(() => {
        if(node === 'development'){
            setRuntimeMode('http://localhost:8080');
        }else{
            setRuntimeMode('');
        }
      }, [node]);


    let counter = 1;

    function add(message) { // adds emitter message to log
        const li = document.createElement("li");
        li.innerText = message;
        document.getElementById("binderinstDisplay").appendChild(li);
    }
    function checkElementExists(id) { // check if element with id exists
        return document.getElementById(id) !== null;
    }

    function createAnySelect(selectid,labelid,labelmessage){ // creates select with label
        if(!checkElementExists(selectid)){
            var lab = document.createElement('label');
            var sel = document.createElement('select');
            lab.id = labelid;
            createdElements.push(labelid);
            sel.id = selectid;
            createdElements.push(selectid);
            lab.innerHTML = labelmessage;
            lab.style.whiteSpace = 'nowrap';
            lab.style.display = 'block';
            lab.htmlFor = labelmessage;
            sel.style.marginLeft = '90px';
            var logpartDiv = document.getElementById('logpart');
            logpartDiv.appendChild(lab);
            logpartDiv.appendChild(sel);
        }
    }
    function createOptionForAnySelect(data,selectid) { // creates one option for select
        // if(checkDuplicateOptions(select,data)){
            var option = document.createElement('option');
            option.value = data;
            option.textContent = data;
            var select = document.getElementById(selectid)
            select.appendChild(option);
            if(data==='ponechat aktualni'){
                option.selected = true;
            }
            option.selected = true;
        // }
    }

    //TODO - DEPRECATE THIS
    function createSelect(message,counter){ // creates select with label LVM: abcabc
        if(!checkElementExists(message)){
            var lab = document.createElement('label');
            var sel = document.createElement('select');
            lab.id = 'lvm'+counter;
            createdElements.push('lvm'+counter);
            sel.id = message;
            createdElements.push(message);
            lab.innerHTML = 'LVM: '+message;
            lab.style.whiteSpace = 'nowrap';
            lab.style.display = 'block';
            lab.htmlFor = message;
            sel.style.marginLeft = '90px';
            var logpartDiv = document.getElementById('logpart');
            logpartDiv.appendChild(lab);
            logpartDiv.appendChild(sel);

            counter = counter+1;
        }
    }

    function wipeCreatedElements(){
        createdElements.forEach(element => {
            const elementId = element;
            const elementToDelete = document.getElementById(elementId);
            if (elementToDelete) {
                elementToDelete.remove();
            }
        });
        // document.getElementById('binderinstDisplay').innerHTML=''; 
        // mazani logu - mozna nechtene
    }

    function checkDuplicateOptions(select,data){ // check if option exists in given select
        if (!select) {
            return false;
        }
        let options = select.options;
        for (var i = 0; i < options.length; i++) {
            if(options[i].value===data){
                return false;
            }
        }
        return true;
    }

    function createOptionForSelect(data, counter) {
        var parts = data.split('separator');
        var select = document.getElementById(parts[0])
        data = parts[1];

        if(checkDuplicateOptions(select,data)){
            var option = document.createElement('option');
            option.value = data;
            option.textContent = data;
            select.appendChild(option);

            if(data==='ponechat aktualni'){
                option.selected = true;
            }
        }
    }

    useEffect(() => { // creates ref for place - allowing to create option for mock select after log actions 
        placeFieldRef.current = placeField;
    }, [placeField]);
    useEffect(() => {
        platoFieldRef.current = platoField;
    }, [platoField]);

    useEffect(() => {
        // const eventSource = new EventSource('/maintenance/binderinstlog');
        // const eventSource = new EventSource(contextPath+'/maintenance/binderinstlog');
        const eventSource = new EventSource(runtimeMode+'/maintenance/binderinstlog');
        eventSource.onmessage = (e) => {
            
            if(e.data.includes('deletePreviouslyCreatedElements')){
                wipeCreatedElements();
            }

            if(e.data.includes('createElementAddVyvodyToLvm')){
                if(!checkElementExists('AddVyvodyToLvm')){
                    createdElements.push('AddVyvodyToLvm');
                    const button = createButton('AddVyvodyToLvm',AddVyvodyToLvmPost);
                    var logpartDiv = document.getElementById('logpart');
                    logpartDiv.appendChild(button);
                }
            }
            if(e.data.includes('odvazatVyvod')){ // prida mock select k odvazani vyvodu od lvm
                if(!checkElementExists('odvazatvyvod')){
                    createAnySelect('odvazatvyvod','odvazatvyvodlabel','Odvazat vyvod od LVM:');
                    let data = e.data;
                    data = data.replace('odvazatVyvod','');
                    createOptionForAnySelect(data,'odvazatvyvod');
                    createdElements.push('odvazatvyvod');
                }
            }
            if(e.data.includes('createElementUnbindVyvodBindCorrect')){ // prida tlacitko od odvazani vyvody jine DTS nez je prirazena platu a privazani spravneho vyvodu
                if(!checkElementExists('UnbindVyvodBindCorrect')){
                    createdElements.push('UnbindVyvodBindCorrect');
                    const button = createButton('UnbindVyvodBindCorrect',UnbindVyvodBindCorrectPost);
                    var logpartDiv = document.getElementById('logpart');
                    logpartDiv.appendChild(button);
                }
            }

            if(e.data.includes('createElementNavazatPlato')){ // prida tlacitko pro navazani plata na dts a privazani vyvody k lvm tohoto plata
                if(!checkElementExists('navazatPlatoDts')){
                    createdElements.push('navazatPlatoDts');
                    const button = createButton('navazatPlatoDts',navazatPlatoDtsPost);
                    var logpartDiv = document.getElementById('logpart');
                    logpartDiv.appendChild(button);
                }
            }
            if(e.data.includes('odvazatOd')){ // prida label, select a option aktualni DTS ktera se ma odvazat
                if(!checkElementExists('odvazatplato')){
                    createAnySelect('odvazatplato','odvazatplatolabel','Odvazat plato od DTS:');
                    let data = e.data;
                    data = data.replace('odvazatOd','');
                    createOptionForAnySelect(data,'odvazatplato');
                    createdElements.push('odvazatplato');
                }
            }
            if(e.data.includes('privazatK')){ // prida select a option DTS ktera se ma nove navazat na plato
                if(!checkElementExists('platodts')){
                    createAnySelect('platotodts','platotodtslabel','Navazat plato na DTS:');
                    createOptionForAnySelect(placeFieldRef.current,'platotodts');
                    createdElements.push('platodts');
                }
            }
            if(e.data.includes('createElementNewDTSvyvody')){ // prida tlacitko potvrzeni case 2 BE
                if(!checkElementExists('newDtsVyvody')){
                    createdElements.push('newDtsVyvody');
                    const button = createButton('newDtsVyvody',prevazatPlatoDTSPost);
                    var logpartDiv = document.getElementById('logpart');
                    logpartDiv.appendChild(button);
                }
            }


            if(e.data.includes('createLVM')){ // prida label, select pro pridani vyvody na lvm
                let data = e.data;
                data = data.replace('createLVM','');
                createSelect(data, counter);
            }

            if(e.data.includes('addOption')){ // prida option pro select
                let data = e.data;
                data = data.replace('addOption','');
                createOptionForSelect(data,counter);  
            }
            if(e.data != 'endSpin' && e.data != 'startSpin' 
            && !e.data.includes('createLVM') 
            && !e.data.includes('addOption') 
            && !e.data.includes('createElementNavazatPlato')
            && !e.data.includes('createElementNewDTSvyvody')
            && !e.data.includes('deletePreviouslyCreatedElements')
            && !e.data.includes('createElementAddVyvodyToLvm')
            && !e.data.includes('createElementUnbindVyvodBindCorrect')
            && !e.data.includes('odvazatOd')
            && !e.data.includes('privazatK')
            && !e.data.includes('odvazatVyvod')
            ){
                console.log(e.data);
                add(e.data);
            }

        };
    
        return () => {
          eventSource.close();
        };
      }, []);

    function createReq(header){
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        header:header },
            body: JSON.stringify({
                plato: platoField,
                place: placeField
              }),
        };
        return req;
    }
    function createReqAllSelects(header){
        const selectElements = document.querySelectorAll('select');
        const selectData = {};
        selectElements.forEach(select => {
            const selectId = select.id;
            const selectedOption = select.value;
            selectData[selectId] = selectedOption;
        });
        selectData['plato'] = platoFieldRef.current;
        selectData['place'] = placeFieldRef.current;
        const req = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                [header]: header
            },
            body: JSON.stringify(selectData),
        };
        return req;
    }

    function AddVyvodyToLvmPost(){
        const req = createReqAllSelects('addvyvodytolvm');
        postService(req,runtimeMode+'/maintenance/binderinst')
        wipeCreatedElements();
    }
    function UnbindVyvodBindCorrectPost(){
        const req = createReqAllSelects('addvyvodytolvmunbind');
        postService(req,runtimeMode+'/maintenance/binderinst')
        wipeCreatedElements();
    }

    function prevazatPlatoDTSPost(){
        const req = createReqAllSelects('prevazatPlatoDTS');
        postService(req,runtimeMode+'/maintenance/binderinst')
        wipeCreatedElements();
    }
    function navazatPlatoDtsPost(){
        const req = createReqAllSelects('navazatPlatoDts');
        postService(req,runtimeMode+'/maintenance/binderinst')
        wipeCreatedElements();
    }


    function createPrevazbitPost() {
        const selectElements = document.querySelectorAll('select');
        const selectData = {};
        selectElements.forEach(select => {
            const selectId = select.id;
            const selectedOption = select.value;
            selectData[selectId] = selectedOption;
        });
        const req = {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'prevazbit': 'prevazbit'
            },
            body: JSON.stringify(selectData),
        };
        postService(req,runtimeMode+'/maintenance/binderinst')
    }

    function analyzeAction(){
        // reset the elements on page
        createdElements.forEach(created =>{
            const element = document.getElementById(created);
            if (element) {
                element.parentNode.removeChild(element);
            }
        })
        createdElements = [];
        const req = {
            method: 'POST',
            headers: { 'Content-Type': 'application/json',
                        'analyze':'analyze' },
            body: JSON.stringify({
                plato: platoField,
                place: placeField,
            }),
        };
        postService(req,runtimeMode+'/maintenance/binderinst')
    }
    
    return (
            <div style={{display:"block", width:"100%",marginTop:50}}>
                {/* firstcolumn */}
                <div style={{display:"inline-block", width:"30%"}}>
                    <label>plato ckod:</label><input name="platockod" id="platockod" type="text" onInput={(e) => setPlatoField(e.target.value)} />
                    <label htmlFor="vypsatlvm"></label>
                    {/* <input onClick={createVypsatLVMPost} class="submit" type="submit" style={{width:"175px",marginBottom:"20px"}} id="vypsatlvm" name="vypsatlvm" value="Vypsat vazby LVM"/> */}
                    
                    <label>trafostanice ckod:</label><input name="placeckod" id="placeckod" type="text" field="*{placeckodField}" onInput={(e) => setPlaceField(e.target.value)} value={placeField}/>
                    <label htmlFor="vypsatDTS"></label>
                    {/* <input onClick={createVypsatDTSPost} class="submit" type="submit" style={{width:"175px"}} id="vypsatDTS" name="vypsatDTS" value="Vypsat vazby DTS"/> */}


                    <label htmlFor="prevazbit"></label>
                    {/* <input onClick={createPrevazbitPost} class="submit" type="submit" style={{width:"175px"}} id="prevazbit" name="prevazbit" value="Zapsat změny"/> */}
                    <input onClick={analyzeAction} className="submit" type="submit" style={{width:"175px",marginBottom:"20px"}} id="nacist" name="nacist" value="Nacist"/>
                </div>
                {/* secondcolumn */}
                    {/* <div id="logpart" style={{width:"50%", float: "left", display: "inline-block"}}> */}
                    {/* <div id="logpart" style={{width:"50%",float: "right",position:"fixed",top:100,right:0, display: "inline-block",maxHeight: "400px", overflowY: "auto"}}> */}
                    <div id="logpart" style={{width:"70%",float: "right",top:0, display: "inline-block", overflowY: "auto"}}>
                        <ul id="binderinstDisplay" style={{listStyleType:"none"}}></ul>
                        <ul id="actions" style={{listStyleType:"none"}}></ul>
                    </div>
            </div>
    );
}
export default Binderinst;