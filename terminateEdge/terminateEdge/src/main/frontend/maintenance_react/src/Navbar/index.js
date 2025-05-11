import React from "react";
import {
    Nav,
    NavLink,
    NavLogo,
} from "./NavbarElements";


const Navbar = () => {
    const handleReload = (e, path) => {
        e.preventDefault();
        window.location.href = path;
    };
    
    return (
        <>
            <Nav>
                <NavLogo onClick={(e) => handleReload(e, "/")}>
                    <img src="data:image/png;base64,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" alt="logottc" />
                </NavLogo>
                <NavLink to="/maintenance/home" onClick={(e) => handleReload(e, "/maintenance/home")} >
                    Home
                </NavLink>
                <NavLink to="/maintenance/terminateEdge" onClick={(e) => handleReload(e, "/maintenance/terminateEdge")}>
                    Terminate Edges
                </NavLink>
                <NavLink to="/maintenance/ipsetswitch" onClick={(e) => handleReload(e, "/maintenance/ipsetswitch")}>
                    IPSet Switcher
                </NavLink>
                <NavLink to="/maintenance/dpubind" onClick={(e) => handleReload(e, "/maintenance/dpubind")}>
                    DPU Binder
                </NavLink>
                <NavLink to="/maintenance/inventoryimport" onClick={(e) => handleReload(e, "/maintenance/inventoryimport")}>
                    Inventory Import
                </NavLink>
                <NavLink to="/maintenance/duplicateEdges" onClick={(e) => handleReload(e, "/maintenance/duplicateEdges")}>
                    DuplicateEdges
                </NavLink>
                <NavLink to="/maintenance/attributeallign" onClick={(e) => handleReload(e, "/maintenance/attributeallign")}>
                    Allign Attributes
                </NavLink>
                <NavLink to="/maintenance/binderinst" onClick={(e) => handleReload(e, "/maintenance/binderinst")}>
                    BinderINST
                </NavLink>
                <NavLink to="/maintenance/ipsetipfix" onClick={(e) => handleReload(e, "/maintenance/ipsetipfix")}>
                    ipsetipfix
                </NavLink>
                <NavLink to="/maintenance/didswitcher" onClick={(e) => handleReload(e, "/maintenance/didswitcher")}>
                    didswitcher
                </NavLink>
            </Nav>
        </>
    );
};
 
export default Navbar;