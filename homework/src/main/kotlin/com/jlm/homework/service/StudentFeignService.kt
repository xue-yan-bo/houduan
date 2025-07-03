package com.jlm.homework.service

import com.jlm.homework.feign.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 基于 OpenFeign 的学生服务
 * 替代原有的 NacosRemoteService 实现
 */
@Service
class StudentFeignService {

    private val logger = LoggerFactory.getLogger(StudentFeignService::class.java)

    @Autowired
    private lateinit var studentFeignClient: StudentFeignClient

    /**
     * 根据ID获取学生信息
     * @param studentId 学生ID
     * @return 学生信息
     */
    fun getStudentById(studentId: String): StudentInfo? {
        logger.info("通过Feign调用获取学生信息，学生ID: {}", studentId)
        
        return try {
            val response = studentFeignClient.getStudentById(studentId)
            if (response?.success == true) {
                logger.info("成功获取学生信息: {}", response.data?.name)
                response.data
            } else {
                logger.warn("获取学生信息失败: {}", response?.message)
                null
            }
        } catch (e: Exception) {
            logger.error("获取学生信息异常，学生ID: {}", studentId, e)
            null
        }
    }

    /**
     * 获取学生列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 学生列表数据
     */
    fun getStudentList(pageNum: Int = 1, pageSize: Int = 10): StudentListData? {
        logger.info("通过Feign调用获取学生列表，页码: {}, 每页大小: {}", pageNum, pageSize)

        return try {
            val response = studentFeignClient.getStudentList(pageNum, pageSize)
            logger.info("学生服务响应: {}", response)

            // 检查不同的成功标识
            val isSuccess = response?.success == true || response?.code == 200 || response?.code == 0

            if (isSuccess) {
                // 如果有 rows 字段，使用新格式
                if (response?.rows != null) {
                    val studentListData = StudentListData(
                        students = response.rows!!,
                        total = response.total ?: 0,
                        page = pageNum,
                        size = pageSize,
                        totalPages = (((response.total ?: 0) + pageSize - 1) / pageSize).toInt()
                    )
                    logger.info("成功获取学生列表，总数: {}", response.total)
                    return studentListData
                }
                // 如果有 data 字段，使用旧格式
                else if (response?.data != null) {
                    logger.info("成功获取学生列表，总数: {}", response.data?.total)
                    return response.data
                }
            }

            val errorMsg = response?.message ?: response?.msg ?: "未知错误"
            logger.warn("获取学生列表失败: {}", errorMsg)
            null
        } catch (e: Exception) {
            logger.error("获取学生列表异常", e)
            null
        }
    }

    /**
     * 创建学生
     * @param studentRequest 学生信息
     * @return 创建的学生信息
     */
    fun createStudent(studentRequest: CreateStudentRequest): StudentInfo? {
        logger.info("通过Feign调用创建学生: {}", studentRequest.name)
        
        return try {
            val response = studentFeignClient.createStudent(studentRequest)
            if (response?.success == true) {
                logger.info("成功创建学生: {}", response.data?.name)
                response.data
            } else {
                logger.warn("创建学生失败: {}", response?.message)
                null
            }
        } catch (e: Exception) {
            logger.error("创建学生异常", e)
            null
        }
    }

    /**
     * 更新学生信息
     * @param studentId 学生ID
     * @param studentRequest 更新的学生信息
     * @return 更新后的学生信息
     */
    fun updateStudent(studentId: String, studentRequest: UpdateStudentRequest): StudentInfo? {
        logger.info("通过Feign调用更新学生信息，学生ID: {}", studentId)
        
        return try {
            val response = studentFeignClient.updateStudent(studentId, studentRequest)
            if (response?.success == true) {
                logger.info("成功更新学生信息: {}", response.data?.name)
                response.data
            } else {
                logger.warn("更新学生信息失败: {}", response?.message)
                null
            }
        } catch (e: Exception) {
            logger.error("更新学生信息异常，学生ID: {}", studentId, e)
            null
        }
    }

    /**
     * 删除学生
     * @param studentId 学生ID
     * @return 删除是否成功
     */
    fun deleteStudent(studentId: String): Boolean {
        logger.info("通过Feign调用删除学生，学生ID: {}", studentId)
        
        return try {
            val response = studentFeignClient.deleteStudent(studentId)
            if (response?.success == true) {
                logger.info("成功删除学生，学生ID: {}", studentId)
                true
            } else {
                logger.warn("删除学生失败: {}", response?.message)
                false
            }
        } catch (e: Exception) {
            logger.error("删除学生异常，学生ID: {}", studentId, e)
            false
        }
    }

    /**
     * 检查学生服务是否可用
     * @return 服务可用性
     */
    fun isStudentServiceAvailable(): Boolean {
        return try {
            val healthResponse = studentFeignClient.healthCheck()
            val status = healthResponse?.get("status") as? String
            val isAvailable = status == "UP"
            logger.info("学生服务健康检查结果: {}", status)
            isAvailable
        } catch (e: Exception) {
            logger.error("学生服务健康检查失败", e)
            false
        }
    }

    /**
     * 获取服务健康状态详情
     * @return 健康状态详情
     */
    fun getServiceHealthDetails(): Map<String, Any> {
        return try {
            val healthResponse = studentFeignClient.healthCheck()
            healthResponse ?: mapOf(
                "status" to "UNKNOWN",
                "message" to "无法获取健康状态"
            )
        } catch (e: Exception) {
            logger.error("获取服务健康状态详情失败", e)
            mapOf(
                "status" to "DOWN",
                "message" to "服务调用异常: ${e.message}"
            )
        }
    }

    /**
     * 批量获取学生信息
     * @param studentIds 学生ID列表
     * @return 学生信息列表
     */
    fun getStudentsByIds(studentIds: List<String>): List<StudentInfo> {
        logger.info("批量获取学生信息，数量: {}", studentIds.size)
        
        return studentIds.mapNotNull { studentId ->
            try {
                getStudentById(studentId)
            } catch (e: Exception) {
                logger.warn("获取学生信息失败，学生ID: {}", studentId, e)
                null
            }
        }
    }

    /**
     * 搜索学生
     * @param keyword 搜索关键词
     * @param page 页码
     * @param size 每页大小
     * @return 搜索结果
     */
    fun searchStudents(keyword: String, page: Int = 1, size: Int = 10): StudentListData? {
        logger.info("搜索学生，关键词: {}, 页码: {}, 每页大小: {}", keyword, page, size)
        
        // 这里可以根据实际的API接口调整
        // 目前使用基本的列表接口，实际项目中可能需要专门的搜索接口
        return getStudentList(page, size)
    }
}
