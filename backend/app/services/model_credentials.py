"""Provider credentials are encrypted at rest with an operator supplied Fernet key."""

from __future__ import annotations

import os

from cryptography.fernet import Fernet, InvalidToken

from app.core.config import get_settings


def _cipher() -> Fernet:
    key = (os.environ.get("MODEL_CREDENTIAL_KEY") or get_settings().model_credential_key).strip()
    if not key:
        raise ValueError("Set MODEL_CREDENTIAL_KEY before saving or using a model API key")
    try:
        return Fernet(key.encode("ascii"))
    except (ValueError, UnicodeEncodeError) as exc:
        raise ValueError("MODEL_CREDENTIAL_KEY must be a valid Fernet key") from exc


def encrypt_api_key(value: str) -> str:
    return _cipher().encrypt(value.encode("utf-8")).decode("ascii")


def decrypt_api_key(value: str) -> str:
    try:
        return _cipher().decrypt(value.encode("ascii")).decode("utf-8")
    except InvalidToken as exc:
        raise ValueError("MODEL_CREDENTIAL_KEY cannot decrypt the stored provider key") from exc
