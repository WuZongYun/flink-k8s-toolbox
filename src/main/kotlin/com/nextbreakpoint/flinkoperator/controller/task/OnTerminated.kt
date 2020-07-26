package com.nextbreakpoint.flinkoperator.controller.task

import com.nextbreakpoint.flinkoperator.common.model.ManualAction
import com.nextbreakpoint.flinkoperator.controller.core.Task
import com.nextbreakpoint.flinkoperator.controller.core.TaskContext

class OnTerminated : Task() {
    private val actions = setOf(
        ManualAction.START,
        ManualAction.FORGET_SAVEPOINT
    )

    override fun execute(context: TaskContext) {
        if (context.isResourceDeleted()) {
            context.removeFinalizer()
            return
        }

        if (!context.terminateCluster()) {
            return
        }

        if (context.isManualActionPresent()) {
            context.executeManualAction(actions)
            return
        }
    }
}