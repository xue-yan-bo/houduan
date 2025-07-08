package com.jlm.homework.entity

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test

/**
 * 简单的图片功能测试
 */
class SimpleImageTest {

    @Test
    fun `测试JSON序列化和反序列化`() {
        val objectMapper = ObjectMapper()
        
        val images = listOf(
            ExerciseBookImage(
                url = "https://example.com/cover.jpg",
                description = "封面图",
                type = "cover",
                order = 0
            )
        )
        
        // 序列化
        val json = objectMapper.writeValueAsString(images)
        println("序列化结果: $json")
        
        // 反序列化
        val listType = objectMapper.typeFactory.constructCollectionType(List::class.java, ExerciseBookImage::class.java)
        val deserializedImages: List<ExerciseBookImage> = objectMapper.readValue(json, listType)
        
        println("反序列化结果: $deserializedImages")
        println("图片数量: ${deserializedImages.size}")
        
        assert(deserializedImages.size == 1)
        assert(deserializedImages[0].url == "https://example.com/cover.jpg")
    }

    @Test
    fun `测试实体扩展属性`() {
        val json = """[{"url":"https://example.com/cover.jpg","description":"封面图","type":"cover","order":0}]"""
        
        val entity = ExerciseBookEntity(
            title = "测试练习册",
            images = json
        )
        
        println("实体图片JSON: ${entity.images}")
        
        val imageList = entity.imageList
        println("图片列表: $imageList")
        println("图片数量: ${imageList.size}")
        
        if (imageList.isNotEmpty()) {
            println("第一张图片: ${imageList[0]}")
        }
        
        val coverUrl = entity.coverImageUrl
        println("封面URL: $coverUrl")
    }
}
