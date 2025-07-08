package com.jlm.homework.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * 练习册图片功能测试
 */
class ExerciseBookImageTest {

    @Test
    fun `测试图片列表扩展属性 - 空图片`() {
        val entity = ExerciseBookEntity(
            title = "测试练习册",
            images = null
        )
        
        val imageList = entity.imageList
        assertTrue(imageList.isEmpty(), "空图片应该返回空列表")
    }

    @Test
    fun `测试图片列表扩展属性 - 有效图片`() {
        val imagesJson = """[
            {
                "url": "https://example.com/cover.jpg",
                "description": "封面图",
                "type": "cover",
                "order": 0
            },
            {
                "url": "https://example.com/content.jpg",
                "description": "内容图",
                "type": "content",
                "order": 1
            }
        ]"""
        
        val entity = ExerciseBookEntity(
            title = "测试练习册",
            images = imagesJson
        )
        
        val imageList = entity.imageList
        assertEquals(2, imageList.size, "应该有2张图片")
        
        val coverImage = imageList[0]
        assertEquals("https://example.com/cover.jpg", coverImage.url)
        assertEquals("封面图", coverImage.description)
        assertEquals("cover", coverImage.type)
        assertEquals(0, coverImage.order)
        
        val contentImage = imageList[1]
        assertEquals("https://example.com/content.jpg", contentImage.url)
        assertEquals("内容图", contentImage.description)
        assertEquals("content", contentImage.type)
        assertEquals(1, contentImage.order)
    }

    @Test
    fun `测试封面图片URL扩展属性`() {
        val imagesJson = """[
            {
                "url": "https://example.com/content.jpg",
                "type": "content",
                "order": 1
            },
            {
                "url": "https://example.com/cover.jpg",
                "type": "cover",
                "order": 0
            }
        ]"""
        
        val entity = ExerciseBookEntity(
            title = "测试练习册",
            images = imagesJson
        )
        
        val coverUrl = entity.coverImageUrl
        assertEquals("https://example.com/cover.jpg", coverUrl, "应该返回封面图片URL")
    }

    @Test
    fun `测试封面图片URL扩展属性 - 无封面图`() {
        val imagesJson = """[
            {
                "url": "https://example.com/content1.jpg",
                "type": "content",
                "order": 0
            },
            {
                "url": "https://example.com/content2.jpg",
                "type": "content",
                "order": 1
            }
        ]"""
        
        val entity = ExerciseBookEntity(
            title = "测试练习册",
            images = imagesJson
        )
        
        val coverUrl = entity.coverImageUrl
        assertEquals("https://example.com/content1.jpg", coverUrl, "无封面图时应该返回第一张图片")
    }

    @Test
    fun `测试图片URL列表扩展属性`() {
        val imagesJson = """[
            {
                "url": "https://example.com/cover.jpg",
                "type": "cover"
            },
            {
                "url": "https://example.com/content1.jpg",
                "type": "content"
            },
            {
                "url": "https://example.com/content2.jpg",
                "type": "content"
            }
        ]"""
        
        val entity = ExerciseBookEntity(
            title = "测试练习册",
            images = imagesJson
        )
        
        val imageUrls = entity.imageUrls
        assertEquals(3, imageUrls.size, "应该有3个URL")
        assertEquals("https://example.com/cover.jpg", imageUrls[0])
        assertEquals("https://example.com/content1.jpg", imageUrls[1])
        assertEquals("https://example.com/content2.jpg", imageUrls[2])
    }

    @Test
    fun `测试withImages扩展函数`() {
        val entity = ExerciseBookEntity(title = "测试练习册")
        
        val images = listOf(
            ExerciseBookImage(
                url = "https://example.com/cover.jpg",
                description = "封面图",
                type = "cover",
                order = 0
            ),
            ExerciseBookImage(
                url = "https://example.com/content.jpg",
                description = "内容图",
                type = "content",
                order = 1
            )
        )
        
        val entityWithImages = entity.withImages(images)
        
        assertNotNull(entityWithImages.images, "应该有图片JSON数据")
        assertEquals(2, entityWithImages.imageList.size, "应该有2张图片")
        assertEquals("https://example.com/cover.jpg", entityWithImages.coverImageUrl)
    }

    @Test
    fun `测试createImagesFromUrls函数`() {
        val urls = listOf(
            "https://example.com/image1.jpg",
            "https://example.com/image2.jpg",
            "https://example.com/image3.jpg"
        )
        
        val images = createImagesFromUrls(urls)
        
        assertEquals(3, images.size, "应该创建3个图片对象")
        
        // 第一张图片应该是封面
        assertEquals("https://example.com/image1.jpg", images[0].url)
        assertEquals("cover", images[0].type)
        assertEquals(0, images[0].order)
        
        // 其他图片应该是内容图
        assertEquals("https://example.com/image2.jpg", images[1].url)
        assertEquals("content", images[1].type)
        assertEquals(1, images[1].order)
        
        assertEquals("https://example.com/image3.jpg", images[2].url)
        assertEquals("content", images[2].type)
        assertEquals(2, images[2].order)
    }

    @Test
    fun `测试无效JSON的处理`() {
        val entity = ExerciseBookEntity(
            title = "测试练习册",
            images = "invalid json"
        )
        
        val imageList = entity.imageList
        assertTrue(imageList.isEmpty(), "无效JSON应该返回空列表")
        
        val coverUrl = entity.coverImageUrl
        assertNull(coverUrl, "无效JSON应该返回null封面URL")
        
        val imageUrls = entity.imageUrls
        assertTrue(imageUrls.isEmpty(), "无效JSON应该返回空URL列表")
    }

    @Test
    fun `测试空图片列表的withImages`() {
        val entity = ExerciseBookEntity(title = "测试练习册")
        val entityWithEmptyImages = entity.withImages(emptyList())
        
        assertNull(entityWithEmptyImages.images, "空图片列表应该设置为null")
        assertTrue(entityWithEmptyImages.imageList.isEmpty(), "应该返回空列表")
    }
}
