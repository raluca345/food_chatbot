import React, { useState } from "react";
import "./App.css";
import FoodImageGenerator from "./components/FoodImageGenerator";
import FoodChat from "./components/FoodChat";
import RecipeGenerator from "./components/RecipeGenerator";

function App() {
  const [activeTab, setActiveTab] = useState("food-image-generator");

  const handleTabChange = (tab) => {
    setActiveTab(tab);
  };

  return (
    <>
      <div className="App">
        <button
          className={activeTab === "food-image-generator" ? "active" : ""}
          onClick={() => handleTabChange("food-image-generator")}
        >
          Generate Food Image
        </button>
        <button
          className={activeTab === "food-chat" ? "active" : ""}
          onClick={() => handleTabChange("food-chat")}
        >
          Talk About Food
        </button>
        <button
          className={activeTab === "recipe-generator" ? "active" : ""}
          onClick={() => handleTabChange("recipe-generator")}
        >
          Generate a Recipe
        </button>

        <div className="food-tabs">
          {activeTab === "food-image-generator" && <FoodImageGenerator />}
          {activeTab === "food-chat" && <FoodChat />}
          {activeTab === "recipe-generator" && <RecipeGenerator />}
        </div>
      </div>
    </>
  );
}

export default App;
