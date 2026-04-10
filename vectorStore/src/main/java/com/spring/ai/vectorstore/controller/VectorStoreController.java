package com.spring.ai.vectorstore.controller;

import com.spring.ai.vectorstore.manager.VectorStoreManager;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 向量存储上传Controller
 *
 * @author zhouqi
 * @version 初次构建
 * @since 2026/4/9
 */
@RestController()
@RequestMapping("/vectorStore")
public class VectorStoreController {

    @Resource
    VectorStoreManager vectorStoreManager;

    // 1. 上传文件 → 存入向量库
    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) throws Exception {
        vectorStoreManager.upload(file);
        return "上传并向量化成功";
    }

    // 2. 搜索
    @GetMapping("/search")
    public Object search(@RequestParam String query) {
        return vectorStoreManager.search(query);
    }

    // 3. 清空所有向量数据
    @PostMapping("/deleteAll")
    public String deleteAll() {
        vectorStoreManager.deleteAll();
        return "已清空所有向量数据";
    }

    // 4. 根据文件名删除向量（你要的接口）
    @PostMapping("/deleteByFileName")
    public String deleteByFileName(@RequestParam String fileName) {
        vectorStoreManager.deleteByFileName(fileName);
        return "已删除文件对应的向量：" + fileName;
    }



}
