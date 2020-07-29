/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blobit.pravega;

import io.pravega.segmentstore.storage.ConfigSetup;
import io.pravega.segmentstore.storage.StorageFactory;
import io.pravega.segmentstore.storage.StorageFactoryCreator;
import io.pravega.segmentstore.storage.StorageFactoryInfo;
import io.pravega.segmentstore.storage.StorageLayoutType;
import java.util.concurrent.ScheduledExecutorService;

/**
 *
 * @author eolivelli
 */
public class BlobItStorageFactoryCreator implements StorageFactoryCreator {

    @Override
    public StorageFactory createFactory(StorageFactoryInfo storageFactoryInfo, ConfigSetup setup, ScheduledExecutorService executor) {
        return new BlobItStorageFactory(setup.getConfig(BlobItStorageConfig::builder), executor);
    }

    @Override
    public StorageFactoryInfo[] getStorageFactories() {
        return new StorageFactoryInfo[]{
            StorageFactoryInfo.builder()
                    .name("BLOBIT")
                    .storageLayoutType(StorageLayoutType.CHUNKED_STORAGE)
                    .build()
        };
    }
}
