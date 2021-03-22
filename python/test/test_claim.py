import jwt
import pytest
from auth_middleware import Claim, UserClaim

from auth_middleware.role import (
    DatasetRole,
    OrganizationRole,
    WorkspaceRole,
    OrganizationId,
    DatasetId,
    WorkspaceId,
)
from auth_middleware.models import RoleType, FeatureFlag
from auth_middleware.config import JwtConfig


def test_dataset_id():
    data = UserClaim(
        id=12345,
        roles=[DatasetRole(id=DatasetId(2), role=RoleType.OWNER, node_id="test_id")],
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.head_dataset_id == DatasetId(2)
    assert claim.head_dataset_node_id == "test_id"


def test_dataset_ids():
    data = UserClaim(
        id=12345,
        roles=[
            DatasetRole(id=DatasetId(2), role=RoleType.OWNER, node_id="N:dataset:2"),
            DatasetRole(id=DatasetId(3), role=RoleType.OWNER, node_id="N:dataset:3"),
        ],
    )
    claim = Claim.from_claim_type(data, 10)
    dataset_ids = claim.dataset_ids
    dataset_node_ids = claim.dataset_node_ids
    assert len(dataset_ids) == 2
    assert DatasetId(2) in dataset_ids
    assert DatasetId(3) in dataset_ids
    assert len(dataset_node_ids) == 2
    assert "N:dataset:2" in dataset_node_ids
    assert "N:dataset:3" in dataset_node_ids


def test_node_id():
    data = UserClaim(
        id=12345,
        roles=[DatasetRole(id=DatasetId(2), role=RoleType.OWNER, node_id="id")],
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.head_dataset_node_id == "id"


def test_encryption_key_id():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(
                id=OrganizationId(1), role=RoleType.OWNER, encryption_key_id="id"
            )
        ],
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.encryption_key_id(OrganizationId(1)) == "id"


def test_organization_id():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(id=OrganizationId(1), role=RoleType.OWNER, node_id="id")
        ],
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.head_organization_id == OrganizationId(1)
    assert claim.head_organization_node_id == "id"


def test_organization_ids():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(
                id=OrganizationId(1), role=RoleType.OWNER, node_id="N:org:1"
            ),
            OrganizationRole(
                id=OrganizationId(2), role=RoleType.OWNER, node_id="N:org:2"
            ),
        ],
    )
    claim = Claim.from_claim_type(data, 10)
    organization_ids = claim.organization_ids
    organization_node_ids = claim.organization_node_ids
    assert len(organization_ids) == 2
    assert OrganizationId(1) in organization_ids
    assert OrganizationId(2) in organization_ids
    assert len(organization_node_ids) == 2
    assert "N:org:1" in organization_node_ids
    assert "N:org:2" in organization_node_ids


def test_enabled_features():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(
                id=OrganizationId(1),
                role=RoleType.OWNER,
                enabled_features=[
                    FeatureFlag.CONCEPTS_FEATURE
                ],
            )
        ],
    )

    claim = Claim.from_claim_type(data, 10)
    features = claim.enabled_features(OrganizationId(1))
    assert len(features) == 1
    assert FeatureFlag.CONCEPTS_FEATURE in features


def test_no_dataset_id():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(
                id=OrganizationId(1), role=RoleType.OWNER, enabled_features=None
            ),
            WorkspaceRole(id=WorkspaceId(1), role=RoleType.VIEWER),
        ],
    )

    claim = Claim.from_claim_type(data, 10)
    assert claim.get_role(DatasetId(1)) is None


def test_dataset_role_wildcard():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(
                id=OrganizationId(1), role=RoleType.OWNER, enabled_features=None
            ),
            DatasetRole(id=DatasetId("*"), role=RoleType.EDITOR),
            WorkspaceRole(id=WorkspaceId(1), role=RoleType.VIEWER),
        ],
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.get_role(DatasetId(1)).role == RoleType.EDITOR


def test_dataset_role_wildcard_order():
    data = UserClaim(
        id=12345,
        roles=[
            OrganizationRole(
                id=OrganizationId(1), role=RoleType.OWNER, enabled_features=None
            ),
            DatasetRole(id=DatasetId("*"), role=RoleType.OWNER),
            DatasetRole(id=DatasetId(2), role=RoleType.EDITOR),
            WorkspaceRole(id=WorkspaceId(1), role=RoleType.VIEWER),
        ],
    )

    claim = Claim.from_claim_type(data, 10)
    assert claim.get_role(DatasetId(2)).role == RoleType.EDITOR
    assert claim.get_role(DatasetId(1)).role == RoleType.OWNER


def test_missing_organization_role():
    data = UserClaim(
        id=12345,
        roles=[
            DatasetRole(id=DatasetId(1), role=RoleType.OWNER),
            WorkspaceRole(id=WorkspaceId("*"), role=RoleType.OWNER),
        ],
    )

    claim = Claim.from_claim_type(data, 10)
    assert claim.get_role(OrganizationId(1)) is None


def test_organization_role_wildcard_order():
    data = UserClaim(
        id=12345,
        roles=[
            DatasetRole(id=DatasetId(1), role=RoleType.OWNER),
            OrganizationRole(id=OrganizationId("*"), role=RoleType.OWNER),
            OrganizationRole(
                id=OrganizationId(2), role=RoleType.EDITOR, enabled_features=None
            ),
            WorkspaceRole(id=WorkspaceId(1), role=RoleType.VIEWER),
        ],
    )

    claim = Claim.from_claim_type(data, 10)

    assert claim.get_role(OrganizationId(2)).role == RoleType.EDITOR
    assert claim.get_role(OrganizationId(1)).role == RoleType.OWNER


def test_expired_claim():
    data = UserClaim(
        id=12345,
        roles=[DatasetRole(id=DatasetId(2), role=RoleType.OWNER, node_id="test_id")],
    )
    claim = Claim.from_claim_type(data, -1)
    assert claim.is_valid is False


def test_unexpired_claim():
    data = UserClaim(
        id=12345,
        roles=[DatasetRole(id=DatasetId(2), role=RoleType.OWNER, node_id="test_id")],
    )
    claim = Claim.from_claim_type(data, 10)
    assert claim.is_valid is True


def test_decode_same_key():
    # Decoding should succeed if the key is the same as what was used to encode
    data = UserClaim(
        id=12345,
        roles=[DatasetRole(id=DatasetId(2), role=RoleType.OWNER, node_id="test_id")],
    )
    claim = Claim.from_claim_type(data, 10)
    test_config = JwtConfig("test-key")
    jwt = claim.encode(test_config)
    decoded_claim = Claim.from_token(jwt, test_config)
    assert decoded_claim is not None


def test_decode_different_key():
    # Decoding should fail if the key is different from what was used to encode
    data = UserClaim(
        id=12345,
        roles=[DatasetRole(id=DatasetId(2), role=RoleType.OWNER, node_id="test_id")],
    )
    claim = Claim.from_claim_type(data, 10)
    test_config = JwtConfig("test-key")
    bad_config = JwtConfig("other-key")
    token = claim.encode(test_config)
    with pytest.raises(jwt.exceptions.InvalidSignatureError):
        decoded_claim = Claim.from_token(token, bad_config)
        assert decoded_claim is not None
