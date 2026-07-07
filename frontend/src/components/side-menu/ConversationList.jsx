import "./ConversationList.css";
import Spinner from "../commons/Spinner";
import { useNavigate } from "react-router-dom";
import { FaEllipsis } from "react-icons/fa6";
import ContextMenu from "../context-menu/ContextMenu";
import useConversations from "../../hooks/useConversations";

export default function ConversationList({ isOpen }) {
  const navigate = useNavigate();

  const {
    conversations,
    isLoading,
    isLoadingMore,
    savingId,
    openMenuId,
    menuAnchor,
    editingId,
    selectedId,
    inputRef,
    editingTitleRef,
    hasMore,
    setOpenMenuId,
    setMenuAnchor,
    startRename,
    cancelRename,
    saveRename,
    handleDelete,
    handleLoadMore,
  } = useConversations();

  return (
    <>
      {isLoading && <Spinner />}

      {isOpen && (
        <div className="conversation-list-wrap">
          <div className="conversation-list-scroll">
            <ul className="conversation-list">
              {conversations.map((c) => {
                const id = c.conversationId;
                const isSelected = String(id) === String(selectedId);
                const isEditing = String(editingId) === String(id);

                return (
                  <li key={id}>
                    <div
                      className={[
                        "conv-item",
                        isSelected ? "selected" : "",
                        isEditing ? "editing" : "",
                      ].join(" ")}
                    >
                      <button
                        className="conv-link"
                        onClick={() =>
                          !isEditing && navigate(`/home/chat/${id}`)
                        }
                      >
                        <span
                          ref={isEditing ? inputRef : null}
                          className="conv-title"
                          contentEditable={isEditing}
                          suppressContentEditableWarning
                          spellCheck={false}
                          onInput={(e) => {
                            editingTitleRef.current = e.currentTarget.textContent;
                          }}
                          onKeyDown={(e) => {
                            if (e.key === "Enter") {
                              e.preventDefault();
                              saveRename(id);
                            }
                            if (e.key === "Escape") {
                              e.preventDefault();
                              cancelRename();
                            }
                          }}
                          onBlur={() => isEditing && saveRename(id)}
                        >
                          {c.title}
                        </span>
                      </button>

                      {!isEditing && (
                        <button
                          className="conv-ellipsis"
                          onClick={(e) => {
                            e.stopPropagation();
                            setMenuAnchor(
                              e.currentTarget.getBoundingClientRect(),
                            );
                            setOpenMenuId(openMenuId === id ? null : id);
                          }}
                        >
                          <FaEllipsis className="inline-icon menu" />
                        </button>
                      )}
                    </div>
                  </li>
                );
              })}

              {hasMore && (
                <li className="conv-load-more-item">
                  <button
                    className="conv-load-more"
                    onClick={handleLoadMore}
                    disabled={isLoading || isLoadingMore}
                  >
                    {isLoadingMore ? (
                      <>
                        <span
                          className="conv-mini-spinner"
                          aria-hidden="true"
                        ></span>
                        <span>Loading more</span>
                      </>
                    ) : (
                      "Load more"
                    )}
                  </button>
                </li>
              )}
            </ul>
          </div>
        </div>
      )}

      <ContextMenu
        open={!!openMenuId}
        anchorRect={menuAnchor}
        onClose={() => setOpenMenuId(null)}
      >
        <button
          className="context-menu-item rename"
          onClick={() => {
            const convo = conversations.find(
              (c) => String(c.conversationId) === String(openMenuId),
            );
            if (convo) startRename(openMenuId, convo.title);
          }}
        >
          Rename
        </button>

        <button
          className="context-menu-item delete"
          onClick={() => handleDelete(openMenuId)}
          disabled={savingId === openMenuId}
        >
          {savingId === openMenuId ? "Deleting…" : "Delete"}
        </button>
      </ContextMenu>
    </>
  );
}
