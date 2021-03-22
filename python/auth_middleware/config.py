# -*- coding: utf-8 -*-
from dataclasses import dataclass


@dataclass
class JwtConfig:
    key: str
    algorithm: str = "HS256"
