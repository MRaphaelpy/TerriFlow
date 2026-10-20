package com.mraphaelpy.terriflow.domain.policy

import com.mraphaelpy.terriflow.domain.model.UserRole
import com.mraphaelpy.terriflow.testutil.Fixtures.admin
import com.mraphaelpy.terriflow.testutil.Fixtures.otherPublisher
import com.mraphaelpy.terriflow.testutil.Fixtures.publisher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserManagementPolicyTest {

    private val superAdmin = admin.copy(id = "super", role = UserRole.SUPER_ADMIN)

    @Test
    fun `renomear a si mesmo ou como admin`() {
        assertTrue(UserManagementPolicy.canRename(publisher, publisher))
        assertTrue(UserManagementPolicy.canRename(admin, publisher))
        assertFalse(UserManagementPolicy.canRename(publisher, otherPublisher))
    }

    @Test
    fun `mudancas de papel respeitam o super admin`() {
        assertEquals(listOf(UserRole.ADMIN), UserManagementPolicy.roleChanges(admin, publisher))
        assertEquals(listOf(UserRole.RESPONSIBLE), UserManagementPolicy.roleChanges(superAdmin, admin))
        assertTrue(UserManagementPolicy.roleChanges(admin, superAdmin).isEmpty())
        assertTrue(UserManagementPolicy.roleChanges(admin, admin).isEmpty())
        assertTrue(UserManagementPolicy.roleChanges(publisher, otherPublisher).isEmpty())
    }

    @Test
    fun `ativar e desativar`() {
        assertTrue(UserManagementPolicy.canToggleActive(admin, publisher))
        assertFalse(UserManagementPolicy.canToggleActive(admin, admin))
        assertFalse(UserManagementPolicy.canToggleActive(admin, superAdmin))
        assertFalse(UserManagementPolicy.canToggleActive(publisher, otherPublisher))
    }
}
