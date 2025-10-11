import React, { useState } from "react";
import "./App.css";

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

        <div>
          {activeTab === "food-image-generator" && (
            <h2>Food Image Generator</h2>
          )}
          {activeTab === "food-chat" && <h2>Hello, User</h2>}
          {activeTab === "recipe-generator" && <h2>Recipe Generator</h2>}
        </div>
      </div>
    </>
  );
}

export default App;
