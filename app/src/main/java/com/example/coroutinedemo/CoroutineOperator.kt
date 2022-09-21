package com.example.coroutinedemo

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking

object CoroutineOperator {

    private suspend fun performRequest(request: Int): String {
        delay(1000)
        return "response $request"
    }

    /**
     * map操作符
     * 2022-09-21 23:50:04.878 7153-7153/com.example.coroutinedemo D/west: map
     * 2022-09-21 23:50:05.879 7153-7153/com.example.coroutinedemo D/west: response 1
     * 2022-09-21 23:50:05.879 7153-7153/com.example.coroutinedemo D/west: map
     * 2022-09-21 23:50:06.880 7153-7153/com.example.coroutinedemo D/west: response 2
     * 2022-09-21 23:50:06.880 7153-7153/com.example.coroutinedemo D/west: map
     * 2022-09-21 23:50:07.881 7153-7153/com.example.coroutinedemo D/west: response 3
     */
    fun testMapFlowOperator() = runBlocking {
        (1..3).asFlow()
            .map { request ->
                LogUtil.d("map")
                performRequest(request)
            }
            .collect {
                LogUtil.d(it)
            }
    }

    /**
     * transform操作符
     * 2022-09-21 23:52:44.308 7741-7741/com.example.coroutinedemo D/west: Making request 1
     * 2022-09-21 23:52:45.319 7741-7741/com.example.coroutinedemo D/west: response 1
     * 2022-09-21 23:52:45.320 7741-7741/com.example.coroutinedemo D/west: Making request 2
     * 2022-09-21 23:52:46.362 7741-7741/com.example.coroutinedemo D/west: response 2
     * 2022-09-21 23:52:46.362 7741-7741/com.example.coroutinedemo D/west: Making request 3
     * 2022-09-21 23:52:47.391 7741-7741/com.example.coroutinedemo D/west: response 3
     */
    fun testTransformFlowOperator() = runBlocking {
        (1..3).asFlow()
            .transform { request ->
                emit("Making request $request")
                emit(performRequest(request))
            }
            .collect {
                LogUtil.d(it)
            }
    }
}