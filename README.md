# diag-stn
This project is a sample implementation of the *DiagSTN* algorithm. This algorithm applies Maximum Accuracy and Confirmation diagnosis (**MAC**) on Simple Temporal Diagnosis Problems (**STDPs**). This is a form of [Model-based diagnosis](https://en.wikipedia.org/wiki/Diagnosis_(artificial_intelligence)#Model-based_diagnosis) on temporal problems.

With this software, STDPs can be diagnosed.  An input problem can be given as a YAML file or can be (hard)coded in Java. There is a possibility to generate test problems. The underlying network of these problems are either according to the [Barabási–Albert model](https://en.wikipedia.org/wiki/Barab%C3%A1si%E2%80%93Albert_model) or according to a Plan-like model. A Plan-like model combines multiple simple plans according to the settings.

# Usage

Import the git repository as a Netbeans project to build the project. The only dependency is [SnakeYAML](https://bitbucket.org/asomov/snakeyaml).

After clean & build, use `java -jar DiagSTN.jar someproblem.yaml` to analyze some problem. Or use the code as a library.

## Code structure

The code consists of a few packages. The important ones are:

Root package *diag.stn* containing the program entry point and problem generators.

A *diag.stn.STN* package, which has everything needed to describe Simple Temporal Networks (i.e. graph, edge and vertex classes) and a class to describe an observation (using graph vertices).

The *diag.stn.analyse* package contains different analysis-classes which use different diagnosis methods. Also has a class describing a diagnosis and a helper class for paths in a graph.

Some sample problems in the YAML problem format can be found [here](https://github.com/FransHeuvelmans/diag-stn/tree/master/test/Data).
