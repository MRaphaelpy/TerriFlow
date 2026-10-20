package com.mraphaelpy.terriflow.core.updater

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {

    @Test
    fun `remove link de changelog e marcacao`() {
        val body = "## Novidades\n- **Mapa**: quadras numeradas\n- Corrige [notificações](https://x)\n\n**Full Changelog**: https://github.com/a/b/compare/v1...v2"
        assertEquals("• Mapa: quadras numeradas\n• Corrige notificações", ReleaseNotes.clean(body))
    }

    @Test
    fun `texto padrao quando nao ha notas`() {
        assertEquals("Melhorias gerais e correções.", ReleaseNotes.clean("**Full Changelog**: https://x"))
    }
}
