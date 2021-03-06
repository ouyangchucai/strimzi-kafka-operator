/*
 * Copyright Strimzi authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.strimzi.operator.cluster.model;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.PodSecurityContextBuilder;
import io.fabric8.kubernetes.api.model.Probe;
import io.strimzi.api.kafka.model.ProbeBuilder;
import io.strimzi.api.kafka.model.storage.EphemeralStorageBuilder;
import io.strimzi.api.kafka.model.storage.JbodStorageBuilder;
import io.strimzi.api.kafka.model.storage.PersistentClaimStorageBuilder;
import io.strimzi.api.kafka.model.storage.Storage;
import io.strimzi.api.kafka.model.template.PodDisruptionBudgetTemplate;
import io.strimzi.api.kafka.model.template.PodDisruptionBudgetTemplateBuilder;
import io.strimzi.api.kafka.model.template.PodTemplate;
import io.strimzi.api.kafka.model.template.PodTemplateBuilder;
import io.strimzi.operator.cluster.KafkaVersionTestUtils;
import io.strimzi.operator.common.model.Labels;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.strimzi.operator.common.Util.parseMap;

public class ModelUtilsTest {

    @Test
    public void testParseImageMap() {
        Map<String, String> m = parseMap(
                KafkaVersionTestUtils.LATEST_KAFKA_VERSION + "=" + KafkaVersionTestUtils.LATEST_KAFKA_IMAGE + "\n  " +
                        KafkaVersionTestUtils.PREVIOUS_KAFKA_VERSION + "=" + KafkaVersionTestUtils.PREVIOUS_KAFKA_IMAGE + "\n ");
        assertThat(m.size(), is(2));
        assertThat(m.get(KafkaVersionTestUtils.LATEST_KAFKA_VERSION), is(KafkaVersionTestUtils.LATEST_KAFKA_IMAGE));
        assertThat(m.get(KafkaVersionTestUtils.PREVIOUS_KAFKA_VERSION), is(KafkaVersionTestUtils.PREVIOUS_KAFKA_IMAGE));

        m = parseMap(
                KafkaVersionTestUtils.LATEST_KAFKA_VERSION + "=" + KafkaVersionTestUtils.LATEST_KAFKA_IMAGE + "," +
                KafkaVersionTestUtils.PREVIOUS_KAFKA_VERSION + "=" + KafkaVersionTestUtils.PREVIOUS_KAFKA_IMAGE);
        assertThat(m.size(), is(2));
        assertThat(m.get(KafkaVersionTestUtils.LATEST_KAFKA_VERSION), is(KafkaVersionTestUtils.LATEST_KAFKA_IMAGE));
        assertThat(m.get(KafkaVersionTestUtils.PREVIOUS_KAFKA_VERSION), is(KafkaVersionTestUtils.PREVIOUS_KAFKA_IMAGE));
    }

    @Test
    public void testAnnotationsOrLabelsImageMap() {
        Map<String, String> m = parseMap(" discovery.3scale.net=true");
        assertThat(m.size(), is(1));
        assertThat(m.get("discovery.3scale.net"), is("true"));

        m = parseMap(" discovery.3scale.net/scheme=http\n" +
                "        discovery.3scale.net/port=8080\n" +
                "        discovery.3scale.net/path=path/\n" +
                "        discovery.3scale.net/description-path=oapi/");
        assertThat(m.size(), is(4));
        assertThat(m.get("discovery.3scale.net/scheme"), is("http"));
        assertThat(m.get("discovery.3scale.net/port"), is("8080"));
        assertThat(m.get("discovery.3scale.net/path"), is("path/"));
        assertThat(m.get("discovery.3scale.net/description-path"), is("oapi/"));
    }

    @Test
    public void testParsePodDisruptionBudgetTemplate()  {
        PodDisruptionBudgetTemplate template = new PodDisruptionBudgetTemplateBuilder()
                .withNewMetadata()
                .withAnnotations(Collections.singletonMap("annoKey", "annoValue"))
                .withLabels(Collections.singletonMap("labelKey", "labelValue"))
                .endMetadata()
                .withMaxUnavailable(2)
                .build();

        Model model = new Model();

        ModelUtils.parsePodDisruptionBudgetTemplate(model, template);
        assertThat(model.templatePodDisruptionBudgetLabels, is(Collections.singletonMap("labelKey", "labelValue")));
        assertThat(model.templatePodDisruptionBudgetAnnotations, is(Collections.singletonMap("annoKey", "annoValue")));
        assertThat(model.templatePodDisruptionBudgetMaxUnavailable, is(2));
    }

    @Test
    public void testParseNullPodDisruptionBudgetTemplate()  {
        Model model = new Model();

        ModelUtils.parsePodDisruptionBudgetTemplate(model, null);
        assertThat(model.templatePodDisruptionBudgetLabels, is(nullValue()));
        assertThat(model.templatePodDisruptionBudgetAnnotations, is(nullValue()));
        assertThat(model.templatePodDisruptionBudgetMaxUnavailable, is(1));
    }

    @Test
    public void testParsePodTemplate()  {
        LocalObjectReference secret1 = new LocalObjectReference("some-pull-secret");
        LocalObjectReference secret2 = new LocalObjectReference("some-other-pull-secret");

        PodTemplate template = new PodTemplateBuilder()
                .withNewMetadata()
                .withAnnotations(Collections.singletonMap("annoKey", "annoValue"))
                .withLabels(Collections.singletonMap("labelKey", "labelValue"))
                .endMetadata()
                .withSecurityContext(new PodSecurityContextBuilder().withFsGroup(123L).withRunAsGroup(456L).withRunAsUser(789L).build())
                .withImagePullSecrets(secret1, secret2)
                .withTerminationGracePeriodSeconds(123)
                .build();

        Model model = new Model();

        ModelUtils.parsePodTemplate(model, template);
        assertThat(model.templatePodLabels, is(Collections.singletonMap("labelKey", "labelValue")));
        assertThat(model.templatePodAnnotations, is(Collections.singletonMap("annoKey", "annoValue")));
        assertThat(model.templateTerminationGracePeriodSeconds, is(123));
        assertThat(model.templateImagePullSecrets.size(), is(2));
        assertThat(model.templateImagePullSecrets.contains(secret1), is(true));
        assertThat(model.templateImagePullSecrets.contains(secret2), is(true));
        assertThat(model.templateSecurityContext, is(notNullValue()));
        assertThat(model.templateSecurityContext.getFsGroup(), is(Long.valueOf(123)));
        assertThat(model.templateSecurityContext.getRunAsGroup(), is(Long.valueOf(456)));
        assertThat(model.templateSecurityContext.getRunAsUser(), is(Long.valueOf(789)));
    }

    @Test
    public void testParseNullPodTemplate()  {
        Model model = new Model();

        ModelUtils.parsePodTemplate(model, null);
        assertThat(model.templatePodLabels, is(nullValue()));
        assertThat(model.templatePodAnnotations, is(nullValue()));
        assertThat(model.templateImagePullSecrets, is(nullValue()));
        assertThat(model.templateSecurityContext, is(nullValue()));
        assertThat(model.templateTerminationGracePeriodSeconds, is(30));
    }

    private class Model extends AbstractModel   {
        public Model()  {
            super("", "", Labels.EMPTY);
        }

        @Override
        protected String getDefaultLogConfigFileName() {
            return null;
        }

        @Override
        protected List<Container> getContainers(ImagePullPolicy imagePullPolicy) {
            return null;
        }
    }

    @Test
    public void testStorageSerializationAndDeserialization()    {
        Storage jbod = new JbodStorageBuilder().withVolumes(
                new PersistentClaimStorageBuilder().withStorageClass("gp2-ssd").withDeleteClaim(false).withId(0).withSize("100Gi").build(),
                new PersistentClaimStorageBuilder().withStorageClass("gp2-st1").withDeleteClaim(true).withId(1).withSize("1000Gi").build())
                .build();

        Storage ephemeral = new EphemeralStorageBuilder().build();

        Storage persistent = new PersistentClaimStorageBuilder().withStorageClass("gp2-ssd").withDeleteClaim(false).withId(0).withSize("100Gi").build();

        assertThat(ModelUtils.decodeStorageFromJson(ModelUtils.encodeStorageToJson(jbod)), is(jbod));
        assertThat(ModelUtils.decodeStorageFromJson(ModelUtils.encodeStorageToJson(ephemeral)), is(ephemeral));
        assertThat(ModelUtils.decodeStorageFromJson(ModelUtils.encodeStorageToJson(persistent)), is(persistent));
    }

    @Test
    public void testCreateTcpSocketProbe()  {
        Probe probe = ModelUtils.createTcpSocketProbe(1234, new ProbeBuilder().withInitialDelaySeconds(10).withTimeoutSeconds(20).build());
        assertThat(probe.getTcpSocket().getPort().getIntVal(), is(new Integer(1234)));
        assertThat(probe.getInitialDelaySeconds(), is(new Integer(10)));
        assertThat(probe.getTimeoutSeconds(), is(new Integer(20)));
    }
}
