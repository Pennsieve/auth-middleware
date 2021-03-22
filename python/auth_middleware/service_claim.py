# -*- coding: utf-8 -*-

from . import JwtConfig
from .claim import Claim, ServiceClaim
from auth_middleware.role import OrganizationId, OrganizationRole
from auth_middleware.models import RoleType


def create_service_jwt_token(
    config: JwtConfig, organization_id: OrganizationId, expiry_in_minutes: int = 5
):
    data = ServiceClaim(
        roles=[OrganizationRole(id=organization_id, role=RoleType.OWNER)]
    )
    claim = Claim.from_claim_type(data, expiry_in_minutes * 60)

    return claim.encode(config)


def create_service_jwt_header(
    config: JwtConfig, organization_id: OrganizationId, expiry_in_minutes: int = 5
):
    jwt_token = create_service_jwt_token(config, organization_id)

    return {"Authorization": "Bearer {}".format(jwt_token)}
