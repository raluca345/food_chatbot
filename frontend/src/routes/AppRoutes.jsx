import React from "react";
import { Routes, Route } from "react-router-dom";
import FoodImageGenerator from "../components/home/FoodImageGenerator";
import FoodChat from "../components/home/FoodChat";
import RecipeGenerator from "../components/home/RecipeGenerator";
import SignUpPage from "../components/auth/SignUpPage";
import LoginPage from "../components/auth/LoginPage";
import NotFound from "../components/NotFound";
import RecipeHistory from "../components/side-menu/RecipeHistory";
import Gallery from "../components/side-menu/Gallery";
import ForgotPassword from "../components/auth/ForgotPassword";

export default function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<FoodChat />} />
      <Route path="/home" element={<FoodChat />} />
      <Route path="/home/image-generator" element={<FoodImageGenerator />} />
      <Route path="/home/chat" element={<FoodChat />} />
      <Route path="/home/recipe-generator" element={<RecipeGenerator />} />
      <Route path="/sign-up" element={<SignUpPage />}></Route>
      <Route path="/login" element={<LoginPage />}></Route>
      <Route path="/forgot" element={<ForgotPassword />}></Route>
      <Route path="/me/recipe-history" element={<RecipeHistory />}></Route>
      <Route path="me/gallery" element={<Gallery />}></Route>
      <Route path="*" element={<NotFound />}></Route>
    </Routes>
  );
}
