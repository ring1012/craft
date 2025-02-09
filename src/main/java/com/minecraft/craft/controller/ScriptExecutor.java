package com.minecraft.craft.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/script")
public class ScriptExecutor {


    private static final String SCRIPT_DIR = "/home/container/"; // 指定允许执行的脚本目录

    @GetMapping("/{scriptName}")
    public String executeScript(@PathVariable String scriptName) {
        System.out.println(scriptName+" 执行");
        if (!scriptName.matches("^[a-zA-Z0-9_.-]+$")) {
            return "非法脚本名称！";
        }

        String scriptPath = Paths.get(SCRIPT_DIR, scriptName).toString();

        try {
            // 先执行 chmod 755
            Process chmodProcess = new ProcessBuilder("chmod", "755", scriptPath).start();
            int chmodExitCode = chmodProcess.waitFor();
            if (chmodExitCode != 0) {
                return "权限修改失败，退出码：" + chmodExitCode;
            }

            // 执行 shell 脚本
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", scriptPath);
            processBuilder.redirectErrorStream(true); // 合并 stderr 到 stdout
            Process process = processBuilder.start();

            // 逐行读取 shell 输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Shell Output] " + line); // 逐行打印到控制台
                    output.append(line).append("\n");
                }
            }

            // 等待 shell 执行完成
            int exitCode = process.waitFor();
            output.append("Shell 进程退出，退出码：").append(exitCode);

            return output.toString();
        } catch (Exception e) {
            return "执行错误：" + e.getMessage();
        }
    }

}
