from typing import Any
from pydantic import BaseModel, Field

class DatasetExportRequest(BaseModel):
    dataset_code: str = Field(min_length=2, max_length=128)
    dataset_name: str = Field(min_length=1, max_length=256)
    layer: str = Field(pattern=r"^(RAW|NORMALIZED|SERVING)$")
    source_table: str = Field(min_length=2, max_length=128)
    limit: int = Field(default=10000, ge=1, le=100000)


class DatasetQualityRequest(BaseModel):
    source_table: str = Field(min_length=2, max_length=128)
    layer: str = Field(default="NORMALIZED", pattern=r"^(RAW|NORMALIZED|SERVING)$")
    limit: int = Field(default=10000, ge=1, le=100000)

class DocumentChunkRequest(BaseModel):
    document_key: str = Field(min_length=1, max_length=512)
    document_id: str | None = None
    text: str = Field(min_length=1, max_length=2_000_000)
    chunk_size: int = Field(default=1800, ge=300, le=6000)
    overlap: int = Field(default=180, ge=0, le=1000)
    parser_version: str = Field(default="STRUCTURE_V1", max_length=64)
    embedding_model: str | None = None
    archive_original: bool = True


class DocumentArchiveRequest(BaseModel):
    limit: int = Field(default=100, ge=1, le=1000)
    chunk_size: int = Field(default=1800, ge=300, le=6000)
    overlap: int = Field(default=180, ge=0, le=1000)
    parser_version: str = Field(default="STRUCTURE_V1", max_length=64)
