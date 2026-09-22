"""Read-only acceptance checks against the running data platform.

Use after starting the API. No model calls, source refreshes or database writes
are performed. A failed check is reported instead of being silently skipped.
"""

import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import json
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import urlopen


STOCKS = [
    *(('CN_A', code) for code in (
        '000001', '000002', '000333', '000651', '000858', '002415', '002594',
        '300059', '300750', '600000', '600009', '600030', '600036', '600276',
        '600519', '600887', '601166', '601318', '601398', '601857',
    )),
    *(('HK', code) for code in ('00001', '00700', '00939', '00941', '09988')),
]


def get_json(base, path, **params):
    url = base.rstrip('/') + path
    if params:
        url += '?' + urlencode(params)
    with urlopen(url, timeout=45) as response:
        return json.load(response)


def check_stock(base, pair):
    market, symbol = pair
    report = {'market': market, 'symbol': symbol, 'passed': False}
    try:
        result = get_json(base, '/knowledge-network/explore', market=market, symbol=symbol)
        center = f'security:{market}:{symbol}'
        nodes = {node['id']: node for node in result['nodes']}
        edges = result['edges']
        assert result['center_id'] == center and center in nodes, 'wrong graph center'
        assert all(edge['source'] in nodes and edge['target'] in nodes for edge in edges), 'dangling edge'
        issuers = [edge for edge in edges if edge['source'] == center and edge['type'] == 'ISSUED_BY']
        assert len(issuers) == 1, 'missing or ambiguous issuer'
        company_id = issuers[0]['target']
        assert nodes[company_id]['type'] == 'COMPANY', 'issuer is not a company'
        company = get_json(base, '/knowledge-network/explore', center_id=company_id)
        assert company['center_id'] == company_id, 'company graph center mismatch'
        assert center in {node['id'] for node in company['nodes']}, 'company does not link back to security'
        proof = next(item for item in result['evidence'] if item.get('source_table') == 'stock_symbol')
        detail = get_json(base, '/knowledge-network/evidence', source_table=proof['source_table'], record_id=proof['source_record_id'])
        assert detail.get('content'), 'evidence cannot be opened'
        report.update(passed=True, nodes=len(nodes), edges=len(edges), company_id=company_id,
                      evidence_count=len(result['evidence']), coverage=result.get('coverage', []))
    except Exception as exc:
        report['error'] = str(exc)
    return report


def run(base):
    with ThreadPoolExecutor(max_workers=3) as pool:
        stocks = list(pool.map(lambda pair: check_stock(base, pair), STOCKS))
    return {'checked_at': datetime.now(timezone.utc).isoformat(), 'read_only': True,
            'passed': all(stock['passed'] for stock in stocks),
            'checked_stocks': len(stocks), 'passed_stocks': sum(stock['passed'] for stock in stocks),
            'stocks': stocks}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--api-base', default='http://127.0.0.1:8000/api/v1')
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    if args.report and args.report.suffix.lower() != '.json':
        parser.error('The report must be a JSON file, never a database or source file')
    result = run(args.api_base)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({key: value for key, value in result.items() if key != 'stocks'}, ensure_ascii=False))
    for stock in result['stocks']:
        if not stock['passed']:
            print(json.dumps(stock, ensure_ascii=False))
    raise SystemExit(0 if result['passed'] else 1)


if __name__ == '__main__':
    main()
