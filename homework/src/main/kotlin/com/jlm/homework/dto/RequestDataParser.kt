package com.jlm.homework.dto

/**
 * 请求数据解析器
 * 负责解析前端传递的各种格式数据（数组、逗号分隔字符串等）
 */
object RequestDataParser {

    /**
     * 解析字符串列表
     * 智能处理多种输入格式，兼容前端传递字符串或数组
     */
    fun parseStringList(input: Any?): List<String> {
        return when (input) {
            is List<*> -> input.mapNotNull { it?.toString()?.trim() }.filter { it.isNotBlank() }
            is String -> input.split(",").map { it.trim() }.filter { it.isNotBlank() }
            else -> emptyList()
        }
    }

    /**
     * 解析长整型列表
     * 智能处理多种输入格式，兼容前端传递数组或逗号分隔字符串
     * @param input 输入数据
     * @param fallbackValue 当解析失败时的备用单值
     */
    fun parseLongList(input: Any?, fallbackValue: Long? = null): List<Long> {
        return when (input) {
            is List<*> -> input.mapNotNull {
                when (it) {
                    is Number -> it.toLong()
                    is String -> it.trim().toLongOrNull()
                    else -> null
                }
            }
            is String -> input.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { it.toLongOrNull() }
            else -> fallbackValue?.let { listOf(it) } ?: emptyList()
        }
    }

    /**
     * 解析整型列表
     * 智能处理多种输入格式，兼容前端传递数组或逗号分隔字符串
     */
    fun parseIntList(input: Any?): List<Int> {
        return when (input) {
            is List<*> -> input.mapNotNull {
                when (it) {
                    is Number -> it.toInt()
                    is String -> it.trim().toIntOrNull()
                    else -> null
                }
            }
            is String -> input.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { it.toIntOrNull() }
            else -> emptyList()
        }
    }
} 