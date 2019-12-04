package com.nextbreakpoint.flinkoperator.cli.command

import com.nextbreakpoint.flinkoperator.cli.DefaultWebClientFactory
import com.nextbreakpoint.flinkoperator.cli.HttpUtils
import com.nextbreakpoint.flinkoperator.cli.RemoteCommand
import com.nextbreakpoint.flinkoperator.common.model.ConnectionConfig
import com.nextbreakpoint.flinkoperator.common.model.ScaleOptions

class ClusterScale : RemoteCommand<ScaleOptions>(DefaultWebClientFactory) {
    override fun run(
        connectionConfig: ConnectionConfig,
        clusterName: String,
        args: ScaleOptions
    ) {
        HttpUtils.putJson(factory, connectionConfig, "/cluster/$clusterName/scale", args)
    }
}
