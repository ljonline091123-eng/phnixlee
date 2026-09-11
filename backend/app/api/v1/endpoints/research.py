"""Expose the research router at the conventional v1 endpoint path."""

from app.api.research import analyze_stock, router

__all__ = ["analyze_stock", "router"]
