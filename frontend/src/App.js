import './App.css';
import Search from './components/search/Search.tsx'
import React from "react";
import {Link, Outlet} from "react-router-dom";

function App() {
  return (
    <div className="outer-container">
      <div className="nav-bar-container">
        <div className="nav-bar-logo">
          Zuchtprogramm
        </div>
        <div className="nav-bar-search">
          <Search/>
        </div>
      </div>
      <div className="side-bar-container">
        <div className="side-bar-section side-bar-section-top">
          <Link to={`search`} className="side-bar-element side-bar-element-top">Erweiterte
            Suche</Link>
          <div className="side-bar-element side-bar-element-top">Meine Hunde
          </div>
          <div className="side-bar-element side-bar-element-top">Erfassen</div>
          <div className="side-bar-element side-bar-element-top">Statistik</div>
        </div>
        <div className="side-bar-section side-bar-section-bottom">
          <div className="side-bar-element side-bar-element-bottom">Abmelden
          </div>
          <div className="side-bar-element side-bar-element-bottom">Account
            Einstellungen
          </div>
          <div className="side-bar-element side-bar-element-bottom">Programm
            Einstellungen
          </div>
        </div>
      </div>
      <div className="content-container">
        <Outlet/>
      </div>
    </div>
  );
}

export default App;
