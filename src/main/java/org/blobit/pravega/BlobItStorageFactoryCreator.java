/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blobit.pravega;

import io.pravega.segmentstore.storage.ConfigSetup;
import io.pravega.segmentstore.storage.StorageFactory;
import io.pravega.segmentstore.storage.StorageFactoryCreator;
import io.pravega.storage.extendeds3.ExtendedS3StorageConfig;
import io.pravega.storage.extendeds3.ExtendedS3StorageFactory;
import java.util.concurrent.ScheduledExecutorService;

/**
 *
 * @author eolivelli
 */
public class BlobItStorageFactoryCreator implements StorageFactoryCreator {
    
    @Override
    public StorageFactory createFactory(ConfigSetup setup, ScheduledExecutorService executor) {
        return new BlobItStorageFactory(setup.getConfig(BlobItStorageConfig::builder), executor);
    }

    @Override
    public String getName() {
        return "BLOBIT";
    }
}
