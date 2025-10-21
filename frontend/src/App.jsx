import React from "react";
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
import NotFound from "./components/NotFound";

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

  const activeTab = getActiveTab();
  const isSignUpPage = location.pathname === "/sign-up";
  const isLoginPage = location.pathname === "/login";

  return (
    <div className="App">
      {!isSignUpPage && !isLoginPage && (
        <header className="app-header">
          <nav className="tab-nav">
            <button
              className={activeTab === "image-generator" ? "active" : ""}
              onClick={() => navigate("/home/image-generator")}
            >
              Generate Food Image
            </button>
            <button
              className={activeTab === "food-chat" ? "active" : ""}
              onClick={() => navigate("/home/chat")}
            >
              Talk About Food
            </button>
            <button
              className={activeTab === "recipe-generator" ? "active" : ""}
              onClick={() => navigate("/home/recipe-generator")}
            >
              Generate a Recipe
            </button>
          </nav>

          <div className="auth-links">
            <Link to="/login" className="auth-link login-link">
              <FaUtensils className="icon-inline" /> Log In
            </Link>
            <Link to="/sign-up" className="auth-link signup-link">
              <GiCookingPot className="icon-inline" /> Sign Up
            </Link>
          </div>
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
          <Route path="/home/recipe-generator" element={<RecipeGenerator />} />
          <Route path="/sign-up" element={<SignUpPage />}></Route>
          <Route path="/login" element={<LoginPage />}></Route>
          <Route path="*" element={<NotFound />}></Route>
        </Routes>
      </div>
    </div>
  );
}

export default App;
