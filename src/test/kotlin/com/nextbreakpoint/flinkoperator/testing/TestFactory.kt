package com.nextbreakpoint.flinkoperator.testing

import com.nextbreakpoint.flinkoperator.common.crd.V1FlinkCluster
import com.nextbreakpoint.flinkoperator.common.crd.V1FlinkClusterSpec
import com.nextbreakpoint.flinkoperator.server.common.Resource
import io.kubernetes.client.models.V1JobBuilder
import io.kubernetes.client.models.V1ObjectMeta
import io.kubernetes.client.models.V1PodBuilder
import io.kubernetes.client.models.V1ServiceBuilder

object TestFactory {
    fun aCluster(name: String, namespace: String, taskManagers: Int = 1, taskSlots: Int = 1): V1FlinkCluster {
        val flinkClusterSpec = Resource.parseV1FlinkClusterSpec(
            """
            {
              "taskManagers": $taskManagers,
              "runtime": {
                "pullSecrets": "flink-regcred",
                "pullPolicy": "IfNotPresent",
                "image": "registry:30000/flink:1.9.2"
              },
              "bootstrap": {
                "serviceAccount": "bootstrap-test",
                "pullSecrets": "bootstrap-regcred",
                "pullPolicy": "IfNotPresent",
                "image": "registry:30000/flink-jobs:1",
                "jarPath": "/flink-jobs.jar",
                "className": "com.nextbreakpoint.flink.jobs.stream.TestJob",
                "arguments": [
                  "--BUCKET_BASE_PATH",
                  "file:///var/tmp"
                ]
              },
              "jobManager": {
                "serviceAccount": "jobmanager-test",
                "serviceMode": "ClusterIP",
                "maxHeapMemory": 512,
                "environment": [
                  {
                    "name": "FLINK_GRAPHITE_HOST",
                    "value": "graphite.default.svc.cluster.local"
                  }
                ],
                "environmentFrom": [
                  {
                    "secretRef": {
                      "name": "flink-secrets"
                    }
                  }
                ],
                "volumeMounts": [
                  {
                    "name": "config-vol",
                    "mountPath": "/hadoop/etc/core-site.xml",
                    "subPath": "core-site.xml"
                  },
                  {
                    "name": "config-vol",
                    "mountPath": "/docker-entrypoint.sh",
                    "subPath": "docker-entrypoint.sh"
                  },
                  {
                    "name": "config-vol",
                    "mountPath": "/opt/flink/conf/flink-conf.yaml",
                    "subPath": "flink-conf.yaml"
                  },
                  {
                    "name": "jobmanager",
                    "mountPath": "/var/tmp"
                  }
                ],
                "volumes": [
                  {
                    "name": "config-vol",
                    "configMap": {
                      "name": "flink-config",
                      "defaultMode": "511"
                    }
                  }
                ],
                "initContainers": [
                    {
                        "image": "busybox",
                        "command": [
                            "ls"
                        ],
                        "imagePullPolicy": "IfNotPresent",
                        "name": "busybox"
                    }
                ],
                "sideContainers": [
                    {
                        "image": "busybox",
                        "command": [
                            "sleep",
                            "3600"
                        ],
                        "imagePullPolicy": "IfNotPresent",
                        "name": "busybox"
                    }
                ],
                "persistentVolumeClaimsTemplates": [
                  {
                    "metadata": {
                      "name": "jobmanager"
                    },
                    "spec": {
                      "storageClassName": "hostpath",
                      "accessModes": [ "ReadWriteOnce" ],
                      "resources": {
                        "requests": {
                          "storage": "1Gi"
                        }
                      }
                    }
                  }
                ],
                "resources": {
                    "limits": {
                        "cpu": "1",
                        "memory": "512Mi"
                    },
                    "requests": {
                        "cpu": "0.1",
                        "memory": "256Mi"
                    }
                }
              },
              "taskManager": {
                "serviceAccount": "taskmanager-test",
                "taskSlots": $taskSlots,
                "maxHeapMemory": 2048,
                "environment": [
                  {
                    "name": "FLINK_GRAPHITE_HOST",
                    "value": "graphite.default.svc.cluster.local"
                  }
                ],
                "volumeMounts": [
                  {
                    "name": "config-vol",
                    "mountPath": "/hadoop/etc/core-site.xml",
                    "subPath": "core-site.xml"
                  },
                  {
                    "name": "config-vol",
                    "mountPath": "/docker-entrypoint.sh",
                    "subPath": "docker-entrypoint.sh"
                  },
                  {
                    "name": "config-vol",
                    "mountPath": "/opt/flink/conf/flink-conf.yaml",
                    "subPath": "flink-conf.yaml"
                  },
                  {
                    "name": "taskmanager",
                    "mountPath": "/var/tmp"
                  }
                ],
                "volumes": [
                  {
                    "name": "config-vol",
                    "configMap": {
                      "name": "flink-config",
                      "defaultMode": "511"
                    }
                  }
                ],
                "initContainers": [
                    {
                        "image": "busybox",
                        "command": [
                            "ls"
                        ],
                        "imagePullPolicy": "IfNotPresent",
                        "name": "busybox"
                    }
                ],
                "sideContainers": [
                    {
                        "image": "busybox",
                        "command": [
                            "sleep",
                            "3600"
                        ],
                        "imagePullPolicy": "IfNotPresent",
                        "name": "busybox"
                    }
                ],
                "persistentVolumeClaimsTemplates": [
                  {
                    "metadata": {
                      "name": "taskmanager"
                    },
                    "spec": {
                      "storageClassName": "hostpath",
                      "accessModes": [ "ReadWriteOnce" ],
                      "resources": {
                        "requests": {
                          "storage": "5Gi"
                        }
                      }
                    }
                  }
                ],
                "resources": {
                    "limits": {
                        "cpu": "1",
                        "memory": "2048Mi"
                    },
                    "requests": {
                        "cpu": "0.2",
                        "memory": "1024Mi"
                    }
                }
              },
              "operator": {
                "restartPolicy": "Never",
                "savepointMode": "Automatic",
                "savepointInterval": "60",
                "savepointTargetPath": "file:///var/tmp/test"
              }
            }
            """.trimIndent()
        )
        return makeV1FlinkCluster(
            name,
            namespace,
            flinkClusterSpec
        )
    }

    private fun makeV1FlinkCluster(name: String, namespace: String, flinkClusterSpec: V1FlinkClusterSpec): V1FlinkCluster {
        val flinkCluster = V1FlinkCluster()
            .apiVersion("nextbreakpoint.com/v1")
            .kind("FlinkCluster")
        val objectMeta = V1ObjectMeta().namespace(namespace).name(name)
        flinkCluster.metadata = objectMeta
        flinkCluster.spec = flinkClusterSpec
        return flinkCluster
    }

    fun aBootstrapJob(cluster: V1FlinkCluster) = V1JobBuilder()
        .withNewMetadata()
        .withNamespace(cluster.metadata.namespace)
        .withName("${cluster.metadata.name}-job")
        .withLabels(mapOf(
            "name" to cluster.metadata.name,
            "uid" to cluster.metadata.uid
        ))
        .withUid(cluster.metadata.uid)
        .endMetadata()
        .build()

    fun aJobManagerService(cluster: V1FlinkCluster) = V1ServiceBuilder()
        .withNewMetadata()
        .withNamespace(cluster.metadata.namespace)
        .withName("${cluster.metadata.name}-service")
        .withLabels(mapOf(
            "name" to cluster.metadata.name,
            "uid" to cluster.metadata.uid
        ))
        .withUid(cluster.metadata.uid)
        .endMetadata()
        .build()

    fun aJobManagerPod(cluster: V1FlinkCluster, suffix: String) = V1PodBuilder()
        .withNewMetadata()
        .withNamespace(cluster.metadata.namespace)
        .withName("${cluster.metadata.name}-pod-$suffix")
        .withLabels(mapOf(
            "name" to cluster.metadata.name,
            "uid" to cluster.metadata.uid,
            "role" to "jobmanager"
        ))
        .withUid(cluster.metadata.uid)
        .endMetadata()
        .build()

    fun aTaskManagerPod(cluster: V1FlinkCluster, suffix: String) = V1PodBuilder()
        .withNewMetadata()
        .withNamespace(cluster.metadata.namespace)
        .withName("${cluster.metadata.name}-pod-$suffix")
        .withLabels(mapOf(
            "name" to cluster.metadata.name,
            "uid" to cluster.metadata.uid,
            "role" to "taskmanager"
        ))
        .withUid(cluster.metadata.uid)
        .endMetadata()
        .build()
}
