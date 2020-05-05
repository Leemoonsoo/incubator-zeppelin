# Apache Zeppelin on Staroid ⭐

[Apache Zeppelin](https://zeppelin.apache.org) on Staroid.

### [Click here to Launch on Staroid!](https://staroid.com/g/open-datastudio/zeppelin)


Key features
  - Zeppelin-0.9.0-SNNAPSHOT on Kubernetes
  - Spark on Kubernetes integration
  - Markdown, Shell, Spark, Python, JDBC interpreters are included
  - Clone and customize as you want
  
#### Screenshots

<sub>Click to Launch, no installation or complex configuration</sub>

<img src="https://user-images.githubusercontent.com/1540981/80290413-ae736c00-86f9-11ea-9a85-d479c285e1bf.png" width="600px" />
<img src="https://user-images.githubusercontent.com/1540981/80290354-8126be00-86f9-11ea-8bd7-cdeadbd32db6.png" width="600px" />


<sub>Click 'Open' button and bring Zeppelin notebook</sub>

<img src="https://user-images.githubusercontent.com/1540981/80290427-bcc18800-86f9-11ea-8626-d899218dd3a1.png" width="600px" />

<sub>Spark on Kubernetes works out of the box</sub>

<img src="https://user-images.githubusercontent.com/1540981/80290438-cf3bc180-86f9-11ea-8c1f-d2dedcd48a86.png" width="600px" />

<sub>Access to Spark UI</sub>

<img src="https://user-images.githubusercontent.com/1540981/80290443-d8c52980-86f9-11ea-999c-eeafab25cf38.png" width="600px" />

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
