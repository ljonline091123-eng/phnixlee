from app.models.lakehouse import DocumentChunkVersion
from app.services.graph_rag import _chunk_payload


def test_chunk_without_embedding_can_be_retrieved_and_hash_is_not_semantic():
    chunk = DocumentChunkVersion(id="test", document_id="1", chunk_index=0, chunk_version="v1",
        document_key="doc:1", content_hash="hash", chunk_text="原文证据", parser_version="v1",
        start_offset=0, end_offset=4, embedding_kind="NONE", metadata_json={"embedding": None})
    assert _chunk_payload(chunk)["embedding_status"] == "MISSING"
    chunk.embedding_model = "HASH_LOCAL"
    chunk.embedding_kind = "HASH"
    chunk.metadata_json = {"embedding": {"vector": [0.1, 0.2]}}
    assert _chunk_payload(chunk)["embedding_status"] == "HASH_ONLY"
    chunk.embedding_model = "real-provider"
    chunk.embedding_kind = "SEMANTIC"
    assert _chunk_payload(chunk)["embedding_status"] == "AVAILABLE"
    chunk.metadata_json = {"embedding": None}
    assert _chunk_payload(chunk)["embedding_status"] == "MISSING"
