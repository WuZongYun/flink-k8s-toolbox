package com.nextbreakpoint.flinkoperator.controller.task

import com.nextbreakpoint.flinkoperator.common.model.ClusterId
import com.nextbreakpoint.flinkoperator.controller.core.OperationResult
import com.nextbreakpoint.flinkoperator.controller.core.OperationStatus
import com.nextbreakpoint.flinkoperator.controller.core.Status
import com.nextbreakpoint.flinkoperator.controller.core.TaskAction
import com.nextbreakpoint.flinkoperator.controller.core.TaskContext
import com.nextbreakpoint.flinkoperator.controller.core.Timeout
import com.nextbreakpoint.flinkoperator.testing.KotlinMockito.eq
import com.nextbreakpoint.flinkoperator.testing.KotlinMockito.given
import com.nextbreakpoint.flinkoperator.testing.TestFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

class StartJobTest {
    private val clusterId = ClusterId(namespace = "flink", name = "test", uuid = "123")
    private val cluster = TestFactory.aCluster(name = "test", namespace = "flink")
    private val context = mock(TaskContext::class.java)
    private val task = StartJob()

    @BeforeEach
    fun configure() {
        Status.setBootstrap(cluster, cluster.spec.bootstrap)
        given(context.flinkCluster).thenReturn(cluster)
        given(context.clusterId).thenReturn(clusterId)
        given(context.timeSinceLastUpdateInSeconds()).thenReturn(0)
    }

    @Test
    fun `onExecuting should return expected result when operation times out`() {
        given(context.timeSinceLastUpdateInSeconds()).thenReturn(Timeout.STARTING_JOB_TIMEOUT + 1)
        val result = task.onExecuting(context)
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.FAIL)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onExecuting should return expected result when job has been started already`() {
        given(context.isJobStarted(eq(clusterId))).thenReturn(OperationResult(OperationStatus.COMPLETED, null))
        val result = task.onExecuting(context)
        verify(context, atLeastOnce()).clusterId
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verify(context, Mockito.times(1)).isJobStarted(eq(clusterId))
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.SKIP)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onExecuting should return expected result when job has not been started yet`() {
        given(context.isJobStarted(eq(clusterId))).thenReturn(OperationResult(OperationStatus.RETRY, null))
        given(context.startJob(eq(clusterId), eq(cluster))).thenReturn(OperationResult(OperationStatus.RETRY, null))
        val result = task.onExecuting(context)
        verify(context, atLeastOnce()).clusterId
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verify(context, Mockito.times(1)).isJobStarted(eq(clusterId))
        verify(context, Mockito.times(1)).startJob(eq(clusterId), eq(cluster))
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.REPEAT)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onExecuting should return expected result when job has failed`() {
        given(context.isJobStarted(eq(clusterId))).thenReturn(OperationResult(OperationStatus.RETRY, null))
        given(context.startJob(eq(clusterId), eq(cluster))).thenReturn(OperationResult(OperationStatus.FAILED, null))
        val result = task.onExecuting(context)
        verify(context, atLeastOnce()).clusterId
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verify(context, Mockito.times(1)).isJobStarted(eq(clusterId))
        verify(context, Mockito.times(1)).startJob(eq(clusterId), eq(cluster))
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.REPEAT)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onExecuting should return expected result when job has been started`() {
        given(context.isJobStarted(eq(clusterId))).thenReturn(OperationResult(OperationStatus.RETRY, null))
        given(context.startJob(eq(clusterId), eq(cluster))).thenReturn(OperationResult(OperationStatus.COMPLETED, null))
        val result = task.onExecuting(context)
        verify(context, atLeastOnce()).clusterId
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verify(context, Mockito.times(1)).isJobStarted(eq(clusterId))
        verify(context, Mockito.times(1)).startJob(eq(clusterId), eq(cluster))
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.NEXT)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onAwaiting should return expected result when operation times out`() {
        given(context.timeSinceLastUpdateInSeconds()).thenReturn(Timeout.STARTING_JOB_TIMEOUT + 1)
        val result = task.onAwaiting(context)
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.FAIL)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onAwaiting should return expected result when job has been started`() {
        given(context.isJobStarted(eq(clusterId))).thenReturn(OperationResult(OperationStatus.COMPLETED, null))
        val result = task.onAwaiting(context)
        verify(context, atLeastOnce()).clusterId
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verify(context, Mockito.times(1)).isJobStarted(eq(clusterId))
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.NEXT)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onAwaiting should return expected result when job has not been started yet`() {
        given(context.isJobStarted(eq(clusterId))).thenReturn(OperationResult(OperationStatus.RETRY, null))
        val result = task.onAwaiting(context)
        verify(context, atLeastOnce()).clusterId
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verify(context, Mockito.times(1)).isJobStarted(eq(clusterId))
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.REPEAT)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onAwaiting should return expected result when job can't be started`() {
        given(context.isJobStarted(eq(clusterId))).thenReturn(OperationResult(OperationStatus.FAILED, null))
        val result = task.onAwaiting(context)
        verify(context, atLeastOnce()).clusterId
        verify(context, atLeastOnce()).flinkCluster
        verify(context, atLeastOnce()).timeSinceLastUpdateInSeconds()
        verify(context, Mockito.times(1)).isJobStarted(eq(clusterId))
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.REPEAT)
        assertThat(result.output).isNotBlank()
    }

    @Test
    fun `onIdle should return expected result`() {
        val result = task.onIdle(context)
        verify(context, atLeastOnce()).flinkCluster
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.NEXT)
        assertThat(result.output).isNotNull()
    }

    @Test
    fun `onFailed should return expected result`() {
        val result = task.onFailed(context)
        verify(context, atLeastOnce()).flinkCluster
        verifyNoMoreInteractions(context)
        assertThat(result).isNotNull()
        assertThat(result.action).isEqualTo(TaskAction.REPEAT)
        assertThat(result.output).isNotNull()
    }
}