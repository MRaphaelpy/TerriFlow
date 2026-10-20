package com.mraphaelpy.terriflow.domain.model

sealed class DomainException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotAuthenticated : DomainException("Usuário não autenticado")
    class TerritoryNotFound : DomainException("Território não encontrado")
    class UserNotFound : DomainException("Responsável não encontrado")
    class DuplicateTerritoryCode(code: String) : DomainException("Já existe um território com o código '$code'")
    class ActionNotAllowed(action: TerritoryAction) :
        DomainException("Ação não permitida: ${action.label()}")
    class AlreadyInCongregation(name: String) :
        DomainException("Você já pertence à congregação \"$name\". Não é possível criar outra.")
    class NotesNotAllowed : DomainException("Somente administradores e o dirigente atual podem gerenciar anotações")
    class EmptyNote : DomainException("Preencha o endereço ou a anotação")
    class InvalidCongregationCode(code: String) : DomainException("Código de congregação inválido: $code")
}

sealed class AuthException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidCredentials(cause: Throwable? = null) : AuthException("E-mail ou senha incorretos", cause)
    class UserNotFound(cause: Throwable? = null) : AuthException("Usuário não encontrado", cause)
    class EmailAlreadyInUse(cause: Throwable? = null) : AuthException("Este e-mail já está em uso", cause)
    class InvalidEmail(cause: Throwable? = null) : AuthException("E-mail inválido", cause)
    class WeakPassword(cause: Throwable? = null) : AuthException("A senha deve ter pelo menos 6 caracteres", cause)
    class Network(cause: Throwable? = null) : AuthException("Sem conexão com a internet", cause)
    class Unknown(cause: Throwable? = null) :
        AuthException("Erro: ${cause?.message ?: "tente novamente"}", cause)
}
