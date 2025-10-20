import React, { useState } from "react";
import Spinner from "../commons/Spinner";
import ReactMarkdown from "react-markdown";
import {
  rehypePlugins,
  markdownComponents,
} from "../../utils/sanitizeMarkdown";
import "./RecipeGenerator.css";

function RecipeGenerator() {
  const [params, setParams] = useState({
    ingredients: "",
    cuisine: "",
    dietaryRestrictions: "",
  });

  const [recipe, setRecipe] = useState("");
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setParams({ ...params, [e.target.name]: e.target.value });
  };

  const handleGenerateRecipe = async () => {
    setLoading(true);
    const query = new URLSearchParams(params);

    const response = await fetch(
      `http://localhost:8080/api/v1/recipes?${query.toString()}`,
      { method: "POST" }
    );

    const recipeTxt = await response.text();
    setRecipe(recipeTxt);
    setLoading(false);
  };

  return (
    <div className="tab-content">
      <h2>Create a recipe</h2>
      <div className="form-row">
        <label>Ingredients</label>
        <input
          name="ingredients"
          type="text"
          value={params.ingredients}
          onChange={handleChange}
        />
      </div>
      <div className="form-row">
        <label>Cuisine</label>
        <input
          name="cuisine"
          type="text"
          value={params.cuisine}
          onChange={handleChange}
        />
      </div>
      <div className="form-row">
        <label>Dietary Restrictions</label>
        <input
          name="dietaryRestrictions"
          type="text"
          value={params.dietaryRestrictions}
          onChange={handleChange}
        />
      </div>
      <button onClick={handleGenerateRecipe}>Generate Recipe</button>
      {loading && <Spinner />}
      <div className="output">
        <div className="recipe-text">
          <ReactMarkdown
            rehypePlugins={rehypePlugins}
            components={markdownComponents}
          >
            {recipe}
          </ReactMarkdown>
        </div>
      </div>
    </div>
  );
}

export default RecipeGenerator;
