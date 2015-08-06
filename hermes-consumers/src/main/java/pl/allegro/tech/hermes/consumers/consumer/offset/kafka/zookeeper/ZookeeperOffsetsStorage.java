package pl.allegro.tech.hermes.consumers.consumer.offset.kafka.zookeeper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import org.apache.curator.framework.CuratorFramework;
import pl.allegro.tech.hermes.api.Subscription;
import pl.allegro.tech.hermes.common.di.CuratorType;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.consumers.consumer.offset.OffsetsStorage;
import pl.allegro.tech.hermes.domain.subscription.offset.PartitionOffset;

import javax.inject.Inject;
import javax.inject.Named;

public class ZookeeperOffsetsStorage implements OffsetsStorage {

    private static final String OFFSET_PATTERN_PATH = "/consumers/%s/offsets/%s";

    private final CuratorFramework curatorFramework;

    @Inject
    public ZookeeperOffsetsStorage(@Named(CuratorType.KAFKA) CuratorFramework curatorFramework) {
        this.curatorFramework = curatorFramework;
    }

    @Override
    public void setSubscriptionOffset(Subscription subscription, PartitionOffset partitionOffset) {
        try {
            Long actualOffset = convertByteArrayToLong(curatorFramework.getData()
                    .forPath(getPartitionOffsetPath(subscription, partitionOffset.getPartition())));

            if (actualOffset > partitionOffset.getOffset()) {
                curatorFramework.setData().forPath(
                        getPartitionOffsetPath(subscription, partitionOffset.getPartition()),
                        Long.valueOf(partitionOffset.getOffset()).toString().getBytes(Charsets.UTF_8)
                );
            }
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    private Long convertByteArrayToLong(byte[] data) {
        return Long.valueOf(new String(data, Charsets.UTF_8));
    }

    @Override
    public long getSubscriptionOffset(Subscription subscription, int partitionId) {
        try {
            byte[] offset = curatorFramework.getData().forPath(getPartitionOffsetPath(subscription, partitionId));
            return Long.valueOf(new String(offset));
        } catch (Exception e) {
            throw new InternalProcessingException(e);
        }
    }

    @VisibleForTesting
    protected String getPartitionOffsetPath(Subscription subscription, int partition) {
        return Joiner.on("/").join(getOffsetPath(subscription), partition);
    }

    private static String getOffsetPath(Subscription subscription) {
        return String.format(OFFSET_PATTERN_PATH, subscription.getId(), subscription.getQualifiedTopicName());
    }
}