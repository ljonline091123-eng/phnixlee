"""Add new governance resources without rewriting existing user configuration."""

from pathlib import Path

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.ai_hub import AgentDefinition, AgentSkillLink, AgentDataSourceLink, ModelSkill
from app.models.market_data import DataInterface, DataSource
from app.services.skill_registry import content_hash, ensure_current_revision


SKILLS = (
    ("COMPANY_MASTER_RESOLVER", "公司证券身份治理", "Resolve issuer identities using source IDs and registration identifiers; never merge on names alone."),
    ("COMPANY_DISCLOSURE_ARCHIVER", "公司与司法原文归档", "Archive source responses and legal disclosure pages with provenance; preserve failed and partial coverage."),
    ("COMPANY_RELATION_NORMALIZER", "股权与公司关系治理", "Normalize disclosed ownership, industry and security classifications as evidence-backed observations; no inferred control."),
    ("COMPANY_COVERAGE_AUDITOR", "股票公司覆盖审计", "Audit stock/company mappings, evidence, source status, candidates and known gaps in a bounded batch."),
)
SOURCE_DEFINITIONS = (
    ("TENCENT_MARKET_CAP", "腾讯 A 股市值快照", "MARKET_DATA", "COMPANY_MARKET_CAP", True,
     "https://qt.gtimg.cn/", ["MARKET_CAP", "SIZE_CLASSIFICATION"],
     "供应商按 A 股价格及总股本计算的市值快照；A/H 不相加。大小盘采用明确人民币绝对阈值，仅快照当日有效；港股暂不支持。"),
    ("EASTMONEY_COMPANY_PROFILE", "东方财富公司资料与工商字段", "COMPANY_DATA", "COMPANY_PUBLIC", True,
     "https://datacenter.eastmoney.com/", ["COMPANY_PROFILE", "INDUSTRY", "DISCLOSED_HOLDERS"],
     "公开聚合公司资料，不等于工商登记机关核验；来源报告期与获取时间分别保留。"),
    ("CNINFO_LEGAL_DISCLOSURES", "巨潮公司司法事项正式披露", "JUDICIAL_DISCLOSURE", "COMPANY_DISCLOSURE", True,
     "https://www.cninfo.com.cn/", ["LEGAL_DISCLOSURE", "PDF_TEXT"],
     "上市公司司法披露正文；不是完整法院案件库，披露公司不必然是涉案当事人。"),
    ("CNINFO_BUSINESS_DISCLOSURES", "巨潮公司经营合同正式披露", "BUSINESS_DISCLOSURE", "COMPANY_DISCLOSURE", True,
     "https://www.cninfo.com.cn/", ["BUSINESS_DISCLOSURE", "PDF_TEXT"],
     "经营、合同和担保公告原文；匿名子公司和汇总金额须进一步主体解析，不自动推断母公司直接交易。"),
    ("CNINFO_SUPPLY_CHAIN_DISCLOSURES", "巨潮公司供应链及年报披露", "SUPPLY_CHAIN_DISCLOSURE", "COMPANY_DISCLOSURE", True,
     "https://www.cninfo.com.cn/", ["SUPPLY_CHAIN_DISCLOSURE", "PDF_TEXT"],
     "供应商、客户、供应链、关联交易及年度报告原文线索；每词限前30条、正文限100页。匿名前五名不解析为具体企业，不自动确认供销关系。"),
    ("GSXT_REGISTRATION", "国家企业信用信息公示系统（待授权接入）", "COMPANY_REGISTRY", "OFFICIAL_REFERENCE", False,
     "https://www.gsxt.gov.cn/", ["REFERENCE"],
     "需许可查询或授权数据产品；尚未配置批量查询授权，不以聚合字段冒充官方登记结果。"),
    ("COURT_JUDICIAL", "法院司法公开数据（待授权接入）", "JUDICIAL_REFERENCE", "OFFICIAL_REFERENCE", False,
     "https://wenshu.court.gov.cn/", ["REFERENCE"],
     "法院案件及执行查询需按访问和授权条件接入；缺少授权不表示企业无诉讼。"),
)


def register_company_resources(db: Session) -> dict:
    """Insert missing codes only. Caller owns the transaction."""
    source_ids = []
    for code, name, category, adapter, enabled, url, capabilities, description in SOURCE_DEFINITIONS:
        source = db.scalar(select(DataSource).where(DataSource.source_code == code))
        if source is None:
            source = DataSource(source_code=code, source_name=name, source_type=category,
                adapter_type=adapter, enabled=enabled, priority=350,
                config_json={"official_url": url, "capabilities": capabilities,
                    "provider_kind": "PUBLIC_DISCLOSURE" if enabled else "OFFICIAL_PENDING",
                    "authorization_status": "PUBLIC" if enabled else "AUTH_REQUIRED",
                    "governance_workflow": "company_graph_governance"},
                description=description)
            db.add(source)
            db.flush()
        source_ids.append(source.id)
        methods = (
            [("COMPANY_PROFILE", "公司工商资料", "fetch_company_profile"), ("DISCLOSED_HOLDERS", "披露股东", "fetch_shareholders")]
            if code == "EASTMONEY_COMPANY_PROFILE" else
            [("LEGAL_DISCLOSURE", "司法披露及原文", "fetch_legal_disclosures")]
            if code == "CNINFO_LEGAL_DISCLOSURES" else
            [("BUSINESS_DISCLOSURE", "经营披露及原文", "fetch_business_disclosures")]
            if code == "CNINFO_BUSINESS_DISCLOSURES" else
            [("SUPPLY_CHAIN_DISCLOSURE", "供应链及年报原文", "fetch_supply_chain_disclosures")]
            if code == "CNINFO_SUPPLY_CHAIN_DISCLOSURES" else
            [("MARKET_CAP", "A 股市值快照", "fetch_market_cap")]
            if code == "TENCENT_MARKET_CAP" else []
        )
        for interface_code, title, method in methods:
            existing = db.scalar(select(DataInterface).where(DataInterface.source_id == source.id, DataInterface.interface_code == interface_code))
            if existing is None:
                db.add(DataInterface(source_id=source.id, interface_code=interface_code,
                    interface_name=title, data_category=category, adapter_method=method,
                    supported_markets=["CN_A", "HK"] if code == "EASTMONEY_COMPANY_PROFILE" else ["CN_A"],
                    input_schema={"type": "object", "required": ["market", "symbol"]},
                    output_schema={"type": "object", "required": ["status", "records", "warnings"]},
                    description=description, enabled=True))
    agent = db.scalar(select(AgentDefinition).where(AgentDefinition.agent_code == "COMPANY_GRAPH_GOVERNOR"))
    if agent is None:
        agent = AgentDefinition(agent_code="COMPANY_GRAPH_GOVERNOR", display_name="股票公司关系治理",
            system_prompt="Run bounded company_graph_governance stages. Preserve original stock data. Require identity, source evidence and time. Model output remains candidate-only. Report authorization and coverage gaps.",
            description="Executable workflow: company_graph_governance. Deterministic source normalization and audited publication; model chat alone does not execute ingestion.",
            version="1.0.0", enabled=True)
        db.add(agent)
        db.flush()
    skill_root = Path(__file__).resolve().parents[2] / "skill_docs"
    skill_ids = []
    for code, name, instructions in SKILLS:
        skill = db.scalar(select(ModelSkill).where(ModelSkill.skill_code == code))
        if skill is None:
            path = skill_root / f"{code}.SKILL.md"
            content = path.read_text(encoding="utf-8") if path.exists() else instructions
            skill = ModelSkill(skill_code=code, skill_name=name, description=instructions,
                instructions=content, skill_type="PROMPT_SOP", is_builtin=False,
                file_path=f"skill_docs/{code}.SKILL.md" if path.exists() else None,
                content_hash=content_hash(content), config_json={"workflow": "company_graph_governance", "stage": code},
                version="1.0.0", enabled=True)
            db.add(skill)
            db.flush()
            ensure_current_revision(db, skill)
        skill_ids.append(skill.id)
        if db.scalar(select(AgentSkillLink).where(AgentSkillLink.agent_id == agent.id, AgentSkillLink.skill_id == skill.id)) is None:
            db.add(AgentSkillLink(agent_id=agent.id, skill_id=skill.id))
    for source_id in source_ids:
        if db.scalar(select(AgentDataSourceLink).where(AgentDataSourceLink.agent_id == agent.id, AgentDataSourceLink.data_source_id == source_id)) is None:
            db.add(AgentDataSourceLink(agent_id=agent.id, data_source_id=source_id))
    db.flush()
    return {"source_ids": source_ids, "agent_id": agent.id, "skill_ids": skill_ids}
