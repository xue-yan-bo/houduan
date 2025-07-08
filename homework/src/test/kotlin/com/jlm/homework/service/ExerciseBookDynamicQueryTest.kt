package com.jlm.homework.service

import com.jlm.homework.dto.ExerciseBookRequest
import com.jlm.homework.entity.ExerciseBookEntity
import com.jlm.homework.entity.ExerciseBookImage
import com.jlm.homework.entity.ExerciseBookStatus
import com.jlm.homework.entity.withImages
import com.jlm.homework.repository.ExerciseBookRepo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import com.jlm.homework.config.EnvLoader
import com.jlm.homework.config.TestConfiguration

/**
 * 练习册动态查询功能测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfiguration::class)
@Transactional
class ExerciseBookDynamicQueryTest {

    companion object {
        @JvmStatic
        @BeforeAll
        fun setupClass() {
            EnvLoader.initTestEnvironment()
        }
    }

    @Autowired
    private lateinit var exerciseBookService: ExerciseBookServer

    @Autowired
    private lateinit var exerciseBookRepo: ExerciseBookRepo

    @BeforeEach
    fun setUp() {
        // 清理数据
        exerciseBookRepo.deleteAll()
        
        // 准备测试数据
        val testData = listOf(
            ExerciseBookEntity(
                title = "数学基础练习册",
                description = "小学三年级数学基础练习题集",
                subject = "数学",
                subjectId = 1L,
                grade = "三年级",
                gradeId = 3L,
                classId = 101L,
                difficultyLevel = 2,
                creatorId = 1L,
                status = ExerciseBookStatus.ACTIVE
            ).withImages(listOf(
                ExerciseBookImage(
                    url = "https://example.com/math-cover.jpg",
                    description = "数学练习册封面",
                    type = "cover",
                    order = 0
                )
            )),
            ExerciseBookEntity(
                title = "语文阅读理解",
                description = "四年级语文阅读理解专项练习",
                subject = "语文",
                subjectId = 2L,
                grade = "四年级",
                gradeId = 4L,
                classId = 102L,
                difficultyLevel = 3,
                creatorId = 1L,
                status = ExerciseBookStatus.ACTIVE
            ),
            ExerciseBookEntity(
                title = "英语单词练习",
                description = "五年级英语单词记忆练习册",
                subject = "英语",
                subjectId = 3L,
                grade = "五年级",
                gradeId = 5L,
                classId = 103L,
                difficultyLevel = 2,
                creatorId = 2L,
                status = ExerciseBookStatus.ACTIVE
            ),
            ExerciseBookEntity(
                title = "高级数学练习",
                description = "六年级高级数学练习册",
                subject = "数学",
                subjectId = 1L,
                grade = "六年级",
                gradeId = 6L,
                classId = 104L,
                difficultyLevel = 4,
                creatorId = 2L,
                status = ExerciseBookStatus.INACTIVE
            )
        )
        
        exerciseBookRepo.saveAll(testData)
    }

    @Test
    fun `测试按标题模糊查询`() {
        val request = ExerciseBookRequest(
            title = "数学",
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 2) { "应该找到2个包含'数学'的练习册" }
        assert(result.content.all { it.title?.contains("数学") == true }) { "所有结果都应该包含'数学'" }
    }

    @Test
    fun `测试按学科查询`() {
        val request = ExerciseBookRequest(
            subject = "语文",
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 1) { "应该找到1个语文练习册" }
        assert(result.content[0].subject == "语文") { "结果应该是语文学科" }
    }

    @Test
    fun `测试按年级查询`() {
        val request = ExerciseBookRequest(
            grade = "四年级",
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 1) { "应该找到1个四年级练习册" }
        assert(result.content[0].grade == "四年级") { "结果应该是四年级" }
    }

    @Test
    fun `测试按难度等级查询`() {
        val request = ExerciseBookRequest(
            difficultyLevel = 2,
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 2) { "应该找到2个难度等级为2的练习册" }
        assert(result.content.all { it.difficultyLevel == 2 }) { "所有结果的难度等级都应该是2" }
    }

    @Test
    fun `测试按创建者ID查询`() {
        val request = ExerciseBookRequest(
            creatorId = 1L,
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 2) { "应该找到2个创建者ID为1的练习册" }
        assert(result.content.all { it.creatorId == 1L }) { "所有结果的创建者ID都应该是1" }
    }

    @Test
    fun `测试按状态查询`() {
        val request = ExerciseBookRequest(
            status = ExerciseBookStatus.INACTIVE,
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 1) { "应该找到1个INACTIVE状态的练习册" }
        assert(result.content[0].status == ExerciseBookStatus.INACTIVE) { "结果状态应该是INACTIVE" }
    }

    @Test
    fun `测试多条件组合查询`() {
        val request = ExerciseBookRequest(
            subject = "数学",
            difficultyLevel = 2,
            status = ExerciseBookStatus.ACTIVE,
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 1) { "应该找到1个符合所有条件的练习册" }
        val book = result.content[0]
        assert(book.subject == "数学") { "学科应该是数学" }
        assert(book.difficultyLevel == 2) { "难度等级应该是2" }
        assert(book.status == ExerciseBookStatus.ACTIVE) { "状态应该是ACTIVE" }
    }

    @Test
    fun `测试分页功能`() {
        val request = ExerciseBookRequest(
            status = ExerciseBookStatus.ACTIVE,
            pageNum = 1,
            pageSize = 2
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 2) { "第一页应该有2条记录" }
        assert(result.totalElements == 3L) { "总共应该有3条ACTIVE记录" }
        assert(result.totalPages == 2) { "总共应该有2页" }
    }

    @Test
    fun `测试空条件查询`() {
        val request = ExerciseBookRequest(
            pageNum = 1,
            pageSize = 10
        )
        
        val result = exerciseBookService.searchExerciseBooks(request)
        
        assert(result.content.size == 3) { "默认应该查询ACTIVE状态的记录，共3条" }
    }

    @Test
    fun `测试参数验证`() {
        val invalidRequest = ExerciseBookRequest(
            pageNum = 0,
            pageSize = 10
        )
        
        val validationError = invalidRequest.validateForQuery()
        assert(validationError != null) { "页码为0应该返回验证错误" }
        assert(validationError!!.contains("页码")) { "错误信息应该包含页码相关内容" }
    }
}
