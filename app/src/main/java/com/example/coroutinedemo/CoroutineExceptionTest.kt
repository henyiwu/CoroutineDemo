package com.example.coroutinedemo

import kotlinx.coroutines.*
import java.io.IOException
import java.lang.ArithmeticException
import java.lang.AssertionError
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.lang.IndexOutOfBoundsException

/**
 * Created by wangzhiping on 2022/9/13.
 * 协程的构建器有两种形式：
 * 1. 自动传播异常
 * 2. 向用户暴露异常
 * 当这些构建器用于创建一个根协程时，前者会在发生异常的第一时间抛出，后者依赖用户消费异常，例如通过await或receive
 */
object CoroutineExceptionTest {

    fun testCoroutineContext() = runBlocking {
        // 指定调度器和协程名字，'+'是重载运算符
        launch(Dispatchers.Default + CoroutineName("test")) {
            println("i'm working in thread ${Thread.currentThread().name}")
        }
    }

    /**
     * 子协程继承父协程的上下文（协程名，调度器）
     * 2022-09-13 16:09:48.803 29209-29347/com.gzik.pandora I/System.out: StandaloneCoroutine{Active}@d275801 currentThread: Thread[DefaultDispatcher-worker-2,5,main]
     * 2022-09-13 16:09:48.804 29209-29348/com.gzik.pandora I/System.out: DeferredCoroutine{Active}@856f6a6 currentThread: Thread[DefaultDispatcher-worker-3,5,main]
     */
    fun testCoroutineContextExtend() = runBlocking {
        // 指定调度器和协程名字，'+'是重载运算符
        val scope = CoroutineScope(Job() + Dispatchers.IO + CoroutineName("test"))
        val job = scope.launch {
            println("${coroutineContext[Job]} currentThread: ${Thread.currentThread()}")
            val result = async {
                println("${coroutineContext[Job]} currentThread: ${Thread.currentThread()}")
                "ok"
            }.await()
        }
        job.join()
    }

    /**
     * 协程上下文包括
     * 1. job控制协程的生命周期
     * 2. 调度器，io、main、default
     * 3. 协程名
     * 4. 异常处理器
     */
    fun testCoroutineContextExtend2() = runBlocking {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught $exception")
        }
        val scope = CoroutineScope(Job() + Dispatchers.Main + coroutineExceptionHandler)
        val job = scope.launch(Dispatchers.IO) {
            // 调度器是io，即协程上下文的继承规则是，默认从父协程继承，子协程定义的上下文会覆盖继承的上下文
        }
    }

    /**
     * 通过launch和async方式启动的协程，catch的位置不同
     */
    fun testExceptionPropagation() = runBlocking<Unit> {
        val job = GlobalScope.launch {
            try {
                throw IndexOutOfBoundsException()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        job.join()
        val deferred = GlobalScope.async {
            throw ArithmeticException()
        }

        try {
            deferred.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 非根协程的异常，一定会抛出
     * async启动的协程，没有调用await，也抛出了异常
     *
     * 当一个协程由于一个异常而运行失败时，它会传播这个异常并传递给它的父级，接下来父级会进行以下操作
     * 1. 取消它自己的子级
     * 2. 取消它自己
     * 3. 将异常传播给自己的父级
     */
    fun testExceptionPropagation2() = runBlocking<Unit> {
        val scope = CoroutineScope(Job())
        val job = scope.launch {
            async {
                throw IllegalAccessException()
            }
        }
        job.join()
    }

    /**
     * 使用SupervisorJob()，job1抛出异常后，job2没有被取消，继续运行
     */
    fun testSupervisorJob() = runBlocking {
        val supervisor = CoroutineScope(SupervisorJob())
        val job1 = supervisor.launch {
            delay(100)
            println("child 1")
            throw IllegalArgumentException()
        }
        val job2 = supervisor.launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                println("child2 finished.")
            }
        }
        joinAll(job1, job2)
    }

    /**
     * supervisorScope的子协程发生异常，不会影响兄弟协程序
     * 但如果是自身发生异常，所有子协程都会停止
     */
    fun testSupervisorScope() = runBlocking {
        supervisorScope {
            launch {
                delay(100)
                println("child 1")
                throw IllegalArgumentException()
            }
            // 如果在这里抛出异常，所有子协程都会取消
            try {
                repeat(1000) {
                    delay(200)
                    println("time $it")
                }
            } finally {
                println("child 2 finished.")
            }
        }
    }

    /**
     * supervisorScope作用域抛出的异常，会取消子协程
     */
    fun testSupervisorScope2() = runBlocking<Unit> {
        supervisorScope {
            val child = launch {
                try {
                    println("the child is sleeping")
                    delay(Long.MAX_VALUE)
                } finally {
                    println("the child is cancelled")
                }
            }
            yield()
            println("throwing an exception from the scope")
            throw AssertionError()
        }
    }

    /**
     * 异常捕获测试1
     * job能够被捕获
     * deferred不能被捕获
     */
    fun testCoroutineExceptionHandler() = runBlocking<Unit> {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught $exception")
        }
        val job = GlobalScope.launch(coroutineExceptionHandler) {
            // 这个异常被捕获，符合两个条件
            // 1. launch
            // 2. 根协程
            throw AssertionError()
        }
        val deferred = GlobalScope.async(coroutineExceptionHandler) {
            // 这个异常没被捕获
            throw ArithmeticException()
        }
        job.join()
        deferred.await()
    }

    /**
     * 异常捕获测试2
     * 异常可以被捕获
     */
    fun testCoroutineExceptionHandler2() = runBlocking {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught $exception")
        }
        val scope = CoroutineScope(Job())
        val job = scope.launch(coroutineExceptionHandler) {
            launch {
                throw IllegalArgumentException()
            }
        }
        job.join()
    }

    /**
     * 异常捕获测试3
     * 把异常处理器，放到子协程中，异常没有被捕获
     * 即异常处理器要放在根协程上（经过测试，只有在根协程设置异常）
     */
    fun testCoroutineExceptionHandler3() = runBlocking {
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, exception ->
            println("caught $exception")
        }
        val scope = CoroutineScope(Job())
        val job = scope.launch {
            launch(coroutineExceptionHandler) {
                launch {
                    // 异常没有被捕获
                    throw IllegalArgumentException()
                }
            }
        }
        job.join()
    }

    /**
     * 子协程child取消后，父协程没有被取消
     * 除CancellationException外，子协程发生其他异常都会向父协程抛出
     */
    fun testCancelAndException() = runBlocking {
        val job = launch {
            val child = launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    println("child is cancelled")
                }
            }
            yield()
            println("cancelling child")
            child.cancelAndJoin()
            yield()
            println("parent is not cancelled")
        }
        job.join()
    }

    /**
     * 父协程要等待子协程结束之后，再捕获到异常
     * 2022-09-13 18:31:21.189 29767-30157/com.gzik.pandora D/west: [, , 0]:second child throws an exception
     * 2022-09-13 18:31:21.191 29767-29838/com.gzik.pandora D/west: [, , 0]:children are cancelled, but exception is not handled until all terminate
     * 2022-09-13 18:31:21.293 29767-29838/com.gzik.pandora D/west: [, , 0]:the first child finished its non cancellable block
     * 2022-09-13 18:31:21.294 29767-29838/com.gzik.pandora D/west: [, , 0]:caught java.lang.ArithmeticException
     */
    fun testCancelAndException2() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("caught $exception")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    withContext(NonCancellable) {
                        println("children are cancelled, but exception is not handled until all terminate")
                        delay(100)
                        println("the first child finished its non cancellable block")
                    }
                }
            }
            launch {
                delay(10)
                println("second child throws an exception")
                throw ArithmeticException()
            }
        }
        job.join()
    }

    /**
     * 异常聚合
     * 2022-09-13 18:40:16.850 31881-32352/com.gzik.pandora I/System.out: caught java.io.IOException, [java.lang.ArithmeticException, java.lang.IndexOutOfBoundsException]
     */
    fun testExceptionAggregation() = runBlocking {
        val handler = CoroutineExceptionHandler { _, exception ->
            println("caught $exception, ${exception.suppressed.contentToString()}")
        }
        val job = GlobalScope.launch(handler) {
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    throw ArithmeticException() // 2
                }
            }
            launch {
                try {
                    delay(Long.MAX_VALUE)
                } finally {
                    throw IndexOutOfBoundsException() // 3
                }
            }
            launch {
                delay(100)
                throw IOException() // 1
            }
        }
        job.join()
    }
}