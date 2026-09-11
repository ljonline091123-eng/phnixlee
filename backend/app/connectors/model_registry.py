from app.connectors.model_adapters import GeminiRestAdapter, MockModelAdapter, ModelAdapter, OpenAICompatAdapter

_MODEL_ADAPTERS: dict[str, type[ModelAdapter]] = {
    "MOCK": MockModelAdapter,
    "OPENAI_COMPAT": OpenAICompatAdapter,
    "DEEPSEEK": OpenAICompatAdapter,
    "GEMINI_REST": GeminiRestAdapter,
}


def get_model_adapter(provider_type: str) -> ModelAdapter:
    adapter_class = _MODEL_ADAPTERS.get(provider_type.upper())
    if not adapter_class:
        supported = ", ".join(sorted(_MODEL_ADAPTERS))
        raise ValueError(f"Unsupported model provider type '{provider_type}'. Supported providers: {supported}")
    return adapter_class()
