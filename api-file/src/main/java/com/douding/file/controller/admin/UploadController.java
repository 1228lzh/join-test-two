package com.douding.file.controller.admin;

import com.douding.server.domain.Test;
import com.douding.server.dto.FileDto;
import com.douding.server.dto.PageDto;
import com.douding.server.dto.ResponseDto;
import com.douding.server.enums.FileUseEnum;
import com.douding.server.service.FileService;
import com.douding.server.service.TestService;
import com.douding.server.util.Base64ToMultipartFile;
import com.douding.server.util.UuidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/*
    返回json 应用@RestController
    返回页面  用用@Controller
 */
@RequestMapping("/admin/file")
@RestController
public class UploadController {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);
    public  static final String BUSINESS_NAME ="文件上传";
    @Resource
    private TestService testService;

    @Value("${file.path}")
    private String FILE_PATH;

    @Value("${file.domain}")
    private String FILE_DOMAIN;

    @Resource
    private FileService fileService;

    @RequestMapping("/upload")
    public ResponseDto upload(@RequestBody FileDto fileDto) throws Exception {
        ResponseDto<PageDto> responseDto = new ResponseDto<>();
        //fileDto.setPath(FILE_DOMAIN+fileDto.getKey()+"."+fileDto.getSuffix());
        fileService.save(fileDto);
        return responseDto;
    }

    // 合并分片
    public void merge(FileDto fileDto) throws Exception {
        LOG.info("合并分片开始");

        // 目标文件路径
        //String destFilePath = fileDto.getFilePath();
        String destFilePath = FILE_PATH;
        // 分片大小
        int chunkSize = fileDto.getShardSize();
        // 分片数量
        int chunkNum = fileDto.getShardTotal();
        // 分片文件列表
        List<File> chunkFiles = new ArrayList<>();

        // 构建分片文件列表
        for (int i = 1; i <= chunkNum; i++) {
            // 分片文件路径
            String chunkFilePath = destFilePath + "_" + i;
            // 分片文件
            File chunkFile = new File(chunkFilePath);
            chunkFiles.add(chunkFile);
        }

        // 检查所有分片文件是否都已上传完毕
        boolean isAllChunksUploaded = true;
        for (File chunkFile : chunkFiles) {
            if (!chunkFile.exists()) {
                isAllChunksUploaded = false;
                break;
            }
        }

        // 如果存在未上传完毕的分片文件，则直接返回
        if (!isAllChunksUploaded) {
            LOG.warn("存在未上传完毕的分片文件，合并分片失败");
            return;
        }

        // 将所有分片文件合并到目标文件中
        try (OutputStream out = new FileOutputStream(destFilePath)) {
            byte[] buffer = new byte[1024];
            for (File chunkFile : chunkFiles) {
                try (InputStream in = new FileInputStream(chunkFile)) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                }
            }
        }

        // 删除所有分片文件
        for (File chunkFile : chunkFiles) {
            if (!chunkFile.delete()) {
                LOG.warn("删除分片文件失败：{}", chunkFile.getAbsolutePath());
            }
        }

        LOG.info("合并分片完成");
    }

    @GetMapping("/check/{key}")
    public ResponseDto check(@PathVariable String key) throws Exception {
        LOG.info("检查上传分片开始：{}", key);
        ResponseDto responseDto = new ResponseDto();
        FileDto fileDto = fileService.findByKey(key);

        responseDto.setSuccess(fileDto==null);
        return responseDto;
    }

}//end class
