import React, { useState } from "react";
import Spinner from "./Spinner";

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

  const handleGenerateImage = async () => {
    setLoading(true);
    try {
      const query = new URLSearchParams(params);
      const response = await fetch(
        `http://localhost:8080/api/v1/food-images?${query.toString()}`,
        { method: "POST" }
      );
      const url = await response.text();
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
