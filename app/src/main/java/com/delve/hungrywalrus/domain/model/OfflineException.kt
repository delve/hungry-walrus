package com.delve.hungrywalrus.domain.model

/**
 * Thrown when a network operation fails due to the device being offline
 * (i.e. an [java.io.IOException] from the HTTP stack with no usable cached
 * fallback). See architecture §8.4 -- "No network (IOException)" handling.
 *
 * Placed in [com.delve.hungrywalrus.domain.model] alongside other cross-cutting
 * domain types. The architecture (§4) prescribes only two sub-packages under
 * `domain/` (`model/` and `usecase/`); this exception is neither a use case
 * nor a behaviourally-rich aggregate, so it lives in `model/` with the other
 * value-style domain types (enums, data classes).
 */
class OfflineException(
    message: String = "No network connection available",
) : Exception(message)
