# NPL

Normative Programming Language (NPL) is a language to program the norms of multi-agent systems. 
Some papers describing the language are available in the folder [publications](https://github.com/jomifred/npl/tree/master/doc/publications). 

The folder [examples/npl](https://github.com/jomifred/npl/tree/master/examples/npl) contains Java programs to illustrates its use.

This language is used to implement the organisational platform for [Moise](http://moise.sf.net), called _ORA4MAS_, that is integrated into the [JaCaMo](http://jacamo.sf.net) project.

# Installation

Some built releases are available at [GitHub](https://github.com/jomifred/npl/releases) and the source code can be used with the following commands ([gradle](https://gradle.org) is required).


	git clone https://github.com/jomifred/npl.git
	cd npl
	gradle dist
	gradle eclipse

# Examples

Steps to compile and run the examples:

	cd examples/npl
	javac -cp ../../lib/npl-0.2.jar:../../lib/jason-2.0.jar:. Example0.java
	java  -cp ../../lib/npl-0.2.jar:../../lib/jason-2.0.jar:. Example0

---
Developed by Jomi F. Hubner, Rafael H. Bordini, and Olivier Boissier.

![ScreenShot](doc/figures/s1.png?raw=true)