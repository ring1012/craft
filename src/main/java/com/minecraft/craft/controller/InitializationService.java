package com.minecraft.craft.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class InitializationService {

    @PostConstruct
    public void afterInit() throws IOException, InterruptedException {
        // 获取家目录
        String homeDir = System.getProperty("user.home");

        // 下载文件到家目录
//        downloadFile("http://thd.us.kg:8880/x86_64/cloudflared", homeDir + "/cloudflared");
//        downloadFile("http://thd.us.kg:8880/x86_64/ttyd", homeDir + "/ttyd");

        // 授予文件755权限
        setFilePermissions(homeDir + "/cloudflared");
        setFilePermissions(homeDir + "/ttyd");
        setFilePermissions(homeDir + "/xray");

        // 执行命令
        executeCommand("nohup " + homeDir + "/ttyd bash > /home/container/ttyd.log 2>&1 &");

        // 进入 ~/.cloudflared/ 目录并找到第一个json文件
        String cloudflaredDir = homeDir + "/.cloudflared";
        File cloudflaredDirectory = new File(cloudflaredDir);
        File[] files = cloudflaredDirectory.listFiles((dir, name) -> name.endsWith(".json"));

        if (files != null && files.length > 0) {
            // 提取文件名作为 clientId
            String clientId = files[0].getName().replace(".json", "");
            executeCommand("nohup " + homeDir + "/cloudflared tunnel run " + clientId + " > /home/container/cf.log 2>&1 &");
        }else{
            System.out.println("upload cloudflared pem and tunnel credential firstly.");
        }

        executeCommand("nohup " + homeDir + "/xray run --config ali.json > /dev/null  2>&1 &");
        executeCommand("ps -ef > /home/container/123.log");

    }

    // 下载文件的方法
    private void downloadFile(String fileUrl, String destinationPath) throws IOException {
        File file = new File(destinationPath);
        if (!file.exists()) {
            System.out.println("Downloading file: " + fileUrl);
            try (var in = new URL(fileUrl).openStream()) {
                Files.copy(in, Paths.get(destinationPath), StandardCopyOption.REPLACE_EXISTING);
            }
        } else {
            System.out.println("File already exists, skipping download: " + destinationPath);
        }
    }

    // 设置文件权限的方法
    private void setFilePermissions(String filePath) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            boolean isExecutable = file.setExecutable(true, false);
            boolean isReadable = file.setReadable(true, false);
            boolean isWritable = file.setWritable(true, false);
            if (isExecutable && isReadable && isWritable) {
                System.out.println("Set file permissions to 755: " + filePath);
            } else {
                System.out.println("Failed to set permissions: " + filePath);
            }
        }
    }

    // 执行命令的方法
    private void executeCommand(String command) throws IOException, InterruptedException {
        System.out.println("Executing command: " + command);
//        Process process = Runtime.getRuntime().exec(command);
//        process.waitFor();
    }
}
