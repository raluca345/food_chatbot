import React from "react";
import "./Gallery.css";
import { LuDownload } from "react-icons/lu";
import { FaTrashCan } from "react-icons/fa6";
import { IoClose } from "react-icons/io5";
import Spinner from "../commons/Spinner";
import PaginationControls from "../commons/PaginationControls";
import ConfirmModal from "../commons/ConfirmModal";
import useGallery from "../../hooks/useGallery";

export default function Gallery() {
  const {
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
  } = useGallery();

  return (
    <>
      {loading ? (
        <div className="image-grid image-grid--loading">
          <Spinner />
        </div>
      ) : error ? (
        <div className="image-grid image-grid--error">{error}</div>
      ) : userImages.length === 0 ? (
        <div className="image-grid image-grid-empty">No images yet.</div>
      ) : (
        <div className="image-grid">
          {userImages.map((image, index) => (
            <div
              className="image-slot"
              key={image.id ?? `${page}-${index}`}
              onClick={() => openModal(image)}
            >
              <img
                src={image.thumbnailUrl}
                alt={image.alt ?? `Generated ${index}`}
                loading="lazy"
                width="300"
                height="300"
                decoding="async"
              />

              <div className="image-actions">
                <button
                  className="image-action-btn image-action-btn-download"
                  aria-label="Download image"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleDownload(image);
                  }}
                >
                  <LuDownload />
                </button>
                {deletingIds.includes(image.id) ? (
                  <Spinner className="image-action-spinner" />
                ) : (
                  <button
                    className="image-action-btn image-action-btn-danger"
                    aria-label="Delete image"
                    onClick={(e) => {
                      e.stopPropagation();
                      const img = userImages[index] ?? image;
                      setPendingDeleteImage(img);
                    }}
                  >
                    <FaTrashCan />
                  </button>
                )}
              </div>
            </div>
          ))}
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
            onClick={(e) => e.stopPropagation()}
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
