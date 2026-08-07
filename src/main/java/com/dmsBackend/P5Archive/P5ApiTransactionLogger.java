package com.dmsBackend.P5Archive;

import com.dmsBackend.entity.RetentionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class P5ApiTransactionLogger {

    private final P5ApiTransactionsRepository repo;

    public P5ApiTransactions create(String method, String url, RetentionPolicy retaintinId, String apiType) {
        P5ApiTransactions tx = new P5ApiTransactions();
        tx.setHttpMethod(method);
        tx.setApiUrl(url);
        tx.setApiType(apiType);
        tx.setRetentionPolicy(retaintinId);
        return repo.save(tx);
    }

    public void update(P5ApiTransactions tx) {
        repo.save(tx);
    }
}
