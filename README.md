# diag-stn
This project is a sample implementation of the *DiagSTN* algorithm. This algorithm applies Maximum Accuracy and Consistency diagnosis on Simple Temporal Diagnosis Problems (**STDPs**). This is a form of [Model-based diagnosis](https://en.wikipedia.org/wiki/Diagnosis_(artificial_intelligence)#Model-based_diagnosis) on temporal problems.

With this software STDPs can be diagnosed.  An input problem can be given as a YAML file or can be (hard)coded in Java. It is also possible to generate a test problem. The underlying network of the problems generated are either according to the [Barabási–Albert model](https://en.wikipedia.org/wiki/Barab%C3%A1si%E2%80%93Albert_model) or according to a Plan-like model. A Plan-like model combines multiple simple plans according to the settings.

# Usage

Use `java -jar DiagSTN.jar someproblem.yaml` to analyze some problem. Or use the code as a library.

## Code structure

The code consists of a few packages. The important ones are:

Root package *diag.stn* containing the program entry point and problem generators.

A *diag.stn.STN* package, which has everything needed to describe Simple Temporal Networks (i.e. graph, edge and vertex classes) and a class to describe an observation (on a graph). 

The *diag.stn.analyse* package, which contains different Analysis classes which use different diagnosis methods. Also has the diagnosis class and a helper class for paths in a graph.

Some sample problems in a YAML format can be found [here](https://github.com/FransHeuvelmans/diag-stn/tree/master/test/Data).
