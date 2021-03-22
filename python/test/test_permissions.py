from auth_middleware.models import (
    RoleType,
    OrganizationLevelPermission,
    DatasetPermission,
)


def test_role_has_permission():
    assert RoleType.VIEWER.has_permission(
        OrganizationLevelPermission.CREATE_DATASET_FROM_TEMPLATE
    )


def test_role_no_permission():
    assert not RoleType.VIEWER.has_permission(DatasetPermission.DELETE_DATASET)


def test_role_all_permissions():
    assert RoleType.VIEWER.has_permissions(
        [
            OrganizationLevelPermission.CREATE_DATASET_FROM_TEMPLATE,
            DatasetPermission.VIEW_FILES,
            DatasetPermission.VIEW_RECORDS,
        ]
    )


def test_role_missing_permissions():
    assert not RoleType.VIEWER.has_permissions(
        [
            OrganizationLevelPermission.CREATE_DATASET_FROM_TEMPLATE,
            DatasetPermission.VIEW_FILES,
            DatasetPermission.VIEW_RECORDS,
            DatasetPermission.DELETE_DATASET,
        ]
    )
