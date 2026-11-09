import { useEffect, useRef, useState } from "react";

/** Use browser fullscreen, retaining a viewport fallback for unsupported hosts. */
export function useAnalystFullscreen() {
  const ref = useRef<HTMLElement>(null);
  const [fullscreen, setFullscreen] = useState(false);
  const [fallback, setFallback] = useState(false);
  useEffect(() => {
    const changed = () => setFullscreen(document.fullscreenElement === ref.current);
    document.addEventListener("fullscreenchange", changed);
    return () => document.removeEventListener("fullscreenchange", changed);
  }, []);
  useEffect(() => {
    if (!fallback) return;
    const prior = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const escape = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !ref.current?.querySelector('[role="dialog"]')) { setFallback(false); setFullscreen(false); }
    };
    window.addEventListener("keydown", escape);
    return () => { document.body.style.overflow = prior; window.removeEventListener("keydown", escape); };
  }, [fallback]);
  async function toggle() {
    if (fallback) { setFallback(false); setFullscreen(false); return; }
    try {
      if (document.fullscreenElement === ref.current) await document.exitFullscreen();
      else if (ref.current?.requestFullscreen) await ref.current.requestFullscreen();
      else { setFallback(true); setFullscreen(true); }
    } catch { setFallback(true); setFullscreen(true); }
  }
  async function exit() {
    setFallback(false); setFullscreen(false);
    if (document.fullscreenElement === ref.current) await document.exitFullscreen();
  }
  useEffect(() => {
    const node = ref.current;
    return () => { if (document.fullscreenElement === node) void document.exitFullscreen().catch(() => {}); };
  }, []);
  return { ref, fullscreen, toggle, exit, className: fallback ? "analyst-pane-fullscreen" : "" };
}
