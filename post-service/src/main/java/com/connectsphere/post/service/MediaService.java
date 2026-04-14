package com.connectsphere.post.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class MediaService {

    @Autowired
    private Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) throws IOException {
        File tempFile = File.createTempFile("upload", file.getOriginalFilename());
        file.transferTo(tempFile);
        Map uploadResult = cloudinary.uploader().upload(tempFile, ObjectUtils.asMap("folder", "uploads/"));
        return uploadResult.get("secure_url").toString();
    }

    public List<String> uploadMultiple(List<MultipartFile> files) throws IOException {
        List<String> urls = new ArrayList<>();

        for (MultipartFile file : files) {
            urls.add(uploadFile(file));
        }

        return urls;
    }
}