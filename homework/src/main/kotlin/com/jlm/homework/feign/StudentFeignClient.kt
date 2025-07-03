package com.jlm.homework.feign

import com.jlm.homework.feign.fallback.StudentFeignClientFallback
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

/**
 * 学生服务 Feign 客户端
 * 用于调用 jlm-student 服务的接口
 */
@FeignClient(
    name = "jlm-student",
    fallback = StudentFeignClientFallback::class,
    configuration = [FeignConfiguration::class]
)
interface StudentFeignClient {

    /**
     * 根据ID获取学生信息
     * @param studentId 学生ID
     * @return 学生信息
     */
    @GetMapping("/prod-api/student/student/{studentId}")
    fun getStudentById(@PathVariable("studentId") studentId: String): StudentResponse?

    /**
     * 获取学生列表
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 学生列表
     */
    @GetMapping("/student/list")
    fun getStudentList(
        @RequestParam("pageNum", defaultValue = "1") pageNum: Int,
        @RequestParam("pageSize", defaultValue = "10") pageSize: Int
    ): StudentListResponse?

    /**
     * 创建学生
     * @param studentRequest 学生信息
     * @return 创建结果
     */
    @PostMapping("/prod-api/student/student")
    fun createStudent(@RequestBody studentRequest: CreateStudentRequest): StudentResponse?

    /**
     * 更新学生信息
     * @param studentId 学生ID
     * @param studentRequest 学生信息
     * @return 更新结果
     */
    @PutMapping("/prod-api/student/student/{studentId}")
    fun updateStudent(
        @PathVariable("studentId") studentId: String,
        @RequestBody studentRequest: UpdateStudentRequest
    ): StudentResponse?

    /**
     * 删除学生
     * @param studentId 学生ID
     * @return 删除结果
     */
    @DeleteMapping("/prod-api/student/student/{studentId}")
    fun deleteStudent(@PathVariable("studentId") studentId: String): ApiResponse?

    /**
     * 健康检查
     * @return 健康状态
     */
    @GetMapping("/actuator/health")
    fun healthCheck(): Map<String, Any>?
}

/**
 * 通用API响应
 */
data class ApiResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: Any? = null,
    val timestamp: Long? = null,
    val code: Int? = null,
    val msg: String? = null
)

/**
 * 学生信息响应
 */
data class StudentResponse(
    val success: Boolean? = null,
    val message: String? = null,
    val data: StudentInfo? = null,
    val timestamp: Long? = null,
    val code: Int? = null,
    val msg: String? = null
)

/**
 * 学生列表响应
 */
data class StudentListResponse(
    val total: Long? = null,
    val rows: List<StudentInfo>? = null,
    val code: Int? = null,
    val msg: String? = null,
    val success: Boolean? = null,
    val message: String? = null,
    val data: StudentListData? = null,
    val timestamp: Long? = null
)

/**
 * 学生信息
 */
data class StudentInfo(
    val studentId: Long? = null,
    val studentName: String? = null,
    val studentCode: String? = null,
    val studentIdCard: String? = null,
    val sex: String? = null,
    val classesId: Long? = null,
    val classesName: String? = null,
    val classesCode: String? = null,
    val gradeId: Long? = null,
    val gradeName: String? = null,
    val schoolId: Long? = null,
    val userId: String? = null,
    val studentStatusNumber: String? = null,
    val studentImage: String? = null,
    val enrolledData: String? = null,
    val studentType: String? = null,
    val mainLinkmanName: String? = null,
    val mainLinkmanPhone: String? = null,
    val studentStatus: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
    val createBy: String? = null,
    val updateBy: String? = null,
    // 兼容旧格式
    val id: String? = null,
    val name: String? = null,
    val age: Int? = null,
    val grade: String? = null,
    val className: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
)

/**
 * 学生列表数据
 */
data class StudentListData(
    val students: List<StudentInfo>,
    val total: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int
)

/**
 * 创建学生请求
 */
data class CreateStudentRequest(
    val name: String,
    val age: Int? = null,
    val grade: String? = null,
    val className: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
)

/**
 * 更新学生请求
 */
data class UpdateStudentRequest(
    val name: String? = null,
    val age: Int? = null,
    val grade: String? = null,
    val className: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null
)
