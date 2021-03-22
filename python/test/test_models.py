from auth_middleware import UserClaim, ServiceClaim, claim_from_dict, session_from_data
from test.utils import load_claim
from auth_middleware.role import (
    OrganizationRole,
    DatasetRole,
    WorkspaceRole,
    OrganizationId,
    DatasetId,
    WorkspaceId,
)
from auth_middleware.models import RoleType, FeatureFlag


def test_session_browser_type():
    data = load_claim("claim_browser_type.json")
    session = session_from_data(data)
    assert session.is_browser


def test_session_api_type():
    data = load_claim("claim_api_type.json")
    session = session_from_data(data)
    assert session.is_api


def test_session_temporary_type():
    data = load_claim("claim_temporary_type.json")
    session = session_from_data(data)
    assert session.is_temporary


def test_reject_invalid_type():
    data = load_claim("claim_invalid_type.json")
    session = session_from_data(data)
    assert session is None


def test_encode_simple_user():
    data = load_claim("claim_simple_user.json")
    claim = UserClaim(
        id=1, roles=[OrganizationRole(id=OrganizationId("*"), role=RoleType.OWNER)]
    )
    assert claim_from_dict(data) is not None
    assert claim_from_dict(data) == claim

def test_encode_simple_user_with_node_id():
    data = load_claim("claim_simple_user_with_node_id.json")

    claim = UserClaim(
        id=1, roles=[OrganizationRole(id=OrganizationId("*"), role=RoleType.OWNER)],
        node_id="N:user:d39c79c0-1fbb-4b3e-aab1-77323d965101"
    )
    assert claim_from_dict(data) is not None
    assert claim_from_dict(data) == claim


def test_encode_simple_service():
    data = load_claim("claim_simple_service.json")
    claim = ServiceClaim(
        roles=[OrganizationRole(id=OrganizationId("*"), role=RoleType.OWNER)]
    )
    assert claim_from_dict(data) is not None
    assert claim_from_dict(data) == claim


def test_encode_complex_roles():
    dataset_id = DatasetId(123)
    dataset_node_id = "N:dataset:38e9544e-3f23-4057-a76a-3e2a4f767e61"
    workspace_id = WorkspaceId(456)
    data = load_claim("claim_complex_roles.json")
    claim = UserClaim(
        id=1,
        roles=[
            OrganizationRole(id=OrganizationId("*"), role=RoleType.OWNER),
            DatasetRole(id=dataset_id, role=RoleType.OWNER, node_id=dataset_node_id),
            WorkspaceRole(id=workspace_id, role=RoleType.OWNER),
        ],
    )

    assert claim_from_dict(data) is not None
    assert claim_from_dict(data) == claim


def test_encode_dataset_roles_with_locked_status():
    dataset_id = DatasetId(123)
    dataset_node_id = "N:dataset:38e9544e-3f23-4057-a76a-3e2a4f767e61"
    data = load_claim("claim_locked_datasets.json")
    claim = UserClaim(
        id=1,
        roles=[
            OrganizationRole(id=OrganizationId("*"), role=RoleType.OWNER),
            DatasetRole(id=dataset_id, role=RoleType.OWNER, node_id=dataset_node_id, locked=True),
            DatasetRole(id=DatasetId("*"), role=RoleType.OWNER, locked=True),
        ],
    )

    assert claim_from_dict(data) is not None
    assert claim_from_dict(data) == claim


def test_encode_organization_secret_key():
    organization_id = OrganizationId(1)
    organization_secret_key = "secret-key-id"
    organization_node_id = "N:organization:38e9544e-3f23-4057-a76a-3e2a4f767e61"

    data = load_claim("claim_secret_key_id.json")
    claim = UserClaim(
        id=1,
        roles=[
            OrganizationRole(
                id=organization_id,
                role=RoleType.EDITOR,
                encryption_key_id=organization_secret_key,
                node_id=organization_node_id,
                enabled_features=[FeatureFlag.CONCEPTS_FEATURE]
            )
        ],
    )

    assert claim_from_dict(data) is not None
    assert claim_from_dict(data) == claim
