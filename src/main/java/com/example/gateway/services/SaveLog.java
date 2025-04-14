package com.example.gateway.services;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.gateway.entity.ActiveLogEntity;
import com.example.gateway.entity.TransactionEntity;


public class SaveLog {
    @Autowired
    public static ActiveLogEntity createActiveLog(TransactionEntity transactionEntity, String logStage) {
        ActiveLogEntity activeLog = new ActiveLogEntity();
        activeLog.setTransaction(transactionEntity);
        activeLog.setLogStage(logStage);
        return activeLog;
    }
}
