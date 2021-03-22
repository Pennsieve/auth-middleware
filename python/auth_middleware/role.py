from typing import Optional, List
from dataclasses import dataclass, field
from dataclasses_json import dataclass_json
import json
from .models import FeatureFlag, RoleType, Permission, Role as PennsieveRole


@dataclass
class Id:
    id: int = -1
    wildcard: str = ""

    def __init__(self, value):
        if isinstance(value, int):
            self.id = value
        elif value.isdigit():
            self.id = int(value)
        else:
            self.wildcard = value

    def __eq__(self, other) -> bool:
        return type(other) is type(self) and other.id == self.id

    def matches(self, other: "Id") -> bool:
        return self == other or (
            (type(self) is type(other))
            and (self.wildcard == "*" or self.wildcard == "*")
        )


class DatasetId(Id):
    pass


class OrganizationId(Id):
    pass


class WorkspaceId(Id):
    pass


@dataclass
class Role:
    role: RoleType
    node_id: Optional[str] = None
    type: PennsieveRole = PennsieveRole("")
    id: Id = Id(0)

    @property
    def permissions(self) -> List[Permission]:
        return self.role.permissions

    def has_permission(self, permission: Permission) -> bool:
        return self.role.has_permission(permission)

    def has_permissions(self, permissions: List[Permission]) -> bool:
        return self.role.has_permissions(permissions)


# Provide a metadata dict for the id field with custom hooks for dataclass_json
# to use when encoding/decoding the values, because the raw json doesn't exactly
# reflect the object structure we want to use.


@dataclass_json
@dataclass
class OrganizationRole(Role):
    id: OrganizationId = field(
        default=OrganizationId(-1),
        metadata={
            "dataclasses_json": {
                "decoder": lambda x: OrganizationId(x),
                "encoder": lambda x: x["wildcard"] if x["wildcard"] else x["id"],
            }
        },
    )
    type: PennsieveRole = PennsieveRole.ORGANIZATION_ROLE
    enabled_features: Optional[List[FeatureFlag]] = field(
        default=None,
        metadata={
            "dataclasses_json": {
                "decoder": lambda flags: [
                    FeatureFlag(flag) for flag in flags if flag in FeatureFlag.values()
                ]
                if flags is not None
                else None
            }
        },
    )
    encryption_key_id: Optional[str] = None


@dataclass_json
@dataclass
class DatasetRole(Role):
    id: DatasetId = field(
        default=DatasetId(-1),
        metadata={
            "dataclasses_json": {
                "decoder": lambda x: DatasetId(x),
                "encoder": lambda x: x["id"],
            }
        },
    )
    locked: Optional[bool] = field(default=None)
    type: PennsieveRole = PennsieveRole.DATASET_ROLE


@dataclass_json
@dataclass
class WorkspaceRole(Role):
    id: WorkspaceId = field(
        default=WorkspaceId(-1),
        metadata={
            "dataclasses_json": {
                "decoder": lambda x: WorkspaceId(x),
                "encoder": lambda x: x["id"],
            }
        },
    )
    type: PennsieveRole = PennsieveRole.WORKSPACE_ROLE


def role_from_dict(data):
    roles = {
        "organization_role": OrganizationRole,
        "dataset_role": DatasetRole,
        "workspace_role": WorkspaceRole,
    }
    return [roles[role["type"]].from_json(json.dumps(role)) for role in data]
