# -*- coding: utf-8 -*-

from auth_middleware import create_service_jwt_header, JwtConfig
from auth_middleware.role import OrganizationId


def test_create_service_token_header():
    config = JwtConfig("key")

    assert create_service_jwt_header(config, OrganizationId(1)) is not None
