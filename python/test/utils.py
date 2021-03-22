import json
from auth_middleware import JwtConfig

config = JwtConfig("secret-key")


def load_claim(name: str) -> dict:
    with open("./resources/{}".format(name)) as f:
        return json.load(f)
