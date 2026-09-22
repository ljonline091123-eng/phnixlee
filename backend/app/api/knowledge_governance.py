from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.schemas.knowledge_governance import KnowledgeGovernanceRequest
from app.schemas.resource_hub import GovernanceRunRead
from app.services.knowledge_governance import governance_catalog, govern_knowledge_sources


router = APIRouter(prefix="/resources/knowledge-governance", tags=["Knowledge governance"])


@router.get("/catalog")
def catalog(db: Session = Depends(get_db)) -> dict:
    return governance_catalog(db)


@router.post("/run", response_model=GovernanceRunRead)
def run(request: KnowledgeGovernanceRequest, db: Session = Depends(get_db)):
    try:
        return govern_knowledge_sources(db, request)
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc
