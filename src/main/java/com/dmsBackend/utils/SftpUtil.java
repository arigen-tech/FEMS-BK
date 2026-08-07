package com.dmsBackend.utils;

import com.jcraft.jsch.*;

import java.io.*;
import java.util.Properties;

public class SftpUtil {

    private static Session createSession(String host, int port, String username, String password) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, port);
        session.setPassword(password);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);

        session.connect();
        return session;
    }

    /** Ensures that the remote directory exists (creates missing parents). */
    private static void ensureDirectory(ChannelSftp channel, String remoteDir) throws SftpException {
        String[] folders = remoteDir.replace("\\", "/").split("/");
        String path = "";
        for (String folder : folders) {
            if (folder == null || folder.isBlank()) continue;
            path += "/" + folder;
            try {
                channel.cd(path);
            } catch (SftpException e) {
                channel.mkdir(path);  // create if missing
                channel.cd(path);
            }
        }
    }

    // ---------- Upload ----------
    public static void upload(InputStream inputStream, String remoteDir, String remoteFileName,
                              String host, int port, String username, String password) throws Exception {
        Session session = createSession(host, port, username, password);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        try {
            ensureDirectory(channel, remoteDir);
            channel.put(inputStream, remoteFileName);
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }

    // ---------- Download as InputStream ----------
    public static InputStream download(String remoteFilePath,
                                       String host, int port, String username, String password) throws Exception {
        Session session = createSession(host, port, username, password);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.get(remoteFilePath, outputStream);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }

    // ---------- Delete ----------
    public static void delete(String remoteFilePath,
                              String host, int port, String username, String password) throws Exception {
        Session session = createSession(host, port, username, password);
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();

        try {
            channel.rm(remoteFilePath);
        } finally {
            channel.disconnect();
            session.disconnect();
        }
    }

    // ---------- Move ----------
    public static void move(String oldRemotePath, String newRemoteDir, String newFileName,
                            String host, int port, String username, String password) throws Exception {
        try (InputStream in = download(oldRemotePath, host, port, username, password)) {
            upload(in, newRemoteDir, newFileName, host, port, username, password);
            delete(oldRemotePath, host, port, username, password);
        }
    }

    // ---------- Exists ----------
    public static boolean exists(String host, int port, String user, String password, String remoteFilePath) {
        ChannelSftp channelSftp = null;
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            SftpATTRS attrs = channelSftp.lstat(remoteFilePath);
            return attrs != null;
        } catch (SftpException e) {
            return false; // file not found
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }

    // ---------- Download into OutputStream ----------
    public static void download(String remoteDir, String fileName, OutputStream os,
                                String host, int port, String user, String password) throws Exception {
        ChannelSftp channelSftp = null;
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            String remoteFilePath = remoteDir.replace("\\", "/") + "/" + fileName;
            try (InputStream is = channelSftp.get(remoteFilePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
