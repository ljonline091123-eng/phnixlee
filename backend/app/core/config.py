from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


DEFAULT_DATABASE_URL = f"sqlite:///{(Path(__file__).resolve().parents[2] / 'quant.db').as_posix()}"
DEFAULT_ENV_FILE = Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):
    app_name: str = "Gemini Quant Agent"
    app_env: str = "development"
    api_v1_prefix: str = "/api/v1"
    database_url: str = DEFAULT_DATABASE_URL
    sql_echo: bool = False
    seed_defaults_on_startup: bool = True
    data_foundation_enabled: bool = True
    company_research_context_enabled: bool = True
    model_credential_key: str = ""
    lake_storage_backend: str = "filesystem"
    lake_filesystem_root: str = str(Path(__file__).resolve().parents[2] / "lake")
    lake_s3_endpoint: str = "127.0.0.1:9000"
    lake_s3_access_key: str = "minioadmin"
    lake_s3_secret_key: str = "minioadmin"
    lake_s3_bucket: str = "quant-lake"
    lake_s3_secure: bool = False

    model_config = SettingsConfigDict(
        env_file=DEFAULT_ENV_FILE,
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
