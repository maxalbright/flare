package enchant.flare

import kotlinx.coroutines.*
import platform.Foundation.*
import platform.darwin.*
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
@ExperimentalCoroutinesApi
internal actual val Dispatchers.Background: CoroutineDispatcher
    get() = object : CoroutineDispatcher(), Delay {
        val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH.toLong(), 0)

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            dispatch_async(queue) {
                block.run()
            }
        }

        override fun scheduleResumeAfterDelay(
            timeMillis: Long,
            continuation: CancellableContinuation<Unit>
        ) {
            val time = dispatch_time(DISPATCH_TIME_NOW, (timeMillis * NSEC_PER_MSEC.toLong()))
            dispatch_after(time, queue) {
                with(continuation) {
                    resumeUndispatched(Unit)
                }
            }
        }
    }