"""Queryable table descriptions for SQLite and other database clients."""

from sqlalchemy import JSON, String, Text
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base


class DatabaseTableComment(Base):
    __tablename__ = "database_table_comment"

    table_name: Mapped[str] = mapped_column(String(128), primary_key=True)
    display_name: Mapped[str] = mapped_column(String(128), nullable=False)
    domain: Mapped[str] = mapped_column(String(64), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    column_comments_json: Mapped[dict[str, str]] = mapped_column(JSON, nullable=False, default=dict)
