package com.example.coroutinedemo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Created by wangzhiping on 2022/9/14.
 */
object CoroutineFlow {

    private fun simpleList(): List<Int> = listOf(1, 2, 3)

    /**
     * 需求：异步地，一秒钟返回一个值
     */
    fun simpleSequence() = sequence {
        for (i in 1..3) {
            Thread.sleep(1000) //阻塞
//            kotlinx.coroutines.delay(1000) 必须要在挂起函数调用
            yield(i)
        }
    }

    /**
     * 返回了多个值，但不是异步
     */
    fun testMultipleValues() {
        simpleList().forEach {
            println(it)
        }
    }

    /**
     * 异步返回多个值
     */
    private suspend fun simpleFlow() = flow {
        for (i in 1..3) {
            // 这里没有阻塞
            delay(1000)
            emit(i)
        }
    }

    /**
     * 收集同步事件发送的列表
     * 2022-09-14 19:01:35.739 20553-20553 System.out              com.example.coroutinedemo            I  west 1
     * 2022-09-14 19:01:36.780 20553-20553 System.out              com.example.coroutinedemo            I  west 2
     * 2022-09-14 19:01:37.813 20553-20553 System.out              com.example.coroutinedemo            I  west 3
     * 2022-09-14 19:01:37.858 20553-20553 System.out              com.example.coroutinedemo            I  west i'm not blocked.
     * 2022-09-14 19:01:39.402 20553-20553 System.out              com.example.coroutinedemo            I  west i'm not blocked.
     * 2022-09-14 19:01:40.943 20553-20553 System.out              com.example.coroutinedemo            I  west i'm not blocked.
     * 对比testMultipleValues4，这里的队列产生方式，阻塞了主线程，导致west i'm not blocked没有及时打印
     */
    fun testMultipleValues3() {
        GlobalScope.launch(Dispatchers.Main) {
            launch {
                for (i in 1..3) {
                    println("west i'm not blocked.")
                    delay(1500)
                }
            }
            simpleSequence().forEach { value ->
                println("west $value")
            }
        }
    }

    /**
     * 收集flow发射的值
     * 2022-09-14 18:46:17.078 15209-15272 System.out              com.example.coroutinedemo            I  west i'm not blocked.
     * 2022-09-14 18:46:18.083 15209-15271 System.out              com.example.coroutinedemo            I  west 1
     * 2022-09-14 18:46:18.581 15209-15271 System.out              com.example.coroutinedemo            I  west i'm not blocked.
     * 2022-09-14 18:46:19.085 15209-15271 System.out              com.example.coroutinedemo            I  west 2
     * 2022-09-14 18:46:20.083 15209-15271 System.out              com.example.coroutinedemo            I  west i'm not blocked.
     * 2022-09-14 18:46:20.086 15209-15271 System.out              com.example.coroutinedemo            I  west 3
     * 对比testMultipleValues3，这里的队列产生方式，没有阻塞了主线
     */
    fun testMultipleValues4() {
        GlobalScope.launch(Dispatchers.Main) {
            launch {
                for (i in 1..3) {
                    println("west i'm not blocked.")
                    delay(1500)
                }
            }
            simpleFlow().collect { value ->
                println("west $value")
            }
        }
    }

    private fun simpleFlow2() = flow<Int> {
        println("west flow started")
        for (i in 1..3) {
            delay(1000)
            emit(i)
        }
    }

    /**
     * 2022-09-17 15:09:51.919 26493-26493/com.example.coroutinedemo I/System.out: west calling collect...
     * 2022-09-17 15:09:51.921 26493-26493/com.example.coroutinedemo I/System.out: west flow started
     * 2022-09-17 15:09:52.977 26493-26493/com.example.coroutinedemo I/System.out: west 1
     * 2022-09-17 15:09:54.009 26493-26493/com.example.coroutinedemo I/System.out: west 2
     * 2022-09-17 15:09:55.043 26493-26493/com.example.coroutinedemo I/System.out: west 3
     * 2022-09-17 15:09:55.044 26493-26493/com.example.coroutinedemo I/System.out: west calling collect again...
     * 2022-09-17 15:09:55.045 26493-26493/com.example.coroutinedemo I/System.out: west flow started
     * 2022-09-17 15:09:56.057 26493-26493/com.example.coroutinedemo I/System.out: west 1
     * 2022-09-17 15:09:57.071 26493-26493/com.example.coroutinedemo I/System.out: west 2
     * 2022-09-17 15:09:58.094 26493-26493/com.example.coroutinedemo I/System.out: west 3
     */
    fun testFlowIsCold() = runBlocking {
        val flow = simpleFlow2()
        println("west calling collect...")
        flow.collect {
            println("west $it")
        }
        println("west calling collect again...")
        flow.collect {
            println("west $it")
        }
    }

    /**
     * 2022-09-17 15:19:31.869 29586-29586/com.example.coroutinedemo I/System.out: west collect string 2
     * 2022-09-17 15:19:31.869 29586-29586/com.example.coroutinedemo I/System.out: west collect string 4
     */
    fun testFlowContinuation() = runBlocking {
        (1..5).asFlow().filter {
            it % 2 == 0
        }.map {
            "string $it"
        }.collect {
            println("west collect $it")
        }
    }

    /**
     * 2022-09-17 15:21:49.204 29934-29934/com.example.coroutinedemo I/System.out: west1
     * 2022-09-17 15:21:50.240 29934-29934/com.example.coroutinedemo I/System.out: west2
     * 2022-09-17 15:21:51.283 29934-29934/com.example.coroutinedemo I/System.out: west3
     */
    fun testFlowBuilder() = runBlocking {
        (1..3).asFlow()
            .onEach {
                delay(1000)
            }
            .collect {
                println("west$it")
            }
    }

    private fun simpleFlow3() = flow {
        println("west flow started ${Thread.currentThread()}")
        for (i in 1..3) {
            delay(1000)
            emit(i)
        }
    }

    /**
     * 流发射数据的携程作用域，默认使用收集数据时的协程作用域
     * 2022-09-17 15:28:10.644 32658-32658/com.example.coroutinedemo I/System.out: west testFlowContext started.
     * 2022-09-17 15:28:10.645 32658-32658/com.example.coroutinedemo I/System.out: west flow started Thread[main,5,main]
     * 2022-09-17 15:28:11.648 32658-32658/com.example.coroutinedemo I/System.out: west 1, Thread Thread[main,5,main]
     * 2022-09-17 15:28:12.649 32658-32658/com.example.coroutinedemo I/System.out: west 2, Thread Thread[main,5,main]
     * 2022-09-17 15:28:13.650 32658-32658/com.example.coroutinedemo I/System.out: west 3, Thread Thread[main,5,main]
     */
    fun testFlowContext() = runBlocking {
        println("west testFlowContext started.")
        simpleFlow3().collect {
            println("west $it, Thread ${Thread.currentThread()}")
        }
    }

    /**
     * 将流的发射指定为在io线程进行，流的收集依然在主线程
     * 2022-09-17 15:45:05.018 13584-13584/com.example.coroutinedemo I/System.out: west testFlowContext started.
     * 2022-09-17 15:45:05.068 13584-13614/com.example.coroutinedemo I/System.out: west flow started Thread[DefaultDispatcher-worker-1,5,main]
     * 2022-09-17 15:45:06.112 13584-13584/com.example.coroutinedemo I/System.out: west 1, Thread Thread[main,5,main]
     * 2022-09-17 15:45:07.155 13584-13584/com.example.coroutinedemo I/System.out: west 2, Thread Thread[main,5,main]
     * 2022-09-17 15:45:08.193 13584-13584/com.example.coroutinedemo I/System.out: west 3, Thread Thread[main,5,main]
     */
    private fun simpleFlow4() = flow {
        println("west flow started ${Thread.currentThread()}")
        for (i in 1..3) {
            delay(1000)
            emit(i)
        }
    }.flowOn(Dispatchers.IO)

    fun testFlowContext4() = runBlocking {
        println("west testFlowContext started.")
        simpleFlow4().collect {
            println("west $it, Thread ${Thread.currentThread()}")
        }
    }

    private fun events() = (1..3)
        .asFlow()
        .onEach {
            delay(1000)
        }
        .flowOn(Dispatchers.Default)

    /**
     * 不调用collect，没有任何输出
     * launchIn可以代替collect启动流的收集
     * 2022-09-17 15:57:04.235 20324-20324/com.example.coroutinedemo I/System.out: west event: 1, Thread: main
     * 2022-09-17 15:57:05.279 20324-20324/com.example.coroutinedemo I/System.out: west event: 2, Thread: main
     * 2022-09-17 15:57:06.297 20324-20324/com.example.coroutinedemo I/System.out: west event: 3, Thread: main
     */
    fun testFlowLaunch() = runBlocking {
        events().onEach { event ->
            println("west event: ${event}, Thread: ${Thread.currentThread().name}")
        }.launchIn(CoroutineScope(Dispatchers.Main))
    }

    /**
     * 2022-09-17 16:00:56.097 21862-21862/com.example.coroutinedemo I/System.out: west 1
     * 2022-09-17 16:00:57.134 21862-21862/com.example.coroutinedemo I/System.out: west 2
     * 2022-09-17 16:00:57.607 21862-21862/com.example.coroutinedemo I/System.out: west done
     */
    fun testCancelFlow() = runBlocking {
        withTimeoutOrNull(2500) {
            simpleFlow4().collect {
                println("west $it")
            }
        }
        println("west done")
    }

    /**
     * 2022-09-17 16:03:16.013 22153-22183/com.example.coroutinedemo I/System.out: west flow started Thread[DefaultDispatcher-worker-1,5,main]
     * 2022-09-17 16:03:17.062 22153-22153/com.example.coroutinedemo I/System.out: west value 1
     */
    fun testCancelFlowCheck() = runBlocking {
        simpleFlow4().collect {
            println("west value $it")
            if (it == 1) {
                cancel()
            }
        }
    }

    /**
     * 出于性能考虑，大多数其他流不会自行执行取消检测，所以以下代码，如果没有声明cancellable，即使调用了cancel()，也可能来不及取消
     *
     * 声明了cancellable后，调用了cancel，便进入了取消中状态，后续不再收集
     */
    fun testCancellableFlow() = runBlocking {
        /*
        2022-09-17 16:07:55.606 22402-22402/com.example.coroutinedemo D/west: 1
        2022-09-17 16:07:55.608 22402-22402/com.example.coroutinedemo D/west: 2
        2022-09-17 16:07:55.609 22402-22402/com.example.coroutinedemo D/west: 3
        2022-09-17 16:07:55.609 22402-22402/com.example.coroutinedemo D/west: 4
        2022-09-17 16:07:55.609 22402-22402/com.example.coroutinedemo D/west: 5
         */
//        (1..5).asFlow()
//            .collect {
//                LogUtil.d("$it")
//                cancel()
//            }

        /*
        2022-09-17 16:10:10.194 22529-22529/com.example.coroutinedemo D/west: 1
         */
        (1..5).asFlow()
            .cancellable()
            .collect {
                LogUtil.d("$it")
                cancel()
            }
    }
}