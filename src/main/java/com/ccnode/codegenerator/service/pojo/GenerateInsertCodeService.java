package com.ccnode.codegenerator.service.pojo;

import com.ccnode.codegenerator.dialog.InsertDialogResult;
import com.ccnode.codegenerator.dialog.InsertFileProp;
import com.ccnode.codegenerator.dialog.InsertFileType;
import com.ccnode.codegenerator.genCode.GenDaoService;
import com.ccnode.codegenerator.genCode.GenMapperService;
import com.ccnode.codegenerator.genCode.GenServiceService;
import com.ccnode.codegenerator.genCode.GenSqlService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by bruce.ge on 2016/12/26.
 */
public class GenerateInsertCodeService {

    private static Logger logger = LoggerFactory.getLogger(GenerateInsertCodeService.class);

    public static List<String> generateInsert(InsertDialogResult insertDialogResult) {
        Map<InsertFileType, InsertFileProp> fileProps = insertDialogResult.getFileProps();
        ExecutorService executorService = Executors.newFixedThreadPool(fileProps.size());
        List<String> errorMsgs = new CopyOnWriteArrayList<>();
        CountDownLatch latch = new CountDownLatch(fileProps.size());
        for (InsertFileType fileType : fileProps.keySet()) {
            executorService.submit(() -> {
                try {
                    generateFiles(fileType, fileProps, insertDialogResult);
                    //need to catch with exception.
                } catch (Exception e) {
                    logger.error("generate files catch exception", e);
                    //todo shall not use with getMessage
                    errorMsgs.add(e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        executorService.shutdown();
        return errorMsgs;
    }


    private static void generateFiles(InsertFileType type, Map<InsertFileType, InsertFileProp> propMap, InsertDialogResult insertDialogResult) {
        switch (type) {
            case SQL: {
                GenSqlService.generateSqlFile(propMap.get(type), insertDialogResult.getPropList(), insertDialogResult.getPrimaryProp(), insertDialogResult.getTableName());
                break;
            }
            case DAO: {
                GenDaoService.generateDaoFileUsingFtl(propMap.get(type), insertDialogResult.getSrcClass());
                break;
            }
            case MAPPER_XML: {
                GenMapperService.generateMapperXmlUsingFtl(propMap.get(type), insertDialogResult.getPropList(), insertDialogResult.getSrcClass(), propMap.get(InsertFileType.DAO), insertDialogResult.getTableName(), insertDialogResult.getPrimaryProp());
                break;
            }
            case SERVICE: {
                GenServiceService.generateServiceUsingFtl(propMap.get(type), insertDialogResult.getSrcClass(), propMap.get(InsertFileType.DAO));
                break;
            }
        }
    }


}
