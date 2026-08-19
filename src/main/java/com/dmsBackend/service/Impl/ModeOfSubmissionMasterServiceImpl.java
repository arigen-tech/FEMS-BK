package com.dmsBackend.service.Impl;

import com.dmsBackend.entity.ModeOfSubmissionMaster;
import com.dmsBackend.repository.ModeOfSubmissionMasterRepository;
import com.dmsBackend.repository.MasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("modeOfSubmissionMasterService")
public class ModeOfSubmissionMasterServiceImpl extends GenericMasterServiceImpl<ModeOfSubmissionMaster> {

    @Autowired
    private ModeOfSubmissionMasterRepository repository;

    @Override
    protected MasterRepository<ModeOfSubmissionMaster> getRepository() {
        return repository;
    }

    @Override
    protected String getEntityLabel() {
        return "ModeOfSubmission";
    }

    @Override
    protected ModeOfSubmissionMaster newInstance() {
        return new ModeOfSubmissionMaster();
    }
}
