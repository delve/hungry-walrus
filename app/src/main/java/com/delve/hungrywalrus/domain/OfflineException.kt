package com.delve.hungrywalrus.domain

/**
 * Thrown when a network operation fails due to the device being offline.
 */
class OfflineException(
    message: String = "No network connection available",
) : Exception(message)
