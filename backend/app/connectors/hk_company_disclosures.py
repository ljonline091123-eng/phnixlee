"""Issuer-specific HK public shareholder disclosures; never infer control."""
from datetime import datetime, timezone
import hashlib
import re

from bs4 import BeautifulSoup
import httpx


SOURCE = "经济通公司资料（公开披露转录）"


def parse_etnet_disclosures(html: str, symbol: str, source_url: str) -> dict:
    soup = BeautifulSoup(html, "html.parser")
    header = soup.find(id="QuoteNameA")
    identity = re.match(r"^(\d{5})(?:\s|$)", header.get_text(" ", strip=True) if header else "")
    if identity is None or identity.group(1) != symbol:
        raise ValueError("经济通页面证券身份不匹配，未接受股东数据")
    rows, errors = [], []
    label = soup.find(string=lambda value: value is not None and value.strip() == "主要股東")
    table = label.find_parent("tr").find("table") if label else None
    if table:
        for tr in table.find_all("tr"):
            cells = [c.get_text(" ", strip=True) for c in tr.find_all("td", recursive=False)]
            if not cells or cells[0] == "股東名稱":
                continue
            try:
                if len(cells) != 4 or not cells[0]:
                    raise ValueError("股东行字段不完整")
                shares = int(cells[1].replace(",", ""))
                if not cells[2].endswith("%"):
                    raise ValueError("持股比例缺少百分号")
                ratio = float(cells[2][:-1])
                disclosed = datetime.strptime(cells[3], "%d/%m/%Y").date().isoformat()
                if shares < 0 or not 0 <= ratio <= 100:
                    raise ValueError("持股数量或比例越界")
                rows.append({"股东名称": cells[0], "持股数量": shares, "持股比例": ratio,
                             "披露日期": disclosed, "date_kind": "SOURCE_DISCLOSURE_DATE",
                             "ratio_basis": "来源所示总股本比例（对应各自披露日）",
                             "source_name": SOURCE, "source_url": source_url,
                             "verification_status": "PENDING", "source_record": cells})
            except (ValueError, IndexError) as exc:
                errors.append({"source_record": cells, "error": str(exc)})
    capital = []
    label = soup.find(string=lambda value: value is not None and value.strip() == "發行股數")
    if label:
        cells = label.find_parent("tr").find_all("td", recursive=False)
        try:
            value = int(cells[1].get_text(strip=True).replace(",", ""))
            if value <= 0:
                raise ValueError("发行股数非正值")
            capital = [{"label": "已发行股数", "value": value, "unit": "股",
                        "as_of": None, "date_kind": "PAGE_SNAPSHOT_DATE_UNSPECIFIED",
                        "source_name": SOURCE, "source_url": source_url}]
        except (ValueError, IndexError) as exc:
            errors.append({"field": "發行股數", "error": str(exc)})
    return {"source": SOURCE, "major_source": SOURCE, "capital_structure_source": SOURCE,
            "major": rows, "capital_structure": capital, "source_errors": errors,
            "major_scope": "PUBLIC_MAJOR_SHAREHOLDER_DISCLOSURES",
            "collection_status": "PARTIAL" if rows or capital else "EMPTY_UNVERIFIED",
            "as_of": None,
            "message": "公开主要股东转录，非十大股东全量名单；每条记录的披露日期可能不同，不推断控制关系、流通属性或持股变动。发行股数仅为页面快照，未明确生效日。"}


def fetch_etnet_disclosures(symbol: str) -> dict:
    if not re.fullmatch(r"\d{5}", symbol):
        raise ValueError("港股证券代码必须为五位数字")
    url = f"https://www.etnet.com.hk/www/tc/stocks/realtime/quote_ci_brief.php?code={int(symbol)}"
    with httpx.Client(timeout=15, trust_env=False, follow_redirects=True,
                      headers={"User-Agent": "Mozilla/5.0"}) as client:
        response = client.get(url)
        response.raise_for_status()
    result = parse_etnet_disclosures(response.text, symbol, url)
    observed = datetime.now(timezone.utc).isoformat()
    result.update(observed_at=observed, original_source_response={"url": str(response.url),
                  "fetched_at": observed, "status_code": response.status_code,
                  "response_sha256": hashlib.sha256(response.content).hexdigest(), "html": response.text})
    for row in result["major"]:
        row["observed_at"] = observed
    return result
