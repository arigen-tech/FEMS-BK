package com.dmsBackend.response;

import lombok.Data;

@Data
public class CartridgeInfo {

    private String drive;
    private String cartridge;
    private String slot;
    private String library;
    private String error;

}
