package com.gzik.pandora

import com.meelive.ingkee.logger.IKLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

/**
 * Created by wangzhiping on 2022/9/13.
 */
class GlobalCoroutineExceptionHandler : CoroutineExceptionHandler{

    override val key = CoroutineExceptionHandler

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        IKLog.d("west", "un handle exception $exception")
    }
}