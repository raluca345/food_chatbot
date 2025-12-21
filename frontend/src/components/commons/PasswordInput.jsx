import { useState } from "react";
import { LuEye, LuEyeOff } from "react-icons/lu";
import "./PasswordInput.css";

export default function PasswordInput({
  value,
  onChange,
  name = "password",
  placeholder = "",
  minLength,
  showValidation = false,
  externalError = "",
  errorId,
  onBlur: parentOnBlur,
  onFocus: parentOnFocus,
}) {
  const [show, setShow] = useState(false);
  const [touched, setTouched] = useState(false);

  const tooShort = minLength && value && value.length < minLength;
  const internalInvalid = tooShort && (touched || showValidation);

  const hasExternalError = Boolean(externalError);

  const isInvalid = internalInvalid || hasExternalError;

  const handleBlur = (e) => {
    setTouched(true);
    if (typeof parentOnBlur === "function") parentOnBlur(e);
  };

  const handleFocus = (e) => {
    if (typeof parentOnFocus === "function") parentOnFocus(e);
  };

  const descId = errorId || (name ? `${name}-error` : undefined);

  return (
    <div className="password-wrapper">
      {isInvalid && (
        <div id={descId} className="password-error">
          {hasExternalError
            ? externalError
            : `Password must be at least ${minLength} characters long.`}
        </div>
      )}

      <div className={`password-field ${isInvalid ? "invalid" : ""}`}>
        <input
          type={show ? "text" : "password"}
          name={name}
          value={value}
          onChange={onChange}
          onBlur={handleBlur}
          onFocus={handleFocus}
          placeholder={placeholder}
          className="user-form-input"
          aria-invalid={isInvalid}
          {...(minLength ? { minLength } : {})}
          {...(isInvalid && descId ? { "aria-describedby": descId } : {})}
        />

        <span
          className="password-toggle-icon"
          onClick={() => setShow((prev) => !prev)}
          onKeyDown={(e) => {
            if (e.key === "Enter" || e.key === " ") {
              e.preventDefault();
              setShow((prev) => !prev);
            }
          }}
          role="button"
          tabIndex={0}
          aria-label={show ? "Hide password" : "Show password"}
        >
          {show ? <LuEyeOff size={20} /> : <LuEye size={20} />}
        </span>
      </div>
    </div>
  );
}
