import React from "react";
import { Nav, NavLink, NavMenu } from "./NavbarElements";
 
const Navbar = () => {
    return (
        <>
            <Nav>
                <NavMenu>
                    <NavLink to="/home" activeStyle>
                        home
                    </NavLink>
                    <NavLink to="/test" activeStyle>
                        test
                    </NavLink>
                </NavMenu>
            </Nav>
        </>
    );
};
 
export default Navbar;