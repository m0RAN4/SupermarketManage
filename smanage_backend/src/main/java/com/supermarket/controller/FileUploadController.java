package com.supermarket.controller;

import com.supermarket.common.BusinessException;
import com.supermarket.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器 — 商品图片上传与读取
 */
@RestController
@RequestMapping("/api")
public class FileUploadController {

    // 图片存储根目录：项目根目录下的 picture 文件夹
    private static final String PICTURE_DIR = System.getProperty("user.dir") + File.separator + ".." + File.separator + "picture";

    @Autowired
    private HttpServletRequest request;

    /**
     * 校验管理员权限
     */
    private void checkAdmin() {
        Integer userType = (Integer) request.getAttribute("userType");
        if (userType == null || userType != 0) {
            throw new BusinessException(403, "无权限访问");
        }
    }

    /**
     * 上传商品图片
     * POST /api/upload/image
     */
    @PostMapping("/upload/image")
    public Result<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "cropData", required = false) String cropData) {
        checkAdmin();
        if (file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }

        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            throw new BusinessException("仅支持上传图片文件");
        }

        // 校验文件大小（最大 5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        try {
            // 确保目录存在
            File dir = new File(PICTURE_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // 生成唯一文件名（保留原始扩展名）
            String originalFilename = file.getOriginalFilename();
            String extension = ".jpg";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path filePath = Paths.get(PICTURE_DIR, filename);
            Files.write(filePath, file.getBytes());

            // 返回文件名和定位参数
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("url", filename);
            result.put("cropData", cropData);
            return Result.ok(result);
        } catch (IOException e) {
            throw new BusinessException("图片上传失败：" + e.getMessage());
        }
    }
}
