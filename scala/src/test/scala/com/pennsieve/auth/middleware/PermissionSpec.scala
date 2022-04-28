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
  }
}
