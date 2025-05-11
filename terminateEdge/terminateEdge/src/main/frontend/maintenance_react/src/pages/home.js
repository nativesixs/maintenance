import React from "react";
import "../stylePage.css";

function Home() {
    const gitCommitId = process.env.REACT_APP_GIT_COMMIT_ID;
    const gitCommitTime = process.env.REACT_APP_GIT_COMMIT_TIME;
    const gitBranch = process.env.REACT_APP_GIT_BRANCH;
    const version = process.env.REACT_APP_VERSION;

    return (
        <div>
            <h2>TerminateEdge</h2>
            <p>
                Adresa: <a href="/maintenance/terminateEdge">/terminateEdge</a>
            </p>
            <p>Po zadání ckodu plata smaže vazby mezi platem a k němu přivazbenými dpu, lvm a umístění.</p>

            <h3>Mody</h3>
            <ul>
                <li>vypsat vazby - Vypíše všechny aktivní vazby k danému platu.</li>
                <li>vypsat odvazbovací requesty bez odeslání - Vypíše sestavené requesty k odvazbení všeho a neodešle je.</li>
                <li>odvazbit vše - Nalezne všechna zařízení a umístění plata a smaže vazby, vazby na SIM kartu zůstanou zachovány.</li>
            </ul>
            <ul>
                <li>Plato -> DTS, LVM, DPU, AHS, ZDROJ</li>
                <li>DTS (navázané na zadané plato) -> AHS, ZDROJ</li>
                <li>DPU (navázané na zadané plato) -> DTS, LVM</li>
                <li>LVM (navázané na zadané plato) -> Sek.Vývod</li>
            </ul>
            <p>odvazbit zařízení - Nalezne všechna zařízení a smaže vazby, vazby na SIM kartu a umístění plata zůstanou zachovány.</p>
            <ul>
                <li>Plato -> LVM, DPU, AHS, ZDROJ</li>
                <li>DPU (navázané na zadané plato) -> LVM</li>
            </ul>
            <p>odvazbit umístění - Nalezne všechna umístění plata a smaže vazby, vazby na SIM kartu a připojená zařízení zůstanou zachovány.</p>
            <ul>
                <li>Plato -> DTS</li>
                <li>DTS (navázané na zadané plato) -> AHS, ZDROJ</li>
                <li>DPU (navázané na zadané plato) -> DTS</li>
                <li>LVM (navázané na zadané plato) -> Sek.Vývod</li>
            </ul>

            <h2>IPSet Switcher</h2>
            <p>
                Adresa: <a href="/maintenance/ipsetswitch">/ipsetswitch</a>
            </p>
            <p>Slouží k převazbování aktivních IPSet tunelů nebo APN z testu na produkci a opačně, lze zadat přímo tel. číslo SIM, nebo ckod DPU na kterém je SIM umístěna.</p>

            <h3>Mody</h3>
            <ul>
                <li>vypsat aktivní - Vypíše aktuálně navázaný IPSet tunel a APN.</li>
                <li>vypsat assigned vazby - Vypíše vazby typu assigned pro dané DPU/SIM.</li>
                <li>přepnout IPSet/APN na prod - Převazbí aktuálně aktivní tunel / APN z testu na produkci, pokud již vazba na produkci existuje, převazbení neproběhne.</li>
                <li>přepnout IPSet/APN na test - Převazbí aktuálně aktivní tunel / APN z produkce na test, pokud již vazba na testu existuje, převazbení neproběhne.</li>
            </ul>

            <h2>DPU Binder</h2>
            <p>Adresa: <a href="/maintenance/dpubind">/dpubind</a></p>
            <p>Vytvoří vazby mezi komponentami: LVM, plato, DPU, vývod, umístění, SIM, AHS, zdroj.</p>

            <h2>Inventory Import</h2>
            <p>Adresa: <a href="/maintenance/inventoryimport">/inventoryimport</a></p>
            <p>Umožňuje zadat odeslat příkazy na inventory/load.</p>
            <h3>Mody</h3>
            <ul>
                <li>Odeslat - Odešle všechny zadané příkazy.</li>
                <li>Analyze - Vypíše počet nalezených příkazů v zadaném textu (CREATE_INSTANCE, CREATE_EDGE etc..)</li>
                <li>Odeslat ve várce - Odešle zadané příkazy po várkách maximální velikosti dle zadané 'Request size' (0 = vše).</li>
            </ul>
            <p>Příkazy je třeba zadávat ve formátu: <br /> &lt;příkaz&gt;: <br /> {`{tělo}`}</p>
            <p>např: <br/>CREATE_EDGE: <br/>{`{...}`}</p>
            <p>formát CREATE_EDGE: {`{...}`} není podporován</p>
            <p>Složené příkazy jako:<br/>
            CREATE_EDGE:<br/>
            {`{...}`}<br/>
            {`{...}`}<br/>
            ...<br/>
            se počítají jako jeden příkaz,přestože tento zápis provádí více operací.
            </p>



            <h2>Duplicate Edges</h2>
            <p>Adresa: <a href="/maintenance/duplicateEdges">/duplicateEdges</a></p>
            <p>Najde vazby nad povolený limit pro určitý typ mezi zařízeními. Obsahuje nástroje pro mazání jednotlivých/všech nalezených vazeb.</p>

            <h3>Mody</h3>
            <h4>Mode</h4>
            <ul>
                <li>zkontrolovat ckod - Nalezne vazby vedoucí od/k zadanému ckodu a vypíše vazby které porušily povolený počet vazeb daného typu na jedno zařízení, kontroluje ckod jednoho zařízení (např. dpu) nebo ckod jednoho umístění (např. pozice trafa).</li>
                <li>zkontrolovat navázaná zařízení - Nalezne všechna zařízení připojená k zadanému ckodu, a zařízení připojená k takto nalezeným zařízením (př. při zadání ckodu plata nalezne také připojené lvm,dpu,sim), po nalezení všech zařízení postupuje stejně jako předchozí mod pro každé nalezené zařízení.</li>
                <li>zkontrolovat navázaná umístění - Identický s modem pro kontrolu navázaných zařízení - místo zařízení kontroluje umístění (př. umístění trafa, kobky).</li>
                <li>zkontrolovat zařízení tohoto typu na inventory - Nalezne všechna zařízení na inventory stejného typu jako zadaný ckod a postupuje jako první mod pro zařízení.</li>
            </ul>

            <h4>Log</h4>
            <ul>
                <li>zobrazit pouze vazby mimo limit - Částečné zobrazení logu. Zobrazují se pouze "vadné" vazby.</li>
                <li>zobrazit kompletní log - Zobrazí i vazby v povoleném limitu.</li>
            </ul>

            <h4>Odvazbit</h4>
            <ul>
                <li>smazat všechny duplicitní vazby - Smaže všechny nalezené vazby nad povolený limit.</li>
                <li>ponechat jednu nejnovější vazbu - Smaže všechny nalezené vazby nad povolený limit kromě nejnovější přidané.</li>
                <li>ponechat jednu nejstarší vazbu - Smaže všechny nalezené vazby nad povolený limit kromě nejstarší přidané.</li>
            </ul>

            <h2>Allign Attributes</h2>
            <p>Adresa: <a href="/maintenance/attributeallign">/attributeallign</a></p>
            <p>Najde, vypíše, aktualizuje nebo přidá atributy a jejich hodnoty pro zařízení typu dpu.</p>

            <h3>Mody</h3>
            <h4>Mode</h4>
            <ul>
                <li>vypsat atributy - vypíše atributy a jejich hodnoty (dle Log nastavení) pro dpu se zadaným ckodem / všechna dpu na inventory (dle nastavení Rozsahu).</li>
                <li>přidat chybějící atributy - zkontroluje přítomnost did pro dpu se zadaným ckodem / všechna dpu na inventory (dle nastavení Rozsahu), pokud v diossetu nalezne did které není dpu přiřazeno tak jej inicializuje na základní hodnotu "didValue"</li>
                <li>aktualizovat nalezené atributy - zkontroluje hodnoty did pro dpu se zadaným ckodem / všechna dpu na inventory (dle nastavení Rozsahu), v případě že jsou hodnoty oproti diossetu zastarále je aktualizuje.</li>
            </ul>
            <h4>Log</h4>
            <ul>
                <li>vypsat atributy a jejich hodnoty - zkontroluje aktuálnost hodnot atributů dpu, výsledky vypíše</li>
                <li>vypsat pouze atributy - vypíše pouze atributy vztahující se k danému dpu</li>
                <li>vypsat pouze chybějící atributy - vypíše pouze atributy které u daného dpu nebyly nalezeny</li>
            </ul>

            <h4>Rozsah</h4>
            <ul>
                <li>pouze pro tohle zařízení - provede zvolenou akci pouze pro dpu se zadaným ckodem</li>
                <li>pro všechna zařízení na inventory - provede zvolenou akci pro všechna dpu na inventory.</li>
            </ul>

            <h2>BinderINST</h2>
            <p>Adresa: <a href="/maintenance/binderinst">/binderinst</a></p>
            <p>Simuluje stav po úspěšném instalačním testu</p>
            <p>Vypíše LVM vázané na plato a jejich vývody, vypíše vývody vázané k DTS která je vázaná k zadanému platu, a umožní tyto vývody navazbit k LVM nebo odvazbit stávající vývod LVM</p>
            <p>Pokud plato nemá navazbenou DTS, lze zadat do pole "trafostanice ckod", po navázání jsou zobrazeny LVM navázané na plato a vývody nově navázané DTS pro navázání na LVM.</p>

            <h1>Version:</h1>
            <p style={{ fontFamily: "'Lucida Console', monospace", fontWeight: "bold", margin: "1%" }}>version: {version}</p>
            <p style={{ fontFamily: "'Lucida Console', monospace", fontWeight: "bold", margin: "1%" }}>git commit id: {gitCommitId}</p>
            <p style={{ fontFamily: "'Lucida Console', monospace", fontWeight: "bold", margin: "1%" }}>git version commit time: {gitCommitTime}</p>
            <p style={{ fontFamily: "'Lucida Console', monospace", fontWeight: "bold", margin: "1%" }}>git branch: {gitBranch}</p>
        </div>
    );
}

export default Home;
