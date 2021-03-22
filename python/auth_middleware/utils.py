def clean_dict(data):
    """
    Delete keys with the value ``None`` in a dictionary, recursively.

    This alters the input so you may wish to ``copy`` the dict first.
    """
    for key, value in list(data.items()):
        if value is None:
            del data[key]
        elif isinstance(value, dict):
            clean_dict(value)
        elif isinstance(value, list):
            data[key] = [
                clean_dict(v) if isinstance(v, list) or isinstance(v, dict) else v
                for v in value
            ]
    return data
