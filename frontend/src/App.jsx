import React, { useState, useEffect } from "react";
import { useNavigate, useLocation, Link } from "react-router-dom";
import "./App.css";
import { FaUtensils } from "react-icons/fa";
import { GiCookingPot } from "react-icons/gi";
import UserDropdown from "./components/UserMenu/UserDropdown";
import Sidebar from "./components/side-menu/Sidebar";
import { getEmailFromToken } from "./utils/jwt";
import Tabs from "./components/Tabs/Tabs";
import AppRoutes from "./routes/AppRoutes";
import { getToken } from "./api/homeApi";

function App() {
  const navigate = useNavigate();
  const location = useLocation();

  const getActiveTab = () => {
    if (location.pathname === "/sign-up") return "sign-up";

    if (
      location.pathname === "/" ||
      location.pathname === "/home" ||
      location.pathname === "/home/food-chat"
    )
      return "food-chat";

    if (location.pathname === "/home/image-generator") return "image-generator";

    if (location.pathname === "/home/recipe-generator")
      return "recipe-generator";

    return null;
  };

  const [isSideBarOpen, setIsSideBarOpen] = useState(false);

  const toggleSidebar = () => setIsSideBarOpen((s) => !s);

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/home");
    window.location.reload();
  };

  const activeTab = getActiveTab();
  const isSignUpPage = location.pathname === "/sign-up";
  const isLoginPage = location.pathname === "/login";
  const isForgotPage = location.pathname === "/forgot";
  const isResetPage = location.pathname === "/reset-password";
  const token = getToken();
  const isLoggedIn = Boolean(token);
  const userEmail = token ? getEmailFromToken(token) : null;

  return (
    <div className="App">
      <article>
        {!isForgotPage && !isLoginPage && !isSignUpPage && !isResetPage && isLoggedIn && (
          <Sidebar isOpen={isSideBarOpen} onToggle={toggleSidebar} />
        )}
        <section>
          {!isSignUpPage && !isLoginPage && !isForgotPage && !isResetPage && (
            <header className="app-header">
              <Tabs />

              {!isLoggedIn && (
                <div className="auth-links">
                  <Link to="/login" className="auth-link login-link">
                    <FaUtensils className="icon-inline" /> Log In
                  </Link>
                  <Link to="/sign-up" className="auth-link signup-link">
                    <GiCookingPot className="icon-inline" /> Sign Up
                  </Link>
                </div>
              )}
              {isLoggedIn && (
                <UserDropdown token={token} onLogout={handleLogout} />
              )}
            </header>
          )}

          <div className="food-tabs">
            <AppRoutes />
          </div>
        </section>
      </article>
    </div>
  );
}

export default App;
