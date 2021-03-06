package com.nextbreakpoint.flinkoperator.server.supervisor.task

import com.nextbreakpoint.flinkoperator.common.ManualAction
import com.nextbreakpoint.flinkoperator.server.supervisor.core.TaskContext
import com.nextbreakpoint.flinkoperator.testing.KotlinMockito.eq
import com.nextbreakpoint.flinkoperator.testing.KotlinMockito.given
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verifyNoMoreInteractions

class OnFailedTest {
    private val context = mock(TaskContext::class.java)
    private val task = OnFailed()
    private val actions = setOf(
        ManualAction.START,
        ManualAction.STOP,
        ManualAction.FORGET_SAVEPOINT
    )

    @BeforeEach
    fun configure() {
        given(context.isResourceDeleted()).thenReturn(false)
        given(context.suspendCluster()).thenReturn(true)
        given(context.isManualActionPresent()).thenReturn(false)
        given(context.shouldRestart()).thenReturn(false)
        given(context.hasTaskTimedOut()).thenReturn(false)
    }

    @Test
    fun `should behave as expected when nothing happens`() {
        task.execute(context)
        val inOrder = inOrder(context)
        inOrder.verify(context, times(1)).isResourceDeleted()
        inOrder.verify(context, times(1)).suspendCluster()
        inOrder.verify(context, times(1)).isManualActionPresent()
        inOrder.verify(context, times(1)).shouldRestart()
        verifyNoMoreInteractions(context)
    }

    @Test
    fun `should behave as expected when resource has been deleted`() {
        given(context.isResourceDeleted()).thenReturn(true)
        task.execute(context)
        val inOrder = inOrder(context)
        inOrder.verify(context, times(1)).isResourceDeleted()
        inOrder.verify(context, times(1)).onResourceDeleted()
        verifyNoMoreInteractions(context)
    }

    @Test
    fun `should behave as expected when cluster has not been suspended`() {
        given(context.suspendCluster()).thenReturn(false)
        task.execute(context)
        val inOrder = inOrder(context)
        inOrder.verify(context, times(1)).isResourceDeleted()
        inOrder.verify(context, times(1)).suspendCluster()
        verifyNoMoreInteractions(context)
    }

    @Test
    fun `should behave as expected when manual action is present`() {
        given(context.isManualActionPresent()).thenReturn(true)
        task.execute(context)
        val inOrder = inOrder(context)
        inOrder.verify(context, times(1)).isResourceDeleted()
        inOrder.verify(context, times(1)).suspendCluster()
        inOrder.verify(context, times(1)).isManualActionPresent()
        inOrder.verify(context, times(1)).executeManualAction(eq(actions))
        verifyNoMoreInteractions(context)
    }

    @Test
    fun `should behave as expected when job should restart but resource haven't changed`() {
        given(context.shouldRestart()).thenReturn(true)
        given(context.hasResourceChanged()).thenReturn(false)
        task.execute(context)
        val inOrder = inOrder(context)
        inOrder.verify(context, times(1)).isResourceDeleted()
        inOrder.verify(context, times(1)).suspendCluster()
        inOrder.verify(context, times(1)).isManualActionPresent()
        inOrder.verify(context, times(1)).shouldRestart()
        inOrder.verify(context, times(1)).hasResourceChanged()
        inOrder.verify(context, times(1)).hasTaskTimedOut()
        verifyNoMoreInteractions(context)
    }

    @Test
    fun `should behave as expected when job should restart and resource has changed`() {
        given(context.shouldRestart()).thenReturn(true)
        given(context.hasResourceChanged()).thenReturn(true)
        task.execute(context)
        val inOrder = inOrder(context)
        inOrder.verify(context, times(1)).isResourceDeleted()
        inOrder.verify(context, times(1)).suspendCluster()
        inOrder.verify(context, times(1)).isManualActionPresent()
        inOrder.verify(context, times(1)).shouldRestart()
        inOrder.verify(context, times(1)).hasResourceChanged()
        inOrder.verify(context, times(1)).onResourceChanged()
        verifyNoMoreInteractions(context)
    }

    @Test
    fun `should behave as expected when job should restart and timeout happened`() {
        given(context.shouldRestart()).thenReturn(true)
        given(context.hasTaskTimedOut()).thenReturn(true)
        task.execute(context)
        val inOrder = inOrder(context)
        inOrder.verify(context, times(1)).isResourceDeleted()
        inOrder.verify(context, times(1)).suspendCluster()
        inOrder.verify(context, times(1)).isManualActionPresent()
        inOrder.verify(context, times(1)).shouldRestart()
        inOrder.verify(context, times(1)).hasResourceChanged()
        inOrder.verify(context, times(1)).hasTaskTimedOut()
        inOrder.verify(context, times(1)).onClusterReadyToRestart()
        verifyNoMoreInteractions(context)
    }
}