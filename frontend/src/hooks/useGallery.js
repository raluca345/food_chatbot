import { useState, useEffect, useRef, useCallback } from "react";
import {
  getUserImages,
  deleteImage,
  downloadImage,
} from "../api/galleryApi";

const PAGE_SIZE = 18;

export default function useGallery() {
  const [userImages, setUserImages] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(null);
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [deletingIds, setDeletingIds] = useState([]);
  const [pendingDeleteImage, setPendingDeleteImage] = useState(null);

  const isMounted = useRef(true);
  const skipFetchRef = useRef(false);

  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);

  const fetchImages = useCallback(async (pageToFetch = page) => {
    setError("");
    if (isMounted.current) setLoading(true);
    try {
      const res = await getUserImages({
        page: pageToFetch,
        pageSize: PAGE_SIZE,
      });
      let items = res?.items ?? [];
      const tot = Number(res?.total) || 0;
      if (isMounted.current) {
        setUserImages(items);
        setTotal(tot);
      }
      return { items, total: tot };
    } catch (err) {
      console.error(err);
      if (isMounted.current) {
        setError(err?.userMessage || "Failed to fetch images.");
        setTotal(0);
      }
    } finally {
      if (isMounted.current) setLoading(false);
    }
    return { items: [], total: 0 };
  }, []);

  useEffect(() => {
    if (skipFetchRef.current) {
      skipFetchRef.current = false;
      return;
    }
    fetchImages(page);
  }, [page, fetchImages]);

  useEffect(() => {
    const totalPages = Math.max(1, Math.ceil((total || 0) / PAGE_SIZE));
    if (page > totalPages) setPage(totalPages);
  }, [total, page]);

  const handleDownload = async (image) => {
    if (!image?.id) return;

    try {
      const { blob, filename } = await downloadImage(image.id);

      const url = window.URL.createObjectURL(blob);

      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      a.style.display = "none";

      document.body.appendChild(a);
      a.click();
      a.remove();

      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error("Download error:", err);
      setError(
        err?.userMessage || "Failed to download the image. Please try again.",
      );
    }
  };

  const handleDelete = async (image) => {
    if (!image || !image.id) return;
    const id = image.id;
    const prevImages = userImages;
    const prevTotal = total;

    setUserImages((cur) => cur.filter((i) => i.id !== id));
    setTotal((t) => Math.max(0, t - 1));
    setDeletingIds((s) => [...s, id]);

    try {
      const ok = await deleteImage(id);
      setDeletingIds((s) => s.filter((x) => x !== id));

      const newTotal = Math.max(0, prevTotal - 1);
      const newTotalPages = Math.max(1, Math.ceil(newTotal / PAGE_SIZE));

      if (page > newTotalPages) {
        const result = await fetchImages(newTotalPages);
        if (isMounted.current) {
          setUserImages(result.items);
          setTotal(Number(result.total) || newTotal);
        }
        skipFetchRef.current = true;
        if (isMounted.current) setPage(newTotalPages);
      } else {
        const result = await fetchImages(page);
        if (isMounted.current) {
          setUserImages(result.items);
          setTotal(Number(result.total) || newTotal);
        }
      }
    } catch (err) {
      console.error("Image delete error:", err);
      setError(
        err?.userMessage || "Failed to delete the image. Please try again.",
      );
      setUserImages(prevImages);
      setTotal(prevTotal);
      setDeletingIds((s) => s.filter((x) => x !== id));
    }
  };

  const openModal = (image) => {
    setSelectedImage(image);
    document.body.style.overflow = "hidden";
  };

  const closeModal = () => {
    setSelectedImage(null);
    document.body.style.overflow = "auto";
  };

  useEffect(() => {
    if (!selectedImage) return;
    const handleKeyDown = (e) => {
      if (e.key === "Escape") closeModal();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [selectedImage]);

  return {
    userImages,
    error,
    loading,
    selectedImage,
    page,
    total,
    PAGE_SIZE,
    deletingIds,
    pendingDeleteImage,
    setPage,
    setPendingDeleteImage,
    handleDownload,
    handleDelete,
    openModal,
    closeModal,
  };
}
