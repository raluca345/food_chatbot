import React, { useEffect, useState, useRef } from "react";
import "./Gallery.css";
import { LuDownload } from "react-icons/lu";
import { FaTrashCan } from "react-icons/fa6";
import { getUserImages, deleteImage, downloadImage } from "../../api/homeApi";
import { IoClose } from "react-icons/io5";
import Spinner from "../commons/Spinner";
import PaginationControls from "../commons/PaginationControls";
import ConfirmModal from "../commons/ConfirmModal";

export default function Gallery() {
  const [userImages, setUserImages] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);

  const [selectedImage, setSelectedImage] = useState(null);
  const [page, setPage] = useState(1);
  const PAGE_SIZE = 18;
  const [total, setTotal] = useState(0);
  const [deletingIds, setDeletingIds] = useState([]);
  const isMounted = useRef(true);
  const [pendingDeleteImage, setPendingDeleteImage] = useState(null);
  const skipFetchRef = useRef(false);

  useEffect(() => {
    isMounted.current = true;
    return () => {
      isMounted.current = false;
    };
  }, []);

  const fetchImages = async (pageToFetch = page) => {
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
  };

  useEffect(() => {
    if (skipFetchRef.current) {
      skipFetchRef.current = false;
      return;
    }
    fetchImages(page);
  }, [page]);

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
      setError(err?.userMessage || "Failed to download the image. Please try again.");
    }
  };

  const handleDelete = async (image) => {
    if (!image || !image.id) return;
    const id = image.id;
    const prevImages = userImages;
    const prevTotal = total;

    // optimistic update
    setUserImages((cur) => cur.filter((i) => i.id !== id));
    setTotal((t) => Math.max(0, t - 1));
    setDeletingIds((s) => [...s, id]);

    try {
      const ok = await deleteImage(id);
      setDeletingIds((s) => s.filter((x) => x !== id));

      const newTotal = Math.max(0, prevTotal - 1);
      const newTotalPages = Math.max(1, Math.ceil(newTotal / PAGE_SIZE));

      if (page > newTotalPages) {
        // fetch the last valid page and then set page while skipping the automatic fetch
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
      setError(err?.userMessage || "Failed to delete the image. Please try again.");
      // revert
      setUserImages(prevImages);
      setTotal(prevTotal);
      setDeletingIds((s) => s.filter((x) => x !== id));
    }
  };

  const openModal = (image) => {
    setSelectedImage(image);
    document.body.style.overflow = "hidden"; // disable scroll
  };

  const closeModal = () => {
    setSelectedImage(null);
    document.body.style.overflow = "auto"; // restore scroll
  };

  return (
    <>
      {loading ? (
        <div className="image-grid image-grid--loading">
          <Spinner />
        </div>
      ) : error ? (
        <div className="image-grid image-grid--error">{error}</div>
      ) : userImages.length === 0 ? (
        <div className="image-grid image-grid--empty">No images yet.</div>
      ) : (
        <div className="image-grid">
          {(() => {
            return userImages.map((image, index) => (
              <div
                className="image-slot"
                key={image.id ?? `${page}-${index}`}
                onClick={() => openModal(image)}
              >
                <img
                  src={image.url}
                  alt={image.alt ?? `Generated ${index}`}
                  loading="lazy"
                />

                <div className="image-actions">
                  <LuDownload
                    className="image-action-btn image-action-btn-download"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDownload(image);
                    }}
                  />
                  {deletingIds.includes(image.id) ? (
                    <Spinner className="image-action-spinner" />
                  ) : (
                    <FaTrashCan
                      className="image-action-btn image-action-btn-danger"
                      onClick={(e) => {
                        e.stopPropagation();
                        const img = userImages[index] ?? image;
                        setPendingDeleteImage(img);
                      }}
                    />
                  )}
                </div>
              </div>
            ));
          })()}
        </div>
      )}

      {!loading && !error && userImages.length > 0 && (
        <div className="gallery-pagination">
          <PaginationControls
            page={page}
            pageSize={PAGE_SIZE}
            total={total}
            onPageChange={(p) => setPage(p)}
            disabled={loading}
          />
        </div>
      )}

      {selectedImage && (
        <div className="modal-overlay" onClick={closeModal}>
          <div
            className="modal-content"
            onClick={(e) => e.stopPropagation()} // prevent closing when clicking image
          >
            <img src={selectedImage.url} alt="Fullscreen" />
            <button className="modal-close" onClick={closeModal}>
              <IoClose size={32} />
            </button>
          </div>
        </div>
      )}

      <ConfirmModal
        open={!!pendingDeleteImage}
        title="Delete image"
        message={
          pendingDeleteImage
            ? "Are you sure you want to delete this image?"
            : ""
        }
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
        confirmDisabled={
          pendingDeleteImage
            ? deletingIds.includes(pendingDeleteImage.id)
            : false
        }
        onCancel={() => setPendingDeleteImage(null)}
        onConfirm={() => {
          const img = pendingDeleteImage;
          if (img) {
            setPendingDeleteImage(null);
            handleDelete(img);
          }
        }}
      />
    </>
  );
}
