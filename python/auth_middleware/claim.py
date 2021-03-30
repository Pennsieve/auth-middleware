import jwt
import datetime
import json
from typing import List, Optional, Union
from dataclasses import dataclass, field
from dataclasses_json import dataclass_json
from . import JwtConfig
from .utils import clean_dict
from .role import role_from_dict, Role, Id, OrganizationId, DatasetId, WorkspaceId
from .models import CognitoSessionType, Permission, FeatureFlag, Role as PennsieveRole


def cognito_session_from_data(data) -> Optional["CognitoSession"]:
    if isinstance(data, dict):
        if data["type"] in CognitoSessionType.values():
            return CognitoSession.from_json(json.dumps(data))  # type: ignore
    return None


@dataclass_json
@dataclass
class CognitoSession:
    id: str
    type: CognitoSessionType

    @property
    def is_browser(self):
        return self.type == CognitoSessionType.BROWSER

    @property
    def is_api(self):
        return self.type == CognitoSessionType.API


@dataclass_json
@dataclass
class ClaimType:
    roles: List[Role] = field(
        metadata={"dataclasses_json": {"decoder": role_from_dict}}
    )


@dataclass_json
@dataclass
class UserClaim(ClaimType):
    id: int
    type: str = "user_claim"
    cognito: Optional[Union[CognitoSession, str]] = field(
        default=None, metadata={"dataclasses_json": {"decoder": cognito_session_from_data}}
    )
    node_id: Optional[str] = None


@dataclass_json
@dataclass
class ServiceClaim(ClaimType):
    type: str = "service_claim"


def claim_from_dict(data) -> ClaimType:
    if data["type"] == "user_claim":
        cls = UserClaim
    elif data["type"] == "service_claim":
        cls = ServiceClaim
    else:
        raise ValueError("Invalid claim type {}".format(data["type"]))

    return cls.from_json(json.dumps(data))  # type: ignore


@dataclass
class Claim:
    content: ClaimType
    exp: datetime.datetime
    iat: datetime.datetime = datetime.datetime.utcnow()

    @property
    def is_valid(self) -> bool:
        return (datetime.datetime.utcnow() - self.exp).total_seconds() < 0

    def encode(self, config: JwtConfig) -> bytes:
        data = clean_dict(json.loads(self.content.to_json()))  # type: ignore
        data["exp"] = self.exp
        data["iat"] = self.iat
        return jwt.encode(data, config.key, algorithm=config.algorithm)

    @classmethod
    def from_token(cls, token: str, config: JwtConfig) -> "Claim":
        data = jwt.decode(token, config.key, algorithms=[config.algorithm], verify=True)
        return cls.from_dict(data)

    @classmethod
    def from_dict(cls, data) -> "Claim":
        if "exp" not in data or "iat" not in data:
            raise KeyError("Claims need an expiration and issued at timestamp")
        exp = datetime.datetime.fromtimestamp(data.pop("exp"))
        iat = datetime.datetime.fromtimestamp(data.pop("iat"))
        return cls(claim_from_dict(data), exp, iat)

    @classmethod
    def from_claim_type(cls, content: ClaimType, seconds: int) -> "Claim":
        now = datetime.datetime.utcnow()
        return cls(content, now + datetime.timedelta(seconds=seconds))

    def _head_role_id(self, role_type: PennsieveRole) -> Optional[Id]:
        for role in self.content.roles:
            if role.type == role_type:
                return role.id
        return None

    def _head_role_node_id(self, role_type: PennsieveRole) -> Optional[str]:
        for role in self.content.roles:
            if role.type == role_type:
                return role.node_id
        return None

    def _role_ids(self, role_type: PennsieveRole) -> List[Id]:
        return [role.id for role in self.content.roles if role.type == role_type]

    def _role_node_ids(self, role_type: PennsieveRole) -> List[str]:
        return [
            role.node_id
            for role in self.content.roles
            if role.type == role_type and role.node_id is not None
        ]

    @property
    def head_dataset_id(self) -> Optional[DatasetId]:
        return self._head_role_id(PennsieveRole.DATASET_ROLE)  # type:ignore

    @property
    def head_dataset_node_id(self) -> Optional[str]:
        return self._head_role_node_id(PennsieveRole.DATASET_ROLE)

    @property
    def dataset_ids(self) -> List[DatasetId]:
        return self._role_ids(PennsieveRole.DATASET_ROLE)  # type:ignore

    @property
    def dataset_node_ids(self) -> List[str]:
        return self._role_node_ids(PennsieveRole.DATASET_ROLE)

    @property
    def head_organization_id(self) -> Optional[OrganizationId]:
        return self._head_role_id(PennsieveRole.ORGANIZATION_ROLE)  # type:ignore

    @property
    def head_organization_node_id(self) -> Optional[str]:
        return self._head_role_node_id(PennsieveRole.ORGANIZATION_ROLE)

    def organization_node_id(self, orgainization_id: OrganizationId) -> Optional[str]:
        for role in self.content.roles:
            if (
                role.type == PennsieveRole.ORGANIZATION_ROLE
                and role.id == orgainization_id
            ):
                return role.node_id
        return None

    @property
    def organization_ids(self) -> List[OrganizationId]:
        return self._role_ids(PennsieveRole.ORGANIZATION_ROLE)  # type: ignore

    @property
    def organization_node_ids(self) -> List[str]:
        return self._role_node_ids(PennsieveRole.ORGANIZATION_ROLE)

    def enabled_features(
        self, organization_id: OrganizationId
    ) -> Optional[List[FeatureFlag]]:
        role = self.get_role(organization_id)
        if role:
            return role.enabled_features  # type:ignore
        return None

    def has_feature_enabled(
        self, organization_id: OrganizationId, feature: FeatureFlag
    ) -> bool:
        features = self.enabled_features(organization_id)
        if features:
            return feature in features
        return False

    def encryption_key_id(self, organization_id: OrganizationId) -> Optional[str]:
        role = self.get_role(organization_id)
        if role:
            return role.encryption_key_id  # type:ignore
        return None

    def has_organization_access(self, organization_id: OrganizationId) -> bool:
        return self.get_role(organization_id) is not None

    def has_dataset_access(self, dataset_id: DatasetId, permission: Permission) -> bool:
        role = self.get_role(dataset_id)
        if role:
            return role.has_permission(permission)
        return False

    def has_workspace_access(
        self, workspace_id: WorkspaceId, permission: Permission
    ) -> bool:
        role = self.get_role(workspace_id)
        if role:
            return role.has_permission(permission)
        return False

    @property
    def is_service_claim(self) -> bool:
        return isinstance(self.content, ServiceClaim)

    @property
    def is_user_claim(self) -> bool:
        return isinstance(self.content, UserClaim)

    def get_role(self, role_id: Id) -> Optional[Role]:
        wildcard_role = None
        for role in self.content.roles:
            if role.id == role_id:
                return role
            if role.id.matches(role_id) and wildcard_role is None:
                wildcard_role = role
        return wildcard_role
