package com.nextbreakpoint.flinkoperator.server.controller.action

import com.nextbreakpoint.flinkoperator.common.ClusterSelector
import com.nextbreakpoint.flinkoperator.common.ClusterStatus
import com.nextbreakpoint.flinkoperator.common.FlinkOptions
import com.nextbreakpoint.flinkoperator.common.ManualAction
import com.nextbreakpoint.flinkoperator.common.StartOptions
import com.nextbreakpoint.flinkoperator.server.common.FlinkClient
import com.nextbreakpoint.flinkoperator.server.common.KubeClient
import com.nextbreakpoint.flinkoperator.server.common.Annotations
import com.nextbreakpoint.flinkoperator.server.controller.ControllerContext
import com.nextbreakpoint.flinkoperator.server.controller.core.ResultStatus
import com.nextbreakpoint.flinkoperator.server.common.Status
import com.nextbreakpoint.flinkoperator.testing.KotlinMockito
import com.nextbreakpoint.flinkoperator.testing.KotlinMockito.any
import com.nextbreakpoint.flinkoperator.testing.KotlinMockito.eq
import com.nextbreakpoint.flinkoperator.testing.TestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class RequestClusterStartTest {
    private val clusterSelector = ClusterSelector(namespace = "flink", name = "test", uuid = "123")
    private val cluster = TestFactory.aCluster(name = "test", namespace = "flink")
    private val flinkOptions = FlinkOptions(hostname = "localhost", portForward = null, useNodePort = false)
    private val flinkClient = mock(FlinkClient::class.java)
    private val kubeClient = mock(KubeClient::class.java)
    private val supervisorContext = ControllerContext(cluster)
    private val command = RequestClusterStart(flinkOptions, flinkClient, kubeClient, supervisorContext)

    @BeforeEach
    fun configure() {
        Status.setClusterStatus(cluster, ClusterStatus.Running)
    }

    @Test
    fun `should fail when kubeClient throws exception`() {
        KotlinMockito.given(kubeClient.updateAnnotations(eq(clusterSelector), any())).thenThrow(RuntimeException::class.java)
        val result = command.execute(clusterSelector, StartOptions(withoutSavepoint = true))
        verify(kubeClient, times(1)).updateAnnotations(eq(clusterSelector), any())
        verifyNoMoreInteractions(kubeClient)
        verifyNoMoreInteractions(flinkClient)
        assertThat(result).isNotNull()
        assertThat(result.status).isEqualTo(ResultStatus.ERROR)
        assertThat(result.output).isNull()
    }

    @Test
    fun `should return expected result when starting without savepoint`() {
        Status.setClusterStatus(cluster, ClusterStatus.Terminated)
        val actionTimestamp = Annotations.getActionTimestamp(cluster)
        val result = command.execute(clusterSelector, StartOptions(withoutSavepoint = true))
        verify(kubeClient, times(1)).updateAnnotations(eq(clusterSelector), any())
        verifyNoMoreInteractions(kubeClient)
        verifyNoMoreInteractions(flinkClient)
        assertThat(result).isNotNull()
        assertThat(result.status).isEqualTo(ResultStatus.OK)
        assertThat(result.output).isNull()
        assertThat(Annotations.getManualAction(cluster)).isEqualTo(ManualAction.START)
        assertThat(Annotations.isWithoutSavepoint(cluster)).isEqualTo(true)
        assertThat(Annotations.getActionTimestamp(cluster)).isNotEqualTo(actionTimestamp)
    }

    @Test
    fun `should return expected result when starting with savepoint`() {
        Status.setClusterStatus(cluster, ClusterStatus.Terminated)
        val actionTimestamp = Annotations.getActionTimestamp(cluster)
        val result = command.execute(clusterSelector, StartOptions(withoutSavepoint = false))
        verify(kubeClient, times(1)).updateAnnotations(eq(clusterSelector), any())
        verifyNoMoreInteractions(kubeClient)
        verifyNoMoreInteractions(flinkClient)
        assertThat(result).isNotNull()
        assertThat(result.status).isEqualTo(ResultStatus.OK)
        assertThat(result.output).isNull()
        assertThat(Annotations.getManualAction(cluster)).isEqualTo(ManualAction.START)
        assertThat(Annotations.isWithoutSavepoint(cluster)).isEqualTo(false)
        assertThat(Annotations.getActionTimestamp(cluster)).isNotEqualTo(actionTimestamp)
    }
}