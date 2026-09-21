from functools import lru_cache
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict


DEFAULT_DATABASE_URL = f"sqlite:///{(Path(__file__).resolve().parents[2] / 'quant.db').as_posix()}"


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

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    return Settings()
