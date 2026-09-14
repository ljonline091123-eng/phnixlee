# Gemini Quant Agent

Phase 1 provides a configurable market-data foundation for A-share and Hong Kong
stock master data:

- data source management
- data interface catalog management
- A-share and Hong Kong stock symbol synchronization
- synchronization logs and stock master-data queries
- primary/fallback source routing for Hong Kong stock master data, including HKEX fallback
- on-demand K-line, financial-indicator, and announcement fetch APIs
- stock-code click-through F10-style detail drawer in the console
- stock analysis tool endpoints for realtime quote, K-line, news, notices, and model-assisted analysis
- multi-model provider, model instance, and task-routing management
- model test, call audit log, and fallback route support

Financial reports, announcements, and historical K-lines are registered as
on-demand interfaces. They are intentionally not bulk-downloaded in this phase:
they are fetched only after a user selects a stock.

## Run locally

```powershell
cd backend
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --reload
```

The local default database is `backend/quant.db`. To use PostgreSQL, set
`DATABASE_URL` before starting the API:

```powershell
$env:DATABASE_URL="postgresql+psycopg://quant_user:quant_password@127.0.0.1:5432/quant_agent"
python -m uvicorn app.main:app --reload
```

Open `http://127.0.0.1:8000/docs` after startup.

To create tables and seed the initial source/interface catalog without starting
the API:

```powershell
cd backend
python -m scripts.init_database
```

## Run with Docker

```powershell
docker compose up --build
```

The API will be available on port `8000`, PostgreSQL on port `5432`, and the
data-source management console on `http://127.0.0.1:5173`.

To run the console in development mode:

```powershell
cd frontend
copy .env.example .env.local
npm install
npm run dev
```

The console now includes a `模型实验室` module. It can manage model providers,
model instances, task routes, connection tests, offline model chat, and recent
call logs. The default `MOCK_GENERAL` instance works without external
credentials, so the model-routing flow can be verified locally first.

## First synchronization

The application seeds the `AKSHARE` source and interface catalog on startup.
Run either market through Swagger, or call:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8000/api/v1/stocks/sync" `
  -ContentType "application/json" `
  -Body '{"source_code":"AKSHARE","market":"CN_A"}'
```

Supported markets are `CN_A`, `HK`, `NEEQ`, `NEEQ_INNOVATION`, and `ALL`.

Hong Kong master-data synchronization uses a fallback chain. If the AkShare     
Eastmoney and Sina HK endpoints are reset by the upstream network, the app falls
back to the HKEXnews SDW stock list and still stores HK stock code/name master  
data.

### Optional and official data sources

- `PYTDX` is an optional A-share quote/K-line adapter backed by TongdaXin
  protocol nodes. It is seeded disabled because node connectivity varies by
  network; install `pytdx` and enable the source only after testing a reachable
  node. Set `PYTDX_HOSTS=host:port,host:port` to use private or regional nodes.
  It does not provide announcements, financial reports, Hong Kong, or NEEQ data.
- `HKEX_SDW` is the Hong Kong Exchange securities-list/CCASS query source and is
  used as a master-data fallback. HKEXnews disclosure notices and reports remain
  routed through `HKEXNEWS_OFFICIAL`.
- `NEEQ_OFFICIAL` records the National Equities Exchange and Quotations official
  portal. It is seeded as a disabled, pending structured-integration source:
  the official portal is authoritative for NEEQ private placements, market-maker
  details, and layer changes, but those pages require separate endpoint parsing
  and are not claimed as available until implemented. Existing NEEQ reports and
  notices continue to use `NEEQ_EASTMONEY`.

You can also run the command-line synchronizer:

```powershell
cd backend
python -m scripts.sync_symbols --market CN_A
```

## On-demand stock data

Fetches are logged in `data_fetch_log`; when `persist` is true, the returned
records are also stored in their dedicated tables.

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://127.0.0.1:8000/api/v1/stocks/CN_A/000001/kline/fetch" `
  -ContentType "application/json" `
  -Body '{"period":"daily","start_date":"2026-01-01","end_date":"2026-09-01","persist":true}'
```

Available endpoints:

- `POST /api/v1/stocks/{market}/{symbol}/kline/fetch`
- `POST /api/v1/stocks/{market}/{symbol}/financials/fetch`
- `POST /api/v1/stocks/{market}/{symbol}/notices/fetch`
- `GET /api/v1/stocks/{market}/{symbol}/f10`
- `GET /api/v1/stocks/{market}/{symbol}/kline`
- `GET /api/v1/stocks/{market}/{symbol}/financials`
- `GET /api/v1/stocks/{market}/{symbol}/notices`
- `GET /api/v1/stocks/fetch-logs/list`
- `GET /api/v1/stock-tools`
- `POST /api/v1/stock-tools/execute`
- `POST /api/v1/stock-tools/analyze`

## Important boundaries

- This phase stores stock master data only: code, name, market, exchange, status,
  source, and raw source payload.
- Data source credentials are configuration placeholders. Production secrets should
  be encrypted or stored in a secret manager before multi-user deployment.
- Network failures from upstream providers are written to synchronization logs and
  do not corrupt existing stock master data.
- Model API keys are not returned by management APIs. Newly configured provider
  keys are encrypted at rest using `MODEL_CREDENTIAL_KEY`; keep that key outside Git.

## Model hub

The application starts with an offline `MOCK_GENERAL` model so model routing,
skills, and tests can be validated before real model credentials are configured.

For provider API keys, create a Fernet encryption key once and set it before
starting the API. Keep the same key across restarts; losing it makes stored
provider credentials unreadable:

```powershell
python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
$env:MODEL_CREDENTIAL_KEY="<generated-value>"
```

For the local Windows setup, the same stable key can be kept in the ignored
`backend/.env` file as `MODEL_CREDENTIAL_KEY=...`. The Model Lab already contains
DeepSeek V4 Pro/Flash and Gemini 2.5 Pro/Flash instances. Open **模型实验室 →
模型与路由 → 模型供应商**, edit the DeepSeek and Gemini providers, enter each
provider's API Key, save, then use **测试** to verify the real API response.
DeepSeek handles routine chat, governance, warnings, and stock analysis;
Gemini Pro handles deep research and is the fallback for detailed analysis.
Without either provider key, task routes can fall back to the clearly labeled
local Mock instance; no external model connectivity is implied by a Mock result.

Model management APIs:

- `GET|POST|PUT /api/v1/model-hub/providers`
- `GET|POST|PUT /api/v1/model-hub/instances`
- `GET|POST|PUT /api/v1/model-hub/routes`
- `POST /api/v1/model-hub/instances/{instance_id}/test`
- `POST /api/v1/model-hub/chat`
- `GET /api/v1/model-hub/call-logs`

Supported provider types are `MOCK`, `OPENAI_COMPAT`, `DEEPSEEK`, `GEMINI_REST`,
and `ANTHROPIC`. The
task route chooses a preferred instance first, then attempts its configured
fallback chain when the preferred model cannot complete the call.

The core model/Agent/Skill refactor, data contracts, and prediction-review
workflow are documented in [docs/core-architecture.md](docs/core-architecture.md).

## Phase 2 AI research center

The `05 研究中心` navigation item opens the AI research workflow. It accepts
an A-share, Hong Kong, NEEQ, or innovation-layer code/name and streams:

1. local F10 financial indicators and year-on-year growth;
2. 20-day capital-flow context, daily K-line moving averages, support and
   resistance levels;
3. recent local news and announcements with deterministic evidence labels;
4. structured scores from `FundamentalAgent` and `TechnicalCapitalAgent`;
5. a Markdown report assembled by `MasterOrchestratorAgent`.

The endpoint is `POST /api/v1/research/analyze` and returns
`text/event-stream` events named `stage`, `agent`, `report`, `done`, and
`error`. Its request body is:

```json
{"symbol":"000001","market":"CN_A","top_k":5,"refresh":false}
```

When the `stock_analysis` route has a configured DeepSeek instance, the model
is used to refine the structured agent output and final report. Without a key,
or when the provider is unavailable, the workflow falls back to the existing
`MOCK_GENERAL` route and a deterministic local Markdown report, so research
remains usable offline. The implementation is organized under
`backend/app/skills`, `backend/app/agents`, `backend/app/prompts`,
`backend/app/services/market_data.py`, and
`backend/app/api/v1/endpoints/research.py`.
