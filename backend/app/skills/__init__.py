"""Reusable, database-backed tools for the research agents.

The functions in this package deliberately return Pydantic models instead of
unstructured dictionaries.  That makes them useful from an HTTP endpoint,
from a LangChain/LangGraph adapter, or from a plain Python workflow.
"""

from app.skills.capital_tech import (
    CapitalAndTechInput,
    CapitalAndTechOutput,
    get_capital_and_tech_skill,
)
from app.skills.financial import (
    FinancialAnalysisInput,
    FinancialAnalysisOutput,
    get_financial_analysis_skill,
)
from app.skills.news_rag import (
    RecentNewsRagInput,
    RecentNewsRagOutput,
    get_recent_news_rag_skill,
)

__all__ = [
    "CapitalAndTechInput",
    "CapitalAndTechOutput",
    "FinancialAnalysisInput",
    "FinancialAnalysisOutput",
    "RecentNewsRagInput",
    "RecentNewsRagOutput",
    "get_capital_and_tech_skill",
    "get_financial_analysis_skill",
    "get_recent_news_rag_skill",
]
