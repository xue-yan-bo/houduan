package com.jlm.homework.dto;

import com.jlm.homework.entity.ExerciseBookEntity;
import com.jlm.homework.entity.ExerciseBookImage;
import com.jlm.homework.entity.ExerciseBookStatus;
import java.util.ArrayList;
import java.util.List;

/**
 * 练习册请求转换器
 * 负责将请求DTO转换为实体对象
 */
public class ExerciseBookRequestConverter {

    /**
     * 转换为实体对象（用于创建）
     */
    public static ExerciseBookEntity toEntity(ExerciseBookRequest request, Long currentUserId, Long schoolId) {
        ExerciseBookEntity entity = new ExerciseBookEntity(
                request.getTitle(),
                request.getDescription(),
                request.getSubject(),
                request.getSubjectId(),
                request.getGrade(),
                request.getGradeId(),
                request.getClassId(), // 保留向后兼容
                request.getDifficultyLevel() != null ? request.getDifficultyLevel() : 1,
                currentUserId,
                schoolId,
                request.getStatus() != null ? request.getStatus() : ExerciseBookStatus.ACTIVE
        );

        // 处理图片信息
        List<ExerciseBookImage> imageList = new ArrayList<>();
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            // 优先使用详细的图片信息
            imageList = request.getImages();
        } else if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            // 其次使用URL列表（智能解析）
            imageList = createImagesFromUrls(request.getImageUrls());
        }
        // 默认为空列表

        // 先添加图片信息
        ExerciseBookEntity result = entity.withImages(imageList);

        // 处理班级ID和名称列表
        List<Long> classIds = request.getClassIds();
        if (classIds != null && !classIds.isEmpty()) {
            List<String> classNames = request.getClassNames();
            if (classNames != null && !classNames.isEmpty() && classNames.size() == classIds.size()) {
                // 如果班级名称列表存在且与ID列表长度一致，同时设置ID和名称
                result = result.withClassIdsAndNames(classIds, classNames);
            } else {
                // 否则只设置ID列表
                result = result.withClassIds(classIds);
            }
        }

        return result;
    }

    /**
     * 从URL列表创建图片对象
     */
    private static List<ExerciseBookImage> createImagesFromUrls(List<String> imageUrls) {
        List<ExerciseBookImage> images = new ArrayList<>();
        for (String url : imageUrls) {
            ExerciseBookImage image = new ExerciseBookImage();
            image.setUrl(url);
            images.add(image);
        }
        return images;
    }
}