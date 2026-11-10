import { useEffect, useRef, useState } from "react";

/** Use browser fullscreen, retaining a viewport fallback for unsupported hosts. */
export function useAnalystFullscreen<T extends HTMLElement = HTMLElement>() {
  const ref = useRef<T>(null);
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
    return () => { document.body.style.overflow = prior; };
  }, [fallback]);
  useEffect(() => {
    if (!fullscreen) return;
    const escape = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      if (fallback) {
        if (ref.current?.querySelector('[role="dialog"]')) return;
        setFallback(false);
        setFullscreen(false);
        return;
      }
      if (document.fullscreenElement === ref.current) void document.exitFullscreen().catch(() => {});
    };
    window.addEventListener("keydown", escape, true);
    return () => window.removeEventListener("keydown", escape, true);
  }, [fallback, fullscreen]);
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
