package com.example.coroutinedemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.*
import kotlin.coroutines.*
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        originalCreateCoroutine()
//        useMainScope()
//        testCoroutineBuilder()
//        testCoroutineJoin()
//        testCoroutineAwait()
//        testAsync()
//        CoroutineLaunchMode.launchModeDefault()
//        CoroutineLaunchMode.launchModeAtomic()
//        CoroutineLaunchMode.launchModeLazy()
        CoroutineLaunchMode.launchModeUnDispatch()
//        CoroutineFlow.testMultipleValues4()
//        CoroutineFlow.testCoroutineBackPressWithCollectLast()
//        CoroutineOperator.testMapFlowOperator()
//        CoroutineOperator.testTransformFlowOperator()
//        CoroutineOperator.testZipOperator()
//        CoroutineOperator.testFlatMapConcat()
//        CoroutineOperator.testFlatMapMerge()
//        CoroutineOperator.testFlatMapLatest()
//        CoroutineFLowExceptionTest.testFlowException()
//        CoroutineFLowExceptionTest.testFlowException3()
    }

    /**
     * 原生创建协程
     */
    private fun originalCreateCoroutine() {
        val continuation = suspend { // suspend方法创建协程
            5
        }.createCoroutine(object : Continuation<Int> {
            override val context: CoroutineContext
                get() = EmptyCoroutineContext

            override fun resumeWith(result: Result<Int>) {
                LogUtil.d("Continuation $result")
            }
        })
        // 启动协程
        continuation.resume(Unit)
    }

    private fun useMainScope() {
        launch {
            try {
                delay(10000)
            } catch (e: Exception) {
                // west: e kotlinx.coroutines.JobCancellationException:
                // Job was cancelled; job=SupervisorJobImpl{Cancelling}@18958cf
                LogUtil.d("e $e")
            }
        }
    }

    /**
     * runBlocking会等待子协程执行完毕
     */
    private fun testCoroutineBuilder() = runBlocking {
        val job1 = launch {
            delay(200)
            LogUtil.d("job1 finished.")
        }
        val job2 = async {
            delay(200)
            LogUtil.d("job2 finished.")
            "job2 finish"
        }
        println(job2.await())
    }

    /**
     * 测试join()
     * job1执行完之后，job2和job3再同时执行
     */
    private fun testCoroutineJoin() = runBlocking {
        val job1 = launch {
            delay(200)
            LogUtil.d("One")
        }
        job1.join()
        val job2 = launch {
            delay(2000)
            LogUtil.d("Two")
        }
        val job3 = launch {
            delay(2000)
            LogUtil.d("Three")
        }
    }

    /**
     * job1执行完之后，job2和job3同时执行
     */
    private fun testCoroutineAwait() = runBlocking {
        val job1 = async {
            delay(200)
            LogUtil.d("One")
        }
        job1.await()
        val job2 = launch {
            delay(2000)
            LogUtil.d("Two")
        }
        val job3 = launch {
            delay(2000)
            LogUtil.d("Three")
        }
    }

    /**
     * 结构化并发
     */
    private fun testAsync() = runBlocking {
//        val time = measureTimeMillis {
//            val one = doOne()
//            val two = doTwo()
//            LogUtil.d("The result is ${one + two}")
//        }
//        LogUtil.d("completed in $time mills")
//        结果：2s

        val time = measureTimeMillis {
            val one = async {
                doOne()
            }
            val two = async {
                doTwo()
            }
            LogUtil.d("The result is ${one.await() + two.await()}")
        }
        LogUtil.d("completed in $time mills")
        /*
        2022-09-12 17:52:13.170 6448-6448/com.example.coroutinedemo D/west: The result is 3
        2022-09-12 17:52:13.170 6448-6448/com.example.coroutinedemo D/west: completed in 1052 mills
         */
    }

    private suspend fun doOne(): Int {
        delay(1000)
        return 2
    }

    private suspend fun doTwo(): Int {
        delay(1000)
        return 1
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}