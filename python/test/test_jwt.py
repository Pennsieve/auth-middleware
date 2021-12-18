from auth_middleware import Claim, UserClaim, ServiceClaim, claim_from_dict
from auth_middleware.role import (
    DatasetRole,
    OrganizationRole,
    WorkspaceRole,
    OrganizationId,
    DatasetId,
    WorkspaceId,
)
from auth_middleware.models import (
    FeatureFlag,
    RoleType,
    DatasetPermission,
)
from test.utils import load_claim, config


def test_encode_decode_simple_claim():
    data = ServiceClaim(
        roles=[
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.OWNER),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    token = claim.encode(config)
    decoded = Claim.from_token(token, config)
    assert decoded.content == data


def test_decode_user_claim_explicit_session():
    data = load_claim("claim_with_explicit_session.json")
    claim = claim_from_dict(data)
    assert isinstance(claim, UserClaim)
    session = claim.cognito
    assert session.id == "60e5aea3-1853-4a3f-8bd5-31822868cf41"
    assert session.is_api


def test_decode_user_claim_no_explicit_session():
    data = load_claim("claim_no_session.json")
    claim = claim_from_dict(data)
    assert isinstance(claim, UserClaim)
    assert claim.cognito is None


# IMPORTANT: THIS TEST IS CRITICAL AND SHOULD NOT BE ARBITRARILY CHANGED
# TO ACCOMMODATE LARGER JWT TOKENS
#
# If this test is failing we need to reconsider the structure
# of our JWTs in order to limit its size. Unbounded lists
# should be removed and/or large objects should be cut down to their
# essential properties.
#
# Or we should re-investigate this method of authorization.
def test_exceeds_default_header_size_limit():
    data = ServiceClaim(
        roles=[
            OrganizationRole(
                id=OrganizationId(1),
                role=RoleType.OWNER,
                encryption_key_id="arn:aws:iam::111122223333:role/KMSAdminRole",
                node_id="N:organization:38e9544e-3f23-4057-a76a-3e2a4f767e61",
                enabled_features=FeatureFlag.values(),
            ),
            DatasetRole(
                id=DatasetId(2),
                role=RoleType.OWNER,
                node_id="N:dataset:38e9544e-3f23-4057-a76a-3e2a4f767e61",
            ),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    token = claim.encode(config)
    # Apache's default limit for headers is 8K.
    #   See: http://httpd.apache.org/docs/2.2/mod/core.html#limitrequestfieldsize
    # Let's ensure our JWT header is less than half that.
    assert len(token) < (8190 / 2)


def test_ignore_unsupported_features():
    data = load_claim("claim_with_unsupported_features.json")
    claim = Claim.from_claim_type(claim_from_dict(data), 10)
    token = claim.encode(config)
    parsed_claim = Claim.from_token(token, config)
    features = parsed_claim.enabled_features(OrganizationId(1))
    assert features == []


def test_extract_typed_claim_from_token():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.OWNER),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.OWNER),
        ],
    )

    claim = Claim.from_claim_type(data, 10)
    token = claim.encode(config)
    parsed_claim = Claim.from_token(token, config)
    assert isinstance(parsed_claim.content, UserClaim)
    assert parsed_claim.content == data


def test_handle_user_claim():
    claim = Claim.from_claim_type(UserClaim(id=1, roles=[]), 10)
    assert claim.is_user_claim
    assert not claim.is_service_claim


def test_handle_service_claim():
    claim = Claim.from_claim_type(ServiceClaim([]), 10)
    assert not claim.is_user_claim
    assert claim.is_service_claim


def test_wildcard_org_access():
    data = ServiceClaim([OrganizationRole(id=OrganizationId("*"), role=RoleType.OWNER)])
    claim = Claim.from_claim_type(data, 10)
    assert claim.has_organization_access(OrganizationId(1))


def test_claim_org_access():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            OrganizationRole(id=OrganizationId(2), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.has_organization_access(OrganizationId(2))


def test_claim_invalid_org_access():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.OWNER),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert not claim.has_organization_access(OrganizationId(2))


def test_claim_dataset_access_valid_permissions():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.has_dataset_access(DatasetId(2), DatasetPermission.DELETE_DATASET)


def test_claim_dataset_access_invalid_permissions():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.VIEWER),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert not claim.has_dataset_access(DatasetId(2), DatasetPermission.DELETE_DATASET)


def test_claim_dataset_access_valid_permissions_wrong_dataset():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.EDITOR),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert not claim.has_dataset_access(DatasetId(3), DatasetPermission.VIEW_FILES)


def test_claim_workspace_access_valid_permissions():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.EDITOR),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.has_workspace_access(
        WorkspaceId(3), WorskpacePermission.VIEW_DASHBOARD
    )


def test_claim_workspace_access_invalid_permissions():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.VIEWER),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.EDITOR),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert not claim.has_workspace_access(
        WorkspaceId(3), WorskpacePermission.MANAGE_VIEWS
    )


def test_claim_workspace_access_valid_permissions_wrong_workspace():
    data = ServiceClaim(
        [
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.EDITOR),
            WorkspaceRole(id=WorkspaceId(3), role=RoleType.OWNER),
        ]
    )
    claim = Claim.from_claim_type(data, 10)
    assert not claim.has_workspace_access(
        WorkspaceId(4), WorskpacePermission.VIEW_DASHBOARD
    )
