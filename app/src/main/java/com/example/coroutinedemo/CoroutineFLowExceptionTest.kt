package com.example.coroutinedemo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import java.lang.ArithmeticException

object CoroutineFLowExceptionTest {

    private fun simpleFlow() = flow<Int> {
        for (i in 1..3) {
            LogUtil.d("emitting $i")
            emit(i)
        }
    }

    /**
     * 收集流时发生异常，并捕获
     * 2022-09-25 21:16:58.209 17176-17176/com.example.coroutinedemo D/west: emitting 1
     * 2022-09-25 21:16:58.210 17176-17176/com.example.coroutinedemo D/west: 1
     * 2022-09-25 21:16:58.210 17176-17176/com.example.coroutinedemo D/west: emitting 2
     * 2022-09-25 21:16:58.210 17176-17176/com.example.coroutinedemo D/west: 2
     * 2022-09-25 21:16:58.211 17176-17176/com.example.coroutinedemo D/west: caught java.lang.IllegalStateException: collected 2
     */
    fun testFlowException() = runBlocking {
        try {
            simpleFlow().collect {
                LogUtil.d(it.toString())
                // it == 2 时抛出异常
                check(it <= 1) {
                    "collected $it"
                }
            }
        } catch (e: Throwable) {
            LogUtil.d("caught $e")
        }
    }

    /**
     * 上游抛出异常，下游没有打印
     */
    fun testFlowException2() = runBlocking {
        flow {
            throw ArithmeticException("div 0")
            emit(1)
        }.flowOn(Dispatchers.IO)
            .collect {
                LogUtil.d("value -> $it")
            }
    }

    /**
     * 声明式捕获上游异常
     * 命令式是直接try/catch，也可以捕获，但是打破了flow的设计原则
     * 一般下游的异常可以直接try/catch，上游使用命令式捕获
     * 2022-09-25 21:26:06.325 21782-21782/com.example.coroutinedemo D/west: value -> 1
     * 2022-09-25 21:26:06.325 21782-21812/com.example.coroutinedemo D/west: caught java.lang.ArithmeticException: div 0
     */
    fun testFlowException3() = runBlocking {
        flow {
            emit(1)
            throw ArithmeticException("div 0")
        }.catch { e: Throwable ->
            LogUtil.d("caught $e")
//            emit(100) 默认值
        }.flowOn(Dispatchers.IO)
            .collect {
                LogUtil.d("value -> $it")
            }
    }
}