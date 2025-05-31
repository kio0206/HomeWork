package com.itheima.controller;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SSHController {

    private static final String HOST = "192.168.100.10";
    private static final String USER = "root";
    private static final String PASSWORD = "000000";
    private static final int PORT = 22;

    // 查询主机列表
    @GetMapping("/hosts")
    public List<HostStatus> getHostStatus() {
        log.info("开始获取主机状态...");
        List<HostStatus> hostStatuses = new ArrayList<>();

        try {
            Session session = createSession();
            String output = executeCommand(session, "ansible ansible-nodes -m ping");

            String[] lines = output.split("\n");
            for (String line : lines) {
                if (line.contains("| SUCCESS")) {
                    String[] parts = line.split(" ");
                    String hostName = parts[0].trim();
                    String status = line.contains("pong") ? "online" : "offline";
                    hostStatuses.add(new HostStatus(hostName, status));
                }
            }
        } catch (Exception e) {
            log.error("获取主机状态失败", e);
        }

        log.info("返回的数据为 " + hostStatuses);
        return hostStatuses;
    }

    // 添加新主机
    @PostMapping("/hosts")
    public String addHost(@RequestBody HostRequest request) {
        log.info("开始添加主机: {}", request);
        try {
            Session session = createSession();

            // 1. 启动Docker容器
            String dockerCommand = String.format(
                    "docker run -d --name %s -p %s:22 ansible-node",
                    request.getName(), request.getPort()
            );
            executeCommand(session, dockerCommand);

            // 2. 添加到ansible hosts文件
            String hostEntry = String.format(
                    "echo '%s ansible_ssh_port=%s ansible_ssh_user=ansible ansible_ssh_pass=ansible' >> /etc/ansible/hosts",
                    request.getName(), request.getPort()
            );
            executeCommand(session, hostEntry);

            return "success";
        } catch (Exception e) {
            log.error("添加主机失败", e);
            return "error: " + e.getMessage();
        }
    }

    // 删除主机
    @DeleteMapping("/hosts/{name}")
    public String deleteHost(@PathVariable String name) {
        log.info("开始删除主机: {}", name);
        try {
            Session session = createSession();

            // 1. 停止并删除Docker容器
            executeCommand(session, "docker stop " + name);
            executeCommand(session, "docker rm " + name);

            // 2. 从ansible hosts文件中删除
            String sedCommand = String.format(
                    "sed -i '/%s/d' /etc/ansible/hosts",
                    name
            );
            executeCommand(session, sedCommand);

            return "success";
        } catch (Exception e) {
            log.error("删除主机失败", e);
            return "error: " + e.getMessage();
        }
    }

    // 修改主机信息
    @PutMapping("/hosts/{oldName}")
    public String updateHost(@PathVariable String oldName, @RequestBody HostRequest request) {
        log.info("开始更新主机信息: {} -> {}", oldName, request);
        try {
            Session session = createSession();

            // 从ansible hosts文件中更新
            String sedCommand = String.format(
                    "sed -i 's/%s ansible_ssh_port=.*/%s ansible_ssh_port=%s ansible_ssh_user=ansible ansible_ssh_pass=ansible/' /etc/ansible/hosts",
                    oldName, request.getName(), request.getPort()
            );
            executeCommand(session, sedCommand);

            return "success";
        } catch (Exception e) {
            log.error("更新主机失败", e);
            return "error: " + e.getMessage();
        }
    }

    // 创建SSH会话
    private Session createSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(USER, HOST, PORT);
        session.setPassword(PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }

    // 执行命令
    private String executeCommand(Session session, String command) throws JSchException, IOException {
        log.info("执行命令: {}", command);
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        channelExec.setCommand(command);

        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        channelExec.setOutputStream(responseStream);
        channelExec.connect();

        // 等待命令执行完成
        while (channelExec.isConnected()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        channelExec.disconnect();
        String response = responseStream.toString();
        log.info("命令执行结果: {}", response);
        return response;
    }

    // 请求实体类
    public static class HostRequest {
        private String name;
        private String port;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        @Override
        public String toString() {
            return "HostRequest{" +
                    "name='" + name + '\'' +
                    ", port='" + port + '\'' +
                    '}';
        }
    }

    // 主机状态类
    public static class HostStatus {
        private String name;
        private String status;

        public HostStatus(String name, String status) {
            this.name = name;
            this.status = status;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "HostStatus{" +
                    "name='" + name + '\'' +
                    ", status='" + status + '\'' +
                    '}';
        }
    }
}