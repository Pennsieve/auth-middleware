// Copyright (c) 2021 University of Pennsylvania. All Rights Reserved.

package com.pennsieve.auth.middleware

import com.pennsieve.models.Role
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PermissionSpec extends AnyWordSpec with Matchers {

  "permissions" should {
    "be accepted if the role has a given permission" in {
      Permission.hasPermission(Role.Viewer)(
        OrganizationLevelPermission.CreateDatasetFromTemplate
      ) shouldBe (true)
    }

    "be rejected if the role does not have a given permission" in {
      Permission.hasPermission(Role.Viewer)(DatasetPermission.DeleteDataset) shouldBe (false)
    }

    "be accepted if the role has all given permissions" in {
      Permission.hasPermissions(Role.Viewer)(
        Set(
          OrganizationLevelPermission.CreateDatasetFromTemplate,
          DatasetPermission.ViewFiles,
          DatasetPermission.ViewRecords
        )
      ) shouldBe (true)
    }

    "be rejected if the role does not have all given permissions" in {
      Permission.hasPermissions(Role.Viewer)(
        Set(
          OrganizationLevelPermission.CreateDatasetFromTemplate,
          DatasetPermission.ViewFiles,
          DatasetPermission.ViewRecords,
          DatasetPermission.DeleteDataset
        )
      ) shouldBe (false)
    }

    // Guest grants no permissions. Previously Role.Guest had no case in
    // rolePermissions and these calls threw a MatchError at runtime.
    "be rejected for a Guest role (no permissions), not throw" in {
      Permission.hasPermission(Role.Guest)(
        OrganizationLevelPermission.CreateDatasetFromTemplate
      ) shouldBe (false)
      Permission.hasPermission(Role.Guest)(DatasetPermission.ViewFiles) shouldBe (false)
    }

    "treat a Guest role as having every permission absent" in {
      val allPermissions: Set[Permission] = Set(
        OrganizationLevelPermission.CreateDatasetFromTemplate,
        DatasetPermission.ViewFiles,
        DatasetPermission.ViewRecords,
        DatasetPermission.ViewAnnotations,
        DatasetPermission.DeleteDataset,
        DatasetPermission.AddPeople
      )
      allPermissions.foreach { permission =>
        Permission.hasPermission(Role.Guest)(permission) shouldBe (false)
      }
    }

    "accept the empty permission set for a Guest role" in {
      // hasPermissions is a forall, so the empty set is vacuously satisfied
      // for any role, including Guest — this must not throw.
      Permission.hasPermissions(Role.Guest)(Set.empty) shouldBe (true)
    }

    "reject any non-empty permission set for a Guest role" in {
      Permission.hasPermissions(Role.Guest)(Set(DatasetPermission.ViewFiles)) shouldBe (false)
    }
  }
}
