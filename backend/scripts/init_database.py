from app.db.bootstrap import initialize_database


def main() -> None:
    initialize_database()
    print("Database tables and default data-source catalog are ready.")


if __name__ == "__main__":
    main()
