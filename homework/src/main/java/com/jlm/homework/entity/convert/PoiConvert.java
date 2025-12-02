package com.jlm.homework.entity.convert;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @author QingYang
 * @version 1.0
 * @description
 * @date 2023/3/25 0025
 */
@Slf4j
public class PoiConvert implements Html2Word {

    private final InputStream inputStream;

    public PoiConvert(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public byte[] convert() {
        try (POIFSFileSystem fs = new POIFSFileSystem()) {
            DirectoryEntry directory = fs.getRoot();
            try (inputStream) {
                directory.createDocument("WordDocument", inputStream);
            }
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                fs.writeFilesystem(bos);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.error("[POI转换器]转换文档异常", e);
            throw new RuntimeException( "POI转换器异常，稍后重试");
        }
    }
}
