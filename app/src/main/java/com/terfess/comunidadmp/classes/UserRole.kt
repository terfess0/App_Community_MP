package com.terfess.comunidadmp.classes


object UserRole {
    private var currentUserRole = Roles.USER
    fun setUserRole(newRole:Roles){ currentUserRole = newRole }
    fun getCurrentUserRole(): Roles {
        return currentUserRole
    }
}