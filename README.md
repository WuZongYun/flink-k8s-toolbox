# Flink Kubernetes Toolbox

Flink Kubernetes Toolbox contains tools for managing Flink clusters and jobs on Kubernetes:

- Flink Operator

    The Flink Operator is an implementation of the Kubernetes Operator pattern for managing Flink clusters and jobs.
    The operator uses a Custom Resource Definition to represent a cluster with a single job.
    It detects changes of the custom resource and modifies the derived resources which constitute the cluster.
    It takes care of creating savepoints periodically, monitoring the status of the job to detect a failure,
    restarting the job from the latest savepoint if needed, and rescaling the cluster when required.

- Flink Operator CLI

    The Flink Operator CLI provides an interface for controlling Flink clusters and jobs from a terminal.
    It supports commands for creating or deleting clusters, starting or stopping clusters and jobs,
    rescaling clusters, getting metrics and other information about clusters and jobs.

Main features:

- Automatic creation of Supervisor process 
- Automatic creation of JobManager and TaskManagers
- Automatic creation of Kubernetes services
- Support for batch and stream jobs
- Support for init containers and side containers for JobManager and TaskManagers
- Support for mounted volumes (same as volumes in Pod specification)
- Support for environment variables, including variables from ConfigMap or Secret
- Support for resource requirements (for all components)
- Support for user defined annotations
- Support for user defined container ports
- Support for pull secrets and private registries
- Support for public Flink images or custom images
- Support for single job cluster or cluster without job
- Support separate image for bootstrap (with single JAR file)
- Configurable task slots
- Configurable service accounts
- Configurable savepoints location
- Configurable periodic savepoints
- Automatic recovery from temporary failure
- Automatic scaling via standard autoscaling interface
- Automatic restart of the cluster or job when specification changed
- Automatic creation of savepoint before stopping cluster or job  
- Automatic recovery from latest savepoint when restarting job  
- Resource status and printer columns
- Readiness probe for JobManager
- CLI interface for operations and monitoring   
- Internal metrics compatible with Prometheus  

## License

The tools are distributed under the terms of BSD 3-Clause License.

    Copyright (c) 2020, Andrea Medeghini
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this
      list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    * Neither the name of the tools nor the names of its
      contributors may be used to endorse or promote products derived from
      this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
    DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
    FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
    DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
    SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
    CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
    OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
    OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

## Flink Operator and Cluster status

The operator detects new resource of kind FlinkCluster (primary resource) in a namespace, and automatically creates other managed
resources (secondary resources), like Pods, Service, and BatchJob, based on the specification provided in the custom resource.  
The operator persists some status on the resource, and performs several tasks automatically, such as creating savepoints
or recreating the cluster when the specification changed.

The possible states of a FlinkCluster resource are represented in this graph:

![Cluster status graph](/graphs/flink-cluster.png "Cluster status graph")

- **UNKNOWN**

  This is the initial status of the Flink cluster when it is created.

- **STARTING**

  This status means that the cluster is starting up. The secondary resources will be created, the JAR file will be uploaded to Flink, and the job will be started (optional).

- **STOPPING**

  This status means that the cluster is going to stop. The job will be canceled (creating a new savepoint) or stopped (without creating a new savepoint).

- **SUSPENDED**

  This status means that the cluster has been suspended. The secondary resources are stopped.

- **TERMINATED**

  This status means that the cluster has been terminated. The secondary resources are deleted.

- **RUNNING**

  This status means that the cluster is running and when defined also the job is running.

- **CANCELLING**

  This status means that the cluster is cancelling the job. The operator will attempt to cancel the job creating a savepoint if required. 

- **RESTARTING**

  This status means that the cluster is restarting the secondary resources. A change has been detected in the primary resource specification and the operator is modifying the secondary resources to reflect that change. The operator will attempt to cancel the job creating a savepoint if required.

- **SCALING**

  This status means that the cluster is scaling the secondary resources. A change has been detected in the primary resource specification and the operator is modifying the secondary resources to reflect that change. The operator will attempt to cancel the job creating a savepoint if required.
  
- **UPDATING**

  This status means that the cluster is updating the secondary resources. A change has been detected in the primary resource specification and the operator is modifying the secondary resources to reflect that change. The operator might need to destroy and recreate some resources. 

- **FAILED**

  This status means that the job has failed or some other error has occurred. The secondary resources are stopped.

- **FINISHED**

  This status means that the job has finished. The secondary resources are stopped.

## Flink Operator REST API

The operator exposes a REST API on port 4444 by default. The API provides information about the status of the resources, metrics of clusters and jobs, and more:

    http://localhost:4444/clusters

    http://localhost:4444/cluster/<name>/status

    http://localhost:4444/cluster/<name>/job/details

    http://localhost:4444/cluster/<name>/job/metrics

    http://localhost:4444/cluster/<name>/jobmanager/metrics

    http://localhost:4444/cluster/<name>/taskmanagers

    http://localhost:4444/cluster/<name>/taskmanagers/<taskmanager>/metrics

Please note that you must use SSL certificates when HTTPS is enabled (see instructions for generating SSL certificates):

    curl --cacert secrets/ca_cert.pem --cert secrets/operator-cli_cert.pem --key secrets/operator-cli_key.pem https://localhost:4444/cluster/test/status

## Generate SSL certificates and keystores

Execute the script secrets.sh to generate self-signed certificates and keystores to use with the Flink Operator:

    ./secrets.sh flink-operator key-password keystore-password truststore-password

This command will generate new certificates and keystores in the directory secrets.

## Monitor Flink Operator

The operator exposes metrics to Prometheus on port 8080 by default:

    http://localhost:8080/metrics

## Install Flink Operator

Create a namespace for the operator:

    kubectl create namespace flink-operator

The name of the namespace can be any name you like.

Create a namespace for the clusters:

    kubectl create namespace flink-jobs

The name of the namespace can be any name you like.

Create a secret which contain the keystore and the truststore files:

    kubectl -n flink-operator create secret generic flink-operator-ssl \
        --from-file=keystore.jks=secrets/keystore-operator-api.jks \
        --from-file=truststore.jks=secrets/truststore-operator-api.jks \
        --from-literal=keystore-secret=keystore-password \
        --from-literal=truststore-secret=truststore-password

The name of the secret can be any name you like.

Install the operator's CRD resource with Helm command:

    helm install flink-k8s-toolbox-crd helm/flink-k8s-toolbox-crd

Install the operator's default roles with Helm command:

    helm install flink-k8s-toolbox-roles helm/flink-k8s-toolbox-roles --namespace flink-jobs

Install the operator's resources with SSL enabled:

    helm install flink-k8s-toolbox-operator helm/flink-k8s-toolbox-operator --namespace flink-operator --set namespace=flink-jobs --set secretName=flink-operator-ssl

Or if you prefer install the operator's resources with SSL disabled:

    helm install flink-k8s-toolbox-operator helm/flink-k8s-toolbox-operator --namespace flink-operator --set namespace=flink-jobs

Run the operator with command:

    kubectl -n flink-operator scale deployment flink-operator --replicas=1

Alternatively, you can add the argument --set replicas=1 when installing the operator with Helm.

## Uninstall Flink Operator

Stop the operator with command:

    kubectl -n flink-operator scale deployment flink-operator --replicas=0

Remove the operator's resources with command:    

    helm uninstall flink-k8s-toolbox-operator --namespace flink-operator

Remove the operator's default roles with command:    

    helm uninstall flink-k8s-toolbox-roles --namespace flink-jobs

Remove the operator's CRD resource with command:    

    helm uninstall flink-k8s-toolbox-crd

Remove secret with command:    

    kubectl -n flink-operator delete secret flink-operator-ssl

Remove operator namespace with command:    

    kubectl delete namespace flink-operator

Remove clusters namespace with command:    

    kubectl delete namespace flink-jobs

## Upgrade Flink Operator from previous version

PLEASE NOTE THAT THE OPERATOR IS STILL IN BETA VERSION AND IT DOESN'T HAVE A STABLE API YET, THEREFORE EACH RELEASE MIGHT INTRODUCE BREAKING CHANGES.

Before upgrading to a new release you must cancel all jobs creating a savepoint into a durable storage location (for instance AWS S3).

Create a copy of your FlinkCluster resources:

    kubectl -n flink-operator get fc -o yaml > clusters-backup.yaml

Upgrade the CRD using Helm (it prevents from deletion of existing FlinkCluster resources):

    helm upgrade flink-k8s-toolbox-crd --install helm/flink-k8s-toolbox-crd

Upgrade the operator using Helm:

    helm upgrade flink-k8s-toolbox-operator --install helm/flink-k8s-toolbox-operator --namespace flink-operator --set namespace=flink-jobs --set secretName=flink-operator-ssl --set replicas=1

After installing the new version, you can restart the jobs. However, the custom resources might not be compatible with the new CRD.
If that is the case, then you have to fix the resource specification, perhaps you have to delete the resource and recreate it.

## Get Docker image of Flink Operator

The operator's Docker image can be downloaded from Docker Hub:

    docker pull nextbreakpoint/flink-k8s-toolbox:1.3.5-beta

Tag and push the image into your private registry if needed:

    docker tag nextbreakpoint/flink-k8s-toolbox:1.3.5-beta some-registry/flink-k8s-toolbox:1.3.5-beta
    docker login some-registry
    docker push some-registry/flink-k8s-toolbox:1.3.5-beta

## Run Flink Operator manually

Run the operator using the image on Docker Hub:

    kubectl run flink-operator --restart=Never -n flink-operator --image=nextbreakpoint/flink-k8s-toolbox:1.3.5-beta \
        --overrides='{ "apiVersion": "v1", "metadata": { "labels": { "app": "flink-operator" } }, "spec": { "serviceAccountName": "flink-operator", "imagePullPolicy": "Always" } }' -- operator run --namespace=flink-jobs

Or run the operator using your private registry and pull secrets:

    kubectl run flink-operator --restart=Never -n flink-operator --image=some-registry/flink-k8s-toolbox:1.3.5-beta \
        --overrides='{ "apiVersion": "v1", "metadata": { "labels": { "app": "flink-operator" } }, "spec": { "serviceAccountName": "flink-operator", "imagePullPolicy": "Always", "imagePullSecrets": [{"name": "your-pull-secrets"}] } }' -- operator run --namespace=flink-jobs

Please note that you **MUST** run only one operator for each namespace to avoid conflicts.

The service account flink-operator is created when installing the Helm chart:

    helm install flink-k8s-toolbox-operator --namespace flink-operator helm/flink-k8s-toolbox-operator

Verify that the pod has been created:

    kubectl -n flink-operator get pod flink-operator -o yaml     

Verify that there are no errors in the logs:

    kubectl -n flink-operator logs flink-operator

Check the events in case that the pod doesn't start:

    kubectl -n flink-operator get events

Stop the operator with command:

    kubectl -n flink-operator delete pod flink-operator

## Flink operator custom resources

FlinkCluster resources can be created or deleted as any other resource in Kubernetes using kubectl command.

The Custom Resource Definition is installed with a separate Helm chart:

    helm install flink-k8s-toolbox-crd helm/flink-k8s-toolbox-crd

The CRD and its schema are defined in the Helm template:

    https://github.com/nextbreakpoint/flink-k8s-toolbox/blob/master/helm/flink-k8s-toolbox-crd/templates/crd.yaml

When updating the operator, upgrade the CRD with Helm instead of deleting and reinstalling:

    helm upgrade flink-k8s-toolbox-crd --install helm/flink-k8s-toolbox-crd  

Do not delete the CRD unless you want to delete all custom resources depending on it.

### Create service account for Flink cluster components

JobManager and TaskManagers can be configured to use a different service account.
Like Pod resources, you can specify a service account in the FlinkCluster resource:

    jobManager:
      serviceAcount: some-account
    ...

    taskManager:
      serviceAcount: some-account
    ...

If not specified, the default service account will be used for JobManager and TaskManagers.

There is another service account required for starting the cluster which is
configured in the bootstrap section of the FlinkCluster resource:

    bootstrap:
      serviceAccount: flink-bootstrap
    ...

This account requires specific roles and permissions in the jobs namespace:

    apiVersion: rbac.authorization.k8s.io/v1
    kind: Role
    metadata:
      name: flink-bootstrap
      namespace: flink-jobs
    rules:
      - apiGroups: [""]
        resources: ["services", "pods"]
        verbs: ["get", "list"]
      - apiGroups: ["nextbreakpoint.com"]
        resources: ["flinkclusters"]
        verbs: ["get", "list"]
    ---
    apiVersion: v1
    kind: ServiceAccount
    metadata:
      name: flink-bootstrap
      namespace: flink-jobs
    ---
    apiVersion: rbac.authorization.k8s.io/v1
    kind: RoleBinding
    metadata:
      name: flink-bootstrap
      namespace: flink-jobs
    roleRef:
      apiGroup: rbac.authorization.k8s.io
      kind: Role
      name: flink-bootstrap
    subjects:
      - kind: ServiceAccount
        name: flink-bootstrap
        namespace: flink-jobs

The default name of the service account is flink-bootstrap, but it can be changed to any name.

### Create a Flink cluster

Make sure the CRD has been installed (see above).

Create a Docker file:

    FROM nextbreakpoint/flink-k8s-toolbox:1.3.5-beta
    COPY flink-jobs.jar /flink-jobs.jar

where flink-jobs.jar contains the code of your Flink jobs.

Create a Docker image:

    docker build -t flink-jobs:1 .

Tag and push the image into your registry if needed:

    docker tag flink-jobs:1 some-registry/flink-jobs:1
    docker login some-registry
    docker push some-registry/flink-jobs:1

Pull Flink image:

    docker pull flink:1.9.2

Tag and push the image into your registry if needed:

    docker tag flink:1.9.2 some-registry/flink:1.9.2
    docker login some-registry
    docker push some-registry/flink:1.9.2

Create a Flink Cluster file:

    cat <<EOF >test.yaml
    apiVersion: "nextbreakpoint.com/v1"
    kind: FlinkCluster
    metadata:
      name: test
    spec:
      taskManagers: 1
      runtime:
        pullPolicy: Always
        image: some-registry/flink:1.9.2
      bootstrap:
        pullPolicy: Always
        image: some-registry/flink-jobs:1
        jarPath: /flink-jobs.jar
        className: com.nextbreakpoint.flink.jobs.stream.TestJob
        arguments:
          - --DEVELOP_MODE
          - disabled
      jobManager:
        serviceMode: NodePort
        annotations:
          managed: true
        environment:
        - name: FLINK_JM_HEAP
          value: "256"
        - name: FLINK_GRAPHITE_HOST
          value: graphite.default.svc.cluster.local
        environmentFrom:
        - secretRef:
            name: flink-secrets
        volumeMounts:
          - name: jobmanager
            mountPath: /var/tmp
        extraPorts:
          - name: prometheus
            containerPort: 9999
            protocol: TCP
        persistentVolumeClaimsTemplates:
          - metadata:
              name: jobmanager
            spec:
              storageClassName: hostpath
              accessModes:
               - ReadWriteOnce
              resources:
                requests:
                  storage: 1Gi
      taskManager:
        taskSlots: 1
        annotations:
          managed: true
        environment:
        - name: FLINK_TM_HEAP
          value: "1024"
        - name: FLINK_GRAPHITE_HOST
          value: graphite.default.svc.cluster.local
        volumeMounts:
          - name: taskmanager
            mountPath: /var/tmp
        extraPorts:
          - name: prometheus
            containerPort: 9999
            protocol: TCP
        persistentVolumeClaimsTemplates:
          - metadata:
              name: taskmanager
            spec:
              storageClassName: hostpath
              accessModes:
               - ReadWriteOnce
              resources:
                requests:
                  storage: 5Gi
      operator:
        savepointMode: Automatic
        savepointInterval: 60
        savepointTargetPath: file:///var/tmp/test
        restartPolicy: Always
    EOF

Create a FlinkCluster resource with command:

    kubectl -n flink-jobs apply -f test.yaml

Please note that you can use any image of Flink as far as the image implements the standard commands for running JobManager and TaskManager.

### Delete a Flink cluster

Delete a FlinkCluster with command:

    kubectl -n flink-jobs delete -f test.yaml

### List Flink clusters

List custom objects of type FlinkCluster with command:

    kubectl -n flink-jobs get flinkclusters

The command should produce an output like:

    NAME   CLUSTER-STATUS   TASK-STATUS   TASK-MANAGERS   TASK-SLOTS   ACTIVE-TASK-MANAGERS   TOTAL-TASK-SLOTS   JOB-PARALLELISM   JOB-RESTART   SERVICE-MODE   SAVEPOINT-MODE   SAVEPOINT-PATH                                       SAVEPOINT-AGE   AGE
    test   Running          Idle          1               1            1                      1                  1                 Always        NodePort       Manual           file:/var/savepoints/savepoint-e0e430-7a6d1c33dee3   42s             3m55s

## Build Flink Operator from source code

Build the uber JAR file with command:

    ./gradlew clean shadowJar

and test the JAR invoking the CLI:

    java -jar build/libs/flink-k8s-toolbox-1.3.5-beta-with-dependencies.jar --help

Please note that Java 8 is required to build the JAR. Define JAVA_HOME variable to specify the correct JDK:  

    export JAVA_HOME=/path_to_jdk
    ./gradlew clean shadowJar

Build a Docker image with command:

    docker build -t flink-k8s-toolbox:1.3.5-beta .

and test the image printing the CLI usage:

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta --help

Tag and push the image to your Docker registry if needed:

    docker tag flink-k8s-toolbox:1.3.5-beta some-registry/flink-k8s-toolbox:1.3.5-beta
    docker login some-registry
    docker push some-registry/flink-k8s-toolbox:1.3.5-beta

## Run automated tests of Flink Operator

Run unit tests with command:

    ./gradlew clean test

Run integration tests against Docker for Desktop or Minikube with command:

    export BUILD_IMAGES=true
    ./gradlew clean integrationTest

You can skip the Docker images build step, if images already exist:

    export BUILD_IMAGES=false
    ./gradlew clean integrationTest

Please note that only Java 8 is supported. Define JAVA_HOME variable to specify the correct JDK:  

    export JAVA_HOME=/path_to_jdk
    export BUILD_IMAGES=true
    ./gradlew clean integrationTest

## Automatic savepoints

The operator automatically creates savepoints before stopping the cluster.
This might happen when a change is applied to the cluster specification or
the cluster is rescaled or manually stopped. This feature is very handy to
avoid losing the status of the job.
When the operator restarts the cluster, it uses the latest savepoint to
recover the status of the job. However, for this feature to work properly,
the savepoints must be created in a durable storage location such as HDFS or S3.
Only a durable location can be used to recover the job after recreating
the Job Manager and the Task Managers.

## How to use the Operator CLI

Print the CLI usage:

    docker run --rm -it nextbreakpoint/flink-k8s-toolbox:1.3.5-beta --help

The output should look like:

    Usage: flink-k8s-toolbox [OPTIONS] COMMAND [ARGS]...

    Options:
      -h, --help  Show this message and exit

    Commands:
      operator      Access operator subcommands
      clusters      Access clusters subcommands
      cluster       Access cluster subcommands
      savepoint     Access savepoint subcommands
      bootstrap     Access bootstrap subcommands
      job           Access job subcommands
      jobmanager    Access JobManager subcommands
      taskmanager   Access TaskManager subcommands
      taskmanagers  Access TaskManagers subcommands

### How to create a cluster

Create a Docker file like:

    FROM nextbreakpoint/flink-k8s-toolbox:1.3.5-beta
    COPY flink-jobs.jar /flink-jobs.jar

where flink-jobs.jar contains the code of your Flink job.

Create a Docker image:

    docker build -t flink-jobs:1 .

Tag and push the image into your registry if needed:

    docker tag flink-jobs:1 some-registry/flink-jobs:1
    docker login some-registry
    docker push some-registry/flink-jobs:1

Create a JSON file:

    cat <<EOF >test.json
    {
      "taskManagers": 1,
      "runtime": {
        "pullPolicy": "Always",
        "image": "some-registry/flink:1.9.2"
      },
      "bootstrap": {
        "pullPolicy": "Always",
        "image": "some-registry/flink-jobs:1",
        "jarPath": "/flink-jobs.jar",
        "className": "com.nextbreakpoint.flink.jobs.stream.TestJob",
        "arguments": [
          "--DEVELOP_MODE",
          "disabled"
        ]
      },
      "jobManager": {
        "serviceMode": "NodePort",
        "environment": [
          {
            "name": "FLINK_JM_HEAP",
            "value": "256"
          },
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
        ]
      },
      "taskManager": {
        "environment": [
          {
            "name": "FLINK_TM_HEAP",
            "value": "1024"
          },
          {
            "name": "FLINK_GRAPHITE_HOST",
            "value": "graphite.default.svc.cluster.local"
          }
        ],
        "volumeMounts": [
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
          },
          {
            "name": "data-vol",
            "hostPath": {
              "path": "/var/data"
            }
          }
        ]
      },
      "operator": {
        "savepointMode": "Automatic",
        "savepointInterval": 300,
        "savepointTargetPath": "file:///var/tmp/test",
        "restartPolicy": "Never"
      }
    }
    EOF

Execute the command:

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster create --cluster-name=test --cluster-spec=test.json --host=$OPERATOR_HOST --port=4444

Pass keystore and truststore if SSL is enabled:

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster create --cluster-name=test --cluster-spec=test.json --host=$OPERATOR_HOST --port=4444
    --keystore-path=secrets/keystore-operator-cli.jks --truststore-path=secrets/truststore-operator-cli.jks --keystore-secret=keystore-password --truststore-secret=truststore-password

If you expose the operator on a port of Docker's host:

        Set OPERATOR_HOST to localhost on Linux

        Set OPERATOR_HOST to host.docker.internal on MacOS

Show more options with the command:

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster create --help

Get the list of clusters

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta clusters list --host=$OPERATOR_HOST --port=4444

Get the status of a cluster

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster status --cluster-name=test --host=$OPERATOR_HOST --port=4444

Use jq to format the output:

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster status --cluster-name=test --host=$OPERATOR_HOST --port=4444 | jq -r

Delete a cluster

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster delete --cluster-name=test --host=$OPERATOR_HOST --port=4444

Stop a running cluster

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster stop --cluster-name=test --host=$OPERATOR_HOST --port=4444

Restart a stopped or failed cluster

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster start --cluster-name=test --host=$OPERATOR_HOST --port=4444

Start a cluster and run the job without savepoint

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster start --cluster-name=test --without-savepoint --host=$OPERATOR_HOST --port=4444

Stop a cluster without creating a savepoint

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster stop --cluster-name=test --without-savepoint --host=$OPERATOR_HOST --port=4444

Create a new savepoint

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta savepoint trigger --cluster-name=test --host=$OPERATOR_HOST --port=4444

Get the status of a cluster

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster status --cluster-name=test --host=$OPERATOR_HOST --port=4444

Rescale a cluster (with savepoint)

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta cluster scale --cluster-name=test --task-managers=4 --host=$OPERATOR_HOST --port=4444

Get the details of the running job

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta job details --cluster-name=test --host=$OPERATOR_HOST --port=4444

Get the metrics of the running job

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta job metrics --cluster-name=test --host=$OPERATOR_HOST --port=4444

Get a list of Task Managers

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta taskmanagers list --cluster-name=test --host=$OPERATOR_HOST --port=4444

Get the metrics of the Job Manager

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta jobmanager metrics --cluster-name=test --host=$OPERATOR_HOST --port=4444

Get the metrics of a Task Manager

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta taskmanager metrics --cluster-name=test --host=$OPERATOR_HOST --port=4444

You will be asked to provide a Task Manager id which you can get from the list of Task Managers.   

Get the details of a Task Manager

    docker run --rm -it flink-k8s-toolbox:1.3.5-beta taskmanager details --cluster-name=test --host=$OPERATOR_HOST --port=4444

You will be asked to provide a Task Manager id which you can get from the list of Task Managers.   

## How to upload a JAR file and start a job

Flink jobs must be packaged in a regular JAR file and uploaded to the JobManager.

Upload a JAR file using the command:

    java -jar flink-k8s-toolbox-1.3.5-beta.jar bootstrap run --cluster-name=test --class-name=your-main-class --jar-path=/your-job-jar.jar

When running outside Kubernetes use the command:

    java -jar flink-k8s-toolbox-1.3.5-beta.jar bootstrap run --kube-config=/your-kube-config --cluster-name=test --class-name=your-main-class --jar-path=/your-job-jar.jar

## How to run the Operator for testing

The Flink Operator can be executed as Docker image or JAR file, pointing to a local or remote Kubernetes cluster.    

Run the operator with a given namespace and Kubernetes config using the JAR file:

    java -jar flink-k8s-toolbox:1.3.5-beta.jar operator run --namespace=test --kube-config=~/.kube/config

Run the operator with a given namespace and Kubernetes config using the Docker image:

    docker run --rm -it -v ~/.kube/config:/kube/config flink-k8s-toolbox:1.3.5-beta operator run --namespace=test --kube-config=/kube/config

## Configure task timeout

The Flink Operator uses timeouts to recover for anomalies.

The duration of the timeout has a default value of 300 seconds and can be changed setting the environment variable TASK_TIMEOUT (number of seconds).   

## Configure polling interval

The Flink Operator polls periodically the status of the resources.

The polling interval has a default value of 5 seconds and can be changed setting the environment variable POLLING_INTERVAL (number of seconds).   
