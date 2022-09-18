package com.example.coroutinedemo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

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
     * 声明了cancellable后，调用了cancel，便进入了取消中状态，后续不再收集，抛出异常，程序退出
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

    fun simpleFlow8() = flow<Int> {
        for (i in 1..3) {
            delay(100)
            emit(i)
            LogUtil.d("emitting $i ${Thread.currentThread().name}")
        }
    }

    /**
     * 流的背压
     * "背压"指的是在数据流生产与消耗的过程中，生产的速度比消耗的速度快
     * 2022-09-18 23:37:32.271 30513-30513/com.example.coroutinedemo D/west: collected 1 main
     * 2022-09-18 23:37:32.272 30513-30513/com.example.coroutinedemo D/west: emitting 1 main
     * 2022-09-18 23:37:32.754 30513-30513/com.example.coroutinedemo D/west: collected 2 main
     * 2022-09-18 23:37:32.754 30513-30513/com.example.coroutinedemo D/west: emitting 2 main
     * 2022-09-18 23:37:33.231 30513-30513/com.example.coroutinedemo D/west: collected 3 main
     * 2022-09-18 23:37:33.231 30513-30513/com.example.coroutinedemo D/west: emitting 3 main
     * 2022-09-18 23:37:33.231 30513-30513/com.example.coroutinedemo D/west: 1459
     * 收集流数据耗时1400多毫秒，即至少(100+300)*3
     */
    fun testCoroutineBackPress() = runBlocking {
        val time = measureTimeMillis {
            simpleFlow8()
                .collect {
                    delay(300)
                    LogUtil.d("collected $it ${Thread.currentThread().name}")
                }
        }
        LogUtil.d(time.toString())
    }

    /**
     * 使用缓冲区
     * 2022-09-18 23:40:08.000 30845-30845/com.example.coroutinedemo D/west: emitting 1 main
     * 2022-09-18 23:40:08.140 30845-30845/com.example.coroutinedemo D/west: emitting 2 main
     * 2022-09-18 23:40:08.278 30845-30845/com.example.coroutinedemo D/west: emitting 3 main
     * 2022-09-18 23:40:08.342 30845-30845/com.example.coroutinedemo D/west: collected 1 main
     * 2022-09-18 23:40:08.684 30845-30845/com.example.coroutinedemo D/west: collected 2 main
     * 2022-09-18 23:40:09.022 30845-30845/com.example.coroutinedemo D/west: collected 3 main
     * 2022-09-18 23:40:09.081 30845-30845/com.example.coroutinedemo D/west: 1255
     * 耗时减少为1200+
     * 即flow数据流，是在主线程同时发出的，同时发出数据1..3，耗时100ms，收集数据耗时300ms*3
     */
    fun testCoroutineBackPressWithBuffer() = runBlocking {
        val time = measureTimeMillis {
            simpleFlow8()
                .buffer(50)
                .collect {
                    delay(300)
                    LogUtil.d("collected $it ${Thread.currentThread().name}")
                }
        }
        LogUtil.d(time.toString())
    }

    /**
     * 上述testCoroutineBackPressWithBuffer()也可以用flowOn进行优化，达到相同的效果
     * 即流的发射在后台线程进行，执行延时100ms后挂起协程，并发地执行3个协程
     * 2022-09-18 23:49:20.916 4137-4245/com.example.coroutinedemo D/west: emitting 1 DefaultDispatcher-worker-1
     * 2022-09-18 23:49:21.045 4137-4245/com.example.coroutinedemo D/west: emitting 2 DefaultDispatcher-worker-1
     * 2022-09-18 23:49:21.181 4137-4245/com.example.coroutinedemo D/west: emitting 3 DefaultDispatcher-worker-1
     * 2022-09-18 23:49:21.251 4137-4137/com.example.coroutinedemo D/west: collected 1 main
     * 2022-09-18 23:49:21.592 4137-4137/com.example.coroutinedemo D/west: collected 2 main
     * 2022-09-18 23:49:21.933 4137-4137/com.example.coroutinedemo D/west: collected 3 main
     * 2022-09-18 23:49:22.000 4137-4137/com.example.coroutinedemo D/west: 1292
     * @see testCoroutineBackPressWithBuffer
     */
    fun testCoroutineBackPressWithFlowOn() = runBlocking {
        val time = measureTimeMillis {
            simpleFlow8()
                .flowOn(Dispatchers.IO)
                .collect {
                    delay(300)
                    LogUtil.d("collected $it ${Thread.currentThread().name}")
                }
        }
        LogUtil.d(time.toString())
    }

    /**
     * conflate:合并，取最新的数据，但一定会取第一个和最后一个数据
     * 不处理每一项数据，只取最新的数据，在实例中，跳过了i=2的情况
     * 2022-09-18 23:55:10.782 6381-6381/com.example.coroutinedemo D/west: emitting 1 main
     * 2022-09-18 23:55:10.923 6381-6381/com.example.coroutinedemo D/west: emitting 2 main
     * 2022-09-18 23:55:11.052 6381-6381/com.example.coroutinedemo D/west: emitting 3 main
     * 2022-09-18 23:55:11.112 6381-6381/com.example.coroutinedemo D/west: collected 1 main
     * 2022-09-18 23:55:11.450 6381-6381/com.example.coroutinedemo D/west: collected 3 main
     * 2022-09-18 23:55:11.521 6381-6381/com.example.coroutinedemo D/west: 923
     */
    fun testCoroutineBackPressWithConflate() = runBlocking {
        val time = measureTimeMillis {
            simpleFlow8()
                .conflate()
                .collect {
                    delay(300)
                    LogUtil.d("collected $it ${Thread.currentThread().name}")
                }
        }
        LogUtil.d(time.toString())
    }

    /**
     * 2022-09-19 00:05:00.180 8260-8260/com.example.coroutinedemo D/west: emitting 1 main
     * 2022-09-19 00:05:00.316 8260-8260/com.example.coroutinedemo D/west: emitting 2 main
     * 2022-09-19 00:05:00.456 8260-8260/com.example.coroutinedemo D/west: emitting 3 main
     * 2022-09-19 00:05:00.602 8260-8260/com.example.coroutinedemo D/west: emitting 4 main
     * 2022-09-19 00:05:00.744 8260-8260/com.example.coroutinedemo D/west: emitting 5 main
     * 2022-09-19 00:05:00.890 8260-8260/com.example.coroutinedemo D/west: emitting 6 main
     * 2022-09-19 00:05:01.032 8260-8260/com.example.coroutinedemo D/west: emitting 7 main
     * 2022-09-19 00:05:01.178 8260-8260/com.example.coroutinedemo D/west: emitting 8 main
     * 2022-09-19 00:05:01.321 8260-8260/com.example.coroutinedemo D/west: emitting 9 main
     * 2022-09-19 00:05:01.460 8260-8260/com.example.coroutinedemo D/west: emitting 10 main
     * 2022-09-19 00:05:01.799 8260-8260/com.example.coroutinedemo D/west: collected 10 main
     * 2022-09-19 00:05:01.871 8260-8260/com.example.coroutinedemo D/west: 1834
     */
    fun testCoroutineBackPressWithCollectLast() = runBlocking {
        val time = measureTimeMillis {
            simpleFlow8()
                .collectLatest {
                    delay(300)
                    LogUtil.d("collected $it ${Thread.currentThread().name}")
                }
        }
        LogUtil.d(time.toString())
    }
}