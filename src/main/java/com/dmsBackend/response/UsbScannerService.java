//package com.dmsBackend.response;
//
//import com.dmsBackend.service.ScannersService;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.stereotype.Service;
//import org.usb4java.*;
//import java.io.File;
//import java.nio.ByteBuffer;
//import java.nio.IntBuffer;
//import java.nio.file.Files;
//import java.util.List;
//
//
//@Service("usbScanner")  // Explicit bean name
//@Qualifier("usbScanner")  // Qualifier annotation
//public class UsbScannerService implements ScannersService {
//
//    private static final short VENDOR_ID = 0x04a9;  // Canon example
//    private static final short PRODUCT_ID = 0x1900; // Scanner model
//
//    @Override
//    public File scanDocument() throws Exception {
//        Context context = new Context();
//        int result = LibUsb.init(context);
//        if (result != LibUsb.SUCCESS) {
//            throw new Exception("LibUsb initialization failed");
//        }
//
//        try {
//            // Find and open device
//            DeviceHandle handle = findDevice(context);
//            if (handle == null) {
//                throw new Exception("Scanner device not found");
//            }
//
//            // Claim interface
//            result = LibUsb.claimInterface(handle, 0);
//            if (result != LibUsb.SUCCESS) {
//                throw new Exception("Failed to claim interface");
//            }
//
//            // Send scan command (device-specific)
//            ByteBuffer command = ByteBuffer.allocateDirect(3);
//            command.put(new byte[]{0x1B, 0x24, 0x01}); // ESC/POS example
//            IntBuffer transferred = IntBuffer.allocate(1);
//            result = LibUsb.bulkTransfer(
//                    handle,
//                    (byte) 0x01,
//                    command,
//                    transferred,
//                    1000
//            );
//
//            // Read scan data
//            ByteBuffer buffer = ByteBuffer.allocateDirect(65536);
//            result = LibUsb.bulkTransfer(
//                    handle,
//                    (byte) 0x82,
//                    buffer,
//                    transferred,
//                    5000
//            );
//
//            // Save to file
//            byte[] imageData = new byte[transferred.get()];
//            buffer.get(imageData);
//            File output = File.createTempFile("scan_", ".jpg");
//            Files.write(output.toPath(), imageData);
//
//            return output;
//        } finally {
//            LibUsb.exit(context);
//        }
//    }
//
//    private DeviceHandle findDevice(Context context) {
//        // Get device list
//        DeviceList list = new DeviceList();
//        int result = LibUsb.getDeviceList(context, list);
//        if (result < 0) return null;
//
//        try {
//            // Iterate devices
//            for (Device device : list) {
//                DeviceDescriptor descriptor = new DeviceDescriptor();
//                result = LibUsb.getDeviceDescriptor(device, descriptor);
//                if (result != LibUsb.SUCCESS) continue;
//
//                if (descriptor.idVendor() == VENDOR_ID &&
//                        descriptor.idProduct() == PRODUCT_ID) {
//
//                    DeviceHandle handle = new DeviceHandle();
//                    result = LibUsb.open(device, handle);
//                    if (result == LibUsb.SUCCESS) {
//                        return handle;
//                    }
//                }
//            }
//            return null;
//        } finally {
//            LibUsb.freeDeviceList(list, true);
//        }
//    }
//
//    @Override
//    public List<String> listConnectedScanners() {
//        // Implementation to list connected USB scanners
//        return List.of("USB Scanner (Vendor: 0x" + Integer.toHexString(VENDOR_ID));
//    }
//}