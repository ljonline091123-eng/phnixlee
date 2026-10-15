"""Versioned profile for L0 deterministic change detection.

Every threshold used by :mod:`app.services.change_detection` lives here.
Bump ``L0_PROFILE_VERSION`` whenever a value changes so detections stay
attributable to the exact rule set that produced them (required for the
L4 review / attribution loop).

Threshold defaults intentionally mirror the existing watch-filter
conventions in ``app.schemas.skill_tools`` (``min_abs_change_pct=3.0``,
``min_volume_ratio=2.0``): L0 is the recall layer, precision filtering
belongs to L1 scoring.
"""

from __future__ import annotations

L0_PROFILE_VERSION = "L0_PROFILE_V1"

# Each entry mirrors exactly one deterministic detector's keyword arguments.
L0_PROFILE: dict[str, dict[str, float | int | bool]] = {
    "price_move": {"min_abs_change_pct": 3.0},
    "volume_spike": {"min_volume_ratio": 2.0, "window": 20},
    "breakout_20d": {"window": 20},
    "ma_cross": {"fast_window": 5, "slow_window": 20},
    "notice_new": {"enabled": True},
    "notice_cluster": {"min_count": 3, "window_days": 3},
    "news_cluster": {"min_count": 5, "window_days": 3},
}
