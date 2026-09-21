import { Database } from "lucide-react";

export function EnvironmentBanner() {
  const configuredLabel = String(import.meta.env.VITE_DATA_ENV_LABEL || "当前连接数据库");
  const label = configuredLabel.replace(/\s*[·•]\s*\d+\s*证券验收.*$/, "");
  const endpoint = String(import.meta.env.VITE_API_BASE_URL || "http://127.0.0.1:8000/api/v1");
  return <div className="data-environment-banner" role="status"><Database size={15} /><strong>{label}</strong><span>{endpoint}</span></div>;
}
