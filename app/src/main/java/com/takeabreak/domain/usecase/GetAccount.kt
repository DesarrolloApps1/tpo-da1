package com.takeabreak.domain.usecase

import com.takeabreak.domain.model.Account
import com.takeabreak.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

class GetAccount(private val auth: AuthRepository) {
    operator fun invoke(): Flow<Account?> = auth.account.distinctUntilChanged()
}
