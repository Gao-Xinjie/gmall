package com.gaoxinjie.gmall.manage.controller;


import org.apache.commons.lang3.StringUtils;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;

@RestController
@CrossOrigin
public class FileController {
    @Value("${fileServer.url}")
    private String fileServer;

    @PostMapping("fileUpload")
    public String fileUpload(@RequestParam("file")MultipartFile file){
        String file1 = this.getClass().getResource("/tracker.conf").getFile();
        try {
            ClientGlobal.init(file1);
            TrackerClient trackerClient = new TrackerClient();
            TrackerServer trackerServer=trackerClient.getTrackerServer();
            StorageClient storageClient = new StorageClient(trackerServer,null);
            String originalFilename = file.getOriginalFilename();
            String file_ext_name = StringUtils.substringAfterLast(originalFilename, ".");
            String[] upload_file = storageClient.upload_file(file.getBytes(), file_ext_name, null);
            String url=fileServer;
        for (String s : upload_file) {
            url +="/" +s;
        }

        return url;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
