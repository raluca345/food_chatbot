import React, { useState } from "react";
import Spinner from "./Spinner";

function FoodImageGenerator() {
  const [name, setName] = useState("");
  const [style, setStyle] = useState("vivid");
  const [size, setSize] = useState("");
  const [course, setCourse] = useState("");
  const [ingredients, setIngredients] = useState("");
  const [dishType, setDishType] = useState("");
  const [imageUrls, setImageUrls] = useState([]);

  const [loading, setLoading] = useState(false);

  const handleGenerateImage = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams({
        name,
        style,
        size,
        course,
        ingredients,
        dishType,
      });

      const response = await fetch(
        `http://localhost:8080/api/v1/food-images?${params.toString()}`,
        {
          method: "POST",
        }
      );

      const url = await response.text();
      console.log("Generated image URL: ", url);
      setImageUrls([url]);
    } catch (err) {
      console.error("Error generating image: ", err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="tab-content">
      <div className="form-row">
        <label>Name</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label>Style</label>
        <select value={style} onChange={(e) => setStyle(e.target.value)}>
          <option value="vivid">Vivid</option>
          <option value="natural">Natural</option>
        </select>
      </div>
      <div className="form-row">
        <label>Size</label>
        <select value={size} onChange={(e) => setSize(e.target.value)}>
          <option value="1024x1024">1024x1024</option>
          <option value="1024x1792">1024x1792</option>
          <option value="1792x1024">1792x1024</option>
        </select>
      </div>
      <div className="form-row">
        <label>Course</label>
        <input
          type="text"
          value={course}
          onChange={(e) => setCourse(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label>Ingredients</label>
        <input
          type="text"
          value={ingredients}
          onChange={(e) => setIngredients(e.target.value)}
        />
      </div>
      <div className="form-row">
        <label>Dish Type</label>
        <input
          type="text"
          value={dishType}
          onChange={(e) => setDishType(e.target.value)}
        />
      </div>
      <button onClick={handleGenerateImage}>Generate Image</button>
      {loading && <Spinner />}
      <div className="image-grid">
        {imageUrls.map((url, index) => (
          <img key={index} src={url} alt={`Generated food ${index}`} />
        ))}
        {[...Array(4 - imageUrls.length)].map((_, index) => (
          <div
            key={index + imageUrls.length}
            className="empty-image-slot"
          ></div>
        ))}
      </div>
    </div>
  );
}

export default FoodImageGenerator;
