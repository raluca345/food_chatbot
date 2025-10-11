import React, { useState } from "react";
import "./App.css";

function App() {
  const [activeTab, setActiveTab] = useState("food-image-generator");

  const handleTabChange = (tab) => {
    console.log(tab);
    setActiveTab(tab);
  };

  return (
    <>
      <div>
        <button onClick={() => handleTabChange("food-pic-generator")}>
          Generate Food Picture
        </button>
        <button onClick={() => handleTabChange("food-chat")}>
          Talk About Food
        </button>
        <button onClick={() => handleTabChange("recipe-generator")}>
          Generate a Recipe
        </button>
      </div>
    </>
  );
}

export default App;
