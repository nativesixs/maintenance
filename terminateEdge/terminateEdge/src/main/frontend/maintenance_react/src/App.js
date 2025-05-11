import React from 'react'
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom'
import Home from "./pages/home"
import Navbar from "./Navbar/index"
import TerminateEdge from './pages/terminateEdge'
import Ipsetswitch from './pages/ipsetswitch'
import Dpubind from './pages/dpubind'
import Inventoryimport from './pages/inventoryimport'
import DuplicateEdges from './pages/duplicateEdges'
import AttributeAllign from './pages/attributeallign'
import Ipsetipfix from './pages/ipsetipfix'
import Binderinst from './pages/binderinst'
import Didswitcher from './pages/didswitcher'

function App() {
    return(
        <Router>
          <Navbar/>
            <Routes>
              <Route exact path="/maintenance/" element={<Home />} />
              <Route exact path="/maintenance/home" element={<Home />} />
              <Route exact path="/maintenance/terminateEdge" element={<TerminateEdge />} />
              <Route exact path="/maintenance/ipsetswitch" element={<Ipsetswitch />} />
              <Route exact path="/maintenance/dpubind" element={<Dpubind />} />
              <Route exact path="/maintenance/inventoryimport" element={<Inventoryimport />} />
              <Route exact path="/maintenance/duplicateEdges" element={<DuplicateEdges />} />
              <Route exact path="/maintenance/attributeallign" element={<AttributeAllign />} />
              <Route exact path="/maintenance/ipsetipfix" element={<Ipsetipfix />} />
              <Route exact path="/maintenance/binderinst" element={<Binderinst />} />
              <Route exact path="/maintenance/didswitcher" element={<Didswitcher />} />
            </Routes>
      </Router>
    );
}
export default App;