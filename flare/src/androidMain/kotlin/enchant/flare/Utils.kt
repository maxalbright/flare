package enchant.flare

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val Dispatchers.Background: CoroutineDispatcher get() = Dispatchers.IO