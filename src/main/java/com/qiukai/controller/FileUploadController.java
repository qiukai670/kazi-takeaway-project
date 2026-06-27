package com.qiukai.controller;

import com.qiukai.common.BusinessException;
import com.qiukai.common.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传控制器
 * 接收图片文件，保存到本地 uploads 目录，返回可访问的 URL 路径
 */
@RestController
@RequestMapping("/api")
public class FileUploadController {

    /** 上传目录：基于项目根目录的绝对路径，避免被解析到 Tomcat 临时目录 */
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads" + File.separator;
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    /**
     * 上传图片文件
     * 返回 /uploads/xxx.png 格式的访问路径，可直接用于 <img src>
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException("仅支持 JPG/PNG/GIF/WebP 格式的图片");
        }

        // 生成唯一文件名：UUID + 原始扩展名
        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确保上传目录存在
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("创建上传目录失败");
        }

        // 保存文件到磁盘（使用绝对路径）
        try {
            file.transferTo(new File(UPLOAD_DIR + fileName));
        } catch (IOException e) {
            throw new BusinessException("文件上传失败：" + e.getMessage());
        }

        return Result.success("/uploads/" + fileName);
    }
}
