package com.dmsBackend.controller;

import com.dmsBackend.entity.RoleMaster;
import com.dmsBackend.entity.TypeMaster;
import com.dmsBackend.exception.ResourceNotFoundException;
import com.dmsBackend.payloads.ApiResponse;
import com.dmsBackend.service.TypeMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("TypeMaster")
//@CrossOrigin("https://happytyagi.github.io/DmsFrontend/")
public class TypeMasterController {
    @Autowired
    TypeMasterService typeMasterService;

    @PostMapping("/save")
    public ResponseEntity<TypeMaster>createTypeMaster(@RequestBody TypeMaster typeMaster){
        TypeMaster savetypeMaster = this.typeMasterService.saveTypeMaster(typeMaster);
        return new ResponseEntity<TypeMaster>(savetypeMaster, HttpStatus.CREATED);
    }

    @PutMapping("update/{id}")
    public ResponseEntity<TypeMaster> updateTypeMaster(@PathVariable Integer id, @RequestBody TypeMaster typeMaster) {
        try {
            TypeMaster updatedtypeMaster = typeMasterService.updateTypeMaster(typeMaster, id);
            return new ResponseEntity<>(updatedtypeMaster, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<TypeMaster> deletebyIdTypeMaster(@PathVariable Integer id) {
        this.typeMasterService.deleteByIdTypeMaster(id);
        return new ResponseEntity(new ApiResponse("TypeMaster deleted successfully", true), HttpStatus.OK);
    }

    @GetMapping("/findAll")
    public ResponseEntity<List<TypeMaster>> findAllTypeMaster() {
        List<TypeMaster> allTypeMasterMaster = this.typeMasterService.findAllTypeMaster();
        return new ResponseEntity(allTypeMasterMaster, HttpStatus.OK);
    }

    @GetMapping("/findById/{id}")
    public ResponseEntity<TypeMaster> findByIdTypeMaster(@PathVariable Integer id) {
        Optional<TypeMaster> typeMaster = typeMasterService.findTypeMasterById(id);
        return typeMaster.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PutMapping("updatestatus/{id}")
    public ResponseEntity<TypeMaster> updateRoleStatus(@PathVariable Integer id, @RequestBody TypeMaster typeMaster) {
        try {
            TypeMaster typeMaster1 = typeMasterService.updateStatus(id,typeMaster.getIsActive());
            return new ResponseEntity<>(typeMaster1, HttpStatus.OK);
        } catch (ResourceNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}