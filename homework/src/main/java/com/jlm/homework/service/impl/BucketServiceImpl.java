package com.jlm.homework.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jlm.homework.entity.convert.FileConstant;
import com.jlm.homework.service.IBucketService;
import com.jlm.homework.util.BasicUtil;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;



import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * @author QingYang
 * @version 1.0
 * @description
 * @date 2023/3/3 0003
 */
@Slf4j
@Service
public class BucketServiceImpl implements IBucketService {

    @Value("${plus.download-url}")
    private String download;

    @Value("${minio.bucketName:exercise}")
    private String bucketName;

    @Resource
    private MinioClient minioClient;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String upload(Object object, String contentType, Long studentId, Long groupId, String status, String objectName) throws IOException, ServerException,
            InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException {

        PutObjectArgs putObjectArgs = null;

        if (object instanceof MultipartFile) {
            MultipartFile multipartFile = (MultipartFile) object;
            putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(studentId + "/" + groupId + "/" + status + "/" + multipartFile.getOriginalFilename())
                    .stream(multipartFile.getInputStream(), multipartFile.getSize(), -1)
                    .contentType(StrUtil.isBlank(contentType) ? multipartFile.getContentType() : contentType)
                    .build();
        } else if (object instanceof byte[]) {
            byte[] data = (byte[]) object;
            putObjectArgs = PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(studentId + "/" + groupId + "/" + status + "/" + objectName)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(StrUtil.isBlank(contentType) ? FileConstant.MIME_TYPE_DOCX : contentType)
                    .build();
        }

        if (!Objects.isNull(putObjectArgs)) {
            minioClient.putObject(putObjectArgs);
        } else {
            throw new RuntimeException( "文件上传失败，检查文件类型");
        }

        return BasicUtil.buildFileDownloadUrl(download, studentId, groupId, status, objectName);
    }

    @Override
    public ResponseEntity<byte[]> download(String objectName, Long studentId, Long groupId, String status, HttpServletResponse response) throws
            IOException, NoSuchAlgorithmException, InvalidKeyException, ServerException, InsufficientDataException, ErrorResponseException, InvalidResponseException, XmlParserException, InternalException {
        byte[] bytes = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(studentId + "/" + groupId + "/" + status + "/" + objectName)
                        .build()
        ).readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", URLEncoder.encode(objectName, StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(objectName, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).contentLength(bytes.length).body(bytes);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean remove(Long studentId, Long groupId) throws ServerException, InsufficientDataException,
            ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException,
            XmlParserException, InternalException {
        // 获取objects
        String objectNames = studentId + "/" + groupId + "/";
        Iterable<Result<Item>> objectIterator = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(objectNames)
                        .recursive(true)
                        .build());
        // 循环删除文件
        for (Result<Item> itemResult : objectIterator) {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(itemResult.get().objectName())
                            .build());
        }
        // 文件删除成功
        return true;
    }

}
