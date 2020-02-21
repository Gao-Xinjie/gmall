package com.gaoxinjie.gmall.manage;


import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class GmallManageWebApplicationTests {


    @Test
    public void upload() throws IOException, MyException {
        String file = this.getClass().getResource("/tracker.conf").getFile();
        ClientGlobal.init(file);
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer=trackerClient.getTrackerServer();
        StorageClient storageClient = new StorageClient(trackerServer,null);
        String[] upload_file = storageClient.upload_file("d://test0.jpg", "jpg", null);
        for (String s : upload_file) {
            System.out.println("s="+s);
        }
    }
}
