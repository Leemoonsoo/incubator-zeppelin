# Apache Zeppelin on Staroid ⭐

[Apache Zeppelin](https://zeppelin.apache.org) on Staroid.

### [Click here to Launch on Staroid!](https://staroid.com/g/open-datastudio/zeppelin)


Key features
  - Zeppelin-0.9.0-SNNAPSHOT on Kubernetes
  - Spark on Kubernetes integration
  - Markdown, Shell, Spark, Python, JDBC interpreters are included
  - Clone and customize as you want
  

## Branch

| Branch |  Zeppelin version|
| ------ | --------------- |
| master-snapshot | latest master |

## Development

Check out [.staroid](https://github.com/open-datastudio/zeppelin/tree/master-snapshot/.staroid) directory of `master-snapshot` branch.


| contents | description |
| -------- | ----------  |
| staroid.yaml | [staroid config](https://docs.staroid.com/references/staroid_yaml.html) file |
| skaffold.yaml | skaffold config file |
| conf | Zeppelin configuration files to override |
| docker | Dockerfile to build images |
| k8s | Kubernetes resource manifests to run zeppelin-server |
