import { useState } from "react";
import { LuEye, LuEyeOff } from "react-icons/lu";
import "./PasswordInput.css";

export default function PasswordInput({ value, onChange }) {
  const [show, setShow] = useState(false);

  return (
    <div className="password-field">
      <input
        type={show ? "text" : "password"}
        name="password"
        value={value}
        onChange={onChange}
        placeholder="Password"
        className="user-form-input"
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
  );
}
