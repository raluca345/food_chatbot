import React, { useState } from "react";
import Spinner from "../commons/Spinner";

function FoodImageGenerator() {
  const [params, setParams] = useState({
    name: "",
    style: "vivid",
    size: "",
    course: "",
    ingredients: "",
    dishType: "",
  });

  const handleChange = (e) => {
    setParams({ ...params, [e.target.name]: e.target.value });
  };

  const [imageUrls, setImageUrls] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleGenerateImage = async () => {
    window.scrollTo({ top: 0, behavior: "smooth" });

    setError("");
    setLoading(true);
    try {
      const query = new URLSearchParams(params);
      const response = await fetch(
        `http://localhost:8080/api/v1/food-images?${query.toString()}`,
        { method: "POST" }
      );
      if (!response.ok) {
        throw new Error("Network response was not ok");
      }
      const url = await response.text();
      setImageUrls([url]);
    } catch (err) {
      console.error("Error generating image: ", err);
      setError("You can only generate pictures of food!");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="tab-content">
      {error && <div className="error-message">{error}</div>}
      {loading && <Spinner />}

      <div className="image-form-grid">
        <div className="controls">
          <div className="form-row">
            <label>Name</label>
            <input
              name="name"
              type="text"
              value={params.name}
              onChange={handleChange}
            />
          </div>
          <div className="form-row">
            <label>Style</label>
            <select name="style" value={params.style} onChange={handleChange}>
              <option value="vivid">Vivid</option>
              <option value="natural">Natural</option>
            </select>
          </div>
          <div className="form-row">
            <label>Size</label>
            <select name="size" value={params.size} onChange={handleChange}>
              <option value="1024x1024">1024x1024</option>
              <option value="1024x1792">1024x1792</option>
              <option value="1792x1024">1792x1024</option>
            </select>
          </div>
          <div className="form-row">
            <label>Course</label>
            <input
              name="course"
              type="text"
              value={params.course}
              onChange={handleChange}
            />
          </div>
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
            <label>Dish Type</label>
            <input
              name="dishType"
              type="text"
              value={params.dishType}
              onChange={handleChange}
            />
          </div>

          <div className="form-actions">
            <button className="primary" onClick={handleGenerateImage}>
              Generate Image
            </button>
          </div>
        </div>

        <div className="single-image">
          {imageUrls.length > 0 ? (
            <img src={imageUrls[0]} alt="Generated food" />
          ) : (
            <div className="empty-image-slot" />
          )}
        </div>
      </div>
    </div>
  );
}

export default FoodImageGenerator;
