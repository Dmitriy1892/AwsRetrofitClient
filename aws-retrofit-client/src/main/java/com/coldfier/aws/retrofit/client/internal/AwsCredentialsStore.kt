package com.coldfier.aws.retrofit.client.internal

import com.coldfier.aws.retrofit.client.AwsCredentials
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AwsCredentialsStore(
    initialCredentials: AwsCredentials,
    private val credentialsUpdater: suspend () -> AwsCredentials
) {

    var accessKey: String = initialCredentials.accessKey
        private set

    var secretKey: String = initialCredentials.secretKey
        private set

    private val mutex = Mutex()

    suspend fun updateCredentials() {
        mutex.withLock {
            val newCredentials = credentialsUpdater()
            this.accessKey = newCredentials.accessKey
            this.secretKey = newCredentials.secretKey
        }
    }
}