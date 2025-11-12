import React, { useState, useEffect } from "react";
import {
  Routes,
  Route,
  useNavigate,
  useLocation,
  Link,
} from "react-router-dom";
import "./App.css";
import FoodImageGenerator from "./components/home/FoodImageGenerator";
import FoodChat from "./components/home/FoodChat";
import RecipeGenerator from "./components/home/RecipeGenerator";
import SignUpPage from "./components/auth/SignUpPage";
import LoginPage from "./components/auth/LoginPage";
import { FaUtensils } from "react-icons/fa";
import { GiCookingPot } from "react-icons/gi";
import UserDropdown from "./components/UserMenu/UserDropdown";
import NotFound from "./components/NotFound";
import Sidebar from "./components/Sidebar/Sidebar";
import { getEmailFromToken } from "./utils/jwt";
import Tabs from "./components/Tabs/Tabs";

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

    return "food-chat";
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
  const token = localStorage.getItem("token");
  const isLoggedIn = Boolean(token);
  const userEmail = token ? getEmailFromToken(token) : null;

  return (
    <div className="App">
      <article>
        <Sidebar isOpen={isSideBarOpen} onToggle={toggleSidebar} />
        <section>
          {!isSignUpPage && !isLoginPage && (
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
            <Routes>
              <Route path="/" element={<FoodChat />} />
              <Route path="/home" element={<FoodChat />} />
              <Route
                path="/home/image-generator"
                element={<FoodImageGenerator />}
              />
              <Route path="/home/chat" element={<FoodChat />} />
              <Route
                path="/home/recipe-generator"
                element={<RecipeGenerator />}
              />
              <Route path="/sign-up" element={<SignUpPage />}></Route>
              <Route path="/login" element={<LoginPage />}></Route>
              <Route path="*" element={<NotFound />}></Route>
            </Routes>
          </div>
        </section>
      </article>
    </div>
  );
}

export default App;
