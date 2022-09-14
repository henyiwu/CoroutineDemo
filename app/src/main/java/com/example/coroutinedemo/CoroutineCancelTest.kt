package com.example.coroutinedemo

import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.FileReader

/**
 * Created by wangzhiping on 2022/9/13.
 */
object CoroutineCancelTest {

    /**
     * CoroutineScope创建的协程作用域有自己的上下文，不会继承runBlocking的上下文
     * 所以runBlocking不会等待CoroutineScope任务执行结束
     */
    fun testScopeCancel() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            delay(1000)
            println("job 1")
        }

        scope.launch {
            delay(1000)
            println("job 2")
        }
        delay(100)
        scope.cancel()
        delay(2000)
    }

    /**
     * job1取消后，不会影响job2运行
     */
    fun testBrotherCancel() = runBlocking {
        val scope = CoroutineScope(Dispatchers.Default)
        val job1 = scope.launch {
            delay(1000)
            println("job 1")
        }
        val job2 = scope.launch {
            delay(1000)
            println("job 2")
        }
        delay(100)
        job1.cancel()
        delay(2000)
    }

    /**
     * GlobalScope有自己的上下文，不会继承runBlocking的上下文
     * 如果加入join()，runBlocking便会等待job执行完成
     */
    fun testCancellationException() = runBlocking {
        val job = GlobalScope.launch {
            try {
                delay(1000) // 取消后这里会抛出异常，可以被捕获
                println("job 1")
            } catch (e: CancellationException) {
                e.printStackTrace()
            }
        }
        job.cancel(CancellationException("取消"))
        job.join()
//        可以合并为一个cancelAndJoin()的api，
//        job.cancelAndJoin()
    }

    /**
     * cpu密集型任务，没有取消成功
     * 2022-09-13 14:48:01.020 29009-29065/com.gzik.pandora D/west: [, , 0]:job : i'm sleeping 0 ...
     * 2022-09-13 14:48:01.519 29009-29065/com.gzik.pandora D/west: [, , 0]:job : i'm sleeping 1 ...
     * 2022-09-13 14:48:02.019 29009-29065/com.gzik.pandora D/west: [, , 0]:job : i'm sleeping 2 ...
     * 2022-09-13 14:48:02.321 29009-29009/com.gzik.pandora D/west: [, , 0]:main: i'm tried of waiting!
     * 2022-09-13 14:48:02.519 29009-29065/com.gzik.pandora D/west: [, , 0]:job : i'm sleeping 3 ...
     * 2022-09-13 14:48:03.019 29009-29065/com.gzik.pandora D/west: [, , 0]:job : i'm sleeping 4 ...
     * 2022-09-13 14:48:03.019 29009-29009/com.gzik.pandora D/west: [, , 0]:main: now i can quit.
     */
    fun testCancelCpuTask() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) {
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job : i'm sleeping ${i++} ...")
                    nextPrintTime += 500
                }
            }
        }
        delay(1300)
        println("main: i'm tried of waiting!")
        job.cancelAndJoin()
        println("main: now i can quit.")
    }

    /**
     * 取消cpu密集型任务
     * 调用cancel后协程会变为isAlive = false状态
     * 2022-09-13 15:00:57.036 30632-30695/com.gzik.pandora I/System.out: job : i'm sleeping 0 ...
     * 2022-09-13 15:00:57.535 30632-30695/com.gzik.pandora I/System.out: job : i'm sleeping 1 ...
     * 2022-09-13 15:00:58.035 30632-30695/com.gzik.pandora I/System.out: job : i'm sleeping 2 ...
     * 2022-09-13 15:00:58.337 30632-30632/com.gzik.pandora I/System.out: main: i'm tried of waiting!
     * 2022-09-13 15:00:58.337 30632-30632/com.gzik.pandora I/System.out: main: now i can quit.
     */
    fun testCancelCpuTaskByIsActive() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5 && isActive) {
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job : i'm sleeping ${i++} ...")
                    nextPrintTime += 500
                }
            }
//            可以优化为
//            while (i < 5) {
//                ensureActive() 抛出异常CancellationException，被静默处理掉
//                if (System.currentTimeMillis() >= nextPrintTime) {
//                    println("job : i'm sleeping ${i++} ...")
//                    nextPrintTime += 500
//                }
//            }
        }
        delay(1300)
        println("main: i'm tried of waiting!")
        job.cancelAndJoin() // cancel()能使协程进入cancelling状态（isActive = false, isCancelled = true）
        println("main: now i can quit.")
    }

    /**
     * yield会出让线程的执行权，并抛出异常
     * 2022-09-13 15:00:57.036 30632-30695/com.gzik.pandora I/System.out: job : i'm sleeping 0 ...
     * 2022-09-13 15:00:57.535 30632-30695/com.gzik.pandora I/System.out: job : i'm sleeping 1 ...
     * 2022-09-13 15:00:58.035 30632-30695/com.gzik.pandora I/System.out: job : i'm sleeping 2 ...
     * 2022-09-13 15:00:58.337 30632-30632/com.gzik.pandora I/System.out: main: i'm tried of waiting!
     * 2022-09-13 15:00:58.337 30632-30632/com.gzik.pandora I/System.out: main: now i can quit.
     */
    fun testCancelCpuTaskByYield() = runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0
            while (i < 5) {
                // 抛出JobCancellationException
                yield()
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job : i'm sleeping ${i++} ...")
                    nextPrintTime += 500
                }
            }
        }
        delay(1300)
        println("main: i'm tried of waiting!")
        job.cancelAndJoin() // cancel()能使协程进入cancelling状态（isActive = false, isCancelled = true）
        println("main: now i can quit.")
    }

    /**
     * 协程取消后，如何释放资源
     * 在finally里释放资源
     */
    fun testCancelReleaseResource() = runBlocking {
        val job = launch {
            try {
                repeat(1000) {
                    println("job: i'm sleeping $it...")
                    delay(500)
                }
            } finally {
                println("job: i'm running finally")
            }
        }
        delay(1300)
        println("main: i'm tried of waiting!")
        job.cancelAndJoin()
        println("main: now i can quit.")
    }

    /**
     * 测试标准函数use
     */
    fun testUseFunction() = runBlocking {
        val br = BufferedReader(FileReader("text.txt"))
        with(br) {
            var line: String?
//            try {
//                while (true) {
//                    line = readLine()?:break
//                    println(line)
//                }
//            } finally {
//                close()
//            }
            // 可优化为
            use {
                while (true) {
                    line = readLine()?:break
                    println(line)
                }
            }
        }
    }

    /**
     * 不可取消的任务
     */
    fun testCancelWithNonCancellable() = runBlocking {
        val job = launch {
            try {
                repeat(1000) {
                    println("job: i'm sleeping $it ...")
                    delay(500L)
                }
            } finally {
                withContext(NonCancellable) { // 这个作用域内的协程不能被取消
                    println("job: i'm running finally")
                    delay(1000L)
                    println("job: i'm non cancellable")
                }
            }
        }
        delay(1300)
        println("main: i'm tried of waiting!")
        job.cancelAndJoin()
        println("main: now i can quit.")
    }

    /**
     * 超时任务测试
     * 2022-09-13 15:48:11.280 27591-27591/com.gzik.pandora D/west: [, , 0]:e kotlinx.coroutines.TimeoutCancellationException: Timed out waiting for 1300 ms
     * 超时抛出异常，没有catch程序没有崩溃
     */
    fun testDealWithTimeOut() = runBlocking {
        withTimeout(1300) {
            repeat(1000) {
                println("job: i'm sleeping $it ...")
                delay(500)
            }
        }
    }
}