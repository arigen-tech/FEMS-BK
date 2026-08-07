package com.dmsBackend.utils;

import com.dmsBackend.response.CartridgeInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class DetectCurrCartridge {

    private static final String LTFS_PATH =
            "C:\\Program Files\\HPE\\LTFS\\ltfslibutil.exe";

    // TESTING PURPOSE
    private static final boolean TEST_MODE = true;

    public static CartridgeInfo detect(String driveLetter) {

        // Hardcoded cartridge for testing
        if (TEST_MODE) {
            CartridgeInfo info = new CartridgeInfo();
            info.setDrive("E:");
            info.setCartridge("TEST_CART_001");
            info.setSlot("Slot1");
            info.setLibrary("Library1");
            return info;
        }

        // Original code
        CartridgeInfo info = new CartridgeInfo();

        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder(LTFS_PATH, "-i", driveLetter);

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line.trim(), info);
            }

            process.waitFor();

        } catch (Exception e) {
            info.setError(e.getMessage());
        }

        return info;
    }

    private static void parseLine(String line, CartridgeInfo info) {

        if (line.startsWith("Drive")) {
            info.setDrive(getValue(line));
        }
        else if (line.startsWith("Cartridge")) {
            String value = getValue(line);
            String[] parts = value.split(" from slot ");
            info.setCartridge(parts[0]);

            if (parts.length > 1) {
                info.setSlot(parts[1]);
            }
        }
        else if (line.startsWith("Library")) {
            info.setLibrary(getValue(line));
        }
    }

    private static String getValue(String line) {
        return line.substring(line.indexOf(":") + 1).trim();
    }
}