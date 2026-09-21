"""Add canonical identity aliases without rebuilding or deleting legacy graphs."""

import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from sqlalchemy import create_engine
from sqlalchemy.orm import Session

from app.services.graph_identity import backfill_legacy_identity


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--database-url", required=True)
    parser.add_argument("--apply", action="store_true", help="Commit property additions; otherwise read-only preview")
    args = parser.parse_args()
    engine = create_engine(args.database_url)
    try:
        with Session(engine) as db:
            result = backfill_legacy_identity(db, apply=args.apply)
            if args.apply:
                db.commit()
            print(json.dumps({"applied": args.apply, **result}))
    finally:
        engine.dispose()


if __name__ == "__main__":
    main()
