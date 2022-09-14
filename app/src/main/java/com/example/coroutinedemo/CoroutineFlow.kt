package com.example.coroutinedemo

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

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
}