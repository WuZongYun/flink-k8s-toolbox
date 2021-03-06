package com.nextbreakpoint.flinkoperator.server.common

import com.nextbreakpoint.flinkoperator.common.crd.V1FlinkCluster

object Configuration {
    fun getSavepointMode(flinkCluster: V1FlinkCluster) : String =
        flinkCluster.spec?.operator?.savepointMode ?: "MANUAL"

    fun getSavepointPath(flinkCluster: V1FlinkCluster) : String? =
        flinkCluster.spec?.operator?.savepointPath?.trim('\"')

    fun getSavepointInterval(flinkCluster: V1FlinkCluster) : Long =
        flinkCluster.spec?.operator?.savepointInterval?.toLong() ?: 3600

    fun getSavepointTargetPath(flinkCluster: V1FlinkCluster) : String? =
        flinkCluster.spec?.operator?.savepointTargetPath?.trim()

    fun getRestartPolicy(flinkCluster: V1FlinkCluster) : String =
        flinkCluster.spec?.operator?.restartPolicy ?: "NEVER"
}