/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.blobit.pravega;

import com.google.common.base.Preconditions;
import io.pravega.common.util.ConfigBuilder;
import io.pravega.common.util.ConfigurationException;
import io.pravega.common.util.Property;
import io.pravega.common.util.TypedProperties;
import lombok.Getter;

/**
 *
 * @author eolivelli
 */
public class BlobItStorageConfig {
    
    public static final Property<String> CONFIGURI = Property.named("blobit.config.uri", "", "configUri");
    public static final Property<String> BUCKET = Property.named("bucket", "");
    
    
    @Getter
    private final String bkUri;

    @Getter
    private final String bucket;
    
    private BlobItStorageConfig(TypedProperties properties) throws ConfigurationException {
        this.bucket = Preconditions.checkNotNull(properties.get(BUCKET), "bucket");
        this.bkUri = Preconditions.checkNotNull(properties.get(CONFIGURI), "blobkit.config.uri");
    }

    /**
     * Creates a new ConfigBuilder that can be used to create instances of this class.
     *
     * @return A new Builder for this class.
     */
    public static ConfigBuilder<BlobItStorageConfig> builder() {
        return new ConfigBuilder<>("BLOBIT", BlobItStorageConfig::new);
    }

    //endregion
}
