from enum import Enum
from typing import List


class ModelType(Enum):
    @classmethod
    def values(cls):
        return [t.value for t in cls if isinstance(t, cls)]

    @classmethod
    def members(cls):
        return [t for t in cls if isinstance(t, cls)]


class Permission(ModelType):
    pass


class OrganizationLevelPermission(Permission):
    CREATE_DATASET_FROM_TEMPLATE = "create_dataset_from_template"


class DatasetPermission(Permission):
    VIEW_GRAPH_SCHEMA = "view_graph_schema"
    MANAGE_GRAPH_SCHEMA = "manage_graph_schema"
    MANAGE_MODEL_TEMPLATES = "manage_model_templates"
    MANAGE_DATASET_TEMPLATES = "manage_dataset_templates"
    PUBLISH_DATASET_TEMPLATE = "publish_dataset_template"
    CREATE_DELETE_RECORD = "create_delete_record"
    CREATE_DELETE_FILES = "create_delete_files"
    EDIT_RECORDS = "edit_records"
    EDIT_FILES = "edit_files"
    VIEW_RECORDS = "view_records"
    VIEW_FILES = "view_files"
    MANAGE_COLLECTIONS = "manage_collections"
    MANAGE_RECORD_RELATIONSHIPS = "manage_record_relationships"
    MANAGE_DATASET_COLLECTIONS = "manage_dataset_collections"
    ADD_PEOPLE = "add_people"
    CHANGE_ROLES = "change_roles"
    VIEW_PEOPLE_AND_ROLES = "view_people_and_roles"
    TRANSFER_OWNERSHIP = "transfer_ownership"
    RESERVE_DOI = "reserve_doi"
    MANAGE_ANNOTATIONS = "manage_annotations"
    MANAGE_ANNOTATION_LAYERS = "manage_annotation_layers"
    VIEW_ANNOTATIONS = "view_annotations"
    MANAGE_DISCUSSION_COMMENTS = "manage_discussion_comments"
    VIEW_DISCUSSION_COMMENTS = "view_discussion_comments"
    EDIT_DATASET_NAME = "edit_dataset_name"
    EDIT_DATASET_DESCRIPTION = "edit_dataset_description"
    EDIT_CONTRIBUTORS = "edit_contributors"
    EDIT_DATASET_AUTOMATICALLY_PROCESSING_PACKAGES = (
        "edit_dataset_automatically_processing_packages"
    )
    DELETE_DATASET = "delete_dataset"
    REQUEST_CANCEL_PUBLISH_REVISE = "request_cancel_publish_revise"
    REQUEST_REVISE = "request_revise"
    SHOW_SETTINGS_PAGE = "show_settings_page"
    VIEW_EXTERNAL_PUBLICATIONS = "view_external_publications"
    MANAGE_EXTERNAL_PUBLICATIONS = "manage_external_publications"
    VIEW_WEBHOOKS = "view_webhooks"
    MANAGE_WEBHOOKS = "manage_webhooks"
    TRIGGER_CUSTOM_EVENTS = "trigger_custom_events"

class CognitoSessionType(ModelType):
    BROWSER = "browser"
    API = "api"

class Role(ModelType):
    ORGANIZATION_ROLE = "organization_role"
    DATASET_ROLE = "dataset_role"
    WORKSPACE_ROLE = "workspace_role"
    NONE = ""


class RoleType(ModelType):
    VIEWER = "viewer"
    EDITOR = "editor"
    MANAGER = "manager"
    OWNER = "owner"

    @property
    def permissions(self):
        return RoleType.role_permissions()[self]

    def has_permission(self, permission: Permission):
        return permission in self.permissions

    def has_permissions(self, permissions: List[Permission]):
        return all([self.has_permission(permission) for permission in permissions])

    @classmethod
    def role_permissions(cls):
        viewer = [
            OrganizationLevelPermission.CREATE_DATASET_FROM_TEMPLATE,
            DatasetPermission.VIEW_GRAPH_SCHEMA,
            DatasetPermission.VIEW_FILES,
            DatasetPermission.VIEW_ANNOTATIONS,
            DatasetPermission.VIEW_RECORDS,
            DatasetPermission.VIEW_PEOPLE_AND_ROLES,
            DatasetPermission.MANAGE_DISCUSSION_COMMENTS,
            DatasetPermission.VIEW_DISCUSSION_COMMENTS,
            DatasetPermission.VIEW_EXTERNAL_PUBLICATIONS,
            DatasetPermission.VIEW_WEBHOOKS,
        ]
        editor = viewer + [
            DatasetPermission.CREATE_DELETE_RECORD,
            DatasetPermission.CREATE_DELETE_FILES,
            DatasetPermission.EDIT_RECORDS,
            DatasetPermission.EDIT_FILES,
            DatasetPermission.MANAGE_COLLECTIONS,
            DatasetPermission.MANAGE_RECORD_RELATIONSHIPS,
            DatasetPermission.MANAGE_ANNOTATIONS,
            DatasetPermission.MANAGE_ANNOTATION_LAYERS,
            DatasetPermission.TRIGGER_CUSTOM_EVENTS,
        ]
        manager = editor + [
            DatasetPermission.MANAGE_GRAPH_SCHEMA,
            DatasetPermission.MANAGE_MODEL_TEMPLATES,
            DatasetPermission.MANAGE_DATASET_TEMPLATES,
            DatasetPermission.PUBLISH_DATASET_TEMPLATE,
            DatasetPermission.ADD_PEOPLE,
            DatasetPermission.CHANGE_ROLES,
            DatasetPermission.EDIT_DATASET_NAME,
            DatasetPermission.EDIT_CONTRIBUTORS,
            DatasetPermission.EDIT_DATASET_DESCRIPTION,
            DatasetPermission.EDIT_DATASET_AUTOMATICALLY_PROCESSING_PACKAGES,
            DatasetPermission.SHOW_SETTINGS_PAGE,
            DatasetPermission.RESERVE_DOI,
            DatasetPermission.MANAGE_DATASET_COLLECTIONS,
            DatasetPermission.MANAGE_EXTERNAL_PUBLICATIONS,
            DatasetPermission.REQUEST_REVISE,
            DatasetPermission.MANAGE_WEBHOOKS,
        ]
        owner = manager + [
            DatasetPermission.TRANSFER_OWNERSHIP,
            DatasetPermission.DELETE_DATASET,
            DatasetPermission.REQUEST_CANCEL_PUBLISH_REVISE,
        ]
        return {
            cls.VIEWER: viewer,
            cls.EDITOR: editor,
            cls.MANAGER: manager,
            cls.OWNER: owner,
        }


class FeatureFlag(ModelType):
    TIME_SERIES_EVENTS_FEATURE = "time_series_events_feature"
    VIEWER2_FEATURE = "viewer2_feature"
    CONCEPTS_FEATURE = "concepts_feature"
    DISCOVER_FEATURE = "discover_feature"
    OLD_ETL = "old_etl"
    NEW_ETL = "new_etl"
    ETL_FAIRNESS = "etl_fairness"
    CLINICAL_MANAGEMENT_FEATURE = "clinical_management_feature"
    MODEL_TEMPLATES_FEATURE = "model_templates_feature"
    DATASET_TEMPLATES_FEATURE = "dataset_templates_feature"
    UPLOADS2_FEATURE = "uploads2_feature"
    PROGRESSION_TOOL_FEATURE = "progression_tool_feature"
    DISCOVER2_FEATURE = "discover2_feature"
    DOI_FEATURE = "doi_feature"
