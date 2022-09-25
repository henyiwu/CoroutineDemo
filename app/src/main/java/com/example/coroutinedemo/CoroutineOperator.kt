package com.example.coroutinedemo

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private fun numbers() = flow<Int> {
        try {
            emit(1)
            emit(2)
            LogUtil.d("this line will not execute")
            emit(3)
        } finally {
            LogUtil.d("finally runs")
        }
    }

    /**
     * 限制长度操作符
     * 2022-09-22 13:01:16.891 14983-15022 west                    com.example.coroutinedemo            D  1
     * 2022-09-22 13:01:16.891 14983-15022 west                    com.example.coroutinedemo            D  2
     * 2022-09-22 13:01:16.891 14983-15022 west                    com.example.coroutinedemo            D  finally runs
     */
    fun testLimitLengthOperator() {
        GlobalScope.launch {
            numbers().take(2).collect {
                LogUtil.d(it.toString())
            }
        }
    }

    /**
     * 末端操作符
     * 把1..5的平方数累加
     * 2022-09-22 13:03:55.155 15186-15224 west                    com.example.coroutinedemo            D  Reduce a:1, b:4
     * 2022-09-22 13:03:55.155 15186-15224 west                    com.example.coroutinedemo            D  Reduce a:5, b:9
     * 2022-09-22 13:03:55.155 15186-15224 west                    com.example.coroutinedemo            D  Reduce a:14, b:16
     * 2022-09-22 13:03:55.155 15186-15224 west                    com.example.coroutinedemo            D  Reduce a:30, b:25
     */
    fun testTerminalOperator() {
        GlobalScope.launch {
            (1..5).asFlow()
                .map { it * it }
                .reduce { a, b ->
                    LogUtil.d("Reduce a:$a, b:$b")
                    a + b
                }
        }
    }

    /**
     * 组合多个流
     * ---------------------------- PROCESS STARTED (15911) for package com.example.coroutinedemo ----------------------------
     * 数字和字符串是异步发出
     * 2022-09-22 13:13:29.547 15911-15949 west                    com.example.coroutinedemo            D  1, one at 560 ms from start.
     * 2022-09-22 13:13:30.088 15911-15948 west                    com.example.coroutinedemo            D  2, two at 1101 ms from start.
     * 2022-09-22 13:13:30.629 15911-15949 west                    com.example.coroutinedemo            D  3, three at 1642 ms from start.
     */
    fun testZipOperator() {
        GlobalScope.launch {
            val numbers = (1..3).asFlow().onEach { delay(300) }
            val strings = flowOf("one", "two", "three").onEach { delay(500) }
            val startTime = System.currentTimeMillis()
            numbers.zip(strings) { a, b ->
                "$a, $b"
            }.collect {
                LogUtil.d("$it at ${System.currentTimeMillis() - startTime} ms from start.")
            }
        }
    }

    private fun requestFlow(i: Int) = flow {
        emit("i:${i}, first")
        delay(500)
        emit("i:${i}, second")
    }

    /**
     * flow展平
     * 2022-09-25 20:54:25.723 6853-6853/com.example.coroutinedemo D/west: i:1, first at 149 ms from start
     * 2022-09-25 20:54:26.258 6853-6853/com.example.coroutinedemo D/west: i:1, second at 685 ms from start
     * 2022-09-25 20:54:26.402 6853-6853/com.example.coroutinedemo D/west: i:2, first at 829 ms from start
     * 2022-09-25 20:54:26.943 6853-6853/com.example.coroutinedemo D/west: i:2, second at 1370 ms from start
     * 2022-09-25 20:54:27.084 6853-6853/com.example.coroutinedemo D/west: i:3, first at 1511 ms from start
     * 2022-09-25 20:54:27.585 6853-6853/com.example.coroutinedemo D/west: i:3, second at 2012 ms from start
     */
    fun testFlatMapConcat() = runBlocking {
        val startTime = System.currentTimeMillis()
        (1..3).asFlow()
            .onEach {
                delay(100)
            }
            .flatMapConcat {
                requestFlow(it)
            }
            .collect {
                LogUtil.d("$it at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }

    /**
     * flow合并
     * 2022-09-25 21:01:49.997 10631-10631/com.example.coroutinedemo D/west: i:1, first at 588 ms from start
     * 2022-09-25 21:01:50.537 10631-10631/com.example.coroutinedemo D/west: i:1, second at 1129 ms from start
     * 2022-09-25 21:01:50.538 10631-10631/com.example.coroutinedemo D/west: i:2, first at 1130 ms from start
     * 2022-09-25 21:01:51.084 10631-10631/com.example.coroutinedemo D/west: i:2, second at 1675 ms from start
     * 2022-09-25 21:01:51.084 10631-10631/com.example.coroutinedemo D/west: i:3, first at 1676 ms from start
     * 2022-09-25 21:01:51.595 10631-10631/com.example.coroutinedemo D/west: i:3, second at 2186 ms from start
     */
    fun testFlatMapMerge() = runBlocking {
        val startTime = System.currentTimeMillis()
        (1..3).asFlow()
            .onEach {
                delay(500)
            }
            .flatMapMerge {
                requestFlow(it)
            }
            .collect {
                LogUtil.d("$it at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }

    /**
     * 2022-09-25 21:06:17.863 12867-12867/com.example.coroutinedemo D/west: i:1, first at 197 ms from start
     * 2022-09-25 21:06:18.010 12867-12867/com.example.coroutinedemo D/west: i:2, first at 344 ms from start
     * 2022-09-25 21:06:18.133 12867-12867/com.example.coroutinedemo D/west: i:3, first at 466 ms from start
     * 2022-09-25 21:06:18.674 12867-12867/com.example.coroutinedemo D/west: i:3, second at 1008 ms from start
     */
    fun testFlatMapLatest() = runBlocking {
        val startTime = System.currentTimeMillis()
        (1..3).asFlow()
            .onEach {
                delay(100)
            }
            .flatMapLatest {
                requestFlow(it)
            }
            .collect {
                LogUtil.d("$it at ${System.currentTimeMillis() - startTime} ms from start")
            }
    }
}