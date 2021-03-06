/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.systemtest.utils.kafkaUtils;

import io.strimzi.api.kafka.Crds;
import io.strimzi.systemtest.Constants;
import io.strimzi.test.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static io.strimzi.test.k8s.KubeClusterResource.kubeClient;

public class KafkaConnectS2IUtils {

    private static final Logger LOGGER = LogManager.getLogger(KafkaConnectS2IUtils.class);

    private KafkaConnectS2IUtils() {}

    /**
     * Wait until the given Kafka Connect S2I cluster is in desired state.
     * @param name The name of the Kafka Connect S2I cluster.
     * @param status desired status value
     */
    public static void waitForConnectS2IStatus(String name, String status) {
        LOGGER.info("Waiting for Kafka Connect S2I {} state: {}", name, status);
        TestUtils.waitFor("Kafka Connect S2I " + name + " state: " + status, Constants.POLL_INTERVAL_FOR_RESOURCE_READINESS, Constants.TIMEOUT_FOR_RESOURCE_READINESS,
            () -> Crds.kafkaConnectS2iOperation(kubeClient().getClient()).inNamespace(kubeClient().getNamespace()).withName(name).get().getStatus().getConditions().get(0).getType().equals(status));
        LOGGER.info("Kafka Connect S2I {} is in desired state: {}", name, status);
    }
}
