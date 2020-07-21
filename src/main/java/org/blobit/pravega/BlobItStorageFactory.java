/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blobit.pravega;

import com.google.common.base.Preconditions;
import io.pravega.segmentstore.storage.AsyncStorageWrapper;
import io.pravega.segmentstore.storage.Storage;
import io.pravega.segmentstore.storage.StorageFactory;
import io.pravega.segmentstore.storage.rolling.RollingStorage;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.blobit.core.api.ObjectManagerException;

public class BlobItStorageFactory implements StorageFactory {
    
    private final BlobItStorageConfig config;
    private final ExecutorService executor;

    /**
     * Creates a new instance of the FileSystemStorageFactory class.
     *
     * @param config   The Configuration to use.
     * @param executor An executor to use for background operations.
     */
    public BlobItStorageFactory(BlobItStorageConfig config, ExecutorService executor) {
        Preconditions.checkNotNull(config, "config");
        Preconditions.checkNotNull(executor, "executor");
        this.config = config;
        this.executor = executor;
    }

    @Override
    public Storage createStorageAdapter() {
        try {
            BlobItStorage s = new BlobItStorage(this.config);
            // return new AsyncStorageWrapper(new RollingStorage(s), this.executor);
            throw new RuntimeException("....waiting for next commit in feture branch...");
        } catch (ObjectManagerException ex) {
            throw new RuntimeException(ex);
        }
    }

}
