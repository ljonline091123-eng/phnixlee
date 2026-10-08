class DurableRetryError(RuntimeError):
    """Transient external failure: retain the durable job until it can recover."""
