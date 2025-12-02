package com.jlm.homework.service;

import io.minio.*;
import io.minio.errors.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;


import java.io.IOException;


/**
 * @author QingYang
 * @version 1.0
 * @description
 * @date 2023/3/3 0003
 */
public interface IBucketService {

    /**
     * minio上传文件(MultipartFile方式)
     *
     * @param object      上传文件
     * @param contentType 文件类型
     * @param studentId    用户uuid
     * @param groupId     题卷主键id
     * @param status      是否包含答案（on：是，off：否）
     * @param objectName  对象名
     * @return 文件名
     * @throws IOException               异常类型
     * @throws ServerException           异常类型
     * @throws InsufficientDataException 异常类型
     * @throws ErrorResponseException    异常类型
     * @throws NoSuchAlgorithmException  异常类型
     * @throws InvalidKeyException       异常类型
     * @throws InvalidResponseException  异常类型
     * @throws XmlParserException        异常类型
     * @throws InternalException         异常类型
     */
    String upload(Object object, String contentType, Long studentId, Long groupId, String status, String objectName) throws IOException, ServerException,
            InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException,
            InvalidResponseException, XmlParserException, InternalException;

    /**
     * 删除文件
     *
     * @param studentId 用户uuid
     * @param groupId  题卷id
     * @return 删除结果
     * @throws ServerException           异常类型
     * @throws InsufficientDataException 异常类型
     * @throws ErrorResponseException    异常类型
     * @throws IOException               异常类型
     * @throws NoSuchAlgorithmException  异常类型
     * @throws InvalidKeyException       异常类型
     * @throws InvalidResponseException  异常类型
     * @throws XmlParserException        异常类型
     * @throws InternalException         异常类型
     */
    Boolean remove(Long studentId, Long groupId) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException;

    /**
     * 下载文件
     *
     * @param objectName 对象名
     * @param studentId   用户uuid
     * @param groupId    组id
     * @param status     类型
     * @param response   响应
     * @return 下载流
     * @throws IOException               异常类型
     * @throws NoSuchAlgorithmException  异常类型
     * @throws InvalidKeyException       异常类型
     * @throws ServerException           异常类型
     * @throws InsufficientDataException 异常类型
     * @throws ErrorResponseException    异常类型
     * @throws InvalidResponseException  异常类型
     * @throws XmlParserException        异常类型
     * @throws InternalException         异常类型
     */
    ResponseEntity<byte[]> download(String objectName, Long studentId, Long groupId, String status, HttpServletResponse response) throws IOException, NoSuchAlgorithmException, InvalidKeyException, ServerException, InsufficientDataException, ErrorResponseException, InvalidResponseException, XmlParserException, InternalException;
}
