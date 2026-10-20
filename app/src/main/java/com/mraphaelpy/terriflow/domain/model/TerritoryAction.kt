package com.mraphaelpy.terriflow.domain.model

enum class TerritoryAction { CREATE, ASSIGN, START, PAUSE, COMPLETE, RETURN, EDIT, DELETE }

fun TerritoryAction.label(): String = when (this) {
    TerritoryAction.CREATE -> "criar território"
    TerritoryAction.ASSIGN -> "atribuir território"
    TerritoryAction.START -> "iniciar trabalho"
    TerritoryAction.PAUSE -> "pausar trabalho"
    TerritoryAction.COMPLETE -> "finalizar território"
    TerritoryAction.RETURN -> "devolver território"
    TerritoryAction.EDIT -> "editar território"
    TerritoryAction.DELETE -> "apagar território"
}
