package com.coldfier.aws.v2.retrofit.aws.s3.v2.client.internal

import com.coldfier.aws.v2.retrofit.aws.s3.v2.client.AwsCredentials
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class AwsCredentialsStore(private val credentialsUpdater: suspend () -> AwsCredentials) {

    var accessKey: String = ""
        private set

    var secretKey: String = ""
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