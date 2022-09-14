package com.example.coroutinedemo

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlin.coroutines.CoroutineContext

/**
 * Created by wangzhiping on 2022/9/13.
 */
class GlobalCoroutineExceptionHandler : CoroutineExceptionHandler {

    override val key = CoroutineExceptionHandler

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        println("un handle exception $exception")
    }
}