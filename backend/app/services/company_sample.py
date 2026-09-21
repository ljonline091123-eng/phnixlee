"""Explicit acceptance scope; securities must already exist in stock_symbol."""

SAMPLE_BATCH = "company-graph-20260921"
SAMPLE_SECURITIES = [
    {"market": "CN_A", "symbol": code}
    for code in (
        "000001", "000333", "000725", "000858", "002594", "300750",
        "600028", "600030", "600036", "600104", "600221", "600276",
        "600519", "600900", "601318", "601398", "601668", "601766",
        "601857", "601899",
    )
] + [{"market": "HK", "symbol": code} for code in ("02318", "03968", "01211", "02899", "01398")]
