// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.models.Role
import com.pennsieve.models.Role._

sealed trait Permission

object DatasetPermission {
  case object ViewGraphSchema extends Permission
  case object ManageGraphSchema extends Permission
  case object ManageModelTemplates extends Permission
  case object ManageDatasetTemplates extends Permission
  case object PublishDatasetTemplate extends Permission
  case object CreateDeleteRecord extends Permission
  case object CreateDeleteFiles extends Permission
  case object EditRecords extends Permission
  case object EditFiles extends Permission
  case object ViewRecords extends Permission
  case object ViewFiles extends Permission
  case object ManageCollections extends Permission
  case object ManageRecordRelationships extends Permission
  case object ManageDatasetCollections extends Permission
  case object AddPeople extends Permission
  case object ChangeRoles extends Permission
  case object ViewPeopleAndRoles extends Permission
  case object TransferOwnership extends Permission
  case object ReserveDoi extends Permission
  case object ManageAnnotations extends Permission
  case object ManageAnnotationLayers extends Permission
  case object ViewAnnotations extends Permission
  case object ManageDiscussionComments extends Permission
  case object ViewDiscussionComments extends Permission
  case object EditContributors extends Permission
  case object EditDatasetName extends Permission
  case object EditDatasetDescription extends Permission
  case object EditDatasetAutomaticallyProcessingPackages extends Permission
  case object DeleteDataset extends Permission
  case object RequestRevise extends Permission
  case object RequestCancelPublishRevise extends Permission
  case object ShowSettingsPage extends Permission
  case object ViewExternalPublications extends Permission
  case object ManageExternalPublications extends Permission
  case object ViewWebhooks extends Permission
  case object ManageWebhooks extends Permission
}

object OrganizationLevelPermission {
  case object CreateDatasetFromTemplate extends Permission
}

object ClinicalTrialPermission {
  case object TrialOverview extends Permission
  case object ViewParticipants extends Permission
  case object CreateParticipant extends Permission
  case object EditParticipant extends Permission
  case object DeleteParticipant extends Permission
  case object AddFilesToRecord extends Permission
  case object LinkSiteToRecord extends Permission
  case object CreateSubmissionData extends Permission
  case object ViewSubmissionData extends Permission
  case object ManageModels extends Permission
  case object ShowSettingsPage extends Permission
  case object ManageSites extends Permission
  case object ViewSites extends Permission
}

object WorkspacePermission {
  case object ManageViews extends Permission
  case object ManageQueries extends Permission
  case object CreateSnapshot extends Permission
  case object ViewDashboard extends Permission
}

object Permission {
  private def rolePermissions(role: Role): Set[Permission] = role match {
    case Viewer =>
      Set(
        OrganizationLevelPermission.CreateDatasetFromTemplate,
        DatasetPermission.ViewGraphSchema,
        DatasetPermission.ViewRecords,
        DatasetPermission.ViewFiles,
        DatasetPermission.ViewAnnotations,
        DatasetPermission.ViewPeopleAndRoles,
        DatasetPermission.ManageDiscussionComments,
        DatasetPermission.ViewDiscussionComments,
        DatasetPermission.ViewExternalPublications,
        DatasetPermission.ViewWebhooks,
        ClinicalTrialPermission.TrialOverview,
        ClinicalTrialPermission.ViewParticipants,
        ClinicalTrialPermission.ViewSubmissionData,
        ClinicalTrialPermission.ViewSites,
        WorkspacePermission.ViewDashboard
      )
    case Editor =>
      rolePermissions(Viewer) ++ Set(
        DatasetPermission.CreateDeleteRecord,
        DatasetPermission.CreateDeleteFiles,
        DatasetPermission.EditRecords,
        DatasetPermission.EditFiles,
        DatasetPermission.ManageCollections,
        DatasetPermission.ManageRecordRelationships,
        DatasetPermission.ManageAnnotations,
        DatasetPermission.ManageAnnotationLayers,
        ClinicalTrialPermission.CreateParticipant,
        ClinicalTrialPermission.EditParticipant,
        ClinicalTrialPermission.DeleteParticipant,
        ClinicalTrialPermission.AddFilesToRecord,
        ClinicalTrialPermission.LinkSiteToRecord,
        ClinicalTrialPermission.CreateSubmissionData
      )
    case Manager =>
      rolePermissions(Editor) ++ Set(
        DatasetPermission.ManageGraphSchema,
        DatasetPermission.ManageModelTemplates,
        DatasetPermission.ManageDatasetTemplates,
        DatasetPermission.PublishDatasetTemplate,
        DatasetPermission.AddPeople,
        DatasetPermission.ChangeRoles,
        DatasetPermission.EditDatasetName,
        DatasetPermission.EditDatasetDescription,
        DatasetPermission.EditDatasetAutomaticallyProcessingPackages,
        DatasetPermission.EditContributors,
        DatasetPermission.ShowSettingsPage,
        DatasetPermission.RequestRevise,
        DatasetPermission.ReserveDoi,
        DatasetPermission.ManageDatasetCollections,
        DatasetPermission.ManageExternalPublications,
        DatasetPermission.ManageWebhooks,
        ClinicalTrialPermission.ShowSettingsPage,
        ClinicalTrialPermission.ManageModels,
        ClinicalTrialPermission.ManageSites,
        WorkspacePermission.ManageViews,
        WorkspacePermission.ManageQueries,
        WorkspacePermission.CreateSnapshot
      )
    case Owner =>
      rolePermissions(Manager) ++ Set(
        DatasetPermission.TransferOwnership,
        DatasetPermission.DeleteDataset,
        DatasetPermission.RequestCancelPublishRevise
      )
  }

  def hasPermission(role: Role)(permission: Permission): Boolean =
    rolePermissions(role) contains permission
  def hasPermissions(role: Role)(permissions: Set[Permission]): Boolean =
    permissions.forall(hasPermission(role))
}
