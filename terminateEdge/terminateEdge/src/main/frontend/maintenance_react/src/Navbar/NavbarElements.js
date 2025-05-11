import { NavLink as Link } from "react-router-dom";
import styled from "styled-components";
 
export const Nav = styled.nav`
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-start;
    height: 40px;
    width: 100%;
    align-items: center;
    background-color: rgb(63, 81, 181);
    //hex for background color: #3f51b5
    box-shadow: rgba(0, 0, 0, 0.2) 0px 3px 1px -2px,rgba(0, 0, 0, 0.14) 0px 2px 2px 0px, rgba(0, 0, 0, 0.12) 0px 1px 5px 0px;
`;
 
export const NavLink = styled(Link)`
    color: #fff;
    display: flex;
    align-items: center;
    text-decoration: none;
    padding: 0 1rem;
    height: 100%;
    cursor: pointer;
    &.active {
        color: #89CFF0;
    }
    &:hover {
        color: #DDE9F5;
    }
`;
 
export const NavLogo = styled.a`
    margin-right: 2rem;
    align-items: flex-start;
    justify-content: center;
    display: flex;
    margin-left: 2rem;

    img {
        height: 30px;
        overflow-clip-margin: content-box;
        overflow: clip;
        display: block;
    }

    @media screen and (max-width: 960px) {
        height: 80px;
        img {
          width: 80px;
        }
      }
`;

export const Bars = styled(Link)`
    display: none;
    color: #808080;
    @media screen and (max-width: 768px) {
        display: block;
        position: absolute;
        top: 0;
        right: 0;
        transform: translate(-100%, 75%);
        font-size: 1.8rem;
        cursor: pointer;
    }
`;
 
export const NavMenu = styled.div`
    display: flex;
    align-items: flex-start;
    margin-right: 2rem;

    @media screen and (max-width: 960px) {
    display: none;
    }
`;
 
export const NavBtn = styled.nav`
    display: flex;
    align-items: center;
    margin-right: 24px;
    /* Third Nav */
    /* justify-content: flex-end;
  width: 100vw; */
    @media screen and (max-width: 768px) {
        display: none;
    }
`;
 
export const NavBtnLink = styled(Link)`
    border-radius: 4px;
    background: #808080;
    padding: 10px 22px;
    color: #000000;
    outline: none;
    border: none;
    cursor: pointer;
    transition: all 0.2s ease-in-out;
    text-decoration: none;
    /* Second Nav */
    margin-left: 24px;
    &:hover {
        transition: all 0.2s ease-in-out;
        background: #fff;
        color: #808080;
    }
`;