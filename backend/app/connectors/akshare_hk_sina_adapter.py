import akshare as ak

from app.connectors.akshare_adapter import AkshareAdapter
from app.connectors.base import SymbolRecord


class AkshareHkSinaAdapter(AkshareAdapter):
    """AkShare's Sina Hong Kong market endpoints as a fallback source."""

    adapter_type = "AKSHARE_HK_SINA"

    def health_check(self) -> tuple[str, list[str]]:
        if not hasattr(ak, "stock_hk_spot") or not hasattr(ak, "stock_hk_daily"):
            raise RuntimeError("Installed AkShare package is missing Sina Hong Kong market methods")
        return (
            "AkShare Sina Hong Kong fallback adapter is configured.",
            ["HK_SYMBOLS", "HK_KLINE_ON_DEMAND"],
        )

    def fetch_symbol_master(self, market: str) -> list[SymbolRecord]:
        if market != "HK":
            raise ValueError("AkShare Sina fallback only supports the HK market")
        dataframe = ak.stock_hk_spot()
        return self._normalize_dataframe(dataframe=dataframe, market="HK")
