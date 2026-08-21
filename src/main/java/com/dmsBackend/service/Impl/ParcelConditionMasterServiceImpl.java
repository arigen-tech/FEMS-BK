package com.dmsBackend.service.Impl;



import com.dmsBackend.entity.ParcelConditionMaster;
import com.dmsBackend.repository.ParcelConditionMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("parcelConditionMasterService")
public class ParcelConditionMasterServiceImpl extends GenericMasterServiceImpl<ParcelConditionMaster> {

    @Autowired
    private ParcelConditionMasterRepository repository;

    @Override
    protected MasterRepository<ParcelConditionMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "ParcelCondition";
    }

    @Override
    protected ParcelConditionMaster newInstance() {
        return new ParcelConditionMaster();
    }
}
