from __future__ import annotations

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.models.decision_review import SkillEvaluationCase, SkillEvaluationResult, SkillEvaluationRun
from app.schemas.skill_evaluation import SkillEvaluationCaseCreate, SkillEvaluationRunCreate
from app.services import skill_evaluation


router = APIRouter(prefix="/skill-evaluations", tags=["Skill evaluation"])


@router.get("/cases")
def list_cases(task_type: str | None = None, enabled: bool = True, limit: int = 200,
               db: Session = Depends(get_db)):
    query = select(SkillEvaluationCase).where(SkillEvaluationCase.enabled.is_(enabled)).order_by(SkillEvaluationCase.id.desc()).limit(min(max(limit, 1), 500))
    if task_type:
        query = query.where(SkillEvaluationCase.task_type == task_type)
    return list(db.scalars(query).all())


@router.post("/cases", status_code=201)
def create_case(payload: SkillEvaluationCaseCreate, db: Session = Depends(get_db)):
    try:
        return skill_evaluation.create_case(db, **payload.model_dump())
    except ValueError as exc:
        raise HTTPException(status_code=409, detail=str(exc)) from exc


@router.post("/runs", status_code=201)
def run(payload: SkillEvaluationRunCreate, db: Session = Depends(get_db)):
    try:
        return skill_evaluation.run_evaluation(db, **payload.model_dump())
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@router.get("/runs")
def list_runs(skill_code: str | None = None, limit: int = 100, db: Session = Depends(get_db)):
    query = select(SkillEvaluationRun).order_by(SkillEvaluationRun.started_at.desc()).limit(min(max(limit, 1), 500))
    if skill_code:
        query = query.where(SkillEvaluationRun.skill_code == skill_code)
    return list(db.scalars(query).all())


@router.get("/runs/{run_id}")
def get_run(run_id: int, db: Session = Depends(get_db)):
    row = db.get(SkillEvaluationRun, run_id)
    if row is None:
        raise HTTPException(status_code=404, detail="evaluation run not found")
    return {"run": row, "results": list(db.scalars(select(SkillEvaluationResult).where(
        SkillEvaluationResult.run_id == run_id).order_by(SkillEvaluationResult.id)).all())}


@router.post("/runs/{run_id}/repair-draft")
def repair_draft(run_id: int, db: Session = Depends(get_db)):
    try:
        draft = skill_evaluation.propose_repair_draft(db, run_id)
    except LookupError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    if draft is None:
        raise HTTPException(status_code=409, detail="该评测没有可生成的待审核修复草案，或当前没有可用的非模拟模型")
    return draft

