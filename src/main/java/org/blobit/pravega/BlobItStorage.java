package org.blobit.pravega;

import herddb.jdbc.HerdDBEmbeddedDataSource;
import io.pravega.segmentstore.storage.chunklayer.BaseChunkStorage;
import io.pravega.segmentstore.storage.chunklayer.ChunkHandle;
import io.pravega.segmentstore.storage.chunklayer.ChunkInfo;
import io.pravega.segmentstore.storage.chunklayer.ChunkNotFoundException;
import io.pravega.segmentstore.storage.chunklayer.ChunkStorageException;
import io.pravega.segmentstore.storage.chunklayer.ConcatArgument;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import org.blobit.core.api.BucketConfiguration;
import org.blobit.core.api.BucketHandle;
import org.blobit.core.api.Configuration;
import org.blobit.core.api.NamedObjectMetadata;
import org.blobit.core.api.ObjectManager;
import org.blobit.core.api.ObjectManagerException;
import org.blobit.core.api.ObjectManagerFactory;
import org.blobit.core.api.PutOptions;

/**
 * BlobIt based ChunkStorage implementation
 *
 * @author eolivelli
 */
public class BlobItStorage extends BaseChunkStorage {

    private final ObjectManager objectManager;
    private final HerdDBEmbeddedDataSource datasource;
    private final BucketHandle bucket;

    BlobItStorage(BlobItStorageConfig config) throws ObjectManagerException {
        Configuration configuration
                = new Configuration()
                        .setType(Configuration.TYPE_BOOKKEEPER)
                        .setUseTablespaces(false)
                        .setConcurrentWriters(10)
                        .setEmptyLedgerMinTtl(0)
                        .setZookeeperUrl(config.getBkUri());
        Properties dsProperties = new Properties();
        datasource = new HerdDBEmbeddedDataSource(dsProperties);
        objectManager = ObjectManagerFactory.
                createObjectManager(configuration, datasource);
        objectManager.start();
        if (objectManager.getBucketMetadata(config.getBucket()) == null) {
            objectManager.createBucket(config.getBucket(), config.getBucket(), BucketConfiguration.DEFAULT);
        }
        bucket = objectManager.getBucket(config.getBucket());
    }

    @Override
    public void close() {
        objectManager.close();
        datasource.close();
        super.close();
    }

    @Override
    public boolean supportsTruncation() {
        return false;
    }

    @Override
    public boolean supportsAppend() {
        return true;
    }

    @Override
    public boolean supportsConcat() {
        return true;
    }

    @Override
    protected ChunkInfo doGetInfo(String chunkName) throws ChunkStorageException {
        try {
            NamedObjectMetadata metadata = bucket.statByName(chunkName);
            if (metadata == null) {
                throw new ChunkNotFoundException(chunkName, "");
            }
            return ChunkInfo.builder().name(chunkName).length(metadata.getSize()).build();
        } catch (ObjectManagerException ex) {
            // BAD CONSTRUCTOR
            throw new ChunkStorageException(chunkName, "", ex);
        }
    }

    @Override
    protected ChunkHandle doCreate(String chunkName) throws ChunkStorageException {
        return ChunkHandle.writeHandle(chunkName);
    }

    @Override
    protected boolean checkExists(String chunkName) throws ChunkStorageException {
        try {
            NamedObjectMetadata metadata = bucket.statByName(chunkName);
            return metadata != null;
        } catch (ObjectManagerException ex) {
            // BAD CONSTRUCTOR
            throw new ChunkStorageException(chunkName, "", ex);
        }
    }

    @Override
    protected void doDelete(ChunkHandle handle) throws ChunkStorageException {
        try {
            bucket
                    .deleteByName(handle.getChunkName())
                    .get();

        } catch (ObjectManagerException ex) {
            // BAD CONSTRUCTOR
            throw new ChunkStorageException(handle.getChunkName(), "", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // BAD CONSTRUCTOR
            throw new ChunkStorageException(handle.getChunkName(), "", ex);
        }
    }

    @Override
    protected ChunkHandle doOpenRead(String chunkName) throws ChunkStorageException {
        return ChunkHandle.readHandle(chunkName);
    }

    @Override
    protected ChunkHandle doOpenWrite(String chunkName) throws ChunkStorageException {
        return ChunkHandle.writeHandle(chunkName);
    }

    private static class BufferOutputStream extends OutputStream {

        private final byte[] buffer;
        private int pos;

        public BufferOutputStream(byte[] buffer, int pos) {
            this.buffer = buffer;
            this.pos = pos;
        }

        @Override
        public void write(int b) throws IOException {
            buffer[pos++] = (byte) b;
        }

    }

    @Override
    protected int doRead(ChunkHandle handle, long fromOffset, int length, byte[] buffer, int bufferOffset) throws ChunkStorageException {
        AtomicLong read = new AtomicLong();
        try {
            bucket.downloadByName(handle.getChunkName(), (numBytes) -> {
                read.set(numBytes);
            },
                    new BufferOutputStream(buffer, bufferOffset),
                    (int) fromOffset, length)
                    .get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ChunkStorageException(handle.getChunkName(), "", ex);
        } catch (ObjectManagerException ex) {
            throw new ChunkStorageException(handle.getChunkName(), "", ex);
        }
        return read.intValue();
    }

    @Override
    protected int doWrite(ChunkHandle handle, long offset, int length, InputStream data) throws ChunkStorageException {
        try {
            if (offset == 0) {
                bucket.put(handle.getChunkName(), length, data, PutOptions.OVERWRITE).get();
            } else {
                // it is expected to be offset == current size
                bucket.put(handle.getChunkName(), length, data, PutOptions.APPEND).get();
            }
            return length;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ChunkStorageException(handle.getChunkName(), "", ex);
        } catch (ObjectManagerException ex) {
            throw new ChunkStorageException(handle.getChunkName(), "", ex);
        }
    }

    @Override
    protected int doConcat(ConcatArgument[] chunks) throws ChunkStorageException, UnsupportedOperationException {
        String first = null;
        int total = 0;
        for (ConcatArgument concat : chunks) {
            if (first == null) {
                first = concat.getName();
            } else {
                try {
                    bucket.concat(first, concat.getName());
                } catch (ObjectManagerException ex) {
                    throw new ChunkStorageException(concat.getName(), "error during concat with " + first, ex);
                }
            }
            total += (int) concat.getLength();
        }
        return total;
    }

    @Override
    protected void doSetReadOnly(ChunkHandle handle, boolean isReadOnly) throws ChunkStorageException, UnsupportedOperationException {
        // not supported
    }

    @Override
    protected boolean doTruncate(ChunkHandle handle, long offset) throws ChunkStorageException, UnsupportedOperationException {
        return false;
    }


}
