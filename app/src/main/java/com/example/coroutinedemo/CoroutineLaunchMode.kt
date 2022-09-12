package com.example.coroutinedemo

import kotlinx.coroutines.*

object CoroutineLaunchMode {

    /**
     * 协程创建后，立即开始调度，在调度前如果取消，将直接进入取消响应的状态
     * 输出:job cancel
     */
    fun launchModeDefault() = runBlocking {
        val job = launch(start = CoroutineStart.DEFAULT) {
            LogUtil.d("job start")
            launch {
                LogUtil.d("launch1")
            }
            launch {
                LogUtil.d("launch2")
            }
        }
        // 默认的启动方式能够cancel掉
        LogUtil.d("job cancel")
        job.cancel()
    }

    /**
     * 协程创建后，立即开始调度，执行到第一个挂起点之前不能响应取消
     * 输出:
     * job cancel
     * job start
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun launchModeAtomic() = runBlocking {
        val job = launch(start = CoroutineStart.ATOMIC) {
            // ATOMIC的启动方式能保证这句日志被打印
            LogUtil.d("job start")
            /*
            即使取消，在挂起点前的代码都可以被执行
             */
            launch {
                LogUtil.d("launch1")
            }
            launch {
                LogUtil.d("launch2")
            }
        }
        LogUtil.d("job cancel")
        job.cancel()
    }

    /**
     * 只有协程被需要时，包括主动调用start，join或者await函数时才会开始调度，如果调度前被取消，直接进入异常响应状态
     */
    fun launchModeLazy() = runBlocking {
        val job = launch(start = CoroutineStart.LAZY) {
            LogUtil.d("job start")
        }
        // 不调用start，协程不被调度，只被定义
        job.start()
    }

    /**
     * 协程创建后立即在当前函数调用栈中执行，直到遇到第一个挂起点
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun launchModeUnDispatch() = runBlocking {
        launch(context = Dispatchers.Main, start = CoroutineStart.UNDISPATCHED) {
            // thread is Thread[main,5,main]}
            LogUtil.d("thread is ${Thread.currentThread()}}")
            launch {
                LogUtil.d("inner coroutine,thread is ${Thread.currentThread()}")
            }
        }
    }
}